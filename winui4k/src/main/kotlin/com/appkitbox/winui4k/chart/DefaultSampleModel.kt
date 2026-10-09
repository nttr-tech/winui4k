package com.appkitbox.winui4k.chart

/**
 * Like javax.swing.DefaultListModel: an editable [SampleModel] that holds its elements in a list.
 *
 * Adding, inserting, removing, or replacing elements automatically notifies the corresponding [SampleModelEvent], and a
 * displayed chart is redrawn immediately (e.g. a live display that appends new values and removes old ones from the
 * front).
 */
open class DefaultSampleModel<E>() : AbstractSampleModel<E>() {
    private val elements: MutableList<E> = mutableListOf()

    /** A model initialized with [elements]. */
    constructor(elements: Collection<E>) : this() {
        this.elements.addAll(elements)
    }

    override fun getSize(): Int = elements.size

    override fun getElementAt(index: Int): E = elements[index]

    /** The number of elements (same as [getSize]; DefaultListModel.size). */
    fun size(): Int = elements.size

    /** Whether there are no elements. */
    fun isEmpty(): Boolean = elements.isEmpty()

    /** The [index]-th element. */
    operator fun get(index: Int): E = elements[index]

    /** Replaces the [index]-th element with [element] and returns the element before replacement. */
    operator fun set(index: Int, element: E): E {
        val old = elements.set(index, element)
        fireContentsChanged(index, index)
        return old
    }

    /** Appends [element] to the end (DefaultListModel.addElement). */
    fun addElement(element: E) {
        elements += element
        fireIntervalAdded(elements.size - 1, elements.size - 1)
    }

    /** Inserts [element] at [index]. */
    fun add(index: Int, element: E) {
        elements.add(index, element)
        fireIntervalAdded(index, index)
    }

    /** Appends all of [newElements] to the end. Does nothing if it is empty. */
    fun addAll(newElements: Collection<E>) {
        addAll(elements.size, newElements)
    }

    /** Inserts all of [newElements] at [index]. Does nothing if it is empty. */
    fun addAll(index: Int, newElements: Collection<E>) {
        if (newElements.isEmpty()) return
        elements.addAll(index, newElements)
        fireIntervalAdded(index, index + newElements.size - 1)
    }

    /** Removes the [index]-th element and returns the removed element. */
    fun remove(index: Int): E {
        val removed = elements.removeAt(index)
        fireIntervalRemoved(index, index)
        return removed
    }

    /** Removes the first element equal to [element] (DefaultListModel.removeElement). Returns true if removed. */
    fun removeElement(element: E): Boolean {
        val index = elements.indexOf(element)
        if (index < 0) return false
        remove(index)
        return true
    }

    /** Removes the elements [fromIndex]..[toIndex] (both inclusive) (DefaultListModel.removeRange). */
    fun removeRange(fromIndex: Int, toIndex: Int) {
        require(fromIndex <= toIndex) { "fromIndex must be <= toIndex: $fromIndex > $toIndex" }
        elements.subList(fromIndex, toIndex + 1).clear()
        fireIntervalRemoved(fromIndex, toIndex)
    }

    /** Removes all elements (DefaultListModel.removeAllElements). Does nothing if it is empty. */
    fun removeAllElements() {
        if (elements.isEmpty()) return
        val last = elements.size - 1
        elements.clear()
        fireIntervalRemoved(0, last)
    }

    /** Replaces all elements with [newElements]. Notifications are combined into two: a removal and an addition. */
    fun setAll(newElements: Collection<E>) {
        removeAllElements()
        addAll(newElements)
    }

    /** Whether an element equal to [element] is contained. */
    fun contains(element: E): Boolean = elements.contains(element)

    /** The position of the first element equal to [element], or -1 if none. */
    fun indexOf(element: E): Int = elements.indexOf(element)

    /** A copy of all elements. */
    fun toList(): List<E> = elements.toList()

    override fun toString(): String = elements.toString()

    companion object {
        /** A model initialized with [elements] (from Java, `DefaultSampleModel.of(1.0, 2.0, 3.0)`). */
        @JvmStatic
        fun <E> of(vararg elements: E): DefaultSampleModel<E> = DefaultSampleModel(elements.asList())
    }
}
