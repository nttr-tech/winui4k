package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.Dispatcher
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference

/**
 * A shared part that remembers operations requested before being added to the screen and doing the first layout (scroll positions, selection visuals, etc.)
 * and performs them after being added and laid out. Operations that work in Swing even when called before or right after showing either do not work
 * in WinUI before being added (before Loaded) or before the first layout, or are overwritten by the first layout.
 *
 * After layout ([isLaidOut]), a request is performed right away. Otherwise only the last request of each kind (the key of [runOrDefer]) is remembered
 * and performed after the element's Loaded (in the next turn of the message loop; Loaded arrives during layout, and layout may continue after it).
 * While requests are remembered, [WComponent.listenerAnchor] keeps the wrapper from being collected even if the user does not hold it.
 */
internal class LaidOutRequests(private val owner: WComponent) {
    private val pending = LinkedHashMap<Any, () -> Unit>()

    private var loadedSubscribed = false

    /** Whether the work for the next turn is already scheduled (merged into one even if Loaded comes repeatedly). */
    private var flushScheduled = false

    /** Whether it is connected to the window's visual tree and its size has been decided at least once. */
    fun isLaidOut(): Boolean = owner.isDisplayable && (owner.actualWidth > 0.0 || owner.actualHeight > 0.0)

    /**
     * Whether it is on a window (or in the content of a window that was just shown) but has not done the first layout yet. The content of a window that was just shown
     * does not have a XamlRoot yet ([WComponent.isDisplayable] is false), but it is laid out once loaded, so it can wait.
     */
    fun awaitsFirstLayout(): Boolean = !isLaidOut() && (owner.isDisplayable || WFrame.isInShownFrame(owner))

    /** Performs [action] right away if laid out, and otherwise remembers it as the request of [key] (dropping the previous request of the same [key]). */
    fun runOrDefer(key: Any, action: () -> Unit) {
        if (pending.isEmpty() && isLaidOut()) {
            action()
            return
        }
        if (pending.isEmpty()) owner.listenerAnchor.listenerAdded()
        pending.remove(key)
        pending[key] = action
        subscribeLoaded()
        // Check in the next turn too, so it also works right after being added to a window (when an element whose Loaded already happened is moved, etc.)
        scheduleFlush()
    }

    /** Drops the request of [key] if remembered (when another operation overrides it later). */
    fun cancel(key: Any) {
        if (pending.remove(key) != null && pending.isEmpty()) owner.listenerAnchor.listenerRemoved()
    }

    /** Whether the request of [key] is remembered. */
    fun isPending(key: Any): Boolean = key in pending

    private fun subscribeLoaded() {
        if (loadedSubscribed) return
        loadedSubscribed = true
        // The handler stays on the native side, so this part is not captured strongly (while requests are remembered, listenerAnchor keeps the whole wrapper)
        val requests = WeakReference(this)
        owner.frameworkElement.addEventHandler(
            "WinUI4K.LaidOutRequestsLoaded",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> requests.get()?.scheduleFlush() }
    }

    private fun scheduleFlush() {
        if (flushScheduled) return
        flushScheduled = true
        Dispatcher.invokeLater {
            flushScheduled = false
            flush()
        }
    }

    private fun flush() {
        if (pending.isEmpty() || !isLaidOut()) return
        val actions = pending.values.toList()
        pending.clear()
        owner.listenerAnchor.listenerRemoved()
        for (action in actions) action()
    }
}
