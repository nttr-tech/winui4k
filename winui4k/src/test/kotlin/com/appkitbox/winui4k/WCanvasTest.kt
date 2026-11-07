package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the absolute placement of [WCanvas] (JPanel with setLayout(null)-like).
 * Expected values come from Canvas's rule (child position = Canvas.Left / Canvas.Top as is; child size = the child's own Width / Height).
 */
@Tags("E2E")
class WCanvasTest : FunSpec() {
    private fun box(width: Double, height: Double): WCanvas = WCanvas().also {
        it.width = width
        it.height = height
    }

    private fun canvas(): WCanvas = WCanvas().also {
        it.width = 200.0
        it.height = 100.0
        it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
        it.verticalAlignmentInParent = VerticalAlignment.TOP
    }

    private fun <T> shown(canvas: WCanvas, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(canvas)
        try {
            onUiThread { canvas.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(canvas)
        }
    }

    init {
        test("a child added with add(c, x, y) is placed at those coordinates with its own size") {
            val (canvas, child) = onUiThreadGet {
                val child = box(40.0, 20.0)
                canvas().also { it.add(child, 12.0, 8.0) } to child
            }
            shown(canvas) { child.bounds } shouldBe WRectangle(12, 8, 40, 20)
        }

        test("a child added without coordinates is placed at the top-left (0, 0)") {
            val (canvas, child) = onUiThreadGet {
                val child = box(40.0, 20.0)
                canvas().also { it.add(child) } to child
            }
            shown(canvas) { child.location } shouldBe WPoint(0, 0)
        }

        test("setLocation moves a child, and changes after it is shown are reflected too") {
            val (canvas, child) = onUiThreadGet {
                val child = box(40.0, 20.0)
                canvas().also { it.add(child, 4.0, 4.0) } to child
            }
            shown(canvas) {
                val before = child.location
                canvas.setLocation(child, 60.0, 40.0)
                canvas.updateLayout()
                before to child.location
            } shouldBe (WPoint(4, 4) to WPoint(60, 40))
        }

        test("negative coordinates can be used (outside the parent's top-left)") {
            val (canvas, child) = onUiThreadGet {
                val child = box(40.0, 20.0)
                canvas().also { it.add(child, -8.0, -4.0) } to child
            }
            shown(canvas) { child.location } shouldBe WPoint(-8, -4)
        }

        test("each child has its own coordinates and can be placed at overlapping positions") {
            val (canvas, children) = onUiThreadGet {
                val children = listOf(box(40.0, 20.0), box(40.0, 20.0))
                canvas().also {
                    it.add(children[0], 20.0, 20.0)
                    it.add(children[1], 40.0, 28.0)
                } to children
            }
            shown(canvas) { children.map { it.location } } shouldBe listOf(WPoint(20, 20), WPoint(40, 28))
        }

        test("the Canvas keeps the size it was given regardless of the children's sizes") {
            val canvas = onUiThreadGet {
                canvas().also { it.add(box(400.0, 300.0), 0.0, 0.0) }
            }
            shown(canvas) { canvas.size } shouldBe WSize(200.0, 100.0)
        }
    }
}
