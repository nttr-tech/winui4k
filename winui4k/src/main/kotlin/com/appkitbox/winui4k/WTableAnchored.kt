package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.TabularInterop
import com.appkitbox.winui4k.table.RowSorter
import com.appkitbox.winui4k.table.RowSorterHost
import com.appkitbox.winui4k.table.TableModel
import com.appkitbox.winui4k.table.TableModelListener
import com.appkitbox.winui4k.table.TableRowSorter

// Gateways through which the things WTable passes to the native side (the row collection, columns, row double-click, sorting) look up the table.
// If any of them captured the table strongly it would create the cycle "native -> handler -> wrapper -> native",
// so they hold only the ListenerAnchor and look the table up on each call (doing nothing if it has been collected).
// The following create the things passed to the native side. So that the lambdas do not capture the table, they take only the anchor, not the table

internal fun anchoredModelListener(anchor: ListenerAnchor<WTable>) = TableModelListener { event -> anchor.get()?.onTableChanged(event) }

internal fun anchoredRowInvoker(table: ComPtr, anchor: ListenerAnchor<WTable>) = TableViewRowInvoker(
    table,
    { pointer ->
        // itemOf releases pointer. If the table has been collected, release it here instead
        val table = anchor.get()
        if (table == null) null.also { pointer.release() } else table.itemOf(pointer)
    },
    { modelRow -> anchor.get()?.convertRowIndexToView(modelRow) ?: -1 },
)

/** The predicate (Predicate<Object>) that passes [WTable.rowSorter]'s RowFilter to TableViewSource.Filter. */
internal fun anchoredRowFilter(anchor: ListenerAnchor<WTable>): KComObject = KComObject("WinUI4K.TableViewPredicate", inspectable = false)
    .addInterface(
        TabularInterop.IID_TableViewPredicate,
        listOf(
            // Invoke(this, Object item, out boolean) — vtbl[3]
            KComObject.Method(DESC_PTR_PTR) { args ->
                val table = anchor.get()
                val item = table?.rows?.itemOf(args[1] as Ptr)
                val include = item == null || table.sorter?.includes(item.modelRow) != false
                Ffi.backend.memory.putByte(args[2] as Ptr, 0, if (include) 1 else 0)
                KComObject.S_OK
            },
        ),
    )

/** The key function (Func<Object, Object>) that passes [WTable.groupBy]'s key [key] to TableViewSource.GroupBy. */
internal fun anchoredGroupKeySelector(anchor: ListenerAnchor<WTable>, key: TableGroupKey): KComObject =
    KComObject("WinUI4K.TableViewKeySelector", inspectable = false)
        .addInterface(
            TabularInterop.IID_TableViewKeySelector,
            listOf(
                // Invoke(this, Object item, out Object key) — vtbl[3]
                KComObject.Method(DESC_PTR_PTR) { args ->
                    val table = anchor.get()
                    val item = table?.rows?.itemOf(args[1] as Ptr)
                    val boxed = item?.let { PropertyValues.boxAny(groupKeyOf(key, table.model, it.modelRow)) }
                    Ffi.backend.memory.putPtr(args[2] as Ptr, 0, boxed?.ptr ?: Ptr.NULL)
                    KComObject.S_OK
                },
            ),
        )

private val DESC_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)

/** The gateway through which row objects ([TableRowItem]) read and write the model's values. The native side holds the rows, so it looks the table up weakly. */
internal class AnchoredRowValues(private val anchor: ListenerAnchor<WTable>) : TableRowValues {
    override fun columnCount(): Int = anchor.get()?.model?.getColumnCount() ?: 0

    override fun valueAt(row: Int, column: Int): Any? = anchor.get()?.model?.getValueAt(row, column)

    override fun valueWritten(item: TableRowItem, column: Int, value: Any?) {
        anchor.get()?.onValueWritten(item, column, value)
    }
}

/** The gateway from [TableRowSorter] to the table. Predicates and comparisons (held by the native side) look up the sorting, so it looks the table up weakly and forwards to [sorterBridge]. */
internal class AnchoredSorterHost(private val anchor: ListenerAnchor<WTable>) : RowSorterHost {
    private val target: RowSorterHost?
        get() = anchor.get()?.sorterBridge

    override fun sortKeysChanged(keys: List<RowSorter.SortKey>) {
        target?.sortKeysChanged(keys)
    }

    override fun toggleSortOrder(column: Int) {
        target?.toggleSortOrder(column)
    }

    override fun currentSortKeys(): List<RowSorter.SortKey> = target?.currentSortKeys().orEmpty()

    override fun rowFilterChanged() {
        target?.rowFilterChanged()
    }

    override fun comparatorChanged(column: Int) {
        target?.comparatorChanged(column)
    }

    override fun sortableChanged(column: Int) {
        target?.sortableChanged(column)
    }

    // After the table has been collected, treat it as having no sorting or filtering (view and model rows are the same)
    override fun convertRowIndexToModel(index: Int): Int = target?.convertRowIndexToModel(index) ?: index

    override fun convertRowIndexToView(index: Int): Int = target?.convertRowIndexToView(index) ?: index

    override fun viewRowCount(): Int = target?.viewRowCount() ?: 0
}

/** The gateway through which columns ([TableViewColumnPeer]) read the table's state. The native columns hold the Kotlin side of the columns, so it looks the table up weakly. */
internal class AnchoredColumnHost(private val anchor: ListenerAnchor<WTable>) : TableViewColumnHost {
    override val isAlive: Boolean
        get() = anchor.get() != null
    override val table: WTable
        get() = checkNotNull(anchor.get()) { "the WTable has been garbage-collected" }
    override val model: TableModel
        get() = table.model
    override val rows: TableRowCollection
        get() = table.rows
    override val sorter: TableRowSorter<*>?
        get() = table.sorter

    override fun columnClassOf(modelIndex: Int): Class<*> {
        val model = model
        return if (modelIndex in 0 until model.getColumnCount()) model.getColumnClass(modelIndex) else Any::class.java
    }

    override fun itemOf(pointer: ComPtr): TableRowItem? {
        val table = anchor.get() ?: return null.also { pointer.release() }
        return table.itemOf(pointer)
    }

    override fun rebuild(peer: TableViewColumnPeer) {
        anchor.get()?.rebuildPeer(peer)
    }

    override fun viewColumnOf(peer: TableViewColumnPeer): Int = anchor.get()?.peers?.indexOf(peer) ?: -1
}
