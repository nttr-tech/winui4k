package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * SpringLayout-like JPanel: WinUI 3's RelativePanel.
 * Places children relative to each other (e.g. [placeRightOf]) or relative to the panel itself (e.g. [alignRightWithPanel]).
 *
 * If the positions of children refer to each other (A to the right of B and B below A, etc.; even on different axes), WinUI cannot lay them out and throws an exception on the UI thread,
 * and the window is no longer drawn. Such calls throw [IllegalArgumentException] right away (so the calling line is known).
 * When a child used as an anchor is [remove]d, the position constraints based on it are also removed (left in place, WinUI cannot find the anchor and throws the same way).
 * Put anchor children into this panel before it is laid out.
 *
 * Unlike SpringLayout, there is no distance argument to the anchor (the pad of putConstraint) and no text baseline alignment (BASELINE)
 * (because WinUI's RelativePanel does not have them). Use the child's [WComponent.margin] for the distance.
 */
class WRelativePanel : WContainer(
    Activation.composeDefault(XamlInterop.CLS_RelativePanel, XamlInterop.IID_IRelativePanelFactory),
) {
    /** Each child's position constraints pointing to anchor children (the SetXxx slot of IRelativePanelStatics -> the anchor). Used to find cycles. */
    private val relations = HashMap<WComponent, HashMap<Int, WComponent>>()

    /** Places [component] to the left of [anchor] (RelativePanel.LeftOf). */
    fun placeLeftOf(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetLeftOf, component, anchor)

    /** Places [component] above [anchor] (RelativePanel.Above). */
    fun placeAbove(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetAbove, component, anchor)

    /** Places [component] to the right of [anchor] (RelativePanel.RightOf). */
    fun placeRightOf(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetRightOf, component, anchor)

    /** Places [component] below [anchor] (RelativePanel.Below). */
    fun placeBelow(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetBelow, component, anchor)

    /** Aligns [component]'s left edge with [anchor]'s left edge (RelativePanel.AlignLeftWith). */
    fun alignLeftWith(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetAlignLeftWith, component, anchor)

    /** Aligns [component]'s top edge with [anchor]'s top edge (RelativePanel.AlignTopWith). */
    fun alignTopWith(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetAlignTopWith, component, anchor)

    /** Aligns [component]'s right edge with [anchor]'s right edge (RelativePanel.AlignRightWith). */
    fun alignRightWith(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetAlignRightWith, component, anchor)

    /** Aligns [component]'s bottom edge with [anchor]'s bottom edge (RelativePanel.AlignBottomWith). */
    fun alignBottomWith(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetAlignBottomWith, component, anchor)

    /** Aligns [component]'s horizontal center with [anchor] (RelativePanel.AlignHorizontalCenterWith). */
    fun alignHorizontalCenterWith(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetAlignHorizontalCenterWith, component, anchor)

    /** Aligns [component]'s vertical center with [anchor] (RelativePanel.AlignVerticalCenterWith). */
    fun alignVerticalCenterWith(component: WComponent, anchor: WComponent) =
        putElement(XamlInterop.IRelativePanelStatics_SetAlignVerticalCenterWith, component, anchor)

    /** Aligns [component]'s left edge with the panel's left edge (RelativePanel.AlignLeftWithPanel). */
    @JvmOverloads
    fun alignLeftWithPanel(component: WComponent, value: Boolean = true) =
        putBool(XamlInterop.IRelativePanelStatics_SetAlignLeftWithPanel, component, value)

    /** Aligns [component]'s top edge with the panel's top edge (RelativePanel.AlignTopWithPanel). */
    @JvmOverloads
    fun alignTopWithPanel(component: WComponent, value: Boolean = true) =
        putBool(XamlInterop.IRelativePanelStatics_SetAlignTopWithPanel, component, value)

    /** Aligns [component]'s right edge with the panel's right edge (RelativePanel.AlignRightWithPanel). */
    @JvmOverloads
    fun alignRightWithPanel(component: WComponent, value: Boolean = true) =
        putBool(XamlInterop.IRelativePanelStatics_SetAlignRightWithPanel, component, value)

    /** Aligns [component]'s bottom edge with the panel's bottom edge (RelativePanel.AlignBottomWithPanel). */
    @JvmOverloads
    fun alignBottomWithPanel(component: WComponent, value: Boolean = true) =
        putBool(XamlInterop.IRelativePanelStatics_SetAlignBottomWithPanel, component, value)

    /** Aligns [component]'s horizontal center with the panel (RelativePanel.AlignHorizontalCenterWithPanel). */
    @JvmOverloads
    fun alignHorizontalCenterWithPanel(component: WComponent, value: Boolean = true) =
        putBool(XamlInterop.IRelativePanelStatics_SetAlignHorizontalCenterWithPanel, component, value)

    /** Aligns [component]'s vertical center with the panel (RelativePanel.AlignVerticalCenterWithPanel). */
    @JvmOverloads
    fun alignVerticalCenterWithPanel(component: WComponent, value: Boolean = true) =
        putBool(XamlInterop.IRelativePanelStatics_SetAlignVerticalCenterWithPanel, component, value)

    /**
     * SetXxx(UIElement, Object value) — the anchor element is passed as value as is. If following the position constraints from [anchor] reaches [component]
     * (a cycle), does nothing and throws [IllegalArgumentException].
     */
    private fun putElement(slot: Int, component: WComponent, anchor: WComponent) {
        require(component !== anchor) { "WRelativePanel: a child cannot be the anchor of its own position ($component)" }
        require(!dependsOn(anchor, component)) {
            "WRelativePanel: the position constraints form a cycle ($anchor is positioned based on $component, so it cannot be the anchor of $component)"
        }
        statics.call(slot, component.uiElement.ptr, anchor.uiElement.ptr)
        relations.getOrPut(component) { HashMap() }[slot] = anchor
    }

    /** Whether the position of [from] is based on the position of [target], following the position constraints. */
    private fun dependsOn(from: WComponent, target: WComponent): Boolean {
        val visited = HashSet<WComponent>()
        val pending = ArrayDeque<WComponent>()
        pending += from
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (current === target) return true
            if (!visited.add(current)) continue
            relations[current]?.values?.let { pending.addAll(it) }
        }
        return false
    }

    /** Removes the position constraints of the removed child and those of other children based on the removed child. */
    override fun onComponentRemoved(component: WComponent) {
        relations.remove(component)?.keys?.forEach { slot -> statics.call(slot, component.uiElement.ptr, null) }
        for ((other, slots) in relations) {
            val referring = slots.filterValues { it === component }.keys
            for (slot in referring) {
                statics.call(slot, other.uiElement.ptr, null)
                slots.remove(slot)
            }
        }
        relations.values.removeAll { it.isEmpty() }
    }

    /** SetXxxWithPanel(UIElement, boolean) — boolean is 1 byte. */
    private fun putBool(slot: Int, component: WComponent, value: Boolean) {
        statics.callWith(
            slot,
            CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.U8),
            component.uiElement.ptr,
            if (value) 1.toByte() else 0.toByte(),
        )
    }

    private companion object {
        /** Attached-property operations for RelativePanel (IRelativePanelStatics). */
        val statics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_RelativePanel, XamlInterop.IID_IRelativePanelStatics) }
    }
}
