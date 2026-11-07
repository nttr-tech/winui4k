package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.concurrent.atomic.AtomicLong

/**
 * JRadioButton-like: WinUI 3's RadioButton.
 *
 * As in Swing, radio buttons not in a [WButtonGroup] can be selected independently (they do not deselect each other even in the same parent).
 * WinUI's RadioButton is automatically exclusive within the same parent when GroupName is empty, so a button whose [groupName] is empty gets a unique
 * GroupName per button on the native side to make it independent. To make them exclusive, put them into a [WButtonGroup] (as in Swing). As in WinUI,
 * giving the same name to [groupName] also makes them exclusive.
 */
open class WRadioButton internal constructor(inspectable: ComPtr) : WToggleButton(inspectable) {
    @JvmOverloads
    constructor(text: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_RadioButton, XamlInterop.IID_IRadioButtonFactory), // Default interface = IRadioButton
    ) {
        groupName = ""
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

    /**
     * The GroupName of this button only. Used so that a button without a name ([groupName] is empty) is not exclusive with other buttons
     * (because WinUI makes radio buttons with an empty GroupName exclusive within the same parent).
     */
    private val standaloneGroupName by lazy { "WinUI4K.RadioButton#" + standaloneGroupCounter.incrementAndGet() }

    /**
     * The name of the exclusive group (RadioButton.GroupName). Radio buttons with the same name are exclusive within the same root of the visual tree.
     * The default is empty, and buttons with an empty name are not exclusive with other buttons (the same as buttons not in a Swing ButtonGroup; WinUI's default
     * "exclusive within the same parent" is not used). Putting it into a [WButtonGroup] changes it to that group's name, and removing it restores the original name.
     */
    var groupName: String
        get() = radioButton.getString(XamlInterop.IRadioButton_get_GroupName).takeUnless { it == standaloneGroupName } ?: ""
        set(value) = Hstring.use(value.ifEmpty { standaloneGroupName }) { h ->
            radioButton.call(XamlInterop.IRadioButton_put_GroupName, h)
        }

    override val stateColors: ToggleStateColors
        get() = RADIO_BUTTON_COLORS

    /** The preferred size measured from the content. Slack is added to the width so the text does not wrap and get cut ([withWrapSlack]; the template is built the same way as CheckBox). */
    override fun readDesiredSize(): WSize = withWrapSlack(super.readDesiredSize())

    /**
     * As in Swing, if this button is in a [WButtonGroup] and another button is selected, the initial focus goes to that button
     * (if the selected button cannot take focus, it stays on this button).
     */
    override fun focusAsInitial(): Boolean {
        val selected = buttonGroup?.selection as? WComponent ?: return false
        return selected !== this && selected.focusInitially()
    }
}

/** The counter for the unique GroupName that makes a [WRadioButton] independent. */
private val standaloneGroupCounter = AtomicLong()

/**
 * RadioButton keys (controls/dev/CommonStyles/RadioButton_themeresources.xaml). The background is
 * RadioButtonBackground{,PointerOver,Pressed,Disabled}, which paints RootGrid, and the foreground is
 * RadioButtonForeground{,PointerOver,Pressed}. The fill of the mark (RadioButtonOuterEllipse*, RadioButtonCheckGlyph*)
 * is not included.
 */
private val RADIO_BUTTON_COLORS = ToggleStateColors(
    background = StateColorKeys.uniform(listOf("", "PointerOver", "Pressed", "Disabled").map { "RadioButtonBackground$it" }),
    foreground = StateColorKeys.uniform(listOf("", "PointerOver", "Pressed").map { "RadioButtonForeground$it" }),
)
