package com.appkitbox.winui4k

/**
 * Adjustable-like: the point for reading and writing something with a value in a range (a scroll bar). Implemented by [WScrollPane.verticalScrollBar] etc.
 * Values are in DIP rounded to integers. Unlike Swing's Adjustable, the minimum, maximum and visible amount are determined by the content and viewport, so they cannot be written.
 * It has no per-line / per-page increments (unitIncrement / blockIncrement), since ScrollViewer moves by fixed amounts.
 */
interface WAdjustable {
    /** The orientation ([HORIZONTAL] / [VERTICAL]). */
    val orientation: Int

    /** The minimum (Adjustable.getMinimum). */
    val minimum: Int

    /** The maximum (Adjustable.getMaximum). The value moves from 0 to maximum - [visibleAmount]. */
    val maximum: Int

    /** The current value (Adjustable.getValue / setValue). */
    var value: Int

    /** The visible amount (Adjustable.getVisibleAmount). */
    val visibleAmount: Int

    /** Whether the value is in the middle of moving by dragging, inertial scrolling, etc. (Adjustable.getValueIsAdjusting). */
    val valueIsAdjusting: Boolean

    /** Adds a [WAdjustmentListener] called when the value changes. */
    fun addAdjustmentListener(listener: WAdjustmentListener)

    /** Removes one listener added with [addAdjustmentListener]. */
    fun removeAdjustmentListener(listener: WAdjustmentListener)

    /** The listeners currently added (in the order added). */
    fun getAdjustmentListeners(): Array<WAdjustmentListener>

    companion object {
        /** Horizontal (the same value as Adjustable.HORIZONTAL). */
        const val HORIZONTAL = 0

        /** Vertical (the same value as Adjustable.VERTICAL). */
        const val VERTICAL = 1

        /** No orientation (the same value as Adjustable.NO_ORIENTATION). */
        const val NO_ORIENTATION = 2
    }
}
