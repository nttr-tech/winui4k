package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode
import com.appkitbox.winui4k.tree.DefaultTreeModel
import com.appkitbox.winui4k.tree.ExpandVetoException
import com.appkitbox.winui4k.tree.TreeExpansionEvent
import com.appkitbox.winui4k.tree.TreeExpansionListener
import com.appkitbox.winui4k.tree.TreePath
import com.appkitbox.winui4k.tree.TreeWillExpandListener
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for WTree's JTree-style display features (visibleRowCount and expandsSelectedPaths). The expected values follow JTree's behavior:
 * - visibleRowCount defaults to 20. Setting it sets the height to rows × the estimated height of one row (TreeViewItemMinHeight 28 + top and bottom outer margins 2 + 2 = 32)
 *   + padding and border (the same approximation as WList)
 * - expandsSelectedPaths defaults to true, and every time the selection changes it expands the parents of the selected paths to make them visible
 *   (BasicTreeUI's selection handler calls makeVisible; it does not expand if there is a leaf along the path). If false, the parents stay closed
 */
@Tags("E2E")
class WTreeDisplayTest : FunSpec() {
    private class Sample {
        val root = DefaultMutableTreeNode("Root")
        val fruit = DefaultMutableTreeNode("Fruit")
        val apple = DefaultMutableTreeNode("Apple")
        val seed = DefaultMutableTreeNode("Seed")
        val water = DefaultMutableTreeNode("Water")
        val model = DefaultTreeModel(root)

        init {
            root.add(fruit)
            fruit.add(apple)
            apple.add(seed)
            root.add(water)
        }

        fun path(vararg nodes: DefaultMutableTreeNode): TreePath = TreePath(arrayOf<Any?>(root, *nodes))
    }

    /** "kind:last node" received by the expansion listeners. */
    private class ExpansionRecorder : TreeExpansionListener {
        val calls = mutableListOf<String>()

        override fun treeExpanded(event: TreeExpansionEvent) {
            calls += "expanded:${event.getPath()?.getLastPathComponent()}"
        }

        override fun treeCollapsed(event: TreeExpansionEvent) {
            calls += "collapsed:${event.getPath()?.getLastPathComponent()}"
        }
    }

    /** The sum of the top and bottom padding (Padding) and border (BorderThickness) of [tree]. */
    private fun verticalPaddingAndBorder(tree: WTree): Double {
        val padding = XamlStructs.getThickness(tree.control, XamlInterop.IControl_get_Padding)
        val border = XamlStructs.getThickness(tree.control, XamlInterop.IControl_get_BorderThickness)
        return padding[1] + padding[3] + border[1] + border[3]
    }

    init {
        context("Defaults") {
            test("visibleRowCount defaults to 20, the same as JTree, and does not affect the height until set") {
                onUiThreadGet { WTree(Sample().model).let { it.visibleRowCount to it.height.isNaN() } } shouldBe (20 to true)
            }

            test("expandsSelectedPaths defaults to true, the same as JTree") {
                onUiThreadGet { WTree(Sample().model).expandsSelectedPaths } shouldBe true
            }
        }

        context("Reading and writing") {
            test("setting visibleRowCount makes the height rows × 32 + padding and border, and negative values become 0 rows") {
                onUiThreadGet {
                    val tree = WTree(Sample().model)
                    val extra = verticalPaddingAndBorder(tree)
                    tree.visibleRowCount = 5
                    val five = tree.height - extra
                    tree.visibleRowCount = -3
                    listOf(five, tree.height - extra, tree.visibleRowCount.toDouble())
                } shouldBe listOf(160.0, 0.0, 0.0)
            }

            test("if the user specified a height, setting visibleRowCount does not change the height") {
                onUiThreadGet {
                    WTree(Sample().model).also {
                        it.height = 77.0
                        it.visibleRowCount = 5
                    }.height
                } shouldBe 77.0
            }

            test("the height of one row of a shown tree is the same as the estimate of 32") {
                val tree = onUiThreadGet { WTree(Sample().model) }
                UiTestHarness.attachAndAwaitLoaded(tree)
                try {
                    onUiThreadGet {
                        tree.updateLayout()
                        listOf(tree.getRowBounds(0)?.height, tree.getRowBounds(1)?.height)
                    } shouldBe listOf(32, 32)
                } finally {
                    UiTestHarness.detach(tree)
                }
            }

            test("if expandsSelectedPaths is true, selecting a path under a closed parent expands the parent and makes the path visible") {
                val s = Sample()
                onUiThreadGet {
                    val tree = WTree(s.model)
                    val recorder = ExpansionRecorder()
                    tree.addTreeExpansionListener(recorder)
                    tree.selectionPath = s.path(s.fruit, s.apple, s.seed)
                    listOf(
                        tree.isExpanded(s.path(s.fruit)),
                        tree.isExpanded(s.path(s.fruit, s.apple)),
                        tree.isVisible(s.path(s.fruit, s.apple, s.seed)),
                        tree.getRowForPath(s.path(s.fruit, s.apple, s.seed)),
                        recorder.calls,
                    )
                } shouldBe listOf(true, true, true, 3, listOf("expanded:Fruit", "expanded:Apple"))
            }

            test("if expandsSelectedPaths is false, the parents stay closed after selecting and the path remains in the selection model") {
                val s = Sample()
                onUiThreadGet {
                    val tree = WTree(s.model)
                    tree.expandsSelectedPaths = false
                    tree.selectionPath = s.path(s.fruit, s.apple)
                    listOf(tree.expandsSelectedPaths, tree.isExpanded(s.path(s.fruit)), tree.getRowForPath(s.path(s.fruit, s.apple)), tree.selectionPath)
                } shouldBe listOf(false, false, -1, s.path(s.fruit, s.apple))
            }
        }

        context("Events") {
            test("the parents expand even when the selection model is changed directly or a path is added with addSelectionPath") {
                val s = Sample()
                onUiThreadGet {
                    val tree = WTree(s.model)
                    tree.selectionModel.setSelectionPath(s.path(s.fruit, s.apple))
                    val direct = tree.isExpanded(s.path(s.fruit))
                    tree.collapsePath(s.path(s.fruit))
                    tree.selectionModel.setSelectionMode(com.appkitbox.winui4k.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
                    tree.addSelectionPath(s.path(s.water))
                    listOf(direct, tree.isExpanded(s.path(s.fruit)))
                } shouldBe listOf(true, true) // Every time the selection changes, the parents of all selected paths are expanded (the same as Swing)
            }

            test("setting false back to true does not expand the parents of the existing selection until the selection changes next") {
                val s = Sample()
                onUiThreadGet {
                    val tree = WTree(s.model)
                    tree.expandsSelectedPaths = false
                    tree.selectionPath = s.path(s.fruit, s.apple)
                    tree.expandsSelectedPaths = true
                    val before = tree.isExpanded(s.path(s.fruit))
                    tree.selectionPath = s.path(s.water)
                    tree.selectionPath = s.path(s.fruit, s.apple)
                    listOf(before, tree.isExpanded(s.path(s.fruit)))
                } shouldBe listOf(false, true)
            }
        }

        context("Boundaries and exceptions") {
            test("selecting a path with a leaf along it (not in the tree) expands nothing") {
                val s = Sample()
                onUiThreadGet {
                    val tree = WTree(s.model)
                    val recorder = ExpansionRecorder()
                    tree.addTreeExpansionListener(recorder)
                    tree.selectionPath = s.path(s.water, DefaultMutableTreeNode("Missing node"))
                    recorder.calls
                } shouldBe emptyList()
            }

            test("when a TreeWillExpandListener vetoes the expansion, the parents stay closed and the selection remains") {
                val s = Sample()
                onUiThreadGet {
                    val tree = WTree(s.model)
                    tree.addTreeWillExpandListener(
                        object : TreeWillExpandListener {
                            override fun treeWillExpand(event: TreeExpansionEvent): Unit = throw ExpandVetoException(event)

                            override fun treeWillCollapse(event: TreeExpansionEvent) = Unit
                        },
                    )
                    tree.selectionPath = s.path(s.fruit, s.apple)
                    tree.isExpanded(s.path(s.fruit)) to tree.selectionPath
                } shouldBe (false to s.path(s.fruit, s.apple))
            }
        }
    }
}
