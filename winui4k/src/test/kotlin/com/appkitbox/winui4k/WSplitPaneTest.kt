package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Tests that verify [WSplitPane] against the behavior of javax.swing.JSplitPane (BasicSplitPaneUI).
 * The expected child positions and sizes are values computed by hand from BasicHorizontalLayoutManager's resetToPreferredSizes / setDividerLocation / distributeSpace and
 * BasicSplitPaneDivider's drag rules (the computations are left in each test's comments). The divider thickness is the default 10.
 * Children's preferred sizes are determined by a Border (stretching to fill the cell) containing a Canvas with an explicit size.
 */
@Tags("E2E")
class WSplitPaneTest : FunSpec() {
    /** A child whose preferred size is [width] x [height] and that stretches to fill the cell. */
    private fun child(width: Double, height: Double): WBorder = WBorder(
        WCanvas().also {
            it.width = width
            it.height = height
        },
    )

    private class Scene(val pane: WSplitPane, val left: WBorder, val right: WBorder)

    /** Puts a left child with a preferred width of 60 and a right child of 50 into a 300 x 100 panel. */
    private fun scene(
        orientation: Int = WSplitPane.HORIZONTAL_SPLIT,
        width: Double = 300.0,
        height: Double = 100.0,
        configure: (WSplitPane) -> Unit = {},
    ): Scene = onUiThreadGet {
        val horizontal = orientation == WSplitPane.HORIZONTAL_SPLIT
        val left = if (horizontal) child(60.0, 10.0) else child(10.0, 30.0)
        val right = if (horizontal) child(50.0, 10.0) else child(10.0, 20.0)
        val pane = WSplitPane(orientation, left, right).also {
            it.width = width
            it.height = height
            it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
            configure(it)
        }
        Scene(pane, left, right)
    }

    /** Loads it and runs layout until the distribution (cell changes from SizeChanged) is reflected. */
    private fun show(scene: Scene) {
        UiTestHarness.attachAndAwaitLoaded(scene.pane)
        settle(scene.pane)
    }

    private fun settle(pane: WSplitPane) {
        onUiThread { pane.validate() }
        onUiThread { pane.validate() }
    }

    private fun bounds(vararg components: WComponent): List<WRectangle> = onUiThreadGet { components.map { it.bounds } }

    private fun <T> shown(scene: Scene, block: () -> T): T {
        show(scene)
        try {
            return block()
        } finally {
            UiTestHarness.detach(scene.pane)
        }
    }

    init {
        test("defaults are a horizontal split, divider 10, location -1, last location 0, resizeWeight 0 and continuous layout, and the constants have the same values as Swing") {
            WSplitPane.VERTICAL_SPLIT shouldBe 0
            WSplitPane.HORIZONTAL_SPLIT shouldBe 1
            onUiThread {
                val pane = WSplitPane()
                pane.orientation shouldBe WSplitPane.HORIZONTAL_SPLIT
                pane.dividerSize shouldBe 10
                pane.dividerLocation shouldBe -1
                pane.lastDividerLocation shouldBe 0
                pane.resizeWeight shouldBe 0.0
                pane.isContinuousLayout.shouldBeTrue()
                pane.leftComponent.shouldBeNull()
                pane.rightComponent.shouldBeNull()
                WSplitPane(WSplitPane.VERTICAL_SPLIT, false).isContinuousLayout.shouldBeFalse()
            }
        }

        test("the first layout puts the preferred size on the left and gives the remainder after the divider to the right (resizeWeight 0)") {
            val s = scene()
            shown(s) {
                // The remainder 300 - (60 + 50 + 10) = 180 all goes to the right: left 60, divider 60..70, right 70..300
                onUiThreadGet { s.pane.dividerLocation } shouldBe 60
                bounds(s.left, s.pane.divider, s.right) shouldBe listOf(
                    WRectangle(0, 0, 60, 100),
                    WRectangle(60, 0, 10, 100),
                    WRectangle(70, 0, 230, 100),
                )
                // Deciding the position by layout does not change lastDividerLocation
                onUiThreadGet { s.pane.lastDividerLocation } shouldBe 0
            }
        }

        test("the remainder of the first layout is given to the left by the resizeWeight ratio (0.5 and 1.0)") {
            val half = scene { it.resizeWeight = 0.5 }
            shown(half) {
                // (int) (0.5 * 180) = 90 of the remainder 180 goes to the left: left 60 + 90 = 150
                onUiThreadGet { half.pane.dividerLocation } shouldBe 150
                bounds(half.right) shouldBe listOf(WRectangle(160, 0, 140, 100))
            }
            val all = scene { it.resizeWeight = 1.0 }
            shown(all) {
                onUiThreadGet { all.pane.dividerLocation } shouldBe 240
            }
        }

        test("setting dividerLocation changes the layout, lastDividerLocation is the value before setting, and listeners are called once with the new value") {
            val s = scene()
            shown(s) {
                val seen = mutableListOf<Pair<Any?, Int>>()
                onUiThread {
                    s.pane.addChangeListener { e -> seen += e.source to s.pane.dividerLocation }
                    s.pane.dividerLocation = 100
                }
                settle(s.pane)
                seen shouldBe listOf<Pair<Any?, Int>>(s.pane to 100)
                onUiThreadGet { s.pane.lastDividerLocation } shouldBe 60
                bounds(s.left, s.pane.divider, s.right) shouldBe listOf(
                    WRectangle(0, 0, 100, 100),
                    WRectangle(100, 0, 10, 100),
                    WRectangle(110, 0, 190, 100),
                )
                onUiThread { s.pane.dividerLocation = 100 } // The same value does not call the listeners
                seen.size shouldBe 1
            }
        }

        test("a dividerLocation beyond the range is corrected by layout to (width - divider), and the correction does not change lastDividerLocation") {
            val s = scene()
            shown(s) {
                val seen = mutableListOf<Int>()
                onUiThread {
                    s.pane.addChangeListener { seen += s.pane.dividerLocation }
                    s.pane.dividerLocation = 1000
                }
                settle(s.pane)
                // 1000 fits into 300 - 10 = 290. Notified twice: the setting (60 → 1000) and the correction (1000 → 290)
                seen shouldBe listOf(1000, 290)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 290
                onUiThreadGet { s.pane.lastDividerLocation } shouldBe 60
                bounds(s.left) shouldBe listOf(WRectangle(0, 0, 290, 100))
            }
        }

        test("a negative dividerLocation and resetToPreferredSizes redecide from the preferred sizes") {
            val s = scene()
            shown(s) {
                onUiThread { s.pane.dividerLocation = 200 }
                settle(s.pane)
                onUiThread { s.pane.resetToPreferredSizes() }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 60
                onUiThread { s.pane.dividerLocation = 200 }
                onUiThread { s.pane.dividerLocation = -1 }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 60
                bounds(s.left) shouldBe listOf(WRectangle(0, 0, 60, 100))
            }
        }

        test("setDividerLocation(ratio) is (width - divider) x ratio (truncated), and when called before being shown it is decided on the first layout") {
            val s = scene()
            shown(s) {
                onUiThread { s.pane.setDividerLocation(0.25) }
                // (300 - 10) * 0.25 = 72.5 → 72
                onUiThreadGet { s.pane.dividerLocation } shouldBe 72
                onUiThreadGet { s.pane.lastDividerLocation } shouldBe 60
            }
            val early = scene { it.setDividerLocation(0.5) }
            onUiThreadGet { early.pane.dividerLocation } shouldBe -1
            shown(early) {
                // (300 - 10) * 0.5 = 145
                onUiThreadGet { early.pane.dividerLocation } shouldBe 145
                bounds(early.left) shouldBe listOf(WRectangle(0, 0, 145, 100))
            }
        }

        test("ratios outside 0.0 to 1.0, an out-of-range resizeWeight and other orientation values throw IllegalArgumentException") {
            onUiThread {
                val pane = WSplitPane()
                shouldThrow<IllegalArgumentException> { pane.setDividerLocation(-0.1) }
                shouldThrow<IllegalArgumentException> { pane.setDividerLocation(1.1) }
                shouldThrow<IllegalArgumentException> { pane.resizeWeight = -0.01 }
                shouldThrow<IllegalArgumentException> { pane.resizeWeight = 1.01 }
                shouldThrow<IllegalArgumentException> { pane.orientation = 2 }
                shouldThrow<IllegalArgumentException> { WSplitPane(-1) }
                pane.resizeWeight shouldBe 0.0
                pane.orientation shouldBe WSplitPane.HORIZONTAL_SPLIT
            }
        }

        test("when the panel grows, the increase is given to the left by the resizeWeight ratio (0 / 0.5 / 1.0)") {
            for ((weight, expected) in listOf(0.0 to 100, 0.5 to 150, 1.0 to 200)) {
                val s = scene { it.resizeWeight = weight }
                shown(s) {
                    val initial = onUiThreadGet { s.pane.dividerLocation }
                    onUiThread { s.pane.dividerLocation = 100 }
                    settle(s.pane)
                    onUiThread { s.pane.width = 400.0 }
                    settle(s.pane)
                    // Of the increase of 100, (int) (weight * 100) goes to the left
                    onUiThreadGet { s.pane.dividerLocation } shouldBe expected
                    bounds(s.right) shouldBe listOf(WRectangle(expected + 10, 0, 400 - expected - 10, 100))
                    onUiThreadGet { s.pane.lastDividerLocation } shouldBe initial // Still the position before setting (distribution does not change it)
                }
            }
        }

        test("when shrinking, the other side yields so as not to cut into the minimum size") {
            val s = scene {
                it.resizeWeight = 1.0
                it.leftComponent!!.minimumSize = WSize(40.0, 0.0)
            }
            shown(s) {
                onUiThread { s.pane.dividerLocation = 100 }
                settle(s.pane)
                onUiThread { s.pane.width = 200.0 }
                settle(s.pane)
                // The decrease of 100 all goes to the left: left 0, right 190. That cuts into the left minimum 40, so the right yields from 190 - 40 = 150 (at least the right minimum 0)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 40
                bounds(s.right) shouldBe listOf(WRectangle(50, 0, 150, 100))
            }
        }

        test("a vertical split distributes heights to top and bottom, and topComponent / bottomComponent are the same as left / right") {
            val s = scene(WSplitPane.VERTICAL_SPLIT, width = 100.0, height = 300.0)
            shown(s) {
                onUiThreadGet { s.pane.topComponent } shouldBeSameInstanceAs s.left
                onUiThreadGet { s.pane.bottomComponent } shouldBeSameInstanceAs s.right
                // The top's preferred height is 30: top 0..30, divider 30..40, bottom 40..300
                onUiThreadGet { s.pane.dividerLocation } shouldBe 30
                bounds(s.left, s.pane.divider, s.right) shouldBe listOf(
                    WRectangle(0, 0, 100, 30),
                    WRectangle(0, 30, 100, 10),
                    WRectangle(0, 40, 100, 260),
                )
                // Changing the orientation keeps the position
                onUiThread { s.pane.orientation = WSplitPane.HORIZONTAL_SPLIT }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 30
                bounds(s.right) shouldBe listOf(WRectangle(40, 0, 60, 300))
            }
        }

        test("changing dividerSize changes the divider thickness and keeps the position") {
            val s = scene()
            shown(s) {
                onUiThread {
                    s.pane.dividerLocation = 100
                    s.pane.dividerSize = 4
                }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 100
                bounds(s.pane.divider, s.right) shouldBe listOf(WRectangle(100, 0, 4, 100), WRectangle(104, 0, 196, 100))
            }
        }

        test("when there is only one child or one is not shown, that child uses everything except the divider (reflected with revalidate)") {
            val s = scene()
            shown(s) {
                onUiThread {
                    s.pane.dividerLocation = 100
                    s.right.isVisible = false
                    s.pane.revalidate()
                }
                settle(s.pane)
                // The right is disabled: left = 300 - 10 = 290
                onUiThreadGet { s.pane.dividerLocation } shouldBe 290
                onUiThread {
                    s.right.isVisible = true
                    s.left.isVisible = false
                    s.pane.revalidate()
                }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 0
                bounds(s.right) shouldBe listOf(WRectangle(10, 0, 290, 100))
            }
        }

        test("children point to this panel with parent, putting the right child on the left removes it from the right, and remove removes them") {
            onUiThread {
                val a = WLabel("a")
                val b = WLabel("b")
                val pane = WSplitPane(WSplitPane.HORIZONTAL_SPLIT, a, b)
                a.parent shouldBeSameInstanceAs pane
                b.parent shouldBeSameInstanceAs pane
                pane.leftComponent = b
                pane.leftComponent shouldBeSameInstanceAs b
                pane.rightComponent.shouldBeNull()
                a.parent.shouldBeNull()
                val panel = WPanel()
                panel.add(b) // Adding it to another parent removes it from this panel (as in Swing)
                pane.leftComponent.shouldBeNull()
                pane.rightComponent = a
                pane.remove(a)
                pane.rightComponent.shouldBeNull()
                a.parent.shouldBeNull()
            }
        }

        test("dragging the divider stays within the minimum sizes, and when it ends lastDividerLocation is the position before the drag") {
            val s = scene {
                it.leftComponent!!.minimumSize = WSize(40.0, 0.0)
                it.rightComponent!!.minimumSize = WSize(50.0, 0.0)
            }
            shown(s) {
                val mouse = TestMouse()
                val divider = listOf(s.pane.divider)
                val seen = mutableListOf<Int>()
                onUiThread {
                    s.pane.dividerLocation = 100
                    s.pane.addChangeListener { seen += s.pane.dividerLocation }
                }
                settle(s.pane)
                onUiThreadGet { s.pane.minimumDividerLocation } shouldBe 40
                onUiThreadGet { s.pane.maximumDividerLocation } shouldBe 240 // 300 - 50 - 10
                // Grab x = 5 inside the divider and move to 25: the divider's left edge is 100 + 25 - 5 = 120
                onUiThread {
                    mouse.press(5.0, 50.0, WMouseEvent.BUTTON1, divider)
                    mouse.move(25.0, 50.0, divider)
                }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 120
                bounds(s.pane.divider) shouldBe listOf(WRectangle(120, 0, 10, 100))
                // Going too far left stops at the left minimum 40, and too far right stops at 240
                onUiThread { mouse.move(-300.0, 50.0, divider) }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 40
                onUiThread { mouse.move(500.0, 50.0, divider) }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 240
                onUiThread { mouse.release(500.0, 50.0, WMouseEvent.BUTTON1, divider) }
                settle(s.pane)
                seen shouldBe listOf(120, 40, 240)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 240
                onUiThreadGet { s.pane.lastDividerLocation } shouldBe 100
                mouse.captures.first() shouldBe "capture:" + onUiThreadGet { s.pane.divider.name }
            }
        }

        test("without continuous layout, only the shadow moves while dragging, and the position changes on release") {
            val s = scene { it.isContinuousLayout = false }
            shown(s) {
                val mouse = TestMouse()
                val divider = listOf(s.pane.divider)
                onUiThread { s.pane.dividerLocation = 100 }
                settle(s.pane)
                onUiThread {
                    mouse.press(5.0, 50.0, WMouseEvent.BUTTON1, divider)
                    mouse.move(25.0, 50.0, divider)
                }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 100
                onUiThreadGet { s.pane.dragIndicator.isVisible }.shouldBeTrue()
                bounds(s.pane.dragIndicator, s.left) shouldBe listOf(WRectangle(120, 0, 10, 100), WRectangle(0, 0, 100, 100))
                onUiThread { mouse.release(25.0, 50.0, WMouseEvent.BUTTON1, divider) }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 120
                onUiThreadGet { s.pane.lastDividerLocation } shouldBe 100
                onUiThreadGet { s.pane.dragIndicator.isVisible }.shouldBeFalse()
                bounds(s.left) shouldBe listOf(WRectangle(0, 0, 120, 100))
            }
        }

        test("the right button does not drag the divider") {
            val s = scene()
            shown(s) {
                val mouse = TestMouse()
                val divider = listOf(s.pane.divider)
                onUiThread {
                    mouse.press(5.0, 50.0, WMouseEvent.BUTTON3, divider)
                    mouse.move(55.0, 50.0, divider)
                    mouse.release(55.0, 50.0, WMouseEvent.BUTTON3, divider)
                }
                settle(s.pane)
                onUiThreadGet { s.pane.dividerLocation } shouldBe 60
            }
        }

        test("the mouse pointer over the divider is a left-right arrow in a horizontal split (ProtectedCursor is set)") {
            onUiThread {
                val pane = WSplitPane()
                val protectedElement = pane.divider.uiElement.queryInterface(XamlInterop.IID_IUIElementProtected)
                try {
                    val cursor = protectedElement.getPtrOrNull(XamlInterop.IUIElementProtected_get_ProtectedCursor)
                    (cursor != null).shouldBeTrue()
                    cursor?.release()
                } finally {
                    protectedElement.release()
                }
            }
        }
        context("Swing compatibility") {
            test("isEnabled defaults to true. While false, dragging the divider does not change the position, and setting it back to true allows dragging (the same as JSplitPane.setEnabled)") {
                val s = scene()
                shown(s) {
                    val mouse = TestMouse()
                    val divider = listOf(s.pane.divider)
                    fun drag() = onUiThread {
                        mouse.press(5.0, 50.0, WMouseEvent.BUTTON1, divider)
                        mouse.move(25.0, 50.0, divider)
                        mouse.release(25.0, 50.0, WMouseEvent.BUTTON1, divider)
                    }
                    onUiThread {
                        s.pane.isEnabled.shouldBeTrue()
                        s.pane.dividerLocation = 100
                        s.pane.isEnabled = false
                    }
                    settle(s.pane)
                    drag()
                    settle(s.pane)
                    onUiThreadGet { s.pane.dividerLocation } shouldBe 100
                    onUiThread { s.pane.isEnabled = true }
                    drag()
                    settle(s.pane)
                    onUiThreadGet { s.pane.dividerLocation } shouldBe 120
                }
            }

            test("setting dividerLocation works even with isEnabled = false (as in Swing, only user drags are stopped)") {
                onUiThread {
                    val pane = WSplitPane(WSplitPane.HORIZONTAL_SPLIT, WLabel("a"), WLabel("b"))
                    pane.isEnabled = false
                    pane.dividerLocation = 30
                    pane.dividerLocation shouldBe 30
                    pane.isEnabled.shouldBeFalse()
                }
            }

            test("removeAll removes the left and right children, and the children's parent becomes null too") {
                onUiThread {
                    val a = WLabel("a")
                    val b = WLabel("b")
                    val pane = WSplitPane(WSplitPane.VERTICAL_SPLIT, a, b)
                    pane.removeAll()
                    pane.topComponent.shouldBeNull()
                    pane.bottomComponent.shouldBeNull()
                    a.parent.shouldBeNull()
                    b.parent.shouldBeNull()
                }
            }
        }
    }
}
