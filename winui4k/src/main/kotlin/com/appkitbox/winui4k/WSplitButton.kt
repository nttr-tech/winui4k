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
 * It has no Action equivalent to [WButtonBase.action].
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
     * `addActionListener(Runnable)`. It is not visible from Kotlin (HIDDEN), so calls that passed a value of type
     * `() -> Unit` must be rewritten as `addActionListener { save() }` (the same as [WButtonBase.addActionListener]).
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

    /** Removal for the `() -> Unit` legacy form of [addActionListener] (not visible from Kotlin), for the same reason as the legacy [addActionListener]. */
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

/** ABI values of SplitButton.Click (TypedEventHandler<SplitButton, SplitButtonClickEventArgs>). */
private val SPLIT_BUTTON_CLICK = ClickEvent(
    "WinUI4K.SplitButtonClickHandler",
    XamlInterop.IID_SplitButtonClickHandler,
    XamlInterop.ISplitButton_add_Click,
    XamlInterop.ISplitButton_remove_Click,
)

/** The name of the main (left-side) Button in the SplitButton / ToggleSplitButton template. */
private const val PRIMARY_BUTTON_PART = "PrimaryButton"
