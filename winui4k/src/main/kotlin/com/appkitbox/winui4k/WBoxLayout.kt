package com.appkitbox.winui4k

/**
 * javax.swing.BoxLayout-style: lays out children in a single line. [X_AXIS] lays them out left to right, [Y_AXIS] top to bottom,
 * [LINE_AXIS] in the direction of text (horizontal; from the right in a right-to-left flow), and [PAGE_AXIS] in the direction lines advance (vertical).
 *
 * The calculation is a port of Swing's BoxLayout and SizeRequirements: along the layout direction the children's preferred sizes are added, spare space is distributed in proportion
 * to how much each can grow up to its maximum size, and a shortfall in proportion to how much each can shrink down to its minimum size. In the crossing direction children are aligned by
 * their [WComponent.alignmentX] / [WComponent.alignmentY] and stretched within the allocation up to their maximum size. Hidden children are laid out with size 0. Gaps are made with [WBox] struts and glue.
 *
 * Differences from Swing:
 * - Swing calculates with int truncation, but this calculates with DIP Doubles
 * - An unbounded maximum size ([Double.POSITIVE_INFINITY]) is treated as Short.MAX_VALUE (32767), as for Swing's glue and JPanel
 *   (the ratio of the growth margins cannot be computed with infinity)
 * - Children without a minimum size set (0 x 0) are treated as having their preferred size as the minimum. Swing's JButton, JLabel and others have minimum size =
 *   preferred size, and with WinUI4K's default minimum size of 0 they would shrink below their preferred size and break the alignment calculation (which uses the minimum size)
 * - Labels and buttons ([WLabel] and subclasses of [WButtonBase]) without a maximum size set are not made larger than their preferred size, as with JLabel / AbstractButton.
 *   Other components are unbounded and stretch to fill the crossing direction (same as JPanel and JTextField)
 * - An out-of-range [axis] or use with another panel throws IllegalArgumentException instead of Swing's AWTError
 *
 * @param target the [WLayoutPanel] that uses this layout (dedicated to one panel, as in Swing)
 * @param axis the layout direction ([X_AXIS], [Y_AXIS], [LINE_AXIS], [PAGE_AXIS])
 */
class WBoxLayout(target: WLayoutPanel, axis: Int) : WLayoutManager {
    /** Like BoxLayout.getTarget: the panel that uses this layout. */
    val target: WLayoutPanel = target

    /** Like BoxLayout.getAxis: the layout direction. */
    val axis: Int = axis

    init {
        require(axis in X_AXIS..PAGE_AXIS) { "BoxLayout axis out of range: $axis" }
    }

    /** The per-child requirements and the overall requirements for the two directions. */
    private class Requests(
        val x: List<BoxRequirements>,
        val y: List<BoxRequirements>,
        val xTotal: BoxRequirements,
        val yTotal: BoxRequirements,
    )

    /** Like BoxLayout.invalidateLayout. The calculation is redone every time, so this only checks the panel. */
    override fun invalidateLayout(parent: WLayoutPanel) {
        checkContainer(parent)
    }

    override fun preferredLayoutSize(parent: WLayoutPanel): WSize = withInsets(parent) { it.preferred }

    override fun minimumLayoutSize(parent: WLayoutPanel): WSize = withInsets(parent) { it.minimum }

    override fun maximumLayoutSize(parent: WLayoutPanel): WSize = withInsets(parent) { it.maximum }

    override fun getLayoutAlignmentX(parent: WLayoutPanel): Float {
        checkContainer(parent)
        return requests(parent).xTotal.alignment.toFloat()
    }

    override fun getLayoutAlignmentY(parent: WLayoutPanel): Float {
        checkContainer(parent)
        return requests(parent).yTotal.alignment.toFloat()
    }

    override fun layoutContainer(parent: WLayoutPanel) {
        checkContainer(parent)
        val insets = parent.insets
        val width = parent.layoutSize.width - (insets.left + insets.right)
        val height = parent.layoutSize.height - (insets.top + insets.bottom)
        val horizontal = isHorizontal(parent)
        // Only when the direction was resolved from LINE / PAGE (absoluteAxis != axis), reverse the order and alignment by the text flow
        val ltr = axis == X_AXIS || axis == Y_AXIS || parent.flowDirection == FlowDirection.LEFT_TO_RIGHT
        val r = requests(parent)
        val (xs, ys) = if (horizontal) {
            tiled(width, r.x, ltr) to aligned(height, r.yTotal, r.y, true)
        } else {
            aligned(width, r.xTotal, r.x, ltr) to tiled(height, r.y, true)
        }
        parent.layoutChildren.forEachIndexed { i, child ->
            parent.setBounds(child, insets.left + xs[i].offset, insets.top + ys[i].offset, xs[i].span, ys[i].span)
        }
    }

    /** Whether the layout direction is horizontal (like BoxLayout.resolveAxis; WinUI text is always horizontal, so LINE_AXIS is horizontal and PAGE_AXIS vertical). */
    private fun isHorizontal(parent: WLayoutPanel): Boolean {
        checkContainer(parent)
        return axis == X_AXIS || axis == LINE_AXIS
    }

    private fun checkContainer(parent: WLayoutPanel) {
        require(parent === target) { "WBoxLayout can be used only with the panel it was created for (target)" }
    }

    /** Adds [WLayoutPanel.insets] to the value of the overall requirements selected by [select] (BoxLayout.preferredLayoutSize and so on). */
    private inline fun withInsets(parent: WLayoutPanel, select: (BoxRequirements) -> Double): WSize {
        checkContainer(parent)
        val r = requests(parent)
        val insets = parent.insets
        return WSize(select(r.xTotal) + insets.left + insets.right, select(r.yTotal) + insets.top + insets.bottom)
    }

    /** The per-child and overall requirements (like BoxLayout.checkRequests; Swing caches them, but this calculates them every time). */
    private fun requests(parent: WLayoutPanel): Requests {
        val x = ArrayList<BoxRequirements>()
        val y = ArrayList<BoxRequirements>()
        for (child in parent.layoutChildren) {
            val alignmentX = child.alignmentX.toDouble()
            val alignmentY = child.alignmentY.toDouble()
            if (!child.isVisible) {
                x += BoxRequirements(0.0, 0.0, 0.0, alignmentX)
                y += BoxRequirements(0.0, 0.0, 0.0, alignmentY)
                continue
            }
            val preferred = child.preferredSize()
            val minimum = child.minimumSize.let { if (it.width == 0.0 && it.height == 0.0) preferred else it }
            val maximum = child.maximumSize.let {
                if (child.maximumFollowsPreferred && it == UNBOUNDED) preferred else it
            }
            x += BoxRequirements(minimum.width, preferred.width, bounded(maximum.width), alignmentX)
            y += BoxRequirements(minimum.height, preferred.height, bounded(maximum.height), alignmentY)
        }
        return if (isHorizontal(parent)) {
            Requests(x, y, tiledTotal(x), alignedTotal(y))
        } else {
            Requests(x, y, alignedTotal(x), tiledTotal(y))
        }
    }

    companion object {
        /** Like BoxLayout.X_AXIS: lays out from left to right. */
        const val X_AXIS = 0

        /** Like BoxLayout.Y_AXIS: lays out from top to bottom. */
        const val Y_AXIS = 1

        /** Like BoxLayout.LINE_AXIS: lays out in the direction text runs (horizontal; right to left in a right-to-left flow). */
        const val LINE_AXIS = 2

        /** Like BoxLayout.PAGE_AXIS: lays out in the direction lines advance (vertical). */
        const val PAGE_AXIS = 3
    }
}

/** The position and size of one child (along either the layout direction or the crossing direction). */
private class BoxPlacement(val offset: Double, val span: Double)

/** The minimum, preferred, maximum and alignment along one direction (like SizeRequirements). */
private class BoxRequirements(val minimum: Double, val preferred: Double, val maximum: Double, val alignment: Double)

/** The value used instead of an unbounded maximum size (Swing's Short.MAX_VALUE). */
private const val MAX_SPAN = 32767.0

private val UNBOUNDED = WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

private fun bounded(maximum: Double): Double = if (maximum.isInfinite()) MAX_SPAN else maximum

/** Like SizeRequirements.getTiledSizeRequirements: the whole along the layout direction is the sum of the children, and the alignment is 0.5. */
private fun tiledTotal(children: List<BoxRequirements>): BoxRequirements = BoxRequirements(
    children.sumOf { it.minimum },
    children.sumOf { it.preferred },
    children.sumOf { it.maximum },
    0.5,
)

/**
 * Like SizeRequirements.getAlignedSizeRequirements: the whole in the crossing direction is the sum of the maxima of the parts before (ascent) and after (descent)
 * the alignment point, and the overall alignment is the ascent's share of the minimum size.
 */
private fun alignedTotal(children: List<BoxRequirements>): BoxRequirements {
    fun span(size: (BoxRequirements) -> Double): Pair<Double, Double> {
        var ascent = 0.0
        var descent = 0.0
        for (child in children) {
            val childAscent = child.alignment * size(child)
            ascent = maxOf(ascent, childAscent)
            descent = maxOf(descent, size(child) - childAscent)
        }
        return ascent to descent
    }
    val (minAscent, minDescent) = span { it.minimum }
    val (prefAscent, prefDescent) = span { it.preferred }
    val (maxAscent, maxDescent) = span { it.maximum }
    val minimum = minAscent + minDescent
    val alignment = if (minimum > 0.0) (minAscent / minimum).coerceIn(0.0, 1.0) else 0.0
    return BoxRequirements(minimum, prefAscent + prefDescent, maxAscent + maxDescent, alignment)
}

/**
 * Like SizeRequirements.calculateTiledPositions: lays out the children in order within [allocated]. If it is at least the preferred total, they are stretched in proportion to
 * how much each can grow up to its maximum size (expandedTile); if not, they are shrunk in proportion to how much each can shrink down to its minimum size (compressedTile).
 * If [forward] is false, they are placed in reverse from the end of [allocated].
 */
private fun tiled(allocated: Double, children: List<BoxRequirements>, forward: Boolean): List<BoxPlacement> {
    val min = children.sumOf { it.minimum }
    val pref = children.sumOf { it.preferred }
    val max = children.sumOf { it.maximum }
    val spans = if (allocated >= pref) {
        val factor = if (max - pref == 0.0) 0.0 else minOf(allocated - pref, max - pref) / (max - pref)
        children.map { it.preferred + factor * (it.maximum - it.preferred) }
    } else {
        val factor = if (pref - min == 0.0) 0.0 else minOf(pref - allocated, pref - min) / (pref - min)
        children.map { it.preferred - factor * (it.preferred - it.minimum) }
    }
    var total = if (forward) 0.0 else allocated
    return spans.map { span ->
        if (forward) {
            BoxPlacement(total, span).also { total += span }
        } else {
            BoxPlacement(total - span, span).also { total = maxOf(total - span, 0.0) }
        }
    }
}

/**
 * Like SizeRequirements.calculateAlignedPositions: aligns each child's alignment point with the overall alignment point and stretches it within
 * [allocated] up to its maximum size. If [normal] is false the alignment is reversed (right-to-left flow).
 */
private fun aligned(allocated: Double, total: BoxRequirements, children: List<BoxRequirements>, normal: Boolean): List<BoxPlacement> {
    val totalAlignment = if (normal) total.alignment else 1.0 - total.alignment
    val totalAscent = allocated * totalAlignment
    val totalDescent = allocated - totalAscent
    return children.map { child ->
        val alignment = if (normal) child.alignment else 1.0 - child.alignment
        val maxAscent = child.maximum * alignment
        val maxDescent = child.maximum - maxAscent
        val ascent = minOf(totalAscent, maxAscent)
        val descent = minOf(totalDescent, maxDescent)
        BoxPlacement(totalAscent - ascent, ascent + descent)
    }
}
