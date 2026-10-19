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
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
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
     * (WinUI4K has no setBounds that writes this value; placement is left to layout. Children of [WLayoutPanel] are placed
     * by the WLayoutManager.)
     */
    val bounds: WRectangle
        get() = WRectangle(x, y, Math.round(actualWidth).toInt(), Math.round(actualHeight).toInt())

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
    var minimumSize: WSize
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
    var maximumSize: WSize
        get() = WSize(
            frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MaxWidth),
            frameworkElement.getDouble(XamlInterop.IFrameworkElement_get_MaxHeight),
        )
        set(value) {
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MaxWidth, value.width)
            frameworkElement.call(XamlInterop.IFrameworkElement_put_MaxHeight, value.height)
        }

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
     * focus of that part is reported as this component's, and moving between the parts does not call it.
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
        if (listener != null) input.mouse.addMouseListener(listener)
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
        if (listener != null) input.mouse.addMouseMotionListener(listener)
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
        if (listener != null) input.mouse.addMouseWheelListener(listener)
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

    /**
     * Invalidates the natural size cache (equivalent to Swing's revalidate). Call this alongside
     * the parent [WLayoutPanel]'s revalidate whenever the content's size changes, e.g. from a text
     * change.
     */
    fun invalidateNaturalSize() {
        naturalSize = null
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

    /** A uniform margin on all four sides. Passes a Thickness (double×4) by value to put_Margin. Use [setMargin] to set each side individually. */
    var margin: Double = 0.0
        set(value) {
            field = value
            setMargin(value, value, value, value)
        }

    /** Sets the margin on each side individually (FrameworkElement.Margin). */
    fun setMargin(left: Double, top: Double, right: Double, bottom: Double) {
        XamlStructs.putThickness(frameworkElement, XamlInterop.IFrameworkElement_put_Margin, left, top, right, bottom)
    }

    /** Opacity, from 0.0 (transparent) to 1.0 (opaque, the default) (UIElement.Opacity). */
    var opacity: Double
        get() = uiElement.getDouble(XamlInterop.IUIElement_get_Opacity)
        set(value) = uiElement.call(XamlInterop.IUIElement_put_Opacity, value)

    /** Whether this is shown (UIElement.Visibility). false is Collapsed (doesn't reserve layout space either). */
    var isVisible: Boolean
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

    /** Subscribes to post-layout size changes (FrameworkElement.SizeChanged). */
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
        ) { _, _ -> listener.run() }
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
        ) { _, _ -> listener.run() }
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
     */
    var toolTip: String? = null
        set(value) {
            field = value
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
     */
    fun setToolTip(toolTip: WToolTip) {
        this.toolTip = null
        toolTipServiceStatics.call(
            XamlInterop.IToolTipServiceStatics_SetToolTip,
            dependencyObject,
            toolTip.inspectable.ptr,
        )
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
): ComPtr {
    val accelerator =
        Activation.composeDefault(XamlInterop.CLS_KeyboardAccelerator, XamlInterop.IID_IKeyboardAcceleratorFactory)
    accelerator.call(XamlInterop.IKeyboardAccelerator_put_Key, key.native)
    val combined = modifiers.fold(0) { acc, modifier -> acc or modifier.native }
    if (combined != 0) accelerator.call(XamlInterop.IKeyboardAccelerator_put_Modifiers, combined)
    return accelerator
}
