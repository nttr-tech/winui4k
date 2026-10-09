package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.util.Locale

/**
 * Internal helper that creates elements from XAML strings and lets Kotlin manipulate their named parts.
 *
 * Custom-drawn controls (such as WRibbon) build their visuals from XAML strings (colors are referenced via
 * `{ThemeResource}`) and reflect state changes from Kotlin onto the parts. Pass-by-value structs that contain floats
 * (Size / Point / Rect) are avoided because the FFI cannot handle single precision and gets them wrong on ARM64
 * (instead of UIElement.Measure / TransformToVisual, the out parameters of DesiredSize / ActualOffset are read).
 */
internal object Xaml {
    /** Declarations of the default XAML namespace and the x: namespace. */
    const val NAMESPACES =
        "xmlns=\"http://schemas.microsoft.com/winfx/2006/xaml/presentation\" " +
            "xmlns:x=\"http://schemas.microsoft.com/winfx/2006/xaml\""

    private val readerStatics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_XamlReader, XamlInterop.IID_IXamlReaderStatics) }

    val canvasStatics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasStatics) }

    val gridStatics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Grid, XamlInterop.IID_IGridStatics) }

    val toolTipStatics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_ToolTipService, XamlInterop.IID_IToolTipServiceStatics) }

    private val automationStatics: ComPtr by lazy {
        Activation.factory(XamlInterop.CLS_AutomationProperties, XamlInterop.IID_IAutomationPropertiesStatics)
    }

    private val visualStateStatics: ComPtr by lazy {
        Activation.factory(XamlInterop.CLS_VisualStateManager, XamlInterop.IID_IVisualStateManagerStatics)
    }

    private val visualTreeStatics: ComPtr by lazy {
        Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
    }

    private val focusStatics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_FocusManager, XamlInterop.IID_IFocusManagerStatics) }

    private val keyboardStatics: ComPtr by lazy {
        Activation.factory(WindowingInterop.CLS_InputKeyboardSource, WindowingInterop.IID_IInputKeyboardSourceStatics)
    }

    /**
     * Turns [xaml] into an object with XamlReader.Load, adding xmlns to the root element if it is missing.
     * The caller owns the returned reference.
     */
    fun load(xaml: String): ComPtr = Hstring.use(withNamespaces(xaml)) { h -> readerStatics.getPtr(XamlInterop.IXamlReaderStatics_Load, h) }

    /** Adds the xmlns declarations to the start tag of the root element if they are missing. */
    fun withNamespaces(xaml: String): String {
        val trimmed = xaml.trimStart()
        val end = trimmed.indexOfAny(charArrayOf(' ', '>', '/', '\n', '\r', '\t'))
        if (!trimmed.startsWith("<") || end < 0) return xaml
        val tagEnd = trimmed.indexOf('>')
        if (tagEnd >= 0 && trimmed.substring(0, tagEnd).contains("xmlns=")) return trimmed
        return trimmed.substring(0, end) + " " + NAMESPACES + trimmed.substring(end)
    }

    /** Escapes [text] so it can be embedded in an XML attribute value or text. */
    fun escape(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val builder = StringBuilder(text.length + 8)
        for (c in text) {
            when (c) {
                '&' -> builder.append("&amp;")
                '<' -> builder.append("&lt;")
                '>' -> builder.append("&gt;")
                '"' -> builder.append("&quot;")
                '\'' -> builder.append("&apos;")
                '\n' -> builder.append("&#10;")
                '\r' -> builder.append("&#13;")
                '\t' -> builder.append("&#9;")
                else -> if (c.code < 0x20) builder.append("&#").append(c.code).append(';') else builder.append(c)
            }
        }
        return builder.toString()
    }

    /** Writes a number as a XAML attribute value (always a period as the decimal point regardless of locale, and no fractional part for integers). */
    fun num(value: Double): String = when {
        value.isNaN() -> "NaN"
        value.isInfinite() -> if (value > 0) "Infinity" else "-Infinity"
        value == Math.rint(value) && Math.abs(value) < 1e15 -> value.toLong().toString()
        else -> String.format(Locale.ROOT, "%.4f", value).trimEnd('0').trimEnd('.')
    }

    /** VisualStateManager.GoToState. [control] is any view of a Control-derived element. */
    fun goToState(control: ComPtr, state: String, useTransitions: Boolean = false): Boolean {
        val view = control.queryInterface(XamlInterop.IID_IControl)
        return try {
            Hstring.use(state) { h ->
                Ffi.backend.withScope { scope ->
                    val out = scope.allocate(1, 1)
                    visualStateStatics.call(
                        XamlInterop.IVisualStateManagerStatics_GoToState,
                        view.ptr,
                        h,
                        if (useTransitions) 1 else 0,
                        out,
                    )
                    Ffi.backend.memory.getByte(out, 0) != 0.toByte()
                }
            }
        } finally {
            view.release()
        }
    }

    /** The parent in the visual tree (VisualTreeHelper.GetParent), or null if none. The caller owns the returned reference. */
    fun parentOf(element: ComPtr): ComPtr? {
        val dependency = element.queryInterface(XamlInterop.IID_IDependencyObject)
        return try {
            visualTreeStatics.getPtrOrNull(XamlInterop.IVisualTreeHelperStatics_GetParent, dependency.ptr)
        } finally {
            dependency.release()
        }
    }

    /** The root of the visual tree (reached by following parents), or null if there is no parent. The caller owns the returned reference. */
    fun visualRoot(element: ComPtr): ComPtr? {
        var current = parentOf(element) ?: return null
        while (true) {
            val parent = parentOf(current) ?: return current
            current.release()
            current = parent
        }
    }

    /** The number of children in the visual tree (VisualTreeHelper.GetChildrenCount). */
    fun childCount(element: ComPtr): Int {
        val dependency = element.queryInterface(XamlInterop.IID_IDependencyObject)
        return try {
            Ffi.backend.withScope { scope ->
                val out = scope.allocate(4)
                visualTreeStatics.call(XamlInterop.IVisualTreeHelperStatics_GetChildrenCount, dependency.ptr, out)
                Ffi.backend.memory.getInt(out, 0)
            }
        } finally {
            dependency.release()
        }
    }

    /** The [index]-th child in the visual tree (VisualTreeHelper.GetChild). The caller owns the returned reference. */
    fun childAt(element: ComPtr, index: Int): ComPtr? {
        val dependency = element.queryInterface(XamlInterop.IID_IDependencyObject)
        return try {
            visualTreeStatics.getPtrOrNull(XamlInterop.IVisualTreeHelperStatics_GetChild, dependency.ptr, index)
        } finally {
            dependency.release()
        }
    }

    /**
     * Detaches [component] from its current parent (a Panel's children, a Border's child, or a ContentControl's
     * content), since an element can have only one parent; call this before moving it elsewhere. Does nothing if it
     * has no parent.
     */
    fun detach(component: WComponent) {
        val parent = parentOf(component.uiElement) ?: return
        try {
            val panel = parent.queryInterfaceOrNull(XamlInterop.IID_IPanel)
            if (panel != null) {
                val children = panel.getPtr(XamlInterop.IPanel_get_Children)
                Ffi.backend.withScope { scope ->
                    val memory = Ffi.backend.memory
                    val index = scope.allocate(4)
                    val found = scope.allocate(1)
                    children.call(FoundationInterop.IVector_IndexOf, component.uiElement.ptr, index, found)
                    if (memory.getByte(found, 0) != 0.toByte()) children.call(FoundationInterop.IVector_RemoveAt, memory.getInt(index, 0))
                }
                children.release()
                panel.release()
                return
            }
            parent.queryInterfaceOrNull(XamlInterop.IID_IBorder)?.let { border ->
                border.call(XamlInterop.IBorder_put_Child, null)
                border.release()
                return
            }
            // The content of a ContentControl sits in the visual tree as a child of a ContentPresenter
            parent.queryInterfaceOrNull(XamlInterop.IID_IContentPresenter)?.let { presenter ->
                presenter.call(XamlInterop.IContentPresenter_put_Content, null)
                presenter.release()
            }
        } finally {
            parent.release()
        }
    }

    /** The element that has focus in [xamlRoot] (FocusManager.GetFocusedElement). The caller owns the returned reference. */
    fun focusedElement(xamlRoot: ComPtr): ComPtr? = focusStatics.getPtrOrNull(XamlInterop.IFocusManagerStatics_GetFocusedElement, xamlRoot.ptr)

    /** The first focusable element within [scope] (FocusManager.FindFirstFocusableElement). */
    fun firstFocusable(scope: ComPtr): ComPtr? {
        val dependency = scope.queryInterface(XamlInterop.IID_IDependencyObject)
        return try {
            focusStatics.getPtrOrNull(XamlInterop.IFocusManagerStatics_FindFirstFocusableElement, dependency.ptr)
        } finally {
            dependency.release()
        }
    }

    /** Moves focus to the next (true) / previous (false) element (FocusManager.TryMoveFocus). */
    fun moveFocus(next: Boolean): Boolean = Ffi.backend.withScope { scope ->
        val out = scope.allocate(1, 1)
        focusStatics.call(
            XamlInterop.IFocusManagerStatics_TryMoveFocus,
            if (next) XamlInterop.FocusNavigationDirection_Next else XamlInterop.FocusNavigationDirection_Previous,
            out,
        )
        Ffi.backend.memory.getByte(out, 0) != 0.toByte()
    }

    /** Whether [virtualKey] is pressed (InputKeyboardSource.GetKeyStateForCurrentThread). */
    fun isKeyDown(virtualKey: Int): Boolean = Ffi.backend.withScope { scope ->
        val out = scope.allocate(4)
        keyboardStatics.call(WindowingInterop.IInputKeyboardSourceStatics_GetKeyStateForCurrentThread, virtualKey, out)
        Ffi.backend.memory.getInt(out, 0) and WindowingInterop.CoreVirtualKeyStates_Down != 0
    }

    /** The modifier keys currently pressed (Ctrl=1, Alt=2, Shift=4, Win=8; the same bits as VirtualKeyModifiers). */
    fun currentModifiers(): Int {
        var modifiers = 0
        if (isKeyDown(VK_CONTROL)) modifiers = modifiers or MODIFIER_CONTROL
        if (isKeyDown(VK_MENU)) modifiers = modifiers or MODIFIER_MENU
        if (isKeyDown(VK_SHIFT)) modifiers = modifiers or MODIFIER_SHIFT
        if (isKeyDown(VK_LWIN) || isKeyDown(VK_RWIN)) modifiers = modifiers or MODIFIER_WINDOWS
        return modifiers
    }

    /** Sets a UI Automation name, id, help text, etc. (null clears it with an empty string). */
    fun setAutomation(target: ComPtr, slot: Int, value: String?) {
        val dependency = target.queryInterface(XamlInterop.IID_IDependencyObject)
        try {
            Hstring.use(value.orEmpty()) { h -> automationStatics.call(slot, dependency.ptr, h) }
        } finally {
            dependency.release()
        }
    }

    /**
     * Raises a notification for screen readers from the automation peer of [target] (AutomationPeer.RaiseNotificationEvent).
     * Narrator and similar tools read [text] aloud. Does nothing in environments that do not support notifications.
     */
    fun announce(target: ComPtr, text: String, activityId: String) {
        val ui = target.queryInterfaceOrNull(XamlInterop.IID_IUIElement) ?: return
        try {
            val statics = Activation.factory(XamlInterop.CLS_FrameworkElementAutomationPeer, XamlInterop.IID_IFrameworkElementAutomationPeerStatics)
            val peer = try {
                statics.getPtrOrNull(XamlInterop.IFrameworkElementAutomationPeerStatics_CreatePeerForElement, ui.ptr)
            } finally {
                statics.release()
            } ?: return
            try {
                raiseNotification(peer, text, activityId)
            } finally {
                peer.release()
            }
        } catch (e: com.appkitbox.winui4k.internal.com.WindowsRuntimeException) {
            // Notifications for screen readers are only an aid, so give up in environments that do not support them
            System.err.println("ribbon: automation notification is not available: ${e.message}")
        } finally {
            ui.release()
        }
    }

    private fun raiseNotification(peer: ComPtr, text: String, activityId: String) = Hstring.use(text) { t ->
        Hstring.use(activityId) { a ->
            peer.call(
                XamlInterop.IAutomationPeer_RaiseNotificationEvent,
                XamlInterop.AutomationNotificationKind_ItemAdded,
                XamlInterop.AutomationNotificationProcessing_MostRecent,
                t,
                a,
            )
        }
    }

    /** Reads a UI Automation name, id, help text, etc. ([slot] is a GetXxx of AutomationProperties). */
    fun getAutomation(target: ComPtr, slot: Int): String {
        val dependency = target.queryInterface(XamlInterop.IID_IDependencyObject)
        return try {
            automationStatics.getString(slot, dependency.ptr)
        } finally {
            dependency.release()
        }
    }

    /** Sets ToolTipService.ToolTip ([tip] is a string, an element, or null). */
    fun setToolTip(target: ComPtr, tip: Any?) {
        val dependency = target.queryInterface(XamlInterop.IID_IDependencyObject)
        try {
            when (tip) {
                null -> toolTipStatics.call(XamlInterop.IToolTipServiceStatics_SetToolTip, dependency.ptr, null)
                is String -> {
                    val boxed = PropertyValues.boxString(tip)
                    toolTipStatics.call(XamlInterop.IToolTipServiceStatics_SetToolTip, dependency.ptr, boxed.ptr)
                    boxed.release()
                }
                is WComponent -> toolTipStatics.call(XamlInterop.IToolTipServiceStatics_SetToolTip, dependency.ptr, tip.inspectable.ptr)
                is ComPtr -> toolTipStatics.call(XamlInterop.IToolTipServiceStatics_SetToolTip, dependency.ptr, tip.ptr)
                else -> error("Value cannot be set as a ToolTip: $tip")
            }
        } finally {
            dependency.release()
        }
    }

    /** Creates a solid color brush. The caller owns the returned reference. */
    fun solidBrush(a: Int, r: Int, g: Int, b: Int): ComPtr {
        val brush = Activation.activate(XamlInterop.CLS_SolidColorBrush, XamlInterop.IID_ISolidColorBrush)
        XamlStructs.putColor(brush, XamlInterop.ISolidColorBrush_put_Color, a, r, g, b)
        return brush
    }

    /** Calls [slot] of [target] with (..., out Point) and returns [x, y] (Point is an out of two r4 values). */
    fun readPoint(target: ComPtr, slot: Int, vararg args: Any?): DoubleArray = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        target.call(slot, *args, out)
        val memory = Ffi.backend.memory
        doubleArrayOf(Float.fromBits(memory.getInt(out, 0)).toDouble(), Float.fromBits(memory.getInt(out, 4)).toDouble())
    }

    /** Calls [slot] of [target] with (out T) and returns the first [count] r4 values (an out of Size / Vector2 / Vector3). */
    fun readFloats(target: ComPtr, slot: Int, count: Int): DoubleArray = Ffi.backend.withScope { scope ->
        val out = scope.allocate(count * 4L)
        target.call(slot, out)
        val memory = Ffi.backend.memory
        DoubleArray(count) { Float.fromBits(memory.getInt(out, it * 4L)).toDouble() }
    }

    /** The size [width, height] of the window (XamlRoot) that [component] is in, or null if it is not in a tree. */
    fun rootSize(component: WComponent): DoubleArray? {
        val root = component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
        return try {
            readFloats(root, XamlInterop.IXamlRoot_get_Size, 2)
        } finally {
            root.release()
        }
    }

    /** The top-left position of the element relative to the root of its visual tree (the window content or the popup child). */
    fun positionInRoot(element: ComPtr): DoubleArray {
        var x = 0.0
        var y = 0.0
        var current: ComPtr? = element.queryInterface(XamlInterop.IID_IUIElement)
        var guard = 0
        while (current != null && guard++ < MAX_TREE_DEPTH) {
            val offset = readFloats(current, XamlInterop.IUIElement_get_ActualOffset, 3)
            x += offset[0]
            y += offset[1]
            val parent = parentOf(current)
            current.release()
            current = parent?.let { p ->
                val ui = p.queryInterfaceOrNull(XamlInterop.IID_IUIElement)
                p.release()
                ui
            }
        }
        current?.release()
        return doubleArrayOf(x, y)
    }

    /** RoutedEventArgs.OriginalSource. The caller owns the returned reference. */
    fun originalSource(args: Ptr): ComPtr? {
        val routed = ComPtr(args).queryInterfaceOrNull(XamlInterop.IID_IRoutedEventArgs) ?: return null
        return try {
            routed.getPtrOrNull(XamlInterop.IRoutedEventArgs_get_OriginalSource)
        } finally {
            routed.release()
        }
    }

    /** Whether two COM references point to the same object (IUnknown identity). */
    fun sameObject(first: ComPtr, second: ComPtr): Boolean {
        val a = first.queryInterface(IID_IUNKNOWN)
        val b = second.queryInterface(IID_IUNKNOWN)
        return try {
            a.ptr == b.ptr
        } finally {
            a.release()
            b.release()
        }
    }

    private const val IID_IUNKNOWN = "00000000-0000-0000-c000-000000000046"
    private const val MAX_TREE_DEPTH = 512

    // Windows.System.VirtualKey (same values as VirtualKey.kt)
    private const val VK_SHIFT = 16
    private const val VK_CONTROL = 17
    private const val VK_MENU = 18
    private const val VK_LWIN = 91
    private const val VK_RWIN = 92

    /** Bits of Windows.System.VirtualKeyModifiers. */
    const val MODIFIER_CONTROL = 1
    const val MODIFIER_MENU = 2
    const val MODIFIER_SHIFT = 4
    const val MODIFIER_WINDOWS = 8
}

/** Arguments of KeyDown / KeyUp / PreviewKeyDown (valid only during the callback). */
internal class XamlKeyEvent(private val args: ComPtr) {
    /** The pressed key (a Windows.System.VirtualKey value). */
    val key: Int get() = args.getInt(XamlInterop.IKeyRoutedEventArgs_get_Key)

    /** The original key before modification (a Windows.System.VirtualKey value). */
    val originalKey: Int get() = args.getInt(XamlInterop.IKeyRoutedEventArgs_get_OriginalKey)

    /** Whether the event has been handled. Setting it to true stops it from reaching subsequent elements. */
    var handled: Boolean
        get() = args.getBool(XamlInterop.IKeyRoutedEventArgs_get_Handled)
        set(value) = args.putBool(XamlInterop.IKeyRoutedEventArgs_put_Handled, value)
}

/** Arguments of pointer events (valid only during the callback). */
internal class XamlPointerEvent(private val args: ComPtr) {
    /** Whether the event has been handled. */
    var handled: Boolean
        get() = args.getBool(XamlInterop.IPointerRoutedEventArgs_get_Handled)
        set(value) = args.putBool(XamlInterop.IPointerRoutedEventArgs_put_Handled, value)

    /** The modifier keys that were pressed (VirtualKeyModifiers). */
    val modifiers: Int get() = args.getInt(XamlInterop.IPointerRoutedEventArgs_get_KeyModifiers)

    /** Whether the event originated from a button (ButtonBase) (pressing a button on a drag handle does not start a drag). */
    val isFromButton: Boolean
        get() {
            val source = Xaml.originalSource(args.ptr) ?: return false
            return try {
                val button = source.queryInterfaceOrNull(XamlInterop.IID_IButtonBase)
                button?.release()
                button != null
            } finally {
                source.release()
            }
        }

    /** The pointer position [x, y] relative to [relativeTo] (the window content if null). */
    fun position(relativeTo: WComponent?): DoubleArray = withPoint(relativeTo) { point ->
        Xaml.readPoint(point, WindowingInterop.IPointerPoint_get_Position)
    }

    /** Whether the left button is pressed. */
    val isLeftButtonPressed: Boolean get() = withProperties { it.getBool(WindowingInterop.IPointerPointProperties_get_IsLeftButtonPressed) }

    /** Whether the right button is pressed. */
    val isRightButtonPressed: Boolean get() = withProperties { it.getBool(WindowingInterop.IPointerPointProperties_get_IsRightButtonPressed) }

    /** The wheel rotation amount (positive = away / right). */
    val wheelDelta: Int get() = withProperties { it.getInt(WindowingInterop.IPointerPointProperties_get_MouseWheelDelta) }

    /** Whether this is a horizontal wheel. */
    val isHorizontalWheel: Boolean get() = withProperties { it.getBool(WindowingInterop.IPointerPointProperties_get_IsHorizontalMouseWheel) }

    /** Captures this pointer to [element] (it keeps receiving move events while dragging). */
    fun capture(element: WComponent): Boolean {
        val pointer = args.getPtr(XamlInterop.IPointerRoutedEventArgs_get_Pointer)
        return try {
            Ffi.backend.withScope { scope ->
                val out = scope.allocate(1, 1)
                element.uiElement.call(XamlInterop.IUIElement_CapturePointer, pointer.ptr, out)
                Ffi.backend.memory.getByte(out, 0) != 0.toByte()
            }
        } finally {
            pointer.release()
        }
    }

    /** Releases the pointer capture of [element]. */
    fun release(element: WComponent) {
        val pointer = args.getPtr(XamlInterop.IPointerRoutedEventArgs_get_Pointer)
        try {
            element.uiElement.call(XamlInterop.IUIElement_ReleasePointerCapture, pointer.ptr)
        } finally {
            pointer.release()
        }
    }

    private fun <T> withPoint(relativeTo: WComponent?, block: (ComPtr) -> T): T {
        val point = args.getPtr(XamlInterop.IPointerRoutedEventArgs_GetCurrentPoint, relativeTo?.uiElement?.ptr)
        return try {
            block(point)
        } finally {
            point.release()
        }
    }

    private fun <T> withProperties(block: (ComPtr) -> T): T = withPoint(null) { point ->
        val properties = point.getPtr(WindowingInterop.IPointerPoint_get_Properties)
        try {
            block(properties)
        } finally {
            properties.release()
        }
    }
}

/** Arguments of Tapped / DoubleTapped / RightTapped (valid only during the callback). */
internal class XamlTapEvent(private val args: ComPtr) {
    /** Marks the event as handled. */
    fun markHandled() = args.putBool(XamlInterop.ITappedRoutedEventArgs_put_Handled, true)

    /** The tap position [x, y] relative to [relativeTo] (the window content if null). */
    fun position(relativeTo: WComponent?): DoubleArray =
        Xaml.readPoint(args, XamlInterop.ITappedRoutedEventArgs_GetPosition, relativeTo?.uiElement?.ptr)
}

/**
 * A wrapper for any element created from a XAML string (internal use).
 * Provides named parts ([part]) and general operations for text, visibility, layout, events, and so on.
 */
internal open class XamlElement(inspectable: ComPtr) : WComponent(inspectable) {
    private val parts = HashMap<String, XamlElement>()

    /** Cache of internally used views (interface IID → QI result). */
    private val views = HashMap<String, ComPtr>()

    /** The view for [iid] (the QI result is held for the lifetime of this wrapper). */
    fun view(iid: String): ComPtr = views.getOrPut(iid) { own(inspectable.queryInterface(iid)) }

    /** Finds the part named [name] in the XAML namescope (FrameworkElement.FindName). Throws if not found. */
    fun part(name: String): XamlElement = partOrNull(name) ?: error("XAML part '$name' not found")

    /** Finds the part named [name] in the XAML namescope. Returns null if not found. */
    fun partOrNull(name: String): XamlElement? {
        parts[name]?.let { return it }
        val found = Hstring.use(name) { h -> frameworkElement.getPtrOrNull(XamlInterop.IFrameworkElement_FindName, h) } ?: return null
        val element = XamlElement(found)
        parts[name] = element
        return element
    }

    /**
     * Finds a part of the Control's template by name (applies the template, then calls FindName in the namescope of
     * the template root). Throws if not found.
     */
    fun templatePart(name: String): XamlElement {
        parts["template:$name"]?.let { return it }
        applyTemplate()
        val root = Xaml.childAt(inspectable, 0) ?: error("Template has not been applied")
        val rootElement = XamlElement(root)
        val found = rootElement.partOrNull(name) ?: error("Template part '$name' not found")
        parts["template:$name"] = found
        return found
    }

    /** Applies the Control's template (for switching visual states before the element enters the visual tree). */
    fun applyTemplate() {
        Ffi.backend.withScope { scope ->
            val out = scope.allocate(1, 1)
            view(XamlInterop.IID_IControl).call(XamlInterop.IControl_ApplyTemplate, out)
        }
    }

    /** Sets FrameworkElement.MinHeight. */
    fun setMinHeight(value: Double) = frameworkElement.call(XamlInterop.IFrameworkElement_put_MinHeight, value)

    /** Sets FrameworkElement.MinWidth. */
    fun setMinWidth(value: Double) = frameworkElement.call(XamlInterop.IFrameworkElement_put_MinWidth, value)

    /** The text of a TextBlock. */
    fun setText(value: String?) {
        Hstring.use(value.orEmpty()) { h -> view(XamlInterop.IID_ITextBlock).call(XamlInterop.ITextBlock_put_Text, h) }
    }

    /** Control.IsEnabled. */
    var isControlEnabled: Boolean
        get() = view(XamlInterop.IID_IControl).getBool(XamlInterop.IControl_get_IsEnabled)
        set(value) = view(XamlInterop.IID_IControl).putBool(XamlInterop.IControl_put_IsEnabled, value)

    /** Whether the element is a hit-test target (UIElement.IsHitTestVisible). */
    fun setHitTestVisible(value: Boolean) = uiElement.putBool(XamlInterop.IUIElement_put_IsHitTestVisible, value)

    /** Whether the Tab key stops at the element (UIElement.IsTabStop). */
    var isTabStop: Boolean
        get() = uiElement.getBool(XamlInterop.IUIElement_get_IsTabStop)
        set(value) = uiElement.putBool(XamlInterop.IUIElement_put_IsTabStop, value)

    /** Moves focus to the element. */
    fun focus(state: Int = XamlInterop.FocusState_Keyboard): Boolean = Ffi.backend.withScope { scope ->
        val out = scope.allocate(1, 1)
        uiElement.call(XamlInterop.IUIElement_Focus, state, out)
        Ffi.backend.memory.getByte(out, 0) != 0.toByte()
    }

    /** Whether the element has focus. */
    val hasFocus: Boolean get() = uiElement.getInt(XamlInterop.IUIElement_get_FocusState) != 0

    /** The position as a child of a Canvas (Canvas.Left / Canvas.Top). */
    fun setCanvasPosition(x: Double, y: Double) {
        Xaml.canvasStatics.call(XamlInterop.ICanvasStatics_SetLeft, uiElement.ptr, x)
        Xaml.canvasStatics.call(XamlInterop.ICanvasStatics_SetTop, uiElement.ptr, y)
    }

    /** The explicit size (Width / Height; NaN clears it). */
    fun setSize(width: Double, height: Double) = setLayoutSize(width, height)

    /** The row and column in a Grid (Grid.Row / Grid.Column). */
    fun setGridCell(row: Int, column: Int) {
        Xaml.gridStatics.call(XamlInterop.IGridStatics_SetRow, frameworkElement.ptr, row)
        Xaml.gridStatics.call(XamlInterop.IGridStatics_SetColumn, frameworkElement.ptr, column)
    }

    /** The desired size [width, height] from the most recent Measure. */
    fun desiredSize(): DoubleArray = XamlStructs.getSizeFloat(uiElement, XamlInterop.IUIElement_get_DesiredSize)

    /** The position [x, y] relative to the parent (UIElement.ActualOffset). */
    fun actualOffset(): DoubleArray = Xaml.readFloats(uiElement, XamlInterop.IUIElement_get_ActualOffset, 2)

    /** The top-left position [x, y] relative to the root of the visual tree. */
    fun positionInRoot(): DoubleArray = Xaml.positionInRoot(uiElement)

    /** The font size of a Control / ContentPresenter / TextBlock. */
    fun setFontSize(size: Double) = view(XamlInterop.IID_IControl).call(XamlInterop.IControl_put_FontSize, size)

    /** VisualStateManager.GoToState (Control only). */
    fun goToState(state: String, useTransitions: Boolean = false): Boolean = Xaml.goToState(inspectable, state, useTransitions)

    /** The UI Automation name. */
    fun setAutomationName(name: String?) = Xaml.setAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_SetName, name)

    /** The UI Automation id. */
    fun setAutomationId(id: String?) = Xaml.setAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_SetAutomationId, id)

    /** The UI Automation help text. */
    fun setAutomationHelpText(text: String?) = Xaml.setAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_SetHelpText, text)

    /** The UI Automation name, id, help text, and access key (an empty string if not set). */
    val automationName: String get() = Xaml.getAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_GetName)
    val automationId: String get() = Xaml.getAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_GetAutomationId)
    val automationHelpText: String get() = Xaml.getAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_GetHelpText)
    val automationAccessKey: String get() = Xaml.getAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_GetAccessKey)

    /** The UI Automation access key (the KeyTip; read aloud by Narrator). */
    fun setAutomationAccessKey(key: String?) = Xaml.setAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_SetAccessKey, key)

    /** The UI Automation accelerator key. */
    fun setAutomationAcceleratorKey(text: String?) =
        Xaml.setAutomation(inspectable, XamlInterop.IAutomationPropertiesStatics_SetAcceleratorKey, text)

    /** The tooltip (a string, an element, or null). */
    fun setToolTipValue(tip: Any?) = Xaml.setToolTip(inspectable, tip)

    /** Puts an element into ContentControl.Content (null removes it). */
    fun setContent(content: WComponent?) {
        view(XamlInterop.IID_IContentControl).call(XamlInterop.IContentControl_put_Content, content?.inspectable?.ptr)
    }

    /** Puts a string into ContentControl.Content. */
    fun setContentText(text: String) {
        val boxed = PropertyValues.boxString(text)
        try {
            view(XamlInterop.IID_IContentControl).call(XamlInterop.IContentControl_put_Content, boxed.ptr)
        } finally {
            boxed.release()
        }
    }

    /** Puts an element into Border.Child (null removes it). */
    fun setChild(child: WComponent?) {
        view(XamlInterop.IID_IBorder).call(XamlInterop.IBorder_put_Child, child?.uiElement?.ptr)
    }

    private val children: ComPtr by lazy { own(view(XamlInterop.IID_IPanel).getPtr(XamlInterop.IPanel_get_Children)) }

    /** Appends a child to the end of the Panel. */
    fun addChild(child: WComponent) = children.call(FoundationInterop.IVector_Append, child.uiElement.ptr)

    /** Inserts a child into the Panel at [index]. */
    fun insertChild(index: Int, child: WComponent) = children.call(FoundationInterop.IVector_InsertAt, index, child.uiElement.ptr)

    /** Removes a child from the Panel (does nothing if it is not contained). */
    fun removeChild(child: WComponent) {
        Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val index = scope.allocate(4)
            val found = scope.allocate(1)
            children.call(FoundationInterop.IVector_IndexOf, child.uiElement.ptr, index, found)
            if (memory.getByte(found, 0) != 0.toByte()) children.call(FoundationInterop.IVector_RemoveAt, memory.getInt(index, 0))
        }
    }

    /** Removes all children from the Panel. */
    fun clearChildren() = children.call(FoundationInterop.IVector_Clear)

    /** The number of children of the Panel. */
    val childCount: Int get() = Ffi.backend.withScope { scope ->
        val out = scope.allocate(4)
        children.call(FoundationInterop.IVector_get_Size, out)
        Ffi.backend.memory.getInt(out, 0)
    }

    /** Subscribes to ButtonBase.Click. */
    fun onClick(handler: () -> Unit): Long = view(XamlInterop.IID_IButtonBase).addEventHandler(
        "WinUI4K.XamlClick",
        XamlInterop.IID_RoutedEventHandler,
        XamlInterop.IButtonBase_add_Click,
    ) { _, _ -> handler() }

    /** Subscribes to Checked / Unchecked / Indeterminate of a ToggleButton (the argument is the new state). */
    fun onToggled(handler: (Boolean?) -> Unit) {
        val toggle = view(XamlInterop.IID_IToggleButton)
        val slots = intArrayOf(XamlInterop.IToggleButton_add_Checked, XamlInterop.IToggleButton_add_Unchecked, XamlInterop.IToggleButton_add_Indeterminate)
        for (slot in slots) {
            toggle.addEventHandler("WinUI4K.XamlToggled", XamlInterop.IID_RoutedEventHandler, slot) { _, _ -> handler(toggleChecked) }
        }
    }

    /** ToggleButton.IsChecked (null is the indeterminate state). */
    var toggleChecked: Boolean?
        get() {
            val boxed = view(XamlInterop.IID_IToggleButton).getPtrOrNull(XamlInterop.IToggleButton_get_IsChecked) ?: return null
            return try {
                PropertyValues.unboxBool(boxed)
            } finally {
                boxed.release()
            }
        }
        set(value) {
            if (value == null) {
                view(XamlInterop.IID_IToggleButton).call(XamlInterop.IToggleButton_put_IsChecked, null)
                return
            }
            val boxed = PropertyValues.boxBool(value)
            val reference = boxed.queryInterface(FoundationInterop.IID_IReference_Boolean)
            try {
                view(XamlInterop.IID_IToggleButton).call(XamlInterop.IToggleButton_put_IsChecked, reference.ptr)
            } finally {
                reference.release()
                boxed.release()
            }
        }

    /** TextBox.Text. */
    var textBoxText: String
        get() = view(XamlInterop.IID_ITextBox).getString(XamlInterop.ITextBox_get_Text)
        set(value) {
            Hstring.use(value) { h -> view(XamlInterop.IID_ITextBox).call(XamlInterop.ITextBox_put_Text, h) }
        }

    /** Sets TextBox.PlaceholderText. */
    fun setPlaceholderText(text: String?) {
        Hstring.use(text.orEmpty()) { h -> view(XamlInterop.IID_ITextBox).call(XamlInterop.ITextBox_put_PlaceholderText, h) }
    }

    /** Selects all text in the TextBox. */
    fun selectAllText() = view(XamlInterop.IID_ITextBox).call(XamlInterop.ITextBox_SelectAll)

    /** Subscribes to TextBox.TextChanged. */
    fun onTextChanged(handler: () -> Unit): Long = view(XamlInterop.IID_ITextBox).addEventHandler(
        "WinUI4K.XamlTextChanged",
        XamlInterop.IID_TextChangedEventHandler,
        XamlInterop.ITextBox_add_TextChanged,
    ) { _, _ -> handler() }

    /** RangeBase.Value. */
    var rangeValue: Double
        get() = view(XamlInterop.IID_IRangeBase).getDouble(XamlInterop.IRangeBase_get_Value)
        set(value) = view(XamlInterop.IID_IRangeBase).call(XamlInterop.IRangeBase_put_Value, value)

    /** Sets the minimum and maximum of the RangeBase. */
    fun setRange(minimum: Double, maximum: Double) {
        val range = view(XamlInterop.IID_IRangeBase)
        range.call(XamlInterop.IRangeBase_put_Minimum, minimum)
        range.call(XamlInterop.IRangeBase_put_Maximum, maximum)
    }

    /** Subscribes to RangeBase.ValueChanged (the argument is the new value). */
    fun onValueChanged(handler: (Double) -> Unit): Long = view(XamlInterop.IID_IRangeBase).addEventHandler(
        "WinUI4K.XamlValueChanged",
        XamlInterop.IID_RangeBaseValueChangedEventHandler,
        XamlInterop.IRangeBase_add_ValueChanged,
    ) { _, args -> handler(ComPtr(args).getDouble(XamlInterop.IRangeBaseValueChangedEventArgs_get_NewValue)) }

    /** Subscribes to KeyDown of the UIElement. */
    fun onKeyDown(handler: (XamlKeyEvent) -> Unit): Long = addKey(XamlInterop.IUIElement_add_KeyDown, handler)

    /** Subscribes to KeyUp of the UIElement. */
    fun onKeyUp(handler: (XamlKeyEvent) -> Unit): Long = addKey(XamlInterop.IUIElement_add_KeyUp, handler)

    /** Subscribes to PreviewKeyDown of the UIElement (delivered to parents first). */
    fun onPreviewKeyDown(handler: (XamlKeyEvent) -> Unit): Long = addKey(XamlInterop.IUIElement_add_PreviewKeyDown, handler)

    /** Subscribes to PreviewKeyUp of the UIElement. */
    fun onPreviewKeyUp(handler: (XamlKeyEvent) -> Unit): Long = addKey(XamlInterop.IUIElement_add_PreviewKeyUp, handler)

    private fun addKey(slot: Int, handler: (XamlKeyEvent) -> Unit): Long =
        uiElement.addEventHandler("WinUI4K.XamlKey", XamlInterop.IID_KeyEventHandler, slot) { _, args -> handler(XamlKeyEvent(ComPtr(args))) }

    /** Subscribes to a pointer event ([addSlot] is one of IUIElement_add_Pointer*). */
    fun onPointer(addSlot: Int, handler: (XamlPointerEvent) -> Unit): Long =
        uiElement.addEventHandler("WinUI4K.XamlPointer", XamlInterop.IID_PointerEventHandler, addSlot) { _, args ->
            handler(XamlPointerEvent(ComPtr(args)))
        }

    /**
     * Subscribes to a routed pointer event ([eventSlot] is a get_XxxEvent of IUIElementStatics), including events that
     * a child has marked as handled (UIElement.AddHandler(XxxEvent, handler, handledEventsToo = true)).
     * This also receives clicks on buttons. Calling the returned function unsubscribes.
     * Since AddHandler takes the handler as an object, the delegate is wrapped in an IReference<PointerEventHandler>
     * (the same as box_value in C++/WinRT).
     */
    fun onPointerHandledToo(eventSlot: Int, handler: (XamlPointerEvent) -> Unit): () -> Unit {
        val statics = Activation.factory(XamlInterop.CLS_UIElement, XamlInterop.IID_IUIElementStatics)
        val routedEvent = try {
            statics.getPtr(eventSlot)
        } finally {
            statics.release()
        }
        val delegate = KComObject("WinUI4K.XamlPointerHandledToo", inspectable = false).addInterface(
            XamlInterop.IID_PointerEventHandler,
            listOf(
                // Invoke(this, sender, args)
                KComObject.Method(CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)) { args ->
                    handler(XamlPointerEvent(ComPtr(args[2] as Ptr)))
                    KComObject.S_OK
                },
            ),
        )
        val boxed = KComObject("Windows.Foundation.IReference`1<Microsoft.UI.Xaml.Input.PointerEventHandler>").addInterface(
            FoundationInterop.IID_IReference_PointerEventHandler,
            listOf(
                // get_Value(this, out PointerEventHandler): hands over a reference that the caller releases
                KComObject.Method(CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)) { args ->
                    delegate.addRef()
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, delegate.primary)
                    KComObject.S_OK
                },
            ),
        )
        uiElement.call(XamlInterop.IUIElement_AddHandler, routedEvent, boxed.primary, 1)
        var removed = false
        return {
            if (!removed) {
                removed = true
                uiElement.call(XamlInterop.IUIElement_RemoveHandler, routedEvent, boxed.primary)
                routedEvent.release()
                boxed.release()
                delegate.release()
            }
        }
    }

    /** Subscribes to Tapped. */
    fun onTapped(handler: (XamlTapEvent) -> Unit): Long =
        uiElement.addEventHandler("WinUI4K.XamlTapped", XamlInterop.IID_TappedEventHandler, XamlInterop.IUIElement_add_Tapped) { _, args ->
            handler(XamlTapEvent(ComPtr(args)))
        }

    /** Subscribes to DoubleTapped. */
    fun onDoubleTapped(handler: (XamlTapEvent) -> Unit): Long = uiElement.addEventHandler(
        "WinUI4K.XamlDoubleTapped",
        XamlInterop.IID_DoubleTappedEventHandler,
        XamlInterop.IUIElement_add_DoubleTapped,
    ) { _, args -> handler(XamlTapEvent(ComPtr(args))) }

    /** Subscribes to RightTapped. */
    fun onRightTapped(handler: (XamlTapEvent) -> Unit): Long = uiElement.addEventHandler(
        "WinUI4K.XamlRightTapped",
        XamlInterop.IID_RightTappedEventHandler,
        XamlInterop.IUIElement_add_RightTapped,
    ) { _, args -> handler(XamlTapEvent(ComPtr(args))) }

    /** Subscribes to GotFocus / LostFocus (GotFocus if [gotFocus] is true). */
    fun onFocus(gotFocus: Boolean, handler: () -> Unit): Long = uiElement.addEventHandler(
        "WinUI4K.XamlFocus",
        XamlInterop.IID_RoutedEventHandler,
        if (gotFocus) XamlInterop.IUIElement_add_GotFocus else XamlInterop.IUIElement_add_LostFocus,
    ) { _, _ -> handler() }

    /** Subscribes to Unloaded. */
    fun onUnloaded(handler: () -> Unit): Long = frameworkElement.addEventHandler(
        "WinUI4K.XamlUnloaded",
        XamlInterop.IID_RoutedEventHandler,
        XamlInterop.IFrameworkElement_add_Unloaded,
    ) { _, _ -> handler() }

    /** Subscribes to Control.IsEnabledChanged. */
    fun onEnabledChanged(handler: () -> Unit): Long = view(XamlInterop.IID_IControl).addEventHandler(
        "WinUI4K.XamlEnabledChanged",
        XamlInterop.IID_DependencyPropertyChangedEventHandler,
        XamlInterop.IControl_add_IsEnabledChanged,
    ) { _, _ -> handler() }

    /** Unsubscribes using a token obtained from [onKeyDown] and similar ([removeSlot] is the corresponding remove_*). */
    fun removeUiHandler(removeSlot: Int, token: Long) = uiElement.removeEventHandler(removeSlot, token)

    companion object {
        /** Creates an element from a XAML string. */
        fun load(xaml: String): XamlElement = XamlElement(Xaml.load(xaml))

        /** Wraps an existing reference [ptr] (takes over a reference owned by the caller). */
        fun adopt(ptr: ComPtr): XamlElement = XamlElement(ptr)
    }
}
