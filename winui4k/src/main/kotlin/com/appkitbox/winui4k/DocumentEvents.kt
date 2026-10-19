package com.appkitbox.winui4k

/**
 * The list of DocumentListener-style listeners ([WDocumentListener]) of a single text field, and a shared part that builds
 * events from text changes. Owned by [WTextField], and designed so that WPasswordField (PasswordBox.PasswordChanging) and
 * WTextPane (RichEditBox.TextChanging) can use it in the same way. The user connects the following three:
 *
 * - [readText]: a function that reads the current native text (held by the text field wrapper, so it is not captured
 *   from the native handler)
 * - [subscribe]: called only once, when the first listener is added. Subscribes to the native change notification
 *   (TextChanging and so on) and calls `anchor.get()?.<this part>?.onNativeChange(caret)` from the handler (the handler
 *   looks up the wrapper through [WComponent.listenerAnchor] and does not hold a strong reference to it)
 * - Changes from the program: replacing the whole text (setText) rewrites the native object inside [replaceAll], and
 *   replacing the selected range does so inside [replaceRange]. As in Swing, this part reports the REMOVE of the removed
 *   range and the INSERT of the inserted range, and ignores native notifications delivered synchronously during the
 *   rewrite (TextBox.TextChanging is called inside put_Text)
 *
 * User edits such as key input are reported as a single REMOVE and INSERT at one place, based on the difference from the
 * previous text ([TextDiff]).
 */
internal class DocumentEvents(
    private val owner: WComponent,
    private val readText: () -> String,
    private val subscribe: () -> Unit,
) {
    private val listeners = ArrayList<WDocumentListener>()
    private var subscribed = false

    /** The text last reported (seen by the listeners). Tracked only while there is at least one listener. */
    private var lastText = ""

    /** Whether a rewrite from the program is in progress (native notifications during it are ignored). */
    private var applying = false

    fun add(listener: WDocumentListener) {
        if (!subscribed) {
            subscribe()
            subscribed = true
        }
        if (listeners.isEmpty()) lastText = readText()
        listeners += listener
        owner.listenerAnchor.listenerAdded()
    }

    fun remove(listener: WDocumentListener) {
        val index = listeners.indexOfLast { it === listener }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    fun listeners(): Array<WDocumentListener> = listeners.toTypedArray()

    /**
     * Called on a native change notification. Reports the difference from the previous text. [caret] is the caret position
     * after the change (a hint for locating the edit within a run of identical characters; negative if unknown).
     */
    fun onNativeChange(caret: Int) {
        if (applying || listeners.isEmpty()) return
        val text = readText()
        val diff = TextDiff.between(lastText, text, caret) ?: return
        lastText = text
        fire(diff.offset, diff.removedLength, diff.insertedLength)
    }

    /**
     * Rewrites the whole text with [apply] (setText). As with Swing's JTextComponent.setText, reports the REMOVE of the
     * whole old text and then the INSERT of the whole new text. Reports nothing if the text does not change.
     */
    fun replaceAll(apply: () -> Unit) {
        val old = readText()
        write(apply)
        if (listeners.isEmpty()) return
        val text = readText()
        lastText = text
        if (text != old) fire(0, old.length, text.length)
    }

    /**
     * Performs with [apply] a rewrite that replaces [removedLength] characters from [offset] with another string, and
     * reports REMOVE and INSERT (replaceSelection). The number of inserted characters is derived from the text lengths
     * before and after (excluding anything cut off by the maximum length). A side with length 0 is not reported.
     */
    fun replaceRange(offset: Int, removedLength: Int, apply: () -> Unit) {
        val oldLength = readText().length
        write(apply)
        if (listeners.isEmpty()) return
        val text = readText()
        lastText = text
        fire(offset, removedLength, text.length - oldLength + removedLength)
    }

    private fun write(apply: () -> Unit) {
        val wasApplying = applying
        applying = true
        try {
            apply()
        } finally {
            applying = wasApplying
        }
    }

    private fun fire(offset: Int, removedLength: Int, insertedLength: Int) {
        if (removedLength > 0) {
            val event = WDocumentEvent(owner, offset, removedLength, WDocumentEvent.EventType.REMOVE)
            for (listener in listeners.toList()) listener.removeUpdate(event)
        }
        if (insertedLength > 0) {
            val event = WDocumentEvent(owner, offset, insertedLength, WDocumentEvent.EventType.INSERT)
            for (listener in listeners.toList()) listener.insertUpdate(event)
        }
    }
}
