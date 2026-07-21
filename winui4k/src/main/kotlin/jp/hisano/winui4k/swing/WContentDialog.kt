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
 * Microsoft.UI.Xaml.Controls.ContentDialogResult (which button it closed with).
 * Values extracted from the winmd (None=0, Primary=1, Secondary=2).
 */
enum class ContentDialogResult(internal val native: Int) {
    /** Closed by something other than a button (e.g. the Esc key or a close button). */
    NONE(0),

    /** Closed with the primary button. */
    PRIMARY(1),

    /** Closed with the secondary button. */
    SECONDARY(2),
    ;

    internal companion object {
        fun of(native: Int): ContentDialogResult = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.ContentDialogButton (which button the Enter key presses).
 * Values extracted from the winmd (None=0, Primary=1, Secondary=2, Close=3).
 */
enum class ContentDialogButton(internal val native: Int) {
    /** No default button (default). */
    NONE(0),

    /** Make the primary button the default. */
    PRIMARY(1),

    /** Make the secondary button the default. */
    SECONDARY(2),

    /** Make the close button the default. */
    CLOSE(3),
    ;

    internal companion object {
        fun of(native: Int): ContentDialogButton = entries.first { it.native == native }
    }
}

/**
 * JOptionPane-like: WinUI 3's ContentDialog. A modal dialog shown layered inside the window.
 * [show] opens it, and the button it closed with ([ContentDialogResult]) is delivered via a callback.
 */
class WContentDialog(title: String = "", content: WComponent? = null) : WControl(
    Activation.composeDefault(Abi.CLS_ContentDialog, Abi.IID_IContentDialogFactory), // default interface = IContentDialog
) {
    private val contentControl: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IContentControl)
    }

    /** The heading at the top of the dialog (ContentDialog.Title). Object-typed, so a boxed string is passed. */
    var title: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(Abi.IContentDialog_put_Title, boxed.ptr)
            boxed.release()
        }

    /** The dialog's content (ContentControl.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(
                Abi.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    /** The primary button's label. Left empty, the button itself isn't shown. */
    var primaryButtonText: String
        get() = inspectable.getString(Abi.IContentDialog_get_PrimaryButtonText)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IContentDialog_put_PrimaryButtonText, h) }

    /** The secondary button's label. Left empty, the button itself isn't shown. */
    var secondaryButtonText: String
        get() = inspectable.getString(Abi.IContentDialog_get_SecondaryButtonText)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IContentDialog_put_SecondaryButtonText, h) }

    /** The close button's label. Left empty, the button itself isn't shown. */
    var closeButtonText: String
        get() = inspectable.getString(Abi.IContentDialog_get_CloseButtonText)
        set(value) = Hstring.use(value) { h -> inspectable.call(Abi.IContentDialog_put_CloseButtonText, h) }

    /** Whether the primary button is pressable (ContentDialog.IsPrimaryButtonEnabled). */
    var isPrimaryButtonEnabled: Boolean
        get() = inspectable.getBool(Abi.IContentDialog_get_IsPrimaryButtonEnabled)
        set(value) = inspectable.putBool(Abi.IContentDialog_put_IsPrimaryButtonEnabled, value)

    /** Whether the secondary button is pressable (ContentDialog.IsSecondaryButtonEnabled). */
    var isSecondaryButtonEnabled: Boolean
        get() = inspectable.getBool(Abi.IContentDialog_get_IsSecondaryButtonEnabled)
        set(value) = inspectable.putBool(Abi.IContentDialog_put_IsSecondaryButtonEnabled, value)

    /** The default button pressed by the Enter key (ContentDialog.DefaultButton). Also gets emphasized styling. */
    var defaultButton: ContentDialogButton
        get() = ContentDialogButton.of(inspectable.getInt(Abi.IContentDialog_get_DefaultButton))
        set(value) = inspectable.call(Abi.IContentDialog_put_DefaultButton, value.native)

    init {
        if (title.isNotEmpty()) this.title = title
        if (content != null) this.content = content
    }

    /**
     * Opens the dialog (ContentDialog.ShowAsync). Shown in the same window as [owner]
     * (inherits its XamlRoot). When closed, [onClosed] is called with the button that was pressed.
     */
    fun show(owner: WComponent, onClosed: ((ContentDialogResult) -> Unit)? = null) {
        // WinUI 3's ContentDialog requires the XamlRoot to show in
        val root = owner.uiElement.getPtr(Abi.IUIElement_get_XamlRoot)
        uiElement.call(Abi.IUIElement_put_XamlRoot, root.ptr)
        root.release()

        if (onClosed != null) {
            // A one-shot subscription: unsubscribe its own token before reporting the result once closed
            var token = 0L
            token = inspectable.addEventHandler(
                "WinUI4K.ContentDialogClosedHandler",
                Abi.IID_ContentDialogClosedHandler,
                Abi.IContentDialog_add_Closed,
            ) { _, args ->
                inspectable.removeEventHandler(Abi.IContentDialog_remove_Closed, token)
                onClosed(ContentDialogResult.of(ComPtr(args).getInt(Abi.IContentDialogClosedEventArgs_get_Result)))
            }
        }

        // Completion (= the dialog closing) is received via the Closed event, so the async object isn't used
        inspectable.getPtr(Abi.IContentDialog_ShowAsync).release()
    }

    /** Closes the dialog programmatically (ContentDialog.Hide). The result is [ContentDialogResult.NONE]. */
    fun hide() {
        inspectable.call(Abi.IContentDialog_Hide)
    }
}
