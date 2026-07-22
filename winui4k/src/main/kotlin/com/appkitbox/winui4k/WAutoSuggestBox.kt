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
 * Microsoft.UI.Xaml.Controls.AutoSuggestionBoxTextChangeReason (why the text changed).
 * Values extracted from the winmd (UserInput=0, ProgrammaticChange=1, SuggestionChosen=2).
 */
enum class TextChangeReason(internal val native: Int) {
    /** A change from the user's own keystrokes. Suggestions should only be filtered on this reason. */
    USER_INPUT(0),

    /** A change made from code. */
    PROGRAMMATIC_CHANGE(1),

    /** A change from choosing a suggestion. */
    SUGGESTION_CHOSEN(2),
    ;

    internal companion object {
        fun of(native: Int): TextChangeReason = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's AutoSuggestBox (an ItemsControl subclass). No Swing equivalent.
 *
 * A text box that shows a list of suggestions as you type.
 * Detect input via [addTextChangedListener] and replace suggestions via [setSuggestions];
 * you can also subscribe to [addQuerySubmittedListener] (Enter / choosing a suggestion) and
 * [addSuggestionChosenListener] (a suggestion getting highlighted).
 */
class WAutoSuggestBox(placeholder: String = "") : WControl(
    Activation.activate(XamlInterop.CLS_AutoSuggestBox, XamlInterop.IID_IAutoSuggestBox),
) {
    /** The IItemsControl view used to set ItemsSource. */
    private val itemsControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IItemsControl))
    }

    /** TextChanged event tokens registered via addTextChangedListener. */
    private val textChangedTokens = ListenerTokens<(String, TextChangeReason) -> Unit>()

    /** QuerySubmitted event tokens registered via addQuerySubmittedListener. */
    private val querySubmittedTokens = ListenerTokens<(String, String?) -> Unit>()

    /** SuggestionChosen event tokens registered via addSuggestionChosenListener. */
    private val suggestionChosenTokens = ListenerTokens<(String) -> Unit>()

    /** The text currently being entered (AutoSuggestBox.Text). */
    var text: String
        get() = inspectable.getString(XamlInterop.IAutoSuggestBox_get_Text)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IAutoSuggestBox_put_Text, h) }

    /** The placeholder shown when empty (AutoSuggestBox.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(XamlInterop.IAutoSuggestBox_get_PlaceholderText)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IAutoSuggestBox_put_PlaceholderText, h) }

    /** The header shown above the box (AutoSuggestBox.Header). It's an Object, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IAutoSuggestBox_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The icon shown on the right of the text (AutoSuggestBox.QueryIcon). Creates and passes a SymbolIcon. */
    var queryIcon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                inspectable.call(XamlInterop.IAutoSuggestBox_put_QueryIcon, null)
            } else {
                val icon = value.createIconElement()
                inspectable.call(XamlInterop.IAutoSuggestBox_put_QueryIcon, icon)
                icon.release()
            }
        }

    /** Whether the suggestion list is open (AutoSuggestBox.IsSuggestionListOpen). */
    var isSuggestionListOpen: Boolean
        get() = inspectable.getBool(XamlInterop.IAutoSuggestBox_get_IsSuggestionListOpen)
        set(value) = inspectable.putBool(XamlInterop.IAutoSuggestBox_put_IsSuggestionListOpen, value)

    init {
        if (placeholder.isNotEmpty()) placeholderText = placeholder
    }

    /**
     * Replaces the suggestion list with [suggestions] (assigning to ItemsControl.ItemsSource).
     * The suggestion popup only refreshes when ItemsSource itself changes, so instead of mutating
     * Items (ItemCollection), a brand-new IIterable<Object> is set each time.
     */
    fun setSuggestions(suggestions: List<String>) {
        val iterable = StringIterable(suggestions)
        itemsControl.call(XamlInterop.IItemsControl_put_ItemsSource, iterable.comObject.primary)
    }

    /**
     * Subscribes to text changes (AutoSuggestBox.TextChanged).
     * The listener receives the text after the change and the reason for the change.
     * As a rule, only filter suggestions when the reason is [TextChangeReason.USER_INPUT].
     */
    fun addTextChangedListener(listener: (String, TextChangeReason) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.AutoSuggestBoxTextChangedHandler",
            XamlInterop.IID_AutoSuggestBoxTextChangedHandler,
            XamlInterop.IAutoSuggestBox_add_TextChanged,
        ) { _, args ->
            // args is AutoSuggestBoxTextChangedEventArgs; read Reason and pass it along
            val reason = TextChangeReason.of(
                ComPtr(args).getInt(XamlInterop.IAutoSuggestBoxTextChangedEventArgs_get_Reason),
            )
            listener(text, reason)
        }
        textChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    fun removeTextChangedListener(listener: (String, TextChangeReason) -> Unit) {
        val token = textChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IAutoSuggestBox_remove_TextChanged, token)
    }

    /**
     * Subscribes to confirmation via Enter or choosing a suggestion (AutoSuggestBox.QuerySubmitted).
     * The listener receives the entered text, and if confirmed from a suggestion, that suggestion (null otherwise).
     */
    fun addQuerySubmittedListener(listener: (queryText: String, chosenSuggestion: String?) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.AutoSuggestBoxQuerySubmittedHandler",
            XamlInterop.IID_AutoSuggestBoxQuerySubmittedHandler,
            XamlInterop.IAutoSuggestBox_add_QuerySubmitted,
        ) { _, args ->
            // args is AutoSuggestBoxQuerySubmittedEventArgs; read QueryText and ChosenSuggestion and pass them along
            val argsPtr = ComPtr(args)
            val queryText = argsPtr.getString(XamlInterop.IAutoSuggestBoxQuerySubmittedEventArgs_get_QueryText)
            val boxed = argsPtr.getPtrOrNull(XamlInterop.IAutoSuggestBoxQuerySubmittedEventArgs_get_ChosenSuggestion)
            val chosen = boxed?.let {
                try {
                    PropertyValues.unboxString(it)
                } finally {
                    it.release()
                }
            }
            listener(queryText, chosen)
        }
        querySubmittedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addQuerySubmittedListener]. */
    fun removeQuerySubmittedListener(listener: (String, String?) -> Unit) {
        val token = querySubmittedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IAutoSuggestBox_remove_QuerySubmitted, token)
    }

    /** Subscribes to suggestion highlight changes (AutoSuggestBox.SuggestionChosen). The listener receives the suggestion string. */
    fun addSuggestionChosenListener(listener: (String) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.AutoSuggestBoxSuggestionChosenHandler",
            XamlInterop.IID_AutoSuggestBoxSuggestionChosenHandler,
            XamlInterop.IAutoSuggestBox_add_SuggestionChosen,
        ) { _, args ->
            // args is AutoSuggestBoxSuggestionChosenEventArgs; read SelectedItem and pass it along
            val boxed = ComPtr(args).getPtr(XamlInterop.IAutoSuggestBoxSuggestionChosenEventArgs_get_SelectedItem)
            val selected = try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
            listener(selected)
        }
        suggestionChosenTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSuggestionChosenListener]. */
    fun removeSuggestionChosenListener(listener: (String) -> Unit) {
        val token = suggestionChosenTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IAutoSuggestBox_remove_SuggestionChosen, token)
    }
}
