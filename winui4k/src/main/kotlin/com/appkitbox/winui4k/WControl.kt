package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.getString
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
        get() {
            val family = control.getPtrOrNull(XamlInterop.IControl_get_FontFamily)
            val name = try {
                family?.getString(XamlInterop.IFontFamily_get_Source).orEmpty()
            } finally {
                family?.release()
            }
            val bold = XamlStructs.getFontWeight(control, XamlInterop.IControl_get_FontWeight) >= SEMI_BOLD_WEIGHT
            val italic = control.getInt(XamlInterop.IControl_get_FontStyle) == FONT_STYLE_ITALIC
            val style = (if (bold) WFont.BOLD else WFont.PLAIN) or (if (italic) WFont.ITALIC else WFont.PLAIN)
            return WFont(name, style, control.getDouble(XamlInterop.IControl_get_FontSize))
        }
        set(value) {
            if (value == null) {
                clearControlProperty(XamlInterop.IControlStatics_get_FontFamilyProperty)
                clearControlProperty(XamlInterop.IControlStatics_get_FontSizeProperty)
                clearControlProperty(XamlInterop.IControlStatics_get_FontWeightProperty)
                clearControlProperty(XamlInterop.IControlStatics_get_FontStyleProperty)
                return
            }
            val family = createFontFamily(value.name)
            try {
                control.call(XamlInterop.IControl_put_FontFamily, family.ptr)
            } finally {
                family.release()
            }
            control.call(XamlInterop.IControl_put_FontSize, value.size2D)
            XamlStructs.putFontWeight(
                control,
                XamlInterop.IControl_put_FontWeight,
                if (value.isBold) BOLD_WEIGHT else NORMAL_WEIGHT,
            )
            control.call(XamlInterop.IControl_put_FontStyle, if (value.isItalic) FONT_STYLE_ITALIC else FONT_STYLE_NORMAL)
        }

    /**
     * JComponent.getForeground / setForeground-like: the text color (Control.Foreground). Converted to a SolidColorBrush.
     * Reading returns the native effective value, which is the theme's default color before it is set (null if it is not a
     * solid color brush).
     * Setting null clears the value and returns to the theme's default color (DependencyObject.ClearValue).
     */
    var foreground: WColor?
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

    /** Clears the local value of a Control dependency property ([staticsSlot] is get_XxxProperty of IControlStatics). */
    internal fun clearControlProperty(staticsSlot: Int) {
        val statics = Activation.factory(XamlInterop.CLS_Control, XamlInterop.IID_IControlStatics)
        try {
            val property = statics.getPtr(staticsSlot)
            try {
                dependencyObject.call(XamlInterop.IDependencyObject_ClearValue, property.ptr)
            } finally {
                property.release()
            }
        } finally {
            statics.release()
        }
    }
}

// FontWeight weights and Windows.UI.Text.FontStyle values (Normal=0, Italic=2; extracted from the winmd)
private const val NORMAL_WEIGHT = 400
private const val SEMI_BOLD_WEIGHT = 600
private const val BOLD_WEIGHT = 700
private const val FONT_STYLE_NORMAL = 0
private const val FONT_STYLE_ITALIC = 2
