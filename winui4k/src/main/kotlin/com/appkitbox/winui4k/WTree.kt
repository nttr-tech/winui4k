package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Microsoft.UI.Xaml.Controls.TreeViewSelectionMode (how many nodes can be selected).
 * Values extracted from the winmd (None=0, Single=1, Multiple=2).
 */
enum class TreeViewSelectionMode(internal val native: Int) {
    /** Nothing can be selected. */
    NONE(0),

    /** Only one node can be selected (default). */
    SINGLE(1),

    /** Multiple nodes can be selected via each node's checkbox. */
    MULTIPLE(2),
    ;

    internal companion object {
        fun of(native: Int): TreeViewSelectionMode = entries.first { it.native == native }
    }
}

/**
 * DefaultMutableTreeNode-like: WinUI 3's TreeViewNode.
 *
 * A single node of the tree displayed in [WTree]. Holds a [text] label, and
 * [add] / [remove] / [removeAllChildren] edit its child nodes.
 * Expanded state is read via [isExpanded], hierarchy info via [hasChildren] / [depth] / [childCount].
 */
class WTreeNode(text: String = "") {
    /** Default interface pointer (ITreeViewNode). */
    internal val inspectable: ComPtr =
        Activation.composeDefault(XamlInterop.CLS_TreeViewNode, XamlInterop.IID_ITreeViewNodeFactory)

    /** The record of COM references this wrapper owns (the same mechanism as WComponent). */
    private val lifetime = ComLifetime.adopt(this, inspectable)

    /** IVector<TreeViewNode> view of TreeViewNode.Children (a typed pointer, so no QI needed). */
    private val childVector: ComPtr by lazy {
        lifetime.own(inspectable.getPtr(XamlInterop.ITreeViewNode_get_Children))
    }

    /** Child nodes added via [add]. Used to look nodes back up from e.g. get_Parent. */
    private val childNodes = mutableListOf<WTreeNode>()

    /** The label shown on the node (TreeViewNode.Content). Strings are boxed before being passed. */
    var text: String
        get() {
            val boxed = inspectable.getPtrOrNull(XamlInterop.ITreeViewNode_get_Content) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            inspectable.call(XamlInterop.ITreeViewNode_put_Content, boxed.ptr)
            boxed.release()
        }

    init {
        if (text.isNotEmpty()) this.text = text
    }

    /** Whether child nodes are expanded and shown (TreeViewNode.IsExpanded). */
    var isExpanded: Boolean
        get() = inspectable.getBool(XamlInterop.ITreeViewNode_get_IsExpanded)
        set(value) = inspectable.putBool(XamlInterop.ITreeViewNode_put_IsExpanded, value)

    /** Whether the node has child nodes (TreeViewNode.HasChildren). */
    val hasChildren: Boolean
        get() = inspectable.getBool(XamlInterop.ITreeViewNode_get_HasChildren)

    /** Hierarchy depth with the root at 0 (TreeViewNode.Depth). -1 if not yet added to a tree. */
    val depth: Int
        get() = inspectable.getInt(XamlInterop.ITreeViewNode_get_Depth)

    /** Number of child nodes. */
    val childCount: Int
        get() = childNodes.size

    /** The child nodes added via [add] (an unmodifiable view). */
    val children: List<WTreeNode>
        get() = childNodes.toList()

    /** Appends a child node at the end (Children.Append). */
    fun add(child: WTreeNode) {
        childVector.call(FoundationInterop.IVector_Append, child.inspectable.ptr)
        childNodes += child
    }

    /** Removes a child node (Children.RemoveAt). */
    fun remove(child: WTreeNode) {
        val index = childNodes.indexOf(child)
        if (index < 0) return
        childVector.call(FoundationInterop.IVector_RemoveAt, index)
        childNodes.removeAt(index)
    }

    /** Removes all child nodes (Children.Clear). */
    fun removeAllChildren() {
        childVector.call(FoundationInterop.IVector_Clear)
        childNodes.clear()
    }

    /** Enumerates this node and its descendants depth-first (for look-ups). */
    internal fun selfAndDescendants(): Sequence<WTreeNode> = sequence {
        yield(this@WTreeNode)
        for (child in childNodes) yieldAll(child.selfAndDescendants())
    }
}

/**
 * JTree-like: WinUI 3's TreeView.
 *
 * Displays a hierarchy of [WTreeNode]s: [addRootNode] / [removeRootNode] / [rootNodes],
 * [selectionMode] / [selectedNode] / [selectedNodes] / [selectAll],
 * [expand] / [collapse], [canDragItems] / [canReorderItems] (drag-to-reorder),
 * [addItemInvokedListener] (ItemInvoked),
 * [addExpandingListener] / [addCollapsedListener] (Expanding / Collapsed).
 */
class WTree : WControl(
    Activation.composeDefault(XamlInterop.CLS_TreeView, XamlInterop.IID_ITreeViewFactory), // default interface = ITreeView
) {
    /** ITreeView2 view, which has CanDragItems / SelectedNode and the like. */
    private val treeView2: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_ITreeView2))
    }

    /** IVector<TreeViewNode> view of TreeView.RootNodes (a typed pointer, so no QI needed). */
    private val rootVector: ComPtr by lazy {
        own(inspectable.getPtr(XamlInterop.ITreeView_get_RootNodes))
    }

    /** Root nodes added via [addRootNode]. Used to look nodes back up from e.g. event args. */
    private val roots = mutableListOf<WTreeNode>()

    /** ItemInvoked event tokens registered via addItemInvokedListener. */
    private val itemInvokedTokens = ListenerTokens<(WTreeNode?) -> Unit>()

    /** Expanding event tokens registered via addExpandingListener. */
    private val expandingTokens = ListenerTokens<(WTreeNode?) -> Unit>()

    /** Collapsed event tokens registered via addCollapsedListener. */
    private val collapsedTokens = ListenerTokens<(WTreeNode?) -> Unit>()

    /** Selection mode (TreeView.SelectionMode). MULTIPLE adds a checkbox to each node. */
    var selectionMode: TreeViewSelectionMode
        get() = TreeViewSelectionMode.of(inspectable.getInt(XamlInterop.ITreeView_get_SelectionMode))
        set(value) = inspectable.call(XamlInterop.ITreeView_put_SelectionMode, value.native)

    /**
     * Whether a node can be dragged out (TreeView.CanDragItems).
     * Use [canReorderItems] if you only want in-tree reordering.
     */
    var canDragItems: Boolean
        get() = treeView2.getBool(XamlInterop.ITreeView2_get_CanDragItems)
        set(value) = treeView2.putBool(XamlInterop.ITreeView2_put_CanDragItems, value)

    /** Whether nodes can be reordered via drag & drop (TreeView.CanReorderItems). */
    var canReorderItems: Boolean
        get() = treeView2.getBool(XamlInterop.ITreeView2_get_CanReorderItems)
        set(value) = treeView2.putBool(XamlInterop.ITreeView2_put_CanReorderItems, value)

    /**
     * The currently selected node, or null if nothing is selected (TreeView.SelectedNode).
     * The getter looks the result up against the nodes added via [addRootNode] (including descendants)
     * by COM identity.
     */
    var selectedNode: WTreeNode?
        get() {
            val selected = treeView2.getPtrOrNull(XamlInterop.ITreeView2_get_SelectedNode) ?: return null
            return try {
                resolveNode(selected)
            } finally {
                selected.release()
            }
        }
        set(value) {
            treeView2.call(XamlInterop.ITreeView2_put_SelectedNode, value?.inspectable?.ptr)
        }

    /**
     * The list of currently selected nodes (TreeView.SelectedNodes).
     * Looks up the checked nodes (in MULTIPLE mode) against the nodes added via [addRootNode].
     */
    val selectedNodes: List<WTreeNode>
        get() {
            val vector = inspectable.getPtr(XamlInterop.ITreeView_get_SelectedNodes)
            return try {
                (0 until vector.getInt(FoundationInterop.IVector_get_Size)).mapNotNull { index ->
                    val node = vector.getPtr(FoundationInterop.IVector_GetAt, index)
                    try {
                        resolveNode(node)
                    } finally {
                        node.release()
                    }
                }
            } finally {
                vector.release()
            }
        }

    /** The root nodes added via [addRootNode] (an unmodifiable view). */
    val rootNodes: List<WTreeNode>
        get() = roots.toList()

    /** Appends a root node at the end (RootNodes.Append). */
    fun addRootNode(node: WTreeNode) {
        rootVector.call(FoundationInterop.IVector_Append, node.inspectable.ptr)
        roots += node
    }

    /** Removes a root node (RootNodes.RemoveAt). */
    fun removeRootNode(node: WTreeNode) {
        val index = roots.indexOf(node)
        if (index < 0) return
        rootVector.call(FoundationInterop.IVector_RemoveAt, index)
        roots.removeAt(index)
    }

    /** Expands a node (TreeView.Expand). Unlike [WTreeNode.isExpanded], this also works before lazy realization. */
    fun expand(node: WTreeNode) {
        inspectable.call(XamlInterop.ITreeView_Expand, node.inspectable.ptr)
    }

    /** Collapses a node (TreeView.Collapse). */
    fun collapse(node: WTreeNode) {
        inspectable.call(XamlInterop.ITreeView_Collapse, node.inspectable.ptr)
    }

    /** Selects all nodes (TreeView.SelectAll). Effective in MULTIPLE mode. */
    fun selectAll() {
        inspectable.call(XamlInterop.ITreeView_SelectAll)
    }

    /**
     * Subscribes to node clicks (TreeView.ItemInvoked).
     * The listener receives the clicked [WTreeNode] (null if it isn't one of the added nodes).
     */
    fun addItemInvokedListener(listener: (WTreeNode?) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TreeViewHandler",
            XamlInterop.IID_TreeViewItemInvokedHandler,
            XamlInterop.ITreeView_add_ItemInvoked,
        ) { _, args ->
            // In node mode (no ItemsSource), InvokedItem is the TreeViewNode itself
            val invoked = ComPtr(args).getPtrOrNull(XamlInterop.ITreeViewItemInvokedEventArgs_get_InvokedItem)
            val node = try {
                invoked?.let(::resolveNode)
            } finally {
                invoked?.release()
            }
            listener(node)
        }
        itemInvokedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemInvokedListener]. */
    fun removeItemInvokedListener(listener: (WTreeNode?) -> Unit) {
        val token = itemInvokedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITreeView_remove_ItemInvoked, token)
    }

    /**
     * Subscribes to node expansion (TreeView.Expanding).
     * The listener receives the [WTreeNode] being expanded (null if it isn't one of the added nodes).
     */
    fun addExpandingListener(listener: (WTreeNode?) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TreeViewHandler",
            XamlInterop.IID_TreeViewExpandingHandler,
            XamlInterop.ITreeView_add_Expanding,
        ) { _, args ->
            listener(resolveNodeArg(args, XamlInterop.ITreeViewExpandingEventArgs_get_Node))
        }
        expandingTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addExpandingListener]. */
    fun removeExpandingListener(listener: (WTreeNode?) -> Unit) {
        val token = expandingTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITreeView_remove_Expanding, token)
    }

    /**
     * Subscribes to node collapse (TreeView.Collapsed).
     * The listener receives the [WTreeNode] that was collapsed (null if it isn't one of the added nodes).
     */
    fun addCollapsedListener(listener: (WTreeNode?) -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.TreeViewHandler",
            XamlInterop.IID_TreeViewCollapsedHandler,
            XamlInterop.ITreeView_add_Collapsed,
        ) { _, args ->
            listener(resolveNodeArg(args, XamlInterop.ITreeViewCollapsedEventArgs_get_Node))
        }
        collapsedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCollapsedListener]. */
    fun removeCollapsedListener(listener: (WTreeNode?) -> Unit) {
        val token = collapsedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITreeView_remove_Collapsed, token)
    }

    /** Reads an event args' get_Node and looks it back up to a [WTreeNode]. */
    private fun resolveNodeArg(args: Ptr, slot: Int): WTreeNode? {
        val node = ComPtr(args).getPtrOrNull(slot) ?: return null
        return try {
            resolveNode(node)
        } finally {
            node.release()
        }
    }

    /**
     * Looks a TreeViewNode pointer back up to the added [WTreeNode].
     * Compares addresses using COM identity rules (QI'ing IUnknown always returns the same pointer).
     */
    @Suppress("NestedBlockDepth") // Only looks deep due to the try/finally releasing the COM reference
    private fun resolveNode(node: ComPtr): WTreeNode? {
        val target = node.queryInterface(KComObject.IID_IUNKNOWN)
        try {
            for (root in roots) {
                for (candidate in root.selfAndDescendants()) {
                    val mine = candidate.inspectable.queryInterface(KComObject.IID_IUNKNOWN)
                    val matched = mine.ptr.address == target.ptr.address
                    mine.release()
                    if (matched) return candidate
                }
            }
            return null
        } finally {
            target.release()
        }
    }
}
