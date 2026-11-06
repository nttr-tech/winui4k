package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.NativeFunction
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.win32.Win32

/**
 * A nested message loop on the UI thread. Used to build APIs shaped like Swing's modal dialogs (JOptionPane.showConfirmDialog and the like
 * block the caller until they return a value). [run] keeps dispatching messages until a condition holds, and meanwhile
 * input, rendering, layout and [Dispatcher.invokeLater] (and the GC-driven Release of [com.appkitbox.winui4k.internal.com.lifetime.ReleasePump])
 * keep working. The loop runs with the same steps as WinUI's own loop (FrameworkApplication::RunDesktopWindowMessageLoop):
 * GetMessageW -> ContentPreTranslateMessage -> TranslateMessage -> DispatchMessageW.
 *
 * Reentrancy rules (the verification results are in "Nested message loop" in doc/verification.md):
 * - Can be called only from the UI thread. [IllegalStateException] from other threads.
 * - May be called from a button's Click, a menu item's Click, a keyboard accelerator's Invoked, or [Dispatcher.invokeLater].
 * - Cannot be called from layout or frame processing (SizeChanged, Loaded, LayoutUpdated, layout manager arrangement)
 *   ([IllegalStateException]). XAML keeps CLayoutManager::m_isInUpdateLayout and UIThreadScheduler's Ticking set during those,
 *   so no further layout or rendering happens inside the loop and the opened dialog never appears. Places that pass such notifications to W* users
 *   must wrap them in [duringLayout].
 * - May be nested (waiting for another dialog or a picker from inside a dialog, and so on). The outer loop does not return until the inner [run] returns
 *   (even if the outer condition holds first, it returns after the inner one ends; last-in first-out, like Win32 modal loops).
 * - On receiving WM_QUIT (Application.Exit, or the automatic exit when the last window is closed), it calls PostQuitMessage again with the same exit code
 *   so that the outer loops (finally WinUI's loop) receive it too, and [run] returns false. Callers must treat this as "closed"
 *   (CLOSED_OPTION for JOptionPane).
 * - If the owner window is closed while a ContentDialog is open, ContentDialog.Closed never arrives (the loop would never return).
 *   Code waiting for a dialog must include in isDone "the owner element's IsLoaded became false" (it becomes false when the window closes)
 *   or the window's Closed.
 * - The isDone condition of [run] is called on the UI thread after each dispatched message. The code that makes it true (ContentDialog.Closed and so on)
 *   runs inside UI thread messages, so it does not wait forever.
 */
internal object ModalLoop {
    /** WM_QUIT. */
    private const val WM_QUIT = 0x0012

    /** The size of the MSG structure (x64: hwnd 8, message 4 + padding 4, wParam 8, lParam 8, time 4, pt 8, lPrivate 4). */
    private const val MSG_SIZE = 48L
    private const val MSG_MESSAGE_OFFSET = 8L
    private const val MSG_WPARAM_OFFSET = 16L

    private val getMessageW by lazy {
        Ffi.backend.function("user32.dll", "GetMessageW", CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.I32, ArgKind.I32))
    }
    private val translateMessage by lazy {
        Ffi.backend.function("user32.dll", "TranslateMessage", CallDescriptor(ValueKind.I32, ArgKind.PTR))
    }
    private val dispatchMessageW by lazy {
        Ffi.backend.function("user32.dll", "DispatchMessageW", CallDescriptor(ValueKind.I64, ArgKind.PTR))
    }
    private val postQuitMessage by lazy {
        Ffi.backend.function("user32.dll", "PostQuitMessage", CallDescriptor(ValueKind.VOID, ArgKind.I32))
    }

    /**
     * ContentPreTranslateMessage, which WinUI's loop calls before TranslateMessage (Microsoft.UI.Windowing.Core.dll;
     * Microsoft.UI.Dispatching.Interop.h). Passes Tab navigation, access keys and so on to the XAML island first. Looks up the DLL WinUI has already loaded
     * by its full path. Null if not found (an old runtime), in which case it is not called and the loop goes on to TranslateMessage.
     */
    private val contentPreTranslateMessage: NativeFunction? by lazy {
        val path = Win32.moduleFilePath(WINDOWING_CORE_DLL) ?: return@lazy null
        try {
            Ffi.backend.function(path, "ContentPreTranslateMessage", CallDescriptor(ValueKind.I32, ArgKind.PTR))
        } catch (_: UnsatisfiedLinkError) {
            null
        }
    }

    private const val WINDOWING_CORE_DLL = "Microsoft.UI.Windowing.Core.dll"

    /** The number of [run]s currently running. Read and written only by the UI thread. */
    var depth: Int = 0
        private set

    /** The nesting count of [duringLayout]. If not 0, [run] is refused. */
    private var layoutDepth = 0

    /**
     * Runs a nested message loop on the UI thread until [isDone] returns true. Returns true when it returned because the condition held,
     * false when it returned on receiving WM_QUIT (WM_QUIT is posted again to the outer loop). If [isDone] is true at first, returns true without looping.
     * [IllegalStateException] where it cannot be called (off the UI thread, inside layout notifications).
     */
    fun run(isDone: () -> Boolean): Boolean {
        check(Dispatcher.isDispatchThread) { "the nested message loop can be run only from the UI thread" }
        check(layoutDepth == 0) {
            "cannot wait for a dialog to close inside a layout notification (SizeChanged, Loaded, LayoutUpdated, layout manager)" +
                " (XAML layout and rendering would stay stopped). Open it later with WinUiUtilities.invokeLater"
        }
        depth++
        try {
            return Ffi.backend.withScope { scope ->
                val msg = scope.allocate(MSG_SIZE)
                pump(msg, isDone)
            }
        } finally {
            depth--
        }
    }

    private fun pump(msg: Ptr, isDone: () -> Boolean): Boolean {
        val memory = Ffi.backend.memory
        while (!isDone()) {
            when (getMessageW(msg, null, 0, 0) as Int) {
                0 -> {
                    // WM_QUIT: post it again so that the outer loops (finally WinUI's loop inside Application.Start) end too
                    check(memory.getInt(msg, MSG_MESSAGE_OFFSET) == WM_QUIT)
                    postQuitMessage(memory.getLong(msg, MSG_WPARAM_OFFSET).toInt())
                    return false
                }
                -1 -> error("GetMessageW failed")
                else -> {
                    val preTranslated = contentPreTranslateMessage?.let { (it(msg) as Int) != 0 } ?: false
                    if (!preTranslated) {
                        translateMessage(msg)
                        dispatchMessageW(msg)
                    }
                }
            }
        }
        return true
    }

    /**
     * Wraps places that call W* users' code during XAML layout and frame processing (SizeChanged, Loaded and other listeners).
     * Inside it, [run] throws [IllegalStateException]. Call from the UI thread.
     */
    inline fun <T> duringLayout(block: () -> T): T {
        enterLayout()
        try {
            return block()
        } finally {
            exitLayout()
        }
    }

    @PublishedApi
    internal fun enterLayout() {
        layoutDepth++
    }

    @PublishedApi
    internal fun exitLayout() {
        layoutDepth--
    }
}
