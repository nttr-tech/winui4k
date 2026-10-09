package com.appkitbox.winui4k.sample.filer

import com.appkitbox.winui4k.table.AbstractTableModel
import java.io.File

/**
 * The model for the file list's details view (WTableView).
 * Returns the four columns Name / Date modified / Type / Size as display strings.
 */
internal class FileTableModel : AbstractTableModel() {
    /** The files being shown (in model row order). */
    var entries: List<File> = emptyList()
        private set

    /**
     * Replaces the files to show. Since this is notified as a removal and addition of rows,
     * the new rows are also arranged in the current sort order while sorting is active.
     */
    fun setEntries(newEntries: List<File>) {
        val oldSize = entries.size
        entries = emptyList()
        if (oldSize > 0) fireTableRowsDeleted(0, oldSize - 1)
        entries = newEntries
        if (newEntries.isNotEmpty()) fireTableRowsInserted(0, newEntries.size - 1)
    }

    override fun getRowCount(): Int = entries.size

    override fun getColumnCount(): Int = COLUMN_NAMES.size

    override fun getColumnName(columnIndex: Int): String = COLUMN_NAMES[columnIndex]

    override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val file = entries[rowIndex]
        return when (columnIndex) {
            NAME_COLUMN -> file.name
            DATE_COLUMN -> formatDate(file)
            KIND_COLUMN -> formatKind(file)
            else -> formatSize(file)
        }
    }

    companion object {
        const val NAME_COLUMN = 0
        const val DATE_COLUMN = 1
        const val KIND_COLUMN = 2
        const val SIZE_COLUMN = 3

        private val COLUMN_NAMES = listOf("Name", "Date modified", "Type", "Size")
    }
}
