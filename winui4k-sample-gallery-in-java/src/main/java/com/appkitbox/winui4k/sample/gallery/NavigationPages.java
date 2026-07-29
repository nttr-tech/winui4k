package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.NavigationViewBackButtonVisible;
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WBreadcrumbBar;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WNavigationView;
import com.appkitbox.winui4k.WNavigationViewItem;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WSelectorBar;
import com.appkitbox.winui4k.WSelectorBarItem;
import com.appkitbox.winui4k.WTabView;
import com.appkitbox.winui4k.WTabViewItem;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;


/*
 * Navigation category: demo pages for NavigationView.
 */
final class NavigationPages {
    private NavigationPages() {
    }

    // region NavigationView

    /** The NavigationView page: lines up demos for trying out WNavigationView's various features. */
    static WComponent buildNavigationViewPage() {
        WPanel page = GalleryScaffold.buildPage(
            "NavigationView",
            "A control that provides an app's top-level navigation. Try out WNavigationView's various features.");

        page.add(buildSimpleNavigationViewExample());
        page.add(buildNavigationViewPaneExample());
        page.add(buildHierarchicalNavigationViewExample());
        return page;
    }

    /** Basic navigation: items with icons, and responding to selection changes (SelectionChanged). */
    private static WComponent buildSimpleNavigationViewExample() {
        WLabel contentLabel = new WLabel("Showing Home");
        contentLabel.setMargin(16.0);

        WNavigationView navigationView = new WNavigationView();
        navigationView.setWidth(480.0);
        navigationView.setHeight(280.0);
        navigationView.setOpenPaneLength(160.0);
        // The demo is narrower than the Auto mode threshold, so always show the left pane
        navigationView.setPaneDisplayMode(NavigationViewPaneDisplayMode.LEFT);
        navigationView.setSettingsVisible(false);
        navigationView.setBackButtonVisible(NavigationViewBackButtonVisible.COLLAPSED);
        navigationView.setContent(contentLabel);

        WNavigationViewItem home = new WNavigationViewItem("Home", Symbol.HOME);
        navigationView.addItem(home);
        navigationView.addItem(new WNavigationViewItem("Mail", Symbol.MAIL));
        navigationView.addItem(new WNavigationViewItem("Calendar", Symbol.CALENDAR));
        navigationView.addFooterItem(new WNavigationViewItem("Help", Symbol.HELP));

        navigationView.addSelectionListener(item -> {
            if (item != null) {
                contentLabel.setText("Showing " + item.getText());
            }
        });
        navigationView.setSelectedItem(home);

        return GalleryScaffold.buildExample("Simple navigation (MenuItems / Icon / SelectionChanged)", navigationView);
    }

    /** Controlling the pane: open/close, title, placement, and showing the settings item. */
    private static WComponent buildNavigationViewPaneExample() {
        WNavigationView navigationView = new WNavigationView();
        navigationView.setWidth(480.0);
        navigationView.setHeight(280.0);
        navigationView.setOpenPaneLength(180.0);
        navigationView.setPaneDisplayMode(NavigationViewPaneDisplayMode.LEFT);
        navigationView.setPaneTitle("Menu");
        navigationView.setBackButtonVisible(NavigationViewBackButtonVisible.COLLAPSED);
        WLabel contentLabel = new WLabel("Content area");
        contentLabel.setMargin(16.0);
        navigationView.setContent(contentLabel);

        navigationView.addItem(new WNavigationViewItem("Documents", Symbol.DOCUMENT));
        navigationView.addItem(new WNavigationViewItem("Pictures", Symbol.PICTURES));
        navigationView.addItem(new WNavigationViewItem("Music", Symbol.AUDIO));

        WButton toggleButton = new WButton("Toggle pane");
        toggleButton.addActionListener(() -> {
            navigationView.setPaneOpen(!navigationView.isPaneOpen());
        });

        WButton settingsButton = new WButton("Toggle settings item");
        settingsButton.addActionListener(() -> {
            navigationView.setSettingsVisible(!navigationView.isSettingsVisible());
        });

        WPanel modeButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        modeButtons.add(new WLabel("PaneDisplayMode:"));
        for (NavigationViewPaneDisplayMode mode : NavigationViewPaneDisplayMode.values()) {
            WButton button = new WButton(mode.name());
            button.addActionListener(() -> {
                navigationView.setPaneDisplayMode(mode);
            });
            modeButtons.add(button);
        }

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(toggleButton);
        buttons.add(settingsButton);

        WPanel body = new WPanel(8.0);
        body.add(navigationView);
        body.add(buttons);
        body.add(modeButtons);
        return GalleryScaffold.buildExample("Controlling the pane (IsPaneOpen / PaneTitle / PaneDisplayMode / IsSettingsVisible)", body);
    }

    /** A hierarchical menu: nests child items under a parent item, and also responds to clicks (ItemInvoked). */
    private static WComponent buildHierarchicalNavigationViewExample() {
        WLabel result = new WLabel("Clicked: none");

        WNavigationView navigationView = new WNavigationView();
        navigationView.setWidth(480.0);
        navigationView.setHeight(280.0);
        navigationView.setOpenPaneLength(180.0);
        navigationView.setPaneDisplayMode(NavigationViewPaneDisplayMode.LEFT);
        navigationView.setSettingsVisible(false);
        navigationView.setBackButtonVisible(NavigationViewBackButtonVisible.COLLAPSED);
        WLabel contentLabel = new WLabel("Content area");
        contentLabel.setMargin(16.0);
        navigationView.setContent(contentLabel);

        navigationView.addItem(new WNavigationViewItem("Home", Symbol.HOME));

        // The parent item isn't selectable (SelectsOnInvoked=false); it only toggles its children open/closed
        WNavigationViewItem documents = new WNavigationViewItem("Documents", Symbol.FOLDER);
        documents.setSelectsOnInvoked(false);
        documents.setExpanded(true);
        documents.addItem(new WNavigationViewItem("Specs"));
        documents.addItem(new WNavigationViewItem("Meeting notes"));
        navigationView.addItem(documents);

        navigationView.addItemInvokedListener(name -> {
            result.setText("Clicked: " + name);
        });

        WPanel body = new WPanel(8.0);
        body.add(navigationView);
        body.add(result);
        return GalleryScaffold.buildExample("A hierarchical menu and clicks (nested MenuItems / IsExpanded / ItemInvoked)", body);
    }

    // endregion

    // region BreadcrumbBar page

    /** The BreadcrumbBar page: lines up demos for trying out WBreadcrumbBar's hierarchy display and click-to-navigate. */
    static WComponent buildBreadcrumbBarPage() {
        WPanel page = GalleryScaffold.buildPage(
            "BreadcrumbBar",
            "A breadcrumb trail that shows your current position in a hierarchy and lets you jump back up it. Try out WBreadcrumbBar's various features.");

        page.add(buildBreadcrumbBarClickExample());
        return page;
    }

    /** Truncating the breadcrumb by clicking a level (ItemClicked). */
    private static WComponent buildBreadcrumbBarClickExample() {
        List<String> fullPath = Arrays.asList("Home", "Documents", "2026", "Report");
        WLabel result = new WLabel("Click a level to go back to it");

        WBreadcrumbBar breadcrumbBar = new WBreadcrumbBar();
        breadcrumbBar.setItems(fullPath);
        breadcrumbBar.addItemClickedListener(index -> {
            breadcrumbBar.setItems(fullPath.subList(0, index + 1));
            result.setText("Clicked: " + fullPath.get(index) + " (index " + index + ")");
        });

        WButton resetButton = new WButton("Reset the hierarchy");
        resetButton.addActionListener(() -> {
            breadcrumbBar.setItems(fullPath);
            result.setText("Hierarchy reset");
        });

        WPanel body = new WPanel(8.0);
        body.add(breadcrumbBar);
        body.add(result);
        body.add(resetButton);
        return GalleryScaffold.buildExample("Displaying and clicking the hierarchy (ItemsSource / ItemClicked)", body);
    }

    // endregion

    // region SelectorBar page

    /** The SelectorBar page: lines up demos for trying out WSelectorBar's selection switching. */
    static WComponent buildSelectorBarPage() {
        WPanel page = GalleryScaffold.buildPage(
            "SelectorBar",
            "A control for switching between a small number of options. Try out WSelectorBar's various features.");

        page.add(buildSelectorBarSelectionExample());
        return page;
    }

    /** Switching the selection (SelectionChanged) and setting SelectedItem. */
    private static WComponent buildSelectorBarSelectionExample() {
        WLabel result = new WLabel("Showing Recent");

        WSelectorBar selectorBar = new WSelectorBar();
        List<String> labels = Arrays.asList("Recent", "Share", "Favorite");
        for (String label : labels) {
            selectorBar.addItem(new WSelectorBarItem(label));
        }
        selectorBar.addSelectionListener(index -> {
            if (index >= 0) {
                result.setText("Showing " + labels.get(index));
            }
        });
        selectorBar.setSelectedIndex(0);

        WPanel body = new WPanel(8.0);
        body.add(selectorBar);
        body.add(result);
        return GalleryScaffold.buildExample("Switching the selection (Items / SelectedItem / SelectionChanged)", body);
    }

    // endregion

    // region TabView page

    /** The TabView page: lines up demos for trying out WTabView's tab management. */
    static WComponent buildTabViewPage() {
        WPanel page = GalleryScaffold.buildPage(
            "TabView",
            "A control that switches between multiple pages via tabs. Try out WTabView's various features.");

        page.add(buildTabViewBasicExample());
        return page;
    }

    /** Adding, removing, and switching tabs (TabItems / AddTabButtonClick / TabCloseRequested / SelectionChanged). */
    private static WComponent buildTabViewBasicExample() {
        WLabel result = new WLabel("Selecting Tab 1");

        WTabView tabView = new WTabView();
        tabView.setWidth(480.0);
        tabView.setHeight(240.0);
        int[] tabNumber = {0};

        Supplier<WTabViewItem> newTab = () -> {
            tabNumber[0]++;
            WLabel content = new WLabel("Tab " + tabNumber[0] + "'s content");
            content.setMargin(16.0);
            WTabViewItem tab = new WTabViewItem("Tab " + tabNumber[0]);
            tab.setContent(content);
            return tab;
        };

        for (int i = 0; i < 3; i++) {
            tabView.addTab(newTab.get());
        }
        tabView.setSelectedIndex(0);

        // The "+" button adds a tab and the close button removes one (TabView does not close tabs automatically, so removeTab is called)
        tabView.addAddTabButtonClickListener(() -> {
            tabView.addTab(newTab.get());
            tabView.setSelectedIndex(tabView.getTabCount() - 1);
        });
        tabView.addTabCloseRequestedListener(index -> {
            if (tabView.getTabCount() > 1) {
                tabView.removeTab(index);
            }
        });
        tabView.addSelectionListener(() -> {
            int index = tabView.getSelectedIndex();
            if (index >= 0) {
                result.setText(tabView.getTab(index).getHeader());
            }
        });

        WPanel body = new WPanel(8.0);
        body.add(tabView);
        body.add(result);
        return GalleryScaffold.buildExample("Adding, removing, and switching tabs (TabItems / AddTabButtonClick / TabCloseRequested)", body);
    }

    // endregion
}
