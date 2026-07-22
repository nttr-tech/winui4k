package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * A column's sort direction (equivalent to WinUI.TableView's SortDirection).
 */
enum class SortDirection {
    /** Ascending. */
    ASCENDING,

    /** Descending. */
    DESCENDING,
}

/**
 * A column definition for [WTable] (equivalent to WinUI.TableView's TableViewColumn).
 */
class WTableColumn(
    /** The title shown in the column header. */
    val title: String,

    /** The column's width, in pixels. */
    val width: Double = 160.0,

    /** Whether clicking the header can sort this column (equivalent to TableViewColumn.CanSort). */
    val canSort: Boolean = true,
)

// Fluent colors referenced by the real WinUI.TableView's theme resources (Themes/Resources.xaml).
// Header background = SubtleFillColorSecondary; grid lines and outer border = CardStrokeColorDefault in
// Light, ControlStrokeColorDefault in Dark. Both are semi-transparent, so they're drawn over the background.
private val LIGHT_HEADER_BACKGROUND = WColor(0, 0, 0, 9)       // SubtleFillColorSecondary (Light #09000000)
private val LIGHT_GRID_LINE = WColor(0, 0, 0, 15)              // CardStrokeColorDefault (Light #0F000000)
private val DARK_HEADER_BACKGROUND = WColor(255, 255, 255, 15) // SubtleFillColorSecondary (Dark #0FFFFFFF)
private val DARK_GRID_LINE = WColor(255, 255, 255, 18)         // ControlStrokeColorDefault (Dark #12FFFFFF)

/**
 * JTable-like: a table built on top of WinUI 3's ListView.
 *
 * WinUI doesn't ship a table control, so this borrows the design of the third-party
 * [WinUI.TableView](https://github.com/w-ahmad/WinUI.TableView) (TableView : ListView,
 * with a column-header row + row items + sorting via header clicks): a column-header
 * row is placed in ListView's Header, and each row is rendered as a Grid whose columns
 * match the header's widths. Rendering also matches the real control's theme
 * resources — the header background, horizontal/vertical grid lines, and outer border
 * are painted with the Fluent colors for each of the light/dark themes (following
 * ActualThemeChanged).
 *
 * Provides:
 * [addRow] / [getValueAt] / [setValueAt] / [removeRow] / [removeAllRows] / [rowCount] / [columnCount],
 * [selectedRow] / [selectionMode] / [addRowSelectionListener] / [removeRowSelectionListener],
 * [sortBy] / [clearSort] / [canSortColumns] (a header click cycles through ascending -> descending -> cleared).
 */
class WTable(private val columns: List<WTableColumn>) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ListView, XamlInterop.IID_IListViewFactory), // default interface = IListView
) {
    private val selector: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_ISelector))
    }
    private val listViewBase: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IListViewBase))
    }
    private val itemsControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IItemsControl))
    }
    private val control: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IControl))
    }

    /** The IVector<Object> view of ItemsControl.Items (ItemCollection). */
    private val itemVector: ComPtr by lazy {
        val items = own(itemsControl.getPtr(XamlInterop.IItemsControl_get_Items))
        own(items.queryInterface(FoundationInterop.IID_IVector_Object))
    }

    /**
     * One row's data plus the UI elements used to display it.
     * [element] (the whole row's Border = the horizontal grid line) is what's added to Items.
     * [cellBorders] are the vertical grid lines, [cells] are the cell WLabels (rewritten by setValueAt).
     */
    private class Row(
        val values: MutableList<String>,
        val element: WBorder,
        val cellBorders: List<WBorder>,
        val cells: List<WLabel>,
    )

    /** The model rows (in the order they were added). Sorting never changes this order. */
    private val rows = mutableListOf<Row>()

    /** Display order -> model row index (the ordering with the current sort applied). */
    private var displayToModel: List<Int> = emptyList()

    /** The whole header row's Border (background color + the grid line below the header). */
    private val headerRowBorder: WBorder

    /** Each header cell's Border (vertical grid lines). */
    private val headerCellBorders: List<WBorder>

    /** The header's column buttons (used to update the sort-indicator display). */
    private val headerButtons: List<WButton>

    /** SelectionChanged event tokens registered via addRowSelectionListener. */
    private val selectionTokens = ListenerTokens<() -> Unit>()

    /** A guard so listeners don't fire from the re-selection that happens during rebuildItems. */
    private var isRebuilding = false

    /** Whether header clicks are allowed to sort (equivalent to TableView.CanSortColumns). */
    var canSortColumns: Boolean = true

    /** The index of the column currently sorted on, or -1 if unsorted. */
    var sortedColumn: Int = -1
        private set

    /** The current sort direction, or null if unsorted. */
    var sortDirection: SortDirection? = null
        private set

    /** The number of columns. */
    val columnCount: Int
        get() = columns.size

    /** The number of rows. */
    val rowCount: Int
        get() = rows.size

    /**
     * The model index (the order [addRow] was called in) of the selected row, or -1 if none is selected.
     * Returns this index rather than the display position even while sorted.
     */
    var selectedRow: Int
        get() {
            val displayIndex = selector.getInt(XamlInterop.ISelector_get_SelectedIndex)
            return if (displayIndex < 0) -1 else displayToModel[displayIndex]
        }
        set(value) = selector.call(XamlInterop.ISelector_put_SelectedIndex, displayToModel.indexOf(value))

    /** The selection mode (ListViewBase.SelectionMode). */
    var selectionMode: ListViewSelectionMode
        get() = ListViewSelectionMode.of(listViewBase.getInt(XamlInterop.IListViewBase_get_SelectionMode))
        set(value) = listViewBase.call(XamlInterop.IListViewBase_put_SelectionMode, value.native)

    init {
        require(columns.isNotEmpty()) { "columns must not be empty" }

        // The outer border (equivalent to the real TableView style's BorderBrush / BorderThickness=1).
        // The color is painted in applyTheme
        XamlStructs.putThickness(control, XamlInterop.IControl_put_BorderThickness, 1.0, 1.0, 1.0, 1.0)

        // Remove ListViewItem's default left/right padding so the grid lines reach the edges
        putItemContainerStyle()

        // Build the column-header row and place it in ListView's Header (the real control uses
        // the template's HeaderRow)
        val header = WGrid()
        header.addRow()
        val cellBorders = mutableListOf<WBorder>()
        headerButtons = columns.mapIndexed { index, column ->
            header.addColumn(GridLength.pixel(column.width))
            val button = WButton(column.title)
            styleHeaderButton(button)
            button.addActionListener { onHeaderClicked(index) }
            val cellBorder = buildCellBorder(button, isLast = index == columns.lastIndex)
            cellBorders += cellBorder
            header.add(cellBorder, 0, index)
            button
        }
        headerCellBorders = cellBorders

        headerRowBorder = WBorder(header)
        XamlStructs.putThickness(headerRowBorder.inspectable, XamlInterop.IBorder_put_BorderThickness, 0.0, 0.0, 0.0, 1.0)
        listViewBase.call(XamlInterop.IListViewBase_put_Header, headerRowBorder.uiElement.ptr)

        applyTheme()

        // Repaint whenever the theme (light / dark) changes
        frameworkElement.addEventHandler(
            "WinUI4K.ActualThemeChangedHandler",
            XamlInterop.IID_ActualThemeChangedHandler,
            XamlInterop.IFrameworkElement_add_ActualThemeChanged,
        ) { _, _ -> applyTheme() }
    }

    /** Appends a row. Any [values] short of the column count are padded with empty strings. */
    fun addRow(vararg values: String) {
        addRow(values.toList())
    }

    /** Appends a row. Any [values] short of the column count are padded with empty strings. */
    fun addRow(values: List<String>) {
        val cellValues = MutableList(columns.size) { values.getOrElse(it) { "" } }

        // The row's UI (a Grid whose columns match the header's widths + cell WLabels) is built
        // once when the row is added and reused afterward
        val grid = WGrid()
        grid.addRow()
        val cellBorders = mutableListOf<WBorder>()
        val cells = columns.mapIndexed { index, column ->
            grid.addColumn(GridLength.pixel(column.width))
            val cell = WLabel(cellValues[index])
            cell.verticalAlignment = VerticalAlignment.CENTER
            XamlStructs.putThickness(cell.frameworkElement, XamlInterop.IFrameworkElement_put_Margin, 12.0, 8.0, 12.0, 8.0)
            val cellBorder = buildCellBorder(cell, isLast = index == columns.lastIndex)
            cellBorders += cellBorder
            grid.add(cellBorder, 0, index)
            cell
        }

        // Add the row wrapped in a Border that draws the horizontal grid line along its bottom edge
        val element = WBorder(grid)
        XamlStructs.putThickness(element.inspectable, XamlInterop.IBorder_put_BorderThickness, 0.0, 0.0, 0.0, 1.0)
        element.borderColor = gridLineColor()

        val row = Row(cellValues, element, cellBorders, cells)
        rows += row

        if (sortDirection == null) {
            // If unsorted, just append to the end (avoids a full rebuild)
            itemVector.call(FoundationInterop.IVector_Append, element.uiElement.ptr)
            displayToModel = displayToModel + (rows.size - 1)
        } else {
            rebuildItems(selectedRow)
        }
    }

    /** Returns a cell's value. */
    fun getValueAt(row: Int, column: Int): String = rows[row].values[column]

    /** Rewrites a cell's value. Doesn't move the row's display position even while sorted (doesn't re-sort). */
    fun setValueAt(row: Int, column: Int, value: String) {
        rows[row].values[column] = value
        rows[row].cells[column].text = value
    }

    /** Removes the row at model index [row]. */
    fun removeRow(row: Int) {
        // Removing shifts later rows' model indices down by one, so fix up the position to
        // restore the selection to before doing so
        val selected = selectedRow
        val newSelected = when {
            selected == row -> -1
            selected > row -> selected - 1
            else -> selected
        }
        rows.removeAt(row)
        rebuildItems(newSelected)
    }

    /** Removes all rows. Keeps the column definitions and sort state. */
    fun removeAllRows() {
        rows.clear()
        rebuildItems(-1)
    }

    /** Sorts by [column]. Also updates the header's sort indicator (▲ / ▼). */
    fun sortBy(column: Int, direction: SortDirection) {
        require(column in columns.indices) { "column out of range: $column" }
        val selected = selectedRow
        sortedColumn = column
        sortDirection = direction
        updateHeaderIndicators()
        rebuildItems(selected)
    }

    /** Clears the sort, returning to insertion order. */
    fun clearSort() {
        val selected = selectedRow
        sortedColumn = -1
        sortDirection = null
        updateHeaderIndicators()
        rebuildItems(selected)
    }

    /** Subscribes to row-selection changes. Backed by subscribing to Selector.SelectionChanged. */
    fun addRowSelectionListener(listener: () -> Unit) {
        val token = selector.addEventHandler(
            "WinUI4K.SelectionChangedHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ISelector_add_SelectionChanged,
        ) { _, _ -> if (!isRebuilding) listener() }
        selectionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addRowSelectionListener]. */
    fun removeRowSelectionListener(listener: () -> Unit) {
        val token = selectionTokens.remove(listener) ?: return
        selector.removeEventHandler(XamlInterop.ISelector_remove_SelectionChanged, token)
    }

    /**
     * Overrides ListViewItem's default style (padding and centering) for table use.
     * A Style object can't be built from code, so this is generated via XamlReader.
     */
    private fun putItemContainerStyle() {
        val statics = Activation.factory(XamlInterop.CLS_XamlReader, XamlInterop.IID_IXamlReaderStatics)
        val loaded = Hstring.use(ITEM_CONTAINER_STYLE_XAML) { h ->
            statics.getPtr(XamlInterop.IXamlReaderStatics_Load, h)
        }
        statics.release()
        val style = loaded.queryInterface(XamlInterop.IID_IStyle)
        loaded.release()
        itemsControl.call(XamlInterop.IItemsControl_put_ItemContainerStyle, style.ptr)
        style.release()
    }

    /**
     * Gives the header's column buttons a flat look matching the real TableViewColumnHeader
     * (transparent background, no border, no rounded corners, left-aligned, SemiBold). The
     * theme paints the hover background.
     */
    private fun styleHeaderButton(button: WButton) {
        button.horizontalAlignment = HorizontalAlignment.STRETCH
        button.verticalAlignment = VerticalAlignment.STRETCH
        val buttonControl = button.inspectable.queryInterface(XamlInterop.IID_IControl)
        val transparent = WColor(0, 0, 0, 0).createBrush()
        buttonControl.call(XamlInterop.IControl_put_Background, transparent.ptr)
        transparent.release()
        XamlStructs.putThickness(buttonControl, XamlInterop.IControl_put_BorderThickness, 0.0, 0.0, 0.0, 0.0)
        XamlStructs.putThickness(buttonControl, XamlInterop.IControl_put_Padding, 12.0, 8.0, 12.0, 8.0)
        XamlStructs.putCornerRadius(buttonControl, XamlInterop.IControl_put_CornerRadius, 0.0)
        buttonControl.call(XamlInterop.IControl_put_HorizontalContentAlignment, HorizontalAlignment.LEFT.native)
        XamlStructs.putFontWeight(buttonControl, XamlInterop.IControl_put_FontWeight, 600)
        buttonControl.release()
    }

    /** Wraps a cell in a Border that draws a vertical grid line (right edge, 1px; the last column skips it, leaving it to the outer border). */
    private fun buildCellBorder(child: WComponent, isLast: Boolean): WBorder {
        val border = WBorder(child)
        if (!isLast) {
            XamlStructs.putThickness(border.inspectable, XamlInterop.IBorder_put_BorderThickness, 0.0, 0.0, 1.0, 0.0)
        }
        return border
    }

    /** Whether the current theme (FrameworkElement.ActualTheme) is dark. */
    private fun isDarkTheme(): Boolean =
        frameworkElement.getInt(XamlInterop.IFrameworkElement_get_ActualTheme) == 2 // ElementTheme.Dark

    /** The current theme's grid-line / outer-border color. */
    private fun gridLineColor(): WColor = if (isDarkTheme()) DARK_GRID_LINE else LIGHT_GRID_LINE

    /** Repaints the header background, grid lines, and outer border to match the current theme. */
    private fun applyTheme() {
        val gridLine = gridLineColor()
        val headerBackground = if (isDarkTheme()) DARK_HEADER_BACKGROUND else LIGHT_HEADER_BACKGROUND

        val outerBrush = gridLine.createBrush()
        control.call(XamlInterop.IControl_put_BorderBrush, outerBrush.ptr)
        outerBrush.release()

        headerRowBorder.background = headerBackground
        headerRowBorder.borderColor = gridLine
        for (border in headerCellBorders) border.borderColor = gridLine
        for (row in rows) {
            row.element.borderColor = gridLine
            for (border in row.cellBorders) border.borderColor = gridLine
        }
    }

    /**
     * Cycles the sort on a header click (matching the real TableViewColumnHeader.GetNextSortDirection):
     * unsorted/other column -> ascending -> descending -> cleared.
     */
    private fun onHeaderClicked(column: Int) {
        if (!canSortColumns || !columns[column].canSort) return
        val next = when {
            sortedColumn != column -> SortDirection.ASCENDING
            sortDirection == SortDirection.ASCENDING -> SortDirection.DESCENDING
            else -> null
        }
        if (next == null) clearSort() else sortBy(column, next)
    }

    /** Re-appends the sort indicator (▲ / ▼) to the end of the header's title. */
    private fun updateHeaderIndicators() {
        columns.forEachIndexed { index, column ->
            val suffix = if (index == sortedColumn) {
                if (sortDirection == SortDirection.ASCENDING) " ▲" else " ▼"
            } else {
                ""
            }
            headerButtons[index].text = column.title + suffix
        }
    }

    /**
     * Reorders the ListView's items with the current sort applied, and restores the
     * selection at [selectedModel] (a model index; -1 means none selected).
     */
    private fun rebuildItems(selectedModel: Int) {
        isRebuilding = true
        try {
            displayToModel = computeDisplayOrder()
            itemVector.call(FoundationInterop.IVector_Clear)
            for (modelIndex in displayToModel) {
                itemVector.call(FoundationInterop.IVector_Append, rows[modelIndex].element.uiElement.ptr)
            }
            selector.call(XamlInterop.ISelector_put_SelectedIndex, displayToModel.indexOf(selectedModel))
        } finally {
            isRebuilding = false
        }
    }

    /** Computes the display order (-> model row index) from the current sort state. */
    private fun computeDisplayOrder(): List<Int> {
        val indices = rows.indices.toList()
        val column = sortedColumn
        if (column < 0 || sortDirection == null) return indices
        val ascending = indices.sortedWith { a, b ->
            compareCells(rows[a].values[column], rows[b].values[column])
        }
        return if (sortDirection == SortDirection.DESCENDING) ascending.reversed() else ascending
    }

    /** Compares two cell values: numerically if both parse as numbers, otherwise as strings. */
    private fun compareCells(a: String, b: String): Int {
        val x = a.toDoubleOrNull()
        val y = b.toDoubleOrNull()
        return if (x != null && y != null) x.compareTo(y) else a.compareTo(b)
    }

    private companion object {
        /**
         * ListViewItem's ItemContainerStyle. Removes the default left/right padding so the
         * grid lines reach the table's edges, and matches the row height to the real
         * control's default.
         */
        val ITEM_CONTAINER_STYLE_XAML = """
            <Style xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
                   TargetType="ListViewItem">
                <Setter Property="Padding" Value="0" />
                <Setter Property="MinHeight" Value="40" />
                <Setter Property="HorizontalContentAlignment" Value="Stretch" />
                <Setter Property="VerticalContentAlignment" Value="Stretch" />
            </Style>
        """.trimIndent()
    }
}
