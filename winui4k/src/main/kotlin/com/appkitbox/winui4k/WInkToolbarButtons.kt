package com.appkitbox.winui4k

import com.appkitbox.winui4k.ink.InkDrawingAttributes
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.InkInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.InkToolbarTool (the kind of tool button).
 * Values extracted from Microsoft.UI.Xaml.winmd (BallpointPen=0, Pencil=1, Highlighter=2, Eraser=3, CustomPen=4,
 * CustomTool=5).
 */
enum class InkToolbarTool(internal val native: Int) {
    /** Ballpoint pen. */
    BALLPOINT_PEN(0),

    /** Pencil. */
    PENCIL(1),

    /** Highlighter. */
    HIGHLIGHTER(2),

    /** Eraser. */
    ERASER(3),

    /** An app-defined pen ([WInkToolbarCustomPenButton]). */
    CUSTOM_PEN(4),

    /** An app-defined tool ([WInkToolbarCustomToolButton]). */
    CUSTOM_TOOL(5),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkToolbarTool = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.InkToolbarToggle (the kind of toggle button).
 * Values extracted from Microsoft.UI.Xaml.winmd (Ruler=0, Custom=1).
 */
enum class InkToolbarToggle(internal val native: Int) {
    /** Ruler. */
    RULER(0),

    /** An app-defined toggle ([WInkToolbarCustomToggleButton]). */
    CUSTOM(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkToolbarToggle = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.InkToolbarMenuKind (the kind of menu button).
 * Values extracted from Microsoft.UI.Xaml.winmd (Stencil=0).
 */
enum class InkToolbarMenuKind(internal val native: Int) {
    /** The stencil (ruler, protractor) menu ([WInkToolbarStencilButton]). */
    STENCIL(0),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkToolbarMenuKind = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.InkToolbarStencilKind (the kind of stencil).
 * Values extracted from Microsoft.UI.Xaml.winmd (Ruler=0, Protractor=1).
 */
enum class InkToolbarStencilKind(internal val native: Int) {
    /** Ruler. */
    RULER(0),

    /** Protractor. */
    PROTRACTOR(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkToolbarStencilKind = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.InkToolbarFlyoutItemKind (the kind of flyout item).
 * Values extracted from Microsoft.UI.Xaml.winmd (Simple=0, Radio=1, Check=2, RadioCheck=3).
 */
enum class InkToolbarFlyoutItemKind(internal val native: Int) {
    /** An item that is just pressed. */
    SIMPLE(0),

    /** A mutually exclusive item. */
    RADIO(1),

    /** A check item. */
    CHECK(2),

    /** A mutually exclusive item that can be cleared by pressing it again while selected. */
    RADIO_CHECK(3),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkToolbarFlyoutItemKind = entries.first { it.native == native }
    }
}

/**
 * The common type of buttons (tools, toggles, menus) that can be placed on a [WInkToolbar].
 * Implemented by [WInkToolbarToolButton] / [WInkToolbarToggleButton] / [WInkToolbarMenuButton].
 */
sealed interface WInkToolbarItem

// ======================================================================
// Tool buttons (InkToolbarToolButton : RadioButton)
// ======================================================================

/**
 * The WinUI 3 InkToolbarToolButton (experimental in Windows App SDK 2.5): the base of buttons for tools of which only
 * one can be selected in an InkToolbar (pens, eraser, and so on). Derived from [WRadioButton]; the selected tool
 * becomes [WInkToolbar.activeTool].
 */
abstract class WInkToolbarToolButton internal constructor(inspectable: ComPtr) : WRadioButton(inspectable), WInkToolbarItem {
    private val toolButton: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarToolButton)) }

    /** The kind of tool (ToolKind). */
    val toolKind: InkToolbarTool
        get() = InkToolbarTool.of(toolButton.getInt(InkInterop.IInkToolbarToolButton_get_ToolKind))

    /**
     * Whether to show the mark indicating a settings flyout (the arrow in the button's corner) (IsExtensionGlyphShown).
     */
    var isExtensionGlyphShown: Boolean
        get() = toolButton.getBool(InkInterop.IInkToolbarToolButton_get_IsExtensionGlyphShown)
        set(value) = toolButton.putBool(InkInterop.IInkToolbarToolButton_put_IsExtensionGlyphShown, value)
}

/**
 * The WinUI 3 InkToolbarPenButton (experimental in Windows App SDK 2.5): the base of pen tool buttons.
 * It has color candidates ([palette]) and a range of sizes, and the color and size can be chosen in the button's flyout.
 */
abstract class WInkToolbarPenButton internal constructor(inspectable: ComPtr) : WInkToolbarToolButton(inspectable) {
    private val penButton: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarPenButton)) }

    /**
     * The color candidates (Palette; IVector<Brush>). Only solid color brushes are returned as colors.
     * Empty before being placed on an InkToolbar (the toolbar sets the default candidates). Setting this replaces the
     * candidates.
     */
    var palette: List<WColor>
        get() {
            val vector = penButton.getPtrOrNull(InkInterop.IInkToolbarPenButton_get_Palette) ?: return emptyList()
            try {
                val size = vector.getInt(FoundationInterop.IVector_get_Size)
                return (0 until size).mapNotNull { index ->
                    val brush = vector.getPtr(FoundationInterop.IVector_GetAt, index)
                    try {
                        colorOf(brush)
                    } finally {
                        brush.release()
                    }
                }
            } finally {
                vector.release()
            }
        }
        set(value) {
            val collection = Activation.activate(InkInterop.CLS_BrushCollection, InkInterop.IID_IVector_Brush)
            try {
                for (color in value) {
                    val solid = color.createBrush()
                    val brush = solid.queryInterface(InkInterop.IID_IBrush)
                    try {
                        collection.call(FoundationInterop.IVector_Append, brush)
                    } finally {
                        brush.release()
                        solid.release()
                    }
                }
                penButton.call(InkInterop.IInkToolbarPenButton_put_Palette, collection)
            } finally {
                collection.release()
            }
        }

    /** The minimum selectable size (MinStrokeWidth). */
    var minStrokeWidth: Double
        get() = penButton.getDouble(InkInterop.IInkToolbarPenButton_get_MinStrokeWidth)
        set(value) = penButton.call(InkInterop.IInkToolbarPenButton_put_MinStrokeWidth, value)

    /** The maximum selectable size (MaxStrokeWidth). */
    var maxStrokeWidth: Double
        get() = penButton.getDouble(InkInterop.IInkToolbarPenButton_get_MaxStrokeWidth)
        set(value) = penButton.call(InkInterop.IInkToolbarPenButton_put_MaxStrokeWidth, value)

    /** The selected color (SelectedBrush). Null if there are no candidates or the brush is not a solid color. */
    val selectedColor: WColor?
        get() {
            val brush = penButton.getPtrOrNull(InkInterop.IInkToolbarPenButton_get_SelectedBrush) ?: return null
            return try {
                colorOf(brush)
            } finally {
                brush.release()
            }
        }

    /** The position of the selected color in [palette] (SelectedBrushIndex). */
    var selectedBrushIndex: Int
        get() = penButton.getInt(InkInterop.IInkToolbarPenButton_get_SelectedBrushIndex)
        set(value) = penButton.call(InkInterop.IInkToolbarPenButton_put_SelectedBrushIndex, value)

    /** The selected size (SelectedStrokeWidth). */
    var selectedStrokeWidth: Double
        get() = penButton.getDouble(InkInterop.IInkToolbarPenButton_get_SelectedStrokeWidth)
        set(value) = penButton.call(InkInterop.IInkToolbarPenButton_put_SelectedStrokeWidth, value)
}

/** The color of a brush (IBrush and so on) if it is a solid color (SolidColorBrush). */
internal fun colorOf(brush: ComPtr): WColor? {
    val solid = brush.queryInterfaceOrNull(XamlInterop.IID_ISolidColorBrush) ?: return null
    return try {
        val argb = XamlStructs.getColor(solid, XamlInterop.ISolidColorBrush_get_Color)
        WColor(argb[1], argb[2], argb[3], argb[0])
    } finally {
        solid.release()
    }
}

/** The WinUI 3 InkToolbarBallpointPenButton: the ballpoint pen button. */
class WInkToolbarBallpointPenButton internal constructor(inspectable: ComPtr) : WInkToolbarPenButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarBallpointPenButton, InkInterop.IID_IInkToolbarBallpointPenButtonFactory),
    )
}

/** The WinUI 3 InkToolbarPencilButton: the pencil button. */
class WInkToolbarPencilButton internal constructor(inspectable: ComPtr) : WInkToolbarPenButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarPencilButton, InkInterop.IID_IInkToolbarPencilButtonFactory),
    )
}

/** The WinUI 3 InkToolbarHighlighterButton: the highlighter button. */
class WInkToolbarHighlighterButton internal constructor(inspectable: ComPtr) : WInkToolbarPenButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarHighlighterButton, InkInterop.IID_IInkToolbarHighlighterButtonFactory),
    )
}

/**
 * The WinUI 3 InkToolbarCustomPenButton: the button for an app-defined pen ([customPen]).
 * The process of creating drawing attributes from the selected color and size is replaced by a [WInkToolbarCustomPen].
 */
class WInkToolbarCustomPenButton internal constructor(inspectable: ComPtr) : WInkToolbarPenButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarCustomPenButton, InkInterop.IID_IInkToolbarCustomPenButtonFactory),
    )

    private val customPenButton: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarCustomPenButton)) }

    /** The pen that creates the drawing attributes (CustomPen). */
    var customPen: WInkToolbarCustomPen? = null
        set(value) {
            field = value
            customPenButton.call(InkInterop.IInkToolbarCustomPenButton_put_CustomPen, value?.native)
        }

    /**
     * The settings UI shown in the button's flyout (ConfigurationContent). Put a [WInkToolbarPenConfigurationControl]
     * or the like here.
     */
    var configurationContent: WComponent? = null
        set(value) {
            field = value
            customPenButton.call(InkInterop.IInkToolbarCustomPenButton_put_ConfigurationContent, value?.uiElement)
        }
}

/** The WinUI 3 InkToolbarEraserButton: the eraser button (its flyout has "Erase all"). */
class WInkToolbarEraserButton internal constructor(inspectable: ComPtr) : WInkToolbarToolButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarEraserButton, InkInterop.IID_IInkToolbarEraserButtonFactory),
    )

    private val eraserButton: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarEraserButton)) }

    /** Whether to show "Erase all ink" in the flyout (IsClearAllVisible; true by default). */
    var isClearAllVisible: Boolean
        get() = eraserButton.getBool(InkInterop.IInkToolbarEraserButton_get_IsClearAllVisible)
        set(value) = eraserButton.putBool(InkInterop.IInkToolbarEraserButton_put_IsClearAllVisible, value)
}

/**
 * The WinUI 3 InkToolbarCustomToolButton: the button for an app-defined tool (such as lasso selection).
 * When selected, the InkPresenter's input processing mode becomes [InkInputProcessingMode.NONE], and
 * input goes to [WInkPresenter.addUnprocessedInputListener].
 */
class WInkToolbarCustomToolButton internal constructor(inspectable: ComPtr) : WInkToolbarToolButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarCustomToolButton, InkInterop.IID_IInkToolbarCustomToolButtonFactory),
    )

    private val customToolButton: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarCustomToolButton)) }

    /** The settings UI shown in the button's flyout (ConfigurationContent). */
    var configurationContent: WComponent? = null
        set(value) {
            field = value
            customToolButton.call(InkInterop.IInkToolbarCustomToolButton_put_ConfigurationContent, value?.uiElement)
        }
}

// ======================================================================
// Toggle buttons (InkToolbarToggleButton : CheckBox)
// ======================================================================

/**
 * The WinUI 3 InkToolbarToggleButton (experimental in Windows App SDK 2.5): the base of buttons that are turned on and
 * off independently of the tool (such as the ruler). Derived from [WCheckBox].
 */
abstract class WInkToolbarToggleButton internal constructor(inspectable: ComPtr) : WCheckBox(inspectable), WInkToolbarItem {
    private val toggleButton: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarToggleButton)) }

    /** The kind of toggle (ToggleKind). */
    val toggleKind: InkToolbarToggle
        get() = InkToolbarToggle.of(toggleButton.getInt(InkInterop.IInkToolbarToggleButton_get_ToggleKind))
}

/** The WinUI 3 InkToolbarRulerButton: the button that toggles the display of the ruler. */
class WInkToolbarRulerButton internal constructor(inspectable: ComPtr) : WInkToolbarToggleButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarRulerButton, InkInterop.IID_IInkToolbarRulerButtonFactory),
    )
}

/**
 * The WinUI 3 InkToolbarCustomToggleButton: the button for an app-defined toggle. Its on/off state is handled with
 * [isSelected] / [addItemListener].
 */
class WInkToolbarCustomToggleButton internal constructor(inspectable: ComPtr) : WInkToolbarToggleButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarCustomToggleButton, InkInterop.IID_IInkToolbarCustomToggleButtonFactory),
    )
}

// ======================================================================
// Menu buttons (InkToolbarMenuButton : ToggleButton)
// ======================================================================

/**
 * The WinUI 3 InkToolbarMenuButton (experimental in Windows App SDK 2.5): the base of buttons that open a menu
 * (flyout) when pressed. Derived from [WToggleButton]; the menu items (such as stencils) are active while it is on.
 */
abstract class WInkToolbarMenuButton internal constructor(inspectable: ComPtr) : WToggleButton(inspectable), WInkToolbarItem {
    private val menuButton: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarMenuButton)) }

    /** The kind of menu (MenuKind). */
    val menuKind: InkToolbarMenuKind
        get() = InkToolbarMenuKind.of(menuButton.getInt(InkInterop.IInkToolbarMenuButton_get_MenuKind))

    /** Whether to show the mark indicating that there is a menu (IsExtensionGlyphShown). */
    var isExtensionGlyphShown: Boolean
        get() = menuButton.getBool(InkInterop.IInkToolbarMenuButton_get_IsExtensionGlyphShown)
        set(value) = menuButton.putBool(InkInterop.IInkToolbarMenuButton_put_IsExtensionGlyphShown, value)
}

/**
 * The WinUI 3 InkToolbarStencilButton: the menu button that shows the ruler and protractor (stencils).
 * When turned on, the [selectedStencil] stencil is shown on the canvas, and lines can be drawn along it.
 */
class WInkToolbarStencilButton internal constructor(inspectable: ComPtr) : WInkToolbarMenuButton(inspectable) {
    constructor() : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarStencilButton, InkInterop.IID_IInkToolbarStencilButtonFactory),
    )

    private val stencilButton: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarStencilButton)) }

    /** The stencil to show (SelectedStencil). */
    var selectedStencil: InkToolbarStencilKind
        get() = InkToolbarStencilKind.of(stencilButton.getInt(InkInterop.IInkToolbarStencilButton_get_SelectedStencil))
        set(value) = stencilButton.call(InkInterop.IInkToolbarStencilButton_put_SelectedStencil, value.native)

    /** Whether to show the ruler item in the menu (IsRulerItemVisible; true by default). */
    var isRulerItemVisible: Boolean
        get() = stencilButton.getBool(InkInterop.IInkToolbarStencilButton_get_IsRulerItemVisible)
        set(value) = stencilButton.putBool(InkInterop.IInkToolbarStencilButton_put_IsRulerItemVisible, value)

    /** Whether to show the protractor item in the menu (IsProtractorItemVisible; true by default). */
    var isProtractorItemVisible: Boolean
        get() = stencilButton.getBool(InkInterop.IInkToolbarStencilButton_get_IsProtractorItemVisible)
        set(value) = stencilButton.putBool(InkInterop.IInkToolbarStencilButton_put_IsProtractorItemVisible, value)

    /**
     * The ruler (Ruler; Windows.UI.Input.Inking.InkPresenterRuler). In the experimental Windows App SDK 2.5, InkToolbar
     * displays the ruler directly through the InkPresenter, so this property is never set and is always null.
     */
    val ruler: WInkPresenterRuler?
        get() = stencilButton.getPtrOrNull(InkInterop.IInkToolbarStencilButton_get_Ruler)?.let { WInkPresenterRuler(it) }

    /**
     * The protractor (Protractor). Always null in the experimental Windows App SDK 2.5 for the same reason as [ruler].
     */
    val protractor: WInkPresenterProtractor?
        get() = stencilButton.getPtrOrNull(InkInterop.IInkToolbarStencilButton_get_Protractor)?.let { WInkPresenterProtractor(it) }
}

/**
 * Windows.UI.Input.Inking.InkPresenterStencilKind (the kind of stencil).
 * Values extracted from Windows.Foundation.UniversalApiContract.winmd (Other=0, Ruler=1, Protractor=2).
 */
enum class InkPresenterStencilKind(internal val native: Int) {
    /** Other. */
    OTHER(0),

    /** Ruler. */
    RULER(1),

    /** Protractor. */
    PROTRACTOR(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InkPresenterStencilKind = entries.first { it.native == native }
    }
}

/** Windows.UI.Input.Inking.IInkPresenterStencil: display settings common to the ruler and protractor. */
abstract class WInkPresenterStencil internal constructor(
    /** An owned reference to the default interface (IInkPresenterRuler / IInkPresenterProtractor). */
    internal val native: ComPtr,
) {
    private val lifetime = ComLifetime.adopt(this, native)
    private val stencil: ComPtr by lazy { lifetime.own(native.queryInterface(InkInterop.IID_IInkPresenterStencil)) }

    /** The kind of stencil (Kind). */
    val kind: InkPresenterStencilKind
        get() = InkPresenterStencilKind.of(stencil.getInt(InkInterop.IInkPresenterStencil_get_Kind))

    /** Whether it is shown (IsVisible). */
    var isVisible: Boolean
        get() = stencil.getBool(InkInterop.IInkPresenterStencil_get_IsVisible)
        set(value) = stencil.putBool(InkInterop.IInkPresenterStencil_put_IsVisible, value)

    /** The background color (BackgroundColor). */
    var backgroundColor: WColor
        get() = readColor(stencil, InkInterop.IInkPresenterStencil_get_BackgroundColor)
        set(value) = writeColor(stencil, InkInterop.IInkPresenterStencil_put_BackgroundColor, value)

    /** The foreground color, such as for tick marks (ForegroundColor). */
    var foregroundColor: WColor
        get() = readColor(stencil, InkInterop.IInkPresenterStencil_get_ForegroundColor)
        set(value) = writeColor(stencil, InkInterop.IInkPresenterStencil_put_ForegroundColor, value)

    /** The position and orientation transform (Transform). */
    var transform: com.appkitbox.winui4k.ink.InkTransform
        get() = InkNative.readMatrix(stencil, InkInterop.IInkPresenterStencil_get_Transform)
        set(value) = Ffi.backend.withScope { scope ->
            stencil.call(InkInterop.IInkPresenterStencil_put_Transform, InkNative.matrixValue(scope, value))
        }

    internal fun own(ptr: ComPtr): ComPtr = lifetime.own(ptr)
}

private fun readColor(target: ComPtr, slot: Int): WColor {
    val argb = XamlStructs.getColor(target, slot)
    return WColor(argb[1], argb[2], argb[3], argb[0])
}

private fun writeColor(target: ComPtr, slot: Int, color: WColor) {
    XamlStructs.putColor(target, slot, color.alpha, color.red, color.green, color.blue)
}

/** Windows.UI.Input.Inking.InkPresenterRuler: the ruler. */
class WInkPresenterRuler internal constructor(native: ComPtr) : WInkPresenterStencil(native) {
    private val ruler2: ComPtr by lazy { own(native.queryInterface(InkInterop.IID_IInkPresenterRuler2)) }

    /** The length (Length, in DIP). */
    var length: Double
        get() = native.getDouble(InkInterop.IInkPresenterRuler_get_Length)
        set(value) = native.call(InkInterop.IInkPresenterRuler_put_Length, value)

    /** The width (Width, in DIP). */
    var width: Double
        get() = native.getDouble(InkInterop.IInkPresenterRuler_get_Width)
        set(value) = native.call(InkInterop.IInkPresenterRuler_put_Width, value)

    /** Whether tick marks are shown (AreTickMarksVisible). */
    var areTickMarksVisible: Boolean
        get() = ruler2.getBool(InkInterop.IInkPresenterRuler2_get_AreTickMarksVisible)
        set(value) = ruler2.putBool(InkInterop.IInkPresenterRuler2_put_AreTickMarksVisible, value)

    /** Whether the compass indicating the angle is shown (IsCompassVisible). */
    var isCompassVisible: Boolean
        get() = ruler2.getBool(InkInterop.IInkPresenterRuler2_get_IsCompassVisible)
        set(value) = ruler2.putBool(InkInterop.IInkPresenterRuler2_put_IsCompassVisible, value)
}

/** Windows.UI.Input.Inking.InkPresenterProtractor: the protractor. */
class WInkPresenterProtractor internal constructor(native: ComPtr) : WInkPresenterStencil(native) {
    /** Whether tick marks are shown (AreTickMarksVisible). */
    var areTickMarksVisible: Boolean
        get() = native.getBool(InkInterop.IInkPresenterProtractor_get_AreTickMarksVisible)
        set(value) = native.putBool(InkInterop.IInkPresenterProtractor_put_AreTickMarksVisible, value)

    /** Whether the angle rays are shown (AreRaysVisible). */
    var areRaysVisible: Boolean
        get() = native.getBool(InkInterop.IInkPresenterProtractor_get_AreRaysVisible)
        set(value) = native.putBool(InkInterop.IInkPresenterProtractor_put_AreRaysVisible, value)

    /** Whether the center marker is shown (IsCenterMarkerVisible). */
    var isCenterMarkerVisible: Boolean
        get() = native.getBool(InkInterop.IInkPresenterProtractor_get_IsCenterMarkerVisible)
        set(value) = native.putBool(InkInterop.IInkPresenterProtractor_put_IsCenterMarkerVisible, value)

    /** Whether the angle readout is shown (IsAngleReadoutVisible). */
    var isAngleReadoutVisible: Boolean
        get() = native.getBool(InkInterop.IInkPresenterProtractor_get_IsAngleReadoutVisible)
        set(value) = native.putBool(InkInterop.IInkPresenterProtractor_put_IsAngleReadoutVisible, value)

    /** Whether it can be resized (IsResizable). */
    var isResizable: Boolean
        get() = native.getBool(InkInterop.IInkPresenterProtractor_get_IsResizable)
        set(value) = native.putBool(InkInterop.IInkPresenterProtractor_put_IsResizable, value)

    /** The radius (Radius, in DIP). */
    var radius: Double
        get() = native.getDouble(InkInterop.IInkPresenterProtractor_get_Radius)
        set(value) = native.call(InkInterop.IInkPresenterProtractor_put_Radius, value)

    /** The accent color (AccentColor). */
    var accentColor: WColor
        get() = readColor(native, InkInterop.IInkPresenterProtractor_get_AccentColor)
        set(value) = writeColor(native, InkInterop.IInkPresenterProtractor_put_AccentColor, value)
}

// ======================================================================
// Custom pens, flyout items, and pen settings
// ======================================================================

/**
 * The WinUI 3 InkToolbarCustomPen (experimental in Windows App SDK 2.5): a pen that creates drawing attributes from the
 * color and size selected with a [WInkToolbarCustomPenButton]. Override [createInkDrawingAttributesCore] to decide how
 * they are created (equivalent to inheriting from InkToolbarCustomPen in C# and overriding
 * CreateInkDrawingAttributesCore; implemented with COM aggregation).
 *
 * InkToolbar in the experimental Windows App SDK 2.5.4 does not call custom pens (a WinUI bug), so
 * [WInkToolbar] compensates by recreating the drawing attributes with this pen when a custom pen button is selected.
 *
 * ```kotlin
 * val calligraphy = object : WInkToolbarCustomPen() {
 *     override fun createInkDrawingAttributesCore(color: WColor?, strokeWidth: Double) =
 *         InkDrawingAttributes.builder().color(color ?: WColor.BLACK).size(strokeWidth, strokeWidth * 3)
 *             .penTip(PenTipShape.RECTANGLE).penTipTransform(InkTransform.rotation(Math.PI / 4)).build()
 * }
 * ```
 */
abstract class WInkToolbarCustomPen protected constructor() {
    /** IInkToolbarCustomPen (the COM-aggregated instance). */
    internal val native: ComPtr

    private val lifetime: ComLifetime

    init {
        val outer = KComObject("WinUI4K.InkToolbarCustomPen")
            .addInterface(
                InkInterop.IID_IInkToolbarCustomPenOverrides,
                listOf(
                    // vtbl[6] CreateInkDrawingAttributesCore(this, Brush, double, out InkDrawingAttributes)
                    KComObject.Method(DESC_CREATE_CORE) { args ->
                        val brush = args[1] as Ptr
                        val color = if (brush.isNull) null else colorOf(ComPtr(brush))
                        val attributes = createInkDrawingAttributesCore(color, args[2] as Double)
                        // Pass the creation reference (count 1) as is to out. It is reclaimed by the caller's Release
                        Ffi.backend.memory.putPtr(args[3] as Ptr, 0, InkNative.createDrawingAttributes(attributes).ptr)
                        KComObject.S_OK
                    },
                ),
            )
        val factory = Activation.factory(InkInterop.CLS_InkToolbarCustomPen, InkInterop.IID_IInkToolbarCustomPenFactory)
        try {
            native = Ffi.backend.withScope { scope ->
                val inner = scope.allocate(8)
                val instance = scope.allocate(8)
                factory.call(InkInterop.IInkToolbarCustomPenFactory_CreateInstance, outer.primary, inner, instance)
                outer.innerUnknown = ComPtr(Ffi.backend.memory.getPtr(inner, 0))
                ComPtr(Ffi.backend.memory.getPtr(instance, 0))
            }
        } catch (t: Throwable) {
            outer.release()
            throw t
        } finally {
            factory.release()
        }
        // Tie the creation references of the instance and the outer to the lifetime of this wrapper
        // (when the outer's reference count drops to 0, the inner is released too)
        lifetime = ComLifetime.adopt(this, native, ComPtr(outer.primary))
    }

    /**
     * Creates drawing attributes from the color [color] selected with the pen button (null for a brush that is not a
     * solid color) and the size [strokeWidth] (CreateInkDrawingAttributesCore). InkToolbar calls this every time a
     * color or size is selected.
     */
    protected abstract fun createInkDrawingAttributesCore(color: WColor?, strokeWidth: Double): InkDrawingAttributes

    /**
     * Creates drawing attributes with the color [color] and the size [strokeWidth] (CreateInkDrawingAttributes; calls
     * [createInkDrawingAttributesCore]).
     *
     * In the experimental Windows App SDK 2.5.4, InkToolbarCustomPen.CreateInkDrawingAttributes calls the default
     * implementation directly instead of making a virtual call to the overridden CreateInkDrawingAttributesCore (a WinUI
     * bug), so this calls [createInkDrawingAttributesCore] without going through native code.
     */
    fun createInkDrawingAttributes(color: WColor?, strokeWidth: Double): InkDrawingAttributes =
        createInkDrawingAttributesCore(color, strokeWidth)

    private companion object {
        /** CreateInkDrawingAttributesCore(this, Brush, double, out InkDrawingAttributes) */
        val DESC_CREATE_CORE = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.F64, ArgKind.PTR)
    }
}

/**
 * The WinUI 3 InkToolbarFlyoutItem (experimental in Windows App SDK 2.5): an item placed in the flyout of an InkToolbar
 * button (laid out in [WInkToolbarCustomToolButton.configurationContent] and the like). Derived from [WButtonBase];
 * if [kind] is a check or mutually exclusive item, [isChecked] toggles with each press.
 */
class WInkToolbarFlyoutItem internal constructor(inspectable: ComPtr) : WButtonBase(inspectable) {
    @JvmOverloads
    constructor(text: String = "") : this(
        Activation.composeDefault(InkInterop.CLS_InkToolbarFlyoutItem, InkInterop.IID_IInkToolbarFlyoutItemFactory),
    ) {
        if (text.isNotEmpty()) this.text = text
    }

    private val flyoutItem: ComPtr by lazy { own(inspectable.queryInterface(InkInterop.IID_IInkToolbarFlyoutItem)) }

    /** The event tokens registered by addItemListener (two per listener: Checked / Unchecked). */
    private val itemTokens = ArrayDeque<Pair<Consumer<Boolean>, LongArray>>()
    private val itemListenerAdapters = KotlinListenerAdapters<Consumer<Boolean>>()

    /** The kind of item (Kind). */
    var kind: InkToolbarFlyoutItemKind
        get() = InkToolbarFlyoutItemKind.of(flyoutItem.getInt(InkInterop.IInkToolbarFlyoutItem_get_Kind))
        set(value) = flyoutItem.call(InkInterop.IInkToolbarFlyoutItem_put_Kind, value.native)

    /** Whether it is checked (IsChecked). */
    var isChecked: Boolean
        get() = flyoutItem.getBool(InkInterop.IInkToolbarFlyoutItem_get_IsChecked)
        set(value) = flyoutItem.putBool(InkInterop.IInkToolbarFlyoutItem_put_IsChecked, value)

    /**
     * Like ItemListener: subscribes to changes of the checked state (Checked / Unchecked). The listener receives
     * [isChecked] after the change.
     */
    @JvmSynthetic
    fun addItemListener(listener: (Boolean) -> Unit) {
        val adapter = Consumer<Boolean> { listener(it) }
        addItemListenerForJava(adapter)
        itemListenerAdapters.add(listener, adapter)
    }

    @JvmName("addItemListener")
    fun addItemListenerForJava(listener: Consumer<Boolean>) {
        val tokens = LongArray(ITEM_ADD_SLOTS.size) { i ->
            flyoutItem.addEventHandler(
                "WinUI4K.InkToolbarFlyoutItemHandler",
                InkInterop.IID_InkToolbarFlyoutItemObjectHandler,
                ITEM_ADD_SLOTS[i],
            ) { _, _ -> listener.accept(i == 0) }
        }
        itemTokens.addLast(listener to tokens)
    }

    /** Unsubscribes a listener registered via [addItemListener]. */
    @JvmSynthetic
    fun removeItemListener(listener: (Boolean) -> Unit) {
        val adapter = itemListenerAdapters.remove(listener) ?: return
        removeItemListenerForJava(adapter)
    }

    @JvmName("removeItemListener")
    fun removeItemListenerForJava(listener: Consumer<Boolean>) {
        val index = itemTokens.indexOfLast { it.first === listener }
        if (index < 0) return
        val (_, tokens) = itemTokens.removeAt(index)
        ITEM_REMOVE_SLOTS.forEachIndexed { i, slot -> flyoutItem.removeEventHandler(slot, tokens[i]) }
    }

    private companion object {
        /** In the order Checked / Unchecked (the value passed to listeners is true for Checked). */
        val ITEM_ADD_SLOTS = intArrayOf(InkInterop.IInkToolbarFlyoutItem_add_Checked, InkInterop.IInkToolbarFlyoutItem_add_Unchecked)
        val ITEM_REMOVE_SLOTS = intArrayOf(InkInterop.IInkToolbarFlyoutItem_remove_Checked, InkInterop.IInkToolbarFlyoutItem_remove_Unchecked)
    }
}

/**
 * The WinUI 3 InkToolbarPenConfigurationControl (experimental in Windows App SDK 2.5): the content of a pen button's
 * flyout (color candidates and a size slider). Placing it in [WInkToolbarCustomPenButton.configurationContent] gives
 * the same settings UI as the built-in pens.
 */
class WInkToolbarPenConfigurationControl internal constructor(inspectable: ComPtr) : WControl(inspectable) {
    constructor() : this(
        Activation.composeDefault(
            InkInterop.CLS_InkToolbarPenConfigurationControl,
            InkInterop.IID_IInkToolbarPenConfigurationControlFactory,
        ),
    )

    /** The pen button being configured (PenButton). Null until it is shown in a flyout. */
    val penButton: WInkToolbarPenButton?
        get() = inspectable.getPtrOrNull(InkInterop.IInkToolbarPenConfigurationControl_get_PenButton)
            ?.let { InkToolbarItems.wrapToolButton(it) as? WInkToolbarPenButton }
}

/** Turns a native button (an owned reference to any interface) into a wrapper of the matching kind. */
internal object InkToolbarItems {
    fun wrapToolButton(native: ComPtr): WInkToolbarToolButton {
        try {
            val kind = InkNative.withView(native, InkInterop.IID_IInkToolbarToolButton) {
                InkToolbarTool.of(it.getInt(InkInterop.IInkToolbarToolButton_get_ToolKind))
            }
            return when (kind) {
                InkToolbarTool.BALLPOINT_PEN ->
                    WInkToolbarBallpointPenButton(native.queryInterface(InkInterop.IID_IInkToolbarBallpointPenButton))
                InkToolbarTool.PENCIL -> WInkToolbarPencilButton(native.queryInterface(InkInterop.IID_IInkToolbarPencilButton))
                InkToolbarTool.HIGHLIGHTER ->
                    WInkToolbarHighlighterButton(native.queryInterface(InkInterop.IID_IInkToolbarHighlighterButton))
                InkToolbarTool.ERASER -> WInkToolbarEraserButton(native.queryInterface(InkInterop.IID_IInkToolbarEraserButton))
                InkToolbarTool.CUSTOM_PEN ->
                    WInkToolbarCustomPenButton(native.queryInterface(InkInterop.IID_IInkToolbarCustomPenButton))
                InkToolbarTool.CUSTOM_TOOL ->
                    WInkToolbarCustomToolButton(native.queryInterface(InkInterop.IID_IInkToolbarCustomToolButton))
            }
        } finally {
            native.release()
        }
    }

    fun wrapToggleButton(native: ComPtr): WInkToolbarToggleButton {
        try {
            val kind = InkNative.withView(native, InkInterop.IID_IInkToolbarToggleButton) {
                InkToolbarToggle.of(it.getInt(InkInterop.IInkToolbarToggleButton_get_ToggleKind))
            }
            return when (kind) {
                InkToolbarToggle.RULER -> WInkToolbarRulerButton(native.queryInterface(InkInterop.IID_IInkToolbarRulerButton))
                InkToolbarToggle.CUSTOM ->
                    WInkToolbarCustomToggleButton(native.queryInterface(InkInterop.IID_IInkToolbarCustomToggleButton))
            }
        } finally {
            native.release()
        }
    }

    fun wrapMenuButton(native: ComPtr): WInkToolbarMenuButton {
        try {
            return WInkToolbarStencilButton(native.queryInterface(InkInterop.IID_IInkToolbarStencilButton))
        } finally {
            native.release()
        }
    }
}
