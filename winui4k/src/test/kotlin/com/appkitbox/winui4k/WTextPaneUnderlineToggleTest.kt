package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that verify [WTextPane.toggleSelectionUnderline], which corresponds to StyledEditorKit.UnderlineAction. The expected values come from Swing's rules
 * (toggles the underline of the selection: a selection including parts without an underline gets underlined, and if all of it is underlined, it is removed).
 */
@Tags("E2E")
class WTextPaneUnderlineToggleTest : FunSpec() {
    /** Whether each character of [pane] is underlined. */
    private fun underlines(pane: WTextPane): List<Boolean> =
        pane.text.indices.map { WStyleConstants.isUnderline(pane.document.getCharacterAttributes(it)) }

    init {
        test("underlines the selection, and calling it again removes it. Outside the selection does not change") {
            onUiThreadGet {
                val pane = WTextPane()
                pane.text = "abcdef"
                pane.select(1, 4)
                pane.toggleSelectionUnderline()
                val on = underlines(pane)
                pane.select(1, 4)
                pane.toggleSelectionUnderline()
                on to underlines(pane)
            } shouldBe (listOf(false, true, true, true, false, false) to List(6) { false })
        }

        test("a selection with an underline on only part of it gets underlined entirely") {
            onUiThreadGet {
                val pane = WTextPane()
                pane.text = "abcdef"
                pane.select(0, 2)
                pane.toggleSelectionUnderline()
                pane.select(0, 4)
                pane.toggleSelectionUnderline()
                underlines(pane)
            } shouldBe listOf(true, true, true, true, false, false)
        }
    }
}
