package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests that use {@code frame.setDefaultButton(button)} from Java, the same shape as JRootPane.setDefaultButton. */
class WFrameForJavaTest {
    private final WFrame frame = UiTestHarness.sharedFrame();

    @AfterEach
    void clearDefaultButton() {
        onUiThread(() -> frame.setDefaultButton(null));
    }

    @Test
    void onlyTheButtonSetWithSetDefaultButtonBecomesIsDefaultButton() {
        onUiThread(() -> {
            WButton ok = new WButton("OK");
            WButton cancel = new WButton("Cancel");
            assertNull(frame.getDefaultButton());
            assertFalse(ok.isDefaultButton());

            frame.setDefaultButton(ok);
            assertSame(ok, frame.getDefaultButton());
            assertTrue(ok.isDefaultButton());
            assertFalse(cancel.isDefaultButton());
        });
    }

    @Test
    void switchingToAnotherButtonMakesThePreviousButtonNoLongerIsDefaultButton() {
        onUiThread(() -> {
            WButton ok = new WButton("OK");
            WButton cancel = new WButton("Cancel");
            frame.setDefaultButton(ok);
            frame.setDefaultButton(cancel);
            assertFalse(ok.isDefaultButton());
            assertTrue(cancel.isDefaultButton());
            assertSame(cancel, frame.getDefaultButton());
        });
    }

    @Test
    void passingNullClearsTheDefaultButton() {
        onUiThread(() -> {
            WButton ok = new WButton("OK");
            frame.setDefaultButton(ok);
            frame.setDefaultButton(null);
            assertNull(frame.getDefaultButton());
            assertFalse(ok.isDefaultButton());
        });
    }
}
