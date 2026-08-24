package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winui.FoundationInterop

/**
 * A Kotlin implementation that exposes a List<String> as a read-only IVector<String>
 * (+ IVectorView / IIterable). Used to pass strings where the native side demands an
 * IVector<String>, such as FileSavePicker.FileTypeChoices values (lists of extensions).
 * Mutating methods (SetAt / Append, etc.) return E_NOTIMPL.
 */
internal class StringVector(private val items: List<String>) {
    /** The COM object handed to the WinRT side as an IVector<String>. */
    val comObject: KComObject = KComObject("WinUI4K.StringVector")
        .addInterface(
            FoundationInterop.IID_IVector_String,
            listOf(
                getAtMethod(),  // vtbl[6] GetAt(this, UINT32, out HSTRING)
                getSizeMethod(), // vtbl[7] get_Size(this, out UINT32)
                // vtbl[8] GetView(this, out IVectorView<String>)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    // Pass the creation reference (count 1) straight to the out param; the caller's Release reclaims it
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createView().primary)
                    KComObject.S_OK
                },
                indexOfMethod(), // vtbl[9] IndexOf(this, HSTRING, out UINT32, out boolean)
                KComObject.Method(DESC_SET_AT) { E_NOTIMPL },     // vtbl[10] SetAt
                KComObject.Method(DESC_SET_AT) { E_NOTIMPL },     // vtbl[11] InsertAt
                KComObject.Method(DESC_REMOVE_AT) { E_NOTIMPL },  // vtbl[12] RemoveAt
                KComObject.Method(DESC_THIS_PTR) { E_NOTIMPL },   // vtbl[13] Append
                KComObject.Method(DESC_THIS_ONLY) { E_NOTIMPL },  // vtbl[14] RemoveAtEnd
                KComObject.Method(DESC_THIS_ONLY) { E_NOTIMPL },  // vtbl[15] Clear
                getManyMethod(), // vtbl[16] GetMany(this, UINT32 startIndex, UINT32 capacity, HSTRING*, out UINT32)
                KComObject.Method(DESC_REPLACE_ALL) { E_NOTIMPL }, // vtbl[17] ReplaceAll
            ),
        )
        .addInterface(
            FoundationInterop.IID_IIterable_String,
            listOf(
                // vtbl[6] First(this, out IIterator<String>)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createIterator().primary)
                    KComObject.S_OK
                },
            ),
        )

    /** Creates the IVectorView<String> implementation (GetAt=6 get_Size=7 IndexOf=8 GetMany=9). */
    private fun createView(): KComObject = KComObject("WinUI4K.StringVectorView").addInterface(
        FoundationInterop.IID_IVectorView_String,
        listOf(getAtMethod(), getSizeMethod(), indexOfMethod(), getManyMethod()),
    )

    /** Creates the IIterator<String> implementation. Each First call returns an independent cursor. */
    private fun createIterator(): KComObject {
        var index = 0
        return KComObject("WinUI4K.StringVectorIterator").addInterface(
            FoundationInterop.IID_IIterator_String,
            listOf(
                // vtbl[6] get_Current(this, out HSTRING) — hands out a reference the caller releases
                KComObject.Method(DESC_THIS_PTR) { args ->
                    if (index >= items.size) return@Method E_BOUNDS
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, Hstring.of(items[index]))
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
                // vtbl[9] GetMany(this, UINT32 capacity, HSTRING* items, out UINT32 actual)
                KComObject.Method(DESC_ITERATOR_GET_MANY) { args ->
                    val capacity = args[1] as Int
                    val out = args[2] as Ptr
                    var written = 0
                    while (written < capacity && index < items.size) {
                        Ffi.backend.memory.putPtr(out, written.toLong() * 8, Hstring.of(items[index]))
                        written++
                        index++
                    }
                    Ffi.backend.memory.putInt(args[3] as Ptr, 0, written)
                    KComObject.S_OK
                },
            ),
        )
    }

    /** GetAt(this, UINT32 index, out HSTRING) — hands out a reference the caller releases. */
    private fun getAtMethod() = KComObject.Method(DESC_GET_AT) { args ->
        val index = args[1] as Int
        if (index !in items.indices) return@Method E_BOUNDS
        Ffi.backend.memory.putPtr(args[2] as Ptr, 0, Hstring.of(items[index]))
        KComObject.S_OK
    }

    /** get_Size(this, out UINT32). */
    private fun getSizeMethod() = KComObject.Method(DESC_THIS_PTR) { args ->
        Ffi.backend.memory.putInt(args[1] as Ptr, 0, items.size)
        KComObject.S_OK
    }

    /** IndexOf(this, HSTRING value, out UINT32 index, out boolean found). */
    private fun indexOfMethod() = KComObject.Method(DESC_INDEX_OF) { args ->
        val index = items.indexOf(Hstring.read(args[1] as Ptr))
        Ffi.backend.memory.putInt(args[2] as Ptr, 0, if (index >= 0) index else 0)
        Ffi.backend.memory.putByte(args[3] as Ptr, 0, if (index >= 0) 1 else 0)
        KComObject.S_OK
    }

    /** GetMany(this, UINT32 startIndex, UINT32 capacity, HSTRING* items, out UINT32 actual). */
    private fun getManyMethod() = KComObject.Method(DESC_VECTOR_GET_MANY) { args ->
        val startIndex = args[1] as Int
        val capacity = args[2] as Int
        val out = args[3] as Ptr
        var written = 0
        while (written < capacity && startIndex + written < items.size) {
            Ffi.backend.memory.putPtr(out, written.toLong() * 8, Hstring.of(items[startIndex + written]))
            written++
        }
        Ffi.backend.memory.putInt(args[4] as Ptr, 0, written)
        KComObject.S_OK
    }

    private companion object {
        val DESC_THIS_ONLY = CallDescriptor(ValueKind.I32, ArgKind.PTR)
        val DESC_THIS_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
        val DESC_GET_AT = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR)
        val DESC_INDEX_OF = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
        val DESC_SET_AT = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR)
        val DESC_REMOVE_AT = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32)
        val DESC_REPLACE_ALL = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR)
        val DESC_VECTOR_GET_MANY =
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
        val DESC_ITERATOR_GET_MANY = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
        val E_BOUNDS = 0x8000000B.toInt()
        val E_NOTIMPL = 0x80004001.toInt()
    }
}
