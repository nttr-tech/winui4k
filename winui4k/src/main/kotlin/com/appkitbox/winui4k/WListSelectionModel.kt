package com.appkitbox.winui4k

/**
 * ListSelectionModel-style: a model that holds the selection of the rows of a list or table (a set of row indexes and the two row indexes anchor / lead).
 *
 * Row indexes start at 0 and -1 means "none". Selection changes are notified to [WListSelectionListener]s with a [WListSelectionEvent].
 * The default implementation is [WDefaultListSelectionModel].
 *
 * The selection mode constants have the same values as Swing's ListSelectionModel.
 */
interface WListSelectionModel {
    /**
     * Makes the selection only [index0]..[index1] (either may be the larger). The anchor becomes [index0] and the lead [index1].
     * Does nothing if either is -1. In [SINGLE_SELECTION] only the one row [index1] is selected.
     */
    fun setSelectionInterval(index0: Int, index1: Int)

    /**
     * Adds [index0]..[index1] to the selection. The anchor becomes [index0] and the lead [index1]. Does nothing if either is -1.
     * Same as [setSelectionInterval] in [SINGLE_SELECTION]. In [SINGLE_INTERVAL_SELECTION], a range that would make the selection non-contiguous
     * is not added; the selection becomes only that range instead.
     */
    fun addSelectionInterval(index0: Int, index1: Int)

    /**
     * Removes [index0]..[index1] from the selection. The anchor becomes [index0] and the lead [index1]. Does nothing if either is -1.
     * Outside [MULTIPLE_INTERVAL_SELECTION], if the removal would split the selection in two, the range is extended to the end of the selection before removing.
     */
    fun removeSelectionInterval(index0: Int, index1: Int)

    /** The smallest selected row, or -1 if nothing is selected. */
    fun getMinSelectionIndex(): Int

    /** The largest selected row, or -1 if nothing is selected. */
    fun getMaxSelectionIndex(): Int

    /** True if the row at [index] is selected. */
    fun isSelectedIndex(index: Int): Boolean

    /** The starting point of the selection (anchor), or -1 if none. */
    fun getAnchorSelectionIndex(): Int

    /** Sets the anchor to [index]. Does not change the selection. */
    fun setAnchorSelectionIndex(index: Int)

    /** The end point of the selection (lead), or -1 if none. */
    fun getLeadSelectionIndex(): Int

    /** Sets the lead to [index] and makes the selection of the anchor..lead range match the anchor's selection state (same as Swing's setLeadSelectionIndex). */
    fun setLeadSelectionIndex(index: Int)

    /** True if nothing is selected. */
    fun isSelectionEmpty(): Boolean

    /** Clears the whole selection. Does not change the anchor and lead. */
    fun clearSelection()

    /**
     * Shifts the selection back to match [length] rows inserted at [index]. If [before] is true they go before [index],
     * otherwise after it. Inserted rows inherit the selection state of the row at [index] (unselected in [SINGLE_SELECTION]).
     */
    fun insertIndexInterval(index: Int, length: Int, before: Boolean)

    /** Shifts the selection forward to match the removal of rows [index0]..[index1]. */
    fun removeIndexInterval(index0: Int, index1: Int)

    /** True while an operation is in progress. */
    fun getValueIsAdjusting(): Boolean

    /**
     * Sets whether an operation is in progress. Changes while it is true are combined into one event, and when it is set back to false,
     * one event for the whole range changed in the meantime is fired.
     */
    fun setValueIsAdjusting(valueIsAdjusting: Boolean)

    /** The selection mode ([SINGLE_SELECTION] / [SINGLE_INTERVAL_SELECTION] / [MULTIPLE_INTERVAL_SELECTION]). */
    fun getSelectionMode(): Int

    /** Sets the selection mode. [IllegalArgumentException] for anything other than the 3 constants. */
    fun setSelectionMode(selectionMode: Int)

    /** Returns the selected rows in ascending order. */
    fun getSelectedIndices(): IntArray

    /** The number of selected rows. */
    fun getSelectedItemsCount(): Int

    /** Adds a listener. Adding the same listener twice makes it called twice. */
    fun addListSelectionListener(listener: WListSelectionListener)

    /** Removes a listener (if added several times, the one added last). */
    fun removeListSelectionListener(listener: WListSelectionListener)

    companion object {
        /** Only one row can be selected. */
        const val SINGLE_SELECTION: Int = 0

        /** Only one contiguous range can be selected. */
        const val SINGLE_INTERVAL_SELECTION: Int = 1

        /** Multiple non-contiguous ranges can be selected. */
        const val MULTIPLE_INTERVAL_SELECTION: Int = 2
    }
}
