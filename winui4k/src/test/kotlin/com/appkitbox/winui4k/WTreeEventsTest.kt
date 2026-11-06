package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode
import com.appkitbox.winui4k.tree.DefaultTreeModel
import com.appkitbox.winui4k.tree.DefaultTreeSelectionModel
import com.appkitbox.winui4k.tree.ExpandVetoException
import com.appkitbox.winui4k.tree.TreeExpansionEvent
import com.appkitbox.winui4k.tree.TreeExpansionListener
import com.appkitbox.winui4k.tree.TreePath
import com.appkitbox.winui4k.tree.TreeSelectionEvent
import com.appkitbox.winui4k.tree.TreeSelectionModel
import com.appkitbox.winui4k.tree.TreeWillExpandListener
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for WTree's selection (syncing TreeSelectionModel with the TreeView's SelectedNodes, TreeSelectionListener) and expansion listeners
 * (TreeExpansionListener / TreeWillExpandListener and ExpandVetoException). Expected values come from JTree's behavior.
 */
@Tags("E2E")
class WTreeEventsTest : FunSpec() {
    private class Sample {
        val root = DefaultMutableTreeNode("Root")
        val fruit = DefaultMutableTreeNode("Fruit")
        val apple = DefaultMutableTreeNode("Apple")
        val water = DefaultMutableTreeNode("Water")
        val model = DefaultTreeModel(root)

        init {
            root.add(fruit)
            fruit.add(apple)
            root.add(water)
        }

        fun path(vararg nodes: DefaultMutableTreeNode): TreePath = TreePath(arrayOf<Any?>(root, *nodes))
    }

    /** Records what the expansion listeners received as "kind:last node", and vetoes nodes listed in [veto]. */
    private class ExpansionRecorder : TreeExpansionListener, TreeWillExpandListener {
        val calls = mutableListOf<String>()
        val veto = mutableSetOf<String>()
        val sources = mutableListOf<Any>()

        private fun record(kind: String, event: TreeExpansionEvent) {
            sources += event.source
            calls += "$kind:${event.getPath()?.getLastPathComponent()}"
        }

        override fun treeExpanded(event: TreeExpansionEvent) = record("expanded", event)

        override fun treeCollapsed(event: TreeExpansionEvent) = record("collapsed", event)

        override fun treeWillExpand(event: TreeExpansionEvent) {
            record("willExpand", event)
            if ("expand:${event.getPath()?.getLastPathComponent()}" in veto) throw ExpandVetoException(event)
        }

        override fun treeWillCollapse(event: TreeExpansionEvent) {
            record("willCollapse", event)
            if ("collapse:${event.getPath()?.getLastPathComponent()}" in veto) throw ExpandVetoException(event, "Vetoed")
        }
    }

    /** The display texts of the TreeView's SelectedNodes (the native state). */
    private fun WTree.nativeSelectedTexts(): List<String> {
        val vector = inspectable.getPtr(XamlInterop.ITreeView_get_SelectedNodes)
        try {
            return (0 until vector.getInt(FoundationInterop.IVector_get_Size)).map { index ->
                val node = vector.getPtr(FoundationInterop.IVector_GetAt, index)
                try {
                    val content = checkNotNull(node.getPtrOrNull(XamlInterop.ITreeViewNode_get_Content))
                    try {
                        PropertyValues.unboxString(content).orEmpty()
                    } finally {
                        content.release()
                    }
                } finally {
                    node.release()
                }
            }
        } finally {
            vector.release()
        }
    }

    /** Sets the TreeView's SelectedNode directly, the same as a user clicking a node. */
    private fun WTree.selectNatively(node: WTreeNode?) {
        val treeView2: ComPtr = inspectable.queryInterface(XamlInterop.IID_ITreeView2)
        try {
            treeView2.call(XamlInterop.ITreeView2_put_SelectedNode, node?.inspectable?.ptr)
        } finally {
            treeView2.release()
        }
    }

    init {
        test("setSelectionPath is mirrored to the TreeView's SelectedNode, and the tree's listeners receive events with the tree as source") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                val events = mutableListOf<TreeSelectionEvent>()
                tree.addTreeSelectionListener { events += it }
                tree.selectionPath = s.path(s.fruit, s.apple)
                tree.nativeSelectedTexts() shouldBe listOf("Apple")
                tree.lastSelectedPathComponent shouldBe s.apple
                tree.isPathSelected(s.path(s.fruit, s.apple)).shouldBeTrue()
                events.map { listOf(it.source, it.getPath(), it.isAddedPath()) } shouldBe listOf(listOf(tree, s.path(s.fruit, s.apple), true))
                tree.clearSelection()
                tree.nativeSelectedTexts() shouldBe emptyList()
                tree.getSelectionPaths().shouldBeNull()
                tree.isSelectionEmpty.shouldBeTrue()
                events.size shouldBe 2
            }
        }

        test("the selection row APIs answer with visible rows, and setSelectionRow selects the path of the row") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.setSelectionRow(2)
                tree.selectionPath shouldBe s.path(s.water)
                tree.getSelectionRows()?.toList() shouldBe listOf(2)
                tree.leadSelectionRow shouldBe 2
                tree.isRowSelected(2).shouldBeTrue()
                tree.setSelectionRow(99)
                tree.isSelectionEmpty.shouldBeTrue()
            }
        }

        test("setting the selection model's mode to DISCONTIGUOUS makes the TreeView MULTIPLE, and multiple paths go into SelectedNodes") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.selectionModel.getSelectionMode() shouldBe TreeSelectionModel.SINGLE_TREE_SELECTION
                tree.selectionModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
                tree.selectionMode shouldBe TreeViewSelectionMode.MULTIPLE
                tree.setSelectionPaths(arrayOf(s.path(s.water), s.path(s.fruit)))
                tree.nativeSelectedTexts() shouldBe listOf("Water", "Fruit")
                tree.selectionCount shouldBe 2
                tree.selectionMode = TreeViewSelectionMode.SINGLE
                tree.selectionModel.getSelectionMode() shouldBe TreeSelectionModel.SINGLE_TREE_SELECTION
                tree.selectionMode = TreeViewSelectionMode.NONE
                tree.isSelectionEmpty.shouldBeTrue()
            }
        }

        test("the mode and selection of a model replaced with setSelectionModel are mirrored to the TreeView, and changes of the old model are not delivered") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                val old = tree.selectionModel
                val replacement = DefaultTreeSelectionModel()
                replacement.setSelectionPath(s.path(s.water))
                tree.selectionModel = replacement
                tree.selectionMode shouldBe TreeViewSelectionMode.MULTIPLE
                tree.nativeSelectedTexts() shouldBe listOf("Water")
                old.setSelectionPath(s.path(s.fruit))
                tree.nativeSelectedTexts() shouldBe listOf("Water")
                (replacement.getRowMapper() != null).shouldBeTrue()
                old.getRowMapper().shouldBeNull()
            }
        }

        test("nodes removed from the model and their descendants are deselected, and setModel clears the selection") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                tree.selectionModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
                tree.setSelectionPaths(arrayOf(s.path(s.fruit, s.apple), s.path(s.water)))
                s.model.removeNodeFromParent(s.fruit)
                tree.getSelectionPaths()?.toList() shouldBe listOf(s.path(s.water))
                tree.nativeSelectedTexts() shouldBe listOf("Water")
                tree.model = DefaultTreeModel(DefaultMutableTreeNode("Other"))
                tree.isSelectionEmpty.shouldBeTrue()
            }
        }

        test("in the node-building usage too, selectedNode and the path selection agree") {
            onUiThread {
                val tree = WTree()
                val root = WTreeNode("Root")
                val child = WTreeNode("Child")
                root.add(child)
                tree.addRootNode(root)
                tree.selectedNode = child
                tree.selectionPath shouldBe TreePath(arrayOf<Any?>(root, child))
                tree.selectionPath = TreePath(root)
                tree.selectedNode shouldBe root
            }
        }

        test("when the user selects in the TreeView after it is shown, the selection model and listeners receive it") {
            val s = Sample()
            val tree = onUiThreadGet { WTree(s.model) }
            UiTestHarness.attachAndAwaitLoaded(tree)
            try {
                onUiThread {
                    val events = mutableListOf<TreeSelectionEvent>()
                    tree.addTreeSelectionListener { events += it }
                    tree.selectNatively(tree.nativeNodeOf(s.path(s.water)))
                    tree.selectionPath shouldBe s.path(s.water)
                    events.map { it.getPath() } shouldBe listOf(s.path(s.water))
                    // Programmatic selection after it is shown is not reported twice through the TreeView notification either
                    tree.selectionPath = s.path(s.fruit)
                    tree.nativeSelectedTexts() shouldBe listOf("Fruit")
                    events.size shouldBe 2
                }
            } finally {
                UiTestHarness.detach(tree)
            }
        }

        test("expandPath and collapsePath call the expansion and collapse listeners after will, and do not expand or collapse when vetoed") {
            val s = Sample()
            onUiThread {
                val tree = WTree(s.model)
                val recorder = ExpansionRecorder()
                tree.addTreeWillExpandListener(recorder)
                tree.addTreeExpansionListener(recorder)
                tree.expandPath(s.path(s.fruit))
                tree.collapsePath(s.path(s.fruit))
                recorder.calls shouldBe listOf("willExpand:Fruit", "expanded:Fruit", "willCollapse:Fruit", "collapsed:Fruit")
                recorder.sources.all { it === tree }.shouldBeTrue()
                recorder.calls.clear()
                recorder.veto += "expand:Fruit"
                tree.collapsePath(s.path(s.fruit, s.apple)) // Once the expansion of the parent is vetoed, it goes no further
                tree.isExpanded(s.path(s.fruit)).shouldBeFalse()
                recorder.veto += "collapse:Root"
                tree.collapsePath(TreePath(s.root))
                tree.isExpanded(TreePath(s.root)).shouldBeTrue()
                recorder.calls shouldBe listOf("willExpand:Fruit", "willCollapse:Root")
            }
        }

        test("user expansion after it is shown calls will and the expansion listener once each, and a veto collapses it again (without a collapse notification)") {
            val s = Sample()
            val tree = onUiThreadGet { WTree(s.model) }
            val recorder = ExpansionRecorder()
            onUiThread {
                tree.addTreeWillExpandListener(recorder)
                tree.addTreeExpansionListener(recorder)
            }
            UiTestHarness.attachAndAwaitLoaded(tree)
            try {
                onUiThread {
                    val fruit = checkNotNull(tree.nativeNodeOf(s.path(s.fruit)))
                    fruit.isExpanded = true
                    tree.rowCount shouldBe 4
                    fruit.isExpanded = false
                    recorder.calls shouldBe listOf("willExpand:Fruit", "expanded:Fruit", "collapsed:Fruit")
                    recorder.calls.clear()
                    recorder.veto += "expand:Fruit"
                    fruit.isExpanded = true
                    fruit.isExpanded.shouldBeFalse()
                    tree.isExpanded(s.path(s.fruit)).shouldBeFalse()
                    tree.rowCount shouldBe 3
                    recorder.calls shouldBe listOf("willExpand:Fruit")
                }
            } finally {
                UiTestHarness.detach(tree)
            }
        }
    }
}
