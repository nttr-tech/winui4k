package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.ColorSpectrumShape (the spectrum's shape).
 * Values extracted from the winmd (Box=0, Ring=1).
 */
enum class ColorSpectrumShape(internal val native: Int) {
    /** A rectangle (default). */
    BOX(0),

    /** A ring. */
    RING(1),
    ;

    internal companion object {
        fun of(native: Int): ColorSpectrumShape = entries.first { it.native == native }
    }
}

/**
 * JColorChooser-like: WinUI 3's ColorPicker.
 *
 * Provides [color] / [isAlphaEnabled] / [isMoreButtonVisible] / [isHexInputVisible] / [spectrumShape],
 * and [addChangeListener] / [removeChangeListener] (ColorChanged).
 */
class WColorPicker : WControl(
    WinRt.composeDefault(Abi.CLS_ColorPicker, Abi.IID_IColorPickerFactory),
) {
    /** ColorChanged event tokens registered via addChangeListener. */
    private val changeTokens = ListenerTokens<(WColor) -> Unit>()

    /** The selected color (ColorPicker.Color). A Windows.UI.Color struct is passed by value. */
    var color: WColor
        get() {
            val (a, r, g, b) = XamlStructs.getColor(inspectable, Abi.IColorPicker_get_Color)
            return WColor(r, g, b, a)
        }
        set(value) = XamlStructs.putColor(
            inspectable, Abi.IColorPicker_put_Color, value.alpha, value.red, value.green, value.blue,
        )

    /** Whether alpha (opacity) can be adjusted (ColorPicker.IsAlphaEnabled). */
    var isAlphaEnabled: Boolean
        get() = inspectable.getBool(Abi.IColorPicker_get_IsAlphaEnabled)
        set(value) = inspectable.putBool(Abi.IColorPicker_put_IsAlphaEnabled, value)

    /** Whether to show the "more" button that expands the text input fields (ColorPicker.IsMoreButtonVisible). */
    var isMoreButtonVisible: Boolean = false
        set(value) {
            field = value
            inspectable.putBool(Abi.IColorPicker_put_IsMoreButtonVisible, value)
        }

    /** Whether to show the hex input field (ColorPicker.IsHexInputVisible). */
    var isHexInputVisible: Boolean = true
        set(value) {
            field = value
            inspectable.putBool(Abi.IColorPicker_put_IsHexInputVisible, value)
        }

    /** The spectrum's shape (ColorPicker.ColorSpectrumShape). */
    var spectrumShape: ColorSpectrumShape
        get() = ColorSpectrumShape.of(inspectable.getInt(Abi.IColorPicker_get_ColorSpectrumShape))
        set(value) = inspectable.call(Abi.IColorPicker_put_ColorSpectrumShape, value.native)

    /**
     * ChangeListener-like: subscribes to color changes. The listener receives the new color.
     * Subscribes to ColorPicker.ColorChanged (TypedEventHandler<ColorPicker, ColorChangedEventArgs>) under the hood.
     */
    fun addChangeListener(listener: (WColor) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ColorChangedHandler",
            Abi.IID_ColorPickerColorChangedHandler,
            Abi.IColorPicker_add_ColorChanged,
        ) { _, args ->
            // args is a ColorChangedEventArgs; read NewColor (an out struct) and pass it along
            val (a, r, g, b) = XamlStructs.getColor(ComPtr(args), Abi.IColorChangedEventArgs_get_NewColor)
            listener(WColor(r, g, b, a))
        }
        changeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addChangeListener]. */
    fun removeChangeListener(listener: (WColor) -> Unit) {
        val token = changeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IColorPicker_remove_ColorChanged, token)
    }
}
