package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.win32.Win32
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI
import java.net.URISyntaxException
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * JFrame-like: WinUI 3's Window.
 *
 * As in Swing, WFrames that have been shown and are not yet closed are held by [frames], so they are not removed by GC even if not kept in a variable,
 * and are released when closed (Window.Closed). A window that is only hidden ([isVisible] = false) is considered still open, and
 * the automatic app exit when the last window closes ([WinUiUtilities.exitOnLastWindowClosed]) does not happen either (as in Swing).
 * A WFrame never shown is not counted as an open window (the same as before it becomes displayable in Swing) and does not stop the automatic exit.
 * It is collected by GC once the reference is released.
 *
 * Differences from Swing:
 * - The default of [defaultCloseOperation] is [DISPOSE_ON_CLOSE], not Swing's HIDE_ON_CLOSE (so that no invisible
 *   process remains after closing with ×).
 * - A [dispose]d window cannot be shown again ([isVisible] = true throws [IllegalStateException]).
 */
@Suppress("TooManyFunctions") // Lists the JFrame / Window APIs in one class
class WFrame @JvmOverloads constructor(title: String = "") :
    WWindowConstants,
    WDialogParent {
    internal val window: ComPtr =
        Activation.composeDefault(XamlInterop.CLS_Window, XamlInterop.IID_IWindowFactory) // IWindow

    /**
     * The owner of the [window] and [window2] references. Released on the UI thread when the WFrame is collected (not when it is closed, so that
     * [appWindow] etc. can still be read after closing). From being shown until closed, [openFrames] holds the WFrame, so they are not released while it is open.
     */
    private val lifetime = ComLifetime.adopt(this, window)

    private val window2: ComPtr by lazy { lifetime.own(window.queryInterface(XamlInterop.IID_IWindow2)) }

    /**
     * An AppWindow view (IWindow2.AppWindow) that handles native window management (position, size, title bar appearance, etc.).
     * The AppWindow reference is held by the [WAppWindow] and released when that is collected.
     */
    val appWindow: WAppWindow by lazy {
        WAppWindow(window2.getPtr(XamlInterop.IWindow2_get_AppWindow), ownsReference = true)
    }

    /**
     * The default root panel corresponding to JFrame's contentPane (a WPanel, that is, a vertical StackPanel). Even after replacing it with [setContentPane]
     * or setting [layout], this returns the default panel (to keep the type WPanel). Swing's `getContentPane().add(...)` form goes into this panel, which is not
     * on screen after the replacement, so use [add] (which goes into the current content) after replacing it.
     */
    val contentPane: WPanel = WPanel(spacing = 12.0).also { it.margin = 24.0 }

    /** The current window content (Window.Content). Initially [contentPane], replaced by [setContentPane]. */
    internal var content: WComponent = contentPane
        private set

    /** The root Grid with a menu row and a content row, created when [jMenuBar] is set. null until then (Window.Content is the content itself). */
    private var rootGrid: WGrid? = null

    /** The element put in Window.Content (measured by [pack]). */
    private val rootContent: WComponent get() = rootGrid ?: content

    var title: String = ""
        set(value) {
            field = value
            Hstring.use(value) { h -> window.call(XamlInterop.IWindow_put_Title, h) }
        }

    private val windowListenerList = ArrayList<WWindowListener>()
    private val windowStateListenerList = ArrayList<WWindowStateListener>()
    private val windowFocusListenerList = ArrayList<WWindowFocusListener>()

    /** true if closed (Window.Closed, or Close was called by [dispose]). */
    private var closed = false

    /**
     * Whether it has been counted as an open window ([registerOpen]). The Window.Closed handler also reads it, so it is a holder independent of the WFrame
     * (so that the handler does not hold the WFrame strongly, and the count can be restored correctly even if the WFrame is collected before closing).
     */
    private val openCount = OpenCount()

    /** true if windowOpened has been fired (fired only once). */
    private var opened = false

    /** true while windowClosing listeners are being called ([dispose] inside them works by not stopping the close). */
    private var inClosing = false

    /** true if [dispose] was called inside windowClosing. */
    private var disposeRequestedInClosing = false

    /**
     * true while [dispose] is calling Window.Close. AppWindow.Closing does not come with Window.Close (doc/verification.md), but
     * this guards against turning it into windowClosing if it ever comes.
     */
    private var disposing = false

    /** The last read state ([NORMAL] / [ICONIFIED] / [MAXIMIZED_BOTH]). */
    private var lastState = NORMAL

    /**
     * Window.isActive-like: true if this window is active (tracked with Window.Activated). A window shown with [WAppWindow.show]
     * with activate = false does not become active until the user selects it.
     */
    var isActive: Boolean = false
        private set

    /** Window.isFocused-like: true if this window has the focus. Same as [isActive] in WinUI. */
    val isFocused: Boolean get() = isActive

    init {
        window.call(XamlInterop.IWindow_put_Content, contentPane.uiElement.ptr)
        if (title.isNotEmpty()) this.title = title
        subscribeNativeEvents()
    }

    /**
     * When first shown, counts it as an open window and puts it into [openFrames] (does nothing from the second time on). The count is used for the automatic
     * app exit when the last one closes (WinUiUtilities.exitOnLastWindowClosed), and is decremented on Window.Closed.
     * Called not only for [isVisible] = true but also when shown with [appWindow]'s show (AppWindow.Changed / Window.Activated).
     */
    private fun registerOpen() {
        if (closed || openCount.counted) return
        openCount.counted = true
        WinUiUtilities.noteWindowCreated()
        openFrames.add(this)
    }

    /**
     * Subscribes to window notifications. The handlers stay in the window (native), so they reference the WFrame weakly. From being shown until it is closed,
     * [openFrames] holds it strongly, so the weak reference is never cleared during that time.
     */
    private fun subscribeNativeEvents() {
        val self = WeakReference(this)
        val openCount = openCount
        window.addEventHandler(
            "WinUI4K.WindowClosedHandler",
            XamlInterop.IID_WindowClosedHandler,
            XamlInterop.IWindow_add_Closed,
        ) { _, _ ->
            val frame = self.get()
            if (frame != null) {
                DefaultButtons.forget(frame)
                frame.notifyWindowClosedHooks()
                frame.onClosed()
            }
            if (openCount.counted) {
                openCount.counted = false
                WinUiUtilities.noteWindowClosed()
            }
        }
        window.addEventHandler(
            "WinUI4K.WindowActivatedHandler",
            XamlInterop.IID_WindowActivatedHandler,
            XamlInterop.IWindow_add_Activated,
        ) { _, args ->
            val state = ComPtr(args).getInt(XamlInterop.IWindowActivatedEventArgs_get_WindowActivationState)
            self.get()?.onActivated(state != XamlInterop.WINDOW_ACTIVATION_STATE_DEACTIVATED)
        }
        val native = appWindow.appWindow
        native.addEventHandler(
            "WinUI4K.AppWindowClosingHandler",
            WindowingInterop.IID_AppWindowClosingHandler,
            WindowingInterop.IAppWindow_add_Closing,
        ) { _, args ->
            if (self.get()?.onClosing() == true) {
                ComPtr(args).putBool(WindowingInterop.IAppWindowClosingEventArgs_put_Cancel, true)
            }
        }
        native.addEventHandler(
            "WinUI4K.AppWindowChangedHandler",
            WindowingInterop.IID_AppWindowChangedHandler,
            WindowingInterop.IAppWindow_add_Changed,
        ) { _, args ->
            val visibilityChanged = ComPtr(args).getBool(WindowingInterop.IAppWindowChangedEventArgs_get_DidVisibilityChange)
            self.get()?.onChanged(visibilityChanged)
        }
    }

    /**
     * Passes this window's XamlRoot to the [addWindowClosedHook] hooks. Window.Closed comes before the content tree is torn down
     * (ContentRoot.Close sets popups to IsOpen = false but does not raise Popup.Closed; ForceCloseForTreeReset in Popup.cpp),
     * so the XamlRoot can still be read at this point.
     */
    private fun notifyWindowClosedHooks() {
        if (windowClosedHooks.isEmpty()) return
        val root = rootContent.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return
        try {
            windowClosedHooks.toList().forEach { it.windowClosed(root) }
        } finally {
            root.release()
        }
    }

    private fun onClosed() {
        closed = true
        openFrames.remove(this)
        if (isActive) onActivated(false)
        fireWindow(WWindowEvent.WINDOW_CLOSED) { l, e -> l.windowClosed(e) }
    }

    private fun onActivated(active: Boolean) {
        if (active) {
            registerOpen()
            fireOpenedOnce()
            // Put the one that became active last at the end (used to decide where a WOptionPane without a parent opens)
            if (!closed && openFrames.remove(this)) openFrames.add(this)
        }
        if (active == isActive) return
        isActive = active
        if (active) {
            fireWindow(WWindowEvent.WINDOW_ACTIVATED) { l, e -> l.windowActivated(e) }
            fireFocus(WWindowEvent.WINDOW_GAINED_FOCUS) { l, e -> l.windowGainedFocus(e) }
        } else {
            fireFocus(WWindowEvent.WINDOW_LOST_FOCUS) { l, e -> l.windowLostFocus(e) }
            fireWindow(WWindowEvent.WINDOW_DEACTIVATED) { l, e -> l.windowDeactivated(e) }
        }
    }

    private fun fireOpenedOnce() {
        if (opened || closed) return
        opened = true
        fireWindow(WWindowEvent.WINDOW_OPENED) { l, e -> l.windowOpened(e) }
    }

    /** AppWindow.Closing. Returns true to cancel the close. */
    private fun onClosing(): Boolean {
        if (disposing || closed) return false
        inClosing = true
        disposeRequestedInClosing = false
        try {
            fireWindow(WWindowEvent.WINDOW_CLOSING) { l, e -> l.windowClosing(e) }
        } finally {
            inClosing = false
        }
        if (disposeRequestedInClosing) return false
        return when (defaultCloseOperation) {
            DO_NOTHING_ON_CLOSE -> true
            HIDE_ON_CLOSE -> {
                appWindow.hide()
                true
            }
            EXIT_ON_CLOSE -> {
                WinUiUtilities.exit()
                false
            }
            else -> false
        }
    }

    /** AppWindow.Changed. Fires windowOpened when shown, and iconify / maximize events when the presenter state changes. */
    private fun onChanged(visibilityChanged: Boolean) {
        if (closed) return
        if (visibilityChanged && appWindow.isVisible) {
            registerOpen()
            fireOpenedOnce()
        }
        if (!applyingState) syncState()
    }

    /** Compares OverlappedPresenter.State with the previously read value, and fires iconify / maximize events if it changed. */
    private fun syncState() {
        val state = readState() ?: return
        val old = lastState
        if (state == old) return
        lastState = state
        if (state == ICONIFIED) {
            fireWindow(WWindowEvent.WINDOW_ICONIFIED) { l, e -> l.windowIconified(e) }
        } else if (old == ICONIFIED) {
            fireWindow(WWindowEvent.WINDOW_DEICONIFIED) { l, e -> l.windowDeiconified(e) }
        }
        if (windowStateListenerList.isEmpty()) return
        val event = WWindowEvent(this, WWindowEvent.WINDOW_STATE_CHANGED, old, state)
        windowStateListenerList.toList().forEach { it.windowStateChanged(event) }
    }

    /** Reads OverlappedPresenter.State as [NORMAL] / [ICONIFIED] / [MAXIMIZED_BOTH]. null for other presenters. */
    private fun readState(): Int? = withOverlapped { presenter ->
        when (presenter.state) {
            OverlappedPresenterState.MAXIMIZED -> MAXIMIZED_BOTH
            OverlappedPresenterState.MINIMIZED -> ICONIFIED
            OverlappedPresenterState.RESTORED -> NORMAL
        }
    }

    /**
     * If the current AppWindow.Presenter is an OverlappedPresenter, wraps it temporarily and passes it to [block] (releasing it afterwards).
     * For other presenters (full screen, etc.), returns null without calling [block].
     */
    private inline fun <T> withOverlapped(block: (WOverlappedPresenter) -> T): T? {
        val presenter = appWindow.appWindow.getPtrOrNull(WindowingInterop.IAppWindow_get_Presenter) ?: return null
        val overlapped = try {
            presenter.queryInterfaceOrNull(WindowingInterop.IID_IOverlappedPresenter)
        } finally {
            presenter.release()
        } ?: return null
        val wrapper = WOverlappedPresenter(overlapped)
        return try {
            block(wrapper)
        } finally {
            wrapper.release()
        }
    }

    private inline fun fireWindow(id: Int, call: (WWindowListener, WWindowEvent) -> Unit) {
        if (windowListenerList.isEmpty()) return
        val event = WWindowEvent(this, id)
        windowListenerList.toList().forEach { call(it, event) }
    }

    private inline fun fireFocus(id: Int, call: (WWindowFocusListener, WWindowEvent) -> Unit) {
        if (windowFocusListenerList.isEmpty()) return
        val event = WWindowEvent(this, id)
        windowFocusListenerList.toList().forEach { call(it, event) }
    }

    /**
     * Window.addWindowListener-like: adds a listener for opened, closing, closed, iconified and activated.
     * Does nothing if null. Adding the same listener twice calls it twice (as in Swing). Called in the order added.
     *
     * - windowOpened: once, when first shown (the show in AppWindow.Changed, or the first Window.Activated)
     * - windowClosing: when the user tries to close with × or Alt+F4 (AppWindow.Closing). [defaultCloseOperation] is performed after it.
     *   Calling [dispose] inside it closes the window as is. Not fired when closed with [dispose] or when the app exits (as in Swing)
     * - windowClosed: after closing (Window.Closed). Fired both for [dispose] and ×
     * - windowIconified / windowDeiconified: when iconified or restored (compares OverlappedPresenter.State on AppWindow.Changed)
     * - windowActivated / windowDeactivated: Window.Activated
     */
    fun addWindowListener(listener: WWindowListener?) {
        if (listener != null) windowListenerList.add(listener)
    }

    /** Window.removeWindowListener-like: removes one listener added with [addWindowListener]. Does nothing if it was not added. */
    fun removeWindowListener(listener: WWindowListener?) {
        windowListenerList.remove(listener)
    }

    /** Window.getWindowListeners-like: the added listeners (in the order added). */
    val windowListeners: Array<WWindowListener>
        get() = windowListenerList.toTypedArray()

    /**
     * Window.addWindowStateListener-like: adds a listener for when the state changes between normal ([NORMAL]), iconified ([ICONIFIED])
     * and maximized ([MAXIMIZED_BOTH]). On iconifying and restoring it is called after [WWindowListener.windowIconified] / [WWindowListener.windowDeiconified].
     * When iconified from maximized, the new state is only [ICONIFIED] (not Swing's ICONIFIED | MAXIMIZED_BOTH).
     */
    fun addWindowStateListener(listener: WWindowStateListener?) {
        if (listener != null) windowStateListenerList.add(listener)
    }

    /** Window.removeWindowStateListener-like: removes one listener added with [addWindowStateListener]. */
    fun removeWindowStateListener(listener: WWindowStateListener?) {
        windowStateListenerList.remove(listener)
    }

    /** Window.getWindowStateListeners-like: the added listeners (in the order added). */
    val windowStateListeners: Array<WWindowStateListener>
        get() = windowStateListenerList.toTypedArray()

    /**
     * Window.addWindowFocusListener-like: adds a listener for when the window gains or loses the focus. In WinUI this is the same time
     * as activation (Window.Activated); gaining is called after windowActivated and losing before windowDeactivated (the same order as Swing).
     */
    fun addWindowFocusListener(listener: WWindowFocusListener?) {
        if (listener != null) windowFocusListenerList.add(listener)
    }

    /** Window.removeWindowFocusListener-like: removes one listener added with [addWindowFocusListener]. */
    fun removeWindowFocusListener(listener: WWindowFocusListener?) {
        windowFocusListenerList.remove(listener)
    }

    /** Window.getWindowFocusListeners-like: the added listeners (in the order added). */
    val windowFocusListeners: Array<WWindowFocusListener>
        get() = windowFocusListenerList.toTypedArray()

    /**
     * JFrame.setDefaultCloseOperation-like: the action when the user tries to close with × or Alt+F4. Performed after the windowClosing listeners.
     * [DO_NOTHING_ON_CLOSE] does not close, [HIDE_ON_CLOSE] hides (AppWindow.Hide; stays open), [DISPOSE_ON_CLOSE] closes,
     * and [EXIT_ON_CLOSE] closes and exits the app ([WinUiUtilities.exit]). Other values throw [IllegalArgumentException] (as in Swing).
     * Unlike Swing's HIDE_ON_CLOSE, the default is [DISPOSE_ON_CLOSE] (keeping WinUI4K's previous behavior).
     */
    var defaultCloseOperation: Int = DISPOSE_ON_CLOSE
        set(value) {
            require(value in DO_NOTHING_ON_CLOSE..EXIT_ON_CLOSE) {
                "defaultCloseOperation must be one of DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE, DISPOSE_ON_CLOSE or EXIT_ON_CLOSE: $value"
            }
            field = value
        }

    /**
     * Window.dispose-like: closes and disposes of the window (Window.Close). windowClosing is not fired, and windowClosed is.
     * Does nothing from the second time on. Called from inside a windowClosing listener, it lets the user's close go through as is.
     * If it is the last window, the app exits according to [WinUiUtilities.exitOnLastWindowClosed].
     */
    fun dispose() {
        if (closed) return
        if (inClosing) {
            disposeRequestedInClosing = true
            return
        }
        disposing = true
        try {
            window.call(XamlInterop.IWindow_Close)
        } finally {
            disposing = false
        }
        closed = true
        openFrames.remove(this)
    }

    /** Component.isDisplayable-like: true if not yet closed (neither [dispose]d nor disposed with ×). true if only hidden. */
    val isDisplayable: Boolean get() = !closed

    // ---- Position and size (DIP) ----
    //
    // As with Swing since Java 9, window position and size are handled in logical units divided by the scale factor (DIP; one pixel at 100%).
    // It is the same unit as other WinUI4K components; on a 150% monitor, setSize(400, 300) becomes physical 600 x 450 pixels.
    // The scale factor is that of the monitor this window is currently on (GetDpiForWindow). Use [appWindow] to work in physical pixels.

    /** Physical pixels per DIP (this window's DPI / 96). Even before it is shown, the value of the monitor it will be on. */
    internal val dpiScale: Double
        get() {
            // The WindowId value is the same as the HWND (Win32Interop.GetWindowFromWindowId returns the value as is)
            val dpi = Win32.dpiForWindow(Ptr(appWindow.id))
            return if (dpi > 0) dpi / BASE_DPI else 1.0
        }

    private fun toDip(pixels: Int, scale: Double): Int = (pixels / scale).roundToInt()

    private fun toPixels(dip: Int, scale: Double): Int = (dip * scale).roundToInt()

    /**
     * Component.getSize / setSize(Dimension)-like: the size of the whole window including the title bar and border (DIP; AppWindow.Size).
     * Setting is the same as [setSize].
     */
    var size: WDimension
        get() {
            val scale = dpiScale
            return appWindow.size.let { WDimension(toDip(it.width, scale), toDip(it.height, scale)) }
        }
        set(value) = setSize(value.width, value.height)

    /**
     * Window.setSize(int, int)-like: changes the size of the whole window in DIP (AppWindow.Resize). Values smaller than [minimumSize] are
     * enlarged to [minimumSize] (as in Swing). Negative values are treated as 0. It does not get smaller than the minimum size the OS decides
     * (a width that fits the title bar buttons).
     */
    fun setSize(width: Int, height: Int) {
        val minimum = minimumSize
        val scale = dpiScale
        appWindow.resize(
            toPixels(maxOf(width, minimum?.width ?: 0, 0), scale),
            toPixels(maxOf(height, minimum?.height ?: 0, 0), scale),
        )
    }

    /** Component.getWidth-like: the width of the whole window (DIP). */
    val width: Int get() = size.width

    /** Component.getHeight-like: the height of the whole window (DIP). */
    val height: Int get() = size.height

    /**
     * Component.getLocation / setLocation(Point)-like: the on-screen position of the window's top-left (DIP; AppWindow.Position divided by the scale factor).
     * As in Swing, coordinates are not continuous across monitors with different scale factors. Setting is the same as [setLocation].
     */
    var location: WPoint
        get() {
            val scale = dpiScale
            return appWindow.position.let { WPoint(toDip(it.x, scale), toDip(it.y, scale)) }
        }
        set(value) = setLocation(value.x, value.y)

    /** Component.setLocation(int, int)-like: moves the window's top-left to ([x], [y]) (DIP) on screen (AppWindow.Move). */
    fun setLocation(x: Int, y: Int) {
        val scale = dpiScale
        appWindow.move(toPixels(x, scale), toPixels(y, scale))
    }

    /** Component.getX-like: the on-screen position of the window's left edge (DIP). */
    val x: Int get() = location.x

    /** Component.getY-like: the on-screen position of the window's top edge (DIP). */
    val y: Int get() = location.y

    /** Component.getBounds / setBounds(Rectangle)-like: the rectangle combining [location] and [size] (DIP). Setting is the same as [setBounds]. */
    var bounds: WRectangle
        get() {
            val location = location
            val size = size
            return WRectangle(location.x, location.y, size.width, size.height)
        }
        set(value) = setBounds(value.x, value.y, value.width, value.height)

    /** Component.setBounds(int, int, int, int)-like: changes the position and size (DIP). The size is handled the same as [setSize]. */
    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        setLocation(x, y)
        setSize(width, height)
    }

    /**
     * Window.setMinimumSize / getMinimumSize-like: the minimum size the user can shrink it to (DIP; OverlappedPresenter.PreferredMinimumWidth /
     * PreferredMinimumHeight). null removes the constraint (default). Setting a value larger than the current size enlarges the window to it (as in Swing).
     * It is converted to physical pixels with the scale factor at the time it is set. With presenters such as full screen it only enlarges, and no constraint applies.
     *
     * @throws IllegalArgumentException if the width or height is negative
     */
    var minimumSize: WDimension? = null
        set(value) {
            require(value == null || (value.width >= 0 && value.height >= 0)) { "The minimumSize width and height must be 0 or more: $value" }
            field = value
            val scale = dpiScale
            withOverlapped { presenter ->
                presenter.preferredMinimumWidth = value?.let { toPixels(it.width, scale) }
                presenter.preferredMinimumHeight = value?.let { toPixels(it.height, scale) }
            }
            if (value != null) growToMinimum()
        }

    /** Enlarges it if the current size is smaller than [minimumSize]. */
    private fun growToMinimum() {
        val minimum = minimumSize ?: return
        val current = size
        if (current.width < minimum.width || current.height < minimum.height) setSize(current.width, current.height)
    }

    /**
     * Window.pack-like: fits the client area (the part excluding the title bar) to the size of the content (Window.Content) measured without constraints
     * (UIElement.Measure → IAppWindow2.ResizeClient). It can be used before or after it is shown. Enlarges it if smaller than [minimumSize].
     */
    fun pack() {
        val root = rootContent.uiElement
        // Before it is shown, Measure alone does not apply templates, so buttons and the like are measured small (0 wide). First apply
        // the templates with UpdateLayout, then measure again without constraints (doc/verification.md)
        root.call(XamlInterop.IUIElement_UpdateLayout)
        Ffi.backend.withScope { scope ->
            val available = scope.allocate(XamlStructs.SIZE_FLOAT)
            Ffi.backend.memory.putFloat(available.ptr, 0, Float.POSITIVE_INFINITY)
            Ffi.backend.memory.putFloat(available.ptr, 4, Float.POSITIVE_INFINITY)
            root.call(XamlInterop.IUIElement_Measure, available)
        }
        val desired = XamlStructs.getSizeFloat(root, XamlInterop.IUIElement_get_DesiredSize)
        val scale = dpiScale
        appWindow.resizeClient(ceil(desired[0] * scale).toInt(), ceil(desired[1] * scale).toInt())
        growToMinimum()
    }

    /**
     * Window.setLocationRelativeTo-like: if null, puts the window at the center of the primary display's work area (the part excluding the taskbar).
     * If a component, aligns the window's center with the center of its on-screen rectangle, and shifts it so that it does not extend outside the
     * work area of the display the component is on (as in Swing). A component not in a window is treated the same as null.
     */
    fun setLocationRelativeTo(component: WComponent?) {
        val target = component?.let { screenBoundsOf(it) }
        val own = appWindow.size
        val area = if (target == null) {
            WDisplayArea.primaryWorkArea()
        } else {
            WDisplayArea.workAreaAt(target.x + target.width / 2, target.y + target.height / 2)
        }
        val center = target ?: area
        val x = center.x + (center.width - own.width) / 2
        val y = center.y + (center.height - own.height) / 2
        appWindow.move(
            x.coerceAtMost(area.x + area.width - own.width).coerceAtLeast(area.x),
            y.coerceAtMost(area.y + area.height - own.height).coerceAtLeast(area.y),
        )
    }

    /** The on-screen rectangle of [component] (physical pixels). null if it is not in a window. */
    private fun screenBoundsOf(component: WComponent): WRectangle? {
        val root = component.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
        val scale = try {
            root.getDouble(XamlInterop.IXamlRoot_get_RasterizationScale)
        } finally {
            root.release()
        }
        val origin = Xaml.positionOnScreen(component) ?: return null
        return WRectangle(
            origin[0],
            origin[1],
            (component.actualWidth * scale).roundToInt(),
            (component.actualHeight * scale).roundToInt(),
        )
    }

    /** Container.add(Component)-like: adds a child to the current content ([contentPane], or the container replaced with [setContentPane]). */
    fun add(component: WComponent) = currentContainer().add(component)

    /**
     * Container.add(Component, Object)-like: adds a child with a constraint to the current content (the `frame.add(panel, WBorderLayout.CENTER)` form).
     * If the content is a [WLayoutPanel] with a layout manager (after setting [layout] etc.), the constraint is passed to that manager.
     *
     * Difference from Swing: the default content ([contentPane]) is a vertical StackPanel, not a BorderLayout, so set [layout] first.
     * A non-null constraint passed to content without a layout manager is not silently discarded as in Swing but throws [IllegalArgumentException]
     * (the child is not added). With a null constraint it is the same as [add].
     */
    fun add(component: WComponent, constraints: Any?) = ContentPanes.add(currentContainer(), component, constraints, -1)

    /** Container.add(Component, int)-like: adds a child at [index] of the current content (-1 for the end). */
    fun add(component: WComponent, index: Int) = currentContainer().add(component, index)

    /**
     * Container.add(Component, Object, int)-like: adds a child with a constraint at [index] of the current content (-1 for the end).
     * [IllegalArgumentException] if [index] is out of range (as in Swing). Constraints are handled the same as add(component, constraints).
     */
    fun add(component: WComponent, constraints: Any?, index: Int) = ContentPanes.add(currentContainer(), component, constraints, index)

    /**
     * JFrame.getLayout / setLayout-like: the layout manager that arranges the children of the content. The manager if the content is a [WLayoutPanel],
     * otherwise (such as the vertical StackPanel of the default [contentPane]) null.
     *
     * When set and the content is the default [contentPane], the content is replaced with a [WLayoutPanel] with that manager and the children of [contentPane] are moved
     * in the same order (null corresponds to Swing's null layout, a WLayoutPanel without a manager). The constraints of the moved children are null
     * (the center for [WBorderLayout]). If the content is already a [WLayoutPanel], its manager is replaced without replacing the content.
     *
     * Differences from Swing:
     * - The default is null, not BorderLayout (the default content stays a vertical StackPanel as before). To use the `frame.add(c, WBorderLayout.NORTH)`
     *   form, set this first.
     * - Because the content is replaced, [contentPane] is not on screen after setting it (to keep the type [WPanel]; the same as after [setContentPane]).
     *   Add children with `frame.add(...)`.
     *
     * @throws IllegalStateException when [setContentPane] has made a component other than a [WLayoutPanel] the content (the content is not replaced)
     */
    var layout: WLayoutManager?
        get() = (content as? WLayoutPanel)?.layout
        set(value) {
            val current = content
            when {
                current is WLayoutPanel -> current.layout = value
                current === contentPane -> setContentPane(ContentPanes.movedToLayoutPanel(contentPane, value))
                else -> error(
                    "A layout manager cannot be attached to the content replaced with setContentPane (${current.javaClass.simpleName}). " +
                        "Use a WLayoutPanel as the content",
                )
            }
        }

    /** Container.remove(Component)-like: removes a child from the current content. */
    fun remove(component: WComponent) = currentContainer().remove(component)

    /**
     * Component.invalidate-like: records that the size of the window content (the root including the menu bar, if any) may change
     * ([WComponent.invalidate]).
     */
    fun invalidate() = rootContent.invalidate()

    /**
     * Component.revalidate-like: schedules a relayout of the window content ([WComponent.revalidate]). Swing code's
     * `frame.revalidate(); frame.repaint();` can be written as is.
     */
    fun revalidate() = rootContent.revalidate()

    /** Container.validate-like: performs the scheduled relayout of the content now and finishes the layout ([WComponent.validate]). */
    fun validate() = rootContent.validate()

    /** Component.repaint-like: does nothing (XAML redraws by itself; [WComponent.repaint]). */
    fun repaint() = rootContent.repaint()

    private fun currentContainer(): WContainer = content as? WContainer ?: error(
        "The content replaced with setContentPane (${content.javaClass.simpleName}) is not a container, so children cannot be added. Add them to the content's container directly",
    )

    /**
     * Equivalent to JFrame.setContentPane: replaces the root with [component] instead of the
     * default [contentPane] (Window.Content). Useful when you want a full-bleed layout with,
     * say, a Grid instead of StackPanel's unconstrained height measurement.
     * Subsequent [add] / [remove] apply to [component] (if it is a container). [contentPane] stays the default panel.
     * If [component] is in another container, it is removed from there first (as in Swing).
     */
    fun setContentPane(component: WComponent) {
        val grid = rootGrid
        if (grid == null) {
            component.removeFromParent()
            window.call(XamlInterop.IWindow_put_Content, component.uiElement.ptr)
        } else {
            grid.remove(content)
            grid.add(component, CONTENT_ROW, 0)
        }
        content = component
    }

    /**
     * JFrame.getJMenuBar / setJMenuBar-like: the menu bar placed at the top of the window (below the title bar, above the content). null removes it.
     * When first set, replaces Window.Content with a Grid that has a menu row and a content row (the element tree of windows without one stays as before).
     * The Grid remains after removal, and the content row spreads over the whole.
     */
    var jMenuBar: WMenuBar? = null
        set(value) {
            val previous = field
            if (previous === value) return
            val grid = rootGrid ?: createRootGrid()
            if (previous != null) grid.remove(previous)
            field = value
            if (value != null) grid.add(value, MENU_ROW, 0)
        }

    private fun createRootGrid(): WGrid {
        val grid = WGrid()
        grid.addRow(GridLength.AUTO)
        grid.addRow(GridLength.star())
        // First replace Window.Content with the Grid to detach the content from the window, then put it into the Grid
        window.call(XamlInterop.IWindow_put_Content, grid.uiElement.ptr)
        grid.add(content, CONTENT_ROW, 0)
        rootGrid = grid
        return grid
    }

    /**
     * JRootPane.getDefaultButton / setDefaultButton-like: the window's default button. It is pressed by Enter with no
     * modifier keys and shown with the accent appearance ([WButton.isDefaultButton]). Changing to another button or null
     * makes the previous button stop being the default button.
     * It is implemented with a WinUI keyboard accelerator, so it is not pressed while focus is on a control that handles
     * Enter itself (if another WButton has focus, that button is pressed). In a single-line [WTextField], it is pressed
     * only when that field has no ActionListener (same as JTextField; [WTextField.addActionListener]). A disabled button
     * is not pressed.
     * When the window is closed, the button is removed from Enter dispatching (the value that was set and
     * [WButton.isDefaultButton] remain as they are).
     */
    var defaultButton: WButton? = null
        set(value) {
            val previous = field
            if (previous === value) return
            field = value
            previous?.leaveDefaultButton(this)
            value?.becomeDefaultButton(this)
        }

    /**
     * Component.setVisible / isVisible-like: true shows the window and brings it to the front (Window.Activate), and false hides it without closing
     * (AppWindow.Hide; as in Swing, a hidden window can be shown again with true). Use [dispose] to close it.
     * Reading returns the native visibility (AppWindow.IsVisible), and false after it is closed.
     * Setting true after it is closed throws [IllegalStateException] (unlike Swing, it cannot be shown again), and setting false does nothing.
     */
    var isVisible: Boolean
        get() = !closed && appWindow.isVisible
        set(value) {
            if (value) {
                check(!closed) { "A closed (disposed) WFrame cannot be shown again" }
                registerOpen()
                val pending = pendingState
                pendingState = null
                // Maximizing is done together with showing (so it does not first appear at the normal size). Minimizing is done after showing (because Activate restores it)
                if (pending != null && pending != ICONIFIED) applyState(pending)
                if (isAutoRequestFocus) window.call(XamlInterop.IWindow_Activate) else appWindow.show(activate = false)
                if (pending == ICONIFIED) applyState(pending)
            } else if (!closed) {
                appWindow.hide()
            }
        }

    /** Window.isShowing-like: the same as [isVisible] (a top-level window has no parent that hides it). */
    val isShowing: Boolean get() = isVisible

    /**
     * Window.isAutoRequestFocus / setAutoRequestFocus-like: whether to activate this window when showing it with [isVisible] = true and on [toFront]
     * (true by default). If false, showing does not activate it (AppWindow.Show(false); it does not steal the focus from the window being worked in),
     * and [toFront] only brings it to the front. It becomes active when the user selects the window.
     */
    var isAutoRequestFocus: Boolean = true

    // ---- State and decorations ----

    /** The value put into [extendedState] while hidden ([NORMAL] / [ICONIFIED] / [MAXIMIZED_BOTH]). Applied the next time it is shown. */
    private var pendingState: Int? = null

    /**
     * Frame.getExtendedState / setExtendedState-like: the normal ([NORMAL]), iconified ([ICONIFIED]) or maximized ([MAXIMIZED_BOTH]) state
     * (OverlappedPresenter.State and Restore / Minimize / Maximize). Windows has no horizontal-only or vertical-only maximization, so
     * [MAXIMIZED_HORIZ] and [MAXIMIZED_VERT] are treated as [MAXIMIZED_BOTH], and values including [ICONIFIED] (ICONIFIED | MAXIMIZED_BOTH, etc.)
     * minimize it. The value read is one of [NORMAL], [ICONIFIED] and [MAXIMIZED_BOTH].
     * A value set while hidden (also before it is shown) is applied the next time it is shown with [isVisible] = true (as in Swing, setting alone does not show it).
     * With presenters such as full screen, it reads as [NORMAL] and setting does nothing. Setting after it is closed also does nothing.
     */
    var extendedState: Int
        get() = pendingState ?: readState() ?: NORMAL
        set(value) {
            if (closed) return
            val state = normalizeState(value)
            if (appWindow.isVisible) {
                pendingState = null
                applyState(state)
            } else {
                pendingState = state
            }
        }

    /**
     * Frame.getState / setState-like: [ICONIFIED] if minimized, otherwise [NORMAL] (maximized is also NORMAL).
     * As in Swing, setting [ICONIFIED] minimizes it (if not minimized), and [NORMAL] only undoes the minimization (a window minimized from maximized
     * goes back to maximized, the same as Windows' restore). Other values, and setting the state it is already in, do nothing.
     * A setting while hidden is applied the next time it is shown, the same as [extendedState].
     */
    var state: Int
        get() = if (extendedState and ICONIFIED != 0) ICONIFIED else NORMAL
        set(value) {
            val iconified = extendedState and ICONIFIED != 0
            when {
                value == ICONIFIED && !iconified -> extendedState = ICONIFIED
                value == NORMAL && iconified -> deiconify()
            }
        }

    /** Undoes the minimization (OverlappedPresenter.Restore once; if minimized from maximized, it goes back to maximized). */
    private fun deiconify() {
        if (closed) return
        if (!appWindow.isVisible) {
            pendingState = NORMAL
            return
        }
        applyingState = true
        try {
            withOverlapped { it.restore() }
        } finally {
            applyingState = false
        }
        syncState()
    }

    private fun normalizeState(value: Int): Int = when {
        value and ICONIFIED != 0 -> ICONIFIED
        value and MAXIMIZED_BOTH != 0 -> MAXIMIZED_BOTH
        else -> NORMAL
    }

    /** true inside [applyState] (no events for intermediate states; compared only once at the end). */
    private var applyingState = false

    private fun applyState(state: Int) {
        applyingState = true
        try {
            withOverlapped { presenter ->
                when (state) {
                    ICONIFIED -> presenter.minimize()
                    MAXIMIZED_BOTH -> presenter.maximize()
                    else -> {
                        presenter.restore()
                        // A window minimized from maximized goes back to maximized with Restore (Windows' SW_RESTORE). Restore once more to make it normal
                        if (presenter.state == OverlappedPresenterState.MAXIMIZED) presenter.restore()
                    }
                }
            }
        } finally {
            applyingState = false
        }
        if (!closed) syncState()
    }

    /**
     * Frame.isResizable / setResizable-like: true (default) if the user can resize it with the border and maximize it.
     * Changes OverlappedPresenter's IsResizable and IsMaximizable together (as a non-resizable JFrame has no maximize button).
     * Programmatic [setSize] and [pack] work even when false. With presenters such as full screen, it reads as true and setting does nothing.
     */
    var isResizable: Boolean
        get() = withOverlapped { it.isResizable } ?: true
        set(value) {
            withOverlapped {
                it.isResizable = value
                it.isMaximizable = value
            }
        }

    /**
     * Window.isAlwaysOnTop / setAlwaysOnTop-like: true to always keep it in front of other apps' windows (OverlappedPresenter.IsAlwaysOnTop).
     * The default is false. With presenters such as full screen, it reads as false and setting does nothing.
     */
    var isAlwaysOnTop: Boolean
        get() = withOverlapped { it.isAlwaysOnTop } ?: false
        set(value) {
            withOverlapped { it.isAlwaysOnTop = value }
        }

    /** Window.isAlwaysOnTopSupported-like: always true ([isAlwaysOnTop] can always be used on Windows). */
    val isAlwaysOnTopSupported: Boolean get() = true

    /**
     * Frame.isUndecorated / setUndecorated-like: true to remove the border and title bar (OverlappedPresenter.SetBorderAndTitleBar(false, false)).
     * The default is false. Unlike Swing, it may be changed after it is shown (no IllegalComponentStateException).
     * With presenters such as full screen, it reads as false and setting does nothing.
     */
    var isUndecorated: Boolean
        get() = withOverlapped { !it.hasBorder && !it.hasTitleBar } ?: false
        set(value) {
            withOverlapped { it.setBorderAndTitleBar(!value, !value) }
        }

    /**
     * Window.toFront-like: if shown, brings it in front of other windows and activates it (IAppWindow2.MoveInZOrderAtTop and
     * Window.Activate). Does nothing while hidden or after it is closed (as in Swing). Whether it can come to the foreground while another app is in front
     * depends on Windows' foreground rules. If [isAutoRequestFocus] is false, it is only brought to the front without being activated (as in Swing).
     */
    fun toFront() {
        if (!isVisible) return
        appWindow.moveInZOrderAtTop()
        if (isAutoRequestFocus) window.call(XamlInterop.IWindow_Activate)
    }

    /** Window.toBack-like: if shown, sends it behind other windows (IAppWindow2.MoveInZOrderAtBottom). Does nothing while hidden or after it is closed. */
    fun toBack() {
        if (!isVisible) return
        appWindow.moveInZOrderAtBottom()
    }

    /** The path of the icon file last set with [setIconImage] (as passed). null if not set. */
    var iconImage: String? = null
        private set

    /**
     * Frame.setIconImage-like: makes the image in file [path] the icon of the title bar and taskbar (AppWindow.SetIcon).
     * Besides .ico, .png can be used (an .ico holding the PNG as is as one image is created in a temporary file and passed; the temporary file is deleted at exit).
     *
     * @throws IllegalArgumentException if the file does not exist, the extension is not .ico or .png, or it cannot be read as a PNG
     */
    fun setIconImage(path: String) {
        val file = File(path)
        require(file.isFile) { "The icon file does not exist: $path" }
        val icoPath = when (file.extension.lowercase()) {
            "ico" -> file.absolutePath
            "png" -> IcoFiles.temporaryIcoFromPng(file.readBytes()).absolutePath
            else -> throw IllegalArgumentException("The icon must be a .ico or .png file: $path")
        }
        appWindow.setIcon(icoPath)
        iconImage = path
    }

    /**
     * Frame.setIconImage(Image)-like: makes the file of [icon] the icon (same as [setIconImage]).
     *
     * @throws IllegalArgumentException for images that are not files (file URIs) (http:// or ms-appx:///), when it is not a valid URI
     *   (when passing a URI that does not escape spaces etc.; passing a path may include spaces), or when the file conditions are not met
     */
    fun setIconImage(icon: WImageIcon) {
        val uri = try {
            URI(icon.uri)
        } catch (e: URISyntaxException) {
            // A URI as passed (such as "file:///C:/my icons/a.png" with unescaped spaces) cannot be read as a java.net.URI
            throw IllegalArgumentException("Cannot be read as an icon URI (pass a path for a file): ${icon.path}", e)
        }
        require(uri.scheme.equals("file", ignoreCase = true)) { "Only file images can be used for the icon: ${icon.path}" }
        setIconImage(File(uri).path)
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

    companion object {
        /** The same value as Frame.NORMAL: the normal state. */
        const val NORMAL: Int = 0

        /** The same value as Frame.ICONIFIED: the iconified state. */
        const val ICONIFIED: Int = 1

        /** The same value as Frame.MAXIMIZED_HORIZ (Windows has no horizontal-only maximization). */
        const val MAXIMIZED_HORIZ: Int = 2

        /** The same value as Frame.MAXIMIZED_VERT (Windows has no vertical-only maximization). */
        const val MAXIMIZED_VERT: Int = 4

        /** The same value as Frame.MAXIMIZED_BOTH: the maximized state. */
        const val MAXIMIZED_BOTH: Int = 6

        /** Same as [WWindowConstants.DO_NOTHING_ON_CLOSE] (so that Kotlin can write `WFrame.DO_NOTHING_ON_CLOSE`). */
        const val DO_NOTHING_ON_CLOSE: Int = WWindowConstants.DO_NOTHING_ON_CLOSE

        /** Same as [WWindowConstants.HIDE_ON_CLOSE]. */
        const val HIDE_ON_CLOSE: Int = WWindowConstants.HIDE_ON_CLOSE

        /** Same as [WWindowConstants.DISPOSE_ON_CLOSE]. */
        const val DISPOSE_ON_CLOSE: Int = WWindowConstants.DISPOSE_ON_CLOSE

        /** Same as [WWindowConstants.EXIT_ON_CLOSE]. */
        const val EXIT_ON_CLOSE: Int = WWindowConstants.EXIT_ON_CLOSE

        /** The menu row and content row of the root Grid when [jMenuBar] is set. */
        private const val MENU_ROW = 0
        private const val CONTENT_ROW = 1

        /** The DPI at 100% scaling. */
        private const val BASE_DPI = 96.0

        /** MicaKind.BaseAlt (value extracted from the winmd). */
        private const val MICA_KIND_BASE_ALT = 1

        /**
         * The WFrames that have been shown and are not closed. In the order first shown, with ones that become active moved to the end. Holds WFrames strongly until closed
         * so that the native handlers (weak references) can reach them. Read and written only by the UI thread.
         */
        private val openFrames = LinkedHashSet<WFrame>()

        /** The hooks called when a window closes ([addWindowClosedHook]). Read and written only by the UI thread. */
        private val windowClosedHooks = ArrayList<WindowClosedHook>()

        /**
         * Calls [hook] when a window closes (Window.Closed; both for × and [dispose]) (internal). Popups, dialogs and flyouts whose owner window
         * was closed while they were open get no closed notification (Closed), so this releases their while-open state.
         * Call on the UI thread.
         */
        internal fun addWindowClosedHook(hook: WindowClosedHook) {
            windowClosedHooks += hook
        }

        /** Removes a hook added with [addWindowClosedHook]. */
        internal fun removeWindowClosedHook(hook: WindowClosedHook) {
            windowClosedHooks.remove(hook)
        }

        /**
         * Frame.getFrames-like: an array of the WFrames that have been shown and are not yet closed, including hidden ones. Unlike Swing, closed ones and
         * ones never shown are not included. Call it on the UI thread.
         */
        @JvmStatic
        val frames: Array<WFrame>
            get() = openFrames.toTypedArray()

        /**
         * The shown active window. If none, the shown window that became active (or was created) last. null if neither exists.
         * A window right after being hidden stays [isActive] until Activated (inactive) arrives, so whether it is shown is checked too.
         */
        internal fun activeOrRecentFrame(): WFrame? =
            openFrames.firstOrNull { it.isActive && it.isVisible } ?: openFrames.lastOrNull { it.isVisible }

        /**
         * The component of the window to open a dialog ([WOptionPane], [WFileChooser]) on: [parent] if it is a component,
         * the content of a [WFrame] ([componentOf]), and the content of [activeOrRecentFrame] if null. [IllegalStateException] if none exists.
         * Call on the UI thread.
         */
        internal fun dialogOwnerOf(parent: WDialogParent?): WComponent {
            if (parent != null) return componentOf(parent)
            val frame = checkNotNull(activeOrRecentFrame()) {
                "The parent is null and there is no shown window (WFrame) to open on. Show a window, or pass a component that is in a window"
            }
            return frame.content
        }

        /** The component placed in the window of the dialog parent [parent] (itself for a component, the current content for a [WFrame]). */
        internal fun componentOf(parent: WDialogParent): WComponent = when (parent) {
            is WComponent -> parent
            is WFrame -> parent.content
        }
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

/** A hook for [WFrame.addWindowClosedHook]. */
internal fun interface WindowClosedHook {
    /** A window closed. [xamlRoot] is the XamlRoot of that window (borrowed; not Released). */
    fun windowClosed(xamlRoot: ComPtr)
}

/** Whether the [WFrame] has been counted as an open window (shared by the Window.Closed handler and the WFrame; does not reference the WFrame). */
private class OpenCount {
    var counted = false
}
