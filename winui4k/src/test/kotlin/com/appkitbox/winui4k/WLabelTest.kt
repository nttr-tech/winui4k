package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

/** Tests that WLabel's italic, underline, and strikethrough are reflected in the TextBlock's FontStyle / TextDecorations. */
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
    }
}
