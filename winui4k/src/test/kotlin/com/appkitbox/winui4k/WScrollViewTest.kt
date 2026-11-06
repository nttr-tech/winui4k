package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for WScrollView's Swing-like aliases (setViewportView, the mapping of policy constants to ContentOrientation, viewPosition, ChangeListener) and
 * the original scrollTo / offsets. The viewport is 200 x 150 DIP and the content is 1000 x 800 DIP.
 */
@Tags("E2E")
class WScrollViewTest : FunSpec() {
    private fun content(): WCanvas = WCanvas().apply {
        width = CONTENT_WIDTH
        height = CONTENT_HEIGHT
    }

    private fun bothWays(): WScrollView = WScrollView(content()).apply {
        width = VIEW_WIDTH
        height = VIEW_HEIGHT
        horizontalScrollBarPolicy = WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
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

    private fun shown(view: WScrollView, block: () -> Unit) {
        UiTestHarness.attachAndAwaitLoaded(view)
        try {
            onUiThread { view.updateLayout() }
            block()
        } finally {
            UiTestHarness.detach(view)
        }
    }

    init {
        test("the default (ContentOrientation.VERTICAL) reads as vertical AS_NEEDED and horizontal NEVER") {
            onUiThreadGet {
                val view = WScrollView()
                listOf(view.verticalScrollBarPolicy, view.horizontalScrollBarPolicy, view.contentOrientation)
            } shouldBe listOf(WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_NEVER, ScrollingContentOrientation.VERTICAL)
        }

        test("setting a horizontal policy enables horizontal scrolling (BOTH), and NEVER only hides the bar while keeping horizontal scrolling") {
            onUiThreadGet {
                val view = WScrollView()
                view.horizontalScrollBarPolicy = WScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
                val always = listOf(view.contentOrientation, view.horizontalScrollBarVisibility, view.horizontalScrollBarPolicy)
                view.horizontalScrollBarPolicy = WScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                always + listOf(view.contentOrientation, view.horizontalScrollBarVisibility, view.horizontalScrollBarPolicy)
            } shouldBe listOf(
                ScrollingContentOrientation.BOTH,
                ScrollingScrollBarVisibility.VISIBLE,
                WScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS,
                ScrollingContentOrientation.BOTH,
                ScrollingScrollBarVisibility.HIDDEN,
                WScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
            )
        }

        test("a vertical policy turns HORIZONTAL into BOTH and NONE into VERTICAL, and reads as NEVER for orientations without vertical") {
            onUiThreadGet {
                val view = WScrollView()
                view.contentOrientation = ScrollingContentOrientation.HORIZONTAL
                val before = view.verticalScrollBarPolicy
                view.verticalScrollBarPolicy = WScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                val fromHorizontal = view.contentOrientation
                view.contentOrientation = ScrollingContentOrientation.NONE
                view.verticalScrollBarPolicy = WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                listOf(before, fromHorizontal, view.contentOrientation, view.verticalScrollBarPolicy)
            } shouldBe listOf(
                WScrollPane.VERTICAL_SCROLLBAR_NEVER,
                ScrollingContentOrientation.BOTH,
                ScrollingContentOrientation.VERTICAL,
                WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            )
        }

        test("a policy with vertical and horizontal swapped throws IllegalArgumentException, and the orientation does not change") {
            onUiThread {
                val view = WScrollView()
                shouldThrow<IllegalArgumentException> { view.horizontalScrollBarPolicy = WScrollPane.VERTICAL_SCROLLBAR_NEVER }
                shouldThrow<IllegalArgumentException> { view.verticalScrollBarPolicy = 0 }
                view.contentOrientation shouldBe ScrollingContentOrientation.VERTICAL
            }
        }

        test("setViewportView replaces the content") {
            onUiThreadGet {
                val label = WLabel("a")
                val view = WScrollView(WLabel("b"))
                view.setViewportView(label)
                listOf(view.content === label, label.parent === view)
            } shouldBe listOf(true, true)
        }

        test("viewPosition changes the position without animation, and values outside the range stop at the edge. The size getters are the content and viewport") {
            val view = onUiThreadGet { bothWays() }
            shown(view) {
                onUiThreadGet { listOf(view.extentWidth, view.extentHeight, view.viewportWidth, view.viewportHeight) } shouldBe
                    listOf(CONTENT_WIDTH, CONTENT_HEIGHT, VIEW_WIDTH, VIEW_HEIGHT)
                onUiThread { view.viewPosition = WPoint(120, 340) }
                awaitOnUiThread { view.viewPosition == WPoint(120, 340) } shouldBe true
                onUiThread { view.viewPosition = WPoint(5000, -10) }
                awaitOnUiThread { view.viewPosition == WPoint((CONTENT_WIDTH - VIEW_WIDTH).toInt(), 0) } shouldBe true
            }
        }

        test("scrollTo moves to the given position (after the animation)") {
            val view = onUiThreadGet { bothWays() }
            shown(view) {
                onUiThread { view.scrollTo(300.0, 200.0) }
                awaitOnUiThread { view.horizontalOffset == 300.0 && view.verticalOffset == 200.0 } shouldBe true
                onUiThreadGet { listOf(view.scrollableWidth, view.scrollableHeight) } shouldBe
                    listOf(CONTENT_WIDTH - VIEW_WIDTH, CONTENT_HEIGHT - VIEW_HEIGHT)
            }
        }

        test("ChangeListeners are called when the position changes, the source is this control, and the new position can be read inside the listener. Removed ones are not called") {
            val view = onUiThreadGet { bothWays() }
            shown(view) {
                val positions = mutableListOf<WPoint>()
                val sources = mutableListOf<Any>()
                val removed = WChangeListener { positions += WPoint(-1, -1) }
                onUiThread {
                    view.addChangeListener { e ->
                        sources += e.source
                        positions += (e.source as WScrollView).viewPosition
                    }
                    view.addChangeListener(removed)
                    view.removeChangeListener(removed)
                    view.viewPosition = WPoint(0, 250)
                }
                awaitOnUiThread { positions.contains(WPoint(0, 250)) } shouldBe true
                onUiThreadGet { listOf(positions.contains(WPoint(-1, -1)), sources.all { it === view }, view.getChangeListeners().size) } shouldBe
                    listOf(false, true, 1)
            }
        }
    }

    private companion object {
        const val VIEW_WIDTH = 200.0
        const val VIEW_HEIGHT = 150.0
        const val CONTENT_WIDTH = 1000.0
        const val CONTENT_HEIGHT = 800.0
        const val TIMEOUT_NANOS = 10_000_000_000L
        const val POLL_MILLIS = 50L
    }
}
