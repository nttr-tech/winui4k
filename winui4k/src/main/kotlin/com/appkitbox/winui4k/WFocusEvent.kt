package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * FocusEvent-style: an event that tells a [WFocusListener] that a component gained or lost keyboard focus.
 * [getSource] (`source` in Kotlin) and [component] are the component the listener was added to, and [id] is
 * [FOCUS_GAINED] or [FOCUS_LOST].
 *
 * WinUI's GotFocus / LostFocus do not pass the opposite element, and there is no way to look up a wrapper from a native
 * element, so [oppositeComponent] is always null. Focus stays on the element even when the window becomes inactive
 * (no LostFocus arrives), so temporary focus changes do not occur and [isTemporary] is always false.
 *
 * This class does not depend on java.awt, so it defines its own constants with the same values as AWT's FocusEvent.
 */
class WFocusEvent @JvmOverloads constructor(
    source: WComponent,
    /** The kind of event ([FOCUS_GAINED] / [FOCUS_LOST]). In Java, `getID()` as in AWT. */
    @get:JvmName("getID") val id: Int,
    /** Whether this is a temporary focus change (equivalent to FocusEvent.isTemporary). Always false for events created by WinUI4K. */
    val isTemporary: Boolean = false,
    /** The opposite component of the focus change (equivalent to FocusEvent.getOppositeComponent). Always null for events created by WinUI4K. */
    val oppositeComponent: WComponent? = null,
) : EventObject(source) {
    /** The component the event originated from (equivalent to ComponentEvent.getComponent). Same as [getSource]. */
    val component: WComponent get() = source as WComponent

    companion object {
        /** The minimum ID of focus events (the same value as AWT's FocusEvent.FOCUS_FIRST). */
        const val FOCUS_FIRST: Int = 1004

        /** The maximum ID of focus events (the same value as AWT's FocusEvent.FOCUS_LAST). */
        const val FOCUS_LAST: Int = 1005

        /** Gained focus (the same value as AWT's FocusEvent.FOCUS_GAINED). */
        const val FOCUS_GAINED: Int = 1004

        /** Lost focus (the same value as AWT's FocusEvent.FOCUS_LOST). */
        const val FOCUS_LOST: Int = 1005
    }
}

/** FocusListener-style: the listener passed to [WComponent.addFocusListener]. [WFocusAdapter] lets you skip unused methods. */
interface WFocusListener : EventListener {
    /** Called when the component gains focus. */
    fun focusGained(e: WFocusEvent)

    /** Called when the component loses focus. */
    fun focusLost(e: WFocusEvent)
}

/** FocusAdapter-style: a base class implementing all methods of [WFocusListener] as empty. Override only the ones you need. */
abstract class WFocusAdapter : WFocusListener {
    override fun focusGained(e: WFocusEvent) {
        // Does nothing by default
    }

    override fun focusLost(e: WFocusEvent) {
        // Does nothing by default
    }
}
