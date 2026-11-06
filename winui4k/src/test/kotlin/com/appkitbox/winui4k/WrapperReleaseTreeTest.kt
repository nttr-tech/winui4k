package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.tree.DefaultMutableTreeNode
import com.appkitbox.winui4k.tree.DefaultTreeModel
import com.appkitbox.winui4k.tree.DefaultTreeSelectionModel
import com.appkitbox.winui4k.tree.TreeExpansionEvent
import com.appkitbox.winui4k.tree.TreeExpansionListener
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference

/**
 * Tests that a WTree mirroring a TreeModel is not captured strongly by resident native handlers or a shared model and is collected after removal
 * (continuation of [WrapperReleaseTest], split off to avoid detekt's LargeClass). Collection is awaited while prompting GC ([Reclamation]).
 */
class WrapperReleaseTreeTest : FunSpec() {
    /** Puts the tree made by [create] (created on the UI thread) in a window, removes it, and returns only a weak reference. */
    private fun shownAndHidden(create: () -> WTree): WeakReference<WTree> {
        val tree = onUiThreadGet(create)
        UiTestHarness.attachAndAwaitLoaded(tree)
        UiTestHarness.detach(tree)
        Reclamation.awaitWithGc { !onUiThreadGet { tree.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handlers to finish in the same cycle
        return WeakReference(tree)
    }

    init {
        test("a WTree never shown is collected whether it builds nodes or mirrors a model (even if the user keeps holding the model)") {
            val model = onUiThreadGet { DefaultTreeModel(DefaultMutableTreeNode("Root").also { it.add(DefaultMutableTreeNode("Child")) }) }
            val references = onUiThreadGet {
                val plain = WTree()
                val nodes = WTree().also { tree ->
                    tree.addRootNode(WTreeNode("Root").also { it.add(WTreeNode("Child")) })
                    tree.selectionMode = TreeViewSelectionMode.MULTIPLE
                    tree.selectAll()
                }
                val modelled = WTree(model).also { it.setSelectionRow(0) }
                listOf(WeakReference(plain), WeakReference(nodes), WeakReference(modelled))
            }
            references.forEach { Reclamation.awaitCollected(it).shouldBeTrue() }
            onUiThreadGet { model.getChildCount(checkNotNull(model.getRoot())) } shouldBe 1
        }

        test("a WTree mirroring a model the user keeps holding is also collected after being put in a window and removed, and the model can still be used") {
            val root = DefaultMutableTreeNode("Root")
            val model = onUiThreadGet { DefaultTreeModel(root.also { it.add(DefaultMutableTreeNode("Child")) }) }
            val reference = shownAndHidden { WTree(model).also { it.expandRow(1) } }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            onUiThreadGet {
                model.insertNodeInto(DefaultMutableTreeNode("Later"), root, 0)
                root.getChildCount()
            } shouldBe 2
        }

        test("a WTree with a shared selection model and expansion and selection listeners is also collected after being put in a window and removed while the model is kept") {
            val selection = onUiThreadGet { DefaultTreeSelectionModel() }
            val calls = mutableListOf<String>()
            val reference = shownAndHidden {
                WTree(DefaultMutableTreeNode("Root").also { it.add(DefaultMutableTreeNode("Child")) }).also { tree ->
                    tree.selectionModel = selection
                    tree.addTreeSelectionListener { calls += "selection" }
                    tree.addTreeExpansionListener(object : TreeExpansionListener {
                        override fun treeExpanded(event: TreeExpansionEvent) {
                            calls += "expanded"
                        }

                        override fun treeCollapsed(event: TreeExpansionEvent) {
                            calls += "collapsed"
                        }
                    })
                    tree.setSelectionRow(1)
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            onUiThreadGet { selection.getSelectionCount() } shouldBe 1
        }
    }
}
