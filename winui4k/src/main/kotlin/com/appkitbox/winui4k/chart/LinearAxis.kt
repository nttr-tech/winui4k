package com.appkitbox.winui4k.chart

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.ChartsInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop

/**
 * Charts.LinearAxis of WinUI 3: a numeric axis (value axis).
 *
 * If the range ([minimum] / [maximum]) and the tick interval ([spacing]) are null, they are determined automatically
 * from the data.
 */
class LinearAxis @JvmOverloads constructor(label: String = "") : CartesianAxis(
    Activation.activate(ChartsInterop.CLS_LinearAxis, ChartsInterop.IID_ILinearAxis), // default interface = ILinearAxis
) {
    /** ILinearAxis (the default interface). */
    private val linearAxis: ComPtr = inspectable

    /** The axis minimum (LinearAxis.Minimum). If null, it is determined automatically from the data. */
    var minimum: Double?
        get() = linearAxis.getDoubleReference(ChartsInterop.ILinearAxis_get_Minimum)
        set(value) {
            linearAxis.putDoubleReference(ChartsInterop.ILinearAxis_put_Minimum, value)
        }

    /** The axis maximum (LinearAxis.Maximum). If null, it is determined automatically from the data. */
    var maximum: Double?
        get() = linearAxis.getDoubleReference(ChartsInterop.ILinearAxis_get_Maximum)
        set(value) {
            linearAxis.putDoubleReference(ChartsInterop.ILinearAxis_put_Maximum, value)
        }

    /** The interval of the major ticks (LinearAxis.Spacing). If null, it is determined automatically from the range. */
    var spacing: Double?
        get() = linearAxis.getDoubleReference(ChartsInterop.ILinearAxis_get_Spacing)
        set(value) {
            linearAxis.putDoubleReference(ChartsInterop.ILinearAxis_put_Spacing, value)
        }

    init {
        if (label.isNotEmpty()) this.label = label
    }
}

/** Reads the IReference<Double> property [slot]. Returns null if it is null (not set). */
private fun ComPtr.getDoubleReference(slot: Int): Double? {
    val boxed = getPtrOrNull(slot) ?: return null
    return try {
        (PropertyValues.unboxAny(boxed) as? Number)?.toDouble()
    } finally {
        boxed.release()
    }
}

/** Sets [value] on the IReference<Double> property [slot]. null resets it to not set (automatic). */
private fun ComPtr.putDoubleReference(slot: Int, value: Double?) {
    if (value == null) {
        call(slot, null)
        return
    }
    val boxed = PropertyValues.boxDouble(value)
    val reference = boxed.queryInterface(FoundationInterop.IID_IReference_Double)
    try {
        call(slot, reference)
    } finally {
        reference.release()
        boxed.release()
    }
}
