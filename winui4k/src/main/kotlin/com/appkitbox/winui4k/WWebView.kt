package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.win32.Win32
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Async
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.WebView2Interop
import com.appkitbox.winui4k.internal.winui.WebView2Runtime
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * Microsoft.Web.WebView2.Core.CoreWebView2WebErrorStatus (the reason a navigation failed).
 * Values extracted from the winmd.
 */
enum class WebErrorStatus(internal val native: Int) {
    /** An unknown error. */
    UNKNOWN(0),

    /** The certificate's common name doesn't match. */
    CERTIFICATE_COMMON_NAME_IS_INCORRECT(1),

    /** The certificate has expired. */
    CERTIFICATE_EXPIRED(2),

    /** The client certificate has errors. */
    CLIENT_CERTIFICATE_CONTAINS_ERRORS(3),

    /** The certificate has been revoked. */
    CERTIFICATE_REVOKED(4),

    /** The certificate is invalid. */
    CERTIFICATE_IS_INVALID(5),

    /** The server is unreachable. */
    SERVER_UNREACHABLE(6),

    /** The request timed out. */
    TIMEOUT(7),

    /** The server's response was malformed. */
    ERROR_HTTP_INVALID_SERVER_RESPONSE(8),

    /** The connection was aborted. */
    CONNECTION_ABORTED(9),

    /** The connection was reset. */
    CONNECTION_RESET(10),

    /** Disconnected from the network. */
    DISCONNECTED(11),

    /** Unable to connect. */
    CANNOT_CONNECT(12),

    /** The host name couldn't be resolved. */
    HOST_NAME_NOT_RESOLVED(13),

    /** The operation was canceled (including a cancel from [WWebView.addNavigationStartingListener]). */
    OPERATION_CANCELED(14),

    /** The redirect failed. */
    REDIRECT_FAILED(15),

    /** An unexpected error. */
    UNEXPECTED_ERROR(16),

    /** Valid authentication credentials are required. */
    VALID_AUTHENTICATION_CREDENTIALS_REQUIRED(17),

    /** Valid proxy authentication is required. */
    VALID_PROXY_AUTHENTICATION_REQUIRED(18),
    ;

    internal companion object {
        fun of(native: Int): WebErrorStatus = entries.first { it.native == native }
    }
}

/**
 * JEditorPane-like: WinUI 3's WebView2 (a Microsoft Edge-based web browser control).
 * Set [source] to a URL, or pass HTML to [navigateToString], to display a page.
 * Displaying content requires the WebView2 Runtime to be installed (bundled with Windows 11).
 *
 * The browser process starts asynchronously and is implicitly kicked off by, e.g., setting
 * [source] (or explicitly via [ensureCoreWebView2]). Completion of initialization can be
 * subscribed to via [addCoreWebView2InitializedListener], and CoreWebView2-backed features
 * such as [documentTitle] and [postWebMessageAsJson] are only usable once initialization completes.
 */
class WWebView(source: String = "") : WComponent(
    Activation.composeDefault(WebView2Interop.CLS_WebView2, WebView2Interop.IID_IWebView2Factory),
) {
    private companion object {
        init {
            // The WinRT implementation DLL (Microsoft.Web.WebView2.Core.dll) isn't bundled with
            // the runtime package, so pre-load the one bundled in the JAR (see WebView2Runtime for details)
            WebView2Runtime.ensureLoaded()
            // For an unpackaged app, WebView2 defaults to creating its user data folder next to
            // the executable (java.exe), which isn't writable under Program Files and makes
            // CoreWebView2 initialization fail. Point it at a writable location, but only if unset
            if (System.getenv("WEBVIEW2_USER_DATA_FOLDER") == null) {
                val base = System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir")
                Win32.setEnvironmentVariable("WEBVIEW2_USER_DATA_FOLDER", "$base\\winui4k\\WebView2")
            }
        }
    }

    /** NavigationStarting event tokens registered via addNavigationStartingListener. */
    private val navigationStartingTokens = ListenerTokens<(String) -> Boolean>()

    /** NavigationCompleted event tokens registered via addNavigationCompletedListener. */
    private val navigationCompletedTokens = ListenerTokens<(Boolean, WebErrorStatus) -> Unit>()

    /** WebMessageReceived event tokens registered via addWebMessageReceivedListener. */
    private val webMessageReceivedTokens = ListenerTokens<(String) -> Unit>()

    /** CoreWebView2Initialized event tokens registered via addCoreWebView2InitializedListener. */
    private val coreWebView2InitializedTokens = ListenerTokens<(Int) -> Unit>()

    /**
     * The URI of the page to display (WebView2.Source). "" if unset.
     * Setting it starts a navigation (the first time, this also implicitly initializes CoreWebView2).
     * Setting "" is ignored (an empty string becomes a NULL HSTRING, which makes CreateUri fail).
     * A value that isn't a valid URI (e.g. missing a scheme) throws.
     * Created via IUriRuntimeClassFactory.CreateUri to build a Windows.Foundation.Uri and pass it in.
     */
    var source: String
        get() {
            val uri = inspectable.getPtrOrNull(WebView2Interop.IWebView2_get_Source) ?: return ""
            return try {
                uri.getString(FoundationInterop.IUriRuntimeClass_get_AbsoluteUri)
            } finally {
                uri.release()
            }
        }
        set(value) {
            if (value.isEmpty()) return
            val factory = Activation.factory(FoundationInterop.CLS_Uri, FoundationInterop.IID_IUriRuntimeClassFactory)
            val uri = Hstring.use(value) { h ->
                factory.getPtr(FoundationInterop.IUriRuntimeClassFactory_CreateUri, h)
            }
            factory.release()
            inspectable.call(WebView2Interop.IWebView2_put_Source, uri.ptr)
            uri.release()
        }

    /**
     * Whether CoreWebView2 has been initialized (whether WebView2.CoreWebView2 is non-null).
     * Lets you check upfront whether an API that's a no-op before initialization — [reload] /
     * [executeScript] / [postWebMessageAsJson], etc. — will actually take effect.
     */
    val isCoreWebView2Initialized: Boolean
        get() {
            val core = inspectable.getPtrOrNull(WebView2Interop.IWebView2_get_CoreWebView2) ?: return false
            core.release()
            return true
        }

    /** Whether there's back history (WebView2.CanGoBack). */
    val canGoBack: Boolean
        get() = inspectable.getBool(WebView2Interop.IWebView2_get_CanGoBack)

    /** Whether there's forward history (WebView2.CanGoForward). */
    val canGoForward: Boolean
        get() = inspectable.getBool(WebView2Interop.IWebView2_get_CanGoForward)

    /**
     * The background color shown before a page loads or through transparent areas
     * (WebView2.DefaultBackgroundColor). Only alpha 0 (transparent) or 255 (opaque) is valid.
     */
    var defaultBackgroundColor: WColor
        get() {
            val (a, r, g, b) = XamlStructs.getColor(inspectable, WebView2Interop.IWebView2_get_DefaultBackgroundColor)
            return WColor(r, g, b, a)
        }
        set(value) = XamlStructs.putColor(
            inspectable,
            WebView2Interop.IWebView2_put_DefaultBackgroundColor,
            value.alpha,
            value.red,
            value.green,
            value.blue,
        )

    /**
     * The page's title (CoreWebView2.DocumentTitle).
     * Returns "" before CoreWebView2 is initialized (no page shown yet).
     */
    val documentTitle: String
        get() = withCoreWebView2 { core ->
            core.getString(WebView2Interop.ICoreWebView2_get_DocumentTitle)
        } ?: ""

    /**
     * Reloads the current page (WebView2.Reload).
     * A no-op before CoreWebView2 is initialized (the native call would throw E_ILLEGAL_METHOD_CALL).
     */
    fun reload() {
        if (!isCoreWebView2Initialized) return
        inspectable.call(WebView2Interop.IWebView2_Reload)
    }

    /**
     * Goes back one entry in history (WebView2.GoBack).
     * A no-op when [canGoBack] is false, and before CoreWebView2 is initialized.
     */
    fun goBack() {
        if (!isCoreWebView2Initialized) return
        inspectable.call(WebView2Interop.IWebView2_GoBack)
    }

    /**
     * Goes forward one entry in history (WebView2.GoForward).
     * A no-op when [canGoForward] is false, and before CoreWebView2 is initialized.
     */
    fun goForward() {
        if (!isCoreWebView2Initialized) return
        inspectable.call(WebView2Interop.IWebView2_GoForward)
    }

    /**
     * Displays [html] as the page, as-is (WebView2.NavigateToString).
     * The native NavigateToString fails if called before CoreWebView2 is initialized
     * (unlike [source], it doesn't implicitly initialize), so if not yet initialized this
     * starts initialization and runs the navigation automatically once it completes.
     */
    fun navigateToString(html: String) {
        val core = inspectable.getPtrOrNull(WebView2Interop.IWebView2_get_CoreWebView2)
        if (core == null) {
            lateinit var navigateOnce: (Int) -> Unit
            navigateOnce = { exceptionHresult ->
                removeCoreWebView2InitializedListener(navigateOnce)
                if (exceptionHresult == 0) {
                    Hstring.use(html) { h ->
                        inspectable.call(WebView2Interop.IWebView2_NavigateToString, h)
                    }
                }
            }
            addCoreWebView2InitializedListener(navigateOnce)
            ensureCoreWebView2()
            return
        }
        core.release()
        Hstring.use(html) { h ->
            inspectable.call(WebView2Interop.IWebView2_NavigateToString, h)
        }
    }

    /**
     * Terminates the browser process and releases resources (WebView2.Close).
     * This control can't be used afterward (create a new WWebView to display content again).
     */
    fun close() {
        inspectable.call(WebView2Interop.IWebView2_Close)
    }

    /**
     * Explicitly starts CoreWebView2 initialization (WebView2.EnsureCoreWebView2Async).
     * Returns immediately without waiting for completion; subscribe to completion via
     * [addCoreWebView2InitializedListener]. (Setting [source] also implicitly initializes it,
     * so only call this if you want to use e.g. [postWebMessageAsJson] before navigating.)
     */
    fun ensureCoreWebView2() {
        inspectable.getPtr(WebView2Interop.IWebView2_EnsureCoreWebView2Async).release()
    }

    /**
     * Runs JavaScript on the current page (WebView2.ExecuteScriptAsync).
     * Returns immediately without waiting for completion; if [resultHandler] is given, the
     * result (the JSON representation of the last expression's value — quoted if it's a
     * string, or "null" if there's no value) is delivered on the UI thread.
     * Only usable once CoreWebView2 is initialized (a no-op before that, and [resultHandler]
     * won't be called either).
     */
    fun executeScript(script: String, resultHandler: ((String) -> Unit)? = null) {
        if (!isCoreWebView2Initialized) return
        val operation = Hstring.use(script) { h ->
            inspectable.getPtr(WebView2Interop.IWebView2_ExecuteScriptAsync, h)
        }
        if (resultHandler == null) {
            operation.release()
            return
        }
        Async.onStringResult(
            operation,
            WebView2Interop.IID_AsyncOperationCompletedHandler_String,
            "WebView2.ExecuteScriptAsync",
        ) { result ->
            // Dispatch from the completion-notification thread over to the UI thread before delivering it
            WinUiUtilities.invokeLater { resultHandler(result) }
        }
    }

    /**
     * Sends a JSON message to the page's JavaScript (CoreWebView2.PostWebMessageAsJson).
     * The page receives it via window.chrome.webview.addEventListener("message", ...).
     * Only usable once CoreWebView2 is initialized (a no-op before that).
     */
    fun postWebMessageAsJson(json: String) {
        withCoreWebView2 { core ->
            Hstring.use(json) { h ->
                core.call(WebView2Interop.ICoreWebView2_PostWebMessageAsJson, h)
            }
        }
    }

    /**
     * Sends a string message to the page's JavaScript (CoreWebView2.PostWebMessageAsString).
     * Only usable once CoreWebView2 is initialized (a no-op before that).
     */
    fun postWebMessageAsString(message: String) {
        withCoreWebView2 { core ->
            Hstring.use(message) { h ->
                core.call(WebView2Interop.ICoreWebView2_PostWebMessageAsString, h)
            }
        }
    }

    /**
     * Opens the developer tools window (CoreWebView2.OpenDevToolsWindow).
     * Only usable once CoreWebView2 is initialized (a no-op before that).
     */
    fun openDevToolsWindow() {
        withCoreWebView2 { core ->
            core.call(WebView2Interop.ICoreWebView2_OpenDevToolsWindow)
        }
    }

    /**
     * Subscribes to the start of a navigation (WebView2.NavigationStarting).
     * The listener receives the destination URI; returning false cancels the navigation
     * (a cancellation shows up as NavigationCompleted with [WebErrorStatus.OPERATION_CANCELED]).
     */
    fun addNavigationStartingListener(listener: (uri: String) -> Boolean) {
        val token = inspectable.addEventHandler(
            "WinUI4K.WebView2NavigationStartingHandler",
            WebView2Interop.IID_WebView2NavigationStartingHandler,
            WebView2Interop.IWebView2_add_NavigationStarting,
        ) { _, args ->
            // args is CoreWebView2NavigationStartingEventArgs. Read Uri, and set Cancel if false is returned
            val argsPtr = ComPtr(args)
            val uri = argsPtr.getString(WebView2Interop.ICoreWebView2NavigationStartingEventArgs_get_Uri)
            if (!listener(uri)) {
                argsPtr.putBool(WebView2Interop.ICoreWebView2NavigationStartingEventArgs_put_Cancel, true)
            }
        }
        navigationStartingTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addNavigationStartingListener]. */
    fun removeNavigationStartingListener(listener: (String) -> Boolean) {
        val token = navigationStartingTokens.remove(listener) ?: return
        inspectable.removeEventHandler(WebView2Interop.IWebView2_remove_NavigationStarting, token)
    }

    /**
     * Subscribes to the completion of a navigation (WebView2.NavigationCompleted).
     * The listener receives success/failure and, on failure, the reason (on success,
     * [WebErrorStatus.UNKNOWN]).
     */
    fun addNavigationCompletedListener(listener: (isSuccess: Boolean, status: WebErrorStatus) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.WebView2NavigationCompletedHandler",
            WebView2Interop.IID_WebView2NavigationCompletedHandler,
            WebView2Interop.IWebView2_add_NavigationCompleted,
        ) { _, args ->
            // args is CoreWebView2NavigationCompletedEventArgs. Read IsSuccess and WebErrorStatus and deliver them
            val argsPtr = ComPtr(args)
            listener(
                argsPtr.getBool(WebView2Interop.ICoreWebView2NavigationCompletedEventArgs_get_IsSuccess),
                WebErrorStatus.of(
                    argsPtr.getInt(WebView2Interop.ICoreWebView2NavigationCompletedEventArgs_get_WebErrorStatus),
                ),
            )
        }
        navigationCompletedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addNavigationCompletedListener]. */
    fun removeNavigationCompletedListener(listener: (Boolean, WebErrorStatus) -> Unit) {
        val token = navigationCompletedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(WebView2Interop.IWebView2_remove_NavigationCompleted, token)
    }

    /**
     * Subscribes to messages from the page's JavaScript (WebView2.WebMessageReceived).
     * The page sends them via window.chrome.webview.postMessage(...).
     * The listener receives the JSON representation of the message (quoted if a string was sent).
     */
    fun addWebMessageReceivedListener(listener: (messageAsJson: String) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.WebView2WebMessageReceivedHandler",
            WebView2Interop.IID_WebView2WebMessageReceivedHandler,
            WebView2Interop.IWebView2_add_WebMessageReceived,
        ) { _, args ->
            // args is CoreWebView2WebMessageReceivedEventArgs. Read WebMessageAsJson and deliver it
            listener(
                ComPtr(args).getString(WebView2Interop.ICoreWebView2WebMessageReceivedEventArgs_get_WebMessageAsJson),
            )
        }
        webMessageReceivedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addWebMessageReceivedListener]. */
    fun removeWebMessageReceivedListener(listener: (String) -> Unit) {
        val token = webMessageReceivedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(WebView2Interop.IWebView2_remove_WebMessageReceived, token)
    }

    /**
     * Subscribes to CoreWebView2 initialization completing (WebView2.CoreWebView2Initialized).
     * The listener receives the initialization result's HRESULT (0 on success; typical failure
     * causes are the WebView2 Runtime not being installed, or the user data folder not being writable).
     * Once this succeeds, CoreWebView2-backed features like [documentTitle] and [postWebMessageAsJson] become usable.
     */
    fun addCoreWebView2InitializedListener(listener: (exceptionHresult: Int) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.WebView2CoreWebView2InitializedHandler",
            WebView2Interop.IID_WebView2CoreWebView2InitializedHandler,
            WebView2Interop.IWebView2_add_CoreWebView2Initialized,
        ) { _, args ->
            // args is CoreWebView2InitializedEventArgs. Read Exception (Windows.Foundation.HResult) and deliver it
            listener(ComPtr(args).getInt(WebView2Interop.ICoreWebView2InitializedEventArgs_get_Exception))
        }
        coreWebView2InitializedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCoreWebView2InitializedListener]. */
    fun removeCoreWebView2InitializedListener(listener: (Int) -> Unit) {
        val token = coreWebView2InitializedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(WebView2Interop.IWebView2_remove_CoreWebView2Initialized, token)
    }

    /**
     * Gets the ICoreWebView2 view and runs [block].
     * Does nothing and returns null if CoreWebView2 isn't initialized yet (get_CoreWebView2 is null).
     */
    private fun <T> withCoreWebView2(block: (ComPtr) -> T): T? {
        // get_CoreWebView2's return value is CoreWebView2's default interface (ICoreWebView2), so it can be called directly
        val core = inspectable.getPtrOrNull(WebView2Interop.IWebView2_get_CoreWebView2) ?: return null
        return try {
            block(core)
        } finally {
            core.release()
        }
    }

    init {
        if (source.isNotEmpty()) {
            // Calling put_Source directly here would kick off CoreWebView2's implicit
            // initialization, and if environment creation fails synchronously, the resulting
            // CoreWebView2Initialized would fire before the caller has registered a listener.
            // Defer applying it to a later message so the caller can register listeners after construction
            WinUiUtilities.invokeLater { this.source = source }
        }
    }
}
