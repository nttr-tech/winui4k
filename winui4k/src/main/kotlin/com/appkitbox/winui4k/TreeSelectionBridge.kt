package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.tree.DefaultTreeSelectionModel
import com.appkitbox.winui4k.tree.RowMapper
import com.appkitbox.winui4k.tree.TreePath
import com.appkitbox.winui4k.tree.TreeSelectionEvent
import com.appkitbox.winui4k.tree.TreeSelectionListener
import com.appkitbox.winui4k.tree.TreeSelectionModel
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Keeps the selection model ([TreeSelectionModel]) of a [WTree] and the TreeView selection (SelectedNodes) in sync both ways.
 *
 * - Model change → TreeView: rewrites SelectedNode for SINGLE and SelectedNodes for MULTIPLE (creating children first for paths whose children are not created yet).
 * - TreeView SelectionChanged (user operation) → model: converts SelectedNodes to paths and sets them on the model
 *   (in MULTIPLE, the selection of paths without a native node (such as a hidden root) is kept).
 * - Model listeners are notified with the model as source, and the tree's listeners ([addListener]) get a copy with the tree as source, in the order added.
 *
 * Only something that looks up the tree weakly ([AnchoredSelectionHandler]) is registered on the model, so the tree can be collected even if the user keeps holding the model.
 */
internal class TreeSelectionBridge(
    private val tree: WTree,
    anchor: ListenerAnchor<WTree>,
    private val binding: () -> TreeBinding,
    private val treeView: ComPtr,
) {
    private val handler = AnchoredSelectionHandler(anchor)

    private val listeners = CopyOnWriteArrayList<TreeSelectionListener>()

    /** While greater than 0, the model is being written to the TreeView, so notifications from the TreeView are ignored. */
    private var pushing = 0

    /** While writing from the TreeView to the model, model notifications are not written back to the TreeView. */
    private var pulling = false

    var model: TreeSelectionModel = DefaultTreeSelectionModel().also { it.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION) }
        set(value) {
            detach(field)
            field = value
            attach(value)
            applyModeToNative()
            pushToNative()
        }

    init {
        attach(model)
    }

    fun addListener(listener: TreeSelectionListener) {
        listeners += listener
    }

    fun removeListener(listener: TreeSelectionListener) {
        listeners.remove(listener)
    }

    fun listeners(): Array<TreeSelectionListener> = listeners.toTypedArray()

    private fun attach(target: TreeSelectionModel) {
        target.setRowMapper(handler)
        target.addTreeSelectionListener(handler)
        (target as? DefaultTreeSelectionModel)?.modeObservers?.add(handler.modeObserver)
    }

    private fun detach(target: TreeSelectionModel) {
        target.removeTreeSelectionListener(handler)
        if (target.getRowMapper() === handler) target.setRowMapper(null)
        (target as? DefaultTreeSelectionModel)?.modeObservers?.remove(handler.modeObserver)
    }

    /** The row of a path (-1 if not visible). */
    fun rowsFor(paths: Array<out TreePath?>?): IntArray? = paths?.map { tree.getRowForPath(it) }?.toIntArray()

    /** Matches the TreeView selection mode to the model's (SINGLE → SINGLE, otherwise → MULTIPLE). */
    fun applyModeToNative() {
        val native = if (model.getSelectionMode() == TreeSelectionModel.SINGLE_TREE_SELECTION) {
            TreeViewSelectionMode.SINGLE
        } else {
            TreeViewSelectionMode.MULTIPLE
        }
        treeView.call(XamlInterop.ITreeView_put_SelectionMode, native.native)
    }

    /** When the TreeView selection mode is changed with [WTree.selectionMode]: matches the model's mode (NONE clears the selection). */
    fun onNativeModeSet(mode: TreeViewSelectionMode) {
        when (mode) {
            TreeViewSelectionMode.SINGLE -> model.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION)
            TreeViewSelectionMode.MULTIPLE -> if (model.getSelectionMode() == TreeSelectionModel.SINGLE_TREE_SELECTION) {
                model.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
            }
            TreeViewSelectionMode.NONE -> model.clearSelection()
        }
    }

    fun onModelSelectionChanged(e: TreeSelectionEvent) {
        if (!pulling) pushToNative()
        val event = e.cloneWithSource(tree) as TreeSelectionEvent
        listeners.forEach { it.valueChanged(event) }
    }

    /** Writes the model's selection to the TreeView. */
    fun pushToNative() {
        pushing++
        try {
            val mode = TreeViewSelectionMode.of(treeView.getInt(XamlInterop.ITreeView_get_SelectionMode))
            val paths = model.getSelectionPaths().orEmpty()
            val nodes = when (mode) {
                TreeViewSelectionMode.NONE -> return
                TreeViewSelectionMode.SINGLE -> paths.take(1)
                TreeViewSelectionMode.MULTIPLE -> paths.toList()
            }.mapNotNull { binding().nodeOf(it, true) }
            val vector = treeView.getPtr(XamlInterop.ITreeView_get_SelectedNodes)
            try {
                if (nativeNodes(vector) == nodes) return
                vector.call(FoundationInterop.IVector_Clear)
                for (node in nodes) vector.call(FoundationInterop.IVector_Append, node.inspectable.ptr)
            } finally {
                vector.release()
            }
        } finally {
            pushing--
        }
    }

    /** While the TreeView selection is rebuilt by [block], notifications from the TreeView are not copied to the model (rewrite it with [pushToNative] afterwards). */
    fun <T> quietly(block: () -> T): T {
        pushing++
        try {
            return block()
        } finally {
            pushing--
        }
    }

    /** TreeView SelectionChanged: mirrors the user's selection to the model. */
    fun onNativeSelectionChanged() {
        if (pushing > 0) return
        val vector = treeView.getPtr(XamlInterop.ITreeView_get_SelectedNodes)
        val selected = try {
            nativeNodes(vector).mapNotNull { binding().pathOf(it) }
        } finally {
            vector.release()
        }
        pulling = true
        try {
            pull(selected)
        } finally {
            pulling = false
        }
    }

    private fun pull(selected: List<TreePath>) {
        val current = model.getSelectionPaths().orEmpty().toList()
        if (model.getSelectionMode() == TreeSelectionModel.SINGLE_TREE_SELECTION) {
            val path = selected.firstOrNull()
            when {
                path == null -> model.clearSelection()
                current != listOf(path) -> model.setSelectionPath(path)
            }
            return
        }
        if (current.toSet() == selected.toSet()) return
        val kept = current.filter { it in selected || binding().nodeOf(it, false) == null }
        model.setSelectionPaths((kept + selected.filter { it !in current }).toTypedArray())
    }

    private fun nativeNodes(vector: ComPtr): List<WTreeNode> =
        (0 until vector.getInt(FoundationInterop.IVector_get_Size)).mapNotNull { index ->
            val node = vector.getPtr(FoundationInterop.IVector_GetAt, index)
            try {
                binding().resolve(node)
            } finally {
                node.release()
            }
        }

    /** What is registered on the selection model. It looks up the tree weakly (so that a shared model does not hold the tree strongly). */
    private class AnchoredSelectionHandler(private val anchor: ListenerAnchor<WTree>) : TreeSelectionListener, RowMapper {
        val modeObserver: () -> Unit = { anchor.get()?.selectionBridge?.applyModeToNative() }

        override fun valueChanged(e: TreeSelectionEvent) {
            anchor.get()?.selectionBridge?.onModelSelectionChanged(e)
        }

        override fun getRowsForPaths(path: Array<out TreePath?>?): IntArray? =
            anchor.get()?.selectionBridge?.rowsFor(path) ?: path?.let { IntArray(it.size) { -1 } }
    }
}
