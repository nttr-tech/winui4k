package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.attachAndAwaitLoaded
import com.appkitbox.winui4k.UiTestHarness.detach
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests for WItemsView's selection API (in the form of Swing's JList) and WItemContainer.isSelected. */
class WItemsViewTest : FunSpec() {
    /** Creates a WItemsView with 5 items (call on the UI thread). */
    private fun createView(mode: ItemsViewSelectionMode): WItemsView {
        val view = WItemsView()
        view.selectionMode = mode
        view.setItems((0 until 5).map { WItemContainer(WLabel("Item $it")) })
        return view
    }

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
        test("while nothing is selected, selectedIndex is -1, selectedIndices is empty and isSelectionEmpty is true") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            shown(view) {
                listOf(it.selectedIndex, it.minSelectionIndex, it.maxSelectionIndex, it.selectedIndices.toList(), it.isSelectionEmpty)
            } shouldBe listOf(-1, -1, -1, emptyList<Int>(), true)
        }

        test("setSelectedIndex selects only one row, specifying another row deselects the previous one, and -1 empties the selection") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            shown(view) {
                it.selectedIndex = 3
                val first = it.selectedIndices.toList()
                it.selectedIndex = 1
                val second = it.selectedIndices.toList()
                it.selectedIndex = -1
                Triple(first, second, it.isSelectionEmpty)
            } shouldBe Triple(listOf(3), listOf(1), true)
        }

        test("setSelectionInterval selects only index0..index1 (even reversed), addSelectionInterval adds, and removeSelectionInterval removes") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            shown(view) {
                it.setSelectionInterval(3, 1)
                val set = it.selectedIndices.toList()
                it.addSelectionInterval(4, 4)
                val added = it.selectedIndices.toList()
                it.removeSelectionInterval(2, 3)
                val removed = it.selectedIndices.toList()
                listOf(set, added, removed, listOf(it.minSelectionIndex, it.maxSelectionIndex))
            } shouldBe listOf(listOf(1, 2, 3), listOf(1, 2, 3, 4), listOf(1, 4), listOf(1, 4))
        }

        test("isSelectedIndex is true only for selected rows, and false for out-of-range rows") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            shown(view) {
                it.setSelectionInterval(1, 2)
                listOf(0, 1, 2, 3, -1, 99).map { index -> it.isSelectedIndex(index) }
            } shouldBe listOf(false, true, true, false, false, false)
        }

        test("setSelectedIndices reselects only the given rows, selectAll selects all rows, and clearSelection empties it") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            shown(view) {
                it.selectedIndices = intArrayOf(0, 4)
                val picked = it.selectedIndices.toList()
                it.selectAll()
                val all = it.selectedIndices.toList()
                it.clearSelection()
                Triple(picked, all, it.isSelectionEmpty)
            } shouldBe Triple(listOf(0, 4), listOf(0, 1, 2, 3, 4), true)
        }

        test("in single selection, setSelectionInterval and addSelectionInterval select only the one row index1 (same as Swing's SINGLE_SELECTION)") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.SINGLE) }
            shown(view) {
                it.setSelectionInterval(0, 2)
                val set = it.selectedIndices.toList()
                it.addSelectionInterval(3, 4)
                set to it.selectedIndices.toList()
            } shouldBe (listOf(2) to listOf(4))
        }

        test("row numbers outside the items are ignored as in Swing's JList, and intervals affect only rows in range") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            shown(view) {
                it.selectedIndex = 1
                it.selectedIndex = 5
                it.selectedIndex = -2
                val single = it.selectedIndices.toList()
                it.selectedIndices = intArrayOf(0, 7, 2, -3)
                val indices = it.selectedIndices.toList()
                it.setSelectionInterval(3, 9)
                val interval = it.selectedIndices.toList()
                it.addSelectionInterval(-5, 0)
                val added = it.selectedIndices.toList()
                it.removeSelectionInterval(4, 8)
                val removed = it.selectedIndices.toList()
                it.setSelectionInterval(5, 9)
                listOf(single, indices, interval, added, removed, it.selectedIndices.toList())
            } shouldBe listOf(listOf(1), listOf(0, 2), listOf(3, 4), listOf(0, 3, 4), listOf(0, 3), listOf(0, 3))
        }

        test("in single selection, setSelectionInterval / addSelectionInterval with an out-of-range index1 do nothing") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.SINGLE) }
            shown(view) {
                it.selectedIndex = 2
                it.setSelectionInterval(0, 5)
                it.addSelectionInterval(1, 6)
                it.selectedIndices.toList()
            } shouldBe listOf(2)
        }

        test("setSelectionInterval with either row number -1 does nothing (as in Swing)") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            shown(view) {
                it.selectedIndex = 2
                it.setSelectionInterval(-1, 3)
                it.addSelectionInterval(0, -1)
                it.selectedIndices.toList()
            } shouldBe listOf(2)
        }

        test("WItemContainer.isSelected follows the selection, and assigning it also changes the selection") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            val containers = onUiThreadGet { (0 until 3).map { WItemContainer(WLabel("c$it")) }.also { view.setItems(it) } }
            shown(view) {
                it.selectedIndex = 1
                val viaView = containers.map { c -> c.isSelected }
                containers[2].isSelected = true
                viaView to listOf(it.selectedIndices.toList(), containers.map { c -> c.isSelected })
            } shouldBe (listOf(false, true, false) to listOf(listOf(1, 2), listOf(false, true, true)))
        }

        test("addListSelectionListener is called once per change, with firstIndex..lastIndex of the changed range and source = the view") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            val events = mutableListOf<Triple<Int, Int, Boolean>>()
            val sources = mutableListOf<Any>()
            shown(view) {
                it.addListSelectionListener { e ->
                    events += Triple(e.firstIndex, e.lastIndex, e.valueIsAdjusting)
                    sources += e.source
                }
                it.setSelectionInterval(1, 2) // Changed range 1..2
                it.addSelectionInterval(4, 4) // 4..4
                it.setSelectionInterval(0, 1) // {1,2,4} -> {0,1}: 0 is added and 2 and 4 are removed, so 0..4
                it.clearSelection() // {0,1} -> {} : 0..1
                it.clearSelection() // Unchanged, so no notification
            }
            events shouldBe listOf(Triple(1, 2, false), Triple(4, 4, false), Triple(0, 4, false), Triple(0, 1, false))
            sources.all { it === view } shouldBe true
        }

        test("a listener removed with removeListSelectionListener is not called, and the rest are called in the order added") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            val log = mutableListOf<String>()
            shown(view) {
                val removed = WListSelectionListener { log += "removed" }
                val a = WListSelectionListener { log += "a" }
                val b = WListSelectionListener { log += "b" }
                it.addListSelectionListener(a)
                it.addListSelectionListener(removed)
                it.addListSelectionListener(b)
                it.removeListSelectionListener(removed)
                it.selectedIndex = 0
            }
            log shouldBe listOf("a", "b")
        }

        test("assigning WItemContainer.isSelected also notifies the selection change") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            val containers = onUiThreadGet { (0 until 3).map { WItemContainer(WLabel("c$it")) }.also { view.setItems(it) } }
            val events = mutableListOf<Pair<Int, Int>>()
            shown(view) {
                it.addListSelectionListener { e -> events += e.firstIndex to e.lastIndex }
                containers[2].isSelected = true
            }
            events shouldBe listOf(2 to 2)
        }

        test("ensureIndexIsVisible works without exceptions while shown, and does nothing for out-of-range row numbers (as in JList)") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.SINGLE) }
            shown(view) {
                it.ensureIndexIsVisible(4)
                it.ensureIndexIsVisible(5)
                it.ensureIndexIsVisible(-1)
                it.selectedIndex
            } shouldBe -1
        }

        test("specifying a selection on a view whose selection mode is NONE leaves the selection empty") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.NONE) }
            shown(view) {
                it.selectedIndex = 1
                it.isSelectionEmpty
            } shouldBe true
        }

        test("when the selection disappears on the native side with setItems, the listeners are notified of the vanished range") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            val events = mutableListOf<Pair<Int, Int>>()
            shown(view) {
                it.setSelectionInterval(1, 3)
                it.addListSelectionListener { e -> events += e.firstIndex to e.lastIndex }
                it.setItems((0 until 2).map { n -> WItemContainer(WLabel("New $n")) })
                it.isSelectionEmpty
            } shouldBe true
            events shouldBe listOf(1 to 3)
        }

        test("selection notifications after setItems do not compare with the selection of the items before the replacement") {
            val view = onUiThreadGet { createView(ItemsViewSelectionMode.EXTENDED) }
            val events = mutableListOf<Pair<Int, Int>>()
            shown(view) {
                it.addListSelectionListener { e -> events += e.firstIndex to e.lastIndex }
                it.selectedIndex = 3
                it.setItems((0 until 5).map { n -> WItemContainer(WLabel("New $n")) })
                val afterItems = it.selectedIndices.toSet()
                events.clear()
                it.selectedIndex = 0
                // Only the change from the selection on the new items (afterItems) to {0} is notified
                val changed = (afterItems - 0) + (setOf(0) - afterItems)
                it.selectedIndices.toList() to listOf(changed.min() to changed.max())
            } shouldBe (listOf(0) to events.toList())
        }
    }
}
