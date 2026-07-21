package jp.hisano.winui4k.ffi

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_SHORT
import java.lang.invoke.MethodHandle

/**
 * Bottom layer: access to the flat Win32 / WinRT API via Panama (java.lang.foreign).
 */
object Native {
    val linker: Linker = Linker.nativeLinker()

    /** An arena that lives for the process lifetime. Holds vtables, upcall stubs, cached GUIDs, etc. */
    val arena: Arena = Arena.ofShared()

    fun lookup(library: String): SymbolLookup = SymbolLookup.libraryLookup(library, arena)

    fun downcall(lookup: SymbolLookup, name: String, descriptor: FunctionDescriptor): MethodHandle =
        linker.downcallHandle(
            lookup.find(name).orElseThrow { UnsatisfiedLinkError("symbol not found: $name") },
            descriptor,
        )

    val combase: SymbolLookup by lazy { lookup("combase.dll") }
    private val user32: SymbolLookup by lazy { lookup("user32.dll") }
    private val kernel32: SymbolLookup by lazy { lookup("kernel32.dll") }

    private val roInitialize: MethodHandle by lazy {
        downcall(combase, "RoInitialize", FunctionDescriptor.of(JAVA_INT, JAVA_INT))
    }
    private val roUninitialize: MethodHandle by lazy {
        downcall(combase, "RoUninitialize", FunctionDescriptor.ofVoid())
    }
    private val setProcessDpiAwarenessContext: MethodHandle by lazy {
        downcall(user32, "SetProcessDpiAwarenessContext", FunctionDescriptor.of(JAVA_INT, ADDRESS))
    }

    fun checkHr(hr: Int, what: String) {
        if (hr < 0) {
            val detail = RestrictedError.messageFor(hr)
            throw WindowsRuntimeException(if (detail != null) "$what [$detail]" else what, hr)
        }
    }

    /** Initializes the UI thread as STA (equivalent to C#'s [STAThread]). */
    fun roInitializeSta() {
        val hr = roInitialize.invokeWithArguments(0) as Int // RO_INIT_SINGLETHREADED
        checkHr(hr, "RoInitialize")
    }

    /**
     * Closes the COM apartment. Deferred COM releases (which involve upcalls) must be
     * finished while the JVM is still alive; otherwise an upcall from a native thread
     * during JVM shutdown fails to attach and aborts the whole JVM.
     */
    fun roUninitialize() {
        runCatching { roUninitialize.invokeWithArguments() }
    }

    /** Enables Per-Monitor v2 DPI awareness. java.exe has no manifest, so this is done in code. */
    fun enablePerMonitorDpiAwareness() {
        // DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2 = (HANDLE)-4
        setProcessDpiAwarenessContext.invokeWithArguments(MemorySegment.ofAddress(-4L))
    }

    private val getModuleHandleW: MethodHandle by lazy {
        downcall(kernel32, "GetModuleHandleW", FunctionDescriptor.of(ADDRESS, ADDRESS))
    }
    private val getModuleFileNameW: MethodHandle by lazy {
        downcall(kernel32, "GetModuleFileNameW", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT))
    }

    /** Returns the full path of a loaded module, or null if it isn't loaded. */
    fun moduleFilePath(module: String): String? = Arena.ofConfined().use { a ->
        val name = a.allocate(JAVA_CHAR, (module.length + 1).toLong())
        for (i in module.indices) name.setAtIndex(JAVA_CHAR, i.toLong(), module[i])
        name.setAtIndex(JAVA_CHAR, module.length.toLong(), '\u0000')
        val h = getModuleHandleW.invokeWithArguments(name) as MemorySegment
        if (h.address() == 0L) return null
        val buf = a.allocate(JAVA_CHAR, 32768)
        val n = getModuleFileNameW.invokeWithArguments(h, buf, 32768) as Int
        if (n <= 0) return null
        String(CharArray(n) { buf.getAtIndex(JAVA_CHAR, it.toLong()) })
    }
}

class WindowsRuntimeException(what: String, val hresult: Int) :
    RuntimeException("%s failed: HRESULT=0x%08X".format(what, hresult))

/**
 * Retrieves the detailed message (IRestrictedErrorInfo) for the most recent WinRT error.
 * WinRT APIs often leave a human-readable description in thread-local storage on
 * failure; used to pin down the cause of generic HRESULTs like E_FAIL.
 */
private object RestrictedError {
    private val getRestrictedErrorInfo by lazy {
        Native.downcall(
            Native.combase, "GetRestrictedErrorInfo",
            FunctionDescriptor.of(JAVA_INT, ADDRESS),
        )
    }
    private val sysFreeString by lazy {
        Native.downcall(Native.lookup("oleaut32.dll"), "SysFreeString", FunctionDescriptor.ofVoid(ADDRESS))
    }

    fun messageFor(failedHr: Int): String? = runCatching { fetch(failedHr) }.getOrNull()

    private fun fetch(failedHr: Int): String? = Arena.ofConfined().use { a ->
        val out = a.allocate(ADDRESS)
        val hr = getRestrictedErrorInfo.invokeWithArguments(out) as Int
        val infoPtr = out.get(ADDRESS, 0)
        if (hr != 0 || infoPtr.address() == 0L) return null
        val info = ComPtr(infoPtr)
        try {
            // IRestrictedErrorInfo::GetErrorDetails(BSTR* desc, HRESULT* err, BSTR* restrictedDesc, BSTR* sid) = vtbl[3]
            val desc = a.allocate(ADDRESS)
            val err = a.allocate(JAVA_INT)
            val restricted = a.allocate(ADDRESS)
            val sid = a.allocate(ADDRESS)
            info.call(3, desc, err, restricted, sid)
            val descStr = takeBstr(desc.get(ADDRESS, 0))
            val restrictedStr = takeBstr(restricted.get(ADDRESS, 0))
            takeBstr(sid.get(ADDRESS, 0))
            if (err.get(JAVA_INT, 0) != failedHr) return null // leftover from a different error
            listOf(descStr, restrictedStr).filter { it.isNotBlank() }.distinct()
                .joinToString(" / ").trim().ifBlank { null }
        } finally {
            info.release()
        }
    }

    /** Reads and frees a BSTR (length-prefixed UTF-16). */
    private fun takeBstr(b: MemorySegment): String {
        if (b.address() == 0L) return ""
        try {
            val byteLen = MemorySegment.ofAddress(b.address() - 4).reinterpret(4).get(JAVA_INT, 0)
            val src = b.reinterpret(byteLen.toLong())
            val chars = CharArray(byteLen / 2) { src.getAtIndex(JAVA_CHAR, it.toLong()) }
            return String(chars)
        } finally {
            sysFreeString.invokeWithArguments(b)
        }
    }
}

/** Native representation of a GUID (16 bytes, mixed little-endian layout). */
object Guid {
    private val cache = HashMap<String, MemorySegment>()

    /** Converts "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" to a native GUID struct (cached in the global arena). */
    @Synchronized
    fun of(iid: String): MemorySegment = cache.getOrPut(iid.lowercase()) {
        val hex = iid.replace("-", "")
        require(hex.length == 32) { "bad GUID: $iid" }
        val seg = arenaAlloc()
        seg.set(JAVA_INT, 0, hex.substring(0, 8).toLong(16).toInt())
        seg.set(JAVA_SHORT, 4, hex.substring(8, 12).toInt(16).toShort())
        seg.set(JAVA_SHORT, 6, hex.substring(12, 16).toInt(16).toShort())
        for (i in 0 until 8) {
            seg.set(JAVA_BYTE, 8L + i, hex.substring(16 + i * 2, 18 + i * 2).toInt(16).toByte())
        }
        seg
    }

    private fun arenaAlloc(): MemorySegment = Native.arena.allocate(16)

    /** Reads a native GUID as a lowercase canonical string. */
    fun read(ptr: MemorySegment): String {
        val g = if (ptr.byteSize() >= 16) ptr else ptr.reinterpret(16)
        val d1 = g.get(JAVA_INT, 0).toLong() and 0xFFFFFFFFL
        val d2 = g.get(JAVA_SHORT, 4).toInt() and 0xFFFF
        val d3 = g.get(JAVA_SHORT, 6).toInt() and 0xFFFF
        val b = ByteArray(8) { g.get(JAVA_BYTE, 8L + it) }
        return "%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x".format(
            d1, d2, d3, b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7],
        )
    }
}

/** The WinRT string type HSTRING. */
object Hstring {
    private val create = Native.downcall(
        Native.combase, "WindowsCreateString",
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS),
    )
    private val getRawBuffer = Native.downcall(
        Native.combase, "WindowsGetStringRawBuffer",
        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS),
    )
    private val delete = Native.downcall(
        Native.combase, "WindowsDeleteString",
        FunctionDescriptor.of(JAVA_INT, ADDRESS),
    )

    private val leakedCache = HashMap<String, MemorySegment>()

    /** Creates an HSTRING. The caller must release it with [free]. */
    fun of(s: String): MemorySegment = Arena.ofConfined().use { a ->
        val buf = a.allocate(JAVA_CHAR, (s.length + 1).toLong())
        for (i in s.indices) buf.setAtIndex(JAVA_CHAR, i.toLong(), s[i])
        buf.setAtIndex(JAVA_CHAR, s.length.toLong(), '\u0000')
        val out = a.allocate(ADDRESS)
        Native.checkHr(create.invokeWithArguments(buf, s.length, out) as Int, "WindowsCreateString")
        out.get(ADDRESS, 0)
    }

    /** An HSTRING reused for the process lifetime, e.g. a runtime class name (intentionally leaked). */
    @Synchronized
    fun ofCached(s: String): MemorySegment = leakedCache.getOrPut(s) { of(s) }

    fun read(h: MemorySegment): String {
        if (h.address() == 0L) return "" // a NULL HSTRING is the empty string
        return Arena.ofConfined().use { a ->
            val lenOut = a.allocate(JAVA_INT)
            val buf = getRawBuffer.invokeWithArguments(h, lenOut) as MemorySegment
            val n = lenOut.get(JAVA_INT, 0)
            val src = buf.reinterpret(n.toLong() * 2)
            val chars = CharArray(n) { src.getAtIndex(JAVA_CHAR, it.toLong()) }
            String(chars)
        }
    }

    fun free(h: MemorySegment) {
        if (h.address() != 0L) delete.invokeWithArguments(h)
    }

    /** Creates a temporary HSTRING, passes it to the block, and reliably frees it afterward. */
    inline fun <T> use(s: String, block: (MemorySegment) -> T): T {
        val h = of(s)
        try {
            return block(h)
        } finally {
            free(h)
        }
    }
}
