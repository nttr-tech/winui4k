package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that reading [WComponent.margin] and [WControl.padding] returns the native values (FrameworkElement.Margin / Control.Padding).
 * It is the value if all four sides are equal, otherwise NaN (it cannot be expressed as one number).
 * Previously the last value written to margin / padding was remembered and returned on the Kotlin side, so it stayed stale after changes
 * through [WComponent.setMargin], [WControl.setPadding] or [WControl.border].
 */
@Tags("E2E")
class WComponentMarginPaddingTest : FunSpec() {
    init {
        context("Defaults") {
            test("the margin of a panel with nothing set is 0") {
                onUiThreadGet { WPanel().margin } shouldBe 0.0
            }

            test("the padding of a WButton placed in a window is NaN because the theme padding (11, 5, 11, 6) is not uniform") {
                val button = onUiThreadGet { WButton("Press") }
                UiTestHarness.attachAndAwaitLoaded(button)
                try {
                    onUiThreadGet { button.padding }.isNaN() shouldBe true
                } finally {
                    UiTestHarness.detach(button)
                }
            }
        }

        context("Reading and writing") {
            test("margin reads back the written value as is") {
                onUiThreadGet { WPanel().also { it.margin = 6.0 }.margin } shouldBe 6.0
            }

            test("after writing margin, making all four sides equal with setMargin makes margin read that value") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.margin = 6.0
                    panel.setMargin(3.0, 3.0, 3.0, 3.0)
                    panel.margin
                } shouldBe 3.0
            }

            test("making the four sides different with setMargin makes margin NaN") {
                onUiThreadGet {
                    val panel = WPanel()
                    panel.margin = 6.0
                    panel.setMargin(1.0, 2.0, 3.0, 4.0)
                    panel.margin
                }.isNaN() shouldBe true
            }

            test("after writing padding, making all four sides equal with setPadding makes padding read that value") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.padding = 8.0
                    button.setPadding(2.0, 2.0, 2.0, 2.0)
                    button.padding
                } shouldBe 2.0
            }

            test("if the padding of an empty border is equal on all four sides, padding reads that value") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.padding = 8.0
                    button.border = WBorderFactory.createEmptyBorder(5, 5, 5, 5)
                    button.padding
                } shouldBe 5.0
            }

            test("making the four sides different with setPadding makes padding NaN") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.setPadding(1.0, 2.0, 3.0, 4.0)
                    button.padding
                }.isNaN() shouldBe true
            }
        }
    }
}
