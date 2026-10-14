package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for Windows.UI.ViewManagement (OS-side,
 * Windows.Foundation.UniversalApiContract.winmd from the Windows SDK contracts package; not in Microsoft.UI.Xaml.winmd).
 *
 * Values are mechanically extracted with tools/dump_winmd.py. Not a single value is
 * handwritten or guessed.
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, the interface body starts
 * at 6 and follows the winmd's method declaration order.
 */
internal object ViewManagementInterop {
    // ---- Windows.UI.ViewManagement.UISettings ----
    const val CLS_UISettings = "Windows.UI.ViewManagement.UISettings"
    const val IID_IUISettings = "85361600-1c63-4627-bcb1-3a89e0bc9c55"
    const val IID_IUISettings3 = "03021be4-5254-4781-8194-5168f7d06d7b"
    const val IUISettings3_GetColorValue = 6           // GetColorValue(UIColorType, out Windows.UI.Color)
    const val IUISettings3_add_ColorValuesChanged = 7  // add_ColorValuesChanged(TypedEventHandler<UISettings, object>, out token)
    const val IUISettings3_remove_ColorValuesChanged = 8

    // ---- Windows.UI.ViewManagement.UIColorType (enum values) ----
    const val UIColorType_AccentDark3 = 2
    const val UIColorType_AccentDark2 = 3
    const val UIColorType_AccentDark1 = 4
    const val UIColorType_Accent = 5
    const val UIColorType_AccentLight1 = 6
    const val UIColorType_AccentLight2 = 7
    const val UIColorType_AccentLight3 = 8

    /** Handler for UISettings.ColorValuesChanged: TypedEventHandler<UISettings, Object> (computed at runtime). */
    val IID_ColorValuesChangedHandler: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};" +
                "rc($CLS_UISettings;{$IID_IUISettings});cinterface(IInspectable))",
        )
    }
}
