package com.appkitbox.winui4k

/**
 * SpinnerModel-style: a model of the sequence of values a spinner ([WSpinner]) steps through.
 *
 * Returns the current value ([value]) and the next ([nextValue]) and previous ([previousValue]) values, and notifies [WChangeListener]s when the value and so on change.
 * The implementations are the numeric [WSpinnerNumberModel], the list [WSpinnerListModel] and the date-time [WSpinnerDateModel].
 * When implementing your own, extending [WAbstractSpinnerModel], which has the listener list and notifications, makes it easier.
 */
interface WSpinnerModel {
    /** The current value. Setting a value that is not accepted throws [IllegalArgumentException] (which values are accepted depends on the implementation). */
    var value: Any?

    /** The value after [value]. Null when it goes past the end of the sequence (the upper limit of the range). Does not change [value]. */
    val nextValue: Any?

    /** The value before [value]. Null when it goes past the start of the sequence (the lower limit of the range). Does not change [value]. */
    val previousValue: Any?

    /** Adds a change listener (adding the same listener twice makes it called twice). */
    fun addChangeListener(listener: WChangeListener)

    /** Removes one listener added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener)
}

/**
 * AbstractSpinnerModel-style: a base class with the [WSpinnerModel] listener list and notification ([fireStateChanged]).
 * Subclasses call [fireStateChanged] when the value and so on change.
 *
 * As with Swing's EventListenerList, listeners receive the same event, whose source is this model, starting from the most recently added one.
 * Listeners added or removed during a notification do not affect it: it goes to the list as it was when it started.
 */
abstract class WAbstractSpinnerModel protected constructor() : WSpinnerModel {
    private val listeners = ArrayList<WChangeListener>()

    override fun addChangeListener(listener: WChangeListener) {
        listeners += listener
    }

    override fun removeChangeListener(listener: WChangeListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index >= 0) listeners.removeAt(index)
    }

    /** The listeners currently added (in the order they were added; Swing's getChangeListeners). */
    fun getChangeListeners(): Array<WChangeListener> = listeners.toTypedArray()

    /** Passes one event whose source is this model to every listener (starting from the most recently added one). */
    protected fun fireStateChanged() {
        if (listeners.isEmpty()) return
        val event = WChangeEvent(this)
        for (listener in listeners.asReversed().toList()) listener.stateChanged(event)
    }
}
