package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
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
 *   and the JTable-style [selectionModel] ([WListSelectionModel]) / [setRowSelectionInterval] / [changeSelection] / [rowSelectionAllowed].
 *   TableView can select only one row, so intervals and multiple rows are shrunk to the single lead row, and there is never a column or cell selection ([selectedColumn] etc.)
 * - Row and column from a point: [rowAtPoint] / [columnAtPoint] (DIP)
 * - Row double-click: [addRowInvokedListener]
 * - Sorting: clicking column headers (enabled when [rowSorter] is set) / [TableRowSorter.setSortKeys] /
 *   [addSortingListener] (cancelable) / [addSortedListener]
 * - Filtering: [TableRowSorter.setRowFilter] (TableViewSource.Filter)
 * - Grouping: [groupBy] / [clearGrouping] / [expandAllGroups] / [collapseAllGroups] / [groupHeaderTemplate]
 * - Appearance: [showHorizontalLines] / [showVerticalLines] / [isHeaderVisible] / [density] / [rowBackground] /
 *   [alternatingRowBackground] / [emptyText] / [emptyTemplate] / [canUserResizeColumns].
 *   There is no equivalent of JTable.setSelectionBackground; the color of selected rows follows the theme (lightweight styling keys put into the Resources of the table or rows
 *   do not reach TableView rows. See "TableView (2.5.4-experimental): putting MUX_TabularSurfaceRowBackgroundSelectedBrush into the Resources of the table or rows
 *   does not change the background of selected rows" in doc/troubleshooting.md)
 *
 * Row and column indices are view positions (rows after sorting and filtering, columns in display order), except for
 * those whose names contain model. Conversion to and from the model is done with [convertRowIndexToModel] /
 * [convertColumnIndexToModel] and so on (as in Swing).
 */
@Suppress("LargeClass") // Holds the same public API as JTable in one class (internal processing is split into TableRows, TableViewColumnPeer, TableSelectionSync, etc.)
class WTable @JvmOverloads constructor(
    model: TableModel = DefaultTableModel(),
    columnModel: TableColumnModel? = null,
) : WControl(
    Activation.composeDefault(TabularInterop.CLS_TableView, TabularInterop.IID_ITableViewFactory), // Default interface = ITableView
) {
    /** A table of row data [rowData] and column names [columnNames] (equivalent to JTable(Object[][], Object[])). */
    constructor(rowData: List<List<Any?>>, columnNames: List<Any?>) : this(DefaultTableModel(rowData, columnNames))

    /** A table of the arrays of row data [rowData] and column names [columnNames] (equivalent to JTable(Object[][], Object[])). */
    constructor(rowData: Array<out Array<out Any?>>, columnNames: Array<out Any?>) : this(
        DefaultTableModel(rowData.map { it.toList() }, columnNames.toList()),
    )

    /**
     * A table of [model], the column model [columnModel] (columns are created from the model if null) and the selection model [selectionModel] (the default if null)
     * (equivalent to JTable(TableModel, TableColumnModel, ListSelectionModel)).
     */
    constructor(model: TableModel, columnModel: TableColumnModel?, selectionModel: WListSelectionModel?) : this(model, columnModel) {
        if (selectionModel != null) this.selectionModel = selectionModel
    }

    /** A table of [rowCount] rows × [columnCount] columns with all values null (equivalent to JTable(int, int)). */
    constructor(rowCount: Int, columnCount: Int) : this(DefaultTableModel(rowCount, columnCount))

    /** ITableView (the default interface). */
    private val tableView: ComPtr = inspectable

    /**
     * The reference through which things passed to the native side (the row collection, columns, event handlers, filter predicates and so on) look up this table.
     * If they captured the table strongly, it would create the cycle "native -> handler -> wrapper -> native", and the table would not be
     * collected even after being removed from the window (doc/memory_management.md, "Handling cycles that cross the boundary"). The table always receives model changes and edit commits,
     * so it is treated as always having listeners and is held strongly only while being built and while shown (and while a removed subtree remains).
     * A table that is never placed in a window is not collected.
     */
    private val anchor = ListenerAnchor(this).also { it.listenerAdded() }

    /** Sync between the row selection and [selectionModel]. */
    private val selection = TableSelectionSync(this, inspectable)

    /** TableView.Columns (IVector<TableViewColumn>). */
    private val nativeColumns: ComPtr by lazy { own(tableView.getPtr(TabularInterop.ITableView_get_Columns)) }

    /** The access point for reading and writing model values (used by row objects). */
    private val rowValues: TableRowValues = AnchoredRowValues(anchor)

    /** The row collection behind ItemsSource (in model row order). */
    internal var rows = TableRowCollection(rowValues)
        private set

    /** The TableViewSource set as ItemsSource (projects the row collection sorted, filtered and grouped). */
    private var source: ComPtr? = null

    /** The visible columns (matching the order of columnModel). */
    internal val peers = mutableListOf<TableViewColumnPeer>()

    private val modelListener = anchoredModelListener(anchor)

    private val columnModelListener = object : TableColumnModelListener {
        override fun columnAdded(event: TableColumnModelEvent) {
            val peer = TableViewColumnPeer(this@WTable.columnModel.getColumn(event.toIndex), columnHost)
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
    internal val rowInvoker = anchoredRowInvoker(uiElement, anchor)

    /** The grouping key function (no grouping if null). */
    private var groupKey: TableGroupKey? = null

    /** Reads the row order in the view (the result of sorting, filtering and grouping). */
    private val rowOrder: TableViewRowOrder = TableViewRowOrder(
        root = { dependencyObject },
        rows = { rows },
        model = { this.model },
        sorter = { sorter },
        sortKeys = { sorterBridge.currentSortKeys() },
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

    /**
     * The model of the data to display. Changing it recreates the rows and (if autoCreateColumnsFromModel) the columns, and clears the selection, as JTable.setModel does.
     * The model's fireTableDataChanged (a change of all rows) and fireTableStructureChanged (a change of the column structure) also clear the selection, as in Swing
     * (adding or removing rows, or changing some rows, keeps the same model row selected).
     */
    var model: TableModel = model
        set(value) {
            val old = field
            if (old === value) return
            old.removeTableModelListener(modelListener)
            field = value
            // Clear the selection, as Swing's JTable.setModel does
            selection.reset()
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
     * headers is also disabled). WTable accepts only [TableRowSorter].
     */
    var rowSorter: RowSorter<out TableModel>? = null
        set(value) {
            require(value == null || value is TableRowSorter<*>) { "WTable supports TableRowSorter only" }
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
                if (pending.isNotEmpty()) sorterBridge.sortKeysChanged(pending)
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

    internal val sorter: TableRowSorter<*>?
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

    /**
     * The column of [identifier] (JTable.getColumn). Looks for a column whose [TableColumn.identifier] is equal, and if none, returns the displayed column
     * whose model column name ([TableModel.getColumnName]) is equal. Columns created automatically from the model have no headerValue
     * (the heading follows the model's column name), so this is a fallback so that they can be looked up by column name as in Swing (headerValue is not changed).
     * @throws IllegalArgumentException if not found either way
     */
    fun getColumn(identifier: Any?): TableColumn {
        val columns = columnModel.getColumns()
        val byIdentifier = columns.firstOrNull { it.identifier == identifier }
        if (byIdentifier != null) return byIdentifier
        return columns.firstOrNull { it.identifier == null && it.modelIndex in 0 until model.getColumnCount() && model.getColumnName(it.modelIndex) == identifier }
            ?: throw IllegalArgumentException("Identifier not found: $identifier")
    }

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

    /** The selected row (a view index; JTable.getSelectedRow). -1 if unselected. Read from [selectionModel], so the new selection is visible even inside its listeners. */
    val selectedRow: Int
        get() = selection.selectedRow()

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

    /** Whether row [row] (view) is selected (JTable.isRowSelected; read from [selectionModel] like [selectedRow]). */
    fun isRowSelected(row: Int): Boolean = row >= 0 && row == selectedRow

    /** Clears the selection (JTable.clearSelection; clears the selection of [selectionModel] and the displayed selection). */
    fun clearSelection() = selection.call { it.clearSelection() }

    /**
     * The row selection model (JTable.getSelectionModel / setSelectionModel). Indices are view rows, and it is synced with the display selection both ways.
     * TableView can select only one row, so even if the model selects intervals or several rows, it is shrunk to the lead row only (the smallest row if the lead is not selected).
     * The default is a single selection [WDefaultListSelectionModel] (Swing's JTable defaults to multiple intervals). After sorting, filtering, and adding or removing rows,
     * the same model row stays selected as in Swing (the selection of rows that disappeared from the display or were removed is cleared).
     * WinUI does not report operations in progress, so valueIsAdjusting of events from display operations is always false
     * (when the user calls the model's setValueIsAdjusting, events with true also arrive, as in Swing).
     * As in Swing, the selection model references the table through its listener, so the table is not collected while the model is held.
     */
    var selectionModel: WListSelectionModel
        get() = selection.call { it }
        set(value) {
            selection.model = value
        }

    /**
     * Sets the Swing selection mode (a ListSelectionModel constant) (JTable.setSelectionMode). Clears the selection first, as JTable does.
     * TableView can select only one row, so the [selectionModel]'s selection mode becomes SINGLE_SELECTION for any value. Make a non-selectable table with
     * [selectionMode] NONE or [rowSelectionAllowed].
     * @throws IllegalArgumentException if it is none of the constants
     */
    fun setSelectionMode(selectionMode: Int) {
        require(selectionMode in WListSelectionModel.SINGLE_SELECTION..WListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
            "invalid selectionMode: $selectionMode"
        }
        selection.call {
            it.clearSelection()
            it.setSelectionMode(WListSelectionModel.SINGLE_SELECTION)
        }
    }

    /** Whether rows can be selected (JTable.getRowSelectionAllowed / setRowSelectionAllowed). false sets [selectionMode] to NONE, true to SINGLE. */
    var rowSelectionAllowed: Boolean
        get() = selectionMode != TableSelectionMode.NONE
        set(value) {
            selectionMode = if (value) TableSelectionMode.SINGLE else TableSelectionMode.NONE
        }

    /**
     * Selects rows [index0]..[index1] (JTable.setRowSelectionInterval). Only one row can be selected, so it becomes row [index1].
     * @throws IllegalArgumentException if a row is out of range
     */
    fun setRowSelectionInterval(index0: Int, index1: Int) = selection.call { it.setSelectionInterval(selection.boundRow(index0), selection.boundRow(index1)) }

    /**
     * Adds rows [index0]..[index1] to the selection (JTable.addRowSelectionInterval). Only one row can be selected, so only row [index1] remains.
     * @throws IllegalArgumentException if a row is out of range
     */
    fun addRowSelectionInterval(index0: Int, index1: Int) = selection.call { it.addSelectionInterval(selection.boundRow(index0), selection.boundRow(index1)) }

    /**
     * Removes rows [index0]..[index1] from the selection (JTable.removeRowSelectionInterval).
     * @throws IllegalArgumentException if a row is out of range
     */
    fun removeRowSelectionInterval(index0: Int, index1: Int) = selection.call { it.removeSelectionInterval(selection.boundRow(index0), selection.boundRow(index1)) }

    /**
     * Changes the selection by the same rules as Swing's JTable.changeSelection ([columnIndex] is not used since there is no column selection).
     * If neither toggle nor extend, selects only [rowIndex]; toggle only toggles the selection of [rowIndex]; extend only selects from
     * the anchor to [rowIndex] (only one row can be selected, so [rowIndex]); both extend the anchor's selection state to anchor..[rowIndex].
     * Does nothing if [rowIndex] is outside the table's rows (changes neither the selection nor anchor / lead).
     */
    fun changeSelection(rowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {
        selection.changeSelection(rowIndex, toggle, extend) { isCellSelected(it, columnIndex) }
    }

    /** The selected column (JTable.getSelectedColumn). TableView cannot select columns, so always -1. */
    val selectedColumn: Int
        get() = -1

    /** The selected columns (JTable.getSelectedColumns). TableView cannot select columns, so always empty. */
    val selectedColumns: IntArray
        get() = IntArray(0)

    /** The number of selected columns (JTable.getSelectedColumnCount). TableView cannot select columns, so always 0. */
    val selectedColumnCount: Int
        get() = 0

    /** Whether column [column] is selected (JTable.isColumnSelected). TableView cannot select columns, so always false. */
    @Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant") // To match Swing's shape
    fun isColumnSelected(column: Int): Boolean = false

    /** Whether the cell at row [row], column [column] is selected (JTable.isCellSelected). Columns cannot be selected, so it is decided by the row selection. */
    @Suppress("UNUSED_PARAMETER") // To match Swing's shape
    fun isCellSelected(row: Int, column: Int): Boolean = rowSelectionAllowed && row >= 0 && isRowSelected(row)

    /** Whether columns can be selected (JTable.getColumnSelectionAllowed). TableView cannot select columns, so always false, and setting true does nothing. */
    var columnSelectionAllowed: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) = Unit

    /** Whether cells can be selected individually (JTable.getCellSelectionEnabled). TableView cannot select cells, so always false, and setting true does nothing. */
    var cellSelectionEnabled: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) = Unit

    /**
     * The view index of the row at the table coordinates ([x], [y]) (DIP; the table's top-left is the origin) (JTable.rowAtPoint).
     * -1 if not over a row (column header, area without rows, outside the table, not shown).
     */
    fun rowAtPoint(x: Double, y: Double): Int = TableHitTest.rowAt(uiElement, rowInvoker, x, y)

    /**
     * The display-order index of the column at the table coordinates ([x], [y]) (DIP; the table's top-left is the origin) (JTable.columnAtPoint).
     * Horizontal scrolling is taken into account over rows and the column header. -1 outside the columns (right of the last column, outside the table, not shown).
     */
    fun columnAtPoint(x: Double, y: Double): Int = TableHitTest.columnAt(uiElement, columnModel.getColumns().map { it.width }, x, y)

    /** The row at [point] (a position in DIP as integers, such as [WMouseEvent.point]) (JTable.rowAtPoint(Point)). See the overload above for details. */
    fun rowAtPoint(point: WPoint): Int = rowAtPoint(point.x.toDouble(), point.y.toDouble())

    /** The column at [point] (a position in DIP as integers) (JTable.columnAtPoint(Point)). See the overload above for details. */
    fun columnAtPoint(point: WPoint): Int = columnAtPoint(point.x.toDouble(), point.y.toDouble())

    /**
     * Selects all rows (JTable.selectAll). As in Swing, if editing, cancels the edit and then selects from row 0 to the last row.
     * TableView can select only one row, so the last row is selected, as with a single-selection JTable. Does nothing if there are no rows or columns.
     */
    fun selectAll() {
        if (isEditing) cancelCellEditing()
        if (rowCount > 0 && columnCount > 0) setRowSelectionInterval(0, rowCount - 1)
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

    /**
     * The renderer that draws the cell at row [row], column [column] (display order) (JTable.getCellRenderer). As in Swing, the column's [TableColumn.cellRenderer],
     * or if none, [getDefaultRenderer] for the column's value type. null if neither exists (unlike Swing, WinUI draws the value's string or template).
     * It is decided by the column regardless of the row (to draw rows differently, branch inside the renderer).
     */
    @Suppress("UNUSED_PARAMETER") // To match Swing's shape (independent of the row)
    fun getCellRenderer(row: Int, column: Int): TableCellRenderer? =
        columnModel.getColumn(column).cellRenderer ?: getDefaultRenderer(getColumnClass(column))

    /**
     * The editor used to edit the cell at row [row], column [column] (display order) (JTable.getCellEditor(int, int)). As in Swing, the column's
     * [TableColumn.cellEditor], or if none, [getDefaultEditor] for the column's value type. null if neither exists (WinUI4K's default editing applies).
     */
    @Suppress("UNUSED_PARAMETER") // To match Swing's shape (independent of the row)
    fun getCellEditor(row: Int, column: Int): TableCellEditor? =
        columnModel.getColumn(column).cellEditor ?: getDefaultEditor(getColumnClass(column))

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

    internal fun rebuildPeer(peer: TableViewColumnPeer) {
        val index = peers.indexOf(peer)
        if (index < 0) return
        val replacement = TableViewColumnPeer(peer.column, columnHost)
        peers[index] = replacement
        nativeColumns.call(FoundationInterop.IVector_SetAt, index, replacement.native.ptr)
        peer.dispose()
    }

    internal fun onTableChanged(event: TableModelEvent) {
        when {
            event.firstRow == TableModelEvent.HEADER_ROW -> selection.reordering { onStructureChanged() }.also { selectionModel.clearSelection() }
            event.type == TableModelEvent.INSERT -> {
                selection.quietly { rows.insert(event.firstRow, event.lastRow - event.firstRow + 1) }
                selection.modelRowsChanged(event.firstRow, event.lastRow, inserted = true)
            }
            event.type == TableModelEvent.DELETE -> {
                selection.beforeModelRowsRemoved(event.firstRow, event.lastRow)
                selection.quietly { rows.remove(event.firstRow, event.lastRow - event.firstRow + 1) }
                selection.modelRowsChanged(event.firstRow, event.lastRow, inserted = false)
            }
            // As with Swing's JTable, a full change (fireTableDataChanged) clears the selection
            event.lastRow == Int.MAX_VALUE -> selection.reordering { onDataChanged() }.also { selectionModel.clearSelection() }
            else -> selection.reordering { for (row in event.firstRow..minOf(event.lastRow, rows.size - 1)) updateRow(row, event.column) }
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
    internal fun onValueWritten(item: TableRowItem, column: Int, value: Any?) {
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
        val anchor = anchor // Let the handler capture only the anchor, not the table (no cycle)
        tableView.addEventHandler(
            "WinUI4K.TableViewBeginningEditHandler",
            TabularInterop.IID_BeginningEditHandler,
            TabularInterop.ITableView_add_BeginningEdit,
        ) { _, args -> anchor.get()?.onBeginningEdit(ComPtr(args)) }
        tableView.addEventHandler(
            "WinUI4K.TableViewCellEditEndingHandler",
            TabularInterop.IID_CellEditEndingHandler,
            TabularInterop.ITableView_add_CellEditEnding,
        ) { _, args -> anchor.get()?.onCellEditEnding(ComPtr(args)) }
        tableView.addEventHandler(
            "WinUI4K.TableViewSortingHandler",
            TabularInterop.IID_SortingHandler,
            TabularInterop.ITableView_add_Sorting,
        ) { _, args -> anchor.get()?.onSorting(ComPtr(args)) }
        tableView.addEventHandler(
            "WinUI4K.TableViewSortedHandler",
            TabularInterop.IID_SortedHandler,
            TabularInterop.ITableView_add_Sorted,
        ) { _, args -> anchor.get()?.onSorted(ComPtr(args)) }
        tableView.addEventHandler(
            "WinUI4K.TableViewSelectionSyncHandler",
            TabularInterop.IID_SelectionChangedHandler,
            TabularInterop.ITableView_add_SelectionChanged,
        ) { _, _ -> anchor.get()?.selection?.pullFromNative() }
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
        // Do not copy the selection during sorting, and reselect the same model row when it ends (ends with Sorted)
        selection.beginUserReorder()
        try {
            // A single-column sort by clicking a column header replaces the previous multi-key sort
            if (multiSortKeys.isNotEmpty()) {
                multiSortKeys = emptyList()
                clearSourceSort()
            }
            sorter?.notifySortOrderChanged()
        } catch (e: RuntimeException) {
            selection.endUserReorder() // Do not leave syncing stopped even if a user listener throws
            throw e
        }
    }

    private fun onSorted(args: ComPtr) {
        // Restore syncing first and reselect the same model row (syncing is not left stopped even if a listener throws, and listeners see the new selection)
        selection.endUserReorder()
        val peer = args.getPtrOrNull(TabularInterop.ITableViewSortedEventArgs_get_Column)?.let { peerOf(it) }
        if (peer != null && sortedListeners.isNotEmpty()) {
            val order = sortOrderOf(args.getInt(TabularInterop.ITableViewSortedEventArgs_get_Direction))
            val event = TableSortEvent(peers.indexOf(peer), peer.column.modelIndex, order)
            for (listener in sortedListeners.toList()) listener.sorting(event)
        }
        sorter?.notifySorted()
    }

    /** Looks up the row object for an item passed from native code (an owned reference) and releases the item. */
    internal fun itemOf(pointer: ComPtr): TableRowItem? = try {
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

    /** The gateway passed to [TableRowSorter] (predicates and comparisons look up the sorting from the native side, so it is an [AnchoredSorterHost] that looks the table up weakly). */
    private val sorterHost: RowSorterHost = AnchoredSorterHost(anchor)

    /** The actual sorting implementation (where [sorterHost] forwards to; held strongly only by the table itself). */
    internal val sorterBridge: RowSorterHost = object : RowSorterHost {
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

        override fun convertRowIndexToModel(index: Int): Int = this@WTable.convertRowIndexToModel(index)

        override fun convertRowIndexToView(index: Int): Int = this@WTable.convertRowIndexToView(index)

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
        selection.reordering {
            callBool(TabularInterop.ITableView_ClearSort)
            for (key in keys) {
                val result = Hstring.use(TableRowItem.keyOf(key.column)) { path ->
                    currentSource.getPtr(TabularInterop.ITableViewSource_SortByPath, path, directionOf(key.sortOrder))
                }
                result.release()
            }
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
        val result = selection.reordering { filterResult(currentSource) }
        result.release()
        sorter?.notifySorted()
    }

    private fun filterResult(currentSource: ComPtr): ComPtr {
        return if (sorter?.getRowFilter() == null) {
            currentSource.getPtr(TabularInterop.ITableViewSource_ClearFilter)
        } else {
            val predicate = anchoredRowFilter(anchor)
            try {
                currentSource.getPtr(TabularInterop.ITableViewSource_Filter, predicate.primary)
            } finally {
                predicate.release()
            }
        }
    }

    /** Groups TableViewSource by the current [groupKey] (clears the grouping if there is none). */
    private fun applyGrouping() {
        val currentSource = source ?: return
        val key = groupKey
        val result = selection.reordering { groupResult(currentSource, key) }
        result.release()
    }

    private fun groupResult(currentSource: ComPtr, key: TableGroupKey?): ComPtr {
        return if (key == null) {
            currentSource.getPtr(TabularInterop.ITableViewSource_ClearGroupBy)
        } else {
            val selector = anchoredGroupKeySelector(anchor, key)
            try {
                currentSource.getPtr(TabularInterop.ITableViewSource_GroupBy, selector.primary)
            } finally {
                selector.release()
            }
        }
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

    /** The gateway through which columns ([TableViewColumnPeer]) read the table's state (looked up from the native columns, so it looks the table up weakly). */
    private val columnHost: TableViewColumnHost = AnchoredColumnHost(anchor)

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
        // TableView is in a cycle with the row collection (a Kotlin-implemented IObservableVector that strongly holds native handlers),
        // so it is not destroyed on the native side even when the wrapper is collected. Returning the reference when the wrapper is collected can make
        // the destruction run during XAML cleanup at exit and crash the process (0xC0000409), so one reference is kept (doc/troubleshooting.md)
        tableView.addRef()
        attachColumnModel()
        attachModel()
    }

    private companion object {
        // TableViewGridLinesVisibility: All = 0, Horizontal = 1, None = 2, Vertical = 3 (extracted from winmd)
        private const val GRID_LINES_ALL = 0
        private const val GRID_LINES_HORIZONTAL = 1
        private const val GRID_LINES_NONE = 2
        private const val GRID_LINES_VERTICAL = 3
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
internal fun groupKeyOf(key: TableGroupKey, model: TableModel, modelRow: Int): Any = when (val value = key.groupKey(model, modelRow)) {
    null -> ""
    is String, is Int, is Long, is Double -> value
    is Byte -> value.toInt()
    is Short -> value.toInt()
    is Float -> value.toDouble()
    else -> value.toString()
}
