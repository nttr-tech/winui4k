package com.appkitbox.winui4k.ribbon

import com.appkitbox.winui4k.ribbon.RibbonGroupState.COLLAPSED
import com.appkitbox.winui4k.ribbon.RibbonGroupState.LARGE
import com.appkitbox.winui4k.ribbon.RibbonGroupState.MEDIUM
import com.appkitbox.winui4k.ribbon.RibbonGroupState.SMALL
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Tests for adaptive group reduction (RibbonAdaptiveLayout). A port of RibbonSpace's AdaptiveLayoutTests. */
class RibbonAdaptiveLayoutTest : FunSpec() {
    /** A group with the large, medium, small, and collapsed widths [widths]. */
    private fun group(vararg widths: Double, order: Int = 0, canCollapse: Boolean = true) =
        RibbonGroupLayoutInfo(widths.toList(), order, canCollapse)

    private val standard = group(100.0, 80.0, 60.0, 40.0)

    init {
        test("when everything fits, all groups stay large and the total width is the sum of the large widths") {
            val result = RibbonAdaptiveLayout.compute(listOf(standard, standard), 500.0)
            result.fits shouldBe true
            result.states shouldBe listOf(LARGE, LARGE)
            result.totalWidth shouldBe 200.0
        }

        test("does not shrink at a width that fits exactly (equal to the total)") {
            RibbonAdaptiveLayout.compute(listOf(standard, standard), 200.0).states shouldBe listOf(LARGE, LARGE)
        }

        test("shrinks starting from the rightmost group") {
            val result = RibbonAdaptiveLayout.compute(listOf(standard, standard), 185.0)
            result.states shouldBe listOf(LARGE, MEDIUM)
            result.fits shouldBe true
        }

        test("moves on to small only after every group is medium") {
            RibbonAdaptiveLayout.compute(listOf(standard, standard), 145.0).states shouldBe listOf(MEDIUM, SMALL)
        }

        test("groups with a larger reductionOrder shrink first regardless of their position") {
            val result = RibbonAdaptiveLayout.compute(listOf(group(100.0, 80.0, 60.0, 40.0, order = 5), standard), 185.0)
            result.states shouldBe listOf(MEDIUM, LARGE)
        }

        test("collapsing comes last, and groups with canCollapse false are not collapsed") {
            val result = RibbonAdaptiveLayout.compute(listOf(standard, group(100.0, 80.0, 60.0, 40.0, canCollapse = false)), 100.0)
            result.states shouldBe listOf(COLLAPSED, SMALL)
            result.fits shouldBe true
        }

        test("if nothing makes it fit, collapses everything and returns fits = false") {
            val result = RibbonAdaptiveLayout.compute(listOf(standard, standard), 10.0)
            result.fits shouldBe false
            result.states shouldBe listOf(COLLAPSED, COLLAPSED)
            result.totalWidth shouldBe 80.0
        }

        test("skips steps that do not reduce the width and shrinks other groups instead") {
            // Group 1 does not change width when made medium, so group 0 shrinks
            val result = RibbonAdaptiveLayout.compute(listOf(group(100.0, 70.0, 60.0, 40.0), group(100.0, 100.0, 100.0, 40.0)), 175.0)
            result.states[0] shouldBe MEDIUM
            result.states[1] shouldNotBe COLLAPSED
            result.fits shouldBe true
        }

        test("the spacing between groups is included in the total width") {
            val result = RibbonAdaptiveLayout.compute(listOf(standard, standard), 205.0, spacing = 10.0)
            result.states[1] shouldBe MEDIUM
            result.totalWidth shouldBe 190.0
        }

        test("with no groups, it counts as fitting with a total width of 0") {
            val result = RibbonAdaptiveLayout.compute(emptyList(), 0.0)
            result.fits shouldBe true
            result.states shouldBe emptyList()
            result.totalWidth shouldBe 0.0
        }

        test("groups lacking widths for some states can still be computed, and the missing states are not used") {
            val result = RibbonAdaptiveLayout.compute(
                listOf(RibbonGroupLayoutInfo(listOf(100.0, 80.0)), RibbonGroupLayoutInfo(listOf(100.0, 80.0, 40.0, 20.0))),
                120.0,
            )
            result.states.size shouldBe 2
            result.states[0] shouldNotBe SMALL
            result.states[0] shouldNotBe COLLAPSED
            RibbonGroupLayoutInfo(listOf(100.0)).getWidth(MEDIUM).isNaN() shouldBe true
        }

        test("GROUP_BY_GROUP shrinks the group with the larger reduction order all the way to collapsed before moving on to the next") {
            val groups = listOf(
                RibbonGroupLayoutInfo(listOf(200.0, 150.0, 100.0, 50.0), reductionOrder = 0),
                RibbonGroupLayoutInfo(listOf(200.0, 150.0, 100.0, 50.0), reductionOrder = 0),
                RibbonGroupLayoutInfo(listOf(200.0, 150.0, 100.0, 50.0), reductionOrder = 1),
            )
            val result = RibbonAdaptiveLayout.compute(groups, 460.0, 0.0, RibbonReductionStrategy.GROUP_BY_GROUP)
            result.fits shouldBe true
            result.states shouldBe listOf(LARGE, LARGE, COLLAPSED)
            // With the same input, STEPWISE goes through a step where all groups are medium
            RibbonAdaptiveLayout.compute(groups, 460.0, 0.0).states[0] shouldBe MEDIUM
        }

        test("even with GROUP_BY_GROUP, groups with canCollapse false stop at small") {
            val groups = listOf(group(100.0, 80.0, 60.0, 40.0, canCollapse = false))
            val result = RibbonAdaptiveLayout.compute(groups, 10.0, 0.0, RibbonReductionStrategy.GROUP_BY_GROUP)
            result.states shouldBe listOf(SMALL)
            result.fits shouldBe false
            result.totalWidth shouldBe 60.0
        }
    }
}
