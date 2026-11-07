package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that check whether WTextPane's wrapping (JTextArea.setLineWrap / setWrapStyleWord) actually wraps lines when shown, using the heights of
 * text panes with the same width and text stacked vertically. A wrapping pane is taller by the extra lines, and a non-wrapping one stays one line tall.
 */
@Tags("E2E")
class WTextPaneWrapTest : FunSpec() {
    /** Shows text panes stacked vertically, each [PANE_WIDTH] wide with [LONG_TEXT] and [configure] applied. */
    private fun shownPanes(vararg configure: WTextPane.() -> Unit): Pair<WPanel, List<WTextPane>> {
        val shown = onUiThreadGet {
            val panel = WPanel()
            val panes = configure.map { setUp ->
                WTextPane().apply {
                    width = PANE_WIDTH
                    text = LONG_TEXT
                    panel.add(this)
                    setUp()
                }
            }
            panel to panes
        }
        UiTestHarness.attachAndAwaitLoaded(shown.first)
        return shown
    }

    private fun heightsOf(panel: WPanel, panes: List<WTextPane>): List<Double> = onUiThreadGet {
        panel.updateLayout()
        panes.map { it.actualHeight }
    }

    init {
        test("a pane with nothing set does not wrap, as with JTextArea, has the same height as a pane with lineWrap = false, and is lower than a pane with lineWrap = true") {
            val (panel, panes) = shownPanes({}, { lineWrap = false }, { lineWrap = true })
            try {
                eventually(3.seconds) {
                    val (byDefault, noWrap, wrap) = heightsOf(panel, panes)
                    byDefault shouldBe (noWrap plusOrMinus TOLERANCE)
                    wrap shouldBeGreaterThan byDefault * 2
                }
                onUiThreadGet { panes[0].lineWrap to panes[0].textWrapping } shouldBe (false to TextWrapping.NO_WRAP)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("a pane with lineWrap and wrapStyleWord true wraps into as many lines as a wrapping pane without wrapStyleWord, and is taller than a non-wrapping pane") {
            val (panel, panes) = shownPanes(
                { lineWrap = false },
                { lineWrap = true },
                {
                    lineWrap = true
                    wrapStyleWord = true
                },
            )
            try {
                eventually(3.seconds) {
                    val (noWrap, wrap, wrapWord) = heightsOf(panel, panes)
                    wrap shouldBeGreaterThan noWrap * 2
                    wrapWord shouldBe (wrap plusOrMinus TOLERANCE)
                }
                onUiThreadGet { panes[2].lineWrap to panes[2].wrapStyleWord } shouldBe (true to true)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("setting wrapStyleWord to true after showing keeps wrapping, and setting it back to false also wraps") {
            val (panel, panes) = shownPanes({ lineWrap = true }, { lineWrap = true })
            try {
                val reference = eventually(3.seconds) { heightsOf(panel, panes).also { it[0] shouldBeGreaterThan 0.0 }[0] }
                onUiThreadGet { panes[1].wrapStyleWord = true }
                eventually(3.seconds) { heightsOf(panel, panes)[1] shouldBe (reference plusOrMinus TOLERANCE) }
                onUiThreadGet { panes[1].wrapStyleWord = false }
                eventually(3.seconds) { heightsOf(panel, panes)[1] shouldBe (reference plusOrMinus TOLERANCE) }
                onUiThreadGet { panes[1].lineWrap to panes[1].wrapStyleWord } shouldBe (true to false)
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("putting WRAP_WHOLE_WORDS directly into textWrapping also wraps, and lineWrap and wrapStyleWord become true") {
            val (panel, panes) = shownPanes({ lineWrap = true }, { textWrapping = TextWrapping.WRAP_WHOLE_WORDS })
            try {
                eventually(3.seconds) {
                    val (wrap, wholeWords) = heightsOf(panel, panes)
                    wholeWords shouldBe (wrap plusOrMinus TOLERANCE)
                }
                onUiThreadGet { panes[1].lineWrap to panes[1].wrapStyleWord } shouldBe (true to true)
            } finally {
                UiTestHarness.detach(panel)
            }
        }
    }

    private companion object {
        const val PANE_WIDTH = 160.0
        const val LONG_TEXT = "WinUI4K brings WinUI controls to Swing developers from Java and Kotlin with familiar APIs"
        const val TOLERANCE = 0.5
    }
}
