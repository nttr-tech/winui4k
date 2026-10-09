package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonIcon

/**
 * A component that draws a ribbon icon ([RibbonIcon]: a glyph, a path, layered line art, an image or text) anywhere
 * (RibbonSpace's RibbonIconPresenter). Used for title bar buttons, combo box items, decorations on drawings, and so on.
 * Layers without a color follow the theme's icon color (RibbonIconBrush) or [foreground].
 */
class WRibbonIcon @JvmOverloads constructor(
    icon: RibbonIcon? = null,
    size: Double = DEFAULT_SIZE,
) : WComponent(RibbonThemeResources.load("<Border IsHitTestVisible=\"False\" />")) {
    private val root: XamlElement = XamlElement(inspectable.also { it.addRef() })

    /** The icon to draw (nothing is drawn if null). */
    var icon: RibbonIcon? = icon
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

    /** The color of layers without a color (the theme's icon color if null). */
    var foreground: WColor? = null
        set(value) {
            field = value
            rebuild()
        }

    init {
        rebuild()
    }

    private fun rebuild() {
        val brush = foreground?.let { hex(it) } ?: RibbonIconXaml.ICON_BRUSH
        val xaml = RibbonIconXaml.build(icon, iconSize, brush)
        root.setChild(xaml?.let { XamlElement.load(it) })
    }

    private companion object {
        const val DEFAULT_SIZE = 16.0
    }
}

/** Converts a [WColor] to a XAML color string (#AARRGGBB). */
private fun hex(color: WColor): String = "#%02X%02X%02X%02X".format(color.alpha, color.red, color.green, color.blue)
