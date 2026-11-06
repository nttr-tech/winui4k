package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.ParseException;
import org.junit.jupiter.api.Test;

/**
 * Tests that Swing's JSpinner form `try { spinner.commitEdit(); } catch (ParseException e) { ... }` compiles as is with WSpinner
 * (commitEdit declares throws ParseException) and works.
 */
class WSpinnerCommitEditForJavaTest {
    @Test
    void commitEditCanBeCalledInsideACatchForParseException() {
        onUiThread(() -> {
            WSpinner spinner = new WSpinner(new WSpinnerNumberModel(3, 0, 10, 1));
            try {
                spinner.commitEdit(); // Before being placed in a window there is nothing to commit, so it does nothing
            } catch (ParseException e) {
                fail("commitEdit before placing does not throw: " + e.getMessage());
            }
            assertEquals(3, spinner.getModel().getValue());
        });
    }
}
