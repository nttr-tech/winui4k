package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that check `revalidate()` of a scroll pane's content does not change the scroll position and does not notify the scroll listeners either
 * (Swing's `panel.add(...); panel.revalidate();` keeps the position, and AdjustmentListener is called only when the position changes).
 * revalidate of the content schedules a relayout of the ancestor WLayoutPanels, and while measuring again the ScrollViewer's viewport grows to the content's height and
 * the position becomes 0 temporarily (it is restored after measuring). Notifying listeners of that intermediate position makes "load more when the end is reached" code run extra times.
 */
@Tags("E2E")
class WScrollPaneRevalidateTest : FunSpec() {
    private class Shown(val frame: WFrame, val items: WPanel, val scroll: WScrollPane)

    /** Shows a WScrollPane containing a WPanel with 50 labels in CENTER of a 400 x 260 window with WBorderLayout. */
    private fun show(): Shown = onUiThreadGet {
        val items = WPanel().also { panel -> (1..50).forEach { panel.add(WLabel("Item $it")) } }
        val scroll = WScrollPane(items)
        val frame = WFrame("WScrollPaneRevalidateTest").also {
            it.layout = WBorderLayout()
            it.add(scroll, WBorderLayout.CENTER)
            it.setSize(400, 260)
            it.isAutoRequestFocus = false
            it.isVisible = true
        }
        Shown(frame, items, scroll)
    }

    init {
        test("adding to the content and calling revalidate does not change the vertical position, and calls neither AdjustmentListener nor ChangeListener") {
            val shown = show()
            try {
                val bar = onUiThreadGet { shown.scroll.verticalScrollBar }
                onUiThread { bar.value = 300 }
                eventually(3.seconds) { onUiThreadGet { shown.scroll.viewport.viewPosition.y } shouldBe 300 }
                Thread.sleep(SETTLE_MILLIS)
                val adjustments = mutableListOf<Int>()
                val changes = mutableListOf<Int>()
                onUiThread {
                    bar.addAdjustmentListener { adjustments += it.value }
                    shown.scroll.viewport.addChangeListener { changes += shown.scroll.viewport.viewPosition.y }
                    (51..70).forEach { shown.items.add(WLabel("Item $it")) }
                    shown.items.revalidate()
                }
                Thread.sleep(SETTLE_MILLIS * 2)
                onUiThreadGet { shown.scroll.viewport.viewPosition.y } shouldBe 300
                adjustments.shouldBeEmpty()
                changes.shouldBeEmpty()
            } finally {
                onUiThread { shown.frame.dispose() }
            }
        }

        test("even after revalidate, positions the user scrolls to still reach AdjustmentListener") {
            val shown = show()
            try {
                val bar = onUiThreadGet { shown.scroll.verticalScrollBar }
                onUiThread { bar.value = 100 }
                eventually(3.seconds) { onUiThreadGet { shown.scroll.viewport.viewPosition.y } shouldBe 100 }
                Thread.sleep(SETTLE_MILLIS)
                val adjustments = mutableListOf<Int>()
                onUiThread {
                    bar.addAdjustmentListener { if (!it.valueIsAdjusting) adjustments += it.value }
                    shown.items.add(WLabel("Item 51"))
                    shown.items.revalidate()
                }
                Thread.sleep(SETTLE_MILLIS * 2)
                onUiThread { bar.value = 200 }
                eventually(3.seconds) { adjustments.toList() shouldContainExactly listOf(200) }
            } finally {
                onUiThread { shown.frame.dispose() }
            }
        }
    }

    private companion object {
        const val SETTLE_MILLIS = 500L
    }
}
