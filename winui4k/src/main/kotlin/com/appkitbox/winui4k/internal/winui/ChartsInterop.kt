package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for Chart (the Microsoft.UI.Xaml.Controls.Charts namespace).
 *
 * All values were mechanically extracted with tools/dump_winmd.py from metadata/Microsoft.UI.Xaml.winmd of
 * Microsoft.WindowsAppSDK.WinUI 2.3.10-experimental (a dependency of Microsoft.WindowsAppSDK 2.5.4-experimental).
 * Not a single value is handwritten or guessed. The types are experimental (WinUIChartingContract), and the
 * implementation lives in a separate DLL (Microsoft.UI.Xaml.Controls.Charts.dll).
 * Experimental APIs may change before they become stable, so they are kept separate from XamlInterop.
 * Enum values are placed in the public API's enum classes (the com.appkitbox.winui4k.chart package).
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, and the interface body
 * starts at 6 in the winmd's method-declaration order.
 */
internal object ChartsInterop {
    private const val NS = "Microsoft.UI.Xaml.Controls.Charts"

    // ---- Microsoft.UI.Xaml.Controls.Charts.Chart ----
    // base: Control / composable factory: IChartFactory / statics: IChartStatics
    const val CLS_Chart = "$NS.Chart"
    const val IID_IChart = "181b158c-73fc-56e4-8660-fd908fc5dd84"
    const val IID_IChartFactory = "538a4af8-eddf-547f-bb3d-7ce41eb19c90"
    const val IChart_get_Data = 6                       // get_Data(out IObservableVector<Samples>)
    const val IChart_get_Axes = 7                       // get_Axes(out IObservableVector<Axis>)
    const val IChart_get_Series = 8                     // get_Series(out IObservableVector<CartesianSeries>)
    const val IChart_get_ShowLegend = 9                 // get_ShowLegend(out boolean)
    const val IChart_put_ShowLegend = 10                // put_ShowLegend(boolean)
    const val IChart_get_LegendTitle = 11               // get_LegendTitle(out HSTRING)
    const val IChart_put_LegendTitle = 12               // put_LegendTitle(HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.Charts.Samples ----
    // base: DependencyObject / activatable factory: <default IActivationFactory>
    const val CLS_Samples = "$NS.Samples"
    const val IID_ISamples = "edda9a4a-1c5a-5a07-b086-77e32ff75c8d"
    const val ISamples_get_ItemsSource = 6              // get_ItemsSource(out object)
    const val ISamples_put_ItemsSource = 7              // put_ItemsSource(object)

    // ---- Microsoft.UI.Xaml.Controls.Charts.CartesianSeries ----
    // base: DependencyObject / composable factory: ICartesianSeriesProtectedFactory (for derived classes only)
    const val IID_ICartesianSeries = "ca75923f-6de1-5aef-a353-aa9a15fd2223"
    const val ICartesianSeries_get_Title = 6            // get_Title(out HSTRING)
    const val ICartesianSeries_put_Title = 7            // put_Title(HSTRING)
    const val ICartesianSeries_get_IsVisible = 8        // get_IsVisible(out boolean)
    const val ICartesianSeries_put_IsVisible = 9        // put_IsVisible(boolean)
    const val ICartesianSeries_get_XValues = 10         // get_XValues(out Samples)
    const val ICartesianSeries_put_XValues = 11         // put_XValues(Samples)
    const val ICartesianSeries_get_YValues = 12         // get_YValues(out Samples)
    const val ICartesianSeries_put_YValues = 13         // put_YValues(Samples)
    const val ICartesianSeries_get_Stroke = 14          // get_Stroke(out Brush)
    const val ICartesianSeries_put_Stroke = 15          // put_Stroke(Brush)
    const val ICartesianSeries_get_StrokeThickness = 16 // get_StrokeThickness(out r8)
    const val ICartesianSeries_put_StrokeThickness = 17 // put_StrokeThickness(r8)
    const val ICartesianSeries_get_StrokeDashStyle = 18 // get_StrokeDashStyle(out StrokeDashStyle)
    const val ICartesianSeries_put_StrokeDashStyle = 19 // put_StrokeDashStyle(StrokeDashStyle)
    const val ICartesianSeries_get_ShowDataLabels = 20  // get_ShowDataLabels(out boolean)
    const val ICartesianSeries_put_ShowDataLabels = 21  // put_ShowDataLabels(boolean)
    const val ICartesianSeries_get_ShowDataMarkers = 22 // get_ShowDataMarkers(out boolean)
    const val ICartesianSeries_put_ShowDataMarkers = 23 // put_ShowDataMarkers(boolean)
    const val ICartesianSeries_get_MarkerShape = 24     // get_MarkerShape(out MarkerShape)
    const val ICartesianSeries_put_MarkerShape = 25     // put_MarkerShape(MarkerShape)
    const val ICartesianSeries_get_DataLabelBrush = 26  // get_DataLabelBrush(out Brush)
    const val ICartesianSeries_put_DataLabelBrush = 27  // put_DataLabelBrush(Brush)
    const val ICartesianSeries_get_DataMarkerBrush = 28 // get_DataMarkerBrush(out Brush)
    const val ICartesianSeries_put_DataMarkerBrush = 29 // put_DataMarkerBrush(Brush)
    const val ICartesianSeries_get_DataLabelOverrides = 30  // get_DataLabelOverrides(out IObservableMap<UInt32, DataLabelOverride>)
    const val ICartesianSeries_get_DataMarkerOverrides = 31 // get_DataMarkerOverrides(out IObservableMap<UInt32, DataMarkerOverride>)
    const val ICartesianSeries_get_XAxis = 32           // get_XAxis(out CartesianAxis)
    const val ICartesianSeries_put_XAxis = 33           // put_XAxis(CartesianAxis)
    const val ICartesianSeries_get_YAxis = 34           // get_YAxis(out CartesianAxis)
    const val ICartesianSeries_put_YAxis = 35           // put_YAxis(CartesianAxis)

    // ---- Microsoft.UI.Xaml.Controls.Charts.LineSeries ----
    // base: CartesianSeries / composable factory: ILineSeriesFactory (ILineSeries has no members)
    const val CLS_LineSeries = "$NS.LineSeries"
    const val IID_ILineSeriesFactory = "006840f7-4ee5-54b9-bbc0-38360b8a1962"

    // ---- Microsoft.UI.Xaml.Controls.Charts.AreaSeries ----
    // base: CartesianSeries / composable factory: IAreaSeriesFactory
    const val CLS_AreaSeries = "$NS.AreaSeries"
    const val IID_IAreaSeries = "39987537-709d-56c2-88de-f7748d889fb8"
    const val IID_IAreaSeriesFactory = "b8d5831f-2f75-5a15-a0ac-5af47c126bb7"
    const val IAreaSeries_get_Fill = 6                  // get_Fill(out Brush)
    const val IAreaSeries_put_Fill = 7                  // put_Fill(Brush)

    // ---- Microsoft.UI.Xaml.Controls.Charts.BarSeries ----
    // base: CartesianSeries / composable factory: IBarSeriesFactory
    const val CLS_BarSeries = "$NS.BarSeries"
    const val IID_IBarSeries = "ce37dcbe-df2e-5e0a-a978-de94bcfb19f8"
    const val IID_IBarSeriesFactory = "85a89fb5-a579-5f7a-94e1-f7d30ab47211"
    const val IBarSeries_get_Fill = 6                   // get_Fill(out Brush)
    const val IBarSeries_put_Fill = 7                   // put_Fill(Brush)
    const val IBarSeries_get_Orientation = 8            // get_Orientation(out BarOrientation)
    const val IBarSeries_put_Orientation = 9            // put_Orientation(BarOrientation)

    // ---- Microsoft.UI.Xaml.Controls.Charts.Axis ----
    // base: DependencyObject / composable factory: IAxisProtectedFactory (for derived classes only)
    const val IID_IAxis = "18282a43-9ee2-5fce-8729-bccba3be9817"
    const val IAxis_get_IsVisible = 6                   // get_IsVisible(out boolean)
    const val IAxis_put_IsVisible = 7                   // put_IsVisible(boolean)
    const val IAxis_get_Label = 8                       // get_Label(out HSTRING)
    const val IAxis_put_Label = 9                       // put_Label(HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.Charts.CartesianAxis ----
    // base: Axis / composable factory: ICartesianAxisProtectedFactory (for derived classes only)
    const val IID_ICartesianAxis = "7424bd83-0510-5efa-a0b1-6bb96fa1ec37"
    const val ICartesianAxis_get_ShowTickLabels = 6     // get_ShowTickLabels(out boolean)
    const val ICartesianAxis_put_ShowTickLabels = 7     // put_ShowTickLabels(boolean)
    const val ICartesianAxis_get_ShowTickMarks = 8      // get_ShowTickMarks(out boolean)
    const val ICartesianAxis_put_ShowTickMarks = 9      // put_ShowTickMarks(boolean)
    const val ICartesianAxis_get_GridLines = 10         // get_GridLines(out GridLines)
    const val ICartesianAxis_put_GridLines = 11         // put_GridLines(GridLines)
    const val ICartesianAxis_get_GridLineMajorBrush = 12 // get_GridLineMajorBrush(out Brush)
    const val ICartesianAxis_put_GridLineMajorBrush = 13 // put_GridLineMajorBrush(Brush)
    const val ICartesianAxis_get_GridLineMinorBrush = 14 // get_GridLineMinorBrush(out Brush)
    const val ICartesianAxis_put_GridLineMinorBrush = 15 // put_GridLineMinorBrush(Brush)
    const val ICartesianAxis_get_TickBrush = 16         // get_TickBrush(out Brush)
    const val ICartesianAxis_put_TickBrush = 17         // put_TickBrush(Brush)
    const val ICartesianAxis_get_TickLabelBrush = 18    // get_TickLabelBrush(out Brush)
    const val ICartesianAxis_put_TickLabelBrush = 19    // put_TickLabelBrush(Brush)
    const val ICartesianAxis_get_AxisLineBrush = 20     // get_AxisLineBrush(out Brush)
    const val ICartesianAxis_put_AxisLineBrush = 21     // put_AxisLineBrush(Brush)

    // ---- Microsoft.UI.Xaml.Controls.Charts.LinearAxis ----
    // base: CartesianAxis / activatable factory: <default IActivationFactory>
    const val CLS_LinearAxis = "$NS.LinearAxis"
    const val IID_ILinearAxis = "b2b6ef6d-a5e9-5191-b502-c8659ebbd1e7"
    const val ILinearAxis_get_Minimum = 6               // get_Minimum(out IReference<Double>)
    const val ILinearAxis_put_Minimum = 7               // put_Minimum(IReference<Double>)
    const val ILinearAxis_get_Maximum = 8               // get_Maximum(out IReference<Double>)
    const val ILinearAxis_put_Maximum = 9               // put_Maximum(IReference<Double>)
    const val ILinearAxis_get_Spacing = 10              // get_Spacing(out IReference<Double>)
    const val ILinearAxis_put_Spacing = 11              // put_Spacing(IReference<Double>)

    // ---- Microsoft.UI.Xaml.Controls.Charts.CategoryAxis ----
    // base: CartesianAxis / activatable factory: <default IActivationFactory>
    const val CLS_CategoryAxis = "$NS.CategoryAxis"
    const val IID_ICategoryAxis = "a6782ef2-07e1-552b-88ed-29e9875db64a"
    const val ICategoryAxis_get_SortKey = 6             // get_SortKey(out CategorySortKey)
    const val ICategoryAxis_put_SortKey = 7             // put_SortKey(CategorySortKey)
    const val ICategoryAxis_get_SortOrder = 8           // get_SortOrder(out SortOrder)
    const val ICategoryAxis_put_SortOrder = 9           // put_SortOrder(SortOrder)

    // ---- Microsoft.UI.Xaml.Controls.Charts.DateTimeAxis ----
    // base: CartesianAxis / activatable factory: <default IActivationFactory>
    const val CLS_DateTimeAxis = "$NS.DateTimeAxis"
    const val IID_IDateTimeAxis = "5cc30d2a-110f-527f-9ca3-14b79d61103a"
    const val IDateTimeAxis_get_Minimum = 6             // get_Minimum(out IReference<DateTime>)
    const val IDateTimeAxis_put_Minimum = 7             // put_Minimum(IReference<DateTime>)
    const val IDateTimeAxis_get_Maximum = 8             // get_Maximum(out IReference<DateTime>)
    const val IDateTimeAxis_put_Maximum = 9             // put_Maximum(IReference<DateTime>)
    const val IDateTimeAxis_get_IntervalType = 10       // get_IntervalType(out DateTimeIntervalType)
    const val IDateTimeAxis_put_IntervalType = 11       // put_IntervalType(DateTimeIntervalType)
    const val IDateTimeAxis_get_LabelFormat = 12        // get_LabelFormat(out HSTRING)
    const val IDateTimeAxis_put_LabelFormat = 13        // put_LabelFormat(HSTRING)

    // ---- Microsoft.UI.Xaml.Controls.Charts.DataLabelOverride ----
    // base: Object / activatable factory: IDataLabelOverrideFactory (read-only after creation)
    const val CLS_DataLabelOverride = "$NS.DataLabelOverride"
    const val IID_IDataLabelOverride = "59154e83-3bc6-5f9c-bfed-fe0d786daa82"
    const val IID_IDataLabelOverrideFactory = "5759e320-a994-5704-a02e-7a76fa349ac9"
    const val IDataLabelOverrideFactory_CreateInstance = 6 // CreateInstance(HSTRING text, Brush, out DataLabelOverride)
    const val IDataLabelOverride_get_Text = 6           // get_Text(out HSTRING)
    const val IDataLabelOverride_get_Brush = 7          // get_Brush(out Brush)

    // ---- Microsoft.UI.Xaml.Controls.Charts.DataMarkerOverride ----
    // base: Object / activatable factory: IDataMarkerOverrideFactory (read-only after creation)
    const val CLS_DataMarkerOverride = "$NS.DataMarkerOverride"
    const val IID_IDataMarkerOverride = "74d2fd53-0df9-5621-a617-7891b112079c"
    const val IID_IDataMarkerOverrideFactory = "4c837289-d445-507b-8eb2-a96ed887f392"
    const val IDataMarkerOverrideFactory_CreateInstance = 6 // CreateInstance(MarkerShape, Brush, out DataMarkerOverride)
    const val IDataMarkerOverride_get_Shape = 6         // get_Shape(out MarkerShape)
    const val IDataMarkerOverride_get_Brush = 7         // get_Brush(out Brush)

    // ---- Microsoft.UI.Xaml.Controls.Charts.XamlChartsResources (activatable: ResourceDictionary) ----
    // The default styles and theme resources of Chart (ChartsControlBackgroundBrush, etc.). Equivalent to
    // `<charts:XamlChartsResources />` in App.xaml; merged into Application.Resources.MergedDictionaries
    const val CLS_XamlChartsResources = "$NS.XamlChartsResources"

    // ---- Microsoft.UI.Xaml.XamlTypeInfo.XamlControlsChartsXamlMetaDataProvider ----
    // XAML type information for the Charts types. The app's IXamlMetadataProvider queries this for types that
    // XamlControlsXamlMetaDataProvider could not resolve (needed to resolve the TargetType of Styles in XamlChartsResources)
    const val CLS_XamlControlsChartsXamlMetaDataProvider =
        "Microsoft.UI.Xaml.XamlTypeInfo.XamlControlsChartsXamlMetaDataProvider"

    // ---- Concrete IIDs of generics (computed at run time) ----
    private val SAMPLES_SIGNATURE = "rc($NS.Samples;{$IID_ISamples})"
    private val AXIS_SIGNATURE = "rc($NS.Axis;{$IID_IAxis})"
    private val CARTESIAN_SERIES_SIGNATURE = "rc($NS.CartesianSeries;{$IID_ICartesianSeries})"

    /** IVector<Samples> (Chart.Data; QI'd from the IObservableVector returned by get_Data). */
    val IID_IVector_Samples: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IVector_OPEN}};$SAMPLES_SIGNATURE)")
    }

    /** IVector<Axis> (Chart.Axes). */
    val IID_IVector_Axis: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IVector_OPEN}};$AXIS_SIGNATURE)")
    }

    /** IVector<CartesianSeries> (Chart.Series). */
    val IID_IVector_CartesianSeries: String by lazy {
        Pinterface.iid("pinterface({${FoundationInterop.IID_IVector_OPEN}};$CARTESIAN_SERIES_SIGNATURE)")
    }

    /** IMap<UInt32, DataLabelOverride> (CartesianSeries.DataLabelOverrides; QI'd from the IObservableMap). */
    val IID_IMap_UInt32_DataLabelOverride: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_IMap_OPEN}};u4;rc($NS.DataLabelOverride;{$IID_IDataLabelOverride}))",
        )
    }

    /** IMap<UInt32, DataMarkerOverride> (CartesianSeries.DataMarkerOverrides). */
    val IID_IMap_UInt32_DataMarkerOverride: String by lazy {
        Pinterface.iid(
            "pinterface({${FoundationInterop.IID_IMap_OPEN}};u4;rc($NS.DataMarkerOverride;{$IID_IDataMarkerOverride}))",
        )
    }
}
