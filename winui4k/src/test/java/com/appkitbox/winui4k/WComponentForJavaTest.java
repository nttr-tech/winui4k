package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the same names as JComponent's setToolTipText / setName / setMinimumSize / requestFocusInWindow, etc.,
 * from Java, written the way Swing developers would write them (checked on a WButton).
 */
class WComponentForJavaTest {
    @Test
    void setToolTipTextAndGetToolTipTextSetAndClearTheTooltip() {
        onUiThread(() -> {
            WButton button = new WButton("Save");
            assertNull(button.getToolTipText());
            button.setToolTipText("Saves the file");
            assertEquals("Saves the file", button.getToolTipText());
            button.setToolTipText(null);
            assertNull(button.getToolTipText());
        });
    }

    @Test
    void setNameAndGetNameReadBackTheNameWhichIsEmptyWhenUnset() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            assertEquals("", button.getName());
            button.setName("okButton");
            assertEquals("okButton", button.getName());
        });
    }

    @Test
    void setMinimumSizeWithAWSizeCanBeReadBackWithGetMinimumSize() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            assertEquals(new WSize(0, 0), button.getMinimumSize());
            button.setMinimumSize(new WSize(120, 32));
            assertEquals(new WSize(120.0, 32.0), button.getMinimumSize());
            assertEquals(120.0, button.getMinimumSize().getWidth());
            assertEquals(32.0, button.getMinimumSize().getHeight());
        });
    }

    @Test
    void setPreferredSizeAndSetMaximumSizeCanAlsoBeSetWithWSize() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            button.setPreferredSize(new WSize(200, 40));
            button.setMaximumSize(new WSize(300, 60));
            assertEquals(new WSize(200, 40), button.getPreferredSize());
            assertEquals(new WSize(300, 60), button.getMaximumSize());
        });
    }

    @Test
    void aButtonAttachedToAWindowHasFocusAfterRequestFocusInWindow() {
        WButton first = UiTestHarness.onUiThreadGet(() -> new WButton("One"));
        WButton second = UiTestHarness.onUiThreadGet(() -> new WButton("Two"));
        WPanel panel = UiTestHarness.onUiThreadGet(() -> {
            WPanel box = new WPanel();
            box.add(first);
            box.add(second);
            return box;
        });
        UiTestHarness.attachAndAwaitLoaded(panel);
        try {
            onUiThread(() -> {
                assertFalse(first.hasFocus());
                assertTrue(first.requestFocusInWindow());
                assertTrue(first.hasFocus());
                assertFalse(second.hasFocus());
                second.requestFocus();
                assertFalse(first.hasFocus());
                assertTrue(second.hasFocus());
            });
        } finally {
            UiTestHarness.detach(panel);
        }
    }

    @Test
    void aButtonWithSetFocusableFalseDoesNotReceiveFocus() {
        WButton button = UiTestHarness.onUiThreadGet(() -> {
            WButton created = new WButton("Not focusable");
            assertTrue(created.isFocusable());
            created.setFocusable(false);
            return created;
        });
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                assertFalse(button.isFocusable());
                assertFalse(button.requestFocusInWindow());
                assertFalse(button.hasFocus());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }

    @Test
    void requestFocusInWindowIsFalseForAButtonNotYetAttachedToAWindow() {
        onUiThread(() -> {
            WButton button = new WButton("Not attached");
            assertFalse(button.requestFocusInWindow());
            assertFalse(button.hasFocus());
        });
    }
}
