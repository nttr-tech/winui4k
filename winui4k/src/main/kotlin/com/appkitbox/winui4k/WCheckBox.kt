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
        alignTextWithBox()
        if (text.isNotEmpty()) this.text = text
    }

    /**
     * Even when the height grows, the text is lined up at the same height as the box (the same as Swing's JCheckBox). WinUI's template puts the box at the vertical center
     * and the text (ContentPresenter) at the top (VerticalContentAlignment = Top, the top 5 of the padding CheckBoxPadding = 8,5,0,0), so
     * when stretched to the row height only the text stays at the top. The text is centered vertically too, and the padding is 0 at the top and 2 at the bottom, so at the unstretched height (32)
     * the center of the text is 1 DIP above the center of the box, the same as the template's default. If the padding is changed with [setPadding] or [border],
     * that takes effect.
     */
    private fun alignTextWithBox() {
        verticalContentAlignment = VerticalAlignment.CENTER
        setPadding(TEXT_LEFT_PADDING, 0.0, 0.0, TEXT_BOTTOM_PADDING)
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

    /** The preferred size measured from the content. Slack is added to the width so the text does not wrap and get cut ([withWrapSlack]). */
    override fun readDesiredSize(): WSize = withWrapSlack(super.readDesiredSize())
}

/**
 * Adds the slack [WRAP_SLACK] to the width of [size], the measured preferred size of a CheckBox / RadioButton (unchanged if the width is 0 and not measured yet).
 *
 * The template's text (ContentPresenter) has TextWrapping = Wrap, and the measured width (DesiredSize) is rounded to physical pixel
 * boundaries by layout rounding and can be slightly narrower than the real width of the text (less than 1/3 DIP at 150% scale). Assigning that width as is makes the last
 * word go to a second line, which is invisible because the height is one line. The assigned width (Width) is rounded the same way, so at least one physical pixel is added.
 */
internal fun withWrapSlack(size: WSize): WSize = if (size.width > 0.0) WSize(size.width + WRAP_SLACK, size.height) else size

/** The left padding of the CheckBox's text (the left of the template's CheckBoxPadding; DIP). */
private const val TEXT_LEFT_PADDING = 8.0

/** The bottom padding of the CheckBox's text (DIP). With the text centered vertically, it puts the center of the text 1 DIP above the center of the box. */
private const val TEXT_BOTTOM_PADDING = 2.0

/** The width [withWrapSlack] adds (DIP). At least one physical pixel at 100% scale or more. */
private const val WRAP_SLACK = 1.0

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
