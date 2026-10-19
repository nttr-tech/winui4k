package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for text / append / foreground of WRichTextBlock (an API modeled on reading and writing JLabel and JTextArea).
 * Paragraphs are separated by the line break "\n", and formatted text (such as bold built with addParagraph) is also
 * included in text.
 */
class WRichTextBlockTextTest : FunSpec() {
    private fun <T> withBlock(block: (WRichTextBlock) -> T): T = onUiThreadGet { block(WRichTextBlock()) }

    init {
        test("text of an empty WRichTextBlock is an empty string") {
            withBlock { it.text } shouldBe ""
        }

        test("text returns the set string as is") {
            withBlock { block ->
                listOf("abc", "Some text", "x y z").map {
                    block.text = it
                    block.text
                }
            } shouldContainExactly listOf("abc", "Some text", "x y z")
        }

        test("line breaks in text separate paragraphs, and trailing and consecutive line breaks can be read back too") {
            withBlock { block ->
                listOf("a\nb", "a\n", "\n\nb", "a\n\nb").map {
                    block.text = it
                    block.text
                }
            } shouldContainExactly listOf("a\nb", "a\n", "\n\nb", "a\n\nb")
        }

        test("\\r\\n and \\r line breaks become a single \\n") {
            withBlock { block ->
                block.text = "a\r\nb\rc"
                block.text
            } shouldBe "a\nb\nc"
        }

        test("setting text replaces the existing content, and setting an empty string leaves nothing") {
            withBlock { block ->
                block.addParagraph { run("Old") }
                block.text = "New"
                val replaced = block.text
                block.text = ""
                replaced to block.text
            } shouldBe ("New" to "")
        }

        test("formatted paragraphs built with addParagraph can also be read as text, with paragraphs joined by line breaks") {
            withBlock { block ->
                block.addParagraph {
                    run("Normal ")
                    bold("Bold")
                    italic("Italic")
                    underline("Underline")
                    run(" End")
                }
                block.addParagraph { run("Second paragraph") }
                block.text
            } shouldBe "Normal BoldItalicUnderline End\nSecond paragraph"
        }

        test("append continues at the end of the last paragraph, and text after a line break becomes a new paragraph") {
            withBlock { block ->
                block.text = "abc"
                block.append("def")
                val joined = block.text
                block.append("\nghi\njkl")
                listOf(joined, block.text)
            } shouldContainExactly listOf("abcdef", "abcdef\nghi\njkl")
        }

        test("append to an empty WRichTextBlock becomes text, and appending null or an empty string does nothing") {
            withBlock { block ->
                block.append(null)
                block.append("")
                val untouched = block.text
                block.append("Beginning")
                untouched to block.text
            } shouldBe ("" to "Beginning")
        }

        test("append after a paragraph formatted with addParagraph continues at the end of that paragraph") {
            withBlock { block ->
                block.addParagraph {
                    run("a")
                    bold("B")
                }
                block.append("c")
                block.text
            } shouldBe "aBc"
        }

        test("text after removeAllParagraphs is an empty string") {
            withBlock { block ->
                block.text = "Removed\nContent"
                block.removeAllParagraphs()
                block.text
            } shouldBe ""
        }

        test("more paragraphs make the displayed height taller (text is actually rendered)") {
            val one = onUiThreadGet { WRichTextBlock().also { it.text = "Line" } }
            val three = onUiThreadGet { WRichTextBlock().also { it.text = "Line\nLine\nLine" } }
            UiTestHarness.attachAndAwaitLoaded(one)
            UiTestHarness.attachAndAwaitLoaded(three)
            try {
                val (oneHeight, threeHeight) = onUiThreadGet {
                    one.updateLayout()
                    three.updateLayout()
                    one.actualHeight to three.actualHeight
                }
                oneHeight shouldBeGreaterThan 0.0
                threeHeight shouldBeGreaterThan oneHeight * 2
            } finally {
                UiTestHarness.detach(one)
                UiTestHarness.detach(three)
            }
        }

        test("the color set to foreground can be read back") {
            withBlock { block ->
                block.foreground = WColor(10, 20, 30)
                block.foreground
            } shouldBe WColor(10, 20, 30)
        }

        test("foreground = null restores the default color from before it was set") {
            val (initial, changed, restored) = withBlock { block ->
                val initial = block.foreground
                block.foreground = WColor(200, 0, 0)
                val changed = block.foreground
                block.foreground = null
                Triple(initial, changed, block.foreground)
            }
            changed shouldNotBe initial
            restored shouldBe initial
        }

        test("setting foreground does not change text or font") {
            val (text, font) = withBlock { block ->
                block.text = "Colored"
                block.font = WFont("Consolas", WFont.BOLD, 20.0)
                block.foreground = WColor(0, 128, 0)
                block.text to block.font
            }
            text shouldBe "Colored"
            font shouldBe WFont("Consolas", WFont.BOLD, 20.0)
        }
    }
}
