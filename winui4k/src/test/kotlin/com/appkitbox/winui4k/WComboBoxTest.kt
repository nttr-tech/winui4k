package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** Tests verifying WComboBox's item management, selection, and SelectionChanged listener. */
class WComboBoxTest : FunSpec() {
    init {
        test("the items passed to the constructor can be retrieved via itemCount and getItem") {
            val (count, first, second) = onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue"))
                Triple(comboBox.itemCount, comboBox.getItem(0), comboBox.getItem(1))
            }
            count shouldBe 3
            first shouldBe "Red"
            second shouldBe "Green"
        }

        test("the initial state is unselected (selectedIndex = -1, selectedItem = null)") {
            val (index, item) = onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green"))
                comboBox.selectedIndex to comboBox.selectedItem
            }
            index shouldBe -1
            item.shouldBeNull()
        }

        test("setting selectedIndex makes selectedItem return the matching string") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue"))
                comboBox.selectedIndex = 2
                comboBox.selectedItem
            } shouldBe "Blue"
        }

        test("the ListSelectionListener fires on every selection change") {
            var count = 0
            onUiThread {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue"))
                comboBox.addListSelectionListener { count++ }
                comboBox.selectedIndex = 0
                comboBox.selectedIndex = 1
            }
            count shouldBe 2
        }

        test("removeItem shrinks the item count and shifts later items down") {
            val (count, item) = onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green", "Blue"))
                comboBox.removeItem(1)
                comboBox.itemCount to comboBox.getItem(1)
            }
            count shouldBe 2
            item shouldBe "Blue"
        }

        test("removeAllItems clears every item and also clears the selection") {
            val (count, index) = onUiThreadGet {
                val comboBox = WComboBox(listOf("Red", "Green"))
                comboBox.selectedIndex = 0
                comboBox.removeAllItems()
                comboBox.itemCount to comboBox.selectedIndex
            }
            count shouldBe 0
            index shouldBe -1
        }

        test("isEditable = true allows setting text to a string not among the items") {
            onUiThreadGet {
                val comboBox = WComboBox(listOf("Red"))
                comboBox.isEditable = true
                comboBox.text = "Freeform input"
                comboBox.text
            } shouldBe "Freeform input"
        }
    }
}
