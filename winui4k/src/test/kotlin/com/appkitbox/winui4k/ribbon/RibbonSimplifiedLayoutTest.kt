package com.appkitbox.winui4k.ribbon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/** Tests for simplified ribbon overflow (RibbonSimplifiedLayout). Ported from RibbonSpace's SimplifiedLayoutTests. */
class RibbonSimplifiedLayoutTest : FunSpec() {
    private fun group(vararg widths: Double) =
        RibbonSimplifiedGroup(widths.map { RibbonSimplifiedItem(it, RibbonSimplifiedVisibility.AUTO) })

    init {
        test("when everything fits, all items are in the line and no overflow button is shown") {
            val result = RibbonSimplifiedLayout.compute(listOf(group(30.0, 30.0), group(30.0)), 200.0, 32.0)
            result.hasOverflow shouldBe false
            result.inLine shouldBe listOf(listOf(true, true), listOf(true))
            result.totalWidth shouldBe 90.0
        }

        test("removes items from the end of the rightmost group and fits the line including the overflow button width") {
            val result = RibbonSimplifiedLayout.compute(listOf(group(30.0, 30.0), group(30.0, 30.0)), 110.0, 20.0)
            result.hasOverflow shouldBe true
            result.inLine[0] shouldBe listOf(true, true)
            result.inLine[1] shouldBe listOf(true, false)
            result.totalWidth shouldBe 110.0
        }

        test("PINNED items are removed last, and OVERFLOW items are never in the line") {
            val groups = listOf(
                RibbonSimplifiedGroup(
                    listOf(
                        RibbonSimplifiedItem(30.0, RibbonSimplifiedVisibility.PINNED),
                        RibbonSimplifiedItem(30.0, RibbonSimplifiedVisibility.AUTO),
                        RibbonSimplifiedItem(30.0, RibbonSimplifiedVisibility.OVERFLOW),
                    ),
                ),
            )
            val result = RibbonSimplifiedLayout.compute(groups, 60.0, 20.0)
            result.inLine[0] shouldBe listOf(true, false, false)
            result.hasOverflow shouldBe true
        }

        test("HIDDEN items count neither toward the line nor the overflow") {
            val groups = listOf(
                RibbonSimplifiedGroup(
                    listOf(
                        RibbonSimplifiedItem(30.0, RibbonSimplifiedVisibility.HIDDEN),
                        RibbonSimplifiedItem(30.0, RibbonSimplifiedVisibility.AUTO),
                    ),
                ),
            )
            val result = RibbonSimplifiedLayout.compute(groups, 100.0, 20.0)
            result.inLine[0] shouldBe listOf(false, true)
            result.hasOverflow shouldBe false
            result.totalWidth shouldBe 30.0
        }

        test("groups with a higher reduction order are removed first") {
            val groups = listOf(
                RibbonSimplifiedGroup(listOf(RibbonSimplifiedItem(30.0, RibbonSimplifiedVisibility.AUTO)), reductionOrder = 1),
                RibbonSimplifiedGroup(listOf(RibbonSimplifiedItem(30.0, RibbonSimplifiedVisibility.AUTO))),
            )
            RibbonSimplifiedLayout.compute(groups, 50.0, 20.0).inLine shouldBe listOf(listOf(false), listOf(true))
        }

        test("group spacing goes only between groups that still have items in the line") {
            val result = RibbonSimplifiedLayout.compute(listOf(group(30.0), group(30.0)), 200.0, 20.0, groupSpacing = 8.0)
            result.totalWidth shouldBe 68.0
        }

        test("if nothing else fits, even PINNED items are removed and everything goes to the overflow") {
            val groups = listOf(RibbonSimplifiedGroup(listOf(RibbonSimplifiedItem(30.0, RibbonSimplifiedVisibility.PINNED))))
            val result = RibbonSimplifiedLayout.compute(groups, 10.0, 20.0)
            result.inLine shouldBe listOf(listOf(false))
            result.totalWidth shouldBeLessThanOrEqual 20.0
        }
    }
}
