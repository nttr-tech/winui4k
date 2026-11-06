package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Tests for WTree's node API (the usage that builds WTreeNodes directly). */
class WTreeTest : FunSpec() {
    init {
        test("expand / collapse / selectAll before it is shown do not crash and affect node expansion and selection") {
            val result = onUiThreadGet {
                val tree = WTree()
                val root = WTreeNode("Root")
                val child = WTreeNode("Child")
                root.add(child)
                tree.addRootNode(root)
                tree.expand(root)
                val expanded = root.isExpanded
                tree.collapse(root)
                val collapsed = !root.isExpanded
                tree.selectionMode = TreeViewSelectionMode.MULTIPLE
                tree.selectAll()
                Triple(expanded, collapsed, tree.selectedNodes.map { it.text })
            }
            result shouldBe Triple(true, true, listOf("Root", "Child"))
        }

        test("WTreeNode has text, adding and removing children, childCount and hasChildren, and the depth of a node not yet added is -1") {
            onUiThreadGet {
                val root = WTreeNode("Root")
                val a = WTreeNode("A")
                val b = WTreeNode("B")
                val before = Triple(root.text, root.hasChildren, root.depth)
                root.add(a)
                root.add(b)
                val added = Triple(root.childCount, root.hasChildren, root.children.map { it.text })
                root.remove(a)
                root.remove(a) // Removing a node that is not a child does nothing
                val removedOne = root.children.map { it.text }
                b.text = "B2"
                root.removeAllChildren()
                listOf(before, added, removedOne, b.text, root.childCount, root.hasChildren)
            } shouldBe listOf(Triple("Root", false, -1), Triple(2, true, listOf("A", "B")), listOf("B"), "B2", 0, false)
        }

        test("addRootNode and removeRootNode are reflected in rootNodes, and the depth of nodes put in the tree is 0 for roots and 1 for children") {
            onUiThreadGet {
                val tree = WTree()
                val first = WTreeNode("One")
                val second = WTreeNode("Two")
                val child = WTreeNode("Child")
                first.add(child)
                tree.addRootNode(first)
                tree.addRootNode(second)
                val added = tree.rootNodes.map { it.text }
                val depths = listOf(first.depth, child.depth)
                tree.removeRootNode(first)
                tree.removeRootNode(first) // Removing a node that is not a root does nothing
                listOf(added, depths, tree.rootNodes.map { it.text })
            } shouldBe listOf(listOf("One", "Two"), listOf(0, 1), listOf("Two"))
        }

        test("selectedNode returns the assigned node, and assigning null goes back to unselected") {
            onUiThreadGet {
                val tree = WTree()
                val root = WTreeNode("Root")
                val child = WTreeNode("Child")
                root.add(child)
                tree.addRootNode(root)
                val initial = tree.selectedNode
                tree.selectedNode = child
                val selected = tree.selectedNode?.text
                tree.selectedNode = null
                Triple(initial, selected, tree.selectedNode)
            } shouldBe Triple(null, "Child", null)
        }

        test("selectAll while selectionMode is SINGLE selects nothing") {
            onUiThreadGet {
                val tree = WTree()
                val root = WTreeNode("Root")
                root.add(WTreeNode("Child"))
                tree.addRootNode(root)
                tree.selectAll()
                tree.selectedNodes.size
            } shouldBe 0
        }
    }
}
