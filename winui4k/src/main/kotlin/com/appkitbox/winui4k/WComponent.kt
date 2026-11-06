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
) {
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
        val options = Activation.activate(XamlInterop.CLS_BringIntoViewOptions, XamlInterop.IID_IBringIntoViewOptions)
        try {
            options.putBool(XamlInterop.IBringIntoViewOptions_put_AnimationDesired, false)
            val boxed = PropertyValues.boxRect(rect.x.toDouble(), rect.y.toDouble(), rect.width.toDouble(), rect.height.toDouble())
            val target = try {
                boxed.queryInterface(FoundationInterop.IID_IReference_Rect)
            } finally {
                boxed.release()
            }
            try {
                options.call(XamlInterop.IBringIntoViewOptions_put_TargetRect, target.ptr)
            } finally {
                target.release()
            }
            uiElement.call(XamlInterop.IUIElement_StartBringIntoViewWithOptions, options.ptr)
        } finally {
            options.release()
        }
    }

    /**
     * Sets the size a layout manager assigns. Does not change [width] / [height] (the record of the
     * user's intent). Called from [WLayoutPanel.setBounds].
     */
    internal fun setLayoutSize(width: Double, height: Double) {
        applyWidth(width)
        applyHeight(height)
    }

    /**
     * A cache of the natural size (the preferred size measured from the content). null means it
     * hasn't been measured yet, or [invalidateNaturalSize] was called since — the parent
     * [WLayoutPanel]'s next layout pass will re-measure it.
     */
    internal var naturalSize: WSize? = null

    /**
     * Clears the explicit size on whichever dimension the user hasn't explicitly set, in
     * preparation for re-measuring the natural size (in XAML, DesiredSize keeps tracking an
     * explicitly-set Width even after it's set).
     */
    internal fun clearLayoutSizeForMeasure() {
        if (width.isNaN()) applyWidth(Double.NaN)
        if (height.isNaN()) applyHeight(Double.NaN)
    }

    /** The preferred size computed by the most recent Measure pass (UIElement.DesiredSize). */
    internal fun readDesiredSize(): WSize {
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
        }

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
        }

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

    /** Requests focus (equivalent to Component.requestFocus). Use when the result is not needed. */
    fun requestFocus() {
        requestFocusInWindow()
    }

    /**
     * Requests focus and returns whether it was accepted (equivalent to Component.requestFocusInWindow; the result of
     * UIElement.Focus). false for elements not placed in a window, disabled or unable to receive focus, and for elements
     * whose [isFocusable] is set to false. IsTabStop is not checked, so it is true for [WSpinner] and the like, whose own
     * IsTabStop is false but whose template part receives focus.
     */
    fun requestFocusInWindow(): Boolean = focusableSetting != false && focus(XamlInterop.FocusState_Programmatic)

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
        set(value) = frameworkElement.call(XamlInterop.IFrameworkElement_put_MaxWidth, value)

    /** Horizontal position within the space the parent allots (FrameworkElement.HorizontalAlignment). */
    var horizontalAlignment: HorizontalAlignment
        get() = HorizontalAlignment.of(frameworkElement.getInt(XamlInterop.IFrameworkElement_get_HorizontalAlignment))
        set(value) = frameworkElement.call(XamlInterop.IFrameworkElement_put_HorizontalAlignment, value.native)

    /** The direction of text and layout (FrameworkElement.FlowDirection). Inherited from the parent unless set. */
    var flowDirection: FlowDirection
        get() = FlowDirection.of(frameworkElement.getInt(XamlInterop.IFrameworkElement_get_FlowDirection))
        set(value) = frameworkElement.call(XamlInterop.IFrameworkElement_put_FlowDirection, value.native)

    /** Vertical position within the space the parent allots (FrameworkElement.VerticalAlignment). */
    var verticalAlignment: VerticalAlignment
        get() = VerticalAlignment.of(frameworkElement.getInt(XamlInterop.IFrameworkElement_get_VerticalAlignment))
        set(value) = frameworkElement.call(XamlInterop.IFrameworkElement_put_VerticalAlignment, value.native)

    /**
     * A uniform margin on all four sides. Passes a Thickness (double×4) by value to put_Margin. Use [setMargin] to set each side individually.
     * Reading returns the native FrameworkElement.Margin: its value if all four sides are equal, otherwise NaN
     * (for example after setting different values with [setMargin], since they cannot be expressed as one number).
     */
    var margin: Double
        get() = uniformThickness(XamlStructs.getThickness(frameworkElement, XamlInterop.IFrameworkElement_get_Margin))
        set(value) = setMargin(value, value, value, value)

    /** Sets the margin on each side individually (FrameworkElement.Margin). */
    fun setMargin(left: Double, top: Double, right: Double, bottom: Double) {
        XamlStructs.putThickness(frameworkElement, XamlInterop.IFrameworkElement_put_Margin, left, top, right, bottom)
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
        addActualThemeChangedListenerForJava(adapter)
        actualThemeChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addActualThemeChangedListener")
    fun addActualThemeChangedListenerForJava(listener: Runnable) {
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
        removeActualThemeChangedListenerForJava(adapter)
    }

    @JvmName("removeActualThemeChangedListener")
    fun removeActualThemeChangedListenerForJava(listener: Runnable) {
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
        addSizeChangedListenerForJava(adapter)
        sizeChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addSizeChangedListener")
    fun addSizeChangedListenerForJava(listener: Runnable) {
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
        removeSizeChangedListenerForJava(adapter)
    }

    @JvmName("removeSizeChangedListener")
    fun removeSizeChangedListenerForJava(listener: Runnable) {
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
        addLoadedListenerForJava(adapter)
        loadedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addLoadedListener")
    fun addLoadedListenerForJava(listener: Runnable) {
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
        removeLoadedListenerForJava(adapter)
    }

    @JvmName("removeLoadedListener")
    fun removeLoadedListenerForJava(listener: Runnable) {
        val token = loadedTokens.remove(listener) ?: return
        frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_Loaded, token)
    }

    /**
     * The string hint shown on hover (ToolTipService.ToolTip). Equivalent to JComponent.setToolTipText.
     * null removes it. Use [setToolTip] if you need to specify placement or a non-string hint.
     * Reading returns the current hint string: if the content of the [WToolTip] attached with [setToolTip] is a string, that string
     * ([WToolTip.tipText]); null for non-string content ([WToolTip.content]) or when there is no hint.
     */
    var toolTip: String?
        get() {
            val current = toolTipServiceStatics.getPtrOrNull(XamlInterop.IToolTipServiceStatics_GetToolTip, dependencyObject)
                ?: return null
            try {
                val contentControl = current.queryInterfaceOrNull(XamlInterop.IID_IContentControl)
                    ?: return PropertyValues.unboxString(current) // A string hint
                try {
                    // A ToolTip (WToolTip) hint: its string if the content is a string
                    val content = contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content) ?: return null
                    try {
                        return PropertyValues.unboxString(content)
                    } finally {
                        content.release()
                    }
                } finally {
                    contentControl.release()
                }
            } finally {
                current.release()
            }
        }
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
     * Attaches a hint with a specific placement or non-string content to this target
     * (sets [WToolTip] as ToolTipService.ToolTip). Use [toolTip] for a plain string.
     * If [toolTip] is attached to another component, it is removed from there (that component loses its hint).
     */
    fun setToolTip(toolTip: WToolTip) {
        this.toolTip = null
        toolTip.component?.takeIf { it !== this }?.toolTip = null
        toolTipServiceStatics.call(
            XamlInterop.IToolTipServiceStatics_SetToolTip,
            dependencyObject,
            toolTip.inspectable.ptr,
        )
        attachedToolTip = toolTip
        toolTip.attachedTo(this)
    }

    /** The hint attached with [setToolTip] (until another hint is set). */
    private var attachedToolTip: WToolTip? = null

    private fun detachToolTip() {
        attachedToolTip?.attachedTo(null)
        attachedToolTip = null
    }

    /** An alias for [toolTip] (equivalent to JComponent.get/setToolTipText). Reads and writes the same value. */
    var toolTipText: String?
        get() = toolTip
        set(value) {
            toolTip = value
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
     * Use it instead of Swing's InputMap and ActionMap (close to WHEN_IN_FOCUSED_WINDOW; works even when this element does not have focus).
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
    internal fun removeKeyboardAccelerator(key: Int, modifiers: Int): Boolean {
        val accelerators = uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        try {
            for (index in 0 until accelerators.getInt(FoundationInterop.IVector_get_Size)) {
                val accelerator = accelerators.getPtr(FoundationInterop.IVector_GetAt, index)
                val matches = try {
                    accelerator.getInt(XamlInterop.IKeyboardAccelerator_get_Key) == key &&
                        accelerator.getInt(XamlInterop.IKeyboardAccelerator_get_Modifiers) == modifiers
                } finally {
                    accelerator.release()
                }
                if (matches) {
                    accelerators.call(FoundationInterop.IVector_RemoveAt, index)
                    return true
                }
            }
            return false
        } finally {
            accelerators.release()
        }
    }

    companion object {
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

/** The value if all four of a Thickness (left, top, right, bottom) are equal, otherwise NaN (for reading [WComponent.margin] / [WControl.padding]). */
internal fun uniformThickness(thickness: DoubleArray): Double =
    if (thickness.all { it == thickness[0] }) thickness[0] else Double.NaN
