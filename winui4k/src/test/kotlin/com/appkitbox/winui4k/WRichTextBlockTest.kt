package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Tests that the font of WRichTextBlock (RichTextBlock) reaches the native object and can be read back. */
class WRichTextBlockTest : FunSpec() {
    init {
        test("the family, weight, italic and size set in font can be read back from the RichTextBlock") {
            onUiThreadGet {
                val block = WRichTextBlock()
                block.font = WFont("Consolas", WFont.BOLD or WFont.ITALIC, 22.0)
                block.font
            } shouldBe WFont("Consolas", WFont.BOLD or WFont.ITALIC, 22.0)
        }

        test("font = null restores the native value before setting (the default)") {
            val (initial, changed, restored) = onUiThreadGet {
                val block = WRichTextBlock()
                val initial = block.font
                block.font = WFont("Consolas", WFont.PLAIN, 30.0)
                val changed = block.font
                block.font = null
                Triple(initial, changed, block.font)
            }
            changed shouldNotBe initial
            restored shouldBe initial
        }
    }
}
