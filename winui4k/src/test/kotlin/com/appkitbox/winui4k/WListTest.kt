package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/** Tests verifying WList (ListView)'s item management, selection, and SelectionChanged listener. */
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
    }
}
