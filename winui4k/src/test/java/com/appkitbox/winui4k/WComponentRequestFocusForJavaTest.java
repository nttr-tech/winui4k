package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the same shapes as JComponent.setRequestFocusEnabled / isRequestFocusEnabled / grabFocus and Component.isFocusOwner
 * in the Java that Swing developers would write (the way to keep toolbar buttons from taking the focus when pressed).
 */
class WComponentRequestFocusForJavaTest {
    @Test
    void evenWithSetRequestFocusEnabledFalseGrabFocusAndRequestFocusInWindowMoveTheFocusAndIsFocusOwnerBecomesTrue() {
        WButton toolButton = onUiThreadGet(() -> new WButton("Bold"));
        WTextField field = onUiThreadGet(() -> new WTextField("Body"));
        WPanel panel = onUiThreadGet(() -> {
            WPanel box = new WPanel();
            box.add(toolButton);
            box.add(field);
            toolButton.setRequestFocusEnabled(false);
            return box;
        });
        UiTestHarness.attachAndAwaitLoaded(panel);
        try {
            onUiThread(() -> {
                assertFalse(toolButton.isRequestFocusEnabled());
                assertTrue(field.isRequestFocusEnabled());
                field.grabFocus();
                assertTrue(field.isFocusOwner());
                assertFalse(toolButton.isFocusOwner());
                assertTrue(toolButton.requestFocusInWindow());
                assertTrue(toolButton.isFocusOwner());
                assertFalse(field.isFocusOwner());
            });
        } finally {
            UiTestHarness.detach(panel);
        }
    }
}
