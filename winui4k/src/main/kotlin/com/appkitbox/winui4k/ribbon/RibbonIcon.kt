package com.appkitbox.winui4k.ribbon

import com.appkitbox.winui4k.Symbol

/** Kind of a [RibbonIcon]. */
enum class RibbonIconKind {
    /** A glyph of an icon font (by default, a Segoe Fluent Icons code point). */
    GLYPH,

    /** A vector path in the SVG / XAML path mini-language. */
    PATH,

    /** An image referenced by a URI (ms-appx:///, https://, file:///). */
    IMAGE,

    /** Text or an emoji drawn as an icon. */
    TEXT,
}

/**
 * A UI-independent description of an icon. The view converts it to a FontIcon / Path / Image / TextBlock and draws it.
 *
 * [value] is a glyph, path data, a URI or a string depending on [kind].
 * [foreground] is a fixed color (#RRGGBB / #AARRGGBB). If null, the theme's icon color is used.
 * [viewBoxSize] is the size of the design grid of a path icon (16 / 20 / 24, etc.).
 */
data class RibbonIcon @JvmOverloads constructor(
    /** Kind of the icon. */
    val kind: RibbonIconKind,
    /** Glyph, path data, URI or string. */
    val value: String,
    /** Font family of the glyph (if null, the default symbol font). */
    val fontFamily: String? = null,
    /** Fixed color (if null, the theme's icon color). */
    val foreground: String? = null,
    /** Size of the design grid of a path icon. */
    val viewBoxSize: Double = 24.0,
) {
    /** This icon with its fixed color replaced by [color]. */
    fun withForeground(color: String?): RibbonIcon = copy(foreground = color)

    companion object {
        /** An icon from a font glyph. */
        @JvmStatic
        @JvmOverloads
        fun glyph(glyph: String, fontFamily: String? = null, foreground: String? = null): RibbonIcon =
            RibbonIcon(RibbonIconKind.GLYPH, glyph, fontFamily, foreground)

        /** An icon from the glyph of a WinUI predefined icon ([Symbol]). */
        @JvmStatic
        fun symbol(symbol: Symbol): RibbonIcon = glyph(String(Character.toChars(symbol.native)))

        /** A vector path icon (filled). */
        @JvmStatic
        @JvmOverloads
        fun path(data: String, viewBoxSize: Double = 24.0, foreground: String? = null): RibbonIcon =
            RibbonIcon(RibbonIconKind.PATH, data, null, foreground, viewBoxSize)

        /**
         * A line-art icon: the path is not filled but drawn with round-capped lines of [thickness] (in design units) (e.g. CAD command icons).
         */
        @JvmStatic
        @JvmOverloads
        fun stroke(data: String, thickness: Double = 1.5, viewBoxSize: Double = 24.0, foreground: String? = null): RibbonIcon =
            RibbonIcon(RibbonIconKind.PATH, RibbonIconLayer(data, thickness).toString(), null, foreground, viewBoxSize)

        /**
         * A multi-layer vector icon (such as a blue shape with a yellow highlight). Layers are drawn in order, and
         * layers without a color use the theme's icon color.
         */
        @JvmStatic
        fun layers(viewBoxSize: Double, vararg layers: RibbonIconLayer): RibbonIcon =
            RibbonIcon(RibbonIconKind.PATH, layers.joinToString("|"), null, null, viewBoxSize)

        /** An image icon. */
        @JvmStatic
        fun image(uri: String): RibbonIcon = RibbonIcon(RibbonIconKind.IMAGE, uri)

        /** An icon from text or an emoji. */
        @JvmStatic
        @JvmOverloads
        fun text(text: String, foreground: String? = null): RibbonIcon = RibbonIcon(RibbonIconKind.TEXT, text, null, foreground)
    }
}

/**
 * One layer of a vector icon: path data that is filled ([strokeThickness] = 0) or stroked, and an optional fixed
 * color. As a string, it is written as `[stroke=1.5;color=#E8C66E;opacity=0.6]M4,4 L20,20`, with multiple layers
 * separated by `|`. `viewbox=` in the header of the first layer specifies the design grid of a string icon
 * (default 24). Headers in curly braces (`{stroke=1.5}`) are also accepted.
 */
data class RibbonIconLayer @JvmOverloads constructor(
    /** Data in the path mini-language. */
    val data: String,
    /** Stroke thickness (in design units). If 0, the path is filled. */
    val strokeThickness: Double = 0.0,
    /** Fixed color (if null, the icon's foreground color is used). */
    val color: String? = null,
    /** Opacity of the layer (0..1). */
    val opacity: Double = 1.0,
) {
    override fun toString(): String {
        val parts = mutableListOf<String>()
        if (strokeThickness > 0) parts += "stroke=" + formatNumber(strokeThickness)
        if (!color.isNullOrEmpty()) parts += "color=$color"
        if (opacity < 1) parts += "opacity=" + formatNumber(opacity)
        return if (parts.isEmpty()) data else "[" + parts.joinToString(";") + "]" + data
    }

    companion object {
        /** Reads `viewbox=` (the design grid of a string icon) from the header of the first layer. Returns null if absent. */
        @JvmStatic
        fun parseViewBox(value: String?): Double? {
            if (value.isNullOrBlank()) return null
            val text = value.trimStart()
            val close = when {
                text.startsWith('[') -> ']'
                text.startsWith('{') -> '}'
                else -> return null
            }
            val end = text.indexOf(close)
            if (end <= 0) return null
            for (setting in splitSettings(text.substring(1, end))) {
                val pair = setting.split('=', limit = 2).map { it.trim() }
                if (pair.size == 2 && pair[0].equals("viewbox", ignoreCase = true)) {
                    val size = pair[1].toDoubleOrNull()
                    if (size != null && size > 0) return size
                }
            }
            return null
        }

        /** Parses a layer string (separated by `|`, with optional `[key=value;...]` headers). */
        @JvmStatic
        fun parse(value: String?): List<RibbonIconLayer> {
            if (value.isNullOrBlank()) return emptyList()
            return value.split('|').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { parseLayer(it) }
        }

        private fun parseLayer(raw: String): RibbonIconLayer? {
            var data = raw
            var stroke = 0.0
            var opacity = 1.0
            var color: String? = null
            val close = if (data.startsWith('[')) ']' else '}'
            val end = data.indexOf(close)
            if ((data.startsWith('[') || data.startsWith('{')) && end > 0) {
                for (setting in splitSettings(data.substring(1, end))) {
                    val pair = setting.split('=', limit = 2).map { it.trim() }
                    val text = if (pair.size > 1) pair[1] else ""
                    when (pair[0].lowercase()) {
                        "stroke" -> stroke = text.toDoubleOrNull() ?: 1.5
                        "color", "fill" -> color = text.ifEmpty { null }
                        "opacity" -> opacity = text.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
                    }
                }
                data = data.substring(end + 1).trim()
            }
            return if (data.isEmpty()) null else RibbonIconLayer(data, stroke, color, opacity)
        }

        private fun splitSettings(text: String): List<String> = text.split(';').map { it.trim() }.filter { it.isNotEmpty() }

        /** Number formatting of .NET's InvariantCulture (1.5 → "1.5", 2.0 → "2"). */
        private fun formatNumber(value: Double): String =
            if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()
    }
}
