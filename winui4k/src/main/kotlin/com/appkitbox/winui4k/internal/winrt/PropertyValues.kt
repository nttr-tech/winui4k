package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.Abi

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
    private const val IPropertyValueStatics_CreateDateTime = 21 // CreateDateTime(DateTime i8, out IInspectable)
    private const val IPropertyValueStatics_CreateTimeSpan = 22 // CreateTimeSpan(TimeSpan i8, out IInspectable)
    private const val IPropertyValue_GetDateTime = 21 // GetDateTime(out DateTime i8)
    private const val IPropertyValue_GetTimeSpan = 22 // GetTimeSpan(out TimeSpan i8)

    /**
     * Process-lifetime cache of the PropertyValue statics factory. box is a hot path used by
     * Content assignment and list-item insertion, so this avoids a per-call RoGetActivationFactory
     * (the statics factory is agile, so it's safe to reuse across threads).
     */
    private val statics: ComPtr by lazy {
        Activation.factory("Windows.Foundation.PropertyValue", IID_IPROPERTY_VALUE_STATICS)
    }

    /** Boxes a Kotlin String into an IInspectable (PropertyValue.CreateString). */
    fun boxString(value: String): ComPtr =
        Hstring.use(value) { hstring -> statics.getPtr(IPropertyValueStatics_CreateString, hstring) }

    /**
     * The reverse of [boxString]: extracts the string if the IInspectable is a boxed string.
     * Returns null if it isn't a boxed string (PropertyValue), e.g. when it holds a UIElement.
     */
    fun unboxString(boxed: ComPtr): String? {
        val propertyValue = boxed.queryInterfaceOrNull(IID_IPROPERTY_VALUE) ?: return null
        return try {
            propertyValue.getString(IPropertyValue_GetString)
        } finally {
            propertyValue.release()
        }
    }

    /**
     * Boxes a Kotlin Boolean into an IInspectable (PropertyValue.CreateBoolean).
     * Used to pass it to an IReference<Boolean>-typed property (ToggleButton.IsChecked).
     * WinRT's boolean is 1 byte, so the descriptor is given explicitly.
     */
    fun boxBool(value: Boolean): ComPtr = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        statics.callWith(
            IPropertyValueStatics_CreateBoolean,
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.U8, ArgKind.PTR),
            if (value) 1.toByte() else 0.toByte(),
            out,
        )
        ComPtr(Ffi.backend.memory.getPtr(out, 0))
    }

    /**
     * The reverse of [boxBool]: extracts the boolean if the IInspectable is a boxed boolean.
     * Returns null if it isn't a boxed boolean (PropertyValue).
     */
    fun unboxBool(boxed: ComPtr): Boolean? {
        val propertyValue = boxed.queryInterfaceOrNull(IID_IPROPERTY_VALUE) ?: return null
        return try {
            propertyValue.getBool(IPropertyValue_GetBoolean)
        } finally {
            propertyValue.release()
        }
    }

    /**
     * Boxes a Kotlin Int into an IInspectable (PropertyValue.CreateInt32).
     * Used to pass it to an IReference<Int32>-typed property (OverlappedPresenter's
     * PreferredMinimum/MaximumWidth/Height). INT32 is an ordinary 4-byte argument, so it can be
     * passed as-is via [ComPtr.getPtr]'s automatic inference.
     */
    fun boxInt(value: Int): ComPtr = statics.getPtr(IPropertyValueStatics_CreateInt32, value)

    /**
     * Boxes a Kotlin Double into an IInspectable (PropertyValue.CreateDouble).
     * Used to pass it to an IReference<Double>-typed parameter (ScrollViewer.ChangeView's offset).
     */
    fun boxDouble(value: Double): ComPtr = statics.getPtr(IPropertyValueStatics_CreateDouble, value)

    /**
     * The reverse of [boxInt]: extracts the Int if the IInspectable is a boxed Int32.
     * Returns null if it isn't a boxed Int32 (PropertyValue).
     */
    fun unboxInt(boxed: ComPtr): Int? {
        val propertyValue = boxed.queryInterfaceOrNull(IID_IPROPERTY_VALUE) ?: return null
        return try {
            propertyValue.getInt(IPropertyValue_GetInt32)
        } finally {
            propertyValue.release()
        }
    }

    /**
     * Boxes a Windows.Foundation.DateTime (100ns ticks since 1601-01-01 UTC) into an IInspectable.
     * Used to pass it to an IReference<DateTime>-typed property (CalendarDatePicker.Date).
     */
    fun boxDateTime(ticks: Long): ComPtr = statics.getPtr(IPropertyValueStatics_CreateDateTime, ticks)

    /**
     * The reverse of [boxDateTime]: extracts the 100ns ticks from an IReference<DateTime>.
     * Reads the by-value DateTime (i8) via IReference<T>.get_Value (vtbl[6]).
     */
    fun unboxDateTime(boxed: ComPtr): Long? {
        val reference = boxed.queryInterfaceOrNull(Abi.IID_IReference_DateTime) ?: return null
        return try {
            Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                reference.call(IREFERENCE_GET_VALUE, out)
                Ffi.backend.memory.getLong(out, 0)
            }
        } finally {
            reference.release()
        }
    }

    /**
     * Boxes a Windows.Foundation.TimeSpan (100ns ticks) into an IInspectable.
     * Used to pass it to an IReference<TimeSpan>-typed property (TimePicker.SelectedTime).
     */
    fun boxTimeSpan(ticks: Long): ComPtr = statics.getPtr(IPropertyValueStatics_CreateTimeSpan, ticks)

    /**
     * The reverse of [boxTimeSpan]: extracts the 100ns ticks from an IReference<TimeSpan>.
     */
    fun unboxTimeSpan(boxed: ComPtr): Long? {
        val reference = boxed.queryInterfaceOrNull(Abi.IID_IReference_TimeSpan) ?: return null
        return try {
            Ffi.backend.withScope { scope ->
                val out = scope.allocate(8)
                reference.call(IREFERENCE_GET_VALUE, out)
                Ffi.backend.memory.getLong(out, 0)
            }
        } finally {
            reference.release()
        }
    }

    /** IReference<T>.get_Value — vtbl[6]. */
    private const val IREFERENCE_GET_VALUE = 6
}
