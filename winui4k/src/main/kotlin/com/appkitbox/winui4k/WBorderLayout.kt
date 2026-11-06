package com.appkitbox.winui4k

/**
 * java.awt.BorderLayout-like: positions children across 5 regions — north, south, east, west,
 * and center. North and south get the full width and their preferred height; east and west get
 * their preferred width and the remaining height; center gets everything left over.
 * Hidden children ([WComponent.isVisible] = false) are excluded from layout (same as Swing).
 *
 * Constraints are [Constraint]s, and from Java they can be written with constants of the same names as BorderLayout, like `WBorderLayout.NORTH`.
 * Swing's string constraints ("North", "South", "East", "West", "Center", "First", "Last", "Before", "After") are also accepted.
 * PAGE_START / PAGE_END take precedence over NORTH / SOUTH, and LINE_START / LINE_END are placed at the WEST / EAST positions when the parent's
 * [WComponent.flowDirection] is left-to-right, and at the opposite positions when it is right-to-left (same as Swing's BorderLayout).
 */
class WBorderLayout @JvmOverloads constructor(
    /** The horizontal gap between regions (like BorderLayout.get/setHgap). After changing it, rearrange with the parent's [WLayoutPanel.revalidate]. */
    var hgap: Double = 0.0,
    /** The vertical gap between regions (like BorderLayout.get/setVgap). After changing it, rearrange with the parent's [WLayoutPanel.revalidate]. */
    var vgap: Double = 0.0,
) : WLayoutManager {
    /** The region a child is placed in (like BorderLayout.NORTH and the other constraint constants). */
    enum class Constraint {
        /** The top edge (full width x preferred height). */
        NORTH,

        /** The bottom edge (full width x preferred height). */
        SOUTH,

        /** The right edge (preferred width x remaining height). */
        EAST,

        /** The left edge (preferred width x remaining height). */
        WEST,

        /** Everything left over (the default). */
        CENTER,

        /** The top edge (BorderLayout.PAGE_START). Takes precedence over [NORTH]. */
        PAGE_START,

        /** The bottom edge (BorderLayout.PAGE_END). Takes precedence over [SOUTH]. */
        PAGE_END,

        /** The start of the line (BorderLayout.LINE_START). The [WEST] position in a left-to-right flow, the [EAST] position in a right-to-left flow. */
        LINE_START,

        /** The end of the line (BorderLayout.LINE_END). The [EAST] position in a left-to-right flow, the [WEST] position in a right-to-left flow. */
        LINE_END,
        ;

        internal companion object {
            /** From Swing's string constraints (BorderLayout.NORTH = "North" and so on) to a constraint. Null for unknown strings. */
            fun ofSwingName(name: String): Constraint? = when (name) {
                "North" -> NORTH
                "South" -> SOUTH
                "East" -> EAST
                "West" -> WEST
                "Center" -> CENTER
                "First" -> PAGE_START
                "Last" -> PAGE_END
                "Before" -> LINE_START
                "After" -> LINE_END
                else -> null
            }
        }
    }

    // Constants with the same names as BorderLayout.NORTH and the others, so Java can write WBorderLayout.NORTH
    companion object {
        /** Like BorderLayout.NORTH: [Constraint.NORTH]. */
        @JvmField
        val NORTH: Constraint = Constraint.NORTH

        /** Like BorderLayout.SOUTH: [Constraint.SOUTH]. */
        @JvmField
        val SOUTH: Constraint = Constraint.SOUTH

        /** Like BorderLayout.EAST: [Constraint.EAST]. */
        @JvmField
        val EAST: Constraint = Constraint.EAST

        /** Like BorderLayout.WEST: [Constraint.WEST]. */
        @JvmField
        val WEST: Constraint = Constraint.WEST

        /** Like BorderLayout.CENTER: [Constraint.CENTER]. */
        @JvmField
        val CENTER: Constraint = Constraint.CENTER

        /** Like BorderLayout.PAGE_START (= BEFORE_FIRST_LINE): [Constraint.PAGE_START]. */
        @JvmField
        val PAGE_START: Constraint = Constraint.PAGE_START

        /** Like BorderLayout.PAGE_END (= AFTER_LAST_LINE): [Constraint.PAGE_END]. */
        @JvmField
        val PAGE_END: Constraint = Constraint.PAGE_END

        /** Like BorderLayout.LINE_START (= BEFORE_LINE_BEGINS): [Constraint.LINE_START]. */
        @JvmField
        val LINE_START: Constraint = Constraint.LINE_START

        /** Like BorderLayout.LINE_END (= AFTER_LINE_ENDS): [Constraint.LINE_END]. */
        @JvmField
        val LINE_END: Constraint = Constraint.LINE_END

        /** The 5 regions whose positions are actually determined. */
        private val PLACED = setOf(Constraint.NORTH, Constraint.SOUTH, Constraint.EAST, Constraint.WEST, Constraint.CENTER)
    }

    private val regions = mutableMapOf<Constraint, WComponent>()

    /** Converts a constraint ([Constraint], a Swing string, or null = CENTER) into a region. As in Swing, other types and unknown strings throw. */
    private fun toConstraint(constraints: Any?): Constraint = when (constraints) {
        null -> Constraint.CENTER
        is Constraint -> constraints
        is String -> requireNotNull(Constraint.ofSwingName(constraints)) { "unknown constraint: $constraints" }
        else -> throw IllegalArgumentException(
            "constraints must be a WBorderLayout.Constraint or a Swing string: $constraints",
        )
    }

    override fun addLayoutComponent(component: WComponent, constraints: Any?) {
        regions[toConstraint(constraints)] = component
    }

    override fun removeLayoutComponent(component: WComponent) {
        regions.values.removeAll { it === component }
    }

    /**
     * Like BorderLayout.getLayoutComponent(Object): the child added with [constraints] (a [Constraint] or a Swing string).
     * No reinterpretation by orientation (WEST is the child added with WEST, not LINE_START). Null if none.
     *
     * @throws IllegalArgumentException if [constraints] does not represent a region (including null; same as Swing)
     */
    fun getLayoutComponent(constraints: Any?): WComponent? {
        require(constraints != null) { "constraints is null" }
        return regions[toConstraint(constraints)]
    }

    /**
     * Like BorderLayout.getLayoutComponent(Container, Object): looks at [parent]'s [WComponent.flowDirection] and
     * returns the child actually placed at that position (PAGE_START takes precedence over NORTH, WEST is LINE_START in a left-to-right flow).
     * The region is one of NORTH / SOUTH / EAST / WEST / CENTER. Visibility is not considered.
     *
     * @throws IllegalArgumentException if [constraints] is not one of the 5 above (or a Swing string such as "North")
     */
    fun getLayoutComponent(parent: WLayoutPanel, constraints: Any?): WComponent? {
        require(constraints != null) { "constraints is null" }
        val region = toConstraint(constraints)
        require(region in PLACED) { "constraint that cannot be used as a region: $constraints" }
        return childAt(region, isLeftToRight(parent))
    }

    /**
     * Like BorderLayout.getConstraints: the region [component] was added to. Null for children not in the layout and for null.
     * Swing returns a string, but this returns a [Constraint] (Java can compare it with `WBorderLayout.NORTH`).
     */
    fun getConstraints(component: WComponent?): Constraint? {
        if (component == null) return null
        return Constraint.entries.firstOrNull { regions[it] === component }
    }

    private fun isLeftToRight(parent: WLayoutPanel): Boolean = parent.flowDirection == FlowDirection.LEFT_TO_RIGHT

    /** The child placed at the [region] position, taking orientation into account (visibility not considered; the first half of BorderLayout.getChild). */
    private fun childAt(region: Constraint, leftToRight: Boolean): WComponent? = when (region) {
        Constraint.NORTH -> regions[Constraint.PAGE_START] ?: regions[Constraint.NORTH]
        Constraint.SOUTH -> regions[Constraint.PAGE_END] ?: regions[Constraint.SOUTH]
        Constraint.WEST ->
            (if (leftToRight) regions[Constraint.LINE_START] else regions[Constraint.LINE_END])
                ?: regions[Constraint.WEST]
        Constraint.EAST ->
            (if (leftToRight) regions[Constraint.LINE_END] else regions[Constraint.LINE_START])
                ?: regions[Constraint.EAST]
        else -> regions[Constraint.CENTER]
    }

    /** The child actually placed. Null if hidden, excluding it from layout. */
    private fun visibleChild(region: Constraint, leftToRight: Boolean): WComponent? =
        childAt(region, leftToRight)?.takeIf { it.isVisible }

    /** The formula that sums the children's sizes ([sizeOf]) over the middle row (west + center + east) and north/south (shared by BorderLayout's minimum / preferred). */
    private fun combinedSize(parent: WLayoutPanel, sizeOf: (WComponent) -> WSize): WSize {
        val ltr = isLeftToRight(parent)
        // The widths of the middle row (east + west + center) are summed and the height is the maximum
        var width = 0.0
        var height = 0.0
        visibleChild(Constraint.EAST, ltr)?.let { child ->
            val size = sizeOf(child)
            width += size.width + hgap
            height = maxOf(height, size.height)
        }
        visibleChild(Constraint.WEST, ltr)?.let { child ->
            val size = sizeOf(child)
            width += size.width + hgap
            height = maxOf(height, size.height)
        }
        visibleChild(Constraint.CENTER, ltr)?.let { child ->
            val size = sizeOf(child)
            width += size.width
            height = maxOf(height, size.height)
        }
        visibleChild(Constraint.NORTH, ltr)?.let { child ->
            val size = sizeOf(child)
            width = maxOf(width, size.width)
            height += size.height + vgap
        }
        visibleChild(Constraint.SOUTH, ltr)?.let { child ->
            val size = sizeOf(child)
            width = maxOf(width, size.width)
            height += size.height + vgap
        }
        val insets = parent.insets
        return WSize(width + insets.left + insets.right, height + insets.top + insets.bottom)
    }

    override fun preferredLayoutSize(parent: WLayoutPanel): WSize = combinedSize(parent) { it.preferredSize() }

    /** Like BorderLayout.minimumLayoutSize: sums the children's [WComponent.minimumSize] with the same formula as the preferred size. */
    override fun minimumLayoutSize(parent: WLayoutPanel): WSize = combinedSize(parent) { it.minimumSize }

    override fun layoutContainer(parent: WLayoutPanel) {
        val insets = parent.insets
        val ltr = isLeftToRight(parent)
        var top = insets.top
        var bottom = parent.layoutSize.height - insets.bottom
        var left = insets.left
        var right = parent.layoutSize.width - insets.right

        visibleChild(Constraint.NORTH, ltr)?.let { child ->
            val preferredHeight = child.preferredSize().height
            parent.setBounds(child, left, top, maxOf(0.0, right - left), preferredHeight)
            top += preferredHeight + vgap
        }
        visibleChild(Constraint.SOUTH, ltr)?.let { child ->
            val preferredHeight = child.preferredSize().height
            parent.setBounds(child, left, bottom - preferredHeight, maxOf(0.0, right - left), preferredHeight)
            bottom -= preferredHeight + vgap
        }
        visibleChild(Constraint.EAST, ltr)?.let { child ->
            val preferredWidth = child.preferredSize().width
            parent.setBounds(child, right - preferredWidth, top, preferredWidth, maxOf(0.0, bottom - top))
            right -= preferredWidth + hgap
        }
        visibleChild(Constraint.WEST, ltr)?.let { child ->
            val preferredWidth = child.preferredSize().width
            parent.setBounds(child, left, top, preferredWidth, maxOf(0.0, bottom - top))
            left += preferredWidth + hgap
        }
        visibleChild(Constraint.CENTER, ltr)?.let { child ->
            parent.setBounds(child, left, top, maxOf(0.0, right - left), maxOf(0.0, bottom - top))
        }
    }
}
