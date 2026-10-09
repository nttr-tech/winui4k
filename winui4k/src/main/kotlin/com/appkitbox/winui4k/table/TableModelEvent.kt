package com.appkitbox.winui4k.table

import java.util.EventObject

/**
 * javax.swing.event.TableModelEvent-like: describes a change to a [TableModel].
 *
 * - Rows inserted ([INSERT]) / deleted ([DELETE]) / updated ([UPDATE]), and the row range ([firstRow]..[lastRow])
 * - The updated column ([column]; [ALL_COLUMNS] means all columns)
 * - If [firstRow] is [HEADER_ROW], a change to the column structure (column count, names, and types) (structure changed)
 * - An [UPDATE] whose [lastRow] is [Int.MAX_VALUE] is a change to all data (data changed; the row count may change too)
 *
 * Rows and columns are model indices.
 */
class TableModelEvent(
    source: TableModel,
    /** The first row of the changed range (model index). [HEADER_ROW] means the column structure changed. */
    val firstRow: Int,
    /** The last row of the changed range (model index, inclusive). */
    val lastRow: Int,
    /** The changed column (model index). [ALL_COLUMNS] means all columns. */
    val column: Int,
    /** The kind of change ([INSERT] / [UPDATE] / [DELETE]). */
    val type: Int,
) : EventObject(source) {
    /** A change to all data (same as Swing's TableModelEvent(source); the row count may change too). */
    constructor(source: TableModel) : this(source, 0, Int.MAX_VALUE, ALL_COLUMNS, UPDATE)

    /**
     * An update of a single row (same as Swing's TableModelEvent(source, row)).
     * If [row] is [HEADER_ROW], it represents a change to the column structure.
     */
    constructor(source: TableModel, row: Int) : this(source, row, row, ALL_COLUMNS, UPDATE)

    /** An update of all columns of rows [firstRow]..[lastRow]. */
    constructor(source: TableModel, firstRow: Int, lastRow: Int) : this(source, firstRow, lastRow, ALL_COLUMNS, UPDATE)

    /** An update of column [column] of rows [firstRow]..[lastRow]. */
    constructor(source: TableModel, firstRow: Int, lastRow: Int, column: Int) :
        this(source, firstRow, lastRow, column, UPDATE)

    /** The model that changed. */
    override fun getSource(): TableModel = super.getSource() as TableModel

    override fun toString(): String =
        "TableModelEvent(type=$type, firstRow=$firstRow, lastRow=$lastRow, column=$column)"

    companion object {
        /** Rows were inserted. */
        const val INSERT = 1

        /** Values were updated. */
        const val UPDATE = 0

        /** Rows were deleted. */
        const val DELETE = -1

        /** The row number that represents a change to the column structure (the header row). */
        const val HEADER_ROW = -1

        /** The column number that represents all columns. */
        const val ALL_COLUMNS = -1
    }
}

/**
 * javax.swing.event.TableModelListener-like: receives change notifications from a [TableModel].
 */
fun interface TableModelListener {
    /** Called when the model changes. */
    fun tableChanged(event: TableModelEvent)
}
