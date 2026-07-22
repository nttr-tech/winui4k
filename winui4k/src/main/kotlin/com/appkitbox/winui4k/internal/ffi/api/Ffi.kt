package com.appkitbox.winui4k.internal.ffi.api

import java.util.ServiceConfigurationError
import java.util.ServiceLoader

/**
 * Selects and holds the FFI backend to use.
 *
 * Selection priority:
 *  1. Explicit choice via [setBackend] (only possible before the first FFI use)
 *  2. The system property -Dwinui4k.ffi=panama|jna|jnr
 *  3. The highest-priority available [FfiBackendProvider] discovered via ServiceLoader
 *     (registered by winui4k-panama / winui4k-jna / winui4k-jnr via META-INF/services)
 *
 * Once selected, the backend is fixed for the rest of the process (GUIDs / HSTRINGs /
 * upcall stubs are cached in globalScope, so switching backends mid-process isn't possible).
 */
object Ffi {
    @Volatile
    private var selected: FfiBackend? = null

    val backend: FfiBackend
        get() = selected ?: synchronized(this) {
            selected ?: load().also { selected = it }
        }

    /** Explicitly selects a backend. Only allowed before the first FFI use (= WinUI startup). */
    fun setBackend(backend: FfiBackend): Unit = synchronized(this) {
        check(selected == null) { "the FFI backend is already fixed: ${selected?.name}" }
        selected = backend
    }

    private fun load(): FfiBackend {
        // Look up via the SPI-defining class's loader rather than the TCCL (avoids mix-ups in app servers etc.)
        val loader = FfiBackendProvider::class.java.classLoader
        val iterator = ServiceLoader.load(FfiBackendProvider::class.java, loader).iterator()
        val providers = ArrayList<FfiBackendProvider>()
        while (true) {
            // On a Java 8 runtime, winui4k-panama (Java 22 bytecode) on the classpath can't be
            // loaded and throws UnsupportedClassVersionError, so each candidate is skipped individually
            // (Java 8's ServiceLoader doesn't wrap a LinkageError in a ServiceConfigurationError)
            try {
                if (!iterator.hasNext()) break
                providers.add(iterator.next())
            } catch (_: ServiceConfigurationError) {
            } catch (_: LinkageError) {
            }
        }
        val requested = System.getProperty("winui4k.ffi")
        val provider = if (requested != null) {
            providers.firstOrNull { it.name.equals(requested, ignoreCase = true) }
                ?.takeIf { it.isAvailable() }
                ?: error(
                    "FFI backend '$requested' is not available" +
                        " (candidates: ${providers.joinToString { it.name }.ifEmpty { "none" }}). $HINT",
                )
        } else {
            providers.filter { it.isAvailable() }.maxByOrNull { it.priority }
                ?: error("no FFI backend is available. $HINT")
        }
        return provider.create()
    }

    private const val HINT =
        "Add one of winui4k-panama (Java 22+), winui4k-jna (Windows x64, Java 8+), " +
            "or winui4k-jnr (Windows, Java 8+) as a runtime dependency"
}
