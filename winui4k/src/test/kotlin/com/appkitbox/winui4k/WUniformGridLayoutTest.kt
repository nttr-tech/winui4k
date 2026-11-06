package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for reading and writing the settings of [WUniformGridLayout] (UniformGridLayout) and the arrangement of cells when set to [WItemsView.layout].
 * The defaults are as defined by WinUI's UniformGridLayout (MUX_DEFAULT_VALUE in ItemsRepeater.idl).
 * The arrangement is checked by differences in the on-screen coordinates of the cells' top-left (compared as ratios of differences so as not to depend on scaling).
 */
class WUniformGridLayoutTest : FunSpec() {
    /** A WItemsView with 4 items. Cells are 80 × 40, row and column spacing is 20, wrapping every 2. */
    private class Scene {
        val items = List(4) { WItemContainer(WLabel("Item $it")) }
        val layout = WUniformGridLayout().also {
            it.minItemWidth = 80.0
            it.minItemHeight = 40.0
            it.minColumnSpacing = 20.0
            it.minRowSpacing = 20.0
            it.maximumRowsOrColumns = 2
        }
        val view = WItemsView().also {
            it.width = 400.0
            it.height = 300.0
            it.horizontalAlignment = HorizontalAlignment.LEFT
            it.verticalAlignment = VerticalAlignment.TOP
            it.setItems(items)
            it.layout = layout
        }
    }

    private fun <T> shown(scene: Scene, block: (Scene) -> T): T {
        UiTestHarness.attachAndAwaitLoaded(scene.view)
        try {
            onUiThread { scene.view.updateLayout() }
            return onUiThreadGet { block(scene) }
        } finally {
            UiTestHarness.detach(scene.view)
        }
    }

    init {
        test("the defaults are as WinUI defines them (horizontal, minimum size 0, spacing 0, Start, None, no wrapping limit)") {
            onUiThreadGet {
                val layout = WUniformGridLayout()
                listOf(
                    layout.orientation,
                    layout.minItemWidth,
                    layout.minItemHeight,
                    layout.minRowSpacing,
                    layout.minColumnSpacing,
                    layout.itemsJustification,
                    layout.itemsStretch,
                    layout.maximumRowsOrColumns,
                )
            } shouldBe listOf(
                Orientation.HORIZONTAL,
                0.0,
                0.0,
                0.0,
                0.0,
                UniformGridLayoutItemsJustification.START,
                UniformGridLayoutItemsStretch.NONE,
                -1,
            )
        }

        test("all set values can be read back") {
            onUiThreadGet {
                val layout = WUniformGridLayout()
                layout.orientation = Orientation.VERTICAL
                layout.minItemWidth = 80.0
                layout.minItemHeight = 40.0
                layout.minRowSpacing = 8.0
                layout.minColumnSpacing = 12.0
                layout.itemsJustification = UniformGridLayoutItemsJustification.SPACE_EVENLY
                layout.itemsStretch = UniformGridLayoutItemsStretch.UNIFORM
                layout.maximumRowsOrColumns = 3
                listOf(
                    layout.orientation,
                    layout.minItemWidth,
                    layout.minItemHeight,
                    layout.minRowSpacing,
                    layout.minColumnSpacing,
                    layout.itemsJustification,
                    layout.itemsStretch,
                    layout.maximumRowsOrColumns,
                )
            } shouldBe listOf(
                Orientation.VERTICAL,
                80.0,
                40.0,
                8.0,
                12.0,
                UniformGridLayoutItemsJustification.SPACE_EVENLY,
                UniformGridLayoutItemsStretch.UNIFORM,
                3,
            )
        }

        test("values set to ItemsView.layout can be read back, and null returns to the default") {
            onUiThread {
                val view = WItemsView()
                val layout = WUniformGridLayout()
                view.layout.shouldBeNull()
                view.layout = layout
                (view.layout === layout) shouldBe true
                view.layout = null
                view.layout.shouldBeNull()
            }
        }

        test("horizontal, 2 at a time: 0 and 1 are in the same row, 0 and 2 in the same column, and the column and row steps are (80 + 20) : (40 + 20)") {
            val scene = onUiThreadGet { Scene() }
            shown(scene) {
                val p = it.items.map { item -> item.locationOnScreen }
                // The same row has equal y, and the same column has equal x. The step ratio is 100 : 60 = 5 : 3
                val sameRow = p[0].y == p[1].y && p[2].y == p[3].y
                val sameColumn = p[0].x == p[2].x && p[1].x == p[3].x
                val ratio = (p[1].x - p[0].x) * 3 == (p[2].y - p[0].y) * 5
                Triple(sameRow, sameColumn, ratio)
            } shouldBe Triple(true, true, true)
        }

        test("making it vertical changes it so that 0 and 1 are in the same column and 0 and 2 in the same row") {
            val scene = onUiThreadGet { Scene().also { it.layout.orientation = Orientation.VERTICAL } }
            shown(scene) {
                val p = it.items.map { item -> item.locationOnScreen }
                val sameColumn = p[0].x == p[1].x && p[2].x == p[3].x
                val sameRow = p[0].y == p[2].y && p[1].y == p[3].y
                sameColumn to sameRow
            } shouldBe (true to true)
        }
    }
}
