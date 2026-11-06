package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * PopupMenuEvent-style: an event that tells a [WPopupMenuListener] that a popup menu opens, closes or is canceled.
 * As with Swing's PopupMenuEvent, it only carries [getSource] (`source` in Kotlin), and the source is the owner of the popup menu
 * (the component that provides the menu, or the Flyout).
 */
class WPopupMenuEvent(source: Any) : EventObject(source)

/**
 * PopupMenuListener-style: a listener that receives changes in a popup menu's visibility.
 * As in Swing, it has three methods. [WPopupMenuAdapter] also exists so you do not have to write empty methods you do not use.
 */
interface WPopupMenuListener : EventListener {
    /** Called just before the popup menu becomes visible. */
    fun popupMenuWillBecomeVisible(e: WPopupMenuEvent)

    /** Called just before the popup menu becomes invisible. */
    fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent)

    /** Called when the popup menu is canceled without choosing an item (in Swing, before [popupMenuWillBecomeInvisible]). */
    fun popupMenuCanceled(e: WPopupMenuEvent)
}
