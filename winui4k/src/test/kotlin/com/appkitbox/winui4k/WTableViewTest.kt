package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.table.DefaultTableColumnModel
import com.appkitbox.winui4k.table.DefaultTableModel
import com.appkitbox.winui4k.table.RowFilter
import com.appkitbox.winui4k.table.RowSorter
import com.appkitbox.winui4k.table.SortOrder
import com.appkitbox.winui4k.table.TableCellRenderer
import com.appkitbox.winui4k.table.TableColumn
import com.appkitbox.winui4k.table.TableModel
import com.appkitbox.winui4k.table.TableRowSorter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Tests WTableView (TableView) by actually displaying it and verifying integration with the model, selection, sorting,
 * filtering, grouping, column operations, and renderers.
 */
class WTableViewTest : FunSpec() {
    /** A model of 3 columns (product name / price / in stock) × 4 rows. */
    private fun productModel(): DefaultTableModel = object : DefaultTableModel(
        listOf(
            listOf("Apple", 150, true),
            listOf("Orange", 80, false),
            listOf("Grape", 480, true),
            listOf("Peach", 320, false),
        ),
        listOf("Product", "Price", "In Stock"),
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            1 -> Integer::class.java
            2 -> java.lang.Boolean::class.java
            else -> String::class.java
        }
    }

    /** Shows [table], waits for its template to be applied, runs [block] on the UI thread, and then removes it. */
    private fun <T> withShownTable(table: WTableView, block: (WTableView) -> T): T {
        UiTestHarness.attachAndAwaitLoaded(table)
        try {
            return onUiThreadGet {
                table.updateLayout()
                block(table)
            }
        } finally {
            UiTestHarness.detach(table)
        }
    }

    /** The runtime class name of [node] (a DependencyObject) (IInspectable.GetRuntimeClassName). */
    private fun runtimeClassName(node: ComPtr): String = Ffi.backend.withScope { scope ->
        val out = scope.allocate(8)
        node.call(4, out) // IInspectable::GetRuntimeClassName
        val hstring = Ffi.backend.memory.getPtr(out, 0)
        try {
            Hstring.read(hstring)
        } finally {
            Hstring.free(hstring)
        }
    }

    /** The children of [node] (VisualTreeHelper.GetChild). The caller releases them. */
    private fun children(node: ComPtr): List<ComPtr> {
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        try {
            val count = Ffi.backend.withScope { scope ->
                val out = scope.allocate(4)
                statics.call(XamlInterop.IVisualTreeHelperStatics_GetChildrenCount, node.ptr, out)
                Ffi.backend.memory.getInt(out, 0)
            }
            return (0 until count).mapNotNull { statics.getPtrOrNull(XamlInterop.IVisualTreeHelperStatics_GetChild, node.ptr, it) }
        } finally {
            statics.release()
        }
    }

    /** The non-empty strings of the TextBlocks under [node] (in display order). */
    private fun texts(node: ComPtr): List<String> {
        val own = node.queryInterfaceOrNull(XamlInterop.IID_ITextBlock)?.let { textBlock ->
            textBlock.getString(XamlInterop.ITextBlock_get_Text).also { textBlock.release() }
        }
        return listOfNotNull(own?.takeIf { it.isNotEmpty() }) + children(node).flatMap { child -> texts(child).also { child.release() } }
    }

    /** The display strings of each group header (TableViewGroupHeader) being shown. */
    private fun groupHeaderTexts(node: ComPtr): List<List<String>> {
        if (runtimeClassName(node) == "Microsoft.UI.Xaml.Controls.Tabular.TableViewGroupHeader") return listOf(texts(node))
        return children(node).flatMap { child -> groupHeaderTexts(child).also { child.release() } }
    }

    /** Lists the values of column [column] (model) for all rows in the view from top to bottom (group header rows are "#"). */
    private fun viewValues(table: WTableView, column: Int): List<Any?> = (0 until table.rowCount).map { row ->
        val modelRow = table.convertRowIndexToModel(row)
        if (modelRow < 0) "#" else table.model.getValueAt(modelRow, column)
    }

    init {
        test("the shown table's row count, column count, cell values, and column names match the model") {
            val table = onUiThreadGet { WTableView(productModel()) }
            withShownTable(table) {
                listOf(it.rowCount, it.columnCount, it.getValueAt(1, 0), it.getColumnName(2))
            } shouldBe listOf(4, 3, "Orange", "In Stock")
        }

        test("adding and removing rows and updating values in the model are reflected in the view's rows") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                model.addRow("Melon", 900, true)
                model.removeRow(0)
                model.setValueAt(85, 0, 1)
                listOf(it.rowCount, viewValues(it, 0), it.getValueAt(0, 1))
            } shouldBe listOf(4, listOf("Orange", "Grape", "Peach", "Melon"), 85)
        }

        test("a row selected with selectRow is reflected in selectedRow and isRowSelected, and listeners are called") {
            val table = onUiThreadGet { WTableView(productModel()) }
            var notified = 0
            val result = withShownTable(table) {
                it.addRowSelectionListener { notified++ }
                it.selectRow(2)
                val selected = listOf(it.selectedRow, it.isRowSelected(2), it.isRowSelected(1), it.selectedRowCount)
                it.clearSelection()
                selected + it.selectedRow
            }
            result shouldBe listOf(2, true, false, 1, -1)
            notified shouldBe 2 // Twice: selection and deselection
        }

        test("with selectionMode set to NONE, selectRow does not select anything") {
            val table = onUiThreadGet { WTableView(productModel()) }
            withShownTable(table) {
                it.selectionMode = TableSelectionMode.NONE
                it.selectRow(1)
                it.selectionMode to it.selectedRow
            } shouldBe (TableSelectionMode.NONE to -1)
        }

        test("TableRowSorter's setSortKeys sorts by price in descending order, and view and model row indices can be converted") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                val sorter = TableRowSorter(model)
                it.rowSorter = sorter
                sorter.setSortKeys(listOf(RowSorter.SortKey(1, SortOrder.DESCENDING)))
                listOf(
                    viewValues(it, 0),
                    it.convertRowIndexToModel(0),
                    it.convertRowIndexToView(1),
                    sorter.getSortKeys(),
                    it.columnModel.getColumn(1).sortOrder,
                )
            } shouldBe listOf(
                listOf("Grape", "Peach", "Apple", "Orange"),
                2, // The first row is model row 2 (Grape)
                3, // Model row 1 (Orange) is last
                listOf(RowSorter.SortKey(1, SortOrder.DESCENDING)),
                SortOrder.DESCENDING,
            )
        }

        test("passing multiple keys to setSortKeys orders rows with the same primary key by the secondary key") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                val sorter = TableRowSorter(model)
                it.rowSorter = sorter
                val keys = listOf(RowSorter.SortKey(2, SortOrder.DESCENDING), RowSorter.SortKey(1, SortOrder.ASCENDING))
                sorter.setSortKeys(keys) // In-stock items first, then cheapest first within the same value
                val multi = listOf(viewValues(it, 0), sorter.getSortKeys())
                sorter.setSortKeys(listOf(RowSorter.SortKey(0, SortOrder.ASCENDING))) // Going back to a single key replaces the keys
                multi + listOf(viewValues(it, 0), sorter.getSortKeys())
            } shouldBe listOf(
                listOf("Apple", "Grape", "Orange", "Peach"),
                listOf(RowSorter.SortKey(2, SortOrder.DESCENDING), RowSorter.SortKey(1, SortOrder.ASCENDING)),
                listOf("Apple", "Grape", "Orange", "Peach"),
                listOf(RowSorter.SortKey(0, SortOrder.ASCENDING)),
            )
        }

        test("a column with a comparator set via setComparator is sorted with that comparator (ascending by length)") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                val sorter = TableRowSorter(model)
                sorter.setComparator(0, compareBy<String> { name -> name.length }.thenBy { name -> name })
                it.rowSorter = sorter
                sorter.setSortKeys(listOf(RowSorter.SortKey(0, SortOrder.ASCENDING)))
                viewValues(it, 0)
            } shouldBe listOf("Apple", "Grape", "Peach", "Orange")
        }

        test("clearing sorting with setSortKeys(null) returns to the model order") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                val sorter = TableRowSorter(model)
                it.rowSorter = sorter
                sorter.setSortKeys(listOf(RowSorter.SortKey(1, SortOrder.ASCENDING)))
                val sorted = viewValues(it, 0)
                sorter.setSortKeys(null)
                listOf(sorted, viewValues(it, 0), sorter.getSortKeys())
            } shouldBe listOf(
                listOf("Orange", "Apple", "Peach", "Grape"),
                listOf("Apple", "Orange", "Grape", "Peach"),
                emptyList<RowSorter.SortKey>(),
            )
        }

        test("filtering with a RowFilter shows only the matching rows, and clearing it shows all rows again") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                val sorter = TableRowSorter(model)
                it.rowSorter = sorter
                sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 200, 1))
                val filtered = listOf(it.rowCount, viewValues(it, 0), it.convertRowIndexToView(0), sorter.getViewRowCount())
                sorter.setRowFilter(null)
                filtered + it.rowCount
            } shouldBe listOf(2, listOf("Grape", "Peach"), -1, 2, 4)
        }

        test("rows added while filtering are also filtered by the filter condition") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                val sorter = TableRowSorter(model)
                it.rowSorter = sorter
                sorter.setRowFilter(RowFilter.regexFilter("^O|^M", 0))
                model.addRow("Melon", 900, true)
                model.addRow("Strawberry", 400, true)
                viewValues(it, 0)
            } shouldBe listOf("Orange", "Melon")
        }

        test("updating a value while filtering re-evaluates that row against the filter condition") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                val sorter = TableRowSorter(model)
                it.rowSorter = sorter
                sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 200, 1))
                val before = viewValues(it, 0)
                model.setValueAt(500, 1, 1) // Orange goes above 200 yen
                model.setValueAt(100, 2, 1) // Grape drops to 200 yen or less
                listOf(before, viewValues(it, 0))
            } shouldBe listOf(listOf("Grape", "Peach"), listOf("Orange", "Peach"))
        }

        test("when sortsOnUpdates is true, a row whose value was updated is sorted again") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                val sorter = TableRowSorter(model)
                sorter.sortsOnUpdates = true
                it.rowSorter = sorter
                sorter.setSortKeys(listOf(RowSorter.SortKey(1, SortOrder.ASCENDING)))
                model.setValueAt(1000, 1, 1) // Make Orange the most expensive
                viewValues(it, 0)
            } shouldBe listOf("Apple", "Peach", "Grape", "Orange")
        }

        test("groupBy groups rows with the same value, and header rows convert to model row -1") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                it.groupBy(2) // Group by in stock (true / false)
                it.updateLayout() // Actually render the group header rows (including resolving theme resources)
                val grouped = listOf(it.isGrouped, it.rowCount, viewValues(it, 0))
                it.clearGrouping()
                grouped + listOf(it.isGrouped, it.rowCount)
            } shouldBe listOf(
                true,
                6, // 2 group headers + 4 rows
                listOf("#", "Apple", "Grape", "#", "Orange", "Peach"),
                false,
                4,
            )
        }

        test("group headers show the key text and the count (the default template's {Binding KeyText} is resolved)") {
            val table = onUiThreadGet { WTableView(productModel()) }
            UiTestHarness.attachAndAwaitLoaded(table)
            try {
                onUiThread {
                    table.groupBy(2)
                    table.updateLayout()
                }
                // The bindings in the header template are evaluated after layout, so wait a little before reading
                Thread.sleep(500)
                onUiThreadGet { groupHeaderTexts(table.dependencyObject) }
            } finally {
                UiTestHarness.detach(table)
            }.map { header -> header.drop(1) } shouldBe listOf(listOf("true", "(2)"), listOf("false", "(2)")) // The first one is the expand button's icon
        }

        test("moving, removing, and adding columns are reflected in the column order and column names") {
            val table = onUiThreadGet { WTableView(productModel()) }
            withShownTable(table) {
                it.moveColumn(2, 0)
                val moved = (0 until it.columnCount).map { column -> it.getColumnName(column) }
                it.removeColumn(it.columnModel.getColumn(1))
                val removed = (0 until it.columnCount).map { column -> it.getColumnName(column) }
                val added = TableColumn(1)
                added.headerValue = "Price (excl. tax)"
                it.addColumn(added)
                listOf(moved, removed, it.columnCount, it.convertColumnIndexToModel(2), it.getColumn("Price (excl. tax)") === added)
            } shouldBe listOf(
                listOf("In Stock", "Product", "Price"),
                listOf("In Stock", "Price"),
                3,
                1,
                true,
            )
        }

        test("a table created with a column model shows only the given columns in that order (columns are not created automatically)") {
            val table = onUiThreadGet {
                val columns = DefaultTableColumnModel()
                columns.addColumn(TableColumn(2).also { it.headerValue = "Stock" })
                columns.addColumn(TableColumn(0))
                WTableView(productModel(), columns)
            }
            withShownTable(table) {
                listOf(it.autoCreateColumnsFromModel, it.columnCount, it.getColumnName(1), it.columnModel.getColumn(0).headerValue)
            } shouldBe listOf(false, 2, "Product", "Stock")
        }

        test("changing the model's column structure recreates the columns from the model") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                model.addColumn("Origin", listOf("Aomori", "Ehime", "Yamanashi", "Yamanashi"))
                listOf(it.columnCount, it.getColumnName(3), it.getValueAt(2, 3))
            } shouldBe listOf(4, "Origin", "Yamanashi")
        }

        test("column properties are reflected in the displayed native columns, and actualWidth returns the actual width") {
            val table = onUiThreadGet { WTableView(productModel()) }
            withShownTable(table) {
                val column = it.columnModel.getColumn(0)
                column.preferredWidth = 200.0
                it.updateLayout()
                column.width
            } shouldBe 200.0
        }

        test("for a column with a cellRenderer, the renderer is called with the values of the displayed rows") {
            val rendered = mutableListOf<Pair<Any?, Int>>()
            val table = onUiThreadGet {
                val view = WTableView(productModel())
                view.columnModel.getColumn(1).cellRenderer = TableCellRenderer { _, value, _, _, row, _ ->
                    rendered += value to row
                    WLabel("¥$value")
                }
                view
            }
            withShownTable(table) { }
            rendered.toSet() shouldContainAll listOf(150 to 0, 80 to 1, 480 to 2, 320 to 3)
        }

        test("a renderer column is redrawn by the renderer when the value of a displayed cell changes") {
            val model = productModel()
            val rendered = mutableListOf<Any?>()
            val table = onUiThreadGet {
                val view = WTableView(model)
                view.columnModel.getColumn(1).cellRenderer = TableCellRenderer { _, value, _, _, _, _ ->
                    rendered += value
                    WLabel(value.toString())
                }
                view
            }
            UiTestHarness.attachAndAwaitLoaded(table)
            try {
                onUiThread {
                    table.updateLayout()
                    rendered.clear()
                    model.setValueAt(999, 2, 1) // Grape's price
                    table.updateLayout()
                }
                Thread.sleep(300)
            } finally {
                UiTestHarness.detach(table)
            }
            rendered shouldContain 999
        }

        test("setDefaultRenderer is used as the renderer for the column's type") {
            val renderedValues = mutableListOf<Any?>()
            val table = onUiThreadGet {
                val view = WTableView(productModel())
                view.setDefaultRenderer(Integer::class.java) { _, value, _, _, _, _ ->
                    renderedValues += value
                    WLabel(value.toString())
                }
                view
            }
            withShownTable(table) { }
            renderedValues.toSet() shouldBe setOf(150, 80, 480, 320)
        }

        test("the column header template and the display when there are no rows (emptyText) are rendered") {
            val model = DefaultTableModel(listOf("Product"), 0)
            val table = onUiThreadGet {
                val view = WTableView(model)
                view.columnModel.getColumn(0).headerTemplate =
                    "<StackPanel Orientation=\"Horizontal\"><TextBlock Text=\"★\" /><TextBlock Text=\"{Binding}\" /></StackPanel>"
                view.emptyText = "No products"
                view
            }
            UiTestHarness.attachAndAwaitLoaded(table)
            try {
                onUiThread { table.updateLayout() }
                Thread.sleep(300)
                onUiThreadGet { texts(table.dependencyObject) }
            } finally {
                UiTestHarness.detach(table)
            } shouldContainAll listOf("★", "Product", "No products")
        }

        test("appearance properties return the values that were set") {
            val table = onUiThreadGet { WTableView(productModel()) }
            withShownTable(table) {
                it.showVerticalLines = false
                it.isHeaderVisible = false
                it.density = TableDensity.COMPACT
                it.canUserResizeColumns = false
                it.isReadOnly = true
                listOf(it.showHorizontalLines, it.showVerticalLines, it.isHeaderVisible, it.density, it.canUserResizeColumns, it.isReadOnly)
            } shouldBe listOf(true, false, false, TableDensity.COMPACT, false, true)
        }

        test("values written by the binding when an edit is committed are converted to the column type and written back to the model, and inputs that cannot be converted are not written back") {
            val model = productModel()
            val table = onUiThreadGet { WTableView(model) }
            withShownTable(table) {
                // Insert into the displayed row object, just like a binding write-back (TwoWay UpdateSource)
                val rowsField = WTableView::class.java.getDeclaredField("rows").also { field -> field.isAccessible = true }
                val rows = rowsField.get(it) as TableRowCollection
                val map = ComPtr(rows[1].comObject.primary)
                fun write(key: String, value: Any?) {
                    val boxed = PropertyValues.boxAny(value)
                    Hstring.use(key) { h ->
                        Ffi.backend.withScope { scope -> map.call(FoundationInterop.IMap_Insert, h, boxed?.ptr, scope.allocate(1, 1)) }
                    }
                    boxed?.release()
                }
                write("c1", " 95 ") // Text box string → Integer column
                val converted = model.getValueAt(1, 1)
                write("c1", "abc") // Non-numeric input is not written back
                write("c2", true) // Check box Boolean → Boolean column
                listOf(converted, model.getValueAt(1, 1), model.getValueAt(1, 2))
            } shouldBe listOf(95, 95, true)
        }

        test("when not editing, isEditing is false and committing and canceling return false") {
            val table = onUiThreadGet { WTableView(productModel()) }
            withShownTable(table) {
                listOf(it.isEditing, it.stopCellEditing(), it.cancelCellEditing(), it.editingRow, it.editingColumn)
            } shouldBe listOf(false, false, false, -1, -1)
        }

        test("replacing the model shows the new model's rows and columns") {
            val table = onUiThreadGet { WTableView(productModel()) }
            withShownTable(table) {
                it.model = DefaultTableModel(listOf(listOf("Tokyo", 1400)), listOf("City", "Population (10k)"))
                listOf(it.rowCount, it.columnCount, it.getValueAt(0, 0), it.getColumnName(1))
            } shouldBe listOf(1, 2, "Tokyo", "Population (10k)")
        }

        test("setting autoCreateRowSorter to true creates a TableRowSorter") {
            val model = productModel()
            onUiThreadGet {
                val table = WTableView(model)
                table.autoCreateRowSorter = true
                (table.rowSorter as TableRowSorter<*>).getModel() === model
            } shouldBe true
        }

        test("view and model row indices can be converted with filtering and sorting taken into account even before display") {
            val model: TableModel = productModel()
            onUiThreadGet {
                val table = WTableView(model)
                val sorter = TableRowSorter(model)
                table.rowSorter = sorter
                sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.NOT_EQUAL, 80, 1))
                listOf(table.rowCount, table.convertRowIndexToModel(1), table.convertRowIndexToView(1))
            } shouldBe listOf(3, 2, -1)
        }
    }
}
