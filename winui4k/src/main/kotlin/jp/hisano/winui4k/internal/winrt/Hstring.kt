package jp.hisano.winui4k.internal.winrt

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.com.checkHr
import jp.hisano.winui4k.internal.ffi.api.ArgKind
import jp.hisano.winui4k.internal.ffi.api.CallDescriptor
import jp.hisano.winui4k.internal.ffi.api.Ffi
import jp.hisano.winui4k.internal.ffi.api.Ptr
import jp.hisano.winui4k.internal.ffi.api.ValueKind
import jp.hisano.winui4k.internal.ffi.api.function
import jp.hisano.winui4k.internal.ffi.api.withScope

/** The WinRT string type HSTRING. */
internal object Hstring {
    private val create by lazy {
        Ffi.backend.function(
            "combase.dll", "WindowsCreateString",
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR),
        )
    }
    private val getRawBuffer by lazy {
        Ffi.backend.function(
            "combase.dll", "WindowsGetStringRawBuffer",
            CallDescriptor(ValueKind.PTR, ArgKind.PTR, ArgKind.PTR),
        )
    }
    private val delete by lazy {
        Ffi.backend.function(
            "combase.dll", "WindowsDeleteString",
            CallDescriptor(ValueKind.I32, ArgKind.PTR),
        )
    }

    private val leakedCache = HashMap<String, Ptr>()

    /** Creates an HSTRING. The caller must release it with [free]. */
    fun of(s: String): Ptr = Ffi.backend.withScope { scope ->
        val buf = scope.allocate((s.length + 1).toLong() * 2, 2)
        Ffi.backend.memory.putUtf16z(buf, 0, s)
        val out = scope.allocate(8)
        checkHr(create(buf, s.length, out) as Int, "WindowsCreateString")
        Ffi.backend.memory.getPtr(out, 0)
    }

    /** An HSTRING reused for the process lifetime, e.g. a runtime class name (intentionally leaked). */
    @Synchronized
    fun ofCached(s: String): Ptr = leakedCache.getOrPut(s) { of(s) }

    fun read(h: Ptr): String {
        if (h.isNull) return "" // a NULL HSTRING is the empty string
        return Ffi.backend.withScope { scope ->
            val lenOut = scope.allocate(4)
            val buf = getRawBuffer(h, lenOut) as Ptr
            val n = Ffi.backend.memory.getInt(lenOut, 0)
            Ffi.backend.memory.getUtf16(buf, 0, n)
        }
    }

    fun free(h: Ptr) {
        if (!h.isNull) delete(h)
    }

    /** Creates a temporary HSTRING, passes it to the block, and reliably frees it afterward. */
    internal inline fun <T> use(s: String, block: (Ptr) -> T): T {
        val h = of(s)
        try {
            return block(h)
        } finally {
            free(h)
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
    val h = Ffi.backend.memory.getPtr(out, 0)
    try {
        Hstring.read(h)
    } finally {
        Hstring.free(h)
    }
}
