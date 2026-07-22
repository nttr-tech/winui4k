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
 * When a child's content (e.g. its text) changes and its natural size changes with it, call that
 * child's [WComponent.invalidateNaturalSize] and this panel's [revalidate].
 */
class WLayoutPanel(layout: WLayoutManager? = null) : WContainer(
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
        addSizeChangedListener { revalidate() }
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
        layout?.addLayoutComponent(component, constraints)
        super.add(component)
        revalidate()
    }

    override fun add(component: WComponent) = add(component, null)

    override fun remove(component: WComponent) {
        if (!mutableLayoutChildren.remove(component)) return
        layout?.removeLayoutComponent(component)
        super.remove(component)
        revalidate()
    }

    override fun removeAll() {
        val manager = layout
        if (manager != null) mutableLayoutChildren.forEach { manager.removeLayoutComponent(it) }
        mutableLayoutChildren.clear()
        super.removeAll()
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
     * turn coalesce into a single run.
     */
    fun revalidate() {
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
            // 1. Clear the explicit size of any child whose natural size is invalid, 2. run a
            //    synchronous Measure to fix its preferred size, and 3. cache it (Canvas measures
            //    children with an infinite constraint, so this equals the natural size)
            mutableLayoutChildren.forEach { child ->
                if (child.naturalSize == null) child.clearLayoutSizeForMeasure()
            }
            uiElement.call(XamlInterop.IUIElement_UpdateLayout)
            mutableLayoutChildren.forEach { child ->
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
            revalidate()
        }
    }

    private companion object {
        /** Attached-property operations for Canvas (ICanvasStatics). */
        val statics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasStatics) }
    }
}
