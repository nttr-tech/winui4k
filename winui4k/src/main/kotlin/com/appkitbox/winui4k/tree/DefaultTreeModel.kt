package com.appkitbox.winui4k.tree

import java.util.concurrent.CopyOnWriteArrayList

/**
 * javax.swing.tree.DefaultTreeModel-style: the standard implementation that handles a tree of [TreeNode]s as a [TreeModel].
 *
 * Adding, removing and changing nodes through this model ([insertNodeInto] / [removeNodeFromParent] / [nodeChanged] and so on)
 * sends a [TreeModelEvent] to the [TreeModelListener]s. Changing nodes directly on the [DefaultMutableTreeNode] side notifies nothing,
 * so report such changes with [nodesWereInserted] and the like.
 *
 * - If [asksAllowsChildren] is false (the default), [isLeaf] is the node's [TreeNode.isLeaf]; if true, whether [TreeNode.getAllowsChildren] is false.
 * - Listeners are called starting from the most recently added one (same as Swing).
 *
 * Swing's protected fields, the protected getPathToRoot that takes a depth, and serialization are not ported.
 */
open class DefaultTreeModel @JvmOverloads constructor(
    private var root: TreeNode?,
    private var asksAllowsChildren: Boolean = false,
) : TreeModel {
    private val listeners = CopyOnWriteArrayList<TreeModelListener>()

    /** Sets whether to use [TreeNode.getAllowsChildren] to decide whether a node is a leaf. */
    fun setAsksAllowsChildren(newValue: Boolean) {
        asksAllowsChildren = newValue
    }

    /** True if [TreeNode.getAllowsChildren] is used to decide whether a node is a leaf. */
    fun asksAllowsChildren(): Boolean = asksAllowsChildren

    /**
     * Sets the root to [newRoot]. When set to null (and there was a root before), notifies a structure change without a path.
     * Otherwise notifies the new root with [nodeStructureChanged].
     */
    fun setRoot(newRoot: TreeNode?) {
        val oldRoot = root
        root = newRoot
        if (newRoot == null && oldRoot != null) {
            fireTreeStructureChanged(this, null as TreePath?)
        } else {
            nodeStructureChanged(newRoot)
        }
    }

    override fun getRoot(): Any? = root

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        if (parent == null || child == null) return -1
        return (parent as TreeNode).getIndex(child as TreeNode)
    }

    override fun getChild(parent: Any, index: Int): Any? = (parent as TreeNode).getChildAt(index)

    override fun getChildCount(parent: Any): Int = (parent as TreeNode).getChildCount()

    override fun isLeaf(node: Any): Boolean {
        val treeNode = node as TreeNode
        return if (asksAllowsChildren) !treeNode.getAllowsChildren() else treeNode.isLeaf()
    }

    /** Notifies that the structure under the root changed (same as [reload]; does not change the node contents). */
    fun reload() {
        reload(root)
    }

    /** Notifies that the structure under [node] changed. */
    fun reload(node: TreeNode?) {
        if (node != null) fireTreeStructureChanged(this, getPathToRoot(node), null, null)
    }

    /**
     * The value of the node at [path] was changed to [newValue] by a user edit. Sets the userObject of the node ([MutableTreeNode]) to [newValue]
     * and notifies it with [nodeChanged].
     */
    override fun valueForPathChanged(path: TreePath, newValue: Any?) {
        val node = path.getLastPathComponent() as MutableTreeNode
        node.setUserObject(newValue)
        nodeChanged(node)
    }

    /** Inserts [newChild] into [parent] at position [index] and notifies the insertion. */
    fun insertNodeInto(newChild: MutableTreeNode, parent: MutableTreeNode, index: Int) {
        parent.insert(newChild, index)
        nodesWereInserted(parent, intArrayOf(index))
    }

    /** Removes [node] from its parent and notifies the removal. [IllegalArgumentException] if it has no parent. */
    fun removeNodeFromParent(node: MutableTreeNode) {
        val parent = node.getParent() as MutableTreeNode?
        requireNotNull(parent) { "node does not have a parent." }
        val childIndex = parent.getIndex(node)
        parent.remove(childIndex)
        nodesWereRemoved(parent, intArrayOf(childIndex), arrayOf<Any?>(node))
    }

    /**
     * Notifies that the value of [node] changed. If [node] is the root, notifies with no path and no child information; otherwise notifies that the one [node] under its parent changed.
     * Nothing is notified for a node connected to neither a parent nor the root.
     */
    fun nodeChanged(node: TreeNode?) {
        if (listeners.isEmpty() || node == null) return
        val parent = node.getParent()
        if (parent != null) {
            val index = parent.getIndex(node)
            if (index != -1) nodesChanged(parent, intArrayOf(index))
        } else if (node === getRoot()) {
            nodesChanged(node, null)
        }
    }

    /** Notifies that children of [node] (those at [childIndices]) were inserted. Nothing is notified if [childIndices] is empty. */
    fun nodesWereInserted(node: TreeNode?, childIndices: IntArray?) {
        if (node == null || childIndices == null || childIndices.isEmpty()) return
        val newChildren = Array<Any?>(childIndices.size) { node.getChildAt(childIndices[it]) }
        fireTreeNodesInserted(this, getPathToRoot(node), childIndices, newChildren)
    }

    /** Notifies that children of [node] ([removedChildren], which were at [childIndices]) were removed. */
    fun nodesWereRemoved(node: TreeNode?, childIndices: IntArray?, removedChildren: Array<out Any?>?) {
        if (node != null && childIndices != null) {
            fireTreeNodesRemoved(this, getPathToRoot(node), childIndices, removedChildren)
        }
    }

    /**
     * Notifies that the values of children of [node] (those at [childIndices]) changed.
     * If [childIndices] is null and [node] is the root, notifies that the root itself changed. Nothing is notified if [childIndices] is empty.
     */
    fun nodesChanged(node: TreeNode?, childIndices: IntArray?) {
        if (node == null) return
        if (childIndices != null) {
            if (childIndices.isNotEmpty()) {
                val changed = Array<Any?>(childIndices.size) { node.getChildAt(childIndices[it]) }
                fireTreeNodesChanged(this, getPathToRoot(node), childIndices, changed)
            }
        } else if (node === getRoot()) {
            fireTreeNodesChanged(this, getPathToRoot(node), null, null)
        }
    }

    /** Notifies that the structure under [node] changed. */
    fun nodeStructureChanged(node: TreeNode?) {
        if (node != null) fireTreeStructureChanged(this, getPathToRoot(node), null, null)
    }

    /**
     * The nodes from the root to [aNode]. If the root is on the way, up to there (nothing above the root is included).
     * If [aNode] is not connected to the root, from the topmost node that could be reached. Null if [aNode] is null.
     */
    open fun getPathToRoot(aNode: TreeNode?): Array<TreeNode>? {
        if (aNode == null) return null
        val path = ArrayList<TreeNode>()
        var node: TreeNode? = aNode
        while (node != null) {
            path.add(0, node)
            if (node === root) break
            node = node.getParent()
        }
        return path.toTypedArray()
    }

    override fun addTreeModelListener(l: TreeModelListener) {
        listeners += l
    }

    override fun removeTreeModelListener(l: TreeModelListener) {
        val index = listeners.lastIndexOf(l)
        if (index >= 0) listeners.removeAt(index)
    }

    /** The registered listeners (in registration order). */
    fun getTreeModelListeners(): Array<TreeModelListener> = listeners.toTypedArray()

    /** Notifies a node change, starting from the most recently added listener. */
    protected fun fireTreeNodesChanged(source: Any, path: Array<out Any?>?, childIndices: IntArray?, children: Array<out Any?>?) {
        fire { it.treeNodesChanged(TreeModelEvent(source, path, childIndices, children)) }
    }

    /** Notifies a node insertion, starting from the most recently added listener. */
    protected fun fireTreeNodesInserted(source: Any, path: Array<out Any?>?, childIndices: IntArray?, children: Array<out Any?>?) {
        fire { it.treeNodesInserted(TreeModelEvent(source, path, childIndices, children)) }
    }

    /** Notifies a node removal, starting from the most recently added listener. */
    protected fun fireTreeNodesRemoved(source: Any, path: Array<out Any?>?, childIndices: IntArray?, children: Array<out Any?>?) {
        fire { it.treeNodesRemoved(TreeModelEvent(source, path, childIndices, children)) }
    }

    /** Notifies a structure change, starting from the most recently added listener. */
    protected fun fireTreeStructureChanged(source: Any, path: Array<out Any?>?, childIndices: IntArray?, children: Array<out Any?>?) {
        fire { it.treeStructureChanged(TreeModelEvent(source, path, childIndices, children)) }
    }

    /** Notifies a structure change, passing the path as a [TreePath] (no path if null). */
    protected fun fireTreeStructureChanged(source: Any, path: TreePath?) {
        fire { it.treeStructureChanged(TreeModelEvent(source, path)) }
    }

    private inline fun fire(call: (TreeModelListener) -> Unit) {
        // Walk the list as it was before the calls in reverse order, so that listeners added or removed during a notification have no effect
        for (listener in listeners.toTypedArray().reversed()) call(listener)
    }
}
