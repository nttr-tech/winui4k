package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Common base for commands that can be set on ButtonBase.Command / MenuFlyoutItem.Command
 * and the like (the Kotlin-implemented [WCommand], and the WinUI-side WXamlUICommand /
 * WStandardUICommand).
 */
abstract class WCommandBase internal constructor() {
    /** The ICommand pointer passed to put_Command. */
    internal abstract val commandPtr: Ptr
}

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
) : WCommandBase() {
    override val commandPtr: Ptr
        get() = comObject.primary

    /** Subscribers to CanExecuteChanged (token -> EventHandler<Object>). */
    private val canExecuteChangedHandlers = LinkedHashMap<Long, ComPtr>()
    private var nextToken = 1L

    var isEnabled: Boolean = isEnabled
        set(value) {
            if (field == value) return
            field = value
            // CanExecuteChanged(this, null) — XAML responds to this by re-evaluating CanExecute
            for (handler in canExecuteChangedHandlers.values) {
                handler.rawCall(3, INVOKE_DESC, comObject.primary, null)
            }
        }

    /**
     * The COM object passed to the XAML side as an ICommand (ButtonBase.put_Command).
     * Unlike a delegate, ICommand is a WinRT interface, so it needs an IInspectable row
     * (inspectable = true, methods starting at vtbl[6]).
     */
    internal val comObject: KComObject = KComObject("WinUI4K.Command")
        .addInterface(
            XamlInterop.IID_ICommand,
            listOf(
                // vtbl[6] add_CanExecuteChanged(this, EventHandler<Object>, out token)
                KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                    val handler = ComPtr(args[1] as Ptr)
                    handler.addRef()
                    val token = nextToken++
                    canExecuteChangedHandlers[token] = handler
                    Ffi.backend.memory.putLong(args[2] as Ptr, 0, token)
                    KComObject.S_OK
                },
                // vtbl[7] remove_CanExecuteChanged(this, token)
                KComObject.Method(DESC_THIS_I64) { args ->
                    canExecuteChangedHandlers.remove(args[1] as Long)?.release()
                    KComObject.S_OK
                },
                // vtbl[8] CanExecute(this, parameter, out boolean)
                KComObject.Method(DESC_THIS_PTR_PTR) { args ->
                    Ffi.backend.memory.putByte(args[2] as Ptr, 0, if (this.isEnabled) 1 else 0)
                    KComObject.S_OK
                },
                // vtbl[9] Execute(this, parameter)
                KComObject.Method(DESC_THIS_PTR) { args ->
                    val param = args[1] as Ptr
                    execute(
                        if (param.isNull) null else PropertyValues.unboxString(ComPtr(param)),
                    )
                    KComObject.S_OK
                },
            ),
        )

    private companion object {
        val DESC_THIS_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
        val DESC_THIS_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
        val DESC_THIS_I64 = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.I64)

        /** delegate EventHandler<Object>.Invoke(this, sender, args) — vtbl[3] */
        val INVOKE_DESC = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
    }
}
