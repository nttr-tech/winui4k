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
 *
 * The selection can also be handled in the same form as Swing's JList: [selectionModel] ([WListSelectionModel], synced with the native selection both ways),
 * [setSelectionMode] (Swing constants), [selectedIndices] / [setSelectionInterval] / [addSelectionInterval] /
 * [removeSelectionInterval] / [clearSelection] / [isSelectedIndex] / [isSelectionEmpty] /
 * [minSelectionIndex] / [maxSelectionIndex] / [anchorSelectionIndex] / [leadSelectionIndex] / [setSelectedValue],
 * and [addListSelectionListener] with a [WListSelectionListener] (the source is this list).
 * The default selection mode is single selection, as in WinUI (Swing's JList defaults to multiple interval selection). WinUI's MULTIPLE (check boxes
 * toggled by clicking) is not Swing's MULTIPLE_INTERVAL_SELECTION; Swing's MULTIPLE_INTERVAL_SELECTION corresponds to
 * WinUI's EXTENDED (Ctrl / Shift). [selectionMode] stays the WinUI enumeration, and MULTIPLE can also be chosen with [selectionMode]
 * (the selectionModel's selection mode is then MULTIPLE_INTERVAL_SELECTION).
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

    /** A gate that collapses notifications to Runnable selection listeners into one per Swing-style operation. */
    private val selectionGate = SelectionChangeGate()

    private val selectionModes = object : NativeListSelectionModes {
        override fun isSingle(): Boolean = selectionMode == SelectionMode.SINGLE

        override fun isNone(): Boolean = false

        override fun swingMode(): Int =
            if (isSingle()) WListSelectionModel.SINGLE_SELECTION else WListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        override fun setNative(single: Boolean) = writeSelectionMode(if (single) SelectionMode.SINGLE else SelectionMode.EXTENDED)
    }

    private val selectionSyncHolder = lazy {
        // resolve does not capture the wrapper (it looks it up from the argument wrapper on every call)
        ListSelectionSync(this, listItems, selector, selectionModes, selectionGate) { list: WListBox -> list.selectionSync }
    }

    /** The core of the Swing-style selection API (starts syncing with the native selection on first use). */
    internal val selectionSync: ListSelectionSync<WListBox>
        get() = selectionSyncHolder.value

    /**
     * The selection model (JList.getSelectionModel / setSelectionModel). Synced with the native selection both ways
     * (a selection made in the display is copied to the model, and direct changes to the model are reflected in the display).
     *
     * The default is a [WDefaultListSelectionModel] built from the current native mode and selection. Change the mode with [setSelectionMode]
     * (calling the model's setSelectionMode directly does not change the display mode).
     * Replacing it changes the display mode according to whether the new model is single selection, and copies the new model's selection to the display.
     * WinUI does not report operations in progress, so valueIsAdjusting of events from display operations is always false.
     * Using this API (or another Swing-style selection API) for the first time starts syncing with native, and from then on, as when listeners are added,
     * a shown list is not collected even without references to the wrapper.
     * Also, as in Swing, the selection model references the list through its listener, so the list is not collected while the model is held.
     */
    var selectionModel: WListSelectionModel
        get() = selectionSync.call { it }
        set(value) {
            selectionSync.model = value
        }

    /**
     * Sets the Swing selection mode (a ListSelectionModel constant) (JList.setSelectionMode).
     * SINGLE_SELECTION becomes native single selection, and SINGLE_INTERVAL_SELECTION and MULTIPLE_INTERVAL_SELECTION become
     * Ctrl / Shift multiple selection (EXTENDED). In SINGLE_INTERVAL_SELECTION, adding a disjoint range with Ctrl makes the selection only that range.
     * If the current selection does not fit the new mode, it is shrunk from the front, as in Swing since JDK 9.
     * Read it with `selectionModel.selectionMode` (corresponds to Swing's getSelectionMode(): int; [selectionMode] is the WinUI enumeration).
     * @throws IllegalArgumentException if it is none of the constants
     */
    fun setSelectionMode(selectionMode: Int) {
        require(selectionMode in WListSelectionModel.SINGLE_SELECTION..WListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
            "invalid selectionMode: $selectionMode"
        }
        selectionSync.changeMode(selectionMode) {
            writeSelectionMode(if (selectionMode == WListSelectionModel.SINGLE_SELECTION) SelectionMode.SINGLE else SelectionMode.EXTENDED)
        }
    }

    /**
     * All selected indices (JList.getSelectedIndices / setSelectedIndices), in ascending order.
     * As in Swing, setting clears the selection and then adds the indices within the element range one by one.
     */
    var selectedIndices: IntArray
        get() = selectionSync.call { it.getSelectedIndices() }
        set(value) = selectionSync.setSelectedIndices(value)

    /** Selects only [anchor]..[lead] (JList.setSelectionInterval). Only [lead] in single selection mode. */
    fun setSelectionInterval(anchor: Int, lead: Int) = selectionSync.call { it.setSelectionInterval(anchor, lead) }

    /** Adds [anchor]..[lead] to the selection (JList.addSelectionInterval). */
    fun addSelectionInterval(anchor: Int, lead: Int) = selectionSync.call { it.addSelectionInterval(anchor, lead) }

    /** Removes [index0]..[index1] from the selection (JList.removeSelectionInterval). */
    fun removeSelectionInterval(index0: Int, index1: Int) = selectionSync.call { it.removeSelectionInterval(index0, index1) }

    /** Clears the whole selection (JList.clearSelection). */
    fun clearSelection() = selectionSync.call { it.clearSelection() }

    /** true if [index] is selected (JList.isSelectedIndex). */
    fun isSelectedIndex(index: Int): Boolean = selectionSync.call { it.isSelectedIndex(index) }

    /** true if nothing is selected (JList.isSelectionEmpty). */
    val isSelectionEmpty: Boolean
        get() = selectionSync.call { it.isSelectionEmpty() }

    /** The smallest selected index. -1 if unselected (JList.getMinSelectionIndex). */
    val minSelectionIndex: Int
        get() = selectionSync.call { it.getMinSelectionIndex() }

    /** The largest selected index. -1 if unselected (JList.getMaxSelectionIndex). */
    val maxSelectionIndex: Int
        get() = selectionSync.call { it.getMaxSelectionIndex() }

    /** The start of the operation that last changed the selection (JList.getAnchorSelectionIndex). -1 if none. */
    val anchorSelectionIndex: Int
        get() = selectionSync.call { it.getAnchorSelectionIndex() }

    /** The end of the operation that last changed the selection (JList.getLeadSelectionIndex). -1 if none. */
    val leadSelectionIndex: Int
        get() = selectionSync.call { it.getLeadSelectionIndex() }

    /**
     * The selection model's valueIsAdjusting (JList.getValueIsAdjusting / setValueIsAdjusting).
     * It stays false for display operations (WinUI does not report operations in progress).
     */
    var valueIsAdjusting: Boolean
        get() = selectionSync.call { it.getValueIsAdjusting() }
        set(value) = selectionSync.call { it.setValueIsAdjusting(value) }

    /**
     * Selects the first element equal to [anObject] by equals (JList.setSelectedValue). If [shouldScroll] is true, scrolls to
     * that element. Deselects if null. As in Swing, the selection is not changed if it is already selected (the frontmost selection is equal)
     * or there is no equal element.
     */
    fun setSelectedValue(anObject: Any?, shouldScroll: Boolean) {
        selectionSync.setSelectedValue(anObject) { index ->
            if (shouldScroll) listItems.withItem(index) { item -> inspectable.call(XamlInterop.IListBox_ScrollIntoView, item.ptr) }
        }
    }

    /**
     * ListSelectionListener-like: receives selection changes as [WListSelectionEvent]s (JList.addListSelectionListener).
     * The event is the selection model's event passed on with this list as source. Called for display operations as well as
     * for Swing-style APIs and direct changes to the selection model. Called in the order added.
     * The no-arg Runnable / `() -> Unit` form (called on every WinUI SelectionChanged) can still be used.
     */
    fun addListSelectionListener(listener: WListSelectionListener) = selectionSync.addListener(listener)

    /** Removes a [WListSelectionListener] added with [addListSelectionListener]. */
    fun removeListSelectionListener(listener: WListSelectionListener) = selectionSync.removeListener(listener)

    /** The added [WListSelectionListener]s (JList.getListSelectionListeners). */
    val listSelectionListeners: Array<WListSelectionListener>
        get() = selectionSync.listeners()

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
        set(value) {
            if (selectionSyncHolder.isInitialized()) selectionSync.changeMode(null) { writeSelectionMode(value) } else writeSelectionMode(value)
        }

    private fun writeSelectionMode(value: SelectionMode) = inspectable.call(XamlInterop.IListBox_put_SelectionMode, value.native)

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
        // The handler captures only the gate and the listener (the gate collapses notifications into one during Swing-style operations)
        val gate = selectionGate
        val key = Any()
        val token = selector.addEventHandler(
            "WinUI4K.SelectionChangedHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ISelector_add_SelectionChanged,
        ) { _, _ -> gate.deliver(key, listener) }
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
