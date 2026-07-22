package com.appkitbox.winui4k.internal.com.lifetime

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import java.util.concurrent.ThreadFactory

/**
 * Selects and holds the [CleanupBackend] to use (the same structure as [Ffi]).
 *
 * Selection priority:
 *  1. The system property -Dwinui4k.lifetime=cleaner|phantom
 *  2. cleaner on Java 9+, phantom on Java 8
 *
 * How the Java version maps to the underlying mechanism (the FFI layer switches
 * independently, per [Ffi]):
 *
 * | Runtime        | Cleanup               | Fence                        | FFI          |
 * |----------------|------------------------|-------------------------------|--------------|
 * | Java 8         | PhantomReference       | synchronized idiom            | JNA / JNR    |
 * | Java 9-21      | java.lang.ref.Cleaner  | Reference.reachabilityFence   | JNA / JNR    |
 * | Java 22+       | java.lang.ref.Cleaner  | Reference.reachabilityFence   | Panama (FFM) |
 */
internal object Cleanup {

    /** The running JVM's feature version (8, 9, 11, 17, 21, 22, ...). */
    val javaFeatureVersion: Int = parseFeatureVersion(System.getProperty("java.specification.version"))

    val backend: CleanupBackend by lazy { load() }

    private fun load(): CleanupBackend {
        val factory = cleanerThreadFactory()
        val requested = System.getProperty("winui4k.lifetime")
        return when {
            "phantom".equals(requested, ignoreCase = true) -> PhantomCleanupBackend(factory)
            "cleaner".equals(requested, ignoreCase = true) ->
                CleanerCleanupBackend.createOrNull(factory)
                    ?: error("winui4k.lifetime=cleaner requires Java 9 or above (running: Java $javaFeatureVersion)")
            requested != null -> error("unknown winui4k.lifetime: $requested (cleaner or phantom)")
            javaFeatureVersion >= 9 ->
                CleanerCleanupBackend.createOrNull(factory) ?: PhantomCleanupBackend(factory)
            else -> PhantomCleanupBackend(factory)
        }
    }

    /**
     * The factory for the cleaner thread. On startup it joins COM via RoInitialize(MTA).
     * The cleanup action itself only posts to the UI thread, but that posting
     * (DispatcherQueue.TryEnqueue) is also a COM call, so we avoid calling it from a
     * thread whose apartment hasn't been initialized.
     */
    private fun cleanerThreadFactory(): ThreadFactory = ThreadFactory { runnable ->
        val thread = Thread({
            initializeMta()
            runnable.run()
        }, "WinUI4K-Cleaner")
        thread.isDaemon = true
        thread
    }

    private fun initializeMta() {
        runCatching {
            val roInitialize = Ffi.backend.function(
                "combase.dll",
                "RoInitialize",
                CallDescriptor(ValueKind.I32, ArgKind.I32),
            )
            roInitialize(1) // RO_INIT_MULTITHREADED
        }
    }

    /** "1.8" -> 8, "9" -> 9, "22" -> 22. Falls back to the most conservative value, 8, if unparsable. */
    private fun parseFeatureVersion(specVersion: String?): Int {
        if (specVersion == null) return 8
        val normalized = if (specVersion.startsWith("1.")) specVersion.substring(2) else specVersion
        return normalized.substringBefore('.').toIntOrNull() ?: 8
    }
}
