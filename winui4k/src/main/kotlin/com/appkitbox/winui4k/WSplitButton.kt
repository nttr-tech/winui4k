package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * A two-part button with a main click and a drop-down of choices (a JButton with an arrow added): WinUI 3's
 * SplitButton (derived from ContentControl). A click on the left side goes to [addActionListener], and the arrow on the
 * right side opens [flyout].
 *
 * It has the same JButton-style operations as [WButtonBase] ([text] / [content] / [icon] and the text position,
 * [actionCommand], [mnemonic], [doClick], [command] / [commandParameter], and [addActionListener] receiving a
 * [WActionEvent]).
 * WinUI's SplitButton is not a ButtonBase, so this is not derived from [WButtonBase] but shares the same parts
 * (ButtonLabel and ClickListeners).
 * An Action ([action]) is also bound with the same mechanism (ActionBinder) as [WButtonBase.action].
 */
open class WSplitButton internal constructor(inspectable: ComPtr) : WControl(inspectable) {
    @JvmOverloads
    constructor(text: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_SplitButton, XamlInterop.IID_ISplitButtonFactory),
    ) {
        if (text.isNotEmpty()) this.text = text
    }

    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /** The ISplitButton view holding Flyout / Click (also used by ToggleSplitButton). */
    private val splitButton: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_ISplitButton))
    }

    /** Assembles content / text / icon (shared with WButtonBase). */
    private val label: ButtonLabel by lazy { ButtonLabel(this, contentControl) }

    /** The Click subscriptions registered with addActionListener (shared with WButtonBase). */
    private val clickListeners: ClickListeners by lazy {
        ClickListeners(splitButton, SPLIT_BUTTON_CLICK, listenerAnchor) { (it as WSplitButton).actionCommand }
    }

    /**
     * The button's label string (ContentControl.Content). It is an Object, so it is boxed when passed.
     * With an icon ([icon]), returns the text placed next to the icon. Returns "" if [content] holds a component.
     */
    var text: String
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
            label.content = value
        }

    /**
     * AbstractButton.getIcon / setIcon-style: an icon displayed next to the text. null (default) means no icon.
     * The layout, the relation to [content] and the name for screen readers are the same as [WButtonBase.icon]
     * (Content becomes a StackPanel that lines up the icon and text).
     */
    var icon: WIcon?
        get() = label.icon
        set(value) {
            label.icon = value
        }

    /** AbstractButton.setHorizontalTextPosition-style: the horizontal position of the text relative to the icon. Same meaning as [WButtonBase.horizontalTextPosition]. */
    var horizontalTextPosition: HorizontalTextPosition
        get() = label.horizontalTextPosition
        set(value) {
            label.horizontalTextPosition = value
        }

    /** AbstractButton.setVerticalTextPosition-style: the vertical position of the text relative to the icon. Same meaning as [WButtonBase.verticalTextPosition]. */
    var verticalTextPosition: VerticalTextPosition
        get() = label.verticalTextPosition
        set(value) {
            label.verticalTextPosition = value
        }

    /** AbstractButton.setIconTextGap-style: the gap between the icon and the text (DIP). Same meaning and constraints as [WButtonBase.iconTextGap]. */
    var iconTextGap: Double
        get() = label.iconTextGap
        set(value) {
            label.iconTextGap = value
        }

    /** The flyout opened by clicking the right-side arrow (SplitButton.Flyout). */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            splitButton.call(XamlInterop.ISplitButton_put_Flyout, value?.flyoutBase?.ptr)
            value?.attachedTo(this)
        }

    /**
     * AbstractButton.getActionCommand-style: the string put in [WActionEvent.actionCommand] on a click of the main part.
     * While unset (null), returns [text]. Setting null returns it to unset.
     */
    var actionCommand: String? = null
        get() = field ?: text

    /**
     * The command executed on a click of the main part (SplitButton.Command). As with [WButtonBase.command], a click
     * calls [WCommand.execute] with [commandParameter], and the button is disabled if WCommand.isEnabled=false.
     */
    var command: WCommandBase? = null
        set(value) {
            field = value
            splitButton.call(XamlInterop.ISplitButton_put_Command, value?.commandPtr)
        }

    /** The argument passed when [command] is executed (SplitButton.CommandParameter). */
    var commandParameter: String? = null
        set(value) {
            field = value
            putCommandParameter(splitButton, XamlInterop.ISplitButton_put_CommandParameter, value)
        }

    /**
     * AbstractButton.getMnemonic / setMnemonic(int)-style: the key code of the access key (UIElement.AccessKey).
     * The value range and behavior are the same as [WButtonBase.mnemonic] (the same values as AWT's VK_A..VK_Z /
     * VK_0..VK_9; 0 clears it).
     * Pressing the access key clicks the main part.
     */
    var mnemonic: Int
        get() = Mnemonics.read(uiElement)
        set(value) = Mnemonics.write(uiElement, value)

    /** AbstractButton.setMnemonic(char)-style: sets the access key by a character. Same meaning as [WButtonBase.setMnemonic]. */
    fun setMnemonic(mnemonic: Char) {
        this.mnemonic = Mnemonics.keyCodeOf(mnemonic)
    }

    /**
     * AbstractButton.doClick-style: clicks the main part (left side) once programmatically.
     * As with a mouse click, the Click listeners ([addActionListener]) and [command] run (the arrow on the right is not
     * pressed, so [flyout] does not open).
     * In [WToggleSplitButton], the check state is toggled first, then the Click listeners run.
     * Does nothing if disabled ([isEnabled] is false) or if [command] cannot execute (WCommand.isEnabled is false), as in
     * Swing.
     *
     * **Does nothing before being placed in a window (while the real template does not exist yet)** (unlike
     * [WButtonBase.doClick]).
     * WinUI's SplitButton cannot raise Click without its main Button (a template part). In that case the UI Automation
     * Invoke of the SplitButton itself fails with E_INVALIDARG (SplitButton.cpp passes null to
     * FrameworkElementAutomationPeer::FromElement), and ToggleSplitButton's UI Automation has no Invoke, while Toggle
     * only changes the check state without raising Click.
     * The default template is applied when the control enters the visual tree (no need to wait for Loaded).
     *
     * Under the hood, it calls the UI Automation Invoke of the template's main Button (PrimaryButton).
     * This goes through the same path as a real click (the check toggle of [WToggleSplitButton], then the Click
     * listeners and [command]).
     */
    fun doClick() {
        if (!isEnabled) return
        clickPrimary()
    }

    /** AbstractButton.doClick(int pressTime)-like: the same as [doClick] ([pressTime] is not used, for the same reason as [WButtonBase.doClick]). */
    @Suppress("UNUSED_PARAMETER")
    fun doClick(pressTime: Int) {
        doClick()
    }

    private fun clickPrimary() {
        val primary = Xaml.templatePartOrNull(inspectable, PRIMARY_BUTTON_PART) ?: return
        try {
            if (isControlEnabled(primary)) Xaml.invoke(primary)
        } finally {
            primary.release()
        }
    }

    /** Whether [element] (Control) is enabled. When the command cannot execute, only the main Button is disabled (the SplitButton itself stays enabled). */
    private fun isControlEnabled(element: ComPtr): Boolean {
        val control = element.queryInterface(XamlInterop.IID_IControl)
        return try {
            control.getBool(XamlInterop.IControl_get_IsEnabled)
        } finally {
            control.release()
        }
    }

    /**
     * ActionListener-like: subscribes to clicks on the body (left side).
     * Subscribes to SplitButton.Click (TypedEventHandler<SplitButton, SplitButtonClickEventArgs>) under the hood.
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

    /** Removal for the `() -> Unit` legacy form of [addActionListener] (not visible from Kotlin), for the same reason as the legacy [addActionListener]. */
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

    /** AbstractButton.getActionListeners-like: the added listeners (in the order added; wrapped ones can be removed too, as with [WButtonBase.actionListeners]). */
    val actionListeners: Array<WActionListener>
        get() = clickListeners.listeners()

    /**
     * AbstractButton.getAction / setAction-like: the [WAction] bound to the main part (left side). null (the default) means no binding.
     * The copied items, following, calling on click and handling of null are the same as [WButtonBase.action]
     * ([WAction.NAME] to [text], [WAction.SMALL_ICON] to [icon], [WAction.SHORT_DESCRIPTION] to [toolTipText],
     * [WAction.MNEMONIC_KEY] to [mnemonic], [WAction.ACTION_COMMAND_KEY] to [actionCommand], [WAction.isEnabled] to [isEnabled].
     * Only [WToggleSplitButton] copies [WAction.SELECTED_KEY] to and from [WToggleSplitButton.isChecked]. Writing the checked state back to the action
     * is done from IsCheckedChanged, so changes before being placed in a window, for which WinUI does not raise this event, are not written back).
     * On click, [WAction.actionPerformed] is called from the main part's Click (in the same order and with the same [WActionEvent] as the [addActionListener] listeners).
     * It is not called when [flyout] is opened with the arrow on the right. There is no hideActionText.
     * [IllegalArgumentException] if a value has the wrong type. The action refers to the button through a weak reference.
     */
    var action: WAction? = null
        set(value) {
            if (field === value) return
            actionBinder.detach()
            // Switch before copying the values (so that a selection change from copying is not written back to the previous action; the same order as Swing's setAction)
            field = value
            actionBinder.attach(value)
            onActionChanged(value)
        }

    /** The binding to [action] (following its values and calling it on click). The action looks up the button through a weak reference. */
    private val actionBinder = ActionBinder(
        object : ActionTarget {
            override fun applyActionValue(action: WAction?, key: String) = this@WSplitButton.applyActionValue(action, key)

            override fun addActionClickListener(listener: WActionListener) = addActionListener(listener)

            override fun removeActionClickListener(listener: WActionListener) = removeActionListener(listener)
        },
        SPLIT_BUTTON_ACTION_KEYS,
    )

    /** Copies the value of [action] to the item corresponding to [key] (unset if action is null). */
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

    /** Copies [WAction.SELECTED_KEY] of [action] to the selection state. Only [WToggleSplitButton], which has a selection state, overrides this. */
    internal open fun applyActionSelected(action: WAction?) {
        // A button without a selection state does not read SELECTED_KEY
    }

    /** Called after [action] changes ([WToggleSplitButton] attaches or detaches the subscription that writes the selection state back to the action). */
    internal open fun onActionChanged(action: WAction?) {
        // A button without a selection state has nothing to write back to the action
    }
}

/** The keys of the items copied from [WSplitButton.action] (in the order of the initial full copy; the selection state comes last, as in Swing). */
private val SPLIT_BUTTON_ACTION_KEYS = listOf(
    WAction.NAME,
    WAction.SMALL_ICON,
    WAction.SHORT_DESCRIPTION,
    WAction.MNEMONIC_KEY,
    WAction.ACTION_COMMAND_KEY,
    WAction.ENABLED,
    WAction.SELECTED_KEY,
)

/** ABI values of SplitButton.Click (TypedEventHandler<SplitButton, SplitButtonClickEventArgs>). */
private val SPLIT_BUTTON_CLICK = ClickEvent(
    "WinUI4K.SplitButtonClickHandler",
    XamlInterop.IID_SplitButtonClickHandler,
    XamlInterop.ISplitButton_add_Click,
    XamlInterop.ISplitButton_remove_Click,
)

/** The name of the main (left-side) Button in the SplitButton / ToggleSplitButton template. */
private const val PRIMARY_BUTTON_PART = "PrimaryButton"
