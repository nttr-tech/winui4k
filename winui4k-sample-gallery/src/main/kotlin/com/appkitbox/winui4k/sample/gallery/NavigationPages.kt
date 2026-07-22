package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.NavigationViewBackButtonVisible
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WNavigationView
import com.appkitbox.winui4k.WNavigationViewItem
import com.appkitbox.winui4k.WPanel

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
