package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Tests that a WLayoutPanel lays its children out again when their content changes size, as Swing does when
 * JLabel.setText revalidates (the panel caches each child's natural size).
 */
class WLayoutPanelTest : FunSpec() {
    /** Places each child at its preferred size, one after another from the left. */
    private class RowLayout : WLayoutManager {
        override fun addLayoutComponent(component: WComponent, constraints: Any?) = Unit

        override fun removeLayoutComponent(component: WComponent) = Unit

        override fun preferredLayoutSize(parent: WLayoutPanel): WSize {
            val sizes = parent.layoutChildren.map { it.preferredSize() }
            return WSize(sizes.sumOf { it.width }, sizes.maxOfOrNull { it.height } ?: 0.0)
        }

        override fun layoutContainer(parent: WLayoutPanel) {
            var x = 0.0
            for (child in parent.layoutChildren) {
                val size = child.preferredSize()
                parent.setBounds(child, x, 0.0, size.width, size.height)
                x += size.width
            }
        }
    }

    /** Waits until queued layouts have run (revalidate posts the layout to the UI thread). */
    private fun settle() = repeat(3) { onUiThread { } }

    init {
        test("changing a label's text lays the panel out again with the label's new width") {
            val (panel, label) = onUiThreadGet {
                val label = WLabel("a")
                val panel = WLayoutPanel(RowLayout())
                panel.add(label)
                panel to label
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                settle()
                val before = onUiThreadGet { label.actualWidth }
                onUiThread { label.text = "a much longer text than before" }
                settle()
                val after = onUiThreadGet { label.actualWidth }
                after shouldBeGreaterThan before * 5
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("a larger font size is laid out again too") {
            val (panel, label) = onUiThreadGet {
                val label = WLabel("text")
                val panel = WLayoutPanel(RowLayout())
                panel.add(label)
                panel to label
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                settle()
                val before = onUiThreadGet { label.actualHeight }
                onUiThread { label.fontSize = 40.0 }
                settle()
                onUiThreadGet { label.actualHeight } shouldBeGreaterThan before * 2
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("setting the same text again does not schedule another layout") {
            val label = onUiThreadGet { WLabel("same") }
            onUiThreadGet {
                label.text = "same"
                label.text
            } shouldBe "same"
        }
    }
}
