package jp.hisano.winui4k

import jp.hisano.winui4k.internal.com.ComPtr
import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winrt.Hstring
import jp.hisano.winui4k.internal.winui.Abi

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

    /** An AppWindow view (IWindow2.AppWindow) that handles native window management (position, size, title bar appearance, etc.). */
    val appWindow: WAppWindow by lazy {
        val window2 = window.queryInterface(Abi.IID_IWindow2)
        WAppWindow(window2.getPtr(Abi.IWindow2_get_AppWindow))
    }

    /**
     * Whether the app's content extends into the system title bar area (Window.ExtendsContentIntoTitleBar).
     * Set to true together with [setTitleBar] to build a custom title bar (like WTitleBar).
     */
    var extendsContentIntoTitleBar: Boolean
        get() = window.getBool(Abi.IWindow_get_ExtendsContentIntoTitleBar)
        set(value) = window.putBool(Abi.IWindow_put_ExtendsContentIntoTitleBar, value)

    /**
     * When [extendsContentIntoTitleBar] = true, specifies the component to treat as the draggable
     * region (Window.SetTitleBar). Pass null to clear it.
     */
    fun setTitleBar(component: WComponent?) {
        window.call(Abi.IWindow_SetTitleBar, component?.uiElement?.ptr)
    }
}
