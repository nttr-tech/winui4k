package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.MemoryScope
import com.appkitbox.winui4k.internal.ffi.api.StructType
import com.appkitbox.winui4k.internal.ffi.api.StructType.Field
import com.appkitbox.winui4k.internal.ffi.api.StructValue
import com.appkitbox.winui4k.internal.ffi.api.ValueKind.F64
import com.appkitbox.winui4k.internal.ffi.api.ValueKind.I32
import com.appkitbox.winui4k.internal.ffi.api.ValueKind.I64
import com.appkitbox.winui4k.internal.ffi.api.ValueKind.PTR
import com.appkitbox.winui4k.internal.ffi.api.ValueKind.U16
import com.appkitbox.winui4k.internal.ffi.api.ValueKind.U8
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope

/**
 * Layouts and put helpers for XAML structs (passed by value).
 * Field order is extracted from the winmd for every one of them (same policy as XamlInterop.kt).
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

    /**
     * Windows.Foundation.Size { FLOAT Width, Height } (FoundationContract.winmd).
     * There's no F32 in [ValueKind], so this is out-argument-only: its bit pattern is read as an
     * I32 and decoded with Float.intBitsToFloat ([getSizeFloat]). Can't be passed by value (e.g. to
     * Measure).
     */
    val SIZE_FLOAT = StructType(
        "Windows.Foundation.Size",
        listOf(Field("Width", I32), Field("Height", I32)),
    )

    /** Windows.Graphics.PointInt32 { INT32 X, Y } (Windows.Graphics.winmd) */
    val POINT_INT32 = StructType(
        "Windows.Graphics.PointInt32",
        listOf(Field("X", I32), Field("Y", I32)),
    )

    /** Windows.Graphics.SizeInt32 { INT32 Width, Height } (Windows.Graphics.winmd) */
    val SIZE_INT32 = StructType(
        "Windows.Graphics.SizeInt32",
        listOf(Field("Width", I32), Field("Height", I32)),
    )

    /** Windows.Graphics.RectInt32 { INT32 X, Y, Width, Height } (Windows.Graphics.winmd) */
    val RECT_INT32 = StructType(
        "Windows.Graphics.RectInt32",
        listOf(Field("X", I32), Field("Y", I32), Field("Width", I32), Field("Height", I32)),
    )

    /**
     * Microsoft.UI.WindowId { UINT64 Value } (Microsoft.UI.winmd).
     * WinRT's u8 (unsigned 64-bit) has the same ABI representation as [I64], so it's used as-is.
     */
    val WINDOW_ID = StructType("Microsoft.UI.WindowId", listOf(Field("Value", I64)))

    /** Puts a Thickness (double×4) by value. */
    fun putThickness(target: ComPtr, slot: Int, left: Double, top: Double, right: Double, bottom: Double) {
        Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val thickness = scope.allocate(THICKNESS)
            memory.putDouble(thickness.ptr, 0, left)
            memory.putDouble(thickness.ptr, 8, top)
            memory.putDouble(thickness.ptr, 16, right)
            memory.putDouble(thickness.ptr, 24, bottom)
            target.call(slot, thickness)
        }
    }

    /** Puts a CornerRadius with the same radius on all four corners (double×4) by value. */
    fun putCornerRadius(target: ComPtr, slot: Int, radius: Double) {
        Ffi.backend.withScope { scope ->
            val cornerRadius = scope.allocate(CORNER_RADIUS)
            for (i in 0 until 4) Ffi.backend.memory.putDouble(cornerRadius.ptr, i * 8L, radius)
            target.call(slot, cornerRadius)
        }
    }

    /** Puts a GridLength by value (RowDefinition.Height / ColumnDefinition.Width). */
    fun putGridLength(target: ComPtr, slot: Int, value: Double, unitType: Int) {
        Ffi.backend.withScope { scope ->
            val gridLength = scope.allocate(GRID_LENGTH)
            Ffi.backend.memory.putDouble(gridLength.ptr, 0, value)
            Ffi.backend.memory.putInt(gridLength.ptr, 8, unitType)
            target.call(slot, gridLength)
        }
    }

    /** Puts a FontWeight (u2) by value (TextBlock.FontWeight). */
    fun putFontWeight(target: ComPtr, slot: Int, weight: Int) {
        Ffi.backend.withScope { scope ->
            val fontWeight = scope.allocate(FONT_WEIGHT)
            Ffi.backend.memory.putShort(fontWeight.ptr, 0, weight.toShort())
            target.call(slot, fontWeight)
        }
    }

    /** Gets a Windows.UI.Color (u8×4) via an out argument (e.g. ColorPicker.Color). */
    fun getColor(target: ComPtr, slot: Int): IntArray = Ffi.backend.withScope { scope ->
        val color = scope.allocate(COLOR)
        target.call(slot, color.ptr) // an out argument, so it's passed as a pointer
        IntArray(4) { Ffi.backend.memory.getByte(color.ptr, it.toLong()).toInt() and 0xFF } // A, R, G, B
    }

    /** Puts a Windows.UI.Color (u8×4) by value (SolidColorBrush.Color). */
    fun putColor(target: ComPtr, slot: Int, alpha: Int, red: Int, green: Int, blue: Int) {
        Ffi.backend.withScope { scope ->
            val color = scope.allocate(COLOR)
            val memory = Ffi.backend.memory
            memory.putByte(color.ptr, 0, alpha.toByte())
            memory.putByte(color.ptr, 1, red.toByte())
            memory.putByte(color.ptr, 2, green.toByte())
            memory.putByte(color.ptr, 3, blue.toByte())
            target.call(slot, color)
        }
    }

    /** Puts a PointInt32 (i4×2) by value (AppWindow.Move). */
    fun putPointInt32(target: ComPtr, slot: Int, x: Int, y: Int) {
        Ffi.backend.withScope { scope ->
            val point = scope.allocate(POINT_INT32)
            Ffi.backend.memory.putInt(point.ptr, 0, x)
            Ffi.backend.memory.putInt(point.ptr, 4, y)
            target.call(slot, point)
        }
    }

    /** Puts a SizeInt32 (i4×2) by value (AppWindow.Resize / IAppWindow2.ResizeClient). */
    fun putSizeInt32(target: ComPtr, slot: Int, width: Int, height: Int) {
        Ffi.backend.withScope { scope ->
            val size = scope.allocate(SIZE_INT32)
            Ffi.backend.memory.putInt(size.ptr, 0, width)
            Ffi.backend.memory.putInt(size.ptr, 4, height)
            target.call(slot, size)
        }
    }

    /**
     * Gets a Windows.Foundation.Size (r4×2) via an out argument (UIElement.DesiredSize).
     * Returned in [width, height] order (float widened to double).
     */
    fun getSizeFloat(target: ComPtr, slot: Int): DoubleArray = Ffi.backend.withScope { scope ->
        val size = scope.allocate(SIZE_FLOAT)
        target.call(slot, size.ptr) // an out argument, so it's passed as a pointer
        val memory = Ffi.backend.memory
        doubleArrayOf(
            Float.fromBits(memory.getInt(size.ptr, 0)).toDouble(),
            Float.fromBits(memory.getInt(size.ptr, 4)).toDouble(),
        )
    }

    /** Gets a PointInt32 (i4×2) via an out argument (AppWindow.Position). Returned in [x, y] order. */
    fun getPointInt32(target: ComPtr, slot: Int): IntArray = Ffi.backend.withScope { scope ->
        val point = scope.allocate(POINT_INT32)
        target.call(slot, point.ptr) // an out argument, so it's passed as a pointer
        intArrayOf(Ffi.backend.memory.getInt(point.ptr, 0), Ffi.backend.memory.getInt(point.ptr, 4))
    }

    /** Gets a SizeInt32 (i4×2) via an out argument (AppWindow.Size / IAppWindow2.ClientSize). Returned in [width, height] order. */
    fun getSizeInt32(target: ComPtr, slot: Int): IntArray = Ffi.backend.withScope { scope ->
        val size = scope.allocate(SIZE_INT32)
        target.call(slot, size.ptr)
        intArrayOf(Ffi.backend.memory.getInt(size.ptr, 0), Ffi.backend.memory.getInt(size.ptr, 4))
    }

    /**
     * Gets a RectInt32 (i4×4) via an out argument (DisplayArea.OuterBounds / WorkArea).
     * Returned in [x, y, width, height] order.
     */
    fun getRectInt32(target: ComPtr, slot: Int): IntArray = Ffi.backend.withScope { scope ->
        val rect = scope.allocate(RECT_INT32)
        target.call(slot, rect.ptr)
        val memory = Ffi.backend.memory
        intArrayOf(
            memory.getInt(rect.ptr, 0), memory.getInt(rect.ptr, 4),
            memory.getInt(rect.ptr, 8), memory.getInt(rect.ptr, 12),
        )
    }

    /** Gets a WindowId (u8 Value) via an out argument (AppWindow.Id / OwnerWindowId). */
    fun getWindowId(target: ComPtr, slot: Int): Long = Ffi.backend.withScope { scope ->
        val windowId = scope.allocate(WINDOW_ID)
        target.call(slot, windowId.ptr)
        Ffi.backend.memory.getLong(windowId.ptr, 0)
    }

    /**
     * Turns a WindowId (u8 Value) into a [StructValue] for passing by value.
     * Used when it needs to be passed alongside other arguments (like a presenter pointer) in the
     * same call, so this only prepares the value without making the call itself (use it inside the
     * caller's own `Ffi.backend.withScope`).
     */
    fun windowIdValue(scope: MemoryScope, value: Long): StructValue {
        val windowId = scope.allocate(WINDOW_ID)
        Ffi.backend.memory.putLong(windowId.ptr, 0, value)
        return windowId
    }
}
