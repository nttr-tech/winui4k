package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Dispatcher
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * JComboBox-like: WinUI 3's ComboBox (a Selector subclass).
 *
 * The items and the selected item are held by [model] ([WComboBoxModel]; [WDefaultComboBoxModel] by default), and the native ComboBox.Items get
 * the `toString()` strings of the model's items. Model changes ([WListDataListener] notifications) are copied to native, and native selection changes made
 * by the user are written back to the model's [WComboBoxModel.selectedItem]. One model can be shared by several combos.
 *
 * Item operations: [addItem] / [insertItemAt] / [getItemAt] / [removeItemAt] / [removeItem] / [removeAllItems] / [itemCount]
 * (adding and removing only with a [WMutableComboBoxModel] model). Selection: [selectedIndex] / [selectedItem] (the displayed string) /
 * [setSelectedItem] / [getSelectedObjects] (the model's items themselves). Also [placeholderText] / [header],
 * [isEditable] / [text] / [addTextSubmitListener] (for an editable combo), [showPopup] / [hidePopup] / [isPopupVisible] /
 * [maximumRowCount] (drop-down).
 *
 * Events can be received in the same form as JComboBox:
 * - [addItemListener]: when the selected item changes, the previous item's DESELECTED arrives, then the new item's SELECTED ([WItemEvent.item] is the model's item).
 *   A change is detected by "whether the selected items are equal", so moving to another equal item delivers nothing (as in Swing)
 * - [addActionListener]: arrives after the item events ([actionCommand] is `"comboBoxChanged"` by default). [selectedIndex] /
 *   [setSelectedItem] deliver it once even if the selection does not change, as in Swing. It is also delivered when the model's selection changes or items are removed,
 *   under the same conditions as JComboBox. Confirming the input with Enter in an editable combo delivers `"comboBoxEdited"` after the selection change events
 * - [addPopupMenuListener]: opening and closing of the drop-down (ComboBox.DropDownOpened / DropDownClosed)
 *
 * The existing [addListSelectionListener] (a Runnable called on every SelectionChanged) can still be used.
 *
 * Differences from Swing:
 * - Reselecting the current item from the popup delivers no ActionEvent (WinUI reports nothing when the selection does not change)
 * - [popupMenuWillBecomeVisible][WPopupMenuListener.popupMenuWillBecomeVisible] arrives after it starts opening, and
 *   [popupMenuWillBecomeInvisible][WPopupMenuListener.popupMenuWillBecomeInvisible] after it has finished closing
 * - The models made by the constructor taking a List and the no-arg constructor still select nothing when items are added, and removing the selected item leaves it unselected
 *   (JComboBox selects the first item and selects an adjacent one on removal. The constructors taking an array or a model behave like JComboBox)
 * - When the model selects a value not in the list, a non-editable combo shows nothing ([selectedItem] is that value's string, [selectedIndex] is -1).
 *   An editable combo shows that string (after the template that lets WinUI accept custom values is applied, that is, after it is in a window)
 */
class WComboBox @JvmOverloads constructor(items: List<String> = emptyList()) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ComboBox, XamlInterop.IID_IComboBoxFactory), // default interface = IComboBox
) {
    /** JComboBox(E[] items)-like: creates a combo with a [WDefaultComboBoxModel] of [items] and the first item selected, as with JComboBox (unselected if empty). */
    constructor(items: Array<String>) : this() {
        replaceModel(WDefaultComboBoxModel(items)) // A model it created itself, so do not pin ([pinForModel] is for when [model] is read or written)
    }

    /** JComboBox(ComboBoxModel)-like: creates a combo that shows the items and selection of [model]. */
    constructor(model: WComboBoxModel<*>) : this() {
        this.model = model
    }

    private val selector: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_ISelector))
    }

    /** The IVector<Object> view of ItemsControl.Items (ItemCollection). */
    private val itemVector: ComPtr by lazy {
        val itemsControl = own(inspectable.queryInterface(XamlInterop.IID_IItemsControl))
        val items = own(itemsControl.getPtr(XamlInterop.IItemsControl_get_Items))
        own(items.queryInterface(FoundationInterop.IID_IVector_Object))
    }

    /** SelectionChanged event tokens registered via addListSelectionListener. */
    private val selectionTokens = ListenerTokens<Runnable>()

    /** TextSubmitted event tokens registered via addTextSubmitListener. */
    private val textSubmitTokens = ListenerTokens<Consumer<String>>()

    /** The ItemListeners. The native SelectionChanged is always subscribed in init ([addSelectionHandler]). */
    private val itemListeners = ItemListeners(this) {}

    /** The ActionListeners. Enter confirmation (TextSubmitted) is subscribed when the first listener is added. */
    private val actionListeners = AnchoredListeners<WActionListener>(this) { addTextSubmittedHandler() }

    /** The PopupMenuListeners. DropDownOpened / DropDownClosed are subscribed when the first listener is added. */
    private val popupListeners = AnchoredListeners<WPopupMenuListener>(this) { addDropDownHandlers() }

    /** JComboBox's selectedItemReminder: the selected item last reported by item events (null if unselected). */
    private var selectedItemReminder: Any? = null

    /** Whether the selection is being changed programmatically (native notifications during that time do not fire ActionEvents; the changer fires one at the end). */
    private var selectingItem = false

    /** Whether ActionEvents are being delivered (ActionEvents during that time are not fired, as in JComboBox, to prevent endless repetition when a listener changes the selection). */
    private var firingActionEvent = false

    /** Whether the comboBoxEdited ActionEvent has not been fired yet after an Enter confirmation (TextSubmitted). */
    private var pendingEdited = false

    /** Decides how the drop-down closed (whether it was a cancellation). */
    private val popupClose = ComboBoxPopupClose()

    /** The model of items and selection. The default is a [WDefaultComboBoxModel] that does not move the selection, like the previous WComboBox. */
    private var dataModel: WComboBoxModel<*> = WDefaultComboBoxModel<String>(movesSelection = false)

    /**
     * A listener that copies model changes to native and passes them to this combo's listeners, as in JComboBox. The model references the combo strongly through it,
     * but the native side references the combo only weakly ([addSelectionHandler]), so no cycle forms across the boundary.
     */
    private val modelListener = object : WListDataListener {
        override fun intervalAdded(e: WListDataEvent) = onIntervalAdded(e)

        override fun intervalRemoved(e: WListDataEvent) = onIntervalRemoved(e)

        override fun contentsChanged(e: WListDataEvent) = onContentsChanged(e)
    }

    /** Whether the model state is being copied to native (native SelectionChanged during that time is not written back to the model). */
    private var pushing = false

    private var loadedHandlerAdded = false

    /** The [selectedIndex] set while there were no items (selected when an item at that position is added). -1 if none. */
    private var pendingSelectedIndex = -1

    /** Whether [model] has been read or written (the first time, the combo is pinned with ListenerAnchor; [pinForModel]). */
    private var modelExposed = false

    /**
     * JComboBox.getActionCommand / setActionCommand-like: the [WActionEvent.actionCommand] of [addActionListener] events.
     * The default is `"comboBoxChanged"`, as in JComboBox. An Enter confirmation in an editable combo uses `"comboBoxEdited"` instead of this value.
     */
    var actionCommand: String = COMBO_BOX_CHANGED

    /**
     * JComboBox.getModel / setModel-like: the model of items and selection. Replacing it matches the native items to the new model, but as in JComboBox
     * listeners are not called. The model references the combo through its listener, so holding on to the model keeps the combo from being collected too (as in Swing).
     * A combo whose model has been read or written even once (including the constructor that takes a model) is, for the other components sharing the model, the same as when listeners are added:
     * it is not collected while shown even without references (it is not collected if never put in a window; it is collected after being put in and removed).
     */
    var model: WComboBoxModel<*>
        get() {
            pinForModel()
            return dataModel
        }
        set(newModel) {
            pinForModel()
            replaceModel(newModel)
        }

    /** The number of items (getSize of [model]). */
    val itemCount: Int
        get() = dataModel.getSize()

    /**
     * The selected index. -1 if unselected or if a value not in the list is selected.
     * As with JComboBox.setSelectedIndex, values outside -1 to `itemCount - 1` throw [IllegalArgumentException], and like [setSelectedItem]
     * it selects that item (the [addActionListener] listeners are called once even if the selection does not change).
     *
     * Difference from Swing: setting 0 or more while there are no items does not throw; the value is remembered and the item at that position is selected once when it is added
     * (the same as the previous WComboBox and WinUI's Selector; `WComboBox().apply { selectedIndex = 2; addItem("a"); addItem("b"); addItem("c") }`
     * selects "c"). While remembered it reads as -1, and setting it again (including -1) replaces it. -2 or less throws even without items.
     */
    var selectedIndex: Int
        get() {
            val selected = dataModel.selectedItem ?: return -1
            val native = nativeSelectedIndex
            if (native in 0 until itemCount && dataModel.getElementAt(native) == selected) return native
            return indexOfItem(selected)
        }
        set(value) {
            if (value >= 0 && itemCount == 0) {
                pendingSelectedIndex = value
                return
            }
            require(value in -1 until itemCount) { "setSelectedIndex: $value out of bounds" }
            pendingSelectedIndex = -1
            if (value == -1) {
                setSelectedItem(null)
                return
            }
            val element = dataModel.getElementAt(value)
            setSelectedItem(element)
            // Even if an equal item appears earlier, show the given index as selected (same as the previous WComboBox)
            if (nativeSelectedIndex != value && dataModel.selectedItem == element) push { putNativeSelectedIndex(value) }
        }

    /**
     * The display string of the selected item (`toString()` of the selectedItem of [model]). null if unselected. When a string not in the list is
     * confirmed in an editable combo, that string ([selectedIndex] is then -1). Use [setSelectedItem] to change it. The item itself is [getSelectedObjects] or [model].
     */
    val selectedItem: String?
        get() = dataModel.selectedItem?.toString()

    /** The placeholder text shown when nothing is selected (ComboBox.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(XamlInterop.IComboBox_get_PlaceholderText)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IComboBox_put_PlaceholderText, h) }

    /** The heading above the combo box (ComboBox.Header). It is of type Object, so a boxed string is passed and read back from native ("" for non-strings). */
    var header: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.IComboBox_get_Header) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IComboBox_put_Header, boxed.ptr)
            boxed.release()
        }

    /** Whether a string outside the list of choices can be typed in (ComboBox.IsEditable). */
    var isEditable: Boolean
        get() = inspectable.getBool(XamlInterop.IComboBox_get_IsEditable)
        set(value) {
            inspectable.putBool(XamlInterop.IComboBox_put_IsEditable, value)
            syncNativeSelection()
        }

    /** The typed-in text for an editable combo (ComboBox.Text). */
    var text: String
        get() = inspectable.getString(XamlInterop.IComboBox_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IComboBox_put_Text, h) }

    /** Whether the drop-down is open (ComboBox.IsDropDownOpen). Same as [isPopupVisible]. */
    var isDropDownOpen: Boolean
        get() = inspectable.getBool(XamlInterop.IComboBox_get_IsDropDownOpen)
        set(value) {
            if (!value) popupClose.hideRequested = true
            inspectable.putBool(XamlInterop.IComboBox_put_IsDropDownOpen, value)
        }

    /**
     * JComboBox.isPopupVisible / setPopupVisible-like: whether the drop-down is open (ComboBox.IsDropDownOpen).
     * Setting it to true before the combo is in a window does not open it (WinUI behavior).
     */
    var isPopupVisible: Boolean
        get() = isDropDownOpen
        set(value) {
            isDropDownOpen = value
        }

    /**
     * JComboBox.getMaximumRowCount / setMaximumRowCount-like: the number of rows shown in the drop-down without scrolling. The default is 8, as in JComboBox.
     * WinUI has no row count setting, so setting it sets ComboBox.MaxDropDownHeight to "(rows + 0.5) × item height + 10 DIP top and bottom padding".
     * WinUI lays out as many whole items as fit in this height and shrinks the drop-down to that height (GetNonPannablePopupLayout in ComboBox_Partial.cpp), so
     * with half a row of slack, exactly the number of rows is laid out even if the measured height is slightly off.
     * The item height used is the actual height of the items (ComboBoxItem) laid out each time the drop-down opens (so the row count matches even if the font or style changes).
     * Until it has been opened, the 36 DIP estimate from the default theme and 14px body text is used.
     * Until set, it keeps the native default (expanding as far as fits on screen). The drop-down is also cut by the window height (WinUI behavior).
     */
    var maximumRowCount: Int = DEFAULT_MAXIMUM_ROW_COUNT
        set(value) {
            field = value
            addRowHeightHandler()
            applyMaxDropDownHeight()
        }

    /** The item height (DIP) measured when the drop-down opened. 0 if not measured yet. */
    private var measuredRowHeight = 0.0

    private var rowHeightHandlerAdded = false

    init {
        dataModel.addListDataListener(modelListener)
        addSelectionHandler()
        for (item in items) addItem(item)
    }

    /** Adds an item at the end ([WMutableComboBoxModel.addElement]). Throws [UnsupportedOperationException] if the model is not mutable. */
    fun addItem(item: String) {
        mutableModel().addElement(item)
    }

    /** JComboBox.insertItemAt-like: inserts an item at [index] ([WMutableComboBoxModel.insertElementAt]). */
    fun insertItemAt(item: String, index: Int) {
        mutableModel().insertElementAt(item, index)
    }

    /** Returns the display string of the item at [index]. Throws [IndexOutOfBoundsException] if out of range ([getItemAt] returns null when out of range). */
    fun getItem(index: Int): String {
        if (index !in 0 until itemCount) throw IndexOutOfBoundsException("index: $index, size: $itemCount")
        return textOf(dataModel.getElementAt(index))
    }

    /** JComboBox.getItemAt-like: the display string of the item at [index]. null if out of range (the item itself is getElementAt of [model]). */
    fun getItemAt(index: Int): String? = if (index in 0 until itemCount) getItem(index) else null

    /** Removes the item at [index]. Same as JComboBox's removeItemAt ([removeItemAt] is the Swing way to write it). */
    fun removeItem(index: Int) {
        mutableModel().removeElementAt(index)
    }

    /** JComboBox.removeItemAt-like: removes the item at [index] ([WMutableComboBoxModel.removeElementAt]). */
    fun removeItemAt(index: Int) {
        removeItem(index)
    }

    /** JComboBox.removeItem(Object)-like: removes the first item equal to [item] ([WMutableComboBoxModel.removeElement]). Does nothing if there is none. */
    fun removeItem(item: Any?) {
        mutableModel().removeElement(item)
    }

    /** Removes all items. If an item was selected, DESELECTED and an ActionEvent are delivered, as in JComboBox. */
    fun removeAllItems() {
        val mutable = mutableModel()
        val current = dataModel
        if (current is WDefaultComboBoxModel<*>) {
            current.removeAllElements()
        } else {
            repeat(mutable.getSize()) { mutable.removeElement(mutable.getElementAt(0)) }
        }
        selectedItemReminder = null
    }

    /**
     * JComboBox.setSelectedItem-like: selects the item equal to [item] (deselects if null; the equal item itself is put into the model).
     * In a non-editable combo, values not in the list are ignored (no events either). If editable, a value not in the list is selected as is
     * (displayed as its `toString()` string, [selectedIndex] is -1). As in Swing, the [addActionListener] listeners are called once even if the selection does not change.
     */
    fun setSelectedItem(item: Any?) {
        val old = selectedItemReminder
        if (old == null || old != item) {
            var toSelect = item
            if (item != null && !isEditable) {
                val index = indexOfItem(item)
                if (index < 0) return
                toSelect = dataModel.getElementAt(index)
            }
            selectInModel(toSelect)
        }
        fireActionEvent(actionCommand)
    }

    /** JComboBox.getSelectedObjects-like: an array of only the selected item (empty if unselected). */
    fun getSelectedObjects(): Array<Any> = dataModel.selectedItem?.let { arrayOf(it) } ?: emptyArray()

    /** JComboBox.showPopup-like: opens the drop-down. */
    fun showPopup() {
        isPopupVisible = true
    }

    /** JComboBox.hidePopup-like: closes the drop-down. */
    fun hidePopup() {
        isPopupVisible = false
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
     * ItemListener-like: receives changes of the selected item as [WItemEvent]s. The previous item's [WItemEvent.DESELECTED] is followed by the new item's
     * [WItemEvent.SELECTED]; [WItemEvent.item] is the model's item itself (not its string display) and the source is this combo. Called for both user operations and programmatic settings.
     */
    @JvmSynthetic
    fun addItemListener(listener: (WItemEvent) -> Unit) {
        itemListeners.addAdapter(listener, WItemListener(listener))
    }

    /** ItemListener-like: receives changes of the selected item with a [WItemListener] (same as the Kotlin lambda form). */
    fun addItemListener(listener: WItemListener) {
        itemListeners.add(listener)
    }

    /** Removes one lambda added with [addItemListener]. */
    @JvmSynthetic
    fun removeItemListener(listener: (WItemEvent) -> Unit) {
        itemListeners.removeAdapter(listener)
    }

    /** Removes one [WItemListener] added with [addItemListener]. */
    fun removeItemListener(listener: WItemListener) {
        itemListeners.remove(listener)
    }

    /** JComboBox.getItemListeners-like: the ItemListeners currently added (in the order added; Kotlin lambdas appear wrapped). */
    fun getItemListeners(): Array<WItemListener> = itemListeners.toArray()

    /**
     * ActionListener-like: receives selection changes (after the item events) and Enter confirmations in an editable combo as [WActionEvent]s.
     * The source is this combo and [WActionEvent.actionCommand] is [actionCommand] (`"comboBoxEdited"` for a confirmation).
     */
    @JvmSynthetic
    fun addActionListener(listener: (WActionEvent) -> Unit) {
        actionListeners.addAdapter(listener, WActionListener(listener))
    }

    /** ActionListener-like: receives selection changes and input confirmations with a [WActionListener] (same as the Kotlin lambda form). */
    fun addActionListener(listener: WActionListener) {
        actionListeners.add(listener)
    }

    /** Removes one lambda added with [addActionListener]. */
    @JvmSynthetic
    fun removeActionListener(listener: (WActionEvent) -> Unit) {
        actionListeners.removeAdapter(listener)
    }

    /** Removes one [WActionListener] added with [addActionListener]. */
    fun removeActionListener(listener: WActionListener) {
        actionListeners.remove(listener)
    }

    /** JComboBox.getActionListeners-like: the ActionListeners currently added (in the order added; Kotlin lambdas appear wrapped). */
    fun getActionListeners(): Array<WActionListener> = actionListeners.toList().toTypedArray()

    /**
     * PopupMenuListener-like: receives the opening and closing of the drop-down (the source is this combo). Opening delivers
     * [popupMenuWillBecomeVisible][WPopupMenuListener.popupMenuWillBecomeVisible], and closing delivers
     * [popupMenuWillBecomeInvisible][WPopupMenuListener.popupMenuWillBecomeInvisible]. As in Swing, only closing with Esc or an outside click (or losing the focus)
     * without choosing an item is treated as a cancellation, and
     * [popupMenuCanceled][WPopupMenuListener.popupMenuCanceled] arrives before that. Reselecting the current item, closing with [hidePopup] /
     * `isPopupVisible = false`, and closing with Tab, F4 or Alt + ↑↓ are not cancellations.
     */
    fun addPopupMenuListener(listener: WPopupMenuListener) {
        popupListeners.add(listener)
    }

    /** Removes one listener added with [addPopupMenuListener]. */
    fun removePopupMenuListener(listener: WPopupMenuListener) {
        popupListeners.remove(listener)
    }

    /** JComboBox.getPopupMenuListeners-like: the PopupMenuListeners currently added (in the order added). */
    fun getPopupMenuListeners(): Array<WPopupMenuListener> = popupListeners.toList().toTypedArray()

    /**
     * Subscribes to text being committed with Enter on an editable combo (ComboBox.TextSubmitted).
     * The listener receives the committed string. Only fires while [isEditable] is true.
     */
    @JvmSynthetic
    fun addTextSubmitListener(listener: (String) -> Unit) {
        val adapter = Consumer<String> { listener(it) }
        addTextSubmitListenerForJava(adapter)
        textSubmitTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addTextSubmitListener")
    fun addTextSubmitListenerForJava(listener: Consumer<String>) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TextSubmittedHandler",
            XamlInterop.IID_ComboBoxTextSubmittedHandler,
            XamlInterop.IComboBox_add_TextSubmitted,
        ) { _, args ->
            // args is a ComboBoxTextSubmittedEventArgs; read the committed Text and pass it along
            listener.accept(ComPtr(args).getString(XamlInterop.IComboBoxTextSubmittedEventArgs_get_Text))
        }
        textSubmitTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTextSubmitListener]. */
    @JvmSynthetic
    fun removeTextSubmitListener(listener: (String) -> Unit) {
        val adapter = textSubmitTokens.removeKotlinAdapter(listener) ?: return
        removeTextSubmitListenerForJava(adapter)
    }

    @JvmName("removeTextSubmitListener")
    fun removeTextSubmitListenerForJava(listener: Consumer<String>) {
        val token = textSubmitTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IComboBox_remove_TextSubmitted, token)
    }

    /** The index of the first item equal to [item]. -1 if none. */
    private fun indexOfItem(item: Any): Int = (0 until itemCount).firstOrNull { dataModel.getElementAt(it) == item } ?: -1

    @Suppress("UNCHECKED_CAST")
    private fun mutableModel(): WMutableComboBoxModel<Any?> = dataModel as? WMutableComboBoxModel<Any?>
        ?: throw UnsupportedOperationException("Cannot use this method with a non-Mutable data model.")

    /** Sets the model's selection to [item]. Model notifications during that time do not fire ActionEvents (the caller fires them). The middle part of JComboBox.setSelectedItem. */
    private fun selectInModel(item: Any?) {
        val was = selectingItem
        selectingItem = true
        try {
            dataModel.selectedItem = item
        } finally {
            selectingItem = was
        }
        // Fire item events even for model implementations that do not report selection changes (same as JComboBox)
        if (selectedItemReminder !== dataModel.selectedItem) selectedItemChanged()
    }

    /** JComboBox.selectedItemChanged: reports the previous item's DESELECTED and the new item's SELECTED. */
    private fun selectedItemChanged() {
        selectedItemReminder?.let { itemListeners.fire(WItemEvent(this, WItemEvent.DESELECTED, it)) }
        val newItem = dataModel.selectedItem
        selectedItemReminder = newItem
        newItem?.let { itemListeners.fire(WItemEvent(this, WItemEvent.SELECTED, it)) }
    }

    private fun onIntervalAdded(e: WListDataEvent) {
        val nativeSize = nativeItemCount
        if (e.index0 in 0..nativeSize && nativeSize + (e.index1 - e.index0 + 1) == itemCount) {
            push { for (i in e.index0..e.index1) insertNative(i, textOf(dataModel.getElementAt(i))) }
            syncNativeSelection()
        } else {
            rebuildNative()
        }
        if (selectedItemReminder !== dataModel.selectedItem) selectedItemChanged()
        selectPendingIndex()
    }

    /** Selects the item when an item comes to the position of the [selectedIndex] set while there were no items (same as m_selectedIndexValueSetBeforeItemsAvailable of WinUI's Selector). */
    private fun selectPendingIndex() {
        if (pendingSelectedIndex in 0 until itemCount) selectedIndex = pendingSelectedIndex
    }

    private fun onIntervalRemoved(e: WListDataEvent) {
        val nativeSize = nativeItemCount
        if (e.index0 >= 0 && e.index1 < nativeSize && nativeSize - (e.index1 - e.index0 + 1) == itemCount) {
            push { for (i in e.index1 downTo e.index0) itemVector.call(FoundationInterop.IVector_RemoveAt, i) }
            syncNativeSelection()
        } else {
            rebuildNative()
        }
        onSelectionMaybeChanged()
    }

    private fun onContentsChanged(e: WListDataEvent) {
        if (e.index0 >= 0) {
            if (nativeItemCount == itemCount && e.index1 < itemCount) {
                push { for (i in e.index0..e.index1) setNative(i, textOf(dataModel.getElementAt(i))) }
                syncNativeSelection()
            } else {
                rebuildNative()
            }
        } else {
            syncNativeSelection()
        }
        onSelectionMaybeChanged()
    }

    /** JComboBox.contentsChanged's handling of the selection (same as Swing, down to firing an ActionEvent even without a selection change when nothing was selected before). */
    private fun onSelectionMaybeChanged() {
        val old = selectedItemReminder
        if (old == null || old != dataModel.selectedItem) {
            selectedItemChanged()
            if (!selectingItem) fireActionEvent(actionCommand)
        }
    }

    private val nativeItemCount: Int
        get() = itemVector.getInt(FoundationInterop.IVector_get_Size)

    private val nativeSelectedIndex: Int
        get() = selector.getInt(XamlInterop.ISelector_get_SelectedIndex)

    /** The string of the native SelectedItem (null if unselected). */
    private val nativeSelectedText: String?
        get() {
            val boxed = selector.getPtrOrNull(XamlInterop.ISelector_get_SelectedItem) ?: return null
            return try {
                PropertyValues.unboxString(boxed)
            } finally {
                boxed.release()
            }
        }

    /** Runs [block], which copies from the model to native, without writing native notifications back to the model. */
    private inline fun push(block: () -> Unit) {
        val was = pushing
        pushing = true
        try {
            block()
        } finally {
            pushing = was
        }
    }

    private fun insertNative(index: Int, text: String) {
        val boxed = PropertyValues.boxString(text)
        try {
            itemVector.call(FoundationInterop.IVector_InsertAt, index, boxed.ptr)
        } finally {
            boxed.release()
        }
    }

    private fun setNative(index: Int, text: String) {
        val boxed = PropertyValues.boxString(text)
        try {
            itemVector.call(FoundationInterop.IVector_SetAt, index, boxed.ptr)
        } finally {
            boxed.release()
        }
    }

    private fun putNativeSelectedIndex(index: Int) {
        selector.call(XamlInterop.ISelector_put_SelectedIndex, index)
    }

    /** Rebuilds the native items from the model's items and matches the selection too. */
    private fun rebuildNative() {
        push {
            itemVector.call(FoundationInterop.IVector_Clear)
            for (i in 0 until itemCount) {
                val boxed = PropertyValues.boxString(textOf(dataModel.getElementAt(i)))
                try {
                    itemVector.call(FoundationInterop.IVector_Append, boxed.ptr)
                } finally {
                    boxed.release()
                }
            }
        }
        syncNativeSelection()
    }

    /**
     * Matches the native selection to the model's selectedItem. A value not in the list is passed as a custom value only when an editable combo is in a window.
     * Otherwise it shows as unselected (when WinUI does not accept a custom value and there is no ItemsSource, it remembers the value and selects an item with the same value
     * added later (m_itemPendingSelection in OnSelectedItemChanged of Selector_Partial.cpp), so it is not passed).
     */
    private fun syncNativeSelection() {
        val selected = dataModel.selectedItem ?: return clearNativeSelection()
        val native = nativeSelectedIndex
        if (native in 0 until itemCount && dataModel.getElementAt(native) == selected) return
        val index = indexOfItem(selected)
        when {
            index >= 0 -> push { putNativeSelectedIndex(index) }
            isEditable && isLoaded -> putNativeCustomValue(selected.toString())
            else -> {
                if (isEditable) addLoadedHandler()
                clearNativeSelection()
            }
        }
    }

    /** Shows the native side as unselected (does nothing if already unselected). */
    private fun clearNativeSelection() {
        if (nativeSelectedIndex >= 0 || nativeSelectedText != null) push { selector.call(XamlInterop.ISelector_put_SelectedItem, null) }
    }

    /** Passes a value [text] not in the list to the native side of an editable combo as a custom value (does nothing if it already has that value). */
    private fun putNativeCustomValue(text: String) {
        if (nativeSelectedIndex < 0 && nativeSelectedText == text) return
        val boxed = PropertyValues.boxString(text)
        try {
            push { selector.call(XamlInterop.ISelector_put_SelectedItem, boxed.ptr) }
        } finally {
            boxed.release()
        }
    }

    private val isLoaded: Boolean
        get() = frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded)

    private fun fireActionEvent(command: String) {
        if (actionListeners.isEmpty() || firingActionEvent) return
        firingActionEvent = true
        try {
            val event = WActionEvent(this, command, WActionEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()))
            for (listener in actionListeners.toList()) listener.actionPerformed(event)
        } finally {
            firingActionEvent = false
        }
    }

    /**
     * The native selection changed. Except while copying from the model (that is, for user operations and Enter confirmations), changes the model's
     * selection, reports it and fires one ActionEvent, as with JComboBox's item selection.
     */
    private fun onNativeSelectionChanged() {
        if (pushing) return
        val index = nativeSelectedIndex
        val target = if (index in 0 until itemCount) dataModel.getElementAt(index) else nativeSelectedText
        selectInModel(target)
        fireActionEvent(actionCommand)
        flushEdited()
    }

    /** Fires the comboBoxEdited ActionEvent after an Enter confirmation, if not fired yet. */
    private fun flushEdited() {
        if (!pendingEdited) return
        pendingEdited = false
        fireActionEvent(COMBO_BOX_EDITED)
    }

    /**
     * The input was confirmed with Enter (TextSubmitted). ComboBox changes the selection right after this notification within the same call (put_SelectedIndex /
     * put_SelectedItem after TextSubmitted in ComboBox_Partial.cpp), so comboBoxEdited is fired after the notification of that selection change.
     * In case the selection did not change, it is also fired after returning to the message loop (if not fired yet).
     */
    private fun onTextSubmitted() {
        pendingEdited = true
        val comboBox = WeakReference(this)
        Dispatcher.invokeLater { comboBox.get()?.flushEdited() }
    }

    private fun onDropDownOpened() {
        popupClose.opened(inspectable, isEditable)
        val event = WPopupMenuEvent(this)
        for (listener in popupListeners.toList()) listener.popupMenuWillBecomeVisible(event)
    }

    private fun onDropDownClosed() {
        val event = WPopupMenuEvent(this)
        val canceled = popupClose.closed()
        if (canceled) for (listener in popupListeners.toList()) listener.popupMenuCanceled(event)
        for (listener in popupListeners.toList()) listener.popupMenuWillBecomeInvisible(event)
    }

    private fun replaceModel(newModel: WComboBoxModel<*>) {
        dataModel.removeListDataListener(modelListener)
        dataModel = newModel
        newModel.addListDataListener(modelListener)
        selectedItemReminder = newModel.selectedItem
        rebuildNative()
        selectPendingIndex()
    }

    /**
     * Pins the combo with ListenerAnchor only once when a model is passed or read, the same as when listeners are added.
     * The native handlers reference the combo only weakly, so without pinning it would be collected even while shown and stop writing user selections back to the model.
     */
    private fun pinForModel() {
        if (modelExposed) return
        modelExposed = true
        listenerAnchor.listenerAdded()
    }

    private fun addSelectionHandler() {
        // The handler stays on the native side, so it references the combo weakly (ListenerAnchor holds it strongly while shown with listeners)
        val comboBox = WeakReference(this)
        selector.addEventHandler(
            "WinUI4K.ComboBoxSelectionHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ISelector_add_SelectionChanged,
        ) { _, _ -> comboBox.get()?.onNativeSelectionChanged() }
    }

    /** When an editable combo is put in a window, passes the selected value not in the list to native again (it cannot be accepted before the template is applied). */
    private fun addLoadedHandler() {
        if (loadedHandlerAdded) return
        loadedHandlerAdded = true
        val comboBox = WeakReference(this)
        frameworkElement.addEventHandler(
            "WinUI4K.ComboBoxLoadedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> comboBox.get()?.syncNativeSelection() }
    }

    /** Decides MaxDropDownHeight from [maximumRowCount] rows and the measured (or, if not yet, estimated) item height (adding half a row of slack). */
    private fun applyMaxDropDownHeight() {
        val row = if (measuredRowHeight > 0.0) measuredRowHeight else ROW_HEIGHT
        inspectable.call(XamlInterop.IComboBox_put_MaxDropDownHeight, (maximumRowCount + ROW_SLACK) * row + DROP_DOWN_CHROME_HEIGHT)
    }

    /** Measures the item height every time the drop-down opens (only for combos with [maximumRowCount] set). */
    private fun addRowHeightHandler() {
        if (rowHeightHandlerAdded) return
        rowHeightHandlerAdded = true
        val comboBox = WeakReference(this)
        inspectable.addEventHandler(
            "WinUI4K.ComboBoxRowHeightHandler",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.IComboBox_add_DropDownOpened,
        ) { _, _ ->
            val opened = comboBox.get() ?: return@addEventHandler
            // Right after opening, the items may not be laid out yet, so if it cannot be measured, measure again after returning to the message loop
            if (!opened.measureRowHeight()) Dispatcher.invokeLater { comboBox.get()?.measureRowHeight() }
        }
    }

    /**
     * Measures the actual height of the first laid-out item (ComboBoxItem), and re-fits MaxDropDownHeight if it changed. true if measured.
     * While unselected, WinUI lays out items centered on the middle item, so the container of the first item may not be created yet.
     */
    private fun measureRowHeight(): Boolean {
        val mapping = inspectable.queryInterface(XamlInterop.IID_IItemContainerMapping)
        val height = try {
            val container = (0 until itemCount).firstNotNullOfOrNull {
                mapping.getPtrOrNull(XamlInterop.IItemContainerMapping_ContainerFromIndex, it)
            } ?: return false
            try {
                val element = container.queryInterface(XamlInterop.IID_IFrameworkElement)
                try {
                    element.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight)
                } finally {
                    element.release()
                }
            } finally {
                container.release()
            }
        } finally {
            mapping.release()
        }
        if (height <= 0.0) return false
        if (height != measuredRowHeight) {
            measuredRowHeight = height
            applyMaxDropDownHeight()
        }
        return true
    }

    private fun textOf(element: Any?): String = element?.toString() ?: ""

    private fun addTextSubmittedHandler() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.ComboBoxEditedHandler",
            XamlInterop.IID_ComboBoxTextSubmittedHandler,
            XamlInterop.IComboBox_add_TextSubmitted,
        ) { _, _ -> (anchor.get() as? WComboBox)?.onTextSubmitted() }
    }

    private fun addDropDownHandlers() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.ComboBoxDropDownOpenedHandler",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.IComboBox_add_DropDownOpened,
        ) { _, _ -> (anchor.get() as? WComboBox)?.onDropDownOpened() }
        inspectable.addEventHandler(
            "WinUI4K.ComboBoxDropDownClosedHandler",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.IComboBox_add_DropDownClosed,
        ) { _, _ -> (anchor.get() as? WComboBox)?.onDropDownClosed() }
    }

    companion object {
        /** JComboBox's default actionCommand. */
        private const val COMBO_BOX_CHANGED = "comboBoxChanged"

        /** The actionCommand of an Enter confirmation in an editable combo (same as JComboBox). */
        private const val COMBO_BOX_EDITED = "comboBoxEdited"

        /** JComboBox's default maximumRowCount. */
        private const val DEFAULT_MAXIMUM_ROW_COUNT = 8

        /**
         * The estimated height (DIP) of one ComboBoxItem row before it is measured. The template's LayoutRoot Margin top and bottom 2+2 and
         * ComboBoxItemThemePadding top and bottom 5+7 (the ComboBoxItem style in controls/dev/ComboBox/ComboBox_themeresources.xaml) are fixed, and
         * the body line height of 20 is an estimate that varies with the font (not in WinUI's source). After opening, the actual height is measured and used ([measureRowHeight]).
         */
        private const val ROW_HEIGHT = 36.0

        /** The slack added to MaxDropDownHeight relative to the row height (half a row). */
        private const val ROW_SLACK = 0.5

        /** The outer height (DIP) of the drop-down items: ComboBoxDropdownContentMargin top and bottom 4+4 and ComboBoxDropdownBorderThickness top and bottom 1+1. */
        private const val DROP_DOWN_CHROME_HEIGHT = 10.0
    }
}

/**
 * A list of a component's Swing-style listeners (in the order added). Calls [addNativeHandler] only once when the first listener is added,
 * and keeps the component with [WComponent.listenerAnchor] while there are listeners. Kotlin lambdas are stored wrapped as [L] and remembered so they can be removed by the original lambda.
 */
internal class AnchoredListeners<L : Any>(private val owner: WComponent, private val addNativeHandler: () -> Unit) {
    private val listeners = ArrayList<L>()
    private val adapters = KotlinListenerAdapters<L>()
    private var handlerAdded = false

    fun isEmpty(): Boolean = listeners.isEmpty()

    fun add(listener: L) {
        if (!handlerAdded) {
            addNativeHandler()
            handlerAdded = true
        }
        listeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    fun addAdapter(key: Any, adapter: L) {
        add(adapter)
        adapters.add(key, adapter)
    }

    fun remove(listener: L) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    fun removeAdapter(key: Any) {
        remove(adapters.remove(key) ?: return)
    }

    /** A copy unaffected by adding or removing during calls. */
    fun toList(): List<L> = listeners.toList()
}
