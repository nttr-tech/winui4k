package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.attachAndAwaitLoaded
import com.appkitbox.winui4k.UiTestHarness.detach
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for replacing WItemsView items (setItems), enabling and subscribing to ItemInvoked, and the arrangement (layout).
 * ItemInvoked fires only on an actual click on an item, so this checks reading and writing the enabling, and that subscribing and unsubscribing go through without exceptions.
 * Selection is in WItemsViewTest.
 */
class WItemsViewItemsTest : FunSpec() {
    private fun containers(count: Int, prefix: String): List<WItemContainer> = (0 until count).map { WItemContainer(WLabel("$prefix$it")) }

    /** Shows [view], waits for Loaded, runs [block] on the UI thread and returns the result. Removes it when done. */
    private fun <T> shown(view: WItemsView, block: (WItemsView) -> T): T {
        attachAndAwaitLoaded(view)
        try {
            return onUiThreadGet { block(view) }
        } finally {
            detach(view)
        }
    }

    init {
        test("calling setItems again makes only the number of items in the new list selectable, and an empty list empties the selection") {
            val view = onUiThreadGet { WItemsView().also { it.selectionMode = ItemsViewSelectionMode.EXTENDED } }
            shown(view) {
                it.setItems(containers(3, "Old"))
                it.selectAll()
                val first = it.selectedIndices.toList()
                it.setItems(containers(2, "New"))
                it.selectAll()
                val second = it.selectedIndices.toList()
                it.setItems(emptyList())
                it.selectAll()
                Triple(first, second, it.isSelectionEmpty)
            } shouldBe Triple(listOf(0, 1, 2), listOf(0, 1), true)
        }

        test("isItemInvokedEnabled is false by default, and can be rewritten to true and false and read back") {
            onUiThreadGet {
                val view = WItemsView()
                val initial = view.isItemInvokedEnabled
                view.isItemInvokedEnabled = true
                val enabled = view.isItemInvokedEnabled
                view.isItemInvokedEnabled = false
                Triple(initial, enabled, view.isItemInvokedEnabled)
            } shouldBe Triple(false, true, false)
        }

        test("addItemInvokedListener can be removed with the same lambda, and removing twice or removing an unregistered lambda does nothing") {
            onUiThreadGet {
                val view = WItemsView()
                view.setItems(containers(2, "Item"))
                view.isItemInvokedEnabled = true
                val first: (Int) -> Unit = { }
                val second: (Int) -> Unit = { }
                view.addItemInvokedListener(first)
                view.addItemInvokedListener(second)
                view.removeItemInvokedListener(first)
                view.removeItemInvokedListener(first)
                view.removeItemInvokedListener { }
                view.removeItemInvokedListener(second)
                true
            } shouldBe true
        }

        test("while layout is null they line up vertically, setting a WUniformGridLayout lines them up horizontally too, and setting null goes back to a single vertical column") {
            val view = onUiThreadGet { WItemsView() }
            val items = onUiThreadGet { containers(3, "Item") }
            onUiThreadGet {
                view.setItems(items)
                view.width = 600.0
                view.height = 400.0
            }
            shown(view) {
                fun positions(): List<Pair<Int, Int>> {
                    it.updateLayout()
                    return items.map { item -> Xaml.positionInRoot(item.uiElement).let { p -> Math.round(p[0]).toInt() to Math.round(p[1]).toInt() } }
                }
                val stacked = positions()
                it.layout = WUniformGridLayout().apply {
                    minItemWidth = 100.0
                    minItemHeight = 40.0
                    orientation = Orientation.HORIZONTAL
                }
                val grid = positions()
                it.layout = null
                val back = positions()
                listOf(
                    stacked.map { p -> p.first }.distinct().size to stacked.map { p -> p.second }.distinct().size,
                    grid.map { p -> p.first }.distinct().size to grid.map { p -> p.second }.distinct().size,
                    back.map { p -> p.first }.distinct().size to back.map { p -> p.second }.distinct().size,
                )
            } shouldBe listOf(1 to 3, 3 to 1, 1 to 3)
        }
    }
}
