package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the way toolbar-like buttons are made with Swing's JToggleButton (setContentAreaFilled(false) and setBorderPainted(false))
 * on WToggleButton from Java.
 */
class WToggleButtonFillForJavaTest {
    @Test
    void valuesSwitchedWithSetContentAreaFilledAndSetBorderPaintedReadBack() {
        onUiThread(() -> {
            WToggleButton bold = new WToggleButton("B");
            assertTrue(bold.isContentAreaFilled());
            assertTrue(bold.isBorderPainted());
            bold.setContentAreaFilled(false);
            bold.setBorderPainted(false);
            assertFalse(bold.isContentAreaFilled());
            assertFalse(bold.isBorderPainted());
        });
    }

    @Test
    void theSetBackgroundColorIsKeptEvenWithSetContentAreaFilledFalse() {
        onUiThread(() -> {
            WColor color = new WColor(200, 40, 40);
            WToggleButton bold = new WToggleButton("B");
            bold.setBackground(color);
            bold.setContentAreaFilled(false);
            assertEquals(color, bold.getBackground());
        });
    }
}
