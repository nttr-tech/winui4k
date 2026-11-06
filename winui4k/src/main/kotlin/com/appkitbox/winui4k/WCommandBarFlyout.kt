package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A context menu with a mini toolbar attached: WinUI 3's CommandBarFlyout.
 * Buttons added with [addPrimaryCommand] show in a row; items added with
 * [addSecondaryCommand] show in the menu below them. Set it on WComponent.contextFlyout to use it.
 * The Primary commands can be handled with the same JToolBar-like [add] / [addSeparator] / [remove] / [getComponentAtIndex] as [WCommandBar]
 * (FlyoutBase is not a WComponent, so the [WComponent.parent] of added components does not become this flyout).
 */
class WCommandBarFlyout : WFlyoutBase(
    Activation.composeDefault(XamlInterop.CLS_CommandBarFlyout, XamlInterop.IID_ICommandBarFlyoutFactory),
) {
    private val primaryCommands: ComPtr by lazy {
        queryVector(XamlInterop.ICommandBarFlyout_get_PrimaryCommands)
    }
    private val secondaryCommands: ComPtr by lazy {
        queryVector(XamlInterop.ICommandBarFlyout_get_SecondaryCommands)
    }

    /** QIs an IObservableVector<ICommandBarElement> to an IVector view that has Append. */
    private fun queryVector(getSlot: Int): ComPtr {
        val observable = inspectable.getPtr(getSlot)
        return try {
            own(observable.queryInterface(XamlInterop.IID_IVector_ICommandBarElement))
        } finally {
            observable.release()
        }
    }

    /**
     * Appends a command shown in the row-of-buttons mini toolbar (CommandBarFlyout.PrimaryCommands).
     * [element] must be an ICommandBarElement implementation (WAppBarButton /
     * WAppBarToggleButton / WAppBarSeparator). Otherwise IllegalArgumentException
     * (use [add] to put an arbitrary component).
     */
    fun addPrimaryCommand(element: WControl) {
        items.add(element, -1, wrap = false)
    }

    /** Appends a command shown in the menu below the mini toolbar (SecondaryCommands). */
    fun addSecondaryCommand(element: WControl) {
        appendCommandBarElement(secondaryCommands, element)
    }

    private val items = CommandBarItems(null) { primaryCommands }

    /**
     * JToolBar.add(Action)-like: creates a [WAppBarButton] bound to [action] at the end and returns it. The label, icon and enabled state
     * follow the action by the same rules as [WButtonBase.action], and pressing it calls [WAction.actionPerformed].
     */
    fun add(action: WAction): WAppBarButton {
        val button = WAppBarButton()
        button.action = action
        items.add(button, -1, wrap = true)
        return button
    }

    /**
     * JToolBar.add(Component)-like: appends [component] at the end and returns it. AppBar buttons and separators (ICommandBarElement) are put
     * as is, and other components are wrapped in an internal AppBarElementContainer ([getComponentAtIndex] returns [component] itself).
     * If it is in another parent, it is removed from there first. Itself or an ancestor throws IllegalArgumentException.
     */
    fun add(component: WComponent): WComponent {
        items.add(component, -1, wrap = true)
        return component
    }

    /** JToolBar.add(Component, int)-like: adds at [index] (-1 for the end). Out of range throws IllegalArgumentException. */
    fun add(component: WComponent, index: Int): WComponent {
        items.add(component, index, wrap = true)
        return component
    }

    /** JToolBar.addSeparator-like: appends a separator ([WAppBarSeparator]) at the end and returns it. */
    fun addSeparator(): WAppBarSeparator {
        val separator = WAppBarSeparator()
        items.add(separator, -1, wrap = false)
        return separator
    }

    /** JToolBar.getComponentCount-like: the number of (Primary) children on the mini toolbar. Items from [addSecondaryCommand] are not counted. */
    val componentCount: Int
        get() = items.count

    /** JToolBar.getComponentAtIndex-like: the child at [index]. null if out of range (as in Swing). */
    fun getComponentAtIndex(index: Int): WComponent? = items.componentAt(index)

    /** JToolBar.getComponentIndex-like: the position of [component]. -1 if it is not a child (as in Swing). */
    fun getComponentIndex(component: WComponent?): Int = items.indexOf(component)

    /** Container.getComponents-like: the list of children (in order; a new array on each call). */
    val components: Array<WComponent>
        get() = items.components()

    /** JToolBar.remove(Component)-like: removes a child. Does nothing if it is not a child. */
    fun remove(component: WComponent) {
        items.remove(component)
    }

    /** Container.remove(int)-like: removes the child at [index]. Out of range throws ArrayIndexOutOfBoundsException. */
    fun remove(index: Int) {
        items.removeAt(index)
    }

    /** Container.removeAll-like: removes all children added with [add] / [addPrimaryCommand]. Items from [addSecondaryCommand] remain. */
    fun removeAll() {
        items.removeAll()
    }
}
