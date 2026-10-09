package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.FoundationInterop

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
    private const val IPropertyValue_get_Type = 6 // get_Type(out PropertyType)
    private const val IPropertyValue_GetUInt8 = 8 // GetUInt8(out u1)
    private const val IPropertyValue_GetInt16 = 9 // GetInt16(out i2)
    private const val IPropertyValue_GetUInt16 = 10 // GetUInt16(out u2)
    private const val IPropertyValue_GetUInt32 = 12 // GetUInt32(out u4)
    private const val IPropertyValue_GetInt64 = 13 // GetInt64(out i8)
    private const val IPropertyValue_GetUInt64 = 14 // GetUInt64(out u8)
    private const val IPropertyValue_GetSingle = 15 // GetSingle(out r4)
    private const val IPropertyValue_GetDouble = 16 // GetDouble(out r8)
    private const val IPropertyValue_GetChar16 = 17 // GetChar16(out char16)
    private const val IPropertyValueStatics_CreateInt64 = 12 // CreateInt64(i8, out IInspectable)
    private const val IPropertyValueStatics_CreateString = 18 // CreateString(HSTRING, out IInspectable)
    private const val IPropertyValueStatics_CreateBoolean = 17 // CreateBoolean(boolean, out IInspectable)
    private const val IPropertyValueStatics_CreateInt32 = 10 // CreateInt32(i4, out IInspectable)
    private const val IPropertyValueStatics_CreateDouble = 15 // CreateDouble(r8, out IInspectable)
    private const val IPropertyValueStatics_CreateDateTime = 21 // CreateDateTime(DateTime i8, out IInspectable)
    private const val IPropertyValueStatics_CreateTimeSpan = 22 // CreateTimeSpan(TimeSpan i8, out IInspectable)

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
        val reference = boxed.queryInterfaceOrNull(FoundationInterop.IID_IReference_DateTime) ?: return null
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
        val reference = boxed.queryInterfaceOrNull(FoundationInterop.IID_IReference_TimeSpan) ?: return null
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

    /** Boxes a Kotlin Long into an IInspectable (PropertyValue.CreateInt64). */
    fun boxLong(value: Long): ComPtr = statics.getPtr(IPropertyValueStatics_CreateInt64, value)

    /**
     * Boxes a Kotlin value into an IInspectable. String / Boolean / Int / Long / Double and
     * other numbers (Byte / Short are widened to Int32, Float to Double) become a PropertyValue,
     * and Char becomes a one-character String. Returns null for null.
     * Any other type (objects such as Date) is boxed as its toString() string.
     */
    fun boxAny(value: Any?): ComPtr? = when (value) {
        null -> null
        is String -> boxString(value)
        is Boolean -> boxBool(value)
        is Int -> boxInt(value)
        is Long -> boxLong(value)
        is Double -> boxDouble(value)
        is Byte -> boxInt(value.toInt())
        is Short -> boxInt(value.toInt())
        is Float -> boxDouble(value.toDouble())
        is Char -> boxString(value.toString())
        else -> boxString(value.toString())
    }

    /**
     * The inverse of [boxAny]: extracts a scalar boxed in a PropertyValue as a Kotlin value
     * (String / Boolean / Int / Long / Double; unsigned integers become Int / Long and Single is widened to Double).
     * Returns null if it is not a PropertyValue (such as a UIElement) or if the PropertyType is not supported.
     */
    @Suppress("CyclomaticComplexMethod") // Extraction for each PropertyType is enumerated in a single when
    fun unboxAny(boxed: ComPtr): Any? {
        val propertyValue = boxed.queryInterfaceOrNull(IID_IPROPERTY_VALUE) ?: return null
        return try {
            when (propertyValue.getInt(IPropertyValue_get_Type)) {
                PROPERTY_TYPE_STRING -> propertyValue.getString(IPropertyValue_GetString)
                PROPERTY_TYPE_BOOLEAN -> propertyValue.getBool(IPropertyValue_GetBoolean)
                PROPERTY_TYPE_INT32 -> propertyValue.getInt(IPropertyValue_GetInt32)
                PROPERTY_TYPE_INT64 -> propertyValue.getLongValue(IPropertyValue_GetInt64)
                PROPERTY_TYPE_DOUBLE -> propertyValue.getDouble(IPropertyValue_GetDouble)
                PROPERTY_TYPE_SINGLE -> propertyValue.getSingle()
                PROPERTY_TYPE_UINT8 -> propertyValue.getInt(IPropertyValue_GetUInt8) and 0xFF
                PROPERTY_TYPE_INT16 -> propertyValue.getInt(IPropertyValue_GetInt16).toShort().toInt()
                PROPERTY_TYPE_UINT16 -> propertyValue.getInt(IPropertyValue_GetUInt16) and 0xFFFF
                PROPERTY_TYPE_UINT32 -> propertyValue.getInt(IPropertyValue_GetUInt32).toLong() and 0xFFFF_FFFFL
                PROPERTY_TYPE_UINT64 -> propertyValue.getLongValue(IPropertyValue_GetUInt64)
                PROPERTY_TYPE_CHAR16 -> (propertyValue.getInt(IPropertyValue_GetChar16) and 0xFFFF).toChar().toString()
                else -> null
            }
        } finally {
            propertyValue.release()
        }
    }

    /** Reads an i8 / u8 out parameter. */
    private fun ComPtr.getLongValue(slot: Int): Long = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        call(slot, out)
        Ffi.backend.memory.getLong(out, 0)
    }

    /** Reads an r4 out parameter (converts the 32-bit floating-point bit pattern back to Float and widens it to Double). */
    private fun ComPtr.getSingle(): Double = Ffi.backend.withScope { scope ->
        val out = scope.allocate(4)
        call(IPropertyValue_GetSingle, out)
        java.lang.Float.intBitsToFloat(Ffi.backend.memory.getInt(out, 0)).toDouble()
    }

    // Windows.Foundation.PropertyType (extracted from the winmd)
    private const val PROPERTY_TYPE_UINT8 = 1
    private const val PROPERTY_TYPE_INT16 = 2
    private const val PROPERTY_TYPE_UINT16 = 3
    private const val PROPERTY_TYPE_INT32 = 4
    private const val PROPERTY_TYPE_UINT32 = 5
    private const val PROPERTY_TYPE_INT64 = 6
    private const val PROPERTY_TYPE_UINT64 = 7
    private const val PROPERTY_TYPE_SINGLE = 8
    private const val PROPERTY_TYPE_DOUBLE = 9
    private const val PROPERTY_TYPE_CHAR16 = 10
    private const val PROPERTY_TYPE_BOOLEAN = 11
    private const val PROPERTY_TYPE_STRING = 12

    /** IReference<T>.get_Value — vtbl[6]. */
    private const val IREFERENCE_GET_VALUE = 6
}
