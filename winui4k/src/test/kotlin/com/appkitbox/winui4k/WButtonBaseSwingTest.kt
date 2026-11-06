package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for the AbstractButton APIs added through the comparison with Swing (AUDIT)
 * (getActionListeners, getItemListeners, doClick(int), focusPainted, hideActionText, getSelectedObjects, JToggleButton(String, boolean))
 * on each button class.
 */
@Tags("E2E")
class WButtonBaseSwingTest : FunSpec() {
    init {
        context("Defaults") {
            test("isFocusPainted of buttons, toggles, check boxes and radio buttons put in a window is true (the default style value)") {
                val buttons = onUiThreadGet { listOf(WButton("a"), WToggleButton("b"), WCheckBox("c"), WRadioButton("d")) }
                buttons.forEach { UiTestHarness.attachAndAwaitLoaded(it) }
                try {
                    onUiThreadGet { buttons.map { it.isFocusPainted } } shouldContainExactly listOf(true, true, true, true)
                } finally {
                    buttons.forEach { UiTestHarness.detach(it) }
                }
            }

            test("a newly created button has empty actionListeners and hideActionText false") {
                val (listeners, hide) = onUiThreadGet {
                    val button = WButton("Save")
                    button.actionListeners.size to button.hideActionText
                }
                listeners shouldBe 0
                hide shouldBe false
            }

            test("getItemListeners of newly created toggles, switches and toggle split buttons are empty") {
                onUiThreadGet {
                    listOf(
                        WToggleButton().getItemListeners(),
                        WToggleSwitch().getItemListeners(),
                        WToggleSplitButton().getItemListeners(),
                    ).map { it.size }
                } shouldContainExactly listOf(0, 0, 0)
            }
        }

        context("Reading and writing") {
            test("isFocusPainted = false can be read back and is not overwritten by the default style after being put in a window") {
                val button = onUiThreadGet { WButton("a").apply { isFocusPainted = false } }
                UiTestHarness.attachAndAwaitLoaded(button)
                try {
                    val (afterLoad, restored) = onUiThreadGet {
                        val afterLoad = button.isFocusPainted
                        button.isFocusPainted = true
                        afterLoad to button.isFocusPainted
                    }
                    afterLoad shouldBe false
                    restored shouldBe true
                } finally {
                    UiTestHarness.detach(button)
                }
            }

            test("selectedObjects is an array of only the text when selected, and null when unselected or indeterminate") {
                val (unselected, selected, indeterminate) = onUiThreadGet {
                    val box = WCheckBox("Notify")
                    box.isThreeState = true
                    val unselected = box.selectedObjects
                    box.isSelected = true
                    val selected = box.selectedObjects?.toList()
                    box.isChecked = null
                    Triple(unselected, selected, box.selectedObjects)
                }
                unselected shouldBe null
                selected shouldBe listOf<Any>("Notify")
                indeterminate shouldBe null
            }

            test("hideActionText = true does not copy the action's NAME to text, and setting it back to false returns to the NAME text") {
                val texts = onUiThreadGet {
                    val action = object : WAbstractAction("Open") {
                        override fun actionPerformed(e: WActionEvent) = Unit
                    }
                    val button = WButton(action)
                    val bound = button.text
                    button.hideActionText = true
                    val hidden = button.text
                    action.putValue(WAction.NAME, "Open file")
                    val renamedWhileHidden = button.text
                    button.hideActionText = false
                    listOf(bound, hidden, renamedWhileHidden, button.text)
                }
                texts shouldContainExactly listOf("Open", "", "", "Open file")
            }

            test("even when hideActionText is set to true before binding an action, text becomes empty") {
                onUiThreadGet {
                    val button = WButton("Original text")
                    button.hideActionText = true
                    button.action = object : WAbstractAction("Open") {
                        override fun actionPerformed(e: WActionEvent) = Unit
                    }
                    button.text
                } shouldBe ""
            }

            test("toggling hideActionText on a button without an action does not change text") {
                onUiThreadGet {
                    val button = WButton("Save")
                    button.hideActionText = true
                    val hidden = button.text
                    button.hideActionText = false
                    hidden to button.text
                } shouldBe ("Save" to "Save")
            }

            test("WToggleSwitch(header, true) is on and (header, false) is off, and the header is set too") {
                onUiThreadGet {
                    val on = WToggleSwitch("Notifications", true)
                    val off = WToggleSwitch("Notifications", false)
                    listOf(on.header, on.isOn, off.header, off.isOn)
                } shouldContainExactly listOf("Notifications", true, "Notifications", false)
            }
        }

        context("Events") {
            test("actionListeners are in the order added, a WActionListener appears as itself, and removing the returned ones stops them being called") {
                val calls = mutableListOf<String>()
                val (sameInstance, sizes) = onUiThreadGet {
                    val button = WButton("Save")
                    val listener = WActionListener { calls += "listener" }
                    button.addActionListener { calls += "lambda" }
                    button.addActionListenerForJava(listener)
                    button.addActionListenerForJava(Runnable { calls += "runnable" })
                    val listeners = button.actionListeners
                    button.doClick()
                    for (l in listeners) button.removeActionListenerForJava(l)
                    button.doClick()
                    (listeners[1] === listener) to listOf(listeners.size, button.actionListeners.size)
                }
                sameInstance shouldBe true
                sizes shouldContainExactly listOf(3, 0)
                calls shouldContainExactly listOf("lambda", "listener", "runnable")
            }

            test("while an action is bound, actionListeners includes one internal listener, which disappears when unbound") {
                onUiThreadGet {
                    val button = WButton("a")
                    button.action = object : WAbstractAction("Open") {
                        override fun actionPerformed(e: WActionEvent) = Unit
                    }
                    val bound = button.actionListeners.size
                    button.action = null
                    bound to button.actionListeners.size
                } shouldBe (1 to 0)
            }

            test("WSplitButton's actionListeners are also in the order added, and the returned ones can be removed") {
                onUiThreadGet {
                    val button = WSplitButton("Split")
                    val listener = WActionListener { }
                    button.addActionListener { }
                    button.addActionListenerForJava(listener)
                    val listeners = button.actionListeners
                    for (l in listeners) button.removeActionListenerForJava(l)
                    Triple(listeners.size, listeners[1] === listener, button.actionListeners.size)
                } shouldBe Triple(2, true, 0)
            }

            test("doClick(pressTime) clicks once, like doClick, and a toggle is toggled") {
                val calls = mutableListOf<String>()
                val states = onUiThreadGet {
                    val button = WButton("Save")
                    button.addActionListener { calls += "button" }
                    val toggle = WToggleButton("Bold")
                    toggle.addActionListener { calls += "toggle" }
                    val toggleSwitch = WToggleSwitch("Notifications")
                    button.doClick(100)
                    toggle.doClick(0)
                    toggleSwitch.doClick(50)
                    listOf(toggle.isSelected, toggleSwitch.isOn)
                }
                calls shouldContainExactly listOf("button", "toggle")
                states shouldContainExactly listOf(true, true)
            }

            test("WSplitButton's doClick(pressTime) clicks the main part once (after being put in a window)") {
                val calls = mutableListOf<String>()
                val button = onUiThreadGet { WSplitButton("Split").apply { addActionListener { calls += "click" } } }
                UiTestHarness.attachAndAwaitLoaded(button)
                try {
                    onUiThread { button.doClick(100) }
                } finally {
                    UiTestHarness.detach(button)
                }
                calls shouldContainExactly listOf("click")
            }

            test("getItemListeners are in the order added, a WItemListener appears as itself, and removing the returned ones stops them being called") {
                val calls = mutableListOf<String>()
                val result = onUiThreadGet {
                    val toggle = WToggleButton("Bold")
                    val listener = WItemListener { calls += "listener" }
                    toggle.addItemListener { _: Boolean? -> calls += "lambda" }
                    toggle.addItemListener(listener)
                    val listeners = toggle.getItemListeners()
                    toggle.isSelected = true
                    for (l in listeners) toggle.removeItemListener(l)
                    toggle.isSelected = false
                    Triple(listeners.size, listeners[1], toggle.getItemListeners().size) to listener
                }
                result.first.first shouldBe 2
                result.first.second shouldBeSameInstanceAs result.second
                result.first.third shouldBe 0
                calls shouldContainExactly listOf("lambda", "listener")
            }

            test("getItemListeners of WToggleSwitch and WToggleSplitButton return the added listeners, and become empty when removed") {
                onUiThreadGet {
                    val toggleSwitch = WToggleSwitch()
                    val split = WToggleSplitButton()
                    val listener = WItemListener { }
                    toggleSwitch.addItemListener(listener)
                    split.addItemListener(listener)
                    val before = listOf(toggleSwitch.getItemListeners().single() === listener, split.getItemListeners().single() === listener)
                    toggleSwitch.removeItemListener(listener)
                    split.removeItemListener(listener)
                    before + listOf(toggleSwitch.getItemListeners().isEmpty(), split.getItemListeners().isEmpty())
                } shouldContainExactly listOf(true, true, true, true)
            }
        }

        context("Boundaries and exceptions") {
            test("doClick(pressTime) on disabled buttons and switches does nothing") {
                val calls = mutableListOf<String>()
                val on = onUiThreadGet {
                    val button = WButton("Save")
                    button.addActionListener { calls += "button" }
                    button.isEnabled = false
                    val toggleSwitch = WToggleSwitch()
                    toggleSwitch.isEnabled = false
                    button.doClick(10)
                    toggleSwitch.doClick(10)
                    toggleSwitch.isOn
                }
                calls.shouldBeEmpty()
                on shouldBe false
            }

            test("removing listeners never added or already removed does nothing") {
                onUiThreadGet {
                    val button = WButton("Save")
                    button.addActionListener { }
                    val listener = button.actionListeners.single()
                    button.removeActionListenerForJava(listener)
                    button.removeActionListenerForJava(listener)
                    button.removeActionListenerForJava(WActionListener { })
                    button.actionListeners.size
                } shouldBe 0
            }
        }
    }
}
