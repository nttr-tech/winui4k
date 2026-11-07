package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the relative placement and panel-relative placement of [WRelativePanel] (RelativePanel) with the children's [WComponent.bounds].
 * Expected values are computed by hand from RelativePanel's rules (children are placed next to, at the edge of, or at the center of the reference element; a child with nothing specified goes to the top-left).
 * The numbers are DIP multiples of 4 (integers even when halved), so the physical pixels are integers even at 125% scaling or more.
 */
@Tags("E2E")
class WRelativePanelTest : FunSpec() {
    private fun box(width: Double, height: Double): WCanvas = WCanvas().also {
        it.width = width
        it.height = height
    }

    /** A 300 × 200 panel. */
    private fun panel(): WRelativePanel = WRelativePanel().also {
        it.width = 300.0
        it.height = 200.0
        it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
        it.verticalAlignmentInParent = VerticalAlignment.TOP
    }

    private fun <T> shown(panel: WRelativePanel, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            onUiThread { panel.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        test("a child with nothing specified is placed at the top-left") {
            val (panel, a) = onUiThreadGet {
                val a = box(60.0, 20.0)
                panel().also { it.add(a) } to a
            }
            shown(panel) { a.bounds } shouldBe WRectangle(0, 0, 60, 20)
        }

        test("placeRightOf goes right next to the reference (x = reference x + width), and placeBelow goes below it (y = reference y + height)") {
            val (panel, children) = onUiThreadGet {
                val a = box(60.0, 20.0)
                val right = box(40.0, 20.0)
                val below = box(40.0, 12.0)
                panel().also {
                    it.add(a)
                    it.add(right)
                    it.add(below)
                    it.placeRightOf(right, a)
                    it.placeBelow(below, a)
                } to listOf(right, below)
            }
            shown(panel) { children.map { it.location } } shouldBe listOf(WPoint(60, 0), WPoint(0, 20))
        }

        test("alignRightWithPanel and alignBottomWithPanel stick to the right and bottom edges, and alignHorizontalCenterWithPanel centers horizontally") {
            val (panel, children) = onUiThreadGet {
                val right = box(60.0, 20.0)
                val bottom = box(40.0, 20.0)
                val center = box(100.0, 20.0)
                panel().also {
                    it.add(right)
                    it.add(bottom)
                    it.add(center)
                    it.alignRightWithPanel(right)
                    it.alignBottomWithPanel(bottom)
                    it.alignHorizontalCenterWithPanel(center)
                } to listOf(right, bottom, center)
            }
            // Right edge: 300 - 60. Bottom edge: 200 - 20 (x stays left). Horizontal center: (300 - 100) / 2
            shown(panel) { children.map { it.location } } shouldBe listOf(WPoint(240, 0), WPoint(0, 180), WPoint(100, 0))
        }

        test("alignVerticalCenterWithPanel centers vertically, and alignLeftWithPanel and alignTopWithPanel stay at the top-left") {
            val (panel, children) = onUiThreadGet {
                val middle = box(60.0, 40.0)
                val corner = box(60.0, 40.0)
                panel().also {
                    it.add(middle)
                    it.add(corner)
                    it.alignVerticalCenterWithPanel(middle)
                    it.alignLeftWithPanel(corner)
                    it.alignTopWithPanel(corner)
                } to listOf(middle, corner)
            }
            shown(panel) { children.map { it.location } } shouldBe listOf(WPoint(0, 80), WPoint(0, 0))
        }

        test("placeLeftOf and placeAbove are placed to the left of and above a reference attached to the panel's bottom-right") {
            val (panel, children) = onUiThreadGet {
                val anchor = box(60.0, 20.0)
                val left = box(40.0, 20.0)
                val above = box(40.0, 12.0)
                panel().also {
                    it.add(anchor)
                    it.add(left)
                    it.add(above)
                    it.alignRightWithPanel(anchor)
                    it.alignBottomWithPanel(anchor)
                    it.placeLeftOf(left, anchor)
                    it.placeAbove(above, anchor)
                } to listOf(left, above)
            }
            // The reference is at (240, 180). Left of it is x = 240 - 40 = 200 with y staying at the top. Above it is y = 180 - 12 = 168 with x staying left
            shown(panel) { children.map { it.location } } shouldBe listOf(WPoint(200, 0), WPoint(0, 168))
        }

        test("alignLeftWith / alignTopWith align the left / top edges with the reference, and alignRightWith / alignBottomWith the right / bottom edges") {
            val (panel, children) = onUiThreadGet {
                val anchor = box(80.0, 40.0)
                val left = box(20.0, 12.0)
                val top = box(20.0, 12.0)
                val right = box(20.0, 12.0)
                val bottom = box(20.0, 12.0)
                panel().also {
                    listOf(anchor, left, top, right, bottom).forEach(it::add)
                    it.alignHorizontalCenterWithPanel(anchor)
                    it.alignVerticalCenterWithPanel(anchor)
                    it.alignLeftWith(left, anchor)
                    it.alignTopWith(top, anchor)
                    it.alignRightWith(right, anchor)
                    it.alignBottomWith(bottom, anchor)
                } to listOf(left, top, right, bottom)
            }
            // The reference is at x = (300 - 80) / 2 = 110, y = (200 - 40) / 2 = 80.
            // left's x = 110, top's y = 80, right's x = 110 + 80 - 20 = 170, bottom's y = 80 + 40 - 12 = 108 (the other coordinate stays at the top-left)
            shown(panel) { children.map { it.location } } shouldBe listOf(
                WPoint(110, 0),
                WPoint(0, 80),
                WPoint(170, 0),
                WPoint(0, 108),
            )
        }

        test("passing false to alignRightWithPanel clears it, and the child goes back to the top-left") {
            val (panel, a) = onUiThreadGet {
                val a = box(60.0, 20.0)
                panel().also {
                    it.add(a)
                    it.alignRightWithPanel(a)
                    it.alignRightWithPanel(a, false)
                } to a
            }
            shown(panel) { a.location } shouldBe WPoint(0, 0)
        }
    }
}
