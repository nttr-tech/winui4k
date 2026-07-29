package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * java.awt.Color-like: a Windows.UI.Color value (each component 0..255).
 * For Brush-typed properties (e.g. Border.BorderBrush), converted to a SolidColorBrush before being passed.
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

    companion object {
        @JvmField
        val BLACK = WColor(0, 0, 0)

        @JvmField
        val WHITE = WColor(255, 255, 255)

        @JvmField
        val GRAY = WColor(128, 128, 128)

        @JvmField
        val LIGHT_GRAY = WColor(192, 192, 192)

        @JvmField
        val RED = WColor(237, 28, 36)

        @JvmField
        val GREEN = WColor(34, 177, 76)

        @JvmField
        val BLUE = WColor(0, 120, 215)

        @JvmField
        val YELLOW = WColor(255, 201, 14)

        @JvmField
        val ORANGE = WColor(255, 127, 39)

        @JvmField
        val PURPLE = WColor(163, 73, 164)
    }
}
