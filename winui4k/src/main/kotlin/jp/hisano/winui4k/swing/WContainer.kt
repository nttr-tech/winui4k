package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winui.Abi

/**
 * java.awt.Container-like: common base for Panel-derived types (StackPanel / Grid / Canvas / ...).
 * Provides adding and removing child components (Panel.Children = IVector<UIElement>).
 */
abstract class WContainer internal constructor(inspectable: ComPtr) : WComponent(inspectable) {
    private val children: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IPanel)
            .getPtr(Abi.IPanel_get_Children) // IVector<UIElement>
    }

    open fun add(component: WComponent) {
        // IVector<UIElement>.Append(UIElement) — the actual IID is a SHA-1 computed
        // Abi.IID_IVector_UIElement, but since we already hold a pointer of the correct
        // type, we can just call vtbl[13] directly
        children.call(Abi.IVector_Append, component.uiElement.ptr)
    }

    /** Removes all children (IVector<UIElement>.Clear). */
    fun removeAll() {
        children.call(Abi.IVector_Clear)
    }
}
