package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [WComponent.isPreferredSizeSet] / [WComponent.isMinimumSizeSet] / [WComponent.isMaximumSizeSet], which correspond to
 * Component.isPreferredSizeSet / isMinimumSizeSet / isMaximumSizeSet.
 * Expected values come from Swing's rules (false until setXxxSize is called, true afterwards regardless of the value, false again with setPreferredSize(null)) and
 * WinUI4K's own rule (the preferred width and height are held separately, so setting either one makes it true; resetting both to NaN makes it false).
 */
@Tags("E2E")
class WComponentSizeSetTest : FunSpec() {
    init {
        context("Defaults") {
            test("a newly created panel has none of the preferred, minimum or maximum sizes set") {
                onUiThreadGet {
                    val panel = WPanel()
                    listOf(panel.isPreferredSizeSet, panel.isMinimumSizeSet, panel.isMaximumSizeSet)
                } shouldBe listOf(false, false, false)
            }
        }

        context("Reading and writing") {
            test("setting preferredSize makes only isPreferredSizeSet true") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.preferredSize = WSize(10.0, 20.0)
                    listOf(panel.isPreferredSizeSet, panel.isMinimumSizeSet, panel.isMaximumSizeSet)
                } shouldBe listOf(true, false, false)
            }

            test("setting only width or only height also makes isPreferredSizeSet true (part of the preferred size was decided)") {
                onUiThreadGet {
                    val onlyWidth = WPanel().also { it.width = 10.0 }
                    val onlyHeight = WPanel().also { it.height = 20.0 }
                    onlyWidth.isPreferredSizeSet to onlyHeight.isPreferredSizeSet
                } shouldBe (true to true)
            }

            test("setting minimumSize makes isMinimumSizeSet true (even (0, 0), the same as the default)") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.minimumSize = WSize(0.0, 0.0)
                    panel.isMinimumSizeSet to panel.isMaximumSizeSet
                } shouldBe (true to false)
            }

            test("setting maximumSize makes isMaximumSizeSet true") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.maximumSize = WSize(100.0, 50.0)
                    panel.isMaximumSizeSet to panel.isMinimumSizeSet
                } shouldBe (true to false)
            }

            test("setting maxWidth also makes isMaximumSizeSet true (the width of the maximum size was decided)") {
                onUiThreadGet { WPanel().also { it.maxWidth = 100.0 }.isMaximumSizeSet } shouldBe true
            }

            test("setting the minimum and maximum sizes makes them true even on a WLayoutPanel with a layout manager") {
                onUiThreadGet {
                    val panel = WLayoutPanel(WFlowLayout())
                    val before = panel.isMinimumSizeSet to panel.isMaximumSizeSet
                    panel.minimumSize = WSize(5.0, 5.0)
                    panel.maximumSize = WSize(50.0, 50.0)
                    before to (panel.isMinimumSizeSet to panel.isMaximumSizeSet)
                } shouldBe ((false to false) to (true to true))
            }
        }

        context("Boundaries and exceptions") {
            test("resetting both the width and the height of preferredSize to NaN makes isPreferredSizeSet false (corresponding to Swing's setPreferredSize(null))") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.preferredSize = WSize(10.0, 20.0)
                    panel.preferredSize = WSize(Double.NaN, Double.NaN)
                    panel.isPreferredSizeSet
                } shouldBe false
            }

            test("resetting width to NaN after setting only width makes isPreferredSizeSet false") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.width = 10.0
                    panel.width = Double.NaN
                    panel.isPreferredSizeSet
                } shouldBe false
            }
        }
    }
}
