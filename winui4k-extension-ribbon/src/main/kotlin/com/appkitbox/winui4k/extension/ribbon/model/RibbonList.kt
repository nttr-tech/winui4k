package com.appkitbox.winui4k.extension.ribbon.model

import java.util.EventObject

/**
 * Like javax.swing.event.ListDataEvent: the details of a change to a [RibbonList].
 *
 * An addition ([Type.ADDED]) means [elements] were inserted at [index], a removal ([Type.REMOVED]) means [elements]
 * were removed from [index], and a replacement ([Type.REPLACED]) means the elements at [index] were replaced from
 * [oldElements] with [elements].
 */
class RibbonListEvent<E>(
    source: RibbonList<E>,
    /** The kind of change. */
    val type: Type,
    /** The first index of the changed range. */
    val index: Int,
    /** The elements after the addition or replacement (the removed elements for a removal). */
    val elements: List<E>,
    /** The elements before the replacement (empty except for replacements). */
    val oldElements: List<E> = emptyList(),
) : EventObject(source) {
    /** The kind of change. */
    enum class Type {
        /** Elements were added. */
        ADDED,

        /** Elements were removed. */
        REMOVED,

        /** Elements were replaced. */
        REPLACED,
    }

    /** The list that changed. */
    @Suppress("UNCHECKED_CAST") // The constructor accepts only RibbonList<E>
    override fun getSource(): RibbonList<E> = super.getSource() as RibbonList<E>

    override fun toString(): String = "RibbonListEvent($type, index=$index, elements=$elements, oldElements=$oldElements)"
}

/** Like javax.swing.event.ListDataListener: receives change notifications from a [RibbonList]. */
fun interface RibbonListListener<E> {
    /** Called when the list changes. */
    fun listChanged(event: RibbonListEvent<E>)
}

/**
 * A list that notifies changes (the equivalent of RibbonSpace's ObservableCollection and Swing's DefaultListModel).
 *
 * It can be used as an ordinary java.util.List (`tab.groups += group` / `groups.add(group)`) and notifies
 * [RibbonListListener]s with a [RibbonListEvent] on every change. Views apply only the differences from these
 * notifications. [addAll] and [clear] notify only once for the whole operation.
 * Notifications are delivered through [RibbonNotifications.dispatcher] (on the UI thread when there is a view).
 */
class RibbonList<E> @JvmOverloads constructor(initial: Collection<E> = emptyList()) :
    AbstractMutableList<E>(),
    RandomAccess {
    private val items = ArrayList(initial)
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<RibbonListListener<E>>()

    override val size: Int get() = items.size

    override fun get(index: Int): E = items[index]

    override fun add(index: Int, element: E) {
        items.add(index, element)
        fire(RibbonListEvent(this, RibbonListEvent.Type.ADDED, index, listOf(element)))
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        if (elements.isEmpty()) return false
        val added = elements.toList()
        items.addAll(index, added)
        fire(RibbonListEvent(this, RibbonListEvent.Type.ADDED, index, added))
        return true
    }

    override fun addAll(elements: Collection<E>): Boolean = addAll(items.size, elements)

    override fun removeAt(index: Int): E {
        val removed = items.removeAt(index)
        fire(RibbonListEvent(this, RibbonListEvent.Type.REMOVED, index, listOf(removed)))
        return removed
    }

    override fun set(index: Int, element: E): E {
        val old = items.set(index, element)
        if (old !== element) {
            fire(RibbonListEvent(this, RibbonListEvent.Type.REPLACED, index, listOf(element), listOf(old)))
        }
        return old
    }

    override fun clear() {
        if (items.isEmpty()) return
        val removed = items.toList()
        items.clear()
        fire(RibbonListEvent(this, RibbonListEvent.Type.REMOVED, 0, removed))
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        if (fromIndex >= toIndex) return
        val range = items.subList(fromIndex, toIndex)
        val removed = range.toList()
        range.clear()
        fire(RibbonListEvent(this, RibbonListEvent.Type.REMOVED, fromIndex, removed))
    }

    /** Moves the element at [from] to [to] (notifies twice: a removal and an addition). */
    fun move(from: Int, to: Int) {
        if (from == to) return
        val element = removeAt(from)
        add(to, element)
    }

    /** Replaces the contents with [elements] (notifies twice: removal of all and addition of all). */
    fun setAll(elements: Collection<E>) {
        clear()
        addAll(elements)
    }

    /** Subscribes to change notifications. */
    fun addListListener(listener: RibbonListListener<E>) {
        listeners += listener
    }

    /** Removes a listener registered with [addListListener]. */
    fun removeListListener(listener: RibbonListListener<E>) {
        listeners -= listener
    }

    /** The registered listeners. */
    fun getListListeners(): List<RibbonListListener<E>> = listeners.toList()

    private fun fire(event: RibbonListEvent<E>) {
        modCount++
        if (listeners.isEmpty()) return
        RibbonNotifications.deliver { for (listener in listeners) listener.listChanged(event) }
    }
}
