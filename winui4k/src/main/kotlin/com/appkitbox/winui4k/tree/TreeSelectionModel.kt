package com.appkitbox.winui4k.tree

/**
 * javax.swing.tree.TreeSelectionModel-like: the selection state of a tree. The standard implementation is [DefaultTreeSelectionModel].
 * It does not have Swing's PropertyChangeListener (java.beans).
 *
 * Selection modes in [com.appkitbox.winui4k.WTree]: [SINGLE_TREE_SELECTION] becomes TreeView's SINGLE, and
 * [CONTIGUOUS_TREE_SELECTION] and [DISCONTIGUOUS_TREE_SELECTION] become MULTIPLE (a check box on each node),
 * because WinUI has no multiple selection mode with Ctrl / Shift.
 */
interface TreeSelectionModel {
    /** Sets the selection mode ([SINGLE_TREE_SELECTION] etc.). */
    fun setSelectionMode(mode: Int)

    /** The selection mode. */
    fun getSelectionMode(): Int

    /** Selects only [path] (clears the selection if null). */
    fun setSelectionPath(path: TreePath?)

    /** Selects only [paths] (clears the selection if null or empty). */
    fun setSelectionPaths(paths: Array<out TreePath?>?)

    /** Adds [path] to the selection. */
    fun addSelectionPath(path: TreePath?)

    /** Adds [paths] to the selection. */
    fun addSelectionPaths(paths: Array<out TreePath?>?)

    /** Removes [path] from the selection. */
    fun removeSelectionPath(path: TreePath?)

    /** Removes [paths] from the selection. */
    fun removeSelectionPaths(paths: Array<out TreePath?>?)

    /** The first selected path. null if none. */
    fun getSelectionPath(): TreePath?

    /** The selected paths (a copy). */
    fun getSelectionPaths(): Array<TreePath>?

    /** The number of selected paths. */
    fun getSelectionCount(): Int

    /** true if [path] is selected. */
    fun isPathSelected(path: TreePath?): Boolean

    /** true if nothing is selected. */
    fun isSelectionEmpty(): Boolean

    /** Clears the whole selection. */
    fun clearSelection()

    /** Converts paths to rows (used for the row APIs and to check contiguous selection). */
    fun setRowMapper(newMapper: RowMapper?)

    /** The one set with [setRowMapper]. */
    fun getRowMapper(): RowMapper?

    /** The rows of the selected paths (excluding invisible ones). */
    fun getSelectionRows(): IntArray?

    /** The minimum row of the selected paths. -1 if none. */
    fun getMinSelectionRow(): Int

    /** The maximum row of the selected paths. -1 if none. */
    fun getMaxSelectionRow(): Int

    /** true if row [row] is selected. */
    fun isRowSelected(row: Int): Boolean

    /** Called when the order of rows changes (implementations that remember rows recompute them). */
    fun resetRowSelection()

    /** The row of the lead (last added) path. -1 if none. */
    fun getLeadSelectionRow(): Int

    /** The lead (last added) path. null if none. */
    fun getLeadSelectionPath(): TreePath?

    /** Adds a listener that receives selection changes. */
    fun addTreeSelectionListener(x: TreeSelectionListener)

    /** Removes a listener. */
    fun removeTreeSelectionListener(x: TreeSelectionListener)

    companion object {
        /** Only one path can be selected. */
        const val SINGLE_TREE_SELECTION: Int = 1

        /** Paths of contiguous rows can be selected (same as [DISCONTIGUOUS_TREE_SELECTION] without a path-to-row converter ([RowMapper])). */
        const val CONTIGUOUS_TREE_SELECTION: Int = 2

        /** Any paths can be selected. */
        const val DISCONTIGUOUS_TREE_SELECTION: Int = 4
    }
}
