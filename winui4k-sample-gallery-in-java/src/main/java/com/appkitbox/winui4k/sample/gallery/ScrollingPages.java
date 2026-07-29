package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.PipsPagerButtonVisibility;
import com.appkitbox.winui4k.ScrollBarVisibility;
import com.appkitbox.winui4k.ScrollingContentOrientation;
import com.appkitbox.winui4k.ScrollingScrollBarVisibility;
import com.appkitbox.winui4k.ScrollingZoomMode;
import com.appkitbox.winui4k.Stretch;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WAnnotatedScrollBar;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WImage;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WList;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WPipsPager;
import com.appkitbox.winui4k.WScrollPane;
import com.appkitbox.winui4k.WScrollView;
import com.appkitbox.winui4k.WSemanticZoom;
import com.appkitbox.winui4k.WSlider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import kotlin.Unit;

/*
 * Scrolling category: demo pages for AnnotatedScrollBar / PipsPager / ScrollView / ScrollViewer / SemanticZoom.
 */
final class ScrollingPages {
    private ScrollingPages() {
    }

    // region Common helper

    /** A tile grid (14 rows x 6 columns) larger than the viewport, used by the scrolling demos. Overflows in both directions. */
    private static WComponent buildScrollingTileContent() {
        List<WColor> palette = Arrays.asList(
            new WColor(0, 120, 212, 255),
            new WColor(16, 137, 62, 255),
            new WColor(202, 80, 16, 255),
            new WColor(142, 68, 173, 255),
            new WColor(193, 0, 76, 255),
            new WColor(0, 153, 188, 255));
        WPanel content = new WPanel(8.0, Orientation.VERTICAL);
        for (int row = 0; row < 14; row++) {
            WPanel rowPanel = new WPanel(8.0, Orientation.HORIZONTAL);
            for (int column = 0; column < 6; column++) {
                WBorder tile = GalleryScaffold.buildTile(palette.get((row + column) % palette.size()), 130.0, 48.0);
                WLabel label = new WLabel(row + "-" + column);
                label.setForeground(WColor.Companion.getWHITE());
                tile.setChild(label);
                tile.setPadding(8.0);
                rowPanel.add(tile);
            }
            content.add(rowPanel);
        }
        return content;
    }

    // endregion

    // region AnnotatedScrollBar

    /** The AnnotatedScrollBar page: lines up a demo for trying out WAnnotatedScrollBar's features. */
    static WComponent buildAnnotatedScrollBarPage() {
        WPanel page = GalleryScaffold.buildPage(
            "AnnotatedScrollBar",
            "A control that extends a vertical scrollbar with labels along the rail so you can jump "
                + "quickly through a large collection. Try it out with WAnnotatedScrollBar connected "
                + "as a ScrollView's vertical scroll controller. Clicking a label jumps to that "
                + "position, and hovering over the rail shows the color group name as a tooltip "
                + "(DetailLabelRequested). (The real Gallery uses ItemsRepeater for the content, but "
                + "since winui4k doesn't support that yet, this substitutes a tile grid.)");

        page.add(buildAnnotatedScrollBarExample());
        return page;
    }

    /** The same color groups as the real Gallery. Attach a label at each group's starting scroll offset */
    private static final class ColorGroup {
        final String name;
        final WColor color;
        final int count;

        ColorGroup(String name, WColor color, int count) {
            this.name = name;
            this.color = color;
            this.count = count;
        }
    }

    /** Each group's starting scroll offset (in pixels) paired with its name. */
    private static final class GroupOffset {
        final double offset;
        final String name;

        GroupOffset(double offset, String name) {
            this.offset = offset;
            this.name = name;
        }
    }

    /** Navigation paired with a ScrollView: Labels / connecting a ScrollController / DetailLabelRequested. */
    private static WComponent buildAnnotatedScrollBarExample() {
        List<ColorGroup> groups = Arrays.asList(
            new ColorGroup("Azure", new WColor(0, 120, 212, 255), 12),
            new ColorGroup("Crimson", new WColor(220, 20, 60, 255), 18),
            new ColorGroup("Cyan", new WColor(0, 153, 188, 255), 6),
            new ColorGroup("Fuchsia", new WColor(194, 57, 179, 255), 24),
            new ColorGroup("Gold", new WColor(202, 149, 16, 255), 18));
        int columns = 6;
        double tileSize = 100.0;
        double rowSpacing = 4.0;
        double rowPitch = tileSize + rowSpacing;

        WPanel content = new WPanel(rowSpacing, Orientation.VERTICAL);
        // Each group's starting scroll offset (in pixels) and name. Used by the labels and DetailLabelRequested
        List<GroupOffset> groupStartOffsets = new ArrayList<>();
        int rowIndex = 0;
        for (ColorGroup group : groups) {
            groupStartOffsets.add(new GroupOffset(rowIndex * rowPitch, group.name));
            int remaining = group.count;
            while (remaining > 0) {
                WPanel rowPanel = new WPanel(rowSpacing, Orientation.HORIZONTAL);
                int tilesInRow = Math.min(columns, remaining);
                for (int i = 0; i < tilesInRow; i++) {
                    rowPanel.add(GalleryScaffold.buildTile(group.color, tileSize, tileSize));
                }
                remaining -= tilesInRow;
                content.add(rowPanel);
                rowIndex++;
            }
        }

        WScrollView scrollView = new WScrollView(content);
        scrollView.setWidth(680.0);
        scrollView.setHeight(500.0);
        // AnnotatedScrollBar takes over the vertical bar's role, so hide it (scrolling itself stays enabled)
        scrollView.setVerticalScrollBarVisibility(ScrollingScrollBarVisibility.HIDDEN);

        WAnnotatedScrollBar annotatedScrollBar = new WAnnotatedScrollBar();
        annotatedScrollBar.setHeight(500.0);
        annotatedScrollBar.setVerticalAlignment(VerticalAlignment.TOP);
        annotatedScrollBar.addDetailLabelRequestedListener(offset -> {
            // Return the name of the group containing this offset, as the tooltip
            for (int i = groupStartOffsets.size() - 1; i >= 0; i--) {
                if (groupStartOffsets.get(i).offset <= offset) {
                    return groupStartOffsets.get(i).name;
                }
            }
            return groups.get(0).name;
        });

        // Connect it as the vertical scroll controller after the ScrollView's template is applied (Loaded)
        scrollView.addLoadedListener(() -> {
            annotatedScrollBar.connectTo(scrollView);
            return Unit.INSTANCE;
        });
        // Place the labels (markers) after the content is laid out (SizeChanged = the scroll range is
        // finalized). The real Gallery also rebuilds its labels on ItemsRepeater's SizeChanged
        content.addSizeChangedListener(() -> {
            annotatedScrollBar.connectTo(scrollView); // connect it if it is not connected yet
            annotatedScrollBar.clearLabels();
            for (GroupOffset groupOffset : groupStartOffsets) {
                annotatedScrollBar.addLabel(groupOffset.name, groupOffset.offset);
            }
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(8.0, Orientation.HORIZONTAL);
        body.add(scrollView);
        body.add(annotatedScrollBar);

        // Options: as in the real Gallery, change AnnotatedScrollBar's height to observe labels being thinned out
        WSlider heightSlider = new WSlider(100.0, 500.0, 500.0);
        heightSlider.setHeader("AnnotatedScrollBar height");
        heightSlider.setWidth(240.0);
        heightSlider.addChangeListener(value -> {
            annotatedScrollBar.setHeight(value);
            return Unit.INSTANCE;
        });

        WPanel options = new WPanel(12.0, Orientation.VERTICAL);
        options.add(GalleryScaffold.optionsLabel("Shrinking the height lets you see labels get thinned out so they don't collide."));
        options.add(heightSlider);

        return GalleryScaffold.buildExample(
            "Navigation paired with a ScrollView (Labels / ScrollController / DetailLabelRequested)",
            body,
            options);
    }

    // endregion

    // region PipsPager

    /** The PipsPager page: lines up demos for trying out WPipsPager's various features. */
    static WComponent buildPipsPagerPage() {
        WPanel page = GalleryScaffold.buildPage(
            "PipsPager",
            "A pager that moves between pages via a row of dots (pips) instead of explicit page "
                + "numbers. Try out WPipsPager's various features.");

        page.add(buildPipsPagerGalleryExample());
        page.add(buildPipsPagerOptionsExample());
        return page;
    }

    /** Paired with an image gallery: NumberOfPages / SelectedPageIndex / SelectedIndexChanged. */
    private static WComponent buildPipsPagerGalleryExample() {
        // The same 8 landscape photos as the real Gallery. Swap the displayed image based on the selected page
        List<String> imageUris = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            imageUris.add(HomePage.galleryImageUri("LandscapeImage" + i + ".jpg"));
        }

        WImage image = new WImage(imageUris.get(0));
        image.setWidth(400.0);
        image.setHeight(270.0);

        WPipsPager pager = new WPipsPager();
        pager.setNumberOfPages(imageUris.size());
        // Unlike the real Gallery's FlipView example, also show the buttons so paging works via them too
        pager.setPreviousButtonVisibility(PipsPagerButtonVisibility.VISIBLE);
        pager.setNextButtonVisibility(PipsPagerButtonVisibility.VISIBLE);
        pager.setHorizontalAlignment(HorizontalAlignment.CENTER);
        pager.addSelectedIndexChangedListener(index -> {
            image.setSourceUri(imageUris.get(index));
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(12.0, Orientation.VERTICAL);
        body.add(image);
        body.add(pager);
        return GalleryScaffold.buildExample("Paired with an image gallery (NumberOfPages / SelectedPageIndex / SelectedIndexChanged)", body);
    }

    /** Orientation and button visibility: switch Orientation / the previous/next buttons' visibility via Options. */
    private static WComponent buildPipsPagerOptionsExample() {
        WPipsPager pager = new WPipsPager();
        pager.setNumberOfPages(10);
        pager.setPreviousButtonVisibility(PipsPagerButtonVisibility.VISIBLE);
        pager.setNextButtonVisibility(PipsPagerButtonVisibility.VISIBLE);

        WLabel selected = new WLabel("Selected page: 1");
        selected.setForeground(GalleryTheme.TEXT_SECONDARY());
        pager.addSelectedIndexChangedListener(index -> {
            selected.setText("Selected page: " + (index + 1));
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(12.0, Orientation.VERTICAL);
        body.add(pager);
        body.add(selected);

        // Options: the same controls as the real Gallery's PipsPagerPage Example2
        WComboBox orientationCombo = new WComboBox(Arrays.asList("Horizontal", "Vertical"));
        orientationCombo.setHeader("Orientation");
        orientationCombo.setWidth(240.0);
        orientationCombo.setSelectedIndex(0);
        orientationCombo.addListSelectionListener(() -> {
            pager.setOrientation(
                "Vertical".equals(orientationCombo.getSelectedItem()) ? Orientation.VERTICAL : Orientation.HORIZONTAL);
            return Unit.INSTANCE;
        });

        WPanel options = new WPanel(12.0, Orientation.VERTICAL);
        options.add(orientationCombo);
        options.add(visibilityCombo("Previous button visibility", pager::setPreviousButtonVisibility));
        options.add(visibilityCombo("Next button visibility", pager::setNextButtonVisibility));

        return GalleryScaffold.buildExample(
            "Orientation and button visibility (Orientation / PreviousButtonVisibility / NextButtonVisibility)",
            body,
            options);
    }

    /** A combo box for switching the visibility of the previous / next buttons (for buildPipsPagerOptionsExample). */
    private static WComboBox visibilityCombo(String header, Consumer<PipsPagerButtonVisibility> apply) {
        WComboBox combo = new WComboBox(Arrays.asList("Visible", "VisibleOnPointerOver", "Collapsed"));
        combo.setHeader(header);
        combo.setWidth(240.0);
        combo.setSelectedIndex(0);
        combo.addListSelectionListener(() -> {
            PipsPagerButtonVisibility visibility;
            String selectedItem = combo.getSelectedItem();
            if ("VisibleOnPointerOver".equals(selectedItem)) {
                visibility = PipsPagerButtonVisibility.VISIBLE_ON_POINTER_OVER;
            } else if ("Collapsed".equals(selectedItem)) {
                visibility = PipsPagerButtonVisibility.COLLAPSED;
            } else {
                visibility = PipsPagerButtonVisibility.VISIBLE;
            }
            apply.accept(visibility);
            return Unit.INSTANCE;
        });
        return combo;
    }

    // endregion

    // region ScrollView

    /** The ScrollView page: lines up demos for trying out WScrollView's various features. */
    static WComponent buildScrollViewPage() {
        WPanel page = GalleryScaffold.buildPage(
            "ScrollView",
            "A container that shows content larger than the viewport by scrolling, panning, and "
                + "zooming it. Try out the various features of WScrollView, the successor to ScrollViewer.");

        page.add(buildScrollViewImageExample());
        page.add(buildScrollViewProgrammaticExample());
        return page;
    }

    /** Zooming an image: scale via ContentOrientation=None + ZoomMode, with Options to change the behavior. */
    private static WComponent buildScrollViewImageExample() {
        WImage image = new WImage(HomePage.galleryImageUri("cliff.jpg"));
        image.setStretch(Stretch.UNIFORM);

        WScrollView scrollView = new WScrollView(image);
        scrollView.setWidth(400.0);
        scrollView.setHeight(266.0);
        scrollView.setContentOrientation(ScrollingContentOrientation.NONE);
        scrollView.setZoomMode(ScrollingZoomMode.ENABLED);

        WLabel note = new WLabel("Zoom with Ctrl+mouse wheel, or a touch pinch gesture. Use the settings on the right to change the behavior.");
        note.setTextWrapping(TextWrapping.WRAP);
        note.setForeground(GalleryTheme.TEXT_SECONDARY());

        WPanel body = new WPanel(12.0, Orientation.VERTICAL);
        body.add(note);
        body.add(scrollView);

        // Options: the same controls as the real Gallery's ScrollViewPage Example1 (zoom-value fields omitted due to the float constraint)
        WComboBox zoomModeCombo = new WComboBox(Arrays.asList("Enabled", "Disabled"));
        zoomModeCombo.setHeader("ZoomMode");
        zoomModeCombo.setWidth(240.0);
        zoomModeCombo.setSelectedIndex(0);
        zoomModeCombo.addListSelectionListener(() -> {
            scrollView.setZoomMode(
                "Disabled".equals(zoomModeCombo.getSelectedItem()) ? ScrollingZoomMode.DISABLED : ScrollingZoomMode.ENABLED);
            return Unit.INSTANCE;
        });

        WComboBox orientationCombo = new WComboBox(Arrays.asList("None", "Vertical", "Horizontal", "Both"));
        orientationCombo.setHeader("ContentOrientation");
        orientationCombo.setWidth(240.0);
        orientationCombo.setSelectedIndex(0);
        orientationCombo.addListSelectionListener(() -> {
            ScrollingContentOrientation orientation;
            String selectedItem = orientationCombo.getSelectedItem();
            if ("Vertical".equals(selectedItem)) {
                orientation = ScrollingContentOrientation.VERTICAL;
            } else if ("Horizontal".equals(selectedItem)) {
                orientation = ScrollingContentOrientation.HORIZONTAL;
            } else if ("Both".equals(selectedItem)) {
                orientation = ScrollingContentOrientation.BOTH;
            } else {
                orientation = ScrollingContentOrientation.NONE;
            }
            scrollView.setContentOrientation(orientation);
            return Unit.INSTANCE;
        });

        WPanel options = new WPanel(12.0, Orientation.VERTICAL);
        options.add(zoomModeCombo);
        options.add(orientationCombo);
        options.add(scrollBarCombo("Horizontal ScrollBar", scrollView::setHorizontalScrollBarVisibility));
        options.add(scrollBarCombo("Vertical ScrollBar", scrollView::setVerticalScrollBarVisibility));

        return GalleryScaffold.buildExample(
            "Zooming an image (ZoomMode / ContentOrientation / ScrollBarVisibility)",
            body,
            options);
    }

    /** A combo box for switching how the scrollbars are shown (for buildScrollViewImageExample). */
    private static WComboBox scrollBarCombo(String header, Consumer<ScrollingScrollBarVisibility> apply) {
        WComboBox combo = new WComboBox(Arrays.asList("Auto", "Visible", "Hidden"));
        combo.setHeader(header);
        combo.setWidth(240.0);
        combo.setSelectedIndex(0);
        combo.addListSelectionListener(() -> {
            ScrollingScrollBarVisibility visibility;
            String selectedItem = combo.getSelectedItem();
            if ("Visible".equals(selectedItem)) {
                visibility = ScrollingScrollBarVisibility.VISIBLE;
            } else if ("Hidden".equals(selectedItem)) {
                visibility = ScrollingScrollBarVisibility.HIDDEN;
            } else {
                visibility = ScrollingScrollBarVisibility.AUTO;
            }
            apply.accept(visibility);
            return Unit.INSTANCE;
        });
        return combo;
    }

    /** Scrolling programmatically: ScrollTo / ScrollBy and subscribing to scroll position (ViewChanged). */
    private static WComponent buildScrollViewProgrammaticExample() {
        WScrollView scrollView = new WScrollView(buildScrollingTileContent());
        scrollView.setWidth(400.0);
        scrollView.setHeight(260.0);
        scrollView.setContentOrientation(ScrollingContentOrientation.BOTH);

        WLabel offset = new WLabel("Position: (0, 0)");
        offset.setForeground(GalleryTheme.TEXT_SECONDARY());
        scrollView.addViewChangedListener(() -> {
            offset.setText("Position: (" + (int) scrollView.getHorizontalOffset() + ", " + (int) scrollView.getVerticalOffset() + ")");
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(12.0, Orientation.VERTICAL);
        body.add(scrollView);
        body.add(offset);

        WButton toStart = new WButton("To start");
        toStart.addActionListener(() -> {
            scrollView.scrollTo(0.0, 0.0);
            return Unit.INSTANCE;
        });
        WButton toEnd = new WButton("To end");
        toEnd.addActionListener(() -> {
            scrollView.scrollTo(scrollView.getScrollableWidth(), scrollView.getScrollableHeight());
            return Unit.INSTANCE;
        });
        WButton down = new WButton("Down +100");
        down.addActionListener(() -> {
            scrollView.scrollBy(0.0, 100.0);
            return Unit.INSTANCE;
        });
        WButton right = new WButton("Right +100");
        right.addActionListener(() -> {
            scrollView.scrollBy(100.0, 0.0);
            return Unit.INSTANCE;
        });

        WPanel options = new WPanel(8.0, Orientation.VERTICAL);
        options.add(GalleryScaffold.optionsLabel("ScrollTo animates to an absolute position; ScrollBy animates by a relative amount from the current position."));
        options.add(toStart);
        options.add(toEnd);
        options.add(down);
        options.add(right);

        return GalleryScaffold.buildExample("Scrolling programmatically (ScrollTo / ScrollBy / ViewChanged)", body, options);
    }

    // endregion

    // region ScrollViewer

    /** The ScrollViewer page: lines up demos for trying out WScrollPane's various features. */
    static WComponent buildScrollViewerPage() {
        WPanel page = GalleryScaffold.buildPage(
            "ScrollViewer",
            "The traditional container that shows content larger than the viewport by scrolling it. "
                + "Try out WScrollPane's various features.");

        page.add(buildScrollViewerVisibilityExample());
        page.add(buildScrollViewerProgrammaticExample());
        return page;
    }

    /** How scrollbars are shown: switch horizontal / vertical ScrollBarVisibility via Options. */
    private static WComponent buildScrollViewerVisibilityExample() {
        WScrollPane scrollPane = new WScrollPane(buildScrollingTileContent());
        scrollPane.setWidth(400.0);
        scrollPane.setHeight(260.0);
        // Horizontal scrolling is disabled by default, so set it to AUTO to also allow interacting with the horizontal overflow
        scrollPane.setHorizontalScrollBarVisibility(ScrollBarVisibility.AUTO);
        scrollPane.setVerticalScrollBarVisibility(ScrollBarVisibility.VISIBLE);

        WPanel body = new WPanel(12.0, Orientation.VERTICAL);
        body.add(scrollPane);

        WPanel options = new WPanel(12.0, Orientation.VERTICAL);
        options.add(visibilityCombo("Horizontal ScrollBar", ScrollBarVisibility.AUTO, scrollPane::setHorizontalScrollBarVisibility));
        options.add(visibilityCombo("Vertical ScrollBar", ScrollBarVisibility.VISIBLE, scrollPane::setVerticalScrollBarVisibility));

        return GalleryScaffold.buildExample("How scrollbars are shown (HorizontalScrollBarVisibility / VerticalScrollBarVisibility)", body, options);
    }

    /** A combo box for switching how the scrollbars are shown (for buildScrollViewerVisibilityExample). */
    private static WComboBox visibilityCombo(String header, ScrollBarVisibility initial, Consumer<ScrollBarVisibility> apply) {
        List<String> names = Arrays.asList("Disabled", "Auto", "Hidden", "Visible");
        WComboBox combo = new WComboBox(names);
        combo.setHeader(header);
        combo.setWidth(240.0);
        String initialName = initial.name().toLowerCase(Locale.ROOT);
        initialName = Character.toUpperCase(initialName.charAt(0)) + initialName.substring(1);
        combo.setSelectedIndex(names.indexOf(initialName));
        combo.addListSelectionListener(() -> {
            ScrollBarVisibility visibility;
            String selectedItem = combo.getSelectedItem();
            if ("Disabled".equals(selectedItem)) {
                visibility = ScrollBarVisibility.DISABLED;
            } else if ("Hidden".equals(selectedItem)) {
                visibility = ScrollBarVisibility.HIDDEN;
            } else if ("Visible".equals(selectedItem)) {
                visibility = ScrollBarVisibility.VISIBLE;
            } else {
                visibility = ScrollBarVisibility.AUTO;
            }
            apply.accept(visibility);
            return Unit.INSTANCE;
        });
        return combo;
    }

    /** Scrolling horizontally from code: ChangeView and subscribing to scroll position (ViewChanged). */
    private static WComponent buildScrollViewerProgrammaticExample() {
        WScrollPane scrollPane = new WScrollPane(buildScrollingTileContent());
        scrollPane.setWidth(400.0);
        scrollPane.setHeight(200.0);
        scrollPane.setHorizontalScrollBarVisibility(ScrollBarVisibility.AUTO);

        WLabel offset = new WLabel("Horizontal position: 0 / Scrollable width: 0");
        offset.setForeground(GalleryTheme.TEXT_SECONDARY());
        scrollPane.addViewChangedListener(() -> {
            offset.setText("Horizontal position: " + (int) scrollPane.getHorizontalOffset()
                + " / Scrollable width: " + (int) scrollPane.getScrollableWidth());
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(12.0, Orientation.VERTICAL);
        body.add(scrollPane);
        body.add(offset);

        WButton toStart = new WButton("To left edge");
        toStart.addActionListener(() -> {
            scrollPane.scrollToHorizontalOffset(0.0);
            return Unit.INSTANCE;
        });
        WButton toEnd = new WButton("To right edge");
        toEnd.addActionListener(() -> {
            scrollPane.scrollToHorizontalOffset(scrollPane.getScrollableWidth());
            return Unit.INSTANCE;
        });
        WButton center = new WButton("To center");
        center.addActionListener(() -> {
            scrollPane.scrollToHorizontalOffset(scrollPane.getScrollableWidth() / 2.0);
            return Unit.INSTANCE;
        });

        WPanel options = new WPanel(8.0, Orientation.VERTICAL);
        options.add(GalleryScaffold.optionsLabel("ScrollToHorizontalOffset (ChangeView) animates the horizontal scroll position."));
        options.add(toStart);
        options.add(center);
        options.add(toEnd);

        return GalleryScaffold.buildExample("Scrolling horizontally from code (ChangeView / ViewChanged)", body, options);
    }

    // endregion

    // region SemanticZoom

    /** The SemanticZoom page: lines up a demo for trying out WSemanticZoom's features. */
    static WComponent buildSemanticZoomPage() {
        WPanel page = GalleryScaffold.buildPage(
            "SemanticZoom",
            "A control that switches between a detail view and a summary view of a single "
                + "collection, making large collections easier to navigate. Try out WSemanticZoom's "
                + "various features. (The real Gallery uses a grouped CollectionViewSource, but since "
                + "winui4k doesn't support that yet, this demo simplifies it to detail view = all "
                + "items, summary view = range labels.)");

        page.add(buildSemanticZoomExample());
        return page;
    }

    /** Switching views: ZoomedInView / ZoomedOutView / ToggleActiveView / ViewChangeStarted. */
    private static WComponent buildSemanticZoomExample() {
        // Detail view: all 60 items. Summary view: range labels grouped by 20 items
        List<String> zoomedInItems = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            zoomedInItems.add("Item " + i);
        }
        WList zoomedIn = new WList(zoomedInItems);
        WList zoomedOut = new WList(Arrays.asList("Item 1-20", "Item 21-40", "Item 41-60"));

        WSemanticZoom semanticZoom = new WSemanticZoom(zoomedIn, zoomedOut);
        semanticZoom.setWidth(400.0);
        semanticZoom.setHeight(400.0);
        semanticZoom.setCanChangeViews(true);
        semanticZoom.setZoomOutButtonEnabled(true);

        WLabel activeView = new WLabel("Showing: detail view (ZoomedIn)");
        activeView.setForeground(GalleryTheme.TEXT_SECONDARY());
        semanticZoom.addViewChangeStartedListener(() -> {
            // ViewChangeStarted fires at the "start" of a switch, so this shows the view about to be displayed
            activeView.setText(
                semanticZoom.isZoomedInViewActive() ? "Showing: summary view (ZoomedOut)" : "Showing: detail view (ZoomedIn)");
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(12.0, Orientation.VERTICAL);
        body.add(semanticZoom);
        body.add(activeView);

        WButton toggle = new WButton("Switch view (ToggleActiveView)");
        toggle.addActionListener(() -> {
            semanticZoom.toggleActiveView();
            return Unit.INSTANCE;
        });

        WPanel options = new WPanel(8.0, Orientation.VERTICAL);
        options.add(GalleryScaffold.optionsLabel("You can also switch by zooming out with Ctrl+mouse wheel, or via the zoom-out button in the bottom-left of the summary view."));
        options.add(toggle);

        return GalleryScaffold.buildExample("Switching views (ZoomedInView / ZoomedOutView / ToggleActiveView)", body, options);
    }

    // endregion
}
