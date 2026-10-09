package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.table.RowSorter
import com.appkitbox.winui4k.table.SortOrder
import com.appkitbox.winui4k.table.TableModel
import com.appkitbox.winui4k.table.TableRowSorter

/**
 * Reads the row order in the view of a [WTableView] (the result of sorting, filtering, and grouping).
 *
 * The order is projected by TableViewSource and passed to PART_RowsRepeater (an ItemsRepeater) in the TableView
 * template, so this reads its ItemsSourceView (while grouped, each group header row also counts as one row).
 * When it cannot be read, such as before the template is applied, an order computed on the Kotlin side with filtering
 * and sorting (by one column) is used instead.
 */
internal class TableViewRowOrder(
    /** The DependencyObject view of the table (TableView). The starting point for searching inside the template. */
    private val root: () -> ComPtr,
    private val rows: () -> TableRowCollection,
    private val model: () -> TableModel,
    private val sorter: () -> TableRowSorter<*>?,
    /** The current sort keys (the sort directions of the displayed columns). */
    private val sortKeys: () -> List<RowSorter.SortKey>,
) {
    /** The PART_RowsRepeater (IItemsRepeater) that was found. Kept for the lifetime of the table. */
    private var rowsRepeater: ComPtr? = null

    /** View row index → model row index. -1 for group header rows and out-of-range indexes. */
    fun toModel(viewRowIndex: Int): Int {
        val view = itemsSourceView() ?: return fallbackOrder().getOrElse(viewRowIndex) { -1 }
        try {
            if (viewRowIndex !in 0 until view.getInt(XamlInterop.IItemsSourceView_get_Count)) return -1
            return modelRowAt(view, viewRowIndex)
        } finally {
            view.release()
        }
    }

    /** Model row index → view row index. -1 if the row is not displayed. */
    fun toView(modelRowIndex: Int): Int {
        if (modelRowIndex !in 0 until rows().size) return -1
        val view = itemsSourceView() ?: return fallbackOrder().indexOf(modelRowIndex)
        try {
            val count = view.getInt(XamlInterop.IItemsSourceView_get_Count)
            return (0 until count).firstOrNull { modelRowAt(view, it) == modelRowIndex } ?: -1
        } finally {
            view.release()
        }
    }

    /** The number of rows in the view. */
    fun count(): Int {
        val view = itemsSourceView() ?: return fallbackOrder().size
        return try {
            view.getInt(XamlInterop.IItemsSourceView_get_Count)
        } finally {
            view.release()
        }
    }

    /** The model index of the row at [index] in the ItemsSourceView (-1 for a header row). */
    private fun modelRowAt(view: ComPtr, index: Int): Int {
        val item = view.getPtrOrNull(XamlInterop.IItemsSourceView_GetAt, index) ?: return -1
        return try {
            rows().itemOf(item.ptr)?.modelRow ?: -1
        } finally {
            item.release()
        }
    }

    /**
     * The ItemsSourceView of PART_RowsRepeater (the TableViewSource projection), or null if it cannot be read.
     * The caller releases it.
     */
    private fun itemsSourceView(): ComPtr? {
        val repeater = rowsRepeater ?: findRowsRepeater()?.also { rowsRepeater = it } ?: return null
        return repeater.getPtrOrNull(XamlInterop.IItemsRepeater_get_ItemsSourceView)
    }

    /** Walks the visual tree breadth-first to find PART_RowsRepeater. Returns null if not found. */
    @Suppress("NestedBlockDepth") // The nesting only looks deep because of try/finally releases of COM references
    private fun findRowsRepeater(): ComPtr? {
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val queue = ArrayDeque<ComPtr>()
        try {
            queue.addLast(root().also { it.addRef() })
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                try {
                    asRowsRepeater(node)?.let { return it }
                    for (index in 0 until childCount(statics, node.ptr)) {
                        statics.getPtrOrNull(XamlInterop.IVisualTreeHelperStatics_GetChild, node.ptr, index)
                            ?.let { queue.addLast(it) }
                    }
                } finally {
                    node.release()
                }
            }
            return null
        } finally {
            queue.forEach { it.release() }
            statics.release()
        }
    }

    /** If [node] is PART_RowsRepeater, returns its IItemsRepeater (an owned reference); otherwise null. */
    private fun asRowsRepeater(node: ComPtr): ComPtr? {
        val element = node.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement) ?: return null
        val name = try {
            element.getString(XamlInterop.IFrameworkElement_get_Name)
        } finally {
            element.release()
        }
        return if (name == ROWS_REPEATER_PART) node.queryInterfaceOrNull(XamlInterop.IID_IItemsRepeater) else null
    }

    private fun childCount(statics: ComPtr, node: Ptr): Int = Ffi.backend.withScope { scope ->
        val out = scope.allocate(4)
        statics.call(XamlInterop.IVisualTreeHelperStatics_GetChildrenCount, node, out)
        Ffi.backend.memory.getInt(out, 0)
    }

    /**
     * The fallback before display: the order of model rows with filtering and sorting (by the first sort key only;
     * columns without a comparator use the natural order of values) applied on the Kotlin side. Grouping is not
     * taken into account.
     */
    private fun fallbackOrder(): List<Int> {
        val currentSorter = sorter()
        val currentModel = model()
        val filtered = (0 until rows().size).filter { currentSorter == null || currentSorter.includes(it) }
        if (currentSorter == null) return filtered
        val key = sortKeys().firstOrNull() ?: currentSorter.pendingSortKeys().firstOrNull() ?: return filtered

        @Suppress("UNCHECKED_CAST") // The app specifies a comparator that matches the column's value type
        val comparator = (currentSorter.getComparator(key.column) as Comparator<Any?>?) ?: NATURAL_ORDER
        val ascending = filtered.sortedWith { a, b ->
            comparator.compare(currentModel.getValueAt(a, key.column), currentModel.getValueAt(b, key.column))
        }
        return if (key.sortOrder == SortOrder.DESCENDING) ascending.reversed() else ascending
    }

    private companion object {
        /**
         * The name of the ItemsRepeater that displays rows in the TableView template
         * (Template parts in TableView-spec.md).
         */
        const val ROWS_REPEATER_PART = "PART_RowsRepeater"

        /**
         * The natural order of values (Comparables of the same type in their own order, everything else in string
         * order; null first).
         */
        val NATURAL_ORDER = Comparator<Any?> { a, b ->
            when {
                a == null && b == null -> 0
                a == null -> -1
                b == null -> 1
                a is Comparable<*> && a.javaClass == b.javaClass -> {
                    @Suppress("UNCHECKED_CAST") // Comparing Comparables of the same class
                    (a as Comparable<Any>).compareTo(b)
                }
                else -> a.toString().compareTo(b.toString())
            }
        }
    }
}
