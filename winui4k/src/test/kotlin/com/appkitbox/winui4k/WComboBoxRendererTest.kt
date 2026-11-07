package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for WComboBox.renderer (a function that turns items into display strings, like JComboBox.setRenderer) with the strings of the native ComboBox.Items.
 */
@Tags("E2E")
class WComboBoxRendererTest : FunSpec() {
    /** A non-string item with a display name and a value. */
    private data class Price(val name: String, val yen: Int)

    /** The strings of the native ComboBox.Items. */
    private fun nativeItems(comboBox: WComboBox): List<String?> {
        val itemsControl = comboBox.inspectable.queryInterface(XamlInterop.IID_IItemsControl)
        val items = itemsControl.getPtr(XamlInterop.IItemsControl_get_Items)
        itemsControl.release()
        val vector = items.queryInterface(FoundationInterop.IID_IVector_Object)
        items.release()
        return try {
            (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index ->
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
    }

    private fun priceModel() = WDefaultComboBoxModel(arrayOf(Price("apple", 120), Price("orange", 80)))

    init {
        context("Defaults") {
            test("renderer is null by default, and the items' toString() is shown") {
                onUiThreadGet {
                    val combo = WComboBox(listOf("a", "b"))
                    combo.renderer to nativeItems(combo)
                } shouldBe (null to listOf("a", "b"))
            }
        }

        context("Reading and writing") {
            test("setting renderer recreates the native items with its text, and getItem and selectedItemText also return that text") {
                onUiThreadGet {
                    val combo = WComboBox(priceModel())
                    combo.renderer = { value -> (value as Price).let { "${it.name} (${it.yen} yen)" } }
                    nativeItems(combo) + listOf(combo.getItem(0), combo.getItem(1), combo.selectedItemText)
                } shouldContainExactly listOf("apple (120 yen)", "orange (80 yen)", "apple (120 yen)", "orange (80 yen)", "apple (120 yen)")
            }

            test("items added to the model and items replaced after setting renderer are also shown with its strings") {
                onUiThreadGet {
                    val model = priceModel()
                    val combo = WComboBox(model)
                    combo.renderer = { value -> (value as Price).name.uppercase() + "!" }
                    model.addElement(Price("grape", 300))
                    nativeItems(combo)
                } shouldContainExactly listOf("APPLE!", "ORANGE!", "GRAPE!")
            }

            test("setting renderer does not change the selection; selectedItem stays the item and selectedItemText becomes the renderer's text") {
                onUiThreadGet {
                    val combo = WComboBox(listOf("a", "b"))
                    combo.selectedIndex = 1
                    combo.renderer = { value -> "[$value]" }
                    listOf(combo.selectedIndex, combo.selectedItem, combo.selectedItemText, combo.getItem(1))
                } shouldContainExactly listOf(1, "b", "[b]", "[b]")
            }

            test("even with renderer set, selectedItem and getItemAt return the item itself, not the displayed text (as with JComboBox)") {
                onUiThreadGet {
                    val model = priceModel()
                    val combo = WComboBox(model)
                    combo.renderer = { value -> (value as Price).name }
                    combo.selectedIndex = 1
                    listOf(combo.selectedItem, combo.getItemAt(0), combo.getItemAt(2))
                } shouldContainExactly listOf(Price("Orange", 80), Price("Apple", 120), null)
            }

            test("selectedItemText of an editable combo box with a value not in the list selected is toString() without the renderer, and null if nothing is selected") {
                onUiThreadGet {
                    val combo = WComboBox(priceModel())
                    combo.isEditable = true
                    combo.renderer = { value -> (value as Price).name }
                    combo.setSelectedItem("banana")
                    val custom = listOf(combo.selectedIndex, combo.selectedItem, combo.selectedItemText)
                    combo.selectedIndex = -1
                    custom + listOf(combo.selectedItem, combo.selectedItemText)
                } shouldContainExactly listOf(-1, "banana", "banana", null, null)
            }

            test("resetting it to null returns to showing toString()") {
                onUiThreadGet {
                    val combo = WComboBox(listOf("a", "b"))
                    combo.renderer = { value -> "[$value]" }
                    combo.renderer = null
                    nativeItems(combo)
                } shouldContainExactly listOf("a", "b")
            }
        }

        context("Events") {
            test("setting renderer does not call ItemListeners or ActionListeners") {
                onUiThreadGet {
                    val combo = WComboBox(listOf("a", "b"))
                    combo.selectedIndex = 0
                    val events = mutableListOf<String>()
                    combo.addItemListener(WItemListener { events += "item" })
                    combo.addActionListener(WActionListener { events += "action" })
                    combo.renderer = { value -> "[$value]" }
                    events.toList()
                } shouldBe emptyList()
            }
        }
    }
}
