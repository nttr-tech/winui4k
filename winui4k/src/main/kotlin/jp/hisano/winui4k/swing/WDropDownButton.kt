package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winui.Abi

/**
 * JButton-like with a drop-down arrow: WinUI 3's DropDownButton (a Button subclass).
 * Opens [flyout] on click. The arrow (chevron) is drawn automatically by WinUI.
 */
class WDropDownButton(text: String = "") : WButtonBase(
    Activation.composeDefault(Abi.CLS_DropDownButton, Abi.IID_IDropDownButtonFactory),
) {
    /** The IButton view holding Flyout (DropDownButton is a Button subclass). */
    private val button: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IButton)
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
