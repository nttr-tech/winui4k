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

    init {
        if (spacing > 0) this.spacing = spacing
        if (orientation != Orientation.VERTICAL) this.orientation = orientation
    }

    /**
     * In a vertical stack, whether children whose width is decided by the column count ([WTextField.columns] / [WTextPane.columns]) stretch to the panel's width
     * instead of being fixed to that width (default false: they get the column width and are centered). [WContentDialog.contentPane] sets it to true (as in the center
     * of the BorderLayout of a JDialog's contentPane, text fields span the dialog's width). Children with an explicit [WComponent.width] keep that width.
     */
    internal var stretchesColumnSizedChildren = false
}
