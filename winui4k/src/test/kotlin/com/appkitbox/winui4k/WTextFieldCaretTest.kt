package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that verify WTextField.addCaretListener (CaretListener-style). The expected values are the dot (caret) and mark
 * (the other end of the selection) of caretUpdate when the same operation is done on Swing's JTextField. Key input is
 * sent to the shared window with [TestKeyboard].
 */
class WTextFieldCaretTest : FunSpec() {
    /** Records received events in the form "dot:mark". */
    private class Recorder : WCaretListener {
        val events = mutableListOf<String>()
        val sources = mutableListOf<Any?>()

        override fun caretUpdate(e: WCaretEvent) {
            events += "${e.dot}:${e.mark}"
            sources += e.source
        }
    }

    private fun shownField(text: String, caret: Int): Pair<WTextField, Recorder> {
        val pair = onUiThreadGet {
            val field = WTextField()
            field.text = text
            field.caretPosition = caret
            val recorder = Recorder()
            field.addCaretListener(recorder)
            field to recorder
        }
        UiTestHarness.attachAndAwaitLoaded(pair.first)
        // Wait for the SelectionChanged on placing it (same position) before starting
        Thread.sleep(SETTLE_MILLIS)
        onUiThread { pair.second.events.clear() }
        return pair
    }

    private fun settledEvents(recorder: Recorder): List<String> {
        Thread.sleep(SETTLE_MILLIS)
        return onUiThreadGet { recorder.events.toList() }
    }

    init {
        test("changing select and caretPosition immediately produces an event with dot (end) and mark (start), and nothing is delivered again later") {
            val (field, recorder) = shownField("abcdef", 0)
            try {
                val immediate = onUiThreadGet {
                    field.select(1, 4)
                    val afterSelect = recorder.events.toList()
                    field.caretPosition = 2
                    afterSelect to recorder.events.toList()
                }
                immediate shouldBe (listOf("4:1") to listOf("4:1", "2:2"))
                settledEvents(recorder) shouldContainExactly listOf("4:1", "2:2")
                onUiThreadGet { recorder.sources.all { it === field } } shouldBe true
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("select or caretPosition that does not change the position delivers nothing") {
            val (field, recorder) = shownField("abc", 1)
            try {
                onUiThread {
                    field.caretPosition = 1
                    field.select(1, 1)
                }
                settledEvents(recorder) shouldContainExactly emptyList()
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("after replaceSelection, an event for the caret after the inserted text is delivered") {
            val (field, recorder) = shownField("abcdef", 0)
            try {
                onUiThread {
                    field.select(2, 4)
                    recorder.events.clear()
                    field.replaceSelection("XYZ")
                }
                settledEvents(recorder) shouldContainExactly listOf("5:5")
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("moving the caret with the right arrow key delivers an event for the new position") {
            val (field, recorder) = shownField("abc", 0)
            try {
                onUiThread { check(field.requestFocusInWindow()) { "could not move focus" } }
                Thread.sleep(SETTLE_MILLIS)
                onUiThread { recorder.events.clear() }
                TestKeyboard.keyDown(VK_RIGHT, SCAN_RIGHT, extended = true)
                TestKeyboard.keyUp(VK_RIGHT, SCAN_RIGHT, extended = true)
                eventually(3.seconds) { onUiThreadGet { recorder.events.toList() } shouldBe listOf("1:1") }
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("typing a character delivers an event for the caret moving after the typed character") {
            val (field, recorder) = shownField("bc", 0)
            try {
                onUiThread { check(field.requestFocusInWindow()) { "could not move focus" } }
                Thread.sleep(SETTLE_MILLIS)
                onUiThread { recorder.events.clear() }
                TestKeyboard.press(VK_A, SCAN_A)
                eventually(3.seconds) { onUiThreadGet { recorder.events.toList() } shouldBe listOf("1:1") }
                onUiThreadGet { field.text } shouldBe "abc"
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("a listener removed with removeCaretListener receives nothing and disappears from caretListeners; null does nothing") {
            val (field, recorder) = shownField("abc", 0)
            try {
                val listeners = onUiThreadGet {
                    field.addCaretListener(null)
                    field.removeCaretListener(null)
                    field.removeCaretListener(recorder)
                    field.select(0, 2)
                    field.caretListeners.toList()
                }
                listeners shouldContainExactly emptyList()
                settledEvents(recorder) shouldContainExactly emptyList()
            } finally {
                UiTestHarness.detach(field)
            }
        }
    }

    private companion object {
        const val VK_RIGHT = 0x27
        const val SCAN_RIGHT = 0x4D
        const val VK_A = 0x41
        const val SCAN_A = 0x1E

        /** Time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
