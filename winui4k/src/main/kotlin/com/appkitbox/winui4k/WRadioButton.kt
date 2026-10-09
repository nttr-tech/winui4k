package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JRadioButton-like: WinUI 3's RadioButton.
 * Instead of a ButtonGroup, [groupName] forms the exclusive group
 * (RadioButtons in the same parent are already in the same group even without a groupName).
 */
open class WRadioButton internal constructor(inspectable: ComPtr) : WToggleButton(inspectable) {
    @JvmOverloads
    constructor(text: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_RadioButton, XamlInterop.IID_IRadioButtonFactory), // Default interface = IRadioButton
    ) {
        if (text.isNotEmpty()) this.text = text
    }

    /** The IRadioButton view that has GroupName (QI'd because derived classes have a different default interface). */
    private val radioButton: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IRadioButton)) }

    /** The name of the exclusive group (RadioButton.GroupName). */
    var groupName: String
        get() = radioButton.getString(XamlInterop.IRadioButton_get_GroupName)
        set(value) = Hstring.use(value) { h -> radioButton.call(XamlInterop.IRadioButton_put_GroupName, h) }
}
