package com.appkitbox.winui4k.table

/**
 * Like javax.swing.table.AbstractTableModel: a skeletal implementation of [TableModel].
 *
 * Implementing the three methods [getRowCount] / [getColumnCount] / [getValueAt] gives a read-only model.
 * It provides listener management and change notifications (the fire methods), and column names default to
 * spreadsheet style (A, B, ..., Z, AA, ...).
 * To allow editing, override [isCellEditable] and [setValueAt].
 */
abstract class AbstractTableModel : TableModel {
    private val listeners = mutableListOf<TableModelListener>()

    /** A spreadsheet-style column name (0 → A, 25 → Z, 26 → AA). */
    override fun getColumnName(columnIndex: Int): String {
        val builder = StringBuilder()
        var index = columnIndex
        while (index >= 0) {
            builder.insert(0, ('A' + index % ALPHABET_SIZE))
            index = index / ALPHABET_SIZE - 1
        }
        return builder.toString()
    }

    /** The index of the column named [columnName], or -1 if there is none. */
    fun findColumn(columnName: String): Int = (0 until getColumnCount()).firstOrNull { getColumnName(it) == columnName } ?: -1

    /** Object by default (no special display or conversion by type). */
    override fun getColumnClass(columnIndex: Int): Class<*> = Any::class.java

    /** Not editable by default. */
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

    /** Does nothing by default (a non-editable model). */
    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        // The default implementation for a read-only model
    }

    override fun addTableModelListener(listener: TableModelListener) {
        listeners += listener
    }

    override fun removeTableModelListener(listener: TableModelListener) {
        listeners -= listener
    }

    /** The registered listeners. */
    fun getTableModelListeners(): List<TableModelListener> = listeners.toList()

    /** Notifies that all data changed (the row count may change). */
    fun fireTableDataChanged() {
        fireTableChanged(TableModelEvent(this))
    }

    /** Notifies that the column structure (column count, names, and types) changed. */
    fun fireTableStructureChanged() {
        fireTableChanged(TableModelEvent(this, TableModelEvent.HEADER_ROW))
    }

    /** Notifies that the rows [firstRow]..[lastRow] were inserted. */
    fun fireTableRowsInserted(firstRow: Int, lastRow: Int) {
        fireTableChanged(TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT))
    }

    /** Notifies that the values of the rows [firstRow]..[lastRow] changed. */
    fun fireTableRowsUpdated(firstRow: Int, lastRow: Int) {
        fireTableChanged(TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE))
    }

    /** Notifies that the rows [firstRow]..[lastRow] were deleted. */
    fun fireTableRowsDeleted(firstRow: Int, lastRow: Int) {
        fireTableChanged(TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE))
    }

    /** Notifies that the value of one cell changed. */
    fun fireTableCellUpdated(row: Int, column: Int) {
        fireTableChanged(TableModelEvent(this, row, row, column))
    }

    /** Notifies all registered listeners of [event] (calling the most recently registered first, as in Swing). */
    fun fireTableChanged(event: TableModelEvent) {
        for (listener in listeners.asReversed().toList()) listener.tableChanged(event)
    }

    private companion object {
        const val ALPHABET_SIZE = 26
    }
}
