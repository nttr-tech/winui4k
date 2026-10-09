package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Dispatcher
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.TabularInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import com.appkitbox.winui4k.table.DefaultTableColumnModel
import com.appkitbox.winui4k.table.DefaultTableModel
import com.appkitbox.winui4k.table.RowSorter
import com.appkitbox.winui4k.table.RowSorterHost
import com.appkitbox.winui4k.table.SortOrder
import com.appkitbox.winui4k.table.TableCellEditor
import com.appkitbox.winui4k.table.TableCellRenderer
import com.appkitbox.winui4k.table.TableColumn
import com.appkitbox.winui4k.table.TableColumnModel
import com.appkitbox.winui4k.table.TableColumnModelEvent
import com.appkitbox.winui4k.table.TableColumnModelListener
import com.appkitbox.winui4k.table.TableModel
import com.appkitbox.winui4k.table.TableModelEvent
import com.appkitbox.winui4k.table.TableModelListener
import com.appkitbox.winui4k.table.TableRowSorter
import com.appkitbox.winui4k.table.escape
import java.util.function.IntConsumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * JTable-like: WinUI 3's TableView (Microsoft.UI.Xaml.Controls.Tabular.TableView; Windows App SDK 2.5 experimental).
 *
 * It follows the same MVC structure as Swing's JTable: data is handled by [TableModel] (the com.appkitbox.winui4k.table
 * package), column order and display attributes by [TableColumnModel] / [TableColumn], and sorting and filtering by
 * [TableRowSorter]. Rows are displayed virtualized (only the cells of visible rows are created), and cell values are
 * read from the model when needed.
 *
 * Mapping to TableView features:
 * - Cell display: strings (TableViewTextColumn), XAML templates (TableViewTemplateColumn),
 *   Kotlin / Java renderers (an implementation of TableViewColumn.GenerateElementCore) — see [TableColumn]
 * - Editing: starts with double-click / F2, Enter commits, Esc cancels. Committed values are written back to [TableModel.setValueAt].
 *   [isEditing] / [stopCellEditing] / [cancelCellEditing] / [addBeginningEditListener] / [addCellEditEndingListener]
 * - Selection: single selection ([selectionMode] / [selectedRow] / [selectRow] / [clearSelection] / [addRowSelectionListener])
 * - Row double-click: [addRowInvokedListener]
 * - Sorting: clicking column headers (enabled when [rowSorter] is set) / [TableRowSorter.setSortKeys] /
 *   [addSortingListener] (cancelable) / [addSortedListener]
 * - Filtering: [TableRowSorter.setRowFilter] (TableViewSource.Filter)
 * - Grouping: [groupBy] / [clearGrouping] / [expandAllGroups] / [collapseAllGroups] / [groupHeaderTemplate]
 * - Appearance: [showHorizontalLines] / [showVerticalLines] / [isHeaderVisible] / [density] / [rowBackground] /
 *   [alternatingRowBackground] / [emptyText] / [emptyTemplate] / [canUserResizeColumns]
 *
 * Row and column indices are view positions (rows after sorting and filtering, columns in display order), except for
 * those whose names contain model. Conversion to and from the model is done with [convertRowIndexToModel] /
 * [convertColumnIndexToModel] and so on (as in Swing).
 */
class WTableView @JvmOverloads constructor(
    model: TableModel = DefaultTableModel(),
    columnModel: TableColumnModel? = null,
) : WControl(
    Activation.composeDefault(TabularInterop.CLS_TableView, TabularInterop.IID_ITableViewFactory), // Default interface = ITableView
) {
    /** A table of row data [rowData] and column names [columnNames] (equivalent to JTable(Object[][], Object[])). */
    constructor(rowData: List<List<Any?>>, columnNames: List<Any?>) : this(DefaultTableModel(rowData, columnNames))

    /** A table of [rowCount] rows × [columnCount] columns with all values null (equivalent to JTable(int, int)). */
    constructor(rowCount: Int, columnCount: Int) : this(DefaultTableModel(rowCount, columnCount))

    /** ITableView (the default interface). */
    private val tableView: ComPtr = inspectable

    /** TableView.Columns (IVector<TableViewColumn>). */
    private val nativeColumns: ComPtr by lazy { own(tableView.getPtr(TabularInterop.ITableView_get_Columns)) }

    /** The access point for reading and writing model values (used by row objects). */
    private val rowValues = object : TableRowValues {
        override fun columnCount(): Int = this@WTableView.model.getColumnCount()

        override fun valueAt(row: Int, column: Int): Any? = this@WTableView.model.getValueAt(row, column)

        override fun valueWritten(item: TableRowItem, column: Int, value: Any?) {
            onValueWritten(item, column, value)
        }
    }

    /** The row collection behind ItemsSource (in model row order). */
    private var rows = TableRowCollection(rowValues)

    /** The TableViewSource set as ItemsSource (projects the row collection sorted, filtered and grouped). */
    private var source: ComPtr? = null

    /** The visible columns (matching the order of columnModel). */
    private val peers = mutableListOf<TableViewColumnPeer>()

    private val modelListener = TableModelListener { event -> onTableChanged(event) }

    private val columnModelListener = object : TableColumnModelListener {
        override fun columnAdded(event: TableColumnModelEvent) {
            val peer = TableViewColumnPeer(this@WTableView.columnModel.getColumn(event.toIndex), columnHost)
            peers.add(event.toIndex, peer)
            nativeColumns.call(FoundationInterop.IVector_InsertAt, event.toIndex, peer.native.ptr)
        }

        override fun columnRemoved(event: TableColumnModelEvent) {
            val peer = peers.removeAt(event.fromIndex)
            nativeColumns.call(FoundationInterop.IVector_RemoveAt, event.fromIndex)
            peer.dispose()
        }

        override fun columnMoved(event: TableColumnModelEvent) {
            if (event.fromIndex == event.toIndex) return
            val peer = peers.removeAt(event.fromIndex)
            peers.add(event.toIndex, peer)
            nativeColumns.call(FoundationInterop.IVector_RemoveAt, event.fromIndex)
            nativeColumns.call(FoundationInterop.IVector_InsertAt, event.toIndex, peer.native.ptr)
        }
    }

    private val defaultRenderers = LinkedHashMap<Class<*>, TableCellRenderer>()
    private val defaultEditors = LinkedHashMap<Class<*>, TableCellEditor>()

    /** The cell being written back to the model on edit commit (change notifications for that cell are applied later). */
    private var writingCell: Pair<Int, Int>? = null

    /** The cell being edited (recorded in BeginningEdit, cleared in CellEditEnding). */
    private var editingItem: TableRowItem? = null
    private var editingPeer: TableViewColumnPeer? = null

    private val beginningEditListeners = mutableListOf<TableCellEditListener>()
    private val cellEditEndingListeners = mutableListOf<TableCellEditListener>()
    private val sortingListeners = mutableListOf<TableSortListener>()
    private val sortedListeners = mutableListOf<TableSortListener>()
    private val selectionTokens = ListenerTokens<Runnable>()

    /** Notifications of row double-clicks (addRowInvokedListener). */
    internal val rowInvoker = TableViewRowInvoker(uiElement, { itemOf(it) }, { convertRowIndexToView(it) })

    /** The grouping key function (no grouping if null). */
    private var groupKey: TableGroupKey? = null

    /** Reads the row order in the view (the result of sorting, filtering and grouping). */
    private val rowOrder: TableViewRowOrder = TableViewRowOrder(
        root = { dependencyObject },
        rows = { rows },
        model = { this.model },
        sorter = { sorter },
        sortKeys = { sorterHost.currentSortKeys() },
    )

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------

    /**
     * Whether columns are created automatically from the model (JTable.autoCreateColumnsFromModel). If true, a
     * [TableColumn] is recreated for every model column when the model is set and when the column structure changes
     * (structure changed). As in Swing, it is false when a column model is passed to the constructor (the passed columns
     * are used as is).
     */
    var autoCreateColumnsFromModel: Boolean = columnModel == null

    /** The model of the data to display. Changing it recreates the rows and (if autoCreateColumnsFromModel) the columns. */
    var model: TableModel = model
        set(value) {
            val old = field
            if (old === value) return
            old.removeTableModelListener(modelListener)
            field = value
            attachModel()
            if (autoCreateRowSorter) rowSorter = TableRowSorter(value)
        }

    /** The model of column order and display attributes. */
    var columnModel: TableColumnModel = columnModel ?: DefaultTableColumnModel()
        set(value) {
            if (field === value) return
            field.removeColumnModelListener(columnModelListener)
            removeAllPeers()
            field = value
            attachColumnModel()
        }

    /**
     * The [RowSorter] responsible for sorting and filtering (JTable.rowSorter). If null, no sorting (clicking column
     * headers is also disabled). WTableView accepts only [TableRowSorter].
     */
    var rowSorter: RowSorter<out TableModel>? = null
        set(value) {
            require(value == null || value is TableRowSorter<*>) { "WTableView supports TableRowSorter only" }
            (field as? TableRowSorter<*>)?.host = null
            field = value
            val sorter = value as? TableRowSorter<*>
            sorter?.host = sorterHost
            tableView.putBool(TabularInterop.ITableView_put_CanUserSortColumns, sorter != null)
            for (peer in peers) peer.applySortProperties()
            applyRowFilter()
            if (sorter == null) {
                callBool(TabularInterop.ITableView_ClearSort)
            } else {
                val pending = sorter.pendingSortKeys()
                if (pending.isNotEmpty()) sorterHost.sortKeysChanged(pending)
            }
        }

    /**
     * If true, a [TableRowSorter] is created and set as [rowSorter] every time a model is set
     * (JTable.autoCreateRowSorter). An easy way to enable sorting by clicking column headers.
     */
    var autoCreateRowSorter: Boolean = false
        set(value) {
            field = value
            if (value) rowSorter = TableRowSorter(model)
        }

    private val sorter: TableRowSorter<*>?
        get() = rowSorter as? TableRowSorter<*>

    /** Recreates a [TableColumn] for every model column (JTable.createDefaultColumnsFromModel). */
    fun createDefaultColumnsFromModel() {
        val columns = columnModel
        for (column in columns.getColumns().reversed()) columns.removeColumn(column)
        for (index in 0 until model.getColumnCount()) columns.addColumn(TableColumn(index))
    }

    /** Appends a column (adds it to columnModel). */
    fun addColumn(column: TableColumn) {
        columnModel.addColumn(column)
    }

    /** Removes a column. */
    fun removeColumn(column: TableColumn) {
        columnModel.removeColumn(column)
    }

    /** Moves the [column]-th column to position [targetColumn]. */
    fun moveColumn(column: Int, targetColumn: Int) {
        columnModel.moveColumn(column, targetColumn)
    }

    /** The column for [identifier] (looked up by [TableColumn.identifier]). */
    fun getColumn(identifier: Any?): TableColumn = columnModel.getColumn(columnModel.getColumnIndex(identifier))

    /** The number of visible columns. */
    val columnCount: Int
        get() = columnModel.getColumnCount()

    /** The name of column [column] (view) (the model's column name). */
    fun getColumnName(column: Int): String = model.getColumnName(convertColumnIndexToModel(column))

    /** The value type of column [column] (view). */
    fun getColumnClass(column: Int): Class<*> = model.getColumnClass(convertColumnIndexToModel(column))

    /** Converts a view column index to a model column index. -1 if out of range. */
    fun convertColumnIndexToModel(viewColumnIndex: Int): Int =
        if (viewColumnIndex in 0 until columnModel.getColumnCount()) columnModel.getColumn(viewColumnIndex).modelIndex else -1

    /** Converts a model column index to a view column index. -1 if it is not shown. */
    fun convertColumnIndexToView(modelColumnIndex: Int): Int =
        columnModel.getColumns().indexOfFirst { it.modelIndex == modelColumnIndex }

    /** The number of rows in the view (after filtering; includes group header rows while grouped). */
    val rowCount: Int
        get() = rowOrder.count()

    /** The value of a cell (at a view position). */
    fun getValueAt(row: Int, column: Int): Any? =
        model.getValueAt(convertRowIndexToModel(row), convertColumnIndexToModel(column))

    /** Sets the value of a cell (at a view position), delegating to the model. */
    fun setValueAt(value: Any?, row: Int, column: Int) {
        model.setValueAt(value, convertRowIndexToModel(row), convertColumnIndexToModel(column))
    }

    /** Whether a cell (at a view position) is editable (when both the model and the column allow it). */
    fun isCellEditable(row: Int, column: Int): Boolean {
        val modelRow = convertRowIndexToModel(row)
        val viewColumn = column.takeIf { it in 0 until columnModel.getColumnCount() } ?: return false
        return modelRow >= 0 && columnModel.getColumn(viewColumn).isEditable &&
            model.isCellEditable(modelRow, convertColumnIndexToModel(viewColumn))
    }

    /** Converts a view row index to a model row index. -1 for group header rows or out of range. */
    fun convertRowIndexToModel(viewRowIndex: Int): Int = rowOrder.toModel(viewRowIndex)

    /** Converts a model row index to a view row index. -1 if it is filtered out. */
    fun convertRowIndexToView(modelRowIndex: Int): Int = rowOrder.toView(modelRowIndex)

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    /** The row selection mode (TableView.SelectionMode). Setting NONE also clears the selection. */
    var selectionMode: TableSelectionMode
        get() = TableSelectionMode.of(tableView.getInt(TabularInterop.ITableView_get_SelectionMode))
        set(value) = tableView.call(TabularInterop.ITableView_put_SelectionMode, value.native)

    /** The selected row (view index; TableView.SelectedIndex). -1 if nothing is selected. */
    val selectedRow: Int
        get() = tableView.getInt(TabularInterop.ITableView_get_SelectedIndex)

    /** An array of the selected row indices (0 or 1 elements because of single selection; equivalent to JTable.getSelectedRows). */
    val selectedRows: IntArray
        get() = selectedRow.let { if (it < 0) IntArray(0) else intArrayOf(it) }

    /** The number of selected rows. */
    val selectedRowCount: Int
        get() = if (selectedRow < 0) 0 else 1

    /** Selects row [row] (view) (TableView.Select). A negative value clears the selection. */
    fun selectRow(row: Int) {
        tableView.call(TabularInterop.ITableView_Select, row)
    }

    /** Deselects row [row] (view) if it is selected (TableView.Deselect). */
    fun deselectRow(row: Int) {
        tableView.call(TabularInterop.ITableView_Deselect, row)
    }

    /** Whether row [row] (view) is selected (TableView.IsSelected). */
    fun isRowSelected(row: Int): Boolean = callBool(TabularInterop.ITableView_IsSelected, row)

    /** Clears the selection (TableView.DeselectAll). */
    fun clearSelection() {
        tableView.call(TabularInterop.ITableView_DeselectAll)
    }

    /** Subscribes to row selection changes (TableView.SelectionChanged). */
    @JvmSynthetic
    fun addRowSelectionListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addRowSelectionListenerForJava(adapter)
        selectionTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addRowSelectionListener")
    fun addRowSelectionListenerForJava(listener: Runnable) {
        val token = tableView.addEventHandler(
            "WinUI4K.TableViewSelectionChangedHandler",
            TabularInterop.IID_SelectionChangedHandler,
            TabularInterop.ITableView_add_SelectionChanged,
        ) { _, _ -> listener.run() }
        selectionTokens.add(listener, token)
    }

    /** Removes a listener registered with [addRowSelectionListener]. */
    @JvmSynthetic
    fun removeRowSelectionListener(listener: () -> Unit) {
        val adapter = selectionTokens.removeKotlinAdapter(listener) ?: return
        removeRowSelectionListenerForJava(adapter)
    }

    @JvmName("removeRowSelectionListener")
    fun removeRowSelectionListenerForJava(listener: Runnable) {
        val token = selectionTokens.remove(listener) ?: return
        tableView.removeEventHandler(TabularInterop.ITableView_remove_SelectionChanged, token)
    }

    /**
     * Subscribes to row double-clicks (double-taps) (UIElement.DoubleTapped).
     * The listener receives the view index of the double-clicked row
     * (not called for double-clicks on column headers, group header rows or areas without rows).
     * Use this for actions that commit a row, such as double-click-to-open in the Filer sample.
     * In editable cells a double-click also starts editing, so set [isReadOnly] to true for this use.
     */
    @JvmSynthetic
    fun addRowInvokedListener(listener: (Int) -> Unit) {
        rowInvoker.add(listener)
    }

    @JvmName("addRowInvokedListener")
    fun addRowInvokedListenerForJava(listener: IntConsumer) {
        rowInvoker.add(listener)
    }

    /** Unsubscribes a listener registered via [addRowInvokedListener]. */
    @JvmSynthetic
    fun removeRowInvokedListener(listener: (Int) -> Unit) {
        rowInvoker.remove(listener)
    }

    @JvmName("removeRowInvokedListener")
    fun removeRowInvokedListenerForJava(listener: IntConsumer) {
        rowInvoker.remove(listener)
    }

    // ------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------

    /**
     * Whether the whole table is read-only (TableView.IsReadOnly). While true, editing does not start even if the model
     * or column allows it (setting it to true during editing closes that edit). Defaults to false (as in Swing, it is up
     * to the model).
     */
    var isReadOnly: Boolean
        get() = tableView.getBool(TabularInterop.ITableView_get_IsReadOnly)
        set(value) = tableView.putBool(TabularInterop.ITableView_put_IsReadOnly, value)

    /** Whether a cell is being edited (TableView.IsEditing). */
    val isEditing: Boolean
        get() = tableView.getBool(TabularInterop.ITableView_get_IsEditing)

    /** The row (view) of the cell being edited. -1 if not editing. */
    val editingRow: Int
        get() = if (isEditing) editingItem?.let { convertRowIndexToView(it.modelRow) } ?: -1 else -1

    /** The column (view) of the cell being edited. -1 if not editing. */
    val editingColumn: Int
        get() = if (isEditing) editingPeer?.let { peers.indexOf(it) } ?: -1 else -1

    /**
     * Commits the cell being edited (TableView.CommitEdit). Returns false if not editing, if a listener canceled it, or
     * if the value could not be written back.
     */
    fun stopCellEditing(): Boolean = callBool(TabularInterop.ITableView_CommitEdit)

    /** Cancels the cell being edited (TableView.CancelEdit). Returns false if not editing or if a listener canceled it. */
    fun cancelCellEditing(): Boolean = callBool(TabularInterop.ITableView_CancelEdit)

    /**
     * Subscribes to the start of cell editing (TableView.BeginningEdit). Setting [TableCellEditEvent.isCanceled] to true
     * prevents it from starting. Not called for cells the model ([TableModel.isCellEditable]) or the column
     * ([TableColumn.isEditable]) does not allow to be edited.
     */
    fun addBeginningEditListener(listener: TableCellEditListener) {
        beginningEditListeners += listener
    }

    /** Removes a listener registered with [addBeginningEditListener]. */
    fun removeBeginningEditListener(listener: TableCellEditListener) {
        beginningEditListeners -= listener
    }

    /**
     * Subscribes to the end of cell editing (just before commit or cancel; TableView.CellEditEnding).
     * Setting [TableCellEditEvent.isCanceled] to true keeps the edit going.
     */
    fun addCellEditEndingListener(listener: TableCellEditListener) {
        cellEditEndingListeners += listener
    }

    /** Removes a listener registered with [addCellEditEndingListener]. */
    fun removeCellEditEndingListener(listener: TableCellEditListener) {
        cellEditEndingListeners -= listener
    }

    /**
     * The default renderer for columns whose value type is [columnClass] (JTable.setDefaultRenderer).
     * Used when the column has no [TableColumn.cellRenderer] / [TableColumn.cellTemplate]. null to remove it.
     */
    fun setDefaultRenderer(columnClass: Class<*>, renderer: TableCellRenderer?) {
        if (renderer == null) defaultRenderers.remove(columnClass) else defaultRenderers[columnClass] = renderer
        rebuildAllPeers()
    }

    /** The default renderer for [columnClass] (superclasses are also searched). null if none. */
    fun getDefaultRenderer(columnClass: Class<*>): TableCellRenderer? = lookupByClass(defaultRenderers, columnClass)

    /** The default editor for columns whose value type is [columnClass] (JTable.setDefaultEditor). null to remove it. */
    fun setDefaultEditor(columnClass: Class<*>, editor: TableCellEditor?) {
        if (editor == null) defaultEditors.remove(columnClass) else defaultEditors[columnClass] = editor
        rebuildAllPeers()
    }

    /** The default editor for [columnClass] (superclasses are also searched). null if none. */
    fun getDefaultEditor(columnClass: Class<*>): TableCellEditor? = lookupByClass(defaultEditors, columnClass)

    // ------------------------------------------------------------------
    // Sorting
    // ------------------------------------------------------------------

    /**
     * Registers a listener called before a column is sorted (TableView.Sorting).
     * Setting [TableSortEvent.isCanceled] to true cancels the sort.
     */
    fun addSortingListener(listener: TableSortListener) {
        sortingListeners += listener
    }

    /** Removes a listener registered with [addSortingListener]. */
    fun removeSortingListener(listener: TableSortListener) {
        sortingListeners -= listener
    }

    /** Registers a listener called after a column is sorted (TableView.Sorted). */
    fun addSortedListener(listener: TableSortListener) {
        sortedListeners += listener
    }

    /** Removes a listener registered with [addSortedListener]. */
    fun removeSortedListener(listener: TableSortListener) {
        sortedListeners -= listener
    }

    // ------------------------------------------------------------------
    // Grouping
    // ------------------------------------------------------------------

    /** Whether rows are grouped. */
    val isGrouped: Boolean
        get() = groupKey != null

    /** Groups rows that have the same value in column [modelColumn] (model) (TableViewSource.GroupBy). */
    fun groupBy(modelColumn: Int) {
        groupBy { model, row -> model.getValueAt(row, modelColumn) }
    }

    /** Groups rows for which [key] returns the same key (TableViewSource.GroupBy). */
    fun groupBy(key: TableGroupKey) {
        groupKey = key
        applyGrouping()
    }

    /** Clears the grouping (TableViewSource.ClearGroupBy). */
    fun clearGrouping() {
        groupKey = null
        applyGrouping()
    }

    /** Expands all groups (TableView.ExpandAllGroups). */
    fun expandAllGroups() {
        tableView.call(TabularInterop.ITableView_ExpandAllGroups)
    }

    /** Collapses all groups (TableView.CollapseAllGroups). */
    fun collapseAllGroups() {
        tableView.call(TabularInterop.ITableView_CollapseAllGroups)
    }

    /**
     * The content of the DataTemplate for group header rows (XAML; TableView.GroupHeaderTemplate).
     * The DataContext is a TableViewGroupInfo, so `{Binding KeyText}` (the key string) / `{Binding ItemCount}` (the row
     * count) / `{Binding ItemCountText}` / `{Binding Key}` / `{Binding Level}` / `{Binding IsExpanded}` /
     * `{Binding IsExpandable}` can be used. The default namespace (xmlns) is added automatically. The default header if null.
     */
    var groupHeaderTemplate: String? = null
        set(value) {
            field = value
            putTemplate(TabularInterop.ITableView_put_GroupHeaderTemplate, value)
        }

    // ------------------------------------------------------------------
    // Appearance
    // ------------------------------------------------------------------

    /** Whether to show horizontal grid lines (between rows) (TableView.GridLinesVisibility). */
    var showHorizontalLines: Boolean
        get() = gridLines() in setOf(GRID_LINES_ALL, GRID_LINES_HORIZONTAL)
        set(value) = putGridLines(value, showVerticalLines)

    /** Whether to show vertical grid lines (between columns) (TableView.GridLinesVisibility). */
    var showVerticalLines: Boolean
        get() = gridLines() in setOf(GRID_LINES_ALL, GRID_LINES_VERTICAL)
        set(value) = putGridLines(showHorizontalLines, value)

    /** Shows or hides both horizontal and vertical grid lines (JTable.setShowGrid). */
    fun setShowGrid(showGrid: Boolean) {
        putGridLines(showGrid, showGrid)
    }

    /** Whether to show the column headers (TableView.HeadersVisibility). */
    var isHeaderVisible: Boolean
        get() = tableView.getInt(TabularInterop.ITableView_get_HeadersVisibility) == TabularInterop.TableViewHeadersVisibility_Column
        set(value) = tableView.call(
            TabularInterop.ITableView_put_HeadersVisibility,
            if (value) TabularInterop.TableViewHeadersVisibility_Column else TabularInterop.TableViewHeadersVisibility_None,
        )

    /** The row density (TableView.Density). */
    var density: TableDensity
        get() = TableDensity.of(tableView.getInt(TabularInterop.ITableView_get_Density))
        set(value) = tableView.call(TabularInterop.ITableView_put_Density, value.native)

    /** Whether column widths can be changed by dragging the column header borders (TableView.CanUserResizeColumns). */
    var canUserResizeColumns: Boolean
        get() = tableView.getBool(TabularInterop.ITableView_get_CanUserResizeColumns)
        set(value) = tableView.putBool(TabularInterop.ITableView_put_CanUserResizeColumns, value)

    /** The row background color (TableView.RowBackground). The default if null. */
    var rowBackground: WColor? = null
        set(value) {
            field = value
            putBrush(TabularInterop.ITableView_put_RowBackground, value)
        }

    /** The background color of every other row (TableView.AlternatingRowBackground; stripes). No stripes if null. */
    var alternatingRowBackground: WColor? = null
        set(value) {
            field = value
            putBrush(TabularInterop.ITableView_put_AlternatingRowBackground, value)
        }

    /** Text shown in the center when there are no rows (generates TableView.EmptyTemplate). Nothing is shown if null. */
    var emptyText: String? = null
        set(value) {
            field = value
            putTemplate(
                TabularInterop.ITableView_put_EmptyTemplate,
                value?.let {
                    "<TextBlock Text=\"${escape(it)}\" HorizontalAlignment=\"Center\" VerticalAlignment=\"Center\" " +
                        "Margin=\"24\" Foreground=\"{ThemeResource TextFillColorSecondaryBrush}\" />"
                },
            )
        }

    /**
     * The content of the DataTemplate shown when there are no rows (XAML; TableView.EmptyTemplate).
     * The default namespace (xmlns) is added automatically. When set, it takes precedence over [emptyText].
     */
    var emptyTemplate: String? = null
        set(value) {
            field = value
            putTemplate(TabularInterop.ITableView_put_EmptyTemplate, value)
        }

    // ------------------------------------------------------------------
    // Internals: binding the model and columns
    // ------------------------------------------------------------------

    /** Recreates the row collection of the current model and sets it as ItemsSource. */
    private fun attachModel() {
        rows.clear()
        rows = TableRowCollection(rowValues)
        rows.insert(0, model.getRowCount())
        source?.release()
        val statics = Activation.factory(TabularInterop.CLS_TableViewSource, TabularInterop.IID_ITableViewSourceStatics)
        val newSource = try {
            statics.getPtr(TabularInterop.ITableViewSourceStatics_From, rows.comObject.primary)
        } finally {
            statics.release()
        }
        source = newSource
        applyRowFilter()
        applyGrouping()
        tableView.call(TabularInterop.ITableView_put_ItemsSource, newSource.ptr)
        model.addTableModelListener(modelListener)
        if (autoCreateColumnsFromModel) createDefaultColumnsFromModel() else rebuildAllPeers()
    }

    private fun attachColumnModel() {
        for ((index, column) in columnModel.getColumns().withIndex()) {
            val peer = TableViewColumnPeer(column, columnHost)
            peers.add(index, peer)
            nativeColumns.call(FoundationInterop.IVector_Append, peer.native.ptr)
        }
        columnModel.addColumnModelListener(columnModelListener)
    }

    private fun removeAllPeers() {
        nativeColumns.call(FoundationInterop.IVector_Clear)
        for (peer in peers) peer.dispose()
        peers.clear()
    }

    /** Recreates the native TableViewColumn of every column (when column types or renderer resolution change). */
    private fun rebuildAllPeers() {
        for (index in peers.indices) rebuildPeer(peers[index])
    }

    private fun rebuildPeer(peer: TableViewColumnPeer) {
        val index = peers.indexOf(peer)
        if (index < 0) return
        val replacement = TableViewColumnPeer(peer.column, columnHost)
        peers[index] = replacement
        nativeColumns.call(FoundationInterop.IVector_SetAt, index, replacement.native.ptr)
        peer.dispose()
    }

    private fun onTableChanged(event: TableModelEvent) {
        when {
            event.firstRow == TableModelEvent.HEADER_ROW -> onStructureChanged()
            event.type == TableModelEvent.INSERT -> rows.insert(event.firstRow, event.lastRow - event.firstRow + 1)
            event.type == TableModelEvent.DELETE -> rows.remove(event.firstRow, event.lastRow - event.firstRow + 1)
            event.lastRow == Int.MAX_VALUE -> onDataChanged()
            else -> for (row in event.firstRow..minOf(event.lastRow, rows.size - 1)) updateRow(row, event.column)
        }
    }

    /** A column structure change: recreates the rows and recreates the columns (or updates the column headers). */
    private fun onStructureChanged() {
        rows.remove(0, rows.size)
        rows.insert(0, model.getRowCount())
        if (autoCreateColumnsFromModel) createDefaultColumnsFromModel() else rebuildAllPeers()
    }

    /** A change of all data: notifies existing rows as value changes, and adds or removes the difference in row count. */
    private fun onDataChanged() {
        val newCount = model.getRowCount()
        val oldCount = rows.size
        for (row in 0 until minOf(newCount, oldCount)) updateRow(row, TableModelEvent.ALL_COLUMNS)
        if (newCount > oldCount) rows.insert(oldCount, newCount - oldCount)
        if (newCount < oldCount) rows.remove(newCount, oldCount - newCount)
    }

    private fun updateRow(row: Int, column: Int) {
        val reshape = sorter?.sortsOnUpdates == true || sorter?.getRowFilter() != null || groupKey != null
        if (writingCell == row to column) {
            // The binding is writing on edit commit: do not re-notify the same row inside that notification; apply it later
            val item = rows[row]
            Dispatcher.invokeLater {
                if (item.modelRow >= 0 && item.modelRow < rows.size && rows[item.modelRow] === item) {
                    rows.update(item.modelRow, column, reshape)
                }
            }
            return
        }
        rows.update(row, column, reshape)
    }

    /** The binding (edit commit) wrote a value to a cell: converts it to the column type and writes it back to the model. */
    private fun onValueWritten(item: TableRowItem, column: Int, value: Any?) {
        val row = item.modelRow
        val converted = TableCellValues.convert(value, model.getColumnClass(column))
        if (converted === TableCellValues.INVALID) {
            // Input that cannot be converted (e.g. letters in a numeric column) is not written to the model, and the
            // display reverts to the model's value
            Dispatcher.invokeLater { if (rows.size > item.modelRow && rows[item.modelRow] === item) item.raiseValueChanged(column) }
            return
        }
        writingCell = row to column
        try {
            model.setValueAt(converted, row, column)
        } finally {
            writingCell = null
        }
    }

    // ------------------------------------------------------------------
    // Internals: native events
    // ------------------------------------------------------------------

    private fun subscribeNativeEvents() {
        tableView.addEventHandler(
            "WinUI4K.TableViewBeginningEditHandler",
            TabularInterop.IID_BeginningEditHandler,
            TabularInterop.ITableView_add_BeginningEdit,
        ) { _, args -> onBeginningEdit(ComPtr(args)) }
        tableView.addEventHandler(
            "WinUI4K.TableViewCellEditEndingHandler",
            TabularInterop.IID_CellEditEndingHandler,
            TabularInterop.ITableView_add_CellEditEnding,
        ) { _, args -> onCellEditEnding(ComPtr(args)) }
        tableView.addEventHandler(
            "WinUI4K.TableViewSortingHandler",
            TabularInterop.IID_SortingHandler,
            TabularInterop.ITableView_add_Sorting,
        ) { _, args -> onSorting(ComPtr(args)) }
        tableView.addEventHandler(
            "WinUI4K.TableViewSortedHandler",
            TabularInterop.IID_SortedHandler,
            TabularInterop.ITableView_add_Sorted,
        ) { _, args -> onSorted(ComPtr(args)) }
    }

    private fun onBeginningEdit(args: ComPtr) {
        val item = args.getPtrOrNull(TabularInterop.ITableViewBeginningEditEventArgs_get_Item)?.let { itemOf(it) }
        val peer = args.getPtrOrNull(TabularInterop.ITableViewBeginningEditEventArgs_get_Column)?.let { peerOf(it) }
        if (item == null || peer == null) return
        val modelColumn = peer.column.modelIndex
        val editable = modelColumn in 0 until model.getColumnCount() && model.isCellEditable(item.modelRow, modelColumn)
        if (!editable) {
            args.putBool(TabularInterop.ITableViewBeginningEditEventArgs_put_Cancel, true)
            return
        }
        val event = TableCellEditEvent(convertRowIndexToView(item.modelRow), peers.indexOf(peer), item.modelRow, modelColumn, null)
        for (listener in beginningEditListeners.toList()) listener.editing(event)
        if (event.isCanceled) {
            args.putBool(TabularInterop.ITableViewBeginningEditEventArgs_put_Cancel, true)
            return
        }
        editingItem = item
        editingPeer = peer
    }

    private fun onCellEditEnding(args: ComPtr) {
        val item = args.getPtrOrNull(TabularInterop.ITableViewCellEditEndingEventArgs_get_Item)?.let { itemOf(it) }
        val peer = args.getPtrOrNull(TabularInterop.ITableViewCellEditEndingEventArgs_get_Column)?.let { peerOf(it) }
        if (item != null && peer != null && cellEditEndingListeners.isNotEmpty()) {
            val action = TableEditAction.of(args.getInt(TabularInterop.ITableViewCellEditEndingEventArgs_get_EditAction))
            val event = TableCellEditEvent(
                convertRowIndexToView(item.modelRow),
                peers.indexOf(peer),
                item.modelRow,
                peer.column.modelIndex,
                action,
            )
            for (listener in cellEditEndingListeners.toList()) listener.editing(event)
            if (event.isCanceled) {
                args.putBool(TabularInterop.ITableViewCellEditEndingEventArgs_put_Cancel, true)
                return
            }
        }
        editingItem = null
        editingPeer = null
    }

    private fun onSorting(args: ComPtr) {
        val peer = args.getPtrOrNull(TabularInterop.ITableViewSortingEventArgs_get_Column)?.let { peerOf(it) } ?: return
        val order = sortOrderOf(args.getInt(TabularInterop.ITableViewSortingEventArgs_get_Direction))
        val event = TableSortEvent(peers.indexOf(peer), peer.column.modelIndex, order)
        for (listener in sortingListeners.toList()) listener.sorting(event)
        if (event.isCanceled) {
            args.putBool(TabularInterop.ITableViewSortingEventArgs_put_Cancel, true)
            return
        }
        // A single-column sort from clicking a column header replaces any previous multi-key sort
        if (multiSortKeys.isNotEmpty()) {
            multiSortKeys = emptyList()
            clearSourceSort()
        }
        sorter?.notifySortOrderChanged()
    }

    private fun onSorted(args: ComPtr) {
        val peer = args.getPtrOrNull(TabularInterop.ITableViewSortedEventArgs_get_Column)?.let { peerOf(it) }
        if (peer != null && sortedListeners.isNotEmpty()) {
            val order = sortOrderOf(args.getInt(TabularInterop.ITableViewSortedEventArgs_get_Direction))
            val event = TableSortEvent(peers.indexOf(peer), peer.column.modelIndex, order)
            for (listener in sortedListeners.toList()) listener.sorting(event)
        }
        sorter?.notifySorted()
    }

    /** Looks up the row object for an item passed from native code (an owned reference) and releases the item. */
    private fun itemOf(pointer: ComPtr): TableRowItem? = try {
        rows.itemOf(pointer.ptr)
    } finally {
        pointer.release()
    }

    /** Maps a TableViewColumn passed from native code (an owned reference) back to its column and releases it. */
    private fun peerOf(pointer: ComPtr): TableViewColumnPeer? {
        val unknown = pointer.queryInterface(KComObject.IID_IUNKNOWN)
        pointer.release()
        return try {
            peers.firstOrNull { it.identity == unknown.ptr.address }
        } finally {
            unknown.release()
        }
    }

    // ------------------------------------------------------------------
    // Internals: sorting, filtering and grouping
    // ------------------------------------------------------------------

    private val sorterHost: RowSorterHost = object : RowSorterHost {
        override fun sortKeysChanged(keys: List<RowSorter.SortKey>) {
            // Remove the previous multi-key sort (the TableViewSource sort axes) before setting it again
            if (multiSortKeys.isNotEmpty()) clearSourceSort()
            multiSortKeys = emptyList()
            if (keys.size > 1) {
                sortByKeys(keys)
                return
            }
            val key = keys.firstOrNull()
            val peer = key?.let { k -> peers.firstOrNull { it.column.modelIndex == k.column } }
            if (key == null || peer == null) {
                callBool(TabularInterop.ITableView_ClearSort)
                return
            }
            callBool(TabularInterop.ITableView_SortByColumn, peer.native.ptr, directionOf(key.sortOrder))
        }

        override fun toggleSortOrder(column: Int) {
            val peer = peers.firstOrNull { it.column.modelIndex == column } ?: return
            callBool(TabularInterop.ITableView_ToggleSortDirection, peer.native.ptr)
        }

        override fun currentSortKeys(): List<RowSorter.SortKey> = multiSortKeys.ifEmpty {
            peers.mapNotNull { peer ->
                val order = peer.sortOrder()
                if (order == SortOrder.UNSORTED) null else RowSorter.SortKey(peer.column.modelIndex, order)
            }
        }

        override fun rowFilterChanged() {
            applyRowFilter()
        }

        override fun comparatorChanged(column: Int) {
            for (peer in peers) if (peer.column.modelIndex == column) peer.applySortProperties()
        }

        override fun sortableChanged(column: Int) {
            for (peer in peers) if (peer.column.modelIndex == column) peer.applySortProperties()
        }

        override fun convertRowIndexToModel(index: Int): Int = this@WTableView.convertRowIndexToModel(index)

        override fun convertRowIndexToView(index: Int): Int = this@WTableView.convertRowIndexToView(index)

        override fun viewRowCount(): Int = rowOrder.count()
    }

    /** The keys of the current multi-key sort (set as TableViewSource sort axes). Empty for a single-column sort. */
    private var multiSortKeys: List<RowSorter.SortKey> = emptyList()

    /**
     * Sorts by multiple keys (stacks TableViewSource.Sort(sortMemberPath, direction) in key order).
     * The axis declared first becomes the primary key, and later axes order the rows that tie on earlier axes (as
     * TableViewSource specifies). These axes are separate from TableView's column header sort (one column), so the column
     * header arrow appears only on the primary key's column. Comparators ([TableRowSorter.setComparator]) are not used in
     * this sort (the standard comparison of values applies).
     */
    private fun sortByKeys(keys: List<RowSorter.SortKey>) {
        val currentSource = source ?: return
        callBool(TabularInterop.ITableView_ClearSort)
        for (key in keys) {
            val result = Hstring.use(TableRowItem.keyOf(key.column)) { path ->
                currentSource.getPtr(TabularInterop.ITableViewSource_SortByPath, path, directionOf(key.sortOrder))
            }
            result.release()
        }
        multiSortKeys = keys
        sorter?.notifySortOrderChanged()
        sorter?.notifySorted()
    }

    /** Removes all sort axes set on TableViewSource (TableViewSource.ClearSort). */
    private fun clearSourceSort() {
        source?.getPtr(TabularInterop.ITableViewSource_ClearSort)?.release()
    }

    /** Sets the filter of the current [rowSorter] on TableViewSource (clears it if there is none). */
    private fun applyRowFilter() {
        val currentSource = source ?: return
        val currentSorter = sorter
        val result = if (currentSorter?.getRowFilter() == null) {
            currentSource.getPtr(TabularInterop.ITableViewSource_ClearFilter)
        } else {
            val predicate = KComObject("WinUI4K.TableViewPredicate", inspectable = false)
                .addInterface(
                    TabularInterop.IID_TableViewPredicate,
                    listOf(
                        // Invoke(this, Object item, out boolean) — vtbl[3]
                        KComObject.Method(DESC_PTR_PTR) { args ->
                            val item = rows.itemOf(args[1] as Ptr)
                            val include = item == null || currentSorter.includes(item.modelRow)
                            Ffi.backend.memory.putByte(args[2] as Ptr, 0, if (include) 1 else 0)
                            KComObject.S_OK
                        },
                    ),
                )
            try {
                currentSource.getPtr(TabularInterop.ITableViewSource_Filter, predicate.primary)
            } finally {
                predicate.release()
            }
        }
        result.release()
        sorter?.notifySorted()
    }

    /** Groups TableViewSource by the current [groupKey] (clears the grouping if there is none). */
    private fun applyGrouping() {
        val currentSource = source ?: return
        val key = groupKey
        val result = if (key == null) {
            currentSource.getPtr(TabularInterop.ITableViewSource_ClearGroupBy)
        } else {
            val selector = KComObject("WinUI4K.TableViewKeySelector", inspectable = false)
                .addInterface(
                    TabularInterop.IID_TableViewKeySelector,
                    listOf(
                        // Invoke(this, Object item, out Object key) — vtbl[3]
                        KComObject.Method(DESC_PTR_PTR) { args ->
                            val item = rows.itemOf(args[1] as Ptr)
                            val boxed = item?.let { PropertyValues.boxAny(groupKeyOf(key, model, it.modelRow)) }
                            Ffi.backend.memory.putPtr(args[2] as Ptr, 0, boxed?.ptr ?: Ptr.NULL)
                            KComObject.S_OK
                        },
                    ),
                )
            try {
                currentSource.getPtr(TabularInterop.ITableViewSource_GroupBy, selector.primary)
            } finally {
                selector.release()
            }
        }
        result.release()
    }

    // ------------------------------------------------------------------
    // Internals: helpers
    // ------------------------------------------------------------------

    private fun gridLines(): Int = tableView.getInt(TabularInterop.ITableView_get_GridLinesVisibility)

    private fun putGridLines(horizontal: Boolean, vertical: Boolean) {
        val value = when {
            horizontal && vertical -> GRID_LINES_ALL
            horizontal -> GRID_LINES_HORIZONTAL
            vertical -> GRID_LINES_VERTICAL
            else -> GRID_LINES_NONE
        }
        tableView.call(TabularInterop.ITableView_put_GridLinesVisibility, value)
    }

    private fun putBrush(slot: Int, color: WColor?) {
        if (color == null) {
            tableView.call(slot, null)
            return
        }
        val brush = color.createBrush()
        tableView.call(slot, brush.ptr)
        brush.release()
    }

    private fun putTemplate(slot: Int, content: String?) {
        if (content == null) {
            tableView.call(slot, null)
            return
        }
        val template = TableViewXaml.createDataTemplate(content)
        tableView.call(slot, template.ptr)
        template.release()
    }

    /** Calls a method that returns a boolean (taking an out boolean as its last argument). */
    private fun callBool(slot: Int, vararg args: Any?): Boolean = Ffi.backend.withScope { scope ->
        val out = scope.allocate(1, 1)
        tableView.call(slot, *args, out)
        Ffi.backend.memory.getByte(out, 0).toInt() != 0
    }

    /** The access point through which columns ([TableViewColumnPeer]) read the table's state. */
    private val columnHost: TableViewColumnHost = object : TableViewColumnHost {
        override val table: WTableView
            get() = this@WTableView
        override val model: TableModel
            get() = this@WTableView.model
        override val rows: TableRowCollection
            get() = this@WTableView.rows
        override val sorter: TableRowSorter<*>?
            get() = this@WTableView.sorter

        override fun columnClassOf(modelIndex: Int): Class<*> =
            if (modelIndex in 0 until model.getColumnCount()) model.getColumnClass(modelIndex) else Any::class.java

        override fun itemOf(pointer: ComPtr): TableRowItem? = this@WTableView.itemOf(pointer)

        override fun rebuild(peer: TableViewColumnPeer) {
            rebuildPeer(peer)
        }

        override fun viewColumnOf(peer: TableViewColumnPeer): Int = peers.indexOf(peer)
    }

    // Bind to the native side only after all properties are initialized (placed at the end because initialization
    // follows declaration order)
    init {
        // Merge the theme resources referenced by TableView's default style into the app (only the first time)
        WinUiUtilities.ensureTabularControlsResources()
        // Whether a cell is editable is decided by the model (isCellEditable) and the column (TableColumn.isEditable),
        // as in Swing. TableView defaults to read-only, so lift the control-wide restriction
        tableView.putBool(TabularInterop.ITableView_put_IsReadOnly, false)
        tableView.putBool(TabularInterop.ITableView_put_CanUserSortColumns, false)
        subscribeNativeEvents()
        attachColumnModel()
        attachModel()
    }

    private companion object {
        // TableViewGridLinesVisibility: All = 0, Horizontal = 1, None = 2, Vertical = 3 (extracted from winmd)
        private const val GRID_LINES_ALL = 0
        private const val GRID_LINES_HORIZONTAL = 1
        private const val GRID_LINES_NONE = 2
        private const val GRID_LINES_VERTICAL = 3

        private val DESC_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
    }
}

/** Converts a sort direction to TableView's SortDirection (the winmd value). */
private fun directionOf(order: SortOrder): Int = if (order == SortOrder.DESCENDING) {
    TabularInterop.SortDirection_Descending
} else {
    TabularInterop.SortDirection_Ascending
}

/** The value for [columnClass] (superclasses are also searched). null if none. */
private fun <T> lookupByClass(map: Map<Class<*>, T>, columnClass: Class<*>): T? {
    var current: Class<*>? = columnClass
    while (current != null) {
        map[current]?.let { return it }
        current = current.superclass
    }
    return null
}

/**
 * The group key passed to TableViewSource. TableViewSource determines identity by value-type keys, and TableView can
 * only turn String / Int32 / Int64 / Double keys into header text (others are shown as "(group)"), so integers and
 * decimals are widened to those types, and everything else (Boolean or arbitrary objects) becomes its toString()
 * string. Rows with a null key become an empty string so that they also form a single group.
 */
private fun groupKeyOf(key: TableGroupKey, model: TableModel, modelRow: Int): Any = when (val value = key.groupKey(model, modelRow)) {
    null -> ""
    is String, is Int, is Long, is Double -> value
    is Byte -> value.toInt()
    is Short -> value.toInt()
    is Float -> value.toDouble()
    else -> value.toString()
}
