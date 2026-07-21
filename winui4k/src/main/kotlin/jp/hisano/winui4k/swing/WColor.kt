package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winui.Abi
import jp.hisano.winui4k.winui.XamlStructs

/**
 * java.awt.Color-like: a Windows.UI.Color value (each component 0..255).
 * For Brush-typed properties (e.g. Border.BorderBrush), converted to a SolidColorBrush before being passed.
 */
class WColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
) {
    /** Creates a new SolidColorBrush for this color. The caller must release it. */
    internal fun createBrush(): ComPtr {
        val brush = Activation.activate(Abi.CLS_SolidColorBrush).queryInterface(Abi.IID_ISolidColorBrush)
        XamlStructs.putColor(brush, Abi.ISolidColorBrush_put_Color, alpha, red, green, blue)
        return brush
    }

    companion object {
        val BLACK = WColor(0, 0, 0)
        val WHITE = WColor(255, 255, 255)
        val GRAY = WColor(128, 128, 128)
        val LIGHT_GRAY = WColor(192, 192, 192)
        val RED = WColor(237, 28, 36)
        val GREEN = WColor(34, 177, 76)
        val BLUE = WColor(0, 120, 215)
        val YELLOW = WColor(255, 201, 14)
        val ORANGE = WColor(255, 127, 39)
        val PURPLE = WColor(163, 73, 164)
    }
}
