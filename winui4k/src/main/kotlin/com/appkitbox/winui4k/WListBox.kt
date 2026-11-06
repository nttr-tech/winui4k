package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
        @JvmSynthetic
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
 *
 * As with Swing's JList, the data is held by a [WListModel] ([model] / [setListData]; [WDefaultListModel] by default).
 * Each element is shown as its toString() string (an empty string if null), and model changes are reflected in the display immediately.
 * Selected elements can be read as the model's elements themselves with [selectedValue] / [selectedValuesList]
 * (elements with the same string are distinguished by position).
 *
 * Model changes and the selection (same as Swing's JList): inserting or removing before the selected element shifts the selection, removing the selected element
 * clears that selection, and replacing an element (contentsChanged) keeps the selection at that position (WinUI deselects replaced items, so they are
 * selected again, which calls the selection listeners). Replacing [model] clears the selection.
 *
 * [addItem] / [removeItem] / [removeAllItems] edit the default model ([WDefaultListModel]). When an app-specific
 * model (a read-only [WAbstractListModel] and so on) is set, they throw UnsupportedOperationException.
 * Swing's JList(Object[]) / setListData create a read-only model, but these create a [WDefaultListModel], so
 * the list can still be edited afterwards with addItem and the like.
 * * The default selection mode is single selection, as in WinUI (Swing's JList defaults to multiple-interval selection).
 */
class WListBox(model: WListModel<*>) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ListBox, XamlInterop.IID_IListBoxFactory), // default interface = IListBox
) {
    /** A list showing each element of [items] (creates a default model [WDefaultListModel]). */
    @JvmOverloads
    constructor(items: List<*> = emptyList<Any?>()) : this(defaultListModelOf(items))

    /** A list showing each element of [listData] (equivalent to JList(Object[]); creates a default model [WDefaultListModel]). */
    constructor(listData: Array<out Any?>) : this(defaultListModelOf(listData.asList()))

    private val selector: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_ISelector))
    }

    /** The IVector<Object> view of ItemsControl.Items (ItemCollection). */
    private val itemVector: ComPtr by lazy {
        val itemsControl = own(inspectable.queryInterface(XamlInterop.IID_IItemsControl))
        val items = own(itemsControl.getPtr(XamlInterop.IItemsControl_get_Items))
        own(items.queryInterface(FoundationInterop.IID_IVector_Object))
    }

    /** The synchronization of the model and Items (shared by ListView and ListBox). */
    internal val listItems = ListModelItems(
        owner = this,
        items = itemVector,
        model = model,
        // get_SelectedItems returns a typed IVector<Object> pointer, so no QI is needed
        selectedItems = { inspectable.getPtr(XamlInterop.IListBox_get_SelectedItems) },
        selector = selector,
        isSingleSelection = { selectionMode == SelectionMode.SINGLE },
    )

    /**
     * The model of the elements shown (JList.getModel / setModel). Replacing it rebuilds the display and clears the selection.
     * [WDefaultListModel] by default.
     */
    var model: WListModel<*>
        get() = listItems.model
        set(value) {
            listItems.model = value
        }

    /**
     * The selected element (JList.getSelectedValue). The frontmost element when several are selected, null when nothing is selected.
     * Returns the model's element itself, not a string.
     */
    val selectedValue: Any?
        get() = listItems.selectedIndices().firstOrNull()?.let { listItems.valueAt(it) }

    /** All selected elements (JList.getSelectedValuesList). In index order. */
    val selectedValuesList: List<Any?>
        get() = listItems.selectedIndices().map { listItems.valueAt(it) }

    /** Event tokens for SelectionChanged registered via addListSelectionListener. */
    private val selectionTokens = ListenerTokens<Runnable>()

    /** The number of items (the number of elements in the model). */
    val itemCount: Int
        get() = listItems.size

    /**
     * The selected index, or -1 if nothing is selected (Selector.SelectedIndex).
     * In multi-select mode, this is the first selected position.
     */
    var selectedIndex: Int
        get() = selector.getInt(XamlInterop.ISelector_get_SelectedIndex)
        set(value) = selector.call(XamlInterop.ISelector_put_SelectedIndex, value)

    /** The string of the selected item (toString() of [selectedValue]). Null when nothing is selected. */
    val selectedItem: String?
        get() = selectedValue?.toString()

    /** All selected item strings (ListBox.SelectedItems). Used in multi-select mode. */
    val selectedItems: List<String>
        get() {
            // get_SelectedItems already returns a typed IVector<Object> pointer, so no QI is needed
            val vector = inspectable.getPtr(XamlInterop.IListBox_get_SelectedItems)
            return try {
                (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index ->
                    val boxed = vector.getPtr(FoundationInterop.IVector_GetAt, index)
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
        get() = SelectionMode.of(inspectable.getInt(XamlInterop.IListBox_get_SelectionMode))
        set(value) = inspectable.call(XamlInterop.IListBox_put_SelectionMode, value.native)

    /** Shows each element of [listData] (JList.setListData). Sets a new [WDefaultListModel] as [model]. */
    fun setListData(listData: Array<out Any?>) {
        model = defaultListModelOf(listData.asList())
    }

    /** Shows each element of [listData] (JList.setListData(Vector)). Sets a new [WDefaultListModel] as [model]. */
    fun setListData(listData: List<*>) {
        model = defaultListModelOf(listData)
    }

    /**
     * Adds an item at the end (the default model's addElement).
     * @throws UnsupportedOperationException when [model] is not a [WDefaultListModel]
     */
    fun addItem(item: String) {
        listItems.editableModel().addElement(item)
    }

    /**
     * Returns the string of the [index]th item (toString() of the model's element; an empty string if null).
     * @throws ArrayIndexOutOfBoundsException when [index] is out of range (with the default model)
     */
    fun getItem(index: Int): String = listItems.textAt(index)

    /**
     * Removes the [index]th item (the default model's removeElementAt).
     * @throws UnsupportedOperationException when [model] is not a [WDefaultListModel]
     */
    fun removeItem(index: Int) {
        listItems.editableModel().removeElementAt(index)
    }

    /**
     * Removes all items (the default model's removeAllElements).
     * @throws UnsupportedOperationException when [model] is not a [WDefaultListModel]
     */
    fun removeAllItems() {
        listItems.editableModel().removeAllElements()
    }

    /** Selects all items (ListBox.SelectAll). Effective in Multiple / Extended mode. */
    fun selectAll() {
        inspectable.call(XamlInterop.IListBox_SelectAll)
    }

    /**
     * Scrolls until the item at [index] is visible (ListBox.ScrollIntoView).
     * Items requires the actual object it holds, so it's fetched via GetAt before being passed.
     */
    fun scrollIntoView(index: Int) {
        val boxed = itemVector.getPtr(FoundationInterop.IVector_GetAt, index)
        try {
            inspectable.call(XamlInterop.IListBox_ScrollIntoView, boxed.ptr)
        } finally {
            boxed.release()
        }
    }

    /** ListSelectionListener-like. Backed by a subscription to Selector.SelectionChanged. */
    @JvmSynthetic
    fun addListSelectionListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addListSelectionListenerForJava(adapter)
        selectionTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addListSelectionListener")
    fun addListSelectionListenerForJava(listener: Runnable) {
        val token = selector.addEventHandler(
            "WinUI4K.SelectionChangedHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ISelector_add_SelectionChanged,
        ) { _, _ -> listener.run() }
        selectionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addListSelectionListener]. */
    @JvmSynthetic
    fun removeListSelectionListener(listener: () -> Unit) {
        val adapter = selectionTokens.removeKotlinAdapter(listener) ?: return
        removeListSelectionListenerForJava(adapter)
    }

    @JvmName("removeListSelectionListener")
    fun removeListSelectionListenerForJava(listener: Runnable) {
        val token = selectionTokens.remove(listener) ?: return
        selector.removeEventHandler(XamlInterop.ISelector_remove_SelectionChanged, token)
    }
}
