package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode
import com.appkitbox.winui4k.tree.DefaultTreeModel
import com.appkitbox.winui4k.tree.ExpandVetoException
import com.appkitbox.winui4k.tree.TreeExpansionEvent
import com.appkitbox.winui4k.tree.TreeExpansionListener
import com.appkitbox.winui4k.tree.TreePath
import com.appkitbox.winui4k.tree.TreeSelectionModel
import com.appkitbox.winui4k.tree.TreeWillExpandListener
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for the usage of WTree that mirrors a TreeModel (the same shape as JTree).
 * The expected rows, paths and expansion come from JTree's behavior (the root is expanded from the start, rows count visible nodes from the top).
 */
@Tags("E2E")
class WTreeModelTest : FunSpec() {
    /** The tree Food ─ Fruit (Apple, Orange) / Vegetables (Carrot ─ Variety (Kintoki)) / Water. */
    private class Sample {
        val root = DefaultMutableTreeNode("Food")
        val fruit = DefaultMutableTreeNode("Fruit")
        val apple = DefaultMutableTreeNode("Apple")
        val orange = DefaultMutableTreeNode("Orange")
        val vegetable = DefaultMutableTreeNode("Vegetables")
        val carrot = DefaultMutableTreeNode("Carrot")
        val variety = DefaultMutableTreeNode("Kintoki")
        val water = DefaultMutableTreeNode("Water")
        val model = DefaultTreeModel(root)

        init {
            root.add(fruit)
            fruit.add(apple)
            fruit.add(orange)
            root.add(vegetable)
            vegetable.add(carrot)
            carrot.add(variety)
            root.add(water)
        }

        fun path(vararg nodes: DefaultMutableTreeNode): TreePath = TreePath(arrayOf<Any?>(root, *nodes))
    }

    /** The display texts of the visible rows (from the top). */
    private fun WTree.rowTexts(): List<String> = (0 until rowCount).map { checkNotNull(getPathForRow(it)).getLastPathComponent().toString() }

    init {
        test("when the root is shown it is expanded from the start, and the rows are the root and its children") {
            val s = Sample()
            val (rows, rootPath, row1) = onUiThreadGet {
                val tree = WTree(s.root)
                Triple(tree.rowTexts(), tree.getPathForRow(0), tree.getPathForRow(1))
            }
            rows shouldBe listOf("Food", "Fruit", "Vegetables", "Water")
            rootPath shouldBe TreePath(s.root)
            row1 shouldBe s.path(s.fruit)
        }

        test("setting rootVisible to false makes the root's children the top-level rows, and the root path counts as expanded without a row") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.rootVisible = false
                tree.rowTexts() shouldBe listOf("Fruit", "Vegetables", "Water")
                tree.getPathForRow(0) shouldBe s.path(s.fruit)
                tree.isExpanded(TreePath(s.root)).shouldBeTrue()
                tree.getRowForPath(TreePath(s.root)) shouldBe -1
                tree.getRowForPath(s.path(s.water)) shouldBe 2
            }
        }

        test("changing rootVisible keeps the selection and expanded paths, and fires no selection or expansion events (as in JTree)") {
            val s = Sample()
            onUiThreadGet {
                val tree = WTree(s.model)
                tree.expandPath(s.path(s.vegetable, s.carrot))
                tree.collapsePath(s.path(s.vegetable)) // Keep Carrot expanded and hide it by closing Vegetables
                tree.expandPath(s.path(s.fruit))
                tree.selectionPath = s.path(s.fruit, s.orange)
                val events = mutableListOf<String>()
                tree.addTreeSelectionListener { events += "selection" }
                tree.addTreeExpansionListener(object : TreeExpansionListener {
                    override fun treeExpanded(event: TreeExpansionEvent) {
                        events += "expanded"
                    }

                    override fun treeCollapsed(event: TreeExpansionEvent) {
                        events += "collapsed"
                    }
                })
                fun state() = listOf(
                    tree.rowTexts(),
                    tree.selectionPath,
                    tree.getSelectionRows()?.toList(),
                    tree.isExpanded(s.path(s.vegetable)),
                    tree.nativeNodeOf(s.path(s.vegetable, s.carrot))?.isExpanded == true,
                )
                tree.rootVisible = false
                val hidden = state()
                tree.rootVisible = true
                listOf(hidden, state(), events)
            } shouldBe listOf(
                listOf(listOf("Fruit", "Apple", "Orange", "Vegetables", "Water"), s.path(s.fruit, s.orange), listOf(2), false, true),
                listOf(listOf("Food", "Fruit", "Apple", "Orange", "Vegetables", "Water"), s.path(s.fruit, s.orange), listOf(3), false, true),
                emptyList<String>(),
            )
        }

        test("children do not get native nodes until expanded, and get them on expansion") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.nativeNodeOf(s.path(s.fruit, s.apple)).shouldBeNull()
                checkNotNull(tree.nativeNodeOf(s.path(s.fruit))).hasUnrealizedChildren.shouldBeTrue()
                checkNotNull(tree.nativeNodeOf(s.path(s.water))).hasUnrealizedChildren.shouldBeFalse()
                tree.expandPath(s.path(s.fruit))
                tree.nativeNodeOf(s.path(s.fruit, s.apple))?.text shouldBe "Apple"
                tree.rowTexts() shouldBe listOf("Food", "Fruit", "Apple", "Orange", "Vegetables", "Water")
                tree.getRowForPath(s.path(s.fruit, s.orange)) shouldBe 3
            }
        }

        test("expandPath on a deep path expands all parents, and expandPath on a leaf and expandRow out of range do nothing") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.expandPath(s.path(s.vegetable, s.carrot))
                tree.isExpanded(s.path(s.vegetable)).shouldBeTrue()
                tree.isExpanded(s.path(s.vegetable, s.carrot)).shouldBeTrue()
                tree.rowTexts() shouldBe listOf("Food", "Fruit", "Vegetables", "Carrot", "Kintoki", "Water")
                tree.expandPath(s.path(s.water))
                tree.isExpanded(s.path(s.water)).shouldBeFalse()
                tree.expandRow(99)
                tree.rowCount shouldBe 6
            }
        }

        test("collapsePath remembers the expansion of descendants of the collapsed node, and isExpanded is false when a parent is closed") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.expandPath(s.path(s.vegetable, s.carrot))
                tree.collapsePath(s.path(s.vegetable))
                tree.isCollapsed(s.path(s.vegetable)).shouldBeTrue()
                // The parent is closed, so even a child that remembers its expansion has isExpanded false (as in Swing)
                tree.isExpanded(s.path(s.vegetable, s.carrot)).shouldBeFalse()
                tree.isVisible(s.path(s.vegetable, s.carrot)).shouldBeFalse()
                tree.getRowForPath(s.path(s.vegetable, s.carrot)) shouldBe -1
                tree.expandRow(2)
                tree.isExpanded(s.path(s.vegetable, s.carrot)).shouldBeTrue()
                tree.rowCount shouldBe 6
            }
        }

        test("collapsePath and makeVisible on a collapsed node expand its parents") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.collapsePath(s.path(s.vegetable, s.carrot))
                tree.isExpanded(s.path(s.vegetable)).shouldBeTrue()
                tree.collapsePath(s.path(s.vegetable))
                tree.makeVisible(s.path(s.fruit, s.apple))
                tree.isExpanded(s.path(s.fruit)).shouldBeTrue()
                tree.isVisible(s.path(s.fruit, s.apple)).shouldBeTrue()
                tree.getPathForRow(-1).shouldBeNull()
                tree.getRowForPath(null) shouldBe -1
            }
        }

        test("insertions, removals, value changes and structure changes in the model are reflected in the display") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.expandPath(s.path(s.fruit))
                s.model.insertNodeInto(DefaultMutableTreeNode("Grape"), s.fruit, 1)
                tree.rowTexts() shouldBe listOf("Food", "Fruit", "Apple", "Grape", "Orange", "Vegetables", "Water")
                s.model.removeNodeFromParent(s.apple)
                tree.rowTexts() shouldBe listOf("Food", "Fruit", "Grape", "Orange", "Vegetables", "Water")
                s.water.setUserObject("Tea")
                s.model.nodeChanged(s.water)
                tree.nativeNodeOf(s.path(s.water))?.text shouldBe "Tea"
                s.fruit.removeAllChildren()
                s.fruit.add(DefaultMutableTreeNode("Peach"))
                s.model.nodeStructureChanged(s.fruit)
                // A node that was expanded stays expanded after being rebuilt
                tree.rowTexts() shouldBe listOf("Food", "Fruit", "Peach", "Vegetables", "Tea")
            }
        }

        test("an insertion into a node whose children are not created is reflected when it is expanded") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                s.model.insertNodeInto(DefaultMutableTreeNode("Melon"), s.fruit, 2)
                tree.rowCount shouldBe 4
                tree.expandPath(s.path(s.fruit))
                tree.rowTexts() shouldBe listOf("Food", "Fruit", "Apple", "Orange", "Melon", "Vegetables", "Water")
                s.model.insertNodeInto(DefaultMutableTreeNode("Sprout"), s.water, 0)
                checkNotNull(tree.nativeNodeOf(s.path(s.water))).hasUnrealizedChildren.shouldBeTrue()
            }
        }

        test("setRoot replacing the root and replacing model rebuild the display, and changes of the old model are not delivered") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                s.model.setRoot(DefaultMutableTreeNode("Empty"))
                tree.rowTexts() shouldBe listOf("Empty")
                val other = Sample()
                tree.model = other.model
                s.model.insertNodeInto(DefaultMutableTreeNode("Not delivered"), s.model.getRoot() as DefaultMutableTreeNode, 0)
                tree.rowTexts() shouldBe listOf("Food", "Fruit", "Vegetables", "Water")
                (tree.model === other.model).shouldBeTrue()
            }
        }

        test("while a model is shown the node APIs throw IllegalStateException, and they can be used after setting model to null") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                shouldThrow<IllegalStateException> { tree.addRootNode(WTreeNode("x")) }
                shouldThrow<IllegalStateException> { tree.rootNodes }
                shouldThrow<IllegalStateException> { tree.selectedNode }
                tree.model = null
                tree.rowCount shouldBe 0
                val node = WTreeNode("Node")
                tree.addRootNode(node)
                tree.getPathForRow(0) shouldBe TreePath(node)
            }
        }

        test("in the node-building usage, paths are sequences of WTreeNodes too, and the row and expansion APIs can be used") {
            onUiThread {
                val tree = WTree()
                val root = WTreeNode("Root")
                val child = WTreeNode("Child")
                val grandchild = WTreeNode("Grandchild")
                child.add(grandchild)
                root.add(child)
                tree.addRootNode(root)
                tree.rowCount shouldBe 1
                tree.expandPath(TreePath(arrayOf<Any?>(root, child)))
                tree.rowCount shouldBe 3
                tree.getPathForRow(2) shouldBe TreePath(arrayOf<Any?>(root, child, grandchild))
                tree.getRowForPath(TreePath(arrayOf<Any?>(root, child))) shouldBe 1
                tree.collapseRow(0)
                tree.rowCount shouldBe 1
            }
        }

        test("children are also created on user expansion after it is shown (native IsExpanded), and paths can be looked up from row positions") {
            val s = Sample()
            val tree = onUiThreadGet { WTree(s.model) }
            UiTestHarness.attachAndAwaitLoaded(tree)
            try {
                onUiThread {
                    checkNotNull(tree.nativeNodeOf(s.path(s.fruit))).isExpanded = true
                    tree.rowTexts() shouldBe listOf("Food", "Fruit", "Apple", "Orange", "Vegetables", "Water")
                }
                // Row placement is decided after layout
                Reclamation.awaitWithGc { onUiThreadGet { tree.getPathForLocation(10, 10) } != null }.shouldBeTrue()
                onUiThread {
                    tree.getPathForLocation(10, 10) shouldBe TreePath(s.root)
                    tree.getRowForLocation(10, 10) shouldBe 0
                    tree.getPathForLocation(10, -5).shouldBeNull()
                    tree.getClosestPathForLocation(10, 100_000) shouldBe tree.getPathForRow(tree.rowCount - 1)
                    tree.scrollPathToVisible(s.path(s.vegetable, s.carrot, s.variety))
                    tree.isVisible(s.path(s.vegetable, s.carrot, s.variety)).shouldBeTrue()
                }
            } finally {
                UiTestHarness.detach(tree)
            }
        }

        test("showing a tree on which expandPath was called before it was shown does not crash, and it is shown expanded") {
            val s = Sample()
            val tree = onUiThreadGet { WTree(s.model).also { it.expandPath(s.path(s.fruit)) } }
            UiTestHarness.attachAndAwaitLoaded(tree)
            try {
                onUiThread {
                    tree.isExpanded(s.path(s.fruit)).shouldBeTrue()
                    tree.rowCount shouldBe 6
                }
            } finally {
                UiTestHarness.detach(tree)
            }
        }

        context("Swing compatibility") {
            test("setSelectionInterval selects the rows with both ends clamped to the row range, and add / removeSelectionInterval and removeSelectionRow(s) add and subtract") {
                val s = Sample()
                val steps = onUiThreadGet {
                    val tree = WTree(s.model)
                    tree.selectionModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
                    val rows = { tree.getSelectionRows()?.sorted().orEmpty() }
                    val result = mutableListOf<List<Int>>()
                    tree.setSelectionInterval(-3, 1)
                    result += rows()
                    tree.addSelectionInterval(10, 3)
                    result += rows()
                    tree.removeSelectionInterval(1, 0)
                    result += rows()
                    tree.addSelectionRows(intArrayOf(0, 2))
                    tree.removeSelectionRow(2)
                    result += rows()
                    tree.removeSelectionRows(intArrayOf(0, 99))
                    result += rows()
                    tree.setSelectionInterval(10, 20)
                    result += rows()
                    result
                }
                steps shouldBe listOf(listOf(0, 1), listOf(0, 1, 3), listOf(3), listOf(0, 3), listOf(3), emptyList())
            }

            test("getExpandedDescendants returns the visible expanded descendants in row order, excluding the parent itself, and null if the parent is not expanded") {
                val s = Sample()
                val results = onUiThreadGet {
                    val tree = WTree(s.model)
                    tree.expandPath(s.path(s.fruit))
                    tree.expandPath(s.path(s.vegetable, s.carrot))
                    val all = tree.getExpandedDescendants(TreePath(s.root))?.toList()
                    tree.collapsePath(s.path(s.vegetable))
                    listOf(
                        all,
                        tree.getExpandedDescendants(TreePath(s.root))?.toList(),
                        tree.getExpandedDescendants(s.path(s.fruit))?.toList(),
                        tree.getExpandedDescendants(s.path(s.vegetable)),
                        tree.getExpandedDescendants(null),
                    )
                }
                results shouldBe listOf(
                    listOf(s.path(s.fruit), s.path(s.vegetable), s.path(s.vegetable, s.carrot)),
                    listOf(s.path(s.fruit)),
                    emptyList<TreePath>(),
                    null,
                    null,
                )
            }

            test("the fireTree methods call listeners in the order added without changing the expansion state, and a vetoing ExpandVetoException reaches the caller") {
                val s = Sample()
                val calls = mutableListOf<String>()
                val vetoed = onUiThreadGet {
                    val tree = WTree(s.model)
                    tree.addTreeExpansionListener(object : TreeExpansionListener {
                        override fun treeExpanded(event: TreeExpansionEvent) {
                            calls += "expanded:${event.getPath()?.getLastPathComponent()}:${event.source === tree}"
                        }

                        override fun treeCollapsed(event: TreeExpansionEvent) {
                            calls += "collapsed:${event.getPath()?.getLastPathComponent()}"
                        }
                    })
                    tree.addTreeWillExpandListener(object : TreeWillExpandListener {
                        override fun treeWillExpand(event: TreeExpansionEvent) {
                            calls += "will-expand-1"
                            throw ExpandVetoException(event)
                        }

                        override fun treeWillCollapse(event: TreeExpansionEvent) {
                            calls += "will-collapse-1"
                        }
                    })
                    tree.addTreeWillExpandListener(object : TreeWillExpandListener {
                        override fun treeWillExpand(event: TreeExpansionEvent) {
                            calls += "will-expand-2"
                        }

                        override fun treeWillCollapse(event: TreeExpansionEvent) {
                            calls += "will-collapse-2"
                        }
                    })
                    tree.fireTreeExpanded(s.path(s.fruit))
                    tree.fireTreeCollapsed(s.path(s.fruit))
                    tree.fireTreeWillCollapse(s.path(s.fruit))
                    shouldThrow<ExpandVetoException> { tree.fireTreeWillExpand(s.path(s.fruit)) }
                    tree.isExpanded(s.path(s.fruit))
                }
                vetoed shouldBe false
                calls shouldBe listOf("expanded:Fruit:true", "collapsed:Fruit", "will-collapse-1", "will-collapse-2", "will-expand-1")
            }

            test("as with JTree(TreeNode, boolean), setting asksAllowsChildren to true keeps a folder node without children from being a leaf") {
                val folder = DefaultMutableTreeNode("Empty folder", true)
                val root = DefaultMutableTreeNode("Root").also { it.add(folder) }
                onUiThreadGet {
                    val asks = WTree(root, true)
                    val plain = WTree(root, false)
                    listOf(
                        (asks.model as DefaultTreeModel).asksAllowsChildren(),
                        asks.model?.isLeaf(folder),
                        plain.model?.isLeaf(folder),
                        asks.rowCount,
                    )
                } shouldBe listOf(true, false, true, 2)
            }

            test("getRowBounds, getPathBounds and getClosestRowForLocation answer with the positions of shown rows, and null / -1 for invisible rows and before it is shown") {
                val s = Sample()
                val tree = onUiThreadGet { WTree(s.model) }
                onUiThreadGet { listOf(tree.getRowBounds(0), tree.getClosestRowForLocation(0, 0)) } shouldBe listOf(null, -1)
                UiTestHarness.attachAndAwaitLoaded(tree)
                try {
                    Reclamation.awaitWithGc { onUiThreadGet { tree.getRowBounds(3) } != null }.shouldBeTrue()
                    onUiThread {
                        val first = checkNotNull(tree.getRowBounds(0))
                        val second = checkNotNull(tree.getRowBounds(1))
                        tree.getPathBounds(TreePath(s.root)) shouldBe first
                        (first.width > 0 && first.height > 0).shouldBeTrue()
                        (second.y >= first.y + first.height - 1).shouldBeTrue()
                        tree.getRowForLocation(second.x + 1, second.y + second.height / 2) shouldBe 1
                        tree.getClosestRowForLocation(second.x + 1, 100_000) shouldBe tree.rowCount - 1
                        tree.getPathBounds(s.path(s.fruit, s.apple)).shouldBeNull()
                        tree.getPathBounds(null).shouldBeNull()
                        tree.getRowBounds(-1).shouldBeNull()
                        tree.getRowBounds(tree.rowCount).shouldBeNull()
                    }
                } finally {
                    UiTestHarness.detach(tree)
                }
            }
        }
    }
}
