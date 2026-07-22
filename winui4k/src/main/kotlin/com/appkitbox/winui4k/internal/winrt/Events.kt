package com.appkitbox.winui4k.internal.winrt

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.ffi.api.withScope

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
