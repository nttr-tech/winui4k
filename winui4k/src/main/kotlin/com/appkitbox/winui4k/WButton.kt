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
class WButton @JvmOverloads constructor(text: String = "") : WButtonBase(
    Activation.composeDefault(XamlInterop.CLS_Button, XamlInterop.IID_IButtonFactory), // default interface = IButton
) {
    /** JButton(Icon)-like: a button with only an icon. */
    constructor(icon: WIcon) : this("") {
        this.icon = icon
    }

    /** JButton(String, Icon)-like: a button with text and an icon. If [icon] is null, text only. */
    constructor(text: String, icon: WIcon?) : this(text) {
        if (icon != null) this.icon = icon
    }

    /**
     * AbstractButton.getIcon / setIcon-like: the icon shown alongside the text. null (the default) means no icon.
     * WinUI's Button has no icon property, so the Content is made a StackPanel that arranges the icon (IconElement) and
     * the text (TextBlock). If the text is empty, only the icon becomes the Content. [text] keeps returning the arranged
     * string.
     * Putting a component into [content] removes the icon (back to null), and setting an icon later removes [content].
     * While the icon and text are arranged together, the string is put into the screen reader name ([accessibleName])
     * (left as is if the user has set a different name).
     */
    var icon: WIcon?
        get() = labelIcon
        set(value) = changeLabelIcon(value)

    /**
     * AbstractButton.setHorizontalTextPosition-like: the position of the text relative to the icon. The default is
     * [TextPosition.TRAILING] (text after the icon). [TextPosition.TOP] / [TextPosition.BOTTOM] arrange them vertically.
     */
    var horizontalTextPosition: TextPosition
        get() = labelTextPosition
        set(value) = changeLabelTextPosition(value)

    /**
     * AbstractButton.setIconTextGap-like: the gap between the icon and the text (DIP; StackPanel.Spacing). The default is
     * 4, the same as Swing.
     * Negative values, NaN, and infinity throw [IllegalArgumentException] (unlike Swing, they cannot be overlapped).
     */
    var iconTextGap: Double
        get() = labelIconTextGap
        set(value) = changeLabelIconTextGap(value)

    /** The flyout opened by clicking the button (Button.Flyout). */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.IButton_put_Flyout, value?.flyoutBase?.ptr)
        }

    /**
     * Whether to apply the built-in accent-colored style (AccentButtonStyle).
     * Gives the emphasized look of a dialog's default button. Setting it back to false restores the default style.
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
     */
    var isContentAreaFilled: Boolean = true
        set(value) {
            field = value
            updateAppearance()
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
     * The disabled appearance stays at the theme default. Reading returns the native effective value, which is the
     * theme's default color before it is set.
     * Setting null removes the Resources keys and the Control.Background value and returns to the theme default.
     */
    var background: WColor?
        get() {
            val brush = control.getPtrOrNull(XamlInterop.IControl_get_Background) ?: return null
            return try {
                colorOf(brush)
            } finally {
                brush.release()
            }
        }
        set(value) {
            if (value == null) {
                removeStateResources(BACKGROUND_KEY)
                clearControlProperty(XamlInterop.IControlStatics_get_BackgroundProperty)
                return
            }
            putStateResources(
                BACKGROUND_KEY,
                value,
                value.withAlphaScaled(POINTER_OVER_BACKGROUND_OPACITY),
                value.withAlphaScaled(PRESSED_BACKGROUND_OPACITY),
            )
            // The Normal state is painted with Control.Background as already resolved by the Style's Setter, and adding
            // Resources after loading does not re-resolve it, so also set it as a local value (doc/troubleshooting.md)
            val brush = value.createBrush()
            try {
                control.call(XamlInterop.IControl_put_Background, brush.ptr)
            } finally {
                brush.release()
            }
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
            if (value == null) {
                removeStateResources(FOREGROUND_KEY)
            } else {
                putStateResources(FOREGROUND_KEY, value, value, value.withAlphaScaled(PRESSED_FOREGROUND_OPACITY))
            }
            super.foreground = value
        }

    /**
     * Puts the [baseKey] (Normal) / PointerOver / Pressed theme resources of the three built-in styles into the button's
     * own Resources.
     */
    private fun putStateResources(baseKey: String, normal: WColor, pointerOver: WColor, pressed: WColor) {
        withResources { resources ->
            for (prefix in STYLE_KEY_PREFIXES) {
                resources.put("$prefix$baseKey", normal)
                resources.put("$prefix$baseKey$POINTER_OVER", pointerOver)
                resources.put("$prefix$baseKey$PRESSED", pressed)
            }
        }
    }

    /** Removes the keys put by [putStateResources] (returning to the theme's default colors). */
    private fun removeStateResources(baseKey: String) {
        withResources { resources ->
            for (prefix in STYLE_KEY_PREFIXES) {
                for (state in listOf("", POINTER_OVER, PRESSED)) resources.remove("$prefix$baseKey$state")
            }
        }
    }

    private inline fun withResources(block: (ElementResources) -> Unit) {
        val dictionary = frameworkElement.getPtr(XamlInterop.IFrameworkElement_get_Resources)
        val map = try {
            dictionary.queryInterface(FoundationInterop.IID_IMap_Object_Object)
        } finally {
            dictionary.release()
        }
        try {
            block(ElementResources(map))
        } finally {
            map.release()
        }
    }

    /** The WFrame that has this button as its default button ([WFrame.defaultButton]). */
    private var defaultButtonOwner: WFrame? = null

    /** The Enter accelerator put into UIElement.KeyboardAccelerators only while this is the default button. */
    private var defaultButtonAccelerator: ComPtr? = null

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
     */
    internal fun becomeDefaultButton(frame: WFrame) {
        val previous = defaultButtonOwner
        if (previous === frame) return
        if (previous != null) previous.defaultButton = null // previous calls leaveDefaultButton
        defaultButtonOwner = frame
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
        defaultButtonAccelerator?.let { accelerator ->
            removeKeyboardAccelerator(accelerator)
            accelerator.release()
        }
        defaultButtonAccelerator = null
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
        isDefaultButton || isAccent -> ButtonAppearance.ACCENT
        !isContentAreaFilled -> ButtonAppearance.SUBTLE
        else -> ButtonAppearance.DEFAULT
    }

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

    init {
        if (text.isNotEmpty()) this.text = text
    }

    private companion object {
        /** Keys of Button's lightweight styling (controls/dev/CommonStyles/Button_themeresources.xaml). */
        const val BACKGROUND_KEY = "ButtonBackground"
        const val FOREGROUND_KEY = "ButtonForeground"
        const val POINTER_OVER = "PointerOver"
        const val PRESSED = "Pressed"

        /** Key prefixes for the default style (DefaultButtonStyle), AccentButtonStyle, and SubtleButtonStyle. */
        val STYLE_KEY_PREFIXES = listOf("", "Accent", "Subtle")

        // Opacity multipliers for pointer-over and pressed. The background uses 0.9 / 0.8, the same as
        // AccentFillColorSecondary / Tertiary (Common_themeresources_any.xaml), and the text matches
        // TextFillColorSecondary / Primary (about 0.69 in the light theme)
        const val POINTER_OVER_BACKGROUND_OPACITY = 0.9
        const val PRESSED_BACKGROUND_OPACITY = 0.8
        const val PRESSED_FOREGROUND_OPACITY = 0.7
    }
}

/** The color with only its opacity multiplied by [factor] (rounded). */
private fun WColor.withAlphaScaled(factor: Double): WColor = WColor(red, green, blue, Math.round(alpha * factor).toInt())

/** Puts color brushes into and removes them from FrameworkElement.Resources (IMap<Object, Object>). */
private class ElementResources(private val map: ComPtr) {
    /** Puts a SolidColorBrush of [color] at [key] (replacing any existing one). */
    fun put(key: String, color: WColor) {
        val boxedKey = PropertyValues.boxString(key)
        val brush = color.createBrush()
        try {
            Ffi.backend.withScope { scope ->
                val replaced = scope.allocate(1, 1)
                map.call(FoundationInterop.IMap_Insert, boxedKey.ptr, brush.ptr, replaced)
            }
        } finally {
            brush.release()
            boxedKey.release()
        }
    }

    /** Removes [key] if present (IMap.Remove fails for a missing key, so check with HasKey). */
    fun remove(key: String) {
        val boxedKey = PropertyValues.boxString(key)
        try {
            val present = Ffi.backend.withScope { scope ->
                val out = scope.allocate(1, 1)
                map.call(FoundationInterop.IMap_HasKey, boxedKey.ptr, out)
                Ffi.backend.memory.getByte(out, 0) != 0.toByte()
            }
            if (present) map.call(FoundationInterop.IMap_Remove, boxedKey.ptr)
        } finally {
            boxedKey.release()
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
