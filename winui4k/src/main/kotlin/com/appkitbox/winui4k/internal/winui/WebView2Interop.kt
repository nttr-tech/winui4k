package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for WebView2.
 * Covers Microsoft.UI.Xaml.Controls.WebView2 (Microsoft.UI.Xaml.winmd) and
 * Microsoft.Web.WebView2.Core (from Microsoft.Web.WebView2 1.0.3719.77's
 * Microsoft.Web.WebView2.Core.winmd).
 *
 * Values are mechanically extracted with tools/dump_winmd.py. Not a single value is
 * handwritten or guessed.
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, the interface body starts
 * at 6 and follows the winmd's method declaration order.
 */
internal object WebView2Interop {
    // ---- Microsoft.UI.Xaml.Controls.WebView2 ----
    const val CLS_WebView2 = "Microsoft.UI.Xaml.Controls.WebView2"
    const val IID_IWebView2Factory = "fb4ec2ce-3074-5c42-b655-64fb81fbd040" // composable factory
    const val IID_IWebView2 = "2b2c76c2-997c-5069-a8f0-9b84cd7e624b"
    const val IWebView2_get_CoreWebView2 = 6            // get_CoreWebView2(out CoreWebView2)
    const val IWebView2_EnsureCoreWebView2Async = 7     // EnsureCoreWebView2Async(out IAsyncAction)
    const val IWebView2_ExecuteScriptAsync = 8          // ExecuteScriptAsync(string, out IAsyncOperation<string>)
    const val IWebView2_get_Source = 9                  // get_Source(out Uri)
    const val IWebView2_put_Source = 10                 // put_Source(Uri)
    const val IWebView2_get_CanGoForward = 11           // get_CanGoForward(out boolean)
    const val IWebView2_get_CanGoBack = 13              // get_CanGoBack(out boolean)
    const val IWebView2_get_DefaultBackgroundColor = 15 // get_DefaultBackgroundColor(out Color)
    const val IWebView2_put_DefaultBackgroundColor = 16 // put_DefaultBackgroundColor(Color)
    const val IWebView2_Reload = 17                     // Reload()
    const val IWebView2_GoForward = 18                  // GoForward()
    const val IWebView2_GoBack = 19                     // GoBack()
    const val IWebView2_NavigateToString = 20           // NavigateToString(string)
    const val IWebView2_Close = 21                      // Close()
    const val IWebView2_add_NavigationCompleted = 22    // add_NavigationCompleted(TypedEventHandler, out token)
    const val IWebView2_remove_NavigationCompleted = 23 // remove_NavigationCompleted(token)
    const val IWebView2_add_WebMessageReceived = 24     // add_WebMessageReceived(TypedEventHandler, out token)
    const val IWebView2_remove_WebMessageReceived = 25  // remove_WebMessageReceived(token)
    const val IWebView2_add_NavigationStarting = 26     // add_NavigationStarting(TypedEventHandler, out token)
    const val IWebView2_remove_NavigationStarting = 27  // remove_NavigationStarting(token)
    const val IWebView2_add_CoreWebView2Initialized = 30    // add_CoreWebView2Initialized(TypedEventHandler, out token)
    const val IWebView2_remove_CoreWebView2Initialized = 31 // remove_CoreWebView2Initialized(token)
    const val IID_ICoreWebView2InitializedEventArgs = "ee59d277-8b2e-57ab-8631-91d27b12ebd9"
    const val ICoreWebView2InitializedEventArgs_get_Exception = 6 // get_Exception(out Windows.Foundation.HResult = i4)

    // ---- Microsoft.Web.WebView2.Core (from Microsoft.Web.WebView2 1.0.3719.77's Microsoft.Web.WebView2.Core.winmd) ----
    const val IID_ICoreWebView2 = "3a3f559a-e5e9-5338-bb67-4eb0504a4f14"
    const val ICoreWebView2_get_DocumentTitle = 11      // get_DocumentTitle(out string)
    const val ICoreWebView2_PostWebMessageAsJson = 52   // PostWebMessageAsJson(string)
    const val ICoreWebView2_PostWebMessageAsString = 53 // PostWebMessageAsString(string)
    const val ICoreWebView2_OpenDevToolsWindow = 61     // OpenDevToolsWindow()
    const val IID_ICoreWebView2NavigationCompletedEventArgs = "4865e238-036a-5664-95a3-447ec44cf498"
    const val ICoreWebView2NavigationCompletedEventArgs_get_IsSuccess = 6      // get_IsSuccess(out boolean)
    const val ICoreWebView2NavigationCompletedEventArgs_get_WebErrorStatus = 7 // get_WebErrorStatus(out CoreWebView2WebErrorStatus)
    const val IID_ICoreWebView2NavigationStartingEventArgs = "548d23d3-fea3-5616-bd05-ae08066c86d3"
    const val ICoreWebView2NavigationStartingEventArgs_get_Uri = 6     // get_Uri(out string)
    const val ICoreWebView2NavigationStartingEventArgs_put_Cancel = 11 // put_Cancel(boolean)
    const val IID_ICoreWebView2WebMessageReceivedEventArgs = "eb066159-b725-5d5b-adc8-f5d7b9290304"
    const val ICoreWebView2WebMessageReceivedEventArgs_get_Source = 6           // get_Source(out string)
    const val ICoreWebView2WebMessageReceivedEventArgs_get_WebMessageAsJson = 7 // get_WebMessageAsJson(out string)

    /** Concrete IID of TypedEventHandler<WebView2, CoreWebView2NavigationStartingEventArgs> (computed at runtime). */
    val IID_WebView2NavigationStartingHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc(Microsoft.UI.Xaml.Controls.WebView2;{$IID_IWebView2});" +
                "rc(Microsoft.Web.WebView2.Core.CoreWebView2NavigationStartingEventArgs;" +
                "{$IID_ICoreWebView2NavigationStartingEventArgs}))",
        )
    }

    /** Concrete IID of TypedEventHandler<WebView2, CoreWebView2NavigationCompletedEventArgs> (computed at runtime). */
    val IID_WebView2NavigationCompletedHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc(Microsoft.UI.Xaml.Controls.WebView2;{$IID_IWebView2});" +
                "rc(Microsoft.Web.WebView2.Core.CoreWebView2NavigationCompletedEventArgs;" +
                "{$IID_ICoreWebView2NavigationCompletedEventArgs}))",
        )
    }

    /** Concrete IID of TypedEventHandler<WebView2, CoreWebView2WebMessageReceivedEventArgs> (computed at runtime). */
    val IID_WebView2WebMessageReceivedHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc(Microsoft.UI.Xaml.Controls.WebView2;{$IID_IWebView2});" +
                "rc(Microsoft.Web.WebView2.Core.CoreWebView2WebMessageReceivedEventArgs;" +
                "{$IID_ICoreWebView2WebMessageReceivedEventArgs}))",
        )
    }

    /** Concrete IID of TypedEventHandler<WebView2, CoreWebView2InitializedEventArgs> (computed at runtime). */
    val IID_WebView2CoreWebView2InitializedHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc(Microsoft.UI.Xaml.Controls.WebView2;{$IID_IWebView2});" +
                "rc(Microsoft.UI.Xaml.Controls.CoreWebView2InitializedEventArgs;" +
                "{$IID_ICoreWebView2InitializedEventArgs}))",
        )
    }

    /** Concrete IID of AsyncOperationCompletedHandler<String> (computed at runtime). Used by ExecuteScriptAsync. */
    val IID_AsyncOperationCompletedHandler_String: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};string)")
    }
}
