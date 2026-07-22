package com.appkitbox.winui4k.internal.ffi.api

/**
 * A native pointer (address value). A common representation independent of the FFI
 * backend (Panama / JNA).
 *
 * This package (ffi.api) is the SPI for backend implementers: it defines the only FFI
 * vocabulary in winui4k that doesn't depend on either java.lang.foreign or com.sun.jna.
 * It stays public because backends in other modules (a future winui4k-ffi-jna) implement it.
 */
@JvmInline
value class Ptr(val address: Long) {
    val isNull: Boolean
        get() = address == 0L

    companion object {
        val NULL = Ptr(0L)
    }
}

/**
 * The kind of a scalar value. This is the entire vocabulary winui4k needs for WinRT ABI
 * calls (keeping it a closed vocabulary makes it implementable even on JNA, which can't
 * represent arbitrary layouts).
 */
enum class ValueKind(val byteSize: Long) {
    PTR(8),
    I32(4),
    I64(8),
    F64(8),
    U8(1),
    U16(2),
    VOID(0),
    ;

    val alignment: Long
        get() = byteSize
}

/**
 * The layout of a struct passed by value. Fields are laid out with natural alignment
 * (shared by Windows x64 / ARM64). How the ABI actually passes it (registers or a
 * pointer) is resolved by each backend (Panama = MemoryLayout, JNA = Structure.ByValue),
 * so no lowering happens here.
 */
class StructType(val name: String, val fields: List<Field>) {
    class Field(val name: String, val kind: ValueKind)

    /** The byte offset of each field from the start of the struct. */
    val offsets: LongArray
    val byteSize: Long
    val alignment: Long

    init {
        require(fields.isNotEmpty()) { "empty struct: $name" }
        var offset = 0L
        var maxAlignment = 1L
        offsets = LongArray(fields.size)
        fields.forEachIndexed { i, field ->
            require(field.kind != ValueKind.VOID) { "VOID field in $name" }
            val fieldAlignment = field.kind.alignment
            offset = (offset + fieldAlignment - 1) / fieldAlignment * fieldAlignment
            offsets[i] = offset
            offset += field.kind.byteSize
            if (fieldAlignment > maxAlignment) maxAlignment = fieldAlignment
        }
        alignment = maxAlignment
        byteSize = (offset + maxAlignment - 1) / maxAlignment * maxAlignment
    }

    fun offsetOf(fieldName: String): Long = offsets[fields.indexOfFirst { it.name == fieldName }]

    override fun toString(): String = "StructType($name)"
}

/** The kind of a call argument: a scalar, or a struct passed by value. */
sealed interface ArgKind {
    data class Scalar(val kind: ValueKind) : ArgKind
    data class Struct(val type: StructType) : ArgKind

    companion object {
        val PTR = Scalar(ValueKind.PTR)
        val I32 = Scalar(ValueKind.I32)
        val I64 = Scalar(ValueKind.I64)
        val F64 = Scalar(ValueKind.F64)
        val U8 = Scalar(ValueKind.U8)
        val U16 = Scalar(ValueKind.U16)
    }
}

/**
 * A call signature. Has equals/hashCode, so it can key the downcall handle cache.
 * For COM vtable calls, remember to include the implicit this (the leading PTR) in args.
 */
data class CallDescriptor(val ret: ValueKind, val args: List<ArgKind>) {
    constructor(ret: ValueKind, vararg args: ArgKind) : this(ret, args.toList())
}

/** A struct-by-value call argument: the memory at [ptr], passed by value using [type]'s layout. */
class StructValue(val type: StructType, val ptr: Ptr)
