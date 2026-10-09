package com.appkitbox.winui4k.chart

/**
 * Like javax.swing.AbstractListModel: a skeletal implementation of [SampleModel].
 *
 * Implementing just [getSize] and [getElementAt] gives a read-only model
 * (so arrays, database results, and the like can be shown to a chart without copying).
 * Provides listener management and change notification (the fire methods). Subclasses must call a fire method after
 * changing the contents.
 */
abstract class AbstractSampleModel<E> : SampleModel<E> {
    private val listeners = mutableListOf<SampleModelListener>()

    override fun addSampleModelListener(listener: SampleModelListener) {
        listeners += listener
    }

    override fun removeSampleModelListener(listener: SampleModelListener) {
        listeners -= listener
    }

    /** The registered listeners. */
    fun getSampleModelListeners(): List<SampleModelListener> = listeners.toList()

    /** Notifies that the values of the elements [index0]..[index1] have changed. */
    protected fun fireContentsChanged(index0: Int, index1: Int) {
        fireSamplesChanged(SampleModelEvent(this, SampleModelEvent.CONTENTS_CHANGED, index0, index1))
    }

    /** Notifies that elements have been added at [index0]..[index1] (positions after the addition). */
    protected fun fireIntervalAdded(index0: Int, index1: Int) {
        fireSamplesChanged(SampleModelEvent(this, SampleModelEvent.INTERVAL_ADDED, index0, index1))
    }

    /** Notifies that the elements at [index0]..[index1] have been removed (positions before the removal). */
    protected fun fireIntervalRemoved(index0: Int, index1: Int) {
        fireSamplesChanged(SampleModelEvent(this, SampleModelEvent.INTERVAL_REMOVED, index0, index1))
    }

    /** Notifies all registered listeners of [event] (calling the most recently registered first, as in Swing). */
    protected fun fireSamplesChanged(event: SampleModelEvent) {
        for (listener in listeners.asReversed().toList()) listener.samplesChanged(event)
    }
}
