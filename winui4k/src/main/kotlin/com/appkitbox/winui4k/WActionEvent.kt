package com.appkitbox.winui4k

import java.util.EventObject

/**
 * ActionEvent-like: an event that delivers a button click to a [WActionListener].
 * [getSource] (`source` in Kotlin) is the clicked button, [actionCommand] is the [WButtonBase.actionCommand] at the time
 * of the click, [modifiers] are the modifier keys that were held down ([SHIFT_MASK] and so on), and [when] is the time of
 * the click (System.currentTimeMillis).
 *
 * This class does not depend on java.awt, so it defines its own constants with the same values as AWT's ActionEvent.
 */
class WActionEvent @JvmOverloads constructor(
    source: Any,
    /** The action command at the time of the click. null if the button returns null. */
    val actionCommand: String?,
    /** The combination of modifier keys that were held down ([SHIFT_MASK] / [CTRL_MASK] / [META_MASK] / [ALT_MASK]). */
    val modifiers: Int = 0,
    /** The time of the click (milliseconds since 1970-01-01T00:00:00Z). */
    val `when`: Long = System.currentTimeMillis(),
) : EventObject(source) {
    companion object {
        /** The Shift key (same value as AWT's ActionEvent.SHIFT_MASK). */
        const val SHIFT_MASK: Int = 1

        /** The Ctrl key (same value as AWT's ActionEvent.CTRL_MASK). */
        const val CTRL_MASK: Int = 2

        /** The Meta key. On Windows, it corresponds to the Windows key (same value as AWT's ActionEvent.META_MASK). */
        const val META_MASK: Int = 4

        /** The Alt key (same value as AWT's ActionEvent.ALT_MASK). */
        const val ALT_MASK: Int = 8

        /**
         * Converts the VirtualKeyModifiers format ([Xaml.currentModifiers]: Ctrl=1, Alt=2, Shift=4, Win=8) into
         * a combination of this class's modifier key constants.
         */
        @JvmSynthetic
        internal fun fromVirtualKeyModifiers(virtualKeyModifiers: Int): Int {
            var result = 0
            if (virtualKeyModifiers and Xaml.MODIFIER_CONTROL != 0) result = result or CTRL_MASK
            if (virtualKeyModifiers and Xaml.MODIFIER_MENU != 0) result = result or ALT_MASK
            if (virtualKeyModifiers and Xaml.MODIFIER_SHIFT != 0) result = result or SHIFT_MASK
            if (virtualKeyModifiers and Xaml.MODIFIER_WINDOWS != 0) result = result or META_MASK
            return result
        }
    }
}

/** ActionListener-like: a listener passed to [WButtonBase.addActionListener] (can be written as a lambda in Java). */
fun interface WActionListener {
    /** Called when the button is clicked. */
    fun actionPerformed(e: WActionEvent)
}
