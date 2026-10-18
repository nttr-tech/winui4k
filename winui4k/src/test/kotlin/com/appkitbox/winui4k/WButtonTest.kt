package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.function.Consumer

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

        test("doClick calls the Click listener once per call") {
            onUiThreadGet {
                val button = WButton("Press")
                var count = 0
                button.addActionListener { count++ }
                button.doClick()
                val afterFirst = count
                button.doClick()
                afterFirst to count
            } shouldBe (1 to 2)
        }

        test("doClick does not call unregistered listeners and calls only the remaining ones") {
            val (removed, kept) = onUiThreadGet {
                val button = WButton("Press")
                var removed = 0
                var kept = 0
                val removedListener: () -> Unit = { removed++ }
                button.addActionListener(removedListener)
                button.addActionListener { kept++ }
                button.removeActionListener(removedListener)
                button.doClick()
                removed to kept
            }
            removed shouldBe 0
            kept shouldBe 1
        }

        test("doClick on a disabled button throws no exception and does nothing") {
            val (count, enabledAfter) = onUiThreadGet {
                val button = WButton("Press")
                var count = 0
                button.addActionListener { count++ }
                button.isEnabled = false
                button.doClick()
                button.isEnabled = true
                button.doClick()
                count to button.isEnabled
            }
            // The one click while disabled is not counted; only the one after re-enabling is
            count shouldBe 1
            enabledAfter.shouldBeTrue()
        }

        test("doClick executes command with commandParameter") {
            val received = onUiThreadGet {
                val received = mutableListOf<String?>()
                val button = WButton("Run")
                button.command = WCommand(true, Consumer { received += it })
                button.commandParameter = "Argument"
                button.doClick()
                button.doClick()
                received
            }
            received shouldContainExactly listOf("Argument", "Argument")
        }

        test("doClick on a button whose command has CanExecute false does not execute the command") {
            val executed = onUiThreadGet {
                var executed = 0
                val button = WButton("Run")
                button.command = WCommand(false, Consumer { executed++ })
                button.doClick()
                executed
            }
            executed shouldBe 0
        }

        test("doClick runs both the Click listener and the command") {
            val order = onUiThreadGet {
                val order = mutableListOf<String>()
                val button = WButton("Run")
                button.command = WCommand(true, Consumer { order += "command" })
                button.addActionListener { order += "click" }
                button.doClick()
                order
            }
            // ButtonBase.OnClick raises Click first and then calls ExecuteCommand
            order shouldContainExactly listOf("click", "command")
        }
    }
}
