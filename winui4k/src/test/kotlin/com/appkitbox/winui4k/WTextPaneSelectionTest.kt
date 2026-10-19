package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify WTextPane's selection, caret, replaceSelection, clipboard, undo and CaretListener.
 * The expected values are the results of the same operations on Swing's JTextArea (the same form as
 * [WTextFieldEditTest]). A line break counts as one character.
 */
class WTextPaneSelectionTest : FunSpec() {
    /** Places a text pane containing [text] in the shared window, runs [block] on the UI thread, and removes it. */
    private fun <T> withPane(text: String, block: (WTextPane) -> T): T {
        val pane = onUiThreadGet { WTextPane().also { it.text = text } }
        UiTestHarness.attachAndAwaitLoaded(pane)
        try {
            return onUiThreadGet { block(pane) }
        } finally {
            UiTestHarness.detach(pane)
        }
    }

    /** (selection start, selection end, caret position) of [pane]. */
    private fun selectionOf(pane: WTextPane) = Triple(pane.selectionStart, pane.selectionEnd, pane.caretPosition)

    /** Records received events in the form "dot:mark". */
    private class Recorder : WCaretListener {
        val events = mutableListOf<String>()
        val sources = mutableListOf<Any?>()

        override fun caretUpdate(e: WCaretEvent) {
            events += "${e.dot}:${e.mark}"
            sources += e.source
        }
    }

    init {
        test("select(start, end) selects the range excluding the end character, reflected in selectionStart, selectionEnd and caretPosition (the end)") {
            withPane("abcde") { pane ->
                pane.select(1, 3)
                pane.selectedText to selectionOf(pane)
            } shouldBe ("bc" to Triple(1, 3, 3))
        }

        test("a line break counts as one character, and the positions and selectedText of a selection across line breaks are the same as in Swing") {
            withPane("ab\ncd\nef") { pane ->
                pane.select(1, 4)
                val across = pane.selectedText to selectionOf(pane)
                pane.select(3, 5)
                listOf(across, pane.selectedText to selectionOf(pane))
            } shouldContainExactly listOf("b\nc" to Triple(1, 4, 4), "cd" to Triple(3, 5, 5))
        }

        test("select clamps the range into the text as in Swing, and selects nothing if the end is before the start") {
            withPane("abcde") { pane ->
                listOf(-3 to 2, 2 to 100, 4 to 1, 10 to 12).map { (start, end) ->
                    pane.select(start, end)
                    selectionOf(pane)
                }
            } shouldContainExactly listOf(Triple(0, 2, 2), Triple(2, 5, 5), Triple(4, 4, 4), Triple(5, 5, 5))
        }

        test("selectAll selects all characters, and the selection end is the character count excluding the last paragraph mark") {
            withPane("ab\ncd") { pane ->
                pane.selectAll()
                Triple(pane.selectedText, selectionOf(pane), pane.length)
            } shouldBe Triple("ab\ncd", Triple(0, 5, 5), 5)
        }

        test("setting selectionStart or selectionEnd reselects while keeping the other end (as in Swing)") {
            withPane("abcde") { pane ->
                pane.select(1, 4)
                pane.selectionStart = 2
                val start = selectionOf(pane)
                pane.selectionEnd = 5
                val end = selectionOf(pane)
                pane.selectionStart = 5 // a start past the end moves the end to the same position
                val past = selectionOf(pane)
                pane.selectionEnd = 0 // an end before the start is put at the same position as the start
                listOf(start, end, past, selectionOf(pane))
            } shouldContainExactly listOf(Triple(2, 4, 4), Triple(2, 5, 5), Triple(5, 5, 5), Triple(5, 5, 5))
        }

        test("setting caretPosition clears the selection and puts the caret at that position, which can also be 0 or the text length") {
            withPane("abcde") { pane ->
                pane.select(1, 4)
                pane.caretPosition = 2
                val middle = selectionOf(pane) to pane.selectedText
                pane.caretPosition = 0
                val first = selectionOf(pane)
                pane.caretPosition = 5
                listOf(middle, first to "", selectionOf(pane) to "")
            } shouldContainExactly listOf(Triple(2, 2, 2) to "", Triple(0, 0, 0) to "", Triple(5, 5, 5) to "")
        }

        test("a caretPosition outside the text throws IllegalArgumentException, and the caret does not move") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abc" } }
            UiTestHarness.attachAndAwaitLoaded(pane)
            try {
                onUiThread { pane.caretPosition = 1 }
                shouldThrow<IllegalArgumentException> { onUiThread { pane.caretPosition = -1 } }
                shouldThrow<IllegalArgumentException> { onUiThread { pane.caretPosition = 4 } }
                onUiThreadGet { pane.caretPosition } shouldBe 1
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("replaceSelection replaces the selected range and puts the caret after the inserted text") {
            withPane("abcdef") { pane ->
                pane.select(2, 4)
                pane.replaceSelection("XYZ")
                pane.text to selectionOf(pane)
            } shouldBe ("abXYZef" to Triple(5, 5, 5))
        }

        test("without a selection, replaceSelection inserts at the caret position, and null or an empty string deletes the selected range") {
            withPane("abcdef") { pane ->
                pane.caretPosition = 3
                pane.replaceSelection("-")
                val inserted = pane.text to pane.caretPosition
                pane.select(0, 2)
                pane.replaceSelection(null)
                val removedByNull = pane.text to pane.caretPosition
                pane.select(0, 1)
                pane.replaceSelection("")
                listOf(inserted, removedByNull, pane.text to pane.caretPosition)
            } shouldContainExactly listOf("abc-def" to 4, "c-def" to 0, "-def" to 0)
        }

        test("replaceSelection with a string containing a line break places the caret counting the line break as one character") {
            withPane("abcd") { pane ->
                pane.select(1, 3)
                pane.replaceSelection("X\nY")
                pane.text to pane.caretPosition
            } shouldBe ("aX\nYd" to 4)
        }

        test("when isEditable is false, replaceSelection, cut and paste do not change the text, and copy works") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abc" } }
            UiTestHarness.attachAndAwaitLoaded(pane)
            try {
                onUiThread {
                    pane.isEditable = false
                    pane.select(0, 1)
                    pane.replaceSelection("X")
                    pane.cut()
                    pane.paste()
                }
                Thread.sleep(SETTLE_MILLIS) // paste may read the clipboard later, so wait a little to confirm nothing changes
                onUiThreadGet { pane.text } shouldBe "abc"
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("cut moves the selected range to the clipboard, paste inserts it at the caret position, and copy does not change the text") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abc" } }
            UiTestHarness.attachAndAwaitLoaded(pane)
            try {
                onUiThread {
                    pane.select(0, 1)
                    pane.cut()
                }
                onUiThreadGet { pane.text } shouldBe "bc"
                onUiThread {
                    pane.caretPosition = 2
                    pane.paste()
                }
                eventually(3.seconds) { onUiThreadGet { pane.text } shouldBe "bca" }
                onUiThread {
                    pane.select(0, 2)
                    pane.copy()
                }
                onUiThreadGet { pane.text } shouldBe "bca"
                onUiThread {
                    pane.caretPosition = pane.length
                    pane.paste()
                }
                eventually(3.seconds) { onUiThreadGet { pane.text } shouldBe "bcabc" }
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("copy and cut with no selection do nothing and change neither the clipboard contents nor the text") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abc" } }
            UiTestHarness.attachAndAwaitLoaded(pane)
            try {
                onUiThread {
                    pane.select(0, 1)
                    pane.copy() // the clipboard holds "a"
                    pane.caretPosition = 3
                    pane.copy()
                    pane.cut()
                    pane.paste()
                }
                eventually(3.seconds) { onUiThreadGet { pane.text } shouldBe "abca" }
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("undo reverts the last replaceSelection and redo reapplies it, with canUndo and canRedo following accordingly") {
            withPane("abc") { pane ->
                pane.caretPosition = 3
                pane.replaceSelection("d")
                val edited = listOf(pane.text, pane.canUndo)
                pane.undo()
                val undone = listOf(pane.text, pane.canRedo)
                pane.redo()
                edited + undone + listOf(pane.text)
            } shouldContainExactly listOf("abcd", true, "abc", true, "abcd")
        }

        test("the CaretListener immediately receives changes of select and caretPosition as events with dot (end) and mark (start), and nothing is delivered again later") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abcdef" } }
            val recorder = Recorder()
            onUiThread { pane.addCaretListener(recorder) }
            UiTestHarness.attachAndAwaitLoaded(pane)
            Thread.sleep(SETTLE_MILLIS) // wait for the SelectionChanged on placing it before starting
            onUiThread {
                pane.caretPosition = 0
                recorder.events.clear()
            }
            try {
                val immediate = onUiThreadGet {
                    pane.select(1, 4)
                    val afterSelect = recorder.events.toList()
                    pane.caretPosition = 2
                    afterSelect to recorder.events.toList()
                }
                immediate shouldBe (listOf("4:1") to listOf("4:1", "2:2"))
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { recorder.events.toList() } shouldContainExactly listOf("4:1", "2:2")
                onUiThreadGet { recorder.sources.all { it === pane } } shouldBe true
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("select or caretPosition that does not change the position does not reach the CaretListener, nor a removed listener") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abc" } }
            val recorder = Recorder()
            onUiThread {
                pane.addCaretListener(recorder)
                pane.caretPosition = 1
                recorder.events.clear()
                pane.caretPosition = 1
                pane.select(1, 1)
                pane.removeCaretListener(recorder)
                pane.select(0, 3)
            }
            onUiThreadGet { recorder.events.toList() } shouldContainExactly emptyList()
            onUiThreadGet { pane.caretListeners.size } shouldBe 0
        }

        test("when setting text moves the caret, it also reaches the CaretListener immediately") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abcdef" } }
            val recorder = Recorder()
            onUiThread {
                pane.addCaretListener(recorder)
                pane.select(2, 4)
                recorder.events.clear()
                pane.replaceSelection("Z")
            }
            onUiThreadGet { recorder.events.toList() } shouldContainExactly listOf("3:3")
        }

        test("adding and removing a null CaretListener does nothing") {
            onUiThreadGet {
                val pane = WTextPane()
                pane.addCaretListener(null)
                pane.removeCaretListener(null)
                pane.caretListeners.size
            } shouldBe 0
        }
    }

    private companion object {
        /** Time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
