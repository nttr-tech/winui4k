package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.Guid
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
 *
 * The content can be built either by [add]ing to [contentPane] as in JDialog (the default is [WBorderLayout], and [add] without a constraint goes to the center), or by putting
 * one component into [content].
 * Setting [defaultCloseOperation] to [DO_NOTHING_ON_CLOSE] keeps the Esc key and the close button from closing it.
 *
 * Differences from Swing (content and position):
 * - The dialog size is always decided by its content and its position is the center of the owner window ([pack] and [setLocationRelativeTo] do nothing).
 */
class WContentDialog @JvmOverloads constructor(title: String = "", content: WComponent? = null) :
    WControl(
        Activation.composeDefault(XamlInterop.CLS_ContentDialog, XamlInterop.IID_IContentDialogFactory), // default interface = IContentDialog
    ),
    WWindowConstants {
    /**
     * Like JDialog(owner, title, modal): creates it with the owner [owner] (it opens in this component's window, or in the [WFrame] itself) and
     * a title. ContentDialog is modal only, so [IllegalArgumentException] if [modal] is false.
     */
    @JvmOverloads
    constructor(owner: WDialogParent?, title: String, modal: Boolean = true) : this(title) {
        require(modal) { "ContentDialog is modal only; a modeless dialog (modal = false) cannot be created" }
        this.owner = owner
    }

    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IContentControl))
    }

    /** The state while it is open. Handlers left on the native side capture this, not the wrapper. */
    private val state = ShowState()

    /** The listeners added with [addDialogListener] and the tokens of Opened, Closing and Closed. */
    private val dialogListenerTokens = ArrayList<Pair<WContentDialogListener, LongArray>>()

    /** The listeners added with [addButtonClickListener] and the tokens of the primary, secondary and close buttons. */
    private val buttonListeners = ArrayList<Pair<WContentDialogButtonListener, LongArray>>()
    private val kotlinButtonListeners = KotlinListenerAdapters<WContentDialogButtonListener>()

    /**
     * The owner (JDialog's owner). [showAndWait] (without arguments) and [isVisible] = true open the dialog in this component's window (for a [WFrame],
     * in that window).
     */
    var owner: WDialogParent? = null

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

    /**
     * The content corresponding to JDialog's default contentPane (a [WLayoutPanel] with [WBorderLayout]). While [content] is null, this becomes the dialog's content
     * if it has children (no content is shown while it has none; it switches on [add] and when opening).
     */
    private val defaultContentPane = WLayoutPanel(WBorderLayout())

    /**
     * JDialog.getContentPane-like: the current content container. If [content] is null, the default content (as in JDialog, a [WLayoutPanel] with
     * [WBorderLayout]; [add] without a constraint goes to the center), and if a container is put into [content], that container. Setting [layout] does not
     * replace the content, so `getContentPane().add(...)` always goes into the shown content.
     *
     * @throws IllegalStateException if a component that is not a container is put into [content]
     */
    val contentPane: WContainer get() = currentContainer()

    /**
     * The dialog's content (ContentControl.Content; corresponds to JDialog.setContentPane). While null (default), the default content ([contentPane])
     * is the content (when it has children).
     */
    var content: WComponent? = null
        set(value) {
            field = value
            updateNativeContent()
        }

    /** The component currently shown as the native content (null if none). */
    private var shownContent: WComponent? = null

    /**
     * Makes the native content [content], or, if it is null, the default content if it has children (removes it if neither). A [WLayoutPanel] content
     * expands the dialog to the layout manager's preferred size, as JDialog's pack does ([WLayoutPanel.isDialogContent]).
     */
    private fun updateNativeContent() {
        val shown = content ?: defaultContentPane.takeIf { it.componentCount > 0 }
        if (shown !== shownContent) {
            (shownContent as? WLayoutPanel)?.isDialogContent = false
            (shown as? WLayoutPanel)?.isDialogContent = true
            shownContent = shown
        }
        contentControl.call(XamlInterop.IContentControl_put_Content, shown?.uiElement?.ptr)
    }

    /** Where children are added (the current content). The default content if [content] is null. [IllegalStateException] if it is not a container. */
    private fun currentContainer(): WContainer = (content ?: defaultContentPane) as? WContainer ?: error(
        "The content put in content (${content?.javaClass?.simpleName}) is not a container, so children cannot be added. Add them directly to the content's container",
    )

    /** Container.add(Component)-like: adds a child to the current content ([contentPane]). In the default content it goes to the center, as in JDialog. */
    fun add(component: WComponent) {
        currentContainer().add(component)
        updateNativeContent()
    }

    /**
     * Container.add(Component, Object)-like: adds a child with constraints to the current content (the form `dialog.add(buttons, WBorderLayout.SOUTH)`).
     * It is handled the same way as WFrame.add(component, constraints): passing a non-null constraint to content without a layout manager (a [WPanel] put into [content] or
     * a null layout) throws [IllegalArgumentException] (unlike Swing, it is not silently dropped).
     */
    fun add(component: WComponent, constraints: Any?) = add(component, constraints, -1)

    /** Container.add(Component, int)-like: adds a child at [index] (-1 for the end) of the current content. [IllegalArgumentException] if out of range. */
    fun add(component: WComponent, index: Int) {
        currentContainer().add(component, index)
        updateNativeContent()
    }

    /** Container.add(Component, Object, int)-like: adds a child with a constraint at [index] (-1 for the end) of the current content. */
    fun add(component: WComponent, constraints: Any?, index: Int) {
        ContentPanes.add(currentContainer(), component, constraints, index)
        updateNativeContent()
    }

    /** Container.remove(Component)-like: removes a child from the current content. Does nothing if it is not a child. */
    fun remove(component: WComponent) {
        currentContainer().remove(component)
        updateNativeContent()
    }

    /**
     * JDialog.getLayout / setLayout-like: the layout manager that lays out the children of the content ([contentPane]). The default is [WBorderLayout], as in JDialog.
     * null if the content is not a [WLayoutPanel]. Setting it replaces the manager of the content's [WLayoutPanel]
     * (as in JDialog, the content itself is not replaced; null is Swing's null layout).
     *
     * @throws IllegalStateException when a component other than a [WLayoutPanel] is in [content] (the content is not replaced)
     */
    var layout: WLayoutManager?
        get() = ((content ?: defaultContentPane) as? WLayoutPanel)?.layout
        set(value) {
            val current = (content ?: defaultContentPane) as? WLayoutPanel ?: error(
                "The content put in content (${content?.javaClass?.simpleName}) cannot have a layout manager. Use a WLayoutPanel as the content",
            )
            current.layout = value
        }

    /** Window.pack-like: does nothing. The size of a ContentDialog is decided by its content every time it opens (unlike Swing, the size cannot be specified). */
    fun pack() {
        // ContentDialog measures its content and decides its size itself
    }

    /**
     * Window.setLocationRelativeTo-like: does nothing. A ContentDialog always opens at the center of the owner window
     * (unlike Swing, the position cannot be specified).
     */
    @Suppress("UnusedParameter") // Takes the argument so that it can be called the same way as JDialog
    fun setLocationRelativeTo(component: WDialogParent?) {
        // WinUI places the ContentDialog at the center of the owner window
    }

    /**
     * JDialog.getDefaultCloseOperation / setDefaultCloseOperation-like: the behavior when the user tries to close it (the Esc key and the close button,
     * both ways of closing whose result is [ContentDialogResult.NONE]). The default is [HIDE_ON_CLOSE], the same as JDialog.
     * [HIDE_ON_CLOSE] and [DISPOSE_ON_CLOSE] close it (the dialog can be opened again with either). [DO_NOTHING_ON_CLOSE] does not close it, and
     * dialogClosing ([addDialogListener]) arrives with [WContentDialogEvent.isCancel] true. Calling [dispose] in it or setting
     * isCancel to false closes it (the form of disposing in JDialog's windowClosing). The primary and secondary buttons, [hide] /
     * [dispose], and closing of the owner window are not stopped. Other values (including [EXIT_ON_CLOSE]) throw [IllegalArgumentException] (the same as JDialog).
     */
    var defaultCloseOperation: Int = HIDE_ON_CLOSE
        set(value) {
            require(value in DO_NOTHING_ON_CLOSE..DISPOSE_ON_CLOSE) {
                "defaultCloseOperation must be one of DO_NOTHING_ON_CLOSE, HIDE_ON_CLOSE or DISPOSE_ON_CLOSE: $value"
            }
            field = value
            state.blocksUserClose = value == DO_NOTHING_ON_CLOSE
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
        // DO_NOTHING_ON_CLOSE of defaultCloseOperation. Added first so that it runs before the user's dialogClosing, which can then read the cancellation
        inspectable.addEventHandler(
            "WinUI4K.ContentDialogCloseOperationHandler",
            XamlInterop.IID_ContentDialogClosingHandler,
            XamlInterop.IContentDialog_add_Closing,
        ) { _, args ->
            val closingArgs = ComPtr(args) // Borrowed (not Released)
            val result = ContentDialogResult.of(closingArgs.getInt(XamlInterop.IContentDialogClosingEventArgs_get_Result))
            if (state.blockClosing(result)) closingArgs.putBool(XamlInterop.IContentDialogClosingEventArgs_put_Cancel, true)
        }
    }

    /**
     * Opens the dialog (ContentDialog.ShowAsync). Shown in the same window as [owner]
     * (inherits its XamlRoot). When closed, [onClosed] is called with the button that was pressed.
     * Does not block the caller (use [showAndWait] to wait until it closes). When the owner window is closed, [onClosed] is not called and it is released (what the callback refers to can be collected too; the dialog is also closed).
     * [IllegalStateException] if another ContentDialog is open in the same window or this dialog is already open,
     * [IllegalArgumentException] if [owner] is not on a window. If [owner] is in a window that was just shown (whose content is not loaded yet),
     * no exception is thrown, as in Swing, and the dialog opens after the content loads and is laid out ([showAndWait] waits until then before opening).
     */
    @JvmOverloads
    fun show(owner: WComponent, onClosed: Consumer<ContentDialogResult>? = null) {
        open(owner, onClosed)
    }

    /**
     * Like JDialog.setVisible(true): opens it in [owner]'s window, blocks the caller until it is closed, and returns the button pressed.
     * [ContentDialogResult.NONE] when the owner window is closed and when the app exits (WM_QUIT).
     * Call on the UI thread, outside layout notifications (otherwise [IllegalStateException], and the dialog does not open).
     * Other exceptions are the same as [show]. If the message loop exits with an exception while waiting, the dialog is closed before that exception is thrown.
     */
    fun showAndWait(owner: WDialogParent): ContentDialogResult {
        ModalLoop.checkRunnable()
        var result: ContentDialogResult? = null
        val component = WFrame.componentOf(owner)
        // For a window that was just shown, wait until the content loads (has a XamlRoot) before opening (Swing can open right after showing)
        if (awaitsXamlRoot(component)) ModalLoop.run { !awaitsXamlRoot(component) }
        open(component) { result = it }
        val state = state
        var waited = false
        try {
            ModalLoop.run { result != null || !state.isShowing() }
            waited = true
        } finally {
            // If an exception occurs while waiting (a GetMessageW failure, etc.), close it before rethrowing. If it stayed open, no dialog
            // could be opened in this window afterwards (WinUI allows one per window at a time). A failure to close does not hide the original exception
            if (!waited && result == null) runCatching { hide() }
        }
        return result ?: ContentDialogResult.NONE
    }

    /** Opens it in the window of the [owner] property and waits until it closes ([showAndWait]). [IllegalStateException] if [owner] is null. */
    fun showAndWait(): ContentDialogResult = showAndWait(
        checkNotNull(owner) { "there is no owner, so the window to open in is unknown; set owner or use showAndWait(owner)" },
    )

    /** Whether [owner] is in the content of a shown window and not loaded yet (has no XamlRoot). */
    private fun awaitsXamlRoot(owner: WComponent): Boolean = !hasXamlRoot(owner) && WFrame.isInShownFrame(owner)

    private fun hasXamlRoot(owner: WComponent): Boolean {
        val root = owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot) ?: return false
        root.release()
        return true
    }

    /** Whether it is waiting for the content to load (when [show] was called on a window that was just shown). */
    private var openPending = false

    private fun open(owner: WComponent, onClosed: Consumer<ContentDialogResult>?) {
        check(!state.isShowing() && !openPending) { "this dialog is already open" }
        if (awaitsXamlRoot(owner)) {
            // A window that was just shown: open after the content loads and is laid out (if it can no longer be opened, the exception comes from the call at that time)
            openPending = true
            owner.laidOutRequests.runOrDefer(this) {
                openPending = false
                open(owner, onClosed)
            }
            return
        }
        updateNativeContent() // Show children added directly to contentPane
        // WinUI 3's ContentDialog requires the XamlRoot to show in
        val root = requireNotNull(owner.uiElement.getPtrOrNull(XamlInterop.IUIElement_get_XamlRoot)) {
            "the owner is not placed in a window, so the dialog cannot be opened"
        }
        try {
            uiElement.call(XamlInterop.IUIElement_put_XamlRoot, root.ptr)
            // Completion (= the dialog closing) is received through the Closed event, so the async object is not used
            inspectable.getPtr(XamlInterop.IContentDialog_ShowAsync).release()
        } catch (e: WindowsRuntimeException) {
            root.release()
            if (e.hresult == E_ASYNC_OPERATION_NOT_STARTED) {
                throw IllegalStateException(
                    "another ContentDialog is already open in this window; WinUI can open only one at a time in a window",
                    e,
                )
            }
            throw e
        }
        state.opened(root, weakReferenceOf(inspectable), onClosed)
    }

    /**
     * Closes the dialog programmatically (ContentDialog.Hide). The result is [ContentDialogResult.NONE].
     * Called from a dialogClosing canceled because [defaultCloseOperation] is [DO_NOTHING_ON_CLOSE], it lets that closing through.
     */
    fun hide() {
        if (state.requestHideInBlockedClosing()) return
        state.hidingByProgram = true
        try {
            inspectable.call(XamlInterop.IContentDialog_Hide)
        } finally {
            state.hidingByProgram = false
        }
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
                // If dispose / hide is called during a closing canceled by DO_NOTHING_ON_CLOSE, let that closing through
                if (dialog.state.takeHideRequest()) event.isCancel = false
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
        dialogListenerTokens += listener to longArrayOf(opened, closing, closed)
        anchor.listenerAdded()
    }

    /** Like WindowListener's getWindowListeners: the listeners attached with [addDialogListener] (in the order added, a copy). */
    val dialogListeners: Array<WContentDialogListener>
        get() = dialogListenerTokens.map { it.first }.toTypedArray()

    /** Removes a listener added with [addDialogListener] (does nothing if not registered). */
    fun removeDialogListener(listener: WContentDialogListener) {
        val index = dialogListenerTokens.indexOfLast { it.first === listener }
        if (index < 0) return
        val tokens = dialogListenerTokens.removeAt(index).second
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
        addButtonClickListener(adapter)
        kotlinButtonListeners.add(listener, adapter)
    }

    /** Removes a Kotlin lambda added with [addButtonClickListener] (does nothing if not registered). */
    @JvmSynthetic
    fun removeButtonClickListener(listener: (WContentDialogButtonEvent) -> Unit) {
        removeButtonClickListener(kotlinButtonListeners.remove(listener) ?: return)
    }

    /** [addButtonClickListener] for Java. */
    fun addButtonClickListener(listener: WContentDialogButtonListener) {
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
    fun removeButtonClickListener(listener: WContentDialogButtonListener) {
        val index = buttonListeners.indexOfLast { it.first === listener }
        if (index < 0) return
        val tokens = buttonListeners.removeAt(index).second
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_PrimaryButtonClick, tokens[0])
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_SecondaryButtonClick, tokens[1])
        inspectable.removeEventHandler(XamlInterop.IContentDialog_remove_CloseButtonClick, tokens[2])
        listenerAnchor.listenerRemoved()
    }

    /**
     * The state while it is open (read and written only by the UI thread). It holds the XamlRoot of the window it opened in, and while open it is registered with
     * the window closed hook ([WFrame.addWindowClosedHook]). When that window closes, the dialog is considered closed and the state is released (then
     * ContentDialog.Closed does not arrive; releasing it detaches the [show] callback and the dialog beyond it from the native handlers).
     * Merely replacing the window content (Window.Content) keeps the dialog open, so it is not considered closed.
     * When the window closes, the native dialog is also Hidden (otherwise the never-ending asynchronous work of ShowAsync keeps holding the dialog,
     * and the native is not destroyed). The dialog itself is held through a weak reference ([dialog]) (this state is captured by native handlers, so holding it strongly would form a cycle).
     */
    private class ShowState {
        private var xamlRoot: ComPtr? = null

        /** true if [defaultCloseOperation] is [DO_NOTHING_ON_CLOSE]. */
        var blocksUserClose = false

        /** true while [hide] is calling ContentDialog.Hide (that Closing is not stopped). */
        var hidingByProgram = false

        /** true while dispatching a Closing canceled by [blockClosing] (reset to false on the next turn of the UI thread). */
        private var inBlockedClosing = false

        /** true if [hide] was called during [inBlockedClosing]. */
        private var hideRequested = false

        /** Returns true if the Closing with result [result] is to be canceled by [DO_NOTHING_ON_CLOSE], and remembers it until dispatching finishes. */
        fun blockClosing(result: ContentDialogResult): Boolean {
            if (!blocksUserClose || hidingByProgram || result != ContentDialogResult.NONE) return false
            inBlockedClosing = true
            hideRequested = false
            WinUiUtilities.invokeLater { finishBlockedClosing() }
            return true
        }

        /** If it is a [hide] inside a canceled Closing, remembers to let the closing through and returns true (the native Hide has no effect during it). */
        fun requestHideInBlockedClosing(): Boolean {
            if (!inBlockedClosing) return false
            hideRequested = true
            return true
        }

        /** If [hide] was called inside a canceled Closing, clears that request and returns true (lifts the cancellation of dialogClosing). */
        fun takeHideRequest(): Boolean {
            if (!inBlockedClosing || !hideRequested) return false
            hideRequested = false
            return true
        }

        /**
         * Dispatching a canceled Closing has finished. If [hide] was called outside dialogClosing (in another handler of the same Closing etc.)
         * and it is still open, close it now.
         */
        private fun finishBlockedClosing() {
            inBlockedClosing = false
            if (!hideRequested) return
            hideRequested = false
            val native = dialog?.getPtrOrNull(RESOLVE, Guid.of(XamlInterop.IID_IContentDialog)) ?: return
            hidingByProgram = true
            try {
                native.call(XamlInterop.IContentDialog_Hide)
            } finally {
                hidingByProgram = false
                native.release()
            }
        }

        /** A weak reference (IWeakReference) to the native open dialog. Used to close it when the window closes. */
        private var dialog: ComPtr? = null
        private var onClosed: Consumer<ContentDialogResult>? = null

        private val windowClosed = WindowClosedHook { root ->
            val own = xamlRoot
            if (own != null && Xaml.sameObject(own, root)) {
                val weak = dialog
                dialog = null
                forget()
                weak?.let { hideNative(it) }
            }
        }

        /** Opened. This state owns the [xamlRoot] reference and releases it when closed. */
        fun opened(xamlRoot: ComPtr, dialog: ComPtr, onClosed: Consumer<ContentDialogResult>?) {
            this.xamlRoot = xamlRoot
            this.dialog = dialog
            this.onClosed = onClosed
            WFrame.addWindowClosedHook(windowClosed)
        }

        fun isShowing(): Boolean = xamlRoot != null

        fun closed(result: ContentDialogResult) {
            val callback = onClosed
            forget()
            callback?.accept(result)
        }

        private fun forget() {
            if (xamlRoot != null) WFrame.removeWindowClosedHook(windowClosed)
            xamlRoot?.release()
            xamlRoot = null
            dialog?.release()
            dialog = null
            onClosed = null
        }
    }

    companion object {
        /** The same as [WWindowConstants.DO_NOTHING_ON_CLOSE] (so that `WContentDialog.DO_NOTHING_ON_CLOSE` can be written from Kotlin). */
        const val DO_NOTHING_ON_CLOSE: Int = WWindowConstants.DO_NOTHING_ON_CLOSE

        /** The same as [WWindowConstants.HIDE_ON_CLOSE]. */
        const val HIDE_ON_CLOSE: Int = WWindowConstants.HIDE_ON_CLOSE

        /** The same as [WWindowConstants.DISPOSE_ON_CLOSE]. */
        const val DISPOSE_ON_CLOSE: Int = WWindowConstants.DISPOSE_ON_CLOSE

        /** The same as [WWindowConstants.EXIT_ON_CLOSE] (cannot be used for [defaultCloseOperation], the same as JDialog). */
        const val EXIT_ON_CLOSE: Int = WWindowConstants.EXIT_ON_CLOSE

        /** IWeakReferenceSource (weakreference.h). GetWeakReference(out IWeakReference) is vtbl[3]. */
        private const val IID_IWEAK_REFERENCE_SOURCE = "00000038-0000-0000-c000-000000000046"
        private const val GET_WEAK_REFERENCE = 3

        /** IWeakReference.Resolve(REFIID, out IInspectable) is vtbl[3]. null if already destroyed. */
        private const val RESOLVE = 3

        private fun weakReferenceOf(target: ComPtr): ComPtr {
            val source = target.queryInterface(IID_IWEAK_REFERENCE_SOURCE)
            return try {
                source.getPtr(GET_WEAK_REFERENCE)
            } finally {
                source.release()
            }
        }

        /** Hides the dialog [weak] (released by this function) refers to, if it still exists. */
        private fun hideNative(weak: ComPtr) {
            try {
                val native = weak.getPtrOrNull(RESOLVE, Guid.of(XamlInterop.IID_IContentDialog)) ?: return
                try {
                    native.call(XamlInterop.IContentDialog_Hide)
                } finally {
                    native.release()
                }
            } finally {
                weak.release()
            }
        }

        /** E_ASYNC_OPERATION_NOT_STARTED. Returned by ShowAsync when another dialog is open (ContentDialog_Partial.cpp). */
        private const val E_ASYNC_OPERATION_NOT_STARTED = 0x80000019.toInt()

        /** A button and the add slot of its Click. */
        private val BUTTON_EVENTS = listOf(
            ContentDialogButton.PRIMARY to XamlInterop.IContentDialog_add_PrimaryButtonClick,
            ContentDialogButton.SECONDARY to XamlInterop.IContentDialog_add_SecondaryButtonClick,
            ContentDialogButton.CLOSE to XamlInterop.IContentDialog_add_CloseButtonClick,
        )
    }
}
