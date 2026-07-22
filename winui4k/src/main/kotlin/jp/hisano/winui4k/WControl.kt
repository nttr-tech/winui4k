package jp.hisano.winui4k

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.winui.Abi
import jp.hisano.winui4k.internal.winui.XamlStructs

/**
 * Common base for WinUI 3's Control-derived controls (Button, TextBox, ...).
 * Non-Control components such as StackPanel stay as plain [WComponent].
 */
abstract class WControl internal constructor(inspectable: ComPtr) : WComponent(inspectable) {
    private val control: ComPtr by lazy { inspectable.queryInterface(Abi.IID_IControl) }

    /** setEnabled-like (Control.IsEnabled). */
    var isEnabled: Boolean
        get() = control.getBool(Abi.IControl_get_IsEnabled)
        set(value) = control.putBool(Abi.IControl_put_IsEnabled, value)

    /** The content's horizontal alignment (Control.HorizontalContentAlignment). Defaults to CENTER for e.g. Button. */
    var horizontalContentAlignment: HorizontalAlignment
        get() = HorizontalAlignment.of(control.getInt(Abi.IControl_get_HorizontalContentAlignment))
        set(value) = control.call(Abi.IControl_put_HorizontalContentAlignment, value.native)

    /** The content's vertical alignment (Control.VerticalContentAlignment). Defaults to CENTER for e.g. Button. */
    var verticalContentAlignment: VerticalAlignment
        get() = VerticalAlignment.of(control.getInt(Abi.IControl_get_VerticalContentAlignment))
        set(value) = control.call(Abi.IControl_put_VerticalContentAlignment, value.native)

    /** A uniform corner radius on all four corners (Control.CornerRadius). Half the height makes it a pill shape. */
    var cornerRadius: Double = 0.0
        set(value) {
            field = value
            XamlStructs.putCornerRadius(control, Abi.IControl_put_CornerRadius, value)
        }

    /** A uniform inner padding on all four sides (Control.Padding). Use [setPadding] to set each side individually. */
    var padding: Double = 0.0
        set(value) {
            field = value
            setPadding(value, value, value, value)
        }

    /** Sets the inner padding on each side individually (Control.Padding). */
    fun setPadding(left: Double, top: Double, right: Double, bottom: Double) {
        XamlStructs.putThickness(control, Abi.IControl_put_Padding, left, top, right, bottom)
    }
}
