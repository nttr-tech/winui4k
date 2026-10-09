package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WMenuFlyout
import com.appkitbox.winui4k.WMenuFlyoutItem
import com.appkitbox.winui4k.WMenuFlyoutSeparator
import com.appkitbox.winui4k.WMenuFlyoutSubItem
import com.appkitbox.winui4k.WRadioMenuFlyoutItem
import com.appkitbox.winui4k.WToggleMenuFlyoutItem
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomizePage
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDisplayMode
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMinimizeBehavior
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonQuickAccessPosition
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonVisibilityMode

/**
 * The ribbon's menus (the menu parts of Ribbon.Menus / Ribbon.Panels / Ribbon.QuickAccess in RibbonSpace):
 * the right-click menus of items and groups, "Ribbon Display Options", AutoCAD's minimize behavior, "Show Tabs" /
 * "Show Panels", and "Customize Quick Access Toolbar".
 */
internal class RibbonMenuBuilder(private val ribbon: WRibbon) {
    private val strings get() = RibbonStrings.current

    private fun item(text: String, glyph: String? = null, action: () -> Unit): WMenuFlyoutItem {
        val entry = WMenuFlyoutItem(text)
        glyph?.let { RibbonMenus.setIcon(entry.inspectable, com.appkitbox.winui4k.internal.winui.XamlInterop.IMenuFlyoutItem_put_Icon, RibbonIcon.glyph(it)) }
        entry.addActionListener(action)
        return entry
    }

    private fun toggle(text: String, checked: Boolean, action: (Boolean) -> Unit): WToggleMenuFlyoutItem {
        val entry = WToggleMenuFlyoutItem(text)
        entry.isChecked = checked
        entry.addActionListener { action(entry.isChecked) }
        return entry
    }

    private fun radio(text: String, group: String, checked: Boolean, glyph: String? = null, action: () -> Unit): WRadioMenuFlyoutItem {
        val entry = WRadioMenuFlyoutItem(text, group)
        entry.isChecked = checked
        glyph?.let { RibbonMenus.setIcon(entry.inspectable, com.appkitbox.winui4k.internal.winui.XamlInterop.IMenuFlyoutItem_put_Icon, RibbonIcon.glyph(it)) }
        entry.addActionListener(action)
        return entry
    }

    private fun menu(): WMenuFlyout = WMenuFlyout().also { RibbonMenus.applyMenuStyle(it) }

    /**
     * The right-click menu of an item, group, or tab: add to / remove from the QAT, float the panel, the tabs and panels
     * to show, customize the QAT, the QAT position, customize the ribbon, and collapse the ribbon.
     */
    fun contextMenu(target: RibbonNodeModel?, group: RibbonGroupModel?): WMenuFlyout {
        val m = menu()
        val model = ribbon.model
        if (target is RibbonItemModel && target.canAddToQuickAccess) {
            if (target in model.quickAccessItems) {
                m.add(item(strings.removeFromQuickAccessToolbar) { ribbon.removeFromQuickAccess(target) })
            } else {
                m.add(item(strings.addToQuickAccessToolbar, "") { ribbon.addToQuickAccess(target) })
            }
            m.add(WMenuFlyoutSeparator())
        }
        addPanelEntries(m, group)
        if (ribbon.canCustomize) {
            m.add(item(strings.customizeQuickAccessToolbar + "...") { ribbon.showCustomizeDialog(RibbonCustomizePage.QUICK_ACCESS_TOOLBAR) })
        }
        val above = model.quickAccessPosition == RibbonQuickAccessPosition.ABOVE_RIBBON
        m.add(item(if (above) strings.showBelowRibbon else strings.showAboveRibbon) { ribbon.toggleQuickAccessPosition() })
        if (ribbon.canCustomize) {
            m.add(WMenuFlyoutSeparator())
            m.add(item(strings.customizeRibbon) { ribbon.showCustomizeDialog(RibbonCustomizePage.RIBBON) })
        }
        if (ribbon.isCollapsible) {
            val always = model.visibilityMode == RibbonVisibilityMode.ALWAYS_SHOW
            m.add(
                item(if (always) strings.collapseRibbon else strings.pinRibbon) {
                    model.visibilityMode = if (always) ribbon.minimizedState() else RibbonVisibilityMode.ALWAYS_SHOW
                },
            )
        }
        return m
    }

    private fun addPanelEntries(m: WMenuFlyout, group: RibbonGroupModel?) {
        var added = false
        val floating = ribbon.floatingGroups()
        if (ribbon.canFloatGroups && group != null) {
            val isFloating = group in floating
            m.add(
                if (isFloating) {
                    item(strings.returnPanelToRibbon, "") { ribbon.returnGroupToRibbon(group) }
                } else {
                    item(strings.floatPanel, "") { ribbon.floatGroup(group) }
                },
            )
            added = true
        }
        if (ribbon.canFloatGroups && floating.size > (if (group != null && group in floating) 1 else 0)) {
            m.add(item(strings.returnPanelsToRibbon) { ribbon.returnAllPanelsToRibbon() })
            added = true
        }
        if (ribbon.isVisibilityMenuEnabled) {
            m.add(showTabsMenu())
            ribbon.selectedTab?.let { tab -> if (tab.groups.isNotEmpty()) m.add(showPanelsMenu(tab)) }
            m.add(toggle(strings.showGroupTitles, ribbon.model.showGroupCaptions) { ribbon.model.showGroupCaptions = it })
            added = true
        }
        if (added) m.add(WMenuFlyoutSeparator())
    }

    /** "Show Tabs": a check item per regular tab (unchecking hides it via customization). */
    fun showTabsMenu(): WMenuFlyoutSubItem {
        val sub = WMenuFlyoutSubItem(strings.showTabs)
        for (tab in ribbon.model.tabs.filter { !it.isContextual && it.id != null }) {
            val id = tab.id!!
            sub.add(toggle(ribbon.tabLabel(tab), id !in ribbon.customization.hiddenTabIds) { ribbon.setHiddenByUser(id, !it, isTab = true) })
        }
        return sub
    }

    /** "Show Panels": a check item per group of [tab]. */
    fun showPanelsMenu(tab: RibbonTabModel): WMenuFlyoutSubItem {
        val sub = WMenuFlyoutSubItem(strings.showPanels)
        for (group in tab.groups.filter { it.id != null }) {
            val id = group.id!!
            sub.add(toggle(group.label ?: id, id !in ribbon.customization.hiddenGroupIds) { ribbon.setHiddenByUser(id, !it, isTab = false) })
        }
        return sub
    }

    /**
     * "Ribbon Display Options": full screen, panel titles / buttons, tabs only, always show, classic / simplified, and
     * QAT visibility.
     */
    fun displayOptionsMenu(): WMenuFlyout {
        val m = menu()
        val model = ribbon.model
        fun mode(text: String, mode: RibbonVisibilityMode, glyph: String) =
            m.add(radio(text, "visibility", model.visibilityMode == mode, glyph) { model.visibilityMode = mode })
        mode(strings.fullScreenMode, RibbonVisibilityMode.FULL_SCREEN, "")
        val panels = model.minimizeBehavior != RibbonMinimizeBehavior.TABS || ribbon.isMinimizeButtonVisible ||
            (WRibbon.isMinimizedMode(model.visibilityMode) && model.visibilityMode != RibbonVisibilityMode.TABS_ONLY)
        if (panels) {
            mode(strings.showPanelTitles, RibbonVisibilityMode.PANEL_TITLES, "")
            mode(strings.showPanelButtons, RibbonVisibilityMode.PANEL_BUTTONS, "")
        }
        mode(strings.showTabsOnly, RibbonVisibilityMode.TABS_ONLY, "")
        mode(strings.alwaysShowRibbon, RibbonVisibilityMode.ALWAYS_SHOW, "")
        if (ribbon.isSimplifiedModeAvailable) {
            m.add(WMenuFlyoutSeparator())
            m.add(radio(strings.useClassicRibbon, "layout", model.displayMode == RibbonDisplayMode.CLASSIC) { model.displayMode = RibbonDisplayMode.CLASSIC })
            m.add(radio(strings.useSimplifiedRibbon, "layout", model.displayMode == RibbonDisplayMode.SIMPLIFIED) { model.displayMode = RibbonDisplayMode.SIMPLIFIED })
        }
        m.add(WMenuFlyoutSeparator())
        m.add(toggle(strings.showQuickAccessToolbar, model.isQuickAccessVisible) { model.isQuickAccessVisible = it })
        return m
    }

    /** The menu for AutoCAD's minimize behavior (minimize to tabs / panel titles / panel buttons, cycle through all). */
    fun minimizeBehaviorMenu(): WMenuFlyout {
        val m = menu()
        val model = ribbon.model
        val entries = listOf(
            strings.minimizeToTabs to RibbonMinimizeBehavior.TABS,
            strings.minimizeToPanelTitles to RibbonMinimizeBehavior.PANEL_TITLES,
            strings.minimizeToPanelButtons to RibbonMinimizeBehavior.PANEL_BUTTONS,
            strings.cycleThroughAll to RibbonMinimizeBehavior.CYCLE_ALL,
        )
        for ((text, behavior) in entries) {
            m.add(radio(text, "minimize", model.minimizeBehavior == behavior) { model.minimizeBehavior = behavior })
        }
        return m
    }

    /** "Customize Quick Access Toolbar": candidate check items, More Commands, position, labels, and hide. */
    fun quickAccessCustomizeMenu(): WMenuFlyout {
        val m = menu()
        val model = ribbon.model
        m.add(WMenuFlyoutItem(strings.customizeQuickAccessToolbar).also { it.isEnabled = false })
        val candidates = (model.quickAccessCandidates + model.quickAccessItems).distinct()
        for (candidate in candidates) {
            m.add(
                toggle(candidate.label?.replace('\n', ' ') ?: candidate.id.orEmpty(), candidate in model.quickAccessItems) { checked ->
                    if (checked) ribbon.addToQuickAccess(candidate) else ribbon.removeFromQuickAccess(candidate)
                },
            )
        }
        if (candidates.isNotEmpty()) m.add(WMenuFlyoutSeparator())
        if (ribbon.canCustomize) m.add(item(strings.moreCommands) { ribbon.showCustomizeDialog(RibbonCustomizePage.QUICK_ACCESS_TOOLBAR) })
        val above = model.quickAccessPosition == RibbonQuickAccessPosition.ABOVE_RIBBON
        m.add(item(if (above) strings.showBelowRibbon else strings.showAboveRibbon) { ribbon.toggleQuickAccessPosition() })
        m.add(toggle(strings.showCommandLabels, model.showQuickAccessLabels) { model.showQuickAccessLabels = it })
        m.add(item(strings.hideQuickAccessToolbar) { model.isQuickAccessVisible = false })
        return m
    }
}
