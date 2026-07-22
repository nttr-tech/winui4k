package com.appkitbox.winui4k.gallery

import com.appkitbox.winui4k.coroutines.WinUi
import com.appkitbox.winui4k.BadgeGlyph
import com.appkitbox.winui4k.ColorSpectrumShape
import com.appkitbox.winui4k.CommandBarDefaultLabelPosition
import com.appkitbox.winui4k.ContentDialogButton
import com.appkitbox.winui4k.ContentDialogResult
import com.appkitbox.winui4k.ExpandDirection
import com.appkitbox.winui4k.FlyoutPlacement
import com.appkitbox.winui4k.GridLength
import com.appkitbox.winui4k.HorizontalAlignment
import com.appkitbox.winui4k.ListViewSelectionMode
import com.appkitbox.winui4k.NavigationViewBackButtonVisible
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode
import com.appkitbox.winui4k.NotificationDuration
import com.appkitbox.winui4k.NotificationScenario
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.SliderSnapsTo
import com.appkitbox.winui4k.SplitViewDisplayMode
import com.appkitbox.winui4k.SplitViewPanePlacement
import com.appkitbox.winui4k.StandardUICommandKind
import com.appkitbox.winui4k.SystemBackdropType
import com.appkitbox.winui4k.SwipeMode
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.VirtualKey
import com.appkitbox.winui4k.VirtualKeyModifier
import com.appkitbox.winui4k.PasswordRevealMode
import com.appkitbox.winui4k.SpinButtonPlacementMode
import com.appkitbox.winui4k.TeachingTipCloseReason
import com.appkitbox.winui4k.TeachingTipPlacement
import com.appkitbox.winui4k.TextChangeReason
import com.appkitbox.winui4k.TextTrimming
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.TitleBarHeightOption
import com.appkitbox.winui4k.WAutoSuggestBox
import com.appkitbox.winui4k.TickPlacement
import com.appkitbox.winui4k.WAppBarButton
import com.appkitbox.winui4k.WAppBarSeparator
import com.appkitbox.winui4k.WAppBarToggleButton
import com.appkitbox.winui4k.WAppNotification
import com.appkitbox.winui4k.WAppNotificationManager
import com.appkitbox.winui4k.WBadgeNotification
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCanvas
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WColor
import com.appkitbox.winui4k.WColorPicker
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WCommand
import com.appkitbox.winui4k.WCommandBar
import com.appkitbox.winui4k.WCommandBarFlyout
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WContentDialog
import com.appkitbox.winui4k.WDropDownButton
import com.appkitbox.winui4k.WExpander
import com.appkitbox.winui4k.WFlyout
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WGrid
import com.appkitbox.winui4k.WHyperlinkButton
import com.appkitbox.winui4k.WJumpList
import com.appkitbox.winui4k.WJumpListItem
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WLinearGradientPaint
import com.appkitbox.winui4k.WList
import com.appkitbox.winui4k.WListBox
import com.appkitbox.winui4k.SelectionMode
import com.appkitbox.winui4k.WMenuBar
import com.appkitbox.winui4k.WMenuBarItem
import com.appkitbox.winui4k.WMenuFlyout
import com.appkitbox.winui4k.WMenuFlyoutItem
import com.appkitbox.winui4k.WMenuFlyoutSeparator
import com.appkitbox.winui4k.WMenuFlyoutSubItem
import com.appkitbox.winui4k.WNavigationView
import com.appkitbox.winui4k.WNavigationViewItem
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WPasswordField
import com.appkitbox.winui4k.WPopup
import com.appkitbox.winui4k.WRadioButton
import com.appkitbox.winui4k.WRadioMenuFlyoutItem
import com.appkitbox.winui4k.WRatingControl
import com.appkitbox.winui4k.WRelativePanel
import com.appkitbox.winui4k.WRepeatButton
import com.appkitbox.winui4k.WRichTextBlock
import com.appkitbox.winui4k.WScrollPane
import com.appkitbox.winui4k.WSlider
import com.appkitbox.winui4k.WSpinner
import com.appkitbox.winui4k.WSplitButton
import com.appkitbox.winui4k.SortDirection
import com.appkitbox.winui4k.WSplitView
import com.appkitbox.winui4k.WStandardUICommand
import com.appkitbox.winui4k.WSwipeControl
import com.appkitbox.winui4k.WSwipeItem
import com.appkitbox.winui4k.WSwipeItems
import com.appkitbox.winui4k.WTable
import com.appkitbox.winui4k.WTableColumn
import com.appkitbox.winui4k.WTeachingTip
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WTextPane
import com.appkitbox.winui4k.WTitleBar
import com.appkitbox.winui4k.WToggleButton
import com.appkitbox.winui4k.WToggleMenuFlyoutItem
import com.appkitbox.winui4k.WToggleSplitButton
import com.appkitbox.winui4k.WToggleSwitch
import com.appkitbox.winui4k.TreeViewSelectionMode
import com.appkitbox.winui4k.WTree
import com.appkitbox.winui4k.WTreeNode
import com.appkitbox.winui4k.WVariableSizedWrapGrid
import com.appkitbox.winui4k.WXamlUICommand
import com.appkitbox.winui4k.WinUiUtilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A WinUI 3 Gallery-style component gallery.
 * Shows a page navigation list on the left and the selected component's demo page on the right.
 * Add more pages to [pages] as they're added.
 */
fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame(title = "WinUI4K Gallery")

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

        titleBar.addBackRequestedListener {
            if (history.isEmpty()) return@addBackRequestedListener
            val previousName = history.removeLast()
            // null means home; go through SelectionChanged to show the right page (or Home)
            val previousItem =
                if (previousName == null) navigation.homeItem
                else navigation.itemsByPageName[previousName] ?: return@addBackRequestedListener
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

        val rootGrid = WGrid()
        rootGrid.addRow(GridLength.AUTO)
        rootGrid.addRow(GridLength.star())
        rootGrid.add(titleBar, row = 0, column = 0)
        rootGrid.add(navigationView, row = 1, column = 0)

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

/**
 * Extracts the same icon as the real WinUI 3 Gallery (GalleryIcon.ico) from resources to a temp file.
 * WinUI's BitmapImage / AppWindow.SetIcon only accept a URI or file path.
 */
private fun extractGalleryIcon(): File? {
    val resource = object {}.javaClass.getResourceAsStream("/GalleryIcon.ico") ?: return null
    val file = File.createTempFile("winui4k-gallery-icon-", ".ico")
    file.deleteOnExit()
    resource.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    return file
}

// Colors matching the WinUI 3 Gallery's light theme
/** The content area's background (translucent white, matching LayerFillColorDefault). The Mica behind it shows through faintly. */
private val PAGE_BACKGROUND = WColor(255, 255, 255, 128)

/** The background of cards that host demos (translucent white, matching CardBackgroundFillColorDefault). */
private val CARD_BACKGROUND = WColor(255, 255, 255, 179)

/** A card's border. */
private val CARD_BORDER = WColor(229, 229, 229)

/** A subdued text color for things like page descriptions. */
internal val TEXT_SECONDARY = WColor(97, 97, 97)

/** Page name (WinUI control name) -> the function that builds its demo page. */
internal val pages: Map<String, () -> WComponent> = linkedMapOf(
    "AppBarButton" to ::buildAppBarButtonPage,
    "AppBarSeparator" to ::buildAppBarSeparatorPage,
    "AppBarToggleButton" to ::buildAppBarToggleButtonPage,
    "AutoSuggestBox" to ::buildAutoSuggestBoxPage,
    "AppNotification" to ::buildAppNotificationPage,
    "BadgeNotification" to ::buildBadgeNotificationPage,
    "Border" to ::buildBorderPage,
    "Button" to ::buildButtonPage,
    "Canvas" to ::buildCanvasPage,
    "CheckBox" to ::buildCheckBoxPage,
    "ColorPicker" to ::buildColorPickerPage,
    "ComboBox" to ::buildComboBoxPage,
    "CommandBar" to ::buildCommandBarPage,
    "CommandBarFlyout" to ::buildCommandBarFlyoutPage,
    "ContentDialog" to ::buildContentDialogPage,
    "DropDownButton" to ::buildDropDownButtonPage,
    "Expander" to ::buildExpanderPage,
    "Flyout" to ::buildFlyoutPage,
    "Grid" to ::buildGridPage,
    "HyperlinkButton" to ::buildHyperlinkButtonPage,
    "JumpList" to ::buildJumpListPage,
    "ListBox" to ::buildListBoxPage,
    "ListView" to ::buildListViewPage,
    "MenuBar" to ::buildMenuBarPage,
    "MenuFlyout" to ::buildMenuFlyoutPage,
    "NavigationView" to ::buildNavigationViewPage,
    "NumberBox" to ::buildNumberBoxPage,
    "PasswordBox" to ::buildPasswordBoxPage,
    "Popup" to ::buildPopupPage,
    "RadioButton" to ::buildRadioButtonPage,
    "RatingControl" to ::buildRatingControlPage,
    "RelativePanel" to ::buildRelativePanelPage,
    "RepeatButton" to ::buildRepeatButtonPage,
    "RichEditBox" to ::buildRichEditBoxPage,
    "RichTextBlock" to ::buildRichTextBlockPage,
    "Slider" to ::buildSliderPage,
    "SplitButton" to ::buildSplitButtonPage,
    "SplitView" to ::buildSplitViewPage,
    "StackPanel" to ::buildStackPanelPage,
    "StandardUICommand" to ::buildStandardUICommandPage,
    "SwipeControl" to ::buildSwipeControlPage,
    "TableView" to ::buildTableViewPage,
    "TeachingTip" to ::buildTeachingTipPage,
    "TextBlock" to ::buildTextBlockPage,
    "TextBox" to ::buildTextBoxPage,
    "ToggleButton" to ::buildToggleButtonPage,
    "ToggleSplitButton" to ::buildToggleSplitButtonPage,
    "ToggleSwitch" to ::buildToggleSwitchPage,
    "TreeView" to ::buildTreeViewPage,
    "VariableSizedWrapGrid" to ::buildVariableSizedWrapGridPage,
    "XamlUICommand" to ::buildXamlUICommandPage,
    "AppWindow" to ::buildAppWindowPage,
    "AppWindowTitleBar" to ::buildAppWindowTitleBarPage,
    "Multiple windows" to ::buildMultipleWindowsPage,
    "SystemBackdrop" to ::buildSystemBackdropPage,
    "TitleBar" to ::buildTitleBarPage,
)

/** Navigation categories (matching the real WinUI 3 Gallery's grouping) -> the page names in each. */
private val categories: Map<String, List<String>> = linkedMapOf(
    "Basic input" to listOf(
        "Button",
        "CheckBox",
        "ColorPicker",
        "ComboBox",
        "DropDownButton",
        "HyperlinkButton",
        "RadioButton",
        "RatingControl",
        "RepeatButton",
        "Slider",
        "SplitButton",
        "ToggleButton",
        "ToggleSplitButton",
        "ToggleSwitch",
    ),
    "Collections" to listOf(
        "ListBox",
        "ListView",
        "TableView",
        "TreeView",
    ),
    "Dialogs & flyouts" to listOf(
        "ContentDialog",
        "Flyout",
        "Popup",
        "TeachingTip",
    ),
    "Layout" to listOf(
        "Border",
        "Canvas",
        "Expander",
        "Grid",
        "RelativePanel",
        "SplitView",
        "StackPanel",
        "VariableSizedWrapGrid",
    ),
    "Menus & toolbars" to listOf(
        "AppBarButton",
        "AppBarSeparator",
        "AppBarToggleButton",
        "CommandBar",
        "CommandBarFlyout",
        "MenuBar",
        "MenuFlyout",
        "SwipeControl",
        "StandardUICommand",
        "XamlUICommand",
    ),
    "Navigation" to listOf(
        "NavigationView",
    ),
    "Shell" to listOf(
        "AppNotification",
        "BadgeNotification",
        "JumpList",
    ),
    "Text" to listOf(
        "AutoSuggestBox",
        "NumberBox",
        "PasswordBox",
        "RichEditBox",
        "RichTextBlock",
        "TextBlock",
        "TextBox",
    ),
    "Windowing" to listOf(
        "AppWindow",
        "AppWindowTitleBar",
        "Multiple windows",
        "SystemBackdrop",
        "TitleBar",
    ),
)

/** Category name -> the icon shown to the left of the category name in the navigation. */
private val categoryIcons: Map<String, Symbol> = mapOf(
    "Basic input" to Symbol.KEYBOARD,
    "Collections" to Symbol.LIST,
    "Dialogs & flyouts" to Symbol.COMMENT,
    "Layout" to Symbol.VIEW_ALL,
    "Menus & toolbars" to Symbol.SAVE,
    "Navigation" to Symbol.GLOBAL_NAVIGATION_BUTTON,
    "Shell" to Symbol.MESSAGE,
    "Text" to Symbol.FONT,
    "Windowing" to Symbol.NEW_WINDOW,
)

/**
 * [buildGalleryNavigationView]'s return value: the navigation itself, plus a "page name ->
 * WNavigationViewItem" lookup used to sync selection for the back button.
 */
private class GalleryNavigation(
    val navigationView: WNavigationView,
    val homeItem: WNavigationViewItem,
    val itemsByPageName: Map<String, WNavigationViewItem>,
)

/**
 * The left-hand navigation. Puts Home first, then lines up page items under each category's
 * parent item (with an icon) below it. Selecting Home passes control to [onHome]; selecting a
 * page passes the selected page's name and builder to [onSelect].
 */
private fun buildGalleryNavigationView(
    onHome: () -> Unit,
    onSelect: (String, () -> WComponent) -> Unit,
): GalleryNavigation {
    val navigationView = WNavigationView()
    navigationView.isSettingsVisible = false
    navigationView.isBackButtonVisible = NavigationViewBackButtonVisible.COLLAPSED
    navigationView.openPaneLength = 260.0

    // Put Home first, matching the real Gallery
    val homeItem = WNavigationViewItem("Home", Symbol.HOME)
    navigationView.addItem(homeItem)

    val itemsByPageName = mutableMapOf<String, WNavigationViewItem>()
    for ((category, names) in categories) {
        // Categories aren't selectable (SelectsOnInvoked=false); they only toggle their children open/closed
        val categoryItem = WNavigationViewItem(category, categoryIcons[category])
        categoryItem.selectsOnInvoked = false
        categoryItem.isExpanded = false // start collapsed on launch
        for (name in names) {
            val pageItem = WNavigationViewItem(name)
            categoryItem.addItem(pageItem)
            itemsByPageName[name] = pageItem
        }
        navigationView.addItem(categoryItem)
    }

    navigationView.addSelectionListener { item ->
        if (item == null) return@addSelectionListener
        if (item == homeItem) {
            onHome()
        } else {
            pages[item.text]?.let { buildPage -> onSelect(item.text, buildPage) }
        }
    }
    return GalleryNavigation(navigationView, homeItem, itemsByPageName)
}

/** A page's skeleton (large heading + favorite star + description). Each page adds its demos onto this return value. */
internal fun buildPage(title: String, description: String): WPanel {
    val titleRow = WPanel(spacing = 12.0, orientation = Orientation.HORIZONTAL)
    titleRow.add(WLabel(title).also { it.fontSize = 28.0; it.fontWeight = 600 })
    if (title in pages) {
        titleRow.add(buildFavoriteToggle(title).also { it.verticalAlignment = VerticalAlignment.CENTER })
    }

    val header = WPanel(spacing = 4.0)
    header.add(titleRow)
    header.add(
        WLabel(description).also {
            it.foreground = TEXT_SECONDARY
            it.textWrapping = TextWrapping.WRAP
        },
    )

    val page = WPanel(spacing = 24.0)
    page.add(header)
    return page
}

/** The Button page: lines up demos for trying out WButton's various features. */
private fun buildButtonPage(): WComponent {
    val page = buildPage("Button", "A button that responds to clicks. Try out WButton's various features.")

    page.add(buildSimpleButtonExample())
    page.add(buildFlyoutButtonExample())
    page.add(buildCommandButtonExample())
    page.add(buildCoroutineButtonExample())
    return page
}

/** One demo section (heading + body placed on a card). */
internal fun buildExample(title: String, body: WComponent): WComponent {
    // Like the real Gallery, keep the demo body from stretching to the card's
    // full width; left-align it instead
    body.horizontalAlignment = HorizontalAlignment.LEFT

    val card = WBorder(body)
    card.background = CARD_BACKGROUND
    card.borderColor = CARD_BORDER
    card.borderThickness = 1.0
    card.cornerRadius = 8.0
    card.padding = 16.0

    val section = WPanel(spacing = 8.0)
    section.add(WLabel(title).also { it.fontWeight = 600; it.textWrapping = TextWrapping.WRAP })
    section.add(card)
    return section
}

/** A basic button: responding to clicks and toggling isEnabled. */
private fun buildSimpleButtonExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val standardButton = WButton("Standard XAML Button")
    standardButton.addActionListener {
        count++
        result.text = "Click count: $count"
    }

    val toggleButton = WButton("Disable button")
    toggleButton.addActionListener {
        standardButton.isEnabled = !standardButton.isEnabled
        toggleButton.text = if (standardButton.isEnabled) "Disable button" else "Enable button"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(standardButton)
    row.add(toggleButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Simple button", row)
}

/** A button with a flyout: opens a popup on click. */
private fun buildFlyoutButtonExample(): WComponent {
    val flyoutContent = WPanel(spacing = 8.0)
    val flyout = WFlyout(flyoutContent)

    flyoutContent.add(WLabel("Delete all items?"))
    flyoutContent.add(
        WButton("Yes, delete all").also { button ->
            button.addActionListener { flyout.hide() }
        },
    )

    val flyoutButton = WButton("Show options")
    flyoutButton.flyout = flyout
    return buildExample("Button with a flyout", flyoutButton)
}

/** A button with a WCommand: running the command and auto-disabling via isEnabled. */
private fun buildCommandButtonExample(): WComponent {
    val result = WLabel("Command has not run yet")
    val command = WCommand { parameter ->
        result.text = "Command ran (parameter = $parameter)"
    }

    val commandButton = WButton("Run command")
    commandButton.command = command
    commandButton.commandParameter = "Gallery"

    val toggleButton = WButton("Disable command")
    toggleButton.addActionListener {
        command.isEnabled = !command.isEnabled
        toggleButton.text = if (command.isEnabled) "Disable command" else "Enable command"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(commandButton)
    row.add(toggleButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Button with a command", row)
}

/** The UI-thread coroutine scope used across the gallery (launches on Dispatchers.WinUi). */
private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.WinUi)

/** Coroutine integration: launch / delay / withContext / cancellation on Dispatchers.WinUi. */
private fun buildCoroutineButtonExample(): WComponent {
    val result = WLabel("Not run yet")
    var job: Job? = null

    val startButton = WButton("Start a 3-second task")
    val cancelButton = WButton("Cancel")
    cancelButton.isEnabled = false

    startButton.addActionListener {
        startButton.isEnabled = false
        cancelButton.isEnabled = true
        job = uiScope.launch {
            try {
                // delay doesn't block the UI thread; it waits via a DispatcherQueueTimer
                for (remaining in 3 downTo 1) {
                    result.text = "Working... $remaining seconds left"
                    delay(1_000)
                }
                // Move the heavy computation to a worker thread, only bringing the result back to the UI thread
                val sum = withContext(Dispatchers.Default) {
                    (1L..1_000_000_000L).sum()
                }
                result.text = "Done (sum of 1 to 1 billion = $sum)"
            } catch (e: CancellationException) {
                result.text = "Cancelled"
                throw e
            } finally {
                startButton.isEnabled = true
                cancelButton.isEnabled = false
            }
        }
    }
    cancelButton.addActionListener { job?.cancel() }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(startButton)
    row.add(cancelButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Coroutine integration (Dispatchers.WinUi / delay / withContext / cancel)", row)
}

/** The ListBox page: lines up demos for trying out WListBox's various features. */
private fun buildListBoxPage(): WComponent {
    val page = buildPage("ListBox", "A control for selecting an item from an always-visible list. Try out WListBox's various features.")

    page.add(buildListBoxColorExample())
    page.add(buildListBoxFontExample())
    page.add(buildListBoxSelectionModeExample())
    return page
}

/** Color selection: mirrors example 1 from the official Gallery (inline items + SelectionChanged changes a rectangle's color). */
private fun buildListBoxColorExample(): WComponent {
    // Same color names as the official Gallery. Repaints the rectangle below based on the selection
    val colors = linkedMapOf(
        "Blue" to WColor(0, 0, 255),
        "Green" to WColor(0, 128, 0),
        "Red" to WColor(255, 0, 0),
        "Yellow" to WColor(255, 255, 0),
    )

    // A fixed-width child gets centered inside a vertical WPanel, so align it left explicitly
    val output = WBorder()
    output.width = 100.0
    output.height = 30.0
    output.horizontalAlignment = HorizontalAlignment.LEFT

    val listBox = WListBox(colors.keys.toList())
    listBox.width = 200.0
    listBox.horizontalAlignment = HorizontalAlignment.LEFT
    listBox.addListSelectionListener {
        output.background = colors[listBox.selectedItem]
    }

    val body = WPanel(spacing = 10.0)
    body.add(listBox)
    body.add(output)
    return buildExample("A list box with inline items (SelectionChanged)", body)
}

/** Font selection: mirrors example 2 from the official Gallery (fixed height + initial selection + selection changes the font). */
private fun buildListBoxFontExample(): WComponent {
    val fonts = listOf("Arial", "Comic Sans MS", "Courier New", "Segoe UI", "Times New Roman")

    val output = WLabel("You can set the font used for this text.")
    output.foreground = TEXT_SECONDARY

    val listBox = WListBox(fonts)
    listBox.width = 200.0
    listBox.height = 164.0
    listBox.horizontalAlignment = HorizontalAlignment.LEFT
    listBox.addListSelectionListener {
        listBox.selectedItem?.let { output.fontFamily = it }
    }
    listBox.selectedIndex = 2 // Selects Courier New initially, same as the official Gallery

    val body = WPanel(spacing = 10.0)
    body.add(listBox)
    body.add(output)
    return buildExample("A list box with a fixed height (SelectedIndex / FontFamily)", body)
}

/** Selection mode: switching selectionMode plus selectAll / selectedItems / scrollIntoView. */
private fun buildListBoxSelectionModeExample(): WComponent {
    val listBox = WListBox((1..20).map { "Option $it" })
    listBox.width = 240.0
    listBox.height = 200.0
    listBox.horizontalAlignment = HorizontalAlignment.LEFT

    val selection = WLabel("Selection: none")
    listBox.addListSelectionListener {
        val items = listBox.selectedItems
        selection.text = if (items.isEmpty()) "Selection: none" else "Selection: ${items.joinToString(", ")}"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (selectionMode in SelectionMode.entries) {
        buttons.add(
            WButton(selectionMode.name).also { button ->
                button.addActionListener { listBox.selectionMode = selectionMode }
            },
        )
    }
    buttons.add(
        WButton("Select all").also { button ->
            button.addActionListener { listBox.selectAll() }
        },
    )
    buttons.add(
        WButton("Scroll to end").also { button ->
            button.addActionListener { listBox.scrollIntoView(listBox.itemCount - 1) }
        },
    )

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(listBox)
    body.add(selection)
    return buildExample("Selection mode (SelectionMode / SelectAll / SelectedItems / ScrollIntoView)", body)
}

/** The ListView page: lines up demos for trying out WList's various features. */
private fun buildListViewPage(): WComponent {
    val page = buildPage("ListView", "A list that lines items up vertically for selection. Try out WList's various features.")

    page.add(buildSimpleListExample())
    page.add(buildListItemOperationsExample())
    page.add(buildListSelectionModeExample())
    page.add(buildListItemClickExample())
    return page
}

/** A basic list: responding to selection changes (SelectionChanged). */
private fun buildSimpleListExample(): WComponent {
    val result = WLabel("Selected: none")

    val list = WList(listOf("Apple", "Orange", "Grape", "Peach", "Cherry"))
    list.width = 240.0
    list.addListSelectionListener {
        val item = list.selectedItem
        result.text = if (item == null) "Selected: none" else "Selected: $item (index = ${list.selectedIndex})"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(list)
    row.add(result)
    return buildExample("A simple list", row)
}

/** Adding and removing items: addItem / removeItem / removeAllItems / itemCount. */
private fun buildListItemOperationsExample(): WComponent {
    // A fixed-width child gets centered inside a vertical WPanel, so align it left explicitly
    val list = WList(listOf("Item 1", "Item 2", "Item 3"))
    list.width = 240.0
    list.horizontalAlignment = HorizontalAlignment.LEFT

    val count = WLabel("Item count: ${list.itemCount}")
    val input = WTextField("Item name to add").also { it.width = 200.0 }

    val addButton = WButton("Add")
    addButton.addActionListener {
        if (input.text.isNotEmpty()) {
            list.addItem(input.text)
            input.text = ""
            count.text = "Item count: ${list.itemCount}"
        }
    }

    val removeButton = WButton("Remove selected")
    removeButton.addActionListener {
        val index = list.selectedIndex
        if (index >= 0) {
            list.removeItem(index)
            count.text = "Item count: ${list.itemCount}"
        }
    }

    val clearButton = WButton("Remove all")
    clearButton.addActionListener {
        list.removeAllItems()
        count.text = "Item count: ${list.itemCount}"
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(input)
    buttons.add(addButton)
    buttons.add(removeButton)
    buttons.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(list)
    body.add(count)
    return buildExample("Adding and removing items", body)
}

/** Selection mode: switching selectionMode and selectAll. */
private fun buildListSelectionModeExample(): WComponent {
    val list = WList((1..5).map { "Option $it" })
    list.width = 240.0
    list.horizontalAlignment = HorizontalAlignment.LEFT

    val mode = WLabel("Selection mode: ${list.selectionMode}")

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (selectionMode in ListViewSelectionMode.entries) {
        buttons.add(
            WButton(selectionMode.name).also { button ->
                button.addActionListener {
                    list.selectionMode = selectionMode
                    mode.text = "Selection mode: ${list.selectionMode}"
                }
            },
        )
    }
    buttons.add(
        WButton("Select all").also { button ->
            button.addActionListener { list.selectAll() }
        },
    )

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(list)
    body.add(mode)
    return buildExample("Selection mode (SelectionMode / SelectAll)", body)
}

/** The TableView page: lines up demos for trying out WTable's various features. */
private fun buildTableViewPage(): WComponent {
    val page = buildPage(
        "TableView",
        "A table that displays data in rows and columns. Try out WTable's various " +
            "features, implemented on top of ListView based on the design of WinUI.TableView.",
    )

    page.add(buildSimpleTableExample())
    page.add(buildTableSortExample())
    page.add(buildTableRowOperationsExample())
    return page
}

/** Sample data for the TableView demos (product name, price, quantity). */
private fun buildProductTable(): WTable {
    val table = WTable(
        listOf(
            WTableColumn("Product", width = 160.0),
            WTableColumn("Price", width = 100.0),
            WTableColumn("Quantity", width = 100.0),
        ),
    )
    table.addRow("Apple", "150", "12")
    table.addRow("Orange", "80", "30")
    table.addRow("Grape", "480", "5")
    table.addRow("Peach", "320", "8")
    table.addRow("Cherry", "600", "3")
    table.width = 400.0
    return table
}

/** A basic table: responding to row selection (SelectionChanged). */
private fun buildSimpleTableExample(): WComponent {
    val result = WLabel("Selection: none")

    val table = buildProductTable()
    table.addRowSelectionListener {
        val row = table.selectedRow
        result.text = if (row < 0) {
            "Selection: none"
        } else {
            "Selection: ${table.getValueAt(row, 0)} (row = $row)"
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(table)
    body.add(result)
    return buildExample("A basic table (row selection)", body)
}

/** Sorting columns: cycling through header clicks (ascending -> descending -> cleared) and sortBy / clearSort. */
private fun buildTableSortExample(): WComponent {
    val table = buildProductTable()

    val sortByPriceButton = WButton("Sort by price descending")
    sortByPriceButton.addActionListener {
        table.sortBy(1, SortDirection.DESCENDING)
    }

    val clearButton = WButton("Clear sort")
    clearButton.addActionListener {
        table.clearSort()
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(sortByPriceButton)
    buttons.add(clearButton)

    val note = WLabel("Clicking a column header also cycles through ascending -> descending -> cleared.").also {
        it.foreground = TEXT_SECONDARY
        it.textWrapping = TextWrapping.WRAP
    }

    val body = WPanel(spacing = 8.0)
    body.add(note)
    body.add(buttons)
    body.add(table)
    return buildExample("Sorting columns (header click / SortBy / ClearSort)", body)
}

/** Adding and removing rows: addRow / removeRow / removeAllRows / setValueAt / rowCount. */
private fun buildTableRowOperationsExample(): WComponent {
    val table = buildProductTable()

    val count = WLabel("Row count: ${table.rowCount}")
    val updateCount = { count.text = "Row count: ${table.rowCount}" }

    var nextItemNumber = 1
    val addButton = WButton("Add row")
    addButton.addActionListener {
        table.addRow("New item $nextItemNumber", "${nextItemNumber * 100}", "1")
        nextItemNumber++
        updateCount()
    }

    val removeButton = WButton("Remove selected row")
    removeButton.addActionListener {
        val row = table.selectedRow
        if (row >= 0) {
            table.removeRow(row)
            updateCount()
        }
    }

    val incrementButton = WButton("Selected quantity +1")
    incrementButton.addActionListener {
        val row = table.selectedRow
        if (row >= 0) {
            val quantity = table.getValueAt(row, 2).toIntOrNull() ?: 0
            table.setValueAt(row, 2, "${quantity + 1}")
        }
    }

    val clearButton = WButton("Remove all")
    clearButton.addActionListener {
        table.removeAllRows()
        updateCount()
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(addButton)
    buttons.add(removeButton)
    buttons.add(incrementButton)
    buttons.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(table)
    body.add(count)
    return buildExample("Adding and removing rows (AddRow / RemoveRow / SetValueAt)", body)
}

/** TreeView page: lines up demos for trying out WTree's various features. */
private fun buildTreeViewPage(): WComponent {
    val page = buildPage("TreeView", "A tree that can expand and collapse hierarchical data. Try out WTree's various features.")

    page.add(buildSimpleTreeExample())
    page.add(buildTreeMultiSelectExample())
    page.add(buildTreeExpandCollapseExample())
    return page
}

/** Builds the same sample tree (Work Documents / Personal Documents) as the real Gallery. */
private fun buildSampleTree(): WTree {
    val tree = WTree()
    tree.width = 345.0
    // Pin it to the left so the tree doesn't shift toward the center if the panel widens (e.g. from a long label)
    tree.horizontalAlignment = HorizontalAlignment.LEFT

    val workFolder = WTreeNode("Work Documents")
    workFolder.add(WTreeNode("XYZ Functional Spec"))
    workFolder.add(WTreeNode("Feature Schedule"))
    workFolder.isExpanded = true

    val remodelFolder = WTreeNode("Home Remodel")
    remodelFolder.add(WTreeNode("Contractor Contact Info"))
    remodelFolder.add(WTreeNode("Paint Color Scheme"))
    remodelFolder.isExpanded = true

    val personalFolder = WTreeNode("Personal Documents")
    personalFolder.add(remodelFolder)
    personalFolder.isExpanded = true

    tree.addRootNode(workFolder)
    tree.addRootNode(personalFolder)
    return tree
}

/** Basic tree: drag-to-reorder and responding to node clicks (ItemInvoked). */
private fun buildSimpleTreeExample(): WComponent {
    val result = WLabel("Click: none")
    result.textWrapping = TextWrapping.WRAP

    val tree = buildSampleTree()
    tree.canDragItems = true
    tree.canReorderItems = true
    tree.addItemInvokedListener { node ->
        result.text = if (node == null) "Click: none" else "Click: ${node.text} (depth = ${node.depth})"
    }

    val body = WPanel(spacing = 8.0)
    body.add(tree)
    body.add(result)
    return buildExample("Simple tree (drag & drop reordering / ItemInvoked)", body)
}

/** Multiple selection: checkboxes from SelectionMode = MULTIPLE, plus SelectAll / SelectedNodes. */
private fun buildTreeMultiSelectExample(): WComponent {
    val tree = buildSampleTree()
    tree.selectionMode = TreeViewSelectionMode.MULTIPLE

    val result = WLabel("Selected: none")
    result.textWrapping = TextWrapping.WRAP
    val showButton = WButton("Show selection")
    showButton.addActionListener {
        val names = tree.selectedNodes.joinToString(", ") { it.text }
        result.text = if (names.isEmpty()) "Selected: none" else "Selected: $names"
    }
    val selectAllButton = WButton("Select all")
    selectAllButton.addActionListener { tree.selectAll() }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(showButton)
    buttons.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(tree)
    body.add(buttons)
    body.add(result)
    return buildExample("Multiple selection (SelectionMode / SelectAll / SelectedNodes)", body)
}

/** Expand and collapse: the Expand / Collapse methods and the Expanding / Collapsed events. */
private fun buildTreeExpandCollapseExample(): WComponent {
    val log = WLabel("Event: none")
    log.textWrapping = TextWrapping.WRAP

    val tree = buildSampleTree()
    tree.addExpandingListener { node -> log.text = "Event: Expanding (${node?.text})" }
    tree.addCollapsedListener { node -> log.text = "Event: Collapsed (${node?.text})" }

    val expandButton = WButton("Expand all")
    expandButton.addActionListener {
        for (root in tree.rootNodes) tree.expand(root)
    }
    val collapseButton = WButton("Collapse all")
    collapseButton.addActionListener {
        for (root in tree.rootNodes) tree.collapse(root)
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(expandButton)
    buttons.add(collapseButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(tree)
    body.add(log)
    return buildExample("Expand and collapse (Expand / Collapse / Expanding / Collapsed)", body)
}

/** A colored tile for the layout demos (a Border painted with a background). Fills its parent if a size isn't given. */
private fun buildTile(color: WColor, width: Double = Double.NaN, height: Double = Double.NaN, label: String = ""): WBorder {
    val tile = WBorder()
    tile.background = color
    tile.cornerRadius = 4.0
    if (!width.isNaN()) tile.width = width
    if (!height.isNaN()) tile.height = height
    if (label.isNotEmpty()) {
        tile.padding = 8.0
        tile.child = WLabel(label)
    }
    return tile
}

/** The Border page: lines up demos for trying out WBorder's various features. */
private fun buildBorderPage(): WComponent {
    val page = buildPage("Border", "A container that draws a border, background, and rounded corners around a single child. Try out WBorder's various features.")

    page.add(buildBorderStyleExample())
    page.add(buildBorderBackgroundExample())
    page.add(buildBorderGradientExample())
    return page
}

/** Switching the border thickness and corner rounding with buttons. */
private fun buildBorderStyleExample(): WComponent {
    val border = WBorder(WLabel("Content with a border"))
    border.borderColor = WColor.BLUE
    border.borderThickness = 2.0
    border.padding = 16.0

    val thicknessButton = WButton("Increase border thickness")
    thicknessButton.addActionListener {
        border.borderThickness = if (border.borderThickness >= 8.0) 2.0 else border.borderThickness + 2.0
    }

    val cornerButton = WButton("Toggle rounded corners")
    cornerButton.addActionListener {
        border.cornerRadius = if (border.cornerRadius > 0) 0.0 else 12.0
    }

    val colorButton = WButton("Change border color")
    var colorIndex = 0
    val colors = listOf(WColor.BLUE, WColor.RED, WColor.GREEN, WColor.ORANGE)
    colorButton.addActionListener {
        colorIndex = (colorIndex + 1) % colors.size
        border.borderColor = colors[colorIndex]
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(thicknessButton)
    buttons.add(cornerButton)
    buttons.add(colorButton)

    val body = WPanel(spacing = 8.0)
    body.add(border)
    body.add(buttons)
    return buildExample("Border and corner rounding (BorderBrush / BorderThickness / CornerRadius)", body)
}

/** Background color and padding. */
private fun buildBorderBackgroundExample(): WComponent {
    val border = WBorder(WLabel("Content with a background"))
    border.background = WColor.LIGHT_GRAY
    border.cornerRadius = 8.0
    border.padding = 16.0

    val paddingButton = WButton("Increase padding")
    paddingButton.addActionListener {
        border.padding = if (border.padding >= 48.0) 16.0 else border.padding + 16.0
    }

    val body = WPanel(spacing = 8.0)
    body.add(border)
    body.add(paddingButton)
    return buildExample("Background and padding (Background / Padding)", body)
}

/** Switching between a gradient background and its angle. */
private fun buildBorderGradientExample(): WComponent {
    val stops = listOf(0.0 to WColor.BLUE, 1.0 to WColor.PURPLE)

    val border = WBorder(WLabel("Content with a gradient background").also { it.foreground = WColor.WHITE })
    border.backgroundGradient = WLinearGradientPaint(stops)
    border.cornerRadius = 8.0
    border.padding = 16.0
    border.width = 320.0
    border.height = 80.0
    border.horizontalAlignment = HorizontalAlignment.LEFT

    var angle = 90.0
    val angleButton = WButton("Change angle (90°)")
    angleButton.addActionListener {
        angle = if (angle >= 270.0) 0.0 else angle + 90.0
        border.backgroundGradient = WLinearGradientPaint(stops, angle = angle)
        angleButton.text = "Change angle (${angle.toInt()}°)"
    }

    val body = WPanel(spacing = 8.0)
    body.add(border)
    body.add(angleButton)
    return buildExample("Gradient background (LinearGradientBrush)", body)
}

/** The Canvas page: lines up demos for trying out WCanvas's various features. */
private fun buildCanvasPage(): WComponent {
    val page = buildPage("Canvas", "A panel that positions children with absolute coordinates. Try out WCanvas's various features.")

    page.add(buildCanvasPositionExample())
    page.add(buildCanvasZIndexExample())
    return page
}

/** Positioning: moving a child with SetLeft / SetTop. */
private fun buildCanvasPositionExample(): WComponent {
    val canvas = WCanvas()
    canvas.width = 320.0
    canvas.height = 160.0

    canvas.add(buildTile(WColor.LIGHT_GRAY, 320.0, 160.0), 0.0, 0.0) // background (visualizes the canvas bounds)
    val movingTile = buildTile(WColor.BLUE, 48.0, 48.0)
    var x = 16.0
    var y = 16.0
    canvas.add(movingTile, x, y)

    val moveButton = WButton("Move the tile")
    moveButton.addActionListener {
        x = if (x >= 256.0) 16.0 else x + 48.0
        y = if (y >= 96.0) 16.0 else y + 16.0
        canvas.setLocation(movingTile, x, y)
    }

    val body = WPanel(spacing = 8.0)
    body.add(canvas)
    body.add(moveButton)
    return buildExample("Absolute positioning (Canvas.Left / Canvas.Top)", body)
}

/** Stacking order: swapping front/back with SetZIndex. */
private fun buildCanvasZIndexExample(): WComponent {
    val canvas = WCanvas()
    canvas.width = 320.0
    canvas.height = 120.0

    val redTile = buildTile(WColor.RED, 64.0, 64.0)
    val greenTile = buildTile(WColor.GREEN, 64.0, 64.0)
    canvas.add(redTile, 16.0, 16.0)
    canvas.add(greenTile, 48.0, 40.0)

    var redOnTop = false
    val swapButton = WButton("Swap stacking order")
    swapButton.addActionListener {
        redOnTop = !redOnTop
        canvas.setZIndex(redTile, if (redOnTop) 1 else 0)
        canvas.setZIndex(greenTile, if (redOnTop) 0 else 1)
    }

    val body = WPanel(spacing = 8.0)
    body.add(canvas)
    body.add(swapButton)
    return buildExample("Stacking order (Canvas.ZIndex)", body)
}

/** The Expander page: lines up demos for trying out WExpander's various features. */
private fun buildExpanderPage(): WComponent {
    val page = buildPage("Expander", "A control that expands/collapses its content when the header is clicked. Try out WExpander's various features.")

    page.add(buildExpanderBasicExample())
    page.add(buildExpanderDirectionExample())
    return page
}

/** Basic expand/collapse plus the Expanding / Collapsed events. */
private fun buildExpanderBasicExample(): WComponent {
    val state = WLabel("State: collapsed")

    val content = WPanel(spacing = 8.0)
    content.add(WLabel("This is the expanded content."))
    content.add(WButton("A button inside"))

    val expander = WExpander("Click to expand or collapse", content)
    expander.width = 320.0
    expander.addExpandListener { state.text = "State: expanded" }
    expander.addCollapseListener { state.text = "State: collapsed" }

    val toggleButton = WButton("Toggle from code")
    toggleButton.addActionListener { expander.isExpanded = !expander.isExpanded }

    val body = WPanel(spacing = 8.0)
    body.add(expander)
    body.add(toggleButton)
    body.add(state)
    return buildExample("Expand/collapse and events (IsExpanded / Expanding / Collapsed)", body)
}

/** The expand direction (ExpandDirection). */
private fun buildExpanderDirectionExample(): WComponent {
    val expander = WExpander("A header whose expand direction can change", WLabel("Can expand either up or down."))
    expander.width = 320.0

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for (direction in ExpandDirection.entries) {
        buttons.add(
            WButton(direction.name).also { button ->
                button.addActionListener { expander.expandDirection = direction }
            },
        )
    }

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(expander)
    return buildExample("Expand direction (ExpandDirection)", body)
}

/** The Grid page: lines up demos for trying out WGrid's various features. */
private fun buildGridPage(): WComponent {
    val page = buildPage("Grid", "A panel that defines rows and columns and places children into cells. Try out WGrid's various features.")

    page.add(buildGridCellExample())
    page.add(buildGridSpanExample())
    return page
}

/** Row/column definitions and cell placement: combining Auto / Pixel / Star. */
private fun buildGridCellExample(): WComponent {
    val grid = WGrid(rowSpacing = 4.0, columnSpacing = 4.0)
    grid.width = 320.0
    grid.addRow(GridLength.pixel(48.0))
    grid.addRow(GridLength.pixel(48.0))
    grid.addColumn(GridLength.pixel(80.0))
    grid.addColumn(GridLength.star())
    grid.addColumn(GridLength.star(2.0))

    grid.add(buildTile(WColor.RED, label = "80px"), row = 0, column = 0)
    grid.add(buildTile(WColor.GREEN, label = "1*"), row = 0, column = 1)
    grid.add(buildTile(WColor.BLUE, label = "2*"), row = 0, column = 2)
    grid.add(buildTile(WColor.ORANGE, label = "Row 2"), row = 1, column = 0)
    grid.add(buildTile(WColor.PURPLE, label = "Row 2"), row = 1, column = 2)

    return buildExample("Row/column definitions and cell placement (RowDefinitions / ColumnDefinitions)", grid)
}

/** RowSpan / ColumnSpan and cell spacing. */
private fun buildGridSpanExample(): WComponent {
    val grid = WGrid(rowSpacing = 4.0, columnSpacing = 4.0)
    grid.width = 320.0
    repeat(2) { grid.addRow(GridLength.pixel(48.0)) }
    repeat(3) { grid.addColumn(GridLength.star()) }

    grid.add(buildTile(WColor.BLUE, label = "2 columns"), row = 0, column = 0, columnSpan = 2)
    grid.add(buildTile(WColor.GREEN, label = "2 rows"), row = 0, column = 2, rowSpan = 2)
    grid.add(buildTile(WColor.RED, label = "1 cell"), row = 1, column = 0)
    grid.add(buildTile(WColor.ORANGE, label = "1 cell"), row = 1, column = 1)

    val spacingButton = WButton("Toggle cell spacing")
    spacingButton.addActionListener {
        val next = if (grid.rowSpacing > 4.0) 4.0 else 16.0
        grid.rowSpacing = next
        grid.columnSpacing = next
    }

    val body = WPanel(spacing = 8.0)
    body.add(grid)
    body.add(spacingButton)
    return buildExample("Cell spans and spacing (RowSpan / ColumnSpan / Spacing)", body)
}

/** The RelativePanel page: lines up demos for trying out WRelativePanel's various features. */
private fun buildRelativePanelPage(): WComponent {
    val page = buildPage("RelativePanel", "A panel that positions children relative to each other or to the panel. Try out WRelativePanel's various features.")

    page.add(buildRelativePanelSiblingExample())
    page.add(buildRelativePanelAlignExample())
    return page
}

/** Placement relative to sibling elements (RightOf / Below). */
private fun buildRelativePanelSiblingExample(): WComponent {
    val panel = WRelativePanel()
    panel.width = 320.0
    panel.height = 160.0

    val anchor = buildTile(WColor.BLUE, label = "Anchor")
    val right = buildTile(WColor.GREEN, label = "Right")
    val below = buildTile(WColor.RED, label = "Below")
    val rightBelow = buildTile(WColor.ORANGE, label = "Right+Below")

    panel.add(anchor)
    panel.add(right)
    panel.add(below)
    panel.add(rightBelow)

    panel.placeRightOf(right, anchor)
    panel.placeBelow(below, anchor)
    panel.placeRightOf(rightBelow, below)
    panel.placeBelow(rightBelow, right)

    return buildExample("Placement relative to siblings (RightOf / Below)", panel)
}

/** Placement relative to the panel (AlignXxxWithPanel). */
private fun buildRelativePanelAlignExample(): WComponent {
    val panel = WRelativePanel()
    panel.width = 320.0
    panel.height = 160.0

    panel.add(buildTile(WColor.LIGHT_GRAY, 320.0, 160.0)) // background (visualizes the panel bounds)

    val topRight = buildTile(WColor.GREEN, 48.0, 48.0)
    panel.add(topRight)
    panel.alignRightWithPanel(topRight)
    panel.alignTopWithPanel(topRight)

    val bottomRight = buildTile(WColor.RED, 48.0, 48.0)
    panel.add(bottomRight)
    panel.alignRightWithPanel(bottomRight)
    panel.alignBottomWithPanel(bottomRight)

    val center = buildTile(WColor.PURPLE, 48.0, 48.0)
    panel.add(center)
    panel.alignHorizontalCenterWithPanel(center)
    panel.alignVerticalCenterWithPanel(center)

    return buildExample("Placement relative to the panel (AlignXxxWithPanel)", panel)
}

/** The SplitView page: lines up demos for trying out WSplitView's various features. */
private fun buildSplitViewPage(): WComponent {
    val page = buildPage("SplitView", "A control that lines up a collapsible pane alongside content. Try out WSplitView's various features.")

    page.add(buildSplitViewExample())
    return page
}

/** Opening/closing the pane, display mode, and placement. */
private fun buildSplitViewExample(): WComponent {
    val paneContent = WPanel(spacing = 8.0)
    paneContent.add(WLabel("Pane"))
    paneContent.add(WButton("Menu 1"))
    paneContent.add(WButton("Menu 2"))

    val pane = WBorder(paneContent)
    pane.background = WColor.LIGHT_GRAY
    pane.padding = 8.0

    val mainContent = WBorder(WLabel("Main content"))
    mainContent.padding = 16.0

    val splitView = WSplitView(pane = pane, content = mainContent)
    splitView.width = 480.0
    splitView.height = 200.0
    splitView.openPaneLength = 160.0
    splitView.displayMode = SplitViewDisplayMode.INLINE

    val toggleButton = WButton("Toggle pane")
    toggleButton.addActionListener { splitView.isPaneOpen = !splitView.isPaneOpen }

    val modeButtons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    modeButtons.add(WLabel("DisplayMode:"))
    for (mode in SplitViewDisplayMode.entries) {
        modeButtons.add(
            WButton(mode.name).also { button ->
                button.addActionListener { splitView.displayMode = mode }
            },
        )
    }

    val placementButton = WButton("Flip pane placement")
    placementButton.addActionListener {
        splitView.panePlacement = when (splitView.panePlacement) {
            SplitViewPanePlacement.LEFT -> SplitViewPanePlacement.RIGHT
            SplitViewPanePlacement.RIGHT -> SplitViewPanePlacement.LEFT
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(splitView)
    body.add(toggleButton)
    body.add(modeButtons)
    body.add(placementButton)
    return buildExample("Opening/closing the pane (IsPaneOpen / DisplayMode / PanePlacement)", body)
}

/** The StackPanel page: lines up demos for trying out WPanel's various features. */
private fun buildStackPanelPage(): WComponent {
    val page = buildPage("StackPanel", "A panel that lines up children in one direction. Try out WPanel's various features.")

    page.add(buildStackPanelExample())
    return page
}

/** The direction children line up in, and the spacing between them. */
private fun buildStackPanelExample(): WComponent {
    val panel = WPanel(spacing = 8.0)
    panel.add(buildTile(WColor.RED, 48.0, 48.0))
    panel.add(buildTile(WColor.GREEN, 48.0, 48.0))
    panel.add(buildTile(WColor.BLUE, 48.0, 48.0))

    val orientationButton = WButton("Toggle direction")
    orientationButton.addActionListener {
        panel.orientation = when (panel.orientation) {
            Orientation.VERTICAL -> Orientation.HORIZONTAL
            Orientation.HORIZONTAL -> Orientation.VERTICAL
        }
    }

    val spacingButton = WButton("Toggle spacing")
    spacingButton.addActionListener {
        panel.spacing = if (panel.spacing > 8.0) 8.0 else 24.0
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(orientationButton)
    buttons.add(spacingButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(panel)
    return buildExample("Direction and spacing (Orientation / Spacing)", body)
}

/** The VariableSizedWrapGrid page: lines up demos for trying out WVariableSizedWrapGrid's various features. */
private fun buildVariableSizedWrapGridPage(): WComponent {
    val page = buildPage("VariableSizedWrapGrid", "A panel that wraps children by cell. Try out WVariableSizedWrapGrid's various features.")

    page.add(buildWrapGridSpanExample())
    return page
}

/** Cell spans and wrapping. */
private fun buildWrapGridSpanExample(): WComponent {
    val grid = WVariableSizedWrapGrid(itemWidth = 56.0, itemHeight = 56.0)
    grid.orientation = Orientation.HORIZONTAL
    grid.maximumRowsOrColumns = 4

    grid.add(buildTile(WColor.BLUE, label = "2×2"), rowSpan = 2, columnSpan = 2)
    grid.add(buildTile(WColor.RED, label = "1×1"), rowSpan = 1, columnSpan = 1)
    grid.add(buildTile(WColor.GREEN, label = "1×2"), rowSpan = 1, columnSpan = 2)
    grid.add(buildTile(WColor.ORANGE, label = "1×1"), rowSpan = 1, columnSpan = 1)
    grid.add(buildTile(WColor.PURPLE, label = "2×1"), rowSpan = 2, columnSpan = 1)
    grid.add(buildTile(WColor.GRAY, label = "1×1"), rowSpan = 1, columnSpan = 1)

    val maxButton = WButton("Toggle wrap count")
    maxButton.addActionListener {
        grid.maximumRowsOrColumns = if (grid.maximumRowsOrColumns == 4) 6 else 4
    }

    val orientationButton = WButton("Toggle direction")
    orientationButton.addActionListener {
        grid.orientation = when (grid.orientation) {
            Orientation.VERTICAL -> Orientation.HORIZONTAL
            Orientation.HORIZONTAL -> Orientation.VERTICAL
        }
    }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(maxButton)
    buttons.add(orientationButton)

    val body = WPanel(spacing = 8.0)
    body.add(buttons)
    body.add(grid)
    return buildExample("Cell spans and wrapping (RowSpan / ColumnSpan / MaximumRowsOrColumns)", body)
}

/** ItemClick: enabling isItemClickEnabled and receiving the clicked item. */
private fun buildListItemClickExample(): WComponent {
    val result = WLabel("Clicked: none")

    val list = WList(listOf("Document", "Pictures", "Music", "Video"))
    list.width = 240.0
    list.isItemClickEnabled = true
    list.addItemClickListener { item ->
        result.text = "Clicked: $item"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(list)
    row.add(result)
    return buildExample("Item clicks (ItemClick)", row)
}

/** The CheckBox page: lines up demos for trying out WCheckBox's various features. */
private fun buildCheckBoxPage(): WComponent {
    val page = buildPage("CheckBox", "A control for toggling checked/unchecked (and indeterminate). Try out WCheckBox's various features.")

    page.add(buildSimpleCheckBoxExample())
    page.add(buildThreeStateCheckBoxExample())
    page.add(buildSelectAllCheckBoxExample())
    return page
}

/** A basic checkbox: responding to state changes (Checked / Unchecked). */
private fun buildSimpleCheckBoxExample(): WComponent {
    val result = WLabel("State: off")

    val checkBox = WCheckBox("Receive notifications")
    checkBox.addItemListener { checked ->
        result.text = if (checked == true) "State: on" else "State: off"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(checkBox)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple checkbox", row)
}

/** Three states: isThreeState cycles on -> indeterminate -> off. */
private fun buildThreeStateCheckBoxExample(): WComponent {
    val result = WLabel("State: off")

    val checkBox = WCheckBox("Three-state checkbox")
    checkBox.isThreeState = true
    checkBox.addItemListener { checked ->
        result.text = when (checked) {
            true -> "State: on"
            false -> "State: off"
            null -> "State: indeterminate"
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(checkBox)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Three states (IsThreeState)", row)
}

/** Select all: a parent checkbox controls three children, and the children's state makes the parent indeterminate. */
private fun buildSelectAllCheckBoxExample(): WComponent {
    val parent = WCheckBox("Select all options")
    val children = listOf(
        WCheckBox("Option 1"),
        WCheckBox("Option 2"),
        WCheckBox("Option 3"),
    )

    // Guard against an infinite event loop from the parent and children updating each other
    var updating = false
    parent.addItemListener { checked ->
        if (updating || checked == null) return@addItemListener
        updating = true
        for (child in children) child.isChecked = checked
        updating = false
    }
    for (child in children) {
        child.margin = 4.0
        child.addItemListener {
            if (updating) return@addItemListener
            updating = true
            val checkedCount = children.count { it.isChecked == true }
            parent.isChecked = when (checkedCount) {
                0 -> false
                children.size -> true
                else -> null
            }
            updating = false
        }
    }

    val body = WPanel(spacing = 4.0)
    body.add(parent)
    for (child in children) body.add(child)
    return buildExample("Select all (parent/child linkage and indeterminate state)", body)
}

/** The RadioButton page: lines up demos for trying out WRadioButton's various features. */
private fun buildRadioButtonPage(): WComponent {
    val page = buildPage("RadioButton", "A control for picking exactly one option within a group. Try out WRadioButton's various features.")

    page.add(buildSimpleRadioButtonExample())
    page.add(buildRadioButtonGroupExample())
    return page
}

/** A basic radio button: mutually exclusive selection within the same group. */
private fun buildSimpleRadioButtonExample(): WComponent {
    val result = WLabel("Selected: none")

    val body = WPanel(spacing = 4.0)
    for (name in listOf("Small", "Medium", "Large")) {
        body.add(
            WRadioButton(name).also { radioButton ->
                radioButton.groupName = "Size"
                radioButton.addItemListener { checked ->
                    if (checked == true) result.text = "Selected: $name"
                }
            },
        )
    }
    body.add(result)
    return buildExample("A simple radio button (mutually exclusive selection)", body)
}

/** Multiple groups: separate groupName values let groups be selected independently. */
private fun buildRadioButtonGroupExample(): WComponent {
    val result = WLabel("Background: unselected / Foreground: unselected")
    var background = "unselected"
    var foreground = "unselected"

    fun buildGroup(title: String, group: String, onSelect: (String) -> Unit): WComponent {
        val panel = WPanel(spacing = 4.0)
        panel.add(WLabel(title))
        for (name in listOf("White", "Black", "Blue")) {
            panel.add(
                WRadioButton(name).also { radioButton ->
                    radioButton.groupName = group
                    radioButton.addItemListener { checked ->
                        if (checked == true) onSelect(name)
                    }
                },
            )
        }
        return panel
    }

    val row = WPanel(spacing = 32.0, orientation = Orientation.HORIZONTAL)
    row.add(buildGroup("Background", "background") { background = it; result.text = "Background: $background / Foreground: $foreground" })
    row.add(buildGroup("Foreground", "foreground") { foreground = it; result.text = "Background: $background / Foreground: $foreground" })

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample("Multiple groups (GroupName)", body)
}

/** The ToggleButton page: lines up demos for trying out WToggleButton's various features. */
private fun buildToggleButtonPage(): WComponent {
    val page = buildPage("ToggleButton", "A button that toggles on/off each time it's pressed. Try out WToggleButton's various features.")

    page.add(buildSimpleToggleButtonExample())
    return page
}

/** A basic toggle button: displaying isChecked and toggling it from code. */
private fun buildSimpleToggleButtonExample(): WComponent {
    val result = WLabel("State: off")

    val toggleButton = WToggleButton("Mute")
    toggleButton.addItemListener { checked ->
        result.text = if (checked == true) "State: on" else "State: off"
    }

    val codeButton = WButton("Toggle from code")
    codeButton.addActionListener {
        toggleButton.isChecked = toggleButton.isChecked != true
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleButton)
    row.add(codeButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple toggle button (IsChecked)", row)
}

/** The RepeatButton page: lines up demos for trying out WRepeatButton's various features. */
private fun buildRepeatButtonPage(): WComponent {
    val page = buildPage("RepeatButton", "A button that fires Click repeatedly while held down. Try out WRepeatButton's various features.")

    page.add(buildSimpleRepeatButtonExample())
    page.add(buildRepeatButtonSpeedExample())
    return page
}

/** A basic repeat button: the counter keeps increasing while held down. */
private fun buildSimpleRepeatButtonExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val repeatButton = WRepeatButton("Press and hold")
    repeatButton.addActionListener {
        count++
        result.text = "Click count: $count"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(repeatButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple repeat button", row)
}

/** Repeat speed: the difference between delay (wait before the first fire) and interval (time between fires). */
private fun buildRepeatButtonSpeedExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    for ((label, delay, interval) in listOf(
        Triple("Slow (500ms interval)", 500, 500),
        Triple("Fast (50ms interval)", 250, 50),
    )) {
        row.add(
            WRepeatButton(label).also { repeatButton ->
                repeatButton.delay = delay
                repeatButton.interval = interval
                repeatButton.addActionListener {
                    count++
                    result.text = "Click count: $count"
                }
            },
        )
    }
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Repeat speed (Delay / Interval)", row)
}

/** The HyperlinkButton page: lines up demos for trying out WHyperlinkButton's various features. */
private fun buildHyperlinkButtonPage(): WComponent {
    val page = buildPage("HyperlinkButton", "A button displayed as a hyperlink. Try out WHyperlinkButton's various features.")

    page.add(buildNavigateUriHyperlinkExample())
    page.add(buildClickHyperlinkExample())
    return page
}

/** NavigateUri: clicking opens the default browser. */
private fun buildNavigateUriHyperlinkExample(): WComponent {
    val hyperlinkButton = WHyperlinkButton(
        text = "Open the WinUI 3 documentation",
        navigateUri = "https://learn.microsoft.com/windows/apps/winui/winui3/",
    )
    return buildExample("Navigating to a URI (NavigateUri)", hyperlinkButton)
}

/** Handling Click: respond to clicks in code without setting NavigateUri. */
private fun buildClickHyperlinkExample(): WComponent {
    val result = WLabel("Click count: 0")
    var count = 0

    val hyperlinkButton = WHyperlinkButton("A link whose click is handled in code")
    hyperlinkButton.addActionListener {
        count++
        result.text = "Click count: $count"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(hyperlinkButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Handling the Click event", row)
}

/** The DropDownButton page: lines up demos for trying out WDropDownButton's various features. */
private fun buildDropDownButtonPage(): WComponent {
    val page = buildPage("DropDownButton", "A button that opens a flyout of choices when clicked. Try out WDropDownButton's various features.")

    page.add(buildSimpleDropDownButtonExample())
    return page
}

/** A basic drop-down button: choosing from a flyout menu. */
private fun buildSimpleDropDownButtonExample(): WComponent {
    val result = WLabel("Selected: none")

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    for (name in listOf("Mail", "Calendar", "Contacts")) {
        menu.add(
            WButton(name).also { button ->
                button.width = 120.0
                button.addActionListener {
                    result.text = "Selected: $name"
                    flyout.hide()
                }
            },
        )
    }

    val dropDownButton = WDropDownButton("New")
    dropDownButton.flyout = flyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(dropDownButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple drop-down button (Flyout)", row)
}

/** The SplitButton page: lines up demos for trying out WSplitButton's various features. */
private fun buildSplitButtonPage(): WComponent {
    val page = buildPage("SplitButton", "A two-part button split between clicking the body and expanding choices. Try out WSplitButton's various features.")

    page.add(buildSimpleSplitButtonExample())
    return page
}

/** A basic split button: clicking the body applies the current color, the arrow picks a color. */
private fun buildSimpleSplitButtonExample(): WComponent {
    val tile = buildTile(WColor.LIGHT_GRAY, width = 48.0, height = 48.0)
    var currentColor = WColor.RED

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    val splitButton = WSplitButton("Apply color")
    for ((name, color) in listOf("Red" to WColor.RED, "Green" to WColor.GREEN, "Blue" to WColor.BLUE)) {
        menu.add(
            WButton(name).also { button ->
                button.width = 100.0
                button.addActionListener {
                    currentColor = color
                    tile.background = color
                    splitButton.text = "Apply color ($name)"
                    flyout.hide()
                }
            },
        )
    }
    splitButton.flyout = flyout
    splitButton.addActionListener { tile.background = currentColor }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(splitButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(tile)
    return buildExample("A simple split button (Click + Flyout)", row)
}

/** The ToggleSplitButton page: lines up demos for trying out WToggleSplitButton's various features. */
private fun buildToggleSplitButtonPage(): WComponent {
    val page = buildPage("ToggleSplitButton", "A split button whose body toggles on/off when clicked. Try out WToggleSplitButton's various features.")

    page.add(buildSimpleToggleSplitButtonExample())
    return page
}

/** A basic toggle split button: toggling a bulleted list on/off and choosing its marker. */
private fun buildSimpleToggleSplitButtonExample(): WComponent {
    val result = WLabel("")
    var marker = "•"
    val items = listOf("Apple", "Orange", "Grape")

    val toggleSplitButton = WToggleSplitButton("Bulleted list")
    fun render() {
        val prefix = if (toggleSplitButton.isChecked) marker else ""
        result.text = items.joinToString("\n") { prefix + it }
    }
    render()

    val menu = WPanel(spacing = 4.0)
    val flyout = WFlyout(menu)
    for (name in listOf("•", "-", "◆")) {
        menu.add(
            WButton(name).also { button ->
                button.width = 80.0
                button.addActionListener {
                    marker = name
                    toggleSplitButton.isChecked = true
                    render()
                    flyout.hide()
                }
            },
        )
    }
    toggleSplitButton.flyout = flyout
    toggleSplitButton.addItemListener { render() }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleSplitButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple toggle split button (IsChecked + Flyout)", row)
}

/** The Slider page: lines up demos for trying out WSlider's various features. */
private fun buildSliderPage(): WComponent {
    val page = buildPage("Slider", "A control for picking a value in a range by moving a thumb along a track. Try out WSlider's various features.")

    page.add(buildSimpleSliderExample())
    page.add(buildRangeSliderExample())
    page.add(buildTickSliderExample())
    page.add(buildVerticalSliderExample())
    return page
}

/** A basic slider: responding to value changes (ValueChanged). */
private fun buildSimpleSliderExample(): WComponent {
    val result = WLabel("Value: 0")

    val slider = WSlider()
    slider.width = 300.0
    slider.addChangeListener { value ->
        result.text = "Value: ${value.toInt()}"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(slider)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple slider (ValueChanged)", row)
}

/** Range and step: minimum / maximum / stepFrequency / header. */
private fun buildRangeSliderExample(): WComponent {
    val slider = WSlider(minimum = 500.0, maximum = 1000.0, value = 800.0)
    slider.width = 300.0
    slider.stepFrequency = 10.0
    slider.header = "Range 500-1000, step 10"
    return buildExample("Range and step (Minimum / Maximum / StepFrequency / Header)", slider)
}

/** Tick marks: tickFrequency / tickPlacement and snapsTo. */
private fun buildTickSliderExample(): WComponent {
    val slider = WSlider(maximum = 50.0)
    slider.width = 300.0
    slider.tickFrequency = 10.0
    slider.tickPlacement = TickPlacement.OUTSIDE
    slider.snapsTo = SliderSnapsTo.TICKS
    return buildExample("Tick marks (TickFrequency / TickPlacement / SnapsTo)", slider)
}

/** Vertical: orientation and isDirectionReversed. */
private fun buildVerticalSliderExample(): WComponent {
    val slider = WSlider(value = 30.0)
    slider.height = 160.0
    slider.orientation = Orientation.VERTICAL
    slider.isDirectionReversed = true
    return buildExample("Vertical (Orientation / IsDirectionReversed)", slider)
}

/** The ToggleSwitch page: lines up demos for trying out WToggleSwitch's various features. */
private fun buildToggleSwitchPage(): WComponent {
    val page = buildPage("ToggleSwitch", "A switch for toggling between two on/off states. Try out WToggleSwitch's various features.")

    page.add(buildSimpleToggleSwitchExample())
    page.add(buildCustomContentToggleSwitchExample())
    return page
}

/** A basic toggle switch: responding to toggling (Toggled) and switching it from code. */
private fun buildSimpleToggleSwitchExample(): WComponent {
    val result = WLabel("State: off")

    val toggleSwitch = WToggleSwitch()
    toggleSwitch.addItemListener { isOn ->
        result.text = if (isOn) "State: on" else "State: off"
    }

    val codeButton = WButton("Toggle from code")
    codeButton.addActionListener {
        toggleSwitch.isOn = !toggleSwitch.isOn
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(toggleSwitch)
    row.add(codeButton.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple toggle switch (IsOn / Toggled)", row)
}

/** Customizing the displayed text: header / onContent / offContent. */
private fun buildCustomContentToggleSwitchExample(): WComponent {
    val toggleSwitch = WToggleSwitch(header = "Server status")
    toggleSwitch.onContent = "Running"
    toggleSwitch.offContent = "Stopped"
    return buildExample("Displayed text (Header / OnContent / OffContent)", toggleSwitch)
}

/** The ComboBox page: lines up demos for trying out WComboBox's various features. */
private fun buildComboBoxPage(): WComponent {
    val page = buildPage("ComboBox", "A control for picking one item from a drop-down. Try out WComboBox's various features.")

    page.add(buildSimpleComboBoxExample())
    page.add(buildHeaderComboBoxExample())
    page.add(buildEditableComboBoxExample())
    return page
}

/** A basic combo box: responding to selection changes (SelectionChanged). */
private fun buildSimpleComboBoxExample(): WComponent {
    val result = WLabel("Selected: none")

    val comboBox = WComboBox(listOf("Red", "Green", "Blue", "Yellow"))
    comboBox.width = 200.0
    comboBox.addListSelectionListener {
        val item = comboBox.selectedItem
        result.text = if (item == null) "Selected: none" else "Selected: $item (index = ${comboBox.selectedIndex})"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(comboBox)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple combo box (SelectionChanged)", row)
}

/** Heading and placeholder: header / placeholderText. */
private fun buildHeaderComboBoxExample(): WComponent {
    val comboBox = WComboBox(listOf("Meiryo", "Yu Gothic", "BIZ UDGothic"))
    comboBox.width = 200.0
    comboBox.header = "Font"
    comboBox.placeholderText = "Choose a font"
    return buildExample("Heading and placeholder (Header / PlaceholderText)", comboBox)
}

/** An editable combo box: using isEditable and TextSubmitted to add values not in the list. */
private fun buildEditableComboBoxExample(): WComponent {
    val result = WLabel("Submitted: none")

    val comboBox = WComboBox(listOf("10", "20", "30"))
    comboBox.width = 200.0
    comboBox.isEditable = true
    comboBox.addTextSubmitListener { text ->
        result.text = "Submitted: $text"
        if ((0 until comboBox.itemCount).none { comboBox.getItem(it) == text }) {
            comboBox.addItem(text)
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(comboBox)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("An editable combo box (IsEditable / TextSubmitted)", row)
}

/** The RatingControl page: lines up demos for trying out WRatingControl's various features. */
private fun buildRatingControlPage(): WComponent {
    val page = buildPage("RatingControl", "A control for entering a star rating. Try out WRatingControl's various features.")

    page.add(buildSimpleRatingExample())
    page.add(buildPlaceholderRatingExample())
    page.add(buildReadOnlyRatingExample())
    return page
}

/** A basic rating: responding to value changes (ValueChanged) and clearing it. */
private fun buildSimpleRatingExample(): WComponent {
    val result = WLabel("Rating: unset")

    val rating = WRatingControl()
    rating.isClearEnabled = true
    rating.addChangeListener { value ->
        result.text = if (value < 0) "Rating: unset" else "Rating: ${value.toInt()}"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(rating)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple rating (ValueChanged / IsClearEnabled)", row)
}

/** A placeholder: faintly shows something like an average value before the user rates it. */
private fun buildPlaceholderRatingExample(): WComponent {
    val rating = WRatingControl()
    rating.placeholderValue = 3.5
    rating.caption = "512 reviews"
    return buildExample("A placeholder (PlaceholderValue / Caption)", rating)
}

/** Read-only and star count: isReadOnly / maxRating. */
private fun buildReadOnlyRatingExample(): WComponent {
    val rating = WRatingControl()
    rating.maxRating = 10
    rating.value = 7.0
    rating.isReadOnly = true

    val toggleButton = WButton("Turn off read-only")
    toggleButton.addActionListener {
        rating.isReadOnly = !rating.isReadOnly
        toggleButton.text = if (rating.isReadOnly) "Turn off read-only" else "Make read-only"
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(rating)
    row.add(toggleButton)
    return buildExample("Read-only and star count (IsReadOnly / MaxRating)", row)
}

/** The ColorPicker page: lines up demos for trying out WColorPicker's various features. */
private fun buildColorPickerPage(): WComponent {
    val page = buildPage("ColorPicker", "A control for picking a color from a spectrum. Try out WColorPicker's various features.")

    page.add(buildSimpleColorPickerExample())
    page.add(buildColorPickerOptionsExample())
    return page
}

/** A basic color picker: reflecting color changes (ColorChanged) onto a tile. */
private fun buildSimpleColorPickerExample(): WComponent {
    val tile = buildTile(WColor.BLUE, width = 64.0, height = 64.0)

    val colorPicker = WColorPicker()
    colorPicker.color = WColor.BLUE
    colorPicker.addChangeListener { color ->
        tile.background = color
    }

    val row = WPanel(spacing = 24.0, orientation = Orientation.HORIZONTAL)
    row.add(colorPicker)
    row.add(tile)
    return buildExample("A simple color picker (Color / ColorChanged)", row)
}

/** The NavigationView page: lines up demos for trying out WNavigationView's various features. */
private fun buildNavigationViewPage(): WComponent {
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

/** Display options: isAlphaEnabled / spectrumShape / isMoreButtonVisible. */
private fun buildColorPickerOptionsExample(): WComponent {
    val colorPicker = WColorPicker()
    colorPicker.isAlphaEnabled = true
    colorPicker.isMoreButtonVisible = true

    val shapeButton = WButton("Make the spectrum a ring")
    shapeButton.addActionListener {
        val ring = colorPicker.spectrumShape == ColorSpectrumShape.RING
        colorPicker.spectrumShape = if (ring) ColorSpectrumShape.BOX else ColorSpectrumShape.RING
        shapeButton.text = if (ring) "Make the spectrum a ring" else "Make the spectrum a box"
    }

    val body = WPanel(spacing = 8.0)
    body.add(shapeButton)
    body.add(colorPicker)
    return buildExample("Display options (IsAlphaEnabled / ColorSpectrumShape / IsMoreButtonVisible)", body)
}

/** The AppNotification page: lines up demos for trying out WAppNotification / WAppNotificationManager's various features. */
private fun buildAppNotificationPage(): WComponent {
    val page = buildPage(
        "AppNotification",
        "A notification shown in the Action Center and as a toast popup. " +
            "Try out WAppNotification / WAppNotificationManager's various features.",
    )

    page.add(buildNotificationStatusExample())
    page.add(buildSimpleNotificationExample())
    page.add(buildInteractiveNotificationExample())
    return page
}

/** Registering as a notification sender only needs to happen once per process, so do it once before the first send. */
private var notificationRegistered = false

private fun ensureNotificationRegistered() {
    if (!notificationRegistered) {
        WAppNotificationManager.register()
        notificationRegistered = true
    }
}

/** Whether notifications are usable in this environment: isSupported / setting. */
private fun buildNotificationStatusExample(): WComponent {
    val supported = WLabel("IsSupported: ${WAppNotificationManager.isSupported}")
    val setting = WLabel("Setting: not fetched yet")

    val refreshButton = WButton("Fetch Setting")
    refreshButton.addActionListener {
        setting.text = runCatching { "Setting: ${WAppNotificationManager.setting}" }
            .getOrElse { "Failed to fetch Setting: ${it.message}" }
    }

    val body = WPanel(spacing = 8.0)
    body.add(supported)
    body.add(setting)
    body.add(refreshButton)
    return buildExample("Whether notifications are usable in this environment (IsSupported / Setting)", body)
}

/** A basic notification: a 2-line body + attribution text + display duration. */
private fun buildSimpleNotificationExample(): WComponent {
    val titleField = WTextField("Line 1 (title)").also { it.width = 320.0 }
    titleField.text = "A notification from winui4k"
    val bodyField = WTextField("Line 2 (body)").also { it.width = 320.0 }
    bodyField.text = "A toast notification was sent from Kotlin."
    val longDuration = WCheckBox("Use a longer display duration (Duration.LONG)")
    val result = WLabel("")

    val sendButton = WButton("Send notification")
    sendButton.addActionListener {
        result.text = runCatching {
            ensureNotificationRegistered()
            val notification = WAppNotification(titleField.text)
                .addText(bodyField.text)
                .setAttributionText("WinUI4K Gallery")
                .setTag("gallery-simple")
                .setGroup("gallery")
            if (longDuration.isChecked == true) notification.setDuration(NotificationDuration.LONG)
            WAppNotificationManager.show(notification)
            "Sent"
        }.getOrElse { "Failed to send: ${it.message}" }
    }

    val body = WPanel(spacing = 8.0)
    body.add(titleField)
    body.add(bodyField)
    body.add(longDuration)
    body.add(sendButton)
    body.add(result)
    return buildExample("A basic notification (AddText / SetAttributionText / SetTag / SetGroup / SetDuration)", body)
}

/** A notification with buttons and receiving clicks: AddArgument / AddButton / NotificationInvoked. */
private fun buildInteractiveNotificationExample(): WComponent {
    val scenarioComboBox = WComboBox()
    scenarioComboBox.header = "Scenario"
    for (scenario in NotificationScenario.entries) scenarioComboBox.addItem(scenario.name)
    scenarioComboBox.selectedIndex = 0

    val received = WLabel("Waiting for a click (clicking the notification body or a button delivers the argument here)")
    WAppNotificationManager.addNotificationInvokedListener { argument ->
        received.text = "Received argument: $argument"
    }

    val sendButton = WButton("Send notification with buttons")
    sendButton.addActionListener {
        received.text = runCatching {
            ensureNotificationRegistered()
            val scenario = NotificationScenario.entries[scenarioComboBox.selectedIndex]
            WAppNotificationManager.show(
                WAppNotification("Want to reply?")
                    .addText("Button clicks can be received on the app side.")
                    .addArgument("action", "open")
                    .addButton("Approve", "action" to "approve")
                    .addButton("Reject", "action" to "reject")
                    .setScenario(scenario),
            )
            "Sent. Click the notification"
        }.getOrElse { "Failed to send: ${it.message}" }
    }

    val body = WPanel(spacing = 8.0)
    body.add(scenarioComboBox)
    body.add(sendButton)
    body.add(received)
    return buildExample("A notification with buttons and receiving clicks (AddButton / AddArgument / NotificationInvoked)", body)
}

/** The BadgeNotification page: lines up demos for trying out WBadgeNotification's various features. */
private fun buildBadgeNotificationPage(): WComponent {
    val page = buildPage(
        "BadgeNotification",
        "A badge overlaid on the app's taskbar icon. Try out WBadgeNotification's various features. " +
            "Badges can only be shown by an app with a package identity; setting one under an unpackaged run errors out.",
    )

    page.add(buildBadgeCountExample())
    page.add(buildBadgeGlyphExample())
    return page
}

/** A numeric badge: SetBadgeAsCount / ClearBadge. */
private fun buildBadgeCountExample(): WComponent {
    val result = WLabel("Check the taskbar icon")

    fun countButton(count: Int) = WButton("$count").also { button ->
        button.addActionListener {
            result.text = runCatching {
                WBadgeNotification.setCount(count)
                "Set the badge to $count" + if (count > 99) " (100 and above shows as 99+)" else ""
            }.getOrElse { "Failed to set: ${it.message}" }
        }
    }

    val clearButton = WButton("Clear")
    clearButton.addActionListener {
        result.text = runCatching {
            WBadgeNotification.clear()
            "Cleared the badge"
        }.getOrElse { "Failed to clear: ${it.message}" }
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(countButton(1))
    row.add(countButton(42))
    row.add(countButton(150))
    row.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample("A numeric badge (SetBadgeAsCount / ClearBadge)", body)
}

/** A glyph badge: SetBadgeAsGlyph. */
private fun buildBadgeGlyphExample(): WComponent {
    val glyphComboBox = WComboBox()
    glyphComboBox.header = "BadgeGlyph"
    for (glyph in BadgeGlyph.entries) glyphComboBox.addItem(glyph.name)
    glyphComboBox.selectedIndex = BadgeGlyph.NEW_MESSAGE.ordinal

    val result = WLabel("")
    val applyButton = WButton("Set glyph")
    applyButton.addActionListener {
        val glyph = BadgeGlyph.entries[glyphComboBox.selectedIndex]
        result.text = runCatching {
            WBadgeNotification.setGlyph(glyph)
            "Set the badge to $glyph"
        }.getOrElse { "Failed to set: ${it.message}" }
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(glyphComboBox)
    // The ComboBox is taller because of its header, so nudge this down to align with the input box
    row.add(applyButton.also { it.verticalAlignment = VerticalAlignment.BOTTOM })

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample("A status-glyph badge (SetBadgeAsGlyph)", body)
}

/** The JumpList page: lines up demos for trying out WJumpList / WJumpListItem's various features. */
private fun buildJumpListPage(): WComponent {
    val page = buildPage(
        "JumpList",
        "Adds custom tasks or items to the menu shown when right-clicking the app's taskbar icon. " +
            "Try out WJumpList / WJumpListItem's various features.",
    )

    // IsSupported can return true even for a run without a package identity, so actually
    // load it to check
    val loadFailure = if (WJumpList.isSupported) {
        runCatching { WJumpList.load() }.exceptionOrNull()
    } else {
        IllegalStateException("JumpList.IsSupported is false")
    }
    if (loadFailure != null) {
        page.add(
            buildExample(
                "Not usable in this environment",
                WLabel(
                    "Jump lists only work for an app with a package identity, so they don't work " +
                        "under an unpackaged run (launching java.exe directly). " +
                        "(${loadFailure.message})",
                ).also { it.textWrapping = TextWrapping.WRAP },
            ),
        )
        return page
    }

    page.add(buildJumpListEditExample())
    return page
}

/** Adding an item, removing all, saving, and the current item list. */
private fun buildJumpListEditExample(): WComponent {
    val nameField = WTextField("DisplayName").also { it.width = 320.0 }
    nameField.text = "New document"
    val argumentsField = WTextField("Arguments (launch arguments)").also { it.width = 320.0 }
    argumentsField.text = "/new"

    val itemsLabel = WLabel("")
    val result = WLabel("")

    fun refreshItems(jumpList: WJumpList) {
        val names = jumpList.items.map { if (it.isSeparator) "――――" else it.displayName }
        itemsLabel.text =
            if (names.isEmpty()) "No custom items"
            else "Current items: " + names.joinToString(" / ")
    }

    fun edit(block: (WJumpList) -> String) {
        result.text = runCatching {
            val jumpList = WJumpList.load()
            val message = block(jumpList)
            jumpList.save()
            refreshItems(jumpList)
            message
        }.getOrElse { "Operation failed: ${it.message}" }
    }

    val addButton = WButton("Add item and save")
    addButton.addActionListener {
        edit { jumpList ->
            val item = WJumpListItem.of(argumentsField.text, nameField.text)
            item.description = "An item added by WinUI4K Gallery"
            item.groupName = "Gallery"
            jumpList.add(item)
            "Added. Right-click the taskbar icon to check"
        }
    }

    val separatorButton = WButton("Add separator and save")
    separatorButton.addActionListener {
        edit { jumpList ->
            jumpList.add(WJumpListItem.separator())
            "Added a separator"
        }
    }

    val clearButton = WButton("Remove all items and save")
    clearButton.addActionListener {
        edit { jumpList ->
            jumpList.removeAll()
            "Removed all items"
        }
    }

    runCatching { refreshItems(WJumpList.load()) }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(addButton)
    buttons.add(separatorButton)
    buttons.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(nameField)
    body.add(argumentsField)
    body.add(buttons)
    body.add(itemsLabel)
    body.add(result)
    return buildExample("Editing items (Items / SaveAsync / CreateWithArguments / CreateSeparator)", body)
}

/** The ContentDialog page: lines up demos for trying out WContentDialog's various features. */
private fun buildContentDialogPage(): WComponent {
    val page = buildPage("ContentDialog", "A modal dialog shown layered inside the window. Try out WContentDialog's various features.")

    page.add(buildSimpleContentDialogExample())
    page.add(buildPrimaryButtonEnabledDialogExample())
    return page
}

/** A basic dialog: 3 buttons plus a default button, and receiving the closed result (ContentDialogResult). */
private fun buildSimpleContentDialogExample(): WComponent {
    val result = WLabel("Result: not shown yet")

    val dialog = WContentDialog("Save your work?", WLabel("Saving lets you resume from the same state next time."))
    dialog.primaryButtonText = "Save"
    dialog.secondaryButtonText = "Don't save"
    dialog.closeButtonText = "Cancel"
    dialog.defaultButton = ContentDialogButton.PRIMARY

    val showButton = WButton("Show dialog")
    showButton.addActionListener {
        dialog.show(showButton) { dialogResult ->
            result.text = when (dialogResult) {
                ContentDialogResult.PRIMARY -> "Result: Save"
                ContentDialogResult.SECONDARY -> "Result: Don't save"
                ContentDialogResult.NONE -> "Result: Cancel"
            }
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A basic dialog (PrimaryButton / SecondaryButton / CloseButton / DefaultButton)", row)
}

/** A dialog with a consent checkbox: toggling IsPrimaryButtonEnabled from inside the dialog. */
private fun buildPrimaryButtonEnabledDialogExample(): WComponent {
    val result = WLabel("Result: not shown yet")

    val agreeCheckBox = WCheckBox("I agree to the terms of use")
    val dialogContent = WPanel(spacing = 8.0)
    dialogContent.add(WLabel("Agreeing enables the primary button."))
    dialogContent.add(agreeCheckBox)

    val dialog = WContentDialog("Terms of use", dialogContent)
    dialog.primaryButtonText = "Continue"
    dialog.closeButtonText = "Cancel"
    dialog.defaultButton = ContentDialogButton.PRIMARY
    dialog.isPrimaryButtonEnabled = false
    agreeCheckBox.addItemListener { checked ->
        dialog.isPrimaryButtonEnabled = checked == true
    }

    val showButton = WButton("Show dialog with consent checkbox")
    showButton.addActionListener {
        dialog.show(showButton) { dialogResult ->
            result.text = if (dialogResult == ContentDialogResult.PRIMARY) "Result: Continue" else "Result: Cancel"
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Enabling/disabling a button (IsPrimaryButtonEnabled)", row)
}

/** The Flyout page: lines up demos for trying out WFlyout's various features. */
private fun buildFlyoutPage(): WComponent {
    val page = buildPage("Flyout", "A lightweight popup for confirmations or supplementary information. Try out WFlyout's various features.")

    page.add(buildSimpleFlyoutExample())
    page.add(buildFlyoutPlacementExample())
    return page
}

/** A basic flyout: setting Button.Flyout and a confirmation UI via hide. */
private fun buildSimpleFlyoutExample(): WComponent {
    val result = WLabel("Not run yet")

    val flyoutContent = WPanel(spacing = 8.0)
    val flyout = WFlyout(flyoutContent)
    flyoutContent.add(WLabel("Permanently delete all items?"))
    flyoutContent.add(
        WButton("Yes, delete everything").also { button ->
            button.addActionListener {
                result.text = "Deleted"
                flyout.hide()
            }
        },
    )

    val flyoutButton = WButton("Empty the file")
    flyoutButton.flyout = flyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(flyoutButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A confirmation flyout attached to a button (Button.Flyout / Hide)", row)
}

/** Display position: pick a Placement and open with ShowAt. */
private fun buildFlyoutPlacementExample(): WComponent {
    val flyout = WFlyout(WLabel("A flyout for trying out Placement."))

    val placementComboBox = WComboBox(FlyoutPlacement.entries.map { it.name })
    placementComboBox.width = 240.0
    placementComboBox.header = "Placement"
    placementComboBox.selectedIndex = FlyoutPlacement.TOP.ordinal
    placementComboBox.addListSelectionListener {
        flyout.placement = FlyoutPlacement.entries[placementComboBox.selectedIndex]
    }

    val showButton = WButton("Show flyout")
    showButton.addActionListener { flyout.showAt(showButton) }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(placementComboBox)
    // The ComboBox is taller because of its header, so nudge this down to align with the input box
    row.add(showButton.also { it.verticalAlignment = VerticalAlignment.BOTTOM })
    return buildExample("Display position (Placement / ShowAt)", row)
}

/** The Popup page: lines up demos for trying out WPopup's various features. */
private fun buildPopupPage(): WComponent {
    val page = buildPage("Popup", "A lightweight container for showing arbitrary content layered on the window. Try out WPopup's various features.")

    page.add(buildSimplePopupExample())
    return page
}

/** A basic popup: open/close, offset, light dismiss, and the Closed event. */
private fun buildSimplePopupExample(): WComponent {
    val result = WLabel("State: closed")

    val popup = WPopup()
    popup.horizontalOffset = 200.0
    popup.verticalOffset = 200.0
    popup.addCloseListener { result.text = "State: closed" }

    val popupContent = WPanel(spacing = 8.0)
    popupContent.add(WLabel("This is a popup.").also { it.fontSize = 18.0 })
    popupContent.add(
        WButton("Close").also { button ->
            button.addActionListener { popup.hide() }
        },
    )

    // Popup itself has no decoration, so add a border and background on the content side
    val popupCard = WBorder(popupContent)
    popupCard.background = CARD_BACKGROUND
    popupCard.borderColor = CARD_BORDER
    popupCard.borderThickness = 1.0
    popupCard.cornerRadius = 8.0
    popupCard.padding = 16.0
    popup.child = popupCard

    val lightDismissCheckBox = WCheckBox("Close on an outside click (IsLightDismissEnabled)")
    lightDismissCheckBox.addItemListener { checked ->
        popup.isLightDismissEnabled = checked == true
    }

    val showButton = WButton("Show popup")
    showButton.addActionListener {
        if (!popup.isOpen) {
            popup.show(showButton)
            result.text = "State: open"
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })

    val body = WPanel(spacing = 8.0)
    body.add(lightDismissCheckBox)
    body.add(row)
    return buildExample("A basic popup (IsOpen / Offset / IsLightDismissEnabled / Closed)", body)
}

/** The TeachingTip page: lines up demos for trying out WTeachingTip's various features. */
private fun buildTeachingTipPage(): WComponent {
    val page = buildPage("TeachingTip", "A control that shows a callout pointing at an element, for things like feature announcements. Try out WTeachingTip's various features.")

    page.add(buildTargetedTeachingTipExample())
    page.add(buildUntargetedTeachingTipExample())
    return page
}

/** A callout with a target: Target / PreferredPlacement / ActionButtonClick / Closed. */
private fun buildTargetedTeachingTipExample(): WComponent {
    val result = WLabel("Not shown yet")

    val showButton = WButton("Save (introduces this feature)")

    val tip = WTeachingTip("Autosave is available", "Turn on the setting and your edits will be saved automatically.")
    tip.target = showButton
    tip.preferredPlacement = TeachingTipPlacement.BOTTOM
    tip.actionButtonText = "Turn on"
    tip.closeButtonText = "Later"
    tip.addActionListener {
        result.text = "Action: turned on autosave"
        tip.hide()
    }
    tip.addCloseListener { reason ->
        if (reason != TeachingTipCloseReason.PROGRAMMATIC) result.text = "Close reason: $reason"
    }

    showButton.addActionListener { tip.show() }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(tip) // placed in the XAML tree, matching the real Gallery (renders nothing while closed)
    return buildExample("A callout pointing at an element (Target / ActionButtonClick / Closed)", row)
}

/** A callout without a target: shown in a screen corner, with light dismiss. */
private fun buildUntargetedTeachingTipExample(): WComponent {
    val tip = WTeachingTip("New feature announcement", "A callout without a target is shown in a screen corner. Closes on an outside click.")
    tip.isLightDismissEnabled = true

    val showButton = WButton("Show announcement")
    showButton.addActionListener { tip.show() }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(tip) // placed in the XAML tree, matching the real Gallery (renders nothing while closed)
    return buildExample("A callout without a target (IsLightDismissEnabled)", row)
}

/** MenuBar page: lines up demos exercising WMenuBar / WMenuBarItem. */
private fun buildMenuBarPage(): WComponent {
    val page = buildPage("MenuBar", "A menu bar that lines up top-level menus horizontally. Try out WMenuBar / WMenuBarItem.")

    page.add(buildSimpleMenuBarExample())
    page.add(buildRichMenuBarExample())
    return page
}

/** A basic menu bar: File / Edit / Help menus, with item-click subscriptions. */
private fun buildSimpleMenuBarExample(): WComponent {
    val result = WLabel("Selected item: none")

    fun item(text: String) = WMenuFlyoutItem(text).also { item ->
        item.addActionListener { result.text = "Selected item: $text" }
    }

    val fileMenu = WMenuBarItem("File")
    fileMenu.add(item("New"))
    fileMenu.add(item("Open"))
    fileMenu.add(item("Save"))
    fileMenu.add(WMenuFlyoutSeparator())
    fileMenu.add(item("Exit"))

    val editMenu = WMenuBarItem("Edit")
    editMenu.add(item("Undo"))
    editMenu.add(item("Cut"))
    editMenu.add(item("Copy"))
    editMenu.add(item("Paste"))

    val helpMenu = WMenuBarItem("Help")
    helpMenu.add(item("About"))

    val menuBar = WMenuBar()
    menuBar.add(fileMenu)
    menuBar.add(editMenu)
    menuBar.add(helpMenu)

    val body = WPanel(spacing = 8.0)
    body.add(menuBar)
    body.add(result)
    return buildExample("A simple menu bar", body)
}

/** A richer menu bar: icons, shortcuts, submenus, toggle and radio items. */
private fun buildRichMenuBarExample(): WComponent {
    val result = WLabel("Selected item: none")

    fun item(text: String, icon: Symbol? = null) = WMenuFlyoutItem(text, icon).also { item ->
        item.addActionListener { result.text = "Selected item: $text" }
    }

    // Icons plus shortcuts that actually fire (work even without opening the menu)
    val newItem = item("New", Symbol.ADD)
    newItem.addKeyboardAccelerator(VirtualKey.N, VirtualKeyModifier.CONTROL)
    val openItem = item("Open", Symbol.OPEN_FILE)
    openItem.addKeyboardAccelerator(VirtualKey.O, VirtualKeyModifier.CONTROL)
    val saveItem = item("Save", Symbol.SAVE)
    saveItem.addKeyboardAccelerator(VirtualKey.S, VirtualKeyModifier.CONTROL)

    // A submenu (cascading)
    val shareSubMenu = WMenuFlyoutSubItem("Share", Symbol.SHARE)
    shareSubMenu.add(item("Send by email", Symbol.MAIL))
    shareSubMenu.add(item("Copy link", Symbol.LINK))

    val fileMenu = WMenuBarItem("File")
    fileMenu.add(newItem)
    fileMenu.add(openItem)
    fileMenu.add(saveItem)
    fileMenu.add(WMenuFlyoutSeparator())
    fileMenu.add(shareSubMenu)

    // A toggle item and radio items (mutually exclusive selection)
    val statusBarItem = WToggleMenuFlyoutItem("Show status bar")
    statusBarItem.isChecked = true
    statusBarItem.addActionListener {
        result.text = "Show status bar: ${statusBarItem.isChecked}"
    }

    fun radio(text: String) = WRadioMenuFlyoutItem(text, groupName = "orientation").also { item ->
        item.addActionListener { result.text = "Orientation: $text" }
    }

    val landscapeItem = radio("Landscape")
    landscapeItem.isChecked = true

    val viewMenu = WMenuBarItem("View")
    viewMenu.add(statusBarItem)
    viewMenu.add(WMenuFlyoutSeparator())
    viewMenu.add(landscapeItem)
    viewMenu.add(radio("Portrait"))

    val menuBar = WMenuBar()
    menuBar.add(fileMenu)
    menuBar.add(viewMenu)

    val body = WPanel(spacing = 8.0)
    body.add(menuBar)
    body.add(result)
    return buildExample("Icons, shortcuts, submenus, and toggle / radio items", body)
}

/** MenuFlyout page: lines up demos exercising WMenuFlyout. */
private fun buildMenuFlyoutPage(): WComponent {
    val page = buildPage("MenuFlyout", "A menu that temporarily shows a list of commands. Try out WMenuFlyout and its menu items.")

    page.add(buildSimpleMenuFlyoutExample())
    page.add(buildRadioMenuFlyoutExample())
    page.add(buildContextMenuFlyoutExample())
    return page
}

/** A basic menu flyout: opened from a DropDownButton. */
private fun buildSimpleMenuFlyoutExample(): WComponent {
    val result = WLabel("Selected item: none")

    val menuFlyout = WMenuFlyout()
    for (text in listOf("Reset items", "Repeat", "Shuffle")) {
        menuFlyout.add(
            WMenuFlyoutItem(text).also { item ->
                item.addActionListener { result.text = "Selected item: $text" }
            },
        )
    }

    val button = WDropDownButton("Options")
    button.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple menu flyout", row)
}

/** A menu flyout with radio items: mutually-exclusive sort criteria. */
private fun buildRadioMenuFlyoutExample(): WComponent {
    val result = WLabel("Sort by: rating")

    val menuFlyout = WMenuFlyout()
    for (text in listOf("Rating", "Name", "Date")) {
        val item = WRadioMenuFlyoutItem(text, groupName = "sort")
        item.isChecked = text == "Rating"
        item.addActionListener { result.text = "Sort by: $text" }
        menuFlyout.add(item)
    }

    val button = WDropDownButton("Sort")
    button.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Mutually-exclusive selection with radio items (WRadioMenuFlyoutItem)", row)
}

/** A context menu: a MenuFlyout opened by right-click (UIElement.ContextFlyout). */
private fun buildContextMenuFlyoutExample(): WComponent {
    val result = WLabel("Selected item: none")

    val menuFlyout = WMenuFlyout()
    val copyItem = WMenuFlyoutItem("Copy", Symbol.COPY)
    copyItem.keyboardAcceleratorText = "Ctrl+C" // a display-only shortcut string
    copyItem.addActionListener { result.text = "Selected item: Copy" }
    menuFlyout.add(copyItem)
    menuFlyout.add(
        WMenuFlyoutItem("Delete", Symbol.DELETE).also { item ->
            item.addActionListener { result.text = "Selected item: Delete" }
        },
    )

    val target = WBorder(
        WLabel("Right-click here").also { it.margin = 24.0 },
    )
    target.background = WColor(226, 235, 246)
    target.cornerRadius = 8.0
    target.contextFlyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(target)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A context menu (ContextFlyout)", row)
}

/** AppBarButton page: lines up demos exercising WAppBarButton. */
private fun buildAppBarButtonPage(): WComponent {
    val page = buildPage("AppBarButton", "A toolbar button with an icon and label stacked vertically. Try out WAppBarButton.")

    page.add(buildSimpleAppBarButtonExample())
    page.add(buildFlyoutAppBarButtonExample())
    return page
}

/** A basic AppBarButton: a Symbol icon and a click subscription. */
private fun buildSimpleAppBarButtonExample(): WComponent {
    val result = WLabel("Click: none")

    val likeButton = WAppBarButton("Like", Symbol.LIKE)
    likeButton.addActionListener { result.text = "Click: Like" }

    val saveButton = WAppBarButton("Save", Symbol.SAVE)
    saveButton.keyboardAcceleratorText = "Ctrl+S" // shown in the tooltip
    saveButton.addActionListener { result.text = "Click: Save" }

    val disabledButton = WAppBarButton("Disabled", Symbol.CANCEL)
    disabledButton.isEnabled = false

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(likeButton)
    row.add(saveButton)
    row.add(disabledButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple AppBarButton (Label / Icon / IsEnabled)", row)
}

/** An AppBarButton with a flyout: opens a menu on click. */
private fun buildFlyoutAppBarButtonExample(): WComponent {
    val result = WLabel("Selected item: none")

    val menuFlyout = WMenuFlyout()
    for (text in listOf("PNG format", "JPEG format", "SVG format")) {
        menuFlyout.add(
            WMenuFlyoutItem(text).also { item ->
                item.addActionListener { result.text = "Selected item: $text" }
            },
        )
    }

    val exportButton = WAppBarButton("Export", Symbol.DOWNLOAD)
    exportButton.flyout = menuFlyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(exportButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("An AppBarButton with a menu (Flyout)", row)
}

/** AppBarToggleButton page: lines up demos exercising WAppBarToggleButton. */
private fun buildAppBarToggleButtonPage(): WComponent {
    val page = buildPage("AppBarToggleButton", "A toolbar button with on/off state. Try out WAppBarToggleButton.")

    page.add(buildSimpleAppBarToggleButtonExample())
    return page
}

/** A basic AppBarToggleButton: subscribing to and flipping the checked state. */
private fun buildSimpleAppBarToggleButtonExample(): WComponent {
    val result = WLabel("Shuffle: off")

    val shuffleButton = WAppBarToggleButton("Shuffle", Symbol.SHUFFLE)
    shuffleButton.addItemListener { isChecked ->
        result.text = "Shuffle: ${if (isChecked == true) "on" else "off"}"
    }

    val boldButton = WAppBarToggleButton("Bold", Symbol.BOLD)
    boldButton.isChecked = true

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(shuffleButton)
    row.add(boldButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A simple AppBarToggleButton (IsChecked)", row)
}

/** AppBarSeparator page: lines up a WAppBarSeparator demo. */
private fun buildAppBarSeparatorPage(): WComponent {
    val page = buildPage("AppBarSeparator", "A divider line that groups commands on a toolbar.")

    page.add(buildAppBarSeparatorExample())
    return page
}

/** Dividing a row of AppBarButtons with AppBarSeparator. */
private fun buildAppBarSeparatorExample(): WComponent {
    val row = WPanel(spacing = 4.0, orientation = Orientation.HORIZONTAL)
    row.add(WAppBarButton("Back", Symbol.BACK))
    row.add(WAppBarButton("Forward", Symbol.FORWARD))
    row.add(WAppBarSeparator())
    row.add(WAppBarButton("Refresh", Symbol.REFRESH))
    row.add(WAppBarSeparator())
    row.add(WAppBarButton("Favorite", Symbol.FAVORITE))
    return buildExample("Grouping commands", row)
}

/** CommandBar page: lines up demos exercising WCommandBar. */
private fun buildCommandBarPage(): WComponent {
    val page = buildPage("CommandBar", "A toolbar of commands. Try out WCommandBar.")

    page.add(buildSimpleCommandBarExample())
    page.add(buildCommandBarLabelPositionExample())
    return page
}

/** A basic command bar: Primary / Secondary commands and open/close. */
private fun buildSimpleCommandBarExample(): WComponent {
    val result = WLabel("Click: none")

    fun appBarButton(label: String, icon: Symbol) = WAppBarButton(label, icon).also { button ->
        button.addActionListener { result.text = "Click: $label" }
    }

    val commandBar = WCommandBar()
    commandBar.addPrimaryCommand(appBarButton("Add", Symbol.ADD))
    commandBar.addPrimaryCommand(appBarButton("Edit", Symbol.EDIT))
    commandBar.addPrimaryCommand(WAppBarSeparator())
    commandBar.addPrimaryCommand(appBarButton("Share", Symbol.SHARE))
    // Secondary commands go into the overflow menu opened via […]
    commandBar.addSecondaryCommand(appBarButton("Settings", Symbol.SETTING))
    commandBar.addSecondaryCommand(appBarButton("Help", Symbol.HELP))

    val openButton = WButton("Toggle IsOpen")
    openButton.addActionListener { commandBar.isOpen = !commandBar.isOpen }

    val body = WPanel(spacing = 8.0)
    body.add(commandBar)
    body.add(
        WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL).also { controls ->
            controls.add(openButton)
            controls.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
        },
    )
    return buildExample("A simple command bar (PrimaryCommands / SecondaryCommands / IsOpen)", body)
}

/** Changing the label position: showing labels to the right of icons via DefaultLabelPosition. */
private fun buildCommandBarLabelPositionExample(): WComponent {
    val commandBar = WCommandBar()
    commandBar.defaultLabelPosition = CommandBarDefaultLabelPosition.RIGHT
    commandBar.addPrimaryCommand(WAppBarButton("Add", Symbol.ADD))
    commandBar.addPrimaryCommand(WAppBarButton("Edit", Symbol.EDIT))
    commandBar.addPrimaryCommand(WAppBarButton("Delete", Symbol.DELETE))
    return buildExample("Showing labels to the right of icons (DefaultLabelPosition)", commandBar)
}

/** CommandBarFlyout page: lines up demos exercising WCommandBarFlyout. */
private fun buildCommandBarFlyoutPage(): WComponent {
    val page = buildPage("CommandBarFlyout", "A context menu with a mini toolbar attached. Try out WCommandBarFlyout.")

    page.add(buildCommandBarFlyoutExample())
    return page
}

/** Attaching a CommandBarFlyout to an image-like tile: opens via right-click or a button. */
private fun buildCommandBarFlyoutExample(): WComponent {
    val result = WLabel("Click: none")

    fun appBarButton(label: String, icon: Symbol) = WAppBarButton(label, icon).also { button ->
        button.addActionListener { result.text = "Click: $label" }
    }

    val flyout = WCommandBarFlyout()
    flyout.addPrimaryCommand(appBarButton("Share", Symbol.SHARE))
    flyout.addPrimaryCommand(appBarButton("Save", Symbol.SAVE))
    flyout.addPrimaryCommand(appBarButton("Delete", Symbol.DELETE))
    // Secondary commands go into the menu below the mini toolbar
    flyout.addSecondaryCommand(appBarButton("Resize", Symbol.ZOOM))
    flyout.addSecondaryCommand(appBarButton("Move", Symbol.MOVE_TO_FOLDER))

    val target = WBorder(
        WLabel("Right-click here").also { it.margin = 32.0 },
    )
    target.background = WColor(226, 246, 235)
    target.cornerRadius = 8.0
    target.contextFlyout = flyout

    val showButton = WButton("Show flyout")
    showButton.addActionListener { flyout.showAt(target) }

    val body = WPanel(spacing = 8.0)
    body.add(target)
    body.add(
        WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL).also { controls ->
            controls.add(showButton)
            controls.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
        },
    )
    return buildExample("A context menu with a mini toolbar attached (ContextFlyout / ShowAt)", body)
}

/** SwipeControl page: lines up demos exercising WSwipeControl. */
private fun buildSwipeControlPage(): WComponent {
    val page = buildPage(
        "SwipeControl",
        "A container that reveals commands on a touch swipe. Swiping is touch/pen only; it doesn't open with a mouse.",
    )

    page.add(buildSwipeControlExample())
    return page
}

/** Attaching Reveal / Execute swipe items to list-row-like content. */
private fun buildSwipeControlExample(): WComponent {
    val result = WLabel("Item run: none")

    // Swipe right (from the left edge) -> reveals a pin button (Reveal)
    val pinItem = WSwipeItem("Pin", Symbol.PIN)
    pinItem.background = WColor(96, 165, 250)
    pinItem.addActionListener { result.text = "Item run: Pin" }
    val leftItems = WSwipeItems(SwipeMode.REVEAL)
    leftItems.add(pinItem)

    // Swipe left (from the right edge) -> deletes immediately once fully swiped (Execute)
    val deleteItem = WSwipeItem("Delete", Symbol.DELETE)
    deleteItem.background = WColor(239, 68, 68)
    deleteItem.addActionListener { result.text = "Item run: Delete" }
    val rightItems = WSwipeItems(SwipeMode.EXECUTE)
    rightItems.add(deleteItem)

    val rowContent = WBorder(
        WLabel("Touch-swipe this row left or right").also { it.margin = 16.0 },
    )
    rowContent.background = CARD_BACKGROUND
    rowContent.borderColor = CARD_BORDER
    rowContent.borderThickness = 1.0

    val swipeControl = WSwipeControl(rowContent)
    swipeControl.width = 320.0
    swipeControl.leftItems = leftItems
    swipeControl.rightItems = rightItems

    val body = WPanel(spacing = 8.0)
    body.add(swipeControl)
    body.add(result)
    return buildExample("Swipe items (LeftItems: Reveal / RightItems: Execute)", body)
}

/** StandardUICommand page: lines up demos exercising WStandardUICommand. */
private fun buildStandardUICommandPage(): WComponent {
    val page = buildPage(
        "StandardUICommand",
        "A predefined command with an OS-standard label, icon, and shortcut already set. Try out WStandardUICommand.",
    )

    page.add(buildStandardUICommandExample())
    return page
}

/** A list of predefined commands: each Kind's label and icon apply automatically. */
private fun buildStandardUICommandExample(): WComponent {
    val result = WLabel("Command run: none")

    val row = WPanel(spacing = 4.0, orientation = Orientation.HORIZONTAL)
    for (kind in listOf(
        StandardUICommandKind.CUT,
        StandardUICommandKind.COPY,
        StandardUICommandKind.PASTE,
        StandardUICommandKind.DELETE,
        StandardUICommandKind.UNDO,
        StandardUICommandKind.REDO,
    )) {
        val command = WStandardUICommand(kind)
        command.addExecuteListener { result.text = "Command run: ${command.label}" }
        // The label, icon, and shortcut (e.g. Ctrl+C) apply automatically from the command
        row.add(WAppBarButton().also { it.command = command })
    }

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample("A toolbar of predefined commands (Kind / ExecuteRequested)", body)
}

/** XamlUICommand page: lines up demos exercising WXamlUICommand. */
private fun buildXamlUICommandPage(): WComponent {
    val page = buildPage(
        "XamlUICommand",
        "A reusable command that bundles a label, icon, and shortcut together. Try out WXamlUICommand.",
    )

    page.add(buildXamlUICommandExample())
    return page
}

/** A custom command: sharing the same look and behavior across multiple controls. */
private fun buildXamlUICommandExample(): WComponent {
    val result = WLabel("Run count: 0")
    var count = 0

    val command = WXamlUICommand("Add to favorites")
    command.icon = Symbol.FAVORITE
    command.description = "Adds the selected item to your favorites"
    command.addKeyboardAccelerator(VirtualKey.F, VirtualKeyModifier.CONTROL, VirtualKeyModifier.SHIFT)
    command.addExecuteListener {
        count++
        result.text = "Run count: $count"
    }

    // Set the same command on a Button and an AppBarButton (label and icon apply automatically)
    val button = WButton()
    button.command = command
    val appBarButton = WAppBarButton()
    appBarButton.command = command

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(button.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(appBarButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(WLabel("The Ctrl+Shift+F shortcut also runs it").also { it.foreground = TEXT_SECONDARY })
    return buildExample("Sharing a command (Label / IconSource / KeyboardAccelerator / ExecuteRequested)", body)
}

/** TextBlock page: lines up demos exercising WLabel. */
private fun buildTextBlockPage(): WComponent {
    val page = buildPage("TextBlock", "A basic control that displays read-only text. Try out WLabel.")

    page.add(buildSimpleTextBlockExample())
    page.add(buildTextBlockStyleExample())
    page.add(buildTextBlockWrappingExample())
    page.add(buildTextBlockSelectionExample())
    return page
}

/** Basic text display. */
private fun buildSimpleTextBlockExample(): WComponent {
    return buildExample("Simple text", WLabel("I am a TextBlock."))
}

/** Text appearance: change font size, weight, and color. */
private fun buildTextBlockStyleExample(): WComponent {
    val body = WPanel(spacing = 8.0)
    body.add(WLabel("Text at font size 18").also { it.fontSize = 18.0 })
    body.add(WLabel("SemiBold (600) text").also { it.fontWeight = 600 })
    body.add(WLabel("Colored text").also { it.foreground = WColor(0, 95, 184) })
    return buildExample("Changing the style (FontSize / FontWeight / Foreground)", body)
}

/** Wrapping and trimming: handling text that doesn't fit the width. */
private fun buildTextBlockWrappingExample(): WComponent {
    val longText = "This text is long enough that it doesn't fit the control's width, so you can see wrapping and trimming in action."

    val wrapped = WLabel(longText)
    wrapped.width = 300.0
    wrapped.textWrapping = TextWrapping.WRAP

    val trimmed = WLabel(longText)
    trimmed.width = 300.0
    trimmed.textTrimming = TextTrimming.CHARACTER_ELLIPSIS

    val body = WPanel(spacing = 8.0)
    body.add(WLabel("TextWrapping.WRAP:").also { it.foreground = TEXT_SECONDARY })
    body.add(wrapped)
    body.add(WLabel("TextTrimming.CHARACTER_ELLIPSIS:").also { it.foreground = TEXT_SECONDARY })
    body.add(trimmed)
    return buildExample("Wrapping and trimming (TextWrapping / TextTrimming)", body)
}

/** Text selection: allow selecting and copying with the mouse. */
private fun buildTextBlockSelectionExample(): WComponent {
    val selectable = WLabel("This text can be selected by dragging with the mouse.")
    selectable.isTextSelectionEnabled = true
    return buildExample("Text selection (IsTextSelectionEnabled)", selectable)
}

/** TextBox page: lines up demos exercising WTextField. */
private fun buildTextBoxPage(): WComponent {
    val page = buildPage("TextBox", "A control for entering single-line or multi-line text. Try out WTextField.")

    page.add(buildSimpleTextBoxExample())
    page.add(buildHeaderTextBoxExample())
    page.add(buildMultiLineTextBoxExample())
    page.add(buildReadOnlyTextBoxExample())
    page.add(buildTextChangedTextBoxExample())
    return page
}

/** Basic text box: with a placeholder. */
private fun buildSimpleTextBoxExample(): WComponent {
    val textField = WTextField(placeholder = "Enter your name")
    textField.width = 300.0
    return buildExample("Simple text box (PlaceholderText)", textField)
}

/** Header and max length: Header and MaxLength. */
private fun buildHeaderTextBoxExample(): WComponent {
    val textField = WTextField(placeholder = "You can enter up to 10 characters")
    textField.width = 300.0
    textField.header = "Username"
    textField.maxLength = 10
    return buildExample("Header and max length (Header / MaxLength)", textField)
}

/** Multi-line input: AcceptsReturn and TextWrapping. */
private fun buildMultiLineTextBoxExample(): WComponent {
    val textArea = WTextField(placeholder = "Press Enter to add a new line")
    textArea.width = 400.0
    textArea.height = 120.0
    textArea.acceptsReturn = true
    textArea.textWrapping = TextWrapping.WRAP
    return buildExample("Multi-line input (AcceptsReturn / TextWrapping)", textArea)
}

/** Read-only: toggling IsReadOnly. */
private fun buildReadOnlyTextBoxExample(): WComponent {
    val textField = WTextField()
    textField.width = 300.0
    textField.text = "This text is read-only"
    textField.isReadOnly = true

    val toggleButton = WButton("Allow editing")
    toggleButton.addActionListener {
        textField.isReadOnly = !textField.isReadOnly
        toggleButton.text = if (textField.isReadOnly) "Allow editing" else "Make read-only again"
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(textField)
    row.add(toggleButton)
    return buildExample("Read-only (IsReadOnly)", row)
}

/** Watching input: mirror the input via TextChanged, and select all via SelectAll. */
private fun buildTextChangedTextBoxExample(): WComponent {
    val mirror = WLabel("The text you type appears here").also { it.foreground = TEXT_SECONDARY }

    val textField = WTextField(placeholder = "Reflected below as you type")
    textField.width = 300.0
    textField.addTextChangedListener { text ->
        mirror.text = if (text.isEmpty()) "The text you type appears here" else text
    }

    val selectAllButton = WButton("Select all")
    selectAllButton.addActionListener { textField.selectAll() }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(textField)
    row.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(mirror)
    return buildExample("Watching input (TextChanged / SelectAll)", body)
}

/** PasswordBox page: lines up demos exercising WPasswordField. */
private fun buildPasswordBoxPage(): WComponent {
    val page = buildPage("PasswordBox", "A control for entering a password as masked characters. Try out WPasswordField.")

    page.add(buildSimplePasswordBoxExample())
    page.add(buildRevealModePasswordBoxExample())
    return page
}

/** Basic password box: simple validation via PasswordChanged. */
private fun buildSimplePasswordBoxExample(): WComponent {
    val result = WLabel("Enter a password of at least 8 characters").also { it.foreground = TEXT_SECONDARY }

    val passwordField = WPasswordField(placeholder = "Enter password")
    passwordField.width = 300.0
    passwordField.header = "Password"
    passwordField.addPasswordChangedListener { password ->
        result.text = when {
            password.isEmpty() -> "Enter a password of at least 8 characters"
            password.length < 8 -> "${8 - password.length} more characters needed"
            else -> "OK (${password.length} characters)"
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(passwordField)
    body.add(result)
    return buildExample("Simple password box (Header / PasswordChanged)", body)
}

/** Reveal mode and mask character: PasswordRevealMode and PasswordChar. */
private fun buildRevealModePasswordBoxExample(): WComponent {
    val hiddenField = WPasswordField(placeholder = "No reveal button (HIDDEN)")
    hiddenField.width = 300.0
    hiddenField.passwordRevealMode = PasswordRevealMode.HIDDEN

    val customCharField = WPasswordField(placeholder = "Use # as the mask character")
    customCharField.width = 300.0
    customCharField.passwordChar = "#"

    val body = WPanel(spacing = 8.0)
    body.add(hiddenField)
    body.add(customCharField)
    return buildExample("Reveal mode and mask character (PasswordRevealMode / PasswordChar)", body)
}

/** NumberBox page: lines up demos exercising WSpinner. */
private fun buildNumberBoxPage(): WComponent {
    val page = buildPage("NumberBox", "A control for entering, validating, and incrementing/decrementing numbers. Try out WSpinner.")

    page.add(buildExpressionNumberBoxExample())
    page.add(buildSpinButtonNumberBoxExample())
    return page
}

/** Expression input: evaluate expressions like "(5 + 3) * 2" via AcceptsExpression. */
private fun buildExpressionNumberBoxExample(): WComponent {
    val result = WLabel("The confirmed value appears here").also { it.foreground = TEXT_SECONDARY }

    val spinner = WSpinner()
    spinner.width = 300.0
    spinner.header = "You can also enter an expression (e.g. (5 + 3) * 2)"
    spinner.placeholderText = "1 + 2 * 3"
    spinner.acceptsExpression = true
    spinner.addChangeListener { value ->
        result.text = if (value.isNaN()) "Not entered" else "Value: $value"
    }

    val body = WPanel(spacing = 8.0)
    body.add(spinner)
    body.add(result)
    return buildExample("Expression input (AcceptsExpression / ValueChanged)", body)
}

/** Spin buttons: placement, step, and wrapping of the increment/decrement buttons. */
private fun buildSpinButtonNumberBoxExample(): WComponent {
    val spinner = WSpinner(value = 10.0)
    spinner.width = 150.0
    spinner.header = "0 to 100 (steps of 10, wraps around)"
    spinner.minimum = 0.0
    spinner.maximum = 100.0
    spinner.smallChange = 10.0
    spinner.largeChange = 25.0
    spinner.spinButtonPlacementMode = SpinButtonPlacementMode.INLINE
    spinner.isWrapEnabled = true
    return buildExample("Spin buttons (SpinButtonPlacementMode / SmallChange / IsWrapEnabled)", spinner)
}

/** AutoSuggestBox page: lines up demos exercising WAutoSuggestBox. */
private fun buildAutoSuggestBoxPage(): WComponent {
    val page = buildPage(
        "AutoSuggestBox",
        "A text box that shows a list of suggestions as you type. Try out WAutoSuggestBox.",
    )

    page.add(buildSimpleAutoSuggestBoxExample())
    return page
}

/** Filtering suggestions: replace suggestions via TextChanged, and confirm via QuerySubmitted. */
private fun buildSimpleAutoSuggestBoxExample(): WComponent {
    val fruits = listOf(
        "Apple", "Orange", "Grape", "Peach", "Cherry",
        "Banana", "Pineapple", "Melon", "Strawberry", "Kiwi",
    )
    val result = WLabel("Confirm with Enter or by choosing a suggestion").also { it.foreground = TEXT_SECONDARY }

    val suggestBox = WAutoSuggestBox(placeholder = "Enter a fruit name")
    suggestBox.width = 300.0
    suggestBox.header = "Search fruits"
    suggestBox.queryIcon = Symbol.FIND
    suggestBox.addTextChangedListener { text, reason ->
        // Only filter suggestions on the user's own keystrokes (do nothing for e.g. suggestion selection)
        if (reason == TextChangeReason.USER_INPUT) {
            suggestBox.setSuggestions(fruits.filter { it.contains(text) })
        }
    }
    suggestBox.addQuerySubmittedListener { queryText, chosenSuggestion ->
        result.text = if (chosenSuggestion != null) {
            "Confirmed from suggestion: $chosenSuggestion"
        } else {
            "Confirmed as typed: $queryText"
        }
    }

    val body = WPanel(spacing = 8.0)
    body.add(suggestBox)
    body.add(result)
    return buildExample("Filtering suggestions (TextChanged / QuerySubmitted)", body)
}

/** RichEditBox page: lines up demos exercising WTextPane. */
private fun buildRichEditBoxPage(): WComponent {
    val page = buildPage(
        "RichEditBox",
        "A control for editing formatted text such as bold and italic. Try out WTextPane.",
    )

    page.add(buildFormattingRichEditBoxExample())
    return page
}

/** Editing formatted text: toggling bold/italic on the selection, plus Undo/Redo. */
private fun buildFormattingRichEditBoxExample(): WComponent {
    val textPane = WTextPane(placeholder = "Enter text, select it, then press a formatting button")
    textPane.width = 400.0
    textPane.height = 150.0

    val boldButton = WButton("Bold")
    boldButton.addActionListener { textPane.toggleSelectionBold() }

    val italicButton = WButton("Italic")
    italicButton.addActionListener { textPane.toggleSelectionItalic() }

    val undoButton = WButton("Undo")
    undoButton.addActionListener { if (textPane.canUndo) textPane.undo() }

    val redoButton = WButton("Redo")
    redoButton.addActionListener { if (textPane.canRedo) textPane.redo() }

    val toolbar = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    toolbar.add(boldButton)
    toolbar.add(italicButton)
    toolbar.add(undoButton)
    toolbar.add(redoButton)

    val body = WPanel(spacing = 8.0)
    body.add(toolbar)
    body.add(textPane)
    return buildExample("Editing formatted text (Bold / Italic / Undo / Redo)", body)
}

/** RichTextBlock page: lines up demos exercising WRichTextBlock. */
private fun buildRichTextBlockPage(): WComponent {
    val page = buildPage(
        "RichTextBlock",
        "A control that displays read-only formatted text mixing bold and italic. Try out WRichTextBlock.",
    )

    page.add(buildSimpleRichTextBlockExample())
    page.add(buildSelectionRichTextBlockExample())
    return page
}

/** Displaying formatted text: composing paragraphs from Run / Bold / Italic / Underline. */
private fun buildSimpleRichTextBlockExample(): WComponent {
    val richTextBlock = WRichTextBlock()
    richTextBlock.width = 400.0
    richTextBlock.addParagraph {
        run("RichTextBlock can mix ")
        bold("bold")
        run(", ")
        italic("italic")
        run(", and ")
        underline("underlined")
        run(" text together in a single block.")
    }
    richTextBlock.addParagraph {
        run("Adding multiple paragraphs displays them with spacing in between.")
    }
    return buildExample("Displaying formatted text (Paragraph / Run / Bold / Italic / Underline)", richTextBlock)
}

/** Text selection: SelectAll and reading SelectedText. */
private fun buildSelectionRichTextBlockExample(): WComponent {
    val result = WLabel("The selected text appears here").also { it.foreground = TEXT_SECONDARY }

    val richTextBlock = WRichTextBlock()
    richTextBlock.width = 400.0
    richTextBlock.addParagraph {
        run("This text can be selected with the mouse. Drag to select, then press the button below.")
    }

    val readButton = WButton("Get selected text")
    readButton.addActionListener {
        val selected = richTextBlock.selectedText
        result.text = if (selected.isEmpty()) "Nothing is selected" else "Selected: $selected"
    }

    val selectAllButton = WButton("Select all")
    selectAllButton.addActionListener { richTextBlock.selectAll() }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(readButton)
    row.add(selectAllButton)

    val body = WPanel(spacing = 8.0)
    body.add(richTextBlock)
    body.add(row)
    body.add(result)
    return buildExample("Text selection (IsTextSelectionEnabled / SelectedText / SelectAll)", body)
}
