package jp.hisano.winui4k.winui

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.winrt.WinRt
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT

/**
 * WinUI's UI thread DispatcherQueue.
 * [invokeLater] moves a callback arriving off the UI thread (e.g. a notification click)
 * onto the UI thread so W* APIs can be used safely (SwingUtilities.invokeLater-like).
 */
internal object Dispatcher {
    /** The delegate DispatcherQueueHandler's Invoke(this) — vtbl[3], no arguments */
    private val DESC_HANDLER = FunctionDescriptor.of(JAVA_INT, ADDRESS)

    private var queue: ComPtr? = null

    /**
     * Captures the current thread's (= the UI thread's) DispatcherQueue.
     * Called by [WinUiToolkit] in OnLaunched.
     */
    fun capture() {
        if (queue != null) return
        val statics = WinRt.factory(Abi.CLS_DispatcherQueue, Abi.IID_IDispatcherQueueStatics)
        queue = statics.getPtr(Abi.IDispatcherQueueStatics_GetForCurrentThread)
        statics.release()
    }

    /** Posts [block] to the UI thread's message loop. Can be called from any thread. */
    fun invokeLater(block: () -> Unit) {
        val q = checkNotNull(queue) { "DispatcherQueue hasn't been captured yet (only usable inside WinUiToolkit.launch)" }
        val handler = KComObject("WinUI4K.DispatcherQueueHandler", inspectable = false)
            .addInterface(
                Abi.IID_DispatcherQueueHandler,
                listOf(
                    KComObject.Method(DESC_HANDLER) {
                        block()
                        KComObject.S_OK
                    },
                ),
            )
        Arena.ofConfined().use { a ->
            val out = a.allocate(JAVA_BYTE) // TryEnqueue(handler, out boolean)
            q.call(Abi.IDispatcherQueue_TryEnqueue, handler.primary, out)
        }
    }
}
