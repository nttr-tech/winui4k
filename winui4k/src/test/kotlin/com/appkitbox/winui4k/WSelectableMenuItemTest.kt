package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the selection state (isSelected / state), ItemListener, Swing-shaped constructors, WAction SELECTED_KEY and WButtonGroup of the
 * JCheckBoxMenuItem-like WToggleMenuFlyoutItem and the JRadioButtonMenuItem-like WRadioMenuFlyoutItem.
 * Expected values come from Swing's behavior (on doClick the ItemEvent comes before the ActionEvent, a selected item in a ButtonGroup is not
 * deselected by a click, radio items not in a group are independent). The shape seen from Java is tested in WSelectableMenuItemForJavaTest.
 */
class WSelectableMenuItemTest : FunSpec() {
    /** Adds both an ItemListener and an ActionListener and records "item:<name>:SELECTED" / "action:<name>" in the order received. */
    private fun record(item: WMenuFlyoutItem, name: String, events: MutableList<String>) {
        (item as WSelectable).addItemListener { e ->
            val state = if (e.stateChange == WItemEvent.SELECTED) "SELECTED" else "DESELECTED"
            events += "item:$name:$state:${e.source === item}"
        }
        item.addActionListener { events += "action:$name" }
    }

    /** An action that records the value of SELECTED_KEY when called. */
    private class RecordingAction(name: String? = null) : WAbstractAction(name) {
        val selectedWhenPerformed = mutableListOf<Any?>()

        override fun actionPerformed(e: WActionEvent) {
            selectedWhenPerformed += getValue(WAction.SELECTED_KEY)
        }
    }

    private fun nativeGroupName(item: WRadioMenuFlyoutItem): String =
        item.inspectable.getString(XamlInterop.IRadioMenuFlyoutItem_get_GroupName)

    init {
        // ---- WToggleMenuFlyoutItem ----

        test("isSelected and state of WToggleMenuFlyoutItem are aliases of isChecked") {
            onUiThreadGet {
                val item = WToggleMenuFlyoutItem("Bold")
                val initial = listOf(item.isChecked, item.isSelected, item.state)
                item.isSelected = true
                val selected = listOf(item.isChecked, item.state)
                item.state = false
                initial + selected + listOf(item.isChecked, item.isSelected)
            } shouldContainExactly listOf(false, false, false, true, true, false, false)
        }

        test("the Swing-shaped constructors of WToggleMenuFlyoutItem set the text, icon, selection state and action") {
            onUiThreadGet {
                val selected = WToggleMenuFlyoutItem("Bold", true)
                val unselected = WToggleMenuFlyoutItem("Italic", false)
                val icon = WToggleMenuFlyoutItem(Symbol.BOLD)
                val all = WToggleMenuFlyoutItem("Underline", Symbol.UNDERLINE, true)
                val action = RecordingAction("Strikethrough").also { it.putValue(WAction.SELECTED_KEY, true) }
                val fromAction = WToggleMenuFlyoutItem(action)
                listOf(
                    selected.text to selected.isSelected,
                    unselected.text to unselected.isSelected,
                    icon.text to (icon.icon to icon.isSelected),
                    all.text to (all.icon to all.isSelected),
                    fromAction.text to fromAction.isSelected,
                )
            } shouldContainExactly listOf(
                "Bold" to true,
                "Italic" to false,
                "" to (Symbol.BOLD to false),
                "Underline" to (Symbol.UNDERLINE to true),
                "Strikethrough" to true,
            )
        }

        test("doClick on WToggleMenuFlyoutItem toggles the check and calls ItemListeners (SELECTED / DESELECTED) before ActionListeners") {
            val events = mutableListOf<String>()
            val states = onUiThreadGet {
                val item = WToggleMenuFlyoutItem("Bold")
                record(item, "Bold", events)
                item.doClick()
                val first = item.isSelected
                item.doClick()
                listOf(first, item.isSelected)
            }
            states shouldContainExactly listOf(true, false)
            onUiThreadGet { events.toList() } shouldContainExactly listOf(
                "item:Bold:SELECTED:true",
                "action:Bold",
                "item:Bold:DESELECTED:true",
                "action:Bold",
            )
        }

        test("ItemListeners of WToggleMenuFlyoutItem are called on programmatic changes too, but not when the same value is set") {
            val events = mutableListOf<String>()
            onUiThread {
                val item = WToggleMenuFlyoutItem("Bold")
                record(item, "Bold", events)
                item.isSelected = true
                item.state = true
                item.isChecked = false
            }
            onUiThreadGet { events.toList() } shouldContainExactly listOf("item:Bold:SELECTED:true", "item:Bold:DESELECTED:true")
        }

        test("a listener removed with removeItemListener is not called") {
            val events = mutableListOf<String>()
            onUiThread {
                val item = WToggleMenuFlyoutItem("Bold")
                val listener = WItemListener { events += "removed" }
                item.addItemListener(listener)
                item.addItemListener { events += "kept" }
                item.removeItemListener(listener)
                item.isSelected = true
            }
            onUiThreadGet { events.toList() } shouldContainExactly listOf("kept")
        }

        test("SELECTED_KEY of the action of WToggleMenuFlyoutItem is mapped both ways, and inside actionPerformed it is already the new state") {
            val action = RecordingAction("Bold").also { it.putValue(WAction.SELECTED_KEY, false) }
            val described = onUiThreadGet {
                val item = WToggleMenuFlyoutItem(action)
                item.doClick()
                val afterClick = listOf(item.isSelected, action.getValue(WAction.SELECTED_KEY))
                action.putValue(WAction.SELECTED_KEY, false)
                val afterPut = item.isSelected
                item.isSelected = true
                afterClick + listOf(afterPut, action.getValue(WAction.SELECTED_KEY), action.selectedWhenPerformed.toList())
            }
            described shouldContainExactly listOf(true, true, false, true, listOf(true))
        }

        test("the selection state is not written to an action without SELECTED_KEY") {
            val action = RecordingAction("Bold")
            onUiThreadGet {
                val item = WToggleMenuFlyoutItem(action)
                item.doClick()
                item.isSelected to action.getValue(WAction.SELECTED_KEY)
            } shouldBe (true to null)
        }

        test("an action whose SELECTED_KEY is not a Boolean throws IllegalArgumentException") {
            onUiThread {
                val action = RecordingAction("Bold").also { it.putValue(WAction.SELECTED_KEY, "yes") }
                shouldThrow<IllegalArgumentException> { WToggleMenuFlyoutItem(action) }
            }
        }

        test("doClick on a disabled WToggleMenuFlyoutItem does not change the check") {
            onUiThreadGet {
                val item = WToggleMenuFlyoutItem("Bold")
                item.isEnabled = false
                item.doClick()
                item.isSelected
            } shouldBe false
        }

        // ---- WRadioMenuFlyoutItem ----

        test("the Swing-shaped constructors of WRadioMenuFlyoutItem set the text, icon, selection state and action") {
            onUiThreadGet {
                val selected = WRadioMenuFlyoutItem("Small", true)
                val icon = WRadioMenuFlyoutItem(Symbol.FONT_SIZE)
                val textIcon = WRadioMenuFlyoutItem("Medium", Symbol.FONT_SIZE)
                val noIcon = WRadioMenuFlyoutItem("Medium", null as WIcon?)
                val all = WRadioMenuFlyoutItem("Large", Symbol.FONT_SIZE, true)
                val action = RecordingAction("Extra large").also { it.putValue(WAction.SELECTED_KEY, true) }
                val fromAction = WRadioMenuFlyoutItem(action)
                listOf(
                    listOf(selected.text, selected.icon, selected.isSelected),
                    listOf(icon.text, icon.icon, icon.isSelected),
                    listOf(textIcon.text, textIcon.icon, textIcon.isSelected),
                    listOf(noIcon.text, noIcon.icon, noIcon.isSelected),
                    listOf(all.text, all.icon, all.isSelected),
                    listOf(fromAction.text, fromAction.icon, fromAction.isSelected),
                )
            } shouldContainExactly listOf(
                listOf("Small", null, true),
                listOf("", Symbol.FONT_SIZE, false),
                listOf("Medium", Symbol.FONT_SIZE, false),
                listOf("Medium", null, false),
                listOf("Large", Symbol.FONT_SIZE, true),
                listOf("Extra large", null, true),
            )
        }

        test("WRadioMenuFlyoutItems without a group name are independent of each other (as in Swing), and groupName returns \"\"") {
            onUiThreadGet {
                val a = WRadioMenuFlyoutItem("A")
                val b = WRadioMenuFlyoutItem("B")
                a.doClick()
                b.doClick()
                listOf(a.isSelected, b.isSelected, a.groupName, b.groupName, nativeGroupName(a) != nativeGroupName(b))
            } shouldContainExactly listOf(true, true, "", "", true)
        }

        test("setting groupName back to \"\" makes it an independent item again") {
            onUiThreadGet {
                val a = WRadioMenuFlyoutItem("A", "Size")
                val b = WRadioMenuFlyoutItem("B", "Size")
                a.isSelected = true
                b.isSelected = true
                val grouped = listOf(a.isSelected, b.isSelected, a.groupName)
                a.groupName = ""
                a.isSelected = true
                grouped + listOf(a.isSelected, b.isSelected, a.groupName)
            } shouldContainExactly listOf(false, true, "Size", true, true, "")
        }

        test("items with the same explicit groupName are exclusive even in different menus (WinUI's GroupName is shared across the UI thread; the selected one is notified first)") {
            val events = mutableListOf<String>()
            onUiThreadGet {
                val a = WRadioMenuFlyoutItem("A", "Shared name")
                val b = WRadioMenuFlyoutItem("B", "Shared name")
                WMenuFlyout().add(a)
                WMenuFlyout().add(b)
                record(a, "A", events)
                record(b, "B", events)
                a.doClick()
                b.doClick()
                listOf(a.isSelected, b.isSelected)
            } shouldContainExactly listOf(false, true)
            onUiThreadGet { events.toList() } shouldContainExactly listOf(
                "item:A:SELECTED:true",
                "action:A",
                // With exclusion by WinUI's GroupName alone, the selected item's SELECTED arrives before the deselected item's DESELECTED (the reverse in WButtonGroup, as in Swing)
                "item:B:SELECTED:true",
                "item:A:DESELECTED:true",
                "action:B",
            )
        }

        test("doClick again on a checked WRadioMenuFlyoutItem keeps the check, and only ActionListeners are called, not ItemListeners") {
            val events = mutableListOf<String>()
            onUiThreadGet {
                val a = WRadioMenuFlyoutItem("A", "Size")
                record(a, "A", events)
                a.doClick()
                a.doClick()
                a.isSelected
            } shouldBe true
            onUiThreadGet { events.toList() } shouldContainExactly listOf("item:A:SELECTED:true", "action:A", "action:A")
        }

        test("SELECTED_KEY of the action of WRadioMenuFlyoutItem is mapped both ways") {
            val action = RecordingAction("Small").also { it.putValue(WAction.SELECTED_KEY, false) }
            onUiThreadGet {
                val item = WRadioMenuFlyoutItem(action)
                item.doClick()
                val afterClick = listOf(item.isSelected, action.getValue(WAction.SELECTED_KEY), action.selectedWhenPerformed.toList())
                action.putValue(WAction.SELECTED_KEY, false)
                afterClick + listOf(item.isSelected)
            } shouldContainExactly listOf(true, true, listOf(true), false)
        }

        // ---- WButtonGroup ----

        test("radio items in a WButtonGroup are exclusive, and the deselected item's DESELECTED arrives before the selected item's SELECTED") {
            val events = mutableListOf<String>()
            val state = onUiThreadGet {
                val group = WButtonGroup()
                val small = WRadioMenuFlyoutItem("Small")
                val large = WRadioMenuFlyoutItem("Large")
                record(small, "Small", events)
                record(large, "Large", events)
                group.add(small)
                group.add(large)
                small.doClick()
                large.doClick()
                listOf(small.isSelected, large.isSelected, group.selection === large, group.selection?.actionCommand)
            }
            state shouldContainExactly listOf(false, true, true, "Large")
            onUiThreadGet { events.toList() } shouldContainExactly listOf(
                "item:Small:SELECTED:true",
                "action:Small",
                "item:Small:DESELECTED:true",
                "item:Large:SELECTED:true",
                "action:Large",
            )
        }

        test("two groups of radio items with the same texts do not interfere with each other") {
            onUiThreadGet {
                val first = WButtonGroup()
                val second = WButtonGroup()
                val items = listOf("Small", "Large", "Small", "Large").map { WRadioMenuFlyoutItem(it) }
                first.add(items[0])
                first.add(items[1])
                second.add(items[2])
                second.add(items[3])
                items[0].doClick()
                items[3].doClick()
                items.map { it.isSelected } + listOf(items[0].groupName == items[1].groupName, items[0].groupName != items[2].groupName)
            } shouldContainExactly listOf(true, false, false, true, true, true)
        }

        test("check items in a WButtonGroup are exclusive too, and clicking the selected item does not deselect it and only calls ActionListeners") {
            val events = mutableListOf<String>()
            val state = onUiThreadGet {
                val group = WButtonGroup()
                val left = WToggleMenuFlyoutItem("Align left")
                val center = WToggleMenuFlyoutItem("Center")
                record(left, "Left", events)
                record(center, "Medium", events)
                group.add(left)
                group.add(center)
                left.doClick()
                center.doClick()
                center.doClick()
                listOf(left.isSelected, center.isSelected, group.selection === center)
            }
            state shouldContainExactly listOf(false, true, true)
            onUiThreadGet { events.toList() } shouldContainExactly listOf(
                "item:Left:SELECTED:true",
                "action:Left",
                "item:Left:DESELECTED:true",
                "item:Medium:SELECTED:true",
                "action:Medium",
                "action:Medium",
            )
        }

        test("buttons and menu items in one WButtonGroup are exclusive too (the same choice on a toolbar and in a menu)") {
            onUiThreadGet {
                val group = WButtonGroup()
                val button = WToggleButton("Bold")
                val item = WToggleMenuFlyoutItem("Bold")
                group.add(button)
                group.add(item)
                button.doClick()
                item.doClick()
                listOf(button.isSelected, item.isSelected, group.selection === item, group.buttonCount)
            } shouldContainExactly listOf(false, true, true, 2)
        }

        test("a radio item removed from a WButtonGroup goes back to its original groupName (independent if \"\")") {
            onUiThreadGet {
                val group = WButtonGroup()
                val named = WRadioMenuFlyoutItem("A", "Original name")
                val ungrouped = WRadioMenuFlyoutItem("B")
                group.add(named)
                group.add(ungrouped)
                val inGroup = named.groupName == ungrouped.groupName
                group.remove(named)
                group.remove(ungrouped)
                named.isSelected = true
                ungrouped.isSelected = true
                listOf(inGroup, named.groupName, ungrouped.groupName, named.isSelected, ungrouped.isSelected)
            } shouldContainExactly listOf(true, "Original name", "", true, true)
        }

        test("the group's clearSelection and the action's SELECTED_KEY = false also deselect menu items") {
            onUiThreadGet {
                val group = WButtonGroup()
                val action = RecordingAction("Small").also { it.putValue(WAction.SELECTED_KEY, true) }
                val small = WRadioMenuFlyoutItem(action)
                val large = WRadioMenuFlyoutItem("Large")
                group.add(small)
                group.add(large)
                val added = group.selection === small
                action.putValue(WAction.SELECTED_KEY, false)
                val afterAction = listOf(small.isSelected, group.selection)
                large.isSelected = true
                group.clearSelection()
                listOf(added) + afterAction + listOf(large.isSelected, group.selection)
            } shouldContainExactly listOf(true, false, null, false, null)
        }

        test("selecting a radio item of an open menu with the keyboard (Enter) checks it and calls ItemListeners and ActionListeners") {
            val button = onUiThreadGet { WButton("Menu") }
            val small = onUiThreadGet { WRadioMenuFlyoutItem("Small") }
            val large = onUiThreadGet { WRadioMenuFlyoutItem("Large") }
            val menu = onUiThreadGet {
                WMenuFlyout().also {
                    it.add(small)
                    it.add(large)
                }
            }
            val events = mutableListOf<String>()
            onUiThread {
                val group = WButtonGroup()
                group.add(small)
                group.add(large)
                small.isSelected = true
                record(small, "Small", events)
                record(large, "Large", events)
            }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread { menu.showAt(button) }
                eventually(5.seconds) {
                    onUiThreadGet { large.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) }.shouldBeTrue()
                }
                Thread.sleep(OPEN_SETTLE_MILLIS)
                onUiThread { check(large.requestFocusInWindow()) }
                TestKeyboard.pressEnter()
                eventually(3.seconds) {
                    onUiThreadGet { events.toList() } shouldContainExactly listOf(
                        "item:Small:DESELECTED:true",
                        "item:Large:SELECTED:true",
                        "action:Large",
                    )
                }
                onUiThreadGet { listOf(small.isSelected, large.isSelected) } shouldContainExactly listOf(false, true)
            } finally {
                onUiThread { menu.hide() }
                UiTestHarness.detach(button)
            }
        }

        test("selection and elements of WButtonGroup can read text and actionCommand as WSelectable") {
            onUiThreadGet {
                val group = WButtonGroup()
                val item = WRadioMenuFlyoutItem("Large").also { it.actionCommand = "size-large" }
                group.add(WRadioButton("Small"))
                group.add(item)
                group.setSelected(item, true)
                listOf(group.selection?.text, group.selection?.actionCommand, group.elements.toList().map { it.text })
            } shouldContainExactly listOf("Large", "size-large", listOf("Small", "Large"))
        }

        test("ItemListeners of WRadioMenuFlyoutItem watch its own IsChecked (not the base ToggleMenuFlyoutItem.IsChecked)") {
            val events = mutableListOf<String>()
            onUiThread {
                val item = WRadioMenuFlyoutItem("A")
                item.addItemListener { e -> events += "${e.stateChange}" }
                item.isChecked = true
                item.isChecked = false
            }
            onUiThreadGet { events.toList() } shouldContainExactly listOf("${WItemEvent.SELECTED}", "${WItemEvent.DESELECTED}")
        }
    }

    private companion object {
        const val OPEN_SETTLE_MILLIS = 500L
    }
}
