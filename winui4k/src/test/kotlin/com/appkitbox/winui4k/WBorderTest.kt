package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests for the single child of [WBorder] (Border) and the placement of the child by border thickness and padding.
 * The expected child positions and sizes are computed by hand from Border's rule (child area = outer size - border thickness - padding).
 * The numbers are DIP multiples of 4, so the physical pixels are integers even at 125% scaling or more.
 */
@Tags("E2E")
class WBorderTest : FunSpec() {
    /** A 100 × 60 border. The child is a WPanel (which stretches) so that the child area is visible as is. */
    private fun border(): WBorder = WBorder().also {
        it.width = 100.0
        it.height = 60.0
        it.horizontalAlignment = HorizontalAlignment.LEFT
        it.verticalAlignment = VerticalAlignment.TOP
    }

    private fun <T> shown(border: WBorder, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(border)
        try {
            onUiThread { border.updateLayout() }
            return onUiThreadGet(block)
        } finally {
            UiTestHarness.detach(border)
        }
    }

    init {
        test("child returns what was set, and null removes it and resets the parent to null") {
            onUiThread {
                val border = WBorder()
                val child = WPanel()
                border.child.shouldBeNull()
                border.child = child
                border.child shouldBeSameInstanceAs child
                child.parent shouldBeSameInstanceAs border
                border.child = null
                border.child.shouldBeNull()
                child.parent.shouldBeNull()
            }
        }

        test("the child passed to the constructor becomes child") {
            onUiThread {
                val child = WPanel()
                val border = WBorder(child)
                border.child shouldBeSameInstanceAs child
            }
        }

        test("replacing the child with another removes the previous one") {
            onUiThread {
                val first = WPanel()
                val second = WPanel()
                val border = WBorder(first)
                border.child = second
                first.parent.shouldBeNull()
                second.parent shouldBeSameInstanceAs border
            }
        }

        test("without border or padding, the child occupies the whole border") {
            val (border, child) = onUiThreadGet {
                val child = WPanel()
                border().also { it.child = child } to child
            }
            shown(border) { child.bounds } shouldBe WRectangle(0, 0, 100, 60)
        }

        test("with border thickness 4 and padding 8, the child area shrinks by 12 on every side") {
            val (border, child) = onUiThreadGet {
                val child = WPanel()
                border().also {
                    it.borderThickness = 4.0
                    it.padding = 8.0
                    it.child = child
                } to child
            }
            shown(border) { child.bounds } shouldBe WRectangle(12, 12, 76, 36)
        }

        test("setPadding can specify each side separately and is added to the border thickness (left 4, top 8, right 12, bottom 16)") {
            val (border, child) = onUiThreadGet {
                val child = WPanel()
                border().also {
                    it.borderThickness = 4.0
                    it.setPadding(4.0, 8.0, 12.0, 16.0)
                    it.child = child
                } to child
            }
            // left = 4 + 4, top = 4 + 8, width = 100 - (8 + 16), height = 60 - (12 + 20)
            shown(border) { child.bounds } shouldBe WRectangle(8, 12, 76, 28)
        }

        test("changing the padding after it is shown makes the child area follow") {
            val (border, child) = onUiThreadGet {
                val child = WPanel()
                border().also { it.child = child } to child
            }
            shown(border) {
                border.padding = 16.0
                border.updateLayout()
                child.bounds
            } shouldBe WRectangle(16, 16, 68, 28)
        }

        test("border thickness, corner radius, background color and gradient can be set and read back, and removed with null") {
            onUiThread {
                val border = WBorder()
                border.borderThickness = 2.0
                border.cornerRadius = 6.0
                border.borderColor = WColor(255, 0, 0)
                border.background = WColor(0, 128, 255)
                border.borderThickness shouldBe 2.0
                border.cornerRadius shouldBe 6.0
                border.borderColor shouldBe WColor(255, 0, 0)
                border.background shouldBe WColor(0, 128, 255)
                border.borderColor = null
                border.background = null
                border.borderColor.shouldBeNull()
                border.background.shouldBeNull()
            }
        }
    }
}
