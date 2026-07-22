package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests verifying WTable's row management, single / multiple selection, and sorting (including column comparators). */
class WTableTest : FunSpec() {
    private fun buildTable(comparator: Comparator<String>? = null): WTable =
        WTable(
            listOf(
                WTableColumn("Name", comparator = comparator),
                WTableColumn("Size"),
            ),
        )

    init {
        test("rows added in bulk via addRows can be retrieved through rowCount and getValueAt") {
            val (count, cell) = onUiThreadGet {
                val table = buildTable()
                table.addRows(listOf(listOf("a.txt", "1"), listOf("b.txt", "2"), listOf("c.txt", "3")))
                table.rowCount to table.getValueAt(2, 0)
            }
            count shouldBe 3
            cell shouldBe "c.txt"
        }

        test("setting selectedRow makes selectedRows return that same single row") {
            val (selected, selectedList) = onUiThreadGet {
                val table = buildTable()
                table.addRows(listOf(listOf("a", "1"), listOf("b", "2")))
                table.selectedRow = 1
                table.selectedRow to table.selectedRows
            }
            selected shouldBe 1
            selectedList shouldBe listOf(1)
        }

        test("calling selectAll with EXTENDED selection puts every row into selectedRows") {
            onUiThreadGet {
                val table = buildTable()
                table.selectionMode = ListViewSelectionMode.EXTENDED
                table.addRows(listOf(listOf("a", "1"), listOf("b", "2"), listOf("c", "3")))
                table.selectAll()
                table.selectedRows
            } shouldBe listOf(0, 1, 2)
        }

        test("sorting reorders displayOrder, and selectedRow keeps its model index") {
            val (order, selected) = onUiThreadGet {
                val table = buildTable()
                table.addRows(listOf(listOf("b", "2"), listOf("a", "1"), listOf("c", "3")))
                table.selectedRow = 0 // the "b" row
                table.sortBy(0, SortDirection.ASCENDING)
                table.displayOrder to table.selectedRow
            }
            order shouldBe listOf(1, 0, 2) // in the order a, b, c
            selected shouldBe 0 // the model index still points to "b" even though its display position moved
        }

        test("a row added via addRow while sorted is also included in the reordering") {
            onUiThreadGet {
                val table = buildTable()
                table.addRows(listOf(listOf("b", "2"), listOf("c", "3")))
                table.sortBy(0, SortDirection.ASCENDING)
                table.addRow("a", "1")
                table.displayOrder
            } shouldBe listOf(2, 0, 1) // "a" (model index 2) comes first
        }

        test("specifying a column comparator sorts using it (comparing the numeric part)") {
            // With plain string comparison, "10 KB" sorts before "9 KB", so this shows the effect of numeric comparison
            val numeric = Comparator<String> { a, b ->
                val x = a.filter { it.isDigit() }.toLong()
                val y = b.filter { it.isDigit() }.toLong()
                x.compareTo(y)
            }
            onUiThreadGet {
                val table = buildTable(comparator = numeric)
                table.addRows(listOf(listOf("10 KB", "x"), listOf("9 KB", "y")))
                table.sortBy(0, SortDirection.ASCENDING)
                table.displayOrder
            } shouldBe listOf(1, 0) // "9 KB" (model index 1) comes first
        }
    }
}
