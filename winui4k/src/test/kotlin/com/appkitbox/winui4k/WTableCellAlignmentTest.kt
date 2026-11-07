package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.table.DefaultTableModel
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

/**
 * Tests that check, by actually showing them, that with WTable's default cell display the columns whose getColumnClass is Number are right-aligned (the same as JTable's default renderer for Number)
 * and the other columns are left-aligned.
 */
@Tags("E2E")
class WTableCellAlignmentTest : FunSpec() {
    /** A table with the 4 columns Name (String) / Stock (Integer) / Price (BigDecimal) / Code (numbers in Object), each 100 wide. */
    private fun stockTable(): WTable {
        val model = object : DefaultTableModel(
            listOf(listOf("Apple", 12, BigDecimal("1.50"), 7)),
            listOf("Name", "Stock", "Price", "Code"),
        ) {
            override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
                0 -> String::class.java
                1 -> Integer::class.java
                2 -> BigDecimal::class.java
                else -> Any::class.java
            }
        }
        return WTable(model).also { table ->
            table.width = 400.0
            table.columnModel.getColumns().forEach { it.preferredWidth = 100.0 }
        }
    }

    /** The distances from the column's left edge and to the column's right edge of the TextBlock (including the left and right padding) of the cell whose text is [text]. */
    private fun gapsInColumn(table: WTable, text: String, column: Int): Pair<Double, Double> {
        val block = checkNotNull(tableTreeFindText(table.dependencyObject, text)) { "TextBlock not found: $text" }
        try {
            val (width, _) = VisualHitTest.actualSizeOf(block)
            val (left, _) = VisualHitTest.transform(block, table.uiElement, 0.0, 0.0)
            val columnLeft = (0 until column).sumOf { table.columnModel.getColumn(it).width } + BORDER
            val columnRight = columnLeft + table.columnModel.getColumn(column).width
            return (left - columnLeft) to (columnRight - (left + width))
        } finally {
            block.release()
        }
    }

    private fun <T> withShown(table: WTable, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(table)
        try {
            return onUiThreadGet {
                table.updateLayout()
                block()
            }
        } finally {
            UiTestHarness.detach(table)
        }
    }

    init {
        test("values in a column whose getColumnClass is Integer are aligned to the right of the column") {
            val table = onUiThreadGet { stockTable() }
            val (leftGap, rightGap) = withShown(table) { gapsInColumn(table, "12", 1) }
            rightGap shouldBe (0.0 plusOrMinus GRID_LINE)
            (leftGap > MIN_SPACE) shouldBe true
        }

        test("values in a column whose getColumnClass is BigDecimal (a subclass of Number) are also aligned to the right of the column") {
            val table = onUiThreadGet { stockTable() }
            val (leftGap, rightGap) = withShown(table) { gapsInColumn(table, "1.50", 2) }
            rightGap shouldBe (0.0 plusOrMinus GRID_LINE)
            (leftGap > MIN_SPACE) shouldBe true
        }

        test("values in columns whose getColumnClass is String or Object are aligned to the left even if they are numbers") {
            val table = onUiThreadGet { stockTable() }
            val gaps = withShown(table) { listOf(gapsInColumn(table, "Apple", 0), gapsInColumn(table, "7", 3)) }
            gaps.forEach { (leftGap, rightGap) ->
                leftGap shouldBe (0.0 plusOrMinus GRID_LINE)
                (rightGap > MIN_SPACE) shouldBe true
            }
        }
    }

    private companion object {
        /** The thickness of the table's border (TableView's Border). */
        const val BORDER = 4.0 / 3

        /** The tolerance for the vertical grid line at the right edge of a cell (4/3) and rounding. */
        const val GRID_LINE = 2.0

        /** The minimum width that should be empty on the side opposite the alignment (short values go into 100-wide columns, so about half is empty). */
        const val MIN_SPACE = 30.0
    }
}
