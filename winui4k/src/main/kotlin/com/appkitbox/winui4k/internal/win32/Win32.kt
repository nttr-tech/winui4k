package com.appkitbox.winui4k.internal.win32

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope

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
    private val createEventW by lazy {
        Ffi.backend.function(
            "kernel32.dll", "CreateEventW",
            CallDescriptor(ValueKind.PTR, ArgKind.PTR, ArgKind.I32, ArgKind.I32, ArgKind.PTR),
        )
    }
    private val setEvent by lazy {
        Ffi.backend.function("kernel32.dll", "SetEvent", CallDescriptor(ValueKind.I32, ArgKind.PTR))
    }
    private val closeHandle by lazy {
        Ffi.backend.function("kernel32.dll", "CloseHandle", CallDescriptor(ValueKind.I32, ArgKind.PTR))
    }

    /** Enables Per-Monitor v2 DPI awareness. java.exe has no manifest, so this is done in code. */
    fun enablePerMonitorDpiAwareness() {
        // DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2 = (HANDLE)-4
        setProcessDpiAwarenessContext(Ptr(-4L))
    }

    /** Creates an auto-reset, initially-unsignaled Win32 event (CreateEventW). */
    fun newAutoResetEvent(): Ptr {
        val handle = createEventW(null, 0, 0, null) as Ptr
        check(!handle.isNull) { "CreateEventW failed" }
        return handle
    }

    /** Signals a Win32 event (SetEvent). */
    fun signalEvent(handle: Ptr) {
        setEvent(handle)
    }

    /** Closes a Win32 handle (CloseHandle). */
    fun closeEventHandle(handle: Ptr) {
        closeHandle(handle)
    }

    private val loadLibraryW by lazy {
        Ffi.backend.function(
            "kernel32.dll", "LoadLibraryW",
            CallDescriptor(ValueKind.PTR, ArgKind.PTR),
        )
    }

    /** Loads a DLL into this process (LoadLibraryW). Throws IllegalStateException on failure. */
    fun loadLibrary(path: String) {
        val handle = Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val pathBuffer = scope.allocate((path.length + 1).toLong() * 2, 2)
            memory.putUtf16z(pathBuffer, 0, path)
            loadLibraryW(pathBuffer) as Ptr
        }
        check(!handle.isNull) { "LoadLibraryW($path) failed" }
    }

    private val setEnvironmentVariableW by lazy {
        Ffi.backend.function(
            "kernel32.dll", "SetEnvironmentVariableW",
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR),
        )
    }

    /**
     * Sets an environment variable for this process (SetEnvironmentVariableW).
     * Only visible to the native side (GetEnvironmentVariable); not reflected in the JVM's System.getenv.
     */
    fun setEnvironmentVariable(name: String, value: String) {
        Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val nameBuffer = scope.allocate((name.length + 1).toLong() * 2, 2)
            memory.putUtf16z(nameBuffer, 0, name)
            val valueBuffer = scope.allocate((value.length + 1).toLong() * 2, 2)
            memory.putUtf16z(valueBuffer, 0, value)
            setEnvironmentVariableW(nameBuffer, valueBuffer)
        }
    }

    /** Returns the full path of a loaded module, or null if it isn't loaded. */
    fun moduleFilePath(module: String): String? = Ffi.backend.withScope { scope ->
        val memory = Ffi.backend.memory
        val nameBuffer = scope.allocate((module.length + 1).toLong() * 2, 2)
        memory.putUtf16z(nameBuffer, 0, module)
        val handle = getModuleHandleW(nameBuffer) as Ptr
        if (handle.isNull) return null
        val pathBuffer = scope.allocate(32768L * 2, 2)
        val length = getModuleFileNameW(handle, pathBuffer, 32768) as Int
        if (length <= 0) return null
        memory.getUtf16(pathBuffer, 0, length)
    }
}
