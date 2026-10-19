package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
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
 * JLabel-like: a label that shows text with WinUI 3's TextBlock.
 * TextBlock is not a Control but a direct FrameworkElement, so it derives from [WComponent].
 *
 * TextBlock cannot have an image as a child, so the label's element (root) is a Grid holding one TextBlock, and an icon,
 * when set, is placed in that Grid.
 * Text properties ([text] / [font] / [foreground] / [textWrapping], etc.) apply to the inner TextBlock, while placement
 * and size ([horizontalAlignment] / [margin] / [width], etc., those of [WComponent]) apply to the root Grid.
 * The Grid is the same size as the TextBlock, and when it is given a width the TextBlock gets the same width, so
 * [textAlignment] and [textTrimming] work the same as when the root was the TextBlock. UI Automation names such as
 * [accessibleName] go into the TextBlock (the Grid does not appear as an element to screen readers).
 */
class WLabel @JvmOverloads constructor(text: String = "") : WComponent(
    Activation.composeDefault(XamlInterop.CLS_Grid, XamlInterop.IID_IGridFactory), // Default interface = IGrid
) {
    /** The TextBlock (ITextBlock) that shows the text. A child of the root Grid. */
    internal val textBlock: ComPtr = own(Activation.activate(XamlInterop.CLS_TextBlock, XamlInterop.IID_ITextBlock))

    /** The IDependencyObject view of [textBlock] (used for ClearValue). */
    private val textBlockObject: ComPtr = own(textBlock.queryInterface(XamlInterop.IID_IDependencyObject))

    /** The Children of the root Grid (IVector<UIElement>). */
    private val children: ComPtr = own(
        inspectable.queryInterface(XamlInterop.IID_IPanel).let { panel ->
            try {
                panel.getPtr(XamlInterop.IPanel_get_Children)
            } finally {
                panel.release()
            }
        },
    )

    override val automationTarget: ComPtr get() = textBlock

    var text: String
        get() = textBlock.getString(XamlInterop.ITextBlock_get_Text)
        set(value) = Hstring.use(value) { h -> textBlock.call(XamlInterop.ITextBlock_put_Text, h) }

    /**
     * JLabel.getFont / setFont-like: the font (TextBlock.FontFamily / FontWeight / FontStyle / FontSize).
     * Reads return the native effective value, which is the default font even before setting (a weight of 600 or more is
     * [WFont.BOLD]).
     * Setting null clears the four properties and restores the default (the value inherited from the parent)
     * (DependencyObject.ClearValue).
     * [fontSize] / [fontFamily] / [fontWeight] / [isItalic] read and write the same properties one at a time.
     */
    var font: WFont?
        get() = FontProperties.read(textBlock, FontProperties.TEXT_BLOCK)
        set(value) = FontProperties.write(textBlock, textBlockObject, FontProperties.TEXT_BLOCK, value)

    /** Font size (TextBlock.FontSize). Used to emphasize headings and the like. */
    var fontSize: Double
        get() = textBlock.getDouble(XamlInterop.ITextBlock_get_FontSize)
        set(value) = textBlock.call(XamlInterop.ITextBlock_put_FontSize, value)

    /** Font family name (TextBlock.FontFamily). Sets a family name such as "Yu Gothic UI". */
    var fontFamily: String
        get() = FontProperties.readFamily(textBlock, FontProperties.TEXT_BLOCK)
        set(value) = FontProperties.writeFamily(textBlock, FontProperties.TEXT_BLOCK, value)

    /** The font weight (TextBlock.FontWeight). 400=Normal, 600=SemiBold, 700=Bold. Reads return the native effective value. */
    var fontWeight: Int
        get() = XamlStructs.getFontWeight(textBlock, XamlInterop.ITextBlock_get_FontWeight)
        set(value) = XamlStructs.putFontWeight(textBlock, XamlInterop.ITextBlock_put_FontWeight, value)

    /** Whether the text is italic (TextBlock.FontStyle is Italic). */
    var isItalic: Boolean
        get() = textBlock.getInt(XamlInterop.ITextBlock_get_FontStyle) == FontProperties.FONT_STYLE_ITALIC
        set(value) = textBlock.call(
            XamlInterop.ITextBlock_put_FontStyle,
            if (value) FontProperties.FONT_STYLE_ITALIC else FontProperties.FONT_STYLE_NORMAL,
        )

    /** Whether the text is underlined (Underline in TextBlock.TextDecorations). */
    var isUnderline: Boolean
        get() = hasDecoration(DECORATION_UNDERLINE)
        set(value) = setDecoration(DECORATION_UNDERLINE, value)

    /** Whether the text is struck through (Strikethrough in TextBlock.TextDecorations). */
    var isStrikethrough: Boolean
        get() = hasDecoration(DECORATION_STRIKETHROUGH)
        set(value) = setDecoration(DECORATION_STRIKETHROUGH, value)

    private fun hasDecoration(flag: Int): Boolean = textBlock.getInt(XamlInterop.ITextBlock_get_TextDecorations) and flag != 0

    private fun setDecoration(flag: Int, on: Boolean) {
        val current = textBlock.getInt(XamlInterop.ITextBlock_get_TextDecorations)
        textBlock.call(XamlInterop.ITextBlock_put_TextDecorations, if (on) current or flag else current and flag.inv())
    }

    /**
     * JLabel.getForeground / setForeground-like: the text color (TextBlock.Foreground). Passed as a SolidColorBrush.
     * Reads return the native effective value, which is the default color even before setting (null if not a solid color
     * brush).
     * Setting null clears it and restores the default (the color inherited from the parent) (DependencyObject.ClearValue).
     */
    var foreground: WColor?
        get() {
            val brush = textBlock.getPtrOrNull(XamlInterop.ITextBlock_get_Foreground) ?: return null
            return try {
                colorOf(brush)
            } finally {
                brush.release()
            }
        }
        set(value) {
            if (value == null) {
                clearDependencyProperty(
                    textBlockObject,
                    XamlInterop.CLS_TextBlock,
                    XamlInterop.IID_ITextBlockStatics,
                    XamlInterop.ITextBlockStatics_get_ForegroundProperty,
                )
                return
            }
            val brush = value.createBrush()
            try {
                textBlock.call(XamlInterop.ITextBlock_put_Foreground, brush.ptr)
            } finally {
                brush.release()
            }
        }

    /** How text wraps (TextBlock.TextWrapping). */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(textBlock.getInt(XamlInterop.ITextBlock_get_TextWrapping))
        set(value) = textBlock.call(XamlInterop.ITextBlock_put_TextWrapping, value.native)

    /** How text that doesn't fit is trimmed (TextBlock.TextTrimming). */
    var textTrimming: TextTrimming
        get() = TextTrimming.of(textBlock.getInt(XamlInterop.ITextBlock_get_TextTrimming))
        set(value) = textBlock.call(XamlInterop.ITextBlock_put_TextTrimming, value.native)

    /** Text alignment (TextBlock.TextAlignment). */
    var textAlignment: TextAlignment
        get() = TextAlignment.of(textBlock.getInt(XamlInterop.ITextBlock_get_TextAlignment))
        set(value) = textBlock.call(XamlInterop.ITextBlock_put_TextAlignment, value.native)

    /** Whether text can be selected with the mouse (TextBlock.IsTextSelectionEnabled). */
    var isTextSelectionEnabled: Boolean
        get() = textBlock.getBool(XamlInterop.ITextBlock_get_IsTextSelectionEnabled)
        set(value) = textBlock.putBool(XamlInterop.ITextBlock_put_IsTextSelectionEnabled, value)

    init {
        val element = textBlock.queryInterface(XamlInterop.IID_IUIElement)
        try {
            children.call(FoundationInterop.IVector_Append, element.ptr)
        } finally {
            element.release()
        }
        if (text.isNotEmpty()) this.text = text
    }
}

// Windows.UI.Text.TextDecorations values (extracted from winmd)
private const val DECORATION_UNDERLINE = 1
private const val DECORATION_STRIKETHROUGH = 2
