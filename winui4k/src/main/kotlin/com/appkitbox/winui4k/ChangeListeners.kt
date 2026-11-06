package com.appkitbox.winui4k

/**
 * The list of a component's ChangeListeners ([WChangeListener]). As with Swing's EventListenerList, each change
 * creates one [WChangeEvent] (whose source is [owner]) and passes the same event to every listener in the order
 * they were added ([fire]). Listeners added or removed while firing do not affect that notification: it goes to
 * the list as it was when firing started (a snapshot).
 *
 * The native handler ([addNativeHandler]) is registered only once, when the first listener is added, and stays
 * registered even after every listener is removed (same as [ItemListeners]). The handler must not strongly capture
 * the component; look it up through [WComponent.listenerAnchor] instead. Kotlin lambdas and legacy Consumers are
 * wrapped in a [WChangeListener] and put in the same list, and the mapping is remembered so they can be removed by
 * the original listener ([addAdapter]). Use only from the UI thread.
 *
 * For controls whose native side sends notifications even when the value has not changed (RatingControl and so
 * on), [fireIfChanged] compares with the previous value and drops notifications of the same value.
 *
 * Components that receive no notifications from the native side (progress bars whose value changes only through the model, and so on) set [anchored] to false
 * so that adding listeners does not keep the component alive through [WComponent.listenerAnchor] (there is no reason to keep it, and components not yet placed would stay alive).
 */
internal class ChangeListeners(
    private val owner: WComponent,
    private val anchored: Boolean = true,
    private val addNativeHandler: () -> Unit,
) {
    private val listeners = ArrayList<WChangeListener>()
    private val adapters = KotlinListenerAdapters<WChangeListener>()
    private var handlerAdded = false

    /** Registers the native handler if it is not registered yet (also used by internal subscriptions that receive notifications outside the listener list). */
    fun ensureNativeHandler() {
        if (handlerAdded) return
        addNativeHandler()
        handlerAdded = true
    }

    fun add(listener: WChangeListener) {
        ensureNativeHandler()
        listeners += listener
        if (anchored) owner.listenerAnchor.listenerAdded()
    }

    /** Adds [adapter], which wraps [key] (a Kotlin lambda or Consumer). To remove it, pass [key] to [removeAdapter]. */
    fun addAdapter(key: Any, adapter: WChangeListener) {
        add(adapter)
        adapters.add(key, adapter)
    }

    /** Removes the most recently added [listener]. Does nothing if it was not added. */
    fun remove(listener: WChangeListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        if (anchored) owner.listenerAnchor.listenerRemoved()
    }

    /** Removes one listener added for [key] with [addAdapter]. */
    fun removeAdapter(key: Any) {
        remove(adapters.remove(key) ?: return)
    }

    /** The listeners currently added (in the order they were added; for getChangeListeners). */
    fun toArray(): Array<WChangeListener> = listeners.toTypedArray()

    /** Creates one event whose source is [owner] and passes it to every listener in the order they were added. */
    fun fire() {
        if (listeners.isEmpty()) return
        fire(WChangeEvent(owner))
    }

    /** Passes [event] to every listener in the order they were added (listeners added or removed while firing do not affect the list it goes to). */
    fun fire(event: WChangeEvent) {
        if (listeners.isEmpty()) return
        for (listener in listeners.toList()) listener.stateChanged(event)
    }

    /** Calls [fire] only when [old] and [new] differ. Returns whether it notified. */
    fun <T> fireIfChanged(old: T, new: T): Boolean {
        if (old == new) return false
        fire()
        return true
    }
}
