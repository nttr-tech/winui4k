package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests [WGridLayout] against the behavior of java.awt.GridLayout.
 * The expected child positions and sizes were calculated by hand with the algorithms of GridLayout.layoutContainer / preferredLayoutSize
 * (the calculations are left in each test's comments). Only evenly divisible values are used, so the results match Swing's integer calculation.
 */
class WGridLayoutTest : FunSpec() {
    private fun box(width: Double = 1.0, height: Double = 1.0): WCanvas = WCanvas().also {
        it.width = width
        it.height = height
    }

    private fun panelOf(layout: WGridLayout, width: Double, height: Double, count: Int): Pair<WLayoutPanel, List<WCanvas>> =
        onUiThreadGet {
            val children = List(count) { box() }
            val panel = WLayoutPanel(layout).also {
                it.width = width
                it.height = height
                it.horizontalAlignment = HorizontalAlignment.LEFT
                children.forEach { child -> it.add(child) }
            }
            panel to children
        }

    private fun show(panel: WLayoutPanel) {
        UiTestHarness.attachAndAwaitLoaded(panel)
        onUiThread {
            panel.revalidate()
            panel.validate()
        }
    }

    private fun bounds(components: List<WComponent>): List<WRectangle> = onUiThreadGet { components.map { it.bounds } }

    init {
        test("with 2 rows specified, 5 children make 3 columns and go into cells of equal size left to right and top to bottom in the order they were added") {
            val (panel, children) = panelOf(WGridLayout(2, 3, 4.0, 6.0), 116.0, 66.0, 5)
            show(panel)
            try {
                // Cell width (116 - 2 x 4) / 3 = 36, cell height (66 - 6) / 2 = 30. x = 0, 40, 80, y = 0, 36
                bounds(children) shouldBe listOf(
                    WRectangle(0, 0, 36, 30),
                    WRectangle(40, 0, 36, 30),
                    WRectangle(80, 0, 36, 30),
                    WRectangle(0, 36, 36, 30),
                    WRectangle(40, 36, 36, 30),
                )
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("the insets are taken inside before dividing equally") {
            val (panel, children) = panelOf(WGridLayout(2, 3, 4.0, 6.0), 118.0, 72.0, 5)
            onUiThread { panel.insets = WInsets(top = 2.0, left = 3.0, bottom = 4.0, right = 5.0) }
            show(panel)
            try {
                // Area (118 - 8) x (72 - 6) = 110 x 66. Cells (110 - 8) / 3 = 34, (66 - 6) / 2 = 30. x = 3, 41, 79, y = 2, 38
                bounds(children) shouldBe listOf(
                    WRectangle(3, 2, 34, 30),
                    WRectangle(41, 2, 34, 30),
                    WRectangle(79, 2, 34, 30),
                    WRectangle(3, 38, 34, 30),
                    WRectangle(41, 38, 34, 30),
                )
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("with 0 rows the number of columns is used, and the rows are the number of children rounded up") {
            val (panel, children) = panelOf(WGridLayout(0, 2, 4.0, 6.0), 84.0, 90.0, 5)
            show(panel)
            try {
                // 5 children / 2 columns = 3 rows. Cells (84 - 4) / 2 = 40, (90 - 2 x 6) / 3 = 26. x = 0, 44, y = 0, 32, 64
                bounds(children) shouldBe listOf(
                    WRectangle(0, 0, 40, 26),
                    WRectangle(44, 0, 40, 26),
                    WRectangle(0, 32, 40, 26),
                    WRectangle(44, 32, 40, 26),
                    WRectangle(0, 64, 40, 26),
                )
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("when rows is not 0, the column setting is ignored (same as Swing)") {
            val (panel, children) = panelOf(WGridLayout(2, 2, 0.0, 0.0), 90.0, 40.0, 5)
            show(panel)
            try {
                // 2 rows, so the columns are 5 children rounded up: 3. Cells 90 / 3 = 30, 40 / 2 = 20
                bounds(children) shouldBe listOf(
                    WRectangle(0, 0, 30, 20),
                    WRectangle(30, 0, 30, 20),
                    WRectangle(60, 0, 30, 20),
                    WRectangle(0, 20, 30, 20),
                    WRectangle(30, 20, 30, 20),
                )
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("the default is 1 row, and the columns grow with the number of children") {
            val (panel, children) = panelOf(WGridLayout(), 90.0, 40.0, 3)
            show(panel)
            try {
                bounds(children) shouldBe listOf(
                    WRectangle(0, 0, 30, 40),
                    WRectangle(30, 0, 30, 40),
                    WRectangle(60, 0, 30, 40),
                )
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("hidden children also take a cell, and the children's preferred sizes do not affect the cell size") {
            val layout = WGridLayout(1, 3, 0.0, 0.0)
            val (panel, children) = panelOf(layout, 90.0, 30.0, 3)
            onUiThread {
                children[1].isVisible = false
                children[0].width = 5.0 // Cells are divided equally even if the preferred sizes differ
            }
            show(panel)
            try {
                bounds(listOf(children[0], children[2])) shouldBe listOf(WRectangle(0, 0, 30, 30), WRectangle(60, 0, 30, 30))
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("in a right-to-left flow the first column is at the right edge") {
            val (panel, children) = panelOf(WGridLayout(1, 3, 4.0, 6.0), 116.0, 30.0, 3)
            onUiThread { panel.flowDirection = FlowDirection.RIGHT_TO_LEFT }
            show(panel)
            try {
                // Cell width 36. x is (116 - 0 - 36) - column x 40 = 80, 40, 0
                bounds(children) shouldBe listOf(
                    WRectangle(80, 0, 36, 30),
                    WRectangle(40, 0, 36, 30),
                    WRectangle(0, 0, 36, 30),
                )
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("changing hgap and vgap and re-laying out changes the cell spacing and size") {
            val layout = WGridLayout(2, 2, 0.0, 0.0)
            val (panel, children) = panelOf(layout, 100.0, 60.0, 4)
            show(panel)
            try {
                bounds(children) shouldBe listOf(
                    WRectangle(0, 0, 50, 30),
                    WRectangle(50, 0, 50, 30),
                    WRectangle(0, 30, 50, 30),
                    WRectangle(50, 30, 50, 30),
                )
                onUiThread {
                    layout.hgap = 10.0
                    layout.vgap = 20.0
                    panel.revalidate()
                    panel.validate()
                }
                layout.hgap shouldBe 10.0
                layout.vgap shouldBe 20.0
                // Cells (100 - 10) / 2 = 45, (60 - 20) / 2 = 20. x = 0, 55, y = 0, 40
                bounds(children) shouldBe listOf(
                    WRectangle(0, 0, 45, 20),
                    WRectangle(55, 0, 45, 20),
                    WRectangle(0, 40, 45, 20),
                    WRectangle(55, 40, 45, 20),
                )
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("the preferred size takes the maximum preferred width and height of the children as one cell and adds up the cells, gaps and insets") {
            onUiThread {
                val layout = WGridLayout(1, 0, 4.0, 6.0)
                val panel = WLayoutPanel(layout)
                panel.add(box(10.0, 5.0))
                panel.add(box(20.0, 8.0))
                panel.add(box(15.0, 12.0))
                // 1 row 3 columns, cells 20 x 12. Width 3 x 20 + 2 x 4 = 68, height 12
                layout.preferredLayoutSize(panel) shouldBe WSize(68.0, 12.0)
                panel.insets = WInsets(top = 2.0, left = 3.0, bottom = 4.0, right = 5.0)
                layout.preferredLayoutSize(panel) shouldBe WSize(76.0, 18.0)
                layout.rows = 2
                // 2 rows 2 columns: width 2 x 20 + 4 + 8 = 52, height 2 x 12 + 6 + 6 = 36
                layout.preferredLayoutSize(panel) shouldBe WSize(52.0, 36.0)
            }
        }

        test("hidden children also count in the preferred size; without children the width becomes 0 instead of negative by the gaps (Swing gives -4)") {
            onUiThread {
                val layout = WGridLayout(2, 3, 4.0, 6.0)
                val panel = WLayoutPanel(layout)
                // 2 rows 0 columns: the width 0 x 0 + (0 - 1) x 4 = -4 is rounded up to 0, the height is 2 x 0 + 6
                layout.preferredLayoutSize(panel) shouldBe WSize(0.0, 6.0)
                val a = box(10.0, 5.0)
                panel.add(a)
                a.isVisible = false
                // 2 rows 1 column: width 10, height 2 x 5 + 6 = 16
                layout.preferredLayoutSize(panel) shouldBe WSize(10.0, 16.0)
            }
        }

        test("the minimum size sums the children's minimumSize with the same formula as the preferred size") {
            onUiThread {
                val layout = WGridLayout(1, 0, 4.0, 6.0)
                val panel = WLayoutPanel(layout)
                listOf(WSize(3.0, 4.0), WSize(5.0, 6.0), WSize(7.0, 2.0)).forEach {
                    panel.add(box(50.0, 50.0).also { child -> child.minimumSize = it })
                }
                // Cells 7 x 6, 1 row 3 columns: width 3 x 7 + 8 = 29, height 6
                layout.minimumLayoutSize(panel) shouldBe WSize(29.0, 6.0)
            }
        }

        test("setting both rows and columns to 0 throws IllegalArgumentException (both at construction and with setRows / setColumns)") {
            shouldThrow<IllegalArgumentException> { WGridLayout(0, 0) }
            shouldThrow<IllegalArgumentException> { WGridLayout(0, 0, 4.0, 4.0) }
            val rowsOnly = WGridLayout(2, 0)
            shouldThrow<IllegalArgumentException> { rowsOnly.rows = 0 }
            rowsOnly.rows shouldBe 2
            val columnsOnly = WGridLayout(0, 3)
            shouldThrow<IllegalArgumentException> { columnsOnly.columns = 0 }
            columnsOnly.columns shouldBe 3
            // If one is not 0, the other can be set to 0
            rowsOnly.columns = 4
            rowsOnly.rows = 0
            rowsOnly.rows shouldBe 0
            rowsOnly.columns shouldBe 4
        }

        test("the default is 1 row, no columns and gap 0, and the arguments are in the same order as Swing's constructor") {
            val layout = WGridLayout()
            layout.rows shouldBe 1
            layout.columns shouldBe 0
            layout.hgap shouldBe 0.0
            layout.vgap shouldBe 0.0
            val full = WGridLayout(2, 3, 4.0, 5.0)
            listOf(full.rows, full.columns) shouldBe listOf(2, 3)
            listOf(full.hgap, full.vgap) shouldBe listOf(4.0, 5.0)
        }
    }
}
