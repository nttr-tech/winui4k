package com.appkitbox.winui4k

import java.util.EventListener
import java.util.EventObject

/**
 * AdjustmentEvent-like: an event that tells a [WAdjustmentListener] that the value of a [WAdjustable] changed.
 * The source (and [adjustable]) is the scroll bar whose value changed. The ID and type values are the same as Swing's AdjustmentEvent.
 */
class WAdjustmentEvent(
    source: WAdjustable,
    /** The kind of event (always [ADJUSTMENT_VALUE_CHANGED]). `getID()` in Java, as in AWT. */
    @get:JvmName("getID") val id: Int,
    /** How the value changed (AdjustmentEvent.getAdjustmentType; always [TRACK] for WinUI4K scroll bars). */
    val adjustmentType: Int,
    /** The value after the change (AdjustmentEvent.getValue). */
    val value: Int,
    /** Whether the value is in the middle of moving (AdjustmentEvent.getValueIsAdjusting). */
    val valueIsAdjusting: Boolean,
) : EventObject(source) {
    /** The thing whose value changed (AdjustmentEvent.getAdjustable). */
    val adjustable: WAdjustable
        get() = source as WAdjustable

    override fun toString(): String =
        "${javaClass.name}[id=$id,adjustmentType=$adjustmentType,value=$value,valueIsAdjusting=$valueIsAdjusting]"

    companion object {
        /** The value changed (the same value as AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED). */
        const val ADJUSTMENT_VALUE_CHANGED = 601

        /** Increased by one unit (the same value as AdjustmentEvent.UNIT_INCREMENT). */
        const val UNIT_INCREMENT = 1

        /** Decreased by one unit (the same value as AdjustmentEvent.UNIT_DECREMENT). */
        const val UNIT_DECREMENT = 2

        /** Decreased by one page (the same value as AdjustmentEvent.BLOCK_DECREMENT). */
        const val BLOCK_DECREMENT = 3

        /** Increased by one page (the same value as AdjustmentEvent.BLOCK_INCREMENT). */
        const val BLOCK_INCREMENT = 4

        /** The thumb was moved, or the value was changed directly (the same value as AdjustmentEvent.TRACK). */
        const val TRACK = 5
    }
}

/**
 * AdjustmentListener-like: a listener called when the value of a [WAdjustable] changes (can be written as a lambda in Java).
 * It has only one method, so there is no Adapter.
 */
fun interface WAdjustmentListener : EventListener {
    /** Called when the value changes. */
    fun adjustmentValueChanged(e: WAdjustmentEvent)
}
