package com.appkitbox.winui4k.ink

/**
 * Like javax.swing.DefaultListModel: the standard implementation of [InkStrokeModel] that holds strokes in a list.
 *
 * The default model of [com.appkitbox.winui4k.WInkCanvas]. The bulk add/remove methods [addStrokes] / [removeStrokes] /
 * [setStrokes] combine their changes into a single notification.
 */
class DefaultInkStrokeModel() : AbstractInkStrokeModel() {
    private val strokes = ArrayList<InkStroke>()

    /** A model initialized with [strokes]. */
    constructor(strokes: List<InkStroke>) : this() {
        this.strokes.addAll(strokes)
    }

    override fun getStrokeCount(): Int = strokes.size

    override fun getStroke(index: Int): InkStroke = strokes[index]

    override fun getStrokes(): List<InkStroke> = strokes.toList()

    override fun addStroke(stroke: InkStroke) {
        strokes.add(stroke)
        fireStrokesInserted(strokes.size - 1, strokes.size - 1)
    }

    /** Appends all of [strokes] to the end (one notification). Does nothing if it is empty. */
    fun addStrokes(strokes: List<InkStroke>) {
        if (strokes.isEmpty()) return
        val first = this.strokes.size
        this.strokes.addAll(strokes)
        fireStrokesInserted(first, this.strokes.size - 1)
    }

    override fun insertStroke(index: Int, stroke: InkStroke) {
        if (index !in 0..strokes.size) throw IndexOutOfBoundsException("index=$index, size=${strokes.size}")
        strokes.add(index, stroke)
        fireStrokesInserted(index, index)
    }

    override fun setStroke(index: Int, stroke: InkStroke) {
        strokes[index] = stroke
        fireStrokesUpdated(index, index)
    }

    override fun removeStroke(index: Int) {
        strokes.removeAt(index)
        fireStrokesDeleted(index, index)
    }

    /** Removes the strokes [fromIndex]..[toIndex] (both inclusive) at once (one notification). */
    fun removeStrokes(fromIndex: Int, toIndex: Int) {
        if (fromIndex < 0 || toIndex >= strokes.size || fromIndex > toIndex) {
            throw IndexOutOfBoundsException("fromIndex=$fromIndex, toIndex=$toIndex, size=${strokes.size}")
        }
        strokes.subList(fromIndex, toIndex + 1).clear()
        fireStrokesDeleted(fromIndex, toIndex)
    }

    /** Replaces everything with [strokes] (one notification of a change to everything). */
    fun setStrokes(strokes: List<InkStroke>) {
        this.strokes.clear()
        this.strokes.addAll(strokes)
        fireDataChanged()
    }

    override fun clear() {
        if (strokes.isEmpty()) return
        val last = strokes.size - 1
        strokes.clear()
        fireStrokesDeleted(0, last)
    }
}
