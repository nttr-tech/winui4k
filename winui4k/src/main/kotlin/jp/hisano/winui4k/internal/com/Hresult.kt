package jp.hisano.winui4k.internal.com

import jp.hisano.winui4k.internal.ffi.api.ArgKind
import jp.hisano.winui4k.internal.ffi.api.CallDescriptor
import jp.hisano.winui4k.internal.ffi.api.Ffi
import jp.hisano.winui4k.internal.ffi.api.Ptr
import jp.hisano.winui4k.internal.ffi.api.ValueKind
import jp.hisano.winui4k.internal.ffi.api.function
import jp.hisano.winui4k.internal.ffi.api.withScope

/** Throws [WindowsRuntimeException] if the HRESULT [hr] is a failure (negative). */
internal fun checkHr(hr: Int, what: String) {
    if (hr < 0) {
        val detail = RestrictedError.messageFor(hr)
        throw WindowsRuntimeException(if (detail != null) "$what [$detail]" else what, hr)
    }
}

class WindowsRuntimeException(what: String, val hresult: Int) :
    RuntimeException("%s failed: HRESULT=0x%08X".format(what, hresult))

/**
 * Retrieves the detailed message (IRestrictedErrorInfo) for the most recent WinRT error.
 * WinRT APIs often leave a human-readable description in thread-local storage on
 * failure; used to pin down the cause of generic HRESULTs like E_FAIL.
 * (This originates from WinRT, but it's an implementation detail specific to checkHr's
 * failure diagnostics, so it lives in this layer alongside it.)
 */
private object RestrictedError {
    private val getRestrictedErrorInfo by lazy {
        Ffi.backend.function(
            "combase.dll", "GetRestrictedErrorInfo",
            CallDescriptor(ValueKind.I32, ArgKind.PTR),
        )
    }
    private val sysFreeString by lazy {
        Ffi.backend.function(
            "oleaut32.dll", "SysFreeString",
            CallDescriptor(ValueKind.VOID, ArgKind.PTR),
        )
    }

    fun messageFor(failedHr: Int): String? = runCatching { fetch(failedHr) }.getOrNull()

    private fun fetch(failedHr: Int): String? = Ffi.backend.withScope { scope ->
        val memory = Ffi.backend.memory
        val out = scope.allocate(8)
        val hr = getRestrictedErrorInfo(out) as Int
        val infoPtr = memory.getPtr(out, 0)
        if (hr != 0 || infoPtr.isNull) return null
        val info = ComPtr(infoPtr)
        try {
            // IRestrictedErrorInfo::GetErrorDetails(BSTR* desc, HRESULT* err, BSTR* restrictedDesc, BSTR* sid) = vtbl[3]
            val desc = scope.allocate(8)
            val err = scope.allocate(4)
            val restricted = scope.allocate(8)
            val sid = scope.allocate(8)
            info.call(3, desc, err, restricted, sid)
            val descStr = takeBstr(memory.getPtr(desc, 0))
            val restrictedStr = takeBstr(memory.getPtr(restricted, 0))
            takeBstr(memory.getPtr(sid, 0))
            if (memory.getInt(err, 0) != failedHr) return null // leftover from a different error
            listOf(descStr, restrictedStr).filter { it.isNotBlank() }.distinct()
                .joinToString(" / ").trim().ifBlank { null }
        } finally {
            info.release()
        }
    }

    /** Reads and frees a BSTR (length-prefixed UTF-16). */
    private fun takeBstr(b: Ptr): String {
        if (b.isNull) return ""
        try {
            val byteLen = Ffi.backend.memory.getInt(Ptr(b.address - 4), 0)
            return Ffi.backend.memory.getUtf16(b, 0, byteLen / 2)
        } finally {
            sysFreeString(b)
        }
    }
}
