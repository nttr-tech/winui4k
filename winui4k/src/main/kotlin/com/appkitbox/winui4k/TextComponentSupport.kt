package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import java.io.IOException
import java.io.Reader
import java.io.Writer

/**
 * Reading and writing of the JTextComponent.getSelectionColor / setSelectionColor-like selection background color (SelectionHighlightColor;
 * a SolidColorBrush) of input fields ([WTextField] / [WPasswordField] / [WTextPane]).
 *
 * - [view]: the interface that has SelectionHighlightColor (borrowed; not released)
 * - [getSlot] / [putSlot]: the vtable slots of its get / put
 * - [property]: the dependency property passed to ClearValue when resetting to the default with null (class name, Statics IID, slot of get_XxxProperty)
 */
internal class SelectionHighlight(
    private val owner: WComponent,
    private val view: ComPtr,
    private val getSlot: Int,
    private val putSlot: Int,
    private val property: Triple<String, String, Int>,
) {
    /** The current selection background color (the native effective value; null if there is no brush or it is not a solid color). */
    fun get(): WColor? {
        val brush = view.getPtrOrNull(getSlot) ?: return null
        return try {
            colorOf(brush)
        } finally {
            brush.release()
        }
    }

    /** Sets the selection background color to [value]. If null, clears the setting and returns to the theme color (the default style value). */
    fun set(value: WColor?) {
        if (value == null) {
            clearDependencyProperty(owner.dependencyObject, property.first, property.second, property.third)
            return
        }
        val brush = value.createBrush()
        try {
            view.call(putSlot, brush.ptr)
        } finally {
            brush.release()
        }
    }
}

/** JTextComponent.getFocusAccelerator-like: returns the access key (UIElement.AccessKey) of [component] as a character. '\u0000' if unset. */
internal fun readFocusAccelerator(component: WComponent): Char = Mnemonics.read(component.uiElement).toChar()

/**
 * JTextComponent.setFocusAccelerator-like: sets the access key of [component] to [key]. Letters are converted to upper case, and '\u0000' clears it.
 * Anything other than ASCII letters and digits throws [IllegalArgumentException] (only characters usable as access keys; Swing accepts any character).
 */
internal fun writeFocusAccelerator(component: WComponent, key: Char) {
    Mnemonics.write(component.uiElement, Mnemonics.keyCodeOf(key))
}

/** JTextComponent.getText(offs, len)-like: [length] characters of [content] from [offset]. [IllegalArgumentException] if the range is outside the string. */
internal fun textRange(content: String, offset: Int, length: Int): String {
    require(offset >= 0 && length >= 0 && length <= content.length - offset) {
        "Range outside the string: offset=$offset, length=$length (length ${content.length})"
    }
    return content.substring(offset, offset + length)
}

/** JTextComponent.read-like: reads [reader] to the end and returns it with line breaks normalized to LF, as Swing (DefaultEditorKit.read) does (CRLF and CR become LF). */
@Throws(IOException::class)
internal fun readText(reader: Reader): String = reader.readText().replace("\r\n", "\n").replace('\r', '\n')

/** JTextComponent.write-like: writes [text] to [writer] and flushes it (without closing; the same as Swing's DefaultEditorKit.write). */
@Throws(IOException::class)
internal fun writeText(writer: Writer, text: String) {
    writer.write(text)
    writer.flush()
}
