package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Tests that WLabel's text decorations, font and text color are applied to the TextBlock and can be read back from the native object. */
class WLabelTest : FunSpec() {
    /** The DesiredSize of [element] ([width, height]). */
    private fun desiredSizeOf(element: ComPtr): List<Double> {
        val ui = element.queryInterface(XamlInterop.IID_IUIElement)
        return try {
            XamlStructs.getSizeFloat(ui, XamlInterop.IUIElement_get_DesiredSize).toList()
        } finally {
            ui.release()
        }
    }

    /** The left edge of the first character of the TextBlock [element] (TextPointer.GetCharacterRect). */
    private fun firstCharacterX(element: ComPtr): Double {
        val textBlock = element.queryInterface(XamlInterop.IID_ITextBlock)
        val start = try {
            textBlock.getPtr(XamlInterop.ITextBlock_get_ContentStart)
        } finally {
            textBlock.release()
        }
        return try {
            Ffi.backend.withScope { scope ->
                val rect = scope.allocate(RECT_BYTES, FLOAT_ALIGNMENT) // Windows.Foundation.Rect { float X, Y, Width, Height }
                start.call(XamlInterop.ITextPointer_GetCharacterRect, XamlInterop.LogicalDirection_Forward, rect)
                Ffi.backend.memory.getFloat(rect, 0).toDouble()
            }
        } finally {
            start.release()
        }
    }

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

        test("the desired size of a label without an icon is the same as that of a TextBlock alone with the same text") {
            val panel = onUiThreadGet {
                val panel = WPanel()
                panel.add(WLabel("Save now").also { it.horizontalAlignment = HorizontalAlignment.LEFT })
                panel.add(XamlElement.load("""<TextBlock Text="Save now" HorizontalAlignment="Left" />"""))
                panel
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                val (label, textBlock) = onUiThreadGet {
                    panel.updateLayout()
                    val label = checkNotNull(Xaml.childAt(panel.uiElement, 0))
                    val textBlock = checkNotNull(Xaml.childAt(panel.uiElement, 1))
                    try {
                        listOf(label, textBlock).map { desiredSizeOf(it) }
                    } finally {
                        label.release()
                        textBlock.release()
                    }
                }.let { it[0] to it[1] }
                label shouldBe textBlock
                label[0] shouldBeGreaterThan 0.0
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("a label without enough width trims its text with textTrimming, and does not trim once widened") {
            val label = onUiThreadGet {
                WLabel("a very long label string").also {
                    it.textTrimming = TextTrimming.CHARACTER_ELLIPSIS
                    it.width = 40.0
                    it.horizontalAlignment = HorizontalAlignment.LEFT
                }
            }
            UiTestHarness.attachAndAwaitLoaded(label)
            try {
                val narrow = onUiThreadGet {
                    label.updateLayout()
                    label.textBlock.getBool(XamlInterop.ITextBlock_get_IsTextTrimmed)
                }
                val wide = onUiThreadGet {
                    label.width = 1000.0
                    label.updateLayout()
                    label.textBlock.getBool(XamlInterop.ITextBlock_get_IsTextTrimmed)
                }
                (narrow to wide) shouldBe (true to false)
            } finally {
                UiTestHarness.detach(label)
            }
        }

        test("textAlignment aligns the text within the widened label, and the text position is the same as a TextBlock alone with the same width") {
            // A TextBlock's ActualWidth is the text width even with Width set to 300 (measured in WinUI), so compare the position of the first character
            val panel = onUiThreadGet {
                val panel = WPanel()
                panel.add(
                    WLabel("Right").also {
                        it.width = 300.0
                        it.horizontalAlignment = HorizontalAlignment.LEFT
                        it.textAlignment = TextAlignment.RIGHT
                    },
                )
                panel.add(XamlElement.load("""<TextBlock Width="300" HorizontalAlignment="Left" TextAlignment="Right" Text="Right" />"""))
                panel
            }
            UiTestHarness.attachAndAwaitLoaded(panel)
            try {
                val (labelX, textBlockX) = onUiThreadGet {
                    panel.updateLayout()
                    val label = checkNotNull(Xaml.childAt(panel.uiElement, 0))
                    val textBlock = checkNotNull(Xaml.childAt(panel.uiElement, 1))
                    val inner = checkNotNull(Xaml.childAt(label, 0))
                    try {
                        firstCharacterX(inner) to firstCharacterX(textBlock)
                    } finally {
                        listOf(label, textBlock, inner).forEach { it.release() }
                    }
                }
                labelX shouldBe textBlockX
                labelX shouldBeGreaterThan 250.0
            } finally {
                UiTestHarness.detach(panel)
            }
        }

        test("accessibleName and accessibleDescription go into the TextBlock that is read aloud") {
            onUiThreadGet {
                val label = WLabel("Full name")
                label.accessibleName = "Full name label"
                label.accessibleDescription = "Description"
                listOf(
                    label.accessibleName,
                    Xaml.getAutomation(label.textBlock, XamlInterop.IAutomationPropertiesStatics_GetName),
                    Xaml.getAutomation(label.textBlock, XamlInterop.IAutomationPropertiesStatics_GetHelpText),
                )
            } shouldContainExactly listOf("Full name label", "Full name label", "Description")
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

        test("the root Grid has a transparent brush as its Background (without a Background, mouse events are not received in places such as the gap between the icon and the text)") {
            onUiThreadGet {
                val withIcon = WLabel("Text", Symbol.SAVE).also { it.iconTextGap = 24.0 }
                listOf(WLabel("Text"), withIcon, WLabel(Symbol.SAVE), WLabel()).map { rootBackgroundAlpha(it) }
            } shouldContainExactly listOf(0, 0, 0, 0)
        }
    }

    /** The alpha of the root Grid's Background (0 is transparent and hit-testable). -1 if there is no Background (null). */
    private fun rootBackgroundAlpha(label: WLabel): Int {
        val panel = label.inspectable.queryInterface(XamlInterop.IID_IPanel)
        val brush = try {
            panel.getPtrOrNull(XamlInterop.IPanel_get_Background)
        } finally {
            panel.release()
        } ?: return -1
        val solid = try {
            brush.queryInterface(XamlInterop.IID_ISolidColorBrush)
        } finally {
            brush.release()
        }
        return try {
            XamlStructs.getColor(solid, XamlInterop.ISolidColorBrush_get_Color)[0]
        } finally {
            solid.release()
        }
    }

    private companion object {
        const val RECT_BYTES = 16L
        const val FLOAT_ALIGNMENT = 4L
    }
}
