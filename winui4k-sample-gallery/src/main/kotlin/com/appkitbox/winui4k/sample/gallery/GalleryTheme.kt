package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.WColor

// Colors matching the WinUI 3 Gallery's theme resources (return light or dark depending on the current theme).
// Pages are rebuilt on every navigation, so they're painted with whichever theme's color was in
// effect when built. Repainting on a theme change is handled by main()'s ActualThemeChanged (which
// rebuilds the visible page).

// Muted WinUI 3 Gallery-like colors used for demo tiles, ink and color choices. WColor.RED etc. are the same primary colors as java.awt.Color,
// so they are not used for on-screen examples.

/** The demo red. */
internal val DEMO_RED = WColor(237, 28, 36)

/** The demo green. */
internal val DEMO_GREEN = WColor(34, 177, 76)

/** The demo blue (Windows's default accent color). */
internal val DEMO_BLUE = WColor(0, 120, 215)

/** The demo yellow. */
internal val DEMO_YELLOW = WColor(255, 201, 14)

/** The demo orange. */
internal val DEMO_ORANGE = WColor(255, 127, 39)

/** Whether the current app theme is dark. Updated by the root element's ActualThemeChanged. */
internal var isDarkTheme = false

/** The content area's background (translucent, matching LayerFillColorDefault). The Mica behind it shows through faintly. */
internal val PAGE_BACKGROUND: WColor
    get() = if (isDarkTheme) WColor(58, 58, 58, 76) else WColor(255, 255, 255, 128)

/** The background of cards that host demos (translucent, matching CardBackgroundFillColorDefault). */
internal val CARD_BACKGROUND: WColor
    get() = if (isDarkTheme) WColor(255, 255, 255, 13) else WColor(255, 255, 255, 179)

/** A card's border (matching CardStrokeColorDefault). */
internal val CARD_BORDER: WColor
    get() = if (isDarkTheme) WColor(0, 0, 0, 25) else WColor(229, 229, 229)

/** A subdued text color for things like page descriptions (matching TextFillColorSecondary). */
internal val TEXT_SECONDARY: WColor
    get() = if (isDarkTheme) WColor(255, 255, 255, 197) else WColor(97, 97, 97)
