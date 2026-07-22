package com.appkitbox.winui4k.internal.com.lifetime

/**
 * Abstraction over "run a cleanup action exactly once when an object becomes GC-unreachable."
 *
 * Which underlying mechanism is available differs by Java version, so — just like
 * [Ffi][com.appkitbox.winui4k.internal.ffi.api.Ffi]'s backend — the implementation is
 * switched at runtime (the choice is made by [Cleanup]):
 *  - Java 9 and above: java.lang.ref.Cleaner + Reference.reachabilityFence ([CleanerCleanupBackend])
 *  - Java 8: PhantomReference + ReferenceQueue + a dedicated daemon thread ([PhantomCleanupBackend])
 */
internal interface CleanupBackend {
    val name: String

    /**
     * Registers [action] to run exactly once when [referent] becomes unreachable.
     *
     * [action] must not reference [referent] (even indirectly). Doing so would make the
     * registration itself hold a strong reference to [referent], and it would never become
     * unreachable.
     */
    fun register(referent: Any, action: Runnable): Cleanable

    /**
     * Guarantees to the JIT that [referent] is reachable up through the completion of this
     * call (a defense against premature finalization). On Java 9+ this is
     * Reference.reachabilityFence; on Java 8 it's the synchronized idiom, which relies on
     * the JMM property that the target of a monitor operation must be alive.
     */
    fun keepAlive(referent: Any)

    /** A registered cleanup action. [clean] is for explicit release (unregisters, and runs the action if it hasn't run yet). */
    interface Cleanable {
        fun clean()
    }
}
