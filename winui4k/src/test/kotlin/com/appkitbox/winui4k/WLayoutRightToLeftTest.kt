package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that, in a right-to-left flow ([FlowDirection.RIGHT_TO_LEFT]) panel, children placed by layout managers
 * appear on screen at the same positions as in Swing.
 *
 * XAML draws the content of elements whose flow direction differs from their parent mirrored (CUIElement::GetShouldFlipRTL in microsoft-ui-xaml's uielement.cpp).
 * So instead of [WComponent.bounds] (logical coordinates within the parent), the child's left and right edges are converted to window content coordinates,
 * and how many DIP to the right of the panel's on-screen left edge it appears is measured. Expected values are Swing's computed values (laid out in non-mirrored screen coordinates).
 */
@Tags("Integration")
class WLayoutRightToLeftTest : FunSpec() {
    private fun box(width: Double, height: Double): WCanvas = WCanvas().also {
        it.width = width
        it.height = height
    }

    /** A right-to-left panel [width] wide × 100 high. */
    private fun rtlPanel(layout: WLayoutManager?, width: Double = 100.0): WLayoutPanel = WLayoutPanel(layout).also {
        it.width = width
        it.height = 100.0
        it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
        it.flowDirection = FlowDirection.RIGHT_TO_LEFT
    }

    private fun show(panel: WLayoutPanel) {
        UiTestHarness.attachAndAwaitLoaded(panel)
        onUiThread {
            panel.revalidate()
            panel.validate()
        }
    }

    /** The on-screen left x of [element] when its left and right edges are converted to window content coordinates (if mirrored, the right edge comes to the left). */
    private fun screenLeft(element: WComponent): Double {
        val (x0, _) = VisualHitTest.transform(element.uiElement, null, 0.0, 0.0)
        val (x1, _) = VisualHitTest.transform(element.uiElement, null, element.actualWidth, 0.0)
        return minOf(x0, x1)
    }

    /** How many DIP to the right of the on-screen left edge of [panel] [child] appears (rounded to an integer). */
    private fun visibleX(panel: WLayoutPanel, child: WComponent): Int =
        onUiThreadGet { Math.round(screenLeft(child) - screenLeft(panel)).toInt() }

    private inline fun withPanel(panel: WLayoutPanel, block: () -> Unit) {
        show(panel)
        try {
            block()
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        test("WFlowLayout's LEADING appears aligned right in right-to-left flow, with the first child at the right edge") {
            val a = onUiThreadGet { box(31.0, 10.0) }
            val b = onUiThreadGet { box(40.0, 20.0) }
            val panel = onUiThreadGet {
                rtlPanel(WFlowLayout(WFlowLayout.LEADING)).also {
                    it.add(a)
                    it.add(b)
                }
            }
            withPanel(panel) {
                // Swing: a is 100 - 5 - 31 = 64, b is 100 - (5 + 31 + 5) - 40 = 19
                visibleX(panel, a) shouldBe 64
                visibleX(panel, b) shouldBe 19
            }
        }

        test("WBorderLayout's LINE_START appears on the right in right-to-left flow, and WEST appears on the left regardless of direction") {
            val lineStart = onUiThreadGet { box(30.0, 10.0) }
            val center = onUiThreadGet { box(10.0, 10.0) }
            val panel = onUiThreadGet {
                rtlPanel(WBorderLayout(), width = 200.0).also {
                    it.add(lineStart, WBorderLayout.LINE_START)
                    it.add(center, WBorderLayout.CENTER)
                }
            }
            withPanel(panel) {
                // Swing: LINE_START is at the EAST position in right-to-left (200 - 30 = 170), and CENTER is the rest 0..170
                visibleX(panel, lineStart) shouldBe 170
                visibleX(panel, center) shouldBe 0
            }
            val west = onUiThreadGet { box(30.0, 10.0) }
            val panel2 = onUiThreadGet { rtlPanel(WBorderLayout(), width = 200.0).also { it.add(west, WBorderLayout.WEST) } }
            withPanel(panel2) {
                visibleX(panel2, west) shouldBe 0
            }
        }

        test("WGridLayout shows the first column at the right edge in right-to-left flow") {
            val cells = onUiThreadGet { List(2) { box(10.0, 10.0) } }
            val panel = onUiThreadGet {
                rtlPanel(WGridLayout(1, 2)).also { p -> cells.forEach { p.add(it) } }
            }
            withPanel(panel) {
                // Swing: cell width 50. The first child is at 100 - 50 = 50, the next at 0
                visibleX(panel, cells[0]) shouldBe 50
                visibleX(panel, cells[1]) shouldBe 0
            }
        }

        test("WBoxLayout's LINE_AXIS lines up from the right in right-to-left flow, and X_AXIS lines up from the left regardless of direction") {
            val line = onUiThreadGet { List(2) { box(20.0, 10.0).also { c -> c.maximumSize = WSize(20.0, 10.0) } } }
            val linePanel = onUiThreadGet {
                rtlPanel(null).also { p ->
                    p.layout = WBoxLayout(p, WBoxLayout.LINE_AXIS)
                    line.forEach { p.add(it) }
                }
            }
            withPanel(linePanel) {
                // Swing: in right-to-left, LINE_AXIS puts the first child at the right edge (100 - 20 = 80), and the next at 60
                visibleX(linePanel, line[0]) shouldBe 80
                visibleX(linePanel, line[1]) shouldBe 60
            }
            val x = onUiThreadGet { List(2) { box(20.0, 10.0).also { c -> c.maximumSize = WSize(20.0, 10.0) } } }
            val xPanel = onUiThreadGet {
                rtlPanel(null).also { p ->
                    p.layout = WBoxLayout(p, WBoxLayout.X_AXIS)
                    x.forEach { p.add(it) }
                }
            }
            withPanel(xPanel) {
                visibleX(xPanel, x[0]) shouldBe 0
                visibleX(xPanel, x[1]) shouldBe 20
            }
        }
    }
}
