package com.appkitbox.winui4k.tree

/**
 * javax.swing.tree.MutableTreeNode-style: a [TreeNode] whose children can be added and removed.
 * The standard implementation is [DefaultMutableTreeNode].
 */
interface MutableTreeNode : TreeNode {
    /** Inserts [child] at position [index]. */
    fun insert(child: MutableTreeNode, index: Int)

    /** Removes the child at [index]. */
    fun remove(index: Int)

    /** Removes the child [node]. */
    fun remove(node: MutableTreeNode)

    /** Sets this node's value to [obj]. */
    fun setUserObject(obj: Any?)

    /** Removes this node from its parent. */
    fun removeFromParent()

    /** Sets the parent to [newParent] (does not change the parent's child list; an internal operation called by [insert] and [remove]). */
    fun setParent(newParent: MutableTreeNode?)
}
