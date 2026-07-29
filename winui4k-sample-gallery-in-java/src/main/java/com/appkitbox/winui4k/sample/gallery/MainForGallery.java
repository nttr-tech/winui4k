package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.ElementTheme;
import com.appkitbox.winui4k.GridLength;
import com.appkitbox.winui4k.NavigationViewPaneDisplayMode;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.SystemBackdropType;
import com.appkitbox.winui4k.TextChangeReason;
import com.appkitbox.winui4k.TitleBarHeightOption;
import com.appkitbox.winui4k.TitleBarTheme;
import com.appkitbox.winui4k.WAutoSuggestBox;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WGrid;
import com.appkitbox.winui4k.WNavigationView;
import com.appkitbox.winui4k.WNavigationViewItem;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WTitleBar;
import com.appkitbox.winui4k.WinUiUtilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A WinUI 3 Gallery-style component gallery.
 * Shows a page navigation list on the left and the selected component's demo page on the right.
 * As pages are added, register them in {@link GalleryNavigation#pages}.
 */
public class MainForGallery {
    private MainForGallery() {
    }

    public static void main(String[] args) {
        WinUiUtilities.INSTANCE.invokeLater(() -> {
            WFrame frame = new WFrame("WinUI4K Gallery");

            // The root that hosts the title bar and navigation. Settings' App theme is set here as
            // RequestedTheme and applies to every element in the window
            WGrid rootGrid = new WGrid(0.0, 0.0);

            // Applies the app theme and title bar coloring saved on the Settings page
            Consumer<String> applyAppTheme = appTheme -> {
                ElementTheme theme = elementThemeOf(appTheme);
                rootGrid.setRequestedTheme(theme);
                TitleBarTheme titleBarTheme;
                switch (theme) {
                    case LIGHT:
                        titleBarTheme = TitleBarTheme.LIGHT;
                        break;
                    case DARK:
                        titleBarTheme = TitleBarTheme.DARK;
                        break;
                    default:
                        titleBarTheme = TitleBarTheme.USE_DEFAULT_APP_MODE;
                        break;
                }
                frame.getAppWindow().getTitleBar().setPreferredTheme(titleBarTheme);
            };
            applyAppTheme.accept(GallerySettings.getAppTheme());
            // Lets pages built from here on pick colors (CARD_BACKGROUND etc.) for the current theme
            GalleryTheme.isDarkTheme = rootGrid.getActualTheme() == ElementTheme.DARK;

            // Content gets set by the initial selection when the navigation is built
            WPanel pageArea = new WPanel(0.0, Orientation.VERTICAL);
            pageArea.setMargin(24.0);

            // The content area is translucent white, matching the real Gallery's Layer; the Mica behind it shows through faintly
            WBorder pageBackground = new WBorder(new WScrollPane(pageArea));
            pageBackground.setBackground(GalleryTheme.PAGE_BACKGROUND());

            // History of page names to go back through via the back button (null = the startup home state).
            // Assigning selectedItem also fires SelectionChanged, so isNavigatingBack marks "currently going
            // back" to avoid re-pushing onto the history.
            Deque<String> history = new LinkedList<>();
            boolean[] isNavigatingBack = {false};
            String[] currentPageName = {null};

            WTitleBar titleBar = new WTitleBar();
            titleBar.setTitle("WinUI4K Gallery");
            titleBar.setPaneToggleButtonVisible(true);
            // Don't show the back button until a page is selected (the real Gallery can't go back from home)
            titleBar.setBackButtonVisible(false);

            // The home state shown when Home is selected in the navigation, and when going back through the whole history
            GalleryNavigation[] navigation = new GalleryNavigation[1];
            Runnable showHome = () -> {
                currentPageName[0] = null;
                titleBar.setBackButtonVisible(!history.isEmpty());
                // Home shows the hero image flush to the edges, so no margin here (each page uses 24)
                pageArea.setMargin(0.0);
                pageArea.removeAll();
                pageArea.add(HomePage.buildHomePage(name -> {
                    WNavigationViewItem item = navigation[0].itemsByPageName.get(name);
                    if (item != null) {
                        navigation[0].navigationView.setSelectedItem(item);
                    }
                }));
            };

            // The Settings page shown when the pane's bottom-left Settings (gear) item is selected
            Runnable showSettings = () -> {
                if (!isNavigatingBack[0] && !SettingsPage.SETTINGS_PAGE_NAME.equals(currentPageName[0])) {
                    history.addLast(currentPageName[0]);
                }
                currentPageName[0] = SettingsPage.SETTINGS_PAGE_NAME;
                titleBar.setBackButtonVisible(true);
                pageArea.setMargin(24.0);
                pageArea.removeAll();
                pageArea.add(SettingsPage.buildSettingsPage(navigation[0].navigationView, applyAppTheme));
            };

            // Set the same icon as the real WinUI 3 Gallery on the title bar and taskbar
            File iconFile = extractGalleryIcon();
            if (iconFile != null) {
                titleBar.setIconUri(iconFile.toPath().toUri().toString());
                frame.getAppWindow().setIcon(iconFile.getAbsolutePath());
            }

            // Puts a page search box in the center of the title bar, same as the official Gallery (the width of 580 matches its MaxWidth too)
            WAutoSuggestBox searchBox = new WAutoSuggestBox("Search controls and samples...");
            searchBox.setWidth(580.0);
            searchBox.setQueryIcon(Symbol.FIND); // the same magnifier as the official QueryIcon="Find"
            titleBar.setContent(searchBox);

            navigation[0] = GalleryNavigation.buildGalleryNavigationView(
                    () -> {
                        if (!isNavigatingBack[0] && currentPageName[0] != null) {
                            history.addLast(currentPageName[0]); // push onto the history so Home can be reached back from a page
                        }
                        showHome.run();
                    },
                    showSettings,
                    (name, buildPage) -> {
                        if (!isNavigatingBack[0]) {
                            history.addLast(currentPageName[0]); // a transition from Home pushes null
                        }
                        currentPageName[0] = name;
                        GallerySettings.addRecentlyVisited(name); // list it in Home's Recently visited
                        titleBar.setBackButtonVisible(true);
                        pageArea.setMargin(24.0);
                        pageArea.removeAll();
                        pageArea.add(buildPage.get());
                        // The TitleBar page's demo creates a separate WTitleBar for illustration. WinUI 3's TitleBar control
                        // overwrites the ancestor window's real title (Window.Title / AppWindow.Title) with its own Title
                        // on Loaded (an asynchronous point after this callback), so it is scheduled once more after the
                        // layout settles to restore the main title
                        frame.setTitle("WinUI4K Gallery");
                        WinUiUtilities.INSTANCE.schedule(200, () -> {
                            frame.setTitle("WinUI4K Gallery");
                        });
                    });
            WNavigationView navigationView = navigation[0].navigationView;
            navigationView.setContent(pageBackground);
            // Consolidate the pane-toggle button onto the title bar to avoid showing it twice
            navigationView.setPaneToggleButtonVisible(false);
            // Restore the navigation placement (Left / Top) saved on the Settings page
            if ("Top".equals(GallerySettings.getNavigationStyle())) {
                navigationView.setPaneDisplayMode(NavigationViewPaneDisplayMode.TOP);
            }

            titleBar.addBackRequestedListener(() -> {
                if (history.isEmpty()) {
                }
                String previousName = history.removeLast();
                // null means home; go through SelectionChanged to show the right page (or Home / Settings)
                WNavigationViewItem previousItem;
                if (previousName == null) {
                    previousItem = navigation[0].homeItem;
                } else if (previousName.equals(SettingsPage.SETTINGS_PAGE_NAME)) {
                    previousItem = navigation[0].settingsItem;
                } else {
                    previousItem = navigation[0].itemsByPageName.get(previousName);
                    if (previousItem == null) {
                    }
                }
                isNavigatingBack[0] = true;
                navigationView.setSelectedItem(previousItem);
                isNavigatingBack[0] = false;
            });
            titleBar.addPaneToggleRequestedListener(() -> {
                navigationView.setPaneOpen(!navigationView.isPaneOpen());
            });

            // Narrow down page names as the search box is typed into, and navigate to the chosen page on submit
            List<String> pageNames = new ArrayList<>(GalleryNavigation.pages.keySet());
            searchBox.addTextChangedListener((text, reason) -> {
                if (reason == TextChangeReason.USER_INPUT) {
                    List<String> suggestions = new ArrayList<>();
                    for (String name : pageNames) {
                        if (containsIgnoreCase(name, text)) {
                            suggestions.add(name);
                        }
                    }
                    searchBox.setSuggestions(suggestions);
                }
            });
            searchBox.addQuerySubmittedListener((query, chosen) -> {
                String target = chosen;
                if (target == null) {
                    for (String name : pageNames) {
                        if (containsIgnoreCase(name, query)) {
                            target = name;
                            break;
                        }
                    }
                }
                WNavigationViewItem item = target != null ? navigation[0].itemsByPageName.get(target) : null;
                if (item != null) {
                    navigationView.setSelectedItem(item);
                    searchBox.setText("");
                }
            });

            rootGrid.addRow(GridLength.Companion.getAUTO());
            rootGrid.addRow(GridLength.Companion.star(1.0));
            rootGrid.add(titleBar, 0, 0, 1, 1);
            rootGrid.add(navigationView, 1, 0, 1, 1);

            // When the theme changes, rebuild the visible page to re-pick colors (CARD_BACKGROUND etc.).
            // To avoid destroying the source of the change (the Settings page's combo box) while it's
            // still handling its own event, defer the rebuild onto the message loop
            rootGrid.addActualThemeChangedListener(() -> {
                GalleryTheme.isDarkTheme = rootGrid.getActualTheme() == ElementTheme.DARK;
                pageBackground.setBackground(GalleryTheme.PAGE_BACKGROUND());
                WinUiUtilities.INSTANCE.invokeLater(() -> {
                    String name = currentPageName[0];
                    if (name == null) {
                        showHome.run();
                    } else if (name.equals(SettingsPage.SETTINGS_PAGE_NAME)) {
                        showSettings.run();
                    } else {
                        pageArea.removeAll();
                        pageArea.add(GalleryNavigation.pages.get(name).get());
                    }
                });
            });

            // Select Home on launch (SelectionChanged -> showHome)
            navigationView.setSelectedItem(navigation[0].homeItem);

            frame.setContentPane(rootGrid);
            frame.setExtendsContentIntoTitleBar(true);
            frame.setTitleBar(titleBar);
            frame.getAppWindow().getTitleBar().setPreferredHeightOption(TitleBarHeightOption.TALL);
            // Mica, matching the real Gallery, lets the wallpaper's color (a pale blue with the default wallpaper) show through faintly across the whole window
            frame.setSystemBackdrop(SystemBackdropType.MICA);
            frame.setVisible(true);
        });
    }

    /** Whether {@code text} contains {@code part} ignoring case (the equivalent of Kotlin's contains(ignoreCase = true)). */
    private static boolean containsIgnoreCase(String text, String part) {
        return text.toLowerCase(Locale.ROOT).contains(part.toLowerCase(Locale.ROOT));
    }

    /** Converts the app theme saved in Settings ("Light" / "Dark" / "Default") into an {@link ElementTheme}. */
    private static ElementTheme elementThemeOf(String appTheme) {
        if ("Light".equals(appTheme)) {
            return ElementTheme.LIGHT;
        }
        if ("Dark".equals(appTheme)) {
            return ElementTheme.DARK;
        }
        return ElementTheme.DEFAULT;
    }

    /**
     * Extracts the same icon as the real WinUI 3 Gallery (GalleryIcon.ico) from resources to a temp file.
     * WinUI's BitmapImage / AppWindow.SetIcon only accept a URI or file path.
     */
    private static File extractGalleryIcon() {
        InputStream resource = MainForGallery.class.getResourceAsStream("/GalleryIcon.ico");
        if (resource == null) {
            return null;
        }
        try {
            File file = File.createTempFile("winui4k-sample-gallery-icon-", ".ico");
            file.deleteOnExit();
            try (InputStream input = resource; OutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
