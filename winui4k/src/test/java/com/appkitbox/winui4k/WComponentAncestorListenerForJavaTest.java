package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Tests that use addAncestorListener in the Java a Swing developer writes
 * (writing the three AncestorListener methods in an anonymous class and initializing when it appears on screen).
 */
class WComponentAncestorListenerForJavaTest {
    @Test
    void ancestorAddedIsCalledWhenItAppearsOnScreen_getComponentAndGetIDCanBeRead() throws InterruptedException {
        List<String> calls = new ArrayList<>();
        List<WAncestorEvent> received = new ArrayList<>();
        WButton button = onUiThreadGet(() -> {
            WButton b = new WButton("OK");
            b.addAncestorListener(new WAncestorListener() {
                @Override
                public void ancestorAdded(WAncestorEvent event) {
                    calls.add("added");
                    received.add(event);
                }

                @Override
                public void ancestorRemoved(WAncestorEvent event) {
                    calls.add("removed");
                }

                @Override
                public void ancestorMoved(WAncestorEvent event) {
                    calls.add("moved");
                }
            });
            return b;
        });
        UiTestHarness.attachAndAwaitLoaded(button);
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (onUiThreadGet(calls::size) == 0 && System.nanoTime() < deadline) {
                onUiThread(() -> { });
            }
            onUiThread(() -> {
                assertEquals(Collections.singletonList("added"), calls);
                WAncestorEvent event = received.get(0);
                assertSame(button, event.getComponent());
                assertEquals(WAncestorEvent.ANCESTOR_ADDED, event.getID());
                assertEquals(1, button.getAncestorListeners().length);
                assertTrue(button.getAncestorListeners()[0] instanceof WAncestorListener);
            });
        } finally {
            UiTestHarness.detach(button);
        }
    }
}
