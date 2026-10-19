package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

/**
 * Tests that use the same shape as Component.addFocusListener in the plain Java a Swing developer would write
 * (an anonymous subclass of FocusAdapter, a FocusListener implementation, getID / isTemporary / getOppositeComponent /
 * getFocusListeners).
 */
class WComponentFocusForJavaTest {
    private static final long TIMEOUT_MILLIS = 3_000;
    private static final long SETTLE_MILLIS = 500;

    /** Waits until [events] read on the UI thread becomes [expected], and checks it does not change after a short wait. */
    private static void awaitEvents(List<String> events, List<String> expected) throws InterruptedException {
        Callable<List<String>> snapshot = () -> new ArrayList<>(events);
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!onUiThreadGet(snapshot).equals(expected) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        Thread.sleep(SETTLE_MILLIS);
        assertEquals(expected, onUiThreadGet(snapshot));
    }

    @Test
    void receivesFocusGainedAndLostWithAnonymousFocusAdapterSubclass() throws InterruptedException {
        List<String> events = new ArrayList<>();
        List<WFocusEvent> received = new ArrayList<>();
        WTextField field = onUiThreadGet(WTextField::new);
        WButton button = onUiThreadGet(() -> new WButton("OK"));
        WPanel panel = onUiThreadGet(() -> {
            WPanel box = new WPanel();
            box.add(field);
            box.add(button);
            return box;
        });
        UiTestHarness.attachAndAwaitLoaded(panel);
        try {
            onUiThread(() -> {
                field.addFocusListener(new WFocusAdapter() {
                    @Override
                    public void focusGained(WFocusEvent e) {
                        events.add("gained");
                        received.add(e);
                    }

                    @Override
                    public void focusLost(WFocusEvent e) {
                        events.add("lost");
                        received.add(e);
                    }
                });
                assertTrue(field.requestFocusInWindow());
            });
            awaitEvents(events, Arrays.asList("gained"));
            onUiThread(button::requestFocus);
            awaitEvents(events, Arrays.asList("gained", "lost"));
            onUiThread(() -> {
                WFocusEvent gained = received.get(0);
                assertSame(field, gained.getSource());
                assertSame(field, gained.getComponent());
                assertEquals(WFocusEvent.FOCUS_GAINED, gained.getID());
                assertFalse(gained.isTemporary());
                assertNull(gained.getOppositeComponent());
                assertEquals(WFocusEvent.FOCUS_LOST, received.get(1).getID());
            });
        } finally {
            UiTestHarness.detach(panel);
        }
    }

    @Test
    void canAddAndRemoveFocusListenerImplementationAndListThemWithGetFocusListeners() {
        onUiThread(() -> {
            class Recorder implements WFocusListener {
                @Override
                public void focusGained(WFocusEvent e) {
                }

                @Override
                public void focusLost(WFocusEvent e) {
                }
            }
            WButton button = new WButton("OK");
            Recorder first = new Recorder();
            Recorder second = new Recorder();
            assertEquals(0, button.getFocusListeners().length);
            button.addFocusListener(first);
            button.addFocusListener(second);
            button.addFocusListener(null);
            assertArrayEquals(new WFocusListener[] {first, second}, button.getFocusListeners());
            button.removeFocusListener(first);
            assertArrayEquals(new WFocusListener[] {second}, button.getFocusListeners());
        });
    }

    @Test
    void focusGainedAndFocusLostHaveSameValuesAsAwtFocusEventAndEventsCanBeCreatedManually() {
        assertEquals(1004, WFocusEvent.FOCUS_GAINED);
        assertEquals(1005, WFocusEvent.FOCUS_LOST);
        assertEquals(1004, WFocusEvent.FOCUS_FIRST);
        assertEquals(1005, WFocusEvent.FOCUS_LAST);
        onUiThread(() -> {
            WButton button = new WButton("OK");
            WFocusEvent event = new WFocusEvent(button, WFocusEvent.FOCUS_LOST);
            assertSame(button, event.getSource());
            assertEquals(WFocusEvent.FOCUS_LOST, event.getID());
            assertFalse(event.isTemporary());
            WFocusEvent temporary = new WFocusEvent(button, WFocusEvent.FOCUS_LOST, true, null);
            assertTrue(temporary.isTemporary());
        });
    }
}
