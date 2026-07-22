package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A single item of [WSelectorBar]: WinUI 3's SelectorBarItem.
 * Provides [text] (the label).
 */
class WSelectorBarItem(text: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_SelectorBarItem, XamlInterop.IID_ISelectorBarItemFactory), // default interface = ISelectorBarItem
) {
    /** The item's label (SelectorBarItem.Text). */
    var text: String
        get() = inspectable.getString(XamlInterop.ISelectorBarItem_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.ISelectorBarItem_put_Text, h) }

    init {
        if (text.isNotEmpty()) this.text = text
    }
}

/**
 * A JToggleButton-group-like control for switching between a small number of options: WinUI 3's SelectorBar.
 *
 * Add items with [addItem]; selection is handled via [selectedIndex] and [addSelectionListener].
 * Use it for switching a view's display mode (details / icons, etc).
 */
class WSelectorBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_SelectorBar, XamlInterop.IID_ISelectorBarFactory), // default interface = ISelectorBar
) {
    /** SelectorBar.Items (IVector<SelectorBarItem>). A strongly-typed pointer, so its slots are called directly without QI. */
    private val itemVector: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.ISelectorBar_get_Items))
    }

    /** The items added via [addItem] (used to reverse-look-up the selected item). */
    private val items = mutableListOf<WSelectorBarItem>()

    /** Listener → event token (used by the remove function). */
    private val selectionTokens = ListenerTokens<(Int) -> Unit>()

    /** The number of items. */
    val itemCount: Int
        get() = items.size

    /**
     * The index of the currently selected item, or -1 if none is selected (SelectorBar.SelectedItem).
     * The getter reverse-looks-up the added item by COM identity.
     */
    var selectedIndex: Int
        get() {
            val selected = inspectable.getPtrOrNull(XamlInterop.ISelectorBar_get_SelectedItem) ?: return -1
            return try {
                indexOfItem(selected)
            } finally {
                selected.release()
            }
        }
        set(value) {
            inspectable.call(XamlInterop.ISelectorBar_put_SelectedItem, items[value].inspectable.ptr)
        }

    /** Appends an item to the end (Items.Append). */
    fun addItem(item: WSelectorBarItem) {
        itemVector.call(FoundationInterop.IVector_Append, item.inspectable.ptr)
        items += item
    }

    /**
     * Subscribes to selection changes (SelectorBar.SelectionChanged).
     * The listener receives the selected item's index.
     */
    fun addSelectionListener(listener: (Int) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.SelectorBarSelectionChangedHandler",
            XamlInterop.IID_SelectorBarSelectionChangedHandler,
            XamlInterop.ISelectorBar_add_SelectionChanged,
        ) { _, _ -> listener(selectedIndex) }
        selectionTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSelectionListener]. */
    fun removeSelectionListener(listener: (Int) -> Unit) {
        val token = selectionTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ISelectorBar_remove_SelectionChanged, token)
    }

    /**
     * Reverse-looks-up an added item's index from a SelectorBarItem pointer.
     * Compares addresses using the COM identity rule (QI'ing IUnknown always returns the same pointer).
     */
    private fun indexOfItem(target: ComPtr): Int {
        val targetUnknown = target.queryInterface(KComObject.IID_IUNKNOWN)
        try {
            return items.indexOfFirst { item ->
                val mine = item.inspectable.queryInterface(KComObject.IID_IUNKNOWN)
                val matched = mine.ptr.address == targetUnknown.ptr.address
                mine.release()
                matched
            }
        } finally {
            targetUnknown.release()
        }
    }
}
