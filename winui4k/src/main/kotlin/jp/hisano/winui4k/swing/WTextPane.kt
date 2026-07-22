package jp.hisano.winui4k.swing

import jp.hisano.winui4k.com.ComPtr
import jp.hisano.winui4k.winrt.Activation
import jp.hisano.winui4k.winrt.Hstring
import jp.hisano.winui4k.winrt.PropertyValues
import jp.hisano.winui4k.winrt.addEventHandler
import jp.hisano.winui4k.winrt.getString
import jp.hisano.winui4k.winrt.removeEventHandler
import jp.hisano.winui4k.winui.Abi

/**
 * JTextPane-like: WinUI 3's RichEditBox.
 *
 * Supports editing formatted text such as bold and italic. Formatting is toggled on the
 * selection via [toggleSelectionBold] / [toggleSelectionItalic].
 * The whole content is read/written via [text] (plain text) and [rtfText] (RTF format).
 */
class WTextPane(placeholder: String = "") : WControl(
    Activation.composeDefault(Abi.CLS_RichEditBox, Abi.IID_IRichEditBoxFactory), // default interface = IRichEditBox
) {
    /** RichEditBox.Document (Microsoft.UI.Text.ITextDocument). The entry point for text and formatting operations. */
    private val document: ComPtr by lazy {
        inspectable.getPtr(Abi.IRichEditBox_get_Document)
    }

    /** TextChanged event tokens registered via addTextChangedListener. */
    private val textChangedTokens = ListenerTokens<() -> Unit>()

    /** The whole content as plain text (ITextDocument.GetText / SetText). Ends with a trailing newline. */
    var text: String
        get() = document.getString(Abi.ITextDocument_GetText, Abi.TextOptions_None)
        set(value) = Hstring.use(value) { h ->
            document.call(Abi.ITextDocument_SetText, Abi.TextOptions_None, h)
        }

    /** The whole content as RTF text. Used to save and restore content along with its formatting. */
    var rtfText: String
        get() = document.getString(Abi.ITextDocument_GetText, Abi.TextOptions_FormatRtf)
        set(value) = Hstring.use(value) { h ->
            document.call(Abi.ITextDocument_SetText, Abi.TextOptions_FormatRtf, h)
        }

    /** The placeholder shown when empty (RichEditBox.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(Abi.IRichEditBox_get_PlaceholderText)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IRichEditBox_put_PlaceholderText, h) }

    /** The header shown above the text pane (RichEditBox.Header). It's an Object, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.IRichEditBox_put_Header, boxed.ptr)
            boxed.release()
        }

    /** Whether the text pane is read-only (RichEditBox.IsReadOnly). */
    var isReadOnly: Boolean
        get() = inspectable.getBool(Abi.IRichEditBox_get_IsReadOnly)
        set(value) = inspectable.putBool(Abi.IRichEditBox_put_IsReadOnly, value)

    /** How text wraps (RichEditBox.TextWrapping). Wraps by default. */
    var textWrapping: TextWrapping
        get() = TextWrapping.of(inspectable.getInt(Abi.IRichEditBox_get_TextWrapping))
        set(value) = inspectable.call(Abi.IRichEditBox_put_TextWrapping, value.native)

    init {
        if (placeholder.isNotEmpty()) placeholderText = placeholder
    }

    /** Whether there's an edit that can be undone (ITextDocument.CanUndo). */
    val canUndo: Boolean
        get() = document.getBool(Abi.ITextDocument_CanUndo)

    /** Whether there's an undone edit that can be redone (ITextDocument.CanRedo). */
    val canRedo: Boolean
        get() = document.getBool(Abi.ITextDocument_CanRedo)

    /** Undoes the most recent edit (ITextDocument.Undo). */
    fun undo() {
        document.call(Abi.ITextDocument_Undo)
    }

    /** Redoes the most recently undone edit (ITextDocument.Redo). */
    fun redo() {
        document.call(Abi.ITextDocument_Redo)
    }

    /** Toggles bold on the selection (Selection.CharacterFormat.Bold = Toggle). */
    fun toggleSelectionBold() {
        withSelectionFormat { format -> format.call(Abi.ITextCharacterFormat_put_Bold, Abi.FormatEffect_Toggle) }
    }

    /** Toggles italic on the selection (Selection.CharacterFormat.Italic = Toggle). */
    fun toggleSelectionItalic() {
        withSelectionFormat { format -> format.call(Abi.ITextCharacterFormat_put_Italic, Abi.FormatEffect_Toggle) }
    }

    /** Gets the selection's ITextCharacterFormat, passes it to the block, and reliably releases it. */
    private fun withSelectionFormat(block: (ComPtr) -> Unit) {
        // ITextSelection doesn't inherit ITextRange's members, so QI it first before use
        val selection = document.getPtr(Abi.ITextDocument_get_Selection)
        try {
            val range = selection.queryInterface(Abi.IID_ITextRange)
            try {
                val format = range.getPtr(Abi.ITextRange_get_CharacterFormat)
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
            Abi.IID_RoutedEventHandler,
            Abi.IRichEditBox_add_TextChanged,
        ) { _, _ -> listener() }
        textChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    fun removeTextChangedListener(listener: () -> Unit) {
        val token = textChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IRichEditBox_remove_TextChanged, token)
    }
}
