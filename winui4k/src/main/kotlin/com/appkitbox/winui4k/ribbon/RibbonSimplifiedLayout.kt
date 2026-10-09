package com.appkitbox.winui4k.ribbon

/** An item of the simplified ribbon (one row). */
data class RibbonSimplifiedItem(
    /** The width within the row. */
    val width: Double,
    /** How the item is handled in the simplified ribbon. */
    val visibility: RibbonSimplifiedVisibility,
)

/** A group of the simplified ribbon. */
data class RibbonSimplifiedGroup @JvmOverloads constructor(
    /** Items. */
    val items: List<RibbonSimplifiedItem>,
    /** Groups with larger values are moved to the overflow first. */
    val reductionOrder: Int = 0,
)

/** The result of [RibbonSimplifiedLayout.compute]. */
data class RibbonSimplifiedLayoutResult(
    /** For each group and each item, true to show it in the row, or false to move it to the overflow menu. */
    val inLine: List<List<Boolean>>,
    /** Whether the overflow menu has one or more items. */
    val hasOverflow: Boolean,
    /** The width of the row (including the overflow button's width when it is shown). */
    val totalWidth: Double,
)

/**
 * Overflow of the simplified ribbon: moves items, starting from the end of the groups (in reduce order, then from the
 * right), to the [More Options] menu until the row fits. [RibbonSimplifiedVisibility.PINNED] items are moved last.
 */
object RibbonSimplifiedLayout {
    /**
     * Determines the items that stay in the row. [overflowButtonWidth] is the width of the overflow button and
     * [groupSpacing] is the spacing between groups.
     */
    @JvmStatic
    @JvmOverloads
    fun compute(
        groups: List<RibbonSimplifiedGroup>,
        availableWidth: Double,
        overflowButtonWidth: Double,
        groupSpacing: Double = 0.0,
    ): RibbonSimplifiedLayoutResult {
        val line = Line(groups, overflowButtonWidth, groupSpacing)
        val groupOrder = groups.indices.sortedWith(compareByDescending<Int> { groups[it].reductionOrder }.thenByDescending { it })
        // Candidates to remove one at a time from the end (all AUTO items first, then PINNED)
        val candidates = listOf(RibbonSimplifiedVisibility.AUTO, RibbonSimplifiedVisibility.PINNED).flatMap { pass ->
            groupOrder.flatMap { g -> groups[g].items.indices.reversed().map { i -> Triple(pass, g, i) } }
        }
        for ((pass, g, i) in candidates) {
            if (line.width() <= availableWidth) return line.result()
            line.moveToOverflow(g, i, pass)
        }
        return line.result()
    }

    /** The intermediate state of the row (which items stay in the row). */
    private class Line(
        private val groups: List<RibbonSimplifiedGroup>,
        private val overflowButtonWidth: Double,
        private val groupSpacing: Double,
    ) {
        private val inLine = groups.map { group ->
            group.items.map {
                it.visibility == RibbonSimplifiedVisibility.AUTO || it.visibility == RibbonSimplifiedVisibility.PINNED
            }.toBooleanArray()
        }
        private var hasOverflow = groups.any { group -> group.items.any { it.visibility == RibbonSimplifiedVisibility.OVERFLOW } }

        /** Moves the item to the overflow if it is still in the row and its handling is [pass]. */
        fun moveToOverflow(group: Int, item: Int, pass: RibbonSimplifiedVisibility) {
            if (inLine[group][item] && groups[group].items[item].visibility == pass) {
                inLine[group][item] = false
                hasOverflow = true
            }
        }

        fun width(): Double {
            var total = 0.0
            var visibleGroups = 0
            for ((g, group) in groups.withIndex()) {
                val widths = group.items.filterIndexed { i, _ -> inLine[g][i] }.map { it.width }
                if (widths.isNotEmpty()) visibleGroups++
                total += widths.sum()
            }
            return total + maxOf(0, visibleGroups - 1) * groupSpacing + if (hasOverflow) overflowButtonWidth else 0.0
        }

        fun result() = RibbonSimplifiedLayoutResult(inLine.map { it.toList() }, hasOverflow, width())
    }
}
