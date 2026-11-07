package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.function.Function;
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
            assertEquals("APPLE", combo.getItem(0));
            assertEquals("ORANGE", combo.getItem(1));
            assertEquals("apple", combo.getItemAt(0));
            combo.setSelectedIndex(1);
            assertEquals("orange", combo.getSelectedItem());
            assertEquals("ORANGE", combo.getSelectedItemText());
            combo.setRenderer(null);
            assertNull(combo.getRenderer());
            assertEquals("apple", combo.getItem(0));
        });
    }

    @Test
    void setRendererAcceptsAJavaUtilFunctionAndGetRendererReturnsTheSameFunction() {
        onUiThread(() -> {
            WComboBox combo = new WComboBox(Arrays.asList("apple", "orange"));
            Function<Object, String> renderer = value -> "[" + value + "]";
            combo.setRenderer(renderer);
            Function<Object, String> read = combo.getRenderer();
            assertSame(renderer, read);
            assertEquals("[apple]", combo.getItemAt(0));
            assertEquals("[orange]", combo.getItem(1));
            combo.setRenderer(null);
            assertNull(combo.getRenderer());
            assertEquals("orange", combo.getItemAt(1));
        });
    }
}
