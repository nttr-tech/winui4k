package com.appkitbox.winui4k.tree

import java.util.EventListener

/**
 * javax.swing.event.TreeModelListener-style: receives change notifications of a [TreeModel].
 * As in Swing, all four methods are implemented (there is no adapter).
 */
interface TreeModelListener : EventListener {
    /** The values of nodes changed. [TreeModelEvent.getChildren] of the event are the changed nodes. */
    fun treeNodesChanged(e: TreeModelEvent)

    /** Nodes were added. */
    fun treeNodesInserted(e: TreeModelEvent)

    /** Nodes were removed. */
    fun treeNodesRemoved(e: TreeModelEvent)

    /** The structure of the tree changed substantially. Everything below [TreeModelEvent.getTreePath] was rebuilt. */
    fun treeStructureChanged(e: TreeModelEvent)
}
