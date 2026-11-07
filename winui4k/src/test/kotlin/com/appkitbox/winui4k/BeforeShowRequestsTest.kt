package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.TabularInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that check operations requested before being added to the screen (before showing, or right after being added to a window and before the first layout)
 * take effect when shown, as in Swing. The expected values are what Swing shows when called in the same order (the selected row appears selected,
 * the row to show is visible, etc.).
 */
@Tags("E2E")
class BeforeShowRequestsTest : FunSpec() {
    private fun items(count: Int) = (0 until count).map { "Item ${it + 1}" }

    /** Runs layouts and waits until [condition] holds (up to 5 seconds). true if it holds. */
    private fun awaitUi(condition: () -> Boolean): Boolean {
        repeat(100) {
            if (onUiThreadGet(condition)) return true
            Thread.sleep(50)
        }
        return false
    }

    init {
        context("WList.ensureIndexIsVisible") {
            test("when called before showing, the element is visible when shown") {
                val list = onUiThreadGet {
                    WList(items(100)).also {
                        it.height = 200.0
                        it.ensureIndexIsVisible(42)
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(list)
                try {
                    awaitUi { list.firstVisibleIndex in 1..42 && list.lastVisibleIndex >= 42 } shouldBe true
                } finally {
                    UiTestHarness.detach(list)
                }
            }

            test("even when called right after being added to a shown window (before the first layout), the element is visible") {
                val list = onUiThreadGet { WList(items(100)).also { it.height = 200.0 } }
                val frame = UiTestHarness.sharedFrame()
                onUiThread {
                    frame.add(list)
                    list.ensureIndexIsVisible(42)
                }
                try {
                    awaitUi { list.firstVisibleIndex in 1..42 && list.lastVisibleIndex >= 42 } shouldBe true
                } finally {
                    UiTestHarness.detach(list)
                }
            }

            test("requesting an element out of range before showing does nothing (the first one stays visible)") {
                val list = onUiThreadGet {
                    WList(items(100)).also {
                        it.height = 200.0
                        it.ensureIndexIsVisible(100)
                        it.ensureIndexIsVisible(-1)
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(list)
                try {
                    awaitUi { list.lastVisibleIndex > 0 } shouldBe true
                    onUiThreadGet { list.firstVisibleIndex } shouldBe 0
                } finally {
                    UiTestHarness.detach(list)
                }
            }
        }

        context("WListBox.ensureIndexIsVisible") {
            test("when called before showing, the element is visible when shown") {
                val list = onUiThreadGet {
                    WListBox(items(100)).also {
                        it.height = 200.0
                        it.ensureIndexIsVisible(42)
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(list)
                try {
                    awaitUi { list.firstVisibleIndex in 1..42 && list.lastVisibleIndex >= 42 } shouldBe true
                } finally {
                    UiTestHarness.detach(list)
                }
            }
        }
        context("WTable.setRowSelectionInterval") {
            /** A table of 3 rows of Name / Department. */
            fun table(): WTable = WTable(
                arrayOf(arrayOf<Any?>("Alice", "Sales"), arrayOf<Any?>("Bob", "Engineering"), arrayOf<Any?>("Carol", "Support")),
                arrayOf<Any?>("Name", "Department"),
            )

            /** The row the table (TableView) selects on screen (ITableView.SelectedIndex). */
            fun nativeSelectedRow(table: WTable): Int = table.inspectable.getInt(TabularInterop.ITableView_get_SelectedIndex)

            test("selecting the second row before showing makes the table select that row when shown, and selectedRow is 1") {
                val table = onUiThreadGet { table().also { it.setRowSelectionInterval(1, 1) } }
                UiTestHarness.attachAndAwaitLoaded(table)
                try {
                    awaitUi { nativeSelectedRow(table) == 1 } shouldBe true
                    onUiThreadGet { table.selectedRow } shouldBe 1
                } finally {
                    UiTestHarness.detach(table)
                }
            }

            test("selecting and then clearing the selection before showing leaves nothing selected when shown") {
                val table = onUiThreadGet {
                    table().also {
                        it.setRowSelectionInterval(1, 1)
                        it.clearSelection()
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(table)
                try {
                    awaitUi { table.actualHeight > 0.0 } shouldBe true
                    Thread.sleep(300)
                    onUiThreadGet { nativeSelectedRow(table) to table.selectedRow } shouldBe (-1 to -1)
                } finally {
                    UiTestHarness.detach(table)
                }
            }
        }

        context("WSplitPane divider position") {
            /**
             * Puts [split] in CENTER of a 400 x 260 window with WBorderLayout and shows it without activating it (the same form as the JSplitPane sample).
             * [afterShow] is called right after showing (within the same UI thread task). Returns after the content is laid out.
             */
            fun showInFrame(split: WSplitPane, afterShow: () -> Unit = {}): WFrame {
                val frame = onUiThreadGet {
                    WFrame("BeforeShowRequestsTest").also {
                        it.layout = WBorderLayout()
                        it.add(split, WBorderLayout.CENTER)
                        it.add(WLabel(" "), WBorderLayout.SOUTH)
                        it.setSize(400, 260)
                        it.isAutoRequestFocus = false
                        it.isVisible = true
                        afterShow()
                    }
                }
                awaitUi { split.actualWidth > 0.0 && split.actualHeight > 0.0 } shouldBe true
                Thread.sleep(500) // Wait for the layout after showing (until it grows to the window's size)
                return frame
            }

            test("calling setDividerLocation(200) right after showing puts the divider at 200, and the last position the listener saw is also 200 (as in Swing)") {
                val seen = mutableListOf<Int>()
                val split = onUiThreadGet {
                    WSplitPane(WSplitPane.HORIZONTAL_SPLIT, WLabel("Left"), WLabel("Right")).also { pane ->
                        pane.addChangeListener { seen += pane.dividerLocation }
                    }
                }
                val frame = showInFrame(split) { split.dividerLocation = 200 }
                try {
                    onUiThreadGet { listOf(split.dividerLocation, seen.last(), split.divider.x) } shouldBe listOf(200, 200, 200)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("calling setDividerLocation(150) before showing keeps the divider at 150 after showing") {
                val split = onUiThreadGet {
                    WSplitPane(WSplitPane.HORIZONTAL_SPLIT, WLabel("Left"), WLabel("Right")).also { it.dividerLocation = 150 }
                }
                val frame = showInFrame(split)
                try {
                    onUiThreadGet { split.dividerLocation to split.divider.x } shouldBe (150 to 150)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("in a vertical split with resizeWeight 0.7 and setDividerLocation(0.7) given before showing, the divider is at 70% of the shown height") {
                val split = onUiThreadGet {
                    WSplitPane(WSplitPane.VERTICAL_SPLIT, WTextPane().also { it.text = "Editor" }, WTextPane().also { it.text = "Console" })
                        .also {
                            it.resizeWeight = 0.7
                            it.setDividerLocation(0.7)
                        }
                }
                val frame = showInFrame(split)
                try {
                    onUiThreadGet {
                        // The same formula as Swing: (int) ((height - divider size) * 0.7)
                        val expected = ((Math.floor(split.actualHeight).toInt() - split.dividerSize) * 0.7).toInt()
                        listOf(split.dividerLocation, split.divider.y) to listOf(expected, expected)
                    }.let { (actual, expected) -> actual shouldBe expected }
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("setDividerLocation(0.0) and (1.0) given before showing put the divider at the ends (0 and height - divider)") {
                val top = onUiThreadGet {
                    WSplitPane(WSplitPane.VERTICAL_SPLIT, WLabel("Top"), WLabel("Bottom")).also { it.setDividerLocation(0.0) }
                }
                val topFrame = showInFrame(top)
                try {
                    onUiThreadGet { top.dividerLocation } shouldBe 0
                } finally {
                    onUiThread { topFrame.dispose() }
                }
                val bottom = onUiThreadGet {
                    WSplitPane(WSplitPane.VERTICAL_SPLIT, WLabel("Top"), WLabel("Bottom")).also { it.setDividerLocation(1.0) }
                }
                val bottomFrame = showInFrame(bottom)
                try {
                    onUiThreadGet { bottom.dividerLocation to Math.floor(bottom.actualHeight).toInt() - bottom.dividerSize }
                        .let { (actual, expected) -> actual shouldBe expected }
                } finally {
                    onUiThread { bottomFrame.dispose() }
                }
            }
        }

        context("WScrollPane scroll position") {
            /** Shows a WScrollPane containing a WPanel with 50 labels stacked vertically in CENTER of a 400 x 260 window with WBorderLayout. */
            fun showScroll(afterShow: (WScrollPane) -> Unit): Pair<WFrame, WScrollPane> = onUiThreadGet {
                val items = WPanel().also { panel -> (1..50).forEach { panel.add(WLabel("Item $it")) } }
                val scroll = WScrollPane(items)
                val frame = WFrame("BeforeShowRequestsTest").also {
                    it.layout = WBorderLayout()
                    it.add(scroll, WBorderLayout.CENTER)
                    it.setSize(400, 260)
                    it.isAutoRequestFocus = false
                    it.isVisible = true
                }
                afterShow(scroll)
                frame to scroll
            }

            test("setting viewport.viewPosition to (0, 300) right after showing keeps that position visible after layout") {
                val (frame, scroll) = showScroll { it.viewport.viewPosition = WPoint(0, 300) }
                try {
                    awaitUi { scroll.viewport.viewPosition == WPoint(0, 300) } shouldBe true
                    Thread.sleep(500) // It must not be reset by a later layout
                    onUiThreadGet { scroll.viewport.viewPosition } shouldBe WPoint(0, 300)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("setting the value of the vertical scroll bar to 200 right after showing keeps that position visible after layout") {
                val (frame, scroll) = showScroll { it.verticalScrollBar.value = 200 }
                try {
                    awaitUi { scroll.viewport.viewPosition.y == 200 } shouldBe true
                    Thread.sleep(500)
                    onUiThreadGet { scroll.verticalScrollBar.value } shouldBe 200
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("a position requested right after showing is replaced by a position requested next (the last request takes effect)") {
                val (frame, scroll) = showScroll {
                    it.viewport.viewPosition = WPoint(0, 300)
                    it.viewport.viewPosition = WPoint(0, 120)
                }
                try {
                    awaitUi { scroll.viewport.viewPosition == WPoint(0, 120) } shouldBe true
                    Thread.sleep(500)
                    onUiThreadGet { scroll.viewport.viewPosition } shouldBe WPoint(0, 120)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }
        }
    }
}
