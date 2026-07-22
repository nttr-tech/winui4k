package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.Abi
import com.appkitbox.winui4k.internal.winui.XamlStructs

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
 * Microsoft.UI.Xaml.TextAlignment (horizontal text alignment).
 * Shared by TextBlock / TextBox / RichTextBlock.
 * Values extracted from the winmd (Center=0, Left=1, Right=2, Justify=3, DetectFromContent=4).
 */
enum class TextAlignment(internal val native: Int) {
    /** Centered. */
    CENTER(0),

    /** Left-aligned (default). */
    LEFT(1),

    /** Right-aligned. */
    RIGHT(2),

    /** Justified. */
    JUSTIFY(3),

    /** Detected automatically from the content. */
    DETECT_FROM_CONTENT(4),
    ;

    internal companion object {
        fun of(native: Int): TextAlignment = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.TextTrimming (how text that doesn't fit is trimmed).
 * Shared by TextBlock / RichTextBlock.
 * Values extracted from the winmd (None=0, CharacterEllipsis=1, WordEllipsis=2, Clip=3).
 */
enum class TextTrimming(internal val native: Int) {
    /** Don't trim (default). */
    NONE(0),

    /** Trim character-by-character and append an ellipsis ("…"). */
    CHARACTER_ELLIPSIS(1),

    /** Trim word-by-word and append an ellipsis ("…"). */
    WORD_ELLIPSIS(2),

    /** Clip to the display area. */
    CLIP(3),
    ;

    internal companion object {
        fun of(native: Int): TextTrimming = entries.first { it.native == native }
    }
}

/**
 * JLabel-like: WinUI 3's TextBlock.
 * TextBlock is not a Control but a direct FrameworkElement, so it derives from [WComponent].
 */
class WLabel(text: String = "") : WComponent(
    Activation.activate(Abi.CLS_TextBlock, Abi.IID_ITextBlock),
) {
    var text: String
        get() = inspectable.getString(Abi.ITextBlock_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.ITextBlock_put_Text, h) }

    /** Font size (TextBlock.FontSize). Used to emphasize headings and the like. */
    var fontSize: Double
        get() = inspectable.getDouble(Abi.ITextBlock_get_FontSize)
        set(value) = inspectable.call(Abi.ITextBlock_put_FontSize, value)

    /** Font family name (TextBlock.FontFamily). Sets a family name such as "Yu Gothic UI". */
    var fontFamily: String
        get() {
            val family = inspectable.getPtrOrNull(Abi.ITextBlock_get_FontFamily) ?: return ""
            return try {
                family.getString(Abi.IFontFamily_get_Source)
            } finally {
                family.release()
            }
        }
        set(value) {
            val family = createFontFamily(value)
            inspectable.call(Abi.ITextBlock_put_FontFamily, family.ptr)
            family.release()
        }

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

    /** How text that doesn't fit is trimmed (TextBlock.TextTrimming). */
    var textTrimming: TextTrimming
        get() = TextTrimming.of(inspectable.getInt(Abi.ITextBlock_get_TextTrimming))
        set(value) = inspectable.call(Abi.ITextBlock_put_TextTrimming, value.native)

    /** Text alignment (TextBlock.TextAlignment). */
    var textAlignment: TextAlignment
        get() = TextAlignment.of(inspectable.getInt(Abi.ITextBlock_get_TextAlignment))
        set(value) = inspectable.call(Abi.ITextBlock_put_TextAlignment, value.native)

    /** Whether text can be selected with the mouse (TextBlock.IsTextSelectionEnabled). */
    var isTextSelectionEnabled: Boolean
        get() = inspectable.getBool(Abi.ITextBlock_get_IsTextSelectionEnabled)
        set(value) = inspectable.putBool(Abi.ITextBlock_put_IsTextSelectionEnabled, value)

    init {
        if (text.isNotEmpty()) this.text = text
    }
}

/**
 * Creates a Media.FontFamily from a family name.
 * Calls the composable factory's CreateInstanceWithName(HSTRING, outer, out inner, out instance)
 * with outer = NULL (same convention as [Activation.composeDefault]).
 */
private fun createFontFamily(name: String): ComPtr = Ffi.backend.withScope { scope ->
    val factory = Activation.factory(Abi.CLS_FontFamily, Abi.IID_IFontFamilyFactory)
    val inner = scope.allocate(8)
    val instance = scope.allocate(8)
    Hstring.use(name) { h ->
        factory.call(Abi.IFontFamilyFactory_CreateInstanceWithName, h, null, inner, instance)
    }
    factory.release()
    ComPtr(Ffi.backend.memory.getPtr(instance, 0))
}
