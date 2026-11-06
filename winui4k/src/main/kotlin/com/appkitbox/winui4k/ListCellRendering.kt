package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Showing the elements of a [WList] / [WListBox] with a [WListCellRenderer] (the implementation of cellRenderer).
 *
 * ListView ([listViewBase] is not null): Items stay strings (type-ahead search and ItemClick's ClickedItem work as before),
 * the ItemTemplate becomes a DataTemplate with only an empty Border, and the content is put on that template's root (ContentTemplateRoot).
 * The renderer is called on ListViewBase.ContainerContentChanging (when an element is assigned to a container, including when it is reused by scrolling),
 * and the result is put into Border.Child and marked Handled (so WinUI does not set Content from the element; SetupContainerContentChangingAfterPrepare
 * in ListViewBase_Partial_ContainerPhase.cpp). Putting a component directly into the container's Content made ListViewItemPresenter show
 * an empty TextBlock instead of the component (measured), so the template's root is used.
 *
 * ListBox: ListBox has no event for when a container is assigned, so Items become one ListBoxItem per element
 * (a ListBoxItem is its own container) carrying the content made by the renderer. Elements are drawn whenever they are added or replaced, so
 * elements not visible are drawn too (no virtualization).
 *
 * Both redraw the elements whose selection changed (AddedItems / RemovedItems of SelectionChanged), and
 * set the container's AutomationProperties.Name to the element's toString() (so that UI Automation can read the text).
 * The native handlers capture only the [ListenerAnchor], not the wrapper. While a renderer is set, a shown list
 * is not collected, the same as when listeners are added.
 */
internal class ListCellRendering<T : WControl>(
    private val owner: T,
    private val items: ListModelItems,
    private val viewport: ListViewport,
    private val selector: ComPtr,
    /** The ListView's IListViewBase (owned by [owner]). null for ListBox. */
    private val listViewBase: ComPtr?,
    /** A function that looks up this object from the wrapper (must not capture the wrapper). */
    private val resolve: (T) -> ListCellRendering<T>,
) {
    private val anchor by lazy { ListenerAnchor(owner) }
    private var selectionToken: Long? = null
    private var containerToken: Long? = null

    /** The renderer that draws elements. If null, the native side shows the strings. */
    var renderer: WListCellRenderer<Any?>? = null
        set(value) {
            val old = field
            field = value
            if (old == null && value != null) start()
            if (old != null && value == null) stop()
            val switched = (old == null) != (value == null)
            when {
                // ListView recreates its items when the template is replaced (drawing happens in ContainerContentChanging)
                listViewBase != null && switched -> putItemTemplate(listViewBase, value != null)
                // ListBox swaps Items between ListBoxItems and strings (the selection stays at its position)
                listViewBase == null && switched -> {
                    items.itemFactory = if (value == null) null else ::createListBoxItem
                    items.rebuildItems()
                }
                value != null -> repaintRealized()
            }
        }

    private fun start() {
        anchor.listenerAdded()
        // Let the handlers capture only the anchor and resolve (capturing this would form a cycle to the wrapper)
        val anchor = anchor
        val resolve = resolve
        selectionToken = selector.addEventHandler(
            "WinUI4K.ListCellRendererSelectionHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ISelector_add_SelectionChanged,
        ) { _, args -> anchor.get()?.let(resolve)?.onSelectionChanged(ComPtr(args)) }
        if (listViewBase != null) {
            containerToken = listViewBase.addEventHandler(
                "WinUI4K.ListCellRendererContainerHandler",
                XamlInterop.IID_ContainerContentChangingHandler,
                XamlInterop.IListViewBase_add_ContainerContentChanging,
            ) { _, args ->
                // It is called during layout, so refuse if the user's renderer tries to wait for a dialog to close (do not freeze)
                ModalLoop.duringLayout { anchor.get()?.let(resolve)?.onContainerContentChanging(ComPtr(args)) }
            }
        }
    }

    private fun stop() {
        selectionToken?.let { selector.removeEventHandler(XamlInterop.ISelector_remove_SelectionChanged, it) }
        containerToken?.let { listViewBase?.removeEventHandler(XamlInterop.IListViewBase_remove_ContainerContentChanging, it) }
        selectionToken = null
        containerToken = null
        anchor.listenerRemoved()
    }

    /** Sets the ListView's ItemTemplate to the Border template that carries the renderer's content (if [rendering] is true) or to the default (null). */
    private fun putItemTemplate(listViewBase: ComPtr, rendering: Boolean) {
        val itemsControl = listViewBase.queryInterface(XamlInterop.IID_IItemsControl)
        try {
            if (rendering) {
                val template = TableViewXaml.createDataTemplate("<Border/>")
                try {
                    itemsControl.call(XamlInterop.IItemsControl_put_ItemTemplate, template.ptr)
                } finally {
                    template.release()
                }
            } else {
                itemsControl.call(XamlInterop.IItemsControl_put_ItemTemplate, null)
            }
        } finally {
            itemsControl.release()
        }
    }

    /** Redraws all items created now (when the renderer is replaced with another one). */
    private fun repaintRealized() {
        val selected = items.selectedIndices().toHashSet()
        for (cell in viewport.realizedCells()) {
            val container = viewport.containerFromIndex(cell.index) ?: continue
            try {
                render(container, cell.index, cell.index in selected)
            } finally {
                container.release()
            }
        }
    }

    /** Draws the element at [index] with the renderer and puts it in [container] (something that QIs to ContentControl; borrowed). */
    private fun render(container: ComPtr, index: Int, isSelected: Boolean) {
        val currentRenderer = renderer ?: return
        val component = currentRenderer.getListCellRendererComponent(owner, items.valueAt(index), index, isSelected, hasFocus(container))
        putContent(container, component.uiElement)
        Xaml.setAutomation(container, XamlInterop.IAutomationPropertiesStatics_SetName, items.textAt(index))
    }

    /**
     * Puts the content [element] (a UIElement; removes it if null) in [container] (borrowed): Border.Child of the template root for ListView,
     * the ListBoxItem's Content for ListBox. For ListView, does nothing if the template root does not exist yet.
     */
    private fun putContent(container: ComPtr, element: ComPtr?) {
        val content = container.queryInterface(XamlInterop.IID_IContentControl)
        try {
            if (listViewBase == null) {
                content.call(XamlInterop.IContentControl_put_Content, element?.ptr)
                return
            }
            val root = content.getPtrOrNull(XamlInterop.IContentControl_get_ContentTemplateRoot) ?: return
            val border = root.queryInterfaceOrNull(XamlInterop.IID_IBorder)
            root.release()
            border?.call(XamlInterop.IBorder_put_Child, element?.ptr)
            border?.release()
        } finally {
            content.release()
        }
    }

    private fun hasFocus(container: ComPtr): Boolean {
        val element = container.queryInterface(XamlInterop.IID_IUIElement)
        return try {
            element.getInt(XamlInterop.IUIElement_get_FocusState) != 0
        } finally {
            element.release()
        }
    }

    /** A ListBoxItem drawing the element at [index] of the ListBox (an owned reference; ListModelItems puts it into Items). */
    private fun createListBoxItem(index: Int): ComPtr {
        val item = Activation.composeDefault(XamlInterop.CLS_ListBoxItem, XamlInterop.IID_IListBoxItemFactory)
        render(item, index, false)
        return item
    }

    private fun onContainerContentChanging(args: ComPtr) { // args is borrowed (the default interface)
        if (renderer == null) return
        val container = args.getPtr(XamlInterop.IContainerContentChangingEventArgs_get_ItemContainer)
        try {
            if (args.getBool(XamlInterop.IContainerContentChangingEventArgs_get_InRecycleQueue)) {
                // Remove the content while waiting for reuse (redrawn when assigned next)
                putContent(container, null)
            } else {
                val index = args.getInt(XamlInterop.IContainerContentChangingEventArgs_get_ItemIndex)
                if (index in 0 until items.size) render(container, index, isSelected(container))
            }
            args.putBool(XamlInterop.IContainerContentChangingEventArgs_put_Handled, true)
        } finally {
            container.release()
        }
    }

    private fun isSelected(container: ComPtr): Boolean {
        val selectorItem = container.queryInterface(XamlInterop.IID_ISelectorItem)
        return try {
            selectorItem.getBool(XamlInterop.ISelectorItem_get_IsSelected)
        } finally {
            selectorItem.release()
        }
    }

    private fun onSelectionChanged(args: ComPtr) { // args is borrowed
        if (renderer == null) return
        val selectionArgs = args.queryInterface(XamlInterop.IID_ISelectionChangedEventArgs)
        val (added, removed) = try {
            indicesOf(selectionArgs, XamlInterop.ISelectionChangedEventArgs_get_AddedItems) to
                indicesOf(selectionArgs, XamlInterop.ISelectionChangedEventArgs_get_RemovedItems)
        } finally {
            selectionArgs.release()
        }
        for ((indices, selected) in listOf(removed to false, added to true)) {
            for (index in indices) {
                val container = viewport.containerFromIndex(index) ?: continue // If not created, it is drawn when assigned
                try {
                    render(container, index, selected)
                } finally {
                    container.release()
                }
            }
        }
    }

    private fun indicesOf(args: ComPtr, slot: Int): IntArray {
        val vector = args.getPtrOrNull(slot) ?: return IntArray(0)
        return try {
            items.indicesOf(vector)
        } finally {
            vector.release()
        }
    }
}
