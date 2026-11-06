package com.appkitbox.winui4k

/**
 * ScrollPaneConstants-like: the scroll bar policy constants of [WScrollPane]. The values are the same as Swing's ScrollPaneConstants.
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
    }
}
