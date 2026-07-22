package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JButton-like with a drop-down arrow: WinUI 3's DropDownButton (a Button subclass).
 * Opens [flyout] on click. The arrow (chevron) is drawn automatically by WinUI.
 */
class WDropDownButton(text: String = "") : WButtonBase(
    Activation.composeDefault(XamlInterop.CLS_DropDownButton, XamlInterop.IID_IDropDownButtonFactory),
) {
    /** The IButton view holding Flyout (DropDownButton is a Button subclass). */
    private val button: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IButton))
    }

    /** The flyout opened by clicking the button (Button.Flyout). */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            button.call(XamlInterop.IButton_put_Flyout, value?.flyoutBase?.ptr)
        }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
