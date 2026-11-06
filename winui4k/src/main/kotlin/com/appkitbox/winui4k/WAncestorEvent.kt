package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * AncestorEvent-like: an event telling a [WAncestorListener] that a component appeared on screen, disappeared from it, or moved within the window.
 * [getSource] (`source` in Kotlin) and [component] are the component the listener was added to, and [id] is [ANCESTOR_ADDED] /
 * [ANCESTOR_REMOVED] / [ANCESTOR_MOVED].
 *
 * Swing's [ancestor] is the ancestor that changed (added, hidden or moved), but WinUI does not tell which ancestor changed,
 * so [ancestor] is always the component the listener was added to, and [ancestorParent] is its [WComponent.parent].
 * Swing's AncestorEvent derives from AWTEvent, but to avoid depending on java.awt this derives from EventObject and holds the same constant values itself.
 */
class WAncestorEvent(
    source: WComponent,
    /** The event type. `getID()` in Java, as in Swing. */
    @get:JvmName("getID") val id: Int,
    /** The ancestor that changed (equivalent to AncestorEvent.getAncestor). Always [component] itself in WinUI4K (see the class description). */
    val ancestor: WComponent?,
    /** The parent of [ancestor] (equivalent to AncestorEvent.getAncestorParent). */
    val ancestorParent: WComponent?,
) : EventObject(source) {
    /** The component the listener was added to (equivalent to AncestorEvent.getComponent). The same as [getSource]. */
    val component: WComponent get() = source as WComponent

    companion object {
        /** Appeared on screen (the same value as Swing's AncestorEvent.ANCESTOR_ADDED). */
        const val ANCESTOR_ADDED: Int = 1

        /** Disappeared from the screen (the same value as Swing's AncestorEvent.ANCESTOR_REMOVED). */
        const val ANCESTOR_REMOVED: Int = 2

        /** The position within the window changed (the same value as Swing's AncestorEvent.ANCESTOR_MOVED). */
        const val ANCESTOR_MOVED: Int = 3
    }
}

/**
 * AncestorListener-like: the listener passed to [WComponent.addAncestorListener]. As in Swing, implement all three methods
 * (Swing has no Adapter either).
 */
interface WAncestorListener : EventListener {
    /** Called after the component appears on screen (placed in a window, or it or an ancestor was shown). */
    fun ancestorAdded(event: WAncestorEvent)

    /** Called after the component disappears from the screen (removed from a window, or it or an ancestor was hidden). */
    fun ancestorRemoved(event: WAncestorEvent)

    /** Called after the position within the window of a component on screen changes because it or an ancestor moved. */
    fun ancestorMoved(event: WAncestorEvent)
}
