package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode
import com.appkitbox.winui4k.tree.DefaultTreeModel
import com.appkitbox.winui4k.tree.TreePath
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies WTree's cellRenderer ([WTreeCellRenderer], like TreeCellRenderer) with the content of rows actually shown.
 * The expected values are decided from the meaning of JTree's getTreeCellRendererComponent arguments (value is the last node of the path, selected is whether it is in the selection model,
 * expanded is whether the node is expanded, leaf is the model's isLeaf, and row is the position among visible rows).
 * The form seen from Java is in WTreeForJavaTest.
 */
@Tags("E2E")
class WTreeCellRendererTest : FunSpec() {
    /** One call of the renderer. */
    private data class Call(val value: String, val selected: Boolean, val expanded: Boolean, val leaf: Boolean, val row: Int)

    /** A renderer that records calls and draws nodes as the text "<node>". */
    private class RecordingRenderer : WTreeCellRenderer {
        val calls = ArrayList<Call>()
        val trees = ArrayList<WTree>()

        override fun getTreeCellRendererComponent(
            tree: WTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ): WComponent {
            calls += Call(value.toString(), selected, expanded, leaf, row)
            trees += tree
            return WLabel("<$value>")
        }

        /** The last call for node [value]. */
        fun last(value: String): Call? = calls.lastOrNull { it.value == value }
    }

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

    private fun hasText(tree: WTree, text: String): Boolean = tableTreeFindText(tree.dependencyObject, text)?.also { it.release() } != null

    /** Advances layout on [tree] until [condition] holds (up to 5 seconds). true if it holds. */
    private fun awaitOnUi(tree: WTree, condition: (WTree) -> Boolean): Boolean {
        repeat(100) {
            if (onUiThreadGet { tree.updateLayout().let { _ -> condition(tree) } }) return true
            Thread.sleep(50)
        }
        return false
    }

    /** Shows [tree], runs [block], and then removes it. */
    private fun <T> shown(tree: WTree, block: () -> T): T {
        UiTestHarness.attachAndAwaitLoaded(tree)
        try {
            return block()
        } finally {
            UiTestHarness.detach(tree)
        }
    }

    /** AutomationProperties.Name of the row container (TreeViewItem) of [path]. */
    private fun automationNameOf(tree: WTree, path: TreePath): String {
        val node = checkNotNull(tree.nativeNodeOf(path)) { "no node: $path" }
        val treeView2: ComPtr = tree.inspectable.queryInterface(XamlInterop.IID_ITreeView2)
        val container = try {
            checkNotNull(treeView2.getPtrOrNull(XamlInterop.ITreeView2_ContainerFromNode, node.inspectable.ptr)) { "no container: $path" }
        } finally {
            treeView2.release()
        }
        return try {
            Xaml.getAutomation(container, XamlInterop.IAutomationPropertiesStatics_GetName)
        } finally {
            container.release()
        }
    }

    init {
        context("Defaults") {
            test("cellRenderer is null by default, and nodes are shown as their toString() text") {
                val s = Sample()
                val tree = onUiThreadGet { WTree(s.model) }
                onUiThreadGet { tree.cellRenderer } shouldBe null
                shown(tree) { awaitOnUi(tree) { hasText(it, "Fruit") && hasText(it, "Water") } } shouldBe true
            }
        }

        context("Reading and writing") {
            test("setting cellRenderer draws the visible rows with the renderer's components, and the names become the node strings") {
                val s = Sample()
                val renderer = RecordingRenderer()
                val tree = onUiThreadGet { WTree(s.model).also { it.cellRenderer = renderer } }
                shown(tree) {
                    awaitOnUi(tree) { listOf("<Root>", "<Fruit>", "<Water>").all { text -> hasText(it, text) } } shouldBe true
                    onUiThreadGet {
                        listOf(
                            tree.cellRenderer === renderer,
                            hasText(tree, "Fruit"),
                            hasText(tree, "<Apple>"),
                            automationNameOf(tree, s.path(s.fruit)),
                            renderer.trees.all { it === tree },
                        )
                    }
                } shouldBe listOf(true, false, false, "Fruit", true)
            }

            test("the renderer receives the last node of the path and selected, expanded, leaf and row with the same meaning as JTree") {
                val s = Sample()
                val renderer = RecordingRenderer()
                val tree = onUiThreadGet { WTree(s.model).also { it.cellRenderer = renderer } }
                shown(tree) {
                    awaitOnUi(tree) { renderer.last("Water") != null } shouldBe true
                    listOf(renderer.last("Root"), renderer.last("Fruit"), renderer.last("Water"))
                } shouldBe listOf(
                    Call("Root", selected = false, expanded = true, leaf = false, row = 0),
                    Call("Fruit", selected = false, expanded = false, leaf = false, row = 1),
                    Call("Water", selected = false, expanded = false, leaf = true, row = 2),
                )
            }

            test("resetting cellRenderer to null returns to showing the node text") {
                val s = Sample()
                val tree = onUiThreadGet { WTree(s.model).also { it.cellRenderer = RecordingRenderer() } }
                shown(tree) {
                    awaitOnUi(tree) { hasText(it, "<Fruit>") } shouldBe true
                    onUiThreadGet { tree.cellRenderer = null }
                    awaitOnUi(tree) { hasText(it, "Fruit") && !hasText(it, "<Fruit>") }
                } shouldBe true
            }

            test("setting cellRenderer after showing also redraws the visible rows") {
                val s = Sample()
                val tree = onUiThreadGet { WTree(s.model) }
                shown(tree) {
                    awaitOnUi(tree) { hasText(it, "Fruit") } shouldBe true
                    onUiThreadGet { tree.cellRenderer = RecordingRenderer() }
                    awaitOnUi(tree) { hasText(it, "<Fruit>") && hasText(it, "<Water>") }
                } shouldBe true
            }

            test("when assembling nodes, value is a WTreeNode") {
                val node = onUiThreadGet { WTreeNode("Built") }
                val values = ArrayList<Any?>()
                val tree = onUiThreadGet {
                    WTree().also {
                        it.addRootNode(node)
                        it.cellRenderer = WTreeCellRenderer { _, value, _, _, _, _, _ ->
                            values += value
                            WLabel("<$value>")
                        }
                    }
                }
                shown(tree) { awaitOnUi(tree) { values.isNotEmpty() } } shouldBe true
                values.first() shouldBe node
            }
        }

        context("Events") {
            test("when the selection changes, the newly selected and deselected nodes are redrawn with their selection state") {
                val s = Sample()
                val renderer = RecordingRenderer()
                val tree = onUiThreadGet { WTree(s.model).also { it.cellRenderer = renderer } }
                shown(tree) {
                    awaitOnUi(tree) { renderer.last("Water") != null } shouldBe true
                    onUiThreadGet { tree.selectionPath = s.path(s.water) }
                    val selected = renderer.last("Water")?.selected
                    onUiThreadGet { tree.selectionPath = s.path(s.fruit) }
                    listOf(selected, renderer.last("Water")?.selected, renderer.last("Fruit")?.selected)
                } shouldBe listOf(true, false, true)
            }

            test("expanding redraws that node with expanded, draws the children that appear, and shifts row of the rows below") {
                val s = Sample()
                val renderer = RecordingRenderer()
                val tree = onUiThreadGet { WTree(s.model).also { it.cellRenderer = renderer } }
                shown(tree) {
                    awaitOnUi(tree) { renderer.last("Water") != null } shouldBe true
                    onUiThreadGet { tree.expandPath(s.path(s.fruit)) }
                    awaitOnUi(tree) { hasText(it, "<Apple>") } shouldBe true
                    onUiThreadGet { listOf(renderer.last("Fruit")?.expanded, renderer.last("Apple")?.row, renderer.last("Water")?.row) }
                } shouldBe listOf(true, 2, 3)
            }

            test("a change to a model node (nodeChanged) redraws that node") {
                val s = Sample()
                val renderer = RecordingRenderer()
                val tree = onUiThreadGet { WTree(s.model).also { it.cellRenderer = renderer } }
                shown(tree) {
                    awaitOnUi(tree) { hasText(it, "<Water>") } shouldBe true
                    onUiThreadGet {
                        s.water.setUserObject("Hot water")
                        s.model.nodeChanged(s.water)
                    }
                    awaitOnUi(tree) { hasText(it, "<Hot water>") && !hasText(it, "<Water>") }
                } shouldBe true
            }
        }

        context("Threads and reentrancy") {
            test("a renderer called from ContainerContentChanging (during layout) throws IllegalStateException when it tries to wait for a dialog to close") {
                val outcomes = ArrayList<String>()
                val renderer = WTreeCellRenderer { _, value, _, _, _, _, _ ->
                    outcomes += runCatching { ModalLoop.run { true } }.fold({ "ran" }, { it.javaClass.simpleName })
                    WLabel("<$value>")
                }
                val s = Sample()
                val tree = onUiThreadGet { WTree(s.model).also { it.cellRenderer = renderer } }
                shown(tree) {
                    awaitOnUi(tree) { hasText(it, "<Water>") } shouldBe true
                    outcomes.isNotEmpty() to outcomes.distinct()
                } shouldBe (true to listOf("IllegalStateException"))
            }
        }

        context("Swing compatibility") {
            test("WDefaultTreeCellRenderer creates labels with the convertValueToText text and the icons for leaves, expanded nodes and collapsed nodes") {
                val s = Sample()
                onUiThreadGet {
                    val tree = WTree(s.model)
                    val renderer = WDefaultTreeCellRenderer()
                    val noIcon = renderer.getTreeCellRendererComponent(tree, s.water, false, false, true, 2, false) as WLabel
                    renderer.leafIcon = Symbol.DOCUMENT
                    renderer.openIcon = Symbol.OPEN_FILE
                    renderer.closedIcon = Symbol.FOLDER
                    val leaf = renderer.getTreeCellRendererComponent(tree, s.water, false, false, true, 2, false) as WLabel
                    val open = renderer.getTreeCellRendererComponent(tree, s.root, false, true, false, 0, false) as WLabel
                    val closed = renderer.getTreeCellRendererComponent(tree, s.fruit, false, false, false, 1, false) as WLabel
                    listOf(noIcon.text, noIcon.icon, leaf.text, leaf.icon, open.icon, closed.icon, closed.text)
                } shouldBe listOf("Water", null, "Water", Symbol.DOCUMENT, Symbol.OPEN_FILE, Symbol.FOLDER, "Fruit")
            }

            test("overriding getText and configure of WDefaultTreeCellRenderer changes the text and decoration") {
                val s = Sample()
                val tree = onUiThreadGet {
                    WTree(s.model).also { tree ->
                        tree.cellRenderer = object : WDefaultTreeCellRenderer() {
                            override fun getText(tree: WTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): String =
                                "[$value]"

                            override fun configure(label: WLabel, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
                                label.accessibleDescription = "row $row"
                            }
                        }
                    }
                }
                shown(tree) {
                    awaitOnUi(tree) { hasText(it, "[Fruit]") && hasText(it, "[Water]") && !hasText(it, "Fruit") }
                } shouldBe true
            }
        }
    }
}
