package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.function.Consumer

/**
 * Tests that check text change listeners of text fields (addTextChangedListener, addPasswordChangedListener) are called right when the text is set,
 * as with Swing's DocumentListener. WinUI's TextChanged and the like do not come for changes before being added to the screen (Loaded), so the tests
 * check that they are called for settings before showing too, and are not called twice when WinUI's events arrive later after showing.
 */
@Tags("E2E")
class TextChangedBeforeShowTest : FunSpec() {
    /** How long to wait for WinUI's events to arrive later (milliseconds). */
    private val settleMillis = 500L

    private fun <T> settled(read: () -> T): T {
        Thread.sleep(settleMillis)
        return onUiThreadGet(read)
    }

    init {
        test("WTextField: setting text before showing calls the addTextChangedListener listener right away with the new text, and showing does not call it twice") {
            val received = mutableListOf<String>()
            val field = onUiThreadGet {
                WTextField().also {
                    it.addTextChangedListener { text -> received += text }
                    it.text = "Today's notes"
                    received.toList() shouldContainExactly listOf("Today's notes")
                }
            }
            UiTestHarness.attachAndAwaitLoaded(field)
            try {
                settled { received.toList() } shouldContainExactly listOf("Today's notes")
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("WTextField: setting text after showing also calls the listener right away only once, and setting the same text does not call it") {
            val received = mutableListOf<String>()
            val field = onUiThreadGet { WTextField().also { it.addTextChangedListener { text -> received += text } } }
            UiTestHarness.attachAndAwaitLoaded(field)
            try {
                onUiThread {
                    field.text = "ka"
                    received.toList() shouldContainExactly listOf("ka")
                    field.text = "ka"
                    field.text = ""
                    received.toList() shouldContainExactly listOf("ka", "")
                }
                settled { received.toList() } shouldContainExactly listOf("ka", "")
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("WTextField: a listener in the form of a Java Consumer is also called right away when text is set before showing") {
            val received = mutableListOf<String>()
            onUiThread {
                val field = WTextField()
                field.addTextChangedListener(Consumer { text -> received += text })
                field.text = "abc"
            }
            received shouldContainExactly listOf("abc")
        }

        test("WTextField: a listener removed before showing is not called when text is set") {
            val received = mutableListOf<String>()
            onUiThread {
                val field = WTextField()
                val listener: (String) -> Unit = { received += it }
                field.addTextChangedListener(listener)
                field.removeTextChangedListener(listener)
                field.text = "abc"
            }
            received shouldBe emptyList()
        }

        test("WPasswordField: setting password before showing calls the addPasswordChangedListener listener right away, and showing does not call it twice") {
            val received = mutableListOf<String>()
            val field = onUiThreadGet {
                WPasswordField().also {
                    it.addPasswordChangedListener { text -> received += text }
                    it.setText("secret")
                    received.toList() shouldContainExactly listOf("secret")
                }
            }
            UiTestHarness.attachAndAwaitLoaded(field)
            try {
                settled { received.toList() } shouldContainExactly listOf("secret")
                onUiThread {
                    field.password = "secret2"
                    received.toList() shouldContainExactly listOf("secret", "secret2")
                }
                settled { received.toList() } shouldContainExactly listOf("secret", "secret2")
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("WTextPane: setting text before showing calls the addTextChangedListener listener right away, and showing does not call it twice") {
            val received = mutableListOf<String>()
            val pane = onUiThreadGet {
                WTextPane().also { p ->
                    p.addTextChangedListener { received += p.text }
                    p.text = "notes"
                    received.toList() shouldContainExactly listOf("notes")
                }
            }
            UiTestHarness.attachAndAwaitLoaded(pane)
            try {
                settled { received.toList() } shouldContainExactly listOf("notes")
                onUiThread {
                    pane.text = "notes2"
                    received.toList() shouldContainExactly listOf("notes", "notes2")
                }
                settled { received.toList() } shouldContainExactly listOf("notes", "notes2")
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("WAutoSuggestBox: for text set before showing, WinUI's TextChanged delivers it only once with PROGRAMMATIC_CHANGE after showing (it is not lost)") {
            val received = mutableListOf<String>()
            val box = onUiThreadGet {
                WAutoSuggestBox().also {
                    it.addTextChangedListener { text, reason -> received += "$text:$reason" }
                    it.text = "ka"
                }
            }
            UiTestHarness.attachAndAwaitLoaded(box)
            try {
                settled { received.toList() } shouldContainExactly listOf("ka:PROGRAMMATIC_CHANGE")
            } finally {
                UiTestHarness.detach(box)
            }
        }
    }
}
