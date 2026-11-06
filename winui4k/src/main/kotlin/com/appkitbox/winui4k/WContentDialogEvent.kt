package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * WindowEvent-style: an event that tells a [WContentDialogListener] that a [WContentDialog] opened, is about to close, or closed.
 * The source is the dialog ([dialog]).
 *
 * [result] is the button that is the reason for closing (for dialogClosing / dialogClosed; [ContentDialogResult.NONE] for dialogOpened).
 * [isCancel] has meaning only in dialogClosing, and setting it to true cancels the closing (ContentDialogClosingEventArgs.Cancel).
 * With several listeners, the next listener receives the value set by the previous one.
 */
class WContentDialogEvent internal constructor(
    source: WContentDialog,
    /** The button that is the reason for closing. [ContentDialogResult.NONE] when opened. */
    val result: ContentDialogResult,
    /** Setting it to true in dialogClosing keeps the dialog open instead of closing. Has no effect in other methods. */
    var isCancel: Boolean = false,
) : EventObject(source) {
    /** The dialog the event came from (the same as [getSource]). */
    val dialog: WContentDialog get() = source as WContentDialog
}

/**
 * WindowListener-style: a listener that receives the opening and closing of a [WContentDialog] (ContentDialog's Opened, Closing, Closed).
 * [WContentDialogAdapter] also exists so that you do not have to write empty methods you do not use.
 * The order is dialogOpened when it opens, and dialogClosing -> (unless canceled) dialogClosed when it closes.
 * When closed by a button, [WContentDialogButtonListener] is called before that.
 */
interface WContentDialogListener : EventListener {
    /** Called after the dialog opens (ContentDialog.Opened). */
    fun dialogOpened(e: WContentDialogEvent)

    /** Called before the dialog closes (ContentDialog.Closing). Setting [WContentDialogEvent.isCancel] to true keeps it from closing. */
    fun dialogClosing(e: WContentDialogEvent)

    /** Called after the dialog closes (ContentDialog.Closed). */
    fun dialogClosed(e: WContentDialogEvent)
}

/** WindowAdapter-style: an abstract class implementing all methods of [WContentDialogListener] as empty. Override only the methods you use. */
abstract class WContentDialogAdapter : WContentDialogListener {
    override fun dialogOpened(e: WContentDialogEvent) = Unit

    override fun dialogClosing(e: WContentDialogEvent) = Unit

    override fun dialogClosed(e: WContentDialogEvent) = Unit
}

/**
 * An event that tells that one of the three buttons of a [WContentDialog] (primary, secondary, close) was pressed. The source is the dialog.
 * In Swing this is where input is validated in the ActionListener of buttons you put on the JDialog yourself; setting [isCancel] to true
 * keeps the dialog open instead of closing (ContentDialogButtonClickEventArgs.Cancel).
 * The Esc key counts as pressing the close button (when [closeButtonText][WContentDialog.closeButtonText] is not empty).
 */
class WContentDialogButtonEvent internal constructor(
    source: WContentDialog,
    /** The button pressed ([ContentDialogButton.PRIMARY], [ContentDialogButton.SECONDARY], [ContentDialogButton.CLOSE]). */
    val button: ContentDialogButton,
    /** Setting it to true keeps the dialog from closing even though the button was pressed. */
    var isCancel: Boolean = false,
) : EventObject(source) {
    /** The dialog the event came from (the same as [getSource]). */
    val dialog: WContentDialog get() = source as WContentDialog
}

/** The listener passed to [WContentDialog.addButtonClickListener] (can be written as a lambda in Java). */
fun interface WContentDialogButtonListener : EventListener {
    /** Called when a dialog button is pressed, before closing. */
    fun buttonClicked(e: WContentDialogButtonEvent)
}
