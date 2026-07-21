package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT

/**
 * SpringLayout-like JPanel: WinUI 3's RelativePanel.
 * Places children relative to each other (e.g. [placeRightOf]) or relative to the panel itself (e.g. [alignRightWithPanel]).
 */
class WRelativePanel : WContainer(
    WinRt.composeDefault(Abi.CLS_RelativePanel, Abi.IID_IRelativePanelFactory),
) {
    /** Places [component] to the left of [anchor] (RelativePanel.LeftOf). */
    fun placeLeftOf(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetLeftOf, component, anchor)

    /** Places [component] above [anchor] (RelativePanel.Above). */
    fun placeAbove(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetAbove, component, anchor)

    /** Places [component] to the right of [anchor] (RelativePanel.RightOf). */
    fun placeRightOf(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetRightOf, component, anchor)

    /** Places [component] below [anchor] (RelativePanel.Below). */
    fun placeBelow(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetBelow, component, anchor)

    /** Aligns [component]'s left edge with [anchor]'s left edge (RelativePanel.AlignLeftWith). */
    fun alignLeftWith(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetAlignLeftWith, component, anchor)

    /** Aligns [component]'s top edge with [anchor]'s top edge (RelativePanel.AlignTopWith). */
    fun alignTopWith(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetAlignTopWith, component, anchor)

    /** Aligns [component]'s right edge with [anchor]'s right edge (RelativePanel.AlignRightWith). */
    fun alignRightWith(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetAlignRightWith, component, anchor)

    /** Aligns [component]'s bottom edge with [anchor]'s bottom edge (RelativePanel.AlignBottomWith). */
    fun alignBottomWith(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetAlignBottomWith, component, anchor)

    /** Aligns [component]'s horizontal center with [anchor] (RelativePanel.AlignHorizontalCenterWith). */
    fun alignHorizontalCenterWith(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetAlignHorizontalCenterWith, component, anchor)

    /** Aligns [component]'s vertical center with [anchor] (RelativePanel.AlignVerticalCenterWith). */
    fun alignVerticalCenterWith(component: WComponent, anchor: WComponent) =
        putElement(Abi.IRelativePanelStatics_SetAlignVerticalCenterWith, component, anchor)

    /** Aligns [component]'s left edge with the panel's left edge (RelativePanel.AlignLeftWithPanel). */
    fun alignLeftWithPanel(component: WComponent, value: Boolean = true) =
        putBool(Abi.IRelativePanelStatics_SetAlignLeftWithPanel, component, value)

    /** Aligns [component]'s top edge with the panel's top edge (RelativePanel.AlignTopWithPanel). */
    fun alignTopWithPanel(component: WComponent, value: Boolean = true) =
        putBool(Abi.IRelativePanelStatics_SetAlignTopWithPanel, component, value)

    /** Aligns [component]'s right edge with the panel's right edge (RelativePanel.AlignRightWithPanel). */
    fun alignRightWithPanel(component: WComponent, value: Boolean = true) =
        putBool(Abi.IRelativePanelStatics_SetAlignRightWithPanel, component, value)

    /** Aligns [component]'s bottom edge with the panel's bottom edge (RelativePanel.AlignBottomWithPanel). */
    fun alignBottomWithPanel(component: WComponent, value: Boolean = true) =
        putBool(Abi.IRelativePanelStatics_SetAlignBottomWithPanel, component, value)

    /** Aligns [component]'s horizontal center with the panel (RelativePanel.AlignHorizontalCenterWithPanel). */
    fun alignHorizontalCenterWithPanel(component: WComponent, value: Boolean = true) =
        putBool(Abi.IRelativePanelStatics_SetAlignHorizontalCenterWithPanel, component, value)

    /** Aligns [component]'s vertical center with the panel (RelativePanel.AlignVerticalCenterWithPanel). */
    fun alignVerticalCenterWithPanel(component: WComponent, value: Boolean = true) =
        putBool(Abi.IRelativePanelStatics_SetAlignVerticalCenterWithPanel, component, value)

    /** SetXxx(UIElement, Object value) — value is the anchor element itself. */
    private fun putElement(slot: Int, component: WComponent, anchor: WComponent) {
        statics.call(slot, component.uiElement.ptr, anchor.uiElement.ptr)
    }

    /** SetXxxWithPanel(UIElement, boolean) — boolean is 1 byte. */
    private fun putBool(slot: Int, component: WComponent, value: Boolean) {
        statics.callWith(
            slot,
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_BYTE),
            component.uiElement.ptr,
            if (value) 1.toByte() else 0.toByte(),
        )
    }

    private companion object {
        /** Attached-property operations for RelativePanel (IRelativePanelStatics). */
        val statics: ComPtr by lazy { WinRt.factory(Abi.CLS_RelativePanel, Abi.IID_IRelativePanelStatics) }
    }
}
