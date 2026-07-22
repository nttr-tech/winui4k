package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.Guid
import com.appkitbox.winui4k.internal.com.checkHr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.function
import com.appkitbox.winui4k.internal.ffi.api.withScope

/** WinRT activation (instantiation via RoGetActivationFactory). */
internal object Activation {
    private const val IID_IACTIVATION_FACTORY = "00000035-0000-0000-c000-000000000046"

    private val roGetActivationFactory by lazy {
        Ffi.backend.function(
            "combase.dll", "RoGetActivationFactory",
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR),
        )
    }

    /** Gets the activation factory for a runtime class. */
    fun factory(runtimeClass: String, iid: String): ComPtr = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        val hr = roGetActivationFactory(Hstring.ofCached(runtimeClass), Guid.of(iid), out) as Int
        checkHr(hr, "RoGetActivationFactory($runtimeClass)")
        ComPtr(Ffi.backend.memory.getPtr(out, 0))
    }

    /** Equivalent to a default constructor (IActivationFactory::ActivateInstance). */
    fun activate(runtimeClass: String): ComPtr {
        val f = factory(runtimeClass, IID_IACTIVATION_FACTORY)
        try {
            return f.getPtr(6)
        } finally {
            f.release()
        }
    }

    /**
     * Instantiates a composable (inheritable) class.
     * Passing outer = NULL creates a plain, non-derived instance (the same convention
     * as C++/WinRT). A factory's CreateInstance is always vtbl[6]:
     *   HRESULT CreateInstance(IInspectable* outer, IInspectable** inner, T** instance)
     */
    fun composeDefault(runtimeClass: String, factoryIid: String): ComPtr =
        Ffi.backend.withScope { scope ->
            val f = factory(runtimeClass, factoryIid)
            try {
                val inner = scope.allocate(8)
                val instance = scope.allocate(8)
                f.call(6, null, inner, instance)
                // A non-derived instantiation doesn't use inner, so release the reference it returned.
                val innerPtr = Ffi.backend.memory.getPtr(inner, 0)
                if (!innerPtr.isNull) ComPtr(innerPtr).release()
                ComPtr(Ffi.backend.memory.getPtr(instance, 0))
            } finally {
                f.release()
            }
        }
}
