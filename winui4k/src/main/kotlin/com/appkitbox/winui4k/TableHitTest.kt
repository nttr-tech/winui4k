package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.TabularInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Finding the row and column at a point of a [WTable] (the implementation of JTable.rowAtPoint / columnAtPoint).
 *
 * Row: among the elements at the point (VisualTreeHelper.FindElementsInHostCoordinates), starting from the frontmost, walks up the parents
 * to find the row object, the same as for a row double-click ([TableViewRowInvoker.viewRowOf]).
 * Column: rows and the column header have a TableViewCellsPanel that lays out cells left to right in column order (measured; cell widths are the columns' ActualWidth).
 * Converts to the coordinates of the panel at the point (or the column header's panel if none) and adds up the column widths from the left. The panel moves
 * with horizontal scrolling, so the columns match even when scrolled.
 */
internal object TableHitTest {
    fun rowAt(table: ComPtr, rowInvoker: TableViewRowInvoker, x: Double, y: Double): Int {
        val elements = VisualHitTest.elementsAt(table, x, y)
        try {
            // viewRowOf walks up the parents with VisualTreeHelper.GetParent, so pass it as the DependencyObject interface
            val top = elements.firstOrNull()?.queryInterface(XamlInterop.IID_IDependencyObject) ?: return -1
            return try {
                rowInvoker.viewRowOf(top)
            } finally {
                top.release()
            }
        } finally {
            elements.forEach { it.release() }
        }
    }

    fun columnAt(table: ComPtr, widths: List<Double>, x: Double, y: Double): Int {
        val (width, height) = VisualHitTest.actualSizeOf(table)
        if (x !in 0.0..<width || y !in 0.0..<height) return -1
        val panel = panelAt(table, x, y) ?: firstPanel(table) ?: return -1
        val panelX = try {
            VisualHitTest.transform(table, panel, x, y).first
        } finally {
            panel.release()
        }
        if (panelX < 0) return -1
        var right = 0.0
        for ((index, columnWidth) in widths.withIndex()) {
            right += columnWidth
            if (panelX < right) return index
        }
        return -1
    }

    /** The TableViewCellsPanel among the elements at the point (an owned reference). null if none. */
    private fun panelAt(table: ComPtr, x: Double, y: Double): ComPtr? {
        val elements = VisualHitTest.elementsAt(table, x, y)
        try {
            return elements.firstNotNullOfOrNull { it.queryInterfaceOrNull(TabularInterop.IID_ITableViewCellsPanel) }
        } finally {
            elements.forEach { it.release() }
        }
    }

    /** The first TableViewCellsPanel found searching the visual tree from the front (the column header's; an owned reference). null if none. */
    private fun firstPanel(node: ComPtr): ComPtr? {
        node.queryInterfaceOrNull(TabularInterop.IID_ITableViewCellsPanel)?.let { return it }
        for (index in 0 until Xaml.childCount(node)) {
            val child = Xaml.childAt(node, index) ?: continue
            try {
                firstPanel(child)?.let { return it }
            } finally {
                child.release()
            }
        }
        return null
    }
}
