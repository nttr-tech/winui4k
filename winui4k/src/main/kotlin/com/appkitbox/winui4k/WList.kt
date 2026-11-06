package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
        @JvmSynthetic
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
class WList(model: WListModel<*>) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ListView, XamlInterop.IID_IListViewFactory), // default interface = IListView
) {
    /** A list showing each element of [items] (creates a default model [WDefaultListModel]). */
    @JvmOverloads
    constructor(items: List<*> = emptyList<Any?>()) : this(defaultListModelOf(items))

    /** A list showing each element of [listData] (equivalent to JList(Object[]); creates a default model [WDefaultListModel]). */
    constructor(listData: Array<out Any?>) : this(defaultListModelOf(listData.asList()))

    private val selector: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_ISelector))
    }
    private val listViewBase: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IListViewBase))
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
        selectedItems = { listViewBase.getPtr(XamlInterop.IListViewBase_get_SelectedItems) },
        selector = selector,
        isSingleSelection = { selectionMode == ListViewSelectionMode.SINGLE },
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

    /** SelectionChanged event tokens registered via addListSelectionListener. */
    private val selectionTokens = ListenerTokens<Runnable>()

    /** ItemClick event tokens registered via addItemClickListener. */
    private val itemClickTokens = ListenerTokens<Consumer<String>>()

    /** The number of items (the number of elements in the model). */
    val itemCount: Int
        get() = listItems.size

    /**
     * The selected index, or -1 if nothing is selected (Selector.SelectedIndex).
     * With multiple selection, this is the first selected position.
     */
    var selectedIndex: Int
        get() = selector.getInt(XamlInterop.ISelector_get_SelectedIndex)
        set(value) = selector.call(XamlInterop.ISelector_put_SelectedIndex, value)

    /** The string of the selected item (toString() of [selectedValue]). Null when nothing is selected. */
    val selectedItem: String?
        get() = selectedValue?.toString()

    /** The selection mode (ListViewBase.SelectionMode). */
    var selectionMode: ListViewSelectionMode
        get() = ListViewSelectionMode.of(listViewBase.getInt(XamlInterop.IListViewBase_get_SelectionMode))
        set(value) = listViewBase.call(XamlInterop.IListViewBase_put_SelectionMode, value.native)

    /**
     * Whether clicking an item fires ItemClick (ListViewBase.IsItemClickEnabled).
     * Set this to true if you use [addItemClickListener].
     */
    var isItemClickEnabled: Boolean
        get() = listViewBase.getBool(XamlInterop.IListViewBase_get_IsItemClickEnabled)
        set(value) = listViewBase.putBool(XamlInterop.IListViewBase_put_IsItemClickEnabled, value)

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

    /** Selects all items (ListViewBase.SelectAll). Only effective in Multiple / Extended mode. */
    fun selectAll() {
        listViewBase.call(XamlInterop.IListViewBase_SelectAll)
    }

    /** ListSelectionListener-like. Subscribes to Selector.SelectionChanged under the hood. */
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

    /**
     * Subscribes to item clicks (ListViewBase.ItemClick).
     * The listener receives the clicked item's string.
     * Only fires while [isItemClickEnabled] is true.
     */
    @JvmSynthetic
    fun addItemClickListener(listener: (String) -> Unit) {
        val adapter = Consumer<String> { listener(it) }
        addItemClickListenerForJava(adapter)
        itemClickTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addItemClickListener")
    fun addItemClickListenerForJava(listener: Consumer<String>) {
        val token = listViewBase.addEventHandler(
            "WinUI4K.ItemClickHandler",
            XamlInterop.IID_ItemClickEventHandler,
            XamlInterop.IListViewBase_add_ItemClick,
        ) { _, args ->
            // args is ItemClickEventArgs's default interface, so it can be called directly
            val e = ComPtr(args)
            val boxed = e.getPtr(XamlInterop.IItemClickEventArgs_get_ClickedItem)
            val item = try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
            listener.accept(item)
        }
        itemClickTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemClickListener]. */
    @JvmSynthetic
    fun removeItemClickListener(listener: (String) -> Unit) {
        val adapter = itemClickTokens.removeKotlinAdapter(listener) ?: return
        removeItemClickListenerForJava(adapter)
    }

    @JvmName("removeItemClickListener")
    fun removeItemClickListenerForJava(listener: Consumer<String>) {
        val token = itemClickTokens.remove(listener) ?: return
        listViewBase.removeEventHandler(XamlInterop.IListViewBase_remove_ItemClick, token)
    }
}
