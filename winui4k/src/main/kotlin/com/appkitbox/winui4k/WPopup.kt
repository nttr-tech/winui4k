package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * javax.swing.Popup-like: WinUI 3's Primitives.Popup. A lightweight container that shows
 * arbitrary content ([child]) layered at an offset position from the window's top-left origin.
 * Unlike Flyout, it has no decoration (background/shadow), so decorate the content side with
 * something like WBorder.
 * It also has [show] (owner, x, y), which opens at coordinates of the owner like PopupFactory.getPopup(owner, contents, x, y), and the JPopupMenu-like [isVisible] and
 * [addPopupMenuListener]. Unlike Swing's heavyweight (separate-window) popups, it is drawn only inside the window
 * (ShouldConstrainToRootBounds stays at the default true).
 */
class WPopup @JvmOverloads constructor(child: WComponent? = null) : WComponent(
    Activation.activate(XamlInterop.CLS_Popup, XamlInterop.IID_IPopup), // created via the default factory
) {
    /** Listener -> event token (used to remove). */
    private val closeTokens = ListenerTokens<Runnable>()
    private val openTokens = ListenerTokens<Runnable>()

    /** The content shown in the popup (Popup.Child). */
    var child: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.IPopup_put_Child, value?.uiElement?.ptr)
        }

    /** The horizontal offset from the origin (the window's top-left) (Popup.HorizontalOffset). */
    var horizontalOffset: Double
        get() = inspectable.getDouble(XamlInterop.IPopup_get_HorizontalOffset)
        set(value) = inspectable.call(XamlInterop.IPopup_put_HorizontalOffset, value)

    /** The vertical offset from the origin (the window's top-left) (Popup.VerticalOffset). */
    var verticalOffset: Double
        get() = inspectable.getDouble(XamlInterop.IPopup_get_VerticalOffset)
        set(value) = inspectable.call(XamlInterop.IPopup_put_VerticalOffset, value)

    /** Whether it closes automatically on an outside click (Popup.IsLightDismissEnabled). */
    var isLightDismissEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.IPopup_get_IsLightDismissEnabled)
        set(value) = inspectable.putBool(XamlInterop.IPopup_put_IsLightDismissEnabled, value)

    val isOpen: Boolean
        get() = inspectable.getBool(XamlInterop.IPopup_get_IsOpen)

    init {
        if (child != null) this.child = child
    }

    /** The owner last passed to [show] (used when reopening with `isVisible = true`). */
    private var lastOwner: WComponent? = null

    /**
     * Opens the popup (Popup.IsOpen = true). Shown in the same window as [owner]
     * (inherits its XamlRoot). Pass an already-shown element. The position stays at [horizontalOffset] / [verticalOffset].
     */
    fun show(owner: WComponent) {
        lastOwner = owner
        popupMenuEvents?.hideRequested = false
        // A Popup outside the XAML tree requires the XamlRoot to show in
        val root = owner.uiElement.getPtr(XamlInterop.IUIElement_get_XamlRoot)
        uiElement.call(XamlInterop.IUIElement_put_XamlRoot, root.ptr)
        root.release()
        keepWhileOpen()
        inspectable.putBool(XamlInterop.IPopup_put_IsOpen, true)
    }

    /** Whether the Closed subscription for [keepWhileOpen] is done. */
    private var closedSubscribed = false

    /**
     * Holds this wrapper strongly while it is open (as with a shown Swing popup, it is not collected until closed even if the reference is released,
     * and can be the source of listeners). A Popup is not in the tree, so [ListenerAnchor] does not hold it. Released on Closed.
     */
    private fun keepWhileOpen() {
        openPopups += this
        if (closedSubscribed) return
        closedSubscribed = true
        val self = WeakReference(this)
        inspectable.addEventHandler(
            "WinUI4K.PopupKeepWhileOpenClosed",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.IPopup_add_Closed,
        ) { _, _ -> self.get()?.let { openPopups -= it } }
    }

    /**
     * PopupFactory.getPopup(owner, contents, x, y) + Popup.show-like: opens at ([x], [y]) (DIP) from the top-left of [owner].
     * Adds them to the position of [owner] within its window (the sum of the parents' ActualOffset) and sets [horizontalOffset] / [verticalOffset].
     * Swing's x / y are screen coordinates, but WinUI popups cannot go outside the window, so they are taken in the owner's coordinates
     * (same as JPopupMenu.show(invoker, x, y)). When [owner] is inside a popup, the position is relative to the top-left of that popup.
     */
    fun show(owner: WComponent, x: Double, y: Double) {
        val origin = Xaml.positionInRoot(owner.uiElement)
        horizontalOffset = origin[0] + x
        verticalOffset = origin[1] + y
        show(owner)
    }

    /**
     * The form taking Int coordinates, as with JPopupMenu.show(Component, int, int) (so Kotlin can write `popup.show(e.component, e.x, e.y)`).
     * The same as the Double [show].
     */
    fun show(owner: WComponent, x: Int, y: Int) = show(owner, x.toDouble(), y.toDouble())

    /** Closes it (Popup.IsOpen = false). This way of closing does not call [WPopupMenuListener.popupMenuCanceled]. */
    fun hide() {
        popupMenuEvents?.hideRequested = true
        inspectable.putBool(XamlInterop.IPopup_put_IsOpen, false)
    }

    /**
     * JPopupMenu.isVisible / setVisible-like: whether it is open (same as [isOpen]). Setting false calls [hide]; setting true,
     * if closed, opens it at the current position in the window of the owner last passed to [show] (IllegalStateException if it has never been opened).
     * Unlike other components, this is not UIElement.Visibility (a popup takes no layout space, and opening and closing is how it appears).
     */
    override var isVisible: Boolean
        get() = isOpen
        set(value) {
            if (!value) {
                hide()
            } else if (!isOpen) {
                show(checkNotNull(lastOwner) { "There is no owner to show it on. Open it with show(owner) or show(owner, x, y)" })
            }
        }

    /** Calls the [WPopupMenuListener]s (created with the first listener). */
    private var popupMenuEvents: PopupPopupMenuEvents? = null

    /**
     * JPopupMenu.addPopupMenuListener-like: adds an open/close listener. Popup has no events before opening or before closing, so
     * popupMenuWillBecomeVisible is called after opening (Opened), and after closing (Closed), popupMenuCanceled if it was a light dismiss (an outside click
     * with [isLightDismissEnabled]), then popupMenuWillBecomeInvisible, in the order added. [hide] is not a cancellation.
     */
    fun addPopupMenuListener(listener: WPopupMenuListener) {
        val events = popupMenuEvents ?: PopupPopupMenuEvents(WeakReference(this)).also { popupMenuEvents = it }
        events.add(inspectable, listener)
    }

    /** Removes a listener added with [addPopupMenuListener]. Does nothing if not registered. */
    fun removePopupMenuListener(listener: WPopupMenuListener) {
        popupMenuEvents?.remove(listener)
    }

    /** The added listeners (in the order added; a new array on each call). */
    val popupMenuListeners: Array<WPopupMenuListener>
        get() = popupMenuEvents?.all ?: emptyArray()

    /** Registers a listener called when it opens (Popup.Opened). */
    @JvmSynthetic
    fun addOpenListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addOpenListener(adapter)
        openTokens.addKotlinAdapter(listener, adapter)
    }

    fun addOpenListener(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.PopupOpenedHandler",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.IPopup_add_Opened,
        ) { _, _ -> listener.run() }
        openTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered with [addOpenListener]. */
    @JvmSynthetic
    fun removeOpenListener(listener: () -> Unit) {
        val adapter = openTokens.removeKotlinAdapter(listener) ?: return
        removeOpenListener(adapter)
    }

    fun removeOpenListener(listener: Runnable) {
        val token = openTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IPopup_remove_Opened, token)
    }

    /** Registers a listener called when it closes (Popup.Closed). Also called on a light dismiss. */
    @JvmSynthetic
    fun addCloseListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addCloseListener(adapter)
        closeTokens.addKotlinAdapter(listener, adapter)
    }

    fun addCloseListener(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.PopupClosedHandler",
            FoundationInterop.IID_EventHandler_Object,
            XamlInterop.IPopup_add_Closed,
        ) { _, _ -> listener.run() }
        closeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCloseListener]. */
    @JvmSynthetic
    fun removeCloseListener(listener: () -> Unit) {
        val adapter = closeTokens.removeKotlinAdapter(listener) ?: return
        removeCloseListener(adapter)
    }

    fun removeCloseListener(listener: Runnable) {
        val token = closeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IPopup_remove_Closed, token)
    }

    /** Whether this popup is shown on [xamlRoot] (the XamlRoot of the closed window). */
    private fun isShownIn(xamlRoot: ComPtr): Boolean {
        val own = uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return false
        return try {
            Xaml.sameObject(own, xamlRoot)
        } finally {
            own.release()
        }
    }

    private companion object {
        /** The open WPopups (read and written only by the UI thread). */
        val openPopups = HashSet<WPopup>()

        init {
            // When the window it is shown on closes while it is open, WinUI only sets IsOpen to false and does not raise Closed
            // (ContentRoot::Close → CPopup::ForceCloseForTreeReset; Popup.cpp), so release it here
            WFrame.addWindowClosedHook { root -> openPopups.removeAll { it.isShownIn(root) } }
        }
    }
}
