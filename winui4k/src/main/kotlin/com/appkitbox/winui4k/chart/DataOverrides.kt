package com.appkitbox.winui4k.chart

import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winui.ChartsInterop

/**
 * Charts.DataLabelOverride of WinUI 3: an override of the label (value display) of one data point.
 * Set it with [CartesianSeries.setDataLabelOverride] by specifying the data point position (e.g. showing "Peak" at the
 * maximum). Labels of data points with an override are shown even when [CartesianSeries.showDataLabels] is false.
 */
data class DataLabelOverride @JvmOverloads constructor(
    /** The string shown instead of the value. */
    val text: String,
    /** The label text color. If null, the series' [CartesianSeries.dataLabelColor] (the theme's default color if not set). */
    val color: WColor? = null,
) {
    /** Creates the native DataLabelOverride (the caller takes ownership). */
    internal fun createNative(): ComPtr {
        val factory = Activation.factory(ChartsInterop.CLS_DataLabelOverride, ChartsInterop.IID_IDataLabelOverrideFactory)
        val brush = color?.createBrush()
        return try {
            Hstring.use(text) { h ->
                factory.getPtr(ChartsInterop.IDataLabelOverrideFactory_CreateInstance, h, brush?.ptr)
            }
        } finally {
            brush?.release()
            factory.release()
        }
    }
}

/**
 * Charts.DataMarkerOverride of WinUI 3: an override of the marker of one data point.
 * Set it with [CartesianSeries.setDataMarkerOverride] by specifying the data point position (e.g. changing the shape
 * and color of outliers only). Markers of data points with an override are shown even when
 * [CartesianSeries.showDataMarkers] is false.
 */
data class DataMarkerOverride @JvmOverloads constructor(
    /** The marker shape. */
    val shape: MarkerShape,
    /** The marker color. If null, the series' [CartesianSeries.dataMarkerColor] (the chart's palette color if not set). */
    val color: WColor? = null,
) {
    /** Creates the native DataMarkerOverride (the caller takes ownership). */
    internal fun createNative(): ComPtr {
        val factory = Activation.factory(ChartsInterop.CLS_DataMarkerOverride, ChartsInterop.IID_IDataMarkerOverrideFactory)
        val brush = color?.createBrush()
        return try {
            factory.getPtr(ChartsInterop.IDataMarkerOverrideFactory_CreateInstance, shape.native, brush?.ptr)
        } finally {
            brush?.release()
            factory.release()
        }
    }
}
