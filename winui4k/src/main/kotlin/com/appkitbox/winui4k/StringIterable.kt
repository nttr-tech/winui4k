package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop

/**
 * A Kotlin implementation that exposes a List<String> as an IIterable<Object> (a sequence of boxed HSTRINGs).
 * Used to pass the value to ItemsControl.ItemsSource / BreadcrumbBar.ItemsSource.
 */
internal class StringIterable(private val items: List<String>) {
    /** The COM object passed to the XAML side as an IIterable<Object>. */
    val comObject: KComObject = KComObject("WinUI4K.StringIterable")
        .addInterface(
            FoundationInterop.IID_IIterable_Object,
            listOf(
                // vtbl[6] First(this, out IIterator<Object>)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    // Passes the freshly created reference (count 1) straight into the out param;
                    // it's reclaimed by the caller's Release
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createIterator().primary)
                    KComObject.S_OK
                },
            ),
        )

    /** Builds an IIterator<Object> implementation. Each call to First returns an independent cursor. */
    private fun createIterator(): KComObject {
        var index = 0
        return KComObject("WinUI4K.StringIterator").addInterface(
            FoundationInterop.IID_IIterator_Object,
            listOf(
                // vtbl[6] get_Current(this, out IInspectable) — hands the caller a reference it must release
                KComObject.Method(DESC_THIS_PTR) { args ->
                    if (index >= items.size) return@Method E_BOUNDS
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, PropertyValues.boxString(items[index]).ptr)
                    KComObject.S_OK
                },
                // vtbl[7] get_HasCurrent(this, out boolean)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < items.size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[8] MoveNext(this, out boolean)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    index++
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < items.size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] GetMany(this, UINT32 capacity, IInspectable* items, out UINT32 actual)
                KComObject.Method(DESC_GET_MANY) { args ->
                    val capacity = args[1] as Int
                    val out = args[2] as Ptr
                    var written = 0
                    while (written < capacity && index < items.size) {
                        Ffi.backend.memory.putPtr(out, written.toLong() * 8, PropertyValues.boxString(items[index]).ptr)
                        written++
                        index++
                    }
                    Ffi.backend.memory.putInt(args[3] as Ptr, 0, written)
                    KComObject.S_OK
                },
            ),
        )
    }

    private companion object {
        val DESC_THIS_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
        val DESC_GET_MANY = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
        val E_BOUNDS = 0x8000000B.toInt()
    }
}
