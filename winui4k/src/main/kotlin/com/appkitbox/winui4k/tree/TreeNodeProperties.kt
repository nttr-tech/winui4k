package com.appkitbox.winui4k.tree

// When Swing's TreeNode / DefaultMutableTreeNode is used from Kotlin, Java's getters / setters look like properties, so you can
// write `node.userObject` and `node.childCount`. The nodes here are classes written in Kotlin and do not get that, so extension properties
// with the same names are added to match the way Swing is written (Java uses the getters / setters as is, so they are hidden from Java).

/** The same as TreeNode.getChildCount. */
val TreeNode.childCount: Int
    @JvmSynthetic get() = getChildCount()

/** The same as TreeNode.getParent. */
val TreeNode.parent: TreeNode?
    @JvmSynthetic get() = getParent()

/** The same as TreeNode.getAllowsChildren. */
val TreeNode.allowsChildren: Boolean
    @JvmSynthetic get() = getAllowsChildren()

/** The same as TreeNode.isLeaf. */
val TreeNode.isLeaf: Boolean
    @JvmSynthetic get() = isLeaf()

/** The same as [DefaultMutableTreeNode.getUserObject] / [DefaultMutableTreeNode.setUserObject]. */
var DefaultMutableTreeNode.userObject: Any?
    @JvmSynthetic get() = getUserObject()

    @JvmSynthetic set(value) = setUserObject(value)

/** The same as [DefaultMutableTreeNode.getAllowsChildren] / [DefaultMutableTreeNode.setAllowsChildren]. */
var DefaultMutableTreeNode.allowsChildren: Boolean
    @JvmSynthetic get() = getAllowsChildren()

    @JvmSynthetic set(value) = setAllowsChildren(value)

/** The same as [DefaultMutableTreeNode.isRoot]. */
val DefaultMutableTreeNode.isRoot: Boolean
    @JvmSynthetic get() = isRoot()

/** The same as [DefaultMutableTreeNode.getDepth]. */
val DefaultMutableTreeNode.depth: Int
    @JvmSynthetic get() = getDepth()

/** The same as [DefaultMutableTreeNode.getLevel]. */
val DefaultMutableTreeNode.level: Int
    @JvmSynthetic get() = getLevel()

/** The same as [DefaultMutableTreeNode.getPath]. */
val DefaultMutableTreeNode.path: Array<TreeNode>
    @JvmSynthetic get() = getPath()

/** The same as [DefaultMutableTreeNode.getUserObjectPath]. */
val DefaultMutableTreeNode.userObjectPath: Array<Any?>
    @JvmSynthetic get() = getUserObjectPath()

/** The same as [DefaultMutableTreeNode.getRoot]. */
val DefaultMutableTreeNode.root: TreeNode
    @JvmSynthetic get() = getRoot()

/** The same as [DefaultMutableTreeNode.getFirstChild] (NoSuchElementException if there are no children). */
val DefaultMutableTreeNode.firstChild: TreeNode
    @JvmSynthetic get() = getFirstChild()

/** The same as [DefaultMutableTreeNode.getLastChild] (NoSuchElementException if there are no children). */
val DefaultMutableTreeNode.lastChild: TreeNode
    @JvmSynthetic get() = getLastChild()

/** The same as [DefaultMutableTreeNode.getSiblingCount]. */
val DefaultMutableTreeNode.siblingCount: Int
    @JvmSynthetic get() = getSiblingCount()

/** The same as [DefaultMutableTreeNode.getNextSibling]. */
val DefaultMutableTreeNode.nextSibling: DefaultMutableTreeNode?
    @JvmSynthetic get() = getNextSibling()

/** The same as [DefaultMutableTreeNode.getPreviousSibling]. */
val DefaultMutableTreeNode.previousSibling: DefaultMutableTreeNode?
    @JvmSynthetic get() = getPreviousSibling()

/** The same as [DefaultMutableTreeNode.getFirstLeaf]. */
val DefaultMutableTreeNode.firstLeaf: DefaultMutableTreeNode
    @JvmSynthetic get() = getFirstLeaf()

/** The same as [DefaultMutableTreeNode.getLastLeaf]. */
val DefaultMutableTreeNode.lastLeaf: DefaultMutableTreeNode
    @JvmSynthetic get() = getLastLeaf()

/** The same as [DefaultMutableTreeNode.getNextLeaf]. */
val DefaultMutableTreeNode.nextLeaf: DefaultMutableTreeNode?
    @JvmSynthetic get() = getNextLeaf()

/** The same as [DefaultMutableTreeNode.getPreviousLeaf]. */
val DefaultMutableTreeNode.previousLeaf: DefaultMutableTreeNode?
    @JvmSynthetic get() = getPreviousLeaf()

/** The same as [DefaultMutableTreeNode.getLeafCount]. */
val DefaultMutableTreeNode.leafCount: Int
    @JvmSynthetic get() = getLeafCount()

/** The same as [DefaultMutableTreeNode.getNextNode]. */
val DefaultMutableTreeNode.nextNode: DefaultMutableTreeNode?
    @JvmSynthetic get() = getNextNode()

/** The same as [DefaultMutableTreeNode.getPreviousNode]. */
val DefaultMutableTreeNode.previousNode: DefaultMutableTreeNode?
    @JvmSynthetic get() = getPreviousNode()
