package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonIconKind
import com.appkitbox.winui4k.ribbon.RibbonIconLayer

/**
 * Creates XAML elements that draw a [RibbonIcon] at a given size (RibbonIconPresenter in RibbonSpace).
 *
 * Glyphs are drawn with FontIcon; paths with Paths stacked on a Canvas inside a Viewbox (layers without a color use the
 * foreground color, and stroke layers use round-capped lines); images with Image; and text with TextBlock. Glyphs for
 * characters not in the symbol font ("$" or "%") are drawn as text (because WinUI's FontIcon does not fall back to text fonts).
 */
internal object RibbonIconXaml {
    /** The default foreground (the theme's icon color). */
    const val ICON_BRUSH = "{ThemeResource RibbonIconBrush}"

    /**
     * The XAML of an element that draws [icon] at [size]. [foreground] is the XAML value of the foreground
     * (`{ThemeResource ...}` or #AARRGGBB).
     * A fixed color on the icon takes precedence. Returns null if [icon] is null.
     */
    fun build(icon: RibbonIcon?, size: Double, foreground: String = ICON_BRUSH): String? {
        if (icon == null || icon.value.isEmpty()) return null
        val fixed = icon.foreground?.let { RibbonColor.tryParse(it) }?.toHex(true)
        val brush = fixed ?: foreground
        val s = Xaml.num(size)
        return when (icon.kind) {
            RibbonIconKind.GLYPH -> if (icon.fontFamily == null && classify(icon.value) == RibbonIconKind.TEXT) {
                text(icon.value, size, brush)
            } else {
                val family = icon.fontFamily?.let { Xaml.escape(it) } ?: "{ThemeResource SymbolThemeFontFamily}"
                "<FontIcon Glyph=\"${Xaml.escape(icon.value)}\" FontSize=\"$s\" FontFamily=\"$family\" Foreground=\"$brush\" " +
                    "Width=\"$s\" Height=\"$s\" IsHitTestVisible=\"False\" IsTextScaleFactorEnabled=\"False\" />"
            }
            RibbonIconKind.PATH -> path(icon.value, icon.viewBoxSize, size, brush)
            RibbonIconKind.IMAGE -> image(icon.value, size)
            RibbonIconKind.TEXT -> text(icon.value, size, brush)
        }
    }

    /**
     * Determines the kind of a string icon (Classify in RibbonSpace).
     * URIs and image files are images; "M..." values containing digits and values with a header are paths; one or two
     * private-use characters are glyphs; everything else is text.
     */
    fun classify(value: String): RibbonIconKind {
        val trimmed = value.trim()
        return when {
            isImage(trimmed.lowercase()) -> RibbonIconKind.IMAGE
            trimmed.length > 6 && isPathStart(trimmed) && trimmed.any { it.isDigit() } -> RibbonIconKind.PATH
            trimmed.isNotEmpty() && trimmed.length <= 2 && trimmed.all { it in ''..'' || Character.isSurrogate(it) } ->
                RibbonIconKind.GLYPH
            else -> RibbonIconKind.TEXT
        }
    }

    private val IMAGE_PREFIXES = listOf("ms-appx:", "http", "ms-appdata:", "file:")

    private fun isImage(lower: String): Boolean = IMAGE_PREFIXES.any { lower.startsWith(it) } || lower.endsWith(".png") || lower.endsWith(".svg")

    private fun isPathStart(text: String): Boolean = when (text.first()) {
        'M', 'm', 'F' -> true
        '{' -> text.contains('}')
        '[' -> text.contains(']')
        else -> false
    }

    /**
     * The XAML of an IconElement for a menu item (MenuFlyoutItem.Icon). Returns null if it cannot be represented
     * (anything other than line art or images).
     * [WRibbonTheme.menuIconConverter] is applied first if set.
     */
    fun menuIcon(original: RibbonIcon?): String? {
        if (original == null || original.value.isEmpty()) return null
        val icon = WRibbonTheme.menuIconConverter?.convert(original) ?: original
        return when (icon.kind) {
            RibbonIconKind.GLYPH -> if (icon.fontFamily == null && classify(icon.value) == RibbonIconKind.TEXT) {
                null
            } else {
                val family = icon.fontFamily?.let { " FontFamily=\"${Xaml.escape(it)}\"" }.orEmpty()
                "<FontIcon Glyph=\"${Xaml.escape(icon.value)}\"$family />"
            }
            RibbonIconKind.PATH -> {
                // PathIcon can only fill, so line art that contains stroke layers cannot be represented
                val layers = RibbonIconLayer.parse(icon.value)
                if (layers.isEmpty() || layers.any { it.strokeThickness > 0 }) {
                    null
                } else {
                    "<PathIcon Data=\"${Xaml.escape(layers.joinToString(" ") { it.data })}\" />"
                }
            }
            RibbonIconKind.IMAGE -> "<ImageIcon Source=\"${Xaml.escape(imageUri(icon.value))}\" />"
            RibbonIconKind.TEXT -> null
        }
    }

    private fun text(value: String, size: Double, brush: String): String {
        val block = "<TextBlock Text=\"${Xaml.escape(value)}\" FontSize=\"${Xaml.num(size * TEXT_SCALE)}\" FontWeight=\"SemiBold\" " +
            "Foreground=\"$brush\" HorizontalAlignment=\"Center\" VerticalAlignment=\"Center\" TextAlignment=\"Center\" " +
            "IsHitTestVisible=\"False\" IsTextScaleFactorEnabled=\"False\" />"
        if (value.length <= 2) return "<Grid Width=\"${Xaml.num(size)}\" Height=\"${Xaml.num(size)}\" IsHitTestVisible=\"False\">$block</Grid>"
        return "<Viewbox Width=\"${Xaml.num(size)}\" Height=\"${Xaml.num(size)}\" Stretch=\"Uniform\" StretchDirection=\"DownOnly\" " +
            "IsHitTestVisible=\"False\">$block</Viewbox>"
    }

    private fun path(data: String, viewBoxSize: Double, size: Double, brush: String): String = buildString {
        val box = Xaml.num(viewBoxSize)
        append("<Viewbox Width=\"").append(Xaml.num(size)).append("\" Height=\"").append(Xaml.num(size))
        append("\" Stretch=\"Uniform\" IsHitTestVisible=\"False\"><Canvas Width=\"").append(box).append("\" Height=\"").append(box).append("\">")
        for (layer in RibbonIconLayer.parse(data)) {
            val layerBrush = layer.color?.let { RibbonColor.tryParse(it) }?.toHex(true) ?: brush
            append("<Path Data=\"").append(Xaml.escape(layer.data)).append('"')
            if (layer.strokeThickness > 0) {
                append(" Stroke=\"").append(layerBrush).append("\" StrokeThickness=\"").append(Xaml.num(layer.strokeThickness))
                append("\" StrokeStartLineCap=\"Round\" StrokeEndLineCap=\"Round\" StrokeLineJoin=\"Round\"")
            } else {
                append(" Fill=\"").append(layerBrush).append('"')
            }
            if (layer.opacity < 1) append(" Opacity=\"").append(Xaml.num(layer.opacity)).append('"')
            append(" />")
        }
        append("</Canvas></Viewbox>")
    }

    private fun image(value: String, size: Double): String {
        val uri = Xaml.escape(imageUri(value))
        val s = Xaml.num(size)
        return if (value.trim().lowercase().endsWith(".svg")) {
            "<Image Width=\"$s\" Height=\"$s\" Stretch=\"Uniform\" IsHitTestVisible=\"False\"><Image.Source><SvgImageSource UriSource=\"$uri\" /></Image.Source></Image>"
        } else {
            "<Image Source=\"$uri\" Width=\"$s\" Height=\"$s\" Stretch=\"Uniform\" IsHitTestVisible=\"False\" />"
        }
    }

    /**
     * The image URI. A path without a scheme (a single drive letter is not a scheme) is treated as a file (because apps
     * without a package have no ms-appx).
     */
    fun imageUri(value: String): String {
        val trimmed = value.trim()
        if (Regex("^[A-Za-z][A-Za-z0-9+.-]+:").containsMatchIn(trimmed)) return trimmed
        val file = java.io.File(trimmed.replace('/', java.io.File.separatorChar))
        return file.absoluteFile.toURI().toString()
    }

    /** The size ratio of the characters in a text icon (the same as RibbonSpace). */
    private const val TEXT_SCALE = 0.8
}
