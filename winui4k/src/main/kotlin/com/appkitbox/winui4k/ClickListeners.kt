package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler

/**
 * A shared part that accepts subscriptions to a button's Click in three forms: [WActionListener] / [Runnable] / Kotlin
 * lambdas. [WButtonBase] (ButtonBase.Click) and [WSplitButton] (SplitButton.Click) each have one.
 *
 * The handler stays in the native object, so it does not capture the button strongly but gets it from [anchor] (see
 * ListenerAnchor). [actionCommandOf] gives the string to put in [WActionEvent.actionCommand] for the button (the one
 * obtained from [anchor]); pass a function that does not capture the button.
 *
 * [events] is the view of the interface that has the event, and [click] is the ABI values of its Click event.
 */
internal class ClickListeners(
    private val events: ComPtr,
    private val click: ClickEvent,
    private val anchor: ListenerAnchor<WComponent>,
    private val actionCommandOf: (WComponent) -> String?,
) {
    /** Mapping from registered listeners to Click event tokens. */
    private val tokens = ListenerTokens<Any>()

    /** Adds a Kotlin lambda that receives a [WActionEvent]. */
    fun addKotlin(listener: (WActionEvent) -> Unit) {
        val adapter = WActionListener(listener)
        add(adapter)
        tokens.addKotlinAdapter(listener, adapter)
    }

    /** Adds a Kotlin lambda in the legacy form that does not receive the event. */
    fun addKotlinWithoutEvent(listener: () -> Unit) {
        val adapter = Runnable(listener)
        add(adapter)
        tokens.addKotlinAdapter(listener, adapter)
    }

    fun add(listener: Runnable) {
        subscribe(listener) { listener.run() }
    }

    fun add(listener: WActionListener) {
        subscribe(listener) { listener.actionPerformed(it) }
    }

    /** Unsubscribes a lambda added with [addKotlin] / [addKotlinWithoutEvent] (does nothing if not registered). */
    fun removeKotlin(listener: Any) {
        remove(tokens.removeKotlinAdapter(listener) ?: return)
    }

    /** Unsubscribes a [Runnable] / [WActionListener] added with [add] (does nothing if not registered). */
    fun remove(listener: Any) {
        val token = tokens.remove(listener) ?: return
        events.removeEventHandler(click.removeSlot, token)
        anchor.listenerRemoved()
    }

    private fun subscribe(key: Any, onClick: (WActionEvent) -> Unit) {
        val anchor = anchor
        val actionCommandOf = actionCommandOf
        val token = events.addEventHandler(click.handlerName, click.handlerIid, click.addSlot) { _, _ ->
            anchor.get()?.let { button ->
                onClick(
                    WActionEvent(
                        button,
                        actionCommandOf(button),
                        WActionEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()),
                    ),
                )
            }
        }
        tokens.add(key, token)
        anchor.listenerAdded()
    }
}

/** ABI values of a Click event: the handler name (for debugging), the IID of the handler type, and the vtable slots of add_Click and remove_Click. */
internal class ClickEvent(val handlerName: String, val handlerIid: String, val addSlot: Int, val removeSlot: Int)

/** Boxes a string and passes it to [slot] (put_CommandParameter) of [view] (null clears it). Common to buttons' CommandParameter. */
internal fun putCommandParameter(view: ComPtr, slot: Int, value: String?) {
    if (value == null) {
        view.call(slot, null)
    } else {
        val boxed = PropertyValues.boxString(value)
        view.call(slot, boxed.ptr)
        boxed.release()
    }
}
