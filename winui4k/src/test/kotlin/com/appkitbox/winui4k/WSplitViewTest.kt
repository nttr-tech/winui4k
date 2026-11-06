package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for opening and closing, display mode and placement of [WSplitView] (SplitView; a collapsible pane, not a JSplitPane).
 * Expected values are computed by hand from SplitView's rules (Inline narrows the content by the pane's width, Overlay covers the content without narrowing it,
 * and CompactInline leaves a narrow pane of compactPaneLength even when closed).
 * Left and right of the pane are checked by comparing on-screen x coordinates (independent of scaling).
 */
@Tags("E2E")
class WSplitViewTest : FunSpec() {
    /** A SplitView 400 wide × 200 high. The open pane width is 100. The pane and content are WPanels (which stretch). */
    private class Scene {
        val pane = WPanel()
        val content = WPanel()
        val split = WSplitView(pane, content).also {
            it.width = 400.0
            it.height = 200.0
            it.openPaneLength = 100.0
            it.horizontalAlignment = HorizontalAlignment.LEFT
            it.verticalAlignment = VerticalAlignment.TOP
        }
    }

    private fun <T> shown(scene: Scene, block: (Scene) -> T): T {
        UiTestHarness.attachAndAwaitLoaded(scene.split)
        try {
            onUiThread { scene.split.updateLayout() }
            return onUiThreadGet { block(scene) }
        } finally {
            UiTestHarness.detach(scene.split)
        }
    }

    init {
        test("by default it is closed, on the left, with Overlay") {
            onUiThreadGet {
                val split = WSplitView()
                listOf(split.isPaneOpen, split.panePlacement, split.displayMode)
            } shouldBe listOf(false, SplitViewPanePlacement.LEFT, SplitViewDisplayMode.OVERLAY)
        }

        test("the default openPaneLength of 320 can be read after it is shown with the default style applied (0 before it is shown)") {
            val split = onUiThreadGet { WSplitView().also { it.width = 400.0 } }
            val before = onUiThreadGet { split.openPaneLength }
            UiTestHarness.attachAndAwaitLoaded(split)
            try {
                onUiThreadGet { split.openPaneLength } shouldBe 320.0
            } finally {
                UiTestHarness.detach(split)
            }
            before shouldBe 0.0
        }

        test("isPaneOpen, openPaneLength, panePlacement and displayMode read back the set values") {
            onUiThreadGet {
                val split = WSplitView()
                split.isPaneOpen = true
                split.openPaneLength = 120.0
                split.panePlacement = SplitViewPanePlacement.RIGHT
                split.displayMode = SplitViewDisplayMode.COMPACT_INLINE
                listOf(split.isPaneOpen, split.openPaneLength, split.panePlacement, split.displayMode)
            } shouldBe listOf(true, 120.0, SplitViewPanePlacement.RIGHT, SplitViewDisplayMode.COMPACT_INLINE)
        }

        test("all four display mode values can be set and read back") {
            onUiThreadGet {
                val split = WSplitView()
                SplitViewDisplayMode.entries.map {
                    split.displayMode = it
                    split.displayMode
                }
            } shouldBe SplitViewDisplayMode.entries.toList()
        }

        test("the pane and content passed to the constructor become pane and content, with the SplitView as their parent") {
            onUiThread {
                val pane = WPanel()
                val content = WPanel()
                val split = WSplitView(pane, content)
                split.pane shouldBeSameInstanceAs pane
                split.content shouldBeSameInstanceAs content
                pane.parent shouldBeSameInstanceAs split
                content.parent shouldBeSameInstanceAs split
            }
        }

        test("setting null removes the pane and content, and their parent goes back to null") {
            onUiThread {
                val pane = WPanel()
                val content = WPanel()
                val split = WSplitView(pane, content)
                split.pane = null
                split.content = null
                split.pane.shouldBeNull()
                split.content.shouldBeNull()
                pane.parent.shouldBeNull()
                content.parent.shouldBeNull()
            }
        }

        test("opened with Inline, the pane is openPaneLength wide and the content narrows by that much (100 / 300)") {
            val scene = onUiThreadGet {
                Scene().also {
                    it.split.displayMode = SplitViewDisplayMode.INLINE
                    it.split.isPaneOpen = true
                }
            }
            val (pane, content) = shown(scene) { it.pane.size.width to it.content.size.width }
            pane shouldBe (100.0 plusOrMinus PANE_ROOT_TOLERANCE)
            content shouldBe 300.0
        }

        test("closed with Inline, the content is the full width of 400") {
            val scene = onUiThreadGet {
                Scene().also { it.split.displayMode = SplitViewDisplayMode.INLINE }
            }
            shown(scene) { it.content.size.width } shouldBe 400.0
        }

        test("opened with Overlay, the pane only covers the content and the content stays the full width of 400") {
            val scene = onUiThreadGet {
                Scene().also {
                    it.split.displayMode = SplitViewDisplayMode.OVERLAY
                    it.split.isPaneOpen = true
                }
            }
            val (pane, content) = shown(scene) { it.pane.size.width to it.content.size.width }
            pane shouldBe (100.0 plusOrMinus PANE_ROOT_TOLERANCE)
            content shouldBe 400.0
        }

        test("CompactInline narrows the content by the narrow pane (the default compactPaneLength 48) even when closed (352), and by 100 when opened (300)") {
            val scene = onUiThreadGet {
                Scene().also { it.split.displayMode = SplitViewDisplayMode.COMPACT_INLINE }
            }
            shown(scene) {
                val closed = it.content.size.width
                it.split.isPaneOpen = true
                it.split.updateLayout()
                closed to it.content.size.width
            } shouldBe (352.0 to 300.0)
        }

        test("with panePlacement Left the pane is to the left of the content (smaller on-screen x), and with Right to the right") {
            val scene = onUiThreadGet {
                Scene().also {
                    it.split.displayMode = SplitViewDisplayMode.INLINE
                    it.split.isPaneOpen = true
                }
            }
            shown(scene) {
                val left = it.pane.locationOnScreen.x < it.content.locationOnScreen.x
                it.split.panePlacement = SplitViewPanePlacement.RIGHT
                it.split.updateLayout()
                left to (it.pane.locationOnScreen.x > it.content.locationOnScreen.x)
            } shouldBe (true to true)
        }

        test("closing isPaneOpen after it is shown returns the Inline content to the full width") {
            val scene = onUiThreadGet {
                Scene().also {
                    it.split.displayMode = SplitViewDisplayMode.INLINE
                    it.split.isPaneOpen = true
                }
            }
            shown(scene) {
                val open = it.content.size.width
                it.split.isPaneOpen = false
                it.split.updateLayout()
                open to it.content.size.width
            } shouldBe (300.0 to 400.0)
        }
    }

    private companion object {
        /** The width of the pane content is narrower than openPaneLength by the border of the pane root (measured at about 1.3 DIP). */
        const val PANE_ROOT_TOLERANCE = 2.0
    }
}
