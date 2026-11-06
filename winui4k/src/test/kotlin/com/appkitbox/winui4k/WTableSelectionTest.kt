package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.table.DefaultCellEditor
import com.appkitbox.winui4k.table.DefaultTableCellRenderer
import com.appkitbox.winui4k.table.DefaultTableModel
import com.appkitbox.winui4k.table.RowFilter
import com.appkitbox.winui4k.table.RowSorter
import com.appkitbox.winui4k.table.SortOrder
import com.appkitbox.winui4k.table.TableRowSorter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies WTable's JTable-style selection (sync with selectionModel, interval selection, changeSelection, column and cell selection),
 * the array constructor, getColumn by column name, and rowAtPoint / columnAtPoint by actually showing the table.
 * Expected values come from Swing's JTable behavior (TableView can select only one row, so intervals are shrunk to the single lead row).
 */
class WTableSelectionTest : FunSpec() {
    /** A model of 2 columns (Product / Price) × 4 rows. */
    private fun productModel(): DefaultTableModel = object : DefaultTableModel(
        listOf(listOf("Apple", 150), listOf("Orange", 80), listOf("Grape", 480), listOf("Peach", 320)),
        listOf("Product", "Price"),
    ) {
        override fun getColumnClass(columnIndex: Int): Class<*> = if (columnIndex == 1) Integer::class.java else String::class.java
    }

    /** Shows [table], waits for the template to be applied, runs [block] on the UI thread, and then removes it. */
    private fun <T> withShownTable(table: WTable, block: (WTable) -> T): T {
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

    private fun shown(block: (WTable) -> Any?): Any? = withShownTable(onUiThreadGet { WTable(productModel()) }, block)

    /** The table coordinates of the center of the TextBlock of the cell whose text is [text]. */
    private fun centerOf(table: WTable, text: String): Pair<Double, Double> {
        val block = checkNotNull(tableTreeFindText(table.dependencyObject, text)) { "TextBlock not found: $text" }
        return try {
            val (width, height) = VisualHitTest.actualSizeOf(block)
            VisualHitTest.transform(block, table.uiElement, width / 2, height / 2)
        } finally {
            block.release()
        }
    }

    init {
        test("the array constructor (JTable(Object[][], Object[])) fills in the rows and column names") {
            onUiThreadGet {
                val table = WTable(arrayOf(arrayOf<Any?>("Apple", 3), arrayOf<Any?>("Orange", 5)), arrayOf<Any?>("Name", "Count"))
                listOf(table.rowCount, table.columnCount, table.getValueAt(1, 0), table.getColumnName(1))
            } shouldBe listOf(2, 2, "Orange", "Count")
        }

        test("with the constructor taking a selection model, that model becomes selectionModel and the columns are created from the model") {
            onUiThreadGet {
                val selection = WDefaultListSelectionModel()
                val table = WTable(productModel(), null, selection)
                (table.selectionModel === selection) to table.columnCount
            } shouldBe (true to 2)
        }

        test("the default selectionModel is single selection, display-side selection is copied to the model, and the model's selection is copied to the display") {
            shown { table ->
                val mode = table.selectionModel.getSelectionMode()
                table.selectRow(2)
                val fromNative = table.selectionModel.getSelectedIndices().toList() to table.selectionModel.getLeadSelectionIndex()
                table.selectionModel.setSelectionInterval(1, 1)
                listOf(mode, fromNative, table.selectedRow)
            } shouldBe listOf(WListSelectionModel.SINGLE_SELECTION, listOf(2) to 2, 1)
        }

        test("selecting an interval shrinks it to the single lead row (also when a multiple interval model is set)") {
            shown { table ->
                table.setRowSelectionInterval(1, 3)
                val single = table.selectedRows.toList() to table.selectionModel.getSelectedIndices().toList()
                val multiple = WDefaultListSelectionModel()
                table.selectionModel = multiple
                multiple.setSelectionInterval(0, 2)
                listOf(single, table.selectedRow to multiple.getSelectedIndices().toList())
            } shouldBe listOf(listOf(3) to listOf(3), 2 to listOf(2))
        }

        test("setSelectionMode clears the selection first, as JTable does") {
            shown { table ->
                table.setRowSelectionInterval(2, 2)
                table.setSelectionMode(WListSelectionModel.SINGLE_SELECTION)
                listOf(table.selectedRow, table.selectionModel.isSelectionEmpty())
            } shouldBe listOf(-1, true)
        }

        test("changeSelection for an out-of-range row does not change the selection or move lead, as with JTable's out-of-range rows") {
            shown { table ->
                table.changeSelection(1, 0, toggle = false, extend = false)
                table.changeSelection(10, 0, toggle = false, extend = false)
                table.changeSelection(-1, 0, toggle = true, extend = false)
                table.changeSelection(7, 0, toggle = false, extend = true)
                listOf(table.selectedRow, table.selectionModel.getLeadSelectionIndex(), table.selectionModel.getAnchorSelectionIndex())
            } shouldBe listOf(1, 1, 1)
        }

        test("setSelectionMode makes it single selection for any constant, and a non-constant throws IllegalArgumentException") {
            shown { table ->
                table.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
                shouldThrow<IllegalArgumentException> { table.setSelectionMode(5) }
                table.selectionModel.getSelectionMode()
            } shouldBe WListSelectionModel.SINGLE_SELECTION
        }

        test("adding and removing row intervals; an out-of-range row throws IllegalArgumentException (as in Swing)") {
            shown { table ->
                table.addRowSelectionInterval(2, 2)
                val added = table.selectedRow
                table.removeRowSelectionInterval(2, 2)
                val removed = table.selectedRow
                shouldThrow<IllegalArgumentException> { table.setRowSelectionInterval(0, 4) }
                shouldThrow<IllegalArgumentException> { table.addRowSelectionInterval(-1, 0) }
                listOf(added, removed)
            } shouldBe listOf(2, -1)
        }

        test("changeSelection changes the selection with combinations of toggle and extend, as in Swing") {
            shown { table ->
                table.changeSelection(1, 0, toggle = false, extend = false)
                val plain = table.selectedRow
                table.changeSelection(1, 0, toggle = true, extend = false)
                val toggledOff = table.selectedRow
                table.changeSelection(2, 0, toggle = true, extend = false)
                val toggledOn = table.selectedRow
                table.changeSelection(3, 0, toggle = false, extend = true)
                listOf(plain, toggledOff, toggledOn, table.selectedRow)
            } shouldBe listOf(1, -1, 2, 3)
        }

        test("setting rowSelectionAllowed to false makes it non-selectable, and true returns it to single selection") {
            shown { table ->
                table.rowSelectionAllowed = false
                table.selectRow(1)
                val disallowed = listOf(table.selectionMode, table.selectedRow, table.isCellSelected(1, 0))
                table.rowSelectionAllowed = true
                table.selectRow(1)
                disallowed + listOf(table.selectionMode, table.isCellSelected(1, 0), table.isCellSelected(1, 1))
            } shouldBe listOf(TableSelectionMode.NONE, -1, false, TableSelectionMode.SINGLE, true, true)
        }

        test("there is no column or cell selection, and it stays false even when column selection is allowed") {
            shown { table ->
                table.selectRow(0)
                table.columnSelectionAllowed = true
                table.cellSelectionEnabled = true
                listOf(
                    table.selectedColumn,
                    table.selectedColumns.size,
                    table.selectedColumnCount,
                    table.isColumnSelected(0),
                    table.columnSelectionAllowed,
                    table.cellSelectionEnabled,
                )
            } shouldBe listOf(-1, 0, 0, false, false, false)
        }

        test("getColumn looks up columns created from the model by column name, and the column's headerValue stays null. An unknown name throws IllegalArgumentException") {
            onUiThreadGet {
                val table = WTable(productModel())
                val column = table.getColumn("Price")
                shouldThrow<IllegalArgumentException> { table.getColumn("Stock") }
                column.modelIndex to column.headerValue
            } shouldBe (1 to null)
        }

        test("the same model row stays selected after sorting (as in Swing)") {
            val model = productModel()
            withShownTable(onUiThreadGet { WTable(model) }) { table ->
                table.selectRow(1) // Orange
                val sorter = TableRowSorter(model)
                table.rowSorter = sorter
                sorter.setSortKeys(listOf(RowSorter.SortKey(1, SortOrder.DESCENDING)))
                listOf(table.selectedRow, table.convertRowIndexToModel(table.selectedRow), table.selectionModel.getLeadSelectionIndex())
            } shouldBe listOf(3, 1, 3) // In descending order of price, Orange (80) is last
        }

        test("filtering adjusts the position if the selected row remains, and clears the selection if it disappears") {
            val model = productModel()
            withShownTable(onUiThreadGet { WTable(model) }) { table ->
                val sorter = TableRowSorter(model)
                table.rowSorter = sorter
                table.selectRow(3) // Peach (320)
                sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 200, 1))
                val kept = table.selectedRow to table.getValueAt(table.selectedRow, 0)
                sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 400, 1))
                listOf(kept, table.selectedRow to table.selectionModel.isSelectionEmpty())
            } shouldBe listOf(1 to "Peach", -1 to true)
        }

        test("adding a row in front shifts the selection, and removing the selected row clears the selection (as in Swing)") {
            val model = productModel()
            withShownTable(onUiThreadGet { WTable(model) }) { table ->
                table.selectRow(1)
                model.insertRow(0, listOf("Pear", 100))
                val shifted = table.selectedRow to table.getValueAt(table.selectedRow, 0)
                model.removeRow(2)
                listOf(shifted, table.selectedRow to table.selectionModel.isSelectionEmpty())
            } shouldBe listOf(2 to "Orange", -1 to true)
        }

        test("when filtering or removing rows clears the selection, selectedRow read from a Runnable selection listener is -1") {
            val model = productModel()
            val seen = mutableListOf<Int>()
            withShownTable(onUiThreadGet { WTable(model) }) { table ->
                val sorter = TableRowSorter(model)
                table.rowSorter = sorter
                table.selectRow(3) // Peach (320)
                table.addRowSelectionListener { seen += table.selectedRow }
                sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 400, 1)) // Only Grape remains
                val filtered = seen.toList()
                sorter.setRowFilter(null)
                table.selectRow(1) // Orange
                seen.clear()
                model.removeRow(1)
                listOf(filtered, seen.toList(), table.selectedRow)
            } shouldBe listOf(listOf(-1), listOf(-1), -1)
        }

        test("even if a listener after sorting throws, later selections in the display are copied to the model") {
            val model = productModel()
            withShownTable(onUiThreadGet { WTable(model) }) { table ->
                val sorter = TableRowSorter(model)
                table.rowSorter = sorter
                table.addSortedListener { error("Failure of the listener after sorting") }
                runCatching { sorter.setSortKeys(listOf(RowSorter.SortKey(1, SortOrder.ASCENDING))) }
                table.selectRow(2)
                listOf(table.selectedRow, table.selectionModel.getMinSelectionIndex())
            } shouldBe listOf(2, 2)
        }

        test("rowAtPoint and columnAtPoint return the row and column from a cell position, and -1 for the column header row and outside the table") {
            shown { table ->
                val (grapeX, grapeY) = centerOf(table, "Grape")
                val (priceX, priceY) = centerOf(table, "480")
                val (headerX, headerY) = centerOf(table, "Price")
                listOf(
                    table.rowAtPoint(grapeX, grapeY) to table.columnAtPoint(grapeX, grapeY),
                    table.rowAtPoint(priceX, priceY) to table.columnAtPoint(priceX, priceY),
                    table.rowAtPoint(headerX, headerY) to table.columnAtPoint(headerX, headerY),
                    table.rowAtPoint(-5.0, -5.0) to table.columnAtPoint(-5.0, -5.0),
                    table.rowAtPoint(grapeX, 100_000.0) to table.columnAtPoint(100_000.0, grapeY),
                )
            } shouldBe listOf(2 to 0, 2 to 1, -1 to 1, -1 to -1, -1 to -1)
        }

        test("the row selection listener (Runnable) is called only once for one interval selection") {
            var calls = 0
            shown { table ->
                table.addRowSelectionListener { calls++ }
                table.setRowSelectionInterval(0, 2)
                table.selectedRow
            } shouldBe 2
            calls shouldBe 1
        }

        context("Swing compatibility") {
            test("selectAll selects from row 0 to the last row and the last row ends up selected, as with a single-selection JTable, and does nothing for a table without rows") {
                shown { table ->
                    table.selectAll()
                    listOf(table.selectedRow, table.selectionModel.getLeadSelectionIndex())
                } shouldBe listOf(3, 3)
                withShownTable(onUiThreadGet { WTable(DefaultTableModel(listOf("Product"), 0)) }) { table ->
                    table.selectAll()
                    table.selectedRow
                } shouldBe -1
            }

            test("the WPoint versions of rowAtPoint and columnAtPoint (JTable's Point versions) return the same row and column as the Double versions at the same position") {
                shown { table ->
                    val (x, y) = centerOf(table, "480")
                    val point = WPoint(x.toInt(), y.toInt())
                    listOf(table.rowAtPoint(point), table.columnAtPoint(point), table.rowAtPoint(WPoint(-5, -5)))
                } shouldBe listOf(2, 1, -1)
            }

            test("getCellRenderer and getCellEditor return the column's own first, otherwise the default for the column's type, and null if neither exists") {
                onUiThreadGet {
                    val table = WTable(productModel())
                    val columnRenderer = DefaultTableCellRenderer()
                    val integerRenderer = DefaultTableCellRenderer()
                    val integerEditor = DefaultCellEditor(WSpinner())
                    table.getColumn("Product").cellRenderer = columnRenderer
                    table.setDefaultRenderer(Integer::class.java, integerRenderer)
                    table.setDefaultEditor(Integer::class.java, integerEditor)
                    val before = listOf(table.getCellRenderer(0, 0) === columnRenderer, table.getCellRenderer(3, 1) === integerRenderer)
                    table.getColumn("Product").cellRenderer = null
                    before + listOf(
                        table.getCellRenderer(0, 0) == null,
                        table.getCellEditor(1, 1) === integerEditor,
                        table.getCellEditor(1, 0) == null,
                        table.getDefaultEditor(Integer::class.java) === integerEditor,
                    )
                } shouldBe listOf(true, true, true, true, true, true)
            }
        }
    }
}
