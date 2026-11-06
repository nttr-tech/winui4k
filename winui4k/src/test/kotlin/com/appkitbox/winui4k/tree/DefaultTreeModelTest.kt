package com.appkitbox.winui4k.tree

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests DefaultTreeModel and TreeModelEvent (no UI needed). The expected values were derived from the rules of Swing's javax.swing.tree.DefaultTreeModel.
 *
 * The tree used: root ─ a (a1, a2) / b / c (c1)
 */
@Tags("Pure")
class DefaultTreeModelTest : FunSpec() {
    private data class Ev(val kind: String, val path: List<String>?, val indices: List<Int>?, val children: List<String>?)

    private class Fixture(asksAllowsChildren: Boolean = false) {
        val root = DefaultMutableTreeNode("root")
        val a = DefaultMutableTreeNode("a")
        val a1 = DefaultMutableTreeNode("a1")
        val a2 = DefaultMutableTreeNode("a2")
        val b = DefaultMutableTreeNode("b")
        val c = DefaultMutableTreeNode("c")
        val c1 = DefaultMutableTreeNode("c1")
        val model: DefaultTreeModel

        init {
            root.add(a)
            root.add(b)
            root.add(c)
            a.add(a1)
            a.add(a2)
            c.add(c1)
            model = DefaultTreeModel(root, asksAllowsChildren)
        }
    }

    private fun event(kind: String, e: TreeModelEvent) = Ev(
        kind,
        e.getPath()?.map { it.toString() },
        e.getChildIndices()?.toList(),
        e.getChildren()?.map { it.toString() },
    )

    private fun DefaultTreeModel.record(): MutableList<Ev> {
        val events = mutableListOf<Ev>()
        addTreeModelListener(object : TreeModelListener {
            override fun treeNodesChanged(e: TreeModelEvent) {
                events += event("changed", e)
            }

            override fun treeNodesInserted(e: TreeModelEvent) {
                events += event("inserted", e)
            }

            override fun treeNodesRemoved(e: TreeModelEvent) {
                events += event("removed", e)
            }

            override fun treeStructureChanged(e: TreeModelEvent) {
                events += event("structure", e)
            }
        })
        return events
    }

    private fun path(vararg names: String) = names.toList()

    init {
        test("returns the root, children, child counts and positions as a TreeModel, and the position of a non-child or null is -1") {
            val f = Fixture()
            val m = f.model
            listOf(m.getRoot(), m.getChild(f.root, 1), m.getChildCount(f.root), m.getChildCount(f.b)) shouldBe
                listOf<Any?>(f.root, f.b, 3, 0)
            listOf(m.getIndexOfChild(f.root, f.c), m.getIndexOfChild(f.root, f.a1), m.getIndexOfChild(null, f.a), m.getIndexOfChild(f.root, null)) shouldBe
                listOf(2, -1, -1, -1)
        }

        test("isLeaf is the node's isLeaf (whether it has children) by default; with asksAllowsChildren true only nodes whose getAllowsChildren is false are leaves") {
            val f = Fixture()
            val noChildren = DefaultMutableTreeNode("n", false)
            listOf(f.model.isLeaf(f.b), f.model.isLeaf(f.a), f.model.isLeaf(noChildren)) shouldBe listOf(true, false, true)
            f.model.setAsksAllowsChildren(true)
            f.model.asksAllowsChildren() shouldBe true
            listOf(f.model.isLeaf(f.b), f.model.isLeaf(f.a), f.model.isLeaf(noChildren)) shouldBe listOf(false, false, true)
            Fixture(true).model.asksAllowsChildren() shouldBe true
        }

        test("insertNodeInto inserts into the parent and notifies treeNodesInserted once with the path to the parent, the position and the inserted node") {
            val f = Fixture()
            val events = f.model.record()
            val added = DefaultMutableTreeNode("new")
            f.model.insertNodeInto(added, f.a, 1)
            f.a.getChildAt(1) shouldBe added
            events shouldBe listOf(Ev("inserted", path("root", "a"), listOf(1), listOf("new")))
        }

        test("when insertNodeInto fails (a parent that cannot have children, inserting an ancestor, an out-of-range position), nothing is notified and the tree does not change") {
            val f = Fixture()
            val events = f.model.record()
            val leaf = DefaultMutableTreeNode("leaf", false)
            f.root.add(leaf)
            shouldThrow<IllegalStateException> { f.model.insertNodeInto(DefaultMutableTreeNode("x"), leaf, 0) }
            shouldThrow<IllegalArgumentException> { f.model.insertNodeInto(f.root, f.a1, 0) }
            shouldThrow<ArrayIndexOutOfBoundsException> { f.model.insertNodeInto(DefaultMutableTreeNode("y"), f.a, 9) }
            events shouldBe emptyList()
            f.a.getChildCount() shouldBe 2
        }

        test("removeNodeFromParent removes the node and notifies treeNodesRemoved with the path to the parent, the removed position and the removed node") {
            val f = Fixture()
            val events = f.model.record()
            f.model.removeNodeFromParent(f.a2)
            f.a2.getParent() shouldBe null
            f.a.getChildCount() shouldBe 1
            events shouldBe listOf(Ev("removed", path("root", "a"), listOf(1), listOf("a2")))
        }

        test("removeNodeFromParent on a node without a parent (the root) throws IllegalArgumentException and notifies nothing") {
            val f = Fixture()
            val events = f.model.record()
            shouldThrow<IllegalArgumentException> { f.model.removeNodeFromParent(f.root) }
            events shouldBe emptyList()
        }

        test("nodeChanged notifies treeNodesChanged with the position under the parent, and notifies the root without child information") {
            val f = Fixture()
            val events = f.model.record()
            f.model.nodeChanged(f.a1)
            f.model.nodeChanged(f.root)
            events shouldBe listOf(
                Ev("changed", path("root", "a"), listOf(0), listOf("a1")),
                Ev("changed", path("root"), null, null),
            )
        }

        test("nodeChanged notifies nothing for null or for a parentless node not connected to the root") {
            val f = Fixture()
            val events = f.model.record()
            f.model.nodeChanged(null)
            f.model.nodeChanged(DefaultMutableTreeNode("loose"))
            events shouldBe emptyList()
        }

        test("valueForPathChanged replaces the userObject of the last node of the path and notifies nodeChanged") {
            val f = Fixture()
            val events = f.model.record()
            f.model.valueForPathChanged(TreePath(arrayOf(f.root, f.a, f.a1)), "renamed")
            f.a1.getUserObject() shouldBe "renamed"
            events shouldBe listOf(Ev("changed", path("root", "a"), listOf(0), listOf("renamed")))
        }

        test("nodesWereInserted notifies the children at several positions in order of position, and notifies nothing if the positions are empty or null") {
            val f = Fixture()
            val events = f.model.record()
            f.model.nodesWereInserted(f.root, intArrayOf(0, 2))
            f.model.nodesWereInserted(f.root, intArrayOf())
            f.model.nodesWereInserted(f.root, null)
            f.model.nodesWereInserted(null, intArrayOf(0))
            events shouldBe listOf(Ev("inserted", path("root"), listOf(0, 2), listOf("a", "c")))
        }

        test("nodesWereInserted throws ArrayIndexOutOfBoundsException if a position is outside the children") {
            val f = Fixture()
            shouldThrow<ArrayIndexOutOfBoundsException> { f.model.nodesWereInserted(f.root, intArrayOf(3)) }
        }

        test("nodesWereRemoved notifies the given positions and nodes as they are (nothing for null positions)") {
            val f = Fixture()
            val events = f.model.record()
            f.model.nodesWereRemoved(f.a, intArrayOf(0, 3), arrayOf<Any?>("x", "y"))
            f.model.nodesWereRemoved(f.a, null, null)
            events shouldBe listOf(Ev("removed", path("root", "a"), listOf(0, 3), listOf("x", "y")))
        }

        test("nodesChanged notifies the children at the positions; nothing if the positions are empty, and only the root if the positions are null") {
            val f = Fixture()
            val events = f.model.record()
            f.model.nodesChanged(f.root, intArrayOf(1, 2))
            f.model.nodesChanged(f.root, intArrayOf())
            f.model.nodesChanged(f.a, null)
            f.model.nodesChanged(f.root, null)
            events shouldBe listOf(
                Ev("changed", path("root"), listOf(1, 2), listOf("b", "c")),
                Ev("changed", path("root"), null, null),
            )
        }

        test("nodeStructureChanged / reload notify treeStructureChanged with just the path") {
            val f = Fixture()
            val events = f.model.record()
            f.model.nodeStructureChanged(f.a)
            f.model.reload()
            f.model.reload(f.c1)
            f.model.nodeStructureChanged(null)
            f.model.reload(null)
            events shouldBe listOf(
                Ev("structure", path("root", "a"), null, null),
                Ev("structure", path("root"), null, null),
                Ev("structure", path("root", "c", "c1"), null, null),
            )
        }

        test("setRoot notifies a structure change with the new root, and without a path when set to null; getRoot returns the new root") {
            val f = Fixture()
            val events = f.model.record()
            val newRoot = DefaultMutableTreeNode("new root")
            f.model.setRoot(newRoot)
            f.model.getRoot() shouldBe newRoot
            f.model.setRoot(null)
            f.model.getRoot() shouldBe null
            events shouldBe listOf(
                Ev("structure", path("new root"), null, null),
                Ev("structure", null, null, null),
            )
        }

        test("getPathToRoot returns from the root to the node, null for null, and from the topmost reachable node for a node not connected to the root") {
            val f = Fixture()
            f.model.getPathToRoot(f.c1)!!.map { it.toString() } shouldBe path("root", "c", "c1")
            f.model.getPathToRoot(f.root)!!.map { it.toString() } shouldBe path("root")
            f.model.getPathToRoot(null) shouldBe null
            val top = DefaultMutableTreeNode("top")
            val middle = DefaultMutableTreeNode("middle")
            top.add(middle)
            f.model.getPathToRoot(middle)!!.map { it.toString() } shouldBe path("top", "middle")
        }

        test("listeners are called starting from the most recently added one, a listener added twice is called twice, and removing removes the last one") {
            val f = Fixture()
            val calls = mutableListOf<String>()
            fun listener(name: String) = object : TreeModelListener {
                override fun treeNodesChanged(e: TreeModelEvent) {
                    calls += name
                }

                override fun treeNodesInserted(e: TreeModelEvent) = Unit

                override fun treeNodesRemoved(e: TreeModelEvent) = Unit

                override fun treeStructureChanged(e: TreeModelEvent) = Unit
            }
            val first = listener("first")
            val second = listener("second")
            f.model.addTreeModelListener(first)
            f.model.addTreeModelListener(second)
            f.model.addTreeModelListener(first)
            f.model.getTreeModelListeners().toList() shouldBe listOf(first, second, first)
            f.model.nodeChanged(f.a1)
            calls shouldBe listOf("first", "second", "first")
            calls.clear()
            f.model.removeTreeModelListener(first)
            f.model.nodeChanged(f.a1)
            calls shouldBe listOf("second", "first")
        }

        test("the event source is the DefaultTreeModel itself") {
            val f = Fixture()
            var source: Any? = null
            f.model.addTreeModelListener(object : TreeModelListener {
                override fun treeNodesChanged(e: TreeModelEvent) {
                    source = e.source
                }

                override fun treeNodesInserted(e: TreeModelEvent) = Unit

                override fun treeNodesRemoved(e: TreeModelEvent) = Unit

                override fun treeStructureChanged(e: TreeModelEvent) = Unit
            })
            f.model.nodeChanged(f.b)
            (source === f.model) shouldBe true
        }

        test("TreeModelEvent: created from an array path it can be read both as a TreePath and as an array, and positions and children are returned as copies") {
            val e = TreeModelEvent("src", arrayOf<Any?>("r", "a"), intArrayOf(2, 4), arrayOf<Any?>("x", "y"))
            e.getTreePath() shouldBe TreePath(arrayOf<Any?>("r", "a"))
            e.getPath()!!.toList() shouldBe listOf("r", "a")
            e.getChildIndices()!![0] = 99
            e.getChildIndices()!!.toList() shouldBe listOf(2, 4)
            e.getChildren()!!.toList() shouldBe listOf("x", "y")
            e.source shouldBe "src"
        }

        test("TreeModelEvent: an event without child information has null positions and children, and a null or empty array path throws IllegalArgumentException") {
            val e = TreeModelEvent("src", TreePath("r"))
            listOf(e.getChildIndices(), e.getChildren()) shouldBe listOf(null, null)
            TreeModelEvent("src", null as TreePath?).getPath() shouldBe null
            shouldThrow<IllegalArgumentException> { TreeModelEvent("src", null as Array<Any?>?) }
            shouldThrow<IllegalArgumentException> { TreeModelEvent("src", arrayOf<Any?>()) }
        }
    }
}
