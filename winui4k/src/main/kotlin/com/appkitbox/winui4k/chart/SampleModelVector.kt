package com.appkitbox.winui4k.chart

import com.appkitbox.winui4k.DateTimeConversions
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.ChartsInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.Date

/**
 * A Kotlin implementation of IObservableVector<Object> (+ IVector<Object> / IIterable<Object>) that passes a
 * [SampleModel] to Charts.Samples.ItemsSource. Each time an element is read, it is taken from the model and boxed
 * (numbers as Double, strings as String, date-times as a DateTime PropertyValue; see [box]).
 *
 * Since the Charts implementation (Microsoft.UI.Xaml.Controls.Charts.dll) subscribes to VectorChanged of
 * IObservableVector<Object> and redraws, change notifications from the model are converted directly into VectorChanged.
 * Changes from the Charts side (SetAt / Append, etc.) are not accepted.
 *
 * The listener registered with the model holds this vector through a weak reference. While the native side (Samples)
 * references it, the KComObject registry keeps the vector alive; once Samples is released, the vector is also
 * collected and the listener is removed (even if the model outlives the chart, it does not drag the native Samples
 * along with it).
 */
internal class SampleModelVector private constructor(private val model: SampleModel<*>) {
    /** VectorChanged subscriptions (token → AddRef'd handler). */
    private val handlers = LinkedHashMap<Long, ComPtr>()
    private var nextToken = 1L

    val comObject: KComObject = KComObject("WinUI4K.ChartSamples")
        .addInterface(
            FoundationInterop.IID_IVector_Object,
            listOf(
                getAtMethod(), // vtbl[6] GetAt(this, UINT32, out IInspectable)
                sizeMethod(), // vtbl[7] get_Size(this, out UINT32)
                // vtbl[8] GetView(this, out IVectorView<Object>)
                KComObject.Method(DESC_PTR) { args ->
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createView().primary)
                    KComObject.S_OK
                },
                indexOfMethod(), // vtbl[9] IndexOf(this, IInspectable, out UINT32, out boolean)
                KComObject.Method(DESC_I32_PTR) { E_NOTIMPL }, // vtbl[10] SetAt
                KComObject.Method(DESC_I32_PTR) { E_NOTIMPL }, // vtbl[11] InsertAt
                KComObject.Method(DESC_I32) { E_NOTIMPL }, // vtbl[12] RemoveAt
                KComObject.Method(DESC_PTR) { E_NOTIMPL }, // vtbl[13] Append
                KComObject.Method(DESC_THIS) { E_NOTIMPL }, // vtbl[14] RemoveAtEnd
                KComObject.Method(DESC_THIS) { E_NOTIMPL }, // vtbl[15] Clear
                getManyMethod(), // vtbl[16] GetMany(this, UINT32 start, UINT32 capacity, IInspectable*, out UINT32)
                KComObject.Method(DESC_I32_PTR) { E_NOTIMPL }, // vtbl[17] ReplaceAll
            ),
        )
        .addInterface(
            FoundationInterop.IID_IIterable_Object,
            listOf(
                // vtbl[6] First(this, out IIterator<Object>)
                KComObject.Method(DESC_PTR) { args ->
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createIterator().primary)
                    KComObject.S_OK
                },
            ),
        )
        .addInterface(
            FoundationInterop.IID_IObservableVector_Object,
            listOf(
                // vtbl[6] add_VectorChanged(this, VectorChangedEventHandler<Object>, out token)
                KComObject.Method(DESC_PTR_PTR) { args ->
                    val handler = ComPtr(args[1] as Ptr)
                    handler.addRef()
                    val token = nextToken++
                    handlers[token] = handler
                    Ffi.backend.memory.putLong(args[2] as Ptr, 0, token)
                    KComObject.S_OK
                },
                // vtbl[7] remove_VectorChanged(this, token)
                KComObject.Method(DESC_I64) { args ->
                    handlers.remove(args[1] as Long)?.release()
                    KComObject.S_OK
                },
            ),
        )

    /** Converts change notifications from the model into VectorChanged. */
    private fun onSamplesChanged(event: SampleModelEvent) {
        val count = event.index1 - event.index0 + 1
        when {
            // Only a single-element change is notified with its position; multi-element changes are combined into one Reset
            // (at notification time the model is already in its changed state, so per-element notifications cannot reproduce the intermediate states)
            count != 1 -> raiseVectorChanged(FoundationInterop.CollectionChange_Reset, 0)
            event.type == SampleModelEvent.INTERVAL_ADDED ->
                raiseVectorChanged(FoundationInterop.CollectionChange_ItemInserted, event.index0)
            event.type == SampleModelEvent.INTERVAL_REMOVED ->
                raiseVectorChanged(FoundationInterop.CollectionChange_ItemRemoved, event.index0)
            else -> raiseVectorChanged(FoundationInterop.CollectionChange_ItemChanged, event.index0)
        }
    }

    private fun raiseVectorChanged(change: Int, index: Int) {
        if (handlers.isEmpty()) return
        val args = KComObject("WinUI4K.VectorChangedEventArgs")
            .addInterface(
                FoundationInterop.IID_IVectorChangedEventArgs,
                listOf(
                    // vtbl[6] get_CollectionChange(this, out CollectionChange)
                    KComObject.Method(DESC_PTR) { a ->
                        Ffi.backend.memory.putInt(a[1] as Ptr, 0, change)
                        KComObject.S_OK
                    },
                    // vtbl[7] get_Index(this, out UINT32)
                    KComObject.Method(DESC_PTR) { a ->
                        Ffi.backend.memory.putInt(a[1] as Ptr, 0, index)
                        KComObject.S_OK
                    },
                ),
            )
        try {
            val sender = comObject.pointerFor(FoundationInterop.IID_IObservableVector_Object)
            for (handler in handlers.values.toList()) {
                // VectorChangedEventHandler.Invoke(this, sender, args) — vtbl[3]
                handler.rawCall(3, DESC_INVOKE, sender, args.primary)
            }
        } finally {
            args.release()
        }
    }

    /** Boxes the [index]-th element and writes it to out (the caller releases it; null is written as NULL). */
    private fun putElement(out: Ptr, offset: Long, index: Int) {
        val boxed = box(model.getElementAt(index))
        Ffi.backend.memory.putPtr(out, offset, boxed?.ptr ?: Ptr.NULL)
    }

    private fun getAtMethod() = KComObject.Method(DESC_I32_PTR) { args ->
        val index = args[1] as Int
        if (index !in 0 until model.getSize()) return@Method E_BOUNDS
        putElement(args[2] as Ptr, 0, index)
        KComObject.S_OK
    }

    private fun sizeMethod() = KComObject.Method(DESC_PTR) { args ->
        Ffi.backend.memory.putInt(args[1] as Ptr, 0, model.getSize())
        KComObject.S_OK
    }

    /** Boxed values are distinct objects on each call, so they cannot be found by identity (always not found). */
    private fun indexOfMethod() = KComObject.Method(DESC_PTR_PTR_PTR) { args ->
        Ffi.backend.memory.putInt(args[2] as Ptr, 0, 0)
        Ffi.backend.memory.putByte(args[3] as Ptr, 0, 0)
        KComObject.S_OK
    }

    private fun getManyMethod() = KComObject.Method(DESC_I32_I32_PTR_PTR) { args ->
        val start = args[1] as Int
        val capacity = args[2] as Int
        val out = args[3] as Ptr
        val size = model.getSize()
        var written = 0
        while (written < capacity && start + written < size) {
            putElement(out, written.toLong() * POINTER_SIZE, start + written)
            written++
        }
        Ffi.backend.memory.putInt(args[4] as Ptr, 0, written)
        KComObject.S_OK
    }

    /** IVectorView<Object> (GetAt=6 get_Size=7 IndexOf=8 GetMany=9). */
    private fun createView(): KComObject = KComObject("WinUI4K.ChartSamplesView").addInterface(
        FoundationInterop.IID_IVectorView_Object,
        listOf(getAtMethod(), sizeMethod(), indexOfMethod(), getManyMethod()),
    )

    /** IIterator<Object>. Each First returns an independent cursor. */
    private fun createIterator(): KComObject {
        var index = 0
        return KComObject("WinUI4K.ChartSamplesIterator").addInterface(
            FoundationInterop.IID_IIterator_Object,
            listOf(
                // vtbl[6] get_Current(this, out IInspectable)
                KComObject.Method(DESC_PTR) { args ->
                    if (index >= model.getSize()) return@Method E_BOUNDS
                    putElement(args[1] as Ptr, 0, index)
                    KComObject.S_OK
                },
                // vtbl[7] get_HasCurrent(this, out boolean)
                KComObject.Method(DESC_PTR) { args ->
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < model.getSize()) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[8] MoveNext(this, out boolean)
                KComObject.Method(DESC_PTR) { args ->
                    index++
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < model.getSize()) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] GetMany(this, UINT32 capacity, IInspectable* items, out UINT32 actual)
                KComObject.Method(DESC_I32_PTR_PTR) { args ->
                    val capacity = args[1] as Int
                    val out = args[2] as Ptr
                    var written = 0
                    while (written < capacity && index < model.getSize()) {
                        putElement(out, written.toLong() * POINTER_SIZE, index)
                        written++
                        index++
                    }
                    Ffi.backend.memory.putInt(args[3] as Ptr, 0, written)
                    KComObject.S_OK
                },
            ),
        )
    }

    /** The listener registered with the model. Holds the vector through a weak reference and removes itself once the vector is collected. */
    private class WeakListener(vector: SampleModelVector) : SampleModelListener {
        private val vector = WeakReference(vector)

        override fun samplesChanged(event: SampleModelEvent) {
            val target = vector.get()
            if (target == null) {
                event.getSource().removeSampleModelListener(this)
                return
            }
            target.onSamplesChanged(event)
        }
    }

    companion object {
        /**
         * Creates a Charts.Samples whose ItemsSource is [model] and returns its ISamples (the caller takes ownership).
         * The lifetime of the ItemsSource vector is left to the reference held by Samples.
         */
        fun createSamples(model: SampleModel<*>): ComPtr {
            val vector = SampleModelVector(model)
            model.addSampleModelListener(WeakListener(vector))
            val samples = Activation.activate(ChartsInterop.CLS_Samples, ChartsInterop.IID_ISamples)
            try {
                samples.call(ChartsInterop.ISamples_put_ItemsSource, vector.comObject.primary)
            } finally {
                // Release the reference from creation (from now on it lives only on the reference held by Samples)
                vector.comObject.release()
            }
            return samples
        }

        /**
         * Boxes a model element into an IInspectable (the caller releases it; null stays null).
         * Numbers become Double, date-times (java.time / java.util.Date) become DateTime, and anything else becomes a
         * string (toString). Since Charts handles date-times in UTC (tick boundaries and labels use UTC dates),
         * LocalDate / LocalDateTime, which have no time zone, are passed as UTC date-times so that the specified date and
         * time are shown on the axis as is.
         */
        fun box(value: Any?): ComPtr? = when (value) {
            null -> null
            is Number -> PropertyValues.boxDouble(value.toDouble())
            is String -> PropertyValues.boxString(value)
            is Instant -> PropertyValues.boxDateTime(DateTimeConversions.instantToTicks(value))
            is LocalDateTime -> PropertyValues.boxDateTime(DateTimeConversions.utcDateTimeToTicks(value))
            is LocalDate -> PropertyValues.boxDateTime(DateTimeConversions.utcDateTimeToTicks(value.atStartOfDay()))
            is OffsetDateTime -> PropertyValues.boxDateTime(DateTimeConversions.instantToTicks(value.toInstant()))
            is ZonedDateTime -> PropertyValues.boxDateTime(DateTimeConversions.instantToTicks(value.toInstant()))
            is Date -> PropertyValues.boxDateTime(DateTimeConversions.instantToTicks(value.toInstant()))
            else -> PropertyValues.boxString(value.toString())
        }

        private const val POINTER_SIZE = 8L
        private val E_BOUNDS = 0x8000000B.toInt()
        private val E_NOTIMPL = 0x80004001.toInt()
    }
}

private val DESC_THIS = CallDescriptor(ValueKind.I32, ArgKind.PTR)
private val DESC_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
private val DESC_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
private val DESC_PTR_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
private val DESC_I32 = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32)
private val DESC_I32_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR)
private val DESC_I32_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
private val DESC_I32_I32_PTR_PTR =
    CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
private val DESC_I64 = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I64)

/** delegate.Invoke(this, sender, args) — vtbl[3]. */
private val DESC_INVOKE = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
