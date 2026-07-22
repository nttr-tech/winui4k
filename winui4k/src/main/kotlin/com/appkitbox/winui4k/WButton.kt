package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * JButton-like: WinUI 3's Button.
 *
 * Covers the full surface Button defines (IButton + Primitives.IButtonBase +
 * ContentControl.Content): [text] / [content], [clickMode], [isPressed], [isPointerOver],
 * [addActionListener] / [removeActionListener] (Click), [command] / [commandParameter], [flyout].
 */
class WButton(text: String = "") : WButtonBase(
    Activation.composeDefault(Abi.CLS_Button, Abi.IID_IButtonFactory), // default interface = IButton
) {
    /** The flyout opened by clicking the button (Button.Flyout). */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            inspectable.call(Abi.IButton_put_Flyout, value?.flyoutBase?.ptr)
        }

    /**
     * Whether to apply the built-in accent-colored style (AccentButtonStyle).
     * Gives the emphasized look of a dialog's default button. Setting it back to false restores the default style.
     */
    var isAccent: Boolean = false
        set(value) {
            field = value
            if (value) {
                val style = WinUiUtilities.lookupApplicationResource("AccentButtonStyle")
                frameworkElement.call(Abi.IFrameworkElement_put_Style, style.ptr)
                style.release()
            } else {
                frameworkElement.call(Abi.IFrameworkElement_put_Style, null)
            }
        }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
