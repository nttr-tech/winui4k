package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Can be created from Java with both {@code new WSplitButton()} and {@code new WSplitButton("text")}. */
class WSplitButtonForJavaTest {
    @Test
    void canBeCreatedWithOrWithoutText() {
        onUiThread(() -> {
            assertEquals("", new WSplitButton().getText());
            assertEquals("Split", new WSplitButton("Split").getText());
        });
    }
}
