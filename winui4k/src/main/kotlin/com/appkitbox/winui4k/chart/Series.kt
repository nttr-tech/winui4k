package com.appkitbox.winui4k.chart

import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.ChartsInterop

/**
 * Charts.LineSeries of WinUI 3: a line chart series that connects data points with lines.
 */
class LineSeries @JvmOverloads constructor(
    title: String = "",
    xValues: SampleModel<*>? = null,
    yValues: SampleModel<*>? = null,
) : CartesianSeries(
    Activation.composeDefault(ChartsInterop.CLS_LineSeries, ChartsInterop.IID_ILineSeriesFactory), // default interface = ILineSeries
) {
    init {
        initialize(title, xValues, yValues)
    }
}

/**
 * Charts.AreaSeries of WinUI 3: an area chart series that fills the region between the line and the axis.
 * Specify the line color with [stroke] and the fill color with [fill] (making it semi-transparent lets overlapping
 * series show through).
 */
class AreaSeries @JvmOverloads constructor(
    title: String = "",
    xValues: SampleModel<*>? = null,
    yValues: SampleModel<*>? = null,
) : CartesianSeries(
    Activation.composeDefault(ChartsInterop.CLS_AreaSeries, ChartsInterop.IID_IAreaSeriesFactory), // default interface = IAreaSeries
) {
    /** IAreaSeries (the default interface). */
    private val areaSeries: ComPtr = inspectable

    /** The fill color (AreaSeries.Fill). The chart's palette color if null. */
    var fill: WColor? = null
        set(value) {
            field = value
            areaSeries.putColor(ChartsInterop.IAreaSeries_put_Fill, value)
        }

    init {
        initialize(title, xValues, yValues)
    }
}

/**
 * The orientation of bars (Charts.BarOrientation). Values are extracted from the winmd.
 */
enum class BarOrientation(internal val native: Int) {
    /** Horizontal bars (categories are arranged vertically and values are shown as horizontal lengths). */
    HORIZONTAL(0),

    /** Vertical bars (categories are arranged horizontally and values are shown as vertical heights). */
    VERTICAL(1),
    ;

    internal companion object {
        fun of(native: Int): BarOrientation = entries.first { it.native == native }
    }
}

/**
 * Charts.BarSeries of WinUI 3: a bar chart series that shows values as bar lengths.
 *
 * The orientation is switched with [orientation]. Series that share an axis must have the same orientation
 * (using the same axis for series of different orientations, such as horizontal bars and a line, results in a
 * WindowsRuntimeException (E_INVALIDARG), the equivalent of IllegalArgumentException, when added to the chart).
 */
class BarSeries @JvmOverloads constructor(
    title: String = "",
    xValues: SampleModel<*>? = null,
    yValues: SampleModel<*>? = null,
) : CartesianSeries(
    Activation.composeDefault(ChartsInterop.CLS_BarSeries, ChartsInterop.IID_IBarSeriesFactory), // default interface = IBarSeries
) {
    /** IBarSeries (the default interface). */
    private val barSeries: ComPtr = inspectable

    /** The fill color of the bars (BarSeries.Fill). The chart's palette color if null. */
    var fill: WColor? = null
        set(value) {
            field = value
            barSeries.putColor(ChartsInterop.IBarSeries_put_Fill, value)
        }

    /** The orientation of the bars (BarSeries.Orientation). */
    var orientation: BarOrientation
        get() = BarOrientation.of(barSeries.getInt(ChartsInterop.IBarSeries_get_Orientation))
        set(value) {
            barSeries.call(ChartsInterop.IBarSeries_put_Orientation, value.native)
        }

    init {
        initialize(title, xValues, yValues)
    }
}

/** Applies the initial values of the constructor arguments (not set if they are left at their defaults). */
private fun CartesianSeries.initialize(title: String, xValues: SampleModel<*>?, yValues: SampleModel<*>?) {
    if (title.isNotEmpty()) this.title = title
    if (xValues != null) this.xValues = xValues
    if (yValues != null) this.yValues = yValues
}
