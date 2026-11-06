package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * java.awt.Container-like: common base for Panel-derived types (StackPanel / Grid / Canvas / ...).
 * Provides adding and removing child components (Panel.Children = IVector<UIElement>).
 *
 * The children are also kept in a Kotlin list in the same order as the native Children, and [getComponent] / [components] / [componentCount]
 * read that list (because the wrapper cannot be looked up from the native element). Children are only those added with [add]; elements put directly
 * into the native side are not included. Adding a component that has a parent with [add] first removes it from its old parent, as in Swing ([WComponent.parent]).
 */
abstract class WContainer internal constructor(inspectable: ComPtr) : WComponent(inspectable) {
    private val children: ComPtr by lazy {
        val panel = own(inspectable.queryInterface(XamlInterop.IID_IPanel))
        own(panel.getPtr(XamlInterop.IPanel_get_Children)) // IVector<UIElement>
    }

    /** The child wrappers (in the same order as the native Children). */
    private val mutableComponents = ArrayList<WComponent>()

    /** The child wrappers (in the order of the positions they were added at). [WLayoutPanel.layoutChildren] exposes the same list. */
    internal val componentList: List<WComponent> get() = mutableComponents

    /** Container.add(Component)-style: adds a child at the end. */
    open fun add(component: WComponent) {
        addImpl(component, null, -1)
    }

    /**
     * Container.add(Component, int)-style: adds a child at position [index] (-1 is the end).
     * IllegalArgumentException if [index] is less than -1 or greater than the number of children (same as Swing).
     * Java's `add(c, 0)` chooses this one rather than [WLayoutPanel]'s `add(c, Object)` (same as Swing).
     */
    fun add(component: WComponent, index: Int) {
        addImpl(component, null, index)
    }

    /**
     * The common part of adding (Container.addImpl-style). Checks the position, removes the child from its old parent, puts it into the native side and the Kotlin list,
     * and then calls [onComponentAdded] (notifying the layout manager). If that throws, the addition is undone and the exception rethrown.
     */
    internal fun addImpl(component: WComponent, constraints: Any?, index: Int) {
        checkPosition(index)
        checkCanAdopt(component)
        component.removeFromParent()
        checkPosition(index) // For a move within the same container, the child count has dropped by the removed child
        val position = if (index == -1) mutableComponents.size else index
        insertNative(position, component)
        mutableComponents.add(position, component)
        component.setParentLink(this)
        try {
            onComponentAdded(component, constraints)
        } catch (e: RuntimeException) {
            mutableComponents.removeAt(position)
            component.setParentLink(null)
            removeNative(component)
            throw e
        }
        fireContainerEvent(WContainerEvent.COMPONENT_ADDED, component)
    }

    private fun checkPosition(index: Int) {
        require(index >= -1 && index <= mutableComponents.size) {
            "position to insert the child is out of range (index: $index, child count: ${mutableComponents.size})"
        }
    }

    /**
     * Inserts into the native Children at the position corresponding to [position] in the Kotlin list. So that elements put directly into the native side
     * do not shift it, inserts before the native position (IndexOf) of the next child.
     */
    private fun insertNative(position: Int, component: WComponent) {
        val nativeIndex = mutableComponents.getOrNull(position)?.let { nativeIndexOf(it) }
        if (nativeIndex == null) {
            // IVector<UIElement>.Append(UIElement) — the actual IID is a SHA-1 computed
            // FoundationInterop.IID_IVector_UIElement, but since we already hold a pointer of the correct
            // type, we can just call vtbl[13] directly
            children.call(FoundationInterop.IVector_Append, component.uiElement.ptr)
        } else {
            children.call(FoundationInterop.IVector_InsertAt, nativeIndex, component.uiElement.ptr)
        }
    }

    /** The position of [component] in the native Children (IVector<UIElement>.IndexOf), or null if it is not included. */
    private fun nativeIndexOf(component: WComponent): Int? = Ffi.backend.withScope { scope ->
        val memory = Ffi.backend.memory
        val index = scope.allocate(4)
        val found = scope.allocate(1)
        children.call(FoundationInterop.IVector_IndexOf, component.uiElement.ptr, index, found)
        if (memory.getByte(found, 0) != 0.toByte()) memory.getInt(index, 0) else null
    }

    private fun removeNative(component: WComponent) {
        nativeIndexOf(component)?.let { children.call(FoundationInterop.IVector_RemoveAt, it) }
    }

    /** Called after a child is added (to notify the layout manager and so on). Throwing undoes the addition. */
    internal open fun onComponentAdded(component: WComponent, constraints: Any?) {
        // Does nothing by default
    }

    /** Called after a child is removed (to notify the layout manager and so on). */
    internal open fun onComponentRemoved(component: WComponent) {
        // Does nothing by default
    }

    /** Container.remove(Component)-style: removes a child (IVector<UIElement>.IndexOf -> RemoveAt). Does nothing if it is not a child. */
    open fun remove(component: WComponent) {
        val index = mutableComponents.indexOfFirst { it === component }
        if (index >= 0) remove(index)
    }

    /** Container.remove(int)-style: removes the child at [index]. ArrayIndexOutOfBoundsException if out of range (same as Swing). */
    fun remove(index: Int) {
        if (index !in mutableComponents.indices) throw ArrayIndexOutOfBoundsException(index)
        val component = mutableComponents[index]
        removeNative(component)
        mutableComponents.removeAt(index)
        component.setParentLink(null)
        onComponentRemoved(component)
        fireContainerEvent(WContainerEvent.COMPONENT_REMOVED, component)
    }

    /** Container.removeAll-style: removes all children. As in Swing, removes them starting from the last child. */
    open fun removeAll() {
        while (mutableComponents.isNotEmpty()) remove(mutableComponents.size - 1)
        children.call(FoundationInterop.IVector_Clear) // Also removes elements put directly into the native side, as before
    }

    override fun detachChild(child: WComponent) {
        remove(child)
    }

    /** Container.getComponent-style: the [n]th child. ArrayIndexOutOfBoundsException if out of range (same as Swing). */
    fun getComponent(n: Int): WComponent =
        mutableComponents.getOrNull(n) ?: throw ArrayIndexOutOfBoundsException("no child at position: $n")

    /** Container.getComponents-style: the children (in order; a new array on every call). */
    val components: Array<WComponent>
        get() = mutableComponents.toTypedArray()

    /** Container.getComponentCount-style: the number of children. */
    val componentCount: Int
        get() = mutableComponents.size

    /**
     * Container.isAncestorOf-style: whether this container is reached by following [component]'s parents ([WComponent.parent]).
     * False if [component] is this container itself.
     */
    fun isAncestorOf(component: WComponent?): Boolean {
        var ancestor = component?.parent
        while (ancestor != null) {
            if (ancestor === this) return true
            ancestor = ancestor.parent
        }
        return false
    }

    override fun validateTree() {
        mutableComponents.forEach { it.validateTree() }
    }

    /** The listeners for children entering and leaving (in the order they were added). No native handler is used (all children enter and leave through the Kotlin API). */
    private val containerListenerList = ArrayList<WContainerListener>()

    /**
     * Container.addContainerListener-style: adds a listener called when a child is added or removed. Does nothing if null.
     * Adding the same listener twice makes it called twice (same as Swing).
     *
     * Listeners are called synchronously inside [add] / [remove], after the native child list has been changed. [removeAll] calls them once per removed child,
     * starting from the last child, and when a child with a parent is added to another container with [add], the old container's listeners
     * receive componentRemoved first (all the same as Swing).
     */
    fun addContainerListener(listener: WContainerListener?) {
        if (listener != null) containerListenerList += listener
    }

    /** Container.removeContainerListener-style: removes one listener added with [addContainerListener]. Does nothing if it was not added. */
    fun removeContainerListener(listener: WContainerListener?) {
        val index = containerListenerList.indexOfLast { it === listener }
        if (index >= 0) containerListenerList.removeAt(index)
    }

    /** Container.getContainerListeners-style: the listeners for children entering and leaving that are added (in the order they were added). */
    val containerListeners: Array<WContainerListener>
        get() = containerListenerList.toTypedArray()

    private fun fireContainerEvent(id: Int, child: WComponent) {
        if (containerListenerList.isEmpty()) return
        val event = WContainerEvent(this, id, child)
        for (listener in containerListenerList.toList()) {
            if (id == WContainerEvent.COMPONENT_ADDED) listener.componentAdded(event) else listener.componentRemoved(event)
        }
    }
}
