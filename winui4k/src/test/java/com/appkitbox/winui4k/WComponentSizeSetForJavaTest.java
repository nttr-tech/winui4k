package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use isPreferredSizeSet / isMinimumSizeSet / isMaximumSizeSet in the Java a Swing developer writes
 * (custom layout code that checks "whether the user decided the size" and measures from the content if not).
 */
class WComponentSizeSetForJavaTest {
    @Test
    void callingSetPreferredSizeAndSoOnMakesIsXxxSetTrue() {
        onUiThread(() -> {
            WPanel panel = new WPanel();
            assertFalse(panel.isPreferredSizeSet());
            assertFalse(panel.isMinimumSizeSet());
            assertFalse(panel.isMaximumSizeSet());

            panel.setPreferredSize(new WSize(120, 40));
            panel.setMinimumSize(new WSize(60, 20));
            panel.setMaximumSize(new WSize(240, 80));
            assertTrue(panel.isPreferredSizeSet());
            assertTrue(panel.isMinimumSizeSet());
            assertTrue(panel.isMaximumSizeSet());
        });
    }

    @Test
    void resettingBothPreferredWidthAndHeightToNaNMakesIsPreferredSizeSetFalseAgain() {
        onUiThread(() -> {
            WButton button = new WButton("OK");
            button.setPreferredSize(new WSize(120, 40));
            button.setPreferredSize(new WSize(Double.NaN, Double.NaN));
            assertFalse(button.isPreferredSizeSet());
        });
    }
}
