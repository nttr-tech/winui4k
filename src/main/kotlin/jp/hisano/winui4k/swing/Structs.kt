package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_DOUBLE
import java.lang.foreign.ValueLayout.JAVA_INT

/**
 * Layouts and put helpers for XAML structs (passed by value).
 * All field orders are extracted from the winmd (same policy as Abi.kt).
 */
internal object XamlStructs {
    /** Microsoft.UI.Xaml.Thickness { double Left, Top, Right, Bottom } */
    val THICKNESS: MemoryLayout = MemoryLayout.structLayout(
        JAVA_DOUBLE.withName("Left"),
        JAVA_DOUBLE.withName("Top"),
        JAVA_DOUBLE.withName("Right"),
        JAVA_DOUBLE.withName("Bottom"),
    )

    /** Microsoft.UI.Xaml.CornerRadius { double TopLeft, TopRight, BottomRight, BottomLeft } */
    val CORNER_RADIUS: MemoryLayout = MemoryLayout.structLayout(
        JAVA_DOUBLE.withName("TopLeft"),
        JAVA_DOUBLE.withName("TopRight"),
        JAVA_DOUBLE.withName("BottomRight"),
        JAVA_DOUBLE.withName("BottomLeft"),
    )

    /** Microsoft.UI.Xaml.GridLength { double Value; GridUnitType (INT32) } + trailing padding */
    val GRID_LENGTH: MemoryLayout = MemoryLayout.structLayout(
        JAVA_DOUBLE.withName("Value"),
        JAVA_INT.withName("GridUnitType"),
        MemoryLayout.paddingLayout(4),
    )

    /** Windows.UI.Color { UINT8 A, R, G, B } */
    val COLOR: MemoryLayout = MemoryLayout.structLayout(
        JAVA_BYTE.withName("A"),
        JAVA_BYTE.withName("R"),
        JAVA_BYTE.withName("G"),
        JAVA_BYTE.withName("B"),
    )

    /** Puts a Thickness (double×4) by value. */
    fun putThickness(target: ComPtr, slot: Int, left: Double, top: Double, right: Double, bottom: Double) {
        Arena.ofConfined().use { a ->
            val t = a.allocate(THICKNESS)
            t.setAtIndex(JAVA_DOUBLE, 0, left)
            t.setAtIndex(JAVA_DOUBLE, 1, top)
            t.setAtIndex(JAVA_DOUBLE, 2, right)
            t.setAtIndex(JAVA_DOUBLE, 3, bottom)
            target.callWith(slot, FunctionDescriptor.of(JAVA_INT, ADDRESS, THICKNESS), t)
        }
    }

    /** Puts a CornerRadius (double×4) with the same value on all four corners, by value. */
    fun putCornerRadius(target: ComPtr, slot: Int, radius: Double) {
        Arena.ofConfined().use { a ->
            val c = a.allocate(CORNER_RADIUS)
            for (i in 0 until 4) c.setAtIndex(JAVA_DOUBLE, i.toLong(), radius)
            target.callWith(slot, FunctionDescriptor.of(JAVA_INT, ADDRESS, CORNER_RADIUS), c)
        }
    }

    /** Puts a GridLength by value (RowDefinition.Height / ColumnDefinition.Width). */
    fun putGridLength(target: ComPtr, slot: Int, value: Double, unitType: Int) {
        Arena.ofConfined().use { a ->
            val g = a.allocate(GRID_LENGTH)
            g.set(JAVA_DOUBLE, 0, value)
            g.set(JAVA_INT, 8, unitType)
            target.callWith(slot, FunctionDescriptor.of(JAVA_INT, ADDRESS, GRID_LENGTH), g)
        }
    }

    /** Puts a Windows.UI.Color (u8×4) by value (SolidColorBrush.Color). */
    fun putColor(target: ComPtr, slot: Int, alpha: Int, red: Int, green: Int, blue: Int) {
        Arena.ofConfined().use { a ->
            val c = a.allocate(COLOR)
            c.set(JAVA_BYTE, 0, alpha.toByte())
            c.set(JAVA_BYTE, 1, red.toByte())
            c.set(JAVA_BYTE, 2, green.toByte())
            c.set(JAVA_BYTE, 3, blue.toByte())
            target.callWith(slot, FunctionDescriptor.of(JAVA_INT, ADDRESS, COLOR), c)
        }
    }
}
