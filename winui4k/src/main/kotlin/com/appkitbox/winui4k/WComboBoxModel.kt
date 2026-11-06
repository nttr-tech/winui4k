package com.appkitbox.winui4k

/**
 * ComboBoxModel-like: a model that adds the selected item [selectedItem] to the item list ([WListModel]) of a [WComboBox].
 * The selected item may be a value not in the list (such as a string typed into an editable combo). When the selection changes, listeners are notified
 * with `contentsChanged` (index0 = index1 = -1), as in Swing.
 */
interface WComboBoxModel<E> : WListModel<E> {
    /** The selected item (null if unselected). From Java, `getSelectedItem()` / `setSelectedItem(Object)`. */
    var selectedItem: Any?
}

/** MutableComboBoxModel-like: a [WComboBoxModel] whose items can be added and removed. [WComboBox.addItem] etc. call these methods. */
interface WMutableComboBoxModel<E> : WComboBoxModel<E> {
    /** Adds [item] at the end. */
    fun addElement(item: E)

    /** Removes the first item equal to [obj] (does nothing if there is none). */
    fun removeElement(obj: Any?)

    /** Inserts [item] at [index]. */
    fun insertElementAt(item: E, index: Int)

    /** Removes the item at [index]. */
    fun removeElementAt(index: Int)
}
