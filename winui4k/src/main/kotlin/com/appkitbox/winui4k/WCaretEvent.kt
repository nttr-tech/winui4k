package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * CaretEvent-style: an event that tells a [WCaretListener] that the caret (and the selection) of a text field moved.
 * [getSource] (`source` in Kotlin) and [component] are the text field the listener is attached to.
 * [dot] is the caret position, and [mark] is the other end of the selection, the same as [dot] if there is no
 * selection (as in Swing).
 *
 * WinUI text fields do not report the direction of the selection (whether it was extended backward with Shift + Left),
 * so when there is a selection, [mark] is always its start and [dot] its end (the same shape as Swing's
 * select(start, end)).
 */
class WCaretEvent(
    source: WComponent,
    /** The caret position (equivalent to CaretEvent.getDot). The end of the selection if there is one. */
    val dot: Int,
    /** The other end of the selection (equivalent to CaretEvent.getMark). Its start if there is a selection, otherwise the same as [dot]. */
    val mark: Int,
) : EventObject(source) {
    /** The text field whose caret moved. Same as [getSource]. */
    val component: WComponent get() = source as WComponent

    override fun toString(): String = "WCaretEvent[dot=$dot, mark=$mark, source=$source]"
}

/** CaretListener-style: a listener passed to [WTextField.addCaretListener] and the like (can be written as a lambda in Java). */
fun interface WCaretListener : EventListener {
    /** Called when the caret or the selection moves. */
    fun caretUpdate(e: WCaretEvent)
}
