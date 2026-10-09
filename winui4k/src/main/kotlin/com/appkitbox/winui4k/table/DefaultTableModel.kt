package com.appkitbox.winui4k.table

/**
 * Like javax.swing.table.DefaultTableModel: an editable [TableModel] that holds row data in lists.
 *
 * Uses [MutableList] instead of Swing's Vector. Adding, inserting, removing, and moving rows, adding columns, and
 * changing the row or column count automatically notify the corresponding [TableModelEvent].
 * Every cell is editable ([isCellEditable] is true), and the column type is Object
 * (override [getColumnClass] if special display by type is needed).
 */
open class DefaultTableModel() : AbstractTableModel() {
    /** The row data. Each row is a list as long as the column count. */
    private val dataVector: MutableList<MutableList<Any?>> = mutableListOf()

    /** The column identifiers (the values shown as column names). */
    private val columnIdentifiers: MutableList<Any?> = mutableListOf()

    /** A model of [rowCount] rows × [columnCount] columns whose values are all null. */
    constructor(rowCount: Int, columnCount: Int) : this() {
        repeat(columnCount) { columnIdentifiers += null }
        repeat(rowCount) { dataVector += MutableList<Any?>(columnCount) { null } }
    }

    /** A model with the column names [columnNames] and [rowCount] rows whose values are all null. */
    constructor(columnNames: List<Any?>, rowCount: Int) : this() {
        columnIdentifiers.addAll(columnNames)
        repeat(rowCount) { dataVector += MutableList<Any?>(columnNames.size) { null } }
    }

    /**
     * A model with the column names [columnNames] and the row data [data] (equivalent to Swing's
     * DefaultTableModel(Vector, Vector)).
     */
    constructor(data: List<List<Any?>>, columnNames: List<Any?>) : this() {
        setDataVector(data, columnNames)
    }

    /**
     * A model with the column names [columnNames] and the row data [data] (equivalent to Swing's
     * DefaultTableModel(Object[][], Object[])).
     */
    constructor(data: Array<Array<Any?>>, columnNames: Array<Any?>) : this(
        data.map { it.toList() },
        columnNames.toList(),
    )

    /** A read-only view of the row data (a list per row). */
    fun getDataVector(): List<List<Any?>> = dataVector.map { it.toList() }

    /** Replaces the row data and column names. Notifies a change of the column structure (structure changed). */
    fun setDataVector(data: List<List<Any?>>, columnNames: List<Any?>) {
        columnIdentifiers.clear()
        columnIdentifiers.addAll(columnNames)
        dataVector.clear()
        for (row in data) dataVector += normalize(row)
        fireTableStructureChanged()
    }

    /** Replaces the column names. If the column count changes, the length of each row is adjusted too. */
    fun setColumnIdentifiers(columnNames: List<Any?>) {
        columnIdentifiers.clear()
        columnIdentifiers.addAll(columnNames)
        for (row in dataVector) resize(row, columnNames.size)
        fireTableStructureChanged()
    }

    /** Changes the column count. Added columns have null names (shown with spreadsheet-style names) and null values. */
    fun setColumnCount(columnCount: Int) {
        resize(columnIdentifiers, columnCount)
        for (row in dataVector) resize(row, columnCount)
        fireTableStructureChanged()
    }

    /** Appends a column named [columnName]. [values] are the values from the first row on (null for any missing). */
    @JvmOverloads
    fun addColumn(columnName: Any?, values: List<Any?> = emptyList()) {
        columnIdentifiers += columnName
        dataVector.forEachIndexed { index, row -> row += values.getOrNull(index) }
        fireTableStructureChanged()
    }

    /** Appends a row. Missing values up to the column count are filled with null, and extra values are dropped. */
    fun addRow(vararg rowData: Any?) {
        insertRow(dataVector.size, rowData.toList())
    }

    /** Appends a row (List version). */
    fun addRow(rowData: List<Any?>) {
        insertRow(dataVector.size, rowData)
    }

    /** Inserts a row at [row]. */
    fun insertRow(row: Int, rowData: List<Any?>) {
        dataVector.add(row, normalize(rowData))
        fireTableRowsInserted(row, row)
    }

    /** Inserts a row at [row] (array version). */
    fun insertRow(row: Int, vararg rowData: Any?) {
        insertRow(row, rowData.toList())
    }

    /** Removes row [row]. */
    fun removeRow(row: Int) {
        dataVector.removeAt(row)
        fireTableRowsDeleted(row, row)
    }

    /**
     * Moves the rows [start]..[end] so that row [start] ends up at row [to]
     * (same as Swing's DefaultTableModel.moveRow).
     */
    fun moveRow(start: Int, end: Int, to: Int) {
        require(start in 0..end && end < dataVector.size) { "invalid range: $start..$end" }
        val moved = (start..end).map { dataVector.removeAt(start) }
        dataVector.addAll(to, moved)
        val first = minOf(start, to)
        val last = maxOf(end, to + (end - start))
        fireTableRowsUpdated(first, last)
    }

    /** Changes the row count. Decreasing it removes rows from the end, and increasing it adds rows of null values. */
    fun setRowCount(rowCount: Int) {
        val old = dataVector.size
        when {
            rowCount < old -> {
                while (dataVector.size > rowCount) dataVector.removeAt(dataVector.size - 1)
                fireTableRowsDeleted(rowCount, old - 1)
            }

            rowCount > old -> {
                repeat(rowCount - old) { dataVector += MutableList<Any?>(columnIdentifiers.size) { null } }
                fireTableRowsInserted(old, rowCount - 1)
            }
        }
    }

    override fun getRowCount(): Int = dataVector.size

    override fun getColumnCount(): Int = columnIdentifiers.size

    /** The column identifier as a string. If the identifier is null, a spreadsheet-style name (A, B, ...). */
    override fun getColumnName(columnIndex: Int): String =
        columnIdentifiers.getOrNull(columnIndex)?.toString() ?: super.getColumnName(columnIndex)

    /** Every cell is editable. */
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? = dataVector[rowIndex][columnIndex]

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        dataVector[rowIndex][columnIndex] = value
        fireTableCellUpdated(rowIndex, columnIndex)
    }

    /** Creates a copy of a row fitted to the column count. */
    private fun normalize(row: List<Any?>): MutableList<Any?> =
        MutableList(columnIdentifiers.size) { row.getOrNull(it) }

    private fun resize(list: MutableList<Any?>, size: Int) {
        while (list.size > size) list.removeAt(list.size - 1)
        while (list.size < size) list += null
    }
}
