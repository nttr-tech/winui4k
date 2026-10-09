package com.appkitbox.winui4k.extension.ribbon.model

/**
 * Office-compatible color palette (theme colors with automatically generated tints and shades, plus the standard
 * colors). Swatch names are in English, the same as RibbonSpace (the view translates them for display).
 */
object RibbonColorPalette {
    /** Base colors of Office's default theme (Background 1 through Accent 6). */
    @JvmField
    val OFFICE_THEME_COLORS: List<RibbonColorSwatch> = listOf(
        swatch("#FFFFFF", "White, Background 1"),
        swatch("#000000", "Black, Text 1"),
        swatch("#E7E6E6", "Gray, Background 2"),
        swatch("#44546A", "Blue-Gray, Text 2"),
        swatch("#4472C4", "Blue, Accent 1"),
        swatch("#ED7D31", "Orange, Accent 2"),
        swatch("#A5A5A5", "Gray, Accent 3"),
        swatch("#FFC000", "Gold, Accent 4"),
        swatch("#5B9BD5", "Blue, Accent 5"),
        swatch("#70AD47", "Green, Accent 6"),
    )

    /** Office's 10 standard colors. */
    @JvmField
    val STANDARD_COLORS: List<RibbonColorSwatch> = listOf(
        swatch("#C00000", "Dark Red"),
        swatch("#FF0000", "Red"),
        swatch("#FFC000", "Orange"),
        swatch("#FFFF00", "Yellow"),
        swatch("#92D050", "Light Green"),
        swatch("#00B050", "Green"),
        swatch("#00B0F0", "Light Blue"),
        swatch("#0070C0", "Blue"),
        swatch("#002060", "Dark Blue"),
        swatch("#7030A0", "Purple"),
    )

    /** Highlighter colors (Word's [Text Highlight Color]). */
    @JvmField
    val HIGHLIGHT_COLORS: List<RibbonColorSwatch> = listOf(
        swatch("#FFFF00", "Yellow"),
        swatch("#00FF00", "Bright Green"),
        swatch("#00FFFF", "Turquoise"),
        swatch("#FF00FF", "Pink"),
        swatch("#0000FF", "Blue"),
        swatch("#FF0000", "Red"),
        swatch("#000080", "Dark Blue"),
        swatch("#008080", "Teal"),
        swatch("#008000", "Green"),
        swatch("#800080", "Violet"),
        swatch("#800000", "Dark Red"),
        swatch("#808000", "Dark Yellow"),
        swatch("#808080", "Gray 50%"),
        swatch("#C0C0C0", "Gray 25%"),
        swatch("#000000", "Black"),
    )

    /** Creates the 5 tints and shades of a theme color (the rows below the base color in the Office palette). */
    @JvmStatic
    fun generateShades(swatch: RibbonColorSwatch): List<RibbonColorSwatch> {
        val color = swatch.color
        val l = color.lightness
        // 5 steps of (whether to lighten, ratio). Uses the same steps as Office for white, black, dark colors, light colors and everything else
        val steps: List<Pair<Boolean, Double>> = when {
            l >= 0.99 -> listOf(false to .05, false to .15, false to .25, false to .35, false to .50)
            l <= 0.01 -> listOf(true to .50, true to .35, true to .25, true to .15, true to .05)
            l < 0.20 -> listOf(true to .90, true to .75, true to .50, true to .25, true to .10)
            l > 0.80 -> listOf(false to .10, false to .25, false to .50, false to .75, false to .90)
            else -> listOf(true to .80, true to .60, true to .40, false to .25, false to .50)
        }
        return steps.map { (lighter, amount) ->
            RibbonColorSwatch(
                if (lighter) color.lighten(amount) else color.darken(amount),
                "${swatch.name}, ${if (lighter) "Lighter" else "Darker"} ${Math.round(amount * 100)}%",
            )
        }
    }

    /**
     * Creates the Office theme grid: row 0 is the base colors, rows 1..5 are the generated tints and shades.
     * Returns a list of rows (each a list of columns). If [themeColors] is null, the Office theme is used.
     */
    @JvmStatic
    @JvmOverloads
    fun buildThemeGrid(themeColors: List<RibbonColorSwatch>? = null): List<List<RibbonColorSwatch>> {
        val colors = themeColors ?: OFFICE_THEME_COLORS
        val shades = colors.map { generateShades(it) }
        return listOf(colors.toList()) + (0 until SHADE_ROWS).map { row -> shades.map { it[row] } }
    }

    private const val SHADE_ROWS = 5

    private fun swatch(hex: String, name: String) = RibbonColorSwatch(RibbonColor.parse(hex), name)
}

/**
 * Accent palette of the ribbon theme. The presets match the brand colors of each Office app.
 */
data class RibbonThemePalette(
    /** Display name. */
    val name: String,
    /** Main accent (selected tab text, focus, checked state and the Backstage pane). */
    val accent: RibbonColor,
    /** Accent used in the dark theme (lighter, for contrast). */
    val accentDark: RibbonColor,
) {
    /** The accent for the theme. */
    fun getAccent(dark: Boolean): RibbonColor = if (dark) accentDark else accent

    /** A very light accent used for hover and checked backgrounds. */
    fun getAccentSubtle(dark: Boolean): RibbonColor = if (dark) accentDark.withAlpha(0x33) else accent.lighten(0.86)

    /** A slightly stronger accent used for pressed backgrounds and hover while checked. */
    fun getAccentSubtleStrong(dark: Boolean): RibbonColor = if (dark) accentDark.withAlpha(0x55) else accent.lighten(0.74)

    /** Foreground color (black or white) drawn on the accent color. */
    fun getOnAccent(dark: Boolean): RibbonColor = if (getAccent(dark).prefersDarkForeground) RibbonColor.BLACK else RibbonColor.WHITE

    companion object {
        /** Word blue. */
        @JvmField
        val WORD = palette("Word", "#185ABD", "#6CA0F5")

        /** Excel green. */
        @JvmField
        val EXCEL = palette("Excel", "#107C41", "#5CC689")

        /** PowerPoint red-orange. */
        @JvmField
        val POWER_POINT = palette("PowerPoint", "#C43E1C", "#F08B6C")

        /** Outlook blue. */
        @JvmField
        val OUTLOOK = palette("Outlook", "#0F6CBD", "#62ABF5")

        /** OneNote purple. */
        @JvmField
        val ONE_NOTE = palette("OneNote", "#7719AA", "#C38DE8")

        /** Access dark red. */
        @JvmField
        val ACCESS = palette("Access", "#A4373A", "#EB8487")

        /** Visio indigo. */
        @JvmField
        val VISIO = palette("Visio", "#3955A3", "#8DA6EE")

        /** Project green. */
        @JvmField
        val PROJECT = palette("Project", "#31752F", "#7DC47A")

        /** Publisher teal. */
        @JvmField
        val PUBLISHER = palette("Publisher", "#077568", "#4FC4B6")

        /** Teams blue-violet. */
        @JvmField
        val TEAMS = palette("Teams", "#5B5FC7", "#9EA2FF")

        /** Neutral graphite for business tools (CAD, IDEs). */
        @JvmField
        val GRAPHITE = palette("Graphite", "#3B4758", "#9DB4D3")

        /** CAD blue (combine with [RibbonThemeStyle.CAD]). */
        @JvmField
        val CAD = palette("CAD", "#0A7CB8", "#3DA9F5")

        /** All built-in presets. */
        @JvmField
        val PRESETS: List<RibbonThemePalette> =
            listOf(WORD, EXCEL, POWER_POINT, OUTLOOK, ONE_NOTE, ACCESS, VISIO, PROJECT, PUBLISHER, TEAMS, GRAPHITE, CAD)

        /** Creates a palette from a single accent (the dark theme color is chosen automatically). */
        @JvmStatic
        @JvmOverloads
        fun fromAccent(accent: RibbonColor, name: String = "Custom"): RibbonThemePalette =
            RibbonThemePalette(name, accent, if (accent.lightness < 0.55) accent.lighten(0.45) else accent)

        private fun palette(name: String, accent: String, accentDark: String) =
            RibbonThemePalette(name, RibbonColor.parse(accent), RibbonColor.parse(accentDark))
    }
}
