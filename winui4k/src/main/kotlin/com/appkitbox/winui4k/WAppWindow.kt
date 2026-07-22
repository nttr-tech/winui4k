package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.Abi
import com.appkitbox.winui4k.internal.winui.XamlStructs

/** An integer point on screen (Windows.Graphics.PointInt32). Used by [WAppWindow.position], etc. */
data class WPoint(val x: Int, val y: Int)

/** An integer width/height size (Windows.Graphics.SizeInt32). Used by [WAppWindow.size], etc. */
data class WDimension(val width: Int, val height: Int)

/**
 * Native window management: WinUI 3's AppWindow. No Swing equivalent.
 * Obtained from [WFrame.appWindow], or created for modal use via [create].
 *
 * Provides [title] / [size] / [clientSize] / [position] / [titleBar], plus
 * [resize] / [resizeClient] / [move] / [setIcon] / [setPresenter].
 */
class WAppWindow internal constructor(private val appWindow: ComPtr) {
    /** An IAppWindow2 view for ClientSize / ResizeClient. */
    private val appWindow2: ComPtr by lazy { appWindow.queryInterface(Abi.IID_IAppWindow2) }

    /** The internal WindowId used by owner specification in [WAppWindow.create] and by [WDisplayArea.nearest]. */
    internal val id: Long
        get() = XamlStructs.getWindowId(appWindow, Abi.IAppWindow_get_Id)

    /** The window's title string (AppWindow.Title). Also used by the taskbar and task switcher. */
    var title: String
        get() = appWindow.getString(Abi.IAppWindow_get_Title)
        set(value) = Hstring.use(value) { h -> appWindow.call(Abi.IAppWindow_put_Title, h) }

    /** The whole window's size (title bar included). Setting it is the same as [resize]. */
    var size: WDimension
        get() = XamlStructs.getSizeInt32(appWindow, Abi.IAppWindow_get_Size).let { WDimension(it[0], it[1]) }
        set(value) = resize(value.width, value.height)

    /** The client area's size (the drawing area excluding the title bar) (IAppWindow2.ClientSize, read-only). */
    val clientSize: WDimension
        get() = XamlStructs.getSizeInt32(appWindow2, Abi.IAppWindow2_get_ClientSize).let { WDimension(it[0], it[1]) }

    /** The position relative to the top-left of the screen. Setting it is the same as [move]. */
    var position: WPoint
        get() = XamlStructs.getPointInt32(appWindow, Abi.IAppWindow_get_Position).let { WPoint(it[0], it[1]) }
        set(value) = move(value.x, value.y)

    /** The title bar's appearance settings (AppWindow.TitleBar). */
    val titleBar: WAppWindowTitleBar by lazy {
        WAppWindowTitleBar(appWindow.getPtr(Abi.IAppWindow_get_TitleBar))
    }

    /** Changes the whole window's size (title bar included) (AppWindow.Resize). */
    fun resize(width: Int, height: Int) = XamlStructs.putSizeInt32(appWindow, Abi.IAppWindow_Resize, width, height)

    /** Changes the client area's size (IAppWindow2.ResizeClient). */
    fun resizeClient(width: Int, height: Int) =
        XamlStructs.putSizeInt32(appWindow2, Abi.IAppWindow2_ResizeClient, width, height)

    /** Moves the window relative to the top-left of the screen (AppWindow.Move). */
    fun move(x: Int, y: Int) = XamlStructs.putPointInt32(appWindow, Abi.IAppWindow_Move, x, y)

    /** Sets the taskbar/task switcher icon from an .ico file path (AppWindow.SetIcon). */
    fun setIcon(path: String) = Hstring.use(path) { h -> appWindow.call(Abi.IAppWindow_SetIcon, h) }

    /** Switches the window's behavior (e.g. resizability, modality) (AppWindow.SetPresenter). */
    fun setPresenter(presenter: WAppWindowPresenter) =
        appWindow.call(Abi.IAppWindow_SetPresenter, presenter.presenterPtr.ptr)

    /** Switches to one of the OS default behavior sets (AppWindow.SetPresenter(AppWindowPresenterKind)). */
    fun setPresenter(kind: WAppWindowPresenterKind) =
        appWindow.call(Abi.IAppWindow_SetPresenterKind, kind.native)

    companion object {
        /**
         * Creates a raw AppWindow (AppWindowStatics.Create(presenter, WindowId)).
         * Specifying [owner] makes it an "empty window" owned by that window (its content is
         * unrelated to any [WFrame] — a raw AppWindow. Combine it with [WAppWindowPresenter]'s
         * isModal as an entry point for something like a modal dialog. There is no equivalent of
         * `frame.setContentPane`, so the caller must either prepare a separate [WFrame] to show
         * whatever UI is needed, or operate on this AppWindow itself).
         */
        fun create(presenter: WAppWindowPresenter, owner: WFrame? = null): WAppWindow {
            val statics = Activation.factory(Abi.CLS_AppWindow, Abi.IID_IAppWindowStatics)
            return try {
                Ffi.backend.withScope { scope ->
                    val windowId = XamlStructs.windowIdValue(scope, owner?.appWindow?.id ?: 0L)
                    WAppWindow(
                        statics.getPtr(Abi.IAppWindowStatics_Create, presenter.presenterPtr.ptr, windowId),
                    )
                }
            } finally {
                statics.release()
            }
        }
    }
}
