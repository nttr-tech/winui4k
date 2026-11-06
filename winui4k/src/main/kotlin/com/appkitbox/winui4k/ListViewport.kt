package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import kotlin.math.abs
import kotlin.math.roundToInt

/** The bounds of one shown item (DIP with the list's top-left as the origin). */
internal data class ListCell(val index: Int, val x: Double, val y: Double, val width: Double, val height: Double) {
    val bottom: Double get() = y + height
}

/**
 * The implementation of the JList display API shared by [WList] (ListView) and [WListBox] (ListBox)
 * (ensureIndexIsVisible / visibleRowCount / first / lastVisibleIndex / locationToIndex / indexToLocation / getCellBounds).
 *
 * Positions are found by converting, among the children of the item panel (ItemsControl.ItemsPanelRoot), the item containers (those with IndexFromContainer of 0 or more)
 * to list coordinates. WinUI creates containers only for the visible items and a few around them (virtualization), so positions of items not created are unknown.
 * Values are as of the last layout.
 */
internal class ListViewport(
    /** The list wrapper (this object is held by the wrapper). */
    private val owner: WControl,
    /** The number of elements. */
    private val itemCount: () -> Int,
    /** Scrolls until the item at [index] is visible (ScrollIntoView). */
    private val scrollTo: (index: Int) -> Unit,
    /** The estimated height of one row (DIP; used for the [visibleRowCount] height). */
    rowHeight: () -> Double = { ROW_HEIGHT },
) {
    private val itemsControl: ComPtr by lazy { owner.own(owner.inspectable.queryInterface(XamlInterop.IID_IItemsControl)) }
    private val mapping: ComPtr by lazy { owner.own(owner.inspectable.queryInterface(XamlInterop.IID_IItemContainerMapping)) }

    private val rows = VisibleRows(owner, rowHeight)

    /** The value of JList.getVisibleRowCount (8 by default, as in Swing). */
    var visibleRowCount: Int by rows::count

    /** When the estimated height of one row changes (fixedCellHeight): decides the height again if it was decided from the row count. */
    fun rowHeightChanged() = rows.rowHeightChanged()

    /** JList.ensureIndexIsVisible: does nothing for an out-of-range [index]. */
    fun ensureIndexIsVisible(index: Int) {
        if (index in 0 until itemCount()) scrollTo(index)
    }

    /** The bounds of the item containers created now (in index order). Empty if not shown. */
    fun realizedCells(): List<ListCell> {
        val panel = itemsControl.getPtrOrNull(XamlInterop.IItemsControl_get_ItemsPanelRoot) ?: return emptyList()
        try {
            return (0 until Xaml.childCount(panel)).mapNotNull { position ->
                Xaml.childAt(panel, position)?.let { child ->
                    try {
                        cellOf(child)
                    } finally {
                        child.release()
                    }
                }
            }.sortedBy { it.index }
        } finally {
            panel.release()
        }
    }

    /** The bounds of the panel child [child] (IDependencyObject; borrowed) if it is an item container. null for containers waiting for reuse or without a size. */
    private fun cellOf(child: ComPtr): ListCell? {
        val index = indexFromContainer(child)
        if (index < 0) return null
        val (width, height) = VisualHitTest.actualSizeOf(child)
        if (width <= 0.0 || height <= 0.0) return null
        val (x, y) = VisualHitTest.transform(child, owner.uiElement, 0.0, 0.0)
        return ListCell(index, x, y, width, height)
    }

    /** The items at least partly inside the list's bounds (in index order). */
    private fun visibleCells(): List<ListCell> {
        val (width, height) = VisualHitTest.actualSizeOf(owner.uiElement)
        if (width <= 0.0 || height <= 0.0) return emptyList()
        return realizedCells().filter { it.y < height && it.bottom > 0.0 && it.x < width && it.x + it.width > 0.0 }
    }

    /** JList.getFirstVisibleIndex: the first visible item. -1 if none. */
    val firstVisibleIndex: Int
        get() = visibleCells().firstOrNull()?.index ?: -1

    /** JList.getLastVisibleIndex: the last visible item. -1 if none. */
    val lastVisibleIndex: Int
        get() = visibleCells().lastOrNull()?.index ?: -1

    /**
     * JList.locationToIndex: the item closest to the vertical position [y] (the horizontal position is not considered, as with Swing's vertical lists).
     * -1 when there are no elements or it is not shown.
     */
    fun locationToIndex(@Suppress("UNUSED_PARAMETER") x: Double, y: Double): Int {
        if (itemCount() == 0) return -1
        val cells = realizedCells()
        return cells.firstOrNull { y >= it.y && y < it.bottom }?.index
            ?: cells.minByOrNull { if (y < it.y) it.y - y else abs(y - it.bottom) }?.index
            ?: -1
    }

    /** JList.getCellBounds: the bounds enclosing the items at [index0] and [index1]. null if either is out of range or has no container. */
    fun cellBounds(index0: Int, index1: Int): WRectangle? {
        val count = itemCount()
        if (index0 !in 0 until count || index1 !in 0 until count) return null
        val cells = realizedCells()
        val first = cells.firstOrNull { it.index == minOf(index0, index1) } ?: return null
        val last = cells.firstOrNull { it.index == maxOf(index0, index1) } ?: return null
        val left = minOf(first.x, last.x)
        val right = maxOf(first.x + first.width, last.x + last.width)
        return WRectangle(left.roundToInt(), first.y.roundToInt(), (right - left).roundToInt(), (last.bottom - first.y).roundToInt())
    }

    /** The container of the item at [index] (an owned IDependencyObject reference). null if not created. */
    fun containerFromIndex(index: Int): ComPtr? = mapping.getPtrOrNull(XamlInterop.IItemContainerMapping_ContainerFromIndex, index)

    private fun indexFromContainer(container: ComPtr): Int = Ffi.backend.withScope { scope ->
        val out = scope.allocate(4)
        mapping.call(XamlInterop.IItemContainerMapping_IndexFromContainer, container.ptr, out)
        Ffi.backend.memory.getInt(out, 0)
    }

    companion object {
        /** The default value of JList's visibleRowCount. */
        const val DEFAULT_VISIBLE_ROW_COUNT = 8

        /**
         * The estimated height of one row (ListViewItemMinHeight = 40; ListBoxItem is also about 40 with padding 9 + 12 and 14px text).
         * It drifts for apps with Compact density (ListViewItemMinHeight = 32) or when a renderer draws items of different heights.
         */
        const val ROW_HEIGHT = 40.0
    }
}

/**
 * The implementation of visibleRowCount of JList / JTree: sets the height ([WComponent.height]) to "rows × the estimated height of one row + top and bottom padding (Padding) and border
 * (BorderThickness)". If the user has specified a height (if it differs from the height last decided from the row count), it is not changed.
 */
internal class VisibleRows(
    private val owner: WControl,
    /** The estimated height of one row (DIP). */
    private val rowHeight: () -> Double,
) {
    /** The height last set by [count] (NaN if not set). */
    private var heightFromRows = Double.NaN

    /** The number of rows to show (8 by default, as in Swing; negative values become 0). */
    var count: Int = ListViewport.DEFAULT_VISIBLE_ROW_COUNT
        set(value) {
            field = value.coerceAtLeast(0)
            apply()
        }

    /** When the estimated height of one row changes: decides the height again if it was decided from the row count (Swing recalculates the size every time). */
    fun rowHeightChanged() {
        if (!heightFromRows.isNaN()) apply()
    }

    private fun apply() {
        // If the user specified a height, that height takes precedence (a height decided from the row count may be overwritten)
        if (!owner.height.isNaN() && owner.height != heightFromRows) return
        val padding = XamlStructs.getThickness(owner.control, XamlInterop.IControl_get_Padding)
        val border = XamlStructs.getThickness(owner.control, XamlInterop.IControl_get_BorderThickness)
        heightFromRows = count * rowHeight() + padding[1] + padding[3] + border[1] + border[3]
        owner.height = heightFromRows
    }
}
