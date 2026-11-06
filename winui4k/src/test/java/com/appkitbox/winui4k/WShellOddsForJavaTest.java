package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WDisplayArea.all() / getBounds() and WAppNotificationManager.addActionListener in the same way Java developers
 * who have used GraphicsEnvironment.getScreenDevices() and ActionListener would write it.
 */
class WShellOddsForJavaTest {
    @Test
    void displayAreasAreListedWithBounds() {
        List<WRectangle> bounds = new ArrayList<>();
        onUiThread(() -> {
            for (WDisplayArea area : WDisplayArea.all()) {
                bounds.add(area.getBounds());
            }
        });
        assertFalse(bounds.isEmpty());
        assertTrue(bounds.stream().anyMatch(b -> b.getX() == 0 && b.getY() == 0 && b.getWidth() > 0));
    }

    /** How the argument arrives is checked on the Kotlin side (WShellOddsTest). Here it checks that registration and removal can be written from Java. */
    @Test
    void actionListenerCanBeAddedAndRemovedWithLambda() {
        if (!WAppNotificationManager.isSupported()) return;
        WActionListener listener = e -> assertEquals("action=join", e.getActionCommand());
        onUiThread(() -> {
            WAppNotificationManager.addActionListener(listener);
            WAppNotificationManager.removeActionListener(listener);
        });
    }
}
