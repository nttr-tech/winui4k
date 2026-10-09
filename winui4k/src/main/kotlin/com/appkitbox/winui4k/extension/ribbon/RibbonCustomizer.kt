package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomGroup
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomization
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel

/**
 * Applies the user's customization ([RibbonCustomization]) to the display (the customization part of Ribbon.State in
 * RibbonSpace).
 *
 * The model is not modified; reordering, hiding, and renaming are applied at display time. Custom tabs and groups are
 * created as display-only models whose items are the existing item models as is (the same model is shown in multiple views).
 */
internal class RibbonCustomizer(private val model: RibbonModel) {
    /** The current customization (a copy). */
    var customization: RibbonCustomization = RibbonCustomization()
        private set

    private val customTabs = mutableListOf<RibbonTabModel>()
    private val customGroups = mutableMapOf<String, MutableList<RibbonGroupModel>>()

    /** Replaces the customization (rebuilds the custom tabs and groups). */
    fun apply(value: RibbonCustomization) {
        val copy = clone(value)
        copy.normalize()
        customization = copy
        customTabs.clear()
        customGroups.clear()
        for (custom in copy.customTabs) {
            val tab = RibbonTabModel(custom.id, custom.label)
            custom.groups.forEach { tab.groups.add(createGroup(it)) }
            customTabs += tab
        }
        for (group in copy.customGroups) {
            val tabId = group.tabId ?: continue
            customGroups.getOrPut(tabId) { mutableListOf() } += createGroup(group)
        }
    }

    private fun createGroup(definition: RibbonCustomGroup): RibbonGroupModel {
        val group = RibbonGroupModel(definition.id, definition.label)
        for (id in definition.itemIds) {
            model.findItem(id)?.let { group.items.add(it) }
        }
        return group
    }

    /** The custom tabs. */
    fun customTabs(): List<RibbonTabModel> = customTabs.toList()

    /** Whether this is a custom tab or group (one created by customization). */
    fun isCustom(tab: RibbonTabModel): Boolean = tab in customTabs

    /** The display name of a tab. */
    fun labelOf(tab: RibbonTabModel): String = tab.id?.let { customization.labels[it] } ?: tab.label ?: tab.id.orEmpty()

    /** The display name of a group. */
    fun labelOf(group: RibbonGroupModel): String? = group.id?.let { customization.labels[it] } ?: group.label

    /** Whether the tab is hidden. */
    fun isHidden(tab: RibbonTabModel): Boolean = tab.id != null && tab.id in customization.hiddenTabIds

    /** Whether the group is hidden. */
    fun isHidden(group: RibbonGroupModel): Boolean = group.id != null && group.id in customization.hiddenGroupIds

    /** Orders the regular tabs in display order (contextual tabs are not included). */
    fun orderTabs(tabs: List<RibbonTabModel>): List<RibbonTabModel> {
        val order = customization.tabOrder
        if (order.isEmpty()) return tabs
        return tabs.withIndex().sortedBy { (index, tab) ->
            val rank = order.indexOf(tab.id.orEmpty())
            if (rank < 0) order.size + index else rank
        }.map { it.value }
    }

    /** The groups shown in a tab (custom groups added, reordered, and hidden ones excluded). */
    fun groupsOf(tab: RibbonTabModel): List<RibbonGroupModel> {
        val all = tab.groups.toList() + tab.id?.let { customGroups[it] }.orEmpty()
        val order = tab.id?.let { customization.groupOrder[it] }.orEmpty()
        val ordered = if (order.isEmpty()) {
            all
        } else {
            all.withIndex().sortedBy { (index, group) ->
                val rank = order.indexOf(group.id.orEmpty())
                if (rank < 0) order.size + index else rank
            }.map { it.value }
        }
        return ordered.filter { !isHidden(it) }
    }

    /** Hides or shows [id] (AutoCAD's "Show Tabs" / "Show Panels"). */
    fun setHidden(id: String, hidden: Boolean, isTab: Boolean): RibbonCustomization {
        val copy = clone(customization)
        val list = if (isTab) copy.hiddenTabIds else copy.hiddenGroupIds
        list.remove(id)
        if (hidden) list += id
        return copy
    }

    companion object {
        /** A deep copy of a customization. */
        fun clone(value: RibbonCustomization): RibbonCustomization = value.deepCopy()
    }
}
