package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests that use WTitleBar's getters / setters and listeners (Runnable, lambdas) in the same way Java developers would write it. */
class WTitleBarForJavaTest {
    @Test
    void theTitleAndSubtitleCanBeWrittenWithSettersAndReadBackWithGetters() {
        onUiThread(() -> {
            WTitleBar bar = new WTitleBar();
            bar.setTitle("Document 1");
            bar.setSubtitle("Draft");
            assertEquals("Document 1", bar.getTitle());
            assertEquals("Draft", bar.getSubtitle());
            bar.setBackButtonVisible(true);
            assertTrue(bar.isBackButtonVisible());
            bar.setPaneToggleButtonVisible(false);
            assertFalse(bar.isPaneToggleButtonVisible());
        });
    }

    @Test
    void componentsCanBePlacedInTheHeaders() {
        onUiThread(() -> {
            WTitleBar bar = new WTitleBar();
            WButton button = new WButton("Left");
            bar.setLeftHeader(button);
            assertSame(button, bar.getLeftHeader());
            bar.setLeftHeader(null);
            assertNull(bar.getLeftHeader());
        });
    }

    @Test
    void listenersCanBeAddedAndRemovedWithRunnableLambdas() {
        onUiThread(() -> {
            WTitleBar bar = new WTitleBar();
            Runnable back = () -> { };
            Runnable toggle = () -> { };
            bar.addBackRequestedListener(back);
            bar.addPaneToggleRequestedListener(toggle);
            bar.removeBackRequestedListener(back);
            bar.removePaneToggleRequestedListener(toggle);
        });
    }
}
