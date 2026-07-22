package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * JButton-like with a drop-down arrow: WinUI 3's DropDownButton (a Button subclass).
 * Opens [flyout] on click. The arrow (chevron) is drawn automatically by WinUI.
 */
class WDropDownButton(text: String = "") : WButtonBase(
    Activation.composeDefault(Abi.CLS_DropDownButton, Abi.IID_IDropDownButtonFactory),
) {
    /** The IButton view holding Flyout (DropDownButton is a Button subclass). */
    private val button: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_IButton))
    }

    /** The flyout opened by clicking the button (Button.Flyout). */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            button.call(Abi.IButton_put_Flyout, value?.flyoutBase?.ptr)
        }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
