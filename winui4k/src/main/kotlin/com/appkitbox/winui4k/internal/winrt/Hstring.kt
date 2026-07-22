package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.checkHr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope

/** The WinRT string type HSTRING. */
internal object Hstring {
    private val create by lazy {
        Ffi.backend.function(
            "combase.dll",
            "WindowsCreateString",
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR),
        )
    }
    private val getRawBuffer by lazy {
        Ffi.backend.function(
            "combase.dll",
            "WindowsGetStringRawBuffer",
            CallDescriptor(ValueKind.PTR, ArgKind.PTR, ArgKind.PTR),
        )
    }
    private val delete by lazy {
        Ffi.backend.function(
            "combase.dll",
            "WindowsDeleteString",
            CallDescriptor(ValueKind.I32, ArgKind.PTR),
        )
    }
    private val duplicateString by lazy {
        Ffi.backend.function(
            "combase.dll",
            "WindowsDuplicateString",
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR),
        )
    }

    private val leakedCache = HashMap<String, Ptr>()

    /** Creates an HSTRING. The caller must release it with [free]. */
    fun of(value: String): Ptr = Ffi.backend.withScope { scope ->
        val buffer = scope.allocate((value.length + 1).toLong() * 2, 2)
        Ffi.backend.memory.putUtf16z(buffer, 0, value)
        val out = scope.allocate(8)
        checkHr(create(buffer, value.length, out) as Int, "WindowsCreateString")
        Ffi.backend.memory.getPtr(out, 0)
    }

    /** An HSTRING reused for the process lifetime, e.g. a runtime class name (intentionally leaked). */
    @Synchronized
    fun ofCached(value: String): Ptr = leakedCache.getOrPut(value) { of(value) }

    /**
     * Duplicates the reference to [hstring] (WindowsDuplicateString) and returns the copy.
     * Use this when handing a cached HSTRING to an out parameter whose contract
     * requires transferring ownership (e.g. GetRuntimeClassName): passing the cached
     * HSTRING as-is would let the caller's WindowsDeleteString free it, leaving the
     * cache holding a dangling pointer (use-after-free on the next lookup).
     */
    fun duplicate(hstring: Ptr): Ptr {
        if (hstring.isNull) return Ptr.NULL
        return Ffi.backend.withScope { scope ->
            val out = scope.allocate(8)
            checkHr(duplicateString(hstring, out) as Int, "WindowsDuplicateString")
            Ffi.backend.memory.getPtr(out, 0)
        }
    }

    fun read(hstring: Ptr): String {
        if (hstring.isNull) return "" // a NULL HSTRING is the empty string
        return Ffi.backend.withScope { scope ->
            val lengthOut = scope.allocate(4)
            val buffer = getRawBuffer(hstring, lengthOut) as Ptr
            val length = Ffi.backend.memory.getInt(lengthOut, 0)
            Ffi.backend.memory.getUtf16(buffer, 0, length)
        }
    }

    fun free(hstring: Ptr) {
        if (!hstring.isNull) delete(hstring)
    }

    /** Creates a temporary HSTRING, passes it to the block, and reliably frees it afterward. */
    internal inline fun <T> use(value: String, block: (Ptr) -> T): T {
        val hstring = of(value)
        try {
            return block(hstring)
        } finally {
            free(hstring)
        }
    }
}

/**
 * `HRESULT f(..., HSTRING* out)` pattern. HSTRING is a WinRT type, so this lives here rather than on ComPtr itself.
 * [args] are arguments passed before `out` (e.g. the TextGetOptions of ITextDocument.GetText).
 */
internal fun ComPtr.getString(slot: Int, vararg args: Any?): String = Ffi.backend.withScope { scope ->
    val out = scope.allocate(8)
    call(slot, *args, out)
    val hstring = Ffi.backend.memory.getPtr(out, 0)
    try {
        Hstring.read(hstring)
    } finally {
        Hstring.free(hstring)
    }
}
