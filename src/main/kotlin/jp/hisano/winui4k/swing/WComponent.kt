package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winui.Abi

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
 * A thin Swing-like API layer. Everything underneath is a native WinUI 3 control.
 * (Use only inside the WinUiToolkit.launch callback = on the UI thread)
 */
abstract class WComponent internal constructor(
    /** The control's default interface pointer (ITextBox, IButton, ...) */
    internal val inspectable: ComPtr,
) {
    /** IUIElement view used for XAML tree operations. */
    internal val uiElement: ComPtr by lazy { inspectable.queryInterface(Abi.IID_IUIElement) }

    /** FrameworkElement view (also used as an argument to things like Flyout.ShowAt). */
    internal val frameworkElement: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IFrameworkElement)
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

    /** Horizontal position within the space the parent allots (FrameworkElement.HorizontalAlignment). */
    var horizontalAlignment: HorizontalAlignment
        get() = HorizontalAlignment.of(frameworkElement.getInt(Abi.IFrameworkElement_get_HorizontalAlignment))
        set(value) = frameworkElement.call(Abi.IFrameworkElement_put_HorizontalAlignment, value.native)

    /** Vertical position within the space the parent allots (FrameworkElement.VerticalAlignment). */
    var verticalAlignment: VerticalAlignment
        get() = VerticalAlignment.of(frameworkElement.getInt(Abi.IFrameworkElement_get_VerticalAlignment))
        set(value) = frameworkElement.call(Abi.IFrameworkElement_put_VerticalAlignment, value.native)

    /** A uniform margin on all four sides. Passes a Thickness (double×4) by value to put_Margin. */
    var margin: Double = 0.0
        set(value) {
            field = value
            XamlStructs.putThickness(
                frameworkElement, Abi.IFrameworkElement_put_Margin, value, value, value, value,
            )
        }
}
