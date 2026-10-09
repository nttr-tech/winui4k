package com.appkitbox.winui4k.extension.ribbon.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/** Tests for arranging items in a classic group (RibbonGroupItemsArranger). Ported from RibbonSpace's GroupItemsArrangerTests. */
class RibbonGroupItemsArrangerTest : FunSpec() {
    init {
        test("a large item takes a whole column, and small items stack three rows per column") {
            val items = listOf(
                RibbonArrangeItem(50.0, 66.0, true),
                RibbonArrangeItem(60.0, 22.0, false),
                RibbonArrangeItem(70.0, 22.0, false),
                RibbonArrangeItem(40.0, 22.0, false),
                RibbonArrangeItem(30.0, 22.0, false),
            )
            val result = RibbonGroupItemsArranger.arrange(items, 66.0, 3, columnSpacing = 0.0)
            result.items[0].column shouldBe 0
            result.items[0].height shouldBe 66.0
            result.items[1].column shouldBe 1
            result.items[3].row shouldBe 2
            result.items[4].column shouldBe 2
            result.items[1].x shouldBe 50.0
            result.items[4].x shouldBe 120.0 // 50 + max(60, 70, 40)
            result.width shouldBe 150.0
            result.height shouldBe 66.0
        }

        test("a large item breaks the columns of small items") {
            val items = listOf(
                RibbonArrangeItem(40.0, 22.0, false),
                RibbonArrangeItem(50.0, 66.0, true),
                RibbonArrangeItem(40.0, 22.0, false),
            )
            RibbonGroupItemsArranger.arrange(items, 66.0, 3, 0.0).items.map { it.column } shouldBe listOf(0, 1, 2)
        }

        test("two-row layout and column breaks via startsNewColumn") {
            val items = listOf(
                RibbonArrangeItem(40.0, 30.0, false),
                RibbonArrangeItem(40.0, 30.0, false),
                RibbonArrangeItem(40.0, 30.0, false),
                RibbonArrangeItem(40.0, 30.0, false, startsNewColumn = true),
            )
            val result = RibbonGroupItemsArranger.arrange(items, 60.0, 2, 0.0)
            result.items.map { it.column } shouldBe listOf(0, 0, 1, 2)
            result.items[1].y shouldBe 30.0
        }

        test("distributeRows spreads out vertically a column with fewer items than rows") {
            val items = listOf(RibbonArrangeItem(40.0, 22.0, false))
            RibbonGroupItemsArranger.arrange(items, 66.0, 3).items[0].y shouldBe 0.0
            RibbonGroupItemsArranger.arrange(items, 66.0, 3, distributeRows = true).items[0].y shouldBeGreaterThan 0.0
        }

        test("an item shorter than the row is vertically centered in it, and a taller item is shrunk to the row height") {
            val items = listOf(RibbonArrangeItem(40.0, 12.0, false), RibbonArrangeItem(40.0, 40.0, false))
            val result = RibbonGroupItemsArranger.arrange(items, 66.0, 3)
            result.items[0].y shouldBe 5.0 // (22 - 12) / 2
            result.items[1].y shouldBe 22.0
            result.items[1].height shouldBe 22.0
        }

        test("column spacing goes only between columns, and the row count is clamped to 1..3") {
            val items = listOf(RibbonArrangeItem(40.0, 22.0, false), RibbonArrangeItem(30.0, 22.0, false))
            val result = RibbonGroupItemsArranger.arrange(items, 66.0, rows = 1, columnSpacing = 4.0)
            result.items[1].x shouldBe 44.0
            result.width shouldBe 74.0
            RibbonGroupItemsArranger.arrange(items, 66.0, rows = 9).items.map { it.column } shouldBe listOf(0, 0)
            RibbonGroupItemsArranger.arrange(items, 66.0, rows = 0).items.map { it.column } shouldBe listOf(0, 1)
        }

        test("no items means zero width") {
            val result = RibbonGroupItemsArranger.arrange(emptyList(), 66.0)
            result.items shouldBe emptyList()
            result.width shouldBe 0.0
        }
    }
}
