package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [WDocumentFilter], like DocumentFilter. The expected values are the calls that reach the filter (method, position, length, text) and the resulting text
 * when the same operations are done with Swing's AbstractDocument + DocumentFilter. Key input is sent to the shared window with [TestKeyboard].
 */
@Tags("E2E")
class WDocumentFilterTest : FunSpec() {
    /** A filter that records received calls in the form "replace:offset:length:text" and lets through only those for which [accept] returns true. */
    private class Recording(private val accept: (String?) -> Boolean = { true }) : WDocumentFilter() {
        val calls = mutableListOf<String>()
        val documents = mutableListOf<WDocument>()

        override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: WAttributeSet?) {
            calls += "insert:$offset:$string"
            documents += fb.document
            if (accept(string)) super.insertString(fb, offset, string, attr)
        }

        override fun remove(fb: FilterBypass, offset: Int, length: Int) {
            calls += "remove:$offset:$length"
            documents += fb.document
            super.remove(fb, offset, length)
        }

        override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: WAttributeSet?) {
            calls += "replace:$offset:$length:$text"
            documents += fb.document
            if (accept(text)) super.replace(fb, offset, length, text, attrs)
        }
    }

    /** Lets only digits through (the form commonly written in Swing). */
    private val digitsOnly: (String?) -> Boolean = { it == null || it.all(Char::isDigit) }

    /** A filter that rewrites inserted text to upper case. */
    private class UpperCase : WDocumentFilter() {
        override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: WAttributeSet?) =
            fb.insertString(offset, string?.uppercase(), attr)

        override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: WAttributeSet?) =
            fb.replace(offset, length, text?.uppercase(), attrs)
    }

    /** A listener that records received events in the form "insert:offset:length". */
    private class Events : WDocumentListener {
        val events = mutableListOf<String>()

        override fun insertUpdate(e: WDocumentEvent) {
            events += "insert:${e.offset}:${e.length}"
        }

        override fun removeUpdate(e: WDocumentEvent) {
            events += "remove:${e.offset}:${e.length}"
        }

        override fun changedUpdate(e: WDocumentEvent) = Unit
    }

    /** A key to send (a Win32 virtual key code and scan code). */
    private class Key(val virtualKey: Int, val scanCode: Int)

    /** Places a text field with [filter] in the shared window. */
    private fun shownField(text: String, filter: WDocumentFilter): WTextField {
        val field = onUiThreadGet {
            WTextField().also {
                it.text = text
                it.document.documentFilter = filter
            }
        }
        UiTestHarness.attachAndAwaitLoaded(field)
        return field
    }

    /** Moves the focus to [field], selects [selection] (start, end), and presses the keys. */
    private fun type(field: WTextField, selection: Pair<Int, Int>, key: Key) {
        onUiThread {
            check(field.requestFocusInWindow()) { "Cannot move the focus" }
            field.select(selection.first, selection.second)
        }
        TestKeyboard.press(key.virtualKey, key.scanCode)
    }

    init {
        context("Defaults") {
            test("documentFilter is null by default (the three kinds of text fields)") {
                onUiThreadGet {
                    listOf(WTextField().document.documentFilter, WTextPane().document.documentFilter, WPasswordField().document.documentFilter)
                } shouldBe listOf(null, null, null)
            }
        }

        context("Reading and writing") {
            test("the Document's insertString, remove and replace go through the filter's methods of the same names, and the FilterBypass document is the same Document") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "ab"
                    val filter = Recording()
                    field.document.documentFilter = filter
                    field.document.insertString(2, "c", null)
                    field.document.remove(0, 1)
                    field.document.replace(0, 1, "X", null)
                    listOf(filter.calls, field.text, filter.documents.all { it === field.document })
                } shouldBe listOf(listOf("insert:2:c", "remove:0:1", "replace:0:1:X"), "Xc", true)
            }

            test("a digits-only filter: a rejected insertString does not change the text and nothing reaches the listeners") {
                onUiThreadGet {
                    val field = WTextField()
                    val events = Events()
                    field.addDocumentListener(events)
                    field.document.documentFilter = Recording(digitsOnly)
                    field.document.insertString(0, "12", null)
                    field.document.insertString(2, "a", null)
                    field.text to events.events
                } shouldBe ("12" to listOf("insert:0:2"))
            }

            test("setting text goes through replace(0, old length, new text), as with Swing's setText (unchanged if rejected)") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "12"
                    val filter = Recording(digitsOnly)
                    field.document.documentFilter = filter
                    field.text = "345"
                    field.text = "x"
                    filter.calls to field.text
                } shouldBe (listOf("replace:0:2:345", "replace:0:3:x") to "345")
            }

            test("replaceSelection goes through replace(selection start, selection length, text), and the caret after it is behind the inserted text") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "abcd"
                    field.document.documentFilter = UpperCase()
                    field.select(1, 3)
                    field.replaceSelection("xyz")
                    listOf(field.text, field.caretPosition, field.selectedText)
                } shouldBe listOf("aXYZd", 4, "")
            }

            test("a rewriting filter: the text passed to FilterBypass is put in (WTextPane's append / insert / replaceRange and WPasswordField's setText)") {
                onUiThreadGet {
                    val pane = WTextPane()
                    pane.document.documentFilter = UpperCase()
                    pane.append("ab")
                    pane.insert("c", 0)
                    pane.replaceRange("d", 1, 2)
                    val password = WPasswordField()
                    password.document.documentFilter = UpperCase()
                    password.setText("pw")
                    pane.text to String(password.getPassword())
                } shouldBe ("CDB" to "PW")
            }

            test("WTextPane's text and replaceSelection also go through the filter (the same as Swing's JTextArea)") {
                onUiThreadGet {
                    val pane = WTextPane()
                    val filter = Recording(digitsOnly)
                    pane.document.documentFilter = filter
                    pane.text = "12"
                    pane.select(0, 1)
                    pane.replaceSelection("a")
                    filter.calls to pane.text
                } shouldBe (listOf("replace:0:0:12", "replace:0:1:a") to "12")
            }

            test("resetting documentFilter to null accepts all changes again") {
                onUiThreadGet {
                    val field = WTextField()
                    field.document.documentFilter = Recording(digitsOnly)
                    field.text = "a"
                    field.document.documentFilter = null
                    field.text = "b"
                    field.text
                } shouldBe "b"
            }
        }

        context("Input") {
            test("characters typed with keys go through the filter's replace(caret position, 0, character), and a digits-only filter rejects letters") {
                val filter = Recording(digitsOnly)
                val field = shownField("12", filter)
                type(field, 2 to 2, KEY_A)
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { filter.calls.toList() to field.text } shouldBe (listOf("replace:2:0:a") to "12")
                type(field, 1 to 1, KEY_3)
                eventually(3.seconds) { onUiThreadGet { field.text } shouldBe "132" }
                onUiThreadGet { filter.calls.last() } shouldBe "replace:1:0:3"
            }

            test("typing over selected text calls replace(selection start, selection length, character), and Backspace calls remove(before the caret, 1)") {
                val filter = Recording()
                val field = shownField("abcd", filter)
                type(field, 1 to 3, KEY_A)
                eventually(3.seconds) { onUiThreadGet { field.text } shouldBe "aad" }
                type(field, 2 to 2, KEY_BACK)
                eventually(3.seconds) { onUiThreadGet { field.text } shouldBe "ad" }
                onUiThreadGet { filter.calls.toList() } shouldBe listOf("replace:1:2:a", "remove:1:1")
            }

            test("a rewriting filter: typed lower-case letters go in as upper case, the caret is after them, and listeners receive the upper-case insertUpdate") {
                val field = shownField("xy", UpperCase())
                val events = Events()
                onUiThread { field.addDocumentListener(events) }
                type(field, 1 to 1, KEY_A)
                eventually(3.seconds) { onUiThreadGet { field.text } shouldBe "xAy" }
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { listOf(field.caretPosition, events.events.toList()) } shouldBe listOf(2, listOf("insert:1:1"))
            }
        }

        context("Boundaries and exceptions") {
            test("when the filter passes a range outside the text to FilterBypass, IllegalArgumentException is thrown and the text does not change") {
                onUiThreadGet {
                    val field = WTextField()
                    field.text = "ab"
                    field.document.documentFilter = object : WDocumentFilter() {
                        override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: WAttributeSet?) =
                            fb.insertString(offset + 10, string, attr)
                    }
                    val error = runCatching { field.document.insertString(0, "x", null) }.exceptionOrNull()
                    (error is IllegalArgumentException) to field.text
                } shouldBe (true to "ab")
            }
        }
    }

    private companion object {
        val KEY_A = Key(0x41, 0x1E)
        val KEY_3 = Key(0x33, 0x04)
        val KEY_BACK = Key(0x08, 0x0E)

        /** The time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
