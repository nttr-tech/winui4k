package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.Primitives.SliderSnapsTo (where the thumb snaps to).
 * Values extracted from the winmd (StepValues=0, Ticks=1).
 */
enum class SliderSnapsTo(internal val native: Int) {
    /** Snaps to multiples of stepFrequency (default). */
    STEP_VALUES(0),

    /** Snaps to the tickFrequency graduations. */
    TICKS(1),
    ;

    internal companion object {
        fun of(native: Int): SliderSnapsTo = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.Primitives.TickPlacement (where tick marks are drawn).
 * Values extracted from the winmd (None=0, TopLeft=1, BottomRight=2, Outside=3, Inline=4).
 */
enum class TickPlacement(internal val native: Int) {
    /** No tick marks (default). */
    NONE(0),

    /** Above when horizontal, left when vertical. */
    TOP_LEFT(1),

    /** Below when horizontal, right when vertical. */
    BOTTOM_RIGHT(2),

    /** On both sides of the track. */
    OUTSIDE(3),

    /** On the track itself. */
    INLINE(4),
    ;

    internal companion object {
        fun of(native: Int): TickPlacement = entries.first { it.native == native }
    }
}

/**
 * JSlider-like: WinUI 3's Slider (a RangeBase subclass).
 *
 * Provides [value] / [minimum] / [maximum] (RangeBase), [stepFrequency] / [snapsTo] /
 * [tickFrequency] / [tickPlacement] / [orientation] / [isDirectionReversed] / [header] (Slider),
 * and [addChangeListener] / [removeChangeListener] (ValueChanged).
 */
class WSlider(minimum: Double = 0.0, maximum: Double = 100.0, value: Double = 0.0) : WControl(
    WinRt.composeDefault(Abi.CLS_Slider, Abi.IID_ISliderFactory), // default interface = ISlider
) {
    /** The Primitives.IRangeBase view holding Value / Minimum / Maximum / ValueChanged. */
    private val rangeBase: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IRangeBase)
    }

    /** ValueChanged event tokens registered via addChangeListener. */
    private val changeTokens = ListenerTokens<(Double) -> Unit>()

    /** The current value (RangeBase.Value). */
    var value: Double
        get() = rangeBase.getDouble(Abi.IRangeBase_get_Value)
        set(value) = rangeBase.call(Abi.IRangeBase_put_Value, value)

    /** The minimum value (RangeBase.Minimum). */
    var minimum: Double
        get() = rangeBase.getDouble(Abi.IRangeBase_get_Minimum)
        set(value) = rangeBase.call(Abi.IRangeBase_put_Minimum, value)

    /** The maximum value (RangeBase.Maximum). */
    var maximum: Double
        get() = rangeBase.getDouble(Abi.IRangeBase_get_Maximum)
        set(value) = rangeBase.call(Abi.IRangeBase_put_Maximum, value)

    /** The step the value moves by while dragging (Slider.StepFrequency). */
    var stepFrequency: Double
        get() = inspectable.getDouble(Abi.ISlider_get_StepFrequency)
        set(value) = inspectable.call(Abi.ISlider_put_StepFrequency, value)

    /** Where the thumb snaps to (Slider.SnapsTo). */
    var snapsTo: SliderSnapsTo
        get() = SliderSnapsTo.of(inspectable.getInt(Abi.ISlider_get_SnapsTo))
        set(value) = inspectable.call(Abi.ISlider_put_SnapsTo, value.native)

    /** The spacing between tick marks (Slider.TickFrequency). */
    var tickFrequency: Double
        get() = inspectable.getDouble(Abi.ISlider_get_TickFrequency)
        set(value) = inspectable.call(Abi.ISlider_put_TickFrequency, value)

    /** Where tick marks are drawn (Slider.TickPlacement). */
    var tickPlacement: TickPlacement
        get() = TickPlacement.of(inspectable.getInt(Abi.ISlider_get_TickPlacement))
        set(value) = inspectable.call(Abi.ISlider_put_TickPlacement, value.native)

    /** The slider's orientation (Slider.Orientation). */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(Abi.ISlider_get_Orientation))
        set(value) = inspectable.call(Abi.ISlider_put_Orientation, value.native)

    /** Whether the increasing direction is reversed (Slider.IsDirectionReversed). */
    var isDirectionReversed: Boolean
        get() = inspectable.getBool(Abi.ISlider_get_IsDirectionReversed)
        set(value) = inspectable.putBool(Abi.ISlider_put_IsDirectionReversed, value)

    /** The heading above the slider (Slider.Header). Object-typed, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = WinRt.boxString(value)
            inspectable.call(Abi.ISlider_put_Header, boxed.ptr)
            boxed.release()
        }

    init {
        if (minimum != 0.0) this.minimum = minimum
        if (maximum != 100.0) this.maximum = maximum
        if (value != 0.0) this.value = value
    }

    /**
     * ChangeListener-like: subscribes to value changes. The listener receives the new value.
     * Subscribes to RangeBase.ValueChanged (RangeBaseValueChangedEventHandler) under the hood.
     */
    fun addChangeListener(listener: (Double) -> Unit) {
        val token = rangeBase.addEventHandler(
            "WinUI4K.ValueChangedHandler",
            Abi.IID_RangeBaseValueChangedEventHandler,
            Abi.IRangeBase_add_ValueChanged,
        ) { _, args ->
            // args is a RangeBaseValueChangedEventArgs; read NewValue and pass it along
            listener(ComPtr(args).getDouble(Abi.IRangeBaseValueChangedEventArgs_get_NewValue))
        }
        changeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addChangeListener]. */
    fun removeChangeListener(listener: (Double) -> Unit) {
        val token = changeTokens.remove(listener) ?: return
        rangeBase.removeEventHandler(Abi.IRangeBase_remove_ValueChanged, token)
    }
}
