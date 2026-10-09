package com.appkitbox.winui4k

import com.appkitbox.winui4k.ink.InkPoint
import com.appkitbox.winui4k.ink.InkRect
import com.appkitbox.winui4k.ink.InkStroke
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.InkInterop
import java.util.EventListener
import java.util.EventObject

/**
 * Windows.UI.Core.CoreInputDeviceTypes (devices that can input ink; bit flags).
 * Values extracted from Windows.Foundation.UniversalApiContract.winmd (Touch=1, Pen=2, Mouse=4).
 */
enum class CoreInputDeviceType(internal val native: Int) {
    /** Touch. */
    TOUCH(1),

    /** Pen (the InkCanvas default is pen only). */
    PEN(2),

    /** Mouse. */
    MOUSE(4),
    ;

    internal companion object {
        @JvmSynthetic
        fun setOf(mask: Int): Set<CoreInputDeviceType> = entries.filterTo(java.util.EnumSet.noneOf(CoreInputDeviceType::class.java)) {
            mask and it.native != 0
        }

        @JvmSynthetic
        fun maskOf(types: Set<CoreInputDeviceType>): Int = types.fold(0) { mask, type -> mask or type.native }
    }
}

/**
 * Windows.Devices.Input.PointerDeviceType (the kind of device for pointer input).
 * Values extracted from Windows.Foundation.UniversalApiContract.winmd (Touch=0, Pen=1, Mouse=2, Touchpad=3).
 */
enum class PointerDeviceType(internal val native: Int) {
    /** Touch. */
    TOUCH(0),

    /** Pen. */
    PEN(1),

    /** Mouse. */
    MOUSE(2),

    /** Touchpad. */
    TOUCHPAD(3),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): PointerDeviceType = entries.first { it.native == native }
    }
}

/**
 * Windows.UI.Input.PointerUpdateKind (the change in the state of the pointer's buttons).
 * Values extracted from Windows.Foundation.UniversalApiContract.winmd.
 */
enum class PointerUpdateKind(internal val native: Int) {
    /** No button change (such as a move). */
    OTHER(0),

    /** The left button was pressed. */
    LEFT_BUTTON_PRESSED(1),

    /** The left button was released. */
    LEFT_BUTTON_RELEASED(2),

    /** The right button was pressed. */
    RIGHT_BUTTON_PRESSED(3),

    /** The right button was released. */
    RIGHT_BUTTON_RELEASED(4),

    /** The middle button was pressed. */
    MIDDLE_BUTTON_PRESSED(5),

    /** The middle button was released. */
    MIDDLE_BUTTON_RELEASED(6),

    /** The X1 button was pressed. */
    X_BUTTON1_PRESSED(7),

    /** The X1 button was released. */
    X_BUTTON1_RELEASED(8),

    /** The X2 button was pressed. */
    X_BUTTON2_PRESSED(9),

    /** The X2 button was released. */
    X_BUTTON2_RELEASED(10),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): PointerUpdateKind = entries.first { it.native == native }
    }
}

/**
 * A snapshot of Windows.UI.Input.PointerPoint: the position and state of the pointer for ink input
 * ([InkPointerEvent.pointerPoint]). Positions use the top-left of the canvas as the origin (DIP).
 */
data class InkPointerPoint(
    /** The X coordinate (Position.X). */
    val x: Double,
    /** The Y coordinate (Position.Y). */
    val y: Double,
    /** The pointer ID (the same value for a series of input from the same finger or pen). */
    val pointerId: Int,
    /** The time of the input (microseconds; the same timeline as [InkPoint.timestamp]). */
    val timestamp: Long,
    /** Whether the pointer is in contact with the screen (the pen tip is touching or a button is pressed). */
    val isInContact: Boolean,
    /** The kind of input device. */
    val deviceType: PointerDeviceType,
    /** The pressure (0.0 to 1.0; 0.5 for devices without pressure). */
    val pressure: Float,
    /** The tilt along the X axis (degrees). */
    val tiltX: Float,
    /** The tilt along the Y axis (degrees). */
    val tiltY: Float,
    /** The rotation of the pen around its axis (degrees). */
    val twist: Float,
    /** The orientation of the contact (degrees). */
    val orientation: Float,
    /** Whether input is made with the eraser end of the pen. */
    val isEraser: Boolean,
    /** Whether the pen is held upside down. */
    val isInverted: Boolean,
    /** Whether the pen's barrel button is pressed. */
    val isBarrelButtonPressed: Boolean,
    /** Whether the left button (or pen tip or touch contact) is pressed. */
    val isLeftButtonPressed: Boolean,
    /** Whether the right button is pressed. */
    val isRightButtonPressed: Boolean,
    /** Whether the middle button is pressed. */
    val isMiddleButtonPressed: Boolean,
    /** Whether this is the primary pointer (such as the first finger in multi-touch). */
    val isPrimary: Boolean,
    /** Whether the pen is within detection range. */
    val isInRange: Boolean,
    /** Whether the input was canceled. */
    val isCanceled: Boolean,
    /** The X coordinate before prediction and correction (RawPosition.X). */
    val rawX: Double,
    /** The Y coordinate before prediction and correction (RawPosition.Y). */
    val rawY: Double,
    /** The ID of the input frame (FrameId; the same value for multiple pointers that arrived at the same time). */
    val frameId: Int,
    /** The contact area (ContactRect; such as the size of a finger for touch). */
    val contactRect: InkRect,
    /** The contact area before correction (ContactRectRaw). */
    val contactRectRaw: InkRect,
    /** Whether the touch was judged to be an intended contact (TouchConfidence). */
    val touchConfidence: Boolean,
    /** The wheel rotation amount (MouseWheelDelta; positive = away / right). */
    val mouseWheelDelta: Int,
    /** Whether this is a horizontal wheel (IsHorizontalMouseWheel). */
    val isHorizontalMouseWheel: Boolean,
    /** Whether the X1 button is pressed. */
    val isXButton1Pressed: Boolean,
    /** Whether the X2 button is pressed. */
    val isXButton2Pressed: Boolean,
    /** The change in the state of the buttons (PointerUpdateKind). */
    val updateKind: PointerUpdateKind,
) {
    /** The position, pressure, tilt, and time as an [InkPoint]. */
    fun toInkPoint(): InkPoint = InkPoint(
        x,
        y,
        pressure.coerceIn(0f, 1f),
        tiltX.coerceIn(-MAX_TILT, MAX_TILT),
        tiltY.coerceIn(-MAX_TILT, MAX_TILT),
        timestamp,
    )

    private companion object {
        const val MAX_TILT = 90f
    }
}

/**
 * Like java.awt.event.MouseEvent: a pointer event for ink input
 * (the event argument Windows.UI.Core.PointerEventArgs of InkStrokeInput / InkUnprocessedInput).
 *
 * WinUI (experimental in Windows App SDK 2.5) receives this event on a dedicated ink thread and then delivers it to
 * the UI thread late, so by the time it arrives the original arguments may no longer be readable (reads fail with
 * CO_E_NOT_SUPPORTED and the like). Therefore read failures do not throw; if values cannot be read, [pointerPoint] /
 * [modifiers] are null and [intermediatePoints] is empty.
 * Values are fetched together and kept the first time they are read during the listener call, so they can be used
 * after the call as well (values never read during the call are null after the call).
 */
class InkPointerEvent internal constructor(source: WInkPresenter, private val args: ComPtr) : EventObject(source) {
    private var disposed = false
    private var pointCache: Lazy<InkPointerPoint?> = lazy { readPoint() }
    private var modifiersCache: Lazy<Set<VirtualKeyModifier>?> = lazy { readModifiers() }
    private var intermediateCache: Lazy<List<InkPoint>> = lazy { readIntermediatePoints() }

    /** The source of the event (the canvas's InkPresenter). */
    override fun getSource(): WInkPresenter = super.getSource() as WInkPresenter

    /** The position and state of the pointer (PointerEventArgs.CurrentPoint), or null if it cannot be read. */
    val pointerPoint: InkPointerPoint?
        get() = pointCache.value

    /** The modifier keys that were pressed (PointerEventArgs.KeyModifiers), or null if they cannot be read. */
    val modifiers: Set<VirtualKeyModifier>?
        get() = modifiersCache.value

    /**
     * The input points that arrived between the previous event and this one (GetIntermediatePoints).
     * The newest point comes first (the WinRT order as is). Empty if they cannot be read.
     */
    val intermediatePoints: List<InkPoint>
        get() = intermediateCache.value

    /**
     * Whether the event has been handled (ICoreWindowEventArgs.Handled). Ink input processing has already proceeded,
     * so setting this to true is not guaranteed to have any effect (a limitation of the WinUI implementation).
     * If it cannot be read or written, reads return false and writes do nothing.
     */
    var isHandled: Boolean
        get() = withCoreWindowEventArgs { it.getBool(InkInterop.ICoreWindowEventArgs_get_Handled) } ?: false
        set(value) {
            withCoreWindowEventArgs { it.putBool(InkInterop.ICoreWindowEventArgs_put_Handled, value) }
        }

    private fun <T> withCoreWindowEventArgs(block: (ComPtr) -> T): T? {
        if (disposed) return null
        return runCatching {
            val view = args.queryInterface(InkInterop.IID_ICoreWindowEventArgs)
            try {
                block(view)
            } finally {
                view.release()
            }
        }.getOrNull()
    }

    private fun readPoint(): InkPointerPoint? {
        if (disposed) return null
        return runCatching {
            val point = args.getPtr(InkInterop.IPointerEventArgs_get_CurrentPoint)
            try {
                readPointerPoint(point)
            } finally {
                point.release()
            }
        }.getOrNull()
    }

    private fun readModifiers(): Set<VirtualKeyModifier>? {
        if (disposed) return null
        return runCatching {
            val mask = args.getInt(InkInterop.IPointerEventArgs_get_KeyModifiers)
            VirtualKeyModifier.entries.filterTo(java.util.EnumSet.noneOf(VirtualKeyModifier::class.java)) { mask and it.native != 0 }
        }.getOrNull()
    }

    private fun readIntermediatePoints(): List<InkPoint> {
        if (disposed) return emptyList()
        return runCatching {
            val vector = args.getPtr(InkInterop.IPointerEventArgs_GetIntermediatePoints)
            try {
                val size = vector.getInt(FoundationInterop.IVector_get_Size)
                List(size) { index ->
                    val point = vector.getPtr(FoundationInterop.IVector_GetAt, index)
                    try {
                        readPointerPoint(point).toInkPoint()
                    } finally {
                        point.release()
                    }
                }
            } finally {
                vector.release()
            }
        }.getOrDefault(emptyList())
    }

    /** Called when the listener call is over. Values not read yet become null (empty) from then on. */
    internal fun dispose() {
        disposed = true
        if (!pointCache.isInitialized()) pointCache = lazyOf(null)
        if (!modifiersCache.isInitialized()) modifiersCache = lazyOf(null)
        if (!intermediateCache.isInitialized()) intermediateCache = lazyOf(emptyList())
    }
}

/** Takes a snapshot of a Windows.UI.Input.PointerPoint (IPointerPoint). */
private fun readPointerPoint(point: ComPtr): InkPointerPoint {
    val position = Xaml.readPoint(point, InkInterop.IPointerPoint_get_Position)
    val raw = Xaml.readPoint(point, InkInterop.IPointerPoint_get_RawPosition)
    val timestamp = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        point.call(InkInterop.IPointerPoint_get_Timestamp, out)
        Ffi.backend.memory.getLong(out, 0)
    }
    val device = point.getPtr(InkInterop.IPointerPoint_get_PointerDevice)
    val deviceType = try {
        PointerDeviceType.of(device.getInt(InkInterop.IPointerDevice_get_PointerDeviceType))
    } finally {
        device.release()
    }
    val properties = point.getPtr(InkInterop.IPointerPoint_get_Properties)
    try {
        return InkPointerPoint(
            x = position[0],
            y = position[1],
            pointerId = point.getInt(InkInterop.IPointerPoint_get_PointerId),
            timestamp = timestamp,
            isInContact = point.getBool(InkInterop.IPointerPoint_get_IsInContact),
            deviceType = deviceType,
            pressure = properties.getFloat(InkInterop.IPointerPointProperties_get_Pressure),
            tiltX = properties.getFloat(InkInterop.IPointerPointProperties_get_XTilt),
            tiltY = properties.getFloat(InkInterop.IPointerPointProperties_get_YTilt),
            twist = properties.getFloat(InkInterop.IPointerPointProperties_get_Twist),
            orientation = properties.getFloat(InkInterop.IPointerPointProperties_get_Orientation),
            isEraser = properties.getBool(InkInterop.IPointerPointProperties_get_IsEraser),
            isInverted = properties.getBool(InkInterop.IPointerPointProperties_get_IsInverted),
            isBarrelButtonPressed = properties.getBool(InkInterop.IPointerPointProperties_get_IsBarrelButtonPressed),
            isLeftButtonPressed = properties.getBool(InkInterop.IPointerPointProperties_get_IsLeftButtonPressed),
            isRightButtonPressed = properties.getBool(InkInterop.IPointerPointProperties_get_IsRightButtonPressed),
            isMiddleButtonPressed = properties.getBool(InkInterop.IPointerPointProperties_get_IsMiddleButtonPressed),
            isPrimary = properties.getBool(InkInterop.IPointerPointProperties_get_IsPrimary),
            isInRange = properties.getBool(InkInterop.IPointerPointProperties_get_IsInRange),
            isCanceled = properties.getBool(InkInterop.IPointerPointProperties_get_IsCanceled),
            rawX = raw[0],
            rawY = raw[1],
            frameId = point.getInt(InkInterop.IPointerPoint_get_FrameId),
            contactRect = InkNative.callForRect(properties, InkInterop.IPointerPointProperties_get_ContactRect),
            contactRectRaw = InkNative.callForRect(properties, InkInterop.IPointerPointProperties_get_ContactRectRaw),
            touchConfidence = properties.getBool(InkInterop.IPointerPointProperties_get_TouchConfidence),
            mouseWheelDelta = properties.getInt(InkInterop.IPointerPointProperties_get_MouseWheelDelta),
            isHorizontalMouseWheel = properties.getBool(InkInterop.IPointerPointProperties_get_IsHorizontalMouseWheel),
            isXButton1Pressed = properties.getBool(InkInterop.IPointerPointProperties_get_IsXButton1Pressed),
            isXButton2Pressed = properties.getBool(InkInterop.IPointerPointProperties_get_IsXButton2Pressed),
            updateKind = PointerUpdateKind.of(properties.getInt(InkInterop.IPointerPointProperties_get_PointerUpdateKind)),
        )
    } finally {
        properties.release()
    }
}

/**
 * Like java.awt.event.MouseListener: the receiver of stroke input (InkPresenter.StrokeInput) events.
 * For input processed as ink, it receives the start, continuation, end, and cancellation of drawing.
 * Methods you do not use need not be implemented (they do nothing by default).
 */
interface InkStrokeInputListener : EventListener {
    /** Drawing started (StrokeStarted). */
    fun strokeStarted(event: InkPointerEvent) {}

    /** The pointer moved while drawing (StrokeContinued). */
    fun strokeContinued(event: InkPointerEvent) {}

    /** Drawing ended (StrokeEnded). */
    fun strokeEnded(event: InkPointerEvent) {}

    /** Drawing was canceled midway (StrokeCanceled). */
    fun strokeCanceled(event: InkPointerEvent) {}
}

/**
 * Like java.awt.event.MouseListener / MouseMotionListener: the receiver of events for input that was not processed as
 * ink (InkPresenter.UnprocessedInput). It receives input while the input processing mode is
 * [InkInputProcessingMode.NONE], and right-button input when right drags are set not to be processed
 * (used for implementing lasso selection and the like). Methods you do not use need not be implemented (they do
 * nothing by default).
 * In the experimental Windows App SDK 2.5, the position and other values of the event arguments may not be readable
 * (see [InkPointerEvent]).
 */
interface InkUnprocessedInputListener : EventListener {
    /** The pointer entered the canvas (PointerEntered). */
    fun pointerEntered(event: InkPointerEvent) {}

    /** The pen moved over the canvas without touching it (PointerHovered). */
    fun pointerHovered(event: InkPointerEvent) {}

    /** The pointer left the canvas (PointerExited). */
    fun pointerExited(event: InkPointerEvent) {}

    /** Pressed (PointerPressed). */
    fun pointerPressed(event: InkPointerEvent) {}

    /** Moved while pressed (PointerMoved). */
    fun pointerMoved(event: InkPointerEvent) {}

    /** Released (PointerReleased). */
    fun pointerReleased(event: InkPointerEvent) {}

    /** Capture was lost (PointerLost). */
    fun pointerLost(event: InkPointerEvent) {}
}

/** The argument of InkPresenter.StrokesCollected / StrokesErased: the strokes that were drawn (or erased). */
class InkStrokesEvent internal constructor(
    source: WInkPresenter,
    /** The strokes that were drawn (erased). */
    val strokes: List<InkStroke>,
) : EventObject(source) {
    /** The source of the event (the canvas's InkPresenter). */
    override fun getSource(): WInkPresenter = super.getSource() as WInkPresenter
}

/**
 * The receiver of notifications that strokes were drawn or erased ([WInkPresenter.addStrokesCollectedListener] and
 * others).
 */
fun interface InkStrokesListener : EventListener {
    fun strokesChanged(event: InkStrokesEvent)
}
