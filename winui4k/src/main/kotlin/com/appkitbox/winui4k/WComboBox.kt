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
 * Provides drop-down selection of string items:
 * [addItem] / [insertItemAt] / [getItemAt] / [removeItemAt] / [removeItem] / [removeAllItems] / [itemCount],
 * [selectedIndex] / [selectedItem] / [setSelectedItem] / [getSelectedObjects], [placeholderText] / [header],
 * [isEditable] / [text] / [addTextSubmitListener] (for an editable combo),
 * [showPopup] / [hidePopup] / [isPopupVisible] / [maximumRowCount] (drop-down).
 *
 * Events can be received in the same form as JComboBox:
 * - [addItemListener]: when the selected item changes, the previous item's DESELECTED arrives, then the new item's SELECTED ([WItemEvent.item] is the item string).
 *   A change is detected by "whether the selected items are equal", so moving to another item with the same string delivers nothing (as in Swing)
 * - [addActionListener]: arrives after the item events ([actionCommand] is `"comboBoxChanged"` by default). [selectedIndex] /
 *   [setSelectedItem] deliver it once even if the selection does not change, as in Swing. Confirming the input with Enter in an editable combo
 *   delivers `"comboBoxEdited"` after the selection change events
 * - [addPopupMenuListener]: opening and closing of the drop-down (ComboBox.DropDownOpened / DropDownClosed)
 *
 * The existing [addListSelectionListener] (a Runnable called on every SelectionChanged) can still be used.
 *
 * Differences from Swing:
 * - Reselecting the current item from the popup delivers no ActionEvent (WinUI reports nothing when the selection does not change)
 * - [popupMenuWillBecomeVisible][WPopupMenuListener.popupMenuWillBecomeVisible] arrives after it starts opening, and
 *   [popupMenuWillBecomeInvisible][WPopupMenuListener.popupMenuWillBecomeInvisible] after it has finished closing
 * - The constructor taking a List and the no-arg constructor still select nothing when items are added
 *   (JComboBox selects the first item. The constructor taking an array selects the first item, as with JComboBox(E[]))
 */
class WComboBox @JvmOverloads constructor(items: List<String> = emptyList()) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ComboBox, XamlInterop.IID_IComboBoxFactory), // default interface = IComboBox
) {
    /** JComboBox(E[] items)-like: creates a combo with [items] as its items and the first item selected, as with JComboBox (unselected if empty). */
    constructor(items: Array<String>) : this(items.asList()) {
        if (items.isNotEmpty()) selectNative(0)
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

    /** The selected item when the drop-down was opened (if it is the same when closed, it is treated as canceled). */
    private var itemWhenOpened: Any? = null

    /**
     * JComboBox.getActionCommand / setActionCommand-like: the [WActionEvent.actionCommand] of [addActionListener] events.
     * The default is `"comboBoxChanged"`, as in JComboBox. An Enter confirmation in an editable combo uses `"comboBoxEdited"` instead of this value.
     */
    var actionCommand: String = COMBO_BOX_CHANGED

    /** The number of items (Items.Size). */
    val itemCount: Int
        get() = itemVector.getInt(FoundationInterop.IVector_get_Size)

    /**
     * The selected index. -1 if nothing is selected (Selector.SelectedIndex).
     * As with JComboBox.setSelectedIndex, values outside -1 to `itemCount - 1` throw [IllegalArgumentException], and the [addActionListener]
     * listeners are called once even if the selection does not change.
     */
    var selectedIndex: Int
        get() = selector.getInt(XamlInterop.ISelector_get_SelectedIndex)
        set(value) {
            require(value in -1 until itemCount) { "setSelectedIndex: $value out of bounds" }
            if (value != selectedIndex || (value == -1 && selectedItem != null)) selectNative(value)
            fireActionEvent(actionCommand)
        }

    /**
     * The selected item string. null if nothing is selected (Selector.SelectedItem). When a string not in the list is confirmed in an editable combo,
     * that string ([selectedIndex] is then -1). Use [setSelectedItem] to change it.
     */
    val selectedItem: String?
        get() {
            val boxed = selector.getPtrOrNull(XamlInterop.ISelector_get_SelectedItem) ?: return null
            return try {
                PropertyValues.unboxString(boxed)
            } finally {
                boxed.release()
            }
        }

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
        set(value) = inspectable.putBool(XamlInterop.IComboBox_put_IsEditable, value)

    /** The typed-in text for an editable combo (ComboBox.Text). */
    var text: String
        get() = inspectable.getString(XamlInterop.IComboBox_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IComboBox_put_Text, h) }

    /** Whether the drop-down is open (ComboBox.IsDropDownOpen). Same as [isPopupVisible]. */
    var isDropDownOpen: Boolean
        get() = inspectable.getBool(XamlInterop.IComboBox_get_IsDropDownOpen)
        set(value) = inspectable.putBool(XamlInterop.IComboBox_put_IsDropDownOpen, value)

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
     * WinUI has no row count setting, so setting it sets ComboBox.MaxDropDownHeight to "rows × 36 DIP item height of the default theme + 10 DIP top and bottom padding"
     * (the item height is estimated from the ComboBoxItem template padding and the 14px body line height, and drifts if the font or style is changed).
     * Until set, it keeps the native default (expanding as far as fits on screen).
     */
    var maximumRowCount: Int = DEFAULT_MAXIMUM_ROW_COUNT
        set(value) {
            field = value
            inspectable.call(XamlInterop.IComboBox_put_MaxDropDownHeight, value * ROW_HEIGHT + DROP_DOWN_CHROME_HEIGHT)
        }

    init {
        addSelectionHandler()
        for (item in items) addItem(item)
    }

    /** Appends an item at the end (Items.Append). The string is boxed before passing. */
    fun addItem(item: String) {
        val boxed = PropertyValues.boxString(item)
        itemVector.call(FoundationInterop.IVector_Append, boxed.ptr)
        boxed.release()
    }

    /** JComboBox.insertItemAt-like: inserts an item at [index] (Items.InsertAt). */
    fun insertItemAt(item: String, index: Int) {
        val boxed = PropertyValues.boxString(item)
        itemVector.call(FoundationInterop.IVector_InsertAt, index, boxed.ptr)
        boxed.release()
    }

    /** Returns the item string at [index] (Items.GetAt). Throws if out of range ([getItemAt] returns null when out of range). */
    fun getItem(index: Int): String {
        val boxed = itemVector.getPtr(FoundationInterop.IVector_GetAt, index)
        return try {
            PropertyValues.unboxString(boxed) ?: ""
        } finally {
            boxed.release()
        }
    }

    /** JComboBox.getItemAt-like: the item string at [index]. null if out of range. */
    fun getItemAt(index: Int): String? = if (index in 0 until itemCount) getItem(index) else null

    /** Removes the item at [index] (Items.RemoveAt). Same as JComboBox's removeItemAt ([removeItemAt] is the Swing way to write it). */
    fun removeItem(index: Int) {
        itemVector.call(FoundationInterop.IVector_RemoveAt, index)
    }

    /** JComboBox.removeItemAt-like: removes the item at [index]. */
    fun removeItemAt(index: Int) {
        removeItem(index)
    }

    /** JComboBox.removeItem(Object)-like: removes the first item equal to [item]. Does nothing if there is none. */
    fun removeItem(item: Any?) {
        val index = indexOfItem(item ?: return)
        if (index >= 0) removeItem(index)
    }

    /** Removes all items (Items.Clear). If an item was selected, DESELECTED and an ActionEvent are delivered, as in JComboBox. */
    fun removeAllItems() {
        itemVector.call(FoundationInterop.IVector_Clear)
    }

    /**
     * JComboBox.setSelectedItem-like: selects the item equal to [item] (deselects if null).
     * In a non-editable combo, values not in the list are ignored (no events either). If editable, a value not in the list is also selected as its `toString()` string
     * ([selectedIndex] is -1. WinUI accepts custom values only after the template is applied, that is, after the combo is in a window).
     * As in Swing, the [addActionListener] listeners are called once even if the selection does not change.
     */
    fun setSelectedItem(item: Any?) {
        val old = selectedItemReminder
        if (old == null || old != item) {
            when {
                item == null -> selectNative(-1)
                else -> {
                    val index = indexOfItem(item)
                    when {
                        index >= 0 -> selectNative(index)
                        !isEditable -> return
                        else -> selectCustom(item.toString())
                    }
                }
            }
        }
        fireActionEvent(actionCommand)
    }

    /** JComboBox.getSelectedObjects-like: an array of only the selected item (empty if unselected). */
    fun getSelectedObjects(): Array<Any> = selectedItem?.let { arrayOf<Any>(it) } ?: emptyArray()

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
     * [WItemEvent.SELECTED]; [WItemEvent.item] is the item string and the source is this combo. Called for both user operations and programmatic settings.
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
     * [popupMenuWillBecomeInvisible][WPopupMenuListener.popupMenuWillBecomeInvisible]. If the selected item when closed equals the one when opened
     * (closed with Esc or a click outside), it is treated as canceled and [popupMenuCanceled][WPopupMenuListener.popupMenuCanceled] arrives first
     * (closing after reselecting the same item is also a cancellation).
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
    private fun indexOfItem(item: Any): Int = (0 until itemCount).firstOrNull { getItem(it) == item } ?: -1

    /**
     * Sets the native selection to [index]. Notifications during that time do not fire ActionEvents (the caller fires them).
     * -1 deselects by setting SelectedItem to null (for a value not in the list of an editable combo, SelectedIndex is already -1, so the index cannot deselect it).
     */
    private fun selectNative(index: Int) {
        val was = selectingItem
        selectingItem = true
        try {
            if (index < 0) {
                selector.call(XamlInterop.ISelector_put_SelectedItem, null)
            } else {
                selector.call(XamlInterop.ISelector_put_SelectedIndex, index)
            }
        } finally {
            selectingItem = was
        }
    }

    /** Selects a string [text] not in the list (editable combos only. ComboBox accepts it as a custom value and SelectedIndex becomes -1). */
    private fun selectCustom(text: String) {
        val was = selectingItem
        selectingItem = true
        val boxed = PropertyValues.boxString(text)
        try {
            selector.call(XamlInterop.ISelector_put_SelectedItem, boxed.ptr)
        } finally {
            boxed.release()
            selectingItem = was
        }
    }

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

    /** The native selection changed (by user operation or programmatically). Reports in the same order as JComboBox.selectedItemChanged. */
    private fun onNativeSelectionChanged() {
        val newItem = selectedItem
        val oldItem = selectedItemReminder
        if (oldItem != newItem) {
            selectedItemReminder = newItem
            if (oldItem != null) itemListeners.fire(WItemEvent(this, WItemEvent.DESELECTED, oldItem))
            if (newItem != null) itemListeners.fire(WItemEvent(this, WItemEvent.SELECTED, newItem))
            if (!selectingItem) fireActionEvent(actionCommand)
        }
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
        itemWhenOpened = selectedItemReminder
        val event = WPopupMenuEvent(this)
        for (listener in popupListeners.toList()) listener.popupMenuWillBecomeVisible(event)
    }

    private fun onDropDownClosed() {
        val event = WPopupMenuEvent(this)
        val canceled = itemWhenOpened == selectedItemReminder
        itemWhenOpened = null
        if (canceled) for (listener in popupListeners.toList()) listener.popupMenuCanceled(event)
        for (listener in popupListeners.toList()) listener.popupMenuWillBecomeInvisible(event)
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
         * The height (DIP) of one ComboBoxItem row in the default theme: the template's LayoutRoot Margin top and bottom 2+2, ComboBoxItemThemePadding top and bottom 5+7
         * (the ComboBoxItem style in controls/dev/ComboBox/ComboBox_themeresources.xaml), and the 14px body line height of 20.
         */
        private const val ROW_HEIGHT = 36.0

        /** The outer height (DIP) of the drop-down items: ComboBoxDropdownContentMargin top and bottom 4+4 and ComboBoxDropdownBorderThickness top and bottom 1+1. */
        private const val DROP_DOWN_CHROME_HEIGHT = 10.0
    }
}

/**
 * A list of a component's Swing-style listeners (in the order added). Calls [addNativeHandler] only once when the first listener is added,
 * and keeps the component with [WComponent.listenerAnchor] while there are listeners. Kotlin lambdas are stored wrapped as [L] and remembered so they can be removed by the original lambda.
 */
private class AnchoredListeners<L : Any>(private val owner: WComponent, private val addNativeHandler: () -> Unit) {
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
