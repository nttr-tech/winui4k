package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that verify [WRelativePanel] throws IllegalArgumentException where position constraints referring to each other are made (WinUI's RelativePanel throws on the UI thread when laying them out),
 * and removes the position constraints when an anchor child is removed.
 * The expected values come from RelativePanel's rules (MeasureNode in RPGraph.cpp: regardless of the axis, following the anchors back to itself is a cycle) and from
 * children without constraints being placed at the top left.
 */
@Tags("E2E")
class WRelativePanelCycleTest : FunSpec() {
    private fun box(): WCanvas = WCanvas().also {
        it.width = 40.0
        it.height = 20.0
    }

    /** A 300 x 200 panel with three boxes. */
    private fun panel(): Pair<WRelativePanel, List<WCanvas>> {
        val boxes = listOf(box(), box(), box())
        val panel = WRelativePanel().also {
            it.width = 300.0
            it.height = 200.0
            it.horizontalAlignment = HorizontalAlignment.LEFT
            it.verticalAlignment = VerticalAlignment.TOP
            boxes.forEach(it::add)
        }
        return panel to boxes
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
        test("calls that make two children anchor each other (even on different axes) throw IllegalArgumentException, and the panel can be laid out") {
            val (panel, boxes) = onUiThreadGet { panel() }
            val (a, b) = boxes
            onUiThread {
                panel.placeRightOf(a, b)
                shouldThrow<IllegalArgumentException> { panel.placeBelow(b, a) }
            }
            shown(panel) { a.bounds to b.bounds } shouldBe (WRectangle(40, 0, 40, 20) to WRectangle(0, 0, 40, 20))
        }

        test("a cycle going back through three children also throws IllegalArgumentException") {
            onUiThreadGet {
                val (panel, boxes) = panel()
                val (a, b, c) = boxes
                panel.placeRightOf(a, b)
                panel.alignTopWith(b, c)
                shouldThrow<IllegalArgumentException> { panel.alignHorizontalCenterWith(c, a) }
                true
            } shouldBe true
        }

        test("making a child its own anchor throws IllegalArgumentException") {
            onUiThreadGet {
                val (panel, boxes) = panel()
                shouldThrow<IllegalArgumentException> { panel.placeBelow(boxes[0], boxes[0]) }
                true
            } shouldBe true
        }

        test("after replacing the same position constraint with another anchor, the reference to the previous anchor is not counted in cycles") {
            val (panel, boxes) = onUiThreadGet { panel() }
            val (a, b, c) = boxes
            onUiThread {
                panel.placeRightOf(a, b)
                panel.placeRightOf(a, c) // Change the RightOf of a to c
                panel.placeBelow(b, a) // b -> a -> c is not a cycle
            }
            shown(panel) { b.bounds } shouldBe WRectangle(0, 20, 40, 20)
        }

        test("removing an anchor child also removes the position constraints based on it, and they are placed at the top left (no exception when laid out)") {
            val (panel, boxes) = onUiThreadGet { panel() }
            val (a, b) = boxes
            onUiThread {
                panel.placeRightOf(a, b)
                panel.remove(b)
            }
            shown(panel) { a.bounds } shouldBe WRectangle(0, 0, 40, 20)
        }
    }
}
