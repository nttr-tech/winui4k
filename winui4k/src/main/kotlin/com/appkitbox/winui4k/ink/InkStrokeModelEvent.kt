package com.appkitbox.winui4k.ink

import java.util.EventObject

/**
 * Like javax.swing.event.TableModelEvent / ListDataEvent: the details of a change to an [InkStrokeModel].
 *
 * - addition ([INSERT]) / deletion ([DELETE]) / replacement ([UPDATE]) of strokes, and its range ([firstIndex]..[lastIndex])
 * - an [UPDATE] whose [lastIndex] is [Int.MAX_VALUE] is a change to everything (the number of strokes may also change)
 *
 * Indexes of [DELETE] are positions before removal, and those of [INSERT] are positions after addition.
 */
class InkStrokeModelEvent(
    source: InkStrokeModel,
    /** The index of the start of the changed range. */
    val firstIndex: Int,
    /** The index of the end of the changed range (both inclusive). */
    val lastIndex: Int,
    /** The kind of change ([INSERT] / [UPDATE] / [DELETE]). */
    val type: Int,
) : EventObject(source) {
    /** A change to everything (the number of strokes may also change). */
    constructor(source: InkStrokeModel) : this(source, 0, Int.MAX_VALUE, UPDATE)

    /** The model that changed. */
    override fun getSource(): InkStrokeModel = super.getSource() as InkStrokeModel

    /** Whether this is a change to everything. */
    val isDataChanged: Boolean
        get() = type == UPDATE && lastIndex == Int.MAX_VALUE

    override fun toString(): String = "InkStrokeModelEvent(type=$type, firstIndex=$firstIndex, lastIndex=$lastIndex)"

    companion object {
        /** Addition of strokes. */
        const val INSERT = 1

        /** Replacement of strokes. */
        const val UPDATE = 0

        /** Deletion of strokes. */
        const val DELETE = -1
    }
}
