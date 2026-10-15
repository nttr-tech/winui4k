package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.Dispatcher
import com.appkitbox.winui4k.internal.winui.TabularInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Keeps the rows of a [WTable] shown when the table leaves the visual tree and comes back in the same UI turn.
 *
 * TableView detaches its rows (the ItemsSource of the ItemsRepeater in its template) on Unloaded and reattaches them
 * on Loaded. When the table is removed and added again in one turn (e.g. a page shown again in a reopened popup),
 * WinUI can raise the new Loaded before the old Unloaded, so the table stays in the tree with no rows. An Unloaded
 * that arrives while the table is still loaded is such a stale one: once TableView's own handler has run, this hands
 * TableView its ItemsSource again.
 */
internal class TableViewItemsSourceKeeper(
    /** The table's IFrameworkElement. */
    private val element: ComPtr,
    /** The table's ITableView. */
    private val tableView: ComPtr,
    /** The TableViewSource currently set as ItemsSource. */
    private val source: () -> ComPtr?,
) {
    fun subscribe() {
        element.addEventHandler(
            "WinUI4K.TableViewUnloadedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Unloaded,
        ) { _, _ -> onUnloaded() }
    }

    private fun onUnloaded() {
        if (!isLoaded()) return
        Dispatcher.invokeLater { if (isLoaded()) reattach() }
    }

    private fun isLoaded(): Boolean = element.getBool(XamlInterop.IFrameworkElement_get_IsLoaded)

    /**
     * Sets ItemsSource again (it is cleared first, as TableView does nothing for the value it already has). The
     * selected row stays selected.
     */
    private fun reattach() {
        val current = source() ?: return
        tableView.call(TabularInterop.ITableView_put_ItemsSource, null)
        tableView.call(TabularInterop.ITableView_put_ItemsSource, current.ptr)
    }
}
