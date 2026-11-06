package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Tests that pass, as a Java lambda, a function that turns items into display strings instead of Swing's combo.setRenderer(...).
 */
class WComboBoxRendererForJavaTest {
    @Test
    void passingALambdaToSetRendererChangesTheDisplayStrings() {
        onUiThread(() -> {
            WComboBox combo = new WComboBox(Arrays.asList("apple", "orange"));
            combo.setRenderer(value -> value.toString().toUpperCase());
            assertEquals("APPLE", combo.getItemAt(0));
            assertEquals("ORANGE", combo.getItemAt(1));
            combo.setRenderer(null);
            assertNull(combo.getRenderer());
            assertEquals("apple", combo.getItemAt(0));
        });
    }
}
