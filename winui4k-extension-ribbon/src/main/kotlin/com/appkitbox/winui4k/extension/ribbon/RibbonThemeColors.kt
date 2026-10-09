package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.extension.ribbon.model.RibbonChromeStyle
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColor
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemeStyle

/**
 * Computes the colors of the ribbon theme's brushes and its shape resources (pure functions independent of the UI).
 *
 * The values are the same as RibbonThemeResources in RibbonSpace. The light / dark colors are determined by the palette
 * ([RibbonThemePalette]), the chrome coloring ([RibbonChromeStyle]), and the surface look ([RibbonThemeStyle]).
 * High contrast is not computed here, because it refers to the system colors (SystemColor*) directly in XAML.
 */
internal object RibbonThemeColors {
    /** The keys of all brushes the ribbon defines. */
    val BRUSH_KEYS: List<String> = listOf(
        "RibbonChromeBackgroundBrush", "RibbonChromeForegroundBrush", "RibbonCommandBarBackgroundBrush", "RibbonCommandBarBorderBrush",
        "RibbonForegroundBrush", "RibbonSecondaryForegroundBrush", "RibbonDisabledForegroundBrush", "RibbonIconBrush",
        "RibbonAccentBrush", "RibbonAccentForegroundBrush", "RibbonAccentSubtleBrush", "RibbonAccentSubtleStrongBrush", "RibbonAccentTextBrush",
        "RibbonItemHoverBrush", "RibbonItemPressedBrush", "RibbonItemCheckedBrush", "RibbonItemCheckedHoverBrush", "RibbonItemCheckedBorderBrush",
        "RibbonItemBorderHoverBrush", "RibbonSeparatorBrush", "RibbonTabForegroundBrush", "RibbonTabHoverBrush", "RibbonTabSelectedForegroundBrush",
        "RibbonTabIndicatorBrush", "RibbonPopupBackgroundBrush", "RibbonPopupBorderBrush", "RibbonInputBackgroundBrush", "RibbonInputBorderBrush",
        "RibbonInputHoverBorderBrush", "RibbonInputFocusBorderBrush", "RibbonKeyTipBackgroundBrush", "RibbonKeyTipForegroundBrush", "RibbonKeyTipBorderBrush",
        "RibbonTitleBarBackgroundBrush", "RibbonTitleBarForegroundBrush", "RibbonTitleBarHoverBrush", "RibbonTitleBarIconBrush", "RibbonBackstagePaneBackgroundBrush",
        "RibbonBackstagePaneForegroundBrush", "RibbonBackstagePaneHoverBrush", "RibbonBackstagePaneSelectedBrush", "RibbonBackstageContentBackgroundBrush",
        "RibbonGalleryItemBorderBrush", "RibbonGalleryItemSelectedBorderBrush", "RibbonScreenTipBackgroundBrush", "RibbonScreenTipBorderBrush",
        "RibbonFocusBrush", "RibbonWindowBackgroundBrush", "RibbonStatusBarBackgroundBrush", "RibbonStatusBarForegroundBrush", "RibbonToolBarBackgroundBrush",
        "RibbonSearchBackgroundBrush", "RibbonSearchBorderBrush", "RibbonSwatchBorderBrush", "RibbonShadowBrush", "RibbonScrollButtonBackgroundBrush",
        "RibbonGroupCaptionBackgroundBrush", "RibbonGroupCaptionForegroundBrush", "RibbonTabSelectedBackgroundBrush", "RibbonFloatingPanelBarBrush",
    )

    /** The keys of the shape resources that change with the surface look ([RibbonThemeStyle]). */
    val SHAPE_KEYS: List<String> = listOf(
        "RibbonControlCornerRadius", "RibbonCommandBarCornerRadius", "RibbonPopupCornerRadius", "RibbonTabCornerRadius", "RibbonGroupCaptionCornerRadius",
        "RibbonCommandBarMargin", "RibbonCommandBarBorderThickness", "RibbonTabMargin", "RibbonTabRowPadding", "RibbonGroupCaptionMargin",
    )

    /** The values of the shape resources (key → type name and XAML value). */
    fun shapes(style: RibbonThemeStyle): Map<String, Pair<String, String>> {
        val cad = style == RibbonThemeStyle.CAD
        return linkedMapOf(
            "RibbonControlCornerRadius" to ("CornerRadius" to if (cad) "2" else "4"),
            "RibbonCommandBarCornerRadius" to ("CornerRadius" to if (cad) "0" else "8"),
            "RibbonPopupCornerRadius" to ("CornerRadius" to if (cad) "2" else "6"),
            "RibbonTabCornerRadius" to ("CornerRadius" to if (cad) "2,2,0,0" else "4"),
            "RibbonGroupCaptionCornerRadius" to ("CornerRadius" to if (cad) "1" else "0"),
            "RibbonCommandBarMargin" to ("Thickness" to if (cad) "0" else "6,1,6,6"),
            "RibbonCommandBarBorderThickness" to ("Thickness" to if (cad) "0,0,0,1" else "1"),
            "RibbonTabMargin" to ("Thickness" to if (cad) "0,3,1,0" else "1,3,1,1"),
            "RibbonTabRowPadding" to ("Thickness" to if (cad) "4,0,4,0" else "6,0,6,0"),
            "RibbonGroupCaptionMargin" to ("Thickness" to if (cad) "1" else "0"),
        )
    }

    /** The brush colors of the light theme. */
    fun light(palette: RibbonThemePalette, chrome: RibbonChromeStyle, style: RibbonThemeStyle): Map<String, RibbonColor> {
        val m = LinkedHashMap<String, RibbonColor>()
        applyLight(m, palette, chrome)
        if (style == RibbonThemeStyle.CAD) applyCadLight(m, palette, chrome)
        return m
    }

    /** The brush colors of the dark theme. */
    fun dark(palette: RibbonThemePalette, chrome: RibbonChromeStyle, style: RibbonThemeStyle): Map<String, RibbonColor> {
        val m = LinkedHashMap<String, RibbonColor>()
        applyDark(m, palette, chrome)
        if (style == RibbonThemeStyle.CAD) applyCadDark(m, palette, chrome)
        return m
    }

    private fun c(hex: String): RibbonColor = RibbonColor.parse(hex)

    @Suppress("LongMethod") // A declarative table copying RibbonSpace's color table key by key
    private fun applyLight(m: MutableMap<String, RibbonColor>, palette: RibbonThemePalette, chrome: RibbonChromeStyle) {
        val accent = palette.getAccent(false)
        val onAccent = palette.getOnAccent(false)
        val colorful = chrome == RibbonChromeStyle.COLORFUL
        m["RibbonWindowBackgroundBrush"] = c("#F5F5F5")
        m["RibbonChromeBackgroundBrush"] = if (colorful) accent else c("#F5F5F5")
        m["RibbonChromeForegroundBrush"] = if (colorful) onAccent else c("#242424")
        m["RibbonCommandBarBackgroundBrush"] = c("#FFFFFF")
        m["RibbonCommandBarBorderBrush"] = c("#E0E0E0")
        m["RibbonForegroundBrush"] = c("#242424")
        m["RibbonSecondaryForegroundBrush"] = c("#616161")
        m["RibbonDisabledForegroundBrush"] = c("#BDBDBD")
        m["RibbonIconBrush"] = c("#424242")
        m["RibbonAccentBrush"] = accent
        m["RibbonAccentForegroundBrush"] = onAccent
        m["RibbonAccentSubtleBrush"] = palette.getAccentSubtle(false)
        m["RibbonAccentSubtleStrongBrush"] = palette.getAccentSubtleStrong(false)
        m["RibbonAccentTextBrush"] = accent.darken(0.1)
        m["RibbonItemHoverBrush"] = c("#F0F0F0")
        m["RibbonItemPressedBrush"] = c("#E0E0E0")
        m["RibbonItemCheckedBrush"] = palette.getAccentSubtle(false)
        m["RibbonItemCheckedHoverBrush"] = palette.getAccentSubtleStrong(false)
        m["RibbonItemCheckedBorderBrush"] = accent.lighten(0.55)
        m["RibbonItemBorderHoverBrush"] = c("#00000000")
        m["RibbonSeparatorBrush"] = c("#E0E0E0")
        m["RibbonTabForegroundBrush"] = if (colorful) onAccent else c("#424242")
        m["RibbonTabHoverBrush"] = if (colorful) accent.lighten(0.15) else c("#E8E8E8")
        m["RibbonTabSelectedForegroundBrush"] = if (colorful) onAccent else accent
        m["RibbonTabIndicatorBrush"] = if (colorful) onAccent else accent
        m["RibbonPopupBackgroundBrush"] = c("#FFFFFF")
        m["RibbonPopupBorderBrush"] = c("#D1D1D1")
        m["RibbonInputBackgroundBrush"] = c("#FFFFFF")
        m["RibbonInputBorderBrush"] = c("#D1D1D1")
        m["RibbonInputHoverBorderBrush"] = c("#A6A6A6")
        m["RibbonInputFocusBorderBrush"] = accent
        m["RibbonKeyTipBackgroundBrush"] = c("#3B3B3B")
        m["RibbonKeyTipForegroundBrush"] = c("#FFFFFF")
        m["RibbonKeyTipBorderBrush"] = c("#1F1F1F")
        m["RibbonTitleBarBackgroundBrush"] = if (colorful) accent else c("#F5F5F5")
        m["RibbonTitleBarForegroundBrush"] = if (colorful) onAccent else c("#242424")
        m["RibbonTitleBarHoverBrush"] = if (colorful) accent.lighten(0.15) else c("#E8E8E8")
        m["RibbonTitleBarIconBrush"] = if (colorful) onAccent else accent
        m["RibbonBackstagePaneBackgroundBrush"] = accent
        m["RibbonBackstagePaneForegroundBrush"] = onAccent
        m["RibbonBackstagePaneHoverBrush"] = accent.lighten(0.15)
        m["RibbonBackstagePaneSelectedBrush"] = accent.darken(0.25)
        m["RibbonBackstageContentBackgroundBrush"] = c("#FFFFFF")
        m["RibbonGalleryItemBorderBrush"] = c("#E0E0E0")
        m["RibbonGalleryItemSelectedBorderBrush"] = accent
        m["RibbonScreenTipBackgroundBrush"] = c("#FFFFFF")
        m["RibbonScreenTipBorderBrush"] = c("#C7C7C7")
        m["RibbonFocusBrush"] = c("#000000")
        m["RibbonStatusBarBackgroundBrush"] = c("#F5F5F5")
        m["RibbonStatusBarForegroundBrush"] = c("#424242")
        m["RibbonToolBarBackgroundBrush"] = c("#FAFAFA")
        m["RibbonSearchBackgroundBrush"] = if (colorful) accent.lighten(0.2) else c("#FFFFFF")
        m["RibbonSearchBorderBrush"] = if (colorful) accent.lighten(0.3) else c("#D1D1D1")
        m["RibbonSwatchBorderBrush"] = c("#33000000")
        m["RibbonShadowBrush"] = c("#1A000000")
        m["RibbonScrollButtonBackgroundBrush"] = c("#F2FFFFFF")
        m["RibbonGroupCaptionBackgroundBrush"] = c("#00FFFFFF")
        m["RibbonGroupCaptionForegroundBrush"] = c("#616161")
        m["RibbonTabSelectedBackgroundBrush"] = c("#00FFFFFF")
        m["RibbonFloatingPanelBarBrush"] = c("#F0F0F0")
    }

    @Suppress("LongMethod") // A declarative table copying RibbonSpace's color table key by key
    private fun applyDark(m: MutableMap<String, RibbonColor>, palette: RibbonThemePalette, chrome: RibbonChromeStyle) {
        val accent = palette.getAccent(true)
        val colorful = chrome == RibbonChromeStyle.COLORFUL
        val baseAccent = palette.getAccent(false)
        m["RibbonWindowBackgroundBrush"] = c("#1F1F1F")
        m["RibbonChromeBackgroundBrush"] = if (colorful) baseAccent.darken(0.35) else c("#1F1F1F")
        m["RibbonChromeForegroundBrush"] = c("#FFFFFF")
        m["RibbonCommandBarBackgroundBrush"] = c("#292929")
        m["RibbonCommandBarBorderBrush"] = c("#3D3D3D")
        m["RibbonForegroundBrush"] = c("#FFFFFF")
        m["RibbonSecondaryForegroundBrush"] = c("#C7C7C7")
        m["RibbonDisabledForegroundBrush"] = c("#6E6E6E")
        m["RibbonIconBrush"] = c("#E0E0E0")
        m["RibbonAccentBrush"] = accent
        m["RibbonAccentForegroundBrush"] = palette.getOnAccent(true)
        m["RibbonAccentSubtleBrush"] = palette.getAccentSubtle(true)
        m["RibbonAccentSubtleStrongBrush"] = palette.getAccentSubtleStrong(true)
        m["RibbonAccentTextBrush"] = accent
        m["RibbonItemHoverBrush"] = c("#383838")
        m["RibbonItemPressedBrush"] = c("#454545")
        m["RibbonItemCheckedBrush"] = palette.getAccentSubtle(true)
        m["RibbonItemCheckedHoverBrush"] = palette.getAccentSubtleStrong(true)
        m["RibbonItemCheckedBorderBrush"] = accent.withAlpha(0x88)
        m["RibbonItemBorderHoverBrush"] = c("#00000000")
        m["RibbonSeparatorBrush"] = c("#454545")
        m["RibbonTabForegroundBrush"] = c("#D6D6D6")
        m["RibbonTabHoverBrush"] = c("#2E2E2E")
        m["RibbonTabSelectedForegroundBrush"] = accent
        m["RibbonTabIndicatorBrush"] = accent
        m["RibbonPopupBackgroundBrush"] = c("#2B2B2B")
        m["RibbonPopupBorderBrush"] = c("#474747")
        m["RibbonInputBackgroundBrush"] = c("#1F1F1F")
        m["RibbonInputBorderBrush"] = c("#5C5C5C")
        m["RibbonInputHoverBorderBrush"] = c("#8A8A8A")
        m["RibbonInputFocusBorderBrush"] = accent
        m["RibbonKeyTipBackgroundBrush"] = c("#F0F0F0")
        m["RibbonKeyTipForegroundBrush"] = c("#1F1F1F")
        m["RibbonKeyTipBorderBrush"] = c("#FFFFFF")
        m["RibbonTitleBarBackgroundBrush"] = if (colorful) baseAccent.darken(0.35) else c("#1F1F1F")
        m["RibbonTitleBarForegroundBrush"] = c("#FFFFFF")
        m["RibbonTitleBarHoverBrush"] = c("#383838")
        m["RibbonTitleBarIconBrush"] = if (colorful) c("#FFFFFF") else accent
        m["RibbonBackstagePaneBackgroundBrush"] = c("#141414")
        m["RibbonBackstagePaneForegroundBrush"] = c("#FFFFFF")
        m["RibbonBackstagePaneHoverBrush"] = c("#2E2E2E")
        m["RibbonBackstagePaneSelectedBrush"] = accent.withAlpha(0x55)
        m["RibbonBackstageContentBackgroundBrush"] = c("#1F1F1F")
        m["RibbonGalleryItemBorderBrush"] = c("#454545")
        m["RibbonGalleryItemSelectedBorderBrush"] = accent
        m["RibbonScreenTipBackgroundBrush"] = c("#2B2B2B")
        m["RibbonScreenTipBorderBrush"] = c("#5C5C5C")
        m["RibbonFocusBrush"] = c("#FFFFFF")
        m["RibbonStatusBarBackgroundBrush"] = c("#1F1F1F")
        m["RibbonStatusBarForegroundBrush"] = c("#D6D6D6")
        m["RibbonToolBarBackgroundBrush"] = c("#262626")
        m["RibbonSearchBackgroundBrush"] = c("#2E2E2E")
        m["RibbonSearchBorderBrush"] = c("#474747")
        m["RibbonSwatchBorderBrush"] = c("#44FFFFFF")
        m["RibbonShadowBrush"] = c("#66000000")
        m["RibbonScrollButtonBackgroundBrush"] = c("#F2292929")
        m["RibbonGroupCaptionBackgroundBrush"] = c("#00000000")
        m["RibbonGroupCaptionForegroundBrush"] = c("#C7C7C7")
        m["RibbonTabSelectedBackgroundBrush"] = c("#00000000")
        m["RibbonFloatingPanelBarBrush"] = c("#333333")
    }

    // CAD: blue-gray surfaces in the style of AutoCAD-like apps. Only the surface and neutral colors are overridden; the
    // accent-derived brushes (checked, focus, selection) keep following the palette
    @Suppress("LongMethod") // A declarative table copying RibbonSpace's color table key by key
    private fun applyCadDark(m: MutableMap<String, RibbonColor>, palette: RibbonThemePalette, chrome: RibbonChromeStyle) {
        val accent = palette.getAccent(true)
        val colorful = chrome == RibbonChromeStyle.COLORFUL
        m["RibbonWindowBackgroundBrush"] = c("#2B313B")
        m["RibbonChromeBackgroundBrush"] = if (colorful) palette.getAccent(false).darken(0.35) else c("#2B313B")
        m["RibbonChromeForegroundBrush"] = c("#E1E6EC")
        m["RibbonTitleBarBackgroundBrush"] = if (colorful) palette.getAccent(false).darken(0.35) else c("#252A33")
        m["RibbonTitleBarForegroundBrush"] = c("#E1E6EC")
        m["RibbonTitleBarHoverBrush"] = c("#3B4453")
        m["RibbonCommandBarBackgroundBrush"] = c("#3B4453")
        m["RibbonCommandBarBorderBrush"] = c("#252A33")
        m["RibbonForegroundBrush"] = c("#E1E6EC")
        m["RibbonSecondaryForegroundBrush"] = c("#AEB8C4")
        m["RibbonDisabledForegroundBrush"] = c("#6C7787")
        m["RibbonIconBrush"] = c("#D8DEE6")
        m["RibbonItemHoverBrush"] = c("#4A5568")
        m["RibbonItemPressedBrush"] = c("#56637A")
        m["RibbonItemBorderHoverBrush"] = c("#5F6E86")
        m["RibbonSeparatorBrush"] = c("#2F3641")
        m["RibbonTabForegroundBrush"] = c("#C9D1DB")
        m["RibbonTabHoverBrush"] = c("#343C48")
        m["RibbonTabSelectedForegroundBrush"] = c("#FFFFFF")
        m["RibbonTabSelectedBackgroundBrush"] = c("#3B4453")
        m["RibbonTabIndicatorBrush"] = c("#00000000")
        m["RibbonPopupBackgroundBrush"] = c("#3B4453")
        m["RibbonPopupBorderBrush"] = c("#252A33")
        m["RibbonInputBackgroundBrush"] = c("#2B313B")
        m["RibbonInputBorderBrush"] = c("#56637A")
        m["RibbonInputHoverBorderBrush"] = c("#7A889E")
        m["RibbonInputFocusBorderBrush"] = accent
        m["RibbonKeyTipBackgroundBrush"] = c("#E8EDF2")
        m["RibbonKeyTipForegroundBrush"] = c("#1B2027")
        m["RibbonKeyTipBorderBrush"] = c("#FFFFFF")
        m["RibbonBackstagePaneBackgroundBrush"] = c("#252A33")
        m["RibbonBackstagePaneHoverBrush"] = c("#343C48")
        m["RibbonBackstageContentBackgroundBrush"] = c("#2B313B")
        m["RibbonGalleryItemBorderBrush"] = c("#4A5568")
        m["RibbonScreenTipBackgroundBrush"] = c("#3B4453")
        m["RibbonScreenTipBorderBrush"] = c("#252A33")
        m["RibbonStatusBarBackgroundBrush"] = c("#2B313B")
        m["RibbonStatusBarForegroundBrush"] = c("#C9D1DB")
        m["RibbonToolBarBackgroundBrush"] = c("#3B4453")
        m["RibbonSearchBackgroundBrush"] = c("#2B313B")
        m["RibbonSearchBorderBrush"] = c("#4A5568")
        m["RibbonScrollButtonBackgroundBrush"] = c("#F23B4453")
        m["RibbonGroupCaptionBackgroundBrush"] = c("#323A47")
        m["RibbonGroupCaptionForegroundBrush"] = c("#B8C2CE")
        m["RibbonFloatingPanelBarBrush"] = c("#2F3641")
    }

    @Suppress("LongMethod") // A declarative table copying RibbonSpace's color table key by key
    private fun applyCadLight(m: MutableMap<String, RibbonColor>, palette: RibbonThemePalette, chrome: RibbonChromeStyle) {
        val accent = palette.getAccent(false)
        val onAccent = palette.getOnAccent(false)
        val colorful = chrome == RibbonChromeStyle.COLORFUL
        m["RibbonWindowBackgroundBrush"] = c("#D5D8DD")
        m["RibbonChromeBackgroundBrush"] = if (colorful) accent else c("#DADDE2")
        m["RibbonChromeForegroundBrush"] = if (colorful) onAccent else c("#1F242B")
        m["RibbonTitleBarBackgroundBrush"] = if (colorful) accent else c("#CDD1D7")
        m["RibbonTitleBarForegroundBrush"] = if (colorful) onAccent else c("#1F242B")
        m["RibbonTitleBarHoverBrush"] = if (colorful) accent.lighten(0.15) else c("#BFC5CD")
        m["RibbonCommandBarBackgroundBrush"] = c("#F0F1F3")
        m["RibbonCommandBarBorderBrush"] = c("#B7BEC8")
        m["RibbonForegroundBrush"] = c("#1F242B")
        m["RibbonSecondaryForegroundBrush"] = c("#4E5866")
        m["RibbonDisabledForegroundBrush"] = c("#9AA3AF")
        m["RibbonIconBrush"] = c("#2F3742")
        m["RibbonItemHoverBrush"] = c("#DDE3EA")
        m["RibbonItemPressedBrush"] = c("#CBD3DD")
        m["RibbonItemBorderHoverBrush"] = c("#A9B4C2")
        m["RibbonSeparatorBrush"] = c("#C7CCD3")
        m["RibbonTabForegroundBrush"] = if (colorful) onAccent else c("#2F3742")
        m["RibbonTabHoverBrush"] = if (colorful) accent.lighten(0.15) else c("#E3E6EA")
        m["RibbonTabSelectedForegroundBrush"] = c("#000000")
        m["RibbonTabSelectedBackgroundBrush"] = c("#F0F1F3")
        m["RibbonTabIndicatorBrush"] = c("#00000000")
        m["RibbonPopupBackgroundBrush"] = c("#F7F8F9")
        m["RibbonPopupBorderBrush"] = c("#A9B4C2")
        m["RibbonInputBorderBrush"] = c("#B7BEC8")
        m["RibbonInputHoverBorderBrush"] = c("#8C97A5")
        m["RibbonInputFocusBorderBrush"] = accent
        m["RibbonBackstageContentBackgroundBrush"] = c("#F0F1F3")
        m["RibbonGalleryItemBorderBrush"] = c("#C7CCD3")
        m["RibbonScreenTipBackgroundBrush"] = c("#F7F8F9")
        m["RibbonScreenTipBorderBrush"] = c("#A9B4C2")
        m["RibbonStatusBarBackgroundBrush"] = c("#D5D8DD")
        m["RibbonStatusBarForegroundBrush"] = c("#2F3742")
        m["RibbonToolBarBackgroundBrush"] = c("#F0F1F3")
        m["RibbonSearchBackgroundBrush"] = if (colorful) accent.lighten(0.2) else c("#FFFFFF")
        m["RibbonSearchBorderBrush"] = if (colorful) accent.lighten(0.3) else c("#B7BEC8")
        m["RibbonScrollButtonBackgroundBrush"] = c("#F2F0F1F3")
        m["RibbonGroupCaptionBackgroundBrush"] = c("#DCE0E5")
        m["RibbonGroupCaptionForegroundBrush"] = c("#3F4855")
        m["RibbonFloatingPanelBarBrush"] = c("#DCE0E5")
    }

    /**
     * High-contrast brushes: XAML elements that refer to the system colors (SystemColor* ThemeResources).
     * Fills for interaction states (hover, pressed, checked) overlay the highlight color semi-transparently to keep the
     * contrast with the window text color (equivalent to RibbonSpace's blend of the background and highlight colors).
     */
    fun highContrastBrushXaml(key: String): String {
        val (color, opacity) = HIGH_CONTRAST[key] ?: ((if (key.contains("Background")) "SystemColorWindowColor" else "SystemColorWindowTextColor") to 1.0)
        if (color.isEmpty()) return "<SolidColorBrush x:Key=\"$key\" Color=\"Transparent\" />"
        val opacityAttribute = if (opacity < 1.0) " Opacity=\"${Xaml.num(opacity)}\"" else ""
        return "<SolidColorBrush x:Key=\"$key\" Color=\"{ThemeResource $color}\"$opacityAttribute />"
    }

    /**
     * Brushes that differ from the high-contrast default (window color for backgrounds, window text color otherwise)
     * (key → system color and opacity).
     */
    private val HIGH_CONTRAST: Map<String, Pair<String, Double>> = buildMap {
        fun put(color: String, opacity: Double, vararg keys: String) = keys.forEach { put(it, color to opacity) }
        put(
            "SystemColorHighlightColor", 1.0, "RibbonAccentBrush", "RibbonTabIndicatorBrush", "RibbonInputFocusBorderBrush",
            "RibbonGalleryItemSelectedBorderBrush", "RibbonItemCheckedBorderBrush", "RibbonTabSelectedForegroundBrush",
            "RibbonItemBorderHoverBrush", "RibbonFocusBrush", "RibbonKeyTipBackgroundBrush",
        )
        put("SystemColorButtonFaceColor", 1.0, "RibbonCommandBarBackgroundBrush", "RibbonScrollButtonBackgroundBrush")
        put("SystemColorHighlightTextColor", 1.0, "RibbonAccentForegroundBrush", "RibbonKeyTipForegroundBrush")
        put("SystemColorHotlightColor", 1.0, "RibbonAccentTextBrush")
        put("SystemColorGrayTextColor", 1.0, "RibbonDisabledForegroundBrush")
        put("SystemColorHighlightColor", 0.3, "RibbonItemHoverBrush", "RibbonTabHoverBrush", "RibbonTitleBarHoverBrush", "RibbonBackstagePaneHoverBrush")
        put("SystemColorHighlightColor", 0.4, "RibbonAccentSubtleBrush", "RibbonItemCheckedBrush")
        put(
            "SystemColorHighlightColor",
            0.5,
            "RibbonAccentSubtleStrongBrush",
            "RibbonItemPressedBrush",
            "RibbonItemCheckedHoverBrush",
            "RibbonBackstagePaneSelectedBrush",
        )
        put("", 0.0, "RibbonShadowBrush")
        put("SystemColorWindowTextColor", 1.0, "RibbonKeyTipBorderBrush", "RibbonSwatchBorderBrush")
    }
}
