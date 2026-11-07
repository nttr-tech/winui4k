package com.appkitbox.winui4k.tree

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.NoSuchElementException

/**
 * Tests that check, as when using Swing's DefaultMutableTreeNode from Kotlin, that it can be read and written in property form such as `node.userObject` and `node.childCount`
 * (no UI needed). The values are the same as those of the getters / setters.
 *
 * The tree used:
 * ```
 * root
 * ├─ a
 * │  └─ a1
 * └─ b
 * ```
 */
@Tags("Pure")
class TreeNodePropertiesTest : FunSpec() {
    private class Fixture {
        val root = DefaultMutableTreeNode("root")
        val a = DefaultMutableTreeNode("a")
        val a1 = DefaultMutableTreeNode("a1")
        val b = DefaultMutableTreeNode("b")

        init {
            root.add(a)
            a.add(a1)
            root.add(b)
        }
    }

    init {
        test("userObject and allowsChildren can be read and written as properties and have the same values as the getters / setters") {
            val node = DefaultMutableTreeNode("First")
            node.userObject shouldBe "First"
            node.userObject = 42
            node.getUserObject() shouldBe 42
            node.userObject = null
            node.userObject.shouldBeNull()

            node.allowsChildren shouldBe true
            node.allowsChildren = false
            node.getAllowsChildren() shouldBe false
        }

        test("the number of children, the parent, and whether it is a leaf or the root can be read as properties") {
            val f = Fixture()
            f.root.childCount shouldBe 2
            f.a1.childCount shouldBe 0
            f.a1.parent shouldBeSameInstanceAs f.a
            f.root.parent.shouldBeNull()
            f.a1.isLeaf shouldBe true
            f.a.isLeaf shouldBe false
            f.root.isRoot shouldBe true
            f.a.isRoot shouldBe false
        }

        test("childCount, parent, allowsChildren and isLeaf can be read even with the TreeNode type") {
            val f = Fixture()
            val node: TreeNode = f.a
            node.childCount shouldBe 1
            node.parent shouldBeSameInstanceAs f.root
            node.allowsChildren shouldBe true
            node.isLeaf shouldBe false
        }

        test("the depth, level, root and path can be read as properties") {
            val f = Fixture()
            f.root.depth shouldBe 2
            f.a1.depth shouldBe 0
            f.a1.level shouldBe 2
            f.a1.root shouldBeSameInstanceAs f.root
            f.a1.path.toList() shouldContainExactly listOf(f.root, f.a, f.a1)
            f.a1.userObjectPath.toList() shouldContainExactly listOf("root", "a", "a1")
        }

        test("children, siblings, leaves and the previous and next nodes can be read as properties, and firstChild of a node without children throws NoSuchElementException as in Swing") {
            val f = Fixture()
            f.root.firstChild shouldBeSameInstanceAs f.a
            f.root.lastChild shouldBeSameInstanceAs f.b
            shouldThrow<NoSuchElementException> { f.b.firstChild }
            f.a.nextSibling shouldBeSameInstanceAs f.b
            f.b.previousSibling shouldBeSameInstanceAs f.a
            f.b.nextSibling.shouldBeNull()
            f.a.siblingCount shouldBe 2
            f.root.firstLeaf shouldBeSameInstanceAs f.a1
            f.root.lastLeaf shouldBeSameInstanceAs f.b
            f.a1.nextLeaf shouldBeSameInstanceAs f.b
            f.b.previousLeaf shouldBeSameInstanceAs f.a1
            f.root.leafCount shouldBe 2
            f.a.nextNode shouldBeSameInstanceAs f.a1
            f.a1.previousNode shouldBeSameInstanceAs f.a
            f.b.nextNode.shouldBeNull()
        }
    }
}
