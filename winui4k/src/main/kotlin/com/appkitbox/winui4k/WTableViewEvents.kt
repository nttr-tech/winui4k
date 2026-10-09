package com.appkitbox.winui4k

import com.appkitbox.winui4k.table.SortOrder
import com.appkitbox.winui4k.table.TableModel
import kotlin.jvm.JvmSynthetic

/**
 * The row selection mode of [WTableView] (TableViewSelectionMode). Values are extracted from winmd.
 * The experimental TableView supports only single selection (multiple selection is planned).
 */
enum class TableSelectionMode(internal val native: Int) {
    /** No selection (a display-only table). */
    NONE(0),

    /** Only one row can be selected (default). */
    SINGLE(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): TableSelectionMode = entries.first { it.native == native }
    }
}

/** The row density of [WTableView] (TableViewDensity). Values are extracted from winmd. */
enum class TableDensity(internal val native: Int) {
    /** Compact display (row height 30px). */
    COMPACT(0),

    /** Standard (default). */
    STANDARD(1),

    /** Spacious display. */
    COMFORTABLE(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): TableDensity = entries.first { it.native == native }
    }
}

/** How cell editing ends (TableViewEditAction). Values are extracted from winmd. */
enum class TableEditAction(internal val native: Int) {
    /** Commit (writes the edited value back to the model). */
    COMMIT(0),

    /** Cancel (discards the edited value). */
    CANCEL(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): TableEditAction = entries.first { it.native == native }
    }
}

/**
 * The event for the start (TableView.BeginningEdit) and end (TableView.CellEditEnding) of cell editing.
 * Setting [isCanceled] to true prevents editing from starting, or for the end event keeps the edit going (e.g. to
 * keep it open on a validation error).
 */
class TableCellEditEvent internal constructor(
    /** The row of the cell being edited (view index). */
    val row: Int,
    /** The column of the cell being edited (view index). */
    val column: Int,
    /** The row of the cell being edited (model index). */
    val modelRow: Int,
    /** The column of the cell being edited (model index). */
    val modelColumn: Int,
    /** How editing ends, for the end event. null for the start event. */
    val action: TableEditAction?,
) {
    /** Setting this to true cancels the start or end. */
    var isCanceled: Boolean = false

    override fun toString(): String =
        "TableCellEditEvent(row=$row, column=$column, modelRow=$modelRow, modelColumn=$modelColumn, action=$action)"
}

/** A listener that receives the start and end of cell editing. */
fun interface TableCellEditListener {
    /** Called just before editing starts or ends. */
    fun editing(event: TableCellEditEvent)
}

/**
 * The event for sorting a column. Before sorting (TableView.Sorting), setting [isCanceled] to true cancels the sort;
 * after sorting (TableView.Sorted), it carries the resulting direction.
 */
class TableSortEvent internal constructor(
    /** The column to sort (view index). */
    val column: Int,
    /** The column to sort (model index). */
    val modelColumn: Int,
    /** The sort direction. [SortOrder.UNSORTED] clears the sort. */
    val sortOrder: SortOrder,
) {
    /** Setting this to true cancels the sort (effective only for the event before sorting). */
    var isCanceled: Boolean = false

    override fun toString(): String = "TableSortEvent(column=$column, modelColumn=$modelColumn, sortOrder=$sortOrder)"
}

/** A listener that receives events before and after a column is sorted. */
fun interface TableSortListener {
    /** Called before or after sorting. */
    fun sorting(event: TableSortEvent)
}

/**
 * A function that determines the grouping key of a row ([WTableView.groupBy]).
 * Rows that return the same key are gathered into one group. Keys are compared by value, such as strings, numbers and
 * booleans (other objects are compared by their toString() string).
 */
fun interface TableGroupKey {
    /** The group key of row [modelRow] of [model]. */
    fun groupKey(model: TableModel, modelRow: Int): Any?
}
