package jp.hisano.winui4k.winui

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.Hstring
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.ffi.Native
import jp.hisano.winui4k.winrt.WinRt
import java.io.File
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG

/**
 * Toolkit responsible for launching a WinUI 3 application.
 *
 * Launch sequence:
 *  1. Declare Per-Monitor v2 DPI awareness
 *  2. Windows App SDK bootstrap (MddBootstrapInitialize2) — binds an installed WinAppSDK
 *     runtime package to this process as a dynamic reference
 *  3. RoInitialize(STA) — this thread becomes the UI thread
 *  4. Application.Start(callback) — inside the callback, composes an Application
 *     "subclass" via COM aggregation (WinRT composition) and builds the UI in OnLaunched
 *  5. Start returns once the last window closes, and the bootstrap is then released
 */
object WinUiToolkit {

    /**
     * Windows App SDK 2.x (majorMinorVersion = 0x00020000 for MddBootstrapInitialize2).
     * Since release 2.0, the minor part is ignored and resolution is based on major
     * only, so the desired version is specified via [WINAPPSDK_MIN_VERSION] (minVersion).
     */
    private const val WINAPPSDK_MAJOR_MINOR = 0x0002_0000

    /** Minimum runtime version 2.2.0.0 (PACKAGE_VERSION: Major<<48 | Minor<<32 | Build<<16 | Revision). */
    private const val WINAPPSDK_MIN_VERSION = 0x0002_0002_0000_0000L

    /** MddBootstrapInitializeOptions_OnNoMatch_ShowUI: prompts the user to install the runtime if it's missing. */
    private const val BOOTSTRAP_ON_NO_MATCH_SHOW_UI = 0x08

    private val DESC_THIS_PTR = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)

    /** Windows.UI.Xaml.Interop.TypeName { HSTRING Name; TypeKind Kind; } — passed by value. */
    private val TYPE_NAME_LAYOUT: MemoryLayout = MemoryLayout.structLayout(
        ADDRESS.withName("Name"),
        JAVA_INT.withName("Kind"),
        MemoryLayout.paddingLayout(4),
    )

    /** GetXamlType(this, TypeName byval, out IXamlType) */
    private val DESC_GET_XAML_TYPE =
        FunctionDescriptor.of(JAVA_INT, ADDRESS, TYPE_NAME_LAYOUT, ADDRESS)

    /** GetXmlnsDefinitions(this, out UINT32, out XmlnsDefinition*) / GetXamlType(this, HSTRING, out) */
    private val DESC_THIS_PTR_PTR = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)

    private var currentApp: ComPtr? = null

    /**
     * Launches WinUI 3 and runs [onLaunched] on the UI thread.
     * This method blocks until the application exits (the last window closes).
     */
    fun launch(onLaunched: () -> Unit) {
        Native.enablePerMonitorDpiAwareness()
        bootstrapInitialize()
        try {
            Native.roInitializeSta()

            // Application.Start(callback) — the callback is invoked in the UI thread context set up by XAML
            val initCallback = KComObject("WinUI4K.InitCallback", inspectable = false)
                .addInterface(
                    Abi.IID_ApplicationInitializationCallback,
                    listOf(
                        KComObject.Method(DESC_THIS_PTR) { // Invoke(this, params)
                            createApplication(onLaunched)
                            KComObject.S_OK
                        },
                    ),
                )

            val statics = WinRt.factory(Abi.CLS_Application, Abi.IID_IApplicationStatics)
            statics.call(Abi.IApplicationStatics_Start, initCallback.primary) // the message loop runs here
            statics.release()
        } finally {
            Native.roUninitialize()
            bootstrapShutdown()
        }
    }

    /**
     * Does at the ABI level what C#'s `class App : Application { override OnLaunched(...) }`
     * does: passes the Kotlin-implemented outer (IApplicationOverrides +
     * IXamlMetadataProvider) to IApplicationFactory.CreateInstance and composes it with
     * the XAML Application via aggregation.
     */
    private fun createApplication(onLaunched: () -> Unit) {
        // Forwards all methods to the real provider (created lazily) that resolves XAML types for WinUI controls
        val realProvider: ComPtr by lazy {
            WinRt.activate(Abi.CLS_XamlControlsXamlMetaDataProvider)
                .queryInterface(Abi.IID_IXamlMetadataProvider)
        }

        val outer = KComObject("WinUI4K.App")
        outer.addInterface(
            Abi.IID_IApplicationOverrides,
            listOf(
                KComObject.Method(DESC_THIS_PTR) { // OnLaunched(this, LaunchActivatedEventArgs)
                    // Application.Resources cannot be touched until core initialization
                    // completes (= from OnLaunched onward); it's E_UNEXPECTED during the
                    // init callback
                    installControlStyles()
                    onLaunched()
                    KComObject.S_OK
                },
            ),
        )
        outer.addInterface(
            Abi.IID_IXamlMetadataProvider,
            listOf(
                KComObject.Method(DESC_GET_XAML_TYPE) { args ->
                    realProvider.rawCall(
                        Abi.IXamlMetadataProvider_GetXamlType, DESC_GET_XAML_TYPE,
                        args[1] as MemorySegment, args[2] as MemorySegment,
                    )
                },
                KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                    realProvider.rawCall(
                        Abi.IXamlMetadataProvider_GetXamlTypeByFullName, DESC_THIS_PTR_PTR,
                        args[1] as MemorySegment, args[2] as MemorySegment,
                    )
                },
                KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                    realProvider.rawCall(
                        Abi.IXamlMetadataProvider_GetXmlnsDefinitions, DESC_THIS_PTR_PTR,
                        args[1] as MemorySegment, args[2] as MemorySegment,
                    )
                },
            ),
        )

        // Aggregation composition of Application: passing outer returns inner (the base
        // implementation); from then on XAML calls OnLaunched and friends via outer
        val factory = WinRt.factory(Abi.CLS_Application, Abi.IID_IApplicationFactory)
        val app = Arena.ofConfined().use { a ->
            val inner = a.allocate(ADDRESS)
            val instance = a.allocate(ADDRESS)
            factory.call(Abi.IApplicationFactory_CreateInstance, outer.primary, inner, instance)
            outer.innerUnknown = ComPtr(inner.get(ADDRESS, 0))
            ComPtr(instance.get(ADDRESS, 0)) // IApplication
        }
        factory.release()
        currentApp = app

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
        val xcr = WinRt.activate(Abi.CLS_XamlControlsResources)
        val xcrDict = xcr.queryInterface(Abi.IID_IResourceDictionary)
        val appResources = app.getPtr(Abi.IApplication_get_Resources)
        val merged = appResources.getPtr(Abi.IResourceDictionary_get_MergedDictionaries)
        merged.call(Abi.IVector_Append, xcrDict.ptr)
        merged.release()
        appResources.release()
        xcrDict.release()
        xcr.release()
    }

    /**
     * Registers a handler on Application.ResourceManagerRequested that returns an MRT
     * Core ResourceManager reading the runtime package's resources.pri.
     * Called the first time XAML looks up a resource (= when XamlControlsResources is created).
     */
    private fun installFrameworkResourceManager(app: ComPtr) {
        val handler = KComObject("WinUI4K.ResourceManagerRequestedHandler", inspectable = false)
            .addInterface(
                Abi.IID_ResourceManagerRequestedHandler,
                listOf(
                    KComObject.Method(DESC_THIS_PTR_PTR) { args -> // Invoke(this, sender, args)
                        val eventArgs = ComPtr(args[2] as MemorySegment)
                            .queryInterface(Abi.IID_IResourceManagerRequestedEventArgs)
                        val rm = createFrameworkResourceManager()
                        eventArgs.call(
                            Abi.IResourceManagerRequestedEventArgs_put_CustomResourceManager, rm.ptr,
                        )
                        eventArgs.release()
                        KComObject.S_OK
                    },
                ),
            )

        val app2 = app.queryInterface(Abi.IID_IApplication2)
        Arena.ofConfined().use { a ->
            val token = a.allocate(JAVA_LONG)
            app2.call(Abi.IApplication2_add_ResourceManagerRequested, handler.primary, token)
        }
        app2.release()
    }

    /** A ResourceManager reading resources.pri from the runtime package's location (where Microsoft.ui.xaml.dll lives). */
    private fun createFrameworkResourceManager(): ComPtr {
        val xamlDll = Native.moduleFilePath("Microsoft.ui.xaml.dll")
            ?: error("Microsoft.ui.xaml.dll is not loaded")
        val pri = File(xamlDll).resolveSibling("resources.pri")
        check(pri.isFile) { "could not find the runtime package's resources.pri: $pri" }

        val factory = WinRt.factory(Abi.CLS_ResourceManager, Abi.IID_IResourceManagerFactory)
        val rm = Hstring.use(pri.absolutePath) { h -> factory.getPtr(6, h) } // CreateInstance(fileName)
        factory.release()
        return rm
    }

    // ------------------------------------------------------------------
    // Windows App SDK bootstrapper (Microsoft.WindowsAppRuntime.Bootstrap.dll)
    // ------------------------------------------------------------------

    private val bootstrapLookup by lazy {
        val explicit = System.getProperty("winui4k.bootstrap.dll")
        if (explicit != null) {
            Native.lookup(explicit)
        } else {
            // resolved from PATH / the current directory
            Native.lookup("Microsoft.WindowsAppRuntime.Bootstrap.dll")
        }
    }

    private fun bootstrapInitialize() {
        // HRESULT MddBootstrapInitialize2(UINT32 majorMinor, PCWSTR versionTag,
        //                                 PACKAGE_VERSION minVersion, MddBootstrapInitializeOptions options)
        val init = Native.downcall(
            bootstrapLookup, "MddBootstrapInitialize2",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT),
        )
        Arena.ofConfined().use { a ->
            val emptyTag = a.allocate(JAVA_CHAR) // L"" (the stable channel)
            val hr = init.invokeWithArguments(
                WINAPPSDK_MAJOR_MINOR, emptyTag, WINAPPSDK_MIN_VERSION, BOOTSTRAP_ON_NO_MATCH_SHOW_UI,
            ) as Int
            Native.checkHr(hr, "MddBootstrapInitialize2 (is the Windows App SDK 2.2 runtime installed?)")
        }
    }

    private fun bootstrapShutdown() {
        runCatching {
            Native.downcall(bootstrapLookup, "MddBootstrapShutdown", FunctionDescriptor.ofVoid())
                .invokeWithArguments()
        }
    }
}
