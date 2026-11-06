package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Smoke tests of WJumpList / WAppNotification as seen from Java. The bodies are not run in environments where isSupported is false. */
class WShellSmokeForJavaTest {
    @Test
    void notificationsCanBeBuiltWithMethodChaining() {
        if (!WAppNotificationManager.isSupported()) {
            return;
        }
        onUiThread(() -> {
            new WAppNotification("Title")
                    .addText("Body")
                    .addArgument("action", "open")
                    .addButton("Open", Collections.singletonMap("action", "open"))
                    .setTag("smoke");
        });
    }

    @Test
    void jumpListItemsCanBeCreatedAndReadBack() {
        if (!WJumpList.isSupported()) {
            return;
        }
        onUiThread(() -> {
            WJumpListItem item = WJumpListItem.of("--open", "Open");
            assertEquals("--open", item.getArguments());
            assertEquals("Open", item.getDisplayName());
            assertTrue(WJumpListItem.separator().isSeparator());
        });
    }
}
