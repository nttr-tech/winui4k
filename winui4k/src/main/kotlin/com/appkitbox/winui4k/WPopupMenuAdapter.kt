package com.appkitbox.winui4k

/**
 * An empty implementation of [WPopupMenuListener]: override only the methods you need (in Java, write an anonymous class
 * `new WPopupMenuAdapter() { ... }`. Swing's PopupMenuListener has no Adapter, but
 * WinUI4K provides one, as it does for its other multi-method listeners).
 */
abstract class WPopupMenuAdapter : WPopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
        // Do nothing
    }

    override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
        // Do nothing
    }

    override fun popupMenuCanceled(e: WPopupMenuEvent) {
        // Do nothing
    }
}
