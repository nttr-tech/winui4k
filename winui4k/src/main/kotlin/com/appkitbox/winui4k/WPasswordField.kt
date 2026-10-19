package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Microsoft.UI.Xaml.Controls.PasswordRevealMode (how the password is displayed).
 * Values extracted from the winmd (Peek=0, Hidden=1, Visible=2).
 */
enum class PasswordRevealMode(internal val native: Int) {
    /** Shows the text only while the eye button is pressed (the WinUI default; the [WPasswordField] default is [HIDDEN]). */
    PEEK(0),

    /** Always shows echo characters and no eye button (the [WPasswordField] default, same as Swing's JPasswordField). */
    HIDDEN(1),

    /** Always shown in plain text. */
    VISIBLE(2),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): PasswordRevealMode = entries.first { it.native == native }
    }
}

/**
 * JPasswordField-like: WinUI 3's PasswordBox.
 *
 * As in Swing, by default only echo characters are shown and no eye button ([passwordRevealMode] defaults to
 * [PasswordRevealMode.HIDDEN]; the WinUI default PEEK shows the eye button). In Java, [getPassword] returns a `char[]`,
 * the same as JPasswordField.
 * As a single-line text field, Enter calls the [addActionListener] listeners, and if there are none, the default button
 * of the same window ([WFrame.defaultButton]) is pressed (same as [WTextField]).
 *
 * The password is kept in the PasswordBox as a WinRT string (HSTRING) and is also copied into a JVM string on every read.
 * Filling the array received from [getPassword] with 0 after use, as with Swing's JPasswordField, cannot erase these copies
 * (immutable strings cannot be overwritten, and PasswordBox exposes no way to zero out its string). It shortens the time
 * the password stays in memory, but cannot guarantee that the password is unreadable from a memory dump or the like.
 */
class WPasswordField @JvmOverloads constructor(placeholder: String = "") : WControl(
    Activation.activate(XamlInterop.CLS_PasswordBox, XamlInterop.IID_IPasswordBox),
) {
    /** PasswordChanged event tokens registered via addPasswordChangedListener. */
    private val passwordChangedTokens = ListenerTokens<Consumer<String>>()

    /** The list of ActionListener-like listeners and the handling of the Enter KeyDown ([ActionEvents]). PasswordBox is single-line, so Enter is not a line break. */
    private val actions = ActionEvents(this, { actionCommand }, { false })

    /** DocumentListener-like listeners (subscribes to PasswordBox.PasswordChanging when the first listener is added). */
    private val document = DocumentEvents(this, ::nativePassword, ::subscribePasswordChanging)

    private fun nativePassword(): String = inspectable.getString(XamlInterop.IPasswordBox_get_Password)

    /**
     * The entered password (PasswordBox.Password). When set, the [addDocumentListener] listeners receive, as with
     * [WTextField.text], removeUpdate for the whole old string followed by insertUpdate for the whole new string (nothing
     * if the string is the same).
     *
     * From Java, `getPassword()` returns a `char[]` as in JPasswordField, so this getter returning a string is not visible
     * from Java (it is kept on the JVM as a synthetic method, so calls compiled against earlier versions still work). When
     * you need a string in Java, write `new String(field.getPassword())`.
     */
    @get:JvmSynthetic
    var password: String
        get() = nativePassword()
        set(value) {
            document.replaceAll { Hstring.use(value) { h -> inspectable.call(XamlInterop.IPasswordBox_put_Password, h) } }
        }

    /**
     * JPasswordField.getPassword-like: returns the entered password as a new `char[]` (a different array on every call).
     * You may fill it with 0 after use, but the WinRT string cannot be erased (see the class description).
     */
    fun getPassword(): CharArray = nativePassword().toCharArray()

    /** JTextComponent.setText-like: same as setting [password]. null is an empty string. */
    fun setText(text: String?) {
        password = text.orEmpty()
    }

    /** The character used for echoing (PasswordBox.PasswordChar). Defaults to "●". [echoChar] reads and writes the first character of this string as a Char. */
    var passwordChar: String
        get() = inspectable.getString(XamlInterop.IPasswordBox_get_PasswordChar)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IPasswordBox_put_PasswordChar, h) }

    /**
     * JPasswordField.getEchoChar / setEchoChar-like: the character shown in place of the typed characters. Defaults to
     * PasswordBox's "●" (Swing's default is "•"). Setting `'\u0000'` shows the typed characters as they are, as in Swing
     * (it sets [passwordRevealMode] to [PasswordRevealMode.VISIBLE]). Setting anything other than `'\u0000'` sets
     * [passwordChar] to that character and, if it was [PasswordRevealMode.VISIBLE], returns to [PasswordRevealMode.HIDDEN]
     * (the eye button's [PasswordRevealMode.PEEK] is kept).
     * Reads return `'\u0000'` while [passwordRevealMode] is [PasswordRevealMode.VISIBLE].
     */
    var echoChar: Char
        get() = if (passwordRevealMode == PasswordRevealMode.VISIBLE) NO_ECHO else passwordChar.firstOrNull() ?: DEFAULT_ECHO
        set(value) {
            if (value == NO_ECHO) {
                passwordRevealMode = PasswordRevealMode.VISIBLE
                return
            }
            passwordChar = value.toString()
            if (passwordRevealMode == PasswordRevealMode.VISIBLE) passwordRevealMode = PasswordRevealMode.HIDDEN
        }

    /** JPasswordField.echoCharIsSet-like: whether echo characters are shown in place of the typed characters (whether [echoChar] is not `'\u0000'`). */
    fun echoCharIsSet(): Boolean = echoChar != NO_ECHO

    /**
     * JTextField.setActionCommand-like: the string put into [WActionEvent.actionCommand] of the listeners called on Enter
     * (null by default).
     * JTextField returns the entered text when unset, but a password field returns null so that the password does not
     * spread through events.
     */
    var actionCommand: String? = null

    /** The placeholder shown when empty (PasswordBox.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(XamlInterop.IPasswordBox_get_PlaceholderText)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IPasswordBox_put_PlaceholderText, h) }

    /** The header shown above the password box (PasswordBox.Header). It's an Object, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IPasswordBox_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The maximum number of characters that can be entered. 0 means unlimited (PasswordBox.MaxLength). */
    var maxLength: Int
        get() = inspectable.getInt(XamlInterop.IPasswordBox_get_MaxLength)
        set(value) = inspectable.call(XamlInterop.IPasswordBox_put_MaxLength, value)

    /**
     * How the password is shown (PasswordBox.PasswordRevealMode). Defaults to [PasswordRevealMode.HIDDEN] (no eye button,
     * as in Swing's JPasswordField; the WinUI default [PasswordRevealMode.PEEK] shows the eye button).
     */
    var passwordRevealMode: PasswordRevealMode
        get() = PasswordRevealMode.of(inspectable.getInt(XamlInterop.IPasswordBox_get_PasswordRevealMode))
        set(value) = inspectable.call(XamlInterop.IPasswordBox_put_PasswordRevealMode, value.native)

    init {
        if (placeholder.isNotEmpty()) placeholderText = placeholder
        passwordRevealMode = PasswordRevealMode.HIDDEN
        actions.install()
    }

    /** Selects all text (PasswordBox.SelectAll). */
    fun selectAll() {
        inspectable.call(XamlInterop.IPasswordBox_SelectAll)
    }

    /**
     * JTextComponent.paste-like: replaces the password with the clipboard text (PasswordBox.PasteFromClipboard).
     * Reading the clipboard is asynchronous, and the password changes after the call (in the message loop).
     * JPasswordField's copy and cut do nothing (the password is never put on the clipboard). PasswordBox is the same, so
     * they are not provided.
     */
    fun paste() {
        inspectable.call(XamlInterop.IPasswordBox_PasteFromClipboard)
    }

    /**
     * ActionListener-like: called when Enter is pressed without modifier keys (same as JTextField.addActionListener).
     * The listener receives a [WActionEvent] whose source is this field. While there are listeners, Enter does not press
     * the default button.
     */
    @JvmSynthetic
    fun addActionListener(listener: (WActionEvent) -> Unit) = actions.add(listener, listener)

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: Runnable) = actions.add(listener) { listener.run() }

    @JvmName("addActionListener")
    fun addActionListenerForJava(listener: WActionListener) = actions.add(listener) { listener.actionPerformed(it) }

    /** Removes a listener registered with [addActionListener] (only one if the same listener was registered several times). */
    @JvmSynthetic
    fun removeActionListener(listener: (WActionEvent) -> Unit) = actions.remove(listener)

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: Runnable) = actions.remove(listener)

    @JvmName("removeActionListener")
    fun removeActionListenerForJava(listener: WActionListener) = actions.remove(listener)

    /** JTextField.postActionEvent-like: calls the registered listeners in registration order, as when Enter is pressed. Does not press the default button. */
    fun postActionEvent() = actions.post()

    /**
     * Document.addDocumentListener-like: adds a listener called when the password changes. Does nothing if null.
     * Notifies in the same way as [WTextField.addDocumentListener] (key input and pastes are detected via
     * PasswordBox.PasswordChanging, and the difference from the previous string is reported with removeUpdate and
     * insertUpdate; changes via [password] and [setText] are removeUpdate for the whole old text followed by insertUpdate
     * for the whole new text). PasswordBox does not expose the caret position, so the position of an edit within a run of
     * the same character is treated as the right end of the run (typing a anywhere in "aa" gives offset 2).
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

    /** Subscribes to PasswordBox.PasswordChanging. The handler stays in the native object, so the field is looked up from [listenerAnchor]. */
    private fun subscribePasswordChanging() {
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.DocumentPasswordChanging",
            XamlInterop.IID_PasswordBoxPasswordChangingHandler,
            XamlInterop.IPasswordBox_add_PasswordChanging,
        ) { _, _ -> (anchor.get() as? WPasswordField)?.document?.onNativeChange(-1) }
    }

    /** Subscribes to password changes (PasswordBox.PasswordChanged). The listener receives the password after the change. */
    @JvmSynthetic
    fun addPasswordChangedListener(listener: (String) -> Unit) {
        val adapter = Consumer<String> { listener(it) }
        addPasswordChangedListenerForJava(adapter)
        passwordChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addPasswordChangedListener")
    fun addPasswordChangedListenerForJava(listener: Consumer<String>) {
        val token = inspectable.addEventHandler(
            "WinUI4K.PasswordChangedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IPasswordBox_add_PasswordChanged,
        ) { _, _ -> listener.accept(password) }
        passwordChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addPasswordChangedListener]. */
    @JvmSynthetic
    fun removePasswordChangedListener(listener: (String) -> Unit) {
        val adapter = passwordChangedTokens.removeKotlinAdapter(listener) ?: return
        removePasswordChangedListenerForJava(adapter)
    }

    @JvmName("removePasswordChangedListener")
    fun removePasswordChangedListenerForJava(listener: Consumer<String>) {
        val token = passwordChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IPasswordBox_remove_PasswordChanged, token)
    }

    private companion object {
        /** setEchoChar('\u0000'). The marker for showing typed characters without echo characters (as in Swing). */
        const val NO_ECHO = '\u0000'

        /** PasswordBox's default echo character (U+25CF). */
        const val DEFAULT_ECHO = '●'
    }
}
