package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The [WAncestorListener]s of one component and the native handlers that call them.
 *
 * - ancestorAdded / ancestorRemoved: when whether it is "on screen" (connected to a window's visual tree ([WComponent.isDisplayable]),
 *   with it and all its visual tree parents visible) changes. Being placed in or removed from a window is noticed with FrameworkElement.Loaded / Unloaded, and changes
 *   to an ancestor's Visibility through the layout they cause (FrameworkElement.LayoutUpdated). As in Swing, merely hiding the window itself
 *   does not change it (unlike [WComponent.isShowing])
 * - ancestorMoved: when the position seen from the top left of the window content (UIElement.TransformToVisual(null)) changes while on screen
 *   (compared on every LayoutUpdated). Unlike Swing, it does not come when the window is merely moved on the screen
 *
 * Every trigger is checked together on the next turn of the UI thread before calling (so as not to run user code in the middle of a layout; Swing's
 * AncestorNotifier also calls after receiving a ComponentEvent). When unloading and loading again finish within one turn, as when removing and
 * re-adding to another parent, ancestorRemoved is called and then ancestorAdded (the same as Swing's remove and add).
 *
 * The handlers are registered only while there are listeners. None of them strongly captures the component or this object;
 * they look up the component from [WComponent.listenerAnchor].
 */
internal class AncestorEvents(private val owner: WComponent) {
    private val listeners = ArrayList<WAncestorListener>()

    private var loadedToken = 0L
    private var unloadedToken = 0L
    private var layoutUpdatedToken = 0L

    /** Whether it was on screen when last notified (or when the listener was added). */
    private var lastShowing = false

    /** The position seen from the top left of the window content when last checked (null when not on screen). */
    private var lastPosition: Pair<Double, Double>? = null

    /** Whether Unloaded came since the previous check (to report ancestorRemoved / ancestorAdded when removed and re-added). */
    private var unloadedSinceCheck = false

    /** Whether a check on the next turn is already scheduled (combines consecutive LayoutUpdated into one). */
    private var checkScheduled = false

    fun add(listener: WAncestorListener) {
        if (listeners.isEmpty()) subscribe()
        listeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    fun remove(listener: WAncestorListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
        if (listeners.isEmpty()) unsubscribe()
    }

    fun listeners(): Array<WAncestorListener> = listeners.toTypedArray()

    private fun subscribe() {
        val anchor = owner.listenerAnchor
        lastShowing = isShowingInWindow()
        lastPosition = if (lastShowing) position() else null
        unloadedSinceCheck = false
        loadedToken = owner.frameworkElement.addEventHandler(
            "WinUI4K.AncestorListenerLoaded",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> anchor.get()?.ancestorEventsOrNull?.scheduleCheck() }
        unloadedToken = owner.frameworkElement.addEventHandler(
            "WinUI4K.AncestorListenerUnloaded",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Unloaded,
        ) { _, _ -> anchor.get()?.ancestorEventsOrNull?.onUnloaded() }
        layoutUpdatedToken = owner.frameworkElement.addEventHandler(
            "WinUI4K.AncestorListenerLayoutUpdated",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.IFrameworkElement_add_LayoutUpdated,
        ) { _, _ -> anchor.get()?.ancestorEventsOrNull?.scheduleCheck() }
    }

    private fun unsubscribe() {
        owner.frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_Loaded, loadedToken)
        owner.frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_Unloaded, unloadedToken)
        owner.frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_LayoutUpdated, layoutUpdatedToken)
    }

    private fun onUnloaded() {
        unloadedSinceCheck = true
        scheduleCheck()
    }

    private fun scheduleCheck() {
        if (checkScheduled || listeners.isEmpty()) return
        checkScheduled = true
        WinUiUtilities.invokeLater {
            checkScheduled = false
            check()
        }
    }

    private fun check() {
        if (listeners.isEmpty()) return
        val showing = isShowingInWindow()
        val reattached = unloadedSinceCheck && lastShowing && showing
        unloadedSinceCheck = false
        val position = if (showing) position() else null
        val moved = lastShowing && showing && position != lastPosition
        val wasShowing = lastShowing
        lastShowing = showing
        lastPosition = position
        when {
            reattached -> {
                fire(WAncestorEvent.ANCESTOR_REMOVED)
                fire(WAncestorEvent.ANCESTOR_ADDED)
            }
            showing != wasShowing -> fire(if (showing) WAncestorEvent.ANCESTOR_ADDED else WAncestorEvent.ANCESTOR_REMOVED)
            moved -> fire(WAncestorEvent.ANCESTOR_MOVED)
        }
    }

    /** Whether it is on screen (ignores whether the window itself is shown; [WComponent.isShowing] without the [WFrame] visibility). */
    private fun isShowingInWindow(): Boolean = owner.isDisplayable && ComponentShowing.isVisibleInTree(owner)

    /** The top left of this component seen from the top left of the window content (DIP). */
    private fun position(): Pair<Double, Double> = VisualHitTest.transform(owner.inspectable, null, 0.0, 0.0)

    private fun fire(id: Int) {
        if (listeners.isEmpty()) return
        val event = WAncestorEvent(owner, id, owner, owner.parent)
        for (listener in listeners.toList()) {
            when (id) {
                WAncestorEvent.ANCESTOR_ADDED -> listener.ancestorAdded(event)
                WAncestorEvent.ANCESTOR_REMOVED -> listener.ancestorRemoved(event)
                else -> listener.ancestorMoved(event)
            }
        }
    }
}
