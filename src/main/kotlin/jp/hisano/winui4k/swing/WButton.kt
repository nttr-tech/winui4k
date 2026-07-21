package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT

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
