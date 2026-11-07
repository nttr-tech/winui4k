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
    val page = buildPage("ContentDialog", Messages.get("contentdialog.description"))

    page.add(buildSimpleContentDialogExample())
    page.add(buildPrimaryButtonEnabledDialogExample())
    return page
}

/** A basic dialog: 3 buttons plus a default button, and receiving the closed result (ContentDialogResult). */
private fun buildSimpleContentDialogExample(): WComponent {
    val result = WLabel(Messages.get("contentdialog.result.notshown"))

    val dialog = WContentDialog(Messages.get("contentdialog.simple.title"), WLabel(Messages.get("contentdialog.simple.content")))
    dialog.primaryButtonText = Messages.get("contentdialog.simple.save")
    dialog.secondaryButtonText = Messages.get("contentdialog.simple.dontsave")
    dialog.closeButtonText = Messages.get("contentdialog.cancel")
    dialog.defaultButton = ContentDialogButton.PRIMARY

    val showButton = WButton(Messages.get("contentdialog.simple.show"))
    showButton.addActionListener {
        dialog.show(showButton) { dialogResult ->
            result.text = when (dialogResult) {
                ContentDialogResult.PRIMARY -> Messages.get("contentdialog.result.save")
                ContentDialogResult.SECONDARY -> Messages.get("contentdialog.result.dontsave")
                ContentDialogResult.NONE -> Messages.get("contentdialog.result.cancel")
            }
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("contentdialog.example.simple.title"), row)
}

/** A dialog with a consent checkbox: toggling IsPrimaryButtonEnabled from inside the dialog. */
private fun buildPrimaryButtonEnabledDialogExample(): WComponent {
    val result = WLabel(Messages.get("contentdialog.result.notshown"))

    val agreeCheckBox = WCheckBox(Messages.get("contentdialog.terms.agree"))
    val dialogContent = WPanel(spacing = 8.0)
    dialogContent.add(WLabel(Messages.get("contentdialog.terms.content")))
    dialogContent.add(agreeCheckBox)

    val dialog = WContentDialog(Messages.get("contentdialog.terms.title"), dialogContent)
    dialog.primaryButtonText = Messages.get("contentdialog.terms.continue")
    dialog.closeButtonText = Messages.get("contentdialog.cancel")
    dialog.defaultButton = ContentDialogButton.PRIMARY
    dialog.isPrimaryButtonEnabled = false
    agreeCheckBox.addItemListener { checked ->
        dialog.isPrimaryButtonEnabled = checked == true
    }

    val showButton = WButton(Messages.get("contentdialog.terms.show"))
    showButton.addActionListener {
        dialog.show(showButton) { dialogResult ->
            result.text = if (dialogResult == ContentDialogResult.PRIMARY) Messages.get("contentdialog.result.continue") else Messages.get("contentdialog.result.cancel")
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("contentdialog.example.terms.title"), row)
}

// endregion

// region Flyout

/** The Flyout page: lines up demos for trying out WFlyout's various features. */
internal fun buildFlyoutPage(): WComponent {
    val page = buildPage("Flyout", Messages.get("flyout.description"))

    page.add(buildSimpleFlyoutExample())
    page.add(buildFlyoutPlacementExample())
    return page
}

/** A basic flyout: setting Button.Flyout and a confirmation UI via hide. */
private fun buildSimpleFlyoutExample(): WComponent {
    val result = WLabel(Messages.get("flyout.simple.status.none"))

    val flyoutContent = WPanel(spacing = 8.0)
    val flyout = WFlyout(flyoutContent)
    flyoutContent.add(WLabel(Messages.get("flyout.simple.question")))
    flyoutContent.add(
        WButton(Messages.get("flyout.simple.confirm")).also { button ->
            button.addActionListener {
                result.text = Messages.get("flyout.simple.status.deleted")
                flyout.hide()
            }
        },
    )

    val flyoutButton = WButton(Messages.get("flyout.simple.button"))
    flyoutButton.flyout = flyout

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(flyoutButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    return buildExample(Messages.get("flyout.example.simple.title"), row)
}

/** Display position: pick a Placement and open with ShowAt. */
private fun buildFlyoutPlacementExample(): WComponent {
    val flyout = WFlyout(WLabel(Messages.get("flyout.placement.content")))

    val placementComboBox = WComboBox(FlyoutPlacement.entries.map { it.name })
    placementComboBox.width = 240.0
    placementComboBox.header = "Placement"
    placementComboBox.selectedIndex = FlyoutPlacement.TOP.ordinal
    placementComboBox.addListSelectionListener {
        flyout.placement = FlyoutPlacement.entries[placementComboBox.selectedIndex]
    }

    val showButton = WButton(Messages.get("flyout.placement.show"))
    showButton.addActionListener { flyout.showAt(showButton) }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(placementComboBox)
    // The ComboBox is taller because of its header, so nudge this down to align with the input box
    row.add(showButton.also { it.verticalAlignmentInParent = VerticalAlignment.BOTTOM })
    return buildExample(Messages.get("flyout.example.placement.title"), row)
}

// endregion

// region Popup

/** The Popup page: lines up demos for trying out WPopup's various features. */
internal fun buildPopupPage(): WComponent {
    val page = buildPage("Popup", Messages.get("popup.description"))

    page.add(buildSimplePopupExample())
    return page
}

/** A basic popup: open/close, offset, light dismiss, and the Closed event. */
private fun buildSimplePopupExample(): WComponent {
    val result = WLabel(Messages.get("popup.status.closed"))

    val popup = WPopup()
    popup.horizontalOffset = 200.0
    popup.verticalOffset = 200.0
    popup.addCloseListener { result.text = Messages.get("popup.status.closed") }

    val popupContent = WPanel(spacing = 8.0)
    popupContent.add(WLabel(Messages.get("popup.content.text")).also { it.fontSize = 18.0 })
    popupContent.add(
        WButton(Messages.get("popup.content.close")).also { button ->
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

    val lightDismissCheckBox = WCheckBox(Messages.get("popup.lightdismiss"))
    lightDismissCheckBox.addItemListener { checked ->
        popup.isLightDismissEnabled = checked == true
    }

    val showButton = WButton(Messages.get("popup.show"))
    showButton.addActionListener {
        if (!popup.isOpen) {
            popup.show(showButton)
            result.text = Messages.get("popup.status.open")
        }
    }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })

    val body = WPanel(spacing = 8.0)
    body.add(lightDismissCheckBox)
    body.add(row)
    return buildExample(Messages.get("popup.example.simple.title"), body)
}

// endregion

// region TeachingTip

/** The TeachingTip page: lines up demos for trying out WTeachingTip's various features. */
internal fun buildTeachingTipPage(): WComponent {
    val page = buildPage("TeachingTip", Messages.get("teachingtip.description"))

    page.add(buildTargetedTeachingTipExample())
    page.add(buildUntargetedTeachingTipExample())
    return page
}

/** A callout with a target: Target / PreferredPlacement / ActionButtonClick / Closed. */
private fun buildTargetedTeachingTipExample(): WComponent {
    val result = WLabel(Messages.get("teachingtip.targeted.status.none"))

    val showButton = WButton(Messages.get("teachingtip.targeted.button"))

    val tip = WTeachingTip(Messages.get("teachingtip.targeted.title"), Messages.get("teachingtip.targeted.subtitle"))
    tip.target = showButton
    tip.preferredPlacement = TeachingTipPlacement.BOTTOM
    tip.actionButtonText = Messages.get("teachingtip.targeted.action")
    tip.closeButtonText = Messages.get("teachingtip.targeted.close")
    tip.addActionListener {
        result.text = Messages.get("teachingtip.targeted.status.action")
        tip.hide()
    }
    tip.addCloseListener { reason ->
        if (reason != TeachingTipCloseReason.PROGRAMMATIC) result.text = Messages.get("teachingtip.result.closereason", reason.toString())
    }

    showButton.addActionListener { tip.show() }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(result.also { it.verticalAlignmentInParent = VerticalAlignment.CENTER })
    row.add(tip) // placed in the XAML tree, matching the real Gallery (renders nothing while closed)
    return buildExample(Messages.get("teachingtip.example.targeted.title"), row)
}

/** A callout without a target: shown in a screen corner, with light dismiss. */
private fun buildUntargetedTeachingTipExample(): WComponent {
    val tip = WTeachingTip(Messages.get("teachingtip.untargeted.title"), Messages.get("teachingtip.untargeted.subtitle"))
    tip.isLightDismissEnabled = true

    val showButton = WButton(Messages.get("teachingtip.untargeted.show"))
    showButton.addActionListener { tip.show() }

    val row = WPanel(spacing = 16.0, orientation = Orientation.HORIZONTAL)
    row.add(showButton)
    row.add(tip) // placed in the XAML tree, matching the real Gallery (renders nothing while closed)
    return buildExample(Messages.get("teachingtip.example.untargeted.title"), row)
}

// endregion
