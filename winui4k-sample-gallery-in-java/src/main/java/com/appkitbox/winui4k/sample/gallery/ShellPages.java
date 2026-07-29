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

import kotlin.Pair;

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
            "A notification shown in the Action Center and as a toast popup. "
                + "Try out WAppNotification / WAppNotificationManager's various features.");

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
        WLabel setting = new WLabel("Setting: not fetched yet");

        WButton refreshButton = new WButton("Fetch Setting");
        refreshButton.addActionListener(() -> {
            String text;
            try {
                text = "Setting: " + WAppNotificationManager.getSetting();
            } catch (Exception e) {
                text = "Failed to fetch Setting: " + e.getMessage();
            }
            setting.setText(text);
        });

        WPanel body = new WPanel(8.0);
        body.add(supported);
        body.add(setting);
        body.add(refreshButton);
        return GalleryScaffold.buildExample("Whether notifications are usable in this environment (IsSupported / Setting)", body);
    }

    /** A basic notification: a 2-line body + attribution text + display duration. */
    private static WComponent buildSimpleNotificationExample() {
        WTextField titleField = new WTextField("Line 1 (title)");
        titleField.setWidth(320.0);
        titleField.setText("A notification from winui4k");
        WTextField bodyField = new WTextField("Line 2 (body)");
        bodyField.setWidth(320.0);
        bodyField.setText("A toast notification was sent from Kotlin.");
        WCheckBox longDuration = new WCheckBox("Use a longer display duration (Duration.LONG)");
        WLabel result = new WLabel("");

        WButton sendButton = new WButton("Send notification");
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
                message = "Sent";
            } catch (Exception e) {
                message = "Failed to send: " + e.getMessage();
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
            "A basic notification (AddText / SetAttributionText / SetTag / SetGroup / SetDuration)", body);
    }

    /** A notification with buttons and receiving clicks: AddArgument / AddButton / NotificationInvoked. */
    private static WComponent buildInteractiveNotificationExample() {
        WComboBox scenarioComboBox = new WComboBox(Collections.<String>emptyList());
        scenarioComboBox.setHeader("Scenario");
        for (NotificationScenario scenario : NotificationScenario.values()) {
            scenarioComboBox.addItem(scenario.name());
        }
        scenarioComboBox.setSelectedIndex(0);

        WLabel received = new WLabel("Waiting for a click (clicking the notification body or a button delivers the argument here)");
        WAppNotificationManager.addNotificationInvokedListener(argument -> {
            received.setText("Received argument: " + argument);
        });

        WButton sendButton = new WButton("Send notification with buttons");
        sendButton.addActionListener(() -> {
            String message;
            try {
                ensureNotificationRegistered();
                NotificationScenario scenario = NotificationScenario.values()[scenarioComboBox.getSelectedIndex()];
                WAppNotificationManager.show(
                    new WAppNotification("Want to reply?")
                        .addText("Button clicks can be received on the app side.")
                        .addArgument("action", "open")
                        .addButton("Approve", new Pair<String, String>("action", "approve"))
                        .addButton("Reject", new Pair<String, String>("action", "reject"))
                        .setScenario(scenario));
                message = "Sent. Click the notification";
            } catch (Exception e) {
                message = "Failed to send: " + e.getMessage();
            }
            received.setText(message);
        });

        WPanel body = new WPanel(8.0);
        body.add(scenarioComboBox);
        body.add(sendButton);
        body.add(received);
        return GalleryScaffold.buildExample(
            "A notification with buttons and receiving clicks (AddButton / AddArgument / NotificationInvoked)", body);
    }

    // endregion

    // region BadgeNotification

    /** The BadgeNotification page: lines up demos for trying out WBadgeNotification's various features. */
    static WComponent buildBadgeNotificationPage() {
        WPanel page = GalleryScaffold.buildPage(
            "BadgeNotification",
            "A badge overlaid on the app's taskbar icon. Try out WBadgeNotification's various features. "
                + "Badges can only be shown by an app with a package identity; setting one under an unpackaged run errors out.");

        page.add(buildBadgeCountExample());
        page.add(buildBadgeGlyphExample());
        return page;
    }

    /** A numeric badge: SetBadgeAsCount / ClearBadge. */
    private static WComponent buildBadgeCountExample() {
        WLabel result = new WLabel("Check the taskbar icon");

        WButton clearButton = new WButton("Clear");
        clearButton.addActionListener(() -> {
            String message;
            try {
                WBadgeNotification.clear();
                message = "Cleared the badge";
            } catch (Exception e) {
                message = "Failed to clear: " + e.getMessage();
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
        return GalleryScaffold.buildExample("A numeric badge (SetBadgeAsCount / ClearBadge)", body);
    }

    private static WButton countButton(int count, WLabel result) {
        WButton button = new WButton(String.valueOf(count));
        button.addActionListener(() -> {
            String message;
            try {
                WBadgeNotification.setCount(count);
                message = "Set the badge to " + count + (count > 99 ? " (100 and above shows as 99+)" : "");
            } catch (Exception e) {
                message = "Failed to set: " + e.getMessage();
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
        WButton applyButton = new WButton("Set glyph");
        applyButton.addActionListener(() -> {
            BadgeGlyph glyph = BadgeGlyph.values()[glyphComboBox.getSelectedIndex()];
            String message;
            try {
                WBadgeNotification.setGlyph(glyph);
                message = "Set the badge to " + glyph;
            } catch (Exception e) {
                message = "Failed to set: " + e.getMessage();
            }
            result.setText(message);
        });

        WPanel row = new WPanel(8.0, Orientation.HORIZONTAL);
        row.add(glyphComboBox);
        // The ComboBox is taller because of its header, so nudge this down to align with the input box
        applyButton.setVerticalAlignment(VerticalAlignment.BOTTOM);
        row.add(applyButton);

        WPanel body = new WPanel(8.0);
        body.add(row);
        body.add(result);
        return GalleryScaffold.buildExample("A status-glyph badge (SetBadgeAsGlyph)", body);
    }

    // endregion

    // region JumpList

    /** The JumpList page: lines up demos for trying out WJumpList / WJumpListItem's various features. */
    static WComponent buildJumpListPage() {
        WPanel page = GalleryScaffold.buildPage(
            "JumpList",
            "Adds custom tasks or items to the menu shown when right-clicking the app's taskbar icon. "
                + "Try out WJumpList / WJumpListItem's various features.");

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
            loadFailure = new IllegalStateException("JumpList.IsSupported is false");
        }
        if (loadFailure != null) {
            WLabel message = new WLabel(
                "Jump lists only work for an app with a package identity, so they don't work "
                    + "under an unpackaged run (launching java.exe directly). "
                    + "(" + loadFailure.getMessage() + ")");
            message.setTextWrapping(TextWrapping.WRAP);
            page.add(GalleryScaffold.buildExample("Not usable in this environment", message));
            return page;
        }

        page.add(buildJumpListEditExample());
        return page;
    }

    /** Adding an item, removing all, saving, and the current item list. */
    private static WComponent buildJumpListEditExample() {
        WTextField nameField = new WTextField("DisplayName");
        nameField.setWidth(320.0);
        nameField.setText("New document");
        WTextField argumentsField = new WTextField("Arguments (launch arguments)");
        argumentsField.setWidth(320.0);
        argumentsField.setText("/new");

        WLabel itemsLabel = new WLabel("");
        WLabel result = new WLabel("");

        WButton addButton = new WButton("Add item and save");
        addButton.addActionListener(() -> {
            edit(jumpList -> {
                WJumpListItem item = WJumpListItem.of(argumentsField.getText(), nameField.getText());
                item.setDescription("An item added by WinUI4K Gallery");
                item.setGroupName("Gallery");
                jumpList.add(item);
                return "Added. Right-click the taskbar icon to check";
            }, result, itemsLabel);
        });

        WButton separatorButton = new WButton("Add separator and save");
        separatorButton.addActionListener(() -> {
            edit(jumpList -> {
                jumpList.add(WJumpListItem.separator());
                return "Added a separator";
            }, result, itemsLabel);
        });

        WButton clearButton = new WButton("Remove all items and save");
        clearButton.addActionListener(() -> {
            edit(jumpList -> {
                jumpList.removeAll();
                return "Removed all items";
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
            "Editing items (Items / SaveAsync / CreateWithArguments / CreateSeparator)", body);
    }

    private static void refreshItems(WJumpList jumpList, WLabel itemsLabel) {
        List<String> names = new ArrayList<String>();
        for (WJumpListItem item : jumpList.getItems()) {
            names.add(item.isSeparator() ? "――――" : item.getDisplayName());
        }
        itemsLabel.setText(
            names.isEmpty()
                ? "No custom items"
                : "Current items: " + String.join(" / ", names));
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
            message = "Operation failed: " + e.getMessage();
        }
        result.setText(message);
    }

    // endregion
}
