package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests [WContainer.background] and hit testing of panels with mouse listeners.
 * JPanel receives mouse events over its whole bounds, but WinUI's Panel is hit only over its children when it has no Background.
 * Hit testing is checked with WinUI's own VisualTreeHelper.FindElementsInHostCoordinates ([HitTest]).
 */
class WContainerBackgroundTest : FunSpec() {
    /** A 200x100 panel without children (aligned to the top left). */
    private fun <T : WContainer> sized(panel: T): T = panel.also {
        it.width = 200.0
        it.height = 100.0
        it.horizontalAlignment = HorizontalAlignment.LEFT
        it.verticalAlignment = VerticalAlignment.TOP
    }

    /** Places [panel], finishes the layout, and calls [block] on the UI thread. */
    private fun <T> withShown(panel: WContainer, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            return onUiThreadGet {
                panel.updateLayout()
                block()
            }
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    private val noop = object : WMouseAdapter() {}

    init {
        test("background is null by default, reads back the color set, and null removes it") {
            onUiThreadGet {
                val panel = WPanel()
                val before = panel.background
                panel.background = WColor(10, 20, 30)
                val set = panel.background
                panel.background = WColor(1, 2, 3, 128)
                val translucent = panel.background
                panel.background = WColor(255, 255, 255, 0)
                val transparent = panel.background
                panel.background = null
                listOf(before, set, translucent, transparent, panel.background)
            } shouldBe listOf(null, WColor(10, 20, 30), WColor(1, 2, 3, 128), WColor(255, 255, 255, 0), null)
        }

        test("a panel without Background is not hit, and adding a mouse listener makes it hit over its whole bounds") {
            val panel = onUiThreadGet { sized(WPanel()) }
            withShown(panel) {
                val before = HitTest.hits(panel, 100.0, 50.0)
                panel.addMouseListener(noop)
                panel.updateLayout()
                listOf(before, HitTest.hits(panel, 100.0, 50.0), HitTest.hits(panel, 1.0, 1.0), HitTest.hits(panel, 199.0, 99.0))
            } shouldBe listOf(false, true, true, true)
        }

        test("mouse motion and wheel listeners also make it hit (for Canvas, Grid and WLayoutPanel alike)") {
            val motion = onUiThreadGet { sized(WCanvas()) }
            val wheel = onUiThreadGet { sized(WGrid()) }
            val layout = onUiThreadGet { sized(WLayoutPanel(WFlowLayout())) }
            onUiThread {
                motion.addMouseMotionListener(noop)
                wheel.addMouseWheelListener { }
                layout.addMouseListener(noop)
            }
            listOf(motion, wheel, layout).map { panel -> withShown(panel) { HitTest.hits(panel, 100.0, 50.0) } } shouldBe
                listOf(true, true, true)
        }

        test("the transparent brush for hit testing does not appear in background, and hit testing stays even when background is set to null") {
            val panel = onUiThreadGet { sized(WPanel()) }
            withShown(panel) {
                panel.addMouseListener(noop)
                val hidden = panel.background
                panel.background = WColor(255, 0, 0)
                val red = panel.background
                panel.background = null
                panel.updateLayout()
                listOf(hidden, red, panel.background, HitTest.hits(panel, 100.0, 50.0))
            } shouldBe listOf(null, WColor(255, 0, 0), null, true)
        }

        test("a panel with a color is hit even without listeners, and adding listeners keeps the color") {
            val panel = onUiThreadGet { sized(WPanel()).also { it.background = WColor(0, 0, 255) } }
            withShown(panel) {
                val hit = HitTest.hits(panel, 100.0, 50.0)
                panel.addMouseListener(noop)
                listOf(hit, panel.background)
            } shouldBe listOf(true, WColor(0, 0, 255))
        }

        test("adding a null listener does not change hit testing") {
            val panel = onUiThreadGet { sized(WPanel()) }
            withShown(panel) {
                panel.addMouseListener(null)
                panel.updateLayout()
                HitTest.hits(panel, 100.0, 50.0)
            } shouldBe false
        }
    }
}
