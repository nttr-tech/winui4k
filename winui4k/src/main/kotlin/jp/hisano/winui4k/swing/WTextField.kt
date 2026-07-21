package jp.hisano.winui4k.swing

import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.Hstring
import jp.hisano.winui4k.winui.Abi

/** JTextField-like: WinUI 3's TextBox. */
class WTextField(placeholder: String = "") : WControl(
    Activation.composeDefault(Abi.CLS_TextBox, Abi.IID_ITextBoxFactory)
        .queryInterface(Abi.IID_ITextBox),
) {
    init {
        if (placeholder.isNotEmpty()) {
            Hstring.use(placeholder) { h -> inspectable.call(Abi.ITextBox_put_PlaceholderText, h) }
        }
    }

    var text: String
        get() = inspectable.getString(Abi.ITextBox_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.ITextBox_put_Text, h) }
}
