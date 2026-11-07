package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.lang.ref.WeakReference
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.HorizontalAlignment (horizontal position within the space the parent allots).
 * Values extracted from the winmd (Left=0, Center=1, Right=2, Stretch=3).
 */
enum class HorizontalAlignment(internal val native: Int) {
    /** Align to the left. */
    LEFT(0),

    /** Center. */
    CENTER(1),

    /** Align to the right. */
    RIGHT(2),

    /** Stretch to fill the width (default). */
    STRETCH(3),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): HorizontalAlignment = entries.first { it.native == native }
    }
}

/** Microsoft.UI.Xaml.FlowDirection (the direction of text and layout; inherited by children). */
enum class FlowDirection(internal val native: Int) {
    /** Left to right (the default). */
    LEFT_TO_RIGHT(0),

    /** Right to left (Arabic, Hebrew, and so on; the layout is mirrored horizontally). */
    RIGHT_TO_LEFT(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): FlowDirection = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.VerticalAlignment (vertical position within the space the parent allots).
 * Values extracted from the winmd (Top=0, Center=1, Bottom=2, Stretch=3).
 */
enum class VerticalAlignment(internal val native: Int) {
    /** Align to the top. */
    TOP(0),

    /** Center. */
    CENTER(1),

    /** Align to the bottom. */
    BOTTOM(2),

    /** Stretch to fill the height (default). */
    STRETCH(3),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): VerticalAlignment = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.ElementTheme (the UI theme applied to an element and its descendants).
 * Values extracted from the winmd (Default=0, Light=1, Dark=2).
 */
enum class ElementTheme(internal val native: Int) {
    /** Follows the OS's app mode setting (or the parent element's theme) (the default). */
    DEFAULT(0),

    /** Always the light theme. */
    LIGHT(1),

    /** Always the dark theme. */
    DARK(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): ElementTheme = entries.first { it.native == native }
    }
}

/**
 * A thin Swing-like API layer. Everything underneath is a native WinUI 3 control.
 * (Use only inside the WinUiUtilities.invokeLater callback = on the UI thread)
 */
abstract class WComponent internal constructor(
    /** The control's default interface pointer (ITextBox, IButton, ...) */
    internal val inspectable: ComPtr,
) : WDialogParent {
    /**
     * The record of COM references this wrapper owns. When this wrapper becomes GC-unreachable,
     * its owned references (inspectable and each QI'd view) are released on the UI thread.
     * As long as something in the visual tree still holds a reference to the underlying control,
     * the control stays alive.
     */
    private val lifetime = ComLifetime.adopt(this, inspectable)

    /**
     * Ties ownership of [ptr] to this wrapper's lifetime. Any field that long-term holds a view
     * obtained via QueryInterface/getPtr must be wrapped with this — an unwrapped count that
     * happens to be a reference to the control itself would mean the control never gets released
     * even after the wrapper is collected.
     */
    internal fun own(ptr: ComPtr): ComPtr = lifetime.own(ptr)

    /** IUIElement view used for XAML tree operations. */
    internal val uiElement: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_IUIElement)) }

    /** FrameworkElement view (also used as an argument to things like Flyout.ShowAt). */
    internal val frameworkElement: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IFrameworkElement))
    }

    /**
     * A reference from the event handlers that remain on the native object (those of ActionListener and the input
     * listeners) to this wrapper (created when the first listener is added). Not created per listener type; one element
     * shares a single one.
     */
    internal val listenerAnchor: ListenerAnchor<WComponent> by lazy { ListenerAnchor(this) }

    /** DependencyObject view (passed as the target of attached properties like ToolTipService). */
    internal val dependencyObject: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IDependencyObject))
    }

    /**
     * A weak reference to the parent wrapper. If the child held its parent strongly, entrusting the child to the handler of a detached subtree's root via [ListenerAnchor]
     * would create the cycle "root native -> handler -> child -> parent -> ... -> root wrapper -> root native", and the whole subtree would never be collected
     * (doc/memory_management.md, "Handling cycles that cross the boundary"). From parent to child, the container's child list holds the reference strongly.
     */
    private var parentReference: WeakReference<WComponent>? = null

    /**
     * Component.getParent-style: the component that holds this component as a child. Set when it is added to a [WContainer] with [WContainer.add], and when it is
     * put into a component holding a single child ([WBorder.child] / [WScrollPane.content] / [WScrollView.content] / [WExpander.content] /
     * [WTabViewItem.content] / [WSplitView.content] / [WSplitView.pane]); goes back to null when removed.
     * Represents only parent-child relationships made through WinUI4K's API, and does not include a [WFrame]'s content or native parents inside templates.
     *
     * Children reference their parent weakly (strongly from parent to child). When neither the parent nor any wrapper above it is referenced from anywhere,
     * the parent may be collected and this may become null (a parent placed in a window is normally referenced from the [WFrame]'s content).
     */
    val parent: WComponent?
        get() = parentReference?.get()

    /** Replaces the parent record (called only by [WContainer] and single-child hosts). */
    internal fun setParentLink(newParent: WComponent?) {
        parentReference = newParent?.let { WeakReference(it) }
    }

    /** Removes this component from its current parent (same as Swing's add removing it from its old parent first). Does nothing if there is no parent. */
    internal fun removeFromParent() {
        parent?.detachChild(this)
    }

    /** Removes the child [child] from this component (called from [removeFromParent]; implemented by components that have children). */
    internal open fun detachChild(child: WComponent) {
        // Components without children do nothing
    }

    /** Checks whether [child] can be made a child. IllegalArgumentException if it is this component itself or an ancestor (same as Swing). */
    internal fun checkCanAdopt(child: WComponent) {
        var current: WComponent? = this
        while (current != null) {
            require(current !== child) { "cannot make the component itself or a parent (ancestor) a child" }
            current = current.parent
        }
    }

    /**
     * Called by a component holding a single child (WBorder.child and so on) before the native put when it changes the child from [old] to [new].
     * Removes [new] from its old parent and updates the parent records. Hosts must also implement [detachChild], which removes a child.
     */
    internal fun replaceChild(old: WComponent?, new: WComponent?) {
        if (old === new) return
        if (new != null) checkCanAdopt(new)
        if (old != null && old.parent === this) old.setParentLink(null)
        if (new != null) {
            new.removeFromParent()
            new.setParentLink(this)
        }
    }

    var width: Double = Double.NaN
        set(value) {
            field = value
            applyWidth(value)
        }

    var height: Double = Double.NaN
        set(value) {
            field = value
            applyHeight(value)
        }

    /**
     * The last value written to put_Width / put_Height (as its bit pattern, so NaN compares equal to
     * NaN). Used to skip redundant puts of the same value (prevents an infinite
     * layout -> SizeChanged -> re-layout loop).
     */
    private var appliedWidthBits = Double.NaN.toRawBits()
    private var appliedHeightBits = Double.NaN.toRawBits()

    /** The value last written with put_Width (NaN if not written). The width assigned by the layout manager also appears here. */
    internal val appliedWidth: Double
        get() = Double.fromBits(appliedWidthBits)

    /** The value last written with put_Height (NaN if not written). */
    internal val appliedHeight: Double
        get() = Double.fromBits(appliedHeightBits)

    internal fun applyWidth(value: Double) {
        if (value.toRawBits() == appliedWidthBits) return
        appliedWidthBits = value.toRawBits()
        frameworkElement.call(XamlInterop.IFrameworkElement_put_Width, value)
    }

    internal fun applyHeight(value: Double) {
        if (value.toRawBits() == appliedHeightBits) return
        appliedHeightBits = value.toRawBits()
        frameworkElement.call(XamlInterop.IFrameworkElement_put_Height, value)
    }

    /** The actual width after layout (FrameworkElement.ActualWidth). 0 before layout. */
    val actualWidth: Double
        get() = frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth)

    /**
     * Synchronously finishes any pending layout (UIElement.UpdateLayout).
     * Use in tests/measurements that read layout results like [actualWidth] right after a change.
     */
    fun updateLayout() {
        uiElement.call(XamlInterop.IUIElement_UpdateLayout)
    }

    /** The actual height after layout (FrameworkElement.ActualHeight). 0 before layout. */
    val actualHeight: Double
        get() = frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight)

    /**
     * Swing's getSize-style: the actual size after layout (DIP, FrameworkElement.ActualWidth / ActualHeight).
     * (0, 0) for an element not placed in a window or before layout.
     *
     * Swing's getWidth() / getHeight() correspond to the width and height of this [size] ([actualWidth] / [actualHeight]).
     * This class's [width] / [height] are the explicit size (FrameworkElement.Width / Height), which is NaN when unset, so
     * they are different things.
     */
    val size: WSize
        get() = WSize(actualWidth, actualHeight)

    /**
     * Swing's getX-style: the left edge of this element as seen from the parent's top-left (DIP rounded to the nearest
     * integer). The parent is the parent in the XAML visual tree, which includes not only panels and Canvas but also parts
     * of a control's template. It points inside the margin (the left edge of the visible frame). 0 for an element not
     * placed in a window or before layout. Movement by transforms such as rotation (RenderTransform) or by scrolling is not
     * included. The unrounded value is [actualX].
     *
     * If the parent's [flowDirection] is right-to-left, unlike Swing it is in WinUI coordinates (before XAML mirrors the drawing), measured from the parent's right edge
     * to this element's right edge (a child placed by a WLayoutPanel layout manager at x from the visual left edge
     * returns parent width - x - child width).
     */
    val x: Int
        get() = Math.round(actualX).toInt()

    /** Swing's getY-style: the top edge of this element as seen from the parent's top-left (DIP rounded to the nearest integer). See [x] for details. */
    val y: Int
        get() = Math.round(actualY).toInt()

    /** The left edge of this element as seen from the parent's top-left (DIP, UIElement.ActualOffset). The rounded value is [x]. */
    val actualX: Double
        get() = Xaml.actualOffset(uiElement)[0]

    /** The top edge of this element as seen from the parent's top-left (DIP, UIElement.ActualOffset). The rounded value is [y]. */
    val actualY: Double
        get() = Xaml.actualOffset(uiElement)[1]

    /**
     * Swing's getLocation-style: the top-left of this element as seen from the parent's top-left ([x], [y]). The unit is
     * DIP rounded to integers. [WPoint] is also used for screen pixels such as the window position ([WAppWindow.position]),
     * but here it is DIP.
     */
    val location: WPoint
        get() = WPoint(x, y)

    /**
     * Swing's getBounds-style: the position and size of this element as seen from the parent's top-left ([x], [y], width,
     * height). Integers as in Swing's Rectangle, with DIP rounded to the nearest integer. The unrounded values are
     * [actualX] / [actualY] / [size].
     * It is written with [setBounds] (effective inside a parent with a null layout; inside a parent that lays out, the next layout decides it).
     */
    val bounds: WRectangle
        get() = WRectangle(x, y, Math.round(actualWidth).toInt(), Math.round(actualHeight).toInt())

    /**
     * Component.contains(int, int)-like: whether the position ([x], [y], DIP) seen from this component's top-left is inside it.
     * As in Swing, 0 <= x < width and 0 <= y < height (the width and height are the laid-out [size]). A component not in a window or
     * before layout has size 0, so it is always false. The shape is treated as a rectangle (including rounded corners and transparent parts).
     */
    fun contains(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < actualWidth && y < actualHeight

    /** Component.contains(Point)-like: the same as [contains] (x, y) (a [WMouseEvent.point] can be passed as is). */
    fun contains(point: WPoint): Boolean = contains(point.x, point.y)

    /**
     * Component.getComponentAt(int, int)-like: the component at the position ([x], [y], DIP) seen from the top left of this component.
     * For a component without children, itself if [contains], otherwise null. A [WContainer] returns a child ([WContainer.getComponentAt]).
     */
    open fun getComponentAt(x: Int, y: Int): WComponent? = if (contains(x, y)) this else null

    /** Component.getComponentAt(Point)-like: the same as [getComponentAt] (x, y). */
    fun getComponentAt(point: WPoint): WComponent? = getComponentAt(point.x, point.y)

    /**
     * Like Component.setBounds: sets the position relative to the parent ([x], [y]) and the size ([width] x [height]) in DIP ([setLocation] and [setSize]).
     * From Java, ints can be passed too, as in Swing.
     *
     * Inside parents corresponding to Swing's null layout ([WCanvas], and a [WLayoutPanel] whose [WLayoutPanel.layout] is null) they become the
     * position and size as is. Inside a [WLayoutPanel] with a layout manager they take effect immediately but, as in Swing, are overwritten
     * by the manager's values on the next layout. Inside other panels (parents that arrange their children themselves, such as [WPanel]) only the size takes effect and the position is ignored
     * (as Swing ignores the position under a layout manager).
     */
    fun setBounds(x: Double, y: Double, width: Double, height: Double) {
        setLocation(x, y)
        setSize(width, height)
    }

    /** Like Component.setBounds(Rectangle): sets the position and size of [bounds]. See [setBounds] for details. */
    fun setBounds(bounds: WRectangle) {
        setBounds(bounds.x.toDouble(), bounds.y.toDouble(), bounds.width.toDouble(), bounds.height.toDouble())
    }

    /**
     * Like Component.setLocation: sets the position relative to the parent in DIP (the attached properties Canvas.Left / Canvas.Top).
     * It takes effect only when the parent is a [WCanvas] or a [WLayoutPanel] and is ignored under other parents. The value is remembered, so
     * moving it into a [WCanvas] later puts it at that position (same as Swing).
     */
    fun setLocation(x: Double, y: Double) {
        Xaml.canvasStatics.call(XamlInterop.ICanvasStatics_SetLeft, uiElement.ptr, x)
        Xaml.canvasStatics.call(XamlInterop.ICanvasStatics_SetTop, uiElement.ptr, y)
    }

    /** Like Component.setLocation(Point): places it at [location] (DIP). See [setLocation] for details. */
    fun setLocation(location: WPoint) {
        setLocation(location.x.toDouble(), location.y.toDouble())
    }

    /**
     * Like Component.setSize: sets the size in DIP (FrameworkElement.Width / Height). Negative values become 0 (in Swing nothing is drawn).
     * NaN removes the size in that dimension, and WinUI decides it from the content.
     *
     * As in Swing, the explicit size ([width] / [height], corresponding to JComponent.setPreferredSize) is not changed. It takes effect under any parent, but
     * inside a [WLayoutPanel] with a layout manager it is overwritten on the next layout. Under parents other than [WLayoutPanel],
     * WinUI measures with the placed size, so [preferredSize] read later is also this size (Swing's getPreferredSize does not change).
     */
    fun setSize(width: Double, height: Double) {
        setLayoutSize(width.coerceAtLeast(0.0), height.coerceAtLeast(0.0))
    }

    /** Like Component.setSize(Dimension): sets it to [size] (DIP). See [setSize] for details. */
    fun setSize(size: WSize) {
        setSize(size.width, size.height)
    }

    /**
     * Swing's getLocationOnScreen-style: the on-screen position of this element's top-left (physical pixels; with multiple
     * monitors, virtual desktop coordinates). DIP is converted with the window's scale (XamlRoot.RasterizationScale). As
     * with [x], movement by RenderTransform or scrolling is not included.
     *
     * @throws IllegalStateException for an element not placed in a window (corresponds to Swing's IllegalComponentStateException)
     */
    val locationOnScreen: WPoint
        get() {
            val position = checkNotNull(Xaml.positionOnScreen(this)) { "cannot determine the on-screen position because the component is not placed in a window" }
            return WPoint(position[0], position[1])
        }

    /**
     * JComponent.scrollRectToVisible-like: scrolls the ancestor scrolling controls ([WScrollPane] etc.) so that [rect] in this element
     * (DIP from the top-left of this element) is visible (passes TargetRect to UIElement.StartBringIntoView, without animation).
     * Every ancestor moves, so nested scroll panes each move too. Scrolling happens at the next layout, so
     * the position may not have changed yet when the call returns (Swing moves it inside the call). Does nothing when not in a window.
     */
    fun scrollRectToVisible(rect: WRectangle) {
        ComponentSupport.scrollRectToVisible(uiElement, rect)
    }

    /**
     * Sets the size a layout manager assigns. Does not change [width] / [height] (the record of the
     * user's intent). Called from [WLayoutPanel.setBounds].
     */
    internal fun setLayoutSize(width: Double, height: Double) {
        applyWidth(width)
        applyLayoutHeight(height)
    }

    /** Writes the height the layout manager assigned in [setLayoutSize]. The default is put_Height. */
    internal open fun applyLayoutHeight(height: Double) {
        applyHeight(height)
    }

    /** Whether the native element is a Canvas (it is not a XAML layout element, so changing its size inside a Canvas needs [remeasureInCanvas]). */
    internal open val isCanvasElement: Boolean get() = false

    /**
     * Makes this Canvas, a child of a Canvas ([WLayoutPanel] etc.), be measured again with the current Width / Height.
     *
     * A Canvas is not a layout element, so when the parent is also a Canvas, changing Width / Height does not make XAML measure again; it only sets
     * a "measure pending" flag (CUIElement::InvalidateMeasure in microsoft-ui-xaml uielement.cpp, GetIsLayoutElement in canvas.h). That flag
     * turns into "needs measure" when InvalidateMeasure of a descendant layout element propagates to the ancestors (PropagateOnPathInternal in
     * UIElementLayout.cpp). So InvalidateMeasure is called on the descendant layout elements (children that are not Canvases; their descendants if a child is a Canvas).
     * A Canvas without descendant layout elements (an empty one) is not measured again.
     */
    internal fun remeasureInCanvas() {
        uiElement.call(XamlInterop.IUIElement_InvalidateMeasure)
        (this as? WContainer)?.componentList?.forEach { child ->
            if (child.isCanvasElement) child.remeasureInCanvas() else child.uiElement.call(XamlInterop.IUIElement_InvalidateMeasure)
        }
    }

    /**
     * A cache of the natural size (the preferred size measured from the content). null means it
     * hasn't been measured yet, or [invalidateNaturalSize] was called since — the parent
     * [WLayoutPanel]'s next layout pass will re-measure it.
     */
    internal var naturalSize: WSize? = null

    /** Performs operations requested before the first layout after laying out ([LaidOutRequests]). Each kind of request uses its own key. */
    internal val laidOutRequests: LaidOutRequests by lazy { LaidOutRequests(this) }

    /**
     * Clears the explicit size on whichever dimension the user hasn't explicitly set, in
     * preparation for re-measuring the natural size (in XAML, DesiredSize keeps tracking an
     * explicitly-set Width even after it's set).
     */
    internal open fun clearLayoutSizeForMeasure() {
        if (width.isNaN()) applyWidth(Double.NaN)
        if (height.isNaN()) applyHeight(Double.NaN)
    }

    /** The preferred size computed by the most recent Measure pass (UIElement.DesiredSize). */
    internal open fun readDesiredSize(): WSize {
        val size = XamlStructs.getSizeFloat(uiElement, XamlInterop.IUIElement_get_DesiredSize)
        return WSize(size[0], size[1])
    }

    /**
     * The preferred size (equivalent to JComponent.getPreferredSize). Prefers [width] / [height]
     * wherever they've been explicitly set, and fills in the remaining dimension from the natural
     * size (measured and cached by the parent [WLayoutPanel]'s layout pass).
     */
    open fun preferredSize(): WSize {
        val natural = naturalSize ?: readDesiredSize()
        return WSize(
            if (width.isNaN()) natural.width else width,
            if (height.isNaN()) natural.height else height,
        )
    }

    /**
     * A helper for [preferredSize]: keeps the dimensions of [size] that are not given explicitly ([width] / [height] is NaN) from being smaller than [minWidth] / [minHeight] (DIP).
     * Some WinUI controls (Slider, ProgressBar, etc.) ask only for the minimum size of their content (or 0) when no size is given,
     * so the default preferred size decided by Swing's UI (200 for JSlider, etc.) is supplied here.
     */
    internal fun withDefaultExtent(size: WSize, minWidth: Double, minHeight: Double): WSize = WSize(
        if (width.isNaN()) maxOf(size.width, minWidth) else size.width,
        if (height.isNaN()) maxOf(size.height, minHeight) else size.height,
    )

    /**
     * The preferred size (equivalent to JComponent.get/setPreferredSize). Reading is the same as the [preferredSize]
     * function.
     * Setting it writes [width] / [height] as an explicit size (the actual size after layout also becomes this value).
     */
    var preferredSize: WSize
        get() = preferredSize()
        set(value) {
            width = value.width
            height = value.height
        }

    /**
     * The minimum size (equivalent to JComponent.get/setMinimumSize; FrameworkElement.MinWidth / MinHeight).
     * At least this size is reserved even if the content is smaller. The default is (0, 0).
     */
    open var minimumSize: WSize
        get() = WSize(
            frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MinWidth),
            frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MinHeight),
        )
        set(value) {
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MinWidth, value.width)
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MinHeight, value.height)
            isMinimumSizeSet = true
        }

    /**
     * Component.isPreferredSizeSet-like: whether the preferred size has been decided. Besides [preferredSize], setting only one of [width] / [height]
     * also makes it true (WinUI4K holds the preferred width and height separately and measures only the undecided dimension from the content; Swing's
     * Dimension decides both at once). Resetting both the width and the height to NaN makes it false (corresponding to Swing's setPreferredSize(null)).
     * The set values ([width] / [height]) are the ones remembered on the Kotlin side; direct changes to the native Width / Height are not seen.
     */
    val isPreferredSizeSet: Boolean
        get() = !width.isNaN() || !height.isNaN()

    /**
     * Component.isMinimumSizeSet-like: whether [minimumSize] has been set. As in Swing, once set it stays true regardless of the value (even (0, 0), the same as the default)
     * (there is no way to unset it, corresponding to Swing's setMinimumSize(null)). Whether it was set is remembered on the Kotlin side.
     */
    var isMinimumSizeSet: Boolean = false
        private set

    /**
     * The maximum size (equivalent to JComponent.get/setMaximumSize; FrameworkElement.MaxWidth / MaxHeight).
     * The size is limited to this even if the content is larger. The default is unlimited (Double.POSITIVE_INFINITY).
     */
    open var maximumSize: WSize
        get() = WSize(
            frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MaxWidth),
            frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MaxHeight),
        )
        set(value) {
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MaxWidth, value.width)
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MaxHeight, value.height)
            isMaximumSizeSet = true
        }

    /**
     * Component.isMaximumSizeSet-like: whether [maximumSize] or [maxWidth] has been set ([maxWidth] decides only the width of the maximum size,
     * so it alone also makes it true). Otherwise the same as [isMinimumSizeSet].
     */
    var isMaximumSizeSet: Boolean = false
        private set

    /** The value set to [alignmentX] (null if not set, in which case [defaultAlignmentX] is returned). */
    private var alignmentXValue: Float? = null

    /** The value set to [alignmentY] (null if not set, in which case [defaultAlignmentY] is returned). */
    private var alignmentYValue: Float? = null

    /**
     * Like JComponent.getAlignmentX / setAlignmentX: the horizontal alignment (0 = left [LEFT_ALIGNMENT], 0.5 = center [CENTER_ALIGNMENT],
     * 1 = right [RIGHT_ALIGNMENT]). Values below 0 become 0 and values above 1 become 1 (same as Swing).
     *
     * A value kept on the Kotlin side, separate from the native [horizontalAlignment] (WinUI's panels do not look at it). It is used by
     * layout managers such as [WBoxLayout]. The default, as in Swing, is 0 for labels ([WLabel]) and buttons (subclasses of [WButtonBase]),
     * the manager's value ([WLayoutManager.getLayoutAlignmentX]) for a [WLayoutPanel] with a layout manager, and 0.5 otherwise.
     * As in Swing, changing it does not rearrange (call [revalidate]).
     */
    var alignmentX: Float
        get() = alignmentXValue ?: defaultAlignmentX
        set(value) {
            alignmentXValue = value.coerceIn(0.0f, 1.0f)
        }

    /**
     * Like JComponent.getAlignmentY / setAlignmentY: the vertical alignment (0 = top [TOP_ALIGNMENT], 0.5 = center, 1 = bottom [BOTTOM_ALIGNMENT]).
     * The default is 0.5 (the manager's value for a [WLayoutPanel]). Otherwise the same as [alignmentX].
     */
    var alignmentY: Float
        get() = alignmentYValue ?: defaultAlignmentY
        set(value) {
            alignmentYValue = value.coerceIn(0.0f, 1.0f)
        }

    /** The value of [alignmentX] when it is not set. */
    internal open val defaultAlignmentX: Float get() = CENTER_ALIGNMENT

    /** The value of [alignmentY] when it is not set. */
    internal open val defaultAlignmentY: Float get() = CENTER_ALIGNMENT

    /**
     * Whether [WBoxLayout] keeps this component from growing beyond its preferred size when [maximumSize] is not set.
     * Swing's JLabel and AbstractButton have maximum size = preferred size, so the corresponding [WLabel] and [WButtonBase] are true.
     */
    internal open val maximumFollowsPreferred: Boolean get() = false

    /** The values put with [putClientProperty] (not created until one is put). */
    private var clientProperties: MutableMap<Any, Any>? = null

    /**
     * JComponent.putClientProperty-style: associates an arbitrary value with an arbitrary key (a Kotlin-side map that does
     * not change the native state). Used by apps to attach their own markers or data to a component. Keys are compared with
     * equals / hashCode. If [value] is null, the value for that key is removed. Unlike Swing, value changes are not reported
     * with PropertyChangeEvent.
     */
    fun putClientProperty(key: Any, value: Any?) {
        if (value == null) {
            clientProperties?.remove(key)
            return
        }
        val properties = clientProperties ?: HashMap<Any, Any>().also { clientProperties = it }
        properties[key] = value
    }

    /** JComponent.getClientProperty-style: the value put with [putClientProperty]. null for keys not put or removed. */
    fun getClientProperty(key: Any): Any? = clientProperties?.get(key)

    /**
     * Equivalent to Component.getName / setName (FrameworkElement.Name). An empty string if unset.
     * Used as the lookup key in the XAML name scope.
     */
    var name: String
        get() = frameworkElement.getString(XamlInterop.IFrameworkElement_get_Name)
        set(value) = Hstring.use(value) { h -> frameworkElement.call(XamlInterop.IFrameworkElement_put_Name, h) }

    /**
     * The element on which UI Automation attached properties ([accessibleName] etc.) are set. The default is this element
     * itself; [WLabel], whose inner element is the target read aloud (the TextBlock rather than the root Grid), overrides it.
     */
    internal open val automationTarget: ComPtr get() = inspectable

    /**
     * The name read by screen readers (AutomationProperties.Name; equivalent to AccessibleContext.getAccessibleName).
     * An empty string if unset; setting an empty string clears it.
     */
    var accessibleName: String
        get() = Xaml.getAutomation(automationTarget, XamlInterop.IAutomationPropertiesStatics_GetName)
        set(value) = Xaml.setAutomation(automationTarget, XamlInterop.IAutomationPropertiesStatics_SetName, value)

    /**
     * Supplementary description for screen readers (AutomationProperties.HelpText;
     * equivalent to AccessibleContext.getAccessibleDescription). An empty string if unset.
     */
    var accessibleDescription: String
        get() = Xaml.getAutomation(automationTarget, XamlInterop.IAutomationPropertiesStatics_GetHelpText)
        set(value) = Xaml.setAutomation(automationTarget, XamlInterop.IAutomationPropertiesStatics_SetHelpText, value)

    /** The value set to [isFocusable] (null = not set). */
    private var focusableSetting: Boolean? = null

    /**
     * Whether this is a control whose own IsTabStop is false, but where UIElement.Focus passes focus to a part inside the
     * template (such as the NumberBox input field) that can receive it. [WSpinner], [WAutoSuggestBox], [WDatePicker] and
     * [WTimePicker] set it to true.
     */
    internal open val focusIsDelegated: Boolean get() = false

    /**
     * Equivalent to JComponent.isFocusable: whether it can receive focus.
     * If not set, true if it is a Tab key destination (UIElement.IsTabStop), or a control whose template part receives
     * focus ([WSpinner] etc.; its own IsTabStop is false, but [requestFocusInWindow] succeeds).
     * Setting it to false sets IsTabStop to false and it does not receive focus even with [requestFocus] /
     * [requestFocusInWindow] (WinUI's IsTabStop only decides Tab key destinations and does not stop programmatic focus, so
     * the set value is remembered and used to stop it).
     * Setting it to true makes it able to receive focus (IsTabStop of a control whose part receives focus stays false).
     */
    var isFocusable: Boolean
        get() = focusableSetting ?: (focusIsDelegated || uiElement.getBool(XamlInterop.IUIElement_get_IsTabStop))
        set(value) {
            focusableSetting = value
            if (!(value && focusIsDelegated)) uiElement.putBool(XamlInterop.IUIElement_put_IsTabStop, value)
        }

    /** Moves focus (UIElement.Focus). true if accepted. false for an element that is not on a window. */
    internal fun focus(state: Int = XamlInterop.FocusState_Keyboard): Boolean = Ffi.backend.withScope { scope ->
        val out = scope.allocate(1, 1)
        uiElement.call(XamlInterop.IUIElement_Focus, state, out)
        Ffi.backend.memory.getByte(out, 0) != 0.toByte()
    }

    /**
     * Puts the window's initial focus (put when the [WFrame] becomes active) on this component without showing the keyboard focus rectangle
     * ([focusWithoutKeyboardCue]). Not put on a component with [isFocusable] false. true if it succeeds.
     */
    internal fun focusInitially(): Boolean = focusableSetting != false && focusWithoutKeyboardCue(uiElement)

    /**
     * When the window's initial focus is decided to be this component, puts it, as in Swing, on the right place in the component (the selected button
     * for a radio button group, the selected row for a tree) without showing the keyboard focus rectangle. true if it succeeds. If false,
     * it goes to the element WinUI considers the first that can take focus.
     */
    internal open fun focusAsInitial(): Boolean = false

    /** Requests focus (equivalent to Component.requestFocus). Use when the result is not needed. */
    fun requestFocus() {
        requestFocusInWindow()
    }

    /**
     * Requests focus and returns whether it was accepted (equivalent to Component.requestFocusInWindow; the result of
     * UIElement.Focus). false for elements not placed in a window, disabled or unable to receive focus, and for elements
     * whose [isFocusable] is set to false. IsTabStop is not checked, so it is true for [WSpinner] and the like, whose own
     * IsTabStop is false but whose template part receives focus.
     * For an element in a [WFrame] that was just shown and whose content is not loaded yet, the request is remembered and true is returned as in Swing,
     * and focus moves when it loads and becomes active (the initial focus can be chosen right after `frame.isVisible = true`).
     */
    fun requestFocusInWindow(): Boolean =
        focusableSetting != false && (focus(XamlInterop.FocusState_Programmatic) || WFrame.deferFocus(this))

    /**
     * Whether this element has focus (equivalent to Component.hasFocus). true when FocusState is other than Unfocused, or
     * when a template part has focus (such as the input field of [WSpinner]).
     */
    fun hasFocus(): Boolean = uiElement.getInt(XamlInterop.IUIElement_get_FocusState) != 0 || ownsFocus(this)

    /** Component.isFocusOwner-like: the same as [hasFocus] (as in Swing). */
    val isFocusOwner: Boolean
        get() = hasFocus()

    /** JComponent.grabFocus-like: the same as [requestFocus]. As in Swing, it moves the focus even if [isRequestFocusEnabled] is false. */
    fun grabFocus() {
        requestFocus()
    }

    /**
     * Component.transferFocus-like: moves the focus to the component after this one, in the same order as the Tab key
     * (Next of FocusManager.TryMoveFocus). The order is WinUI's Tab order (tree order and TabIndex); Swing's
     * FocusTraversalPolicy cannot be used.
     *
     * If this component does not have the focus, the focus is first moved to this component and then to the next (the corresponding
     * focusGained / focusLost also arrive; Swing does not move it). For a component that cannot receive the focus ([isFocusable] is false,
     * [WLabel] etc.), it moves to the element after the one that currently has the focus (Swing moves to the one after this component).
     * Does nothing when not placed in a window ([isDisplayable] is false).
     */
    fun transferFocus() {
        FocusTraversal.transfer(this, next = true)
    }

    /** Component.transferFocusBackward-like: moves the focus to the previous component in the same order as Shift+Tab. Otherwise the same as [transferFocus]. */
    fun transferFocusBackward() {
        FocusTraversal.transfer(this, next = false)
    }

    /**
     * JComponent.isRequestFocusEnabled / setRequestFocusEnabled-like: whether it takes the focus when pressed or touched with the mouse
     * (FrameworkElement.AllowFocusOnInteraction). The default is true. As in Swing, false does not stop moving with the Tab key or
     * the focus from [requestFocus] / [requestFocusInWindow] / [grabFocus] (used so that toolbar buttons do not steal the focus from
     * input fields).
     */
    var isRequestFocusEnabled: Boolean
        get() = frameworkElement.getBool(XamlInterop.IFrameworkElement_get_AllowFocusOnInteraction)
        set(value) = frameworkElement.putBool(XamlInterop.IFrameworkElement_put_AllowFocusOnInteraction, value)

    /** The list of input listeners and the native handlers (created when the first listener is added). */
    private var inputOrNull: ComponentInput? = null

    internal val input: ComponentInput
        get() = inputOrNull ?: ComponentInput(this).also { inputOrNull = it }

    /**
     * Component.addFocusListener-style: adds a listener called when this component gains or loses focus
     * (UIElement.GotFocus / LostFocus). Does nothing if null. Adding the same listener twice makes it be called twice
     * (same as Swing).
     *
     * As in Swing, it is not called for the focus of child components (such as a button inside a panel). For controls
     * where a part inside the template (the input field) receives focus, such as [WSpinner] which uses NumberBox, the
     * focus of that part is reported as this component's, and moving between the parts does not call it. The same goes for
     * parts shown in a Popup inside the template: opening the [WComboBox] dropdown and moving focus to an item, or closing
     * it and coming back, does not call it (just as JComboBox keeps focus while its popup is open).
     * However, for a Flyout (the picker of [WDatePicker] / [WTimePicker], or [WFlyout]) WinUI cannot tell which component
     * it belongs to, so when it takes focus focusLost is called, and when it closes and focus returns focusGained is called
     * (unlike Swing; doc/verification.md).
     */
    fun addFocusListener(listener: WFocusListener?) {
        if (listener != null) input.addFocusListener(listener)
    }

    /** Component.removeFocusListener-style: removes one listener added with [addFocusListener]. Does nothing if not added. */
    fun removeFocusListener(listener: WFocusListener?) {
        if (listener != null) inputOrNull?.removeFocusListener(listener)
    }

    /** Component.getFocusListeners-style: the focus listeners added (in the order they were added). */
    val focusListeners: Array<WFocusListener>
        get() = inputOrNull?.focusListeners() ?: emptyArray()

    /**
     * Component.addMouseListener-style: adds a listener called when a button is pressed, released or clicked over this
     * component, or when the mouse enters or exits it (UIElement.PointerPressed / PointerReleased / PointerEntered /
     * PointerExited). Does nothing if null.
     *
     * As in Swing, the component that was pressed receives the release and drags (it captures the pointer), and if both a
     * child and a parent have mouse listeners, only the child receives them. Presses that a child control handled itself
     * (such as pressing a button inside a panel) are not reported to the parent.
     * Listeners added to a button or input field itself also receive presses that the control handled (because the
     * control's handling runs first, [WInputEvent.consume] cannot stop clicks and the like). Touch and pen input also
     * arrive as the left mouse button.
     */
    fun addMouseListener(listener: WMouseListener?) {
        if (listener == null) return
        input.mouse.addMouseListener(listener)
        mouseListenerAdded()
    }

    /** Called after a mouse listener (press, move or wheel) is added ([WContainer] puts a brush for hit testing). */
    internal open fun mouseListenerAdded() {
        // Does nothing by default
    }

    /** Component.removeMouseListener-style: removes one listener added with [addMouseListener]. Does nothing if not added. */
    fun removeMouseListener(listener: WMouseListener?) {
        if (listener != null) inputOrNull?.mouse?.removeMouseListener(listener)
    }

    /** Component.getMouseListeners-style: the mouse listeners added (in the order they were added). */
    val mouseListeners: Array<WMouseListener>
        get() = inputOrNull?.mouse?.mouseListeners() ?: emptyArray()

    /**
     * Component.addMouseMotionListener-style: adds a listener called when the mouse is moved over this component, or moved
     * while pressed (dragged) (UIElement.PointerMoved). Does nothing if null. Drags are delivered to the pressed component
     * even after leaving the component.
     */
    fun addMouseMotionListener(listener: WMouseMotionListener?) {
        if (listener == null) return
        input.mouse.addMouseMotionListener(listener)
        mouseListenerAdded()
    }

    /** Component.removeMouseMotionListener-style: removes one listener added with [addMouseMotionListener]. */
    fun removeMouseMotionListener(listener: WMouseMotionListener?) {
        if (listener != null) inputOrNull?.mouse?.removeMouseMotionListener(listener)
    }

    /** Component.getMouseMotionListeners-style: the mouse motion listeners added (in the order they were added). */
    val mouseMotionListeners: Array<WMouseMotionListener>
        get() = inputOrNull?.mouse?.mouseMotionListeners() ?: emptyArray()

    /**
     * Component.addMouseWheelListener-style: adds a listener called when the wheel is rotated over this component
     * (UIElement.PointerWheelChanged). Does nothing if null. If both a child and a parent have wheel listeners, only the
     * child receives them. Calling [WInputEvent.consume] stops it from propagating to a parent ScrollViewer and the like.
     */
    fun addMouseWheelListener(listener: WMouseWheelListener?) {
        if (listener == null) return
        input.mouse.addMouseWheelListener(listener)
        mouseListenerAdded()
    }

    /** Component.removeMouseWheelListener-style: removes one listener added with [addMouseWheelListener]. */
    fun removeMouseWheelListener(listener: WMouseWheelListener?) {
        if (listener != null) inputOrNull?.mouse?.removeMouseWheelListener(listener)
    }

    /** Component.getMouseWheelListeners-style: the wheel listeners added (in the order they were added). */
    val mouseWheelListeners: Array<WMouseWheelListener>
        get() = inputOrNull?.mouse?.mouseWheelListeners() ?: emptyArray()

    /**
     * Component.addKeyListener-style: adds a listener called for key operations while this component has focus
     * (UIElement.PreviewKeyDown / PreviewKeyUp / CharacterReceived). Does nothing if null.
     * As in Swing, it is not called while a child component has focus (as with [addFocusListener], for controls where a
     * part inside the template receives focus, such as [WSpinner], it is called for that part's keys).
     *
     * keyPressed is called before the control's own key handling, such as Enter on a button, and [WInputEvent.consume]
     * can stop that handling and keyboard accelerators (the default button and so on). Because WinUI may not send the
     * character (CharacterReceived) of a press that became handled (Handled), keyTyped may not arrive for consumed presses
     * or for Enter / Space presses that a button handles itself (in Swing it does). Consuming keyTyped does not stop the
     * character from being inserted into an input field (TextBox inserts the character before the handler).
     */
    fun addKeyListener(listener: WKeyListener?) {
        if (listener != null) input.keys.addKeyListener(listener)
    }

    /** Component.removeKeyListener-style: removes one listener added with [addKeyListener]. Does nothing if not added. */
    fun removeKeyListener(listener: WKeyListener?) {
        if (listener != null) inputOrNull?.keys?.removeKeyListener(listener)
    }

    /** Component.getKeyListeners-style: the key listeners added (in the order they were added). */
    val keyListeners: Array<WKeyListener>
        get() = inputOrNull?.keys?.keyListeners() ?: emptyArray()

    /** The key binding mappings and native handlers (created when the first map is created). */
    internal var keyBindingsOrNull: KeyBindings? = null
        private set

    private val keyBindings: KeyBindings
        get() = keyBindingsOrNull ?: KeyBindings(this).also { keyBindingsOrNull = it }

    /**
     * JComponent.getInputMap()-like: the [WInputMap] for [WHEN_FOCUSED] (created when first read; always the same instance).
     * Used together with [actionMap] to run actions by keystroke. See [getInputMap] (Int) for the differences from Swing.
     */
    val inputMap: WInputMap
        get() = getInputMap(WHEN_FOCUSED)

    /**
     * JComponent.getInputMap(int)-like: the [WInputMap] for [condition] ([WHEN_FOCUSED] / [WHEN_ANCESTOR_OF_FOCUSED_COMPONENT] / [WHEN_IN_FOCUSED_WINDOW])
     * (created when first read). When a key is pressed, released or a character is typed, as in Swing, keys not consumed by the key listeners are looked up
     * in [WHEN_FOCUSED] of the focused component, then in
     * [WHEN_ANCESTOR_OF_FOCUSED_COMPONENT] of each component from the focus up through its parents, then in [WHEN_IN_FOCUSED_WINDOW] of components on screen ([isShowing]) in the same window,
     * and if the [actionMap] action is enabled it is called and the key is consumed (if disabled, the key is not consumed and the next is looked up).
     * The source of the action's [WActionEvent] is this component, and actionCommand is the action's [WAction.ACTION_COMMAND_KEY],
     * or if absent the character of a character-input keystroke (null otherwise). Not looked up in a disabled [WControl].
     *
     * Differences from Swing:
     * - The controls' default key operations (a button's Space, a text field's arrows, etc.) are not in the maps (the maps start empty)
     * - [WHEN_FOCUSED] is looked up before the control's own key handling, but [WHEN_ANCESTOR_OF_FOCUSED_COMPONENT] and
     *   [WHEN_IN_FOCUSED_WINDOW] are looked up after it, so they do not work for keys a child control handles itself (a button's Space, a text field's arrows,
     *   Ctrl+C, etc.)
     * - Character-input keystrokes in a text field ("typed a") run after the character is inserted (the insertion cannot be stopped)
     * - [WHEN_IN_FOCUSED_WINDOW] is looked up only when the focus is inside the window content (not inside flyouts or dialogs).
     *   When several components have the same keystroke, which one runs is undefined (the same as Swing)
     * - Swing's KEY_PRESSED events carry the key's character, which can become the actionCommand, but here they do not
     * - There is no setInputMap / setActionMap / getActionForKeyStroke (share maps through [WInputMap.parent])
     *
     * @throws IllegalArgumentException if [condition] is none of the three (the same as Swing)
     */
    fun getInputMap(condition: Int): WInputMap = checkNotNull(keyBindings.inputMap(condition, true))

    /** JComponent.getActionMap-like: the [WActionMap] that looks up actions from the keys of [getInputMap] (created when first read). */
    val actionMap: WActionMap
        get() = checkNotNull(keyBindings.actionMap(true))

    /**
     * JComponent.registerKeyboardAction-like: maps [keyStroke] to [listener] in the [WInputMap] for [condition]
     * (puts an action wrapping [listener] into [actionMap]). The actionCommand when it runs is [command].
     * If [listener] is null, that keystroke runs nothing and does not let the parent map's mapping be looked up either (the same as Swing).
     *
     * @throws IllegalArgumentException if [condition] is none of the three
     */
    fun registerKeyboardAction(listener: WActionListener?, command: String?, keyStroke: WKeyStroke?, condition: Int) =
        keyBindings.register(listener, command, keyStroke, condition)

    /** JComponent.registerKeyboardAction(ActionListener, KeyStroke, int)-like: [registerKeyboardAction] with a null actionCommand. */
    fun registerKeyboardAction(listener: WActionListener?, keyStroke: WKeyStroke?, condition: Int) =
        keyBindings.register(listener, null, keyStroke, condition)

    /** JComponent.unregisterKeyboardAction-like: removes the mapping of [keyStroke] from the maps of the three conditions and the action it points to. */
    fun unregisterKeyboardAction(keyStroke: WKeyStroke?) {
        keyBindingsOrNull?.unregister(keyStroke)
    }

    /** JComponent.getRegisteredKeyStrokes-like: the keystrokes of the maps of the three conditions (including parents) (in condition order from [WHEN_FOCUSED]). */
    val registeredKeyStrokes: Array<WKeyStroke>
        get() = keyBindingsOrNull?.registeredKeyStrokes() ?: emptyArray()

    /** JComponent.getConditionForKeyStroke-like: the first condition that maps [keyStroke]. [UNDEFINED_CONDITION] if none. */
    fun getConditionForKeyStroke(keyStroke: WKeyStroke?): Int = keyBindingsOrNull?.conditionFor(keyStroke) ?: UNDEFINED_CONDITION

    /** JComponent.resetKeyboardActions-like: empties the maps of the three conditions and [actionMap] (the maps' parents are not removed). */
    fun resetKeyboardActions() {
        keyBindingsOrNull?.reset()
    }

    /** The size, position and visibility listeners (created when the first listener is added). */
    internal var componentEventsOrNull: ComponentEvents? = null
        private set

    /**
     * Component.addComponentListener-style: adds a listener called when this component's size, position or visibility changes.
     * Does nothing if null. Adding the same listener twice makes it called twice (same as Swing).
     *
     * - componentResized: after the size changed in a layout (FrameworkElement.SizeChanged; also arrives on the first layout)
     * - componentMoved: after the position relative to the parent (UIElement.ActualOffset) changed in a layout. Does not arrive when only the parent moved
     * - componentShown / componentHidden: after this component's own [isVisible] changed. As in Swing, hiding a parent does not trigger them,
     *   and as with Swing posting them to the event queue, they are called after returning from the call that changed [isVisible]
     *
     * While listeners are added, the position is read every time a layout runs anywhere in the tree (FrameworkElement.LayoutUpdated).
     * componentResized and componentMoved are called in the middle of XAML's layout, so a dialog that waits until it is closed cannot be opened from inside them
     * ([IllegalStateException]; open it later with WinUiUtilities.invokeLater).
     */
    fun addComponentListener(listener: WComponentListener?) {
        if (listener == null) return
        val events = componentEventsOrNull ?: ComponentEvents(this).also { componentEventsOrNull = it }
        events.add(listener)
    }

    /** Component.removeComponentListener-style: removes one listener added with [addComponentListener]. Does nothing if it was not added. */
    fun removeComponentListener(listener: WComponentListener?) {
        if (listener != null) componentEventsOrNull?.remove(listener)
    }

    /** Component.getComponentListeners-style: the size, position and visibility listeners that are added (in the order they were added). */
    val componentListeners: Array<WComponentListener>
        get() = componentEventsOrNull?.listeners() ?: emptyArray()

    /** The ancestor listeners (created when the first listener is added). */
    internal var ancestorEventsOrNull: AncestorEvents? = null
        private set

    /**
     * JComponent.addAncestorListener-like: adds a listener called when this component appears on screen, disappears from it, or moves within the window.
     * Does nothing if null. Adding the same listener twice makes it called twice (the same as Swing).
     *
     * - ancestorAdded / ancestorRemoved: when placed in a window and loaded by XAML (FrameworkElement.Loaded) or removed (Unloaded),
     *   or when [isVisible] of it or an ancestor changes and whether it and all its ancestors are visible changes. As in Swing,
     *   it does not come when the window ([WFrame]) is merely hidden. When removed and re-added to another parent, ancestorRemoved comes and then ancestorAdded
     * - ancestorMoved: when the position seen from the top left of the window content changes while on screen because it or an ancestor moved (compared after
     *   layout). Unlike Swing, it does not come when the window is merely moved on the screen. Also, [setLocation] on a child of a [WCanvas] is moved by WinUI
     *   without a layout, so its descendants get it the next time a layout runs somewhere
     *
     * All of them are called on the next turn of the UI thread (as Swing's AncestorNotifier calls after receiving an event, they are not called
     * within the call that made the change). The event's [WAncestorEvent.ancestor] is always this component itself, because WinUI does not tell
     * which ancestor changed. There is no equivalent of HierarchyListener. While listeners are attached, visibility and position are checked
     * every time a layout runs anywhere in the tree (FrameworkElement.LayoutUpdated).
     */
    fun addAncestorListener(listener: WAncestorListener?) {
        if (listener == null) return
        val events = ancestorEventsOrNull ?: AncestorEvents(this).also { ancestorEventsOrNull = it }
        events.add(listener)
    }

    /** JComponent.removeAncestorListener-like: removes one listener added with [addAncestorListener]. Does nothing if it is not attached. */
    fun removeAncestorListener(listener: WAncestorListener?) {
        if (listener != null) ancestorEventsOrNull?.remove(listener)
    }

    /** JComponent.getAncestorListeners-like: the attached ancestor listeners (in the order added). */
    val ancestorListeners: Array<WAncestorListener>
        get() = ancestorEventsOrNull?.listeners() ?: emptyArray()

    /**
     * Invalidates the natural size cache. Call this alongside the parent [WLayoutPanel]'s revalidate whenever the
     * content's size changes, e.g. from a text change ([revalidate] does both).
     */
    fun invalidateNaturalSize() {
        naturalSize = null
    }

    /**
     * Component.invalidate-style: records that this component's size may change. Discards the natural size cache
     * ([invalidateNaturalSize], also for the ancestors reached through [parent]) and calls UIElement.InvalidateMeasure.
     * Does not go as far as scheduling a re-layout (that is [revalidate]).
     */
    open fun invalidate() {
        invalidateNaturalSize()
        uiElement.call(XamlInterop.IUIElement_InvalidateMeasure)
        var ancestor = parent
        while (ancestor != null) {
            ancestor.invalidateNaturalSize()
            ancestor = ancestor.parent
        }
    }

    /**
     * JComponent.revalidate-style: calls [invalidate] and then schedules a re-layout of every ancestor [WLayoutPanel] reached through [parent]
     * (multiple calls within the same UI thread turn are coalesced into one). Call it on the component when its natural size changes,
     * for example after changing its text. WinUI's own panels (WPanel, WGrid and so on) rearrange themselves without it.
     */
    open fun revalidate() {
        invalidate()
        var ancestor = parent
        while (ancestor != null) {
            if (ancestor is WLayoutPanel) ancestor.scheduleLayout()
            ancestor = ancestor.parent
        }
    }

    /**
     * Container.validate-style: runs the scheduled re-layouts of [WLayoutPanel]s now (following the [WContainer] children below)
     * and completes the layout synchronously ([updateLayout]).
     */
    open fun validate() {
        validateTree()
        updateLayout()
    }

    /** The re-layout part of [validate]. Containers with children follow it down to the children. */
    internal open fun validateTree() {
        // Components without children do nothing
    }

    /**
     * Component.repaint-style: does nothing. XAML redraws itself when the displayed values change, so there is no need to request a redraw
     * (provided so that Swing code's `revalidate(); repaint();` works as is).
     */
    fun repaint() {
        // XAML redraws itself, so do nothing
    }

    /**
     * Upper bound on the width (FrameworkElement.MaxWidth). Unlike [width], this is measured against the
     * content's natural width and only clamps it once it exceeds this value (unlimited by default).
     */
    var maxWidth: Double
        get() = frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MaxWidth)
        set(value) {
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MaxWidth, value)
            isMaximumSizeSet = true
        }

    /**
     * This component's own horizontal position within the width its parent assigns (FrameworkElement.HorizontalAlignment).
     *
     * **Difference from Swing**: the name is the same as setHorizontalAlignment of JLabel / JTextField / AbstractButton, but the meaning differs.
     * Swing's aligns the text or content inside the component, while this decides where the component itself is placed in its parent (for a component with a fixed width
     * it often changes nothing). For text alignment use [WLabel.textAlignment] / [WTextField.textAlignment], and for the content alignment of buttons etc.
     * use [WControl.horizontalContentAlignment].
     */
    var horizontalAlignment: HorizontalAlignment
        get() = HorizontalAlignment.of(frameworkElement.getInt(XamlInterop.IFrameworkElement_get_HorizontalAlignment))
        set(value) = frameworkElement.call(XamlInterop.IFrameworkElement_put_HorizontalAlignment, value.native)

    /** The direction of text and layout (FrameworkElement.FlowDirection). Inherited from the parent unless set. */
    var flowDirection: FlowDirection
        get() = FlowDirection.of(frameworkElement.getInt(XamlInterop.IFrameworkElement_get_FlowDirection))
        set(value) = frameworkElement.call(XamlInterop.IFrameworkElement_put_FlowDirection, value.native)

    /**
     * This component's own vertical position within the height its parent assigns (FrameworkElement.VerticalAlignment).
     *
     * **Difference from Swing**: the name is the same as setVerticalAlignment of JLabel / AbstractButton, but the meaning differs. Swing's aligns the text or
     * content inside the component vertically, while this decides where the component itself is placed in its parent. For the vertical content alignment of buttons etc.
     * use [WControl.verticalContentAlignment].
     */
    var verticalAlignment: VerticalAlignment
        get() = VerticalAlignment.of(frameworkElement.getInt(XamlInterop.IFrameworkElement_get_VerticalAlignment))
        set(value) = frameworkElement.call(XamlInterop.IFrameworkElement_put_VerticalAlignment, value.native)

    /**
     * A uniform margin on all four sides. Passes a Thickness (double×4) by value to put_Margin. Use [setMargin] to set each side individually.
     * Reading returns the value set through this library, or 0 if unset. When set with [setMargin], it is that value if all four sides are equal,
     * otherwise 0, since they cannot be expressed as one number.
     */
    var margin: Double
        get() = marginValue
        set(value) = setMargin(value, value, value, value)

    /** The last set margin returned by [margin] (0 if the four sides are not equal). */
    private var marginValue: Double = 0.0

    /** Sets the margin on each side individually (FrameworkElement.Margin). The value read from [margin] changes too. */
    fun setMargin(left: Double, top: Double, right: Double, bottom: Double) {
        XamlStructs.putThickness(frameworkElement, XamlInterop.IFrameworkElement_put_Margin, left, top, right, bottom)
        marginValue = uniformThickness(left, top, right, bottom)
    }

    /** Opacity, from 0.0 (transparent) to 1.0 (opaque, the default) (UIElement.Opacity). */
    var opacity: Double
        get() = uiElement.getDouble(XamlInterop.IUIElement_get_Opacity)
        set(value) = uiElement.call(XamlInterop.IUIElement_put_Opacity, value)

    /** Whether this is shown (UIElement.Visibility). false is Collapsed (doesn't reserve layout space either). */
    open var isVisible: Boolean
        get() = uiElement.getInt(XamlInterop.IUIElement_get_Visibility) == 0 // Visibility.Visible
        set(value) = uiElement.call(XamlInterop.IUIElement_put_Visibility, if (value) 0 else 1)

    /**
     * Component.isDisplayable-like: whether it is connected to the visual tree of a displayable window (it has a XamlRoot and following visual tree
     * parents reaches the same root as XamlRoot.Content). As in Swing, it is true right after being added to a container in a shown window, and
     * stays true inside a hidden window ([WFrame.isVisible] = false) and when it or a parent is hidden ([isVisible] = false),
     * and becomes false when removed from the window and when the window is closed ([WFrame.dispose]).
     * Unlike Swing, it is false inside a window that has never been shown (true after pack in Swing) and inside closed flyouts and popups.
     */
    val isDisplayable: Boolean
        get() = ComponentShowing.isConnected(this)

    /**
     * Component.isShowing-like: whether it is on screen. true when it is [isDisplayable], it and all its parents in the visual tree are visible (Visibility.Visible),
     * and the window it is placed in ([topLevelAncestor]) is not hidden. Inside a hidden [WFrame] it is false as in Swing
     * ([isDisplayable] stays true). It is true even if the window is minimized or behind other windows (the same as Swing).
     * It is also true when transparent ([opacity] = 0) or scrolled out of view.
     */
    val isShowing: Boolean
        get() = isDisplayable && ComponentShowing.isVisibleInTree(this) && topLevelAncestor?.isVisible != false

    /**
     * JComponent.getTopLevelAncestor-like: the window this component is placed in. Unlike Swing's Container, the window is returned
     * as a [WFrame]. For a component in a dialog ([WContentDialog] etc.) or a flyout, the window showing it is returned.
     * null when [isDisplayable] is false (not placed, or a closed window) (Swing also returns a window before it is shown).
     */
    val topLevelAncestor: WFrame?
        get() = if (isDisplayable) ComponentShowing.frameOf(this) else null

    /**
     * The theme applied to this element and everything below it (FrameworkElement.RequestedTheme).
     * Setting it on the root element makes it the whole app's theme. [ElementTheme.DEFAULT] follows the OS setting.
     */
    var requestedTheme: ElementTheme
        get() = ElementTheme.of(frameworkElement.getInt(XamlInterop.IFrameworkElement_get_RequestedTheme))
        set(value) = frameworkElement.call(XamlInterop.IFrameworkElement_put_RequestedTheme, value.native)

    /**
     * The theme actually in effect (FrameworkElement.ActualTheme).
     * Returns the resolved LIGHT / DARK even when [requestedTheme] is [ElementTheme.DEFAULT].
     */
    val actualTheme: ElementTheme
        get() = ElementTheme.of(frameworkElement.getInt(XamlInterop.IFrameworkElement_get_ActualTheme))

    /** Event tokens registered via addActualThemeChangedListener. */
    private val actualThemeChangedTokens = ListenerTokens<Runnable>()

    /** Subscribes to changes in the applied theme (FrameworkElement.ActualThemeChanged). */
    @JvmSynthetic
    fun addActualThemeChangedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addActualThemeChangedListener(adapter)
        actualThemeChangedTokens.addKotlinAdapter(listener, adapter)
    }

    fun addActualThemeChangedListener(listener: Runnable) {
        val token = frameworkElement.addEventHandler(
            "WinUI4K.ActualThemeChangedHandler",
            XamlInterop.IID_ActualThemeChangedHandler,
            XamlInterop.IFrameworkElement_add_ActualThemeChanged,
        ) { _, _ -> listener.run() }
        actualThemeChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addActualThemeChangedListener]. */
    @JvmSynthetic
    fun removeActualThemeChangedListener(listener: () -> Unit) {
        val adapter = actualThemeChangedTokens.removeKotlinAdapter(listener) ?: return
        removeActualThemeChangedListener(adapter)
    }

    fun removeActualThemeChangedListener(listener: Runnable) {
        val token = actualThemeChangedTokens.remove(listener) ?: return
        frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_ActualThemeChanged, token)
    }

    /** Event tokens registered via addSizeChangedListener. */
    private val sizeChangedTokens = ListenerTokens<Runnable>()

    /**
     * Subscribes to post-layout size changes (FrameworkElement.SizeChanged).
     * The listener is called in the middle of XAML's layout, so a dialog that waits until it is closed cannot be opened from inside it
     * ([IllegalStateException]; open it later with WinUiUtilities.invokeLater).
     */
    @JvmSynthetic
    fun addSizeChangedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addSizeChangedListener(adapter)
        sizeChangedTokens.addKotlinAdapter(listener, adapter)
    }

    fun addSizeChangedListener(listener: Runnable) {
        val token = frameworkElement.addEventHandler(
            "WinUI4K.SizeChangedHandler",
            XamlInterop.IID_SizeChangedEventHandler,
            XamlInterop.IFrameworkElement_add_SizeChanged,
        ) { _, _ -> ModalLoop.duringLayout { listener.run() } }
        sizeChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSizeChangedListener]. */
    @JvmSynthetic
    fun removeSizeChangedListener(listener: () -> Unit) {
        val adapter = sizeChangedTokens.removeKotlinAdapter(listener) ?: return
        removeSizeChangedListener(adapter)
    }

    fun removeSizeChangedListener(listener: Runnable) {
        val token = sizeChangedTokens.remove(listener) ?: return
        frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_SizeChanged, token)
    }

    /** Event tokens registered via addLoadedListener. */
    private val loadedTokens = ListenerTokens<Runnable>()

    /**
     * Subscribes to the moment the element is added to the visual tree and ready to render
     * (FrameworkElement.Loaded). Useful for initializing parts that are only reachable after the
     * template is applied (e.g. ScrollView.ScrollPresenter).
     * The listener is called in the middle of XAML's frame processing, so a dialog that waits until it is closed cannot be opened from inside it
     * ([IllegalStateException]; open it later with WinUiUtilities.invokeLater).
     */
    @JvmSynthetic
    fun addLoadedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addLoadedListener(adapter)
        loadedTokens.addKotlinAdapter(listener, adapter)
    }

    fun addLoadedListener(listener: Runnable) {
        val token = frameworkElement.addEventHandler(
            "WinUI4K.LoadedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> ModalLoop.duringLayout { listener.run() } }
        loadedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addLoadedListener]. */
    @JvmSynthetic
    fun removeLoadedListener(listener: () -> Unit) {
        val adapter = loadedTokens.removeKotlinAdapter(listener) ?: return
        removeLoadedListener(adapter)
    }

    fun removeLoadedListener(listener: Runnable) {
        val token = loadedTokens.remove(listener) ?: return
        frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_Loaded, token)
    }

    /**
     * The text hint shown on hover (ToolTipService.ToolTip). Equivalent to JComponent.get/setToolTipText. null removes it.
     * If you need placement or a non-text hint, set a [WToolTip] to [toolTip].
     * Reading it returns the current hint's string: if the content of the [WToolTip] attached with [toolTip] is a string, that string
     * ([WToolTip.tipText]); null for non-string content ([WToolTip.content]) or when there is no hint.
     */
    var toolTipText: String?
        get() = ComponentSupport.toolTipText(dependencyObject)
        set(value) {
            detachToolTip()
            if (value == null) {
                toolTipServiceStatics.call(XamlInterop.IToolTipServiceStatics_SetToolTip, dependencyObject, null)
            } else {
                val boxed = PropertyValues.boxString(value)
                toolTipServiceStatics.call(
                    XamlInterop.IToolTipServiceStatics_SetToolTip,
                    dependencyObject,
                    boxed.ptr,
                )
                boxed.release()
            }
        }

    /**
     * The attached [WToolTip] (sets a [WToolTip] to ToolTipService.ToolTip). Used for hints with placement or non-text content.
     * For a simple string, [toolTipText] is enough.
     * Reading it returns the attached [WToolTip], and null if a text hint was set with [toolTipText] or there is no hint.
     * Setting null removes the hint (including a text hint).
     * If the set [WToolTip] is attached to another component, it is detached from there (that component loses its hint).
     */
    var toolTip: WToolTip?
        get() = attachedToolTip
        set(value) {
            toolTipText = null
            if (value == null) return
            value.component?.takeIf { it !== this }?.toolTipText = null
            toolTipServiceStatics.call(
                XamlInterop.IToolTipServiceStatics_SetToolTip,
                dependencyObject,
                value.inspectable.ptr,
            )
            attachedToolTip = value
            value.attachedTo(this)
        }

    /** The hint attached with [toolTip] (until another hint is set). */
    private var attachedToolTip: WToolTip? = null

    private fun detachToolTip() {
        attachedToolTip?.attachedTo(null)
        attachedToolTip = null
    }

    /** The context menu opened by right-click / long-press (UIElement.ContextFlyout). */
    var contextFlyout: WFlyoutBase? = null
        set(value) {
            field = value
            uiElement.call(
                XamlInterop.IUIElement_put_ContextFlyout,
                value?.flyoutBase?.ptr,
            )
            value?.attachedTo(this)
        }

    /**
     * JComponent.getComponentPopupMenu / setComponentPopupMenu-like: the popup menu opened by right click, long press or the application key (Shift+F10).
     * It is [contextFlyout] seen as a [WMenuFlyout], and setting it sets [contextFlyout].
     * null when [contextFlyout] is set to something other than a [WMenuFlyout] (WFlyout, etc.). There is no mechanism to inherit the parent's menu
     * (JComponent.setInheritsPopupMenu).
     */
    var componentPopupMenu: WMenuFlyout?
        get() = contextFlyout as? WMenuFlyout
        set(value) {
            contextFlyout = value
        }

    /**
     * Adds a keyboard shortcut (UIElement.KeyboardAccelerators).
     * While this element is visible, pressing [key] + [modifiers] triggers the equivalent of a click.
     */
    fun addKeyboardAccelerator(key: VirtualKey, vararg modifiers: VirtualKeyModifier) {
        val accelerator = createKeyboardAccelerator(key, modifiers)
        val accelerators = uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        accelerators.call(FoundationInterop.IVector_Append, accelerator.ptr)
        accelerators.release()
        accelerator.release()
    }

    /**
     * Adds a keyboard shortcut that runs [action] on press
     * (UIElement.KeyboardAccelerators + KeyboardAccelerator.Invoked).
     * While this element is visible, pressing [key] + [modifiers] calls [action].
     * Sets Handled so a click-triggered default action doesn't also fire.
     */
    fun addKeyboardAccelerator(key: VirtualKey, vararg modifiers: VirtualKeyModifier, action: Runnable) {
        val accelerator = createKeyboardAccelerator(key, modifiers)
        accelerator.addEventHandler(
            "WinUI4K.KeyboardAcceleratorInvokedHandler",
            XamlInterop.IID_KeyboardAcceleratorInvokedHandler,
            XamlInterop.IKeyboardAccelerator_add_Invoked,
        ) { _, args ->
            ComPtr(args).putBool(XamlInterop.IKeyboardAcceleratorInvokedEventArgs_put_Handled, true)
            action.run()
        }
        val accelerators = uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        accelerators.call(FoundationInterop.IVector_Append, accelerator.ptr)
        accelerators.release()
        accelerator.release()
    }

    /**
     * Adds a keyboard shortcut for [keyStroke] (Ctrl+S and so on) (UIElement.KeyboardAccelerators).
     * As with [addKeyboardAccelerator] (VirtualKey), pressing it while this element is visible invokes the click-equivalent action.
     * [WKeyStroke] is written with AWT key codes like Swing's KeyStroke, so keys not in VirtualKey (; , and so on) can also be used.
     * Key strokes for character input, for key release, or including AltGraph or mouse buttons throw [IllegalArgumentException].
     */
    fun addKeyboardAccelerator(keyStroke: WKeyStroke) {
        val (key, modifiers) = keyStroke.toAccelerator()
        appendKeyboardAccelerator(createKeyboardAccelerator(key, modifiers))
    }

    /**
     * Adds a keyboard shortcut that calls [action] when [keyStroke] is pressed
     * (the [WKeyStroke] version of [addKeyboardAccelerator] (VirtualKey, ..., Runnable)). It sets Handled, so
     * default behavior such as invoking a click does not happen. Unusable key strokes are the same as for [addKeyboardAccelerator] (WKeyStroke).
     * It works even if this element does not have the focus (close to WHEN_IN_FOCUSED_WINDOW). Swing-style key bindings are [getInputMap] and [actionMap].
     */
    fun addKeyboardAccelerator(keyStroke: WKeyStroke, action: Runnable) {
        val (key, modifiers) = keyStroke.toAccelerator()
        val accelerator = createKeyboardAccelerator(key, modifiers)
        accelerator.addEventHandler(
            "WinUI4K.KeyboardAcceleratorInvokedHandler",
            XamlInterop.IID_KeyboardAcceleratorInvokedHandler,
            XamlInterop.IKeyboardAccelerator_add_Invoked,
        ) { _, args ->
            ComPtr(args).putBool(XamlInterop.IKeyboardAcceleratorInvokedEventArgs_put_Handled, true)
            action.run()
        }
        appendKeyboardAccelerator(accelerator)
    }

    /** Adds [accelerator] to KeyboardAccelerators and releases it. */
    internal fun appendKeyboardAccelerator(accelerator: ComPtr) {
        val accelerators = uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        accelerators.call(FoundationInterop.IVector_Append, accelerator.ptr)
        accelerators.release()
        accelerator.release()
    }

    /**
     * Removes the first accelerator in KeyboardAccelerators whose key is [key] (a Windows virtual key) and whose modifiers are
     * [modifiers] (VirtualKeyModifiers). Returns true if one was removed, false if there was none.
     */
    internal fun removeKeyboardAccelerator(key: Int, modifiers: Int): Boolean =
        ComponentSupport.removeKeyboardAccelerator(uiElement, key, modifiers)

    companion object {
        /** JComponent.WHEN_FOCUSED-like: key bindings while it has the focus (a [getInputMap] condition; the value is 0, the same as Swing). */
        const val WHEN_FOCUSED: Int = 0

        /** JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT-like: key bindings while it or a descendant has the focus (the value is 1). */
        const val WHEN_ANCESTOR_OF_FOCUSED_COMPONENT: Int = 1

        /** JComponent.WHEN_IN_FOCUSED_WINDOW-like: key bindings while somewhere in the same window has the focus (the value is 2). */
        const val WHEN_IN_FOCUSED_WINDOW: Int = 2

        /** JComponent.UNDEFINED_CONDITION-like: "no mapping" for [getConditionForKeyStroke] (the value is -1, the same as Swing). */
        const val UNDEFINED_CONDITION: Int = -1

        /** Like Component.TOP_ALIGNMENT: top alignment for [alignmentY]. */
        const val TOP_ALIGNMENT: Float = 0.0f

        /** Like Component.CENTER_ALIGNMENT: center alignment for [alignmentX] / [alignmentY]. */
        const val CENTER_ALIGNMENT: Float = 0.5f

        /** Like Component.BOTTOM_ALIGNMENT: bottom alignment for [alignmentY]. */
        const val BOTTOM_ALIGNMENT: Float = 1.0f

        /** Like Component.LEFT_ALIGNMENT: left alignment for [alignmentX]. */
        const val LEFT_ALIGNMENT: Float = 0.0f

        /** Like Component.RIGHT_ALIGNMENT: right alignment for [alignmentX]. */
        const val RIGHT_ALIGNMENT: Float = 1.0f
    }
}

/**
 * Windows.System.VirtualKeyModifiers (the accelerator's modifier keys, a bit flag).
 * Values extracted from Windows.Foundation.UniversalApiContract.winmd.
 */
enum class VirtualKeyModifier(internal val native: Int) {
    /** The Ctrl key. */
    CONTROL(1),

    /** The Alt key (called Menu in WinRT). */
    MENU(2),

    /** The Shift key. */
    SHIFT(4),

    /** The Windows key. */
    WINDOWS(8),
}

/** Creates a KeyboardAccelerator and returns its default interface pointer. Caller must release it. */
internal fun createKeyboardAccelerator(
    key: VirtualKey,
    modifiers: Array<out VirtualKeyModifier>,
): ComPtr = createKeyboardAccelerator(key.native, modifiers.fold(0) { acc, modifier -> acc or modifier.native })

/**
 * Creates a KeyboardAccelerator with the Windows virtual key [key] and the VirtualKeyModifiers value [modifiers] and returns its default interface pointer
 * (OEM keys not in the VirtualKey enumeration can also be passed). Release it after use.
 */
internal fun createKeyboardAccelerator(key: Int, modifiers: Int): ComPtr {
    val accelerator =
        Activation.composeDefault(XamlInterop.CLS_KeyboardAccelerator, XamlInterop.IID_IKeyboardAcceleratorFactory)
    accelerator.call(XamlInterop.IKeyboardAccelerator_put_Key, key)
    if (modifiers != 0) accelerator.call(XamlInterop.IKeyboardAccelerator_put_Modifiers, modifiers)
    return accelerator
}

/**
 * The value if all four sides are equal, otherwise 0 (cannot be expressed as one number).
 * The value remembered by [WComponent.margin] / [WControl.padding] / [WBorder.padding].
 */
internal fun uniformThickness(left: Double, top: Double, right: Double, bottom: Double): Double =
    if (left == top && top == right && right == bottom) left else 0.0
