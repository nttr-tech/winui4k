package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * ABI values, per element kind, for elements with the four font properties (FontFamily / FontSize / FontWeight / FontStyle).
 * Control, TextBlock and RichTextBlock have properties of the same names on separate interfaces.
 * [staticsClass] / [staticsIid] are the statics that return the dependency properties passed to ClearValue.
 */
@Suppress("LongParameterList") // The arguments map 1:1 to the vtable slots of each interface
internal class FontSlots(
    val getSize: Int,
    val putSize: Int,
    val getFamily: Int,
    val putFamily: Int,
    val getWeight: Int,
    val putWeight: Int,
    val getStyle: Int,
    val putStyle: Int,
    val staticsClass: String,
    val staticsIid: String,
    val sizeProperty: Int,
    val familyProperty: Int,
    val weightProperty: Int,
    val styleProperty: Int,
)

/**
 * JComponent.getFont / setFont-like reading and writing of [WFont]. Shared by [WControl.font] (Control), [WLabel.font]
 * (TextBlock) and [WRichTextBlock.font] (RichTextBlock).
 *
 * Reads return the native effective value, which is the theme's default font even before setting (a weight of 600 or
 * more is [WFont.BOLD]).
 * Setting null clears the four properties and restores the defaults (DependencyObject.ClearValue; it reverts to the
 * value inherited from the parent).
 */
internal object FontProperties {
    val CONTROL = FontSlots(
        XamlInterop.IControl_get_FontSize,
        XamlInterop.IControl_put_FontSize,
        XamlInterop.IControl_get_FontFamily,
        XamlInterop.IControl_put_FontFamily,
        XamlInterop.IControl_get_FontWeight,
        XamlInterop.IControl_put_FontWeight,
        XamlInterop.IControl_get_FontStyle,
        XamlInterop.IControl_put_FontStyle,
        XamlInterop.CLS_Control,
        XamlInterop.IID_IControlStatics,
        XamlInterop.IControlStatics_get_FontSizeProperty,
        XamlInterop.IControlStatics_get_FontFamilyProperty,
        XamlInterop.IControlStatics_get_FontWeightProperty,
        XamlInterop.IControlStatics_get_FontStyleProperty,
    )

    val TEXT_BLOCK = FontSlots(
        XamlInterop.ITextBlock_get_FontSize,
        XamlInterop.ITextBlock_put_FontSize,
        XamlInterop.ITextBlock_get_FontFamily,
        XamlInterop.ITextBlock_put_FontFamily,
        XamlInterop.ITextBlock_get_FontWeight,
        XamlInterop.ITextBlock_put_FontWeight,
        XamlInterop.ITextBlock_get_FontStyle,
        XamlInterop.ITextBlock_put_FontStyle,
        XamlInterop.CLS_TextBlock,
        XamlInterop.IID_ITextBlockStatics,
        XamlInterop.ITextBlockStatics_get_FontSizeProperty,
        XamlInterop.ITextBlockStatics_get_FontFamilyProperty,
        XamlInterop.ITextBlockStatics_get_FontWeightProperty,
        XamlInterop.ITextBlockStatics_get_FontStyleProperty,
    )

    val RICH_TEXT_BLOCK = FontSlots(
        XamlInterop.IRichTextBlock_get_FontSize,
        XamlInterop.IRichTextBlock_put_FontSize,
        XamlInterop.IRichTextBlock_get_FontFamily,
        XamlInterop.IRichTextBlock_put_FontFamily,
        XamlInterop.IRichTextBlock_get_FontWeight,
        XamlInterop.IRichTextBlock_put_FontWeight,
        XamlInterop.IRichTextBlock_get_FontStyle,
        XamlInterop.IRichTextBlock_put_FontStyle,
        XamlInterop.CLS_RichTextBlock,
        XamlInterop.IID_IRichTextBlockStatics,
        XamlInterop.IRichTextBlockStatics_get_FontSizeProperty,
        XamlInterop.IRichTextBlockStatics_get_FontFamilyProperty,
        XamlInterop.IRichTextBlockStatics_get_FontWeightProperty,
        XamlInterop.IRichTextBlockStatics_get_FontStyleProperty,
    )

    /** The effective font of [view] (an interface of the kind given by [slots]). */
    fun read(view: ComPtr, slots: FontSlots): WFont {
        val bold = XamlStructs.getFontWeight(view, slots.getWeight) >= SEMI_BOLD_WEIGHT
        val italic = view.getInt(slots.getStyle) == FONT_STYLE_ITALIC
        val style = (if (bold) WFont.BOLD else WFont.PLAIN) or (if (italic) WFont.ITALIC else WFont.PLAIN)
        return WFont(readFamily(view, slots), style, view.getDouble(slots.getSize))
    }

    /** The font family name of [view] (FontFamily.Source). An empty string if unset. */
    fun readFamily(view: ComPtr, slots: FontSlots): String {
        val family = view.getPtrOrNull(slots.getFamily) ?: return ""
        return try {
            family.getString(XamlInterop.IFontFamily_get_Source)
        } finally {
            family.release()
        }
    }

    /** Sets the font family of [view] to [name]. */
    fun writeFamily(view: ComPtr, slots: FontSlots, name: String) {
        val family = createFontFamily(name)
        try {
            view.call(slots.putFamily, family.ptr)
        } finally {
            family.release()
        }
    }

    /**
     * Sets the font of [view] to [value]. If null, clears the local values of the four properties of [dependencyObject]
     * (the IDependencyObject of the same element).
     */
    fun write(view: ComPtr, dependencyObject: ComPtr, slots: FontSlots, value: WFont?) {
        if (value == null) {
            for (property in listOf(slots.familyProperty, slots.sizeProperty, slots.weightProperty, slots.styleProperty)) {
                clearDependencyProperty(dependencyObject, slots.staticsClass, slots.staticsIid, property)
            }
            return
        }
        writeFamily(view, slots, value.name)
        view.call(slots.putSize, value.sizeDip)
        XamlStructs.putFontWeight(view, slots.putWeight, if (value.isBold) BOLD_WEIGHT else NORMAL_WEIGHT)
        view.call(slots.putStyle, if (value.isItalic) FONT_STYLE_ITALIC else FONT_STYLE_NORMAL)
    }

    // FontWeight weights and Windows.UI.Text.FontStyle values (Normal=0, Italic=2; extracted from winmd)
    const val NORMAL_WEIGHT = 400
    const val SEMI_BOLD_WEIGHT = 600
    const val BOLD_WEIGHT = 700
    const val FONT_STYLE_NORMAL = 0
    const val FONT_STYLE_ITALIC = 2
}

/**
 * Clears the local value of a dependency property of [dependencyObject] (IDependencyObject) (DependencyObject.ClearValue).
 * The property is looked up with [staticsSlot] (get_XxxProperty) of the statics ([staticsIid]) of [staticsClass].
 */
internal fun clearDependencyProperty(dependencyObject: ComPtr, staticsClass: String, staticsIid: String, staticsSlot: Int) {
    val statics = Activation.factory(staticsClass, staticsIid)
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
