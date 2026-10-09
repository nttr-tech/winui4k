package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonChromeStyle
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonIcon
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonThemeStyle
import java.util.function.Consumer

/**
 * Replaces icons that cannot be shown in menu items (line art, multicolor layers, text) with icons that can
 * (RibbonSpace's RibbonItemHelper.MenuIconConverter).
 */
fun interface RibbonMenuIconConverter {
    /** The icon to show in menus instead of [icon] (a glyph, a filled path or an image). null to keep it as is. */
    fun convert(icon: RibbonIcon): RibbonIcon?
}

/**
 * Runtime theme settings for ribbon controls ([WRibbon] and others; RibbonSpace's RibbonTheme).
 *
 * Switches the palette (the app's brand color), the decoration color scheme (colorful / neutral) and the surface style
 * (Office / CAD). Brushes are rewritten in place, so controls already shown follow immediately. However, the shape
 * aspects of the surface style (rounded corners, padding) apply only to controls created afterwards, so switch it before
 * building pages. Light / dark follows each element's [WComponent.requestedTheme] (or [setTheme]). Use from the UI thread.
 */
object WRibbonTheme {
    private val changedListeners = mutableListOf<Runnable>()

    /** The current palette. */
    @JvmStatic
    val palette: RibbonThemePalette get() = RibbonThemeResources.palette

    /** The current chrome coloring. */
    @JvmStatic
    val chromeStyle: RibbonChromeStyle get() = RibbonThemeResources.chromeStyle

    /** The current surface style (Office / CAD). */
    @JvmStatic
    val style: RibbonThemeStyle get() = RibbonThemeResources.style

    /** The keys of all brushes defined by the ribbon (they can also be referenced from the app's XAML with `{ThemeResource key}`). */
    @JvmStatic
    val brushKeys: List<String> get() = RibbonThemeColors.BRUSH_KEYS

    /** The keys of shape resources that change with the surface style. */
    @JvmStatic
    val shapeKeys: List<String> get() = RibbonThemeColors.SHAPE_KEYS

    /**
     * Merges the ribbon's theme resources into Application.Resources (does nothing if already merged).
     * Called automatically when a ribbon control is created. Call it when the app's XAML uses ribbon brushes before that.
     */
    @JvmStatic
    fun ensureResources() = RibbonThemeResources.ensure()

    /** Applies a palette (e.g. [RibbonThemePalette.EXCEL]), keeping the decoration color scheme. */
    @JvmStatic
    fun applyPalette(palette: RibbonThemePalette) = apply(palette, chromeStyle, style)

    /** Applies an arbitrary accent color (the dark-theme color is generated automatically). */
    @JvmStatic
    fun applyAccent(accent: RibbonColor) = applyPalette(RibbonThemePalette.fromAccent(accent))

    /** Switches between accent-colored (colorful) decoration and neutral decoration. */
    @JvmStatic
    fun applyChromeStyle(chromeStyle: RibbonChromeStyle) = apply(palette, chromeStyle, style)

    /** Switches the surface style (Office / AutoCAD-style CAD). */
    @JvmStatic
    fun applyStyle(style: RibbonThemeStyle) = apply(palette, chromeStyle, style)

    /** Applies a palette and a decoration color scheme. */
    @JvmStatic
    fun apply(palette: RibbonThemePalette, chromeStyle: RibbonChromeStyle) = apply(palette, chromeStyle, style)

    /** Applies a palette, a decoration color scheme and a surface style together. */
    @JvmStatic
    fun apply(palette: RibbonThemePalette, chromeStyle: RibbonChromeStyle, style: RibbonThemeStyle) {
        RibbonThemeResources.apply(palette, chromeStyle, style)
        for (listener in changedListeners.toList()) listener.run()
    }

    /** Overrides the color of one brush for [theme] ("Light" / "Dark") (reverted the next time a palette is applied). */
    @JvmStatic
    @JvmOverloads
    fun setBrushColor(key: String, color: RibbonColor, theme: String = RibbonThemeResources.THEME_LIGHT) {
        ensureResources()
        RibbonThemeResources.setColor(key, color, theme)
    }

    /** The current color of the [key] brush for [theme] ("Light" / "Dark"). null for an unknown key. */
    @JvmStatic
    @JvmOverloads
    fun getBrushColor(key: String, theme: String = RibbonThemeResources.THEME_LIGHT): RibbonColor? = RibbonThemeResources.getColor(key, theme)

    /**
     * The color of the [key] brush in the theme of [scope] (ActualTheme; light if null) (RibbonSpace's RibbonTheme.GetBrush).
     * Instead of the app-wide color, this gives the color for elements or popups partially made dark with [setTheme] or
     * requestedTheme. null for an unknown key.
     */
    @JvmStatic
    fun getBrushColor(scope: WComponent?, key: String): RibbonColor? =
        RibbonThemeResources.getColor(key, if (scope?.actualTheme == ElementTheme.DARK) RibbonThemeResources.THEME_DARK else RibbonThemeResources.THEME_LIGHT)

    /**
     * Sets the color of the [key] brush in the theme of [target] with [apply], and sets it again whenever the light / dark
     * theme of [target] or the palette changes (RibbonSpace's RibbonTheme.SetThemeBrush). Use it to match the colors of
     * surfaces and text created in code to the ribbon's theme.
     * Example: `WRibbonTheme.setThemeBrush(border, "RibbonPopupBackgroundBrush") { border.background = it }`
     */
    @JvmStatic
    fun setThemeBrush(target: WComponent, key: String, apply: Consumer<WColor>) {
        require(key in RibbonThemeColors.BRUSH_KEYS) { "Not a ribbon brush key: $key" }
        val update = Runnable { getBrushColor(target, key)?.let { apply.accept(WColor(it.r, it.g, it.b, it.a)) } }
        update.run()
        target.addActualThemeChangedListener { update.run() }
        addChangedListener(update)
    }

    /**
     * The value of the corner radius resource [key] that changes with the surface style (e.g. "RibbonPopupCornerRadius")
     * (RibbonSpace's RibbonTheme.GetCornerRadius). In the order top-left, top-right, bottom-right, bottom-left. Four
     * [fallback] values for an unknown key.
     */
    @JvmStatic
    @JvmOverloads
    fun getCornerRadius(key: String, fallback: Double = 0.0): DoubleArray {
        val value = RibbonThemeColors.shapes(style)[key]?.takeIf { it.first == "CornerRadius" }?.second
            ?: return doubleArrayOf(fallback, fallback, fallback, fallback)
        val parts = value.split(',').map { it.trim().toDouble() }
        return if (parts.size == CORNERS) parts.toDoubleArray() else DoubleArray(CORNERS) { parts[0] }
    }

    private const val CORNERS = 4

    /**
     * Converts the icons of drop-down menu items (RibbonSpace's RibbonItemHelper.MenuIconConverter).
     * Menu items can only hold a single-color IconElement, so line art and multicolor layered icons cannot be shown as is.
     * When set, it is applied first to the icons of all menu items, and the returned icon (a glyph, a filled path or an
     * image) is shown.
     */
    @JvmStatic
    var menuIconConverter: RibbonMenuIconConverter? = null

    /** Sets light / dark for [root] and its descendants (usually the window's content). */
    @JvmStatic
    fun setTheme(root: WComponent, theme: ElementTheme) {
        root.requestedTheme = theme
    }

    /**
     * Wraps [content] in a surface (Border) whose background is the ribbon brush [brushKey] (equivalent to RibbonSpace's
     * RibbonTheme.SetThemeBrush). Follows light / dark / high contrast and palette changes (window backgrounds, the
     * surface next to the status bar, etc.).
     */
    @JvmStatic
    @JvmOverloads
    fun surface(content: WComponent?, brushKey: String = "RibbonWindowBackgroundBrush"): WComponent {
        require(brushKey in RibbonThemeColors.BRUSH_KEYS) { "Not a ribbon brush key: $brushKey" }
        val border = XamlElement(RibbonThemeResources.load("<Border Background=\"{ThemeResource $brushKey}\" />"))
        content?.let {
            Xaml.detach(it)
            border.setChild(it)
        }
        return border
    }

    /** Text whose foreground is the ribbon brush [brushKey] (follows the theme). */
    @JvmStatic
    @JvmOverloads
    fun label(text: String, brushKey: String = "RibbonForegroundBrush", fontSize: Double = 12.0): WComponent {
        require(brushKey in RibbonThemeColors.BRUSH_KEYS) { "Not a ribbon brush key: $brushKey" }
        return XamlElement(
            RibbonThemeResources.load(
                "<TextBlock Text=\"${Xaml.escape(text)}\" FontSize=\"${Xaml.num(fontSize)}\" TextWrapping=\"Wrap\" Foreground=\"{ThemeResource $brushKey}\" />",
            ),
        )
    }

    /** Subscribes to notifications when the palette, decoration color scheme or surface style changes. */
    @JvmStatic
    fun addChangedListener(listener: Runnable) {
        changedListeners += listener
    }

    /** Removes a listener registered with [addChangedListener]. */
    @JvmStatic
    fun removeChangedListener(listener: Runnable) {
        changedListeners -= listener
    }
}
