package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.Hstring
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi

/**
 * JLabel-like: WinUI 3's TextBlock.
 * TextBlock is not a Control but a direct FrameworkElement, so it derives from [WComponent].
 */
class WLabel(text: String = "") : WComponent(
    WinRt.activate(Abi.CLS_TextBlock).queryInterface(Abi.IID_ITextBlock),
) {
    var text: String
        get() = inspectable.getString(Abi.ITextBlock_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.ITextBlock_put_Text, h) }

    /** Font size (TextBlock.FontSize). Used to emphasize headings and the like. */
    var fontSize: Double
        get() = inspectable.getDouble(Abi.ITextBlock_get_FontSize)
        set(value) = inspectable.call(Abi.ITextBlock_put_FontSize, value)

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
