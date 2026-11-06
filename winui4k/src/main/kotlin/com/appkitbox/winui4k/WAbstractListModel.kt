package com.appkitbox.winui4k

/**
 * AbstractListModel-style: a skeletal implementation of [WListModel]. Provides listener management and change
 * notification (the fire methods).
 *
 * Implementing [getSize] and [getElementAt] gives a read-only model. Implementations that change the contents call
 * [fireIntervalAdded] / [fireIntervalRemoved] / [fireContentsChanged] after the change to notify the listeners.
 * As with Swing's EventListenerList, listeners are notified starting from the most recently added one, and listeners
 * added or removed during a notification do not affect it: it goes to the list as it was when it started. The fire
 * methods can also be called from Java subclasses (protected).
 */
abstract class WAbstractListModel<E> : WListModel<E> {
    private val listeners = ArrayList<WListDataListener>()

    override fun addListDataListener(listener: WListDataListener) {
        listeners += listener
    }

    override fun removeListDataListener(listener: WListDataListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index >= 0) listeners.removeAt(index)
    }

    /** The listeners currently added (in the order they were added; Swing's getListDataListeners). */
    fun getListDataListeners(): Array<WListDataListener> = listeners.toTypedArray()

    /** Notifies that the elements in [index0]..[index1] were replaced (the listeners' `contentsChanged`). */
    protected open fun fireContentsChanged(source: Any, index0: Int, index1: Int) {
        fire(WListDataEvent(source, WListDataEvent.CONTENTS_CHANGED, index0, index1)) { l, e -> l.contentsChanged(e) }
    }

    /** Notifies that elements were added in [index0]..[index1] (the listeners' `intervalAdded`). */
    protected open fun fireIntervalAdded(source: Any, index0: Int, index1: Int) {
        fire(WListDataEvent(source, WListDataEvent.INTERVAL_ADDED, index0, index1)) { l, e -> l.intervalAdded(e) }
    }

    /** Notifies that the elements in [index0]..[index1] were removed (the listeners' `intervalRemoved`). */
    protected open fun fireIntervalRemoved(source: Any, index0: Int, index1: Int) {
        fire(WListDataEvent(source, WListDataEvent.INTERVAL_REMOVED, index0, index1)) { l, e -> l.intervalRemoved(e) }
    }

    private inline fun fire(event: WListDataEvent, call: (WListDataListener, WListDataEvent) -> Unit) {
        if (listeners.isEmpty()) return
        for (listener in listeners.asReversed().toList()) call(listener, event)
    }
}
