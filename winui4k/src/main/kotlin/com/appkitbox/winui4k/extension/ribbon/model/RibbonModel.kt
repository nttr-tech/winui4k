package com.appkitbox.winui4k.extension.ribbon.model

/**
 * The model of an entire ribbon definition (data separated from the view, like javax.swing.table.TableModel).
 *
 * When passed to [com.appkitbox.winui4k.extension.ribbon.WRibbon], the tabs, groups, items, contextual groups, QAT,
 * tab row items, and Backstage are created from this model and kept in two-way sync
 * (selected tab, display options, check states, values, etc.). Collection changes are applied as diffs.
 */
open class RibbonModel : RibbonObservable() {
    /** The regular tabs and contextual tabs (in display order). */
    val tabs: RibbonList<RibbonTabModel> = RibbonList()

    /** The contextual tab groups. */
    val contextualGroups: RibbonList<RibbonContextualGroupModel> = RibbonList()

    /** The items of the Quick Access Toolbar. */
    val quickAccessItems: RibbonList<RibbonItemModel> = RibbonList()

    /** The commands offered as candidates in the QAT customization menu. */
    val quickAccessCandidates: RibbonList<RibbonItemModel> = RibbonList()

    /** The items at the right end of the tab row ([Comments], [Share], [Editing mode]). */
    val tabStripItems: RibbonList<RibbonItemModel> = RibbonList()

    /** The Backstage (shown by [File]). */
    val backstage: RibbonBackstageModel = RibbonBackstageModel()

    /** The id of the selected tab. */
    var selectedTabId: String? by observable(null) { old, new ->
        firePropertyChange("selectedTab", tabs.firstOrNull { it.id == old }, tabs.firstOrNull { it.id == new })
    }

    /** Classic or simplified. */
    var displayMode: RibbonDisplayMode by observable(RibbonDisplayMode.CLASSIC)

    /** Always show, show tabs only, full screen, and so on. */
    var visibilityMode: RibbonVisibilityMode by observable(RibbonVisibilityMode.ALWAYS_SHOW)

    /** The spacing density. */
    var density: RibbonDensity by observable(RibbonDensity.COMFORTABLE)

    /** The position of the QAT. */
    var quickAccessPosition: RibbonQuickAccessPosition by observable(RibbonQuickAccessPosition.ABOVE_RIBBON)

    /** Whether to show the QAT. */
    var isQuickAccessVisible: Boolean by observable(true)

    /** Whether to show the QAT customization drop-down button. */
    var showQuickAccessCustomizeButton: Boolean by observable(true)

    /** Whether to show labels in the QAT (Office's "Show command labels"). */
    var showQuickAccessLabels: Boolean by observable(false)

    /** The minimize behavior (Office's show tabs only, or AutoCAD's panel titles / buttons / cycle). */
    var minimizeBehavior: RibbonMinimizeBehavior by observable(RibbonMinimizeBehavior.TABS)

    /** Whether to show the panel (group) title below each group. */
    var showGroupCaptions: Boolean by observable(true)

    /** The label of the application ([File]) button (null means "File" in the current language). */
    var applicationButtonLabel: String? by observable(null)

    /** Whether to show the application ([File]) button. */
    var isApplicationButtonVisible: Boolean by observable(true)

    /** The document / window title shown by [com.appkitbox.winui4k.extension.ribbon.WRibbonTitleBar]. */
    var title: String? by observable(null)

    /** The command catalog that resolves [RibbonItemModel.commandId]. */
    var commandCatalog: RibbonCommandCatalog? by observable(null)

    /** The selected tab. */
    var selectedTab: RibbonTabModel?
        get() = tabs.firstOrNull { it.id == selectedTabId }
        set(value) {
            selectedTabId = value?.id
        }

    /** Finds a tab by id. */
    fun findTab(id: String): RibbonTabModel? = tabs.firstOrNull { it.id == id }

    /** Finds a contextual group by id. */
    fun findContextualGroup(id: String): RibbonContextualGroupModel? = contextualGroups.firstOrNull { it.id == id }

    /** Finds a group by id across all tabs. */
    fun findGroup(id: String): RibbonGroupModel? = tabs.asSequence().flatMap { it.groups }.firstOrNull { it.id == id }

    /** Finds an item by id (also searching recursively inside button groups and rows). */
    fun findItem(id: String): RibbonItemModel? = enumerateItems().firstOrNull { it.id == id }

    /** Finds any node (tab, group, item, contextual group, or Backstage item) by id. */
    fun findNode(id: String): RibbonNodeModel? = findTab(id) ?: findGroup(id) ?: findItem(id)
        ?: findContextualGroup(id) ?: backstage.items.firstOrNull { it.id == id }

    /** Enumerates all items of all tabs (including slide-outs and nested items) and the tab row items. */
    fun enumerateItems(): Sequence<RibbonItemModel> = sequence {
        for (tab in tabs) {
            for (group in tab.groups) {
                yieldAll(flatten(group.items + group.slideOutItems))
            }
        }
        yieldAll(flatten(tabStripItems))
    }

    /** Enumerates items paired with their tab and group (for search and customization). */
    fun enumerateItemsWithPath(): Sequence<RibbonItemPath> = sequence {
        for (tab in tabs) {
            for (group in tab.groups) {
                for (item in flatten(group.items)) yield(RibbonItemPath(tab, group, item))
            }
        }
    }

    /** The items that reference [commandId] (including the QAT and the tab row). */
    fun findItemsByCommand(commandId: String): List<RibbonItemModel> =
        (enumerateItems() + flatten(quickAccessItems) + flatten(tabStripItems))
            .filter { it.commandId.equals(commandId, ignoreCase = true) }
            .distinct()
            .toList()

    /**
     * All nodes that reference [commandId]: items, menu items (the menus of drop-downs, split buttons, and galleries,
     * and nested submenus), and Backstage items.
     */
    fun findNodesByCommand(commandId: String): List<RibbonNodeModel> {
        val items = (enumerateItems() + flatten(quickAccessItems) + flatten(tabStripItems)).distinct().toList()
        val nodes = items.asSequence() + items.asSequence().flatMap { menuEntries(it) } + backstage.items.asSequence()
        return nodes.distinct().filter { commandIdOf(it).equals(commandId, ignoreCase = true) }.toList()
    }

    /** Sets the enabled state of the items, menu items, and Backstage items that reference [commandId]. */
    fun setCommandEnabled(commandId: String, enabled: Boolean) {
        for (node in findNodesByCommand(commandId)) node.isEnabled = enabled
    }

    /** Sets the checked state of the toggles, check items, and checkable menu items that reference [commandId]. */
    fun setCommandChecked(commandId: String, isChecked: Boolean) {
        for (node in findNodesByCommand(commandId)) {
            when (node) {
                is RibbonToggleButtonModel -> node.isChecked = isChecked
                is RibbonSplitButtonModel -> node.isChecked = isChecked
                is RibbonCheckBoxModel -> node.isChecked = isChecked
                is RibbonMenuItemModel -> node.isChecked = isChecked
            }
        }
    }

    /** Shows or hides a contextual group. Returns false if it is not found. */
    fun setContextualGroupVisible(id: String, visible: Boolean): Boolean {
        val group = findContextualGroup(id) ?: return false
        group.isVisible = visible
        return true
    }

    /** Shows only the specified contextual groups and hides the others (switching according to the selection). */
    fun setActiveContextualGroups(vararg ids: String) {
        for (group in contextualGroups) group.isVisible = group.id in ids
    }

    /** The currently shown tabs (the visible regular tabs and the tabs of the visible contextual groups). */
    val visibleTabs: List<RibbonTabModel>
        get() = tabs.filter { tab ->
            tab.isVisible && (tab.contextualGroupId == null || findContextualGroup(tab.contextualGroupId!!)?.isVisible == true)
        }

    companion object {
        /** Flattens nested containers (button groups and rows). */
        @JvmStatic
        fun flatten(items: Iterable<RibbonItemModel>): Sequence<RibbonItemModel> = sequence {
            for (item in items) {
                yield(item)
                if (item is RibbonButtonGroupModel) yieldAll(flatten(item.items))
            }
        }

        private fun menuEntries(item: RibbonItemModel): Sequence<RibbonNodeModel> {
            val roots = when (item) {
                is RibbonDropDownButtonModel -> item.menuItems
                is RibbonGalleryModel -> item.menuItems
                else -> return emptySequence()
            }
            return flattenMenu(roots)
        }

        private fun flattenMenu(nodes: Iterable<RibbonNodeModel>): Sequence<RibbonNodeModel> = sequence {
            for (node in nodes) {
                yield(node)
                if (node is RibbonMenuItemModel && node.items.isNotEmpty()) yieldAll(flattenMenu(node.items))
            }
        }

        private fun commandIdOf(node: RibbonNodeModel): String? = when (node) {
            is RibbonItemModel -> node.commandId
            is RibbonMenuItemModel -> node.commandId
            is RibbonBackstageItemModel -> node.commandId
            else -> null
        }
    }
}

/** An item paired with its tab and group ([RibbonModel.enumerateItemsWithPath]). */
data class RibbonItemPath(
    /** The tab that contains the item. */
    val tab: RibbonTabModel,
    /** The group that contains the item. */
    val group: RibbonGroupModel,
    /** Items. */
    val item: RibbonItemModel,
)
