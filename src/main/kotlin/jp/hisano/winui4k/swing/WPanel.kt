package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.Orientation (the direction StackPanel lines up children).
 * Values extracted from the winmd (Vertical=0, Horizontal=1).
 */
enum class Orientation(internal val native: Int) {
    /** Lines up vertically (default). */
    VERTICAL(0),

    /** Lines up horizontally. */
    HORIZONTAL(1),
    ;

    internal companion object {
        fun of(native: Int): Orientation = entries.first { it.native == native }
    }
}

/** JPanel-like with a BoxLayout: WinUI 3's StackPanel. Defaults to vertical. */
class WPanel(
    spacing: Double = 0.0,
    orientation: Orientation = Orientation.VERTICAL,
) : WComponent(
    WinRt.composeDefault(Abi.CLS_StackPanel, Abi.IID_IStackPanelFactory)
        .queryInterface(Abi.IID_IStackPanel),
) {
    private val children: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IPanel)
            .getPtr(Abi.IPanel_get_Children) // IVector<UIElement>
    }

    /** The direction children are lined up in (StackPanel.Orientation). */
    var orientation: Orientation
        get() = Orientation.of(inspectable.getInt(Abi.IStackPanel_get_Orientation))
        set(value) = inspectable.call(Abi.IStackPanel_put_Orientation, value.native)

    init {
        if (spacing > 0) inspectable.call(Abi.IStackPanel_put_Spacing, spacing)
        if (orientation != Orientation.VERTICAL) this.orientation = orientation
    }

    fun add(component: WComponent) {
        // IVector<UIElement>.Append(UIElement) — the actual IID is a SHA-1 computed
        // Abi.IID_IVector_UIElement, but since we already hold a pointer of the correct
        // type, we can just call vtbl[13] directly
        children.call(Abi.IVector_Append, component.uiElement.ptr)
    }
}
