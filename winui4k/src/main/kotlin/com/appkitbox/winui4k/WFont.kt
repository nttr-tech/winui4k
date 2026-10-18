package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.math.roundToInt

/**
 * java.awt.Font-like: a font value (family name, style, and size).
 * When passed to [WControl.font], it is expanded into FontFamily / FontWeight / FontStyle / FontSize.
 *
 * The size unit is DIP (WinUI pixels; at 96 dpi, the same visual size as AWT points).
 * [style] is a bitwise OR of [PLAIN] / [BOLD] / [ITALIC]; [BOLD] is FontWeight 700 and [ITALIC] is FontStyle.Italic.
 */
class WFont(
    /** The font family name (for example, "Segoe UI"). */
    val name: String,
    /** A bitwise OR of [PLAIN] / [BOLD] / [ITALIC]. */
    val style: Int,
    /**
     * The exact size in DIPs. The public [size2D] is a Float to match AWT, so this error-free value is used internally.
     */
    internal val sizeDip: Double,
) {
    init {
        require(style and (BOLD or ITALIC).inv() == 0) { "style must be a bitwise OR of PLAIN / BOLD / ITALIC: $style" }
        require(sizeDip > 0.0 && sizeDip.isFinite()) { "size must be a finite value greater than 0: $sizeDip" }
    }

    /** Specifies the size as an integer (the same call style as Swing's `new Font(name, style, 12)`). */
    constructor(name: String, style: Int, size: Int) : this(name, style, size.toDouble())

    /**
     * The size in DIPs (equivalent to AWT's Font.getSize2D; a Float so that Java can write `float s = font.getSize2D();`).
     */
    val size2D: Float get() = sizeDip.toFloat()

    /** The size rounded to an integer (equivalent to AWT's Font.getSize). */
    val size: Int get() = sizeDip.roundToInt()

    /** Whether it is bold ([style] includes [BOLD]). */
    val isBold: Boolean get() = style and BOLD != 0

    /** Whether it is italic ([style] includes [ITALIC]). */
    val isItalic: Boolean get() = style and ITALIC != 0

    /** A new WFont with only [style] replaced. */
    fun deriveFont(style: Int): WFont = WFont(name, style, sizeDip)

    /** A new WFont with only [size] replaced. */
    fun deriveFont(size: Float): WFont = WFont(name, style, size.toDouble())

    /** A new WFont with [style] and [size] replaced. */
    fun deriveFont(style: Int, size: Float): WFont = WFont(name, style, size.toDouble())

    /** Equal if name, style, and size are all equal. */
    override fun equals(other: Any?): Boolean =
        other is WFont && name == other.name && style == other.style && sizeDip == other.sizeDip

    override fun hashCode(): Int = (name.hashCode() * HASH_FACTOR + style) * HASH_FACTOR + sizeDip.hashCode()

    override fun toString(): String = "WFont(name=$name, style=$style, size=$sizeDip)"

    companion object {
        /** Plain (neither bold nor italic). */
        const val PLAIN = 0

        /** Bold (FontWeight 700). */
        const val BOLD = 1

        /** Italic (FontStyle.Italic). */
        const val ITALIC = 2
    }
}

private const val HASH_FACTOR = 31

/**
 * Creates a Media.FontFamily from a family name. The caller must release it.
 * Calls the composable factory's CreateInstanceWithName(HSTRING, outer, out inner, out instance)
 * with outer = NULL (same convention as [Activation.composeDefault]).
 */
internal fun createFontFamily(name: String): ComPtr = Ffi.backend.withScope { scope ->
    val factory = Activation.factory(XamlInterop.CLS_FontFamily, XamlInterop.IID_IFontFamilyFactory)
    val inner = scope.allocate(8)
    val instance = scope.allocate(8)
    Hstring.use(name) { h ->
        factory.call(XamlInterop.IFontFamilyFactory_CreateInstanceWithName, h, null, inner, instance)
    }
    factory.release()
    ComPtr(Ffi.backend.memory.getPtr(instance, 0))
}
