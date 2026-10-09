package com.appkitbox.winui4k.table

import java.util.EventObject
import kotlin.jvm.JvmSynthetic

/**
 * javax.swing.RowSorter-like: sorts and filters the rows of a view.
 * Holds the mapping between view row indices and model row indices ([convertRowIndexToModel] / [convertRowIndexToView]).
 */
abstract class RowSorter<M> {
    /** javax.swing.RowSorter.SortKey-like: a sort key (a model column index and a direction). */
    class SortKey(
        /** The column to sort by (model index). */
        val column: Int,
        /** The sort direction. */
        val sortOrder: SortOrder,
    ) {
        override fun equals(other: Any?): Boolean =
            other is SortKey && other.column == column && other.sortOrder == sortOrder

        override fun hashCode(): Int = column * 31 + sortOrder.hashCode()

        override fun toString(): String = "SortKey(column=$column, sortOrder=$sortOrder)"
    }

    private val listeners = mutableListOf<RowSorterListener>()

    /** The target model. */
    abstract fun getModel(): M

    /** The current sort keys (the first one is the primary key). Empty if not sorted. */
    abstract fun getSortKeys(): List<SortKey>

    /** Sets the sort keys. Null or empty clears the sorting. */
    abstract fun setSortKeys(keys: List<SortKey>?)

    /** Toggles the sort direction of column [column] (model index) in the same order as clicking its column header. */
    abstract fun toggleSortOrder(column: Int)

    /** Converts a view row index to a model row index. */
    abstract fun convertRowIndexToModel(index: Int): Int

    /** Converts a model row index to a view row index. -1 for rows that are not shown. */
    abstract fun convertRowIndexToView(index: Int): Int

    /** The number of rows in the view (after filtering). */
    abstract fun getViewRowCount(): Int

    /** The number of rows in the model. */
    abstract fun getModelRowCount(): Int

    /** Subscribes to changes in sorting and filtering. */
    fun addRowSorterListener(listener: RowSorterListener) {
        listeners += listener
    }

    /** Removes a listener registered with [addRowSorterListener]. */
    fun removeRowSorterListener(listener: RowSorterListener) {
        listeners -= listener
    }

    /** Notifies that the sort keys have changed. */
    protected fun fireSortOrderChanged() {
        fire(RowSorterEvent(this, RowSorterEvent.Type.SORT_ORDER_CHANGED))
    }

    /** Notifies that the result of sorting and filtering (the order of the view's rows) has changed. */
    protected fun fireRowSorterChanged() {
        fire(RowSorterEvent(this, RowSorterEvent.Type.SORTED))
    }

    private fun fire(event: RowSorterEvent) {
        for (listener in listeners.asReversed().toList()) listener.sorterChanged(event)
    }
}

/** javax.swing.event.RowSorterEvent-like: describes a change in a [RowSorter]. */
class RowSorterEvent(
    source: RowSorter<*>,
    /** The kind of change. */
    val type: Type,
) : EventObject(source) {
    /** The kind of change. */
    enum class Type {
        /** The sort keys changed. */
        SORT_ORDER_CHANGED,

        /** The order of the view's rows (the result of sorting and filtering) changed. */
        SORTED,
    }

    /** The RowSorter that changed. */
    override fun getSource(): RowSorter<*> = super.getSource() as RowSorter<*>
}

/** javax.swing.event.RowSorterListener-like: receives notifications of changes in a [RowSorter]. */
fun interface RowSorterListener {
    /** Called when sorting or filtering changes. */
    fun sorterChanged(event: RowSorterEvent)
}

/**
 * The entry point through which [TableRowSorter] delegates work to the WTableView that is showing it
 * (the computation lives on the WTableView side, because WinUI's TableView does the sorting and filtering).
 */
internal interface RowSorterHost {
    fun sortKeysChanged(keys: List<RowSorter.SortKey>)

    fun toggleSortOrder(column: Int)

    fun currentSortKeys(): List<RowSorter.SortKey>

    fun rowFilterChanged()

    fun comparatorChanged(column: Int)

    fun sortableChanged(column: Int)

    fun convertRowIndexToModel(index: Int): Int

    fun convertRowIndexToView(index: Int): Int

    fun viewRowCount(): Int
}

/**
 * javax.swing.table.TableRowSorter-like: a [RowSorter] that sorts and filters the rows of a [TableModel].
 *
 * Setting it as [com.appkitbox.winui4k.WTableView.rowSorter] enables sorting by clicking column headers, and the
 * sorting and filtering are done by WinUI's TableView (TableViewSource).
 * - With a single key in [setSortKeys], it is a column sort (TableView.SortByColumn, same as clicking the column
 *   header), and columns given a comparator with [setComparator] are sorted with that comparator
 *   (TableViewColumn.CustomSortComparer). Other columns use the standard comparison for their value type (numeric
 *   order for numbers, locale-aware order for strings)
 * - With multiple keys, TableViewSource sort descriptions are stacked (TableViewSource.Sort). The first key is the
 *   primary key. In this case comparators are not used (the standard comparison applies), and the column header
 *   arrow appears only on the primary key's column
 * - Rows are filtered with the filter from [setRowFilter] (TableViewSource.Filter)
 */
class TableRowSorter<M : TableModel>(private val model: M) : RowSorter<M>() {
    private val comparators = HashMap<Int, Comparator<*>>()
    private val unsortableColumns = HashSet<Int>()
    private var rowFilter: RowFilter<in M, in Int>? = null

    /** The sort keys set before display (applied to the TableView when it is shown). */
    private var pendingSortKeys: List<SortKey> = emptyList()

    /** The WTableView that is showing it. */
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var host: RowSorterHost? = null

    /**
     * Whether rows whose values are updated are sorted and filtered again (same as Swing's
     * TableRowSorter.setSortsOnUpdates). If false, rewriting values does not move rows (the change takes effect when
     * rows are added or removed and when sorting or filtering is redone).
     */
    var sortsOnUpdates: Boolean = false

    override fun getModel(): M = model

    override fun getSortKeys(): List<SortKey> = host?.currentSortKeys() ?: pendingSortKeys

    override fun setSortKeys(keys: List<SortKey>?) {
        val newKeys = keys.orEmpty().filter { it.sortOrder != SortOrder.UNSORTED }
        pendingSortKeys = newKeys
        host?.sortKeysChanged(newKeys)
    }

    override fun toggleSortOrder(column: Int) {
        val current = host
        if (current != null) {
            current.toggleSortOrder(column)
            return
        }
        // Before display: ascending → descending → ascending (same as TableViewSortCycle's default, AscendingDescending)
        val currentOrder = pendingSortKeys.firstOrNull { it.column == column }?.sortOrder
        val next = if (currentOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
        setSortKeys(listOf(SortKey(column, next)))
    }

    /** The comparator used to sort column [column] (model index). Null reverts to the standard comparison. */
    fun setComparator(column: Int, comparator: Comparator<*>?) {
        if (comparator == null) comparators.remove(column) else comparators[column] = comparator
        host?.comparatorChanged(column)
    }

    /** The comparator for column [column]. Null if none is set. */
    fun getComparator(column: Int): Comparator<*>? = comparators[column]

    /** Whether column [column] (model index) can be sorted by clicking its column header. */
    fun setSortable(column: Int, sortable: Boolean) {
        if (sortable) unsortableColumns.remove(column) else unsortableColumns += column
        host?.sortableChanged(column)
    }

    /** Whether column [column] can be sorted. */
    fun isSortable(column: Int): Boolean = column !in unsortableColumns

    /** The filter for filtering rows. Null shows all rows. */
    fun setRowFilter(filter: RowFilter<in M, in Int>?) {
        rowFilter = filter
        host?.rowFilterChanged()
    }

    /** The current filter. */
    fun getRowFilter(): RowFilter<in M, in Int>? = rowFilter

    /** Clears all sort keys. */
    fun clearSortKeys() {
        setSortKeys(null)
    }

    override fun convertRowIndexToModel(index: Int): Int = host?.convertRowIndexToModel(index) ?: index

    override fun convertRowIndexToView(index: Int): Int = host?.convertRowIndexToView(index) ?: index

    override fun getViewRowCount(): Int = host?.viewRowCount() ?: model.getRowCount()

    override fun getModelRowCount(): Int = model.getRowCount()

    /** Whether row [modelRow] passes the filter (true if there is no filter). */
    @JvmSynthetic
    internal fun includes(modelRow: Int): Boolean {
        val filter = rowFilter ?: return true
        @Suppress("UNCHECKED_CAST") // Entry is read-only, so it can be passed to a contravariant filter
        return (filter as RowFilter<M, Int>).include(ModelEntry(model, modelRow))
    }

    /** The sort keys set before display (WTableView applies them when it is shown). */
    @JvmSynthetic
    internal fun pendingSortKeys(): List<SortKey> = pendingSortKeys

    @JvmSynthetic
    internal fun notifySortOrderChanged() {
        fireSortOrderChanged()
    }

    @JvmSynthetic
    internal fun notifySorted() {
        fireRowSorterChanged()
    }

    /** Presents one row of the model as a [RowFilter.Entry]. */
    private class ModelEntry<M : TableModel>(private val model: M, private val row: Int) : RowFilter.Entry<M, Int>() {
        override fun getModel(): M = model

        override fun getValueCount(): Int = model.getColumnCount()

        override fun getValue(index: Int): Any? = model.getValueAt(row, index)

        override fun getIdentifier(): Int = row
    }
}
