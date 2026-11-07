package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * java.awt.Color-like: a Windows.UI.Color value (each component 0..255).
 * For Brush-typed properties (e.g. Border.BorderBrush), converted to a SolidColorBrush before being passed.
 *
 * The named colors ([RED] etc.) have the same values as the java.awt.Color constants of the same names ([RED] is (255, 0, 0), [ORANGE] is (255, 200, 0)).
 * Replacing `Color.RED` with `WColor.RED` does not change the color. Only [PURPLE], which AWT does not have, is a WinUI4K-specific value.
 */
class WColor @JvmOverloads constructor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
) {
    /** Creates a new SolidColorBrush for this color. The caller must release it. */
    internal fun createBrush(): ComPtr {
        val brush = Activation.activate(XamlInterop.CLS_SolidColorBrush, XamlInterop.IID_ISolidColorBrush)
        XamlStructs.putColor(brush, XamlInterop.ISolidColorBrush_put_Color, alpha, red, green, blue)
        return brush
    }

    /** Equal if all four components are equal (value comparison, like java.awt.Color.equals). */
    override fun equals(other: Any?): Boolean =
        other is WColor && red == other.red && green == other.green && blue == other.blue && alpha == other.alpha

    override fun hashCode(): Int = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    override fun toString(): String = "WColor(red=$red, green=$green, blue=$blue, alpha=$alpha)"

    companion object {
        /** (0, 0, 0), the same as Color.BLACK. */
        @JvmField
        val BLACK = WColor(0, 0, 0)

        /** (255, 255, 255), the same as Color.WHITE. */
        @JvmField
        val WHITE = WColor(255, 255, 255)

        /** (128, 128, 128), the same as Color.GRAY. */
        @JvmField
        val GRAY = WColor(128, 128, 128)

        /** (192, 192, 192), the same as Color.LIGHT_GRAY. */
        @JvmField
        val LIGHT_GRAY = WColor(192, 192, 192)

        /** (64, 64, 64), the same as Color.DARK_GRAY. */
        @JvmField
        val DARK_GRAY = WColor(64, 64, 64)

        /** (255, 0, 0), the same as Color.RED. */
        @JvmField
        val RED = WColor(255, 0, 0)

        /** (0, 255, 0), the same as Color.GREEN. */
        @JvmField
        val GREEN = WColor(0, 255, 0)

        /** (0, 0, 255), the same as Color.BLUE. */
        @JvmField
        val BLUE = WColor(0, 0, 255)

        /** (255, 255, 0), the same as Color.YELLOW. */
        @JvmField
        val YELLOW = WColor(255, 255, 0)

        /** (255, 200, 0), the same as Color.ORANGE. */
        @JvmField
        val ORANGE = WColor(255, 200, 0)

        /** (255, 175, 175), the same as Color.PINK. */
        @JvmField
        val PINK = WColor(255, 175, 175)

        /** (255, 0, 255), the same as Color.MAGENTA. */
        @JvmField
        val MAGENTA = WColor(255, 0, 255)

        /** (0, 255, 255), the same as Color.CYAN. */
        @JvmField
        val CYAN = WColor(0, 255, 255)

        /** Purple (163, 73, 164). A WinUI4K-specific color that java.awt.Color does not have. */
        @JvmField
        val PURPLE = WColor(163, 73, 164)
    }
}
