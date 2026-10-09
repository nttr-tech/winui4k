package com.appkitbox.winui4k.extension.ribbon.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formatting ([RibbonSpinnerModel.format]) and parsing of spinner numbers (RibbonSpace's RibbonSpinner.FormatValue / CommitText).
 *
 * Of the .NET custom numeric format, only `0` (a digit always shown), `#` (a digit shown only when needed), and `.`
 * (the decimal point) are interpreted. Like .NET, rounding is away from zero. A unit of two or more characters is
 * appended after a space ("12 pt", "45°").
 */
internal object RibbonNumberFormat {
    /** Formats [value] with [pattern] and [unit]. The decimal point is the symbol of [locale]. */
    fun format(value: Double, pattern: String, unit: String?, locale: Locale = Locale.getDefault()): String {
        val integerPart = pattern.substringBefore('.')
        val fractionPart = if (pattern.contains('.')) pattern.substringAfter('.') else ""
        val minFraction = fractionPart.count { it == '0' }
        val maxFraction = fractionPart.count { it == '0' || it == '#' }
        val minInteger = integerPart.count { it == '0' }
        val rounded = BigDecimal.valueOf(value).setScale(maxFraction, RoundingMode.HALF_UP)
        var text = rounded.abs().toPlainString()
        if (text.contains('.')) {
            var end = text.length
            val point = text.indexOf('.')
            while (end > point + 1 + minFraction && text[end - 1] == '0') end--
            if (end == point + 1) end = point
            text = text.substring(0, end)
        }
        val integerDigits = text.substringBefore('.')
        val padded = if (integerDigits == "0" && minInteger == 0 && text.contains('.')) {
            text.substring(1)
        } else {
            integerDigits.padStart(minInteger, '0') + text.substring(integerDigits.length)
        }
        val negative = rounded.signum() < 0
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val number = (if (negative) "-" else "") + padded.replace('.', separator)
        return if (unit.isNullOrEmpty()) number else number + (if (unit.length > 1) " " else "") + unit
    }

    /** Parses the input [text] as a number (removing a trailing [unit]). Returns null if it cannot be parsed. */
    fun parse(text: String, unit: String?, locale: Locale = Locale.getDefault()): Double? {
        var trimmed = text.trim()
        if (!unit.isNullOrEmpty() && trimmed.endsWith(unit, ignoreCase = true)) trimmed = trimmed.dropLast(unit.length).trim()
        if (trimmed.isEmpty()) return null
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val normalized = if (separator != '.') trimmed.replace(separator, '.') else trimmed
        return normalized.toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
    }
}
