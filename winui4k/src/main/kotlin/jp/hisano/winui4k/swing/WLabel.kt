package jp.hisano.winui4k.swing

import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.Hstring
import jp.hisano.winui4k.winrt.getString
import jp.hisano.winui4k.winui.Abi
import jp.hisano.winui4k.winui.XamlStructs

/**
 * Microsoft.UI.Xaml.TextWrapping (how text wraps).
 * Values extracted from the winmd (NoWrap=1, Wrap=2, WrapWholeWords=3).
 */
enum class TextWrapping(internal val native: Int) {
    /** Don't wrap (default). */
    NO_WRAP(1),

    /** Wrap to fit the available width. */
    WRAP(2),

    /** Don't break in the middle of a word. */
    WRAP_WHOLE_WORDS(3),
    ;

    internal companion object {
        fun of(native: Int): TextWrapping = entries.first { it.native == native }
    }
}

/**
 * JLabel-like: WinUI 3's TextBlock.
 * TextBlock is not a Control but a direct FrameworkElement, so it derives from [WComponent].
 */
class WLabel(text: String = "") : WComponent(
    Activation.activate(Abi.CLS_TextBlock).queryInterface(Abi.IID_ITextBlock),
) {
    var text: String
        get() = inspectable.getString(Abi.ITextBlock_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.ITextBlock_put_Text, h) }

    /** Font size (TextBlock.FontSize). Used to emphasize headings and the like. */
    var fontSize: Double
        get() = inspectable.getDouble(Abi.ITextBlock_get_FontSize)
        set(value) = inspectable.call(Abi.ITextBlock_put_FontSize, value)

    /** Font weight (TextBlock.FontWeight). 400=Normal, 600=SemiBold, 700=Bold. */
    var fontWeight: Int = 400
        set(value) {
            field = value
            XamlStructs.putFontWeight(inspectable, Abi.ITextBlock_put_FontWeight, value)
        }

    /** Text color (TextBlock.Foreground). Converted to a SolidColorBrush before being set. Null restores the default color. */
    var foreground: WColor? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(Abi.ITextBlock_put_Foreground, null)
            } else {
                val brush = value.createBrush()
                inspectable.call(Abi.ITextBlock_put_Foreground, brush.ptr)
                brush.release()
            }
        }

    /** How text wraps (TextBlock.TextWrapping). */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(Abi.ITextBlock_get_TextWrapping))
        set(value) = inspectable.call(Abi.ITextBlock_put_TextWrapping, value.native)

    init {
        if (text.isNotEmpty()) this.text = text
    }
}
