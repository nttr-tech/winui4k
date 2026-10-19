package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests using the same form as Component.requestFocusInWindow / isFocusable / setFocusable / hasFocus, in Java as
 * Swing developers write it (requestFocusInWindow of WSpinner, the counterpart of JSpinner, returns true; it returns
 * false for a component with setFocusable(false)).
 */
class WComponentFocusableForJavaTest {
    @Test
    void wSpinnerRequestFocusInWindowReturnsTrueAndHasFocusIsTrue() {
        WSpinner spinner = onUiThreadGet(() -> new WSpinner(3));
        UiTestHarness.attachAndAwaitLoaded(spinner);
        try {
            onUiThread(() -> {
                assertTrue(spinner.isFocusable());
                assertTrue(spinner.requestFocusInWindow());
                assertTrue(spinner.hasFocus());
            });
        } finally {
            UiTestHarness.detach(spinner);
        }
    }

    @Test
    void wSpinnerAndWAutoSuggestBoxWithSetFocusableFalseDoNotReceiveFocus() {
        WSpinner spinner = onUiThreadGet(() -> new WSpinner(3));
        WAutoSuggestBox suggest = onUiThreadGet(() -> new WAutoSuggestBox("Find"));
        WPanel panel = onUiThreadGet(() -> {
            WPanel box = new WPanel();
            box.add(spinner);
            box.add(suggest);
            spinner.setFocusable(false);
            suggest.setFocusable(false);
            return box;
        });
        UiTestHarness.attachAndAwaitLoaded(panel);
        try {
            onUiThread(() -> {
                assertFalse(spinner.isFocusable());
                assertFalse(spinner.requestFocusInWindow());
                assertFalse(spinner.hasFocus());
                assertFalse(suggest.isFocusable());
                assertFalse(suggest.requestFocusInWindow());
                assertFalse(suggest.hasFocus());
            });
        } finally {
            UiTestHarness.detach(panel);
        }
    }
}
