package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonChromeStyle
import com.appkitbox.winui4k.ribbon.RibbonColor
import com.appkitbox.winui4k.ribbon.RibbonThemePalette
import com.appkitbox.winui4k.ribbon.RibbonThemeStyle

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

    /** Sets light / dark for [root] and its descendants (usually the window's content). */
    @JvmStatic
    fun setTheme(root: WComponent, theme: ElementTheme) {
        root.requestedTheme = theme
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
