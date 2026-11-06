package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * ListDataEvent-style: an event that tells a [WListDataListener] that the contents of a [WListModel] changed.
 * [type] is [CONTENTS_CHANGED] (elements replaced) / [INTERVAL_ADDED] (elements added) / [INTERVAL_REMOVED] (elements
 * removed), and [index0]..[index1] is the changed range (inclusive). As in Swing, the indexes are normalized to
 * `index0 <= index1` even if they are passed to the constructor in reverse order. For additions and removals, the
 * model has already changed when the listener is called (same as Swing).
 *
 * This class does not depend on java.awt or javax.swing, so it defines its own constants with the same values as
 * Swing's ListDataEvent.
 */
class WListDataEvent(
    source: Any,
    /** How it changed ([CONTENTS_CHANGED] / [INTERVAL_ADDED] / [INTERVAL_REMOVED]). */
    val type: Int,
    index0: Int,
    index1: Int,
) : EventObject(source) {
    /** The start of the changed range (inclusive). */
    val index0: Int = minOf(index0, index1)

    /** The end of the changed range (inclusive). */
    val index1: Int = maxOf(index0, index1)

    override fun toString(): String = "${javaClass.name}[type=$type,index0=$index0,index1=$index1]"

    companion object {
        /** The elements in the range were replaced (same value as Swing's ListDataEvent.CONTENTS_CHANGED). */
        const val CONTENTS_CHANGED: Int = 0

        /** Elements were added in the range (same value as Swing's ListDataEvent.INTERVAL_ADDED). */
        const val INTERVAL_ADDED: Int = 1

        /** The elements in the range were removed (same value as Swing's ListDataEvent.INTERVAL_REMOVED). */
        const val INTERVAL_REMOVED: Int = 2
    }
}

/**
 * ListDataListener-style: the listener passed to [WListModel.addListDataListener].
 * As in Swing, it has three methods and no Adapter.
 */
interface WListDataListener : EventListener {
    /** Called when elements are added in a range ([WListDataEvent.type] is [WListDataEvent.INTERVAL_ADDED]). */
    fun intervalAdded(e: WListDataEvent)

    /** Called when the elements in a range are removed ([WListDataEvent.type] is [WListDataEvent.INTERVAL_REMOVED]). */
    fun intervalRemoved(e: WListDataEvent)

    /** Called when the elements in a range are replaced (the whole list if the range is unclear) ([WListDataEvent.type] is [WListDataEvent.CONTENTS_CHANGED]). */
    fun contentsChanged(e: WListDataEvent)
}
