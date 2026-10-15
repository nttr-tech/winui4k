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
 * DesiredSize. This is decided again on every layout pass, so a panel that sized itself once (e.g.
 * in a pass that ran before its parent arranged it) follows the parent's STRETCH again as soon as
 * the parent gives it a size.
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

    /** Whether the last layout pass applied the preferred size to itself as Width / Height ([Sizing.SELF]). */
    private var selfSizedWidth = false
    private var selfSizedHeight = false

    /**
     * The MinWidth / MinHeight requested from a native parent that sizes this panel by its DesiredSize (0 = not
     * requested; [Sizing.REQUEST]): a Canvas measures as 0x0, but FrameworkElement raises the DesiredSize to
     * MinWidth / MinHeight.
     */
    private var requestedMinWidth = 0.0
    private var requestedMinHeight = 0.0

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

    /** A Canvas measures as 0x0 at any width, so the height for a width is the layout manager's preferred height. */
    override fun preferredHeight(forWidth: Double): Double = preferredSize().height

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

    private fun performLayout() {
        layoutPending = false
        val manager = layout ?: return
        inLayout = true
        try {
            // 1-3. Measure the children whose natural size is unknown
            measureChildren()

            // 4. Determine the layout area. Priority order: explicit user value > assignment from
            //    the parent WLayoutPanel > ActualWidth/Height (an assignment from a native parent,
            //    e.g. STRETCH) > preferredSize. How a native parent sizes this panel is decided
            //    again on every pass (sizingOf)
            val assigned = assignedSize
            val available = if (loaded && assigned == null) availableSize() else null
            val slot = if (available != null) layoutSlot() else WSize(0.0, 0.0)
            val currentWidth = if (!width.isNaN()) width else assigned?.width ?: actualWidth
            val currentHeight = if (!height.isNaN()) height else assigned?.height ?: actualHeight
            val widthSizing = sizingOf(
                currentWidth,
                given = !width.isNaN() || assigned != null,
                available = available?.width,
                slot = slot.width,
                stretched = horizontalAlignment == HorizontalAlignment.STRETCH,
                requested = requestedMinWidth,
            )
            val heightSizing = sizingOf(
                currentHeight,
                given = !height.isNaN() || assigned != null,
                available = available?.height,
                slot = slot.height,
                stretched = verticalAlignment == VerticalAlignment.STRETCH,
                requested = requestedMinHeight,
            )
            val preferred = if (widthSizing == Sizing.GIVEN && heightSizing == Sizing.GIVEN) {
                null
            } else {
                manager.preferredLayoutSize(this)
            }
            layoutSize = WSize(
                sizeWidth(widthSizing, currentWidth, preferred, available, native = assigned == null),
                sizeHeight(heightSizing, currentHeight, preferred, available, native = assigned == null),
            )

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

    /** How this panel gets its size in one dimension (decided on every layout pass by [sizingOf]). */
    private enum class Sizing {
        /** The size is explicit, assigned by a parent WLayoutPanel, or arranged by a native parent. */
        GIVEN,

        /** The parent gives nothing (an unlimited offer, or it arranges this panel at its DesiredSize): Width / Height = preferred. */
        SELF,

        /** The parent offers more than it arranges (it sizes itself to its content): MinWidth / MinHeight = preferred, capped at the offer. */
        REQUEST,

        /** No size yet (e.g. a pass before the parent arranged this panel): lay out at the preferred size without applying it. */
        PENDING,
    }

    /**
     * Decides how this panel gets its size in one dimension from the size it has now ([current]; [given] when it is
     * explicit or assigned by a parent WLayoutPanel) and, for a native parent after Loaded, what the parent offered in
     * its last measure ([available], LayoutInformation.GetAvailableSize) and the slot it arranged this panel in ([slot],
     * LayoutInformation.GetLayoutSlot). An unlimited offer or an alignment other than STRETCH ([stretched]) means the
     * parent sizes this panel by its DesiredSize; an offer larger than the slot means the parent would give more if
     * asked. While a request is in effect ([requested] > 0), a slot no larger than the request is the parent honoring
     * it, so the request stays.
     */
    @Suppress("LongParameterList") // One dimension's inputs, passed separately so that both dimensions share the decision
    private fun sizingOf(
        current: Double,
        given: Boolean,
        available: Double?,
        slot: Double,
        stretched: Boolean,
        requested: Double,
    ): Sizing = when {
        given -> Sizing.GIVEN
        available == null -> if (current > 0.0) Sizing.GIVEN else Sizing.PENDING
        available.isInfinite() || !stretched -> Sizing.SELF
        available > slot + SIZE_TOLERANCE || (requested > 0.0 && slot <= requested + SIZE_TOLERANCE) -> Sizing.REQUEST
        current <= 0.0 -> Sizing.PENDING
        else -> Sizing.GIVEN
    }

    /**
     * Applies [sizing] to the width and returns the width to lay the children out in. A width this panel applied to
     * itself in an earlier pass is taken off once it is no longer self-sized ([native] is false when a parent
     * WLayoutPanel assigns the width instead).
     */
    private fun sizeWidth(sizing: Sizing, current: Double, preferred: WSize?, available: WSize?, native: Boolean): Double {
        if (sizing == Sizing.SELF) {
            applyWidth(checkNotNull(preferred).width)
        } else if (selfSizedWidth && native && width.isNaN()) {
            applyWidth(Double.NaN)
        }
        selfSizedWidth = sizing == Sizing.SELF
        val request = if (sizing == Sizing.REQUEST) minOf(checkNotNull(preferred).width, checkNotNull(available).width) else 0.0
        if (request != requestedMinWidth) {
            requestedMinWidth = request
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MinWidth, request)
        }
        return layoutExtent(sizing, current, preferred?.width)
    }

    /** The height counterpart of [sizeWidth]. */
    private fun sizeHeight(sizing: Sizing, current: Double, preferred: WSize?, available: WSize?, native: Boolean): Double {
        if (sizing == Sizing.SELF) {
            applyHeight(checkNotNull(preferred).height)
        } else if (selfSizedHeight && native && height.isNaN()) {
            applyHeight(Double.NaN)
        }
        selfSizedHeight = sizing == Sizing.SELF
        val request = if (sizing == Sizing.REQUEST) minOf(checkNotNull(preferred).height, checkNotNull(available).height) else 0.0
        if (request != requestedMinHeight) {
            requestedMinHeight = request
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MinHeight, request)
        }
        return layoutExtent(sizing, current, preferred?.height)
    }

    /** The extent to lay the children out in: the current size, or the preferred size while the panel has none. */
    private fun layoutExtent(sizing: Sizing, current: Double, preferred: Double?): Double = when (sizing) {
        Sizing.GIVEN -> current
        Sizing.REQUEST -> if (current > 0.0) current else checkNotNull(preferred)
        Sizing.SELF, Sizing.PENDING -> checkNotNull(preferred)
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
