package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * JPanel-like with a null layout: WinUI 3's Canvas.
 * Positions children with absolute coordinates (Canvas.Left / Canvas.Top). Overlap order is controlled with [setZIndex].
 */
class WCanvas : WContainer(
    Activation.composeDefault(Abi.CLS_Canvas, Abi.IID_ICanvasFactory),
) {
    /** Adds a child positioned at ([x], [y]). */
    fun add(component: WComponent, x: Double, y: Double) {
        setLocation(component, x, y)
        add(component)
    }

    /** Changes a child's position (the Canvas.SetLeft / SetTop attached properties). */
    fun setLocation(component: WComponent, x: Double, y: Double) {
        statics.call(Abi.ICanvasStatics_SetLeft, component.uiElement.ptr, x)
        statics.call(Abi.ICanvasStatics_SetTop, component.uiElement.ptr, y)
    }

    /** Changes a child's stacking order (the Canvas.SetZIndex attached property). Higher values are on top. */
    fun setZIndex(component: WComponent, zIndex: Int) {
        statics.call(Abi.ICanvasStatics_SetZIndex, component.uiElement.ptr, zIndex)
    }

    private companion object {
        /** Attached-property operations for Canvas (ICanvasStatics). */
        val statics: ComPtr by lazy { Activation.factory(Abi.CLS_Canvas, Abi.IID_ICanvasStatics) }
    }
}
