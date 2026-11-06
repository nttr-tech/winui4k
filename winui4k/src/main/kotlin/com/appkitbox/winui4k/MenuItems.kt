package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.FoundationInterop

/**
 * Item management shared by the menu containers (WMenuBar / WMenuBarItem / WMenuFlyoutSubItem / WMenuFlyout).
 * Updates the native IVector (MenuBar.Items / MenuBarItem.Items / MenuFlyoutSubItem.Items / MenuFlyout.Items) together with
 * a list of wrappers in the same order. All adding and removing of items is assumed to go through this class, and positions are decided by the wrapper list.
 *
 * As with Swing's Container.addImpl, adding an item that is in another container (or the same container) first removes it from the original container
 * (WinUI cannot put an element that has a parent into other Items). Which container an item is in is remembered on the item with [ownerOf] / [setOwner].
 */
internal class MenuItems<T : Any>(
    /** The IVector of the items (obtained when called; pass the container's lazy property). */
    private val vector: () -> ComPtr,
    /** The pointer of the IVector element type (the MenuFlyoutItemBase / MenuBarItem view). */
    private val nativeOf: (T) -> ComPtr,
    private val ownerOf: (T) -> MenuItems<T>?,
    private val setOwner: (T, MenuItems<T>?) -> Unit,
) {
    private val items = ArrayList<T>()

    /** The items (in order). */
    val list: List<T> get() = items

    val size: Int get() = items.size

    /** Adds at the end and returns [item]. */
    fun <E : T> add(item: E): E = insertAt(Int.MAX_VALUE, item)

    /**
     * JPopupMenu.insert-like: inserts at [index] and returns [item]. A negative [index] throws IllegalArgumentException,
     * and one at or beyond the number of items inserts at the end (as in Swing).
     */
    fun <E : T> insert(item: E, index: Int): E {
        requireNonNegative(index)
        return insertAt(index, item)
    }

    private fun <E : T> insertAt(index: Int, item: E): E {
        ownerOf(item)?.remove(item)
        val position = minOf(index, items.size)
        val native = nativeOf(item).ptr
        if (position == items.size) {
            vector().call(FoundationInterop.IVector_Append, native)
        } else {
            vector().call(FoundationInterop.IVector_InsertAt, position, native)
        }
        items.add(position, item)
        setOwner(item, this)
        return item
    }

    /** Removes an item. Does nothing if it is not an item of this container (as in Swing). */
    fun remove(item: T) {
        val index = items.indexOfFirst { it === item }
        if (index >= 0) removeAt(index)
    }

    /** JPopupMenu.remove(int)-like: removes the item at [index]. Out of range throws IllegalArgumentException (as in Swing). */
    fun removeAt(index: Int) {
        requireNonNegative(index)
        require(index < items.size) { "index greater than the number of items. (index: $index, number of items: ${items.size})" }
        vector().call(FoundationInterop.IVector_RemoveAt, index)
        setOwner(items.removeAt(index), null)
    }

    /**
     * Removes all items one by one from the end (IVector.RemoveAt), as with Swing's Container.removeAll. IVector.Clear is not used because
     * when MenuBarItem copies Items changes to its open menu after the template is applied, it copies only inserts and removals and ignores Reset (Clear)
     * (OnItemsVectorChanged in controls/dev/MenuBar/MenuBarItem.cpp).
     */
    fun clear() {
        while (items.isNotEmpty()) removeAt(items.size - 1)
    }

    /** Container.getComponent-like: the item at [index]. Out of range throws ArrayIndexOutOfBoundsException (as in Swing). */
    operator fun get(index: Int): T = items.getOrNull(index) ?: throw ArrayIndexOutOfBoundsException("No item at index: $index")

    /** Checks a position the JMenu.getItem way: negative throws IllegalArgumentException, at or beyond the number of items throws ArrayIndexOutOfBoundsException. */
    fun getChecked(index: Int): T {
        requireNonNegative(index)
        return get(index)
    }

    /** Container.getComponentIndex-like: the position of [item]. -1 if it is not an item of this container. */
    fun indexOf(item: T?): Int = items.indexOfFirst { it === item }

    private fun requireNonNegative(index: Int) {
        require(index >= 0) { "index less than zero. (index: $index)" }
    }

    companion object {
        /** A container of menu items (MenuFlyoutItemBase). */
        fun forMenuItems(vector: () -> ComPtr): MenuItems<WMenuFlyoutItemBase> = MenuItems(
            vector,
            { it.menuFlyoutItemBase },
            { it.menuOwner },
            { item, owner -> item.menuOwner = owner },
        )
    }
}
