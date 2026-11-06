package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName

/**
 * JPopupMenu-like: WinUI 3's MenuFlyout.
 * Setting it to WButton.flyout or WComponent.contextFlyout opens the list of [add]ed menu items.
 * Adding and removing items ([add] / [insert] / [remove] / [getItem] ...) uses the same JMenu-like API as [WMenuBarItem], and JPopupMenu's
 * names as a Container ([componentCount] / [getComponent] / [components] / [getComponentIndex]) can be used too.
 */
class WMenuFlyout : WFlyoutBase(
    Activation.composeDefault(XamlInterop.CLS_MenuFlyout, XamlInterop.IID_IMenuFlyoutFactory),
) {
    private val nativeItems: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IMenuFlyout_get_Items)) // IVector<MenuFlyoutItemBase>
    }

    private val items = MenuItems.forMenuItems { nativeItems }

    /** JMenu.add(JMenuItem)-like: appends an item at the end and returns [item]. An item that is in another menu is first removed from there. */
    fun <T : WMenuFlyoutItemBase> add(item: T): T = items.add(item)

    /** The legacy `void add(WMenuFlyoutItemBase)` (for binary compatibility; not visible from source). */
    @Deprecated("Use add that returns the item", level = DeprecationLevel.HIDDEN)
    @JvmName("add")
    fun addReturningUnit(item: WMenuFlyoutItemBase) {
        items.add(item)
    }

    /** JMenu.add(String)-like: creates a [WMenuFlyoutItem] with [text], appends it at the end, and returns it. */
    fun add(text: String): WMenuFlyoutItem = items.add(WMenuFlyoutItem(text))

    /** JMenu.add(Action)-like: creates a [WMenuFlyoutItem] bound to [action], appends it at the end, and returns it. */
    fun add(action: WAction): WMenuFlyoutItem = items.add(WMenuFlyoutItem(action))

    /** JMenu.addSeparator-like: appends a separator ([WMenuFlyoutSeparator]) at the end. */
    fun addSeparator() {
        items.add(WMenuFlyoutSeparator())
    }

    /** JMenu.insert(JMenuItem, int)-like: inserts at [index] and returns [item]. Negative throws IllegalArgumentException; at or beyond the number of items, at the end. */
    fun <T : WMenuFlyoutItemBase> insert(item: T, index: Int): T = items.insert(item, index)

    /** JMenu.insert(String, int)-like: creates an item with [text], inserts it at [index], and returns it. */
    fun insert(text: String, index: Int): WMenuFlyoutItem = items.insert(WMenuFlyoutItem(text), index)

    /** JMenu.insert(Action, int)-like: creates an item bound to [action], inserts it at [index], and returns it. */
    fun insert(action: WAction, index: Int): WMenuFlyoutItem = items.insert(WMenuFlyoutItem(action), index)

    /** JMenu.insertSeparator-like: inserts a separator at [index]. */
    fun insertSeparator(index: Int) {
        items.insert(WMenuFlyoutSeparator(), index)
    }

    /** JMenu.remove(JMenuItem)-like: removes an item. Does nothing if it is not an item of this menu. */
    fun remove(item: WMenuFlyoutItemBase) {
        items.remove(item)
    }

    /** JMenu.remove(int)-like: removes the item at [index]. Negative or at or beyond the number of items throws IllegalArgumentException (as in Swing). */
    fun remove(index: Int) {
        items.removeAt(index)
    }

    /** JMenu.removeAll-like: removes all items from the end. */
    fun removeAll() {
        items.clear()
    }

    /**
     * JMenu.getItem-like: the item at [index]. null for a separator (as in Swing). It also returns submenus ([WMenuFlyoutSubItem]), so
     * the type is [WMenuFlyoutItemBase]. Negative throws IllegalArgumentException; at or beyond the number of items throws ArrayIndexOutOfBoundsException.
     */
    fun getItem(index: Int): WMenuFlyoutItemBase? = items.getChecked(index).takeUnless { it is WMenuFlyoutSeparator }

    /** JMenu.getItemCount-like: the number of items including separators (same as [menuComponentCount]). */
    val itemCount: Int
        get() = items.size

    /** JMenu.getMenuComponentCount-like: the number of items including separators. */
    val menuComponentCount: Int
        get() = items.size

    /** JMenu.getMenuComponent-like: the item at [index] (separators included). Out of range throws ArrayIndexOutOfBoundsException. */
    fun getMenuComponent(index: Int): WMenuFlyoutItemBase = items[index]

    /** JMenu.getMenuComponents-like: the list of items (in order; a new array on each call). */
    val menuComponents: Array<WMenuFlyoutItemBase>
        get() = items.list.toTypedArray()

    /** Container.getComponentCount-like: the number of items including separators. */
    val componentCount: Int
        get() = items.size

    /** Container.getComponent-like: the item at [index]. Out of range throws ArrayIndexOutOfBoundsException. */
    fun getComponent(index: Int): WMenuFlyoutItemBase = items[index]

    /** Container.getComponents-like: the list of items (in order; a new array on each call). */
    val components: Array<WMenuFlyoutItemBase>
        get() = items.list.toTypedArray()

    /** JPopupMenu.getComponentIndex-like: the position of [item]. -1 if it is not an item of this menu. */
    fun getComponentIndex(item: WMenuFlyoutItemBase?): Int = items.indexOf(item)
}
