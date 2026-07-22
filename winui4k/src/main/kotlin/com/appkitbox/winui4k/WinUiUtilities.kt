package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ReleasePump
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.FfiBackend
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.StructValue
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.win32.Win32
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.WinRtRuntime
import com.appkitbox.winui4k.internal.winui.Dispatcher
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.WinAppSdkBootstrap
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WinUI 3's equivalent of SwingUtilities. Handles lazily starting the UI thread and
 * posting work to it.
 *
 * As with Swing's EDT, the developer doesn't need to think about startup explicitly.
 * The first call to [invokeLater] (or [schedule]) automatically starts WinUI 3 on a
 * dedicated worker thread. The JVM stays alive as long as this thread (non-daemon) is
 * alive, i.e. until the last window is closed.
 *
 * Launch sequence (on the dedicated thread):
 *  1. Declare Per-Monitor v2 DPI awareness
 *  2. Windows App SDK bootstrap (MddBootstrapInitialize2) — binds an installed WinAppSDK
 *     runtime package to this process as a dynamic reference
 *  3. RoInitialize(STA) — this thread becomes the UI thread
 *  4. Application.Start(callback) — inside the callback, composes an Application
 *     "subclass" via COM aggregation (WinRT composition) and builds the UI in OnLaunched
 *  5. Start returns via [exit] (or the automatic exit triggered by the last window
 *     closing), and the bootstrap is then released
 *
 * How shutdown works: XAML's default (DispatcherShutdownMode.OnLastWindowClose) ends the
 * message loop the instant the last window closes, and XAML's own constraints mean it can
 * never be restarted within the same process. So this switches to OnExplicitShutdown to
 * keep the loop itself alive, tracks the number of open [WFrame]s, and once that count hits
 * 0, "posts" running Application.Exit onto the message loop instead of calling it immediately
 * (the default behavior when [exitOnLastWindowClosed] = true). That means even after the
 * last window closes, opening a new window before the deferred check runs keeps the app alive.
 */
object WinUiUtilities {

    private val DESC_THIS_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)

    /** GetXamlType(this, TypeName byval, out IXamlType) */
    private val DESC_GET_XAML_TYPE = CallDescriptor(
        ValueKind.I32,
        ArgKind.PTR,
        ArgKind.Struct(XamlStructs.TYPE_NAME),
        ArgKind.PTR,
    )

    /** GetXmlnsDefinitions(this, out UINT32, out XmlnsDefinition*) / GetXamlType(this, HSTRING, out) */
    private val DESC_THIS_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

    private var currentApp: ComPtr? = null

    private val started = AtomicBoolean(false)
    private val ready = CountDownLatch(1)

    @Volatile
    private var startupError: Throwable? = null

    @Volatile
    private var exited = false

    /**
     * Explicitly selects the FFI backend to use (defaults to Panama).
     * Only callable before the first FFI use (= WinUI startup).
     * Can also be selected via the system property -Dwinui4k.ffi=panama|jna.
     */
    fun setFfiBackend(backend: FfiBackend) {
        Ffi.setBackend(backend)
    }

    /**
     * Starts WinUI on a dedicated thread if it hasn't been started yet, and waits until
     * the UI thread is ready to use. If startup fails, every call to this method throws
     * [IllegalStateException].
     */
    private fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            // A non-daemon thread: keeps the JVM alive until the last window closes
            Thread({
                try {
                    runMessageLoop { ready.countDown() }
                } catch (t: Throwable) {
                    startupError = t
                } finally {
                    exited = true
                    ready.countDown() // also release waiters if startup failed
                }
            }, "WinUI4K-UI").start()
        }
        ready.await()
        startupError?.let { throw IllegalStateException("Failed to start WinUI", it) }
        check(!exited) { "The WinUI application has already exited (the last window was closed, or exit() was called)" }
    }

    /**
     * Launches WinUI 3 and runs [onReady] on the UI thread.
     * Blocks until the application exits (the last window closes).
     */
    private fun runMessageLoop(onReady: () -> Unit) {
        Win32.enablePerMonitorDpiAwareness()
        WinAppSdkBootstrap.initialize()
        try {
            WinRtRuntime.initializeSta()

            // Application.Start(callback) — the callback is invoked in the UI thread context set up by XAML
            val initCallback = KComObject("WinUI4K.InitCallback", inspectable = false)
                .addInterface(
                    XamlInterop.IID_ApplicationInitializationCallback,
                    listOf(
                        KComObject.Method(DESC_THIS_PTR) {
                            // Invoke(this, params)
                            createApplication(onReady)
                            KComObject.S_OK
                        },
                    ),
                )

            val statics = Activation.factory(XamlInterop.CLS_Application, XamlInterop.IID_IApplicationStatics)
            statics.call(XamlInterop.IApplicationStatics_Start, initCallback.primary) // the message loop runs here
            statics.release()
        } finally {
            // Stop GC-driven releases from here on (a Release after RoUninitialize would crash)
            ReleasePump.shutdown()
            WinRtRuntime.uninitialize()
            WinAppSdkBootstrap.shutdown()
            WinAppSdkBootstrap.cleanupExtractedFiles()
        }
    }

    /** True if the current thread is WinUI's UI thread (SwingUtilities.isEventDispatchThread-like). */
    val isDispatchThread: Boolean
        get() = Dispatcher.isDispatchThread

    /**
     * Posts [block] to the UI thread's message loop (SwingUtilities.invokeLater-like).
     * Can be called from any thread. If WinUI hasn't started yet, it starts automatically.
     * Calls made after the app has exited (the last window closed) throw [IllegalStateException].
     * A [block] posted right before exit may not run.
     */
    fun invokeLater(block: () -> Unit) {
        ensureStarted()
        Dispatcher.invokeLater(block)
    }

    /**
     * Runs [block] once on the UI thread after [delayMillis] milliseconds (a one-shot
     * javax.swing.Timer-like). Calling close() on the return value cancels it if it hasn't
     * fired yet. Can be called from any thread. If WinUI hasn't started yet, it starts automatically.
     */
    fun schedule(delayMillis: Long, block: () -> Unit): AutoCloseable {
        ensureStarted()
        return Dispatcher.schedule(delayMillis, block)
    }

    /**
     * Whether to auto-exit the app when the last [WFrame] closes (default true).
     * Set to false to keep the message loop running even after every window closes,
     * so [invokeLater] can still open a new window. Call [exit] to shut down explicitly.
     */
    @Volatile
    var exitOnLastWindowClosed: Boolean = true

    /** The number of open (not yet Closed) WFrames. Updated only on the UI thread. */
    private var openWindowCount = 0

    /** Called when a [WFrame] is created. Must be called on the UI thread. */
    internal fun noteWindowCreated() {
        openWindowCount++
    }

    /**
     * Called when a [WFrame] closes (Window.Closed). Must be called on the UI thread.
     * If it was the last window, posts the exit check onto the message loop instead of
     * running it immediately — this deferral means opening a new window right after
     * closing (within the same cycle) keeps the app from exiting.
     */
    internal fun noteWindowClosed() {
        openWindowCount--
        if (openWindowCount == 0 && exitOnLastWindowClosed) {
            Dispatcher.invokeLater {
                if (openWindowCount == 0 && exitOnLastWindowClosed) exitNow()
            }
        }
    }

    /**
     * Exits the app (Application.Exit). The message loop ends, the UI thread returns, and
     * any later [invokeLater] call throws [IllegalStateException]. Callable from any thread.
     */
    fun exit() {
        invokeLater { exitNow() }
    }

    /** Calls Application.Exit. Must be called on the UI thread. */
    private fun exitNow() {
        currentApp?.call(XamlInterop.IApplication_Exit)
    }

    /**
     * Does at the ABI level what C#'s `class App : Application { override OnLaunched(...) }`
     * does: passes the Kotlin-implemented outer (IApplicationOverrides +
     * IXamlMetadataProvider) to IApplicationFactory.CreateInstance and composes it with
     * the XAML Application via aggregation.
     */
    @Suppress("LongMethod") // Keeps the ABI steps for aggregating the Application together in one place, on purpose
    private fun createApplication(onReady: () -> Unit) {
        // Forwards all methods to the real provider (created lazily) that resolves XAML types for WinUI controls
        val realProvider: ComPtr by lazy {
            Activation.activate(XamlInterop.CLS_XamlControlsXamlMetaDataProvider)
                .queryInterface(XamlInterop.IID_IXamlMetadataProvider)
        }

        val outer = KComObject("WinUI4K.App")
        outer.addInterface(
            XamlInterop.IID_IApplicationOverrides,
            listOf(
                KComObject.Method(DESC_THIS_PTR) {
                    // OnLaunched(this, LaunchActivatedEventArgs)
                    // Application.Resources cannot be touched until core initialization
                    // completes (= from OnLaunched onward); it's E_UNEXPECTED during the
                    // init callback
                    Dispatcher.capture() // capture the UI thread's DispatcherQueue here
                    installControlStyles()
                    onReady()
                    KComObject.S_OK
                },
            ),
        )
        outer.addInterface(
            XamlInterop.IID_IXamlMetadataProvider,
            listOf(
                KComObject.Method(DESC_GET_XAML_TYPE) { args ->
                    realProvider.rawCall(
                        XamlInterop.IXamlMetadataProvider_GetXamlType,
                        DESC_GET_XAML_TYPE,
                        args[1] as StructValue,
                        args[2] as Ptr,
                    )
                },
                KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                    realProvider.rawCall(
                        XamlInterop.IXamlMetadataProvider_GetXamlTypeByFullName,
                        DESC_THIS_PTR_PTR,
                        args[1] as Ptr,
                        args[2] as Ptr,
                    )
                },
                KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                    realProvider.rawCall(
                        XamlInterop.IXamlMetadataProvider_GetXmlnsDefinitions,
                        DESC_THIS_PTR_PTR,
                        args[1] as Ptr,
                        args[2] as Ptr,
                    )
                },
            ),
        )

        // Aggregation composition of Application: passing outer returns inner (the base
        // implementation); from then on XAML calls OnLaunched and friends via outer
        val factory = Activation.factory(XamlInterop.CLS_Application, XamlInterop.IID_IApplicationFactory)
        val app = Ffi.backend.withScope { scope ->
            val inner = scope.allocate(8)
            val instance = scope.allocate(8)
            factory.call(XamlInterop.IApplicationFactory_CreateInstance, outer.primary, inner, instance)
            outer.innerUnknown = ComPtr(Ffi.backend.memory.getPtr(inner, 0))
            ComPtr(Ffi.backend.memory.getPtr(instance, 0)) // IApplication
        }
        factory.release()
        currentApp = app

        // Don't let the message loop end when the last window closes (OnExplicitShutdown).
        // Exit instead happens via noteWindowClosed's deferred check (default) or exit()'s
        // call to Application.Exit
        val app3 = app.queryInterface(XamlInterop.IID_IApplication3)
        app3.call(
            XamlInterop.IApplication3_put_DispatcherShutdownMode,
            XamlInterop.DispatcherShutdownMode_OnExplicitShutdown,
        )
        app3.release()

        // An unpackaged (non-templated) app can't discover the default theme resources
        // (ms-appx:///Microsoft.UI.Xaml/Themes/themeresources.xaml) on its own via XAML,
        // since there's no resources.pri next to the exe (= java.exe). We handle the
        // ResourceManagerRequested event and hand back a custom ResourceManager pointing
        // at the WinAppSDK runtime package's own resources.pri.
        installFrameworkResourceManager(app)
    }

    /**
     * Wires the default control styles (generic.xaml) into Application.Resources.
     * Without this, controls like Button don't render. Just like a template app's
     * App.xaml, this appends XamlControlsResources to Resources.MergedDictionaries.
     */
    private fun installControlStyles() {
        val app = checkNotNull(currentApp) { "Application has not been created yet" }
        val xcr = Activation.activate(XamlInterop.CLS_XamlControlsResources)
        val xcrDict = xcr.queryInterface(XamlInterop.IID_IResourceDictionary)
        val appResources = app.getPtr(XamlInterop.IApplication_get_Resources)
        val merged = appResources.getPtr(XamlInterop.IResourceDictionary_get_MergedDictionaries)
        merged.call(FoundationInterop.IVector_Append, xcrDict)
        merged.release()
        appResources.release()
        xcrDict.release()
        xcr.release()
    }

    /**
     * Registers resource [value] under key [key] in Application.Resources (ResourceDictionary.Insert).
     * XAML loaded via XamlReader can then reference it with `{StaticResource key}`.
     * Re-registering under the same key overwrites the previous value. Call this from the UI thread.
     */
    internal fun insertApplicationResource(key: String, value: Ptr) {
        val app = checkNotNull(currentApp) { "Application has not been created yet" }
        val appResources = app.getPtr(XamlInterop.IApplication_get_Resources)
        val map = appResources.queryInterface(FoundationInterop.IID_IMap_Object_Object)
        appResources.release()
        val boxedKey = PropertyValues.boxString(key)
        Ffi.backend.withScope { scope ->
            val replaced = scope.allocate(1, 1)
            map.call(FoundationInterop.IMap_Insert, boxedKey.ptr, value, replaced)
        }
        boxedKey.release()
        map.release()
    }

    /**
     * Looks up the resource under key [key] in Application.Resources (ResourceDictionary.Lookup).
     * MergedDictionaries (e.g. XamlControlsResources's AccentButtonStyle) are searched too.
     * Throws an HRESULT exception if not found. The caller must release the returned reference.
     */
    internal fun lookupApplicationResource(key: String): ComPtr {
        val app = checkNotNull(currentApp) { "The Application has not been created yet" }
        val appResources = app.getPtr(XamlInterop.IApplication_get_Resources)
        val map = appResources.queryInterface(FoundationInterop.IID_IMap_Object_Object)
        appResources.release()
        val boxedKey = PropertyValues.boxString(key)
        return try {
            map.getPtr(FoundationInterop.IMap_Lookup, boxedKey.ptr)
        } finally {
            boxedKey.release()
            map.release()
        }
    }

    /**
     * Registers a handler on Application.ResourceManagerRequested that returns an MRT
     * Core ResourceManager reading the runtime package's resources.pri.
     * Called the first time XAML looks up a resource (= when XamlControlsResources is created).
     */
    private fun installFrameworkResourceManager(app: ComPtr) {
        val handler = KComObject("WinUI4K.ResourceManagerRequestedHandler", inspectable = false)
            .addInterface(
                XamlInterop.IID_ResourceManagerRequestedHandler,
                listOf(
                    KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                        // Invoke(this, sender, args)
                        val eventArgs = ComPtr(args[2] as Ptr)
                            .queryInterface(XamlInterop.IID_IResourceManagerRequestedEventArgs)
                        val rm = createFrameworkResourceManager()
                        eventArgs.call(
                            XamlInterop.IResourceManagerRequestedEventArgs_put_CustomResourceManager,
                            rm,
                        )
                        eventArgs.release()
                        KComObject.S_OK
                    },
                ),
            )

        val app2 = app.queryInterface(XamlInterop.IID_IApplication2)
        Ffi.backend.withScope { scope ->
            val token = scope.allocate(8)
            app2.call(XamlInterop.IApplication2_add_ResourceManagerRequested, handler.primary, token)
        }
        app2.release()
    }

    /** A ResourceManager reading resources.pri from the runtime package's location (where Microsoft.ui.xaml.dll lives). */
    private fun createFrameworkResourceManager(): ComPtr {
        val xamlDll = Win32.moduleFilePath("Microsoft.ui.xaml.dll")
            ?: error("Microsoft.ui.xaml.dll is not loaded")
        val pri = File(xamlDll).resolveSibling("resources.pri")
        check(pri.isFile) { "could not find the runtime package's resources.pri: $pri" }

        val factory = Activation.factory(XamlInterop.CLS_ResourceManager, XamlInterop.IID_IResourceManagerFactory)
        val rm = Hstring.use(pri.absolutePath) { h -> factory.getPtr(6, h) } // CreateInstance(fileName)
        factory.release()
        return rm
    }
}
