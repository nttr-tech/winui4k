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

    /** JRadioButton(String, boolean)-style: a button with the specified text and selection state. */
    constructor(text: String, selected: Boolean) : this(text) {
        if (selected) isSelected = true
    }

    /** JRadioButton(Icon)-style: a button with only an icon (the icon is placed to the right of the mark; see [icon]). */
    constructor(icon: WIcon) : this("") {
        this.icon = icon
    }

    /** JRadioButton(String, Icon)-style: a button with text and an icon. Text only if [icon] is null. */
    constructor(text: String, icon: WIcon?) : this(text) {
        if (icon != null) this.icon = icon
    }

    /** JRadioButton(String, Icon, boolean)-style: a button with the specified text, icon and selection state. Text only if [icon] is null. */
    constructor(text: String, icon: WIcon?, selected: Boolean) : this(text, icon) {
        if (selected) isSelected = true
    }

    /** JRadioButton(Action)-style: a button that copies the values of [action] (including the SELECTED_KEY selection state) and calls the action on click. */
    constructor(action: WAction) : this("") {
        this.action = action
    }

    /** The IRadioButton view that has GroupName (QI'd because derived classes have a different default interface). */
    private val radioButton: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IRadioButton)) }

    /** The name of the exclusive group (RadioButton.GroupName). */
    var groupName: String
        get() = radioButton.getString(XamlInterop.IRadioButton_get_GroupName)
        set(value) = Hstring.use(value) { h -> radioButton.call(XamlInterop.IRadioButton_put_GroupName, h) }
}
