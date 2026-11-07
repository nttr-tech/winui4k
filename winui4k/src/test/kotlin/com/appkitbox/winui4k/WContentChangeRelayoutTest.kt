package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that verify [WLayoutPanel] lays out again by itself when the content (text, icon, font) changes after being shown
 * (the same as Swing's JLabel.setText / AbstractButton.setText / setIcon revalidating by themselves).
 *
 * The expected values are the sizes of reference components laid out with the same content from the start. Comparing with the reference makes the tests independent of fonts and DPI.
 */
@Tags("E2E")
class WContentChangeRelayoutTest : FunSpec() {
    /** Lays out [components] in a 600 x 200 panel with a left-aligned [WFlowLayout]. */
    private fun panelOf(vararg components: WComponent): WLayoutPanel = WLayoutPanel(WFlowLayout(WFlowLayout.LEFT)).also {
        it.width = PANEL_WIDTH
        it.height = PANEL_HEIGHT
        it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
        it.verticalAlignmentInParent = VerticalAlignment.TOP
        components.forEach(it::add)
    }

    /**
     * Shows [panel] and finishes laying it out, changes the content with [change], then advances the UI thread two turns without the user
     * calling revalidate before reading [measure].
     */
    private fun <T> afterChange(panel: WLayoutPanel, change: () -> Unit, measure: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(panel)
        try {
            repeat(2) { onUiThread { panel.updateLayout() } }
            onUiThread(change)
            repeat(2) { onUiThread { panel.updateLayout() } }
            return onUiThreadGet(measure)
        } finally {
            UiTestHarness.detach(panel)
        }
    }

    init {
        test("putting a long text into an empty WLabel after showing grows it to the reference size") {
            val (panel, label, sample) = onUiThreadGet {
                val label = WLabel()
                val sample = WLabel(LONG_TEXT)
                Triple(panelOf(label, sample), label, sample)
            }
            afterChange(panel, { label.text = LONG_TEXT }) { label.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }

        test("making the text of a WLabel longer after showing gives the reference size instead of keeping the previous width") {
            val (panel, label, sample) = onUiThreadGet {
                val label = WLabel("Start")
                val sample = WLabel(LONG_TEXT)
                Triple(panelOf(label, sample), label, sample)
            }
            afterChange(panel, { label.text = LONG_TEXT }) { label.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }

        test("making the text of a WLabel shorter after showing shrinks it to the reference size") {
            val (panel, label, sample) = onUiThreadGet {
                val label = WLabel(LONG_TEXT)
                val sample = WLabel("Start")
                Triple(panelOf(label, sample), label, sample)
            }
            afterChange(panel, { label.text = "Start" }) { label.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }

        test("emptying the text of a WLabel after showing makes its width 0") {
            val (panel, label) = onUiThreadGet {
                val label = WLabel("Start")
                panelOf(label) to label
            }
            afterChange(panel, { label.text = "" }) { label.size.width } shouldBe 0.0
        }

        test("changing the font size of a WLabel after showing gives the reference size") {
            val (panel, label, sample) = onUiThreadGet {
                val label = WLabel("Start")
                val sample = WLabel("Start").also { it.fontSize = BIG_FONT }
                Triple(panelOf(label, sample), label, sample)
            }
            afterChange(panel, { label.fontSize = BIG_FONT }) { label.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }

        test("adding an icon to a WLabel after showing gives the reference size") {
            val (panel, label, sample) = onUiThreadGet {
                val label = WLabel("Start")
                val sample = WLabel("Start", Symbol.CONTACT)
                Triple(panelOf(label, sample), label, sample)
            }
            afterChange(panel, { label.icon = Symbol.CONTACT }) { label.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }

        test("making the text of a WButton longer after showing gives the reference size") {
            val (panel, button, sample) = onUiThreadGet {
                val button = WButton("OK")
                val sample = WButton(LONG_TEXT)
                Triple(panelOf(button, sample), button, sample)
            }
            afterChange(panel, { button.text = LONG_TEXT }) { button.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }

        test("adding an icon to a WButton after showing gives the reference size") {
            val (panel, button, sample) = onUiThreadGet {
                val button = WButton("OK")
                val sample = WButton("OK").also { it.icon = Symbol.CONTACT }
                Triple(panelOf(button, sample), button, sample)
            }
            afterChange(panel, { button.icon = Symbol.CONTACT }) { button.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }

        test("making the text of a WCheckBox longer after showing gives the reference size") {
            val (panel, box, sample) = onUiThreadGet {
                val box = WCheckBox("A")
                val sample = WCheckBox(LONG_TEXT)
                Triple(panelOf(box, sample), box, sample)
            }
            afterChange(panel, { box.text = LONG_TEXT }) { box.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }

        test("changing the font size of a WButton after showing gives the reference size") {
            val (panel, button, sample) = onUiThreadGet {
                val button = WButton("OK")
                val sample = WButton("OK").also { it.font = WFont("Segoe UI", WFont.PLAIN, BIG_FONT) }
                Triple(panelOf(button, sample), button, sample)
            }
            afterChange(panel, { button.font = WFont("Segoe UI", WFont.PLAIN, BIG_FONT) }) {
                button.size to sample.size
            }.let { (actual, expected) -> actual shouldBe expected }
        }

        test("making the text of a WLabel in a WPanel longer makes the outer WLayoutPanel lay out again to the reference size") {
            val (panel, label, sample) = onUiThreadGet {
                val label = WLabel("Start")
                val sample = WLabel(LONG_TEXT)
                val inner = WPanel().also { it.add(label) }
                Triple(panelOf(inner, sample), label, sample)
            }
            afterChange(panel, { label.text = LONG_TEXT }) { label.size to sample.size }.let { (actual, expected) ->
                actual shouldBe expected
            }
        }
    }

    private companion object {
        const val PANEL_WIDTH = 600.0
        const val PANEL_HEIGHT = 200.0
        const val BIG_FONT = 28.0
        const val LONG_TEXT = "A much longer label text than before"
    }
}
