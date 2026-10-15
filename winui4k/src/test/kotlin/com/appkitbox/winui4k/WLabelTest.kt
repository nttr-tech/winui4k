package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Tests that WLabel's properties (font style, decorations, and foreground) are reflected in the TextBlock. */
class WLabelTest : FunSpec() {
    /** The color (A, R, G, B) of the TextBlock's Foreground brush, or null if it has no brush. */
    private fun WLabel.foregroundBrushColor(): List<Int>? {
        val brush = inspectable.getPtrOrNull(XamlInterop.ITextBlock_get_Foreground) ?: return null
        val solid: ComPtr = brush.queryInterface(XamlInterop.IID_ISolidColorBrush)
        return try {
            XamlStructs.getColor(solid, XamlInterop.ISolidColorBrush_get_Color).toList()
        } finally {
            solid.release()
            brush.release()
        }
    }

    init {
        test("setting foreground to null restores the default (theme) text color instead of removing the brush") {
            val label = onUiThreadGet { WLabel("Colored") }
            val plain = onUiThreadGet { WLabel("Plain") }
            UiTestHarness.attachAndAwaitLoaded(label)
            UiTestHarness.attachAndAwaitLoaded(plain)
            try {
                val (red, restored, default) = onUiThreadGet {
                    label.foreground = WColor.RED
                    val red = label.foregroundBrushColor()
                    label.foreground = null
                    Triple(red, label.foregroundBrushColor(), plain.foregroundBrushColor())
                }
                red shouldBe listOf(255, 237, 28, 36)
                default shouldNotBe null
                restored shouldBe default
                onUiThreadGet { label.foreground } shouldBe null
            } finally {
                UiTestHarness.detach(label)
                UiTestHarness.detach(plain)
            }
        }

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
    }
}
