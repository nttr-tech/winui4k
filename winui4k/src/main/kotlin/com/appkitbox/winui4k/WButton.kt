package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JButton-like: WinUI 3's Button.
 *
 * Covers the full surface Button defines (IButton + Primitives.IButtonBase +
 * ContentControl.Content): [text] / [content], [clickMode], [isPressed], [isPointerOver],
 * [addActionListener] / [removeActionListener] (Click), [command] / [commandParameter], [flyout].
 */
class WButton @JvmOverloads constructor(text: String = "") : WButtonBase(
    Activation.composeDefault(XamlInterop.CLS_Button, XamlInterop.IID_IButtonFactory), // default interface = IButton
) {
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
     * The built-in style currently applied (FrameworkElement.Style). Not set again if unchanged (to avoid rebuilding the
     * template).
     */
    private var appliedAppearance = ButtonAppearance.DEFAULT

    /** The built-in style determined by the appearance settings ([isAccent] and so on). */
    private fun requestedAppearance(): ButtonAppearance = if (isAccent) ButtonAppearance.ACCENT else ButtonAppearance.DEFAULT

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
