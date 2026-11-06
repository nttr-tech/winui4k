package com.appkitbox.winui4k.tree

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests TreePath (no UI needed). The expected values were derived from the rules of Swing's javax.swing.tree.TreePath. */
class TreePathTest : FunSpec() {
    private fun path(vararg components: Any): TreePath = TreePath(arrayOf(*components))

    init {
        test("a path created from an array returns its element count, last element, each element and all elements in the same order as Swing (root first)") {
            val p = path("a", "b", "c")
            listOf(p.getPathCount(), p.getLastPathComponent(), p.getPathComponent(0), p.getPathComponent(1), p.getPathComponent(2)) shouldBe
                listOf(3, "c", "a", "b", "c")
            p.getPath().toList() shouldBe listOf("a", "b", "c")
        }

        test("a single-element path has a null parent path and can also be created with TreePath(Object)") {
            val p = TreePath("root")
            listOf(p.getPathCount(), p.getLastPathComponent(), p.getParentPath()) shouldBe listOf(1, "root", null)
            p shouldBe path("root")
        }

        test("getParentPath is the path without the last element, and pathByAddingChild is a new path with an element added to the end (the original does not change)") {
            val p = path("a", "b")
            p.getParentPath() shouldBe path("a")
            val longer = p.pathByAddingChild("c")
            longer.getPath().toList() shouldBe listOf("a", "b", "c")
            p.getPathCount() shouldBe 2
            longer.getParentPath() shouldBe p
        }

        test("modifying the array returned by getPath does not change the path") {
            val p = path("a", "b")
            p.getPath()[0] = "z"
            p.getPathComponent(0) shouldBe "a"
        }

        test("getPathComponent throws IllegalArgumentException out of range (negative, or the element count or more)") {
            val p = path("a", "b")
            shouldThrow<IllegalArgumentException> { p.getPathComponent(-1) }
            shouldThrow<IllegalArgumentException> { p.getPathComponent(2) }
        }

        test("an empty array, an array whose last element is null and an array containing null in the middle throw IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> { TreePath(arrayOf<Any?>()) }
            shouldThrow<IllegalArgumentException> { TreePath(arrayOf<Any?>("a", null)) }
            shouldThrow<IllegalArgumentException> { TreePath(arrayOf<Any?>(null, "b")) }
        }

        test("equals is true only when all elements are equal in order, and hashCode is the hash of the last element") {
            val p = path("a", "b", "c")
            listOf(
                p == path("a", "b", "c"),
                p == path("x", "b", "c"),
                p == path("b", "c"),
                p == path("a", "b", "c", "d"),
                p.equals("not a path"),
            ) shouldBe listOf(true, false, false, false, false)
            // If only the last element is the same, the hash is the same but the paths differ
            p.hashCode() shouldBe "c".hashCode()
            path("x", "b", "c").hashCode() shouldBe p.hashCode()
        }

        test("isDescendant is true for the same path or an extension of it, and false for shorter paths, other branches and null") {
            val p = path("a", "b")
            listOf(
                p.isDescendant(p),
                p.isDescendant(path("a", "b")),
                p.isDescendant(path("a", "b", "c", "d")),
                p.isDescendant(path("a")),
                p.isDescendant(path("a", "x", "c")),
                p.isDescendant(path("x", "b", "c")),
                p.isDescendant(null),
            ) shouldBe listOf(true, true, true, false, false, false, false)
        }

        test("toString has the form [root, ..., last]") {
            path("a", "b", "c").toString() shouldBe "[a, b, c]"
            path("only").toString() shouldBe "[only]"
        }

        test("paths of DefaultMutableTreeNode are compared with the nodes' equals (identity)") {
            val root = DefaultMutableTreeNode("r")
            val other = DefaultMutableTreeNode("r")
            path(root) shouldBe path(root)
            (path(root) == path(other)) shouldBe false
        }
    }
}
