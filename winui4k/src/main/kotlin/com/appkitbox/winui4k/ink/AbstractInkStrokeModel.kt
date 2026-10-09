package com.appkitbox.winui4k.ink

/**
 * Like javax.swing.AbstractListModel: a skeletal implementation of [InkStrokeModel].
 *
 * Provides listener management and change notification (the fire methods). Subclasses implement how the data is held
 * and how it is changed, and must call the corresponding fire method after a change.
 */
abstract class AbstractInkStrokeModel : InkStrokeModel {
    private val listeners = mutableListOf<InkStrokeModelListener>()

    override fun addInkStrokeModelListener(listener: InkStrokeModelListener) {
        listeners += listener
    }

    override fun removeInkStrokeModelListener(listener: InkStrokeModelListener) {
        listeners -= listener
    }

    /** The registered listeners. */
    fun getInkStrokeModelListeners(): List<InkStrokeModelListener> = listeners.toList()

    /** Notifies that strokes have been added at [firstIndex]..[lastIndex]. */
    protected fun fireStrokesInserted(firstIndex: Int, lastIndex: Int) {
        fireStrokesChanged(InkStrokeModelEvent(this, firstIndex, lastIndex, InkStrokeModelEvent.INSERT))
    }

    /** Notifies that the strokes at [firstIndex]..[lastIndex] have been replaced. */
    protected fun fireStrokesUpdated(firstIndex: Int, lastIndex: Int) {
        fireStrokesChanged(InkStrokeModelEvent(this, firstIndex, lastIndex, InkStrokeModelEvent.UPDATE))
    }

    /** Notifies that the strokes at [firstIndex]..[lastIndex] (positions before removal) have been deleted. */
    protected fun fireStrokesDeleted(firstIndex: Int, lastIndex: Int) {
        fireStrokesChanged(InkStrokeModelEvent(this, firstIndex, lastIndex, InkStrokeModelEvent.DELETE))
    }

    /** Notifies that everything has changed (the number of strokes may also change). */
    protected fun fireDataChanged() {
        fireStrokesChanged(InkStrokeModelEvent(this))
    }

    /** Notifies all registered listeners of [event] (calling the most recently registered first, as in Swing). */
    protected fun fireStrokesChanged(event: InkStrokeModelEvent) {
        for (listener in listeners.asReversed().toList()) listener.strokesChanged(event)
    }
}
