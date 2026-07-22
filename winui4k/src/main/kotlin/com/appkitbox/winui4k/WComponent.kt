package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi
import com.appkitbox.winui4k.internal.winui.XamlStructs

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
        fun of(native: Int): HorizontalAlignment = entries.first { it.native == native }
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
    internal val uiElement: ComPtr by lazy { own(inspectable.queryInterface(Abi.IID_IUIElement)) }

    /** FrameworkElement view (also used as an argument to things like Flyout.ShowAt). */
    internal val frameworkElement: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_IFrameworkElement))
    }

    /** DependencyObject view (passed as the target of attached properties like ToolTipService). */
    internal val dependencyObject: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_IDependencyObject))
    }

    var width: Double = Double.NaN
        set(value) {
            field = value
            frameworkElement.call(Abi.IFrameworkElement_put_Width, value)
        }

    var height: Double = Double.NaN
        set(value) {
            field = value
            frameworkElement.call(Abi.IFrameworkElement_put_Height, value)
        }

    /**
     * Upper bound on the width (FrameworkElement.MaxWidth). Unlike [width], this is measured against the
     * content's natural width and only clamps it once it exceeds this value (unlimited by default).
     */
    var maxWidth: Double = Double.POSITIVE_INFINITY
        set(value) {
            field = value
            frameworkElement.call(Abi.IFrameworkElement_put_MaxWidth, value)
        }

    /** Horizontal position within the space the parent allots (FrameworkElement.HorizontalAlignment). */
    var horizontalAlignment: HorizontalAlignment
        get() = HorizontalAlignment.of(frameworkElement.getInt(Abi.IFrameworkElement_get_HorizontalAlignment))
        set(value) = frameworkElement.call(Abi.IFrameworkElement_put_HorizontalAlignment, value.native)

    /** Vertical position within the space the parent allots (FrameworkElement.VerticalAlignment). */
    var verticalAlignment: VerticalAlignment
        get() = VerticalAlignment.of(frameworkElement.getInt(Abi.IFrameworkElement_get_VerticalAlignment))
        set(value) = frameworkElement.call(Abi.IFrameworkElement_put_VerticalAlignment, value.native)

    /** A uniform margin on all four sides. Passes a Thickness (double×4) by value to put_Margin. Use [setMargin] to set each side individually. */
    var margin: Double = 0.0
        set(value) {
            field = value
            setMargin(value, value, value, value)
        }

    /** Sets the margin on each side individually (FrameworkElement.Margin). */
    fun setMargin(left: Double, top: Double, right: Double, bottom: Double) {
        XamlStructs.putThickness(frameworkElement, Abi.IFrameworkElement_put_Margin, left, top, right, bottom)
    }

    /** Opacity, from 0.0 (transparent) to 1.0 (opaque, the default) (UIElement.Opacity). */
    var opacity: Double
        get() = uiElement.getDouble(Abi.IUIElement_get_Opacity)
        set(value) = uiElement.call(Abi.IUIElement_put_Opacity, value)

    /** Whether this is shown (UIElement.Visibility). false is Collapsed (doesn't reserve layout space either). */
    var isVisible: Boolean
        get() = uiElement.getInt(Abi.IUIElement_get_Visibility) == 0 // Visibility.Visible
        set(value) = uiElement.call(Abi.IUIElement_put_Visibility, if (value) 0 else 1)

    /**
     * The theme applied to this element and everything below it (FrameworkElement.RequestedTheme).
     * Setting it on the root element makes it the whole app's theme. [ElementTheme.DEFAULT] follows the OS setting.
     */
    var requestedTheme: ElementTheme
        get() = ElementTheme.of(frameworkElement.getInt(Abi.IFrameworkElement_get_RequestedTheme))
        set(value) = frameworkElement.call(Abi.IFrameworkElement_put_RequestedTheme, value.native)

    /**
     * The theme actually in effect (FrameworkElement.ActualTheme).
     * Returns the resolved LIGHT / DARK even when [requestedTheme] is [ElementTheme.DEFAULT].
     */
    val actualTheme: ElementTheme
        get() = ElementTheme.of(frameworkElement.getInt(Abi.IFrameworkElement_get_ActualTheme))

    /** Event tokens registered via addActualThemeChangedListener. */
    private val actualThemeChangedTokens = ListenerTokens<() -> Unit>()

    /** Subscribes to changes in the applied theme (FrameworkElement.ActualThemeChanged). */
    fun addActualThemeChangedListener(listener: () -> Unit) {
        val token = frameworkElement.addEventHandler(
            "WinUI4K.ActualThemeChangedHandler",
            Abi.IID_ActualThemeChangedHandler,
            Abi.IFrameworkElement_add_ActualThemeChanged,
        ) { _, _ -> listener() }
        actualThemeChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addActualThemeChangedListener]. */
    fun removeActualThemeChangedListener(listener: () -> Unit) {
        val token = actualThemeChangedTokens.remove(listener) ?: return
        frameworkElement.removeEventHandler(Abi.IFrameworkElement_remove_ActualThemeChanged, token)
    }

    /** Event tokens registered via addSizeChangedListener. */
    private val sizeChangedTokens = ListenerTokens<() -> Unit>()

    /** Subscribes to post-layout size changes (FrameworkElement.SizeChanged). */
    fun addSizeChangedListener(listener: () -> Unit) {
        val token = frameworkElement.addEventHandler(
            "WinUI4K.SizeChangedHandler",
            Abi.IID_SizeChangedEventHandler,
            Abi.IFrameworkElement_add_SizeChanged,
        ) { _, _ -> listener() }
        sizeChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSizeChangedListener]. */
    fun removeSizeChangedListener(listener: () -> Unit) {
        val token = sizeChangedTokens.remove(listener) ?: return
        frameworkElement.removeEventHandler(Abi.IFrameworkElement_remove_SizeChanged, token)
    }

    /** Event tokens registered via addLoadedListener. */
    private val loadedTokens = ListenerTokens<() -> Unit>()

    /**
     * Subscribes to the moment the element is added to the visual tree and ready to render
     * (FrameworkElement.Loaded). Useful for initializing parts that are only reachable after the
     * template is applied (e.g. ScrollView.ScrollPresenter).
     */
    fun addLoadedListener(listener: () -> Unit) {
        val token = frameworkElement.addEventHandler(
            "WinUI4K.LoadedHandler",
            Abi.IID_RoutedEventHandler,
            Abi.IFrameworkElement_add_Loaded,
        ) { _, _ -> listener() }
        loadedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addLoadedListener]. */
    fun removeLoadedListener(listener: () -> Unit) {
        val token = loadedTokens.remove(listener) ?: return
        frameworkElement.removeEventHandler(Abi.IFrameworkElement_remove_Loaded, token)
    }

    /**
     * The string hint shown on hover (ToolTipService.ToolTip). Equivalent to JComponent.setToolTipText.
     * null removes it. Use [setToolTip] if you need to specify placement or a non-string hint.
     */
    var toolTip: String? = null
        set(value) {
            field = value
            if (value == null) {
                toolTipServiceStatics.call(Abi.IToolTipServiceStatics_SetToolTip, dependencyObject, null)
            } else {
                val boxed = PropertyValues.boxString(value)
                toolTipServiceStatics.call(
                    Abi.IToolTipServiceStatics_SetToolTip, dependencyObject, boxed.ptr,
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
            Abi.IToolTipServiceStatics_SetToolTip, dependencyObject, toolTip.inspectable.ptr,
        )
    }

    /** The context menu opened by right-click / long-press (UIElement.ContextFlyout). */
    var contextFlyout: WFlyoutBase? = null
        set(value) {
            field = value
            uiElement.call(
                Abi.IUIElement_put_ContextFlyout,
                value?.flyoutBase?.ptr,
            )
        }

    /**
     * Adds a keyboard shortcut (UIElement.KeyboardAccelerators).
     * While this element is visible, pressing [key] + [modifiers] triggers the equivalent of a click.
     */
    fun addKeyboardAccelerator(key: VirtualKey, vararg modifiers: VirtualKeyModifier) {
        val accelerator = createKeyboardAccelerator(key, modifiers)
        val accelerators = uiElement.getPtr(Abi.IUIElement_get_KeyboardAccelerators)
        accelerators.call(Abi.IVector_Append, accelerator.ptr)
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
        Activation.composeDefault(Abi.CLS_KeyboardAccelerator, Abi.IID_IKeyboardAcceleratorFactory)
    accelerator.call(Abi.IKeyboardAccelerator_put_Key, key.native)
    val combined = modifiers.fold(0) { acc, modifier -> acc or modifier.native }
    if (combined != 0) accelerator.call(Abi.IKeyboardAccelerator_put_Modifiers, combined)
    return accelerator
}
