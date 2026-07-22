package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JMenuBar-like: WinUI 3's MenuBar.
 * [WMenuBarItem]s added with [add] line up horizontally as top-level menus.
 */
class WMenuBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_MenuBar, XamlInterop.IID_IMenuBarFactory),
) {
    private val items: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IMenuBar_get_Items)) // IVector<MenuBarItem>
    }

    /** Appends a top-level menu (Append onto MenuBar.Items). */
    fun add(item: WMenuBarItem) {
        items.call(FoundationInterop.IVector_Append, item.inspectable.ptr)
    }
}

/**
 * JMenu-like: WinUI 3's MenuBarItem.
 * [title] becomes the label on the menu bar, and clicking it opens the items added with [add].
 */
class WMenuBarItem(title: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_MenuBarItem, XamlInterop.IID_IMenuBarItemFactory),
) {
    private val items: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IMenuBarItem_get_Items)) // IVector<MenuFlyoutItemBase>
    }

    /** The label shown on the menu bar (MenuBarItem.Title). */
    var title: String
        get() = inspectable.getString(XamlInterop.IMenuBarItem_get_Title)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IMenuBarItem_put_Title, h) }

    init {
        if (title.isNotEmpty()) this.title = title
    }

    /** Appends a menu item (Append onto MenuBarItem.Items). */
    fun add(item: WMenuFlyoutItemBase) {
        items.call(FoundationInterop.IVector_Append, item.menuFlyoutItemBase.ptr)
    }
}
