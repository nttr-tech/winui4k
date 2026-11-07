package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for reading and writing the orientation and spacing of [WPanel] (StackPanel), and the arrangement of its children.
 * The expected child positions are computed by hand from StackPanel's rule (children packed in the orientation's direction with the spacing between them).
 * The numbers are DIP multiples of 4, so the physical pixels are integers even at 125% scaling or more.
 */
@Tags("E2E")
class WPanelTest : FunSpec() {
    private fun box(width: Double, height: Double): WCanvas = WCanvas().also {
        it.width = width
        it.height = height
    }

    /** Puts [panel] aligned to the top-left, finishes layout, and then calls [block] on the UI thread. */
    private fun <T> shown(panel: WPanel, block: () -> T): T {
        onUiThread {
            panel.horizontalAlignmentInParent = HorizontalAlignment.LEFT
            panel.verticalAlignmentInParent = VerticalAlignment.TOP
        }
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            onUiThread { panel.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        test("the default is vertical with spacing 0") {
            onUiThreadGet {
                val panel = WPanel()
                Pair(panel.orientation, panel.spacing)
            } shouldBe Pair(Orientation.VERTICAL, 0.0)
        }

        test("the constructor's spacing and orientation can be read back and changed with setters") {
            onUiThreadGet {
                val panel = WPanel(8.0, Orientation.HORIZONTAL)
                val first = Pair(panel.orientation, panel.spacing)
                panel.orientation = Orientation.VERTICAL
                panel.spacing = 4.0
                Pair(first, Pair(panel.orientation, panel.spacing))
            } shouldBe Pair(
                Pair(Orientation.HORIZONTAL, 8.0),
                Pair(Orientation.VERTICAL, 4.0),
            )
        }

        test("with the constructor that specifies only the orientation, the spacing stays 0") {
            onUiThreadGet {
                val panel = WPanel(orientation = Orientation.HORIZONTAL)
                Pair(panel.orientation, panel.spacing)
            } shouldBe Pair(Orientation.HORIZONTAL, 0.0)
        }

        test("vertical stacks children from the top with spacing 4 between them (heights 10, 20, 30 give y = 0, 14, 38)") {
            val (panel, children) = onUiThreadGet {
                val children = listOf(box(40.0, 10.0), box(40.0, 20.0), box(40.0, 30.0))
                WPanel(4.0).also { p -> children.forEach(p::add) } to children
            }
            shown(panel) {
                children.map { it.bounds }
            } shouldBe listOf(
                WRectangle(0, 0, 40, 10),
                WRectangle(0, 14, 40, 20),
                WRectangle(0, 38, 40, 30),
            )
        }

        test("horizontal lines children up from the left with spacing 4 between them (widths 10, 20, 30 give x = 0, 14, 38)") {
            val (panel, children) = onUiThreadGet {
                val children = listOf(box(10.0, 20.0), box(20.0, 20.0), box(30.0, 20.0))
                WPanel(4.0, Orientation.HORIZONTAL).also { p -> children.forEach(p::add) } to children
            }
            shown(panel) {
                children.map { it.bounds }
            } shouldBe listOf(
                WRectangle(0, 0, 10, 20),
                WRectangle(14, 0, 20, 20),
                WRectangle(38, 0, 30, 20),
            )
        }

        test("changing the spacing and orientation after it is shown makes the arrangement follow") {
            val (panel, children) = onUiThreadGet {
                val children = listOf(box(20.0, 20.0), box(20.0, 20.0))
                WPanel().also { p -> children.forEach(p::add) } to children
            }
            shown(panel) {
                val vertical = children.map { it.location }
                panel.spacing = 8.0
                panel.updateLayout()
                val spaced = children.map { it.location }
                panel.orientation = Orientation.HORIZONTAL
                panel.updateLayout()
                val horizontal = children.map { it.location }
                Triple(vertical, spaced, horizontal)
            } shouldBe Triple(
                listOf(WPoint(0, 0), WPoint(0, 20)),
                listOf(WPoint(0, 0), WPoint(0, 28)),
                listOf(WPoint(0, 0), WPoint(28, 0)),
            )
        }

        test("the panel's own size is the total of the children (vertical: the largest width, and the sum of heights plus spacing)") {
            val panel = onUiThreadGet {
                WPanel(4.0).also {
                    it.add(box(40.0, 10.0))
                    it.add(box(60.0, 20.0))
                }
            }
            shown(panel) { panel.size } shouldBe WSize(60.0, 34.0)
        }
    }
}
