package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * Common base for menu items (MenuFlyoutItem / MenuFlyoutSubItem / MenuFlyoutSeparator):
 * WinUI 3's MenuFlyoutItemBase. Can be passed to WMenuFlyout / WMenuBarItem's add.
 */
abstract class WMenuFlyoutItemBase internal constructor(inspectable: ComPtr) :
    WControl(inspectable) {
    /** The MenuFlyoutItemBase view required by IVector<MenuFlyoutItemBase>.Append. */
    internal val menuFlyoutItemBase: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IMenuFlyoutItemBase)
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
        Activation.composeDefault(Abi.CLS_MenuFlyoutItem, Abi.IID_IMenuFlyoutItemFactory),
    ) {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }

    /** The IMenuFlyoutItem view holding text / Click, etc. (also used by the Toggle / Radio subclasses). */
    private val menuFlyoutItem: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IMenuFlyoutItem)
    }

    /** Click event tokens registered via addActionListener (used by removeActionListener). */
    private val clickTokens = ListenerTokens<() -> Unit>()

    /** The menu item's label (MenuFlyoutItem.Text). */
    var text: String
        get() = menuFlyoutItem.getString(Abi.IMenuFlyoutItem_get_Text)
        set(value) = Hstring.use(value) { h -> menuFlyoutItem.call(Abi.IMenuFlyoutItem_put_Text, h) }

    /** The icon shown to the left of the label (MenuFlyoutItem.Icon). Creates and passes a SymbolIcon. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                menuFlyoutItem.call(Abi.IMenuFlyoutItem_put_Icon, null)
                return
            }
            val iconElement = value.createIconElement()
            menuFlyoutItem.call(Abi.IMenuFlyoutItem_put_Icon, iconElement.ptr)
            iconElement.release()
        }

    /** The command run on click (MenuFlyoutItem.Command). */
    var command: WCommandBase? = null
        set(value) {
            field = value
            menuFlyoutItem.call(
                Abi.IMenuFlyoutItem_put_Command,
                value?.commandPtr,
            )
        }

    /** The argument passed when [command] runs (MenuFlyoutItem.CommandParameter). */
    var commandParameter: String? = null
        set(value) {
            field = value
            if (value == null) {
                menuFlyoutItem.call(Abi.IMenuFlyoutItem_put_CommandParameter, null)
            } else {
                val boxed = PropertyValues.boxString(value)
                menuFlyoutItem.call(Abi.IMenuFlyoutItem_put_CommandParameter, boxed.ptr)
                boxed.release()
            }
        }

    /**
     * A shortcut string shown at the right edge of the item
     * (MenuFlyoutItem.KeyboardAcceleratorTextOverride). Display only; it doesn't respond to input.
     */
    var keyboardAcceleratorText: String
        get() = menuFlyoutItem.getString(Abi.IMenuFlyoutItem_get_KeyboardAcceleratorTextOverride)
        set(value) = Hstring.use(value) { h ->
            menuFlyoutItem.call(Abi.IMenuFlyoutItem_put_KeyboardAcceleratorTextOverride, h)
        }

    /** ActionListener-like. Subscribes to MenuFlyoutItem.Click (RoutedEventHandler) under the hood. */
    fun addActionListener(listener: () -> Unit) {
        val token = menuFlyoutItem.addEventHandler(
            "WinUI4K.MenuClickHandler", Abi.IID_RoutedEventHandler, Abi.IMenuFlyoutItem_add_Click,
        ) { _, _ -> listener() }
        clickTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    fun removeActionListener(listener: () -> Unit) {
        val token = clickTokens.remove(listener) ?: return
        menuFlyoutItem.removeEventHandler(Abi.IMenuFlyoutItem_remove_Click, token)
    }
}

/**
 * JCheckBoxMenuItem-like: WinUI 3's ToggleMenuFlyoutItem.
 * [isChecked] toggles on every click, and a checkmark is shown accordingly.
 */
class WToggleMenuFlyoutItem(text: String = "", icon: Symbol? = null) : WMenuFlyoutItem(
    Activation.composeDefault(Abi.CLS_ToggleMenuFlyoutItem, Abi.IID_IToggleMenuFlyoutItemFactory),
) {
    /** The checked state (ToggleMenuFlyoutItem.IsChecked). */
    var isChecked: Boolean
        get() = inspectable.getBool(Abi.IToggleMenuFlyoutItem_get_IsChecked)
        set(value) = inspectable.putBool(Abi.IToggleMenuFlyoutItem_put_IsChecked, value)

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
    Activation.composeDefault(Abi.CLS_RadioMenuFlyoutItem, Abi.IID_IRadioMenuFlyoutItemFactory),
) {
    /** The checked state (RadioMenuFlyoutItem.IsChecked). */
    var isChecked: Boolean
        get() = inspectable.getBool(Abi.IRadioMenuFlyoutItem_get_IsChecked)
        set(value) = inspectable.putBool(Abi.IRadioMenuFlyoutItem_put_IsChecked, value)

    /** The mutually-exclusive group name (RadioMenuFlyoutItem.GroupName). */
    var groupName: String
        get() = inspectable.getString(Abi.IRadioMenuFlyoutItem_get_GroupName)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(Abi.IRadioMenuFlyoutItem_put_GroupName, h)
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
    Activation.activate(Abi.CLS_MenuFlyoutSubItem).queryInterface(Abi.IID_IMenuFlyoutSubItem),
) {
    private val items: ComPtr by lazy {
        inspectable.getPtr(Abi.IMenuFlyoutSubItem_get_Items) // IVector<MenuFlyoutItemBase>
    }

    /** The submenu's label (MenuFlyoutSubItem.Text). */
    var text: String
        get() = inspectable.getString(Abi.IMenuFlyoutSubItem_get_Text)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(Abi.IMenuFlyoutSubItem_put_Text, h)
        }

    /** The icon shown to the left of the label (MenuFlyoutSubItem.Icon). Creates and passes a SymbolIcon. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(Abi.IMenuFlyoutSubItem_put_Icon, null)
                return
            }
            val iconElement = value.createIconElement()
            inspectable.call(Abi.IMenuFlyoutSubItem_put_Icon, iconElement.ptr)
            iconElement.release()
        }

    init {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }

    /** Appends a child item (Append onto MenuFlyoutSubItem.Items). */
    fun add(item: WMenuFlyoutItemBase) {
        items.call(Abi.IVector_Append, item.menuFlyoutItemBase.ptr)
    }
}

/** JSeparator-like: WinUI 3's MenuFlyoutSeparator (a menu divider line). */
class WMenuFlyoutSeparator : WMenuFlyoutItemBase(
    Activation.composeDefault(Abi.CLS_MenuFlyoutSeparator, Abi.IID_IMenuFlyoutSeparatorFactory),
)
