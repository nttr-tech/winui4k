package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
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
    /** TextChanged event tokens registered via addTextChangedListener. */
    private val textChangedTokens = ListenerTokens<Consumer<String>>()

    /**
     * Listeners registered with addActionListener (the registered object and the function that receives the event).
     * Called in registration order.
     */
    private val actionListeners = ArrayDeque<Pair<Any, (WActionEvent) -> Unit>>()

    /**
     * The handling held by the Enter KeyDown handler (the handler stays on the native side, so it does not reference this
     * field strongly).
     */
    private val enterKeyTarget = EnterKeyTarget()

    /** DocumentListener-style listeners (subscribes to TextBox.TextChanging when the first listener is added). */
    private val document = DocumentEvents(this, ::nativeText, ::subscribeTextChanging)

    /**
     * JTextComponent.getText / setText-style: the text of the field (TextBox.Text).
     * When set, the [addDocumentListener] listeners receive removeUpdate for the whole old text and then insertUpdate for
     * the whole new text, as Swing's setText does (nothing is delivered if the text is the same).
     */
    var text: String
        get() = nativeText()
        set(value) = document.replaceAll { Hstring.use(value) { h -> inspectable.call(XamlInterop.ITextBox_put_Text, h) } }

    private fun nativeText(): String = inspectable.getString(XamlInterop.ITextBox_get_Text)

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
    }

    /** Selects all text (TextBox.SelectAll). */
    fun selectAll() {
        inspectable.call(XamlInterop.ITextBox_SelectAll)
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
        val inserted = content.orEmpty()
        document.replaceRange(start, selectionLength) {
            Hstring.use(inserted) { h -> inspectable.call(XamlInterop.ITextBox_put_SelectedText, h) }
        }
        // TextBox leaves the inserted text selected, so place the caret after the inserted text as in Swing
        inspectable.call(XamlInterop.ITextBox_Select, start + inserted.length, 0)
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
        val target = enterKeyTarget
        // Even when single-line, TextBox passes the Enter KeyDown to the text engine and marks it handled (OnKeyDown in
        // TextBoxBase.cpp), so the default button's global accelerator does not fire. It is marked handled after the
        // element's handlers (measured with 2.5.4-experimental), but to avoid relying on the order, subscribe in a way that
        // also receives handled events
        Xaml.addHandlerHandledToo(
            this,
            XamlInterop.IUIElementStatics_get_KeyDownEvent,
            XamlInterop.IID_KeyEventHandler,
            FoundationInterop.IID_IReference_KeyEventHandler,
        ) { sender, args -> target.onKeyDown(sender, XamlKeyEvent(ComPtr(args))) }
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

    private fun addAction(key: Any, invoke: (WActionEvent) -> Unit) {
        actionListeners.addLast(key to invoke)
        enterKeyTarget.anchor = listenerAnchor
        listenerAnchor.listenerAdded()
    }

    private fun removeAction(key: Any) {
        val index = actionListeners.indexOfLast { it.first === key }
        if (index < 0) return
        actionListeners.removeAt(index)
        listenerAnchor.listenerRemoved()
    }

    /** Whether there is at least one ActionListener. */
    internal val hasActionListeners: Boolean get() = actionListeners.isNotEmpty()

    /**
     * JTextField.postActionEvent-like: calls the registered listeners in registration order, as when Enter is pressed.
     * Does not press the default button.
     */
    fun postActionEvent() {
        val event = WActionEvent(this, actionCommand, WActionEvent.fromVirtualKeyModifiers(Xaml.currentModifiers()))
        for ((_, invoke) in actionListeners.toList()) invoke(event)
    }

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
            if (field != null) field.document.onNativeChange(field.selectionStart)
        }
    }

    /** Subscribes to text changes (TextBox.TextChanged). The listener receives the text after the change. */
    @JvmSynthetic
    fun addTextChangedListener(listener: (String) -> Unit) {
        val adapter = Consumer<String> { listener(it) }
        addTextChangedListenerForJava(adapter)
        textChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addTextChangedListener")
    fun addTextChangedListenerForJava(listener: Consumer<String>) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TextChangedHandler",
            XamlInterop.IID_TextChangedEventHandler,
            XamlInterop.ITextBox_add_TextChanged,
        ) { _, _ -> listener.accept(text) }
        textChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    @JvmSynthetic
    fun removeTextChangedListener(listener: (String) -> Unit) {
        val adapter = textChangedTokens.removeKotlinAdapter(listener) ?: return
        removeTextChangedListenerForJava(adapter)
    }

    @JvmName("removeTextChangedListener")
    fun removeTextChangedListenerForJava(listener: Consumer<String>) {
        val token = textChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITextBox_remove_TextChanged, token)
    }
}

/**
 * Handling of Enter KeyDown for WTextField. Called from the handler attached to the native TextBox; the field's wrapper is
 * looked up from [anchor] (the wrapper of a field without listeners may be collected; even then the default button is
 * pressed).
 */
private class EnterKeyTarget {
    /** A reference to the field that has the listeners (null until the first ActionListener is added). */
    var anchor: ListenerAnchor<WComponent>? = null

    /**
     * On Enter with no modifier keys, if [sender] (TextBox) is single-line (AcceptsReturn = false), calls the listeners or
     * presses the default button and marks the event handled. Since it is marked handled, the default button's Enter
     * accelerator does not press it again.
     */
    fun onKeyDown(sender: Ptr, event: XamlKeyEvent) {
        if (event.key != VirtualKey.ENTER.native || Xaml.currentModifiers() != 0) return
        val textBox = ComPtr(sender).queryInterface(XamlInterop.IID_ITextBox)
        try {
            // Does not act on an Enter consumed in a KeyListener's keyPressed (in Swing, bindings for a consumed key do not act either)
            if (ConsumedKeyPresses.take(textBox, VirtualKey.ENTER.native)) return
            if (textBox.getBool(XamlInterop.ITextBox_get_AcceptsReturn)) return
            val field = anchor?.get() as? WTextField
            if (field != null && field.hasActionListeners) {
                event.handled = true
                field.postActionEvent()
                return
            }
            val button = DefaultButtons.inRootOf(textBox) ?: return
            if (button.isEnabled) {
                event.handled = true
                button.doClick()
            }
        } finally {
            textBox.release()
        }
    }
}
