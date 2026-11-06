package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for the defaults, label and hierarchy (addItem) of [WNavigationViewItem], and reading and writing the items and selection of [WNavigationView].
 * The defaults are as defined by WinUI's NavigationViewItem (SelectsOnInvoked = true, IsExpanded = false).
 * Click notifications are covered by [WNavigationViewTest].
 */
@Tags("E2E")
class WNavigationViewItemTest : FunSpec() {
    private fun navigationView(vararg items: WNavigationViewItem): WNavigationView = WNavigationView().also { view ->
        view.paneDisplayMode = NavigationViewPaneDisplayMode.LEFT
        items.forEach(view::addItem)
    }

    init {
        test("by default, the label is empty, it is selected on click, it is not expanded, and it has no icon") {
            onUiThreadGet {
                val item = WNavigationViewItem()
                listOf(item.text, item.selectsOnInvoked, item.isExpanded, item.icon)
            } shouldBe listOf("", true, false, null)
        }

        test("the constructor's label and icon can be read back, and text can be rewritten (Japanese too)") {
            onUiThreadGet {
                val item = WNavigationViewItem("Home", Symbol.HOME)
                val first = item.text to item.icon
                item.text = "\u8a2d\u5b9a Settings"
                Triple(first, item.text, item.actionCommand)
            } shouldBe Triple("Home" to Symbol.HOME, "\u8a2d\u5b9a Settings", "\u8a2d\u5b9a Settings")
        }

        test("selectsOnInvoked and isExpanded can be set and read back") {
            onUiThreadGet {
                val item = WNavigationViewItem("Heading")
                item.selectsOnInvoked = false
                item.isExpanded = true
                item.selectsOnInvoked to item.isExpanded
            } shouldBe (false to true)
        }

        test("addItem gives it child items, nested down to grandchildren") {
            onUiThread {
                val root = WNavigationViewItem("root")
                val child = WNavigationViewItem("child")
                val grandchild = WNavigationViewItem("grandchild")
                child.addItem(grandchild)
                root.addItem(child)
                root.childItems shouldContainExactly listOf(child)
                child.childItems shouldContainExactly listOf(grandchild)
                grandchild.childItems shouldContainExactly emptyList()
            }
        }

        test("selfAndDescendants lists itself, its children (in the order added) and their descendants, depth first") {
            onUiThread {
                val root = WNavigationViewItem("root")
                val a = WNavigationViewItem("a")
                val b = WNavigationViewItem("b")
                val a1 = WNavigationViewItem("a1")
                root.addItem(a)
                root.addItem(b)
                a.addItem(a1)
                root.selfAndDescendants().map { it.text }.toList() shouldContainExactly listOf("root", "a", "a1", "b")
            }
        }

        test("selectedItem of a navigation view with nothing selected is null") {
            onUiThreadGet { navigationView(WNavigationViewItem("A")).selectedItem }.shouldBeNull()
        }

        test("selectedItem reads back menu items and footer items in the order set, and null deselects") {
            onUiThread {
                val menu = WNavigationViewItem("Menu")
                val footer = WNavigationViewItem("Footer")
                val view = navigationView(menu)
                view.addFooterItem(footer)
                view.selectedItem = footer
                view.selectedItem shouldBeSameInstanceAs footer
                view.selectedItem = menu
                view.selectedItem shouldBeSameInstanceAs menu
                view.selectedItem = null
                view.selectedItem.shouldBeNull()
            }
        }

        test("nested child items can also be looked up as selectedItem") {
            onUiThread {
                val root = WNavigationViewItem("root")
                val child = WNavigationViewItem("child")
                root.addItem(child)
                val view = navigationView(root)
                root.isExpanded = true
                view.selectedItem = child
                view.selectedItem shouldBeSameInstanceAs child
            }
        }

        test("rewriting the label of the selected item still reads it as the same item") {
            onUiThread {
                val item = WNavigationViewItem("before")
                val view = navigationView(item)
                view.selectedItem = item
                item.text = "after"
                view.selectedItem shouldBeSameInstanceAs item
                view.selectedItem?.text shouldBe "after"
            }
        }
    }
}
