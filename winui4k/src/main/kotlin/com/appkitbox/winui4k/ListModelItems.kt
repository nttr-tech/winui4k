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

    /**
     * The sync with the selection model ([ListSelectionSync]). Set once the Swing-style selection API starts being used.
     * If set, the selection model is shifted after Items changes, as in Swing's JList.
     */
    var selectionSync: ListSelectionSync<*>? = null

    /**
     * A function that makes the item put into Items from an element (returns an owned reference). If null, the toString() string is boxed.
     * Used by the [WListBox] renderer to put ListBoxItems drawing the elements. Call [rebuildItems] after changing it.
     */
    var itemFactory: ((index: Int) -> ComPtr)? = null

    private val listener = object : WListDataListener {
        override fun intervalAdded(e: WListDataEvent) {
            // If the range disagrees with Items (the model implementation does not report ranges correctly), rebuild everything
            if (e.index0 !in 0..identities.size || identities.size + e.index1 - e.index0 + 1 != this@ListModelItems.model.getSize()) {
                change(ListStructureChange.RELOADED, e) { reload() }
                return
            }
            change(ListStructureChange.ADDED, e) { for (index in e.index0..e.index1) insert(index) }
        }

        override fun intervalRemoved(e: WListDataEvent) {
            val count = e.index1 - e.index0 + 1
            if (e.index0 < 0 || e.index1 >= identities.size || identities.size - count != this@ListModelItems.model.getSize()) {
                change(ListStructureChange.RELOADED, e) { reload() }
                return
            }
            change(ListStructureChange.REMOVED, e) {
                repeat(count) {
                    items.call(FoundationInterop.IVector_RemoveAt, e.index0)
                    identities.removeAt(e.index0)
                }
            }
        }

        override fun contentsChanged(e: WListDataEvent) {
            val size = this@ListModelItems.model.getSize()
            if (identities.size != size) {
                change(ListStructureChange.RELOADED, e) { reload() }
                return
            }
            if (e.index0 < 0 || e.index1 >= size) {
                // A change that does not report its range (such as the (-1, -1) of a DefaultComboBoxModel selection change) is treated as replacing everything
                // if the size is the same (the selection is kept, as with Swing's JList). Items whose text does not change are not replaced (no selection notification either)
                val changed = (0 until size).filter { itemFactory != null || nativeTextAt(it) != textAt(it) }
                if (changed.isNotEmpty()) change(ListStructureChange.CHANGED, WListDataEvent(e.source, e.type, 0, size - 1)) { replaceKeepingSelection(changed) }
                return
            }
            change(ListStructureChange.CHANGED, e) { replaceKeepingSelection((e.index0..e.index1).toList()) }
        }
    }

    /** Replaces the items at [indices]. WinUI deselects replaced items, but Swing keeps the selection at that position, so they are selected again. */
    private fun replaceKeepingSelection(indices: List<Int>) {
        val selected = selectedIndices().filter { it in indices }
        for (index in indices) replace(index)
        if (selected.isNotEmpty()) reselect(selected)
    }

    /** Changes Items with [edit], and if synced with the selection model, also adjusts it according to [kind]. */
    private fun change(kind: ListStructureChange, e: WListDataEvent, edit: () -> Unit) {
        val sync = selectionSync
        if (sync == null) edit() else sync.onStructureChange(kind, e.index0, e.index1, edit)
    }

    /** The model of the elements shown. Replacing it rebuilds Items (the selection is cleared; same as Swing's JList.setModel). */
    var model: WListModel<*> = model
        set(value) {
            if (field === value) return
            field.removeListDataListener(listener)
            field = value
            value.addListDataListener(listener)
            val sync = selectionSync
            if (sync == null) reload() else sync.onStructureChange(ListStructureChange.REPLACED, -1, -1) { reload() }
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

    /**
     * The strings put in Items (for tests to check the native state).
     * If an item is not a string (a renderer's ListBoxItem), its AutomationProperties.Name.
     */
    fun nativeTexts(): List<String> = (0 until items.getInt(FoundationInterop.IVector_get_Size)).map(::nativeTextAt)

    /** The string put at [index] in Items (AutomationProperties.Name if the item is not a string). */
    private fun nativeTextAt(index: Int): String {
        val boxed = items.getPtr(FoundationInterop.IVector_GetAt, index)
        return try {
            PropertyValues.unboxString(boxed) ?: Xaml.getAutomation(boxed, XamlInterop.IAutomationPropertiesStatics_GetName)
        } finally {
            boxed.release()
        }
    }

    /** The element strings ([textAt]) of each item of the selected items IVector<Object> ([selected]; borrowed), in the order of [selected]. */
    fun textsOf(selected: ComPtr): List<String> = (0 until selected.getInt(FoundationInterop.IVector_get_Size)).mapNotNull { position ->
        selected.getPtrOrNull(FoundationInterop.IVector_GetAt, position)?.let { item ->
            try {
                identities.indexOf(identityOf(item)).takeIf { it >= 0 }?.let { textAt(it) }
            } finally {
                item.release()
            }
        }
    }

    /** Recreates all items (the selection stays at its position, as with replacing elements). Used when [itemFactory] changes. */
    fun rebuildItems() {
        if (identities.isNotEmpty()) listener.contentsChanged(WListDataEvent(model, WListDataEvent.CONTENTS_CHANGED, 0, identities.size - 1))
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
     * In a multiple selection mode, makes the selected items exactly [wanted] (model indices).
     * Removes only the items to deselect with SelectedItems.RemoveAt, and appends only the missing items with SelectedItems.Append.
     */
    fun applySelection(wanted: Collection<Int>) {
        val selected = selectedItems()
        try {
            val missing = LinkedHashSet(wanted.sorted())
            // Removing shifts the later positions, so look from the back
            for (position in selected.getInt(FoundationInterop.IVector_get_Size) - 1 downTo 0) {
                val item = selected.getPtrOrNull(FoundationInterop.IVector_GetAt, position) ?: continue
                val index = try {
                    identities.indexOf(identityOf(item))
                } finally {
                    item.release()
                }
                if (!missing.remove(index)) selected.call(FoundationInterop.IVector_RemoveAt, position)
            }
            for (index in missing) {
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

    /** Passes the item at [index] of Items (borrowed) to [block] (for ScrollIntoView etc.). */
    fun <R> withItem(index: Int, block: (ComPtr) -> R): R {
        val item = items.getPtr(FoundationInterop.IVector_GetAt, index)
        return try {
            block(item)
        } finally {
            item.release()
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
        val boxed = newItem(index)
        try {
            items.call(FoundationInterop.IVector_InsertAt, index, boxed.ptr)
            identities.add(index, identityOf(boxed))
        } finally {
            boxed.release()
        }
    }

    private fun replace(index: Int) {
        val boxed = newItem(index)
        try {
            items.call(FoundationInterop.IVector_SetAt, index, boxed.ptr)
            identities[index] = identityOf(boxed)
        } finally {
            boxed.release()
        }
    }

    private fun newItem(index: Int): ComPtr = itemFactory?.invoke(index) ?: PropertyValues.boxString(textAt(index))

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

/** The kind of model change that [ListModelItems] copied to Items (used by [ListSelectionSync] to adjust the selection model). */
internal enum class ListStructureChange {
    /** Elements were added (intervalAdded). */
    ADDED,

    /** Elements were removed (intervalRemoved). */
    REMOVED,

    /** Elements were replaced (contentsChanged). */
    CHANGED,

    /** A change that did not report its range correctly; everything was re-read. */
    RELOADED,

    /** The model was replaced. */
    REPLACED,
}
