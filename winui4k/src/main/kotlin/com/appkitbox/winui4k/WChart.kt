package com.appkitbox.winui4k

import com.appkitbox.winui4k.chart.Axis
import com.appkitbox.winui4k.chart.CartesianAxis
import com.appkitbox.winui4k.chart.CartesianSeries
import com.appkitbox.winui4k.chart.SeriesHost
import com.appkitbox.winui4k.chart.putColor
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.ChartsInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A chart with the same MVC structure as JTable: the WinUI 3 Chart (Microsoft.UI.Xaml.Controls.Charts.Chart,
 * experimental in Windows App SDK 2.5).
 *
 * Division of roles (the com.appkitbox.winui4k.chart package):
 * - Data: [com.appkitbox.winui4k.chart.SampleModel] (a ListModel-like one-dimensional sequence of data.
 *   Changing a [com.appkitbox.winui4k.chart.DefaultSampleModel] redraws the displayed chart immediately)
 * - Series: subclasses of [CartesianSeries] ([com.appkitbox.winui4k.chart.LineSeries] /
 *   [com.appkitbox.winui4k.chart.AreaSeries] / [com.appkitbox.winui4k.chart.BarSeries]).
 *   They hold the X-value and Y-value models and axes, and the appearance of lines, markers, and labels
 * - Axes: subclasses of [CartesianAxis] ([com.appkitbox.winui4k.chart.LinearAxis] /
 *   [com.appkitbox.winui4k.chart.CategoryAxis] / [com.appkitbox.winui4k.chart.DateTimeAxis])
 *
 * When a series is added with [addSeries], the axes it uses are also registered with the chart (Chart.Axes; an axis
 * that no series uses any longer is removed automatically). The legend is controlled by [showLegend] / [legendTitle].
 *
 * ```kotlin
 * val months = DefaultSampleModel.of("Jan", "Feb", "Mar")
 * val sales = DefaultSampleModel.of(12.0, 18.0, 9.0)
 * val chart = WChart()
 * chart.addSeries(LineSeries("Sales", months, sales).apply {
 *     xAxis = CategoryAxis("Month")
 *     yAxis = LinearAxis("Sales").apply { minimum = 0.0 }
 * })
 * sales.addElement(21.0) // Reflected in the chart immediately
 * ```
 *
 * Chart.Data (a container for declaring Samples in XAML markup) is not used by this API, which passes models
 * directly to series (Samples referenced by a series are drawn without being put in Data).
 */
class WChart : WControl(create()) {
    /** IChart (the default interface). */
    private val chart: ComPtr = inspectable

    /** Chart.Series (IVector<CartesianSeries>). */
    private val nativeSeries: ComPtr by lazy {
        nativeVector(ChartsInterop.IChart_get_Series, ChartsInterop.IID_IVector_CartesianSeries)
    }

    /** Chart.Axes (IVector<Axis>). */
    private val nativeAxes: ComPtr by lazy { nativeVector(ChartsInterop.IChart_get_Axes, ChartsInterop.IID_IVector_Axis) }

    /** The displayed series (in the same order as Chart.Series). */
    private val seriesList = mutableListOf<CartesianSeries>()

    /** The registered axes (in the same order as Chart.Axes). */
    private val axisList = mutableListOf<CartesianAxis>()

    private val seriesHost = object : SeriesHost {
        override fun axisAdding(axis: CartesianAxis) {
            registerAxis(axis)
        }

        override fun axesChanged() {
            pruneAxes()
        }
    }

    /** Whether to show the legend (Chart.ShowLegend). The legend lists the [CartesianSeries.title] of each series. */
    var showLegend: Boolean
        get() = chart.getBool(ChartsInterop.IChart_get_ShowLegend)
        set(value) {
            chart.putBool(ChartsInterop.IChart_put_ShowLegend, value)
        }

    /** The legend title (Chart.LegendTitle). An empty string means no title. */
    var legendTitle: String
        get() = chart.getString(ChartsInterop.IChart_get_LegendTitle)
        set(value) {
            Hstring.use(value) { h -> chart.call(ChartsInterop.IChart_put_LegendTitle, h) }
        }

    /** The IControl view (background and text color). */
    private val control: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IControl)) }

    /**
     * The background color of the whole chart (Control.Background). Until it is set, the theme's default background
     * is used; setting null draws no background (Background = null).
     */
    var background: WColor? = null
        set(value) {
            field = value
            control.putColor(XamlInterop.IControl_put_Background, value)
        }

    /**
     * The text color of the legend (title and series names) (Control.Foreground). Until it is set, the theme's default
     * color is used. The color of axis tick labels is specified per axis with [CartesianAxis.tickLabelColor] (this
     * color does not apply to them).
     */
    var foreground: WColor? = null
        set(value) {
            field = value
            control.putColor(XamlInterop.IControl_put_Foreground, value)
        }

    /** The number of series. */
    val seriesCount: Int
        get() = seriesList.size

    /** Adds [series] at the end. */
    fun addSeries(series: CartesianSeries) {
        insertSeries(seriesList.size, series)
    }

    /**
     * Inserts [series] at [index]. The axes the series uses are also registered with the chart.
     * A series can be added to only one chart.
     * If the axis usage is inconsistent (for example, series with different bar orientations sharing an axis),
     * the chart throws a WindowsRuntimeException with E_INVALIDARG and the series is not added.
     */
    fun insertSeries(index: Int, series: CartesianSeries) {
        require(series.host == null) { "series is already added to a chart" }
        require(index in 0..seriesList.size) { "index out of range: $index" }
        series.xAxis?.let { registerAxis(it) }
        series.yAxis?.let { registerAxis(it) }
        try {
            nativeSeries.call(FoundationInterop.IVector_InsertAt, index, series.series)
        } catch (e: RuntimeException) {
            pruneAxes()
            throw e
        }
        seriesList.add(index, series)
        series.host = seriesHost
    }

    /**
     * Removes [series]. Does nothing if it has not been added. Also unregisters axes that no series uses any longer.
     */
    fun removeSeries(series: CartesianSeries) {
        val index = seriesList.indexOf(series)
        if (index >= 0) removeSeriesAt(index)
    }

    /** Removes the series at [index] and returns the removed series. */
    fun removeSeriesAt(index: Int): CartesianSeries {
        val series = seriesList.removeAt(index)
        nativeSeries.call(FoundationInterop.IVector_RemoveAt, index)
        series.host = null
        pruneAxes()
        return series
    }

    /** Removes all series (and the axis registrations). */
    fun removeAllSeries() {
        if (seriesList.isEmpty()) return
        nativeSeries.call(FoundationInterop.IVector_Clear)
        for (series in seriesList) series.host = null
        seriesList.clear()
        pruneAxes()
    }

    /** The series at [index]. */
    fun getSeries(index: Int): CartesianSeries = seriesList[index]

    /** The list of series (in display order). */
    fun getSeriesList(): List<CartesianSeries> = seriesList.toList()

    /** The position of [series], or -1 if it has not been added. */
    fun indexOfSeries(series: CartesianSeries): Int = seriesList.indexOf(series)

    /**
     * The list of axes registered with the chart (Chart.Axes; determined automatically from the axes the series use).
     */
    fun getAxes(): List<Axis> = axisList.toList()

    /** Adds [axis] to Chart.Axes if it is not registered yet. */
    private fun registerAxis(axis: CartesianAxis) {
        if (axisList.any { it === axis }) return
        nativeAxes.call(FoundationInterop.IVector_Append, axis.axis)
        axisList += axis
    }

    /** Removes axes that no series uses any longer from Chart.Axes. */
    private fun pruneAxes() {
        for (index in axisList.indices.reversed()) {
            val axis = axisList[index]
            if (seriesList.none { it.xAxis === axis || it.yAxis === axis }) {
                nativeAxes.call(FoundationInterop.IVector_RemoveAt, index)
                axisList.removeAt(index)
            }
        }
    }

    /**
     * Reads the property [slot] that returns an IObservableVector, converts it to the IVector view [vectorIid],
     * and owns it.
     */
    private fun nativeVector(slot: Int, vectorIid: String): ComPtr {
        val observable = chart.getPtr(slot)
        return try {
            own(observable.queryInterface(vectorIid))
        } finally {
            observable.release()
        }
    }

    private companion object {
        fun create(): ComPtr {
            // Merge the theme resources referenced by the Chart default style into the app (only the first time)
            WinUiUtilities.ensureChartsResources()
            return Activation.composeDefault(ChartsInterop.CLS_Chart, ChartsInterop.IID_IChartFactory) // Default interface = IChart
        }
    }
}
