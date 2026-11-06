package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName

/**
 * JMenuBar-like: WinUI 3's MenuBar.
 * The [WMenuBarItem]s that were [add]ed are laid out horizontally as top-level menus. Menus can be added and removed with [getMenu] / [menuCount] / [remove] / [removeAll]
 * in the same way as Swing's JMenuBar (assuming the native Items are changed only through the WinUI4K API).
 */
class WMenuBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_MenuBar, XamlInterop.IID_IMenuBarFactory),
) {
    private val nativeItems: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IMenuBar_get_Items)) // IVector<MenuBarItem>
    }

    private val menus = MenuItems<WMenuBarItem>(
        { nativeItems },
        { it.inspectable },
        { it.menuOwner },
        { menu, owner -> menu.menuOwner = owner },
    )

    /**
     * JMenuBar.add(JMenu)-like: appends a top-level menu at the end and returns [menu] (Append to MenuBar.Items).
     * A menu that is in another menu bar is first removed from there (as in Swing).
     */
    fun add(menu: WMenuBarItem): WMenuBarItem = menus.add(menu)

    /** The legacy `void add(WMenuBarItem)` (for binary compatibility; not visible from source). */
    @Deprecated("Use add that returns the WMenuBarItem", level = DeprecationLevel.HIDDEN)
    @JvmName("add")
    fun addReturningUnit(menu: WMenuBarItem) {
        menus.add(menu)
    }

    /** JMenuBar.getMenu-like: the menu at [index]. Out of range throws ArrayIndexOutOfBoundsException (as in Swing). */
    fun getMenu(index: Int): WMenuBarItem = menus[index]

    /** JMenuBar.getMenuCount-like: the number of menus. */
    val menuCount: Int
        get() = menus.size

    /** JMenuBar.getComponentIndex-like: the position of [menu]. -1 if it is not a menu of this menu bar. */
    fun getComponentIndex(menu: WMenuBarItem?): Int = menus.indexOf(menu)

    /** Container.remove(Component)-like: removes [menu]. Does nothing if it is not a menu of this menu bar. */
    fun remove(menu: WMenuBarItem) {
        menus.remove(menu)
    }

    /** Container.remove(int)-like: removes the menu at [index]. Out of range throws ArrayIndexOutOfBoundsException (as in Swing). */
    fun remove(index: Int) {
        if (index !in 0 until menus.size) throw ArrayIndexOutOfBoundsException(index)
        menus.removeAt(index)
    }

    /** Container.removeAll-like: removes all menus. */
    fun removeAll() {
        menus.clear()
    }
}

/**
 * JMenu-like: WinUI 3's MenuBarItem.
 * [title] becomes the label on the menu bar, and clicking it opens the [add]ed items. Adding and removing items ([add] / [insert] / [remove] /
 * [getItem] / [itemCount] ...) uses the same names and exceptions as Swing's JMenu (a negative position throws IllegalArgumentException), and is
 * shared with [WMenuFlyoutSubItem] / [WMenuFlyout]. For Swing's JMenu.add(JMenu) (a submenu), add a [WMenuFlyoutSubItem].
 */
class WMenuBarItem @JvmOverloads constructor(title: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_MenuBarItem, XamlInterop.IID_IMenuBarItemFactory),
) {
    private val nativeItems: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IMenuBarItem_get_Items)) // IVector<MenuFlyoutItemBase>
    }

    private val items = MenuItems.forMenuItems { nativeItems }

    /** The menu bar it is in (set by WMenuBar's item management). */
    internal var menuOwner: MenuItems<WMenuBarItem>? = null

    /** The label shown on the menu bar (MenuBarItem.Title). */
    var title: String
        get() = inspectable.getString(XamlInterop.IMenuBarItem_get_Title)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IMenuBarItem_put_Title, h) }

    init {
        if (title.isNotEmpty()) this.title = title
    }

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
}
