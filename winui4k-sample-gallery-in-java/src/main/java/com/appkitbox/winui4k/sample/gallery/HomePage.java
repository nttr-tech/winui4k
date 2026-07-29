package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.GridLength;
import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.ScrollBarVisibility;
import com.appkitbox.winui4k.Stretch;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WGrid;
import com.appkitbox.winui4k.WGradientStop;
import com.appkitbox.winui4k.WImage;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WLinearGradientPaint;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WToggleButton;
import com.appkitbox.winui4k.WVariableSizedWrapGrid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/*
 * Equivalent of the real WinUI 3 Gallery's Home page.
 * A header with a hero image, tiles linking out, and a Recent / Favorites toggle view below it.
 * "Recently visited" and "Favorites" are persisted through {@link GallerySettings}.
 */
final class HomePage {
    private HomePage() {
    }

    /** The header's background color (a representative color from the real light theme's #CED8E4-#D5DBE3 gradient; dark uses a darker blue). */
    private static WColor HEADER_BACKGROUND() {
        return GalleryTheme.isDarkTheme ? new WColor(26, 34, 48, 255) : new WColor(206, 216, 228, 255);
    }

    /** Recently added/updated pages (equivalent to the real Gallery's IsNew / IsUpdated). Swap these out with each release. */
    private static final List<String> recentlyAddedOrUpdatedPages = Arrays.asList(
        "ListBox",
        "AppWindow",
        "AppWindowTitleBar",
        "Multiple windows",
        "SystemBackdrop",
        "TitleBar");

    /** Descriptions shown on Home's cards for each page (equivalent to the real Gallery's ControlInfoData.json Description). */
    static final Map<String, String> pageDescriptions = buildPageDescriptions();

    private static Map<String, String> buildPageDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("AppBarButton", "An icon button placed on a command bar.");
        descriptions.put("AppBarSeparator", "A vertical line that divides command bar buttons into groups.");
        descriptions.put("AppBarToggleButton", "A button placed on a command bar that toggles on/off.");
        descriptions.put("AutoSuggestBox", "A text box that shows suggestions as you type.");
        descriptions.put("AppNotification", "Sends a toast notification that arrives in the action center.");
        descriptions.put("BadgeNotification", "Shows a numeric or glyph badge on the taskbar icon.");
        descriptions.put("Border", "A container that draws a border, background, and rounded corners around a single child.");
        descriptions.put("BreadcrumbBar", "A breadcrumb trail that shows your current position in a hierarchy and lets you jump back up it.");
        descriptions.put("Button", "A button that responds to clicks.");
        descriptions.put("Canvas", "A panel that positions children with absolute coordinates.");
        descriptions.put("CheckBox", "A check box that toggles between three states: on, off, and indeterminate.");
        descriptions.put("ColorPicker", "A control for choosing a color via a spectrum or sliders.");
        descriptions.put("ComboBox", "A control for choosing an item from a drop-down list.");
        descriptions.put("CommandBar", "A toolbar that groups an app's commands together.");
        descriptions.put("CommandBarFlyout", "Shows commands floating near the current selection.");
        descriptions.put("ContentDialog", "A modal dialog that waits for a user response.");
        descriptions.put("DropDownButton", "A button with a chevron that opens a menu on click.");
        descriptions.put("Expander", "A control that expands/collapses its content when the header is clicked.");
        descriptions.put("Flyout", "Shows content in a lightweight popup.");
        descriptions.put("Grid", "A layout panel that places children into cells laid out in rows and columns.");
        descriptions.put("HyperlinkButton", "A button styled as a link that responds to clicks.");
        descriptions.put("InfoBadge", "A small badge that unobtrusively shows an unread count or draws attention.");
        descriptions.put("InfoBar", "An inline notification bar that reports an in-app state change.");
        descriptions.put("JumpList", "Adds items to the taskbar's right-click menu.");
        descriptions.put("ListBox", "A control for selecting an item from an always-visible list.");
        descriptions.put("ListView", "A list that lines items up vertically for selection.");
        descriptions.put("MenuBar", "A menu that runs along the top of a window.");
        descriptions.put("MenuFlyout", "A transient menu such as a context menu.");
        descriptions.put("NavigationView", "Provides an app's top-level navigation.");
        descriptions.put("NumberBox", "Lets you enter a number and adjust it with spin buttons.");
        descriptions.put("PasswordBox", "A password input field that hides the typed characters.");
        descriptions.put("Popup", "Shows content floating at an arbitrary position.");
        descriptions.put("ProgressBar", "Shows a task's progress as a bar (indeterminate display supported too).");
        descriptions.put("ProgressRing", "Shows an ongoing operation or progress as a circular ring.");
        descriptions.put("RadioButton", "A button that lets you select just one option from a group.");
        descriptions.put("RatingControl", "A control for entering and displaying a rating as a number of stars.");
        descriptions.put("RelativePanel", "A panel that lays children out relative to one another.");
        descriptions.put("RepeatButton", "A button that repeatedly fires its click event while held down.");
        descriptions.put("AnnotatedScrollBar", "A control that adds annotations to a vertical scrollbar to make large collections easier to navigate.");
        descriptions.put("PipsPager", "Lets you page through content without displaying explicit page numbers.");
        descriptions.put("ScrollView", "A container control that lets you pan and zoom its content.");
        descriptions.put("ScrollViewer", "A container control that lets you pan and zoom its content.");
        descriptions.put("SelectorBar", "A control for switching between a small number of options.");
        descriptions.put("SemanticZoom", "Switches between two zoomed views of a collection to make large numbers of items easier to navigate.");
        descriptions.put("RichEditBox", "An input field for editing formatted text.");
        descriptions.put("RichTextBlock", "A control that displays formatted text.");
        descriptions.put("Slider", "Lets you drag a thumb to choose a value within a range.");
        descriptions.put("SplitButton", "A button that combines a default action with a menu.");
        descriptions.put("SplitView", "A container that shows a pane alongside content.");
        descriptions.put("StackPanel", "A panel that lines children up in a single vertical or horizontal row.");
        descriptions.put("StandardUICommand", "Provides standard commands such as copy or delete.");
        descriptions.put("SwipeControl", "A container that lets you run commands via a swipe gesture.");
        descriptions.put("TableView", "A table that displays data in rows and columns.");
        descriptions.put("TabView", "A control that switches between multiple pages via tabs.");
        descriptions.put("TeachingTip", "Shows a hint that points to and explains a specific piece of UI.");
        descriptions.put("TextBlock", "Displays read-only text.");
        descriptions.put("TextBox", "Lets you enter single-line or multi-line text.");
        descriptions.put("ToggleButton", "A button that toggles between an on and off state.");
        descriptions.put("ToggleSplitButton", "A button that combines an on/off toggle with a menu.");
        descriptions.put("ToggleSwitch", "A switch that toggles between on and off.");
        descriptions.put("ToolTip", "Shows a floating explanation of an element on hover.");
        descriptions.put("TreeView", "A tree that can expand and collapse hierarchical data.");
        descriptions.put("VariableSizedWrapGrid", "A panel that wraps children in cell-sized units.");
        descriptions.put("XamlUICommand", "A reusable command that carries a label and an icon.");
        descriptions.put("AppWindow", "Controls a window's size, position, and presenter.");
        descriptions.put("AppWindowTitleBar", "Customizes the title bar's colors and button region.");
        descriptions.put("Multiple windows", "Creates and manages multiple windows.");
        descriptions.put("SystemBackdrop", "Applies a background material such as Mica or Acrylic.");
        descriptions.put("TitleBar", "A title bar that can host a back button and a search box.");
        return descriptions;
    }

    /** Cache of file URIs for images extracted from resources to a temp file (null if not found). */
    private static final Map<String, String> extractedImageUris = new HashMap<>();

    /**
     * Extracts a resource image (/images/{@code fileName}) to a temporary file and returns its file URI.
     * WinUI's BitmapImage only accepts a URI (returns null if not found).
     */
    static String galleryImageUri(String fileName) {
        if (extractedImageUris.containsKey(fileName)) {
            return extractedImageUris.get(fileName);
        }
        String uri = extractImageUri(fileName);
        extractedImageUris.put(fileName, uri);
        return uri;
    }

    private static String extractImageUri(String fileName) {
        InputStream resource = HomePage.class.getResourceAsStream("/images/" + fileName);
        if (resource == null) {
            return null;
        }
        try (InputStream input = resource) {
            File file = File.createTempFile("winui4k-sample-gallery-", "-" + fileName);
            file.deleteOnExit();
            try (OutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, length);
                }
            }
            return file.toPath().toUri().toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** One of {@link #buildHorizontalScroller}'s left / right scroll buttons (the official ScrollButtonStyle's 16x38 chevron). */
    private static WButton buildScrollButton(String glyph) {
        WLabel icon = new WLabel(glyph);
        icon.setFontFamily("Segoe Fluent Icons");
        icon.setFontSize(8.0); // the official FlipViewButtonFontSize (8 in generic.xaml)

        WButton button = new WButton("");
        button.setContent(icon);
        button.setPadding(0.0);
        button.setWidth(16.0);
        button.setHeight(38.0);
        button.setVerticalAlignment(VerticalAlignment.CENTER);
        button.setMargin(8.0);
        button.setVisible(false); // hidden until an overflow is detected
        return button;
    }

    /**
     * Overlays left/right scroll buttons when a horizontal row of content overflows
     * (equivalent to the real Gallery's HorizontalScrollContainer). Doesn't show a scrollbar;
     * clicking a button animates a scroll by one viewport's worth.
     */
    private static WComponent buildHorizontalScroller(WComponent content) {
        WScrollPane scroller = new WScrollPane(content);
        scroller.setHorizontalScrollBarVisibility(ScrollBarVisibility.HIDDEN);
        scroller.setVerticalScrollBarVisibility(ScrollBarVisibility.DISABLED);

        // Segoe Fluent Icons: EDD9 = ChevronLeftSmall, EDDA = ChevronRightSmall (same glyphs as the real Gallery)
        WButton backButton = buildScrollButton("");
        backButton.setHorizontalAlignment(HorizontalAlignment.LEFT);
        WButton forwardButton = buildScrollButton("");
        forwardButton.setHorizontalAlignment(HorizontalAlignment.RIGHT);

        // Equivalent to the real Gallery's Scroller_ViewChanging / UpdateScrollButtonsVisibility:
        // hide the back button at the left edge, and the forward button at the right edge
        Runnable updateButtons = () -> {
            backButton.setVisible(scroller.getHorizontalOffset() > 1);
            forwardButton.setVisible(scroller.getHorizontalOffset() < scroller.getScrollableWidth() - 1);
        };
        scroller.addViewChangedListener(() -> {
            updateButtons.run();
        });
        scroller.addSizeChangedListener(() -> {
            updateButtons.run();
        });

        backButton.addActionListener(() -> {
            scroller.scrollToHorizontalOffset(scroller.getHorizontalOffset() - scroller.getViewportWidth());
        });
        forwardButton.addActionListener(() -> {
            scroller.scrollToHorizontalOffset(scroller.getHorizontalOffset() + scroller.getViewportWidth());
        });

        // Placing them in the same cell draws the buttons in front of the scrolled content
        WGrid container = new WGrid();
        container.addRow();
        container.add(scroller, 0, 0, 1, 1);
        container.add(backButton, 0, 0, 1, 1);
        container.add(forwardButton, 0, 0, 1, 1);
        return container;
    }

    /** Derives the card icon's image file name from a page name ("Multiple windows" -> "MultipleWindows.png"). */
    // Capitalizes each space-separated word and joins them. A plain replace(" ", "") would give "Multiplewindows",
    // which cannot resolve "MultipleWindows.png" inside a JAR (where case matters), hence the per-word capitalization.
    private static String controlImageFileName(String pageName) {
        StringBuilder builder = new StringBuilder();
        for (String word : pageName.split(" ")) {
            if (!word.isEmpty()) {
                builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return builder.toString() + ".png";
    }

    /** Opens a URL in the default browser. */
    private static void openUrl(String url) {
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Assembles the whole Home page.
     * {@code navigateTo} is the callback that navigates to a page when its card is clicked (the argument is the page name).
     */
    static WComponent buildHomePage(Consumer<String> navigateTo) {
        WPanel page = new WPanel();
        page.add(buildHomeHeader());
        page.add(buildFilterSection(navigateTo));
        return page;
    }

    /** Header: overlays the app name and external-link tiles on top of the hero image. */
    // The sample's declarative UI construction code
    private static WComponent buildHomeHeader() {
        // Fit the hero image to the area while preserving its aspect ratio, letting the background color show through faintly
        WImage hero = new WImage(galleryImageUri("GalleryHeaderImage.png"));
        hero.setStretch(Stretch.UNIFORM_TO_FILL);
        hero.setOpacity(0.9);

        // The height matches the official OpacityMaskView's Height="400". Without it the cell grows to the
        // image's natural size, and when that is shorter than the overlay (title + tiles, about 370) the image
        // is centered vertically and a band of background shows above the hero, so it is fixed larger than the overlay
        WBorder heroBackground = new WBorder(hero);
        heroBackground.setBackground(HEADER_BACKGROUND());
        heroBackground.setHeight(400.0);

        // Equivalent to the real OpacityMaskView's mask (fades out at 0.75-0.85). Since an opacity mask isn't
        // available, overlay a gradient toward the composited color of the content area below (Mica + translucent
        // white ~= near white) on top of the hero. As in the real Gallery, fully blend it in the gap between the
        // link tiles (0.7-0.8 of the fixed 400 height = 280-320), staying opaque past that. The boundary's y only
        // depends on the hero's fixed height, so it doesn't move even if the window width changes how the image is cropped
        WBorder heroFade = new WBorder();
        // The color it fades into is the content area's composited color (Mica + translucent layer); switch it with the theme
        WColor fadeColor = GalleryTheme.isDarkTheme ? new WColor(42, 42, 42, 255) : new WColor(252, 252, 252, 255);
        WColor fadeTransparent = new WColor(fadeColor.getRed(), fadeColor.getGreen(), fadeColor.getBlue(), 0);
        heroFade.setBackgroundGradient(new WLinearGradientPaint(
            90.0,
            new WGradientStop(0.0, fadeTransparent),
            new WGradientStop(0.7, fadeTransparent),
            new WGradientStop(0.8, fadeColor),
            new WGradientStop(1.0, fadeColor)));

        WPanel titleBlock = new WPanel();
        WLabel appSdkLabel = new WLabel("Windows App SDK");
        appSdkLabel.setFontSize(18.0);
        titleBlock.add(appSdkLabel);
        WLabel galleryLabel = new WLabel("WinUI4K Gallery");
        galleryLabel.setFontSize(40.0);
        galleryLabel.setFontWeight(600);
        titleBlock.add(galleryLabel);

        WPanel tiles = new WPanel(12.0, Orientation.HORIZONTAL);
        tiles.add(
            buildLinkTile(
                "Getting started",
                "See an overview of winui4k and WinUI in the docs.",
                "Header-WinUI.png",
                null,
                "https://github.com/nttr-tech/winui4k#readme"));
        tiles.add(
            buildLinkTile(
                "Design",
                "Guidelines and toolkits for building beautiful WinUI experiences.",
                "Header-WindowsDesign.png",
                null,
                "https://learn.microsoft.com/windows/apps/design/"));
        tiles.add(
            buildLinkTile(
                "winui4k on GitHub",
                "View winui4k's source code and repository.",
                null,
                "", // Segoe Fluent Icons: Globe
                "https://github.com/nttr-tech/winui4k"));
        tiles.add(
            buildLinkTile(
                "Community Toolkit",
                "A collection of helper functions, controls, and app services.",
                "Header-Toolkit.png",
                null,
                "https://apps.microsoft.com/store/detail/windows-community-toolkit-sample-app/9NBLGGH4TLCQ"));
        tiles.add(
            buildLinkTile(
                "Code samples",
                "Find samples that show specific tasks, features, and APIs.",
                null,
                "", // Segoe Fluent Icons: Code
                "https://learn.microsoft.com/windows/apps/get-started/samples"));
        tiles.add(
            buildLinkTile(
                "Partner Center",
                "Publish your app to the Microsoft Store.",
                "Header-Store.light.png",
                null,
                "https://developer.microsoft.com/windows/"));

        // The real spacing: the title block is Margin="36,48,0,0", and the tiles are 56 below the title
        WPanel overlay = new WPanel(56.0);
        overlay.setMargin(36.0, 48.0, 36.0, 36.0);
        overlay.add(titleBlock);
        overlay.add(buildHorizontalScroller(tiles));

        // Placing children in the same cell draws the later-added one on top (overlays text on the image)
        WGrid header = new WGrid();
        header.addRow();
        header.add(heroBackground, 0, 0, 1, 1);
        header.add(heroFade, 0, 0, 1, 1);
        header.add(overlay, 0, 0, 1, 1);
        return header;
    }

    /** A single external-link tile. The icon is either an image ({@code imageFileName}) or a font glyph ({@code glyph}). */
    private static WComponent buildLinkTile(
        String title,
        String description,
        String imageFileName,
        String glyph,
        String url
    ) {
        WComponent icon;
        if (imageFileName != null) {
            WImage image = new WImage(galleryImageUri(imageFileName));
            image.setWidth(36.0); // the official Gallery fills the icon row (height 36) with the image
            image.setHeight(36.0);
            image.setStretch(Stretch.UNIFORM);
            image.setHorizontalAlignment(HorizontalAlignment.LEFT);
            icon = image;
        } else {
            WLabel glyphLabel = new WLabel(glyph != null ? glyph : "");
            glyphLabel.setFontFamily("Segoe Fluent Icons");
            glyphLabel.setFontSize(24.0);
            glyphLabel.setHorizontalAlignment(HorizontalAlignment.LEFT);
            glyphLabel.setMargin(0.0, 8.0, 0.0, 0.0); // the official FontIcon's Margin="0,8,0,0"
            icon = glyphLabel;
        }

        WPanel texts = new WPanel(4.0);
        WLabel titleLabel = new WLabel(title);
        titleLabel.setFontWeight(600);
        texts.add(titleLabel);
        WLabel descriptionLabel = new WLabel(description);
        descriptionLabel.setForeground(GalleryTheme.TEXT_SECONDARY());
        descriptionLabel.setFontSize(12.0);
        descriptionLabel.setTextWrapping(TextWrapping.WRAP);
        texts.add(descriptionLabel);
        texts.setMargin(0.0, 16.0, 0.0, 0.0); // the equivalent of the official RowSpacing="16"

        // Equivalent to the inner Grid of the official Tile (Padding="24", rows 36 / *). Fixing the icon row height
        // keeps the title and everything below it at the same y position across all tiles, image icon or glyph icon
        WGrid content = new WGrid();
        content.addRow(GridLength.pixel(36.0));
        content.addRow(GridLength.star());
        content.add(icon, 0, 0, 1, 1);
        content.add(texts, 1, 0, 1, 1);
        content.setMargin(24.0);

        // Equivalent to the real Tile's bottom-right FontIcon (E8A7 = OpenInNewWindow), indicating it's an
        // external link. The real one uses Margin="-12" inside a Grid with Padding 24, so place it 12 in from the tile's edge
        WLabel cornerIcon = new WLabel("");
        cornerIcon.setFontFamily("Segoe Fluent Icons");
        cornerIcon.setFontSize(14.0);
        cornerIcon.setForeground(GalleryTheme.TEXT_SECONDARY());
        cornerIcon.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        cornerIcon.setVerticalAlignment(VerticalAlignment.BOTTOM);
        cornerIcon.setMargin(12.0);

        // Placing them in the same cell draws the icon at the body's bottom-right
        WGrid body = new WGrid();
        body.addRow();
        body.add(content, 0, 0, 1, 1);
        body.add(cornerIcon, 0, 0, 1, 1);

        WButton tile = new WButton("");
        tile.setContent(body);
        // Spacing is managed via content / cornerIcon's margins (equivalent to the real HyperlinkButton's Padding="-1")
        tile.setPadding(0.0);
        // Left at the default (CENTER), the Grid wouldn't fill the whole button and the icon wouldn't reach the bottom-right
        tile.setHorizontalContentAlignment(HorizontalAlignment.STRETCH);
        tile.setVerticalContentAlignment(VerticalAlignment.STRETCH);
        // The same dimensions as the real Tile (232x172)
        tile.setWidth(232.0);
        tile.setHeight(172.0);
        tile.addActionListener(() -> {
            openUrl(url);
        });
        return tile;
    }

    /**
     * The pill-shaped toggle that switches between Recent / Favorites (equivalent to the real Gallery's
     * SelectorBar TokenView style). Uses the same Symbol Clock / Favorite glyphs as the real Gallery.
     */
    private static WToggleButton buildFilterToggle(String text, String glyph) {
        WLabel icon = new WLabel(glyph);
        icon.setFontFamily("Segoe Fluent Icons");
        icon.setFontSize(16.0);
        icon.setVerticalAlignment(VerticalAlignment.CENTER);

        WLabel label = new WLabel(text);
        label.setVerticalAlignment(VerticalAlignment.CENTER);

        WPanel content = new WPanel(8.0, Orientation.HORIZONTAL);
        content.add(icon);
        content.add(label);

        WToggleButton toggle = new WToggleButton("");
        toggle.setContent(content);
        // The same padding as the real TokenView, plus a corner radius of half the height, for a pill shape
        toggle.setPadding(14.0, 5.0, 14.0, 6.0);
        toggle.setCornerRadius(16.0);
        return toggle;
    }

    /** The Recent / Favorites toggle and the card list below it. */
    private static WComponent buildFilterSection(Consumer<String> navigateTo) {
        WPanel contentArea = new WPanel();

        // Segoe Fluent Icons: E121 = Clock (matches the real Gallery's SelectorBarItem Icon="Clock"), E113 = Favorite
        WToggleButton recentToggle = buildFilterToggle("Recent", "");
        WToggleButton favoritesToggle = buildFilterToggle("Favorites", "");

        Consumer<Boolean> select = showRecent -> {
            recentToggle.setChecked(showRecent);
            favoritesToggle.setChecked(!showRecent);
            contentArea.removeAll();
            contentArea.add(showRecent ? buildRecentView(navigateTo) : buildFavoritesView(navigateTo));
        };
        recentToggle.addActionListener(() -> {
            select.accept(true);
        });
        favoritesToggle.addActionListener(() -> {
            select.accept(false);
        });
        select.accept(true);

        WPanel toggles = new WPanel(8.0, Orientation.HORIZONTAL);
        toggles.add(recentToggle);
        toggles.add(favoritesToggle);
        toggles.setHorizontalAlignment(HorizontalAlignment.CENTER);

        // The real spacing: the SelectorBar is Margin="36,24,0,16", the content below it is Margin="36,0,36,36"
        // (the 16 gap between the toggle and the content is expressed via spacing)
        WPanel section = new WPanel(16.0);
        section.setMargin(36.0, 24.0, 36.0, 36.0);
        section.add(toggles);
        section.add(contentArea);
        return section;
    }

    /** The Recent view: a row of recently-visited pages, plus a grid of recently added/updated pages. */
    private static WComponent buildRecentView(Consumer<String> navigateTo) {
        WPanel view = new WPanel(12.0);

        // Don't show page names that no longer exist (equivalent to the real Gallery's GetValidItems)
        List<String> recentlyVisited = new ArrayList<>();
        for (String name : GallerySettings.getRecentlyVisited()) {
            if (GalleryNavigation.pages.containsKey(name)) {
                recentlyVisited.add(name);
            }
        }
        if (!recentlyVisited.isEmpty()) {
            view.add(buildSectionTitle("Recently visited"));
            WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
            for (String name : recentlyVisited) {
                row.add(buildControlCard(name, navigateTo));
            }
            // Same as the real Gallery: let horizontal scrolling reveal any overflow
            view.add(buildHorizontalScroller(row));
        }

        view.add(buildSectionTitle("Recently added or updated"));
        view.add(buildCardGrid(recentlyAddedOrUpdatedPages, navigateTo));
        return view;
    }

    /** The Favorites view: a grid of favorites. Shows a fallback message if empty. */
    private static WComponent buildFavoritesView(Consumer<String> navigateTo) {
        List<String> favorites = new ArrayList<>();
        for (String name : GallerySettings.getFavorites()) {
            if (GalleryNavigation.pages.containsKey(name)) {
                favorites.add(name);
            }
        }
        if (!favorites.isEmpty()) {
            return buildCardGrid(favorites, navigateTo);
        }

        WPanel fallback = new WPanel(8.0);
        fallback.setMargin(36.0);
        WImage image = new WImage(galleryImageUri("RatingControl.png"));
        image.setHeight(36.0);
        image.setStretch(Stretch.UNIFORM);
        fallback.add(image);
        WLabel titleLabel = new WLabel("No favorites yet");
        titleLabel.setFontWeight(600);
        titleLabel.setHorizontalAlignment(HorizontalAlignment.CENTER);
        fallback.add(titleLabel);
        WLabel descriptionLabel = new WLabel("Click the star icon on any page to have it show up here.");
        descriptionLabel.setForeground(GalleryTheme.TEXT_SECONDARY());
        descriptionLabel.setHorizontalAlignment(HorizontalAlignment.CENTER);
        fallback.add(descriptionLabel);
        return fallback;
    }

    /**
     * The favorite star placed next to a page heading (equivalent to the real Gallery's star icon on each sample page).
     * Keeps the checked state in sync with {@link GallerySettings#getFavorites} and reflects it in Home's Favorites.
     */
    static WToggleButton buildFavoriteToggle(String pageName) {
        // Segoe Fluent Icons: E734 = star (outline), E735 = star (filled)
        WLabel star = new WLabel(GallerySettings.isFavorite(pageName) ? "" : "");
        star.setFontFamily("Segoe Fluent Icons");
        star.setFontSize(14.0);

        WToggleButton toggle = new WToggleButton("");
        toggle.setContent(star);
        toggle.setChecked(GallerySettings.isFavorite(pageName));
        toggle.addItemListener(checked -> {
            boolean isFavorite = Boolean.TRUE.equals(checked);
            star.setText(isFavorite ? "" : "");
            GallerySettings.setFavorite(pageName, isFavorite);
        });
        return toggle;
    }

    /** A section heading (e.g. "Recently visited"). */
    private static WComponent buildSectionTitle(String title) {
        WLabel label = new WLabel(title);
        label.setFontSize(16.0);
        label.setFontWeight(600);
        return label;
    }

    /** Lines cards up in a wrapping grid. */
    private static WComponent buildCardGrid(List<String> names, Consumer<String> navigateTo) {
        WVariableSizedWrapGrid grid = new WVariableSizedWrapGrid(328.0, 96.0);
        grid.setOrientation(Orientation.HORIZONTAL);
        for (String name : names) {
            grid.add(buildControlCard(name, navigateTo));
        }
        return grid;
    }

    /** A single page's card (icon + page name + description). Clicking it navigates to that page. */
    private static WComponent buildControlCard(String name, Consumer<String> navigateTo) {
        WPanel texts = new WPanel(2.0);
        WLabel nameLabel = new WLabel(name);
        nameLabel.setFontWeight(600);
        texts.add(nameLabel);
        WLabel descriptionLabel = new WLabel(pageDescriptions.getOrDefault(name, ""));
        descriptionLabel.setForeground(GalleryTheme.TEXT_SECONDARY());
        descriptionLabel.setFontSize(12.0);
        descriptionLabel.setTextWrapping(TextWrapping.WRAP);
        // Inside a horizontal WPanel the width isn't fixed and wrapping won't kick in, so set it explicitly
        descriptionLabel.setWidth(235.0);
        descriptionLabel.setHorizontalAlignment(HorizontalAlignment.LEFT);
        texts.add(descriptionLabel);
        texts.setVerticalAlignment(VerticalAlignment.CENTER);

        WPanel content = new WPanel(12.0, Orientation.HORIZONTAL);
        WImage image = new WImage(galleryImageUri(controlImageFileName(name)));
        image.setWidth(36.0);
        image.setHeight(36.0);
        image.setStretch(Stretch.UNIFORM);
        image.setVerticalAlignment(VerticalAlignment.CENTER);
        content.add(image);
        content.add(texts);
        content.setHorizontalAlignment(HorizontalAlignment.LEFT);
        content.setVerticalAlignment(VerticalAlignment.CENTER);

        WButton card = new WButton("");
        card.setContent(content);
        card.setWidth(320.0);
        card.setHeight(88.0);
        card.addActionListener(() -> {
            navigateTo.accept(name);
        });
        return card;
    }
}

/** Persists recently-visited / favorite pages via Java Preferences (equivalent to the real Gallery's SettingsHelper). */
final class GallerySettings {
    private GallerySettings() {
    }

    private static final int MAX_RECENTLY_VISITED = 8;

    private static final Preferences preferences =
        Preferences.userRoot().node("com/appkitbox/winui4k/sample/gallery");

    /** Page names can contain spaces, so store them newline-separated. */
    private static List<String> load(String key) {
        List<String> names = new ArrayList<>();
        for (String name : preferences.get(key, "").split("\n")) {
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private static void store(String key, List<String> names) {
        preferences.put(key, String.join("\n", names));
    }

    /** Recently visited page names (newest first). */
    static List<String> getRecentlyVisited() {
        return load("recentlyVisited");
    }

    private static void setRecentlyVisited(List<String> value) {
        store("recentlyVisited", value);
    }

    /** Page names registered as favorites (in registration order). */
    static List<String> getFavorites() {
        return load("favorites");
    }

    private static void setFavorites(List<String> value) {
        store("favorites", value);
    }

    /** Records that a page was visited (moves it to the front, dropping the oldest entries once over the limit). */
    static void addRecentlyVisited(String name) {
        List<String> names = new ArrayList<>();
        names.add(name);
        for (String visited : getRecentlyVisited()) {
            if (!visited.equals(name) && names.size() < MAX_RECENTLY_VISITED) {
                names.add(visited);
            }
        }
        setRecentlyVisited(names);
    }

    static boolean isFavorite(String name) {
        return getFavorites().contains(name);
    }

    static void setFavorite(String name, boolean isFavorite) {
        List<String> names = getFavorites();
        names.remove(name);
        if (isFavorite) {
            names.add(name);
        }
        setFavorites(names);
    }

    /** Clears all recently-visited page history (the Settings page's Clear recents). */
    static void clearRecentlyVisited() {
        setRecentlyVisited(Collections.<String>emptyList());
    }

    /** Clears all favorites (the Settings page's Remove favorites). */
    static void clearFavorites() {
        setFavorites(Collections.<String>emptyList());
    }

    /** The app theme chosen on the Settings page ("Light" / "Dark" / "Default"). */
    static String getAppTheme() {
        return preferences.get("appTheme", "Default");
    }

    static void setAppTheme(String value) {
        preferences.put("appTheme", value);
    }

    /** The navigation placement chosen on the Settings page ("Left" / "Top"). */
    static String getNavigationStyle() {
        return preferences.get("navigationStyle", "Left");
    }

    static void setNavigationStyle(String value) {
        preferences.put("navigationStyle", value);
    }
}
