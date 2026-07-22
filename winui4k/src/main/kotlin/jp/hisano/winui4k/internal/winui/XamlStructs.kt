package jp.hisano.winui4k.internal.winui

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.ffi.api.Ffi
import jp.hisano.winui4k.internal.ffi.api.StructType
import jp.hisano.winui4k.internal.ffi.api.StructType.Field
import jp.hisano.winui4k.internal.ffi.api.ValueKind.F64
import jp.hisano.winui4k.internal.ffi.api.ValueKind.I32
import jp.hisano.winui4k.internal.ffi.api.ValueKind.PTR
import jp.hisano.winui4k.internal.ffi.api.ValueKind.U16
import jp.hisano.winui4k.internal.ffi.api.ValueKind.U8
import jp.hisano.winui4k.internal.ffi.api.allocate
import jp.hisano.winui4k.internal.ffi.api.withScope

/**
 * Layouts and put helpers for XAML structs (passed by value).
 * Field order is extracted from the winmd for every one of them (same policy as Abi.kt).
 * Each backend resolves the by-value ABI lowering from the StructType's size
 * (Panama = MemoryLayout, JNA = manual Windows x64 ABI lowering), so no registration is needed here.
 */
internal object XamlStructs {
    /** Microsoft.UI.Xaml.Thickness { double Left, Top, Right, Bottom } */
    val THICKNESS = StructType(
        "Microsoft.UI.Xaml.Thickness",
        listOf(Field("Left", F64), Field("Top", F64), Field("Right", F64), Field("Bottom", F64)),
    )

    /** Microsoft.UI.Xaml.CornerRadius { double TopLeft, TopRight, BottomRight, BottomLeft } */
    val CORNER_RADIUS = StructType(
        "Microsoft.UI.Xaml.CornerRadius",
        listOf(Field("TopLeft", F64), Field("TopRight", F64), Field("BottomRight", F64), Field("BottomLeft", F64)),
    )

    /** Microsoft.UI.Xaml.GridLength { double Value; GridUnitType (INT32) } (trailing padding is automatic) */
    val GRID_LENGTH = StructType(
        "Microsoft.UI.Xaml.GridLength",
        listOf(Field("Value", F64), Field("GridUnitType", I32)),
    )

    /** Windows.UI.Text.FontWeight { UINT16 Weight } */
    val FONT_WEIGHT = StructType("Windows.UI.Text.FontWeight", listOf(Field("Weight", U16)))

    /** Windows.UI.Color { UINT8 A, R, G, B } */
    val COLOR = StructType(
        "Windows.UI.Color",
        listOf(Field("A", U8), Field("R", U8), Field("G", U8), Field("B", U8)),
    )

    /** Windows.UI.Xaml.Interop.TypeName { HSTRING Name; TypeKind Kind; } (trailing padding is automatic) */
    val TYPE_NAME = StructType(
        "Windows.UI.Xaml.Interop.TypeName",
        listOf(Field("Name", PTR), Field("Kind", I32)),
    )

    /** Puts a Thickness (double×4) by value. */
    fun putThickness(target: ComPtr, slot: Int, left: Double, top: Double, right: Double, bottom: Double) {
        Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val t = scope.allocate(THICKNESS)
            memory.putDouble(t.ptr, 0, left)
            memory.putDouble(t.ptr, 8, top)
            memory.putDouble(t.ptr, 16, right)
            memory.putDouble(t.ptr, 24, bottom)
            target.call(slot, t)
        }
    }

    /** Puts a CornerRadius with the same radius on all four corners (double×4) by value. */
    fun putCornerRadius(target: ComPtr, slot: Int, radius: Double) {
        Ffi.backend.withScope { scope ->
            val c = scope.allocate(CORNER_RADIUS)
            for (i in 0 until 4) Ffi.backend.memory.putDouble(c.ptr, i * 8L, radius)
            target.call(slot, c)
        }
    }

    /** Puts a GridLength by value (RowDefinition.Height / ColumnDefinition.Width). */
    fun putGridLength(target: ComPtr, slot: Int, value: Double, unitType: Int) {
        Ffi.backend.withScope { scope ->
            val g = scope.allocate(GRID_LENGTH)
            Ffi.backend.memory.putDouble(g.ptr, 0, value)
            Ffi.backend.memory.putInt(g.ptr, 8, unitType)
            target.call(slot, g)
        }
    }

    /** Puts a FontWeight (u2) by value (TextBlock.FontWeight). */
    fun putFontWeight(target: ComPtr, slot: Int, weight: Int) {
        Ffi.backend.withScope { scope ->
            val w = scope.allocate(FONT_WEIGHT)
            Ffi.backend.memory.putShort(w.ptr, 0, weight.toShort())
            target.call(slot, w)
        }
    }

    /** Gets a Windows.UI.Color (u8×4) via an out argument (e.g. ColorPicker.Color). */
    fun getColor(target: ComPtr, slot: Int): IntArray = Ffi.backend.withScope { scope ->
        val c = scope.allocate(COLOR)
        target.call(slot, c.ptr) // an out argument, so it's passed as a pointer
        IntArray(4) { Ffi.backend.memory.getByte(c.ptr, it.toLong()).toInt() and 0xFF } // A, R, G, B
    }

    /** Puts a Windows.UI.Color (u8×4) by value (SolidColorBrush.Color). */
    fun putColor(target: ComPtr, slot: Int, alpha: Int, red: Int, green: Int, blue: Int) {
        Ffi.backend.withScope { scope ->
            val c = scope.allocate(COLOR)
            val memory = Ffi.backend.memory
            memory.putByte(c.ptr, 0, alpha.toByte())
            memory.putByte(c.ptr, 1, red.toByte())
            memory.putByte(c.ptr, 2, green.toByte())
            memory.putByte(c.ptr, 3, blue.toByte())
            target.call(slot, c)
        }
    }
}
