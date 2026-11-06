package com.appkitbox.winui4k.tree

/**
 * javax.swing.tree.TreeModel-style: tree data. Nodes are arbitrary objects (not necessarily [TreeNode]s).
 * The standard implementation is [DefaultTreeModel].
 */
interface TreeModel {
    /** The root, or null for an empty tree. */
    fun getRoot(): Any?

    /** The child of [parent] at [index]. */
    fun getChild(parent: Any, index: Int): Any?

    /** The number of children of [parent]. */
    fun getChildCount(parent: Any): Int

    /** True if [node] is a leaf. */
    fun isLeaf(node: Any): Boolean

    /** Called when the value of the node at [path] was changed to [newValue] by a user edit. */
    fun valueForPathChanged(path: TreePath, newValue: Any?)

    /** The position of [child] among the children of [parent], or -1 if it is not a child. */
    fun getIndexOfChild(parent: Any?, child: Any?): Int

    /** Adds a listener. Adding the same listener twice makes it called twice. */
    fun addTreeModelListener(l: TreeModelListener)

    /** Removes a listener (if added several times, the one added last). */
    fun removeTreeModelListener(l: TreeModelListener)
}
