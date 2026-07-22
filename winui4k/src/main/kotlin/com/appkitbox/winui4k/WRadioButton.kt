package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JRadioButton-like: WinUI 3's RadioButton.
 * Instead of a ButtonGroup, [groupName] forms the exclusive group
 * (RadioButtons in the same parent are already in the same group even without a groupName).
 */
class WRadioButton(text: String = "") : WToggleButton(
    Activation.composeDefault(XamlInterop.CLS_RadioButton, XamlInterop.IID_IRadioButtonFactory), // default interface = IRadioButton
) {
    /** The name of the exclusive group (RadioButton.GroupName). */
    var groupName: String
        get() = inspectable.getString(XamlInterop.IRadioButton_get_GroupName)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IRadioButton_put_GroupName, h) }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
