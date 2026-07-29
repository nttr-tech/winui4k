package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.WColor;

// Colors equivalent to the WinUI 3 Gallery's theme resources (returns the light or dark value for the current theme).
// Pages are rebuilt on every navigation, so they are painted with the theme colors as of the time they are read.
// Repainting on a theme change is handled by ActualThemeChanged in main() (which rebuilds the page being shown).
final class GalleryTheme {
    private GalleryTheme() {
    }

    /** Whether the current app theme is dark. Updated by the root element's ActualThemeChanged. */
    static boolean isDarkTheme = false;

    /** The content area's background (translucent, matching LayerFillColorDefault). The Mica behind it shows through faintly. */
    static WColor PAGE_BACKGROUND() {
        return isDarkTheme ? new WColor(58, 58, 58, 76) : new WColor(255, 255, 255, 128);
    }

    /** The background of cards that host demos (translucent, matching CardBackgroundFillColorDefault). */
    static WColor CARD_BACKGROUND() {
        return isDarkTheme ? new WColor(255, 255, 255, 13) : new WColor(255, 255, 255, 179);
    }

    /** A card's border (matching CardStrokeColorDefault). */
    static WColor CARD_BORDER() {
        return isDarkTheme ? new WColor(0, 0, 0, 25) : new WColor(229, 229, 229, 255);
    }

    /** A subdued text color for things like page descriptions (matching TextFillColorSecondary). */
    static WColor TEXT_SECONDARY() {
        return isDarkTheme ? new WColor(255, 255, 255, 197) : new WColor(97, 97, 97, 255);
    }
}
