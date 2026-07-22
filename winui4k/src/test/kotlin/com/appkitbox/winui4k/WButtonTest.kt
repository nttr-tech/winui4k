package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/** Tests verifying WButton's key properties (including behavior inherited from WButtonBase / WControl). */
class WButtonTest : FunSpec() {
    init {
        test("the label passed to the constructor can be retrieved via text") {
            onUiThreadGet { WButton("Run").text } shouldBe "Run"
        }

        test("re-setting text takes effect, and it returns an empty string when unset") {
            val (initial, updated) = onUiThreadGet {
                val button = WButton()
                val initial = button.text
                button.text = "Updated"
                initial to button.text
            }
            initial shouldBe ""
            updated shouldBe "Updated"
        }

        test("setting content to a component clears text to an empty string") {
            val (content, label, text) = onUiThreadGet {
                val button = WButton("Text label")
                val label = WLabel("Content")
                button.content = label
                Triple(button.content, label, button.text)
            }
            content shouldBeSameInstanceAs label
            text shouldBe ""
        }

        test("clickMode returns exactly the value that was set") {
            onUiThreadGet {
                val button = WButton()
                button.clickMode = ClickMode.PRESS
                button.clickMode
            } shouldBe ClickMode.PRESS
        }

        test("isEnabled = false disables it, and true re-enables it") {
            val (disabled, enabled) = onUiThreadGet {
                val button = WButton()
                button.isEnabled = false
                val disabled = button.isEnabled
                button.isEnabled = true
                disabled to button.isEnabled
            }
            disabled.shouldBeFalse()
            enabled.shouldBeTrue()
        }

        test("isPressed and isPointerOver are false with no input interaction") {
            val (pressed, pointerOver) = onUiThreadGet {
                val button = WButton()
                button.isPressed to button.isPointerOver
            }
            pressed.shouldBeFalse()
            pointerOver.shouldBeFalse()
        }
    }
}
