package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify WTextField.addDocumentListener (DocumentListener-style). The expected values are the positions and
 * lengths of insertUpdate / removeUpdate when the same operation is done on Swing's JTextField (PlainDocument). Key input
 * is sent to the shared window with [TestKeyboard].
 */
class WTextFieldDocumentTest : FunSpec() {
    /** A listener that records received events in the form "insert:offset:length:text at that time". */
    private class Recorder(private val field: WTextField) : WDocumentListener {
        val events = mutableListOf<String>()
        val sources = mutableListOf<Any?>()

        override fun insertUpdate(e: WDocumentEvent) = record("insert", e)

        override fun removeUpdate(e: WDocumentEvent) = record("remove", e)

        override fun changedUpdate(e: WDocumentEvent) = record("changed", e)

        private fun record(kind: String, e: WDocumentEvent) {
            check(kind == e.type.name.lowercase().replace("change", "changed")) { "the method and type do not match: $kind / ${e.type}" }
            events += "$kind:${e.offset}:${e.length}:${field.text}"
            sources += e.source
        }
    }

    /** A key to send (Win32 virtual key code and scan code). */
    private class Key(val virtualKey: Int, val scanCode: Int)

    /** Places an input field with the listener in the shared window. */
    private fun shownField(text: String): Pair<WTextField, Recorder> {
        val pair = onUiThreadGet {
            val field = WTextField()
            field.text = text
            val recorder = Recorder(field)
            field.addDocumentListener(recorder)
            field to recorder
        }
        UiTestHarness.attachAndAwaitLoaded(pair.first)
        return pair
    }

    /** Returns [recorder]'s records after waiting briefly to confirm nothing arrives late. */
    private fun settledEvents(recorder: Recorder): List<String> {
        Thread.sleep(SETTLE_MILLIS)
        return onUiThreadGet { recorder.events.toList() }
    }

    /** Moves focus to [field], selects [selection] (start, end) (placing the caret if they are the same), and presses the key. */
    private suspend fun type(field: WTextField, recorder: Recorder, selection: Pair<Int, Int>, key: Key, expected: List<String>) {
        onUiThread {
            check(field.requestFocusInWindow()) { "could not move focus" }
            field.select(selection.first, selection.second)
            recorder.events.clear()
        }
        TestKeyboard.press(key.virtualKey, key.scanCode)
        eventually(3.seconds) { onUiThreadGet { recorder.events.toList() } shouldBe expected }
    }

    init {
        test("setting text from empty delivers only an insertUpdate for the whole, and emptying it only a removeUpdate for the whole (delivered even before placing it)") {
            onUiThreadGet {
                val field = WTextField()
                val recorder = Recorder(field)
                field.addDocumentListener(recorder)
                field.text = "abc"
                field.text = ""
                recorder.events.toList()
            } shouldContainExactly listOf("insert:0:3:abc", "remove:0:3:")
        }

        test("setting text to another string delivers removeUpdate for the whole old text and then insertUpdate for the whole new text, as Swing's setText does") {
            val (field, recorder) = shownField("abc")
            try {
                onUiThread { field.text = "abd" }
                settledEvents(recorder) shouldContainExactly listOf("remove:0:3:abd", "insert:0:3:abd")
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("setting the same string to text delivers nothing") {
            val (field, recorder) = shownField("Same")
            try {
                onUiThread { field.text = "Same" }
                settledEvents(recorder) shouldContainExactly emptyList()
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("the event's source and component are the input field, and changedUpdate is not delivered") {
            val (field, recorder) = shownField("")
            try {
                onUiThread { field.text = "x" }
                settledEvents(recorder) shouldContainExactly listOf("insert:0:1:x")
                onUiThreadGet { recorder.sources.all { it === field } } shouldBe true
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("replaceSelection delivers removeUpdate for the whole selection and then insertUpdate for the inserted text, or only insertUpdate if there is no selection") {
            val (field, recorder) = shownField("abcdef")
            try {
                onUiThread {
                    field.select(2, 4)
                    field.replaceSelection("cd") // replacing with the same text is also treated as remove then insert, as in Swing
                    field.caretPosition = 6
                    field.replaceSelection("!")
                    field.select(0, 1)
                    field.replaceSelection(null)
                }
                settledEvents(recorder) shouldContainExactly listOf(
                    "remove:2:2:abcdef",
                    "insert:2:2:abcdef",
                    "insert:6:1:abcdef!",
                    "remove:0:1:bcdef!",
                )
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("a character typed with a key is delivered as an insertUpdate at the caret position") {
            val (field, recorder) = shownField("bc")
            try {
                type(field, recorder, 0 to 0, KEY_A, listOf("insert:0:1:abc"))
                settledEvents(recorder) shouldContainExactly listOf("insert:0:1:abc")
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("a character typed at the start of a run of the same character is also an insertUpdate at the typed position (0) (same as Swing)") {
            val (field, recorder) = shownField("aa")
            try {
                type(field, recorder, 0 to 0, KEY_A, listOf("insert:0:1:aaa"))
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("a character deleted with Backspace is a removeUpdate at the deleted position, even where the same character repeats") {
            val (field, recorder) = shownField("aaa")
            try {
                type(field, recorder, 1 to 1, KEY_BACK, listOf("remove:0:1:aa"))
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("retyping over selected characters delivers removeUpdate for the selection and then insertUpdate for the typed character") {
            val (field, recorder) = shownField("abcd")
            try {
                type(field, recorder, 1 to 3, KEY_X, listOf("remove:1:2:axd", "insert:1:1:axd"))
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("a listener removed with removeDocumentListener receives nothing and disappears from documentListeners") {
            val (field, recorder) = shownField("")
            try {
                val listeners = onUiThreadGet {
                    field.removeDocumentListener(recorder)
                    field.text = "after removal"
                    field.documentListeners.toList()
                }
                listeners shouldContainExactly emptyList()
                settledEvents(recorder) shouldContainExactly emptyList()
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("adding and removing a null listener does nothing") {
            onUiThreadGet {
                val field = WTextField()
                field.addDocumentListener(null)
                field.removeDocumentListener(null)
                field.documentListeners.size
            } shouldBe 0
        }
    }

    private companion object {
        val KEY_A = Key(0x41, 0x1E)
        val KEY_X = Key(0x58, 0x2D)
        val KEY_BACK = Key(0x08, 0x0E)

        /** Time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
