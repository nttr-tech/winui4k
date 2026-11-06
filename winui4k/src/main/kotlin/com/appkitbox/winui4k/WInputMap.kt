package com.appkitbox.winui4k

/**
 * InputMap-like: a mapping from keystrokes ([WKeyStroke]) to keys (such as action names) used to look up actions in a [WActionMap].
 * Obtained with [WComponent.getInputMap] and used together with [WComponent.actionMap].
 * ```
 * button.getInputMap(WComponent.WHEN_IN_FOCUSED_WINDOW).put(WKeyStroke.getKeyStroke("ctrl S"), "save")
 * button.actionMap.put("save", saveAction)
 * ```
 * Keystrokes not in this map are looked up in [parent] (the same as Swing). Use every method from the UI thread.
 *
 * Differences from Swing:
 * - Putting itself or a map that leads back to itself through its parents into [parent] makes Swing recurse infinitely on lookup, but here it throws
 *   [IllegalArgumentException] when it is put in
 * - Swing's mapping for WHEN_IN_FOCUSED_WINDOW is a ComponentInputMap (an InputMap bound to a component), but here it is
 *   a WInputMap like the other conditions (it is looked up on the spot when a key is pressed, so changes do not need to be reported to the component)
 * - The order of [allKeys] is its own keystrokes in the order put, followed by the parent's (Swing does not define an order)
 */
open class WInputMap {
    /** Its own mappings (in the order put). null if [put] has never been called (as in Swing, [keys] returns null). */
    private var table: LinkedHashMap<WKeyStroke, Any>? = null

    /**
     * InputMap.getParent / setParent-like: the parent in which keystrokes not in this map are looked up. null if there is no parent.
     *
     * @throws IllegalArgumentException when putting itself or a map that leads back to itself through its parents
     */
    var parent: WInputMap? = null
        set(value) {
            var ancestor = value
            while (ancestor != null) {
                require(ancestor !== this) { "An InputMap that leads back to itself through its parents cannot be the parent" }
                ancestor = ancestor.parent
            }
            field = value
        }

    /**
     * InputMap.put-like: maps [keyStroke] to [actionMapKey]. If [actionMapKey] is null, the same as [remove].
     * Does nothing if [keyStroke] is null (the same as Swing).
     */
    open fun put(keyStroke: WKeyStroke?, actionMapKey: Any?) {
        if (keyStroke == null) return
        if (actionMapKey == null) {
            remove(keyStroke)
        } else {
            val entries = table ?: LinkedHashMap<WKeyStroke, Any>().also { table = it }
            entries[keyStroke] = actionMapKey
        }
    }

    /** InputMap.get-like: the key mapped to [keyStroke]. Looked up in [parent] if not in this map, and null if nowhere. */
    open operator fun get(keyStroke: WKeyStroke?): Any? = table?.get(keyStroke) ?: parent?.get(keyStroke)

    /** InputMap.remove-like: removes its own mapping for [key] (the one in [parent] is not removed). */
    open fun remove(key: WKeyStroke?) {
        table?.remove(key)
    }

    /** InputMap.clear-like: removes all its own mappings ([parent] is not changed). */
    open fun clear() {
        table?.clear()
    }

    /** InputMap.keys-like: its own keystrokes (not including those of [parent]). null if [put] has never been called (the same as Swing). */
    open fun keys(): Array<WKeyStroke>? = table?.keys?.toTypedArray()

    /** InputMap.size-like: the number of its own mappings (those of [parent] are not counted). */
    open fun size(): Int = table?.size ?: 0

    /**
     * InputMap.allKeys-like: the keystrokes of itself and [parent] (and its parents) (duplicates appear once). null if [put] has never been called
     * anywhere (the same as Swing).
     */
    open fun allKeys(): Array<WKeyStroke>? {
        val own = keys()
        val inherited = parent?.allKeys() ?: return own
        if (own == null) return inherited
        return (own.asList() + inherited).distinct().toTypedArray()
    }
}
