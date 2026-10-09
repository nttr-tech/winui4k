package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JCheckBox-like: WinUI 3's CheckBox.
 * All functionality comes from [WToggleButton] (ICheckBox itself has no members).
 * Setting [isThreeState] = true cycles through true → null (indeterminate) → false.
 */
open class WCheckBox internal constructor(inspectable: ComPtr) : WToggleButton(inspectable) {
    @JvmOverloads
    constructor(text: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_CheckBox, XamlInterop.IID_ICheckBoxFactory), // Default interface = ICheckBox
    ) {
        if (text.isNotEmpty()) this.text = text
    }
}
