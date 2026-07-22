package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JComboBox-like: WinUI 3's ComboBox (a Selector subclass).
 *
 * Provides drop-down selection of string items:
 * [addItem] / [getItem] / [removeItem] / [removeAllItems] / [itemCount],
 * [selectedIndex] / [selectedItem], [placeholderText] / [header],
 * [isEditable] / [text] / [addTextSubmitListener] (for an editable combo),
 * [addListSelectionListener] / [removeListSelectionListener] (SelectionChanged).
 */
class WComboBox(items: List<String> = emptyList()) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ComboBox, XamlInterop.IID_IComboBoxFactory), // default interface = IComboBox
) {
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
    private val selectionTokens = ListenerTokens<() -> Unit>()

    /** TextSubmitted event tokens registered via addTextSubmitListener. */
    private val textSubmitTokens = ListenerTokens<(String) -> Unit>()

    /** The number of items (Items.Size). */
    val itemCount: Int
        get() = itemVector.getInt(FoundationInterop.IVector_get_Size)

    /** The selected index, or -1 if nothing is selected (Selector.SelectedIndex). */
    var selectedIndex: Int
        get() = selector.getInt(XamlInterop.ISelector_get_SelectedIndex)
        set(value) = selector.call(XamlInterop.ISelector_put_SelectedIndex, value)

    /** The selected item string, or null if nothing is selected (Selector.SelectedItem). */
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

    /** The heading above the combo box (ComboBox.Header). Object-typed, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
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

    /** Whether the drop-down is open (ComboBox.IsDropDownOpen). */
    var isDropDownOpen: Boolean
        get() = inspectable.getBool(XamlInterop.IComboBox_get_IsDropDownOpen)
        set(value) = inspectable.putBool(XamlInterop.IComboBox_put_IsDropDownOpen, value)

    init {
        for (item in items) addItem(item)
    }

    /** Appends an item at the end (Items.Append). The string is boxed before passing. */
    fun addItem(item: String) {
        val boxed = PropertyValues.boxString(item)
        itemVector.call(FoundationInterop.IVector_Append, boxed.ptr)
        boxed.release()
    }

    /** Returns the item string at [index] (Items.GetAt). */
    fun getItem(index: Int): String {
        val boxed = itemVector.getPtr(FoundationInterop.IVector_GetAt, index)
        return try {
            PropertyValues.unboxString(boxed) ?: ""
        } finally {
            boxed.release()
        }
    }

    /** Removes the item at [index] (Items.RemoveAt). */
    fun removeItem(index: Int) {
        itemVector.call(FoundationInterop.IVector_RemoveAt, index)
    }

    /** Removes all items (Items.Clear). */
    fun removeAllItems() {
        itemVector.call(FoundationInterop.IVector_Clear)
    }

    /** ListSelectionListener-like. Subscribes to Selector.SelectionChanged under the hood. */
    fun addListSelectionListener(listener: () -> Unit) {
        val token = selector.addEventHandler(
            "WinUI4K.SelectionChangedHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ISelector_add_SelectionChanged,
        ) { _, _ -> listener() }
        selectionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addListSelectionListener]. */
    fun removeListSelectionListener(listener: () -> Unit) {
        val token = selectionTokens.remove(listener) ?: return
        selector.removeEventHandler(XamlInterop.ISelector_remove_SelectionChanged, token)
    }

    /**
     * Subscribes to text being committed with Enter on an editable combo (ComboBox.TextSubmitted).
     * The listener receives the committed string. Only fires while [isEditable] is true.
     */
    fun addTextSubmitListener(listener: (String) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TextSubmittedHandler",
            XamlInterop.IID_ComboBoxTextSubmittedHandler,
            XamlInterop.IComboBox_add_TextSubmitted,
        ) { _, args ->
            // args is a ComboBoxTextSubmittedEventArgs; read the committed Text and pass it along
            listener(ComPtr(args).getString(XamlInterop.IComboBoxTextSubmittedEventArgs_get_Text))
        }
        textSubmitTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTextSubmitListener]. */
    fun removeTextSubmitListener(listener: (String) -> Unit) {
        val token = textSubmitTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IComboBox_remove_TextSubmitted, token)
    }
}
