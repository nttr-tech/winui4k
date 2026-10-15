package com.appkitbox.winui4k

import com.appkitbox.winui4k.TableTestSupport.awaitNotifications
import com.appkitbox.winui4k.TableTestSupport.cellWidths
import com.appkitbox.winui4k.TableTestSupport.findTextBlock
import com.appkitbox.winui4k.TableTestSupport.productModel
import com.appkitbox.winui4k.TableTestSupport.rowTexts
import com.appkitbox.winui4k.TableTestSupport.texts
import com.appkitbox.winui4k.TableTestSupport.withShownTable
import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.table.AbstractTableModel
import com.appkitbox.winui4k.table.DefaultTableModel
import com.appkitbox.winui4k.table.TableCellRenderer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
/**
 * Tests the cells and rows of a shown WTable as they appear on screen: rows kept across a reattachment, double-taps on
 * every kind of column, the values of reused template cells, and the widths of rebuilt columns.
 */
class WTableCellsTest : FunSpec() {
    init {
        test("a table removed and added again twice in one UI turn still shows its rows and keeps the selected row") {
            val table = onUiThreadGet { WTable(productModel()) }
            val host = onUiThreadGet { WGrid().also { it.add(table) } }
            UiTestHarness.attachAndAwaitLoaded(host)
            try {
                onUiThread {
                    table.updateLayout()
                    table.selectRow(2)
                    // The new Loaded arrives before the old Unloaded (like a page shown again in a reopened popup)
                    host.remove(table)
                    host.add(table)
                    host.remove(table)
                    host.add(table)
                }
                Thread.sleep(500)
                onUiThreadGet {
                    table.updateLayout()
                    texts(table.dependencyObject).filter { it in setOf("Apple", "Orange", "Grape", "Peach") }.toSet() to table.selectedRow
                }
            } finally {
                UiTestHarness.detach(host)
            } shouldBe (setOf("Apple", "Orange", "Grape", "Peach") to 2)
        }

        test("double-tapping a cell of a text, renderer, or XAML template column notifies the row the cell is in") {
            val invoked = mutableListOf<Int>()
            val table = onUiThreadGet {
                val model = DefaultTableModel(
                    listOf(listOf("Apple", "A-1", "apple note"), listOf("Grape", "G-2", "grape note")),
                    listOf("Name", "Code", "Note"),
                )
                WTable(model).also {
                    it.isReadOnly = true // A double-tap on an editable cell starts editing instead
                    it.columnModel.getColumn(1).cellRenderer = TableCellRenderer { _, value, _, _, _, _ -> WLabel("code $value") }
                    it.columnModel.getColumn(2).cellTemplate = "<TextBlock Text=\"{Binding}\" />"
                    it.addRowInvokedListener { row -> invoked += row }
                }
            }
            UiTestHarness.attachAndAwaitLoaded(table)
            try {
                onUiThread { table.updateLayout() }
                listOf("Grape", "code G-2", "grape note").associateWith { text ->
                    onUiThread { invoked.clear() }
                    val cell = checkNotNull(onUiThreadGet { findTextBlock(table.dependencyObject, text) }) { "TextBlock not found: $text" }
                    try {
                        TouchInput.doubleTap(cell)
                    } finally {
                        onUiThread { cell.release() }
                    }
                    awaitNotifications { invoked.toList() }
                }
            } finally {
                UiTestHarness.detach(table)
            } shouldBe mapOf("Grape" to listOf(1), "code G-2" to listOf(1), "grape note" to listOf(1))
        }

        test("a XAML template column shows the current values after rows are removed, inserted, and updated") {
            val ids = mutableListOf(0, 1, 2, 3)
            val model = object : AbstractTableModel() {
                override fun getRowCount(): Int = ids.size

                override fun getColumnCount(): Int = 2

                override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? = (if (columnIndex == 0) "name " else "value ") + ids[rowIndex]
            }
            val table = onUiThreadGet {
                WTable(model).also { it.columnModel.getColumn(1).cellTemplate = "<TextBlock Text=\"{Binding}\" />" }
            }

            // Runs change and returns the texts of each shown row, together with the texts each row should show
            fun shownAfter(change: () -> Unit): Pair<Map<Int, List<String>>, Map<Int, List<String>>> {
                onUiThread(change)
                Thread.sleep(300)
                return onUiThreadGet {
                    table.updateLayout()
                    rowTexts(table) to ids.indices.associateWith { listOf("name ${ids[it]}", "value ${ids[it]}") }
                }
            }
            UiTestHarness.attachAndAwaitLoaded(table)
            try {
                listOf(
                    shownAfter { },
                    shownAfter {
                        ids.removeAt(0)
                        model.fireTableRowsDeleted(0, 0)
                    },
                    shownAfter {
                        ids.add(0, 9)
                        model.fireTableRowsInserted(0, 0)
                    },
                    shownAfter {
                        ids.replaceAll { it + 10 }
                        model.fireTableRowsUpdated(0, ids.size - 1)
                    },
                    shownAfter {
                        ids.replaceAll { it + 10 }
                        model.fireTableDataChanged()
                    },
                )
            } finally {
                UiTestHarness.detach(table)
            }.forEach { (shown, expected) -> shown shouldBe expected }
        }

        test("the widths of columns given a renderer or a XAML template after they were added are laid out") {
            val table = onUiThreadGet {
                WTable(productModel()).also {
                    it.columnModel.getColumn(1).apply {
                        cellRenderer = TableCellRenderer { _, value, _, _, _, _ -> WLabel("¥$value") }
                        preferredWidth = 200.0
                    }
                    it.columnModel.getColumn(2).apply {
                        cellTemplate = "<TextBlock Text=\"{Binding}\" />"
                        preferredWidth = 150.0
                    }
                }
            }
            withShownTable(table) {
                cellWidths(it.dependencyObject).map { cells -> cells.drop(1) }.distinct()
            } shouldBe listOf(listOf(200L, 150L)) // The same in the header and in every row
        }
    }
}
