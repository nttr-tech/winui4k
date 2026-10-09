package com.appkitbox.winui4k.table

import java.util.EventObject

/**
 * javax.swing.table.TableColumnModel-like: the sequence of columns ([TableColumn]) shown in a [com.appkitbox.winui4k.WTableView].
 *
 * A column's position (its column index in the view) is independent of [TableColumn.modelIndex] (the model column).
 * Swing's column margin and column selection model are not provided, because WinUI's TableView has no corresponding
 * feature.
 */
interface TableColumnModel {
    /** Adds a column at the end. */
    fun addColumn(column: TableColumn)

    /** Removes a column. */
    fun removeColumn(column: TableColumn)

    /** Moves the column at [columnIndex] to [newIndex]. */
    fun moveColumn(columnIndex: Int, newIndex: Int)

    /** The number of columns. */
    fun getColumnCount(): Int

    /** All columns (in display order). */
    fun getColumns(): List<TableColumn>

    /** The column at [columnIndex] (in display order). */
    fun getColumn(columnIndex: Int): TableColumn

    /** The position of the first column whose [TableColumn.identifier] equals [identifier]. Throws if there is none (as in Swing). */
    fun getColumnIndex(identifier: Any?): Int

    /** Subscribes to notifications of columns being added, removed, or moved. */
    fun addColumnModelListener(listener: TableColumnModelListener)

    /** Removes a listener registered with [addColumnModelListener]. */
    fun removeColumnModelListener(listener: TableColumnModelListener)
}

/**
 * javax.swing.event.TableColumnModelEvent-like: describes a column being added, removed, or moved.
 * For an addition [toIndex] is the position it was added at, for a removal [fromIndex] is its position before the
 * removal, and for a move they are the positions before and after the move.
 */
class TableColumnModelEvent(
    source: TableColumnModel,
    /** The column position before the change. */
    val fromIndex: Int,
    /** The column position after the change. */
    val toIndex: Int,
) : EventObject(source) {
    /** The column model that changed. */
    override fun getSource(): TableColumnModel = super.getSource() as TableColumnModel
}

/**
 * javax.swing.event.TableColumnModelListener-like: receives notifications of columns being added to, removed from, or
 * moved within a [TableColumnModel].
 * Override only the methods you need (the defaults do nothing).
 */
interface TableColumnModelListener {
    /** A column was added. */
    fun columnAdded(event: TableColumnModelEvent) {
        // Does nothing by default
    }

    /** A column was removed. */
    fun columnRemoved(event: TableColumnModelEvent) {
        // Does nothing by default
    }

    /** A column was moved. */
    fun columnMoved(event: TableColumnModelEvent) {
        // Does nothing by default
    }
}

/**
 * javax.swing.table.DefaultTableColumnModel-like: the standard implementation of [TableColumnModel].
 */
open class DefaultTableColumnModel : TableColumnModel {
    private val columns = mutableListOf<TableColumn>()
    private val listeners = mutableListOf<TableColumnModelListener>()

    override fun addColumn(column: TableColumn) {
        columns += column
        fire { it.columnAdded(TableColumnModelEvent(this, 0, columns.size - 1)) }
    }

    override fun removeColumn(column: TableColumn) {
        val index = columns.indexOf(column)
        if (index < 0) return
        columns.removeAt(index)
        fire { it.columnRemoved(TableColumnModelEvent(this, index, 0)) }
    }

    override fun moveColumn(columnIndex: Int, newIndex: Int) {
        require(columnIndex in columns.indices && newIndex in columns.indices) {
            "moveColumn() - Index out of range: $columnIndex -> $newIndex"
        }
        if (columnIndex != newIndex) {
            val column = columns.removeAt(columnIndex)
            columns.add(newIndex, column)
        }
        fire { it.columnMoved(TableColumnModelEvent(this, columnIndex, newIndex)) }
    }

    override fun getColumnCount(): Int = columns.size

    override fun getColumns(): List<TableColumn> = columns.toList()

    override fun getColumn(columnIndex: Int): TableColumn = columns[columnIndex]

    override fun getColumnIndex(identifier: Any?): Int {
        val index = columns.indexOfFirst { it.identifier == identifier }
        require(index >= 0) { "Identifier not found: $identifier" }
        return index
    }

    override fun addColumnModelListener(listener: TableColumnModelListener) {
        listeners += listener
    }

    override fun removeColumnModelListener(listener: TableColumnModelListener) {
        listeners -= listener
    }

    private fun fire(action: (TableColumnModelListener) -> Unit) {
        for (listener in listeners.asReversed().toList()) action(listener)
    }
}
