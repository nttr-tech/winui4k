package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.concurrent.atomic.AtomicLong
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

    /** The menu it is in (set by the item management of WMenuBarItem / WMenuFlyoutSubItem / WMenuFlyout). */
    internal var menuOwner: MenuItems<WMenuFlyoutItemBase>? = null
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

    /** The shared selection state part of subclasses with a selection state (check and radio items). null for items without one. */
    internal open val menuSelection: MenuItemSelection? get() = null

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
 * Each click toggles [isChecked] (= [isSelected] / [state]) and shows a check mark.
 * Changes are received with [addItemListener], and adding it to a [WButtonGroup] makes it exclusive. It can be created with the same constructors as
 * Swing's JCheckBoxMenuItem (text, icon, selection state, [WAction]).
 */
class WToggleMenuFlyoutItem internal constructor(inspectable: ComPtr) : WMenuFlyoutItem(inspectable), WSelectable {
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

    /** JCheckBoxMenuItem(String, boolean)-like: an item with the given text and selection state. */
    constructor(text: String, selected: Boolean) : this(text) {
        if (selected) isSelected = true
    }

    /** JCheckBoxMenuItem(Icon)-like: an item with only an icon. */
    constructor(icon: WIcon) : this("", icon)

    /** JCheckBoxMenuItem(String, Icon, boolean)-like: an item with the given text, icon and selection state. Text only if [icon] is null. */
    constructor(text: String, icon: WIcon?, selected: Boolean) : this(text, icon) {
        if (selected) isSelected = true
    }

    /** JCheckBoxMenuItem(Action)-like: an item that copies the values of [action] (including the SELECTED_KEY selection state) and calls the action on click. */
    constructor(action: WAction) : this("") {
        this.action = action
    }

    /** The checked state (ToggleMenuFlyoutItem.IsChecked). */
    var isChecked: Boolean
        get() = inspectable.getBool(XamlInterop.IToggleMenuFlyoutItem_get_IsChecked)
        set(value) = inspectable.putBool(XamlInterop.IToggleMenuFlyoutItem_put_IsChecked, value)

    /** AbstractButton.isSelected / setSelected-like: an alias of [isChecked]. */
    override var isSelected: Boolean
        get() = isChecked
        set(value) {
            isChecked = value
        }

    /** JCheckBoxMenuItem.getState / setState-like: an alias of [isChecked]. */
    var state: Boolean
        get() = isChecked
        set(value) {
            isChecked = value
        }

    override val menuSelection = MenuItemSelection(this, this) {
        MenuItemSelection.dependencyProperty(
            XamlInterop.CLS_ToggleMenuFlyoutItem,
            XamlInterop.IID_IToggleMenuFlyoutItemStatics,
            XamlInterop.IToggleMenuFlyoutItemStatics_get_IsCheckedProperty,
        )
    }

    /**
     * ItemListener-like: receives a [WItemEvent] (SELECTED / DESELECTED) every time the check state changes. Called both on clicks and
     * when [isChecked] etc. is set (as in Swing). On [doClick] and clicks, ItemListeners are called before ActionListeners.
     * Implemented as a change callback of ToggleMenuFlyoutItem.IsChecked (since there is no Checked event).
     */
    override fun addItemListener(listener: WItemListener) {
        menuSelection.itemListeners.add(listener)
    }

    /** Removes a listener added with [addItemListener]. */
    override fun removeItemListener(listener: WItemListener) {
        menuSelection.itemListeners.remove(listener)
    }

    override fun applyActionSelected(action: WAction?) = menuSelection.applyActionSelected(action)

    override fun onActionChanged(action: WAction?) = menuSelection.onActionChanged(action)
}

/**
 * JRadioButtonMenuItem-like: WinUI 3's RadioMenuFlyoutItem.
 * Only one item among those with the same [groupName] is checked (exclusive selection). Adding it to a [WButtonGroup] makes it exclusive within that group.
 *
 * An item without a [groupName] (the default "") is independent of every other item, as with Swing's JRadioButtonMenuItem (a unique name is
 * given internally per item; it still reads as ""). WinUI's RadioMenuFlyoutItem shares GroupName across the whole UI thread, so
 * items with the same name are exclusive even in different menus or windows (use [WButtonGroup] to separate groups).
 * With exclusion by name only, the ItemListener receives the selected item's SELECTED before the deselected item's DESELECTED (because WinUI
 * changes IsChecked of the selected item before unchecking the previous one). In a [WButtonGroup], DESELECTED comes first as in Swing.
 * Clicking a checked item does not uncheck it (WinUI's RadioMenuFlyoutItem behavior. Swing's JRadioButtonMenuItem outside a group
 * is unchecked by a click, but not here).
 */
class WRadioMenuFlyoutItem internal constructor(inspectable: ComPtr) : WMenuFlyoutItem(inspectable), WSelectable {
    /** An item with the given text and exclusive selection group name ([groupName]). */
    @JvmOverloads
    constructor(text: String = "", groupName: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_RadioMenuFlyoutItem, XamlInterop.IID_IRadioMenuFlyoutItemFactory),
    ) {
        initLabel(text, null)
        if (groupName.isNotEmpty()) this.groupName = groupName
    }

    /** JRadioButtonMenuItem(String, boolean)-like: an item with the given text and selection state. */
    constructor(text: String, selected: Boolean) : this(text) {
        if (selected) isSelected = true
    }

    /** JRadioButtonMenuItem(Icon)-like: an item with only an icon. */
    constructor(icon: WIcon) : this("") {
        this.icon = icon
    }

    /**
     * JRadioButtonMenuItem(String, Icon)-like: an item with text and an icon. Text only if [icon] is null.
     * Java's `new WRadioMenuFlyoutItem("x", null)` is ambiguous with the (String, String) form, so pass it with a type.
     */
    constructor(text: String, icon: WIcon?) : this(text) {
        if (icon != null) this.icon = icon
    }

    /** JRadioButtonMenuItem(String, Icon, boolean)-like: an item with the given text, icon and selection state. */
    constructor(text: String, icon: WIcon?, selected: Boolean) : this(text, icon) {
        if (selected) isSelected = true
    }

    /** JRadioButtonMenuItem(Action)-like: an item that copies the values of [action] (including the SELECTED_KEY selection state) and calls the action on click. */
    constructor(action: WAction) : this("") {
        this.action = action
    }

    /** The checked state (RadioMenuFlyoutItem.IsChecked). */
    var isChecked: Boolean
        get() = inspectable.getBool(XamlInterop.IRadioMenuFlyoutItem_get_IsChecked)
        set(value) = inspectable.putBool(XamlInterop.IRadioMenuFlyoutItem_put_IsChecked, value)

    /** AbstractButton.isSelected / setSelected-like: an alias of [isChecked]. */
    override var isSelected: Boolean
        get() = isChecked
        set(value) {
            isChecked = value
        }

    /**
     * The exclusive selection group name (RadioMenuFlyoutItem.GroupName). The default "" means "not in any group", independent of other items
     * (a per-item name is given internally, but it reads as ""). Setting "" makes it an independent item again.
     * While it is in a [WButtonGroup], returns the name of that group.
     */
    var groupName: String
        get() = nativeGroupName.let { if (it.startsWith(UNGROUPED_PREFIX)) "" else it }
        set(value) {
            nativeGroupName = value.ifEmpty { newUngroupedName() }
        }

    private var nativeGroupName: String
        get() = inspectable.getString(XamlInterop.IRadioMenuFlyoutItem_get_GroupName)
        set(value) = Hstring.use(value) { h ->
            inspectable.call(XamlInterop.IRadioMenuFlyoutItem_put_GroupName, h)
        }

    init {
        // Make unnamed items independent (WinUI's default "" makes all unnamed items on the UI thread one group)
        nativeGroupName = newUngroupedName()
    }

    override val menuSelection = MenuItemSelection(this, this) {
        MenuItemSelection.dependencyProperty(
            XamlInterop.CLS_RadioMenuFlyoutItem,
            XamlInterop.IID_IRadioMenuFlyoutItemStatics,
            XamlInterop.IRadioMenuFlyoutItemStatics_get_IsCheckedProperty,
        )
    }

    /**
     * ItemListener-like: receives a [WItemEvent] (SELECTED / DESELECTED) every time the check state changes. Called on clicks, on setting,
     * and when it is unchecked because another item of the same group is selected. Clicking a checked item does not change the state, so it is not called
     * (ActionListeners are called. Same as a JRadioButtonMenuItem in a Swing ButtonGroup).
     * Implemented as a change callback of RadioMenuFlyoutItem's own IsChecked (a property separate from the base ToggleMenuFlyoutItem.IsChecked).
     */
    override fun addItemListener(listener: WItemListener) {
        menuSelection.itemListeners.add(listener)
    }

    /** Removes a listener added with [addItemListener]. */
    override fun removeItemListener(listener: WItemListener) {
        menuSelection.itemListeners.remove(listener)
    }

    override fun applyActionSelected(action: WAction?) = menuSelection.applyActionSelected(action)

    override fun onActionChanged(action: WAction?) = menuSelection.onActionChanged(action)

    private companion object {
        /** The prefix of the names given internally to items without a group name (one that does not collide with other names). */
        const val UNGROUPED_PREFIX = "WinUI4K.UngroupedRadioMenuItem#"
        val counter = AtomicLong()

        fun newUngroupedName(): String = UNGROUPED_PREFIX + counter.incrementAndGet()
    }
}

/**
 * JMenu submenu-like: WinUI 3's MenuFlyoutSubItem.
 * Pointing at it cascades the [add]ed child items. Adding and removing items ([add] / [insert] / [remove] / [getItem] ...) uses
 * the same JMenu-like API as [WMenuBarItem].
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

    private val nativeItems: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IMenuFlyoutSubItem_get_Items)) // IVector<MenuFlyoutItemBase>
    }

    private val items = MenuItems.forMenuItems { nativeItems }

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

/** JSeparator-like: WinUI 3's MenuFlyoutSeparator (a menu divider line). */
class WMenuFlyoutSeparator : WMenuFlyoutItemBase(
    Activation.composeDefault(XamlInterop.CLS_MenuFlyoutSeparator, XamlInterop.IID_IMenuFlyoutSeparatorFactory),
)
