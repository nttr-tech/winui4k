package com.appkitbox.winui4k.internal.ffi.api

/**
 * A native memory allocation scope. close() releases everything allocated in it
 * (corresponds to Panama's Arena, or a group of JNA Memory instances).
 */
interface MemoryScope : AutoCloseable {
    fun allocate(byteSize: Long, alignment: Long = 8): Ptr
}

/** Allocates enough space for [type]'s layout and returns it as a [StructValue]. */
fun MemoryScope.allocate(type: StructType): StructValue =
    StructValue(type, allocate(type.byteSize, type.alignment))

/**
 * Reads and writes native memory at an arbitrary address. Bounds checking and alignment
 * handling are left to the backend (unaligned access must be tolerated).
 */
interface MemoryAccess {
    fun getByte(pointer: Ptr, offset: Long): Byte
    fun putByte(pointer: Ptr, offset: Long, value: Byte)
    fun getShort(pointer: Ptr, offset: Long): Short
    fun putShort(pointer: Ptr, offset: Long, value: Short)
    fun getInt(pointer: Ptr, offset: Long): Int
    fun putInt(pointer: Ptr, offset: Long, value: Int)
    fun getLong(pointer: Ptr, offset: Long): Long
    fun putLong(pointer: Ptr, offset: Long, value: Long)
    fun getDouble(pointer: Ptr, offset: Long): Double
    fun putDouble(pointer: Ptr, offset: Long, value: Double)
    fun getPtr(pointer: Ptr, offset: Long): Ptr
    fun putPtr(pointer: Ptr, offset: Long, value: Ptr)

    /** Reads a UTF-16 string of [chars] characters (for reading HSTRING / BSTR buffers). */
    fun getUtf16(pointer: Ptr, offset: Long, chars: Int): String

    /** Writes a NUL-terminated UTF-16 string (needs (length + 1) * 2 bytes). */
    fun putUtf16z(pointer: Ptr, offset: Long, value: String)
}
