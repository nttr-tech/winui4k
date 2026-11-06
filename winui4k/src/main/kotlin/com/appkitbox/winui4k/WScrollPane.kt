package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.ScrollBarVisibility (how a scrollbar is shown).
 * Values extracted from the winmd (Disabled=0, Auto=1, Hidden=2, Visible=3).
 */
enum class ScrollBarVisibility(internal val native: Int) {
    /** Don't allow scrolling (the default for the horizontal direction). */
    DISABLED(0),

    /** Show only when the content overflows. */
    AUTO(1),

    /** Allow scrolling, but don't show the bar. */
    HIDDEN(2),

    /** Always show it. */
    VISIBLE(3),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): ScrollBarVisibility = entries.first { it.native == native }
    }
}

/**
 * JScrollPane-like: WinUI 3's ScrollViewer (a ContentControl subclass).
 * Shows scrollbars when [content] is larger than the viewport.
 *
 * Horizontal scrolling of `WScrollPane(view)` and `WScrollPane()` is disabled by default (DISABLED; the content shrinks to the viewport width).
 * This differs from the default of Swing's JScrollPane (HORIZONTAL_SCROLLBAR_AS_NEEDED horizontally too), so to scroll horizontally as well,
 * use the constructor that takes scroll bar policies as in Swing (`WScrollPane(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED)`)
 * or set [horizontalScrollBarPolicy] / [horizontalScrollBarVisibility].
 */
class WScrollPane @JvmOverloads constructor(content: WComponent? = null) :
    WControl(
        Activation.activate(XamlInterop.CLS_ScrollViewer, XamlInterop.IID_IScrollViewer), // Created with the default factory
    ),
    WScrollPaneConstants {
    /**
     * JScrollPane(view, vsbPolicy, hsbPolicy)-like: shows [view], and specifies the vertical and horizontal scroll bar policies with Swing constants
     * ([VERTICAL_SCROLLBAR_AS_NEEDED] etc.).
     *
     * @throws IllegalArgumentException if a policy value is not one of the vertical or horizontal constants respectively
     */
    constructor(view: WComponent?, vsbPolicy: Int, hsbPolicy: Int) : this(view) {
        verticalScrollBarPolicy = vsbPolicy
        horizontalScrollBarPolicy = hsbPolicy
    }

    /** JScrollPane(vsbPolicy, hsbPolicy)-like: specifies the vertical and horizontal scroll bar policies without content. */
    constructor(vsbPolicy: Int, hsbPolicy: Int) : this(null, vsbPolicy, hsbPolicy)

    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /**
     * How the horizontal scrollbar is shown (ScrollViewer.HorizontalScrollBarVisibility, default DISABLED).
     * Setting anything other than DISABLED also enables scrolling (HorizontalScrollMode = Enabled).
     */
    var horizontalScrollBarVisibility: ScrollBarVisibility
        get() = ScrollBarVisibility.of(inspectable.getInt(XamlInterop.IScrollViewer_get_HorizontalScrollBarVisibility))
        set(value) {
            inspectable.call(XamlInterop.IScrollViewer_put_HorizontalScrollBarVisibility, value.native)
            inspectable.call(
                XamlInterop.IScrollViewer_put_HorizontalScrollMode,
                if (value == ScrollBarVisibility.DISABLED) 0 else 1, // ScrollMode.Disabled / Enabled
            )
        }

    /** How the vertical scrollbar is shown (ScrollViewer.VerticalScrollBarVisibility, default VISIBLE). */
    var verticalScrollBarVisibility: ScrollBarVisibility
        get() = ScrollBarVisibility.of(inspectable.getInt(XamlInterop.IScrollViewer_get_VerticalScrollBarVisibility))
        set(value) {
            inspectable.call(XamlInterop.IScrollViewer_put_VerticalScrollBarVisibility, value.native)
            inspectable.call(
                XamlInterop.IScrollViewer_put_VerticalScrollMode,
                if (value == ScrollBarVisibility.DISABLED) 0 else 1, // ScrollMode.Disabled / Enabled
            )
        }

    /**
     * JScrollPane.getVerticalScrollBarPolicy / setVerticalScrollBarPolicy-like: reads and writes the vertical scroll bar policy with Swing constants.
     * AS_NEEDED = AUTO, ALWAYS = VISIBLE, NEVER = HIDDEN (replaced with the value of [verticalScrollBarVisibility]).
     * NEVER only hides the bar and does not shrink the content height (as in Swing, the overflow can be scrolled programmatically with [viewport]).
     * Returns NEVER if [verticalScrollBarVisibility] was set to DISABLED.
     *
     * @throws IllegalArgumentException for values other than VERTICAL_SCROLLBAR_* (as in Swing)
     */
    var verticalScrollBarPolicy: Int
        get() = when (verticalScrollBarVisibility) {
            ScrollBarVisibility.AUTO -> VERTICAL_SCROLLBAR_AS_NEEDED
            ScrollBarVisibility.VISIBLE -> VERTICAL_SCROLLBAR_ALWAYS
            ScrollBarVisibility.HIDDEN, ScrollBarVisibility.DISABLED -> VERTICAL_SCROLLBAR_NEVER
        }
        set(value) {
            verticalScrollBarVisibility = when (value) {
                VERTICAL_SCROLLBAR_AS_NEEDED -> ScrollBarVisibility.AUTO
                VERTICAL_SCROLLBAR_ALWAYS -> ScrollBarVisibility.VISIBLE
                VERTICAL_SCROLLBAR_NEVER -> ScrollBarVisibility.HIDDEN
                else -> throw IllegalArgumentException("invalid verticalScrollBarPolicy: $value")
            }
        }

    /**
     * JScrollPane.getHorizontalScrollBarPolicy / setHorizontalScrollBarPolicy-like: reads and writes the horizontal scroll bar policy with Swing constants.
     * The mapping is the same as [verticalScrollBarPolicy]. The horizontal default is DISABLED, so it returns NEVER if nothing is set.
     *
     * @throws IllegalArgumentException for values other than HORIZONTAL_SCROLLBAR_* (as in Swing)
     */
    var horizontalScrollBarPolicy: Int
        get() = when (horizontalScrollBarVisibility) {
            ScrollBarVisibility.AUTO -> HORIZONTAL_SCROLLBAR_AS_NEEDED
            ScrollBarVisibility.VISIBLE -> HORIZONTAL_SCROLLBAR_ALWAYS
            ScrollBarVisibility.HIDDEN, ScrollBarVisibility.DISABLED -> HORIZONTAL_SCROLLBAR_NEVER
        }
        set(value) {
            horizontalScrollBarVisibility = when (value) {
                HORIZONTAL_SCROLLBAR_AS_NEEDED -> ScrollBarVisibility.AUTO
                HORIZONTAL_SCROLLBAR_ALWAYS -> ScrollBarVisibility.VISIBLE
                HORIZONTAL_SCROLLBAR_NEVER -> ScrollBarVisibility.HIDDEN
                else -> throw IllegalArgumentException("invalid horizontalScrollBarPolicy: $value")
            }
        }

    /** The current horizontal scroll position (ScrollViewer.HorizontalOffset). */
    val horizontalOffset: Double
        get() = inspectable.getDouble(XamlInterop.IScrollViewer_get_HorizontalOffset)

    /** The viewport's width (ScrollViewer.ViewportWidth). */
    val viewportWidth: Double
        get() = inspectable.getDouble(XamlInterop.IScrollViewer_get_ViewportWidth)

    /** The scrollable width = content width - viewport width (ScrollViewer.ScrollableWidth). 0 means no overflow. */
    val scrollableWidth: Double
        get() = inspectable.getDouble(XamlInterop.IScrollViewer_get_ScrollableWidth)

    /** The current vertical scroll position (ScrollViewer.VerticalOffset). */
    val verticalOffset: Double
        get() = inspectable.getDouble(XamlInterop.IScrollViewer_get_VerticalOffset)

    /** The viewport height (ScrollViewer.ViewportHeight). */
    val viewportHeight: Double
        get() = inspectable.getDouble(XamlInterop.IScrollViewer_get_ViewportHeight)

    /** The scrollable height = content height - viewport height (ScrollViewer.ScrollableHeight). 0 means no overflow. */
    val scrollableHeight: Double
        get() = inspectable.getDouble(XamlInterop.IScrollViewer_get_ScrollableHeight)

    /** The content width (ScrollViewer.ExtentWidth). */
    val extentWidth: Double
        get() = inspectable.getDouble(XamlInterop.IScrollViewer_get_ExtentWidth)

    /** The content height (ScrollViewer.ExtentHeight). */
    val extentHeight: Double
        get() = inspectable.getDouble(XamlInterop.IScrollViewer_get_ExtentHeight)

    /** JScrollPane.getViewport-like: the viewport (reading and writing the shown position, and a ChangeListener for its changes). */
    val viewport: WViewport by lazy { WViewport(this) }

    /** JScrollPane.setViewportView-like: sets the content to show (same as setting [content]). */
    fun setViewportView(view: WComponent?) {
        content = view
    }

    /** The ChangeListeners of [viewport]. The ViewChanged handler is registered only once, when the first listener is added. */
    internal val viewportChangeListeners = ChangeListeners(this) { ensureViewChangedHandler() }

    private var viewChangedHandlerAdded = false

    /** Registers the ViewChanged handler (shared by viewport and scroll bar notifications) if not registered yet. */
    private fun ensureViewChangedHandler() {
        if (viewChangedHandlerAdded) return
        viewChangedHandlerAdded = true
        // The handler stays on the native side, so it does not capture this control strongly but looks it up from the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.ScrollPaneViewChangedHandler",
            XamlInterop.IID_ScrollViewerViewChangedHandler,
            XamlInterop.IScrollViewer_add_ViewChanged,
        ) { _, args ->
            val intermediate = ComPtr(args).getBool(XamlInterop.IScrollViewerViewChangedEventArgs_get_IsIntermediate)
            (anchor.get() as? WScrollPane)?.onViewChanged(intermediate)
        }
    }

    private fun onViewChanged(@Suppress("UNUSED_PARAMETER") intermediate: Boolean) {
        viewportChangeListeners.fire(WChangeEvent(viewport))
    }

    /**
     * Changes the scroll position without animation (disableAnimation = true of ScrollViewer.ChangeView).
     * A null direction is not changed.
     */
    internal fun changeViewImmediately(horizontalOffset: Double?, verticalOffset: Double?) {
        val horizontal = horizontalOffset?.let { boxedDoubleReference(it) }
        val vertical = verticalOffset?.let { boxedDoubleReference(it) }
        try {
            Ffi.backend.withScope { scope ->
                val changed = scope.allocate(8) // out boolean (unused)
                inspectable.callWith(
                    XamlInterop.IScrollViewer_ChangeViewWithDisableAnimation,
                    CHANGE_VIEW_WITH_DISABLE_ANIMATION,
                    horizontal?.ptr,
                    vertical?.ptr,
                    null,
                    1.toByte(),
                    changed,
                )
            }
        } finally {
            horizontal?.release()
            vertical?.release()
        }
        // ChangeView without animation only rewrites the ScrollContentPresenter offsets and schedules a re-arrange,
        // and HorizontalOffset / VerticalOffset change after layout. Finish the layout so that it can be read back right away, as with Swing's setViewPosition
        updateLayout()
    }

    /** Animates the horizontal scroll position to [offset] (ScrollViewer.ChangeView). */
    fun scrollToHorizontalOffset(offset: Double) {
        // Box it as an IReference<Double> to pass (vertical position and zoom are null = no change)
        val reference = boxedDoubleReference(offset)
        Ffi.backend.withScope { scope ->
            val handled = scope.allocate(8) // out boolean (unused)
            inspectable.call(XamlInterop.IScrollViewer_ChangeView, reference.ptr, null, null, handled)
        }
        reference.release()
    }

    /** Event tokens registered via addViewChangedListener. */
    private val viewChangedTokens = ListenerTokens<Runnable>()

    /** Subscribes to scroll position changes (ScrollViewer.ViewChanged; also fires during inertial scrolling). */
    @JvmSynthetic
    fun addViewChangedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addViewChangedListenerForJava(adapter)
        viewChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addViewChangedListener")
    fun addViewChangedListenerForJava(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.ScrollViewerViewChangedHandler",
            XamlInterop.IID_ScrollViewerViewChangedHandler,
            XamlInterop.IScrollViewer_add_ViewChanged,
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
        inspectable.removeEventHandler(XamlInterop.IScrollViewer_remove_ViewChanged, token)
    }

    /** The content to scroll (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            replaceChild(field, value)
            field = value
            contentControl.call(
                XamlInterop.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    override fun detachChild(child: WComponent) {
        if (this.content === child) this.content = null
    }

    init {
        if (content != null) this.content = content
    }

    /** The [WScrollPaneConstants] values so that Kotlin can write `WScrollPane.VERTICAL_SCROLLBAR_ALWAYS` (Java sees them as the same fields as the interface values). */
    companion object {
        /** Same as [WScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED]. */
        const val VERTICAL_SCROLLBAR_AS_NEEDED = WScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

        /** Same as [WScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER]. */
        const val VERTICAL_SCROLLBAR_NEVER = WScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER

        /** Same as [WScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS]. */
        const val VERTICAL_SCROLLBAR_ALWAYS = WScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS

        /** Same as [WScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED]. */
        const val HORIZONTAL_SCROLLBAR_AS_NEEDED = WScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED

        /** Same as [WScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER]. */
        const val HORIZONTAL_SCROLLBAR_NEVER = WScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

        /** Same as [WScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS]. */
        const val HORIZONTAL_SCROLLBAR_ALWAYS = WScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS

        /** ChangeView(this, IReference<double>, IReference<double>, IReference<float>, boolean, out boolean). A WinRT boolean is 1 byte. */
        private val CHANGE_VIEW_WITH_DISABLE_ANIMATION = CallDescriptor(
            ValueKind.I32,
            ArgKind.PTR,
            ArgKind.PTR,
            ArgKind.PTR,
            ArgKind.PTR,
            ArgKind.U8,
            ArgKind.PTR,
        )

        /** Boxes [value] into an IReference<Double> (the caller releases it). */
        private fun boxedDoubleReference(value: Double): ComPtr {
            val boxed = PropertyValues.boxDouble(value)
            return try {
                boxed.queryInterface(FoundationInterop.IID_IReference_Double)
            } finally {
                boxed.release()
            }
        }
    }
}
