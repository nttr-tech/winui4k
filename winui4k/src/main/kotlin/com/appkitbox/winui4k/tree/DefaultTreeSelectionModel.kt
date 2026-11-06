package com.appkitbox.winui4k.tree

import java.util.BitSet
import java.util.concurrent.CopyOnWriteArrayList

/**
 * javax.swing.tree.DefaultTreeSelectionModel-like: the standard implementation of [TreeSelectionModel].
 *
 * It works by the same rules as Swing.
 * - The default mode is [TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION] (the selection model WTree creates itself is SINGLE). Out-of-range values become DISCONTIGUOUS.
 * - In SINGLE, both [setSelectionPaths] and [addSelectionPaths] select only the first path. In CONTIGUOUS, if the rows are not contiguous only the first path is selected
 *   (not checked without a [RowMapper]). In CONTIGUOUS, if removing makes it non-contiguous, the whole selection is cleared.
 * - The lead is the last added path. On removal, it is the last remaining path.
 * - Only when something changed, the changed paths are reported in one event (added paths first, removed paths after).
 * - Listeners are called starting from the last one added (same as Swing's EventListenerList).
 *
 * Swing's PropertyChangeListener and the internal ListSelectionModel that remembers row selection are not ported (rows are computed with [RowMapper] every time).
 */
open class DefaultTreeSelectionModel : TreeSelectionModel {
    private var selection: MutableList<TreePath> = mutableListOf()

    private var leadPath: TreePath? = null

    private var selectionMode: Int = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION

    private var rowMapper: RowMapper? = null

    private val listeners = CopyOnWriteArrayList<TreeSelectionListener>()

    /** Called when the selection mode changes (used by WTree to match the TreeView selection mode). */
    internal val modeObservers = CopyOnWriteArrayList<() -> Unit>()

    override fun setSelectionMode(mode: Int) {
        val old = selectionMode
        selectionMode = when (mode) {
            TreeSelectionModel.SINGLE_TREE_SELECTION, TreeSelectionModel.CONTIGUOUS_TREE_SELECTION -> mode
            else -> TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
        }
        if (old != selectionMode) modeObservers.forEach { it() }
    }

    override fun getSelectionMode(): Int = selectionMode

    override fun setSelectionPath(path: TreePath?) {
        setSelectionPaths(path?.let { arrayOf(it) })
    }

    override fun setSelectionPaths(paths: Array<out TreePath?>?) {
        var requested: List<TreePath> = paths?.filterNotNull().orEmpty()
        if (requested.isEmpty() && selection.isEmpty()) return
        if (requested.size > 1 && !isAllowedAsSelection(requested)) requested = listOf(requested[0])
        val oldLead = leadPath
        val newSelection = requested.distinct()
        val changes = newSelection.filter { it !in selection }.map { it to true } +
            selection.filter { it !in newSelection }.map { it to false }
        selection = newSelection.toMutableList()
        leadPath = newSelection.lastOrNull()
        if (changes.isNotEmpty()) notifyPathChange(changes, oldLead)
    }

    override fun addSelectionPath(path: TreePath?) {
        addSelectionPaths(path?.let { arrayOf(it) })
    }

    override fun addSelectionPaths(paths: Array<out TreePath?>?) {
        val requested = paths?.filterNotNull().orEmpty()
        if (paths.isNullOrEmpty()) return
        when {
            isSingle() -> setSelectionPaths(paths)
            isContiguous() && !canPathsBeAdded(requested) ->
                setSelectionPaths(if (arePathsContiguous(requested)) paths else arrayOf(paths[0]))
            else -> {
                val added = requested.distinct().filter { it !in selection }
                if (added.isEmpty()) return
                val oldLead = leadPath
                selection.addAll(added)
                leadPath = requested.last()
                notifyPathChange(added.map { it to true }, oldLead)
            }
        }
    }

    override fun removeSelectionPath(path: TreePath?) {
        removeSelectionPaths(path?.let { arrayOf(it) })
    }

    override fun removeSelectionPaths(paths: Array<out TreePath?>?) {
        if (paths.isNullOrEmpty() || selection.isEmpty()) return
        val requested = paths.filterNotNull()
        if (!canPathsBeRemoved(requested)) {
            clearSelection()
            return
        }
        val removed = requested.asReversed().distinct().filter { it in selection }
        if (removed.isEmpty()) return
        val oldLead = leadPath
        selection.removeAll(removed)
        leadPath = selection.lastOrNull()
        notifyPathChange(removed.map { it to false }, oldLead)
    }

    override fun getSelectionPath(): TreePath? = selection.firstOrNull()

    /** A copy of the selected paths. An empty array if nothing is selected (as in Swing; JTree's getSelectionPaths is what returns null). */
    override fun getSelectionPaths(): Array<TreePath>? = selection.toTypedArray()

    override fun getSelectionCount(): Int = selection.size

    override fun isPathSelected(path: TreePath?): Boolean = path != null && path in selection

    override fun isSelectionEmpty(): Boolean = selection.isEmpty()

    override fun clearSelection() {
        if (selection.isEmpty()) return
        val event = TreeSelectionEvent(this, selection.toTypedArray(), BooleanArray(selection.size), leadPath, null)
        selection = mutableListOf()
        leadPath = null
        fireValueChanged(event)
    }

    override fun setRowMapper(newMapper: RowMapper?) {
        rowMapper = newMapper
    }

    override fun getRowMapper(): RowMapper? = rowMapper

    /** The rows of the selected paths (excluding invisible ones). null if none are visible; an empty array if nothing is selected or there is no [RowMapper] (as in Swing). */
    override fun getSelectionRows(): IntArray? {
        val mapper = rowMapper ?: return IntArray(0)
        if (selection.isEmpty()) return IntArray(0)
        val rows = mapper.getRowsForPaths(selection.toTypedArray()) ?: return null
        val visible = rows.filter { it != -1 }
        return if (visible.isEmpty()) null else visible.toIntArray()
    }

    override fun getMinSelectionRow(): Int = getSelectionRows()?.minOrNull() ?: -1

    override fun getMaxSelectionRow(): Int = getSelectionRows()?.maxOrNull() ?: -1

    override fun isRowSelected(row: Int): Boolean = getSelectionRows()?.contains(row) == true

    /** Does nothing, since rows are not remembered (they are computed with [RowMapper] every time). */
    override fun resetRowSelection() = Unit

    override fun getLeadSelectionRow(): Int = leadPath?.let { rowOf(it) } ?: -1

    override fun getLeadSelectionPath(): TreePath? = leadPath

    override fun addTreeSelectionListener(x: TreeSelectionListener) {
        listeners += x
    }

    override fun removeTreeSelectionListener(x: TreeSelectionListener) {
        listeners.lastIndexOf(x).takeIf { it >= 0 }?.let { listeners.removeAt(it) }
    }

    /** The added listeners (in the order added). */
    fun getTreeSelectionListeners(): Array<TreeSelectionListener> = listeners.toTypedArray()

    /** Reports the changed paths and whether each was added (the second of [changes]) in one event. */
    protected open fun notifyPathChange(changes: List<Pair<TreePath, Boolean>>, oldLeadSelection: TreePath?) {
        val event = TreeSelectionEvent(
            this,
            changes.map { it.first }.toTypedArray(),
            changes.map { it.second }.toBooleanArray(),
            oldLeadSelection,
            leadPath,
        )
        fireValueChanged(event)
    }

    /** Notifies the listeners (starting from the last one added). */
    protected open fun fireValueChanged(e: TreeSelectionEvent) {
        for (listener in listeners.reversed()) listener.valueChanged(e)
    }

    /** true if the rows of [paths] are contiguous. true if there is no [RowMapper] or fewer than two paths; false if any path is invisible. */
    protected open fun arePathsContiguous(paths: List<TreePath>): Boolean {
        if (rowMapper == null || paths.size < 2) return true
        val rows = paths.map { rowOf(it) ?: return false }
        if (rows.any { it == -1 }) return false
        return isContiguous(rows)
    }

    /** true if adding [paths] in CONTIGUOUS keeps the selection contiguous. */
    protected open fun canPathsBeAdded(paths: List<TreePath>): Boolean {
        if (paths.isEmpty() || selection.isEmpty() || !isContiguousWithMapper()) return true
        val rows = (selection + paths).map { rowOf(it) ?: return false }
        if (rows.any { it == -1 }) return false
        return isContiguous(rows)
    }

    /** true if removing [paths] in CONTIGUOUS keeps the selection contiguous (invisible paths are not counted). */
    protected open fun canPathsBeRemoved(paths: List<TreePath>): Boolean {
        if (rowMapper == null || selection.isEmpty() || !isContiguous()) return true
        val rows = selection.filter { it !in paths }.mapNotNull { rowOf(it) }.filter { it != -1 }
        return rows.distinct().size <= 1 || isContiguous(rows)
    }

    private fun rowOf(path: TreePath): Int? = rowMapper?.getRowsForPaths(arrayOf(path))?.firstOrNull()

    /** Whether two or more [paths] can be selected as is (not in SINGLE; in CONTIGUOUS, if the rows are contiguous). */
    private fun isAllowedAsSelection(paths: List<TreePath>): Boolean = !isSingle() && (!isContiguous() || arePathsContiguous(paths))

    private fun isContiguousWithMapper(): Boolean = rowMapper != null && isContiguous()

    private fun isSingle(): Boolean = selectionMode == TreeSelectionModel.SINGLE_TREE_SELECTION

    private fun isContiguous(): Boolean = selectionMode == TreeSelectionModel.CONTIGUOUS_TREE_SELECTION

    private fun isContiguous(rows: List<Int>): Boolean {
        val bits = BitSet()
        rows.forEach(bits::set)
        val min = rows.min()
        return (min..rows.max()).all { bits[it] }
    }

    override fun toString(): String = buildString {
        append(javaClass.name).append(' ').append(Integer.toHexString(hashCode())).append(' ')
        append(selection.joinToString(prefix = "[ ", postfix = " ]", separator = ", "))
    }
}
