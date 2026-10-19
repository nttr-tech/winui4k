package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for sizing WTextPane by columns and rows from Java written the same way as by developers used to Swing's
 * JTextArea(int rows, int columns). The size values derived from columns and rows are checked in WTextPaneSizeTest.
 */
class WTextPaneSizeForJavaTest {
    @Test
    void textPaneCreatedWithRowsAndColumnsConstructorGetsItsSizeWhenPlacedInWindow() throws InterruptedException {
        WTextPane pane = UiTestHarness.onUiThreadGet(() -> new WTextPane(4, 30));
        WTextPane taller = UiTestHarness.onUiThreadGet(() -> new WTextPane(8, 30));
        UiTestHarness.attachAndAwaitLoaded(pane);
        UiTestHarness.attachAndAwaitLoaded(taller);
        try {
            double[] sizes = new double[4];
            // The size from columns and rows is measured on Loaded, so wait a little until it is applied
            for (int attempt = 0; attempt < 60; attempt++) {
                onUiThread(() -> {
                    pane.updateLayout();
                    taller.updateLayout();
                    sizes[0] = pane.getActualWidth();
                    sizes[1] = pane.getActualHeight();
                    sizes[2] = taller.getActualWidth();
                    sizes[3] = taller.getActualHeight();
                });
                if (sizes[3] > sizes[1]) {
                    break;
                }
                Thread.sleep(50);
            }
            assertTrue(sizes[0] > 0 && sizes[1] > 0);
            assertEquals(sizes[0], sizes[2], 1.0);
            assertTrue(sizes[3] > sizes[1], "more rows make it taller: " + sizes[3] + " > " + sizes[1]);
        } finally {
            UiTestHarness.detach(pane);
            UiTestHarness.detach(taller);
        }
    }
}
