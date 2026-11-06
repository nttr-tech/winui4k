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
 * Adding child items with [addItem] makes a hierarchical menu. Subscribe to item clicks with [addActionListener] (ActionListener-style).
 */
class WNavigationViewItem @JvmOverloads constructor(text: String = "", icon: WIcon? = null) : WControl(
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

    /** The listeners added with [addActionListener] (called in the order they were added). */
    private val actionListeners = ArrayList<WActionListener>()

    /**
     * The legacy `WNavigationViewItem(String, Symbol)`. Kept only for binary compatibility (linking already compiled callers) and
     * not visible from source (HIDDEN). Symbol is a WIcon, so in source the same code selects the form that takes a WIcon.
     * It has default arguments so that the synthetic constructor (String, Symbol, int, DefaultConstructorMarker) used by Kotlin callers
     * compiled with omitted arguments is kept too.
     */
    @Deprecated("Use the constructor that takes a WIcon", level = DeprecationLevel.HIDDEN)
    constructor(text: String = "", icon: Symbol? = null) : this(text, icon as WIcon?)

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

    /**
     * The icon shown to the left of the label (NavigationViewItem.Icon). An IconElement is created from [Symbol] / [WImageIcon] / [WFontIcon] and passed.
     * Returns the icon set as is. Null for no icon. Unsupported implementations throw [IllegalArgumentException].
     */
    var icon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            val iconElement = value?.createIconElement()
            try {
                inspectable.call(XamlInterop.INavigationViewItem_put_Icon, iconElement?.ptr)
            } finally {
                iconElement?.release()
            }
        }

    /** The legacy `setIcon(Symbol)` (for binary compatibility; not visible from source). The reason is the same as for the legacy constructor. */
    @Deprecated("Use icon that takes a WIcon", level = DeprecationLevel.HIDDEN)
    @JvmName("setIcon")
    fun setIconForSymbol(icon: Symbol?) {
        this.icon = icon
    }

    /** The legacy `Symbol getIcon()` (for binary compatibility; not visible from source). Null for icons other than Symbol. */
    @Deprecated("Use icon that returns a WIcon", level = DeprecationLevel.HIDDEN)
    @JvmName("getIcon")
    fun getIconAsSymbol(): Symbol? = icon as? Symbol

    /**
     * Like AbstractButton.getActionCommand: the string that goes into [WActionEvent.actionCommand] on a click.
     * While unset (null), returns [text]. Setting null makes it unset again.
     */
    var actionCommand: String? = null
        get() = field ?: text

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

    /**
     * ActionListener-style: receives a [WActionEvent] every time this item is clicked (including activation with the keyboard).
     * The source is this item and the actionCommand is [actionCommand] (by default [text]).
     * Called for NavigationView.ItemInvoked when the invoked item's container (InvokedItemContainer) is this item, so
     * only items placed with [WNavigationView.addItem] / [WNavigationView.addFooterItem] (or as their descendants with [addItem]) are notified.
     * Also called when the selected item is clicked again.
     */
    fun addActionListener(listener: WActionListener) {
        actionListeners += listener
    }

    /** Removes one listener added with [addActionListener]. Does nothing if it was not added. */
    fun removeActionListener(listener: WActionListener) {
        val index = actionListeners.indexOfLast { it === listener }
        if (index >= 0) actionListeners.removeAt(index)
    }

    /** The ActionListeners currently added (in the order they were added). */
    fun getActionListeners(): Array<WActionListener> = actionListeners.toTypedArray()

    /** Notifies the listeners, in the order they were added, that this item was clicked (called from [WNavigationView]'s ItemInvoked). */
    internal fun fireActionPerformed() {
        if (actionListeners.isEmpty()) return
        val event = WActionEvent(this, actionCommand, WActionEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()))
        for (listener in actionListeners.toList()) listener.actionPerformed(event)
    }

    /** This item and all its descendants (used to resolve the selected item back). */
    internal fun selfAndDescendants(): Sequence<WNavigationViewItem> = sequence {
        yield(this@WNavigationViewItem)
        for (child in childItems) yieldAll(child.selfAndDescendants())
    }
}
