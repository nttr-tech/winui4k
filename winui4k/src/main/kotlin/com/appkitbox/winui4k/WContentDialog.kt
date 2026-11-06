package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.WindowsRuntimeException
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.ModalLoop
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
 * JDialog-style (modal only): WinUI 3's ContentDialog. A modal dialog shown overlaid inside the owner window.
 *
 * There are two ways to open it.
 * - [showAndWait] / [isVisible] = true: like Swing's modal JDialog.setVisible(true), blocks the caller until it is closed
 *   (a nested message loop on the UI thread; input, rendering and invokeLater keep working meanwhile) and returns the button pressed.
 *   It also returns [ContentDialogResult.NONE] when the owner window is closed and when the app exits (WM_QUIT).
 *   It cannot be called from inside layout notifications (SizeChanged, Loaded and other listeners) ([IllegalStateException]).
 * - [show]: opens it without blocking and receives the pressed button in a callback when it closes (not called when the owner window is closed).
 *
 * WinUI can open only one ContentDialog at a time in a window. Trying to open another while one is open
 * cannot nest as in Swing and throws [IllegalStateException]. Modeless dialogs cannot be created (modal = false is refused).
 *
 * Opening and closing can be received with [addDialogListener] (Opened, Closing, Closed) and buttons with [addButtonClickListener]; both can
 * cancel the closing (to validate input and so on).
 */
class WContentDialog @JvmOverloads constructor(title: String = "", content: WComponent? = null) : WControl(
    Activation.composeDefault(XamlInterop.CLS_ContentDialog, XamlInterop.IID_IContentDialogFactory), // default interface = IContentDialog
) {
    /**
     * Like JDialog(owner, title, modal): creates it with the owner [owner] (it opens in this component's window) and a title.
     * ContentDialog is modal only, so [IllegalArgumentException] if [modal] is false.
     */
    @JvmOverloads
    constructor(owner: WComponent?, title: String, modal: Boolean = true) : this(title) {
        require(modal) { "ContentDialog is modal only; a modeless dialog (modal = false) cannot be created" }
        this.owner = owner
    }

    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /** The state while it is open. Handlers left on the native side capture this, not the wrapper. */
    private val state = ShowState()

    /** The listeners added with [addDialogListener] and the tokens of Opened, Closing and Closed. */
    private val dialogListeners = ArrayList<Pair<WContentDialogListener, LongArray>>()

    /** The listeners added with [addButtonClickListener] and the tokens of the primary, secondary and close buttons. */
    private val buttonListeners = ArrayList<Pair<WContentDialogButtonListener, LongArray>>()
    private val kotlinButtonListeners = KotlinListenerAdapters<WContentDialogButtonListener>()

    /**
     * The owner (JDialog's owner). [showAndWait] (without arguments) and [isVisible] = true open the dialog in this component's window.
     * To make a window ([WFrame]) the owner, pass a component placed in that window ([WFrame.contentPane] and so on).
     */
    var owner: WComponent? = null

    /** The title at the top of the dialog (ContentDialog.Title). An empty string if something other than a string is in it (which WinUI4K cannot put in). */
    var title: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.IContentDialog_get_Title) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.IContentDialog_put_Title, boxed.ptr)
            boxed.release()
        }

    /** The dialog's content (ContentControl.Content). */
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

    /** Whether to show it at the full height of the window (ContentDialog.FullSizeDesired). False by default (sized to its content). */
    var isFullSizeDesired: Boolean
        get() = inspectable.getBool(XamlInterop.IContentDialog_get_FullSizeDesired)
        set(value) = inspectable.putBool(XamlInterop.IContentDialog_put_FullSizeDesired, value)

    /** Like JDialog.isModal: always true (ContentDialog is modal only). Setting false throws [IllegalArgumentException]. */
    var isModal: Boolean
        get() = true
        set(value) = require(value) { "ContentDialog is modal only and cannot be made modeless" }

    /**
     * Like JDialog.isVisible / setVisible: whether it is open. True from opening until it finishes closing (Closed), and false after the owner window
     * is closed. Not used in WComponent's sense (UIElement.Visibility).
     *
     * Setting true opens it in [owner]'s window and does not return until it is closed, as with Swing's modal JDialog ([showAndWait]).
     * [IllegalStateException] if [owner] is null. Does nothing if it is already open. Setting false closes it ([hide]).
     */
    override var isVisible: Boolean
        get() = state.isShowing()
        set(value) {
            if (value) {
                if (!state.isShowing()) showAndWait()
            } else {
                hide()
            }
        }

    init {
        if (title.isNotEmpty()) this.title = title
        if (content != null) this.content = content
        // When closed, restore the state and call show's callback. Added first so it runs before the user's dialogClosed.
        // The handler stays on the native side, so capture only the state, not the wrapper
        val state = state
        inspectable.addEventHandler(
            "WinUI4K.ContentDialogClosedHandler",
            XamlInterop.IID_ContentDialogClosedHandler,
            XamlInterop.IContentDialog_add_Closed,
        ) { _, args -> state.closed(ContentDialogResult.of(ComPtr(args).getInt(XamlInterop.IContentDialogClosedEventArgs_get_Result))) }
    }

    /**
     * Opens the dialog (ContentDialog.ShowAsync). Shown in the same window as [owner]
     * (inherits its XamlRoot). When closed, [onClosed] is called with the button that was pressed.
     * Does not block the caller (use [showAndWait] to wait until it closes). [onClosed] is not called when the owner window is closed.
     * [IllegalStateException] if another ContentDialog is open in the same window or this dialog is already open,
     * and [IllegalArgumentException] if [owner] is not placed in a window.
     */
    @JvmOverloads
    fun show(owner: WComponent, onClosed: Consumer<ContentDialogResult>? = null) {
        open(owner, onClosed)
    }

    /**
     * Like JDialog.setVisible(true): opens it in [owner]'s window, blocks the caller until it is closed, and returns the button pressed.
     * [ContentDialogResult.NONE] when the owner window is closed and when the app exits (WM_QUIT).
     * Call on the UI thread, outside layout notifications (otherwise [IllegalStateException], and the dialog does not open).
     * Other exceptions are the same as [show].
     */
    fun showAndWait(owner: WComponent): ContentDialogResult {
        ModalLoop.checkRunnable()
        var result: ContentDialogResult? = null
        open(owner) { result = it }
        val state = state
        ModalLoop.run { result != null || !state.isShowing() }
        return result ?: ContentDialogResult.NONE
    }

    /** Opens it in the window of the [owner] property and waits until it closes ([showAndWait]). [IllegalStateException] if [owner] is null. */
    fun showAndWait(): ContentDialogResult = showAndWait(
        checkNotNull(owner) { "there is no owner, so the window to open in is unknown; set owner or use showAndWait(owner)" },
    )

    private fun open(owner: WComponent, onClosed: Consumer<ContentDialogResult>?) {
        check(!state.isShowing()) { "this dialog is already open" }
        // WinUI 3's ContentDialog requires the XamlRoot to show in
        val root = requireNotNull(owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)) {
            "the owner is not placed in a window, so the dialog cannot be opened"
        }
        val rootContent = try {
            uiElement.call(XamlInterop.IUIElement_put_XamlRoot, root.ptr)
            root.getPtr(XamlInterop.IXamlRoot_get_Content)
        } finally {
            root.release()
        }
        try {
            // Completion (= the dialog closing) is received through the Closed event, so the async object is not used
            inspectable.getPtr(XamlInterop.IContentDialog_ShowAsync).release()
        } catch (e: WindowsRuntimeException) {
            rootContent.release()
            if (e.hresult == E_ASYNC_OPERATION_NOT_STARTED) {
                throw IllegalStateException(
                    "another ContentDialog is already open in this window; WinUI can open only one at a time in a window",
                    e,
                )
            }
            throw e
        }
        state.opened(rootContent, onClosed)
    }

    /** Closes the dialog programmatically (ContentDialog.Hide). The result is [ContentDialogResult.NONE]. */
    fun hide() {
        inspectable.call(XamlInterop.IContentDialog_Hide)
    }

    /** Like JDialog.dispose: closes it (same as [hide]). The dialog can be opened again. */
    fun dispose() {
        hide()
    }

    /**
     * WindowListener-style: adds a listener for opening and closing. Opened, Closing and Closed become dialogOpened, dialogClosing and dialogClosed.
     * Setting [WContentDialogEvent.isCancel] to true in dialogClosing keeps it from closing.
     */
    fun addDialogListener(listener: WContentDialogListener) {
        val anchor = listenerAnchor
        val opened = inspectable.addEventHandler(
            "WinUI4K.ContentDialogOpenedHandler",
            XamlInterop.IID_ContentDialogOpenedHandler,
            XamlInterop.IContentDialog_add_Opened,
        ) { _, _ ->
            (anchor.get() as? WContentDialog)?.let { listener.dialogOpened(WContentDialogEvent(it, ContentDialogResult.NONE)) }
        }
        val closing = inspectable.addEventHandler(
            "WinUI4K.ContentDialogClosingHandler",
            XamlInterop.IID_ContentDialogClosingHandler,
            XamlInterop.IContentDialog_add_Closing,
        ) { _, args ->
            (anchor.get() as? WContentDialog)?.let { dialog ->
                val closingArgs = ComPtr(args) // Borrowed (not Released)
                val event = WContentDialogEvent(
                    dialog,
                    ContentDialogResult.of(closingArgs.getInt(XamlInterop.IContentDialogClosingEventArgs_get_Result)),
                    closingArgs.getBool(XamlInterop.IContentDialogClosingEventArgs_get_Cancel),
                )
                listener.dialogClosing(event)
                closingArgs.putBool(XamlInterop.IContentDialogClosingEventArgs_put_Cancel, event.isCancel)
            }
        }
        val closed = inspectable.addEventHandler(
            "WinUI4K.ContentDialogClosedListenerHandler",
            XamlInterop.IID_ContentDialogClosedHandler,
            XamlInterop.IContentDialog_add_Closed,
        ) { _, args ->
            (anchor.get() as? WContentDialog)?.let { dialog ->
                val result = ContentDialogResult.of(ComPtr(args).getInt(XamlInterop.IContentDialogClosedEventArgs_get_Result))
                listener.dialogClosed(WContentDialogEvent(dialog, result))
            }
        }
        dialogListeners += listener to longArrayOf(opened, closing, closed)
        anchor.listenerAdded()
    }

    /** Removes a listener added with [addDialogListener] (does nothing if not registered). */
    fun removeDialogListener(listener: WContentDialogListener) {
        val index = dialogListeners.indexOfLast { it.first === listener }
        if (index < 0) return
        val tokens = dialogListeners.removeAt(index).second
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_Opened, tokens[0])
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_Closing, tokens[1])
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_Closed, tokens[2])
        listenerAnchor.listenerRemoved()
    }

    /**
     * Adds a listener called when the primary, secondary or close button is pressed, before closing
     * (PrimaryButtonClick, SecondaryButtonClick, CloseButtonClick; which one was pressed is [WContentDialogButtonEvent.button]).
     * Setting [WContentDialogButtonEvent.isCancel] to true keeps it from closing.
     */
    @JvmSynthetic
    fun addButtonClickListener(listener: (WContentDialogButtonEvent) -> Unit) {
        val adapter = WContentDialogButtonListener(listener)
        addButtonClickListenerForJava(adapter)
        kotlinButtonListeners.add(listener, adapter)
    }

    /** Removes a Kotlin lambda added with [addButtonClickListener] (does nothing if not registered). */
    @JvmSynthetic
    fun removeButtonClickListener(listener: (WContentDialogButtonEvent) -> Unit) {
        removeButtonClickListenerForJava(kotlinButtonListeners.remove(listener) ?: return)
    }

    /** [addButtonClickListener] for Java. */
    @JvmName("addButtonClickListener")
    fun addButtonClickListenerForJava(listener: WContentDialogButtonListener) {
        val anchor = listenerAnchor
        val tokens = BUTTON_EVENTS.map { (button, addSlot) ->
            inspectable.addEventHandler(
                "WinUI4K.ContentDialogButtonClickHandler",
                XamlInterop.IID_ContentDialogButtonClickHandler,
                addSlot,
            ) { _, args ->
                (anchor.get() as? WContentDialog)?.let { dialog ->
                    val clickArgs = ComPtr(args) // Borrowed (not Released)
                    val event = WContentDialogButtonEvent(
                        dialog,
                        button,
                        clickArgs.getBool(XamlInterop.IContentDialogButtonClickEventArgs_get_Cancel),
                    )
                    listener.buttonClicked(event)
                    clickArgs.putBool(XamlInterop.IContentDialogButtonClickEventArgs_put_Cancel, event.isCancel)
                }
            }
        }
        buttonListeners += listener to tokens.toLongArray()
        anchor.listenerAdded()
    }

    /** [removeButtonClickListener] for Java. */
    @JvmName("removeButtonClickListener")
    fun removeButtonClickListenerForJava(listener: WContentDialogButtonListener) {
        val index = buttonListeners.indexOfLast { it.first === listener }
        if (index < 0) return
        val tokens = buttonListeners.removeAt(index).second
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_PrimaryButtonClick, tokens[0])
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_SecondaryButtonClick, tokens[1])
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_CloseButtonClick, tokens[2])
        listenerAnchor.listenerRemoved()
    }

    /**
     * The state while it is open (read and written only by the UI thread). Keeps the content of the window it opened in (XamlRoot.Content),
     * and when that is no longer Loaded, the window is considered closed (ContentDialog.Closed does not arrive then).
     */
    private class ShowState {
        private var rootContent: ComPtr? = null
        private var onClosed: Consumer<ContentDialogResult>? = null

        fun opened(rootContent: ComPtr, onClosed: Consumer<ContentDialogResult>?) {
            this.rootContent = rootContent
            this.onClosed = onClosed
        }

        fun isShowing(): Boolean {
            val content = rootContent ?: return false
            val element = content.queryInterface(XamlInterop.IID_IFrameworkElement)
            val loaded = try {
                element.getBool(XamlInterop.IFrameworkElement_get_IsLoaded)
            } finally {
                element.release()
            }
            if (!loaded) forget()
            return loaded
        }

        fun closed(result: ContentDialogResult) {
            val callback = onClosed
            forget()
            callback?.accept(result)
        }

        private fun forget() {
            rootContent?.release()
            rootContent = null
            onClosed = null
        }
    }

    private companion object {
        /** E_ASYNC_OPERATION_NOT_STARTED. Returned by ShowAsync when another dialog is open (ContentDialog_Partial.cpp). */
        const val E_ASYNC_OPERATION_NOT_STARTED = 0x80000019.toInt()

        /** A button and the add slot of its Click. */
        val BUTTON_EVENTS = listOf(
            ContentDialogButton.PRIMARY to XamlInterop.IContentDialog_add_PrimaryButtonClick,
            ContentDialogButton.SECONDARY to XamlInterop.IContentDialog_add_SecondaryButtonClick,
            ContentDialogButton.CLOSE to XamlInterop.IContentDialog_add_CloseButtonClick,
        )
    }
}
