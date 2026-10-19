package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
        @JvmSynthetic
        fun of(native: Int): TextChangeReason = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's AutoSuggestBox (an ItemsControl subclass). No Swing equivalent.
 *
 * A text box that shows a list of suggestions as the user types (similar to an editable JComboBox).
 * Detect input via [addTextChangedListener] and replace suggestions via [setSuggestions];
 * [addActionListener] / [addQuerySubmittedListener] (Enter / choosing a suggestion) and
 * [addSuggestionChosenListener] (a suggestion getting highlighted).
 */
class WAutoSuggestBox @JvmOverloads constructor(placeholder: String = "") : WControl(
    Activation.activate(XamlInterop.CLS_AutoSuggestBox, XamlInterop.IID_IAutoSuggestBox),
) {
    /** Its own IsTabStop is false, and UIElement.Focus passes focus to the template's input field (TextBox) ([isFocusable] / [requestFocusInWindow]). */
    override val focusIsDelegated: Boolean get() = true

    /** The IItemsControl view used to set ItemsSource. */
    private val itemsControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IItemsControl))
    }

    /** TextChanged event tokens registered via addTextChangedListener. */
    private val textChangedTokens = ListenerTokens<BiConsumer<String, TextChangeReason>>()

    /** QuerySubmitted event tokens registered via addQuerySubmittedListener. */
    private val querySubmittedTokens = ListenerTokens<BiConsumer<String, String?>>()

    /** SuggestionChosen event tokens registered via addSuggestionChosenListener. */
    private val suggestionChosenTokens = ListenerTokens<Consumer<String>>()

    /** ActionListener-style listeners ([ActionEvents]; the Enter KeyDown is received via QuerySubmitted, so [ActionEvents.install] is not used). */
    private val actions = ActionEvents(this, { actionCommand ?: submittedQuery ?: text }, { false })

    /** The query committed by QuerySubmitted (only during the commit notification; [text] may not yet have caught up with the field's latest string at this point). */
    private var submittedQuery: String? = null

    /** Whether QuerySubmitted has been subscribed for ActionListeners (subscribed only once, when the first listener is added). */
    private var actionSubscribed = false

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

    /**
     * Icon shown on the right side of the text (AutoSuggestBox.QueryIcon). Creates an IconElement (SymbolIcon /
     * ImageIcon / FontIcon) from a [Symbol], [WImageIcon] or [WFontIcon] and passes it, and returns the icon as set.
     * null for no icon.
     * Earlier versions accepted only [Symbol]. In Kotlin source, `box.queryIcon = Symbol.FIND` still works as written,
     * but the type when reading changed to [WIcon] (use `as? Symbol` when a Symbol is needed).
     */
    var queryIcon: WIcon? = null
        set(value) {
            value?.requireSupported()
            field = value
            val icon = value?.createIconElement()
            try {
                inspectable.call(XamlInterop.IAutoSuggestBox_put_QueryIcon, icon?.ptr)
            } finally {
                icon?.release()
            }
        }

    /** The old-style `setQueryIcon(Symbol)` (for binary compatibility, invisible from source; Symbol is a WIcon, so the same code selects the WIcon form). */
    @Deprecated("use queryIcon, which takes a WIcon", level = DeprecationLevel.HIDDEN)
    @JvmName("setQueryIcon")
    fun setQueryIconForSymbol(icon: Symbol?) {
        queryIcon = icon
    }

    /** The old-style `Symbol getQueryIcon()` (for binary compatibility, invisible from source). null for icons other than Symbol. */
    @Deprecated("use queryIcon, which returns a WIcon", level = DeprecationLevel.HIDDEN)
    @JvmName("getQueryIcon")
    fun getQueryIconAsSymbol(): Symbol? = queryIcon as? Symbol

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
     * JTextField.setActionCommand-style: the string put into [WActionEvent.actionCommand] for [addActionListener]
     * listeners.
     * While unset (null), returns the committed query string (as in JTextField).
     */
    var actionCommand: String? = null

    /**
     * ActionListener-style: called when a query is committed with Enter or by choosing a suggestion
     * (AutoSuggestBox.QuerySubmitted).
     * Listeners receive a [WActionEvent] with this box as the source. When the committed query and the chosen suggestion
     * are needed, use [addQuerySubmittedListener]. It plays the same role as Enter in an editable JComboBox, and the
     * ActionEvent's [WActionEvent.actionCommand] is [actionCommand] (the text if unset).
     */
    @JvmSynthetic
    fun addActionListener(listener: (WActionEvent) -> Unit) = addAction(listener, listener)

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: Runnable) = addAction(listener) { listener.run() }

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: WActionListener) = addAction(listener) { listener.actionPerformed(it) }

    /** Removes a listener registered with [addActionListener] (only one if the same listener was registered several times). */
    @JvmSynthetic
    fun removeActionListener(listener: (WActionEvent) -> Unit) = actions.remove(listener)

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: Runnable) = actions.remove(listener)

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: WActionListener) = actions.remove(listener)

    /** JComboBox.actionPerformed-style: calls the registered listeners in registration order, as when a query is committed. */
    fun postActionEvent() = actions.post()

    /** Calls the listeners with the committed query [queryText] as the default [actionCommand]. */
    private fun postSubmitted(queryText: String) {
        submittedQuery = queryText
        try {
            actions.post()
        } finally {
            submittedQuery = null
        }
    }

    private fun addAction(key: Any, invoke: (WActionEvent) -> Unit) {
        if (!actionSubscribed) {
            actionSubscribed = true
            // The handler stays on the native object, so the box is not captured strongly but looked up from listenerAnchor
            val anchor = listenerAnchor
            inspectable.addEventHandler(
                "WinUI4K.AutoSuggestBoxActionHandler",
                XamlInterop.IID_AutoSuggestBoxQuerySubmittedHandler,
                XamlInterop.IAutoSuggestBox_add_QuerySubmitted,
            ) { _, args ->
                val queryText = ComPtr(args).getString(XamlInterop.IAutoSuggestBoxQuerySubmittedEventArgs_get_QueryText)
                (anchor.get() as? WAutoSuggestBox)?.postSubmitted(queryText)
            }
        }
        actions.add(key, invoke)
    }

    /**
     * Subscribes to text changes (AutoSuggestBox.TextChanged).
     * The listener receives the text after the change and the reason for the change.
     * As a rule, only filter suggestions when the reason is [TextChangeReason.USER_INPUT].
     */
    @JvmSynthetic
    fun addTextChangedListener(listener: (String, TextChangeReason) -> Unit) {
        val adapter = BiConsumer<String, TextChangeReason> { first, second -> listener(first, second) }
        addTextChangedListenerForJava(adapter)
        textChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addTextChangedListener")
    fun addTextChangedListenerForJava(listener: BiConsumer<String, TextChangeReason>) {
        // The handler stays on the native object, so the box is not captured strongly but looked up from listenerAnchor
        val anchor = listenerAnchor
        val token = inspectable.addEventHandler(
            "WinUI4K.AutoSuggestBoxTextChangedHandler",
            XamlInterop.IID_AutoSuggestBoxTextChangedHandler,
            XamlInterop.IAutoSuggestBox_add_TextChanged,
        ) { _, args ->
            // args is AutoSuggestBoxTextChangedEventArgs; read Reason and pass it along
            val reason = TextChangeReason.of(
                ComPtr(args).getInt(XamlInterop.IAutoSuggestBoxTextChangedEventArgs_get_Reason),
            )
            val box = anchor.get() as? WAutoSuggestBox
            if (box != null) listener.accept(box.text, reason)
        }
        textChangedTokens.add(listener, token)
        anchor.listenerAdded()
    }

    /** Unsubscribes a listener registered via [addTextChangedListener]. */
    @JvmSynthetic
    fun removeTextChangedListener(listener: (String, TextChangeReason) -> Unit) {
        val adapter = textChangedTokens.removeKotlinAdapter(listener) ?: return
        removeTextChangedListenerForJava(adapter)
    }

    @JvmName("removeTextChangedListener")
    fun removeTextChangedListenerForJava(listener: BiConsumer<String, TextChangeReason>) {
        val token = textChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IAutoSuggestBox_remove_TextChanged, token)
        listenerAnchor.listenerRemoved()
    }

    /**
     * Subscribes to confirmation via Enter or choosing a suggestion (AutoSuggestBox.QuerySubmitted).
     * The listener receives the entered text, and if confirmed from a suggestion, that suggestion (null otherwise).
     */
    @JvmSynthetic
    fun addQuerySubmittedListener(listener: (String, String?) -> Unit) {
        val adapter = BiConsumer<String, String?> { first, second -> listener(first, second) }
        addQuerySubmittedListenerForJava(adapter)
        querySubmittedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addQuerySubmittedListener")
    fun addQuerySubmittedListenerForJava(listener: BiConsumer<String, String?>) {
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
            listener.accept(queryText, chosen)
        }
        querySubmittedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addQuerySubmittedListener]. */
    @JvmSynthetic
    fun removeQuerySubmittedListener(listener: (String, String?) -> Unit) {
        val adapter = querySubmittedTokens.removeKotlinAdapter(listener) ?: return
        removeQuerySubmittedListenerForJava(adapter)
    }

    @JvmName("removeQuerySubmittedListener")
    fun removeQuerySubmittedListenerForJava(listener: BiConsumer<String, String?>) {
        val token = querySubmittedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IAutoSuggestBox_remove_QuerySubmitted, token)
    }

    /** Subscribes to suggestion highlight changes (AutoSuggestBox.SuggestionChosen). The listener receives the suggestion string. */
    @JvmSynthetic
    fun addSuggestionChosenListener(listener: (String) -> Unit) {
        val adapter = Consumer<String> { listener(it) }
        addSuggestionChosenListenerForJava(adapter)
        suggestionChosenTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addSuggestionChosenListener")
    fun addSuggestionChosenListenerForJava(listener: Consumer<String>) {
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
            listener.accept(selected)
        }
        suggestionChosenTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addSuggestionChosenListener]. */
    @JvmSynthetic
    fun removeSuggestionChosenListener(listener: (String) -> Unit) {
        val adapter = suggestionChosenTokens.removeKotlinAdapter(listener) ?: return
        removeSuggestionChosenListenerForJava(adapter)
    }

    @JvmName("removeSuggestionChosenListener")
    fun removeSuggestionChosenListenerForJava(listener: Consumer<String>) {
        val token = suggestionChosenTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IAutoSuggestBox_remove_SuggestionChosen, token)
    }
}
