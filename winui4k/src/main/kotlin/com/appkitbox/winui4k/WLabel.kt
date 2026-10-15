package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
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
        @JvmSynthetic
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
        @JvmSynthetic
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
        @JvmSynthetic
        fun of(native: Int): TextTrimming = entries.first { it.native == native }
    }
}

/**
 * JLabel-like: WinUI 3's TextBlock.
 * TextBlock is not a Control but a direct FrameworkElement, so it derives from [WComponent].
 */
class WLabel @JvmOverloads constructor(text: String = "") : WComponent(
    Activation.activate(XamlInterop.CLS_TextBlock, XamlInterop.IID_ITextBlock),
) {
    var text: String
        get() = inspectable.getString(XamlInterop.ITextBlock_get_Text)
        set(value) {
            if (value == text) return
            Hstring.use(value) { h -> inspectable.call(XamlInterop.ITextBlock_put_Text, h) }
            // Like JLabel.setText: the new text may need a different size
            revalidate()
        }

    /** Font size (TextBlock.FontSize). Used to emphasize headings and the like. */
    var fontSize: Double
        get() = inspectable.getDouble(XamlInterop.ITextBlock_get_FontSize)
        set(value) {
            inspectable.call(XamlInterop.ITextBlock_put_FontSize, value)
            revalidate()
        }

    /** Font family name (TextBlock.FontFamily). Sets a family name such as "Yu Gothic UI". */
    var fontFamily: String
        get() = getFontFamily(inspectable, XamlInterop.ITextBlock_get_FontFamily)
        set(value) {
            putFontFamily(inspectable, XamlInterop.ITextBlock_put_FontFamily, value)
            revalidate()
        }

    /** Font weight (TextBlock.FontWeight). 400=Normal, 600=SemiBold, 700=Bold. */
    var fontWeight: Int = 400
        set(value) {
            field = value
            XamlStructs.putFontWeight(inspectable, XamlInterop.ITextBlock_put_FontWeight, value)
            revalidate()
        }

    /** Whether the text is italic (TextBlock.FontStyle is Italic). */
    var isItalic: Boolean
        get() = inspectable.getInt(XamlInterop.ITextBlock_get_FontStyle) == FONT_STYLE_ITALIC
        set(value) = inspectable.call(XamlInterop.ITextBlock_put_FontStyle, if (value) FONT_STYLE_ITALIC else FONT_STYLE_NORMAL)

    /** Whether the text is underlined (Underline in TextBlock.TextDecorations). */
    var isUnderline: Boolean
        get() = hasDecoration(DECORATION_UNDERLINE)
        set(value) = setDecoration(DECORATION_UNDERLINE, value)

    /** Whether the text is struck through (Strikethrough in TextBlock.TextDecorations). */
    var isStrikethrough: Boolean
        get() = hasDecoration(DECORATION_STRIKETHROUGH)
        set(value) = setDecoration(DECORATION_STRIKETHROUGH, value)

    private fun hasDecoration(flag: Int): Boolean = inspectable.getInt(XamlInterop.ITextBlock_get_TextDecorations) and flag != 0

    private fun setDecoration(flag: Int, on: Boolean) {
        val current = inspectable.getInt(XamlInterop.ITextBlock_get_TextDecorations)
        inspectable.call(XamlInterop.ITextBlock_put_TextDecorations, if (on) current or flag else current and flag.inv())
    }

    /**
     * Text color (TextBlock.Foreground). Converted to a SolidColorBrush before being set. Null clears the local value,
     * so the text gets the inherited (theme) color again.
     */
    var foreground: WColor? = null
        set(value) {
            field = value
            if (value == null) {
                clearValue(XamlInterop.CLS_TextBlock, XamlInterop.IID_ITextBlockStatics, XamlInterop.ITextBlockStatics_get_ForegroundProperty)
            } else {
                val brush = value.createBrush()
                inspectable.call(XamlInterop.ITextBlock_put_Foreground, brush.ptr)
                brush.release()
            }
        }

    /**
     * How text wraps (TextBlock.TextWrapping). In a [WLayoutPanel], a wrapping label gets the height of its wrapped
     * lines only from a layout manager that asks for [preferredHeight] at the width it gives (MigLayoutManager does).
     */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(XamlInterop.ITextBlock_get_TextWrapping))
        set(value) {
            if (value == textWrapping) return
            inspectable.call(XamlInterop.ITextBlock_put_TextWrapping, value.native)
            revalidate()
        }

    /** How text that doesn't fit is trimmed (TextBlock.TextTrimming). */
    var textTrimming: TextTrimming
        get() = TextTrimming.of(inspectable.getInt(XamlInterop.ITextBlock_get_TextTrimming))
        set(value) = inspectable.call(XamlInterop.ITextBlock_put_TextTrimming, value.native)

    /** Text alignment (TextBlock.TextAlignment). */
    var textAlignment: TextAlignment
        get() = TextAlignment.of(inspectable.getInt(XamlInterop.ITextBlock_get_TextAlignment))
        set(value) = inspectable.call(XamlInterop.ITextBlock_put_TextAlignment, value.native)

    /** Whether text can be selected with the mouse (TextBlock.IsTextSelectionEnabled). */
    var isTextSelectionEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.ITextBlock_get_IsTextSelectionEnabled)
        set(value) = inspectable.putBool(XamlInterop.ITextBlock_put_IsTextSelectionEnabled, value)

    init {
        if (text.isNotEmpty()) this.text = text
    }
}

/** The family name (FontFamily.Source) of the FontFamily property read by [slot] of [target], or "" if it has none. */
internal fun getFontFamily(target: ComPtr, slot: Int): String {
    val family = target.getPtrOrNull(slot) ?: return ""
    return try {
        family.getString(XamlInterop.IFontFamily_get_Source)
    } finally {
        family.release()
    }
}

/** Sets a FontFamily created from the family [name] on the FontFamily property written by [slot] of [target]. */
internal fun putFontFamily(target: ComPtr, slot: Int, name: String) {
    val family = createFontFamily(name)
    try {
        target.call(slot, family.ptr)
    } finally {
        family.release()
    }
}

/**
 * Creates a Media.FontFamily from a family name.
 * Calls the composable factory's CreateInstanceWithName(HSTRING, outer, out inner, out instance)
 * with outer = NULL (same convention as [Activation.composeDefault]).
 */
private fun createFontFamily(name: String): ComPtr = Ffi.backend.withScope { scope ->
    val factory = Activation.factory(XamlInterop.CLS_FontFamily, XamlInterop.IID_IFontFamilyFactory)
    val inner = scope.allocate(8)
    val instance = scope.allocate(8)
    Hstring.use(name) { h ->
        factory.call(XamlInterop.IFontFamilyFactory_CreateInstanceWithName, h, null, inner, instance)
    }
    factory.release()
    ComPtr(Ffi.backend.memory.getPtr(instance, 0))
}

// Values of Windows.UI.Text.FontStyle and TextDecorations (extracted from winmd)
private const val FONT_STYLE_NORMAL = 0
private const val FONT_STYLE_ITALIC = 2
private const val DECORATION_UNDERLINE = 1
private const val DECORATION_STRIKETHROUGH = 2
