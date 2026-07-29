package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.NavigationViewBackButtonVisible;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WNavigationView;
import com.appkitbox.winui4k.WNavigationViewItem;
import kotlin.Unit;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * The return value of {@link #buildGalleryNavigationView}: the navigation view itself, plus
 * WNavigationViewItem" lookup used to sync selection for the back button.
 */
class GalleryNavigation {
    final WNavigationView navigationView;
    final WNavigationViewItem homeItem;
    final WNavigationViewItem settingsItem;
    final Map<String, WNavigationViewItem> itemsByPageName;

    GalleryNavigation(
            WNavigationView navigationView,
            WNavigationViewItem homeItem,
            WNavigationViewItem settingsItem,
            Map<String, WNavigationViewItem> itemsByPageName) {
        this.navigationView = navigationView;
        this.homeItem = homeItem;
        this.settingsItem = settingsItem;
        this.itemsByPageName = itemsByPageName;
    }

    /** Page name (WinUI control name) -> the function that builds its demo page. */
    static final Map<String, Supplier<WComponent>> pages = new LinkedHashMap<>();

    static {
        pages.put("AppBarButton", MenusToolbarsPages::buildAppBarButtonPage);
        pages.put("AppBarSeparator", MenusToolbarsPages::buildAppBarSeparatorPage);
        pages.put("AppBarToggleButton", MenusToolbarsPages::buildAppBarToggleButtonPage);
        pages.put("AutoSuggestBox", TextPages::buildAutoSuggestBoxPage);
        pages.put("AppNotification", ShellPages::buildAppNotificationPage);
        pages.put("BadgeNotification", ShellPages::buildBadgeNotificationPage);
        pages.put("Border", LayoutPages::buildBorderPage);
        pages.put("BreadcrumbBar", NavigationPages::buildBreadcrumbBarPage);
        pages.put("Button", BasicInputPages::buildButtonPage);
        pages.put("CalendarDatePicker", DateTimePages::buildCalendarDatePickerPage);
        pages.put("CalendarView", DateTimePages::buildCalendarViewPage);
        pages.put("Canvas", LayoutPages::buildCanvasPage);
        pages.put("CheckBox", BasicInputPages::buildCheckBoxPage);
        pages.put("ColorPicker", BasicInputPages::buildColorPickerPage);
        pages.put("ComboBox", BasicInputPages::buildComboBoxPage);
        pages.put("CommandBar", MenusToolbarsPages::buildCommandBarPage);
        pages.put("CommandBarFlyout", MenusToolbarsPages::buildCommandBarFlyoutPage);
        pages.put("ContentDialog", DialogsFlyoutsPages::buildContentDialogPage);
        pages.put("DatePicker", DateTimePages::buildDatePickerPage);
        pages.put("DropDownButton", BasicInputPages::buildDropDownButtonPage);
        pages.put("Expander", LayoutPages::buildExpanderPage);
        pages.put("Flyout", DialogsFlyoutsPages::buildFlyoutPage);
        pages.put("Grid", LayoutPages::buildGridPage);
        pages.put("HyperlinkButton", BasicInputPages::buildHyperlinkButtonPage);
        pages.put("InfoBadge", StatusInfoPages::buildInfoBadgePage);
        pages.put("InfoBar", StatusInfoPages::buildInfoBarPage);
        pages.put("JumpList", ShellPages::buildJumpListPage);
        pages.put("ItemsView", CollectionsPages::buildItemsViewPage);
        pages.put("LayoutPanel", LayoutPages::buildLayoutPanelPage);
        pages.put("ListBox", CollectionsPages::buildListBoxPage);
        pages.put("ListView", CollectionsPages::buildListViewPage);
        pages.put("MenuBar", MenusToolbarsPages::buildMenuBarPage);
        pages.put("MenuFlyout", MenusToolbarsPages::buildMenuFlyoutPage);
        pages.put("NavigationView", NavigationPages::buildNavigationViewPage);
        pages.put("NumberBox", TextPages::buildNumberBoxPage);
        pages.put("PasswordBox", TextPages::buildPasswordBoxPage);
        pages.put("PipsPager", ScrollingPages::buildPipsPagerPage);
        pages.put("Popup", DialogsFlyoutsPages::buildPopupPage);
        pages.put("ProgressBar", StatusInfoPages::buildProgressBarPage);
        pages.put("ProgressRing", StatusInfoPages::buildProgressRingPage);
        pages.put("RadioButton", BasicInputPages::buildRadioButtonPage);
        pages.put("RatingControl", BasicInputPages::buildRatingControlPage);
        pages.put("RelativePanel", LayoutPages::buildRelativePanelPage);
        pages.put("RepeatButton", BasicInputPages::buildRepeatButtonPage);
        pages.put("AnnotatedScrollBar", ScrollingPages::buildAnnotatedScrollBarPage);
        pages.put("ScrollView", ScrollingPages::buildScrollViewPage);
        pages.put("ScrollViewer", ScrollingPages::buildScrollViewerPage);
        pages.put("SelectorBar", NavigationPages::buildSelectorBarPage);
        pages.put("SemanticZoom", ScrollingPages::buildSemanticZoomPage);
        pages.put("SettingsCard", LayoutPages::buildSettingsCardPage);
        pages.put("RichEditBox", TextPages::buildRichEditBoxPage);
        pages.put("RichTextBlock", TextPages::buildRichTextBlockPage);
        pages.put("Slider", BasicInputPages::buildSliderPage);
        pages.put("SplitButton", BasicInputPages::buildSplitButtonPage);
        pages.put("SplitView", LayoutPages::buildSplitViewPage);
        pages.put("StackPanel", LayoutPages::buildStackPanelPage);
        pages.put("StandardUICommand", MenusToolbarsPages::buildStandardUICommandPage);
        pages.put("SwipeControl", MenusToolbarsPages::buildSwipeControlPage);
        pages.put("TableView", CollectionsPages::buildTableViewPage);
        pages.put("TabView", NavigationPages::buildTabViewPage);
        pages.put("TeachingTip", DialogsFlyoutsPages::buildTeachingTipPage);
        pages.put("TextBlock", TextPages::buildTextBlockPage);
        pages.put("TextBox", TextPages::buildTextBoxPage);
        pages.put("TimePicker", DateTimePages::buildTimePickerPage);
        pages.put("ToggleButton", BasicInputPages::buildToggleButtonPage);
        pages.put("ToolTip", StatusInfoPages::buildToolTipPage);
        pages.put("ToggleSplitButton", BasicInputPages::buildToggleSplitButtonPage);
        pages.put("ToggleSwitch", BasicInputPages::buildToggleSwitchPage);
        pages.put("TreeView", CollectionsPages::buildTreeViewPage);
        pages.put("WebView2", MediaPages::buildWebView2Page);
        pages.put("VariableSizedWrapGrid", LayoutPages::buildVariableSizedWrapGridPage);
        pages.put("XamlUICommand", MenusToolbarsPages::buildXamlUICommandPage);
        pages.put("AppWindow", WindowingPages::buildAppWindowPage);
        pages.put("AppWindowTitleBar", WindowingPages::buildAppWindowTitleBarPage);
        pages.put("Multiple windows", WindowingPages::buildMultipleWindowsPage);
        pages.put("SystemBackdrop", WindowingPages::buildSystemBackdropPage);
        pages.put("TitleBar", WindowingPages::buildTitleBarPage);
    }

    /** Navigation categories (matching the real WinUI 3 Gallery's grouping) -> the page names in each. */
    private static final Map<String, List<String>> categories = new LinkedHashMap<>();

    static {
        categories.put("Basic input", Arrays.asList(
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
                "ToggleSwitch"));
        categories.put("Date & time", Arrays.asList(
                "CalendarDatePicker",
                "CalendarView",
                "DatePicker",
                "TimePicker"));
        categories.put("Collections", Arrays.asList(
                "ItemsView",
                "ListBox",
                "ListView",
                "TableView",
                "TreeView"));
        categories.put("Dialogs & flyouts", Arrays.asList(
                "ContentDialog",
                "Flyout",
                "Popup",
                "TeachingTip"));
        categories.put("Layout", Arrays.asList(
                "Border",
                "Canvas",
                "Expander",
                "Grid",
                "LayoutPanel",
                "RelativePanel",
                "SettingsCard",
                "SplitView",
                "StackPanel",
                "VariableSizedWrapGrid"));
        categories.put("Media", Arrays.asList(
                "WebView2"));
        categories.put("Menus & toolbars", Arrays.asList(
                "AppBarButton",
                "AppBarSeparator",
                "AppBarToggleButton",
                "CommandBar",
                "CommandBarFlyout",
                "MenuBar",
                "MenuFlyout",
                "SwipeControl",
                "StandardUICommand",
                "XamlUICommand"));
        categories.put("Navigation", Arrays.asList(
                "BreadcrumbBar",
                "NavigationView",
                "SelectorBar",
                "TabView"));
        categories.put("Scrolling", Arrays.asList(
                "AnnotatedScrollBar",
                "PipsPager",
                "ScrollView",
                "ScrollViewer",
                "SemanticZoom"));
        categories.put("Shell", Arrays.asList(
                "AppNotification",
                "BadgeNotification",
                "JumpList"));
        categories.put("Status & info", Arrays.asList(
                "InfoBadge",
                "InfoBar",
                "ProgressBar",
                "ProgressRing",
                "ToolTip"));
        categories.put("Text", Arrays.asList(
                "AutoSuggestBox",
                "NumberBox",
                "PasswordBox",
                "RichEditBox",
                "RichTextBlock",
                "TextBlock",
                "TextBox"));
        categories.put("Windowing", Arrays.asList(
                "AppWindow",
                "AppWindowTitleBar",
                "Multiple windows",
                "SystemBackdrop",
                "TitleBar"));
    }

    /** Category name -> the icon shown to the left of the category name in the navigation. */
    private static final Map<String, Symbol> categoryIcons = new LinkedHashMap<>();

    static {
        categoryIcons.put("Basic input", Symbol.KEYBOARD);
        categoryIcons.put("Date & time", Symbol.CALENDAR);
        categoryIcons.put("Collections", Symbol.LIST);
        categoryIcons.put("Dialogs & flyouts", Symbol.COMMENT);
        categoryIcons.put("Layout", Symbol.VIEW_ALL);
        categoryIcons.put("Media", Symbol.GLOBE);
        categoryIcons.put("Menus & toolbars", Symbol.SAVE);
        categoryIcons.put("Navigation", Symbol.GLOBAL_NAVIGATION_BUTTON);
        categoryIcons.put("Scrolling", Symbol.ZOOM);
        categoryIcons.put("Shell", Symbol.MESSAGE);
        categoryIcons.put("Status & info", Symbol.IMPORTANT);
        categoryIcons.put("Text", Symbol.FONT);
        categoryIcons.put("Windowing", Symbol.NEW_WINDOW);
    }

    /**
     * The left-hand navigation. Puts Home first, then lines up page items under each category's
     * lists the page items. Selecting Home calls {@code onHome} and selecting a page calls
     * page passes the selected page's name and builder to [onSelect].
     */
    static GalleryNavigation buildGalleryNavigationView(
            Runnable onHome,
            Runnable onSettings,
            BiConsumer<String, Supplier<WComponent>> onSelect) {
        WNavigationView navigationView = new WNavigationView();
        // Puts Settings (a gear) at the bottom-left of the pane, same as the official Gallery. The built-in
        // settings item is not used because its label is localized to the OS language, so an English footer item is added instead
        navigationView.setSettingsVisible(false);
        navigationView.setBackButtonVisible(NavigationViewBackButtonVisible.COLLAPSED);
        navigationView.setOpenPaneLength(260.0);

        // Put Home first, matching the real Gallery
        WNavigationViewItem homeItem = new WNavigationViewItem("Home", Symbol.HOME);
        navigationView.addItem(homeItem);

        Map<String, WNavigationViewItem> itemsByPageName = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            String category = entry.getKey();
            // Categories aren't selectable (SelectsOnInvoked=false); they only toggle their children open/closed
            WNavigationViewItem categoryItem = new WNavigationViewItem(category, categoryIcons.get(category));
            categoryItem.setSelectsOnInvoked(false);
            categoryItem.setExpanded(false); // keep every category collapsed at startup
            for (String name : entry.getValue()) {
                WNavigationViewItem pageItem = new WNavigationViewItem(name, null);
                categoryItem.addItem(pageItem);
                itemsByPageName.put(name, pageItem);
            }
            navigationView.addItem(categoryItem);
        }

        WNavigationViewItem settingsItem = new WNavigationViewItem(SettingsPage.SETTINGS_PAGE_NAME, Symbol.SETTING);
        navigationView.addFooterItem(settingsItem);

        navigationView.addSelectionListener(item -> {
            if (item == null) {
                return Unit.INSTANCE;
            }
            if (item == homeItem) {
                onHome.run();
            } else if (item == settingsItem) {
                onSettings.run();
            } else {
                Supplier<WComponent> buildPage = pages.get(item.getText());
                if (buildPage != null) {
                    onSelect.accept(item.getText(), buildPage);
                }
            }
            return Unit.INSTANCE;
        });
        return new GalleryNavigation(navigationView, homeItem, settingsItem, itemsByPageName);
    }
}
