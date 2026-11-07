package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.text.DecimalFormat
import java.text.ParsePosition

/**
 * A COM object implemented in Kotlin, passed to NumberBox.NumberFormatter, that turns numbers into text and reads text as numbers with java.text.DecimalFormat
 * ([WSpinner.NumberEditor]). NumberBox requires the formatter (Windows.Globalization.NumberFormatting.INumberFormatter2) to also implement
 * parsing (INumberParser) (ValidateNumberFormatter in NumberBox.cpp), so one object has both.
 * NumberBox uses only FormatDouble (display) and ParseDouble (committing input; for an expression, each number in it).
 *
 * Parsing returns a value only when the whole text reads as a number of the format, and null otherwise (NumberBox restores the display without changing the value).
 * For expression input (AcceptsExpression), NumberBox passes the text up to an operator or space as one number.
 * DecimalFormat is not thread-safe, but NumberBox calls it only from the UI thread.
 */
internal object DecimalFormatNumberFormatter {
    /** Creates a COM object that formats and parses with [format] (creation reference 1; release it after passing it to NumberBox). */
    fun create(format: DecimalFormat): KComObject =
        KComObject("WinUI4K.DecimalFormatNumberFormatter")
            .addInterface(
                XamlInterop.IID_INumberFormatter2,
                listOf(
                    // vtbl[6] FormatInt(i8, out HSTRING)
                    KComObject.Method(DESC_I64_OUT) { args -> putString(args[2] as Ptr, format.format(args[1] as Long)) },
                    // vtbl[7] FormatUInt(u8, out HSTRING). Read as an unsigned 64-bit value
                    KComObject.Method(DESC_I64_OUT) { args ->
                        putString(args[2] as Ptr, format.format(java.lang.Long.toUnsignedString(args[1] as Long).toBigInteger()))
                    },
                    // vtbl[8] FormatDouble(r8, out HSTRING)
                    KComObject.Method(DESC_F64_OUT) { args -> putString(args[2] as Ptr, format.format(args[1] as Double)) },
                ),
            )
            .addInterface(
                XamlInterop.IID_INumberParser,
                listOf(
                    // vtbl[6] ParseInt(HSTRING, out IReference<i8>). Only values representable as integers
                    KComObject.Method(DESC_PTR_OUT) { args ->
                        val number = parse(format, Hstring.read(args[1] as Ptr))
                        val whole = number?.toDouble()?.takeIf { it == Math.rint(it) && it >= Long.MIN_VALUE && it <= Long.MAX_VALUE }
                        putReference(args[2] as Ptr, whole?.let { PropertyValues.boxLong(it.toLong()) }, FoundationInterop.IID_IReference_Int64)
                    },
                    // vtbl[7] ParseUInt(HSTRING, out IReference<u8>). NumberBox does not call it, so it returns unreadable (null)
                    KComObject.Method(DESC_PTR_OUT) { args -> putReference(args[2] as Ptr, null, "") },
                    // vtbl[8] ParseDouble(HSTRING, out IReference<r8>)
                    KComObject.Method(DESC_PTR_OUT) { args ->
                        val number = parse(format, Hstring.read(args[1] as Ptr))
                        putReference(
                            args[2] as Ptr,
                            number?.let { PropertyValues.boxDouble(it.toDouble()) },
                            FoundationInterop.IID_IReference_Double,
                        )
                    },
                ),
            )

    /** Reads the whole [text] as a number of [format] (ignoring leading and trailing spaces). null if it cannot be read. */
    private fun parse(format: DecimalFormat, text: String): Number? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val position = ParsePosition(0)
        val number = format.parse(trimmed, position) ?: return null
        return number.takeIf { position.index == trimmed.length }
    }

    /** Writes [value] to an out HSTRING (ownership passes to the caller). */
    private fun putString(out: Ptr, value: String): Int {
        Ffi.backend.memory.putPtr(out, 0, Hstring.of(value))
        return KComObject.S_OK
    }

    /** Writes to an out IReference the box [boxed] (ownership taken over) queried for [iid]. null if null (unreadable). */
    private fun putReference(out: Ptr, boxed: ComPtr?, iid: String): Int {
        if (boxed == null) {
            Ffi.backend.memory.putPtr(out, 0, Ptr.NULL)
            return KComObject.S_OK
        }
        try {
            // The reference added by the query is passed to the caller (not released here)
            Ffi.backend.memory.putPtr(out, 0, boxed.queryInterface(iid).ptr)
        } finally {
            boxed.release()
        }
        return KComObject.S_OK
    }

    private val DESC_I64_OUT = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I64, ArgKind.PTR)
    private val DESC_F64_OUT = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.F64, ArgKind.PTR)
    private val DESC_PTR_OUT = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
}
