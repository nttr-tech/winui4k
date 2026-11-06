package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.table.DefaultTableModel
import com.appkitbox.winui4k.table.RowFilter
import com.appkitbox.winui4k.table.TableModel
import com.appkitbox.winui4k.table.TableRowSorter
import com.appkitbox.winui4k.table.TableStringConverter
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies filtering when TableRowSorter.stringConverter is set on a shown WTable
 * (the conversion itself is in TableStringConverterTest, which does not show anything). The form seen from Java is in WTableForJavaTest.
 */
@Tags("E2E")
class WTableStringConverterTest : FunSpec() {
    private fun productModel() = DefaultTableModel(
        listOf(listOf("Apple", 150), listOf("Orange", 80), listOf("Grape", 480)),
        listOf("Product", "Price"),
    )

    /** Shows the table of [model], runs [block] on the UI thread, and then removes it. */
    private fun <T> shown(model: TableModel, block: (WTable) -> T): T {
        val table = onUiThreadGet { WTable(model) }
        UiTestHarness.attachAndAwaitLoaded(table)
        try {
            return onUiThreadGet { block(table) }
        } finally {
            UiTestHarness.detach(table)
        }
    }

    init {
        context("Reading and writing") {
            test("setting stringConverter immediately refilters the shown table with its filter (matching the converted strings)") {
                val model = productModel()
                shown(model) { table ->
                    val sorter = TableRowSorter<TableModel>(model)
                    table.rowSorter = sorter
                    sorter.setRowFilter(RowFilter.regexFilter("^¥[0-9]{3}$", 1))
                    val before = table.rowCount
                    sorter.stringConverter = object : TableStringConverter() {
                        override fun toString(model: TableModel, row: Int, column: Int): String = "¥${model.getValueAt(row, column)}"
                    }
                    listOf(before, table.rowCount, table.convertRowIndexToModel(0), table.convertRowIndexToModel(1))
                } shouldBe listOf(0, 2, 0, 2) // ¥150 and ¥480 (¥80 has 2 digits)
            }

            test("without a filter, setting stringConverter does not change the rows") {
                val model = productModel()
                shown(model) { table ->
                    val sorter = TableRowSorter<TableModel>(model)
                    table.rowSorter = sorter
                    sorter.stringConverter = object : TableStringConverter() {
                        override fun toString(model: TableModel, row: Int, column: Int): String = ""
                    }
                    table.rowCount
                } shouldBe 3
            }
        }
    }
}
