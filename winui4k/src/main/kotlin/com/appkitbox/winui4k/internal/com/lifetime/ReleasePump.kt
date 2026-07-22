package com.appkitbox.winui4k.internal.com.lifetime

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The submission point that funnels native reference releases (IUnknown::Release) onto
 * the UI thread.
 *
 * XAML objects are bound to the UI thread, so we never call Release directly from the
 * cleaner thread — it's always posted to the UI thread's message loop and executed
 * there instead. This means premature finalization — "a pointer getting released behind
 * the back of a method currently running on the UI thread" — structurally cannot happen
 * for calls on the UI thread (a release always happens after whatever UI work is
 * currently running has finished).
 *
 * The actual posting mechanism (DispatcherQueue) is injected by the winui layer via
 * [install] (the com layer can't depend on the winui layer).
 */
internal object ReleasePump {

    /** The posting implementation. A false return means the queue is shutting down, and the task is discarded (an intentional leak). */
    @Volatile
    private var executor: ((() -> Unit) -> Boolean)? = null

    /** Tasks that arrived before [install] (in case GC ran before the Dispatcher was captured). */
    private val pending = ConcurrentLinkedQueue<() -> Unit>()

    @Volatile
    private var shutdown = false

    /** Injects the means of posting to the UI thread, and drains any tasks that had piled up. */
    fun install(newExecutor: (task: () -> Unit) -> Boolean) {
        executor = newExecutor
        drainPending()
    }

    /**
     * Posts a release task. Callable from any thread.
     * Does nothing after [shutdown] (a Release after RoUninitialize would crash, so
     * anything left unreleased is left to process exit — the same trade-off CsWinRT makes).
     */
    fun submit(task: () -> Unit) {
        if (shutdown) return
        val current = executor
        if (current == null) {
            pending.add(task)
            if (executor != null) drainPending() // recover from a race with install
            return
        }
        current(task)
    }

    /** Stops all further releases. Call this before RoUninitialize. */
    fun shutdown() {
        shutdown = true
        executor = null
        pending.clear()
    }

    private fun drainPending() {
        val current = executor ?: return
        while (!shutdown) {
            val task = pending.poll() ?: return
            current(task)
        }
    }
}
