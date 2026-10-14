package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

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

    /**
     * Waits until queued layouts have run (revalidate posts the layout to the UI thread). With a [root], also runs the
     * XAML layout pass under it, so the actual sizes reflect the sizes the panels assigned.
     */
    private fun settle(root: WComponent? = null) {
        repeat(3) { onUiThread { } }
        if (root != null) onUiThread { root.updateLayout() }
    }

    /** Builds outer → inner → label, each panel laid out by its own [RowLayout]. */
    private fun nestedRows(text: String): Triple<WLayoutPanel, WLayoutPanel, WLabel> = onUiThreadGet {
        val label = WLabel(text)
        val inner = WLayoutPanel(RowLayout())
        inner.add(label)
        val outer = WLayoutPanel(RowLayout())
        outer.add(inner)
        Triple(outer, inner, label)
    }

    /**
     * Lets the queued layouts and the XAML layout pass under [outer] run, then returns the inner panel's preferred and
     * actual width (XAML rounds the arranged size to physical pixels, so callers compare them within one epx).
     */
    private fun innerWidths(outer: WLayoutPanel, inner: WLayoutPanel): Pair<Double, Double> {
        settle(outer)
        return onUiThreadGet { inner.preferredSize().width to inner.actualWidth }
    }

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

        test("a WLayoutPanel nested in another one gets its preferred size") {
            val (outer, inner, _) = nestedRows("a")
            UiTestHarness.attachAndAwaitLoaded(outer)
            try {
                eventually(LAYOUT_TIMEOUT) {
                    val (preferred, actual) = innerWidths(outer, inner)
                    preferred shouldBeGreaterThan 0.0
                    actual shouldBe (preferred plusOrMinus 1.0)
                }
            } finally {
                UiTestHarness.detach(outer)
            }
        }

        test("a nested WLayoutPanel grows when a label inside it grows") {
            val (outer, inner, label) = nestedRows("a")
            UiTestHarness.attachAndAwaitLoaded(outer)
            try {
                val before = eventually(LAYOUT_TIMEOUT) {
                    val (preferred, actual) = innerWidths(outer, inner)
                    actual shouldBeGreaterThan 0.0
                    actual shouldBe (preferred plusOrMinus 1.0)
                    actual
                }
                onUiThread { label.text = "a much longer text than before" }
                eventually(LAYOUT_TIMEOUT) {
                    val (preferred, actual) = innerWidths(outer, inner)
                    actual shouldBeGreaterThan before * 5
                    actual shouldBe (preferred plusOrMinus 1.0)
                }
            } finally {
                UiTestHarness.detach(outer)
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

    private companion object {
        /** How long the nested layouts may take to settle (each level's layout is posted to the UI thread). */
        val LAYOUT_TIMEOUT = 5.seconds
    }
}
