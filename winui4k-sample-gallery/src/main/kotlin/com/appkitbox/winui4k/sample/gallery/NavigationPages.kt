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
    val page = buildPage("NavigationView", "A control that provides an app's top-level navigation. Try out WNavigationView's various features.")

    page.add(buildSimpleNavigationViewExample())
    page.add(buildNavigationViewPaneExample())
    page.add(buildHierarchicalNavigationViewExample())
    return page
}

/** Basic navigation: items with icons, and responding to selection changes (SelectionChanged). */
private fun buildSimpleNavigationViewExample(): WComponent {
    val contentLabel = WLabel("Showing Home")
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

    val home = WNavigationViewItem("Home", Symbol.HOME)
    navigationView.addItem(home)
    navigationView.addItem(WNavigationViewItem("Mail", Symbol.MAIL))
    navigationView.addItem(WNavigationViewItem("Calendar", Symbol.CALENDAR))
    navigationView.addFooterItem(WNavigationViewItem("Help", Symbol.HELP))

    navigationView.addSelectionListener { item ->
        if (item != null) contentLabel.text = "Showing ${item.text}"
    }
    navigationView.selectedItem = home

    return buildExample("Simple navigation (MenuItems / Icon / SelectionChanged)", navigationView)
}

/** Controlling the pane: open/close, title, placement, and showing the settings item. */
private fun buildNavigationViewPaneExample(): WComponent {
    val navigationView = WNavigationView()
    navigationView.width = 480.0
    navigationView.height = 280.0
    navigationView.openPaneLength = 180.0
    navigationView.paneDisplayMode = NavigationViewPaneDisplayMode.LEFT
    navigationView.paneTitle = "Menu"
    navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
    navigationView.content = WLabel("Content area").also { it.margin = 16.0 }

    navigationView.addItem(WNavigationViewItem("Documents", Symbol.DOCUMENT))
    navigationView.addItem(WNavigationViewItem("Pictures", Symbol.PICTURES))
    navigationView.addItem(WNavigationViewItem("Music", Symbol.AUDIO))

    val toggleButton = WButton("Toggle pane")
    toggleButton.addActionListener { navigationView.isPaneOpen = !navigationView.isPaneOpen }

    val settingsButton = WButton("Toggle settings item")
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
    return buildExample("Controlling the pane (IsPaneOpen / PaneTitle / PaneDisplayMode / IsSettingsVisible)", body)
}

/** A hierarchical menu: nests child items under a parent item, and also responds to clicks (ItemInvoked). */
private fun buildHierarchicalNavigationViewExample(): WComponent {
    val result = WLabel("Clicked: none")

    val navigationView = WNavigationView()
    navigationView.width = 480.0
    navigationView.height = 280.0
    navigationView.openPaneLength = 180.0
    navigationView.paneDisplayMode = NavigationViewPaneDisplayMode.LEFT
    navigationView.isSettingsVisible = false
    navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
    navigationView.content = WLabel("Content area").also { it.margin = 16.0 }

    navigationView.addItem(WNavigationViewItem("Home", Symbol.HOME))

    // The parent item isn't selectable (SelectsOnInvoked=false); it only toggles its children open/closed
    val documents = WNavigationViewItem("Documents", Symbol.FOLDER)
    documents.selectsOnInvoked = false
    documents.isExpanded = true
    documents.addItem(WNavigationViewItem("Specs"))
    documents.addItem(WNavigationViewItem("Meeting notes"))
    navigationView.addItem(documents)

    navigationView.addItemInvokedListener { name ->
        result.text = "Clicked: $name"
    }

    val body = WPanel(spacing = 8.0)
    body.add(navigationView)
    body.add(result)
    return buildExample("A hierarchical menu and clicks (nested MenuItems / IsExpanded / ItemInvoked)", body)
}

// endregion

// region BreadcrumbBar page

/** The BreadcrumbBar page: lines up demos for trying out WBreadcrumbBar's hierarchy display and click-to-navigate. */
internal fun buildBreadcrumbBarPage(): WComponent {
    val page = buildPage("BreadcrumbBar", "A breadcrumb trail that shows your current position in a hierarchy and lets you jump back up it. Try out WBreadcrumbBar's various features.")

    page.add(buildBreadcrumbBarClickExample())
    return page
}

/** Truncating the breadcrumb by clicking a level (ItemClicked). */
private fun buildBreadcrumbBarClickExample(): WComponent {
    val fullPath = listOf("Home", "Documents", "2026", "Report")
    val result = WLabel("Click a level to go back to it")

    val breadcrumbBar = WBreadcrumbBar()
    breadcrumbBar.setItems(fullPath)
    breadcrumbBar.addItemClickedListener { index ->
        breadcrumbBar.setItems(fullPath.subList(0, index + 1))
        result.text = "Clicked: ${fullPath[index]} (index $index)"
    }

    val resetButton = WButton("Reset the hierarchy")
    resetButton.addActionListener {
        breadcrumbBar.setItems(fullPath)
        result.text = "Hierarchy reset"
    }

    val body = WPanel(spacing = 8.0)
    body.add(breadcrumbBar)
    body.add(result)
    body.add(resetButton)
    return buildExample("Displaying and clicking the hierarchy (ItemsSource / ItemClicked)", body)
}

// endregion

// region SelectorBar page

/** The SelectorBar page: lines up demos for trying out WSelectorBar's selection switching. */
internal fun buildSelectorBarPage(): WComponent {
    val page = buildPage("SelectorBar", "A control for switching between a small number of options. Try out WSelectorBar's various features.")

    page.add(buildSelectorBarSelectionExample())
    return page
}

/** Switching the selection (SelectionChanged) and setting SelectedItem. */
private fun buildSelectorBarSelectionExample(): WComponent {
    val result = WLabel("Showing Recent")

    val selectorBar = WSelectorBar()
    val labels = listOf("Recent", "Shared", "Favorites")
    labels.forEach { selectorBar.addItem(WSelectorBarItem(it)) }
    selectorBar.addSelectionListener { index ->
        if (index >= 0) result.text = "Showing ${labels[index]}"
    }
    selectorBar.selectedIndex = 0

    val body = WPanel(spacing = 8.0)
    body.add(selectorBar)
    body.add(result)
    return buildExample("Switching the selection (Items / SelectedItem / SelectionChanged)", body)
}

// endregion

// region TabView page

/** The TabView page: lines up demos for trying out WTabView's tab management. */
internal fun buildTabViewPage(): WComponent {
    val page = buildPage("TabView", "A control that switches between multiple pages via tabs. Try out WTabView's various features.")

    page.add(buildTabViewBasicExample())
    return page
}

/** Adding, removing, and switching tabs (TabItems / AddTabButtonClick / TabCloseRequested / SelectionChanged). */
private fun buildTabViewBasicExample(): WComponent {
    val result = WLabel("Selecting Tab 1")

    val tabView = WTabView()
    tabView.width = 480.0
    tabView.height = 240.0
    var tabNumber = 0

    fun newTab(): WTabViewItem {
        tabNumber++
        val content = WLabel("Tab $tabNumber's content")
        content.margin = 16.0
        val tab = WTabViewItem("Tab $tabNumber")
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
        if (index >= 0) result.text = "Selecting ${tabView.getTab(index).header}"
    }

    val body = WPanel(spacing = 8.0)
    body.add(tabView)
    body.add(result)
    return buildExample("Adding, removing, and switching tabs (TabItems / AddTabButtonClick / TabCloseRequested)", body)
}

// endregion
