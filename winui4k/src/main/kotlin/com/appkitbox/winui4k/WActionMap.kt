package com.appkitbox.winui4k

/**
 * ActionMap-like: a mapping from keys (such as action names) to [WAction]s. A [WInputMap] maps keystrokes to keys,
 * and this map maps keys to actions ([WComponent.actionMap]). Keys not in this map are looked up in [parent] (the same as Swing).
 * Use every method from the UI thread.
 *
 * Differences from Swing: putting itself or a map that leads back to itself through its parents into [parent] makes Swing recurse infinitely on lookup, but here
 * it throws [IllegalArgumentException] when it is put in. The order of [allKeys] is its own keys in the order put, followed by the parent's (Swing does not define an order).
 */
open class WActionMap {
    /** Its own mappings (in the order put). null if [put] has never been called (as in Swing, [keys] returns null). */
    private var table: LinkedHashMap<Any, WAction>? = null

    /**
     * ActionMap.getParent / setParent-like: the parent in which keys not in this map are looked up. null if there is no parent.
     *
     * @throws IllegalArgumentException when putting itself or a map that leads back to itself through its parents
     */
    var parent: WActionMap? = null
        set(value) {
            var ancestor = value
            while (ancestor != null) {
                require(ancestor !== this) { "An ActionMap that leads back to itself through its parents cannot be the parent" }
                ancestor = ancestor.parent
            }
            field = value
        }

    /**
     * ActionMap.put-like: maps [key] to [action]. If [action] is null, the same as [remove]. Does nothing if [key] is null
     * (the same as Swing).
     */
    open fun put(key: Any?, action: WAction?) {
        if (key == null) return
        if (action == null) {
            remove(key)
        } else {
            val entries = table ?: LinkedHashMap<Any, WAction>().also { table = it }
            entries[key] = action
        }
    }

    /** ActionMap.get-like: the action for [key]. Looked up in [parent] if not in this map, and null if nowhere. */
    open operator fun get(key: Any?): WAction? = table?.get(key) ?: parent?.get(key)

    /** ActionMap.remove-like: removes its own mapping for [key] (the one in [parent] is not removed). */
    open fun remove(key: Any?) {
        table?.remove(key)
    }

    /** ActionMap.clear-like: removes all its own mappings ([parent] is not changed). */
    open fun clear() {
        table?.clear()
    }

    /** ActionMap.keys-like: its own keys (not including those of [parent]). null if [put] has never been called (the same as Swing). */
    open fun keys(): Array<Any>? = table?.keys?.toTypedArray()

    /** ActionMap.size-like: the number of its own mappings (those of [parent] are not counted). */
    open fun size(): Int = table?.size ?: 0

    /** ActionMap.allKeys-like: the keys of itself and [parent] (and its parents) (duplicates appear once). null if [put] has never been called anywhere. */
    open fun allKeys(): Array<Any>? {
        val own = keys()
        val inherited = parent?.allKeys() ?: return own
        if (own == null) return inherited
        return (own.asList() + inherited).distinct().toTypedArray()
    }
}
