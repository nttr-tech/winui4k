package com.appkitbox.winui4k

/**
 * JViewport-like: the viewport of a [WScrollPane] (obtained with [WScrollPane.viewport]). Reads and writes which part of the content ([view]) is shown.
 * Unlike Swing's JViewport it is not a component but a point for reading and writing the ScrollViewer (it cannot be put in a window or added to another panel).
 * Positions and sizes are in DIP and returned with the same types as [WComponent.location] / [WComponent.size] / [WComponent.bounds].
 */
class WViewport internal constructor(private val scrollPane: WScrollPane) {
    /** The content to show (JViewport.getView / setView; the same as [WScrollPane.content]). */
    var view: WComponent?
        get() = scrollPane.content
        set(value) {
            scrollPane.content = value
        }

    /**
     * The content position visible at the top-left of the viewport (JViewport.getViewPosition / setViewPosition; DIP rounded to integers).
     * Setting is done without animation (disableAnimation = true of ScrollViewer.ChangeView) followed by finishing the layout, so
     * this value returns the new position as soon as the setter returns (as in Swing). ScrollViewer keeps the position
     * within 0 to the scrollable amount ([WScrollPane.scrollableWidth] / [WScrollPane.scrollableHeight]), so
     * setting a value outside the range stops at the edge (Swing's JViewport accepts values outside the range too).
     */
    var viewPosition: WPoint
        get() = WPoint(Math.round(scrollPane.requestedHorizontalOffset).toInt(), Math.round(scrollPane.requestedVerticalOffset).toInt())
        set(value) {
            scrollPane.changeViewImmediately(value.x.toDouble(), value.y.toDouble())
        }

    /**
     * The viewport's size (JViewport.getExtentSize; ScrollViewer.ViewportWidth / ViewportHeight). Pending layout is finished before reading
     * (the same as [viewSize], but 0 right after showing and before layout).
     */
    val extentSize: WSize
        get() {
            scrollPane.layOutForSizes()
            return WSize(scrollPane.viewportWidth, scrollPane.viewportHeight)
        }

    /**
     * The content's size (JViewport.getViewSize; ScrollViewer.ExtentWidth / ExtentHeight). As in Swing, pending layout is finished before reading
     * (the size after adding even right after adding to the content; right after showing and before layout, the measured size of the content).
     */
    val viewSize: WSize
        get() {
            val extent = scrollPane.extentForReading()
            return WSize(extent[0], extent[1])
        }

    /** The visible range in content coordinates (JViewport.getViewRect = [viewPosition] and [extentSize]). */
    val viewRect: WRectangle
        get() {
            val position = viewPosition
            val extent = extentSize
            return WRectangle(position.x, position.y, Math.round(extent.width).toInt(), Math.round(extent.height).toInt())
        }

    /**
     * ChangeListener-like: receives a [WChangeEvent] (the source is this viewport) every time the shown position changes
     * (ScrollViewer.ViewChanged; also called during inertial scrolling and dragging). Inside the listener, [viewPosition] returns the value after the change.
     * As with Swing's JViewport, it is called inside setting [viewPosition] (and [WScrollPaneBar.value]) (also before the first layout right after showing,
     * with the requested position). A later ViewChanged does not call it again if the position and whether it is intermediate are the same as the last notification. It is not called when only the sizes of the content or the viewport
     * change (Swing calls it).
     */
    fun addChangeListener(listener: WChangeListener) {
        scrollPane.viewportChangeListeners.add(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        scrollPane.viewportChangeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order added). */
    fun getChangeListeners(): Array<WChangeListener> = scrollPane.viewportChangeListeners.toArray()
}
