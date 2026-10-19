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
    /** The mouse listeners (created when the first mouse listener is added). */
    val mouse: MouseInput by lazy { MouseInput(owner) }

    /** The key listeners (created when the first key listener is added). */
    val keys: KeyInput by lazy { KeyInput(owner) }

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
        if (focusListeners.isEmpty()) focusOwned = ownsFocus(owner)
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
        if (ownsFocus(owner)) return
        focusOwned = false
        val event = WFocusEvent(owner, WFocusEvent.FOCUS_LOST)
        for (listener in focusListeners.toList()) listener.focusLost(event)
    }

    private fun isFromOwnPart(args: Ptr): Boolean {
        val source = Xaml.originalSource(args) ?: return false
        return try {
            isOwnFocusPart(owner, source)
        } finally {
            source.release()
        }
    }
}

/**
 * Whether the element that currently has focus (FocusManager.GetFocusedElement) is [owner] itself or a part of [owner]'s
 * template (including parts shown in a Popup inside the template; see [isOwnFocusPart]).
 */
internal fun ownsFocus(owner: WComponent): Boolean {
    val root = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return false
    val focused = try {
        Xaml.focusedElement(root)
    } finally {
        root.release()
    } ?: return false
    return try {
        isOwnFocusPart(owner, focused)
    } finally {
        focused.release()
    }
}

/**
 * [isOwnPart] for focus checks: parts shown in a Popup inside [owner]'s template (such as the items of a WComboBox
 * drop-down) are also treated as parts of [owner]. Just as JComboBox keeps focus while its popup is open, opening or
 * closing does not report focusLost / focusGained. The visual tree parent of a Popup's child is PopupRoot rather than the
 * Popup, so this moves to the Popup through the logical tree parent (FrameworkElement.Parent of Popup.Child is the Popup)
 * and continues from there.
 * The Popup of a Flyout (the picker of DatePicker / TimePicker, a [WFlyout] attached to a [WComponent], and so on) does
 * not belong to the tree, and which element it belongs to cannot be looked up with WinUI's public API, so it is not
 * treated as a part of [owner] (doc/verification.md).
 */
internal fun isOwnFocusPart(owner: WComponent, element: ComPtr): Boolean =
    classifyPart(owner, element, throughPopups = true) == PartKind.OWN

/**
 * Whether [element] is [owner] itself or a part of [owner]'s template (such as the TextBox inside a NumberBox).
 * The content of [owner] (ContentControl.Content or Panel.Children) and its descendants are excluded (in Swing, too,
 * events of children do not reach the parent).
 *
 * Walks the visual tree parents from [element] up to [owner], and treats [element] as content if the logical tree parent
 * (FrameworkElement.Parent) of an element on the way is [owner]. The logical tree parent of a template part is never
 * [owner] (the root of the template has null). Returns false when [owner] is not reached (such as inside a Popup).
 */
internal fun isOwnPart(owner: WComponent, element: ComPtr): Boolean = classifyPart(owner, element, throughPopups = false) == PartKind.OWN

/**
 * Whether the source [element] of a handled pointer event is inside a control (Control) within the content (children) of
 * [owner]. If true, that control (such as a button inside a panel) is considered to have handled the event. False for
 * [owner] itself, its template parts, or content containing no control (such as text inside a panel). True when [owner]
 * is not reached (including inside a Popup; JComboBox's popup is also a separate component, so for the mouse it counts as
 * outside [owner]).
 */
internal fun isHandledInContent(owner: WComponent, element: ComPtr): Boolean {
    val kind = classifyPart(owner, element, throughPopups = false)
    return kind == PartKind.CONTROL_IN_CONTENT || kind == PartKind.DETACHED
}

/** The result of [classifyPart]. */
private enum class PartKind { OWN, CONTENT, CONTROL_IN_CONTENT, DETACHED }

/**
 * Walks the visual tree parents from [element] up to [owner] to find where [element] lies within [owner].
 * If [throughPopups], continues from a Popup's child to the Popup, its logical tree parent ([isOwnFocusPart]).
 */
private fun classifyPart(owner: WComponent, element: ComPtr, throughPopups: Boolean): PartKind {
    if (Xaml.sameObject(element, owner.inspectable)) return PartKind.OWN
    var current = element.queryInterface(XamlInterop.IID_IDependencyObject)
    var controlSeen = false
    try {
        repeat(MAX_PART_DEPTH) {
            if (isControl(current)) controlSeen = true
            val logicalParent = logicalParentOf(current)
            val parent = try {
                if (logicalParent != null && Xaml.sameObject(logicalParent, owner.inspectable)) {
                    return if (controlSeen) PartKind.CONTROL_IN_CONTENT else PartKind.CONTENT
                }
                (if (throughPopups) popupOrNull(logicalParent) else null) ?: Xaml.parentOf(current)
            } finally {
                logicalParent?.release()
            } ?: return PartKind.DETACHED
            current.release()
            current = parent
            if (Xaml.sameObject(current, owner.inspectable)) return PartKind.OWN
        }
        return PartKind.DETACHED
    } finally {
        current.release()
    }
}

private fun isControl(element: ComPtr): Boolean {
    val control = element.queryInterfaceOrNull(XamlInterop.IID_IControl) ?: return false
    control.release()
    return true
}

/** The logical tree parent (FrameworkElement.Parent) of [element], or null if none. The caller owns the returned reference. */
private fun logicalParentOf(element: ComPtr): ComPtr? {
    val frameworkElement = element.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement) ?: return null
    return try {
        frameworkElement.getPtrOrNull(XamlInterop.IFrameworkElement_get_Parent)
    } finally {
        frameworkElement.release()
    }
}

/** If [element] is a Popup, its IDependencyObject (owned by the caller). Null if null or not a Popup. */
private fun popupOrNull(element: ComPtr?): ComPtr? {
    val popup = element?.queryInterfaceOrNull(XamlInterop.IID_IPopup) ?: return null
    return try {
        popup.queryInterface(XamlInterop.IID_IDependencyObject)
    } finally {
        popup.release()
    }
}

/** The maximum depth for walking the visual tree (prevents infinite loops on cycles or malformed trees). */
private const val MAX_PART_DEPTH = 512
