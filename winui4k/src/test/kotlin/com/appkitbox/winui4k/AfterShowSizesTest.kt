package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Collections

/**
 * Tests that check sizes and positions read right after showing (before the first layout) or right after adding to the content can be used as in Swing.
 * The expected values come from what Swing gives when called in the same order (JScrollBar.getMaximum is the content's height even right after showing, and inside invokeLater
 * the content's height after adding; getLastDividerLocation inside a listener of JSplitPane.setDividerLocation is the previous value).
 */
@Tags("E2E")
class AfterShowSizesTest : FunSpec() {
    /** Runs layouts and waits until [condition] holds (up to 5 seconds). true if it holds. */
    private fun awaitUi(condition: () -> Boolean): Boolean {
        repeat(100) {
            if (onUiThreadGet(condition)) return true
            Thread.sleep(50)
        }
        return false
    }

    /**
     * Shows a WScrollPane containing a WPanel with [rows] labels stacked vertically in CENTER of a 400 x 260 window with WBorderLayout,
     * and calls [afterShow] right after showing (within the same UI thread task).
     */
    private fun showScroll(rows: Int = 50, afterShow: (WScrollPane) -> Unit): Pair<WFrame, WScrollPane> = onUiThreadGet {
        val items = WPanel().also { panel -> (1..rows).forEach { panel.add(WLabel("Item $it")) } }
        val scroll = WScrollPane(items)
        val frame = WFrame("AfterShowSizesTest").also {
            it.layout = WBorderLayout()
            it.add(scroll, WBorderLayout.CENTER)
            it.setSize(400, 260)
            it.isAutoRequestFocus = false
            it.isVisible = true
        }
        afterShow(scroll)
        frame to scroll
    }

    /** Waits until it is laid out (the viewport's size is decided and later layouts are done). */
    private fun awaitLaidOut(scroll: WScrollPane) {
        awaitUi { scroll.viewport.extentSize.height > 0.0 } shouldBe true
        Thread.sleep(500)
    }

    init {
        // Open the shared window first so the application does not end when all the test windows are closed
        beforeSpec { UiTestHarness.sharedFrame() }

        context("scroll sizes right after showing") {
            test("the maximum of the vertical scroll bar and the height of viewport.viewSize are not 0, and differ from the content's height after layout only by rounding") {
                var rightAfterShow = 0 to 0.0
                val (frame, scroll) = showScroll { rightAfterShow = it.verticalScrollBar.maximum to it.viewport.viewSize.height }
                try {
                    awaitLaidOut(scroll)
                    val laidOut = onUiThreadGet { scroll.verticalScrollBar.maximum }
                    (laidOut > 260) shouldBe true // 50 rows are taller than the viewport
                    val (maximum, viewHeight) = rightAfterShow
                    maximum shouldBe Math.round(viewHeight).toInt()
                    // Measuring before layout uses layout rounding different from the screen scale (less than 1 DIP per row, less than 50 for 50 rows)
                    (Math.abs(maximum - laidOut) < 50) shouldBe true
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("calling setValue(getMaximum()) has it scrolled to the end after layout (as in Swing)") {
                val (frame, scroll) = showScroll { it.verticalScrollBar.value = it.verticalScrollBar.maximum }
                try {
                    awaitLaidOut(scroll)
                    onUiThreadGet { scroll.verticalScrollBar.value to Math.round(scroll.scrollableHeight).toInt() }
                        .let { (value, end) -> value shouldBe end }
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("requesting beyond the content (getMaximum() + 1000) also has it scrolled to the end after layout") {
                val (frame, scroll) = showScroll { it.verticalScrollBar.value = it.verticalScrollBar.maximum + 1000 }
                try {
                    awaitLaidOut(scroll)
                    onUiThreadGet { scroll.verticalScrollBar.value to Math.round(scroll.scrollableHeight).toInt() }
                        .let { (value, end) -> value shouldBe end }
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("when the content is shorter than the viewport, setValue(getMaximum()) leaves the position 0 after layout (it cannot scroll)") {
                var rightAfterShow = 0
                val (frame, scroll) = showScroll(rows = 3) {
                    rightAfterShow = it.verticalScrollBar.maximum
                    it.verticalScrollBar.value = rightAfterShow
                }
                try {
                    awaitLaidOut(scroll)
                    (rightAfterShow > 0) shouldBe true
                    onUiThreadGet { scroll.verticalScrollBar.value to scroll.scrollableHeight } shouldBe (0 to 0.0)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }
        }

        context("scroll listeners right after showing") {
            test("when setValue(200) returns, AdjustmentListener and the viewport's ChangeListener have been called, and they are called only once more when layout decides the sizes") {
                // The [value, viewport height] and [position, viewport height] the listeners read
                val values = Collections.synchronizedList(mutableListOf<List<Int>>())
                val positions = Collections.synchronizedList(mutableListOf<Pair<WPoint, Int>>())
                var rightAway = emptyList<List<Int>>() to emptyList<Pair<WPoint, Int>>()
                val (frame, scroll) = showScroll {
                    val bar = it.verticalScrollBar
                    bar.addAdjustmentListener { e -> values += listOf(e.value, bar.visibleAmount) }
                    it.viewport.addChangeListener { _ -> positions += it.viewport.viewPosition to Math.round(it.viewport.extentSize.height).toInt() }
                    bar.value = 200
                    rightAway = values.toList() to positions.toList()
                }
                try {
                    rightAway shouldBe (listOf(listOf(200, 0)) to listOf(WPoint(0, 200) to 0)) // Before layout the viewport's size is 0
                    awaitLaidOut(scroll)
                    val visible = onUiThreadGet { scroll.verticalScrollBar.visibleAmount }
                    (visible > 0) shouldBe true
                    onUiThreadGet { scroll.viewport.viewPosition } shouldBe WPoint(0, 200)
                    (values.toList() to positions.toList()) shouldBe (
                        listOf(listOf(200, 0), listOf(200, visible)) to listOf(WPoint(0, 200) to 0, WPoint(0, 200) to visible)
                        )
                } finally {
                    onUiThread { frame.dispose() }
                }
            }

            test("if it stops at the end after layout and the position differs from the requested value, it notifies again with the stopped position") {
                val values = Collections.synchronizedList(mutableListOf<Int>())
                val (frame, scroll) = showScroll {
                    it.verticalScrollBar.addAdjustmentListener { e -> values += e.value }
                    it.verticalScrollBar.value = 5000
                }
                try {
                    awaitLaidOut(scroll)
                    val end = onUiThreadGet { scroll.verticalScrollBar.value }
                    (end in 1 until 5000) shouldBe true
                    values.toList() shouldBe listOf(5000, end)
                } finally {
                    onUiThread { frame.dispose() }
                }
            }
        }

        test("after adding rows to the content after layout, getMaximum() inside the following invokeLater is the content's height after adding (as in Swing)") {
            val (frame, scroll) = showScroll {}
            try {
                awaitLaidOut(scroll)
                val before = onUiThreadGet { scroll.verticalScrollBar.maximum }
                val seen = Collections.synchronizedList(mutableListOf<Int>())
                onUiThread {
                    val items = scroll.viewport.view as WPanel
                    (51..80).forEach { items.add(WLabel("Item $it")) }
                    WinUiUtilities.invokeLater { seen += scroll.verticalScrollBar.maximum }
                }
                awaitUi { seen.isNotEmpty() } shouldBe true
                Thread.sleep(300)
                val after = onUiThreadGet { scroll.verticalScrollBar.maximum }
                (after > before) shouldBe true
                seen.toList() shouldBe listOf(after)
            } finally {
                onUiThread { frame.dispose() }
            }
        }

        test("lastDividerLocation inside the listener of setDividerLocation(200) right after showing is 0, and -1 after it returns (as in Swing)") {
            val seenLast = mutableListOf<Int>()
            var lastAfterSet = Int.MIN_VALUE
            val (frame, split) = onUiThreadGet {
                val split = WSplitPane(WSplitPane.HORIZONTAL_SPLIT, WLabel("Left"), WLabel("Right")).also { pane ->
                    pane.addChangeListener { seenLast += pane.lastDividerLocation }
                }
                val frame = WFrame("AfterShowSizesTest").also {
                    it.layout = WBorderLayout()
                    it.add(split, WBorderLayout.CENTER)
                    it.setSize(400, 260)
                    it.isAutoRequestFocus = false
                    it.isVisible = true
                }
                split.dividerLocation = 200
                lastAfterSet = split.lastDividerLocation
                frame to split
            }
            try {
                awaitUi { split.actualWidth > 0.0 } shouldBe true
                Thread.sleep(300)
                onUiThreadGet { seenLast.toList() to lastAfterSet } shouldBe (listOf(0) to -1)
            } finally {
                onUiThread { frame.dispose() }
            }
        }
    }
}
