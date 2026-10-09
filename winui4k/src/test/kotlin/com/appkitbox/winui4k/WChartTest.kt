package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.chart.AreaSeries
import com.appkitbox.winui4k.chart.BarOrientation
import com.appkitbox.winui4k.chart.BarSeries
import com.appkitbox.winui4k.chart.CartesianSeries
import com.appkitbox.winui4k.chart.CategoryAxis
import com.appkitbox.winui4k.chart.CategorySortKey
import com.appkitbox.winui4k.chart.DataLabelOverride
import com.appkitbox.winui4k.chart.DataMarkerOverride
import com.appkitbox.winui4k.chart.DateTimeAxis
import com.appkitbox.winui4k.chart.DateTimeIntervalType
import com.appkitbox.winui4k.chart.DefaultSampleModel
import com.appkitbox.winui4k.chart.GridLines
import com.appkitbox.winui4k.chart.LineSeries
import com.appkitbox.winui4k.chart.LinearAxis
import com.appkitbox.winui4k.chart.MarkerShape
import com.appkitbox.winui4k.chart.SortOrder
import com.appkitbox.winui4k.chart.StrokeDashStyle
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.WindowsRuntimeException
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.ChartsInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tests WChart and the com.appkitbox.winui4k.chart package (series, axes, data models) by checking them against the
 * state of the native Chart / CartesianSeries / Samples.
 */
class WChartTest : FunSpec() {
    /** The number of elements in the native Chart.Series / Chart.Axes. */
    private fun WChart.nativeCount(slot: Int, vectorIid: String): Int {
        val observable = inspectable.getPtr(slot)
        val vector = observable.queryInterface(vectorIid)
        return try {
            vector.getInt(FoundationInterop.IVector_get_Size)
        } finally {
            vector.release()
            observable.release()
        }
    }

    private fun WChart.nativeSeriesCount() =
        nativeCount(ChartsInterop.IChart_get_Series, ChartsInterop.IID_IVector_CartesianSeries)

    private fun WChart.nativeAxisCount() = nativeCount(ChartsInterop.IChart_get_Axes, ChartsInterop.IID_IVector_Axis)

    /** Returns the Samples.ItemsSource (IVector<Object>) of a series' XValues / YValues ([slot]) as a list of Kotlin values. */
    private fun CartesianSeries.nativeValues(slot: Int): List<Any?> {
        val samples = series.getPtr(slot)
        val source = samples.getPtr(ChartsInterop.ISamples_get_ItemsSource)
        val vector = source.queryInterface(FoundationInterop.IID_IVector_Object)
        try {
            return (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index ->
                val boxed = vector.getPtrOrNull(FoundationInterop.IVector_GetAt, index) ?: return@map null
                try {
                    PropertyValues.unboxDateTime(boxed)?.let { "ticks:$it" } ?: PropertyValues.unboxAny(boxed)
                } finally {
                    boxed.release()
                }
            }
        } finally {
            vector.release()
            source.release()
            samples.release()
        }
    }

    /** The color (A, R, G, B) of the SolidColorBrush in the Brush-typed property [slot]. Null if not set. */
    private fun ComPtr.brushColor(slot: Int): List<Int>? {
        val brush = getPtrOrNull(slot) ?: return null
        val solid = brush.queryInterface(XamlInterop.IID_ISolidColorBrush)
        return try {
            XamlStructs.getColor(solid, XamlInterop.ISolidColorBrush_get_Color).toList()
        } finally {
            solid.release()
            brush.release()
        }
    }

    init {
        test("showLegend and legendTitle return the values that were set") {
            onUiThreadGet {
                val chart = WChart()
                chart.showLegend = true
                chart.legendTitle = "Sales"
                chart.showLegend to chart.legendTitle
            } shouldBe (true to "Sales")
        }

        test("background and foreground are set to Control.Background / Foreground as a SolidColorBrush") {
            onUiThreadGet {
                val chart = WChart()
                chart.background = WColor(11, 26, 42)
                chart.foreground = WColor.WHITE
                val control = chart.inspectable.queryInterface(XamlInterop.IID_IControl)
                try {
                    listOf(
                        control.brushColor(XamlInterop.IControl_get_Background),
                        control.brushColor(XamlInterop.IControl_get_Foreground),
                    )
                } finally {
                    control.release()
                }
            } shouldBe listOf(listOf(255, 11, 26, 42), listOf(255, 255, 255, 255))
        }

        test("addSeries adds the series to Chart.Series and registers the axes it uses in Chart.Axes without duplicates") {
            onUiThreadGet {
                val chart = WChart()
                val month = CategoryAxis("Month")
                val sales = LinearAxis("Sales")
                chart.addSeries(
                    LineSeries("A").apply {
                        xAxis = month
                        yAxis = sales
                    },
                )
                chart.addSeries(
                    LineSeries("B").apply {
                        xAxis = month
                        yAxis = sales
                    },
                )
                listOf(chart.seriesCount, chart.nativeSeriesCount(), chart.nativeAxisCount(), chart.getAxes().size)
            } shouldBe listOf(2, 2, 2, 2)
        }

        test("insertSeries inserts at the given position, and getSeries / indexOfSeries look up by display position") {
            onUiThreadGet {
                val chart = WChart()
                val a = LineSeries("A")
                val b = LineSeries("B")
                val c = LineSeries("C")
                chart.addSeries(a)
                chart.addSeries(c)
                chart.insertSeries(1, b)
                val nativeTitles = chart.getSeriesList().map { it.title }
                listOf(nativeTitles, chart.getSeries(1).title, chart.indexOfSeries(c), chart.indexOfSeries(LineSeries()))
            } shouldBe listOf(listOf("A", "B", "C"), "B", 2, -1)
        }

        test("removeSeries removes from Chart.Axes only the axes that no series uses anymore") {
            onUiThreadGet {
                val chart = WChart()
                val month = CategoryAxis()
                val salesAxis = LinearAxis()
                val costAxis = LinearAxis()
                val sales = LineSeries("Sales").apply {
                    xAxis = month
                    yAxis = salesAxis
                }
                val cost = LineSeries("Cost").apply {
                    xAxis = month
                    yAxis = costAxis
                }
                chart.addSeries(sales)
                chart.addSeries(cost)
                chart.removeSeries(cost)
                val afterOne = listOf(chart.seriesCount, chart.nativeSeriesCount(), chart.nativeAxisCount())
                val kept = chart.getAxes().map { it === month || it === salesAxis }
                chart.removeSeries(cost) // Does nothing for a series that has not been added
                chart.removeAllSeries()
                Triple(afterOne, kept, listOf(chart.seriesCount, chart.nativeSeriesCount(), chart.nativeAxisCount()))
            } shouldBe Triple(listOf(1, 1, 2), listOf(true, true), listOf(0, 0, 0))
        }

        test("replacing an axis of a series already added to the chart registers the new axis and removes the one no longer used") {
            onUiThreadGet {
                val chart = WChart()
                val oldAxis = LinearAxis()
                val newAxis = LinearAxis()
                val series = LineSeries().apply { yAxis = oldAxis }
                chart.addSeries(series)
                series.yAxis = newAxis
                val replaced = chart.getAxes().single() === newAxis
                series.yAxis = null
                Triple(replaced, chart.getAxes().size, chart.nativeAxisCount())
            } shouldBe Triple(true, 0, 0)
        }

        test("removeSeriesAt returns the removed series, which can then be added to another chart") {
            onUiThreadGet {
                val first = WChart()
                val second = WChart()
                val series = LineSeries("A")
                first.addSeries(series)
                val removed = first.removeSeriesAt(0)
                second.addSeries(series)
                listOf(removed === series, first.nativeSeriesCount(), second.nativeSeriesCount())
            } shouldBe listOf(true, 0, 1)
        }

        test("adding a series that is already added to another chart throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                onUiThread {
                    val series = LineSeries()
                    WChart().addSeries(series)
                    WChart().addSeries(series)
                }
            }
        }

        test("sharing an axis between series of different orientations fails to add, leaving the chart's series and axes unchanged") {
            val (error, state) = onUiThreadGet {
                val chart = WChart()
                val month = CategoryAxis()
                val value = LinearAxis()
                chart.addSeries(
                    LineSeries().apply {
                        xAxis = month
                        yAxis = value
                    },
                )
                // BarSeries is horizontal by default (its axis orientation is the opposite of a vertical line series)
                val bar = BarSeries().apply {
                    xAxis = month
                    yAxis = value
                }
                val error = runCatching { chart.addSeries(bar) }.exceptionOrNull()
                error to listOf(chart.seriesCount, chart.nativeSeriesCount(), chart.nativeAxisCount())
            }
            (error is WindowsRuntimeException) shouldBe true
            state shouldBe listOf(1, 1, 2)
        }

        test("a vertical bar series can share axes with line and area series") {
            onUiThreadGet {
                val chart = WChart()
                val month = CategoryAxis()
                val value = LinearAxis()
                chart.addSeries(
                    AreaSeries().apply {
                        xAxis = month
                        yAxis = value
                    },
                )
                chart.addSeries(
                    BarSeries().apply {
                        orientation = BarOrientation.VERTICAL
                        xAxis = month
                        yAxis = value
                    },
                )
                chart.addSeries(
                    LineSeries().apply {
                        xAxis = month
                        yAxis = value
                    },
                )
                chart.nativeSeriesCount() to chart.nativeAxisCount()
            } shouldBe (3 to 2)
        }

        test("series defaults: visible, a solid line, and no labels or markers") {
            onUiThreadGet {
                val series = LineSeries()
                listOf(series.title, series.isVisible, series.strokeDashStyle, series.showDataLabels, series.showDataMarkers)
            } shouldBe listOf("", true, StrokeDashStyle.SOLID, false, false)
        }

        test("series appearance properties return the values that were set") {
            onUiThreadGet {
                val series = LineSeries("Visits")
                series.isVisible = false
                series.strokeThickness = 3.5
                series.strokeDashStyle = StrokeDashStyle.DASH_DOT
                series.showDataLabels = true
                series.showDataMarkers = true
                series.markerShape = MarkerShape.TRIANGLE
                listOf(
                    series.title,
                    series.isVisible,
                    series.strokeThickness,
                    series.strokeDashStyle,
                    series.showDataLabels,
                    series.showDataMarkers,
                    series.markerShape,
                )
            } shouldBe listOf("Visits", false, 3.5, StrokeDashStyle.DASH_DOT, true, true, MarkerShape.TRIANGLE)
        }

        test("a series color is set natively as a SolidColorBrush and cleared with null") {
            onUiThreadGet {
                val series = LineSeries()
                series.stroke = WColor(0, 120, 212)
                series.dataLabelColor = WColor(1, 2, 3, 128)
                series.dataMarkerColor = WColor.RED
                val set = listOf(
                    series.series.brushColor(ChartsInterop.ICartesianSeries_get_Stroke),
                    series.series.brushColor(ChartsInterop.ICartesianSeries_get_DataLabelBrush),
                    series.series.brushColor(ChartsInterop.ICartesianSeries_get_DataMarkerBrush),
                )
                series.stroke = null
                set to listOf(series.stroke, series.series.brushColor(ChartsInterop.ICartesianSeries_get_Stroke))
            } shouldBe (
                listOf(listOf(255, 0, 120, 212), listOf(128, 1, 2, 3), listOf(255, 237, 28, 36)) to listOf(null, null)
                )
        }

        test("fill colors of area and bar series, and the bar orientation") {
            onUiThreadGet {
                val area = AreaSeries()
                val translucent = WColor(0, 120, 212, 77)
                area.fill = translucent
                val bar = BarSeries()
                val defaultOrientation = bar.orientation
                bar.orientation = BarOrientation.VERTICAL
                bar.fill = WColor.GREEN
                listOf(
                    area.inspectable.brushColor(ChartsInterop.IAreaSeries_get_Fill),
                    defaultOrientation,
                    bar.orientation,
                    bar.inspectable.brushColor(ChartsInterop.IBarSeries_get_Fill),
                    area.fill === translucent,
                )
            } shouldBe listOf(
                listOf(77, 0, 120, 212),
                BarOrientation.HORIZONTAL,
                BarOrientation.VERTICAL,
                listOf(255, 34, 177, 76),
                true,
            )
        }

        test("a series model appears as Samples.ItemsSource, with numbers as Double, strings as is, and dates as DateTime") {
            onUiThreadGet {
                val series = LineSeries(
                    xValues = DefaultSampleModel.of("Jan", "Feb"),
                    yValues = DefaultSampleModel.of(1, 2.5f),
                )
                val dates = LineSeries(xValues = DefaultSampleModel.of(LocalDate.of(2026, 8, 3)))
                // A LocalDate is passed as midnight UTC on that day (because Chart displays dates and times in UTC)
                val expectedTicks = DateTimeConversions.instantToTicks(Instant.parse("2026-08-03T00:00:00Z"))
                listOf(
                    series.nativeValues(ChartsInterop.ICartesianSeries_get_XValues),
                    series.nativeValues(ChartsInterop.ICartesianSeries_get_YValues),
                    dates.nativeValues(ChartsInterop.ICartesianSeries_get_XValues) == listOf("ticks:$expectedTicks"),
                )
            } shouldBe listOf(listOf("Jan", "Feb"), listOf(1.0, 2.5), true)
        }

        test("changes to a model set on a series are immediately visible in the native Samples") {
            onUiThreadGet {
                val values = DefaultSampleModel.of(10.0, 20.0)
                val series = LineSeries(yValues = values)
                values.addElement(30.0)
                values[0] = 15.0
                values.remove(1)
                series.nativeValues(ChartsInterop.ICartesianSeries_get_YValues)
            } shouldBe listOf(15.0, 30.0)
        }

        test("a change to one model element is a VectorChanged with its position, and a change to multiple elements is a Reset") {
            onUiThreadGet {
                val values = DefaultSampleModel.of(1.0, 2.0)
                val series = LineSeries(yValues = values)
                val samples = series.series.getPtr(ChartsInterop.ICartesianSeries_get_YValues)
                val source = samples.getPtr(ChartsInterop.ISamples_get_ItemsSource)
                val observable = source.queryInterface(FoundationInterop.IID_IObservableVector_Object)
                val received = mutableListOf<Pair<Int, Int>>()
                observable.addEventHandler("TestVectorChangedHandler", FoundationInterop.IID_VectorChangedEventHandler_Object, 6) { _, args ->
                    val changed = ComPtr(args)
                    received += changed.getInt(6) to changed.getInt(7) // CollectionChange, Index
                }
                values.addElement(3.0)
                values[0] = 9.0
                values.remove(1)
                values.addAll(listOf(4.0, 5.0))
                observable.release()
                source.release()
                samples.release()
                received
            } shouldBe listOf(
                FoundationInterop.CollectionChange_ItemInserted to 2,
                FoundationInterop.CollectionChange_ItemChanged to 0,
                FoundationInterop.CollectionChange_ItemRemoved to 1,
                FoundationInterop.CollectionChange_Reset to 0,
            )
        }

        test("xValues / yValues return the model that was set and are cleared with null") {
            onUiThreadGet {
                val model = DefaultSampleModel.of(1.0)
                val series = LineSeries(yValues = model)
                val same = series.yValues === model
                series.yValues = null
                val cleared = series.series.getPtrOrNull(ChartsInterop.ICartesianSeries_get_YValues)
                cleared?.release()
                listOf(same, series.yValues == null, cleared == null)
            } shouldBe listOf(true, true, true)
        }

        test("data point label and marker overrides go into the native DataLabelOverrides / DataMarkerOverrides") {
            onUiThreadGet {
                val series = LineSeries()
                series.setDataLabelOverride(3, DataLabelOverride("Peak"))
                series.setDataLabelOverride(1, DataLabelOverride("Low", WColor.BLUE))
                series.setDataMarkerOverride(3, DataMarkerOverride(MarkerShape.DIAMOND, WColor.PURPLE))
                val labels = series.series.getPtr(ChartsInterop.ICartesianSeries_get_DataLabelOverrides)
                    .queryInterface(ChartsInterop.IID_IMap_UInt32_DataLabelOverride)
                val peak = labels.getPtr(FoundationInterop.IMap_Lookup, 3)
                val markers = series.series.getPtr(ChartsInterop.ICartesianSeries_get_DataMarkerOverrides)
                    .queryInterface(ChartsInterop.IID_IMap_UInt32_DataMarkerOverride)
                val marker = markers.getPtr(FoundationInterop.IMap_Lookup, 3)
                val result = listOf(
                    labels.getInt(FoundationInterop.IMap_get_Size),
                    peak.getString(ChartsInterop.IDataLabelOverride_get_Text),
                    peak.getPtrOrNull(ChartsInterop.IDataLabelOverride_get_Brush) == null,
                    MarkerShape.of(marker.getInt(ChartsInterop.IDataMarkerOverride_get_Shape)),
                    marker.brushColor(ChartsInterop.IDataMarkerOverride_get_Brush),
                    series.getDataLabelOverrides().keys.toList(),
                    series.getDataLabelOverride(1),
                    series.getDataMarkerOverride(3),
                )
                marker.release()
                markers.release()
                peak.release()
                labels.release()
                result
            } shouldBe listOf(
                2,
                "Peak",
                true,
                MarkerShape.DIAMOND,
                listOf(255, 163, 73, 164),
                listOf(1, 3),
                DataLabelOverride("Low", WColor.BLUE),
                DataMarkerOverride(MarkerShape.DIAMOND, WColor.PURPLE),
            )
        }

        test("setting null removes one override, clear removes all of them, and setting the same position again replaces it") {
            onUiThreadGet {
                val series = LineSeries()
                series.setDataLabelOverride(0, DataLabelOverride("a"))
                series.setDataLabelOverride(0, DataLabelOverride("b"))
                series.setDataLabelOverride(2, DataLabelOverride("c"))
                series.setDataLabelOverride(5, null) // Does nothing at a position without an override
                series.setDataLabelOverride(2, null)
                val labels = series.series.getPtr(ChartsInterop.ICartesianSeries_get_DataLabelOverrides)
                    .queryInterface(ChartsInterop.IID_IMap_UInt32_DataLabelOverride)
                val afterRemove = labels.getInt(FoundationInterop.IMap_get_Size) to series.getDataLabelOverrides()
                series.setDataMarkerOverride(4, DataMarkerOverride(MarkerShape.X))
                series.clearDataLabelOverrides()
                series.clearDataMarkerOverrides()
                val afterClear = labels.getInt(FoundationInterop.IMap_get_Size)
                labels.release()
                listOf(afterRemove, afterClear, series.getDataLabelOverrides(), series.getDataMarkerOverrides())
            } shouldBe listOf(1 to mapOf(0 to DataLabelOverride("b")), 0, emptyMap<Int, Any>(), emptyMap<Int, Any>())
        }

        test("override positions cannot be negative") {
            shouldThrow<IllegalArgumentException> {
                onUiThread { LineSeries().setDataLabelOverride(-1, DataLabelOverride("x")) }
            }
        }

        test("common axis properties (label, visibility, ticks, gridlines) hide only the tick marks by default and return the values that were set") {
            onUiThreadGet {
                val axis = LinearAxis("Value")
                val defaults = listOf(axis.isVisible, axis.showTickLabels, axis.showTickMarks)
                axis.isVisible = false
                axis.showTickLabels = false
                axis.showTickMarks = true
                axis.gridLines = GridLines.MINOR
                defaults to listOf(axis.label, axis.isVisible, axis.showTickLabels, axis.showTickMarks, axis.gridLines)
            } shouldBe (listOf(true, true, false) to listOf("Value", false, false, true, GridLines.MINOR))
        }

        test("axis colors are set natively as a SolidColorBrush") {
            onUiThreadGet {
                val axis = CategoryAxis()
                axis.gridLineMajorColor = WColor(128, 128, 128, 102)
                axis.gridLineMinorColor = WColor(128, 128, 128, 51)
                axis.tickColor = WColor.BLACK
                axis.tickLabelColor = WColor.BLUE
                axis.axisLineColor = WColor.GRAY
                val native = axis.cartesianAxis
                listOf(
                    ChartsInterop.ICartesianAxis_get_GridLineMajorBrush,
                    ChartsInterop.ICartesianAxis_get_GridLineMinorBrush,
                    ChartsInterop.ICartesianAxis_get_TickBrush,
                    ChartsInterop.ICartesianAxis_get_TickLabelBrush,
                    ChartsInterop.ICartesianAxis_get_AxisLineBrush,
                ).map { native.brushColor(it) }
            } shouldBe listOf(
                listOf(102, 128, 128, 128),
                listOf(51, 128, 128, 128),
                listOf(255, 0, 0, 0),
                listOf(255, 0, 120, 215),
                listOf(255, 128, 128, 128),
            )
        }

        test("LinearAxis range and interval are null (automatic) by default, return the values that were set, and revert to automatic with null") {
            onUiThreadGet {
                val axis = LinearAxis()
                val defaults = listOf(axis.minimum, axis.maximum, axis.spacing)
                axis.minimum = 0.0
                axis.maximum = 100.0
                axis.spacing = 20.0
                val set = listOf(axis.minimum, axis.maximum, axis.spacing)
                axis.maximum = null
                Triple(defaults, set, axis.maximum)
            } shouldBe Triple(listOf(null, null, null), listOf(0.0, 100.0, 20.0), null)
        }

        test("CategoryAxis sorting is ascending by position by default and returns the value that was set") {
            onUiThreadGet {
                val axis = CategoryAxis()
                val defaults = axis.sortKey to axis.sortOrder
                axis.sortKey = CategorySortKey.VALUE
                axis.sortOrder = SortOrder.DESCENDING
                defaults to (axis.sortKey to axis.sortOrder)
            } shouldBe ((CategorySortKey.INDEX to SortOrder.ASCENDING) to (CategorySortKey.VALUE to SortOrder.DESCENDING))
        }

        test("DateTimeAxis range, tick unit, and label format return the values that were set") {
            onUiThreadGet {
                val axis = DateTimeAxis("Date")
                val defaults = listOf(axis.minimum, axis.maximum, axis.intervalType, axis.labelFormat)
                axis.minimum = LocalDateTime.of(2026, 7, 27, 0, 0)
                axis.maximum = LocalDateTime.of(2026, 9, 7, 12, 30, 15)
                axis.intervalType = DateTimeIntervalType.WEEK
                axis.labelFormat = "month day"
                val set = listOf(axis.minimum, axis.maximum, axis.intervalType, axis.labelFormat)
                axis.minimum = null
                Triple(defaults, set, axis.minimum)
            } shouldBe Triple(
                listOf(null, null, DateTimeIntervalType.AUTO, ""),
                listOf(
                    LocalDateTime.of(2026, 7, 27, 0, 0),
                    LocalDateTime.of(2026, 9, 7, 12, 30, 15),
                    DateTimeIntervalType.WEEK,
                    "month day",
                ),
                null,
            )
        }

        test("a DateTimeAxis range given as LocalDateTime is passed natively as a UTC date-time (Chart displays dates and times in UTC)") {
            onUiThreadGet {
                val axis = DateTimeAxis()
                axis.minimum = LocalDateTime.of(2026, 7, 27, 0, 0)
                val boxed = axis.inspectable.getPtr(ChartsInterop.IDateTimeAxis_get_Minimum)
                try {
                    PropertyValues.unboxDateTime(boxed)
                } finally {
                    boxed.release()
                }
            } shouldBe DateTimeConversions.instantToTicks(Instant.parse("2026-07-27T00:00:00Z"))
        }

        test("a chart with series and axes can be shown in a window, and changing models while it is shown does not throw") {
            val months = DefaultSampleModel.of("Jan", "Feb", "Mar")
            val values = DefaultSampleModel.of(10.0, 30.0, 20.0)
            val chart = onUiThreadGet {
                WChart().apply {
                    height = 240.0
                    showLegend = true
                    addSeries(
                        LineSeries("A", months, values).apply {
                            xAxis = CategoryAxis()
                            yAxis = LinearAxis().apply { minimum = 0.0 }
                            showDataMarkers = true
                            setDataLabelOverride(1, DataLabelOverride("Peak"))
                        },
                    )
                }
            }
            UiTestHarness.attachAndAwaitLoaded(chart)
            onUiThread {
                months.addElement("Apr")
                values.addElement(40.0)
                values.removeRange(0, 1)
                months.removeRange(0, 1)
            }
            onUiThreadGet { chart.getSeries(0).nativeValues(ChartsInterop.ICartesianSeries_get_YValues) } shouldBe
                listOf(20.0, 40.0)
            UiTestHarness.detach(chart)
        }
    }
}
