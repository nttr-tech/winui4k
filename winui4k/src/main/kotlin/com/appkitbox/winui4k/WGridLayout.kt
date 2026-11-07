package com.appkitbox.winui4k

/**
 * java.awt.GridLayout-style: lays out children in cells of equal size in rows and columns, left to right and top to bottom in the order they were added.
 * The children's preferred sizes are ignored, and the parent's area (minus the insets) minus the gaps is divided equally among the cells.
 * If [rows] is not 0, the number of columns is determined from the number of children ([columns] is ignored); only when [rows] is 0 does [columns] become the number of columns.
 * Hidden children also take a cell (same as Swing). In right-to-left flow ([WComponent.flowDirection] is RIGHT_TO_LEFT) the columns run from the right edge.
 *
 * The preferred size takes the maximum preferred width and height of the children as the cell size, and adds up the cells, gaps and insets.
 *
 * Swing's GridLayout calculates with integers and splits any remainder that does not divide evenly half to each side, but this divides with DIP Doubles,
 * so there is no remainder. With 0 children, Swing's preferred size is negative by the gaps, but this rounds it up to 0.
 *
 * @throws IllegalArgumentException if both [rows] and [columns] are 0
 */
class WGridLayout @JvmOverloads constructor(
    rows: Int = 1,
    columns: Int = 0,
    /** The horizontal gap between cells (like GridLayout.get/setHgap). */
    var hgap: Double = 0.0,
    /** The vertical gap between cells (like GridLayout.get/setVgap). */
    var vgap: Double = 0.0,
) : WLayoutManager {
    /**
     * GridLayout(int rows, int cols, int hgap, int vgap)-like: the form taking the gaps as Int (so Kotlin can write `WGridLayout(2, 3, 4, 4)`).
     * The same as the Double form.
     */
    constructor(rows: Int, columns: Int, hgap: Int, vgap: Int) : this(rows, columns, hgap.toDouble(), vgap.toDouble())

    init {
        require(rows != 0 || columns != 0) { "rows and columns cannot both be 0" }
    }

    /** The number of rows (like GridLayout.get/setRows). If 0, determined from [columns]. Setting 0 while [columns] is also 0 throws IllegalArgumentException. */
    var rows: Int = rows
        set(value) {
            require(value != 0 || columns != 0) { "rows and columns cannot both be 0" }
            field = value
        }

    /** The number of columns (like GridLayout.get/setColumns). Not used when [rows] is not 0. Setting 0 while [rows] is also 0 throws IllegalArgumentException. */
    var columns: Int = columns
        set(value) {
            require(value != 0 || rows != 0) { "rows and columns cannot both be 0" }
            field = value
        }

    /** (rows, columns) for [count] children. If [rows] is not 0, the column count is rounded up; if 0, the row count is (same as Swing). */
    private fun dimensions(count: Int): Pair<Int, Int> =
        if (rows > 0) rows to (count + rows - 1) / rows else (count + columns - 1) / columns to columns

    private fun combinedSize(parent: WLayoutPanel, sizeOf: (WComponent) -> WSize): WSize {
        val children = parent.layoutChildren
        val (rowCount, columnCount) = dimensions(children.size)
        var cellWidth = 0.0
        var cellHeight = 0.0
        for (child in children) {
            val size = sizeOf(child)
            cellWidth = maxOf(cellWidth, size.width)
            cellHeight = maxOf(cellHeight, size.height)
        }
        val insets = parent.insets
        return WSize(
            insets.left + insets.right + maxOf(0.0, columnCount * cellWidth + (columnCount - 1) * hgap),
            insets.top + insets.bottom + maxOf(0.0, rowCount * cellHeight + (rowCount - 1) * vgap),
        )
    }

    override fun preferredLayoutSize(parent: WLayoutPanel): WSize = combinedSize(parent) { it.preferredSize() }

    /** Like GridLayout.minimumLayoutSize: sums the children's [WComponent.minimumSize] with the same formula as the preferred size. */
    override fun minimumLayoutSize(parent: WLayoutPanel): WSize = combinedSize(parent) { it.minimumSize }

    override fun layoutContainer(parent: WLayoutPanel) {
        val children = parent.layoutChildren
        if (children.isEmpty()) return
        val insets = parent.insets
        val (rowCount, columnCount) = dimensions(children.size)
        val width = parent.layoutSize.width
        val cellWidth = maxOf(0.0, (width - (insets.left + insets.right) - (columnCount - 1) * hgap) / columnCount)
        val cellHeight = maxOf(
            0.0,
            (parent.layoutSize.height - (insets.top + insets.bottom) - (rowCount - 1) * vgap) / rowCount,
        )
        val ltr = parent.flowDirection == FlowDirection.LEFT_TO_RIGHT
        for ((i, child) in children.withIndex()) {
            val row = i / columnCount
            val column = i % columnCount
            val x = if (ltr) {
                insets.left + column * (cellWidth + hgap)
            } else {
                (width - insets.right - cellWidth) - column * (cellWidth + hgap)
            }
            parent.setVisualBounds(child, x, insets.top + row * (cellHeight + vgap), cellWidth, cellHeight)
        }
    }
}
