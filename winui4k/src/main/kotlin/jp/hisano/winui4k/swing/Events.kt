package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.KComObject
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG

/**
 * Common plumbing for subscribing to a WinRT event (implementing the delegate + calling `add_XXX`).
 *
 * The delegate is the common 3-argument `Invoke(this, sender, args)` shape (RoutedEventHandler /
 * SelectionChangedEventHandler / TypedEventHandler<TSender, TArgs> all share this shape); it's
 * implemented via [KComObject], and the `out` EventRegistrationToken (int64) argument of `add_XXX`
 * is returned.
 */
internal fun ComPtr.addEventHandler(
    name: String,
    handlerIid: String,
    addSlot: Int,
    onInvoke: (sender: MemorySegment, args: MemorySegment) -> Unit,
): Long {
    val handler = KComObject(name, inspectable = false)
        .addInterface(
            handlerIid,
            listOf(
                // Invoke(this, sender, args) — vtbl[3]
                KComObject.Method(
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
                ) { args ->
                    onInvoke(args[1] as MemorySegment, args[2] as MemorySegment)
                    KComObject.S_OK
                },
            ),
        )
    return Arena.ofConfined().use { a ->
        val out = a.allocate(JAVA_LONG) // EventRegistrationToken (int64)
        call(addSlot, handler.primary, out)
        out.get(JAVA_LONG, 0)
    }
}

/** Unsubscribes a token obtained from [addEventHandler] (remove_XXX). */
internal fun ComPtr.removeEventHandler(removeSlot: Int, token: Long) {
    call(removeSlot, token)
}

/**
 * A "listener → event token" mapping. Removes one entry at a time, last-added first
 * (so the same listener can be added more than once and removed independently).
 */
internal class ListenerTokens<L : Any> {
    private val tokens = ArrayDeque<Pair<L, Long>>()

    fun add(listener: L, token: Long) {
        tokens.addLast(listener to token)
    }

    /** Removes and returns one token matching [listener]. Returns null if none is registered. */
    fun remove(listener: L): Long? {
        val index = tokens.indexOfLast { it.first === listener }
        if (index < 0) return null
        return tokens.removeAt(index).second
    }
}
