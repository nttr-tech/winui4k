package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Controls.Orientation (the direction children are lined up in).
 * Shared by StackPanel / VariableSizedWrapGrid. Values extracted from the winmd (Vertical=0, Horizontal=1).
 */
enum class Orientation(internal val native: Int) {
    /** Lines up vertically (default). */
    VERTICAL(0),

    /** Lines up horizontally. */
    HORIZONTAL(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): Orientation = entries.first { it.native == native }
    }
}

/** JPanel-like with a BoxLayout: WinUI 3's StackPanel. Defaults to vertical. */
class WPanel @JvmOverloads constructor(
    spacing: Double = 0.0,
    orientation: Orientation = Orientation.VERTICAL,
) : WContainer(
    Activation.composeDefault(XamlInterop.CLS_StackPanel, XamlInterop.IID_IStackPanelFactory), // default interface = IStackPanel
) {
    /** The direction children are lined up in (StackPanel.Orientation). */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(XamlInterop.IStackPanel_get_Orientation))
        set(value) = inspectable.call(XamlInterop.IStackPanel_put_Orientation, value.native)

    /** The spacing between children (StackPanel.Spacing). */
    var spacing: Double
        get() = inspectable.getDouble(XamlInterop.IStackPanel_get_Spacing)
        set(value) = inspectable.call(XamlInterop.IStackPanel_put_Spacing, value)

    /**
     * JComponent.getBorder / setBorder-like: the panel's border (created with [WBorderFactory]). A line border becomes a line around the panel (BorderBrush / BorderThickness
     * of the StackPanel), and an empty border becomes inner padding around the children (Padding) (a compound border does both; the outside / inside order is not distinguished,
     * the same as [WControl.border]). The default is null (no border).
     *
     * @throws IllegalArgumentException for a compound border containing two or more line borders (then neither the panel nor [border] changes)
     */
    var border: WControlBorder? = null
        set(value) {
            PanelBorders.STACK_PANEL.apply(inspectable, value)
            field = value
            revalidate() // As with JComponent.setBorder, the size changes, so lay out again
        }

    init {
        if (spacing > 0) this.spacing = spacing
        if (orientation != Orientation.VERTICAL) this.orientation = orientation
    }
}
