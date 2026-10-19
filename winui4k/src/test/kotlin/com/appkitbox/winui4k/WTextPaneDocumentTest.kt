package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests WTextPane.addDocumentListener (DocumentListener-style). The expected values are the positions and lengths of
 * insertUpdate / removeUpdate when the same operations are done on Swing's JTextArea (PlainDocument). Key input is sent
 * to the shared window with [TestKeyboard].
 */
class WTextPaneDocumentTest : FunSpec() {
    /** A listener that records received events in the form "insert:offset:length:text at that time". */
    private class Recorder(private val pane: WTextPane) : WDocumentListener {
        val events = mutableListOf<String>()
        val sources = mutableListOf<Any?>()

        override fun insertUpdate(e: WDocumentEvent) = record("insert", e)

        override fun removeUpdate(e: WDocumentEvent) = record("remove", e)

        override fun changedUpdate(e: WDocumentEvent) = record("changed", e)

        private fun record(kind: String, e: WDocumentEvent) {
            check(kind == e.type.name.lowercase().replace("change", "changed")) { "the method and type do not match: $kind / ${e.type}" }
            events += "$kind:${e.offset}:${e.length}:${pane.text.replace("\n", "|")}"
            sources += e.source
        }
    }

    /** A key to send (Win32 virtual key code and scan code). */
    private class Key(val virtualKey: Int, val scanCode: Int)

    /** Puts a text pane with a listener attached into the shared window. */
    private fun shownPane(text: String): Pair<WTextPane, Recorder> {
        val pair = onUiThreadGet {
            val pane = WTextPane()
            pane.text = text
            val recorder = Recorder(pane)
            pane.addDocumentListener(recorder)
            pane to recorder
        }
        UiTestHarness.attachAndAwaitLoaded(pair.first)
        return pair
    }

    /** Returns [recorder]'s records after waiting briefly to confirm nothing arrives late. */
    private fun settledEvents(recorder: Recorder): List<String> {
        Thread.sleep(SETTLE_MILLIS)
        return onUiThreadGet { recorder.events.toList() }
    }

    /** Moves focus to [pane], selects [selection] (start, end) (or places the caret if they are equal), presses a key and waits for [expected]. */
    private suspend fun type(pane: WTextPane, recorder: Recorder, selection: Pair<Int, Int>, key: Key, expected: List<String>) {
        onUiThread {
            check(pane.requestFocusInWindow()) { "could not move focus" }
            pane.select(selection.first, selection.second)
            recorder.events.clear()
        }
        TestKeyboard.press(key.virtualKey, key.scanCode)
        eventually(3.seconds) { onUiThreadGet { recorder.events.toList() } shouldBe expected }
    }

    /** A listener that calls [mutate] within a notification and catches and records the IllegalStateException. */
    private class Mutating(private val mutate: () -> Unit) : WDocumentListener {
        val caught = mutableListOf<String?>()

        override fun insertUpdate(e: WDocumentEvent) {
            try {
                mutate()
            } catch (ex: IllegalStateException) {
                caught += ex.message
            }
        }

        override fun removeUpdate(e: WDocumentEvent) = Unit

        override fun changedUpdate(e: WDocumentEvent) = Unit
    }

    init {
        test("when a listener sets text during a notification, it throws IllegalStateException (Attempt to mutate in notification) as in Swing, the text is unchanged, and the remaining listeners receive only the original INSERT") {
            onUiThreadGet {
                val field = WTextPane()
                val mutating = Mutating { field.text = "z" }
                field.addDocumentListener(mutating)
                val recorder = Recorder(field)
                field.addDocumentListener(recorder)
                field.text = "abc"
                Triple(mutating.caught, recorder.events, field.text)
            } shouldBe Triple(listOf<String?>("Attempt to mutate in notification"), listOf("insert:0:3:abc"), "abc")
        }
        test("setting text from empty delivers only an insertUpdate for the whole, and emptying it only a removeUpdate for the whole (delivered even before placing it)") {
            onUiThreadGet {
                val pane = WTextPane()
                val recorder = Recorder(pane)
                pane.addDocumentListener(recorder)
                pane.text = "abc"
                pane.text = ""
                recorder.events.toList()
            } shouldContainExactly listOf("insert:0:3:abc", "remove:0:3:")
        }

        test("setting text to another string delivers removeUpdate for the whole old text and then insertUpdate for the whole new text, as Swing's setText does") {
            val (pane, recorder) = shownPane("abc")
            try {
                onUiThread { pane.text = "abd\nxy" }
                settledEvents(recorder) shouldContainExactly listOf("remove:0:3:abd|xy", "insert:0:6:abd|xy")
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("setting the same string to text delivers nothing, and the source is the text pane") {
            val (pane, recorder) = shownPane("Same")
            try {
                onUiThread { pane.text = "Same" }
                settledEvents(recorder) shouldContainExactly emptyList()
                onUiThread { pane.text = "x" }
                settledEvents(recorder).size shouldBe 2
                onUiThreadGet { recorder.sources.all { it === pane } } shouldBe true
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("append gives an insertUpdate at the end, and insert gives an insertUpdate at the given position (a line break is 1 character)") {
            val (pane, recorder) = shownPane("ab")
            try {
                onUiThread {
                    pane.append("\ncd")
                    pane.insert("X", 0)
                }
                settledEvents(recorder) shouldContainExactly listOf("insert:2:3:ab|cd", "insert:0:1:Xab|cd")
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("replaceRange gives a removeUpdate for the whole range followed by an insertUpdate for the inserted text, and null gives only the removeUpdate") {
            val (pane, recorder) = shownPane("abcdef")
            try {
                onUiThread {
                    pane.replaceRange("XY", 1, 3)
                    pane.replaceRange(null, 0, 1)
                }
                settledEvents(recorder) shouldContainExactly listOf(
                    "remove:1:2:aXYdef",
                    "insert:1:2:aXYdef",
                    "remove:0:1:XYdef",
                )
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("replaceSelection delivers removeUpdate for the whole selection and then insertUpdate for the inserted text, or only insertUpdate if there is no selection") {
            val (pane, recorder) = shownPane("abcdef")
            try {
                onUiThread {
                    pane.select(2, 4)
                    pane.replaceSelection("cd") // Replacing with the same text also counts as remove-then-insert, as in Swing
                    pane.caretPosition = 6
                    pane.replaceSelection("!")
                    pane.select(0, 1)
                    pane.replaceSelection(null)
                }
                settledEvents(recorder) shouldContainExactly listOf(
                    "remove:2:2:abcdef",
                    "insert:2:2:abcdef",
                    "insert:6:1:abcdef!",
                    "remove:0:1:bcdef!",
                )
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("cut arrives as a removeUpdate of the selection") {
            val (pane, recorder) = shownPane("abcdef")
            try {
                onUiThread {
                    pane.select(1, 3)
                    pane.cut()
                }
                settledEvents(recorder) shouldContainExactly listOf("remove:1:2:adef")
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("toggling bold arrives as a changedUpdate of the selection, and nothing arrives without a selection") {
            val (pane, recorder) = shownPane("abcdef")
            try {
                onUiThread {
                    pane.caretPosition = 2
                    pane.toggleSelectionBold()
                    pane.select(1, 4)
                    pane.toggleSelectionBold()
                    pane.toggleSelectionItalic()
                }
                settledEvents(recorder) shouldContainExactly listOf("changed:1:3:abcdef", "changed:1:3:abcdef")
                onUiThreadGet { pane.text } shouldBe "abcdef"
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("a character typed with a key is delivered as an insertUpdate at the caret position") {
            val (pane, recorder) = shownPane("bc")
            try {
                type(pane, recorder, 0 to 0, KEY_A, listOf("insert:0:1:abc"))
                settledEvents(recorder) shouldContainExactly listOf("insert:0:1:abc")
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("a line break entered with Enter arrives as a 1-character insertUpdate, and text returns \\n") {
            val (pane, recorder) = shownPane("ab")
            try {
                type(pane, recorder, 1 to 1, KEY_ENTER, listOf("insert:1:1:a|b"))
                onUiThreadGet { pane.text } shouldBe "a\nb"
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("a character deleted with Backspace is a removeUpdate at the deleted position, even where the same character repeats") {
            val (pane, recorder) = shownPane("aaa")
            try {
                type(pane, recorder, 1 to 1, KEY_BACK, listOf("remove:0:1:aa"))
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("retyping over selected characters delivers removeUpdate for the selection and then insertUpdate for the typed character") {
            val (pane, recorder) = shownPane("abcd")
            try {
                type(pane, recorder, 1 to 3, KEY_X, listOf("remove:1:2:axd", "insert:1:1:axd"))
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("a listener removed with removeDocumentListener receives nothing and disappears from documentListeners") {
            val (pane, recorder) = shownPane("")
            try {
                val listeners = onUiThreadGet {
                    pane.removeDocumentListener(recorder)
                    pane.text = "after removal"
                    pane.documentListeners.toList()
                }
                listeners shouldContainExactly emptyList()
                settledEvents(recorder) shouldContainExactly emptyList()
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("adding and removing a null listener does nothing") {
            onUiThreadGet {
                val pane = WTextPane()
                pane.addDocumentListener(null)
                pane.removeDocumentListener(null)
                pane.documentListeners.size
            } shouldBe 0
        }
    }

    private companion object {
        val KEY_A = Key(0x41, 0x1E)
        val KEY_X = Key(0x58, 0x2D)
        val KEY_BACK = Key(0x08, 0x0E)
        val KEY_ENTER = Key(0x0D, 0x1C)

        /** Time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
