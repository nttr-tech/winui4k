package com.appkitbox.winui4k.internal.com.lifetime

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The [CleanupBackend] for Java 8. Reproduces the same structure as
 * java.lang.ref.Cleaner (Java 9+) using PhantomReference + ReferenceQueue + a daemon
 * thread. Unlike a finalizer, the object is never resurrected and the cleanup action
 * runs exactly once.
 */
internal class PhantomCleanupBackend(threadFactory: ThreadFactory) : CleanupBackend {

    override val name: String = "phantom"

    private val queue = ReferenceQueue<Any>()

    /** Strong references to live registrations (without this, the PhantomReference itself would be GC'd and the notification lost). */
    private val registrations = ConcurrentHashMap.newKeySet<Registration>()

    init {
        threadFactory.newThread(
            Runnable {
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        (queue.remove() as Registration).clean()
                    } catch (_: InterruptedException) {
                        return@Runnable
                    } catch (t: Throwable) {
                        // Don't let an exception from a cleanup action kill the cleaner thread
                        System.err.println("[winui4k] cleanup action failed:")
                        t.printStackTrace()
                    }
                }
            },
        ).start()
    }

    override fun register(referent: Any, action: Runnable): CleanupBackend.Cleanable =
        Registration(referent, action).also { registrations.add(it) }

    override fun keepAlive(referent: Any) {
        synchronized(referent) {}
    }

    private inner class Registration(
        referent: Any,
        private val action: Runnable,
    ) : PhantomReference<Any>(referent, queue), CleanupBackend.Cleanable {

        private val cleaned = AtomicBoolean(false)

        override fun clean() {
            clear()
            registrations.remove(this)
            if (cleaned.compareAndSet(false, true)) action.run()
        }
    }
}
