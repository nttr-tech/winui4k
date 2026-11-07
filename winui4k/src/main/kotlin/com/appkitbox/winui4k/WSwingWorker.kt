package com.appkitbox.winui4k

import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RunnableFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * javax.swing.SwingWorker-like: runs time-consuming work on a background thread and passes intermediate results and the end to the UI thread.
 *
 * With [execute], a background thread (up to 10 daemon threads, the same as Swing) calls [doInBackground]. Values [publish]ed in it
 * are delivered together to [process] on the UI thread, and changes of [progress] to the "progress" [WPropertyChangeListener] on the UI thread.
 * When [doInBackground] ends (including by an exception or [cancel]), [done] is called on the UI thread, and then the change of "state" to [StateValue.DONE]
 * is notified. In [done], [get] gives the result ([java.util.concurrent.ExecutionException] for an exception).
 *
 * ```java
 * new WSwingWorker<Integer, String>() {
 *     protected Integer doInBackground() throws Exception { publish("half"); setProgress(50); return 42; }
 *     protected void process(List<String> chunks) { label.setText(chunks.get(chunks.size() - 1)); }
 *     protected void done() { try { label.setText("Result " + get()); } catch (Exception e) { ... } }
 * }.execute();
 * ```
 *
 * [T] is the type of the result of [doInBackground], and [V] the type of intermediate values passed with [publish]. A worker can run only once (as in Swing).
 * If WinUI is not started at the first [execute] / [publish], it is started and waited for.
 */
abstract class WSwingWorker<T, V> : RunnableFuture<T> {
    /** SwingWorker.StateValue-like: the state of the worker. */
    enum class StateValue {
        /** Not started yet. */
        PENDING,

        /** [doInBackground] has started. */
        STARTED,

        /** Finished (after [done] was called). */
        DONE,
    }

    private val future = object : FutureTask<T>(
        Callable {
            changeState(StateValue.STARTED)
            doInBackground()
        },
    ) {
        override fun done() {
            WinUiUtilities.invokeLater {
                try {
                    this@WSwingWorker.done()
                } finally {
                    changeState(StateValue.DONE)
                }
            }
        }
    }

    private val listeners = CopyOnWriteArrayList<WPropertyChangeListener>()

    /** Values [publish]ed and not passed to [process] yet. While this is not empty, [process] has been posted to the UI thread. */
    private val pendingChunks = ArrayList<V>()

    /** SwingWorker.getState-like: the current state. "state" notifications arrive on the UI thread. */
    @Volatile
    var state: StateValue = StateValue.PENDING
        private set

    /**
     * SwingWorker.getProgress / setProgress-like: the progress (0 to 100). Writing it in [doInBackground] delivers it to the "progress"
     * [WPropertyChangeListener] on the UI thread (when changed repeatedly, only the last value may arrive, as in Swing).
     * Out of range throws [IllegalArgumentException].
     */
    @Volatile
    var progress: Int = 0
        protected set(value) {
            require(value in 0..MAX_PROGRESS) { "the value should be from 0 to 100" }
            if (field == value) return
            val old = field
            field = value
            fireLater("progress", old, value)
        }

    /** SwingWorker.doInBackground-like: the work done on the background thread. The return value becomes the result of [get]. */
    @Throws(Exception::class)
    protected abstract fun doInBackground(): T

    /**
     * SwingWorker.process-like: receives the [publish]ed values on the UI thread (values of consecutive [publish] calls are passed together). Does nothing by default.
     * The parameter type has no `? extends` so that Java can override it with `process(List<V>)`.
     */
    protected open fun process(chunks: List<@JvmSuppressWildcards V>) {
        // Intermediate values are not used by default (as in Swing)
    }

    /** SwingWorker.done-like: called on the UI thread after [doInBackground] ends. Does nothing by default. */
    protected open fun done() {
        // Nothing is done at the end by default (as in Swing)
    }

    /** SwingWorker.publish-like: sends intermediate values to [process] on the UI thread (call it from [doInBackground]). */
    protected fun publish(vararg chunks: V) {
        if (chunks.isEmpty()) return
        val schedule = synchronized(pendingChunks) {
            val wasEmpty = pendingChunks.isEmpty()
            pendingChunks.addAll(chunks)
            wasEmpty
        }
        if (schedule) {
            WinUiUtilities.invokeLater {
                val taken = synchronized(pendingChunks) { ArrayList(pendingChunks).also { pendingChunks.clear() } }
                process(taken)
            }
        }
    }

    /** SwingWorker.execute-like: starts running on a background thread. Does nothing from the second call on. */
    fun execute() {
        EXECUTOR.execute(this)
    }

    /** SwingWorker.run-like: runs [doInBackground] on the calling thread (normally use [execute]). */
    override fun run() {
        future.run()
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = future.cancel(mayInterruptIfRunning)

    override fun isCancelled(): Boolean = future.isCancelled

    override fun isDone(): Boolean = future.isDone

    /** SwingWorker.get-like: waits until it ends and returns the result. Waiting on the UI thread freezes the screen, so normally call it in [done]. */
    override fun get(): T = future.get()

    override fun get(timeout: Long, unit: TimeUnit): T = future.get(timeout, unit)

    /** SwingWorker.addPropertyChangeListener-like: adds a listener receiving changes of "progress" and "state" on the UI thread. */
    fun addPropertyChangeListener(listener: WPropertyChangeListener?) {
        if (listener != null) listeners += listener
    }

    /** SwingWorker.removePropertyChangeListener-like: removes one added listener. */
    fun removePropertyChangeListener(listener: WPropertyChangeListener?) {
        if (listener != null) listeners.remove(listener)
    }

    /** SwingWorker.firePropertyChange-like: notifies the added listeners on the UI thread (not notified if the old and new values are the same). */
    fun firePropertyChange(propertyName: String, oldValue: Any?, newValue: Any?) {
        if (oldValue != null && oldValue == newValue) return
        val event = WPropertyChangeEvent(this, propertyName, oldValue, newValue)
        if (WinUiUtilities.isDispatchThread) {
            for (listener in listeners) listener.propertyChange(event)
        } else {
            WinUiUtilities.invokeLater { for (listener in listeners) listener.propertyChange(event) }
        }
    }

    private fun changeState(newState: StateValue) {
        val old = state
        state = newState
        fireLater("state", old, newState)
    }

    private fun fireLater(propertyName: String, oldValue: Any?, newValue: Any?) {
        if (listeners.isNotEmpty()) firePropertyChange(propertyName, oldValue, newValue)
    }

    private companion object {
        const val MAX_PROGRESS = 100

        /** Runs on up to 10 daemon threads, as Swing's SwingWorker does. */
        const val MAX_WORKER_THREADS = 10

        const val KEEP_ALIVE_SECONDS = 10L

        val EXECUTOR: ExecutorService by lazy {
            val count = AtomicInteger()
            val factory = ThreadFactory { task ->
                Thread(task, "WSwingWorker-pool-thread-${count.incrementAndGet()}").apply { isDaemon = true }
            }
            ThreadPoolExecutor(
                MAX_WORKER_THREADS,
                MAX_WORKER_THREADS,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                LinkedBlockingQueue(),
                factory,
            ).apply { allowCoreThreadTimeOut(true) }
        }
    }
}
