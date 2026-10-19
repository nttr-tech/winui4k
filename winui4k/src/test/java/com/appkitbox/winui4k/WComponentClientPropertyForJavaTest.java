package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use the same form as JComponent.putClientProperty / getClientProperty, in Java as a Swing developer would
 * write it (keys are Object, values are Object and retrieved with a cast; putting null removes it).
 */
class WComponentClientPropertyForJavaTest {
    @Test
    void valuePutWithPutClientPropertyCanBeRetrievedWithCastFromGetClientProperty() {
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        onUiThread(() -> {
            button.putClientProperty("id", 42);
            button.putClientProperty("title", "Save");
        });
        int id = (Integer) onUiThreadGet(() -> button.getClientProperty("id"));
        String title = (String) onUiThreadGet(() -> button.getClientProperty("title"));
        assertEquals(42, id);
        assertEquals("Save", title);
    }

    @Test
    void objectCanBeUsedAsKeyAndSameValueInstanceIsReturned() {
        Object key = new Object();
        List<String> value = new ArrayList<>();
        WLabel label = onUiThreadGet(() -> new WLabel("L"));
        onUiThread(() -> label.putClientProperty(key, value));
        assertSame(value, onUiThreadGet(() -> label.getClientProperty(key)));
    }

    @Test
    void puttingNullRemovesAndUnsetKeyIsNull() {
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        onUiThread(() -> button.putClientProperty("k", "v"));
        assertEquals("v", onUiThreadGet(() -> button.getClientProperty("k")));
        onUiThread(() -> button.putClientProperty("k", null));
        assertNull(onUiThreadGet(() -> button.getClientProperty("k")));
        assertNull(onUiThreadGet(() -> button.getClientProperty("missing")));
    }

    @Test
    void nullKeyThrowsNullPointerExceptionSameAsSwing() {
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        assertThrows(NullPointerException.class, () -> button.putClientProperty(null, "v"));
        assertThrows(NullPointerException.class, () -> button.getClientProperty(null));
    }
}
