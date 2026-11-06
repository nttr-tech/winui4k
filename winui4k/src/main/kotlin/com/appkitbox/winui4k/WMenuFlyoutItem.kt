package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
 * Provides [text] / [icon] / [addActionListener] (Click) / [actionCommand] / [mnemonic] (access key) / [accelerator]
 * (keyboard shortcut) / [doClick] / [action] under the same names as Swing's JMenuItem.
 * WinUI's [command] / [commandParameter] / [keyboardAcceleratorText] (overrides the displayed shortcut) are also available.
 */
open class WMenuFlyoutItem internal constructor(inspectable: ComPtr) :
    WMenuFlyoutItemBase(inspectable) {
    /** JMenuItem(String, Icon)-like: an item with text and an icon. Text only if [icon] is null. */
    @JvmOverloads
    constructor(text: String = "", icon: WIcon? = null) : this(
        Activation.composeDefault(XamlInterop.CLS_MenuFlyoutItem, XamlInterop.IID_IMenuFlyoutItemFactory),
    ) {
        initLabel(text, icon)
    }

    /**
     * The legacy `WMenuFlyoutItem(String, Symbol)`. It is kept only for binary compatibility (linking of compiled callers) and is
     * not visible from source (HIDDEN). Symbol is a WIcon, so in source the same code selects the form that takes a WIcon
     * (Java's `new WMenuFlyoutItem("x", null)` also resolves to the WIcon form). It has default arguments so that the synthetic
     * constructor (String, Symbol, int, DefaultConstructorMarker) used by Kotlin callers compiled with omitted arguments is kept too.
     */
    @Deprecated("Use the constructor that takes a WIcon", level = DeprecationLevel.HIDDEN)
    constructor(text: String = "", icon: Symbol? = null) : this(text, icon as WIcon?)

    /** JMenuItem(Icon)-like: an item with only an icon. */
    constructor(icon: WIcon) : this("", icon)

    /** JMenuItem(String, int)-like: an item with text and a mnemonic ([mnemonic]; a key code such as AWT's KeyEvent.VK_A). */
    constructor(text: String, mnemonic: Int) : this(text) {
        this.mnemonic = mnemonic
    }

    /** JMenuItem(Action)-like: an item that copies the values of [action] and calls the action on click. */
    constructor(action: WAction) : this("") {
        this.action = action
    }

    /** The IMenuFlyoutItem view holding text / Click, etc. (also used by the Toggle / Radio subclasses). */
    private val menuFlyoutItem: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IMenuFlyoutItem))
    }

    /** Sets the text and icon from a subclass constructor (an empty text and null keep the defaults). */
    internal fun initLabel(text: String, icon: WIcon?) {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }

    /** The Click subscriptions registered with addActionListener. */
    private val clickListeners: ClickListeners by lazy {
        ClickListeners(menuFlyoutItem, MENU_ITEM_CLICK, listenerAnchor) { (it as WMenuFlyoutItem).actionCommand }
    }

    /** The menu item's label (MenuFlyoutItem.Text). */
    var text: String
        get() = menuFlyoutItem.getString(XamlInterop.IMenuFlyoutItem_get_Text)
        set(value) = Hstring.use(value) { h -> menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_Text, h) }

    /**
     * AbstractButton.getIcon / setIcon-like: the icon shown to the left of the label (MenuFlyoutItem.Icon). Returns the icon as set.
     * null (default) means no icon. Creates an IconElement from a [Symbol] / [WImageIcon] / [WFontIcon] and passes it.
     */
    var icon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            val iconElement = value?.createIconElement()
            try {
                menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_Icon, iconElement?.ptr)
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
            putCommandParameter(menuFlyoutItem, XamlInterop.IMenuFlyoutItem_put_CommandParameter, value)
        }

    /**
     * The shortcut text shown at the right edge of the item (MenuFlyoutItem.KeyboardAcceleratorTextOverride). Display only; it does not react to input.
     * If empty (default), WinUI builds and shows a string such as "Ctrl+S" from [accelerator] (the first keyboard accelerator).
     */
    var keyboardAcceleratorText: String
        get() = menuFlyoutItem.getString(XamlInterop.IMenuFlyoutItem_get_KeyboardAcceleratorTextOverride)
        set(value) = Hstring.use(value) { h ->
            menuFlyoutItem.call(XamlInterop.IMenuFlyoutItem_put_KeyboardAcceleratorTextOverride, h)
        }

    /**
     * AbstractButton.getActionCommand-like: the string put into [WActionEvent.actionCommand] on click.
     * Returns [text] while unset (null). Setting null makes it unset again.
     */
    var actionCommand: String? = null
        get() = field ?: text

    /**
     * AbstractButton.getMnemonic / setMnemonic(int)-like: the key code of the access key (UIElement.AccessKey).
     * Uses the same values as AWT's KeyEvent.VK_A..VK_Z (65..90) / VK_0..VK_9 (48..57); leaving it unset or setting 0 clears it (reads as 0).
     * Anything other than letters and digits throws [IllegalArgumentException]. Pressing that key while the menu is open selects the item
     * (unlike Swing, the character in the label is not underlined; key tips are shown when Alt is pressed).
     */
    var mnemonic: Int
        get() = Mnemonics.read(uiElement)
        set(value) = Mnemonics.write(uiElement, value)

    /**
     * AbstractButton.setMnemonic(char)-like: sets the access key by character. Letters are stored in upper case, and
     * '\u0000' clears it. Anything other than letters and digits throws [IllegalArgumentException].
     */
    fun setMnemonic(mnemonic: Char) {
        this.mnemonic = Mnemonics.keyCodeOf(mnemonic)
    }

    /**
     * JMenuItem.getAccelerator / setAccelerator-like: a keyboard shortcut that selects the item without opening the menu. null (default) means none.
     * Puts exactly one into the item's KeyboardAccelerators and removes the previous one when replaced (null only removes it).
     * Accelerators added separately with [WComponent.addKeyboardAccelerator] are kept. Pressing it raises Click and calls the [addActionListener]
     * listeners and [action] (the menu may stay closed; it starts working once the menu bar or the ContextFlyout owner is put in a window.
     * In WinUI it may keep working even after the owner is removed from the window; see doc/troubleshooting.md).
     * For menu bar items ([WMenuBarItem]), it may not work yet for a short while right after they are loaded.
     * WinUI generates the text at the right edge automatically ("Ctrl+S" etc.). Setting [keyboardAcceleratorText] takes precedence.
     * Keystrokes for typed characters, key releases, or that include AltGraph or mouse buttons throw [IllegalArgumentException]
     * (the setting stays unchanged).
     */
    var accelerator: WKeyStroke? = null
        set(value) {
            val next = value?.toAccelerator()
            field?.toAccelerator()?.let { (key, modifiers) -> removeKeyboardAccelerator(key, modifiers) }
            field = value
            if (next != null) appendKeyboardAccelerator(createKeyboardAccelerator(next.first, next.second))
        }

    /**
     * AbstractButton.doClick-like: selects the item once programmatically. It is implemented with UI Automation's Invoke, so as with a mouse
     * selection the Click listeners and [command] run and an open menu closes. A [WToggleMenuFlyoutItem] toggles its check, and a
     * [WRadioMenuFlyoutItem] is checked (stays as is if already checked). Does nothing if disabled ([isEnabled] is false), as in Swing.
     */
    fun doClick() {
        if (!isEnabled) return
        Xaml.invoke(inspectable)
    }

    /**
     * AbstractButton.getAction / setAction-like: the [WAction] bound to the item. null (default) means no binding.
     * When set, copies the action's values to the item as follows and keeps following every change of the action's values. Items without a value are unset
     * (text is "", icon, tooltip, action command and accelerator are null, mnemonic is 0). Same as Swing's JMenuItem.
     * - [WAction.NAME] to [text], [WAction.SMALL_ICON] to [icon], [WAction.SHORT_DESCRIPTION] to [toolTipText]
     * - [WAction.MNEMONIC_KEY] to [mnemonic], [WAction.ACTION_COMMAND_KEY] to [actionCommand]
     * - [WAction.ACCELERATOR_KEY] to [accelerator] ([WKeyStroke])
     * - [WAction.isEnabled] to [isEnabled]
     *
     * On click, calls [WAction.actionPerformed] from the item's Click event (in the same order and with the same [WActionEvent]
     * as the [addActionListener] listeners). Replacing it with another action or null unbinds the previous action. A value of the wrong type
     * throws [IllegalArgumentException]. The action calls the item through a weak reference, so unused items can be collected even if the action is shared.
     */
    var action: WAction? = null
        set(value) {
            if (field === value) return
            actionBinder.detach()
            // Replace before mapping the values (so that the mapped selection state change is not written back to the previous action; same order as Swing's setAction)
            field = value
            actionBinder.attach(value)
            onActionChanged(value)
        }

    /** The binding to [action] (following values and calling on click). */
    private val actionBinder = ActionBinder(
        object : ActionTarget {
            override fun applyActionValue(action: WAction?, key: String) = this@WMenuFlyoutItem.applyActionValue(action, key)

            override fun addActionClickListener(listener: WActionListener) = addActionListenerForJava(listener)

            override fun removeActionClickListener(listener: WActionListener) = removeActionListenerForJava(listener)
        },
        MENU_ITEM_ACTION_KEYS,
    )

    private fun applyActionValue(action: WAction?, key: String) {
        when (key) {
            WAction.NAME -> text = action?.stringValue(key) ?: ""
            WAction.SMALL_ICON -> icon = action?.typedValue<WIcon>(key, "WIcon")
            WAction.SHORT_DESCRIPTION -> toolTipText = action?.stringValue(key)
            WAction.MNEMONIC_KEY -> mnemonic = action?.keyCodeValue(key) ?: 0
            WAction.ACTION_COMMAND_KEY -> actionCommand = action?.stringValue(key)
            WAction.ACCELERATOR_KEY -> accelerator = action?.typedValue<WKeyStroke>(key, "WKeyStroke")
            WAction.ENABLED -> isEnabled = action?.isEnabled ?: true
            WAction.SELECTED_KEY -> applyActionSelected(action)
        }
    }

    /** Copies [WAction.SELECTED_KEY] of [action] to the selection state. Only subclasses with a selection state (check and radio items) override it. */
    internal open fun applyActionSelected(action: WAction?) {
        // Items without a selection state do not read SELECTED_KEY (same as Swing's JMenuItem)
    }

    /** Called after [action] is replaced (subclasses with a selection state add or remove the subscription that writes back to the action). */
    internal open fun onActionChanged(action: WAction?) {
        // Items without a selection state have nothing to write back to the action
    }

    /**
     * ActionListener-like. Implemented as a subscription to MenuFlyoutItem.Click (RoutedEventHandler).
     * The listener receives a [WActionEvent] whose source is this item and whose action command is [actionCommand].
     * The handler looks up the item through [ListenerAnchor], so a menu that was opened and closed is collected together with its items once released.
     * As with buttons, items of a menu never opened are not collected until the listeners are removed (to pass the source even when invoked closed by an accelerator).
     */
    @JvmSynthetic
    fun addActionListener(listener: (WActionEvent) -> Unit) {
        clickListeners.addKotlin(listener)
    }

    /**
     * The legacy form that does not receive the event. Kept to preserve the same JVM signature as `addActionListener(Runnable)` (binary compatibility).
     * It is not visible from Kotlin (HIDDEN), so calls that passed a value of type `() -> Unit` or a function reference such as `::save`
     * must be rewritten as `addActionListener { save() }` (same as [WButtonBase.addActionListener]).
     */
    @Deprecated("Use addActionListener that receives a WActionEvent", level = DeprecationLevel.HIDDEN)
    @JvmSynthetic
    fun addActionListener(listener: () -> Unit) {
        clickListeners.addKotlinWithoutEvent(listener)
    }

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: Runnable) {
        clickListeners.add(listener)
    }

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: WActionListener) {
        clickListeners.add(listener)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    @JvmSynthetic
    fun removeActionListener(listener: (WActionEvent) -> Unit) {
        clickListeners.removeKotlin(listener)
    }

    /** Removal for the legacy `() -> Unit` form of [addActionListener] (not visible from Kotlin). The reason is the same as for the legacy [addActionListener]. */
    @Deprecated("Use removeActionListener that receives a WActionEvent", level = DeprecationLevel.HIDDEN)
    @JvmSynthetic
    fun removeActionListener(listener: () -> Unit) {
        clickListeners.removeKotlin(listener)
    }

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: Runnable) {
        clickListeners.remove(listener)
    }

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: WActionListener) {
        clickListeners.remove(listener)
    }
}

/** ABI values for MenuFlyoutItem.Click (RoutedEventHandler). */
private val MENU_ITEM_CLICK = ClickEvent(
    "WinUI4K.MenuClickHandler",
    XamlInterop.IID_RoutedEventHandler,
    XamlInterop.IMenuFlyoutItem_add_Click,
    XamlInterop.IMenuFlyoutItem_remove_Click,
)

/** Keys of the values copied from [WMenuFlyoutItem.action] to the item (in the order of the initial full copy; the selection state comes last, as in Swing). */
private val MENU_ITEM_ACTION_KEYS = listOf(
    WAction.NAME,
    WAction.SMALL_ICON,
    WAction.SHORT_DESCRIPTION,
    WAction.MNEMONIC_KEY,
    WAction.ACTION_COMMAND_KEY,
    WAction.ACCELERATOR_KEY,
    WAction.ENABLED,
    WAction.SELECTED_KEY,
)

/**
 * JCheckBoxMenuItem-like: WinUI 3's ToggleMenuFlyoutItem.
 * [isChecked] toggles on every click, and a checkmark is shown accordingly.
 */
class WToggleMenuFlyoutItem internal constructor(inspectable: ComPtr) : WMenuFlyoutItem(inspectable) {
    /** JCheckBoxMenuItem(String, Icon)-like: an item with text and an icon. Text only if [icon] is null. */
    @JvmOverloads
    constructor(text: String = "", icon: WIcon? = null) : this(
        Activation.composeDefault(XamlInterop.CLS_ToggleMenuFlyoutItem, XamlInterop.IID_IToggleMenuFlyoutItemFactory),
    ) {
        initLabel(text, icon)
    }

    /** The legacy `WToggleMenuFlyoutItem(String, Symbol)` (for binary compatibility; not visible from source). The reason is the same as for [WMenuFlyoutItem]. */
    @Deprecated("Use the constructor that takes a WIcon", level = DeprecationLevel.HIDDEN)
    constructor(text: String = "", icon: Symbol? = null) : this(text, icon as WIcon?)

    /** The checked state (ToggleMenuFlyoutItem.IsChecked). */
    var isChecked: Boolean
        get() = inspectable.getBool(XamlInterop.IToggleMenuFlyoutItem_get_IsChecked)
        set(value) = inspectable.putBool(XamlInterop.IToggleMenuFlyoutItem_put_IsChecked, value)
}

/**
 * JRadioButtonMenuItem-like: WinUI 3's RadioMenuFlyoutItem.
 * Only one item within the same [groupName] can be checked at a time (mutually exclusive).
 */
class WRadioMenuFlyoutItem @JvmOverloads constructor(text: String = "", groupName: String = "") : WMenuFlyoutItem(
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
class WMenuFlyoutSubItem internal constructor(inspectable: ComPtr) : WMenuFlyoutItemBase(inspectable) {
    /** JMenu(String)-like: a submenu with text and an icon. Text only if [icon] is null. */
    @JvmOverloads
    constructor(text: String = "", icon: WIcon? = null) : this(
        // activatable (default factory), so activate, then QI for the default interface
        Activation.activate(XamlInterop.CLS_MenuFlyoutSubItem, XamlInterop.IID_IMenuFlyoutSubItem),
    ) {
        if (text.isNotEmpty()) this.text = text
        if (icon != null) this.icon = icon
    }

    /** The legacy `WMenuFlyoutSubItem(String, Symbol)` (for binary compatibility; not visible from source). The reason is the same as for [WMenuFlyoutItem]. */
    @Deprecated("Use the constructor that takes a WIcon", level = DeprecationLevel.HIDDEN)
    constructor(text: String = "", icon: Symbol? = null) : this(text, icon as WIcon?)

    private val items: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IMenuFlyoutSubItem_get_Items)) // IVector<MenuFlyoutItemBase>
    }

    /** The submenu's label (MenuFlyoutSubItem.Text). */
    var text: String
        get() = inspectable.getString(XamlInterop.IMenuFlyoutSubItem_get_Text)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(XamlInterop.IMenuFlyoutSubItem_put_Text, h)
        }

    /**
     * AbstractButton.getIcon / setIcon-like: the icon shown to the left of the label (MenuFlyoutSubItem.Icon). Returns the icon as set.
     * null (default) means no icon.
     */
    var icon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            val iconElement = value?.createIconElement()
            try {
                inspectable.call(XamlInterop.IMenuFlyoutSubItem_put_Icon, iconElement?.ptr)
            } finally {
                iconElement?.release()
            }
        }

    /** The legacy `setIcon(Symbol)` (for binary compatibility; not visible from source). */
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
     * JMenu.getMnemonic / setMnemonic(int)-like: the key code of the access key (UIElement.AccessKey). The values and constraints are
     * the same as [WMenuFlyoutItem.mnemonic]. Submenus have no action or accelerator (Swing's JMenu.setAccelerator cannot be used either).
     */
    var mnemonic: Int
        get() = Mnemonics.read(uiElement)
        set(value) = Mnemonics.write(uiElement, value)

    /** JMenu.setMnemonic(char)-like: sets the access key by character (same as [WMenuFlyoutItem.setMnemonic]). */
    fun setMnemonic(mnemonic: Char) {
        this.mnemonic = Mnemonics.keyCodeOf(mnemonic)
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
