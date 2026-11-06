package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the row / column definitions and cell placement of [WGrid] (Grid) with the children's [WComponent.bounds].
 * Expected values are computed by hand from Grid's rules (fixed pixels first, Auto takes the child's preferred size, the rest is distributed by Star weights, and spacing goes only between rows / columns).
 * The numbers are DIP multiples of 4, so the physical pixels are integers even at 125% scaling or more.
 * The children are WPanels (stretched to fill the cells) so that the cell areas are visible as is.
 */
class WGridTest : FunSpec() {
    private fun grid(rowSpacing: Double = 0.0, columnSpacing: Double = 0.0): WGrid =
        WGrid(rowSpacing, columnSpacing).also {
            it.width = 300.0
            it.height = 200.0
            it.horizontalAlignment = HorizontalAlignment.LEFT
            it.verticalAlignment = VerticalAlignment.TOP
        }

    private fun <T> shown(grid: WGrid, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(grid)
        try {
            onUiThread { grid.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(grid)
        }
    }

    init {
        test("rowSpacing and columnSpacing read back the constructor values and the setter values") {
            onUiThreadGet {
                val grid = WGrid(8.0, 4.0)
                val first = Pair(grid.rowSpacing, grid.columnSpacing)
                grid.rowSpacing = 12.0
                grid.columnSpacing = 16.0
                Pair(first, Pair(grid.rowSpacing, grid.columnSpacing))
            } shouldBe Pair(Pair(8.0, 4.0), Pair(12.0, 16.0))
        }

        test("the default spacing is 0") {
            onUiThreadGet {
                val grid = WGrid()
                Pair(grid.rowSpacing, grid.columnSpacing)
            } shouldBe Pair(0.0, 0.0)
        }

        test("fixed pixels and Star: with rows 50 + rest and columns 100 + rest, the four cells are (0,0,100,50) (100,0,200,50) (0,50,100,150) (100,50,200,150)") {
            val (grid, cells) = onUiThreadGet {
                val cells = List(4) { WPanel() }
                grid().also {
                    it.addRow(GridLength.pixel(50.0))
                    it.addRow(GridLength.star())
                    it.addColumn(GridLength.pixel(100.0))
                    it.addColumn(GridLength.star())
                    it.add(cells[0], 0, 0)
                    it.add(cells[1], 0, 1)
                    it.add(cells[2], 1, 0)
                    it.add(cells[3], 1, 1)
                } to cells
            }
            shown(grid) { cells.map { it.bounds } } shouldBe listOf(
                WRectangle(0, 0, 100, 50),
                WRectangle(100, 0, 200, 50),
                WRectangle(0, 50, 100, 150),
                WRectangle(100, 50, 200, 150),
            )
        }

        test("row spacing 10 and column spacing 4 go only between cells, and the Star shares shrink by that much") {
            val (grid, cells) = onUiThreadGet {
                val cells = List(4) { WPanel() }
                grid(rowSpacing = 10.0, columnSpacing = 4.0).also {
                    it.addRow(GridLength.pixel(50.0))
                    it.addRow(GridLength.star())
                    it.addColumn(GridLength.pixel(100.0))
                    it.addColumn(GridLength.star())
                    it.add(cells[0], 0, 0)
                    it.add(cells[1], 0, 1)
                    it.add(cells[2], 1, 0)
                    it.add(cells[3], 1, 1)
                } to cells
            }
            // column 1 is x = 100 + 4, width = 300 - 104. row 1 is y = 50 + 10, height = 200 - 60
            shown(grid) { cells.map { it.bounds } } shouldBe listOf(
                WRectangle(0, 0, 100, 50),
                WRectangle(104, 0, 196, 50),
                WRectangle(0, 60, 100, 140),
                WRectangle(104, 60, 196, 140),
            )
        }

        test("Star weights of 1 : 3 split a width of 300 into 75 and 225") {
            val (grid, cells) = onUiThreadGet {
                val cells = List(2) { WPanel() }
                grid().also {
                    it.addRow(GridLength.star())
                    it.addColumn(GridLength.star(1.0))
                    it.addColumn(GridLength.star(3.0))
                    it.add(cells[0], 0, 0)
                    it.add(cells[1], 0, 1)
                } to cells
            }
            shown(grid) { cells.map { it.bounds } } shouldBe listOf(
                WRectangle(0, 0, 75, 200),
                WRectangle(75, 0, 225, 200),
            )
        }

        test("an Auto row gets the child's preferred height, and the next Star row takes the rest") {
            val (grid, cells) = onUiThreadGet {
                val sized = WCanvas().also {
                    it.width = 40.0
                    it.height = 28.0
                }
                val rest = WPanel()
                grid().also {
                    it.addRow() // Auto by default
                    it.addRow(GridLength.star())
                    it.addColumn(GridLength.star())
                    it.add(sized, 0, 0)
                    it.add(rest, 1, 0)
                } to listOf(sized, rest)
            }
            shown(grid) { cells.map { it.bounds.y to it.bounds.height } } shouldBe listOf(0 to 28, 28 to 172)
        }

        test("a child with columnSpan 2 takes the width of two columns, and one with rowSpan 2 the height of two rows") {
            val (grid, cells) = onUiThreadGet {
                val wide = WPanel()
                val tall = WPanel()
                grid().also {
                    it.addRow(GridLength.pixel(60.0))
                    it.addRow(GridLength.pixel(80.0))
                    it.addColumn(GridLength.pixel(100.0))
                    it.addColumn(GridLength.pixel(120.0))
                    it.add(wide, 0, 0, rowSpan = 1, columnSpan = 2)
                    it.add(tall, 0, 1, rowSpan = 2, columnSpan = 1)
                } to listOf(wide, tall)
            }
            // wide: two columns from column 0, width 220, height 60. tall: from column 1 (x = 100), width 120, two rows, height 140 (overlapping wide)
            shown(grid) { cells.map { it.bounds } } shouldBe listOf(
                WRectangle(0, 0, 220, 60),
                WRectangle(100, 0, 120, 140),
            )
        }

        test("changing columnSpacing after it is shown makes the column positions follow") {
            val (grid, cells) = onUiThreadGet {
                val cells = List(2) { WPanel() }
                grid().also {
                    it.addRow(GridLength.star())
                    it.addColumn(GridLength.pixel(100.0))
                    it.addColumn(GridLength.pixel(100.0))
                    it.add(cells[0], 0, 0)
                    it.add(cells[1], 0, 1)
                } to cells
            }
            shown(grid) {
                val before = cells[1].x
                grid.columnSpacing = 20.0
                grid.updateLayout()
                before to cells[1].x
            } shouldBe (100 to 120)
        }
    }
}
