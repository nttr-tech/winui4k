package jp.hisano.winui4k.com

import jp.hisano.winui4k.ffi.api.ArgKind
import jp.hisano.winui4k.ffi.api.CallDescriptor
import jp.hisano.winui4k.ffi.api.Ffi
import jp.hisano.winui4k.ffi.api.Ptr
import jp.hisano.winui4k.ffi.api.StructValue
import jp.hisano.winui4k.ffi.api.ValueKind
import jp.hisano.winui4k.ffi.api.withScope

/**
 * Wrapper around a COM interface pointer.
 *
 * A COM object is a pointer to a struct that starts with a C++ virtual function
 * table (vtable). We follow `ptr -> vtable -> vtable[slot]` to obtain the function
 * pointer and invoke it via the FFI backend's downcall handle. Slot numbers are
 * determined by the method declaration order in the winmd (IUnknown occupies 0..2,
 * IInspectable-derived slots occupy 3..5, and the interface body starts at 6).
 */
internal class ComPtr(val ptr: Ptr) {

    init {
        require(!ptr.isNull) { "null COM pointer" }
    }

    /** Retrieves the [slot]-th function pointer from the vtable. */
    fun vtblEntry(slot: Int): Ptr {
        val memory = Ffi.backend.memory
        val vtbl = memory.getPtr(ptr, 0)
        return memory.getPtr(vtbl, slot.toLong() * 8)
    }

    /** Calls a vtable slot with an explicit descriptor, returning the raw HRESULT / ULONG. */
    fun rawCall(slot: Int, descriptor: CallDescriptor, vararg args: Any?): Int {
        val handle = Ffi.backend.downcallHandle(descriptor)
        val all = arrayOfNulls<Any?>(args.size + 1)
        all[0] = ptr
        for (i in args.indices) {
            val a = args[i]
            all[i + 1] = if (a is ComPtr) a.ptr else a // a ComPtr can be passed unwrapped
        }
        return handle.invoke(vtblEntry(slot), *all) as Int
    }

    /**
     * Calls a method that returns an HRESULT, throwing on failure. The descriptor is
     * inferred automatically from the Kotlin types of the arguments
     * (Ptr/ComPtr/null -> pointer, Int -> I32, Long -> I64, Double -> F64, StructValue -> struct by value).
     */
    fun call(slot: Int, vararg args: Any?) {
        checkHr(rawCall(slot, inferDescriptor(args), *args), "vtbl[$slot]")
    }

    /** For cases where the signature must be explicit, e.g. a struct-by-value return. */
    fun callWith(slot: Int, descriptor: CallDescriptor, vararg args: Any?) {
        checkHr(rawCall(slot, descriptor, *args), "vtbl[$slot]")
    }

    /** `HRESULT f(..., T** out)` pattern: receives the output pointer and wraps it as a ComPtr. */
    fun getPtr(slot: Int, vararg args: Any?): ComPtr = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        call(slot, *args, out)
        ComPtr(Ffi.backend.memory.getPtr(out, 0))
    }

    /** Null-tolerant version of [getPtr], for properties whose output can be a null pointer (e.g. Content). */
    fun getPtrOrNull(slot: Int, vararg args: Any?): ComPtr? = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        call(slot, *args, out)
        val p = Ffi.backend.memory.getPtr(out, 0)
        if (p.isNull) null else ComPtr(p)
    }

    /** `HRESULT f(boolean* out)` pattern. WinRT's boolean is 1 byte. */
    fun getBool(slot: Int): Boolean = Ffi.backend.withScope { scope ->
        val out = scope.allocate(1, 1)
        call(slot, out)
        Ffi.backend.memory.getByte(out, 0).toInt() != 0
    }

    /** `HRESULT f(boolean value)` pattern. */
    fun putBool(slot: Int, value: Boolean) {
        callWith(
            slot,
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.U8),
            if (value) 1.toByte() else 0.toByte(),
        )
    }

    /** `HRESULT f(INT32* out)` pattern (e.g. an enum getter). */
    fun getInt(slot: Int): Int = Ffi.backend.withScope { scope ->
        val out = scope.allocate(4)
        call(slot, out)
        Ffi.backend.memory.getInt(out, 0)
    }

    /** `HRESULT f(DOUBLE* out)` pattern (e.g. FontSize's getter). */
    fun getDouble(slot: Int): Double = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        call(slot, out)
        Ffi.backend.memory.getDouble(out, 0)
    }

    fun queryInterface(iid: String): ComPtr = getPtr(0, Guid.of(iid)) // IUnknown::QueryInterface

    /** Null-tolerant version of [queryInterface]. Returns null if the interface isn't implemented. */
    fun queryInterfaceOrNull(iid: String): ComPtr? = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        val hr = rawCall(0, QI_DESC, Guid.of(iid), out)
        if (hr != 0) null else ComPtr(Ffi.backend.memory.getPtr(out, 0))
    }

    fun addRef() {
        rawCall(1, UNKNOWN_DESC) // IUnknown::AddRef (return value is the reference count)
    }

    fun release() {
        rawCall(2, UNKNOWN_DESC) // IUnknown::Release
    }

    companion object {
        /** ULONG f(this) — AddRef / Release */
        val UNKNOWN_DESC: CallDescriptor = CallDescriptor(ValueKind.I32, ArgKind.PTR)

        /** HRESULT QueryInterface(this, riid, ppv) */
        private val QI_DESC = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

        private fun inferDescriptor(args: Array<out Any?>): CallDescriptor {
            val kinds = buildList(args.size + 1) {
                add(ArgKind.PTR) // the implicit this
                args.forEach {
                    add(
                        when (it) {
                            null, is Ptr, is ComPtr -> ArgKind.PTR
                            is Int -> ArgKind.I32
                            is Long -> ArgKind.I64
                            is Double -> ArgKind.F64
                            is StructValue -> ArgKind.Struct(it.type)
                            else -> error("unsupported argument for auto descriptor: $it")
                        },
                    )
                }
            }
            return CallDescriptor(ValueKind.I32, kinds)
        }
    }
}
