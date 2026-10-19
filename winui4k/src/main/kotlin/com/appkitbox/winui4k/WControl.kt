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

    /** A uniform inner padding on all four sides (Control.Padding). Use [setPadding] to set each side individually. */
    var padding: Double = 0.0
        set(value) {
            field = value
            setPadding(value, value, value, value)
        }

    /** Sets the inner padding on each side individually (Control.Padding). */
    fun setPadding(left: Double, top: Double, right: Double, bottom: Double) {
        XamlStructs.putThickness(control, XamlInterop.IControl_put_Padding, left, top, right, bottom)
    }

    /**
     * JComponent.getFont / setFont-like: the font (Control.FontFamily / FontWeight / FontStyle / FontSize).
     * Reading returns the native effective value, which is the theme's default font before it is set (a weight of 600 or
     * more is [WFont.BOLD]).
     * Setting null clears the four properties and returns to the theme default (DependencyObject.ClearValue).
     */
    var font: WFont?
        get() = FontProperties.read(control, FontProperties.CONTROL)
        set(value) = FontProperties.write(control, dependencyObject, FontProperties.CONTROL, value)

    /**
     * JComponent.getForeground / setForeground-like: the text color (Control.Foreground). Converted to a SolidColorBrush.
     * Reading returns the native effective value, which is the theme's default color before it is set (null if it is not a
     * solid color brush).
     * Setting null clears the value and returns to the theme's default color (DependencyObject.ClearValue).
     * A button's [WButton.foreground] also changes the colors of the pointer-over and pressed visual states.
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
     * Only [WControl] has a border; it cannot be set on [WLabel] or [WPanel] (wrap them in a [WBorder] with a border).
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
                clearControlProperty(XamlInterop.IControlStatics_get_PaddingProperty)
                return
            }
            applyLine(flat.line)
            val padding = flat.padding
            if (padding == null) {
                clearControlProperty(XamlInterop.IControlStatics_get_PaddingProperty)
            } else {
                setPadding(padding.left, padding.top, padding.right, padding.bottom)
            }
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
