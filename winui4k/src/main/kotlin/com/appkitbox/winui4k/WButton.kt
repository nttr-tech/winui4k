package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * JButton-like: WinUI 3's Button.
 *
 * Covers the full surface Button defines (IButton + Primitives.IButtonBase +
 * ContentControl.Content): [text] / [content], [clickMode], [isPressed], [isPointerOver],
 * [addActionListener] / [removeActionListener] (Click), [command] / [commandParameter], [flyout].
 * The appearance is switched with [isAccent] / [isContentAreaFilled] / [isBorderPainted], and the colors are changed with
 * [background] / [foreground].
 * With [icon], an icon can be placed beside (or above or below) the text (same as JButton(String, Icon)).
 */
open class WButton internal constructor(inspectable: ComPtr) : WButtonBase(inspectable) {
    @JvmOverloads
    constructor(text: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_Button, XamlInterop.IID_IButtonFactory), // Default interface = IButton
    ) {
        if (text.isNotEmpty()) this.text = text
    }

    /** JButton(Icon)-like: a button with only an icon. */
    constructor(icon: WIcon) : this("") {
        this.icon = icon
    }

    /** JButton(String, Icon)-like: a button with text and an icon. If [icon] is null, text only. */
    constructor(text: String, icon: WIcon?) : this(text) {
        if (icon != null) this.icon = icon
    }

    /** JButton(Action)-like: a button that copies the values of [action] and calls [WAction.actionPerformed] on click. */
    constructor(action: WAction) : this("") {
        this.action = action
    }

    /** The IButton view with Flyout (QI'd, since the derived class [WDropDownButton] has a different default interface). */
    private val button: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IButton)) }

    /** The flyout opened by clicking the button (Button.Flyout). */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            button.call(XamlInterop.IButton_put_Flyout, value?.flyoutBase?.ptr)
        }

    /**
     * Whether to apply the built-in accent-colored style (AccentButtonStyle).
     * Gives the emphasized look of a dialog's default button. Setting it back to false restores the default style.
     * Has no visual effect on [WDropDownButton] (not applied, since AccentButtonStyle uses the Button template, which removes the chevron).
     */
    var isAccent: Boolean = false
        set(value) {
            field = value
            updateAppearance()
        }

    /**
     * AbstractButton.isContentAreaFilled / setContentAreaFilled-like: whether to paint the button's background.
     * When false, the button uses a built-in style with a transparent background and border (SubtleButtonStyle), and is
     * lightly painted only when the pointer is over it or it is pressed (looks like a toolbar button). [isAccent] and the
     * default button ([isDefaultButton]) highlight take precedence.
     * While false, the [background] color is not painted in any state (normal, pointer over, pressed) (same as Swing's
     * contentAreaFilled). The color that was set is kept and comes back when set to true again. [foreground] is not
     * affected.
     * [WDropDownButton] does not apply SubtleButtonStyle (for the same reason as [isAccent]), so it only stops painting
     * [background], and the theme's default background and border remain.
     */
    var isContentAreaFilled: Boolean = true
        set(value) {
            field = value
            updateAppearance()
            if (customBackground != null) applyBackground()
        }

    /**
     * AbstractButton.isBorderPainted / setBorderPainted-like: whether to paint the border.
     * When false, the border thickness (Control.BorderThickness) is set to 0; setting it back to true clears that value and
     * returns to the theme's default thickness.
     * Preserved across switches of the built-in style (because local values take precedence over styles).
     */
    var isBorderPainted: Boolean = true
        set(value) {
            field = value
            if (value) {
                clearControlProperty(XamlInterop.IControlStatics_get_BorderThicknessProperty)
            } else {
                XamlStructs.putThickness(control, XamlInterop.IControl_put_BorderThickness, 0.0, 0.0, 0.0, 0.0)
            }
        }

    /**
     * JComponent.getBackground / setBackground-like: the button's background color.
     * The template's visual states (PointerOver / Pressed) are painted with theme resource colors rather than
     * Control.Background, so ButtonBackground / ButtonBackgroundPointerOver / ButtonBackgroundPressed (and the keys of the
     * same names with the Accent* / Subtle* prefixes for the built-in styles) are put into the button's own Resources, so
     * that every state is painted in shades of this color (WinUI lightweight styling). The opacity is multiplied by 0.9
     * when the pointer is over the button and by 0.8 when it is pressed (the same ratios as WinUI's accent button; the
     * color behind shows through, so it looks lighter in the light theme and darker in the dark theme).
     * The disabled appearance stays at the theme default. Reading returns the color that was set, or the native effective
     * value (the theme's default color) if none is set.
     * Not painted while [isContentAreaFilled] is false (the color is kept and painted again when set back to true).
     * Setting null removes the Resources keys and the Control.Background value and returns to the theme default.
     */
    var background: WColor?
        get() = customBackground ?: nativeBackground()
        set(value) {
            customBackground = value
            applyBackground()
        }

    /** The color set to [background]. null means unset (theme default). Kept even while [isContentAreaFilled] is false. */
    private var customBackground: WColor? = null

    /** Applies [customBackground] to Resources and Control.Background only while [isContentAreaFilled] is true. */
    private fun applyBackground() {
        val color = customBackground
        if (color == null || !isContentAreaFilled) {
            BACKGROUND_KEYS.remove(this)
            putLocalBackground(null)
            return
        }
        BACKGROUND_KEYS.put(this, color)
        // The Normal state is painted with Control.Background as already resolved by the Style's Setter, and adding
        // Resources after loading does not re-resolve it, so also set it as a local value (doc/troubleshooting.md)
        putLocalBackground(color)
    }

    /**
     * JComponent.getForeground / setForeground-like: the text color. Like [background], it is also put into the
     * lightweight styling keys (ButtonForeground / ButtonForegroundPointerOver / ButtonForegroundPressed and
     * Accent* / Subtle*); it is kept as is when the pointer is over the button and its opacity is multiplied by 0.7 when
     * pressed (a value matched to the ratio between WinUI's primary and secondary text colors).
     * The disabled appearance stays at the theme default. null returns to the theme default.
     */
    override var foreground: WColor?
        get() = super.foreground
        set(value) {
            if (value == null) FOREGROUND_KEYS.remove(this) else FOREGROUND_KEYS.put(this, value)
            super.foreground = value
        }

    /** The WFrame that has this button as its default button ([WFrame.defaultButton]). */
    private var defaultButtonOwner: WFrame? = null

    /** The Enter accelerator put into UIElement.KeyboardAccelerators only while this is the default button. */
    private var defaultButtonAccelerator: ComPtr? = null

    /** The UIElement.KeyboardAcceleratorPlacementMode before becoming the default button (restored on leaving). */
    private var placementModeBeforeDefault = XamlInterop.KeyboardAcceleratorPlacementMode_Auto

    /**
     * JButton.isDefaultButton-like: whether this is the [WFrame.defaultButton] of some frame (read-only).
     * While it is the default button, it is shown with the accent appearance (AccentButtonStyle); when it stops being one,
     * it returns to the appearance from [isAccent] / [isContentAreaFilled].
     */
    val isDefaultButton: Boolean
        get() = defaultButtonOwner != null

    /**
     * Becomes the default button of [frame] (called from the setter of [WFrame.defaultButton]). If it was the default
     * button of another WFrame, it stops being that one's default button.
     * Adds an Enter accelerator with no modifier keys (no ScopeOwner = effective in the whole window). Since Invoked is
     * not handled, WinUI calls this button's UI Automation Invoke (= click). WinUI skips accelerators of disabled buttons.
     * While it is the default button, KeyboardAcceleratorPlacementMode is set to Hidden so that the "Enter" hint is not
     * shown (hints for accelerators that you attached to this button yourself are not shown either).
     */
    internal fun becomeDefaultButton(frame: WFrame) {
        val previous = defaultButtonOwner
        if (previous === frame) return
        if (previous != null) previous.defaultButton = null // previous calls leaveDefaultButton
        defaultButtonOwner = frame
        DefaultButtons.active.add(this)
        // If left as Auto, WinUI adds the accelerator's key ("Enter") as a tooltip hint to buttons without a tooltip
        // (CKeyboardAccelerator::EnterImpl. It is decided when the accelerator enters the tree, so set Hidden before
        // adding it)
        placementModeBeforeDefault = uiElement.getInt(XamlInterop.IUIElement_get_KeyboardAcceleratorPlacementMode)
        uiElement.call(XamlInterop.IUIElement_put_KeyboardAcceleratorPlacementMode, XamlInterop.KeyboardAcceleratorPlacementMode_Hidden)
        val accelerator = createKeyboardAccelerator(VirtualKey.ENTER, emptyArray())
        val accelerators = uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        try {
            accelerators.call(FoundationInterop.IVector_Append, accelerator.ptr)
        } finally {
            accelerators.release()
        }
        defaultButtonAccelerator = accelerator
        updateAppearance()
    }

    /**
     * Stops being the default button of [frame] (called from the setter of [WFrame.defaultButton]).
     * Removes the Enter accelerator.
     */
    internal fun leaveDefaultButton(frame: WFrame) {
        if (defaultButtonOwner !== frame) return
        defaultButtonOwner = null
        DefaultButtons.active.remove(this)
        defaultButtonAccelerator?.let { accelerator ->
            removeKeyboardAccelerator(accelerator)
            accelerator.release()
        }
        defaultButtonAccelerator = null
        uiElement.call(XamlInterop.IUIElement_put_KeyboardAcceleratorPlacementMode, placementModeBeforeDefault)
        updateAppearance()
    }

    private fun removeKeyboardAccelerator(accelerator: ComPtr) {
        val accelerators = uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        try {
            Ffi.backend.withScope { scope ->
                val index = scope.allocate(4)
                val found = scope.allocate(1, 1)
                accelerators.call(FoundationInterop.IVector_IndexOf, accelerator.ptr, index, found)
                if (Ffi.backend.memory.getByte(found, 0) != 0.toByte()) {
                    accelerators.call(FoundationInterop.IVector_RemoveAt, Ffi.backend.memory.getInt(index, 0))
                }
            }
        } finally {
            accelerators.release()
        }
    }

    /**
     * The built-in style currently applied (FrameworkElement.Style). Not set again if unchanged (to avoid rebuilding the
     * template).
     */
    private var appliedAppearance = ButtonAppearance.DEFAULT

    /**
     * The built-in style determined by the appearance settings. The order of precedence is [isDefaultButton] or
     * [isAccent] (emphasis) → [isContentAreaFilled] = false (no fill) → default. When the emphasis goes away, it returns
     * to the appearance of the remaining settings.
     */
    private fun requestedAppearance(): ButtonAppearance = when {
        !usesButtonStyles -> ButtonAppearance.DEFAULT
        isDefaultButton || isAccent -> ButtonAppearance.ACCENT
        !isContentAreaFilled -> ButtonAppearance.SUBTLE
        else -> ButtonAppearance.DEFAULT
    }

    /**
     * Whether the built-in styles (AccentButtonStyle / SubtleButtonStyle) can be used. Both have TargetType Button and
     * replace the whole template, so derived classes with a different template ([WDropDownButton]) return false to keep
     * their own template.
     */
    internal open val usesButtonStyles: Boolean
        get() = true

    private fun updateAppearance() {
        val appearance = requestedAppearance()
        if (appearance == appliedAppearance) return
        appliedAppearance = appearance
        val key = appearance.styleKey
        if (key == null) {
            frameworkElement.call(XamlInterop.IFrameworkElement_put_Style, null)
        } else {
            val style = WinUiUtilities.lookupApplicationResource(key)
            try {
                frameworkElement.call(XamlInterop.IFrameworkElement_put_Style, style.ptr)
            } finally {
                style.release()
            }
        }
    }

    private companion object {
        /** Key prefixes for the default style (DefaultButtonStyle), AccentButtonStyle, and SubtleButtonStyle. */
        val STYLE_KEY_PREFIXES = listOf("", "Accent", "Subtle")

        /**
         * Keys of the Button's lightweight styling (ButtonBackground / ButtonBackgroundPointerOver / ButtonBackgroundPressed
         * and ButtonForeground* in controls/dev/CommonStyles/Button_themeresources.xaml, for the three states).
         */
        val BACKGROUND_KEYS = StateColorKeys.of("ButtonBackground", BUTTON_BACKGROUND_SHADES, STYLE_KEY_PREFIXES)
        val FOREGROUND_KEYS = StateColorKeys.of("ButtonForeground", BUTTON_FOREGROUND_SHADES, STYLE_KEY_PREFIXES)
    }
}

/**
 * The list of buttons that are the default button of an open WFrame (touch only from the UI thread).
 * A button is removed from the list when it stops being the default button and when the window closes ([forget]).
 */
internal object DefaultButtons {
    val active = LinkedHashSet<WButton>()

    /**
     * Removes the default button of the closed [frame] from the list (so as not to keep holding buttons of closed
     * windows, nor use them for dispatching Enter).
     */
    fun forget(frame: WFrame) {
        frame.defaultButton?.let { active.remove(it) }
    }

    /**
     * The default button in the same XamlRoot (the same window) as [element] (any view of a UIElement). null if there is
     * none.
     * Used to find the button to press on Enter in a single-line [WTextField].
     */
    fun inRootOf(element: ComPtr): WButton? {
        if (active.isEmpty()) return null
        val root = xamlRootOf(element) ?: return null
        try {
            return active.firstOrNull { button ->
                val buttonRoot = xamlRootOf(button.uiElement) ?: return@firstOrNull false
                try {
                    Xaml.sameObject(root, buttonRoot)
                } finally {
                    buttonRoot.release()
                }
            }
        } finally {
            root.release()
        }
    }

    private fun xamlRootOf(element: ComPtr): ComPtr? {
        val ui = element.queryInterface(XamlInterop.IID_IUIElement)
        return try {
            ui.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)
        } finally {
            ui.release()
        }
    }
}

/**
 * The built-in styles that determine a WButton's appearance (FrameworkElement.Style).
 * [styleKey] is a key in Application.Resources (XamlControlsResources); null means no Style is set (the implicit style).
 */
internal enum class ButtonAppearance(val styleKey: String?) {
    /** The default button appearance (the implicit style based on DefaultButtonStyle). */
    DEFAULT(null),

    /** Accent colors (AccentButtonStyle). */
    ACCENT("AccentButtonStyle"),

    /** A button that paints neither its background nor its border (SubtleButtonStyle). */
    SUBTLE("SubtleButtonStyle"),
}
