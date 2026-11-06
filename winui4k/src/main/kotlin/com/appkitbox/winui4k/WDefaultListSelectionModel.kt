package com.appkitbox.winui4k

import java.util.BitSet
import java.util.concurrent.CopyOnWriteArrayList

/**
 * DefaultListSelectionModel-style: the standard implementation of [WListSelectionModel]. Holds the selection in a [BitSet].
 *
 * Changes the selection and fires events with the same rules as Swing's DefaultListSelectionModel.
 * - One operation fires one event, covering from the minimum to the maximum of the changed range (including the rows the anchor / lead moved from and to). Nothing is fired if nothing changed.
 * - While [setValueIsAdjusting] is true, changes are accumulated (events fired in the meantime have valueIsAdjusting true),
 *   and when it is set back to false, one event covering the whole accumulated range (with valueIsAdjusting false) is fired.
 * - Listeners are called starting from the most recently added one (same as Swing's EventListenerList).
 *
 * The initial selection mode is [WListSelectionModel.MULTIPLE_INTERVAL_SELECTION], as in Swing.
 * Swing's clone and serialization are not ported.
 */
open class WDefaultListSelectionModel : WListSelectionModel {
    private var selectionMode = WListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    private var minIndex = MAX
    private var maxIndex = MIN
    private var anchorIndex = -1
    private var leadIndex = -1
    private var firstAdjustedIndex = MAX
    private var lastAdjustedIndex = MIN
    private var adjusting = false
    private var firstChangedIndex = MAX
    private var lastChangedIndex = MIN
    private val value = BitSet(INITIAL_BITS)
    private val listeners = CopyOnWriteArrayList<WListSelectionListener>()

    /**
     * True to include anchor / lead changes in the notified range (the default; Swing's isLeadAnchorNotificationEnabled).
     * When false, changes of only the anchor / lead fire no event unless the selected rows change.
     */
    var isLeadAnchorNotificationEnabled: Boolean = true

    override fun getMinSelectionIndex(): Int = if (isSelectionEmpty()) -1 else minIndex

    override fun getMaxSelectionIndex(): Int = maxIndex

    override fun isSelectedIndex(index: Int): Boolean = index in minIndex..maxIndex && value.get(index)

    override fun getAnchorSelectionIndex(): Int = anchorIndex

    override fun getLeadSelectionIndex(): Int = leadIndex

    override fun isSelectionEmpty(): Boolean = minIndex > maxIndex

    override fun getValueIsAdjusting(): Boolean = adjusting

    override fun getSelectionMode(): Int = selectionMode

    /**
     * Changes the selection mode. As with Swing since JDK 9, changing to a narrower mode shrinks the current selection
     * (to only the smallest row for SINGLE_SELECTION, and to only the range continuing from the smallest row for SINGLE_INTERVAL_SELECTION).
     * @throws IllegalArgumentException if it is none of the three constants
     */
    override fun setSelectionMode(selectionMode: Int) {
        require(
            selectionMode == WListSelectionModel.SINGLE_SELECTION ||
                selectionMode == WListSelectionModel.SINGLE_INTERVAL_SELECTION ||
                selectionMode == WListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
        ) { "invalid selectionMode" }
        val oldMode = this.selectionMode
        this.selectionMode = selectionMode
        // As since JDK 9, changing to a narrower mode shrinks the current selection to that mode (the smallest row, or the range continuing from it)
        if (oldMode <= selectionMode || isSelectionEmpty()) return
        if (selectionMode == WListSelectionModel.SINGLE_SELECTION) {
            setSelectionInterval(minIndex, minIndex)
        } else {
            var end = minIndex
            while (value.get(end + 1)) end++
            setSelectionInterval(minIndex, end)
        }
    }

    override fun getSelectedIndices(): IntArray {
        if (isSelectionEmpty()) return IntArray(0)
        val result = IntArray(value.cardinality())
        var count = 0
        for (i in minIndex..maxIndex) {
            if (value.get(i)) result[count++] = i
        }
        return result
    }

    override fun getSelectedItemsCount(): Int = value.cardinality()

    override fun addListSelectionListener(listener: WListSelectionListener) {
        listeners += listener
    }

    override fun removeListSelectionListener(listener: WListSelectionListener) {
        val index = listeners.lastIndexOf(listener)
        if (index >= 0) listeners.removeAt(index)
    }

    /** The registered listeners (in registration order). */
    fun getListSelectionListeners(): Array<WListSelectionListener> = listeners.toTypedArray()

    /** Notifies the listeners of a range. Uses [getValueIsAdjusting] for whether an operation is in progress. */
    protected fun fireValueChanged(firstIndex: Int, lastIndex: Int) {
        fireValueChanged(firstIndex, lastIndex, getValueIsAdjusting())
    }

    /** Notifies the listeners of a range and whether an operation is in progress. Listeners are called starting from the most recently added one. */
    protected fun fireValueChanged(firstIndex: Int, lastIndex: Int, isAdjusting: Boolean) {
        val e = WListSelectionEvent(this, firstIndex, lastIndex, isAdjusting)
        // Walk the list as it was before the calls in reverse order, so that listeners added or removed during the calls have no effect
        for (listener in listeners.toTypedArray().reversed()) listener.valueChanged(e)
    }

    private fun fireValueChanged(isAdjusting: Boolean) {
        if (lastChangedIndex == MIN) return
        val oldFirst = firstChangedIndex
        val oldLast = lastChangedIndex
        firstChangedIndex = MAX
        lastChangedIndex = MIN
        fireValueChanged(oldFirst, oldLast, isAdjusting)
    }

    private fun fireValueChanged() {
        if (lastAdjustedIndex == MIN) return
        // While an operation is in progress, accumulate the changed range, and fire one event covering all of it when set back to false
        if (getValueIsAdjusting()) {
            firstChangedIndex = minOf(firstChangedIndex, firstAdjustedIndex)
            lastChangedIndex = maxOf(lastChangedIndex, lastAdjustedIndex)
        }
        val oldFirst = firstAdjustedIndex
        val oldLast = lastAdjustedIndex
        firstAdjustedIndex = MAX
        lastAdjustedIndex = MIN
        fireValueChanged(oldFirst, oldLast)
    }

    private fun markAsDirty(row: Int) {
        if (row == -1) return
        firstAdjustedIndex = minOf(firstAdjustedIndex, row)
        lastAdjustedIndex = maxOf(lastAdjustedIndex, row)
    }

    private fun updateLeadAnchorIndices(newAnchor: Int, newLead: Int) {
        if (isLeadAnchorNotificationEnabled) {
            if (anchorIndex != newAnchor) {
                markAsDirty(anchorIndex)
                markAsDirty(newAnchor)
            }
            if (leadIndex != newLead) {
                markAsDirty(leadIndex)
                markAsDirty(newLead)
            }
        }
        anchorIndex = newAnchor
        leadIndex = newLead
    }

    private fun set(row: Int) {
        if (value.get(row)) return
        value.set(row)
        markAsDirty(row)
        minIndex = minOf(minIndex, row)
        maxIndex = maxOf(maxIndex, row)
    }

    private fun clear(row: Int) {
        if (!value.get(row)) return
        value.clear(row)
        markAsDirty(row)
        // Search again for the next selected row only when the minimum or maximum row was deselected
        if (row == minIndex) {
            minIndex++
            while (minIndex <= maxIndex && !value.get(minIndex)) minIndex++
        }
        if (row == maxIndex) {
            maxIndex--
            while (minIndex <= maxIndex && !value.get(maxIndex)) maxIndex--
        }
        if (isSelectionEmpty()) {
            minIndex = MAX
            maxIndex = MIN
        }
    }

    private fun setState(row: Int, state: Boolean) {
        if (state) set(row) else clear(row)
    }

    @Suppress("LongParameterList") // The same 5 arguments as Swing's changeSelection (range to clear, range to set, handling of overlapping rows)
    private fun changeSelection(clearMin: Int, clearMax: Int, setMin: Int, setMax: Int, clearFirst: Boolean = true) {
        for (i in minOf(setMin, clearMin)..maxOf(setMax, clearMax)) {
            var shouldClear = i in clearMin..clearMax
            var shouldSet = i in setMin..setMax
            if (shouldSet && shouldClear) {
                if (clearFirst) shouldClear = false else shouldSet = false
            }
            if (shouldSet) set(i)
            if (shouldClear) clear(i)
        }
        fireValueChanged()
    }

    override fun clearSelection() {
        removeSelectionIntervalImpl(minIndex, maxIndex, false)
    }

    override fun setSelectionInterval(index0: Int, index1: Int) {
        if (index0 == -1 || index1 == -1) return
        val from = if (getSelectionMode() == WListSelectionModel.SINGLE_SELECTION) index1 else index0
        updateLeadAnchorIndices(from, index1)
        changeSelection(minIndex, maxIndex, minOf(from, index1), maxOf(from, index1))
    }

    override fun addSelectionInterval(index0: Int, index1: Int) {
        if (index0 == -1 || index1 == -1) return
        if (getSelectionMode() == WListSelectionModel.SINGLE_SELECTION) {
            setSelectionInterval(index0, index1)
            return
        }
        updateLeadAnchorIndices(index0, index1)
        val setMin = minOf(index0, index1)
        val setMax = maxOf(index0, index1)
        // An addition that would make the range non-contiguous selects only the new range
        if (getSelectionMode() == WListSelectionModel.SINGLE_INTERVAL_SELECTION &&
            (setMax < minIndex - 1 || setMin > maxIndex + 1)
        ) {
            setSelectionInterval(index0, index1)
            return
        }
        changeSelection(MAX, MIN, setMin, setMax)
    }

    override fun removeSelectionInterval(index0: Int, index1: Int) {
        removeSelectionIntervalImpl(index0, index1, true)
    }

    private fun removeSelectionIntervalImpl(index0: Int, index1: Int, changeLeadAnchor: Boolean) {
        if (index0 == -1 || index1 == -1) return
        if (changeLeadAnchor) updateLeadAnchorIndices(index0, index1)
        val clearMin = minOf(index0, index1)
        var clearMax = maxOf(index0, index1)
        // A removal that would split the selection in two removes up to the end of the selection
        if (getSelectionMode() != WListSelectionModel.MULTIPLE_INTERVAL_SELECTION &&
            clearMin > minIndex &&
            clearMax < maxIndex
        ) {
            clearMax = maxIndex
        }
        changeSelection(clearMin, clearMax, MAX, MIN)
    }

    /** @throws IndexOutOfBoundsException if [index] or [length] is negative (as in Swing) */
    override fun insertIndexInterval(index: Int, length: Int, before: Boolean) {
        if (!needsInsert(index, length)) return
        val insMinIndex = if (before) index else index + 1
        val insMaxIndex = insMinIndex + length - 1
        // Shift the selection after the insertion point back by length
        for (i in maxIndex downTo insMinIndex) setState(i + length, value.get(i))
        // Inserted rows inherit the selection state of the row at index (unselected in single selection)
        val insertedState = getSelectionMode() != WListSelectionModel.SINGLE_SELECTION && value.get(index)
        for (i in insMinIndex..insMaxIndex) setState(i, insertedState)
        var newLead = leadIndex
        if (newLead > index || (before && newLead == index)) newLead += length
        var newAnchor = anchorIndex
        if (newAnchor > index || (before && newAnchor == index)) newAnchor += length
        if (newLead != leadIndex || newAnchor != anchorIndex) updateLeadAnchorIndices(newAnchor, newLead)
        fireValueChanged()
    }

    /** Checks the arguments of [insertIndexInterval], and returns true if the selection needs to change (negative values throw IndexOutOfBoundsException, as in Swing). */
    private fun needsInsert(index: Int, length: Int): Boolean {
        if (length < 0 || index < 0) throw IndexOutOfBoundsException("index or length is negative")
        return index != MAX && length != 0
    }

    /** @throws IndexOutOfBoundsException if either row is negative (as in Swing) */
    override fun removeIndexInterval(index0: Int, index1: Int) {
        if (index0 < 0 || index1 < 0) throw IndexOutOfBoundsException("index is negative")
        val rmMinIndex = minOf(index0, index1)
        val rmMaxIndex = maxOf(index0, index1)
        val gapLength = rmMaxIndex - rmMinIndex + 1
        // Shift the selection forward to close up the removed range
        for (i in rmMinIndex..maxIndex) setState(i, value.get(i + gapLength))
        val newLead = shiftAfterRemove(leadIndex, rmMinIndex, rmMaxIndex, gapLength)
        val newAnchor = shiftAfterRemove(anchorIndex, rmMinIndex, rmMaxIndex, gapLength)
        if (newLead != leadIndex || newAnchor != anchorIndex) updateLeadAnchorIndices(newAnchor, newLead)
        fireValueChanged()
    }

    private fun shiftAfterRemove(index: Int, rmMin: Int, rmMax: Int, gapLength: Int): Int = when {
        index == 0 && rmMin == 0 -> index
        index > rmMax -> index - gapLength
        index >= rmMin -> rmMin - 1
        else -> index
    }

    override fun setValueIsAdjusting(valueIsAdjusting: Boolean) {
        if (valueIsAdjusting != adjusting) {
            adjusting = valueIsAdjusting
            fireValueChanged(valueIsAdjusting)
        }
    }

    override fun setAnchorSelectionIndex(index: Int) {
        updateLeadAnchorIndices(index, leadIndex)
        fireValueChanged()
    }

    override fun setLeadSelectionIndex(index: Int) {
        var newAnchor = anchorIndex
        // The lead can be set to -1 only when the anchor is also -1. When the anchor is -1, a lead other than -1 cannot be set
        if (index == -1) {
            if (newAnchor == -1) {
                updateLeadAnchorIndices(newAnchor, index)
                fireValueChanged()
            }
            return
        } else if (newAnchor == -1) {
            return
        }
        if (leadIndex == -1) leadIndex = index
        var shouldSelect = value.get(anchorIndex)
        if (getSelectionMode() == WListSelectionModel.SINGLE_SELECTION) {
            newAnchor = index
            shouldSelect = true
        }
        val oldMin = minOf(anchorIndex, leadIndex)
        val oldMax = maxOf(anchorIndex, leadIndex)
        val newMin = minOf(newAnchor, index)
        val newMax = maxOf(newAnchor, index)
        updateLeadAnchorIndices(newAnchor, index)
        if (shouldSelect) {
            changeSelection(oldMin, oldMax, newMin, newMax)
        } else {
            changeSelection(newMin, newMax, oldMin, oldMax, false)
        }
    }

    override fun toString(): String =
        "${javaClass.name} {mode=$selectionMode, anchor=$anchorIndex, lead=$leadIndex, adjusting=$adjusting}"

    private companion object {
        const val MIN = -1
        const val MAX = Int.MAX_VALUE
        const val INITIAL_BITS = 32
    }
}
