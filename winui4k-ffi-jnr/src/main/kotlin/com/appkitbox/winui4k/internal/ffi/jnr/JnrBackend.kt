package com.appkitbox.winui4k.internal.ffi.jnr

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
import com.kenai.jffi.CallContext
import com.kenai.jffi.CallingConvention
import com.kenai.jffi.Closure
import com.kenai.jffi.ClosureManager
import com.kenai.jffi.HeapInvocationBuffer
import com.kenai.jffi.Invoker
import com.kenai.jffi.Library
import com.kenai.jffi.MemoryIO
import com.kenai.jffi.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** malloc / free and direct address reads/writes (Unsafe-based). jffi's actual implementation is libffi. */
private val io: MemoryIO = MemoryIO.getInstance()

/**
 * The JNR backend's discovery provider (registered with ServiceLoader via META-INF/services).
 *
 * Unlike JNA, it doesn't manually lower struct-by-value passing (libffi resolves the ABI),
 * so it works on all of Windows x64 / ARM64 / x86, wherever jffi bundles a native library.
 */
class JnrBackendProvider : FfiBackendProvider {
    override val name: String = "jnr"

    /** Lower than the battle-tested JNA (50), so JNA stays the Java 8 default. */
    override val priority: Int = 40

    override fun isAvailable(): Boolean =
        runCatching { Class.forName("com.kenai.jffi.Invoker") }.isSuccess &&
            System.getProperty("os.name").orEmpty().startsWith("Windows")

    override fun create(): FfiBackend = JnrBackend
}

/**
 * An [FfiBackend] implementation built on jffi (com.kenai.jffi), the JNR project's low-level
 * layer. Works on Java 8. This module is the only place in winui4k-related modules allowed to
 * reference JNR (jnr-ffi / jffi).
 *
 * jnr-ffi's high-level API (LibraryLoader) statically binds to interface declarations, so it
 * can't be used for the dynamic invocation of a runtime-determined function pointer +
 * [CallDescriptor], as with a COM vtable. This uses jffi, which jnr-ffi itself uses internally,
 * directly instead.
 */
internal object JnrBackend : FfiBackend {
    override val name: String = "jnr"

    private val libraries = ConcurrentHashMap<String, Library>()
    private val handles = ConcurrentHashMap<CallDescriptor, JnrDowncallHandle>()
    private val structTypes = ConcurrentHashMap<StructType, com.kenai.jffi.Struct>()

    /** Strong references to closure handles. With autoRelease off, this also keeps them safe from the GC. */
    private val liveClosures = CopyOnWriteArrayList<Closure.Handle>()

    override fun findSymbol(library: String, symbol: String): Ptr {
        // getCachedInstance accepts a bare name like "user32.dll" as well as an absolute path (like LoadLibrary)
        val lib = libraries.computeIfAbsent(library) {
            Library.getCachedInstance(it, Library.LAZY or Library.GLOBAL)
                ?: throw UnsatisfiedLinkError("cannot load library: $it (${Library.getLastError()})")
        }
        // Equivalent to GetProcAddress. Returns 0 if not found
        val address = lib.getSymbolAddress(symbol)
        if (address == 0L) throw UnsatisfiedLinkError("symbol not found: $symbol ($library)")
        return Ptr(address)
    }

    override fun downcallHandle(descriptor: CallDescriptor): DowncallHandle =
        handles.computeIfAbsent(descriptor) { JnrDowncallHandle(it) }

    override fun upcallStub(descriptor: CallDescriptor, body: (Array<Any?>) -> Any?): Ptr {
        val closure = Closure { buffer ->
            val converted = Array(descriptor.args.size) { i -> raise(descriptor.args[i], buffer, i) }
            val result = body(converted)
            when (descriptor.ret) {
                ValueKind.VOID -> Unit
                ValueKind.PTR -> buffer.setAddressReturn((result as Ptr?)?.address ?: 0L)
                ValueKind.I32 -> buffer.setIntReturn(result as Int)
                ValueKind.I64 -> buffer.setLongReturn(result as Long)
                ValueKind.F64 -> buffer.setDoubleReturn(result as Double)
                ValueKind.U8 -> buffer.setByteReturn(result as Byte)
                ValueKind.U16 -> buffer.setShortReturn(result as Short)
            }
        }
        val handle = ClosureManager.getInstance().newClosure(closure, descriptor.toCallContext())
        handle.setAutoRelease(false) // held for the process lifetime, per the SPI contract of never releasing it
        liveClosures.add(handle)
        return Ptr(handle.address)
    }

    /** Converts an argument arriving via an upcall into an api type (Ptr / StructValue). */
    private fun raise(kind: ArgKind, buffer: Closure.Buffer, index: Int): Any? = when (kind) {
        is ArgKind.Scalar -> when (kind.kind) {
            ValueKind.PTR -> Ptr(buffer.getAddress(index))
            ValueKind.I32 -> buffer.getInt(index)
            ValueKind.I64 -> buffer.getLong(index)
            ValueKind.F64 -> buffer.getDouble(index)
            ValueKind.U8 -> buffer.getByte(index)
            ValueKind.U16 -> buffer.getShort(index)
            ValueKind.VOID -> error("a VOID argument can't exist")
        }
        // libffi always passes struct arguments as a pointer to the data regardless of size
        // (valid only while the callback is running, the same contract as the Panama version's stack segment)
        is ArgKind.Struct -> StructValue(kind.type, Ptr(buffer.getStruct(index)))
    }

    override fun newConfinedScope(): MemoryScope = JnrScope()

    override val globalScope: MemoryScope = object : JnrScope() {
        override fun close() = error("globalScope can't be closed")
    }

    override val memory: MemoryAccess = JnrMemoryAccess

    // ------------------------------------------------------------------
    // Converting a CallDescriptor into a jffi call (ABI lowering is left to libffi)
    // ------------------------------------------------------------------

    private class JnrDowncallHandle(private val descriptor: CallDescriptor) : DowncallHandle {
        private val context = descriptor.toCallContext()

        override fun invoke(fn: Ptr, vararg args: Any?): Any? {
            require(args.size == descriptor.args.size) {
                "argument count mismatch: expected=${descriptor.args.size}, actual=${args.size}"
            }
            val buffer = HeapInvocationBuffer(context)
            descriptor.args.forEachIndexed { i, kind -> put(buffer, kind, args[i]) }
            val invoker = Invoker.getInstance()
            return when (descriptor.ret) {
                ValueKind.VOID -> {
                    invoker.invokeInt(context, fn.address, buffer)
                    null
                }
                ValueKind.PTR -> Ptr(invoker.invokeAddress(context, fn.address, buffer))
                ValueKind.I32 -> invoker.invokeInt(context, fn.address, buffer)
                ValueKind.I64 -> invoker.invokeLong(context, fn.address, buffer)
                ValueKind.F64 -> invoker.invokeDouble(context, fn.address, buffer)
                ValueKind.U8 -> invoker.invokeInt(context, fn.address, buffer).toByte()
                ValueKind.U16 -> invoker.invokeInt(context, fn.address, buffer).toShort()
            }
        }

        private fun put(buffer: HeapInvocationBuffer, kind: ArgKind, value: Any?) {
            when (kind) {
                is ArgKind.Scalar -> when (kind.kind) {
                    ValueKind.PTR -> buffer.putAddress(
                        when (value) {
                            null -> 0L // pass null as a NULL pointer
                            is Ptr -> value.address
                            else -> error("can't pass $value as a PTR argument")
                        },
                    )
                    ValueKind.I32 -> buffer.putInt(value as Int)
                    ValueKind.I64 -> buffer.putLong(value as Long)
                    ValueKind.F64 -> buffer.putDouble(value as Double)
                    ValueKind.U8 -> buffer.putByte((value as Byte).toInt())
                    ValueKind.U16 -> buffer.putShort((value as Short).toInt())
                    ValueKind.VOID -> error("a VOID argument can't exist")
                }
                is ArgKind.Struct -> {
                    // Copy the raw bits into the buffer to pass by value (the callee may mutate the
                    // copy without corrupting the original). libffi decides whether it goes in
                    // registers or via a pointer
                    val struct = value as StructValue
                    val size = struct.type.byteSize.toInt()
                    val bytes = ByteArray(size)
                    io.getByteArray(struct.ptr.address, bytes, 0, size)
                    buffer.putStruct(bytes, 0)
                }
            }
        }
    }

    private fun CallDescriptor.toCallContext(): CallContext = CallContext.getCallContext(
        ret.toFfiType(),
        Array(args.size) { args[it].toFfiType() },
        CallingConvention.DEFAULT,
        false, // no need to save errno (Win32's GetLastError isn't used)
    )

    private fun ArgKind.toFfiType(): Type = when (this) {
        is ArgKind.Scalar -> kind.toFfiType()
        // Fields are laid out with natural alignment (an assumption of StructType), so this matches libffi's computation
        is ArgKind.Struct -> structTypes.computeIfAbsent(type) { t ->
            com.kenai.jffi.Struct.newStruct(*Array(t.fields.size) { t.fields[it].kind.toFfiType() })
        }
    }

    private fun ValueKind.toFfiType(): Type = when (this) {
        ValueKind.VOID -> Type.VOID
        ValueKind.PTR -> Type.POINTER
        ValueKind.I32 -> Type.SINT32
        ValueKind.I64 -> Type.SINT64
        ValueKind.F64 -> Type.DOUBLE
        ValueKind.U8 -> Type.UINT8
        ValueKind.U16 -> Type.UINT16
    }

    // ------------------------------------------------------------------
    // Memory
    // ------------------------------------------------------------------

    private open class JnrScope : MemoryScope {
        private val chunks = ArrayList<Long>()

        override fun allocate(byteSize: Long, alignment: Long): Ptr = synchronized(chunks) {
            // Equivalent to malloc (16B-aligned on Win x64; the vocabulary's max is 8). Zero-initialized to match Panama
            val address = io.allocateMemory(maxOf(byteSize, 1L), true)
            check(address != 0L) { "couldn't allocate native memory: $byteSize bytes" }
            require(address % alignment == 0L) { "couldn't satisfy alignment $alignment: $address" }
            chunks.add(address)
            Ptr(address)
        }

        override fun close(): Unit = synchronized(chunks) {
            chunks.forEach(io::freeMemory)
            chunks.clear()
        }
    }

    private object JnrMemoryAccess : MemoryAccess {
        override fun getByte(p: Ptr, offset: Long): Byte = io.getByte(p.address + offset)
        override fun putByte(p: Ptr, offset: Long, value: Byte) = io.putByte(p.address + offset, value)
        override fun getShort(p: Ptr, offset: Long): Short = io.getShort(p.address + offset)
        override fun putShort(p: Ptr, offset: Long, value: Short) = io.putShort(p.address + offset, value)
        override fun getInt(p: Ptr, offset: Long): Int = io.getInt(p.address + offset)
        override fun putInt(p: Ptr, offset: Long, value: Int) = io.putInt(p.address + offset, value)
        override fun getLong(p: Ptr, offset: Long): Long = io.getLong(p.address + offset)
        override fun putLong(p: Ptr, offset: Long, value: Long) = io.putLong(p.address + offset, value)
        override fun getDouble(p: Ptr, offset: Long): Double = io.getDouble(p.address + offset)
        override fun putDouble(p: Ptr, offset: Long, value: Double) = io.putDouble(p.address + offset, value)
        override fun getPtr(p: Ptr, offset: Long): Ptr = Ptr(io.getAddress(p.address + offset))
        override fun putPtr(p: Ptr, offset: Long, value: Ptr) = io.putAddress(p.address + offset, value.address)

        // Char array I/O is always fixed at 2 bytes (independent of wchar_t's size), so it's usable directly for UTF-16
        override fun getUtf16(p: Ptr, offset: Long, chars: Int): String {
            val buffer = CharArray(chars)
            io.getCharArray(p.address + offset, buffer, 0, chars)
            return String(buffer)
        }

        override fun putUtf16z(p: Ptr, offset: Long, value: String) {
            val buffer = CharArray(value.length + 1) // NUL-terminated
            value.toCharArray(buffer, 0, 0, value.length)
            io.putCharArray(p.address + offset, buffer, 0, buffer.size)
        }
    }
}
