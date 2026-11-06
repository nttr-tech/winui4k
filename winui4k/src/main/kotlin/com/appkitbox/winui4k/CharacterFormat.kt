package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.TextInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import kotlin.math.roundToInt

/**
 * The values that copy the formatting attributes of a [WAttributeSet] (the six of [WStyleConstants]) to a RichEditBox ITextCharacterFormat.
 * Attributes not present are null (unchanged). [of] checks the value types first, so if a type differs, [IllegalArgumentException] is thrown without rewriting anything.
 */
internal class CharacterFormat private constructor(
    private val bold: Boolean?,
    private val italic: Boolean?,
    private val underline: Boolean?,
    private val foreground: WColor?,
    private val fontSize: Int?,
    private val fontFamily: String?,
) {
    /** Writes only the attributes present to [format] (ITextCharacterFormat, borrowed). */
    fun applyTo(format: ComPtr) {
        bold?.let { format.call(XamlInterop.ITextCharacterFormat_put_Bold, effect(it)) }
        italic?.let { format.call(XamlInterop.ITextCharacterFormat_put_Italic, effect(it)) }
        underline?.let {
            format.call(TextInterop.ITextCharacterFormat_put_Underline, if (it) TextInterop.UnderlineType_Single else TextInterop.UnderlineType_None)
        }
        foreground?.let {
            XamlStructs.putColor(format, TextInterop.ITextCharacterFormat_put_ForegroundColor, it.alpha, it.red, it.green, it.blue)
        }
        fontSize?.let { format.call(TextInterop.ITextCharacterFormat_put_Size, (it * POINTS_PER_DIP).toFloat()) }
        fontFamily?.let { name -> Hstring.use(name) { h -> format.call(TextInterop.ITextCharacterFormat_put_Name, h) } }
    }

    companion object {
        /** ITextCharacterFormat.Size is in points (1/72 inch), and [WStyleConstants.FontSize] is in DIP (1/96 inch). */
        private const val POINTS_PER_DIP = 0.75

        private fun effect(on: Boolean): Int = if (on) TextInterop.FormatEffect_On else TextInterop.FormatEffect_Off

        /** Reads the formatting attributes from [attributes]. */
        fun of(attributes: WAttributeSet): CharacterFormat = CharacterFormat(
            WStyleConstants.typed<Boolean>(attributes, WStyleConstants.Bold),
            WStyleConstants.typed<Boolean>(attributes, WStyleConstants.Italic),
            WStyleConstants.typed<Boolean>(attributes, WStyleConstants.Underline),
            WStyleConstants.typed<WColor>(attributes, WStyleConstants.Foreground),
            WStyleConstants.typed<Int>(attributes, WStyleConstants.FontSize),
            WStyleConstants.typed<String>(attributes, WStyleConstants.FontFamily),
        )

        /** Reads the six attributes of [format] (ITextCharacterFormat, borrowed). */
        fun read(format: ComPtr): WSimpleAttributeSet {
            val attributes = WSimpleAttributeSet()
            WStyleConstants.setBold(attributes, format.getInt(XamlInterop.ITextCharacterFormat_get_Bold) == TextInterop.FormatEffect_On)
            WStyleConstants.setItalic(attributes, format.getInt(XamlInterop.ITextCharacterFormat_get_Italic) == TextInterop.FormatEffect_On)
            val underline = format.getInt(TextInterop.ITextCharacterFormat_get_Underline)
            WStyleConstants.setUnderline(attributes, underline != TextInterop.UnderlineType_None && underline != TextInterop.UnderlineType_Undefined)
            val (alpha, red, green, blue) = XamlStructs.getColor(format, TextInterop.ITextCharacterFormat_get_ForegroundColor).toList()
            WStyleConstants.setForeground(attributes, WColor(red, green, blue, alpha))
            WStyleConstants.setFontSize(attributes, (format.getFloat(TextInterop.ITextCharacterFormat_get_Size) / POINTS_PER_DIP).roundToInt())
            WStyleConstants.setFontFamily(attributes, format.getString(TextInterop.ITextCharacterFormat_get_Name))
            return attributes
        }
    }
}
