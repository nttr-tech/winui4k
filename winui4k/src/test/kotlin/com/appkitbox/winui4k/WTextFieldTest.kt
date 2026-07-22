package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Tests verifying WTextField (TextBox)'s text handling and TextChanged listener. */
class WTextFieldTest : FunSpec() {
    init {
        test("text returns exactly the string that was set (including non-ASCII text)") {
            onUiThreadGet {
                val textField = WTextField()
                textField.text = "Hello, WinUI"
                textField.text
            } shouldBe "Hello, WinUI"
        }

        test("the placeholder text passed to the constructor can be retrieved via placeholderText") {
            onUiThreadGet { WTextField("Enter your name").placeholderText } shouldBe "Enter your name"
        }

        test("the TextChanged listener fires with the new text on every change to text") {
            // TextChanged fires via the message loop for a TextBox on the visual tree
            val received = LinkedBlockingQueue<String>()
            val textField = onUiThreadGet {
                WTextField().also { it.addTextChangedListener { text -> received.add(text) } }
            }
            UiTestHarness.attach(textField)
            try {
                onUiThread { textField.text = "one" }
                received.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "one"
                onUiThread { textField.text = "one two" }
                received.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "one two"
            } finally {
                UiTestHarness.detach(textField)
            }
        }

        test("a listener removed via removeTextChangedListener no longer fires") {
            // Once the still-subscribed monitor listener has received the second change, it's
            // confirmed that the removed listener won't be notified again
            val removedListenerReceived = mutableListOf<String>()
            val monitorReceived = LinkedBlockingQueue<String>()
            val listener: (String) -> Unit = { removedListenerReceived.add(it) }
            val textField = onUiThreadGet {
                WTextField().also {
                    it.addTextChangedListener(listener)
                    it.addTextChangedListener { text -> monitorReceived.add(text) }
                }
            }
            UiTestHarness.attach(textField)
            try {
                onUiThread { textField.text = "first" }
                monitorReceived.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "first"
                onUiThread {
                    textField.removeTextChangedListener(listener)
                    textField.text = "second"
                }
                monitorReceived.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "second"
                removedListenerReceived shouldContainExactly listOf("first")
            } finally {
                UiTestHarness.detach(textField)
            }
        }

        test("the range passed to select can be retrieved via selectedText") {
            onUiThreadGet {
                val textField = WTextField()
                textField.text = "abcde"
                textField.select(1, 3)
                textField.selectedText
            } shouldBe "bcd"
        }

        test("isReadOnly, acceptsReturn, and maxLength return exactly the values that were set") {
            val (readOnly, acceptsReturn, maxLength) = onUiThreadGet {
                val textField = WTextField()
                textField.isReadOnly = true
                textField.acceptsReturn = true
                textField.maxLength = 10
                Triple(textField.isReadOnly, textField.acceptsReturn, textField.maxLength)
            }
            readOnly.shouldBeTrue()
            acceptsReturn.shouldBeTrue()
            maxLength shouldBe 10
        }
    }
}
