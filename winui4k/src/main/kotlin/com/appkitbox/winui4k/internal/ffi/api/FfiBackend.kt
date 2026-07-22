package com.appkitbox.winui4k.internal.ffi.api

/**
 * A handle for calling a function pointer with a given [CallDescriptor]. Cached on the
 * backend side.
 *
 * Arguments are passed as Ptr / Int / Long / Double / Byte / Short / StructValue / null
 * (= a NULL pointer), matching the descriptor's corresponding [ArgKind].
 * The return value is Int / Long / Double / Byte / Short / Ptr / null (VOID), depending on ret.
 */
interface DowncallHandle {
    fun invoke(fn: Ptr, vararg args: Any?): Any?
}

/** A symbol-resolved native function (a function pointer bundled with its handle). */
class NativeFunction(private val fn: Ptr, private val handle: DowncallHandle) {
    operator fun invoke(vararg args: Any?): Any? = handle.invoke(fn, *args)
}

/**
 * The SPI for FFI backends. Panama (ffi.panama) is the default implementation; the
 * vocabulary is deliberately closed so a future JNA implementation (a winui4k-ffi-jna
 * module) can be added. Layers above this (com / winrt / winui) depend only on this
 * interface and the api types.
 */
interface FfiBackend {
    val name: String

    /** Loads [library] and returns the address of [symbol]. Throws [UnsatisfiedLinkError] if not found. */
    fun findSymbol(library: String, symbol: String): Ptr

    /** Returns the call handle for [descriptor]. Implementations should cache it keyed by the descriptor. */
    fun downcallHandle(descriptor: CallDescriptor): DowncallHandle

    /**
     * Turns a Kotlin lambda into a native function pointer (so it can be written into a
     * COM vtable). body receives arguments already converted according to the
     * descriptor (PTR -> [Ptr], Struct -> [StructValue]).
     * The stub lives for the process lifetime (never released).
     */
    fun upcallStub(descriptor: CallDescriptor, body: (Array<Any?>) -> Any?): Ptr

    /** A temporary allocation scope for the calling thread only (like Panama's Arena.ofConfined). */
    fun newConfinedScope(): MemoryScope

    /** A scope that lives for the process lifetime. Holds vtables, cached GUIDs / HSTRINGs, etc. */
    val globalScope: MemoryScope

    val memory: MemoryAccess
}

/** Resolves [symbol] in [library] into a callable [NativeFunction]. */
fun FfiBackend.function(library: String, symbol: String, descriptor: CallDescriptor): NativeFunction =
    NativeFunction(findSymbol(library, symbol), downcallHandle(descriptor))

/** Creates a temporary scope, passes it to [block], and reliably releases it afterward. */
inline fun <T> FfiBackend.withScope(block: (MemoryScope) -> T): T = newConfinedScope().use(block)

/**
 * The discovery SPI for backends (resolved via ServiceLoader). To provide a JNA backend
 * from a separate module, register an implementation of this interface under
 * META-INF/services.
 */
interface FfiBackendProvider {
    /** The name specified via the winui4k.ffi system property (e.g. "panama", "jna"). */
    val name: String

    /** Whether this backend is available in the current environment (e.g. whether java.lang.foreign exists). */
    fun isAvailable(): Boolean

    /** When multiple candidates exist, the higher value wins (Panama = 100, JNA = 50, JNR = 40 by convention). */
    val priority: Int

    fun create(): FfiBackend
}
