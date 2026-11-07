package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs

/**
 * Writes a [WControlBorder] to a panel (Grid, StackPanel) that has its own border line (BorderBrush, BorderThickness) and inner padding (Padding).
 * [brushSlot] / [thicknessSlot] / [paddingSlot] are put_BorderBrush / put_BorderThickness /
 * put_Padding of the panel's default interface.
 */
internal class PanelBorders(private val brushSlot: Int, private val thicknessSlot: Int, private val paddingSlot: Int) {
    /**
     * Writes [border] to [target] (the panel's default interface). A line border becomes the line color and thickness, and an empty border becomes Padding (a compound border sets both;
     * the outside / inside order is not distinguished, the same as [WControl.border]). null means no line and no padding (the panel's default).
     * If there are two or more line borders, IllegalArgumentException, and nothing is changed.
     */
    fun apply(target: ComPtr, border: WControlBorder?) {
        val flat = border?.let { FlatBorder.of(it) }
        val line = flat?.line
        val brush = line?.color?.createBrush()
        try {
            target.call(brushSlot, brush?.ptr)
        } finally {
            brush?.release()
        }
        val thickness = line?.thickness?.toDouble() ?: 0.0
        XamlStructs.putThickness(target, thicknessSlot, thickness, thickness, thickness, thickness)
        val padding = flat?.padding ?: WInsets(0.0)
        XamlStructs.putThickness(target, paddingSlot, padding.left, padding.top, padding.right, padding.bottom)
    }

    companion object {
        /** Grid (the root of [WLabel]). */
        val GRID = PanelBorders(XamlInterop.IGrid_put_BorderBrush, XamlInterop.IGrid_put_BorderThickness, XamlInterop.IGrid_put_Padding)

        /** StackPanel ([WPanel]). */
        val STACK_PANEL = PanelBorders(
            XamlInterop.IStackPanel_put_BorderBrush,
            XamlInterop.IStackPanel_put_BorderThickness,
            XamlInterop.IStackPanel_put_Padding,
        )
    }
}
