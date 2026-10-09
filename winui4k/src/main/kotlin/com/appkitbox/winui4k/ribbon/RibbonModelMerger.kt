package com.appkitbox.winui4k.ribbon

/**
 * Merges ribbon models by node id and undoes the merge (plug-ins, add-ins, and MDI child documents).
 *
 * Tabs are merged into tabs, groups into groups, and items into groups. The result is determined by
 * [RibbonNodeModel.mergeAction] and [RibbonNodeModel.order].
 */
class RibbonModelMerger private constructor() {
    private val undo = mutableListOf<() -> Unit>()

    /** Undoes all changes made by the merge (in reverse order). */
    fun unmerge() {
        for (i in undo.indices.reversed()) undo[i]()
        undo.clear()
    }

    private fun mergeCore(target: RibbonModel, source: RibbonModel) {
        mergeList(target.contextualGroups, source.contextualGroups, null)
        mergeList(target.tabs, source.tabs) { existing, tab ->
            mergeList(existing.groups, tab.groups) { group, sourceGroup -> mergeItems(group.items, sourceGroup.items) }
        }
        mergeItems(target.quickAccessItems, source.quickAccessItems)
        mergeItems(target.tabStripItems, source.tabStripItems)
        mergeList(target.backstage.items, source.backstage.items, null)
        for (candidate in source.quickAccessCandidates.toList()) {
            if (target.quickAccessCandidates.none { it.id == candidate.id }) insert(target.quickAccessCandidates, candidate)
        }
        mergeCatalog(target, source)
    }

    /** Copies the descriptions of commands missing from the host so that the plug-in's commands can be resolved by the host. */
    private fun mergeCatalog(target: RibbonModel, source: RibbonModel) {
        val sourceCatalog = source.commandCatalog ?: return
        if (sourceCatalog === target.commandCatalog) return
        val catalog = target.commandCatalog
        if (catalog == null) {
            target.commandCatalog = sourceCatalog
            undo += {
                if (target.commandCatalog === sourceCatalog) target.commandCatalog = null
            }
            return
        }
        for (descriptor in sourceCatalog.commands) {
            if (catalog.find(descriptor.id) == null) {
                catalog.register(descriptor)
                undo += {
                    if (catalog.find(descriptor.id) === descriptor) catalog.unregister(descriptor.id)
                }
            }
        }
    }

    /** Merges a sequence of nodes by id (following [RibbonNodeModel.mergeAction]). */
    private fun <T : RibbonNodeModel> mergeList(target: MutableList<T>, source: List<T>, mergeChildren: ((T, T) -> Unit)?) {
        for (node in source.toList()) {
            val existing = if (node.id.isEmpty()) null else target.firstOrNull { it.id == node.id }
            when {
                node.mergeAction == RibbonMergeAction.REMOVE -> if (existing != null) remove(target, existing)
                node.mergeAction == RibbonMergeAction.REPLACE && existing != null -> replace(target, existing, node)
                node.mergeAction == RibbonMergeAction.MERGE && existing != null -> mergeChildren?.invoke(existing, node)
                // ADD, or MERGE / REPLACE with no existing node
                else -> insert(target, node)
            }
        }
    }

    /** Merges items like the other nodes, and also recurses into containers (button groups, rows, and drop-down menus). */
    private fun mergeItems(target: MutableList<RibbonItemModel>, source: List<RibbonItemModel>) {
        mergeList(target, source) { existing, item ->
            when {
                existing is RibbonButtonGroupModel && item is RibbonButtonGroupModel -> mergeItems(existing.items, item.items)
                existing is RibbonDropDownButtonModel && item is RibbonDropDownButtonModel ->
                    mergeList(existing.menuItems, item.menuItems, null)
                existing is RibbonGalleryModel && item is RibbonGalleryModel -> {
                    mergeList(existing.items, item.items, null)
                    mergeList(existing.menuItems, item.menuItems, null)
                }
            }
        }
    }

    private fun <T : RibbonNodeModel> insert(list: MutableList<T>, node: T) {
        val order = node.order
        var index = list.size
        if (order != 0) {
            val position = list.indexOfFirst { it.order > order }
            if (position >= 0) index = position
        }
        list.add(index, node)
        undo += { list.remove(node) }
    }

    private fun <T> remove(list: MutableList<T>, node: T) {
        val index = list.indexOf(node)
        if (index < 0) return
        // Remember the previous neighbor so that the node can return to its original position even if other merges
        // reorder the sequence
        val previous = if (index > 0) list[index - 1] else null
        list.removeAt(index)
        undo += {
            val anchor = if (previous == null) -1 else list.indexOf(previous)
            list.add(if (anchor >= 0) anchor + 1 else minOf(index, list.size), node)
        }
    }

    private fun <T> replace(list: MutableList<T>, existing: T, replacement: T) {
        val index = list.indexOf(existing)
        list[index] = replacement
        undo += {
            val current = list.indexOf(replacement)
            if (current >= 0) list[current] = existing
        }
    }

    companion object {
        /** Merges [source] into [target]. Call [unmerge] on the returned value to undo it. */
        @JvmStatic
        fun merge(target: RibbonModel, source: RibbonModel): RibbonModelMerger {
            val merger = RibbonModelMerger()
            merger.mergeCore(target, source)
            return merger
        }
    }
}
