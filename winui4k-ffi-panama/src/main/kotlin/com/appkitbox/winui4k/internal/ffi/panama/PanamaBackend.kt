package com.appkitbox.winui4k.internal.ffi.panama

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.DowncallHandle
import com.appkitbox.winui4k.internal.ffi.api.FfiBackend
import com.appkitbox.winui4k.internal.ffi.api.FfiBackendProvider
import com.appkitbox.winui4k.internal.ffi.api.MemoryAccess
import com.appkitbox.winui4k.internal.ffi.api.MemoryScope
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.StructType
import com.appkitbox.winui4k.internal.ffi.api.StructValue
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.ADDRESS_UNALIGNED
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_CHAR_UNALIGNED
import java.lang.foreign.ValueLayout.JAVA_DOUBLE_UNALIGNED
import java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED
import java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED
import java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap

/** The discovery provider for the Panama backend (registered with ServiceLoader via META-INF/services). */
class PanamaBackendProvider : FfiBackendProvider {
    override val name: String = "panama"
    override val priority: Int = 100
    override fun isAvailable(): Boolean =
        runCatching { Class.forName("java.lang.foreign.Linker") }.isSuccess

    override fun create(): FfiBackend = PanamaBackend
}

/**
 * [FfiBackend] implementation backed by Panama (java.lang.foreign).
 * This is the only module among the winui4k-related modules allowed to reference java.lang.foreign.
 */
internal object PanamaBackend : FfiBackend {
    override val name: String = "panama"

    private val linker: Linker = Linker.nativeLinker()

    /** An arena that lives for the process lifetime. Holds vtables, upcall stubs, cached GUIDs, etc. */
    private val arena: Arena = Arena.ofShared()

    private val lookups = ConcurrentHashMap<String, SymbolLookup>()
    private val handles = ConcurrentHashMap<CallDescriptor, PanamaDowncallHandle>()

    override fun findSymbol(library: String, symbol: String): Ptr {
        val lookup = lookups.computeIfAbsent(library) { SymbolLookup.libraryLookup(it, arena) }
        val segment = lookup.find(symbol)
            .orElseThrow { UnsatisfiedLinkError("symbol not found: $symbol ($library)") }
        return Ptr(segment.address())
    }

    override fun downcallHandle(descriptor: CallDescriptor): DowncallHandle =
        handles.computeIfAbsent(descriptor) {
            PanamaDowncallHandle(linker.downcallHandle(it.toFunctionDescriptor()), it)
        }

    override fun upcallStub(descriptor: CallDescriptor, body: (Array<Any?>) -> Any?): Ptr {
        val functionDescriptor = descriptor.toFunctionDescriptor()
        val invoker = UpcallInvoker(descriptor, body)
        var handle = LOOKUP.findVirtual(
            UpcallInvoker::class.java,
            "invoke",
            MethodType.methodType(Any::class.java, Array<Any?>::class.java),
        ).bindTo(invoker)
        handle = handle.asCollector(Array<Any?>::class.java, descriptor.args.size)
        handle = handle.asType(functionDescriptor.toMethodType())
        return Ptr(linker.upcallStub(handle, functionDescriptor, arena).address())
    }

    override fun newConfinedScope(): MemoryScope = PanamaScope(Arena.ofConfined())

    override val globalScope: MemoryScope = PanamaScope(arena)

    override val memory: MemoryAccess = PanamaMemoryAccess

    // ------------------------------------------------------------------
    // Converting CallDescriptor / StructType to Panama layouts
    // ------------------------------------------------------------------

    private val structLayouts = ConcurrentHashMap<StructType, MemoryLayout>()

    private fun CallDescriptor.toFunctionDescriptor(): FunctionDescriptor {
        val argLayouts = args.map { it.toLayout() }.toTypedArray()
        return if (ret == ValueKind.VOID) {
            FunctionDescriptor.ofVoid(*argLayouts)
        } else {
            FunctionDescriptor.of(ret.toScalarLayout(), *argLayouts)
        }
    }

    private fun ArgKind.toLayout(): MemoryLayout = when (this) {
        is ArgKind.Scalar -> kind.toScalarLayout()
        is ArgKind.Struct -> structLayouts.computeIfAbsent(type) { it.toStructLayout() }
    }

    private fun ValueKind.toScalarLayout(): ValueLayout = when (this) {
        ValueKind.PTR -> ADDRESS
        ValueKind.I32 -> ValueLayout.JAVA_INT
        ValueKind.I64 -> ValueLayout.JAVA_LONG
        ValueKind.F64 -> ValueLayout.JAVA_DOUBLE
        ValueKind.U8 -> JAVA_BYTE
        ValueKind.U16 -> ValueLayout.JAVA_SHORT
        ValueKind.VOID -> error("VOID cannot become a scalar layout")
    }

    /** Inserts padding to match StructType's offset calculation (natural alignment). */
    private fun StructType.toStructLayout(): MemoryLayout {
        val elements = buildList {
            var offset = 0L
            fields.forEachIndexed { i, field ->
                if (offsets[i] > offset) add(MemoryLayout.paddingLayout(offsets[i] - offset))
                add(field.kind.toScalarLayout().withName(field.name))
                offset = offsets[i] + field.kind.byteSize
            }
            if (byteSize > offset) add(MemoryLayout.paddingLayout(byteSize - offset))
        }
        return MemoryLayout.structLayout(*elements.toTypedArray())
    }

    private class PanamaDowncallHandle(
        private val handle: java.lang.invoke.MethodHandle,
        private val descriptor: CallDescriptor,
    ) : DowncallHandle {
        override fun invoke(fn: Ptr, vararg args: Any?): Any? {
            require(args.size == descriptor.args.size) {
                "argument count mismatch: expected=${descriptor.args.size}, actual=${args.size}"
            }
            val lowered = ArrayList<Any?>(args.size + 1)
            lowered.add(MemorySegment.ofAddress(fn.address))
            descriptor.args.forEachIndexed { i, kind -> lowered.add(lower(kind, args[i])) }
            val result = handle.invokeWithArguments(lowered)
            return when (descriptor.ret) {
                ValueKind.VOID -> null
                ValueKind.PTR -> Ptr((result as MemorySegment).address())
                else -> result
            }
        }

        private fun lower(kind: ArgKind, value: Any?): Any? = when (kind) {
            is ArgKind.Scalar -> when (kind.kind) {
                ValueKind.PTR -> when (value) {
                    null -> MemorySegment.NULL
                    is Ptr -> MemorySegment.ofAddress(value.address)
                    else -> error("cannot pass $value as a PTR argument")
                }
                else -> value
            }
            is ArgKind.Struct -> {
                val struct = value as StructValue
                MemorySegment.ofAddress(struct.ptr.address).reinterpret(struct.type.byteSize)
            }
        }
    }

    /** Converts an upcall's raw arguments (MemorySegment etc.) to api types (Ptr / StructValue) before passing them to body. */
    private class UpcallInvoker(
        private val descriptor: CallDescriptor,
        private val body: (Array<Any?>) -> Any?,
    ) {
        @Suppress("unused") // invoked via MethodHandles
        fun invoke(args: Array<Any?>): Any? {
            val converted = Array(args.size) { i ->
                when (val kind = descriptor.args[i]) {
                    is ArgKind.Scalar ->
                        if (kind.kind == ValueKind.PTR) Ptr((args[i] as MemorySegment).address()) else args[i]
                    is ArgKind.Struct -> StructValue(kind.type, Ptr((args[i] as MemorySegment).address()))
                }
            }
            return body(converted)
        }
    }

    private class PanamaScope(private val arena: Arena) : MemoryScope {
        override fun allocate(byteSize: Long, alignment: Long): Ptr =
            Ptr(arena.allocate(byteSize, alignment).address())

        override fun close() = arena.close()
    }

    private object PanamaMemoryAccess : MemoryAccess {
        private fun segment(p: Ptr, byteSize: Long): MemorySegment =
            MemorySegment.ofAddress(p.address).reinterpret(byteSize)

        override fun getByte(p: Ptr, offset: Long): Byte = segment(p, offset + 1).get(JAVA_BYTE, offset)
        override fun putByte(p: Ptr, offset: Long, value: Byte) = segment(p, offset + 1).set(JAVA_BYTE, offset, value)
        override fun getShort(p: Ptr, offset: Long): Short = segment(p, offset + 2).get(JAVA_SHORT_UNALIGNED, offset)
        override fun putShort(p: Ptr, offset: Long, value: Short) = segment(p, offset + 2).set(JAVA_SHORT_UNALIGNED, offset, value)
        override fun getInt(p: Ptr, offset: Long): Int = segment(p, offset + 4).get(JAVA_INT_UNALIGNED, offset)
        override fun putInt(p: Ptr, offset: Long, value: Int) = segment(p, offset + 4).set(JAVA_INT_UNALIGNED, offset, value)
        override fun getLong(p: Ptr, offset: Long): Long = segment(p, offset + 8).get(JAVA_LONG_UNALIGNED, offset)
        override fun putLong(p: Ptr, offset: Long, value: Long) = segment(p, offset + 8).set(JAVA_LONG_UNALIGNED, offset, value)
        override fun getDouble(p: Ptr, offset: Long): Double = segment(p, offset + 8).get(JAVA_DOUBLE_UNALIGNED, offset)
        override fun putDouble(p: Ptr, offset: Long, value: Double) = segment(p, offset + 8).set(JAVA_DOUBLE_UNALIGNED, offset, value)
        override fun getPtr(p: Ptr, offset: Long): Ptr =
            Ptr(segment(p, offset + 8).get(ADDRESS_UNALIGNED, offset).address())

        override fun putPtr(p: Ptr, offset: Long, value: Ptr) =
            segment(p, offset + 8).set(ADDRESS_UNALIGNED, offset, MemorySegment.ofAddress(value.address))

        override fun getUtf16(p: Ptr, offset: Long, chars: Int): String {
            val source = segment(p, offset + chars.toLong() * 2)
            return String(CharArray(chars) { source.get(JAVA_CHAR_UNALIGNED, offset + it.toLong() * 2) })
        }

        override fun putUtf16z(p: Ptr, offset: Long, value: String) {
            val target = segment(p, offset + (value.length + 1).toLong() * 2)
            for (i in value.indices) target.set(JAVA_CHAR_UNALIGNED, offset + i.toLong() * 2, value[i])
            target.set(JAVA_CHAR_UNALIGNED, offset + value.length.toLong() * 2, 0.toChar()) // NUL terminator
        }
    }

    private val LOOKUP = MethodHandles.lookup()
}
