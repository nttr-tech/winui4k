package com.appkitbox.winui4k

/**
 * StyleConstants-like: the keys of text formatting attributes and static functions to read and write them from a [WAttributeSet].
 * As in Swing, the keys are instances of this class ([Bold] etc.; toString also returns the same names as Swing).
 * In Java you can write `WStyleConstants.setBold(attrs, true)` or `attrs.addAttribute(WStyleConstants.Bold, true)`.
 *
 * Only [Bold] / [Italic] / [Underline] / [Foreground] / [FontSize] / [FontFamily] are reflected in [WTextPane] formatting
 * (Swing's Background, StrikeThrough, paragraph attributes etc. are not provided). As in Swing, the value types are Boolean / Boolean / Boolean /
 * [WColor] (java.awt.Color in Swing) / Int / String.
 * The unit of [FontSize] is DIP, the same as [WFont] (at 96 dpi it looks the same size as Swing's points).
 */
class WStyleConstants private constructor(private val name: String) {
    override fun toString(): String = name

    @Suppress("ObjectPropertyNaming", "VariableNaming") // Use the same names as Swing's StyleConstants.Bold etc.
    companion object {
        /** StyleConstants.Bold-like: whether it is bold (Boolean). */
        @JvmField
        val Bold: Any = WStyleConstants("bold")

        /** StyleConstants.Italic-like: whether it is italic (Boolean). */
        @JvmField
        val Italic: Any = WStyleConstants("italic")

        /** StyleConstants.Underline-like: whether it is underlined (Boolean). */
        @JvmField
        val Underline: Any = WStyleConstants("underline")

        /** StyleConstants.Foreground-like: the text color ([WColor]). */
        @JvmField
        val Foreground: Any = WStyleConstants("foreground")

        /** StyleConstants.FontSize-like: the text size (Int, DIP). */
        @JvmField
        val FontSize: Any = WStyleConstants("size")

        /** StyleConstants.FontFamily-like: the font family name (String, e.g. "Segoe UI"). */
        @JvmField
        val FontFamily: Any = WStyleConstants("family")

        /** StyleConstants.isBold-like: whether [a] is bold. false if there is no value. */
        @JvmStatic
        fun isBold(a: WAttributeSet): Boolean = flag(a, Bold)

        /** StyleConstants.setBold-like: sets the bold of [a] to [b]. */
        @JvmStatic
        fun setBold(a: WMutableAttributeSet, b: Boolean) = a.addAttribute(Bold, b)

        /** StyleConstants.isItalic-like: whether [a] is italic. false if there is no value. */
        @JvmStatic
        fun isItalic(a: WAttributeSet): Boolean = flag(a, Italic)

        /** StyleConstants.setItalic-like: sets the italic of [a] to [b]. */
        @JvmStatic
        fun setItalic(a: WMutableAttributeSet, b: Boolean) = a.addAttribute(Italic, b)

        /** StyleConstants.isUnderline-like: whether [a] is underlined. false if there is no value. */
        @JvmStatic
        fun isUnderline(a: WAttributeSet): Boolean = flag(a, Underline)

        /** StyleConstants.setUnderline-like: sets the underline of [a] to [b]. */
        @JvmStatic
        fun setUnderline(a: WMutableAttributeSet, b: Boolean) = a.addAttribute(Underline, b)

        /** StyleConstants.getForeground-like: the text color of [a]. Black ([WColor.BLACK]) if there is no value, the same as Swing. */
        @JvmStatic
        fun getForeground(a: WAttributeSet): WColor = typed<WColor>(a, Foreground) ?: WColor.BLACK

        /** StyleConstants.setForeground-like: sets the text color of [a] to [fg]. */
        @JvmStatic
        fun setForeground(a: WMutableAttributeSet, fg: WColor) = a.addAttribute(Foreground, fg)

        /** StyleConstants.getFontSize-like: the text size of [a] (DIP). 12 if there is no value, the same as Swing. */
        @JvmStatic
        fun getFontSize(a: WAttributeSet): Int = typed<Int>(a, FontSize) ?: DEFAULT_FONT_SIZE

        /** StyleConstants.setFontSize-like: sets the text size of [a] to [s] (DIP). */
        @JvmStatic
        fun setFontSize(a: WMutableAttributeSet, s: Int) = a.addAttribute(FontSize, s)

        /** StyleConstants.getFontFamily-like: the font family name of [a]. "Monospaced" if there is no value, the same as Swing (WinUI has no font with this name). */
        @JvmStatic
        fun getFontFamily(a: WAttributeSet): String = typed<String>(a, FontFamily) ?: DEFAULT_FONT_FAMILY

        /** StyleConstants.setFontFamily-like: sets the font family of [a] to [fam]. */
        @JvmStatic
        fun setFontFamily(a: WMutableAttributeSet, fam: String) = a.addAttribute(FontFamily, fam)

        private const val DEFAULT_FONT_SIZE = 12
        private const val DEFAULT_FONT_FAMILY = "Monospaced"

        private fun flag(a: WAttributeSet, key: Any): Boolean = typed<Boolean>(a, key) ?: false

        /**
         * Reads the value of [key] in [a] as [T]. null if there is no value. [IllegalArgumentException] if the type differs
         * (ClassCastException in Swing).
         */
        internal inline fun <reified T : Any> typed(a: WAttributeSet, key: Any): T? {
            val value = a.getAttribute(key) ?: return null
            require(value is T) { "The value of $key has the wrong type: ${value::class.java.name} (${T::class.javaObjectType.simpleName} is required)" }
            return value
        }
    }
}
