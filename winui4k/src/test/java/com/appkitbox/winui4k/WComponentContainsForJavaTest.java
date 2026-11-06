package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the same shapes as Component.contains(int, int) / contains(Point) in the Java that Swing developers would write
 * (passing getPoint() of a mouse event to contains to check whether the release position is inside the component).
 */
class WComponentContainsForJavaTest {
    @Test
    void containsIsTrueOnlyInsideTheWidthAndHeightAndAMouseEventsGetPointCanBePassedAsIs() {
        WPanel panel = onUiThreadGet(() -> {
            WPanel box = new WPanel();
            box.setPreferredSize(new WSize(100, 40));
            return box;
        });
        UiTestHarness.attachAndAwaitLoaded(panel);
        try {
            onUiThread(() -> {
                panel.updateLayout();
                assertTrue(panel.contains(0, 0));
                assertTrue(panel.contains(99, 39));
                assertFalse(panel.contains(100, 39));
                assertFalse(panel.contains(-1, 0));
                WMouseEvent inside = new WMouseEvent(panel, WMouseEvent.MOUSE_RELEASED, 0L, 0, 10.5, 20.5, 1, false,
                        WMouseEvent.BUTTON1);
                WMouseEvent outside = new WMouseEvent(panel, WMouseEvent.MOUSE_RELEASED, 0L, 0, 120.0, 20.0, 1, false,
                        WMouseEvent.BUTTON1);
                assertTrue(panel.contains(inside.getPoint()));
                assertFalse(panel.contains(outside.getPoint()));
            });
        } finally {
            UiTestHarness.detach(panel);
        }
    }
}
