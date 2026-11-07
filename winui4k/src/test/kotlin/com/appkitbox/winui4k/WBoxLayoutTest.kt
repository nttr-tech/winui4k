package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * Tests [WBoxLayout] and [WBox] against the behavior of javax.swing.BoxLayout / Box.
 * The expected child positions and sizes were calculated by hand with SizeRequirements' calculateTiledPositions (layout direction) and
 * calculateAlignedPositions (crossing direction) used by BoxLayout.layoutContainer (the calculations are left in each test's comments). Swing truncates to int, but
 * WBoxLayout calculates with DIP Doubles. An unbounded maximum size is treated as 32767, the same as Swing's Short.MAX_VALUE.
 */
@Tags("Integration")
class WBoxLayoutTest : FunSpec() {
    /** A box whose minimum, preferred and maximum are all [width] x [height] (neither grows nor shrinks). */
    private fun rigid(width: Double, height: Double): WCanvas = WCanvas().also {
        it.width = width
        it.height = height
        it.maximumSize = WSize(width, height)
    }

    /** Puts [children] into a [width] x [height] panel with a WBoxLayout along [axis]. */
    private fun box(axis: Int, width: Double, height: Double, vararg children: WComponent): WLayoutPanel {
        val panel = WLayoutPanel()
        panel.layout = WBoxLayout(panel, axis)
        panel.width = width
        panel.height = height
        panel.horizontalAlignmentInParent = HorizontalAlignment.LEFT
        panel.verticalAlignmentInParent = VerticalAlignment.TOP
        children.forEach { panel.add(it) }
        return panel
    }

    /** Places [panel], finishes the layout, and returns the positions and sizes of [components]. */
    private fun laidOut(panel: WLayoutPanel, vararg components: WComponent): List<WRectangle> {
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            return onUiThreadGet {
                panel.revalidate()
                panel.validate()
                panel.updateLayout()
                components.map { it.bounds }
            }
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        test("Y_AXIS stacks from the top, and the crossing direction (horizontal) is centered with alignmentX 0.5 on both") {
            val a = onUiThreadGet { rigid(50.0, 20.0) }
            val b = onUiThreadGet { rigid(80.0, 30.0) }
            // Vertical: preferred total 50 = maximum total 50, so no stretching: a (0, 20), b (20, 30).
            // Horizontal: with minimum = preferred, a is 25 above / 25 below and b is 40 / 40 -> the overall alignment is 40 / (40 + 40) = 0.5.
            // The upper part of the width 200 is 100: a is at offset 100 - 25 = 75, b at 100 - 40 = 60
            laidOut(onUiThreadGet { box(WBoxLayout.Y_AXIS, 200.0, 300.0, a, b) }, a, b) shouldBe listOf(
                WRectangle(75, 0, 50, 20),
                WRectangle(60, 20, 80, 30),
            )
        }

        test("X_AXIS lays out from the left and distributes spare space in proportion to how much each can grow up to its maximum (horizontal glue takes the spare space)") {
            val a = onUiThreadGet { rigid(50.0, 20.0) }
            val glue = onUiThreadGet { WBox.createHorizontalGlue() }
            val b = onUiThreadGet { rigid(60.0, 30.0) }
            // Horizontal: preferred 110, width 300, spare 190. Only the glue can grow (32767), so the glue takes 190 and b starts at 50 + 190 = 240.
            // Vertical: a is 10 / 10, b 15 / 15, the glue's maximum height is 0 -> alignment 0.5; from the upper part 50 of the height 100, a is at 40, b at 35, the glue at 50 with height 0
            laidOut(onUiThreadGet { box(WBoxLayout.X_AXIS, 300.0, 100.0, a, glue, b) }, a, glue, b) shouldBe listOf(
                WRectangle(0, 40, 50, 20),
                WRectangle(50, 50, 190, 0),
                WRectangle(240, 35, 60, 30),
            )
        }

        test("glue on both sides splits the spare space equally and centers the children between") {
            val left = onUiThreadGet { WBox.createHorizontalGlue() }
            val a = onUiThreadGet { rigid(50.0, 20.0) }
            val right = onUiThreadGet { WBox.createHorizontalGlue() }
            // The spare 250 - 50 = 200 is split between the two with growth margins of 32767 each, 100 each
            laidOut(onUiThreadGet { box(WBoxLayout.X_AXIS, 250.0, 20.0, left, a, right) }, a)[0].x shouldBe 100
        }

        test("without glue a child with an unbounded maximum size takes the spare space and stretches to fill the crossing direction") {
            val a = onUiThreadGet { rigid(50.0, 20.0) }
            val stretchy = onUiThreadGet {
                WCanvas().also {
                    it.width = 40.0
                    it.height = 10.0
                }
            }
            // Vertical: preferred 30, height 100, the spare 70 is taken only by stretchy with a growth margin of 32767 - 10, giving 80. Horizontal: stretchy has a maximum of 32767 and fills the width 200.
            // Horizontal alignment: a is 25 / 25 and stretchy 20 / 20 -> 0.5, upper part 100. a is at 75, stretchy is min(100, 16383.5) at offset 0 with width 200
            laidOut(onUiThreadGet { box(WBoxLayout.Y_AXIS, 200.0, 100.0, a, stretchy) }, a, stretchy) shouldBe listOf(
                WRectangle(75, 0, 50, 20),
                WRectangle(0, 20, 200, 80),
            )
        }

        test("when the allocation is less than the preferred size, children shrink in proportion to how much each can shrink down to its minimum size, but not below it") {
            fun shrinkable(minimum: Double) = WCanvas().also {
                it.width = 80.0
                it.height = 10.0
                it.minimumSize = WSize(minimum, 10.0)
                it.maximumSize = WSize(80.0, 10.0)
            }
            val a = onUiThreadGet { shrinkable(40.0) }
            val b = onUiThreadGet { shrinkable(20.0) }
            // Width 100: short by 60, shrink margins 40 + 60 = 100 -> factor 0.6. a is 80 - 0.6 x 40 = 56, b is 80 - 0.6 x 60 = 44
            laidOut(onUiThreadGet { box(WBoxLayout.X_AXIS, 100.0, 10.0, a, b) }, a, b) shouldBe listOf(
                WRectangle(0, 0, 56, 10),
                WRectangle(56, 0, 44, 10),
            )
            val c = onUiThreadGet { shrinkable(40.0) }
            val d = onUiThreadGet { shrinkable(20.0) }
            // Width 50: the shortfall 110 exceeds the shrink margin 100, so the minimum sizes 40 and 20 (overflowing)
            laidOut(onUiThreadGet { box(WBoxLayout.X_AXIS, 50.0, 10.0, c, d) }, c, d) shouldBe listOf(
                WRectangle(0, 0, 40, 10),
                WRectangle(40, 0, 20, 10),
            )
        }

        test("children without a minimum size set do not shrink below their preferred size, as with most Swing components") {
            val a = onUiThreadGet {
                WCanvas().also {
                    it.width = 80.0
                    it.height = 10.0
                    it.maximumSize = WSize(80.0, 10.0)
                }
            }
            // minimum = preferred 80, so there is no shrink margin, and it stays 80 even at width 50
            laidOut(onUiThreadGet { box(WBoxLayout.X_AXIS, 50.0, 10.0, a) }, a) shouldBe listOf(WRectangle(0, 0, 80, 10))
        }

        test("children with alignmentX 0 (left) and 1 (right) are placed by SizeRequirements' alignment formula") {
            val left = onUiThreadGet { rigid(50.0, 20.0).also { it.alignmentX = WComponent.LEFT_ALIGNMENT } }
            val right = onUiThreadGet { rigid(80.0, 30.0).also { it.alignmentX = WComponent.RIGHT_ALIGNMENT } }
            // left is 0 above / 50 below, right is 80 above / 0 below -> minimum total 130, alignment 80 / 130. The upper part of width 200 is 200 x 80 / 130 = 123.08:
            // left is at offset 123.08 - 0, right at 123.08 - 80 = 43.08
            val bounds = laidOut(onUiThreadGet { box(WBoxLayout.Y_AXIS, 200.0, 100.0, left, right) }, left, right)
            bounds shouldBe listOf(WRectangle(123, 0, 50, 20), WRectangle(43, 20, 80, 30))
            onUiThreadGet { left.actualX } shouldBe (200.0 * 80.0 / 130.0 plusOrMinus 0.01)
        }

        test("labels and buttons default to alignmentX 0 (left), as in Swing, and are not stretched beyond their preferred size unless a maximum size is set") {
            val label = onUiThreadGet { WLabel("Name") }
            val button = onUiThreadGet { WButton("OK") }
            val panel = onUiThreadGet { WCanvas() }
            onUiThreadGet { listOf(label.alignmentX, button.alignmentX, panel.alignmentX) } shouldBe listOf(0.0f, 0.0f, 0.5f)
            val layoutPanel = onUiThreadGet { box(WBoxLayout.Y_AXIS, 200.0, 200.0, label, button) }
            val bounds = laidOut(layoutPanel, label, button)
            // All have alignment 0, so the overall alignment is 0 too, the upper part 0, at the left edge. The width is the preferred width, like the maximum size (= preferred size) of JLabel / JButton
            val preferred = onUiThreadGet { listOf(label, button).map { Math.round(it.preferredSize.width).toInt() } }
            bounds.map { it.x } shouldBe listOf(0, 0)
            bounds.map { it.width } shouldBe preferred
            (preferred.max() < 200) shouldBe true
        }

        test("the insets are taken inside, and preferredLayoutSize / minimumLayoutSize / maximumLayoutSize include the insets") {
            val a = onUiThreadGet { rigid(50.0, 20.0) }
            val b = onUiThreadGet { rigid(80.0, 30.0) }
            val panel = onUiThreadGet {
                box(WBoxLayout.Y_AXIS, 200.0, 300.0, a, b).also { it.insets = WInsets(top = 1.0, left = 2.0, bottom = 3.0, right = 4.0) }
            }
            val layout = panel.layout as WBoxLayout
            // Preferred: horizontal is the aligned 40 + 40 = 80, vertical 20 + 30 = 50. The minimum is the same (minimum = preferred). The maximum is the same too since there is no growth margin
            onUiThreadGet {
                listOf(layout.preferredLayoutSize(panel), layout.minimumLayoutSize(panel), layout.maximumLayoutSize(panel))
            } shouldBe listOf(WSize(86.0, 54.0), WSize(86.0, 54.0), WSize(86.0, 54.0))
            // The upper part of the horizontal allocation 200 - 6 = 194 is 97: a is 2 + 97 - 25 = 74, b is 2 + 97 - 40 = 59. Vertically from the top 1
            laidOut(panel, a, b) shouldBe listOf(WRectangle(74, 1, 50, 20), WRectangle(59, 21, 80, 30))
        }

        test("hidden children take no space (laid out with size 0, as in Swing)") {
            val a = onUiThreadGet { rigid(50.0, 20.0) }
            val hidden = onUiThreadGet { rigid(50.0, 40.0).also { it.isVisible = false } }
            val c = onUiThreadGet { rigid(50.0, 10.0) }
            laidOut(onUiThreadGet { box(WBoxLayout.Y_AXIS, 50.0, 100.0, a, hidden, c) }, a, c) shouldBe listOf(
                WRectangle(0, 0, 50, 20),
                WRectangle(0, 20, 50, 10),
            )
        }

        test("LINE_AXIS lays out horizontally and from the right edge in a right-to-left flow; X_AXIS is from the left regardless of the flow") {
            fun scene(axis: Int): Triple<WLayoutPanel, WCanvas, WCanvas> = onUiThreadGet {
                val a = rigid(50.0, 20.0)
                val b = rigid(60.0, 20.0)
                Triple(box(axis, 300.0, 20.0, a, b).also { it.flowDirection = FlowDirection.RIGHT_TO_LEFT }, a, b)
            }
            // Visually (Swing), from the right edge 300, a is 300 - 50 = 250 and b is 250 - 60 = 190. In a right-to-left panel, bounds are
            // the coordinates before XAML mirrors them (measured from the right edge), so x = 300 - visual x - width gives a = 0 and b = 50.
            // The on-screen positions are checked by WLayoutRightToLeftTest
            val (line, a, b) = scene(WBoxLayout.LINE_AXIS)
            laidOut(line, a, b) shouldBe listOf(WRectangle(0, 0, 50, 20), WRectangle(50, 0, 60, 20))
            // X_AXIS is visually a = 0, b = 50 from the left. In bounds, 300 - 0 - 50 = 250 and 300 - 50 - 60 = 190
            val (x, c, d) = scene(WBoxLayout.X_AXIS)
            laidOut(x, c, d) shouldBe listOf(WRectangle(250, 0, 50, 20), WRectangle(190, 0, 60, 20))
        }

        test("PAGE_AXIS stacks vertically and mirrors the horizontal alignment in a right-to-left flow") {
            val left = onUiThreadGet { rigid(50.0, 20.0).also { it.alignmentX = WComponent.LEFT_ALIGNMENT } }
            val panel = onUiThreadGet {
                box(WBoxLayout.PAGE_AXIS, 200.0, 100.0, left).also { it.flowDirection = FlowDirection.RIGHT_TO_LEFT }
            }
            // Mirrored alignment 1 - 0 = 1: with the overall alignment 1 - 0 = 1, the upper side is 200 and the child is top 50 / bottom 0 → visual offset 200 - 50 = 150.
            // bounds are measured from the right edge, so 200 - 150 - 50 = 0
            laidOut(panel, left) shouldBe listOf(WRectangle(0, 0, 50, 20))
        }

        test("WBox struts, rigid areas and glue have the same minimum, preferred and maximum sizes as Swing") {
            onUiThreadGet {
                listOf(
                    WBox.createHorizontalStrut(7.0),
                    WBox.createVerticalStrut(8.0),
                    WBox.createRigidArea(WSize(3.0, 4.0)),
                    WBox.createGlue(),
                    WBox.createHorizontalGlue(),
                    WBox.createVerticalGlue(),
                ).map { listOf(it.minimumSize, it.preferredSize, it.maximumSize) }
            } shouldBe listOf(
                listOf(WSize(7.0, 0.0), WSize(7.0, 0.0), WSize(7.0, 32767.0)),
                listOf(WSize(0.0, 8.0), WSize(0.0, 8.0), WSize(32767.0, 8.0)),
                listOf(WSize(3.0, 4.0), WSize(3.0, 4.0), WSize(3.0, 4.0)),
                listOf(WSize(0.0, 0.0), WSize(0.0, 0.0), WSize(32767.0, 32767.0)),
                listOf(WSize(0.0, 0.0), WSize(0.0, 0.0), WSize(32767.0, 0.0)),
                listOf(WSize(0.0, 0.0), WSize(0.0, 0.0), WSize(0.0, 32767.0)),
            )
        }

        test("struts make a fixed gap, and changing the shape with Filler's changeShape rearranges") {
            val a = onUiThreadGet { rigid(20.0, 10.0) }
            val strut = onUiThreadGet { WBox.createVerticalStrut(15.0) }
            val b = onUiThreadGet { rigid(20.0, 10.0) }
            val panel = onUiThreadGet { box(WBoxLayout.Y_AXIS, 20.0, 100.0, a, strut, b) }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThreadGet {
                    panel.validate()
                    panel.updateLayout()
                    val before = b.bounds
                    strut.changeShape(WSize(0.0, 30.0), WSize(0.0, 30.0), WSize(32767.0, 30.0))
                    panel.validate()
                    panel.updateLayout()
                    listOf(before, b.bounds)
                } shouldBe listOf(WRectangle(0, 25, 20, 10), WRectangle(0, 40, 20, 10))
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("createVerticalBox / createHorizontalBox are WLayoutPanels with a WBoxLayout, and nested boxes pass the inner alignment outward") {
            val outer = onUiThreadGet { WBox.createVerticalBox() }
            val inner = onUiThreadGet { WBox.createHorizontalBox() }
            onUiThreadGet {
                val outerLayout = outer.layout as WBoxLayout
                val innerLayout = inner.layout as WBoxLayout
                listOf(outerLayout.axis, outerLayout.target === outer, innerLayout.axis, innerLayout.target === inner)
            } shouldBe listOf(WBoxLayout.Y_AXIS, true, WBoxLayout.X_AXIS, true)
            // The horizontal box's alignmentX is its children's alignment (0 if it has only LEFT children; the result of SizeRequirements.getAlignedSizeRequirements).
            // However, the alignment along the horizontal layout direction is getTiledSizeRequirements' default 0.5
            onUiThreadGet {
                val vertical = WBox.createVerticalBox()
                vertical.add(rigid(10.0, 10.0).also { it.alignmentX = WComponent.LEFT_ALIGNMENT })
                listOf(vertical.alignmentX, vertical.alignmentY)
            } shouldBe listOf(0.0f, 0.5f)
        }

        test("use with another panel and out-of-range axes throw IllegalArgumentException, and alignmentX / Y are clamped to 0..1") {
            onUiThread {
                val panel = WLayoutPanel()
                shouldThrow<IllegalArgumentException> { WBoxLayout(panel, 4) }
                shouldThrow<IllegalArgumentException> { WBoxLayout(panel, -1) }
                val layout = WBoxLayout(panel, WBoxLayout.Y_AXIS)
                val other = WLayoutPanel()
                shouldThrow<IllegalArgumentException> { layout.preferredLayoutSize(other) }
                shouldThrow<IllegalArgumentException> { layout.layoutContainer(other) }
                val box = WCanvas()
                box.alignmentX = 2.0f
                box.alignmentY = -1.0f
                listOf(box.alignmentX, box.alignmentY) shouldBe listOf(1.0f, 0.0f)
            }
        }
    }
}
