package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Tests that WLabel's text decorations, font and text color are applied to the TextBlock and can be read back from the native object. */
class WLabelTest : FunSpec() {
    init {
        test("isItalic is false by default and can be read back from the TextBlock once set") {
            val states = onUiThreadGet {
                val label = WLabel("Italic")
                val initial = label.isItalic
                label.isItalic = true
                val italic = label.isItalic
                label.isItalic = false
                listOf(initial, italic, label.isItalic)
            }
            states shouldContainExactly listOf(false, true, false)
        }

        test("underline and strikethrough are separate flags that can be set and cleared together") {
            val states = onUiThreadGet {
                val label = WLabel("Decorated")
                val result = mutableListOf(label.isUnderline to label.isStrikethrough)
                label.isUnderline = true
                result.add(label.isUnderline to label.isStrikethrough)
                label.isStrikethrough = true
                result.add(label.isUnderline to label.isStrikethrough)
                label.isUnderline = false
                result.add(label.isUnderline to label.isStrikethrough)
                result
            }
            states shouldContainExactly listOf(false to false, true to false, true to true, false to true)
        }

        test("the family, weight, italic and size set in font can be read back from the TextBlock and also appear in the individual properties") {
            val (font, parts) = onUiThreadGet {
                val label = WLabel("Font")
                label.font = WFont("Consolas", WFont.BOLD or WFont.ITALIC, 20.0)
                label.font to listOf(label.fontFamily, label.fontSize, label.fontWeight, label.isItalic)
            }
            font shouldBe WFont("Consolas", WFont.BOLD or WFont.ITALIC, 20.0)
            parts shouldContainExactly listOf("Consolas", 20.0, 700, true)
        }

        test("a PLAIN font has weight 400 and no italic") {
            onUiThreadGet {
                val label = WLabel("Plain")
                label.font = WFont("Consolas", WFont.BOLD or WFont.ITALIC, 20.0)
                label.font = WFont("Consolas", WFont.PLAIN, 12.5)
                listOf(label.font, label.fontWeight, label.isItalic)
            } shouldContainExactly listOf(WFont("Consolas", WFont.PLAIN, 12.5), 400, false)
        }

        test("font = null restores the native value before setting (the default)") {
            val (initial, changed, restored) = onUiThreadGet {
                val label = WLabel("Reset")
                val initial = label.font
                label.font = WFont("Consolas", WFont.BOLD, 30.0)
                val changed = label.font
                label.font = null
                Triple(initial, changed, label.font)
            }
            changed shouldNotBe initial
            restored shouldBe initial
        }

        test("fontWeight can be read back from the TextBlock, defaults to 400 (Normal), and font is BOLD at 600 or more") {
            onUiThreadGet {
                val label = WLabel("Weight")
                val initial = label.fontWeight
                label.fontWeight = 600
                listOf(initial, label.fontWeight, label.font!!.isBold)
            } shouldContainExactly listOf(400, 600, true)
        }

        test("values changed individually with fontSize, fontFamily and isItalic also appear in font") {
            onUiThreadGet {
                val label = WLabel("Individual")
                label.fontFamily = "Consolas"
                label.fontSize = 18.0
                label.isItalic = true
                label.font
            } shouldBe WFont("Consolas", WFont.ITALIC, 18.0)
        }

        test("the color set in foreground can be read back from the TextBlock's Foreground, and null restores the value before setting") {
            val (initial, red, restored) = onUiThreadGet {
                val label = WLabel("Color")
                val initial = label.foreground
                label.foreground = WColor.RED
                val red = label.foreground
                label.foreground = null
                Triple(initial, red, label.foreground)
            }
            red shouldBe WColor.RED
            restored shouldBe initial
        }

        test("even after foreground = null, the shown text is drawn in the color inherited from the parent (not transparent)") {
            // When the parent's Foreground is blue, a TextBlock whose local value is cleared inherits the parent's color (setting a null brush would not inherit it)
            val panel = onUiThreadGet {
                val border = WBorder()
                val label = WLabel("Inherit")
                label.foreground = WColor.RED
                label.foreground = null
                border.child = label
                border
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                onUiThreadGet {
                    val label = panel.child as WLabel
                    label.foreground
                } shouldNotBe null
            } finally {
                UiTestHarness.detach(panel)
            }
        }
    }
}
