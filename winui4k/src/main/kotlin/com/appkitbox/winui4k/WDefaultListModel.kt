package com.appkitbox.winui4k

import java.util.Enumeration
import java.util.Vector

/**
 * DefaultListModel-style: the standard implementation of a mutable [WListModel] that holds elements in order (wraps a
 * java.util.Vector, as Swing does).
 *
 * Mutating methods notify [WListDataListener]s once, with the same range as Swing (INTERVAL_ADDED for additions,
 * INTERVAL_REMOVED for removals, CONTENTS_CHANGED for [set] / [setElementAt]). Nothing is notified when nothing
 * changes ([clear] on an empty model or an empty [addAll]). Out-of-range indexes throw ArrayIndexOutOfBoundsException,
 * as Vector does. In Swing, [removeRange] with an out-of-range index removes part of the elements before throwing;
 * here it throws without removing anything.
 *
 * Swing's `size()` is [size] in Kotlin (from Java both `size()` and `getSize()` can be used).
 */
open class WDefaultListModel<E> : WAbstractListModel<E>() {
    private val delegate = Vector<E>()

    /** The number of elements. `size()` from Java (Swing's DefaultListModel.size). Same as [getSize]. */
    @get:JvmName("size")
    val size: Int get() = delegate.size

    override fun getSize(): Int = delegate.size

    override fun getElementAt(index: Int): E = delegate.elementAt(index)

    /** Same as [getElementAt] (Swing's DefaultListModel.elementAt). */
    fun elementAt(index: Int): E = delegate.elementAt(index)

    /** Same as [getElementAt] (Swing's DefaultListModel.get). `model[index]` in Kotlin. */
    operator fun get(index: Int): E = delegate[index]

    /** Whether there are no elements. */
    fun isEmpty(): Boolean = delegate.isEmpty()

    /** Whether it contains [element]. */
    operator fun contains(element: Any?): Boolean = delegate.contains(element)

    /** The index of the first occurrence of [element], or -1 if absent. */
    fun indexOf(element: Any?): Int = delegate.indexOf(element)

    /** The index of the first occurrence of [element] at or after [index], or -1 if absent. */
    fun indexOf(element: Any?, index: Int): Int = delegate.indexOf(element, index)

    /** The index of the last occurrence of [element], or -1 if absent. */
    fun lastIndexOf(element: Any?): Int = delegate.lastIndexOf(element)

    /** The index of the last occurrence of [element] at or before [index], or -1 if absent. */
    fun lastIndexOf(element: Any?, index: Int): Int = delegate.lastIndexOf(element, index)

    /** The first element. NoSuchElementException if empty. */
    fun firstElement(): E = delegate.firstElement()

    /** The last element. NoSuchElementException if empty. */
    fun lastElement(): E = delegate.lastElement()

    /** An array of all elements (a copy). */
    fun toArray(): Array<Any?> = delegate.toArray()

    /** An enumeration of all elements. */
    fun elements(): Enumeration<E> = delegate.elements()

    /** Adds [element] at the end (one INTERVAL_ADDED). */
    fun addElement(element: E) {
        val index = delegate.size
        delegate.addElement(element)
        fireIntervalAdded(this, index, index)
    }

    /** Inserts [element] at [index] (one INTERVAL_ADDED). */
    fun add(index: Int, element: E) {
        delegate.add(index, element)
        fireIntervalAdded(this, index, index)
    }

    /** Same as [add] (Swing's DefaultListModel.insertElementAt; the arguments are in reverse order). */
    fun insertElementAt(element: E, index: Int) {
        delegate.insertElementAt(element, index)
        fireIntervalAdded(this, index, index)
    }

    /** Adds the elements of [collection] at the end (one INTERVAL_ADDED; nothing is notified if it is empty). */
    fun addAll(collection: Collection<E>) {
        if (collection.isEmpty()) return
        val startIndex = delegate.size
        delegate.addAll(collection)
        fireIntervalAdded(this, startIndex, delegate.size - 1)
    }

    /** Inserts the elements of [collection] starting at [index] (one INTERVAL_ADDED; nothing is notified if it is empty). */
    fun addAll(index: Int, collection: Collection<E>) {
        if (index < 0 || index > delegate.size) {
            throw ArrayIndexOutOfBoundsException("Index: $index, Size: ${delegate.size}")
        }
        if (collection.isEmpty()) return
        delegate.addAll(index, collection)
        fireIntervalAdded(this, index, index + collection.size - 1)
    }

    /** Replaces the element at [index] with [element] and returns the element before the replacement (one CONTENTS_CHANGED). */
    fun set(index: Int, element: E): E {
        val rv = delegate.set(index, element)
        fireContentsChanged(this, index, index)
        return rv
    }

    /** Same as [set] (Swing's DefaultListModel.setElementAt; the arguments are in reverse order and nothing is returned). */
    fun setElementAt(element: E, index: Int) {
        delegate.setElementAt(element, index)
        fireContentsChanged(this, index, index)
    }

    /** Removes and returns the element at [index] (one INTERVAL_REMOVED). */
    fun remove(index: Int): E {
        val rv = delegate.removeAt(index)
        fireIntervalRemoved(this, index, index)
        return rv
    }

    /** Removes the element at [index] (one INTERVAL_REMOVED). */
    fun removeElementAt(index: Int) {
        delegate.removeElementAt(index)
        fireIntervalRemoved(this, index, index)
    }

    /** Removes the first occurrence of [element]. Returns true if removed (one INTERVAL_REMOVED), false if absent (nothing notified). */
    fun removeElement(element: Any?): Boolean {
        val index = delegate.indexOf(element)
        val removed = delegate.removeElement(element)
        if (index >= 0) fireIntervalRemoved(this, index, index)
        return removed
    }

    /** Removes all elements (one INTERVAL_REMOVED unless it was empty). */
    fun removeAllElements() {
        val lastIndex = delegate.size - 1
        delegate.removeAllElements()
        if (lastIndex >= 0) fireIntervalRemoved(this, 0, lastIndex)
    }

    /** Same as [removeAllElements] (Swing's DefaultListModel.clear). */
    fun clear() {
        removeAllElements()
    }

    /**
     * Removes the elements in [fromIndex]..[toIndex] (inclusive) (one INTERVAL_REMOVED).
     * @throws IllegalArgumentException if [fromIndex] is greater than [toIndex]
     * @throws ArrayIndexOutOfBoundsException if the range does not fit in the model (nothing is removed)
     */
    fun removeRange(fromIndex: Int, toIndex: Int) {
        require(fromIndex <= toIndex) { "fromIndex must be <= toIndex" }
        if (fromIndex < 0 || toIndex >= delegate.size) {
            throw ArrayIndexOutOfBoundsException("Range: $fromIndex..$toIndex, Size: ${delegate.size}")
        }
        for (i in toIndex downTo fromIndex) delegate.removeElementAt(i)
        fireIntervalRemoved(this, fromIndex, toIndex)
    }

    /**
     * Sets the number of elements to [newSize] (Swing's DefaultListModel.setSize, Vector.setSize). Growing fills with null and reports one addition,
     * shrinking reports one removal at the end, and the same size reports nothing.
     * It fills with null even if the element type does not allow null (as in Swing).
     * @throws ArrayIndexOutOfBoundsException if [newSize] is negative (nothing changes)
     */
    fun setSize(newSize: Int) {
        if (newSize < 0) throw ArrayIndexOutOfBoundsException(newSize)
        val oldSize = delegate.size
        delegate.setSize(newSize)
        if (oldSize > newSize) {
            fireIntervalRemoved(this, newSize, oldSize - 1)
        } else if (oldSize < newSize) {
            fireIntervalAdded(this, oldSize, newSize - 1)
        }
    }

    /**
     * Copies all elements to the start of [anArray] (Swing's DefaultListModel.copyInto).
     * @throws IndexOutOfBoundsException if [anArray] is shorter than the number of elements
     */
    fun copyInto(anArray: Array<Any?>) {
        delegate.copyInto(anArray)
    }

    /** The capacity of the inner Vector (Swing's DefaultListModel.capacity). Not the number of elements ([getSize]). */
    fun capacity(): Int = delegate.capacity()

    /** Makes the capacity of the inner Vector at least [minCapacity] (Swing's DefaultListModel.ensureCapacity). Elements do not change, and nothing is reported. */
    fun ensureCapacity(minCapacity: Int) {
        delegate.ensureCapacity(minCapacity)
    }

    /** Shrinks the capacity of the inner Vector to the number of elements (Swing's DefaultListModel.trimToSize). Elements do not change, and nothing is reported. */
    fun trimToSize() {
        delegate.trimToSize()
    }

    override fun toString(): String = delegate.toString()
}
