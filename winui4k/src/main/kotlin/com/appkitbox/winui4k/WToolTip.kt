package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The values of Microsoft.UI.Xaml.Controls.Primitives.PlacementMode that ToolTip uses.
 * Values extracted from the winmd (Bottom=2, Right=4, Mouse=7, Left=9, Top=10).
 */
enum class ToolTipPlacement(internal val native: Int) {
    /** Above the target. */
    TOP(10),

    /** Below the target. */
    BOTTOM(2),

    /** To the left of the target. */
    LEFT(9),

    /** To the right of the target. */
    RIGHT(4),

    /** Near the mouse cursor. */
    MOUSE(7),
    ;

    internal companion object {
        fun of(native: Int): ToolTipPlacement = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's ToolTip (a small hint that pops up on hover). A plain string hint is covered by
 * [WComponent.toolTip]; use this class when you need to specify placement or show content other
 * than a string, and attach it to a target via [WComponent.setToolTip].
 */
class WToolTip : WControl(
    Activation.composeDefault(XamlInterop.CLS_ToolTip, XamlInterop.IID_IToolTipFactory), // default interface = IToolTip
) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    private var contentComponent: WComponent? = null

    /** The string shown as the hint (ContentControl.Content). Mutually exclusive with [content]. */
    var text: String = ""
        set(value) {
            field = value
            contentComponent = null
            val boxed = PropertyValues.boxString(value)
            contentControl.call(XamlInterop.IContentControl_put_Content, boxed.ptr)
            boxed.release()
        }

    /** The Content to show when you need more than a string (ContentControl.Content). Mutually exclusive with [text]. */
    var content: WComponent?
        get() = contentComponent
        set(value) {
            contentComponent = value
            contentControl.call(XamlInterop.IContentControl_put_Content, value?.uiElement?.ptr)
        }

    /** Where the hint is shown (ToolTip.Placement). */
    var placement: ToolTipPlacement
        get() = ToolTipPlacement.of(inspectable.getInt(XamlInterop.IToolTip_get_Placement))
        set(value) = inspectable.call(XamlInterop.IToolTip_put_Placement, value.native)
}

/** The ToolTipService statics (SetToolTip / SetPlacement). It's agile, so it's reused. */
internal val toolTipServiceStatics: ComPtr by lazy {
    Activation.factory(XamlInterop.CLS_ToolTipService, XamlInterop.IID_IToolTipServiceStatics)
}
