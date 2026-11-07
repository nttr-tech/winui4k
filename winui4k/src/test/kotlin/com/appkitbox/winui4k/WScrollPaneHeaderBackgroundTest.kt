package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that check, by actually showing them, that the column header, row header and top-left corner of a WScrollPane hide the content scrolled under them (the content does not show through,
 * as with the headers of Swing's JScrollPane), by checking that the topmost element at the header's position is not the content.
 * The content is a 1000 x 800 DIP WCanvas with a painted background (hit-testable everywhere), and the scroll pane is 300 x 200 DIP.
 */
@Tags("E2E")
class WScrollPaneHeaderBackgroundTest : FunSpec() {
    private fun content(): WCanvas = WCanvas().apply {
        width = CONTENT_WIDTH
        height = CONTENT_HEIGHT
        background = WColor(0xCC, 0xEE, 0xFF)
    }

    private fun pane(view: WComponent): WScrollPane =
        WScrollPane(view, WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED).apply {
            width = PANE_WIDTH
            height = PANE_HEIGHT
        }

    /** Waits until [condition] becomes true on the UI thread (up to 10 seconds). true if it becomes true. */
    private fun awaitOnUiThread(condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TIMEOUT_NANOS
        while (System.nanoTime() < deadline) {
            if (onUiThreadGet(condition)) return true
            Thread.sleep(POLL_MILLIS)
        }
        return false
    }

    /** Shows [pane], scrolls to (horizontal [x], vertical [y]), runs [block] on the UI thread and then removes it. */
    private fun <T> shownAndScrolled(pane: WScrollPane, x: Int, y: Int, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(pane)
        try {
            onUiThread {
                pane.updateLayout()
                pane.horizontalScrollBar.value = x
                pane.verticalScrollBar.value = y
            }
            awaitOnUiThread { pane.horizontalScrollBar.value == x && pane.verticalScrollBar.value == y } shouldBe true
            return onUiThreadGet {
                pane.updateLayout()
                block()
            }
        } finally {
            UiTestHarness.detach(pane)
        }
    }

    /** Whether the topmost element at ([x], [y]) in the coordinates of [pane] is [view] (the content) or one of its descendants. */
    private fun contentIsFrontmostAt(pane: WScrollPane, view: WComponent, x: Double, y: Double): Boolean {
        val elements = VisualHitTest.elementsAt(pane.uiElement, x, y)
        try {
            val front = elements.firstOrNull() ?: return false
            front.addRef()
            var node: ComPtr? = front
            while (node != null) {
                if (Xaml.sameObject(node, view.uiElement)) {
                    node.release()
                    return true
                }
                val parent = Xaml.parentOf(node)
                node.release()
                node = parent
            }
            return false
        } finally {
            elements.forEach { it.release() }
        }
    }

    init {
        test("content scrolled down does not show through the column header band, even to the right of the header's text") {
            val view = onUiThreadGet { content() }
            val pane = onUiThreadGet { pane(view).apply { columnHeaderView = WLabel("Text") } }
            shownAndScrolled(pane, 0, SCROLL) {
                val headerHeight = view.let { VisualHitTest.transform(it.uiElement, pane.uiElement, 0.0, 0.0).second } + SCROLL
                // At the vertical center of the header band, to the right of the text ("Text"), and not over the vertical scroll bar
                contentIsFrontmostAt(pane, view, PANE_WIDTH - 40.0, headerHeight / 2)
            } shouldBe false
        }

        test("content scrolled right does not show through the row header band, even below the header's text") {
            val view = onUiThreadGet { content() }
            val pane = onUiThreadGet { pane(view).apply { rowHeaderView = WLabel("1") } }
            shownAndScrolled(pane, SCROLL, 0) {
                // Toward the left of the row header band, below the text ("1"), and not over the horizontal scroll bar
                contentIsFrontmostAt(pane, view, 2.0, PANE_HEIGHT - 40.0)
            } shouldBe false
        }

        test("with column and row headers, content and headers scrolled to the bottom right do not show through the top-left corner") {
            val view = onUiThreadGet { content() }
            val pane = onUiThreadGet {
                pane(view).apply {
                    columnHeaderView = WLabel("Text")
                    rowHeaderView = WLabel("1")
                }
            }
            shownAndScrolled(pane, SCROLL, SCROLL) {
                contentIsFrontmostAt(pane, view, 2.0, 2.0)
            } shouldBe false
        }

        test("even if the content grows after showing, content scrolled to the grown part does not show through at the column header band") {
            val view = onUiThreadGet { content() }
            val pane = onUiThreadGet { pane(view).apply { columnHeaderView = WLabel("Text") } }
            UiTestHarness.attachAndAwaitLoaded(pane)
            try {
                onUiThread {
                    pane.updateLayout()
                    view.width = WIDENED_WIDTH
                    pane.updateLayout()
                    pane.horizontalScrollBar.value = WIDENED_SCROLL
                    pane.verticalScrollBar.value = SCROLL
                }
                awaitOnUiThread { pane.horizontalScrollBar.value == WIDENED_SCROLL && pane.verticalScrollBar.value == SCROLL } shouldBe true
                onUiThreadGet {
                    pane.updateLayout()
                    val headerHeight = VisualHitTest.transform(view.uiElement, pane.uiElement, 0.0, 0.0).second + SCROLL
                    contentIsFrontmostAt(pane, view, PANE_WIDTH - 40.0, headerHeight / 2)
                } shouldBe false
            } finally {
                UiTestHarness.detach(pane)
            }
        }

        test("even with headers, unscrolled content starts below the headers, and there is no content at the headers") {
            val view = onUiThreadGet { content() }
            val pane = onUiThreadGet { pane(view).apply { columnHeaderView = WLabel("Text") } }
            val (contentTop, labelBottom) = shownAndScrolled(pane, 0, 0) {
                val header = checkNotNull(pane.columnHeaderView)
                VisualHitTest.transform(view.uiElement, pane.uiElement, 0.0, 0.0).second to
                    VisualHitTest.transform(header.uiElement, pane.uiElement, 0.0, header.actualHeight).second
            }
            contentTop shouldBe labelBottom
        }
    }

    private companion object {
        const val CONTENT_WIDTH = 1000.0
        const val CONTENT_HEIGHT = 800.0
        const val PANE_WIDTH = 300.0
        const val PANE_HEIGHT = 200.0
        const val SCROLL = 100
        const val WIDENED_WIDTH = 2000.0
        const val WIDENED_SCROLL = 1600
        const val TIMEOUT_NANOS = 10_000_000_000L
        const val POLL_MILLIS = 50L
    }
}
