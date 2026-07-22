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

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
