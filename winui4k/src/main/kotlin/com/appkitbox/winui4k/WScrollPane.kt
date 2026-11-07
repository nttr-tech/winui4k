package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Dispatcher
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.lang.ref.WeakReference
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic
import kotlin.math.abs
import kotlin.math.min

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

    /**
     * Before reading the viewport's size ([WScrollPaneBar.visibleAmount], [WViewport.extentSize]), finishes pending layout, as Swing's validate does.
     * After layout, the layout in the window (relayout of WLayoutPanels and UpdateLayout) is done now.
     */
    internal fun layOutForSizes() {
        if (laidOutRequests.isLaidOut()) generateSequence<WComponent>(this) { it.parent }.last().validate()
    }

    /**
     * The content's size [width, height] ([WScrollPaneBar.maximum], [WViewport.viewSize]). After layout, ExtentWidth / ExtentHeight after [layOutForSizes]
     * (the size after adding even right after adding to the content; the same value as read inside Swing's invokeLater).
     * Right after showing and before the first layout, the ScrollViewer has not measured the content yet (Extent is 0), so it is the size of the content ([content]) measured
     * without size constraints (the same as ScrollContentPresenter measuring the content in the scrolling direction; Swing's Window.show validates
     * while showing, so the content's size can be read even right after showing). Measuring the ScrollViewer itself first would make scroll position requests
     * fail when it is laid out afterwards, so only the content is measured.
     */
    internal fun extentForReading(): DoubleArray {
        layOutForSizes()
        val view = content
        if (view == null || laidOutRequests.isLaidOut() || !laidOutRequests.awaitsFirstLayout()) {
            return doubleArrayOf(extentWidth, extentHeight)
        }
        Ffi.backend.withScope { scope ->
            val available = scope.allocate(XamlStructs.SIZE_FLOAT)
            Ffi.backend.memory.putFloat(available.ptr, 0, Float.POSITIVE_INFINITY)
            Ffi.backend.memory.putFloat(available.ptr, 4, Float.POSITIVE_INFINITY)
            view.uiElement.call(XamlInterop.IUIElement_Measure, available)
        }
        val desired = XamlStructs.getSizeFloat(view.uiElement, XamlInterop.IUIElement_get_DesiredSize)
        return doubleArrayOf(maxOf(extentWidth, desired[0]), maxOf(extentHeight, desired[1])).also { estimatedExtent = it }
    }

    /**
     * The content size [extentForReading] measured and returned before the first layout (it can differ from the value after layout by layout rounding).
     * Position requests at or beyond it are taken as "to the end" and applied at the end of the scrollable amount after layout (the same as Swing's setValue(getMaximum())).
     */
    private var estimatedExtent: DoubleArray? = null

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
        if (isQuietWhileRemeasuring()) return
        lastViewChangeIntermediate = intermediate
        notifyViewChange(intermediate)
    }

    /** The position and whether it was intermediate last notified to the ChangeListeners of [viewport] (not notified if the same). */
    private var notifiedViewport: Pair<WPoint, Boolean>? = null

    /**
     * If the position differs from the last notification, notifies the ChangeListeners of [viewport] and the AdjustmentListeners of the scroll bars.
     * Programmatic changes ([changeViewImmediately]) are notified right away, as in Swing, and a later ViewChanged is not notified if the position is the same.
     * While the position is temporarily rounded to 0 by measuring again ([savedOffsets]), nothing is notified until it is restored.
     */
    private fun notifyViewChange(intermediate: Boolean) {
        if (savedOffsets != null) return
        val state = viewport.viewPosition to intermediate
        if (state != notifiedViewport) {
            notifiedViewport = state
            viewportChangeListeners.fire(WChangeEvent(viewport))
        }
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
            if (value != null || columnHeaderBand != null) columnBand().setChild(value)
            inspectable.call(XamlInterop.IScrollViewer_put_TopHeader, value?.let { columnBand().uiElement.ptr })
            updateCorner()
        }

    /**
     * JScrollPane.setRowHeaderView-like: the left header (ScrollViewer.LeftHeader). It scrolls only vertically with the content and does not move horizontally (same as Swing's row header).
     */
    var rowHeaderView: WComponent? = null
        set(value) {
            replaceChild(field, value)
            field = value
            alignContentForHeaders()
            if (value != null || rowHeaderBand != null) rowBand().setChild(value)
            inspectable.call(XamlInterop.IScrollViewer_put_LeftHeader, value?.let { rowBand().uiElement.ptr })
            updateCorner()
        }

    /** The top-left corner (ScrollViewer.TopLeftHeader). */
    private var upperLeadingCorner: WComponent? = null
        set(value) {
            replaceChild(field, value)
            field = value
            alignContentForHeaders()
            updateCorner()
        }

    /** A reference that weakly refers to this. Captured by the SizeChanged handlers of the bands and the content (capturing this strongly would form a cycle with the native side and prevent collection). */
    private val weakSelf = WeakReference(this)

    /** The band holding the column header ([ScrollHeaderBand]). Created when a column header is first set. */
    private var columnHeaderBand: ScrollHeaderBand? = null

    /** The band holding the row header. */
    private var rowHeaderBand: ScrollHeaderBand? = null

    /** The band holding the top-left corner (even without a corner component, it is placed empty when there are both column and row headers, to hide the headers and content passing under it). */
    private var cornerBand: ScrollHeaderBand? = null

    private fun columnBand(): ScrollHeaderBand = columnHeaderBand ?: newBand(horizontal = true).also { columnHeaderBand = it }

    private fun rowBand(): ScrollHeaderBand = rowHeaderBand ?: newBand(horizontal = false).also { rowHeaderBand = it }

    private fun newBand(horizontal: Boolean): ScrollHeaderBand {
        val self = weakSelf
        return ScrollHeaderBand(this, horizontal) { self.get()?.fitHeaderBands() }
    }

    /** Places the top-left corner band when there is a corner component or both column and row headers, and removes it otherwise. */
    private fun updateCorner() {
        val corner = upperLeadingCorner
        val shown = corner != null || (columnHeaderView != null && rowHeaderView != null)
        if (shown || cornerBand != null) {
            val band = cornerBand ?: newBand(horizontal = false).also { cornerBand = it }
            band.setChild(corner)
        }
        inspectable.call(XamlInterop.IScrollViewer_put_TopLeftHeader, if (shown) cornerBand?.uiElement?.ptr else null)
        watchSizes()
        fitHeaderBands()
    }

    /** Whether this subscribes to its own SizeChanged. */
    private var watchingOwnSize = false

    /** The content whose SizeChanged is subscribed, and the token of that subscription. */
    private var watchedContent: WComponent? = null
    private var watchedContentToken = 0L

    /**
     * While there are headers, makes changes in the size of this and of the content call [fitHeaderBands] (moving the subscription when the content changes).
     */
    private fun watchSizes() {
        val hasHeaders = columnHeaderView != null || rowHeaderView != null || upperLeadingCorner != null
        if (!hasHeaders && watchedContent == null) return
        val self = weakSelf
        if (hasHeaders && !watchingOwnSize) {
            watchingOwnSize = true
            frameworkElement.addEventHandler(
                "WinUI4K.ScrollPaneHeaderFitHandler",
                XamlInterop.IID_SizeChangedEventHandler,
                XamlInterop.IFrameworkElement_add_SizeChanged,
            ) { _, _ -> self.get()?.fitHeaderBands() }
        }
        val view = content
        if (view === watchedContent) return
        watchedContent?.frameworkElement?.removeEventHandler(XamlInterop.IFrameworkElement_remove_SizeChanged, watchedContentToken)
        watchedContent = view
        watchedContentToken = view?.frameworkElement?.addEventHandler(
            "WinUI4K.ScrollPaneHeaderFitHandler",
            XamlInterop.IID_SizeChangedEventHandler,
            XamlInterop.IFrameworkElement_add_SizeChanged,
        ) { _, _ -> self.get()?.fitHeaderBands() } ?: 0L
    }

    /**
     * Expands the header bands to sizes that can hide the content passing under the headers: the column header band to the width of the content and the viewport, the row header band to the height of the content and
     * the viewport, and the top-left corner band to the width of the row header band and the height of the column header band (the headers scroll horizontally and vertically with the content).
     */
    private fun fitHeaderBands() {
        val column = columnHeaderBand?.takeIf { columnHeaderView != null }
        val row = rowHeaderBand?.takeIf { rowHeaderView != null }
        column?.setMinimumSize(maxOf(actualWidth, inspectable.getDouble(XamlInterop.IScrollViewer_get_ExtentWidth)), 0.0)
        row?.setMinimumSize(0.0, maxOf(actualHeight, inspectable.getDouble(XamlInterop.IScrollViewer_get_ExtentHeight)))
        cornerBand?.setMinimumSize(row?.actualWidth ?: 0.0, column?.actualHeight ?: 0.0)
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

    /** The horizontal and vertical positions requested before the first layout (null for a direction not requested). */
    private var pendingHorizontalOffset: Double? = null
    private var pendingVerticalOffset: Double? = null

    /** Whether the horizontal and vertical positions requested before the first layout are at or beyond the content size returned then ([estimatedExtent]) (to the end). */
    private var pendingHorizontalToEnd = false
    private var pendingVerticalToEnd = false

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
            val estimated = estimatedExtent
            if (horizontalOffset != null) {
                pendingHorizontalOffset = horizontalOffset
                pendingHorizontalToEnd = estimated != null && horizontalOffset >= estimated[0]
            }
            if (verticalOffset != null) {
                pendingVerticalOffset = verticalOffset
                pendingVerticalToEnd = estimated != null && verticalOffset >= estimated[1]
            }
            laidOutRequests.runOrDefer(VIEW_REQUEST) { applyPendingView() }
        } else {
            applyChangeView(horizontalOffset, verticalOffset)
        }
        // As with Swing's setViewPosition and setValue, notify the listeners before returning (before the first layout, with the requested position)
        lastViewChangeIntermediate = false
        notifyViewChange(false)
    }

    /** Applies the remembered position after the layout in the window (relayout of WLayoutPanels) is done. */
    private fun applyPendingView() {
        val horizontal = pendingHorizontalOffset
        val vertical = pendingVerticalOffset
        pendingHorizontalOffset = null
        pendingVerticalOffset = null
        estimatedExtent = null
        generateSequence<WComponent>(this) { it.parent }.last().validate()
        // The listeners notified at the request read the estimated content size and a viewport of 0. Now that the sizes are decided by layout,
        // notify again even if the position is the same (corresponding to Swing calling AdjustmentListener for range changes too)
        forgetNotifiedView()
        applyChangeView(
            if (pendingHorizontalToEnd) scrollableWidth else horizontal,
            if (pendingVerticalToEnd) scrollableHeight else vertical,
        )
        pendingHorizontalToEnd = false
        pendingVerticalToEnd = false
        notifyViewChange(false) // When the position did not change and no ViewChanged came
    }

    /** Makes the next [notifyViewChange] notify even if the position is the same as the last notification. */
    private fun forgetNotifiedView() {
        notifiedViewport = null
        createdVerticalScrollBar?.forgetNotified()
        createdHorizontalScrollBar?.forgetNotified()
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
                quietUntilOffsets = savedOffsets
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

    /**
     * The [horizontal, vertical] position to return to, used so that ViewChanged during measuring again (from when the position becomes 0 temporarily until it reaches the position restored by [restoreSavedOffsets])
     * is not notified to the viewport's ChangeListeners and the scroll bars' AdjustmentListeners. null when not in that quiet period.
     * Swing's revalidate does not change the position, so notifying the intermediate positions makes code such as "load more when the end is reached" run extra times.
     */
    private var quietUntilOffsets: DoubleArray? = null

    /** true (not notified) for ViewChanged during measuring again. When the position to return to is reached, the quiet period ends (that notification is not notified either, since the position is unchanged). */
    private fun isQuietWhileRemeasuring(): Boolean {
        val target = quietUntilOffsets ?: return false
        if (savedOffsets == null && isAt(target)) quietUntilOffsets = null
        return true
    }

    private fun isAt(offsets: DoubleArray): Boolean =
        abs(horizontalOffset - offsets[0]) < OFFSET_TOLERANCE && abs(verticalOffset - offsets[1]) < OFFSET_TOLERANCE

    private fun restoreSavedOffsets() {
        val saved = savedOffsets ?: return
        savedOffsets = null
        updateLayout()
        // If the content shrank, the ScrollViewer rounds to the scrollable amount, so the rounded position becomes the one to return to
        val target = doubleArrayOf(min(saved[0], scrollableWidth), min(saved[1], scrollableHeight))
        if (isAt(target)) {
            quietUntilOffsets = null
        } else {
            quietUntilOffsets = target
            applyChangeView(target[0], target[1])
        }
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
        addViewChangedListener(adapter)
        viewChangedTokens.addKotlinAdapter(listener, adapter)
    }

    fun addViewChangedListener(listener: Runnable) {
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
        removeViewChangedListener(adapter)
    }

    fun removeViewChangedListener(listener: Runnable) {
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
            watchSizes()
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
        /** The difference (DIP) at which [isQuietWhileRemeasuring] considers the position to return to reached. */
        private const val OFFSET_TOLERANCE = 0.5

        /** The kind of scroll position request in [WComponent.laidOutRequests]. */
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
