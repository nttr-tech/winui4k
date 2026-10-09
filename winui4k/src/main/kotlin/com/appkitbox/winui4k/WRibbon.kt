package com.appkitbox.winui4k

import com.appkitbox.winui4k.ribbon.RibbonCommandCatalog
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel
import com.appkitbox.winui4k.ribbon.RibbonCustomization
import com.appkitbox.winui4k.ribbon.RibbonCustomizePage
import com.appkitbox.winui4k.ribbon.RibbonDisplayMode
import com.appkitbox.winui4k.ribbon.RibbonGroupModel
import com.appkitbox.winui4k.ribbon.RibbonItemModel
import com.appkitbox.winui4k.ribbon.RibbonItemSize
import com.appkitbox.winui4k.ribbon.RibbonListListener
import com.appkitbox.winui4k.ribbon.RibbonMetrics
import com.appkitbox.winui4k.ribbon.RibbonMinimizeBehavior
import com.appkitbox.winui4k.ribbon.RibbonModel
import com.appkitbox.winui4k.ribbon.RibbonNodeModel
import com.appkitbox.winui4k.ribbon.RibbonPanelPresentation
import com.appkitbox.winui4k.ribbon.RibbonPropertyChangeListener
import com.appkitbox.winui4k.ribbon.RibbonQuickAccessPosition
import com.appkitbox.winui4k.ribbon.RibbonReductionStrategy
import com.appkitbox.winui4k.ribbon.RibbonSearchEngine
import com.appkitbox.winui4k.ribbon.RibbonSearchEntry
import com.appkitbox.winui4k.ribbon.RibbonSearchResult
import com.appkitbox.winui4k.ribbon.RibbonState
import com.appkitbox.winui4k.ribbon.RibbonStateSerializer
import com.appkitbox.winui4k.ribbon.RibbonStrings
import com.appkitbox.winui4k.ribbon.RibbonTabModel
import com.appkitbox.winui4k.ribbon.RibbonVisibilityMode
import java.util.EventObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An Office-style ribbon (a pure Kotlin implementation of the Ribbon from RibbonSpace.WinUI).
 *
 * As in Swing's MVC, all content and state live in the model ([RibbonModel]); this component displays the model and
 * writes user interactions back to it (adding or removing tabs, groups and items, selection, display mode and the QAT
 * are reflected in the display when the model changes). Settings that concern only the display (enabling KeyTips, the
 * right-click menu, allowing floating panels, how groups shrink, and so on) are specified with this class's properties.
 *
 * Provides the tab row (application button, tabs, contextual tabs and tab row items), a classic / simplified ribbon
 * that shrinks to fit the width, the Quick Access Toolbar, Backstage, KeyTips (Alt / F10), shortcut dispatch, display
 * options, AutoCAD's minimize cycling, floating panels and expanded panels, customization, state saving and restoring,
 * and command search.
 */
// Maps 1:1 to RibbonSpace's Ribbon (a public API split across 10 files as a C# partial class), so its line count grows
// with the number of public APIs. The actual work is delegated to internal classes such as RibbonViewHost,
// RibbonKeyboard, RibbonMenuBuilder and RibbonCustomizer
@Suppress("LargeClass")
class WRibbon @JvmOverloads constructor(
    /** The ribbon's model. */
    val model: RibbonModel = RibbonModel(),
) : WComponent(createRoot()) {
    internal val root: XamlElement = XamlElement(inspectable.also { it.addRef() })
    private val quickAccessAboveHost = root.part("PART_QuickAccessAboveHost")
    private val quickAccessBelowHost = root.part("PART_QuickAccessBelowHost")
    private val tabRow = root.part("PART_TabRow")
    internal val applicationButton = root.part("PART_ApplicationButton")
    private val startContentHost = root.part("PART_StartContent")
    private val tabStripHost = root.part("PART_TabStripHost")
    private val tabStripItemsHost = root.part("PART_TabStripItemsHost")
    private val endContentHost = root.part("PART_EndContent")
    private val minimizeButtons = root.part("PART_MinimizeButtons")
    private val minimizeButton = root.part("PART_MinimizeButton")
    private val minimizeBehaviorButton = root.part("PART_MinimizeBehaviorButton")
    internal val commandBar = root.part("PART_CommandBar")
    private val tabContentHost = root.part("PART_TabContentHost")
    private val displayOptionsButton = root.part("PART_DisplayOptionsButton")
    private val revealBar = root.part("PART_RevealBar")
    private val revealButton = root.part("PART_RevealButton")
    private val measureHost = root.part("PART_Measure")

    internal val textMeasurer = RibbonTextMeasurer(measureHost)
    internal val host = RibbonViewHost(this)
    internal val customizer = RibbonCustomizer(model)
    internal val menus = RibbonMenuBuilder(this)
    internal val keyTips = RibbonKeyTipController(host)
    private val keyboard = RibbonKeyboard(this)
    internal val tabStrip = RibbonTabStrip(host)
    private val quickAccessBar = RibbonQuickAccessBar(this, host)
    private val tabStripItems = RibbonItemStrip(model.tabStripItems, host, { tabStripItemLayout() }, spacing = 4.0)
    internal val tabViews = LinkedHashMap<RibbonTabModel, RibbonTabView>()
    private val minimizedPopup = RibbonMinimizedPopup(this)
    private var backstageView: RibbonBackstageView? = null

    private val itemInvokedListeners = CopyOnWriteArrayList<RibbonItemInvokedListener>()
    private val tabChangeListeners = CopyOnWriteArrayList<RibbonTabChangeListener>()
    private val applicationButtonListeners = CopyOnWriteArrayList<RibbonApplicationButtonListener>()
    private val stateListeners = CopyOnWriteArrayList<RibbonStateChangeListener>()
    private val keyTipModeListeners = CopyOnWriteArrayList<RibbonKeyTipModeListener>()
    private val contextMenuListeners = CopyOnWriteArrayList<RibbonContextMenuListener>()
    private val customizeListeners = CopyOnWriteArrayList<RibbonCustomizeListener>()
    private val floatingListeners = CopyOnWriteArrayList<RibbonGroupFloatingListener>()
    private val backstageListeners = CopyOnWriteArrayList<RibbonBackstageListener>()
    private val quickAccessListeners = CopyOnWriteArrayList<RibbonQuickAccessListener>()
    private val searchRequestListeners = CopyOnWriteArrayList<RibbonSearchRequestListener>()

    private var loaded = false
    private var layoutPending = false
    private var lastRegularTab: RibbonTabModel? = null
    private var visibleContextualIds: Set<String> = emptySet()
    private var defaultQuickAccessIds: List<String>? = null
    private var defaultQuickAccessPosition = RibbonQuickAccessPosition.ABOVE_RIBBON

    // ---------------------------------------------------------------- Display settings (not held by the model)

    /** A full override of the metrics (takes precedence over [RibbonModel.density]). */
    var customMetrics: RibbonMetrics? = null
        set(value) {
            field = value
            onMetricsChanged()
        }

    /** The current metrics. */
    val metrics: RibbonMetrics get() = customMetrics ?: RibbonMetrics.forDensity(model.density)

    /** Whether to use KeyTips (Alt / F10). */
    var isKeyTipsEnabled: Boolean = true

    /** Whether item shortcuts ([RibbonItemModel.shortcut]) are dispatched to their items from anywhere in the window. */
    var isShortcutRoutingEnabled: Boolean = true

    /** Whether to show the right-click menu (add to QAT, collapse, customize). */
    var isContextMenuEnabled: Boolean = true

    /** Whether to show the customization commands (in the right-click menu and the QAT's [More Commands]). */
    var canCustomize: Boolean = true

    /** Whether to show AutoCAD's minimize button (with an arrow for choosing the behavior) at the right end of the tab row. */
    var isMinimizeButtonVisible: Boolean = false
        set(value) {
            field = value
            updateChrome()
        }

    /** Whether a panel can be made floating by dragging its title or with [Float Panel]. */
    var canFloatGroups: Boolean = false

    /** Whether to show [Show Tabs] / [Show Panels] / [Show Panel Titles] in the right-click menu (AutoCAD). */
    var isVisibilityMenuEnabled: Boolean = false

    /** How groups shrink when the width runs short. */
    var reductionStrategy: RibbonReductionStrategy = RibbonReductionStrategy.STEPWISE
        set(value) {
            field = value
            invalidateLayout()
        }

    /** Labels of tab row items are hidden when the ribbon is narrower than this. */
    var compactTabStripWidth: Double = 720.0
        set(value) {
            field = value
            invalidateLayout()
        }

    /** Whether to show the [Ribbon Display Options] button. */
    var isDisplayOptionsButtonVisible: Boolean = true
        set(value) {
            field = value
            updateChrome()
        }

    /** Whether the simplified ribbon can be chosen in the display options. */
    var isSimplifiedModeAvailable: Boolean = true

    /** Whether groups are reduced to fit the width. */
    var isAdaptiveLayoutEnabled: Boolean = true
        set(value) {
            field = value
            invalidateLayout()
        }

    /** Whether the ribbon can be collapsed (double-clicking a tab, Ctrl+F1, display options). */
    var isCollapsible: Boolean = true
        set(value) {
            field = value
            updateChrome()
        }

    /** The default selection behavior when a contextual group is shown (when the group specifies NONE). */
    var contextualActivation: RibbonContextualActivation = RibbonContextualActivation.NONE

    /** The content of the application button (a logo, etc.; the label if null). */
    var applicationButtonContent: WComponent? = null
        set(value) {
            field = value
            updateChrome()
        }

    /** A menu opened by the application button instead of Backstage (a classic application menu). */
    var applicationMenu: WFlyoutBase? = null

    /** The KeyTip of the application button. */
    var applicationButtonKeyTip: String? = "F"

    /** Content placed before the tabs. */
    var tabStripStartContent: WComponent? = null
        set(value) {
            field = value
            startContentHost.setChild(value)
        }

    /** Content placed at the right end of the tab row. */
    var tabStripEndContent: WComponent? = null
        set(value) {
            field = value
            endContentHost.setChild(value)
        }

    /** Whether the QAT is placed outside the ribbon, such as in the title bar (set by [WRibbonTitleBar]). */
    var isQuickAccessHostedExternally: Boolean = false
        set(value) {
            field = value
            updateQuickAccessPlacement()
        }

    /** Whether the ribbon is temporarily shown in full-screen mode. */
    var isFullScreenRevealed: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            updateChrome()
            invalidateLayout()
        }

    /** The command search engine. */
    val searchEngine: RibbonSearchEngine = RibbonSearchEngine()

    /** Additional search entries (help topics, recent files, custom actions; a [Runnable] can be given as the target). */
    val additionalSearchEntries: MutableList<RibbonSearchEntry> = CopyOnWriteArrayList()

    // ---------------------------------------------------------------- Model subscription

    private val modelListener = RibbonPropertyChangeListener { event -> onModelPropertyChanged(event.propertyName) }
    private val tabsListener = RibbonListListener<RibbonTabModel> { syncTabs() }
    private val contextualListener = RibbonListListener<RibbonContextualGroupModel> { syncContextualGroups() }
    private val nodeListener = RibbonPropertyChangeListener { refreshTabStrip() }
    private val qatListener = RibbonListListener<RibbonItemModel> { onQuickAccessItemsChanged() }
    private val groupsListeners = HashMap<RibbonTabModel, RibbonListListener<RibbonGroupModel>>()
    private val subscribedContextual = mutableListOf<RibbonContextualGroupModel>()
    private val stringsListener = Runnable { WinUiUtilities.invokeLater { onStringsChanged() } }

    init {
        tabStripHost.setChild(tabStrip.element)
        tabStripItemsHost.addChild(tabStripItems.element)
        applicationButton.onClick { invokeApplicationButton() }
        displayOptionsButton.onClick {
            val menu = menus.displayOptionsMenu()
            menu.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_RIGHT
            menu.showAt(displayOptionsButton)
        }
        minimizeButton.onClick { toggleMinimized() }
        minimizeBehaviorButton.onClick {
            val menu = menus.minimizeBehaviorMenu()
            menu.placement = FlyoutPlacement.BOTTOM_EDGE_ALIGNED_RIGHT
            menu.showAt(minimizeBehaviorButton)
        }
        revealButton.onClick { isFullScreenRevealed = !isFullScreenRevealed }
        root.onRightTapped { e ->
            val p = e.position(root)
            if (showContextMenu(null, null, root, p[0], p[1])) e.markHandled()
        }
        addLoadedListener { onLoaded() }
        root.onUnloaded { onUnloaded() }
        addSizeChangedListener { invalidateLayout() }
        addActualThemeChangedListener {
            refreshTabStrip()
            invalidateLayout()
        }
        model.addPropertyChangeListener(modelListener)
        model.tabs.addListListener(tabsListener)
        model.contextualGroups.addListListener(contextualListener)
        model.quickAccessItems.addListListener(qatListener)
        RibbonStrings.addCurrentChangedListener(stringsListener)
        quickAccessBar.strip.attach()
        tabStripItems.attach()
        syncContextualGroups()
        syncTabs()
        updateChrome()
    }

    private fun onLoaded() {
        loaded = true
        keyboard.attach()
        if (defaultQuickAccessIds == null) {
            defaultQuickAccessIds = quickAccessItemIds()
            defaultQuickAccessPosition = model.quickAccessPosition
        }
        refreshTabStrip()
        if (selectedTab == null) selectFirstTab()
        invalidateLayout()
        if (model.backstage.isOpen) openBackstage()
        resumePopups()
    }

    private fun onUnloaded() {
        loaded = false
        suspendPopups()
        keyboard.detach()
    }

    // ---------------------------------------------------------------- Tabs

    private fun syncTabs() {
        val wanted = model.tabs.toList() + customizer.customTabs()
        for (stale in tabViews.keys.filter { it !in wanted }) {
            tabViews.remove(stale)?.let {
                it.dispose()
                tabContentHost.removeChild(it.element)
            }
            groupsListeners.remove(stale)?.let { stale.groups.removeListListener(it) }
            stale.removePropertyChangeListener(nodeListener)
        }
        for (tab in wanted) {
            if (tab in tabViews) continue
            val view = RibbonTabView(tab, host)
            tabViews[tab] = view
            view.syncGroups()
            view.element.isVisible = false
            view.element.verticalAlignment = VerticalAlignment.TOP
            tabContentHost.addChild(view.element)
            val listener = RibbonListListener<RibbonGroupModel> {
                view.syncGroups()
                invalidateLayout()
            }
            groupsListeners[tab] = listener
            tab.groups.addListListener(listener)
            tab.addPropertyChangeListener(nodeListener)
        }
        refreshTabStrip()
    }

    private fun syncContextualGroups() {
        subscribedContextual.forEach { it.removePropertyChangeListener(nodeListener) }
        subscribedContextual.clear()
        for (group in model.contextualGroups) {
            group.addPropertyChangeListener(nodeListener)
            subscribedContextual += group
        }
        refreshTabStrip()
    }

    /** The tabs shown in the tab row (regular tabs, then the tabs of visible contextual groups). */
    val visibleTabs: List<RibbonTabModel> get() = availableTabs().map { it.first }

    private fun isAvailable(tab: RibbonTabModel): Boolean = tab.isVisible && !customizer.isHidden(tab)

    private fun availableTabs(): List<Pair<RibbonTabModel, RibbonContextualGroupModel?>> {
        val regular = customizer.orderTabs((model.tabs + customizer.customTabs()).filter { !it.isContextual && isAvailable(it) })
        val contextual = model.contextualGroups.filter { it.isVisible }.flatMap { group ->
            model.tabs.filter { it.contextualGroupId == group.id && isAvailable(it) }.map { it to group }
        }
        return regular.map { it to null } + contextual
    }

    /** Rebuilds the tab row (when tabs or contextual groups are added, removed, shown or renamed). */
    internal fun refreshTabStrip() {
        val tabs = availableTabs()
        tabStrip.update(tabs, selectedTab, actualTheme == ElementTheme.DARK)
        activateShownContextualGroups()
        val current = selectedTab
        if (current != null && tabs.none { it.first === current }) {
            val fallback = lastRegularTab?.takeIf { last -> tabs.any { it.first === last } } ?: tabs.firstOrNull()?.first
            selectedTab = fallback
        } else if (current == null && loaded) {
            selectFirstTab()
        }
        keyboard.invalidateShortcuts()
        invalidateLayout()
    }

    /** The selection behavior for contextual groups that became visible (selects their tab once shown). */
    private fun activateShownContextualGroups() {
        val shown = model.contextualGroups.filter { it.isVisible }.mapNotNull { it.id }.toSet()
        val added = shown - visibleContextualIds
        visibleContextualIds = shown
        for (id in added) {
            val group = model.findContextualGroup(id) ?: continue
            val activation = if (group.activation == RibbonContextualActivation.NONE) contextualActivation else group.activation
            if (activation == RibbonContextualActivation.SELECT_ON_SHOW) {
                model.tabs.firstOrNull { it.contextualGroupId == id && isAvailable(it) }?.let { selectedTab = it }
            }
        }
    }

    private fun selectFirstTab() {
        val id = model.selectedTabId
        val byId = id?.let { findTab(it) }?.takeIf { tab -> visibleTabs.contains(tab) }
        selectedTab = byId ?: visibleTabs.firstOrNull()
    }

    /** The selected tab. */
    var selectedTab: RibbonTabModel?
        get() = model.selectedTabId?.let { findTab(it) }
        set(value) {
            if (value != null && value !in visibleTabs) return
            model.selectedTabId = value?.id
            applySelection()
        }

    private var appliedTab: RibbonTabModel? = null

    private fun applySelection() {
        val newTab = selectedTab
        val oldTab = appliedTab
        if (newTab === oldTab) return
        appliedTab = newTab
        oldTab?.let { tab ->
            tabViews[tab]?.let { view ->
                view.element.isVisible = false
                view.groupViews().forEach {
                    it.closePopup()
                    it.closeSlideOut()
                }
            }
        }
        newTab?.let { tab ->
            if (!tab.isContextual) lastRegularTab = tab
            tabViews[tab]?.let { view ->
                view.element.isVisible = true
                view.groupViews().filter { it.isSlideOutPinned }.forEach { group -> WinUiUtilities.invokeLater { group.openSlideOut() } }
            }
        }
        tabStrip.setSelected(newTab)
        invalidateLayout()
        val event = RibbonTabChangedEvent(this, oldTab, newTab)
        tabChangeListeners.forEach { it.tabChanged(event) }
    }

    /** Finds a tab by id (or by label if it has none), including custom tabs. */
    fun findTab(id: String): RibbonTabModel? =
        (model.tabs + customizer.customTabs()).firstOrNull { it.id == id } ?: model.tabs.firstOrNull { it.label == id }

    /** Selects a tab by id. Returns false if it is unknown or hidden. */
    fun selectTab(id: String): Boolean {
        val tab = findTab(id) ?: return false
        if (tab !in visibleTabs) return false
        selectedTab = tab
        return true
    }

    /** The tab's display name (reflecting renames made through customization). */
    fun tabLabel(tab: RibbonTabModel): String = customizer.labelOf(tab)

    // ---------------------------------------------------------------- Display mode

    /** The display mode (classic / simplified). */
    var displayMode: RibbonDisplayMode
        get() = model.displayMode
        set(value) {
            model.displayMode = value
        }

    /** The display state (always shown / tabs only / full screen / panel buttons / panel titles). */
    var visibilityMode: RibbonVisibilityMode
        get() = model.visibilityMode
        set(value) {
            model.visibilityMode = value
        }

    /** Whether the ribbon is minimized (tabs only, panel buttons, panel titles). */
    var isMinimized: Boolean
        get() = isMinimizedMode(model.visibilityMode)
        set(value) {
            if (value && model.visibilityMode == RibbonVisibilityMode.ALWAYS_SHOW) {
                model.visibilityMode = minimizedState()
            } else if (!value && isMinimizedMode(model.visibilityMode)) {
                model.visibilityMode = RibbonVisibilityMode.ALWAYS_SHOW
            }
        }

    /** The state [toggleMinimized] moves to (follows the minimize behavior, including AutoCAD's cycling). */
    fun nextMinimizeState(): RibbonVisibilityMode {
        val mode = model.visibilityMode
        return when {
            model.minimizeBehavior == RibbonMinimizeBehavior.CYCLE_ALL -> when (mode) {
                RibbonVisibilityMode.ALWAYS_SHOW -> RibbonVisibilityMode.PANEL_BUTTONS
                RibbonVisibilityMode.PANEL_BUTTONS -> RibbonVisibilityMode.PANEL_TITLES
                RibbonVisibilityMode.PANEL_TITLES -> RibbonVisibilityMode.TABS_ONLY
                else -> RibbonVisibilityMode.ALWAYS_SHOW
            }
            mode != RibbonVisibilityMode.ALWAYS_SHOW -> RibbonVisibilityMode.ALWAYS_SHOW
            else -> minimizedState()
        }
    }

    internal fun minimizedState(): RibbonVisibilityMode = when (model.minimizeBehavior) {
        RibbonMinimizeBehavior.PANEL_TITLES -> RibbonVisibilityMode.PANEL_TITLES
        RibbonMinimizeBehavior.PANEL_BUTTONS, RibbonMinimizeBehavior.CYCLE_ALL -> RibbonVisibilityMode.PANEL_BUTTONS
        RibbonMinimizeBehavior.TABS -> RibbonVisibilityMode.TABS_ONLY
    }

    /** Toggles minimization (Ctrl+F1, double-clicking a tab, the minimize button). */
    fun toggleMinimized() {
        if (isCollapsible) model.visibilityMode = nextMinimizeState()
    }

    /** Display as panel buttons / panel titles (the simplified ribbon always uses the normal display). */
    internal val panelPresentation: RibbonPanelPresentation
        get() = if (model.displayMode == RibbonDisplayMode.SIMPLIFIED) {
            RibbonPanelPresentation.FULL
        } else {
            when (model.visibilityMode) {
                RibbonVisibilityMode.PANEL_BUTTONS -> RibbonPanelPresentation.BUTTONS
                RibbonVisibilityMode.PANEL_TITLES -> RibbonPanelPresentation.TITLES
                else -> RibbonPanelPresentation.FULL
            }
        }

    /** Whether the popup that temporarily shows the commands is open in the tabs-only display. */
    val isMinimizedPopupOpen: Boolean get() = minimizedPopup.isOpen

    /** In the tabs-only display, temporarily shows the commands of the selected tab. */
    fun openMinimizedPopup() = minimizedPopup.open()

    /** Closes the popup opened by [openMinimizedPopup]. */
    fun closeMinimizedPopup() = minimizedPopup.close()

    /** Whether the tabs-only popup was just closed (so that clicking the header does not reopen it). */
    internal val minimizedPopupJustClosed: Boolean get() = minimizedPopup.justClosed

    /** Whether the tab row is shown (it is hidden in full screen). */
    internal val tabRowVisible: Boolean get() = tabRow.isVisible

    /** Routes key input inside popups to KeyTips and shortcuts. */
    internal fun attachPopupKeyboard(element: XamlElement) = keyboard.attachPopup(element)

    private fun onModelPropertyChanged(name: String) {
        when (name) {
            "selectedTab" -> applySelection()
            "displayMode" -> {
                closeMinimizedPopup()
                closeGroupPopups()
                updateChrome()
                fireStateChanged()
            }
            "visibilityMode" -> onVisibilityModeChanged()
            "density" -> onMetricsChanged()
            "quickAccessPosition", "isQuickAccessVisible", "showQuickAccessLabels" -> onQuickAccessOptionsChanged()
            "minimizeBehavior" -> {
                updateChrome()
                fireStateChanged()
            }
            "showGroupCaptions" -> {
                invalidateLayout()
                fireStateChanged()
            }
            "applicationButtonLabel", "isApplicationButtonVisible" -> updateChrome()
            "commandCatalog" -> keyboard.invalidateShortcuts()
            "title" -> fireQuickAccessChanged()
        }
        invalidateLayout()
    }

    private fun onVisibilityModeChanged() {
        if (model.visibilityMode != RibbonVisibilityMode.FULL_SCREEN) isFullScreenRevealed = false
        closeMinimizedPopup()
        closeGroupPopups()
        updateChrome()
        fireStateChanged()
    }

    private fun closeGroupPopups() {
        tabViews.values.flatMap { it.groupViews() }.forEach {
            it.closePopup()
            it.closeSlideOut()
        }
    }

    private fun onMetricsChanged() {
        tabViews.values.forEach { it.invalidateWidths() }
        quickAccessBar.strip.applyLayouts()
        tabStripItems.applyLayouts()
        invalidateLayout()
        fireStateChanged()
    }

    private fun onStringsChanged() {
        updateChrome()
        refreshTabStrip()
        tabViews.values.forEach { it.invalidateWidths() }
        invalidateLayout()
    }

    /** Applies the application button, display options, minimize button and full-screen display. */
    private fun updateChrome() {
        val strings = RibbonStrings.current
        applicationButton.isVisible = model.isApplicationButtonVisible
        val label = model.applicationButtonLabel ?: strings.file
        val content = applicationButtonContent
        if (content != null) {
            Xaml.detach(content)
            applicationButton.setContent(content)
        } else {
            applicationButton.setContentText(label)
        }
        applicationButton.setAutomationName(label)
        displayOptionsButton.isVisible = isDisplayOptionsButtonVisible
        displayOptionsButton.setAutomationName(strings.ribbonDisplayOptions)
        displayOptionsButton.setToolTipValue(strings.ribbonDisplayOptions)
        updateMinimizeButton()
        val fullScreen = model.visibilityMode == RibbonVisibilityMode.FULL_SCREEN
        val showRibbon = !fullScreen || isFullScreenRevealed
        tabRow.isVisible = showRibbon
        if (!minimizedPopup.isOpen) commandBar.isVisible = showRibbon && model.visibilityMode != RibbonVisibilityMode.TABS_ONLY
        revealBar.isVisible = fullScreen
        revealButton.setAutomationName(strings.fullScreenMode)
        updateQuickAccessPlacement()
    }

    private fun updateMinimizeButton() {
        val strings = RibbonStrings.current
        minimizeButtons.isVisible = isMinimizeButtonVisible && isCollapsible
        val minimized = isMinimizedMode(model.visibilityMode)
        minimizeButton.setContent(XamlElement.load("<FontIcon Glyph=\"${if (minimized) "&#xE70D;" else "&#xE70E;"}\" FontSize=\"10\" />"))
        val text = when (nextMinimizeState()) {
            RibbonVisibilityMode.PANEL_BUTTONS -> strings.minimizeToPanelButtons
            RibbonVisibilityMode.PANEL_TITLES -> strings.minimizeToPanelTitles
            RibbonVisibilityMode.TABS_ONLY -> strings.minimizeToTabs
            else -> strings.showFullRibbon
        }
        minimizeButton.setAutomationName(text)
        minimizeButton.setToolTipValue(text)
        minimizeBehaviorButton.setAutomationName(strings.minimizeRibbon)
        minimizeBehaviorButton.setToolTipValue(strings.minimizeRibbon)
    }

    // ---------------------------------------------------------------- Layout

    /** Redoes the layout (e.g. after changing an item's content in code). Batched into the next dispatch. */
    fun invalidateLayout() {
        if (layoutPending) return
        layoutPending = true
        WinUiUtilities.invokeLater {
            layoutPending = false
            doLayout()
        }
    }

    private fun doLayout() {
        if (!loaded) return
        val m = metrics
        tabRow.setMinHeight(m.tabHeight)
        tabStripItems.applyLayouts()
        tabStrip.layout(tabStripHost.actualWidth, m.tabHeight)
        val view = selectedTab?.let { tabViews[it] } ?: return
        if (!commandBar.isVisible && !minimizedPopup.isOpen) return
        val available = (tabContentHost.actualWidth - CONTENT_PADDING).takeIf { it > 0 } ?: (actualWidth - FALLBACK_MARGIN)
        val height = view.layout(available, m, model.displayMode == RibbonDisplayMode.SIMPLIFIED)
        view.element.setSize(available, height)
    }

    private fun tabStripItemLayout(): RibbonItemLayout {
        val compact = actualWidth in 1.0..compactTabStripWidth
        return RibbonItemLayout(if (compact) RibbonItemSize.SMALL else RibbonItemSize.MEDIUM, metrics, true, if (compact) false else null)
    }

    // ---------------------------------------------------------------- QAT

    private fun onQuickAccessItemsChanged() {
        keyboard.invalidateShortcuts()
        fireStateChanged()
        fireQuickAccessChanged()
    }

    private fun onQuickAccessOptionsChanged() {
        updateQuickAccessPlacement()
        quickAccessBar.strip.applyLayouts()
        fireStateChanged()
        fireQuickAccessChanged()
    }

    /** Places the QAT above or below (not when it is hidden, external, or hidden in full screen). */
    internal fun updateQuickAccessPlacement() {
        val above = model.quickAccessPosition == RibbonQuickAccessPosition.ABOVE_RIBBON
        val hiddenByFullScreen = model.visibilityMode == RibbonVisibilityMode.FULL_SCREEN && !isFullScreenRevealed
        val target = when {
            !model.isQuickAccessVisible || hiddenByFullScreen -> null
            above -> if (isQuickAccessHostedExternally) null else quickAccessAboveHost
            else -> quickAccessBelowHost
        }
        for (hostElement in listOf(quickAccessAboveHost, quickAccessBelowHost)) {
            if (hostElement !== target) hostElement.setChild(null)
            hostElement.isVisible = hostElement === target
        }
        if (target != null) {
            Xaml.detach(quickAccessBar.element)
            target.setChild(quickAccessBar.element)
        }
        quickAccessBar.strip.applyLayouts()
    }

    /** The QAT element (for placing it outside the ribbon, such as in the title bar). */
    internal val quickAccessElement: XamlElement get() = quickAccessBar.element

    /** Adds [item] to the QAT. Returns false if it cannot be added. */
    fun addToQuickAccess(item: RibbonItemModel): Boolean {
        if (!item.canAddToQuickAccess || item in model.quickAccessItems) return false
        model.quickAccessItems.add(item)
        return true
    }

    /** Adds the item with the given id to the QAT. */
    fun addToQuickAccess(id: String): Boolean = model.findItem(id)?.let { addToQuickAccess(it) } ?: false

    /** Removes [item] from the QAT. */
    fun removeFromQuickAccess(item: RibbonItemModel): Boolean = model.quickAccessItems.remove(item)

    /** Whether [item] is in the QAT. */
    fun isInQuickAccess(item: RibbonItemModel): Boolean = item in model.quickAccessItems

    /** The ids of the QAT items. */
    fun quickAccessItemIds(): List<String> = model.quickAccessItems.mapNotNull { it.id }

    /** The ids of the QAT items declared by the application (as of the first load, before the user's state is applied). */
    val defaultQuickAccessItemIds: List<String>? get() = defaultQuickAccessIds

    /** Restores the application's default QAT items and position. */
    fun resetQuickAccess() {
        val state = getState()
        state.quickAccessItemIds = (defaultQuickAccessIds ?: quickAccessItemIds()).toMutableList()
        state.quickAccessPosition = defaultQuickAccessPosition
        state.isQuickAccessVisible = true
        applyState(state)
    }

    /** Moves the QAT above or below. */
    fun toggleQuickAccessPosition() {
        model.quickAccessPosition = if (model.quickAccessPosition == RibbonQuickAccessPosition.ABOVE_RIBBON) {
            RibbonQuickAccessPosition.BELOW_RIBBON
        } else {
            RibbonQuickAccessPosition.ABOVE_RIBBON
        }
    }

    /** Builds the QAT's customize menu. */
    fun buildQuickAccessCustomizeMenu(): WMenuFlyout = menus.quickAccessCustomizeMenu()

    /** Builds the [Ribbon Display Options] menu. */
    fun buildDisplayOptionsMenu(): WMenuFlyout = menus.displayOptionsMenu()

    /** Builds the menu for AutoCAD's minimize behavior. */
    fun buildMinimizeBehaviorMenu(): WMenuFlyout = menus.minimizeBehaviorMenu()

    // ---------------------------------------------------------------- Application button and Backstage

    /** The application button's action (listeners, then Backstage, then the application menu). */
    fun invokeApplicationButton() {
        keyTips.hide()
        val event = RibbonHandledEvent(this)
        applicationButtonListeners.forEach { it.applicationButtonClicked(event) }
        if (event.isHandled) return
        when {
            model.backstage.items.isNotEmpty() -> isBackstageOpen = true
            else -> applicationMenu?.showAt(applicationButton)
        }
    }

    /** Whether Backstage is open. */
    var isBackstageOpen: Boolean
        get() = model.backstage.isOpen
        set(value) {
            if (value) openBackstage() else closeBackstage()
        }

    private fun openBackstage() {
        if (model.backstage.items.isEmpty()) {
            model.backstage.isOpen = false
            return
        }
        model.backstage.isOpen = true
        if (!loaded) return
        val view = backstageView ?: RibbonBackstageView(this).also { backstageView = it }
        if (view.isOpen) return
        keyTips.hide()
        closeMinimizedPopup()
        view.open()
        backstageListeners.forEach { it.backstageOpened(this) }
    }

    private fun closeBackstage() {
        model.backstage.isOpen = false
        val view = backstageView ?: return
        if (!view.isOpen) return
        keyTips.hide()
        view.close()
        backstageListeners.forEach { it.backstageClosed(this) }
        applicationButton.focus(com.appkitbox.winui4k.internal.winui.XamlInterop.FocusState_Programmatic)
    }

    /** Runs a Backstage item (selects it if it is a page, runs its command if it is an action). */
    fun invokeBackstageItem(item: com.appkitbox.winui4k.ribbon.RibbonBackstageItemModel) {
        if (!isBackstageOpen) isBackstageOpen = true
        backstageView?.invoke(item)
    }

    /** The KeyTip targets of Backstage (while it is open). */
    internal fun backstageKeyTipTargets(): List<RibbonKeyTipTarget>? = backstageView?.takeIf { it.isOpen }?.keyTipTargets()

    // ---------------------------------------------------------------- Running items and menus

    /** An item was invoked (cleans up popups, records search history, notifies listeners). */
    internal fun onItemInvoked(item: RibbonNodeModel, commandId: String?, parameter: Any?) {
        if (minimizedPopup.isOpen) closeMinimizedPopup()
        selectedTab?.let { tab -> tabViews[tab]?.groupViews()?.filter { it.isPopupOpen }?.forEach { it.closePopup() } }
        if (model.visibilityMode == RibbonVisibilityMode.FULL_SCREEN) isFullScreenRevealed = false
        item.id?.let { searchEngine.markUsed(it) }
        val event = RibbonItemInvokedEvent(this, item, commandId, parameter)
        itemInvokedListeners.forEach { it.itemInvoked(event) }
    }

    /** Shows the item's right-click menu. Returns false if it cannot be shown. */
    internal fun showItemContextMenu(view: RibbonItemView, x: Double, y: Double): Boolean =
        showContextMenu(view.model, host.groupOf(view)?.model, view.element, x, y)

    /** Shows the right-click menu of [target] (an item, group or tab). */
    internal fun showContextMenu(target: RibbonNodeModel?, group: RibbonGroupModel?, anchor: XamlElement, x: Double, y: Double): Boolean {
        if (!isContextMenuEnabled) return false
        val menu = menus.contextMenu(target, group)
        val event = RibbonContextMenuEvent(this, target, menu)
        contextMenuListeners.forEach { it.contextMenuOpening(event) }
        if (event.isHandled) return true
        RibbonMenus.showAtPoint(menu, anchor, x, y)
        return true
    }

    /** Shows the right-click menu of the item with the given id (for automation and tests). */
    fun showItemContextMenu(item: RibbonItemModel): Boolean {
        val view = host.findView(item) ?: return false
        return showItemContextMenu(view, 0.0, view.element.actualHeight)
    }

    /** Opens the customization UI (listeners, then [RibbonCustomizeDialog]). */
    fun showCustomizeDialog(page: RibbonCustomizePage) {
        val event = RibbonCustomizeEvent(this, page)
        customizeListeners.forEach { it.customizeRequested(event) }
        if (event.isHandled) return
        RibbonCustomizeDialog(this, page).show()
    }

    // ---------------------------------------------------------------- Floating panels

    /** The groups that are floating panels. */
    fun floatingGroups(): List<RibbonGroupModel> = host.allGroupViews().filter { it.isFloating }.map { it.model }

    /** Makes a group a floating panel ([x], [y] are window coordinates; if null, just below its position in the ribbon). */
    @JvmOverloads
    fun floatGroup(group: RibbonGroupModel, x: Double? = null, y: Double? = null): Boolean {
        val view = host.allGroupViews().firstOrNull { it.model === group } ?: return false
        view.float(x, y)
        return true
    }

    /** Returns a floating panel to the ribbon. */
    fun returnGroupToRibbon(group: RibbonGroupModel) {
        host.allGroupViews().firstOrNull { it.model === group }?.returnToRibbon()
    }

    /** Returns all floating panels to the ribbon. */
    fun returnAllPanelsToRibbon() {
        host.allGroupViews().filter { it.isFloating }.forEach { it.returnToRibbon() }
    }

    internal fun onGroupFloatingChanged(group: RibbonGroupView) {
        keyboard.invalidateShortcuts()
        invalidateLayout()
        floatingListeners.forEach { it.groupFloatingChanged(this, group.model, group.isFloating) }
        fireStateChanged()
    }

    /**
     * Closes transient popups (collapsed groups, expanded panels, the tabs-only popup, KeyTips, Backstage) and hides
     * floating panels. Call this when hiding an ancestor of the ribbon (switching tabs or pages).
     */
    fun suspendPopups() {
        keyTips.cancel()
        closeMinimizedPopup()
        if (isBackstageOpen) isBackstageOpen = false
        closeGroupPopups()
        host.allGroupViews().filter { it.isFloating }.forEach { it.suspendFloat() }
    }

    /** Shows again the floating panels hidden by [suspendPopups] (and the pinned expanded panel of the selected tab). */
    fun resumePopups() {
        if (!loaded) return
        host.allGroupViews().filter { it.isFloating }.forEach { it.showFloat() }
        selectedTab?.let { tab -> tabViews[tab]?.groupViews()?.filter { it.isSlideOutPinned }?.forEach { g -> WinUiUtilities.invokeLater { g.openSlideOut() } } }
    }

    // ---------------------------------------------------------------- KeyTip

    /** Whether KeyTips are shown. */
    val isKeyTipMode: Boolean get() = keyTips.isActive

    /** The strings of the KeyTips currently shown. */
    val activeKeyTips: List<String> get() = keyTips.activeKeyTips

    /** Shows the top-level KeyTips (application button, QAT, tabs, tab row items). */
    fun showKeyTips() {
        if (!isKeyTipsEnabled) return
        val backstage = backstageKeyTipTargets()
        if (backstage != null) keyTips.show(backstage) else keyTips.show()
    }

    /** Hides the KeyTips (popups opened by navigation stay open). */
    fun hideKeyTips() = keyTips.hide()

    /** Exits KeyTip mode and also closes the popups opened by navigation. */
    fun cancelKeyTips() = keyTips.cancel()

    /** Types one character in KeyTip mode (same as a keystroke). Returns true if it matches a KeyTip. */
    fun processKeyTipInput(ch: Char): Boolean = keyTips.process(ch)

    /** Goes back one KeyTip level (same as Esc). Returns false if the mode ends. */
    fun popKeyTipLevel(): Boolean = keyTips.pop()

    internal fun fireKeyTipModeChanged(active: Boolean) {
        keyTipModeListeners.forEach { it.keyTipModeChanged(this, active) }
    }

    internal fun fireSearchRequested(): Boolean {
        if (searchRequestListeners.isEmpty()) return false
        searchRequestListeners.forEach { it.searchRequested(this) }
        return true
    }

    /** Discards the shortcut dispatch cache (called automatically when tabs or items change). */
    fun invalidateShortcuts() = keyboard.invalidateShortcuts()

    // ---------------------------------------------------------------- Commands

    /** The command catalog (the model's). */
    val commandCatalog: RibbonCommandCatalog? get() = model.commandCatalog

    /** Enables or disables all items whose commandId is [commandId]. */
    fun setCommandEnabled(commandId: String, enabled: Boolean) = model.setCommandEnabled(commandId, enabled)

    /** Checks or unchecks all toggles whose commandId is [commandId]. */
    fun setCommandChecked(commandId: String, isChecked: Boolean) = model.setCommandChecked(commandId, isChecked)

    /** The items whose commandId is [commandId]. */
    fun findItemsByCommand(commandId: String): List<RibbonItemModel> = model.findItemsByCommand(commandId)

    /** Finds an item by id. */
    fun findItem(id: String): RibbonItemModel? = model.findItem(id)

    /** All items of the tabs, the tab row and the QAT (including nested ones). */
    internal fun allItemModels(): List<RibbonItemModel> =
        (model.enumerateItems() + RibbonModel.flatten(model.tabStripItems) + RibbonModel.flatten(model.quickAccessItems)).distinct().toList()

    /** The views of the tab row items. */
    internal fun tabStripItemViews(): List<RibbonItemView> = tabStripItems.views()

    /** The views of the QAT items. */
    internal fun quickAccessViews(): List<RibbonItemView> = quickAccessBar.strip.views()

    /** The KeyTip targets of the QAT. */
    internal fun quickAccessKeyTipTargets(): List<RibbonKeyTipTarget> =
        if (model.isQuickAccessVisible && quickAccessBar.element.isVisible) quickAccessBar.keyTipTargets() else emptyList()

    // ---------------------------------------------------------------- Search

    /** Builds the search entries (visible items, Backstage, catalog commands, additional entries). */
    fun buildSearchEntries(): List<RibbonSearchEntry> = RibbonSearchIndex.build(this)

    /** Searches for commands. */
    @JvmOverloads
    fun search(query: String?, maxResults: Int = DEFAULT_SEARCH_RESULTS): List<RibbonSearchResult> {
        searchEngine.setEntries(buildSearchEntries())
        return searchEngine.search(query, maxResults)
    }

    /** Runs a search result (for inputs and galleries, opens their tab so they can be operated). */
    fun executeSearchEntry(entry: RibbonSearchEntry): Boolean = RibbonSearchIndex.execute(this, entry)

    // ---------------------------------------------------------------- State and customization

    /** The current customization (a copy). */
    val customization: RibbonCustomization get() = RibbonCustomizer.clone(customizer.customization)

    /** The savable state (selection, display options, QAT, customization, floating panels, search history). */
    fun getState(): RibbonState = RibbonStatePersistence.capture(this)

    /** Restores a state obtained with [getState]. */
    fun applyState(state: RibbonState) = RibbonStatePersistence.apply(this, state)

    /** Converts the state to JSON. */
    fun saveStateToJson(): String = RibbonStateSerializer.serialize(getState())

    /** Restores the state from JSON (does nothing and returns false if it is invalid). */
    fun loadStateFromJson(json: String?): Boolean {
        val state = RibbonStateSerializer.deserialize(json) ?: return false
        applyState(state)
        return true
    }

    /** Applies a customization (hiding, renaming and reordering tabs and groups, custom tabs and groups). */
    fun applyCustomization(customization: RibbonCustomization) {
        val selectedId = selectedTab?.id
        customizer.apply(customization)
        tabViews.keys.filter { it !in model.tabs }.forEach { tab ->
            tabViews.remove(tab)?.let {
                it.dispose()
                tabContentHost.removeChild(it.element)
            }
        }
        syncTabs()
        tabViews.values.forEach { it.syncGroups() }
        if (selectedId != null && selectedTab?.id != selectedId) selectTab(selectedId)
        refreshTabStrip()
        fireStateChanged()
    }

    /** Removes all user customizations. */
    fun resetCustomization() = applyCustomization(RibbonCustomization())

    internal fun setHiddenByUser(id: String, hidden: Boolean, isTab: Boolean) = applyCustomization(customizer.setHidden(id, hidden, isTab))

    // ---------------------------------------------------------------- Listeners

    /** Subscribes to item invocations. */
    fun addItemInvokedListener(listener: RibbonItemInvokedListener) {
        itemInvokedListeners += listener
    }

    /** Unsubscribes a listener added with [addItemInvokedListener]. */
    fun removeItemInvokedListener(listener: RibbonItemInvokedListener) {
        itemInvokedListeners -= listener
    }

    /** Subscribes to changes of the selected tab. */
    fun addTabChangeListener(listener: RibbonTabChangeListener) {
        tabChangeListeners += listener
    }

    /** Unsubscribes a listener added with [addTabChangeListener]. */
    fun removeTabChangeListener(listener: RibbonTabChangeListener) {
        tabChangeListeners -= listener
    }

    /** Subscribes to clicks on the application button. */
    fun addApplicationButtonListener(listener: RibbonApplicationButtonListener) {
        applicationButtonListeners += listener
    }

    /** Unsubscribes a listener added with [addApplicationButtonListener]. */
    fun removeApplicationButtonListener(listener: RibbonApplicationButtonListener) {
        applicationButtonListeners -= listener
    }

    /** Subscribes to state changes (for auto-saving). */
    fun addStateChangeListener(listener: RibbonStateChangeListener) {
        stateListeners += listener
    }

    /** Unsubscribes a listener added with [addStateChangeListener]. */
    fun removeStateChangeListener(listener: RibbonStateChangeListener) {
        stateListeners -= listener
    }

    /** Subscribes to changes in KeyTip display. */
    fun addKeyTipModeListener(listener: RibbonKeyTipModeListener) {
        keyTipModeListeners += listener
    }

    /** Unsubscribes a listener added with [addKeyTipModeListener]. */
    fun removeKeyTipModeListener(listener: RibbonKeyTipModeListener) {
        keyTipModeListeners -= listener
    }

    /** Subscribes to the moment just before a right-click menu opens. */
    fun addContextMenuListener(listener: RibbonContextMenuListener) {
        contextMenuListeners += listener
    }

    /** Unsubscribes a listener added with [addContextMenuListener]. */
    fun removeContextMenuListener(listener: RibbonContextMenuListener) {
        contextMenuListeners -= listener
    }

    /** Subscribes to customization requests. */
    fun addCustomizeListener(listener: RibbonCustomizeListener) {
        customizeListeners += listener
    }

    /** Unsubscribes a listener added with [addCustomizeListener]. */
    fun removeCustomizeListener(listener: RibbonCustomizeListener) {
        customizeListeners -= listener
    }

    /** Subscribes to changes of floating panels. */
    fun addGroupFloatingListener(listener: RibbonGroupFloatingListener) {
        floatingListeners += listener
    }

    /** Unsubscribes a listener added with [addGroupFloatingListener]. */
    fun removeGroupFloatingListener(listener: RibbonGroupFloatingListener) {
        floatingListeners -= listener
    }

    /** Subscribes to Backstage opening and closing. */
    fun addBackstageListener(listener: RibbonBackstageListener) {
        backstageListeners += listener
    }

    /** Unsubscribes a listener added with [addBackstageListener]. */
    fun removeBackstageListener(listener: RibbonBackstageListener) {
        backstageListeners -= listener
    }

    /** Subscribes to QAT changes. */
    fun addQuickAccessListener(listener: RibbonQuickAccessListener) {
        quickAccessListeners += listener
    }

    /** Unsubscribes a listener added with [addQuickAccessListener]. */
    fun removeQuickAccessListener(listener: RibbonQuickAccessListener) {
        quickAccessListeners -= listener
    }

    /** Subscribes to the search shortcut (Alt+Q). */
    fun addSearchRequestListener(listener: RibbonSearchRequestListener) {
        searchRequestListeners += listener
    }

    /** Unsubscribes a listener added with [addSearchRequestListener]. */
    fun removeSearchRequestListener(listener: RibbonSearchRequestListener) {
        searchRequestListeners -= listener
    }

    /** Notifies that the state changed. */
    internal fun raiseStateChanged() = fireStateChanged()

    private fun fireStateChanged() {
        if (!loaded) return
        val event = EventObject(this)
        stateListeners.forEach { it.stateChanged(event) }
    }

    private fun fireQuickAccessChanged() {
        quickAccessListeners.forEach { it.quickAccessChanged(this) }
    }

    companion object {
        private const val CONTENT_PADDING = 2.0
        private const val FALLBACK_MARGIN = 60.0
        private const val DEFAULT_SEARCH_RESULTS = 12

        /** Whether this is a minimized state (tabs only, panel buttons, panel titles). */
        @JvmStatic
        fun isMinimizedMode(mode: RibbonVisibilityMode): Boolean =
            mode == RibbonVisibilityMode.TABS_ONLY || mode == RibbonVisibilityMode.PANEL_TITLES || mode == RibbonVisibilityMode.PANEL_BUTTONS

        private fun createRoot(): com.appkitbox.winui4k.internal.com.ComPtr {
            RibbonThemeResources.ensure()
            return Xaml.load(RibbonRootXaml.XAML)
        }
    }
}
