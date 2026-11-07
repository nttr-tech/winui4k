package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Tests that check, by actually showing them, that when WSlider's ticks are shown the same way as JSlider's (setMajorTickSpacing + setPaintTicks(true)),
 * they are drawn below the track instead of overlapping it.
 */
@Tags("E2E")
class WSliderTicksTest : FunSpec() {
    private fun <T> withShown(slider: WSlider, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(slider)
        try {
            return onUiThreadGet {
                slider.updateLayout()
                block()
            }
        } finally {
            UiTestHarness.detach(slider)
        }
    }

    init {
        test("a newly created slider draws no ticks, as with JSlider (paintTicks false, position NONE)") {
            onUiThreadGet {
                val slider = WSlider()
                listOf("${slider.paintTicks}", "${slider.tickPlacement}")
            } shouldContainExactly listOf("false", "NONE")
        }

        test("deciding the tick spacing and setting paintTicks to true draws the ticks below the track without overlapping it") {
            val slider = onUiThreadGet {
                WSlider(0.0, 100.0, 50.0).also {
                    it.width = 300.0
                    it.majorTickSpacing = 25.0
                    it.paintTicks = true
                }
            }
            val (track, inline, bottom) = withShown(slider) {
                Triple(
                    templatePartVerticalEdges(slider, "HorizontalTrackRect"),
                    templatePartVerticalEdges(slider, "HorizontalInlineTickBar"),
                    templatePartVerticalEdges(slider, "BottomTickBar"),
                )
            }
            inline shouldBe null
            checkNotNull(bottom) { "BottomTickBar is not shown" }.first shouldBeGreaterThanOrEqual checkNotNull(track).second
        }

        test("setting paintTicks to true on a vertical slider draws them on the right (BOTTOM_RIGHT), as with JSlider") {
            onUiThreadGet {
                val slider = WSlider(Orientation.VERTICAL)
                slider.paintTicks = true
                "${slider.tickPlacement}"
            } shouldBe "BOTTOM_RIGHT"
        }
    }
}
