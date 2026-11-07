package com.appkitbox.winui4k

import java.util.Vector

/**
 * DefaultComboBoxModel-like: the standard implementation of a mutable [WMutableComboBoxModel] that holds items in order (wrapping a java.util.Vector, as in Swing).
 *
 * The same behavior as Swing:
 * - Creating it from an array or List selects the first item. Adding the first item to an empty model with [addElement] also selects it
 * - Removing the selected item with [removeElementAt] / [removeElement] selects the adjacent item (the next one for the first item, otherwise the previous one)
 * - Changes of [selectedItem] are notified with `contentsChanged(-1, -1)`, additions with INTERVAL_ADDED, and removals with INTERVAL_REMOVED
 *   (listeners receive them starting from the last one added; see [WAbstractListModel])
 * - [getElementAt] returns null when out of range (even if E is non-nullable in Kotlin)
 *
 * This is also the model [WComboBox] creates internally with every constructor, and it moves the selection, as with JComboBox.
 *
 * @constructor DefaultComboBoxModel()-like: creates an empty model.
 */
open class WDefaultComboBoxModel<E>() : WAbstractListModel<E>(), WMutableComboBoxModel<E> {
    private val objects = Vector<E>()

    private var selectedObject: Any? = null

    /** DefaultComboBoxModel(E[])-like: creates a model with [items] as its items and the first one selected (unselected if empty). */
    constructor(items: Array<out E>) : this() {
        objects.addAll(items)
        if (objects.isNotEmpty()) selectedObject = objects[0]
    }

    /** DefaultComboBoxModel(Vector)-like: creates a model with [items] as its items and the first one selected (unselected if empty). */
    constructor(items: List<E>) : this() {
        objects.addAll(items)
        if (objects.isNotEmpty()) selectedObject = objects[0]
    }

    /** The selected item. Notifies with `contentsChanged(-1, -1)` only when it changes (setting an equal value does not notify). */
    override var selectedItem: Any?
        get() = selectedObject
        set(value) {
            if (selectedObject != value) {
                selectedObject = value
                fireContentsChanged(this, -1, -1)
            }
        }

    override fun getSize(): Int = objects.size

    @Suppress("UNCHECKED_CAST")
    override fun getElementAt(index: Int): E = if (index in objects.indices) objects[index] else null as E

    /** DefaultComboBoxModel.getIndexOf-like: the index of the first item equal to [obj] (-1 if none). */
    fun getIndexOf(obj: Any?): Int = objects.indexOf(obj)

    override fun addElement(item: E) {
        objects.addElement(item)
        fireIntervalAdded(this, objects.size - 1, objects.size - 1)
        val isFirst = objects.size == 1 && selectedObject == null
        if (isFirst && item != null) selectedItem = item
    }

    override fun insertElementAt(item: E, index: Int) {
        objects.insertElementAt(item, index)
        fireIntervalAdded(this, index, index)
    }

    override fun removeElementAt(index: Int) {
        if (getElementAt(index) === selectedObject) {
            selectedItem = when {
                index == 0 -> if (getSize() == 1) null else getElementAt(index + 1)
                else -> getElementAt(index - 1)
            }
        }
        objects.removeElementAt(index)
        fireIntervalRemoved(this, index, index)
    }

    override fun removeElement(obj: Any?) {
        val index = objects.indexOf(obj)
        if (index != -1) removeElementAt(index)
    }

    /** DefaultComboBoxModel.removeAllElements-like: removes all items and deselects (the selection change is notified only by INTERVAL_REMOVED, as in Swing). */
    fun removeAllElements() {
        if (objects.isNotEmpty()) {
            val lastIndex = objects.size - 1
            objects.removeAllElements()
            selectedObject = null
            fireIntervalRemoved(this, 0, lastIndex)
        } else {
            selectedObject = null
        }
    }

    /** DefaultComboBoxModel.addAll-like: adds [items] at the end at once (does not change the selection; does not notify if empty). */
    fun addAll(items: Collection<E>) {
        if (items.isEmpty()) return
        val startIndex = getSize()
        objects.addAll(items)
        fireIntervalAdded(this, startIndex, getSize() - 1)
    }

    /** DefaultComboBoxModel.addAll(int, Collection)-like: inserts [items] at [index] at once. Throws ArrayIndexOutOfBoundsException if out of range. */
    fun addAll(index: Int, items: Collection<E>) {
        if (index < 0 || index > getSize()) throw ArrayIndexOutOfBoundsException("index out of range: $index")
        if (items.isEmpty()) return
        objects.addAll(index, items)
        fireIntervalAdded(this, index, index + items.size - 1)
    }
}
