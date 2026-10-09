package com.appkitbox.winui4k.ink

/**
 * Like Windows.UI.Input.Inking.InkPoint: one point of a stroke (position, pressure, tilt, and time).
 *
 * Positions are in DIPs (device-independent pixels), with the origin at the top-left of the canvas drawn on.
 * Since WinUI holds positions as float (Windows.Foundation.Point), they are rounded to float precision when passed to
 * the canvas.
 */
data class InkPoint @JvmOverloads constructor(
    /** The X coordinate (DIPs). */
    val x: Double,
    /** The Y coordinate (DIPs). */
    val y: Double,
    /** The pressure (0.0 to 1.0). Input without pressure (mouse, etc.) uses [DEFAULT_PRESSURE]. */
    val pressure: Float = DEFAULT_PRESSURE,
    /** The tilt along the X axis (degrees, -90 to 90). */
    val tiltX: Float = 0f,
    /** The tilt along the Y axis (degrees, -90 to 90). */
    val tiltY: Float = 0f,
    /** The input time (microseconds, on the same timeline as PointerPoint.Timestamp). 0 if unknown. */
    val timestamp: Long = 0L,
) {
    init {
        require(pressure in 0f..1f) { "pressure must be between 0.0 and 1.0: $pressure" }
        require(tiltX in -MAX_TILT..MAX_TILT) { "tiltX must be between -90 and 90: $tiltX" }
        require(tiltY in -MAX_TILT..MAX_TILT) { "tiltY must be between -90 and 90: $tiltY" }
    }

    companion object {
        /** The default pressure (0.5, the same as the InkPoint default). */
        const val DEFAULT_PRESSURE: Float = 0.5f

        private const val MAX_TILT = 90f
    }
}
