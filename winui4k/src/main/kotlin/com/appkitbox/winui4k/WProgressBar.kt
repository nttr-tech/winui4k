package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * JProgressBar-like: WinUI 3's ProgressBar (a RangeBase subclass).
 *
 * A horizontal bar that shows progress. Setting [isIndeterminate] to true runs a "working"
 * animation regardless of the value (like JProgressBar.setIndeterminate). Provides
 * [value] / [minimum] / [maximum] (RangeBase) plus WinUI-specific [showError] / [showPaused]
 * (error / paused visual states).
 */
class WProgressBar(minimum: Double = 0.0, maximum: Double = 100.0, value: Double = 0.0) : WControl(
    Activation.composeDefault(Abi.CLS_ProgressBar, Abi.IID_IProgressBarFactory), // default interface = IProgressBar
) {
    /** The Primitives.IRangeBase view holding Value / Minimum / Maximum. */
    private val rangeBase: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_IRangeBase))
    }

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

    /**
     * Whether progress is indeterminate (ProgressBar.IsIndeterminate). Setting this to true runs
     * a "working" animation regardless of [value] (for tasks whose completion time is unknown).
     */
    var isIndeterminate: Boolean
        get() = inspectable.getBool(Abi.IProgressBar_get_IsIndeterminate)
        set(value) = inspectable.putBool(Abi.IProgressBar_put_IsIndeterminate, value)

    /** Whether to show the error visual state (ProgressBar.ShowError). */
    var showError: Boolean
        get() = inspectable.getBool(Abi.IProgressBar_get_ShowError)
        set(value) = inspectable.putBool(Abi.IProgressBar_put_ShowError, value)

    /** Whether to show the paused visual state (ProgressBar.ShowPaused). */
    var showPaused: Boolean
        get() = inspectable.getBool(Abi.IProgressBar_get_ShowPaused)
        set(value) = inspectable.putBool(Abi.IProgressBar_put_ShowPaused, value)

    init {
        if (minimum != 0.0) this.minimum = minimum
        if (maximum != 100.0) this.maximum = maximum
        if (value != 0.0) this.value = value
    }
}
