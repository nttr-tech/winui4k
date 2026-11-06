package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.tree.TreeModel
import com.appkitbox.winui4k.tree.TreeModelEvent
import com.appkitbox.winui4k.tree.TreePath
import java.util.IdentityHashMap

/**
 * The mapping between the native nodes ([WTreeNode]) shown by a [WTree] and the paths ([TreePath]) seen by the user.
 * There is the usage that builds nodes directly ([NodeTreeBinding]) and the usage that mirrors a [TreeModel] ([ModelTreeBinding]).
 */
internal sealed class TreeBinding {
    /** The nodes placed in RootNodes. */
    abstract val topNodes: List<WTreeNode>

    /** The path to [node]. null if it is not a node of this tree. */
    abstract fun pathOf(node: WTreeNode): TreePath?

    /** The node that [path] points to. If [realize], creates intermediate children while walking. null if not found (also for a hidden root). */
    abstract fun nodeOf(path: TreePath, realize: Boolean): WTreeNode?

    /** true if [path] is a leaf (cannot be expanded). */
    abstract fun isLeaf(path: TreePath): Boolean

    /** true if [path] is a hidden root (the root of a model with rootVisible = false). It is treated as always expanded. */
    open fun isHiddenRoot(path: TreePath): Boolean = false

    /** Looks up a node from a native TreeViewNode pointer. */
    abstract fun resolve(node: ComPtr): WTreeNode?

    /** Creates the children of [node] if not created yet. */
    open fun realize(node: WTreeNode) = Unit

    /** The nodes of the displayed rows (top to bottom, walking only the children of expanded nodes). */
    fun rows(): List<WTreeNode> {
        val result = mutableListOf<WTreeNode>()
        fun visit(node: WTreeNode) {
            result += node
            if (node.isExpanded) node.children.forEach(::visit)
        }
        topNodes.forEach(::visit)
        return result
    }
}

/** The usage that builds nodes with [WTree.addRootNode]. Path elements are [WTreeNode]s, and the root is one of RootNodes. */
internal class NodeTreeBinding(private val roots: List<WTreeNode>) : TreeBinding() {
    override val topNodes: List<WTreeNode> get() = roots

    override fun pathOf(node: WTreeNode): TreePath? {
        fun search(current: WTreeNode, parent: TreePath?): TreePath? {
            val path = parent?.pathByAddingChild(current) ?: TreePath(current)
            if (current === node) return path
            return current.children.firstNotNullOfOrNull { search(it, path) }
        }
        return roots.firstNotNullOfOrNull { search(it, null) }
    }

    override fun nodeOf(path: TreePath, realize: Boolean): WTreeNode? {
        val node = path.getLastPathComponent() as? WTreeNode ?: return null
        return node.takeIf { pathOf(it) == path }
    }

    override fun isLeaf(path: TreePath): Boolean = (path.getLastPathComponent() as? WTreeNode)?.childCount == 0

    override fun resolve(node: ComPtr): WTreeNode? {
        val target = node.queryInterface(KComObject.IID_IUNKNOWN)
        try {
            return roots.asSequence().flatMap { it.selfAndDescendants() }.firstOrNull { it.identity == target.ptr.address }
        } finally {
            target.release()
        }
    }
}

/**
 * The usage that mirrors a [TreeModel] to native nodes. Children are not created until expanded (only the expand mark is shown with HasUnrealizedChildren).
 * Path elements are the model's nodes, starting from the root. If [rootVisible] is false, the root's children are placed in RootNodes and the root itself is not shown.
 */
internal class ModelTreeBinding(
    val model: TreeModel,
    val rootVisible: Boolean,
    private val rootVector: ComPtr,
) : TreeBinding() {
    /** A mirror of one model node. [native] is null for a hidden root, and [children] is null until the children are created. */
    private class Entry(var value: Any, val parent: Entry?, val native: WTreeNode?) {
        var children: MutableList<Entry>? = null
    }

    private var root: Entry? = null

    private val byNative = IdentityHashMap<WTreeNode, Entry>()

    private val byIdentity = HashMap<Long, Entry>()

    override val topNodes: List<WTreeNode>
        get() {
            val root = root ?: return emptyList()
            return if (root.native != null) listOf(root.native) else root.children.orEmpty().mapNotNull { it.native }
        }

    init {
        build()
    }

    /** Rebuilds from the model's root. As in Swing, the root starts expanded. */
    private fun build() {
        val value = model.getRoot() ?: return
        val entry = Entry(value, null, if (rootVisible) createNative(value) else null)
        root = entry
        if (entry.native != null) {
            register(entry)
            rootVector.call(FoundationInterop.IVector_Append, entry.native.inspectable.ptr)
            if (!model.isLeaf(value)) {
                realize(entry)
                entry.native.isExpanded = true
            }
        } else {
            realize(entry)
        }
    }

    /** Empties RootNodes and discards the mirror. */
    fun clear() {
        rootVector.call(FoundationInterop.IVector_Clear)
        root = null
        byNative.clear()
        byIdentity.clear()
    }

    private fun createNative(value: Any): WTreeNode = WTreeNode(value.toString()).also {
        it.hasUnrealizedChildren = !model.isLeaf(value)
    }

    private fun register(entry: Entry) {
        val native = entry.native ?: return
        byNative[native] = entry
        byIdentity[native.identity] = entry
    }

    private fun unregister(entry: Entry) {
        entry.children?.forEach(::unregister)
        val native = entry.native ?: return
        byNative.remove(native)
        byIdentity.remove(native.identity)
    }

    override fun realize(node: WTreeNode) {
        byNative[node]?.let(::realize)
    }

    private fun realize(entry: Entry) {
        if (entry.children != null) return
        entry.children = mutableListOf()
        for (index in 0 until model.getChildCount(entry.value)) {
            insertChild(entry, index, model.getChild(entry.value, index) ?: continue)
        }
        entry.native?.hasUnrealizedChildren = false
    }

    private fun insertChild(parent: Entry, index: Int, value: Any) {
        val children = parent.children ?: return
        val child = Entry(value, parent, createNative(value))
        val position = index.coerceIn(0, children.size)
        children.add(position, child)
        register(child)
        val native = checkNotNull(child.native)
        if (parent.native == null) {
            rootVector.call(FoundationInterop.IVector_InsertAt, position, native.inspectable.ptr)
        } else {
            parent.native.insertAt(position, native)
        }
    }

    private fun removeChild(parent: Entry, index: Int) {
        val children = parent.children ?: return
        if (index !in children.indices) return
        unregister(children.removeAt(index))
        if (parent.native == null) rootVector.call(FoundationInterop.IVector_RemoveAt, index) else parent.native.removeAt(index)
    }

    private fun pathOf(entry: Entry): TreePath = entry.parent?.let { pathOf(it).pathByAddingChild(entry.value) } ?: TreePath(entry.value)

    override fun pathOf(node: WTreeNode): TreePath? = byNative[node]?.let(::pathOf)

    /** The mirror of [path]. Unless [realize], null beyond a node whose children are not created yet. */
    private fun entryOf(path: TreePath?, realize: Boolean): Entry? {
        path ?: return null
        val parent = path.getParentPath() ?: return root?.takeIf { it.value == path.getLastPathComponent() }
        val parentEntry = entryOf(parent, realize) ?: return null
        if (realize) realize(parentEntry)
        return parentEntry.children?.firstOrNull { it.value == path.getLastPathComponent() }
    }

    override fun nodeOf(path: TreePath, realize: Boolean): WTreeNode? = entryOf(path, realize)?.native

    override fun isLeaf(path: TreePath): Boolean = model.isLeaf(path.getLastPathComponent())

    override fun isHiddenRoot(path: TreePath): Boolean = !rootVisible && path.getPathCount() == 1 && root?.value == path.getLastPathComponent()

    override fun resolve(node: ComPtr): WTreeNode? {
        val target = node.queryInterface(KComObject.IID_IUNKNOWN)
        try {
            return byIdentity[target.ptr.address]?.native
        } finally {
            target.release()
        }
    }

    // ---- Apply TreeModelListener notifications to the mirror ----

    /** A node's value changed: re-read the display text. If there are no child indices, the path's node itself (the root) changed. */
    fun nodesChanged(e: TreeModelEvent) {
        val parent = entryOf(e.getTreePath(), false) ?: return
        val indices = e.getChildIndices()
        if (indices == null) {
            parent.native?.text = parent.value.toString()
            return
        }
        val children = parent.children ?: return
        for (index in indices) {
            val child = children.getOrNull(index) ?: continue
            model.getChild(parent.value, index)?.let { child.value = it }
            child.native?.text = child.value.toString()
        }
    }

    /** Nodes were inserted: insert them at the same positions if the parent's children are created, otherwise only update the expand mark. */
    fun nodesInserted(e: TreeModelEvent) {
        val parent = entryOf(e.getTreePath(), false) ?: return
        if (parent.children == null) {
            parent.native?.hasUnrealizedChildren = !model.isLeaf(parent.value)
            return
        }
        for (index in e.getChildIndices()?.sorted().orEmpty()) {
            insertChild(parent, index, model.getChild(parent.value, index) ?: continue)
        }
    }

    /** Nodes were removed: remove them from a parent with created children, in descending order of index. */
    fun nodesRemoved(e: TreeModelEvent) {
        val parent = entryOf(e.getTreePath(), false) ?: return
        if (parent.children == null) {
            parent.native?.hasUnrealizedChildren = !model.isLeaf(parent.value)
            return
        }
        for (index in e.getChildIndices()?.sortedDescending().orEmpty()) removeChild(parent, index)
    }

    /** The structure changed: rebuild below the path's node (if it was expanded, recreate the children and keep it expanded). If it is the root, rebuild everything. */
    fun structureChanged(e: TreeModelEvent) {
        val path = e.getTreePath()
        if (path == null || path.getPathCount() == 1) {
            clear()
            build()
            return
        }
        val entry = entryOf(path, false) ?: return
        val native = entry.native ?: return
        val wasExpanded = native.isExpanded
        entry.children?.let { children ->
            for (index in children.indices.reversed()) removeChild(entry, index)
        }
        entry.children = null
        native.hasUnrealizedChildren = !model.isLeaf(entry.value)
        if (wasExpanded && !model.isLeaf(entry.value)) {
            realize(entry)
            native.isExpanded = true
        }
    }
}
