package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The list of input listeners (focus, mouse, key) of a single component, and the native handlers that call them.
 *
 * Each kind of native handler is registered only once, when the first listener of that kind is added, and stays registered
 * even after all listeners are removed (it does nothing when there are no listeners). Since the handlers remain on the
 * native side, they hold no strong reference to the component or to this object, and look up the component through
 * [WComponent.listenerAnchor] (the listener lists live in this object owned by the component, so even if a listener
 * references the component, the cycle does not cross the native boundary).
 */
internal class ComponentInput(private val owner: WComponent) {
    private val focusListeners = ArrayList<WFocusListener>()
    private var focusHandlersAdded = false

    /**
     * The focus state last reported (true = focusGained was reported). WinUI may send GotFocus twice for the first focus
     * in a window (doc/verification.md), and LostFocus / GotFocus also arrive when focus moves between the parts inside a
     * composite control, so this reports only when the state changes.
     */
    private var focusOwned = false

    fun addFocusListener(listener: WFocusListener) {
        if (!focusHandlersAdded) {
            addFocusHandlers()
            focusHandlersAdded = true
        }
        if (focusListeners.isEmpty()) focusOwned = ownsFocus()
        focusListeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    fun removeFocusListener(listener: WFocusListener) {
        val index = focusListeners.indexOfLast { it === listener }
        if (index < 0) return
        focusListeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    fun focusListeners(): Array<WFocusListener> = focusListeners.toTypedArray()

    private fun addFocusHandlers() {
        val anchor = owner.listenerAnchor
        owner.uiElement.addEventHandler(
            "WinUI4K.FocusListenerGotFocus",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IUIElement_add_GotFocus,
        ) { _, args -> anchor.get()?.input?.onGotFocus(args) }
        owner.uiElement.addEventHandler(
            "WinUI4K.FocusListenerLostFocus",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IUIElement_add_LostFocus,
        ) { _, args -> anchor.get()?.input?.onLostFocus(args) }
    }

    /** GotFocus (also delivered when bubbled up from descendants). Reports only when this component itself gained focus. */
    private fun onGotFocus(args: Ptr) {
        if (focusListeners.isEmpty() || focusOwned || !isFromOwnPart(args)) return
        focusOwned = true
        val event = WFocusEvent(owner, WFocusEvent.FOCUS_GAINED)
        for (listener in focusListeners.toList()) listener.focusGained(event)
    }

    /** LostFocus. Does not report when focus merely moved between parts of this component itself (composite controls). */
    private fun onLostFocus(args: Ptr) {
        if (focusListeners.isEmpty() || !focusOwned || !isFromOwnPart(args)) return
        if (ownsFocus()) return
        focusOwned = false
        val event = WFocusEvent(owner, WFocusEvent.FOCUS_LOST)
        for (listener in focusListeners.toList()) listener.focusLost(event)
    }

    private fun isFromOwnPart(args: Ptr): Boolean {
        val source = Xaml.originalSource(args) ?: return false
        return try {
            isOwnPart(owner, source)
        } finally {
            source.release()
        }
    }

    /** Whether the element that currently has focus (FocusManager.GetFocusedElement) is a part of this component itself. */
    private fun ownsFocus(): Boolean {
        val root = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return false
        val focused = try {
            Xaml.focusedElement(root)
        } finally {
            root.release()
        } ?: return false
        return try {
            isOwnPart(owner, focused)
        } finally {
            focused.release()
        }
    }
}

/**
 * Whether [element] is [owner] itself or a part of [owner]'s template (such as the TextBox inside a NumberBox).
 * The content of [owner] (ContentControl.Content or Panel.Children) and its descendants are excluded (in Swing, too,
 * events of children do not reach the parent).
 *
 * Walks the visual tree parents from [element] up to [owner], and treats [element] as content if the logical tree parent
 * (FrameworkElement.Parent) of an element on the way is [owner]. The logical tree parent of a template part is never
 * [owner] (the root of the template has null). Returns false when [owner] is not reached (such as inside a Popup).
 */
internal fun isOwnPart(owner: WComponent, element: ComPtr): Boolean {
    if (Xaml.sameObject(element, owner.inspectable)) return true
    var current = element.queryInterface(XamlInterop.IID_IDependencyObject)
    try {
        repeat(MAX_PART_DEPTH) {
            if (isLogicalChildOf(current, owner)) return false
            val parent = Xaml.parentOf(current) ?: return false
            current.release()
            current = parent
            if (Xaml.sameObject(current, owner.inspectable)) return true
        }
        return false
    } finally {
        current.release()
    }
}

/** Whether the logical tree parent (FrameworkElement.Parent) of [element] is [owner]. */
private fun isLogicalChildOf(element: ComPtr, owner: WComponent): Boolean {
    val frameworkElement = element.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement) ?: return false
    val logicalParent = try {
        frameworkElement.getPtrOrNull(XamlInterop.IFrameworkElement_get_Parent)
    } finally {
        frameworkElement.release()
    } ?: return false
    return try {
        Xaml.sameObject(logicalParent, owner.inspectable)
    } finally {
        logicalParent.release()
    }
}

/** The maximum depth for walking the visual tree (prevents infinite loops on cycles or malformed trees). */
private const val MAX_PART_DEPTH = 512
