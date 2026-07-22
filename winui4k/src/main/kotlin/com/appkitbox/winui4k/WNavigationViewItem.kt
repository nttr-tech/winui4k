package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JMenuItem-like: WinUI 3's NavigationViewItem.
 *
 * An item placed in a [WNavigationView]'s menu. Has a label ([text]) and an icon
 * ([icon]); adding child items via [addItem] turns it into a hierarchical menu.
 */
class WNavigationViewItem(text: String = "", icon: Symbol? = null) : WControl(
    Activation.composeDefault(XamlInterop.CLS_NavigationViewItem, XamlInterop.IID_INavigationViewItemFactory), // default interface = INavigationViewItem
) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }
    private val item2: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_INavigationViewItem2))
    }

    /** The IVector<Object> view of NavigationViewItem.MenuItems (child items). */
    private val childVector: ComPtr by lazy {
        val menuItems = own(item2.getPtr(XamlInterop.INavigationViewItem2_get_MenuItems))
        own(menuItems.queryInterface(FoundationInterop.IID_IVector_Object))
    }

    /** Child items added via [addItem] (used to resolve the selected item back). */
    internal val childItems = mutableListOf<WNavigationViewItem>()

    /**
     * The item's label string (ContentControl.Content).
     * Content is Object-typed, so the setter passes a boxed string and the getter unboxes it.
     */
    var text: String
        get() {
            val boxed = contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            contentControl.call(XamlInterop.IContentControl_put_Content, boxed.ptr)
            boxed.release()
        }

    /** The icon shown to the left of the label (NavigationViewItem.Icon). Builds a SymbolIcon and passes it. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(XamlInterop.INavigationViewItem_put_Icon, null)
            } else {
                // Build a SymbolIcon via ISymbolIconFactory.CreateInstanceWithSymbol, then QI it
                // to IconElement, put_Icon's declared type, before passing it along
                val symbolIcon = Activation.factory(XamlInterop.CLS_SymbolIcon, XamlInterop.IID_ISymbolIconFactory)
                    .getPtr(XamlInterop.ISymbolIconFactory_CreateInstanceWithSymbol, value.native)
                val iconElement = symbolIcon.queryInterface(XamlInterop.IID_IIconElement)
                inspectable.call(XamlInterop.INavigationViewItem_put_Icon, iconElement.ptr)
                iconElement.release()
                symbolIcon.release()
            }
        }

    /**
     * Whether clicking selects this item (NavigationViewItem.SelectsOnInvoked).
     * Set to false for a heading item that only toggles its children open/closed.
     */
    var selectsOnInvoked: Boolean
        get() = item2.getBool(XamlInterop.INavigationViewItem2_get_SelectsOnInvoked)
        set(value) = item2.putBool(XamlInterop.INavigationViewItem2_put_SelectsOnInvoked, value)

    /** Whether the child items are expanded (NavigationViewItem.IsExpanded). */
    var isExpanded: Boolean
        get() = item2.getBool(XamlInterop.INavigationViewItem2_get_IsExpanded)
        set(value) = item2.putBool(XamlInterop.INavigationViewItem2_put_IsExpanded, value)

    init {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }

    /** Appends a child item to the end (MenuItems.Append). */
    fun addItem(item: WNavigationViewItem) {
        childVector.call(FoundationInterop.IVector_Append, item.inspectable.ptr)
        childItems += item
    }

    /** This item and all its descendants (used to resolve the selected item back). */
    internal fun selfAndDescendants(): Sequence<WNavigationViewItem> = sequence {
        yield(this@WNavigationViewItem)
        for (child in childItems) yieldAll(child.selfAndDescendants())
    }
}
