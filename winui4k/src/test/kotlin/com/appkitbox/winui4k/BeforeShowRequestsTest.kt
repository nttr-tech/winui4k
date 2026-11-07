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
    }
}
