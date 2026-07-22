package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for Microsoft.UI.winmd (WinAppSDK).
 * Covers Microsoft.UI.Windowing (AppWindow / Presenters / AppWindowTitleBar / DisplayArea)
 * and Microsoft.UI.Dispatching (DispatcherQueue / DispatcherQueueTimer).
 *
 * Values are mechanically extracted with tools/dump_winmd.py. Not a single value is
 * handwritten or guessed.
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, the interface body starts
 * at 6 and follows the winmd's method declaration order.
 */
internal object WindowingInterop {
    // ---- Microsoft.UI.Dispatching.DispatcherQueue (Microsoft.UI.winmd) ----
    const val CLS_DispatcherQueue = "Microsoft.UI.Dispatching.DispatcherQueue"
    const val IID_IDispatcherQueueStatics = "cd3382ea-a455-5124-b63a-ca40d34ca23c"
    const val IDispatcherQueueStatics_GetForCurrentThread = 6 // GetForCurrentThread(out DispatcherQueue)
    const val IID_IDispatcherQueue = "f6ebf8fa-be1c-5bf6-a467-73da28738ae8"
    const val IDispatcherQueue_CreateTimer = 6         // CreateTimer(out DispatcherQueueTimer)
    const val IDispatcherQueue_TryEnqueue = 7          // TryEnqueue(DispatcherQueueHandler, out boolean)

    /** delegate Microsoft.UI.Dispatching.DispatcherQueueHandler — Invoke() is vtbl[3], no arguments */
    const val IID_DispatcherQueueHandler = "2e0872a9-4e29-5f14-b688-fb96d5f9d5f8"

    // ---- Microsoft.UI.Dispatching.DispatcherQueueTimer (Microsoft.UI.winmd) ----
    const val IID_IDispatcherQueueTimer = "ad4d63fd-88fe-541f-ac11-bf2dc1ed2ce5"
    const val IDispatcherQueueTimer_put_Interval = 7   // put_Interval(TimeSpan) — passed by value as an int64 in 100ns units
    const val IDispatcherQueueTimer_put_IsRepeating = 10 // put_IsRepeating(boolean)
    const val IDispatcherQueueTimer_Start = 11         // Start()
    const val IDispatcherQueueTimer_Stop = 12          // Stop()
    const val IDispatcherQueueTimer_add_Tick = 13      // add_Tick(TypedEventHandler<DispatcherQueueTimer, Object>, out token)

    /** Concrete IID of TypedEventHandler<DispatcherQueueTimer, Object> (computed at runtime). */
    val IID_DispatcherQueueTimerTickHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc(Microsoft.UI.Dispatching.DispatcherQueueTimer;{$IID_IDispatcherQueueTimer});" +
                "cinterface(IInspectable))",
        )
    }

    // ---- Microsoft.UI.Windowing.AppWindow (Microsoft.UI.winmd) ----
    // Not activated directly — obtained via IAppWindowStatics.Create (WFrame.appWindow / WAppWindow.create).
    const val CLS_AppWindow = "Microsoft.UI.Windowing.AppWindow"
    const val IID_IAppWindow = "cfa788b3-643b-5c5e-ad4e-321d48a82acd"
    const val IAppWindow_get_Id = 6                     // get_Id(out WindowId) — out of a struct (u8)
    const val IAppWindow_get_IsShownInSwitchers = 7
    const val IAppWindow_put_IsShownInSwitchers = 8
    const val IAppWindow_get_IsVisible = 9
    const val IAppWindow_get_OwnerWindowId = 10         // get_OwnerWindowId(out WindowId)
    const val IAppWindow_get_Position = 11              // get_Position(out PointInt32) — out of a struct
    const val IAppWindow_get_Presenter = 12             // get_Presenter(out AppWindowPresenter)
    const val IAppWindow_get_Size = 13                  // get_Size(out SizeInt32) — out of a struct
    const val IAppWindow_get_Title = 14
    const val IAppWindow_put_Title = 15
    const val IAppWindow_get_TitleBar = 16              // get_TitleBar(out AppWindowTitleBar)
    const val IAppWindow_Destroy = 17
    const val IAppWindow_Hide = 18
    const val IAppWindow_Move = 19                      // Move(PointInt32) — struct passed by value
    const val IAppWindow_MoveAndResize = 20             // MoveAndResize(RectInt32)
    const val IAppWindow_Resize = 22                    // Resize(SizeInt32) — struct passed by value
    const val IAppWindow_SetIcon = 23                   // SetIcon(string) — path to an .ico file
    const val IAppWindow_SetPresenter = 25              // SetPresenter(AppWindowPresenter)
    const val IAppWindow_SetPresenterKind = 26          // SetPresenter(AppWindowPresenterKind)
    const val IAppWindow_Show = 27                      // Show()
    const val IAppWindow_ShowWithActivation = 28        // Show(boolean activateWindow)

    const val IID_IAppWindow2 = "6cd41292-794c-5cac-8961-210d012c6ebc"
    const val IAppWindow2_get_ClientSize = 6            // get_ClientSize(out SizeInt32) — out of a struct
    const val IAppWindow2_ResizeClient = 10             // ResizeClient(SizeInt32) — struct passed by value

    const val IID_IAppWindowStatics = "3c315c24-d540-5d72-b518-b226b83627cb"
    const val IAppWindowStatics_Create = 8              // Create(AppWindowPresenter, WindowId) -> AppWindow (for modal windows)

    // ---- Microsoft.UI.Windowing.AppWindowPresenter (common base of the 3 presenter kinds) ----
    const val IID_IAppWindowPresenter = "bc3042c2-c6c6-5632-8989-ff0ec6d3b40d"
    const val IAppWindowPresenter_get_Kind = 6          // get_Kind(out AppWindowPresenterKind)

    // ---- Microsoft.UI.Windowing.OverlappedPresenter ----
    const val CLS_OverlappedPresenter = "Microsoft.UI.Windowing.OverlappedPresenter"
    const val IID_IOverlappedPresenter = "21693970-4f4c-5172-9e9d-682a2d174884"
    const val IOverlappedPresenter_get_HasBorder = 6
    const val IOverlappedPresenter_get_HasTitleBar = 7
    const val IOverlappedPresenter_get_IsAlwaysOnTop = 8
    const val IOverlappedPresenter_put_IsAlwaysOnTop = 9
    const val IOverlappedPresenter_get_IsMaximizable = 10
    const val IOverlappedPresenter_put_IsMaximizable = 11
    const val IOverlappedPresenter_get_IsMinimizable = 12
    const val IOverlappedPresenter_put_IsMinimizable = 13
    const val IOverlappedPresenter_get_IsModal = 14
    const val IOverlappedPresenter_put_IsModal = 15
    const val IOverlappedPresenter_get_IsResizable = 16
    const val IOverlappedPresenter_put_IsResizable = 17
    const val IOverlappedPresenter_get_State = 18       // get_State(out OverlappedPresenterState)
    const val IOverlappedPresenter_Maximize = 19
    const val IOverlappedPresenter_Minimize = 20
    const val IOverlappedPresenter_Restore = 21
    const val IOverlappedPresenter_SetBorderAndTitleBar = 22 // SetBorderAndTitleBar(boolean, boolean)

    const val IID_IOverlappedPresenter3 = "55d26138-4c38-57e7-a0c1-d467b774db8c"
    const val IOverlappedPresenter3_get_PreferredMinimumHeight = 6  // -> IReference<i4>, null = unset
    const val IOverlappedPresenter3_put_PreferredMinimumHeight = 7
    const val IOverlappedPresenter3_get_PreferredMinimumWidth = 8
    const val IOverlappedPresenter3_put_PreferredMinimumWidth = 9
    const val IOverlappedPresenter3_get_PreferredMaximumWidth = 10
    const val IOverlappedPresenter3_put_PreferredMaximumWidth = 11
    const val IOverlappedPresenter3_get_PreferredMaximumHeight = 12
    const val IOverlappedPresenter3_put_PreferredMaximumHeight = 13

    const val IID_IOverlappedPresenterStatics = "997225e4-7b00-5aee-a4be-d4068d1999e2"
    const val IOverlappedPresenterStatics_Create = 6
    const val IOverlappedPresenterStatics_CreateForDialog = 8

    // ---- Microsoft.UI.Windowing.FullScreenPresenter ----
    const val CLS_FullScreenPresenter = "Microsoft.UI.Windowing.FullScreenPresenter"
    const val IID_IFullScreenPresenterStatics = "2ec0d2c1-e086-55bb-a3b2-44942e231c67"
    const val IFullScreenPresenterStatics_Create = 6
    // IFullScreenPresenter (fa9141fd-b8dd-5da1-8b2b-7cdadb76f593) has no members

    // ---- Microsoft.UI.Windowing.CompactOverlayPresenter ----
    const val CLS_CompactOverlayPresenter = "Microsoft.UI.Windowing.CompactOverlayPresenter"
    const val IID_ICompactOverlayPresenter = "efeb0812-6fc7-5b7d-bd92-cc8f9a6454c9"
    const val ICompactOverlayPresenter_get_InitialSize = 6 // -> CompactOverlaySize
    const val ICompactOverlayPresenter_put_InitialSize = 7
    const val IID_ICompactOverlayPresenterStatics = "eab93186-4f6a-52f9-8c03-da57a1522f6e"
    const val ICompactOverlayPresenterStatics_Create = 6

    // ---- Microsoft.UI.Windowing.AppWindowTitleBar (obtained from AppWindow.TitleBar, cannot be created directly) ----
    const val IID_IAppWindowTitleBar = "5574efa2-c91c-5700-a363-539c71a7aaf4"

    // all 12 color properties are IReference<Windows.UI.Color> (nullable)
    const val IAppWindowTitleBar_get_BackgroundColor = 6
    const val IAppWindowTitleBar_put_BackgroundColor = 7
    const val IAppWindowTitleBar_get_ButtonBackgroundColor = 8
    const val IAppWindowTitleBar_put_ButtonBackgroundColor = 9
    const val IAppWindowTitleBar_get_ButtonForegroundColor = 10
    const val IAppWindowTitleBar_put_ButtonForegroundColor = 11
    const val IAppWindowTitleBar_get_ButtonHoverBackgroundColor = 12
    const val IAppWindowTitleBar_put_ButtonHoverBackgroundColor = 13
    const val IAppWindowTitleBar_get_ButtonHoverForegroundColor = 14
    const val IAppWindowTitleBar_put_ButtonHoverForegroundColor = 15
    const val IAppWindowTitleBar_get_ButtonInactiveBackgroundColor = 16
    const val IAppWindowTitleBar_put_ButtonInactiveBackgroundColor = 17
    const val IAppWindowTitleBar_get_ButtonInactiveForegroundColor = 18
    const val IAppWindowTitleBar_put_ButtonInactiveForegroundColor = 19
    const val IAppWindowTitleBar_get_ButtonPressedBackgroundColor = 20
    const val IAppWindowTitleBar_put_ButtonPressedBackgroundColor = 21
    const val IAppWindowTitleBar_get_ButtonPressedForegroundColor = 22
    const val IAppWindowTitleBar_put_ButtonPressedForegroundColor = 23
    const val IAppWindowTitleBar_get_ExtendsContentIntoTitleBar = 24
    const val IAppWindowTitleBar_put_ExtendsContentIntoTitleBar = 25
    const val IAppWindowTitleBar_get_ForegroundColor = 26
    const val IAppWindowTitleBar_put_ForegroundColor = 27
    const val IAppWindowTitleBar_get_Height = 28        // -> INT32 (px, read-only)
    const val IAppWindowTitleBar_get_InactiveBackgroundColor = 31
    const val IAppWindowTitleBar_put_InactiveBackgroundColor = 32
    const val IAppWindowTitleBar_get_InactiveForegroundColor = 33
    const val IAppWindowTitleBar_put_InactiveForegroundColor = 34
    const val IAppWindowTitleBar_get_LeftInset = 35     // -> INT32 (px, read-only)
    const val IAppWindowTitleBar_get_RightInset = 36    // -> INT32 (px, read-only)
    const val IAppWindowTitleBar_ResetToDefault = 37

    const val IID_IAppWindowTitleBar2 = "86faed38-748a-5b4b-9ccf-3ba0496c9041"
    const val IAppWindowTitleBar2_get_PreferredHeightOption = 6 // -> TitleBarHeightOption
    const val IAppWindowTitleBar2_put_PreferredHeightOption = 7

    const val IID_IAppWindowTitleBar3 = "07146e74-0410-5597-aba7-1af276d2ae07"
    const val IAppWindowTitleBar3_get_PreferredTheme = 6 // -> TitleBarTheme
    const val IAppWindowTitleBar3_put_PreferredTheme = 7

    // enum Microsoft.UI.Windowing.TitleBarHeightOption: Standard=0, Tall=1, Collapsed=2
    // enum Microsoft.UI.Windowing.TitleBarTheme: Legacy=0, UseDefaultAppMode=1, Light=2, Dark=3
    // enum Microsoft.UI.Windowing.OverlappedPresenterState: Maximized=0, Minimized=1, Restored=2
    // enum Microsoft.UI.Windowing.CompactOverlaySize: Small=0, Medium=1, Large=2
    // enum Microsoft.UI.Windowing.AppWindowPresenterKind: Default=0, CompactOverlay=1, FullScreen=2, Overlapped=3

    // ---- Microsoft.UI.Windowing.DisplayArea ----
    const val CLS_DisplayArea = "Microsoft.UI.Windowing.DisplayArea"
    const val IID_IDisplayArea = "5c7e0537-b621-5579-bcae-a84aa8746167"
    const val IDisplayArea_get_IsPrimary = 7
    const val IDisplayArea_get_OuterBounds = 8          // -> RectInt32 (out of a struct)
    const val IDisplayArea_get_WorkArea = 9             // -> RectInt32 (out of a struct)
    const val IID_IDisplayAreaStatics = "02ab4926-211e-5d49-8e4b-2af193daed09"
    const val IDisplayAreaStatics_get_Primary = 6
    const val IDisplayAreaStatics_GetFromWindowId = 9   // GetFromWindowId(WindowId, DisplayAreaFallback) -> DisplayArea
    const val DisplayAreaFallback_Nearest = 2           // enum Microsoft.UI.Windowing.DisplayAreaFallback.Nearest
}
