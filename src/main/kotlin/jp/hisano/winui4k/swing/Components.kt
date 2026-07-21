package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.Hstring
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.winrt.WinRt
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

    private val frameworkElement: ComPtr by lazy {
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

/** JTextField-like: WinUI 3's TextBox. */
class WTextField(placeholder: String = "") : WComponent(
    WinRt.composeDefault(Abi.CLS_TextBox, Abi.IID_ITextBoxFactory)
        .queryInterface(Abi.IID_ITextBox),
) {
    init {
        if (placeholder.isNotEmpty()) {
            Hstring.use(placeholder) { h -> inspectable.call(Abi.ITextBox_put_PlaceholderText, h) }
        }
    }

    var text: String
        get() = inspectable.getString(Abi.ITextBox_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.ITextBox_put_Text, h) }
}

/** JButton-like: WinUI 3's Button. */
class WButton(text: String = "") : WComponent(
    WinRt.composeDefault(Abi.CLS_Button, Abi.IID_IButtonFactory),
) {
    private val contentControl: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IContentControl)
    }
    private val buttonBase: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IButtonBase)
    }

    var text: String = ""
        set(value) {
            field = value
            val boxed = WinRt.boxString(value) // Content is Object-typed, so box the string
            contentControl.call(Abi.IContentControl_put_Content, boxed.ptr)
            boxed.release()
        }

    init {
        if (text.isNotEmpty()) this.text = text
    }

    /** ActionListener-like. Subscribes to ButtonBase.Click (RoutedEventHandler) under the hood. */
    fun addActionListener(listener: () -> Unit) {
        val handler = KComObject("WinUI4K.ClickHandler", inspectable = false)
            .addInterface(
                Abi.IID_RoutedEventHandler,
                listOf(
                    // Invoke(this, IInspectable sender, RoutedEventArgs e) — vtbl[3]
                    KComObject.Method(
                        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
                    ) {
                        listener()
                        KComObject.S_OK
                    },
                ),
            )
        Arena.ofConfined().use { a ->
            val token = a.allocate(8) // EventRegistrationToken (int64)
            buttonBase.call(Abi.IButtonBase_add_Click, handler.primary, token)
        }
    }
}

/** JPanel-like with a vertical BoxLayout: WinUI 3's StackPanel. */
class WPanel(spacing: Double = 0.0) : WComponent(
    WinRt.composeDefault(Abi.CLS_StackPanel, Abi.IID_IStackPanelFactory)
        .queryInterface(Abi.IID_IStackPanel),
) {
    private val children: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IPanel)
            .getPtr(Abi.IPanel_get_Children) // IVector<UIElement>
    }

    init {
        if (spacing > 0) inspectable.call(Abi.IStackPanel_put_Spacing, spacing)
    }

    fun add(component: WComponent) {
        // IVector<UIElement>.Append(UIElement) — the actual IID is a SHA-1 computed
        // Abi.IID_IVector_UIElement, but since we already hold a pointer of the correct
        // type, we can just call vtbl[13] directly
        children.call(Abi.IVector_Append, component.uiElement.ptr)
    }
}

/** JFrame-like: WinUI 3's Window. */
class WFrame(title: String = "") {
    private val window: ComPtr =
        WinRt.composeDefault(Abi.CLS_Window, Abi.IID_IWindowFactory) // IWindow

    /** The root panel corresponding to JFrame's contentPane. */
    val contentPane: WPanel = WPanel(spacing = 12.0).also { it.margin = 24.0 }

    var title: String = ""
        set(value) {
            field = value
            Hstring.use(value) { h -> window.call(Abi.IWindow_put_Title, h) }
        }

    init {
        window.call(Abi.IWindow_put_Content, contentPane.uiElement.ptr)
        if (title.isNotEmpty()) this.title = title
    }

    fun add(component: WComponent) = contentPane.add(component)

    /** Setting this to true shows (activates) the window. */
    var isVisible: Boolean = false
        set(value) {
            field = value
            if (value) window.call(Abi.IWindow_Activate) else window.call(Abi.IWindow_Close)
        }
}
