package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.InfoBarSeverity;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.Symbol;
import com.appkitbox.winui4k.ToolTipPlacement;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WInfoBadge;
import com.appkitbox.winui4k.WInfoBar;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WProgressBar;
import com.appkitbox.winui4k.WProgressRing;
import com.appkitbox.winui4k.WSlider;
import com.appkitbox.winui4k.WToolTip;

import kotlin.Unit;

/*
 * Status & info category: demo pages for InfoBadge / InfoBar / ProgressBar / ProgressRing / ToolTip.
 */
final class StatusInfoPages {
    private StatusInfoPages() {
    }

    // region InfoBadge

    /** The InfoBadge page: lines up demos for trying out WInfoBadge's various features. */
    static WComponent buildInfoBadgePage() {
        WPanel page = GalleryScaffold.buildPage(
                "InfoBadge",
                "A small badge that unobtrusively shows an unread count or draws attention. Try out WInfoBadge's various features.");

        page.add(buildInfoBadgeKindsExample());
        page.add(buildInfoBadgeDynamicExample());
        return page;
    }

    /** The kinds of badge: dot badge / numeric badge / icon badge. */
    private static WComponent buildInfoBadgeKindsExample() {
        WInfoBadge dot = new WInfoBadge(); // the default value of -1 makes it a dot badge

        WInfoBadge number = new WInfoBadge();
        number.setValue(5);

        WInfoBadge icon = new WInfoBadge();
        icon.setSymbolIcon(Symbol.MESSAGE);

        WPanel row = new WPanel(24.0, Orientation.HORIZONTAL);
        row.add(labeledColumn("Dot badge", dot));
        row.add(labeledColumn("Numeric badge", number));
        row.add(labeledColumn("Icon badge", icon));
        return GalleryScaffold.buildExample("Kinds of badge (Value = -1 / a number / IconSource)", row);
    }

    /** A dynamic badge: increment/reset the number with buttons. */
    private static WComponent buildInfoBadgeDynamicExample() {
        WInfoBadge badge = new WInfoBadge();
        badge.setValue(0);

        WButton addButton = new WButton("+1");
        addButton.addActionListener(() -> {
            badge.setValue(badge.getValue() + 1);
            return Unit.INSTANCE;
        });

        WButton clearButton = new WButton("Reset to 0");
        clearButton.addActionListener(() -> {
            badge.setValue(0);
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(labeledColumn("Unread", badge));
        addButton.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(addButton);
        clearButton.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(clearButton);
        return GalleryScaffold.buildExample("Updating the number (Value)", row);
    }

    /** A small column that stacks a control under a label (used by the badge demos). */
    private static WComponent labeledColumn(String label, WComponent control) {
        WPanel column = new WPanel(6.0, Orientation.VERTICAL);
        column.setHorizontalAlignment(HorizontalAlignment.LEFT);
        WLabel labelText = new WLabel(label);
        labelText.setForeground(GalleryTheme.TEXT_SECONDARY());
        column.add(labelText);
        control.setHorizontalAlignment(HorizontalAlignment.LEFT);
        column.add(control);
        return column;
    }

    // endregion

    // region InfoBar

    /** The InfoBar page: lines up demos for trying out WInfoBar's various features. */
    static WComponent buildInfoBarPage() {
        WPanel page = GalleryScaffold.buildPage(
                "InfoBar",
                "An inline notification bar that reports an in-app state change. Try out WInfoBar's various features.");

        page.add(buildInfoBarSeverityExample());
        page.add(buildInfoBarInteractiveExample());
        page.add(buildInfoBarActionExample());
        return page;
    }

    /** Severity: lines up all four of Informational / Success / Warning / Error. */
    private static WComponent buildInfoBarSeverityExample() {
        WPanel column = new WPanel(12.0, Orientation.VERTICAL);
        InfoBarSeverity[] severities = {
                InfoBarSeverity.INFORMATIONAL,
                InfoBarSeverity.SUCCESS,
                InfoBarSeverity.WARNING,
                InfoBarSeverity.ERROR,
        };
        String[][] texts = {
                {"Informational", "This is an informational message."},
                {"Success", "Your changes have been saved."},
                {"Warning", "The connection is unstable."},
                {"Error", "The save failed."},
        };
        for (int i = 0; i < severities.length; i++) {
            WInfoBar infoBar = new WInfoBar();
            infoBar.setSeverity(severities[i]);
            infoBar.setTitle(texts[i][0]);
            infoBar.setMessage(texts[i][1]);
            infoBar.setClosable(false);
            infoBar.setOpen(true);
            infoBar.setWidth(500.0);
            column.add(infoBar);
        }
        return GalleryScaffold.buildExample("Severity (Severity: Informational / Success / Warning / Error)", column);
    }

    /** Opening and closing: toggle isOpen with a button and receive close (x) button clicks. */
    private static WComponent buildInfoBarInteractiveExample() {
        WLabel result = new WLabel("The bar is closed");

        WInfoBar infoBar = new WInfoBar();
        infoBar.setSeverity(InfoBarSeverity.SUCCESS);
        infoBar.setTitle("Download complete");
        infoBar.setMessage("The file has finished downloading.");
        infoBar.setWidth(500.0);
        infoBar.addCloseButtonListener(() -> {
            result.setText("Closed via the close (x) button");
            return Unit.INSTANCE;
        });

        WButton toggleButton = new WButton("Open the bar");
        toggleButton.addActionListener(() -> {
            infoBar.setOpen(!infoBar.isOpen());
            toggleButton.setText(infoBar.isOpen() ? "Close the bar" : "Open the bar");
            result.setText(infoBar.isOpen() ? "The bar is open" : "The bar is closed");
            return Unit.INSTANCE;
        });

        WPanel column = new WPanel(12.0, Orientation.VERTICAL);
        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(toggleButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        column.add(row);
        column.add(infoBar);
        return GalleryScaffold.buildExample("Opening/closing and the close button (IsOpen / CloseButtonClick)", column);
    }

    /** An action button: places an action button next to the body text. */
    private static WComponent buildInfoBarActionExample() {
        WLabel result = new WLabel("");

        WButton actionButton = new WButton("Restart");
        actionButton.addActionListener(() -> {
            result.setText("A restart was requested");
            return Unit.INSTANCE;
        });

        WInfoBar infoBar = new WInfoBar();
        infoBar.setSeverity(InfoBarSeverity.WARNING);
        infoBar.setTitle("An update is pending");
        infoBar.setMessage("Restart the app to apply the changes.");
        infoBar.setActionButton(actionButton);
        infoBar.setClosable(false);
        infoBar.setOpen(true);
        infoBar.setWidth(500.0);

        WPanel column = new WPanel(12.0, Orientation.VERTICAL);
        column.add(infoBar);
        column.add(result);
        return GalleryScaffold.buildExample("Action button (ActionButton)", column);
    }

    // endregion

    // region ProgressBar

    /** The ProgressBar page: lines up demos for trying out WProgressBar's various features. */
    static WComponent buildProgressBarPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ProgressBar",
                "A control that shows a task's progress as a bar. Try out WProgressBar's various features.");

        page.add(buildDeterminateProgressBarExample());
        page.add(buildIndeterminateProgressBarExample());
        page.add(buildProgressBarStateExample());
        return page;
    }

    /** Determinate progress: drag a slider to move value and reflect it on the bar. */
    private static WComponent buildDeterminateProgressBarExample() {
        WProgressBar progressBar = new WProgressBar(0.0, 100.0, 40.0);
        progressBar.setWidth(300.0);

        WLabel label = new WLabel("40 %");
        WSlider slider = new WSlider(0.0, 100.0, 40.0);
        slider.setWidth(300.0);
        slider.addChangeListener(value -> {
            progressBar.setValue(value);
            label.setText(value.intValue() + " %");
            return Unit.INSTANCE;
        });

        WPanel column = new WPanel(12.0, Orientation.VERTICAL);
        column.add(progressBar);
        column.add(slider);
        column.add(label);
        return GalleryScaffold.buildExample("Determinate progress (Value / Minimum / Maximum)", column);
    }

    /** Indeterminate progress: indicates a task whose completion time is unknown. */
    private static WComponent buildIndeterminateProgressBarExample() {
        WProgressBar progressBar = new WProgressBar(0.0, 100.0, 0.0);
        progressBar.setWidth(300.0);
        progressBar.setIndeterminate(true);

        WButton toggleButton = new WButton("Stop");
        toggleButton.addActionListener(() -> {
            progressBar.setIndeterminate(!progressBar.isIndeterminate());
            toggleButton.setText(progressBar.isIndeterminate() ? "Stop" : "Start");
            return Unit.INSTANCE;
        });

        WPanel column = new WPanel(12.0, Orientation.VERTICAL);
        column.add(progressBar);
        toggleButton.setHorizontalAlignment(HorizontalAlignment.LEFT);
        column.add(toggleButton);
        return GalleryScaffold.buildExample("Indeterminate progress (IsIndeterminate)", column);
    }

    /** Visual states: toggle the showError / showPaused visual states. */
    private static WComponent buildProgressBarStateExample() {
        WProgressBar progressBar = new WProgressBar(0.0, 100.0, 60.0);
        progressBar.setWidth(300.0);

        WButton pauseButton = new WButton("Pause");
        pauseButton.addActionListener(() -> {
            progressBar.setShowPaused(!progressBar.getShowPaused());
            pauseButton.setText(progressBar.getShowPaused() ? "Resume" : "Pause");
            return Unit.INSTANCE;
        });

        WButton errorButton = new WButton("Error");
        errorButton.addActionListener(() -> {
            progressBar.setShowError(!progressBar.getShowError());
            errorButton.setText(progressBar.getShowError() ? "Clear error" : "Error");
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(pauseButton);
        buttons.add(errorButton);

        WPanel column = new WPanel(12.0, Orientation.VERTICAL);
        column.add(progressBar);
        column.add(buttons);
        return GalleryScaffold.buildExample("Visual states (ShowPaused / ShowError)", column);
    }

    // endregion

    // region ProgressRing

    /** The ProgressRing page: lines up demos for trying out WProgressRing's various features. */
    static WComponent buildProgressRingPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ProgressRing",
                "A control that shows an ongoing operation or progress as a circular ring. Try out WProgressRing's various features.");

        page.add(buildIndeterminateProgressRingExample());
        page.add(buildDeterminateProgressRingExample());
        return page;
    }

    /** Indeterminate progress: toggle the spinning animation with IsActive. */
    private static WComponent buildIndeterminateProgressRingExample() {
        WProgressRing progressRing = new WProgressRing();
        progressRing.setWidth(60.0);
        progressRing.setHeight(60.0);
        progressRing.setActive(true);

        WButton toggleButton = new WButton("Stop");
        toggleButton.addActionListener(() -> {
            progressRing.setActive(!progressRing.isActive());
            toggleButton.setText(progressRing.isActive() ? "Stop" : "Start");
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(progressRing);
        toggleButton.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(toggleButton);
        return GalleryScaffold.buildExample("Indeterminate progress (IsActive / IsIndeterminate)", row);
    }

    /** Determinate progress: drag a slider to move value and reflect it on the ring. */
    private static WComponent buildDeterminateProgressRingExample() {
        WProgressRing progressRing = new WProgressRing();
        progressRing.setWidth(60.0);
        progressRing.setHeight(60.0);
        progressRing.setIndeterminate(false);
        progressRing.setValue(40.0);

        WLabel label = new WLabel("40 %");
        WSlider slider = new WSlider(0.0, 100.0, 40.0);
        slider.setWidth(300.0);
        slider.addChangeListener(value -> {
            progressRing.setValue(value);
            label.setText(value.intValue() + " %");
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(progressRing);
        WPanel column = new WPanel(12.0, Orientation.VERTICAL);
        column.add(slider);
        column.add(label);
        column.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(column);
        return GalleryScaffold.buildExample("Determinate progress (IsIndeterminate = false / Value)", row);
    }

    // endregion

    // region ToolTip

    /** The ToolTip page: lines up demos for trying out WComponent.toolTip / WToolTip's various features. */
    static WComponent buildToolTipPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ToolTip",
                "Shows a floating explanation of an element on hover. Try out the toolTip property and WToolTip.");

        page.add(buildSimpleToolTipExample());
        page.add(buildPlacementToolTipExample());
        page.add(buildRichToolTipExample());
        return page;
    }

    /** A string hint: hover over the toolTip property to see the explanation. */
    private static WComponent buildSimpleToolTipExample() {
        WButton button = new WButton("Hover over me");
        button.setToolTip("This is a plain string tooltip.");
        return GalleryScaffold.buildExample("A string hint (WComponent.toolTip)", button);
    }

    /** Specifying placement: choose WToolTip's placement from top / bottom / left / right. */
    private static WComponent buildPlacementToolTipExample() {
        WPanel row = new WPanel(12.0, Orientation.HORIZONTAL);
        String[] labels = {"Top", "Bottom", "Left", "Right"};
        ToolTipPlacement[] placements = {
                ToolTipPlacement.TOP,
                ToolTipPlacement.BOTTOM,
                ToolTipPlacement.LEFT,
                ToolTipPlacement.RIGHT,
        };
        for (int i = 0; i < labels.length; i++) {
            WButton button = new WButton(labels[i]);
            WToolTip toolTip = new WToolTip();
            toolTip.setText("Shown to the " + labels[i]);
            toolTip.setPlacement(placements[i]);
            button.setToolTip(toolTip);
            row.add(button);
        }
        return GalleryScaffold.buildExample("Specifying placement (WToolTip.placement)", row);
    }

    /** A non-string hint: put an arbitrary component in WToolTip. */
    private static WComponent buildRichToolTipExample() {
        WButton button = new WButton("Rich hint");

        WPanel content = new WPanel(4.0, Orientation.VERTICAL);
        WLabel heading = new WLabel("Heading");
        heading.setFontWeight(600);
        content.add(heading);
        WLabel description = new WLabel("The explanation can span multiple lines.");
        description.setForeground(GalleryTheme.TEXT_SECONDARY());
        content.add(description);

        WToolTip toolTip = new WToolTip();
        toolTip.setContent(content);
        button.setToolTip(toolTip);
        return GalleryScaffold.buildExample("A non-string hint (WToolTip.content)", button);
    }

    // endregion
}
