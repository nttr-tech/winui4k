package com.appkitbox.winui4k

/**
 * A shared part holding the list of text change listeners of text fields ([WTextField.addTextChangedListener], [WPasswordField.addPasswordChangedListener],
 * [WTextPane.addTextChangedListener]) and deciding when to notify them.
 *
 * WinUI's TextChanged and PasswordChanged do not come for text changes before being added to the screen (Loaded), and even after that, programmatic changes
 * arrive after the change (in the message loop). Swing's DocumentListener is called right inside setText, so after a programmatic rewrite
 * (one going through [DocumentEvents]) [fire] is called right away. Native events arriving later also call [fire], but
 * nothing is notified if the text is the same as the one notified last (no double calls). Format-only changes (RichEditBox) are not notified either because the text is the same.
 *
 * - [readText]: a function that reads the current text
 * - [subscribe]: called only once when the first listener is added. Subscribes to the native event, and the handler gets the wrapper
 *   through [WComponent.listenerAnchor] and calls [fire] (it does not capture the wrapper strongly)
 */
internal class TextChangeNotices(
    private val owner: WComponent,
    private val readText: () -> String,
    private val subscribe: () -> Unit,
) {
    /** The added listeners (the added object and a function taking the text). Called in the order added. */
    private val listeners = ArrayList<Pair<Any, (String) -> Unit>>()

    private var subscribed = false

    /** The text notified last (the one the listeners saw). Starts with the text when the first listener is added. */
    private var lastNotified = ""

    fun add(key: Any, invoke: (String) -> Unit) {
        if (!subscribed) {
            subscribe()
            subscribed = true
        }
        // Changes before adding are not notified (as in Swing; native events arriving later are not notified either because the text is the same)
        if (listeners.isEmpty()) lastNotified = readText()
        listeners += key to invoke
        owner.listenerAnchor.listenerAdded()
    }

    /** Removes one listener added with [key] (the last one added). Does nothing if none was added. */
    fun remove(key: Any) {
        val index = listeners.indexOfLast { it.first === key }
        if (index < 0) return
        listeners.removeAt(index)
        owner.listenerAnchor.listenerRemoved()
    }

    /** Calls the listeners in the order added if the current text differs from the one notified last. */
    fun fire() {
        if (listeners.isEmpty()) return
        val current = readText()
        if (current == lastNotified) return
        lastNotified = current
        for ((_, invoke) in listeners.toList()) invoke(current)
    }
}
