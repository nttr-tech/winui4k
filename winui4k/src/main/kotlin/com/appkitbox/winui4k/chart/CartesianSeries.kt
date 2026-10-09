package com.appkitbox.winui4k.chart

import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.ChartsInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import java.util.TreeMap
import kotlin.jvm.JvmSynthetic

/**
 * The dash pattern of a line (Charts.StrokeDashStyle). Values are extracted from the winmd.
 */
enum class StrokeDashStyle(internal val native: Int) {
    /** Solid line. */
    SOLID(0),

    /** Dashed line. */
    DASH(1),

    /** Dotted line. */
    DOT(2),

    /** Dash-dot line. */
    DASH_DOT(3),

    /** Dash-dot-dot line. */
    DASH_DOT_DOT(4),
    ;

    internal companion object {
        fun of(native: Int): StrokeDashStyle = entries.first { it.native == native }
    }
}

/**
 * The shape of a data point marker (Charts.MarkerShape). Values are extracted from the winmd.
 */
enum class MarkerShape(internal val native: Int) {
    /** No marker. */
    NONE(0),

    /** Square. */
    SQUARE(1),

    /** Diamond. */
    DIAMOND(2),

    /** Triangle. */
    TRIANGLE(3),

    /** X mark. */
    X(4),

    /** Asterisk (*). */
    ASTERISK(5),

    /** Short horizontal dash. */
    SHORT_DASH(6),

    /** Long horizontal dash. */
    LONG_DASH(7),

    /** Circle. */
    CIRCLE(8),

    /** Plus (+). */
    PLUS(9),
    ;

    internal companion object {
        fun of(native: Int): MarkerShape = entries.first { it.native == native }
    }
}

/**
 * Notifications of a series' axis changes, received by the chart (WChart) displaying the series.
 * Since the chart must register the axes its series use in Chart.Axes, these are called before and after an axis is
 * replaced.
 */
internal interface SeriesHost {
    /** Called just before [axis] is set on a series (registers it if it is not yet registered with the chart). */
    fun axisAdding(axis: CartesianAxis)

    /** Called after a series' axis is replaced (unregisters axes no series uses anymore). */
    fun axesChanged()
}

/**
 * Charts.CartesianSeries of WinUI 3: the common attributes of a series (one sequence of data) drawn in Cartesian
 * coordinates.
 *
 * Data is passed as [SampleModel]s of X values ([xValues]) and Y values ([yValues]), and elements at the same position
 * are drawn as one data point. If X values are omitted, the element numbers (1, 2, 3, ...) become X.
 * Model changes are reflected immediately in a displayed chart.
 * If the axes ([xAxis] / [yAxis]) are omitted, the chart uses default axes suited to the value types.
 *
 * For a concrete series, use [LineSeries] (line) / [AreaSeries] (area) / [BarSeries] (bar), and add it to a chart with
 * [com.appkitbox.winui4k.WChart.addSeries].
 * Color properties use the chart's palette (colors assigned automatically per series) or the theme's default color if
 * null. Create and use it on the UI thread.
 */
abstract class CartesianSeries internal constructor(
    /** The default interface pointer of the series (ILineSeries / IAreaSeries / IBarSeries). */
    internal val inspectable: ComPtr,
) {
    /** The record of COM references this wrapper owns (the same mechanism as WComponent). */
    private val lifetime = ComLifetime.adopt(this, inspectable)

    /** Ties ownership of [ptr] to this wrapper's lifetime. */
    internal fun own(ptr: ComPtr): ComPtr = lifetime.own(ptr)

    /** The ICartesianSeries view (the pointer added to Chart.Series). */
    internal val series: ComPtr by lazy { own(inspectable.queryInterface(ChartsInterop.IID_ICartesianSeries)) }

    /** CartesianSeries.DataLabelOverrides (an IMap<UInt32, DataLabelOverride> view). */
    private val nativeLabelOverrides: ComPtr by lazy {
        nativeMap(ChartsInterop.ICartesianSeries_get_DataLabelOverrides, ChartsInterop.IID_IMap_UInt32_DataLabelOverride)
    }

    /** CartesianSeries.DataMarkerOverrides (an IMap<UInt32, DataMarkerOverride> view). */
    private val nativeMarkerOverrides: ComPtr by lazy {
        nativeMap(ChartsInterop.ICartesianSeries_get_DataMarkerOverrides, ChartsInterop.IID_IMap_UInt32_DataMarkerOverride)
    }

    private val labelOverrides = TreeMap<Int, DataLabelOverride>()
    private val markerOverrides = TreeMap<Int, DataMarkerOverride>()

    /** The chart displaying this series, or null if it has not been added to a chart. */
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var host: SeriesHost? = null

    /** The series name (CartesianSeries.Title), shown in the legend. */
    var title: String
        get() = series.getString(ChartsInterop.ICartesianSeries_get_Title)
        set(value) {
            Hstring.use(value) { h -> series.call(ChartsInterop.ICartesianSeries_put_Title, h) }
        }

    /** Whether to show the series (CartesianSeries.IsVisible). A series set to false is not drawn and does not appear in the legend. */
    var isVisible: Boolean
        get() = series.getBool(ChartsInterop.ICartesianSeries_get_IsVisible)
        set(value) {
            series.putBool(ChartsInterop.ICartesianSeries_put_IsVisible, value)
        }

    /** The model of X values (CartesianSeries.XValues). If null, the element numbers (1, 2, 3, ...) become X. */
    var xValues: SampleModel<*>? = null
        set(value) {
            field = value
            putSamples(ChartsInterop.ICartesianSeries_put_XValues, value)
        }

    /** The model of Y values (CartesianSeries.YValues). */
    var yValues: SampleModel<*>? = null
        set(value) {
            field = value
            putSamples(ChartsInterop.ICartesianSeries_put_YValues, value)
        }

    /** The axis that displays the X values (CartesianSeries.XAxis). If null, a default axis suited to the value type. */
    var xAxis: CartesianAxis? = null
        set(value) {
            value?.let { host?.axisAdding(it) }
            series.call(ChartsInterop.ICartesianSeries_put_XAxis, value?.cartesianAxis)
            field = value
            host?.axesChanged()
        }

    /** The axis that displays the Y values (CartesianSeries.YAxis). If null, a default axis suited to the value type. */
    var yAxis: CartesianAxis? = null
        set(value) {
            value?.let { host?.axisAdding(it) }
            series.call(ChartsInterop.ICartesianSeries_put_YAxis, value?.cartesianAxis)
            field = value
            host?.axesChanged()
        }

    /** The line color (CartesianSeries.Stroke). The chart's palette color if null. */
    var stroke: WColor? = null
        set(value) {
            field = value
            series.putColor(ChartsInterop.ICartesianSeries_put_Stroke, value)
        }

    /** The line thickness (CartesianSeries.StrokeThickness, in pixels). */
    var strokeThickness: Double
        get() = series.getDouble(ChartsInterop.ICartesianSeries_get_StrokeThickness)
        set(value) {
            series.call(ChartsInterop.ICartesianSeries_put_StrokeThickness, value)
        }

    /** The dash pattern of the line (CartesianSeries.StrokeDashStyle). */
    var strokeDashStyle: StrokeDashStyle
        get() = StrokeDashStyle.of(series.getInt(ChartsInterop.ICartesianSeries_get_StrokeDashStyle))
        set(value) {
            series.call(ChartsInterop.ICartesianSeries_put_StrokeDashStyle, value.native)
        }

    /** Whether to show a value label on each data point (CartesianSeries.ShowDataLabels). */
    var showDataLabels: Boolean
        get() = series.getBool(ChartsInterop.ICartesianSeries_get_ShowDataLabels)
        set(value) {
            series.putBool(ChartsInterop.ICartesianSeries_put_ShowDataLabels, value)
        }

    /** Whether to show a marker on each data point (CartesianSeries.ShowDataMarkers). */
    var showDataMarkers: Boolean
        get() = series.getBool(ChartsInterop.ICartesianSeries_get_ShowDataMarkers)
        set(value) {
            series.putBool(ChartsInterop.ICartesianSeries_put_ShowDataMarkers, value)
        }

    /** The shape of the data point markers (CartesianSeries.MarkerShape). */
    var markerShape: MarkerShape
        get() = MarkerShape.of(series.getInt(ChartsInterop.ICartesianSeries_get_MarkerShape))
        set(value) {
            series.call(ChartsInterop.ICartesianSeries_put_MarkerShape, value.native)
        }

    /** The text color of the value labels (CartesianSeries.DataLabelBrush). The theme's default color if null. */
    var dataLabelColor: WColor? = null
        set(value) {
            field = value
            series.putColor(ChartsInterop.ICartesianSeries_put_DataLabelBrush, value)
        }

    /** The color of the data point markers (CartesianSeries.DataMarkerBrush). The chart's palette color if null (not necessarily the line color). */
    var dataMarkerColor: WColor? = null
        set(value) {
            field = value
            series.putColor(ChartsInterop.ICartesianSeries_put_DataMarkerBrush, value)
        }

    /**
     * Overrides the label of the [index]-th data point with [override] (CartesianSeries.DataLabelOverrides).
     * null removes the override. The data point position is the position of the element in the model and does not
     * shift when elements are added or removed (set it again).
     */
    fun setDataLabelOverride(index: Int, override: DataLabelOverride?) {
        require(index >= 0) { "index must be >= 0: $index" }
        if (override == null) {
            if (labelOverrides.remove(index) != null) nativeLabelOverrides.call(FoundationInterop.IMap_Remove, index)
            return
        }
        nativeLabelOverrides.insert(index, override.createNative())
        labelOverrides[index] = override
    }

    /** The label override of the [index]-th data point, or null if none. */
    fun getDataLabelOverride(index: Int): DataLabelOverride? = labelOverrides[index]

    /** All label overrides (data point position → override, in ascending order of position). */
    fun getDataLabelOverrides(): Map<Int, DataLabelOverride> = LinkedHashMap(labelOverrides)

    /** Removes all label overrides. */
    fun clearDataLabelOverrides() {
        labelOverrides.clear()
        nativeLabelOverrides.call(FoundationInterop.IMap_Clear)
    }

    /**
     * Overrides the marker of the [index]-th data point with [override] (CartesianSeries.DataMarkerOverrides).
     * null removes the override. The data point position is the position of the element in the model and does not
     * shift when elements are added or removed (set it again).
     */
    fun setDataMarkerOverride(index: Int, override: DataMarkerOverride?) {
        require(index >= 0) { "index must be >= 0: $index" }
        if (override == null) {
            if (markerOverrides.remove(index) != null) nativeMarkerOverrides.call(FoundationInterop.IMap_Remove, index)
            return
        }
        nativeMarkerOverrides.insert(index, override.createNative())
        markerOverrides[index] = override
    }

    /** The marker override of the [index]-th data point, or null if none. */
    fun getDataMarkerOverride(index: Int): DataMarkerOverride? = markerOverrides[index]

    /** All marker overrides (data point position → override, in ascending order of position). */
    fun getDataMarkerOverrides(): Map<Int, DataMarkerOverride> = LinkedHashMap(markerOverrides)

    /** Removes all marker overrides. */
    fun clearDataMarkerOverrides() {
        markerOverrides.clear()
        nativeMarkerOverrides.call(FoundationInterop.IMap_Clear)
    }

    /** Sets Samples whose ItemsSource is [model] on XValues / YValues ([slot]). null clears it. */
    private fun putSamples(slot: Int, model: SampleModel<*>?) {
        if (model == null) {
            series.call(slot, null)
            return
        }
        // The series holds the reference to Samples (the Kotlin side does not keep it; the series releases it when replaced)
        val samples = SampleModelVector.createSamples(model)
        try {
            series.call(slot, samples)
        } finally {
            samples.release()
        }
    }

    /** Reads the property [slot] that returns an IObservableMap, and owns it as the IMap view [mapIid]. */
    private fun nativeMap(slot: Int, mapIid: String): ComPtr {
        val observable = series.getPtr(slot)
        return try {
            own(observable.queryInterface(mapIid))
        } finally {
            observable.release()
        }
    }

    /** Calls IMap<UInt32, V>.Insert([key], [value]) and releases the reference to [value]. */
    private fun ComPtr.insert(key: Int, value: ComPtr) {
        try {
            Ffi.backend.withScope { scope ->
                val replaced = scope.allocate(1, 1)
                call(FoundationInterop.IMap_Insert, key, value, replaced)
            }
        } finally {
            value.release()
        }
    }
}
