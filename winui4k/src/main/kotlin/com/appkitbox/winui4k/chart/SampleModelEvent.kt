package com.appkitbox.winui4k.chart

import java.util.EventObject

/**
 * Like javax.swing.event.ListDataEvent: the details of a change to a [SampleModel].
 *
 * - addition ([INTERVAL_ADDED]) / removal ([INTERVAL_REMOVED]) / value change ([CONTENTS_CHANGED]) of elements, and
 *   its range ([index0]..[index1], both inclusive)
 * - the ranges of [INTERVAL_ADDED] / [INTERVAL_REMOVED] are positions in the model after the addition / before the
 *   removal, respectively
 */
class SampleModelEvent(
    source: SampleModel<*>,
    /** The kind of change ([CONTENTS_CHANGED] / [INTERVAL_ADDED] / [INTERVAL_REMOVED]). */
    val type: Int,
    /** The position of the start of the changed range. */
    val index0: Int,
    /** The position of the end of the changed range (both inclusive). */
    val index1: Int,
) : EventObject(source) {
    /** The model that changed. */
    override fun getSource(): SampleModel<*> = super.getSource() as SampleModel<*>

    override fun toString(): String = "SampleModelEvent(type=$type, index0=$index0, index1=$index1)"

    companion object {
        /** A change in element values. */
        const val CONTENTS_CHANGED = 0

        /** Elements were added. */
        const val INTERVAL_ADDED = 1

        /** Elements were removed. */
        const val INTERVAL_REMOVED = 2
    }
}

/**
 * Like javax.swing.event.ListDataListener: receives change notifications from a [SampleModel].
 */
fun interface SampleModelListener {
    /** Called when the model changes. */
    fun samplesChanged(event: SampleModelEvent)
}
