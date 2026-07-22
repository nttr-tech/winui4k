package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for notifications and the taskbar.
 * Covers Microsoft.Windows.AppNotifications / BadgeNotifications (WinAppSDK) and
 * Windows.UI.StartScreen.JumpList (OS-side, UniversalApiContract.winmd).
 *
 * Values are mechanically extracted with tools/dump_winmd.py. Not a single value is
 * handwritten or guessed.
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, the interface body starts
 * at 6 and follows the winmd's method declaration order.
 */
internal object NotificationInterop {
    // ---- Microsoft.Windows.AppNotifications.AppNotificationManager ----
    // (Microsoft.Windows.AppNotifications.winmd — WinAppSDK Foundation 2.1.0)
    const val CLS_AppNotificationManager = "Microsoft.Windows.AppNotifications.AppNotificationManager"
    const val IID_IAppNotificationManagerStatics = "6cfc0d8d-84a3-5592-b4c6-e3e7e7c680e4"
    const val IAppNotificationManagerStatics_get_Default = 6 // get_Default(out AppNotificationManager)
    const val IID_IAppNotificationManagerStatics2 = "6eb42a35-e82f-5732-98f1-129705602f2e"
    const val IAppNotificationManagerStatics2_IsSupported = 6 // IsSupported(out boolean)
    const val IID_IAppNotificationManager = "55129688-b4bd-550b-ae6b-c24061954d91"
    const val IAppNotificationManager_Register = 6
    const val IAppNotificationManager_Unregister = 7
    const val IAppNotificationManager_UnregisterAll = 8
    const val IAppNotificationManager_add_NotificationInvoked = 9 // add(TypedEventHandler<Manager, ActivatedEventArgs>, out token)
    const val IAppNotificationManager_remove_NotificationInvoked = 10
    const val IAppNotificationManager_Show = 11        // Show(AppNotification)
    const val IAppNotificationManager_get_Setting = 14 // get_Setting(out AppNotificationSetting)

    // ---- Microsoft.Windows.AppNotifications.AppNotification ----
    const val CLS_AppNotification = "Microsoft.Windows.AppNotifications.AppNotification"
    const val IID_IAppNotification = "373a6917-4116-5657-936a-15f99afdd667"
    const val IAppNotification_get_Tag = 6
    const val IAppNotification_put_Tag = 7
    const val IAppNotification_get_Group = 8
    const val IAppNotification_put_Group = 9
    const val IAppNotification_get_Id = 10             // get_Id(out UINT32)
    const val IAppNotification_get_Payload = 11        // get_Payload(out HSTRING)

    const val IID_IAppNotificationActivatedEventArgs = "7a8afaf9-31cb-51d5-82be-db6bd5878b77"
    const val IAppNotificationActivatedEventArgs_get_Argument = 6 // get_Argument(out HSTRING)

    /** Concrete IID of TypedEventHandler<AppNotificationManager, AppNotificationActivatedEventArgs>. */
    val IID_NotificationInvokedHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc(Microsoft.Windows.AppNotifications.AppNotificationManager;" +
                "{$IID_IAppNotificationManager});" +
                "rc(Microsoft.Windows.AppNotifications.AppNotificationActivatedEventArgs;" +
                "{$IID_IAppNotificationActivatedEventArgs}))",
        )
    }

    // ---- Microsoft.Windows.AppNotifications.Builder.AppNotificationBuilder ----
    // (Microsoft.Windows.AppNotifications.Builder.winmd)
    const val CLS_AppNotificationBuilder =
        "Microsoft.Windows.AppNotifications.Builder.AppNotificationBuilder"
    const val IID_IAppNotificationBuilder = "e801d31f-ce03-505c-adec-8a02724ec9de"
    const val IAppNotificationBuilder_AddArgument = 6  // AddArgument(HSTRING, HSTRING, out this)
    const val IAppNotificationBuilder_SetDuration = 8  // SetDuration(AppNotificationDuration, out this)
    const val IAppNotificationBuilder_SetScenario = 9  // SetScenario(AppNotificationScenario, out this)
    const val IAppNotificationBuilder_AddText = 10     // AddText(HSTRING, out this)
    const val IAppNotificationBuilder_SetAttributionText = 12 // SetAttributionText(HSTRING, out this)
    const val IAppNotificationBuilder_AddButton = 29   // AddButton(AppNotificationButton, out this)
    const val IAppNotificationBuilder_BuildNotification = 32 // BuildNotification(out AppNotification)
    const val IAppNotificationBuilder_SetTag = 33      // SetTag(HSTRING, out this)
    const val IAppNotificationBuilder_SetGroup = 34    // SetGroup(HSTRING, out this)

    const val CLS_AppNotificationButton =
        "Microsoft.Windows.AppNotifications.Builder.AppNotificationButton"
    const val IID_IAppNotificationButtonFactory = "4f109286-0a6d-5a5e-9e8f-9fe31669fbb8"
    const val IAppNotificationButtonFactory_CreateInstance = 6 // CreateInstance(HSTRING content, out AppNotificationButton)
    const val IID_IAppNotificationButton = "a7c03031-5634-5098-aec9-47ecb60c3499"
    const val IAppNotificationButton_AddArgument = 24  // AddArgument(HSTRING, HSTRING, out this)

    // ---- Microsoft.Windows.BadgeNotifications.BadgeNotificationManager ----
    // (Microsoft.Windows.BadgeNotifications.winmd)
    const val CLS_BadgeNotificationManager =
        "Microsoft.Windows.BadgeNotifications.BadgeNotificationManager"
    const val IID_IBadgeNotificationManagerStatics = "a6e71616-7c9f-5d22-ad1c-f4ab874087b5"
    const val IBadgeNotificationManagerStatics_get_Current = 6 // get_Current(out BadgeNotificationManager)
    const val IID_IBadgeNotificationManager = "11cb6e8f-11ca-53f8-80f6-5330d44ba908"
    const val IBadgeNotificationManager_SetBadgeAsCount = 6 // SetBadgeAsCount(UINT32)
    const val IBadgeNotificationManager_SetBadgeAsGlyph = 7 // SetBadgeAsGlyph(BadgeNotificationGlyph)
    const val IBadgeNotificationManager_ClearBadge = 8

    // ---- Windows.UI.StartScreen.JumpList (UniversalApiContract.winmd — OS-side) ----
    const val CLS_JumpList = "Windows.UI.StartScreen.JumpList"
    const val IID_IJumpListStatics = "a7e0c681-e67e-4b74-8250-3f322c4d92c3"
    const val IJumpListStatics_LoadCurrentAsync = 6    // LoadCurrentAsync(out IAsyncOperation<JumpList>)
    const val IJumpListStatics_IsSupported = 7         // IsSupported(out boolean)
    const val IID_IJumpList = "b0234c3e-cd6f-4cb6-a611-61fd505f3ed1"
    const val IJumpList_get_Items = 6                  // get_Items(out IVector<JumpListItem>)
    const val IJumpList_get_SystemGroupKind = 7        // get_SystemGroupKind(out JumpListSystemGroupKind)
    const val IJumpList_put_SystemGroupKind = 8
    const val IJumpList_SaveAsync = 9                  // SaveAsync(out IAsyncAction)

    const val CLS_JumpListItem = "Windows.UI.StartScreen.JumpListItem"
    const val IID_IJumpListItemStatics = "f1bfc4e8-c7aa-49cb-8dde-ecfccd7ad7e4"
    const val IJumpListItemStatics_CreateWithArguments = 6 // CreateWithArguments(HSTRING args, HSTRING displayName, out JumpListItem)
    const val IJumpListItemStatics_CreateSeparator = 7 // CreateSeparator(out JumpListItem)
    const val IID_IJumpListItem = "7adb6717-8b5d-4820-995b-9b418dbe48b0"
    const val IJumpListItem_get_Kind = 6               // get_Kind(out JumpListItemKind)
    const val IJumpListItem_get_Arguments = 7
    const val IJumpListItem_get_RemovedByUser = 8
    const val IJumpListItem_get_Description = 9
    const val IJumpListItem_put_Description = 10
    const val IJumpListItem_get_DisplayName = 11
    const val IJumpListItem_put_DisplayName = 12
    const val IJumpListItem_get_GroupName = 13
    const val IJumpListItem_put_GroupName = 14

    /** Concrete IID of AsyncOperationCompletedHandler<JumpList> (LoadCurrentAsync's completion notification). */
    val IID_AsyncOperationCompletedHandler_JumpList: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_AsyncOperationCompletedHandler_OPEN}};" +
                "rc(Windows.UI.StartScreen.JumpList;{$IID_IJumpList}))",
        )
    }
}
