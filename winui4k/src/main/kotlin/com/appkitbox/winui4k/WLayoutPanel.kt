package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.lang.ref.WeakReference

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
 *
 * When the preferred size changes after laying out, the ancestor WLayoutPanels lay out again too (so that a nested WLayoutPanel or a WLayoutPanel in a [WBorder]
 * is laid out outside with its size after measuring its children, as with nested JPanels in Swing).
 *
 * As the content of a [WScrollPane] / [WScrollView], it gets the larger of the preferred size and the viewport, as with JViewport
 * (the preferred size is set as MinWidth / MinHeight to decide the scroll range, and it is stretched if smaller than the viewport).
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

    override val isCanvasElement: Boolean get() = true

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

    /** The preferred size from the previous layout (the ancestors lay out again when it changes). null if not laid out yet. */
    private var lastPreferred: WSize? = null

    /** The value the user set to [minimumSize] (kept separately because, as scroll content, the preferred size is written to the native MinWidth / MinHeight). */
    private var userMinimum: WSize = ZERO_SIZE

    /** Whether the preferred size is written to MinWidth / MinHeight as scroll content. */
    private var scrollMinimumApplied = false

    /**
     * Whether it is shown as the content of a [WContentDialog] (WContentDialog writes it). Then, to expand the dialog to the preferred size as JDialog's pack does,
     * the preferred size is set as MinWidth / MinHeight as with scroll content, and it is laid out with the larger of that and the dialog's size.
     */
    internal var isDialogContent = false
        set(value) {
            if (field == value) return
            field = value
            revalidate()
        }

    internal fun noteAssignedSize(width: Double, height: Double) {
        assignedSize = WSize(width, height)
    }

    init {
        // The handlers stay in native code (the Canvas), so refer to the panel weakly. Capturing it strongly would form the cycle "Canvas → handler → panel → Canvas",
        // and neither the panel nor its children would be collected even after the window is closed or the panel removed (doc/memory_management.md "Handling cycles that cross the boundary").
        // While shown, the panel is held by its parent container (the WFrame if it is the window content)
        val panel = WeakReference(this)
        frameworkElement.addEventHandler(
            "WinUI4K.LayoutPanelSizeChangedHandler",
            XamlInterop.IID_SizeChangedEventHandler,
            XamlInterop.IFrameworkElement_add_SizeChanged,
        ) { _, _ -> ModalLoop.duringLayout { panel.get()?.scheduleLayout() } }
        frameworkElement.addEventHandler(
            "WinUI4K.LayoutPanelLoadedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> ModalLoop.duringLayout { panel.get()?.onLoaded() } }
    }

    private fun onLoaded() {
        loaded = true
        // A measurement taken before joining the visual tree can incorrectly return 0, and once
        // cached it stays 0 on later passes. Force every child to be re-measured once it joins
        componentList.forEach { it.invalidateNaturalSize() }
        scheduleLayout()
    }

    /** add with constraints passes them to the layout manager (with a null layout, they are not used, as in Swing). */
    override val acceptsConstraints: Boolean get() = true

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
        get() = layoutOverride(userMinimum, ZERO_SIZE) { it.minimumLayoutSize(this) }
        set(value) {
            super.minimumSize = value
            userMinimum = value
            if (scrollMinimumApplied) revalidate() // Rewrite the larger of this and the preferred size
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
     *
     * Positions are in WinUI coordinates, and in a right-to-left ([flowDirection]) panel XAML draws the content mirrored. So placing children
     * computed the same way as left-to-right makes them naturally line up from the right (WinUI4K's managers compute in the same visual coordinates as Swing and place them mirrored).
     */
    fun setBounds(component: WComponent, x: Double, y: Double, width: Double, height: Double) {
        if (component is WLayoutPanel) component.noteAssignedSize(width, height)
        statics.call(XamlInterop.ICanvasStatics_SetLeft, component.uiElement.ptr, x)
        statics.call(XamlInterop.ICanvasStatics_SetTop, component.uiElement.ptr, y)
        val sizeChanged = component.appliedWidth.toRawBits() != width.toRawBits() ||
            component.appliedHeight.toRawBits() != height.toRawBits()
        component.setLayoutSize(width, height)
        // A Canvas in a Canvas (a nested WLayoutPanel etc.) is not measured again by XAML just by changing Width / Height
        // ([WComponent.remeasureInCanvas]、doc/verification.md)
        if (sizeChanged && component.isCanvasElement) component.remeasureInCanvas()
    }

    /**
     * Places a child in on-screen visual coordinates (as in Swing, measured from the panel's left edge even in right-to-left flow). WinUI4K's layout managers
     * compute the visual position with the same calculation as Swing and call this.
     *
     * XAML draws the content of right-to-left elements mirrored (CUIElement::GetShouldFlipRTL in microsoft-ui-xaml's uielement.cpp), so
     * in a right-to-left panel the mirrored x (the position measured from the panel's right edge) becomes Canvas.Left. [setBounds] does not mirror
     * (it stays in WinUI coordinates; to handle right-to-left flow in your own manager, leave the mirroring to XAML and place children the same as left-to-right).
     */
    internal fun setVisualBounds(component: WComponent, x: Double, y: Double, width: Double, height: Double) {
        val left = if (flowDirection == FlowDirection.RIGHT_TO_LEFT) layoutSize.width - x - width else x
        setBounds(component, left, y, width, height)
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
            val inScroll = isScrollContent || isDialogContent
            if (inScroll) {
                val fitted = fitScrollContent(manager, containerWidth, containerHeight)
                containerWidth = fitted.width
                containerHeight = fitted.height
            } else {
                clearScrollMinimum()
            }
            if (loaded && assigned == null && !inScroll) {
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
        // 6. If measuring the children again changed the preferred size, make the ancestors that used it lay out again
        notifyIfPreferredSizeChanged()
        if (layoutPending) {
            layoutPending = false
            scheduleLayout()
        }
    }

    /**
     * The layout area of scroll content: as with JViewport, the larger of the preferred size and the viewport ([width] x [height], the size the ScrollViewer gave).
     * The preferred size is set as MinWidth / MinHeight to decide the scroll range (in this pass right after writing it, the viewport still has
     * the previous size, so the larger one is taken here too).
     */
    private fun fitScrollContent(manager: WLayoutManager, width: Double, height: Double): WSize {
        val preferred = manager.preferredLayoutSize(this)
        applyScrollMinimum(preferred)
        selfSizedWidth = false
        selfSizedHeight = false
        return WSize(
            if (this.width.isNaN()) maxOf(width, preferred.width) else width,
            if (this.height.isNaN()) maxOf(height, preferred.height) else height,
        )
    }

    /** If the preferred size differs from the previous layout, makes the ancestors lay out again ([preferredSizeChanged]). */
    private fun notifyIfPreferredSizeChanged() {
        val preferred = preferredSize()
        if (preferred == lastPreferred) return
        lastPreferred = preferred
        preferredSizeChanged()
    }

    /** Whether this is the content of a [WScrollPane] / [WScrollView]. */
    private val isScrollContent: Boolean
        get() = when (val p = parent) {
            is WScrollPane -> p.content === this
            is WScrollView -> p.content === this
            else -> false
        }

    /** As scroll content, sets MinWidth / MinHeight of the dimensions not given explicitly to the preferred size (or the user's minimum size if the preferred size is smaller). */
    private fun applyScrollMinimum(preferred: WSize) {
        scrollMinimumApplied = true
        val minWidth = if (width.isNaN()) maxOf(userMinimum.width, preferred.width) else userMinimum.width
        val minHeight = if (height.isNaN()) maxOf(userMinimum.height, preferred.height) else userMinimum.height
        putMinimum(minWidth, minHeight)
    }

    /** When it is no longer scroll content, restores MinWidth / MinHeight to the user's minimum size. */
    private fun clearScrollMinimum() {
        if (!scrollMinimumApplied) return
        scrollMinimumApplied = false
        putMinimum(userMinimum.width, userMinimum.height)
    }

    private fun putMinimum(minWidth: Double, minHeight: Double) {
        if (frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MinWidth) != minWidth) {
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MinWidth, minWidth)
        }
        if (frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MinHeight) != minHeight) {
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MinHeight, minHeight)
        }
    }

    /** Tells the ancestors the preferred size changed: drops the ancestors' natural size cache and schedules a relayout of the ancestor WLayoutPanels. */
    private fun preferredSizeChanged() {
        var ancestor = parent
        while (ancestor != null) {
            ancestor.invalidateNaturalSize()
            if (ancestor is WLayoutPanel) ancestor.scheduleLayout()
            ancestor = ancestor.parent
        }
    }

    private companion object {
        val ZERO_SIZE = WSize(0.0, 0.0)
        val UNBOUNDED_SIZE = WSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        /** Attached-property operations for Canvas (ICanvasStatics). */
        val statics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Canvas, XamlInterop.IID_ICanvasStatics) }
    }
}
