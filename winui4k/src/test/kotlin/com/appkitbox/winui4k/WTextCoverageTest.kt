package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Tests for the public members of the text classes that had no tests (headers, wrapping, whether text can be selected, TextChanged of WTextPane,
 * selection of WRichTextBlock, the suggestion list of WAutoSuggestBox and removing listeners).
 */
@Tags("E2E")
class WTextCoverageTest : FunSpec() {
    init {
        context("Defaults") {
            test("WRichTextBlock defaults to wrapping, no trimming and selectable") {
                onUiThreadGet {
                    val block = WRichTextBlock()
                    Triple(block.textWrapping, block.textTrimming, block.isTextSelectionEnabled)
                } shouldBe Triple(TextWrapping.WRAP, TextTrimming.NONE, true)
            }

            test("WLabel defaults to not wrapping and not selectable") {
                onUiThreadGet {
                    val label = WLabel("Name")
                    label.textWrapping to label.isTextSelectionEnabled
                } shouldBe (TextWrapping.NO_WRAP to false)
            }

            test("the header of input fields and WAutoSuggestBox defaults to empty, and the WAutoSuggestBox suggestion list is closed") {
                onUiThreadGet {
                    val suggest = WAutoSuggestBox()
                    listOf(WTextField().header, WPasswordField().header, WTextPane().header, suggest.header, suggest.isSuggestionListOpen)
                } shouldContainExactly listOf("", "", "", "", false)
            }
        }

        context("Reading and writing") {
            test("header reads back the set string") {
                onUiThreadGet {
                    val field = WTextField().apply { header = "Name" }
                    val password = WPasswordField().apply { header = "Password" }
                    val pane = WTextPane().apply { header = "Body" }
                    val suggest = WAutoSuggestBox().apply { header = "Search" }
                    listOf(field.header, password.header, pane.header, suggest.header)
                } shouldContainExactly listOf("Name", "Password", "Body", "Search")
            }

            test("WTextField's textAlignment reads back the set value") {
                onUiThreadGet {
                    val field = WTextField()
                    field.textAlignment = TextAlignment.RIGHT
                    field.textAlignment
                } shouldBe TextAlignment.RIGHT
            }

            test("textWrapping and isTextSelectionEnabled of WLabel and WRichTextBlock read back the set values") {
                onUiThreadGet {
                    val label = WLabel("Name")
                    label.textWrapping = TextWrapping.WRAP_WHOLE_WORDS
                    label.isTextSelectionEnabled = true
                    val block = WRichTextBlock()
                    block.textWrapping = TextWrapping.NO_WRAP
                    block.textTrimming = TextTrimming.CHARACTER_ELLIPSIS
                    block.isTextSelectionEnabled = false
                    listOf(label.textWrapping, label.isTextSelectionEnabled, block.textWrapping, block.textTrimming, block.isTextSelectionEnabled)
                } shouldContainExactly listOf(TextWrapping.WRAP_WHOLE_WORDS, true, TextWrapping.NO_WRAP, TextTrimming.CHARACTER_ELLIPSIS, false)
            }

            test("reading WTextPane's rtfText and setting it again does not change text (no empty paragraph is added at the end)") {
                onUiThreadGet {
                    listOf("Make it bold", "2 lines\nhere", "Ends with a line break\n", "").map { original ->
                        val pane = WTextPane()
                        pane.text = original
                        pane.selectAll()
                        pane.toggleSelectionBold()
                        val copy = WTextPane()
                        copy.rtfText = pane.rtfText
                        copy.rtfText = copy.rtfText
                        copy.text
                    }
                } shouldContainExactly listOf("Make it bold", "2 lines\nhere", "Ends with a line break\n", "")
            }

            test("selectAll of WRichTextBlock makes selectedText the whole text (after being put in a window)") {
                val block = onUiThreadGet { WRichTextBlock().apply { text = "Text to select" } }
                UiTestHarness.attachAndAwaitLoaded(block)
                try {
                    onUiThreadGet {
                        val before = block.selectedText
                        block.selectAll()
                        before to block.selectedText
                    } shouldBe ("" to "Text to select")
                } finally {
                    UiTestHarness.detach(block)
                }
            }
        }

        context("Events") {
            test("WTextPane's addTextChangedListener is called on every text change, and not after removeTextChangedListener") {
                // TextChanged arrives through the message loop after the template is applied, and a notification of the empty string right after loading may arrive first
                // (measured). So wait with a monitoring listener until the expected string arrives. Once the monitoring listener that is not removed receives "Two",
                // it is certain that no more notifications come to the removed listener
                val removed = LinkedBlockingQueue<String>()
                val monitor = LinkedBlockingQueue<String>()
                val listener: () -> Unit = { removed.add("removed") }
                val pane = onUiThreadGet {
                    WTextPane().also {
                        it.addTextChangedListener(listener)
                        it.addTextChangedListener { monitor.add(it.text) }
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(pane)
                try {
                    onUiThread { pane.text = "One" }
                    awaitValue(monitor, "One") shouldBe true
                    val removedBefore = onUiThreadGet {
                        pane.removeTextChangedListener(listener)
                        pane.text = "Two"
                        removed.size
                    }
                    awaitValue(monitor, "Two") shouldBe true
                    (removedBefore >= 1) shouldBe true
                    removed.size shouldBe removedBefore
                } finally {
                    UiTestHarness.detach(pane)
                }
            }

            test("not called after WPasswordField's removePasswordChangedListener") {
                val removed = LinkedBlockingQueue<String>()
                val monitor = LinkedBlockingQueue<String>()
                val listener: (String) -> Unit = { removed.add(it) }
                val field = onUiThreadGet {
                    WPasswordField().also {
                        it.addPasswordChangedListener(listener)
                        it.addPasswordChangedListener { p -> monitor.add(p) }
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(field)
                try {
                    onUiThread { field.password = "a" }
                    monitor.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "a"
                    onUiThread {
                        field.removePasswordChangedListener(listener)
                        field.password = "ab"
                    }
                    monitor.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "ab"
                    removed.toList() shouldContainExactly listOf("a")
                } finally {
                    UiTestHarness.detach(field)
                }
            }

            test("not called after WAutoSuggestBox's removeTextChangedListener") {
                val removed = LinkedBlockingQueue<String>()
                val monitor = LinkedBlockingQueue<String>()
                val listener: (String, TextChangeReason) -> Unit = { text, _ -> removed.add(text) }
                val box = onUiThreadGet {
                    WAutoSuggestBox().also {
                        it.addTextChangedListener(listener)
                        it.addTextChangedListener { text, _ -> monitor.add(text) }
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(box)
                try {
                    onUiThread { box.text = "a" }
                    monitor.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "a"
                    onUiThread {
                        box.removeTextChangedListener(listener)
                        box.text = "ab"
                    }
                    monitor.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe "ab"
                    removed.toList() shouldContainExactly listOf("a")
                } finally {
                    UiTestHarness.detach(box)
                }
            }
        }
    }

    /** Waits until [expected] comes out of [queue] (discarding other values). true if it comes out in time. */
    private fun awaitValue(queue: LinkedBlockingQueue<String>, expected: String): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(UiTestHarness.TIMEOUT_SECONDS)
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) return false
            if (queue.poll(remaining, TimeUnit.NANOSECONDS) == expected) return true
        }
    }
}
