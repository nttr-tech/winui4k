package com.appkitbox.winui4k.extension.ribbon.model

/**
 * Measured widths of a group in each state ([RibbonGroupState]).
 *
 * [widths] are listed in state order (large, medium, small, collapsed). If there are fewer than 4, the width of a
 * missing state is treated as NaN (that state is unavailable).
 */
data class RibbonGroupLayoutInfo @JvmOverloads constructor(
    /** Width for each state (in the order large, medium, small, collapsed). */
    val widths: List<Double>,
    /** Groups with larger values shrink first. */
    val reductionOrder: Int = 0,
    /** Whether the collapsed state can be used. */
    val canCollapse: Boolean = true,
) {
    /** Width in [state]. NaN if the state has no width (an unavailable state). */
    fun getWidth(state: RibbonGroupState): Double = widths.getOrNull(state.ordinal) ?: Double.NaN
}

/** Result of [RibbonAdaptiveLayout.compute]. */
data class RibbonAdaptiveLayoutResult(
    /** The state chosen for each group. */
    val states: List<RibbonGroupState>,
    /** Total width of the result (including the spacing between groups). */
    val totalWidth: Double,
    /** Whether the result fits in the available width. */
    val fits: Boolean,
)

/**
 * Office-style adaptive shrinking of groups. All groups start at large and, while the tab does not fit, shrink one
 * step at a time: first all groups to medium, then to small, and finally to collapsed. Within each step, groups with
 * a larger [RibbonGroupLayoutInfo.reductionOrder] shrink first, and among equal values the one on the right shrinks
 * first (like Office, starting from the rightmost group).
 * Steps that do not reduce the width are skipped.
 */
object RibbonAdaptiveLayout {
    private val REDUCTION_STEPS = listOf(RibbonGroupState.MEDIUM, RibbonGroupState.SMALL, RibbonGroupState.COLLAPSED)

    /**
     * Determines the group states for the available width [availableWidth].
     * [groups] is in display order, [spacing] is the spacing between groups, and [strategy] is how to shrink.
     */
    @JvmStatic
    @JvmOverloads
    fun compute(
        groups: List<RibbonGroupLayoutInfo>,
        availableWidth: Double,
        spacing: Double = 0.0,
        strategy: RibbonReductionStrategy = RibbonReductionStrategy.STEPWISE,
    ): RibbonAdaptiveLayoutResult {
        val reduction = Reduction(groups, spacing)
        if (reduction.total <= availableWidth || groups.isEmpty()) return reduction.result(fits = true)

        val order = groups.indices.sortedWith(compareByDescending<Int> { groups[it].reductionOrder }.thenByDescending { it })
        // STEPWISE shrinks all groups step by step; GROUP_BY_GROUP shrinks each group through all steps
        val steps = if (strategy == RibbonReductionStrategy.STEPWISE) {
            REDUCTION_STEPS.flatMap { state -> order.map { it to state } }
        } else {
            order.flatMap { index -> REDUCTION_STEPS.map { index to it } }
        }
        for ((index, state) in steps) {
            val reduced = if (strategy == RibbonReductionStrategy.STEPWISE) {
                reduction.reduceStepwise(index, state)
            } else {
                reduction.reduceGroupByGroup(index, state)
            }
            if (reduced && reduction.total <= availableWidth) return reduction.result(fits = true)
        }
        return reduction.result(fits = false, recomputeTotal = true)
    }

    /** Intermediate state of the reduction (the state of each group and the total width). */
    private class Reduction(private val groups: List<RibbonGroupLayoutInfo>, private val spacing: Double) {
        val states = Array(groups.size) { RibbonGroupState.LARGE }
        var total = computeTotal()

        /**
         * One step of STEPWISE: sets the group at [index] to [target]. Returns true if the width actually decreased.
         * Even for a step that does not reduce the width, the state is advanced as "passed" so that the next
         * (narrower) step can be applied.
         */
        fun reduceStepwise(index: Int, target: RibbonGroupState): Boolean {
            val info = groups[index]
            if (target == RibbonGroupState.COLLAPSED && !info.canCollapse) return false
            val current = info.getWidth(states[index])
            val next = info.getWidth(target)
            if (next.isNaN()) return false
            if (next >= current - 0.5) {
                if (target != RibbonGroupState.COLLAPSED && next <= current) apply(index, target, current, next)
                return false
            }
            apply(index, target, current, next)
            return true
        }

        /** One step of GROUP_BY_GROUP: sets the group at [index] to [target] unless it gets wider. Returns true if applied. */
        fun reduceGroupByGroup(index: Int, target: RibbonGroupState): Boolean {
            val info = groups[index]
            if (target == RibbonGroupState.COLLAPSED && !info.canCollapse) return false
            val current = info.getWidth(states[index])
            val next = info.getWidth(target)
            if (next.isNaN() || next > current) return false
            apply(index, target, current, next)
            return true
        }

        fun result(fits: Boolean, recomputeTotal: Boolean = false): RibbonAdaptiveLayoutResult =
            RibbonAdaptiveLayoutResult(states.toList(), if (recomputeTotal) computeTotal() else total, fits)

        private fun apply(index: Int, target: RibbonGroupState, current: Double, next: Double) {
            total -= current - next
            states[index] = target
        }

        private fun computeTotal(): Double {
            var sum = 0.0
            for (i in groups.indices) sum += groups[i].getWidth(states[i])
            return sum + maxOf(0, groups.size - 1) * spacing
        }
    }
}
