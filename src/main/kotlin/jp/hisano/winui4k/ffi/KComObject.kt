package jp.hisano.winui4k.ffi

import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * A COM object implemented on the Kotlin side.
 *
 * Builds a vtable (an array of Panama upcall stubs) in native memory and hands it to
 * the XAML side as a WinRT delegate (event handler), an overrides interface
 * (OnLaunched), or the outer object of COM aggregation (composing an Application
 * subclass).
 *
 * vtable layout:
 *   [0..2]  IUnknown  (QueryInterface / AddRef / Release)
 *   [3..5]  IInspectable (GetIids / GetRuntimeClassName / GetTrustLevel), when inspectable=true
 *   [6.. ]  the interface body's own methods (in winmd declaration order)
 */
class KComObject(
    private val runtimeClassName: String,
    private val inspectable: Boolean = true,
) {
    /** One ABI method. The descriptor must include the implicit this (leading ADDRESS). */
    class Method(val descriptor: FunctionDescriptor, val body: (Array<Any?>) -> Int)

    private val refCount = AtomicInteger(1)
    private val interfaces = LinkedHashMap<String, MemorySegment>() // iid(lowercase) -> COM ptr

    /**
     * The non-delegating IUnknown of the inner object from COM aggregation (WinRT
     * composition). Set this to the result of
     * IApplicationFactory.CreateInstance(outer, &inner, ...) so that QueryInterface
     * calls for IIDs we don't know about are forwarded to inner (the base Application
     * implementation from XAML).
     */
    @Volatile
    var innerUnknown: ComPtr? = null

    /** The pointer of the first interface added via addInterface (the actual IUnknown/IInspectable). */
    val primary: MemorySegment
        get() = interfaces.values.first()

    fun addInterface(iid: String, methods: List<Method>): KComObject {
        val vtblMethods = buildList {
            // --- IUnknown ---
            add(Method(QI_DESC) { args ->
                queryInterface(args[1] as MemorySegment, args[2] as MemorySegment)
            })
            add(Method(ComPtr.UNKNOWN_DESC) { _ -> refCount.incrementAndGet() })
            add(Method(ComPtr.UNKNOWN_DESC) { _ -> refCount.decrementAndGet() })
            // --- IInspectable ---
            if (inspectable) {
                add(Method(GET_IIDS_DESC) { args ->
                    (args[1] as MemorySegment).reinterpret(4).set(JAVA_INT, 0, 0)
                    (args[2] as MemorySegment).reinterpret(ADDRESS.byteSize())
                        .set(ADDRESS, 0, MemorySegment.NULL)
                    S_OK
                })
                add(Method(OUT_PTR_DESC) { args ->
                    (args[1] as MemorySegment).reinterpret(ADDRESS.byteSize())
                        .set(ADDRESS, 0, Hstring.ofCached(runtimeClassName))
                    S_OK
                })
                add(Method(OUT_PTR_DESC) { args ->
                    (args[1] as MemorySegment).reinterpret(4).set(JAVA_INT, 0, 0) // BaseTrust
                    S_OK
                })
            }
            addAll(methods)
        }

        val p = ADDRESS.byteSize()
        val vtbl = Native.arena.allocate(ADDRESS, vtblMethods.size.toLong())
        vtblMethods.forEachIndexed { i, m -> vtbl.setAtIndex(ADDRESS, i.toLong(), upcallStub(m)) }
        val obj = Native.arena.allocate(p) // struct { vtable* }
        obj.set(ADDRESS, 0, vtbl)
        interfaces[iid.lowercase()] = obj
        return this
    }

    private fun queryInterface(riid: MemorySegment, ppv: MemorySegment): Int {
        val iid = Guid.read(riid)
        val out = ppv.reinterpret(ADDRESS.byteSize())
        val match: MemorySegment? = when {
            iid == IID_IUNKNOWN -> primary
            inspectable && iid == IID_IINSPECTABLE -> primary
            iid == IID_IAGILE_OBJECT -> primary // declares that it is safe to use from any thread
            else -> interfaces[iid]
        }
        if (match != null) {
            out.set(ADDRESS, 0, match)
            refCount.incrementAndGet()
            return S_OK
        }
        // During aggregation: forward unknown IIDs to inner (the XAML implementation) as-is
        innerUnknown?.let { inner ->
            return inner.rawCall(0, QI_DESC, riid, ppv)
        }
        out.set(ADDRESS, 0, MemorySegment.NULL)
        return E_NOINTERFACE
    }

    /**
     * Turns a (Array<Any?>) -> Int lambda into a native function pointer with the given
     * descriptor. If an exception escapes to the native side it crashes the whole JVM,
     * so it must always be caught and mapped to E_FAIL.
     */
    private fun upcallStub(m: Method): MemorySegment {
        val invoker = Invoker(m.body)
        var h = LOOKUP.findVirtual(
            Invoker::class.java, "invoke",
            MethodType.methodType(Int::class.javaPrimitiveType, Array<Any?>::class.java),
        ).bindTo(invoker)
        h = h.asCollector(Array<Any?>::class.java, m.descriptor.argumentLayouts().size)
        h = h.asType(m.descriptor.toMethodType())
        return Native.linker.upcallStub(h, m.descriptor, Native.arena)
    }

    class Invoker(private val body: (Array<Any?>) -> Int) {
        @Suppress("unused") // invoked via MethodHandles
        fun invoke(args: Array<Any?>): Int = try {
            body(args)
        } catch (t: Throwable) {
            System.err.println("[winui4k] exception escaped from COM upcall:")
            t.printStackTrace()
            E_FAIL
        }
    }

    init {
        LIVE.add(this) // keep the upcall stubs from being GC'd (never released in this demo)
    }

    companion object {
        const val S_OK = 0
        val E_NOINTERFACE = 0x80004002.toInt()
        val E_FAIL = 0x80004005.toInt()

        const val IID_IUNKNOWN = "00000000-0000-0000-c000-000000000046"
        const val IID_IINSPECTABLE = "af86e2e0-b12d-4c6a-9c5a-d7aa65101e90"
        const val IID_IAGILE_OBJECT = "94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90"

        /** HRESULT f(this, riid, ppv) */
        val QI_DESC: FunctionDescriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)

        /** HRESULT f(this, out) */
        val OUT_PTR_DESC: FunctionDescriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)

        /** HRESULT GetIids(this, ULONG* count, IID** iids) */
        val GET_IIDS_DESC: FunctionDescriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)

        private val LOOKUP = MethodHandles.lookup()
        private val LIVE = CopyOnWriteArrayList<KComObject>()
    }
}
