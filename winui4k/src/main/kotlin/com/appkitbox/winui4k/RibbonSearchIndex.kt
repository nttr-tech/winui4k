package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonBackstageItemModel
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel
import com.appkitbox.winui4k.ribbon.RibbonComboBoxModel
import com.appkitbox.winui4k.ribbon.RibbonCommandDescriptor
import com.appkitbox.winui4k.ribbon.RibbonDropDownButtonModel
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel
import com.appkitbox.winui4k.ribbon.RibbonGridPickerModel
import com.appkitbox.winui4k.ribbon.RibbonGroupState
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonSearchEntry
import com.appkitbox.winui4k.ribbon.RibbonSliderModel
import com.appkitbox.winui4k.ribbon.RibbonSpinnerModel
import com.appkitbox.winui4k.ribbon.RibbonState
import com.appkitbox.winui4k.ribbon.RibbonStrings
import com.appkitbox.winui4k.ribbon.RibbonTextBoxModel
import com.appkitbox.winui4k.ribbon.RibbonVisibilityMode

/** The index and execution for the ribbon's command search (Ribbon.Search in RibbonSpace). */
internal object RibbonSearchIndex {
    /**
     * Builds search entries from the items of the shown tabs, the Backstage, the catalog's commands, and the additional
     * entries.
     */
    fun build(ribbon: WRibbon): List<RibbonSearchEntry> {
        val entries = mutableListOf<RibbonSearchEntry>()
        val seen = HashSet<String>()
        val candidates = itemEntries(ribbon) + backstageEntries(ribbon)
        candidates.filterTo(entries) { seen.add(it.id) }
        commandEntries(ribbon, entries).filterTo(entries) { seen.add(it.id) }
        ribbon.additionalSearchEntries.filterTo(entries) { seen.add(it.id) }
        return entries
    }

    /** The items of the shown tabs and groups (including nested items and expanded panels). */
    private fun itemEntries(ribbon: WRibbon): List<RibbonSearchEntry> = ribbon.visibleTabs.flatMap { tab ->
        ribbon.customizer.groupsOf(tab).filter { it.isVisible }.flatMap { group ->
            RibbonModel.flatten(group.items + group.slideOutItems)
                .filter { !it.label.isNullOrBlank() && it.isVisible }
                .map { item ->
                    val label = item.label!!
                    RibbonSearchEntry(
                        id = item.id ?: "${tab.id}/${group.id}/$label",
                        label = label.replace('\n', ' '),
                        path = "${ribbon.tabLabel(tab)} › ${ribbon.customizer.labelOf(group).orEmpty()}",
                        description = item.screenTip?.description ?: item.description,
                        keywords = item.keywords.toList().ifEmpty { null },
                        shortcut = item.shortcut,
                        target = item,
                        isEnabled = item.isEnabled,
                    )
                }.toList()
        }
    }

    private fun backstageEntries(ribbon: WRibbon): List<RibbonSearchEntry> {
        val file = ribbon.model.applicationButtonLabel ?: RibbonStrings.current.file
        return ribbon.model.backstage.items.filter { it.isVisible && !it.label.isNullOrEmpty() }.map { item ->
            RibbonSearchEntry("backstage/" + (item.id ?: item.label), item.label!!, file, target = item, isEnabled = item.isEnabled)
        }
    }

    /** The catalog's commands that are not already indexed as items. */
    private fun commandEntries(ribbon: WRibbon, existing: List<RibbonSearchEntry>): List<RibbonSearchEntry> {
        val catalog = ribbon.model.commandCatalog ?: return emptyList()
        return catalog.commands.filter { command ->
            existing.none { (it.target as? RibbonItemModel)?.commandId.equals(command.id, ignoreCase = true) }
        }.map { command ->
            RibbonSearchEntry(
                "command/" + command.id,
                command.label,
                command.category,
                command.description,
                command.keywords.toList(),
                command.shortcut,
                command,
                command.isEnabled,
            )
        }
    }

    /** Runs a search result. For inputs, galleries, and drop-downs, opens their tab so they can be operated. */
    fun execute(ribbon: WRibbon, entry: RibbonSearchEntry): Boolean {
        ribbon.searchEngine.markUsed(entry.id)
        return when (val target = entry.target) {
            is RibbonBackstageItemModel -> {
                ribbon.isBackstageOpen = true
                ribbon.invokeBackstageItem(target)
                true
            }
            is RibbonCommandDescriptor -> ribbon.model.commandCatalog?.execute(target.id) ?: false
            is Runnable -> {
                target.run()
                true
            }
            is RibbonItemModel -> executeItem(ribbon, target)
            else -> false
        }
    }

    private fun executeItem(ribbon: WRibbon, item: RibbonItemModel): Boolean {
        if (!needsUi(item)) return ribbon.host.findView(item)?.invoke() ?: false
        val path = ribbon.model.enumerateItemsWithPath().firstOrNull { it.item === item }
        if (path != null) {
            ribbon.selectedTab = path.tab
            if (ribbon.model.visibilityMode == RibbonVisibilityMode.TABS_ONLY) ribbon.openMinimizedPopup()
        }
        WinUiUtilities.invokeLater {
            val view = ribbon.host.findView(item) ?: return@invokeLater
            ribbon.host.groupOf(view)?.let { group ->
                if (group.state == RibbonGroupState.COLLAPSED && !group.isSimplified) group.openPopup()
            }
            WinUiUtilities.invokeLater { view.onKeyTip() }
        }
        return true
    }

    /** Whether the item needs UI to be operated (inputs, galleries, drop-downs). */
    private fun needsUi(item: RibbonItemModel): Boolean = item is RibbonComboBoxModel || item is RibbonSpinnerModel ||
        item is RibbonTextBoxModel || item is RibbonSliderModel || item is RibbonGalleryModel || item is RibbonDropDownButtonModel ||
        item is RibbonGridPickerModel || item is RibbonColorPickerModel
}

/** Capturing and restoring the ribbon's state (Ribbon.State in RibbonSpace). */
internal object RibbonStatePersistence {
    /** Captures the state that can be saved. */
    fun capture(ribbon: WRibbon): RibbonState {
        val model = ribbon.model
        return RibbonState().also { state ->
            state.selectedTabId = ribbon.selectedTab?.id
            state.displayMode = model.displayMode
            state.visibilityMode = model.visibilityMode
            state.density = model.density
            state.quickAccessPosition = model.quickAccessPosition
            state.isQuickAccessVisible = model.isQuickAccessVisible
            state.showQuickAccessLabels = model.showQuickAccessLabels
            state.quickAccessItemIds = ribbon.quickAccessItemIds().toMutableList()
            state.customization = ribbon.customization
            state.recentSearchIds = ribbon.searchEngine.recent.toMutableList()
            state.minimizeBehavior = model.minimizeBehavior
            state.showGroupCaptions = model.showGroupCaptions
            state.floatingGroups = ribbon.host.allGroupViews().filter { it.isFloating && it.model.id != null }
                .map { com.appkitbox.winui4k.ribbon.RibbonFloatingGroupState(it.model.id!!, it.floatingX, it.floatingY) }
                .toMutableList()
            state.recentColors = ribbon.allItemModels().filterIsInstance<RibbonColorPickerModel>()
                .flatMap { it.recentColors }.distinct().map { it.toHex() }.toMutableList()
        }
    }

    /** Restores the state. */
    fun apply(ribbon: WRibbon, state: RibbonState) {
        state.normalize()
        val model = ribbon.model
        ribbon.applyCustomization(state.customization)
        state.recentSearchIds?.let { ribbon.searchEngine.setRecent(it) }
        model.displayMode = state.displayMode
        model.minimizeBehavior = state.minimizeBehavior
        model.showGroupCaptions = state.showGroupCaptions
        model.visibilityMode = state.visibilityMode
        model.density = state.density
        model.quickAccessPosition = state.quickAccessPosition
        model.isQuickAccessVisible = state.isQuickAccessVisible
        model.showQuickAccessLabels = state.showQuickAccessLabels
        state.quickAccessItemIds?.let { ids -> applyQuickAccess(model, ids) }
        state.selectedTabId?.let { ribbon.selectTab(it) }
        state.floatingGroups?.let { floating ->
            for (group in ribbon.host.allGroupViews()) {
                val saved = floating.firstOrNull { it.groupId == group.model.id }
                if (saved == null) group.returnToRibbon() else group.float(saved.x, saved.y)
            }
        }
        state.recentColors?.let { colors ->
            val parsed = colors.mapNotNull { com.appkitbox.winui4k.ribbon.RibbonColor.tryParse(it) }
            for (picker in ribbon.allItemModels().filterIsInstance<RibbonColorPickerModel>()) {
                picker.recentColors.clear()
                parsed.take(RibbonColorPickerModel.MAX_RECENT_COLORS).forEach { picker.recentColors.add(it) }
            }
        }
    }

    /** Reorders the QAT in place (items that remain are kept as is; only items that are gone are removed). */
    private fun applyQuickAccess(model: RibbonModel, ids: List<String>) {
        val desired = ids.mapNotNull { id -> model.quickAccessItems.firstOrNull { it.id == id } ?: model.findItem(id) }.distinct()
        model.quickAccessItems.filter { it !in desired }.forEach { model.quickAccessItems.remove(it) }
        for ((index, item) in desired.withIndex()) {
            val current = model.quickAccessItems.indexOf(item)
            when {
                current < 0 -> model.quickAccessItems.add(index, item)
                current != index -> {
                    model.quickAccessItems.removeAt(current)
                    model.quickAccessItems.add(index, item)
                }
            }
        }
    }
}
