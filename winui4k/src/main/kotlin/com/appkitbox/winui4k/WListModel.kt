package com.appkitbox.winui4k

/**
 * ListModel-style: a model that holds list data. [WAbstractListModel] is the skeleton and [WDefaultListModel] the
 * standard implementation.
 *
 * The contents are read with [getSize] and [getElementAt], and listeners ([WListDataListener]) are notified when the
 * contents change. As in Swing, a read-only model can be made as a subclass of [WAbstractListModel] that implements
 * only [getSize] and [getElementAt].
 */
interface WListModel<E> {
    /** The number of elements. */
    fun getSize(): Int

    /** The element at [index]. The behavior for an out-of-range index depends on the implementation ([WDefaultListModel] throws ArrayIndexOutOfBoundsException). */
    fun getElementAt(index: Int): E

    /** Adds a listener that receives content changes (adding the same listener twice makes it called twice). */
    fun addListDataListener(listener: WListDataListener)

    /** Removes one listener added with [addListDataListener]. */
    fun removeListDataListener(listener: WListDataListener)
}
