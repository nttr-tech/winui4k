package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for getDocument of text fields ([WDocument], like Document). The expected values are the text, events and caret positions from
 * the same operations on the Document (PlainDocument) and DefaultCaret of Swing's JTextField / JPasswordField / JTextArea.
 */
@Tags("E2E")
class WDocumentTest : FunSpec() {
    /** A listener that records received events in the form "insert:offset:length". */
    private class Recorder : WDocumentListener {
        val events = mutableListOf<String>()
        val documents = mutableListOf<WDocument>()

        override fun insertUpdate(e: WDocumentEvent) = record("insert", e)

        override fun removeUpdate(e: WDocumentEvent) = record("remove", e)

        override fun changedUpdate(e: WDocumentEvent) = record("changed", e)

        private fun record(kind: String, e: WDocumentEvent) {
            events += "$kind:${e.offset}:${e.length}"
            documents += e.document
        }
    }

    init {
        context("Defaults") {
            test("getDocument returns the same one every time, and for an empty text field the length is 0 with no listeners and no filter") {
                onUiThreadGet {
                    val field = WTextField()
                    val pane = WTextPane()
                    val password = WPasswordField()
                    listOf(
                        field.document === field.document,
                        pane.document === pane.document,
                        password.document === password.document,
                        field.document.length,
                        pane.document.length,
                        password.document.length,
                        field.document.documentListeners.size,
                        field.document.documentFilter,
                    )
                } shouldBe listOf(true, true, true, 0, 0, 0, 0, null)
            }
        }

        context("Reading and writing") {
            test("WTextField: insertString, remove and replace change the text, which getLength and getText(offs, len) read") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abcd"
                    val doc = field.document
                    doc.insertString(doc.length, "ef", null)
                    val afterAppend = field.text
                    doc.remove(0, 2)
                    val afterRemove = field.text
                    doc.replace(1, 2, "XYZ", null)
                    listOf(afterAppend, afterRemove, field.text, doc.length, doc.getText(0, doc.length), doc.getText(1, 3))
                } shouldBe listOf("abcdef", "cdef", "cXYZf", 5, "cXYZf", "XYZ")
            }

            test("WTextPane: even with text containing line breaks, positions and line breaks are the same as text (LF is 1 character)") {
                onUiThreadGet {
                    val pane = WTextPane()
                    pane.text = "ab"
                    val doc = pane.document
                    doc.insertString(1, "1\n2", null)
                    val inserted = pane.text
                    doc.remove(0, 2)
                    listOf(inserted, pane.text, doc.length, doc.getText(0, 2))
                } shouldBe listOf("a1\n2b", "\n2b", 3, "\n2")
            }

            test("WTextPane: putting text ending with a line break into a text pane that has never had text does not lose the line break (insertString, append, insert)") {
                onUiThreadGet {
                    val viaDocument = WTextPane().also { it.document.insertString(0, "Line 1\n", null) }
                    val viaAppend = WTextPane().also { it.append("Line 1\n") }
                    val viaInsert = WTextPane().also { it.insert("\n", 0) }
                    listOf(viaDocument.text, viaAppend.text, viaInsert.text, viaAppend.length)
                } shouldBe listOf("Line 1\n", "Line 1\n", "\n", 7)
            }

            test("WPasswordField: insertString and remove change the password, and getText reads the password") {
                onUiThreadGet {
                    val field = WPasswordField()
                    field.setText("secret")
                    val doc = field.document
                    doc.insertString(0, "my", null)
                    doc.remove(doc.length - 1, 1)
                    listOf(String(field.getPassword()), doc.getText(0, doc.length))
                } shouldBe listOf("mysecre", "mysecre")
            }

            test("insertString and remove can modify it even if isEditable is false (the same as Swing)") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abc"
                    field.isEditable = false
                    val pane = WTextPane()
                    pane.text = "abc"
                    pane.isEditable = false
                    field.document.insertString(3, "d", null)
                    pane.document.remove(0, 1)
                    listOf(field.text, pane.text)
                } shouldBe listOf("abcd", "bc")
            }

            test("the caret moves like DefaultCaret: inserting before it shifts it back, inserting at the caret moves it after the inserted text, and changes after it do not move it") {
                onUiThreadGet {
                    val result = mutableListOf<Int>()
                    for (field in listOf(WTextField(), WTextPane())) {
                        val doc = if (field is WTextField) field.document else (field as WTextPane).document
                        val caret = { if (field is WTextField) field.caretPosition else (field as WTextPane).caretPosition }
                        val setCaret = { p: Int -> if (field is WTextField) field.caretPosition = p else (field as WTextPane).caretPosition = p }
                        doc.insertString(0, "abcdef", null)
                        setCaret(3)
                        doc.insertString(0, "XY", null) // Before → 5
                        result += caret()
                        doc.insertString(5, "Z", null) // At the caret → 6
                        result += caret()
                        doc.insertString(8, "W", null) // After → stays 6
                        result += caret()
                        doc.remove(0, 2) // Remove before → 4
                        result += caret()
                        doc.remove(2, 4) // A range containing the caret → its start, 2
                        result += caret()
                    }
                    result
                } shouldBe listOf(5, 6, 6, 4, 2, 5, 6, 6, 4, 2)
            }

            test("both ends of the selection move the same way: inserting before the selection shifts the whole selection back") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abcdef"
                    field.select(2, 4)
                    field.document.insertString(0, "XY", null)
                    listOf(field.selectionStart, field.selectionEnd, field.selectedText)
                } shouldBe listOf(4, 6, "cd")
            }
        }

        context("Events") {
            test("listeners added to document go into the same list as the text field's documentListeners, and removing removes them from both") {
                onUiThreadGet {
                    val field = WTextField()
                    val recorder = Recorder()
                    field.document.addDocumentListener(recorder)
                    val viaField = field.documentListeners.toList()
                    field.removeDocumentListener(recorder)
                    listOf(viaField == listOf(recorder), field.document.documentListeners.size)
                } shouldBe listOf(true, 0)
            }

            test("insertString delivers only insertUpdate, remove only removeUpdate, and replace removeUpdate then insertUpdate (the same as Swing)") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abcd"
                    val recorder = Recorder()
                    field.document.addDocumentListener(recorder)
                    field.document.insertString(4, "e", null)
                    field.document.remove(0, 1)
                    field.document.replace(0, 2, "XYZ", null)
                    field.document.replace(0, 0, "", null) // Changes nothing, so nothing is delivered
                    recorder.events
                } shouldBe listOf("insert:4:1", "remove:0:1", "remove:0:2", "insert:0:3")
            }

            test("WDocumentEvent.getDocument is the same instance as the document of the source text field") {
                val (documents, expected) = onUiThreadGet {
                    val field = WTextField()
                    val pane = WTextPane()
                    val password = WPasswordField()
                    val recorder = Recorder()
                    field.addDocumentListener(recorder)
                    pane.addDocumentListener(recorder)
                    password.addDocumentListener(recorder)
                    field.text = "a"
                    pane.text = "b"
                    password.setText("c")
                    recorder.documents.toList() to listOf(field.document, pane.document, password.document)
                }
                documents.size shouldBe 3
                for (i in 0..2) documents[i] shouldBeSameInstanceAs expected[i]
            }

            test("modifying the document from inside a listener throws IllegalStateException (Attempt to mutate in notification)") {
                onUiThreadGet {
                    val field = WTextField()
                    val caught = mutableListOf<String?>()
                    field.document.addDocumentListener(
                        object : WDocumentListener {
                            override fun insertUpdate(e: WDocumentEvent) {
                                try {
                                    e.document.insertString(0, "z", null)
                                } catch (ex: IllegalStateException) {
                                    caught += ex.message
                                }
                            }

                            override fun removeUpdate(e: WDocumentEvent) = Unit

                            override fun changedUpdate(e: WDocumentEvent) = Unit
                        },
                    )
                    field.document.insertString(0, "a", null)
                    caught to field.text
                } shouldBe (listOf<String?>("Attempt to mutate in notification") to "a")
            }
        }

        context("Boundaries and exceptions") {
            test("an insertString position after the number of characters or negative throws IllegalArgumentException, and the text does not change") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "ab"
                    val beyond = runCatching { field.document.insertString(3, "x", null) }.exceptionOrNull()
                    val negative = runCatching { field.document.insertString(-1, "x", null) }.exceptionOrNull()
                    listOf(beyond is IllegalArgumentException, negative is IllegalArgumentException, field.text)
                } shouldBe listOf(true, true, "ab")
            }

            test("insertString with null or an empty string and remove with length 0 do nothing even if the position is out of range (the same as Swing)") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "ab"
                    field.document.insertString(99, null, null)
                    field.document.insertString(99, "", null)
                    field.document.remove(99, 0)
                    field.text
                } shouldBe "ab"
            }

            test("a remove or getText range outside the text throws IllegalArgumentException") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "ab"
                    shouldThrow<IllegalArgumentException> { field.document.remove(1, 2) }
                    shouldThrow<IllegalArgumentException> { field.document.remove(-1, 1) }
                    shouldThrow<IllegalArgumentException> { field.document.getText(0, 3) }
                    field.text
                } shouldBe "ab"
            }
        }
    }
}
