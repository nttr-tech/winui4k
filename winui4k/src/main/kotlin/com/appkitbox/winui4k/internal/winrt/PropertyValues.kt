package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope

/**
 * Conversion between Kotlin values and IInspectable (boxing via Windows.Foundation.PropertyValue).
 * Used to pass values into Object-typed properties like Button.Content, or IReference<T>-typed properties.
 */
internal object PropertyValues {
    private const val IID_IPROPERTY_VALUE_STATICS = "629bdbc8-d932-4ff4-96b9-8d96c5c1e858"

    /** Windows.Foundation.IPropertyValue (the retrieval side of a boxed value). From Windows.Foundation.winmd. */
    private const val IID_IPROPERTY_VALUE = "4bd682dd-7554-40e9-9a9b-82654ede7e62"
    private const val IPropertyValue_GetBoolean = 18 // GetBoolean(out boolean)
    private const val IPropertyValue_GetString = 19 // GetString(out HSTRING)
    private const val IPropertyValue_GetInt32 = 11 // GetInt32(out i4)
    private const val IPropertyValueStatics_CreateString = 18 // CreateString(HSTRING, out IInspectable)
    private const val IPropertyValueStatics_CreateBoolean = 17 // CreateBoolean(boolean, out IInspectable)
    private const val IPropertyValueStatics_CreateInt32 = 10 // CreateInt32(i4, out IInspectable)
    private const val IPropertyValueStatics_CreateDouble = 15 // CreateDouble(r8, out IInspectable)

    private fun statics(): ComPtr =
        Activation.factory("Windows.Foundation.PropertyValue", IID_IPROPERTY_VALUE_STATICS)

    /** Boxes a Kotlin String into an IInspectable (PropertyValue.CreateString). */
    fun boxString(s: String): ComPtr {
        val statics = statics()
        return Hstring.use(s) { h ->
            val boxed = statics.getPtr(IPropertyValueStatics_CreateString, h)
            statics.release()
            boxed
        }
    }

    /**
     * The reverse of [boxString]: extracts the string if the IInspectable is a boxed string.
     * Returns null if it isn't a boxed string (PropertyValue), e.g. when it holds a UIElement.
     */
    fun unboxString(boxed: ComPtr): String? {
        val pv = boxed.queryInterfaceOrNull(IID_IPROPERTY_VALUE) ?: return null
        return try {
            pv.getString(IPropertyValue_GetString)
        } finally {
            pv.release()
        }
    }

    /**
     * Boxes a Kotlin Boolean into an IInspectable (PropertyValue.CreateBoolean).
     * Used to pass it to an IReference<Boolean>-typed property (ToggleButton.IsChecked).
     * WinRT's boolean is 1 byte, so the descriptor is given explicitly.
     */
    fun boxBool(value: Boolean): ComPtr {
        val statics = statics()
        return try {
            Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                statics.callWith(
                    IPropertyValueStatics_CreateBoolean,
                    CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.U8, ArgKind.PTR),
                    if (value) 1.toByte() else 0.toByte(),
                    out,
                )
                ComPtr(Ffi.backend.memory.getPtr(out, 0))
            }
        } finally {
            statics.release()
        }
    }

    /**
     * The reverse of [boxBool]: extracts the boolean if the IInspectable is a boxed boolean.
     * Returns null if it isn't a boxed boolean (PropertyValue).
     */
    fun unboxBool(boxed: ComPtr): Boolean? {
        val pv = boxed.queryInterfaceOrNull(IID_IPROPERTY_VALUE) ?: return null
        return try {
            pv.getBool(IPropertyValue_GetBoolean)
        } finally {
            pv.release()
        }
    }

    /**
     * Boxes a Kotlin Int into an IInspectable (PropertyValue.CreateInt32).
     * Used to pass it to an IReference<Int32>-typed property (OverlappedPresenter's
     * PreferredMinimum/MaximumWidth/Height). INT32 is an ordinary 4-byte argument, so it can be
     * passed as-is via [ComPtr.getPtr]'s automatic inference.
     */
    fun boxInt(value: Int): ComPtr {
        val statics = statics()
        return try {
            statics.getPtr(IPropertyValueStatics_CreateInt32, value)
        } finally {
            statics.release()
        }
    }

    /**
     * Boxes a Kotlin Double into an IInspectable (PropertyValue.CreateDouble).
     * Used to pass it to an IReference<Double>-typed parameter (ScrollViewer.ChangeView's offset).
     */
    fun boxDouble(value: Double): ComPtr {
        val statics = statics()
        return try {
            statics.getPtr(IPropertyValueStatics_CreateDouble, value)
        } finally {
            statics.release()
        }
    }

    /**
     * The reverse of [boxInt]: extracts the Int if the IInspectable is a boxed Int32.
     * Returns null if it isn't a boxed Int32 (PropertyValue).
     */
    fun unboxInt(boxed: ComPtr): Int? {
        val pv = boxed.queryInterfaceOrNull(IID_IPROPERTY_VALUE) ?: return null
        return try {
            pv.getInt(IPropertyValue_GetInt32)
        } finally {
            pv.release()
        }
    }
}
