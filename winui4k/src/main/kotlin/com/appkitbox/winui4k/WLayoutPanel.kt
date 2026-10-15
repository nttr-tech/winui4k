package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JPanel-with-a-LayoutManager-like: a [WLayoutManager] (a Kotlin implementation) computes where
 * children are placed. It's built on a WinUI 3 Canvas (the only panel that doesn't auto-arrange
 * its children) — the manager's computed position is applied to each child via Canvas.SetLeft/
 * SetTop, and its computed size via Width/Height.
 *
 * Canvas always reports its own DesiredSize as 0x0, so a native (non-WLayoutPanel) parent that
 * sizes its content by DesiredSize would not give it room. In a dimension where the parent
 * measures without a limit (e.g. the vertical direction of a ScrollViewer or a vertical WPanel) or
 * gives it nothing, [preferredSize] is applied to itself as Width/Height. In a dimension where the
 * parent offers more than it gives (e.g. a ContentDialog sizing itself to its content), the
 * preferred size, capped at the offered size, is set as MinWidth/MinHeight so it becomes the
 * DesiredSize. Once either happens, it keeps sizing itself this way going forward (a self-sized
 * dimension won't follow a parent's STRETCH).
 *
 * When a child's content (e.g. its text) changes and its natural size changes with it, call that
 * child's [WComponent.invalidateNaturalSize] and this panel's [revalidate].
 */
class WLayoutPanel @JvmOverloads constructor(layout: WLayoutManager? = null) : WContainer(
    Activation.composeDefault(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasFactory),
) {
    /** The layout manager that computes where children go. Replacing it triggers a re-layout. */
    var layout: WLayoutManager? = layout
        set(value) {
            field = value
            revalidate()
        }

    /** Inner padding (Canvas has no Padding, so this is tracked in Kotlin and used by the manager's computation). */
    var insets: WInsets = WInsets()
        set(value) {
            field = value
            revalidate()
        }

    private val mutableLayoutChildren = mutableListOf<WComponent>()

    /** The children under layout management, in addition order. Used by WLayoutManager implementations to enumerate them. */
    val layoutChildren: List<WComponent> get() = mutableLayoutChildren

    /**
     * The current layout area (the whole area, before subtracting [insets]). Fixed by the layout
     * pass; [WLayoutManager.layoutContainer] places children within this area.
     */
    var layoutSize: WSize = WSize(0.0, 0.0)
        private set

    /** Whether revalidate has been called and performLayout is pending (coalesces calls within the same UI thread turn). */
    private var layoutPending = false

    /** Whether performLayout is currently running (a revalidate during that run is deferred to a reschedule at the end). */
    private var inLayout = false

    /** Whether this has been added to the visual tree and ActualWidth has settled (self-sizing is only judged after Loaded). */
    private var loaded = false

    /** Records which dimensions were self-sized via preferredSize because the parent didn't assign a size. */
    private var selfSizedWidth = false
    private var selfSizedHeight = false

    /** Records which dimensions request the preferred size from a native parent via MinWidth / MinHeight. */
    private var minSizedWidth = false
    private var minSizedHeight = false

    /**
     * The size assigned by the parent WLayoutPanel via [setBounds]. null means unassigned (a
     * top-level panel). If assigned, this is used as the layout area (this prevents the
     * misjudgment of seeing ActualWidth = 0 and self-sizing at a moment like right after Loaded,
     * when the parent hasn't called setBounds yet).
     */
    private var assignedSize: WSize? = null

    internal fun noteAssignedSize(width: Double, height: Double) {
        assignedSize = WSize(width, height)
    }

    init {
        // A new size only changes this panel's own layout, so it does not invalidate the panels above
        addSizeChangedListener { scheduleLayout() }
        addLoadedListener {
            loaded = true
            // A measurement taken before joining the visual tree can incorrectly return 0, and once
            // cached it stays 0 on later passes. Force every child to be re-measured once it joins
            mutableLayoutChildren.forEach { it.invalidateNaturalSize() }
            revalidate()
        }
    }

    /** Adds a child with constraints (like Container.add(Component, Object)). */
    fun add(component: WComponent, constraints: Any?) {
        mutableLayoutChildren.add(component)
        component.layoutParent = this
        layout?.addLayoutComponent(component, constraints)
        super.add(component)
        revalidate()
    }

    override fun add(component: WComponent) = add(component, null)

    override fun remove(component: WComponent) {
        if (!mutableLayoutChildren.remove(component)) return
        component.layoutParent = null
        layout?.removeLayoutComponent(component)
        super.remove(component)
        revalidate()
    }

    override fun removeAll() {
        val manager = layout
        if (manager != null) mutableLayoutChildren.forEach { manager.removeLayoutComponent(it) }
        mutableLayoutChildren.forEach { it.layoutParent = null }
        mutableLayoutChildren.clear()
        super.removeAll()
        revalidate()
    }

    override fun preferredSize(): WSize {
        val manager = layout ?: return super.preferredSize()
        if (!width.isNaN() && !height.isNaN()) return WSize(width, height)
        // The manager adds up the children's preferred sizes, so first measure the children whose natural size is
        // unknown. A panel above asks for this size from its own layout pass, which can run before this panel's
        // pending layout has measured them; the fallback (DesiredSize) still reflects the size this panel assigned
        // them last time (0 before the first real measurement), so the panel above would size this one with it.
        // Measuring clears those children's assigned sizes, so lay this panel out again to put them back
        if (measureChildren()) scheduleLayout()
        val preferred = manager.preferredLayoutSize(this)
        return WSize(
            if (width.isNaN()) preferred.width else width,
            if (height.isNaN()) preferred.height else height,
        )
    }

    /**
     * Assigns a child's position and size (like Component.setBounds). Called by a WLayoutManager
     * implementation from [WLayoutManager.layoutContainer]. Position is the Canvas.SetLeft/SetTop
     * attached properties; size is Width/Height (a put is skipped if the value is unchanged).
     */
    fun setBounds(component: WComponent, x: Double, y: Double, width: Double, height: Double) {
        if (component is WLayoutPanel) component.noteAssignedSize(width, height)
        statics.call(XamlInterop.ICanvasStatics_SetLeft, component.uiElement.ptr, x)
        statics.call(XamlInterop.ICanvasStatics_SetTop, component.uiElement.ptr, y)
        component.setLayoutSize(width, height)
    }

    /**
     * Schedules a re-layout (like Container.revalidate) and, since this panel's natural size may change with its
     * content, re-measures it in the panels above. Multiple calls within the same UI thread turn coalesce into a
     * single run.
     */
    override fun revalidate() {
        invalidateNaturalSize()
        layoutParent?.revalidate()
        scheduleLayout()
    }

    /**
     * 1. Clears the explicit size of any child whose natural size is invalid, 2. runs a synchronous Measure to fix
     * its preferred size, and 3. caches it (Canvas measures children with an infinite constraint, so this equals the
     * natural size). Returns whether any child was measured.
     *
     * A nested WLayoutPanel with a manager is skipped: its preferred size comes from its manager, not from a Measure
     * (a Canvas always measures as 0x0), and clearing its assigned size here would collapse it to 0 for nothing.
     */
    private fun measureChildren(): Boolean {
        val stale = mutableLayoutChildren.filter { it.naturalSize == null && !it.isManagedLayoutPanel() }
        if (stale.isEmpty()) return false
        stale.forEach { it.clearLayoutSizeForMeasure() }
        uiElement.call(XamlInterop.IUIElement_UpdateLayout)
        stale.forEach { it.naturalSize = it.readDesiredSize() }
        return true
    }

    /** Schedules a layout of this panel only. */
    private fun scheduleLayout() {
        if (layoutPending) return
        layoutPending = true
        if (inLayout) return // gets rescheduled when performLayout finishes
        WinUiUtilities.invokeLater { performLayout() }
    }

    // The branching is inherent to the layout algorithm's spec (size-determination priority order)
    @Suppress("CyclomaticComplexMethod", "ComplexCondition")
    private fun performLayout() {
        layoutPending = false
        val manager = layout ?: return
        inLayout = true
        try {
            // 1-3. Measure the children whose natural size is unknown
            measureChildren()

            // 4. Determine the layout area. Priority order: explicit user value > assignment from
            //    the parent WLayoutPanel > ActualWidth/Height (an assignment from a native parent,
            //    e.g. STRETCH) > preferredSize. Only the last case (a context where no size comes
            //    down, e.g. inside a WPanel) applies self-sizing, and from then on that dimension
            //    keeps tracking the preferred size as the content changes
            val assigned = assignedSize
            if (assigned != null || !width.isNaN()) {
                selfSizedWidth = false
                minSizedWidth = false
            }
            if (assigned != null || !height.isNaN()) {
                selfSizedHeight = false
                minSizedHeight = false
            }
            withdrawMinSizeRequests()
            var containerWidth = when {
                !width.isNaN() -> width
                assigned != null -> assigned.width
                else -> actualWidth
            }
            var containerHeight = when {
                !height.isNaN() -> height
                assigned != null -> assigned.height
                else -> actualHeight
            }
            //    A native parent's offer (LayoutInformation.GetAvailableSize) tells how it sizes this panel: an
            //    unlimited offer means it sizes the content by DesiredSize (self-size), and an offer larger than
            //    the slot it arranged this panel in means it would give more if asked (request it via MinWidth /
            //    MinHeight). A parent that stretches this panel (e.g. a window) arranges it in the whole offer
            val available = if (loaded && assigned == null) availableSize() else null
            if (available != null) classifyNativeParent(available, containerWidth, containerHeight)
            if (selfSizedWidth || selfSizedHeight || containerWidth <= 0.0 || containerHeight <= 0.0 ||
                (available != null && (minSizedWidth || minSizedHeight))
            ) {
                val preferred = manager.preferredLayoutSize(this)
                if (selfSizedWidth || containerWidth <= 0.0) containerWidth = preferred.width
                if (selfSizedHeight || containerHeight <= 0.0) containerHeight = preferred.height
                if (selfSizedWidth) applyWidth(containerWidth)
                if (selfSizedHeight) applyHeight(containerHeight)
                if (available != null) requestPreferredSize(preferred, available)
            }
            layoutSize = WSize(containerWidth, containerHeight)

            // 5. Place the children
            manager.layoutContainer(this)
        } finally {
            inLayout = false
        }
        if (layoutPending) {
            layoutPending = false
            revalidate()
        }
    }

    /**
     * Decides, per dimension, how a native parent sizes this panel from what it offered ([available]) and arranged:
     * nothing given or an unlimited offer → self-size, an offer larger than the arranged slot → request via MinWidth /
     * MinHeight. Both are sticky; a dimension with an explicit size is left alone.
     */
    private fun classifyNativeParent(available: WSize, containerWidth: Double, containerHeight: Double) {
        val slot = layoutSlot()
        if (width.isNaN() && !selfSizedWidth) {
            if (containerWidth <= 0.0 || available.width.isInfinite()) {
                selfSizedWidth = true
            } else if (available.width > slot.width + SIZE_TOLERANCE) {
                minSizedWidth = true
            }
        }
        if (height.isNaN() && !selfSizedHeight) {
            if (containerHeight <= 0.0 || available.height.isInfinite()) {
                selfSizedHeight = true
            } else if (available.height > slot.height + SIZE_TOLERANCE) {
                minSizedHeight = true
            }
        }
    }

    /** The size the parent offered in its last measure (LayoutInformation.GetAvailableSize; infinite if unlimited). */
    private fun availableSize(): WSize {
        val size = Xaml.readPoint(layoutInformation, XamlInterop.ILayoutInformationStatics_GetAvailableSize, uiElement.ptr)
        return WSize(size[0], size[1])
    }

    /** The size of the slot the parent arranged this panel in last time (LayoutInformation.GetLayoutSlot, margins included). */
    private fun layoutSlot(): WSize = Ffi.backend.withScope { scope ->
        val rect = scope.allocate(RECT_BYTES)
        layoutInformation.call(XamlInterop.ILayoutInformationStatics_GetLayoutSlot, frameworkElement.ptr, rect)
        val memory = Ffi.backend.memory
        WSize(Float.fromBits(memory.getInt(rect, 8)).toDouble(), Float.fromBits(memory.getInt(rect, 12)).toDouble())
    }

    /** The MinWidth / MinHeight last written by [requestPreferredSize] (0 = not requested). */
    private var requestedMinWidth = 0.0
    private var requestedMinHeight = 0.0

    /**
     * Makes a native parent that sizes this panel by its DesiredSize give it the [preferred] size: a Canvas measures
     * as 0x0, but FrameworkElement raises the DesiredSize to MinWidth / MinHeight. The request is capped at the size
     * the parent offers ([available]), so the panel still gets no more than the parent can give.
     */
    private fun requestPreferredSize(preferred: WSize, available: WSize) {
        if (minSizedWidth && !selfSizedWidth) {
            val value = minOf(preferred.width, available.width)
            if (value != requestedMinWidth) {
                requestedMinWidth = value
                frameworkElement.call(XamlInterop.IFrameworkElement_put_MinWidth, value)
            }
        }
        if (minSizedHeight && !selfSizedHeight) {
            val value = minOf(preferred.height, available.height)
            if (value != requestedMinHeight) {
                requestedMinHeight = value
                frameworkElement.call(XamlInterop.IFrameworkElement_put_MinHeight, value)
            }
        }
    }

    /** Clears MinWidth / MinHeight written by [requestPreferredSize] in a dimension that no longer requests a size. */
    private fun withdrawMinSizeRequests() {
        if (!minSizedWidth && requestedMinWidth != 0.0) {
            requestedMinWidth = 0.0
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MinWidth, 0.0)
        }
        if (!minSizedHeight && requestedMinHeight != 0.0) {
            requestedMinHeight = 0.0
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MinHeight, 0.0)
        }
    }

    private companion object {
        /** Differences below this (DIP) are rounding to physical pixels, not a parent that would give more. */
        const val SIZE_TOLERANCE = 1.0

        /** The size of Windows.Foundation.Rect (X, Y, Width, Height as r4). */
        const val RECT_BYTES = 16L

        /** LayoutInformation statics (the size a parent offered in its last measure). */
        val layoutInformation: ComPtr by lazy {
            Activation.factory(XamlInterop.CLS_LayoutInformation, XamlInterop.IID_ILayoutInformationStatics)
        }

        fun WComponent.isManagedLayoutPanel(): Boolean = this is WLayoutPanel && layout != null

        /** Attached-property operations for Canvas (ICanvasStatics). */
        val statics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasStatics) }
    }
}
