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
                Messages.get("infobadge.description"));

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
        row.add(labeledColumn(Messages.get("infobadge.kinds.dot"), dot));
        row.add(labeledColumn(Messages.get("infobadge.kinds.number"), number));
        row.add(labeledColumn(Messages.get("infobadge.kinds.icon"), icon));
        return GalleryScaffold.buildExample(Messages.get("infobadge.example.kinds.title"), row);
    }

    /** A dynamic badge: increment/reset the number with buttons. */
    private static WComponent buildInfoBadgeDynamicExample() {
        WInfoBadge badge = new WInfoBadge();
        badge.setValue(0);

        WButton addButton = new WButton("+1");
        addButton.addActionListener(() -> {
            badge.setValue(badge.getValue() + 1);
        });

        WButton clearButton = new WButton(Messages.get("infobadge.dynamic.reset"));
        clearButton.addActionListener(() -> {
            badge.setValue(0);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(labeledColumn(Messages.get("infobadge.dynamic.unread"), badge));
        addButton.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(addButton);
        clearButton.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(clearButton);
        return GalleryScaffold.buildExample(Messages.get("infobadge.example.dynamic.title"), row);
    }

    /** A small column that stacks a control under a label (used by the badge demos). */
    private static WComponent labeledColumn(String label, WComponent control) {
        WPanel column = new WPanel(6.0);
        column.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
        WLabel labelText = new WLabel(label);
        labelText.setForeground(GalleryTheme.TEXT_SECONDARY());
        column.add(labelText);
        control.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
        column.add(control);
        return column;
    }

    // endregion

    // region InfoBar

    /** The InfoBar page: lines up demos for trying out WInfoBar's various features. */
    static WComponent buildInfoBarPage() {
        WPanel page = GalleryScaffold.buildPage(
                "InfoBar",
                Messages.get("infobar.description"));

        page.add(buildInfoBarSeverityExample());
        page.add(buildInfoBarInteractiveExample());
        page.add(buildInfoBarActionExample());
        return page;
    }

    /** Severity: lines up all four of Informational / Success / Warning / Error. */
    private static WComponent buildInfoBarSeverityExample() {
        WPanel column = new WPanel(12.0);
        InfoBarSeverity[] severities = {
                InfoBarSeverity.INFORMATIONAL,
                InfoBarSeverity.SUCCESS,
                InfoBarSeverity.WARNING,
                InfoBarSeverity.ERROR,
        };
        String[][] texts = {
                {Messages.get("infobar.severity.informational.title"), Messages.get("infobar.severity.informational.message")},
                {Messages.get("infobar.severity.success.title"), Messages.get("infobar.severity.success.message")},
                {Messages.get("infobar.severity.warning.title"), Messages.get("infobar.severity.warning.message")},
                {Messages.get("infobar.severity.error.title"), Messages.get("infobar.severity.error.message")},
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
        return GalleryScaffold.buildExample(Messages.get("infobar.example.severity.title"), column);
    }

    /** Opening and closing: toggle isOpen with a button and receive close (x) button clicks. */
    private static WComponent buildInfoBarInteractiveExample() {
        WLabel result = new WLabel(Messages.get("infobar.interactive.status.closed"));

        WInfoBar infoBar = new WInfoBar();
        infoBar.setSeverity(InfoBarSeverity.SUCCESS);
        infoBar.setTitle(Messages.get("infobar.interactive.title"));
        infoBar.setMessage(Messages.get("infobar.interactive.message"));
        infoBar.setWidth(500.0);
        infoBar.addCloseButtonListener(() -> {
            result.setText(Messages.get("infobar.interactive.status.closebutton"));
        });

        WButton toggleButton = new WButton(Messages.get("infobar.interactive.open"));
        toggleButton.addActionListener(() -> {
            infoBar.setOpen(!infoBar.isOpen());
            toggleButton.setText(infoBar.isOpen() ? Messages.get("infobar.interactive.close") : Messages.get("infobar.interactive.open"));
            result.setText(infoBar.isOpen() ? Messages.get("infobar.interactive.status.open") : Messages.get("infobar.interactive.status.closed"));
        });

        WPanel column = new WPanel(12.0);
        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(toggleButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        column.add(row);
        column.add(infoBar);
        return GalleryScaffold.buildExample(Messages.get("infobar.example.interactive.title"), column);
    }

    /** An action button: places an action button next to the body text. */
    private static WComponent buildInfoBarActionExample() {
        WLabel result = new WLabel("");

        WButton actionButton = new WButton(Messages.get("infobar.action.restart"));
        actionButton.addActionListener(() -> {
            result.setText(Messages.get("infobar.action.status"));
        });

        WInfoBar infoBar = new WInfoBar();
        infoBar.setSeverity(InfoBarSeverity.WARNING);
        infoBar.setTitle(Messages.get("infobar.action.title"));
        infoBar.setMessage(Messages.get("infobar.action.message"));
        infoBar.setActionButton(actionButton);
        infoBar.setClosable(false);
        infoBar.setOpen(true);
        infoBar.setWidth(500.0);

        WPanel column = new WPanel(12.0);
        column.add(infoBar);
        column.add(result);
        return GalleryScaffold.buildExample(Messages.get("infobar.example.action.title"), column);
    }

    // endregion

    // region ProgressBar

    /** The ProgressBar page: lines up demos for trying out WProgressBar's various features. */
    static WComponent buildProgressBarPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ProgressBar",
                Messages.get("progressbar.description"));

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
        slider.addChangeListener(e -> {
            double value = slider.getValue();
            progressBar.setValue(value);
            label.setText((int) value + " %");
        });

        WPanel column = new WPanel(12.0);
        column.add(progressBar);
        column.add(slider);
        column.add(label);
        return GalleryScaffold.buildExample(Messages.get("progressbar.example.determinate.title"), column);
    }

    /** Indeterminate progress: indicates a task whose completion time is unknown. */
    private static WComponent buildIndeterminateProgressBarExample() {
        WProgressBar progressBar = new WProgressBar(0.0, 100.0, 0.0);
        progressBar.setWidth(300.0);
        progressBar.setIndeterminate(true);

        WButton toggleButton = new WButton(Messages.get("progressbar.stop"));
        toggleButton.addActionListener(() -> {
            progressBar.setIndeterminate(!progressBar.isIndeterminate());
            toggleButton.setText(progressBar.isIndeterminate() ? Messages.get("progressbar.stop") : Messages.get("progressbar.start"));
        });

        WPanel column = new WPanel(12.0);
        column.add(progressBar);
        toggleButton.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
        column.add(toggleButton);
        return GalleryScaffold.buildExample(Messages.get("progressbar.example.indeterminate.title"), column);
    }

    /** Visual states: toggle the showError / showPaused visual states. */
    private static WComponent buildProgressBarStateExample() {
        WProgressBar progressBar = new WProgressBar(0.0, 100.0, 60.0);
        progressBar.setWidth(300.0);

        WButton pauseButton = new WButton(Messages.get("progressbar.pause"));
        pauseButton.addActionListener(() -> {
            progressBar.setShowPaused(!progressBar.getShowPaused());
            pauseButton.setText(progressBar.getShowPaused() ? Messages.get("progressbar.resume") : Messages.get("progressbar.pause"));
        });

        WButton errorButton = new WButton(Messages.get("progressbar.error"));
        errorButton.addActionListener(() -> {
            progressBar.setShowError(!progressBar.getShowError());
            errorButton.setText(progressBar.getShowError() ? Messages.get("progressbar.clearerror") : Messages.get("progressbar.error"));
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(pauseButton);
        buttons.add(errorButton);

        WPanel column = new WPanel(12.0);
        column.add(progressBar);
        column.add(buttons);
        return GalleryScaffold.buildExample(Messages.get("progressbar.example.state.title"), column);
    }

    // endregion

    // region ProgressRing

    /** The ProgressRing page: lines up demos for trying out WProgressRing's various features. */
    static WComponent buildProgressRingPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ProgressRing",
                Messages.get("progressring.description"));

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

        WButton toggleButton = new WButton(Messages.get("progressring.stop"));
        toggleButton.addActionListener(() -> {
            progressRing.setActive(!progressRing.isActive());
            toggleButton.setText(progressRing.isActive() ? Messages.get("progressring.stop") : Messages.get("progressring.start"));
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(progressRing);
        toggleButton.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(toggleButton);
        return GalleryScaffold.buildExample(Messages.get("progressring.example.indeterminate.title"), row);
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
        slider.addChangeListener(e -> {
            double value = slider.getValue();
            progressRing.setValue(value);
            label.setText((int) value + " %");
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(progressRing);
        WPanel column = new WPanel(12.0);
        column.add(slider);
        column.add(label);
        column.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(column);
        return GalleryScaffold.buildExample(Messages.get("progressring.example.determinate.title"), row);
    }

    // endregion

    // region ToolTip

    /** The ToolTip page: lines up demos for trying out WComponent.toolTipText / WComponent.toolTip (WToolTip)'s various features. */
    static WComponent buildToolTipPage() {
        WPanel page = GalleryScaffold.buildPage(
                "ToolTip",
                Messages.get("tooltip.description"));

        page.add(buildSimpleToolTipExample());
        page.add(buildPlacementToolTipExample());
        page.add(buildRichToolTipExample());
        return page;
    }

    /** A string hint: hover over the toolTip property to see the explanation. */
    private static WComponent buildSimpleToolTipExample() {
        WButton button = new WButton(Messages.get("tooltip.simple.button"));
        button.setToolTipText(Messages.get("tooltip.simple.tip"));
        return GalleryScaffold.buildExample(Messages.get("tooltip.example.simple.title"), button);
    }

    /** Specifying placement: choose WToolTip's placement from top / bottom / left / right. */
    private static WComponent buildPlacementToolTipExample() {
        WPanel row = new WPanel(12.0, Orientation.HORIZONTAL);
        String[] labels = {
                Messages.get("tooltip.placement.top"),
                Messages.get("tooltip.placement.bottom"),
                Messages.get("tooltip.placement.left"),
                Messages.get("tooltip.placement.right"),
        };
        String[] tipTexts = {
                Messages.get("tooltip.placement.top.tip"),
                Messages.get("tooltip.placement.bottom.tip"),
                Messages.get("tooltip.placement.left.tip"),
                Messages.get("tooltip.placement.right.tip"),
        };
        ToolTipPlacement[] placements = {
                ToolTipPlacement.TOP,
                ToolTipPlacement.BOTTOM,
                ToolTipPlacement.LEFT,
                ToolTipPlacement.RIGHT,
        };
        for (int i = 0; i < labels.length; i++) {
            WButton button = new WButton(labels[i]);
            WToolTip toolTip = new WToolTip();
            toolTip.setText(tipTexts[i]);
            toolTip.setPlacement(placements[i]);
            button.setToolTip(toolTip);
            row.add(button);
        }
        return GalleryScaffold.buildExample(Messages.get("tooltip.example.placement.title"), row);
    }

    /** A non-string hint: put an arbitrary component in WToolTip. */
    private static WComponent buildRichToolTipExample() {
        WButton button = new WButton(Messages.get("tooltip.rich.button"));

        WPanel content = new WPanel(4.0);
        WLabel heading = new WLabel(Messages.get("tooltip.rich.heading"));
        heading.setFontWeight(600);
        content.add(heading);
        WLabel description = new WLabel(Messages.get("tooltip.rich.body"));
        description.setForeground(GalleryTheme.TEXT_SECONDARY());
        content.add(description);

        WToolTip toolTip = new WToolTip();
        toolTip.setContent(content);
        button.setToolTip(toolTip);
        return GalleryScaffold.buildExample(Messages.get("tooltip.example.rich.title"), button);
    }

    // endregion
}
