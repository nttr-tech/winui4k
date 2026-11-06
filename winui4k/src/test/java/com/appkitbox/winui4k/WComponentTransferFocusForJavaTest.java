package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use transferFocus / transferFocusBackward in the Java a Swing developer writes
 * (advancing from a form's input field to the next one, or going back to the previous one, in code).
 */
class WComponentTransferFocusForJavaTest {
    @Test
    void transferFocusAdvancesFromAnInputFieldToTheNext_transferFocusBackwardGoesBack() {
        WTextField[] fields = onUiThreadGet(() -> new WTextField[] {new WTextField(), new WTextField()});
        WPanel form = onUiThreadGet(() -> {
            WPanel panel = new WPanel();
            panel.add(fields[0]);
            panel.add(fields[1]);
            return panel;
        });
        UiTestHarness.attachAndAwaitLoaded(form);
        try {
            onUiThread(() -> {
                WTextField name = fields[0];
                WTextField mail = fields[1];
                name.requestFocusInWindow();
                name.transferFocus();
                assertTrue(mail.hasFocus());
                mail.transferFocusBackward();
                assertTrue(name.hasFocus());
            });
        } finally {
            UiTestHarness.detach(form);
        }
    }
}
