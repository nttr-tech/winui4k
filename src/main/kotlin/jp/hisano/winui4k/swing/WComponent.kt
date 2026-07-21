package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winui.Abi

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

    /** A uniform margin on all four sides. Passes a Thickness (double×4) by value to put_Margin. */
    var margin: Double = 0.0
        set(value) {
            field = value
            XamlStructs.putThickness(
                frameworkElement, Abi.IFrameworkElement_put_Margin, value, value, value, value,
            )
        }
}
