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
import java.util.function.IntConsumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * A single item of [WSelectorBar]: WinUI 3's SelectorBarItem.
 * Provides [text] (the label), [icon] and [isSelected].
 */
class WSelectorBarItem @JvmOverloads constructor(text: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_SelectorBarItem, XamlInterop.IID_ISelectorBarItemFactory), // default interface = ISelectorBarItem
) {
    /** The item's label (SelectorBarItem.Text). */
    var text: String
        get() = inspectable.getString(XamlInterop.ISelectorBarItem_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.ISelectorBarItem_put_Text, h) }

    /** The IItemContainer view that has ItemContainer.IsSelected (the base of SelectorBarItem). */
    private val itemContainer: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IItemContainer)) }

    /**
     * The icon shown to the left of the label (SelectorBarItem.Icon). Creates an IconElement from a [Symbol] / [WImageIcon] / [WFontIcon] and passes it,
     * and returns the icon as set (since a native IconElement cannot be converted back to a WIcon). null means no icon (default).
     * Unsupported implementations throw [IllegalArgumentException].
     */
    var icon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            val iconElement = value?.createIconElement()
            try {
                inspectable.call(XamlInterop.ISelectorBarItem_put_Icon, iconElement?.ptr)
            } finally {
                iconElement?.release()
            }
        }

    /**
     * Whether it is selected (ItemContainer.IsSelected). JToggleButton.isSelected-like.
     * For an item added to a [WSelectorBar], it follows changes of [WSelectorBar.selectedIndex].
     */
    var isSelected: Boolean
        get() = itemContainer.getBool(XamlInterop.IItemContainer_get_IsSelected)
        set(value) = itemContainer.putBool(XamlInterop.IItemContainer_put_IsSelected, value)

    init {
        if (text.isNotEmpty()) this.text = text
    }
}

/**
 * A JToggleButton-group-like control for switching between a small number of options: WinUI 3's SelectorBar.
 *
 * Add items with [addItem] / [insertItem], and handle them with [getItem] / [removeItem]. Handle the selection with [selectedIndex] and
 * [addSelectionListener] ([addChangeListener]).
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
    private val selectionTokens = ListenerTokens<IntConsumer>()

    private val changeListeners: ChangeListeners = ChangeListeners(this) {
        val anchor = listenerAnchor // The handler does not capture this but looks it up from the anchor
        inspectable.addEventHandler(
            "WinUI4K.SelectorBarChangeHandler",
            XamlInterop.IID_SelectorBarSelectionChangedHandler,
            XamlInterop.ISelectorBar_add_SelectionChanged,
        ) { _, _ -> (anchor.get() as? WSelectorBar)?.changeListeners?.fire() }
    }

    /** The number of items. */
    val itemCount: Int
        get() = items.size

    /**
     * The index of the currently selected item, or -1 if none is selected (SelectorBar.SelectedItem).
     * The getter reverse-looks-up the added item by COM identity.
     * Setting -1 clears the selection (SelectedItem = null), and out-of-range values throw [IndexOutOfBoundsException] (same as JList / JTabbedPane).
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
            if (value != -1) checkIndex(value, items.size - 1)
            inspectable.call(XamlInterop.ISelectorBar_put_SelectedItem, items.getOrNull(value)?.inspectable?.ptr)
        }

    /** Appends an item to the end (Items.Append). */
    fun addItem(item: WSelectorBarItem) {
        itemVector.call(FoundationInterop.IVector_Append, item.inspectable.ptr)
        items += item
    }

    /** The item at [index] ([IndexOutOfBoundsException] if out of range). */
    fun getItem(index: Int): WSelectorBarItem {
        checkIndex(index, items.size - 1)
        return items[index]
    }

    /** Inserts [item] at [index] (Items.InsertAt). [IndexOutOfBoundsException] if [index] is outside 0..[itemCount]. */
    fun insertItem(item: WSelectorBarItem, index: Int) {
        checkIndex(index, items.size)
        itemVector.call(FoundationInterop.IVector_InsertAt, index, item.inspectable.ptr)
        items.add(index, item)
    }

    /** Removes the item at [index] (Items.RemoveAt). [IndexOutOfBoundsException] if out of range. */
    fun removeItem(index: Int) {
        checkIndex(index, items.size - 1)
        itemVector.call(FoundationInterop.IVector_RemoveAt, index)
        items.removeAt(index)
    }

    /** Removes [item]. Does nothing if it is not an item of this SelectorBar. */
    fun removeItem(item: WSelectorBarItem) {
        val index = items.indexOfFirst { it === item }
        if (index >= 0) removeItem(index)
    }

    private fun checkIndex(index: Int, max: Int) {
        if (index < 0 || index > max) throw IndexOutOfBoundsException("index: $index, range: 0..$max")
    }

    /**
     * Subscribes to selection changes (SelectorBar.SelectionChanged).
     * The listener receives the selected item's index.
     */
    @JvmSynthetic
    fun addSelectionListener(listener: (Int) -> Unit) {
        val adapter = IntConsumer { listener(it) }
        addSelectionListener(adapter)
        selectionTokens.addKotlinAdapter(listener, adapter)
    }

    fun addSelectionListener(listener: IntConsumer) {
        val anchor = listenerAnchor // The handler does not capture this but looks it up from the anchor (see ListenerAnchor)
        val token = inspectable.addEventHandler(
            "WinUI4K.SelectorBarSelectionChangedHandler",
            XamlInterop.IID_SelectorBarSelectionChangedHandler,
            XamlInterop.ISelectorBar_add_SelectionChanged,
        ) { _, _ -> (anchor.get() as? WSelectorBar)?.let { listener.accept(it.selectedIndex) } }
        selectionTokens.add(listener, token)
        anchor.listenerAdded()
    }

    /** Unsubscribes a listener registered via [addSelectionListener]. */
    @JvmSynthetic
    fun removeSelectionListener(listener: (Int) -> Unit) {
        val adapter = selectionTokens.removeKotlinAdapter(listener) ?: return
        removeSelectionListener(adapter)
    }

    fun removeSelectionListener(listener: IntConsumer) {
        val token = selectionTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ISelectorBar_remove_SelectionChanged, token)
        listenerAnchor.listenerRemoved()
    }

    /**
     * ChangeListener-like: receives a [WChangeEvent] (the source is this SelectorBar) every time the selection changes.
     * Called once per change, both for user operations and programmatic settings. Read the selection with
     * `((WSelectorBar) e.getSource()).getSelectedIndex()`. It has a different name from [addSelectionListener] (which receives the index directly), so
     * Java lambdas are not ambiguous between them.
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** JTabbedPane.getChangeListeners-like: the ChangeListeners currently added (in the order added). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

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
