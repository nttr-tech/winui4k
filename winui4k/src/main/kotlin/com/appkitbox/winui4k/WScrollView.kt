package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Controls.ScrollingScrollBarVisibility (how ScrollView shows its scrollbars).
 * Values extracted from the winmd (Auto=0, Visible=1, Hidden=2). A separate enum from
 * ScrollViewer's [ScrollBarVisibility].
 */
enum class ScrollingScrollBarVisibility(internal val native: Int) {
    /** Show only when the content overflows (the default). */
    AUTO(0),

    /** Always show it. */
    VISIBLE(1),

    /** Allow scrolling, but don't show the bar. */
    HIDDEN(2),
    ;

    internal companion object {
        fun of(native: Int): ScrollingScrollBarVisibility = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.ScrollingContentOrientation (which direction the content is allowed
 * to expand in). Values extracted from the winmd (Vertical=0, Horizontal=1, None=2, Both=3).
 */
enum class ScrollingContentOrientation(internal val native: Int) {
    /** Unconstrained vertically, fit to the viewport width (the default; vertical scrolling). */
    VERTICAL(0),

    /** Unconstrained horizontally, fit to the viewport height (horizontal scrolling). */
    HORIZONTAL(1),

    /** Fit to the viewport (no overflow). */
    NONE(2),

    /** Unconstrained in both directions (scrolling both ways). */
    BOTH(3),
    ;

    internal companion object {
        fun of(native: Int): ScrollingContentOrientation = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.ScrollingZoomMode (whether pinch / Ctrl+wheel zooming is allowed).
 * Values extracted from the winmd (Enabled=0, Disabled=1).
 */
enum class ScrollingZoomMode(internal val native: Int) {
    /** Allow zooming. */
    ENABLED(0),

    /** Disallow zooming (the default). */
    DISABLED(1),
    ;

    internal companion object {
        fun of(native: Int): ScrollingZoomMode = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's ScrollView (a Control subclass). The successor to ScrollViewer; shows [content]
 * larger than the viewport by scrolling, panning, and zooming it. No Swing equivalent, so we
 * use the WinUI name.
 *
 * It differs from ScrollViewer ([WScrollPane]) in that the vertical/horizontal behavior is
 * switched via [contentOrientation], and scroll position changes are exposed as the
 * [scrollTo] / [scrollBy] methods.
 */
class WScrollView(content: WComponent? = null) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ScrollView, XamlInterop.IID_IScrollViewFactory), // default interface = IScrollView
) {
    /** ViewChanged event tokens registered via addViewChangedListener. */
    private val viewChangedTokens = ListenerTokens<() -> Unit>()

    /**
     * The internal ScrollPresenter (ScrollView.ScrollPresenter). null before the template is
     * applied. Used to connect a [WAnnotatedScrollBar] as the vertical scroll controller
     * (only touch this after Loaded).
     */
    internal val scrollPresenter: ComPtr?
        get() = inspectable.getPtrOrNull(XamlInterop.IScrollView_get_ScrollPresenter)

    /** The content to scroll (ScrollView.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.IScrollView_put_Content, value?.uiElement?.ptr)
        }

    /** How the horizontal scrollbar is shown (ScrollView.HorizontalScrollBarVisibility, default AUTO). */
    var horizontalScrollBarVisibility: ScrollingScrollBarVisibility
        get() = ScrollingScrollBarVisibility.of(inspectable.getInt(XamlInterop.IScrollView_get_HorizontalScrollBarVisibility))
        set(value) = inspectable.call(XamlInterop.IScrollView_put_HorizontalScrollBarVisibility, value.native)

    /** How the vertical scrollbar is shown (ScrollView.VerticalScrollBarVisibility, default AUTO). */
    var verticalScrollBarVisibility: ScrollingScrollBarVisibility
        get() = ScrollingScrollBarVisibility.of(inspectable.getInt(XamlInterop.IScrollView_get_VerticalScrollBarVisibility))
        set(value) = inspectable.call(XamlInterop.IScrollView_put_VerticalScrollBarVisibility, value.native)

    /**
     * Which direction the content is allowed to expand in (ScrollView.ContentOrientation,
     * default VERTICAL = vertical scrolling).
     */
    var contentOrientation: ScrollingContentOrientation
        get() = ScrollingContentOrientation.of(inspectable.getInt(XamlInterop.IScrollView_get_ContentOrientation))
        set(value) = inspectable.call(XamlInterop.IScrollView_put_ContentOrientation, value.native)

    /** Whether zooming is allowed (ScrollView.ZoomMode, default DISABLED). */
    var zoomMode: ScrollingZoomMode
        get() = ScrollingZoomMode.of(inspectable.getInt(XamlInterop.IScrollView_get_ZoomMode))
        set(value) = inspectable.call(XamlInterop.IScrollView_put_ZoomMode, value.native)

    /** The minimum zoom factor (ScrollView.MinZoomFactor, default 0.1). */
    var minZoomFactor: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_MinZoomFactor)
        set(value) = inspectable.call(XamlInterop.IScrollView_put_MinZoomFactor, value)

    /** The maximum zoom factor (ScrollView.MaxZoomFactor, default 10.0). */
    var maxZoomFactor: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_MaxZoomFactor)
        set(value) = inspectable.call(XamlInterop.IScrollView_put_MaxZoomFactor, value)

    /** The current horizontal scroll position (ScrollView.HorizontalOffset). */
    val horizontalOffset: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_HorizontalOffset)

    /** The current vertical scroll position (ScrollView.VerticalOffset). */
    val verticalOffset: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_VerticalOffset)

    /** The scrollable width = content width - viewport width (ScrollView.ScrollableWidth). */
    val scrollableWidth: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_ScrollableWidth)

    /** The scrollable height = content height - viewport height (ScrollView.ScrollableHeight). */
    val scrollableHeight: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_ScrollableHeight)

    /** Animates the scroll position to an absolute position (ScrollView.ScrollTo). */
    fun scrollTo(horizontalOffset: Double, verticalOffset: Double) {
        Ffi.backend.withScope { scope ->
            val correlationId = scope.allocate(4) // out int correlationId (unused)
            inspectable.call(XamlInterop.IScrollView_ScrollTo, horizontalOffset, verticalOffset, correlationId)
        }
    }

    /** Animates the scroll position by a relative amount from the current position (ScrollView.ScrollBy). */
    fun scrollBy(horizontalDelta: Double, verticalDelta: Double) {
        Ffi.backend.withScope { scope ->
            val correlationId = scope.allocate(4) // out int correlationId (unused)
            inspectable.call(XamlInterop.IScrollView_ScrollBy, horizontalDelta, verticalDelta, correlationId)
        }
    }

    /**
     * Subscribes to changes in scroll position / zoom factor (ScrollView.ViewChanged; also fires
     * during inertial scrolling).
     */
    fun addViewChangedListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ScrollViewViewChangedHandler",
            XamlInterop.IID_ScrollViewViewChangedHandler,
            XamlInterop.IScrollView_add_ViewChanged,
        ) { _, _ -> listener() }
        viewChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addViewChangedListener]. */
    fun removeViewChangedListener(listener: () -> Unit) {
        val token = viewChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IScrollView_remove_ViewChanged, token)
    }

    init {
        if (content != null) this.content = content
    }
}
