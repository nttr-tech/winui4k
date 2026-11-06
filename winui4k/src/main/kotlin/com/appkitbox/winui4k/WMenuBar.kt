package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference
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

    /** The menu bar it is in (set by WMenuBar's item management). It is from child to parent, so it is held weakly (same as [WMenuFlyoutItemBase]). */
    private var menuOwnerReference: WeakReference<MenuItems<WMenuBarItem>>? = null

    internal var menuOwner: MenuItems<WMenuBarItem>?
        get() = menuOwnerReference?.get()
        set(value) {
            menuOwnerReference = value?.let { WeakReference(it) }
        }

    /** The label shown on the menu bar (MenuBarItem.Title). */
    var title: String
        get() = inspectable.getString(XamlInterop.IMenuBarItem_get_Title)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IMenuBarItem_put_Title, h) }

    init {
        if (title.isNotEmpty()) this.title = title
    }

    /** JMenu(Action)-like: creates a menu with the values of [action] (NAME etc.) copied. */
    constructor(action: WAction) : this("") {
        this.action = action
    }

    /**
     * JMenu.getAction / setAction-like: the [WAction] bound to the menu heading. null (the default) means no binding.
     * As with Swing's JMenu, [WAction.NAME] is copied to [title], [WAction.MNEMONIC_KEY] to [mnemonic], [WAction.isEnabled] to [isEnabled] and
     * [WAction.SHORT_DESCRIPTION] to [toolTipText], following every later change of the action's values. Items without a value are unset
     * (the text is "", the mnemonic 0 and the tooltip null). Switching to another action or null unbinds the previous action.
     * Opening the menu does not call [WAction.actionPerformed] (Swing's JMenu does not either). MenuBarItem has no icon, so
     * [WAction.SMALL_ICON] is not used. [IllegalArgumentException] if a value has the wrong type. The action refers to the menu through a weak reference.
     */
    var action: WAction? = null
        set(value) {
            if (field === value) return
            actionBinder.detach()
            field = value
            actionBinder.attach(value)
        }

    /** The binding to [action] (following its values). The action looks up the menu through a weak reference. */
    private val actionBinder = ActionBinder(
        object : ActionTarget {
            override fun applyActionValue(action: WAction?, key: String) {
                when (key) {
                    WAction.NAME -> this@WMenuBarItem.title = action?.stringValue(key) ?: ""
                    WAction.MNEMONIC_KEY -> mnemonic = action?.keyCodeValue(key) ?: 0
                    WAction.ENABLED -> isEnabled = action?.isEnabled ?: true
                    WAction.SHORT_DESCRIPTION -> toolTipText = action?.stringValue(key)
                }
            }

            override fun addActionClickListener(listener: WActionListener) {
                // JMenu does not call the action when opened
            }

            override fun removeActionClickListener(listener: WActionListener) {
                // Nothing was attached, so there is nothing to remove
            }
        },
        MENU_HEADER_ACTION_KEYS,
    )

    /** JMenu.getText / setText-like: an alias of [title]. */
    var text: String
        get() = title
        set(value) {
            title = value
        }

    /**
     * JMenu.getMnemonic / setMnemonic(int)-like: the key code of the access key (UIElement.AccessKey). The values and constraints are the same as
     * [WMenuFlyoutItem.mnemonic]. Pressing Alt and that key opens the menu (MenuBarItem opens its menu on AccessKeyInvoked).
     */
    var mnemonic: Int
        get() = Mnemonics.read(uiElement)
        set(value) = Mnemonics.write(uiElement, value)

    /** JMenu.setMnemonic(char)-like: sets the access key by character (the same as [WMenuFlyoutItem.setMnemonic]). */
    fun setMnemonic(mnemonic: Char) {
        this.mnemonic = Mnemonics.keyCodeOf(mnemonic)
    }

    /**
     * JMenu.isPopupMenuVisible / setPopupMenuVisible-like: whether the menu is open. true opens it the same as clicking the menu bar heading
     * (does nothing if already open) and false closes it. It does not open before being placed in a window or when there are no items
     * (WinUI's MenuBarItem does not open an empty menu). Opening is asynchronous, and reading returns true only after it starts opening.
     */
    var isPopupMenuVisible: Boolean
        get() = withOpeningMenu { it.getBool(XamlInterop.IFlyoutBase_get_IsOpen) } ?: false
        set(value) {
            if (value == isPopupMenuVisible) return
            if (value) {
                Xaml.invoke(inspectable) // MenuBarItem's Invoke: opens it if closed
            } else {
                withOpeningMenu { it.call(XamlInterop.IFlyoutBase_Hide) }
            }
        }

    /**
     * JMenu.doClick-like: the same as clicking the menu bar heading, opens the menu if closed and closes it if open
     * (UI Automation Invoke). Does nothing if disabled ([isEnabled] is false). Also does nothing before being placed in a window.
     */
    fun doClick() {
        if (!isEnabled) return
        Xaml.invoke(inspectable)
    }

    /** Calls [block] with the menu to open (the ContextFlyout of the template part) if there is one. null if not yet there before being placed. */
    private fun <T> withOpeningMenu(block: (ComPtr) -> T): T? {
        val flyout = MenuBarItemMenuEvents.openingMenuOf(inspectable) ?: return null
        return try {
            block(flyout)
        } finally {
            flyout.release()
        }
    }

    private val menuEvents: MenuBarItemMenuEvents by lazy { MenuBarItemMenuEvents(listenerAnchor) }

    /**
     * JMenu.addMenuListener-like: adds a menu open/close listener. When opening (Opening of the open menu), menuSelected; when closed
     * (Closed), menuDeselected, in the order added. When moving to the adjacent heading in the menu bar, the previous menu's menuDeselected and
     * the next menu's menuSelected are called too. menuCanceled is not called, as with Swing's JMenu ([WMenuListener.menuCanceled]).
     * The open menu is created after the MenuBarItem is put in a window, so a listener added before that is called for opening and closing after it is loaded.
     */
    fun addMenuListener(listener: WMenuListener) {
        menuEvents.add(this, listener)
    }

    /** JMenu.removeMenuListener-like: removes a listener added with [addMenuListener]. Does nothing if not registered. */
    fun removeMenuListener(listener: WMenuListener) {
        menuEvents.remove(listener)
    }

    /** JMenu.getMenuListeners-like: the added listeners (in the order added; a new array on each call). */
    val menuListeners: Array<WMenuListener>
        get() = menuEvents.all

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

/** The keys of the items copied to menu headings ([WMenuBarItem.action] / [WMenuFlyoutSubItem.action]) (the part of JMenu's configurePropertiesFromAction). */
internal val MENU_HEADER_ACTION_KEYS = listOf(WAction.NAME, WAction.SMALL_ICON, WAction.MNEMONIC_KEY, WAction.ENABLED, WAction.SHORT_DESCRIPTION)
