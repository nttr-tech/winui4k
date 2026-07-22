package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import java.util.concurrent.ConcurrentLinkedQueue
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
        try {
            queue = statics.getPtr(Abi.IDispatcherQueueStatics_GetForCurrentThread)
        } finally {
            statics.release()
        }
        uiThread = Thread.currentThread()
    }

    /** True if the current thread is the UI thread (in the spirit of SwingUtilities.isEventDispatchThread). */
    val isDispatchThread: Boolean
        get() = Thread.currentThread() === uiThread

    /** Blocks waiting to be posted by invokeLater. Each TryEnqueue corresponds to exactly one Invoke, matched FIFO. */
    private val pending = ConcurrentLinkedQueue<() -> Unit>()

    /**
     * The DispatcherQueueHandler shared by every invokeLater call.
     * Creating a KComObject (a native vtable + upcall stub) on every call would leak for
     * the lifetime of the process, so a single instance is shared and blocks are pulled from [pending].
     */
    private val enqueueHandler by lazy {
        KComObject("WinUI4K.DispatcherQueueHandler", inspectable = false)
            .addInterface(
                Abi.IID_DispatcherQueueHandler,
                listOf(
                    KComObject.Method(DESC_HANDLER) {
                        pending.poll()?.invoke()
                        KComObject.S_OK
                    },
                ),
            )
    }

    /** Posts [block] onto the UI thread's message loop. Callable from any thread. */
    fun invokeLater(block: () -> Unit) {
        val q = checkNotNull(queue) { "DispatcherQueue hasn't been captured yet (launch WinUI via WinUiUtilities before using this)" }
        pending.add(block)
        val enqueued = Ffi.backend.withScope { scope ->
            val out = scope.allocate(1, 1) // TryEnqueue(handler, out boolean)
            q.call(Abi.IDispatcherQueue_TryEnqueue, enqueueHandler.primary, out)
            Ffi.backend.memory.getByte(out, 0).toInt() != 0
        }
        if (!enqueued) {
            // Don't silently swallow an enqueue failure (the queue shutting down) — surface it
            pending.remove(block)
            throw IllegalStateException("DispatcherQueue.TryEnqueue failed (the UI thread is shutting down)")
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
