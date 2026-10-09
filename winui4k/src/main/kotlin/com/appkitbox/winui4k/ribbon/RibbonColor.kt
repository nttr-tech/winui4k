package com.appkitbox.winui4k.ribbon

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * A UI-independent ARGB color, with Office-compatible helpers to lighten and darken it ([lighten] / [darken]).
 * Each component is 0..255.
 */
data class RibbonColor(
    /** Opacity (0 = transparent, 255 = opaque). */
    val a: Int,
    /** Red. */
    val r: Int,
    /** Green. */
    val g: Int,
    /** Blue. */
    val b: Int,
) {
    init {
        require(a in 0..MAX && r in 0..MAX && g in 0..MAX && b in 0..MAX) { "Each component must be 0..255: ($a, $r, $g, $b)" }
    }

    /** WCAG relative luminance (0 = black, 1 = white). */
    val luminance: Double
        get() {
            fun channel(c: Int): Double {
                val v = c / 255.0
                return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
        }

    /** HSL lightness (0..1). */
    val lightness: Double get() = toHsl()[2]

    /** Whether black text is more readable than white text on this color. */
    val prefersDarkForeground: Boolean get() = contrastRatio(this, BLACK) >= contrastRatio(this, WHITE)

    /** This color with only the opacity replaced by [alpha]. */
    fun withAlpha(alpha: Int): RibbonColor = copy(a = alpha)

    /** Moves the HSL lightness toward white by [amount] (0..1) (such as Office's "Lighter 40%"). */
    fun lighten(amount: Double): RibbonColor {
        val (h, s, l) = toHsl()
        return fromHsl(h, s, l + (1 - l) * amount.coerceIn(0.0, 1.0), a)
    }

    /** Moves the HSL lightness toward black by [amount] (0..1) (such as Office's "Darker 25%"). */
    fun darken(amount: Double): RibbonColor {
        val (h, s, l) = toHsl()
        return fromHsl(h, s, l * (1 - amount.coerceIn(0.0, 1.0)), a)
    }

    /** Linear interpolation with [other] (0 = this color, 1 = [other]). */
    fun blend(other: RibbonColor, amount: Double): RibbonColor {
        val t = amount.coerceIn(0.0, 1.0)
        fun mix(x: Int, y: Int): Int = roundHalfEven(x + (y - x) * t)
        return RibbonColor(mix(a, other.a), mix(r, other.r), mix(g, other.g), mix(b, other.b))
    }

    /** Converts to three components: hue (0..360), saturation and lightness (0..1). */
    fun toHsl(): DoubleArray {
        val rf = r / 255.0
        val gf = g / 255.0
        val bf = b / 255.0
        val maxValue = max(rf, max(gf, bf))
        val minValue = min(rf, min(gf, bf))
        val l = (maxValue + minValue) / 2
        if (maxValue - minValue < 1e-9) return doubleArrayOf(0.0, 0.0, l)
        val d = maxValue - minValue
        val s = if (l > 0.5) d / (2 - maxValue - minValue) else d / (maxValue + minValue)
        val h = when (maxValue) {
            rf -> (gf - bf) / d + (if (gf < bf) 6 else 0)
            gf -> (bf - rf) / d + 2
            else -> (rf - gf) / d + 4
        }
        return doubleArrayOf(h * 60, s, l)
    }

    /** #RRGGBB if opaque, #AARRGGBB if translucent (always #AARRGGBB if [includeAlpha]). */
    @JvmOverloads
    fun toHex(includeAlpha: Boolean = false): String =
        if (includeAlpha || a != MAX) {
            String.format(Locale.ROOT, "#%02X%02X%02X%02X", a, r, g, b)
        } else {
            String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b)
        }

    override fun toString(): String = toHex()

    companion object {
        private const val MAX = 255

        /** A fully transparent color. */
        @JvmField
        val TRANSPARENT = RibbonColor(0, 0, 0, 0)

        /** Opaque white. */
        @JvmField
        val WHITE = RibbonColor(MAX, MAX, MAX, MAX)

        /** Opaque black. */
        @JvmField
        val BLACK = RibbonColor(MAX, 0, 0, 0)

        /** An opaque color. */
        @JvmStatic
        fun fromRgb(r: Int, g: Int, b: Int): RibbonColor = RibbonColor(MAX, r, g, b)

        /** Parses #RGB / #RRGGBB / #AARRGGBB (the leading # is optional). Throws if the format is invalid. */
        @JvmStatic
        fun parse(value: String): RibbonColor =
            tryParse(value) ?: throw IllegalArgumentException("'$value' is not a color. Specify it as #RRGGBB or #AARRGGBB")

        /** Parses #RGB / #RRGGBB / #AARRGGBB. Returns null if the format is invalid. */
        @JvmStatic
        fun tryParse(value: String?): RibbonColor? {
            if (value.isNullOrBlank()) return null
            var hex = value.trim().trimStart('#')
            if (hex.length == 3) hex = hex.map { "$it$it" }.joinToString("")
            if (hex.length == 6) hex = "FF$hex"
            if (hex.length != 8 || hex.any { Character.digit(it, 16) < 0 }) return null
            val argb = hex.toLong(16)
            return RibbonColor(
                (argb shr 24 and 0xFF).toInt(),
                (argb shr 16 and 0xFF).toInt(),
                (argb shr 8 and 0xFF).toInt(),
                (argb and 0xFF).toInt(),
            )
        }

        /** WCAG contrast ratio of two colors (1..21). */
        @JvmStatic
        fun contrastRatio(first: RibbonColor, second: RibbonColor): Double {
            val l1 = first.luminance
            val l2 = second.luminance
            return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
        }

        /** Creates a color from HSL (hue 0..360, saturation and lightness 0..1). */
        @JvmStatic
        @JvmOverloads
        fun fromHsl(h: Double, s: Double, l: Double, alpha: Int = MAX): RibbonColor {
            val hue = ((h % 360) + 360) % 360 / 360.0
            val saturation = s.coerceIn(0.0, 1.0)
            val lightness = l.coerceIn(0.0, 1.0)
            if (saturation <= 0) {
                val v = roundHalfEven(lightness * MAX)
                return RibbonColor(alpha, v, v, v)
            }
            val q = if (lightness < 0.5) lightness * (1 + saturation) else lightness + saturation - lightness * saturation
            val p = 2 * lightness - q
            fun channel(t0: Double): Int {
                var t = t0
                if (t < 0) t += 1
                if (t > 1) t -= 1
                val v = when {
                    t < 1.0 / 6 -> p + (q - p) * 6 * t
                    t < 1.0 / 2 -> q
                    t < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - t) * 6
                    else -> p
                }
                return roundHalfEven(v * MAX)
            }
            return RibbonColor(alpha, channel(hue + 1.0 / 3), channel(hue), channel(hue - 1.0 / 3))
        }

        /** Round-half-to-even, the same as .NET's Math.Round (to produce the same colors as RibbonSpace). */
        private fun roundHalfEven(value: Double): Int = Math.rint(value).toInt()
    }
}

/** A named color swatch (with a name for accessibility, e.g. "Blue, Accent 1, Lighter 40%"). */
data class RibbonColorSwatch(
    /** Color. */
    val color: RibbonColor,
    /** Name for screen readers and tooltips. */
    val name: String,
)
