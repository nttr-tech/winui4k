package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that WComboBox shows the items and selection of a [WComboBoxModel] and fires the same events as JComboBox on model changes.
 * The native ComboBox.Items and SelectedIndex are also read to check that the model is reflected. The shape seen from Java is tested in WComboBoxForJavaTest.
 */
@Tags("E2E")
class WComboBoxModelTest : FunSpec() {
    /** A non-string item whose display string is its name. */
    private data class Color(val name: String, val rgb: Int) {
        override fun toString(): String = name
    }

    /** The strings of the native ComboBox.Items. */
    private fun nativeItems(comboBox: WComboBox): List<String?> {
        val itemsControl = comboBox.inspectable.queryInterface(XamlInterop.IID_IItemsControl)
        try {
            val items = itemsControl.getPtr(XamlInterop.IItemsControl_get_Items)
            try {
                val vector = items.queryInterface(FoundationInterop.IID_IVector_Object)
                try {
                    return (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index ->
                        val boxed = vector.getPtr(FoundationInterop.IVector_GetAt, index)
                        try {
                            PropertyValues.unboxString(boxed)
                        } finally {
                            boxed.release()
                        }
                    }
                } finally {
                    vector.release()
                }
            } finally {
                items.release()
            }
        } finally {
            itemsControl.release()
        }
    }

    /** The native Selector.SelectedIndex. */
    private fun nativeSelectedIndex(comboBox: WComboBox): Int {
        val selector: ComPtr = comboBox.inspectable.queryInterface(XamlInterop.IID_ISelector)
        return try {
            selector.getInt(XamlInterop.ISelector_get_SelectedIndex)
        } finally {
            selector.release()
        }
    }

    /** Records item and action events. */
    private fun WComboBox.recorded(): MutableList<String> {
        val events = mutableListOf<String>()
        addItemListener { e -> events += "${if (e.stateChange == WItemEvent.SELECTED) "SELECTED" else "DESELECTED"}:${e.item}" }
        addActionListener { e -> events += "action:${e.actionCommand}" }
        return events
    }

    init {
        test("a combo given a model shows the model's items and selection, and getModel returns that model") {
            onUiThreadGet {
                val model = WDefaultComboBoxModel(arrayOf("Red", "Green", "Blue"))
                model.selectedItem = "Green"
                val comboBox = WComboBox(model)
                listOf(comboBox.model === model, comboBox.itemCount, comboBox.selectedIndex, nativeItems(comboBox), nativeSelectedIndex(comboBox))
            } shouldContainExactly listOf(true, 3, 1, listOf("Red", "Green", "Blue"), 1)
        }

        test("adding, removing or replacing items in the model makes the native items the same") {
            onUiThreadGet {
                val model = WDefaultComboBoxModel<String>()
                val comboBox = WComboBox(model)
                model.addElement("Red")
                model.addAll(listOf("Blue", "White"))
                model.insertElementAt("Green", 1)
                model.removeElementAt(3)
                nativeItems(comboBox) to nativeSelectedIndex(comboBox)
            } shouldBe (listOf("Red", "Green", "Blue") to 0)
        }

        test("changing the model's selection programmatically delivers item events and an ActionEvent, as in JComboBox") {
            onUiThreadGet {
                val model = WDefaultComboBoxModel(arrayOf("Red", "Green"))
                val comboBox = WComboBox(model)
                val events = comboBox.recorded()
                model.selectedItem = "Green"
                Triple(events.toList(), comboBox.selectedItem, nativeSelectedIndex(comboBox))
            } shouldBe Triple(listOf("DESELECTED:Red", "SELECTED:Green", "action:comboBoxChanged"), "Green", 1)
        }

        test("when the selection changes on the native side (the same path as user operations), the model's selectedItem changes and events are delivered") {
            onUiThreadGet {
                val model = WDefaultComboBoxModel(arrayOf("Red", "Green"))
                val comboBox = WComboBox(model)
                val events = comboBox.recorded()
                val selector = comboBox.inspectable.queryInterface(XamlInterop.IID_ISelector)
                try {
                    selector.call(XamlInterop.ISelector_put_SelectedIndex, 1)
                } finally {
                    selector.release()
                }
                model.selectedItem to events.toList()
            } shouldBe ("Green" to listOf("DESELECTED:Red", "SELECTED:Green", "action:comboBoxChanged"))
        }

        test("non-string items are displayed with toString, and the event item and getSelectedObjects are the items themselves") {
            val red = Color("Red", 0xFF0000)
            val green = Color("Green", 0x00FF00)
            onUiThreadGet {
                val comboBox = WComboBox(WDefaultComboBoxModel(arrayOf(red, green)))
                val items = mutableListOf<Any>()
                comboBox.addItemListener(WItemListener { e -> items += e.item })
                comboBox.setSelectedItem(Color("Green", 0x00FF00)) // An equal but different object selects the item in the list
                listOf(nativeItems(comboBox), comboBox.selectedItem, items.toList(), comboBox.getSelectedObjects().single() === green)
            } shouldContainExactly listOf(listOf("Red", "Green"), "Green", listOf(red, green), true)
        }

        test("when two combos share one model, selecting in one selects the same item in the other") {
            onUiThreadGet {
                val model = WDefaultComboBoxModel(arrayOf("Red", "Green", "Blue"))
                val first = WComboBox(model)
                val second = WComboBox(model)
                first.selectedIndex = 2
                second.addItem("White")
                listOf(second.selectedIndex, nativeSelectedIndex(second), first.itemCount)
            } shouldContainExactly listOf(2, 2, 4)
        }

        test("setModel replaces the items and selection without calling listeners, as in JComboBox, and no longer reflects changes of the old model") {
            onUiThreadGet {
                val old = WDefaultComboBoxModel(arrayOf("Red", "Green"))
                val comboBox = WComboBox(old)
                val events = comboBox.recorded()
                val replacement = WDefaultComboBoxModel(arrayOf("Spring", "Summer", "Autumn"))
                replacement.selectedItem = "Autumn"
                comboBox.model = replacement
                old.addElement("Blue")
                listOf(events.toList(), nativeItems(comboBox), comboBox.selectedItem, nativeSelectedIndex(comboBox))
            } shouldContainExactly listOf(emptyList<String>(), listOf("Spring", "Summer", "Autumn"), "Autumn", 2)
        }

        test("with an immutable model, addItem etc. throw UnsupportedOperationException") {
            onUiThread {
                val readOnly = object : WAbstractListModel<String>(), WComboBoxModel<String> {
                    override var selectedItem: Any? = null

                    override fun getSize(): Int = 2

                    override fun getElementAt(index: Int): String = listOf("Red", "Green")[index]
                }
                val comboBox = WComboBox(readOnly)
                check(comboBox.getItem(1) == "Green")
                shouldThrow<UnsupportedOperationException> { comboBox.addItem("Blue") }
                shouldThrow<UnsupportedOperationException> { comboBox.removeItemAt(0) }
                shouldThrow<UnsupportedOperationException> { comboBox.removeAllItems() }
            }
        }

        test("the default model is a WDefaultComboBoxModel, and addItem / removeItemAt and model operations are visible from both sides") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green"))
                val model = comboBox.model.shouldBeInstanceOf<WDefaultComboBoxModel<*>>()
                comboBox.addItem("Blue")
                comboBox.removeItemAt(0)
                @Suppress("UNCHECKED_CAST")
                (model as WDefaultComboBoxModel<String>).addElement("White")
                listOf(model.getSize(), comboBox.itemCount, comboBox.getItem(2), nativeItems(comboBox))
            } shouldContainExactly listOf(3, 3, "White", listOf("Green", "Blue", "White"))
        }

        test("a combo made from a List still selects nothing when items are added, and removing the selected item leaves it unselected") {
            onUiThreadGet {
                val comboBox = WComboBox()
                comboBox.addItem("Red")
                val afterAdd = comboBox.selectedIndex
                comboBox.addItem("Green")
                comboBox.selectedIndex = 1
                comboBox.removeItemAt(1)
                afterAdd to comboBox.selectedIndex
            } shouldBe (-1 to -1)
        }

        test("a combo made from an array or a model selects the adjacent item when the selected item is removed, as in JComboBox") {
            onUiThreadGet {
                val comboBox = WComboBox(arrayOf("Red", "Green", "Blue"))
                comboBox.selectedIndex = 2
                comboBox.removeItemAt(2)
                comboBox.selectedItem to nativeSelectedIndex(comboBox)
            } shouldBe ("Green" to 1)
        }

        test("removing an unselected item delivers an ActionEvent if nothing was selected before, as in JComboBox") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green"))
                val events = comboBox.recorded()
                comboBox.removeItemAt(0)
                events.toList()
            } shouldContainExactly listOf("action:comboBoxChanged")
        }

        test("when the model of a non-editable combo selects a value not in the list, it shows nothing, and shows it once an item with that value is added later") {
            onUiThreadGet {
                val model = WDefaultComboBoxModel(arrayOf("Red"))
                val comboBox = WComboBox(model)
                model.selectedItem = "Purple"
                val before = listOf(comboBox.selectedItem, comboBox.selectedIndex, nativeSelectedIndex(comboBox))
                model.addElement("Purple")
                before to listOf(comboBox.selectedItem, comboBox.selectedIndex, nativeSelectedIndex(comboBox))
            } shouldBe (listOf("Purple", -1, -1) to listOf("Purple", 1, 1))
        }

        test("when a value not in the list is selected in an editable combo before it is in a window, the string is shown once it is in the window") {
            val comboBox = onUiThreadGet {
                WComboBox(listOf("Red")).apply {
                    isEditable = true
                    setSelectedItem("Purple")
                }
            }
            onUiThreadGet { comboBox.selectedItem to comboBox.selectedIndex } shouldBe ("Purple" to -1)
            UiTestHarness.attachAndAwaitLoaded(comboBox)
            try {
                eventually(3.seconds) { onUiThreadGet { comboBox.text to comboBox.selectedItem } shouldBe ("Purple" to "Purple") }
                onUiThreadGet { comboBox.recorded().also { comboBox.addItem("Green") } }.shouldBeEmpty()
            } finally {
                UiTestHarness.detach(comboBox)
            }
        }

        test("getItemAt returns null when out of range, and getItem throws IndexOutOfBoundsException when out of range") {
            onUiThread {
                val comboBox = WComboBox(listOf("Red"))
                check(comboBox.getItemAt(1) == null)
                shouldThrow<IndexOutOfBoundsException> { comboBox.getItem(1) }
            }
            onUiThreadGet { WComboBox().getItemAt(0) }.shouldBeNull()
        }
    }
}
