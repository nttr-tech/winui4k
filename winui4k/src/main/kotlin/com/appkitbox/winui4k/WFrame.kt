package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winui.XamlInterop

/** JFrame-like: WinUI 3's Window. */
class WFrame(title: String = "") {
    private val window: ComPtr =
        Activation.composeDefault(XamlInterop.CLS_Window, XamlInterop.IID_IWindowFactory) // IWindow

    /** The root panel corresponding to JFrame's contentPane. */
    val contentPane: WPanel = WPanel(spacing = 12.0).also { it.margin = 24.0 }

    var title: String = ""
        set(value) {
            field = value
            Hstring.use(value) { h -> window.call(XamlInterop.IWindow_put_Title, h) }
        }

    init {
        window.call(XamlInterop.IWindow_put_Content, contentPane.uiElement.ptr)
        if (title.isNotEmpty()) this.title = title
    }

    fun add(component: WComponent) = contentPane.add(component)

    /**
     * Equivalent to JFrame.setContentPane: replaces the root with [component] instead of the
     * default [contentPane] (Window.Content). Useful when you want a full-bleed layout with,
     * say, a Grid instead of StackPanel's unconstrained height measurement.
     */
    fun setContentPane(component: WComponent) {
        window.call(XamlInterop.IWindow_put_Content, component.uiElement.ptr)
    }

    /** Setting this to true shows (activates) the window. */
    var isVisible: Boolean = false
        set(value) {
            field = value
            if (value) window.call(XamlInterop.IWindow_Activate) else window.call(XamlInterop.IWindow_Close)
        }

    private val window2: ComPtr by lazy { window.queryInterface(XamlInterop.IID_IWindow2) }

    /** An AppWindow view (IWindow2.AppWindow) that handles native window management (position, size, title bar appearance, etc.). */
    val appWindow: WAppWindow by lazy {
        WAppWindow(window2.getPtr(XamlInterop.IWindow2_get_AppWindow))
    }

    /**
     * The window background's system material (Window.SystemBackdrop).
     * Setting [SystemBackdropType.MICA] or similar gives the Windows 11 look where the desktop
     * wallpaper's color shows through faintly on the window background.
     * Components in the areas you want to show through should leave their background unpainted
     * (or make it translucent).
     */
    var systemBackdrop: SystemBackdropType = SystemBackdropType.NONE
        set(value) {
            field = value
            val backdrop = when (value) {
                SystemBackdropType.NONE -> null
                SystemBackdropType.MICA ->
                    Activation.composeDefault(XamlInterop.CLS_MicaBackdrop, XamlInterop.IID_IMicaBackdropFactory)
                SystemBackdropType.MICA_ALT ->
                    Activation.composeDefault(XamlInterop.CLS_MicaBackdrop, XamlInterop.IID_IMicaBackdropFactory)
                        .also { it.call(XamlInterop.IMicaBackdrop_put_Kind, MICA_KIND_BASE_ALT) }
                SystemBackdropType.ACRYLIC ->
                    Activation.composeDefault(XamlInterop.CLS_DesktopAcrylicBackdrop, XamlInterop.IID_IDesktopAcrylicBackdropFactory)
            }
            window2.call(XamlInterop.IWindow2_put_SystemBackdrop, backdrop)
            backdrop?.release() // the put side keeps its own reference, so release the one we created
        }

    /**
     * Whether the app's content extends into the system title bar area (Window.ExtendsContentIntoTitleBar).
     * Set to true together with [setTitleBar] to build a custom title bar (like WTitleBar).
     */
    var extendsContentIntoTitleBar: Boolean
        get() = window.getBool(XamlInterop.IWindow_get_ExtendsContentIntoTitleBar)
        set(value) = window.putBool(XamlInterop.IWindow_put_ExtendsContentIntoTitleBar, value)

    /**
     * When [extendsContentIntoTitleBar] = true, specifies the component to treat as the draggable
     * region (Window.SetTitleBar). Pass null to clear it.
     */
    fun setTitleBar(component: WComponent?) {
        window.call(XamlInterop.IWindow_SetTitleBar, component?.uiElement?.ptr)
    }

    private companion object {
        /** MicaKind.BaseAlt (value extracted from the winmd). */
        const val MICA_KIND_BASE_ALT = 1
    }
}

/**
 * The kind of system material to set on [WFrame.systemBackdrop].
 * Corresponds to WinUI 3's SystemBackdrop subclasses (MicaBackdrop / DesktopAcrylicBackdrop).
 */
enum class SystemBackdropType {
    /** No backdrop (default: the theme's solid-color background). */
    NONE,

    /** Mica: a material for main windows that shows a faint tint of the wallpaper (MicaKind.Base). */
    MICA,

    /** Mica Alt: shows a stronger tint of the wallpaper than Mica (MicaKind.BaseAlt). For tabbed windows. */
    MICA_ALT,

    /** Acrylic: a translucent material that lets windows behind it show through (DesktopAcrylicBackdrop). */
    ACRYLIC,
}
