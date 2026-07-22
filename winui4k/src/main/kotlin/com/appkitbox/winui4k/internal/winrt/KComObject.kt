package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.Guid
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * A COM object implemented on the Kotlin side (lives in the winrt layer because it
 * depends on WinRT's IInspectable / HSTRING).
 *
 * Builds a vtable (an array of upcall stubs) in native memory and hands it to
 * the XAML side as a WinRT delegate (event handler), an overrides interface
 * (OnLaunched), or the outer object of COM aggregation (composing an Application
 * subclass).
 *
 * vtable layout:
 *   [0..2]  IUnknown  (QueryInterface / AddRef / Release)
 *   [3..5]  IInspectable (GetIids / GetRuntimeClassName / GetTrustLevel), when inspectable=true
 *   [6.. ]  the interface body's own methods (in winmd declaration order)
 */
internal class KComObject(
    private val runtimeClassName: String,
    private val inspectable: Boolean = true,
) {
    /** One ABI method. The descriptor must include the implicit this (leading PTR). */
    class Method(val descriptor: CallDescriptor, val body: (Array<Any?>) -> Int)

    private val refCount = AtomicInteger(1)
    private val interfaces = LinkedHashMap<String, Ptr>() // iid(lowercase) -> COM ptr

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
    val primary: Ptr
        get() = interfaces.values.first()

    fun addInterface(iid: String, methods: List<Method>): KComObject {
        val memory = Ffi.backend.memory
        val vtblMethods = buildList {
            // --- IUnknown ---
            add(Method(QI_DESC) { args ->
                queryInterface(args[1] as Ptr, args[2] as Ptr)
            })
            add(Method(ComPtr.UNKNOWN_DESC) { _ -> refCount.incrementAndGet() })
            add(Method(ComPtr.UNKNOWN_DESC) { _ -> refCount.decrementAndGet() })
            // --- IInspectable ---
            if (inspectable) {
                add(Method(GET_IIDS_DESC) { args ->
                    memory.putInt(args[1] as Ptr, 0, 0)
                    memory.putPtr(args[2] as Ptr, 0, Ptr.NULL)
                    S_OK
                })
                add(Method(OUT_PTR_DESC) { args ->
                    // GetRuntimeClassName's out parameter transfers ownership (the caller deletes it)
                    memory.putPtr(args[1] as Ptr, 0, Hstring.duplicate(Hstring.ofCached(runtimeClassName)))
                    S_OK
                })
                add(Method(OUT_PTR_DESC) { args ->
                    memory.putInt(args[1] as Ptr, 0, 0) // BaseTrust
                    S_OK
                })
            }
            addAll(methods)
        }

        val scope = Ffi.backend.globalScope
        val vtbl = scope.allocate(vtblMethods.size.toLong() * 8)
        vtblMethods.forEachIndexed { i, m -> memory.putPtr(vtbl, i.toLong() * 8, upcallStub(m)) }
        val obj = scope.allocate(8) // struct { vtable* }
        memory.putPtr(obj, 0, vtbl)
        interfaces[iid.lowercase()] = obj
        return this
    }

    private fun queryInterface(riid: Ptr, ppv: Ptr): Int {
        val memory = Ffi.backend.memory
        val iid = Guid.read(riid)
        val match: Ptr? = when {
            iid == IID_IUNKNOWN -> primary
            inspectable && iid == IID_IINSPECTABLE -> primary
            iid == IID_IAGILE_OBJECT -> primary // declares that it is safe to use from any thread
            else -> interfaces[iid]
        }
        if (match != null) {
            memory.putPtr(ppv, 0, match)
            refCount.incrementAndGet()
            return S_OK
        }
        // During aggregation: forward unknown IIDs to inner (the XAML implementation) as-is
        innerUnknown?.let { inner ->
            return inner.rawCall(0, QI_DESC, riid, ppv)
        }
        memory.putPtr(ppv, 0, Ptr.NULL)
        return E_NOINTERFACE
    }

    /**
     * Turns the method body into a native function pointer.
     * If an exception escapes to the native side it crashes the whole JVM, so it must
     * always be caught and mapped to E_FAIL.
     */
    private fun upcallStub(m: Method): Ptr = Ffi.backend.upcallStub(m.descriptor) { args ->
        try {
            m.body(args)
        } catch (t: Throwable) {
            System.err.println("[winui4k] exception escaped from COM upcall:")
            t.printStackTrace()
            E_FAIL
        }
    }

    init {
        LIVE.add(this) // keep the upcall stubs and their lambdas from being GC'd (never released)
    }

    companion object {
        const val S_OK = 0
        val E_NOINTERFACE = 0x80004002.toInt()
        val E_FAIL = 0x80004005.toInt()

        const val IID_IUNKNOWN = "00000000-0000-0000-c000-000000000046"
        const val IID_IINSPECTABLE = "af86e2e0-b12d-4c6a-9c5a-d7aa65101e90"
        const val IID_IAGILE_OBJECT = "94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90"

        /** HRESULT f(this, riid, ppv) */
        val QI_DESC: CallDescriptor = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

        /** HRESULT f(this, out) */
        val OUT_PTR_DESC: CallDescriptor = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)

        /** HRESULT GetIids(this, ULONG* count, IID** iids) */
        val GET_IIDS_DESC: CallDescriptor = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

        private val LIVE = CopyOnWriteArrayList<KComObject>()
    }
}
