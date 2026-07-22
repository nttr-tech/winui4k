package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.ElementTheme
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.SystemBackdropType
import com.appkitbox.winui4k.TextChangeReason
import com.appkitbox.winui4k.TitleBarHeightOption
import com.appkitbox.winui4k.TitleBarTheme
import com.appkitbox.winui4k.WAutoSuggestBox
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WTitleBar
import com.appkitbox.winui4k.WinUiUtilities
import java.io.File

/**
 * A WinUI 3 Gallery-style component gallery.
 * Shows a page navigation list on the left and the selected component's demo page on the right.
 * Add more pages to [pages] as they're added.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // Declarative UI-building + a long list of page registrations
fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame(title = "WinUI4K Gallery")

        // The root that hosts the title bar and navigation. Settings' App theme is set here as
        // RequestedTheme and applies to every element in the window
        val rootGrid = WGrid()

        // Applies the app theme and title bar coloring saved on the Settings page
        fun applyAppTheme(appTheme: String) {
            val theme = elementThemeOf(appTheme)
            rootGrid.requestedTheme = theme
            frame.appWindow.titleBar.preferredTheme = when (theme) {
                ElementTheme.LIGHT -> TitleBarTheme.LIGHT
                ElementTheme.DARK -> TitleBarTheme.DARK
                ElementTheme.DEFAULT -> TitleBarTheme.USE_DEFAULT_APP_MODE
            }
        }
        applyAppTheme(GallerySettings.appTheme)
        // Lets pages built from here on pick colors (CARD_BACKGROUND etc.) for the current theme
        isDarkTheme = rootGrid.actualTheme == ElementTheme.DARK

        // Content gets set by the initial selection when the navigation is built
        val pageArea = WPanel()
        pageArea.margin = 24.0

        // The content area is translucent white, matching the real Gallery's Layer; the Mica behind it shows through faintly
        val pageBackground = WBorder(WScrollPane(pageArea))
        pageBackground.background = PAGE_BACKGROUND

        // History of page names to go back through via the back button (null = the startup home state).
        // Assigning selectedItem also fires SelectionChanged, so isNavigatingBack marks "currently going
        // back" to avoid re-pushing onto the history.
        val history = ArrayDeque<String?>()
        var isNavigatingBack = false
        var currentPageName: String? = null

        val titleBar = WTitleBar()
        titleBar.title = "WinUI4K Gallery"
        titleBar.isPaneToggleButtonVisible = true
        // Don't show the back button until a page is selected (the real Gallery can't go back from home)
        titleBar.isBackButtonVisible = false

        // The home state shown when Home is selected in the navigation, and when going back through the whole history
        lateinit var navigation: GalleryNavigation
        fun showHome() {
            currentPageName = null
            titleBar.isBackButtonVisible = history.isNotEmpty()
            // Home shows the hero image flush to the edges, so no margin here (each page uses 24)
            pageArea.margin = 0.0
            pageArea.removeAll()
            pageArea.add(
                buildHomePage { name ->
                    navigation.itemsByPageName[name]?.let {
                        navigation.navigationView.selectedItem = it
                    }
                },
            )
        }

        // The Settings page shown when the pane's bottom-left Settings (gear) item is selected
        fun showSettings() {
            if (!isNavigatingBack && currentPageName != SETTINGS_PAGE_NAME) {
                history.addLast(currentPageName)
            }
            currentPageName = SETTINGS_PAGE_NAME
            titleBar.isBackButtonVisible = true
            pageArea.margin = 24.0
            pageArea.removeAll()
            pageArea.add(buildSettingsPage(navigation.navigationView, ::applyAppTheme))
        }

        // Set the same icon as the real WinUI 3 Gallery on the title bar and taskbar
        val iconFile = extractGalleryIcon()
        if (iconFile != null) {
            titleBar.iconUri = iconFile.toPath().toUri().toString()
            frame.appWindow.setIcon(iconFile.absolutePath)
        }

        // Like the real Gallery, put a page search box in the middle of the title bar
        // (width 580 also matches the real Gallery's MaxWidth)
        val searchBox = WAutoSuggestBox("Search controls and samples...")
        searchBox.width = 580.0
        searchBox.queryIcon = Symbol.FIND // the same magnifying glass as the real Gallery's QueryIcon="Find"
        titleBar.content = searchBox

        navigation = buildGalleryNavigationView(
            onHome = {
                if (!isNavigatingBack && currentPageName != null) {
                    history.addLast(currentPageName) // push onto history so a page can go back to Home
                }
                showHome()
            },
            onSettings = ::showSettings,
        ) { name, buildPage ->
            if (!isNavigatingBack) {
                history.addLast(currentPageName) // transitions from home push null
            }
            currentPageName = name
            GallerySettings.addRecentlyVisited(name) // list it under Home's Recently visited
            titleBar.isBackButtonVisible = true
            pageArea.margin = 24.0
            pageArea.removeAll()
            pageArea.add(buildPage())
            // The TitleBar page's demo creates a separate WTitleBar for illustration. WinUI 3's
            // TitleBar control has the behavior of overwriting its ancestor window's real title
            // (Window.Title / AppWindow.Title) with its own Title when Loaded fires (an async
            // timing after this callback), so schedule another pass once layout has settled to
            // restore the main title.
            frame.title = "WinUI4K Gallery"
            WinUiUtilities.schedule(200) { frame.title = "WinUI4K Gallery" }
        }
        val navigationView = navigation.navigationView
        navigationView.content = pageBackground
        // Consolidate the pane-toggle button onto the title bar to avoid showing it twice
        navigationView.isPaneToggleButtonVisible = false
        // Restore the navigation placement (Left / Top) saved on the Settings page
        if (GallerySettings.navigationStyle == "Top") {
            navigationView.paneDisplayMode = NavigationViewPaneDisplayMode.TOP
        }

        titleBar.addBackRequestedListener {
            if (history.isEmpty()) return@addBackRequestedListener
            val previousName = history.removeLast()
            // null means home; go through SelectionChanged to show the right page (or Home / Settings)
            val previousItem = when (previousName) {
                null -> navigation.homeItem
                SETTINGS_PAGE_NAME -> navigation.settingsItem
                else -> navigation.itemsByPageName[previousName] ?: return@addBackRequestedListener
            }
            isNavigatingBack = true
            navigationView.selectedItem = previousItem
            isNavigatingBack = false
        }
        titleBar.addPaneToggleRequestedListener {
            navigationView.isPaneOpen = !navigationView.isPaneOpen
        }

        // Narrow down page names as the search box is typed into, and navigate to the chosen page on submit
        val pageNames = pages.keys.toList()
        searchBox.addTextChangedListener { text, reason ->
            if (reason == TextChangeReason.USER_INPUT) {
                searchBox.setSuggestions(pageNames.filter { it.contains(text, ignoreCase = true) })
            }
        }
        searchBox.addQuerySubmittedListener { query, chosen ->
            val target = chosen ?: pageNames.firstOrNull { it.contains(query, ignoreCase = true) }
            val item = target?.let { navigation.itemsByPageName[it] }
            if (item != null) {
                navigationView.selectedItem = item
                searchBox.text = ""
            }
        }

        rootGrid.addRow(GridLength.AUTO)
        rootGrid.addRow(GridLength.star())
        rootGrid.add(titleBar, row = 0, column = 0)
        rootGrid.add(navigationView, row = 1, column = 0)

        // When the theme changes, rebuild the visible page to re-pick colors (CARD_BACKGROUND etc.).
        // To avoid destroying the source of the change (the Settings page's combo box) while it's
        // still handling its own event, defer the rebuild onto the message loop
        rootGrid.addActualThemeChangedListener {
            isDarkTheme = rootGrid.actualTheme == ElementTheme.DARK
            pageBackground.background = PAGE_BACKGROUND
            WinUiUtilities.invokeLater {
                when (val name = currentPageName) {
                    null -> showHome()
                    SETTINGS_PAGE_NAME -> showSettings()
                    else -> {
                        pageArea.removeAll()
                        pageArea.add(pages.getValue(name)())
                    }
                }
            }
        }

        // Select Home on launch (SelectionChanged -> showHome)
        navigationView.selectedItem = navigation.homeItem

        frame.setContentPane(rootGrid)
        frame.extendsContentIntoTitleBar = true
        frame.setTitleBar(titleBar)
        frame.appWindow.titleBar.preferredHeightOption = TitleBarHeightOption.TALL
        // Mica, matching the real Gallery, lets the wallpaper's color (a pale blue with the default wallpaper) show through faintly across the whole window
        frame.systemBackdrop = SystemBackdropType.MICA
        frame.isVisible = true
    }
}

/** Converts the app theme saved on the Settings page ("Light" / "Dark" / "Default") to an [ElementTheme]. */
private fun elementThemeOf(appTheme: String): ElementTheme = when (appTheme) {
    "Light" -> ElementTheme.LIGHT
    "Dark" -> ElementTheme.DARK
    else -> ElementTheme.DEFAULT
}

/**
 * Extracts the same icon as the real WinUI 3 Gallery (GalleryIcon.ico) from resources to a temp file.
 * WinUI's BitmapImage / AppWindow.SetIcon only accept a URI or file path.
 */
private fun extractGalleryIcon(): File? {
    val resource = object {}.javaClass.getResourceAsStream("/GalleryIcon.ico") ?: return null
    val file = File.createTempFile("winui4k-sample-gallery-icon-", ".ico")
    file.deleteOnExit()
    resource.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    return file
}
