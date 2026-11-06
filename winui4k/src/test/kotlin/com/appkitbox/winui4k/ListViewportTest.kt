package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the JList-style display API of WList (ListView) and WListBox (ListBox)
 * (ensureIndexIsVisible / visibleRowCount / first / lastVisibleIndex / locationToIndex / indexToLocation / getCellBounds)
 * by actually showing them. Expected values come from the behavior of Swing's JList and the positions of the shown text (TextBlock).
 */
class ListViewportTest : FunSpec() {
    /** A thin interface to examine the two kinds of lists in the same tests. */
    private class Kind(val name: String, val create: (List<String>) -> WControl)

    private val kinds = listOf(
        Kind("WList") { WList(it) },
        Kind("WListBox") { WListBox(it) },
    )

    private fun WControl.ensureVisible(index: Int) = if (this is WList) ensureIndexIsVisible(index) else (this as WListBox).ensureIndexIsVisible(index)

    private var WControl.rows: Int
        get() = if (this is WList) visibleRowCount else (this as WListBox).visibleRowCount
        set(value) = if (this is WList) visibleRowCount = value else (this as WListBox).visibleRowCount = value

    private val WControl.first: Int get() = if (this is WList) firstVisibleIndex else (this as WListBox).firstVisibleIndex
    private val WControl.last: Int get() = if (this is WList) lastVisibleIndex else (this as WListBox).lastVisibleIndex

    private fun WControl.indexAt(x: Double, y: Double): Int = if (this is WList) locationToIndex(x, y) else (this as WListBox).locationToIndex(x, y)

    private fun WControl.indexAt(point: WPoint): Int = if (this is WList) locationToIndex(point) else (this as WListBox).locationToIndex(point)

    private fun WControl.location(index: Int): WPoint? = if (this is WList) indexToLocation(index) else (this as WListBox).indexToLocation(index)

    private fun WControl.bounds(index0: Int, index1: Int): WRectangle? =
        if (this is WList) getCellBounds(index0, index1) else (this as WListBox).getCellBounds(index0, index1)

    private fun items(count: Int) = (0 until count).map { "Item $it" }

    /** The top-left and size of the TextBlock with text [text] in [list] (list coordinates, DIP). */
    private fun textBox(list: WControl, text: String): List<Double> {
        val block = checkNotNull(tableTreeFindText(list.dependencyObject, text)) { "TextBlock not found: $text" }
        return try {
            val (width, height) = VisualHitTest.actualSizeOf(block)
            val (x, y) = VisualHitTest.transform(block, list.uiElement, 0.0, 0.0)
            listOf(x, y, width, height)
        } finally {
            block.release()
        }
    }

    /** Shows [list] with height [height], runs [block] on the UI thread, and then removes it. */
    private fun <T> shown(list: WControl, height: Double = 200.0, block: (WControl) -> T): T {
        onUiThreadGet { list.height = height }
        UiTestHarness.attachAndAwaitLoaded(list)
        try {
            return onUiThreadGet {
                list.updateLayout()
                block(list)
            }
        } finally {
            UiTestHarness.detach(list)
        }
    }

    /** Waits for layout (up to 5 seconds) with [list] shown until [condition] holds. */
    private fun awaitLayout(list: WControl, condition: (WControl) -> Boolean): Boolean {
        repeat(100) {
            if (onUiThreadGet { list.updateLayout().let { condition(list) } }) return true
            Thread.sleep(50)
        }
        return false
    }

    init {
        for (kind in kinds) {
            test("${kind.name}: visibleRowCount defaults to 8, setting it makes the height rows × 40, and negative values become 0") {
                onUiThreadGet {
                    val list = kind.create(items(3))
                    val initial = list.rows to list.height.isNaN()
                    list.rows = 5
                    val five = list.rows to list.height
                    list.rows = 3
                    val three = list.rows to list.height
                    list.rows = -2
                    listOf(initial, five, three, list.rows to list.height)
                } shouldBe listOf(8 to true, 5 to 200.0, 3 to 120.0, 0 to 0.0)
            }

            test("${kind.name}: if the user specified a height, setting visibleRowCount does not change the height") {
                onUiThreadGet {
                    val list = kind.create(items(3))
                    list.height = 150.0
                    list.rows = 5
                    list.rows to list.height
                } shouldBe (5 to 150.0)
            }

            test("${kind.name}: for a list not shown, first / lastVisibleIndex and locationToIndex are -1, and indexToLocation is null") {
                onUiThreadGet {
                    val list = kind.create(items(3))
                    listOf(list.first, list.last, list.indexAt(10.0, 10.0), list.location(0))
                } shouldBe listOf(-1, -1, -1, null)
            }

            test("${kind.name}: for an empty list, locationToIndex is -1, and first / lastVisibleIndex are -1 too") {
                shown(onUiThreadGet { kind.create(emptyList()) }) { list ->
                    listOf(list.indexAt(10.0, 10.0), list.first, list.last)
                } shouldBe listOf(-1, -1, -1)
            }

            test("${kind.name}: locationToIndex returns the element under the point, and the closest element outside the elements") {
                shown(onUiThreadGet { kind.create(items(3)) }) { list ->
                    val (x, y, width, height) = textBox(list, "Item 1")
                    val point = WPoint((x + width / 2).toInt(), (y + height / 2).toInt())
                    listOf(
                        list.indexAt(x + width / 2, y + height / 2),
                        list.indexAt(point),
                        // The horizontal position is not considered
                        list.indexAt(-50.0, y + height / 2),
                        // Above the top is the first, and below the last element (where there are no elements) is the last element
                        list.indexAt(10.0, -30.0),
                        list.indexAt(10.0, 190.0),
                    )
                } shouldBe listOf(1, 1, 1, 0, 2)
            }

            test("${kind.name}: indexToLocation and getCellBounds return the item bounds enclosing the text, and null when out of range") {
                shown(onUiThreadGet { kind.create(items(3)) }) { list ->
                    val text0 = textBox(list, "Item 0")
                    val text2 = textBox(list, "Item 2")
                    val location0 = checkNotNull(list.location(0))
                    val location1 = checkNotNull(list.location(1))
                    val all = checkNotNull(list.bounds(2, 0))
                    val rowHeight = location1.y - location0.y
                    listOf(
                        // The item bounds enclose the text
                        location0.y <= text0[1] && text0[1] + text0[3] <= location0.y + rowHeight,
                        // The bounds of the three items go from the top of the first to the bottom of the last item (in either order)
                        all == list.bounds(0, 2),
                        all.y == location0.y && all.height == 3 * rowHeight,
                        text2[1] + text2[3] <= all.y + all.height,
                        rowHeight > 0,
                        list.location(3),
                        list.location(-1),
                        list.bounds(0, 3),
                    )
                } shouldBe listOf(true, true, true, true, true, null, null, null)
            }

            test("${kind.name}: first / lastVisibleIndex are the elements within the height, and ensureIndexIsVisible scrolls to that element") {
                val list = onUiThreadGet { kind.create(items(100)).also { it.height = 200.0 } }
                UiTestHarness.attachAndAwaitLoaded(list)
                try {
                    onUiThreadGet {
                        list.updateLayout()
                        val rowHeight = checkNotNull(list.location(1)).y - checkNotNull(list.location(0)).y
                        // The rows within a height of 200 (counted if even partly visible)
                        val expectedLast = Math.ceil(200.0 / rowHeight).toInt() - 1
                        val before = listOf(list.first, list.last == expectedLast, list.location(99))
                        // Out of range does nothing (and does not throw)
                        list.ensureVisible(-1)
                        list.ensureVisible(100)
                        list.ensureVisible(60)
                        before
                    } shouldBe listOf(0, true, null)
                    awaitLayout(list) { it.first in 1..60 && it.last >= 60 } shouldBe true
                    onUiThreadGet { list.indexAt(10.0, 10.0) == list.first && list.location(60) != null } shouldBe true
                } finally {
                    UiTestHarness.detach(list)
                }
            }
        }
    }
}
