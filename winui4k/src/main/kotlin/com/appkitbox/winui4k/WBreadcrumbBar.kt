package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A breadcrumb trail: WinUI 3's BreadcrumbBar (no Swing equivalent).
 *
 * Set the hierarchy's labels with [setItems], and subscribe to a clicked level (its index is
 * passed) via [addItemClickedListener]. Use it for displaying and navigating a path hierarchy,
 * such as a filer's address bar.
 */
class WBreadcrumbBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_BreadcrumbBar, XamlInterop.IID_IBreadcrumbBarFactory), // default interface = IBreadcrumbBar
) {
    /** Listener → event token (used by the remove function). */
    private val itemClickedTokens = ListenerTokens<(Int) -> Unit>()

    /** The hierarchy's labels currently shown. */
    var items: List<String> = emptyList()
        private set

    /**
     * Sets the hierarchy's labels (assigns to BreadcrumbBar.ItemsSource).
     * The display only refreshes when ItemsSource itself changes, so a brand-new
     * IIterable<Object> is set each time.
     */
    fun setItems(items: List<String>) {
        this.items = items
        val iterable = StringIterable(items)
        inspectable.call(XamlInterop.IBreadcrumbBar_put_ItemsSource, iterable.comObject.primary)
    }

    /**
     * Subscribes to a level being clicked (BreadcrumbBar.ItemClicked).
     * The listener receives the clicked level's index (the subscript into [items]).
     */
    fun addItemClickedListener(listener: (Int) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.BreadcrumbBarItemClickedHandler",
            XamlInterop.IID_BreadcrumbBarItemClickedHandler,
            XamlInterop.IBreadcrumbBar_add_ItemClicked,
        ) { _, args ->
            val index = ComPtr(args).getInt(XamlInterop.IBreadcrumbBarItemClickedEventArgs_get_Index)
            listener(index)
        }
        itemClickedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemClickedListener]. */
    fun removeItemClickedListener(listener: (Int) -> Unit) {
        val token = itemClickedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IBreadcrumbBar_remove_ItemClicked, token)
    }
}
