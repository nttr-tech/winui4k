package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
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

    /** Stacks the children at their preferred sizes from the top, each stretched to the panel's width. */
    private class ColumnLayout : WLayoutManager {
        override fun preferredLayoutSize(parent: WLayoutPanel): WSize {
            val sizes = parent.layoutChildren.map { it.preferredSize() }
            return WSize(sizes.maxOfOrNull { it.width } ?: 0.0, sizes.sumOf { it.height })
        }

        override fun layoutContainer(parent: WLayoutPanel) {
            var y = 0.0
            for (child in parent.layoutChildren) {
                val size = child.preferredSize()
                parent.setBounds(child, 0.0, y, parent.layoutSize.width, size.height)
                y += size.height
            }
        }
    }

    /** A panel of [rows] labels stacked by a [ColumnLayout]. */
    private fun column(rows: Int): WLayoutPanel = onUiThreadGet {
        WLayoutPanel(ColumnLayout()).also { panel ->
            repeat(rows) { panel.add(WLabel("Row $it of a form that is laid out by a WLayoutPanel")) }
        }
    }

    /** Shows [content] in a WContentDialog and runs [check] until it passes (each time after a layout pass), then hides it. */
    private suspend fun showInDialog(content: WComponent, check: () -> Unit) {
        val owner = onUiThreadGet { WLabel("owner") }
        UiTestHarness.attachAndAwaitLoaded(owner)
        val dialog = onUiThreadGet { WContentDialog("Form", content).also { it.closeButtonText = "Close" } }
        try {
            onUiThread { dialog.show(owner) }
            eventually(LAYOUT_TIMEOUT) {
                settle(dialog)
                check()
            }
        } finally {
            onUiThread { dialog.hide() }
            settle()
            UiTestHarness.detach(owner)
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

        test("a WLayoutPanel in a ScrollViewer gets its preferred height, so the viewer can scroll it") {
            val panel = column(TALL_ROWS)
            val scroll = onUiThreadGet {
                WScrollPane(panel).also {
                    it.width = VIEWPORT
                    it.height = VIEWPORT
                }
            }
            UiTestHarness.attachAndAwaitLoaded(scroll)
            try {
                eventually(LAYOUT_TIMEOUT) {
                    settle(scroll)
                    val (preferred, actual) = onUiThreadGet { panel.preferredSize().height to panel.actualHeight }
                    preferred shouldBeGreaterThan VIEWPORT * 2
                    actual shouldBe (preferred plusOrMinus 1.0)
                    onUiThreadGet { scroll.inspectable.getDouble(XamlInterop.IScrollViewer_get_ScrollableHeight) } shouldBeGreaterThan VIEWPORT
                }
            } finally {
                UiTestHarness.detach(scroll)
            }
        }

        test("a WLayoutPanel in a vertical StackPanel keeps its preferred size") {
            val panel = column(SHORT_ROWS)
            val stack = onUiThreadGet { WPanel().also { it.add(panel) } }
            UiTestHarness.attachAndAwaitLoaded(stack)
            try {
                eventually(LAYOUT_TIMEOUT) {
                    settle(stack)
                    val (preferred, actual) = onUiThreadGet { panel.preferredSize() to WSize(panel.actualWidth, panel.actualHeight) }
                    preferred.height shouldBeGreaterThan 0.0
                    actual.height shouldBe (preferred.height plusOrMinus 1.0)
                }
            } finally {
                UiTestHarness.detach(stack)
            }
        }

        test("a WLayoutPanel as the content of a WContentDialog gets its preferred size instead of being clipped") {
            val panel = column(SHORT_ROWS)
            showInDialog(panel) {
                val (preferred, actual) = onUiThreadGet { panel.preferredSize() to WSize(panel.actualWidth, panel.actualHeight) }
                preferred.height shouldBeGreaterThan 0.0
                actual.height shouldBe (preferred.height plusOrMinus 1.0)
                actual.width shouldBeGreaterThan preferred.width - 1.0
            }
        }

        test("a WLayoutPanel taller than a WContentDialog scrolls in a WScrollPane (the dialog itself does not scroll)") {
            val panel = column(TALL_ROWS)
            val scroll = onUiThreadGet { WScrollPane(panel) }
            showInDialog(scroll) {
                val (preferred, actual) = onUiThreadGet { panel.preferredSize().height to panel.actualHeight }
                actual shouldBe (preferred plusOrMinus 1.0)
                onUiThreadGet { scroll.inspectable.getDouble(XamlInterop.IScrollViewer_get_ScrollableHeight) } shouldBeGreaterThan 0.0
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

        /** Rows that fit in a dialog, and rows far taller than any window (and than [VIEWPORT]). */
        const val SHORT_ROWS = 6
        const val TALL_ROWS = 80

        /** The size of the viewport of the ScrollViewer test. */
        const val VIEWPORT = 100.0
    }
}
