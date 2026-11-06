package com.appkitbox.winui4k

/**
 * DefaultBoundedRangeModel-style: the standard implementation of [WBoundedRangeModel].
 *
 * As in Swing, every setter keeps `minimum <= value <= value + extent <= maximum`.
 * - Setting [value] clamps it to the range (with 0..100 and an extent of 10, setting 150 gives 90)
 * - Setting [extent] clamps it to 0 or more and `maximum - value` or less
 * - Setting [minimum] raises a smaller value and the maximum (with a value of 50, setting the minimum to 60 makes the value 60 too)
 * - Setting [maximum] lowers a larger minimum and extent, and lowers the value to at most `maximum - extent` (with a value of 50 and an extent of 10, setting the maximum to 40 makes the value 30)
 * The notification ([WChangeListener.stateChanged]) is sent once per change, only when one of the values actually changed.
 * As with Swing's EventListenerList, listeners are notified starting from the most recently added one.
 * Listeners added or removed during a notification do not affect it: it goes to the list as it was when it started.
 *
 * @throws IllegalArgumentException if the constructor arguments do not satisfy `minimum <= value <= value + extent <= maximum`
 */
open class WDefaultBoundedRangeModel @JvmOverloads constructor(
    value: Double = 0.0,
    extent: Double = 0.0,
    min: Double = 0.0,
    max: Double = DEFAULT_MAX,
) : WBoundedRangeModel {
    private var currentValue = value
    private var currentExtent = extent
    private var currentMin = min
    private var currentMax = max
    private var adjusting = false
    private val listeners = ArrayList<WChangeListener>()

    init {
        require(max >= min && value >= min && value + extent >= value && value + extent <= max) { "invalid range properties" }
    }

    override var value: Double
        get() = currentValue
        set(n) {
            val clamped = maxOf(minOf(n, currentMax - currentExtent), currentMin)
            setRangeProperties(clamped, currentExtent, currentMin, currentMax, adjusting)
        }

    override var extent: Double
        get() = currentExtent
        set(n) {
            var newExtent = maxOf(0.0, n)
            if (currentValue + newExtent > currentMax) newExtent = currentMax - currentValue
            setRangeProperties(currentValue, newExtent, currentMin, currentMax, adjusting)
        }

    override var minimum: Double
        get() = currentMin
        set(n) {
            val newMax = maxOf(n, currentMax)
            val newValue = maxOf(n, currentValue)
            val newExtent = minOf(newMax - newValue, currentExtent)
            setRangeProperties(newValue, newExtent, n, newMax, adjusting)
        }

    override var maximum: Double
        get() = currentMax
        set(n) {
            val newMin = minOf(n, currentMin)
            val newExtent = minOf(n - newMin, currentExtent)
            val newValue = minOf(n - newExtent, currentValue)
            setRangeProperties(newValue, newExtent, newMin, n, adjusting)
        }

    override var valueIsAdjusting: Boolean
        get() = adjusting
        set(b) {
            setRangeProperties(currentValue, currentExtent, currentMin, currentMax, b)
        }

    override fun setRangeProperties(newValue: Double, newExtent: Double, newMin: Double, newMax: Double, adjusting: Boolean) {
        var min = newMin
        var max = newMax
        var extent = newExtent
        if (min > max) min = max
        if (newValue > max) max = newValue
        if (newValue < min) min = newValue
        if (extent + newValue > max) extent = max - newValue
        if (extent < 0) extent = 0.0
        val changed = newValue != currentValue || extent != currentExtent || min != currentMin || max != currentMax ||
            adjusting != this.adjusting
        if (!changed) return
        currentValue = newValue
        currentExtent = extent
        currentMin = min
        currentMax = max
        this.adjusting = adjusting
        fireStateChanged()
    }

    override fun addChangeListener(listener: WChangeListener) {
        listeners += listener
    }

    override fun removeChangeListener(listener: WChangeListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index >= 0) listeners.removeAt(index)
    }

    /** The listeners currently added (in the order they were added; Swing's getChangeListeners). */
    fun getChangeListeners(): Array<WChangeListener> = listeners.toTypedArray()

    /** Passes one event whose source is this model to every listener (starting from the most recently added one). */
    protected fun fireStateChanged() {
        if (listeners.isEmpty()) return
        val event = WChangeEvent(this)
        for (listener in listeners.asReversed().toList()) listener.stateChanged(event)
    }

    override fun toString(): String =
        "${javaClass.name}[value=$currentValue, extent=$currentExtent, min=$currentMin, max=$currentMax, adj=$adjusting]"

    private companion object {
        const val DEFAULT_MAX = 100.0
    }
}
