package com.appkitbox.winui4k

import java.util.concurrent.CopyOnWriteArrayList

/**
 * javax.swing.Timer-like: a timer that calls [WActionListener]s on the UI thread at a fixed interval (WinUI's DispatcherQueueTimer).
 *
 * It sends the first [WActionEvent] [initialDelay] milliseconds after [start], and if [isRepeats] is true (default) sends one every [delay] milliseconds after that.
 * If false, it sends one and stops. The listeners are called on the UI thread, so they can touch W* components directly (as in Swing).
 * [start] / [stop] / [restart] and the settings can be called from any thread. If WinUI is not started at the first [start], it is started and waited for.
 *
 * While running, the timer is referenced from the UI thread's queue and does not stop even if nothing else references it (as in Swing).
 * The [WActionEvent.getSource] of the events sent is this timer, and [WActionEvent.actionCommand] is [actionCommand].
 *
 * Differences from Swing: changing the interval ([delay]) takes effect after the next firing. [isCoalesce] only remembers the value, and firings delayed
 * because the UI thread is busy are always coalesced into one (DispatcherQueueTimer's behavior).
 */
class WTimer(delay: Int, listener: WActionListener?) {
    private val listeners = CopyOnWriteArrayList<WActionListener>()

    /** The running timer (null if stopped). Read and written inside [lock]. */
    private var running: AutoCloseable? = null

    private val lock = Any()

    init {
        require(delay >= 0) { "Invalid delay: $delay" }
        if (listener != null) listeners += listener
    }

    /** Timer.getDelay / setDelay-like: the interval between firings from the second on (milliseconds). Negative values throw [IllegalArgumentException] (as in Swing). */
    @Volatile
    var delay: Int = delay
        set(value) {
            require(value >= 0) { "Invalid delay: $value" }
            field = value
        }

    /** Timer.getInitialDelay / setInitialDelay-like: the time from [start] to the first firing (milliseconds). The default is the constructor's delay. */
    @Volatile
    var initialDelay: Int = delay
        set(value) {
            require(value >= 0) { "Invalid initial delay: $value" }
            field = value
        }

    /** Timer.isRepeats / setRepeats-like: whether to repeat. If false, it sends one and stops. The default is true. */
    @Volatile
    var isRepeats: Boolean = true

    /** Timer.isCoalesce / setCoalesce-like: whether to coalesce delayed firings. The default is true. It only remembers the value and always coalesces. */
    @Volatile
    var isCoalesce: Boolean = true

    /** Timer.getActionCommand / setActionCommand-like: the [WActionEvent.actionCommand] of the [WActionEvent]s sent. The default is null. */
    @Volatile
    var actionCommand: String? = null

    /** Timer.addActionListener-like: adds a listener called on each firing. Does nothing for null. */
    fun addActionListener(listener: WActionListener?) {
        if (listener != null) listeners += listener
    }

    /** Timer.removeActionListener-like: removes one listener added with [addActionListener]. */
    fun removeActionListener(listener: WActionListener?) {
        if (listener != null) listeners.remove(listener)
    }

    /** Timer.getActionListeners-like: the added listeners (in the order added). */
    val actionListeners: Array<WActionListener>
        get() = listeners.toTypedArray()

    /** Timer.start-like: starts the timer. Does nothing if it is already running. */
    fun start() {
        synchronized(lock) {
            if (running != null) return
            val repeats = isRepeats
            var handle: AutoCloseable? = null
            handle = WinUiUtilities.startTimer(initialDelay.toLong(), if (repeats) ({ delay.toLong() }) else null) {
                fire(handle)
            }
            running = handle
        }
    }

    /** Timer.stop-like: stops the timer. Does nothing if it is stopped. */
    fun stop() {
        synchronized(lock) {
            running?.close()
            running = null
        }
    }

    /** Timer.restart-like: stops it and starts it again (the first firing is after [initialDelay]). */
    fun restart() {
        synchronized(lock) {
            stop()
            start()
        }
    }

    /** Timer.isRunning-like: true if running. A non-repeating timer is false after it fires. */
    fun isRunning(): Boolean = synchronized(lock) { running != null }

    /** The timer of [handle] fired on the UI thread. Firings arriving after stopping (from an old timer) are not sent. */
    private fun fire(handle: AutoCloseable?) {
        synchronized(lock) {
            if (running == null || running !== handle) return
            if (!isRepeats) {
                running?.close()
                running = null
            }
        }
        val event = WActionEvent(this, actionCommand)
        for (listener in listeners) listener.actionPerformed(event)
    }
}
