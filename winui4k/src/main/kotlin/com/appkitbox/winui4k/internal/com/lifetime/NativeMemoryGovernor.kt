package com.appkitbox.winui4k.internal.com.lifetime

import java.util.concurrent.atomic.AtomicLong

/**
 * An approximation of .NET's GC.AddMemoryPressure. The JVM has no API for reporting
 * arbitrary native allocations to the GC, so instead we count the number of live native
 * references and request System.gc() every time the count crosses a threshold, to
 * encourage the chain of wrapper collection -> Release.
 *
 * Opt-in: only active when -Dwinui4k.gcThreshold=<reference count> is specified
 * (disabled by default). Using it together with -XX:+ExplicitGCInvokesConcurrent is
 * recommended.
 */
internal object NativeMemoryGovernor {

    private val threshold: Long = java.lang.Long.getLong("winui4k.gcThreshold", 0L)

    /** The number of live (not yet released) native references. Also useful for leak diagnostics. */
    private val liveCount = AtomicLong()

    /** The live count at which System.gc() will next be requested. */
    private val nextTrigger = AtomicLong(threshold)

    val live: Long
        get() = liveCount.get()

    // A COM reference backing a large native-side resource looks small on the JVM heap, so GC
    // can't keep up; explicitly requesting a GC once the live count crosses a threshold is the whole point here
    @Suppress("ExplicitGarbageCollectionCall")
    fun onAdopted() {
        val live = liveCount.incrementAndGet()
        if (threshold <= 0) return
        val trigger = nextTrigger.get()
        if (live >= trigger && nextTrigger.compareAndSet(trigger, live + threshold)) {
            System.gc()
        }
    }

    fun onReleased() {
        liveCount.decrementAndGet()
    }
}
