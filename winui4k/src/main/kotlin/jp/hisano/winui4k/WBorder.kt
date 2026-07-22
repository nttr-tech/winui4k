package jp.hisano.winui4k

import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winui.Abi
import jp.hisano.winui4k.internal.winui.XamlStructs

/**
 * JPanel-like with a BorderFactory: WinUI 3's Border.
 * Draws a border ([borderColor] / [borderThickness]), rounded corners ([cornerRadius]),
 * a background ([background]), and padding ([padding]) around a single [child].
 */
class WBorder(child: WComponent? = null) : WComponent(
    Activation.activate(Abi.CLS_Border).queryInterface(Abi.IID_IBorder),
) {
    /** The single child shown inside the border (Border.Child). */
    var child: WComponent? = null
        set(value) {
            field = value
            inspectable.call(Abi.IBorder_put_Child, value?.uiElement?.ptr)
        }

    /** The border color (Border.BorderBrush). Converted to a SolidColorBrush before being passed. */
    var borderColor: WColor? = null
        set(value) {
            field = value
            putBrush(Abi.IBorder_put_BorderBrush, value)
        }

    /** The border thickness, the same on all four sides (Border.BorderThickness). */
    var borderThickness: Double = 0.0
        set(value) {
            field = value
            XamlStructs.putThickness(inspectable, Abi.IBorder_put_BorderThickness, value, value, value, value)
        }

    /** The corner radius, the same on all four corners (Border.CornerRadius). */
    var cornerRadius: Double = 0.0
        set(value) {
            field = value
            XamlStructs.putCornerRadius(inspectable, Abi.IBorder_put_CornerRadius, value)
        }

    /** The background color (Border.Background). Converted to a SolidColorBrush before being passed. */
    var background: WColor? = null
        set(value) {
            field = value
            putBrush(Abi.IBorder_put_Background, value)
        }

    /**
     * A gradient background (Border.Background). Written to the same property as [background],
     * so whichever is set last takes effect.
     */
    var backgroundGradient: WLinearGradientPaint? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(Abi.IBorder_put_Background, null)
            } else {
                val brush = value.createBrush()
                inspectable.call(Abi.IBorder_put_Background, brush)
                brush.release()
            }
        }

    /** The inner padding, the same on all four sides (Border.Padding). */
    var padding: Double = 0.0
        set(value) {
            field = value
            XamlStructs.putThickness(inspectable, Abi.IBorder_put_Padding, value, value, value, value)
        }

    init {
        if (child != null) this.child = child
    }

    private fun putBrush(slot: Int, color: WColor?) {
        if (color == null) {
            inspectable.call(slot, null)
        } else {
            val brush = color.createBrush()
            inspectable.call(slot, brush.ptr)
            brush.release()
        }
    }
}
