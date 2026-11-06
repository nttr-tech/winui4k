package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * ChangeEvent-style: an event that tells a [WChangeListener] that the state of a range model ([WBoundedRangeModel]),
 * a slider or the like has changed. As with Swing's ChangeEvent, it only carries [getSource] (`source` in Kotlin).
 * The event does not contain what changed, so listeners read the state from the source.
 */
class WChangeEvent(source: Any) : EventObject(source)

/**
 * ChangeListener-style: a listener called when the state changes (can be written as a lambda in Java).
 * It has only one method, so unlike Swing's other listeners there is no Adapter.
 */
fun interface WChangeListener : EventListener {
    /** Called when the state changes. */
    fun stateChanged(e: WChangeEvent)
}
