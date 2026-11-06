package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The groundwork of [WComponent.transferFocus] / [WComponent.transferFocusBackward] (FocusManager.TryMoveFocus).
 *
 * On the desktop (XAML in a Window), TryMoveFocus without a SearchRoot fails with E_UNEXPECTED, and the only SearchRoot that can be passed
 * with Next / Previous is XamlRoot.Content (TryMoveFocusStatic in dxaml/xcp/dxaml/lib/FocusManager.cpp of microsoft-ui-xaml).
 */
internal object FocusTraversal {
    /** Moves the focus from [component] to the next element in the same order as the Tab key ([next] = true) or Shift+Tab (false). */
    fun transfer(component: WComponent, next: Boolean) {
        if (!component.isDisplayable) return
        val root = component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return
        try {
            // TryMoveFocus moves from the element that currently has the focus, so move it to this component first
            if (!component.hasFocus()) component.requestFocusInWindow()
            val content = root.getPtrOrNull(XamlInterop.IXamlRoot_get_Content) ?: return
            try {
                tryMoveFocus(next, content)
            } finally {
                content.release()
            }
        } finally {
            root.release()
        }
    }

    /** FocusManager.TryMoveFocus(direction, FindNextElementOptions { SearchRoot = [searchRoot] }). true if the focus moved. */
    private fun tryMoveFocus(next: Boolean, searchRoot: ComPtr): Boolean {
        val options = Activation.activate(XamlInterop.CLS_FindNextElementOptions, XamlInterop.IID_IFindNextElementOptions)
        try {
            val dependency = searchRoot.queryInterface(XamlInterop.IID_IDependencyObject)
            try {
                options.call(XamlInterop.IFindNextElementOptions_put_SearchRoot, dependency.ptr)
            } finally {
                dependency.release()
            }
            val statics = Activation.factory(XamlInterop.CLS_FocusManager, XamlInterop.IID_IFocusManagerStatics)
            try {
                return Ffi.backend.withScope { scope ->
                    val out = scope.allocate(1, 1)
                    val direction =
                        if (next) XamlInterop.FocusNavigationDirection_Next else XamlInterop.FocusNavigationDirection_Previous
                    statics.call(XamlInterop.IFocusManagerStatics_TryMoveFocusWithOptions, direction, options.ptr, out)
                    Ffi.backend.memory.getByte(out, 0) != 0.toByte()
                }
            } finally {
                statics.release()
            }
        } finally {
            options.release()
        }
    }
}
