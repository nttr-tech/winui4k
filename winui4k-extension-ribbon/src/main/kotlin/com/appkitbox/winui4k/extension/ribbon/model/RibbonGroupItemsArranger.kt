package com.appkitbox.winui4k.extension.ribbon.model

/** Desired size of an item passed to [RibbonGroupItemsArranger]. */
data class RibbonArrangeItem @JvmOverloads constructor(
    /** Desired width. */
    val width: Double,
    /** Desired height. */
    val height: Double,
    /** Whether it takes up an entire column (a large item). */
    val isFullHeight: Boolean,
    /** Whether to start a new column before this item. */
    val startsNewColumn: Boolean = false,
)

/** Placement of an item determined by [RibbonGroupItemsArranger]. */
data class RibbonArrangedItem(
    /** Left edge. */
    val x: Double,
    /** Top edge. */
    val y: Double,
    /** Width. */
    val width: Double,
    /** Height. */
    val height: Double,
    /** Column index. */
    val column: Int,
    /** Index of the row within the column. */
    val row: Int,
)

/** Result of [RibbonGroupItemsArranger.arrange]. */
data class RibbonArrangeResult(
    /** Placement of each input item. */
    val items: List<RibbonArrangedItem>,
    /** Total width. */
    val width: Double,
    /** Total height. */
    val height: Double,
)

/**
 * How Office arranges a classic group: full-height items take up a column, and row items are stacked from the top in
 * columns of `rows` rows (each row is `contentHeight / rows` high). A column's width is that of its widest item.
 */
object RibbonGroupItemsArranger {
    /**
     * Arranges [items] in columns.
     * [contentHeight] is the height of the item area, [rows] is the number of rows per column (1..3), and
     * [columnSpacing] is the spacing between columns. If [distributeRows] is true, columns with fewer items than rows
     * spread their items evenly vertically (if false, they are stacked from the top).
     */
    @JvmStatic
    @JvmOverloads
    fun arrange(
        items: List<RibbonArrangeItem>,
        contentHeight: Double,
        rows: Int = 3,
        columnSpacing: Double = 2.0,
        distributeRows: Boolean = false,
    ): RibbonArrangeResult {
        val rowCount = rows.coerceIn(1, 3)
        val result = arrayOfNulls<RibbonArrangedItem>(items.size)
        val columns = mutableListOf<MutableList<Int>>()
        val fullHeight = mutableListOf<Boolean>()
        var current: MutableList<Int>? = null

        for ((i, item) in items.withIndex()) {
            if (item.isFullHeight) {
                columns += mutableListOf(i)
                fullHeight += true
                current = null
                continue
            }
            if (current == null || current.size >= rowCount || item.startsNewColumn) {
                current = mutableListOf()
                columns += current
                fullHeight += false
            }
            current += i
        }

        var x = 0.0
        for ((c, column) in columns.withIndex()) {
            val width = column.maxOf { items[it].width }
            if (fullHeight[c]) {
                result[column[0]] = RibbonArrangedItem(x, 0.0, width, contentHeight, c, 0)
            } else {
                stackColumn(items, column, c, x, contentHeight, rowCount, distributeRows, result)
            }
            x += width + if (c < columns.size - 1) columnSpacing else 0.0
        }
        return RibbonArrangeResult(result.map { it!! }, maxOf(0.0, x), contentHeight)
    }

    /** Stacks the column [column] (item indices) of row items into rows and writes the placement to [result]. */
    @Suppress("LongParameterList") // Internal function that takes arrange's intermediate state (column position and row settings) as is
    private fun stackColumn(
        items: List<RibbonArrangeItem>,
        column: List<Int>,
        columnIndex: Int,
        x: Double,
        contentHeight: Double,
        rowCount: Int,
        distributeRows: Boolean,
        result: Array<RibbonArrangedItem?>,
    ) {
        val slotHeight = contentHeight / rowCount
        val gap = if (distributeRows && column.size < rowCount) (contentHeight - column.size * slotHeight) / (column.size + 1) else 0.0
        for ((r, i) in column.withIndex()) {
            val y = if (distributeRows) gap + r * (slotHeight + gap) else r * slotHeight
            val height = minOf(items[i].height, slotHeight)
            result[i] = RibbonArrangedItem(x, y + (slotHeight - height) / 2, items[i].width, height, columnIndex, r)
        }
    }
}
