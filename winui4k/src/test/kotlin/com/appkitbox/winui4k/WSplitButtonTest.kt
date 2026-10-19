package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.function.Consumer

/**
 * Tests that verify that WSplitButton can be used with the same operations as JButton (text / content / icon /
 * actionCommand / mnemonic / doClick / command / ActionListener). Uses the same expected values that WButtonTest and
 * WButtonIconTest check for WButton.
 *
 * SplitButton does not have its real template (the main Button) until it is placed in a window, and until then doClick
 * cannot raise Click (see [WSplitButton.doClick]). Tests that check doClick are done after placing it in the shared
 * window with [shown].
 * The form seen from Java is in WSplitButtonForJavaTest.
 */
class WSplitButtonTest : FunSpec() {
    /**
     * Places the WSplitButton created by [create] (run on the UI thread) in the window (waiting for Loaded), runs [act] on
     * the UI thread, returns the result and removes it.
     */
    private fun <T> shown(create: () -> WSplitButton = { WSplitButton("Paste") }, act: (WSplitButton) -> T): T {
        val button = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(button)
        try {
            return onUiThreadGet { act(button) }
        } finally {
            UiTestHarness.detach(button)
        }
    }

    init {
        test("the label passed to the constructor can be retrieved via text, and is an empty string when unset") {
            val (labeled, empty) = onUiThreadGet { WSplitButton("Format").text to WSplitButton().text }
            labeled shouldBe "Format"
            empty shouldBe ""
        }

        test("setting a component to content makes text an empty string, and setting text removes content") {
            val (content, label, textWithContent, contentAfterText) = onUiThreadGet {
                val button = WSplitButton("Text label")
                val label = WLabel("Content")
                button.content = label
                val withContent = button.content to button.text
                button.text = "Reset"
                listOf(withContent.first, label, withContent.second, button.content)
            }
            content shouldBeSameInstanceAs label
            textWithContent shouldBe ""
            contentAfterText shouldBe null
        }

        test("doClick calls the ActionListener once per call") {
            shown { button ->
                var count = 0
                button.addActionListener { count++ }
                button.doClick()
                val afterFirst = count
                button.doClick()
                afterFirst to count
            } shouldBe (1 to 2)
        }

        test("doClick right after placing it in the window (without waiting for Loaded) also calls the ActionListener") {
            val button = onUiThreadGet { WSplitButton("Paste") }
            var count = 0
            try {
                UiTestHarness.attach(button)
                onUiThread {
                    button.addActionListener { count++ }
                    button.doClick()
                }
            } finally {
                UiTestHarness.detach(button)
            }
            count shouldBe 1
        }

        test("doClick before placing it in a window throws no exception and calls neither the ActionListener nor command") {
            val (clicked, executed) = onUiThreadGet {
                var clicked = 0
                var executed = 0
                val button = WSplitButton("Paste")
                button.command = WCommand(true, Consumer { executed++ })
                button.addActionListener { clicked++ }
                button.doClick()
                clicked to executed
            }
            clicked shouldBe 0
            executed shouldBe 0
        }

        test("the ActionListener receives a WActionEvent whose source is the clicked WSplitButton itself and whose actionCommand is text") {
            val events = mutableListOf<WActionEvent>()
            val button = onUiThreadGet { WSplitButton("Save").also { b -> b.addActionListener { e -> events += e } } }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread { button.doClick() }
            } finally {
                UiTestHarness.detach(button)
            }
            events.single().source shouldBeSameInstanceAs button
            events.single().actionCommand shouldBe "Save"
        }

        test("the Java forms WActionListener and Runnable are also called once each by doClick, and removed listeners are not called") {
            val (events, runs, removed) = shown { button ->
                val events = mutableListOf<String?>()
                var runs = 0
                var removed = 0
                button.addActionListenerForJava(WActionListener { e -> events += e.actionCommand })
                button.addActionListenerForJava(Runnable { runs++ })
                val removable = Runnable { removed++ }
                button.addActionListenerForJava(removable)
                button.removeActionListenerForJava(removable)
                button.doClick()
                Triple(events, runs, removed)
            }
            events shouldContainExactly listOf("Paste")
            runs shouldBe 1
            removed shouldBe 0
        }

        test("a Kotlin listener removed with removeActionListener is not called, and only the remaining listeners are called") {
            val (removed, kept) = shown { button ->
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

        test("doClick on a disabled WSplitButton throws no exception and does nothing, and after re-enabling it is called only once") {
            shown { button ->
                var count = 0
                button.addActionListener { count++ }
                button.isEnabled = false
                button.doClick()
                button.isEnabled = true
                button.doClick()
                count
            } shouldBe 1
        }

        test("doClick does not press the right-hand arrow, so it does not open the flyout") {
            shown({ WSplitButton("Paste").also { it.flyout = WMenuFlyout() } }) { button ->
                var count = 0
                button.addActionListener { count++ }
                button.doClick()
                count
            } shouldBe 1
        }

        test("doClick runs command with commandParameter, calling the ActionListener first") {
            val order = shown({ WSplitButton("Run") }) { button ->
                val order = mutableListOf<String?>()
                button.command = WCommand(true, Consumer { order += "command:$it" })
                button.commandParameter = "Argument"
                button.addActionListener { order += "click" }
                button.doClick()
                button.doClick()
                order
            }
            // ButtonBase.OnClick fires Click first and then calls ExecuteCommand (the main Button runs the command)
            order shouldContainExactly listOf("click", "command:Argument", "click", "command:Argument")
        }

        test("doClick on a WSplitButton with a command that cannot execute (CanExecute is false) calls neither command nor the ActionListener") {
            val (executed, clicked) = shown({ WSplitButton("Run") }) { button ->
                var executed = 0
                var clicked = 0
                button.command = WCommand(false, Consumer { executed++ })
                button.addActionListener { clicked++ }
                button.doClick()
                executed to clicked
            }
            executed shouldBe 0
            clicked shouldBe 0
        }

        test("resetting commandParameter to null passes null to command, and doClick after resetting command to null does not run command") {
            val (received, state) = shown({ WSplitButton("Run") }) { button ->
                val received = mutableListOf<String?>()
                button.command = WCommand(true, Consumer { received += it })
                button.commandParameter = "Argument"
                button.commandParameter = null
                button.doClick()
                button.command = null
                button.doClick()
                received to (button.command to button.commandParameter)
            }
            received shouldContainExactly listOf(null) // runs only the first time, with a null argument
            state shouldBe (null to null)
        }

        test("actionCommand returns text when not set, the set value when set, and returns to unset with null") {
            val (initial, custom, reset) = onUiThreadGet {
                val button = WSplitButton("Save")
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

        test("WActionEvent.actionCommand is the actionCommand at the time of the click") {
            shown({ WSplitButton("Save") }) { button ->
                val commands = mutableListOf<String?>()
                button.addActionListener { e -> commands += e.actionCommand }
                button.doClick()
                button.actionCommand = "save-command"
                button.doClick()
                button.actionCommand = null
                button.text = "Save As"
                button.doClick()
                commands
            } shouldContainExactly listOf("Save", "save-command", "Save As")
        }

        test("mnemonic is 0 when unset, setMnemonic(Char) stores letters as the uppercase key code, and the NUL character and 0 clear it") {
            val codes = onUiThreadGet {
                val button = WSplitButton("Paste(P)")
                val initial = button.mnemonic
                button.setMnemonic('p')
                val lower = button.mnemonic
                button.mnemonic = 55
                val digit = button.mnemonic
                button.mnemonic = 0
                val cleared = button.mnemonic
                button.setMnemonic('P')
                button.setMnemonic('\u0000')
                listOf(initial, lower, digit, cleared, button.mnemonic)
            }
            codes shouldBe listOf(0, 80, 55, 0, 0)
        }

        test("passing a key code other than a letter or digit to mnemonic throws IllegalArgumentException and leaves the value unchanged") {
            onUiThreadGet {
                val button = WSplitButton("Run")
                button.mnemonic = 69
                listOf(32, 47, 58, 64, 91, 97, 122, -1).forEach { invalid ->
                    shouldThrow<IllegalArgumentException> { button.mnemonic = invalid }
                }
                shouldThrow<IllegalArgumentException> { button.setMnemonic('!') }
                button.mnemonic
            } shouldBe 69
        }

        test("setting icon makes Content a horizontal [icon, text] layout with text unchanged, and null returns to a string only") {
            val (withIcon, text, icon, cleared) = onUiThreadGet {
                val button = WSplitButton("Save")
                button.icon = Symbol.SAVE
                val withIcon = ButtonContents.contentOf(button)
                val text = button.text
                val icon = button.icon
                button.icon = null
                listOf(withIcon, text, icon, ButtonContents.contentOf(button))
            }
            withIcon shouldBe listOf("Panel:HORIZONTAL:4.0", "Symbol:SAVE", "TextBlock:Save")
            text shouldBe "Save"
            icon shouldBe Symbol.SAVE
            cleared shouldBe listOf("String:Save")
        }

        test("a WSplitButton with only an icon has only an IconElement as Content and an empty string as text") {
            val (content, text) = onUiThreadGet {
                val button = WSplitButton()
                button.icon = Symbol.DELETE
                ButtonContents.contentOf(button) to button.text
            }
            content shouldBe listOf("Symbol:DELETE")
            text shouldBe ""
        }

        test("the text position and spacing (horizontalTextPosition, verticalTextPosition, iconTextGap) affect the order, orientation and spacing of the layout") {
            val (above, leading, gap) = onUiThreadGet {
                val button = WSplitButton("Save")
                button.icon = Symbol.SAVE
                button.horizontalTextPosition = HorizontalTextPosition.CENTER
                button.verticalTextPosition = VerticalTextPosition.TOP
                val above = ButtonContents.contentOf(button)
                button.horizontalTextPosition = HorizontalTextPosition.LEADING
                button.verticalTextPosition = VerticalTextPosition.CENTER
                val leading = ButtonContents.contentOf(button)
                button.iconTextGap = 10.0
                Triple(above, leading, ButtonContents.contentOf(button).first())
            }
            above shouldBe listOf("Panel:VERTICAL:4.0", "TextBlock:Save", "Symbol:SAVE") // text above the icon
            leading shouldBe listOf("Panel:HORIZONTAL:4.0", "TextBlock:Save", "Symbol:SAVE") // text before the icon
            gap shouldBe "Panel:HORIZONTAL:10.0"
        }

        test("setting a negative value, NaN or infinity to iconTextGap throws IllegalArgumentException and leaves the value unchanged") {
            onUiThreadGet {
                val button = WSplitButton("Save")
                listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { invalid ->
                    shouldThrow<IllegalArgumentException> { button.iconTextGap = invalid }
                }
                button.iconTextGap
            } shouldBe 4.0
        }

        test("putting a component in content removes the icon, and setting icon removes content") {
            val (iconAfterContent, contentAfterIcon) = onUiThreadGet {
                val button = WSplitButton("Save")
                button.icon = Symbol.SAVE
                button.content = WLabel("Content")
                val iconAfterContent = button.icon
                button.icon = Symbol.OPEN_FILE
                iconAfterContent to button.content
            }
            iconAfterContent shouldBe null
            contentAfterIcon shouldBe null
        }

        test("while the icon and text are laid out together, accessibleName holds the text, and removing the icon restores it") {
            val (withIcon, cleared) = onUiThreadGet {
                val button = WSplitButton("Save")
                button.icon = Symbol.SAVE
                val withIcon = button.accessibleName
                button.icon = null
                withIcon to button.accessibleName
            }
            withIcon shouldBe "Save"
            cleared shouldBe ""
        }

        test("doClick on a WSplitButton with an icon also calls the ActionListener once") {
            shown({ WSplitButton("Save").also { it.icon = Symbol.SAVE } }) { button ->
                var count = 0
                button.addActionListener { count++ }
                button.doClick()
                count
            } shouldBe 1
        }
    }
}
