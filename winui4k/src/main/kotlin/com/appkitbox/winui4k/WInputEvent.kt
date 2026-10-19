package com.appkitbox.winui4k

import java.util.EventObject

/**
 * InputEvent-style: the common base of mouse and key events ([WMouseEvent] / [WKeyEvent]).
 * Holds the modifier keys and buttons that were held down ([modifiersEx], [isShiftDown] etc.), the time ([when]) and
 * the handled mark ([consume]).
 *
 * This class does not depend on java.awt, so it defines its own constants with the same values as AWT's InputEvent.
 */
abstract class WInputEvent internal constructor(
    source: WComponent,
    /** Kind of event (WMouseEvent.MOUSE_PRESSED etc.). `getID()` in Java, as in AWT. */
    @get:JvmName("getID") val id: Int,
    /** Time of the event (milliseconds since 1970-01-01T00:00:00Z; equivalent to InputEvent.getWhen). */
    val `when`: Long,
    /**
     * Modifier keys and mouse buttons that were held down (equivalent to InputEvent.getModifiersEx). A combination of
     * [SHIFT_DOWN_MASK] / [CTRL_DOWN_MASK] / [META_DOWN_MASK] / [ALT_DOWN_MASK] / [BUTTON1_DOWN_MASK] /
     * [BUTTON2_DOWN_MASK] / [BUTTON3_DOWN_MASK].
     */
    val modifiersEx: Int,
) : EventObject(source) {
    /** The component the event originated from (equivalent to ComponentEvent.getComponent). Same as [getSource]. */
    val component: WComponent get() = source as WComponent

    /** Whether the Shift key was held down. */
    val isShiftDown: Boolean get() = modifiersEx and SHIFT_DOWN_MASK != 0

    /** Whether the Ctrl key was held down. */
    val isControlDown: Boolean get() = modifiersEx and CTRL_DOWN_MASK != 0

    /** Whether the Meta key was held down. On Windows, this corresponds to the Windows key. */
    val isMetaDown: Boolean get() = modifiersEx and META_DOWN_MASK != 0

    /** Whether the Alt key was held down. */
    val isAltDown: Boolean get() = modifiersEx and ALT_DOWN_MASK != 0

    /** Whether the AltGraph key was held down. Always false, since WinUI does not distinguish AltGraph. */
    val isAltGraphDown: Boolean get() = modifiersEx and ALT_GRAPH_DOWN_MASK != 0

    /**
     * Returns the modifier keys and buttons in AWT's old format ([SHIFT_MASK] / [CTRL_MASK] / [META_MASK] / [ALT_MASK] /
     * [BUTTON1_MASK] etc.) (equivalent to InputEvent.getModifiers). New code should use [modifiersEx].
     */
    open val modifiers: Int get() = keyMasksOf(modifiersEx)

    /** Whether this was marked handled (whether [consume] was called). */
    var isConsumed: Boolean = false
        private set

    /** Sets Handled on the native event. Set only while the listeners are being called. */
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var onConsume: (() -> Unit)? = null

    /**
     * Marks this event handled (equivalent to InputEvent.consume). The native event also becomes handled (Handled), so
     * it no longer reaches the control's own default handling or the parent elements (see the KDoc of each event kind
     * for what is stopped).
     */
    fun consume() {
        isConsumed = true
        onConsume?.invoke()
    }

    companion object {
        /** Shift key (same value as AWT's InputEvent.SHIFT_MASK; old format). */
        const val SHIFT_MASK: Int = 1

        /** Ctrl key (same value as AWT's InputEvent.CTRL_MASK; old format). */
        const val CTRL_MASK: Int = 2

        /** Meta key (same value as AWT's InputEvent.META_MASK; old format; same value as [BUTTON3_MASK]). */
        const val META_MASK: Int = 4

        /** Alt key (same value as AWT's InputEvent.ALT_MASK; old format; same value as [BUTTON2_MASK]). */
        const val ALT_MASK: Int = 8

        /** AltGraph key (same value as AWT's InputEvent.ALT_GRAPH_MASK; old format). */
        const val ALT_GRAPH_MASK: Int = 32

        /** Left mouse button (same value as AWT's InputEvent.BUTTON1_MASK; old format). */
        const val BUTTON1_MASK: Int = 16

        /** Middle mouse button (same value as AWT's InputEvent.BUTTON2_MASK; old format). */
        const val BUTTON2_MASK: Int = 8

        /** Right mouse button (same value as AWT's InputEvent.BUTTON3_MASK; old format). */
        const val BUTTON3_MASK: Int = 4

        /** Shift key (same value as AWT's InputEvent.SHIFT_DOWN_MASK). */
        const val SHIFT_DOWN_MASK: Int = 1 shl 6

        /** Ctrl key (same value as AWT's InputEvent.CTRL_DOWN_MASK). */
        const val CTRL_DOWN_MASK: Int = 1 shl 7

        /** Meta key (same value as AWT's InputEvent.META_DOWN_MASK). On Windows, this corresponds to the Windows key. */
        const val META_DOWN_MASK: Int = 1 shl 8

        /** Alt key (same value as AWT's InputEvent.ALT_DOWN_MASK). */
        const val ALT_DOWN_MASK: Int = 1 shl 9

        /** Left mouse button (same value as AWT's InputEvent.BUTTON1_DOWN_MASK). */
        const val BUTTON1_DOWN_MASK: Int = 1 shl 10

        /** Middle mouse button (same value as AWT's InputEvent.BUTTON2_DOWN_MASK). */
        const val BUTTON2_DOWN_MASK: Int = 1 shl 11

        /** Right mouse button (same value as AWT's InputEvent.BUTTON3_DOWN_MASK). */
        const val BUTTON3_DOWN_MASK: Int = 1 shl 12

        /** AltGraph key (same value as AWT's InputEvent.ALT_GRAPH_DOWN_MASK). */
        const val ALT_GRAPH_DOWN_MASK: Int = 1 shl 13

        /**
         * The [modifiersEx] bit indicating that mouse button number [button] (1 = left, 2 = middle, 3 = right,
         * 4 / 5 = extra buttons) is held down (same value as InputEvent.getMaskForButton). [IllegalArgumentException]
         * if out of range.
         */
        @JvmStatic
        fun getMaskForButton(button: Int): Int {
            require(button in 1..MAX_BUTTON) { "button number must be from 1 to $MAX_BUTTON: $button" }
            return BUTTON_DOWN_MASKS[button - 1]
        }

        /** Number of supported buttons (left, middle, right and two extra buttons). */
        private const val MAX_BUTTON = 5

        /** Table of AWT InputEvent BUTTON_DOWN_MASKs (from the 4th on, starting at the bit after ALT_GRAPH_DOWN_MASK). */
        private val BUTTON_DOWN_MASKS = intArrayOf(BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK, BUTTON3_DOWN_MASK, 1 shl 14, 1 shl 15)

        /** Maps the modifier keys in [modifiersEx] to the old format. */
        @JvmSynthetic
        internal fun keyMasksOf(modifiersEx: Int): Int {
            var result = 0
            if (modifiersEx and SHIFT_DOWN_MASK != 0) result = result or SHIFT_MASK
            if (modifiersEx and CTRL_DOWN_MASK != 0) result = result or CTRL_MASK
            if (modifiersEx and META_DOWN_MASK != 0) result = result or META_MASK
            if (modifiersEx and ALT_DOWN_MASK != 0) result = result or ALT_MASK
            if (modifiersEx and ALT_GRAPH_DOWN_MASK != 0) result = result or ALT_GRAPH_MASK
            return result
        }

        /**
         * Maps the VirtualKeyModifiers format ([Xaml.currentModifiers] and PointerRoutedEventArgs.KeyModifiers: Ctrl=1,
         * Alt=2, Shift=4, Win=8) to the modifier-key bits of [modifiersEx]. The Windows key is [META_DOWN_MASK] (treated
         * the same as [WActionEvent.META_MASK]).
         */
        @JvmSynthetic
        internal fun fromVirtualKeyModifiers(virtualKeyModifiers: Int): Int {
            var result = 0
            if (virtualKeyModifiers and Xaml.MODIFIER_SHIFT != 0) result = result or SHIFT_DOWN_MASK
            if (virtualKeyModifiers and Xaml.MODIFIER_CONTROL != 0) result = result or CTRL_DOWN_MASK
            if (virtualKeyModifiers and Xaml.MODIFIER_WINDOWS != 0) result = result or META_DOWN_MASK
            if (virtualKeyModifiers and Xaml.MODIFIER_MENU != 0) result = result or ALT_DOWN_MASK
            return result
        }
    }
}
