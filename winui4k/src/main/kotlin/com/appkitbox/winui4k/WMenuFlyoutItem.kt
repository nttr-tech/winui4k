package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Common base for menu items (MenuFlyoutItem / MenuFlyoutSubItem / MenuFlyoutSeparator):
 * WinUI 3's MenuFlyoutItemBase. Can be passed to WMenuFlyout / WMenuBarItem's add.
 */
abstract class WMenuFlyoutItemBase internal constructor(inspectable: ComPtr) :
    WControl(inspectable) {
    /** The MenuFlyoutItemBase view required by IVector<MenuFlyoutItemBase>.Append. */
    internal val menuFlyoutItemBase: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IMenuFlyoutItemBase))
    }
}

/**
 * JMenuItem-like: WinUI 3's MenuFlyoutItem.
 * Provides [text] / [icon] / [command] / [addActionListener] (Click) /
 * [keyboardAcceleratorText] (display only).
 * Register the shortcut that actually fires via WComponent.addKeyboardAccelerator.
 */
open class WMenuFlyoutItem internal constructor(inspectable: ComPtr) :
    WMenuFlyoutItemBase(inspectable) {
    constructor(text: String = "", icon: Symbol? = null) : this(
        Activation.composeDefault(XamlInterop.CLS_MenuFlyoutItem, XamlInterop.IID_IMenuFlyoutItemFactory),
    ) {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }

    /** The IMenuFlyoutItem view holding text / Click, etc. (also used by the Toggle / Radio subclasses). */
    private val menuFlyoutItem: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IMenuFlyoutItem))
    }

    /** Click event tokens registered via addActionListener (used by removeActionListener). */
    private val clickTokens = ListenerTokens<() -> Unit>()

    /** The menu item's label (MenuFlyoutItem.Text). */
    var text: String
        get() = menuFlyoutItem.getString(XamlInterop.IMenuFlyoutItem_get_Text)
        set(value) = Hstring.use(value) { h -> menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_Text, h) }

    /** The icon shown to the left of the label (MenuFlyoutItem.Icon). Creates and passes a SymbolIcon. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_Icon, null)
                return
            }
            val iconElement = value.createIconElement()
            menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_Icon, iconElement.ptr)
            iconElement.release()
        }

    /** The command run on click (MenuFlyoutItem.Command). */
    var command: WCommandBase? = null
        set(value) {
            field = value
            menuFlyoutItem.call(
                XamlInterop.IMenuFlyoutItem_put_Command,
                value?.commandPtr,
            )
        }

    /** The argument passed when [command] runs (MenuFlyoutItem.CommandParameter). */
    var commandParameter: String? = null
        set(value) {
            field = value
            if (value == null) {
                menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_CommandParameter, null)
            } else {
                val boxed = PropertyValues.boxString(value)
                menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_CommandParameter, boxed.ptr)
                boxed.release()
            }
        }

    /**
     * A shortcut string shown at the right edge of the item
     * (MenuFlyoutItem.KeyboardAcceleratorTextOverride). Display only; it doesn't respond to input.
     */
    var keyboardAcceleratorText: String
        get() = menuFlyoutItem.getString(XamlInterop.IMenuFlyoutItem_get_KeyboardAcceleratorTextOverride)
        set(value) = Hstring.use(value) { h ->
            menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_KeyboardAcceleratorTextOverride, h)
        }

    /** ActionListener-like. Subscribes to MenuFlyoutItem.Click (RoutedEventHandler) under the hood. */
    fun addActionListener(listener: () -> Unit) {
        val token = menuFlyoutItem.addEventHandler(
            "WinUI4K.MenuClickHandler", XamlInterop.IID_RoutedEventHandler, XamlInterop.IMenuFlyoutItem_add_Click,
        ) { _, _ -> listener() }
        clickTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    fun removeActionListener(listener: () -> Unit) {
        val token = clickTokens.remove(listener) ?: return
        menuFlyoutItem.removeEventHandler(XamlInterop.IMenuFlyoutItem_remove_Click, token)
    }
}

/**
 * JCheckBoxMenuItem-like: WinUI 3's ToggleMenuFlyoutItem.
 * [isChecked] toggles on every click, and a checkmark is shown accordingly.
 */
class WToggleMenuFlyoutItem(text: String = "", icon: Symbol? = null) : WMenuFlyoutItem(
    Activation.composeDefault(XamlInterop.CLS_ToggleMenuFlyoutItem, XamlInterop.IID_IToggleMenuFlyoutItemFactory),
) {
    /** The checked state (ToggleMenuFlyoutItem.IsChecked). */
    var isChecked: Boolean
        get() = inspectable.getBool(XamlInterop.IToggleMenuFlyoutItem_get_IsChecked)
        set(value) = inspectable.putBool(XamlInterop.IToggleMenuFlyoutItem_put_IsChecked, value)

    init {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }
}

/**
 * JRadioButtonMenuItem-like: WinUI 3's RadioMenuFlyoutItem.
 * Only one item within the same [groupName] can be checked at a time (mutually exclusive).
 */
class WRadioMenuFlyoutItem(text: String = "", groupName: String = "") : WMenuFlyoutItem(
    Activation.composeDefault(XamlInterop.CLS_RadioMenuFlyoutItem, XamlInterop.IID_IRadioMenuFlyoutItemFactory),
) {
    /** The checked state (RadioMenuFlyoutItem.IsChecked). */
    var isChecked: Boolean
        get() = inspectable.getBool(XamlInterop.IRadioMenuFlyoutItem_get_IsChecked)
        set(value) = inspectable.putBool(XamlInterop.IRadioMenuFlyoutItem_put_IsChecked, value)

    /** The mutually-exclusive group name (RadioMenuFlyoutItem.GroupName). */
    var groupName: String
        get() = inspectable.getString(XamlInterop.IRadioMenuFlyoutItem_get_GroupName)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(XamlInterop.IRadioMenuFlyoutItem_put_GroupName, h)
        }

    init {
        if (text.isNotEmpty()) this.text = text
        if (groupName.isNotEmpty()) this.groupName = groupName
    }
}

/**
 * JMenu submenu-like: WinUI 3's MenuFlyoutSubItem.
 * Hovering over it shows the child items added with [add] in a cascade.
 */
class WMenuFlyoutSubItem(text: String = "", icon: Symbol? = null) : WMenuFlyoutItemBase(
    // activatable (default factory), so activate then QI to the default interface
    Activation.activate(XamlInterop.CLS_MenuFlyoutSubItem, XamlInterop.IID_IMenuFlyoutSubItem),
) {
    private val items: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IMenuFlyoutSubItem_get_Items)) // IVector<MenuFlyoutItemBase>
    }

    /** The submenu's label (MenuFlyoutSubItem.Text). */
    var text: String
        get() = inspectable.getString(XamlInterop.IMenuFlyoutSubItem_get_Text)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(XamlInterop.IMenuFlyoutSubItem_put_Text, h)
        }

    /** The icon shown to the left of the label (MenuFlyoutSubItem.Icon). Creates and passes a SymbolIcon. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(XamlInterop.IMenuFlyoutSubItem_put_Icon, null)
                return
            }
            val iconElement = value.createIconElement()
            inspectable.call(XamlInterop.IMenuFlyoutSubItem_put_Icon, iconElement.ptr)
            iconElement.release()
        }

    init {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }

    /** Appends a child item (Append onto MenuFlyoutSubItem.Items). */
    fun add(item: WMenuFlyoutItemBase) {
        items.call(FoundationInterop.IVector_Append, item.menuFlyoutItemBase.ptr)
    }
}

/** JSeparator-like: WinUI 3's MenuFlyoutSeparator (a menu divider line). */
class WMenuFlyoutSeparator : WMenuFlyoutItemBase(
    Activation.composeDefault(XamlInterop.CLS_MenuFlyoutSeparator, XamlInterop.IID_IMenuFlyoutSeparatorFactory),
)
