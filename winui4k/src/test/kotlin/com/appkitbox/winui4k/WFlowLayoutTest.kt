package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests [WFlowLayout] against the behavior of java.awt.FlowLayout.
 * The expected child positions and sizes were calculated by hand with the algorithms of FlowLayout.layoutContainer / moveComponents / preferredLayoutSize
 * (the calculations are left in each test's comments). The children's preferred sizes are set with explicit width / height.
 */
@Tags("Integration")
class WFlowLayoutTest : FunSpec() {
    private fun box(width: Double, height: Double): WCanvas = WCanvas().also {
        it.width = width
        it.height = height
    }

    private class Scene(val panel: WLayoutPanel, val a: WCanvas, val b: WCanvas, val c: WCanvas)

    /**
     * Children with preferred sizes a = 31x10, b = 40x20, c = 50x10 are added in this order to a panel 100 wide.
     * With the default gap of 5, maxwidth = 100 - 10 = 90, so a and b are in the first row (31 + 5 + 40 = 76) and c wraps to the second row.
     */
    private fun scene(layout: WFlowLayout, panelWidth: Double = 100.0): Scene = onUiThreadGet {
        val a = box(31.0, 10.0)
        val b = box(40.0, 20.0)
        val c = box(50.0, 10.0)
        val panel = WLayoutPanel(layout).also {
            it.width = panelWidth
            it.height = 100.0
            it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
            it.add(a)
            it.add(b)
            it.add(c)
        }
        Scene(panel, a, b, c)
    }

    private fun show(scene: Scene) {
        UiTestHarness.attachAndAwaitLoaded(scene.panel)
        onUiThread {
            scene.panel.revalidate()
            scene.panel.validate()
        }
    }

    private fun bounds(vararg components: WComponent): List<WRectangle> = onUiThreadGet { components.map { it.bounds } }

    init {
        test("the default is centered with a gap of 5; children that do not fit in a row wrap to the next row, each row is centered, and children are centered vertically in the row") {
            val s = scene(WFlowLayout())
            show(s)
            try {
                // Row 1: spare 90 - 76 = 14, x = 5 + 14 / 2 = 12, row height 20. a is lowered by (20 - 10) / 2: y = 5 + 5 = 10
                // a = (12, 10, 31, 10), b = (12 + 31 + 5 = 48, 5, 40, 20). Row 2 y = 5 + 5 + 20 = 30: spare 90 - 50 = 40, x = 5 + 20 = 25
                bounds(s.a, s.b, s.c) shouldBe listOf(
                    WRectangle(12, 10, 31, 10),
                    WRectangle(48, 5, 40, 20),
                    WRectangle(25, 30, 50, 10),
                )
            } finally {
                UiTestHarness.detach(s.panel)
            }
        }

        test("LEFT aligns each row to the left and RIGHT to the right") {
            val left = scene(WFlowLayout(WFlowLayout.LEFT))
            show(left)
            try {
                // Row start x = 5: a (5, 10), b (5 + 31 + 5 = 41, 5), row 2 c (5, 30)
                bounds(left.a, left.b, left.c) shouldBe listOf(
                    WRectangle(5, 10, 31, 10),
                    WRectangle(41, 5, 40, 20),
                    WRectangle(5, 30, 50, 10),
                )
            } finally {
                UiTestHarness.detach(left.panel)
            }
            val right = scene(WFlowLayout(WFlowLayout.RIGHT))
            show(right)
            try {
                // Row 1 x = 5 + 14 = 19: a (19, 10), b (19 + 31 + 5 = 55, 5) (right edge 95 = 100 - 5). Row 2 x = 5 + 40 = 45: c (45, 30)
                bounds(right.a, right.b, right.c) shouldBe listOf(
                    WRectangle(19, 10, 31, 10),
                    WRectangle(55, 5, 40, 20),
                    WRectangle(45, 30, 50, 10),
                )
            } finally {
                UiTestHarness.detach(right.panel)
            }
        }

        test("in a left-to-right flow, LEADING is at the same position as LEFT and TRAILING as RIGHT") {
            val leading = scene(WFlowLayout(WFlowLayout.LEADING))
            show(leading)
            try {
                bounds(leading.a, leading.b, leading.c) shouldBe listOf(
                    WRectangle(5, 10, 31, 10),
                    WRectangle(41, 5, 40, 20),
                    WRectangle(5, 30, 50, 10),
                )
            } finally {
                UiTestHarness.detach(leading.panel)
            }
            val trailing = scene(WFlowLayout(WFlowLayout.TRAILING))
            show(trailing)
            try {
                bounds(trailing.a, trailing.b, trailing.c) shouldBe listOf(
                    WRectangle(19, 10, 31, 10),
                    WRectangle(55, 5, 40, 20),
                    WRectangle(45, 30, 50, 10),
                )
            } finally {
                UiTestHarness.detach(trailing.panel)
            }
        }

        test("in right-to-left flow, children are visually laid out from the right edge and LEADING aligns right, and bounds are measured from the panel's right edge") {
            val s = scene(WFlowLayout(WFlowLayout.LEADING))
            onUiThread { s.panel.flowDirection = FlowDirection.RIGHT_TO_LEFT }
            show(s)
            try {
                // LEADING with fixed x = 5: visually (Swing) a is 100 - 5 - 31 = 64, b is at x = 5 + 31 + 5 = 41, so 100 - 41 - 40 = 19,
                // and c on the second row is 100 - 5 - 50 = 45. In a right-to-left panel, bounds are the coordinates before XAML mirrors them (measured from the right edge), so
                // 100 - visual x - width gives a = 5, b = 41, c = 5 (on screen: WLayoutRightToLeftTest)
                bounds(s.a, s.b, s.c) shouldBe listOf(
                    WRectangle(5, 10, 31, 10),
                    WRectangle(41, 5, 40, 20),
                    WRectangle(5, 30, 50, 10),
                )
            } finally {
                UiTestHarness.detach(s.panel)
            }
        }

        test("changing hgap and vgap changes the gaps, and the insets are taken inside") {
            val layout = WFlowLayout(WFlowLayout.LEFT, 2.0, 3.0)
            val s = scene(layout)
            onUiThread { s.panel.insets = WInsets(top = 4.0, left = 6.0, bottom = 8.0, right = 10.0) }
            show(s)
            try {
                // maxwidth = 100 - (6 + 10 + 4) = 80. a 31, b 31 + 2 + 40 = 73 <= 80, c 73 + 2 + 50 > 80 so it wraps.
                // Row start x = 6 + 2 = 8, y = 4 + 3 = 7. a (8, 7 + 5 = 12), b (8 + 31 + 2 = 41, 7). Row 2 y = 7 + 3 + 20 = 30: c (8, 30)
                bounds(s.a, s.b, s.c) shouldBe listOf(
                    WRectangle(8, 12, 31, 10),
                    WRectangle(41, 7, 40, 20),
                    WRectangle(8, 30, 50, 10),
                )
                onUiThread {
                    layout.hgap = 10.0
                    layout.vgap = 1.0
                    s.panel.revalidate()
                    s.panel.validate()
                }
                layout.hgap shouldBe 10.0
                layout.vgap shouldBe 1.0
                // maxwidth = 100 - (6 + 10 + 20) = 64. a 31 + 10 + 40 = 81 > 64, so b is in row 2 and c in row 3.
                // Row start x = 6 + 10 = 16. y = 4 + 1 = 5: a (16, 5). Row 2 y = 5 + 1 + 10 = 16: b (16, 16). Row 3 y = 16 + 1 + 20 = 37: c (16, 37)
                bounds(s.a, s.b, s.c) shouldBe listOf(
                    WRectangle(16, 5, 31, 10),
                    WRectangle(16, 16, 40, 20),
                    WRectangle(16, 37, 50, 10),
                )
            } finally {
                UiTestHarness.detach(s.panel)
            }
        }

        test("hidden children are not laid out and the gap closes up") {
            val s = scene(WFlowLayout(WFlowLayout.LEFT))
            onUiThread { s.b.isVisible = false }
            show(s)
            try {
                // a 31, c 31 + 5 + 50 = 86 <= 90, so one row. Row height 10: a (5, 5), c (5 + 31 + 5 = 41, 5)
                bounds(s.a, s.c) shouldBe listOf(WRectangle(5, 5, 31, 10), WRectangle(41, 5, 50, 10))
            } finally {
                UiTestHarness.detach(s.panel)
            }
        }

        test("the preferred size is the sum of the visible children's widths and the hgaps between them, the maximum height, the insets, and the outer gaps (hgap x 2, vgap x 2)") {
            val layout = WFlowLayout()
            val s = scene(layout)
            onUiThread {
                s.panel.insets = WInsets(top = 1.0, left = 2.0, bottom = 3.0, right = 4.0)
                // Width 31 + 40 + 50 + 5 x 2 + (2 + 4 + 5 x 2) = 147, height 20 + (1 + 3 + 5 x 2) = 34
                layout.preferredLayoutSize(s.panel) shouldBe WSize(147.0, 34.0)
                s.b.isVisible = false
                // Width 31 + 50 + 5 + 16 = 102, height 10 + 14 = 24
                layout.preferredLayoutSize(s.panel) shouldBe WSize(102.0, 24.0)
                s.a.isVisible = false
                s.c.isVisible = false
                // Without children, width 16 and height 14 (only the outer gaps and insets)
                layout.preferredLayoutSize(s.panel) shouldBe WSize(16.0, 14.0)
            }
        }

        test("the minimum size sums the children's minimumSize with the same formula as the preferred size") {
            val layout = WFlowLayout()
            val s = scene(layout)
            onUiThread {
                s.a.minimumSize = WSize(3.0, 4.0)
                s.b.minimumSize = WSize(5.0, 6.0)
                s.c.minimumSize = WSize(7.0, 8.0)
                // Width 3 + 5 + 7 + 5 x 2 + 5 x 2 = 35, height 8 + 5 x 2 = 18
                layout.minimumLayoutSize(s.panel) shouldBe WSize(35.0, 18.0)
            }
        }

        test("the constants and defaults are the same as FlowLayout") {
            WFlowLayout.LEFT shouldBe 0
            WFlowLayout.CENTER shouldBe 1
            WFlowLayout.RIGHT shouldBe 2
            WFlowLayout.LEADING shouldBe 3
            WFlowLayout.TRAILING shouldBe 4
            val layout = WFlowLayout()
            layout.alignment shouldBe WFlowLayout.CENTER
            layout.hgap shouldBe 5.0
            layout.vgap shouldBe 5.0
            WFlowLayout(WFlowLayout.RIGHT).alignment shouldBe WFlowLayout.RIGHT
            WFlowLayout(WFlowLayout.RIGHT).hgap shouldBe 5.0
            layout.alignment = WFlowLayout.TRAILING
            layout.alignment shouldBe WFlowLayout.TRAILING
        }
    }
}
