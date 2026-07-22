package com.appkitbox.winui4k.sample.gallery

import com.appkitbox.winui4k.BadgeGlyph
import com.appkitbox.winui4k.NotificationDuration
import com.appkitbox.winui4k.NotificationScenario
import com.appkitbox.winui4k.Orientation
import com.appkitbox.winui4k.TextWrapping
import com.appkitbox.winui4k.VerticalAlignment
import com.appkitbox.winui4k.WAppNotification
import com.appkitbox.winui4k.WAppNotificationManager
import com.appkitbox.winui4k.WBadgeNotification
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WCheckBox
import com.appkitbox.winui4k.WComboBox
import com.appkitbox.winui4k.WComponent
import com.appkitbox.winui4k.WJumpList
import com.appkitbox.winui4k.WJumpListItem
import com.appkitbox.winui4k.WLabel
import com.appkitbox.winui4k.WPanel
import com.appkitbox.winui4k.WTextField

/**
 * Shell category: demo pages for AppNotification / BadgeNotification / JumpList.
 */

// region AppNotification

/** The AppNotification page: lines up demos for trying out WAppNotification / WAppNotificationManager's various features. */
internal fun buildAppNotificationPage(): WComponent {
    val page = buildPage(
        "AppNotification",
        "A notification shown in the Action Center and as a toast popup. " +
            "Try out WAppNotification / WAppNotificationManager's various features.",
    )

    page.add(buildNotificationStatusExample())
    page.add(buildSimpleNotificationExample())
    page.add(buildInteractiveNotificationExample())
    return page
}

/** Registering as a notification sender only needs to happen once per process, so do it once before the first send. */
private var notificationRegistered = false

private fun ensureNotificationRegistered() {
    if (!notificationRegistered) {
        WAppNotificationManager.register()
        notificationRegistered = true
    }
}

/** Whether notifications are usable in this environment: isSupported / setting. */
private fun buildNotificationStatusExample(): WComponent {
    val supported = WLabel("IsSupported: ${WAppNotificationManager.isSupported}")
    val setting = WLabel("Setting: not fetched yet")

    val refreshButton = WButton("Fetch Setting")
    refreshButton.addActionListener {
        setting.text = runCatching { "Setting: ${WAppNotificationManager.setting}" }
            .getOrElse { "Failed to fetch Setting: ${it.message}" }
    }

    val body = WPanel(spacing = 8.0)
    body.add(supported)
    body.add(setting)
    body.add(refreshButton)
    return buildExample("Whether notifications are usable in this environment (IsSupported / Setting)", body)
}

/** A basic notification: a 2-line body + attribution text + display duration. */
private fun buildSimpleNotificationExample(): WComponent {
    val titleField = WTextField("Line 1 (title)").also { it.width = 320.0 }
    titleField.text = "A notification from winui4k"
    val bodyField = WTextField("Line 2 (body)").also { it.width = 320.0 }
    bodyField.text = "A toast notification was sent from Kotlin."
    val longDuration = WCheckBox("Use a longer display duration (Duration.LONG)")
    val result = WLabel("")

    val sendButton = WButton("Send notification")
    sendButton.addActionListener {
        result.text = runCatching {
            ensureNotificationRegistered()
            val notification = WAppNotification(titleField.text)
                .addText(bodyField.text)
                .setAttributionText("WinUI4K Gallery")
                .setTag("gallery-simple")
                .setGroup("gallery")
            if (longDuration.isChecked == true) notification.setDuration(NotificationDuration.LONG)
            WAppNotificationManager.show(notification)
            "Sent"
        }.getOrElse { "Failed to send: ${it.message}" }
    }

    val body = WPanel(spacing = 8.0)
    body.add(titleField)
    body.add(bodyField)
    body.add(longDuration)
    body.add(sendButton)
    body.add(result)
    return buildExample("A basic notification (AddText / SetAttributionText / SetTag / SetGroup / SetDuration)", body)
}

/** A notification with buttons and receiving clicks: AddArgument / AddButton / NotificationInvoked. */
private fun buildInteractiveNotificationExample(): WComponent {
    val scenarioComboBox = WComboBox()
    scenarioComboBox.header = "Scenario"
    for (scenario in NotificationScenario.entries) scenarioComboBox.addItem(scenario.name)
    scenarioComboBox.selectedIndex = 0

    val received = WLabel("Waiting for a click (clicking the notification body or a button delivers the argument here)")
    WAppNotificationManager.addNotificationInvokedListener { argument ->
        received.text = "Received argument: $argument"
    }

    val sendButton = WButton("Send notification with buttons")
    sendButton.addActionListener {
        received.text = runCatching {
            ensureNotificationRegistered()
            val scenario = NotificationScenario.entries[scenarioComboBox.selectedIndex]
            WAppNotificationManager.show(
                WAppNotification("Want to reply?")
                    .addText("Button clicks can be received on the app side.")
                    .addArgument("action", "open")
                    .addButton("Approve", "action" to "approve")
                    .addButton("Reject", "action" to "reject")
                    .setScenario(scenario),
            )
            "Sent. Click the notification"
        }.getOrElse { "Failed to send: ${it.message}" }
    }

    val body = WPanel(spacing = 8.0)
    body.add(scenarioComboBox)
    body.add(sendButton)
    body.add(received)
    return buildExample("A notification with buttons and receiving clicks (AddButton / AddArgument / NotificationInvoked)", body)
}

// endregion

// region BadgeNotification

/** The BadgeNotification page: lines up demos for trying out WBadgeNotification's various features. */
internal fun buildBadgeNotificationPage(): WComponent {
    val page = buildPage(
        "BadgeNotification",
        "A badge overlaid on the app's taskbar icon. Try out WBadgeNotification's various features. " +
            "Badges can only be shown by an app with a package identity; setting one under an unpackaged run errors out.",
    )

    page.add(buildBadgeCountExample())
    page.add(buildBadgeGlyphExample())
    return page
}

/** A numeric badge: SetBadgeAsCount / ClearBadge. */
private fun buildBadgeCountExample(): WComponent {
    val result = WLabel("Check the taskbar icon")

    fun countButton(count: Int) = WButton("$count").also { button ->
        button.addActionListener {
            result.text = runCatching {
                WBadgeNotification.setCount(count)
                "Set the badge to $count" + if (count > 99) " (100 and above shows as 99+)" else ""
            }.getOrElse { "Failed to set: ${it.message}" }
        }
    }

    val clearButton = WButton("Clear")
    clearButton.addActionListener {
        result.text = runCatching {
            WBadgeNotification.clear()
            "Cleared the badge"
        }.getOrElse { "Failed to clear: ${it.message}" }
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(countButton(1))
    row.add(countButton(42))
    row.add(countButton(150))
    row.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample("A numeric badge (SetBadgeAsCount / ClearBadge)", body)
}

/** A glyph badge: SetBadgeAsGlyph. */
private fun buildBadgeGlyphExample(): WComponent {
    val glyphComboBox = WComboBox()
    glyphComboBox.header = "BadgeGlyph"
    for (glyph in BadgeGlyph.entries) glyphComboBox.addItem(glyph.name)
    glyphComboBox.selectedIndex = BadgeGlyph.NEW_MESSAGE.ordinal

    val result = WLabel("")
    val applyButton = WButton("Set glyph")
    applyButton.addActionListener {
        val glyph = BadgeGlyph.entries[glyphComboBox.selectedIndex]
        result.text = runCatching {
            WBadgeNotification.setGlyph(glyph)
            "Set the badge to $glyph"
        }.getOrElse { "Failed to set: ${it.message}" }
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(glyphComboBox)
    // The ComboBox is taller because of its header, so nudge this down to align with the input box
    row.add(applyButton.also { it.verticalAlignment = VerticalAlignment.BOTTOM })

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample("A status-glyph badge (SetBadgeAsGlyph)", body)
}

// endregion

// region JumpList

/** The JumpList page: lines up demos for trying out WJumpList / WJumpListItem's various features. */
internal fun buildJumpListPage(): WComponent {
    val page = buildPage(
        "JumpList",
        "Adds custom tasks or items to the menu shown when right-clicking the app's taskbar icon. " +
            "Try out WJumpList / WJumpListItem's various features.",
    )

    // IsSupported can return true even for a run without a package identity, so actually
    // load it to check
    val loadFailure = if (WJumpList.isSupported) {
        runCatching { WJumpList.load() }.exceptionOrNull()
    } else {
        IllegalStateException("JumpList.IsSupported is false")
    }
    if (loadFailure != null) {
        page.add(
            buildExample(
                "Not usable in this environment",
                WLabel(
                    "Jump lists only work for an app with a package identity, so they don't work " +
                        "under an unpackaged run (launching java.exe directly). " +
                        "(${loadFailure.message})",
                ).also { it.textWrapping = TextWrapping.WRAP },
            ),
        )
        return page
    }

    page.add(buildJumpListEditExample())
    return page
}

/** Adding an item, removing all, saving, and the current item list. */
private fun buildJumpListEditExample(): WComponent {
    val nameField = WTextField("DisplayName").also { it.width = 320.0 }
    nameField.text = "New document"
    val argumentsField = WTextField("Arguments (launch arguments)").also { it.width = 320.0 }
    argumentsField.text = "/new"

    val itemsLabel = WLabel("")
    val result = WLabel("")

    fun refreshItems(jumpList: WJumpList) {
        val names = jumpList.items.map { if (it.isSeparator) "――――" else it.displayName }
        itemsLabel.text =
            if (names.isEmpty()) "No custom items"
            else "Current items: " + names.joinToString(" / ")
    }

    fun edit(block: (WJumpList) -> String) {
        result.text = runCatching {
            val jumpList = WJumpList.load()
            val message = block(jumpList)
            jumpList.save()
            refreshItems(jumpList)
            message
        }.getOrElse { "Operation failed: ${it.message}" }
    }

    val addButton = WButton("Add item and save")
    addButton.addActionListener {
        edit { jumpList ->
            val item = WJumpListItem.of(argumentsField.text, nameField.text)
            item.description = "An item added by WinUI4K Gallery"
            item.groupName = "Gallery"
            jumpList.add(item)
            "Added. Right-click the taskbar icon to check"
        }
    }

    val separatorButton = WButton("Add separator and save")
    separatorButton.addActionListener {
        edit { jumpList ->
            jumpList.add(WJumpListItem.separator())
            "Added a separator"
        }
    }

    val clearButton = WButton("Remove all items and save")
    clearButton.addActionListener {
        edit { jumpList ->
            jumpList.removeAll()
            "Removed all items"
        }
    }

    runCatching { refreshItems(WJumpList.load()) }

    val buttons = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    buttons.add(addButton)
    buttons.add(separatorButton)
    buttons.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(nameField)
    body.add(argumentsField)
    body.add(buttons)
    body.add(itemsLabel)
    body.add(result)
    return buildExample("Editing items (Items / SaveAsync / CreateWithArguments / CreateSeparator)", body)
}

// endregion
