package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * JTextArea / JTextPane-style: WinUI 3's RichEditBox.
 *
 * Edits multi-line text, and also handles formatted text such as bold and italic. Formatting is toggled on the
 * selection with [toggleSelectionBold] / [toggleSelectionItalic]. The whole content is read and written with [text]
 * (plain text) and [rtfText] (RTF format).
 *
 * As with Swing's JTextArea, positions are the number of characters from the start of the text (in UTF-16 chars), and
 * a line break is the single character `"\n"`. [text] has no final paragraph mark, so the string read back matches the
 * string put in.
 * Passing `"\r\n"` or `"\r"` when setting [text] or to [insert] gives a single `"\n"` line break.
 *
 * **Change from earlier versions**: lines in [text] were separated by `"\r"`, and one `"\r"` paragraph mark was also
 * appended at the end. Now lines are separated by `"\n"` as in JTextArea, and no trailing `"\r"` is appended. Code that
 * split the text on `"\r"` should be changed to use `"\n"`.
 *
 * Unlike JTextArea (false), line wrapping ([lineWrap]) defaults to true, WinUI's default as is. RichEditBox's default
 * style sets TextWrapping = Wrap, which takes effect when placed in a window (reading the native value before placing
 * shows NoWrap).
 * To disable wrapping, set `lineWrap = false`.
 *
 * JTextPane's StyledDocument (a sequence of characters with attributes) is not provided. Character formatting is
 * changed with [toggleSelectionBold] and the like on the selection. Changing formatting at a specified position is not
 * supported.
 *
 * Unlike JTextArea(String text), the `(String)` constructor takes the placeholder text ([placeholderText]) (to keep
 * the form from earlier versions). To initialize with text, use `(text, rows, columns)` or set [text].
 */
class WTextPane @JvmOverloads constructor(placeholder: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_RichEditBox, XamlInterop.IID_IRichEditBoxFactory), // default interface = IRichEditBox
) {
    /** JTextArea(int rows, int columns)-style: creates an empty text pane sized [rows] rows by [columns] columns. */
    constructor(rows: Int, columns: Int) : this("") {
        this.rows = rows
        this.columns = columns
    }

    /** JTextArea(String text, int rows, int columns)-style: creates a text pane containing [text], sized [rows] rows by [columns] columns. */
    constructor(text: String, rows: Int, columns: Int) : this("") {
        this.text = text
        this.rows = rows
        this.columns = columns
    }

    /** RichEditBox.Document (Microsoft.UI.Text.ITextDocument). The entry point for text and formatting operations. */
    private val textDocument: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IRichEditBox_get_Document))
    }

    /** TextChanged event tokens registered via addTextChangedListener. */
    private val textChangedTokens = ListenerTokens<Runnable>()

    /** DocumentListener-style listeners (subscribes to RichEditBox.TextChanging when the first listener is attached). */
    private val document = DocumentEvents(this, ::nativeText, ::subscribeTextChanging)

    /** CaretListener-style listeners (subscribes to RichEditBox.SelectionChanged when the first listener is attached). */
    private val caret = CaretEvents(this, ::nativeCaret, ::subscribeSelectionChanged)

    /** A shared part ([TextExtent]) that determines the width and height from the number of columns and rows. */
    private val extent = TextExtent(this)

    /** The value of [wrapStyleWord]. Remembered even while not wrapping, and used when wrapping is enabled. */
    private var wrapByWord = false

    /**
     * The plain text of the whole content (ITextDocument.GetText / SetText). Positions and line breaks are the same as
     * in Swing (see the class description).
     * When set, listeners of [addDocumentListener] receive, as with [WTextField.text], removeUpdate for the whole old
     * text followed by insertUpdate for the whole new text (nothing is delivered if the text is the same). Formatting
     * is lost.
     * Can be set even when read-only ([isEditable] is false), as in JTextArea.
     */
    var text: String
        get() = nativeText()
        set(value) {
            // Append the final paragraph mark ourselves. Otherwise a trailing line break is used as the final paragraph mark and lost (text ending with a line break would lose it)
            val paragraphs = paragraphsOf(value) + "\r"
            document.replaceAll {
                writable { Hstring.use(paragraphs) { h -> textDocument.call(XamlInterop.ITextDocument_SetText, XamlInterop.TextOptions_None, h) } }
            }
            caret.check()
        }

    /** The number of characters in the whole content (the length of [text]). The upper bound of the insertion position. */
    val length: Int get() = nativeText().length

    private fun nativeText(): String {
        val raw = textDocument.getString(XamlInterop.ITextDocument_GetText, XamlInterop.TextGetOptions_UseLf)
        // RichEditBox always returns the final paragraph mark, so remove one to match the string put in
        return if (raw.endsWith("\n") || raw.endsWith("\r")) raw.substring(0, raw.length - 1) else raw
    }

    /**
     * The RTF text of the whole content. Used to save and restore content including formatting.
     * When set, [addDocumentListener] is notified in the same way as when [text] is set.
     */
    var rtfText: String
        get() = textDocument.getString(XamlInterop.ITextDocument_GetText, XamlInterop.TextOptions_FormatRtf)
        set(value) {
            document.replaceAll {
                writable { Hstring.use(value) { h -> textDocument.call(XamlInterop.ITextDocument_SetText, XamlInterop.TextOptions_FormatRtf, h) } }
            }
            caret.check()
        }

    /** The placeholder shown when empty (RichEditBox.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(XamlInterop.IRichEditBox_get_PlaceholderText)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IRichEditBox_put_PlaceholderText, h) }

    /** The header shown above the text pane (RichEditBox.Header). It's an Object, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IRichEditBox_put_Header, boxed.ptr)
            boxed.release()
        }

    /** Whether it is read-only (RichEditBox.IsReadOnly). The inverse of [isEditable]. */
    var isReadOnly: Boolean
        get() = inspectable.getBool(XamlInterop.IRichEditBox_get_IsReadOnly)
        set(value) = inspectable.putBool(XamlInterop.IRichEditBox_put_IsReadOnly, value)

    /**
     * JTextComponent.isEditable / setEditable-style: whether the user can edit the text (default true). The inverse of
     * [isReadOnly]; selection and copying are possible even when false. [replaceSelection] / [cut] / [paste] do nothing
     * while it is false. [text], [append], [insert] and [replaceRange] can change the text even when false, as in
     * JTextArea.
     */
    var isEditable: Boolean
        get() = !isReadOnly
        set(value) {
            isReadOnly = !value
        }

    /**
     * Text wrapping (RichEditBox.TextWrapping). The default after being placed in a window is [TextWrapping.WRAP] (from
     * the default style; it reads as NoWrap before placing). [lineWrap] and [wrapStyleWord] read and write this in
     * Swing's form.
     */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(XamlInterop.IRichEditBox_get_TextWrapping))
        set(value) = inspectable.call(XamlInterop.IRichEditBox_put_TextWrapping, value.native)

    /**
     * JTextArea.getLineWrap / setLineWrap-style: whether lines wrap when they exceed the width. True if [textWrapping]
     * is not [TextWrapping.NO_WRAP].
     * The default after being placed in a window is true (WinUI's default style; this differs from JTextArea, whose
     * default is false). When not wrapping, long lines scroll horizontally.
     */
    var lineWrap: Boolean
        get() = textWrapping != TextWrapping.NO_WRAP
        set(value) {
            textWrapping = wrappingOf(value, wrapByWord)
        }

    /**
     * JTextArea.getWrapStyleWord / setWrapStyleWord-style: whether wrapping happens at word boundaries (character
     * boundaries if false). True if [textWrapping] is [TextWrapping.WRAP_WHOLE_WORDS]. A value set while [lineWrap] is
     * false is used when wrapping is enabled.
     */
    var wrapStyleWord: Boolean
        get() = if (lineWrap) textWrapping == TextWrapping.WRAP_WHOLE_WORDS else wrapByWord
        set(value) {
            wrapByWord = value
            if (lineWrap) textWrapping = wrappingOf(true, value)
        }

    private fun wrappingOf(wrap: Boolean, byWord: Boolean): TextWrapping = when {
        !wrap -> TextWrapping.NO_WRAP
        byWord -> TextWrapping.WRAP_WHOLE_WORDS
        else -> TextWrapping.WRAP
    }

    /**
     * JTextArea.getColumns / setColumns-style: the number of columns that determines the width (default 0, where the
     * width is not determined by columns).
     * As with [WTextField.columns], the width is "columns x the width of 'm' + the left and right padding and border"
     * (DIP), and the width of 'm' is measured with the current [font] (full-width characters such as CJK characters are
     * wider than 'm', so this is an approximation). An explicit [width] takes priority.
     * A value set before being placed is applied when placed in a window (Loaded).
     *
     * @throws IllegalArgumentException if the value is negative (as in Swing)
     */
    var columns: Int
        get() = extent.columns
        set(value) {
            extent.columns = value
        }

    /**
     * JTextArea.getRows / setRows-style: the number of rows that determines the height (default 0, where the height is
     * not determined by rows).
     * The height is "rows x the height of one line + the top and bottom padding and border" (DIP), and the height of one
     * line is measured by drawing 'm' on one line with the current [font]. An explicit [height] takes priority. The
     * header at the top ([header]) is not included.
     * A value set before being placed is applied when placed in a window (Loaded).
     *
     * @throws IllegalArgumentException if the value is negative (as in Swing)
     */
    var rows: Int
        get() = extent.rows
        set(value) {
            extent.rows = value
        }

    override fun fontChanged() = extent.fontChanged()

    override fun preferredSize(): WSize = extent.preferredSize(super.preferredSize())

    init {
        if (placeholder.isNotEmpty()) placeholderText = placeholder
    }

    /** The number of lines separated by line breaks (equivalent to JTextArea.getLineCount). 1 if empty. Visual lines added by wrapping are not counted. */
    val lineCount: Int get() = nativeText().count { it == '\n' } + 1

    /**
     * JTextArea.getLineOfOffset-style: the number (from 0) of the line containing position [offset]. The position at
     * the end of the text is in the last line.
     *
     * @throws IllegalArgumentException if the position is before 0 or after the number of characters
     */
    fun getLineOfOffset(offset: Int): Int {
        val content = nativeText()
        require(offset in 0..content.length) { "Position out of range: $offset (length ${content.length})" }
        return content.substring(0, offset).count { it == '\n' }
    }

    /**
     * JTextArea.getLineStartOffset-style: the start position of line [line] (from 0).
     *
     * @throws IllegalArgumentException if the line number is not at least 0 and less than [lineCount]
     */
    fun getLineStartOffset(line: Int): Int {
        val content = nativeText()
        val starts = lineStarts(content)
        require(line in starts.indices) { "Line number out of range: $line (line count ${starts.size})" }
        return starts[line]
    }

    /**
     * JTextArea.getLineEndOffset-style: the end position of line [line] (from 0). It is the position after the line's
     * trailing line break (the same as the start of the next line), and the number of characters for the last line.
     *
     * @throws IllegalArgumentException if the line number is not at least 0 and less than [lineCount]
     */
    fun getLineEndOffset(line: Int): Int {
        val content = nativeText()
        val starts = lineStarts(content)
        require(line in starts.indices) { "Line number out of range: $line (line count ${starts.size})" }
        return if (line + 1 < starts.size) starts[line + 1] else content.length
    }

    private fun lineStarts(content: String): List<Int> {
        val starts = ArrayList<Int>()
        starts += 0
        content.forEachIndexed { index, char -> if (char == '\n') starts += index + 1 }
        return starts
    }

    /**
     * JTextArea.append-style: appends [str] to the end of the text. Does nothing if null or an empty string.
     * Listeners of [addDocumentListener] receive insertUpdate at the end position.
     */
    fun append(str: String?) {
        if (str.isNullOrEmpty()) return
        val end = length
        replaceRange(str, end, end)
    }

    /**
     * JTextArea.insert-style: inserts [str] at position [position]. Does nothing if null or an empty string (even if the
     * position is out of range).
     *
     * @throws IllegalArgumentException if [str] is given and the position is before 0 or after the number of characters
     *   (as in Swing)
     */
    fun insert(str: String?, position: Int) {
        if (str.isNullOrEmpty()) return
        require(position in 0..length) { "Position out of range: $position (length $length)" }
        replaceRange(str, position, position)
    }

    /**
     * JTextArea.replaceRange-style: replaces the range from [start] up to (but not including) [end] with [str] (removes
     * the range if null or an empty string).
     * Listeners of [addDocumentListener] receive removeUpdate for the range followed by insertUpdate for the inserted
     * text, as in Swing. It can replace even when [isEditable] is false (as in JTextArea).
     *
     * @throws IllegalArgumentException if [end] is before [start], or the range is outside the text (as in Swing)
     */
    fun replaceRange(str: String?, start: Int, end: Int) {
        require(end >= start) { "end before start: $start..$end" }
        if (start == end && str.isNullOrEmpty()) return
        val content = length
        require(start >= 0 && end <= content) { "Range outside the text: $start..$end (length $content)" }
        val paragraphs = paragraphsOf(str.orEmpty())
        document.replaceRange(start, end - start) {
            writable {
                withRange(start, end) { range ->
                    Hstring.use(paragraphs) { h -> range.call(XamlInterop.ITextRange_SetText, XamlInterop.TextOptions_None, h) }
                }
            }
        }
        caret.check()
    }

    /**
     * Makes the document writable only during [block], even when read-only ([isEditable] is false). RichEditBox refuses
     * to modify the Document with E_ACCESSDENIED when read-only, but JTextArea lets the program change the text even
     * when not editable.
     */
    private fun <T> writable(block: () -> T): T {
        if (!isReadOnly) return block()
        isReadOnly = false
        try {
            return block()
        } finally {
            isReadOnly = true
        }
    }

    /** Gets the ITextRange from [start] to [end], passes it to [block], and reliably releases it. */
    private fun <T> withRange(start: Int, end: Int, block: (ComPtr) -> T): T {
        val range = textDocument.getPtr(XamlInterop.ITextDocument_GetRange, start, end)
        try {
            return block(range)
        } finally {
            range.release()
        }
    }

    /** Gets the ITextRange of the selection (Document.Selection), passes it to [block], and reliably releases it. ITextSelection is used after QI. */
    private fun <T> withSelection(block: (ComPtr) -> T): T {
        val selection = textDocument.getPtr(XamlInterop.ITextDocument_get_Selection)
        try {
            val range = selection.queryInterface(XamlInterop.IID_ITextRange)
            try {
                return block(range)
            } finally {
                range.release()
            }
        } finally {
            selection.release()
        }
    }

    /** The current [dot, mark] (if there is a selection, dot is its end and mark its start). The final paragraph mark is not included. */
    private fun nativeCaret(): IntArray = withSelection { range ->
        val last = range.getInt(XamlInterop.ITextRange_get_StoryLength) - 1
        val start = range.getInt(XamlInterop.ITextRange_get_StartPosition).coerceAtMost(last)
        val end = range.getInt(XamlInterop.ITextRange_get_EndPosition).coerceAtMost(last)
        intArrayOf(end, start)
    }

    /** JTextComponent.getSelectedText-style: the selected text. An empty string (not null) if there is no selection. */
    val selectedText: String
        get() = nativeText().substring(selectionStart, selectionEnd)

    /**
     * JTextComponent.getSelectionStart / setSelectionStart-style: the start of the selection (the caret position if
     * there is no selection).
     * As in Swing, setting it does `select(value, selectionEnd)`; setting it after the end moves the end to the same
     * position.
     */
    var selectionStart: Int
        get() = nativeCaret()[1]
        set(value) = select(value, selectionEnd)

    /**
     * JTextComponent.getSelectionEnd / setSelectionEnd-style: the end of the selection (the character at this position is
     * excluded; the caret position if nothing is selected).
     * As in Swing, setting it is `select(selectionStart, value)`, and setting it before the start makes it the same
     * position as the start (no selection).
     */
    var selectionEnd: Int
        get() = nativeCaret()[0]
        set(value) = select(selectionStart, value)

    /**
     * JTextComponent.select-style: selects from [selectionStart] up to just before [selectionEnd] (the character at the
     * end position is excluded).
     * As in Swing, the range is clamped to the string (a negative start becomes 0, positions past the length become the
     * length), and if the end is before the start it becomes the same position as the start (no selection). The caret is
     * placed at the end.
     */
    fun select(selectionStart: Int, selectionEnd: Int) {
        val content = length
        val start = selectionStart.coerceIn(0, content)
        val end = selectionEnd.coerceAtMost(content).coerceAtLeast(start)
        withSelection { range -> range.call(XamlInterop.ITextRange_SetRange, start, end) }
        caret.check()
    }

    /** JTextComponent.selectAll-style: selects all the text. */
    fun selectAll() {
        select(0, length)
    }

    /**
     * JTextComponent.getCaretPosition / setCaretPosition-style: the caret position (from 0 to the number of characters).
     * When there is a selection, it is the end of the selection (RichEditBox does not keep the direction of the
     * selection, so it is not the start of a selection extended backward as in Swing).
     * Setting it clears the selection and places the caret at that position.
     *
     * @throws IllegalArgumentException if the position is before 0 or after the number of characters (as in Swing)
     */
    var caretPosition: Int
        get() = selectionEnd
        set(value) {
            require(value in 0..length) { "Cannot place the caret at: $value (length $length)" }
            withSelection { range -> range.call(XamlInterop.ITextRange_SetRange, value, value) }
            caret.check()
        }

    /**
     * JTextComponent.replaceSelection-style: replaces the selection with [content] (inserts at the caret position if
     * there is no selection). Removes the selection if null or an empty string. Afterwards, the caret is placed after
     * the inserted text (the selection is cleared).
     * Does nothing if [isEditable] is false (as in Swing).
     * Listeners of [addDocumentListener] receive removeUpdate for the whole selection followed by insertUpdate for the
     * inserted text, as in Swing.
     */
    fun replaceSelection(content: String?) {
        if (isReadOnly) return
        val start = selectionStart
        val selected = selectionEnd - start
        val before = length
        val paragraphs = paragraphsOf(content.orEmpty())
        document.replaceRange(start, selected) {
            withSelection { range ->
                Hstring.use(paragraphs) { h -> range.call(XamlInterop.ITextRange_SetText, XamlInterop.TextOptions_None, h) }
            }
        }
        val position = start + (length - before + selected)
        withSelection { range -> range.call(XamlInterop.ITextRange_SetRange, position, position) }
        caret.check()
    }

    /** JTextComponent.copy-style: copies the selection to the clipboard (ITextRange.Copy). Does nothing if there is no selection. */
    fun copy() {
        if (selectionEnd > selectionStart) withSelection { range -> range.call(XamlInterop.ITextRange_Copy) }
    }

    /**
     * JTextComponent.cut-style: moves the selection to the clipboard (ITextRange.Cut). Does nothing if there is no
     * selection or [isEditable] is false.
     * Listeners of [addDocumentListener] receive removeUpdate for the selection.
     */
    fun cut() {
        if (isReadOnly || selectionEnd <= selectionStart) return
        val start = selectionStart
        val selected = selectionEnd - start
        document.replaceRange(start, selected) { withSelection { range -> range.call(XamlInterop.ITextRange_Cut) } }
        caret.check()
    }

    /**
     * JTextComponent.paste-style: replaces the selection with the clipboard content (inserts at the caret position if
     * there is no selection; ITextRange.Paste).
     * Does nothing if [isEditable] is false. Formatted content is inserted with its formatting.
     * Listeners of [addDocumentListener] receive insertUpdate for the inserted text (preceded by removeUpdate for the
     * selection when replacing).
     */
    fun paste() {
        if (isReadOnly) return
        val start = selectionStart
        val selected = selectionEnd - start
        document.replaceRange(start, selected) { withSelection { range -> range.call(XamlInterop.ITextRange_Paste, 0) } }
        caret.check()
    }

    /** Whether there's an edit that can be undone (ITextDocument.CanUndo). */
    val canUndo: Boolean
        get() = textDocument.getBool(XamlInterop.ITextDocument_CanUndo)

    /** Whether there's an undone edit that can be redone (ITextDocument.CanRedo). */
    val canRedo: Boolean
        get() = textDocument.getBool(XamlInterop.ITextDocument_CanRedo)

    /** Undoes the most recent edit (ITextDocument.Undo). */
    fun undo() {
        textDocument.call(XamlInterop.ITextDocument_Undo)
    }

    /** Redoes the most recently undone edit (ITextDocument.Redo). */
    fun redo() {
        textDocument.call(XamlInterop.ITextDocument_Redo)
    }

    /**
     * Toggles bold on the selection (Selection.CharacterFormat.Bold = Toggle).
     * If there is a selection, listeners of [addDocumentListener] receive changedUpdate for the selection.
     */
    fun toggleSelectionBold() {
        withSelectionFormat { format -> format.call(XamlInterop.ITextCharacterFormat_put_Bold, XamlInterop.FormatEffect_Toggle) }
    }

    /**
     * Toggles italic on the selection (Selection.CharacterFormat.Italic = Toggle).
     * If there is a selection, listeners of [addDocumentListener] receive changedUpdate for the selection.
     */
    fun toggleSelectionItalic() {
        withSelectionFormat { format -> format.call(XamlInterop.ITextCharacterFormat_put_Italic, XamlInterop.FormatEffect_Toggle) }
    }

    /** Gets the ITextCharacterFormat of the selection, passes it to the block, and reliably releases it. Then notifies that the formatting changed. */
    private fun withSelectionFormat(block: (ComPtr) -> Unit) {
        val start = selectionStart
        val selected = selectionEnd - start
        withSelection { range ->
            val format = range.getPtr(XamlInterop.ITextRange_get_CharacterFormat)
            try {
                block(format)
            } finally {
                format.release()
            }
        }
        document.changed(start, selected)
    }

    /**
     * Document.addDocumentListener-style: attaches a listener called when the text changes. Does nothing if null.
     * Swing's JTextArea attaches it to the Document from getDocument(), but WinUI4K has no Document, so it is attached
     * directly to the text pane.
     *
     * User edits such as typing, pasting and undo are detected through RichEditBox.TextChanging, and the difference
     * from the previous text is reported as a single removeUpdate (the removed part) and insertUpdate (the inserted
     * part) (same as [WTextField.addDocumentListener]).
     * Changes from the program, such as [text], [append], [insert], [replaceRange] and [replaceSelection], are reported
     * in the form described for each.
     * Formatting changes ([toggleSelectionBold] and the like) are reported with changedUpdate (formatting changes made
     * with keys are not reported).
     */
    fun addDocumentListener(listener: WDocumentListener?) {
        if (listener != null) document.add(listener)
    }

    /** Document.removeDocumentListener-style: removes one listener added with [addDocumentListener]. Does nothing if it was not added. */
    fun removeDocumentListener(listener: WDocumentListener?) {
        if (listener != null) document.remove(listener)
    }

    /** AbstractDocument.getDocumentListeners-style: the DocumentListener-style listeners added (in the order added). */
    val documentListeners: Array<WDocumentListener>
        get() = document.listeners()

    /** Subscribes to RichEditBox.TextChanging. The handler stays in the native object, so the text pane is obtained from [listenerAnchor]. */
    private fun subscribeTextChanging() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.RichEditDocumentTextChanging",
            XamlInterop.IID_RichEditBoxTextChangingHandler,
            XamlInterop.IRichEditBox_add_TextChanging,
        ) { _, _ ->
            val pane = anchor.get() as? WTextPane
            if (pane != null) pane.document.onNativeChange(pane.nativeCaret()[0])
        }
    }

    /**
     * JTextComponent.addCaretListener-style: attaches a listener called when the caret or the selection moves. Does
     * nothing if null.
     * [WCaretEvent.dot] is the caret position (the end of the selection if there is one), and [WCaretEvent.mark] is the
     * other end of the selection (its start).
     * Delivery is the same as [WTextField.addCaretListener] (moves by keys or mouse arrive afterward, and changes from
     * the program arrive right after the change).
     */
    fun addCaretListener(listener: WCaretListener?) {
        if (listener != null) caret.add(listener)
    }

    /** JTextComponent.removeCaretListener-style: removes one listener added with [addCaretListener]. Does nothing if it was not added. */
    fun removeCaretListener(listener: WCaretListener?) {
        if (listener != null) caret.remove(listener)
    }

    /** JTextComponent.getCaretListeners-style: the CaretListener-style listeners added (in the order added). */
    val caretListeners: Array<WCaretListener>
        get() = caret.listeners()

    /** Subscribes to RichEditBox.SelectionChanged. The handler stays in the native object, so the text pane is obtained from [listenerAnchor]. */
    private fun subscribeSelectionChanged() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.RichEditCaretSelectionChanged",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IRichEditBox_add_SelectionChanged,
        ) { _, _ -> (anchor.get() as? WTextPane)?.caret?.check() }
    }

    /** Subscribes to text changes (RichEditBox.TextChanged). */
    @JvmSynthetic
    fun addTextChangedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addTextChangedListenerForJava(adapter)
        textChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addTextChangedListener")
    fun addTextChangedListenerForJava(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.RichEditTextChangedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IRichEditBox_add_TextChanged,
        ) { _, _ -> listener.run() }
        textChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    @JvmSynthetic
    fun removeTextChangedListener(listener: () -> Unit) {
        val adapter = textChangedTokens.removeKotlinAdapter(listener) ?: return
        removeTextChangedListenerForJava(adapter)
    }

    @JvmName("removeTextChangedListener")
    fun removeTextChangedListenerForJava(listener: Runnable) {
        val token = textChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IRichEditBox_remove_TextChanged, token)
    }

    private companion object {
        /** RichEditBox's paragraph separator is a single CR. CRLF and LF are also normalized to one separator. */
        fun paragraphsOf(value: String): String = value.replace("\r\n", "\r").replace('\n', '\r')
    }
}
