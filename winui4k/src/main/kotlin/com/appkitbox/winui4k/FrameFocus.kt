package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.Guid
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference

/**
 * Finds the focus owner of a [WFrame] (Window.getFocusOwner / getMostRecentFocusOwner). Remembers the element that last got the focus
 * through GotFocus on the root of the window content and [rememberFocused] when the window becomes inactive. Used only by the UI thread.
 */
internal class FrameFocusTracker {
    /** The element that last got the focus. Written by the GotFocus handler on the content root. */
    private val memory = FrameFocusMemory()

    /** The root subscribed to GotFocus (the element put in Window.Content) and its token. */
    private var root: WComponent? = null
    private var token = 0L

    /**
     * Subscribes to GotFocus of [newRoot] put in Window.Content (the focus of elements inside bubbles up) and remembers the element that got the focus.
     * Unsubscribes from the previous root. The handler stays in native code, so it captures only [memory] (neither the WFrame nor wrappers).
     */
    fun track(newRoot: WComponent) {
        root?.uiElement?.removeEventHandler(XamlInterop.IUIElement_remove_GotFocus, token)
        val memory = memory
        token = newRoot.uiElement.addEventHandler(
            "WinUI4K.FrameGotFocusHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IUIElement_add_GotFocus,
        ) { _, args ->
            val element = Xaml.originalSource(args)
            if (element != null) {
                try {
                    memory.remember(element)
                } finally {
                    element.release()
                }
            }
        }
        root = newRoot
    }

    /**
     * Remembers the element that currently has the focus in the window of [content] (the element put in Window.Content). GotFocus may not come
     * when the window is not in the foreground (doc/verification.md), so this is also called when the window becomes inactive.
     */
    fun rememberFocused(content: WComponent) {
        val focused = focusedElement(content) ?: return
        try {
            memory.remember(focused)
        } finally {
            focused.release()
        }
    }

    /** The deepest wrapper in [roots] that contains the element with the focus in the window of [content]. */
    fun focusOwner(content: WComponent, roots: List<WComponent>): WComponent? {
        val focused = focusedElement(content) ?: return null
        return try {
            WrapperLookup.nearestWrapper(roots, focused)
        } finally {
            focused.release()
        }
    }

    /**
     * The wrapper containing the element that last got the focus. If none is remembered or that element is no longer in [roots], the one for the first
     * focusable element in [content] (FocusManager.FindFirstFocusableElement).
     */
    fun mostRecent(content: WComponent, roots: List<WComponent>): WComponent? {
        memory.lastElement()?.let { last ->
            try {
                WrapperLookup.nearestWrapper(roots, last)?.let { return it }
            } finally {
                last.release()
            }
        }
        val first = Xaml.firstFocusable(content.uiElement) ?: return null
        return try {
            WrapperLookup.nearestWrapper(roots, first)
        } finally {
            first.release()
        }
    }

    /** The component requested with [WComponent.requestFocusInWindow] between being shown and loading the content ([WFrame.deferFocus]). */
    var pendingFocus: WComponent? = null

    /** The root (a weak reference) whose Loaded [focusAfterLoad] is waiting for, and the token of that Loaded handler. null if not waiting. */
    private var loadWait: Pair<WeakReference<WComponent>, Long>? = null

    /** What to call when [focusAfterLoad] loads the root. */
    private var retryAfterLoad: (() -> Unit)? = null

    /**
     * As when a Swing Window gains focus, if no element has focus in the window that became active,
     * moves focus to [pendingFocus], the component that last gained focus, or the first component in [content] that can take focus,
     * in that order ([focusIfNone]). Right after being shown the content ([root]) is not loaded yet and UIElement.Focus fails,
     * so [retry] is called when it loads (Loaded) (WFrame calls this again).
     */
    fun focusOnActivation(content: WComponent, root: WComponent, retry: () -> Unit) {
        if (!root.isDisplayable) {
            focusAfterLoad(root, retry)
            return
        }
        val requested = pendingFocus
        pendingFocus = null
        if (requested != null && requested.focusInitially()) return
        focusIfNone(content, root)
    }

    /**
     * Calls [retry] once when [root] loads ([retry] must reference the WFrame weakly). The handler stays on the native side (the root), so
     * both this tracker and the root's wrapper are referenced weakly. In a window closed before loading, it can stay without Loaded ever coming. Capturing the root's wrapper
     * strongly kept "the root's native object -> handler -> the root's wrapper -> the children's wrappers" alive, and the children of the content of a window closed and released were
     * not collected (WrapperReleaseLayoutTest). It is also removed when closed ([forget]).
     */
    private fun focusAfterLoad(root: WComponent, retry: () -> Unit) {
        if (loadWait != null) return
        val self = WeakReference(this)
        val token = root.frameworkElement.addEventHandler(
            "WinUI4K.FrameInitialFocusLoadedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> ModalLoop.duringLayout { self.get()?.onRootLoaded() } }
        loadWait = WeakReference(root) to token
        retryAfterLoad = retry
    }

    private fun onRootLoaded() {
        val retry = retryAfterLoad ?: return
        stopWaitingForLoad()
        retry()
    }

    /** Removes the Loaded handler of [focusAfterLoad]. */
    private fun stopWaitingForLoad() {
        val (root, token) = loadWait ?: return
        root.get()?.frameworkElement?.removeEventHandler(XamlInterop.IFrameworkElement_remove_Loaded, token)
        loadWait = null
        retryAfterLoad = null
    }

    /**
     * If no element has focus in the window of [root] (the element set as Window.Content), moves focus to the element that last gained focus,
     * or, if it is gone, to the first element in [content] that can take focus (FocusManager.FindFirstFocusableElement).
     * moves focus. If the component containing the first element has the same focus place as in Swing ([WComponent.focusAsInitial]; the selected button
     * of a radio button group, etc.), focus goes there. No keyboard focus rectangle is shown ([focusWithoutKeyboardCue]).
     */
    private fun focusIfNone(content: WComponent, root: WComponent) {
        focusedElement(root)?.let {
            it.release()
            return
        }
        memory.lastElement()?.let { last ->
            try {
                if (focusWithoutKeyboardCue(last)) return
            } finally {
                last.release()
            }
        }
        val first = Xaml.firstFocusable(content.uiElement) ?: return
        try {
            if (WrapperLookup.nearestWrapper(listOf(content), first)?.focusAsInitial() == true) return
            focusWithoutKeyboardCue(first)
        } finally {
            first.release()
        }
    }

    /** Releases the remembered element and the requested component (when the window closes). */
    fun forget() {
        memory.forget()
        pendingFocus = null
        stopWaitingForLoad()
    }

    /** The element with the focus in the window (XamlRoot) of [content] (the caller releases it). */
    private fun focusedElement(content: WComponent): ComPtr? {
        val xamlRoot = content.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
        return try {
            Xaml.focusedElement(xamlRoot)
        } finally {
            xamlRoot.release()
        }
    }
}

/**
 * Puts focus on [element] (borrowed) without showing the keyboard focus rectangle (for the window's initial focus). true if it succeeds.
 *
 * The state is Pointer. With Programmatic, when the window's last input was the keyboard (right after launching with a key, etc.; when the window
 * is created, it depends on whether the system shows keyboard cues (InputFocusController.ShouldShowKeyboardCues)), WinUI coerces it to
 * Keyboard and draws a thick focus rectangle (CoerceFocusState in microsoft-ui-xaml's focusmgr.cpp). Swing's initial
 * focus is unobtrusive (moving it with the Tab key makes it Keyboard and shows the rectangle).
 * However, elements with AllowFocusOnInteraction false (AppBarButton etc.) do not accept Pointer focus
 * (ShouldUpdateFocus in FocusSelection.h), so it is set to true only while focus is put and then back to false.
 */
internal fun focusWithoutKeyboardCue(element: ComPtr): Boolean {
    val framework = element.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement) ?: return Xaml.focus(element, XamlInterop.FocusState_Pointer)
    try {
        val blocked = !framework.getBool(XamlInterop.IFrameworkElement_get_AllowFocusOnInteraction)
        if (blocked) framework.putBool(XamlInterop.IFrameworkElement_put_AllowFocusOnInteraction, true)
        try {
            return Xaml.focus(element, XamlInterop.FocusState_Pointer)
        } finally {
            if (blocked) framework.putBool(XamlInterop.IFrameworkElement_put_AllowFocusOnInteraction, false)
        }
    } finally {
        framework.release()
    }
}

/**
 * A holder that remembers, per [WFrame], the element that last got the focus (for Window.getMostRecentFocusOwner). The GotFocus handler on the window content root
 * (which stays in native code) captures only this, so neither the WFrame nor wrappers are held strongly. The element is held through a native weak reference
 * (IWeakReference), so an element removed from the tree and destroyed can no longer be read. Read and written only by the UI thread.
 */
private class FrameFocusMemory {
    private var last: ComPtr? = null

    /** Remembers [element] (borrowed) as the element that last got the focus. */
    fun remember(element: ComPtr) {
        val source = element.queryInterfaceOrNull(IID_IWEAK_REFERENCE_SOURCE) ?: return
        val weak = try {
            source.getPtrOrNull(GET_WEAK_REFERENCE)
        } finally {
            source.release()
        } ?: return
        last?.release()
        last = weak
    }

    /** The remembered element (the caller releases it). null if none is remembered or it has already been destroyed. */
    fun lastElement(): ComPtr? = last?.getPtrOrNull(RESOLVE, Guid.of(XamlInterop.IID_IDependencyObject))

    /** Releases the remembered element (when the window is closed). */
    fun forget() {
        last?.release()
        last = null
    }

    private companion object {
        /** IWeakReferenceSource (weakreference.h). GetWeakReference(out IWeakReference) is vtbl[3]. */
        const val IID_IWEAK_REFERENCE_SOURCE = "00000038-0000-0000-c000-000000000046"
        const val GET_WEAK_REFERENCE = 3

        /** IWeakReference.Resolve(REFIID, out IInspectable) is vtbl[3]. null if already destroyed. */
        const val RESOLVE = 3
    }
}

/**
 * Finds the wrapper containing a native element by following WinUI4K's parent-child relations (from the roots to the children) (no table from native
 * elements to wrappers is kept). Used by Window.getFocusOwner to turn the element with the focus (sometimes a template part) into a wrapper.
 */
internal object WrapperLookup {
    /**
     * The deepest wrapper, found by following children from [roots], that corresponds to [element] (borrowed) itself or one of its ancestors (visual tree parents;
     * elements inside a Popup go through the Popup). null if it is not inside any of [roots].
     */
    fun nearestWrapper(roots: List<WComponent>, element: ComPtr): WComponent? {
        val ancestors = ancestorIdentities(element)
        var found: WComponent? = null
        var level = roots
        while (true) {
            val next = level.firstOrNull { Xaml.identityOf(it.inspectable) in ancestors } ?: return found
            found = next
            level = childrenOf(next)
        }
    }

    /**
     * The children added through WinUI4K's API (the parent-child relations that decide [WComponent.parent]): the children of a [WContainer] and the child of a component with a single child.
     * For components that hold children in other ways, elements inside them are found as that component itself.
     */
    private fun childrenOf(component: WComponent): List<WComponent> = when (component) {
        is WContainer -> component.componentList
        is WBorder -> listOfNotNull(component.child)
        is WScrollPane -> listOfNotNull(component.content, component.columnHeaderView, component.rowHeaderView)
        is WScrollView -> listOfNotNull(component.content)
        is WExpander -> listOfNotNull(component.content)
        is WSplitView -> listOfNotNull(component.pane, component.content)
        is WSplitPane -> listOfNotNull(component.leftComponent, component.rightComponent)
        // The visual tree parent of a tab's content is not the tab (TabViewItem) but the display area inside the TabView, so list the tabs and their contents
        is WTabView -> (0 until component.tabCount).flatMap { index ->
            component.getTab(index).let { tab -> listOfNotNull(tab, tab.content) }
        }
        else -> emptyList()
    }

    /** The identities ([Xaml.identityOf]) of [element] and its ancestors. */
    private fun ancestorIdentities(element: ComPtr): Set<Long> {
        val identities = HashSet<Long>()
        var current: ComPtr? = element.queryInterfaceOrNull(XamlInterop.IID_IDependencyObject)
        var depth = 0
        while (current != null) {
            identities += Xaml.identityOf(current)
            val next = if (++depth < MAX_DEPTH) popupOf(current) ?: Xaml.parentOf(current) else null
            current.release()
            current = next
        }
        return identities
    }

    /**
     * If the logical tree parent (FrameworkElement.Parent) of [element] is a Popup, its IDependencyObject (the caller releases it).
     * The visual tree parent of a Popup's child is not the Popup but PopupRoot, so this is used to get back to the owner through the Popup.
     */
    private fun popupOf(element: ComPtr): ComPtr? {
        val frameworkElement = element.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement) ?: return null
        val parent = try {
            frameworkElement.getPtrOrNull(XamlInterop.IFrameworkElement_get_Parent)
        } finally {
            frameworkElement.release()
        } ?: return null
        return try {
            val popup = parent.queryInterfaceOrNull(XamlInterop.IID_IPopup) ?: return null
            try {
                popup.queryInterface(XamlInterop.IID_IDependencyObject)
            } finally {
                popup.release()
            }
        } finally {
            parent.release()
        }
    }

    /** The maximum depth followed (prevents infinite loops in cycles or broken trees). */
    private const val MAX_DEPTH = 512
}
