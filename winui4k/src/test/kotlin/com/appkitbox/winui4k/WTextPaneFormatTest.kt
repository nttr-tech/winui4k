package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WTextPane's formatting API (JTextPane.setCharacterAttributes / getCharacterAttributes and StyledDocument.setCharacterAttributes).
 * The expected values are the attributes and events from the same operations with Swing's JTextPane (DefaultStyledDocument).
 * The default size and font family should be the same as the text pane's [WTextPane.font] (RichEditBox's FontSize / FontFamily).
 */
@Tags("E2E")
class WTextPaneFormatTest : FunSpec() {
    /** A text pane with the text [text] placed in the shared window (to read after the default style formatting takes effect). */
    private fun shownPane(text: String): WTextPane {
        val pane = onUiThreadGet { WTextPane().also { it.text = text } }
        UiTestHarness.attachAndAwaitLoaded(pane)
        return pane
    }

    /** Attributes for bold, italic, underline, red, 20 and Consolas. */
    private fun everything(): WSimpleAttributeSet = WSimpleAttributeSet().also {
        WStyleConstants.setBold(it, true)
        WStyleConstants.setItalic(it, true)
        WStyleConstants.setUnderline(it, true)
        WStyleConstants.setForeground(it, WColor.RED)
        WStyleConstants.setFontSize(it, 20)
        WStyleConstants.setFontFamily(it, "Consolas")
    }

    /** Turns the six attributes of [attrs] into one readable line. */
    private fun summary(attrs: WAttributeSet): String = listOf(
        if (WStyleConstants.isBold(attrs)) "B" else "-",
        if (WStyleConstants.isItalic(attrs)) "I" else "-",
        if (WStyleConstants.isUnderline(attrs)) "U" else "-",
        WStyleConstants.getFontSize(attrs).toString(),
        WStyleConstants.getFontFamily(attrs),
    ).joinToString(" ")

    /** A listener that records received events in the form "changed:offset:length". */
    private class Events : WDocumentListener {
        val events = mutableListOf<String>()

        override fun insertUpdate(e: WDocumentEvent) {
            events += "insert:${e.offset}:${e.length}"
        }

        override fun removeUpdate(e: WDocumentEvent) {
            events += "remove:${e.offset}:${e.length}"
        }

        override fun changedUpdate(e: WDocumentEvent) {
            events += "changed:${e.offset}:${e.length}"
        }
    }

    init {
        context("Defaults") {
            test("unformatted text is not bold, italic or underlined, and its size and font family are the same as the text pane's font (all six attributes present)") {
                val pane = shownPane("abc")
                val (attrs, font) = onUiThreadGet { pane.getCharacterAttributes() to checkNotNull(pane.font) }
                summary(attrs) shouldBe "- - - ${font.size} ${font.name}"
                attrs.attributeCount shouldBe 6
            }

            test("document is a WStyledDocument, like StyledDocument, and the same as styledDocument") {
                onUiThreadGet {
                    val pane = WTextPane()
                    (pane.document is WStyledDocument) to (pane.document === pane.styledDocument)
                } shouldBe (true to true)
            }
        }

        context("Reading and writing") {
            test("setCharacterAttributes(offset, length, attrs, false) changes only the characters in the range, which getCharacterAttributes(offset) reads back") {
                val pane = shownPane("abcd")
                onUiThreadGet {
                    val doc = pane.styledDocument
                    val untouched = summary(doc.getCharacterAttributes(0))
                    doc.setCharacterAttributes(1, 2, everything(), false)
                    listOf(
                        summary(doc.getCharacterAttributes(0)) == untouched,
                        summary(doc.getCharacterAttributes(1)),
                        summary(doc.getCharacterAttributes(2)),
                        summary(doc.getCharacterAttributes(3)) == untouched,
                        WStyleConstants.getForeground(doc.getCharacterAttributes(2)),
                    )
                } shouldBe listOf(true, "B I U 20 Consolas", "B I U 20 Consolas", true, WColor.RED)
            }

            test("replace = false changes only the passed attributes and keeps the other formatting") {
                val pane = shownPane("abc")
                onUiThreadGet {
                    val doc = pane.styledDocument
                    doc.setCharacterAttributes(0, 3, WSimpleAttributeSet().also { WStyleConstants.setBold(it, true) }, false)
                    doc.setCharacterAttributes(0, 3, WSimpleAttributeSet().also { WStyleConstants.setItalic(it, true) }, false)
                    summary(doc.getCharacterAttributes(1)).substring(0, 5)
                } shouldBe "B I -"
            }

            test("replace = true resets the current formatting to the default before putting it in (bold, size and color return to those of unformatted text)") {
                val pane = shownPane("abcd")
                onUiThreadGet {
                    val doc = pane.styledDocument
                    val untouched = doc.getCharacterAttributes(3)
                    doc.setCharacterAttributes(0, 2, everything(), false)
                    doc.setCharacterAttributes(0, 2, WSimpleAttributeSet().also { WStyleConstants.setItalic(it, true) }, true)
                    val reset = doc.getCharacterAttributes(1)
                    val expected = WSimpleAttributeSet(untouched).also { WStyleConstants.setItalic(it, true) }
                    summary(reset) to summary(expected)
                }.let { (actual, expected) -> actual shouldBe expected }
                onUiThreadGet {
                    val doc = pane.styledDocument
                    WStyleConstants.getForeground(doc.getCharacterAttributes(1)) == WStyleConstants.getForeground(doc.getCharacterAttributes(3))
                } shouldBe true
            }

            test("JTextPane's setCharacterAttributes(attrs, replace) applies to the selection, and getCharacterAttributes() reads the character at the caret position") {
                val pane = shownPane("abcd")
                onUiThreadGet {
                    pane.select(1, 3)
                    pane.setCharacterAttributes(WSimpleAttributeSet().also { WStyleConstants.setBold(it, true) }, false)
                    pane.caretPosition = 0
                    val atStart = WStyleConstants.isBold(pane.getCharacterAttributes())
                    pane.caretPosition = 2
                    val inside = WStyleConstants.isBold(pane.getCharacterAttributes())
                    pane.caretPosition = 3
                    val after = WStyleConstants.isBold(pane.getCharacterAttributes())
                    listOf(atStart, inside, after)
                } shouldBe listOf(false, true, false)
            }

            test("passing attributes to WStyledDocument.insertString formats the inserted text with them (with null it inherits the formatting of the previous character)") {
                val pane = shownPane("ab")
                onUiThreadGet {
                    val doc = pane.styledDocument
                    doc.insertString(1, "XY", WSimpleAttributeSet().also { WStyleConstants.setFontSize(it, 30) })
                    doc.insertString(4, "z", null)
                    listOf(
                        pane.text,
                        WStyleConstants.getFontSize(doc.getCharacterAttributes(1)),
                        WStyleConstants.getFontSize(doc.getCharacterAttributes(2)),
                        WStyleConstants.getFontSize(doc.getCharacterAttributes(3)) == WStyleConstants.getFontSize(doc.getCharacterAttributes(0)),
                    )
                } shouldBe listOf("aXYbz", 30, 30, true)
            }

            test("setCharacterAttributes can change formatting even if isEditable is false (the same as Swing)") {
                val pane = shownPane("abc")
                onUiThreadGet {
                    pane.isEditable = false
                    pane.styledDocument.setCharacterAttributes(0, 1, WSimpleAttributeSet().also { WStyleConstants.setBold(it, true) }, false)
                    WStyleConstants.isBold(pane.styledDocument.getCharacterAttributes(0))
                } shouldBe true
            }

            test("changing the collection returned by getCharacterAttributes does not change the text formatting") {
                val pane = shownPane("abc")
                onUiThreadGet {
                    val attrs = pane.styledDocument.getCharacterAttributes(0) as WMutableAttributeSet
                    WStyleConstants.setBold(attrs, true)
                    WStyleConstants.isBold(pane.styledDocument.getCharacterAttributes(0))
                } shouldBe false
            }
        }

        context("Events") {
            test("setCharacterAttributes delivers only changedUpdate for the range, not counting the part beyond the text. Nothing is delivered for length 0") {
                val pane = shownPane("abcd")
                onUiThreadGet {
                    val events = Events()
                    pane.addDocumentListener(events)
                    val bold = WSimpleAttributeSet().also { WStyleConstants.setBold(it, true) }
                    pane.styledDocument.setCharacterAttributes(1, 2, bold, false)
                    pane.styledDocument.setCharacterAttributes(2, 99, bold, false)
                    pane.styledDocument.setCharacterAttributes(0, 0, bold, false)
                    pane.styledDocument.insertString(0, "x", bold)
                    events.events
                } shouldBe listOf("changed:1:2", "changed:2:2", "insert:0:1")
            }
        }

        context("Boundaries and exceptions") {
            test("getCharacterAttributes at a negative or out-of-range position throws IllegalArgumentException, and an attribute with a wrong value type throws IllegalArgumentException without changing anything") {
                val pane = shownPane("abc")
                onUiThreadGet {
                    val doc = pane.styledDocument
                    val bold = WSimpleAttributeSet().also { WStyleConstants.setBold(it, true) }
                    val negative = runCatching { doc.setCharacterAttributes(-1, 1, bold, false) }.exceptionOrNull()
                    val beyond = runCatching { doc.getCharacterAttributes(4) }.exceptionOrNull()
                    val wrongType = WSimpleAttributeSet().also {
                        WStyleConstants.setBold(it, true)
                        it.addAttribute(WStyleConstants.FontSize, "large")
                    }
                    val typed = runCatching { doc.setCharacterAttributes(0, 3, wrongType, false) }.exceptionOrNull()
                    listOf(
                        negative is IllegalArgumentException,
                        beyond is IllegalArgumentException,
                        typed is IllegalArgumentException,
                        WStyleConstants.isBold(doc.getCharacterAttributes(0)),
                    )
                } shouldBe listOf(true, true, true, false)
            }
        }

        context("Input") {
            test("setCharacterAttributes(attrs, replace) without a selection becomes the formatting of characters typed from now on at the caret position (Swing's input attributes)") {
                val pane = shownPane("ab")
                onUiThread {
                    check(pane.requestFocusInWindow()) { "Cannot move the focus" }
                    pane.caretPosition = 2
                    pane.setCharacterAttributes(WSimpleAttributeSet().also { WStyleConstants.setBold(it, true) }, false)
                }
                TestKeyboard.press(0x41, 0x1E) // A
                eventually(3.seconds) { onUiThreadGet { pane.text } shouldBe "aba" }
                onUiThreadGet {
                    WStyleConstants.isBold(pane.styledDocument.getCharacterAttributes(2)) to
                        WStyleConstants.isBold(pane.styledDocument.getCharacterAttributes(1))
                } shouldBe (true to false)
            }
        }
    }
}
