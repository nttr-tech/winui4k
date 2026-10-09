package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WPopup
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A transparent layer covering the whole window, used to close a popup when the user clicks outside it.
 *
 * A Popup (WPopup) that is not a child of the window's elements, when opened with IsLightDismissEnabled, does not close on
 * an outside click and instead captures all input to the window (doc/troubleshooting.md). So this layer is opened right
 * before the popup (the popup opened later stacks on top), and when the layer is pressed, [onDismiss] closes the popup.
 * As with WinUI's light dismiss, the click does not reach the elements below.
 */
internal class RibbonDismissLayer(private val onDismiss: () -> Unit) {
    private val catcher = XamlElement.load("<Grid Background=\"Transparent\" />")
    private val popup = WPopup(catcher)

    init {
        popup.isLightDismissEnabled = false
        catcher.onPointer(XamlInterop.IUIElement_add_PointerPressed) { e ->
            e.handled = true
            hide()
            onDismiss()
        }
    }

    /** Whether the layer is open. */
    val isOpen: Boolean get() = popup.isOpen

    /** Opens the layer covering the whole window of [owner] (open it before the popup). */
    fun show(owner: WComponent) {
        val size = Xaml.rootSize(owner) ?: return
        catcher.setSize(size[0], size[1])
        popup.horizontalOffset = 0.0
        popup.verticalOffset = 0.0
        if (!popup.isOpen) popup.show(owner)
    }

    /** Closes the layer. */
    fun hide() {
        if (popup.isOpen) popup.hide()
    }
}
