package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JCheckBox-like: WinUI 3's CheckBox.
 * All functionality comes from [WToggleButton] (ICheckBox itself has no members).
 * Setting [isThreeState] = true cycles through true → null (indeterminate) → false.
 */
class WCheckBox(text: String = "") : WToggleButton(
    Activation.composeDefault(XamlInterop.CLS_CheckBox, XamlInterop.IID_ICheckBoxFactory), // default interface = ICheckBox
) {
    init {
        if (text.isNotEmpty()) this.text = text
    }
}
