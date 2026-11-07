package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.TextInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.io.IOException
import java.io.Reader
import java.io.Writer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * JTextArea / JTextPane-style: WinUI 3's RichEditBox.
 *
 * Edits multi-line text, and also handles formatted text such as bold and italic. As with JTextPane, formatting is read and written by passing
 * a [WSimpleAttributeSet] to [setCharacterAttributes] / [getCharacterAttributes] and [styledDocument] ([WStyledDocument]),
 * or toggled on the selection with [toggleSelectionBold] / [toggleSelectionItalic]. The whole content is read and written with [text]
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
 * The formatting attributes are only the six of [WStyleConstants] (bold, italic, underline, color, size, font family). Swing's Style (addStyle / getStyle /
 * setLogicalStyle) and paragraph attributes (setParagraphAttributes) are not provided.
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

    /** The listeners added with addTextChangedListener (RichEditBox.TextChanged is subscribed when the first listener is added). */
    private val textChanges = TextChangeNotices(this, ::nativeText, ::subscribeTextChanged)

    /** DocumentListener-style listeners (subscribes to RichEditBox.TextChanging when the first listener is attached). */
    private val documentEvents = DocumentEvents(this, ::nativeText, ::subscribeTextChanging) { textChanges.fire() }

    /**
     * JTextComponent.getDocument-like: the interface to the text of this text pane ([WDocument]). Returns the same one every time
     * (the same as [WTextField.document]; positions and line breaks are the same as [text]). When modified with [WDocument.insertString] / [WDocument.remove],
     * the caret moves the same way as Swing's DefaultCaret.
     */
    val document: WStyledDocument = WStyledDocument(
        object : DocumentHost {
            override val events: DocumentEvents get() = documentEvents

            override fun readText(): String = nativeText()

            override fun write(offset: Int, length: Int, text: String, attributes: WAttributeSet?) =
                writeRange(offset, length, text, attributes)

            override fun afterWrite() = caret.check()
        },
        this,
    )

    /** JTextPane.getStyledDocument-like: the same as [document]. */
    val styledDocument: WStyledDocument get() = document

    /**
     * Replaces [length] characters from [offset] with [inserted] (ITextRange.SetText) and moves the caret and the selection ends the same way as DefaultCaret.
     * If there are [attributes], the inserted text is formatted with them (the same as replace = true).
     */
    private fun writeRange(offset: Int, length: Int, inserted: String, attributes: WAttributeSet?) {
        val format = attributes?.let(CharacterFormat::of)
        val before = this.length
        val (dot, mark) = nativeCaret()
        writable {
            withRange(offset, offset + length) { range ->
                Hstring.use(insertedParagraphs(inserted)) { h -> range.call(XamlInterop.ITextRange_SetText, XamlInterop.TextOptions_None, h) }
            }
        }
        val added = this.length - before + length
        if (format != null && added > 0) writable { withRange(offset, offset + added) { range -> applyFormat(range, format, true) } }
        setDotMark(caretAfterEdit(dot, offset, length, added), caretAfterEdit(mark, offset, length, added))
    }

    /** Places the caret ([dot]) and the other end of the selection ([mark]). If dot is before mark, the moving end of the selection is put on the start side (StartActive). */
    private fun setDotMark(dot: Int, mark: Int) {
        withSelection { range -> range.call(XamlInterop.ITextRange_SetRange, minOf(dot, mark), maxOf(dot, mark)) }
        withTextSelection { selection ->
            val options = selection.getInt(TextInterop.ITextSelection_get_Options)
            val wanted = if (dot < mark) {
                options or TextInterop.SelectionOptions_StartActive
            } else {
                options and TextInterop.SelectionOptions_StartActive.inv()
            }
            if (wanted != options) selection.call(TextInterop.ITextSelection_put_Options, wanted)
        }
    }

    /** CaretListener-style listeners (subscribes to RichEditBox.SelectionChanged when the first listener is attached). */
    private val caret = CaretEvents(this, ::nativeCaret, ::subscribeSelectionChanged)

    /** A shared part ([TextExtent]) that determines the width and height from the number of columns and rows. */
    private val extent = TextExtent(this)

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
            if (document.documentFilter != null) {
                document.replace(0, length, value, null)
                return
            }
            // Append the final paragraph mark ourselves. Otherwise a trailing line break is used as the final paragraph mark and lost (text ending with a line break would lose it)
            val paragraphs = paragraphsOf(value) + "\r"
            documentEvents.replaceAll {
                writable { Hstring.use(paragraphs) { h -> textDocument.call(XamlInterop.ITextDocument_SetText, XamlInterop.TextOptions_None, h) } }
            }
            caret.check()
        }

    /**
     * JTextComponent.getFocusAccelerator / setFocusAccelerator-like: the access key that moves the focus to the input field with Alt and this character
     * (UIElement.AccessKey). The same as [WTextField.focusAccelerator].
     */
    var focusAccelerator: Char
        get() = readFocusAccelerator(this)
        set(value) = writeFocusAccelerator(this, value)

    /**
     * JTextComponent.getSelectionColor / setSelectionColor-like: the background color of the selected text (RichEditBox.SelectionHighlightColor).
     * As with [WTextField.selectionColor], setting null returns to the theme color.
     */
    var selectionColor: WColor?
        get() = selectionHighlight.get()
        set(value) = selectionHighlight.set(value)

    /** Reads and writes the selection background color ([selectionColor]). */
    private val selectionHighlight = SelectionHighlight(
        this,
        inspectable,
        XamlInterop.IRichEditBox_get_SelectionHighlightColor,
        XamlInterop.IRichEditBox_put_SelectionHighlightColor,
        Triple(XamlInterop.CLS_RichEditBox, XamlInterop.IID_IRichEditBoxStatics, XamlInterop.IRichEditBoxStatics_get_SelectionHighlightColorProperty),
    )

    /**
     * JTextComponent.getText(int offs, int len)-like: [length] characters of [text] from [offset] (positions and line breaks are the same as [text]).
     *
     * @throws IllegalArgumentException for negative values, or when the range is outside the string (corresponds to Swing's BadLocationException)
     */
    fun getText(offset: Int, length: Int): String = textRange(text, offset, length)

    /**
     * JTextComponent.read-like: replaces [text] with the string read from [reader] to the end. Line breaks are normalized to LF, as in Swing
     * (CRLF and CR become LF). Formatting is lost. [desc] (the document description in Swing) is not used. [addDocumentListener] listeners are
     * notified the same as when [text] is set. [reader] is not closed.
     */
    @Throws(IOException::class)
    @Suppress("UNUSED_PARAMETER")
    fun read(reader: Reader, desc: Any?) {
        text = readText(reader)
    }

    /** JTextComponent.write-like: writes [text] (line breaks LF) to [writer] and flushes it ([writer] is not closed). Formatting is not written (use [rtfText]). */
    @Throws(IOException::class)
    fun write(writer: Writer) {
        writeText(writer, text)
    }

    /** The number of characters in the whole content (the length of [text]). The upper bound of the insertion position. */
    val length: Int get() = nativeText().length

    /**
     * Turns the text [value] to put in a range into RichEditBox paragraph breaks. A RichEditBox that has never had text does not yet have the final paragraph mark,
     * and when text ending with a line break is put in, that line break becomes the final paragraph mark and disappears from [text], so in that case one paragraph mark is added
     * (a JTextArea-like `append("Line 1\n")` to an empty text pane lost its line break).
     */
    private fun insertedParagraphs(value: String): String {
        val paragraphs = paragraphsOf(value)
        val empty = textDocument.getString(XamlInterop.ITextDocument_GetText, XamlInterop.TextGetOptions_UseLf).isEmpty()
        return if (empty && paragraphs.endsWith('\r')) paragraphs + "\r" else paragraphs
    }

    private fun nativeText(): String {
        val raw = textDocument.getString(XamlInterop.ITextDocument_GetText, XamlInterop.TextGetOptions_UseLf)
        // RichEditBox always returns the final paragraph mark, so remove one to match the string put in
        return if (raw.endsWith("\n") || raw.endsWith("\r")) raw.substring(0, raw.length - 1) else raw
    }

    /**
     * The RTF text of the whole content. Used to save and restore content including formatting.
     * When set, [addDocumentListener] is notified in the same way as when [text] is set.
     *
     * The last `\par` of the RTF is read as the end of the last paragraph (a paragraph mark), as Word does. RichEditBox adds an empty paragraph at the end when reading it as is,
     * so every time the read RTF was set again, a line break was added to the end of [text]. That empty paragraph is removed.
     */
    var rtfText: String
        get() = textDocument.getString(XamlInterop.ITextDocument_GetText, XamlInterop.TextOptions_FormatRtf)
        set(value) {
            documentEvents.replaceAll {
                writable {
                    Hstring.use(value) { h -> textDocument.call(XamlInterop.ITextDocument_SetText, XamlInterop.TextOptions_FormatRtf, h) }
                    if (TRAILING_PARAGRAPH_BREAK.containsMatchIn(value)) removeExtraFinalParagraph()
                }
            }
            caret.check()
        }

    /** Removes the empty paragraph RichEditBox added for the last `\par` of the RTF (the paragraph mark just before the last one). */
    private fun removeExtraFinalParagraph() {
        val raw = textDocument.getString(XamlInterop.ITextDocument_GetText, XamlInterop.TextGetOptions_UseLf)
        val end = raw.length
        if (end < 2 || raw[end - 1] !in PARAGRAPH_MARKS || raw[end - 2] !in PARAGRAPH_MARKS) return
        withRange(end - 2, end - 1) { range ->
            Hstring.use("") { h -> range.call(XamlInterop.ITextRange_SetText, XamlInterop.TextOptions_None, h) }
        }
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
     * Text wrapping (RichEditBox.TextWrapping). After being added to a window, the default is [TextWrapping.WRAP] (from the default style;
     * before that it reads as NoWrap). [lineWrap] reads and writes this in Swing's form.
     *
     * RichEditBox does not support [TextWrapping.WRAP_WHOLE_WORDS], and setting it stops wrapping (the value is only for TextBlock).
     * RichEditBox's [TextWrapping.WRAP] already wraps at word boundaries (it breaks inside a word only when one word does not fit the width), so
     * setting [TextWrapping.WRAP_WHOLE_WORDS] puts [TextWrapping.WRAP] into the native control and sets [wrapStyleWord] to true
     * (it reads back as [TextWrapping.WRAP]).
     */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(XamlInterop.IRichEditBox_get_TextWrapping))
        set(value) {
            val native = if (value == TextWrapping.WRAP_WHOLE_WORDS) {
                wrapStyleWord = true
                TextWrapping.WRAP
            } else {
                value
            }
            inspectable.call(XamlInterop.IRichEditBox_put_TextWrapping, native.native)
        }

    /**
     * JTextArea.getLineWrap / setLineWrap-style: whether lines wrap when they exceed the width. True if [textWrapping]
     * is not [TextWrapping.NO_WRAP].
     * The default after being placed in a window is true (WinUI's default style; this differs from JTextArea, whose
     * default is false). When not wrapping, long lines scroll horizontally.
     */
    var lineWrap: Boolean
        get() = textWrapping != TextWrapping.NO_WRAP
        set(value) {
            textWrapping = if (value) TextWrapping.WRAP else TextWrapping.NO_WRAP
        }

    /**
     * JTextArea.getWrapStyleWord / setWrapStyleWord-like: whether to wrap at word boundaries when wrapping (default false). Returns the set value.
     * RichEditBox wraps at word boundaries whether this is true or false (it breaks inside a word only when one word does not fit the width), and cannot
     * wrap at character boundaries like Swing's false (see [textWrapping]).
     */
    var wrapStyleWord: Boolean = false

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
        if (document.documentFilter != null) {
            document.insertString(end, str, null)
            return
        }
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
        if (document.documentFilter != null) {
            document.insertString(position, str, null)
            return
        }
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
        if (document.documentFilter != null) {
            document.replace(start, end - start, str, null)
            return
        }
        val paragraphs = insertedParagraphs(str.orEmpty())
        documentEvents.replaceRange(start, end - start) {
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
    private fun <T> withSelection(block: (ComPtr) -> T): T = withTextSelection { selection ->
        val range = selection.queryInterface(XamlInterop.IID_ITextRange)
        try {
            block(range)
        } finally {
            range.release()
        }
    }

    /** The current selection [start, end]. The final paragraph mark is not included. */
    private fun nativeSelection(): IntArray = withSelection { range ->
        val last = range.getInt(XamlInterop.ITextRange_get_StoryLength) - 1
        val start = range.getInt(XamlInterop.ITextRange_get_StartPosition).coerceAtMost(last)
        val end = range.getInt(XamlInterop.ITextRange_get_EndPosition).coerceAtMost(last)
        intArrayOf(start, end)
    }

    /**
     * The current [dot, mark]. If the moving end of the selection (StartActive of ITextSelection.Options) is on the start side, dot is the start, otherwise the end
     * (as in Swing, dot is on the caret side even for a selection the user made from back to front).
     */
    private fun nativeCaret(): IntArray {
        val (start, end) = nativeSelection()
        return if (start < end && startActive()) intArrayOf(start, end) else intArrayOf(end, start)
    }

    /** Whether the moving end of the selection is on the start side (StartActive of ITextSelection.Options). */
    private fun startActive(): Boolean = withTextSelection { selection ->
        selection.getInt(TextInterop.ITextSelection_get_Options) and TextInterop.SelectionOptions_StartActive != 0
    }

    /** Passes Document.Selection (ITextSelection) to [block] and always releases it. */
    private fun <T> withTextSelection(block: (ComPtr) -> T): T {
        val selection = textDocument.getPtr(XamlInterop.ITextDocument_get_Selection)
        try {
            return block(selection)
        } finally {
            selection.release()
        }
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
        get() = nativeSelection()[0]
        set(value) = select(value, selectionEnd)

    /**
     * JTextComponent.getSelectionEnd / setSelectionEnd-style: the end of the selection (the character at this position is
     * excluded; the caret position if nothing is selected).
     * As in Swing, setting it is `select(selectionStart, value)`, and setting it before the start makes it the same
     * position as the start (no selection).
     */
    var selectionEnd: Int
        get() = nativeSelection()[1]
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
        setDotMark(end, start)
        caret.check()
    }

    /** JTextComponent.selectAll-style: selects all the text. */
    fun selectAll() {
        select(0, length)
    }

    /**
     * JTextComponent.getCaretPosition / setCaretPosition-like: the caret position (Swing's dot; from 0 to the number of characters).
     * With a selection, as in Swing, it is the moving end of the selection (the caret side): the start for a selection made from back to front (Shift+← or [moveCaretPosition]),
     * otherwise the end (told apart by StartActive of RichEditBox's ITextSelection).
     * Setting it clears the selection and places the caret at that position.
     *
     * Earlier versions returned the end even for a selection made from back to front.
     *
     * @throws IllegalArgumentException if the position is before 0 or after the number of characters (as in Swing)
     */
    var caretPosition: Int
        get() = nativeCaret()[0]
        set(value) {
            require(value in 0..length) { "Cannot place the caret at: $value (length $length)" }
            setDotMark(value, value)
            caret.check()
        }

    /**
     * JTextComponent.moveCaretPosition-like: moves the caret (dot) to [position] while keeping the other end of the selection (mark), and selects the range between them.
     * The RichEditBox selection direction is matched too (moving forward puts the moving end on the start side, and pressing Shift+← then extends the start side, as in Swing).
     *
     * @throws IllegalArgumentException if the position is before 0 or after the number of characters (as in Swing)
     */
    fun moveCaretPosition(position: Int) {
        require(position in 0..length) { "Cannot place the caret at: $position (length $length)" }
        setDotMark(position, nativeCaret()[1])
        caret.check()
    }

    /**
     * JTextComponent.replaceSelection-style: replaces the selection with [content] (inserts it at the caret position if nothing is selected).
     * null or an empty string deletes the selection. After replacing, the caret is placed after the inserted text (the selection is cleared).
     * Does nothing if [isEditable] is false (as in Swing).
     * The [addDocumentListener] listeners receive removeUpdate for the whole selection and then insertUpdate for the inserted text, as in Swing.
     */
    fun replaceSelection(content: String?) {
        if (isReadOnly) return
        if (document.documentFilter != null) {
            document.replace(selectionStart, selectionEnd - selectionStart, content, null)
            return
        }
        val start = selectionStart
        val selected = selectionEnd - start
        val before = length
        val paragraphs = insertedParagraphs(content.orEmpty())
        documentEvents.replaceRange(start, selected) {
            withSelection { range ->
                Hstring.use(paragraphs) { h -> range.call(XamlInterop.ITextRange_SetText, XamlInterop.TextOptions_None, h) }
            }
        }
        val position = start + (length - before + selected)
        setDotMark(position, position)
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
        documentEvents.replaceRange(start, selected) { withSelection { range -> range.call(XamlInterop.ITextRange_Cut) } }
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
        documentEvents.replaceRange(start, selected) { withSelection { range -> range.call(XamlInterop.ITextRange_Paste, 0) } }
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
     * JTextPane.setCharacterAttributes-like: sets the formatting of the selection to [attributes] (the same as [WStyledDocument.setCharacterAttributes];
     * if [replace] is true, the current formatting is reset to the default first). If there is no selection, as with Swing's input attributes,
     * it becomes the formatting of characters typed from now on at the caret position (RichEditBox's selection formatting). If there is a selection, listeners receive changedUpdate.
     * See [WStyleConstants] for the attribute keys and values.
     *
     * @throws IllegalArgumentException if an attribute value has the wrong type
     */
    fun setCharacterAttributes(attributes: WAttributeSet, replace: Boolean) {
        val format = CharacterFormat.of(attributes)
        val start = selectionStart
        val end = selectionEnd
        if (start != end) {
            setCharacterAttributes(start, end - start, attributes, replace)
            return
        }
        writable { withSelection { range -> applyFormat(range, format, replace) } }
    }

    /** The body of [WStyledDocument.setCharacterAttributes]. */
    internal fun setCharacterAttributes(offset: Int, length: Int, attributes: WAttributeSet, replace: Boolean) {
        require(offset >= 0) { "Negative position: $offset" }
        val format = CharacterFormat.of(attributes)
        if (length <= 0) return
        val end = minOf(offset + length, this.length)
        if (end <= offset) return
        writable { withRange(offset, end) { range -> applyFormat(range, format, replace) } }
        documentEvents.changed(offset, end - offset)
    }

    /**
     * JTextPane.getCharacterAttributes-like: the formatting of the character at the caret position (the one character after the caret; at the end, the final paragraph mark).
     * Unlike Swing, all six attributes of [WStyleConstants] (bold, italic, underline, color, size, font family) are always present
     * (the values are the native effective values; the size is converted to DIP and rounded to an integer). Changing the returned collection does not change the text formatting.
     */
    fun getCharacterAttributes(): WAttributeSet = getCharacterAttributes(caretPosition)

    /** The body of [WStyledDocument.getCharacterAttributes]. */
    internal fun getCharacterAttributes(offset: Int): WAttributeSet {
        require(offset in 0..length) { "Position out of range: $offset (length $length)" }
        return withRange(offset, offset + 1) { range -> readFormat(range) }
    }

    /** Puts [format] into the formatting (ITextCharacterFormat) of [range]. If [replace], the document's default formatting is copied first. */
    private fun applyFormat(range: ComPtr, format: CharacterFormat, replace: Boolean) {
        val native = range.getPtr(XamlInterop.ITextRange_get_CharacterFormat)
        try {
            if (replace) {
                val defaults = textDocument.getPtr(TextInterop.ITextDocument_GetDefaultCharacterFormat)
                try {
                    native.call(TextInterop.ITextCharacterFormat_SetClone, defaults)
                } finally {
                    defaults.release()
                }
            }
            format.applyTo(native)
        } finally {
            native.release()
        }
    }

    /** Reads the formatting of [range] as the six attributes of [WStyleConstants]. */
    private fun readFormat(range: ComPtr): WAttributeSet {
        val native = range.getPtr(XamlInterop.ITextRange_get_CharacterFormat)
        try {
            return CharacterFormat.read(native)
        } finally {
            native.release()
        }
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
        documentEvents.changed(start, selected)
    }

    /**
     * Document.addDocumentListener-like: adds a listener called when the text changes. Does nothing if null.
     * As in Swing, it can also be added with `document.addDocumentListener`; both go into the same list.
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
        if (listener != null) documentEvents.add(listener)
    }

    /** Document.removeDocumentListener-style: removes one listener added with [addDocumentListener]. Does nothing if it was not added. */
    fun removeDocumentListener(listener: WDocumentListener?) {
        if (listener != null) documentEvents.remove(listener)
    }

    /** AbstractDocument.getDocumentListeners-style: the DocumentListener-style listeners added (in the order added). */
    val documentListeners: Array<WDocumentListener>
        get() = documentEvents.listeners()

    /** Subscribes to RichEditBox.TextChanging. The handler stays in the native object, so the text pane is obtained from [listenerAnchor]. */
    private fun subscribeTextChanging() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.RichEditDocumentTextChanging",
            XamlInterop.IID_RichEditBoxTextChangingHandler,
            XamlInterop.IRichEditBox_add_TextChanging,
        ) { _, _ ->
            val pane = anchor.get() as? WTextPane
            if (pane != null) pane.documentEvents.onNativeChange(pane.nativeCaret()[0])
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

    /**
     * Subscribes to text changes (RichEditBox.TextChanged). The listener is called when [text] changes.
     * For programmatic changes such as rewriting [text], [replaceSelection] or [document], it is called right at the change, as with Swing's DocumentListener
     * (even before being added to a window; WinUI's TextChanged does not come for changes before that).
     * User edits such as key input are known through TextChanged and arrive after the change (in the message loop).
     * It is not called if the text is the same as the one notified last (format-only changes and changes before adding are not notified).
     * Listeners in the form of Kotlin lambdas and Java Runnables go into the same list and are called in the order added.
     */
    @JvmSynthetic
    fun addTextChangedListener(listener: () -> Unit) = textChanges.add(listener) { listener() }

    @JvmName("addTextChangedListener")
    fun addTextChangedListenerForJava(listener: Runnable) = textChanges.add(listener) { listener.run() }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    @JvmSynthetic
    fun removeTextChangedListener(listener: () -> Unit) = textChanges.remove(listener)

    @JvmName("removeTextChangedListener")
    fun removeTextChangedListenerForJava(listener: Runnable) = textChanges.remove(listener)

    /** Subscribes to RichEditBox.TextChanged. The handler stays on the native side, so the text pane is not captured strongly and is taken from [listenerAnchor]. */
    private fun subscribeTextChanged() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.RichEditTextChangedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IRichEditBox_add_TextChanged,
        ) { _, _ -> (anchor.get() as? WTextPane)?.textChanges?.fire() }
    }

    private companion object {
        /** RichEditBox's paragraph separator is a single CR. CRLF and LF are also normalized to one separator. */
        fun paragraphsOf(value: String): String = value.replace("\r\n", "\r").replace('\n', '\r')

        /** Whether the RTF ends with only closing braces and whitespace after `\par` (end of paragraph) (other control words such as `\pard` are not included). */
        val TRAILING_PARAGRAPH_BREAK = Regex("""\\par(?![a-zA-Z])[\s}\x00]*$""")

        /** The paragraph mark GetText returns (LF with UseLf, otherwise CR). */
        val PARAGRAPH_MARKS = charArrayOf('\n', '\r')
    }
}
