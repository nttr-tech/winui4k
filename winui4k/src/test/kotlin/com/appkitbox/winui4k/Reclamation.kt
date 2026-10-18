package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.Guid

/** Helper for checking in tests whether wrappers and native objects are collected. */
internal object Reclamation {
    /** Upper bound on how long to wait for collection. */
    private const val TIMEOUT_MILLIS = 5_000L
    private const val POLL_MILLIS = 50L

    /**
     * Repeats System.gc() and a short wait until [condition] becomes true (up to 5 seconds). Returns true if it does.
     * Release after a wrapper is collected runs in the UI thread's message loop, so call this from outside the UI thread
     * (the test thread).
     */
    @Suppress("ExplicitGarbageCollectionCall") // Prompts GC because the test verifies collection itself
    fun awaitWithGc(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
        while (true) {
            if (condition()) return true
            if (System.currentTimeMillis() > deadline) return false
            System.gc()
            Thread.sleep(POLL_MILLIS)
        }
    }

    /**
     * Whether the referent of [reference] still remains after prompting GC several times (verifies it is not collected).
     * There is no need to prompt GC as many times as [awaitCollected], which waits for collection, so this stops after
     * about a second.
     */
    @Suppress("ExplicitGarbageCollectionCall") // Prompts GC because the test verifies that it is not collected
    fun survivesGc(reference: java.lang.ref.WeakReference<*>): Boolean {
        repeat(SURVIVAL_GC_COUNT) {
            System.gc()
            Thread.sleep(POLL_MILLIS)
        }
        return reference.get() != null
    }

    private const val SURVIVAL_GC_COUNT = 20

    /** Waits until the referent of [reference] is collected. */
    fun awaitCollected(reference: java.lang.ref.WeakReference<*>): Boolean = awaitWithGc { reference.get() == null }

    /** Waits until the native object of [probe] is destroyed. */
    fun awaitDestroyed(probe: NativeProbe): Boolean = awaitWithGc { !onUiThreadGet { probe.isAlive() } }
}

/**
 * A weak reference to a native object (the IWeakReference obtained from IWeakReferenceSource.GetWeakReference).
 * It holds no strong reference, so it can tell whether the native object itself has been destroyed, independently of the
 * wrapper. Create it on the UI thread and read it on the UI thread.
 */
internal class NativeProbe(target: ComPtr) {
    private val weak: ComPtr = run {
        val source = target.queryInterface(IID_IWEAK_REFERENCE_SOURCE)
        try {
            source.getPtr(GET_WEAK_REFERENCE)
        } finally {
            source.release()
        }
    }

    /** Whether the native object has not been destroyed yet (IWeakReference.Resolve does not return null). */
    fun isAlive(): Boolean {
        val resolved = weak.getPtrOrNull(RESOLVE, Guid.of(IID_IINSPECTABLE)) ?: return false
        resolved.release()
        return true
    }

    private companion object {
        /** IWeakReferenceSource (weakreference.h). GetWeakReference(out IWeakReference) is vtbl[3]. */
        const val IID_IWEAK_REFERENCE_SOURCE = "00000038-0000-0000-c000-000000000046"
        const val GET_WEAK_REFERENCE = 3

        /** IWeakReference.Resolve(REFIID, out IInspectable) is vtbl[3]. */
        const val RESOLVE = 3
        const val IID_IINSPECTABLE = "af86e2e0-b12d-4c6a-9c5a-d7aa65101e90"
    }
}
