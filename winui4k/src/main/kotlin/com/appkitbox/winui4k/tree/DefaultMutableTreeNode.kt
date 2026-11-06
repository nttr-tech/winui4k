package com.appkitbox.winui4k.tree

import java.util.ArrayDeque
import java.util.Collections
import java.util.Enumeration
import java.util.NoSuchElementException

/**
 * javax.swing.tree.DefaultMutableTreeNode-style: a tree node that holds any value ([getUserObject]) and whose children can be added and removed.
 *
 * The rules are the same as Swing.
 * - Adding a child first removes it from its current parent if it is under another one (a node can be under only one parent).
 *   Trying to make the node itself or one of its ancestors a child throws [IllegalArgumentException], and adding a child to a node whose [getAllowsChildren] is false throws [IllegalStateException].
 * - [toString] is the userObject's [toString]. If the userObject is null, Swing returns null, but this returns an empty string.
 * - Inserting at an out-of-range position throws after the node has been modified in Swing, but here it throws [ArrayIndexOutOfBoundsException] without changing the node.
 *
 * Swing's protected fields (parent / children / userObject / allowsChildren), clone and serialization are not ported.
 * Kotlin also reads and writes with `getUserObject()` / `setUserObject()` rather than a property like `userObject`
 * (because [MutableTreeNode.setUserObject] has the same shape as in Swing).
 */
open class DefaultMutableTreeNode @JvmOverloads constructor(
    userObject: Any? = null,
    allowsChildren: Boolean = true,
) : MutableTreeNode {
    private var parentNode: MutableTreeNode? = null
    private var childNodes: MutableList<TreeNode>? = null
    private var userObjectValue: Any? = userObject
    private var allowsChildrenValue: Boolean = allowsChildren

    override fun insert(child: MutableTreeNode, index: Int) {
        check(allowsChildrenValue) { "node does not allow children" }
        require(!isNodeAncestor(child)) { "new child is an ancestor" }
        val oldParent = child.getParent()
        // For a move within the same parent, the limit is the child count after removing it first
        val limit = getChildCount() - if (oldParent === this) 1 else 0
        if (index < 0 || index > limit) throw ArrayIndexOutOfBoundsException("$index > $limit")
        (oldParent as MutableTreeNode?)?.remove(child)
        child.setParent(this)
        val list = childNodes ?: mutableListOf<TreeNode>().also { childNodes = it }
        list.add(index, child)
    }

    override fun remove(index: Int) {
        val child = getChildAt(index) as MutableTreeNode
        childNodes!!.removeAt(index)
        child.setParent(null)
    }

    override fun remove(node: MutableTreeNode) {
        require(isNodeChild(node)) { "argument is not a child" }
        remove(getIndex(node))
    }

    override fun setParent(newParent: MutableTreeNode?) {
        parentNode = newParent
    }

    override fun getParent(): TreeNode? = parentNode

    override fun getChildAt(childIndex: Int): TreeNode {
        val list = childNodes ?: throw ArrayIndexOutOfBoundsException("node has no children")
        if (childIndex < 0 || childIndex >= list.size) {
            throw ArrayIndexOutOfBoundsException("Array index out of range: $childIndex")
        }
        return list[childIndex]
    }

    override fun getChildCount(): Int = childNodes?.size ?: 0

    override fun getIndex(node: TreeNode): Int {
        if (!isNodeChild(node)) return -1
        return childNodes!!.indexOf(node)
    }

    /** Returns the children in order. Walks a copy of the child list taken at the time of the call. */
    override fun children(): Enumeration<TreeNode> {
        val list = childNodes ?: return EMPTY_ENUMERATION
        return Collections.enumeration(list.toList())
    }

    /** Sets whether the node can have children. Changing it from true to false removes all children. */
    fun setAllowsChildren(allows: Boolean) {
        if (allows != allowsChildrenValue) {
            allowsChildrenValue = allows
            if (!allows) removeAllChildren()
        }
    }

    override fun getAllowsChildren(): Boolean = allowsChildrenValue

    override fun setUserObject(obj: Any?) {
        userObjectValue = obj
    }

    /** The value this node holds. */
    fun getUserObject(): Any? = userObjectValue

    override fun removeFromParent() {
        (getParent() as MutableTreeNode?)?.remove(this)
    }

    /** Removes all children. */
    fun removeAllChildren() {
        for (i in getChildCount() - 1 downTo 0) remove(i)
    }

    /**
     * Adds [newChild] as the last child. If [newChild] is already a child of this node, moves it to the end.
     * If it is a child of another parent, removes it from there and moves it here.
     */
    fun add(newChild: MutableTreeNode) {
        if (newChild.getParent() === this) {
            insert(newChild, getChildCount() - 1)
        } else {
            insert(newChild, getChildCount())
        }
    }

    // ---- Position in the tree ----

    /** True if [anotherNode] is this node or an ancestor of this node. */
    fun isNodeAncestor(anotherNode: TreeNode?): Boolean {
        if (anotherNode == null) return false
        var ancestor: TreeNode? = this
        while (ancestor != null) {
            if (ancestor === anotherNode) return true
            ancestor = ancestor.getParent()
        }
        return false
    }

    /** True if [anotherNode] is this node or a descendant of this node. */
    fun isNodeDescendant(anotherNode: DefaultMutableTreeNode?): Boolean =
        anotherNode != null && anotherNode.isNodeAncestor(this)

    /** The deepest common ancestor of this node and [aNode] (that node if either is an ancestor of the other), or null if they are not connected. */
    fun getSharedAncestor(aNode: DefaultMutableTreeNode?): TreeNode? {
        if (aNode === this) return this
        if (aNode == null) return null
        var node1: TreeNode?
        var node2: TreeNode?
        var diff: Int
        val level1 = getLevel()
        val level2 = aNode.getLevel()
        if (level2 > level1) {
            diff = level2 - level1
            node1 = aNode
            node2 = this
        } else {
            diff = level1 - level2
            node1 = this
            node2 = aNode
        }
        // Bring the depths level, then walk up both at the same time
        while (diff > 0) {
            node1 = node1!!.getParent()
            diff--
        }
        while (node1 != null) {
            if (node1 === node2) return node1
            node1 = node1.getParent()
            node2 = node2!!.getParent()
        }
        return null
    }

    /** True if [aNode] is a node of the same tree (with the same root). */
    fun isNodeRelated(aNode: DefaultMutableTreeNode?): Boolean = aNode != null && getRoot() === aNode.getRoot()

    /** The number of levels from this node to its deepest descendant (0 for a leaf). */
    fun getDepth(): Int {
        var last: TreeNode = this
        val all = breadthFirstEnumeration()
        while (all.hasMoreElements()) last = all.nextElement()
        return (last as DefaultMutableTreeNode).getLevel() - getLevel()
    }

    /** The number of levels from the root to this node (0 for the root). */
    fun getLevel(): Int {
        var levels = 0
        var ancestor: TreeNode? = getParent()
        while (ancestor != null) {
            levels++
            ancestor = ancestor.getParent()
        }
        return levels
    }

    /** The nodes from the root to this node (the root first, this node last). */
    fun getPath(): Array<TreeNode> {
        val path = ArrayList<TreeNode>()
        var node: TreeNode? = this
        while (node != null) {
            path.add(0, node)
            node = node.getParent()
        }
        return path.toTypedArray()
    }

    /** The userObjects of the nodes of [getPath], in order. */
    fun getUserObjectPath(): Array<Any?> = getPath().map { (it as DefaultMutableTreeNode).getUserObject() }.toTypedArray()

    /** The root of the tree this node belongs to (this node itself if it has no parent). */
    fun getRoot(): TreeNode {
        var ancestor: TreeNode = this
        while (true) ancestor = ancestor.getParent() ?: return ancestor
    }

    /** True if it has no parent. */
    fun isRoot(): Boolean = getParent() == null

    /** The node after this one in preorder, or null if this is the last. */
    fun getNextNode(): DefaultMutableTreeNode? {
        if (getChildCount() != 0) return getChildAt(0) as DefaultMutableTreeNode
        // If there are no children, the next sibling, or else the next sibling of an ancestor
        var node: DefaultMutableTreeNode? = this
        while (node != null) {
            val next = node.getNextSibling()
            if (next != null) return next
            node = node.getParent() as DefaultMutableTreeNode?
        }
        return null
    }

    /** The node before this one in preorder, or null for the root. */
    fun getPreviousNode(): DefaultMutableTreeNode? {
        val myParent = getParent() as DefaultMutableTreeNode? ?: return null
        val previousSibling = getPreviousSibling() ?: return myParent
        return if (previousSibling.getChildCount() == 0) previousSibling else previousSibling.getLastLeaf()
    }

    // ---- Enumerations ----

    /** Returns all nodes under and including this node in preorder (parents before children). */
    fun preorderEnumeration(): Enumeration<TreeNode> = PreorderEnumeration(this)

    /** Returns all nodes under and including this node in postorder (children before parents). */
    fun postorderEnumeration(): Enumeration<TreeNode> = PostorderEnumeration(this)

    /** Returns all nodes under and including this node breadth-first (shallower levels first). */
    fun breadthFirstEnumeration(): Enumeration<TreeNode> = BreadthFirstEnumeration(this)

    /** Same as [postorderEnumeration]. */
    fun depthFirstEnumeration(): Enumeration<TreeNode> = postorderEnumeration()

    /**
     * Returns the nodes from [ancestor] to this node, starting with [ancestor].
     * [IllegalArgumentException] if [ancestor] is not an ancestor of this node (or this node itself).
     */
    fun pathFromAncestorEnumeration(ancestor: TreeNode): Enumeration<TreeNode> {
        val path = ArrayList<TreeNode>()
        var current: TreeNode = this
        path.add(current)
        while (current !== ancestor) {
            current = current.getParent()
                ?: throw IllegalArgumentException("node $ancestor is not an ancestor of $this")
            path.add(current)
        }
        path.reverse()
        return Collections.enumeration(path)
    }

    // ---- Children ----

    /** True if [aNode] is a child of this node. */
    fun isNodeChild(aNode: TreeNode?): Boolean = aNode != null && getChildCount() != 0 && aNode.getParent() === this

    /** The first child. [NoSuchElementException] if there are no children. */
    fun getFirstChild(): TreeNode {
        if (getChildCount() == 0) throw NoSuchElementException("node has no children")
        return getChildAt(0)
    }

    /** The last child. [NoSuchElementException] if there are no children. */
    fun getLastChild(): TreeNode {
        if (getChildCount() == 0) throw NoSuchElementException("node has no children")
        return getChildAt(getChildCount() - 1)
    }

    /** The child after [aChild], or null if it is the last child. [IllegalArgumentException] if [aChild] is not a child of this node. */
    fun getChildAfter(aChild: TreeNode): TreeNode? {
        val index = getIndex(aChild)
        require(index != -1) { "node is not a child" }
        return if (index < getChildCount() - 1) getChildAt(index + 1) else null
    }

    /** The child before [aChild], or null if it is the first child. [IllegalArgumentException] if [aChild] is not a child of this node. */
    fun getChildBefore(aChild: TreeNode): TreeNode? {
        val index = getIndex(aChild)
        require(index != -1) { "argument is not a child" }
        return if (index > 0) getChildAt(index - 1) else null
    }

    // ---- Siblings ----

    /** True if [anotherNode] is this node itself or a node with the same parent. */
    fun isNodeSibling(anotherNode: TreeNode?): Boolean {
        if (anotherNode == null) return false
        if (anotherNode === this) return true
        val myParent = getParent()
        return myParent != null && myParent === anotherNode.getParent()
    }

    /** The number of siblings (including this node itself). 1 for the root. */
    fun getSiblingCount(): Int = getParent()?.getChildCount() ?: 1

    /** The next sibling, or null for the last child or the root. */
    fun getNextSibling(): DefaultMutableTreeNode? =
        (getParent() as DefaultMutableTreeNode?)?.getChildAfter(this) as DefaultMutableTreeNode?

    /** The previous sibling, or null for the first child or the root. */
    fun getPreviousSibling(): DefaultMutableTreeNode? =
        (getParent() as DefaultMutableTreeNode?)?.getChildBefore(this) as DefaultMutableTreeNode?

    // ---- Leaves ----

    /** True if there are no children (whether it can have children is not considered; see [getAllowsChildren]). */
    override fun isLeaf(): Boolean = getChildCount() == 0

    /** The first (leftmost) leaf under this node. */
    fun getFirstLeaf(): DefaultMutableTreeNode {
        var node: DefaultMutableTreeNode = this
        while (!node.isLeaf()) node = node.getFirstChild() as DefaultMutableTreeNode
        return node
    }

    /** The last (rightmost) leaf under this node. */
    fun getLastLeaf(): DefaultMutableTreeNode {
        var node: DefaultMutableTreeNode = this
        while (!node.isLeaf()) node = node.getLastChild() as DefaultMutableTreeNode
        return node
    }

    /** The first leaf to the right of this node in the whole tree, or null if none. */
    fun getNextLeaf(): DefaultMutableTreeNode? {
        val myParent = getParent() as DefaultMutableTreeNode? ?: return null
        return getNextSibling()?.getFirstLeaf() ?: myParent.getNextLeaf()
    }

    /** The last leaf to the left of this node in the whole tree, or null if none. */
    fun getPreviousLeaf(): DefaultMutableTreeNode? {
        val myParent = getParent() as DefaultMutableTreeNode? ?: return null
        return getPreviousSibling()?.getLastLeaf() ?: myParent.getPreviousLeaf()
    }

    /** The number of leaves under this node (1 if this node is a leaf). */
    fun getLeafCount(): Int {
        var count = 0
        val all = breadthFirstEnumeration()
        while (all.hasMoreElements()) {
            if (all.nextElement().isLeaf()) count++
        }
        return count
    }

    /** The userObject as a string. An empty string if the userObject is null. */
    override fun toString(): String = userObjectValue?.toString() ?: ""

    private class PreorderEnumeration(root: TreeNode) : Enumeration<TreeNode> {
        private val stack = ArrayDeque<Enumeration<out TreeNode>>()

        init {
            stack.push(Collections.enumeration(listOf(root)))
        }

        override fun hasMoreElements(): Boolean = stack.isNotEmpty() && stack.peek().hasMoreElements()

        override fun nextElement(): TreeNode {
            val current = stack.peek() ?: throw NoSuchElementException("No more elements")
            val node = current.nextElement()
            val children = node.children()
            if (!current.hasMoreElements()) stack.pop()
            if (children.hasMoreElements()) stack.push(children)
            return node
        }
    }

    private class PostorderEnumeration(private var root: TreeNode?) : Enumeration<TreeNode> {
        private val children: Enumeration<out TreeNode> = root!!.children()
        private var subtree: Enumeration<TreeNode> = EMPTY_ENUMERATION

        override fun hasMoreElements(): Boolean = root != null

        override fun nextElement(): TreeNode {
            val self = root ?: throw NoSuchElementException("No more elements")
            if (subtree.hasMoreElements()) return subtree.nextElement()
            if (children.hasMoreElements()) {
                subtree = PostorderEnumeration(children.nextElement())
                return subtree.nextElement()
            }
            root = null
            return self
        }
    }

    private class BreadthFirstEnumeration(root: TreeNode) : Enumeration<TreeNode> {
        private val queue = ArrayDeque<Enumeration<out TreeNode>>()

        init {
            queue.add(Collections.enumeration(listOf(root)))
        }

        override fun hasMoreElements(): Boolean = queue.isNotEmpty() && queue.peekFirst().hasMoreElements()

        override fun nextElement(): TreeNode {
            val current = queue.peekFirst() ?: throw NoSuchElementException("No more elements")
            val node = current.nextElement()
            val children = node.children()
            if (!current.hasMoreElements()) queue.removeFirst()
            if (children.hasMoreElements()) queue.addLast(children)
            return node
        }
    }

    companion object {
        /** An enumeration with no elements (returned by [children] of a node without children). */
        @JvmField
        val EMPTY_ENUMERATION: Enumeration<TreeNode> = Collections.emptyEnumeration()
    }
}
