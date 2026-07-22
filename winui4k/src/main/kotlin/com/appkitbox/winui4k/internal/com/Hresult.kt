package com.appkitbox.winui4k.internal.com

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope

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
            val description = scope.allocate(8)
            val errorCode = scope.allocate(4)
            val restrictedDescription = scope.allocate(8)
            val sid = scope.allocate(8)
            info.call(3, description, errorCode, restrictedDescription, sid)
            val descriptionText = takeBstr(memory.getPtr(description, 0))
            val restrictedText = takeBstr(memory.getPtr(restrictedDescription, 0))
            takeBstr(memory.getPtr(sid, 0))
            if (memory.getInt(errorCode, 0) != failedHr) return null // leftover from a different error
            listOf(descriptionText, restrictedText).filter { it.isNotBlank() }.distinct()
                .joinToString(" / ").trim().ifBlank { null }
        } finally {
            info.release()
        }
    }

    /** Reads and frees a BSTR (length-prefixed UTF-16). */
    private fun takeBstr(bstr: Ptr): String {
        if (bstr.isNull) return ""
        try {
            val byteLength = Ffi.backend.memory.getInt(Ptr(bstr.address - 4), 0)
            return Ffi.backend.memory.getUtf16(bstr, 0, byteLength / 2)
        } finally {
            sysFreeString(bstr)
        }
    }
}
