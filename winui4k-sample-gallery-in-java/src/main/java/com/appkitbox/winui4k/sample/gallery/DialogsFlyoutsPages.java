package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.ContentDialogButton;
import com.appkitbox.winui4k.ContentDialogResult;
import com.appkitbox.winui4k.FlyoutPlacement;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.TeachingTipCloseReason;
import com.appkitbox.winui4k.TeachingTipPlacement;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WBorder;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WContentDialog;
import com.appkitbox.winui4k.WFlyout;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WPopup;
import com.appkitbox.winui4k.WTeachingTip;
import java.util.ArrayList;
import java.util.List;
import kotlin.Unit;

/*
 * Dialogs & flyouts category: demo pages for ContentDialog / Flyout / Popup / TeachingTip.
 */
final class DialogsFlyoutsPages {
    private DialogsFlyoutsPages() {
    }

    // region ContentDialog

    /** The ContentDialog page: lines up demos for trying out WContentDialog's various features. */
    static WComponent buildContentDialogPage() {
        WPanel page = GalleryScaffold.buildPage("ContentDialog", "A modal dialog shown layered inside the window. Try out WContentDialog's various features.");

        page.add(buildSimpleContentDialogExample());
        page.add(buildPrimaryButtonEnabledDialogExample());
        return page;
    }

    /** A basic dialog: 3 buttons plus a default button, and receiving the closed result (ContentDialogResult). */
    private static WComponent buildSimpleContentDialogExample() {
        WLabel result = new WLabel("Result: not shown yet");

        WContentDialog dialog = new WContentDialog("Save your work?", new WLabel("Saving lets you resume from the same state next time."));
        dialog.setPrimaryButtonText("Save");
        dialog.setSecondaryButtonText("Don't save");
        dialog.setCloseButtonText("Cancel");
        dialog.setDefaultButton(ContentDialogButton.PRIMARY);

        WButton showButton = new WButton("Show dialog");
        showButton.addActionListener(() -> {
            dialog.show(showButton, (dialogResult) -> {
                switch (dialogResult) {
                    case PRIMARY:
                        result.setText("Result: Save");
                        break;
                    case SECONDARY:
                        result.setText("Result: Don't save");
                        break;
                    case NONE:
                        result.setText("Result: Cancel");
                        break;
                }
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A basic dialog (PrimaryButton / SecondaryButton / CloseButton / DefaultButton)", row);
    }

    /** A dialog with a consent checkbox: toggling IsPrimaryButtonEnabled from inside the dialog. */
    private static WComponent buildPrimaryButtonEnabledDialogExample() {
        WLabel result = new WLabel("Result: not shown yet");

        WCheckBox agreeCheckBox = new WCheckBox("I agree to the terms of use");
        WPanel dialogContent = new WPanel(8.0, Orientation.VERTICAL);
        dialogContent.add(new WLabel("Agreeing enables the primary button."));
        dialogContent.add(agreeCheckBox);

        WContentDialog dialog = new WContentDialog("Terms of use", dialogContent);
        dialog.setPrimaryButtonText("Continue");
        dialog.setCloseButtonText("Cancel");
        dialog.setDefaultButton(ContentDialogButton.PRIMARY);
        dialog.setPrimaryButtonEnabled(false);
        agreeCheckBox.addItemListener((checked) -> {
            dialog.setPrimaryButtonEnabled(Boolean.TRUE.equals(checked));
            return Unit.INSTANCE;
        });

        WButton showButton = new WButton("Show dialog with consent checkbox");
        showButton.addActionListener(() -> {
            dialog.show(showButton, (dialogResult) -> {
                result.setText(dialogResult == ContentDialogResult.PRIMARY ? "Result: Continue" : "Result: Cancel");
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("Enabling/disabling a button (IsPrimaryButtonEnabled)", row);
    }

    // endregion

    // region Flyout

    /** The Flyout page: lines up demos for trying out WFlyout's various features. */
    static WComponent buildFlyoutPage() {
        WPanel page = GalleryScaffold.buildPage("Flyout", "A lightweight popup for confirmations or supplementary information. Try out WFlyout's various features.");

        page.add(buildSimpleFlyoutExample());
        page.add(buildFlyoutPlacementExample());
        return page;
    }

    /** A basic flyout: setting Button.Flyout and a confirmation UI via hide. */
    private static WComponent buildSimpleFlyoutExample() {
        WLabel result = new WLabel("Not run yet");

        WPanel flyoutContent = new WPanel(8.0, Orientation.VERTICAL);
        WFlyout flyout = new WFlyout(flyoutContent);
        flyoutContent.add(new WLabel("Permanently delete all items?"));
        WButton deleteButton = new WButton("Yes, delete everything");
        deleteButton.addActionListener(() -> {
            result.setText("Deleted");
            flyout.hide();
            return Unit.INSTANCE;
        });
        flyoutContent.add(deleteButton);

        WButton flyoutButton = new WButton("Empty the file");
        flyoutButton.setFlyout(flyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(flyoutButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample("A confirmation flyout attached to a button (Button.Flyout / Hide)", row);
    }

    /** Display position: pick a Placement and open with ShowAt. */
    private static WComponent buildFlyoutPlacementExample() {
        WFlyout flyout = new WFlyout(new WLabel("A flyout for trying out Placement."));

        List<String> placementNames = new ArrayList<>();
        for (FlyoutPlacement placement : FlyoutPlacement.values()) {
            placementNames.add(placement.name());
        }
        WComboBox placementComboBox = new WComboBox(placementNames);
        placementComboBox.setWidth(240.0);
        placementComboBox.setHeader("Placement");
        placementComboBox.setSelectedIndex(FlyoutPlacement.TOP.ordinal());
        placementComboBox.addListSelectionListener(() -> {
            flyout.setPlacement(FlyoutPlacement.values()[placementComboBox.getSelectedIndex()]);
            return Unit.INSTANCE;
        });

        WButton showButton = new WButton("Show flyout");
        showButton.addActionListener(() -> {
            flyout.showAt(showButton);
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(placementComboBox);
        // The ComboBox is taller because of its header, so nudge this down to align with the input box
        showButton.setVerticalAlignment(VerticalAlignment.BOTTOM);
        row.add(showButton);
        return GalleryScaffold.buildExample("Display position (Placement / ShowAt)", row);
    }

    // endregion

    // region Popup

    /** The Popup page: lines up demos for trying out WPopup's various features. */
    static WComponent buildPopupPage() {
        WPanel page = GalleryScaffold.buildPage("Popup", "A lightweight container for showing arbitrary content layered on the window. Try out WPopup's various features.");

        page.add(buildSimplePopupExample());
        return page;
    }

    /** A basic popup: open/close, offset, light dismiss, and the Closed event. */
    private static WComponent buildSimplePopupExample() {
        WLabel result = new WLabel("State: collapsed");

        WPopup popup = new WPopup(null);
        popup.setHorizontalOffset(200.0);
        popup.setVerticalOffset(200.0);
        popup.addCloseListener(() -> {
            result.setText("State: collapsed");
            return Unit.INSTANCE;
        });

        WPanel popupContent = new WPanel(8.0, Orientation.VERTICAL);
        WLabel popupLabel = new WLabel("This is a popup.");
        popupLabel.setFontSize(18.0);
        popupContent.add(popupLabel);
        WButton closeButton = new WButton("Close");
        closeButton.addActionListener(() -> {
            popup.hide();
            return Unit.INSTANCE;
        });
        popupContent.add(closeButton);

        // Popup itself has no decoration, so add a border and background on the content side
        WBorder popupCard = new WBorder(popupContent);
        popupCard.setBackground(GalleryTheme.CARD_BACKGROUND());
        popupCard.setBorderColor(GalleryTheme.CARD_BORDER());
        popupCard.setBorderThickness(1.0);
        popupCard.setCornerRadius(8.0);
        popupCard.setPadding(16.0);
        popup.setChild(popupCard);

        WCheckBox lightDismissCheckBox = new WCheckBox("Close on an outside click (IsLightDismissEnabled)");
        lightDismissCheckBox.addItemListener((checked) -> {
            popup.setLightDismissEnabled(Boolean.TRUE.equals(checked));
            return Unit.INSTANCE;
        });

        WButton showButton = new WButton("Show popup");
        showButton.addActionListener(() -> {
            if (!popup.isOpen()) {
                popup.show(showButton);
                result.setText("State: expanded");
            }
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(lightDismissCheckBox);
        body.add(row);
        return GalleryScaffold.buildExample("A basic popup (IsOpen / Offset / IsLightDismissEnabled / Closed)", body);
    }

    // endregion

    // region TeachingTip

    /** The TeachingTip page: lines up demos for trying out WTeachingTip's various features. */
    static WComponent buildTeachingTipPage() {
        WPanel page = GalleryScaffold.buildPage("TeachingTip", "A control that shows a callout pointing at an element, for things like feature announcements. Try out WTeachingTip's various features.");

        page.add(buildTargetedTeachingTipExample());
        page.add(buildUntargetedTeachingTipExample());
        return page;
    }

    /** A callout with a target: Target / PreferredPlacement / ActionButtonClick / Closed. */
    private static WComponent buildTargetedTeachingTipExample() {
        WLabel result = new WLabel("Not shown yet");

        WButton showButton = new WButton("Save (introduces this feature)");

        WTeachingTip tip = new WTeachingTip("Autosave is available", "Turn on the setting and your edits will be saved automatically.");
        tip.setTarget(showButton);
        tip.setPreferredPlacement(TeachingTipPlacement.BOTTOM);
        tip.setActionButtonText("Turn on");
        tip.setCloseButtonText("Later");
        tip.addActionListener(() -> {
            result.setText("Action: turned on autosave");
            tip.hide();
            return Unit.INSTANCE;
        });
        tip.addCloseListener((reason) -> {
            if (reason != TeachingTipCloseReason.PROGRAMMATIC) {
                result.setText("Reason for closing: " + reason);
            }
            return Unit.INSTANCE;
        });

        showButton.addActionListener(() -> {
            tip.show();
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        result.setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(result);
        row.add(tip); // Placed in the XAML tree, same as the official Gallery (nothing is drawn while it is closed)
        return GalleryScaffold.buildExample("A callout pointing at an element (Target / ActionButtonClick / Closed)", row);
    }

    /** A callout without a target: shown in a screen corner, with light dismiss. */
    private static WComponent buildUntargetedTeachingTipExample() {
        WTeachingTip tip = new WTeachingTip("New feature announcement", "A callout without a target is shown in a screen corner. Closes on an outside click.");
        tip.setLightDismissEnabled(true);

        WButton showButton = new WButton("Show announcement");
        showButton.addActionListener(() -> {
            tip.show();
            return Unit.INSTANCE;
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        row.add(tip); // Placed in the XAML tree, same as the official Gallery (nothing is drawn while it is closed)
        return GalleryScaffold.buildExample("A callout without a target (IsLightDismissEnabled)", row);
    }

    // endregion
}
