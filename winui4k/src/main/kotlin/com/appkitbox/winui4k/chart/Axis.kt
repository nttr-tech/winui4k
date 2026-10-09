package com.appkitbox.winui4k.chart

import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.ChartsInterop

/**
 * Charts.Axis of WinUI 3: the common attributes of a chart axis (axis label and visibility).
 *
 * An axis is used by setting it on a series ([CartesianSeries.xAxis] / [CartesianSeries.yAxis]).
 * When a series is added to a [com.appkitbox.winui4k.WChart], the axes that series uses are also registered with the
 * chart (Chart.Axes). One axis may be shared by multiple series (the sharing series must agree on value type and
 * orientation). Property changes are reflected immediately in a displayed chart. Create and use it on the UI thread.
 */
abstract class Axis internal constructor(
    /** The default interface pointer of the axis (ILinearAxis / ICategoryAxis / IDateTimeAxis). */
    internal val inspectable: ComPtr,
) {
    /** The record of COM references this wrapper owns (the same mechanism as WComponent). */
    private val lifetime = ComLifetime.adopt(this, inspectable)

    /** Ties ownership of [ptr] to this wrapper's lifetime. */
    internal fun own(ptr: ComPtr): ComPtr = lifetime.own(ptr)

    /** The IAxis view (the pointer added to Chart.Axes). */
    internal val axis: ComPtr by lazy { own(inspectable.queryInterface(ChartsInterop.IID_IAxis)) }

    /** The axis label (Axis.Label): a heading shown along the axis. Not shown if empty. */
    var label: String
        get() = axis.getString(ChartsInterop.IAxis_get_Label)
        set(value) {
            Hstring.use(value) { h -> axis.call(ChartsInterop.IAxis_put_Label, h) }
        }

    /**
     * Whether to show the axis line and ticks (tick marks and labels) (Axis.IsVisible).
     * Even when false, the axis label ([label]) is shown and the axis is still used for drawing series.
     */
    var isVisible: Boolean
        get() = axis.getBool(ChartsInterop.IAxis_get_IsVisible)
        set(value) {
            axis.putBool(ChartsInterop.IAxis_put_IsVisible, value)
        }
}

/**
 * Grid line display (Charts.GridLines). Values are extracted from the winmd.
 */
enum class GridLines(internal val native: Int) {
    /** No grid lines. */
    NONE(0),

    /** Draws grid lines at the major ticks. */
    MAJOR(1),

    /** Draws grid lines at the major and minor ticks. */
    MINOR(2),
    ;

    internal companion object {
        fun of(native: Int): GridLines = entries.first { it.native == native }
    }
}

/**
 * Charts.CartesianAxis of WinUI 3: the ticks, grid lines, and colors of a Cartesian axis (X axis / Y axis).
 *
 * For a concrete axis, use [LinearAxis] (numbers) / [CategoryAxis] (string categories) /
 * [DateTimeAxis] (dates and times) depending on the value type. Color properties use the theme's default color if null.
 */
abstract class CartesianAxis internal constructor(inspectable: ComPtr) : Axis(inspectable) {
    /** The ICartesianAxis view (the pointer set as a series' XAxis / YAxis). */
    internal val cartesianAxis: ComPtr by lazy {
        own(inspectable.queryInterface(ChartsInterop.IID_ICartesianAxis))
    }

    /** Whether to show the tick labels (values) (CartesianAxis.ShowTickLabels). */
    var showTickLabels: Boolean
        get() = cartesianAxis.getBool(ChartsInterop.ICartesianAxis_get_ShowTickLabels)
        set(value) {
            cartesianAxis.putBool(ChartsInterop.ICartesianAxis_put_ShowTickLabels, value)
        }

    /** Whether to show the tick marks (CartesianAxis.ShowTickMarks). */
    var showTickMarks: Boolean
        get() = cartesianAxis.getBool(ChartsInterop.ICartesianAxis_get_ShowTickMarks)
        set(value) {
            cartesianAxis.putBool(ChartsInterop.ICartesianAxis_put_ShowTickMarks, value)
        }

    /** Grid line display (CartesianAxis.GridLines): lines across the plot area perpendicular to the axis. */
    var gridLines: GridLines
        get() = GridLines.of(cartesianAxis.getInt(ChartsInterop.ICartesianAxis_get_GridLines))
        set(value) {
            cartesianAxis.call(ChartsInterop.ICartesianAxis_put_GridLines, value.native)
        }

    /** The color of the major grid lines (CartesianAxis.GridLineMajorBrush). The theme's default color if null. */
    var gridLineMajorColor: WColor? = null
        set(value) {
            field = value
            cartesianAxis.putColor(ChartsInterop.ICartesianAxis_put_GridLineMajorBrush, value)
        }

    /** The color of the minor grid lines (CartesianAxis.GridLineMinorBrush). The theme's default color if null. */
    var gridLineMinorColor: WColor? = null
        set(value) {
            field = value
            cartesianAxis.putColor(ChartsInterop.ICartesianAxis_put_GridLineMinorBrush, value)
        }

    /** The color of the tick marks (CartesianAxis.TickBrush). The theme's default color if null. */
    var tickColor: WColor? = null
        set(value) {
            field = value
            cartesianAxis.putColor(ChartsInterop.ICartesianAxis_put_TickBrush, value)
        }

    /** The text color of the tick labels (CartesianAxis.TickLabelBrush). The theme's default color if null. */
    var tickLabelColor: WColor? = null
        set(value) {
            field = value
            cartesianAxis.putColor(ChartsInterop.ICartesianAxis_put_TickLabelBrush, value)
        }

    /** The color of the axis line (CartesianAxis.AxisLineBrush). The theme's default color if null. */
    var axisLineColor: WColor? = null
        set(value) {
            field = value
            cartesianAxis.putColor(ChartsInterop.ICartesianAxis_put_AxisLineBrush, value)
        }
}

/** Sets a SolidColorBrush of [color] on the Brush property [slot]. null clears it (the theme's default color). */
internal fun ComPtr.putColor(slot: Int, color: WColor?) {
    if (color == null) {
        call(slot, null)
    } else {
        val brush = color.createBrush()
        try {
            call(slot, brush.ptr)
        } finally {
            brush.release()
        }
    }
}
