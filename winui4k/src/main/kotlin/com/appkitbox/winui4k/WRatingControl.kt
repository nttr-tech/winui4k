package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.DoubleConsumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Star-based rating input: WinUI 3's RatingControl. Swing has no corresponding class, but change notifications take the ChangeListener form ([addChangeListener]).
 *
 * Provides [value] (unset is -1) / [maxRating] / [isClearEnabled] / [isReadOnly] /
 * [placeholderValue] / [caption], and [addChangeListener] (ValueChanged and setting [value]).
 */
class WRatingControl : WControl(
    Activation.composeDefault(XamlInterop.CLS_RatingControl, XamlInterop.IID_IRatingControlFactory),
) {
    /** The ChangeListeners. A single ValueChanged handler is registered when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addValueChangedHandler() }

    /** The [value] last notified (or when listeners started to be added). Used to drop notifications of the same value. */
    private var notifiedValue = UNSET

    /**
     * The rating value (RatingControl.Value). Unset is -1, and setting -1 clears the rating.
     * Setting it also calls the [addChangeListener] listeners when the value changes (the native ValueChanged is raised only by user input,
     * so it is notified from here).
     */
    var value: Double
        get() = inspectable.getDouble(XamlInterop.IRatingControl_get_Value)
        set(value) {
            inspectable.call(XamlInterop.IRatingControl_put_Value, value)
            notifyIfChanged()
        }

    /** The number of stars (RatingControl.MaxRating). Default is 5. */
    var maxRating: Int
        get() = inspectable.getInt(XamlInterop.IRatingControl_get_MaxRating)
        set(value) = inspectable.call(XamlInterop.IRatingControl_put_MaxRating, value)

    /** Whether swiping left of the first star clears the rating (RatingControl.IsClearEnabled). */
    var isClearEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.IRatingControl_get_IsClearEnabled)
        set(value) = inspectable.putBool(XamlInterop.IRatingControl_put_IsClearEnabled, value)

    /** Whether to display it as read-only (RatingControl.IsReadOnly). */
    var isReadOnly: Boolean
        get() = inspectable.getBool(XamlInterop.IRatingControl_get_IsReadOnly)
        set(value) = inspectable.putBool(XamlInterop.IRatingControl_put_IsReadOnly, value)

    /** The value shown when the user hasn't rated yet (RatingControl.PlaceholderValue), e.g. for an average rating. */
    var placeholderValue: Double
        get() = inspectable.getDouble(XamlInterop.IRatingControl_get_PlaceholderValue)
        set(value) = inspectable.call(XamlInterop.IRatingControl_put_PlaceholderValue, value)

    /** The caption shown to the right of the stars (RatingControl.Caption), e.g. a review count. */
    var caption: String
        get() = inspectable.getString(XamlInterop.IRatingControl_get_Caption)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IRatingControl_put_Caption, h) }

    /**
     * ChangeListener-like: subscribes to changes in the rating value. The listener receives the new [value] (unset is -1).
     * Called on user input and also when [value] is set, and notifications where the value did not change are dropped (same as the form taking a [WChangeListener]).
     */
    @JvmSynthetic
    fun addChangeListener(listener: (Double) -> Unit) {
        changeListeners.addAdapter(listener, WChangeListener { listener(value) })
    }

    /**
     * The legacy Java `addChangeListener(DoubleConsumer)`. Kept only for binary compatibility (linking already compiled
     * callers) and not visible from Java source (HIDDEN). In Java, use the form that takes a [WChangeListener].
     * Unlike before, it is also called when [value] is set.
     */
    @Deprecated("Use addChangeListener that takes a WChangeListener", level = DeprecationLevel.HIDDEN)
    @JvmName("addChangeListener")
    fun addChangeListenerForJava(listener: DoubleConsumer) {
        changeListeners.addAdapter(listener, WChangeListener { listener.accept(value) })
    }

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this control) every time the rating value changes.
     * Called once per change. Called both on user input (RatingControl.ValueChanged) and when [value] is set, and
     * notifications where the value did not change are dropped (the native side sends ValueChanged even for the same value,
     * for example when the highest star is chosen again). Read the new value with `(e.source as WRatingControl).value`.
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Unsubscribes a listener registered via [addChangeListener]. */
    @JvmSynthetic
    fun removeChangeListener(listener: (Double) -> Unit) {
        changeListeners.removeAdapter(listener)
    }

    /** The legacy Java `removeChangeListener(DoubleConsumer)` (HIDDEN; kept only for binary compatibility). */
    @Deprecated("Use removeChangeListener that takes a WChangeListener", level = DeprecationLevel.HIDDEN)
    @JvmName("removeChangeListener")
    fun removeChangeListenerForJava(listener: DoubleConsumer) {
        changeListeners.removeAdapter(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order they were added; Kotlin lambdas appear wrapped). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    private fun notifyIfChanged() {
        val old = notifiedValue
        val now = value
        notifiedValue = now
        changeListeners.fireIfChanged(old, now)
    }

    private fun addValueChangedHandler() {
        notifiedValue = value
        // The handler stays on the native side, so do not strongly capture the control; look it up through the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.RatingChangedHandler",
            XamlInterop.IID_RatingControlValueChangedHandler,
            XamlInterop.IRatingControl_add_ValueChanged,
        ) { _, _ -> (anchor.get() as? WRatingControl)?.notifyIfChanged() }
    }

    private companion object {
        /** RatingControl's "unset" value (c_noValueSetSentinel). */
        const val UNSET = -1.0
    }
}
