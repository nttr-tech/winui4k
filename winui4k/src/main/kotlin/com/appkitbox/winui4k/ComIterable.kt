package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.KComObject

/**
 * A Kotlin implementation that exposes a sequence of [size] elements as IIterable<T> (T is a runtime class or a struct).
 * Used to pass sequences to WinRT methods, such as IIterable<InkPoint> / IIterable<InkStroke> / IIterable<Point>.
 *
 * Elements are written to the out argument by [writeItem] (destination, element index). For runtime class elements the
 * contract transfers ownership, so write an AddRef'd pointer. [itemSize] is the size of one element in an array (bytes).
 * This assumes the receiver finishes enumerating during the call; release [comObject] when done.
 */
internal class ComIterable(
    name: String,
    iterableIid: String,
    private val iteratorIid: String,
    private val size: Int,
    private val itemSize: Long,
    private val writeItem: (out: Ptr, index: Int) -> Unit,
) {
    private val iteratorName = "$name.Iterator"

    /** The COM object passed as IIterable<T>. */
    val comObject: KComObject = KComObject(name)
        .addInterface(
            iterableIid,
            listOf(
                // vtbl[6] First(this, out IIterator<T>)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    // Passes the freshly created reference (count 1) straight into the out param;
                    // it's reclaimed by the caller's Release
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createIterator().primary)
                    KComObject.S_OK
                },
            ),
        )

    /** Creates an IIterator<T> implementation. Each First returns an independent cursor. */
    private fun createIterator(): KComObject {
        var index = 0
        return KComObject(iteratorName).addInterface(
            iteratorIid,
            listOf(
                // vtbl[6] get_Current(this, out T)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    if (index >= size) return@Method E_BOUNDS
                    writeItem(args[1] as Ptr, index)
                    KComObject.S_OK
                },
                // vtbl[7] get_HasCurrent(this, out boolean)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[8] MoveNext(this, out boolean)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    if (index < size) index++
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] GetMany(this, UINT32 capacity, T* items, out UINT32 actual)
                KComObject.Method(DESC_GET_MANY) { args ->
                    val capacity = args[1] as Int
                    val items = args[2] as Ptr
                    var written = 0
                    while (written < capacity && index < size) {
                        writeItem(Ptr(items.address + written * itemSize), index)
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
