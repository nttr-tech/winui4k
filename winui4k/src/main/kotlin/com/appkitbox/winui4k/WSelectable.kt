package com.appkitbox.winui4k

/**
 * A component with a selection state that can be added to a ButtonGroup (corresponds to Swing's AbstractButton with a selection state and ItemSelectable).
 * Implemented by [WToggleButton] (and its subclasses [WCheckBox] / [WRadioButton] etc.), [WToggleMenuFlyoutItem] and [WRadioMenuFlyoutItem].
 * [WButtonGroup.selection] / [WButtonGroup.elements] return this type, so as with Swing's `group.getSelection().getActionCommand()`,
 * the selected one can be identified by [actionCommand] or [text].
 *
 * The selection notification mechanism is internal to the library, so no implementations other than these three (and subclasses of WToggleButton) can be made (sealed in Kotlin).
 * The Java 8 output does not enforce sealed, so it can be implemented in Java, but passing it to [WButtonGroup.add] throws [IllegalArgumentException].
 */
sealed interface WSelectable {
    /** AbstractButton.isSelected / setSelected-like: whether it is selected. */
    var isSelected: Boolean

    /** AbstractButton.getText / setText-like: the displayed text. */
    var text: String

    /** AbstractButton.getActionCommand-like: the string put into [WActionEvent.actionCommand] on click ([text] if unset). */
    var actionCommand: String?

    /** ItemSelectable.addItemListener-like: receives a [WItemEvent] every time the selection state changes. */
    fun addItemListener(listener: WItemListener)

    /** ItemSelectable.removeItemListener-like: removes a listener added with [addItemListener]. */
    fun removeItemListener(listener: WItemListener)
}
