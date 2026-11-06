package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * WinUI 3's ProgressRing (a circular spinner). JProgressBar has no circular visual, so there's
 * no Swing counterpart and we keep WinUI's class name as-is.
 *
 * [isActive] toggles whether the spinning animation is shown. By default [isIndeterminate] is
 * true and it keeps spinning to indicate "working", but setting it to false turns it into a
 * determinate progress ring driven by [value] / [minimum] / [maximum].
 *
 * As with [WProgressBar], the range of values is held by the JProgressBar-style [model] (a [WBoundedRangeModel]), and [addChangeListener] /
 * [percentComplete] / [string] / [isStringPainted] can be used with the same meaning.
 * ProgressRing is not a RangeBase subclass; it holds Value / Minimum / Maximum on IProgressRing
 * (confirmed in the winmd), so the model state is copied to them directly. ProgressRing has no value change event, but
 * the value changes only through the model, so the listeners are called from model changes.
 */
class WProgressRing() : WControl(
    Activation.composeDefault(XamlInterop.CLS_ProgressRing, XamlInterop.IID_IProgressRingFactory), // default interface = IProgressRing
) {
    /** Creates a progress ring that uses [model] for its range of values (the model's extent is set to 0; same as the same form of [WProgressBar]). */
    constructor(model: WBoundedRangeModel) : this() {
        this.model = model
    }

    private val range = ProgressRange(this) { min, max, v ->
        inspectable.call(XamlInterop.IProgressRing_put_Minimum, min)
        inspectable.call(XamlInterop.IProgressRing_put_Maximum, max)
        inspectable.call(XamlInterop.IProgressRing_put_Value, v)
    }

    /**
     * Whether the ring is shown and animating (ProgressRing.IsActive).
     * Setting this to false makes the ring itself disappear.
     */
    var isActive: Boolean
        get() = inspectable.getBool(XamlInterop.IProgressRing_get_IsActive)
        set(value) = inspectable.putBool(XamlInterop.IProgressRing_put_IsActive, value)

    /**
     * Whether progress is indeterminate (ProgressRing.IsIndeterminate). true (the default) keeps
     * spinning to indicate "working"; false draws determinate progress based on [value].
     */
    var isIndeterminate: Boolean
        get() = inspectable.getBool(XamlInterop.IProgressRing_get_IsIndeterminate)
        set(value) = inspectable.putBool(XamlInterop.IProgressRing_put_IsIndeterminate, value)

    /**
     * Like JProgressBar.getModel / setModel: the range model. [WDefaultBoundedRangeModel] by default (0..100, value 0; the same as ProgressRing's default).
     * The rules for sharing and replacing are the same as [WProgressBar.model].
     */
    var model: WBoundedRangeModel
        get() = range.model
        set(value) {
            range.model = value
        }

    /** The current value ([model]'s value). Reflected when [isIndeterminate] = false. Values outside the range are clamped (with [WDefaultBoundedRangeModel]). */
    var value: Double
        get() = range.model.value
        set(value) {
            range.model.value = value
        }

    /** The minimum ([model]'s minimum). */
    var minimum: Double
        get() = range.model.minimum
        set(value) {
            range.model.minimum = value
        }

    /** The maximum ([model]'s maximum). */
    var maximum: Double
        get() = range.model.maximum
        set(value) {
            range.model.maximum = value
        }

    /** Like JProgressBar.getPercentComplete: the fraction completed, `(value - minimum) / (maximum - minimum)`. NaN if the minimum and maximum are the same. */
    val percentComplete: Double get() = range.percentComplete

    /** Like JProgressBar.getString / setString: the progress string (null goes back to the default such as "50%"; same as [WProgressBar.string]). */
    var string: String?
        get() = range.string
        set(value) {
            range.string = value
        }

    /** Like JProgressBar.isStringPainted / setStringPainted: if true, [string] is exposed as the UI Automation ItemStatus (not drawn on the ring). */
    var isStringPainted: Boolean
        get() = range.isStringPainted
        set(value) {
            range.isStringPainted = value
        }

    init {
        range.push()
    }

    /** ChangeListener-style: receives a [WChangeEvent] (whose source is this progress ring) every time [model] changes. */
    fun addChangeListener(listener: WChangeListener) {
        range.changeListeners.add(listener)
    }

    /** Removes one listener added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        range.changeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order they were added). */
    fun getChangeListeners(): Array<WChangeListener> = range.changeListeners.toArray()
}
