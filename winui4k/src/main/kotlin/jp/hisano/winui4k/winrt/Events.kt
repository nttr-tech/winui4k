package jp.hisano.winui4k.winrt

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.ffi.api.ArgKind
import jp.hisano.winui4k.ffi.api.CallDescriptor
import jp.hisano.winui4k.ffi.api.Ffi
import jp.hisano.winui4k.ffi.api.Ptr
import jp.hisano.winui4k.ffi.api.ValueKind
import jp.hisano.winui4k.ffi.api.withScope

/**
 * Common handling for subscribing to WinRT events (implementing the delegate + calling add_XXX).
 *
 * The delegate is `Invoke(this, sender, args)` with 3 arguments (RoutedEventHandler /
 * SelectionChangedEventHandler / TypedEventHandler<TSender, TArgs> are all the same
 * shape); it's implemented via [KComObject], and this returns the EventRegistrationToken
 * (int64) that `add_XXX`'s out argument produces.
 */
internal fun ComPtr.addEventHandler(
    name: String,
    handlerIid: String,
    addSlot: Int,
    onInvoke: (sender: Ptr, args: Ptr) -> Unit,
): Long {
    val handler = KComObject(name, inspectable = false)
        .addInterface(
            handlerIid,
            listOf(
                // Invoke(this, sender, args) — vtbl[3]
                KComObject.Method(
                    CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR),
                ) { args ->
                    onInvoke(args[1] as Ptr, args[2] as Ptr)
                    KComObject.S_OK
                },
            ),
        )
    return Ffi.backend.withScope { scope ->
        val out = scope.allocate(8) // EventRegistrationToken (int64)
        call(addSlot, handler.primary, out)
        Ffi.backend.memory.getLong(out, 0)
    }
}

/** Unsubscribes the token obtained from [addEventHandler] (remove_XXX). */
internal fun ComPtr.removeEventHandler(removeSlot: Int, token: Long) {
    call(removeSlot, token)
}
