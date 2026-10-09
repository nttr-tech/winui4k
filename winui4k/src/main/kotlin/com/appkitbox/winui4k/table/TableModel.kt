package com.appkitbox.winui4k.table

/**
 * javax.swing.table.TableModel-like: the model of the tabular data shown by a [com.appkitbox.winui4k.WTable].
 *
 * The view (WTable) reads cell values from this model when it needs them and writes cell edits back with
 * [setValueAt]. When the model's contents change, notify the [TableModelListener]s with a [TableModelEvent] (usually
 * through the fire methods of [AbstractTableModel]). Row and column indices are both model positions (0-based).
 */
interface TableModel {
    /** The number of rows. */
    fun getRowCount(): Int

    /** The number of columns. */
    fun getColumnCount(): Int

    /** The name of column [columnIndex] (shown in the column header by default). */
    fun getColumnName(columnIndex: Int): String

    /**
     * The type of the values in column [columnIndex]. Used to decide the default cell display (a check box for Boolean,
     * right-aligned for Number) and what to convert strings entered while editing to (Integer / Long / Double, etc.).
     */
    fun getColumnClass(columnIndex: Int): Class<*>

    /** Whether the cell can be edited. For a cell where this is false, double-clicking or pressing F2 does not start editing. */
    fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean

    /** The value of the cell. */
    fun getValueAt(rowIndex: Int, columnIndex: Int): Any?

    /** Sets the value of the cell. Also called when an edit in the view is committed. */
    fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int)

    /** Subscribes to the model's change notifications. */
    fun addTableModelListener(listener: TableModelListener)

    /** Removes a listener registered with [addTableModelListener]. */
    fun removeTableModelListener(listener: TableModelListener)
}
