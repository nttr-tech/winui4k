package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
        @JvmSynthetic
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
        @JvmSynthetic
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
        @JvmSynthetic
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
 *
 * For people familiar with Swing's JScrollPane, it also has [setViewportView] with the same name, scroll bar policies with [WScrollPaneConstants]
 * ([verticalScrollBarPolicy] / [horizontalScrollBarPolicy]), [viewPosition] and [addChangeListener] (the WinUI names can be used as is too).
 */
class WScrollView @JvmOverloads constructor(content: WComponent? = null) :
    WControl(
        Activation.composeDefault(XamlInterop.CLS_ScrollView, XamlInterop.IID_IScrollViewFactory), // Default interface = IScrollView
    ),
    WScrollPaneConstants {
    /** ViewChanged event tokens registered via addViewChangedListener. */
    private val viewChangedTokens = ListenerTokens<Runnable>()

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
            replaceChild(field, value)
            field = value
            inspectable.call(XamlInterop.IScrollView_put_Content, value?.uiElement?.ptr)
        }

    override fun detachChild(child: WComponent) {
        if (this.content === child) this.content = null
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

    /** JScrollPane.setViewportView-like: sets the content to show (same as setting [content]). */
    fun setViewportView(view: WComponent?) {
        content = view
    }

    /**
     * JScrollPane.getVerticalScrollBarPolicy / setVerticalScrollBarPolicy-like: reads and writes the vertical scroll bar policy with [WScrollPaneConstants] values.
     * AS_NEEDED = AUTO, ALWAYS = VISIBLE, NEVER = HIDDEN ([verticalScrollBarVisibility]), and all of them enable vertical scrolling
     * (vertical is included in [contentOrientation]: HORIZONTAL becomes BOTH, and NONE becomes VERTICAL). As with [WScrollPane], NEVER
     * only hides the bar and does not shrink the content height. Returns NEVER when [contentOrientation] does not include vertical (HORIZONTAL / NONE).
     *
     * @throws IllegalArgumentException for values other than VERTICAL_SCROLLBAR_* (as in Swing)
     */
    var verticalScrollBarPolicy: Int
        get() = when {
            !contentOrientation.scrollsVertically() -> WScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            else -> when (verticalScrollBarVisibility) {
                ScrollingScrollBarVisibility.AUTO -> WScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                ScrollingScrollBarVisibility.VISIBLE -> WScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                ScrollingScrollBarVisibility.HIDDEN -> WScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            }
        }
        set(value) {
            verticalScrollBarVisibility = when (value) {
                WScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED -> ScrollingScrollBarVisibility.AUTO
                WScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS -> ScrollingScrollBarVisibility.VISIBLE
                WScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER -> ScrollingScrollBarVisibility.HIDDEN
                else -> throw IllegalArgumentException("invalid verticalScrollBarPolicy: $value")
            }
            contentOrientation = when (contentOrientation) {
                ScrollingContentOrientation.HORIZONTAL -> ScrollingContentOrientation.BOTH
                ScrollingContentOrientation.NONE -> ScrollingContentOrientation.VERTICAL
                else -> contentOrientation
            }
        }

    /**
     * JScrollPane.getHorizontalScrollBarPolicy / setHorizontalScrollBarPolicy-like: reads and writes the horizontal scroll bar policy with [WScrollPaneConstants] values.
     * AS_NEEDED / ALWAYS / NEVER all enable horizontal scrolling (horizontal is included in [contentOrientation]: VERTICAL becomes BOTH, and NONE becomes HORIZONTAL).
     * The default [contentOrientation] is VERTICAL (no horizontal scrolling), so it returns NEVER if nothing is set.
     *
     * @throws IllegalArgumentException for values other than HORIZONTAL_SCROLLBAR_* (as in Swing)
     */
    var horizontalScrollBarPolicy: Int
        get() = when {
            !contentOrientation.scrollsHorizontally() -> WScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            else -> when (horizontalScrollBarVisibility) {
                ScrollingScrollBarVisibility.AUTO -> WScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                ScrollingScrollBarVisibility.VISIBLE -> WScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                ScrollingScrollBarVisibility.HIDDEN -> WScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }
        }
        set(value) {
            horizontalScrollBarVisibility = when (value) {
                WScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED -> ScrollingScrollBarVisibility.AUTO
                WScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS -> ScrollingScrollBarVisibility.VISIBLE
                WScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER -> ScrollingScrollBarVisibility.HIDDEN
                else -> throw IllegalArgumentException("invalid horizontalScrollBarPolicy: $value")
            }
            contentOrientation = when (contentOrientation) {
                ScrollingContentOrientation.VERTICAL -> ScrollingContentOrientation.BOTH
                ScrollingContentOrientation.NONE -> ScrollingContentOrientation.HORIZONTAL
                else -> contentOrientation
            }
        }

    /**
     * JViewport.getViewPosition / setViewPosition-like: the content position visible at the top-left of the viewport (DIP rounded to integers).
     * Setting is done with ScrollView.ScrollTo without animation (AnimationMode = Disabled of ScrollingScrollOptions).
     * ScrollView changes the position asynchronously later, so it may still return the previous position when the setter returns (Swing changes it immediately).
     * The position is kept within 0 to the scrollable amount.
     */
    var viewPosition: WPoint
        get() = WPoint(Math.round(horizontalOffset).toInt(), Math.round(verticalOffset).toInt())
        set(value) {
            val options = Ffi.backend.withScope { scope ->
                val factory = Activation.factory(XamlInterop.CLS_ScrollingScrollOptions, XamlInterop.IID_IScrollingScrollOptionsFactory)
                try {
                    val inner = scope.allocate(8)
                    val instance = scope.allocate(8)
                    factory.call(XamlInterop.IScrollingScrollOptionsFactory_CreateInstance, XamlInterop.ScrollingAnimationMode_Disabled, null, inner, instance)
                    // Creation without derivation does not use inner, so return the received reference
                    val innerPtr = Ffi.backend.memory.getPtr(inner, 0)
                    if (!innerPtr.isNull) ComPtr(innerPtr).release()
                    ComPtr(Ffi.backend.memory.getPtr(instance, 0))
                } finally {
                    factory.release()
                }
            }
            try {
                Ffi.backend.withScope { scope ->
                    val correlationId = scope.allocate(4) // out int correlationId (unused)
                    inspectable.call(XamlInterop.IScrollView_ScrollToWithOptions, value.x.toDouble(), value.y.toDouble(), options.ptr, correlationId)
                }
            } finally {
                options.release()
            }
        }

    /** The content width (ScrollView.ExtentWidth). */
    val extentWidth: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_ExtentWidth)

    /** The content height (ScrollView.ExtentHeight). */
    val extentHeight: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_ExtentHeight)

    /** The viewport width (ScrollView.ViewportWidth). */
    val viewportWidth: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_ViewportWidth)

    /** The viewport height (ScrollView.ViewportHeight). */
    val viewportHeight: Double
        get() = inspectable.getDouble(XamlInterop.IScrollView_get_ViewportHeight)

    /** The ChangeListeners. The ViewChanged handler is registered only once, when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addViewChangedHandler() }

    /**
     * ChangeListener-like (corresponding to JViewport's ChangeListener): receives a [WChangeEvent] every time the scroll position or zoom factor changes
     * (the source is this control) (ScrollView.ViewChanged; also called during inertial scrolling).
     * Inside the listener, [viewPosition] returns the value after the change.
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order added). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    private fun addViewChangedHandler() {
        // The handler stays on the native side, so it does not capture this control strongly but looks it up from the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.ScrollViewChangeListenerHandler",
            XamlInterop.IID_ScrollViewViewChangedHandler,
            XamlInterop.IScrollView_add_ViewChanged,
        ) { _, _ -> (anchor.get() as? WScrollView)?.changeListeners?.fire() }
    }

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
    @JvmSynthetic
    fun addViewChangedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addViewChangedListenerForJava(adapter)
        viewChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addViewChangedListener")
    fun addViewChangedListenerForJava(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ScrollViewViewChangedHandler",
            XamlInterop.IID_ScrollViewViewChangedHandler,
            XamlInterop.IScrollView_add_ViewChanged,
        ) { _, _ -> listener.run() }
        viewChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addViewChangedListener]. */
    @JvmSynthetic
    fun removeViewChangedListener(listener: () -> Unit) {
        val adapter = viewChangedTokens.removeKotlinAdapter(listener) ?: return
        removeViewChangedListenerForJava(adapter)
    }

    @JvmName("removeViewChangedListener")
    fun removeViewChangedListenerForJava(listener: Runnable) {
        val token = viewChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IScrollView_remove_ViewChanged, token)
    }

    init {
        if (content != null) this.content = content
    }
}

private fun ScrollingContentOrientation.scrollsVertically(): Boolean =
    this == ScrollingContentOrientation.VERTICAL || this == ScrollingContentOrientation.BOTH

private fun ScrollingContentOrientation.scrollsHorizontally(): Boolean =
    this == ScrollingContentOrientation.HORIZONTAL || this == ScrollingContentOrientation.BOTH
