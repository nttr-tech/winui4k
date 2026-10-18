package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Can be created from Java with both {@code new WToggleButton()} and {@code new WToggleButton("text")}. */
class WToggleButtonForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WToggleButton().getText());
            assertEquals("Toggle", new WToggleButton("Toggle").getText());
        });
    }
}
