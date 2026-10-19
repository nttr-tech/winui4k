package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Tests that color buttons from Java, written the same way as setBackground / setForeground of JToggleButton /
 * JCheckBox / JRadioButton. How each visual state is painted is checked in WToggleButtonColorTest.
 */
class WToggleButtonColorForJavaTest {
    @Test
    void colorsSetBySetBackgroundAndSetForegroundCanBeReadByGetBackgroundAndGetForeground() {
        onUiThread(() -> {
            for (WToggleButton button : Arrays.asList(new WToggleButton("Bold"), new WCheckBox("Notifications"), new WRadioButton("Small"))) {
                button.setBackground(new WColor(255, 240, 200));
                button.setForeground(new WColor(0, 0, 160));
                assertEquals(new WColor(255, 240, 200), button.getBackground());
                assertEquals(new WColor(0, 0, 160), button.getForeground());
            }
        });
    }

    @Test
    void passingNullReturnsToTheThemeDefaultColor() {
        onUiThread(() -> {
            WCheckBox box = new WCheckBox("Notifications");
            WColor themeForeground = box.getForeground();
            box.setForeground(new WColor(200, 0, 0));
            assertNotEquals(themeForeground, box.getForeground());
            box.setForeground(null);
            assertEquals(themeForeground, box.getForeground());

            WColor themeBackground = box.getBackground();
            box.setBackground(new WColor(200, 0, 0));
            box.setBackground(null);
            assertEquals(themeBackground, box.getBackground());
        });
    }
}
