package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.ListViewSelectionMode (how many items can be selected).
 * Values extracted from the winmd (None=0, Single=1, Multiple=2, Extended=3).
 */
enum class ListViewSelectionMode(internal val native: Int) {
    /** Nothing can be selected. */
    NONE(0),

    /** Only one item can be selected (default). */
    SINGLE(1),

    /** Clicking toggles multiple selection. */
    MULTIPLE(2),

    /** Multiple selection using Ctrl / Shift. */
    EXTENDED(3),
    ;

    internal companion object {
        fun of(native: Int): ListViewSelectionMode = entries.first { it.native == native }
    }
}

/**
 * JList-like: WinUI 3's ListView.
 *
 * Provides display and selection of a list of string items:
 * [addItem] / [getItem] / [removeItem] / [removeAllItems] / [itemCount],
 * [selectedIndex] / [selectedItem] / [selectionMode] / [selectAll],
 * [addListSelectionListener] / [removeListSelectionListener] (SelectionChanged),
 * [isItemClickEnabled] / [addItemClickListener] / [removeItemClickListener] (ItemClick).
 */
class WList(items: List<String> = emptyList()) : WControl(
    Activation.composeDefault(Abi.CLS_ListView, Abi.IID_IListViewFactory), // default interface = IListView
) {
    private val selector: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_ISelector))
    }
    private val listViewBase: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_IListViewBase))
    }

    /** The IVector<Object> view of ItemsControl.Items (ItemCollection). */
    private val itemVector: ComPtr by lazy {
        val itemsControl = own(inspectable.queryInterface(Abi.IID_IItemsControl))
        val items = own(itemsControl.getPtr(Abi.IItemsControl_get_Items))
        own(items.queryInterface(Abi.IID_IVector_Object))
    }

    /** SelectionChanged event tokens registered via addListSelectionListener. */
    private val selectionTokens = ListenerTokens<() -> Unit>()

    /** ItemClick event tokens registered via addItemClickListener. */
    private val itemClickTokens = ListenerTokens<(String) -> Unit>()

    /** Item count (Items.Size). */
    val itemCount: Int
        get() = itemVector.getInt(Abi.IVector_get_Size)

    /**
     * The selected index, or -1 if nothing is selected (Selector.SelectedIndex).
     * With multiple selection, this is the first selected position.
     */
    var selectedIndex: Int
        get() = selector.getInt(Abi.ISelector_get_SelectedIndex)
        set(value) = selector.call(Abi.ISelector_put_SelectedIndex, value)

    /** The selected item string, or null if nothing is selected (Selector.SelectedItem). */
    val selectedItem: String?
        get() {
            val boxed = selector.getPtrOrNull(Abi.ISelector_get_SelectedItem) ?: return null
            return try {
                PropertyValues.unboxString(boxed)
            } finally {
                boxed.release()
            }
        }

    /** The selection mode (ListViewBase.SelectionMode). */
    var selectionMode: ListViewSelectionMode
        get() = ListViewSelectionMode.of(listViewBase.getInt(Abi.IListViewBase_get_SelectionMode))
        set(value) = listViewBase.call(Abi.IListViewBase_put_SelectionMode, value.native)

    /**
     * Whether clicking an item fires ItemClick (ListViewBase.IsItemClickEnabled).
     * Set this to true if you use [addItemClickListener].
     */
    var isItemClickEnabled: Boolean
        get() = listViewBase.getBool(Abi.IListViewBase_get_IsItemClickEnabled)
        set(value) = listViewBase.putBool(Abi.IListViewBase_put_IsItemClickEnabled, value)

    init {
        for (item in items) addItem(item)
    }

    /** Appends an item at the end (Items.Append). The string is boxed before being passed. */
    fun addItem(item: String) {
        val boxed = PropertyValues.boxString(item)
        itemVector.call(Abi.IVector_Append, boxed.ptr)
        boxed.release()
    }

    /** Returns the item string at [index] (Items.GetAt). */
    fun getItem(index: Int): String {
        val boxed = itemVector.getPtr(Abi.IVector_GetAt, index)
        return try {
            PropertyValues.unboxString(boxed) ?: ""
        } finally {
            boxed.release()
        }
    }

    /** Removes the item at [index] (Items.RemoveAt). */
    fun removeItem(index: Int) {
        itemVector.call(Abi.IVector_RemoveAt, index)
    }

    /** Removes all items (Items.Clear). */
    fun removeAllItems() {
        itemVector.call(Abi.IVector_Clear)
    }

    /** Selects all items (ListViewBase.SelectAll). Only effective in Multiple / Extended mode. */
    fun selectAll() {
        listViewBase.call(Abi.IListViewBase_SelectAll)
    }

    /** ListSelectionListener-like. Subscribes to Selector.SelectionChanged under the hood. */
    fun addListSelectionListener(listener: () -> Unit) {
        val token = selector.addEventHandler(
            "WinUI4K.SelectionChangedHandler",
            Abi.IID_SelectionChangedEventHandler,
            Abi.ISelector_add_SelectionChanged,
        ) { _, _ -> listener() }
        selectionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addListSelectionListener]. */
    fun removeListSelectionListener(listener: () -> Unit) {
        val token = selectionTokens.remove(listener) ?: return
        selector.removeEventHandler(Abi.ISelector_remove_SelectionChanged, token)
    }

    /**
     * Subscribes to item clicks (ListViewBase.ItemClick).
     * The listener receives the clicked item's string.
     * Only fires while [isItemClickEnabled] is true.
     */
    fun addItemClickListener(listener: (String) -> Unit) {
        val token = listViewBase.addEventHandler(
            "WinUI4K.ItemClickHandler",
            Abi.IID_ItemClickEventHandler,
            Abi.IListViewBase_add_ItemClick,
        ) { _, args ->
            // args is ItemClickEventArgs's default interface, so it can be called directly
            val e = ComPtr(args)
            val boxed = e.getPtr(Abi.IItemClickEventArgs_get_ClickedItem)
            val item = try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
            listener(item)
        }
        itemClickTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemClickListener]. */
    fun removeItemClickListener(listener: (String) -> Unit) {
        val token = itemClickTokens.remove(listener) ?: return
        listViewBase.removeEventHandler(Abi.IListViewBase_remove_ItemClick, token)
    }
}
