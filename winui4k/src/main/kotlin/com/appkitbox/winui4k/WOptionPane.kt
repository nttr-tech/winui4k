package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.Locale
import java.util.function.Consumer
import java.util.function.IntConsumer

/**
 * JOptionPane-style: a collection of static methods that open standard message, confirmation, input and option dialogs with [WContentDialog].
 * The constants have the same values as Swing, so code comparing with JOptionPane's constants can be written as is.
 *
 * - The forms that return a value (showConfirmDialog and so on) block the caller until closed, as in Swing
 *   ([WContentDialog.showAndWait]; a nested message loop on the UI thread, during which input, rendering and invokeLater keep working).
 *   They cannot be called from inside layout notifications (SizeChanged, Loaded and other listeners) ([IllegalStateException]).
 * - The forms that take a callback ([IntConsumer] and so on) as the last argument open without blocking and pass the result when closed.
 *
 * The buttons are ContentDialog's three (primary = Yes / OK, secondary = No, close = Cancel), and the default button (Enter) is
 * the primary. The Esc key and closing the window (including the app exiting) give [CLOSED_OPTION], as in Swing,
 * distinguished from the Cancel button ([CANCEL_OPTION]).
 * The button texts are Japanese if [Locale.getDefault] is Japanese and English otherwise, and can be changed with [yesButtonText] and the like
 * instead of Swing's UIManager "OptionPane.yesButtonText" and so on.
 *
 * Differences from Swing:
 * - WinUI can open only one ContentDialog at a time in a window, so opening another one while a dialog is open (from inside it)
 *   throws [IllegalStateException] (they do not nest). Close the open dialog before opening another.
 * - If the parent (parentComponent) is null, it opens on the active [WFrame] (if none, the shown [WFrame] that became active last
 *   or was created last). Unlike Swing, no invisible shared frame is created, so if no window is shown at all it throws
 *   [IllegalStateException]. A parent that is not in a window throws [IllegalArgumentException].
 * - [showOptionDialog] supports up to 3 options (a ContentDialog has 3 buttons).
 */
// The arguments correspond 1:1 to JOptionPane's static methods of the same names (so Swing code can be written as is)
@Suppress("LongParameterList")
object WOptionPane {
    /** Return value of [showConfirmDialog]: Yes. */
    const val YES_OPTION = 0

    /** Return value of [showConfirmDialog]: No. */
    const val NO_OPTION = 1

    /** Return value of [showConfirmDialog]: Cancel (the button). */
    const val CANCEL_OPTION = 2

    /** Return value of [showConfirmDialog]: OK. */
    const val OK_OPTION = 0

    /** Return value: closed without pressing a button (the Esc key, closing the owner window, the app exiting). */
    const val CLOSED_OPTION = -1

    /** optionType: OK only. */
    const val DEFAULT_OPTION = -1

    /** optionType: Yes, No. */
    const val YES_NO_OPTION = 0

    /** optionType: Yes, No, Cancel. */
    const val YES_NO_CANCEL_OPTION = 1

    /** optionType: OK, Cancel. */
    const val OK_CANCEL_OPTION = 2

    /** messageType: error (the icon is ErrorBadge). */
    const val ERROR_MESSAGE = 0

    /** messageType: information (the icon is Info). */
    const val INFORMATION_MESSAGE = 1

    /** messageType: warning (the icon is Warning). */
    const val WARNING_MESSAGE = 2

    /** messageType: question (the icon is Unknown). */
    const val QUESTION_MESSAGE = 3

    /** messageType: no icon. */
    const val PLAIN_MESSAGE = -1

    /** The same value as JOptionPane.UNINITIALIZED_VALUE ("uninitializedValue"). WinUI4K's API never returns it. */
    const val UNINITIALIZED_VALUE = "uninitializedValue"

    private val japanese: Boolean get() = Locale.getDefault().language == Locale.JAPANESE.language

    private var customYes: String? = null
    private var customNo: String? = null
    private var customOk: String? = null
    private var customCancel: String? = null

    /** The text of the Yes button. By default Japanese if [Locale.getDefault] is Japanese, otherwise "Yes". */
    @JvmStatic
    var yesButtonText: String
        get() = customYes ?: if (japanese) "\u306f\u3044" else "Yes"
        set(value) {
            customYes = value
        }

    /** The text of the No button. By default Japanese or "No". */
    @JvmStatic
    var noButtonText: String
        get() = customNo ?: if (japanese) "\u3044\u3044\u3048" else "No"
        set(value) {
            customNo = value
        }

    /** The text of the OK button. "OK" by default. */
    @JvmStatic
    var okButtonText: String
        get() = customOk ?: "OK"
        set(value) {
            customOk = value
        }

    /** The text of the Cancel button. By default Japanese or "Cancel". */
    @JvmStatic
    var cancelButtonText: String
        get() = customCancel ?: if (japanese) "\u30ad\u30e3\u30f3\u30bb\u30eb" else "Cancel"
        set(value) {
            customCancel = value
        }

    /** Resets all button texts changed with [yesButtonText] and the like to the defaults for [Locale.getDefault]. */
    @JvmStatic
    fun resetButtonTexts() {
        customYes = null
        customNo = null
        customOk = null
        customCancel = null
    }

    private val messageTitle: String get() = if (japanese) "\u30e1\u30c3\u30bb\u30fc\u30b8" else "Message"
    private val confirmTitle: String get() = if (japanese) "\u30aa\u30d7\u30b7\u30e7\u30f3\u306e\u9078\u629e" else "Select an Option"
    private val inputTitle: String get() = if (japanese) "\u5165\u529b" else "Input"

    // ---- showMessageDialog ----

    /** Shows an information message with the title "Message" (or its Japanese equivalent) and an OK button, and waits until it is closed. */
    @JvmStatic
    fun showMessageDialog(parent: WComponent?, message: Any?) {
        showMessageDialog(parent, message, messageTitle, INFORMATION_MESSAGE)
    }

    /** Shows a message with [title] and the icon of [messageType], and waits until it is closed. */
    @JvmStatic
    fun showMessageDialog(parent: WComponent?, message: Any?, title: String?, messageType: Int) {
        showMessageDialog(parent, message, title, messageType, null)
    }

    /** Shows a message with [title] and waits until it is closed. If [icon] is not null, it is used instead of the icon of [messageType]. */
    @JvmStatic
    fun showMessageDialog(parent: WComponent?, message: Any?, title: String?, messageType: Int, icon: WIcon?) {
        messageDialog(parent, message, title, messageType, icon).showAndWait()
    }

    /** [showMessageDialog] that opens without blocking. [onClosed] is called when it is closed. */
    @JvmStatic
    fun showMessageDialog(parent: WComponent?, message: Any?, title: String?, messageType: Int, icon: WIcon?, onClosed: Runnable) {
        messageDialog(parent, message, title, messageType, icon).show { onClosed.run() }
    }

    private fun messageDialog(parent: WComponent?, message: Any?, title: String?, messageType: Int, icon: WIcon?) =
        OptionDialog(WFrame.dialogOwnerOf(parent), message, title, iconOf(messageType, icon), listOf(okButtonText), 0, null)

    // ---- showConfirmDialog ----

    /** Asks with Yes, No and Cancel and returns the button pressed ([YES_OPTION], [NO_OPTION], [CANCEL_OPTION], [CLOSED_OPTION]). */
    @JvmStatic
    fun showConfirmDialog(parent: WComponent?, message: Any?): Int =
        showConfirmDialog(parent, message, confirmTitle, YES_NO_CANCEL_OPTION)

    /** Asks with the buttons of [optionType] (the icon is a question) and returns the button pressed. */
    @JvmStatic
    fun showConfirmDialog(parent: WComponent?, message: Any?, title: String?, optionType: Int): Int =
        showConfirmDialog(parent, message, title, optionType, QUESTION_MESSAGE)

    /** Asks with the buttons of [optionType] and the icon of [messageType] and returns the button pressed. */
    @JvmStatic
    fun showConfirmDialog(parent: WComponent?, message: Any?, title: String?, optionType: Int, messageType: Int): Int =
        showConfirmDialog(parent, message, title, optionType, messageType, null)

    /**
     * Asks with the buttons of [optionType] and returns the button pressed. Yes / OK is [YES_OPTION] (= [OK_OPTION]), No is [NO_OPTION],
     * Cancel is [CANCEL_OPTION], and the Esc key and closing the window give [CLOSED_OPTION].
     */
    @JvmStatic
    fun showConfirmDialog(parent: WComponent?, message: Any?, title: String?, optionType: Int, messageType: Int, icon: WIcon?): Int =
        confirmDialog(parent, message, title, optionType, messageType, icon).showAndWait()

    /** [showConfirmDialog] that opens without blocking. When it is closed, the button pressed (the same value as the return value) is passed to [onResult]. */
    @JvmStatic
    fun showConfirmDialog(
        parent: WComponent?,
        message: Any?,
        title: String?,
        optionType: Int,
        messageType: Int,
        icon: WIcon?,
        onResult: IntConsumer,
    ) {
        confirmDialog(parent, message, title, optionType, messageType, icon).show { onResult.accept(it) }
    }

    private fun confirmDialog(parent: WComponent?, message: Any?, title: String?, optionType: Int, messageType: Int, icon: WIcon?): OptionDialog {
        val buttons = buttonsOf(optionType)
        return OptionDialog(WFrame.dialogOwnerOf(parent), message, title, iconOf(messageType, icon), buttons, 0, null)
    }

    // ---- showInputDialog ----

    /** Has the user enter text (the title is "Input" or its Japanese equivalent; the icon is a question), and returns the entered text on OK, or null on Cancel or Esc. */
    @JvmStatic
    fun showInputDialog(parent: WComponent?, message: Any?): String? = showInputDialog(parent, message, null as Any?)

    /** Same as JOptionPane.showInputDialog(Object) without a parent: opens on the active window and asks for text input (same as a null parent). */
    @JvmStatic
    fun showInputDialog(message: Any?): String? = showInputDialog(null, message, null as Any?)

    /** Same as JOptionPane.showInputDialog(Object, Object) without a parent: sets the initial text of the input field to [initialSelectionValue]. */
    @JvmStatic
    fun showInputDialog(message: Any?, initialSelectionValue: Any?): String? = showInputDialog(null, message, initialSelectionValue)

    /** Has the user enter text with the input field initially set to (the toString of) [initialSelectionValue]. */
    @JvmStatic
    fun showInputDialog(parent: WComponent?, message: Any?, initialSelectionValue: Any?): String? =
        showInputDialog(parent, message, inputTitle, QUESTION_MESSAGE, null, null, initialSelectionValue) as String?

    /** Has the user enter text with [title] and the icon of [messageType]. */
    @JvmStatic
    fun showInputDialog(parent: WComponent?, message: Any?, title: String?, messageType: Int): String? =
        showInputDialog(parent, message, title, messageType, null, null, null) as String?

    /**
     * Has the user enter a value and returns it on OK, or null on Cancel, Esc or closing the window. If [selectionValues] is null, an input field is shown and
     * the entered text is returned. Otherwise a combo box to choose among them (showing each value's toString) is shown, and the chosen element itself is returned.
     * [initialSelectionValue] is the initial text (input field) or the element selected initially (combo box; the first one if absent).
     */
    @JvmStatic
    fun showInputDialog(
        parent: WComponent?,
        message: Any?,
        title: String?,
        messageType: Int,
        icon: WIcon?,
        selectionValues: Array<out Any?>?,
        initialSelectionValue: Any?,
    ): Any? = inputDialog(parent, message, title, messageType, icon, selectionValues, initialSelectionValue).let { (dialog, input) ->
        if (dialog.showAndWait() == OK_OPTION) input.value() else null
    }

    /** The input-field [showInputDialog] that opens without blocking. When it is closed, the entered text (null on Cancel or Esc) is passed to [onResult]. */
    @JvmStatic
    fun showInputDialog(parent: WComponent?, message: Any?, title: String?, messageType: Int, onResult: Consumer<String?>) {
        val (dialog, input) = inputDialog(parent, message, title, messageType, null, null, null)
        dialog.show { onResult.accept(if (it == OK_OPTION) input.value() as String? else null) }
    }

    /** [showInputDialog] with options that opens without blocking. When it is closed, the value (null on Cancel or Esc) is passed to [onResult]. */
    @JvmStatic
    fun showInputDialog(
        parent: WComponent?,
        message: Any?,
        title: String?,
        messageType: Int,
        icon: WIcon?,
        selectionValues: Array<out Any?>?,
        initialSelectionValue: Any?,
        onResult: Consumer<Any?>,
    ) {
        val (dialog, input) = inputDialog(parent, message, title, messageType, icon, selectionValues, initialSelectionValue)
        dialog.show { onResult.accept(if (it == OK_OPTION) input.value() else null) }
    }

    private fun inputDialog(
        parent: WComponent?,
        message: Any?,
        title: String?,
        messageType: Int,
        icon: WIcon?,
        selectionValues: Array<out Any?>?,
        initialSelectionValue: Any?,
    ): Pair<OptionDialog, InputPart> {
        val owner = WFrame.dialogOwnerOf(parent)
        val input = if (selectionValues != null) {
            InputPart.Selection(selectionValues, initialSelectionValue)
        } else {
            InputPart.Text(initialSelectionValue?.toString() ?: "")
        }
        val dialog = OptionDialog(owner, message, title, iconOf(messageType, icon), listOf(okButtonText, null, cancelButtonText), 0, input.component)
        return dialog to input
    }

    // ---- showOptionDialog ----

    /**
     * Asks with buttons made from [options] (their toString) and returns the position in [options] of the button pressed ([CLOSED_OPTION] for the Esc key
     * and closing the window). If [options] is null or empty, the buttons of [optionType] are used and the return value is the same as [showConfirmDialog].
     * The option equal to [initialValue] becomes the default button (Enter) (the first one if absent). [IllegalArgumentException] for 4 or more options.
     */
    @JvmStatic
    fun showOptionDialog(
        parent: WComponent?,
        message: Any?,
        title: String?,
        optionType: Int,
        messageType: Int,
        icon: WIcon?,
        options: Array<out Any?>?,
        initialValue: Any?,
    ): Int = optionDialog(parent, message, title, optionType, messageType, icon, options, initialValue).showAndWait()

    /** [showOptionDialog] that opens without blocking. When it is closed, the position of the button pressed (the same value as the return value) is passed to [onResult]. */
    @JvmStatic
    fun showOptionDialog(
        parent: WComponent?,
        message: Any?,
        title: String?,
        optionType: Int,
        messageType: Int,
        icon: WIcon?,
        options: Array<out Any?>?,
        initialValue: Any?,
        onResult: IntConsumer,
    ) {
        optionDialog(parent, message, title, optionType, messageType, icon, options, initialValue).show { onResult.accept(it) }
    }

    private fun optionDialog(
        parent: WComponent?,
        message: Any?,
        title: String?,
        optionType: Int,
        messageType: Int,
        icon: WIcon?,
        options: Array<out Any?>?,
        initialValue: Any?,
    ): OptionDialog {
        if (options.isNullOrEmpty()) return confirmDialog(parent, message, title, optionType, messageType, icon)
        require(options.size <= MAX_BUTTONS) {
            "at most 3 options are supported (a ContentDialog has 3 buttons); ${options.size} cannot be shown"
        }
        val labels = options.map { it.toString() }
        val defaultIndex = options.indexOfFirst { it == initialValue }.coerceAtLeast(0)
        return OptionDialog(WFrame.dialogOwnerOf(parent), message, title, iconOf(messageType, icon), labels, defaultIndex, null)
    }

    // ---- Common ----

    private const val MAX_BUTTONS = 3

    private fun buttonsOf(optionType: Int): List<String?> = when (optionType) {
        DEFAULT_OPTION -> listOf(okButtonText)
        YES_NO_OPTION -> listOf(yesButtonText, noButtonText)
        YES_NO_CANCEL_OPTION -> listOf(yesButtonText, noButtonText, cancelButtonText)
        OK_CANCEL_OPTION -> listOf(okButtonText, null, cancelButtonText)
        else -> throw IllegalArgumentException(
            "optionType must be one of DEFAULT_OPTION, YES_NO_OPTION, YES_NO_CANCEL_OPTION, OK_CANCEL_OPTION: $optionType",
        )
    }

    /** The icon of the messageType (a Segoe Fluent Icons glyph). [icon] if given. */
    private fun iconOf(messageType: Int, icon: WIcon?): WIcon? {
        val glyph = when (messageType) {
            ERROR_MESSAGE -> GLYPH_ERROR
            INFORMATION_MESSAGE -> GLYPH_INFORMATION
            WARNING_MESSAGE -> GLYPH_WARNING
            QUESTION_MESSAGE -> GLYPH_QUESTION
            PLAIN_MESSAGE -> null
            else -> throw IllegalArgumentException(
                "messageType must be one of ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, PLAIN_MESSAGE: $messageType",
            )
        }
        return icon ?: glyph?.let { WFontIcon(it) }
    }

    /** ErrorBadge. */
    internal const val GLYPH_ERROR = "\uEA39"

    /** Info. */
    internal const val GLYPH_INFORMATION = "\uE946"

    /** Warning. */
    internal const val GLYPH_WARNING = "\uE7BA"

    /** Unknown (a question mark in a circle). */
    internal const val GLYPH_QUESTION = "\uE9CE"
}

/** The input component of [WOptionPane.showInputDialog] (an input field or a combo box) and the value on OK. */
private sealed class InputPart {
    abstract val component: WComponent

    abstract fun value(): Any?

    class Text(initial: String) : InputPart() {
        private val textField = WTextField().also { it.text = initial }
        override val component: WComponent get() = textField

        override fun value(): Any? = textField.text
    }

    class Selection(private val values: Array<out Any?>, initial: Any?) : InputPart() {
        private val box = WComboBox(values.map { it.toString() }).also { box ->
            if (values.isNotEmpty()) box.selectedIndex = values.indexOfFirst { it == initial }.coerceAtLeast(0)
        }
        override val component: WComponent get() = box

        override fun value(): Any? = box.selectedIndex.takeIf { it in values.indices }?.let { values[it] }
    }
}

/**
 * One dialog of [WOptionPane]. Makes the 0th, 1st and 2nd of [buttons] the primary, secondary and close buttons
 * (null hides that button), and returns the number of the button pressed ([WOptionPane.CLOSED_OPTION] for the Esc key and for closing without pressing).
 *
 * The Esc key is received in the dialog's own PreviewKeyDown (tunneling) and marked handled before ContentDialog presses the close button in the KeyDown
 * of the template's LayoutRoot (ProcessLayoutRootKey in ContentDialog_Partial.cpp), and the dialog closes without pressing a button.
 * The whole dialog sits in a Popup outside the window (EntireControlInPopup), so the dialog is an ancestor of the elements inside.
 */
@Suppress("LongParameterList") // Takes the arguments of each WOptionPane method together
internal class OptionDialog(
    private val owner: WComponent,
    message: Any?,
    title: String?,
    icon: WIcon?,
    buttons: List<String?>,
    defaultIndex: Int,
    input: WComponent?,
) {
    /** The dialog to open (internal so tests can check its inside). */
    val dialog = WContentDialog(title ?: "")

    /** The container for the content components. Emptied when closed so that components passed in the message can be used in the next dialog. */
    private val body = WPanel(spacing = BODY_SPACING)
    private val state = State()

    init {
        val row = WPanel(spacing = ICON_GAP, orientation = Orientation.HORIZONTAL)
        if (icon != null) row.add(WLabel(icon))
        addMessage(body, message)
        if (input != null) body.add(input)
        row.add(body)
        dialog.content = row
        buttons.getOrNull(0)?.let { dialog.primaryButtonText = it }
        buttons.getOrNull(1)?.let { dialog.secondaryButtonText = it }
        buttons.getOrNull(2)?.let { dialog.closeButtonText = it }
        dialog.defaultButton = BUTTONS[defaultIndex]
        val state = state
        dialog.addButtonClickListener { e -> if (e.button == ContentDialogButton.CLOSE) state.closeClicked = true }
        dialog.uiElement.addEventHandler(
            "WinUI4K.OptionPaneEscapeHandler",
            XamlInterop.IID_KeyEventHandler,
            XamlInterop.IUIElement_add_PreviewKeyDown,
        ) { sender, args ->
            val key = XamlKeyEvent(ComPtr(args)) // Borrowed (not Released)
            if (key.key == VK_ESCAPE && !key.handled) {
                key.handled = true
                val contentDialog = ComPtr(sender).queryInterface(XamlInterop.IID_IContentDialog)
                try {
                    contentDialog.call(XamlInterop.IContentDialog_Hide)
                } finally {
                    contentDialog.release()
                }
            }
        }
    }

    /** Waits until it is closed and returns the number of the button pressed. */
    fun showAndWait(): Int = finish(dialog.showAndWait(owner))

    /** Opens without blocking and passes the number of the button pressed to [onResult] when it is closed. */
    fun show(onResult: (Int) -> Unit) {
        dialog.show(owner) { onResult(finish(it)) }
    }

    private fun finish(result: ContentDialogResult): Int {
        body.removeAll()
        return when (result) {
            ContentDialogResult.PRIMARY -> 0
            ContentDialogResult.SECONDARY -> 1
            ContentDialogResult.NONE -> if (state.closeClicked) 2 else WOptionPane.CLOSED_OPTION
        }
    }

    private fun addMessage(panel: WPanel, message: Any?) {
        when (message) {
            null -> Unit
            is WComponent -> panel.add(message)
            is Array<*> -> message.forEach { addMessage(panel, it) }
            else -> panel.add(WLabel(message.toString()).also { it.textWrapping = TextWrapping.WRAP })
        }
    }

    private class State {
        var closeClicked = false
    }

    companion object {
        private val BUTTONS = listOf(ContentDialogButton.PRIMARY, ContentDialogButton.SECONDARY, ContentDialogButton.CLOSE)
        private const val VK_ESCAPE = 0x1B
        private const val ICON_GAP = 16.0
        private const val BODY_SPACING = 12.0
    }
}
