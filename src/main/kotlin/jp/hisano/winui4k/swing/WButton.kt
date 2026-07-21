package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG

/**
 * Microsoft.UI.Xaml.Controls.ClickMode (when Click fires).
 * Values extracted from the winmd (Release=0, Press=1, Hover=2).
 */
enum class ClickMode(internal val native: Int) {
    /** Fires when the button is released (default). */
    RELEASE(0),

    /** Fires when the button is pressed down. */
    PRESS(1),

    /** Fires when the pointer moves over the button. */
    HOVER(2),
    ;

    internal companion object {
        fun of(native: Int): ClickMode = entries.first { it.native == native }
    }
}

/**
 * JButton-like: WinUI 3's Button.
 *
 * Covers the full surface Button defines (IButton + Primitives.IButtonBase +
 * ContentControl.Content): [text] / [content], [clickMode], [isPressed], [isPointerOver],
 * [addActionListener] / [removeActionListener] (Click), [command] / [commandParameter], [flyout].
 */
class WButton(text: String = "") : WControl(
    WinRt.composeDefault(Abi.CLS_Button, Abi.IID_IButtonFactory), // default interface = IButton
) {
    private val contentControl: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IContentControl)
    }
    private val buttonBase: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IButtonBase)
    }

    /** Click event tokens registered via addActionListener (used by removeActionListener). */
    private val clickTokens = ArrayDeque<Pair<() -> Unit, Long>>()

    private var contentComponent: WComponent? = null

    /**
     * The button's label string (ContentControl.Content).
     * Content is Object-typed, so the setter passes a boxed string and the getter unboxes it.
     * Returns "" if [content] currently holds a component.
     */
    var text: String
        get() {
            val boxed = contentControl.getPtrOrNull(Abi.IContentControl_get_Content) ?: return ""
            return try {
                WinRt.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            contentComponent = null
            val boxed = WinRt.boxString(value)
            contentControl.call(Abi.IContentControl_put_Content, boxed.ptr)
            boxed.release()
        }

    /** Content for cases other than plain text. Mutually exclusive with text (whichever is set last wins). */
    var content: WComponent?
        get() = contentComponent
        set(value) {
            contentComponent = value
            contentControl.call(
                Abi.IContentControl_put_Content,
                value?.uiElement?.ptr ?: MemorySegment.NULL,
            )
        }

    /** When Click fires (ButtonBase.ClickMode). */
    var clickMode: ClickMode
        get() = ClickMode.of(buttonBase.getInt(Abi.IButtonBase_get_ClickMode))
        set(value) = buttonBase.call(Abi.IButtonBase_put_ClickMode, value.native)

    /** Whether the button is currently pressed down (ButtonBase.IsPressed). */
    val isPressed: Boolean
        get() = buttonBase.getBool(Abi.IButtonBase_get_IsPressed)

    /** Whether the pointer is currently over the button (ButtonBase.IsPointerOver). */
    val isPointerOver: Boolean
        get() = buttonBase.getBool(Abi.IButtonBase_get_IsPointerOver)

    /**
     * Equivalent to Swing's Action (ButtonBase.Command). On click, [WCommand.execute] is
     * invoked with [commandParameter], and the button is disabled whenever WCommand.isEnabled = false.
     */
    var command: WCommand? = null
        set(value) {
            field = value
            buttonBase.call(
                Abi.IButtonBase_put_Command,
                value?.comObject?.primary ?: MemorySegment.NULL,
            )
        }

    /** The argument passed when [command] runs (ButtonBase.CommandParameter). */
    var commandParameter: String? = null
        set(value) {
            field = value
            if (value == null) {
                buttonBase.call(Abi.IButtonBase_put_CommandParameter, MemorySegment.NULL)
            } else {
                val boxed = WinRt.boxString(value)
                buttonBase.call(Abi.IButtonBase_put_CommandParameter, boxed.ptr)
                boxed.release()
            }
        }

    /** The flyout opened by clicking the button (Button.Flyout). */
    var flyout: WFlyout? = null
        set(value) {
            field = value
            inspectable.call(Abi.IButton_put_Flyout, value?.flyoutBase?.ptr ?: MemorySegment.NULL)
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
        val token = Arena.ofConfined().use { a ->
            val out = a.allocate(JAVA_LONG) // EventRegistrationToken (int64)
            buttonBase.call(Abi.IButtonBase_add_Click, handler.primary, out)
            out.get(JAVA_LONG, 0)
        }
        clickTokens.addLast(listener to token)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    fun removeActionListener(listener: () -> Unit) {
        val index = clickTokens.indexOfLast { it.first === listener }
        if (index < 0) return
        val (_, token) = clickTokens.removeAt(index)
        buttonBase.call(Abi.IButtonBase_remove_Click, token)
    }
}
