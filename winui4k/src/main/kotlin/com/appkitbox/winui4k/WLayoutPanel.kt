package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JPanel-with-a-LayoutManager-like: a [WLayoutManager] (a Kotlin implementation) computes where
 * children are placed. It's built on a WinUI 3 Canvas (the only panel that doesn't auto-arrange
 * its children) — the manager's computed position is applied to each child via Canvas.SetLeft/
 * SetTop, and its computed size via Width/Height.
 *
 * Canvas always reports its own DesiredSize as 0x0, so in contexts where the parent can't assign
 * it a size (e.g. inside a WPanel), [preferredSize] is applied to itself to determine its own
 * size. Once that happens, it keeps sizing itself this way going forward (it won't follow a
 * parent's STRETCH).
 *
 * When a child's content (e.g. its text) changes and its natural size changes with it, call that child's [WComponent.revalidate]
 * (equivalent to calling the child's [WComponent.invalidateNaturalSize] and this panel's [revalidate]).
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

    /** The children under layout management (in order; same as [components]). Used by WLayoutManager implementations to enumerate them. */
    val layoutChildren: List<WComponent> get() = componentList

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
        addSizeChangedListener { scheduleLayout() }
        addLoadedListener {
            loaded = true
            // A measurement taken before joining the visual tree can incorrectly return 0, and once
            // cached it stays 0 on later passes. Force every child to be re-measured once it joins
            componentList.forEach { it.invalidateNaturalSize() }
            scheduleLayout()
        }
    }

    /**
     * Adds a child with constraints (like Container.add(Component, Object)). If the manager rejects the constraints and throws,
     * the child is not added.
     */
    fun add(component: WComponent, constraints: Any?) {
        addImpl(component, constraints, -1)
    }

    /**
     * Adds a child with constraints at position [index] (-1 is the end) (like Container.add(Component, Object, int)).
     * IllegalArgumentException if [index] is out of range.
     */
    fun add(component: WComponent, constraints: Any?, index: Int) {
        addImpl(component, constraints, index)
    }

    override fun onComponentAdded(component: WComponent, constraints: Any?) {
        layout?.addLayoutComponent(component, constraints)
        revalidate()
    }

    override fun onComponentRemoved(component: WComponent) {
        layout?.removeLayoutComponent(component)
        revalidate()
    }

    override fun preferredSize(): WSize {
        val manager = layout ?: return super.preferredSize()
        if (!width.isNaN() && !height.isNaN()) return WSize(width, height)
        val preferred = manager.preferredLayoutSize(this)
        return WSize(
            if (width.isNaN()) preferred.width else width,
            if (height.isNaN()) preferred.height else height,
        )
    }

    /**
     * The minimum size (like Container.getMinimumSize). If [minimumSize] is not set,
     * the value of [WLayoutManager.minimumLayoutSize]. Without a [layout], stays [WComponent.minimumSize].
     */
    override var minimumSize: WSize
        get() = layoutOverride(super.minimumSize, ZERO_SIZE) { it.minimumLayoutSize(this) }
        set(value) {
            super.minimumSize = value
        }

    /**
     * The maximum size (like Container.getMaximumSize). If [maximumSize] is not set,
     * the value of [WLayoutManager.maximumLayoutSize]. Without a [layout], stays [WComponent.maximumSize].
     */
    override var maximumSize: WSize
        get() = layoutOverride(super.maximumSize, UNBOUNDED_SIZE) { it.maximumLayoutSize(this) }
        set(value) {
            super.maximumSize = value
        }

    /** Like Container.getAlignmentX: if [alignmentX] is not set, [layout]'s [WLayoutManager.getLayoutAlignmentX]. */
    override val defaultAlignmentX: Float
        get() = layout?.getLayoutAlignmentX(this) ?: super.defaultAlignmentX

    /** Like Container.getAlignmentY: if [alignmentY] is not set, [layout]'s [WLayoutManager.getLayoutAlignmentY]. */
    override val defaultAlignmentY: Float
        get() = layout?.getLayoutAlignmentY(this) ?: super.defaultAlignmentY

    /** Returns the explicit setting if there is one ([native] other than [unset]), otherwise the layout manager's computed value. */
    private inline fun layoutOverride(native: WSize, unset: WSize, fromLayout: (WLayoutManager) -> WSize): WSize {
        val manager = layout
        return if (manager == null || native != unset) native else fromLayout(manager)
    }

    /** Like Container.invalidate: in addition to [WComponent.invalidate], notifies [layout] with [WLayoutManager.invalidateLayout]. */
    override fun invalidate() {
        super.invalidate()
        layout?.invalidateLayout(this)
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
     * Schedules a re-layout (like Container.revalidate). Multiple calls within the same UI thread
     * turn coalesce into a single run. Also schedules a re-layout of the ancestor WLayoutPanels reached through [parent]
     * (because this panel's preferred size may change).
     */
    final override fun revalidate() {
        scheduleLayout()
        super.revalidate()
    }

    /** Schedules a re-layout of only this panel (not propagated to ancestors). */
    internal fun scheduleLayout() {
        if (layoutPending) return
        layoutPending = true
        if (inLayout) return // gets rescheduled when performLayout finishes
        WinUiUtilities.invokeLater { if (layoutPending) performLayout() } // Does nothing if validate already did it
    }

    /** From [validate]: runs the scheduled re-layout now and follows it down to the child containers. */
    override fun validateTree() {
        if (layoutPending && !inLayout) performLayout()
        super.validateTree()
    }

    // The branching is inherent to the layout algorithm's spec (size-determination priority order)
    @Suppress("CyclomaticComplexMethod", "ComplexCondition")
    private fun performLayout() {
        layoutPending = false
        val manager = layout ?: return
        inLayout = true
        try {
            // 1. Clear the explicit size of any child whose natural size is invalid, 2. run a
            //    synchronous Measure to fix its preferred size, and 3. cache it (Canvas measures
            //    children with an infinite constraint, so this equals the natural size)
            componentList.forEach { child ->
                if (child.naturalSize == null) child.clearLayoutSizeForMeasure()
            }
            uiElement.call(XamlInterop.IUIElement_UpdateLayout)
            componentList.forEach { child ->
                if (child.naturalSize == null) child.naturalSize = child.readDesiredSize()
            }

            // 4. Determine the layout area. Priority order: explicit user value > assignment from
            //    the parent WLayoutPanel > ActualWidth/Height (an assignment from a native parent,
            //    e.g. STRETCH) > preferredSize. Only the last case (a context where no size comes
            //    down, e.g. inside a WPanel) applies self-sizing, and from then on that dimension
            //    keeps tracking the preferred size as the content changes
            val assigned = assignedSize
            if (assigned != null || !width.isNaN()) selfSizedWidth = false
            if (assigned != null || !height.isNaN()) selfSizedHeight = false
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
            if (loaded && assigned == null) {
                if (width.isNaN() && containerWidth <= 0.0) selfSizedWidth = true
                if (height.isNaN() && containerHeight <= 0.0) selfSizedHeight = true
            }
            if (selfSizedWidth || selfSizedHeight || containerWidth <= 0.0 || containerHeight <= 0.0) {
                val preferred = manager.preferredLayoutSize(this)
                if (selfSizedWidth || containerWidth <= 0.0) containerWidth = preferred.width
                if (selfSizedHeight || containerHeight <= 0.0) containerHeight = preferred.height
                if (selfSizedWidth) applyWidth(containerWidth)
                if (selfSizedHeight) applyHeight(containerHeight)
            }
            layoutSize = WSize(containerWidth, containerHeight)

            // 5. Place the children
            manager.layoutContainer(this)
        } finally {
            inLayout = false
        }
        if (layoutPending) {
            layoutPending = false
            scheduleLayout()
        }
    }

    private companion object {
        val ZERO_SIZE = WSize(0.0, 0.0)
        val UNBOUNDED_SIZE = WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        /** Attached-property operations for Canvas (ICanvasStatics). */
        val statics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasStatics) }
    }
}
