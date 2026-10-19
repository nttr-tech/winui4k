package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * JTextField-like: WinUI 3's TextBox.
 *
 * Enabling [acceptsReturn] and [textWrapping] turns it into a JTextArea-like multi-line input.
 * [addTextChangedListener] (TextChanged) delivers the current text on every change.
 * Pressing Enter with no modifier keys in a single-line field ([acceptsReturn] = false) calls the listeners from
 * [addActionListener]; if there are no listeners, the default button of the same window ([WFrame.defaultButton]) is
 * pressed (same as JTextField).
 */
class WTextField @JvmOverloads constructor(placeholder: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_TextBox, XamlInterop.IID_ITextBoxFactory), // default interface = ITextBox
) {
    /** JTextField(int columns)-style: creates a field [columns] columns wide. */
    constructor(columns: Int) : this("") {
        this.columns = columns
    }

    /** Listeners registered with addTextChangedListener (the registered object and a function receiving the text). Called in registration order. */
    private val textChangedListeners = ArrayList<Pair<Any, (String) -> Unit>>()

    /** Whether TextBox.TextChanged has been subscribed (subscribed only once, when the first listener is added). */
    private var textChangedSubscribed = false

    /** ActionListener-style listeners and the handling of Enter's KeyDown ([ActionEvents]). */
    private val actions = ActionEvents(this, { actionCommand }, ::acceptsReturnOf)

    /** DocumentListener-style listeners (subscribes to TextBox.TextChanging when the first listener is added). */
    private val document = DocumentEvents(this, ::nativeText, ::subscribeTextChanging)

    /**
     * JTextComponent.getText / setText-style: the text of the field (TextBox.Text).
     * When set, the [addDocumentListener] listeners receive removeUpdate for the whole old text and then insertUpdate for
     * the whole new text, as Swing's setText does (nothing is delivered if the text is the same).
     */
    var text: String
        get() = nativeText()
        set(value) {
            document.replaceAll { Hstring.use(value) { h -> inspectable.call(XamlInterop.ITextBox_put_Text, h) } }
            caret.check()
        }

    private fun nativeText(): String = inspectable.getString(XamlInterop.ITextBox_get_Text)

    /** CaretListener-style listeners (subscribes to TextBox.SelectionChanged when the first listener is added). */
    private val caret = CaretEvents(this, ::nativeCaret, ::subscribeSelectionChanged)

    /** The current [dot, mark] (with a selection, dot is the end and mark is the start). */
    private fun nativeCaret(): IntArray {
        val start = selectionStart
        return intArrayOf(start + selectionLength, start)
    }

    /** The shared part that determines the width from columns ([TextExtent]). */
    private val extent = TextExtent(this)

    /**
     * JTextField.getColumns / setColumns-style: the number of columns that determines the field's width (0 by default,
     * meaning the width is not determined by columns).
     * As in Swing, the width is "columns x the width of 'm' + the left and right padding (Padding) and border
     * (BorderThickness)" (DIP). The width of 'm' is measured by drawing it in a TextBlock with the current [font] (the
     * weight is not the plain or bold of [font] but stays the control's native value, such as SemiBold; Swing's column
     * width is FontMetrics.charWidth('m')). Full-width characters such as CJK characters are often wider than 'm', so
     * this is a guide rather than a width that exactly fits that many characters. It is never narrower than the theme's
     * default minimum width (MinWidth, 64 DIP), and when the field has focus and contains text, the width available for
     * text is reduced by the clear button at the right end.
     *
     * Measuring needs a shown window (the default padding and border are also determined only after being placed in a
     * window), so columns set before placement become the width when placed (Loaded) (the wrapper is not collected
     * until then). Changing [font] remeasures it.
     * An explicit [width] takes precedence, and inside a [WLayoutPanel] this width is passed to the layout manager as the
     * preferred width ([preferredSize]) (the assigned width is decided by the layout manager). Setting it back to 0
     * removes the width determined by columns.
     *
     * @throws IllegalArgumentException if the value is negative (as in Swing)
     */
    var columns: Int
        get() = extent.columns
        set(value) {
            extent.columns = value
        }

    override fun fontChanged() = extent.fontChanged()

    override fun preferredSize(): WSize = extent.preferredSize(super.preferredSize())

    /** The placeholder shown when empty (TextBox.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(XamlInterop.ITextBox_get_PlaceholderText)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.ITextBox_put_PlaceholderText, h) }

    /** The header shown above the text box (TextBox.Header). It's an Object, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.ITextBox_put_Header, boxed.ptr)
            boxed.release()
        }

    /** Whether the field is read-only (TextBox.IsReadOnly). The inverse of [isEditable]. */
    var isReadOnly: Boolean
        get() = inspectable.getBool(XamlInterop.ITextBox_get_IsReadOnly)
        set(value) = inspectable.putBool(XamlInterop.ITextBox_put_IsReadOnly, value)

    /**
     * JTextComponent.isEditable / setEditable-style: whether the user can edit the text (true by default). The inverse
     * of [isReadOnly]; selecting and copying still work when false. [replaceSelection] / [cut] / [paste] do nothing
     * while it is false (as in Swing).
     */
    var isEditable: Boolean
        get() = !isReadOnly
        set(value) {
            isReadOnly = !value
        }

    /** Whether Enter inserts a line break (TextBox.AcceptsReturn). Used for multi-line input. */
    var acceptsReturn: Boolean
        get() = inspectable.getBool(XamlInterop.ITextBox_get_AcceptsReturn)
        set(value) = inspectable.putBool(XamlInterop.ITextBox_put_AcceptsReturn, value)

    /** How text wraps (TextBox.TextWrapping). */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(XamlInterop.ITextBox_get_TextWrapping))
        set(value) = inspectable.call(XamlInterop.ITextBox_put_TextWrapping, value.native)

    /** Text alignment (TextBox.TextAlignment). */
    var textAlignment: TextAlignment
        get() = TextAlignment.of(inspectable.getInt(XamlInterop.ITextBox_get_TextAlignment))
        set(value) = inspectable.call(XamlInterop.ITextBox_put_TextAlignment, value.native)

    /** The maximum number of characters that can be entered. 0 means unlimited (TextBox.MaxLength). */
    var maxLength: Int
        get() = inspectable.getInt(XamlInterop.ITextBox_get_MaxLength)
        set(value) = inspectable.call(XamlInterop.ITextBox_put_MaxLength, value)

    /** The selected text (TextBox.SelectedText). Unlike JTextComponent.getSelectedText, it is an empty string (not null) when nothing is selected. */
    val selectedText: String
        get() = inspectable.getString(XamlInterop.ITextBox_get_SelectedText)

    /** The number of selected characters (TextBox.SelectionLength). */
    private val selectionLength: Int
        get() = inspectable.getInt(XamlInterop.ITextBox_get_SelectionLength)

    /**
     * JTextComponent.getSelectionStart / setSelectionStart-style: the start of the selection (the caret position if
     * nothing is selected; TextBox.SelectionStart).
     * As in Swing, setting it is `select(value, selectionEnd)`, and setting it past the end moves the end to the same
     * position.
     */
    var selectionStart: Int
        get() = inspectable.getInt(XamlInterop.ITextBox_get_SelectionStart)
        set(value) = select(value, selectionEnd)

    /**
     * JTextComponent.getSelectionEnd / setSelectionEnd-style: the end of the selection (the character at this position is
     * excluded; the caret position if nothing is selected).
     * As in Swing, setting it is `select(selectionStart, value)`, and setting it before the start makes it the same
     * position as the start (no selection).
     */
    var selectionEnd: Int
        get() = selectionStart + selectionLength
        set(value) = select(selectionStart, value)

    /**
     * JTextComponent.select-style: selects from [selectionStart] up to just before [selectionEnd] (the character at the
     * end position is excluded).
     * As in Swing, the range is clamped to the string (a negative start becomes 0, positions past the length become the
     * length), and if the end is before the start it becomes the same position as the start (no selection). The caret is
     * placed at the end.
     *
     * The meaning changed from `select(start, length)` in earlier versions (the second argument being a character count)
     * to (start, end) as in Swing. To select by character count, write `select(start, start + length)`.
     */
    fun select(selectionStart: Int, selectionEnd: Int) {
        val length = text.length
        val start = selectionStart.coerceIn(0, length)
        val end = selectionEnd.coerceAtMost(length).coerceAtLeast(start)
        inspectable.call(XamlInterop.ITextBox_Select, start, end - start)
        caret.check()
    }

    /** Selects all text (TextBox.SelectAll). */
    fun selectAll() {
        inspectable.call(XamlInterop.ITextBox_SelectAll)
        caret.check()
    }

    /**
     * JTextComponent.getCaretPosition / setCaretPosition-style: the caret position (from 0 to the number of characters).
     * With a selection, it is the end of the selection (WinUI does not keep the direction of the selection, so it is not
     * the start of a selection extended backwards as in Swing).
     * Setting it clears the selection and places the caret at that position.
     *
     * @throws IllegalArgumentException if the position is before 0 or after the number of characters (as in Swing)
     */
    var caretPosition: Int
        get() = selectionEnd
        set(value) {
            require(value in 0..text.length) { "cannot place the caret at: $value (length ${text.length})" }
            inspectable.call(XamlInterop.ITextBox_Select, value, 0)
            caret.check()
        }

    /**
     * JTextComponent.replaceSelection-style: replaces the selection with [content] (inserts it at the caret position if
     * nothing is selected).
     * null or an empty string deletes the selection. After replacing, the caret is placed after the inserted text (the
     * selection is cleared).
     * Does nothing if [isEditable] is false (as in Swing). It can be undone like a user edit ([undo]).
     * The [addDocumentListener] listeners receive removeUpdate for the whole selection and then insertUpdate for the
     * inserted text, as in Swing.
     */
    fun replaceSelection(content: String?) {
        if (isReadOnly) return
        val start = selectionStart
        val selected = selectionLength
        val before = text.length
        val inserted = content.orEmpty()
        document.replaceRange(start, selected) {
            Hstring.use(inserted) { h -> inspectable.call(XamlInterop.ITextBox_put_SelectedText, h) }
        }
        // TextBox leaves the inserted text selected, so place the caret after the inserted text as in Swing.
        // TextBox normalizes the inserted string (CRLF to CR when multi-line, and drops everything after a line break when
        // single-line), so the position is computed from the length actually added, not the length of the passed string
        val position = start + (text.length - before + selected)
        inspectable.call(XamlInterop.ITextBox_Select, position, 0)
        caret.check()
    }

    /** JTextComponent.copy-style: copies the selection to the clipboard (TextBox.CopySelectionToClipboard). Does nothing if nothing is selected. */
    fun copy() {
        inspectable.call(XamlInterop.ITextBox_CopySelectionToClipboard)
    }

    /**
     * JTextComponent.cut-style: moves the selection to the clipboard (TextBox.CutSelectionToClipboard).
     * Does nothing if [isEditable] is false (as in Swing).
     */
    fun cut() {
        if (!isReadOnly) inspectable.call(XamlInterop.ITextBox_CutSelectionToClipboard)
    }

    /**
     * JTextComponent.paste-style: replaces the selection with the text on the clipboard (inserts it at the caret position
     * if nothing is selected; TextBox.PasteFromClipboard). Reading the clipboard is asynchronous, so the text changes
     * after the call returns (in the message loop).
     * Does nothing if [isEditable] is false (as in Swing).
     */
    fun paste() {
        if (!isReadOnly) inspectable.call(XamlInterop.ITextBox_PasteFromClipboard)
    }

    /**
     * UndoManager.undo-style: undoes the last edit (typing, [replaceSelection], [cut], [paste], etc.; TextBox.Undo).
     * Does nothing if there is no edit to undo (unlike UndoManager, it does not throw).
     */
    fun undo() {
        inspectable.call(XamlInterop.ITextBox_Undo)
    }

    /** UndoManager.redo-style: redoes an edit undone by [undo] (TextBox.Redo). Does nothing if there is no edit to redo. */
    fun redo() {
        inspectable.call(XamlInterop.ITextBox_Redo)
    }

    /** UndoManager.canUndo-style: whether there is an edit that [undo] can undo (TextBox.CanUndo). */
    fun canUndo(): Boolean = inspectable.getBool(XamlInterop.ITextBox_get_CanUndo)

    /** UndoManager.canRedo-style: whether there is an edit that [redo] can redo (TextBox.CanRedo). */
    fun canRedo(): Boolean = inspectable.getBool(XamlInterop.ITextBox_get_CanRedo)

    /**
     * JTextField.setActionCommand-like: the string put into [WActionEvent.actionCommand] of the listeners called on Enter.
     * While unset (null), returns the [text] at that moment (same as JTextField).
     */
    var actionCommand: String? = null
        get() = field ?: text

    init {
        if (placeholder.isNotEmpty()) placeholderText = placeholder
        actions.install()
    }

    /**
     * ActionListener-like: called when Enter with no modifier keys is pressed in a single-line field (same as
     * JTextField.addActionListener).
     * The listener receives a [WActionEvent] whose source is this field. While there are listeners, Enter does not press
     * the default button.
     *
     * Enter pressed to confirm IME conversion is expected to arrive not as Enter but as WinUI's VirtualKey.ProcessKey
     * (229), in which case the listeners are not called. However, this has not been confirmed by tests (unverified).
     */
    @JvmSynthetic
    fun addActionListener(listener: (WActionEvent) -> Unit) = addAction(listener, listener)

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: Runnable) = addAction(listener) { listener.run() }

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: WActionListener) = addAction(listener) { listener.actionPerformed(it) }

    /** Removes a listener registered with [addActionListener] (only one if the same listener was registered several times). */
    @JvmSynthetic
    fun removeActionListener(listener: (WActionEvent) -> Unit) = removeAction(listener)

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: Runnable) = removeAction(listener)

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: WActionListener) = removeAction(listener)

    private fun addAction(key: Any, invoke: (WActionEvent) -> Unit) = actions.add(key, invoke)

    private fun removeAction(key: Any) = actions.remove(key)

    /**
     * JTextField.postActionEvent-like: calls the registered listeners in registration order, as when Enter is pressed.
     * Does not press the default button.
     */
    fun postActionEvent() = actions.post()

    /**
     * Document.addDocumentListener-style: adds a listener called when the text changes. Does nothing if null.
     * Swing's JTextField adds it to the Document from getDocument(), but WinUI4K has no Document, so it is added to the
     * field directly.
     *
     * User edits such as typing, pasting and undo are detected via TextBox.TextChanging (delivered right after the
     * change, before rendering), and the difference from the previous text is reported as a single removeUpdate (the
     * removed part) and insertUpdate (the inserted part). The position is found by excluding the matching prefix and
     * suffix, and where the same character repeats (such as typing a at the start of "aa") it is decided by the caret
     * position.
     * Characters being composed in the IME are also reported each time the field's text changes. The field has no
     * formatting, so changedUpdate is never called.
     * Changes made by [text] and [replaceSelection] are reported in the same form as Swing (see their descriptions).
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

    /** Subscribes to TextBox.TextChanging. The handler stays in the native object, so the field is looked up from [listenerAnchor]. */
    private fun subscribeTextChanging() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.DocumentTextChanging",
            XamlInterop.IID_TextBoxTextChangingHandler,
            XamlInterop.ITextBox_add_TextChanging,
        ) { _, _ ->
            val field = anchor.get() as? WTextField
            if (field != null) field.document.onNativeChange(field.selectionEnd)
        }
    }

    /**
     * JTextComponent.addCaretListener-style: adds a listener called when the caret or the selection moves. Does nothing
     * if null.
     * [WCaretEvent.dot] is the caret position (the end of the selection, if any), and [WCaretEvent.mark] is the other
     * end of the selection (the start).
     *
     * Movement and typing by key or mouse are detected via TextBox.SelectionChanged and delivered later (in the message
     * loop). Programmatic changes such as [select], [caretPosition], [text] and [replaceSelection] are delivered right
     * after the change, as in Swing. Notifications where the position does not change are not reported.
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

    /** Subscribes to TextBox.SelectionChanged. The handler stays in the native object, so the field is looked up from [listenerAnchor]. */
    private fun subscribeSelectionChanged() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.CaretSelectionChanged",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.ITextBox_add_SelectionChanged,
        ) { _, _ -> (anchor.get() as? WTextField)?.caret?.check() }
    }

    /**
     * Subscribes to text changes (TextBox.TextChanged). The listener receives the text after the change.
     * TextChanged is delivered later in a batch (in the message loop), so consecutive changes may be merged into one.
     * Use [addDocumentListener] when you need the changed position and length.
     * The Kotlin lambda form and the Java Consumer form go into the same list and are called in the order added.
     */
    @JvmSynthetic
    fun addTextChangedListener(listener: (String) -> Unit) = addTextChanged(listener, listener)

    @JvmName("addTextChangedListener")
    fun addTextChangedListenerForJava(listener: Consumer<String>) = addTextChanged(listener) { listener.accept(it) }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    @JvmSynthetic
    fun removeTextChangedListener(listener: (String) -> Unit) = removeTextChanged(listener)

    @JvmName("removeTextChangedListener")
    fun removeTextChangedListenerForJava(listener: Consumer<String>) = removeTextChanged(listener)

    private fun addTextChanged(key: Any, invoke: (String) -> Unit) {
        if (!textChangedSubscribed) {
            // The handler stays in the native object, so do not capture the field strongly; look it up from listenerAnchor
            val anchor = listenerAnchor
            inspectable.addEventHandler(
                "WinUI4K.TextChangedHandler",
                XamlInterop.IID_TextChangedEventHandler,
                XamlInterop.ITextBox_add_TextChanged,
            ) { _, _ -> (anchor.get() as? WTextField)?.fireTextChanged() }
            textChangedSubscribed = true
        }
        textChangedListeners += key to invoke
        listenerAnchor.listenerAdded()
    }

    private fun removeTextChanged(key: Any) {
        val index = textChangedListeners.indexOfLast { it.first === key }
        if (index < 0) return
        textChangedListeners.removeAt(index)
        listenerAnchor.listenerRemoved()
    }

    private fun fireTextChanged() {
        if (textChangedListeners.isEmpty()) return
        val current = text
        for ((_, invoke) in textChangedListeners.toList()) invoke(current)
    }
}

/** Whether the TextBox that raised the Enter event is multi-line and accepts line breaks (AcceptsReturn). [sender] is borrowed. */
private fun acceptsReturnOf(sender: ComPtr): Boolean {
    val textBox = sender.queryInterface(XamlInterop.IID_ITextBox)
    return try {
        textBox.getBool(XamlInterop.ITextBox_get_AcceptsReturn)
    } finally {
        textBox.release()
    }
}
