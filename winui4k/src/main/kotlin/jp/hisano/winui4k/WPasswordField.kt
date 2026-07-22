package jp.hisano.winui4k

import jp.hisano.winui4k.internal.winrt.Activation
import jp.hisano.winui4k.internal.winrt.Hstring
import jp.hisano.winui4k.internal.winrt.PropertyValues
import jp.hisano.winui4k.internal.winrt.addEventHandler
import jp.hisano.winui4k.internal.winrt.getString
import jp.hisano.winui4k.internal.winrt.removeEventHandler
import jp.hisano.winui4k.internal.winui.Abi

/**
 * Microsoft.UI.Xaml.Controls.PasswordRevealMode (how the password is displayed).
 * Values extracted from the winmd (Peek=0, Hidden=1, Visible=2).
 */
enum class PasswordRevealMode(internal val native: Int) {
    /** Shown only while the reveal button is pressed (default). */
    PEEK(0),

    /** Always shown masked, with no reveal button shown either. */
    HIDDEN(1),

    /** Always shown in plain text. */
    VISIBLE(2),
    ;

    internal companion object {
        fun of(native: Int): PasswordRevealMode = entries.first { it.native == native }
    }
}

/** JPasswordField-like: WinUI 3's PasswordBox. */
class WPasswordField(placeholder: String = "") : WControl(
    Activation.activate(Abi.CLS_PasswordBox).queryInterface(Abi.IID_IPasswordBox),
) {
    /** PasswordChanged event tokens registered via addPasswordChangedListener. */
    private val passwordChangedTokens = ListenerTokens<(String) -> Unit>()

    /** The entered password (PasswordBox.Password). */
    var password: String
        get() = inspectable.getString(Abi.IPasswordBox_get_Password)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IPasswordBox_put_Password, h) }

    /** The character used for masking (PasswordBox.PasswordChar). Defaults to "●". */
    var passwordChar: String
        get() = inspectable.getString(Abi.IPasswordBox_get_PasswordChar)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IPasswordBox_put_PasswordChar, h) }

    /** The placeholder shown when empty (PasswordBox.PlaceholderText). */
    var placeholderText: String
        get() = inspectable.getString(Abi.IPasswordBox_get_PlaceholderText)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IPasswordBox_put_PlaceholderText, h) }

    /** The header shown above the password box (PasswordBox.Header). It's an Object, so a boxed string is passed. */
    var header: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.IPasswordBox_put_Header, boxed.ptr)
            boxed.release()
        }

    /** The maximum number of characters that can be entered. 0 means unlimited (PasswordBox.MaxLength). */
    var maxLength: Int
        get() = inspectable.getInt(Abi.IPasswordBox_get_MaxLength)
        set(value) = inspectable.call(Abi.IPasswordBox_put_MaxLength, value)

    /** How the password is displayed (PasswordBox.PasswordRevealMode). */
    var passwordRevealMode: PasswordRevealMode
        get() = PasswordRevealMode.of(inspectable.getInt(Abi.IPasswordBox_get_PasswordRevealMode))
        set(value) = inspectable.call(Abi.IPasswordBox_put_PasswordRevealMode, value.native)

    init {
        if (placeholder.isNotEmpty()) placeholderText = placeholder
    }

    /** Selects all text (PasswordBox.SelectAll). */
    fun selectAll() {
        inspectable.call(Abi.IPasswordBox_SelectAll)
    }

    /** Subscribes to password changes (PasswordBox.PasswordChanged). The listener receives the password after the change. */
    fun addPasswordChangedListener(listener: (String) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.PasswordChangedHandler",
            Abi.IID_RoutedEventHandler,
            Abi.IPasswordBox_add_PasswordChanged,
        ) { _, _ -> listener(password) }
        passwordChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addPasswordChangedListener]. */
    fun removePasswordChangedListener(listener: (String) -> Unit) {
        val token = passwordChangedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.IPasswordBox_remove_PasswordChanged, token)
    }
}
