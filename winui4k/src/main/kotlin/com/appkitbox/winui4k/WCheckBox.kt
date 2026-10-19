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

    /** JCheckBox(String, boolean)-style: a button with the given text and selection state. */
    constructor(text: String, selected: Boolean) : this(text) {
        if (selected) isSelected = true
    }

    /** JCheckBox(Icon)-style: an icon-only button (the icon is placed to the right of the check mark; see [icon]). */
    constructor(icon: WIcon) : this("") {
        this.icon = icon
    }

    /** JCheckBox(String, Icon)-style: a button with text and an icon. Text only if [icon] is null. */
    constructor(text: String, icon: WIcon?) : this(text) {
        if (icon != null) this.icon = icon
    }

    /** JCheckBox(String, Icon, boolean)-style: a button with the given text, icon and selection state. Text only if [icon] is null. */
    constructor(text: String, icon: WIcon?, selected: Boolean) : this(text, icon) {
        if (selected) isSelected = true
    }

    /** JCheckBox(Action)-style: a button that takes the values of [action] (including the selection state of SELECTED_KEY) and calls the action on click. */
    constructor(action: WAction) : this("") {
        this.action = action
    }

    override val stateColors: ToggleStateColors
        get() = CHECK_BOX_COLORS
}

/**
 * CheckBox keys (controls/dev/CommonStyles/CheckBox_themeresources.xaml). The background is
 * CheckBoxBackground{Unchecked,Checked,Indeterminate}{,PointerOver,Pressed,Disabled}, which paints RootGrid, and the
 * foreground is CheckBoxForeground{Unchecked,Checked,Indeterminate}{,PointerOver,Pressed}. The check mark fill
 * (CheckBoxCheckBackgroundFill*) is not included.
 */
private val CHECK_BOX_COLORS = ToggleStateColors(
    background = StateColorKeys.uniform(checkBoxKeys("CheckBoxBackground", listOf("", "PointerOver", "Pressed", "Disabled"))),
    foreground = StateColorKeys.uniform(checkBoxKeys("CheckBoxForeground", listOf("", "PointerOver", "Pressed"))),
)

private fun checkBoxKeys(baseKey: String, states: List<String>): List<String> =
    listOf("Unchecked", "Checked", "Indeterminate").flatMap { check -> states.map { "$baseKey$check$it" } }
