package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [WComponent.contains], corresponding to Component.contains(int, int) / contains(Point).
 * Expected values are taken from the expression of Swing's Component.inside (0 <= x < width and 0 <= y < height; coordinates from the component's top-left).
 */
@Tags("E2E")
class WComponentContainsTest : FunSpec() {
    init {
        context("Placement") {
            test("a panel 100 wide and 40 high contains (0, 0) to (99, 39), but not the right edge, bottom edge or negative coordinates") {
                val panel = onUiThreadGet {
                    WPanel().also {
                        it.width = 100.0
                        it.height = 40.0
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(panel)
                try {
                    onUiThreadGet {
                        panel.updateLayout()
                        listOf(
                            panel.contains(0, 0),
                            panel.contains(99, 39),
                            panel.contains(50, 20),
                            panel.contains(100, 0),
                            panel.contains(0, 40),
                            panel.contains(-1, 0),
                            panel.contains(0, -1),
                        )
                    } shouldBe listOf(true, true, true, false, false, false, false)
                } finally {
                    UiTestHarness.detach(panel)
                }
            }

            test("contains(WPoint) is the same as contains(x, y) (a mouse event's point can be passed as is)") {
                val panel = onUiThreadGet {
                    WPanel().also {
                        it.width = 100.0
                        it.height = 40.0
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(panel)
                try {
                    onUiThreadGet {
                        panel.updateLayout()
                        listOf(panel.contains(WPoint(99, 39)), panel.contains(WPoint(100, 39)), panel.contains(WPoint(-1, 0)))
                    } shouldBe listOf(true, false, false)
                } finally {
                    UiTestHarness.detach(panel)
                }
            }
        }

        context("Boundaries and exceptions") {
            test("a component not in a window (size 0) does not contain even (0, 0) (the same as width 0 in Swing)") {
                onUiThreadGet {
                    val panel = WPanel().also {
                        it.width = 100.0
                        it.height = 40.0
                    }
                    panel.contains(0, 0) to panel.contains(WPoint(0, 0))
                } shouldBe (false to false)
            }
        }
    }
}
