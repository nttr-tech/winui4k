package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.extension.ribbon.model.RibbonChromeStyle
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNotifications
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemeStyle
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.util.concurrent.Executor

/**
 * The ribbon theme's resources (RibbonThemeResources in RibbonSpace).
 *
 * Combines the Light, Default, Dark, and HighContrast theme dictionaries (brushes and shapes) and the shared styles into
 * one ResourceDictionary, and merges it into Application.Resources when the first ribbon control is created. A palette
 * change rewrites the colors of the brush instances, so already-created controls follow immediately. Shapes (corner radii
 * and margins) apply to controls whose templates are applied afterward. Use only from the UI thread.
 */
internal object RibbonThemeResources {
    /** Merges the theme resources and then loads [xaml] (the root of a component that refers to the resources). */
    fun load(xaml: String): com.appkitbox.winui4k.internal.com.ComPtr {
        ensure()
        return Xaml.load(xaml)
    }

    /** The current palette. */
    var palette: RibbonThemePalette = RibbonThemePalette.WORD
        private set

    /** The current chrome coloring. */
    var chromeStyle: RibbonChromeStyle = RibbonChromeStyle.NEUTRAL
        private set

    /** The current surface look. */
    var style: RibbonThemeStyle = RibbonThemeStyle.OFFICE
        private set

    /** The merged dictionary (IResourceDictionary). null if not merged yet. */
    private var dictionary: ComPtr? = null

    /** Theme name → its dictionary (IMap<Object, Object>). */
    private val themeMaps = LinkedHashMap<String, ComPtr>()

    /** "theme name/key" → brush (ISolidColorBrush). High contrast is not included (it refers to system colors). */
    private val brushes = HashMap<String, ComPtr>()

    /**
     * The Light, Default, and Dark theme names (Default has the same colors as Light; WinUI cannot put one dictionary
     * under two keys).
     */
    private val COLOR_THEMES = listOf(THEME_LIGHT, THEME_DEFAULT, THEME_DARK)

    /** Merges the resources into Application.Resources (does nothing if already merged). */
    fun ensure() {
        if (RibbonNotifications.dispatcher == null) {
            // Even if the model is changed on another thread, views and listeners receive notifications on the UI thread
            RibbonNotifications.dispatcher = Executor { notification ->
                if (WinUiUtilities.isDispatchThread) notification.run() else WinUiUtilities.invokeLater(notification)
            }
        }
        if (dictionary != null) return
        val loaded = Xaml.load(RibbonStyles.dictionaryXaml(themeBodies()))
        val view = loaded.queryInterface(XamlInterop.IID_IResourceDictionary)
        loaded.release()
        dictionary = view
        WinUiUtilities.mergeApplicationDictionary(view)
        val themes = view.getPtr(XamlInterop.IResourceDictionary_get_ThemeDictionaries)
        try {
            for (theme in COLOR_THEMES + THEME_HIGH_CONTRAST) {
                val map = lookupAs(themes, theme, FoundationInterop.IID_IMap_Object_Object) ?: continue
                themeMaps[theme] = map
                if (theme != THEME_HIGH_CONTRAST) collectBrushes(theme, map)
            }
        } finally {
            themes.release()
        }
    }

    /** Records the brush instances of the theme dictionary [map] (to rewrite their colors when the palette changes). */
    private fun collectBrushes(theme: String, map: ComPtr) {
        for (key in RibbonThemeColors.BRUSH_KEYS) {
            lookupAs(map, key, XamlInterop.IID_ISolidColorBrush)?.let { brushes["$theme/$key"] = it }
        }
    }

    /** Applies the palette, the chrome coloring, and the surface look. The brushes are rewritten in place. */
    fun apply(palette: RibbonThemePalette, chromeStyle: RibbonChromeStyle, style: RibbonThemeStyle) {
        val styleChanged = style != this.style
        this.palette = palette
        this.chromeStyle = chromeStyle
        this.style = style
        if (dictionary == null) return
        val light = RibbonThemeColors.light(palette, chromeStyle, style)
        val dark = RibbonThemeColors.dark(palette, chromeStyle, style)
        for ((key, color) in light) {
            setColor(key, color, THEME_LIGHT)
            setColor(key, color, THEME_DEFAULT)
        }
        for ((key, color) in dark) setColor(key, color, THEME_DARK)
        if (styleChanged) replaceShapes(style)
    }

    /** Overrides the color of one brush for [theme] ("Light" / "Dark"; "Light" also applies to the Default theme). */
    fun setColor(key: String, color: RibbonColor, theme: String) {
        val themes = if (theme == THEME_LIGHT) listOf(THEME_LIGHT, THEME_DEFAULT) else listOf(theme)
        for (name in themes) {
            val brush = brushes["$name/$key"] ?: continue
            XamlStructs.putColor(brush, XamlInterop.ISolidColorBrush_put_Color, color.a, color.r, color.g, color.b)
        }
    }

    /**
     * The current color of the [key] brush of [theme]. If the resources are not merged yet, the value computed from the
     * palette.
     */
    fun getColor(key: String, theme: String): RibbonColor? {
        val brush = brushes["$theme/$key"]
        if (brush == null) {
            val colors = if (theme == THEME_DARK) {
                RibbonThemeColors.dark(palette, chromeStyle, style)
            } else {
                RibbonThemeColors.light(palette, chromeStyle, style)
            }
            return colors[key]
        }
        val (a, r, g, b) = XamlStructs.getColor(brush, XamlInterop.ISolidColorBrush_get_Color).toList()
        return RibbonColor(a, r, g, b)
    }

    /** The XAML of the content of each theme dictionary (brushes and shapes). */
    private fun themeBodies(): Map<String, String> {
        val light = RibbonThemeColors.light(palette, chromeStyle, style)
        val dark = RibbonThemeColors.dark(palette, chromeStyle, style)
        val shapes = shapesXaml(style)
        fun colors(map: Map<String, RibbonColor>): String = buildString {
            for ((key, color) in map) append("      <SolidColorBrush x:Key=\"").append(key).append("\" Color=\"").append(color.toHex(true)).append("\" />\n")
        }
        val highContrast = buildString {
            for (key in RibbonThemeColors.BRUSH_KEYS) append("      ").append(RibbonThemeColors.highContrastBrushXaml(key)).append('\n')
        }
        return linkedMapOf(
            THEME_LIGHT to colors(light) + shapes,
            THEME_DEFAULT to colors(light) + shapes,
            THEME_DARK to colors(dark) + shapes,
            THEME_HIGH_CONTRAST to highContrast + shapes,
        )
    }

    private fun shapesXaml(style: RibbonThemeStyle): String = buildString {
        for ((key, value) in RibbonThemeColors.shapes(style)) {
            append("      <").append(value.first).append(" x:Key=\"").append(key).append("\">").append(value.second)
                .append("</").append(value.first).append(">\n")
        }
    }

    /**
     * Replaces the shape resources with the values for [style] (loads the new values as a small dictionary and Inserts
     * them into each theme dictionary).
     */
    private fun replaceShapes(style: RibbonThemeStyle) {
        val loaded = Xaml.load("<ResourceDictionary>\n${shapesXaml(style)}</ResourceDictionary>")
        val source = loaded.queryInterface(FoundationInterop.IID_IMap_Object_Object)
        loaded.release()
        try {
            for (key in RibbonThemeColors.SHAPE_KEYS) {
                val value = lookup(source, key) ?: continue
                themeMaps.values.forEach { insert(it, key, value) }
                value.release()
            }
        } finally {
            source.release()
        }
    }

    /** Returns the [lookup] result as a view of [iid] (the original reference is released). */
    private fun lookupAs(map: ComPtr, key: String, iid: String): ComPtr? {
        val value = lookup(map, key) ?: return null
        return try {
            value.queryInterface(iid)
        } finally {
            value.release()
        }
    }

    /** IMap<Object, Object>.Lookup (the key is a string). Returns null if not found. */
    private fun lookup(map: ComPtr, key: String): ComPtr? {
        val boxedKey = PropertyValues.boxString(key)
        return try {
            Ffi.backend.withScope { scope ->
                val found = scope.allocate(1, 1)
                map.call(FoundationInterop.IMap_HasKey, boxedKey.ptr, found)
                if (Ffi.backend.memory.getByte(found, 0) == 0.toByte()) null else map.getPtr(FoundationInterop.IMap_Lookup, boxedKey.ptr)
            }
        } finally {
            boxedKey.release()
        }
    }

    private fun insert(map: ComPtr, key: String, value: ComPtr) {
        val boxedKey = PropertyValues.boxString(key)
        try {
            Ffi.backend.withScope { scope ->
                val replaced = scope.allocate(1, 1)
                map.call(FoundationInterop.IMap_Insert, boxedKey.ptr, value.ptr, replaced)
            }
        } finally {
            boxedKey.release()
        }
    }

    const val THEME_LIGHT = "Light"
    const val THEME_DEFAULT = "Default"
    const val THEME_DARK = "Dark"
    const val THEME_HIGH_CONTRAST = "HighContrast"
}
