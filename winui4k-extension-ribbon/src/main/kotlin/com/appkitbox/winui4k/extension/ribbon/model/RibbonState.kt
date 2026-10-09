package com.appkitbox.winui4k.extension.ribbon.model

/**
 * The savable user state of a ribbon: the selected tab, display options, the Quick Access Toolbar, and customizations.
 * [RibbonStateSerializer] converts it to JSON (compatible with RibbonSpace's JSON).
 *
 * A null collection ([quickAccessItemIds] and others) means "keep the current value when applied".
 */
class RibbonState {
    /** The schema version of the serialized data. */
    var schemaVersion: Int = CURRENT_SCHEMA_VERSION

    /** The id of the selected tab. */
    var selectedTabId: String? = null

    /** Classic or simplified. */
    var displayMode: RibbonDisplayMode = RibbonDisplayMode.CLASSIC

    /** The ribbon display options. */
    var visibilityMode: RibbonVisibilityMode = RibbonVisibilityMode.ALWAYS_SHOW

    /** The spacing density. */
    var density: RibbonDensity = RibbonDensity.COMFORTABLE

    /** The position of the QAT. */
    var quickAccessPosition: RibbonQuickAccessPosition = RibbonQuickAccessPosition.ABOVE_RIBBON

    /** Whether to show the QAT. */
    var isQuickAccessVisible: Boolean = true

    /** Whether to show labels in the QAT. */
    var showQuickAccessLabels: Boolean = false

    /** The minimize behavior (tabs, panel titles, panel buttons, or cycle). */
    var minimizeBehavior: RibbonMinimizeBehavior = RibbonMinimizeBehavior.TABS

    /** Whether to show panel (group) titles. */
    var showGroupCaptions: Boolean = true

    /** The panels (groups) floated outside the ribbon and their positions. If null, the current floating state is kept. */
    var floatingGroups: MutableList<RibbonFloatingGroupState>? = null

    /** The ids of the QAT items (in order). If null, the app's default is kept. */
    var quickAccessItemIds: MutableList<String>? = null

    /** The user customizations of tabs and groups. */
    var customization: RibbonCustomization = RibbonCustomization()

    /** The ids of recently used search results (newest first). If null, the current list is kept. */
    var recentSearchIds: MutableList<String>? = null

    /** Recently used custom colors (#RRGGBB / #AARRGGBB). If null, the current list is kept. */
    var recentColors: MutableList<String>? = null

    /** Removes null elements from hand-edited or old JSON. */
    fun normalize() {
        customization.normalize()
        quickAccessItemIds?.removeAll { it.isEmpty() }
        recentSearchIds?.removeAll { it.isEmpty() }
        recentColors?.removeAll { it.isEmpty() }
        floatingGroups?.removeAll { it.groupId.isEmpty() }
    }

    /** A deep copy (such as a working copy for the customization dialog). */
    fun deepCopy(): RibbonState = RibbonState().also { copy ->
        copy.schemaVersion = schemaVersion
        copy.selectedTabId = selectedTabId
        copy.displayMode = displayMode
        copy.visibilityMode = visibilityMode
        copy.density = density
        copy.quickAccessPosition = quickAccessPosition
        copy.isQuickAccessVisible = isQuickAccessVisible
        copy.showQuickAccessLabels = showQuickAccessLabels
        copy.minimizeBehavior = minimizeBehavior
        copy.showGroupCaptions = showGroupCaptions
        copy.floatingGroups = floatingGroups?.mapTo(mutableListOf()) { it.deepCopy() }
        copy.quickAccessItemIds = quickAccessItemIds?.toMutableList()
        copy.customization = customization.deepCopy()
        copy.recentSearchIds = recentSearchIds?.toMutableList()
        copy.recentColors = recentColors?.toMutableList()
    }

    companion object {
        /** The current schema version. */
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

/** User customizations of the ribbon structure ("Customize the Ribbon"). */
class RibbonCustomization {
    /** The tab ids in the user's order (tabs not listed follow at the end in their relative order). */
    var tabOrder: MutableList<String> = mutableListOf()

    /**
     * The order of groups for each tab (tab id → group ids in the user's order). Groups not listed follow in their
     * relative order, and user-created groups on built-in tabs follow the built-in groups.
     */
    var groupOrder: MutableMap<String, MutableList<String>> = linkedMapOf()

    /** The ids of hidden tabs. */
    var hiddenTabIds: MutableList<String> = mutableListOf()

    /** The ids of hidden groups. */
    var hiddenGroupIds: MutableList<String> = mutableListOf()

    /** Renamed nodes (id → label). */
    var labels: MutableMap<String, String> = linkedMapOf()

    /** User-created tabs. */
    var customTabs: MutableList<RibbonCustomTab> = mutableListOf()

    /** User-created groups placed on built-in tabs. */
    var customGroups: MutableList<RibbonCustomGroup> = mutableListOf()

    /** Whether nothing is customized. */
    val isEmpty: Boolean
        get() = tabOrder.isEmpty() && groupOrder.isEmpty() && hiddenTabIds.isEmpty() && hiddenGroupIds.isEmpty() &&
            labels.isEmpty() && customTabs.isEmpty() && customGroups.isEmpty()

    /** Removes empty elements from hand-edited or old JSON. */
    fun normalize() {
        for (order in groupOrder.values) order.removeAll { it.isEmpty() }
        tabOrder.removeAll { it.isEmpty() }
        hiddenTabIds.removeAll { it.isEmpty() }
        hiddenGroupIds.removeAll { it.isEmpty() }
        customTabs.removeAll { it.id.isEmpty() }
        customGroups.removeAll { it.id.isEmpty() }
        for (tab in customTabs) {
            tab.groups.removeAll { it.id.isEmpty() }
            tab.groups.forEach { it.normalize() }
        }
        customGroups.forEach { it.normalize() }
    }

    /** A deep copy. */
    fun deepCopy(): RibbonCustomization = RibbonCustomization().also { copy ->
        copy.tabOrder = tabOrder.toMutableList()
        groupOrder.forEach { (tab, order) -> copy.groupOrder[tab] = order.toMutableList() }
        copy.hiddenTabIds = hiddenTabIds.toMutableList()
        copy.hiddenGroupIds = hiddenGroupIds.toMutableList()
        copy.labels = LinkedHashMap(labels)
        copy.customTabs = customTabs.mapTo(mutableListOf()) { it.deepCopy() }
        copy.customGroups = customGroups.mapTo(mutableListOf()) { it.deepCopy() }
    }
}

/** A user-created tab. */
class RibbonCustomTab @JvmOverloads constructor(
    /** The id (by convention, starts with "custom."). */
    var id: String = "",
    /** The label. */
    var label: String = "",
) {
    /** The groups. */
    var groups: MutableList<RibbonCustomGroup> = mutableListOf()

    /** A deep copy. */
    fun deepCopy(): RibbonCustomTab = RibbonCustomTab(id, label).also { copy ->
        copy.groups = groups.mapTo(mutableListOf()) { it.deepCopy() }
    }
}

/** A user-created group that holds references to existing commands. */
class RibbonCustomGroup @JvmOverloads constructor(
    /** The id. */
    var id: String = "",
    /** The label. */
    var label: String = "",
    /** The tab the group is placed on (for a group placed on a built-in tab). */
    var tabId: String? = null,
) {
    /** The ids of the items shown in the group (copies of existing ribbon items). */
    var itemIds: MutableList<String> = mutableListOf()

    internal fun normalize() {
        itemIds.removeAll { it.isEmpty() }
    }

    /** A deep copy. */
    fun deepCopy(): RibbonCustomGroup = RibbonCustomGroup(id, label, tabId).also { copy ->
        copy.itemIds = itemIds.toMutableList()
    }
}

/** A panel (group) floated outside the ribbon. */
class RibbonFloatingGroupState @JvmOverloads constructor(
    /** The id of the group. */
    var groupId: String = "",
    /** The left edge in window coordinates. */
    var x: Double = 0.0,
    /** The top edge in window coordinates. */
    var y: Double = 0.0,
) {
    /** A deep copy. */
    fun deepCopy(): RibbonFloatingGroupState = RibbonFloatingGroupState(groupId, x, y)
}
