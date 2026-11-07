package com.appkitbox.winui4k

/**
 * TreeCellRenderer-like: creates a component that shows one node (row) of a [WTree] ([WTree.cellRenderer]).
 *
 * As with [WListCellRenderer], a WinUI component can be placed in only one place at a time, so unlike Swing,
 * **return a new component on each call**. Called each time a node is assigned to a row (including when a row is reused by scrolling) and
 * when the state of a visible row changes through selection, expansion or changes to model nodes.
 * The expand / collapse glyph (chevron), indentation and selection background are drawn by WinUI's TreeViewItem, so the renderer only needs to create the node's content.
 */
@Suppress("LongParameterList") // The arguments correspond 1:1 to Swing's TreeCellRenderer.getTreeCellRendererComponent
fun interface WTreeCellRenderer {
    /**
     * Returns a component that shows node [value] of [tree]. [value] is the last node of the path (the model node) when mirroring a model,
     * and a [WTreeNode] when assembling nodes. [selected] is whether the path is in the selection model, [expanded] whether the node is expanded,
     * [leaf] whether it is a leaf (the model's isLeaf), [row] the position among visible rows ([WTree.getRowForPath]), and [hasFocus] whether
     * that row has the keyboard focus at the time of the call (it is not called again just because the focus moved).
     */
    fun getTreeCellRendererComponent(
        tree: WTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ): WComponent
}

/**
 * DefaultTreeCellRenderer-like: a renderer that shows nodes with a [WLabel].
 *
 * The text is decided by [getText] ([WTree.convertValueToText] by default; the node's toString()) and the icon by [getIcon] (by default [leafIcon] for a leaf,
 * [openIcon] for an expanded node and [closedIcon] for a collapsed node), and the label decoration is changed by overriding [configure].
 * Unlike Swing, the icons are null by default (WinUI's TreeView does not draw node icons, only the expand / collapse glyph).
 *
 * Mapping from Swing overrides: write `setText(...)` after `super.getTreeCellRendererComponent(...)` and overrides of JTree's `convertValueToText`
 * in [getText], `setIcon(...)` in [getIcon], and decoration such as `setForeground(...)` in an override of [configure]
 * ([WTree.convertValueToText] cannot be overridden). The returned label is newly created on each call, so changing and returning the result of `super` does not work.
 */
@Suppress("LongParameterList") // The arguments correspond 1:1 to Swing's TreeCellRenderer.getTreeCellRendererComponent
open class WDefaultTreeCellRenderer : WTreeCellRenderer {
    /** The icon for leaves (DefaultTreeCellRenderer.getLeafIcon / setLeafIcon). null by default. */
    var leafIcon: WIcon? = null

    /** The icon for expanded nodes (DefaultTreeCellRenderer.getOpenIcon / setOpenIcon). null by default. */
    var openIcon: WIcon? = null

    /** The icon for collapsed nodes (DefaultTreeCellRenderer.getClosedIcon / setClosedIcon). null by default. */
    var closedIcon: WIcon? = null

    override fun getTreeCellRendererComponent(
        tree: WTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ): WComponent {
        val label = WLabel(getText(tree, value, selected, expanded, leaf, row, hasFocus), getIcon(value, expanded, leaf))
        label.verticalAlignmentInParent = VerticalAlignment.CENTER
        configure(label, value, selected, expanded, leaf, row, hasFocus)
        return label
    }

    /** The display string of [value]. [WTree.convertValueToText] by default (the node's toString(); empty for null). */
    protected open fun getText(tree: WTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): String =
        tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus)

    /** The icon for [value]. By default [leafIcon] for a leaf, [openIcon] if expanded, and [closedIcon] if collapsed. */
    protected open fun getIcon(value: Any?, expanded: Boolean, leaf: Boolean): WIcon? = when {
        leaf -> leafIcon
        expanded -> openIcon
        else -> closedIcon
    }

    /** Decorates the created label (text color, weight, etc.). Does nothing by default. */
    protected open fun configure(label: WLabel, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
        // No decoration by default
    }
}
