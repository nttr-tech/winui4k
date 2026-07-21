package jp.hisano.winui4k.win32

import jp.hisano.winui4k.ffi.api.ArgKind
import jp.hisano.winui4k.ffi.api.CallDescriptor
import jp.hisano.winui4k.ffi.api.Ffi
import jp.hisano.winui4k.ffi.api.Ptr
import jp.hisano.winui4k.ffi.api.ValueKind
import jp.hisano.winui4k.ffi.api.function
import jp.hisano.winui4k.ffi.api.withScope

/** The subset of the flat Win32 API (user32 / kernel32) that winui4k uses. */
internal object Win32 {
    private val setProcessDpiAwarenessContext by lazy {
        Ffi.backend.function(
            "user32.dll", "SetProcessDpiAwarenessContext",
            CallDescriptor(ValueKind.I32, ArgKind.PTR),
        )
    }
    private val getModuleHandleW by lazy {
        Ffi.backend.function(
            "kernel32.dll", "GetModuleHandleW",
            CallDescriptor(ValueKind.PTR, ArgKind.PTR),
        )
    }
    private val getModuleFileNameW by lazy {
        Ffi.backend.function(
            "kernel32.dll", "GetModuleFileNameW",
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.I32),
        )
    }

    /** Enables Per-Monitor v2 DPI awareness. java.exe has no manifest, so this is done in code. */
    fun enablePerMonitorDpiAwareness() {
        // DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2 = (HANDLE)-4
        setProcessDpiAwarenessContext(Ptr(-4L))
    }

    /** Returns the full path of a loaded module, or null if it isn't loaded. */
    fun moduleFilePath(module: String): String? = Ffi.backend.withScope { scope ->
        val memory = Ffi.backend.memory
        val name = scope.allocate((module.length + 1).toLong() * 2, 2)
        memory.putUtf16z(name, 0, module)
        val handle = getModuleHandleW(name) as Ptr
        if (handle.isNull) return null
        val buf = scope.allocate(32768L * 2, 2)
        val n = getModuleFileNameW(handle, buf, 32768) as Int
        if (n <= 0) return null
        memory.getUtf16(buf, 0, n)
    }
}
