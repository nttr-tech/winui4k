package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi

/** JPanel-like with a vertical BoxLayout: WinUI 3's StackPanel. */
class WPanel(spacing: Double = 0.0) : WComponent(
    WinRt.composeDefault(Abi.CLS_StackPanel, Abi.IID_IStackPanelFactory)
        .queryInterface(Abi.IID_IStackPanel),
) {
    private val children: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IPanel)
            .getPtr(Abi.IPanel_get_Children) // IVector<UIElement>
    }

    init {
        if (spacing > 0) inspectable.call(Abi.IStackPanel_put_Spacing, spacing)
    }

    fun add(component: WComponent) {
        // IVector<UIElement>.Append(UIElement) — the actual IID is a SHA-1 computed
        // Abi.IID_IVector_UIElement, but since we already hold a pointer of the correct
        // type, we can just call vtbl[13] directly
        children.call(Abi.IVector_Append, component.uiElement.ptr)
    }
}
