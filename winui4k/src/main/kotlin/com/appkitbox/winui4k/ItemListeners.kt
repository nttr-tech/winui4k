package com.appkitbox.winui4k

/**
 * The list of ItemListeners ([WItemListener]) of a toggle component. As with Swing's EventListenerList, it creates one
 * [WItemEvent] per native notification and passes the same event to all listeners in the order they were added ([fire]).
 * Even if an earlier listener changes the state, later listeners receive the state at the time of that notification
 * (nested notifications are delivered first).
 *
 * The native handler ([addNativeHandler]) is registered only once, when the first listener is added, and stays
 * registered even after all listeners are removed (it does nothing without listeners, as in ComponentInput). The handler
 * must not capture the component strongly but look it up from [WComponent.listenerAnchor]. Kotlin lambdas and legacy
 * Consumers go into the same list as wrapping [WItemListener]s, with the mapping remembered so they can be removed by the
 * original listener. Use only from the UI thread.
 */
internal class ItemListeners(private val owner: WComponent, private val addNativeHandler: () -> Unit) {
    private val listeners = ArrayList<WItemListener>()
    private val adapters = KotlinListenerAdapters<WItemListener>()
    private var handlerAdded = false

    /** Registers the native handler if not yet registered (also used by internal subscriptions that receive notifications outside the listener list). */
    fun ensureNativeHandler() {
        if (handlerAdded) return
        addNativeHandler()
        handlerAdded = true
    }

    fun add(listener: WItemListener) {
        ensureNativeHandler()
        listeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    /** Adds [adapter], which wraps [key] (a Kotlin lambda or Consumer). To remove it, pass [key] to [removeAdapter]. */
    fun addAdapter(key: Any, adapter: WItemListener) {
        add(adapter)
        adapters.add(key, adapter)
    }

    /** Removes one [listener], the last one added. Does nothing if it was not added. */
    fun remove(listener: WItemListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    /** Removes one listener added for [key] with [addAdapter]. */
    fun removeAdapter(key: Any) {
        remove(adapters.remove(key) ?: return)
    }

    /** Passes [event] to all listeners in the order added (even if listeners are added or removed during the calls, it is delivered to the list as of the start). */
    fun fire(event: WItemEvent) {
        if (listeners.isEmpty()) return
        for (listener in listeners.toList()) listener.itemStateChanged(event)
    }
}
