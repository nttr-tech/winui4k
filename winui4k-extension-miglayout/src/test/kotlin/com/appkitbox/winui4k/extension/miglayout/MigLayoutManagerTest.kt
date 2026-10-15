package com.appkitbox.winui4k.extension.miglayout

import com.appkitbox.winui4k.UiTestHarness
import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WContentDialog
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WLayoutPanel
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WTextField
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/** E2E tests of [MigLayoutManager] laying out real WinUI controls, including MigLayout forms nested in each other. */
class MigLayoutManagerTest : FunSpec() {
    /** Builds outer → inner → label, each panel laid out by its own [MigLayoutManager]. */
    private fun nestedForm(text: String): Triple<WLayoutPanel, WLayoutPanel, WLabel> = onUiThreadGet {
        val label = WLabel(text)
        val inner = WLayoutPanel(MigLayoutManager())
        inner.add(label)
        val outer = WLayoutPanel(MigLayoutManager())
        outer.add(inner)
        Triple(outer, inner, label)
    }

    /**
     * Lets the queued layouts and the XAML layout pass under [outer] run, then returns the inner panel's preferred and
     * actual width (XAML rounds the arranged size to physical pixels, so callers compare them within one epx).
     */
    private fun innerWidths(outer: WLayoutPanel, inner: WLayoutPanel): Pair<Double, Double> {
        repeat(3) { onUiThread { } }
        onUiThread { outer.updateLayout() }
        return onUiThreadGet { inner.preferredSize().width to inner.actualWidth }
    }

    /** Lets the queued layouts and the XAML layout pass under [root] run. */
    private fun settle(root: WComponent) {
        repeat(3) { onUiThread { } }
        onUiThread { root.updateLayout() }
    }

    init {
        test("a MigLayout panel nested in another one gets its preferred size") {
            val (outer, inner, _) = nestedForm("a")
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

        test("a nested MigLayout panel follows a label inside it as it grows and shrinks") {
            val (outer, inner, label) = nestedForm("a")
            UiTestHarness.attachAndAwaitLoaded(outer)
            try {
                val before = eventually(LAYOUT_TIMEOUT) {
                    val (preferred, actual) = innerWidths(outer, inner)
                    actual shouldBeGreaterThan 0.0
                    actual shouldBe (preferred plusOrMinus 1.0)
                    actual
                }
                onUiThread { label.text = "a much longer text than before" }
                val grown = eventually(LAYOUT_TIMEOUT) {
                    val (preferred, actual) = innerWidths(outer, inner)
                    actual shouldBeGreaterThan before * 3
                    actual shouldBe (preferred plusOrMinus 1.0)
                    actual
                }
                onUiThread { label.text = "b" }
                eventually(LAYOUT_TIMEOUT) {
                    val (preferred, actual) = innerWidths(outer, inner)
                    actual shouldBeLessThan grown / 3
                    actual shouldBe (preferred plusOrMinus 1.0)
                }
            } finally {
                UiTestHarness.detach(outer)
            }
        }
        test("a MigLayout form taller than a WContentDialog scrolls in a WScrollPane") {
            val (panel, scroll) = onUiThreadGet {
                val panel = WLayoutPanel(MigLayoutManager("wrap 2", "[right][grow, fill]"))
                repeat(TALL_ROWS) {
                    panel.add(WLabel("Field $it:"))
                    panel.add(WTextField())
                }
                panel to WScrollPane(panel)
            }
            val owner = onUiThreadGet { WLabel("owner") }
            UiTestHarness.attachAndAwaitLoaded(owner)
            val dialog = onUiThreadGet { WContentDialog("Tall form", scroll).also { it.closeButtonText = "Close" } }
            try {
                onUiThread { dialog.show(owner) }
                eventually(LAYOUT_TIMEOUT) {
                    settle(dialog)
                    val (preferred, actual) = onUiThreadGet { panel.preferredSize().height to panel.actualHeight }
                    preferred shouldBeGreaterThan 0.0
                    actual shouldBe (preferred plusOrMinus 1.0)
                    onUiThreadGet { scroll.scrollableHeight } shouldBeGreaterThan 0.0
                }
            } finally {
                onUiThread { dialog.hide() }
                repeat(3) { onUiThread { } }
                UiTestHarness.detach(owner)
            }
        }
    }

    private companion object {
        /** How long the nested layouts may take to settle (each level's layout is posted to the UI thread). */
        val LAYOUT_TIMEOUT = 5.seconds

        /** Rows of a form far taller than any window. */
        const val TALL_ROWS = 60
    }
}
