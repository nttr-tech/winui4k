package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.TextInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [WTextField.moveCaretPosition] / [WTextPane.moveCaretPosition], like JTextComponent.moveCaretPosition, and the selection direction (dot and mark).
 * The expected values are caretPosition and the selection range from the same operations on Swing's JTextField / JTextArea (DefaultCaret).
 */
@Tags("E2E")
class WMoveCaretPositionTest : FunSpec() {
    /** An interface for checking both kinds of text fields with the same operations. */
    private interface Target {
        var text: String
        var caret: Int
        val selection: Pair<Int, Int>
        val document: WDocument

        fun move(position: Int)

        fun select(start: Int, end: Int)

        fun addCaretListener(listener: WCaretListener)
    }

    private fun field(): Target = object : Target {
        val field = WTextField()
        override var text by field::text
        override var caret by field::caretPosition
        override val selection get() = field.selectionStart to field.selectionEnd
        override val document get() = field.document

        override fun move(position: Int) = field.moveCaretPosition(position)

        override fun select(start: Int, end: Int) = field.select(start, end)

        override fun addCaretListener(listener: WCaretListener) = field.addCaretListener(listener)
    }

    private fun pane(): Target = object : Target {
        val pane = WTextPane()
        override var text by pane::text
        override var caret by pane::caretPosition
        override val selection get() = pane.selectionStart to pane.selectionEnd
        override val document get() = pane.document

        override fun move(position: Int) = pane.moveCaretPosition(position)

        override fun select(start: Int, end: Int) = pane.select(start, end)

        override fun addCaretListener(listener: WCaretListener) = pane.addCaretListener(listener)
    }

    /** Runs [check] on both kinds of text fields and lists the results. */
    private fun <T> both(check: (Target) -> T): List<T> = onUiThreadGet { listOf(field(), pane()).map(check) }

    /** Moves the moving end of the ITextSelection of [pane] one character left, like Shift+← (ITextSelection.MoveLeft(Character, 1, extend = true)). */
    private fun extendLeft(pane: WTextPane) {
        val document = pane.inspectable.getPtr(XamlInterop.IRichEditBox_get_Document)
        try {
            val selection = document.getPtr(XamlInterop.ITextDocument_get_Selection)
            try {
                Ffi.backend.withScope { scope ->
                    val moved = scope.allocate(4, 4) // MoveLeft(unit, count, extend, out i4)
                    selection.call(TextInterop.ITextSelection_MoveLeft, TextInterop.TextRangeUnit_Character, 1, 1, moved)
                }
            } finally {
                selection.release()
            }
        } finally {
            document.release()
        }
    }

    /** A text field with "abcdef" whose caret was placed at 4 and then moveCaretPosition'd to 1 (selected from back to front). */
    private fun selectedBackward(t: Target): Target = t.also {
        it.text = "abcdef"
        it.caret = 4
        it.move(1)
    }

    init {
        context("Reading and writing") {
            test("moving forward with moveCaretPosition after caretPosition selects the range between, and caretPosition is the moved-to position (the start of the selection)") {
                both { selectedBackward(it).let { t -> t.selection to t.caret } } shouldBe listOf((1 to 4) to 1, (1 to 4) to 1)
            }

            test("then moving with moveCaretPosition after mark selects from mark (the first position) onward") {
                both { selectedBackward(it).also { t -> t.move(6) }.let { t -> t.selection to t.caret } } shouldBe
                    listOf((4 to 6) to 6, (4 to 6) to 6)
            }

            test("caretPosition after select(start, end) is the end, as in Swing (the forward direction does not remain)") {
                both { selectedBackward(it).also { t -> t.select(1, 4) }.caret } shouldBe listOf(4, 4)
            }

            test("inserting text before the selection after selecting forward shifts the whole selection back while keeping its direction (the same as DefaultCaret)") {
                both { selectedBackward(it).also { t -> t.document.insertString(0, "XY", null) }.let { t -> t.selection to t.caret } } shouldBe
                    listOf((3 to 6) to 3, (3 to 6) to 3)
            }
        }

        context("Events") {
            test("right after moveCaretPosition, the CaretListener receives dot (the moved-to position) and mark (the original position)") {
                both { t ->
                    t.text = "abcdef"
                    t.caret = 4
                    val events = mutableListOf<String>()
                    t.addCaretListener { e -> events += "${e.dot}/${e.mark}" }
                    t.move(2)
                    events
                } shouldBe listOf(listOf("2/4"), listOf("2/4"))
            }
        }

        context("Boundaries and exceptions") {
            test("moveCaretPosition before 0 or after the number of characters throws IllegalArgumentException and the selection does not change (the same as Swing)") {
                both { t ->
                    t.text = "abc"
                    t.caret = 1
                    val before = runCatching { t.move(-1) }.exceptionOrNull() is IllegalArgumentException
                    val after = runCatching { t.move(4) }.exceptionOrNull() is IllegalArgumentException
                    listOf(before, after, t.selection)
                } shouldBe listOf(listOf(true, true, 1 to 1), listOf(true, true, 1 to 1))
            }
        }

        context("Input") {
            test("WTextPane: after selecting forward, moving the moving end like Shift+← extends the start side of the selection (the RichEditBox selection direction is matched too)") {
                val pane = onUiThreadGet { WTextPane().also { it.text = "abcdef" } }
                UiTestHarness.attachAndAwaitLoaded(pane)
                onUiThreadGet {
                    pane.caretPosition = 4
                    pane.moveCaretPosition(2)
                    extendLeft(pane)
                    (pane.selectionStart to pane.selectionEnd) to pane.caretPosition
                } shouldBe ((1 to 4) to 1)
            }

            test("WTextPane: for a selection the user made from back to front (Shift+← twice), caretPosition is the start of the selection") {
                val pane = onUiThreadGet { WTextPane().also { it.text = "abcdef" } }
                UiTestHarness.attachAndAwaitLoaded(pane)
                onUiThreadGet {
                    pane.caretPosition = 3
                    extendLeft(pane)
                    extendLeft(pane)
                    listOf(pane.selectionStart to pane.selectionEnd, pane.caretPosition)
                } shouldBe listOf(1 to 3, 1)
            }
        }
    }
}
