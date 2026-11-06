package com.appkitbox.winui4k.tree

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Enumeration
import java.util.NoSuchElementException

/**
 * Tests DefaultMutableTreeNode (no UI needed). The expected values were derived from the rules of Swing's javax.swing.tree.DefaultMutableTreeNode.
 *
 * The tree used:
 * ```
 * root
 * ├─ a
 * │  ├─ a1
 * │  └─ a2
 * │     └─ a21
 * ├─ b
 * └─ c
 *    ├─ c1
 *    └─ c2
 * ```
 */
class DefaultMutableTreeNodeTest : FunSpec() {
    private class Fixture {
        val root = DefaultMutableTreeNode("root")
        val a = DefaultMutableTreeNode("a")
        val a1 = DefaultMutableTreeNode("a1")
        val a2 = DefaultMutableTreeNode("a2")
        val a21 = DefaultMutableTreeNode("a21")
        val b = DefaultMutableTreeNode("b")
        val c = DefaultMutableTreeNode("c")
        val c1 = DefaultMutableTreeNode("c1")
        val c2 = DefaultMutableTreeNode("c2")

        init {
            root.add(a)
            root.add(b)
            root.add(c)
            a.add(a1)
            a.add(a2)
            a2.add(a21)
            c.add(c1)
            c.add(c2)
        }
    }

    private fun Enumeration<TreeNode>.names(): List<String> {
        val result = mutableListOf<String>()
        while (hasMoreElements()) result += nextElement().toString()
        return result
    }

    private fun DefaultMutableTreeNode.childNames(): List<String> = children().names()

    init {
        test("adding a child appends it, and the child count, index, parent and child list match") {
            val f = Fixture()
            listOf(f.root.getChildCount(), f.a.getChildCount(), f.b.getChildCount()) shouldBe listOf(3, 2, 0)
            f.root.childNames() shouldBe listOf("a", "b", "c")
            listOf(f.root.getChildAt(1), f.a2.getParent(), f.root.getParent()) shouldBe listOf(f.b, f.a, null)
            listOf(f.root.getIndex(f.c), f.a.getIndex(f.a2)) shouldBe listOf(2, 1)
        }

        test("getIndex is -1 for a node that is not a child (a grandchild or an unrelated node)") {
            val f = Fixture()
            listOf(f.root.getIndex(f.a1), f.a.getIndex(f.c1), f.b.getIndex(f.a)) shouldBe listOf(-1, -1, -1)
        }

        test("insert puts the child at the given position, and an out-of-range position throws ArrayIndexOutOfBoundsException without changing the node") {
            val f = Fixture()
            val extra = DefaultMutableTreeNode("x")
            f.root.insert(extra, 0)
            f.root.childNames() shouldBe listOf("x", "a", "b", "c")
            val other = DefaultMutableTreeNode("y")
            shouldThrow<ArrayIndexOutOfBoundsException> { f.root.insert(other, 5) }
            shouldThrow<ArrayIndexOutOfBoundsException> { f.root.insert(other, -1) }
            f.root.getChildCount() shouldBe 4
            other.getParent() shouldBe null
        }

        test("add removes the child from its other parent and moves it; the old parent's child count drops and the parent changes") {
            val f = Fixture()
            f.a.add(f.c1)
            f.a.childNames() shouldBe listOf("a1", "a2", "c1")
            f.c.childNames() shouldBe listOf("c2")
            f.c1.getParent() shouldBe f.a
        }

        test("add moves one of its own children to the end when given it") {
            val f = Fixture()
            f.root.add(f.a)
            f.root.childNames() shouldBe listOf("b", "c", "a")
            f.root.getChildCount() shouldBe 3
        }

        test("when insert moves a child back within the same parent, the position is counted in the order after removing it") {
            val f = Fixture()
            f.root.insert(f.a, 2)
            f.root.childNames() shouldBe listOf("b", "c", "a")
            shouldThrow<ArrayIndexOutOfBoundsException> { f.root.insert(f.b, 3) }
            f.root.childNames() shouldBe listOf("b", "c", "a")
        }

        test("insert / add to a node that cannot have children throws IllegalStateException") {
            val leaf = DefaultMutableTreeNode("leaf", false)
            shouldThrow<IllegalStateException> { leaf.add(DefaultMutableTreeNode("child")) }
            shouldThrow<IllegalStateException> { leaf.insert(DefaultMutableTreeNode("child"), 0) }
            leaf.getChildCount() shouldBe 0
        }

        test("making the node itself or an ancestor a child throws IllegalArgumentException and the tree does not change") {
            val f = Fixture()
            shouldThrow<IllegalArgumentException> { f.a2.add(f.root) }
            shouldThrow<IllegalArgumentException> { f.a2.add(f.a) }
            shouldThrow<IllegalArgumentException> { f.a2.add(f.a2) }
            f.root.childNames() shouldBe listOf("a", "b", "c")
            f.a.childNames() shouldBe listOf("a1", "a2")
        }

        test("getChildAt throws ArrayIndexOutOfBoundsException for a node without children, out of range, or a negative position") {
            val f = Fixture()
            shouldThrow<ArrayIndexOutOfBoundsException> { f.b.getChildAt(0) }
            shouldThrow<ArrayIndexOutOfBoundsException> { f.a.getChildAt(2) }
            shouldThrow<ArrayIndexOutOfBoundsException> { f.a.getChildAt(-1) }
        }

        test("remove(index) removes the child and resets its parent to null, and remove(node) throws IllegalArgumentException for a non-child") {
            val f = Fixture()
            f.root.remove(1)
            f.root.childNames() shouldBe listOf("a", "c")
            f.b.getParent() shouldBe null
            f.root.remove(f.c)
            f.root.childNames() shouldBe listOf("a")
            shouldThrow<IllegalArgumentException> { f.root.remove(f.a1) }
            shouldThrow<ArrayIndexOutOfBoundsException> { f.root.remove(3) }
        }

        test("removeFromParent removes the node from its parent and does nothing for the root; removeAllChildren resets every child's parent to null") {
            val f = Fixture()
            f.a2.removeFromParent()
            f.a.childNames() shouldBe listOf("a1")
            f.a2.getParent() shouldBe null
            f.root.removeFromParent()
            f.root.getChildCount() shouldBe 3
            f.root.removeAllChildren()
            listOf(f.root.getChildCount(), f.a.getParent(), f.c.getParent()) shouldBe listOf(0, null, null)
        }

        test("setAllowsChildren(false) removes all children, and setting the same value again keeps the children") {
            val f = Fixture()
            f.a.setAllowsChildren(true)
            f.a.getChildCount() shouldBe 2
            f.a.setAllowsChildren(false)
            listOf(f.a.getAllowsChildren(), f.a.getChildCount(), f.a1.getParent()) shouldBe listOf(false, 0, null)
        }

        test("isLeaf depends only on the child count: a node without children is a leaf even if it can have children, and getAllowsChildren is read separately") {
            val f = Fixture()
            listOf(f.b.isLeaf(), f.b.getAllowsChildren(), f.a.isLeaf(), DefaultMutableTreeNode("x", false).getAllowsChildren()) shouldBe
                listOf(true, true, false, false)
        }

        test("getLevel and getDepth: the root is 0 and a21 is 3; the depth is the number of levels to the deepest descendant (0 for a leaf)") {
            val f = Fixture()
            listOf(f.root.getLevel(), f.a.getLevel(), f.a2.getLevel(), f.a21.getLevel()) shouldBe listOf(0, 1, 2, 3)
            listOf(f.root.getDepth(), f.a.getDepth(), f.c.getDepth(), f.a21.getDepth()) shouldBe listOf(3, 2, 1, 0)
        }

        test("getPath and getUserObjectPath return from the root to the node, and getRoot / isRoot refer to the root") {
            val f = Fixture()
            f.a21.getPath().map { it.toString() } shouldBe listOf("root", "a", "a2", "a21")
            f.a21.getUserObjectPath().toList() shouldBe listOf("root", "a", "a2", "a21")
            f.root.getPath().toList() shouldBe listOf<TreeNode>(f.root)
            listOf(f.a21.getRoot() === f.root, f.root.getRoot() === f.root, f.root.isRoot(), f.a.isRoot()) shouldBe
                listOf(true, true, true, false)
        }

        test("isNodeAncestor / isNodeDescendant include the node itself, and null is false") {
            val f = Fixture()
            listOf(
                f.a21.isNodeAncestor(f.root),
                f.a21.isNodeAncestor(f.a21),
                f.a21.isNodeAncestor(f.b),
                f.a21.isNodeAncestor(null),
                f.root.isNodeDescendant(f.a21),
                f.root.isNodeDescendant(f.root),
                f.a.isNodeDescendant(f.c1),
                f.a.isNodeDescendant(null),
            ) shouldBe listOf(true, true, false, false, true, true, false, false)
        }

        test("getSharedAncestor is the deepest common ancestor: that node if one is an ancestor of the other, null if not connected") {
            val f = Fixture()
            val stranger = DefaultMutableTreeNode("stranger")
            listOf(
                f.a21.getSharedAncestor(f.c1),
                f.a1.getSharedAncestor(f.a2),
                f.a21.getSharedAncestor(f.a2),
                f.a2.getSharedAncestor(f.a21),
                f.a.getSharedAncestor(f.a),
                f.a.getSharedAncestor(stranger),
                f.a.getSharedAncestor(null),
            ) shouldBe listOf(f.root, f.a, f.a2, f.a2, f.a, null, null)
        }

        test("isNodeRelated tells whether a node is in the same tree (null is false)") {
            val f = Fixture()
            listOf(f.a1.isNodeRelated(f.c2), f.a1.isNodeRelated(DefaultMutableTreeNode("x")), f.a1.isNodeRelated(null)) shouldBe
                listOf(true, false, false)
        }

        test("preorderEnumeration has parents first: root, a, a1, a2, a21, b, c, c1, c2") {
            Fixture().root.preorderEnumeration().names() shouldBe listOf("root", "a", "a1", "a2", "a21", "b", "c", "c1", "c2")
        }

        test("postorderEnumeration and depthFirstEnumeration have children first: a1, a21, a2, a, b, c1, c2, c, root") {
            val f = Fixture()
            val expected = listOf("a1", "a21", "a2", "a", "b", "c1", "c2", "c", "root")
            f.root.postorderEnumeration().names() shouldBe expected
            f.root.depthFirstEnumeration().names() shouldBe expected
        }

        test("breadthFirstEnumeration goes from shallower levels: root, a, b, c, a1, a2, c1, c2, a21") {
            Fixture().root.breadthFirstEnumeration().names() shouldBe listOf("root", "a", "b", "c", "a1", "a2", "c1", "c2", "a21")
        }

        test("enumerating a subtree starts at that node, and enumerating a leaf gives just that leaf") {
            val f = Fixture()
            f.a.preorderEnumeration().names() shouldBe listOf("a", "a1", "a2", "a21")
            f.a.postorderEnumeration().names() shouldBe listOf("a1", "a21", "a2", "a")
            f.b.breadthFirstEnumeration().names() shouldBe listOf("b")
        }

        test("nextElement after an enumeration is exhausted throws NoSuchElementException") {
            val f = Fixture()
            listOf(
                f.b.preorderEnumeration(),
                f.b.postorderEnumeration(),
                f.b.breadthFirstEnumeration(),
                f.b.children(),
            ).forEach { e ->
                while (e.hasMoreElements()) e.nextElement()
                shouldThrow<NoSuchElementException> { e.nextElement() }
            }
        }

        test("pathFromAncestorEnumeration returns from the ancestor to the node, ancestor first, and throws IllegalArgumentException if it is not an ancestor") {
            val f = Fixture()
            f.a21.pathFromAncestorEnumeration(f.root).names() shouldBe listOf("root", "a", "a2", "a21")
            f.a21.pathFromAncestorEnumeration(f.a2).names() shouldBe listOf("a2", "a21")
            f.a21.pathFromAncestorEnumeration(f.a21).names() shouldBe listOf("a21")
            shouldThrow<IllegalArgumentException> { f.a21.pathFromAncestorEnumeration(f.b) }
        }

        test("getNextNode / getPreviousNode are the neighbors in preorder, null at the ends") {
            val f = Fixture()
            listOf(f.root.getNextNode(), f.a.getNextNode(), f.a1.getNextNode(), f.a21.getNextNode(), f.b.getNextNode(), f.c2.getNextNode()) shouldBe
                listOf(f.a, f.a1, f.a2, f.b, f.c, null)
            listOf(f.root.getPreviousNode(), f.a.getPreviousNode(), f.a1.getPreviousNode(), f.b.getPreviousNode(), f.c.getPreviousNode(), f.c1.getPreviousNode()) shouldBe
                listOf(null, f.root, f.a, f.a21, f.b, f.c)
        }

        test("siblings: getNextSibling / getPreviousSibling / getSiblingCount / isNodeSibling") {
            val f = Fixture()
            listOf(f.a.getNextSibling(), f.c.getNextSibling(), f.a.getPreviousSibling(), f.b.getPreviousSibling(), f.root.getNextSibling()) shouldBe
                listOf(f.b, null, null, f.a, null)
            listOf(f.root.getSiblingCount(), f.a.getSiblingCount(), f.a21.getSiblingCount()) shouldBe listOf(1, 3, 1)
            listOf(f.a.isNodeSibling(f.b), f.a.isNodeSibling(f.a), f.a.isNodeSibling(f.a1), f.a.isNodeSibling(null), f.root.isNodeSibling(f.a)) shouldBe
                listOf(true, true, false, false, false)
        }

        test("getChildAfter / getChildBefore throw IllegalArgumentException for a non-child and return null at the ends") {
            val f = Fixture()
            listOf(f.root.getChildAfter(f.a), f.root.getChildAfter(f.c), f.root.getChildBefore(f.a), f.root.getChildBefore(f.c)) shouldBe
                listOf(f.b, null, null, f.b)
            shouldThrow<IllegalArgumentException> { f.root.getChildAfter(f.a1) }
            shouldThrow<IllegalArgumentException> { f.root.getChildBefore(f.a1) }
        }

        test("getFirstChild / getLastChild throw NoSuchElementException if there are no children") {
            val f = Fixture()
            listOf(f.root.getFirstChild(), f.root.getLastChild()) shouldBe listOf(f.a, f.c)
            shouldThrow<NoSuchElementException> { f.b.getFirstChild() }
            shouldThrow<NoSuchElementException> { f.b.getLastChild() }
        }

        test("isNodeChild is true only for direct children (false for grandchildren, null and nodes without children)") {
            val f = Fixture()
            listOf(f.root.isNodeChild(f.a), f.root.isNodeChild(f.a1), f.root.isNodeChild(null), f.b.isNodeChild(f.a)) shouldBe
                listOf(true, false, false, false)
        }

        test("leaves: getFirstLeaf / getLastLeaf / getNextLeaf / getPreviousLeaf / getLeafCount") {
            val f = Fixture()
            listOf(f.root.getFirstLeaf(), f.root.getLastLeaf(), f.b.getFirstLeaf(), f.a.getLastLeaf()) shouldBe listOf(f.a1, f.c2, f.b, f.a21)
            listOf(f.a1.getNextLeaf(), f.a21.getNextLeaf(), f.b.getNextLeaf(), f.c2.getNextLeaf(), f.root.getNextLeaf()) shouldBe
                listOf(f.a21, f.b, f.c1, null, null)
            listOf(f.b.getPreviousLeaf(), f.c1.getPreviousLeaf(), f.a1.getPreviousLeaf(), f.root.getPreviousLeaf()) shouldBe
                listOf(f.a21, f.b, null, null)
            listOf(f.root.getLeafCount(), f.a.getLeafCount(), f.b.getLeafCount()) shouldBe listOf(5, 2, 1)
        }

        test("userObject: toString is the userObject's toString, an empty string if null, and setUserObject replaces it") {
            val n = DefaultMutableTreeNode()
            n.getUserObject() shouldBe null
            n.toString() shouldBe ""
            n.setUserObject(42)
            listOf(n.getUserObject(), n.toString()) shouldBe listOf<Any?>(42, "42")
            DefaultMutableTreeNode("x").getAllowsChildren() shouldBe true
        }

        test("children of a node without children is empty, like EMPTY_ENUMERATION") {
            val f = Fixture()
            f.b.children().hasMoreElements() shouldBe false
            DefaultMutableTreeNode.EMPTY_ENUMERATION.hasMoreElements() shouldBe false
        }
    }
}
