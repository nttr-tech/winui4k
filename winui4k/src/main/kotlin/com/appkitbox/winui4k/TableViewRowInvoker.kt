package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.IntConsumer

/**
 * Notifies listeners of a row double-click on a [WTable] (UIElement.DoubleTapped), passing the view index of the
 * double-clicked row (the implementation of WTable.addRowInvokedListener).
 *
 * Because TableView itself has no row double-click event, this subscribes to DoubleTapped on the whole table and
 * walks up the parents from the double-clicked element (OriginalSource) to find the row object.
 */
internal class TableViewRowInvoker(
    /** The table's IUIElement (the DoubleTapped subscription target, and where the walk up the parents stops). */
    private val table: ComPtr,
    /** Looks up the row object for a native item (an owned reference) and releases the item. */
    private val itemOf: (ComPtr) -> TableRowItem?,
    /** Converts a model row index to a view row index. */
    private val toView: (Int) -> Int,
) {
    private val listeners = mutableListOf<IntConsumer>()
    private val adapters = KotlinListenerAdapters<IntConsumer>()

    /** Whether DoubleTapped has been subscribed to (subscribed when the first listener is added). */
    private var isSubscribed = false

    fun add(listener: (Int) -> Unit) {
        val adapter = IntConsumer { listener(it) }
        add(adapter)
        adapters.add(listener, adapter)
    }

    fun add(listener: IntConsumer) {
        if (!isSubscribed) {
            // Do not subscribe for tables that do not use double-click
            table.addEventHandler(
                "WinUI4K.TableViewDoubleTappedHandler",
                XamlInterop.IID_DoubleTappedEventHandler,
                XamlInterop.IUIElement_add_DoubleTapped,
            ) { _, args -> onDoubleTapped(ComPtr(args)) }
            isSubscribed = true
        }
        listeners += listener
    }

    fun remove(listener: (Int) -> Unit) {
        val adapter = adapters.remove(listener) ?: return
        remove(adapter)
    }

    fun remove(listener: IntConsumer) {
        listeners -= listener
    }

    /**
     * The view index of the row that the element [element] (a DependencyObject) in the table belongs to.
     * Elements in a row's cells inherit the DataContext (the row object), so this walks up the parents to find an
     * element whose DataContext is a row object. Returns -1 for elements outside the table, in column headers, or in
     * group header rows.
     */
    fun viewRowOf(element: ComPtr): Int {
        val statics = Activation.factory(XamlInterop.CLS_VisualTreeHelper, XamlInterop.IID_IVisualTreeHelperStatics)
        val tableIdentity = identityOf(table)
        var node: ComPtr? = element.also { it.addRef() }
        try {
            while (node != null) {
                val current: ComPtr = node
                if (identityOf(current) == tableIdentity) return -1
                rowItemOf(current)?.let { return toView(it.modelRow) }
                node = statics.getPtrOrNull(XamlInterop.IVisualTreeHelperStatics_GetParent, current.ptr)
                current.release()
            }
            return -1
        } finally {
            node?.release()
            statics.release()
        }
    }

    /** Notifies listeners of the row of the double-clicked element (DoubleTappedRoutedEventArgs.OriginalSource). */
    private fun onDoubleTapped(args: ComPtr) {
        if (listeners.isEmpty()) return
        val routed = args.queryInterface(XamlInterop.IID_IRoutedEventArgs)
        val source = try {
            routed.getPtrOrNull(XamlInterop.IRoutedEventArgs_get_OriginalSource)
        } finally {
            routed.release()
        } ?: return
        val row = try {
            viewRowOf(source)
        } finally {
            source.release()
        }
        if (row < 0) return
        for (listener in listeners.toList()) listener.accept(row)
    }

    /** If [node] is a FrameworkElement whose DataContext is a row object, returns that row; otherwise null. */
    private fun rowItemOf(node: ComPtr): TableRowItem? {
        val element = node.queryInterfaceOrNull(XamlInterop.IID_IFrameworkElement) ?: return null
        val dataContext = try {
            element.getPtrOrNull(XamlInterop.IFrameworkElement_get_DataContext)
        } finally {
            element.release()
        } ?: return null
        return itemOf(dataContext)
    }

    /** The COM identity (the address of QueryInterface(IUnknown)). */
    private fun identityOf(pointer: ComPtr): Long {
        val unknown = pointer.queryInterface(KComObject.IID_IUNKNOWN)
        return try {
            unknown.ptr.address
        } finally {
            unknown.release()
        }
    }
}
