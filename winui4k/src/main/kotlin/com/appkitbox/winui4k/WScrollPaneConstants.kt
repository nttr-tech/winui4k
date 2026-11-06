package com.appkitbox.winui4k

/**
 * ScrollPaneConstants-like: constants for the scroll bar policies and corner names of [WScrollPane]. The values are the same as Swing's ScrollPaneConstants.
 * [WScrollPane] implements this interface, so Java can write `WScrollPane.VERTICAL_SCROLLBAR_ALWAYS`.
 * In Kotlin, companion members are not inherited, so `WScrollPane.VERTICAL_SCROLLBAR_ALWAYS` refers to the same value in [WScrollPane]'s own companion.
 */
interface WScrollPaneConstants {
    companion object {
        /** Shows the vertical scroll bar only when the content overflows (ScrollBarVisibility.Auto). */
        const val VERTICAL_SCROLLBAR_AS_NEEDED = 20

        /** Does not show the vertical scroll bar (ScrollBarVisibility.Hidden; the content height stays as is, and it can be scrolled programmatically). */
        const val VERTICAL_SCROLLBAR_NEVER = 21

        /** Always shows the vertical scroll bar (ScrollBarVisibility.Visible). */
        const val VERTICAL_SCROLLBAR_ALWAYS = 22

        /** Shows the horizontal scroll bar only when the content overflows (ScrollBarVisibility.Auto). */
        const val HORIZONTAL_SCROLLBAR_AS_NEEDED = 30

        /** Does not show the horizontal scroll bar (ScrollBarVisibility.Hidden; the content width stays as is, and it can be scrolled programmatically). */
        const val HORIZONTAL_SCROLLBAR_NEVER = 31

        /** Always shows the horizontal scroll bar (ScrollBarVisibility.Visible). */
        const val HORIZONTAL_SCROLLBAR_ALWAYS = 32

        /** The top-left corner (a name for [WScrollPane.setCorner]; in ScrollViewer, the same corner as [UPPER_LEADING_CORNER]). */
        const val UPPER_LEFT_CORNER = "UPPER_LEFT_CORNER"

        /** The top-right corner (not supported in WinUI4K). */
        const val UPPER_RIGHT_CORNER = "UPPER_RIGHT_CORNER"

        /** The bottom-left corner (not supported in WinUI4K). */
        const val LOWER_LEFT_CORNER = "LOWER_LEFT_CORNER"

        /** The bottom-right corner (not supported in WinUI4K). */
        const val LOWER_RIGHT_CORNER = "LOWER_RIGHT_CORNER"

        /** The top corner on the leading side of lines (ScrollViewer.TopLeftHeader). */
        const val UPPER_LEADING_CORNER = "UPPER_LEADING_CORNER"

        /** The top corner on the trailing side of lines (not supported in WinUI4K). */
        const val UPPER_TRAILING_CORNER = "UPPER_TRAILING_CORNER"

        /** The bottom corner on the leading side of lines (not supported in WinUI4K). */
        const val LOWER_LEADING_CORNER = "LOWER_LEADING_CORNER"

        /** The bottom corner on the trailing side of lines (not supported in WinUI4K). */
        const val LOWER_TRAILING_CORNER = "LOWER_TRAILING_CORNER"
    }
}
