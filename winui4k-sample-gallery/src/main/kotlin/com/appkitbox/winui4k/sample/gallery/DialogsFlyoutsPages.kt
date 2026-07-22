package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.ContentDialogButton
import com.appkitbox.winui4k.ContentDialogResult
import com.appkitbox.winui4k.FlyoutPlacement
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TeachingTipCloseReason
import com.appkitbox.winui4k.TeachingTipPlacement
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WBorder
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WContentDialog
import com.appkitbox.winui4k.WFlyout
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WPopup
import com.appkitbox.winui4k.WTeachingTip

/*
 * Dialogs & flyouts category: demo pages for ContentDialog / Flyout / Popup / TeachingTip.
 */

// region ContentDialog

/** The ContentDialog page: lines up demos for trying out WContentDialog's various features. */
internal fun buildContentDialogPage(): WComponent {
    val page = buildPage("ContentDialog", "A modal dialog shown layered inside the window. Try out WContentDialog's various features.")

    page.add(buildSimpleContentDialogExample())
    page.add(buildPrimaryButtonEnabledDialogExample())
    return page
}

/** A basic dialog: 3 buttons plus a default button, and receiving the closed result (ContentDialogResult). */
private fun buildSimpleContentDialogExample(): WComponent {
    val result = WLabel("Result: not shown yet")

    val dialog = WContentDialog("Save your work?", WLabel("Saving lets you resume from the same state next time."))
    dialog.primaryButtonText = "Save"
    dialog.secondaryButtonText = "Don't save"
    dialog.closeButtonText = "Cancel"
    dialog.defaultButton = ContentDialogButton.PRIMARY

    val showButton = WButton("Show dialog")
    showButton.addActionListener {
        dialog.show(showButton) { dialogResult ->
            result.text = when (dialogResult) {
                ContentDialogResult.PRIMARY -> "Result: Save"
                ContentDialogResult.SECONDARY -> "Result: Don't save"
                ContentDialogResult.NONE -> "Result: Cancel"
            }
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A basic dialog (PrimaryButton / SecondaryButton / CloseButton / DefaultButton)", row)
}

/** A dialog with a consent checkbox: toggling IsPrimaryButtonEnabled from inside the dialog. */
private fun buildPrimaryButtonEnabledDialogExample(): WComponent {
    val result = WLabel("Result: not shown yet")

    val agreeCheckBox = WCheckBox("I agree to the terms of use")
    val dialogContent = WPanel(spacing = 8.0)
    dialogContent.add(WLabel("Agreeing enables the primary button."))
    dialogContent.add(agreeCheckBox)

    val dialog = WContentDialog("Terms of use", dialogContent)
    dialog.primaryButtonText = "Continue"
    dialog.closeButtonText = "Cancel"
    dialog.defaultButton = ContentDialogButton.PRIMARY
    dialog.isPrimaryButtonEnabled = false
    agreeCheckBox.addItemListener { checked ->
        dialog.isPrimaryButtonEnabled = checked == true
    }

    val showButton = WButton("Show dialog with consent checkbox")
    showButton.addActionListener {
        dialog.show(showButton) { dialogResult ->
            result.text = if (dialogResult == ContentDialogResult.PRIMARY) "Result: Continue" else "Result: Cancel"
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("Enabling/disabling a button (IsPrimaryButtonEnabled)", row)
}

// endregion

// region Flyout

/** The Flyout page: lines up demos for trying out WFlyout's various features. */
internal fun buildFlyoutPage(): WComponent {
    val page = buildPage("Flyout", "A lightweight popup for confirmations or supplementary information. Try out WFlyout's various features.")

    page.add(buildSimpleFlyoutExample())
    page.add(buildFlyoutPlacementExample())
    return page
}

/** A basic flyout: setting Button.Flyout and a confirmation UI via hide. */
private fun buildSimpleFlyoutExample(): WComponent {
    val result = WLabel("Not run yet")

    val flyoutContent = WPanel(spacing = 8.0)
    val flyout = WFlyout(flyoutContent)
    flyoutContent.add(WLabel("Permanently delete all items?"))
    flyoutContent.add(
        WButton("Yes, delete everything").also { button ->
            button.addActionListener {
                result.text = "Deleted"
                flyout.hide()
            }
        },
    )

    val flyoutButton = WButton("Empty the file")
    flyoutButton.flyout = flyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(flyoutButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    return buildExample("A confirmation flyout attached to a button (Button.Flyout / Hide)", row)
}

/** Display position: pick a Placement and open with ShowAt. */
private fun buildFlyoutPlacementExample(): WComponent {
    val flyout = WFlyout(WLabel("A flyout for trying out Placement."))

    val placementComboBox = WComboBox(FlyoutPlacement.entries.map { it.name })
    placementComboBox.width = 240.0
    placementComboBox.header = "Placement"
    placementComboBox.selectedIndex = FlyoutPlacement.TOP.ordinal
    placementComboBox.addListSelectionListener {
        flyout.placement = FlyoutPlacement.entries[placementComboBox.selectedIndex]
    }

    val showButton = WButton("Show flyout")
    showButton.addActionListener { flyout.showAt(showButton) }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(placementComboBox)
    // The ComboBox is taller because of its header, so nudge this down to align with the input box
    row.add(showButton.also { it.verticalAlignment = VerticalAlignment.BOTTOM })
    return buildExample("Display position (Placement / ShowAt)", row)
}

// endregion

// region Popup

/** The Popup page: lines up demos for trying out WPopup's various features. */
internal fun buildPopupPage(): WComponent {
    val page = buildPage("Popup", "A lightweight container for showing arbitrary content layered on the window. Try out WPopup's various features.")

    page.add(buildSimplePopupExample())
    return page
}

/** A basic popup: open/close, offset, light dismiss, and the Closed event. */
private fun buildSimplePopupExample(): WComponent {
    val result = WLabel("State: closed")

    val popup = WPopup()
    popup.horizontalOffset = 200.0
    popup.verticalOffset = 200.0
    popup.addCloseListener { result.text = "State: closed" }

    val popupContent = WPanel(spacing = 8.0)
    popupContent.add(WLabel("This is a popup.").also { it.fontSize = 18.0 })
    popupContent.add(
        WButton("Close").also { button ->
            button.addActionListener { popup.hide() }
        },
    )

    // Popup itself has no decoration, so add a border and background on the content side
    val popupCard = WBorder(popupContent)
    popupCard.background = CARD_BACKGROUND
    popupCard.borderColor = CARD_BORDER
    popupCard.borderThickness = 1.0
    popupCard.cornerRadius = 8.0
    popupCard.padding = 16.0
    popup.child = popupCard

    val lightDismissCheckBox = WCheckBox("Close on an outside click (IsLightDismissEnabled)")
    lightDismissCheckBox.addItemListener { checked ->
        popup.isLightDismissEnabled = checked == true
    }

    val showButton = WButton("Show popup")
    showButton.addActionListener {
        if (!popup.isOpen) {
            popup.show(showButton)
            result.text = "State: open"
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })

    val body = WPanel(spacing = 8.0)
    body.add(lightDismissCheckBox)
    body.add(row)
    return buildExample("A basic popup (IsOpen / Offset / IsLightDismissEnabled / Closed)", body)
}

// endregion

// region TeachingTip

/** The TeachingTip page: lines up demos for trying out WTeachingTip's various features. */
internal fun buildTeachingTipPage(): WComponent {
    val page = buildPage("TeachingTip", "A control that shows a callout pointing at an element, for things like feature announcements. Try out WTeachingTip's various features.")

    page.add(buildTargetedTeachingTipExample())
    page.add(buildUntargetedTeachingTipExample())
    return page
}

/** A callout with a target: Target / PreferredPlacement / ActionButtonClick / Closed. */
private fun buildTargetedTeachingTipExample(): WComponent {
    val result = WLabel("Not shown yet")

    val showButton = WButton("Save (introduces this feature)")

    val tip = WTeachingTip("Autosave is available", "Turn on the setting and your edits will be saved automatically.")
    tip.target = showButton
    tip.preferredPlacement = TeachingTipPlacement.BOTTOM
    tip.actionButtonText = "Turn on"
    tip.closeButtonText = "Later"
    tip.addActionListener {
        result.text = "Action: turned on autosave"
        tip.hide()
    }
    tip.addCloseListener { reason ->
        if (reason != TeachingTipCloseReason.PROGRAMMATIC) result.text = "Close reason: $reason"
    }

    showButton.addActionListener { tip.show() }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignment = VerticalAlignment.CENTER })
    row.add(tip) // placed in the XAML tree, matching the real Gallery (renders nothing while closed)
    return buildExample("A callout pointing at an element (Target / ActionButtonClick / Closed)", row)
}

/** A callout without a target: shown in a screen corner, with light dismiss. */
private fun buildUntargetedTeachingTipExample(): WComponent {
    val tip = WTeachingTip("New feature announcement", "A callout without a target is shown in a screen corner. Closes on an outside click.")
    tip.isLightDismissEnabled = true

    val showButton = WButton("Show announcement")
    showButton.addActionListener { tip.show() }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(tip) // placed in the XAML tree, matching the real Gallery (renders nothing while closed)
    return buildExample("A callout without a target (IsLightDismissEnabled)", row)
}

// endregion
