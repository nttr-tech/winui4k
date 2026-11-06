package com.appkitbox.winui4k.tree

import java.util.Enumeration

/**
 * javax.swing.tree.TreeNode-style: the read-only definition of one tree node.
 * The standard implementation is [DefaultMutableTreeNode].
 */
interface TreeNode {
    /** The child at [childIndex]. [ArrayIndexOutOfBoundsException] if out of range. */
    fun getChildAt(childIndex: Int): TreeNode

    /** The number of children. */
    fun getChildCount(): Int

    /** The parent, or null for the root. */
    fun getParent(): TreeNode?

    /** The index of the child [node], or -1 if it is not a child. */
    fun getIndex(node: TreeNode): Int

    /** True if the node can have children. */
    fun getAllowsChildren(): Boolean

    /** True if it is a leaf. */
    fun isLeaf(): Boolean

    /** Returns the children in order. */
    fun children(): Enumeration<out TreeNode>
}
