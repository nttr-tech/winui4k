package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The synchronization of a [WListModel] and ItemsControl.Items shared by [WList] (ListView) and [WListBox] (ListBox).
 *
 * For each model element, the toString() string (an empty string if null, same as Swing's DefaultListCellRenderer) is
 * newly boxed and put into Items, and model changes ([WListDataListener]) are mapped directly to InsertAt / RemoveAt / SetAt on Items.
 * Each boxed object is a separate COM object per element, so selected items (SelectedItems) can be mapped back
 * to indexes by identity (the IUnknown address). Comparing strings could not distinguish elements with the same string.
 *
 * Through this object the model's listener strongly references the list wrapper ([owner]). As with Swing's JList,
 * the list stays alive while the model does (the Items pointer is owned by the wrapper, so it would become unusable if only the wrapper were collected).
 * The native side does not reference the model, so there is no cycle across the native boundary.
 */
internal class ListModelItems(
    /** The list wrapper. Held so that it is not collected while the model's listener touches Items. */
    @Suppress("unused") private val owner: WComponent,
    /** The IVector<Object> view of ItemsControl.Items (owned by [owner]). */
    private val items: ComPtr,
    model: WListModel<*>,
    /** The IVector<Object> of the selected items (ListViewBase / ListBox SelectedItems; the caller Releases it). */
    private val selectedItems: () -> ComPtr,
    /** ISelector (owned by [owner]). Used to reselect in single selection mode. */
    private val selector: ComPtr,
    /** Whether it is in single selection mode (a mode where SelectedItems cannot be edited). */
    private val isSingleSelection: () -> Boolean,
) {
    /** The identities (IUnknown addresses) of the items put into Items. In the same order as Items. */
    private val identities = ArrayList<Long>()

    private val listener = object : WListDataListener {
        override fun intervalAdded(e: WListDataEvent) {
            // If the range disagrees with Items (the model implementation does not report ranges correctly), rebuild everything
            if (e.index0 !in 0..identities.size || identities.size + e.index1 - e.index0 + 1 != this@ListModelItems.model.getSize()) {
                reload()
                return
            }
            for (index in e.index0..e.index1) insert(index)
        }

        override fun intervalRemoved(e: WListDataEvent) {
            val count = e.index1 - e.index0 + 1
            if (e.index0 < 0 || e.index1 >= identities.size || identities.size - count != this@ListModelItems.model.getSize()) {
                reload()
                return
            }
            repeat(count) {
                items.call(FoundationInterop.IVector_RemoveAt, e.index0)
                identities.removeAt(e.index0)
            }
        }

        override fun contentsChanged(e: WListDataEvent) {
            val size = this@ListModelItems.model.getSize()
            if (e.index0 < 0 || e.index1 >= identities.size || identities.size != size) {
                reload()
                return
            }
            // WinUI deselects replaced items, but Swing keeps the selection at that position, so select them again
            val selected = selectedIndices().filter { it in e.index0..e.index1 }
            for (index in e.index0..e.index1) replace(index)
            if (selected.isNotEmpty()) reselect(selected)
        }
    }

    /** The model of the elements shown. Replacing it rebuilds Items (the selection is cleared; same as Swing's JList.setModel). */
    var model: WListModel<*> = model
        set(value) {
            if (field === value) return
            field.removeListDataListener(listener)
            field = value
            value.addListDataListener(listener)
            reload()
        }

    init {
        model.addListDataListener(listener)
        reload()
    }

    /** The number of elements. */
    val size: Int
        get() = model.getSize()

    /** The element at [index]. */
    fun valueAt(index: Int): Any? = model.getElementAt(index)

    /** The string that shows the element at [index] (an empty string if null). */
    fun textAt(index: Int): String = valueAt(index)?.toString() ?: ""

    /**
     * The model as the default model ([WDefaultListModel]). The addItem family of WList / WListBox edits it.
     * @throws UnsupportedOperationException when the model is not a [WDefaultListModel] (an app-specific read-only model and so on)
     */
    fun editableModel(): WDefaultListModel<Any?> {
        @Suppress("UNCHECKED_CAST") // Puts a String in regardless of the element type (addItem takes strings)
        return model as? WDefaultListModel<Any?>
            ?: throw UnsupportedOperationException("the list model is not a WDefaultListModel: ${model.javaClass.name}")
    }

    /**
     * Converts the IVector<Object> of selected items ([selected]; borrowed) to model indexes and returns them in ascending order.
     * Items not in Items (in the middle of synchronization and so on) are excluded.
     */
    fun indicesOf(selected: ComPtr): IntArray {
        val count = selected.getInt(FoundationInterop.IVector_get_Size)
        val result = ArrayList<Int>(count)
        for (position in 0 until count) {
            val item = selected.getPtrOrNull(FoundationInterop.IVector_GetAt, position) ?: continue
            val index = try {
                identities.indexOf(identityOf(item))
            } finally {
                item.release()
            }
            if (index >= 0) result += index
        }
        return result.sorted().toIntArray()
    }

    /** The strings put into Items (for tests to check the native state). */
    fun nativeTexts(): List<String> = (0 until items.getInt(FoundationInterop.IVector_get_Size)).map { index ->
        val boxed = items.getPtr(FoundationInterop.IVector_GetAt, index)
        try {
            PropertyValues.unboxString(boxed) ?: ""
        } finally {
            boxed.release()
        }
    }

    /** The model indexes of the selected items (ascending). */
    fun selectedIndices(): IntArray {
        val selected = selectedItems()
        return try {
            indicesOf(selected)
        } finally {
            selected.release()
        }
    }

    /**
     * Adds the items at [indices] to the selection. In single selection mode SelectedItems.Append fails with E_UNEXPECTED (0x8000FFFF),
     * so SelectedIndex is set; in multiple selection modes they are added with SelectedItems.Append.
     */
    private fun reselect(indices: List<Int>) {
        if (isSingleSelection()) {
            selector.call(XamlInterop.ISelector_put_SelectedIndex, indices.first())
            return
        }
        val selected = selectedItems()
        try {
            for (index in indices) {
                val item = items.getPtr(FoundationInterop.IVector_GetAt, index)
                try {
                    selected.call(FoundationInterop.IVector_Append, item.ptr)
                } finally {
                    item.release()
                }
            }
        } finally {
            selected.release()
        }
    }

    /** Rebuilds Items from all elements of the model. */
    private fun reload() {
        items.call(FoundationInterop.IVector_Clear)
        identities.clear()
        for (index in 0 until model.getSize()) insert(index)
    }

    private fun insert(index: Int) {
        val boxed = PropertyValues.boxString(textAt(index))
        try {
            items.call(FoundationInterop.IVector_InsertAt, index, boxed.ptr)
            identities.add(index, identityOf(boxed))
        } finally {
            boxed.release()
        }
    }

    private fun replace(index: Int) {
        val boxed = PropertyValues.boxString(textAt(index))
        try {
            items.call(FoundationInterop.IVector_SetAt, index, boxed.ptr)
            identities[index] = identityOf(boxed)
        } finally {
            boxed.release()
        }
    }

    private companion object {
        /** COM identity (the address from QueryInterface(IUnknown)). */
        fun identityOf(pointer: ComPtr): Long {
            val unknown = pointer.queryInterface(KComObject.IID_IUNKNOWN)
            return try {
                unknown.ptr.address
            } finally {
                unknown.release()
            }
        }
    }
}

/** A [WDefaultListModel] holding [values] in order (used by the array and list constructors of WList / WListBox and by setListData). */
internal fun defaultListModelOf(values: Collection<*>): WDefaultListModel<Any?> = WDefaultListModel<Any?>().also { it.addAll(values) }
