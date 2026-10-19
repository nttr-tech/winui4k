package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * ItemEvent-like: an event that tells a [WItemListener] that the selection state of a toggle component ([WToggleButton] /
 * [WCheckBox] / [WRadioButton] / [WToggleSplitButton] / [WToggleSwitch]) has changed.
 * [getSource] (`source` in Kotlin) and [itemSelectable] are the component whose state changed, and [item] is the same
 * component (just as Swing's JToggleButton puts itself in item). [stateChange] is the new state: besides [SELECTED]
 * (checked) / [DESELECTED] (unchecked), a three-state [WToggleButton.isThreeState] also delivers [INDETERMINATE].
 * Swing's `e.getStateChange() == ItemEvent.SELECTED` check works as is.
 *
 * This class does not depend on java.awt, so it holds its own constants with the same values as AWT's ItemEvent (only
 * [INDETERMINATE] is not in AWT).
 */
class WItemEvent @JvmOverloads constructor(
    source: WComponent,
    /** The new state ([SELECTED] / [DESELECTED] / [INDETERMINATE]). */
    val stateChange: Int,
    /** The item whose state changed (equivalent to ItemEvent.getItem). For toggle components, the component itself. */
    val item: Any = source,
) : EventObject(source) {
    /** The component whose state changed (equivalent to ItemEvent.getItemSelectable). Same as [getSource]. */
    val itemSelectable: WComponent get() = source as WComponent

    /** The event type. In Java it is `getID()`, as in AWT, and always [ITEM_STATE_CHANGED]. */
    @get:JvmName("getID")
    val id: Int get() = ITEM_STATE_CHANGED

    companion object {
        /** The smallest item event ID (same value as AWT's ItemEvent.ITEM_FIRST). */
        const val ITEM_FIRST: Int = 701

        /** The largest item event ID (same value as AWT's ItemEvent.ITEM_LAST). */
        const val ITEM_LAST: Int = 701

        /** The item state changed (same value as AWT's ItemEvent.ITEM_STATE_CHANGED). */
        const val ITEM_STATE_CHANGED: Int = 701

        /** Became checked (same value as AWT's ItemEvent.SELECTED). */
        const val SELECTED: Int = 1

        /** Became unchecked (same value as AWT's ItemEvent.DESELECTED). */
        const val DESELECTED: Int = 2

        /** Became indeterminate (three-state [WToggleButton] only; not in AWT). */
        const val INDETERMINATE: Int = 3

        /** Converts [stateChange] to a [WToggleButton.isChecked] value (true / false / null for indeterminate). */
        @JvmSynthetic
        internal fun checkedOf(stateChange: Int): Boolean? = when (stateChange) {
            SELECTED -> true
            INDETERMINATE -> null
            else -> false
        }
    }
}

/**
 * ItemListener-like: a listener passed to [WToggleButton.addItemListener] and others (can be written as a lambda in Java).
 * In Kotlin, `addItemListener { checked -> }` resolves to the overload that receives only a Boolean, so to receive the
 * event write `addItemListener(WItemListener { e -> })`.
 */
fun interface WItemListener : EventListener {
    /** Called when the selection state changes. */
    fun itemStateChanged(e: WItemEvent)
}
