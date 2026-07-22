package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.NavigationViewBackButtonVisible
import com.appkitbox.winui4k.Symbol
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WNavigationView
import com.appkitbox.winui4k.WNavigationViewItem

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
    "CalendarDatePicker" to ::buildCalendarDatePickerPage,
    "CalendarView" to ::buildCalendarViewPage,
    "Canvas" to ::buildCanvasPage,
    "CheckBox" to ::buildCheckBoxPage,
    "ColorPicker" to ::buildColorPickerPage,
    "ComboBox" to ::buildComboBoxPage,
    "CommandBar" to ::buildCommandBarPage,
    "CommandBarFlyout" to ::buildCommandBarFlyoutPage,
    "ContentDialog" to ::buildContentDialogPage,
    "DatePicker" to ::buildDatePickerPage,
    "DropDownButton" to ::buildDropDownButtonPage,
    "Expander" to ::buildExpanderPage,
    "Flyout" to ::buildFlyoutPage,
    "Grid" to ::buildGridPage,
    "HyperlinkButton" to ::buildHyperlinkButtonPage,
    "InfoBadge" to ::buildInfoBadgePage,
    "InfoBar" to ::buildInfoBarPage,
    "JumpList" to ::buildJumpListPage,
    "ListBox" to ::buildListBoxPage,
    "ListView" to ::buildListViewPage,
    "MenuBar" to ::buildMenuBarPage,
    "MenuFlyout" to ::buildMenuFlyoutPage,
    "NavigationView" to ::buildNavigationViewPage,
    "NumberBox" to ::buildNumberBoxPage,
    "PasswordBox" to ::buildPasswordBoxPage,
    "PipsPager" to ::buildPipsPagerPage,
    "Popup" to ::buildPopupPage,
    "ProgressBar" to ::buildProgressBarPage,
    "ProgressRing" to ::buildProgressRingPage,
    "RadioButton" to ::buildRadioButtonPage,
    "RatingControl" to ::buildRatingControlPage,
    "RelativePanel" to ::buildRelativePanelPage,
    "RepeatButton" to ::buildRepeatButtonPage,
    "AnnotatedScrollBar" to ::buildAnnotatedScrollBarPage,
    "ScrollView" to ::buildScrollViewPage,
    "ScrollViewer" to ::buildScrollViewerPage,
    "SemanticZoom" to ::buildSemanticZoomPage,
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
    "TimePicker" to ::buildTimePickerPage,
    "ToggleButton" to ::buildToggleButtonPage,
    "ToolTip" to ::buildToolTipPage,
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
    "Date & time" to listOf(
        "CalendarDatePicker",
        "CalendarView",
        "DatePicker",
        "TimePicker",
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
    "Scrolling" to listOf(
        "AnnotatedScrollBar",
        "PipsPager",
        "ScrollView",
        "ScrollViewer",
        "SemanticZoom",
    ),
    "Shell" to listOf(
        "AppNotification",
        "BadgeNotification",
        "JumpList",
    ),
    "Status & info" to listOf(
        "InfoBadge",
        "InfoBar",
        "ProgressBar",
        "ProgressRing",
        "ToolTip",
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
    "Date & time" to Symbol.CALENDAR,
    "Collections" to Symbol.LIST,
    "Dialogs & flyouts" to Symbol.COMMENT,
    "Layout" to Symbol.VIEW_ALL,
    "Menus & toolbars" to Symbol.SAVE,
    "Navigation" to Symbol.GLOBAL_NAVIGATION_BUTTON,
    "Scrolling" to Symbol.ZOOM,
    "Shell" to Symbol.MESSAGE,
    "Status & info" to Symbol.IMPORTANT,
    "Text" to Symbol.FONT,
    "Windowing" to Symbol.NEW_WINDOW,
)

/**
 * [buildGalleryNavigationView]'s return value: the navigation itself, plus a "page name ->
 * WNavigationViewItem" lookup used to sync selection for the back button.
 */
internal class GalleryNavigation(
    val navigationView: WNavigationView,
    val homeItem: WNavigationViewItem,
    val settingsItem: WNavigationViewItem,
    val itemsByPageName: Map<String, WNavigationViewItem>,
)

/**
 * The left-hand navigation. Puts Home first, then lines up page items under each category's
 * parent item (with an icon) below it. Selecting Home passes control to [onHome]; selecting a
 * page passes the selected page's name and builder to [onSelect].
 */
internal fun buildGalleryNavigationView(
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onSelect: (String, () -> WComponent) -> Unit,
): GalleryNavigation {
    val navigationView = WNavigationView()
    // Show Settings (gear) at the pane's bottom-left, matching the real Gallery. Not using the
    // built-in Settings entry since its label gets localized to the OS's language ("Settings" ->
    // something else), so a self-built English footer item is used instead
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

    val settingsItem = WNavigationViewItem(SETTINGS_PAGE_NAME, Symbol.SETTING)
    navigationView.addFooterItem(settingsItem)

    navigationView.addSelectionListener { item ->
        when {
            item == null -> return@addSelectionListener
            item == homeItem -> onHome()
            item == settingsItem -> onSettings()
            else -> pages[item.text]?.let { buildPage -> onSelect(item.text, buildPage) }
        }
    }
    return GalleryNavigation(navigationView, homeItem, settingsItem, itemsByPageName)
}
