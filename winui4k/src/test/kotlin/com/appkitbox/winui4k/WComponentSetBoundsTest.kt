package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests [WComponent.setBounds] and the others, like Component.setBounds / setLocation / setSize.
 * Checks that, as in Swing, they become the position and size as is in parents with a null layout ([WCanvas], a [WLayoutPanel] without a layout manager),
 * are overwritten on the next layout in parents with a layout manager, and do not change the explicit size (width / height).
 * The position and size are read after layout with [WComponent.bounds] (the position relative to the parent and the actual size).
 */
@Tags("E2E")
class WComponentSetBoundsTest : FunSpec() {
    /** Places [child] inside a 400x300 [parent] and calls [block] on the UI thread after layout. */
    private fun <T> withChild(parent: WContainer, child: WComponent, block: () -> T): T {
        onUiThread {
            parent.width = 400.0
            parent.height = 300.0
            parent.horizontalAlignmentInParent = HorizontalAlignment.LEFT
            parent.verticalAlignmentInParent = VerticalAlignment.TOP
            parent.add(child)
        }
        UiTestHarness.attachAndAwaitLoaded(parent)
        try {
            return onUiThreadGet {
                parent.updateLayout()
                block()
            }
        } finally {
            UiTestHarness.detach(parent)
        }
    }

    init {
        test("setBounds of a WCanvas child becomes the position and size and does not change the explicit size (width / height)") {
            val label = onUiThreadGet { WLabel("Text") }
            withChild(onUiThreadGet { WCanvas() }, label) {
                label.setBounds(10.0, 20.0, 130.0, 40.0)
                label.updateLayout()
                listOf(label.bounds, label.width.isNaN(), label.height.isNaN())
            } shouldBe listOf(WRectangle(10, 20, 130, 40), true, true)
        }

        test("setLocation and setSize change the position and size separately") {
            val box = onUiThreadGet { WCanvas() }
            withChild(onUiThreadGet { WCanvas() }, box) {
                box.setSize(50.0, 60.0)
                box.updateLayout()
                val sized = box.bounds
                box.setLocation(5.0, 6.0)
                box.updateLayout()
                val moved = box.bounds
                box.setSize(70.0, 80.0)
                box.updateLayout()
                listOf(sized, moved, box.bounds)
            } shouldBe listOf(WRectangle(0, 0, 50, 60), WRectangle(5, 6, 50, 60), WRectangle(5, 6, 70, 80))
        }

        test("the forms taking WRectangle, WPoint and WSize are the same") {
            val box = onUiThreadGet { WCanvas() }
            withChild(onUiThreadGet { WCanvas() }, box) {
                box.setBounds(WRectangle(1, 2, 3, 4))
                box.updateLayout()
                val rectangle = box.bounds
                box.setLocation(WPoint(11, 12))
                box.setSize(WSize(13.0, 14.0))
                box.updateLayout()
                listOf(rectangle, box.bounds)
            } shouldBe listOf(WRectangle(1, 2, 3, 4), WRectangle(11, 12, 13, 14))
        }

        test("negative widths and heights become 0 (in Swing a negative size draws nothing)") {
            val box = onUiThreadGet { WCanvas() }
            withChild(onUiThreadGet { WCanvas() }, box) {
                box.setBounds(7.0, 8.0, -5.0, -1.0)
                box.updateLayout()
                box.bounds
            } shouldBe WRectangle(7, 8, 0, 0)
        }

        test("setBounds stays as is for a child of a WLayoutPanel without a layout manager") {
            val box = onUiThreadGet { WCanvas() }
            withChild(onUiThreadGet { WLayoutPanel(null) }, box) {
                box.setBounds(15.0, 25.0, 35.0, 45.0)
                (box.parent as WLayoutPanel).revalidate()
                box.parent!!.validate()
                box.updateLayout()
                box.bounds
            } shouldBe WRectangle(15, 25, 35, 45)
        }

        test("setBounds of a child of a WLayoutPanel with a layout manager takes effect immediately and is overwritten on the next layout") {
            val box = onUiThreadGet {
                WCanvas().also {
                    it.width = 31.0
                    it.height = 10.0
                }
            }
            withChild(onUiThreadGet { WLayoutPanel(WFlowLayout(WFlowLayout.LEFT)) }, box) {
                val panel = box.parent as WLayoutPanel
                panel.validate()
                val laidOut = box.bounds
                box.setBounds(100.0, 110.0, 120.0, 130.0)
                box.updateLayout()
                val direct = box.bounds
                panel.revalidate()
                panel.validate()
                box.updateLayout()
                // FlowLayout.LEFT: x = left margin 0 + hgap 5, y = vgap 5, the size is the preferred size 31x10
                listOf(laidOut, direct, box.bounds)
            } shouldBe listOf(WRectangle(5, 5, 31, 10), WRectangle(100, 110, 120, 130), WRectangle(5, 5, 31, 10))
        }

        test("for a child of a parent that lays out (WPanel) only the size takes effect and the position is ignored") {
            val box = onUiThreadGet { WCanvas().also { it.horizontalAlignmentInParent = HorizontalAlignment.LEFT } }
            withChild(onUiThreadGet { WPanel().also { it.spacing = 0.0 } }, box) {
                box.setBounds(30.0, 40.0, 50.0, 60.0)
                box.parent!!.updateLayout()
                box.bounds
            } shouldBe WRectangle(0, 0, 50, 60)
        }

        test("a position set with setLocation under a parent that lays out takes effect when moved into a WCanvas (the position is remembered, as in Swing)") {
            val box = onUiThreadGet { WCanvas() }
            val canvas = onUiThreadGet { WCanvas() }
            withChild(onUiThreadGet { WPanel() }, box) {
                box.setBounds(30.0, 40.0, 50.0, 60.0)
                (box.parent as WPanel).add(canvas)
                canvas.width = 200.0
                canvas.height = 200.0
                canvas.add(box)
                canvas.updateLayout()
                box.bounds
            } shouldBe WRectangle(30, 40, 50, 60)
        }
    }
}
