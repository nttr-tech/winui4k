package com.appkitbox.winui4k

/**
 * java.awt.FlowLayout-style: lays out children at their preferred sizes from left to right, wrapping to the next row when they no longer fit in the parent's width.
 * Each row is aligned left, center or right by [alignment]; the row height is that of the tallest child in the row, and shorter children are centered vertically in the row.
 * Children are separated by [hgap] and rows by [vgap], and there is also an hgap / vgap margin at the start and end of each row and outside the first and last rows.
 * Hidden children ([WComponent.isVisible] = false) are not laid out (same as Swing).
 *
 * [LEADING] / [TRAILING] and right-to-left flow ([WComponent.flowDirection] is RIGHT_TO_LEFT) use the same coordinate calculation as Swing;
 * in right-to-left flow children are laid out from the right edge. Baseline alignment (FlowLayout.setAlignOnBaseline) is not supported.
 *
 * Swing's FlowLayout calculates with integers, but this calculates with DIP Doubles (the half fraction of centering is not rounded either).
 */
class WFlowLayout @JvmOverloads constructor(
    /** How rows are aligned ([LEFT], [CENTER], [RIGHT], [LEADING], [TRAILING]). Like FlowLayout.get/setAlignment; as in Swing, the value is not validated. */
    var alignment: Int = CENTER,
    /** The horizontal gap between children and at the start and end of rows (like FlowLayout.get/setHgap). */
    var hgap: Double = DEFAULT_GAP,
    /** The vertical gap between rows and outside the first and last rows (like FlowLayout.get/setVgap). */
    var vgap: Double = DEFAULT_GAP,
) : WLayoutManager {
    /**
     * FlowLayout(int align, int hgap, int vgap)-like: the form taking the gaps as Int (so Kotlin can write `WFlowLayout(LEFT, 10, 10)`).
     * The same as the Double form.
     */
    constructor(alignment: Int, hgap: Int, vgap: Int) : this(alignment, hgap.toDouble(), vgap.toDouble())

    companion object {
        /** FlowLayout.LEFT: aligns each row to the left (0). */
        const val LEFT: Int = 0

        /** FlowLayout.CENTER: centers each row (1). The default. */
        const val CENTER: Int = 1

        /** FlowLayout.RIGHT: aligns each row to the right (2). */
        const val RIGHT: Int = 2

        /** FlowLayout.LEADING: aligns each row to the leading side (3). Left in a left-to-right flow, right in a right-to-left flow. */
        const val LEADING: Int = 3

        /** FlowLayout.TRAILING: aligns each row to the trailing side (4). Right in a left-to-right flow, left in a right-to-left flow. */
        const val TRAILING: Int = 4

        private const val DEFAULT_GAP = 5.0
    }

    /** A child to lay out and its size. */
    private class Member(val component: WComponent, val size: WSize)

    /** The children of one row ([members]), the row's left edge ([left]) and top ([top]), the spare width after laying out ([spare]), and the row height ([height]). */
    private class Row(
        val members: List<Member>,
        val left: Double,
        val top: Double,
        val spare: Double,
        val height: Double,
    )

    private fun visibleMembers(parent: WLayoutPanel, sizeOf: (WComponent) -> WSize): List<Member> =
        parent.layoutChildren.filter { it.isVisible }.map { Member(it, sizeOf(it)) }

    private fun combinedSize(parent: WLayoutPanel, sizeOf: (WComponent) -> WSize): WSize {
        val members = visibleMembers(parent, sizeOf)
        // The width is the sum of the children's widths plus the hgaps between them, and the height is the maximum height
        val width = members.sumOf { it.size.width } + hgap * maxOf(0, members.size - 1)
        val height = members.maxOfOrNull { it.size.height } ?: 0.0
        val insets = parent.insets
        return WSize(
            width + insets.left + insets.right + hgap * 2,
            height + insets.top + insets.bottom + vgap * 2,
        )
    }

    override fun preferredLayoutSize(parent: WLayoutPanel): WSize = combinedSize(parent) { it.preferredSize() }

    /** Like FlowLayout.minimumLayoutSize: sums the children's [WComponent.minimumSize] with the same formula as the preferred size. */
    override fun minimumLayoutSize(parent: WLayoutPanel): WSize = combinedSize(parent) { it.minimumSize }

    override fun layoutContainer(parent: WLayoutPanel) {
        val insets = parent.insets
        val width = parent.layoutSize.width
        val maxWidth = width - (insets.left + insets.right + hgap * 2)
        val ltr = parent.flowDirection == FlowDirection.LEFT_TO_RIGHT
        val members = visibleMembers(parent) { it.preferredSize() }
        var x = 0.0
        var y = insets.top + vgap
        var rowHeight = 0.0
        var start = 0
        for ((i, member) in members.withIndex()) {
            val size = member.size
            if (x == 0.0 || x + size.width <= maxWidth) {
                if (x > 0.0) x += hgap
                x += size.width
                rowHeight = maxOf(rowHeight, size.height)
            } else {
                val row = Row(members.subList(start, i), insets.left + hgap, y, maxWidth - x, rowHeight)
                moveRow(parent, row, ltr)
                x = size.width
                y += vgap + rowHeight
                rowHeight = size.height
                start = i
            }
        }
        moveRow(parent, Row(members.subList(start, members.size), insets.left + hgap, y, maxWidth - x, rowHeight), ltr)
    }

    /** Places the children of [row] within the spare width according to [alignment] (like FlowLayout.moveComponents). */
    private fun moveRow(parent: WLayoutPanel, row: Row, ltr: Boolean) {
        var x = row.left
        when (alignment) {
            LEFT -> if (!ltr) x += row.spare
            CENTER -> x += row.spare / 2
            RIGHT -> if (ltr) x += row.spare
            TRAILING -> x += row.spare
            // LEADING is placed at the start, so do not move it. Out-of-range values are not moved either, as in Swing
        }
        for (member in row.members) {
            val size = member.size
            val y = row.top + (row.height - size.height) / 2
            val childX = if (ltr) x else parent.layoutSize.width - x - size.width
            parent.setVisualBounds(member.component, childX, y, size.width, size.height)
            x += size.width + hgap
        }
    }
}
