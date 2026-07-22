package jp.hisano.winui4k.internal.ffi.api

import jp.hisano.winui4k.internal.ffi.panama.PanamaBackendProvider
import java.util.ServiceLoader

/**
 * Selects and holds the FFI backend to use.
 *
 * Selection priority:
 *  1. Explicit choice via [setBackend] (only possible before the first FFI use)
 *  2. The system property -Dwinui4k.ffi=panama|jna
 *  3. The highest-priority available [FfiBackendProvider] discovered via ServiceLoader
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
        // The built-in Panama backend is always a candidate, without needing ServiceLoader
        // registration (this is the only static reference to ffi.panama)
        val providers = ServiceLoader.load(FfiBackendProvider::class.java).toList() + PanamaBackendProvider
        val requested = System.getProperty("winui4k.ffi")
        val provider = if (requested != null) {
            providers.firstOrNull { it.name.equals(requested, ignoreCase = true) }
                ?.takeIf { it.isAvailable() }
                ?: error("FFI backend '$requested' is not available (candidates: ${providers.joinToString { it.name }})")
        } else {
            providers.filter { it.isAvailable() }.maxByOrNull { it.priority }
                ?: error("no FFI backend is available")
        }
        return provider.create()
    }
}
