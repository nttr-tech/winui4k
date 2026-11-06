package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.IntConsumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * A breadcrumb trail: WinUI 3's BreadcrumbBar (no Swing equivalent).
 *
 * Set the hierarchy's labels with [setItems], and subscribe to a clicked level (its index is
 * Subscribe with [addActionListener] (ActionListener-style; the actionCommand is the label) or [addItemClickedListener] (receives the index).
 * such as a filer's address bar.
 */
class WBreadcrumbBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_BreadcrumbBar, XamlInterop.IID_IBreadcrumbBarFactory), // default interface = IBreadcrumbBar
) {
    /** Listener → event token (used by the remove function). */
    private val itemClickedTokens = ListenerTokens<IntConsumer>()

    /** The listeners added with [addActionListener] (called in the order they were added). A single ItemClicked handler is registered when the first listener is added. */
    private val actionListeners = ArrayList<WActionListener>()
    private var actionHandlerAdded = false

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
    @JvmSynthetic
    fun addItemClickedListener(listener: (Int) -> Unit) {
        val adapter = IntConsumer { listener(it) }
        addItemClickedListenerForJava(adapter)
        itemClickedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addItemClickedListener")
    fun addItemClickedListenerForJava(listener: IntConsumer) {
        val token = inspectable.addEventHandler(
            "WinUI4K.BreadcrumbBarItemClickedHandler",
            XamlInterop.IID_BreadcrumbBarItemClickedHandler,
            XamlInterop.IBreadcrumbBar_add_ItemClicked,
        ) { _, args ->
            val index = ComPtr(args).getInt(XamlInterop.IBreadcrumbBarItemClickedEventArgs_get_Index)
            listener.accept(index)
        }
        itemClickedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemClickedListener]. */
    @JvmSynthetic
    fun removeItemClickedListener(listener: (Int) -> Unit) {
        val adapter = itemClickedTokens.removeKotlinAdapter(listener) ?: return
        removeItemClickedListenerForJava(adapter)
    }

    @JvmName("removeItemClickedListener")
    fun removeItemClickedListenerForJava(listener: IntConsumer) {
        val token = itemClickedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IBreadcrumbBar_remove_ItemClicked, token)
    }

    /**
     * ActionListener-style: receives a [WActionEvent] every time a level is clicked (BreadcrumbBar.ItemClicked).
     * The source is this breadcrumb bar, and the actionCommand is the label of the clicked level (an element of [items]).
     * When the index is needed, use [addItemClickedListener].
     */
    fun addActionListener(listener: WActionListener) {
        if (!actionHandlerAdded) {
            actionHandlerAdded = true
            // The handler stays on the native side, so do not strongly capture this wrapper; look it up through the anchor (see ListenerAnchor)
            val anchor = listenerAnchor
            inspectable.addEventHandler(
                "WinUI4K.BreadcrumbBarActionHandler",
                XamlInterop.IID_BreadcrumbBarItemClickedHandler,
                XamlInterop.IBreadcrumbBar_add_ItemClicked,
            ) { _, args ->
                val index = ComPtr(args).getInt(XamlInterop.IBreadcrumbBarItemClickedEventArgs_get_Index)
                (anchor.get() as? WBreadcrumbBar)?.fireActionPerformed(index)
            }
        }
        actionListeners += listener
        listenerAnchor.listenerAdded()
    }

    /** Removes one listener added with [addActionListener]. Does nothing if it was not added. */
    fun removeActionListener(listener: WActionListener) {
        val index = actionListeners.indexOfLast { it === listener }
        if (index < 0) return
        actionListeners.removeAt(index)
        listenerAnchor.listenerRemoved()
    }

    /** The ActionListeners currently added (in the order they were added). */
    fun getActionListeners(): Array<WActionListener> = actionListeners.toTypedArray()

    private fun fireActionPerformed(index: Int) {
        if (actionListeners.isEmpty()) return
        val event = WActionEvent(this, items.getOrNull(index), WActionEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()))
        for (listener in actionListeners.toList()) listener.actionPerformed(event)
    }
}
