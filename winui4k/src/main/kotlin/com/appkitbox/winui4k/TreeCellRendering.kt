package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Showing the nodes of a [WTree] with a [WTreeCellRenderer] (the implementation of cellRenderer). The same mechanism as [WList]'s ListCellRendering.
 *
 * TreeView.ItemTemplate becomes a DataTemplate with only an empty Border (TreeView passes it to the inner ListView, TreeViewList),
 * and the renderer's component is put in Border.Child of that template's root (the TreeViewItem's ContentTemplateRoot).
 * Rows are drawn and marked Handled on ContainerContentChanging of the inner ListView (the template part ListControl) (when a node is assigned to a row, including reuse by scrolling).
 * ListControl does not exist until the template is applied, so when set before showing, it subscribes on Loaded.
 * On selection, expansion and changes to model nodes, all visible rows are redrawn ([repaintRealized]; because the row positions change too).
 * The row container's AutomationProperties.Name is set to the node's display string (so that UI Automation can read the text).
 *
 * The native handlers capture only the [ListenerAnchor], not the tree. While a renderer is set, a shown tree
 * is not collected, the same as when listeners are added.
 */
internal class TreeCellRendering(
    private val tree: WTree,
    /** The mapping between shown nodes and paths (looked up from the tree every time; it changes when the model is replaced). */
    private val binding: () -> TreeBinding,
    /** A function that looks up this object from the tree (must not capture the tree). */
    private val resolve: (WTree) -> TreeCellRendering,
) {
    private val anchor by lazy { ListenerAnchor(tree) }
    private val treeView2: ComPtr by lazy { tree.own(tree.inspectable.queryInterface(XamlInterop.IID_ITreeView2)) }

    /** The IListViewBase of the inner ListView (ListControl) (owned by this object). null if not subscribed. */
    private var listViewBase: ComPtr? = null
    private var containerToken: Long? = null
    private var loadedToken: Long? = null

    /** The renderer that draws nodes. If null, the TreeView shows the node strings. */
    var renderer: WTreeCellRenderer? = null
        set(value) {
            val old = field
            field = value
            when {
                old == null && value != null -> start()
                old != null && value == null -> stop()
                value != null -> repaintRealized()
            }
        }

    private fun start() {
        anchor.listenerAdded()
        putItemTemplate(true)
        if (!attachListControl()) {
            // Let the handlers capture only anchor and resolve (capturing this would form a cycle to the tree)
            val anchor = anchor
            val resolve = resolve
            loadedToken = tree.frameworkElement.addEventHandler(
                "WinUI4K.TreeCellRendererLoaded",
                XamlInterop.IID_RoutedEventHandler,
                XamlInterop.IFrameworkElement_add_Loaded,
            ) { _, _ ->
                // Loaded is called during layout, so refuse if the renderer tries to wait for a dialog to close
                ModalLoop.duringLayout { anchor.get()?.let(resolve)?.onLoaded() }
            }
        }
    }

    private fun stop() {
        loadedToken?.let { tree.frameworkElement.removeEventHandler(XamlInterop.IFrameworkElement_remove_Loaded, it) }
        loadedToken = null
        val list = listViewBase
        containerToken?.let { list?.removeEventHandler(XamlInterop.IListViewBase_remove_ContainerContentChanging, it) }
        containerToken = null
        list?.release()
        listViewBase = null
        putItemTemplate(false)
        anchor.listenerRemoved()
    }

    private fun onLoaded() {
        if (renderer != null) attachListControl()
    }

    /** Subscribes to ContainerContentChanging of the inner ListView and draws the visible rows. false if the ListView does not exist yet. */
    private fun attachListControl(): Boolean {
        if (listViewBase != null) return true
        val part = Xaml.templatePartOrNull(tree.inspectable, "ListControl") ?: return false
        val list = try {
            part.queryInterface(XamlInterop.IID_IListViewBase)
        } finally {
            part.release()
        }
        listViewBase = list
        val anchor = anchor
        val resolve = resolve
        containerToken = list.addEventHandler(
            "WinUI4K.TreeCellRendererContainerHandler",
            XamlInterop.IID_ContainerContentChangingHandler,
            XamlInterop.IListViewBase_add_ContainerContentChanging,
        ) { _, args ->
            // It is called during layout, so refuse if the user's renderer tries to wait for a dialog to close (do not freeze)
            ModalLoop.duringLayout { anchor.get()?.let(resolve)?.onContainerContentChanging(ComPtr(args)) }
        }
        repaintRealized()
        return true
    }

    /** Sets TreeView.ItemTemplate to the Border template that carries the renderer's content (if [rendering] is true) or to the default (null). */
    private fun putItemTemplate(rendering: Boolean) {
        if (!rendering) {
            treeView2.call(XamlInterop.ITreeView2_put_ItemTemplate, null)
            return
        }
        val template = TableViewXaml.createDataTemplate("<Border/>")
        try {
            treeView2.call(XamlInterop.ITreeView2_put_ItemTemplate, template.ptr)
        } finally {
            template.release()
        }
    }

    /** Redraws all visible rows (those whose containers have been created). Does nothing if there is no renderer. */
    fun repaintRealized() {
        if (renderer == null) return
        for (node in binding().rows()) {
            val container = treeView2.getPtrOrNull(XamlInterop.ITreeView2_ContainerFromNode, node.inspectable.ptr) ?: continue
            try {
                render(container, node)
            } finally {
                container.release()
            }
        }
    }

    private fun onContainerContentChanging(args: ComPtr) { // args is borrowed (the default interface)
        if (renderer == null) return
        val container = args.getPtr(XamlInterop.IContainerContentChangingEventArgs_get_ItemContainer)
        try {
            if (args.getBool(XamlInterop.IContainerContentChangingEventArgs_get_InRecycleQueue)) {
                // Remove the content while waiting for reuse (redrawn when assigned next)
                putContent(container, null)
            } else {
                nodeOf(args)?.let { render(container, it) }
            }
            args.putBool(XamlInterop.IContainerContentChangingEventArgs_put_Handled, true)
        } finally {
            container.release()
        }
    }

    /**
     * The node of the assigned item (ContainerContentChangingEventArgs.Item; in a TreeView that lists nodes, the TreeViewNode itself).
     * At this point TreeView.NodeFromContainer still returns null (measured).
     */
    private fun nodeOf(args: ComPtr): WTreeNode? {
        val item = args.getPtrOrNull(XamlInterop.IContainerContentChangingEventArgs_get_Item) ?: return null
        return try {
            binding().resolve(item)
        } finally {
            item.release()
        }
    }

    /** Draws node [node] with the renderer and puts it in [container] (borrowed). */
    private fun render(container: ComPtr, node: WTreeNode) {
        val current = renderer ?: return
        val path = binding().pathOf(node) ?: return
        val component = current.getTreeCellRendererComponent(
            tree,
            path.getLastPathComponent(),
            tree.isPathSelected(path),
            tree.isExpanded(path),
            binding().isLeaf(path),
            tree.getRowForPath(path),
            hasFocus(container),
        )
        putContent(container, component.uiElement)
        Xaml.setAutomation(container, XamlInterop.IAutomationPropertiesStatics_SetName, node.text)
    }

    /** Puts [element] (removes it if null) in Border.Child of the template root of [container] (borrowed). Does nothing if the root does not exist yet. */
    private fun putContent(container: ComPtr, element: ComPtr?) {
        val content = container.queryInterface(XamlInterop.IID_IContentControl)
        try {
            val root = content.getPtrOrNull(XamlInterop.IContentControl_get_ContentTemplateRoot) ?: return
            val border = root.queryInterfaceOrNull(XamlInterop.IID_IBorder)
            root.release()
            border?.call(XamlInterop.IBorder_put_Child, element?.ptr)
            border?.release()
        } finally {
            content.release()
        }
    }

    private fun hasFocus(container: ComPtr): Boolean {
        val element = container.queryInterface(XamlInterop.IID_IUIElement)
        return try {
            element.getInt(XamlInterop.IUIElement_get_FocusState) != 0
        } finally {
            element.release()
        }
    }
}
