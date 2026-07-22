package com.appkitbox.winui4k.internal.com.lifetime

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * The record of COM reference ownership. Bundles the reference counts owned by a single
 * wrapper object ([owner]) — one count for each deposited [ComPtr] — and ties them to
 * owner's lifetime.
 *
 * There are two paths to release (the same structure as CsWinRT's IObjectReference):
 *  1. The GC detects owner is unreachable -> [CleanupBackend]'s cleanup action -> Release
 *     on the UI thread via [ReleasePump]
 *  2. An explicit [close]
 * The race between the two paths is resolved by a 3-state CAS on [State.disposed], so
 * Release runs exactly once.
 *
 * [State] must be independent of owner (referencing owner would make the cleanup
 * registration itself hold a strong reference to owner, and it would never become
 * unreachable). Only this class holds a reference to owner; the cycle
 * owner -> ComLifetime -> owner is collected together by the GC.
 */
internal class ComLifetime private constructor(
    private val owner: Any,
    private val state: State,
    private val cleanable: CleanupBackend.Cleanable,
) {

    /**
     * Deposits ownership of one reference count of [ptr] into this record.
     * Use this when long-term holding an owned pointer obtained via QueryInterface / getPtr.
     */
    fun own(ptr: ComPtr): ComPtr {
        state.adopt(ptr.ptr)
        // Guarantees owner is still alive up to this point (i.e. that the registration completes before cleanup does)
        Cleanup.backend.keepAlive(owner)
        return ptr
    }

    /** Explicitly releases the owned references (also unregisters the GC-path cleanup). */
    fun close() {
        cleanable.clean()
    }

    companion object {
        /** Creates the ownership record for [owner], deposits ownership of [ptrs], and registers the GC-path cleanup. */
        fun adopt(owner: Any, vararg ptrs: ComPtr): ComLifetime {
            val state = State()
            for (ptr in ptrs) state.adopt(ptr.ptr)
            val cleanable = Cleanup.backend.register(owner, state)
            return ComLifetime(owner, state, cleanable)
        }

        private const val NOT_DISPOSED = 0
        private const val DISPOSE_PENDING = 1
        private const val DISPOSE_COMPLETED = 2
    }

    /** The release information passed to the cleanup action. Must not hold a reference to owner (see the class comment). */
    private class State : Runnable {

        /** The raw pointers whose reference counts are owned. */
        private val ptrs = ConcurrentLinkedQueue<Ptr>()

        private val disposed = AtomicInteger(NOT_DISPOSED)

        fun adopt(ptr: Ptr) {
            ptrs.add(ptr)
            NativeMemoryGovernor.onAdopted()
            // Ownership arriving after release has already completed (doesn't happen in normal usage) is released individually
            if (disposed.get() == DISPOSE_COMPLETED && ptrs.remove(ptr)) {
                ReleasePump.submit { releaseQuietly(ptr) }
            }
        }

        /** The meeting point of the GC-path cleanup (the cleaner thread) and [ComLifetime.close]. */
        override fun run() {
            if (!disposed.compareAndSet(NOT_DISPOSED, DISPOSE_PENDING)) return
            ReleasePump.submit {
                while (true) {
                    val ptr = ptrs.poll() ?: break
                    releaseQuietly(ptr)
                }
                disposed.set(DISPOSE_COMPLETED)
            }
        }

        private fun releaseQuietly(ptr: Ptr) {
            try {
                ComPtr(ptr).release()
            } catch (t: Throwable) {
                System.err.println("[winui4k] failed to release a COM reference: $t")
            }
            NativeMemoryGovernor.onReleased()
        }
    }
}
