package com.appkitbox.winui4k.tree

import java.util.EventListener
import java.util.EventObject

/**
 * javax.swing.tree.RowMapper-like: converts paths to displayed rows. Used by [TreeSelectionModel] for the row APIs and to check contiguous selection.
 * [com.appkitbox.winui4k.WTree] sets this on its selection model.
 */
fun interface RowMapper {
    /** The row of each path in [path] (-1 if not visible). null if [path] is null. */
    fun getRowsForPaths(path: Array<out TreePath?>?): IntArray?
}

/** javax.swing.event.TreeSelectionListener-like: called when the tree selection changes. */
fun interface TreeSelectionListener : EventListener {
    /** The selection changed. [TreeSelectionEvent.getPaths] are the added and removed paths. */
    fun valueChanged(e: TreeSelectionEvent)
}

/** javax.swing.event.TreeExpansionEvent-like: the path of the node being (or that was) expanded or collapsed. */
open class TreeExpansionEvent(source: Any, private val path: TreePath?) : EventObject(source) {
    /** The path being expanded or collapsed. */
    open fun getPath(): TreePath? = path
}

/**
 * javax.swing.event.TreeExpansionListener-like: called after a node is expanded or collapsed.
 * As in Swing, both methods must be implemented (there is no adapter).
 */
interface TreeExpansionListener : EventListener {
    /** A node was expanded. */
    fun treeExpanded(event: TreeExpansionEvent)

    /** A node was collapsed. */
    fun treeCollapsed(event: TreeExpansionEvent)
}

/**
 * javax.swing.event.TreeWillExpandListener-like: called before a node is expanded or collapsed; throwing [ExpandVetoException] cancels it.
 * As in Swing, both methods must be implemented (there is no adapter).
 *
 * Difference in [com.appkitbox.winui4k.WTree]: for expansion by user operation, [treeWillExpand] is called right after WinUI expands it
 * (TreeView.Expanding), and if canceled, it is collapsed again within the same notification (reverted before drawing, so the expanded state never appears on screen).
 * WinUI has no notification before collapsing, so [treeWillCollapse] is called only for programmatic collapsing (collapsePath etc.).
 */
interface TreeWillExpandListener : EventListener {
    /** Called before a node is expanded. Throwing [ExpandVetoException] prevents the expansion. */
    @Throws(ExpandVetoException::class)
    fun treeWillExpand(event: TreeExpansionEvent)

    /** Called before a node is collapsed. Throwing [ExpandVetoException] prevents the collapse. Not called for collapsing by user operation. */
    @Throws(ExpandVetoException::class)
    fun treeWillCollapse(event: TreeExpansionEvent)
}

/** javax.swing.tree.ExpandVetoException-like: thrown by a [TreeWillExpandListener] to cancel an expansion or collapse. */
open class ExpandVetoException @JvmOverloads constructor(
    /** The event of the canceled expansion or collapse. */
    @JvmField protected val event: TreeExpansionEvent,
    message: String? = null,
) : Exception(message)
