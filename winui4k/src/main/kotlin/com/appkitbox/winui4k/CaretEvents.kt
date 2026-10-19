package com.appkitbox.winui4k

/**
 * A shared part that holds the list of CaretListener-like listeners ([WCaretListener]) of one text field and creates
 * events from changes in the caret position.
 * Like [DocumentEvents], it is held by [WTextField] and is designed to be usable by WTextPane and others.
 *
 * - [readCaret]: a function that reads the current [dot, mark]
 * - [subscribe]: called only once, when the first listener is added. It subscribes to the native selection change
 *   notification (TextBox.SelectionChanged) and calls `anchor.get()?.<this part>?.check()` from the handler
 *
 * WinUI's SelectionChanged arrives later in batches (e.g. on the next turn of the message loop), so when the selection is
 * changed programmatically the user calls [check] on the spot (as in Swing, listeners are called right after the change).
 * Notifications for the same position are not reported.
 */
internal class CaretEvents(
    private val owner: WComponent,
    private val readCaret: () -> IntArray,
    private val subscribe: () -> Unit,
) {
    private val listeners = ArrayList<WCaretListener>()
    private var subscribed = false
    private var lastDot = 0
    private var lastMark = 0

    fun add(listener: WCaretListener) {
        if (!subscribed) {
            subscribe()
            subscribed = true
        }
        if (listeners.isEmpty()) {
            val (dot, mark) = readCaret()
            lastDot = dot
            lastMark = mark
        }
        listeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    fun remove(listener: WCaretListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    fun listeners(): Array<WCaretListener> = listeners.toTypedArray()

    /** Notifies the listeners if the current caret position differs from the last reported one. */
    fun check() {
        if (listeners.isEmpty()) return
        val (dot, mark) = readCaret()
        if (dot == lastDot && mark == lastMark) return
        lastDot = dot
        lastMark = mark
        val event = WCaretEvent(owner, dot, mark)
        for (listener in listeners.toList()) listener.caretUpdate(event)
    }
}
