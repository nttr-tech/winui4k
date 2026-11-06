package com.appkitbox.winui4k

import java.util.EventObject

/**
 * ListSelectionEvent-style: an event that tells a [WListSelectionListener] the range in which the selection changed.
 *
 * [firstIndex]..[lastIndex] (inclusive) is the smallest range in which the selection state may have changed. The range may include rows that did not change.
 * While [valueIsAdjusting] is true, the user is in the middle of an operation (dragging, for example) and the final selection is not settled yet.
 * In events fired by a [WListSelectionModel], [firstIndex] <= [lastIndex] always holds.
 */
class WListSelectionEvent(
    source: Any,
    /** The first row of the changed range. */
    val firstIndex: Int,
    /** The last row of the changed range (inclusive). */
    val lastIndex: Int,
    /** True if the event was fired in the middle of an operation (`getValueIsAdjusting()` in Java). */
    val valueIsAdjusting: Boolean,
) : EventObject(source) {
    override fun toString(): String =
        "${javaClass.name}[source=$source, firstIndex=$firstIndex, lastIndex=$lastIndex, isAdjusting=$valueIsAdjusting]"
}

/**
 * ListSelectionListener-style: receives selection changes of a [WListSelectionModel].
 * Can be written as a lambda (`e -> ...`) in Java.
 */
fun interface WListSelectionListener {
    /** Called when the selection changes ([WListSelectionEvent.firstIndex]..[WListSelectionEvent.lastIndex] is the changed range). */
    fun valueChanged(e: WListSelectionEvent)
}
