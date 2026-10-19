package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A shared helper that measures the size of a string drawn in a font. [WTextField.columns] uses it for the column width,
 * and it can also be used for WTextPane's rows / columns.
 *
 * WinUI4K does not call UIElement.Measure (because the float struct Size passed by value is mishandled by some FFI
 * backends; see Xaml), so the TextBlock to measure is put in an invisible (Opacity 0, no hit testing) Popup opened in the
 * displayed window (XamlRoot), laid out synchronously with UpdateLayout to read DesiredSize, and closed right away.
 * Results for the same font and string are cached.
 */
internal object TextMetrics {
    private val cache = HashMap<Pair<WFont, String>, WSize>()

    /**
     * The size (DIP) of [text] drawn on one line in [font]. [host] is a displayed element, and the measurement is done in
     * its XamlRoot (window). null if [host] is not placed in a window.
     */
    fun measure(host: WComponent, font: WFont, text: String): WSize? {
        cache[font to text]?.let { return it }
        val root = host.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return null
        val size = try {
            measureIn(root, font, text)
        } finally {
            root.release()
        }
        cache[font to text] = size
        return size
    }

    private fun measureIn(root: ComPtr, font: WFont, text: String): WSize {
        val probe = WLabel(text)
        probe.font = font
        probe.horizontalAlignment = HorizontalAlignment.LEFT
        probe.verticalAlignment = VerticalAlignment.TOP
        val popup = Activation.activate(XamlInterop.CLS_Popup, XamlInterop.IID_IPopup)
        val popupElement = popup.queryInterface(XamlInterop.IID_IUIElement)
        try {
            popupElement.call(XamlInterop.IUIElement_put_XamlRoot, root.ptr)
            popupElement.call(XamlInterop.IUIElement_put_Opacity, 0.0)
            popupElement.putBool(XamlInterop.IUIElement_put_IsHitTestVisible, false)
            popup.call(XamlInterop.IPopup_put_Child, probe.uiElement.ptr)
            popup.putBool(XamlInterop.IPopup_put_IsOpen, true)
            try {
                probe.updateLayout()
                return probe.readDesiredSize()
            } finally {
                popup.putBool(XamlInterop.IPopup_put_IsOpen, false)
                popup.call(XamlInterop.IPopup_put_Child, null)
            }
        } finally {
            popupElement.release()
            popup.release()
        }
    }
}
