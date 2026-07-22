package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * WinUI 3's ProgressRing (a circular spinner). JProgressBar has no circular visual, so there's
 * no Swing counterpart and we keep WinUI's class name as-is.
 *
 * [isActive] toggles whether the spinning animation is shown. By default [isIndeterminate] is
 * true and it keeps spinning to indicate "working", but setting it to false turns it into a
 * determinate progress ring driven by [value] / [minimum] / [maximum].
 *
 * ProgressRing is not a RangeBase subclass; it holds Value / Minimum / Maximum on IProgressRing
 * itself (confirmed against the winmd), so they're called directly.
 */
class WProgressRing : WControl(
    Activation.composeDefault(Abi.CLS_ProgressRing, Abi.IID_IProgressRingFactory), // default interface = IProgressRing
) {
    /**
     * Whether the ring is shown and animating (ProgressRing.IsActive).
     * Setting this to false makes the ring itself disappear.
     */
    var isActive: Boolean
        get() = inspectable.getBool(Abi.IProgressRing_get_IsActive)
        set(value) = inspectable.putBool(Abi.IProgressRing_put_IsActive, value)

    /**
     * Whether progress is indeterminate (ProgressRing.IsIndeterminate). true (the default) keeps
     * spinning to indicate "working"; false draws determinate progress based on [value].
     */
    var isIndeterminate: Boolean
        get() = inspectable.getBool(Abi.IProgressRing_get_IsIndeterminate)
        set(value) = inspectable.putBool(Abi.IProgressRing_put_IsIndeterminate, value)

    /** The current value (ProgressRing.Value). Reflected when [isIndeterminate] = false. */
    var value: Double
        get() = inspectable.getDouble(Abi.IProgressRing_get_Value)
        set(value) = inspectable.call(Abi.IProgressRing_put_Value, value)

    /** The minimum value (ProgressRing.Minimum). */
    var minimum: Double
        get() = inspectable.getDouble(Abi.IProgressRing_get_Minimum)
        set(value) = inspectable.call(Abi.IProgressRing_put_Minimum, value)

    /** The maximum value (ProgressRing.Maximum). */
    var maximum: Double
        get() = inspectable.getDouble(Abi.IProgressRing_get_Maximum)
        set(value) = inspectable.call(Abi.IProgressRing_put_Maximum, value)
}
