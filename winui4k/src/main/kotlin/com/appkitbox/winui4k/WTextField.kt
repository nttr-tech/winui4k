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

    /** A reference for looking up this field from the Enter KeyDown handler (created when adding the first ActionListener). */
    private val listenerAnchor by lazy { ListenerAnchor(this).also { enterKeyTarget.anchor = it } }

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
    var anchor: ListenerAnchor<WTextField>? = null

    /**
     * On Enter with no modifier keys, if [sender] (TextBox) is single-line (AcceptsReturn = false), calls the listeners or
     * presses the default button and marks the event handled. Since it is marked handled, the default button's Enter
     * accelerator does not press it again.
     */
    fun onKeyDown(sender: Ptr, event: XamlKeyEvent) {
        if (event.key != VirtualKey.ENTER.native || Xaml.currentModifiers() != 0) return
        val textBox = ComPtr(sender).queryInterface(XamlInterop.IID_ITextBox)
        try {
            if (textBox.getBool(XamlInterop.ITextBox_get_AcceptsReturn)) return
            val field = anchor?.get()
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
