package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

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
        fun of(native: Int): ItemsViewSelectionMode = entries.first { it.native == native }
    }
}

/**
 * JList-like (a freeform-layout version): WinUI 3's ItemsView.
 * Set [setItems] to the row of [WItemContainer]s to lay out, and use [layout]'s
 * [WUniformGridLayout] (or similar) to build a card grid.
 * To subscribe to clicks, set [isItemInvokedEnabled] = true and use [addItemInvokedListener].
 */
class WItemsView : WControl(
    Activation.composeDefault(XamlInterop.CLS_ItemsView, XamlInterop.IID_IItemsViewFactory),
) {
    /** ItemInvoked event tokens registered via addItemInvokedListener. */
    private val itemInvokedTokens = ListenerTokens<(Int) -> Unit>()

    /** The items set via setItems. Also used to look up the invoked item's index. */
    private var items: List<WItemContainer> = emptyList()

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

    /**
     * Replaces the row of items shown (assigns to ItemsView.ItemsSource).
     * Each of ItemsView's items must have an ItemContainer as its root element, so items are
     * wrapped in [WItemContainer] rather than passed as bare UIElements.
     */
    fun setItems(items: List<WItemContainer>) {
        this.items = items
        val iterable = UIElementIterable(items.map { it.uiElement })
        inspectable.call(XamlInterop.IItemsView_put_ItemsSource, iterable.comObject.primary)
    }

    /**
     * Subscribes to item clicks (ItemsView.ItemInvoked).
     * The listener receives the index within the list passed to [setItems].
     */
    fun addItemInvokedListener(listener: (Int) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ItemsViewItemInvokedHandler",
            XamlInterop.IID_ItemsViewItemInvokedHandler,
            XamlInterop.IItemsView_add_ItemInvoked,
        ) { _, args ->
            // args is an ItemsViewItemInvokedEventArgs; read InvokedItem (= the WItemContainer passed in)
            val invoked = ComPtr(args).getPtr(XamlInterop.IItemsViewItemInvokedEventArgs_get_InvokedItem)
            val index = try {
                indexOfItem(invoked)
            } finally {
                invoked.release()
            }
            if (index >= 0) listener(index)
        }
        itemInvokedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemInvokedListener]. */
    fun removeItemInvokedListener(listener: (Int) -> Unit) {
        val token = itemInvokedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IItemsView_remove_ItemInvoked, token)
    }

    /**
     * Looks up the index within [items] from InvokedItem's pointer.
     * The same COM object can have different pointer values per interface, so this compares the
     * canonical address obtained by QI-ing to IUnknown.
     */
    private fun indexOfItem(item: ComPtr): Int {
        val identity = item.queryInterface(KComObject.IID_IUNKNOWN)
        val address = identity.ptr.address
        identity.release()
        return items.indexOfFirst { container ->
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
