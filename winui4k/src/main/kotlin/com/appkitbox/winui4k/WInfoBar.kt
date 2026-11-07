package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.EventObject
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
        @JvmSynthetic
        fun of(native: Int): InfoBarSeverity = entries.first { it.native == native }
    }
}

/**
 * Microsoft.UI.Xaml.Controls.InfoBarCloseReason (how it was closed). Values extracted from winmd (CloseButton=0, Programmatic=1).
 */
enum class InfoBarCloseReason(internal val native: Int) {
    /** Closed with the × (close) button. */
    CLOSE_BUTTON(0),

    /** Closed programmatically ([WInfoBar.isOpen] = false). */
    PROGRAMMATIC(1),
    ;

    internal companion object {
        @JvmSynthetic
        fun of(native: Int): InfoBarCloseReason = entries.first { it.native == native }
    }
}

/**
 * An event telling that a [WInfoBar] is about to close (InfoBar.Closing). The source is the bar ([infoBar]).
 * Setting [isCancel] to true keeps it from closing (InfoBarClosingEventArgs.Cancel). With multiple listeners, the next listener receives
 * the value set by the previous one.
 */
class WInfoBarClosingEvent internal constructor(
    source: WInfoBar,
    /** The reason for closing. */
    val reason: InfoBarCloseReason,
    /** If set to true, the bar stays open instead of closing. */
    var isCancel: Boolean = false,
) : EventObject(source) {
    /** The bar the event came from (the same as [getSource]). */
    val infoBar: WInfoBar get() = source as WInfoBar
}

/**
 * WinUI 3's InfoBar (an in-app inline notification bar). No Swing equivalent, so we keep
 * WinUI's class name as-is. Unlike a ContentDialog, it doesn't block user interaction and keeps
 * showing a state change (a save completing, a connection error, etc.) in the content area.
 *
 * [title] / [message] / [severity] control the content and appearance, and [isOpen] shows or
 * hides it (closed by default, so set isOpen = true to display it). It can also have an
 * [isClosable] close (x) button and an [actionButton] action button.
 * The bar is shown and hidden with [isOpen], not [isVisible] (WComponent's visibility)
 * (a bar with isOpen = false draws nothing even if isVisible is true).
 * Before and after closing can be received with [addClosingListener] (cancelable) and [addCloseListener].
 */
class WInfoBar : WControl(
    Activation.composeDefault(XamlInterop.CLS_InfoBar, XamlInterop.IID_IInfoBarFactory), // default interface = IInfoBar
) {
    /** CloseButtonClick event tokens registered via addCloseButtonListener. */
    private val closeButtonTokens = ListenerTokens<Runnable>()
    private val closingTokens = ListenerTokens<Consumer<WInfoBarClosingEvent>>()
    private val closeTokens = ListenerTokens<Consumer<InfoBarCloseReason>>()

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

    /** The icon to the left of the title (InfoBar.IconSource). If null (default), the default icon of [severity]. */
    var icon: WIcon? = null
        set(value) {
            val source = value?.createIconSource()
            try {
                inspectable.call(XamlInterop.IInfoBar_put_IconSource, source?.ptr)
            } finally {
                source?.release()
            }
            field = value
        }

    /** Arbitrary content placed below the message (InfoBar.Content). */
    var content: WComponent? = null
        set(value) {
            field = value
            inspectable.call(XamlInterop.IInfoBar_put_Content, value?.uiElement?.ptr)
        }

    /** Subscribes to clicks on the close (x) button (InfoBar.CloseButtonClick). */
    @JvmSynthetic
    fun addCloseButtonListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addCloseButtonListener(adapter)
        closeButtonTokens.addKotlinAdapter(listener, adapter)
    }

    fun addCloseButtonListener(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.InfoBarCloseButtonClickHandler",
            XamlInterop.IID_InfoBarCloseButtonClickHandler,
            XamlInterop.IInfoBar_add_CloseButtonClick,
        ) { _, _ -> listener.run() }
        closeButtonTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCloseButtonListener]. */
    @JvmSynthetic
    fun removeCloseButtonListener(listener: () -> Unit) {
        val adapter = closeButtonTokens.removeKotlinAdapter(listener) ?: return
        removeCloseButtonListener(adapter)
    }

    fun removeCloseButtonListener(listener: Runnable) {
        val token = closeButtonTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IInfoBar_remove_CloseButtonClick, token)
    }

    /**
     * Registers a listener called before closing (InfoBar.Closing). Called both for the × button and for [isOpen] = false, and
     * setting [WInfoBarClosingEvent.isCancel] to true keeps it from closing.
     */
    @JvmSynthetic
    fun addClosingListener(listener: (WInfoBarClosingEvent) -> Unit) {
        val adapter = Consumer<WInfoBarClosingEvent> { listener(it) }
        addClosingListener(adapter)
        closingTokens.addKotlinAdapter(listener, adapter)
    }

    fun addClosingListener(listener: Consumer<WInfoBarClosingEvent>) {
        val anchor = listenerAnchor
        val token = inspectable.addEventHandler(
            "WinUI4K.InfoBarClosingHandler",
            XamlInterop.IID_InfoBarClosingHandler,
            XamlInterop.IInfoBar_add_Closing,
        ) { _, args ->
            (anchor.get() as? WInfoBar)?.let { bar ->
                val closingArgs = ComPtr(args) // Borrowed (not Released)
                val event = WInfoBarClosingEvent(
                    bar,
                    InfoBarCloseReason.of(closingArgs.getInt(XamlInterop.IInfoBarClosingEventArgs_get_Reason)),
                    closingArgs.getBool(XamlInterop.IInfoBarClosingEventArgs_get_Cancel),
                )
                listener.accept(event)
                closingArgs.putBool(XamlInterop.IInfoBarClosingEventArgs_put_Cancel, event.isCancel)
            }
        }
        closingTokens.add(listener, token)
        anchor.listenerAdded()
    }

    /** Unsubscribes a listener registered with [addClosingListener]. */
    @JvmSynthetic
    fun removeClosingListener(listener: (WInfoBarClosingEvent) -> Unit) {
        val adapter = closingTokens.removeKotlinAdapter(listener) ?: return
        removeClosingListener(adapter)
    }

    fun removeClosingListener(listener: Consumer<WInfoBarClosingEvent>) {
        val token = closingTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IInfoBar_remove_Closing, token)
        listenerAnchor.listenerRemoved()
    }

    /** Registers a listener called when closed (InfoBar.Closed). The reason for closing is passed. */
    @JvmSynthetic
    fun addCloseListener(listener: (InfoBarCloseReason) -> Unit) {
        val adapter = Consumer<InfoBarCloseReason> { listener(it) }
        addCloseListener(adapter)
        closeTokens.addKotlinAdapter(listener, adapter)
    }

    fun addCloseListener(listener: Consumer<InfoBarCloseReason>) {
        val token = inspectable.addEventHandler(
            "WinUI4K.InfoBarClosedHandler",
            XamlInterop.IID_InfoBarClosedHandler,
            XamlInterop.IInfoBar_add_Closed,
        ) { _, args ->
            listener.accept(InfoBarCloseReason.of(ComPtr(args).getInt(XamlInterop.IInfoBarClosedEventArgs_get_Reason)))
        }
        closeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered with [addCloseListener]. */
    @JvmSynthetic
    fun removeCloseListener(listener: (InfoBarCloseReason) -> Unit) {
        val adapter = closeTokens.removeKotlinAdapter(listener) ?: return
        removeCloseListener(adapter)
    }

    fun removeCloseListener(listener: Consumer<InfoBarCloseReason>) {
        val token = closeTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.IInfoBar_remove_Closed, token)
    }
}
