package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt

/**
 * Tests that verify the position and size of [WComponent], corresponding to Swing's getX / getY / getLocation / getSize /
 * getBounds / getLocationOnScreen.
 * Positions are expected to be the values determined by the Canvas's Left / Top and margins. All values are DIP multiples
 * of 4 so that physical pixels are integers even at 125% / 150% / 175% scale (layout rounding does not shift the values).
 */
class WComponentBoundsTest : FunSpec() {
    /** A screen with a box, a nested box and a box with a margin laid out at absolute coordinates in an outer Canvas. */
    private class Scene {
        val outer = WCanvas().also {
            it.width = 400.0
            it.height = 300.0
            // contentPane (StackPanel) centers children when there is extra width, so align left to fix the position
            it.horizontalAlignment = HorizontalAlignment.LEFT
        }
        val box = WCanvas().also {
            it.width = 80.0
            it.height = 40.0
        }
        val inner = WCanvas().also {
            it.width = 20.0
            it.height = 12.0
        }
        val marginBox = WCanvas().also {
            it.width = 48.0
            it.height = 32.0
            it.margin = 8.0
        }

        init {
            box.add(inner, 4.0, 8.0)
            outer.add(box, 32.0, 20.0)
            outer.add(marginBox, 200.0, 100.0)
        }
    }

    private fun showScene(): Scene {
        val scene = onUiThreadGet { Scene() }
        UiTestHarness.attachAndAwaitLoaded(scene.outer)
        onUiThread { scene.outer.updateLayout() }
        return scene
    }

    init {
        test("the position in the parent from getX / getY / getLocation equals the Left / Top set on the Canvas") {
            val scene = showScene()
            try {
                onUiThreadGet { Triple(scene.box.x, scene.box.y, scene.box.location) } shouldBe
                    Triple(32, 20, WPoint(32, 20))
                onUiThreadGet { scene.box.actualX to scene.box.actualY } shouldBe (32.0 to 20.0)
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("the position of a nested box is the distance from its parent's (the box's) top-left, not from the window's top-left") {
            val scene = showScene()
            try {
                onUiThreadGet { scene.inner.location } shouldBe WPoint(4, 8)
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("the position of an element with a margin points inside the margin (the top-left of the visible frame)") {
            val scene = showScene()
            try {
                // Inside the Canvas allocation (200, 100) by the margin of 8
                onUiThreadGet { scene.marginBox.location } shouldBe WPoint(208, 108)
                // The size does not include the margin (same as width / height)
                onUiThreadGet { scene.marginBox.size } shouldBe WSize(48.0, 32.0)
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("getSize is the actual size, and getBounds is the integer set of position and size") {
            val scene = showScene()
            try {
                onUiThreadGet { scene.box.size } shouldBe WSize(80.0, 40.0)
                onUiThreadGet { scene.box.bounds } shouldBe WRectangle(32, 20, 80, 40)
                onUiThreadGet { scene.inner.bounds } shouldBe WRectangle(4, 8, 20, 12)
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("changing the size and laying out makes getSize and getBounds follow") {
            val scene = showScene()
            try {
                onUiThread {
                    scene.box.width = 120.0
                    scene.box.height = 60.0
                    scene.outer.updateLayout()
                }
                onUiThreadGet { scene.box.size } shouldBe WSize(120.0, 60.0)
                onUiThreadGet { scene.box.bounds } shouldBe WRectangle(32, 20, 120, 60)
                onUiThread {
                    scene.outer.setLocation(scene.box, 48.0, 36.0)
                    scene.outer.updateLayout()
                }
                onUiThreadGet { scene.box.bounds } shouldBe WRectangle(48, 36, 120, 60)
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }

        test("even for a component without width / height set, getSize returns the actual size after layout (width stays NaN)") {
            val button = onUiThreadGet { WButton("Size") }
            UiTestHarness.attachAndAwaitLoaded(button)
            try {
                onUiThread { button.updateLayout() }
                val (declared, actual, size) = onUiThreadGet { Triple(button.width, button.actualWidth, button.size) }
                declared.isNaN() shouldBe true
                actual shouldBeGreaterThan 0.0
                size.width shouldBe actual
                onUiThreadGet { button.size.height } shouldBe onUiThreadGet { button.actualHeight }
                onUiThreadGet { button.bounds.width } shouldBe actual.roundToInt()
            } finally {
                UiTestHarness.detach(button)
            }
        }

        test("a component not placed in a window has zero position and size") {
            val (location, size, bounds) = onUiThreadGet { WButton("Not placed").let { Triple(it.location, it.size, it.bounds) } }
            location shouldBe WPoint(0, 0)
            size shouldBe WSize(0.0, 0.0)
            bounds shouldBe WRectangle(0, 0, 0, 0)
        }

        test("getLocationOnScreen of a component not placed in a window throws IllegalStateException") {
            val button = onUiThreadGet { WButton("Not placed") }
            shouldThrow<IllegalStateException> { onUiThreadGet { button.locationOnScreen } }
        }

        test("getLocationOnScreen is the on-screen position of the window content's top-left plus the distance converted by the window's scale, in physical pixels") {
            val scene = showScene()
            try {
                // Measure the screen position independently with Win32 (ClientToScreen and GetDpiForWindow)
                val origin = onUiThreadGet { TestWindow.contentOriginOnScreen() }
                val scale = onUiThreadGet { TestWindow.pixelsPerDip() }

                // The outer Canvas is inside the WFrame contentPane's margin of 24
                fun expected(dipX: Double, dipY: Double) =
                    WPoint(origin.x + (dipX * scale).roundToInt(), origin.y + (dipY * scale).roundToInt())
                onUiThreadGet { scene.outer.locationOnScreen } shouldBe expected(24.0, 24.0)
                onUiThreadGet { scene.box.locationOnScreen } shouldBe expected(24.0 + 32.0, 24.0 + 20.0)
                onUiThreadGet { scene.inner.locationOnScreen } shouldBe expected(24.0 + 32.0 + 4.0, 24.0 + 20.0 + 8.0)
                onUiThreadGet { scene.marginBox.locationOnScreen } shouldBe expected(24.0 + 208.0, 24.0 + 108.0)
            } finally {
                UiTestHarness.detach(scene.outer)
            }
        }
    }
}
