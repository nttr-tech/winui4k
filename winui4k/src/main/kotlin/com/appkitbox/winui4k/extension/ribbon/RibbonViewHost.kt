package com.appkitbox.winui4k.extension.ribbon

import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WPopup
import com.appkitbox.winui4k.WinUiUtilities
import com.appkitbox.winui4k.Xaml
import com.appkitbox.winui4k.XamlElement
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCommandCatalog
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDisplayMode
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMetrics
import com.appkitbox.winui4k.extension.ribbon.model.RibbonNodeModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonPanelPresentation
import com.appkitbox.winui4k.extension.ribbon.model.RibbonReductionStrategy
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonVisibilityMode
import com.appkitbox.winui4k.internal.winui.XamlInterop

/** The XAML of the ribbon root (the Ribbon template of Ribbon.xaml in RibbonSpace). */
internal object RibbonRootXaml {
    const val XAML: String = "<Grid Background=\"{ThemeResource RibbonChromeBackgroundBrush}\" VerticalAlignment=\"Top\">" +
        "<Grid.RowDefinitions><RowDefinition Height=\"Auto\" /><RowDefinition Height=\"Auto\" /><RowDefinition Height=\"Auto\" />" +
        "<RowDefinition Height=\"Auto\" /><RowDefinition Height=\"Auto\" /></Grid.RowDefinitions>" +
        "<Canvas x:Name=\"PART_Measure\" Width=\"0\" Height=\"0\" Opacity=\"0\" IsHitTestVisible=\"False\" HorizontalAlignment=\"Left\" VerticalAlignment=\"Top\" />" +
        "<Border x:Name=\"PART_QuickAccessAboveHost\" Grid.Row=\"0\" Padding=\"6,2,6,0\" Visibility=\"Collapsed\" HorizontalAlignment=\"Left\" />" +
        "<Grid x:Name=\"PART_TabRow\" Grid.Row=\"1\" Padding=\"{ThemeResource RibbonTabRowPadding}\" MinHeight=\"30\">" +
        "<Grid.ColumnDefinitions><ColumnDefinition Width=\"Auto\" /><ColumnDefinition Width=\"Auto\" /><ColumnDefinition Width=\"*\" />" +
        "<ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
        "<Button x:Name=\"PART_ApplicationButton\" Style=\"{StaticResource RibbonApplicationButtonStyle}\" />" +
        "<Border x:Name=\"PART_StartContent\" Grid.Column=\"1\" VerticalAlignment=\"Center\" />" +
        "<Border x:Name=\"PART_TabStripHost\" Grid.Column=\"2\" HorizontalAlignment=\"Stretch\" />" +
        "<StackPanel Grid.Column=\"3\" Orientation=\"Horizontal\" Spacing=\"4\" VerticalAlignment=\"Center\" Margin=\"8,0,0,0\">" +
        "<StackPanel x:Name=\"PART_TabStripItemsHost\" Orientation=\"Horizontal\" Spacing=\"4\" VerticalAlignment=\"Center\" />" +
        "<Border x:Name=\"PART_EndContent\" VerticalAlignment=\"Center\" />" +
        "<StackPanel x:Name=\"PART_MinimizeButtons\" Orientation=\"Horizontal\" Visibility=\"Collapsed\">" +
        "<Button x:Name=\"PART_MinimizeButton\" Style=\"{StaticResource RibbonChromeButtonStyle}\" Width=\"24\" Height=\"22\" Padding=\"0\" />" +
        "<Button x:Name=\"PART_MinimizeBehaviorButton\" Style=\"{StaticResource RibbonChromeButtonStyle}\" Width=\"14\" Height=\"22\" Padding=\"0\">" +
        "<FontIcon Glyph=\"&#xE70D;\" FontSize=\"8\" /></Button></StackPanel></StackPanel></Grid>" +
        "<Border x:Name=\"PART_CommandBar\" Grid.Row=\"2\" Margin=\"{ThemeResource RibbonCommandBarMargin}\" " +
        "Background=\"{ThemeResource RibbonCommandBarBackgroundBrush}\" BorderBrush=\"{ThemeResource RibbonCommandBarBorderBrush}\" " +
        "BorderThickness=\"{ThemeResource RibbonCommandBarBorderThickness}\" CornerRadius=\"{ThemeResource RibbonCommandBarCornerRadius}\">" +
        "<Grid><Grid.ColumnDefinitions><ColumnDefinition Width=\"*\" /><ColumnDefinition Width=\"Auto\" /></Grid.ColumnDefinitions>" +
        "<Grid x:Name=\"PART_TabContentHost\" Padding=\"2,1,0,1\" />" +
        "<Button x:Name=\"PART_DisplayOptionsButton\" Grid.Column=\"1\" Style=\"{StaticResource RibbonChromeButtonStyle}\" VerticalAlignment=\"Bottom\" " +
        "Width=\"24\" Height=\"22\" Padding=\"0\" Margin=\"0,0,3,3\"><FontIcon Glyph=\"&#xE70D;\" FontSize=\"10\" /></Button></Grid></Border>" +
        "<Border x:Name=\"PART_QuickAccessBelowHost\" Grid.Row=\"3\" Padding=\"8,0,6,4\" Visibility=\"Collapsed\" HorizontalAlignment=\"Left\" />" +
        "<Grid x:Name=\"PART_RevealBar\" Grid.Row=\"4\" Height=\"22\" Visibility=\"Collapsed\">" +
        "<Button x:Name=\"PART_RevealButton\" Style=\"{StaticResource RibbonChromeButtonStyle}\" HorizontalAlignment=\"Right\" Width=\"36\" Margin=\"0,0,8,0\">" +
        "<FontIcon Glyph=\"&#xE712;\" FontSize=\"12\" /></Button></Grid>" +
        "</Grid>"
}

/**
 * The ribbon as seen by the views inside it (tabs, groups, items, the tab row, KeyTips) (implements [RibbonTabContainer]
 * and the like). It is separated so that these contracts are not exposed through the public API [WRibbon].
 */
internal class RibbonViewHost(private val owner: WRibbon) : RibbonTabContainer, RibbonTabStripOwner, RibbonKeyTipOwner {
    override val metrics: RibbonMetrics get() = owner.metrics
    override val textWidths: RibbonTextWidths get() = owner.textMeasurer
    override val commandCatalog: RibbonCommandCatalog? get() = owner.model.commandCatalog
    override val ribbon: WRibbon get() = owner

    override fun itemInvoked(model: RibbonNodeModel, commandId: String?, parameter: Any?) = owner.onItemInvoked(model, commandId, parameter)

    override fun invalidateItemsLayout() = owner.invalidateLayout()

    override fun showItemContextMenu(view: RibbonItemView, x: Double, y: Double): Boolean = owner.showItemContextMenu(view, x, y)

    override fun radioScope(): List<RibbonItemModel> = owner.allItemModels()

    // ---- Groups and tabs

    override val showsGroupCaptions: Boolean get() = owner.model.showGroupCaptions
    override val canFloatGroups: Boolean get() = owner.canFloatGroups
    override val panelPresentation: RibbonPanelPresentation get() = owner.panelPresentation
    override val themeSource: WComponent get() = ribbon
    override val reductionStrategy: RibbonReductionStrategy get() = owner.reductionStrategy
    override val isAdaptiveLayoutEnabled: Boolean get() = owner.isAdaptiveLayoutEnabled

    override fun groupLayoutInvalidated(group: RibbonGroupView) = owner.invalidateLayout()

    override fun groupFloatingChanged(group: RibbonGroupView) = owner.onGroupFloatingChanged(group)

    override fun showGroupContextMenu(group: RibbonGroupView, x: Double, y: Double): Boolean =
        owner.showContextMenu(group.model, group.model, group.element, x, y)

    override fun groupLabel(group: RibbonGroupModel): String? = owner.customizer.labelOf(group)

    override fun attachPopupKeyboard(element: XamlElement) = owner.attachPopupKeyboard(element)

    override fun effectiveGroups(tab: RibbonTabModel): List<RibbonGroupModel> = owner.customizer.groupsOf(tab)

    /** The group views of all tabs. */
    fun allGroupViews(): List<RibbonGroupView> = owner.tabViews.values.flatMap { it.groupViews() }

    /** The group that contains [view]. */
    fun groupOf(view: RibbonItemView): RibbonGroupView? = allGroupViews().firstOrNull { group -> flatten(group.itemViews()).any { it === view } }

    /** The view of [item] (searched in the order: the selected tab → other tabs → the tab row → the QAT). */
    fun findView(item: RibbonItemModel): RibbonItemView? {
        val selected = owner.selectedTab?.let { owner.tabViews[it] }
        val tabs = listOfNotNull(selected) + owner.tabViews.values.filter { it !== selected }
        for (tab in tabs) {
            for (group in tab.groupViews()) {
                flatten(group.itemViews()).firstOrNull { it.model === item }?.let { return it }
            }
        }
        return flatten(owner.tabStripItemViews() + owner.quickAccessViews()).firstOrNull { it.model === item }
    }

    /** Expands nested items (rows and button groups). */
    fun flatten(views: List<RibbonItemView>): List<RibbonItemView> = views.flatMap { view ->
        if (view is RibbonContainerView) listOf(view) + flatten(view.childViews()) else listOf(view)
    }

    // ---- Tab row

    override fun tabLabel(tab: RibbonTabModel): String = owner.tabLabel(tab)

    override fun onHeaderClicked(tab: RibbonTabModel) {
        val wasSelected = owner.selectedTab === tab
        owner.selectedTab = tab
        if (owner.model.visibilityMode == RibbonVisibilityMode.TABS_ONLY) {
            if (wasSelected && (owner.isMinimizedPopupOpen || owner.minimizedPopupJustClosed)) {
                owner.closeMinimizedPopup()
            } else {
                WinUiUtilities.invokeLater { owner.openMinimizedPopup() }
            }
        } else if (owner.model.visibilityMode == RibbonVisibilityMode.FULL_SCREEN) {
            owner.isFullScreenRevealed = true
        }
    }

    override fun onHeaderDoubleTapped(): Boolean {
        if (!owner.isCollapsible) return false
        owner.toggleMinimized()
        return true
    }

    override fun onHeaderRightTapped(tab: RibbonTabModel, element: XamlElement, x: Double, y: Double): Boolean =
        owner.showContextMenu(tab, null, element, x, y)

    override fun focusCommands(): Boolean {
        val tab = owner.selectedTab ?: return false
        if (owner.model.visibilityMode == RibbonVisibilityMode.TABS_ONLY) owner.openMinimizedPopup()
        val view = owner.tabViews[tab] ?: return false
        val first = Xaml.firstFocusable(view.element.inspectable) ?: return false
        return try {
            first.queryInterfaceOrNull(XamlInterop.IID_IUIElement)?.let { XamlElement(it).focus() } ?: false
        } finally {
            first.release()
        }
    }

    override fun onHeaderKeyTip(tab: RibbonTabModel): RibbonKeyTipResult {
        owner.selectedTab = tab
        var cleanup: (() -> Unit)? = null
        when (owner.model.visibilityMode) {
            RibbonVisibilityMode.TABS_ONLY -> {
                owner.openMinimizedPopup()
                cleanup = { owner.closeMinimizedPopup() }
            }
            RibbonVisibilityMode.FULL_SCREEN -> if (!owner.isFullScreenRevealed) {
                owner.isFullScreenRevealed = true
                cleanup = { owner.isFullScreenRevealed = false }
            }
            else -> Unit
        }
        return RibbonKeyTipResult.Scope(cleanup) {
            owner.tabViews[tab]?.keyTipTargets(owner.model.displayMode == RibbonDisplayMode.SIMPLIFIED).orEmpty()
        }
    }

    override fun selectTab(tab: RibbonTabModel) {
        owner.selectedTab = tab
    }

    // ---- KeyTip

    override fun topLevelKeyTipTargets(): List<RibbonKeyTipTarget> {
        val targets = mutableListOf<RibbonKeyTipTarget>()
        if (owner.applicationButton.isVisible && owner.tabRowVisible) targets += ApplicationButtonTarget(ribbon)
        targets += owner.tabStrip.headers
        targets += flatten(owner.tabStripItemViews()).filter { it.isShown }.flatMap { it.keyTipTargets() }
        return targets
    }

    override fun quickAccessKeyTipTargets(): List<RibbonKeyTipTarget> = owner.quickAccessKeyTipTargets()

    override val keyTipRoot: WComponent get() = ribbon

    override fun keyTipFocusTarget(): XamlElement? =
        (owner.tabStrip.headerOf(owner.selectedTab) ?: owner.tabStrip.headers.firstOrNull())?.element

    override fun keyTipModeChanged(active: Boolean) = owner.fireKeyTipModeChanged(active)

    /** The KeyTip of the application button. */
    private class ApplicationButtonTarget(private val ribbon: WRibbon) : RibbonKeyTipTarget {
        override val keyTipLabel: String? get() = ribbon.model.applicationButtonLabel ?: RibbonStrings.current.file
        override val explicitKeyTip: String? get() = ribbon.applicationButtonKeyTip
        override val keyTipAnchor: XamlElement get() = ribbon.applicationButton

        override fun onKeyTip(): RibbonKeyTipResult {
            ribbon.invokeApplicationButton()
            val backstage = ribbon.backstageKeyTipTargets() ?: return RibbonKeyTipResult.Close
            return RibbonKeyTipResult.Scope(null) { backstage }
        }
    }
}

/**
 * A popup that temporarily shows the selected tab's commands when only tabs are shown (the minimized popup in RibbonSpace).
 * It moves the ribbon's command area into the popup to show it, and puts it back when closed.
 */
internal class RibbonMinimizedPopup(private val ribbon: WRibbon) {
    private val chrome = XamlElement.load("<Border />")
    private val popup = WPopup(chrome)
    private val dismissLayer = RibbonDismissLayer { close() }
    private var lastClosed = 0L

    /** Whether it is open. */
    var isOpen: Boolean = false
        private set

    init {
        popup.isLightDismissEnabled = false
        popup.addCloseListener { restore() }
        chrome.onKeyDown { e ->
            if (e.key == RibbonInputViews.VK_ESCAPE) {
                close()
                ribbon.tabStrip.headerOf(ribbon.selectedTab)?.element?.focus()
                e.handled = true
            }
        }
    }

    /** Whether it was just closed (so that a header click does not reopen it). */
    val justClosed: Boolean get() = System.currentTimeMillis() - lastClosed < JUST_CLOSED_MILLIS

    /** Opens the popup. */
    fun open() {
        if (ribbon.model.visibilityMode != RibbonVisibilityMode.TABS_ONLY || isOpen) return
        val commandBar = ribbon.commandBar
        Xaml.detach(commandBar)
        commandBar.isVisible = true
        chrome.setChild(commandBar)
        chrome.setSize(ribbon.actualWidth, Double.NaN)
        chrome.requestedTheme = ribbon.actualTheme
        ribbon.attachPopupKeyboard(chrome)
        val origin = ribbon.root.positionInRoot()
        val tabRow = ribbon.root.part("PART_TabRow")
        val rowPosition = tabRow.positionInRoot()
        popup.horizontalOffset = origin[0]
        popup.verticalOffset = rowPosition[1] + tabRow.actualHeight
        isOpen = true
        dismissLayer.show(ribbon)
        popup.show(ribbon)
        ribbon.invalidateLayout()
    }

    /** Closes the popup (Closed arrives asynchronously, so the switch to the closed state is done here). */
    fun close() {
        if (!isOpen) return
        dismissLayer.hide()
        popup.hide()
        restore()
    }

    private fun restore() {
        if (!isOpen) return
        isOpen = false
        lastClosed = System.currentTimeMillis()
        val commandBar = ribbon.commandBar
        chrome.setChild(null)
        ribbon.root.addChild(commandBar)
        commandBar.setGridCell(2, 0)
        commandBar.isVisible = ribbon.model.visibilityMode != RibbonVisibilityMode.TABS_ONLY
        ribbon.invalidateLayout()
    }

    private companion object {
        const val JUST_CLOSED_MILLIS = 300L
    }
}
