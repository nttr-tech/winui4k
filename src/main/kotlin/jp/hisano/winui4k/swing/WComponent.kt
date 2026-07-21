package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_DOUBLE
import java.lang.foreign.ValueLayout.JAVA_INT

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

    /** A uniform margin on all four sides. Passes a Thickness (double×4) by value to put_Margin. */
    var margin: Double = 0.0
        set(value) {
            field = value
            Arena.ofConfined().use { a ->
                val t = a.allocate(THICKNESS)
                for (i in 0 until 4) t.setAtIndex(JAVA_DOUBLE, i.toLong(), value)
                frameworkElement.callWith(
                    Abi.IFrameworkElement_put_Margin,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, THICKNESS),
                    t,
                )
            }
        }

    private companion object {
        /** Microsoft.UI.Xaml.Thickness { double Left, Top, Right, Bottom } */
        val THICKNESS: MemoryLayout = MemoryLayout.structLayout(
            JAVA_DOUBLE.withName("Left"),
            JAVA_DOUBLE.withName("Top"),
            JAVA_DOUBLE.withName("Right"),
            JAVA_DOUBLE.withName("Bottom"),
        )
    }
}
