package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests that set the background colors of WinUI4K controls from Java in the same way as Swing's JComponent.setBackground / getBackground.
 */
class WControlBackgroundForJavaTest {
    @Test
    void colorsSetWithSetBackgroundOnTextFieldsAndSlidersReadBackWithGetBackground() {
        onUiThread(() -> {
            WColor color = new WColor(200, 40, 40);
            WTextField field = new WTextField();
            field.setBackground(color);
            WSlider slider = new WSlider();
            slider.setBackground(color);
            assertEquals(color, field.getBackground());
            assertEquals(color, slider.getBackground());
        });
    }

    @Test
    void setBackgroundWorksEvenThroughAVariableOfTypeWControl() {
        onUiThread(() -> {
            WColor color = new WColor(10, 120, 60);
            WControl[] controls = {new WPasswordField(), new WSpinner(), new WButton("Save"), new WToggleButton("Bold")};
            for (WControl c : controls) {
                c.setBackground(color);
            }
            for (WControl c : controls) {
                assertEquals(color, c.getBackground());
            }
        });
    }

    @Test
    void passingNullToSetBackgroundReturnsToTheThemeDefaultColor() {
        onUiThread(() -> {
            WSlider slider = new WSlider();
            slider.setBackground(new WColor(1, 2, 3));
            slider.setBackground(null);
            assertEquals(new WSlider().getBackground(), slider.getBackground());
        });
    }
}
