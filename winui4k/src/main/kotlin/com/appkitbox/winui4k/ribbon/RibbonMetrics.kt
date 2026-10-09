package com.appkitbox.winui4k.ribbon

/**
 * Layout dimensions for each spacing density ([RibbonDensity]), in DIPs.
 * Passing them to [com.appkitbox.winui4k.WRibbon.customMetrics] draws the ribbon with custom dimensions.
 */
data class RibbonMetrics @JvmOverloads constructor(
    /** The height of the tab row. */
    val tabHeight: Double = 30.0,
    /** The height of the item area of a classic group. */
    val groupContentHeight: Double = 70.0,
    /** The height of the group caption row. */
    val groupCaptionHeight: Double = 17.0,
    /** The height of large items. */
    val largeItemHeight: Double = 68.0,
    /** The minimum width of large items. */
    val largeItemMinWidth: Double = 44.0,
    /** The height of medium and small row items. */
    val rowHeight: Double = 22.0,
    /** The size of large icons. */
    val largeIconSize: Double = 32.0,
    /** The size of small icons. */
    val smallIconSize: Double = 16.0,
    /** The row height of the simplified ribbon. */
    val simplifiedHeight: Double = 40.0,
    /** The height of items in the simplified ribbon and toolbars. */
    val simplifiedItemHeight: Double = 30.0,
    /** The font size. */
    val fontSize: Double = 12.0,
    /** The font size of captions. */
    val captionFontSize: Double = 11.0,
    /** The left and right padding of row items. */
    val itemPadding: Double = 5.0,
) {
    companion object {
        /** The default dimensions (for mouse). */
        @JvmField
        val COMFORTABLE = RibbonMetrics()

        /** Compact dimensions (smaller text and icons so that two-line large labels fit, with the same number of rows). */
        @JvmField
        val COMPACT = RibbonMetrics(
            tabHeight = 26.0, groupContentHeight = 64.0, groupCaptionHeight = 15.0, largeItemHeight = 62.0, largeItemMinWidth = 40.0,
            rowHeight = 20.0, largeIconSize = 26.0, smallIconSize = 16.0, simplifiedHeight = 34.0, simplifiedItemHeight = 26.0,
            fontSize = 11.0, captionFontSize = 10.0, itemPadding = 4.0,
        )

        /** Dimensions for touch. */
        @JvmField
        val TOUCH = RibbonMetrics(
            tabHeight = 38.0, groupContentHeight = 94.0, groupCaptionHeight = 19.0, largeItemHeight = 92.0, largeItemMinWidth = 56.0,
            rowHeight = 30.0, largeIconSize = 32.0, smallIconSize = 20.0, simplifiedHeight = 50.0, simplifiedItemHeight = 40.0,
            fontSize = 13.0, captionFontSize = 12.0, itemPadding = 8.0,
        )

        /** The dimensions for [density]. */
        @JvmStatic
        fun forDensity(density: RibbonDensity): RibbonMetrics = when (density) {
            RibbonDensity.COMPACT -> COMPACT
            RibbonDensity.TOUCH -> TOUCH
            RibbonDensity.COMFORTABLE -> COMFORTABLE
        }
    }
}
