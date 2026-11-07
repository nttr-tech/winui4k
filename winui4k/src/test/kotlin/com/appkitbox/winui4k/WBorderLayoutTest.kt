package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests [WBorderLayout] against the behavior of java.awt.BorderLayout.
 * The expected child positions and sizes were calculated by hand with the algorithms of BorderLayout.layoutContainer / minimumLayoutSize
 * (trimming the space in the order north -> south -> east -> west -> center).
 */
@Tags("Integration")
class WBorderLayoutTest : FunSpec() {
    private fun box(width: Double, height: Double): WCanvas = WCanvas().also {
        it.width = width
        it.height = height
    }

    private fun panelOf(layout: WBorderLayout, width: Double = 200.0, height: Double = 100.0): WLayoutPanel =
        WLayoutPanel(layout).also {
            it.width = width
            it.height = height
            it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
        }

    /** Places the panel in the window and finishes the scheduled re-layout synchronously. */
    private fun show(panel: WLayoutPanel) {
        UiTestHarness.attachAndAwaitLoaded(panel)
        onUiThread {
            panel.revalidate()
            panel.validate()
        }
    }

    private fun relayout(panel: WLayoutPanel) {
        onUiThread {
            panel.revalidate()
            panel.validate()
        }
    }

    init {
        test("the positions and sizes of the 5 regions match Swing's layoutContainer (north/south full width, east/west the remaining height, center the rest)") {
            val north = onUiThreadGet { box(50.0, 10.0) }
            val south = onUiThreadGet { box(50.0, 20.0) }
            val west = onUiThreadGet { box(30.0, 5.0) }
            val east = onUiThreadGet { box(40.0, 5.0) }
            val center = onUiThreadGet { box(1.0, 1.0) }
            val panel = onUiThreadGet {
                panelOf(WBorderLayout(4.0, 2.0)).also {
                    it.add(north, WBorderLayout.NORTH)
                    it.add(south, WBorderLayout.SOUTH)
                    it.add(west, WBorderLayout.WEST)
                    it.add(east, WBorderLayout.EAST)
                    it.add(center, WBorderLayout.CENTER)
                }
            }
            show(panel)
            try {
                // top=0 bottom=100 left=0 right=200. North (0,0,200,10) -> top=12. South (0,80,200,20) -> bottom=78.
                // East (160,12,40,66) -> right=156. West (0,12,30,66) -> left=34. Center (34,12,122,66)
                onUiThreadGet { north.bounds } shouldBe WRectangle(0, 0, 200, 10)
                onUiThreadGet { south.bounds } shouldBe WRectangle(0, 80, 200, 20)
                onUiThreadGet { east.bounds } shouldBe WRectangle(160, 12, 40, 66)
                onUiThreadGet { west.bounds } shouldBe WRectangle(0, 12, 30, 66)
                onUiThreadGet { center.bounds } shouldBe WRectangle(34, 12, 122, 66)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("lays out within the area minus the insets") {
            val north = onUiThreadGet { box(50.0, 10.0) }
            val center = onUiThreadGet { box(1.0, 1.0) }
            val panel = onUiThreadGet {
                panelOf(WBorderLayout()).also {
                    it.insets = WInsets(top = 4.0, left = 8.0, bottom = 12.0, right = 16.0)
                    it.add(north, WBorderLayout.NORTH)
                    it.add(center) // No constraint means center
                }
            }
            show(panel)
            try {
                // left=8 right=184 top=4 bottom=88. North (8,4,176,10) -> top=14. Center (8,14,176,74)
                onUiThreadGet { north.bounds } shouldBe WRectangle(8, 4, 176, 10)
                onUiThreadGet { center.bounds } shouldBe WRectangle(8, 14, 176, 74)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("hgap and vgap can be set, and the gaps change on re-layout") {
            val layout = WBorderLayout()
            val north = onUiThreadGet { box(50.0, 10.0) }
            val west = onUiThreadGet { box(30.0, 5.0) }
            val center = onUiThreadGet { box(1.0, 1.0) }
            val panel = onUiThreadGet {
                panelOf(layout).also {
                    it.add(north, WBorderLayout.NORTH)
                    it.add(west, WBorderLayout.WEST)
                    it.add(center, WBorderLayout.CENTER)
                }
            }
            show(panel)
            try {
                onUiThreadGet { center.bounds } shouldBe WRectangle(30, 10, 170, 90)
                onUiThread {
                    layout.hgap = 6.0
                    layout.vgap = 3.0
                }
                relayout(panel)
                layout.hgap shouldBe 6.0
                layout.vgap shouldBe 3.0
                // vgap 3 below the north, hgap 6 to the right of the west
                onUiThreadGet { center.bounds } shouldBe WRectangle(36, 13, 164, 87)
                onUiThreadGet { west.bounds } shouldBe WRectangle(0, 13, 30, 87)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("hidden children are not placed, and the others use their region") {
            val north = onUiThreadGet { box(50.0, 10.0) }
            val center = onUiThreadGet { box(1.0, 1.0) }
            val panel = onUiThreadGet {
                panelOf(WBorderLayout()).also {
                    it.add(north, WBorderLayout.NORTH)
                    it.add(center, WBorderLayout.CENTER)
                }
            }
            onUiThread { north.isVisible = false }
            show(panel)
            try {
                onUiThreadGet { center.bounds } shouldBe WRectangle(0, 0, 200, 100)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("PAGE_START / PAGE_END take precedence even with NORTH / SOUTH present, and LINE_START is at the WEST position in a left-to-right flow") {
            val north = onUiThreadGet { box(50.0, 10.0) }
            val pageStart = onUiThreadGet { box(50.0, 16.0) }
            val west = onUiThreadGet { box(30.0, 5.0) }
            val lineStart = onUiThreadGet { box(20.0, 5.0) }
            val lineEnd = onUiThreadGet { box(24.0, 5.0) }
            val center = onUiThreadGet { box(1.0, 1.0) }
            val panel = onUiThreadGet {
                panelOf(WBorderLayout()).also {
                    it.add(north, WBorderLayout.NORTH)
                    it.add(pageStart, WBorderLayout.PAGE_START)
                    it.add(west, WBorderLayout.WEST)
                    it.add(lineStart, WBorderLayout.LINE_START)
                    it.add(lineEnd, WBorderLayout.LINE_END)
                    it.add(center, WBorderLayout.CENTER)
                }
            }
            show(panel)
            try {
                // Swing: for north, firstLine (PAGE_START) takes precedence and north is not placed. West is firstItem (LINE_START), east is lastItem (LINE_END)
                onUiThreadGet { pageStart.bounds } shouldBe WRectangle(0, 0, 200, 16)
                onUiThreadGet { lineStart.bounds } shouldBe WRectangle(0, 16, 20, 84)
                onUiThreadGet { lineEnd.bounds } shouldBe WRectangle(176, 16, 24, 84)
                onUiThreadGet { center.bounds } shouldBe WRectangle(20, 16, 156, 84)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("Swing's string constraints (North / South / East / West / Center / First / Last / Before / After) are accepted") {
            onUiThread {
                val layout = WBorderLayout()
                val panel = WLayoutPanel(layout)
                val expected = mapOf(
                    "North" to WBorderLayout.NORTH,
                    "South" to WBorderLayout.SOUTH,
                    "East" to WBorderLayout.EAST,
                    "West" to WBorderLayout.WEST,
                    "Center" to WBorderLayout.CENTER,
                    "First" to WBorderLayout.PAGE_START,
                    "Last" to WBorderLayout.PAGE_END,
                    "Before" to WBorderLayout.LINE_START,
                    "After" to WBorderLayout.LINE_END,
                )
                for ((text, constraint) in expected) {
                    val child = box(1.0, 1.0)
                    panel.add(child, text)
                    layout.getConstraints(child) shouldBe constraint
                    layout.getLayoutComponent(constraint) shouldBeSameInstanceAs child
                    layout.getLayoutComponent(text) shouldBeSameInstanceAs child
                }
            }
        }

        test("a null constraint is CENTER; unknown strings and constraints that are neither strings nor WBorderLayout.Constraint throw IllegalArgumentException") {
            onUiThread {
                val layout = WBorderLayout()
                val panel = WLayoutPanel(layout)
                val child = box(1.0, 1.0)
                panel.add(child, null)
                layout.getConstraints(child) shouldBe WBorderLayout.CENTER
                shouldThrow<IllegalArgumentException> { panel.add(box(1.0, 1.0), "Nowhere") }
                shouldThrow<IllegalArgumentException> { panel.add(box(1.0, 1.0), "north") } // Case-sensitive
                shouldThrow<IllegalArgumentException> { panel.add(box(1.0, 1.0), 42 as Any) }
                shouldThrow<IllegalArgumentException> { layout.getLayoutComponent("Nowhere") }
                shouldThrow<IllegalArgumentException> { layout.getLayoutComponent(null) }
                panel.componentCount shouldBe 1
            }
        }

        test("getLayoutComponent returns the child of a region and getConstraints the region of a child, and they are null after removal") {
            onUiThread {
                val layout = WBorderLayout()
                val panel = WLayoutPanel(layout)
                val north = box(1.0, 1.0)
                val stranger = box(1.0, 1.0)
                panel.add(north, WBorderLayout.NORTH)
                layout.getLayoutComponent(WBorderLayout.NORTH) shouldBeSameInstanceAs north
                layout.getLayoutComponent(WBorderLayout.SOUTH).shouldBeNull()
                layout.getConstraints(north) shouldBe WBorderLayout.NORTH
                layout.getConstraints(stranger).shouldBeNull()
                layout.getConstraints(null).shouldBeNull()
                panel.remove(north)
                layout.getLayoutComponent(WBorderLayout.NORTH).shouldBeNull()
                layout.getConstraints(north).shouldBeNull()
            }
        }

        test("putting another child into the same region replaces it with the later child (same as Swing)") {
            onUiThread {
                val layout = WBorderLayout()
                val panel = WLayoutPanel(layout)
                val first = box(1.0, 1.0)
                val second = box(1.0, 1.0)
                panel.add(first, WBorderLayout.EAST)
                panel.add(second, WBorderLayout.EAST)
                layout.getLayoutComponent(WBorderLayout.EAST) shouldBeSameInstanceAs second
                layout.getConstraints(first).shouldBeNull()
            }
        }

        test("the orientation-aware getLayoutComponent(parent, constraint) treats LINE_START as WEST in LTR and as EAST in RTL") {
            onUiThread {
                val layout = WBorderLayout()
                val panel = WLayoutPanel(layout)
                val west = box(1.0, 1.0)
                val lineStart = box(1.0, 1.0)
                val lineEnd = box(1.0, 1.0)
                val north = box(1.0, 1.0)
                val pageStart = box(1.0, 1.0)
                panel.add(west, WBorderLayout.WEST)
                panel.add(lineStart, WBorderLayout.LINE_START)
                panel.add(lineEnd, WBorderLayout.LINE_END)
                panel.add(north, WBorderLayout.NORTH)
                panel.add(pageStart, WBorderLayout.PAGE_START)
                layout.getLayoutComponent(panel, WBorderLayout.WEST) shouldBeSameInstanceAs lineStart
                layout.getLayoutComponent(panel, WBorderLayout.EAST) shouldBeSameInstanceAs lineEnd
                layout.getLayoutComponent(panel, WBorderLayout.NORTH) shouldBeSameInstanceAs pageStart
                panel.flowDirection = FlowDirection.RIGHT_TO_LEFT
                layout.getLayoutComponent(panel, WBorderLayout.WEST) shouldBeSameInstanceAs lineEnd
                layout.getLayoutComponent(panel, WBorderLayout.EAST) shouldBeSameInstanceAs lineStart
                // The plain getLayoutComponent(constraint) returns the child of that field regardless of orientation
                layout.getLayoutComponent(WBorderLayout.WEST) shouldBeSameInstanceAs west
                shouldThrow<IllegalArgumentException> { layout.getLayoutComponent(panel, WBorderLayout.PAGE_START) }
            }
        }

        test("minimumLayoutSize sums the children's minimum sizes with the same formula as preferredLayoutSize, and maximumLayoutSize is unlimited") {
            onUiThread {
                val layout = WBorderLayout(4.0, 2.0)
                val panel = WLayoutPanel(layout)
                panel.insets = WInsets(top = 1.0, left = 2.0, bottom = 3.0, right = 4.0)
                fun child(minWidth: Double, minHeight: Double, constraint: WBorderLayout.Constraint) {
                    val c = box(100.0, 100.0)
                    c.minimumSize = WSize(minWidth, minHeight)
                    panel.add(c, constraint)
                }
                child(30.0, 10.0, WBorderLayout.NORTH)
                child(20.0, 5.0, WBorderLayout.SOUTH)
                child(9.0, 7.0, WBorderLayout.EAST)
                child(8.0, 6.0, WBorderLayout.WEST)
                child(12.0, 3.0, WBorderLayout.CENTER)
                // Middle row: width 9+4 + 8+4 + 12 = 37, height max(7,6,3) = 7. North (width max(30,37) = 37, height 7+10+2 = 19),
                // south (width 37, height 19+5+2 = 26). Including insets 37+2+4 = 43, 26+1+3 = 30
                layout.minimumLayoutSize(panel) shouldBe WSize(43.0, 30.0)
                layout.maximumLayoutSize(panel) shouldBe WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
                // The preferred size (children of 100x100) is a different value
                layout.preferredLayoutSize(panel) shouldBe WSize(100.0 * 3 + 8.0 + 2 + 4, 100.0 * 3 + 4.0 + 1 + 3)
            }
        }
    }
}
