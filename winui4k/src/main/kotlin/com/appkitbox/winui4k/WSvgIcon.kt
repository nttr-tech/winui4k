package com.appkitbox.winui4k

/**
 * JLabel-with-an-Icon-like: draws an [SvgIcon] (such as a Material Symbols icon) as a square of [iconSize].
 *
 * Paths without a fixed color use [foreground], or the theme's primary text color if it is null, so the icon follows
 * light / dark themes. Built from a Viewbox holding a Canvas of the icon's design grid with one Path per SVG path.
 */
class WSvgIcon @JvmOverloads constructor(
    icon: SvgIcon? = null,
    size: Double = DEFAULT_SIZE,
) : WComponent(Xaml.load("<Border IsHitTestVisible=\"False\" />")) {
    private val root: XamlElement = XamlElement(inspectable.also { it.addRef() })

    /** The icon to draw (nothing is drawn if null). */
    var icon: SvgIcon? = icon
        set(value) {
            field = value
            rebuild()
        }

    /** The icon size (DIP). */
    var iconSize: Double = size
        set(value) {
            field = value
            rebuild()
        }

    /** The color of paths without a fixed color (the theme's primary text color if null). */
    var foreground: WColor? = null
        set(value) {
            field = value
            rebuild()
        }

    init {
        rebuild()
    }

    private fun rebuild() {
        val brush = foreground?.let { "#%02X%02X%02X%02X".format(it.alpha, it.red, it.green, it.blue) } ?: SvgIconXaml.THEME_BRUSH
        // An icon keeps its square size instead of stretching to the space its parent offers
        width = iconSize
        height = iconSize
        root.setChild(icon?.let { XamlElement.load(SvgIconXaml.build(it, iconSize, brush)) })
    }

    private companion object {
        const val DEFAULT_SIZE = 16.0
    }
}

/** Builds the XAML that draws an [SvgIcon]. */
internal object SvgIconXaml {
    /** The default foreground: the theme's primary text color. */
    const val THEME_BRUSH = "{ThemeResource TextFillColorPrimaryBrush}"

    /** The XAML of a Viewbox of [size] that draws [icon]; [foreground] is a XAML brush value (#AARRGGBB or a resource). */
    fun build(icon: SvgIcon, size: Double, foreground: String = THEME_BRUSH): String = buildString {
        val s = Xaml.num(size)
        append("<Viewbox Width=\"").append(s).append("\" Height=\"").append(s)
        append("\" Stretch=\"Uniform\" IsHitTestVisible=\"False\"><Canvas Width=\"").append(Xaml.num(icon.width))
        append("\" Height=\"").append(Xaml.num(icon.height)).append("\">")
        for (path in icon.paths) {
            append("<Path Data=\"").append(Xaml.escape(path.data)).append('"')
            if (path.isFilled) append(" Fill=\"").append(path.fillColor ?: foreground).append('"')
            if (path.isStroked) {
                append(" Stroke=\"").append(path.strokeColor ?: foreground).append("\" StrokeThickness=\"").append(Xaml.num(path.strokeWidth))
                append("\" StrokeStartLineCap=\"Round\" StrokeEndLineCap=\"Round\" StrokeLineJoin=\"Round\"")
            }
            if (path.opacity < 1) append(" Opacity=\"").append(Xaml.num(path.opacity)).append('"')
            append(" />")
        }
        append("</Canvas></Viewbox>")
    }
}
