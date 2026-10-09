package com.appkitbox.winui4k

import com.appkitbox.winui4k.ink.InkPoint
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
 * Like java.awt.event.MouseEvent: a pointer event for ink input
 * (the event argument Windows.UI.Core.PointerEventArgs of InkStrokeInput / InkUnprocessedInput).
 *
 * Values are fetched from native code when read, so they are **valid only during the listener call**
 * (reading them after the call returns throws an exception). Positions use the top-left of the canvas as the
 * origin (DIP).
 */
class InkPointerEvent internal constructor(source: WInkPresenter, private val args: ComPtr) : EventObject(source) {
    private var disposed = false
    private var currentPoint: ComPtr? = null
    private var properties: ComPtr? = null

    /** The source of the event (the canvas's InkPresenter). */
    override fun getSource(): WInkPresenter = super.getSource() as WInkPresenter

    private fun point(): ComPtr {
        check(!disposed) { "InkPointerEvent can only be used during the listener call" }
        return currentPoint ?: args.getPtr(InkInterop.IPointerEventArgs_get_CurrentPoint).also { currentPoint = it }
    }

    private fun properties(): ComPtr =
        properties ?: point().getPtr(InkInterop.IPointerPoint_get_Properties).also { properties = it }

    /** The X coordinate of the pointer (PointerPoint.Position.X). */
    val x: Double get() = position()[0]

    /** The Y coordinate of the pointer (PointerPoint.Position.Y). */
    val y: Double get() = position()[1]

    private fun position(): DoubleArray = Xaml.readPoint(point(), InkInterop.IPointerPoint_get_Position)

    /** The pointer ID (the same value for a series of input from the same finger or pen). */
    val pointerId: Int get() = point().getInt(InkInterop.IPointerPoint_get_PointerId)

    /** The time of the input (microseconds; the same timeline as [InkPoint.timestamp]). */
    val timestamp: Long
        get() = Ffi.backend.withScope { scope ->
            val out = scope.allocate(8)
            point().call(InkInterop.IPointerPoint_get_Timestamp, out)
            Ffi.backend.memory.getLong(out, 0)
        }

    /** Whether the pointer is in contact with the screen (the pen tip is touching or a button is pressed). */
    val isInContact: Boolean get() = point().getBool(InkInterop.IPointerPoint_get_IsInContact)

    /** The kind of input device. */
    val deviceType: PointerDeviceType
        get() {
            val device = point().getPtr(InkInterop.IPointerPoint_get_PointerDevice)
            return try {
                PointerDeviceType.of(device.getInt(InkInterop.IPointerDevice_get_PointerDeviceType))
            } finally {
                device.release()
            }
        }

    /** The pressure (0.0 to 1.0; 0.5 for devices without pressure). */
    val pressure: Float get() = properties().getFloat(InkInterop.IPointerPointProperties_get_Pressure)

    /** The tilt along the X axis (degrees). */
    val tiltX: Float get() = properties().getFloat(InkInterop.IPointerPointProperties_get_XTilt)

    /** The tilt along the Y axis (degrees). */
    val tiltY: Float get() = properties().getFloat(InkInterop.IPointerPointProperties_get_YTilt)

    /** The rotation of the pen around its axis (degrees). */
    val twist: Float get() = properties().getFloat(InkInterop.IPointerPointProperties_get_Twist)

    /** The orientation of the contact (degrees). */
    val orientation: Float get() = properties().getFloat(InkInterop.IPointerPointProperties_get_Orientation)

    /** Whether input is made with the eraser end of the pen. */
    val isEraser: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsEraser)

    /** Whether the pen is held upside down. */
    val isInverted: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsInverted)

    /** Whether the pen's barrel button is pressed. */
    val isBarrelButtonPressed: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsBarrelButtonPressed)

    /** Whether the left button (or pen tip or touch contact) is pressed. */
    val isLeftButtonPressed: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsLeftButtonPressed)

    /** Whether the right button is pressed. */
    val isRightButtonPressed: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsRightButtonPressed)

    /** Whether the middle button is pressed. */
    val isMiddleButtonPressed: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsMiddleButtonPressed)

    /** Whether this is the primary pointer (such as the first finger in multi-touch). */
    val isPrimary: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsPrimary)

    /** Whether the pen is within detection range. */
    val isInRange: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsInRange)

    /** Whether the input was canceled. */
    val isCanceled: Boolean get() = properties().getBool(InkInterop.IPointerPointProperties_get_IsCanceled)

    /** The modifier keys that were pressed (PointerEventArgs.KeyModifiers). */
    val modifiers: Set<VirtualKeyModifier>
        get() {
            check(!disposed) { "InkPointerEvent can only be used during the listener call" }
            val mask = args.getInt(InkInterop.IPointerEventArgs_get_KeyModifiers)
            return VirtualKeyModifier.entries.filterTo(java.util.EnumSet.noneOf(VirtualKeyModifier::class.java)) {
                mask and it.native != 0
            }
        }

    /**
     * Whether the event has been handled (ICoreWindowEventArgs.Handled). Ink input reaches the UI thread after it has
     * been processed on a dedicated ink thread, so even if this is set to true, the OS-side processing may already
     * have proceeded (a limitation of the WinUI implementation).
     */
    var isHandled: Boolean
        get() = withCoreWindowEventArgs { it.getBool(InkInterop.ICoreWindowEventArgs_get_Handled) }
        set(value) = withCoreWindowEventArgs { it.putBool(InkInterop.ICoreWindowEventArgs_put_Handled, value) }

    private fun <T> withCoreWindowEventArgs(block: (ComPtr) -> T): T {
        check(!disposed) { "InkPointerEvent can only be used during the listener call" }
        val view = args.queryInterface(InkInterop.IID_ICoreWindowEventArgs)
        return try {
            block(view)
        } finally {
            view.release()
        }
    }

    /** The current position, pressure, tilt, and time as an [InkPoint]. */
    fun toInkPoint(): InkPoint = InkPoint(
        x,
        y,
        pressure.coerceIn(0f, 1f),
        tiltX.coerceIn(-MAX_TILT, MAX_TILT),
        tiltY.coerceIn(-MAX_TILT, MAX_TILT),
        timestamp,
    )

    /**
     * The input points that arrived between the previous event and this one (GetIntermediatePoints).
     * The newest point comes first (the WinRT order as is).
     */
    fun getIntermediatePoints(): List<InkPoint> {
        check(!disposed) { "InkPointerEvent can only be used during the listener call" }
        val vector = args.getPtr(InkInterop.IPointerEventArgs_GetIntermediatePoints)
        try {
            val size = vector.getInt(FoundationInterop.IVector_get_Size)
            return List(size) { index ->
                val point = vector.getPtr(FoundationInterop.IVector_GetAt, index)
                try {
                    val position = Xaml.readPoint(point, InkInterop.IPointerPoint_get_Position)
                    val pointProperties = point.getPtr(InkInterop.IPointerPoint_get_Properties)
                    val timestamp = Ffi.backend.withScope { scope ->
                        val out = scope.allocate(8)
                        point.call(InkInterop.IPointerPoint_get_Timestamp, out)
                        Ffi.backend.memory.getLong(out, 0)
                    }
                    try {
                        InkPoint(
                            position[0],
                            position[1],
                            pointProperties.getFloat(InkInterop.IPointerPointProperties_get_Pressure).coerceIn(0f, 1f),
                            pointProperties.getFloat(InkInterop.IPointerPointProperties_get_XTilt).coerceIn(-MAX_TILT, MAX_TILT),
                            pointProperties.getFloat(InkInterop.IPointerPointProperties_get_YTilt).coerceIn(-MAX_TILT, MAX_TILT),
                            timestamp,
                        )
                    } finally {
                        pointProperties.release()
                    }
                } finally {
                    point.release()
                }
            }
        } finally {
            vector.release()
        }
    }

    /** Called when the listener call is over. Releases the native references obtained. */
    internal fun dispose() {
        disposed = true
        properties?.release()
        currentPoint?.release()
        properties = null
        currentPoint = null
    }

    private companion object {
        const val MAX_TILT = 90f
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
