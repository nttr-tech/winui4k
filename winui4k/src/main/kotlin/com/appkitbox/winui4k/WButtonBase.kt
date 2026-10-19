package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference
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

    /** Click event tokens registered via addActionListener (used by removeActionListener). */
    private val clickTokens = ListenerTokens<Any>()

    private var contentComponent: WComponent? = null

    /** The value of [icon] (the icon this class lays out in Content). If null, there is no icon and the string is put in Content as is. */
    private var labelIcon: WIcon? = null

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
        get() = labelIcon
        set(value) = changeLabelIcon(value)

    /**
     * AbstractButton.setHorizontalTextPosition-like: the horizontal position of the text relative to the icon. The
     * default is [HorizontalTextPosition.TRAILING] (text after the icon). [HorizontalTextPosition.CENTER] combined with
     * [verticalTextPosition] stacks the text above / below the icon. CENTER + [VerticalTextPosition.CENTER] (text overlaid
     * on the icon) cannot be built with a StackPanel, so it is laid out the same as TRAILING (text after the icon).
     * In [WAppBarButton] / [WAppBarToggleButton] the value is only kept and not used for display (the positions of the icon
     * and text are decided by the WinUI template from [WAppBarButton.labelPosition] and the CommandBar settings).
     */
    var horizontalTextPosition: HorizontalTextPosition = HorizontalTextPosition.TRAILING
        set(value) {
            if (value == field) return
            field = value
            if (labelIcon != null) showIconLabel()
        }

    /**
     * AbstractButton.setVerticalTextPosition-like: the vertical position of the text relative to the icon. The default
     * is [VerticalTextPosition.CENTER].
     * When [horizontalTextPosition] is CENTER, [VerticalTextPosition.TOP] / [VerticalTextPosition.BOTTOM] puts the text
     * above / below the icon. With LEADING / TRAILING, it is the vertical alignment (top, center, bottom) of the text
     * relative to the icon placed beside it.
     * In [WAppBarButton] / [WAppBarToggleButton] the value is only kept and not used for display (same as [horizontalTextPosition]).
     */
    var verticalTextPosition: VerticalTextPosition = VerticalTextPosition.CENTER
        set(value) {
            if (value == field) return
            field = value
            if (labelIcon != null) showIconLabel()
        }

    /**
     * AbstractButton.setIconTextGap-like: the gap between the icon and the text (DIP; StackPanel.Spacing). The default is
     * 4, the same as Swing.
     * Negative values, NaN, and infinity throw [IllegalArgumentException] (unlike Swing, they cannot be overlapped).
     * In [WAppBarButton] / [WAppBarToggleButton] the value is only kept and not used for display (same as [horizontalTextPosition]).
     */
    var iconTextGap: Double = DEFAULT_ICON_TEXT_GAP
        set(value) {
            require(value >= 0.0 && value.isFinite()) { "iconTextGap must be a finite value of 0 or more: $value" }
            if (value == field) return
            field = value
            if (labelIcon != null) showIconLabel()
        }

    /** The label string while shown with an icon (null while [labelIcon] is null). */
    private var iconLabelText: String? = null

    /**
     * The string put into AutomationProperties.Name when the icon and text are arranged together (a name set by the user
     * is not overwritten).
     */
    private var labelAutomationName: String? = null

    /**
     * The button's label string (ContentControl.Content).
     * Content is Object-typed, so the setter passes a boxed string and the getter unboxes it.
     * With an icon ([icon]), returns the string laid out with the icon.
     * Returns "" if [content] currently holds a component.
     * In [WAppBarButton] / [WAppBarToggleButton] it is an alias of the label ([WAppBarButton.label]) rather than Content.
     */
    open var text: String
        get() {
            iconLabelText?.let { return it }
            val boxed = contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            contentComponent = null
            if (labelIcon != null) {
                iconLabelText = value
                showIconLabel()
            } else {
                putContentText(value)
            }
        }

    /**
     * The Content for showing something other than a string. Mutually exclusive with [text] (whichever is set later wins).
     * Setting it also removes the icon ([icon]).
     */
    var content: WComponent?
        get() = contentComponent
        set(value) {
            contentComponent = value
            labelIcon = null
            iconLabelText = null
            releaseLabelAutomationName()
            contentControl.call(
                XamlInterop.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    private fun putContentText(value: String) {
        val boxed = PropertyValues.boxString(value)
        contentControl.call(XamlInterop.IContentControl_put_Content, boxed.ptr)
        boxed.release()
    }

    /**
     * Changes the icon (the setter of [icon]). Keeps the current [text] and redisplays it laid out with the icon
     * (removing any component in [content]). If null, removes the icon and returns to a string-only Content.
     */
    private fun changeLabelIcon(icon: WIcon?) {
        icon?.requireSupported()
        if (icon == null && labelIcon == null) return
        val label = text
        contentComponent = null
        labelIcon = icon
        if (icon == null) {
            iconLabelText = null
            releaseLabelAutomationName()
            putContentText(label)
        } else {
            iconLabelText = label
            showIconLabel()
        }
    }

    /**
     * Rebuilds Content from [labelIcon] and [iconLabelText]. If the text is empty, only the icon (IconElement); otherwise
     * lays out the icon and a TextBlock in a StackPanel in the order and orientation of [horizontalTextPosition] /
     * [verticalTextPosition], with a spacing of [iconTextGap].
     * The TextBlock's text color and font are inherited from the button's ContentPresenter.
     * With composite Content, the button's UI Automation name is no longer determined from the string
     * (ButtonBaseAutomationPeer uses Content for the name only when it is a string), so the string is put in
     * AutomationProperties.Name.
     */
    private fun showIconLabel() {
        val icon = checkNotNull(labelIcon)
        val label = checkNotNull(iconLabelText)
        val iconElement = XamlElement.adopt(icon.createIconElement())
        if (label.isEmpty()) {
            releaseLabelAutomationName()
            contentControl.call(XamlInterop.IContentControl_put_Content, iconElement.uiElement.ptr)
            return
        }
        // CENTER + TOP / BOTTOM stacks them vertically; LEADING / TRAILING places them side by side (the vertical position
        // becomes the text's vertical alignment). CENTER + CENTER (text overlaid on the icon) cannot be built with a
        // StackPanel, so it is the same as TRAILING (text after the icon).
        val vertical = horizontalTextPosition == HorizontalTextPosition.CENTER &&
            verticalTextPosition != VerticalTextPosition.CENTER
        val textFirst = if (vertical) {
            verticalTextPosition == VerticalTextPosition.TOP
        } else {
            horizontalTextPosition == HorizontalTextPosition.LEADING
        }
        val panel = WPanel(iconTextGap, if (vertical) Orientation.VERTICAL else Orientation.HORIZONTAL)
        val textBlock = WLabel(label)
        val children = if (textFirst) listOf(textBlock, iconElement) else listOf(iconElement, textBlock)
        for (child in children) {
            if (vertical) {
                child.horizontalAlignment = HorizontalAlignment.CENTER
            } else {
                child.verticalAlignment = if (child === textBlock) verticalTextPosition.alignment else VerticalAlignment.CENTER
            }
            panel.add(child)
        }
        contentControl.call(XamlInterop.IContentControl_put_Content, panel.uiElement.ptr)
        val current = accessibleName
        if (current.isEmpty() || current == labelAutomationName) {
            accessibleName = label
            labelAutomationName = label
        }
    }

    /**
     * Removes the AutomationProperties.Name put by [showIconLabel] (left as is if the user has changed it to a different
     * name).
     */
    private fun releaseLabelAutomationName() {
        val name = labelAutomationName ?: return
        labelAutomationName = null
        if (accessibleName == name) accessibleName = ""
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
            if (value == null) {
                buttonBase.call(XamlInterop.IButtonBase_put_CommandParameter, null)
            } else {
                val boxed = PropertyValues.boxString(value)
                buttonBase.call(XamlInterop.IButtonBase_put_CommandParameter, boxed.ptr)
                boxed.release()
            }
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
     */
    fun doClick() {
        if (!isEnabled) return
        Xaml.invoke(inspectable)
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
            detachAction()
            // Replace before mapping the values (so that the mapped selection state change is not written back to the previous action; same order as Swing's setAction)
            field = value
            if (value != null) {
                for (key in ACTION_KEYS) applyActionValue(value, key)
                val sync = ActionSync(this)
                val clickListener = WActionListener { e -> value.actionPerformed(e) }
                value.addPropertyChangeListener(sync)
                addActionListenerForJava(clickListener)
                actionBinding = ActionBinding(value, sync, clickListener)
            } else {
                for (key in ACTION_KEYS) applyActionValue(null, key)
            }
            onActionChanged(value)
        }

    private var actionBinding: ActionBinding? = null

    /** Unbinds [action] (change notifications and click calls). The button's items stay as they are. */
    private fun detachAction() {
        val binding = actionBinding ?: return
        actionBinding = null
        binding.action.removePropertyChangeListener(binding.sync)
        removeActionListenerForJava(binding.clickListener)
    }

    /**
     * Copies the value of [action] to the button item for [key] (unsets it if action is null). Does nothing for an
     * unknown key.
     */
    private fun applyActionValue(action: WAction?, key: String) {
        when (key) {
            WAction.NAME -> text = action?.stringValue(key) ?: ""
            WAction.SMALL_ICON -> icon = action?.typedValue<WIcon>(key, "WIcon")
            WAction.SHORT_DESCRIPTION -> toolTipText = action?.stringValue(key)
            WAction.MNEMONIC_KEY -> mnemonic = action?.keyCodeValue(key) ?: 0
            WAction.ACTION_COMMAND_KEY -> actionCommand = action?.stringValue(key)
            WAction.ENABLED -> isEnabled = action?.isEnabled ?: true
            WAction.SELECTED_KEY -> applyActionSelected(action)
        }
    }

    /** Maps [action]'s [WAction.SELECTED_KEY] to the button's selection state (deselects if null). Only [WToggleButton], which has a selection state, overrides this. */
    internal open fun applyActionSelected(action: WAction?) {
        // Buttons without a selection state do not read SELECTED_KEY (same as Swing's JButton)
    }

    /** Called after [action] changes ([WToggleButton] adds or removes the subscription that writes the selection state back to the action). */
    internal open fun onActionChanged(action: WAction?) {
        // Buttons without a selection state have nothing to write back to the action
    }

    /**
     * A listener that copies to the button when the action changes. Holds the button through a weak reference (removes
     * itself when the button is collected).
     */
    private class ActionSync(button: WButtonBase) : WActionChangeListener {
        private val reference = WeakReference(button)

        override fun actionChanged(action: WAction, key: String) {
            val button = reference.get()
            if (button == null) {
                action.removePropertyChangeListener(this)
            } else {
                button.applyActionValue(action, key)
            }
        }
    }

    private class ActionBinding(val action: WAction, val sync: WActionChangeListener, val clickListener: WActionListener)

    /**
     * ActionListener-like. Implemented as a subscription to ButtonBase.Click (RoutedEventHandler).
     * The listener receives a [WActionEvent] that represents the click.
     */
    @JvmSynthetic
    fun addActionListener(listener: (WActionEvent) -> Unit) {
        val adapter = WActionListener(listener)
        addActionListenerForJava(adapter)
        clickTokens.addKotlinAdapter(listener, adapter)
    }

    /**
     * The old form that does not receive the event. Kept to preserve the same JVM signature (binary compatibility) as
     * `addActionListener(Runnable)`. It is not visible from Kotlin (HIDDEN), so calls that passed a value of type
     * `() -> Unit` or a function reference such as `::save` must be rewritten as `addActionListener { save() }`.
     * It is HIDDEN because a WARNING-level deprecation would make `addActionListener { }` ambiguous.
     */
    @Deprecated("Use addActionListener that receives a WActionEvent", level = DeprecationLevel.HIDDEN)
    @JvmSynthetic
    fun addActionListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addActionListenerForJava(adapter)
        clickTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: Runnable) {
        subscribeClick(listener) { listener.run() }
    }

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: WActionListener) {
        subscribeClick(listener) { listener.actionPerformed(it) }
    }

    private fun subscribeClick(key: Any, onClick: (WActionEvent) -> Unit) {
        // The handler stays on the native object, so it does not capture the button strongly but looks it up from anchor
        // (see ListenerAnchor)
        val anchor = listenerAnchor
        val token = buttonBase.addEventHandler(
            "WinUI4K.ClickHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IButtonBase_add_Click,
        ) { _, _ -> (anchor.get() as? WButtonBase)?.let { button -> onClick(button.createActionEvent()) } }
        clickTokens.add(key, token)
        anchor.listenerAdded()
    }

    private fun createActionEvent() = WActionEvent(
        this,
        actionCommand,
        WActionEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()),
    )

    /** Unsubscribes a listener registered via [addActionListener]. */
    @JvmSynthetic
    fun removeActionListener(listener: (WActionEvent) -> Unit) {
        unsubscribeClick(clickTokens.removeKotlinAdapter(listener) ?: return)
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
        unsubscribeClick(clickTokens.removeKotlinAdapter(listener) ?: return)
    }

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: Runnable) {
        unsubscribeClick(listener)
    }

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: WActionListener) {
        unsubscribeClick(listener)
    }

    private fun unsubscribeClick(key: Any) {
        val token = clickTokens.remove(key) ?: return
        buttonBase.removeEventHandler(XamlInterop.IButtonBase_remove_Click, token)
        listenerAnchor.listenerRemoved()
    }
}

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
