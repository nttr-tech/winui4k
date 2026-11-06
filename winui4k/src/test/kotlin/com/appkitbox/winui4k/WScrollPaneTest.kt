package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for WScrollPane's JScrollPane-like API (policy constants, setViewportView, WViewport position and size, ChangeListener).
 * The scroll pane is 200 x 150 DIP and the content has a fixed size of 1000 x 800 DIP, so the expected values can be computed by hand.
 */
class WScrollPaneTest : FunSpec() {
    private fun content(): WCanvas = WCanvas().apply {
        width = CONTENT_WIDTH
        height = CONTENT_HEIGHT
    }

    private fun WScrollPane.sized(): WScrollPane = apply {
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
        test("the values of the constructor taking policies can be read back as Swing constants and are also reflected in ScrollBarVisibility") {
            val policies = onUiThreadGet {
                val pane = WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_ALWAYS, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
                val noView = WScrollPane(WScrollPane.VERTICAL_SCROLLBAR_NEVER, WScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS)
                listOf(
                    pane.verticalScrollBarPolicy,
                    pane.horizontalScrollBarPolicy,
                    pane.verticalScrollBarVisibility,
                    pane.horizontalScrollBarVisibility,
                    noView.verticalScrollBarPolicy,
                    noView.horizontalScrollBarPolicy,
                    noView.verticalScrollBarVisibility,
                    noView.horizontalScrollBarVisibility,
                    noView.content,
                )
            }
            policies shouldBe listOf(
                22, 30, ScrollBarVisibility.VISIBLE, ScrollBarVisibility.AUTO,
                21, 32, ScrollBarVisibility.HIDDEN, ScrollBarVisibility.VISIBLE, null,
            )
        }

        test("the horizontal default of WScrollPane(view) stays DISABLED, and the horizontal policy reads as NEVER (S-D2: the default is not changed)") {
            val read = onUiThreadGet {
                val pane = WScrollPane(content())
                listOf(pane.horizontalScrollBarVisibility, pane.horizontalScrollBarPolicy, pane.verticalScrollBarPolicy)
            }
            read shouldBe listOf(ScrollBarVisibility.DISABLED, WScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, WScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
        }

        test("a swapped vertical / horizontal or out-of-range policy throws IllegalArgumentException and the value does not change (as in Swing)") {
            val pane = onUiThreadGet { WScrollPane() }
            onUiThread {
                shouldThrow<IllegalArgumentException> { pane.horizontalScrollBarPolicy = WScrollPane.VERTICAL_SCROLLBAR_ALWAYS }
                shouldThrow<IllegalArgumentException> { pane.verticalScrollBarPolicy = WScrollPane.HORIZONTAL_SCROLLBAR_NEVER }
                shouldThrow<IllegalArgumentException> { pane.verticalScrollBarPolicy = 19 }
                shouldThrow<IllegalArgumentException> { WScrollPane(null, 23, WScrollPane.HORIZONTAL_SCROLLBAR_NEVER) }
            }
            onUiThreadGet { listOf(pane.verticalScrollBarVisibility, pane.horizontalScrollBarVisibility) } shouldBe
                listOf(ScrollBarVisibility.VISIBLE, ScrollBarVisibility.DISABLED)
        }

        test("NEVER only hides the bar without shrinking the content width (HIDDEN), and the overflow can be scrolled with viewPosition") {
            val never = onUiThreadGet {
                WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_NEVER, WScrollPane.HORIZONTAL_SCROLLBAR_NEVER).sized()
            }
            shown(never) {
                onUiThreadGet { listOf(never.viewport.viewSize, never.viewport.extentSize) } shouldBe
                    listOf(WSize(CONTENT_WIDTH, CONTENT_HEIGHT), WSize(PANE_WIDTH, PANE_HEIGHT))
                onUiThread { never.viewport.viewPosition = WPoint(300, 200) }
                awaitOnUiThread { never.viewport.viewPosition == WPoint(300, 200) } shouldBe true
            }
        }

        test("with the default DISABLED, the content is fitted to the viewport width (the difference from NEVER)") {
            val disabled = onUiThreadGet { WScrollPane(content()).sized() }
            shown(disabled) {
                // The Canvas has an explicit Width, so with DISABLED it is measured only up to 200 DIP horizontally, and ExtentWidth becomes the viewport width
                onUiThreadGet { disabled.extentWidth } shouldBe PANE_WIDTH
                onUiThreadGet { disabled.scrollableWidth } shouldBe 0.0
            }
        }

        test("setting viewPosition does not animate and can be read back as soon as the setter returns (as in Swing). Values outside the range stop at the edge") {
            val pane = onUiThreadGet {
                WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED).sized()
            }
            shown(pane) {
                onUiThreadGet {
                    pane.viewport.viewPosition = WPoint(120, 340)
                    pane.viewport.viewPosition
                } shouldBe WPoint(120, 340)
                val maxX = (CONTENT_WIDTH - PANE_WIDTH).toInt()
                onUiThreadGet {
                    pane.viewport.viewPosition = WPoint(5000, -10)
                    pane.viewport.viewPosition
                } shouldBe WPoint(maxX, 0)
                onUiThreadGet { pane.viewport.viewRect } shouldBe WRectangle(maxX, 0, PANE_WIDTH.toInt(), PANE_HEIGHT.toInt())
                onUiThreadGet { listOf(pane.horizontalOffset, pane.verticalOffset) } shouldBe listOf(maxX.toDouble(), 0.0)
            }
        }

        test("the vertical and horizontal scroll amounts and the content size can be read") {
            val pane = onUiThreadGet {
                WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_NEVER, WScrollPane.HORIZONTAL_SCROLLBAR_NEVER).sized()
            }
            shown(pane) {
                onUiThreadGet {
                    listOf(pane.viewportWidth, pane.viewportHeight, pane.scrollableWidth, pane.scrollableHeight, pane.extentWidth, pane.extentHeight)
                } shouldBe listOf(
                    PANE_WIDTH,
                    PANE_HEIGHT,
                    CONTENT_WIDTH - PANE_WIDTH,
                    CONTENT_HEIGHT - PANE_HEIGHT,
                    CONTENT_WIDTH,
                    CONTENT_HEIGHT,
                )
            }
        }

        test("setViewportView replaces the content, and the same thing is visible from viewport.view. Setting view also replaces the content") {
            val read = onUiThreadGet {
                val first = content()
                val second = WLabel("b")
                val pane = WScrollPane(WLabel("a"))
                pane.setViewportView(first)
                val afterSetViewportView = listOf(pane.content === first, pane.viewport.view === first, first.parent === pane)
                pane.viewport.view = second
                afterSetViewportView + listOf(pane.content === second, first.parent == null, pane.viewport === pane.viewport)
            }
            read shouldBe listOf(true, true, true, true, true, true)
        }

        test("the viewport's ChangeListener is called when the position changes, the source is the viewport, and the new position can be read inside the listener") {
            val pane = onUiThreadGet {
                WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED).sized()
            }
            shown(pane) {
                val positions = mutableListOf<WPoint>()
                val sources = mutableListOf<Any>()
                val removed = WChangeListener { positions += WPoint(-1, -1) }
                onUiThread {
                    pane.viewport.addChangeListener { e ->
                        sources += e.source
                        positions += (e.source as WViewport).viewPosition
                    }
                    pane.viewport.addChangeListener(removed)
                    pane.viewport.removeChangeListener(removed)
                    pane.viewport.viewPosition = WPoint(0, 250)
                }
                awaitOnUiThread { positions.contains(WPoint(0, 250)) } shouldBe true
                onUiThreadGet { positions.last() } shouldBe WPoint(0, 250)
                onUiThreadGet { positions.contains(WPoint(-1, -1)) } shouldBe false
                onUiThreadGet { sources.all { it === pane.viewport } } shouldBe true
                onUiThreadGet { pane.viewport.getChangeListeners().size } shouldBe 1
            }
        }
    }

    private companion object {
        const val PANE_WIDTH = 200.0
        const val PANE_HEIGHT = 150.0
        const val CONTENT_WIDTH = 1000.0
        const val CONTENT_HEIGHT = 800.0
        const val TIMEOUT_NANOS = 10_000_000_000L
        const val POLL_MILLIS = 50L
    }
}
