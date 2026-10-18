package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Can be created from Java with both {@code new WXamlUICommand()} and {@code new WXamlUICommand("label")}. */
class WXamlUICommandForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutALabel() {
        onUiThread(() -> {
            assertEquals("", new WXamlUICommand().getLabel());
            assertEquals("Command", new WXamlUICommand("Command").getLabel());
        });
    }
}
