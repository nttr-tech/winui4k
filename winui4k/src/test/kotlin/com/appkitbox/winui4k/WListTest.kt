package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/** Tests verifying WList (ListView)'s item management, selection, and SelectionChanged listener. */
@Tags("E2E")
class WListTest : FunSpec() {
    init {
        test("the items passed to the constructor can be retrieved via itemCount and getItem") {
            val (count, item) = onUiThreadGet {
                val list = WList(listOf("Mon", "Tue", "Wed"))
                list.itemCount to list.getItem(2)
            }
            count shouldBe 3
            item shouldBe "Wed"
        }

        test("setting selectedIndex makes selectedItem return the matching string") {
            onUiThreadGet {
                val list = WList(listOf("Mon", "Tue", "Wed"))
                list.selectedIndex = 1
                list.selectedItem
            } shouldBe "Tue"
        }

        test("the ListSelectionListener fires on every selection change") {
            var count = 0
            onUiThread {
                val list = WList(listOf("Mon", "Tue", "Wed"))
                list.addListSelectionListener { count++ }
                list.selectedIndex = 0
                list.selectedIndex = 2
            }
            count shouldBe 2
        }

        test("selectionMode and isItemClickEnabled return exactly the values that were set") {
            val (mode, clickEnabled) = onUiThreadGet {
                val list = WList()
                list.selectionMode = ListViewSelectionMode.MULTIPLE
                list.isItemClickEnabled = true
                list.selectionMode to list.isItemClickEnabled
            }
            mode shouldBe ListViewSelectionMode.MULTIPLE
            clickEnabled.shouldBeTrue()
        }

        test("addItem appends the item to the end") {
            val (count, last) = onUiThreadGet {
                val list = WList(listOf("Mon"))
                list.addItem("Added")
                list.itemCount to list.getItem(1)
            }
            count shouldBe 2
            last shouldBe "Added"
        }

        context("Swing compatibility") {
            test("listSelectionListeners returns the added WListSelectionListeners in the order added, and removed ones are excluded") {
                onUiThreadGet {
                    val list = WList(arrayOf("a", "b"))
                    val first = WListSelectionListener { }
                    val second = WListSelectionListener { }
                    list.addListSelectionListener(first)
                    list.addListSelectionListener(second)
                    val both = list.listSelectionListeners.toList()
                    list.removeListSelectionListener(first)
                    listOf(both == listOf(first, second), list.listSelectionListeners.toList() == listOf(second))
                } shouldBe listOf(true, true)
            }

            test("isItemClickEnabled returns the set value, and Kotlin (String) -> Unit item click listeners can be added and removed") {
                onUiThreadGet {
                    val list = WList(listOf("Mon", "Tue"))
                    val before = list.isItemClickEnabled
                    list.isItemClickEnabled = true
                    val listener: (String) -> Unit = { }
                    list.addItemClickListener(listener)
                    list.removeItemClickListener(listener)
                    list.removeItemClickListener(listener) // Removing a listener never added does nothing
                    listOf(before, list.isItemClickEnabled)
                } shouldBe listOf(false, true)
            }
        }
    }
}
