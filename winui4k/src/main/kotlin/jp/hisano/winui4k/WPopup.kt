package jp.hisano.winui4k

import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winrt.addEventHandler
import jp.hisano.winui4k.internal.winrt.removeEventHandler
import jp.hisano.winui4k.internal.winui.Abi

/**
 * javax.swing.Popup-like: WinUI 3's Primitives.Popup. A lightweight container that shows
 * arbitrary content ([child]) layered at an offset position from the window's top-left origin.
 * Unlike Flyout, it has no decoration (background/shadow), so decorate the content side with
 * something like WBorder.
 */
class WPopup(child: WComponent? = null) : WComponent(
    Activation.activate(Abi.CLS_Popup).queryInterface(Abi.IID_IPopup), // created via the default factory
) {
    /** Listener -> event token (used to remove). */
    private val closeTokens = ListenerTokens<() -> Unit>()

    /** The content shown in the popup (Popup.Child). */
    var child: WComponent? = null
        set(value) {
            field = value
            inspectable.call(Abi.IPopup_put_Child, value?.uiElement?.ptr)
        }

    /** The horizontal offset from the origin (the window's top-left) (Popup.HorizontalOffset). */
    var horizontalOffset: Double
        get() = inspectable.getDouble(Abi.IPopup_get_HorizontalOffset)
        set(value) = inspectable.call(Abi.IPopup_put_HorizontalOffset, value)

    /** The vertical offset from the origin (the window's top-left) (Popup.VerticalOffset). */
    var verticalOffset: Double
        get() = inspectable.getDouble(Abi.IPopup_get_VerticalOffset)
        set(value) = inspectable.call(Abi.IPopup_put_VerticalOffset, value)

    /** Whether it closes automatically on an outside click (Popup.IsLightDismissEnabled). */
    var isLightDismissEnabled: Boolean
        get() = inspectable.getBool(Abi.IPopup_get_IsLightDismissEnabled)
        set(value) = inspectable.putBool(Abi.IPopup_put_IsLightDismissEnabled, value)

    val isOpen: Boolean
        get() = inspectable.getBool(Abi.IPopup_get_IsOpen)

    init {
        if (child != null) this.child = child
    }

    /**
     * Opens the popup (Popup.IsOpen = true). Shown in the same window as [owner]
     * (inherits its XamlRoot). Pass an already-shown element.
     */
    fun show(owner: WComponent) {
        // A Popup outside the XAML tree requires the XamlRoot to show in
        val root = owner.uiElement.getPtr(Abi.IUIElement_get_XamlRoot)
        uiElement.call(Abi.IUIElement_put_XamlRoot, root.ptr)
        root.release()
        inspectable.putBool(Abi.IPopup_put_IsOpen, true)
    }

    fun hide() {
        inspectable.putBool(Abi.IPopup_put_IsOpen, false)
    }

    /** Registers a listener called when it closes (Popup.Closed). Also called on a light dismiss. */
    fun addCloseListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.PopupClosedHandler", Abi.IID_EventHandler_Object, Abi.IPopup_add_Closed,
        ) { _, _ -> listener() }
        closeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCloseListener]. */
    fun removeCloseListener(listener: () -> Unit) {
        val token = closeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IPopup_remove_Closed, token)
    }
}
