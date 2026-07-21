package jp.hisano.winui4k.ffi

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_DOUBLE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.lang.invoke.MethodHandle
import java.util.concurrent.ConcurrentHashMap

/**
 * Wrapper around a COM interface pointer.
 *
 * A WinRT object is a pointer to a struct that starts with a C++ virtual function
 * table (vtable). We follow `ptr -> vtable -> vtable[slot]` to obtain the function
 * pointer and invoke it via a Panama downcall handle. Slot numbers are determined by
 * the method declaration order in the winmd (IUnknown occupies 0..2, IInspectable-derived
 * slots occupy 3..5, and the interface body starts at 6).
 */
class ComPtr(val ptr: MemorySegment) {

    init {
        require(ptr.address() != 0L) { "null COM pointer" }
    }

    /** Retrieves the [slot]-th function pointer from the vtable. */
    fun vtblEntry(slot: Int): MemorySegment {
        val p = ADDRESS.byteSize()
        val vtbl = ptr.reinterpret(p).get(ADDRESS, 0)
        return vtbl.reinterpret((slot + 1) * p).get(ADDRESS, slot.toLong() * p)
    }

    /** Calls a vtable slot with an explicit descriptor, returning the raw HRESULT / ULONG. */
    fun rawCall(slot: Int, descriptor: FunctionDescriptor, vararg args: Any?): Int {
        val mh = handleFor(descriptor)
        val all = ArrayList<Any?>(args.size + 2)
        all.add(vtblEntry(slot))
        all.add(ptr)
        all.addAll(args)
        return mh.invokeWithArguments(all) as Int
    }

    /**
     * Calls a method that returns an HRESULT, throwing on failure. The descriptor is
     * inferred automatically from the Kotlin types of the arguments
     * (MemorySegment -> pointer, Int -> INT32, Long -> INT64, Double -> DOUBLE).
     */
    fun call(slot: Int, vararg args: Any?) {
        Native.checkHr(rawCall(slot, inferDescriptor(args), *args), "vtbl[$slot]")
    }

    /** For cases where the layout must be explicit, e.g. passing a struct by value. */
    fun callWith(slot: Int, descriptor: FunctionDescriptor, vararg args: Any?) {
        Native.checkHr(rawCall(slot, descriptor, *args), "vtbl[$slot]")
    }

    /** `HRESULT f(..., T** out)` pattern: receives the output pointer and wraps it as a ComPtr. */
    fun getPtr(slot: Int, vararg args: Any?): ComPtr = Arena.ofConfined().use { a ->
        val out = a.allocate(ADDRESS)
        call(slot, *args, out)
        ComPtr(out.get(ADDRESS, 0))
    }

    /** `HRESULT f(HSTRING* out)` pattern. */
    fun getString(slot: Int): String = Arena.ofConfined().use { a ->
        val out = a.allocate(ADDRESS)
        call(slot, out)
        val h = out.get(ADDRESS, 0)
        try {
            Hstring.read(h)
        } finally {
            Hstring.free(h)
        }
    }

    fun queryInterface(iid: String): ComPtr = getPtr(0, Guid.of(iid)) // IUnknown::QueryInterface

    fun addRef() {
        rawCall(1, UNKNOWN_DESC) // IUnknown::AddRef (return value is the reference count)
    }

    fun release() {
        rawCall(2, UNKNOWN_DESC) // IUnknown::Release
    }

    companion object {
        val UNKNOWN_DESC: FunctionDescriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS)

        private val handleCache = ConcurrentHashMap<FunctionDescriptor, MethodHandle>()

        fun handleFor(descriptor: FunctionDescriptor): MethodHandle =
            handleCache.computeIfAbsent(descriptor) { Native.linker.downcallHandle(it) }

        private fun inferDescriptor(args: Array<out Any?>): FunctionDescriptor {
            val layouts = args.map {
                when (it) {
                    is MemorySegment -> ADDRESS
                    is Int -> JAVA_INT
                    is Long -> JAVA_LONG
                    is Double -> JAVA_DOUBLE
                    else -> error("unsupported argument for auto descriptor: $it")
                }
            }
            return FunctionDescriptor.of(JAVA_INT, ADDRESS, *layouts.toTypedArray())
        }
    }
}
