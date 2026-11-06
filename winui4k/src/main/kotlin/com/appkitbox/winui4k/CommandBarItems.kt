package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Internal: a container for putting components that are not ICommandBarElement (text boxes etc.) on a CommandBar / CommandBarFlyout
 * (WinUI's AppBarElementContainer; a ContentControl subclass that implements ICommandBarElement).
 * Created only when [WCommandBar.add] receives a [WComponent].
 */
internal class AppBarElementContainer : WControl(
    Activation.composeDefault(
        XamlInterop.CLS_AppBarElementContainer,
        XamlInterop.IID_IAppBarElementContainerFactory,
    ),
) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /** Sets the container's content (ContentControl.Content) to [component]. null empties it. */
    fun hold(component: WComponent?) {
        contentControl.call(XamlInterop.IContentControl_put_Content, component?.uiElement?.ptr)
    }
}

/** Whether [component] is a WControl that can be put as is as an ICommandBarElement. */
internal fun isCommandBarElement(component: WComponent): Boolean {
    if (component !is WControl) return false
    val probe = component.inspectable.queryInterfaceOrNull(XamlInterop.IID_ICommandBarElement) ?: return false
    probe.release()
    return true
}

/**
 * Internal: the shared implementation that treats the PrimaryCommands of a CommandBar / CommandBarFlyout as the child sequence of a JToolBar.
 * Holds the sequence in the order added in [entries], kept in the same order as the native PrimaryCommands (IVector<ICommandBarElement>).
 * If [host] is a WComponent (WCommandBar), the [WComponent.parent] of added components is set to that host.
 * FlyoutBase is not a WComponent, so for WCommandBarFlyout [host] is null and no parent is recorded.
 */
internal class CommandBarItems(private val host: WComponent?, private val vector: () -> ComPtr) {
    /** One child. [component] is the user's component, and [element] is the element in PrimaryCommands (the container when wrapped). */
    private class Entry(val component: WComponent, val element: WControl, val container: AppBarElementContainer?)

    private val entries = ArrayList<Entry>()

    val count: Int get() = entries.size

    fun componentAt(index: Int): WComponent? = entries.getOrNull(index)?.component

    fun indexOf(component: WComponent?): Int = entries.indexOfFirst { it.component === component }

    fun components(): Array<WComponent> = entries.map { it.component }.toTypedArray()

    /**
     * Inserts [component] at [index] (-1 for the end). If [wrap] is false, components that are not ICommandBarElement throw
     * IllegalArgumentException (the contract of WCommandBar.addPrimaryCommand); if true, they are wrapped in a container.
     */
    fun add(component: WComponent, index: Int, wrap: Boolean) {
        val direct = isCommandBarElement(component)
        require(direct || wrap) {
            "A component that is not an ICommandBarElement cannot be passed to addPrimaryCommand (add wraps it in a container): " +
                component.javaClass.simpleName
        }
        checkPosition(index)
        host?.checkCanAdopt(component)
        if (host != null) component.removeFromParent()
        checkPosition(index) // When moving within the same host, the count has decreased by the removed one
        val position = if (index == -1) entries.size else index
        val container = if (direct) null else AppBarElementContainer()
        val element: WControl = container ?: component as WControl
        container?.hold(component)
        try {
            insertNative(position, element)
        } catch (e: RuntimeException) {
            container?.hold(null)
            throw e
        }
        entries.add(position, Entry(component, element, container))
        component.setParentLink(host)
    }

    fun remove(component: WComponent?) {
        val index = indexOf(component)
        if (index >= 0) removeAt(index)
    }

    /** Removes the child at [index]. Out of range throws ArrayIndexOutOfBoundsException (same as Container.remove(int)). */
    fun removeAt(index: Int) {
        if (index !in entries.indices) throw ArrayIndexOutOfBoundsException(index)
        val entry = entries[index]
        vector().call(FoundationInterop.IVector_RemoveAt, index)
        entries.removeAt(index)
        entry.container?.hold(null) // Detach the parent of the content UIElement so that it can be put somewhere else
        if (host != null) entry.component.setParentLink(null)
    }

    /** Removes all children starting from the last one (same as Container.removeAll). */
    fun removeAll() {
        while (entries.isNotEmpty()) removeAt(entries.size - 1)
    }

    private fun checkPosition(index: Int) {
        require(index >= -1 && index <= entries.size) {
            "The position to insert the child is out of range (index: $index, number of children: ${entries.size})"
        }
    }

    private fun insertNative(position: Int, element: WControl) {
        val commandBarElement = element.inspectable.queryInterface(XamlInterop.IID_ICommandBarElement)
        try {
            if (position == entries.size) {
                vector().call(FoundationInterop.IVector_Append, commandBarElement.ptr)
            } else {
                vector().call(FoundationInterop.IVector_InsertAt, position, commandBarElement.ptr)
            }
        } finally {
            commandBarElement.release()
        }
    }
}

/** Checks values usable for [WCommandBar.orientation]. VERTICAL (which JToolBar has but CommandBar does not) etc. throw IllegalArgumentException. */
internal fun requireHorizontalOrientation(orientation: Int) {
    require(orientation == WCommandBar.HORIZONTAL) {
        if (orientation == WCommandBar.VERTICAL) {
            "CommandBar is horizontal only and cannot be made vertical (orientation: $orientation)"
        } else {
            "orientation must be HORIZONTAL (0) or VERTICAL (1) (orientation: $orientation)"
        }
    }
}
