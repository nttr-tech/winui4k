package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.Hstring
import jp.hisano.winui4k.winui.Abi

/** JFrame-like: WinUI 3's Window. */
class WFrame(title: String = "") {
    private val window: ComPtr =
        Activation.composeDefault(Abi.CLS_Window, Abi.IID_IWindowFactory) // IWindow

    /** The root panel corresponding to JFrame's contentPane. */
    val contentPane: WPanel = WPanel(spacing = 12.0).also { it.margin = 24.0 }

    var title: String = ""
        set(value) {
            field = value
            Hstring.use(value) { h -> window.call(Abi.IWindow_put_Title, h) }
        }

    init {
        window.call(Abi.IWindow_put_Content, contentPane.uiElement.ptr)
        if (title.isNotEmpty()) this.title = title
    }

    fun add(component: WComponent) = contentPane.add(component)

    /**
     * Equivalent to JFrame.setContentPane: replaces the root with [component] instead of the
     * default [contentPane] (Window.Content). Useful when you want a full-bleed layout with,
     * say, a Grid instead of StackPanel's unconstrained height measurement.
     */
    fun setContentPane(component: WComponent) {
        window.call(Abi.IWindow_put_Content, component.uiElement.ptr)
    }

    /** Setting this to true shows (activates) the window. */
    var isVisible: Boolean = false
        set(value) {
            field = value
            if (value) window.call(Abi.IWindow_Activate) else window.call(Abi.IWindow_Close)
        }
}
