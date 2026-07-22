package com.appkitbox.winui4k.internal.com.lifetime

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.ref.Reference
import java.util.concurrent.ThreadFactory

/**
 * The [CleanupBackend] for Java 9 and above. Uses java.lang.ref.Cleaner and
 * Reference.reachabilityFence.
 *
 * The core module is compiled with -Xjdk-release=8, so it can't hold a compile-time
 * reference to Java 9 APIs; they're resolved at runtime via MethodHandle instead (this
 * API surface isn't wide enough to warrant splitting into a separate module the way
 * winui4k-ffi-panama is).
 */
internal class CleanerCleanupBackend private constructor(
    private val cleaner: Any,
    private val registerHandle: MethodHandle,
    private val cleanHandle: MethodHandle,
    private val fenceHandle: MethodHandle,
) : CleanupBackend {

    override val name: String = "cleaner"

    override fun register(referent: Any, action: Runnable): CleanupBackend.Cleanable {
        val cleanable = checkNotNull(registerHandle.invoke(cleaner, referent, action))
        return object : CleanupBackend.Cleanable {
            override fun clean() {
                cleanHandle.invoke(cleanable)
            }
        }
    }

    override fun keepAlive(referent: Any) {
        fenceHandle.invoke(referent)
    }

    companion object {
        /** Null if the Java 9+ APIs aren't available (the caller falls back to the Java 8 implementation). */
        fun createOrNull(threadFactory: ThreadFactory): CleanerCleanupBackend? = try {
            val lookup = MethodHandles.publicLookup()
            val cleanerClass = Class.forName("java.lang.ref.Cleaner")
            val cleanableClass = Class.forName("java.lang.ref.Cleaner\$Cleanable")
            val create = lookup.findStatic(
                cleanerClass,
                "create",
                MethodType.methodType(cleanerClass, ThreadFactory::class.java),
            )
            val register = lookup.findVirtual(
                cleanerClass,
                "register",
                MethodType.methodType(cleanableClass, Any::class.java, Runnable::class.java),
            )
            val clean = lookup.findVirtual(cleanableClass, "clean", MethodType.methodType(Void.TYPE))
            val fence = lookup.findStatic(
                Reference::class.java,
                "reachabilityFence",
                MethodType.methodType(Void.TYPE, Any::class.java),
            )
            CleanerCleanupBackend(checkNotNull(create.invoke(threadFactory)), register, clean, fence)
        } catch (_: Throwable) {
            null
        }
    }
}
