package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.IntConsumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.ItemsViewSelectionMode (ItemsView's selection mode).
 * Values extracted from the winmd (None=0, Single=1, Multiple=2, Extended=3).
 */
enum class ItemsViewSelectionMode(internal val native: Int) {
    /** No selection. Combine with [WItemsView.isItemInvokedEnabled] if you only need to detect clicks. */
    NONE(0),

    /** Single selection (default). */
    SINGLE(1),

    /** Multiple selection. */
    MULTIPLE(2),

    /** Extended selection via Ctrl / Shift. */
    EXTENDED(3),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): ItemsViewSelectionMode = entries.first { it.native == native }
    }
}

/**
 * JList-like (a freeform-layout version): WinUI 3's ItemsView.
 * Set [setItems] to the row of [WItemContainer]s to lay out, and use [layout]'s
 * [WUniformGridLayout] (or similar) to build a card grid.
 * To subscribe to clicks, set [isItemInvokedEnabled] = true and use [addItemInvokedListener].
 *
 * The selection can be handled in the JList form ([selectedIndex] / [selectedIndices] / [setSelectionInterval] / [addSelectionInterval] /
 * [removeSelectionInterval] / [isSelectedIndex] / [clearSelection] / [selectAll] and [addListSelectionListener]).
 * Row numbers are positions in the list passed to [setItems]. It does not have a [WListSelectionModel] like [WList]
 * (the ItemsView selection is managed by WinUI's SelectionModel, so it is not duplicated in a Swing model.
 * Only the notification types [WListSelectionEvent] and [WListSelectionListener] are shared).
 * If the selection mode ([selectionMode]) is [ItemsViewSelectionMode.SINGLE], only one row can be selected, as with Swing's SINGLE_SELECTION;
 * with [ItemsViewSelectionMode.MULTIPLE] and [ItemsViewSelectionMode.EXTENDED], multiple rows can be selected.
 * While [ItemsViewSelectionMode.NONE], operations that change the selection do nothing.
 */
class WItemsView : WControl(
    Activation.composeDefault(XamlInterop.CLS_ItemsView, XamlInterop.IID_IItemsViewFactory),
) {
    /** ItemInvoked event tokens registered via addItemInvokedListener. */
    private val itemInvokedTokens = ListenerTokens<IntConsumer>()

    /** The items set with setItems. Also used for the reverse lookup in ItemInvoked (the handler captures this, not the wrapper). */
    private val items = InvokedItems()

    /** The addListSelectionListener listeners and the SelectionChanged subscription. Not created until the first listener is added. */
    private var selectionBridge: SelectionBridge? = null

    /** The selection mode (ItemsView.SelectionMode). */
    var selectionMode: ItemsViewSelectionMode
        get() = ItemsViewSelectionMode.of(inspectable.getInt(XamlInterop.IItemsView_get_SelectionMode))
        set(value) = inspectable.call(XamlInterop.IItemsView_put_SelectionMode, value.native)

    /**
     * Whether clicking an item fires ItemInvoked (ItemsView.IsItemInvokedEnabled).
     * Set this to true when using [addItemInvokedListener].
     */
    var isItemInvokedEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.IItemsView_get_IsItemInvokedEnabled)
        set(value) = inspectable.putBool(XamlInterop.IItemsView_put_IsItemInvokedEnabled, value)

    /** How items are laid out (ItemsView.Layout). null restores the default (a single vertical StackLayout). */
    var layout: WUniformGridLayout? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.IItemsView_put_Layout, value?.layout)
        }

    /** The number of items (the number passed to [setItems]). */
    private val itemCount: Int get() = items.containers.size

    /** true if [index] is within the items (0 until the item count). Out-of-range row numbers are ignored, as in Swing's JList. */
    private fun inRange(index: Int): Boolean = index in 0 until itemCount

    /** The set of selected row numbers (reads the native IsSelected of every row). */
    internal fun selectedIndexSet(): Set<Int> {
        val result = LinkedHashSet<Int>()
        for (index in 0 until itemCount) if (nativeIsSelected(index)) result.add(index)
        return result
    }

    private fun nativeIsSelected(index: Int): Boolean = Ffi.backend.withScope { scope ->
        val out = scope.allocate(1, 1)
        inspectable.call(XamlInterop.IItemsView_IsSelected, index, out)
        Ffi.backend.memory.getByte(out, 0).toInt() != 0
    }

    /**
     * Sets the selection to [target]. Native Select / Deselect raise SelectionChanged per row, so
     * the [addListSelectionListener] listeners are notified only once in total.
     */
    private fun applySelection(target: Set<Int>) {
        val current = selectedIndexSet()
        val bridge = selectionBridge
        bridge?.suppress()
        try {
            for (index in current) if (index !in target) inspectable.call(XamlInterop.IItemsView_Deselect, index)
            for (index in target) if (index !in current) inspectable.call(XamlInterop.IItemsView_Select, index)
        } finally {
            bridge?.resume(this)
        }
    }

    /** Whether operations that change the selection are possible. false in [ItemsViewSelectionMode.NONE]. */
    private val isSelectable: Boolean get() = selectionMode != ItemsViewSelectionMode.NONE

    /**
     * Preparation for operations taking two row numbers, such as [setSelectionInterval]. null (do nothing) if either is -1.
     * Out-of-range parts are excluded (as in Swing's JList, it affects only rows in range), and null if nothing remains.
     */
    private fun interval(index0: Int, index1: Int): IntRange? {
        if (index0 == -1 || index1 == -1) return null
        val range = maxOf(minOf(index0, index1), 0)..minOf(maxOf(index0, index1), itemCount - 1)
        return range.takeUnless { it.isEmpty() }
    }

    /** The smallest selected row number. -1 if nothing is selected (JList.getMinSelectionIndex). */
    val minSelectionIndex: Int get() = selectedIndexSet().minOrNull() ?: -1

    /** The largest selected row number. -1 if nothing is selected (JList.getMaxSelectionIndex). */
    val maxSelectionIndex: Int get() = selectedIndexSet().maxOrNull() ?: -1

    /** true if nothing is selected (JList.isSelectionEmpty). */
    val isSelectionEmpty: Boolean get() = minSelectionIndex == -1

    /**
     * The smallest selected row number. -1 if nothing is selected (JList.getSelectedIndex).
     * Assigning selects only that one row (-1 clears the selection). Other out-of-range row numbers are ignored, as in Swing's JList.
     */
    var selectedIndex: Int
        get() = minSelectionIndex
        set(value) {
            when {
                value == -1 -> clearSelection()
                inRange(value) && isSelectable -> applySelection(setOf(value))
            }
        }

    /**
     * The selected row numbers (ascending; JList.getSelectedIndices). Assigning selects only the given rows
     * (only the last one if [selectionMode] is SINGLE). Out-of-range row numbers are ignored, as in Swing's JList.setSelectedIndices.
     */
    var selectedIndices: IntArray
        get() = selectedIndexSet().toIntArray()
        set(value) {
            if (!isSelectable) return
            val valid = value.filter(::inRange)
            val target = if (selectionMode == ItemsViewSelectionMode.SINGLE) valid.takeLast(1).toSet() else valid.toSet()
            applySelection(target)
        }

    /** true if row [index] is selected. false for out-of-range rows (JList.isSelectedIndex). */
    fun isSelectedIndex(index: Int): Boolean = index in 0 until itemCount && nativeIsSelected(index)

    /**
     * Makes the selection only [index0]..[index1] (either may be larger) (JList.setSelectionInterval).
     * Does nothing if either is -1. Only the one row [index1] if [selectionMode] is SINGLE. Out-of-range rows are ignored
     * (does nothing in SINGLE if [index1] is out of range).
     */
    fun setSelectionInterval(index0: Int, index1: Int) {
        val range = interval(index0, index1) ?: return
        if (!isSelectable) return
        if (selectionMode != ItemsViewSelectionMode.SINGLE) {
            applySelection(range.toSet())
        } else if (inRange(index1)) {
            applySelection(setOf(index1))
        }
    }

    /**
     * Adds [index0]..[index1] to the selection (JList.addSelectionInterval). Same as [setSelectionInterval] if [selectionMode] is SINGLE.
     * Does nothing if either is -1. Out-of-range rows are ignored.
     */
    fun addSelectionInterval(index0: Int, index1: Int) {
        if (selectionMode == ItemsViewSelectionMode.SINGLE) return setSelectionInterval(index0, index1)
        val range = interval(index0, index1) ?: return
        if (!isSelectable) return
        applySelection(selectedIndexSet() + range)
    }

    /**
     * Removes [index0]..[index1] from the selection (JList.removeSelectionInterval).
     * Does nothing if either is -1. Out-of-range rows are ignored.
     */
    fun removeSelectionInterval(index0: Int, index1: Int) {
        val range = interval(index0, index1) ?: return
        applySelection(selectedIndexSet() - range.toSet())
    }

    /** Clears the whole selection (JList.clearSelection). */
    fun clearSelection() = applySelection(emptySet())

    /**
     * Selects all rows. Effective only when [selectionMode] is MULTIPLE / EXTENDED, and does nothing for SINGLE / NONE
     * (Swing's JList has no equivalent operation either).
     */
    fun selectAll() {
        if (selectionMode == ItemsViewSelectionMode.MULTIPLE || selectionMode == ItemsViewSelectionMode.EXTENDED) {
            applySelection((0 until itemCount).toSet())
        }
    }

    /**
     * Scrolls until row [index] is visible (JList.ensureIndexIsVisible; ItemsView.StartBringItemIntoView).
     * Does nothing for an out-of-range row number, as in Swing's JList. Before it is in the visual tree (before the template is applied), WinUI
     * returns a failure, so it throws.
     */
    fun ensureIndexIsVisible(index: Int) {
        if (!inRange(index)) return
        inspectable.call(XamlInterop.IItemsView_StartBringItemIntoView, index, null)
    }

    /**
     * Subscribes to selection changes (JList.addListSelectionListener; ItemsView.SelectionChanged).
     * The event's [WListSelectionEvent.source] is this view. [WListSelectionEvent.firstIndex] / [WListSelectionEvent.lastIndex]
     * are the smallest and largest rows whose selection state changed, found by comparing the selection before and after. The native side does not pass which rows changed,
     * so every row is read on each notification (a view with many rows pays that cost per notification).
     * [WListSelectionEvent.valueIsAdjusting] is always false (WinUI has no tentative selection during dragging).
     * Calls of this API ([setSelectionInterval] etc.) are collapsed into one notification. Listeners are called in the order added.
     */
    fun addListSelectionListener(listener: WListSelectionListener) {
        val bridge = selectionBridge ?: SelectionBridge(ListenerAnchor(this)).also { selectionBridge = it }
        bridge.add(listener, this)
    }

    /** Removes a listener added with [addListSelectionListener] (the last one if the same listener was added several times). */
    fun removeListSelectionListener(listener: WListSelectionListener) {
        selectionBridge?.remove(listener, this)
    }

    /**
     * Replaces the row of items shown (assigns to ItemsView.ItemsSource).
     * Each of ItemsView's items must have an ItemContainer as its root element, so items are
     * wrapped in [WItemContainer] rather than passed as bare UIElements.
     */
    fun setItems(items: List<WItemContainer>) {
        val bridge = selectionBridge
        // Notify the selection changed by the replacement only once, and make the new selection the baseline so that the next notification does not compare with the old items' selection
        bridge?.suppress()
        try {
            this.items.containers = items
            val iterable = UIElementIterable(items.map { it.uiElement })
            inspectable.call(XamlInterop.IItemsView_put_ItemsSource, iterable.comObject.primary)
        } finally {
            bridge?.resume(this)
        }
    }

    /**
     * Subscribes to item clicks (ItemsView.ItemInvoked).
     * The listener receives the index within the list passed to [setItems].
     */
    @JvmSynthetic
    fun addItemInvokedListener(listener: (Int) -> Unit) {
        val adapter = IntConsumer { listener(it) }
        addItemInvokedListener(adapter)
        itemInvokedTokens.addKotlinAdapter(listener, adapter)
    }

    fun addItemInvokedListener(listener: IntConsumer) {
        val items = items // Let the handler capture only the item list, not the wrapper (no cycle)
        val token = inspectable.addEventHandler(
            "WinUI4K.ItemsViewItemInvokedHandler",
            XamlInterop.IID_ItemsViewItemInvokedHandler,
            XamlInterop.IItemsView_add_ItemInvoked,
        ) { _, args ->
            // args is an ItemsViewItemInvokedEventArgs; read InvokedItem (= the WItemContainer passed in)
            val invoked = ComPtr(args).getPtr(XamlInterop.IItemsViewItemInvokedEventArgs_get_InvokedItem)
            val index = try {
                items.indexOf(invoked)
            } finally {
                invoked.release()
            }
            if (index >= 0) listener.accept(index)
        }
        itemInvokedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemInvokedListener]. */
    @JvmSynthetic
    fun removeItemInvokedListener(listener: (Int) -> Unit) {
        val adapter = itemInvokedTokens.removeKotlinAdapter(listener) ?: return
        removeItemInvokedListener(adapter)
    }

    fun removeItemInvokedListener(listener: IntConsumer) {
        val token = itemInvokedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IItemsView_remove_ItemInvoked, token)
    }
}

/**
 * The [WItemsView.addListSelectionListener] listeners and the SelectionChanged subscription (the native handler captures only this object,
 * and looks up the view weakly through [anchor]; see ListenerAnchor).
 */
private class SelectionBridge(private val anchor: ListenerAnchor<WItemsView>) {
    private val listeners = ArrayList<WListSelectionListener>()
    private var token: Long? = null

    /** The selection at the time of the last notification (or when the subscription started). */
    private var previous: Set<Int> = emptySet()

    /** While greater than 0, native notifications are ignored ([resume] notifies once for all of them). */
    private var suppressed = 0

    fun add(listener: WListSelectionListener, view: WItemsView) {
        if (token == null) {
            previous = view.selectedIndexSet()
            token = view.inspectable.addEventHandler(
                "WinUI4K.ItemsViewSelectionChangedHandler",
                XamlInterop.IID_ItemsViewSelectionChangedHandler,
                XamlInterop.IItemsView_add_SelectionChanged,
            ) { _, _ -> if (suppressed == 0) anchor.get()?.let { notifyChanged(it) } }
        }
        listeners.add(listener)
        anchor.listenerAdded()
    }

    fun remove(listener: WListSelectionListener, view: WItemsView) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        anchor.listenerRemoved()
        if (listeners.isEmpty()) {
            token?.let { view.inspectable.removeEventHandler(XamlInterop.IItemsView_remove_SelectionChanged, it) }
            token = null
        }
    }

    fun suppress() {
        suppressed++
    }

    fun resume(view: WItemsView) {
        if (--suppressed == 0) notifyChanged(view)
    }

    /** Re-reads the selection, and if it differs from the previous one, notifies once with the range of differing rows. */
    private fun notifyChanged(view: WItemsView) {
        val current = view.selectedIndexSet()
        val changed = (current - previous) + (previous - current)
        previous = current
        if (changed.isEmpty()) return
        val event = WListSelectionEvent(view, changed.min(), changed.max(), false)
        for (listener in ArrayList(listeners)) listener.valueChanged(event)
    }
}

/** The items passed to [WItemsView.setItems]. Held separately so the ItemInvoked handler can look them up without going through the wrapper. */
private class InvokedItems {
    var containers: List<WItemContainer> = emptyList()

    /**
     * Looks up the index in [containers] from an InvokedItem pointer.
     * The same COM object can have different pointer values per interface, so this compares the
     * canonical address obtained by QI-ing to IUnknown.
     */
    fun indexOf(item: ComPtr): Int {
        val identity = item.queryInterface(KComObject.IID_IUNKNOWN)
        val address = identity.ptr.address
        identity.release()
        return containers.indexOfFirst { container ->
            val containerIdentity = container.uiElement.queryInterface(KComObject.IID_IUNKNOWN)
            val containerAddress = containerIdentity.ptr.address
            containerIdentity.release()
            containerAddress == address
        }
    }
}

/**
 * A Kotlin implementation that exposes a List<ComPtr> (UIElement) as an IIterable<Object>.
 * Used to pass the value to ItemsView.ItemsSource (the same structure as WAutoSuggestBox's StringIterable).
 */
private class UIElementIterable(private val elements: List<ComPtr>) {
    /** The COM object passed to the XAML side as an IIterable<Object>. */
    val comObject: KComObject = KComObject("WinUI4K.UIElementIterable")
        .addInterface(
            FoundationInterop.IID_IIterable_Object,
            listOf(
                // vtbl[6] First(this, out IIterator<Object>)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    // Passes the freshly created reference (count 1) straight into the out param;
                    // it's reclaimed by the caller's Release
                    Ffi.backend.memory.putPtr(args[1] as Ptr, 0, createIterator().primary)
                    KComObject.S_OK
                },
            ),
        )

    /** Writes [element] to the out param as a reference the caller must Release. */
    private fun writeElement(out: Ptr, offset: Long, element: ComPtr) {
        element.addRef()
        Ffi.backend.memory.putPtr(out, offset, element.ptr)
    }

    /** Builds an IIterator<Object> implementation. Each call to First returns an independent cursor. */
    private fun createIterator(): KComObject {
        var index = 0
        return KComObject("WinUI4K.UIElementIterator").addInterface(
            FoundationInterop.IID_IIterator_Object,
            listOf(
                // vtbl[6] get_Current(this, out IInspectable)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    if (index >= elements.size) return@Method E_BOUNDS
                    writeElement(args[1] as Ptr, 0, elements[index])
                    KComObject.S_OK
                },
                // vtbl[7] get_HasCurrent(this, out boolean)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < elements.size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[8] MoveNext(this, out boolean)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    index++
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, if (index < elements.size) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] GetMany(this, UINT32 capacity, IInspectable* items, out UINT32 actual)
                KComObject.Method(DESC_GET_MANY) { args ->
                    val capacity = args[1] as Int
                    val out = args[2] as Ptr
                    var written = 0
                    while (written < capacity && index < elements.size) {
                        writeElement(out, written.toLong() * 8, elements[index])
                        written++
                        index++
                    }
                    Ffi.backend.memory.putInt(args[3] as Ptr, 0, written)
                    KComObject.S_OK
                },
            ),
        )
    }

    private companion object {
        val DESC_THIS_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
        val DESC_GET_MANY = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I32, ArgKind.PTR, ArgKind.PTR)
        val E_BOUNDS = 0x8000000B.toInt()
    }
}
