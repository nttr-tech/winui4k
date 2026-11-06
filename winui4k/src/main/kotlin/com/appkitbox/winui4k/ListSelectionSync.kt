package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import java.util.concurrent.CopyOnWriteArrayList

/** The point that maps the native selection modes of [WList] (ListView) and [WListBox] (ListBox) to Swing selection modes. */
internal interface NativeListSelectionModes {
    /** Whether the mode cannot edit SelectedItems (selects with SelectedIndex): single selection and the no-selection mode. */
    fun isSingle(): Boolean

    /** Whether the mode does not allow selection (ListView's NONE). */
    fun isNone(): Boolean

    /** The Swing selection mode corresponding to the current native mode (SINGLE for single selection and no selection, otherwise MULTIPLE_INTERVAL). */
    fun swingMode(): Int

    /** Sets the native mode to single selection (if [single] is true) or to Ctrl / Shift multiple selection (Extended). */
    fun setNative(single: Boolean)
}

/**
 * A gate that holds the Runnable listeners notifying selection changes (addListSelectionListener of WList / WListBox)
 * only during one operation and calls each once at the end.
 *
 * The Swing-style selection API may add and remove native selections item by item, and SelectionChanged arrives each time.
 * The native handlers capture only this gate (not the wrapper).
 */
internal class SelectionChangeGate {
    private var depth = 0

    /** Notifications received while held. Called once per registration (handler), in the order received. */
    private val pending = LinkedHashMap<Any, Runnable>()

    /** Delivers a notification to the listener [listener] of registration [key] (deferred while held). */
    fun deliver(key: Any, listener: Runnable) {
        if (depth > 0) pending.putIfAbsent(key, listener) else listener.run()
    }

    /** Holds notifications only during [block], and calls the deferred notifications when it ends. Can be nested. */
    fun <R> batch(block: () -> R): R {
        depth++
        try {
            return block()
        } finally {
            if (--depth == 0 && pending.isNotEmpty()) {
                val listeners = pending.values.toList()
                pending.clear()
                listeners.forEach { it.run() }
            }
        }
    }
}

/**
 * Two-way sync between the native selection of [WList] / [WListBox] and a [WListSelectionModel] (the core of the Swing-style selection API).
 *
 * - Native → model: on SelectionChanged, copies the selected items (mapped back to indices by identity) to the model.
 *   It is also copied first when a Swing-style API is called, so the latest selection is visible when read from inside any handler.
 * - Model → native: copies model changes (including when the user changes the model directly) to native with SelectedIndex (single selection) or
 *   by adding and removing SelectedItems (multiple selection).
 * - On item additions and removals, shifts the selection with the model's insertIndexInterval / removeIndexInterval, as in Swing's JList,
 *   and copies the result to native.
 *
 * The native handlers capture only the [ListenerAnchor] and look up the wrapper on every call (no cycle).
 * A wrapper that started syncing is not collected while shown (and while being assembled), the same as when listeners are added.
 */
internal class ListSelectionSync<T : WComponent>(
    /** The list wrapper. Copies model changes to native and is the source of events to listeners. */
    private val owner: T,
    private val items: ListModelItems,
    /** ISelector (owned by [owner]). */
    private val selector: ComPtr,
    private val modes: NativeListSelectionModes,
    private val gate: SelectionChangeGate,
    /** A function that looks up this sync from the wrapper (must not capture the wrapper). */
    resolve: (T) -> ListSelectionSync<T>,
) {
    /** Whether a sync is in progress (so as not to copy back changes it caused itself). */
    private var syncing = false

    /** The native selection at the last sync (not copied to the model again if unchanged). */
    private var lastNative = IntArray(0)

    private val listeners = CopyOnWriteArrayList<WListSelectionListener>()

    private val modelListener = WListSelectionListener { e ->
        if (!syncing) push()
        val event = WListSelectionEvent(owner, e.firstIndex, e.lastIndex, e.valueIsAdjusting)
        listeners.forEach { it.valueChanged(event) }
    }

    /** The selection model. The default is a [WDefaultListSelectionModel] built from the native mode and selection. */
    var model: WListSelectionModel = WDefaultListSelectionModel().also { it.setSelectionMode(modes.swingMode()) }
        set(value) {
            if (field === value) return
            field.removeListSelectionListener(modelListener)
            field = value
            value.addListSelectionListener(modelListener)
            val single = value.getSelectionMode() == WListSelectionModel.SINGLE_SELECTION
            if (!modes.isNone() && single != modes.isSingle()) guarded { modes.setNative(single) }
            gate.batch { push() }
        }

    init {
        lastNative = items.selectedIndices()
        updateModelTo(lastNative)
        model.addListSelectionListener(modelListener)
        items.selectionSync = this
        val anchor = ListenerAnchor(owner).also { it.listenerAdded() }
        // Let the handlers capture only the anchor, not the wrapper (no cycle)
        selector.addEventHandler(
            "WinUI4K.ListSelectionSyncHandler",
            XamlInterop.IID_SelectionChangedEventHandler,
            XamlInterop.ISelector_add_SelectionChanged,
        ) { _, _ -> anchor.get()?.let(resolve)?.pullFromNative() }
    }

    /** One call of a Swing-style API. Copies the native selection first, and collapses the notifications to Runnable listeners into one at the end. */
    fun <R> call(block: (WListSelectionModel) -> R): R = gate.batch {
        pullFromNative()
        block(model)
    }

    /** The selected indices (within the element range, ascending). Copies the native selection first. */
    fun selectedIndices(): IntArray {
        pullFromNative()
        return wantedIndices()
    }

    fun addListener(listener: WListSelectionListener) {
        listeners += listener
    }

    fun removeListener(listener: WListSelectionListener) {
        val index = listeners.lastIndexOf(listener)
        if (index >= 0) listeners.removeAt(index)
    }

    fun listeners(): Array<WListSelectionListener> = listeners.toTypedArray()

    /**
     * Sets the Swing selection mode [mode] (decided from the current native mode if null) on the model.
     * [writeNative] changes the native mode, and a selection that does not fit a narrower mode is shrunk, as in Swing since JDK 9,
     * to the frontmost row (single selection) or the range continuing from the frontmost (single interval).
     */
    fun changeMode(mode: Int?, writeNative: () -> Unit) = gate.batch {
        guarded {
            writeNative()
            lastNative = items.selectedIndices()
            // In a mode that does not allow selection the native side clears the selection, so match the model too (so the previous selection is not reselected when switching back)
            if (modes.isNone()) updateModelTo(lastNative)
            model.setSelectionMode(mode ?: modes.swingMode())
            coerceToMode()
        }
        push()
    }

    /** Same as Swing's JList.setSelectedValue. The selection is not changed if not found. */
    fun setSelectedValue(value: Any?, scroll: (Int) -> Unit) = call { model ->
        if (value == null) {
            model.clearSelection()
            return@call
        }
        val min = model.getMinSelectionIndex()
        if (min in 0 until items.size && value == items.valueAt(min)) return@call
        val index = (0 until items.size).firstOrNull { value == items.valueAt(it) } ?: return@call
        model.setSelectionInterval(index, index)
        scroll(index)
    }

    /** Same as Swing's JList.setSelectedIndices (out-of-range rows are ignored). */
    fun setSelectedIndices(indices: IntArray) = call { model ->
        model.clearSelection()
        for (index in indices) if (index < items.size) model.addSelectionInterval(index, index)
    }

    /** Copies the native selection to the model if it changed since the last sync (called from the SelectionChanged handler and Swing-style APIs). */
    fun pullFromNative() {
        if (syncing) return
        val native = items.selectedIndices()
        if (native.contentEquals(lastNative)) return
        lastNative = native
        guarded { updateModelTo(native) }
        // For a selection that does not fit the selection mode (such as a disjoint item added with Ctrl to a single interval) or a custom model's decision, match native to it
        if (!native.contentEquals(wantedIndices())) gate.batch { push() }
    }

    /** Called when [ListModelItems] changes Items with [edit]. Adjusts the selection model as in Swing's JList and copies it to native. */
    fun onStructureChange(kind: ListStructureChange, index0: Int, index1: Int, edit: () -> Unit) {
        guarded {
            edit()
            when (kind) {
                ListStructureChange.ADDED -> model.insertIndexInterval(index0, index1 - index0 + 1, true)
                ListStructureChange.REMOVED -> model.removeIndexInterval(index0, index1)
                ListStructureChange.REPLACED -> model.clearSelection()
                ListStructureChange.CHANGED, ListStructureChange.RELOADED -> Unit
            }
        }
        gate.batch { push() }
    }

    /** Copies the model's selection (within the element range) to native. Not copied in a mode that does not allow selection. */
    private fun push() = guarded {
        val wanted = wantedIndices()
        val current = items.selectedIndices()
        if (!modes.isNone() && !current.contentEquals(wanted)) {
            if (modes.isSingle()) {
                val lead = model.getLeadSelectionIndex()
                val index = when {
                    wanted.isEmpty() -> -1
                    lead in wanted -> lead
                    else -> wanted.first()
                }
                selector.call(XamlInterop.ISelector_put_SelectedIndex, index)
            } else {
                items.applySelection(wanted.asList())
            }
        }
        lastNative = items.selectedIndices()
    }

    private fun wantedIndices(): IntArray {
        val size = items.size
        return model.getSelectedIndices().filter { it in 0 until size }.toIntArray()
    }

    /**
     * Sets the model's selection to [target] (ascending). Copies one native change in as few operations (one event) as possible, and chooses
     * so that anchor / lead come close to the result of the same operation in Swing (Ctrl clicks add or remove, Shift clicks extend from the anchor).
     */
    private fun updateModelTo(target: IntArray) {
        val current = model.getSelectedIndices()
        if (current.contentEquals(target)) return
        if (target.isEmpty()) {
            model.clearSelection()
            return
        }
        val whole = rangeOf(target.asList())
        if (whole != null && Xaml.currentModifiers() and Xaml.MODIFIER_SHIFT != 0) {
            // A Shift click is the range extended from the anchor
            if (model.getAnchorSelectionIndex() == whole.last) {
                model.setSelectionInterval(whole.last, whole.first)
            } else {
                model.setSelectionInterval(whole.first, whole.last)
            }
            return
        }
        val added = rangeOf(target.filter { it !in current })
        val removed = rangeOf(current.filter { it !in target })
        val onlyAdded = target.size - current.size == added?.count()
        val onlyRemoved = current.size - target.size == removed?.count()
        when {
            onlyRemoved && removed != null -> model.removeSelectionInterval(removed.first, removed.last)
            onlyAdded && added != null -> model.addSelectionInterval(added.first, added.last)
            whole != null -> model.setSelectionInterval(whole.first, whole.last)
            else -> {
                model.setValueIsAdjusting(true)
                model.clearSelection()
                rangesOf(target).forEach { model.addSelectionInterval(it.first, it.last) }
                model.setValueIsAdjusting(false)
            }
        }
    }

    private fun coerceToMode() {
        val selected = model.getSelectedIndices()
        if (selected.size <= 1) return
        val min = selected.first()
        when (model.getSelectionMode()) {
            WListSelectionModel.SINGLE_SELECTION -> model.setSelectionInterval(min, min)
            WListSelectionModel.SINGLE_INTERVAL_SELECTION -> if (rangeOf(selected.asList()) == null) {
                var end = min
                while (model.isSelectedIndex(end + 1)) end++
                model.setSelectionInterval(min, end)
            }
        }
    }

    private inline fun <R> guarded(block: () -> R): R {
        val previous = syncing
        syncing = true
        try {
            return block()
        } finally {
            syncing = previous
        }
    }

    private companion object {
        /** The range if the ascending [indices] form one contiguous range; null if empty or disjoint. */
        fun rangeOf(indices: List<Int>): IntRange? =
            if (indices.isNotEmpty() && indices.last() - indices.first() == indices.size - 1) indices.first()..indices.last() else null

        /** Splits the ascending [indices] into contiguous ranges. */
        fun rangesOf(indices: IntArray): List<IntRange> {
            val ranges = ArrayList<IntRange>()
            var start = indices.first()
            var previous = start
            for (index in indices.drop(1)) {
                if (index != previous + 1) {
                    ranges += start..previous
                    start = index
                }
                previous = index
            }
            ranges += start..previous
            return ranges
        }
    }
}
