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
import com.appkitbox.winui4k.WItemEvent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WPopup;
import com.appkitbox.winui4k.WTeachingTip;
import java.util.ArrayList;
import java.util.List;

/*
 * Dialogs & flyouts category: demo pages for ContentDialog / Flyout / Popup / TeachingTip.
 */
final class DialogsFlyoutsPages {
    private DialogsFlyoutsPages() {
    }

    // region ContentDialog

    /** The ContentDialog page: lines up demos for trying out WContentDialog's various features. */
    static WComponent buildContentDialogPage() {
        WPanel page = GalleryScaffold.buildPage("ContentDialog", Messages.get("contentdialog.description"));

        page.add(buildSimpleContentDialogExample());
        page.add(buildPrimaryButtonEnabledDialogExample());
        return page;
    }

    /** A basic dialog: 3 buttons plus a default button, and receiving the closed result (ContentDialogResult). */
    private static WComponent buildSimpleContentDialogExample() {
        WLabel result = new WLabel(Messages.get("contentdialog.result.notshown"));

        WContentDialog dialog = new WContentDialog(Messages.get("contentdialog.simple.title"), new WLabel(Messages.get("contentdialog.simple.content")));
        dialog.setPrimaryButtonText(Messages.get("contentdialog.simple.save"));
        dialog.setSecondaryButtonText(Messages.get("contentdialog.simple.dontsave"));
        dialog.setCloseButtonText(Messages.get("contentdialog.cancel"));
        dialog.setDefaultButton(ContentDialogButton.PRIMARY);

        WButton showButton = new WButton(Messages.get("contentdialog.simple.show"));
        showButton.addActionListener(() -> {
            dialog.show(showButton, (dialogResult) -> {
                switch (dialogResult) {
                    case PRIMARY:
                        result.setText(Messages.get("contentdialog.result.save"));
                        break;
                    case SECONDARY:
                        result.setText(Messages.get("contentdialog.result.dontsave"));
                        break;
                    case NONE:
                        result.setText(Messages.get("contentdialog.result.cancel"));
                        break;
                }
            });
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("contentdialog.example.simple.title"), row);
    }

    /** A dialog with a consent checkbox: toggling IsPrimaryButtonEnabled from inside the dialog. */
    private static WComponent buildPrimaryButtonEnabledDialogExample() {
        WLabel result = new WLabel(Messages.get("contentdialog.result.notshown"));

        WCheckBox agreeCheckBox = new WCheckBox(Messages.get("contentdialog.terms.agree"));
        WPanel dialogContent = new WPanel(8.0);
        dialogContent.add(new WLabel(Messages.get("contentdialog.terms.content")));
        dialogContent.add(agreeCheckBox);

        WContentDialog dialog = new WContentDialog(Messages.get("contentdialog.terms.title"), dialogContent);
        dialog.setPrimaryButtonText(Messages.get("contentdialog.terms.continue"));
        dialog.setCloseButtonText(Messages.get("contentdialog.cancel"));
        dialog.setDefaultButton(ContentDialogButton.PRIMARY);
        dialog.setPrimaryButtonEnabled(false);
        agreeCheckBox.addItemListener(e -> {
            dialog.setPrimaryButtonEnabled(e.getStateChange() == WItemEvent.SELECTED);
        });

        WButton showButton = new WButton(Messages.get("contentdialog.terms.show"));
        showButton.addActionListener(() -> {
            dialog.show(showButton, (dialogResult) -> {
                result.setText(dialogResult == ContentDialogResult.PRIMARY ? Messages.get("contentdialog.result.continue") : Messages.get("contentdialog.result.cancel"));
            });
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("contentdialog.example.terms.title"), row);
    }

    // endregion

    // region Flyout

    /** The Flyout page: lines up demos for trying out WFlyout's various features. */
    static WComponent buildFlyoutPage() {
        WPanel page = GalleryScaffold.buildPage("Flyout", Messages.get("flyout.description"));

        page.add(buildSimpleFlyoutExample());
        page.add(buildFlyoutPlacementExample());
        return page;
    }

    /** A basic flyout: setting Button.Flyout and a confirmation UI via hide. */
    private static WComponent buildSimpleFlyoutExample() {
        WLabel result = new WLabel(Messages.get("flyout.simple.status.none"));

        WPanel flyoutContent = new WPanel(8.0);
        WFlyout flyout = new WFlyout(flyoutContent);
        flyoutContent.add(new WLabel(Messages.get("flyout.simple.question")));
        WButton deleteButton = new WButton(Messages.get("flyout.simple.confirm"));
        deleteButton.addActionListener(() -> {
            result.setText(Messages.get("flyout.simple.status.deleted"));
            flyout.hide();
        });
        flyoutContent.add(deleteButton);

        WButton flyoutButton = new WButton(Messages.get("flyout.simple.button"));
        flyoutButton.setFlyout(flyout);

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(flyoutButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        return GalleryScaffold.buildExample(Messages.get("flyout.example.simple.title"), row);
    }

    /** Display position: pick a Placement and open with ShowAt. */
    private static WComponent buildFlyoutPlacementExample() {
        WFlyout flyout = new WFlyout(new WLabel(Messages.get("flyout.placement.content")));

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
        });

        WButton showButton = new WButton(Messages.get("flyout.placement.show"));
        showButton.addActionListener(() -> {
            flyout.showAt(showButton);
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(placementComboBox);
        // The ComboBox is taller because of its header, so nudge this down to align with the input box
        showButton.setVerticalAlignmentInParent(VerticalAlignment.BOTTOM);
        row.add(showButton);
        return GalleryScaffold.buildExample(Messages.get("flyout.example.placement.title"), row);
    }

    // endregion

    // region Popup

    /** The Popup page: lines up demos for trying out WPopup's various features. */
    static WComponent buildPopupPage() {
        WPanel page = GalleryScaffold.buildPage("Popup", Messages.get("popup.description"));

        page.add(buildSimplePopupExample());
        return page;
    }

    /** A basic popup: open/close, offset, light dismiss, and the Closed event. */
    private static WComponent buildSimplePopupExample() {
        WLabel result = new WLabel(Messages.get("popup.status.closed"));

        WPopup popup = new WPopup();
        popup.setHorizontalOffset(200.0);
        popup.setVerticalOffset(200.0);
        popup.addCloseListener(() -> {
            result.setText(Messages.get("popup.status.closed"));
        });

        WPanel popupContent = new WPanel(8.0);
        WLabel popupLabel = new WLabel(Messages.get("popup.content.text"));
        popupLabel.setFontSize(18.0);
        popupContent.add(popupLabel);
        WButton closeButton = new WButton(Messages.get("popup.content.close"));
        closeButton.addActionListener(() -> {
            popup.hide();
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

        WCheckBox lightDismissCheckBox = new WCheckBox(Messages.get("popup.lightdismiss"));
        lightDismissCheckBox.addItemListener(e -> {
            popup.setLightDismissEnabled(e.getStateChange() == WItemEvent.SELECTED);
        });

        WButton showButton = new WButton(Messages.get("popup.show"));
        showButton.addActionListener(() -> {
            if (!popup.isOpen()) {
                popup.show(showButton);
                result.setText(Messages.get("popup.status.open"));
            }
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);

        WPanel body = new WPanel(8.0);
        body.add(lightDismissCheckBox);
        body.add(row);
        return GalleryScaffold.buildExample(Messages.get("popup.example.simple.title"), body);
    }

    // endregion

    // region TeachingTip

    /** The TeachingTip page: lines up demos for trying out WTeachingTip's various features. */
    static WComponent buildTeachingTipPage() {
        WPanel page = GalleryScaffold.buildPage("TeachingTip", Messages.get("teachingtip.description"));

        page.add(buildTargetedTeachingTipExample());
        page.add(buildUntargetedTeachingTipExample());
        return page;
    }

    /** A callout with a target: Target / PreferredPlacement / ActionButtonClick / Closed. */
    private static WComponent buildTargetedTeachingTipExample() {
        WLabel result = new WLabel(Messages.get("teachingtip.targeted.status.none"));

        WButton showButton = new WButton(Messages.get("teachingtip.targeted.button"));

        WTeachingTip tip = new WTeachingTip(Messages.get("teachingtip.targeted.title"), Messages.get("teachingtip.targeted.subtitle"));
        tip.setTarget(showButton);
        tip.setPreferredPlacement(TeachingTipPlacement.BOTTOM);
        tip.setActionButtonText(Messages.get("teachingtip.targeted.action"));
        tip.setCloseButtonText(Messages.get("teachingtip.targeted.close"));
        tip.addActionListener(() -> {
            result.setText(Messages.get("teachingtip.targeted.status.action"));
            tip.hide();
        });
        tip.addCloseListener((reason) -> {
            if (reason != TeachingTipCloseReason.PROGRAMMATIC) {
                result.setText(Messages.get("teachingtip.result.closereason", reason.toString()));
            }
        });

        showButton.addActionListener(() -> {
            tip.show();
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        result.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        row.add(result);
        row.add(tip); // Placed in the XAML tree, same as the official Gallery (nothing is drawn while it is closed)
        return GalleryScaffold.buildExample(Messages.get("teachingtip.example.targeted.title"), row);
    }

    /** A callout without a target: shown in a screen corner, with light dismiss. */
    private static WComponent buildUntargetedTeachingTipExample() {
        WTeachingTip tip = new WTeachingTip(Messages.get("teachingtip.untargeted.title"), Messages.get("teachingtip.untargeted.subtitle"));
        tip.setLightDismissEnabled(true);

        WButton showButton = new WButton(Messages.get("teachingtip.untargeted.show"));
        showButton.addActionListener(() -> {
            tip.show();
        });

        WPanel row = new WPanel(16.0, Orientation.HORIZONTAL);
        row.add(showButton);
        row.add(tip); // Placed in the XAML tree, same as the official Gallery (nothing is drawn while it is closed)
        return GalleryScaffold.buildExample(Messages.get("teachingtip.example.untargeted.title"), row);
    }

    // endregion
}
