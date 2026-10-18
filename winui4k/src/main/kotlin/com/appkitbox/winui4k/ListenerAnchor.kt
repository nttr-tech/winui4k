package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.Guid
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference

/**
 * A reference that lets event handlers left on the native object look up the wrapper that owns the listeners ([owner])
 * (use only from the UI thread).
 *
 * If a handler captured the wrapper strongly, it would form the cycle "native object → handler → wrapper → native object",
 * and the wrapper would not be collected even after being removed from the window (doc/memory_management.md,
 * "Handling cycles that cross the boundary"). So the wrapper is referenced weakly, and while there are listeners it is
 * held strongly only in the following cases, so that the wrapper can be passed as the event source even if the user does
 * not hold it.
 * - From when a listener is added until the element leaves the visual tree (Unloaded) (while being assembled before it
 *   is put on a window)
 * - From Loaded to Unloaded (while shown)
 *
 * After Unloaded, the handler attached to the root element of the detached subtree holds the wrapper, and on the root's
 * next Loaded the element itself takes it back. While the native side keeps the subtree to show it again (as with a
 * Flyout or tabs), the wrapper stays alive; if the whole subtree is discarded, the wrapper can be collected along with
 * the destruction of the root (the root references its children but the children do not reference the root, so there is
 * no cycle). When the detached element is itself the root, nothing holds the wrapper.
 * WinUI does not return the parent of an element that is not in the tree (both VisualTreeHelper.GetParent and
 * FrameworkElement.Parent are null), so the ancestors at the time of Loaded are remembered through weak references, and
 * on Unloaded the root is found as "the topmost ancestor that is no longer in the tree".
 * An element that is never put in the tree is not collected until all of its listeners are removed.
 */
internal class ListenerAnchor<T : WComponent>(owner: T) {
    private val reference = WeakReference(owner)

    /** The wrapper while it is held strongly. */
    private var pinned: T? = null

    private var listenerCount = 0

    /**
     * IWeakReferences to the ancestors (from parent to root) while in the tree. Used on Unloaded to find the root of the
     * detached subtree.
     */
    private var ancestors: List<ComPtr> = emptyList()

    init {
        // The handlers capture only this object (capturing owner would create a cycle)
        owner.frameworkElement.addEventHandler(
            "WinUI4K.ListenerAnchorLoadedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { sender, _ -> onLoaded(ComPtr(sender)) }
        owner.frameworkElement.addEventHandler(
            "WinUI4K.ListenerAnchorUnloadedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Unloaded,
        ) { sender, _ -> onUnloaded(ComPtr(sender)) }
    }

    /**
     * The wrapper. null if it has been collected (when it was collected while not in the tree and only the native object
     * was shown again).
     */
    fun get(): T? = pinned ?: reference.get()

    /**
     * A listener was added. If it is the first listener, start holding the wrapper strongly (and remember the ancestors if
     * in the tree).
     */
    fun listenerAdded() {
        if (listenerCount++ > 0) return
        val owner = reference.get() ?: return
        pinned = owner
        if (isLoaded(owner.frameworkElement)) recordAncestors(owner.frameworkElement)
    }

    /** A listener was removed. If it was the last listener, stop holding the wrapper strongly. */
    fun listenerRemoved() {
        if (listenerCount > 0 && --listenerCount == 0) {
            pinned = null
            forgetAncestors()
        }
    }

    /** Holds [owner] strongly again (when the root of the detached subtree is put back in the tree). */
    private fun repin(owner: T) {
        if (listenerCount > 0) pinned = owner
    }

    private fun onLoaded(element: ComPtr) { // element is borrowed (do not Release)
        if (listenerCount == 0) return
        pinned = reference.get()
        recordAncestors(element)
    }

    private fun onUnloaded(element: ComPtr) { // element is borrowed (do not Release)
        // When reparenting, the old Unloaded can arrive after the new Loaded, so do nothing if it is still in the tree
        if (isLoaded(element)) return
        val owner = pinned
        pinned = null
        val root = if (owner != null) detachedRoot() else null
        forgetAncestors()
        if (owner != null && root != null) {
            try {
                retainUntilLoaded(root, owner)
            } finally {
                root.release()
            }
        }
    }

    private fun recordAncestors(element: ComPtr) {
        forgetAncestors()
        val recorded = ArrayList<ComPtr>()
        var current = Xaml.parentOf(element)
        while (current != null) {
            recorded += weakReferenceOf(current)
            val parent = Xaml.parentOf(current)
            current.release()
            current = parent
        }
        ancestors = recorded
    }

    private fun forgetAncestors() {
        ancestors.forEach { it.release() }
        ancestors = emptyList()
    }

    /**
     * The topmost of the remembered ancestors that has not been destroyed yet and is no longer in the tree
     * (FrameworkElement). null if there is none.
     */
    private fun detachedRoot(): ComPtr? {
        var root: ComPtr? = null
        for (weak in ancestors) {
            // On reaching an ancestor that is destroyed or still in the tree, the one just below it is the root of the
            // detached subtree
            val ancestor = resolveUnloaded(weak) ?: break
            root?.release()
            root = ancestor
        }
        return root
    }

    /** The FrameworkElement that [weak] points to, if it is not destroyed yet and not in the tree. Otherwise null. */
    private fun resolveUnloaded(weak: ComPtr): ComPtr? {
        val element = weak.getPtrOrNull(RESOLVE, Guid.of(XamlInterop.IID_IFrameworkElement)) ?: return null
        if (!element.getBool(XamlInterop.IFrameworkElement_get_IsLoaded)) return element
        element.release()
        return null
    }

    /**
     * Makes the Loaded handler of [root] hold [owner]; on that Loaded, the element itself takes it back and the handler is
     * removed.
     */
    private fun retainUntilLoaded(root: ComPtr, owner: T) {
        val retention = RootRetention(this, owner)
        retention.token = root.addEventHandler(
            "WinUI4K.ListenerAnchorRetention",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { rootSender, _ -> retention.onRootLoaded(ComPtr(rootSender)) }
    }

    /** A strong reference to the wrapper, held by the Loaded handler of the root of the detached subtree. */
    private class RootRetention<T : WComponent>(private val anchor: ListenerAnchor<T>, owner: T) {
        private var owner: T? = owner
        var token = 0L

        fun onRootLoaded(root: ComPtr) { // root is borrowed (do not Release)
            val retained = owner ?: return
            owner = null
            anchor.repin(retained)
            val rootElement = root.queryInterface(XamlInterop.IID_IFrameworkElement)
            try {
                rootElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_Loaded, token)
            } finally {
                rootElement.release()
            }
        }
    }

    private companion object {
        /** IWeakReferenceSource (weakreference.h). GetWeakReference(out IWeakReference) is vtbl[3]. */
        const val IID_IWEAK_REFERENCE_SOURCE = "00000038-0000-0000-c000-000000000046"
        const val GET_WEAK_REFERENCE = 3

        /** IWeakReference.Resolve(REFIID, out IInspectable) is vtbl[3]. Returns null if the object has been destroyed. */
        const val RESOLVE = 3

        fun isLoaded(element: ComPtr): Boolean {
            val frameworkElement = element.queryInterface(XamlInterop.IID_IFrameworkElement)
            return try {
                frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded)
            } finally {
                frameworkElement.release()
            }
        }

        fun weakReferenceOf(target: ComPtr): ComPtr {
            val source = target.queryInterface(IID_IWEAK_REFERENCE_SOURCE)
            return try {
                source.getPtr(GET_WEAK_REFERENCE)
            } finally {
                source.release()
            }
        }
    }
}
