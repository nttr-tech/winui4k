package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.BadgeGlyph;
import com.appkitbox.winui4k.NotificationDuration;
import com.appkitbox.winui4k.NotificationScenario;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WAppNotification;
import com.appkitbox.winui4k.WAppNotificationManager;
import com.appkitbox.winui4k.WBadgeNotification;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WJumpList;
import com.appkitbox.winui4k.WJumpListItem;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WTextField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/*
 * Shell category: demo pages for AppNotification / BadgeNotification / JumpList.
 */
final class ShellPages {
    private ShellPages() {
    }

    // region AppNotification

    /** The AppNotification page: lines up demos for trying out WAppNotification / WAppNotificationManager's various features. */
    static WComponent buildAppNotificationPage() {
        WPanel page = GalleryScaffold.buildPage(
            "AppNotification",
            Messages.get("appnotification.description"));

        page.add(buildNotificationStatusExample());
        page.add(buildSimpleNotificationExample());
        page.add(buildInteractiveNotificationExample());
        return page;
    }

    /** Registering as a notification sender only needs to happen once per process, so do it once before the first send. */
    private static boolean notificationRegistered = false;

    private static void ensureNotificationRegistered() {
        if (!notificationRegistered) {
            WAppNotificationManager.register();
            notificationRegistered = true;
        }
    }

    /** Whether notifications are usable in this environment: isSupported / setting. */
    private static WComponent buildNotificationStatusExample() {
        WLabel supported = new WLabel("IsSupported: " + WAppNotificationManager.isSupported());
        WLabel setting = new WLabel(Messages.get("appnotification.setting.notloaded"));

        WButton refreshButton = new WButton(Messages.get("appnotification.button.getsetting"));
        refreshButton.addActionListener(() -> {
            String text;
            try {
                text = "Setting: " + WAppNotificationManager.getSetting();
            } catch (Exception e) {
                text = Messages.get("appnotification.setting.failed", e.getMessage());
            }
            setting.setText(text);
        });

        WPanel body = new WPanel(8.0);
        body.add(supported);
        body.add(setting);
        body.add(refreshButton);
        return GalleryScaffold.buildExample(Messages.get("appnotification.example.status.title"), body);
    }

    /** A basic notification: a 2-line body + attribution text + display duration. */
    private static WComponent buildSimpleNotificationExample() {
        WTextField titleField = new WTextField();
        titleField.setPlaceholderText(Messages.get("appnotification.field.title.placeholder"));
        titleField.setWidth(320.0);
        titleField.setText(Messages.get("appnotification.field.title.default"));
        WTextField bodyField = new WTextField();
        bodyField.setPlaceholderText(Messages.get("appnotification.field.body.placeholder"));
        bodyField.setWidth(320.0);
        bodyField.setText(Messages.get("appnotification.field.body.default"));
        WCheckBox longDuration = new WCheckBox(Messages.get("appnotification.check.longduration"));
        WLabel result = new WLabel("");

        WButton sendButton = new WButton(Messages.get("appnotification.button.send"));
        sendButton.addActionListener(() -> {
            String message;
            try {
                ensureNotificationRegistered();
                WAppNotification notification = new WAppNotification(titleField.getText())
                    .addText(bodyField.getText())
                    .setAttributionText("WinUI4K Gallery")
                    .setTag("gallery-simple")
                    .setGroup("gallery");
                if (longDuration.isSelected()) {
                    notification.setDuration(NotificationDuration.LONG);
                }
                WAppNotificationManager.show(notification);
                message = Messages.get("appnotification.result.sent");
            } catch (Exception e) {
                message = Messages.get("appnotification.result.failed", e.getMessage());
            }
            result.setText(message);
        });

        WPanel body = new WPanel(8.0);
        body.add(titleField);
        body.add(bodyField);
        body.add(longDuration);
        body.add(sendButton);
        body.add(result);
        return GalleryScaffold.buildExample(
            Messages.get("appnotification.example.simple.title"), body);
    }

    /** A notification with buttons and receiving clicks: AddArgument / AddButton / NotificationInvoked. */
    private static WComponent buildInteractiveNotificationExample() {
        WComboBox scenarioComboBox = new WComboBox(Collections.<String>emptyList());
        scenarioComboBox.setHeader("Scenario");
        for (NotificationScenario scenario : NotificationScenario.values()) {
            scenarioComboBox.addItem(scenario.name());
        }
        scenarioComboBox.setSelectedIndex(0);

        WLabel received = new WLabel(Messages.get("appnotification.received.waiting"));
        WAppNotificationManager.addNotificationInvokedListener(argument -> {
            received.setText(Messages.get("appnotification.received.argument", argument));
        });

        WButton sendButton = new WButton(Messages.get("appnotification.button.sendwithbuttons"));
        sendButton.addActionListener(() -> {
            String message;
            try {
                ensureNotificationRegistered();
                NotificationScenario scenario = NotificationScenario.values()[scenarioComboBox.getSelectedIndex()];
                WAppNotificationManager.show(
                    new WAppNotification(Messages.get("appnotification.interactive.title"))
                        .addText(Messages.get("appnotification.interactive.text"))
                        .addArgument("action", "open")
                        .addButton(Messages.get("appnotification.interactive.approve"), Collections.singletonMap("action", "approve"))
                        .addButton(Messages.get("appnotification.interactive.reject"), Collections.singletonMap("action", "reject"))
                        .setScenario(scenario));
                message = Messages.get("appnotification.result.sentclick");
            } catch (Exception e) {
                message = Messages.get("appnotification.result.failed", e.getMessage());
            }
            received.setText(message);
        });

        WPanel body = new WPanel(8.0);
        body.add(scenarioComboBox);
        body.add(sendButton);
        body.add(received);
        return GalleryScaffold.buildExample(
            Messages.get("appnotification.example.interactive.title"), body);
    }

    // endregion

    // region BadgeNotification

    /** The BadgeNotification page: lines up demos for trying out WBadgeNotification's various features. */
    static WComponent buildBadgeNotificationPage() {
        WPanel page = GalleryScaffold.buildPage(
            "BadgeNotification",
            Messages.get("badgenotification.description"));

        page.add(buildBadgeCountExample());
        page.add(buildBadgeGlyphExample());
        return page;
    }

    /** A numeric badge: SetBadgeAsCount / ClearBadge. */
    private static WComponent buildBadgeCountExample() {
        WLabel result = new WLabel(Messages.get("badgenotification.result.hint"));

        WButton clearButton = new WButton(Messages.get("badgenotification.button.clear"));
        clearButton.addActionListener(() -> {
            String message;
            try {
                WBadgeNotification.clear();
                message = Messages.get("badgenotification.result.cleared");
            } catch (Exception e) {
                message = Messages.get("badgenotification.result.clearfailed", e.getMessage());
            }
            result.setText(message);
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(countButton(1, result));
        row.add(countButton(42, result));
        row.add(countButton(150, result));
        row.add(clearButton);

        WPanel body = new WPanel(8.0);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("badgenotification.example.count.title"), body);
    }

    private static WButton countButton(int count, WLabel result) {
        WButton button = new WButton(String.valueOf(count));
        button.addActionListener(() -> {
            String message;
            try {
                WBadgeNotification.setCount(count);
                message = count > 99
                    ? Messages.get("badgenotification.result.setcapped", String.valueOf(count))
                    : Messages.get("badgenotification.result.set", String.valueOf(count));
            } catch (Exception e) {
                message = Messages.get("badgenotification.result.setfailed", e.getMessage());
            }
            result.setText(message);
        });
        return button;
    }

    /** A glyph badge: SetBadgeAsGlyph. */
    private static WComponent buildBadgeGlyphExample() {
        WComboBox glyphComboBox = new WComboBox(Collections.<String>emptyList());
        glyphComboBox.setHeader("BadgeGlyph");
        for (BadgeGlyph glyph : BadgeGlyph.values()) {
            glyphComboBox.addItem(glyph.name());
        }
        glyphComboBox.setSelectedIndex(BadgeGlyph.NEW_MESSAGE.ordinal());

        WLabel result = new WLabel("");
        WButton applyButton = new WButton(Messages.get("badgenotification.button.setglyph"));
        applyButton.addActionListener(() -> {
            BadgeGlyph glyph = BadgeGlyph.values()[glyphComboBox.getSelectedIndex()];
            String message;
            try {
                WBadgeNotification.setGlyph(glyph);
                message = Messages.get("badgenotification.result.set", glyph.toString());
            } catch (Exception e) {
                message = Messages.get("badgenotification.result.setfailed", e.getMessage());
            }
            result.setText(message);
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(glyphComboBox);
        // The ComboBox is taller because of its header, so nudge this down to align with the input box
        applyButton.setVerticalAlignmentInParent(VerticalAlignment.BOTTOM);
        row.add(applyButton);

        WPanel body = new WPanel(8.0);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample(Messages.get("badgenotification.example.glyph.title"), body);
    }

    // endregion

    // region JumpList

    /** The JumpList page: lines up demos for trying out WJumpList / WJumpListItem's various features. */
    static WComponent buildJumpListPage() {
        WPanel page = GalleryScaffold.buildPage(
            "JumpList",
            Messages.get("jumplist.description"));

        // IsSupported can return true even for a run without a package identity, so verify it by actually loading
        Throwable loadFailure;
        if (WJumpList.isSupported()) {
            Throwable failure = null;
            try {
                WJumpList.load();
            } catch (Exception e) {
                failure = e;
            }
            loadFailure = failure;
        } else {
            loadFailure = new IllegalStateException(Messages.get("jumplist.unavailable.notsupported"));
        }
        if (loadFailure != null) {
            WLabel message = new WLabel(Messages.get("jumplist.unavailable.message", loadFailure.getMessage()));
            message.setTextWrapping(TextWrapping.WRAP);
            page.add(GalleryScaffold.buildExample(Messages.get("jumplist.unavailable.title"), message));
            return page;
        }

        page.add(buildJumpListEditExample());
        return page;
    }

    /** Adding an item, removing all, saving, and the current item list. */
    private static WComponent buildJumpListEditExample() {
        WTextField nameField = new WTextField();
        nameField.setPlaceholderText(Messages.get("jumplist.field.name.placeholder"));
        nameField.setWidth(320.0);
        nameField.setText(Messages.get("jumplist.field.name.default"));
        WTextField argumentsField = new WTextField();
        argumentsField.setPlaceholderText(Messages.get("jumplist.field.arguments.placeholder"));
        argumentsField.setWidth(320.0);
        argumentsField.setText("/new");

        WLabel itemsLabel = new WLabel("");
        WLabel result = new WLabel("");

        WButton addButton = new WButton(Messages.get("jumplist.button.add"));
        addButton.addActionListener(() -> {
            edit(jumpList -> {
                WJumpListItem item = WJumpListItem.of(argumentsField.getText(), nameField.getText());
                item.setDescription(Messages.get("jumplist.item.description"));
                item.setGroupName("Gallery");
                jumpList.add(item);
                return Messages.get("jumplist.result.added");
            }, result, itemsLabel);
        });

        WButton separatorButton = new WButton(Messages.get("jumplist.button.addseparator"));
        separatorButton.addActionListener(() -> {
            edit(jumpList -> {
                jumpList.add(WJumpListItem.separator());
                return Messages.get("jumplist.result.separatoradded");
            }, result, itemsLabel);
        });

        WButton clearButton = new WButton(Messages.get("jumplist.button.clear"));
        clearButton.addActionListener(() -> {
            edit(jumpList -> {
                jumpList.removeAll();
                return Messages.get("jumplist.result.cleared");
            }, result, itemsLabel);
        });

        try {
            refreshItems(WJumpList.load(), itemsLabel);
        } catch (Exception ignored) {
        }

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(addButton);
        buttons.add(separatorButton);
        buttons.add(clearButton);

        WPanel body = new WPanel(8.0);
        body.add(nameField);
        body.add(argumentsField);
        body.add(buttons);
        body.add(itemsLabel);
        body.add(result);
        return GalleryScaffold.buildExample(
            Messages.get("jumplist.example.edit.title"), body);
    }

    private static void refreshItems(WJumpList jumpList, WLabel itemsLabel) {
        List<String> names = new ArrayList<String>();
        for (WJumpListItem item : jumpList.getItems()) {
            names.add(item.isSeparator() ? "――――" : item.getDisplayName());
        }
        itemsLabel.setText(
            names.isEmpty()
                ? Messages.get("jumplist.items.none")
                : Messages.get("jumplist.items.current", String.join(" / ", names)));
    }

    private static void edit(Function<WJumpList, String> block, WLabel result, WLabel itemsLabel) {
        String message;
        try {
            WJumpList jumpList = WJumpList.load();
            String text = block.apply(jumpList);
            jumpList.save();
            refreshItems(jumpList, itemsLabel);
            message = text;
        } catch (Exception e) {
            message = Messages.get("jumplist.result.failed", e.getMessage());
        }
        result.setText(message);
    }

    // endregion
}
