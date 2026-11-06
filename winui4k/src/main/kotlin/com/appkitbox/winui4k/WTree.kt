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
import com.appkitbox.winui4k.tree.DefaultTreeModel
import com.appkitbox.winui4k.tree.DefaultTreeSelectionModel
import com.appkitbox.winui4k.tree.ExpandVetoException
import com.appkitbox.winui4k.tree.TreeExpansionEvent
import com.appkitbox.winui4k.tree.TreeExpansionListener
import com.appkitbox.winui4k.tree.TreeModel
import com.appkitbox.winui4k.tree.TreeModelEvent
import com.appkitbox.winui4k.tree.TreeModelListener
import com.appkitbox.winui4k.tree.TreeNode
import com.appkitbox.winui4k.tree.TreePath
import com.appkitbox.winui4k.tree.TreeSelectionListener
import com.appkitbox.winui4k.tree.TreeSelectionModel
import com.appkitbox.winui4k.tree.TreeWillExpandListener
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

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
        @JvmSynthetic
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
class WTreeNode @JvmOverloads constructor(text: String = "") {
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

    /** The children are not created yet but will be created on expansion (TreeViewNode.HasUnrealizedChildren). Shows the expand mark even without children. */
    internal var hasUnrealizedChildren: Boolean
        get() = inspectable.getBool(XamlInterop.ITreeViewNode_get_HasUnrealizedChildren)
        set(value) = inspectable.putBool(XamlInterop.ITreeViewNode_put_HasUnrealizedChildren, value)

    /** Inserts a child node at [index] (Children.InsertAt). Used by [WTree], which builds the model mirror. */
    internal fun insertAt(index: Int, child: WTreeNode) {
        childVector.call(FoundationInterop.IVector_InsertAt, index, child.inspectable.ptr)
        childNodes.add(index, child)
    }

    /** Removes the child node at [index] (Children.RemoveAt). */
    internal fun removeAt(index: Int) {
        childVector.call(FoundationInterop.IVector_RemoveAt, index)
        childNodes.removeAt(index)
    }

    /** The COM identity (the IUnknown address). Used to look up nodes from native nodes. */
    internal val identity: Long by lazy {
        val unknown = inspectable.queryInterface(KComObject.IID_IUNKNOWN)
        try {
            unknown.ptr.address
        } finally {
            unknown.release()
        }
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
 * There are two ways to use it.
 * - Mirror a [TreeModel] (as in Swing): `WTree(model)` / `WTree(root)` / [model]. Path ([TreePath]) elements are the model's nodes.
 *   Children are not created until expanded (only the expand mark is shown with HasUnrealizedChildren, and they are created on first expansion), and model changes
 *   ([com.appkitbox.winui4k.tree.TreeModelListener] notifications) are reflected in the display. Nodes are displayed with `toString()` ([convertValueToText]).
 * - Build [WTreeNode]s directly: `WTree()` and [addRootNode] / [removeRootNode] / [rootNodes] / [selectedNode] / [selectedNodes].
 *   Path elements are [WTreeNode]s, and the root is one of RootNodes (Swing's invisible root is not part of the path).
 *   While a model is used, these node APIs throw [IllegalStateException].
 *
 * Either way, the path and row APIs ([expandPath] / [collapsePath] / [isExpanded] / [rowCount] / [getPathForRow] / [getRowForPath] /
 * [makeVisible] / [scrollPathToVisible] / [getPathForLocation] ...) can be used. As in Swing, a row is the position of a displayed node
 * counted from the top (counting only the children of expanded nodes).
 * Also [selectionMode] / [selectAll], [expand] / [collapse], [canDragItems] / [canReorderItems] (reordering by dragging),
 * [addItemInvokedListener] (ItemInvoked), [addExpandingListener] / [addCollapsedListener] (Expanding / Collapsed).
 */
class WTree() : WControl(
    Activation.composeDefault(XamlInterop.CLS_TreeView, XamlInterop.IID_ITreeViewFactory), // default interface = ITreeView
) {
    /** Creates a tree that shows [model] (Swing's `JTree(TreeModel)`). An empty tree if null. */
    constructor(model: TreeModel?) : this() {
        this.model = model
    }

    /** Creates a tree that shows a [DefaultTreeModel] rooted at [root] (Swing's `JTree(TreeNode)`). */
    constructor(root: TreeNode?) : this(DefaultTreeModel(root))

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

    /** The mapping between displayed nodes and paths (either the node-building usage or the model-mirroring usage). */
    private var binding: TreeBinding = NodeTreeBinding(roots)

    /** Looks up this tree from the resident native handlers (a cycle forms if a handler captures the tree strongly). */
    private val anchor = ListenerAnchor(this).also { it.listenerAdded() }

    /** The listener that mirrors model changes (looks up the tree weakly). */
    private val modelListener: TreeModelListener = AnchoredTreeModelListener(anchor)

    /** While greater than 0, native expand/collapse notifications are not passed to the user (while the tree itself is changing them). */
    private var expansionMuted = 0

    /** ItemInvoked event tokens registered via addItemInvokedListener. */
    private val itemInvokedTokens = ListenerTokens<Consumer<WTreeNode?>>()

    /** Expanding event tokens registered via addExpandingListener. */
    private val expandingTokens = ListenerTokens<Consumer<WTreeNode?>>()

    /** Collapsed event tokens registered via addCollapsedListener. */
    private val collapsedTokens = ListenerTokens<Consumer<WTreeNode?>>()

    init {
        val anchor = anchor // Let the handlers capture only the anchor, not the tree (no cycle)
        inspectable.addEventHandler(
            "WinUI4K.TreeViewHandler",
            XamlInterop.IID_TreeViewExpandingHandler,
            XamlInterop.ITreeView_add_Expanding,
        ) { _, args -> anchor.get()?.onNativeExpanding(ComPtr(args)) }
        inspectable.addEventHandler(
            "WinUI4K.TreeViewHandler",
            XamlInterop.IID_TreeViewCollapsedHandler,
            XamlInterop.ITreeView_add_Collapsed,
        ) { _, args -> anchor.get()?.onNativeCollapsed(ComPtr(args)) }
        val treeView3 = inspectable.queryInterface(XamlInterop.IID_ITreeView3)
        try {
            treeView3.addEventHandler(
                "WinUI4K.TreeViewHandler",
                XamlInterop.IID_TreeViewSelectionChangedHandler,
                XamlInterop.ITreeView3_add_SelectionChanged,
            ) { _, _ -> anchor.get()?.selectionBridge?.onNativeSelectionChanged() }
        } finally {
            treeView3.release()
        }
    }

    /** Keeps the selection model and the TreeView selection in sync. */
    internal val selectionBridge = TreeSelectionBridge(this, anchor, { binding }, inspectable)

    /** The listeners added with addTreeExpansionListener (called in the order added). */
    private val expansionListeners = CopyOnWriteArrayList<TreeExpansionListener>()

    /** The listeners added with addTreeWillExpandListener (called in the order added). */
    private val willExpandListeners = CopyOnWriteArrayList<TreeWillExpandListener>()

    /**
     * The selection mode (TreeView.SelectionMode). In MULTIPLE, each node gets a check box.
     * The mode of [selectionModel] is matched too (SINGLE → SINGLE_TREE_SELECTION, MULTIPLE → DISCONTIGUOUS_TREE_SELECTION if it was SINGLE,
     * NONE → clears the selection. NONE has no Swing counterpart, and the selection model's mode is not changed).
     * To decide in Swing terms, use `selectionModel.setSelectionMode(...)` (it is reflected here too).
     */
    var selectionMode: TreeViewSelectionMode
        get() = TreeViewSelectionMode.of(inspectable.getInt(XamlInterop.ITreeView_get_SelectionMode))
        set(value) {
            inspectable.call(XamlInterop.ITreeView_put_SelectionMode, value.native)
            selectionBridge.onNativeModeSet(value)
        }

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

    // ---- Model ----

    /**
     * The [TreeModel] to show (Swing's `getModel` / `setModel`). null in the usage that builds nodes directly (`WTree()`).
     *
     * Setting it rebuilds the display (as in Swing, the root starts expanded). Any built root nodes are removed from the display.
     * Setting null returns to the node-building usage (an empty tree).
     */
    var model: TreeModel?
        get() = (binding as? ModelTreeBinding)?.model
        set(value) = bind(value, rootVisible)

    /**
     * Whether to show the root (Swing's `isRootVisible` / `setRootVisible`, true by default).
     * If false, the children of the model's root are placed at the top level (the root is treated as always expanded and is not counted as a row).
     * It has no effect in the node-building usage (root nodes are always shown).
     */
    @get:JvmName("isRootVisible")
    var rootVisible: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            (binding as? ModelTreeBinding)?.let { bind(it.model, value) }
        }

    /**
     * Whether to show the expand mark of the root (Swing's `getShowsRootHandles` / `setShowsRootHandles`).
     * TreeView always shows the expand mark on nodes with children, so it only remembers the value and the display does not change.
     */
    var showsRootHandles: Boolean = true

    private fun bind(newModel: TreeModel?, newRootVisible: Boolean) {
        when (val old = binding) {
            is ModelTreeBinding -> {
                old.model.removeTreeModelListener(modelListener)
                muted { old.clear() }
            }
            is NodeTreeBinding -> if (roots.isNotEmpty()) {
                rootVector.call(FoundationInterop.IVector_Clear)
                roots.clear()
            }
        }
        binding = if (newModel == null) {
            NodeTreeBinding(roots)
        } else {
            muted { ModelTreeBinding(newModel, newRootVisible, rootVector) }.also { newModel.addTreeModelListener(modelListener) }
        }
        selectionModel.clearSelection() // Clear the selection, as Swing's setModel does
    }

    /** The node APIs cannot be used while a model is mirrored. */
    private fun requireNodeMode(): Unit = check(binding is NodeTreeBinding) {
        "WTree is showing a model, so the node APIs (addRootNode etc.) cannot be used. Set model to null to use them"
    }

    /** Reflects model changes in the display (native expand/collapse caused by rebuilding the mirror is not passed to the user). */
    private fun onModelChanged(e: TreeModelEvent, apply: ModelTreeBinding.(TreeModelEvent) -> Unit) {
        val binding = binding as? ModelTreeBinding ?: return
        muted { binding.apply(e) }
        selectionBridge.pushToNative()
    }

    /**
     * As with Swing's TreeModelHandler, deselects the removed nodes (the children of [e]) and their descendants
     * (only below the path's node if [includeSelf] is false; for structure changes).
     */
    private fun removeDescendantSelectedPaths(e: TreeModelEvent, includeSelf: Boolean) {
        val parent = e.getTreePath() ?: return
        val roots = if (includeSelf) e.getChildren().orEmpty().filterNotNull().map(parent::pathByAddingChild) else listOf(parent)
        val selected = selectionModel.getSelectionPaths().orEmpty()
        val removed = selected.filter { path -> roots.any { it.isDescendant(path) && (includeSelf || it != path) } }
        if (removed.isNotEmpty()) selectionModel.removeSelectionPaths(removed.toTypedArray())
    }

    /**
     * The string that displays [value] (Swing's `convertValueToText`). `toString()`, or an empty string if null.
     * Nodes are always displayed by this rule, and it cannot be overridden.
     */
    @Suppress("UNUSED_PARAMETER", "LongParameterList") // Takes unused parameters too, to have the same shape as Swing
    fun convertValueToText(value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): String =
        value?.toString().orEmpty()

    // ---- Node-building usage ----

    /**
     * The currently selected node, or null if nothing is selected (TreeView.SelectedNode).
     * The getter looks the result up against the nodes added via [addRootNode] (including descendants) by COM identity.
     * Throws [IllegalStateException] while a model is shown.
     */
    var selectedNode: WTreeNode?
        get() {
            requireNodeMode()
            val selected = treeView2.getPtrOrNull(XamlInterop.ITreeView2_get_SelectedNode) ?: return null
            return try {
                resolveNode(roots, selected)
            } finally {
                selected.release()
            }
        }
        set(value) {
            requireNodeMode()
            treeView2.call(XamlInterop.ITreeView2_put_SelectedNode, value?.inspectable?.ptr)
            selectionBridge.onNativeSelectionChanged() // SelectionChanged does not come before it is shown, so mirror to the selection model here
        }

    /**
     * The list of currently selected nodes (TreeView.SelectedNodes).
     * Looks up the checked nodes (in MULTIPLE mode) against the nodes added via [addRootNode].
     * Throws [IllegalStateException] while a model is shown.
     */
    val selectedNodes: List<WTreeNode>
        get() {
            requireNodeMode()
            val vector = inspectable.getPtr(XamlInterop.ITreeView_get_SelectedNodes)
            return try {
                (0 until vector.getInt(FoundationInterop.IVector_get_Size)).mapNotNull { index ->
                    val node = vector.getPtr(FoundationInterop.IVector_GetAt, index)
                    try {
                        resolveNode(roots, node)
                    } finally {
                        node.release()
                    }
                }
            } finally {
                vector.release()
            }
        }

    /** The list of root nodes added with [addRootNode] (an unmodifiable view). Throws [IllegalStateException] while a model is shown. */
    val rootNodes: List<WTreeNode>
        get() {
            requireNodeMode()
            return roots.toList()
        }

    /** Appends a root node at the end (RootNodes.Append). Throws [IllegalStateException] while a model is shown. */
    fun addRootNode(node: WTreeNode) {
        requireNodeMode()
        rootVector.call(FoundationInterop.IVector_Append, node.inspectable.ptr)
        roots += node
    }

    /** Removes a root node (RootNodes.RemoveAt). Throws [IllegalStateException] while a model is shown. */
    fun removeRootNode(node: WTreeNode) {
        requireNodeMode()
        val index = roots.indexOf(node)
        if (index < 0) return
        rootVector.call(FoundationInterop.IVector_RemoveAt, index)
        roots.removeAt(index)
    }

    /**
     * Expands the node (same as setting [WTreeNode.isExpanded] to true). Can be used before it is shown.
     * TreeView.Expand crashes natively when called before it is shown (before the template is applied), so it is not used.
     */
    fun expand(node: WTreeNode) {
        node.isExpanded = true
    }

    /** Collapses the node (same as setting [WTreeNode.isExpanded] to false). Can be used before it is shown. */
    fun collapse(node: WTreeNode) {
        node.isExpanded = false
    }

    /**
     * Selects all nodes (TreeView.SelectAll). Effective in MULTIPLE mode.
     * Before it is shown, adds all created nodes to SelectedNodes (TreeView.SelectAll crashes when called before the template is applied).
     */
    fun selectAll() {
        if (isTemplateApplied()) {
            inspectable.call(XamlInterop.ITreeView_SelectAll)
            return
        }
        if (selectionMode != TreeViewSelectionMode.MULTIPLE) return
        val vector = inspectable.getPtr(XamlInterop.ITreeView_get_SelectedNodes)
        try {
            vector.call(FoundationInterop.IVector_Clear)
            for (node in binding.topNodes.asSequence().flatMap { it.selfAndDescendants() }) {
                vector.call(FoundationInterop.IVector_Append, node.inspectable.ptr)
            }
        } finally {
            vector.release()
        }
    }

    /** true if the template is applied (the ListView inside the TreeView exists). Expand etc. crash natively without it. */
    private fun isTemplateApplied(): Boolean = Xaml.childCount(inspectable) > 0

    // ---- Paths and rows ----

    /** Expands the node that [path] points to (Swing's `expandPath`). Also expands all parents. Does nothing for a leaf or null. */
    fun expandPath(path: TreePath?) {
        if (path == null || binding.isLeaf(path)) return
        setExpandedState(path, true)
    }

    /** Collapses the node that [path] points to (Swing's `collapsePath`). Expands all parents. */
    fun collapsePath(path: TreePath?) {
        path ?: return
        setExpandedState(path, false)
    }

    /** Expands the node at row [row] (Swing's `expandRow`). Does nothing if out of range. */
    fun expandRow(row: Int) = expandPath(getPathForRow(row))

    /** Collapses the node at row [row] (Swing's `collapseRow`). Does nothing if out of range. */
    fun collapseRow(row: Int) = collapsePath(getPathForRow(row))

    /** true if the node of [path] and all its parents are expanded (Swing's `isExpanded`). false for null or a path not in the tree. */
    fun isExpanded(path: TreePath?): Boolean {
        var current = path ?: return false
        while (true) {
            if (!isOwnExpanded(current)) return false
            current = current.getParentPath() ?: return true
        }
    }

    /** true if the node at row [row] is expanded. false if out of range. */
    fun isExpanded(row: Int): Boolean = isExpanded(getPathForRow(row))

    /** The negation of [isExpanded] (Swing's `isCollapsed`). */
    fun isCollapsed(path: TreePath?): Boolean = !isExpanded(path)

    /** true if the node at row [row] is collapsed (also true if out of range). */
    fun isCollapsed(row: Int): Boolean = !isExpanded(row)

    /** true if all parents of [path] are expanded (it is visible as a row) (Swing's `isVisible`). true for a top-level path. */
    fun isVisible(path: TreePath?): Boolean {
        path ?: return false
        return path.getParentPath()?.let(::isExpanded) ?: true
    }

    /** Expands all parents so that [path] is visible (Swing's `makeVisible`). */
    fun makeVisible(path: TreePath?) {
        path?.getParentPath()?.let(::expandPath)
    }

    /** The number of visible rows (Swing's `getRowCount`). */
    val rowCount: Int
        get() = binding.rows().size

    /** The path of the node at row [row] (Swing's `getPathForRow`). null if out of range. */
    fun getPathForRow(row: Int): TreePath? = binding.rows().getOrNull(row)?.let(binding::pathOf)

    /** The row of the node of [path] (Swing's `getRowForPath`). -1 if not visible. */
    fun getRowForPath(path: TreePath?): Int {
        path ?: return -1
        val node = binding.nodeOf(path, false) ?: return -1
        if (!isVisible(path)) return -1
        return binding.rows().indexOfFirst { it === node }
    }

    /**
     * Expands the parents so that [path] is visible and scrolls to that row (Swing's `scrollPathToVisible`).
     * Does not scroll before it is shown (only expands the parents).
     */
    fun scrollPathToVisible(path: TreePath?) {
        path ?: return
        makeVisible(path)
        val node = binding.nodeOf(path, true) ?: return
        if (!isTemplateApplied()) return
        val list = Xaml.templatePartOrNull(inspectable, "ListControl") ?: return
        try {
            val listView = list.queryInterface(XamlInterop.IID_IListViewBase)
            try {
                listView.call(XamlInterop.IListViewBase_ScrollIntoView, node.inspectable.ptr)
            } finally {
                listView.release()
            }
        } finally {
            list.release()
        }
    }

    /** Scrolls to row [row] (Swing's `scrollRowToVisible`). */
    fun scrollRowToVisible(row: Int) = scrollPathToVisible(getPathForRow(row))

    /**
     * The path of the row at the position ([x], [y], in DIP) from the top-left of the tree (Swing's `getPathForLocation`). null outside the rows.
     * Only rows realized on screen (TreeViewItem) are examined, so rows before it is shown or scrolled out are not found.
     */
    fun getPathForLocation(x: Int, y: Int): TreePath? =
        rowBounds().firstOrNull { (_, bounds) -> x >= bounds[0] && x < bounds[0] + bounds[2] && y >= bounds[1] && y < bounds[1] + bounds[3] }
            ?.let { (node, _) -> binding.pathOf(node) }

    /** The row for [getPathForLocation] (Swing's `getRowForLocation`). -1 outside the rows. */
    fun getRowForLocation(x: Int, y: Int): Int = getRowForPath(getPathForLocation(x, y))

    /**
     * The path of the row closest to the vertical position [y] (Swing's `getClosestPathForLocation`). null if there are no rows.
     * As with [getPathForLocation], only rows realized on screen are examined.
     */
    @Suppress("UNUSED_PARAMETER") // Takes the horizontal position too, to have the same shape as Swing (rows span the full width)
    fun getClosestPathForLocation(x: Int, y: Int): TreePath? =
        rowBounds().minByOrNull { (_, bounds) -> distance(y.toDouble(), bounds[1], bounds[1] + bounds[3]) }
            ?.let { (node, _) -> binding.pathOf(node) }

    /** The visible rows that are realized on screen, and their bounds within the tree [x, y, width, height] (DIP). */
    private fun rowBounds(): List<Pair<WTreeNode, DoubleArray>> {
        if (!isTemplateApplied()) return emptyList()
        val origin = Xaml.positionInRoot(uiElement)
        return binding.rows().mapNotNull { node ->
            val container = treeView2.getPtrOrNull(XamlInterop.ITreeView2_ContainerFromNode, node.inspectable.ptr) ?: return@mapNotNull null
            try {
                val framework = container.queryInterface(XamlInterop.IID_IFrameworkElement)
                try {
                    val position = Xaml.positionInRoot(container)
                    node to doubleArrayOf(
                        position[0] - origin[0],
                        position[1] - origin[1],
                        framework.getDouble(XamlInterop.IFrameworkElement_get_ActualWidth),
                        framework.getDouble(XamlInterop.IFrameworkElement_get_ActualHeight),
                    )
                } finally {
                    framework.release()
                }
            } finally {
                container.release()
            }
        }
    }

    /** The native node showing [path] (null if its children are not created). Used by tests to check the mirror. */
    internal fun nativeNodeOf(path: TreePath): WTreeNode? = binding.nodeOf(path, false)

    /** Whether the path's node itself is expanded (parents are not checked). A hidden root is always expanded. */
    private fun isOwnExpanded(path: TreePath): Boolean =
        binding.isHiddenRoot(path) || binding.nodeOf(path, false)?.isExpanded == true

    /** Swing's `setExpandedState`: expands the parents from the top, then expands or collapses [path]. */
    private fun setExpandedState(path: TreePath, expanded: Boolean) {
        val ancestors = generateSequence(path.getParentPath()) { it.getParentPath() }.toList().asReversed()
        for (ancestor in ancestors) {
            if (!isOwnExpanded(ancestor) && !expandOne(ancestor)) return
        }
        when {
            expanded && !isOwnExpanded(path) -> expandOne(path)
            !expanded && isOwnExpanded(path) -> collapseOne(path)
        }
    }

    /** Expands one node of [path] (creating its children if not created yet). false if it cannot be expanded or was vetoed. */
    private fun expandOne(path: TreePath): Boolean {
        val node = binding.nodeOf(path, true) ?: return false
        if (isVetoed { it.treeWillExpand(TreeExpansionEvent(this, path)) }) return false
        binding.realize(node)
        muted { node.isExpanded = true }
        expansionListeners.forEach { it.treeExpanded(TreeExpansionEvent(this, path)) }
        return true
    }

    /** Collapses one node of [path]. A hidden root cannot be collapsed. */
    private fun collapseOne(path: TreePath): Boolean {
        val node = binding.nodeOf(path, false) ?: return false
        if (isVetoed { it.treeWillCollapse(TreeExpansionEvent(this, path)) }) return false
        muted { node.isExpanded = false }
        expansionListeners.forEach { it.treeCollapsed(TreeExpansionEvent(this, path)) }
        return true
    }

    /** Calls TreeWillExpandListeners in the order added, and returns true if one of them throws [ExpandVetoException] (without calling the rest). */
    private fun isVetoed(call: (TreeWillExpandListener) -> Unit): Boolean = try {
        willExpandListeners.forEach(call)
        false
    } catch (_: ExpandVetoException) {
        true
    }

    private inline fun <T> muted(block: () -> T): T {
        expansionMuted++
        try {
            return block()
        } finally {
            expansionMuted--
        }
    }

    /** Native Expanding (after IsExpanded becomes true): creates the children not created yet. */
    private fun onNativeExpanding(args: ComPtr) {
        val pointer = args.getPtrOrNull(XamlInterop.ITreeViewExpandingEventArgs_get_Node) ?: return
        val node = try {
            binding.resolve(pointer)
        } finally {
            pointer.release()
        }
        node ?: return
        binding.realize(node)
        if (expansionMuted > 0) return
        val path = binding.pathOf(node) ?: return
        // A veto collapses it again within the same notification right after expanding (before drawing, so the expanded state never appears on screen)
        if (isVetoed { it.treeWillExpand(TreeExpansionEvent(this, path)) }) {
            muted { node.isExpanded = false }
            return
        }
        expansionListeners.forEach { it.treeExpanded(TreeExpansionEvent(this, path)) }
    }

    /** Native Collapsed (after collapsing, by user operation etc.): notifies TreeExpansionListeners. */
    private fun onNativeCollapsed(args: ComPtr) {
        if (expansionMuted > 0) return
        val pointer = args.getPtrOrNull(XamlInterop.ITreeViewCollapsedEventArgs_get_Node) ?: return
        val node = try {
            binding.resolve(pointer)
        } finally {
            pointer.release()
        }
        val path = node?.let(binding::pathOf) ?: return
        expansionListeners.forEach { it.treeCollapsed(TreeExpansionEvent(this, path)) }
    }

    // ---- Expansion and selection listeners ----

    /** Adds a listener called after expansion and collapse (Swing's `addTreeExpansionListener`). Called in the order added. */
    fun addTreeExpansionListener(listener: TreeExpansionListener) {
        expansionListeners += listener
    }

    /** Removes a listener added with [addTreeExpansionListener]. */
    fun removeTreeExpansionListener(listener: TreeExpansionListener) {
        expansionListeners.remove(listener)
    }

    /** The added TreeExpansionListeners (in the order added). */
    fun getTreeExpansionListeners(): Array<TreeExpansionListener> = expansionListeners.toTypedArray()

    /**
     * Adds a listener called before expansion and collapse (Swing's `addTreeWillExpandListener`). Called in the order added.
     * For expansion by user operation it is called right after WinUI expands, and a veto collapses it again. For collapsing by user operation,
     * [TreeWillExpandListener.treeWillCollapse] cannot be called (WinUI has no notification before collapsing).
     */
    fun addTreeWillExpandListener(listener: TreeWillExpandListener) {
        willExpandListeners += listener
    }

    /** Removes a listener added with [addTreeWillExpandListener]. */
    fun removeTreeWillExpandListener(listener: TreeWillExpandListener) {
        willExpandListeners.remove(listener)
    }

    /** The added TreeWillExpandListeners (in the order added). */
    fun getTreeWillExpandListeners(): Array<TreeWillExpandListener> = willExpandListeners.toTypedArray()

    /** Adds a listener that receives selection changes (Swing's `addTreeSelectionListener`). The event source is this tree, and listeners are called in the order added. */
    fun addTreeSelectionListener(listener: TreeSelectionListener) = selectionBridge.addListener(listener)

    /** Removes a listener added with [addTreeSelectionListener]. */
    fun removeTreeSelectionListener(listener: TreeSelectionListener) = selectionBridge.removeListener(listener)

    /** The added TreeSelectionListeners (in the order added). */
    fun getTreeSelectionListeners(): Array<TreeSelectionListener> = selectionBridge.listeners()

    // ---- Selection ----

    /**
     * The selection state (Swing's `getSelectionModel` / `setSelectionModel`). The default is a SINGLE_TREE_SELECTION [DefaultTreeSelectionModel]
     * (same as the TreeView default; Swing's default is DISCONTIGUOUS). The RowMapper that converts paths to rows is set by this tree.
     * The mode is mirrored to the TreeView (SINGLE → SINGLE, CONTIGUOUS / DISCONTIGUOUS → MULTIPLE with check boxes).
     * In MULTIPLE, the TreeView also selects descendants when a parent is checked, so the selection after a user operation includes descendants too.
     * The selection of children hidden by collapsing follows the TreeView behavior (unlike Swing, the selection is not moved to the collapsed node).
     */
    var selectionModel: TreeSelectionModel
        get() = selectionBridge.model
        set(value) {
            selectionBridge.model = value
        }

    /** The first selected path (Swing's `getSelectionPath` / `setSelectionPath`). null if none. */
    var selectionPath: TreePath?
        get() = selectionModel.getSelectionPath()
        set(value) = selectionModel.setSelectionPath(value)

    /** The selected paths. null if nothing is selected (same as Swing's JTree). */
    fun getSelectionPaths(): Array<TreePath>? = selectionModel.getSelectionPaths()?.takeIf { it.isNotEmpty() }

    /** Selects only [paths]. */
    fun setSelectionPaths(paths: Array<out TreePath?>?) = selectionModel.setSelectionPaths(paths)

    /** Adds [path] to the selection. */
    fun addSelectionPath(path: TreePath?) = selectionModel.addSelectionPath(path)

    /** Adds [paths] to the selection. */
    fun addSelectionPaths(paths: Array<out TreePath?>?) = selectionModel.addSelectionPaths(paths)

    /** Removes [path] from the selection. */
    fun removeSelectionPath(path: TreePath?) = selectionModel.removeSelectionPath(path)

    /** Removes [paths] from the selection. */
    fun removeSelectionPaths(paths: Array<out TreePath?>?) = selectionModel.removeSelectionPaths(paths)

    /** true if [path] is selected. */
    fun isPathSelected(path: TreePath?): Boolean = selectionModel.isPathSelected(path)

    /** Clears the whole selection. */
    fun clearSelection() = selectionModel.clearSelection()

    /** true if nothing is selected. */
    val isSelectionEmpty: Boolean
        get() = selectionModel.isSelectionEmpty()

    /** The number of selected paths. */
    val selectionCount: Int
        get() = selectionModel.getSelectionCount()

    /** The last node of the first selected path (Swing's `getLastSelectedPathComponent`). null if none. */
    val lastSelectedPathComponent: Any?
        get() = selectionModel.getSelectionPath()?.getLastPathComponent()

    /** The lead (last added) path. */
    val leadSelectionPath: TreePath?
        get() = selectionModel.getLeadSelectionPath()

    /** The row of the lead path. -1 if none. */
    val leadSelectionRow: Int
        get() = selectionModel.getLeadSelectionRow()

    /** The minimum row of the selected paths. -1 if none. */
    val minSelectionRow: Int
        get() = selectionModel.getMinSelectionRow()

    /** The maximum row of the selected paths. -1 if none. */
    val maxSelectionRow: Int
        get() = selectionModel.getMaxSelectionRow()

    /** The rows of the selected paths (excluding invisible ones). */
    fun getSelectionRows(): IntArray? = selectionModel.getSelectionRows()

    /** Selects only row [row]. */
    fun setSelectionRow(row: Int) = setSelectionRows(intArrayOf(row))

    /** Selects only the rows in [rows] (out-of-range rows are ignored). */
    fun setSelectionRows(rows: IntArray?) = selectionModel.setSelectionPaths(rows?.toList()?.mapNotNull(::getPathForRow)?.toTypedArray())

    /** Adds row [row] to the selection. */
    fun addSelectionRow(row: Int) = addSelectionRows(intArrayOf(row))

    /** Adds the rows in [rows] to the selection (out-of-range rows are ignored). */
    fun addSelectionRows(rows: IntArray?) = selectionModel.addSelectionPaths(rows?.toList()?.mapNotNull(::getPathForRow)?.toTypedArray())

    /** true if row [row] is selected. */
    fun isRowSelected(row: Int): Boolean = selectionModel.isRowSelected(row)

    /**
     * Subscribes to node clicks (TreeView.ItemInvoked).
     * The listener receives the clicked [WTreeNode] (null for nodes other than added ones, or while a model is shown).
     */
    @JvmSynthetic
    fun addItemInvokedListener(listener: (WTreeNode?) -> Unit) {
        val adapter = Consumer<WTreeNode?> { listener(it) }
        addItemInvokedListenerForJava(adapter)
        itemInvokedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addItemInvokedListener")
    fun addItemInvokedListenerForJava(listener: Consumer<WTreeNode?>) {
        val roots = roots // Let the handler capture only the root list, not the wrapper (no cycle)
        val token = inspectable.addEventHandler(
            "WinUI4K.TreeViewHandler",
            XamlInterop.IID_TreeViewItemInvokedHandler,
            XamlInterop.ITreeView_add_ItemInvoked,
        ) { _, args ->
            // In node mode (no ItemsSource), InvokedItem is the TreeViewNode itself
            val invoked = ComPtr(args).getPtrOrNull(XamlInterop.ITreeViewItemInvokedEventArgs_get_InvokedItem)
            val node = try {
                invoked?.let { resolveNode(roots, it) }
            } finally {
                invoked?.release()
            }
            listener.accept(node)
        }
        itemInvokedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addItemInvokedListener]. */
    @JvmSynthetic
    fun removeItemInvokedListener(listener: (WTreeNode?) -> Unit) {
        val adapter = itemInvokedTokens.removeKotlinAdapter(listener) ?: return
        removeItemInvokedListenerForJava(adapter)
    }

    @JvmName("removeItemInvokedListener")
    fun removeItemInvokedListenerForJava(listener: Consumer<WTreeNode?>) {
        val token = itemInvokedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITreeView_remove_ItemInvoked, token)
    }

    /**
     * Subscribes to node expansion (TreeView.Expanding).
     * The listener receives the [WTreeNode] being expanded (null if it isn't one of the added nodes).
     */
    @JvmSynthetic
    fun addExpandingListener(listener: (WTreeNode?) -> Unit) {
        val adapter = Consumer<WTreeNode?> { listener(it) }
        addExpandingListenerForJava(adapter)
        expandingTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addExpandingListener")
    fun addExpandingListenerForJava(listener: Consumer<WTreeNode?>) {
        val roots = roots // Let the handler capture only the root list, not the wrapper (no cycle)
        val token = inspectable.addEventHandler(
            "WinUI4K.TreeViewHandler",
            XamlInterop.IID_TreeViewExpandingHandler,
            XamlInterop.ITreeView_add_Expanding,
        ) { _, args ->
            listener.accept(resolveNodeArg(roots, args, XamlInterop.ITreeViewExpandingEventArgs_get_Node))
        }
        expandingTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addExpandingListener]. */
    @JvmSynthetic
    fun removeExpandingListener(listener: (WTreeNode?) -> Unit) {
        val adapter = expandingTokens.removeKotlinAdapter(listener) ?: return
        removeExpandingListenerForJava(adapter)
    }

    @JvmName("removeExpandingListener")
    fun removeExpandingListenerForJava(listener: Consumer<WTreeNode?>) {
        val token = expandingTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITreeView_remove_Expanding, token)
    }

    /**
     * Subscribes to node collapse (TreeView.Collapsed).
     * The listener receives the [WTreeNode] that was collapsed (null if it isn't one of the added nodes).
     */
    @JvmSynthetic
    fun addCollapsedListener(listener: (WTreeNode?) -> Unit) {
        val adapter = Consumer<WTreeNode?> { listener(it) }
        addCollapsedListenerForJava(adapter)
        collapsedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmName("addCollapsedListener")
    fun addCollapsedListenerForJava(listener: Consumer<WTreeNode?>) {
        val roots = roots // Let the handler capture only the root list, not the wrapper (no cycle)
        val token = inspectable.addEventHandler(
            "WinUI4K.TreeViewHandler",
            XamlInterop.IID_TreeViewCollapsedHandler,
            XamlInterop.ITreeView_add_Collapsed,
        ) { _, args ->
            listener.accept(resolveNodeArg(roots, args, XamlInterop.ITreeViewCollapsedEventArgs_get_Node))
        }
        collapsedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addCollapsedListener]. */
    @JvmSynthetic
    fun removeCollapsedListener(listener: (WTreeNode?) -> Unit) {
        val adapter = collapsedTokens.removeKotlinAdapter(listener) ?: return
        removeCollapsedListenerForJava(adapter)
    }

    @JvmName("removeCollapsedListener")
    fun removeCollapsedListenerForJava(listener: Consumer<WTreeNode?>) {
        val token = collapsedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ITreeView_remove_Collapsed, token)
    }

    /** A listener that reflects model changes in the display, looking up the tree weakly (so that the model does not hold the tree strongly). */
    private class AnchoredTreeModelListener(private val anchor: ListenerAnchor<WTree>) : TreeModelListener {
        override fun treeNodesChanged(e: TreeModelEvent) {
            anchor.get()?.onModelChanged(e, ModelTreeBinding::nodesChanged)
        }

        override fun treeNodesInserted(e: TreeModelEvent) {
            anchor.get()?.onModelChanged(e, ModelTreeBinding::nodesInserted)
        }

        override fun treeNodesRemoved(e: TreeModelEvent) {
            val tree = anchor.get() ?: return
            tree.removeDescendantSelectedPaths(e, includeSelf = true)
            tree.onModelChanged(e, ModelTreeBinding::nodesRemoved)
        }

        override fun treeStructureChanged(e: TreeModelEvent) {
            val tree = anchor.get() ?: return
            tree.removeDescendantSelectedPaths(e, includeSelf = false)
            tree.onModelChanged(e, ModelTreeBinding::structureChanged)
        }
    }

    // Called from native handlers, so placed in the companion so as not to capture the wrapper
    private companion object {
        /** The distance from the vertical position [y] to the range [top]..[bottom] (0 inside the range). */
        fun distance(y: Double, top: Double, bottom: Double): Double = when {
            y < top -> top - y
            y > bottom -> y - bottom
            else -> 0.0
        }

        /** Reads get_Node of the event arguments and looks up the [WTreeNode] under [roots]. */
        fun resolveNodeArg(roots: List<WTreeNode>, args: Ptr, slot: Int): WTreeNode? {
            val node = ComPtr(args).getPtrOrNull(slot) ?: return null
            return try {
                resolveNode(roots, node)
            } finally {
                node.release()
            }
        }

        /**
         * Looks up an added [WTreeNode] from a TreeViewNode pointer.
         * Compares addresses using COM's identity rule (QI for IUnknown always returns the same pointer).
         */
        @Suppress("NestedBlockDepth") // Releasing COM references with try/finally adds one level of nesting
        fun resolveNode(roots: List<WTreeNode>, node: ComPtr): WTreeNode? {
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
}
