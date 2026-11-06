package com.appkitbox.winui4k.table

/**
 * javax.swing.table.TableStringConverter-like: the conversion [TableRowSorter] uses to turn model values into strings ([TableRowSorter.stringConverter]).
 *
 * Unlike Swing, it affects only filtering ([RowFilter.Entry.getStringValue], [RowFilter.regexFilter] etc.), and
 * is not used for sorting (sorting is done by WinUI's TableView with a comparison per value type or [TableRowSorter.setComparator]).
 */
abstract class TableStringConverter {
    /** The string of the value at row [row], column [column] of [model] (both model indexes). Returning null is treated as an empty string. */
    abstract fun toString(model: TableModel, row: Int, column: Int): String?
}
