package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeBetween
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
                val removedListener: (WActionEvent) -> Unit = { removed++ }
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

        test("the ActionListener receives a WActionEvent whose source is the clicked button itself") {
            val (event, button) = onUiThreadGet {
                val button = WButton("Save")
                val events = mutableListOf<WActionEvent>()
                button.addActionListener { e -> events += e }
                button.doClick()
                events.single() to button
            }
            event.source shouldBeSameInstanceAs button
        }

        test("an ActionListener can also be registered with a no-arg Kotlin lambda") {
            onUiThreadGet {
                val button = WButton("Save")
                var count = 0
                button.addActionListener { count++ }
                button.doClick()
                count
            } shouldBe 1
        }

        test("actionCommand returns the button's text when not set, and the set value once set") {
            val (initial, custom, reset) = onUiThreadGet {
                val button = WButton("Save")
                val initial = button.actionCommand
                button.actionCommand = "save-command"
                val custom = button.actionCommand
                button.actionCommand = null
                Triple(initial, custom, button.actionCommand)
            }
            initial shouldBe "Save"
            custom shouldBe "save-command"
            reset shouldBe "Save"
        }

        test("WActionEvent.actionCommand is the button's actionCommand at the time of the click") {
            val (beforeSet, afterSet, afterReset) = onUiThreadGet {
                val button = WButton("Save")
                val commands = mutableListOf<String?>()
                button.addActionListener { e -> commands += e.actionCommand }
                button.doClick()
                button.actionCommand = "save-command"
                button.doClick()
                button.actionCommand = null
                button.text = "Save As"
                button.doClick()
                Triple(commands[0], commands[1], commands[2])
            }
            beforeSet shouldBe "Save"
            afterSet shouldBe "save-command"
            afterReset shouldBe "Save As"
        }

        test("WActionEvent.when is the click time (milliseconds) and modifiers is 0 when nothing is held") {
            val before = System.currentTimeMillis()
            val (time, modifiers) = onUiThreadGet {
                val button = WButton("Save")
                val events = mutableListOf<WActionEvent>()
                button.addActionListener { e -> events += e }
                button.doClick()
                events.single().`when` to events.single().modifiers
            }
            val after = System.currentTimeMillis()
            time.shouldBeBetween(before, after)
            modifiers shouldBe 0
        }

        test("WActionEvent modifier constants have the same values as AWT") {
            WActionEvent.SHIFT_MASK shouldBe 1
            WActionEvent.CTRL_MASK shouldBe 2
            WActionEvent.META_MASK shouldBe 4
            WActionEvent.ALT_MASK shouldBe 8
        }

        test("converts WinUI modifiers (Ctrl=1, Alt=2, Shift=4, Win=8) to AWT modifiers") {
            WActionEvent.fromVirtualKeyModifiers(0) shouldBe 0
            WActionEvent.fromVirtualKeyModifiers(1) shouldBe WActionEvent.CTRL_MASK
            WActionEvent.fromVirtualKeyModifiers(2) shouldBe WActionEvent.ALT_MASK
            WActionEvent.fromVirtualKeyModifiers(4) shouldBe WActionEvent.SHIFT_MASK
            WActionEvent.fromVirtualKeyModifiers(8) shouldBe WActionEvent.META_MASK
            WActionEvent.fromVirtualKeyModifiers(1 or 4) shouldBe (WActionEvent.CTRL_MASK or WActionEvent.SHIFT_MASK)
            WActionEvent.fromVirtualKeyModifiers(15) shouldBe 15
        }

        test("both the Java-facing Runnable and WActionListener listeners are called by doClick") {
            val (runnableCount, listenerSources) = onUiThreadGet {
                val button = WButton("Save")
                var runnableCount = 0
                val sources = mutableListOf<Any>()
                button.addActionListenerForJava(Runnable { runnableCount++ })
                button.addActionListenerForJava(WActionListener { e -> sources += e.source })
                button.doClick()
                runnableCount to (sources.single() === button)
            }
            runnableCount shouldBe 1
            listenerSources.shouldBeTrue()
        }

        test("a Runnable, WActionListener, or Kotlin lambda removed with removeActionListener is not called") {
            val counts = onUiThreadGet {
                val button = WButton("Save")
                val counts = IntArray(3)
                val runnable = Runnable { counts[0]++ }
                val listener = WActionListener { counts[1]++ }
                val lambda: (WActionEvent) -> Unit = { counts[2]++ }
                button.addActionListenerForJava(runnable)
                button.addActionListenerForJava(listener)
                button.addActionListener(lambda)
                button.removeActionListenerForJava(runnable)
                button.removeActionListenerForJava(listener)
                button.removeActionListener(lambda)
                button.doClick()
                counts.toList()
            }
            counts shouldContainExactly listOf(0, 0, 0)
        }

        test("Java sees both the Runnable and the WActionListener versions of addActionListener") {
            val methods = WButtonBase::class.java.methods.filter { it.name == "addActionListener" && !it.isSynthetic }
                .map { it.parameterTypes.single() }
            methods.toSet() shouldBe setOf(Runnable::class.java, WActionListener::class.java)
        }
    }
}
