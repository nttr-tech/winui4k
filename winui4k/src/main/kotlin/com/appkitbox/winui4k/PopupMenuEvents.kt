package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference

/**
 * A shared part that calls the [WPopupMenuListener]s of a [WFlyoutBase] from FlyoutBase's Opening / Closing / Closed.
 *
 * - popupMenuWillBecomeVisible on Opening; on Closing, popupMenuCanceled (only when canceled) → popupMenuWillBecomeInvisible, in that order
 *   (canceled first, as with Swing's BasicPopupMenuUI). Listeners are called in the order added.
 * - Canceled means it closed after opening without an item being chosen (no Click of an item of the [WMenuFlyout] and no press of a button in the lower menu of a [WCommandBarFlyout]), and not by [WFlyoutBase.hide] /
 *   `isVisible = false` (light dismiss such as an outside click or Esc). In Swing too, setVisible(false) does not call
 *   canceled. Item Clicks are subscribed per item only while open (MenuFlyoutItem.Invoke closes the menu after Click).
 * - Reopening while closing (from Closing to Closed) is deferred until Closed ([showWhenClosed]). WinUI treats a closing flyout
 *   as still open and ignores ShowAt with the same target and position (CheckAndHandleOpenFlyout in FlyoutBase_partial.cpp).
 * - The native handlers capture this object through a weak reference, and this object holds the flyout wrapper weakly. Only while open
 *   (from Opening to Closed, including while waiting to reopen) does [openEvents] hold this object strongly and [pinned] the wrapper.
 *   If the handlers captured this object strongly, adding a listener that captures its own flyout would form a cycle across the boundary, native → handler → listener →
 *   wrapper → native, and it would not be collected. The handlers are added when the [WFlyoutBase] first opens, closes or adds a listener, and are never removed.
 */
internal class FlyoutPopupMenuEvents(owner: WFlyoutBase) {
    private val reference = WeakReference(owner)

    /** The wrapper while open (from Opening to Closed). Used as the event source. */
    private var pinned: WFlyoutBase? = null

    private val listeners = ArrayList<WPopupMenuListener>()
    private var subscribed = false

    /** Asked to close with [WFlyoutBase.hide] (that way of closing is not counted as a cancellation). Reset on Opening and Closed. */
    var hideRequested = false

    /** A mark of whether an item was chosen after opening. Item handlers capture only this (it is also attached to buttons in the lower menu of a [WCommandBarFlyout]). */
    val invocation = InvocationMark()

    /** Closing (from Closing to Closed). */
    private var closing = false

    /** The opening to run once it has finished closing ([showWhenClosed]). */
    private var pendingShow: (() -> Unit)? = null

    /** The items whose Click was subscribed while open, and their tokens. */
    private val itemTokens = ArrayList<Pair<WMenuFlyoutItem, Long>>()

    val all: Array<WPopupMenuListener> get() = listeners.toTypedArray()

    fun add(listener: WPopupMenuListener) {
        listeners += listener
    }

    /** Defers until Closed if closing, otherwise calls [show] right away. */
    fun showWhenClosed(isOpen: Boolean, show: () -> Unit) {
        if (closing && !isOpen) {
            pendingShow = show
        } else {
            pendingShow = null
            show()
        }
    }

    init {
        subscribe()
    }

    fun remove(listener: WPopupMenuListener) {
        listeners.remove(listener)
    }

    private fun subscribe() {
        if (subscribed) return
        val flyoutBase = reference.get()?.flyoutBase ?: return
        subscribed = true
        val events = WeakReference(this)
        flyoutBase.addEventHandler("WinUI4K.PopupMenuOpening", FoundationInterop.IID_EventHandler_Object, XamlInterop.IFlyoutBase_add_Opening) { _, _ ->
            events.get()?.onOpening()
        }
        flyoutBase.addEventHandler(
            "WinUI4K.PopupMenuClosing",
            XamlInterop.IID_TypedEventHandler_FlyoutBase_Closing,
            XamlInterop.IFlyoutBase_add_Closing,
        ) { _, _ -> events.get()?.onClosing() }
        flyoutBase.addEventHandler("WinUI4K.PopupMenuClosed", FoundationInterop.IID_EventHandler_Object, XamlInterop.IFlyoutBase_add_Closed) { _, _ ->
            events.get()?.onClosed()
        }
    }

    private fun onOpening() {
        val owner = reference.get() ?: return
        pinned = owner
        openEvents += this
        invocation.invoked = false
        hideRequested = false
        closing = false
        pendingShow = null
        unhookItems()
        if (listeners.isNotEmpty()) hookItems(owner.invocableMenuItems())
        fire(owner) { listener, e -> listener.popupMenuWillBecomeVisible(e) }
    }

    private fun onClosing() {
        // CommandBarFlyout cancels the first Closing for its closing animation and calls Hide again after the animation, so
        // Closing comes twice (the Closing handler in CommandBarFlyout.cpp). From the second one until Closed, do not call the listeners
        if (closing) return
        closing = true
        val owner = pinned ?: reference.get() ?: return
        if (!invocation.invoked && !hideRequested) fire(owner) { listener, e -> listener.popupMenuCanceled(e) }
        fire(owner) { listener, e -> listener.popupMenuWillBecomeInvisible(e) }
    }

    private fun onClosed() {
        unhookItems()
        hideRequested = false
        closing = false
        pinned = null
        openEvents -= this
        pendingShow?.let { show ->
            pendingShow = null
            // Closed happens before the record of finishing closing (the open flyout in FlyoutMetadata) is removed, so open after that
            // (OnPresenterUnloaded in FlyoutBase_partial.cpp)
            WinUiUtilities.invokeLater { show() }
        }
    }

    private fun hookItems(items: List<WMenuFlyoutItem>) {
        for (item in items) {
            val token = item.menuFlyoutItem.addEventHandler(
                "WinUI4K.PopupMenuItemInvoked",
                XamlInterop.IID_RoutedEventHandler,
                XamlInterop.IMenuFlyoutItem_add_Click,
            ) { _, _ -> invocation.invoked = true }
            itemTokens += item to token
        }
    }

    private fun unhookItems() {
        itemTokens.forEach { (item, token) -> item.menuFlyoutItem.removeEventHandler(XamlInterop.IMenuFlyoutItem_remove_Click, token) }
        itemTokens.clear()
    }

    private inline fun fire(source: WFlyoutBase, call: (WPopupMenuListener, WPopupMenuEvent) -> Unit) {
        if (listeners.isEmpty()) return
        val event = WPopupMenuEvent(source)
        listeners.toList().forEach { call(it, event) }
    }
    private companion object {
        /** The calls of open flyouts (from Opening to Closed). Read and written only by the UI thread. */
        val openEvents = HashSet<FlyoutPopupMenuEvents>()
    }
}

/** The "an item was chosen after opening" mark of [FlyoutPopupMenuEvents]. Native item handlers capture only this (no cycle). */
internal class InvocationMark {
    var invoked = false
}

/**
 * Calls the [WMenuListener]s of a [WMenuBarItem] from Opening (menuSelected) and Closed (menuDeselected) of the open menu MenuBarItem creates when its template is applied
 * (the ContextFlyout of the template part "ContentButton"; PopulateContent in controls/dev/MenuBar/MenuBarItem.cpp).
 * That menu exists only after it is loaded, so it is looked up on every Loaded and subscribed if not subscribed yet
 * (calling ApplyTemplate before it is in the tree makes MenuBarItem unable to find its parent MenuBar, so the template is not applied here).
 * The handlers capture only this object, and the source wrapper is looked up from [anchor].
 */
internal class MenuBarItemMenuEvents(private val anchor: ListenerAnchor<WComponent>) {
    private val listeners = ArrayList<WMenuListener>()
    private var loadedSubscribed = false

    /** The identities (IUnknown addresses) of the subscribed open menus. No native references are held (no cycle). */
    private var hookedIdentity = 0L

    val all: Array<WMenuListener> get() = listeners.toTypedArray()

    fun add(item: WMenuBarItem, listener: WMenuListener) {
        listeners += listener
        anchor.listenerAdded()
        if (!loadedSubscribed) {
            loadedSubscribed = true
            item.frameworkElement.addEventHandler(
                "WinUI4K.MenuBarItemLoadedForMenuListener",
                XamlInterop.IID_RoutedEventHandler,
                XamlInterop.IFrameworkElement_add_Loaded,
            ) { sender, _ -> hook(ComPtr(sender)) }
        }
        hook(item.inspectable)
    }

    fun remove(listener: WMenuListener) {
        if (listeners.remove(listener)) anchor.listenerRemoved()
    }

    /** Subscribes to the open menu of the applied template of [control] (a borrowed reference) if not yet. */
    private fun hook(control: ComPtr) {
        val flyout = openingMenuOf(control) ?: return
        try {
            val identity = identityOf(flyout)
            if (identity == hookedIdentity) return
            hookedIdentity = identity
            flyout.addEventHandler("WinUI4K.MenuBarItemOpening", FoundationInterop.IID_EventHandler_Object, XamlInterop.IFlyoutBase_add_Opening) { _, _ ->
                fire { listener, e -> listener.menuSelected(e) }
            }
            flyout.addEventHandler("WinUI4K.MenuBarItemClosed", FoundationInterop.IID_EventHandler_Object, XamlInterop.IFlyoutBase_add_Closed) { _, _ ->
                fire { listener, e -> listener.menuDeselected(e) }
            }
        } finally {
            flyout.release()
        }
    }

    private inline fun fire(call: (WMenuListener, WMenuEvent) -> Unit) {
        val source = anchor.get() ?: return
        if (listeners.isEmpty()) return
        val event = WMenuEvent(source)
        listeners.toList().forEach { call(it, event) }
    }

    companion object {
        /** The ContextFlyout (IFlyoutBase) of "ContentButton" in the applied template. null if not yet. The caller releases it. */
        fun openingMenuOf(control: ComPtr): ComPtr? {
            val button = Xaml.appliedTemplatePartOrNull(control, "ContentButton") ?: return null
            val element = try {
                button.queryInterfaceOrNull(XamlInterop.IID_IUIElement)
            } finally {
                button.release()
            } ?: return null
            val flyout = try {
                element.getPtrOrNull(XamlInterop.IUIElement_get_ContextFlyout)
            } finally {
                element.release()
            } ?: return null
            return try {
                flyout.queryInterface(XamlInterop.IID_IFlyoutBase)
            } finally {
                flyout.release()
            }
        }

        fun identityOf(pointer: ComPtr): Long {
            val unknown = pointer.queryInterface(KComObject.IID_IUNKNOWN)
            return try {
                unknown.ptr.address
            } finally {
                unknown.release()
            }
        }
    }
}

/**
 * Calls the [WPopupMenuListener]s of a [WPopup] from the Popup's Opened / Closed. Popup has no Opening / Closing, so
 * popupMenuWillBecomeVisible is called after opening (Opened), and popupMenuCanceled (only for light dismiss) and
 * popupMenuWillBecomeInvisible after closing (Closed). Closing with [WPopup.hide] / `isVisible = false` is not a cancellation.
 * The handlers capture this object weakly, and the source wrapper is looked up from the weakly referenced [popup]. A Popup is not in the tree
 * (with [ListenerAnchor], which re-holds on Loaded / Unloaded, it would not be collected until the listeners are removed), so while it is open
 * the [WPopup] itself holds it strongly.
 */
internal class PopupPopupMenuEvents(private val popup: WeakReference<WPopup>) {
    private val listeners = ArrayList<WPopupMenuListener>()
    private var subscribed = false

    /** Asked to close with [WPopup.hide] (the next Closed is not counted as a cancellation). Reset on opening and on Closed. */
    var hideRequested = false

    val all: Array<WPopupMenuListener> get() = listeners.toTypedArray()

    fun add(popup: ComPtr, listener: WPopupMenuListener) {
        listeners += listener
        if (subscribed) return
        subscribed = true
        // The handlers capture this object weakly (capturing it strongly would form a cycle native → handler →
        // listener → wrapper → native when a listener capturing the popup is added). The WPopup holds this object, and while open the open list holds it along with the WPopup
        val events = WeakReference(this)
        popup.addEventHandler("WinUI4K.PopupMenuPopupOpened", FoundationInterop.IID_EventHandler_Object, XamlInterop.IPopup_add_Opened) { _, _ ->
            events.get()?.onOpened()
        }
        popup.addEventHandler("WinUI4K.PopupMenuPopupClosed", FoundationInterop.IID_EventHandler_Object, XamlInterop.IPopup_add_Closed) { _, _ ->
            events.get()?.onClosed()
        }
    }

    private fun onOpened() {
        fire { listener, e -> listener.popupMenuWillBecomeVisible(e) }
    }

    private fun onClosed() {
        val canceled = !hideRequested
        hideRequested = false
        if (canceled) fire { listener, e -> listener.popupMenuCanceled(e) }
        fire { listener, e -> listener.popupMenuWillBecomeInvisible(e) }
    }

    fun remove(listener: WPopupMenuListener) {
        listeners.remove(listener)
    }

    private inline fun fire(call: (WPopupMenuListener, WPopupMenuEvent) -> Unit) {
        val source = popup.get() ?: return
        if (listeners.isEmpty()) return
        val event = WPopupMenuEvent(source)
        listeners.toList().forEach { call(it, event) }
    }
}
