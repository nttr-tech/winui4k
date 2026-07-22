package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * java.awt.Container-like: common base for Panel-derived types (StackPanel / Grid / Canvas / ...).
 * Provides adding and removing child components (Panel.Children = IVector<UIElement>).
 */
abstract class WContainer internal constructor(inspectable: ComPtr) : WComponent(inspectable) {
    private val children: ComPtr by lazy {
        val panel = own(inspectable.queryInterface(Abi.IID_IPanel))
        own(panel.getPtr(Abi.IPanel_get_Children)) // IVector<UIElement>
    }

    open fun add(component: WComponent) {
        // IVector<UIElement>.Append(UIElement) — the actual IID is a SHA-1 computed
        // Abi.IID_IVector_UIElement, but since we already hold a pointer of the correct
        // type, we can just call vtbl[13] directly
        children.call(Abi.IVector_Append, component.uiElement.ptr)
    }

    /** Removes a child (IVector<UIElement>.IndexOf -> RemoveAt). Does nothing if it isn't present. */
    open fun remove(component: WComponent) {
        Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val index = scope.allocate(4)
            val found = scope.allocate(1)
            children.call(Abi.IVector_IndexOf, component.uiElement.ptr, index, found)
            if (memory.getByte(found, 0) != 0.toByte()) {
                children.call(Abi.IVector_RemoveAt, memory.getInt(index, 0))
            }
        }
    }

    /** Removes all children (IVector<UIElement>.Clear). */
    open fun removeAll() {
        children.call(Abi.IVector_Clear)
    }
}
