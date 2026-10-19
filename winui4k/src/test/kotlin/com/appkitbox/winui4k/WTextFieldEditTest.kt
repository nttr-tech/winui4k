package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WTextField's JTextComponent-style editing API (isEditable, selection, caret, replaceSelection, clipboard,
 * undo). The expected values are the results of the same operations on Swing's JTextField.
 */
class WTextFieldEditTest : FunSpec() {
    /** Places a text field containing [text] in the shared window, runs [block] on the UI thread, and removes it. */
    private fun <T> withField(text: String, block: (WTextField) -> T): T {
        val field = onUiThreadGet { WTextField().also { it.text = text } }
        UiTestHarness.attachAndAwaitLoaded(field)
        try {
            return onUiThreadGet { block(field) }
        } finally {
            UiTestHarness.detach(field)
        }
    }

    /** The (selection start, selection end, caret position) of [field]. */
    private fun selectionOf(field: WTextField) = Triple(field.selectionStart, field.selectionEnd, field.caretPosition)

    init {
        test("isEditable is true by default, setting it to false makes isReadOnly true, and vice versa") {
            onUiThreadGet {
                val field = WTextField()
                val initial = field.isEditable
                field.isEditable = false
                val readOnly = field.isReadOnly
                field.isReadOnly = false
                listOf(initial, readOnly, field.isEditable)
            } shouldContainExactly listOf(true, true, true)
        }

        test("select(start, end) selects the range excluding the end character, reflected in selectionStart, selectionEnd and caretPosition (the end)") {
            withField("abcde") { field ->
                field.select(1, 3)
                field.selectedText to selectionOf(field)
            } shouldBe ("bc" to Triple(1, 3, 3))
        }

        test("select clamps the range into the text as in Swing, and selects nothing if the end is before the start") {
            withField("abcde") { field ->
                listOf(-3 to 2, 2 to 100, 4 to 1, 10 to 12).map { (start, end) ->
                    field.select(start, end)
                    selectionOf(field)
                }
            } shouldContainExactly listOf(Triple(0, 2, 2), Triple(2, 5, 5), Triple(4, 4, 4), Triple(5, 5, 5))
        }

        test("setting selectionStart or selectionEnd reselects while keeping the other end (as in Swing)") {
            withField("abcde") { field ->
                field.select(1, 4)
                field.selectionStart = 2
                val start = selectionOf(field)
                field.selectionEnd = 5
                val end = selectionOf(field)
                field.selectionStart = 5 // A start after the end moves the end to the same position
                val past = selectionOf(field)
                field.selectionEnd = 0 // An end before the start is set to the same position as the start
                listOf(start, end, past, selectionOf(field))
            } shouldContainExactly listOf(Triple(2, 4, 4), Triple(2, 5, 5), Triple(5, 5, 5), Triple(5, 5, 5))
        }

        test("setting caretPosition clears the selection and puts the caret at that position, which can also be 0 or the text length") {
            withField("abcde") { field ->
                field.select(1, 4)
                field.caretPosition = 2
                val middle = selectionOf(field) to field.selectedText
                field.caretPosition = 0
                val first = selectionOf(field)
                field.caretPosition = 5
                listOf(middle, first to "", selectionOf(field) to "")
            } shouldContainExactly listOf(Triple(2, 2, 2) to "", Triple(0, 0, 0) to "", Triple(5, 5, 5) to "")
        }

        test("a caretPosition outside the text throws IllegalArgumentException, and the caret does not move") {
            val field = onUiThreadGet { WTextField().also { it.text = "abc" } }
            UiTestHarness.attachAndAwaitLoaded(field)
            try {
                onUiThread { field.caretPosition = 1 }
                shouldThrow<IllegalArgumentException> { onUiThread { field.caretPosition = -1 } }
                shouldThrow<IllegalArgumentException> { onUiThread { field.caretPosition = 4 } }
                onUiThreadGet { field.caretPosition } shouldBe 1
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("replaceSelection replaces the selected range and puts the caret after the inserted text") {
            withField("abcdef") { field ->
                field.select(2, 4)
                field.replaceSelection("XYZ")
                field.text to selectionOf(field)
            } shouldBe ("abXYZef" to Triple(5, 5, 5))
        }

        test("replaceSelection with text containing line breaks puts the caret after what was actually inserted (the boundary between the inserted and following text), after TextBox fixes the line breaks (CRLF to CR for multi-line, dropping everything from the line break for single-line)") {
            withField("xy") { field ->
                field.caretPosition = 1
                field.replaceSelection("a\r\nb")
                val singleLine = Triple(field.text, field.caretPosition, field.text.length - 1)
                field.text = "xy"
                field.acceptsReturn = true
                field.caretPosition = 1
                field.replaceSelection("a\r\nb")
                listOf(singleLine, Triple(field.text, field.caretPosition, field.text.length - 1))
            }.forEach { (text, caret, beforeTail) ->
                // Whatever the length of the actually inserted text, Swing expects the caret right before the trailing "y"
                text shouldStartWith "xa"
                text shouldEndWith "y"
                caret shouldBe beforeTail
            }
        }

        test("replaceSelection exceeding maxLength is not truncated from the program (MaxLength only limits input), and puts the caret after the inserted text") {
            withField("abcd") { field ->
                field.maxLength = 5
                field.select(1, 2) // Replacing "b" with 3 characters makes 6 characters
                field.replaceSelection("XYZ")
                field.text to field.caretPosition
            } shouldBe ("aXYZcd" to 4)
        }

        test("without a selection, replaceSelection inserts at the caret position, and null or an empty string deletes the selected range") {
            withField("abcdef") { field ->
                field.caretPosition = 3
                field.replaceSelection("-")
                val inserted = field.text to field.caretPosition
                field.select(0, 2)
                field.replaceSelection(null)
                val removedByNull = field.text to field.caretPosition
                field.select(0, 1)
                field.replaceSelection("")
                listOf(inserted, removedByNull, field.text to field.caretPosition)
            } shouldContainExactly listOf("abc-def" to 4, "c-def" to 0, "-def" to 0)
        }

        test("when isEditable is false, replaceSelection, cut and paste do not change the text") {
            val field = onUiThreadGet { WTextField().also { it.text = "abc" } }
            UiTestHarness.attachAndAwaitLoaded(field)
            try {
                onUiThread {
                    field.isEditable = false
                    field.select(0, 1)
                    field.replaceSelection("X")
                    field.cut()
                    field.paste()
                }
                Thread.sleep(SETTLE_MILLIS) // paste reads the clipboard later, so wait a little to confirm nothing changes
                onUiThreadGet { field.text } shouldBe "abc"
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("cut moves the selected range to the clipboard, paste inserts it at the caret position, and copy does not change the text") {
            val field = onUiThreadGet { WTextField().also { it.text = "abc" } }
            UiTestHarness.attachAndAwaitLoaded(field)
            try {
                onUiThread {
                    field.select(0, 1)
                    field.cut()
                }
                onUiThreadGet { field.text } shouldBe "bc"
                onUiThread {
                    field.caretPosition = 2
                    field.paste()
                }
                eventually(3.seconds) { onUiThreadGet { field.text } shouldBe "bca" }
                onUiThread {
                    field.select(0, 2)
                    field.copy()
                }
                onUiThreadGet { field.text } shouldBe "bca"
                onUiThread {
                    field.caretPosition = 3
                    field.paste()
                }
                eventually(3.seconds) { onUiThreadGet { field.text } shouldBe "bcabc" }
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("undo reverts the last replaceSelection and redo reapplies it, with canUndo and canRedo following accordingly") {
            withField("abc") { field ->
                field.caretPosition = 3
                field.replaceSelection("d")
                val edited = listOf(field.text, field.canUndo())
                field.undo()
                val undone = listOf(field.text, field.canRedo())
                field.redo()
                edited + undone + listOf(field.text)
            } shouldContainExactly listOf("abcd", true, "abc", true, "abcd")
        }
    }

    private companion object {
        /** Time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
