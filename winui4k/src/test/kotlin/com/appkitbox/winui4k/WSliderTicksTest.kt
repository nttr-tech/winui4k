package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
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
    /** The first element under [node] whose name (x:Name) is [name] (an owned reference). */
    private fun findByName(node: ComPtr, name: String): ComPtr? {
        val matched = node.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement)?.let { element ->
            try {
                element.getString(XamlInterop.IFrameworkElement_get_Name) == name
            } finally {
                element.release()
            }
        } == true
        if (matched) return node.also { it.addRef() }
        val children = tableTreeChildren(node)
        try {
            return children.firstNotNullOfOrNull { findByName(it, name) }
        } finally {
            children.forEach { it.release() }
        }
    }

    /** The top and bottom edges, in the slider's coordinates, of the element named [name] in the template. null if not shown. */
    private fun verticalEdges(slider: WSlider, name: String): Pair<Double, Double>? {
        val element = checkNotNull(findByName(slider.dependencyObject, name)) { "template part not found: $name" }
        try {
            val (width, height) = VisualHitTest.actualSizeOf(element)
            if (width == 0.0 || height == 0.0) return null
            val (_, top) = VisualHitTest.transform(element, slider.uiElement, 0.0, 0.0)
            return top to top + height
        } finally {
            element.release()
        }
    }

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
                    verticalEdges(slider, "HorizontalTrackRect"),
                    verticalEdges(slider, "HorizontalInlineTickBar"),
                    verticalEdges(slider, "BottomTickBar"),
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
