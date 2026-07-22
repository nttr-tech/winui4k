package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Controls.InfoBarSeverity (the color scheme and icon for a message's severity).
 * Values extracted from the winmd (Informational=0, Success=1, Warning=2, Error=3).
 */
enum class InfoBarSeverity(internal val native: Int) {
    /** Informational (blue, the default). */
    INFORMATIONAL(0),

    /** Success (green). */
    SUCCESS(1),

    /** Warning (yellow). */
    WARNING(2),

    /** Error (red). */
    ERROR(3),
    ;

    internal companion object {
        fun of(native: Int): InfoBarSeverity = entries.first { it.native == native }
    }
}

/**
 * WinUI 3's InfoBar (an in-app inline notification bar). No Swing equivalent, so we keep
 * WinUI's class name as-is. Unlike a ContentDialog, it doesn't block user interaction and keeps
 * showing a state change (a save completing, a connection error, etc.) in the content area.
 *
 * [title] / [message] / [severity] control the content and appearance, and [isOpen] shows or
 * hides it (closed by default, so set isOpen = true to display it). It can also have an
 * [isClosable] close (x) button and an [actionButton] action button.
 */
class WInfoBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_InfoBar, XamlInterop.IID_IInfoBarFactory), // default interface = IInfoBar
) {
    /** CloseButtonClick event tokens registered via addCloseButtonListener. */
    private val closeButtonTokens = ListenerTokens<() -> Unit>()

    private var actionButtonComponent: WButtonBase? = null

    /**
     * Whether the bar is shown (InfoBar.IsOpen). Defaults to false, so set it to true to show it.
     * Reverts to false when the close (x) button is clicked or it's closed from code.
     */
    var isOpen: Boolean
        get() = inspectable.getBool(XamlInterop.IInfoBar_get_IsOpen)
        set(value) = inspectable.putBool(XamlInterop.IInfoBar_put_IsOpen, value)

    /** The heading (InfoBar.Title). Shown in bold at the start. */
    var title: String
        get() = inspectable.getString(XamlInterop.IInfoBar_get_Title)
        set(value) = Hstring.use(value) { hstring -> inspectable.call(XamlInterop.IInfoBar_put_Title, hstring) }

    /** The body text (InfoBar.Message). */
    var message: String
        get() = inspectable.getString(XamlInterop.IInfoBar_get_Message)
        set(value) = Hstring.use(value) { hstring -> inspectable.call(XamlInterop.IInfoBar_put_Message, hstring) }

    /** The severity (InfoBar.Severity). Changes the color scheme and default icon. */
    var severity: InfoBarSeverity
        get() = InfoBarSeverity.of(inspectable.getInt(XamlInterop.IInfoBar_get_Severity))
        set(value) = inspectable.call(XamlInterop.IInfoBar_put_Severity, value.native)

    /** Whether the default icon for the severity is shown (InfoBar.IsIconVisible). */
    var isIconVisible: Boolean
        get() = inspectable.getBool(XamlInterop.IInfoBar_get_IsIconVisible)
        set(value) = inspectable.putBool(XamlInterop.IInfoBar_put_IsIconVisible, value)

    /** Whether the close (x) button is shown (InfoBar.IsClosable, default true). */
    var isClosable: Boolean
        get() = inspectable.getBool(XamlInterop.IInfoBar_get_IsClosable)
        set(value) = inspectable.putBool(XamlInterop.IInfoBar_put_IsClosable, value)

    /**
     * An action button placed to the right of the body text (InfoBar.ActionButton).
     * Pass a ButtonBase subclass (WButton / WHyperlinkButton, etc.). null to remove it.
     */
    var actionButton: WButtonBase?
        get() = actionButtonComponent
        set(value) {
            actionButtonComponent = value
            if (value == null) {
                inspectable.call(XamlInterop.IInfoBar_put_ActionButton, null)
            } else {
                val buttonBase = value.inspectable.queryInterface(XamlInterop.IID_IButtonBase)
                try {
                    inspectable.call(XamlInterop.IInfoBar_put_ActionButton, buttonBase.ptr)
                } finally {
                    buttonBase.release()
                }
            }
        }

    /** Arbitrary content placed below the message (InfoBar.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.IInfoBar_put_Content, value?.uiElement?.ptr)
        }

    /** Subscribes to clicks on the close (x) button (InfoBar.CloseButtonClick). */
    fun addCloseButtonListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.InfoBarCloseButtonClickHandler",
            XamlInterop.IID_InfoBarCloseButtonClickHandler,
            XamlInterop.IInfoBar_add_CloseButtonClick,
        ) { _, _ -> listener() }
        closeButtonTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCloseButtonListener]. */
    fun removeCloseButtonListener(listener: () -> Unit) {
        val token = closeButtonTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IInfoBar_remove_CloseButtonClick, token)
    }
}
