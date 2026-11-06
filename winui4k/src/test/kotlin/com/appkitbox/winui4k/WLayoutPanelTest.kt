package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the self-sizing of [WLayoutPanel] (its size inside a WPanel that does not hand down a size) and the collapsing of revalidate within the same turn.
 * Expected values come from the promises in [WLayoutPanel]'s KDoc (without an assigned size, preferredLayoutSize becomes the size; an explicit width / height takes precedence;
 * however many times revalidate is called in a turn, it collapses into one layoutContainer).
 */
class WLayoutPanelTest : FunSpec() {
    /** A manager whose preferred size can be changed, recording the number of layoutContainer calls and the layoutSize at that time. All children are placed at (10, 20, 30, 40). */
    private class ProbeLayout(var preferred: WSize) : WLayoutManager {
        var layoutCount = 0
        val layoutSizes = mutableListOf<WSize>()

        override fun preferredLayoutSize(parent: WLayoutPanel): WSize = preferred

        override fun layoutContainer(parent: WLayoutPanel) {
            layoutCount++
            layoutSizes += parent.layoutSize
            parent.layoutChildren.forEach { parent.setBounds(it, 10.0, 20.0, 30.0, 40.0) }
        }
    }

    private fun child(): WCanvas = WCanvas()

    /** Puts [host] aligned to the top-left and runs the scheduled layouts twice. */
    private fun <T> shown(host: WPanel, block: () -> T): T {
        onUiThread {
            host.horizontalAlignment = HorizontalAlignment.LEFT
            host.verticalAlignment = VerticalAlignment.TOP
        }
        UiTestHarness.attachAndAwaitLoaded(host)
        try {
            repeat(2) { onUiThread { host.validate() } }
            onUiThread { host.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(host)
        }
    }

    init {
        test("inside a WPanel that does not hand down a size, it becomes the preferredLayoutSize (120 × 48)") {
            val (host, panel) = onUiThreadGet {
                val panel = WLayoutPanel(ProbeLayout(WSize(120.0, 48.0))).also { it.add(child()) }
                WPanel().also { it.add(panel) } to panel
            }
            shown(host) { panel.size } shouldBe WSize(120.0, 48.0)
        }

        test("while self-sizing, changing the preferred size and calling revalidate follows that size") {
            val (host, panel, layout) = onUiThreadGet {
                val layout = ProbeLayout(WSize(120.0, 48.0))
                val panel = WLayoutPanel(layout).also { it.add(child()) }
                Triple(WPanel().also { it.add(panel) }, panel, layout)
            }
            shown(host) {
                val before = panel.size
                layout.preferred = WSize(160.0, 64.0)
                panel.revalidate()
                host.validate()
                host.updateLayout()
                before to panel.size
            } shouldBe (WSize(120.0, 48.0) to WSize(160.0, 64.0))
        }

        test("when the width and height are explicit, they become the size instead of the preferred size, and so does layoutSize") {
            val (host, panel, layout) = onUiThreadGet {
                val layout = ProbeLayout(WSize(120.0, 48.0))
                val panel = WLayoutPanel(layout).also {
                    it.width = 200.0
                    it.height = 100.0
                    it.add(child())
                }
                Triple(WPanel().also { it.add(panel) }, panel, layout)
            }
            shown(host) { panel.size to layout.layoutSizes.last() } shouldBe (WSize(200.0, 100.0) to WSize(200.0, 100.0))
        }

        test("the position and size the manager assigned with setBounds become the child's bounds") {
            val (host, kid) = onUiThreadGet {
                val kid = child()
                val panel = WLayoutPanel(ProbeLayout(WSize(120.0, 48.0))).also { it.add(kid) }
                WPanel().also { it.add(panel) } to kid
            }
            shown(host) { kid.bounds } shouldBe WRectangle(10, 20, 30, 40)
        }

        test("setting layout later relayouts it and places the children") {
            val (host, panel, kid) = onUiThreadGet {
                val kid = child()
                val panel = WLayoutPanel().also {
                    it.width = 200.0
                    it.height = 100.0
                    it.add(kid)
                }
                Triple(WPanel().also { it.add(panel) }, panel, kid)
            }
            shown(host) {
                panel.layout = ProbeLayout(WSize(120.0, 48.0))
                host.validate()
                host.updateLayout()
                kid.bounds
            } shouldBe WRectangle(10, 20, 30, 40)
        }

        test("calling revalidate 5 times in the same turn collapses into one layoutContainer") {
            val (host, panel, layout) = onUiThreadGet {
                val layout = ProbeLayout(WSize(120.0, 48.0))
                val panel = WLayoutPanel(layout).also { it.add(child()) }
                Triple(WPanel().also { it.add(panel) }, panel, layout)
            }
            shown(host) { }
            val before = onUiThreadGet { layout.layoutCount }
            onUiThread {
                repeat(5) { panel.revalidate() }
                layout.layoutCount shouldBe before // It does not lay out in the turn it was called
            }
            onUiThread { layout.layoutCount shouldBe before + 1 }
            onUiThread { layout.layoutCount shouldBe before + 1 } // It does not keep running after that
        }

        test("adding children schedules a relayout that runs once (once even for three added)") {
            val (panel, layout) = onUiThreadGet {
                val layout = ProbeLayout(WSize(120.0, 48.0))
                WLayoutPanel(layout) to layout
            }
            onUiThread { } // Finish the relayout scheduled at creation
            val before = onUiThreadGet { layout.layoutCount }
            onUiThread { repeat(3) { panel.add(child()) } }
            onUiThread { layout.layoutCount shouldBe before + 1 }
        }

        test("a scheduled revalidate is done on the spot by validate, and the scheduled one no longer runs") {
            val (host, panel, layout) = onUiThreadGet {
                val layout = ProbeLayout(WSize(120.0, 48.0))
                val panel = WLayoutPanel(layout).also { it.add(child()) }
                Triple(WPanel().also { it.add(panel) }, panel, layout)
            }
            shown(host) { }
            val before = onUiThreadGet { layout.layoutCount }
            onUiThread {
                repeat(3) { panel.revalidate() }
                host.validate()
                layout.layoutCount shouldBe before + 1
            }
            onUiThread { layout.layoutCount shouldBe before + 1 }
        }
    }
}
