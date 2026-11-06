package com.appkitbox.winui4k

/**
 * The interface through which a [WDocument] rewrites the native text of a text field. Each text field ([WTextField] / [WPasswordField] / [WTextPane])
 * holds one private implementation (so that public classes do not implement an internal interface).
 */
internal interface DocumentHost {
    /** The DocumentListener-like listeners of the text field ([DocumentEvents]). */
    val events: DocumentEvents

    /** The current native text. */
    fun readText(): String

    /**
     * Replaces [length] characters from [offset] with [text] (native only; [WDocument] notifies the listeners through [events]).
     * The range has already been checked. [attributes] is used only by text fields with formatting. The caret moves the same way as Swing's DefaultCaret.
     */
    fun write(offset: Int, length: Int, text: String, attributes: WAttributeSet?)

    /** Called after the change notification (to notify the caret listeners). */
    fun afterWrite()
}

/**
 * Document-like: the interface to the text of one text field ([WTextField.document] / [WPasswordField.document] / [WTextPane.document]).
 * It can be written in the same form as Swing's `field.getDocument().addDocumentListener(...)`, `doc.getLength()`, `doc.getText(0, doc.getLength())`,
 * `doc.insertString(doc.getLength(), s, null)`, `doc.remove(...)` and `((AbstractDocument) doc).setDocumentFilter(...)`.
 * Positions are in UTF-16 chars, as in Swing.
 *
 * Unlike Swing, there is no Document detached from a text field: it is bound to one text field and cannot be shared with another text field
 * or replaced with setDocument. Its content is the text field's native text itself, and changes from either side are the same.
 * Listeners go into the same list as the text field's `addDocumentListener`. There is no Position, Element, UndoableEditListener or
 * putProperty of Swing (undo is the text field's undo). Use from the UI thread.
 *
 * Throws [IllegalArgumentException] instead of Swing's BadLocationException.
 */
open class WDocument internal constructor(internal val host: DocumentHost) {
    /** Document.getLength-like: the number of characters. */
    val length: Int get() = host.readText().length

    /**
     * Document.getText(int offset, int length)-like: [length] characters from [offset].
     *
     * @throws IllegalArgumentException for negative values or a range outside the text (Swing's BadLocationException)
     */
    fun getText(offset: Int, length: Int): String = textRange(host.readText(), offset, length)

    /**
     * AbstractDocument.insertString-like: inserts [str] at [offset]. Does nothing if null or empty.
     * If there is a [documentFilter], it goes through its insertString. Listeners receive insertUpdate.
     * [attributes] is used only by [WTextPane], which has formatting (it formats the inserted text with these attributes; see [WStyledDocument]).
     * It can be modified even if the text field's isEditable is false (the same as Swing).
     *
     * @throws IllegalArgumentException for a position before 0 or after the number of characters
     */
    fun insertString(offset: Int, str: String?, attributes: WAttributeSet?) {
        if (str.isNullOrEmpty()) return
        require(offset <= length) { "Position out of range: $offset (length $length)" }
        val filter = documentFilter
        if (filter != null) filter.insertString(bypass, offset, str, attributes) else bypass.insertString(offset, str, attributes)
    }

    /**
     * AbstractDocument.remove-like: removes [length] characters from [offset]. Does nothing if [length] is 0.
     * If there is a [documentFilter], it goes through its remove. Listeners receive removeUpdate.
     *
     * @throws IllegalArgumentException for a range outside the text
     */
    fun remove(offset: Int, length: Int) {
        val filter = documentFilter
        if (filter != null) filter.remove(bypass, offset, length) else bypass.remove(offset, length)
    }

    /**
     * AbstractDocument.replace-like: replaces [length] characters from [offset] with [text]. Does nothing if [length] is 0 and [text] is empty.
     * If there is a [documentFilter], it goes through its replace. As in Swing, listeners receive removeUpdate and then insertUpdate.
     *
     * @throws IllegalArgumentException for a range outside the text
     */
    fun replace(offset: Int, length: Int, text: String?, attributes: WAttributeSet?) {
        if (length == 0 && text.isNullOrEmpty()) return
        val filter = documentFilter
        if (filter != null) filter.replace(bypass, offset, length, text, attributes) else bypass.replace(offset, length, text, attributes)
    }

    /** Document.addDocumentListener-like: the same as the text field's `addDocumentListener`. Does nothing if null. */
    fun addDocumentListener(listener: WDocumentListener?) {
        if (listener != null) host.events.add(listener)
    }

    /** Document.removeDocumentListener-like: the same as the text field's `removeDocumentListener`. */
    fun removeDocumentListener(listener: WDocumentListener?) {
        if (listener != null) host.events.remove(listener)
    }

    /** AbstractDocument.getDocumentListeners-like: the attached listeners (in the order added). */
    val documentListeners: Array<WDocumentListener>
        get() = host.events.listeners()

    /**
     * AbstractDocument.getDocumentFilter / setDocumentFilter-like: the filter that changes to the text go through before being accepted (null by default).
     * Changes from the program (this Document's [insertString] / [remove] / [replace], setting the text field's text, replaceSelection, etc.)
     * always go through it, as in Swing. Whether user input goes through it depends on the text field (see [WDocumentFilter]).
     */
    var documentFilter: WDocumentFilter? = null
        set(value) {
            field = value
            filterChanged(value)
        }

    /** Received by the text field when [documentFilter] changes (to subscribe to the filter for user input). */
    internal var filterChanged: (WDocumentFilter?) -> Unit = {}

    /** The interface that modifies without going through the filter (DocumentFilter.FilterBypass). Listeners are notified. */
    internal val bypass: WDocumentFilter.FilterBypass = object : WDocumentFilter.FilterBypass() {
        override val document: WDocument get() = this@WDocument

        override fun remove(offset: Int, length: Int) {
            if (length <= 0) return
            val content = this@WDocument.length
            require(offset >= 0 && offset + length <= content) { "Range outside the text: offset=$offset, length=$length (length $content)" }
            write(offset, length, "", null)
        }

        override fun insertString(offset: Int, string: String?, attr: WAttributeSet?) {
            if (string.isNullOrEmpty()) return
            val content = this@WDocument.length
            require(offset in 0..content) { "Position out of range: $offset (length $content)" }
            write(offset, 0, string, attr)
        }

        override fun replace(offset: Int, length: Int, text: String?, attrs: WAttributeSet?) {
            remove(offset, length)
            insertString(offset, text, attrs)
        }
    }

    private fun write(offset: Int, length: Int, text: String, attributes: WAttributeSet?) {
        host.events.replaceRange(offset, length) { host.write(offset, length, text, attributes) }
        host.afterWrite()
    }
}

/**
 * DocumentFilter-like: a filter that checks changes to the text of a [WDocument] before they are accepted, and lets them through, rejects or rewrites them.
 * Subclass it and override [insertString] / [remove] / [replace], and to let a change through, call the method of the same name on [FilterBypass]
 * (the default implementation lets it through as is). Returning without calling it rejects the change. It can be written in the same form as Swing:
 *
 * ```java
 * field.getDocument().setDocumentFilter(new WDocumentFilter() {
 *     public void replace(FilterBypass fb, int offset, int length, String text, WAttributeSet attrs) {
 *         if (text == null || text.chars().allMatch(Character::isDigit)) super.replace(fb, offset, length, text, attrs);
 *     }
 * });
 * ```
 *
 * Only [WTextField] passes user input (keys, paste, cut, IME) through the filter (TextBox.BeforeTextChanging). As in Swing,
 * typing calls replace (with the number of removed characters when typing over a selection), and deleting with Backspace / Delete calls remove.
 * The filter works on a copy of the text before the change, and modifications through [FilterBypass] go into that copy. If the result is the same as the user input,
 * it is accepted as is; otherwise the input is rejected. When it was rewritten (for example to upper case), the result is put into the text field after rejecting
 * (on the next turn of the message loop; listeners receive removeUpdate / insertUpdate then). The text read from [FilterBypass.document]
 * stays as before the change even after being rewritten in the filter, unlike Swing. Text being composed with an IME also goes through on every input.
 * Undo does not go through the filter (the same as Swing).
 *
 * [WPasswordField] (PasswordBox) and [WTextPane] (RichEditBox) have no way in WinUI to reject a change before it happens, so user input
 * does not go through the filter (only changes from the program do).
 */
open class WDocumentFilter {
    /** DocumentFilter.remove-like: called before removing [length] characters from [offset]. Removes them as is by default. */
    open fun remove(fb: FilterBypass, offset: Int, length: Int) = fb.remove(offset, length)

    /** DocumentFilter.insertString-like: called before inserting [string] at [offset]. Inserts it as is by default. */
    open fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: WAttributeSet?) = fb.insertString(offset, string, attr)

    /** DocumentFilter.replace-like: called before replacing [length] characters from [offset] with [text]. Replaces them as is by default. */
    open fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: WAttributeSet?) =
        fb.replace(offset, length, text, attrs)

    /** DocumentFilter.FilterBypass-like: the interface that modifies the document without going through the filter. Called from inside the filter. */
    abstract class FilterBypass {
        /** FilterBypass.getDocument-like: the document to modify. */
        abstract val document: WDocument

        /** FilterBypass.remove-like: removes without going through the filter. */
        abstract fun remove(offset: Int, length: Int)

        /** FilterBypass.insertString-like: inserts without going through the filter. */
        abstract fun insertString(offset: Int, string: String?, attr: WAttributeSet?)

        /** FilterBypass.replace-like: replaces without going through the filter. */
        abstract fun replace(offset: Int, length: Int, text: String?, attrs: WAttributeSet?)
    }
}

/**
 * A [WDocumentFilter.FilterBypass] that puts modifications into a copy of the text before the change, for passing user input through the filter.
 * Range checks are the same as the real one.
 */
internal class SimulatedBypass(override val document: WDocument, original: String) : WDocumentFilter.FilterBypass() {
    private val content = StringBuilder(original)

    /** The text after going through the filter. */
    val result: String get() = content.toString()

    override fun remove(offset: Int, length: Int) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= content.length) { "Range outside the text: offset=$offset, length=$length (length ${content.length})" }
        content.delete(offset, offset + length)
    }

    override fun insertString(offset: Int, string: String?, attr: WAttributeSet?) {
        if (string.isNullOrEmpty()) return
        require(offset in 0..content.length) { "Position out of range: $offset (length ${content.length})" }
        content.insert(offset, string)
    }

    override fun replace(offset: Int, length: Int, text: String?, attrs: WAttributeSet?) {
        remove(offset, length)
        insertString(offset, text, attrs)
    }
}

/** Where position [position] moves when Swing's DefaultCaret removes [removed] characters from [offset] and inserts [inserted] characters. */
internal fun caretAfterEdit(position: Int, offset: Int, removed: Int, inserted: Int): Int {
    var p = position
    if (removed > 0) {
        p = when {
            p >= offset + removed -> p - removed
            p >= offset -> offset
            else -> p
        }
    }
    if (inserted > 0 && p >= offset) p += inserted
    return p
}

/**
 * One user edit (from the text before the change to the text after it) in the form passed to Swing's filter (position, number of removed characters, inserted text).
 * The position is the difference excluding the common prefix and suffix ([TextDiff]); where the same character repeats, it is decided by the caret position after the change [caretAfter]
 * (at the time of TextBox.BeforeTextChanging, TextBox's SelectionStart is already the position after the change).
 */
internal class UserEdit(val offset: Int, val removed: Int, val inserted: String) {
    companion object {
        fun between(old: String, new: String, caretAfter: Int): UserEdit {
            val diff = TextDiff.between(old, new, caretAfter) ?: return UserEdit(0, 0, "")
            return UserEdit(diff.offset, diff.removedLength, new.substring(diff.offset, diff.offset + diff.insertedLength))
        }
    }
}
