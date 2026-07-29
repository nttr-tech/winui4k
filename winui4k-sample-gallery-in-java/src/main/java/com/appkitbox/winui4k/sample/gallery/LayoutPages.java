package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.ContentAlignment;
import com.appkitbox.winui4k.ExpandDirection;
import com.appkitbox.winui4k.GridLength;
import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.SplitViewDisplayMode;
import com.appkitbox.winui4k.SplitViewPanePlacement;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WBorderLayout;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCanvas;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WExpander;
import com.appkitbox.winui4k.WGrid;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WLayoutPanel;
import com.appkitbox.winui4k.WLinearGradientPaint;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WRelativePanel;
import com.appkitbox.winui4k.WSettingsCard;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WSplitView;
import com.appkitbox.winui4k.WToggleSwitch;
import com.appkitbox.winui4k.WVariableSizedWrapGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import kotlin.Pair;

/*
 * Layout category: demo pages for Border / Canvas / Expander / Grid / RelativePanel / SplitView / StackPanel / VariableSizedWrapGrid.
 */
final class LayoutPages {
    private LayoutPages() {
    }

    // region Border

    /** The Border page: lines up demos for trying out WBorder's various features. */
    static WComponent buildBorderPage() {
        WPanel page = GalleryScaffold.buildPage("Border", "A container that draws a border, background, and rounded corners around a single child. Try out WBorder's various features.");

        page.add(buildBorderStyleExample());
        page.add(buildBorderBackgroundExample());
        page.add(buildBorderGradientExample());
        return page;
    }

    /** Switching the border thickness and corner rounding with buttons. */
    private static WComponent buildBorderStyleExample() {
        WBorder border = new WBorder(new WLabel("Content with a border"));
        border.setBorderColor(WColor.BLUE);
        border.setBorderThickness(2.0);
        border.setPadding(16.0);

        WButton thicknessButton = new WButton("Increase border thickness");
        thicknessButton.addActionListener(() -> {
            border.setBorderThickness(border.getBorderThickness() >= 8.0 ? 2.0 : border.getBorderThickness() + 2.0);
        });

        WButton cornerButton = new WButton("Toggle rounded corners");
        cornerButton.addActionListener(() -> {
            border.setCornerRadius(border.getCornerRadius() > 0 ? 0.0 : 12.0);
        });

        WButton colorButton = new WButton("Change border color");
        int[] colorIndex = {0};
        List<WColor> colors = Arrays.asList(
                WColor.BLUE,
                WColor.RED,
                WColor.GREEN,
                WColor.ORANGE);
        colorButton.addActionListener(() -> {
            colorIndex[0] = (colorIndex[0] + 1) % colors.size();
            border.setBorderColor(colors.get(colorIndex[0]));
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(thicknessButton);
        buttons.add(cornerButton);
        buttons.add(colorButton);

        WPanel body = new WPanel(8.0);
        body.add(border);
        body.add(buttons);
        return GalleryScaffold.buildExample("Border and corner rounding (BorderBrush / BorderThickness / CornerRadius)", body);
    }

    /** Background color and padding. */
    private static WComponent buildBorderBackgroundExample() {
        WBorder border = new WBorder(new WLabel("Content with a background"));
        border.setBackground(WColor.LIGHT_GRAY);
        border.setCornerRadius(8.0);
        border.setPadding(16.0);

        WButton paddingButton = new WButton("Increase padding");
        paddingButton.addActionListener(() -> {
            border.setPadding(border.getPadding() >= 48.0 ? 16.0 : border.getPadding() + 16.0);
        });

        WPanel body = new WPanel(8.0);
        body.add(border);
        body.add(paddingButton);
        return GalleryScaffold.buildExample("Background and padding (Background / Padding)", body);
    }

    /** Switching between a gradient background and its angle. */
    private static WComponent buildBorderGradientExample() {
        List<Pair<Double, WColor>> stops = Arrays.asList(
                new Pair<Double, WColor>(0.0, WColor.BLUE),
                new Pair<Double, WColor>(1.0, WColor.PURPLE));

        WLabel gradientLabel = new WLabel("Content with a gradient background");
        gradientLabel.setForeground(WColor.WHITE);
        WBorder border = new WBorder(gradientLabel);
        border.setBackgroundGradient(new WLinearGradientPaint(stops, 90.0));
        border.setCornerRadius(8.0);
        border.setPadding(16.0);
        border.setWidth(320.0);
        border.setHeight(80.0);
        border.setHorizontalAlignment(HorizontalAlignment.LEFT);

        double[] angle = {90.0};
        WButton angleButton = new WButton("Change angle (90°)");
        angleButton.addActionListener(() -> {
            angle[0] = angle[0] >= 270.0 ? 0.0 : angle[0] + 90.0;
            border.setBackgroundGradient(new WLinearGradientPaint(stops, angle[0]));
            angleButton.setText("Change angle (" + (int) angle[0] + "°)");
        });

        WPanel body = new WPanel(8.0);
        body.add(border);
        body.add(angleButton);
        return GalleryScaffold.buildExample("Gradient background (LinearGradientBrush)", body);
    }

    // endregion

    // region Canvas

    /** The Canvas page: lines up demos for trying out WCanvas's various features. */
    static WComponent buildCanvasPage() {
        WPanel page = GalleryScaffold.buildPage("Canvas", "A panel that positions children with absolute coordinates. Try out WCanvas's various features.");

        page.add(buildCanvasPositionExample());
        page.add(buildCanvasZIndexExample());
        return page;
    }

    /** Positioning: moving a child with SetLeft / SetTop. */
    private static WComponent buildCanvasPositionExample() {
        WCanvas canvas = new WCanvas();
        canvas.setWidth(320.0);
        canvas.setHeight(160.0);

        canvas.add(GalleryScaffold.buildTile(WColor.LIGHT_GRAY, 320.0, 160.0), 0.0, 0.0); // the background (visualizes the extent of the canvas)
        WBorder movingTile = GalleryScaffold.buildTile(WColor.BLUE, 48.0, 48.0);
        double[] x = {16.0};
        double[] y = {16.0};
        canvas.add(movingTile, x[0], y[0]);

        WButton moveButton = new WButton("Move the tile");
        moveButton.addActionListener(() -> {
            x[0] = x[0] >= 256.0 ? 16.0 : x[0] + 48.0;
            y[0] = y[0] >= 96.0 ? 16.0 : y[0] + 16.0;
            canvas.setLocation(movingTile, x[0], y[0]);
        });

        WPanel body = new WPanel(8.0);
        body.add(canvas);
        body.add(moveButton);
        return GalleryScaffold.buildExample("Absolute positioning (Canvas.Left / Canvas.Top)", body);
    }

    /** Stacking order: swapping front/back with SetZIndex. */
    private static WComponent buildCanvasZIndexExample() {
        WCanvas canvas = new WCanvas();
        canvas.setWidth(320.0);
        canvas.setHeight(120.0);

        WBorder redTile = GalleryScaffold.buildTile(WColor.RED, 64.0, 64.0);
        WBorder greenTile = GalleryScaffold.buildTile(WColor.GREEN, 64.0, 64.0);
        canvas.add(redTile, 16.0, 16.0);
        canvas.add(greenTile, 48.0, 40.0);

        boolean[] redOnTop = {false};
        WButton swapButton = new WButton("Swap stacking order");
        swapButton.addActionListener(() -> {
            redOnTop[0] = !redOnTop[0];
            canvas.setZIndex(redTile, redOnTop[0] ? 1 : 0);
            canvas.setZIndex(greenTile, redOnTop[0] ? 0 : 1);
        });

        WPanel body = new WPanel(8.0);
        body.add(canvas);
        body.add(swapButton);
        return GalleryScaffold.buildExample("Stacking order (Canvas.ZIndex)", body);
    }

    // endregion

    // region LayoutPanel

    /** The LayoutPanel page: lines up demos for trying out WLayoutPanel + WBorderLayout's various features. */
    static WComponent buildLayoutPanelPage() {
        WPanel page = GalleryScaffold.buildPage(
                "LayoutPanel",
                "A container that positions children Swing LayoutManager-style. Try out WLayoutPanel and WBorderLayout's various features.");

        page.add(buildBorderLayoutBasicExample());
        page.add(buildBorderLayoutDynamicExample());
        page.add(buildBorderLayoutNestedExample());
        return page;
    }

    /** Builds a fixed-size panel for BorderLayout (explicitly left-aligned since it sits inside a vertical WPanel). */
    private static WLayoutPanel buildBorderLayoutPanel(double hgap, double vgap) {
        WLayoutPanel panel = new WLayoutPanel(new WBorderLayout(hgap, vgap));
        panel.setWidth(400.0);
        panel.setHeight(220.0);
        panel.setHorizontalAlignment(HorizontalAlignment.LEFT);
        return panel;
    }

    /** Basic placement across the 5 regions. */
    private static WComponent buildBorderLayoutBasicExample() {
        WLayoutPanel panel = buildBorderLayoutPanel(4.0, 4.0);
        panel.add(GalleryScaffold.buildTile(WColor.BLUE, Double.NaN, Double.NaN, "North"), WBorderLayout.Constraint.NORTH);
        panel.add(GalleryScaffold.buildTile(WColor.GREEN, Double.NaN, Double.NaN, "South"), WBorderLayout.Constraint.SOUTH);
        panel.add(GalleryScaffold.buildTile(WColor.ORANGE, Double.NaN, Double.NaN, "West"), WBorderLayout.Constraint.WEST);
        panel.add(GalleryScaffold.buildTile(WColor.PURPLE, Double.NaN, Double.NaN, "East"), WBorderLayout.Constraint.EAST);
        panel.add(GalleryScaffold.buildTile(WColor.LIGHT_GRAY, Double.NaN, Double.NaN, "Center"), WBorderLayout.Constraint.CENTER);
        return GalleryScaffold.buildExample("Placement across 5 regions (BorderLayout)", panel);
    }

    /** Re-layout in response to visibility toggling, detaching, and content changes. */
    private static WComponent buildBorderLayoutDynamicExample() {
        WLayoutPanel panel = buildBorderLayoutPanel(4.0, 4.0);
        WBorder northTile = GalleryScaffold.buildTile(WColor.BLUE, Double.NaN, Double.NaN, "North");
        WBorder westTile = GalleryScaffold.buildTile(WColor.ORANGE, Double.NaN, Double.NaN, "West");
        panel.add(northTile, WBorderLayout.Constraint.NORTH);
        panel.add(westTile, WBorderLayout.Constraint.WEST);
        panel.add(GalleryScaffold.buildTile(WColor.LIGHT_GRAY, Double.NaN, Double.NaN, "Center"), WBorderLayout.Constraint.CENTER);

        WButton northButton = new WButton("Toggle North visibility");
        northButton.addActionListener(() -> {
            northTile.setVisible(!northTile.isVisible());
            panel.revalidate();
        });

        WButton westButton = new WButton("Detach / re-add West");
        boolean[] westAttached = {true};
        westButton.addActionListener(() -> {
            if (westAttached[0]) {
                panel.remove(westTile);
            } else {
                panel.add(westTile, WBorderLayout.Constraint.WEST);
            }
            westAttached[0] = !westAttached[0];
        });

        WButton widenButton = new WButton("Change West's content");
        boolean[] wideWest = {false};
        widenButton.addActionListener(() -> {
            wideWest[0] = !wideWest[0];
            westTile.setChild(new WLabel(wideWest[0] ? "West (wider content)" : "West"));
            westTile.invalidateNaturalSize();
            panel.revalidate();
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(northButton);
        buttons.add(westButton);
        buttons.add(widenButton);

        WPanel body = new WPanel(8.0);
        body.add(panel);
        body.add(buttons);
        return GalleryScaffold.buildExample("Dynamic changes and re-layout (revalidate / invalidateNaturalSize)", body);
    }

    /** Nesting another WLayoutPanel in CENTER (preferredSize is computed recursively in Kotlin). */
    private static WComponent buildBorderLayoutNestedExample() {
        WLayoutPanel innerPanel = new WLayoutPanel(new WBorderLayout(4.0, 4.0));
        innerPanel.add(GalleryScaffold.buildTile(WColor.YELLOW, Double.NaN, Double.NaN, "Inner North"), WBorderLayout.Constraint.NORTH);
        innerPanel.add(GalleryScaffold.buildTile(WColor.LIGHT_GRAY, Double.NaN, Double.NaN, "Inner Center"), WBorderLayout.Constraint.CENTER);

        WLayoutPanel panel = buildBorderLayoutPanel(4.0, 4.0);
        panel.add(GalleryScaffold.buildTile(WColor.BLUE, Double.NaN, Double.NaN, "North"), WBorderLayout.Constraint.NORTH);
        panel.add(GalleryScaffold.buildTile(WColor.ORANGE, Double.NaN, Double.NaN, "West"), WBorderLayout.Constraint.WEST);
        panel.add(innerPanel, WBorderLayout.Constraint.CENTER);
        return GalleryScaffold.buildExample("Nested LayoutPanel (another BorderLayout in CENTER)", panel);
    }

    // endregion

    // region Expander

    /** The Expander page: lines up demos for trying out WExpander's various features. */
    static WComponent buildExpanderPage() {
        WPanel page = GalleryScaffold.buildPage("Expander", "A control that expands/collapses its content when the header is clicked. Try out WExpander's various features.");

        page.add(buildExpanderBasicExample());
        page.add(buildExpanderDirectionExample());
        return page;
    }

    /** Basic expand/collapse plus the Expanding / Collapsed events. */
    private static WComponent buildExpanderBasicExample() {
        WLabel state = new WLabel("State: collapsed");

        WPanel content = new WPanel(8.0);
        content.add(new WLabel("This is the expanded content."));
        content.add(new WButton("A button inside"));

        WExpander expander = new WExpander("Click to expand or collapse", content);
        expander.setWidth(320.0);
        expander.addExpandListener(() -> {
            state.setText("State: expanded");
        });
        expander.addCollapseListener(() -> {
            state.setText("State: collapsed");
        });

        WButton toggleButton = new WButton("Toggle from code");
        toggleButton.addActionListener(() -> {
            expander.setExpanded(!expander.isExpanded());
        });

        WPanel body = new WPanel(8.0);
        body.add(expander);
        body.add(toggleButton);
        body.add(state);
        return GalleryScaffold.buildExample("Expand/collapse and events (IsExpanded / Expanding / Collapsed)", body);
    }

    /** The expand direction (ExpandDirection). */
    private static WComponent buildExpanderDirectionExample() {
        WExpander expander = new WExpander("A header whose expand direction can change", new WLabel("Can expand either up or down."));
        expander.setWidth(320.0);

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (ExpandDirection direction : ExpandDirection.values()) {
            WButton button = new WButton(direction.name());
            button.addActionListener(() -> {
                expander.setExpandDirection(direction);
            });
            buttons.add(button);
        }

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(expander);
        return GalleryScaffold.buildExample("Expand direction (ExpandDirection)", body);
    }

    // endregion

    // region Grid

    /** The Grid page: lines up demos for trying out WGrid's various features. */
    static WComponent buildGridPage() {
        WPanel page = GalleryScaffold.buildPage("Grid", "A panel that defines rows and columns and places children into cells. Try out WGrid's various features.");

        page.add(buildGridCellExample());
        page.add(buildGridSpanExample());
        return page;
    }

    /** Row/column definitions and cell placement: combining Auto / Pixel / Star. */
    private static WComponent buildGridCellExample() {
        WGrid grid = new WGrid(4.0, 4.0);
        grid.setWidth(320.0);
        grid.addRow(GridLength.pixel(48.0));
        grid.addRow(GridLength.pixel(48.0));
        grid.addColumn(GridLength.pixel(80.0));
        grid.addColumn(GridLength.star());
        grid.addColumn(GridLength.star(2.0));

        grid.add(GalleryScaffold.buildTile(WColor.RED, Double.NaN, Double.NaN, "80px"), 0, 0, 1, 1);
        grid.add(GalleryScaffold.buildTile(WColor.GREEN, Double.NaN, Double.NaN, "1*"), 0, 1, 1, 1);
        grid.add(GalleryScaffold.buildTile(WColor.BLUE, Double.NaN, Double.NaN, "2*"), 0, 2, 1, 1);
        grid.add(GalleryScaffold.buildTile(WColor.ORANGE, Double.NaN, Double.NaN, "Row 2"), 1, 0, 1, 1);
        grid.add(GalleryScaffold.buildTile(WColor.PURPLE, Double.NaN, Double.NaN, "Row 2"), 1, 2, 1, 1);

        return GalleryScaffold.buildExample("Row/column definitions and cell placement (RowDefinitions / ColumnDefinitions)", grid);
    }

    /** RowSpan / ColumnSpan and cell spacing. */
    private static WComponent buildGridSpanExample() {
        WGrid grid = new WGrid(4.0, 4.0);
        grid.setWidth(320.0);
        for (int i = 0; i < 2; i++) {
            grid.addRow(GridLength.pixel(48.0));
        }
        for (int i = 0; i < 3; i++) {
            grid.addColumn(GridLength.star());
        }

        grid.add(GalleryScaffold.buildTile(WColor.BLUE, Double.NaN, Double.NaN, "2 columns"), 0, 0, 1, 2);
        grid.add(GalleryScaffold.buildTile(WColor.GREEN, Double.NaN, Double.NaN, "2 rows"), 0, 2, 2, 1);
        grid.add(GalleryScaffold.buildTile(WColor.RED, Double.NaN, Double.NaN, "1 cell"), 1, 0, 1, 1);
        grid.add(GalleryScaffold.buildTile(WColor.ORANGE, Double.NaN, Double.NaN, "1 cell"), 1, 1, 1, 1);

        WButton spacingButton = new WButton("Toggle cell spacing");
        spacingButton.addActionListener(() -> {
            double next = grid.getRowSpacing() > 4.0 ? 4.0 : 16.0;
            grid.setRowSpacing(next);
            grid.setColumnSpacing(next);
        });

        WPanel body = new WPanel(8.0);
        body.add(grid);
        body.add(spacingButton);
        return GalleryScaffold.buildExample("Cell spans and spacing (RowSpan / ColumnSpan / Spacing)", body);
    }

    // endregion

    // region RelativePanel

    /** The RelativePanel page: lines up demos for trying out WRelativePanel's various features. */
    static WComponent buildRelativePanelPage() {
        WPanel page = GalleryScaffold.buildPage("RelativePanel", "A panel that positions children relative to each other or to the panel. Try out WRelativePanel's various features.");

        page.add(buildRelativePanelSiblingExample());
        page.add(buildRelativePanelAlignExample());
        return page;
    }

    /** Placement relative to sibling elements (RightOf / Below). */
    private static WComponent buildRelativePanelSiblingExample() {
        WRelativePanel panel = new WRelativePanel();
        panel.setWidth(320.0);
        panel.setHeight(160.0);

        WBorder anchor = GalleryScaffold.buildTile(WColor.BLUE, Double.NaN, Double.NaN, "Anchor");
        WBorder right = GalleryScaffold.buildTile(WColor.GREEN, Double.NaN, Double.NaN, "Right");
        WBorder below = GalleryScaffold.buildTile(WColor.RED, Double.NaN, Double.NaN, "Below");
        WBorder rightBelow = GalleryScaffold.buildTile(WColor.ORANGE, Double.NaN, Double.NaN, "Right+Below");

        panel.add(anchor);
        panel.add(right);
        panel.add(below);
        panel.add(rightBelow);

        panel.placeRightOf(right, anchor);
        panel.placeBelow(below, anchor);
        panel.placeRightOf(rightBelow, below);
        panel.placeBelow(rightBelow, right);

        return GalleryScaffold.buildExample("Placement relative to siblings (RightOf / Below)", panel);
    }

    /** Placement relative to the panel (AlignXxxWithPanel). */
    private static WComponent buildRelativePanelAlignExample() {
        WRelativePanel panel = new WRelativePanel();
        panel.setWidth(320.0);
        panel.setHeight(160.0);

        panel.add(GalleryScaffold.buildTile(WColor.LIGHT_GRAY, 320.0, 160.0)); // the background (visualizes the extent of the panel)

        WBorder topRight = GalleryScaffold.buildTile(WColor.GREEN, 48.0, 48.0);
        panel.add(topRight);
        panel.alignRightWithPanel(topRight, true);
        panel.alignTopWithPanel(topRight, true);

        WBorder bottomRight = GalleryScaffold.buildTile(WColor.RED, 48.0, 48.0);
        panel.add(bottomRight);
        panel.alignRightWithPanel(bottomRight, true);
        panel.alignBottomWithPanel(bottomRight, true);

        WBorder center = GalleryScaffold.buildTile(WColor.PURPLE, 48.0, 48.0);
        panel.add(center);
        panel.alignHorizontalCenterWithPanel(center, true);
        panel.alignVerticalCenterWithPanel(center, true);

        return GalleryScaffold.buildExample("Placement relative to the panel (AlignXxxWithPanel)", panel);
    }

    // endregion

    // region SplitView

    /** The SplitView page: lines up demos for trying out WSplitView's various features. */
    static WComponent buildSplitViewPage() {
        WPanel page = GalleryScaffold.buildPage("SplitView", "A control that lines up a collapsible pane alongside content. Try out WSplitView's various features.");

        page.add(buildSplitViewExample());
        return page;
    }

    /** Opening/closing the pane, display mode, and placement. */
    private static WComponent buildSplitViewExample() {
        WPanel paneContent = new WPanel(8.0);
        paneContent.add(new WLabel("Pane"));
        paneContent.add(new WButton("Menu 1"));
        paneContent.add(new WButton("Menu 2"));

        WBorder pane = new WBorder(paneContent);
        pane.setBackground(WColor.LIGHT_GRAY);
        pane.setPadding(8.0);

        WBorder mainContent = new WBorder(new WLabel("Main content"));
        mainContent.setPadding(16.0);

        WSplitView splitView = new WSplitView(pane, mainContent);
        splitView.setWidth(480.0);
        splitView.setHeight(200.0);
        splitView.setOpenPaneLength(160.0);
        splitView.setDisplayMode(SplitViewDisplayMode.INLINE);

        WButton toggleButton = new WButton("Toggle pane");
        toggleButton.addActionListener(() -> {
            splitView.setPaneOpen(!splitView.isPaneOpen());
        });

        WPanel modeButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        modeButtons.add(new WLabel("DisplayMode:"));
        for (SplitViewDisplayMode mode : SplitViewDisplayMode.values()) {
            WButton button = new WButton(mode.name());
            button.addActionListener(() -> {
                splitView.setDisplayMode(mode);
            });
            modeButtons.add(button);
        }

        WButton placementButton = new WButton("Flip pane placement");
        placementButton.addActionListener(() -> {
            splitView.setPanePlacement(
                    splitView.getPanePlacement() == SplitViewPanePlacement.LEFT
                            ? SplitViewPanePlacement.RIGHT
                            : SplitViewPanePlacement.LEFT);
        });

        WPanel body = new WPanel(8.0);
        body.add(splitView);
        body.add(toggleButton);
        body.add(modeButtons);
        body.add(placementButton);
        return GalleryScaffold.buildExample("Opening/closing the pane (IsPaneOpen / DisplayMode / PanePlacement)", body);
    }

    // endregion

    // region StackPanel

    /** The StackPanel page: lines up demos for trying out WPanel's various features. */
    static WComponent buildStackPanelPage() {
        WPanel page = GalleryScaffold.buildPage("StackPanel", "A panel that lines up children in one direction. Try out WPanel's various features.");

        page.add(buildStackPanelExample());
        return page;
    }

    /** The direction children line up in, and the spacing between them. */
    private static WComponent buildStackPanelExample() {
        WPanel panel = new WPanel(8.0);
        panel.add(GalleryScaffold.buildTile(WColor.RED, 48.0, 48.0));
        panel.add(GalleryScaffold.buildTile(WColor.GREEN, 48.0, 48.0));
        panel.add(GalleryScaffold.buildTile(WColor.BLUE, 48.0, 48.0));

        WButton orientationButton = new WButton("Toggle direction");
        orientationButton.addActionListener(() -> {
            panel.setOrientation(
                    panel.getOrientation() == Orientation.VERTICAL
                            ? Orientation.HORIZONTAL
                            : Orientation.VERTICAL);
        });

        WButton spacingButton = new WButton("Toggle spacing");
        spacingButton.addActionListener(() -> {
            panel.setSpacing(panel.getSpacing() > 8.0 ? 8.0 : 24.0);
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(orientationButton);
        buttons.add(spacingButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(panel);
        return GalleryScaffold.buildExample("Direction and spacing (Orientation / Spacing)", body);
    }

    // endregion

    // region VariableSizedWrapGrid

    /** The VariableSizedWrapGrid page: lines up demos for trying out WVariableSizedWrapGrid's various features. */
    static WComponent buildVariableSizedWrapGridPage() {
        WPanel page = GalleryScaffold.buildPage("VariableSizedWrapGrid", "A panel that wraps children by cell. Try out WVariableSizedWrapGrid's various features.");

        page.add(buildWrapGridSpanExample());
        return page;
    }

    /** Cell spans and wrapping. */
    private static WComponent buildWrapGridSpanExample() {
        WVariableSizedWrapGrid grid = new WVariableSizedWrapGrid(56.0, 56.0);
        grid.setOrientation(Orientation.HORIZONTAL);
        grid.setMaximumRowsOrColumns(4);

        grid.add(GalleryScaffold.buildTile(WColor.BLUE, Double.NaN, Double.NaN, "2×2"), 2, 2);
        grid.add(GalleryScaffold.buildTile(WColor.RED, Double.NaN, Double.NaN, "1×1"), 1, 1);
        grid.add(GalleryScaffold.buildTile(WColor.GREEN, Double.NaN, Double.NaN, "1×2"), 1, 2);
        grid.add(GalleryScaffold.buildTile(WColor.ORANGE, Double.NaN, Double.NaN, "1×1"), 1, 1);
        grid.add(GalleryScaffold.buildTile(WColor.PURPLE, Double.NaN, Double.NaN, "2×1"), 2, 1);
        grid.add(GalleryScaffold.buildTile(WColor.GRAY, Double.NaN, Double.NaN, "1×1"), 1, 1);

        WButton maxButton = new WButton("Toggle wrap count");
        maxButton.addActionListener(() -> {
            grid.setMaximumRowsOrColumns(grid.getMaximumRowsOrColumns() == 4 ? 6 : 4);
        });

        WButton orientationButton = new WButton("Toggle direction");
        orientationButton.addActionListener(() -> {
            grid.setOrientation(
                    grid.getOrientation() == Orientation.VERTICAL
                            ? Orientation.HORIZONTAL
                            : Orientation.VERTICAL);
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(maxButton);
        buttons.add(orientationButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(grid);
        return GalleryScaffold.buildExample("Cell spans and wrapping (RowSpan / ColumnSpan / MaximumRowsOrColumns)", body);
    }

    // endregion

    // region SettingsCard

    /** The SettingsCard page: lines up demos for trying out WSettingsCard's various features. */
    static WComponent buildSettingsCardPage() {
        WPanel page = GalleryScaffold.buildPage(
                "SettingsCard",
                "A card that represents a single entry on a settings page (equivalent to the Windows Community Toolkit's SettingsCard). "
                        + "Try out WSettingsCard's various features.");

        page.add(buildSettingsCardBasicExample());
        page.add(buildSettingsCardClickExample());
        page.add(buildSettingsCardWrapExample());
        page.add(buildSettingsCardAlignmentExample());
        page.add(buildSettingsCardStateExample());
        return page;
    }

    /** The basic form: a header, description, and icon on the left, with content on the right. */
    private static WComponent buildSettingsCardBasicExample() {
        WSettingsCard toggleCard = new WSettingsCard("Enable the feature", "Write an explanation under Header and Description, and put the control on the right.");
        toggleCard.setHeaderIcon(Symbol.SETTING);
        toggleCard.setContent(new WToggleSwitch(""));

        WSettingsCard comboCard = new WSettingsCard("Display mode", "Content can hold any component.");
        comboCard.setHeaderIcon(Symbol.VIEW_ALL);
        WComboBox displayModeCombo = new WComboBox(Arrays.asList("Light", "Dark", "Follow the system setting"));
        displayModeCombo.setSelectedIndex(2);
        comboCard.setContent(displayModeCombo);

        WSettingsCard plainCard = new WSettingsCard("A card with only a header", "");

        WPanel body = new WPanel(4.0);
        body.setWidth(SETTINGS_CARD_DEMO_WIDTH);
        body.add(toggleCard);
        body.add(comboCard);
        body.add(plainCard);
        return GalleryScaffold.buildExample("A basic card (Header / Description / HeaderIcon / Content)", body);
    }

    /** A clickable card and its chevron icon (ActionIcon). */
    private static WComponent buildSettingsCardClickExample() {
        WLabel state = new WLabel("Click count: 0");
        int[] count = {0};

        WSettingsCard card = new WSettingsCard("Storage", "With IsClickEnabled=true the whole card becomes a button, and the background changes on hover / press.");
        card.setHeaderIcon(Symbol.SAVE);
        card.setClickEnabled(true);
        card.addActionListener(() -> {
            count[0]++;
            state.setText("Click count: " + count[0]);
        });

        WSettingsCard linkCard = new WSettingsCard("Open a web site", "ActionIcon can be changed to any Symbol, and a tooltip can be attached to it.");
        linkCard.setHeaderIcon(Symbol.GLOBE);
        linkCard.setClickEnabled(true);
        linkCard.setActionIcon(Symbol.GO);
        linkCard.setActionIconToolTip("Opens in the browser");

        WToggleSwitch actionIconSwitch = new WToggleSwitch("Show the chevron icon (IsActionIconVisible)");
        actionIconSwitch.setOn(true);
        actionIconSwitch.addItemListener((isOn) -> {
            card.setActionIconVisible(isOn);
        });

        WPanel body = new WPanel(8.0);
        body.setWidth(SETTINGS_CARD_DEMO_WIDTH);
        body.add(card);
        body.add(linkCard);
        body.add(state);
        body.add(actionIconSwitch);
        return GalleryScaffold.buildExample("A clickable card (IsClickEnabled / ActionIcon / ActionIconToolTip)", body);
    }

    /** Automatic wrapping based on width (RightWrapped / RightWrappedNoIcon). */
    private static WComponent buildSettingsCardWrapExample() {
        WSettingsCard card = new WSettingsCard(
                "A card that wraps by width",
                "At 476px or less the content moves to the row below, and under 286px the icon is hidden as well.");
        card.setHeaderIcon(Symbol.SETTING);
        WComboBox choiceCombo = new WComboBox(Arrays.asList("Option 1", "Option 2"));
        choiceCombo.setSelectedIndex(0);
        card.setContent(choiceCombo);

        WLabel widthLabel = new WLabel("Card width: " + (int) SETTINGS_CARD_DEMO_WIDTH + "px");
        WSlider slider = new WSlider(200.0, SETTINGS_CARD_DEMO_WIDTH, SETTINGS_CARD_DEMO_WIDTH);
        slider.setWidth(SETTINGS_CARD_DEMO_WIDTH);
        slider.addChangeListener((value) -> {
            card.setWidth(value);
            widthLabel.setText("Card width: " + (int) value + "px");
        });

        WPanel body = new WPanel(8.0);
        body.setWidth(SETTINGS_CARD_DEMO_WIDTH);
        body.add(card);
        body.add(slider);
        body.add(widthLabel);
        return GalleryScaffold.buildExample("Automatic wrapping based on width (RightWrapped / RightWrappedNoIcon)", body);
    }

    /** ContentAlignment (Right / Left / Vertical). */
    private static WComponent buildSettingsCardAlignmentExample() {
        WSettingsCard card = new WSettingsCard("Content placement", "With Left the header is hidden, and with Vertical the content fills the width of the row below.");
        card.setHeaderIcon(Symbol.ALIGN_LEFT);
        card.setContent(new WCheckBox("A checkbox as the content"));

        List<String> alignmentNames = new ArrayList<String>();
        for (ContentAlignment alignment : ContentAlignment.values()) {
            alignmentNames.add(alignment.name());
        }
        WComboBox combo = new WComboBox(alignmentNames);
        combo.setSelectedIndex(0);
        combo.addListSelectionListener(() -> {
            int index = combo.getSelectedIndex();
            if (index >= 0) {
                card.setContentAlignment(ContentAlignment.values()[index]);
            }
        });

        WPanel options = new WPanel(8.0, Orientation.HORIZONTAL);
        options.add(GalleryScaffold.optionsLabel("ContentAlignment"));
        options.add(combo);

        WPanel body = new WPanel(8.0);
        body.setWidth(SETTINGS_CARD_DEMO_WIDTH);
        body.add(card);
        body.add(options);
        return GalleryScaffold.buildExample("Content placement (ContentAlignment)", body);
    }

    /** How the disabled state looks. */
    private static WComponent buildSettingsCardStateExample() {
        WSettingsCard card = new WSettingsCard("A card that can be disabled", "With IsEnabled=false the colors switch to the disabled state.");
        card.setHeaderIcon(Symbol.IMPORTANT);
        card.setContent(new WToggleSwitch(""));

        WToggleSwitch enabledSwitch = new WToggleSwitch("Enable the card (IsEnabled)");
        enabledSwitch.setOn(true);
        enabledSwitch.addItemListener((isOn) -> {
            card.setEnabled(isOn);
        });

        WPanel body = new WPanel(8.0);
        body.setWidth(SETTINGS_CARD_DEMO_WIDTH);
        body.add(card);
        body.add(enabledSwitch);
        return GalleryScaffold.buildExample("The disabled state (IsEnabled)", body);
    }

    /** The width of the SettingsCard demos (a width that looks like a real settings page list). */
    private static final double SETTINGS_CARD_DEMO_WIDTH = 560.0;

    // endregion
}
