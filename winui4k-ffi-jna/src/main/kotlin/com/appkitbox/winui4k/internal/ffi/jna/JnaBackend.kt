package com.appkitbox.winui4k.internal.ffi.jna

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.DowncallHandle
import com.appkitbox.winui4k.internal.ffi.api.FfiBackend
import com.appkitbox.winui4k.internal.ffi.api.FfiBackendProvider
import com.appkitbox.winui4k.internal.ffi.api.MemoryAccess
import com.appkitbox.winui4k.internal.ffi.api.MemoryScope
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.StructValue
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.sun.jna.CallbackProxy
import com.sun.jna.CallbackReference
import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The discovery provider for the JNA backend (registered with ServiceLoader via META-INF/services).
 *
 * Windows x64 only: struct-by-value passing is manually lowered according to the Windows x64 ABI
 * rules (aggregates of size 1/2/4/8 are passed in integer registers, everything else is passed as
 * a pointer to a copy). Arm64 follows different rules (aggregates up to 16B go in general-purpose
 * registers, and an all-double aggregate of 4 or fewer fields is passed as an HFA in floating-point
 * registers), so use the Panama backend there instead.
 */
class JnaBackendProvider : FfiBackendProvider {
    override val name: String = "jna"
    override val priority: Int = 50

    override fun isAvailable(): Boolean =
        runCatching { Class.forName("com.sun.jna.Native") }.isSuccess &&
            System.getProperty("os.name").orEmpty().startsWith("Windows") &&
            System.getProperty("os.arch").orEmpty().lowercase() in setOf("amd64", "x86_64")

    override fun create(): FfiBackend = JnaBackend
}

/**
 * [FfiBackend] implementation backed by JNA (com.sun.jna). Works on Java 8 + Windows x64.
 * This is the only module among the winui4k-related modules allowed to reference com.sun.jna.
 */
internal object JnaBackend : FfiBackend {
    override val name: String = "jna"

    private val libraries = ConcurrentHashMap<String, NativeLibrary>()
    private val handles = ConcurrentHashMap<CallDescriptor, JnaDowncallHandle>()
    private val functions = ConcurrentHashMap<Long, Function>()

    /** JNA's callback-to-function-pointer map is weakly referenced; a strong reference must be kept or it crashes after GC. */
    private val liveStubs = CopyOnWriteArrayList<CallbackProxy>()

    override fun findSymbol(library: String, symbol: String): Ptr {
        // NativeLibrary.getInstance accepts a bare name like "user32.dll" as well as an absolute path
        val lib = libraries.computeIfAbsent(library) { NativeLibrary.getInstance(it) }
        // Equivalent to GetProcAddress; also works for function symbols. Throws UnsatisfiedLinkError if not found
        return Ptr(Pointer.nativeValue(lib.getGlobalVariableAddress(symbol)))
    }

    override fun downcallHandle(descriptor: CallDescriptor): DowncallHandle =
        handles.computeIfAbsent(descriptor) { JnaDowncallHandle(it) }

    override fun upcallStub(descriptor: CallDescriptor, body: (Array<Any?>) -> Any?): Ptr {
        val proxy = object : CallbackProxy {
            override fun getParameterTypes(): Array<Class<*>> =
                Array(descriptor.args.size) { descriptor.args[it].toParameterClass() }

            override fun getReturnType(): Class<*> = descriptor.ret.toReturnClass()

            override fun callback(rawArgs: Array<Any?>): Any? {
                // Scratch space to write back structs of <=8B that arrived in an integer register.
                // Only needs to live for the duration of the body call (same contract as the Panama stack segment)
                newConfinedScope().use { scope ->
                    val converted = Array(rawArgs.size) { i ->
                        raise(descriptor.args[i], rawArgs[i], scope)
                    }
                    val result = body(converted)
                    return when (descriptor.ret) {
                        ValueKind.VOID -> null
                        ValueKind.PTR -> (result as Ptr?)?.let { if (it.isNull) null else Pointer(it.address) }
                        else -> result
                    }
                }
            }
        }
        liveStubs.add(proxy) // kept for the process lifetime (per the SPI contract, never released)
        return Ptr(Pointer.nativeValue(CallbackReference.getFunctionPointer(proxy)))
    }

    override fun newConfinedScope(): MemoryScope = JnaScope()

    override val globalScope: MemoryScope = object : JnaScope() {
        override fun close() = error("globalScope cannot be closed")
    }

    override val memory: MemoryAccess = JnaMemoryAccess

    // ------------------------------------------------------------------
    // Lowering CallDescriptor to a JNA call (Windows x64 ABI)
    // ------------------------------------------------------------------

    private class JnaDowncallHandle(private val descriptor: CallDescriptor) : DowncallHandle {
        private val returnClass = descriptor.ret.toReturnClass()

        override fun invoke(fn: Ptr, vararg args: Any?): Any? {
            require(args.size == descriptor.args.size) {
                "argument count mismatch: expected=${descriptor.args.size}, actual=${args.size}"
            }
            val function = functions.computeIfAbsent(fn.address) { Function.getFunction(Pointer(it)) }
            // the lowered array holds the Memory (struct copy) references alive until the call completes
            val lowered = Array(args.size) { i -> lower(descriptor.args[i], args[i]) }
            val result = function.invoke(returnClass, lowered)
            return when (descriptor.ret) {
                ValueKind.VOID -> null
                ValueKind.PTR -> Ptr(Pointer.nativeValue(result as Pointer?))
                else -> result // Int / Long / Double / Byte / Short
            }
        }

        private fun lower(kind: ArgKind, value: Any?): Any? = when (kind) {
            is ArgKind.Scalar -> when (kind.kind) {
                ValueKind.PTR -> when (value) {
                    null -> null // JNA passes null as a NULL pointer
                    is Ptr -> if (value.isNull) null else Pointer(value.address)
                    else -> error("cannot pass $value as a PTR argument")
                }
                else -> value
            }
            is ArgKind.Struct -> {
                val struct = value as StructValue
                val size = struct.type.byteSize
                val p = Pointer(struct.ptr.address)
                when (size) {
                    // pass the raw bits as an integer regardless of the field types
                    // (under the Win x64 ABI, aggregates go in an integer register slot even if their fields are floating-point)
                    1L -> p.getByte(0)
                    2L -> p.getShort(0)
                    4L -> p.getInt(0)
                    8L -> p.getLong(0)
                    else -> {
                        // the caller makes a temporary copy and passes its address (the callee is allowed to mutate the copy).
                        // passing the original would let the callee's mutation corrupt the caller's memory, so a copy is mandatory
                        val copy = Memory(size)
                        copy.write(0, p.getByteArray(0, size.toInt()), 0, size.toInt())
                        copy
                    }
                }
            }
        }
    }

    /** Upcall parameter types (used by JNA to build the libffi signature). */
    private fun ArgKind.toParameterClass(): Class<*> = when (this) {
        is ArgKind.Scalar -> kind.toReturnClass()
        is ArgKind.Struct -> when (type.byteSize) {
            1L -> java.lang.Byte.TYPE
            2L -> java.lang.Short.TYPE
            4L -> Integer.TYPE
            8L -> java.lang.Long.TYPE
            else -> Pointer::class.java // anything >8B arrives as a pointer to a copy
        }
    }

    private fun ValueKind.toReturnClass(): Class<*> = when (this) {
        ValueKind.VOID -> Void.TYPE
        ValueKind.PTR -> Pointer::class.java
        ValueKind.I32 -> Integer.TYPE
        ValueKind.I64 -> java.lang.Long.TYPE
        ValueKind.F64 -> java.lang.Double.TYPE
        ValueKind.U8 -> java.lang.Byte.TYPE
        ValueKind.U16 -> java.lang.Short.TYPE
    }

    /** Converts an upcall's raw arguments to api types (Ptr / StructValue). */
    private fun raise(kind: ArgKind, value: Any?, scope: MemoryScope): Any? = when (kind) {
        is ArgKind.Scalar ->
            if (kind.kind == ValueKind.PTR) Ptr(Pointer.nativeValue(value as Pointer?)) else value
        is ArgKind.Struct -> {
            val size = kind.type.byteSize
            if (size > 8) {
                // points at the copy the caller made (valid only for the duration of the callback)
                StructValue(kind.type, Ptr(Pointer.nativeValue(value as Pointer?)))
            } else {
                // write the raw bits that arrived as an integer back to memory and wrap as a StructValue
                val ptr = scope.allocate(size, kind.type.alignment)
                val p = Pointer(ptr.address)
                when (size) {
                    1L -> p.setByte(0, value as Byte)
                    2L -> p.setShort(0, value as Short)
                    4L -> p.setInt(0, value as Int)
                    else -> p.setLong(0, value as Long)
                }
                StructValue(kind.type, ptr)
            }
        }
    }

    // ------------------------------------------------------------------
    // Memory
    // ------------------------------------------------------------------

    private open class JnaScope : MemoryScope {
        private val chunks = ArrayList<Memory>()

        override fun allocate(byteSize: Long, alignment: Long): Ptr = synchronized(chunks) {
            val chunk = Memory(maxOf(byteSize, 1L)) // malloc is 16B-aligned on Win x64 (the vocabulary's max alignment is 8)
            val address = Pointer.nativeValue(chunk)
            require(address % alignment == 0L) { "cannot satisfy alignment $alignment: $address" }
            chunks.add(chunk)
            Ptr(address)
        }

        override fun close(): Unit = synchronized(chunks) {
            chunks.forEach(Memory::close)
            chunks.clear()
        }
    }

    private object JnaMemoryAccess : MemoryAccess {
        override fun getByte(p: Ptr, offset: Long): Byte = Pointer(p.address).getByte(offset)
        override fun putByte(p: Ptr, offset: Long, value: Byte) = Pointer(p.address).setByte(offset, value)
        override fun getShort(p: Ptr, offset: Long): Short = Pointer(p.address).getShort(offset)
        override fun putShort(p: Ptr, offset: Long, value: Short) = Pointer(p.address).setShort(offset, value)
        override fun getInt(p: Ptr, offset: Long): Int = Pointer(p.address).getInt(offset)
        override fun putInt(p: Ptr, offset: Long, value: Int) = Pointer(p.address).setInt(offset, value)
        override fun getLong(p: Ptr, offset: Long): Long = Pointer(p.address).getLong(offset)
        override fun putLong(p: Ptr, offset: Long, value: Long) = Pointer(p.address).setLong(offset, value)
        override fun getDouble(p: Ptr, offset: Long): Double = Pointer(p.address).getDouble(offset)
        override fun putDouble(p: Ptr, offset: Long, value: Double) = Pointer(p.address).setDouble(offset, value)

        override fun getPtr(p: Ptr, offset: Long): Ptr =
            Ptr(Pointer.nativeValue(Pointer(p.address).getPointer(offset)))

        override fun putPtr(p: Ptr, offset: Long, value: Ptr) =
            Pointer(p.address).setPointer(offset, if (value.isNull) null else Pointer(value.address))

        // getWideString assumes NUL-termination and depends on the platform's wchar_t size, so it isn't used
        // (JNA's char-array I/O is always fixed at 2 bytes)
        override fun getUtf16(p: Ptr, offset: Long, chars: Int): String =
            String(Pointer(p.address).getCharArray(offset, chars))

        override fun putUtf16z(p: Ptr, offset: Long, value: String) {
            val buffer = CharArray(value.length + 1) // NUL-terminated
            value.toCharArray(buffer, 0, 0, value.length)
            Pointer(p.address).write(offset, buffer, 0, buffer.size)
        }
    }
}
