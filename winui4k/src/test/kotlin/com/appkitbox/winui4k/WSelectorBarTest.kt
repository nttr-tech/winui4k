package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests verifying WSelectorBar's item management, selection, and SelectionChanged, plus WSelectorBarItem's text. */
class WSelectorBarTest : FunSpec() {
    init {
        test("adding items via addItem is reflected in itemCount") {
            onUiThreadGet {
                val selectorBar = WSelectorBar()
                selectorBar.addItem(WSelectorBarItem("A"))
                selectorBar.addItem(WSelectorBarItem("B"))
                selectorBar.itemCount
            } shouldBe 2
        }

        test("text returns exactly the value that was set") {
            onUiThreadGet {
                val item = WSelectorBarItem("old")
                item.text = "new"
                item.text
            } shouldBe "new"
        }

        test("selectedIndex starts out -1 (unselected) and returns exactly the value that was set") {
            val (before, after) = onUiThreadGet {
                val selectorBar = WSelectorBar()
                selectorBar.addItem(WSelectorBarItem("A"))
                selectorBar.addItem(WSelectorBarItem("B"))
                val before = selectorBar.selectedIndex
                selectorBar.selectedIndex = 1
                before to selectorBar.selectedIndex
            }
            before shouldBe -1
            after shouldBe 1
        }

        test("the SelectionChanged listener fires with the selected index on every change to selectedIndex") {
            val received = mutableListOf<Int>()
            onUiThread {
                val selectorBar = WSelectorBar()
                selectorBar.addItem(WSelectorBarItem("A"))
                selectorBar.addItem(WSelectorBarItem("B"))
                selectorBar.addSelectionListener { index -> received += index }
                selectorBar.selectedIndex = 0
                selectorBar.selectedIndex = 1
            }
            received shouldBe listOf(0, 1)
        }
    }
}
