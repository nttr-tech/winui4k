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
    }
}
