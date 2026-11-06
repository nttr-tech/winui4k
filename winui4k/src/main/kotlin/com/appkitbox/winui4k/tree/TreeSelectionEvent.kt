package com.appkitbox.winui4k.tree

import java.util.EventObject

/**
 * javax.swing.event.TreeSelectionEvent-like: a change of the tree selection. It has the changed paths ([getPaths]), whether each was added or removed
 * ([isAddedPath]), and the lead paths before and after the change.
 */
open class TreeSelectionEvent(
    source: Any,
    private val paths: Array<out TreePath>,
    private val areNew: BooleanArray,
    private val oldLeadSelectionPath: TreePath?,
    private val newLeadSelectionPath: TreePath?,
) : EventObject(source) {
    /** A change of a single path. */
    constructor(source: Any, path: TreePath, isNew: Boolean, oldLeadSelectionPath: TreePath?, newLeadSelectionPath: TreePath?) :
        this(source, arrayOf(path), booleanArrayOf(isNew), oldLeadSelectionPath, newLeadSelectionPath)

    /** The changed paths (returns a copy). */
    fun getPaths(): Array<TreePath> = Array(paths.size) { paths[it] }

    /** The first of the changed paths. */
    fun getPath(): TreePath = paths[0]

    /** true if the first path ([getPath]) was added to the selection, false if removed. */
    fun isAddedPath(): Boolean = areNew[0]

    /** true if [path] was added to the selection, false if removed. [IllegalArgumentException] for a path not in this change. */
    fun isAddedPath(path: TreePath?): Boolean {
        val index = paths.indexOfFirst { it == path }
        require(index >= 0) { "path is not a path identified by the TreeSelectionEvent" }
        return areNew[index]
    }

    /** true if the path at [index] was added to the selection. [IllegalArgumentException] if out of range. */
    fun isAddedPath(index: Int): Boolean {
        require(index in paths.indices) { "index is beyond range of added paths identified by TreeSelectionEvent" }
        return areNew[index]
    }

    /** The lead path before the change. */
    fun getOldLeadSelectionPath(): TreePath? = oldLeadSelectionPath

    /** The lead path after the change. */
    fun getNewLeadSelectionPath(): TreePath? = newLeadSelectionPath

    /** A copy with only the source replaced by [newSource]. */
    open fun cloneWithSource(newSource: Any): Any =
        TreeSelectionEvent(newSource, paths, areNew, oldLeadSelectionPath, newLeadSelectionPath)
}
