package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Tests that verify a nested [WLayoutPanel] (a WLayoutPanel in a WLayoutPanel, [WBorder] or [WScrollPane]) is laid out outside with
 * its preferred size decided from the children's preferred sizes (as with nested JPanels in Swing).
 *
 * The expected values come from the [WFlowLayout] / [WGridLayout] / [WBorderLayout] calculations (in each test's comments) and the children's actual sizes after layout.
 * The user calls neither revalidate nor validate; the tests only show the window and advance the UI thread a few turns (written the same way as the samples).
 */
@Tags("E2E")
class WNestedLayoutPanelTest : FunSpec() {
    /** Shows [root], advances the UI thread until the scheduled layouts finish, then reads [measure]. */
    private fun <T> shown(root: WComponent, measure: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(root)
        try {
            repeat(SETTLE_TURNS) { onUiThread { root.updateLayout() } }
            return onUiThreadGet(measure)
        } finally {
            UiTestHarness.detach(root)
        }
    }

    /** A panel with a fixed size in place of the window content (aligned to the top left). */
    private fun frameLike(layout: WLayoutManager, width: Double = 400.0, height: Double = 220.0): WLayoutPanel =
        WLayoutPanel(layout).also {
            it.width = width
            it.height = height
            it.horizontalAlignmentInParent = HorizontalAlignment.LEFT
            it.verticalAlignmentInParent = VerticalAlignment.TOP
        }

    init {
        test("a WFlowLayout WLayoutPanel in SOUTH of a WBorderLayout gets the button height plus the vertical gaps of 5, and all buttons are visible") {
            val (root, south, ok) = onUiThreadGet {
                val ok = WButton("OK")
                val south = WLayoutPanel(WFlowLayout(WFlowLayout.RIGHT)).also {
                    it.add(ok)
                    it.add(WButton("Cancel"))
                }
                val root = frameLike(WBorderLayout()).also {
                    it.add(WLabel("Center"), WBorderLayout.CENTER)
                    it.add(south, WBorderLayout.SOUTH)
                }
                Triple(root, south, ok)
            }
            shown(root) { Triple(south.size, south.bounds.y, ok.size) }.let { (southSize, southY, okSize) ->
                okSize.height shouldBeGreaterThan 0.0
                // FlowLayout's preferred height = the child's height + vgap 5 x 2. SOUTH is placed at the bottom with the preferred height
                southSize.height shouldBe ((okSize.height + 10.0) plusOrMinus EPSILON)
                southY shouldBe Math.round(220.0 - southSize.height).toInt()
            }
        }

        test("making a label in a nested WLayoutPanel two lines tall after showing makes the outer WBorderLayout lay out again with that height") {
            val (root, north, title) = onUiThreadGet {
                val title = WLabel("Settings")
                val north = WLayoutPanel(WFlowLayout(WFlowLayout.LEFT)).also { it.add(title) }
                val root = frameLike(WBorderLayout()).also {
                    it.add(north, WBorderLayout.NORTH)
                    it.add(WLabel("Content"), WBorderLayout.CENTER)
                }
                Triple(root, north, title)
            }
            UiTestHarness.attachAndAwaitLoaded(root)
            try {
                repeat(SETTLE_TURNS) { onUiThread { root.updateLayout() } }
                val before = onUiThreadGet { north.size.height }
                onUiThread { title.fontSize = 40.0 }
                repeat(SETTLE_TURNS) { onUiThread { root.updateLayout() } }
                val (after, titleHeight) = onUiThreadGet { north.size.height to title.size.height }
                titleHeight shouldBeGreaterThan before
                after shouldBe ((titleHeight + 10.0) plusOrMinus EPSILON)
            } finally {
                UiTestHarness.detach(root)
            }
        }

        test("the height of a WLayoutPanel in NORTH of a WBorderLayout is the height of the label inside plus the vertical gaps of 5") {
            val (root, north, title) = onUiThreadGet {
                val title = WLabel("Settings").also { it.fontSize = 20.0 }
                val north = WLayoutPanel(WFlowLayout(WFlowLayout.LEFT)).also { it.add(title) }
                val root = frameLike(WBorderLayout()).also {
                    it.add(north, WBorderLayout.NORTH)
                    it.add(WLabel("Content"), WBorderLayout.CENTER)
                }
                Triple(root, north, title)
            }
            shown(root) { north.size.height to title.size.height }.let { (northHeight, titleHeight) ->
                titleHeight shouldBeGreaterThan 20.0
                northHeight shouldBe ((titleHeight + 10.0) plusOrMinus EPSILON)
            }
        }

        test("nine buttons in a WGridLayout(0, 3) WLayoutPanel inside a WFlowLayout are arranged in 3 rows and 3 columns and all visible") {
            val (root, keys, buttons) = onUiThreadGet {
                val buttons = (1..9).map { WButton(it.toString()) }
                val keys = WLayoutPanel(WGridLayout(0, 3, 4.0, 4.0)).also { panel -> buttons.forEach(panel::add) }
                val root = frameLike(WFlowLayout(), height = 350.0).also {
                    it.add(WTextField(10))
                    it.add(keys)
                }
                Triple(root, keys, buttons)
            }
            shown(root) { Triple(keys.size, buttons.map { it.size }, buttons[8].actualX to buttons[8].actualY) }
                .let { (keysSize, sizes, last) ->
                    val cell = sizes[0]
                    cell.height shouldBeGreaterThan 0.0
                    sizes[8] shouldBe cell
                    // 3 rows and 3 columns: size = 3 x cell + 2 x 4, the ninth is in row 3, column 3
                    keysSize.width shouldBe ((3.0 * cell.width + 8.0) plusOrMinus EPSILON)
                    keysSize.height shouldBe ((3.0 * cell.height + 8.0) plusOrMinus EPSILON)
                    last.first shouldBe ((2.0 * (cell.width + 4.0)) plusOrMinus EPSILON)
                    last.second shouldBe ((2.0 * (cell.height + 4.0)) plusOrMinus EPSILON)
                }
        }

        test("a WGridLayout(0, 1) WLayoutPanel in a WBorder gets the height of three check boxes, and the border surrounds it") {
            val (root, border, inner, boxes) = onUiThreadGet {
                val boxes = listOf(WCheckBox("Bold"), WCheckBox("Italic"), WCheckBox("Underline"))
                val inner = WLayoutPanel(WGridLayout(0, 1)).also { panel -> boxes.forEach(panel::add) }
                val border = WBorder(inner).also {
                    it.borderColor = WColor.GRAY
                    it.borderThickness = 1.0
                    it.padding = 8.0
                }
                val root = frameLike(WFlowLayout()).also { it.add(border) }
                listOf(root, border, inner, boxes)
            }
            @Suppress("UNCHECKED_CAST")
            val checkBoxes = boxes as List<WCheckBox>
            shown(root as WComponent) {
                Triple((border as WComponent).size, (inner as WComponent).size, checkBoxes.map { it.size })
            }.let { (borderSize, innerSize, boxSizes) ->
                val widest = boxSizes.maxOf { it.width }
                val tallest = boxSizes.maxOf { it.height }
                tallest shouldBeGreaterThan 0.0
                // GridLayout(0, 1): width = the widest child, height = 3 x the tallest child. The border adds padding 8 + line 1 outside
                // (the line thickness of 1 is snapped to physical pixels by layout rounding, so it can be off by less than 1 DIP depending on the scale)
                innerSize.width shouldBe (widest plusOrMinus EPSILON)
                innerSize.height shouldBe ((3.0 * tallest) plusOrMinus EPSILON)
                borderSize.width shouldBe ((widest + 18.0) plusOrMinus 1.0)
                borderSize.height shouldBe ((3.0 * tallest + 18.0) plusOrMinus 1.0)
            }
        }

        test("a WGridLayout(0, 1) WLayoutPanel as the content of a WScrollPane gets the height of 30 check boxes and is not squeezed into the viewport") {
            val (root, inner, boxes) = onUiThreadGet {
                val boxes = (1..30).map { WCheckBox("Option $it") }
                val inner = WLayoutPanel(WGridLayout(0, 1)).also { panel -> boxes.forEach(panel::add) }
                val scroll = WScrollPane(inner)
                val root = frameLike(WBorderLayout()).also { it.add(scroll, WBorderLayout.CENTER) }
                Triple(root, inner, boxes)
            }
            shown(root) { Triple(inner.size, boxes.map { it.size.height }, boxes[29].actualY) }.let { (innerSize, heights, lastY) ->
                val rowHeight = heights.max()
                rowHeight shouldBeGreaterThan 0.0
                // GridLayout(0, 1): height = 30 x the tallest child (taller than the viewport of 220), the 30th is below row 29
                heights.min() shouldBe rowHeight
                innerSize.height shouldBe ((30.0 * rowHeight) plusOrMinus EPSILON)
                lastY shouldBe ((29.0 * rowHeight) plusOrMinus EPSILON)
            }
        }

        test("a WLayoutPanel as the content of a WScrollPane smaller than the viewport grows to the viewport size (as with JViewport)") {
            val (root, scroll, inner) = onUiThreadGet {
                val inner = WLayoutPanel(WGridLayout(0, 1)).also { it.add(WCheckBox("Only one")) }
                val scroll = WScrollPane(inner)
                val root = frameLike(WBorderLayout()).also { it.add(scroll, WBorderLayout.CENTER) }
                Triple(root, scroll, inner)
            }
            shown(root) { inner.size to scroll.size }.let { (innerSize, scrollSize) ->
                scrollSize.height shouldBe 220.0
                innerSize.height shouldBe 220.0
            }
        }
    }

    private companion object {
        const val SETTLE_TURNS = 6

        /** The tolerance when comparing float DIPs with values calculated in double (DesiredSize etc. are float). */
        const val EPSILON = 0.01
    }
}
