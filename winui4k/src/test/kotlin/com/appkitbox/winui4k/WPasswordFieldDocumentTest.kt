package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WPasswordField.addDocumentListener (DocumentListener-like). The expected values are the offsets and lengths of
 * insertUpdate / removeUpdate when the same operation is done in Swing's JPasswordField (PlainDocument). Key input is
 * sent to the shared window with [TestKeyboard].
 */
class WPasswordFieldDocumentTest : FunSpec() {
    /** A listener that records received events in the form "insert:offset:length:password at that time". */
    private class Recorder(private val field: WPasswordField) : WDocumentListener {
        val events = mutableListOf<String>()
        val sources = mutableListOf<Any?>()

        override fun insertUpdate(e: WDocumentEvent) = record("insert", e)

        override fun removeUpdate(e: WDocumentEvent) = record("remove", e)

        override fun changedUpdate(e: WDocumentEvent) = record("changed", e)

        private fun record(kind: String, e: WDocumentEvent) {
            events += "$kind:${e.offset}:${e.length}:${field.password}"
            sources += e.source
        }
    }

    private fun shownField(text: String): Pair<WPasswordField, Recorder> {
        val pair = onUiThreadGet {
            val field = WPasswordField()
            field.password = text
            val recorder = Recorder(field)
            field.addDocumentListener(recorder)
            field to recorder
        }
        UiTestHarness.attachAndAwaitLoaded(pair.first)
        return pair
    }

    private fun settledEvents(recorder: Recorder): List<String> {
        Thread.sleep(SETTLE_MILLIS)
        return onUiThreadGet { recorder.events.toList() }
    }

    init {
        test("setting password from empty delivers only an insertUpdate for the whole, and setting it empty only a removeUpdate for the whole (even before being shown)") {
            onUiThreadGet {
                val field = WPasswordField()
                val recorder = Recorder(field)
                field.addDocumentListener(recorder)
                field.password = "abc"
                field.password = ""
                recorder.events.toList()
            } shouldContainExactly listOf("insert:0:3:abc", "remove:0:3:")
        }

        test("setting password to another string delivers removeUpdate for the whole old text and then insertUpdate for the whole new text") {
            val (field, recorder) = shownField("old")
            try {
                onUiThread { field.setText("new!") }
                settledEvents(recorder) shouldContainExactly listOf("remove:0:3:new!", "insert:0:4:new!")
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("setting the same string delivers nothing, source is the password field, and changedUpdate is never delivered") {
            val (field, recorder) = shownField("same")
            try {
                onUiThread {
                    field.password = "same"
                    field.password = "same2"
                }
                settledEvents(recorder) shouldContainExactly listOf("remove:0:4:same2", "insert:0:5:same2") // Setting replaces the whole
                onUiThreadGet { recorder.sources.all { it === field } } shouldBe true
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("characters typed with keys arrive as insertUpdate, and characters deleted with Backspace arrive as removeUpdate") {
            val (field, recorder) = shownField("")
            try {
                onUiThread { check(field.requestFocusInWindow()) { "could not move focus" } }
                TestKeyboard.press(KEY_A_VK, KEY_A_SCAN)
                eventually(3.seconds) { onUiThreadGet { recorder.events.toList() } shouldBe listOf("insert:0:1:a") }
                TestKeyboard.press(KEY_B_VK, KEY_B_SCAN)
                eventually(3.seconds) {
                    onUiThreadGet { recorder.events.toList() } shouldBe listOf("insert:0:1:a", "insert:1:1:ab")
                }
                TestKeyboard.press(KEY_BACK_VK, KEY_BACK_SCAN)
                eventually(3.seconds) {
                    onUiThreadGet { recorder.events.toList() } shouldBe listOf("insert:0:1:a", "insert:1:1:ab", "remove:1:1:a")
                }
                settledEvents(recorder).size shouldBe 3
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("a listener removed with removeDocumentListener receives nothing and disappears from documentListeners") {
            val (field, recorder) = shownField("")
            try {
                val listeners = onUiThreadGet {
                    field.removeDocumentListener(recorder)
                    field.password = "after removal"
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
                val field = WPasswordField()
                field.addDocumentListener(null)
                field.removeDocumentListener(null)
                field.documentListeners.size
            } shouldBe 0
        }
    }

    private companion object {
        const val KEY_A_VK = 0x41
        const val KEY_A_SCAN = 0x1E
        const val KEY_B_VK = 0x42
        const val KEY_B_SCAN = 0x30
        const val KEY_BACK_VK = 0x08
        const val KEY_BACK_SCAN = 0x0E

        /** Time to wait to confirm that nothing arrives. */
        const val SETTLE_MILLIS = 700L
    }
}
