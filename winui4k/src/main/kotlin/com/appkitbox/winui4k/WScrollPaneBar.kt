package com.appkitbox.winui4k

/**
 * JScrollBar-like (obtained with [WScrollPane.verticalScrollBar] / [WScrollPane.horizontalScrollBar]): the point for reading and writing the scroll position
 * of one direction of a [WScrollPane] as a scroll bar value. Unlike Swing's JScrollBar it is not a component (the bar itself inside the ScrollViewer template
 * is not exposed). Values are in DIP rounded to integers.
 *
 * - [minimum] is 0, [maximum] is the content length (ExtentWidth / ExtentHeight), and [visibleAmount] is the viewport length (ViewportWidth / ViewportHeight).
 * - [value] is the current scroll position. Setting it is done without animation, the same as [WViewport.viewPosition], and it can be read back when the call returns
 *   (the listeners are also called before it returns).
 * - [valueIsAdjusting] is IsIntermediate of the latest ScrollViewer.ViewChanged (true in the middle of dragging or inertial scrolling).
 */
class WScrollPaneBar internal constructor(
    private val scrollPane: WScrollPane,
    override val orientation: Int,
) : WAdjustable {
    private val listeners = ArrayList<WAdjustmentListener>()

    /** The last reported value and whether it was intermediate (ViewChanged with the same combination is not reported). */
    private var notifiedValue = 0
    private var notifiedAdjusting = false

    private val horizontal: Boolean get() = orientation == WAdjustable.HORIZONTAL

    override val minimum: Int get() = 0

    /**
     * The length of the content. As in Swing, pending layout is finished before reading (the length after adding even right after adding to the content; right after showing
     * and before layout, the measured length of the content).
     */
    override val maximum: Int
        get() {
            val extent = scrollPane.extentForReading()
            return Math.round(if (horizontal) extent[0] else extent[1]).toInt()
        }

    override var value: Int
        get() = Math.round(if (horizontal) scrollPane.requestedHorizontalOffset else scrollPane.requestedVerticalOffset).toInt()
        set(value) {
            if (horizontal) {
                scrollPane.changeViewImmediately(value.toDouble(), null)
            } else {
                scrollPane.changeViewImmediately(null, value.toDouble())
            }
        }

    /** The length of the viewport. Pending layout is finished before reading (the same as [maximum], but 0 right after showing and before layout). */
    override val visibleAmount: Int
        get() {
            scrollPane.layOutForSizes()
            return Math.round(if (horizontal) scrollPane.viewportWidth else scrollPane.viewportHeight).toInt()
        }

    override val valueIsAdjusting: Boolean
        get() = scrollPane.viewIsAdjusting

    /**
     * AdjustmentListener-like: receives a [WAdjustmentEvent] (the source is this bar, the type is [WAdjustmentEvent.TRACK]) every time the scroll position changes
     * (ScrollViewer.ViewChanged). Called only when the value or [valueIsAdjusting] differs from the previous notification, so it is not called for scrolling only in the other direction.
     * As with Swing's setValue, it is called inside setting [value] (and [WViewport.viewPosition]) (also before the first layout right after showing,
     * with the requested value). A later ViewChanged does not call it again if the value is the same. It is not called when only the content's size changes
     * (Swing calls it for range changes too).
     */
    override fun addAdjustmentListener(listener: WAdjustmentListener) {
        if (listeners.isEmpty()) {
            notifiedValue = value
            notifiedAdjusting = valueIsAdjusting
        }
        scrollPane.ensureViewChangedHandler()
        listeners += listener
        scrollPane.listenerAnchor.listenerAdded()
    }

    override fun removeAdjustmentListener(listener: WAdjustmentListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        scrollPane.listenerAnchor.listenerRemoved()
    }

    override fun getAdjustmentListeners(): Array<WAdjustmentListener> = listeners.toTypedArray()

    /** Makes the next [onViewChanged] notify even if the value is the same as the last notification (when layout decides the sizes of the content and the viewport). */
    internal fun forgetNotified() {
        notifiedValue = Int.MIN_VALUE
    }

    /** Called from the [WScrollPane] that received ViewChanged. If the value or the intermediate state changed, notifies the listeners in the order added. */
    internal fun onViewChanged(adjusting: Boolean) {
        if (listeners.isEmpty()) return
        val now = value
        if (now == notifiedValue && adjusting == notifiedAdjusting) return
        notifiedValue = now
        notifiedAdjusting = adjusting
        val event = WAdjustmentEvent(this, WAdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, WAdjustmentEvent.TRACK, now, adjusting)
        for (listener in listeners.toList()) listener.adjustmentValueChanged(event)
    }
}
