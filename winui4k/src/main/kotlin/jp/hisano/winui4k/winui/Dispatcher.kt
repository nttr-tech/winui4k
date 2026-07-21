package jp.hisano.winui4k.winui

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.winrt.WinRt
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * WinUI's UI thread DispatcherQueue.
 * [invokeLater] moves a callback arriving off the UI thread (e.g. a notification click)
 * onto the UI thread so W* APIs can be used safely (SwingUtilities.invokeLater-like).
 */
internal object Dispatcher {
    /** The delegate DispatcherQueueHandler's Invoke(this) — vtbl[3], no arguments */
    private val DESC_HANDLER = FunctionDescriptor.of(JAVA_INT, ADDRESS)

    /** The TypedEventHandler's Invoke(this, sender, args) — vtbl[3] */
    private val DESC_TICK_HANDLER = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)

    private var queue: ComPtr? = null
    private var uiThread: Thread? = null

    /**
     * Captures the current thread's (= the UI thread's) DispatcherQueue.
     * Called by WinUiUtilities in OnLaunched.
     */
    fun capture() {
        if (queue != null) return
        val statics = WinRt.factory(Abi.CLS_DispatcherQueue, Abi.IID_IDispatcherQueueStatics)
        queue = statics.getPtr(Abi.IDispatcherQueueStatics_GetForCurrentThread)
        statics.release()
        uiThread = Thread.currentThread()
    }

    /** True if the current thread is the UI thread (SwingUtilities.isEventDispatchThread-like). */
    val isDispatchThread: Boolean
        get() = Thread.currentThread() === uiThread

    /** Posts [block] to the UI thread's message loop. Can be called from any thread. */
    fun invokeLater(block: () -> Unit) {
        val q = checkNotNull(queue) { "DispatcherQueue hasn't been captured yet (start WinUI via WinUiUtilities before using this)" }
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

    /**
     * Starts a DispatcherQueueTimer that runs [block] once on the UI thread after
     * [delayMillis] milliseconds (a one-shot javax.swing.Timer-like). Calling close() on the
     * return value cancels it if it hasn't fired yet. Can be called from any thread.
     */
    fun schedule(delayMillis: Long, block: () -> Unit): AutoCloseable {
        // True once fired or cancelled. Whichever of Tick and close() gets there first transitions this exactly once
        val done = AtomicBoolean(false)
        // The timer is created/started/stopped on the UI thread (the queue belongs to the thread that created it)
        val timerRef = AtomicReference<ComPtr?>(null)
        runOnDispatchThread {
            if (done.get()) return@runOnDispatchThread
            val q = checkNotNull(queue) { "DispatcherQueue hasn't been captured yet (start WinUI via WinUiUtilities before using this)" }
            val timer = q.getPtr(Abi.IDispatcherQueue_CreateTimer)
            // TimeSpan is an int64 in 100ns units, passed by value
            timer.call(Abi.IDispatcherQueueTimer_put_Interval, delayMillis.coerceAtLeast(0) * 10_000)
            timer.putBool(Abi.IDispatcherQueueTimer_put_IsRepeating, false)
            val tickHandler = KComObject("WinUI4K.DispatcherQueueTimerTickHandler", inspectable = false)
                .addInterface(
                    Abi.IID_DispatcherQueueTimerTickHandler,
                    listOf(
                        KComObject.Method(DESC_TICK_HANDLER) {
                            if (done.compareAndSet(false, true)) {
                                stopAndRelease(timerRef)
                                block()
                            }
                            KComObject.S_OK
                        },
                    ),
                )
            Arena.ofConfined().use { a ->
                val token = a.allocate(JAVA_LONG) // EventRegistrationToken (int64)
                timer.call(Abi.IDispatcherQueueTimer_add_Tick, tickHandler.primary, token)
            }
            timerRef.set(timer)
            timer.call(Abi.IDispatcherQueueTimer_Start)
        }
        return AutoCloseable {
            if (done.compareAndSet(false, true)) {
                // If not created yet (creation is queued), the creation side's done check aborts it
                runOnDispatchThread { stopAndRelease(timerRef) }
            }
        }
    }

    private fun stopAndRelease(timerRef: AtomicReference<ComPtr?>) {
        val timer = timerRef.getAndSet(null) ?: return
        timer.call(Abi.IDispatcherQueueTimer_Stop)
        timer.release()
    }

    private fun runOnDispatchThread(block: () -> Unit) {
        if (isDispatchThread) block() else invokeLater(block)
    }
}
