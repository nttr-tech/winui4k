package com.appkitbox.winui4k

/**
 * java.awt.BorderLayout-like: positions children across 5 regions — north, south, east, west,
 * and center. North and south get the full width and their preferred height; east and west get
 * their preferred width and the remaining height; center gets everything left over.
 * Hidden children ([WComponent.isVisible] = false) are excluded from layout (same as Swing).
 */
class WBorderLayout(
    /** The horizontal gap between regions (like BorderLayout.hgap). */
    val hgap: Double = 0.0,
    /** The vertical gap between regions (like BorderLayout.vgap). */
    val vgap: Double = 0.0,
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
    }

    private val regions = mutableMapOf<Constraint, WComponent>()

    override fun addLayoutComponent(component: WComponent, constraints: Any?) {
        require(constraints is Constraint?) { "constraints must be a WBorderLayout.Constraint: $constraints" }
        regions[constraints ?: Constraint.CENTER] = component
    }

    override fun removeLayoutComponent(component: WComponent) {
        regions.values.remove(component)
    }

    /** The region's child. null if there is none, or if it's hidden and excluded from layout. */
    private fun visibleChild(region: Constraint): WComponent? =
        regions[region]?.takeIf { it.isVisible }

    override fun preferredLayoutSize(parent: WLayoutPanel): WSize {
        // The middle row's (west + center + east) width is summed; its height is the max
        var middleWidth = 0.0
        var middleHeight = 0.0
        visibleChild(Constraint.WEST)?.preferredSize()?.let { size ->
            middleWidth += size.width + hgap
            middleHeight = maxOf(middleHeight, size.height)
        }
        visibleChild(Constraint.EAST)?.preferredSize()?.let { size ->
            middleWidth += size.width + hgap
            middleHeight = maxOf(middleHeight, size.height)
        }
        visibleChild(Constraint.CENTER)?.preferredSize()?.let { size ->
            middleWidth += size.width
            middleHeight = maxOf(middleHeight, size.height)
        }
        var width = middleWidth
        var height = middleHeight
        visibleChild(Constraint.NORTH)?.preferredSize()?.let { size ->
            width = maxOf(width, size.width)
            height += size.height + vgap
        }
        visibleChild(Constraint.SOUTH)?.preferredSize()?.let { size ->
            width = maxOf(width, size.width)
            height += size.height + vgap
        }
        val insets = parent.insets
        return WSize(width + insets.left + insets.right, height + insets.top + insets.bottom)
    }

    override fun layoutContainer(parent: WLayoutPanel) {
        val insets = parent.insets
        var top = insets.top
        var bottom = parent.layoutSize.height - insets.bottom
        var left = insets.left
        var right = parent.layoutSize.width - insets.right

        visibleChild(Constraint.NORTH)?.let { child ->
            val preferredHeight = child.preferredSize().height
            parent.setBounds(child, left, top, maxOf(0.0, right - left), preferredHeight)
            top += preferredHeight + vgap
        }
        visibleChild(Constraint.SOUTH)?.let { child ->
            val preferredHeight = child.preferredSize().height
            parent.setBounds(child, left, bottom - preferredHeight, maxOf(0.0, right - left), preferredHeight)
            bottom -= preferredHeight + vgap
        }
        visibleChild(Constraint.WEST)?.let { child ->
            val preferredWidth = child.preferredSize().width
            parent.setBounds(child, left, top, preferredWidth, maxOf(0.0, bottom - top))
            left += preferredWidth + hgap
        }
        visibleChild(Constraint.EAST)?.let { child ->
            val preferredWidth = child.preferredSize().width
            parent.setBounds(child, right - preferredWidth, top, preferredWidth, maxOf(0.0, bottom - top))
            right -= preferredWidth + hgap
        }
        visibleChild(Constraint.CENTER)?.let { child ->
            parent.setBounds(child, left, top, maxOf(0.0, right - left), maxOf(0.0, bottom - top))
        }
    }
}
