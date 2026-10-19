package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JButton-like with a drop-down arrow: WinUI 3's DropDownButton (a Button subclass).
 * Opens [flyout] on click. The arrow (chevron) is drawn automatically by WinUI.
 *
 * Since it extends [WButton], [flyout], [icon], [action], [background] / [foreground] and [isBorderPainted] work the same
 * as in WButton. The DropDownButton template paints the background and text with the same lightweight styling keys as
 * Button (ButtonBackground* / ButtonForeground* and so on), so colors are applied the same way as in WButton. Only the
 * following differ.
 * - The chevron color is determined by DropDownButtonForegroundSecondary* and does not follow [foreground] (it stays at
 *   the theme default)
 * - [isAccent], the default button ([WFrame.defaultButton]) and [isContentAreaFilled] = false do not apply the built-in
 *   styles (AccentButtonStyle / SubtleButtonStyle), because both would replace the template with Button's and remove the
 *   chevron. [isContentAreaFilled] = false only stops [background] from being painted
 */
class WDropDownButton @JvmOverloads constructor(text: String = "") : WButton(
    Activation.composeDefault(XamlInterop.CLS_DropDownButton, XamlInterop.IID_IDropDownButtonFactory),
) {
    override val usesButtonStyles: Boolean
        get() = false

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
