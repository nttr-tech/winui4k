package com.appkitbox.winui4k.table

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests, with a sorter that is not shown (no UI needed), that TableRowSorter.stringConverter (TableStringConverter) affects
 * Entry.getStringValue of a RowFilter. The expected values are the same as the Entry of Swing's DefaultRowSorter:
 * with a converter set, getStringValue is the converted result (an empty string for null); without one, the value's toString() (an empty string for null).
 * Filtering in a shown table is in WTableStringConverterTest.
 */
@Tags("Pure")
class TableStringConverterTest : FunSpec() {
    private val model = DefaultTableModel(
        listOf(
            listOf("Apple", 150),
            listOf("orange", 80),
            listOf("Grape", null),
        ),
        listOf("name", "price"),
    )

    /** A converter that turns values into strings with " yen" (returns null for rows whose value is null). */
    private val yen = object : TableStringConverter() {
        override fun toString(model: TableModel, row: Int, column: Int): String? = model.getValueAt(row, column)?.let { "$it yen" }
    }

    /** The model row indexes of [sorter] that pass [filter]. */
    private fun included(sorter: TableRowSorter<TableModel>, filter: RowFilter<in TableModel, in Int>): List<Int> {
        sorter.setRowFilter(filter)
        return (0 until model.getRowCount()).filter { sorter.includes(it) }
    }

    /** getStringValue of column [column] of each row (as read inside the filter). */
    private fun stringValues(sorter: TableRowSorter<TableModel>, column: Int): List<String> {
        val seen = mutableListOf<String>()
        included(
            sorter,
            object : RowFilter<TableModel, Int>() {
                override fun include(entry: Entry<out TableModel, out Int>): Boolean {
                    seen += entry.getStringValue(column)
                    return true
                }
            },
        )
        return seen
    }

    init {
        context("Defaults") {
            test("stringConverter is null by default, and getStringValue is the value's toString() (an empty string for null)") {
                val sorter = TableRowSorter<TableModel>(model)
                sorter.stringConverter shouldBe null
                stringValues(sorter, 1) shouldBe listOf("150", "80", "")
            }
        }

        context("Reading and writing") {
            test("the set converter reads back, getStringValue becomes the converted result, and a null result becomes an empty string") {
                val sorter = TableRowSorter<TableModel>(model)
                sorter.stringConverter = yen
                sorter.stringConverter shouldBe yen
                stringValues(sorter, 1) shouldBe listOf("150 yen", "80 yen", "")
            }

            test("resetting it to null returns to the value's toString()") {
                val sorter = TableRowSorter<TableModel>(model)
                sorter.stringConverter = yen
                sorter.stringConverter = null
                stringValues(sorter, 1) shouldBe listOf("150", "80", "")
            }

            test("regexFilter matches against the converted strings (\"0 yen$\" matches 150 yen and 80 yen, and nothing without the conversion)") {
                val sorter = TableRowSorter<TableModel>(model)
                included(sorter, RowFilter.regexFilter("0 yen$", 1)) shouldBe emptyList()
                sorter.stringConverter = yen
                included(sorter, RowFilter.regexFilter("0 yen$", 1)) shouldBe listOf(0, 1)
            }

            test("the row and column passed to the converter are model indexes") {
                val sorter = TableRowSorter<TableModel>(model)
                val calls = mutableListOf<Pair<Int, Int>>()
                sorter.stringConverter = object : TableStringConverter() {
                    override fun toString(model: TableModel, row: Int, column: Int): String? {
                        calls += row to column
                        return ""
                    }
                }
                included(sorter, RowFilter.regexFilter("x", 0, 1))
                calls shouldBe listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1, 2 to 0, 2 to 1)
            }
        }

        context("Swing compatibility") {
            test("numberFilter compares the value itself (getValue) regardless of the conversion") {
                val sorter = TableRowSorter<TableModel>(model)
                sorter.stringConverter = yen
                included(sorter, RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 100, 1)) shouldBe listOf(0)
            }
        }
    }
}
