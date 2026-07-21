package jp.hisano.winui4k.swing

import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.Hstring
import jp.hisano.winui4k.winrt.addEventHandler
import jp.hisano.winui4k.winrt.getString
import jp.hisano.winui4k.winrt.removeEventHandler
import jp.hisano.winui4k.winui.Abi

/**
 * Star-based rating input: WinUI 3's RatingControl.
 *
 * Provides [value] (unset is -1) / [maxRating] / [isClearEnabled] / [isReadOnly] /
 * [placeholderValue] / [caption], and [addChangeListener] (ValueChanged).
 */
class WRatingControl : WControl(
    Activation.composeDefault(Abi.CLS_RatingControl, Abi.IID_IRatingControlFactory),
) {
    /** ValueChanged event tokens registered via addChangeListener. */
    private val changeTokens = ListenerTokens<(Double) -> Unit>()

    /** The rating value (RatingControl.Value). Unset is -1. */
    var value: Double
        get() = inspectable.getDouble(Abi.IRatingControl_get_Value)
        set(value) = inspectable.call(Abi.IRatingControl_put_Value, value)

    /** The number of stars (RatingControl.MaxRating). Default is 5. */
    var maxRating: Int
        get() = inspectable.getInt(Abi.IRatingControl_get_MaxRating)
        set(value) = inspectable.call(Abi.IRatingControl_put_MaxRating, value)

    /** Whether swiping left of the first star clears the rating (RatingControl.IsClearEnabled). */
    var isClearEnabled: Boolean
        get() = inspectable.getBool(Abi.IRatingControl_get_IsClearEnabled)
        set(value) = inspectable.putBool(Abi.IRatingControl_put_IsClearEnabled, value)

    /** Whether to display it as read-only (RatingControl.IsReadOnly). */
    var isReadOnly: Boolean
        get() = inspectable.getBool(Abi.IRatingControl_get_IsReadOnly)
        set(value) = inspectable.putBool(Abi.IRatingControl_put_IsReadOnly, value)

    /** The value shown when the user hasn't rated yet (RatingControl.PlaceholderValue), e.g. for an average rating. */
    var placeholderValue: Double
        get() = inspectable.getDouble(Abi.IRatingControl_get_PlaceholderValue)
        set(value) = inspectable.call(Abi.IRatingControl_put_PlaceholderValue, value)

    /** The caption shown to the right of the stars (RatingControl.Caption), e.g. a review count. */
    var caption: String
        get() = inspectable.getString(Abi.IRatingControl_get_Caption)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IRatingControl_put_Caption, h) }

    /**
     * ChangeListener-like: subscribes to changes in the rating value. The listener receives
     * the new [value]. Subscribes to RatingControl.ValueChanged (TypedEventHandler<RatingControl, Object>) under the hood.
     */
    fun addChangeListener(listener: (Double) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.RatingChangedHandler",
            Abi.IID_RatingControlValueChangedHandler,
            Abi.IRatingControl_add_ValueChanged,
        ) { _, _ -> listener(value) }
        changeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addChangeListener]. */
    fun removeChangeListener(listener: (Double) -> Unit) {
        val token = changeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IRatingControl_remove_ValueChanged, token)
    }
}
