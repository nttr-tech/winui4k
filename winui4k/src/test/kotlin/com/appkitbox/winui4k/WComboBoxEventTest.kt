package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WComboBox's JComboBox-like events (ItemListener, ActionListener, PopupMenuListener) and Swing-named item operations.
 * Expected values follow JComboBox (DefaultComboBoxModel) behavior. The shape seen from Java is tested in WComboBoxForJavaTest.
 */
@Tags("E2E")
class WComboBoxEventTest : FunSpec() {
    /** A combo that lines up item and action events in one record. */
    private class Recorded(val comboBox: WComboBox) {
        val events = mutableListOf<String>()

        init {
            comboBox.addItemListener { e ->
                val state = if (e.stateChange == WItemEvent.SELECTED) "SELECTED" else "DESELECTED"
                check(e.source === comboBox && e.itemSelectable === comboBox)
                events += "$state:${e.item}"
            }
            comboBox.addActionListener { e ->
                check(e.source === comboBox)
                events += "action:${e.actionCommand}"
            }
        }
    }

    private fun recorded(vararg items: String) = Recorded(WComboBox(items.toList()))

    /** Records the three PopupMenuListener notifications as "visible", "canceled" and "invisible" (call on the UI thread). */
    private fun popupEvents(comboBox: WComboBox): MutableList<String> {
        val events = mutableListOf<String>()
        comboBox.addPopupMenuListener(object : WPopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
                events += "visible"
            }

            override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                events += "invisible"
            }

            override fun popupMenuCanceled(e: WPopupMenuEvent) {
                events += "canceled"
            }
        })
        return events
    }

    /**
     * Moves the focus, opens the drop-down with F4, and waits until it has finished opening and settled. Opening with the keyboard moves the keyboard
     * focus to the selected item, and Enter selects that item (when opened with showPopup, keys do not reach the items).
     */
    private suspend fun openWithFocus(comboBox: WComboBox, events: MutableList<String>) {
        onUiThread { check(comboBox.requestFocusInWindow()) { "Cannot move the focus" } }
        TestKeyboard.press(VK_F4, SCAN_F4)
        eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible") }
        Thread.sleep(SETTLE_MILLIS) // The focus moves to the selected item during the opening animation
    }

    init {
        test("each selection change delivers the previous item's DESELECTED, the new item's SELECTED and an ActionEvent (comboBoxChanged) in that order") {
            onUiThreadGet {
                val combo = recorded("Red", "Green", "Blue")
                combo.comboBox.selectedIndex = 0
                combo.comboBox.selectedIndex = 1
                combo.events
            } shouldContainExactly listOf(
                "SELECTED:Red",
                "action:comboBoxChanged",
                "DESELECTED:Red",
                "SELECTED:Green",
                "action:comboBoxChanged",
            )
        }

        test("deselecting with selectedIndex = -1 delivers only DESELECTED, plus an ActionEvent") {
            onUiThreadGet {
                val combo = recorded("Red", "Green")
                combo.comboBox.selectedIndex = 1
                combo.events.clear()
                combo.comboBox.selectedIndex = -1
                combo.events to combo.comboBox.selectedItem
            }.let { (events, item) ->
                events shouldContainExactly listOf("DESELECTED:Green", "action:comboBoxChanged")
                item.shouldBeNull()
            }
        }

        test("reselecting the same index delivers no item events, only an ActionEvent (same as JComboBox.setSelectedIndex)") {
            onUiThreadGet {
                val combo = recorded("Red", "Green")
                combo.comboBox.selectedIndex = 1
                combo.events.clear()
                combo.comboBox.selectedIndex = 1
                combo.events
            } shouldContainExactly listOf("action:comboBoxChanged")
        }

        test("actionCommand defaults to comboBoxChanged, and changing it puts that string into events") {
            onUiThreadGet {
                val combo = recorded("Red", "Green")
                val initial = combo.comboBox.actionCommand
                combo.comboBox.actionCommand = "Color"
                combo.comboBox.selectedIndex = 0
                initial to combo.events
            }.let { (initial, events) ->
                initial shouldBe "comboBoxChanged"
                events shouldContainExactly listOf("SELECTED:Red", "action:Color")
            }
        }

        test("setting selectedIndex to -2 or the item count or more throws IllegalArgumentException, and neither the selection nor events change") {
            onUiThreadGet {
                val combo = recorded("Red", "Green")
                combo.comboBox.selectedIndex = 0
                combo.events.clear()
                shouldThrow<IllegalArgumentException> { combo.comboBox.selectedIndex = 2 }
                shouldThrow<IllegalArgumentException> { combo.comboBox.selectedIndex = -2 }
                combo.comboBox.selectedIndex to combo.events.toList()
            }.let { (index, events) ->
                index shouldBe 0
                events.shouldBeEmpty()
            }
        }

        test("selectedIndex set while there are no items is remembered and selects the item at that position when it is added (the previous WinUI behavior; only once)") {
            onUiThreadGet {
                val combo = recorded()
                combo.comboBox.selectedIndex = 2
                val states = mutableListOf(combo.comboBox.selectedIndex)
                combo.comboBox.addItem("Red")
                combo.comboBox.addItem("Green")
                states += combo.comboBox.selectedIndex
                combo.comboBox.addItem("Blue")
                states += combo.comboBox.selectedIndex
                val events = combo.events.toList()
                combo.comboBox.removeAllItems()
                for (item in listOf("Red", "Green", "Blue")) combo.comboBox.addItem(item)
                states += combo.comboBox.selectedIndex
                Triple(states, events, combo.comboBox.selectedItem)
            }.let { (states, events, selected) ->
                states shouldContainExactly listOf(-1, -1, 2, -1)
                events shouldContainExactly listOf("SELECTED:Blue", "action:comboBoxChanged")
                selected.shouldBeNull()
            }
        }

        test("selectedIndex remembered while there were no items is replaced by selectedIndex set later (including -1). -2 throws even without items") {
            onUiThreadGet {
                val cleared = WComboBox()
                cleared.selectedIndex = 1
                cleared.selectedIndex = -1
                for (item in listOf("Red", "Green")) cleared.addItem(item)
                val replaced = WComboBox()
                replaced.selectedIndex = 1
                replaced.selectedIndex = 0
                replaced.addItem("Red")
                shouldThrow<IllegalArgumentException> { WComboBox().selectedIndex = -2 }
                cleared.selectedIndex to replaced.selectedItem
            } shouldBe (-1 to "Red")
        }

        test("setSelectedItem selects the matching item, and ignores strings not in the list if non-editable (no events either)") {
            onUiThreadGet {
                val combo = recorded("Red", "Green", "Blue")
                combo.comboBox.setSelectedItem("Blue")
                val selected = combo.comboBox.selectedIndex
                combo.events.clear()
                combo.comboBox.setSelectedItem("Purple")
                Triple(selected, combo.comboBox.selectedIndex, combo.events.toList())
            }.let { (selected, after, events) ->
                selected shouldBe 2
                after shouldBe 2
                events.shouldBeEmpty()
            }
        }

        test("if editable, setSelectedItem can also select a string not in the list, and selectedItem becomes that string and selectedIndex -1") {
            val combo = onUiThreadGet { recorded("Red", "Green").also { it.comboBox.isEditable = true } }
            UiTestHarness.attachAndAwaitLoaded(combo.comboBox)
            try {
                onUiThreadGet {
                    combo.comboBox.selectedIndex = 0
                    combo.events.clear()
                    combo.comboBox.setSelectedItem("Purple")
                    Triple(combo.comboBox.selectedItem, combo.comboBox.selectedIndex, combo.events.toList())
                }.let { (item, index, events) ->
                    item shouldBe "Purple"
                    index shouldBe -1
                    events shouldContainExactly listOf("DESELECTED:Red", "SELECTED:Purple", "action:comboBoxChanged")
                }
            } finally {
                UiTestHarness.detach(combo.comboBox)
            }
        }

        test("setSelectedItem(null) deselects") {
            onUiThreadGet {
                val combo = recorded("Red")
                combo.comboBox.selectedIndex = 0
                combo.events.clear()
                combo.comboBox.setSelectedItem(null)
                combo.comboBox.selectedIndex to combo.events.toList()
            }.let { (index, events) ->
                index shouldBe -1
                events shouldContainExactly listOf("DESELECTED:Red", "action:comboBoxChanged")
            }
        }

        test("getSelectedObjects is an array of only the selected item, and empty if unselected") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green"))
                val none = comboBox.getSelectedObjects().toList()
                comboBox.selectedIndex = 1
                none to comboBox.getSelectedObjects().toList()
            }.let { (none, one) ->
                none.shouldBeEmpty()
                one shouldContainExactly listOf("Green")
            }
        }

        test("getItemAt returns null when out of range, and insertItemAt inserts at the given position") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Blue"))
                comboBox.insertItemAt("Green", 1)
                listOf(comboBox.getItemAt(0), comboBox.getItemAt(1), comboBox.getItemAt(2), comboBox.getItemAt(3), comboBox.getItemAt(-1))
            } shouldContainExactly listOf("Red", "Green", "Blue", null, null)
        }

        test("removeItemAt removes by index, and removeItem removes the first equal item (does nothing if there is none)") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue", "Green", "White"))
                comboBox.removeItemAt(0)
                comboBox.removeItem("Green")
                comboBox.removeItem("Missing")
                comboBox.removeItemAt(2)
                (0 until comboBox.itemCount).map { comboBox.getItem(it) }
            } shouldContainExactly listOf("Blue", "Green")
        }

        test("removeItem(1) with numeric items removes the item equal to 1, as with JComboBox.removeItem(Object), and does not remove the second item") {
            onUiThreadGet {
                val comboBox = WComboBox(WDefaultComboBoxModel(arrayOf(3, 2, 1, 0)))
                comboBox.removeItem(1)
                comboBox.removeItem(7)
                (0 until comboBox.itemCount).map { comboBox.model.getElementAt(it) }
            } shouldContainExactly listOf(3, 2, 0)
        }

        test("removeAllItems fires DESELECTED for the selected item and an ActionEvent, as in JComboBox") {
            onUiThreadGet {
                val combo = recorded("Red", "Green")
                combo.comboBox.selectedIndex = 0
                combo.events.clear()
                combo.comboBox.removeAllItems()
                combo.comboBox.addItem("Blue")
                combo.comboBox.selectedIndex = 0
                combo.events.toList()
            } shouldContainExactly listOf("DESELECTED:Red", "action:comboBoxChanged", "SELECTED:Blue", "action:comboBoxChanged")
        }

        test("changing the selection inside an ActionListener fires no nested ActionEvent, as in JComboBox") {
            onUiThreadGet {
                val combo = recorded("Red", "Green")
                combo.comboBox.addActionListener { if (combo.comboBox.selectedIndex == 0) combo.comboBox.selectedIndex = 1 }
                combo.comboBox.selectedIndex = 0
                combo.events.toList() to combo.comboBox.selectedIndex
            }.let { (events, index) ->
                events shouldContainExactly listOf("SELECTED:Red", "action:comboBoxChanged", "DESELECTED:Red", "SELECTED:Green")
                index shouldBe 1
            }
        }

        test("the array constructor selects the first item as with JComboBox(E[]), and the List constructor still leaves it unselected") {
            onUiThreadGet {
                WComboBox(arrayOf("Red", "Green")).selectedItem to WComboBox(listOf("Red", "Green")).selectedIndex
            }.let { (fromArray, fromList) ->
                fromArray shouldBe "Red"
                fromList shouldBe -1
            }
        }

        test("listeners removed with removeItemListener and removeActionListener are not called, and getItemListeners etc. return them in the order added") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green"))
                val events = mutableListOf<String>()
                val item: (WItemEvent) -> Unit = { events += "item" }
                val action: (WActionEvent) -> Unit = { events += "action" }
                val javaItem = WItemListener { events += "javaItem" }
                comboBox.addItemListener(item)
                comboBox.addActionListener(action)
                comboBox.addItemListener(javaItem)
                val counts = comboBox.getItemListeners().size to comboBox.getActionListeners().size
                comboBox.removeItemListener(item)
                comboBox.removeActionListener(action)
                comboBox.selectedIndex = 0
                Triple(counts, comboBox.getItemListeners().toList().also { check(it.single() === javaItem) }, events.toList())
            }.let { (counts, itemListeners, events) ->
                counts shouldBe (2 to 1)
                itemListeners.size shouldBe 1
                events shouldContainExactly listOf("javaItem")
            }
        }

        test("header reads back the set string from native") {
            onUiThreadGet {
                val comboBox = WComboBox()
                val initial = comboBox.header
                comboBox.header = "Color"
                initial to comboBox.header
            } shouldBe ("" to "Color")
        }

        test("maximumRowCount defaults to 8, and setting it before opening decides MaxDropDownHeight from the estimated row height of 36 DIP plus half a row of slack and a 10 DIP border") {
            onUiThreadGet {
                val comboBox = WComboBox()
                val initial = comboBox.maximumRowCount
                comboBox.maximumRowCount = 5
                Triple(initial, comboBox.maximumRowCount, comboBox.inspectable.getDouble(XamlInterop.IComboBox_get_MaxDropDownHeight))
            }.let { (initial, rows, height) ->
                initial shouldBe 8
                rows shouldBe 5
                height shouldBe 5.5 * 36.0 + 10.0
            }
        }

        test("opening with showPopup delivers popupMenuWillBecomeVisible, and closing with hidePopup without selecting delivers only popupMenuWillBecomeInvisible without popupMenuCanceled (the same as Swing's hidePopup)") {
            val comboBox = onUiThreadGet { WComboBox(listOf("Red", "Green")) }
            val events = mutableListOf<String>()
            onUiThread {
                comboBox.addPopupMenuListener(object : WPopupMenuListener {
                    override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
                        check(e.source === comboBox)
                        events += "visible"
                    }

                    override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                        events += "invisible"
                    }

                    override fun popupMenuCanceled(e: WPopupMenuEvent) {
                        events += "canceled"
                    }
                })
            }
            UiTestHarness.attachAndAwaitLoaded(comboBox)
            try {
                onUiThread { comboBox.showPopup() }
                eventually(3.seconds) { onUiThreadGet { events.toList() to comboBox.isPopupVisible } shouldBe (listOf("visible") to true) }
                onUiThread { comboBox.hidePopup() }
                eventually(3.seconds) {
                    onUiThreadGet { events.toList() to comboBox.isPopupVisible } shouldBe (listOf("visible", "invisible") to false)
                }
            } finally {
                UiTestHarness.detach(comboBox)
            }
        }

        context("Events") {
            test("opening and closing with Esc delivers popupMenuWillBecomeInvisible after popupMenuCanceled, and the selection stays as before opening") {
                val comboBox = onUiThreadGet { WComboBox(listOf("Red", "Green")).apply { selectedIndex = 0 } }
                val events = onUiThreadGet { popupEvents(comboBox) }
                UiTestHarness.attachAndAwaitLoaded(comboBox)
                try {
                    openWithFocus(comboBox, events)
                    TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)
                    eventually(3.seconds) {
                        onUiThreadGet { events.toList() to comboBox.isPopupVisible } shouldBe (listOf("visible", "canceled", "invisible") to false)
                    }
                    onUiThreadGet { comboBox.selectedIndex } shouldBe 0
                } finally {
                    UiTestHarness.detach(comboBox)
                }
            }

            test("opening and reselecting the current item with Enter does not change the selection, and there is no popupMenuCanceled (as in Swing, it is not a cancellation)") {
                val comboBox = onUiThreadGet { WComboBox(listOf("Red", "Green")).apply { selectedIndex = 0 } }
                val events = onUiThreadGet { popupEvents(comboBox) }
                UiTestHarness.attachAndAwaitLoaded(comboBox)
                try {
                    openWithFocus(comboBox, events)
                    TestKeyboard.pressEnter()
                    eventually(3.seconds) {
                        onUiThreadGet { events.toList() to comboBox.isPopupVisible } shouldBe (listOf("visible", "invisible") to false)
                    }
                    Thread.sleep(SETTLE_MILLIS)
                    onUiThreadGet { events.toList() to comboBox.selectedIndex } shouldBe (listOf("visible", "invisible") to 0)
                } finally {
                    UiTestHarness.detach(comboBox)
                }
            }

            test("after closing by selecting with Enter, opening again and closing with Esc delivers popupMenuCanceled that time (the selection mark is reset on every opening)") {
                val comboBox = onUiThreadGet { WComboBox(listOf("Red", "Green")).apply { selectedIndex = 0 } }
                val events = onUiThreadGet { popupEvents(comboBox) }
                UiTestHarness.attachAndAwaitLoaded(comboBox)
                try {
                    openWithFocus(comboBox, events)
                    TestKeyboard.pressEnter()
                    eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible", "invisible") }
                    onUiThread { events.clear() }
                    openWithFocus(comboBox, events)
                    TestKeyboard.press(VK_ESCAPE, SCAN_ESCAPE)
                    eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible", "canceled", "invisible") }
                } finally {
                    UiTestHarness.detach(comboBox)
                }
            }
        }

        test("closing after the selection changed while open delivers only popupMenuWillBecomeInvisible, without popupMenuCanceled") {
            val comboBox = onUiThreadGet { WComboBox(listOf("Red", "Green")) }
            val events = mutableListOf<String>()
            onUiThread {
                comboBox.addPopupMenuListener(object : WPopupMenuAdapter() {
                    override fun popupMenuWillBecomeVisible(e: WPopupMenuEvent) {
                        events += "visible"
                    }

                    override fun popupMenuWillBecomeInvisible(e: WPopupMenuEvent) {
                        events += "invisible"
                    }

                    override fun popupMenuCanceled(e: WPopupMenuEvent) {
                        events += "canceled"
                    }
                })
            }
            UiTestHarness.attachAndAwaitLoaded(comboBox)
            try {
                onUiThread { comboBox.isPopupVisible = true }
                eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible") }
                onUiThread {
                    comboBox.selectedIndex = 1
                    comboBox.isPopupVisible = false
                }
                eventually(3.seconds) { onUiThreadGet { events.toList() } shouldContainExactly listOf("visible", "invisible") }
            } finally {
                UiTestHarness.detach(comboBox)
            }
        }

        test("pressing Enter in an editable combo delivers the comboBoxEdited ActionEvent after the selection change events") {
            val comboBox = onUiThreadGet { WComboBox(listOf("Red", "Green")).apply { isEditable = true } }
            val recorded = onUiThreadGet { Recorded(comboBox) }
            UiTestHarness.attachAndAwaitLoaded(comboBox)
            try {
                onUiThread {
                    comboBox.text = "Purple"
                    check(comboBox.requestFocusInWindow()) { "Cannot move the focus" }
                }
                TestKeyboard.pressEnter()
                eventually(3.seconds) {
                    onUiThreadGet { recorded.events.toList() } shouldContainExactly
                        listOf("SELECTED:Purple", "action:comboBoxChanged", "action:comboBoxEdited")
                }
                onUiThreadGet { comboBox.selectedItem to comboBox.selectedIndex } shouldBe ("Purple" to -1)
            } finally {
                UiTestHarness.detach(comboBox)
            }
        }
    }

    private companion object {
        const val VK_ESCAPE = 0x1B
        const val SCAN_ESCAPE = 0x01
        const val VK_F4 = 0x73
        const val SCAN_F4 = 0x3E
        const val SETTLE_MILLIS = 500L
    }
}
