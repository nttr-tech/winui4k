package com.appkitbox.winui4k

/**
 * StyledDocument-like: the [WDocument] of a [WTextPane] with formatting ([WTextPane.document] / [WTextPane.styledDocument]).
 * Passing attributes to [insertString] formats the inserted text with them (the same as Swing). Passing null gives unformatted text in Swing, but
 * in RichEditBox the text inherits the formatting of the character before the insertion position.
 */
class WStyledDocument internal constructor(host: DocumentHost, private val pane: WTextPane) : WDocument(host) {
    /**
     * StyledDocument.setCharacterAttributes-like: sets the formatting of [length] characters from [offset] to [attributes].
     * If [replace] is true, the current formatting is reset to the default (RichEditBox's default formatting) before putting in [attributes]. If false,
     * only the attributes present in [attributes] are changed. Does nothing if [length] is 0 or less. The part beyond the text is ignored (the same as Swing).
     * Listeners receive changedUpdate. See [WStyleConstants] for the attribute keys and values.
     *
     * @throws IllegalArgumentException if [offset] is negative or an attribute value has the wrong type
     */
    fun setCharacterAttributes(offset: Int, length: Int, attributes: WAttributeSet, replace: Boolean) =
        pane.setCharacterAttributes(offset, length, attributes, replace)

    /**
     * The formatting of the character at [offset] (corresponding to Swing's `getCharacterElement(offset).getAttributes()`).
     * It contains the same six attributes as [WTextPane.getCharacterAttributes].
     *
     * @throws IllegalArgumentException for a position before 0 or after the number of characters
     */
    fun getCharacterAttributes(offset: Int): WAttributeSet = pane.getCharacterAttributes(offset)
}
