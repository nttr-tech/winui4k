package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
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

    /** Click event tokens registered via addActionListener (used by removeActionListener). */
    private val clickTokens = ListenerTokens<Any>()

    /** A reference for looking up this button from the Click handler (created when the first listener is added). */
    private val listenerAnchor by lazy { ListenerAnchor(this) }

    private var contentComponent: WComponent? = null

    /**
     * The icon shown alongside the label ([WButton.icon]). If null, there is no icon and the string is put into the
     * Content as is.
     * Controls other than Button (such as AppBarButton) have their own icon properties, so only [WButton] exposes this.
     */
    internal var labelIcon: WIcon? = null
        private set

    /** The horizontal position of the text relative to the icon ([WButton.horizontalTextPosition]). */
    internal var labelHorizontalTextPosition: HorizontalTextPosition = HorizontalTextPosition.TRAILING
        private set

    /** The vertical position of the text relative to the icon ([WButton.verticalTextPosition]). */
    internal var labelVerticalTextPosition: VerticalTextPosition = VerticalTextPosition.CENTER
        private set

    /** The gap between the icon and the text ([WButton.iconTextGap]). */
    internal var labelIconTextGap: Double = DEFAULT_ICON_TEXT_GAP
        private set

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
     * With an icon ([WButton.icon]), returns the string arranged alongside the icon.
     * Returns "" if [content] currently holds a component.
     */
    var text: String
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
     * Setting it also removes the icon ([WButton.icon]).
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
     * Changes the icon (the setter of [WButton.icon]). Keeps the current [text] and shows it again alongside the icon
     * (removing any component in [content]). If null, removes the icon and returns to a string-only Content.
     */
    internal fun changeLabelIcon(icon: WIcon?) {
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

    /** Changes the horizontal position of the text relative to the icon ([WButton.horizontalTextPosition] setter). */
    internal fun changeLabelHorizontalTextPosition(position: HorizontalTextPosition) {
        if (position == labelHorizontalTextPosition) return
        labelHorizontalTextPosition = position
        if (labelIcon != null) showIconLabel()
    }

    /** Changes the vertical position of the text relative to the icon (the setter of [WButton.verticalTextPosition]). */
    internal fun changeLabelVerticalTextPosition(position: VerticalTextPosition) {
        if (position == labelVerticalTextPosition) return
        labelVerticalTextPosition = position
        if (labelIcon != null) showIconLabel()
    }

    /** Changes the gap between the icon and the text (the setter of [WButton.iconTextGap]). */
    internal fun changeLabelIconTextGap(gap: Double) {
        require(gap >= 0.0 && gap.isFinite()) { "iconTextGap must be a finite value of 0 or more: $gap" }
        if (gap == labelIconTextGap) return
        labelIconTextGap = gap
        if (labelIcon != null) showIconLabel()
    }

    /**
     * Rebuilds the Content from [labelIcon] and [iconLabelText]. If the text is empty, only the icon (IconElement);
     * otherwise a StackPanel arranging the icon and a TextBlock in the order and orientation of
     * [labelHorizontalTextPosition] / [labelVerticalTextPosition], with the gap set to [labelIconTextGap].
     * The TextBlock inherits its text color and font from the button's ContentPresenter.
     * With a composite Content, the button's UI Automation name is no longer derived from the string
     * (ButtonBaseAutomationPeer uses the Content for the name only when it is a string), so the string is put into
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
        val vertical = labelHorizontalTextPosition == HorizontalTextPosition.CENTER &&
            labelVerticalTextPosition != VerticalTextPosition.CENTER
        val textFirst = if (vertical) {
            labelVerticalTextPosition == VerticalTextPosition.TOP
        } else {
            labelHorizontalTextPosition == HorizontalTextPosition.LEADING
        }
        val panel = WPanel(labelIconTextGap, if (vertical) Orientation.VERTICAL else Orientation.HORIZONTAL)
        val textBlock = WLabel(label)
        val children = if (textFirst) listOf(textBlock, iconElement) else listOf(iconElement, textBlock)
        for (child in children) {
            if (vertical) {
                child.horizontalAlignment = HorizontalAlignment.CENTER
            } else {
                child.verticalAlignment = if (child === textBlock) labelVerticalTextPosition.alignment else VerticalAlignment.CENTER
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
        get() {
            val key = uiElement.getString(XamlInterop.IUIElement_get_AccessKey)
            return if (key.length == 1) key[0].code else 0
        }
        set(value) {
            require(value == 0 || isMnemonicKeyCode(value)) { "mnemonic must be the key code of an uppercase letter or a digit: $value" }
            val key = if (value == 0) "" else value.toChar().toString()
            Hstring.use(key) { h -> uiElement.call(XamlInterop.IUIElement_put_AccessKey, h) }
        }

    /**
     * AbstractButton.setMnemonic(char)-like: sets the access key by character. Letters are stored converted to uppercase,
     * and '\u0000' clears it. Anything other than letters and digits throws [IllegalArgumentException].
     */
    fun setMnemonic(mnemonic: Char) {
        require(mnemonic == '\u0000' || (mnemonic.code < ASCII_LIMIT && mnemonic.isLetterOrDigit())) {
            "mnemonic must be an ASCII letter or digit: '$mnemonic'"
        }
        this.mnemonic = mnemonic.uppercaseChar().code
    }

    /**
     * AbstractButton.doClick-like: clicks the button once programmatically.
     * Implemented with UI Automation's Invoke, so like a mouse click it runs through the Click listeners, [command], and
     * the Button's Flyout in order. Does nothing if disabled ([isEnabled] is false) (same as Swing).
     */
    fun doClick() {
        if (!isEnabled) return
        Xaml.invoke(inspectable)
    }

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

    /** The old form without the event. Kept to preserve the same JVM signature as `addActionListener(Runnable)`. */
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
        ) { _, _ -> anchor.get()?.let { button -> onClick(button.createActionEvent()) } }
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

private const val ASCII_LIMIT = 128

/** The default gap between the icon and the text (4, the same as Swing's AbstractButton.getIconTextGap). */
internal const val DEFAULT_ICON_TEXT_GAP = 4.0

/**
 * AbstractButton.setHorizontalTextPosition-like: the horizontal position of the text relative to the icon
 * ([WButton.horizontalTextPosition]).
 * [LEADING] / [TRAILING] follow the text flow direction (FlowDirection) and mean left / right in left-to-right flow.
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
 * AbstractButton.setVerticalTextPosition-like: the vertical position of the text relative to the icon
 * ([WButton.verticalTextPosition]).
 * With [HorizontalTextPosition.CENTER] the text is placed above / below the icon; with LEADING / TRAILING it is the
 * vertical alignment of the text placed beside the icon.
 */
enum class VerticalTextPosition(internal val alignment: VerticalAlignment) {
    /** Puts the text above the icon (top-aligned when placed side by side). */
    TOP(VerticalAlignment.TOP),

    /** Centers the text vertically with the icon (the default; center-aligned when placed side by side). */
    CENTER(VerticalAlignment.CENTER),

    /** Puts the text below the icon (bottom-aligned when placed side by side). */
    BOTTOM(VerticalAlignment.BOTTOM),
}

/** Whether it is AWT's KeyEvent.VK_A..VK_Z (65..90) or VK_0..VK_9 (48..57). */
private fun isMnemonicKeyCode(keyCode: Int): Boolean = keyCode in 'A'.code..'Z'.code || keyCode in '0'.code..'9'.code
