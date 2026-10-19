package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.win32.Win32
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.math.abs

/** Kind of pointer event ([PointerSample.kind]). */
internal enum class PointerKind { PRESSED, RELEASED, MOVED, ENTERED, EXITED, WHEEL, CAPTURE_LOST }

/**
 * Values read from a native pointer event (PointerRoutedEventArgs). Passed to [MouseInput.dispatch].
 * Tests cannot produce native input (doc/verification.md), so they create these directly and pass them in.
 */
@Suppress("LongParameterList") // The parameters map 1:1 to the values read from the native pointer event
internal class PointerSample(
    val kind: PointerKind,
    /** Position relative to the component's top-left corner (DIP). */
    val x: Double,
    val y: Double,
    /** The button whose state changed (WMouseEvent.BUTTON1 etc.; NOBUTTON if none changed). */
    val changedButton: Int = WMouseEvent.NOBUTTON,
    /** Buttons held down (a combination of WInputEvent.BUTTON1_DOWN_MASK etc.). */
    val buttonsDown: Int = 0,
    /** Modifier keys that were held down (a combination of WInputEvent.SHIFT_DOWN_MASK etc.). */
    val keyModifiersEx: Int = 0,
    /** Time of the input (milliseconds; PointerPoint.Timestamp). Used to count consecutive presses. */
    val timestampMillis: Long = 0,
    /** Wheel rotation (PointerPointProperties.MouseWheelDelta; positive is away from the user / right, 120 per notch). */
    val wheelDelta: Int = 0,
    val isHorizontalWheel: Boolean = false,
    /** Whether the event was already handled (Handled) when it arrived here. */
    val handled: Boolean = false,
    /** Whether the control that handled it is this component's content (child) (called only when handled). */
    val handledByContent: () -> Boolean = { false },
    /** Identifier of this event, which stays the same while it bubbles up to the parents ([PointerRouting]). */
    val routingKey: Any = Any(),
    /** Physical pixels per DIP (XamlRoot.RasterizationScale). Used to compare against system metrics (physical pixels). */
    val pixelsPerDip: Double = 1.0,
)

/** Operations on the element the event reached (pointer capture and Handled). Tests pass one that only records calls. */
internal interface PointerTarget {
    /** Captures this pointer to the component (UIElement.CapturePointer). True if successful. */
    fun capture(): Boolean

    /** Releases the capture taken by [capture] (UIElement.ReleasePointerCapture). */
    fun releaseCapture()

    /** Marks the native event as handled (Handled = true). */
    fun markHandled()
}

/**
 * Marker that lets only the deepest component with listeners receive a pointer event bubbling up to the parents
 * (used only on the UI thread).
 * As in Swing, if both a child and its parent have mouse listeners, only the child receives the event. Events bubble
 * from child to parent one at a time, and the next one comes only after that finishes, so remembering the identifier
 * of the last received event is enough.
 */
internal object PointerRouting {
    private var claimed: Any? = null

    fun isClaimed(key: Any): Boolean = claimed == key

    fun claim(key: Any) {
        claimed = key
    }
}

/** Identifier of a native event (kind, pointer, input frame, time). It stays the same while bubbling up to the parents. */
private data class NativePointerKey(val kind: PointerKind, val pointerId: Int, val frameId: Int, val timestamp: Long)

/**
 * State that builds a component's mouse listeners (MouseListener / MouseMotionListener / MouseWheelListener-style)
 * and the Swing MouseEvent contract (pressed, released, clicked, dragged, entered/exited, wheel) from WinUI pointer events.
 *
 * - The component that was pressed captures the pointer, and release and drag reach it even outside the component
 *   (for a press the control itself already handled (e.g. a button), the control holds the capture, so it is not
 *   captured again)
 * - A click is when the button is released inside the pressed component without having moved beyond Windows' drag
 *   range (SM_CXDRAG / SM_CYDRAG) since the press. Consecutive presses are counted with Windows' double-click time
 *   and range
 * - If both a child and its parent have listeners, only the child receives the event, and a press handled by a child
 *   control (e.g. a button in a panel) is not reported to the parent
 * - Enter/exit come from PointerEntered / PointerExited, which WinUI sends per element, once each time the pointer
 *   crosses this component's bounds
 */
internal class MouseInput(private val owner: WComponent) {
    private val mouseListeners = ArrayList<WMouseListener>()
    private val motionListeners = ArrayList<WMouseMotionListener>()
    private val wheelListeners = ArrayList<WMouseWheelListener>()
    private var handlersAdded = false

    /** Buttons held down on this component (BUTTONn_DOWN_MASK). 0 if none. */
    private var pressedButtons = 0

    /** Whether the pointer was captured on press. */
    private var captured = false

    /** Position, time, button and count of the last press (to count consecutive presses). */
    private var lastPressX = 0.0
    private var lastPressY = 0.0
    private var lastPressTime = Long.MIN_VALUE
    private var lastPressButton = WMouseEvent.NOBUTTON
    private var clickCount = 0

    /** Whether the pointer has not moved beyond the drag range since the press (if it has, it is not a click). */
    private var clickAllowed = false

    /** Whether the pointer is over this component (to report enter/exit once each). */
    private var inside = false

    fun addMouseListener(listener: WMouseListener) = add(mouseListeners, listener)

    fun removeMouseListener(listener: WMouseListener) = remove(mouseListeners, listener)

    fun mouseListeners(): Array<WMouseListener> = mouseListeners.toTypedArray()

    fun addMouseMotionListener(listener: WMouseMotionListener) = add(motionListeners, listener)

    fun removeMouseMotionListener(listener: WMouseMotionListener) = remove(motionListeners, listener)

    fun mouseMotionListeners(): Array<WMouseMotionListener> = motionListeners.toTypedArray()

    fun addMouseWheelListener(listener: WMouseWheelListener) = add(wheelListeners, listener)

    fun removeMouseWheelListener(listener: WMouseWheelListener) = remove(wheelListeners, listener)

    fun mouseWheelListeners(): Array<WMouseWheelListener> = wheelListeners.toTypedArray()

    private fun <L> add(listeners: MutableList<L>, listener: L) {
        if (!handlersAdded) {
            addHandlers()
            handlersAdded = true
        }
        listeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    private fun <L> remove(listeners: MutableList<L>, listener: L) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    /** Whether this can be a target of press, release, drag and move (as in Swing, if it has a MouseListener or MouseMotionListener). */
    private val hasButtonListeners: Boolean get() = mouseListeners.isNotEmpty() || motionListeners.isNotEmpty()

    /** Delivers [sample] to this component's listeners (does nothing if this is not the target). */
    fun dispatch(sample: PointerSample, target: PointerTarget) {
        when (sample.kind) {
            PointerKind.PRESSED -> onPressed(sample, target)
            PointerKind.RELEASED -> onReleased(sample, target)
            PointerKind.MOVED -> onMoved(sample, target)
            PointerKind.ENTERED -> onEntered(sample, target)
            PointerKind.EXITED -> onExited(sample, target)
            PointerKind.WHEEL -> onWheel(sample, target)
            PointerKind.CAPTURE_LOST -> onCaptureLost()
        }
    }

    /** Whether a child component with listeners received the event, or a child control handled it. */
    private fun isForOthers(sample: PointerSample): Boolean =
        PointerRouting.isClaimed(sample.routingKey) || (sample.handled && sample.handledByContent())

    private fun onPressed(sample: PointerSample, target: PointerTarget) {
        val bit = buttonMask(sample.changedButton)
        if (!hasButtonListeners || bit == 0 || isForOthers(sample)) return
        PointerRouting.claim(sample.routingKey)
        if (!sample.handled && pressedButtons == 0 && !captured) captured = target.capture()
        clickCount = if (continuesClicks(sample)) clickCount + 1 else 1
        lastPressX = sample.x
        lastPressY = sample.y
        lastPressTime = sample.timestampMillis
        lastPressButton = sample.changedButton
        pressedButtons = pressedButtons or bit
        clickAllowed = true
        val event = mouseEvent(sample, WMouseEvent.MOUSE_PRESSED, clickCount, sample.changedButton)
        fire(event, target, mouseListeners) { it.mousePressed(event) }
    }

    /** Whether this press continues the previous one (same button, within Windows' double-click time and range). */
    private fun continuesClicks(sample: PointerSample): Boolean {
        if (sample.changedButton != lastPressButton || lastPressTime == Long.MIN_VALUE) return false
        val elapsed = sample.timestampMillis - lastPressTime
        if (elapsed < 0 || elapsed > Win32.doubleClickTimeMillis()) return false
        // The double-click range (SM_CXDOUBLECLK x SM_CYDOUBLECLK) is a rectangle in physical pixels centered on the previous press
        val dx = abs(sample.x - lastPressX) * sample.pixelsPerDip
        val dy = abs(sample.y - lastPressY) * sample.pixelsPerDip
        return dx * 2 <= Win32.systemMetric(Win32.SM_CXDOUBLECLK) && dy * 2 <= Win32.systemMetric(Win32.SM_CYDOUBLECLK)
    }

    private fun onReleased(sample: PointerSample, target: PointerTarget) {
        val bit = buttonMask(sample.changedButton)
        if (bit == 0 || pressedButtons and bit == 0) return // Not a button pressed here
        PointerRouting.claim(sample.routingKey)
        pressedButtons = pressedButtons and bit.inv()
        val released = mouseEvent(
            sample,
            WMouseEvent.MOUSE_RELEASED,
            clickCount,
            sample.changedButton,
            popupTrigger = sample.changedButton == WMouseEvent.BUTTON3,
        )
        fire(released, target, mouseListeners) { it.mouseReleased(released) }
        if (clickAllowed && isInside(sample)) {
            val clicked = mouseEvent(sample, WMouseEvent.MOUSE_CLICKED, clickCount, sample.changedButton)
            fire(clicked, target, mouseListeners) { it.mouseClicked(clicked) }
        }
        if (pressedButtons == 0) {
            clickAllowed = false
            if (captured) {
                captured = false
                target.releaseCapture()
            }
        }
    }

    private fun onMoved(sample: PointerSample, target: PointerTarget) {
        if (!hasButtonListeners || PointerRouting.isClaimed(sample.routingKey)) return
        if (pressedButtons != 0) {
            PointerRouting.claim(sample.routingKey)
            if (clickAllowed && beyondDragThreshold(sample)) clickAllowed = false
            val event = mouseEvent(sample, WMouseEvent.MOUSE_DRAGGED, 0, WMouseEvent.NOBUTTON)
            fire(event, target, motionListeners) { it.mouseDragged(event) }
            return
        }
        // Moving with a button held down is a drag of the pressed component (another element)
        if (sample.buttonsDown != 0 || (sample.handled && sample.handledByContent())) return
        PointerRouting.claim(sample.routingKey)
        val event = mouseEvent(sample, WMouseEvent.MOUSE_MOVED, 0, WMouseEvent.NOBUTTON)
        fire(event, target, motionListeners) { it.mouseMoved(event) }
    }

    private fun beyondDragThreshold(sample: PointerSample): Boolean {
        val dx = abs(sample.x - lastPressX) * sample.pixelsPerDip
        val dy = abs(sample.y - lastPressY) * sample.pixelsPerDip
        return dx > Win32.systemMetric(Win32.SM_CXDRAG) || dy > Win32.systemMetric(Win32.SM_CYDRAG)
    }

    private fun onEntered(sample: PointerSample, target: PointerTarget) {
        if (inside) return
        inside = true
        val event = mouseEvent(sample, WMouseEvent.MOUSE_ENTERED, 0, WMouseEvent.NOBUTTON)
        fire(event, target, mouseListeners) { it.mouseEntered(event) }
    }

    private fun onExited(sample: PointerSample, target: PointerTarget) {
        if (!inside) return
        inside = false
        val event = mouseEvent(sample, WMouseEvent.MOUSE_EXITED, 0, WMouseEvent.NOBUTTON)
        fire(event, target, mouseListeners) { it.mouseExited(event) }
    }

    private fun onWheel(sample: PointerSample, target: PointerTarget) {
        if (wheelListeners.isEmpty() || isForOthers(sample)) return
        PointerRouting.claim(sample.routingKey)
        // In Swing, positive is toward the user (scrolls down). WinUI's vertical wheel is positive away from the user, so the sign is flipped; the horizontal wheel stays positive to the right
        val precise = (if (sample.isHorizontalWheel) sample.wheelDelta else -sample.wheelDelta) / WHEEL_DELTA
        val lines = Win32.wheelScrollLines()
        val shift = if (sample.isHorizontalWheel) WInputEvent.SHIFT_DOWN_MASK else 0
        val event = WMouseWheelEvent(
            owner,
            WMouseEvent.MOUSE_WHEEL,
            System.currentTimeMillis(),
            sample.keyModifiersEx or sample.buttonsDown or shift,
            sample.x,
            sample.y,
            0,
            false,
            if (lines == Win32.WHEEL_PAGESCROLL) WMouseWheelEvent.WHEEL_BLOCK_SCROLL else WMouseWheelEvent.WHEEL_UNIT_SCROLL,
            if (lines == Win32.WHEEL_PAGESCROLL) 1 else lines,
            precise.toInt(),
            precise,
            sample.isHorizontalWheel,
        )
        fire(event, target, wheelListeners) { it.mouseWheelMoved(event) }
    }

    /** The capture was lost (e.g. the window became inactive). Discards the pressed state (does not report a release). */
    private fun onCaptureLost() {
        if (!captured) return // With the control's own capture (e.g. a button), it stays pressed until released
        captured = false
        pressedButtons = 0
        clickAllowed = false
    }

    private fun isInside(sample: PointerSample): Boolean =
        sample.x >= 0 && sample.y >= 0 && sample.x < owner.actualWidth && sample.y < owner.actualHeight

    private fun mouseEvent(sample: PointerSample, id: Int, clickCount: Int, button: Int, popupTrigger: Boolean = false) = WMouseEvent(
        owner,
        id,
        System.currentTimeMillis(),
        sample.keyModifiersEx or sample.buttonsDown,
        sample.x,
        sample.y,
        clickCount,
        popupTrigger,
        button,
    )

    private inline fun <L> fire(event: WInputEvent, target: PointerTarget, listeners: List<L>, call: (L) -> Unit) {
        event.onConsume = { target.markHandled() }
        try {
            for (listener in listeners.toList()) call(listener)
        } finally {
            event.onConsume = null
        }
    }

    private fun addHandlers() {
        val anchor = owner.listenerAnchor
        // Press, release, move, wheel and capture loss bubble from child to parent and controls (ButtonBase etc.) mark them
        // handled, so handled ones are received too. Enter/exit are sent directly to each element by WinUI (they do not
        // bubble to the parent; ProcessPointerEnterLeave in PointerInputProcessor.cpp), so they are subscribed normally
        val routed = listOf(
            XamlInterop.IUIElementStatics_get_PointerPressedEvent to PointerKind.PRESSED,
            XamlInterop.IUIElementStatics_get_PointerReleasedEvent to PointerKind.RELEASED,
            XamlInterop.IUIElementStatics_get_PointerMovedEvent to PointerKind.MOVED,
            XamlInterop.IUIElementStatics_get_PointerWheelChangedEvent to PointerKind.WHEEL,
            XamlInterop.IUIElementStatics_get_PointerCaptureLostEvent to PointerKind.CAPTURE_LOST,
        )
        for ((eventSlot, kind) in routed) {
            Xaml.addHandlerHandledToo(
                owner,
                eventSlot,
                XamlInterop.IID_PointerEventHandler,
                FoundationInterop.IID_IReference_PointerEventHandler,
            ) { _, args -> anchor.get()?.input?.mouse?.onNative(kind, args) }
        }
        for ((addSlot, kind) in listOf(
            XamlInterop.IUIElement_add_PointerEntered to PointerKind.ENTERED,
            XamlInterop.IUIElement_add_PointerExited to PointerKind.EXITED,
        )) {
            owner.uiElement.addEventHandler("WinUI4K.MouseListenerPointer", XamlInterop.IID_PointerEventHandler, addSlot) { _, args ->
                anchor.get()?.input?.mouse?.onNative(kind, args)
            }
        }
    }

    private fun onNative(kind: PointerKind, args: Ptr) {
        val event = XamlPointerEvent(ComPtr(args))
        dispatch(nativeSample(kind, event, args), NativePointerTarget(owner, event))
    }

    private fun nativeSample(kind: PointerKind, event: XamlPointerEvent, args: Ptr): PointerSample {
        val point = event.pointState(owner)
        return PointerSample(
            kind = kind,
            x = point.x,
            y = point.y,
            changedButton = point.changedButton,
            buttonsDown = point.buttonsDown,
            keyModifiersEx = WInputEvent.fromVirtualKeyModifiers(event.modifiers),
            timestampMillis = point.timestampMicros / MICROS_PER_MILLI,
            wheelDelta = point.wheelDelta,
            isHorizontalWheel = point.isHorizontalWheel,
            handled = event.handled,
            handledByContent = { handledByContent(args) },
            routingKey = NativePointerKey(kind, point.pointerId, point.frameId, point.timestampMicros),
            pixelsPerDip = rasterizationScale(),
        )
    }

    private fun handledByContent(args: Ptr): Boolean {
        val source = Xaml.originalSource(args) ?: return true
        return try {
            isHandledInContent(owner, source)
        } finally {
            source.release()
        }
    }

    private fun rasterizationScale(): Double {
        val root = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return 1.0
        return try {
            root.getDouble(XamlInterop.IXamlRoot_get_RasterizationScale)
        } finally {
            root.release()
        }
    }

    private companion object {
        /** Rotation of one wheel notch (WHEEL_DELTA). */
        const val WHEEL_DELTA = 120.0
        const val MICROS_PER_MILLI = 1000L
    }
}

/** The [WInputEvent.modifiersEx] bit for a button such as WMouseEvent.BUTTON1. 0 if it is not a button. */
private fun buttonMask(button: Int): Int = if (button in 1..MAX_MOUSE_BUTTON) WInputEvent.getMaskForButton(button) else 0

private const val MAX_MOUSE_BUTTON = 5

/** Operations on the element a native event reached. */
private class NativePointerTarget(private val owner: WComponent, private val event: XamlPointerEvent) : PointerTarget {
    override fun capture(): Boolean = event.capture(owner)

    override fun releaseCapture() = event.release(owner)

    override fun markHandled() {
        event.handled = true
    }
}

/** Pointer state read via GetCurrentPoint ([XamlPointerEvent.pointState]). */
@Suppress("LongParameterList") // The parameters map 1:1 to the values read from PointerPoint
internal class PointerPointState(
    val x: Double,
    val y: Double,
    val changedButton: Int,
    val buttonsDown: Int,
    val wheelDelta: Int,
    val isHorizontalWheel: Boolean,
    val pointerId: Int,
    val frameId: Int,
    val timestampMicros: Long,
)

/** Reads PointerPoint.Timestamp (u8). */
internal fun readTimestamp(point: ComPtr): Long = Ffi.backend.withScope { scope ->
    val out = scope.allocate(8)
    point.call(WindowingInterop.IPointerPoint_get_Timestamp, out)
    Ffi.backend.memory.getLong(out, 0)
}

/** Maps PointerUpdateKind (the button whose state changed) to WMouseEvent.BUTTON1 etc. NOBUTTON if no button changed. */
internal fun buttonOfUpdateKind(updateKind: Int): Int = when (updateKind) {
    WindowingInterop.PointerUpdateKind_LeftButtonPressed, WindowingInterop.PointerUpdateKind_LeftButtonReleased -> WMouseEvent.BUTTON1
    WindowingInterop.PointerUpdateKind_MiddleButtonPressed, WindowingInterop.PointerUpdateKind_MiddleButtonReleased -> WMouseEvent.BUTTON2
    WindowingInterop.PointerUpdateKind_RightButtonPressed, WindowingInterop.PointerUpdateKind_RightButtonReleased -> WMouseEvent.BUTTON3
    WindowingInterop.PointerUpdateKind_XButton1Pressed, WindowingInterop.PointerUpdateKind_XButton1Released -> EXTRA_BUTTON_1
    WindowingInterop.PointerUpdateKind_XButton2Pressed, WindowingInterop.PointerUpdateKind_XButton2Released -> EXTRA_BUTTON_2
    else -> WMouseEvent.NOBUTTON
}

/** AWT button numbers of the extra buttons (back / forward). */
internal const val EXTRA_BUTTON_1 = 4
internal const val EXTRA_BUTTON_2 = 5
