package com.appkitbox.winui4k.table

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Date

/** Tests for each RowFilter filter and for TableRowSorter settings before display (no UI required). */
class RowFilterTest : FunSpec() {
    private val model = DefaultTableModel(
        listOf(
            listOf("Apple", 150, Date(1_000)),
            listOf("orange", 80, Date(2_000)),
            listOf("Grape", 480, Date(3_000)),
        ),
        listOf("name", "price", "date"),
    )

    /** Model row indices that pass [filter]. */
    private fun included(filter: RowFilter<in TableModel, in Int>): List<Int> {
        val sorter = TableRowSorter<TableModel>(model)
        sorter.setRowFilter(filter)
        return (0 until model.getRowCount()).filter { sorter.includes(it) }
    }

    init {
        test("regexFilter filters by partial string match in the given column, and checks all columns when the column is omitted") {
            included(RowFilter.regexFilter("(?i)^a", 0)) shouldBe listOf(0)
            included(RowFilter.regexFilter("80")) shouldBe listOf(1, 2) // 80 and 480
        }

        test("numberFilter filters by numeric comparison") {
            included(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 100, 1)) shouldBe listOf(0, 2)
            included(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 100, 1)) shouldBe listOf(1)
            included(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, 480, 1)) shouldBe listOf(2)
            included(RowFilter.numberFilter(RowFilter.ComparisonType.NOT_EQUAL, 480, 1)) shouldBe listOf(0, 1)
        }

        test("dateFilter filters by date comparison") {
            included(RowFilter.dateFilter(RowFilter.ComparisonType.AFTER, Date(1_500), 2)) shouldBe listOf(1, 2)
        }

        test("andFilter / orFilter / notFilter combine conditions") {
            val cheap = RowFilter.numberFilter<TableModel, Int>(RowFilter.ComparisonType.BEFORE, 200, 1)
            val startsWithG = RowFilter.regexFilter<TableModel, Int>("^G", 0)
            included(RowFilter.orFilter(listOf(cheap, startsWithG))) shouldBe listOf(0, 1, 2)
            included(RowFilter.andFilter(listOf(cheap, RowFilter.notFilter(RowFilter.regexFilter("^o", 0))))) shouldBe listOf(0)
        }

        test("a TableRowSorter before display keeps sort keys and per-column settings, and toggleSortOrder switches between ascending and descending") {
            val sorter = TableRowSorter(model)
            sorter.toggleSortOrder(1)
            val first = sorter.getSortKeys()
            sorter.toggleSortOrder(1)
            sorter.setSortable(0, false)
            listOf(first, sorter.getSortKeys(), sorter.isSortable(0), sorter.isSortable(1), sorter.getViewRowCount()) shouldBe listOf(
                listOf(RowSorter.SortKey(1, SortOrder.ASCENDING)),
                listOf(RowSorter.SortKey(1, SortOrder.DESCENDING)),
                false,
                true,
                3,
            )
        }

        test("UNSORTED sort keys are ignored, and clearing them removes sorting") {
            val sorter = TableRowSorter(model)
            sorter.setSortKeys(listOf(RowSorter.SortKey(0, SortOrder.UNSORTED)))
            sorter.getSortKeys() shouldBe emptyList()
        }
    }
}
