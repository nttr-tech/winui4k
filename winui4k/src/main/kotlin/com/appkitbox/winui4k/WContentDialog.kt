package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.function.Consumer

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
        @JvmSynthetic
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
        @JvmSynthetic
        fun of(native: Int): ContentDialogButton = entries.first { it.native == native }
    }
}

/**
 * JOptionPane-like: WinUI 3's ContentDialog. A modal dialog shown layered inside the window.
 * [show] opens it, and the button it closed with ([ContentDialogResult]) is delivered via a callback.
 */
class WContentDialog @JvmOverloads constructor(title: String = "", content: WComponent? = null) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ContentDialog, XamlInterop.IID_IContentDialogFactory), // default interface = IContentDialog
) {
    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /** The heading at the top of the dialog (ContentDialog.Title). Object-typed, so a boxed string is passed. */
    var title: String = ""
        set(value) {
            field = value
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IContentDialog_put_Title, boxed.ptr)
            boxed.release()
        }

    /**
     * The dialog's content (ContentControl.Content). The dialog does not scroll its content (the template's
     * ScrollViewer has vertical scrolling disabled), so content that may be taller than the window, such as a long
     * form, should be wrapped in a [WScrollPane].
     */
    var content: WComponent? = null
        set(value) {
            field = value
            contentControl.call(
                XamlInterop.IContentControl_put_Content,
                value?.uiElement?.ptr,
            )
        }

    /** The primary button's label. Left empty, the button itself isn't shown. */
    var primaryButtonText: String
        get() = inspectable.getString(XamlInterop.IContentDialog_get_PrimaryButtonText)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IContentDialog_put_PrimaryButtonText, h) }

    /** The secondary button's label. Left empty, the button itself isn't shown. */
    var secondaryButtonText: String
        get() = inspectable.getString(XamlInterop.IContentDialog_get_SecondaryButtonText)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IContentDialog_put_SecondaryButtonText, h) }

    /** The close button's label. Left empty, the button itself isn't shown. */
    var closeButtonText: String
        get() = inspectable.getString(XamlInterop.IContentDialog_get_CloseButtonText)
        set(value) = Hstring.use(value) { h -> inspectable.call(XamlInterop.IContentDialog_put_CloseButtonText, h) }

    /** Whether the primary button is pressable (ContentDialog.IsPrimaryButtonEnabled). */
    var isPrimaryButtonEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.IContentDialog_get_IsPrimaryButtonEnabled)
        set(value) = inspectable.putBool(XamlInterop.IContentDialog_put_IsPrimaryButtonEnabled, value)

    /** Whether the secondary button is pressable (ContentDialog.IsSecondaryButtonEnabled). */
    var isSecondaryButtonEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.IContentDialog_get_IsSecondaryButtonEnabled)
        set(value) = inspectable.putBool(XamlInterop.IContentDialog_put_IsSecondaryButtonEnabled, value)

    /** The default button pressed by the Enter key (ContentDialog.DefaultButton). Also gets emphasized styling. */
    var defaultButton: ContentDialogButton
        get() = ContentDialogButton.of(inspectable.getInt(XamlInterop.IContentDialog_get_DefaultButton))
        set(value) = inspectable.call(XamlInterop.IContentDialog_put_DefaultButton, value.native)

    /**
     * The maximum width of the dialog (DIP). NaN (the default) keeps the theme's ContentDialogMaxWidth (548); a wide
     * form needs more. The dialog still sizes itself to its content up to this width, and never beyond the window.
     *
     * The inherited [maxWidth] (FrameworkElement.MaxWidth) cannot widen the dialog: its template reads the
     * ContentDialogMaxWidth theme resource, so this puts that resource in the dialog's own Resources. The template
     * reads it when the dialog is first shown, so set this before [show].
     */
    var dialogMaxWidth: Double = Double.NaN
        set(value) {
            field = value
            putResource(MAX_WIDTH_RESOURCE, if (value.isNaN()) null else value)
        }

    /** Puts [value] (null removes the key) under [key] in the dialog's own Resources (FrameworkElement.Resources). */
    private fun putResource(key: String, value: Double?) {
        val resources = frameworkElement.getPtr(XamlInterop.IFrameworkElement_get_Resources)
        val map = resources.queryInterface(FoundationInterop.IID_IMap_Object_Object)
        resources.release()
        val boxedKey = PropertyValues.boxString(key)
        try {
            Ffi.backend.withScope { scope ->
                val flag = scope.allocate(1, 1)
                if (value == null) {
                    map.call(FoundationInterop.IMap_HasKey, boxedKey.ptr, flag)
                    if (Ffi.backend.memory.getByte(flag, 0) != 0.toByte()) map.call(FoundationInterop.IMap_Remove, boxedKey.ptr)
                } else {
                    val boxed = PropertyValues.boxDouble(value)
                    try {
                        map.call(FoundationInterop.IMap_Insert, boxedKey.ptr, boxed.ptr, flag)
                    } finally {
                        boxed.release()
                    }
                }
            }
        } finally {
            boxedKey.release()
            map.release()
        }
    }

    init {
        if (title.isNotEmpty()) this.title = title
        if (content != null) this.content = content
    }

    /**
     * Opens the dialog (ContentDialog.ShowAsync). Shown in the same window as [owner]
     * (inherits its XamlRoot). When closed, [onClosed] is called with the button that was pressed.
     */
    @JvmOverloads
    fun show(owner: WComponent, onClosed: Consumer<ContentDialogResult>? = null) {
        // WinUI 3's ContentDialog requires the XamlRoot to show in
        val root = owner.uiElement.getPtr(XamlInterop.IUIElement_get_XamlRoot)
        uiElement.call(XamlInterop.IUIElement_put_XamlRoot, root.ptr)
        root.release()

        if (onClosed != null) {
            // A one-shot subscription: unsubscribe its own token before reporting the result once closed
            var token = 0L
            token = inspectable.addEventHandler(
                "WinUI4K.ContentDialogClosedHandler",
                XamlInterop.IID_ContentDialogClosedHandler,
                XamlInterop.IContentDialog_add_Closed,
            ) { _, args ->
                inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_Closed, token)
                onClosed.accept(ContentDialogResult.of(ComPtr(args).getInt(XamlInterop.IContentDialogClosedEventArgs_get_Result)))
            }
        }

        // Completion (= the dialog closing) is received via the Closed event, so the async object isn't used
        inspectable.getPtr(XamlInterop.IContentDialog_ShowAsync).release()
    }

    /** Closes the dialog programmatically (ContentDialog.Hide). The result is [ContentDialogResult.NONE]. */
    fun hide() {
        inspectable.call(XamlInterop.IContentDialog_Hide)
    }

    private companion object {
        /** The theme resource that ContentDialog's template uses as the MaxWidth of the dialog. */
        const val MAX_WIDTH_RESOURCE = "ContentDialogMaxWidth"
    }
}
