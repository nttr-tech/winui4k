package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that reading [WComponent.margin] and [WControl.padding] returns "the value set through this library, or 0 if unset".
 * Native values applied by the theme (such as the Padding 11, 5, 11, 6 of a WButton placed in a window) are not settings, so they
 * do not show up when reading (read the actual four sides with [WControl.insets]).
 * Whichever path sets it (margin / setMargin, padding / setPadding / border), the same value is read: that value if all four sides are equal,
 * otherwise 0, since they cannot be expressed as one number. Previously only the value last written to margin / padding was remembered,
 * and it stayed stale after changes through other paths.
 */
@Tags("E2E")
class WComponentMarginPaddingTest : FunSpec() {
    private fun <C : WComponent, T> attached(create: () -> C, block: (C) -> T): T {
        val component = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(component)
        try {
            return onUiThreadGet { block(component) }
        } finally {
            UiTestHarness.detach(component)
        }
    }

    init {
        context("Defaults") {
            test("the margin of a panel with nothing set is 0") {
                onUiThreadGet { WPanel().margin } shouldBe 0.0
            }

            test("the padding of a WButton placed in a window is 0 because it is not set, even with the theme padding (11, 5, 11, 6)") {
                attached({ WButton("Press") }) { it.padding } shouldBe 0.0
            }

            test("the margin of a WButton placed in a window is 0 because it is not set") {
                attached({ WButton("Press") }) { it.margin } shouldBe 0.0
            }

        }

        context("Reading and writing") {
            test("margin reads back the written value as is") {
                onUiThreadGet { WPanel().also { it.margin = 6.0 }.margin } shouldBe 6.0
            }

            test("the padding of a WButton placed in a window reads back the written value as is") {
                attached({ WButton("Press") }) {
                    it.padding = 8.0
                    it.padding
                } shouldBe 8.0
            }

            test("after writing margin, making all four sides equal with setMargin makes margin read that value") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.margin = 6.0
                    panel.setMargin(3.0, 3.0, 3.0, 3.0)
                    panel.margin
                } shouldBe 3.0
            }

            test("making the four sides different with setMargin makes margin 0, since it cannot be expressed as one number") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.margin = 6.0
                    panel.setMargin(1.0, 2.0, 3.0, 4.0)
                    panel.margin
                } shouldBe 0.0
            }

            test("after writing padding, making all four sides equal with setPadding makes padding read that value") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.padding = 8.0
                    button.setPadding(2.0, 2.0, 2.0, 2.0)
                    button.padding
                } shouldBe 2.0
            }

            test("making the four sides different with setPadding makes padding 0, since it cannot be expressed as one number") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.padding = 8.0
                    button.setPadding(1.0, 2.0, 3.0, 4.0)
                    button.padding
                } shouldBe 0.0
            }

            test("if the padding of an empty border is equal on all four sides, padding reads that value") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.padding = 8.0
                    button.border = WBorderFactory.createEmptyBorder(5, 5, 5, 5)
                    button.padding
                } shouldBe 5.0
            }

            test("if the padding of an empty border is not equal on all four sides, padding is 0") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.padding = 8.0
                    button.border = WBorderFactory.createEmptyBorder(1, 2, 3, 4)
                    button.padding
                } shouldBe 0.0
            }

            test("a border without an empty frame clears the Padding setting and returns to the theme default, so padding is 0") {
                attached({ WButton("Press") }) {
                    it.padding = 8.0
                    it.border = WBorderFactory.createLineBorder(WColor.BLACK, 1)
                    it.padding
                } shouldBe 0.0
            }

            test("setting border to null clears the Padding setting and returns to the theme default, so padding is 0") {
                attached({ WButton("Press") }) {
                    it.padding = 8.0
                    it.border = null
                    it.padding
                } shouldBe 0.0
            }
        }
    }
}
