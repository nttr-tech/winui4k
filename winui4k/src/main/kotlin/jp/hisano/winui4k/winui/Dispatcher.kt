package jp.hisano.winui4k.winui

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.ffi.api.ArgKind
import jp.hisano.winui4k.ffi.api.CallDescriptor
import jp.hisano.winui4k.ffi.api.Ffi
import jp.hisano.winui4k.ffi.api.ValueKind
import jp.hisano.winui4k.ffi.api.withScope
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.KComObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * The WinUI UI thread's DispatcherQueue (Microsoft.UI.Dispatching).
 * Lets callbacks that arrive from outside the UI thread (e.g. notification clicks) move
 * onto the UI thread via [invokeLater] so W* APIs can be used safely there
 * (in the spirit of SwingUtilities.invokeLater).
 */
internal object Dispatcher {
    /** delegate DispatcherQueueHandler's Invoke(this) — vtbl[3], no arguments */
    private val DESC_HANDLER = CallDescriptor(ValueKind.I32, ArgKind.PTR)

    /** TypedEventHandler's Invoke(this, sender, args) — vtbl[3] */
    private val DESC_TICK_HANDLER = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

    private var queue: ComPtr? = null
    private var uiThread: Thread? = null

    /**
     * Captures the current thread's (= the UI thread's) DispatcherQueue.
     * WinUiUtilities calls this from OnLaunched.
     */
    fun capture() {
        if (queue != null) return
        val statics = Activation.factory(Abi.CLS_DispatcherQueue, Abi.IID_IDispatcherQueueStatics)
        queue = statics.getPtr(Abi.IDispatcherQueueStatics_GetForCurrentThread)
        statics.release()
        uiThread = Thread.currentThread()
    }

    /** True if the current thread is the UI thread (in the spirit of SwingUtilities.isEventDispatchThread). */
    val isDispatchThread: Boolean
        get() = Thread.currentThread() === uiThread

    /** Posts [block] onto the UI thread's message loop. Callable from any thread. */
    fun invokeLater(block: () -> Unit) {
        val q = checkNotNull(queue) { "DispatcherQueue hasn't been captured yet (launch WinUI via WinUiUtilities before using this)" }
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
        Ffi.backend.withScope { scope ->
            val out = scope.allocate(1, 1) // TryEnqueue(handler, out boolean)
            q.call(Abi.IDispatcherQueue_TryEnqueue, handler.primary, out)
        }
    }

    /**
     * Starts a DispatcherQueueTimer that runs [block] once on the UI thread after
     * [delayMillis] milliseconds (in the spirit of javax.swing.Timer's one-shot mode).
     * Calling close() on the returned value cancels it if it hasn't fired yet.
     * Callable from any thread.
     */
    fun schedule(delayMillis: Long, block: () -> Unit): AutoCloseable {
        // true once fired or cancelled. Whichever of Tick / close() gets there first wins the one-time transition
        val done = AtomicBoolean(false)
        // Creating, starting, and stopping the timer all happen on the UI thread (the queue belongs to its creating thread)
        val timerRef = AtomicReference<ComPtr?>(null)
        runOnDispatchThread {
            if (done.get()) return@runOnDispatchThread
            val q = checkNotNull(queue) { "DispatcherQueue hasn't been captured yet (launch WinUI via WinUiUtilities before using this)" }
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
            Ffi.backend.withScope { scope ->
                val token = scope.allocate(8) // EventRegistrationToken (int64)
                timer.call(Abi.IDispatcherQueueTimer_add_Tick, tickHandler.primary, token)
            }
            timerRef.set(timer)
            timer.call(Abi.IDispatcherQueueTimer_Start)
        }
        return AutoCloseable {
            if (done.compareAndSet(false, true)) {
                // If not yet created (creation is still queued), the creating side's done check aborts it
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
