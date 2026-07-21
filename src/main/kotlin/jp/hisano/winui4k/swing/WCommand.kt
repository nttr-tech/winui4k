package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.KComObject
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG

/**
 * Equivalent to Swing's Action: a Kotlin implementation of Microsoft.UI.Xaml.Input.ICommand.
 *
 * When set on WButton.command, [execute] is called on click.
 * Setting [isEnabled] to false fires CanExecuteChanged, so the XAML side re-evaluates
 * CanExecute and disables the button automatically (the same behavior as C#'s ICommand).
 */
class WCommand(
    isEnabled: Boolean = true,
    private val execute: (parameter: String?) -> Unit,
) {
    /** Subscribers to CanExecuteChanged (token -> EventHandler<Object>). */
    private val canExecuteChangedHandlers = LinkedHashMap<Long, ComPtr>()
    private var nextToken = 1L

    var isEnabled: Boolean = isEnabled
        set(value) {
            if (field == value) return
            field = value
            // CanExecuteChanged(this, null) — XAML responds to this by re-evaluating CanExecute
            for (handler in canExecuteChangedHandlers.values) {
                handler.rawCall(3, INVOKE_DESC, comObject.primary, MemorySegment.NULL)
            }
        }

    /**
     * The COM object passed to the XAML side as an ICommand (ButtonBase.put_Command).
     * Unlike a delegate, ICommand is a WinRT interface, so it needs an IInspectable row
     * (inspectable = true, methods starting at vtbl[6]).
     */
    internal val comObject: KComObject = KComObject("WinUI4K.Command")
        .addInterface(
            Abi.IID_ICommand,
            listOf(
                // vtbl[6] add_CanExecuteChanged(this, EventHandler<Object>, out token)
                KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                    val handler = ComPtr(args[1] as MemorySegment)
                    handler.addRef()
                    val token = nextToken++
                    canExecuteChangedHandlers[token] = handler
                    (args[2] as MemorySegment).reinterpret(8).set(JAVA_LONG, 0, token)
                    KComObject.S_OK
                },
                // vtbl[7] remove_CanExecuteChanged(this, token)
                KComObject.Method(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG)) { args ->
                    canExecuteChangedHandlers.remove(args[1] as Long)?.release()
                    KComObject.S_OK
                },
                // vtbl[8] CanExecute(this, parameter, out boolean)
                KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                    (args[2] as MemorySegment).reinterpret(1)
                        .set(JAVA_BYTE, 0, if (this.isEnabled) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] Execute(this, parameter)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    val param = args[1] as MemorySegment
                    execute(
                        if (param.address() == 0L) null else WinRt.unboxString(ComPtr(param)),
                    )
                    KComObject.S_OK
                },
            ),
        )

    private companion object {
        val DESC_THIS_PTR = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
        val DESC_THIS_PTR_PTR = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)

        /** delegate EventHandler<Object>.Invoke(this, sender, args) — vtbl[3] */
        val INVOKE_DESC = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)
    }
}
