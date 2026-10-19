package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope

/**
 * Key input for tests: posts WM_KEYDOWN / WM_KEYUP with PostMessageW to the InputSiteWindowClass child window of the
 * shared window ("winui4k tests"). WinUI receives keyboard input on this child window, so the keys arrive even when the
 * window is not in the foreground (active), and they do not leak into another window the user is working in.
 * The pressed state of modifier keys (Ctrl / Shift) is not conveyed.
 */
internal object TestKeyboard {
    private val findWindowExW by lazy {
        Ffi.backend.function("user32.dll", "FindWindowExW", CallDescriptor(ValueKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR))
    }
    private val getClassNameW by lazy {
        Ffi.backend.function("user32.dll", "GetClassNameW", CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.I32))
    }
    private val getWindowThreadProcessId by lazy {
        Ffi.backend.function("user32.dll", "GetWindowThreadProcessId", CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR))
    }
    private val postMessageW by lazy {
        Ffi.backend.function("user32.dll", "PostMessageW", CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.I64, ArgKind.I64))
    }
    private val getCurrentProcessId by lazy {
        Ffi.backend.function("kernel32.dll", "GetCurrentProcessId", CallDescriptor(ValueKind.I32))
    }

    private const val WM_KEYDOWN = 0x0100
    private const val WM_KEYUP = 0x0101
    private const val INPUT_SITE_CLASS = "InputSiteWindowClass"
    private const val FRAME_TITLE = "winui4k tests"
    private const val CLASS_NAME_CAPACITY = 256

    /**
     * Presses and releases [virtualKey] (a Win32 virtual-key code). [scanCode] is the scan code put into lParam.
     * WM_CHAR for a character key is generated from WM_KEYDOWN by TranslateMessage in WinUI's message loop, so whether
     * it arrives before or after the WM_KEYUP sent right after is not determined (with real input, before). To check the
     * order, send [keyDown], wait for the character to arrive, and then send [keyUp].
     */
    fun press(virtualKey: Int, scanCode: Int) {
        keyDown(virtualKey, scanCode)
        keyUp(virtualKey, scanCode)
    }

    /** Presses [virtualKey] (WM_KEYDOWN). [extended] marks an extended key (arrows, Delete etc.; bit 24 of lParam). */
    @JvmOverloads
    fun keyDown(virtualKey: Int, scanCode: Int, extended: Boolean = false) {
        check(postMessageW(inputSite(), WM_KEYDOWN, virtualKey.toLong(), keyData(scanCode, extended)) as Int != 0) {
            "could not send WM_KEYDOWN"
        }
    }

    /** Releases [virtualKey] (WM_KEYUP). */
    @JvmOverloads
    fun keyUp(virtualKey: Int, scanCode: Int, extended: Boolean = false) {
        val up = keyData(scanCode, extended) or (1L shl 30) or (1L shl 31)
        check(postMessageW(inputSite(), WM_KEYUP, virtualKey.toLong(), up) as Int != 0) { "could not send WM_KEYUP" }
    }

    /** lParam of WM_KEYDOWN / WM_KEYUP (repeat count 1, scan code, extended-key mark). */
    private fun keyData(scanCode: Int, extended: Boolean): Long =
        1L or (scanCode.toLong() shl 16) or (if (extended) 1L shl 24 else 0L)

    /** Presses and releases the Enter key (VK_RETURN = 0x0D, scan code 0x1C). */
    fun pressEnter() = press(0x0D, 0x1C)

    /** The InputSiteWindowClass inside this process's shared window. */
    private fun inputSite(): Ptr {
        val pid = getCurrentProcessId() as Int
        var top: Ptr? = null
        while (true) {
            val next = Ffi.backend.withScope { scope ->
                val title = scope.allocate((FRAME_TITLE.length + 1).toLong() * 2, 2)
                Ffi.backend.memory.putUtf16z(title, 0, FRAME_TITLE)
                findWindowExW(null, top, null, title) as Ptr
            }
            check(!next.isNull) { "could not find the shared window ($FRAME_TITLE)" }
            top = next
            if (processOf(next) == pid) break
        }
        return checkNotNull(findDescendant(checkNotNull(top), INPUT_SITE_CLASS)) { "could not find $INPUT_SITE_CLASS" }
    }

    private fun processOf(window: Ptr): Int = Ffi.backend.withScope { scope ->
        val out = scope.allocate(4)
        getWindowThreadProcessId(window, out)
        Ffi.backend.memory.getInt(out, 0)
    }

    private fun findDescendant(parent: Ptr, className: String): Ptr? {
        var child: Ptr? = null
        while (true) {
            val next = findWindowExW(parent, child, null, null) as Ptr
            if (next.isNull) return null
            if (classNameOf(next) == className) return next
            findDescendant(next, className)?.let { return it }
            child = next
        }
    }

    private fun classNameOf(window: Ptr): String = Ffi.backend.withScope { scope ->
        val buffer = scope.allocate(CLASS_NAME_CAPACITY * 2L, 2)
        val length = getClassNameW(window, buffer, CLASS_NAME_CAPACITY) as Int
        Ffi.backend.memory.getUtf16(buffer, 0, length)
    }
}
