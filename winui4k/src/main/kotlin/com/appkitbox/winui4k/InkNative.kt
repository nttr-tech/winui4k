package com.appkitbox.winui4k

import com.appkitbox.winui4k.ink.InkDrawingAttributes
import com.appkitbox.winui4k.ink.InkDrawingAttributesKind
import com.appkitbox.winui4k.ink.InkModelerAttributes
import com.appkitbox.winui4k.ink.InkPoint
import com.appkitbox.winui4k.ink.InkRect
import com.appkitbox.winui4k.ink.InkStroke
import com.appkitbox.winui4k.ink.InkStrokeRenderingSegment
import com.appkitbox.winui4k.ink.InkTransform
import com.appkitbox.winui4k.ink.PenTipShape
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.MemoryScope
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.StructValue
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.InkInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.time.Duration
import java.time.Instant

/**
 * Conversion between the Ink model (the value objects in com.appkitbox.winui4k.ink) and WinRT objects
 * (Windows.UI.Input.Inking.InkStroke / InkDrawingAttributes and so on).
 *
 * The [ComPtr] returned by the creation functions is an owned reference (the caller releases it).
 * Shared objects such as InkStrokeBuilder and the InkPoint factory are held for the lifetime of the process.
 */
internal object InkNative {
    /** The IID of IUnknown (used to compare COM identity). */
    private const val IID_IUnknown = "00000000-0000-0000-c000-000000000046"

    /** The difference, in 100ns units, from the Windows.Foundation.DateTime origin (1601-01-01) to the Unix epoch. */
    private const val EPOCH_OFFSET_TICKS = 116_444_736_000_000_000L
    private const val NANOS_PER_TICK = 100L
    private const val TICKS_PER_SECOND = 10_000_000L

    private const val POINTER_SIZE = 8L
    private const val MAX_TILT = 90f

    private val strokeBuilder: ComPtr by lazy {
        Activation.activate(InkInterop.CLS_InkStrokeBuilder, InkInterop.IID_IInkStrokeBuilder)
    }
    private val strokeBuilder3: ComPtr by lazy { strokeBuilder.queryInterface(InkInterop.IID_IInkStrokeBuilder3) }
    private val pointFactory: ComPtr by lazy {
        Activation.factory(InkInterop.CLS_InkPoint, InkInterop.IID_IInkPointFactory2)
    }
    private val attributesStatics: ComPtr by lazy {
        Activation.factory(InkInterop.CLS_InkDrawingAttributes, InkInterop.IID_IInkDrawingAttributesStatics)
    }

    // ------------------------------------------------------------------
    // Drawing attributes
    // ------------------------------------------------------------------

    /** Creates an InkDrawingAttributes (IInkDrawingAttributes) with the same settings as [attributes]. */
    fun createDrawingAttributes(attributes: InkDrawingAttributes): ComPtr {
        val native = if (attributes.kind == InkDrawingAttributesKind.PENCIL) {
            attributesStatics.getPtr(InkInterop.IInkDrawingAttributesStatics_CreateForPencil)
        } else {
            Activation.activate(InkInterop.CLS_InkDrawingAttributes, InkInterop.IID_IInkDrawingAttributes)
        }
        try {
            writeDrawingAttributes(native, attributes)
        } catch (t: Throwable) {
            native.release()
            throw t
        }
        return native
    }

    /**
     * Writes [attributes] to [native]. A pencil InkDrawingAttributes rejects PenTip / PenTipTransform /
     * DrawAsHighlighter with E_INVALIDARG (even re-setting the default value), so these three are not written for a pencil
     * (on the model side, [InkDrawingAttributes.Builder.build] restricts them to their defaults for a pencil).
     */
    private fun writeDrawingAttributes(native: ComPtr, attributes: InkDrawingAttributes) {
        val pencil = attributes.kind == InkDrawingAttributesKind.PENCIL
        val color = attributes.color
        XamlStructs.putColor(native, InkInterop.IInkDrawingAttributes_put_Color, color.alpha, color.red, color.green, color.blue)
        if (!pencil) native.call(InkInterop.IInkDrawingAttributes_put_PenTip, attributes.penTip.native)
        Ffi.backend.withScope { scope ->
            val size = scope.allocate(XamlStructs.SIZE_FLOAT)
            Ffi.backend.memory.putFloat(size.ptr, 0, attributes.width.toFloat())
            Ffi.backend.memory.putFloat(size.ptr, 4, attributes.height.toFloat())
            native.call(InkInterop.IInkDrawingAttributes_put_Size, size)
        }
        native.putBool(InkInterop.IInkDrawingAttributes_put_IgnorePressure, attributes.ignorePressure)
        native.putBool(InkInterop.IInkDrawingAttributes_put_FitToCurve, attributes.fitToCurve)
        if (!pencil) {
            withView(native, InkInterop.IID_IInkDrawingAttributes2) { view ->
                Ffi.backend.withScope { scope ->
                    view.call(InkInterop.IInkDrawingAttributes2_put_PenTipTransform, matrixValue(scope, attributes.penTipTransform))
                }
                view.putBool(InkInterop.IInkDrawingAttributes2_put_DrawAsHighlighter, attributes.drawAsHighlighter)
            }
        }
        withView(native, InkInterop.IID_IInkDrawingAttributes4) { view ->
            view.putBool(InkInterop.IInkDrawingAttributes4_put_IgnoreTilt, attributes.ignoreTilt)
        }
        if (attributes.kind == InkDrawingAttributesKind.PENCIL) {
            withView(native, InkInterop.IID_IInkDrawingAttributes3) { view ->
                val pencil = view.getPtr(InkInterop.IInkDrawingAttributes3_get_PencilProperties)
                try {
                    pencil.call(InkInterop.IInkDrawingAttributesPencilProperties_put_Opacity, attributes.pencilOpacity)
                } finally {
                    pencil.release()
                }
            }
        }
        withView(native, InkInterop.IID_IInkDrawingAttributes5) { view ->
            val modeler = view.getPtr(InkInterop.IInkDrawingAttributes5_get_ModelerAttributes)
            try {
                val settings = attributes.modelerAttributes
                modeler.call(InkInterop.IInkModelerAttributes_put_PredictionTime, durationToTicks(settings.predictionTime))
                modeler.call(InkInterop.IInkModelerAttributes_put_ScalingFactor, settings.scalingFactor)
                withView(modeler, InkInterop.IID_IInkModelerAttributes2) { modeler2 ->
                    modeler2.putBool(
                        InkInterop.IInkModelerAttributes2_put_UseVelocityBasedPressure,
                        settings.useVelocityBasedPressure,
                    )
                }
            } finally {
                modeler.release()
            }
        }
    }

    /** Converts an InkDrawingAttributes (IInkDrawingAttributes) to the model value. */
    fun readDrawingAttributes(native: ComPtr): InkDrawingAttributes {
        val kind = withView(native, InkInterop.IID_IInkDrawingAttributes3) { view ->
            InkDrawingAttributesKind.of(view.getInt(InkInterop.IInkDrawingAttributes3_get_Kind))
        }
        val builder = if (kind == InkDrawingAttributesKind.PENCIL) {
            InkDrawingAttributes.pencilBuilder()
        } else {
            InkDrawingAttributes.builder()
        }
        val argb = XamlStructs.getColor(native, InkInterop.IInkDrawingAttributes_get_Color)
        builder.color(WColor(argb[1], argb[2], argb[3], argb[0]))
        val pencil = kind == InkDrawingAttributesKind.PENCIL
        if (!pencil) builder.penTip(PenTipShape.of(native.getInt(InkInterop.IInkDrawingAttributes_get_PenTip)))
        val size = XamlStructs.getSizeFloat(native, InkInterop.IInkDrawingAttributes_get_Size)
        if (size[0] > 0.0 && size[1] > 0.0) builder.size(size[0], size[1])
        builder.ignorePressure(native.getBool(InkInterop.IInkDrawingAttributes_get_IgnorePressure))
        builder.fitToCurve(native.getBool(InkInterop.IInkDrawingAttributes_get_FitToCurve))
        if (!pencil) {
            withView(native, InkInterop.IID_IInkDrawingAttributes2) { view ->
                builder.penTipTransform(readMatrix(view, InkInterop.IInkDrawingAttributes2_get_PenTipTransform))
                builder.drawAsHighlighter(view.getBool(InkInterop.IInkDrawingAttributes2_get_DrawAsHighlighter))
            }
        }
        withView(native, InkInterop.IID_IInkDrawingAttributes4) { view ->
            builder.ignoreTilt(view.getBool(InkInterop.IInkDrawingAttributes4_get_IgnoreTilt))
        }
        if (pencil) {
            withView(native, InkInterop.IID_IInkDrawingAttributes3) { view ->
                val pencilProperties = view.getPtrOrNull(InkInterop.IInkDrawingAttributes3_get_PencilProperties)
                if (pencilProperties != null) {
                    try {
                        val opacity = pencilProperties.getDouble(InkInterop.IInkDrawingAttributesPencilProperties_get_Opacity)
                        builder.pencilOpacity(opacity.coerceIn(MIN_PENCIL_OPACITY, MAX_PENCIL_OPACITY))
                    } finally {
                        pencilProperties.release()
                    }
                }
            }
        }
        withView(native, InkInterop.IID_IInkDrawingAttributes5) { view ->
            val modeler = view.getPtrOrNull(InkInterop.IInkDrawingAttributes5_get_ModelerAttributes)
            if (modeler != null) {
                try {
                    builder.modelerAttributes(readModelerAttributes(modeler))
                } finally {
                    modeler.release()
                }
            }
        }
        return builder.build()
    }

    private const val MIN_PENCIL_OPACITY = 0.01
    private const val MAX_PENCIL_OPACITY = 5.0

    private fun readModelerAttributes(modeler: ComPtr): InkModelerAttributes {
        val predictionTicks = Ffi.backend.withScope { scope ->
            val out = scope.allocate(8)
            modeler.call(InkInterop.IInkModelerAttributes_get_PredictionTime, out)
            Ffi.backend.memory.getLong(out, 0)
        }
        val scalingFactor = modeler.getFloat(InkInterop.IInkModelerAttributes_get_ScalingFactor)
        val velocity = modeler.queryInterfaceOrNull(InkInterop.IID_IInkModelerAttributes2)?.let { modeler2 ->
            try {
                modeler2.getBool(InkInterop.IInkModelerAttributes2_get_UseVelocityBasedPressure)
            } finally {
                modeler2.release()
            }
        } ?: false
        return InkModelerAttributes(ticksToDuration(predictionTicks), scalingFactor, velocity)
    }

    // ------------------------------------------------------------------
    // Strokes
    // ------------------------------------------------------------------

    /** Creates an InkStroke (IInkStroke) with the same content as [stroke] using InkStrokeBuilder. */
    fun createStroke(stroke: InkStroke): ComPtr {
        val attributes = createDrawingAttributes(stroke.drawingAttributes)
        val points = ArrayList<ComPtr>(stroke.points.size)
        var started: ComPtr? = null
        var duration: ComPtr? = null
        var iterable: ComIterable? = null
        try {
            strokeBuilder.call(InkInterop.IInkStrokeBuilder_SetDefaultDrawingAttributes, attributes)
            stroke.points.mapTo(points) { createPoint(it) }
            iterable = ComIterable(
                "WinUI4K.InkPointIterable",
                InkInterop.IID_IIterable_InkPoint,
                InkInterop.IID_IIterator_InkPoint,
                points.size,
                POINTER_SIZE,
            ) { out, index ->
                points[index].addRef() // pass to out with ownership
                Ffi.backend.memory.putPtr(out, 0, points[index].ptr)
            }
            started = stroke.strokeStartedTime?.let { boxReference(PropertyValues.boxDateTime(instantToTicks(it)), FoundationInterop.IID_IReference_DateTime) }
            duration = stroke.strokeDuration?.let { boxReference(PropertyValues.boxTimeSpan(durationToTicks(it)), FoundationInterop.IID_IReference_TimeSpan) }
            val native = Ffi.backend.withScope { scope ->
                strokeBuilder3.getPtr(
                    InkInterop.IInkStrokeBuilder3_CreateStrokeFromInkPoints,
                    iterable.comObject.primary,
                    matrixValue(scope, stroke.pointTransform),
                    started?.ptr,
                    duration?.ptr,
                )
            }
            // The builder's default attributes are copied at creation, but set them explicitly just in case
            native.call(InkInterop.IInkStroke_put_DrawingAttributes, attributes)
            return native
        } finally {
            iterable?.comObject?.release()
            points.forEach { it.release() }
            started?.release()
            duration?.release()
            attributes.release()
        }
    }

    private fun createPoint(point: InkPoint): ComPtr = Ffi.backend.withScope { scope ->
        pointFactory.getPtr(
            InkInterop.IInkPointFactory2_CreateInkPointWithTiltAndTimestamp,
            pointValue(scope, point.x, point.y),
            point.pressure,
            point.tiltX,
            point.tiltY,
            point.timestamp,
        )
    }

    /** Returns the boxed value [boxed] as an IReference<T> ([referenceIid]) (the box reference is released). */
    private fun boxReference(boxed: ComPtr, referenceIid: String): ComPtr = try {
        boxed.queryInterface(referenceIid)
    } finally {
        boxed.release()
    }

    /** Converts an InkStroke (IInkStroke) to the model value. */
    fun readStroke(native: ComPtr): InkStroke {
        val attributesPtr = native.getPtr(InkInterop.IInkStroke_get_DrawingAttributes)
        val attributes = try {
            readDrawingAttributes(attributesPtr)
        } finally {
            attributesPtr.release()
        }
        val (points, transform) = withView(native, InkInterop.IID_IInkStroke2) { view ->
            val pointsView = view.getPtr(InkInterop.IInkStroke2_GetInkPoints)
            val points = try {
                readVectorView(pointsView).map { point ->
                    try {
                        readPoint(point)
                    } finally {
                        point.release()
                    }
                }
            } finally {
                pointsView.release()
            }
            points to readMatrix(view, InkInterop.IInkStroke2_get_PointTransform)
        }
        val (started, duration) = withView(native, InkInterop.IID_IInkStroke3) { view ->
            val startedBox = view.getPtrOrNull(InkInterop.IInkStroke3_get_StrokeStartedTime)
            val started = startedBox?.let { box ->
                try {
                    PropertyValues.unboxDateTime(box)?.let(::ticksToInstant)
                } finally {
                    box.release()
                }
            }
            val durationBox = view.getPtrOrNull(InkInterop.IInkStroke3_get_StrokeDuration)
            val duration = durationBox?.let { box ->
                try {
                    PropertyValues.unboxTimeSpan(box)?.let(::ticksToDuration)
                } finally {
                    box.release()
                }
            }
            started to duration
        }
        // A stroke without points cannot be represented in the model, so substitute a single point at the top-left
        // of the bounding rectangle (does not normally happen)
        val safePoints = points.ifEmpty { listOf(InkPoint(0.0, 0.0)) }
        return InkStroke(safePoints, attributes, transform, started, duration)
    }

    private fun readPoint(point: ComPtr): InkPoint {
        val position = Xaml.readPoint(point, InkInterop.IInkPoint_get_Position)
        val pressure = point.getFloat(InkInterop.IInkPoint_get_Pressure)
        return withView(point, InkInterop.IID_IInkPoint2) { view ->
            val timestamp = Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                view.call(InkInterop.IInkPoint2_get_Timestamp, out)
                Ffi.backend.memory.getLong(out, 0)
            }
            InkPoint(
                position[0],
                position[1],
                pressure.coerceIn(0f, 1f),
                view.getFloat(InkInterop.IInkPoint2_get_TiltX).coerceIn(-MAX_TILT, MAX_TILT),
                view.getFloat(InkInterop.IInkPoint2_get_TiltY).coerceIn(-MAX_TILT, MAX_TILT),
                timestamp,
            )
        }
    }

    /** Converts InkStroke's GetRenderingSegments to a list of model values. */
    fun readRenderingSegments(native: ComPtr): List<InkStrokeRenderingSegment> {
        val segments = native.getPtr(InkInterop.IInkStroke_GetRenderingSegments)
        try {
            return readVectorView(segments).map { segment ->
                try {
                    val position = Xaml.readPoint(segment, InkInterop.IInkStrokeRenderingSegment_get_Position)
                    val control1 = Xaml.readPoint(segment, InkInterop.IInkStrokeRenderingSegment_get_BezierControlPoint1)
                    val control2 = Xaml.readPoint(segment, InkInterop.IInkStrokeRenderingSegment_get_BezierControlPoint2)
                    InkStrokeRenderingSegment(
                        position[0],
                        position[1],
                        control1[0],
                        control1[1],
                        control2[0],
                        control2[1],
                        segment.getFloat(InkInterop.IInkStrokeRenderingSegment_get_Pressure),
                        segment.getFloat(InkInterop.IInkStrokeRenderingSegment_get_TiltX),
                        segment.getFloat(InkInterop.IInkStrokeRenderingSegment_get_TiltY),
                        segment.getFloat(InkInterop.IInkStrokeRenderingSegment_get_Twist),
                    )
                } finally {
                    segment.release()
                }
            }
        } finally {
            segments.release()
        }
    }

    // ------------------------------------------------------------------
    // Collections, structs, and identity
    // ------------------------------------------------------------------

    /**
     * Returns all elements of an IVectorView<T> (T is a runtime class) as a list of owned references (fetched at once
     * with GetMany).
     */
    fun readVectorView(view: ComPtr): List<ComPtr> {
        val size = view.getInt(FoundationInterop.IVectorView_get_Size)
        if (size == 0) return emptyList()
        return Ffi.backend.withScope { scope ->
            val items = scope.allocate(size * POINTER_SIZE)
            val actual = scope.allocate(4)
            view.call(IVectorView_GetMany, 0, size, items, actual)
            val count = Ffi.backend.memory.getInt(actual, 0)
            List(count) { ComPtr(Ffi.backend.memory.getPtr(items, it * POINTER_SIZE)) }
        }
    }

    /**
     * IVectorView<T>.GetMany(startIndex, capacity, items, out actual) — in the declaration order of
     * FoundationContract.winmd (vtbl[9]).
     */
    private const val IVectorView_GetMany = 9

    /**
     * Creates a Kotlin implementation that passes [items] (runtime class pointers) as IIterable<T>. Release comObject
     * when done.
     */
    fun iterableOf(items: List<ComPtr>, iterableIid: String, iteratorIid: String): ComIterable =
        ComIterable("WinUI4K.ObjectIterable", iterableIid, iteratorIid, items.size, POINTER_SIZE) { out, index ->
            items[index].addRef()
            Ffi.backend.memory.putPtr(out, 0, items[index].ptr)
        }

    /** COM identity (the address of IUnknown). Matches even when the same object is held through different interfaces. */
    fun identityOf(ptr: ComPtr): Long {
        val unknown = ptr.queryInterface(IID_IUnknown)
        return try {
            unknown.ptr.address
        } finally {
            unknown.release()
        }
    }

    /** A Windows.Foundation.Point value for passing by value (valid only within [scope]). */
    fun pointValue(scope: MemoryScope, x: Double, y: Double): StructValue {
        val point = scope.allocate(XamlStructs.POINT_FLOAT)
        Ffi.backend.memory.putFloat(point.ptr, 0, x.toFloat())
        Ffi.backend.memory.putFloat(point.ptr, 4, y.toFloat())
        return point
    }

    /** A Matrix3x2 value for passing by value (valid only within [scope]). */
    fun matrixValue(scope: MemoryScope, transform: InkTransform): StructValue {
        val matrix = scope.allocate(XamlStructs.MATRIX3X2)
        val values = doubleArrayOf(transform.m11, transform.m12, transform.m21, transform.m22, transform.m31, transform.m32)
        values.forEachIndexed { i, value -> Ffi.backend.memory.putFloat(matrix.ptr, i * 4L, value.toFloat()) }
        return matrix
    }

    /** Reads a Matrix3x2 out argument. */
    fun readMatrix(target: ComPtr, slot: Int): InkTransform = Ffi.backend.withScope { scope ->
        val matrix = scope.allocate(XamlStructs.MATRIX3X2)
        target.call(slot, matrix.ptr)
        val m = DoubleArray(6) { Ffi.backend.memory.getFloat(matrix.ptr, it * 4L).toDouble() }
        InkTransform(m[0], m[1], m[2], m[3], m[4], m[5])
    }

    /** Calls a method that returns a Rect in its last out argument with [args], and converts the result to an [InkRect]. */
    fun callForRect(target: ComPtr, slot: Int, vararg args: Any?): InkRect = Ffi.backend.withScope { scope ->
        val rect = scope.allocate(XamlStructs.RECT_FLOAT)
        target.call(slot, *args, rect.ptr)
        readRect(rect.ptr)
    }

    private fun readRect(rect: Ptr): InkRect {
        val memory = Ffi.backend.memory
        return InkRect(
            memory.getFloat(rect, 0).toDouble(),
            memory.getFloat(rect, 4).toDouble(),
            memory.getFloat(rect, 8).toDouble(),
            memory.getFloat(rect, 12).toDouble(),
        )
    }

    /** Runs [block] with a view of [target] QI'd to [iid], then releases the view. */
    inline fun <T> withView(target: ComPtr, iid: String, block: (ComPtr) -> T): T {
        val view = target.queryInterface(iid)
        try {
            return block(view)
        } finally {
            view.release()
        }
    }

    // ------------------------------------------------------------------
    // Time (Windows.Foundation.DateTime / TimeSpan = 100ns units)
    // ------------------------------------------------------------------

    fun instantToTicks(instant: Instant): Long =
        Math.addExact(Math.multiplyExact(instant.epochSecond, TICKS_PER_SECOND), instant.nano / NANOS_PER_TICK) + EPOCH_OFFSET_TICKS

    fun ticksToInstant(ticks: Long): Instant {
        val unixTicks = ticks - EPOCH_OFFSET_TICKS
        return Instant.ofEpochSecond(Math.floorDiv(unixTicks, TICKS_PER_SECOND), Math.floorMod(unixTicks, TICKS_PER_SECOND) * NANOS_PER_TICK)
    }

    fun durationToTicks(duration: Duration): Long =
        Math.addExact(Math.multiplyExact(duration.seconds, TICKS_PER_SECOND), duration.nano / NANOS_PER_TICK)

    fun ticksToDuration(ticks: Long): Duration = Duration.ofNanos(0).plusSeconds(Math.floorDiv(ticks, TICKS_PER_SECOND))
        .plusNanos(Math.floorMod(ticks, TICKS_PER_SECOND) * NANOS_PER_TICK)
}
