package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Tests verifying WListBox's item management, single/multiple selection, and SelectionChanged listener. */
class WListBoxTest : FunSpec() {
    init {
        test("the items passed to the constructor can be retrieved via itemCount and getItem") {
            val (count, item) = onUiThreadGet {
                val listBox = WListBox(listOf("Spring", "Summer", "Fall", "Winter"))
                listBox.itemCount to listBox.getItem(3)
            }
            count shouldBe 4
            item shouldBe "Winter"
        }

        test("setting selectedIndex makes selectedItem return the matching string") {
            onUiThreadGet {
                val listBox = WListBox(listOf("Spring", "Summer"))
                listBox.selectedIndex = 0
                listBox.selectedItem
            } shouldBe "Spring"
        }

        test("the ListSelectionListener fires on every selection change") {
            var count = 0
            onUiThread {
                val listBox = WListBox(listOf("Spring", "Summer"))
                listBox.addListSelectionListener { count++ }
                listBox.selectedIndex = 0
                listBox.selectedIndex = 1
            }
            count shouldBe 2
        }

        test("selectAll in MULTIPLE mode puts every item into selectedItems") {
            onUiThreadGet {
                val listBox = WListBox(listOf("Spring", "Summer", "Fall"))
                listBox.selectionMode = SelectionMode.MULTIPLE
                listBox.selectAll()
                listBox.selectedItems
            } shouldContainExactly listOf("Spring", "Summer", "Fall")
        }

        test("selectionMode returns exactly the value that was set") {
            onUiThreadGet {
                val listBox = WListBox()
                listBox.selectionMode = SelectionMode.EXTENDED
                listBox.selectionMode
            } shouldBe SelectionMode.EXTENDED
        }

        context("Swing compatibility") {
            test("listSelectionListeners returns the added WListSelectionListeners in the order added, and removed ones are excluded") {
                onUiThreadGet {
                    val list = WListBox(arrayOf("a", "b"))
                    val first = WListSelectionListener { }
                    val second = WListSelectionListener { }
                    list.addListSelectionListener(first)
                    list.addListSelectionListener(second)
                    val both = list.listSelectionListeners.toList()
                    list.removeListSelectionListener(first)
                    listOf(both == listOf(first, second), list.listSelectionListeners.toList() == listOf(second))
                } shouldBe listOf(true, true)
            }
        }

        context("Boundaries and exceptions") {
            test("scrollIntoView throws IndexOutOfBoundsException for an out-of-range position, and ensureIndexIsVisible does nothing") {
                onUiThreadGet {
                    val list = WListBox(listOf("Spring", "Summer"))
                    list.scrollIntoView(1)
                    list.ensureIndexIsVisible(5)
                    runCatching { list.scrollIntoView(2) }.exceptionOrNull()
                }.shouldBeInstanceOf<IndexOutOfBoundsException>()
            }
        }
    }
}
