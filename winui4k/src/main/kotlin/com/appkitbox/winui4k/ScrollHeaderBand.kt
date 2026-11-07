package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * An opaque background band (StackPanel) that holds the headers of a [WScrollPane] (the column header, row header and top-left corner).
 *
 * ScrollViewer draws the headers (TopHeader / LeftHeader / TopLeftHeader) over the content, and a header's size stays the size of the header
 * component, so with headers without a background, content scrolled under a header shows through. Like the JViewport of the headers of Swing's JScrollPane,
 * the header is put on a band painted with the window's background color (SolidBackgroundFillColorBaseBrush), and the band's size is
 * expanded by [WScrollPane] to fit the content and the viewport ([setMinimumSize]). The header component is not stretched in the band's direction
 * (Swing's header components keep their preferred sizes too).
 *
 * @param owner the scroll pane that holds the band's reference (released along with it on collection)
 * @param horizontal true if the band lays out headers horizontally (the column header)
 * @param onSizeChanged called when the band's size changes (to fit the size of the top-left corner again)
 */
internal class ScrollHeaderBand(owner: WComponent, horizontal: Boolean, onSizeChanged: () -> Unit) {
    private val element: ComPtr

    /** The band's UIElement (passed to the ScrollViewer's header). */
    val uiElement: ComPtr

    /** The band's children (IVector<UIElement>). */
    private val children: ComPtr

    init {
        val orientation = if (horizontal) "Horizontal" else "Vertical"
        val loaded = Xaml.load(
            "<StackPanel Orientation=\"$orientation\" Background=\"{ThemeResource SolidBackgroundFillColorBaseBrush}\" />",
        )
        try {
            element = owner.own(loaded.queryInterface(XamlInterop.IID_IFrameworkElement))
            uiElement = owner.own(loaded.queryInterface(XamlInterop.IID_IUIElement))
            val panel = loaded.queryInterface(XamlInterop.IID_IPanel)
            try {
                children = owner.own(panel.getPtr(XamlInterop.IPanel_get_Children))
            } finally {
                panel.release()
            }
        } finally {
            loaded.release()
        }
        element.addEventHandler(
            "WinUI4K.ScrollHeaderBandSizeChangedHandler",
            XamlInterop.IID_SizeChangedEventHandler,
            XamlInterop.IFrameworkElement_add_SizeChanged,
        ) { _, _ -> onSizeChanged() }
    }

    /** Makes [component] the header component on the band (empty if null). */
    fun setChild(component: WComponent?) {
        children.call(FoundationInterop.IVector_Clear)
        if (component != null) children.call(FoundationInterop.IVector_Append, component.uiElement.ptr)
    }

    /** The band's minimum size. If larger than the header component, the extra is painted with the background. */
    fun setMinimumSize(width: Double, height: Double) {
        element.call(XamlInterop.IFrameworkElement_put_MinWidth, width)
        element.call(XamlInterop.IFrameworkElement_put_MinHeight, height)
    }

    val actualWidth: Double
        get() = element.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth)

    val actualHeight: Double
        get() = element.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight)
}
