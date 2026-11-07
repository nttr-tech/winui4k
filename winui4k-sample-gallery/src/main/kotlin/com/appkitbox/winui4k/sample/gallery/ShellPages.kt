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

/*
 * Shell category: demo pages for AppNotification / BadgeNotification / JumpList.
 */

// region AppNotification

/** The AppNotification page: lines up demos for trying out WAppNotification / WAppNotificationManager's various features. */
internal fun buildAppNotificationPage(): WComponent {
    val page = buildPage(
        "AppNotification",
        Messages.get("appnotification.description"),
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
    val setting = WLabel(Messages.get("appnotification.setting.notloaded"))

    val refreshButton = WButton(Messages.get("appnotification.button.getsetting"))
    refreshButton.addActionListener {
        setting.text = runCatching { "Setting: ${WAppNotificationManager.setting}" }
            .getOrElse { Messages.get("appnotification.setting.failed", it.message) }
    }

    val body = WPanel(spacing = 8.0)
    body.add(supported)
    body.add(setting)
    body.add(refreshButton)
    return buildExample(Messages.get("appnotification.example.status.title"), body)
}

/** A basic notification: a 2-line body + attribution text + display duration. */
private fun buildSimpleNotificationExample(): WComponent {
    val titleField = WTextField().apply { placeholderText = Messages.get("appnotification.field.title.placeholder") }.also { it.width = 320.0 }
    titleField.text = Messages.get("appnotification.field.title.default")
    val bodyField = WTextField().apply { placeholderText = Messages.get("appnotification.field.body.placeholder") }.also { it.width = 320.0 }
    bodyField.text = Messages.get("appnotification.field.body.default")
    val longDuration = WCheckBox(Messages.get("appnotification.check.longduration"))
    val result = WLabel("")

    val sendButton = WButton(Messages.get("appnotification.button.send"))
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
            Messages.get("appnotification.result.sent")
        }.getOrElse { Messages.get("appnotification.result.failed", it.message) }
    }

    val body = WPanel(spacing = 8.0)
    body.add(titleField)
    body.add(bodyField)
    body.add(longDuration)
    body.add(sendButton)
    body.add(result)
    return buildExample(Messages.get("appnotification.example.simple.title"), body)
}

/** A notification with buttons and receiving clicks: AddArgument / AddButton / NotificationInvoked. */
private fun buildInteractiveNotificationExample(): WComponent {
    val scenarioComboBox = WComboBox()
    scenarioComboBox.header = "Scenario"
    for (scenario in NotificationScenario.entries) scenarioComboBox.addItem(scenario.name)
    scenarioComboBox.selectedIndex = 0

    val received = WLabel(Messages.get("appnotification.received.waiting"))
    WAppNotificationManager.addNotificationInvokedListener { argument ->
        received.text = Messages.get("appnotification.received.argument", argument)
    }

    val sendButton = WButton(Messages.get("appnotification.button.sendwithbuttons"))
    sendButton.addActionListener {
        received.text = runCatching {
            ensureNotificationRegistered()
            val scenario = NotificationScenario.entries[scenarioComboBox.selectedIndex]
            WAppNotificationManager.show(
                WAppNotification(Messages.get("appnotification.interactive.title"))
                    .addText(Messages.get("appnotification.interactive.text"))
                    .addArgument("action", "open")
                    .addButton(Messages.get("appnotification.interactive.approve"), "action" to "approve")
                    .addButton(Messages.get("appnotification.interactive.reject"), "action" to "reject")
                    .setScenario(scenario),
            )
            Messages.get("appnotification.result.sentclick")
        }.getOrElse { Messages.get("appnotification.result.failed", it.message) }
    }

    val body = WPanel(spacing = 8.0)
    body.add(scenarioComboBox)
    body.add(sendButton)
    body.add(received)
    return buildExample(Messages.get("appnotification.example.interactive.title"), body)
}

// endregion

// region BadgeNotification

/** The BadgeNotification page: lines up demos for trying out WBadgeNotification's various features. */
internal fun buildBadgeNotificationPage(): WComponent {
    val page = buildPage(
        "BadgeNotification",
        Messages.get("badgenotification.description"),
    )

    page.add(buildBadgeCountExample())
    page.add(buildBadgeGlyphExample())
    return page
}

/** A numeric badge: SetBadgeAsCount / ClearBadge. */
private fun buildBadgeCountExample(): WComponent {
    val result = WLabel(Messages.get("badgenotification.result.hint"))

    fun countButton(count: Int) = WButton("$count").also { button ->
        button.addActionListener {
            result.text = runCatching {
                WBadgeNotification.setCount(count)
                if (count > 99) {
                    Messages.get("badgenotification.result.setcapped", count.toString())
                } else {
                    Messages.get("badgenotification.result.set", count.toString())
                }
            }.getOrElse { Messages.get("badgenotification.result.setfailed", it.message) }
        }
    }

    val clearButton = WButton(Messages.get("badgenotification.button.clear"))
    clearButton.addActionListener {
        result.text = runCatching {
            WBadgeNotification.clear()
            Messages.get("badgenotification.result.cleared")
        }.getOrElse { Messages.get("badgenotification.result.clearfailed", it.message) }
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(countButton(1))
    row.add(countButton(42))
    row.add(countButton(150))
    row.add(clearButton)

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample(Messages.get("badgenotification.example.count.title"), body)
}

/** A glyph badge: SetBadgeAsGlyph. */
private fun buildBadgeGlyphExample(): WComponent {
    val glyphComboBox = WComboBox()
    glyphComboBox.header = "BadgeGlyph"
    for (glyph in BadgeGlyph.entries) glyphComboBox.addItem(glyph.name)
    glyphComboBox.selectedIndex = BadgeGlyph.NEW_MESSAGE.ordinal

    val result = WLabel("")
    val applyButton = WButton(Messages.get("badgenotification.button.setglyph"))
    applyButton.addActionListener {
        val glyph = BadgeGlyph.entries[glyphComboBox.selectedIndex]
        result.text = runCatching {
            WBadgeNotification.setGlyph(glyph)
            Messages.get("badgenotification.result.set", glyph.toString())
        }.getOrElse { Messages.get("badgenotification.result.setfailed", it.message) }
    }

    val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
    row.add(glyphComboBox)
    // The ComboBox is taller because of its header, so nudge this down to align with the input box
    row.add(applyButton.also { it.verticalAlignmentInParent = VerticalAlignment.BOTTOM })

    val body = WPanel(spacing = 8.0)
    body.add(row)
    body.add(result)
    return buildExample(Messages.get("badgenotification.example.glyph.title"), body)
}

// endregion

// region JumpList

/** The JumpList page: lines up demos for trying out WJumpList / WJumpListItem's various features. */
internal fun buildJumpListPage(): WComponent {
    val page = buildPage(
        "JumpList",
        Messages.get("jumplist.description"),
    )

    // IsSupported can return true even for a run without a package identity, so actually
    // load it to check
    val loadFailure = if (WJumpList.isSupported) {
        runCatching { WJumpList.load() }.exceptionOrNull()
    } else {
        IllegalStateException(Messages.get("jumplist.unavailable.notsupported"))
    }
    if (loadFailure != null) {
        page.add(
            buildExample(
                Messages.get("jumplist.unavailable.title"),
                WLabel(
                    Messages.get("jumplist.unavailable.message", loadFailure.message),
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
    val nameField = WTextField().apply { placeholderText = Messages.get("jumplist.field.name.placeholder") }.also { it.width = 320.0 }
    nameField.text = Messages.get("jumplist.field.name.default")
    val argumentsField = WTextField().apply { placeholderText = Messages.get("jumplist.field.arguments.placeholder") }.also { it.width = 320.0 }
    argumentsField.text = "/new"

    val itemsLabel = WLabel("")
    val result = WLabel("")

    fun refreshItems(jumpList: WJumpList) {
        val names = jumpList.items.map { if (it.isSeparator) "――――" else it.displayName }
        itemsLabel.text =
            if (names.isEmpty()) {
                Messages.get("jumplist.items.none")
            } else {
                Messages.get("jumplist.items.current", names.joinToString(" / "))
            }
    }

    fun edit(block: (WJumpList) -> String) {
        result.text = runCatching {
            val jumpList = WJumpList.load()
            val message = block(jumpList)
            jumpList.save()
            refreshItems(jumpList)
            message
        }.getOrElse { Messages.get("jumplist.result.failed", it.message) }
    }

    val addButton = WButton(Messages.get("jumplist.button.add"))
    addButton.addActionListener {
        edit { jumpList ->
            val item = WJumpListItem.of(argumentsField.text, nameField.text)
            item.description = Messages.get("jumplist.item.description")
            item.groupName = "Gallery"
            jumpList.add(item)
            Messages.get("jumplist.result.added")
        }
    }

    val separatorButton = WButton(Messages.get("jumplist.button.addseparator"))
    separatorButton.addActionListener {
        edit { jumpList ->
            jumpList.add(WJumpListItem.separator())
            Messages.get("jumplist.result.separatoradded")
        }
    }

    val clearButton = WButton(Messages.get("jumplist.button.clear"))
    clearButton.addActionListener {
        edit { jumpList ->
            jumpList.removeAll()
            Messages.get("jumplist.result.cleared")
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
    return buildExample(Messages.get("jumplist.example.edit.title"), body)
}

// endregion
