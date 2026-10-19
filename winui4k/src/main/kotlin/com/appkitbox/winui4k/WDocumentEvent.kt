package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * DocumentEvent-style: an event that tells a [WDocumentListener] that the text of a text field has changed.
 * [getSource] (`source` in Kotlin) and [component] are the text field the listener is attached to, [offset] and
 * [length] are the changed range (in UTF-16 chars, as in Swing), and [type] is [EventType.INSERT] (inserted) /
 * [EventType.REMOVE] (removed).
 * By the time the listener is called, the text of the text field has already changed (as in Swing).
 *
 * WinUI4K text fields do not have a Swing Document, so there is no Swing getDocument() (read the text from
 * [component]). When selected text is typed over, and when the text is set by the program ([WTextField.text]), a
 * REMOVE for the removed part arrives followed by an INSERT for the inserted part, as in Swing.
 */
class WDocumentEvent(
    source: WComponent,
    /** The start position of the changed range (equivalent to DocumentEvent.getOffset). */
    val offset: Int,
    /** The number of characters in the changed range (equivalent to DocumentEvent.getLength). */
    val length: Int,
    /** The kind of change (equivalent to DocumentEvent.getType). */
    val type: EventType,
) : EventObject(source) {
    /** The text field whose text changed. Same as [getSource]. */
    val component: WComponent get() = source as WComponent

    /**
     * DocumentEvent.EventType-style: the kind of change to the text. As in Swing, it can be compared with
     * `e.getType() == WDocumentEvent.EventType.INSERT`.
     * [CHANGE] is a change of character attributes (formatting) in Swing, and is not delivered for text fields without
     * formatting.
     */
    enum class EventType {
        /** Characters were inserted (insertUpdate). */
        INSERT,

        /** Characters were removed (removeUpdate). */
        REMOVE,

        /** Character attributes changed (changedUpdate). */
        CHANGE,
    }

    override fun toString(): String = "WDocumentEvent[type=$type, offset=$offset, length=$length, source=$source]"
}

/**
 * DocumentListener-style: a listener passed to [WTextField.addDocumentListener] and the like.
 * Like Swing's DocumentListener it has three methods, and as in Swing there is no Adapter (leave unused methods empty).
 *
 * As in Swing, changing the text field's text from inside a listener (setting [WTextField.text], replaceSelection and
 * the like) throws IllegalStateException("Attempt to mutate in notification"). Make changes after the notification
 * (such as in another event).
 */
interface WDocumentListener : EventListener {
    /** Called when characters are inserted ([WDocumentEvent.type] is [WDocumentEvent.EventType.INSERT]). */
    fun insertUpdate(e: WDocumentEvent)

    /** Called when characters are removed ([WDocumentEvent.type] is [WDocumentEvent.EventType.REMOVE]). */
    fun removeUpdate(e: WDocumentEvent)

    /** Called when character attributes change. Not called for text fields without formatting (such as [WTextField]). */
    fun changedUpdate(e: WDocumentEvent)
}
