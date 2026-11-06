package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The [WComponentListener]s of one component and the native handlers that call them.
 *
 * - componentResized: FrameworkElement.SizeChanged
 * - componentMoved: on every FrameworkElement.LayoutUpdated and every Canvas.Left / Top change, compares UIElement.ActualOffset
 *   (the position relative to the parent) with the previous value. Changing Left / Top of a Canvas child makes WinUI change only the position without a layout
 *   (OnActualOffsetChanged for Canvas_Left / Canvas_Top in microsoft-ui-xaml's uielement.cpp), so no LayoutUpdated arrives.
 *   LayoutUpdated arrives on every layout anywhere in the tree, so it is subscribed only while there are listeners
 * - componentShown / componentHidden: the Visibility change callback (DependencyObject.RegisterPropertyChangedCallback).
 *   Only this component's own Visibility is watched (hiding a parent does not trigger it, same as Swing)
 *
 * The handlers are registered only while there are listeners. None of them strongly captures the component or this object;
 * they look the component up through [WComponent.listenerAnchor].
 */
internal class ComponentEvents(private val owner: WComponent) {
    private val listeners = ArrayList<WComponentListener>()

    private var sizeChangedToken = 0L
    private var layoutUpdatedToken = 0L
    private var visibilityToken = 0L
    private var leftToken = 0L
    private var topToken = 0L

    /** The last seen position relative to the parent (for detecting componentMoved). */
    private var lastOffset = doubleArrayOf(0.0, 0.0)

    /** The last seen visibility (for detecting componentShown / componentHidden). */
    private var lastVisible = true

    fun add(listener: WComponentListener) {
        if (listeners.isEmpty()) subscribe()
        listeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    fun remove(listener: WComponentListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
        if (listeners.isEmpty()) unsubscribe()
    }

    fun listeners(): Array<WComponentListener> = listeners.toTypedArray()

    private fun subscribe() {
        val anchor = owner.listenerAnchor
        lastOffset = Xaml.actualOffset(owner.uiElement)
        lastVisible = owner.isVisible
        sizeChangedToken = owner.frameworkElement.addEventHandler(
            "WinUI4K.ComponentListenerSizeChanged",
            XamlInterop.IID_SizeChangedEventHandler,
            XamlInterop.IFrameworkElement_add_SizeChanged,
        ) { _, _ -> anchor.get()?.componentEventsOrNull?.onSizeChanged() }
        layoutUpdatedToken = owner.frameworkElement.addEventHandler(
            "WinUI4K.ComponentListenerLayoutUpdated",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.IFrameworkElement_add_LayoutUpdated,
        ) { _, _ -> anchor.get()?.componentEventsOrNull?.onLayoutUpdated() }
        visibilityToken = TableViewXaml.registerPropertyChangedCallback(owner.dependencyObject, visibilityProperty) { _ ->
            anchor.get()?.componentEventsOrNull?.onVisibilityChanged()
        }
        leftToken = TableViewXaml.registerPropertyChangedCallback(owner.dependencyObject, canvasLeftProperty) { _ ->
            anchor.get()?.componentEventsOrNull?.onOffsetPropertyChanged()
        }
        topToken = TableViewXaml.registerPropertyChangedCallback(owner.dependencyObject, canvasTopProperty) { _ ->
            anchor.get()?.componentEventsOrNull?.onOffsetPropertyChanged()
        }
    }

    private fun unsubscribe() {
        owner.frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_SizeChanged, sizeChangedToken)
        owner.frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_LayoutUpdated, layoutUpdatedToken)
        val target = owner.dependencyObject
        target.call(XamlInterop.IDependencyObject_UnregisterPropertyChangedCallback, visibilityProperty.ptr, visibilityToken)
        target.call(XamlInterop.IDependencyObject_UnregisterPropertyChangedCallback, canvasLeftProperty.ptr, leftToken)
        target.call(XamlInterop.IDependencyObject_UnregisterPropertyChangedCallback, canvasTopProperty.ptr, topToken)
    }

    private fun onSizeChanged() {
        fire(WComponentEvent.COMPONENT_RESIZED)
    }

    private fun onLayoutUpdated() {
        if (listeners.isEmpty()) return
        val offset = Xaml.actualOffset(owner.uiElement)
        if (offset.contentEquals(lastOffset)) return
        lastOffset = offset
        fire(WComponentEvent.COMPONENT_MOVED)
    }

    /** The Canvas.Left / Top change callback. As with [onVisibilityChanged], the position is compared on the next turn of the UI thread. */
    private fun onOffsetPropertyChanged() {
        WinUiUtilities.invokeLater { onLayoutUpdated() }
    }

    /**
     * The Visibility change callback. Swing posts componentShown / componentHidden to the event queue and calls them later,
     * so this also calls them later with [WinUiUtilities.invokeLater] (also so as not to run user code in the middle of a property change).
     */
    private fun onVisibilityChanged() {
        val visible = owner.isVisible
        if (visible == lastVisible) return
        lastVisible = visible
        val id = if (visible) WComponentEvent.COMPONENT_SHOWN else WComponentEvent.COMPONENT_HIDDEN
        WinUiUtilities.invokeLater { fire(id) }
    }

    private fun fire(id: Int) {
        if (listeners.isEmpty()) return
        val event = WComponentEvent(owner, id)
        for (listener in listeners.toList()) {
            when (id) {
                WComponentEvent.COMPONENT_RESIZED -> listener.componentResized(event)
                WComponentEvent.COMPONENT_MOVED -> listener.componentMoved(event)
                WComponentEvent.COMPONENT_SHOWN -> listener.componentShown(event)
                else -> listener.componentHidden(event)
            }
        }
    }

    private companion object {
        /** UIElement.VisibilityProperty (DependencyProperty). */
        val visibilityProperty: ComPtr by lazy {
            staticProperty(XamlInterop.CLS_UIElement, XamlInterop.IID_IUIElementStatics, XamlInterop.IUIElementStatics_get_VisibilityProperty)
        }

        /** Canvas.LeftProperty (attached property). */
        val canvasLeftProperty: ComPtr by lazy {
            staticProperty(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasStatics, XamlInterop.ICanvasStatics_get_LeftProperty)
        }

        /** Canvas.TopProperty (attached property). */
        val canvasTopProperty: ComPtr by lazy {
            staticProperty(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasStatics, XamlInterop.ICanvasStatics_get_TopProperty)
        }

        private fun staticProperty(className: String, staticsIid: String, slot: Int): ComPtr {
            val statics = Activation.factory(className, staticsIid)
            return try {
                statics.getPtr(slot)
            } finally {
                statics.release()
            }
        }
    }
}
