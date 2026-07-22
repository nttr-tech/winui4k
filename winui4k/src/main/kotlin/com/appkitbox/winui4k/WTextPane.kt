package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JTextPane-like: WinUI 3's RichEditBox.
 *
 * Supports editing formatted text such as bold and italic. Formatting is toggled on the
 * selection via [toggleSelectionBold] / [toggleSelectionItalic].
 * The whole content is read/written via [text] (plain text) and [rtfText] (RTF format).
 */
class WTextPane(placeholder: String = "") : WControl(
    Activation.composeDefault(XamlInterop.CLS_RichEditBox, XamlInterop.IID_IRichEditBoxFactory), // default interface = IRichEditBox
) {
    /** RichEditBox.Document (Microsoft.UI.Text.ITextDocument). The entry point for text and formatting operations. */
    private val document: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.IRichEditBox_get_Document))
    }

    /** TextChanged event tokens registered via addTextChangedListener. */
    private val textChangedTokens = ListenerTokens<() -> Unit>()

    /** The whole content as plain text (ITextDocument.GetText / SetText). Ends with a trailing newline. */
    var text: String
        get() = document.getString(XamlInterop.ITextDocument_GetText, XamlInterop.TextOptions_None)
        set(value) = Hstring.use(value) { h ->
            document.call(XamlInterop.ITextDocument_SetText, XamlInterop.TextOptions_None, h)
        }

    /** The whole content as RTF text. Used to save and restore content along with its formatting. */
    var rtfText: String
        get() = document.getString(XamlInterop.ITextDocument_GetText, XamlInterop.TextOptions_FormatRtf)
        set(value) = Hstring.use(value) { h ->
            document.call(XamlInterop.ITextDocument_SetText, XamlInterop.TextOptions_FormatRtf, h)
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

    /** Whether the text pane is read-only (RichEditBox.IsReadOnly). */
    var isReadOnly: Boolean
        get() = inspectable.getBool(XamlInterop.IRichEditBox_get_IsReadOnly)
        set(value) = inspectable.putBool(XamlInterop.IRichEditBox_put_IsReadOnly, value)

    /** How text wraps (RichEditBox.TextWrapping). Wraps by default. */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(XamlInterop.IRichEditBox_get_TextWrapping))
        set(value) = inspectable.call(XamlInterop.IRichEditBox_put_TextWrapping, value.native)

    init {
        if (placeholder.isNotEmpty()) placeholderText = placeholder
    }

    /** Whether there's an edit that can be undone (ITextDocument.CanUndo). */
    val canUndo: Boolean
        get() = document.getBool(XamlInterop.ITextDocument_CanUndo)

    /** Whether there's an undone edit that can be redone (ITextDocument.CanRedo). */
    val canRedo: Boolean
        get() = document.getBool(XamlInterop.ITextDocument_CanRedo)

    /** Undoes the most recent edit (ITextDocument.Undo). */
    fun undo() {
        document.call(XamlInterop.ITextDocument_Undo)
    }

    /** Redoes the most recently undone edit (ITextDocument.Redo). */
    fun redo() {
        document.call(XamlInterop.ITextDocument_Redo)
    }

    /** Toggles bold on the selection (Selection.CharacterFormat.Bold = Toggle). */
    fun toggleSelectionBold() {
        withSelectionFormat { format -> format.call(XamlInterop.ITextCharacterFormat_put_Bold, XamlInterop.FormatEffect_Toggle) }
    }

    /** Toggles italic on the selection (Selection.CharacterFormat.Italic = Toggle). */
    fun toggleSelectionItalic() {
        withSelectionFormat { format -> format.call(XamlInterop.ITextCharacterFormat_put_Italic, XamlInterop.FormatEffect_Toggle) }
    }

    /** Gets the selection's ITextCharacterFormat, passes it to the block, and reliably releases it. */
    private fun withSelectionFormat(block: (ComPtr) -> Unit) {
        // ITextSelection doesn't inherit ITextRange's members, so QI it first before use
        val selection = document.getPtr(XamlInterop.ITextDocument_get_Selection)
        try {
            val range = selection.queryInterface(XamlInterop.IID_ITextRange)
            try {
                val format = range.getPtr(XamlInterop.ITextRange_get_CharacterFormat)
                try {
                    block(format)
                } finally {
                    format.release()
                }
            } finally {
                range.release()
            }
        } finally {
            selection.release()
        }
    }

    /** Subscribes to text changes (RichEditBox.TextChanged). */
    fun addTextChangedListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.RichEditTextChangedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IRichEditBox_add_TextChanged,
        ) { _, _ -> listener() }
        textChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    fun removeTextChangedListener(listener: () -> Unit) {
        val token = textChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IRichEditBox_remove_TextChanged, token)
    }
}
