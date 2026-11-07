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
import com.appkitbox.winui4k.internal.winui.Dispatcher
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

    /** IsIntermediate of the latest ViewChanged ([WScrollPaneBar.valueIsAdjusting]). */
    private var lastViewChangeIntermediate = false

    /** Whether scrolling is in progress (IsIntermediate of the latest ViewChanged). Reading it starts the ViewChanged subscription. */
    internal val viewIsAdjusting: Boolean
        get() {
            ensureViewChangedHandler()
            return lastViewChangeIntermediate
        }

    /** Registers the ViewChanged handler (shared by viewport and scroll bar notifications) if not registered yet. */
    internal fun ensureViewChangedHandler() {
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

    private fun onViewChanged(intermediate: Boolean) {
        lastViewChangeIntermediate = intermediate
        viewportChangeListeners.fire(WChangeEvent(viewport))
        createdVerticalScrollBar?.onViewChanged(intermediate)
        createdHorizontalScrollBar?.onViewChanged(intermediate)
    }

    private var createdVerticalScrollBar: WScrollPaneBar? = null
    private var createdHorizontalScrollBar: WScrollPaneBar? = null

    /** JScrollPane.getVerticalScrollBar-like: the point for reading and writing the vertical scroll position as a scroll bar value ([WScrollPaneBar]). */
    val verticalScrollBar: WScrollPaneBar
        get() = createdVerticalScrollBar ?: WScrollPaneBar(this, WAdjustable.VERTICAL).also { createdVerticalScrollBar = it }

    /** JScrollPane.getHorizontalScrollBar-like: the point for reading and writing the horizontal scroll position as a scroll bar value ([WScrollPaneBar]). */
    val horizontalScrollBar: WScrollPaneBar
        get() = createdHorizontalScrollBar ?: WScrollPaneBar(this, WAdjustable.HORIZONTAL).also { createdHorizontalScrollBar = it }

    /**
     * JScrollPane.setColumnHeaderView-like: the top header (ScrollViewer.TopHeader). It scrolls only horizontally with the content and does not move vertically (same as Swing's column header).
     * While there are headers ([rowHeaderView] and the corner too), ScrollViewer crashes unless the content is aligned top-left, so the content's
     * [WComponent.horizontalAlignment] is rewritten to LEFT and [WComponent.verticalAlignment] to TOP (content smaller than the viewport is not stretched).
     * Do not change the content's alignment while there are headers.
     */
    var columnHeaderView: WComponent? = null
        set(value) {
            replaceChild(field, value)
            field = value
            alignContentForHeaders()
            inspectable.call(XamlInterop.IScrollViewer_put_TopHeader, value?.uiElement?.ptr)
        }

    /**
     * JScrollPane.setRowHeaderView-like: the left header (ScrollViewer.LeftHeader). It scrolls only vertically with the content and does not move horizontally (same as Swing's row header).
     */
    var rowHeaderView: WComponent? = null
        set(value) {
            replaceChild(field, value)
            field = value
            alignContentForHeaders()
            inspectable.call(XamlInterop.IScrollViewer_put_LeftHeader, value?.uiElement?.ptr)
        }

    /** The top-left corner (ScrollViewer.TopLeftHeader). */
    private var upperLeadingCorner: WComponent? = null
        set(value) {
            replaceChild(field, value)
            field = value
            alignContentForHeaders()
            inspectable.call(XamlInterop.IScrollViewer_put_TopLeftHeader, value?.uiElement?.ptr)
        }

    /**
     * When there is a header or a corner, aligns the content to the top-left (HorizontalAlignment = Left, VerticalAlignment = Top). With headers, ScrollViewer
     * raises E_NOT_SUPPORTED in the measure pass when the content has any other alignment, and XAML crashes the process
     * (MeasureOverride in microsoft-ui-xaml's ScrollContentPresenter_Partial.cpp; doc/troubleshooting.md).
     */
    private fun alignContentForHeaders() {
        val view = content ?: return
        if (columnHeaderView == null && rowHeaderView == null && upperLeadingCorner == null) return
        view.horizontalAlignment = HorizontalAlignment.LEFT
        view.verticalAlignment = VerticalAlignment.TOP
    }

    /**
     * JScrollPane.setCorner-like: sets the component placed in a corner. ScrollViewer has only the corner where the top header and left header meet
     * (TopLeftHeader), so only [UPPER_LEADING_CORNER] and [UPPER_LEFT_CORNER] are accepted, and both refer to the same corner
     * (when FlowDirection is right-to-left, the whole ScrollViewer is mirrored, so it appears at the top-right).
     *
     * @throws UnsupportedOperationException for other corners (LOWER_* / UPPER_RIGHT_CORNER / UPPER_TRAILING_CORNER)
     * @throws IllegalArgumentException if it is not a corner name (as in Swing)
     */
    fun setCorner(key: String, corner: WComponent?) {
        checkCornerKey(key)
        upperLeadingCorner = corner
    }

    /** JScrollPane.getCorner-like: the component placed with [setCorner]. Always null for unaccepted corners (Swing returns null if it is not a corner name). */
    fun getCorner(key: String): WComponent? = if (key == UPPER_LEADING_CORNER || key == UPPER_LEFT_CORNER) upperLeadingCorner else null

    /** Applies the scroll position requested before the first layout after laying out. */
    private val laidOutRequests = LaidOutRequests(this)

    /** The horizontal and vertical positions requested before the first layout (null for a direction not requested). */
    private var pendingHorizontalOffset: Double? = null
    private var pendingVerticalOffset: Double? = null

    /** The horizontal position ([horizontalOffset]). The requested value if a position was requested before the first layout (as in Swing, the set value can be read right away). */
    internal val requestedHorizontalOffset: Double get() = pendingHorizontalOffset ?: horizontalOffset

    /** The vertical position ([verticalOffset]). The same as [requestedHorizontalOffset]. */
    internal val requestedVerticalOffset: Double get() = pendingVerticalOffset ?: verticalOffset

    /**
     * Changes the scroll position without animation (disableAnimation = true of ScrollViewer.ChangeView).
     * A null direction is not changed.
     *
     * Before the first layout (before or right after showing), the content's size is not decided yet and the scrollable amount is 0, so
     * the ScrollViewer rounds the position to 0. So the requested position is remembered and applied after laying out (after the layout in the window is done)
     * (Swing's setViewPosition works even right after showing).
     */
    internal fun changeViewImmediately(horizontalOffset: Double?, verticalOffset: Double?) {
        if (laidOutRequests.isPending(VIEW_REQUEST) || !laidOutRequests.isLaidOut()) {
            if (horizontalOffset != null) pendingHorizontalOffset = horizontalOffset
            if (verticalOffset != null) pendingVerticalOffset = verticalOffset
            laidOutRequests.runOrDefer(VIEW_REQUEST) { applyPendingView() }
            return
        }
        applyChangeView(horizontalOffset, verticalOffset)
    }

    /** Applies the remembered position after the layout in the window (relayout of WLayoutPanels) is done. */
    private fun applyPendingView() {
        val horizontal = pendingHorizontalOffset
        val vertical = pendingVerticalOffset
        pendingHorizontalOffset = null
        pendingVerticalOffset = null
        generateSequence<WComponent>(this) { it.parent }.last().validate()
        applyChangeView(horizontal, vertical)
    }

    /**
     * While the parent [WLayoutPanel] measures the natural size again, the size is removed and it grows to the content's size, so the ScrollViewer rounds
     * the scroll position to 0 (Swing's getPreferredSize does not change the position). The position before measuring is remembered and restored after measuring, once the size is assigned.
     */
    override fun clearLayoutSizeForMeasure() {
        if (canLoseOffsets()) {
            val horizontal = horizontalOffset
            val vertical = verticalOffset
            if (horizontal > 0.0 || vertical > 0.0) {
                savedOffsets = doubleArrayOf(horizontal, vertical)
                // WLayoutPanel assigns the size within the same task after measuring. Restore it in a later turn after the layout is done
                Dispatcher.invokeLater { restoreSavedOffsets() }
            }
        }
        super.clearLayoutSizeForMeasure()
    }

    /** Whether measuring again changes the size (no explicit size), no position is remembered, it is shown, and no position was requested before the first layout. */
    private fun canLoseOffsets(): Boolean =
        (width.isNaN() || height.isNaN()) && savedOffsets == null && !laidOutRequests.isPending(VIEW_REQUEST) && isDisplayable

    /** The [horizontal, vertical] position before measuring again, remembered in [clearLayoutSizeForMeasure]. */
    private var savedOffsets: DoubleArray? = null

    private fun restoreSavedOffsets() {
        val saved = savedOffsets ?: return
        savedOffsets = null
        updateLayout()
        if (horizontalOffset != saved[0] || verticalOffset != saved[1]) applyChangeView(saved[0], saved[1])
    }

    private fun applyChangeView(horizontalOffset: Double?, verticalOffset: Double?) {
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
            alignContentForHeaders()
            contentControl.call(
                XamlInterop.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    override fun detachChild(child: WComponent) {
        if (this.content === child) this.content = null
        if (columnHeaderView === child) columnHeaderView = null
        if (rowHeaderView === child) rowHeaderView = null
        if (upperLeadingCorner === child) upperLeadingCorner = null
    }

    init {
        if (content != null) this.content = content
    }

    /** The [WScrollPaneConstants] values so that Kotlin can write `WScrollPane.VERTICAL_SCROLLBAR_ALWAYS` (Java sees them as the same fields as the interface values). */
    companion object {
        /** The kind of scroll position request in [laidOutRequests]. */
        private const val VIEW_REQUEST = "view"

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

        /** Same as [WScrollPaneConstants.UPPER_LEFT_CORNER]. */
        const val UPPER_LEFT_CORNER = WScrollPaneConstants.UPPER_LEFT_CORNER

        /** Same as [WScrollPaneConstants.UPPER_RIGHT_CORNER]. */
        const val UPPER_RIGHT_CORNER = WScrollPaneConstants.UPPER_RIGHT_CORNER

        /** Same as [WScrollPaneConstants.LOWER_LEFT_CORNER]. */
        const val LOWER_LEFT_CORNER = WScrollPaneConstants.LOWER_LEFT_CORNER

        /** Same as [WScrollPaneConstants.LOWER_RIGHT_CORNER]. */
        const val LOWER_RIGHT_CORNER = WScrollPaneConstants.LOWER_RIGHT_CORNER

        /** Same as [WScrollPaneConstants.UPPER_LEADING_CORNER]. */
        const val UPPER_LEADING_CORNER = WScrollPaneConstants.UPPER_LEADING_CORNER

        /** Same as [WScrollPaneConstants.UPPER_TRAILING_CORNER]. */
        const val UPPER_TRAILING_CORNER = WScrollPaneConstants.UPPER_TRAILING_CORNER

        /** Same as [WScrollPaneConstants.LOWER_LEADING_CORNER]. */
        const val LOWER_LEADING_CORNER = WScrollPaneConstants.LOWER_LEADING_CORNER

        /** Same as [WScrollPaneConstants.LOWER_TRAILING_CORNER]. */
        const val LOWER_TRAILING_CORNER = WScrollPaneConstants.LOWER_TRAILING_CORNER

        private val UNSUPPORTED_CORNERS = setOf(
            UPPER_RIGHT_CORNER,
            LOWER_LEFT_CORNER,
            LOWER_RIGHT_CORNER,
            UPPER_TRAILING_CORNER,
            LOWER_LEADING_CORNER,
            LOWER_TRAILING_CORNER,
        )

        private fun checkCornerKey(key: String) {
            if (key == UPPER_LEADING_CORNER || key == UPPER_LEFT_CORNER) return
            if (key in UNSUPPORTED_CORNERS) throw UnsupportedOperationException("ScrollViewer has only the upper leading corner: $key")
            throw IllegalArgumentException("invalid corner key: $key")
        }

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
