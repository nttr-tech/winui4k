package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

/**
 * Tests that check, per component, that a [WActionListener] (and a Runnable) can be passed from Kotlin with the same names as Java, `addActionListener` / `removeActionListener`
 * (the Swing form `ActionListener l = ...; a.addActionListener(l); b.addActionListener(l);`).
 * WSplitButton needs a visual tree for doClick, so it is checked in WSplitButtonTest.
 */
@Tags("E2E")
class WActionListenerKotlinTest : FunSpec() {
    init {
        test("one WActionListener can be shared by adding it to several components with addActionListener and removed with removeActionListener") {
            onUiThreadGet {
                val calls = mutableListOf<String>()
                val listener = WActionListener { e -> calls.add((e.source as WComponent).name) }
                val button = WButton("Save").also { it.name = "button" }
                val toggle = WToggleSwitch().also { it.name = "toggle" }
                val field = WTextField().also { it.name = "field" }
                val password = WPasswordField().also { it.name = "password" }
                val suggest = WAutoSuggestBox().also { it.name = "suggest" }
                val combo = WComboBox().also { it.name = "combo" }
                button.addActionListener(listener)
                toggle.addActionListener(listener)
                field.addActionListener(listener)
                password.addActionListener(listener)
                suggest.addActionListener(listener)
                combo.addActionListener(listener)
                button.doClick()
                toggle.doClick()
                field.postActionEvent()
                password.postActionEvent()
                suggest.postActionEvent()
                val added = calls.toList()

                calls.clear()
                button.removeActionListener(listener)
                toggle.removeActionListener(listener)
                field.removeActionListener(listener)
                password.removeActionListener(listener)
                suggest.removeActionListener(listener)
                combo.removeActionListener(listener)
                button.doClick()
                toggle.doClick()
                field.postActionEvent()
                password.postActionEvent()
                suggest.postActionEvent()
                added + listOf("--") + calls
            } shouldContainExactly listOf("button", "toggle", "field", "password", "suggest", "--")
        }

        test("a WActionListener can also be added to and removed from a menu item with addActionListener") {
            onUiThreadGet {
                val calls = mutableListOf<String>()
                val listener = WActionListener { e -> calls.add(e.actionCommand.orEmpty()) }
                val item = WMenuFlyoutItem("Open")
                item.addActionListener(listener)
                item.doClick()
                item.removeActionListener(listener)
                item.doClick()
                calls.toList()
            } shouldContainExactly listOf("Open")
        }

        test("values of Runnable and () -> Unit can also be added with addActionListener, and a lambda still takes the form that receives a WActionEvent") {
            onUiThreadGet {
                val calls = mutableListOf<String>()
                val runnable = Runnable { calls.add("runnable") }
                val button = WButton("Run")
                button.addActionListener(runnable)
                button.addActionListener { e -> calls.add("lambda:" + e.actionCommand) }
                val withoutEvent: () -> Unit = { calls.add("function") }
                button.addActionListener(withoutEvent)
                button.doClick()
                button.removeActionListener(runnable)
                button.doClick()
                calls.toList()
            } shouldContainExactly listOf("runnable", "lambda:Run", "function", "lambda:Run", "function")
        }
    }
}
