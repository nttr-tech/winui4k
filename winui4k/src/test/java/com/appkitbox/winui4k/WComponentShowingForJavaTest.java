package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use isShowing / isDisplayable / getTopLevelAncestor in the Java a Swing developer writes
 * (processing only while a button is on screen and looking up the window the button is placed in).
 */
class WComponentShowingForJavaTest {
    @Test
    void notShownBeforePlacing_afterPlacingIsShowingIsTrueAndGetTopLevelAncestorIsTheWindow() {
        WFrame frame = UiTestHarness.sharedFrame();
        WButton[] holder = new WButton[1];
        onUiThread(() -> {
            WButton button = new WButton("OK");
            assertFalse(button.isDisplayable());
            assertFalse(button.isShowing());
            assertNull(button.getTopLevelAncestor());
            holder[0] = button;
        });
        WButton button = holder[0];
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            onUiThread(() -> {
                assertTrue(button.isDisplayable());
                assertTrue(button.isShowing());
                WFrame topLevel = button.getTopLevelAncestor();
                assertSame(frame, topLevel);
                button.setVisible(false);
                assertFalse(button.isShowing());
                assertTrue(button.isDisplayable());
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }
}
