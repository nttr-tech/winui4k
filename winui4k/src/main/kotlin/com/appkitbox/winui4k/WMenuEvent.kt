package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * MenuEvent-like: an event that tells a [WMenuListener] that a menu ([WMenuBarItem]) opened or closed.
 * As with Swing's MenuEvent, it has only [getSource] (`source` in Kotlin), and the source is the menu ([WMenuBarItem]).
 */
class WMenuEvent(source: Any) : EventObject(source)

/**
 * MenuListener-like: a listener that receives opening and closing of a menu bar menu ([WMenuBarItem]).
 * As in Swing, it has three methods. There is also [WMenuAdapter] so that unused methods do not have to be written empty.
 */
interface WMenuListener : EventListener {
    /** Called when the menu opens (is selected). */
    fun menuSelected(e: WMenuEvent)

    /** Called when the menu closes (is deselected). */
    fun menuDeselected(e: WMenuEvent)

    /**
     * Called when the menu is canceled. Swing's JMenu never calls this method itself either (nothing calls fireMenuCanceled), so
     * WinUI4K does not call it. To know about cancellations, use [WPopupMenuListener.popupMenuCanceled] of the [WMenuFlyout] containing the menu items.
     */
    fun menuCanceled(e: WMenuEvent)
}

/**
 * An empty implementation of [WMenuListener]: override only the methods you need (in Java, it can be written as the anonymous class
 * `new WMenuAdapter() { ... }`. Swing has no MenuAdapter, but WinUI4K provides one in the same form as its other multi-method listeners).
 */
abstract class WMenuAdapter : WMenuListener {
    override fun menuSelected(e: WMenuEvent) {
        // Does nothing
    }

    override fun menuDeselected(e: WMenuEvent) {
        // Does nothing
    }

    override fun menuCanceled(e: WMenuEvent) {
        // Does nothing
    }
}
