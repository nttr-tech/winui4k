package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JProgressBar-like: WinUI 3's ProgressBar (a RangeBase subclass).
 *
 * A horizontal bar that shows progress. Setting [isIndeterminate] to true runs a "working"
 * animation regardless of the value (like JProgressBar.setIndeterminate). Provides
 * The range of values is held by [model] (a [WBoundedRangeModel]; [WDefaultBoundedRangeModel] by default), and [value] / [minimum] / [maximum] read and write the model.
 * Every model change is copied to the native RangeBase (Minimum / Maximum / Value) and calls the [addChangeListener] listeners once.
 * Also provides [percentComplete] / [string] / [isStringPainted] (JProgressBar) and WinUI's own [showError] / [showPaused] (the error / paused visual states).
 *
 * Differences from Swing:
 * - The value is `Double`, not `int` (write Java's `int v = bar.getValue()` as `(int) bar.getValue()`)
 * - There is no vertical orientation. [string] is not drawn on the bar; it is only exposed as the UI Automation ItemStatus when [isStringPainted] is true
 * - Making the maximum smaller than the minimum lowers the minimum too by the model's rules, unlike WinUI's RangeBase (with [WDefaultBoundedRangeModel]; same as Swing)
 */
class WProgressBar @JvmOverloads constructor(minimum: Double = 0.0, maximum: Double = 100.0, value: Double = 0.0) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ProgressBar, XamlInterop.IID_IProgressBarFactory), // default interface = IProgressBar
) {
    /**
     * Like JProgressBar(int min, int max): creates it with the given range. As in Swing, the value starts at [minimum].
     * Java's `new WProgressBar(0, 100)` resolves to this one (the value of the two-argument Double form starts at 0 and is clamped to the range).
     */
    constructor(minimum: Int, maximum: Int) : this(minimum.toDouble(), maximum.toDouble(), minimum.toDouble())

    /** Like JProgressBar(BoundedRangeModel): creates a progress bar that uses [model] for its range of values (the model's extent is set to 0). */
    constructor(model: WBoundedRangeModel) : this() {
        this.model = model
    }

    /** The Primitives.IRangeBase view holding Value / Minimum / Maximum. */
    private val rangeBase: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IRangeBase))
    }

    private val range = ProgressRange(this) { min, max, v ->
        rangeBase.call(XamlInterop.IRangeBase_put_Minimum, min)
        rangeBase.call(XamlInterop.IRangeBase_put_Maximum, max)
        // WinUI remembers the value before clamping to the range and restores it when the range is widened, so always rewrite the model's value last
        rangeBase.call(XamlInterop.IRangeBase_put_Value, v)
    }

    /**
     * Like JProgressBar.getModel / setModel: the range model. [WDefaultBoundedRangeModel] by default.
     * Sharing one model with a [WSlider] or the like makes the progress bar move when the slider moves. When replaced, the native side is updated to the new model and,
     * as with JProgressBar, the model's extent is set to 0 (if the extent changes, the [addChangeListener] listeners are called for that change).
     * The model references the progress bar through a listener, so keeping the model keeps the progress bar from being collected too (same as Swing).
     */
    var model: WBoundedRangeModel
        get() = range.model
        set(value) {
            range.model = value
        }

    /** The current value ([model]'s value). Values outside the range are clamped (with [WDefaultBoundedRangeModel]). */
    var value: Double
        get() = range.model.value
        set(value) {
            range.model.value = value
        }

    /** The minimum ([model]'s minimum). Increasing it also raises the value and the maximum (with [WDefaultBoundedRangeModel]). */
    var minimum: Double
        get() = range.model.minimum
        set(value) {
            range.model.minimum = value
        }

    /** The maximum ([model]'s maximum). Decreasing it also lowers the value and the minimum (with [WDefaultBoundedRangeModel]). */
    var maximum: Double
        get() = range.model.maximum
        set(value) {
            range.model.maximum = value
        }

    /** Like JProgressBar.getPercentComplete: the fraction completed, `(value - minimum) / (maximum - minimum)` (0.0 to 1.0). NaN if the minimum and maximum are the same. */
    val percentComplete: Double get() = range.percentComplete

    /**
     * Like JProgressBar.getString / setString: the progress string. Setting null goes back, as in Swing, to the default string of [percentComplete]
     * written with NumberFormat.getPercentInstance ("50%" in English or Japanese) (it is never null when read).
     * It is not drawn on the bar (see [isStringPainted]).
     */
    var string: String?
        get() = range.string
        set(value) {
            range.string = value
        }

    /**
     * Like JProgressBar.isStringPainted / setStringPainted: whether to show the progress string ([string]). False by default.
     * WinUI's ProgressBar has no part that draws a string, so when true [string] is only put into the UI Automation ItemStatus
     * for screen readers (put in again whenever the value or string changes; cleared when set back to false).
     */
    var isStringPainted: Boolean
        get() = range.isStringPainted
        set(value) {
            range.isStringPainted = value
        }

    /**
     * Whether progress is indeterminate (ProgressBar.IsIndeterminate). Setting this to true runs
     * a "working" animation regardless of [value] (for tasks whose completion time is unknown).
     */
    var isIndeterminate: Boolean
        get() = inspectable.getBool(XamlInterop.IProgressBar_get_IsIndeterminate)
        set(value) = inspectable.putBool(XamlInterop.IProgressBar_put_IsIndeterminate, value)

    /** Whether to show the error visual state (ProgressBar.ShowError). */
    var showError: Boolean
        get() = inspectable.getBool(XamlInterop.IProgressBar_get_ShowError)
        set(value) = inspectable.putBool(XamlInterop.IProgressBar_put_ShowError, value)

    /** Whether to show the paused visual state (ProgressBar.ShowPaused). */
    var showPaused: Boolean
        get() = inspectable.getBool(XamlInterop.IProgressBar_get_ShowPaused)
        set(value) = inspectable.putBool(XamlInterop.IProgressBar_put_ShowPaused, value)

    init {
        // As with WinUI's RangeBase, the arguments are set in the order minimum -> maximum -> value, and values outside the range are clamped
        this.minimum = minimum
        this.maximum = maximum
        this.value = value
        // RangeBase's native default Maximum is 1.0 (ProgressBar does not override it in its metadata), so the default model's 0..100 is copied too
        range.push()
    }

    /**
     * JProgressBar.getPreferredSize-like: if the width is not given explicitly, it is 146, as with JProgressBar (the height stays WinUI's bar height).
     * WinUI's ProgressBar asks for a width of 0 when no width is given, and disappears under a layout manager.
     */
    override fun preferredSize(): WSize = withDefaultExtent(super.preferredSize(), PROGRESS_BAR_PREFERRED_WIDTH, 0.0)

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this progress bar) every time the progress bar's state changes.
     * As with JProgressBar, it is called once per change whichever of [model]'s value, minimum, maximum, extent and so on changes
     * (also when a shared model is moved with a [WSlider]). In Kotlin it can be written as `addChangeListener { e -> }`.
     */
    fun addChangeListener(listener: WChangeListener) {
        range.changeListeners.add(listener)
    }

    /** Removes one listener added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        range.changeListeners.remove(listener)
    }

    /** Like JProgressBar.getChangeListeners: the ChangeListeners currently added (in the order they were added). */
    fun getChangeListeners(): Array<WChangeListener> = range.changeListeners.toArray()
}

/** The default preferred width of JProgressBar (the width of ProgressBar.horizontalSize in BasicProgressBarUI; DIP). */
private const val PROGRESS_BAR_PREFERRED_WIDTH = 146.0
