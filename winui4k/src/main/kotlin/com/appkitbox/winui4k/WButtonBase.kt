package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.ClickMode (when Click fires).
 * Values extracted from the winmd (Release=0, Press=1, Hover=2).
 */
enum class ClickMode(internal val native: Int) {
    /** Fires when the button is released (default). */
    RELEASE(0),

    /** Fires when the button is pressed down. */
    PRESS(1),

    /** Fires when the pointer moves over the button. */
    HOVER(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): ClickMode = entries.first { it.native == native }
    }
}

/**
 * AbstractButton-like: common base for ButtonBase-derived controls (Button / HyperlinkButton /
 * RepeatButton / ToggleButton / CheckBox / RadioButton).
 *
 * Provides ContentControl.Content ([text] / [content]), Primitives.ButtonBase's
 * [clickMode] / [isPressed] / [isPointerOver] / [command] / [commandParameter],
 * and the Click event ([addActionListener] / [removeActionListener]).
 */
abstract class WButtonBase internal constructor(inspectable: ComPtr) : WControl(inspectable) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }
    private val buttonBase: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IButtonBase))
    }

    /** Builds content / text / icon (shared with WSplitButton). */
    private val label: ButtonLabel by lazy { ButtonLabel(this, contentControl) }

    /** The Click subscription registered with addActionListener (shared with WSplitButton). */
    private val clickListeners: ClickListeners by lazy {
        ClickListeners(buttonBase, BUTTON_CLICK, listenerAnchor) { (it as WButtonBase).actionCommand }
    }

    /**
     * AbstractButton.getIcon / setIcon-style: an icon shown alongside the text. null (default) means no icon.
     * WinUI's Button and the like have no icon property, so Content is made a StackPanel that lays out the icon
     * (IconElement) and the text (TextBlock). If the text is empty, only the icon becomes the Content. [text] keeps
     * returning the laid-out string.
     * Putting a component in [content] removes the icon (back to null), and setting an icon later removes [content].
     * While the icon and text are laid out together, the string is put in the name for screen readers ([accessibleName])
     * (left as is if the user has set another name).
     * In [WCheckBox] / [WRadioButton] the icon is placed to the right of the check mark (before the text), and the mark is
     * kept (Swing's JCheckBox / JRadioButton setIcon replaces the mark itself, but the mark in the WinUI template cannot
     * be replaced).
     * [WAppBarButton] / [WAppBarToggleButton], which have their own icon property, set it there (Content is not used).
     */
    open var icon: WIcon?
        get() = defaultIcon
        set(value) {
            value?.requireSupported()
            defaultIcon = value
            showStateIcon()
        }

    /** The icon set to [icon] (returns to null when [content] is set). What is shown is [shownIcon]. */
    private var defaultIcon: WIcon? = null

    /**
     * AbstractButton.getSelectedIcon / setSelectedIcon-like: the icon shown instead of [icon] while selected ([WToggleButton.isSelected] is true).
     * If null (the default), [icon] is shown also while selected. Not used by buttons without a selection state ([WButton] etc.)
     * (the same as Swing's JButton). As in Swing, no icon is shown in any state while [icon] is null.
     * The selection state is followed through IsChecked dependency property notifications, whether changed by a click or by the program.
     * [WAppBarButton] / [WAppBarToggleButton] only keep the value and do not use it for display (their icons go into their own properties).
     *
     * @throws IllegalArgumentException when a WIcon implemented on your own in Java is passed
     */
    var selectedIcon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            stateIconsChanged()
        }

    /**
     * AbstractButton.getDisabledIcon / setDisabledIcon-like: the icon shown instead of [icon] while disabled ([isEnabled] is false).
     * If null (the default), [icon] is shown also while disabled (Swing automatically creates a grayed version of [icon]; WinUI icons fade
     * following the text color of the disabled button). While disabled and selected, as with Swing's getDisabledSelectedIcon, [selectedIcon] is shown if set,
     * otherwise this icon. Nothing is shown while [icon] is null. Otherwise the same as [selectedIcon].
     *
     * @throws IllegalArgumentException when a WIcon implemented on your own in Java is passed
     */
    var disabledIcon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            stateIconsChanged()
        }

    /** Whether the button is kept through state change notifications for the per-state icons (without that it would be collected while shown and stop following). */
    private var stateIconsPinned = false

    private fun stateIconsChanged() {
        val wanted = selectedIcon != null || disabledIcon != null
        if (wanted) stateChanges.ensureRegistered()
        if (wanted != stateIconsPinned) {
            stateIconsPinned = wanted
            if (wanted) listenerAnchor.listenerAdded() else listenerAnchor.listenerRemoved()
        }
        showStateIcon()
    }

    /** Whether a button with a selection state ([WToggleButton]) is selected (for showing [selectedIcon]). */
    internal open val showsSelected: Boolean
        get() = false

    /** The icon to show in the current state (chosen the same way as BasicButtonUI). */
    private val shownIcon: WIcon?
        get() {
            val base = defaultIcon ?: return null
            val selected = showsSelected
            return when {
                !isEnabled -> (if (selected) selectedIcon else null) ?: disabledIcon ?: base
                selected -> selectedIcon ?: base
                else -> base
            }
        }

    /** Rearranges if [shownIcon] differs from the icon currently arranged. */
    private fun showStateIcon() {
        val shown = shownIcon
        if (shown !== label.icon) label.icon = shown
    }

    /**
     * AbstractButton.setHorizontalTextPosition-like: the horizontal position of the text relative to the icon. The
     * default is [HorizontalTextPosition.TRAILING] (text after the icon). [HorizontalTextPosition.CENTER] combined with
     * [verticalTextPosition] stacks the text above / below the icon. CENTER + [VerticalTextPosition.CENTER] (text overlaid
     * on the icon) cannot be built with a StackPanel, so it is laid out the same as TRAILING (text after the icon).
     * In [WAppBarButton] / [WAppBarToggleButton] the value is only kept and not used for display (the positions of the icon
     * and text are decided by the WinUI template from [WAppBarButton.labelPosition] and the CommandBar settings).
     */
    var horizontalTextPosition: HorizontalTextPosition
        get() = label.horizontalTextPosition
        set(value) {
            label.horizontalTextPosition = value
        }

    /**
     * AbstractButton.setVerticalTextPosition-like: the vertical position of the text relative to the icon. The default
     * is [VerticalTextPosition.CENTER].
     * When [horizontalTextPosition] is CENTER, [VerticalTextPosition.TOP] / [VerticalTextPosition.BOTTOM] puts the text
     * above / below the icon. With LEADING / TRAILING, it is the vertical alignment (top, center, bottom) of the text
     * relative to the icon placed beside it.
     * In [WAppBarButton] / [WAppBarToggleButton] the value is only kept and not used for display (same as [horizontalTextPosition]).
     */
    var verticalTextPosition: VerticalTextPosition
        get() = label.verticalTextPosition
        set(value) {
            label.verticalTextPosition = value
        }

    /**
     * AbstractButton.setIconTextGap-like: the gap between the icon and the text (DIP; StackPanel.Spacing). The default is
     * 4, the same as Swing.
     * Negative values, NaN, and infinity throw [IllegalArgumentException] (unlike Swing, they cannot be overlapped).
     * In [WAppBarButton] / [WAppBarToggleButton] the value is only kept and not used for display (same as [horizontalTextPosition]).
     */
    var iconTextGap: Double
        get() = label.iconTextGap
        set(value) {
            label.iconTextGap = value
        }

    /**
     * AbstractButton.setMargin(Insets)-like: the inner padding between the component's border and its content (Control.Padding). [WInsets] is top, left, bottom, right, as with Swing's Insets.
     * If all four sides are equal it can be read with [padding], and the actual margin including the border's width is read with [insets].
     *
     * It has the same name and meaning as in Swing, but [WComponent.margin] / [WComponent.setMargin] with only numbers as arguments are WinUI's Margin
     * (the space outside the component), so do not confuse them. There is no getter corresponding to Swing's getMargin ([margin] returns the outer space).
     */
    fun setMargin(margin: WInsets) = setPaddingFromInsets(margin)

    /**
     * The button's label string (ContentControl.Content).
     * Content is Object-typed, so the setter passes a boxed string and the getter unboxes it.
     * With an icon ([icon]), returns the string laid out with the icon.
     * Returns "" if [content] currently holds a component.
     * In [WAppBarButton] / [WAppBarToggleButton] it is an alias of the label ([WAppBarButton.label]) rather than Content.
     */
    open var text: String
        get() = label.text
        set(value) {
            label.text = value
        }

    /**
     * The Content for showing something other than a string. Mutually exclusive with [text] (whichever is set later wins).
     * Setting it also removes the icon ([icon]).
     */
    var content: WComponent?
        get() = label.content
        set(value) {
            defaultIcon = null
            label.content = value
        }

    /**
     * AbstractButton.getHorizontalAlignment / setHorizontalAlignment-like: the horizontal alignment of the text and icon (the content) inside the button.
     * An alias with Swing's name of [horizontalContentAlignment] (Control.HorizontalContentAlignment). The default is the theme's value; as in Swing,
     * CENTER for [WButton] / [WToggleButton] and LEFT for [WCheckBox] / [WRadioButton] (corresponding to Swing's LEADING; when [flowDirection] is
     * right to left, left and right are swapped as in WinUI) (before being added to a window the theme is not applied, so it may return STRETCH).
     * If the button's width stays the content's width, the look does not change (the same as Swing; it takes effect in a widened button). [HorizontalAlignment.STRETCH], which Swing does not have,
     * stretches the content to the full width as in WinUI. The position of the button itself in its parent is [horizontalAlignmentInParent], and they do not affect each other.
     *
     * Change from earlier versions: the former `horizontalAlignment` was the position in the parent (now [horizontalAlignmentInParent]).
     */
    var horizontalAlignment: HorizontalAlignment
        get() = horizontalContentAlignment
        set(value) {
            horizontalContentAlignment = value
        }

    /**
     * AbstractButton.getVerticalAlignment / setVerticalAlignment-like: the vertical alignment of the text and icon (the content) inside the button.
     * An alias with Swing's name of [verticalContentAlignment] (Control.VerticalContentAlignment). The default is CENTER, the same as Swing
     * (before being added to a window the theme is not applied, so it may return STRETCH). [VerticalAlignment.STRETCH] stretches the content to the full height
     * as in WinUI. The position of the button itself in its parent is [verticalAlignmentInParent], and they do not affect each other.
     *
     * Change from earlier versions: the former `verticalAlignment` was the position in the parent (now [verticalAlignmentInParent]).
     */
    var verticalAlignment: VerticalAlignment
        get() = verticalContentAlignment
        set(value) {
            verticalContentAlignment = value
        }

    /** When Click fires (ButtonBase.ClickMode). */
    var clickMode: ClickMode
        get() = ClickMode.of(buttonBase.getInt(XamlInterop.IButtonBase_get_ClickMode))
        set(value) = buttonBase.call(XamlInterop.IButtonBase_put_ClickMode, value.native)

    /** Whether the button is currently pressed down (ButtonBase.IsPressed). */
    val isPressed: Boolean
        get() = buttonBase.getBool(XamlInterop.IButtonBase_get_IsPressed)

    /** Whether the pointer is currently over the button (ButtonBase.IsPointerOver). */
    val isPointerOver: Boolean
        get() = buttonBase.getBool(XamlInterop.IButtonBase_get_IsPointerOver)

    /**
     * Equivalent to Swing's Action (ButtonBase.Command). On click, [WCommand.execute] is
     * invoked with [commandParameter], and the button is disabled whenever WCommand.isEnabled = false.
     */
    var command: WCommandBase? = null
        set(value) {
            field = value
            buttonBase.call(
                XamlInterop.IButtonBase_put_Command,
                value?.commandPtr,
            )
        }

    /**
     * AbstractButton.getActionCommand-like: the string put into [WActionEvent.actionCommand] on click.
     * While unset (null), returns [text]. Setting null returns it to unset.
     */
    var actionCommand: String? = null
        get() = field ?: text

    /** The argument passed when [command] runs (ButtonBase.CommandParameter). */
    var commandParameter: String? = null
        set(value) {
            field = value
            putCommandParameter(buttonBase, XamlInterop.IButtonBase_put_CommandParameter, value)
        }

    /**
     * AbstractButton.getMnemonic / setMnemonic(int)-like: the key code of the access key (UIElement.AccessKey).
     * Uses the same values as AWT's KeyEvent.VK_A..VK_Z (65..90) / VK_0..VK_9 (48..57); leaving it unset or setting 0
     * clears it (reads as 0). Anything other than letters and digits throws [IllegalArgumentException].
     * The key appears in the key tips shown when the Alt key is pressed, and pressing that key clicks the button
     * (unlike Swing, the character in the label is not underlined).
     */
    var mnemonic: Int
        get() = Mnemonics.read(uiElement)
        set(value) = Mnemonics.write(uiElement, value)

    /**
     * AbstractButton.setMnemonic(char)-like: sets the access key by character. Letters are stored converted to uppercase,
     * and '\u0000' clears it. Anything other than letters and digits throws [IllegalArgumentException].
     */
    fun setMnemonic(mnemonic: Char) {
        this.mnemonic = Mnemonics.keyCodeOf(mnemonic)
    }

    /**
     * AbstractButton.doClick-like: clicks the button once programmatically.
     * Internally it is UI Automation's Invoke (Toggle for toggle types, SelectionItem.Select for [WRadioButton]), and
     * as with a mouse operation, the Click listeners, [command] and the Button's Flyout run in order.
     * For [WToggleButton] / [WCheckBox] it toggles the selection state and also calls the ItemListener; for [WRadioButton]
     * it selects it (if already selected it stays selected and only the ActionListener is called, same as Swing's
     * JRadioButton).
     * Does nothing if disabled ([isEnabled] is false) (same as Swing).
     * Even right after showing (before the first layout), the listeners are called right away, and [WButton.flyout] opens after layout (WinUI silently does not open
     * a Button's Flyout before the first layout).
     */
    fun doClick() {
        if (!isEnabled) return
        val flyoutAfterLayout = flyoutToOpenAfterLayout()
        Xaml.invoke(inspectable)
        flyoutAfterLayout?.showAt(this)
    }

    /**
     * Of the flyouts that [doClick]'s Invoke should open, the one to open after layout because WinUI does not open it before the first layout yet
     * (overridden by [WButton]). null if none.
     */
    internal open fun flyoutToOpenAfterLayout(): WFlyoutBase? = null

    /**
     * AbstractButton.doClick(int pressTime)-like: clicks once, the same as [doClick]. Swing keeps the button looking pressed and
     * stops the UI thread for [pressTime] milliseconds before releasing, but WinUI's Invoke does not create a pressed look, so [pressTime] is not used
     * (and the UI thread is not stopped). The order and content of listener calls are the same as [doClick].
     */
    @Suppress("UNUSED_PARAMETER")
    fun doClick(pressTime: Int) {
        doClick()
    }

    /**
     * AbstractButton.isFocusPainted / setFocusPainted-like: whether to draw the keyboard focus frame when it has the focus
     * (UIElement.UseSystemFocusVisuals). The default is true (the value of WinUI's default style). If false, the focus frame is not drawn even when the focus
     * is moved with the Tab key (it still receives the focus).
     */
    var isFocusPainted: Boolean
        get() = uiElement.getBool(XamlInterop.IUIElement_get_UseSystemFocusVisuals)
        set(value) = uiElement.putBool(XamlInterop.IUIElement_put_UseSystemFocusVisuals, value)

    /**
     * AbstractButton.getHideActionText / setHideActionText-like: if true, [WAction.NAME] of [action] is not copied to [text]
     * (the text becomes "", making an icon-only button). The default is false. Toggling it copies the text again right away if there is an [action]
     * (setting it back to false returns to the NAME text). Without an [action], [text] is not changed (as in Swing).
     */
    var hideActionText: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            action?.let { applyActionValue(it, WAction.NAME) }
        }

    /**
     * AbstractButton.getAction / setAction-like: the [WAction] bound to the button. null (the default) means unbound.
     * When set, the action's values are copied to the button as follows, and the button follows them whenever the
     * action's values change afterwards.
     * Items for which the action has no value are unset (the text becomes "", the icon, tooltip, and action command
     * become null, and the mnemonic becomes 0). Same as Swing's setAction.
     * - [WAction.NAME] to [text], [WAction.SMALL_ICON] to [icon], [WAction.SHORT_DESCRIPTION] to [toolTipText]
     * - [WAction.MNEMONIC_KEY] to [mnemonic], [WAction.ACTION_COMMAND_KEY] to [actionCommand]
     * - [WAction.isEnabled] to [isEnabled]
     * - [WAction.SELECTED_KEY] to [WToggleButton.isSelected] of a [WToggleButton] (and derived classes), which has a
     *   selection state. If there is no value, it is deselected. It is also mapped in reverse: when the selection state
     *   changes by a click or programmatically, the action's SELECTED_KEY value is rewritten only if the action has that
     *   value (same as Swing's JToggleButton; nothing is written to an action without the value).
     *   Buttons without a selection state ([WButton] etc.) do not read SELECTED_KEY.
     *
     * On a click, [WAction.actionPerformed] is called from the button's Click event (it does not go through [command]
     * (ICommand), because going through ICommand would run twice due to the linkage of CanExecute and IsEnabled). So it is
     * called in the same order and with the same [WActionEvent] as listeners added with [addActionListener]. For toggle
     * types it is called after the selection state changes, so reading SELECTED_KEY in actionPerformed gives the new state.
     * Replacing it with another action or null removes the link to the previous action (value tracking and click calls).
     * When set to null, the button's items are put in the "no value" state described above.
     * If a value's type does not match (for example NAME is not a String, or SELECTED_KEY is not a Boolean),
     * [IllegalArgumentException] is thrown.
     * The button is called from the action through a weak reference, so even when an action is shared, buttons no longer
     * used can be collected.
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

    /** The binding to [action] (following values and calling on click). The action reaches the button only through a weak reference. */
    private val actionBinder = ActionBinder(
        object : ActionTarget {
            override fun applyActionValue(action: WAction?, key: String) = this@WButtonBase.applyActionValue(action, key)

            override fun addActionClickListener(listener: WActionListener) = addActionListener(listener)

            override fun removeActionClickListener(listener: WActionListener) = removeActionListener(listener)
        },
        ACTION_KEYS,
    )

    /**
     * Copies the value of [action] to the button item for [key] (unsets it if action is null). Does nothing for an
     * unknown key.
     */
    private fun applyActionValue(action: WAction?, key: String) {
        when (key) {
            WAction.NAME -> text = if (hideActionText) "" else action?.stringValue(key) ?: ""
            WAction.SMALL_ICON -> icon = action?.typedValue<WIcon>(key, "WIcon")
            WAction.SHORT_DESCRIPTION -> toolTipText = action?.stringValue(key)
            WAction.MNEMONIC_KEY -> mnemonic = action?.keyCodeValue(key) ?: 0
            WAction.ACTION_COMMAND_KEY -> actionCommand = action?.stringValue(key)
            WAction.ENABLED -> isEnabled = action?.isEnabled ?: true
            WAction.SELECTED_KEY -> applyActionSelected(action)
        }
    }

    /** As with AbstractButton, the default of [alignmentX] is left-aligned (0). */
    override val defaultAlignmentX: Float get() = LEFT_ALIGNMENT

    /** As with AbstractButton, [WBoxLayout] does not make it larger than its preferred size unless a maximum size is set. */
    override val maximumFollowsPreferred: Boolean get() = true

    /** Maps [action]'s [WAction.SELECTED_KEY] to the button's selection state (deselects if null). Only [WToggleButton], which has a selection state, overrides this. */
    internal open fun applyActionSelected(action: WAction?) {
        // Buttons without a selection state do not read SELECTED_KEY (same as Swing's JButton)
    }

    /** Called after [action] changes ([WToggleButton] adds or removes the subscription that writes the selection state back to the action). */
    internal open fun onActionChanged(action: WAction?) {
        // Buttons without a selection state have nothing to write back to the action
    }

    /**
     * ActionListener-like. Implemented as a subscription to ButtonBase.Click (RoutedEventHandler).
     * The listener receives a [WActionEvent] that represents the click.
     */
    @JvmSynthetic
    fun addActionListener(listener: (WActionEvent) -> Unit) {
        clickListeners.addKotlin(listener)
    }

    /**
     * The old form that does not receive the event. Kept to preserve the same JVM signature (binary compatibility) as
     * `addActionListener(Runnable)`. It is not visible from Kotlin (HIDDEN). A value of type `() -> Unit` or a function reference such as `::save`
     * is passed by SAM conversion to the [addActionListener] that takes a `Runnable`, but it becomes a different `Runnable` each time, so to remove it,
     * keep the `Runnable` value and pass that.
     * It is HIDDEN because a WARNING-level deprecation would make `addActionListener { }` ambiguous.
     */
    @Deprecated("Use addActionListener that receives a WActionEvent", level = DeprecationLevel.HIDDEN)
    @JvmSynthetic
    fun addActionListener(listener: () -> Unit) {
        clickListeners.addKotlinWithoutEvent(listener)
    }

    fun addActionListener(listener: Runnable) {
        clickListeners.add(listener)
    }

    fun addActionListener(listener: WActionListener) {
        clickListeners.add(listener)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    @JvmSynthetic
    fun removeActionListener(listener: (WActionEvent) -> Unit) {
        clickListeners.removeKotlin(listener)
    }

    /**
     * Removal for the old `() -> Unit` form of [addActionListener] (not visible from Kotlin). Calls that passed something
     * like `removeActionListener(::save)` should keep the same function used at registration, such as
     * `val listener = { e: WActionEvent -> save() }`, and pass it.
     * The reason and compatibility are the same as for the old form of [addActionListener].
     */
    @Deprecated("Use removeActionListener that receives a WActionEvent", level = DeprecationLevel.HIDDEN)
    @JvmSynthetic
    fun removeActionListener(listener: () -> Unit) {
        clickListeners.removeKotlin(listener)
    }

    fun removeActionListener(listener: Runnable) {
        clickListeners.remove(listener)
    }

    fun removeActionListener(listener: WActionListener) {
        clickListeners.remove(listener)
    }

    /**
     * AbstractButton.getActionListeners-like: the listeners added with [addActionListener] (in the order added). Kotlin lambdas and [Runnable]s appear as
     * wrapping [WActionListener]s, and any of them can be passed to `removeActionListener` to remove it (the same as writing
     * `for (ActionListener l : b.getActionListeners()) b.removeActionListener(l)` in Java).
     * While an [action] is bound, one internal listener that calls the action is included too (as with Swing's setAction).
     */
    val actionListeners: Array<WActionListener>
        get() = clickListeners.listeners()

    /** The callbacks for state changes (pressed, pointer, enabled, selected). Registered when first needed. */
    internal val stateChanges: ButtonStateChanges by lazy { ButtonStateChanges(this, this is WToggleButton) }

    /** The ChangeListeners. The state change callbacks are registered when the first listener is added. */
    private val changeListeners: ChangeListeners by lazy { ChangeListeners(this) { stateChanges.ensureRegistered() } }

    /**
     * AbstractButton.addChangeListener-like: receives a [WChangeEvent] (whose source is this button) every time the button's state changes.
     * The states, corresponding to Swing's ButtonModel, are four: pressed ([isPressed]), pointer over ([isPointerOver]), enabled ([isEnabled]) and
     * selected ([WToggleButton.isChecked], including changes to indeterminate); an event arrives once every time any one of them changes
     * (both for changes from the program and for user operations). Implemented with the change callbacks of the respective dependency properties.
     *
     * Differences from Swing: there is no Swing armed (pressed and still over the button); it changes together with pressed. The order relative to
     * [WToggleButton.addItemListener] and [addActionListener] for the same change is WinUI's notification order, not necessarily Swing's order (stateChanged → itemStateChanged → actionPerformed).
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** AbstractButton.removeChangeListener-like: removes one listener added with [addChangeListener]. Does nothing if it is not attached. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** AbstractButton.getChangeListeners-like: the ChangeListeners currently attached (in the order added). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    /** The state (pressed, pointer, enabled, selected) changed (called from the [ButtonStateChanges] callbacks). */
    internal fun onButtonStateChanged() {
        showStateIcon()
        changeListeners.fire()
    }
}

/** ABI values of ButtonBase.Click (RoutedEventHandler). */
private val BUTTON_CLICK = ClickEvent(
    "WinUI4K.ClickHandler",
    XamlInterop.IID_RoutedEventHandler,
    XamlInterop.IButtonBase_add_Click,
    XamlInterop.IButtonBase_remove_Click,
)

/** The keys of the items mapped from [WButtonBase.action] to the button (the order used when mapping all at first; the selection state is last, as in Swing). */
private val ACTION_KEYS = listOf(
    WAction.NAME,
    WAction.SMALL_ICON,
    WAction.SHORT_DESCRIPTION,
    WAction.MNEMONIC_KEY,
    WAction.ACTION_COMMAND_KEY,
    WAction.ENABLED,
    WAction.SELECTED_KEY,
)

/** The default gap between the icon and the text (4, the same as Swing's AbstractButton.getIconTextGap). */
internal const val DEFAULT_ICON_TEXT_GAP = 4.0

/**
 * AbstractButton.setHorizontalTextPosition-style: the horizontal position of the text relative to the icon
 * ([WButtonBase.horizontalTextPosition]).
 * [LEADING] / [TRAILING] follow the text flow direction (FlowDirection); in left-to-right flow they are left / right.
 */
enum class HorizontalTextPosition {
    /** Puts the text before the icon (on the left in left-to-right flow). */
    LEADING,

    /** Puts the text after the icon (on the right in left-to-right flow) (the default; same as Swing). */
    TRAILING,

    /**
     * Centers the text horizontally with the icon. With [VerticalTextPosition.TOP] it is above the icon, and with
     * [VerticalTextPosition.BOTTOM] below the icon. [VerticalTextPosition.CENTER] (text overlaid on the icon) cannot be
     * built with a StackPanel, so it is laid out the same as [TRAILING] (text after the icon).
     */
    CENTER,
}

/**
 * AbstractButton.setVerticalTextPosition-style: the vertical position of the text relative to the icon
 * ([WButtonBase.verticalTextPosition]).
 * With [HorizontalTextPosition.CENTER] the text is placed above / below the icon; with LEADING / TRAILING it is the
 * vertical alignment of the text laid out beside the icon.
 */
enum class VerticalTextPosition(internal val alignment: VerticalAlignment) {
    /** Puts the text above the icon (top-aligned when placed side by side). */
    TOP(VerticalAlignment.TOP),

    /** Centers the text vertically with the icon (the default; center-aligned when placed side by side). */
    CENTER(VerticalAlignment.CENTER),

    /** Puts the text below the icon (bottom-aligned when placed side by side). */
    BOTTOM(VerticalAlignment.BOTTOM),
}
