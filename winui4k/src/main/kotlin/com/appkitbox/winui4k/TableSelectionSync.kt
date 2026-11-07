package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.TabularInterop

/**
 * Two-way sync between the native row selection of a [WTable] (TableView) and a [WListSelectionModel] (the core of JTable.getSelectionModel).
 *
 * TableView can select only one row (TableViewSelectionMode has only None / Single), so even if the model selects several rows or intervals,
 * it is shrunk to the single lead row (the smallest row if the lead is not selected) before being copied to the display. Indices are view rows.
 *
 * As with Swing's JTable, the same model row stays selected after sorting, filtering, and adding or removing model rows
 * (the selection of rows that disappeared from the display or were removed is cleared).
 *
 * Before the first layout (before showing, etc.), TableView's Select has no effect (the selection is dropped when it loads), so the selection copied
 * from the model during that time is copied again after layout ([LaidOutRequests]). Until it is copied again, the model is authoritative and the native selection is not copied to the model.
 *
 * WTable calls [pullFromNative] from the native SelectionChanged handler via the anchor (this object is not captured by
 * the handler). The model listener references the table strongly through this object (the table stays while the selection model is held, as in Swing).
 */
internal class TableSelectionSync(
    private val table: WTable,
    /** ITableView (owned by [table]). */
    private val tableView: ComPtr,
) {
    /** Whether a sync is in progress (so as not to copy back changes it caused itself). Also set while rows are being reordered by sorting etc. */
    private var syncing = false

    /** The native selected row (view) at the last sync. */
    private var lastNative = -1

    /** The model row of the selected row. Used to reselect the same row after sorting etc. -1 if nothing is selected. */
    private var selectedModelRow = -1

    private val modelListener = WListSelectionListener { if (!syncing) push() }

    /** Copies the selection copied before the first layout again after layout. */
    private val requests = LaidOutRequests(table)

    /** The selection model. The default is a single selection [WDefaultListSelectionModel] (Swing's JTable defaults to multiple intervals). */
    var model: WListSelectionModel = WDefaultListSelectionModel().also {
        it.setSelectionMode(WListSelectionModel.SINGLE_SELECTION)
        it.addListSelectionListener(modelListener)
    }
        set(value) {
            if (field === value) return
            field.removeListSelectionListener(modelListener)
            field = value
            value.addListSelectionListener(modelListener)
            push()
        }

    /** Copies the native selection to the model first, then runs [block] on the model. */
    fun <R> call(block: (WListSelectionModel) -> R): R {
        pullFromNative()
        return block(model)
    }

    /** The selected row (the one row copied to the display; the new selection is visible even when read from inside a model listener). -1 if none. */
    fun selectedRow(): Int {
        pullFromNative()
        return targetRow()
    }

    /** Copies the native selection to the model if it changed since the last sync (called from the SelectionChanged handler and the APIs). */
    fun pullFromNative() {
        if (syncing || requests.isPending(PUSH_REQUEST)) return
        val native = nativeRow()
        if (native == lastNative) return
        lastNative = native
        selectedModelRow = if (native < 0) -1 else table.convertRowIndexToModel(native)
        guarded {
            if (native < 0) model.clearSelection() else model.setSelectionInterval(native, native)
        }
    }

    /** Shrinks the model's selection to one row and copies it to native. */
    fun push() {
        guarded {
            val target = targetRow()
            val selected = model.getSelectedIndices()
            if (target >= 0 && !(selected.size == 1 && selected[0] == target)) model.setSelectionInterval(target, target)
            if (nativeRow() != target) {
                if (target < 0) {
                    tableView.call(TabularInterop.ITableView_DeselectAll)
                } else {
                    tableView.call(TabularInterop.ITableView_Select, target)
                }
            }
            lastNative = nativeRow()
            selectedModelRow = if (lastNative < 0) -1 else table.convertRowIndexToModel(lastNative)
        }
        pushAgainAfterLayout()
    }

    /**
     * Stops syncing during [reorder], which changes the row order (sorting, filtering, grouping), and reselects the same model row when it ends.
     */
    fun <R> reordering(reorder: () -> R): R {
        val result = guarded(reorder)
        reselect()
        return result
    }

    /** Runs [block] with syncing stopped (when reselecting with [modelRowsChanged] after rows are added or removed). */
    fun <R> quietly(block: () -> R): R = guarded(block)

    /** Clears the selection and forgets the remembered model row (model replacement; same as Swing's JTable.setModel). */
    fun reset() {
        selectedModelRow = -1
        guarded {
            tableView.call(TabularInterop.ITableView_DeselectAll)
            model.clearSelection()
            lastNative = nativeRow()
        }
        pushAgainAfterLayout()
    }

    /** The start of sorting by a column header click (Sorting). Syncing is stopped until the end ([endUserReorder]). */
    fun beginUserReorder() {
        syncing = true
    }

    /** The end of sorting by a column header click (Sorted, or canceled). Reselects the same model row. */
    fun endUserReorder() {
        syncing = false
        reselect()
    }

    /**
     * Before model rows [first]..[last] are removed, if the selected row disappears, clears the model's selection first
     * (so that listeners of the SelectionChanged the native side raises on row removal do not read the vanished row with [selectedRow]).
     */
    fun beforeModelRowsRemoved(first: Int, last: Int) {
        if (selectedModelRow in first..last) guarded { model.clearSelection() }
    }

    /** Model rows [first]..[last] were added (if [inserted] is true) or removed. Shifts or clears the selected row. */
    fun modelRowsChanged(first: Int, last: Int, inserted: Boolean) {
        val row = selectedModelRow
        val count = last - first + 1
        selectedModelRow = when {
            row < 0 -> -1
            inserted -> if (row >= first) row + count else row
            row > last -> row - count
            row >= first -> -1
            else -> row
        }
        reselect()
    }

    /** The rules of JTable.changeSelection ([isSelected] is whether the row's cell is selected). Does nothing for an out-of-range row. */
    fun changeSelection(rowIndex: Int, toggle: Boolean, extend: Boolean, isSelected: (Int) -> Boolean) = call { model ->
        if (rowIndex !in 0 until table.rowCount) return@call
        var anchor = model.getAnchorSelectionIndex()
        var anchorSelected = true
        if (anchor == -1) {
            if (table.rowCount > 0) anchor = 0
            anchorSelected = false
        }
        val selected = isSelected(rowIndex)
        anchorSelected = anchorSelected && isSelected(anchor)
        when {
            extend && toggle -> if (anchorSelected) model.addSelectionInterval(anchor, rowIndex) else model.removeSelectionInterval(anchor, rowIndex)
            extend -> model.setSelectionInterval(anchor, rowIndex)
            toggle -> if (selected) model.removeSelectionInterval(rowIndex, rowIndex) else model.addSelectionInterval(rowIndex, rowIndex)
            else -> model.setSelectionInterval(rowIndex, rowIndex)
        }
    }

    /** Same as Swing's JTable.boundRow: an out-of-range row throws IllegalArgumentException. */
    fun boundRow(row: Int): Int {
        require(row in 0 until table.rowCount) { "Row index out of range" }
        return row
    }

    /** Reselects the view row of [selectedModelRow] (clears the selection if it is not shown). */
    private fun reselect() {
        val view = if (selectedModelRow < 0) -1 else table.convertRowIndexToView(selectedModelRow)
        guarded {
            // Fix the model first (the native Select raises SelectionChanged, and its Runnable listeners read selectedRow
            // from the model, so doing it later would show the old row)
            val selected = model.getSelectedIndices()
            if (view < 0) {
                model.clearSelection()
            } else if (!(selected.size == 1 && selected[0] == view)) {
                model.setSelectionInterval(view, view)
            }
            if (nativeRow() != view) {
                if (view < 0) tableView.call(TabularInterop.ITableView_DeselectAll) else tableView.call(TabularInterop.ITableView_Select, view)
            }
            lastNative = nativeRow()
        }
        if (view < 0) selectedModelRow = -1
        pushAgainAfterLayout()
    }

    /** If not laid out yet, copies the model's selection to the native side again after layout (requests until then are merged into the last one). */
    private fun pushAgainAfterLayout() {
        if (!requests.isLaidOut()) requests.runOrDefer(PUSH_REQUEST) { push() }
    }

    /** The one row of the model's selection copied to the display (the lead if it is selected, otherwise the smallest row). -1 if outside the table's rows. */
    private fun targetRow(): Int {
        if (model.isSelectionEmpty()) return -1
        val lead = model.getLeadSelectionIndex()
        val row = if (lead >= 0 && model.isSelectedIndex(lead)) lead else model.getMinSelectionIndex()
        return if (row in 0 until table.rowCount) row else -1
    }

    private fun nativeRow(): Int = tableView.getInt(TabularInterop.ITableView_get_SelectedIndex)

    private inline fun <R> guarded(block: () -> R): R {
        val previous = syncing
        syncing = true
        try {
            return block()
        } finally {
            syncing = previous
        }
    }

    private companion object {
        /** The kind of request in [requests] that copies the selection again. */
        const val PUSH_REQUEST = "push"
    }
}
