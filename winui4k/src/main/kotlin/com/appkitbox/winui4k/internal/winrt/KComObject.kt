package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.Guid
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

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
 *
 * Upcall stubs and vtables are expensive (native thunks backed by a globalScope that's
 * never freed), so only one is built per vtable shape (its sequence of descriptors) and
 * shared across every instance of that shape. The COM object itself is a 16-byte
 * { vtable*, instanceKey } block; the shared stub looks up the instanceKey in [REGISTRY]
 * to dispatch to that particular instance's method bodies.
 *
 * The reference count starts at 1 (the creation reference held on the Kotlin side). Once
 * a temporary object (an event handler, etc.) has been handed off to the native side,
 * call [release] to give up that creation reference. When the reference count reaches 0
 * the registration is removed, the Kotlin object becomes eligible for GC, and the COM
 * object block is returned to the pool for reuse.
 */
internal class KComObject(
    private val runtimeClassName: String,
    private val inspectable: Boolean = true,
) {
    /** One ABI method. The descriptor must include the implicit this (leading PTR). */
    class Method(val descriptor: CallDescriptor, val body: (Array<Any?>) -> Int)

    private val refCount = AtomicInteger(1)
    private val interfaces = LinkedHashMap<Guid.Bits, Ptr>() // iid -> COM ptr
    private val registrationKeys = ArrayList<Long>()

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
            add(Method(ComPtr.UNKNOWN_DESC) { _ -> releaseRef() })
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

        val vtbl = sharedVtable(vtblMethods.map { it.descriptor })
        val obj = OBJ_POOL.poll() ?: Ffi.backend.globalScope.allocate(16) // struct { vtable*; instanceKey }
        val key = NEXT_KEY.getAndIncrement()
        memory.putPtr(obj, 0, vtbl)
        memory.putLong(obj, 8, key)
        interfaces[Guid.bitsOf(iid)] = obj
        registrationKeys.add(key)
        REGISTRY[key] = vtblMethods // published here (safely visible to the stub via the CHM)
        return this
    }

    /**
     * Gives up the creation reference (initial count 1) held on the Kotlin side. Call this
     * on a temporary object once it has been handed off to the native side; it will then
     * be reclaimed on the native side's final Release.
     * Do not call this on a shared object meant to live for the process's lifetime.
     */
    fun release() {
        releaseRef()
    }

    private fun releaseRef(): Int {
        val rc = refCount.decrementAndGet()
        if (rc == 0) reclaim()
        return rc
    }

    /** Reference count reached 0: removes the registration so the Kotlin side becomes GC-eligible, and returns the obj block to the pool. */
    private fun reclaim() {
        registrationKeys.forEach { REGISTRY.remove(it) }
        interfaces.values.forEach { OBJ_POOL.add(it) }
        innerUnknown?.release()
        innerUnknown = null
    }

    private fun queryInterface(riid: Ptr, ppv: Ptr): Int {
        val memory = Ffi.backend.memory
        // QI is called frequently from the XAML side, so the GUID is compared as raw bits
        // instead of being formatted into a string
        val iid = Guid.readBits(riid)
        val match: Ptr? = when {
            iid == BITS_IUNKNOWN -> primary
            inspectable && iid == BITS_IINSPECTABLE -> primary
            iid == BITS_IAGILE_OBJECT -> primary // declares that it is safe to use from any thread
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

    companion object {
        const val S_OK = 0
        val E_NOINTERFACE = 0x80004002.toInt()
        val E_FAIL = 0x80004005.toInt()

        const val IID_IUNKNOWN = "00000000-0000-0000-c000-000000000046"
        const val IID_IINSPECTABLE = "af86e2e0-b12d-4c6a-9c5a-d7aa65101e90"
        const val IID_IAGILE_OBJECT = "94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90"

        private val BITS_IUNKNOWN = Guid.bitsOf(IID_IUNKNOWN)
        private val BITS_IINSPECTABLE = Guid.bitsOf(IID_IINSPECTABLE)
        private val BITS_IAGILE_OBJECT = Guid.bitsOf(IID_IAGILE_OBJECT)

        /** HRESULT f(this, riid, ppv) */
        val QI_DESC: CallDescriptor = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

        /** HRESULT f(this, out) */
        val OUT_PTR_DESC: CallDescriptor = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)

        /** HRESULT GetIids(this, ULONG* count, IID** iids) */
        val GET_IIDS_DESC: CallDescriptor = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

        /** instanceKey -> the vtable method bodies for that interface. A live registration is a GC root. */
        private val REGISTRY = ConcurrentHashMap<Long, List<Method>>()
        private val NEXT_KEY = AtomicLong(1)

        /** Reclaimed 16-byte obj blocks (globalScope memory can't be freed, so it's reused instead). */
        private val OBJ_POOL = ConcurrentLinkedQueue<Ptr>()

        /** The vtable shared per vtable shape (its sequence of descriptors). */
        private val VTABLES = ConcurrentHashMap<List<CallDescriptor>, Ptr>()

        private fun sharedVtable(shape: List<CallDescriptor>): Ptr =
            VTABLES.computeIfAbsent(shape) { descriptors ->
                val memory = Ffi.backend.memory
                val vtbl = Ffi.backend.globalScope.allocate(descriptors.size.toLong() * 8)
                descriptors.forEachIndexed { slot, descriptor ->
                    val stub = Ffi.backend.upcallStub(descriptor) { args -> dispatch(slot, args) }
                    memory.putPtr(vtbl, slot.toLong() * 8, stub)
                }
                vtbl
            }

        /**
         * Dispatches from a shared stub to an instance's method body, looking up the instanceKey
         * via the this pointer. If an exception escapes to the native side it crashes the whole
         * JVM, so it must always be caught and mapped to E_FAIL.
         */
        private fun dispatch(slot: Int, args: Array<Any?>): Int {
            val self = args[0] as Ptr
            val methods = REGISTRY[Ffi.backend.memory.getLong(self, 8)]
                ?: return E_FAIL // a call on an already-released object (the caller violated the reference count)
            return try {
                methods[slot].body(args)
            } catch (t: Throwable) {
                System.err.println("[winui4k] exception escaped from COM upcall:")
                t.printStackTrace()
                E_FAIL
            }
        }
    }
}
