package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests that verify WTextField.horizontalAlignment (the alignment of the text in the field, the same as JTextField.setHorizontalAlignment).
 * They check the value goes into WinUI's TextBox.TextAlignment and is separate from the field's position in its parent (horizontalAlignmentInParent).
 */
@Tags("E2E")
class WTextFieldAlignmentTest : FunSpec() {
    /** The TextAlignment in the TextBox of [field] (the native value). */
    private fun nativeTextAlignment(field: WTextField): TextAlignment =
        TextAlignment.of(field.inspectable.getInt(XamlInterop.ITextBox_get_TextAlignment))

    init {
        test("the default horizontalAlignment is LEFT (corresponding to JTextField's LEADING)") {
            onUiThreadGet { WTextField("123").horizontalAlignment } shouldBe HorizontalAlignment.LEFT
        }

        test("LEFT, CENTER and RIGHT of horizontalAlignment become the same direction of the TextBox's TextAlignment and can be read back") {
            onUiThreadGet {
                val field = WTextField("123")
                HorizontalAlignment.entries.filter { it != HorizontalAlignment.STRETCH }.map { alignment ->
                    field.horizontalAlignment = alignment
                    Triple(alignment, nativeTextAlignment(field), field.horizontalAlignment)
                }
            } shouldBe listOf(
                Triple(HorizontalAlignment.LEFT, TextAlignment.LEFT, HorizontalAlignment.LEFT),
                Triple(HorizontalAlignment.CENTER, TextAlignment.CENTER, HorizontalAlignment.CENTER),
                Triple(HorizontalAlignment.RIGHT, TextAlignment.RIGHT, HorizontalAlignment.RIGHT),
            )
        }

        test("values set with textAlignment can also be read with horizontalAlignment (JUSTIFY and DETECT_FROM_CONTENT are LEFT)") {
            onUiThreadGet {
                val field = WTextField("123")
                listOf(TextAlignment.RIGHT, TextAlignment.CENTER, TextAlignment.JUSTIFY, TextAlignment.DETECT_FROM_CONTENT).map {
                    field.textAlignment = it
                    field.horizontalAlignment
                }
            } shouldBe listOf(HorizontalAlignment.RIGHT, HorizontalAlignment.CENTER, HorizontalAlignment.LEFT, HorizontalAlignment.LEFT)
        }

        test("horizontalAlignment does not change the field's position in its parent (horizontalAlignmentInParent), and vice versa") {
            onUiThreadGet {
                val field = WTextField("123")
                field.horizontalAlignment = HorizontalAlignment.RIGHT
                val inParent = field.horizontalAlignmentInParent
                field.horizontalAlignmentInParent = HorizontalAlignment.CENTER
                Triple(inParent, field.horizontalAlignment, nativeTextAlignment(field))
            } shouldBe Triple(HorizontalAlignment.STRETCH, HorizontalAlignment.RIGHT, TextAlignment.RIGHT)
        }

        test("in a shown field too, setting horizontalAlignment to RIGHT aligns the TextBox's text to the right") {
            val field = onUiThreadGet {
                WTextField("123").also {
                    it.width = 200.0
                    it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
                }
            }
            UiTestHarness.attachAndAwaitLoaded(field)
            try {
                onUiThreadGet {
                    field.horizontalAlignment = HorizontalAlignment.RIGHT
                    field.updateLayout()
                    nativeTextAlignment(field) to field.actualWidth
                } shouldBe (TextAlignment.RIGHT to 200.0)
            } finally {
                UiTestHarness.detach(field)
            }
        }

        test("STRETCH cannot be used for text alignment and throws IllegalArgumentException, and the value does not change") {
            val (error, value) = onUiThreadGet {
                val field = WTextField("123")
                field.horizontalAlignment = HorizontalAlignment.CENTER
                runCatching { field.horizontalAlignment = HorizontalAlignment.STRETCH }.exceptionOrNull() to field.horizontalAlignment
            }
            error.shouldBeInstanceOf<IllegalArgumentException>()
            value shouldBe HorizontalAlignment.CENTER
        }
    }
}
