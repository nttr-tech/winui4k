package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.SelectionMode (the ListBox's selection mode).
 * Values extracted from the winmd (Single=0, Multiple=1, Extended=2).
 */
enum class SelectionMode(internal val native: Int) {
    /** Only one item can be selected (default). */
    SINGLE(0),

    /** Clicking toggles multiple selections. */
    MULTIPLE(1),

    /** Multiple items can be selected using Ctrl/Shift. */
    EXTENDED(2),
    ;

    internal companion object {
        fun of(native: Int): SelectionMode = entries.first { it.native == native }
    }
}

/**
 * JList-like: WinUI 3's ListBox (a Selector subclass).
 * Unlike ListView, its items are always visible as a simple selection list, with no swipe or similar gestures.
 *
 * Provides displaying and selecting a list of string items:
 * [addItem] / [getItem] / [removeItem] / [removeAllItems] / [itemCount],
 * [selectedIndex] / [selectedItem] / [selectedItems] / [selectionMode] / [selectAll],
 * [scrollIntoView] (scrolls to a given position),
 * [addListSelectionListener] / [removeListSelectionListener] (SelectionChanged).
 */
class WListBox(items: List<String> = emptyList()) : WControl(
    Activation.composeDefault(Abi.CLS_ListBox, Abi.IID_IListBoxFactory), // default interface = IListBox
) {
    private val selector: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_ISelector))
    }

    /** The IVector<Object> view of ItemsControl.Items (ItemCollection). */
    private val itemVector: ComPtr by lazy {
        val itemsControl = own(inspectable.queryInterface(Abi.IID_IItemsControl))
        val items = own(itemsControl.getPtr(Abi.IItemsControl_get_Items))
        own(items.queryInterface(Abi.IID_IVector_Object))
    }

    /** Event tokens for SelectionChanged registered via addListSelectionListener. */
    private val selectionTokens = ListenerTokens<() -> Unit>()

    /** Item count (Items.Size). */
    val itemCount: Int
        get() = itemVector.getInt(Abi.IVector_get_Size)

    /**
     * The selected index, or -1 if nothing is selected (Selector.SelectedIndex).
     * In multi-select mode, this is the first selected position.
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

    /** All selected item strings (ListBox.SelectedItems). Used in multi-select mode. */
    val selectedItems: List<String>
        get() {
            // get_SelectedItems already returns a typed IVector<Object> pointer, so no QI is needed
            val vector = inspectable.getPtr(Abi.IListBox_get_SelectedItems)
            return try {
                (0 until vector.getInt(Abi.IVector_get_Size)).map { index ->
                    val boxed = vector.getPtr(Abi.IVector_GetAt, index)
                    try {
                        PropertyValues.unboxString(boxed) ?: ""
                    } finally {
                        boxed.release()
                    }
                }
            } finally {
                vector.release()
            }
        }

    /** Selection mode (ListBox.SelectionMode). */
    var selectionMode: SelectionMode
        get() = SelectionMode.of(inspectable.getInt(Abi.IListBox_get_SelectionMode))
        set(value) = inspectable.call(Abi.IListBox_put_SelectionMode, value.native)

    init {
        for (item in items) addItem(item)
    }

    /** Appends an item (Items.Append). The string is boxed before being passed. */
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

    /** Selects all items (ListBox.SelectAll). Effective in Multiple / Extended mode. */
    fun selectAll() {
        inspectable.call(Abi.IListBox_SelectAll)
    }

    /**
     * Scrolls until the item at [index] is visible (ListBox.ScrollIntoView).
     * Items requires the actual object it holds, so it's fetched via GetAt before being passed.
     */
    fun scrollIntoView(index: Int) {
        val boxed = itemVector.getPtr(Abi.IVector_GetAt, index)
        try {
            inspectable.call(Abi.IListBox_ScrollIntoView, boxed.ptr)
        } finally {
            boxed.release()
        }
    }

    /** ListSelectionListener-like. Backed by a subscription to Selector.SelectionChanged. */
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
}
