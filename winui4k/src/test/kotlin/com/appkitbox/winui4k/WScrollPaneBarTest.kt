package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for WScrollPane's scroll bar API (WScrollPaneBar and WAdjustmentListener), headers and corners, and WComponent.scrollRectToVisible.
 * The scroll pane is 200 x 150 DIP and the content has a fixed size of 1000 x 800 DIP, and the expected values are computed by hand from Swing's JScrollBar rules
 * (maximum = the content length, visibleAmount = the viewport length, value = the visible top-left position).
 */
@Tags("E2E")
class WScrollPaneBarTest : FunSpec() {
    private fun content(): WCanvas = WCanvas().apply {
        width = CONTENT_WIDTH
        height = CONTENT_HEIGHT
    }

    private fun pane(view: WComponent = content()): WScrollPane =
        WScrollPane(view, WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED).apply {
            width = PANE_WIDTH
            height = PANE_HEIGHT
        }

    /** Waits (up to 10 seconds) until [condition] becomes true on the UI thread. true if it did. */
    private fun awaitOnUiThread(condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TIMEOUT_NANOS
        while (System.nanoTime() < deadline) {
            if (onUiThreadGet(condition)) return true
            Thread.sleep(POLL_MILLIS)
        }
        return false
    }

    /** Puts [pane] in a window, runs [block], and removes it. */
    private fun shown(pane: WScrollPane, block: () -> Unit) {
        UiTestHarness.attachAndAwaitLoaded(pane)
        try {
            onUiThread { pane.updateLayout() }
            block()
        } finally {
            UiTestHarness.detach(pane)
        }
    }

    init {
        test("the minimum, maximum, visible amount and orientation of the vertical and horizontal bars come from the content and viewport lengths") {
            val pane = onUiThreadGet { pane() }
            shown(pane) {
                onUiThreadGet {
                    val v = pane.verticalScrollBar
                    val h = pane.horizontalScrollBar
                    listOf(v.orientation, v.minimum, v.maximum, v.visibleAmount, v.value, h.orientation, h.minimum, h.maximum, h.visibleAmount, h.value)
                } shouldBe listOf(
                    WAdjustable.VERTICAL, 0, CONTENT_HEIGHT.toInt(), PANE_HEIGHT.toInt(), 0,
                    WAdjustable.HORIZONTAL, 0, CONTENT_WIDTH.toInt(), PANE_WIDTH.toInt(), 0,
                )
                onUiThreadGet { pane.verticalScrollBar === pane.verticalScrollBar } shouldBe true
            }
        }

        test("setting a bar's value immediately moves only that direction, and it can be read back when the setter returns") {
            val pane = onUiThreadGet { pane() }
            shown(pane) {
                onUiThreadGet {
                    pane.verticalScrollBar.value = 300
                    listOf(pane.verticalScrollBar.value, pane.horizontalScrollBar.value)
                } shouldBe listOf(300, 0)
                onUiThreadGet {
                    pane.horizontalScrollBar.value = 120
                    pane.viewport.viewPosition
                } shouldBe WPoint(120, 300)
            }
        }

        test("AdjustmentListeners are called only for the bar whose value changed, events have TRACK, the value and the source, and the last notification is not intermediate") {
            val pane = onUiThreadGet { pane() }
            shown(pane) {
                val vertical = mutableListOf<WAdjustmentEvent>()
                val horizontal = mutableListOf<WAdjustmentEvent>()
                onUiThread {
                    pane.verticalScrollBar.addAdjustmentListener { e -> vertical += e }
                    pane.horizontalScrollBar.addAdjustmentListener { e -> horizontal += e }
                    pane.verticalScrollBar.value = 250
                }
                awaitOnUiThread { vertical.any { it.value == 250 && !it.valueIsAdjusting } } shouldBe true
                onUiThreadGet {
                    val last = vertical.last()
                    listOf(last.id, last.adjustmentType, last.value, last.valueIsAdjusting, last.source === pane.verticalScrollBar, last.adjustable === last.source)
                } shouldBe listOf(WAdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, WAdjustmentEvent.TRACK, 250, false, true, true)
                onUiThreadGet { horizontal.size } shouldBe 0
                onUiThreadGet { pane.verticalScrollBar.valueIsAdjusting } shouldBe false
            }
        }

        test("when setValue returns, AdjustmentListener has been called once, and the later ViewChanged does not call it again (as in Swing)") {
            val pane = onUiThreadGet { pane() }
            shown(pane) {
                val events = mutableListOf<WAdjustmentEvent>()
                onUiThreadGet {
                    pane.verticalScrollBar.addAdjustmentListener { e -> events += e }
                    pane.verticalScrollBar.value = 250
                    events.map { listOf(it.value, it.adjustmentType, it.valueIsAdjusting) }
                } shouldBe listOf(listOf(250, WAdjustmentEvent.TRACK, false))
                Thread.sleep(SETTLE_MILLIS) // The later ViewChanged
                onUiThreadGet { events.map { it.value } } shouldBe listOf(250)
            }
        }

        test("setValue with the same value does not call the listener, and changing to different values calls it once per value") {
            val pane = onUiThreadGet { pane() }
            shown(pane) {
                val values = mutableListOf<Int>()
                onUiThreadGet {
                    pane.horizontalScrollBar.addAdjustmentListener { e -> values += e.value }
                    pane.horizontalScrollBar.value = 0
                    pane.horizontalScrollBar.value = 120
                    pane.horizontalScrollBar.value = 120
                    pane.horizontalScrollBar.value = 300
                    values.toList()
                } shouldBe listOf(120, 300)
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { values.toList() } shouldBe listOf(120, 300)
            }
        }

        test("when setting viewport.viewPosition returns, ChangeListener has been called once, reads the new position inside, and is not called again later") {
            val pane = onUiThreadGet { pane() }
            shown(pane) {
                val positions = mutableListOf<WPoint>()
                val vertical = mutableListOf<Int>()
                onUiThreadGet {
                    pane.viewport.addChangeListener { positions += pane.viewport.viewPosition }
                    pane.verticalScrollBar.addAdjustmentListener { e -> vertical += e.value }
                    pane.viewport.viewPosition = WPoint(40, 260)
                    positions.toList() to vertical.toList()
                } shouldBe (listOf(WPoint(40, 260)) to listOf(260))
                Thread.sleep(SETTLE_MILLIS)
                onUiThreadGet { positions.toList() to vertical.toList() } shouldBe (listOf(WPoint(40, 260)) to listOf(260))
            }
        }

        test("non-programmatic (animated) scrolling is still notified with ViewChanged") {
            val pane = onUiThreadGet { pane() }
            shown(pane) {
                val values = mutableListOf<Int>()
                onUiThread {
                    pane.horizontalScrollBar.addAdjustmentListener { e -> if (!e.valueIsAdjusting) values += e.value }
                    pane.scrollToHorizontalOffset(200.0)
                }
                awaitOnUiThread { values.contains(200) } shouldBe true
            }
        }

        test("a listener removed with removeAdjustmentListener is not called and disappears from getAdjustmentListeners") {
            val pane = onUiThreadGet { pane() }
            shown(pane) {
                val calls = mutableListOf<String>()
                val removed = WAdjustmentListener { calls += "removed" }
                onUiThread {
                    pane.horizontalScrollBar.addAdjustmentListener { calls += "kept" }
                    pane.horizontalScrollBar.addAdjustmentListener(removed)
                    pane.horizontalScrollBar.removeAdjustmentListener(removed)
                    pane.horizontalScrollBar.value = 400
                }
                awaitOnUiThread { calls.isNotEmpty() } shouldBe true
                onUiThreadGet { calls.distinct() } shouldBe listOf("kept")
                onUiThreadGet { pane.horizontalScrollBar.getAdjustmentListeners().size } shouldBe 1
            }
        }

        test("the column header, row header and top-left corner are shown in the window (the content becomes top-left aligned), and the scroll pane becomes their parent") {
            val column = onUiThreadGet { WLabel("column").apply { height = HEADER_SIZE } }
            val row = onUiThreadGet { WLabel("row").apply { width = HEADER_SIZE } }
            val corner = onUiThreadGet { WLabel("c") }
            val pane = onUiThreadGet {
                pane().apply {
                    columnHeaderView = column
                    rowHeaderView = row
                    setCorner(WScrollPane.UPPER_LEADING_CORNER, corner)
                }
            }
            shown(pane) {
                onUiThreadGet { listOf(column.actualHeight, row.actualWidth) } shouldBe listOf(HEADER_SIZE, HEADER_SIZE)
                onUiThreadGet { corner.actualWidth > 0.0 } shouldBe true
                onUiThreadGet {
                    listOf(
                        column.parent === pane,
                        row.parent === pane,
                        corner.parent === pane,
                        pane.getCorner(WScrollPane.UPPER_LEFT_CORNER) === corner,
                        pane.getCorner(WScrollPane.LOWER_RIGHT_CORNER) == null,
                    )
                } shouldBe listOf(true, true, true, true, true)
                // With headers, ScrollViewer accepts only top-left aligned content, so the content's alignment has been rewritten
                onUiThreadGet { listOf(pane.content?.horizontalAlignmentInParent, pane.content?.verticalAlignmentInParent) } shouldBe
                    listOf(HorizontalAlignment.LEFT, VerticalAlignment.TOP)
            }
        }

        test("moving the content into a header removes it from the content, and setting null removes the parent") {
            val read = onUiThreadGet {
                val label = WLabel("a")
                val pane = WScrollPane(label)
                pane.columnHeaderView = label
                val moved = listOf(pane.content == null, pane.columnHeaderView === label, label.parent === pane)
                pane.columnHeaderView = null
                moved + listOf(label.parent == null)
            }
            read shouldBe listOf(true, true, true, true)
        }

        test("corners ScrollViewer does not have throw UnsupportedOperationException, and non-corner names throw IllegalArgumentException") {
            onUiThread {
                val pane = WScrollPane()
                shouldThrow<UnsupportedOperationException> { pane.setCorner(WScrollPane.UPPER_RIGHT_CORNER, WLabel("x")) }
                shouldThrow<UnsupportedOperationException> { pane.setCorner(WScrollPane.LOWER_TRAILING_CORNER, null) }
                shouldThrow<IllegalArgumentException> { pane.setCorner("CENTER", WLabel("x")) }
                pane.getCorner("CENTER") shouldBe null
            }
        }

        test("scrollRectToVisible scrolls by the minimum amount needed to make the rectangle in the content visible (same as Swing's JViewport)") {
            val view = onUiThreadGet { content() }
            val pane = onUiThreadGet { pane(view) }
            shown(pane) {
                onUiThread { view.scrollRectToVisible(WRectangle(600, 500, 50, 50)) }
                // The position where the bottom-right end (650, 550) comes to the bottom-right of the viewport (200 x 150)
                awaitOnUiThread { pane.viewport.viewPosition == WPoint(450, 400) } shouldBe true
                onUiThread { view.scrollRectToVisible(WRectangle(100, 100, 10, 10)) }
                // The position where the top-left end comes to the top-left of the viewport
                awaitOnUiThread { pane.viewport.viewPosition == WPoint(100, 100) } shouldBe true
            }
        }

        test("scrollRectToVisible does nothing when not in a window") {
            onUiThreadGet {
                val view = content()
                WScrollPane(view).also { view.scrollRectToVisible(WRectangle(10, 10, 10, 10)) }.verticalOffset
            } shouldBe 0.0
        }
    }

    private companion object {
        const val PANE_WIDTH = 200.0
        const val PANE_HEIGHT = 150.0
        const val CONTENT_WIDTH = 1000.0
        const val CONTENT_HEIGHT = 800.0
        const val HEADER_SIZE = 30.0
        const val TIMEOUT_NANOS = 10_000_000_000L
        const val POLL_MILLIS = 50L

        /** How long to wait for a later ViewChanged. */
        const val SETTLE_MILLIS = 500L
    }
}
