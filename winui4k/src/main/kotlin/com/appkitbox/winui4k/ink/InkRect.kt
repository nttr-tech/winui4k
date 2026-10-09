package com.appkitbox.winui4k.ink

/**
 * Like java.awt.geom.Rectangle2D: a rectangle returned by ink operations (Windows.Foundation.Rect, in DIPs).
 *
 * Represents the bounding rectangle of all strokes, or the area that needs redrawing after a selection, move, delete,
 * or paste. When there is no such area, [isEmpty] is true (WinUI's Rect.Empty).
 */
data class InkRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    /** Whether the rectangle is empty (width or height less than 0; WinUI's Rect.Empty has negative infinite width and height). */
    val isEmpty: Boolean
        get() = width < 0.0 || height < 0.0

    companion object {
        /** The empty rectangle (like Rect.Empty, positive infinite position and negative infinite width and height). */
        @JvmField
        val EMPTY = InkRect(
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
        )
    }
}
