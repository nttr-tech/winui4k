package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.MemoryScope
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope

/**
 * For tests: finds a top-level window of this process and sends it WM_CLOSE (the same way of closing as the title bar's X or Esc in a dialog).
 * Used for WFrame windows (found by title) and the OS file dialog shown by pickers (window class #32770).
 */
internal object TopLevelWindows {
    private val findWindowExW by lazy {
        Ffi.backend.function("user32.dll", "FindWindowExW", CallDescriptor(ValueKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR))
    }
    private val getWindowThreadProcessId by lazy {
        Ffi.backend.function("user32.dll", "GetWindowThreadProcessId", CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR))
    }
    private val isWindowVisible by lazy {
        Ffi.backend.function("user32.dll", "IsWindowVisible", CallDescriptor(ValueKind.I32, ArgKind.PTR))
    }
    private val postMessageW by lazy {
        Ffi.backend.function("user32.dll", "PostMessageW", CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.I64, ArgKind.I64))
    }
    private val getCurrentProcessId by lazy {
        Ffi.backend.function("kernel32.dll", "GetCurrentProcessId", CallDescriptor(ValueKind.I32))
    }

    private const val WM_CLOSE = 0x0010

    /** The window class of OS common dialogs (IFileOpenDialog and so on). */
    const val DIALOG_CLASS = "#32770"

    /** The first visible top-level window of this process matching [className] / [title] (null means no condition). */
    fun find(className: String?, title: String?): Ptr? {
        val pid = getCurrentProcessId() as Int
        var previous: Ptr? = null
        while (true) {
            val next = Ffi.backend.withScope { scope ->
                findWindowExW(null, previous, className?.let { utf16(scope, it) }, title?.let { utf16(scope, it) }) as Ptr
            }
            if (next.isNull) return null
            if (processOf(next) == pid && isWindowVisible(next) as Int != 0) return next
            previous = next
        }
    }

    /** Sends WM_CLOSE to [window]. */
    fun close(window: Ptr) {
        check(postMessageW(window, WM_CLOSE, 0L, 0L) as Int != 0) { "cannot send WM_CLOSE" }
    }

    private fun utf16(scope: MemoryScope, text: String): Ptr {
        val buffer = scope.allocate((text.length + 1).toLong() * 2, 2)
        Ffi.backend.memory.putUtf16z(buffer, 0, text)
        return buffer
    }

    private fun processOf(window: Ptr): Int = Ffi.backend.withScope { scope ->
        val out = scope.allocate(4)
        getWindowThreadProcessId(window, out)
        Ffi.backend.memory.getInt(out, 0)
    }
}
