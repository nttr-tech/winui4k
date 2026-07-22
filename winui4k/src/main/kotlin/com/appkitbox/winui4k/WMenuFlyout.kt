package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * JPopupMenu-like: WinUI 3's MenuFlyout.
 * Set it on WButton.flyout or WComponent.contextFlyout to open the list of
 * menu items added with [add].
 */
class WMenuFlyout : WFlyoutBase(
    Activation.composeDefault(Abi.CLS_MenuFlyout, Abi.IID_IMenuFlyoutFactory),
) {
    private val items: ComPtr by lazy {
        inspectable.getPtr(Abi.IMenuFlyout_get_Items) // IVector<MenuFlyoutItemBase>
    }

    /** Appends a menu item (Append onto MenuFlyout.Items). */
    fun add(item: WMenuFlyoutItemBase) {
        items.call(Abi.IVector_Append, item.menuFlyoutItemBase.ptr)
    }

    /** Removes all menu items (IVector.Clear). */
    fun removeAll() {
        items.call(Abi.IVector_Clear)
    }
}
