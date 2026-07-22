package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JTextField-like: WinUI 3's TextBox.
 *
 * Enabling [acceptsReturn] and [textWrapping] turns it into a JTextArea-like multi-line input.
 * [addTextChangedListener] (TextChanged) delivers the current text on every change.
 */
class WTextField(placeholder: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_TextBox, XamlInterop.IID_ITextBoxFactory)
        .queryInterface(XamlInterop.IID_ITextBox),
) {
    /** TextChanged event tokens registered via addTextChangedListener. */
    private val textChangedTokens = ListenerTokens<(String) -> Unit>()

    var text: String
        get() = inspectable.getString(XamlInterop.ITextBox_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.ITextBox_put_Text, h) }

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

    /** Whether the text box is read-only (TextBox.IsReadOnly). */
    var isReadOnly: Boolean
        get() = inspectable.getBool(XamlInterop.ITextBox_get_IsReadOnly)
        set(value) = inspectable.putBool(XamlInterop.ITextBox_put_IsReadOnly, value)

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

    /** The currently selected text (TextBox.SelectedText). */
    val selectedText: String
        get() = inspectable.getString(XamlInterop.ITextBox_get_SelectedText)

    /** Selects [length] characters starting at [start] (TextBox.Select). */
    fun select(start: Int, length: Int) {
        inspectable.call(XamlInterop.ITextBox_Select, start, length)
    }

    /** Selects all text (TextBox.SelectAll). */
    fun selectAll() {
        inspectable.call(XamlInterop.ITextBox_SelectAll)
    }

    init {
        if (placeholder.isNotEmpty()) placeholderText = placeholder
    }

    /** Subscribes to text changes (TextBox.TextChanged). The listener receives the text after the change. */
    fun addTextChangedListener(listener: (String) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TextChangedHandler",
            XamlInterop.IID_TextChangedEventHandler,
            XamlInterop.ITextBox_add_TextChanged,
        ) { _, _ -> listener(text) }
        textChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    fun removeTextChangedListener(listener: (String) -> Unit) {
        val token = textChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITextBox_remove_TextChanged, token)
    }
}
