package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests the default methods of [WLayoutManager] (minimumLayoutSize / maximumLayoutSize / invalidateLayout) and
 * [WLayoutPanel], which calls them in the same situations as Swing's Container (getMinimumSize / getMaximumSize / invalidate).
 */
class WLayoutManagerTest : FunSpec() {
    /** Implements only preferredLayoutSize and layoutContainer, and counts invalidateLayout calls. */
    private class MinimalLayout : WLayoutManager {
        var invalidated = 0

        override fun preferredLayoutSize(parent: WLayoutPanel): WSize = WSize(70.0, 30.0)

        override fun layoutContainer(parent: WLayoutPanel) {
            // Placement is not checked
        }

        override fun invalidateLayout(parent: WLayoutPanel) {
            invalidated++
        }
    }

    /** A manager that overrides minimumLayoutSize and maximumLayoutSize. */
    private class SizedLayout : WLayoutManager {
        override fun preferredLayoutSize(parent: WLayoutPanel): WSize = WSize(70.0, 30.0)

        override fun minimumLayoutSize(parent: WLayoutPanel): WSize = WSize(20.0, 10.0)

        override fun maximumLayoutSize(parent: WLayoutPanel): WSize = WSize(500.0, 400.0)

        override fun layoutContainer(parent: WLayoutPanel) {
            // Placement is not checked
        }
    }

    init {
        test("the default of minimumLayoutSize is the preferred size, and the default of maximumLayoutSize is unlimited") {
            onUiThread {
                val layout = MinimalLayout()
                val panel = WLayoutPanel(layout)
                layout.minimumLayoutSize(panel) shouldBe WSize(70.0, 30.0)
                layout.maximumLayoutSize(panel) shouldBe WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
            }
        }

        test("WLayoutPanel's minimumSize and maximumSize are the manager's values, a set value takes precedence, and without a manager they are the native values") {
            onUiThread {
                val panel = WLayoutPanel(SizedLayout())
                panel.minimumSize shouldBe WSize(20.0, 10.0)
                panel.maximumSize shouldBe WSize(500.0, 400.0)
                panel.minimumSize = WSize(5.0, 6.0)
                panel.maximumSize = WSize(300.0, 200.0)
                panel.minimumSize shouldBe WSize(5.0, 6.0)
                panel.maximumSize shouldBe WSize(300.0, 200.0)
                val bare = WLayoutPanel()
                bare.minimumSize shouldBe WSize(0.0, 0.0)
                bare.maximumSize shouldBe WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
            }
        }

        test("setting invalidate, revalidate and layout calls the manager's invalidateLayout") {
            onUiThread {
                val layout = MinimalLayout()
                val panel = WLayoutPanel(layout)
                layout.invalidated shouldBe 0
                panel.invalidate()
                layout.invalidated shouldBe 1
                panel.revalidate()
                layout.invalidated shouldBe 2
                val other = MinimalLayout()
                panel.layout = other
                other.invalidated shouldBe 1
                layout.invalidated shouldBe 2
            }
        }
    }
}
