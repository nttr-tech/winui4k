package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * Common base for WinUI 3's Control-derived controls (Button, TextBox, ...).
 * Non-Control components such as StackPanel stay as plain [WComponent].
 */
abstract class WControl internal constructor(inspectable: ComPtr) : WComponent(inspectable) {
    internal val control: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IControl)) }

    /** setEnabled-like (Control.IsEnabled). */
    var isEnabled: Boolean
        get() = control.getBool(XamlInterop.IControl_get_IsEnabled)
        set(value) = control.putBool(XamlInterop.IControl_put_IsEnabled, value)

    /** The content's horizontal alignment (Control.HorizontalContentAlignment). Defaults to CENTER for e.g. Button. */
    var horizontalContentAlignment: HorizontalAlignment
        get() = HorizontalAlignment.of(control.getInt(XamlInterop.IControl_get_HorizontalContentAlignment))
        set(value) = control.call(XamlInterop.IControl_put_HorizontalContentAlignment, value.native)

    /** The content's vertical alignment (Control.VerticalContentAlignment). Defaults to CENTER for e.g. Button. */
    var verticalContentAlignment: VerticalAlignment
        get() = VerticalAlignment.of(control.getInt(XamlInterop.IControl_get_VerticalContentAlignment))
        set(value) = control.call(XamlInterop.IControl_put_VerticalContentAlignment, value.native)

    /** A uniform corner radius on all four corners (Control.CornerRadius). Half the height makes it a pill shape. */
    var cornerRadius: Double = 0.0
        set(value) {
            field = value
            XamlStructs.putCornerRadius(control, XamlInterop.IControl_put_CornerRadius, value)
        }

    /**
     * A uniform inner padding on all four sides (Control.Padding). Use [setPadding] to set each side individually.
     * Reading returns the value set through this library, or 0 if unset. Padding applied by the theme (such as 11, 5, 11, 6 of a WButton placed in a window)
     * is not a setting, so it does not show up. When set with [setPadding] or an empty frame of [border], it is that value if all four sides are equal,
     * otherwise 0, since they cannot be expressed as one number. A [border] without an empty frame or a null [border] clears the Padding setting, so it returns to 0.
     * Read the actual padding on the four sides (the native effective value including the theme default) with [insets].
     */
    var padding: Double
        get() = paddingValue
        set(value) = setPadding(value, value, value, value)

    /** The last set inner padding returned by [padding] (0 if the four sides are not equal, 0 once the setting is cleared). */
    private var paddingValue: Double = 0.0

    /** Sets the inner padding on each side individually (Control.Padding). The value read from [padding] changes too. */
    fun setPadding(left: Double, top: Double, right: Double, bottom: Double) {
        XamlStructs.putThickness(control, XamlInterop.IControl_put_Padding, left, top, right, bottom)
        paddingValue = uniformThickness(left, top, right, bottom)
        revalidate() // As with AbstractButton.setMargin, the size changes, so lay out again
    }

    /**
     * Sets the inner padding like AbstractButton.setMargin(Insets) / JTextComponent.setMargin(Insets) with [setPadding]
     * (called from each component's public setMargin(WInsets)). [insets] is top, left, bottom, right, as with Swing's Insets.
     */
    internal fun setPaddingFromInsets(insets: WInsets) = setPadding(insets.left, insets.top, insets.right, insets.bottom)

    /** Clears the Control.Padding setting and returns to the theme default ([padding] returns to 0). */
    private fun clearPadding() {
        clearControlProperty(XamlInterop.IControlStatics_get_PaddingProperty)
        paddingValue = 0.0
    }

    /**
     * JComponent.getInsets-like: the width inside the border (DIP). The sum of the border thickness (Control.BorderThickness) and the inner padding (Control.Padding),
     * both read as native effective values (the theme default if not set; the theme applies when it is put in a window, so 0 before that).
     * As Swing's JButton margin is included in the border, [padding] and the empty border of [border] are included here too.
     */
    val insets: WInsets
        get() {
            val padding = XamlStructs.getThickness(control, XamlInterop.IControl_get_Padding)
            val border = XamlStructs.getThickness(control, XamlInterop.IControl_get_BorderThickness)
            // Thickness is in left, top, right, bottom order
            return WInsets(
                top = padding[1] + border[1],
                left = padding[0] + border[0],
                bottom = padding[3] + border[3],
                right = padding[2] + border[2],
            )
        }

    /**
     * JComponent.getFont / setFont-like: the font (Control.FontFamily / FontWeight / FontStyle / FontSize).
     * Reading returns the native effective value, which is the theme's default font before it is set (a weight of 600 or
     * more is [WFont.BOLD]).
     * Setting null clears the four properties and returns to the theme default (DependencyObject.ClearValue).
     */
    var font: WFont?
        get() = FontProperties.read(control, FontProperties.CONTROL)
        set(value) {
            FontProperties.write(control, dependencyObject, FontProperties.CONTROL, value)
            fontChanged()
            revalidate() // As with JComponent.setFont, the text size changes, so lay out again
        }

    /** Called after [font] is set (so that [WTextField.columns], which derives the width from the font, remeasures). */
    internal open fun fontChanged() {
        // Does nothing by default
    }

    /**
     * JComponent.getForeground / setForeground-like: the text color (Control.Foreground). Converted to a SolidColorBrush.
     * Reading returns the native effective value, which is the theme's default color before it is set (null if it is not a
     * solid color brush).
     * Setting null clears the value and returns to the theme's default color (DependencyObject.ClearValue).
     * A button's [WButton.foreground] also changes the colors of the pointer-over and pressed visual states.
     *
     * The templates of text fields ([WTextField] / [WPasswordField] / [WTextPane] / [WAutoSuggestBox] / [WSpinner]) paint the text
     * with theme resource colors instead of Control.Foreground when the pointer is over them and when they have focus. As with [background],
     * TextControlForeground / TextControlForegroundPointerOver / TextControlForegroundFocused are put into the control's own Resources
     * with the same color so these states use this color too (the text color of Swing's JTextField does not change with focus). The disabled look stays the theme default.
     */
    open var foreground: WColor?
        get() {
            val brush = control.getPtrOrNull(XamlInterop.IControl_get_Foreground) ?: return null
            return try {
                colorOf(brush)
            } finally {
                brush.release()
            }
        }
        set(value) {
            if (isTextInput) {
                if (value == null) TEXT_CONTROL_FOREGROUND_KEYS.remove(this) else TEXT_CONTROL_FOREGROUND_KEYS.put(this, value)
            }
            if (value == null) {
                clearControlProperty(XamlInterop.IControlStatics_get_ForegroundProperty)
                return
            }
            val brush = value.createBrush()
            try {
                control.call(XamlInterop.IControl_put_Foreground, brush.ptr)
            } finally {
                brush.release()
            }
        }

    /**
     * JComponent.getBackground / setBackground-like: the background color (Control.Background). Converted to a SolidColorBrush and passed.
     * Reading returns the set color, or if unset the native effective value (the theme's default color; null if it is not a solid color brush).
     * Setting null clears the setting and returns to the theme default (DependencyObject.ClearValue; in Swing null means "inherit the parent's color").
     *
     * Many WinUI templates paint the background with the pointer over it or with the focus not with Control.Background but with the color of
     * a theme resource. Text fields ([WTextField] / [WPasswordField] / [WTextPane] / [WAutoSuggestBox] / [WSpinner]) put
     * TextControlBackground / TextControlBackgroundPointerOver / TextControlBackgroundFocused with the same color into the control's own Resources
     * (WinUI lightweight styling), so they are painted with this color also with the pointer over them and with the focus (the background color of Swing's JTextField
     * does not change with the focus). The disabled look stays the theme default.
     * Other controls apply it only in the normal state, and colors such as with the pointer over them stay the theme default.
     * Buttons ([WButton.background] / [WToggleButton.background]) decide the colors per visual state themselves.
     */
    open var background: WColor?
        get() = customBackground ?: nativeBackground()
        set(value) {
            customBackground = value
            val keys = backgroundStateKeys
            if (value == null) keys.remove(this) else keys.put(this, value)
            putLocalBackground(value)
        }

    /** The color set to [background]. null means unset (the theme default). */
    private var customBackground: WColor? = null

    /** The theme resource keys of the backgrounds per visual state into which the [background] color is put. */
    internal open val backgroundStateKeys: StateColorKeys
        get() = if (isTextInput) TEXT_CONTROL_BACKGROUND_KEYS else StateColorKeys.NONE

    /** Whether this is a text field whose template decides the color of each visual state with TextControl* theme resources. */
    private val isTextInput: Boolean
        get() = this is WTextField || this is WPasswordField || this is WTextPane || this is WAutoSuggestBox || this is WSpinner

    /** The border last set to [border]. */
    private var borderValue: WControlBorder? = null

    /**
     * JComponent.getBorder / setBorder-style: the control's border (created with [WBorderFactory]). Returns the border as
     * set. A new border replaces the previous one, and nothing of the previous border remains.
     *
     * A line border ([WLineBorder]) becomes Control.BorderBrush and BorderThickness, and an empty border ([WEmptyBorder])
     * becomes Padding (a compound border sets both; the order of outside and inside is not distinguished; see
     * [WCompoundBorder]). Without a line border no border line is drawn (thickness 0), and without an empty border Padding
     * stays at the theme default. Setting null clears all three and returns to the theme defaults
     * (DependencyObject.ClearValue; in Swing null means "no border", but the border line is part of the look of WinUI
     * controls, so this returns to the default).
     *
     * [padding] and [WButton.isBorderPainted] also write the same Padding / BorderThickness, so whichever is set last takes
     * effect (reading [border] afterwards still returns the border as set). The template draws the pointer-over and pressed
     * states of buttons with the theme's border color, so the line border's color only affects the normal state.
     * The same borders can also be set on [WLabel] / [WPanel] / [WLayoutPanel] ([WLabel.border] etc.).
     *
     * @throws IllegalArgumentException for a combination containing two or more line borders (in which case neither the
     * control nor [border] changes)
     */
    var border: WControlBorder?
        get() = borderValue
        set(value) {
            // Validate first, so that nothing is changed on failure
            val flat = value?.let { FlatBorder.of(it) }
            borderValue = value
            if (flat == null) {
                clearControlProperty(XamlInterop.IControlStatics_get_BorderBrushProperty)
                clearControlProperty(XamlInterop.IControlStatics_get_BorderThicknessProperty)
                clearPadding()
                revalidate() // As with JComponent.setBorder, the size changes, so lay out again
                return
            }
            applyLine(flat.line)
            val padding = flat.padding
            if (padding == null) {
                clearPadding()
            } else {
                setPadding(padding.left, padding.top, padding.right, padding.bottom)
            }
            revalidate()
        }

    private fun applyLine(line: WLineBorder?) {
        if (line == null) {
            clearControlProperty(XamlInterop.IControlStatics_get_BorderBrushProperty)
            XamlStructs.putThickness(control, XamlInterop.IControl_put_BorderThickness, 0.0, 0.0, 0.0, 0.0)
            return
        }
        val brush = line.color.createBrush()
        try {
            control.call(XamlInterop.IControl_put_BorderBrush, brush.ptr)
        } finally {
            brush.release()
        }
        val thickness = line.thickness.toDouble()
        XamlStructs.putThickness(control, XamlInterop.IControl_put_BorderThickness, thickness, thickness, thickness, thickness)
    }

    /** Clears the local value of a Control dependency property ([staticsSlot] is get_XxxProperty of IControlStatics). */
    internal fun clearControlProperty(staticsSlot: Int) {
        clearDependencyProperty(dependencyObject, XamlInterop.CLS_Control, XamlInterop.IID_IControlStatics, staticsSlot)
    }
}

/**
 * The keys the templates of text fields (TextBox, PasswordBox, RichEditBox, and AutoSuggestBox and NumberBox, which contain them) look up for the backgrounds in the normal,
 * pointer-over and focused states (TextControlBackground* in controls/dev/CommonStyles/TextBox_themeresources.xaml etc.).
 * The disabled state (TextControlBackgroundDisabled) is not put in.
 */
private val TEXT_CONTROL_BACKGROUND_KEYS = StateColorKeys.uniform(
    listOf("TextControlBackground", "TextControlBackgroundPointerOver", "TextControlBackgroundFocused"),
)

/** The keys the text field template looks up for the text color in the normal, pointer-over and focused states (the disabled state is not included). */
private val TEXT_CONTROL_FOREGROUND_KEYS = StateColorKeys.uniform(
    listOf("TextControlForeground", "TextControlForegroundPointerOver", "TextControlForegroundFocused"),
)
