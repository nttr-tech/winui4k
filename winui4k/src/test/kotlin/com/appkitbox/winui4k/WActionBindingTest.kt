package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for setAction / getAction other than buttons and menu items (WSplitButton, WToggleSplitButton, WComboBox, and the menu headings
 * WMenuBarItem and WMenuFlyoutSubItem). The copied items are the same as configurePropertiesFromAction of Swing's JButton / JComboBox / JMenu.
 */
@Tags("E2E")
class WActionBindingTest : FunSpec() {
    /** An action that records the events it is called with. */
    private class RecordingAction(name: String? = null, icon: WIcon? = null) : WAbstractAction(name, icon) {
        val events = mutableListOf<WActionEvent>()

        /** The SELECTED_KEY value at the time of the call. */
        val selectedWhenPerformed = mutableListOf<Any?>()

        override fun actionPerformed(e: WActionEvent) {
            events += e
            selectedWhenPerformed += getValue(WAction.SELECTED_KEY)
        }
    }

    private fun fullAction(): RecordingAction = RecordingAction("Save", Symbol.SAVE).apply {
        putValue(WAction.SHORT_DESCRIPTION, "Save the file")
        putValue(WAction.MNEMONIC_KEY, 'S'.code)
        putValue(WAction.ACTION_COMMAND_KEY, "save")
    }

    private fun <T : WComponent> loaded(create: () -> T): T {
        val component = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(component)
        return component
    }

    init {
        context("Defaults") {
            test("action is null by default for every component") {
                onUiThreadGet {
                    listOf(WSplitButton().action, WToggleSplitButton().action, WComboBox().action, WMenuBarItem().action, WMenuFlyoutSubItem().action)
                } shouldContainExactly listOf(null, null, null, null, null)
            }
        }

        context("Reading and writing") {
            test("WSplitButton's action copies NAME, SMALL_ICON, SHORT_DESCRIPTION, MNEMONIC_KEY, ACTION_COMMAND_KEY and enabled") {
                onUiThreadGet {
                    val action = fullAction().apply { isEnabled = false }
                    val button = WSplitButton()
                    button.action = action
                    listOf(button.action === action, button.text, button.icon, button.toolTipText, button.mnemonic, button.actionCommand, button.isEnabled)
                } shouldContainExactly listOf(true, "Save", Symbol.SAVE, "Save the file", 'S'.code, "save", false)
            }

            test("changing the action's values is followed by WSplitButton, and setting null returns to the no-value state") {
                onUiThreadGet {
                    val action = fullAction()
                    val button = WSplitButton().apply { this.action = action }
                    action.putValue(WAction.NAME, "Overwrite")
                    action.isEnabled = false
                    val followed = listOf(button.text, button.isEnabled)
                    button.action = null
                    action.putValue(WAction.NAME, "After unbinding")
                    followed + listOf(button.text, button.icon, button.toolTipText, button.mnemonic, button.isEnabled)
                } shouldContainExactly listOf("Overwrite", false, "", null, null, 0, true)
            }

            test("WToggleSplitButton copies SELECTED_KEY to isChecked, and unchecks if there is no value") {
                onUiThreadGet {
                    val action = RecordingAction("Bold").apply { putValue(WAction.SELECTED_KEY, true) }
                    val button = WToggleSplitButton()
                    button.action = action
                    val checked = button.isChecked
                    action.putValue(WAction.SELECTED_KEY, false)
                    val unchecked = button.isChecked
                    action.putValue(WAction.SELECTED_KEY, true)
                    button.action = RecordingAction("Other")
                    listOf(checked, unchecked, button.isChecked)
                } shouldContainExactly listOf(true, false, false)
            }

            test("WComboBox's action copies only enabled, SHORT_DESCRIPTION and ACTION_COMMAND_KEY, and does not use NAME") {
                onUiThreadGet {
                    val action = fullAction().apply { isEnabled = false }
                    val combo = WComboBox(listOf("a", "b"))
                    combo.action = action
                    listOf(combo.isEnabled, combo.toolTipText, combo.actionCommand, combo.getItemAt(0))
                } shouldContainExactly listOf(false, "Save the file", "save", "a")
            }

            test("with an action without ACTION_COMMAND_KEY or null, WComboBox's actionCommand returns to the default comboBoxChanged") {
                onUiThreadGet {
                    val combo = WComboBox(listOf("a", "b"))
                    combo.action = fullAction()
                    combo.action = RecordingAction("Name only")
                    val withoutCommand = combo.actionCommand
                    combo.action = fullAction()
                    combo.action = null
                    listOf(withoutCommand, combo.actionCommand, combo.toolTipText)
                } shouldContainExactly listOf("comboBoxChanged", "comboBoxChanged", null)
            }

            test("WMenuBarItem(action) copies NAME to title, and MNEMONIC_KEY, enabled and SHORT_DESCRIPTION, and follows them") {
                onUiThreadGet {
                    val action = fullAction()
                    val menu = WMenuBarItem(action)
                    val first = listOf(menu.title, menu.mnemonic, menu.isEnabled, menu.toolTipText)
                    action.putValue(WAction.NAME, "File")
                    action.isEnabled = false
                    first + listOf(menu.title, menu.isEnabled)
                } shouldContainExactly listOf("Save", 'S'.code, true, "Save the file", "File", false)
            }

            test("WMenuFlyoutSubItem(action) copies NAME, SMALL_ICON, MNEMONIC_KEY, enabled and SHORT_DESCRIPTION, and null returns to the no-value state") {
                onUiThreadGet {
                    val action = fullAction().apply { isEnabled = false }
                    val sub = WMenuFlyoutSubItem(action)
                    val first = listOf(sub.text, sub.icon, sub.mnemonic, sub.isEnabled, sub.toolTipText)
                    sub.action = null
                    first + listOf(sub.text, sub.icon, sub.mnemonic, sub.isEnabled, sub.toolTipText)
                } shouldContainExactly listOf("Save", Symbol.SAVE, 'S'.code, false, "Save the file", "", null, 0, true, null)
            }
        }

        context("Events") {
            test("doClick on a WSplitButton placed in the window calls the action with the button as the source and ACTION_COMMAND_KEY as the actionCommand") {
                val action = onUiThreadGet { fullAction() }
                val button = loaded { WSplitButton().apply { this.action = action } }
                try {
                    onUiThread { button.doClick() }
                    val events = onUiThreadGet { action.events.toList() }
                    events.size shouldBe 1
                    events[0].source shouldBeSameInstanceAs button
                    events[0].actionCommand shouldBe "save"
                } finally {
                    UiTestHarness.detach(button)
                }
            }

            test("doClick on a WToggleSplitButton placed in the window toggles the check, writes it back to the action with SELECTED_KEY, and then calls it") {
                val action = onUiThreadGet { RecordingAction("Bold").apply { putValue(WAction.SELECTED_KEY, false) } }
                val button = loaded { WToggleSplitButton().apply { this.action = action } }
                try {
                    onUiThread { button.doClick() }
                    onUiThreadGet { listOf(button.isChecked, action.getValue(WAction.SELECTED_KEY), action.selectedWhenPerformed.toList()) } shouldContainExactly
                        listOf(true, true, listOf(true))
                } finally {
                    UiTestHarness.detach(button)
                }
            }

            test("changing the WComboBox selection calls the action, with the action's ACTION_COMMAND_KEY as the actionCommand") {
                onUiThreadGet {
                    val action = fullAction()
                    val combo = WComboBox(listOf("a", "b"))
                    combo.action = action
                    combo.selectedIndex = 1
                    action.events.map { it.actionCommand } to action.events.map { it.source === combo }
                } shouldBe (listOf("save") to listOf(true))
            }

            test("after removing the WComboBox action, changing the selection does not call the action, and it disappears from getActionListeners") {
                onUiThreadGet {
                    val action = fullAction()
                    val combo = WComboBox(listOf("a", "b"))
                    combo.action = action
                    val whileBound = combo.getActionListeners().size
                    combo.action = null
                    combo.selectedIndex = 1
                    listOf(whileBound, combo.getActionListeners().size, action.events.size)
                } shouldContainExactly listOf(1, 0, 0)
            }
        }

        context("Boundaries and exceptions") {
            test("binding an action whose NAME is not a string throws IllegalArgumentException") {
                val action = RecordingAction().apply { putValue(WAction.NAME, 1) }
                onUiThread {
                    shouldThrow<IllegalArgumentException> { WSplitButton().action = action }
                    shouldThrow<IllegalArgumentException> { WMenuBarItem().action = action }
                    shouldThrow<IllegalArgumentException> { WMenuFlyoutSubItem().action = action }
                }
            }
        }
    }
}
