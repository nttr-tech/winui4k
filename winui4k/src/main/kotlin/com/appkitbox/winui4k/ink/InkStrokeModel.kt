package com.appkitbox.winui4k.ink

/**
 * Like javax.swing.ListModel / javax.swing.text.Document: the model of the sequence of strokes displayed by
 * [com.appkitbox.winui4k.WInkCanvas]. Plays the data role of WinUI's InkStrokeContainer.
 *
 * The view (WInkCanvas) draws the model's strokes, and writes back lines drawn with the pen, lines erased with the
 * eraser, and the results of moving or deleting a selection, pasting, and loading through this model's [addStroke] /
 * [removeStroke] / [setStroke], etc. When the contents of the model change, notify [InkStrokeModelListener]s with an
 * [InkStrokeModelEvent] (usually via the fire methods of [AbstractInkStrokeModel]).
 * When one model is shared by multiple canvases, lines drawn on any canvas are shown on all of them.
 *
 * Indexes start at 0, and later strokes are drawn in front.
 */
interface InkStrokeModel {
    /** The number of strokes. */
    fun getStrokeCount(): Int

    /** The [index]-th stroke. */
    fun getStroke(index: Int): InkStroke

    /** A snapshot of all strokes (changing it later does not affect the model). */
    fun getStrokes(): List<InkStroke> = List(getStrokeCount()) { getStroke(it) }

    /** Appends a stroke to the end. */
    fun addStroke(stroke: InkStroke)

    /** Inserts a stroke at [index]. */
    fun insertStroke(index: Int, stroke: InkStroke)

    /** Replaces the [index]-th stroke. */
    fun setStroke(index: Int, stroke: InkStroke)

    /** Removes the [index]-th stroke. */
    fun removeStroke(index: Int)

    /** Removes all strokes. */
    fun clear()

    /** Subscribes to the model's change notifications. */
    fun addInkStrokeModelListener(listener: InkStrokeModelListener)

    /** Removes a listener registered with [addInkStrokeModelListener]. */
    fun removeInkStrokeModelListener(listener: InkStrokeModelListener)
}

/** Like javax.swing.event.TableModelListener: the receiver of change notifications from an [InkStrokeModel]. */
fun interface InkStrokeModelListener {
    fun strokesChanged(event: InkStrokeModelEvent)
}
