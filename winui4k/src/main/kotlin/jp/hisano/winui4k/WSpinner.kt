package jp.hisano.winui4k

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winrt.Hstring
import jp.hisano.winui4k.internal.winrt.PropertyValues
import jp.hisano.winui4k.internal.winrt.addEventHandler
import jp.hisano.winui4k.internal.winrt.removeEventHandler
import jp.hisano.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.NumberBoxSpinButtonPlacementMode (spin button placement).
 * Values extracted from the winmd (Hidden=0, Compact=1, Inline=2).
 */
enum class SpinButtonPlacementMode(internal val native: Int) {
    /** Don't show spin buttons (default). */
    HIDDEN(0),

    /** Show them in a popup on focus. */
    COMPACT(1),

    /** Always show them at the right edge of the box. */
    INLINE(2),
    ;

    internal companion object {
        fun of(native: Int): SpinButtonPlacementMode = entries.first { it.native == native }
    }
}

/**
 * JSpinner-like: WinUI 3's NumberBox.
 *
 * Provides numeric entry, validation, and incrementing/decrementing via spin buttons. [value] is
 * NaN when empty. Enabling [acceptsExpression] lets it evaluate expressions like "(5 + 3) * 2".
 */
class WSpinner(value: Double = Double.NaN) : WControl(
    Activation.composeDefault(Abi.CLS_NumberBox, Abi.IID_INumberBoxFactory), // default interface = INumberBox
) {
    /** ValueChanged event tokens registered via addChangeListener. */
    private val changeTokens = ListenerTokens<(Double) -> Unit>()

    /** The current value (NumberBox.Value). NaN when empty. */
    var value: Double
        get() = inspectable.getDouble(Abi.INumberBox_get_Value)
        set(value) = inspectable.call(Abi.INumberBox_put_Value, value)

    /** The minimum value (NumberBox.Minimum). */
    var minimum: Double
        get() = inspectable.getDouble(Abi.INumberBox_get_Minimum)
        set(value) = inspectable.call(Abi.INumberBox_put_Minimum, value)

    /** The maximum value (NumberBox.Maximum). */
    var maximum: Double
        get() = inspectable.getDouble(Abi.INumberBox_get_Maximum)
        set(value) = inspectable.call(Abi.INumberBox_put_Maximum, value)

    /** The amount changed per spin button press (NumberBox.SmallChange). */
    var smallChange: Double
        get() = inspectable.getDouble(Abi.INumberBox_get_SmallChange)
        set(value) = inspectable.call(Abi.INumberBox_put_SmallChange, value)

    /** The amount changed via PageUp / PageDown (NumberBox.LargeChange). */
    var largeChange: Double
        get() = inspectable.getDouble(Abi.INumberBox_get_LargeChange)
        set(value) = inspectable.call(Abi.INumberBox_put_LargeChange, value)

    /** The header shown above the spinner (NumberBox.Header). It's an Object, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.INumberBox_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The placeholder shown when empty (NumberBox.PlaceholderText). */
    var placeholderText: String = ""
        set(value) {
            field = value
            Hstring.use(value) { h -> inspectable.call(Abi.INumberBox_put_PlaceholderText, h) }
        }

    /** Spin button placement (NumberBox.SpinButtonPlacementMode). */
    var spinButtonPlacementMode: SpinButtonPlacementMode
        get() = SpinButtonPlacementMode.of(inspectable.getInt(Abi.INumberBox_get_SpinButtonPlacementMode))
        set(value) = inspectable.call(Abi.INumberBox_put_SpinButtonPlacementMode, value.native)

    /** Whether to wrap around to the minimum once the maximum is exceeded (NumberBox.IsWrapEnabled). */
    var isWrapEnabled: Boolean
        get() = inspectable.getBool(Abi.INumberBox_get_IsWrapEnabled)
        set(value) = inspectable.putBool(Abi.INumberBox_put_IsWrapEnabled, value)

    /** Whether to evaluate expression input like "(5 + 3) * 2" (NumberBox.AcceptsExpression). */
    var acceptsExpression: Boolean
        get() = inspectable.getBool(Abi.INumberBox_get_AcceptsExpression)
        set(value) = inspectable.putBool(Abi.INumberBox_put_AcceptsExpression, value)

    init {
        if (!value.isNaN()) this.value = value
    }

    /** Subscribes to value changes (NumberBox.ValueChanged). The listener receives the value after the change. */
    fun addChangeListener(listener: (Double) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.NumberBoxValueChangedHandler",
            Abi.IID_NumberBoxValueChangedHandler,
            Abi.INumberBox_add_ValueChanged,
        ) { _, args ->
            // args is NumberBoxValueChangedEventArgs; read NewValue and pass it along
            listener(ComPtr(args).getDouble(Abi.INumberBoxValueChangedEventArgs_get_NewValue))
        }
        changeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addChangeListener]. */
    fun removeChangeListener(listener: (Double) -> Unit) {
        val token = changeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.INumberBox_remove_ValueChanged, token)
    }
}
