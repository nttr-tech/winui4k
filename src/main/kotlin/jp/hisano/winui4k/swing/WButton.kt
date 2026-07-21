package jp.hisano.winui4k.swing

import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.MemorySegment

/**
 * JButton-like: WinUI 3's Button.
 *
 * Covers the full surface Button defines (IButton + Primitives.IButtonBase +
 * ContentControl.Content): [text] / [content], [clickMode], [isPressed], [isPointerOver],
 * [addActionListener] / [removeActionListener] (Click), [command] / [commandParameter], [flyout].
 */
class WButton(text: String = "") : WButtonBase(
    WinRt.composeDefault(Abi.CLS_Button, Abi.IID_IButtonFactory), // default interface = IButton
) {
    /** The flyout opened by clicking the button (Button.Flyout). */
    var flyout: WFlyout? = null
        set(value) {
            field = value
            inspectable.call(Abi.IButton_put_Flyout, value?.flyoutBase?.ptr ?: MemorySegment.NULL)
        }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
