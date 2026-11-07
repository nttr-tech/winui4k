package com.appkitbox.winui4k

/**
 * The list of [WFrame.addComponentListener] listeners and the code that calls them (called from size and position changes in AppWindow.Changed).
 * The event source is [frame], and [WComponentEvent.component] is its current content ([WFrame.contentPane]). Used only by the UI thread.
 */
internal class FrameComponentEvents(private val frame: WFrame) {
    private val listeners = ArrayList<WComponentListener>()

    fun add(listener: WComponentListener?) {
        if (listener != null) listeners.add(listener)
    }

    fun remove(listener: WComponentListener?) {
        listeners.remove(listener)
    }

    fun listeners(): Array<WComponentListener> = listeners.toTypedArray()

    /** Calls componentResized if the size changed and componentMoved if the position changed, on the listeners in the order added. */
    fun fire(sizeChanged: Boolean, positionChanged: Boolean) {
        if (listeners.isEmpty()) return
        if (sizeChanged) fire(WComponentEvent.COMPONENT_RESIZED)
        if (positionChanged) fire(WComponentEvent.COMPONENT_MOVED)
    }

    private fun fire(id: Int) {
        val event = WComponentEvent(frame, id, frame.content)
        for (listener in listeners.toList()) {
            if (id == WComponentEvent.COMPONENT_RESIZED) listener.componentResized(event) else listener.componentMoved(event)
        }
    }
}
