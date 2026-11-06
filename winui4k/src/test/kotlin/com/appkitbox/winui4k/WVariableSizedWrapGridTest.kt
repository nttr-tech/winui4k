package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the cell size, orientation and wrapping of [WVariableSizedWrapGrid] (VariableSizedWrapGrid), and rowSpan / columnSpan.
 * Expected values are computed by hand from VariableSizedWrapGrid's rules (children go into ItemWidth × ItemHeight cells, MaximumRowsOrColumns at a time in the orientation's direction,
 * wrapping to the next row / column; a cell with spans occupies several cells). The children are WPanels (stretched to fill the cells).
 * The numbers are DIP multiples of 4, so the physical pixels are integers even at 125% scaling or more.
 */
@Tags("E2E")
class WVariableSizedWrapGridTest : FunSpec() {
    /** 40 × 20 cells, wrapping every 2 vertically (the default orientation is vertical). */
    private fun grid(): WVariableSizedWrapGrid = WVariableSizedWrapGrid(40.0, 20.0).also {
        it.maximumRowsOrColumns = 2
        it.horizontalAlignment = HorizontalAlignment.LEFT
        it.verticalAlignment = VerticalAlignment.TOP
    }

    private fun <T> shown(grid: WVariableSizedWrapGrid, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(grid)
        try {
            onUiThread { grid.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(grid)
        }
    }

    init {
        test("the default is vertical, with no wrapping limit (-1) and unspecified cell sizes (NaN)") {
            onUiThreadGet {
                val grid = WVariableSizedWrapGrid()
                listOf(grid.orientation, grid.maximumRowsOrColumns, grid.itemWidth.isNaN(), grid.itemHeight.isNaN())
            } shouldBe listOf(Orientation.VERTICAL, -1, true, true)
        }

        test("the constructor's cell sizes and the setter values can be read back") {
            onUiThreadGet {
                val grid = WVariableSizedWrapGrid(40.0, 20.0)
                val first = grid.itemWidth to grid.itemHeight
                grid.itemWidth = 60.0
                grid.itemHeight = 32.0
                grid.orientation = Orientation.HORIZONTAL
                grid.maximumRowsOrColumns = 3
                listOf(first, grid.itemWidth to grid.itemHeight, grid.orientation, grid.maximumRowsOrColumns)
            } shouldBe listOf(40.0 to 20.0, 60.0 to 32.0, Orientation.HORIZONTAL, 3)
        }

        test("vertical, 2 at a time: three children put two in the first column and one in the second") {
            val (grid, cells) = onUiThreadGet {
                val cells = List(3) { WPanel() }
                grid().also { g -> cells.forEach { g.add(it, 1, 1) } } to cells
            }
            shown(grid) { cells.map { it.bounds } } shouldBe listOf(
                WRectangle(0, 0, 40, 20),
                WRectangle(0, 20, 40, 20),
                WRectangle(40, 0, 40, 20),
            )
        }

        test("horizontal, 2 at a time: three children put two in the first row and one in the second") {
            val (grid, cells) = onUiThreadGet {
                val cells = List(3) { WPanel() }
                grid().also {
                    it.orientation = Orientation.HORIZONTAL
                    cells.forEach { c -> it.add(c, 1, 1) }
                } to cells
            }
            shown(grid) { cells.map { it.bounds } } shouldBe listOf(
                WRectangle(0, 0, 40, 20),
                WRectangle(40, 0, 40, 20),
                WRectangle(0, 20, 40, 20),
            )
        }

        test("a child with rowSpan 2 takes the height of two cells, and the next child goes into the next column") {
            val (grid, cells) = onUiThreadGet {
                val tall = WPanel()
                val next = WPanel()
                grid().also {
                    it.add(tall, 2, 1)
                    it.add(next, 1, 1)
                } to listOf(tall, next)
            }
            shown(grid) { cells.map { it.bounds } } shouldBe listOf(
                WRectangle(0, 0, 40, 40),
                WRectangle(40, 0, 40, 20),
            )
        }

        test("a child with columnSpan 2 takes the width of two cells") {
            val (grid, cells) = onUiThreadGet {
                val wide = WPanel()
                grid().also {
                    it.orientation = Orientation.HORIZONTAL
                    it.add(wide, 1, 2)
                } to listOf(wide)
            }
            shown(grid) { cells.map { it.bounds } } shouldBe listOf(WRectangle(0, 0, 80, 20))
        }

        test("changing itemWidth after it is shown makes the cell width follow") {
            val (grid, cells) = onUiThreadGet {
                val cells = List(2) { WPanel() }
                grid().also {
                    it.orientation = Orientation.HORIZONTAL
                    cells.forEach { c -> it.add(c, 1, 1) }
                } to cells
            }
            shown(grid) {
                val before = cells[1].x
                grid.itemWidth = 60.0
                grid.updateLayout()
                before to cells[1].x
            } shouldBe (40 to 60)
        }
    }
}
