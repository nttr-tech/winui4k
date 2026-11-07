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
        own(panel.getPtr(XamlInterop.IPanel_get_Children)) // IVector<UIElement>
    }

    /** This panel's IPanel (for reading and writing Background). */
    private val panel: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IPanel)) }

    /** Whether Background is the transparent brush put for hit testing, not a color the user set. */
    private var hitTestBrushOnly = false

    /** Whether a mouse listener has ever been added (after that, Background is not set to null; hit testing is kept with a transparent brush). */
    private var wantsHitTest = false

    /**
     * Like JComponent.getBackground / setBackground: the panel's background color (Panel.Background). Converted to a SolidColorBrush when set,
     * and read back as the native brush's color (null if it is not a solid color brush). Null by default (no background). Setting null removes the background.
     *
     * WinUI's Panel receives mouse events only over its children when it has no Background. To receive them over the whole bounds as JPanel does,
     * a transparent brush is put when a mouse listener ([addMouseListener] and so on) is added if there is no background (hit testing is not changed
     * while there are no listeners). That brush is not the user's setting, so this getter stays null. Setting null after adding a listener
     * puts the transparent brush back to keep hit testing (it stays even after the listeners are removed).
     */
    var background: WColor?
        get() {
            if (hitTestBrushOnly) return null
            val brush = panel.getPtrOrNull(XamlInterop.IPanel_get_Background) ?: return null
            return try {
                colorOf(brush)
            } finally {
                brush.release()
            }
        }
        set(value) {
            if (value == null && wantsHitTest) {
                putTransparentBackground()
                return
            }
            hitTestBrushOnly = false
            val brush = value?.createBrush()
            try {
                panel.call(XamlInterop.IPanel_put_Background, brush?.ptr)
            } finally {
                brush?.release()
            }
        }

    override fun mouseListenerAdded() {
        if (wantsHitTest) return
        wantsHitTest = true
        val existing = panel.getPtrOrNull(XamlInterop.IPanel_get_Background)
        if (existing == null) putTransparentBackground() else existing.release()
    }

    /** Puts a transparent brush in Background only for hit testing. */
    private fun putTransparentBackground() {
        val transparent = Xaml.solidBrush(0, 0, 0, 0)
        try {
            panel.call(XamlInterop.IPanel_put_Background, transparent.ptr)
        } finally {
            transparent.release()
        }
        hitTestBrushOnly = true
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
     * Container.add(Component, Object)-like: adds a child with constraints at the end (the form `getContentPane().add(c, WBorderLayout.NORTH)`).
     * The constraints are passed to the layout manager of the [WLayoutPanel]. If the manager rejects the constraints and throws, the child is not added.
     *
     * Difference from Swing: passing a non-null constraint to a container that does not lay out with a layout manager ([WPanel], [WGrid], etc.; anything other than [WLayoutPanel])
     * throws [IllegalArgumentException] instead of silently dropping it (the child is not added). If the constraint is null, the same as [add].
     */
    fun add(component: WComponent, constraints: Any?) {
        add(component, constraints, -1)
    }

    /**
     * Container.add(Component, Object, int)-like: adds a child with constraints at position [index] (-1 for the end).
     * IllegalArgumentException if [index] is out of range. Constraints are handled the same way as add(component, constraints).
     */
    fun add(component: WComponent, constraints: Any?, index: Int) {
        require(constraints == null || acceptsConstraints) {
            "${javaClass.simpleName} does not lay out its children with a layout manager, so the constraint ($constraints) cannot be used. " +
                "Use a WLayoutPanel, or add without a constraint"
        }
        addImpl(component, constraints, index)
    }

    /** Whether it accepts the constraints of add(component, constraints) (true only for [WLayoutPanel]). */
    internal open val acceptsConstraints: Boolean get() = false

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
    private fun nativeIndexOf(component: WComponent): Int? = nativeIndexOf(component.uiElement)

    /** The position of [element] (IUIElement) in the native Children. null if not contained. */
    private fun nativeIndexOf(element: ComPtr): Int? = Ffi.backend.withScope { scope ->
        val memory = Ffi.backend.memory
        val index = scope.allocate(4)
        val found = scope.allocate(1)
        children.call(FoundationInterop.IVector_IndexOf, element.ptr, index, found)
        if (memory.getByte(found, 0) != 0.toByte()) memory.getInt(index, 0) else null
    }

    private fun removeNative(component: WComponent) {
        nativeIndexOf(component)?.let { children.call(FoundationInterop.IVector_RemoveAt, it) }
    }

    /**
     * Inserts a decoration element [element] (IUIElement; the border line of [WLayoutPanel]) that has no child wrapper at the start of the native Children (behind the children).
     * Does nothing if it is already inserted. It is not included in the child list ([components]).
     */
    internal fun insertDecoration(element: ComPtr) {
        if (nativeIndexOf(element) == null) children.call(FoundationInterop.IVector_InsertAt, 0, element.ptr)
    }

    /** Removes [element] inserted with [insertDecoration]. Does nothing if it is not inserted. */
    internal fun removeDecoration(element: ComPtr) {
        nativeIndexOf(element)?.let { children.call(FoundationInterop.IVector_RemoveAt, it) }
    }

    /** Called after [removeAll] empties the native Children (to insert the decoration elements again). */
    internal open fun onNativeChildrenCleared() {
        // Does nothing by default
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
        onNativeChildrenCleared()
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

    /**
     * Container.getComponentAt-like: the child (a direct child added with [add]) at the position ([x], [y], DIP) seen from the top left of this container.
     * Itself where there is no child, and null outside the container ([contains] is false). Even over a template part of a child, that child is returned.
     *
     * Unlike Swing, it does not calculate from the children's sizes but returns the child drawn in front according to WinUI's hit test (VisualTreeHelper.FindElementsInHostCoordinates,
     * also including panels without a background and elements with IsHitTestVisible = false). So a hidden ([isVisible] = false) child is not hit
     * (Swing's getComponentAt returns it), and for overlapping children the one added later (drawn in front in XAML) is returned (in Swing the one added first is in front).
     * A container not placed in a window (before layout) has size 0, so it is null.
     */
    override fun getComponentAt(x: Int, y: Int): WComponent? = ContainerHitTest.componentAt(this, x, y, deepest = false)

    /**
     * Container.findComponentAt-like: the deepest descendant (a child inside a child container) at the position ([x], [y], DIP) seen from the top left of this container.
     * Itself where there is no descendant, and null outside the container. Only [WContainer] children are followed; the inside of components with a single child
     * such as [WBorder.child] or [WScrollPane.content] is not looked at (that [WBorder] etc. is returned). The hit test rules are the same as [getComponentAt]
     * (hidden children are not hit, as in Swing).
     */
    fun findComponentAt(x: Int, y: Int): WComponent? = ContainerHitTest.componentAt(this, x, y, deepest = true)

    /** Container.findComponentAt(Point)-like: the same as [findComponentAt] (x, y). */
    fun findComponentAt(point: WPoint): WComponent? = findComponentAt(point.x, point.y)

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
