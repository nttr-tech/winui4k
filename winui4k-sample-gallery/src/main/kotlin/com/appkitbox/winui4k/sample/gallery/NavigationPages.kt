package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.NavigationViewBackButtonVisible
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.WBreadcrumbBar
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WNavigationView
import com.appkitbox.winui4k.WNavigationViewItem
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WSelectorBar
import com.appkitbox.winui4k.WSelectorBarItem
import com.appkitbox.winui4k.WTabView
import com.appkitbox.winui4k.WTabViewItem

/*
 * Navigation category: demo pages for NavigationView.
 */

// region NavigationView

/** The NavigationView page: lines up demos for trying out WNavigationView's various features. */
internal fun buildNavigationViewPage(): WComponent {
    val page = buildPage("NavigationView", Messages.get("navigationview.description"))

    page.add(buildSimpleNavigationViewExample())
    page.add(buildNavigationViewPaneExample())
    page.add(buildHierarchicalNavigationViewExample())
    return page
}

/** Basic navigation: items with icons, and responding to selection changes (SelectionChanged). */
private fun buildSimpleNavigationViewExample(): WComponent {
    val contentLabel = WLabel(Messages.get("navigationview.simple.showing", Messages.get("navigationview.item.home")))
    contentLabel.margin = 16.0

    val navigationView = WNavigationView()
    navigationView.width = 480.0
    navigationView.height = 280.0
    navigationView.openPaneLength = 160.0
    // The demo is narrower than the Auto mode threshold, so always show the left pane
    navigationView.paneDisplayMode = NavigationViewPaneDisplayMode.LEFT
    navigationView.isSettingsVisible = false
    navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
    navigationView.content = contentLabel

    val home = WNavigationViewItem(Messages.get("navigationview.item.home"), Symbol.HOME)
    navigationView.addItem(home)
    navigationView.addItem(WNavigationViewItem(Messages.get("navigationview.item.mail"), Symbol.MAIL))
    navigationView.addItem(WNavigationViewItem(Messages.get("navigationview.item.calendar"), Symbol.CALENDAR))
    navigationView.addFooterItem(WNavigationViewItem(Messages.get("navigationview.item.help"), Symbol.HELP))

    navigationView.addSelectionListener { item ->
        if (item != null) contentLabel.text = Messages.get("navigationview.simple.showing", item.text)
    }
    navigationView.selectedItem = home

    return buildExample(Messages.get("navigationview.example.simple.title"), navigationView)
}

/** Controlling the pane: open/close, title, placement, and showing the settings item. */
private fun buildNavigationViewPaneExample(): WComponent {
    val navigationView = WNavigationView()
    navigationView.width = 480.0
    navigationView.height = 280.0
    navigationView.openPaneLength = 180.0
    navigationView.paneDisplayMode = NavigationViewPaneDisplayMode.LEFT
    navigationView.paneTitle = Messages.get("navigationview.pane.title")
    navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
    navigationView.content = WLabel(Messages.get("navigationview.content")).also { it.margin = 16.0 }

    navigationView.addItem(WNavigationViewItem(Messages.get("navigationview.item.documents"), Symbol.DOCUMENT))
    navigationView.addItem(WNavigationViewItem(Messages.get("navigationview.item.pictures"), Symbol.PICTURES))
    navigationView.addItem(WNavigationViewItem(Messages.get("navigationview.item.music"), Symbol.AUDIO))

    val toggleButton = WButton(Messages.get("navigationview.button.togglepane"))
    toggleButton.addActionListener { navigationView.isPaneOpen = !navigationView.isPaneOpen }

    val settingsButton = WButton(Messages.get("navigationview.button.togglesettings"))
    settingsButton.addActionListener { navigationView.isSettingsVisible = !navigationView.isSettingsVisible }

    val modeButtons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    modeButtons.add(WLabel("PaneDisplayMode:"))
    for (mode in NavigationViewPaneDisplayMode.entries) {
        modeButtons.add(
            WButton(mode.name).also { button ->
                button.addActionListener { navigationView.paneDisplayMode = mode }
            },
        )
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(toggleButton)
    buttons.add(settingsButton)

    val body = WPanel(spacing = 8.0)
    body.add(navigationView)
    body.add(buttons)
    body.add(modeButtons)
    return buildExample(Messages.get("navigationview.example.pane.title"), body)
}

/** A hierarchical menu: nests child items under a parent item, and also responds to clicks (ItemInvoked). */
private fun buildHierarchicalNavigationViewExample(): WComponent {
    val result = WLabel(Messages.get("navigationview.invoked.none"))

    val navigationView = WNavigationView()
    navigationView.width = 480.0
    navigationView.height = 280.0
    navigationView.openPaneLength = 180.0
    navigationView.paneDisplayMode = NavigationViewPaneDisplayMode.LEFT
    navigationView.isSettingsVisible = false
    navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
    navigationView.content = WLabel(Messages.get("navigationview.content")).also { it.margin = 16.0 }

    navigationView.addItem(WNavigationViewItem(Messages.get("navigationview.item.home"), Symbol.HOME))

    // The parent item isn't selectable (SelectsOnInvoked=false); it only toggles its children open/closed
    val documents = WNavigationViewItem(Messages.get("navigationview.item.documents"), Symbol.FOLDER)
    documents.selectsOnInvoked = false
    documents.isExpanded = true
    documents.addItem(WNavigationViewItem(Messages.get("navigationview.item.specs")))
    documents.addItem(WNavigationViewItem(Messages.get("navigationview.item.minutes")))
    navigationView.addItem(documents)

    navigationView.addItemInvokedListener { name ->
        result.text = Messages.get("navigationview.invoked", name)
    }

    val body = WPanel(spacing = 8.0)
    body.add(navigationView)
    body.add(result)
    return buildExample(Messages.get("navigationview.example.hierarchical.title"), body)
}

// endregion

// region BreadcrumbBar page

/** The BreadcrumbBar page: lines up demos for trying out WBreadcrumbBar's hierarchy display and click-to-navigate. */
internal fun buildBreadcrumbBarPage(): WComponent {
    val page = buildPage("BreadcrumbBar", Messages.get("breadcrumbbar.description"))

    page.add(buildBreadcrumbBarClickExample())
    return page
}

/** Truncating the breadcrumb by clicking a level (ItemClicked). */
private fun buildBreadcrumbBarClickExample(): WComponent {
    val fullPath = listOf(
        Messages.get("breadcrumbbar.item.home"),
        Messages.get("breadcrumbbar.item.documents"),
        Messages.get("breadcrumbbar.item.year"),
        Messages.get("breadcrumbbar.item.reports"),
    )
    val result = WLabel(Messages.get("breadcrumbbar.result.hint"))

    val breadcrumbBar = WBreadcrumbBar()
    breadcrumbBar.setItems(fullPath)
    breadcrumbBar.addItemClickedListener { index ->
        breadcrumbBar.setItems(fullPath.subList(0, index + 1))
        result.text = Messages.get("breadcrumbbar.result.clicked", fullPath[index], index.toString())
    }

    val resetButton = WButton(Messages.get("breadcrumbbar.button.reset"))
    resetButton.addActionListener {
        breadcrumbBar.setItems(fullPath)
        result.text = Messages.get("breadcrumbbar.result.reset")
    }

    val body = WPanel(spacing = 8.0)
    body.add(breadcrumbBar)
    body.add(result)
    body.add(resetButton)
    return buildExample(Messages.get("breadcrumbbar.example.click.title"), body)
}

// endregion

// region SelectorBar page

/** The SelectorBar page: lines up demos for trying out WSelectorBar's selection switching. */
internal fun buildSelectorBarPage(): WComponent {
    val page = buildPage("SelectorBar", Messages.get("selectorbar.description"))

    page.add(buildSelectorBarSelectionExample())
    return page
}

/** Switching the selection (SelectionChanged) and setting SelectedItem. */
private fun buildSelectorBarSelectionExample(): WComponent {
    val result = WLabel(Messages.get("selectorbar.result.showing", Messages.get("selectorbar.item.recent")))

    val selectorBar = WSelectorBar()
    val labels = listOf(
        Messages.get("selectorbar.item.recent"),
        Messages.get("selectorbar.item.shared"),
        Messages.get("selectorbar.item.favorites"),
    )
    labels.forEach { selectorBar.addItem(WSelectorBarItem(it)) }
    selectorBar.addSelectionListener { index ->
        if (index >= 0) result.text = Messages.get("selectorbar.result.showing", labels[index])
    }
    selectorBar.selectedIndex = 0

    val body = WPanel(spacing = 8.0)
    body.add(selectorBar)
    body.add(result)
    return buildExample(Messages.get("selectorbar.example.selection.title"), body)
}

// endregion

// region TabView page

/** The TabView page: lines up demos for trying out WTabView's tab management. */
internal fun buildTabViewPage(): WComponent {
    val page = buildPage("TabView", Messages.get("tabview.description"))

    page.add(buildTabViewBasicExample())
    return page
}

/** Adding, removing, and switching tabs (TabItems / AddTabButtonClick / TabCloseRequested / SelectionChanged). */
private fun buildTabViewBasicExample(): WComponent {
    val result = WLabel(Messages.get("tabview.result.selected", Messages.get("tabview.tab.header", "1")))

    val tabView = WTabView()
    tabView.width = 480.0
    tabView.height = 240.0
    var tabNumber = 0

    fun newTab(): WTabViewItem {
        tabNumber++
        val content = WLabel(Messages.get("tabview.tab.content", tabNumber.toString()))
        content.margin = 16.0
        val tab = WTabViewItem(Messages.get("tabview.tab.header", tabNumber.toString()))
        tab.isClosable = true // Show the close button (WTabViewItem does not show it by default, as with JTabbedPane)
        tab.content = content
        return tab
    }

    repeat(3) { tabView.addTab(newTab()) }
    tabView.selectedIndex = 0

    // Add via the "+" button, remove via the close button (TabView doesn't close tabs
    // automatically, so removeTab is called explicitly)
    tabView.addAddTabButtonClickListener {
        tabView.addTab(newTab())
        tabView.selectedIndex = tabView.tabCount - 1
    }
    tabView.addTabCloseRequestedListener { index ->
        if (tabView.tabCount > 1) tabView.removeTab(index)
    }
    tabView.addSelectionListener {
        val index = tabView.selectedIndex
        if (index >= 0) result.text = Messages.get("tabview.result.selected", tabView.getTab(index).header)
    }

    val body = WPanel(spacing = 8.0)
    body.add(tabView)
    body.add(result)
    return buildExample(Messages.get("tabview.example.basic.title"), body)
}

// endregion
