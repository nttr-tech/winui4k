package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests that verify reading and writing the text of WTextPane (a JTextArea / JTextPane-style RichEditBox). The
 * expected values are the results of the same operations on Swing's JTextArea (positions are character counts, a line
 * break is the single character "\n", and text has no trailing paragraph mark).
 */
class WTextPaneTest : FunSpec() {
    /** Creates a new WTextPane on the UI thread and runs [block]. */
    private fun <T> withPane(text: String = "", block: (WTextPane) -> T): T = onUiThreadGet {
        val pane = WTextPane()
        if (text.isNotEmpty()) pane.text = text
        block(pane)
    }

    init {
        test("an empty text pane has empty text, length 0 and lineCount 1") {
            withPane { pane -> Triple(pane.text, pane.length, pane.lineCount) } shouldBe Triple("", 0, 1)
        }

        test("text returns the string as set, without a trailing paragraph mark (\\r)") {
            withPane { pane ->
                listOf("abc", "Some text", "x").map {
                    pane.text = it
                    pane.text
                }
            } shouldContainExactly listOf("abc", "Some text", "x")
        }

        test("line breaks in text are the single character \\n, and a trailing line break is kept") {
            withPane { pane ->
                listOf("a\nb", "a\n", "\n\nb", "a\n\nb").map {
                    pane.text = it
                    pane.text to pane.length
                }
            } shouldContainExactly listOf("a\nb" to 3, "a\n" to 2, "\n\nb" to 3, "a\n\nb" to 4)
        }

        test("\\r\\n and \\r line breaks become a single \\n") {
            withPane { pane ->
                pane.text = "a\r\nb\rc"
                pane.text
            } shouldBe "a\nb\nc"
        }

        test("setting text to an empty string empties it") {
            withPane("abc") { pane ->
                pane.text = ""
                pane.text to pane.length
            } shouldBe ("" to 0)
        }

        test("lineCount is the number of lines separated by line breaks, and getLineOfOffset, getLineStartOffset and getLineEndOffset return the same values as Swing") {
            withPane("ab\ncd\n\nef") { pane ->
                val lines = pane.lineCount
                val of = listOf(0, 2, 3, 5, 6, 7, 9).map { pane.getLineOfOffset(it) }
                val starts = (0 until lines).map { pane.getLineStartOffset(it) }
                val ends = (0 until lines).map { pane.getLineEndOffset(it) }
                listOf(listOf(lines), of, starts, ends)
            } shouldContainExactly listOf(
                listOf(4),
                listOf(0, 0, 1, 1, 2, 3, 3),
                listOf(0, 3, 6, 7),
                listOf(3, 6, 7, 9),
            )
        }

        test("getLineOfOffset, getLineStartOffset and getLineEndOffset with an out-of-range position or line number throw IllegalArgumentException") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "ab\ncd" } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.getLineOfOffset(-1) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.getLineOfOffset(6) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.getLineStartOffset(2) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.getLineStartOffset(-1) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.getLineEndOffset(2) } }
        }

        test("append adds to the end, and null and an empty string do nothing") {
            withPane("abc") { pane ->
                pane.append("def")
                val appended = pane.text
                pane.append(null)
                pane.append("")
                pane.append("\nghi")
                listOf(appended, pane.text)
            } shouldContainExactly listOf("abcdef", "abcdef\nghi")
        }

        test("append to an empty text pane becomes the text") {
            withPane { pane ->
                pane.append("Beginning")
                pane.text
            } shouldBe "Beginning"
        }

        test("insert inserts at the specified position (start, middle and end)") {
            withPane("bd") { pane ->
                pane.insert("a", 0)
                pane.insert("c", 2)
                pane.insert("e", 4)
                pane.text
            } shouldBe "abcde"
        }

        test("insert at an out-of-range position throws IllegalArgumentException, and null or an empty string does nothing regardless of the position") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abc" } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.insert("x", -1) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.insert("x", 4) } }
            onUiThreadGet {
                pane.insert(null, 99)
                pane.insert("", -5)
                pane.text
            } shouldBe "abc"
        }

        test("replaceRange replaces from start up to (but not including) end, and null or an empty string removes the range") {
            withPane("abcdef") { pane ->
                pane.replaceRange("XY", 1, 3)
                val replaced = pane.text
                pane.replaceRange(null, 0, 1)
                val removed = pane.text
                pane.replaceRange("", 1, 2)
                listOf(replaced, removed, pane.text)
            } shouldContainExactly listOf("aXYdef", "XYdef", "Xdef")
        }

        test("replaceRange also inserts into a zero-length range, and replacing with a string containing line breaks counts positions one character each") {
            withPane("a\nb") { pane ->
                pane.replaceRange("-", 1, 1)
                val inserted = pane.text
                pane.replaceRange("X\nY", 3, 4)
                listOf(inserted, pane.text, pane.length.toString())
            } shouldContainExactly listOf("a-\nb", "a-\nX\nY", "6")
        }

        test("replaceRange with end before start, or with a range outside the text, throws IllegalArgumentException and leaves the text unchanged") {
            val pane = onUiThreadGet { WTextPane().also { it.text = "abc" } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.replaceRange("x", 2, 1) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.replaceRange("x", -1, 1) } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.replaceRange("x", 1, 4) } }
            onUiThreadGet { pane.text } shouldBe "abc"
        }

        test("lineWrap defaults to true once placed in a window, from TextWrapping = Wrap in WinUI's default style (unlike JTextArea's false), stops wrapping when set to false, and wraps again when set back to true") {
            val pane = onUiThreadGet { WTextPane() }
            UiTestHarness.attachAndAwaitLoaded(pane)
            try {
                onUiThreadGet {
                    val initial = pane.lineWrap to pane.textWrapping
                    pane.lineWrap = false
                    val off = pane.lineWrap to pane.textWrapping
                    pane.lineWrap = true
                    listOf(initial, off, pane.lineWrap to pane.textWrapping)
                } shouldContainExactly listOf(
                    true to TextWrapping.WRAP,
                    false to TextWrapping.NO_WRAP,
                    true to TextWrapping.WRAP,
                )
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("with wrapStyleWord true, wrapping does not break in the middle of a word, and a value set while not wrapping is used when wrapping is enabled") {
            withPane { pane ->
                pane.lineWrap = false // the default lineWrap is determined after placing (see the lineWrap default test below), so start from the non-wrapping state
                val initial = pane.wrapStyleWord
                pane.wrapStyleWord = true
                val whileNoWrap = pane.wrapStyleWord to pane.textWrapping
                pane.lineWrap = true
                val wrapped = pane.wrapStyleWord to pane.textWrapping
                pane.wrapStyleWord = false
                listOf(initial to TextWrapping.NO_WRAP, whileNoWrap, wrapped, pane.wrapStyleWord to pane.textWrapping)
            } shouldContainExactly listOf(
                false to TextWrapping.NO_WRAP,
                true to TextWrapping.NO_WRAP,
                true to TextWrapping.WRAP_WHOLE_WORDS,
                false to TextWrapping.WRAP,
            )
        }

        test("setting textWrapping directly to WRAP_WHOLE_WORDS makes lineWrap and wrapStyleWord true") {
            withPane { pane ->
                pane.textWrapping = TextWrapping.WRAP_WHOLE_WORDS
                pane.lineWrap to pane.wrapStyleWord
            } shouldBe (true to true)
        }

        test("isEditable is true by default and the inverse of isReadOnly. Even when false, text, append, insert and replaceRange can change the text (as in JTextArea)") {
            withPane("abc") { pane ->
                val initial = pane.isEditable
                pane.isEditable = false
                val readOnly = pane.isReadOnly
                // Even though it is made writable temporarily for writing, isEditable is back to false after every write
                val editableAfter = mutableListOf<Boolean>()
                pane.append("d")
                editableAfter += pane.isEditable
                pane.insert("0", 0)
                editableAfter += pane.isEditable
                pane.replaceRange("X", 1, 2)
                editableAfter += pane.isEditable
                val edited = pane.text
                pane.text = "New"
                editableAfter += pane.isEditable
                listOf(initial, readOnly, edited, pane.text, editableAfter, pane.isReadOnly)
            } shouldContainExactly listOf(true, true, "0Xbcd", "New", listOf(false, false, false, false), true)
        }

        test("rtfText returns formatted RTF, and setting RTF puts its text into text") {
            withPane("abc") { pane ->
                val rtf = pane.rtfText
                pane.rtfText = "{\\rtf1\\ansi Hello {\\b World}}"
                listOf(rtf.contains("rtf1").toString(), pane.text)
            } shouldContainExactly listOf("true", "Hello World")
        }

        test("the constructors' placeholder, rows and columns, and text with rows and columns are applied") {
            val (placeholder, sized, withText) = onUiThreadGet {
                Triple(
                    WTextPane("Enter text").placeholderText,
                    WTextPane(5, 40).let { it.rows to it.columns },
                    WTextPane("Initial", 3, 20).let { Triple(it.text, it.rows, it.columns) },
                )
            }
            placeholder shouldBe "Enter text"
            sized shouldBe (5 to 40)
            withText shouldBe Triple("Initial", 3, 20)
        }

        test("setting a negative value to rows or columns throws IllegalArgumentException and leaves the value unchanged") {
            val pane = onUiThreadGet { WTextPane(2, 10) }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.rows = -1 } }
            shouldThrow<IllegalArgumentException> { onUiThreadGet { pane.columns = -1 } }
            onUiThreadGet { pane.rows to pane.columns } shouldBe (2 to 10)
        }

        test("reading text is not affected by formatting (making it bold does not change the text)") {
            withPane("abc") { pane ->
                pane.select(0, 2)
                pane.toggleSelectionBold()
                pane.text
            } shouldBe "abc"
        }
    }
}
