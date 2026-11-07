package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference

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
        @JvmSynthetic
        fun of(native: Int): ToolTipPlacement = entries.first { it.native == native }
    }
}

/**
 * JToolTip-like: WinUI 3's ToolTip (a small hint that pops up on hover). A plain string hint is covered by
 * [WComponent.toolTipText]; use this class when you need to specify placement or show content other
 * Use this class when you want to show one, and attach it to the target with [WComponent.toolTip].
 * One hint can be attached to only one component (attaching it to another component removes it from the previous one).
 */
class WToolTip : WControl(
    Activation.composeDefault(XamlInterop.CLS_ToolTip, XamlInterop.IID_IToolTipFactory), // default interface = IToolTip
) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    private var contentComponent: WComponent? = null

    /** The attached component (a weak reference, so that the hint does not keep the component alive). */
    private var owner: WeakReference<WComponent>? = null

    /**
     * The string shown in the hint (ContentControl.Content). Mutually exclusive with [content].
     * Returns "" if the content is not a string ([content] was set, or nothing was set).
     */
    var text: String
        get() = tipText.orEmpty()
        set(value) {
            contentComponent = null
            val boxed = PropertyValues.boxString(value)
            contentControl.call(XamlInterop.IContentControl_put_Content, boxed.ptr)
            boxed.release()
        }

    /**
     * JToolTip.getTipText / setTipText-like: the hint string. null if the content is not a string ([content] was set, or nothing was set).
     * Setting null empties the content.
     */
    var tipText: String?
        get() = contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content)?.let { boxed ->
            try {
                PropertyValues.unboxString(boxed)
            } finally {
                boxed.release()
            }
        }
        set(value) {
            if (value != null) {
                text = value
            } else {
                content = null
            }
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

    /**
     * JToolTip.getComponent-like: the component attached with [WComponent.toolTip]. If not attached or detached,
     * (another hint or [WComponent.toolTipText] was set on the component).
     */
    val component: WComponent?
        get() = owner?.get()

    /**
     * Whether the hint is shown (ToolTip.IsOpen). Setting true shows it right away if the attached component is in a window,
     * and false hides it. It goes back to false when WinUI closes it, such as when the mouse leaves.
     */
    var isOpen: Boolean
        get() = inspectable.getBool(XamlInterop.IToolTip_get_IsOpen)
        set(value) = inspectable.putBool(XamlInterop.IToolTip_put_IsOpen, value)

    /** From setting [WComponent.toolTip]: attached to [component] (detached if null). */
    internal fun attachedTo(component: WComponent?) {
        owner = component?.let(::WeakReference)
    }
}

/** The old form called as a function from Kotlin (Java's `setToolTip(WToolTip)` is unchanged). Sets [WComponent.toolTip]. */
@Deprecated("Set the toolTip property", ReplaceWith("this.toolTip = toolTip"))
@JvmSynthetic
fun WComponent.setToolTip(toolTip: WToolTip) {
    this.toolTip = toolTip
}

/** The ToolTipService statics (SetToolTip / SetPlacement). It's agile, so it's reused. */
internal val toolTipServiceStatics: ComPtr by lazy {
    Activation.factory(XamlInterop.CLS_ToolTipService, XamlInterop.IID_IToolTipServiceStatics)
}
