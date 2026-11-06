package com.appkitbox.winui4k

/**
 * BoundedRangeModel-style: a model of a range of values that sliders and progress bars can share.
 *
 * The value ([value]), extent ([extent]), minimum ([minimum]) and maximum ([maximum]) always satisfy
 * `minimum <= value <= value + extent <= maximum` (implemented by [WDefaultBoundedRangeModel]). Swing uses `int`, but
 * the values of WinUI4K's [WSlider] and [WProgressBar] are `Double`, so this model uses `Double` too. Writing
 * `setValue(5)` from Java compiles as is, but `int v = model.getValue()` does not, because it is an implicit conversion
 * from `double` to `int` (write `(int) model.getValue()`).
 */
interface WBoundedRangeModel {
    /** The current value. Stays at or above [minimum] and at or below `maximum - extent`. */
    var value: Double

    /** The minimum of the range. */
    var minimum: Double

    /** The maximum of the range. */
    var maximum: Double

    /** The extent the value covers (the length of the thumb). 0 or more. */
    var extent: Double

    /** Whether the thumb is being dragged. Corresponds to Swing's `getValueIsAdjusting` / `setValueIsAdjusting`. */
    var valueIsAdjusting: Boolean

    /** Sets the value, extent, minimum, maximum and whether it is being dragged as one change (notifies only once, when something changed). */
    fun setRangeProperties(newValue: Double, newExtent: Double, newMin: Double, newMax: Double, adjusting: Boolean)

    /** Adds a change listener (adding the same listener twice makes it called twice). */
    fun addChangeListener(listener: WChangeListener)

    /** Removes one listener added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener)
}
