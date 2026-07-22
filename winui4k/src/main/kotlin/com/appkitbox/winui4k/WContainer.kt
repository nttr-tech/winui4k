package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * java.awt.Container-like: common base for Panel-derived types (StackPanel / Grid / Canvas / ...).
 * Provides adding and removing child components (Panel.Children = IVector<UIElement>).
 */
abstract class WContainer internal constructor(inspectable: ComPtr) : WComponent(inspectable) {
    private val children: ComPtr by lazy {
        val panel = own(inspectable.queryInterface(XamlInterop.IID_IPanel))
        own(panel.getPtr(XamlInterop.IPanel_get_Children)) // IVector<UIElement>
    }

    open fun add(component: WComponent) {
        // IVector<UIElement>.Append(UIElement) — the actual IID is a SHA-1 computed
        // FoundationInterop.IID_IVector_UIElement, but since we already hold a pointer of the correct
        // type, we can just call vtbl[13] directly
        children.call(FoundationInterop.IVector_Append, component.uiElement.ptr)
    }

    /** Removes a child (IVector<UIElement>.IndexOf -> RemoveAt). Does nothing if it isn't present. */
    open fun remove(component: WComponent) {
        Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val index = scope.allocate(4)
            val found = scope.allocate(1)
            children.call(FoundationInterop.IVector_IndexOf, component.uiElement.ptr, index, found)
            if (memory.getByte(found, 0) != 0.toByte()) {
                children.call(FoundationInterop.IVector_RemoveAt, memory.getInt(index, 0))
            }
        }
    }

    /** Removes all children (IVector<UIElement>.Clear). */
    open fun removeAll() {
        children.call(FoundationInterop.IVector_Clear)
    }
}
