package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.winrt.Pinterface

/**
 * WinRT ABI constants (IIDs / vtable slot numbers) for TableView (the Microsoft.UI.Xaml.Controls.Tabular namespace).
 *
 * All values are mechanically extracted with tools/dump_winmd.py from metadata/Microsoft.UI.Xaml.winmd in
 * Microsoft.WindowsAppSDK.WinUI 2.3.10-experimental (a dependency of Microsoft.WindowsAppSDK 2.5.4-experimental).
 * Not a single value is handwritten or guessed.
 * The types are experimental (MUX_PREVIEW), and the implementation lives in a separate DLL
 * (Microsoft.UI.Xaml.Controls.Tabular.dll). Experimental APIs may change before the stable release,
 * so they are kept separate from XamlInterop.
 *
 * Slot-number convention: IUnknown = 0..2, IInspectable = 3..5, and the interface body
 * starts at 6 in the winmd's method-declaration order.
 */
internal object TabularInterop {
    private const val NS = "Microsoft.UI.Xaml.Controls.Tabular"

    // ---- Microsoft.UI.Xaml.Controls.Tabular.TableView ----
    // base: Control / composable factory: ITableViewFactory / statics: ITableViewStatics
    const val CLS_TableView = "$NS.TableView"
    const val IID_ITableView = "f1405dad-df41-5dfd-b370-c901fd8b207c"
    const val IID_ITableViewFactory = "6d4d66de-7e41-5a94-a251-95892f242523"
    const val ITableView_get_ItemsSource = 6                // get_ItemsSource(out object)
    const val ITableView_put_ItemsSource = 7                // put_ItemsSource(object)
    const val ITableView_get_Columns = 8                    // get_Columns(out IVector<TableViewColumn>)
    const val ITableView_get_HeadersVisibility = 9          // get_HeadersVisibility(out TableViewHeadersVisibility)
    const val ITableView_put_HeadersVisibility = 10         // put_HeadersVisibility(TableViewHeadersVisibility)
    const val ITableView_get_GridLinesVisibility = 11       // get_GridLinesVisibility(out TableViewGridLinesVisibility)
    const val ITableView_put_GridLinesVisibility = 12       // put_GridLinesVisibility(TableViewGridLinesVisibility)
    const val ITableView_get_CanUserResizeColumns = 13      // get_CanUserResizeColumns(out boolean)
    const val ITableView_put_CanUserResizeColumns = 14      // put_CanUserResizeColumns(boolean)
    const val ITableView_get_RowBackground = 15             // get_RowBackground(out Brush)
    const val ITableView_put_RowBackground = 16             // put_RowBackground(Brush)
    const val ITableView_get_AlternatingRowBackground = 17  // get_AlternatingRowBackground(out Brush)
    const val ITableView_put_AlternatingRowBackground = 18  // put_AlternatingRowBackground(Brush)
    const val ITableView_get_EmptyTemplate = 19             // get_EmptyTemplate(out DataTemplate)
    const val ITableView_put_EmptyTemplate = 20             // put_EmptyTemplate(DataTemplate)
    const val ITableView_get_GroupHeaderTemplate = 21       // get_GroupHeaderTemplate(out DataTemplate)
    const val ITableView_put_GroupHeaderTemplate = 22       // put_GroupHeaderTemplate(DataTemplate)
    const val ITableView_get_Density = 23                   // get_Density(out TableViewDensity)
    const val ITableView_put_Density = 24                   // put_Density(TableViewDensity)
    const val ITableView_get_IsReadOnly = 25                // get_IsReadOnly(out boolean)
    const val ITableView_put_IsReadOnly = 26                // put_IsReadOnly(boolean)
    const val ITableView_get_IsEditing = 27                 // get_IsEditing(out boolean)
    const val ITableView_CommitEdit = 28                    // CommitEdit(out boolean)
    const val ITableView_CancelEdit = 29                    // CancelEdit(out boolean)
    const val ITableView_add_BeginningEdit = 30             // add_BeginningEdit(TypedEventHandler<TableView, TableViewBeginningEditEventArgs>, out token)
    const val ITableView_remove_BeginningEdit = 31          // remove_BeginningEdit(token)
    const val ITableView_add_CellEditEnding = 32            // add_CellEditEnding(TypedEventHandler<TableView, TableViewCellEditEndingEventArgs>, out token)
    const val ITableView_remove_CellEditEnding = 33         // remove_CellEditEnding(token)
    const val ITableView_get_SelectionMode = 34             // get_SelectionMode(out TableViewSelectionMode)
    const val ITableView_put_SelectionMode = 35             // put_SelectionMode(TableViewSelectionMode)
    const val ITableView_get_SelectedItem = 36              // get_SelectedItem(out object)
    const val ITableView_get_SelectedIndex = 37             // get_SelectedIndex(out i4)
    const val ITableView_Select = 38                        // Select(i4)
    const val ITableView_Deselect = 39                      // Deselect(i4)
    const val ITableView_IsSelected = 40                    // IsSelected(i4, out boolean)
    const val ITableView_DeselectAll = 41                   // DeselectAll()
    const val ITableView_add_SelectionChanged = 42          // add_SelectionChanged(TypedEventHandler<TableView, SelectionChangedEventArgs>, out token)
    const val ITableView_remove_SelectionChanged = 43       // remove_SelectionChanged(token)
    const val ITableView_ExpandAllGroups = 44               // ExpandAllGroups()
    const val ITableView_CollapseAllGroups = 45             // CollapseAllGroups()
    const val ITableView_get_CanUserSortColumns = 46        // get_CanUserSortColumns(out boolean)
    const val ITableView_put_CanUserSortColumns = 47        // put_CanUserSortColumns(boolean)
    const val ITableView_SortByColumn = 48                  // SortByColumn(TableViewColumn, SortDirection, out boolean)
    const val ITableView_ToggleSortDirection = 49           // ToggleSortDirection(TableViewColumn, out boolean)
    const val ITableView_ClearSort = 50                     // ClearSort(out boolean)
    const val ITableView_add_Sorting = 51                   // add_Sorting(TypedEventHandler<TableView, TableViewSortingEventArgs>, out token)
    const val ITableView_remove_Sorting = 52                // remove_Sorting(token)
    const val ITableView_add_Sorted = 53                    // add_Sorted(TypedEventHandler<TableView, TableViewSortedEventArgs>, out token)
    const val ITableView_remove_Sorted = 54                 // remove_Sorted(token)

    // ---- Microsoft.UI.Xaml.Controls.Tabular.TableViewColumn ----
    // base: DependencyObject / composable factory: ITableViewColumnFactory (overridable: ITableViewColumnOverrides)
    const val CLS_TableViewColumn = "$NS.TableViewColumn"
    const val IID_ITableViewColumn = "12752b5d-4809-5d2c-8d37-b53e1e9c2a1f"
    const val IID_ITableViewColumnFactory = "d50f67a6-cc5b-5946-8394-c61f5725bf6d"
    const val ITableViewColumnFactory_CreateInstance = 6    // CreateInstance(outer, out inner, out TableViewColumn)
    const val ITableViewColumn_get_Header = 6               // get_Header(out object)
    const val ITableViewColumn_put_Header = 7               // put_Header(object)
    const val ITableViewColumn_put_HeaderTemplate = 9       // put_HeaderTemplate(DataTemplate)
    const val ITableViewColumn_get_HeaderToolTip = 12       // get_HeaderToolTip(out object)
    const val ITableViewColumn_put_HeaderToolTip = 13       // put_HeaderToolTip(object)
    const val ITableViewColumn_get_Width = 14               // get_Width(out GridLength)
    const val ITableViewColumn_put_Width = 15               // put_Width(GridLength)
    const val ITableViewColumn_get_MinWidth = 16            // get_MinWidth(out r8)
    const val ITableViewColumn_put_MinWidth = 17            // put_MinWidth(r8)
    const val ITableViewColumn_get_MaxWidth = 18            // get_MaxWidth(out r8)
    const val ITableViewColumn_put_MaxWidth = 19            // put_MaxWidth(r8)
    const val ITableViewColumn_get_CanResize = 20           // get_CanResize(out boolean)
    const val ITableViewColumn_put_CanResize = 21           // put_CanResize(boolean)
    const val ITableViewColumn_get_ActualWidth = 22         // get_ActualWidth(out r8)
    const val ITableViewColumn_get_FrozenEdge = 23          // get_FrozenEdge(out TableViewFrozenEdge)
    const val ITableViewColumn_put_FrozenEdge = 24          // put_FrozenEdge(TableViewFrozenEdge)
    const val ITableViewColumn_get_Visibility = 25          // get_Visibility(out Visibility)
    const val ITableViewColumn_put_Visibility = 26          // put_Visibility(Visibility)
    const val ITableViewColumn_get_CanSort = 27             // get_CanSort(out boolean)
    const val ITableViewColumn_put_CanSort = 28             // put_CanSort(boolean)
    const val ITableViewColumn_get_SortCycle = 29           // get_SortCycle(out TableViewSortCycle)
    const val ITableViewColumn_put_SortCycle = 30           // put_SortCycle(TableViewSortCycle)
    const val ITableViewColumn_get_SortMemberPath = 31      // get_SortMemberPath(out string)
    const val ITableViewColumn_put_SortMemberPath = 32      // put_SortMemberPath(string)
    const val ITableViewColumn_get_CustomSortComparer = 33  // get_CustomSortComparer(out ITableViewSortComparer)
    const val ITableViewColumn_put_CustomSortComparer = 34  // put_CustomSortComparer(ITableViewSortComparer)
    const val ITableViewColumn_get_SortDirection = 35       // get_SortDirection(out SortDirection)
    const val ITableViewColumn_GenerateElement = 36         // GenerateElement(object, out FrameworkElement)
    const val ITableViewColumn_get_IsReadOnly = 37          // get_IsReadOnly(out boolean)
    const val ITableViewColumn_put_IsReadOnly = 38          // put_IsReadOnly(boolean)
    const val ITableViewColumn_get_CellEditingTemplate = 39 // get_CellEditingTemplate(out DataTemplate)
    const val ITableViewColumn_put_CellEditingTemplate = 40 // put_CellEditingTemplate(DataTemplate)
    const val ITableViewColumn_get_CellToolTipBinding = 41  // get_CellToolTipBinding(out Binding)
    const val ITableViewColumn_put_CellToolTipBinding = 42  // put_CellToolTipBinding(Binding)

    // Overridable methods of TableViewColumn (implemented by the outer object of the COM aggregation)
    const val IID_ITableViewColumnOverrides = "71122319-f672-5855-be49-ec049e1c29b8"
    const val ITableViewColumnOverrides_GetSortMemberPathCore = 6 // GetSortMemberPathCore(out string)
    const val ITableViewColumnOverrides_GenerateElementCore = 7   // GenerateElementCore(object, out FrameworkElement)

    // ---- Microsoft.UI.Xaml.Controls.Tabular.TableViewTextColumn ----
    // base: TableViewColumn / composable factory: ITableViewTextColumnFactory
    const val CLS_TableViewTextColumn = "$NS.TableViewTextColumn"
    const val IID_ITableViewTextColumn = "b1efd0de-25c2-558e-ad05-6ecb7bdce6d7"
    const val IID_ITableViewTextColumnFactory = "fc3bd851-92bf-5dbc-9d39-a1689bafa739"
    const val ITableViewTextColumn_get_Binding = 6          // get_Binding(out Binding)
    const val ITableViewTextColumn_put_Binding = 7          // put_Binding(Binding)

    // ---- Microsoft.UI.Xaml.Controls.Tabular.TableViewTemplateColumn ----
    // base: TableViewColumn / composable factory: ITableViewTemplateColumnFactory
    const val CLS_TableViewTemplateColumn = "$NS.TableViewTemplateColumn"
    const val IID_ITableViewTemplateColumn = "6c0d112f-b610-5999-9127-a04877652a70"
    const val IID_ITableViewTemplateColumnFactory = "8b51bfe0-0061-5ccb-a4a6-efc32b01a83f"
    const val ITableViewTemplateColumn_get_CellTemplate = 6 // get_CellTemplate(out DataTemplate)
    const val ITableViewTemplateColumn_put_CellTemplate = 7 // put_CellTemplate(DataTemplate)

    // ---- Microsoft.UI.Xaml.Controls.Tabular.ITableViewSortComparer (implemented by the app) ----
    const val IID_ITableViewSortComparer = "0c05d316-a556-5614-898a-27cb1bee9fe9"
    // vtbl[6] Compare(object, object, out i4)

    // ---- Microsoft.UI.Xaml.Controls.Tabular.TableViewSource ----
    // base: Object / statics: ITableViewSourceStatics (there is no constructor; instances are created with From)
    const val CLS_TableViewSource = "$NS.TableViewSource"
    const val IID_ITableViewSource = "b35bc509-3edb-596b-b62d-113b6852e71b"
    const val IID_ITableViewSourceStatics = "1b1343e5-70ba-51f1-8ca7-4c301f864a26"
    const val ITableViewSourceStatics_From = 6              // From(object, out TableViewSource)
    const val ITableViewSource_Filter = 6                   // Filter(TableViewPredicate, out TableViewSource)
    const val ITableViewSource_GroupBy = 7                  // GroupBy(TableViewKeySelector, out TableViewSource)
    const val ITableViewSource_GroupByWithIdentity = 8      // GroupBy(TableViewKeySelector, TableViewIdentitySelector, out TableViewSource)
    const val ITableViewSource_SortByPath = 9               // Sort(string, SortDirection, out TableViewSource)
    const val ITableViewSource_Sort = 10                    // Sort(TableViewKeySelector, SortDirection, out TableViewSource)
    const val ITableViewSource_ClearFilter = 11             // ClearFilter(out TableViewSource)
    const val ITableViewSource_ClearGroupBy = 12            // ClearGroupBy(out TableViewSource)
    const val ITableViewSource_ClearSort = 13               // ClearSort(out TableViewSource)

    // Delegates of TableViewSource (all have Invoke at vtbl[3]; signatures are from TableViewSource.idl)
    const val IID_TableViewPredicate = "1ce759c7-1e3e-5e96-94d0-8b789a9c1362"         // Boolean Invoke(Object item)
    const val IID_TableViewKeySelector = "d73df11b-e70a-5609-bae8-54da15ee4d13"       // Object Invoke(Object item)
    const val IID_TableViewIdentitySelector = "c5c4cfdd-6f22-5817-8b59-b42b7787f8a1"  // String Invoke(Object item)

    // ---- Event args ----
    const val IID_ITableViewBeginningEditEventArgs = "ea28f48c-2e0f-5e5f-8bb8-3bf83724145c"
    const val ITableViewBeginningEditEventArgs_get_Item = 6     // get_Item(out object)
    const val ITableViewBeginningEditEventArgs_get_Column = 7   // get_Column(out TableViewColumn)
    const val ITableViewBeginningEditEventArgs_get_Cancel = 8   // get_Cancel(out boolean)
    const val ITableViewBeginningEditEventArgs_put_Cancel = 9   // put_Cancel(boolean)

    const val IID_ITableViewCellEditEndingEventArgs = "29ea524c-0124-5e94-a04e-aa21c5c9abf6"
    const val ITableViewCellEditEndingEventArgs_get_Item = 6       // get_Item(out object)
    const val ITableViewCellEditEndingEventArgs_get_Column = 7     // get_Column(out TableViewColumn)
    const val ITableViewCellEditEndingEventArgs_get_EditAction = 8 // get_EditAction(out TableViewEditAction)
    const val ITableViewCellEditEndingEventArgs_get_Cancel = 9     // get_Cancel(out boolean)
    const val ITableViewCellEditEndingEventArgs_put_Cancel = 10    // put_Cancel(boolean)

    const val IID_ITableViewSortingEventArgs = "9565902a-6c4f-52e0-a67b-fdfcc28fe9ba"
    const val ITableViewSortingEventArgs_get_Column = 6     // get_Column(out TableViewColumn)
    const val ITableViewSortingEventArgs_get_Direction = 7  // get_Direction(out SortDirection)
    const val ITableViewSortingEventArgs_get_Cancel = 8     // get_Cancel(out boolean)
    const val ITableViewSortingEventArgs_put_Cancel = 9     // put_Cancel(boolean)

    const val IID_ITableViewSortedEventArgs = "9e9172bb-ed57-51ed-b196-36189dd560c7"
    const val ITableViewSortedEventArgs_get_Column = 6      // get_Column(out TableViewColumn)
    const val ITableViewSortedEventArgs_get_Direction = 7   // get_Direction(out SortDirection)

    // ---- Microsoft.UI.Xaml.Controls.Tabular.TableViewGroupInfo (the DataContext of a group header) ----
    // Referenced from GroupHeaderTemplate via {Binding Key / ItemCount / Level / IsExpandable / IsExpanded / KeyText / ItemCountText}
    const val IID_ITableViewGroupInfo = "8c0fd48a-70bb-59e6-b442-6ee38d58fe70"

    // ---- Microsoft.UI.Xaml.Controls.Tabular.TabularControlsResources (activatable: ResourceDictionary) ----
    const val CLS_TabularControlsResources = "$NS.TabularControlsResources"

    // ---- Microsoft.UI.Xaml.XamlTypeInfo.XamlControlsTabularXamlMetaDataProvider ----
    // XAML type information for the Tabular types. The app's IXamlMetadataProvider queries this for types that
    // XamlControlsXamlMetaDataProvider could not resolve (needed to resolve a Style's TargetType and {Binding KeyText})
    const val CLS_XamlControlsTabularXamlMetaDataProvider =
        "Microsoft.UI.Xaml.XamlTypeInfo.XamlControlsTabularXamlMetaDataProvider"

    // ---- enums (values extracted from the winmd) ----
    // SortDirection: None = 0, Ascending = 1, Descending = 2
    const val SortDirection_None = 0
    const val SortDirection_Ascending = 1
    const val SortDirection_Descending = 2

    // TableViewEditAction: Commit = 0, Cancel = 1
    const val TableViewEditAction_Commit = 0

    // TableViewHeadersVisibility: None = 0, Column = 1
    const val TableViewHeadersVisibility_None = 0
    const val TableViewHeadersVisibility_Column = 1

    // TableViewFrozenEdge: None = 0, Leading = 1, Trailing = 2
    const val TableViewFrozenEdge_None = 0
    const val TableViewFrozenEdge_Leading = 1

    // ---- Concrete IIDs of TypedEventHandler<TableView, TArgs> (computed at runtime) ----
    private val TABLE_VIEW_SIGNATURE = "rc($NS.TableView;{$IID_ITableView})"

    private fun typedHandlerIid(argsSignature: String): String =
        Pinterface.iid("pinterface({${FoundationInterop.IID_TypedEventHandler_OPEN}};$TABLE_VIEW_SIGNATURE;$argsSignature)")

    /** TypedEventHandler<TableView, TableViewBeginningEditEventArgs> (BeginningEdit). */
    val IID_BeginningEditHandler: String by lazy {
        typedHandlerIid("rc($NS.TableViewBeginningEditEventArgs;{$IID_ITableViewBeginningEditEventArgs})")
    }

    /** TypedEventHandler<TableView, TableViewCellEditEndingEventArgs> (CellEditEnding). */
    val IID_CellEditEndingHandler: String by lazy {
        typedHandlerIid("rc($NS.TableViewCellEditEndingEventArgs;{$IID_ITableViewCellEditEndingEventArgs})")
    }

    /** TypedEventHandler<TableView, SelectionChangedEventArgs> (SelectionChanged; the args are the platform-wide type). */
    val IID_SelectionChangedHandler: String by lazy {
        typedHandlerIid(
            "rc(Microsoft.UI.Xaml.Controls.SelectionChangedEventArgs;{${XamlInterop.IID_ISelectionChangedEventArgs}})",
        )
    }

    /** TypedEventHandler<TableView, TableViewSortingEventArgs> (Sorting). */
    val IID_SortingHandler: String by lazy {
        typedHandlerIid("rc($NS.TableViewSortingEventArgs;{$IID_ITableViewSortingEventArgs})")
    }

    /** TypedEventHandler<TableView, TableViewSortedEventArgs> (Sorted). */
    val IID_SortedHandler: String by lazy {
        typedHandlerIid("rc($NS.TableViewSortedEventArgs;{$IID_ITableViewSortedEventArgs})")
    }
}
