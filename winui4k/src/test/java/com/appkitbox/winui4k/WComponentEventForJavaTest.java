package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

/**
 * Tests that use the same shape as Component.addComponentListener from Java as a Swing developer would write it
 * (an anonymous ComponentAdapter subclass, a ComponentListener implementation, getID / getComponent / getComponentListeners).
 */
class WComponentEventForJavaTest {
    private static final long TIMEOUT_MILLIS = 3_000;
    private static final long SETTLE_MILLIS = 500;

    /** Waits until [events], read on the UI thread, becomes [expected], and checks that it does not change after waiting a little longer. */
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
    void anonymousComponentAdapterSubclassReceivesSizeAndVisibilityChanges() throws InterruptedException {
        List<String> events = new ArrayList<>();
        List<WComponentEvent> received = new ArrayList<>();
        WLabel label = onUiThreadGet(() -> {
            WLabel created = new WLabel("label");
            created.setWidth(40.0);
            created.setHeight(20.0);
            return created;
        });
        WCanvas canvas = onUiThreadGet(() -> {
            WCanvas created = new WCanvas();
            created.setWidth(200.0);
            created.setHeight(100.0);
            created.add(label, 0.0, 0.0);
            return created;
        });
        UiTestHarness.attachAndAwaitLoaded(canvas);
        try {
            onUiThread(() -> {
                canvas.updateLayout();
                label.addComponentListener(new WComponentAdapter() {
                    @Override
                    public void componentResized(WComponentEvent e) {
                        events.add("resized");
                        received.add(e);
                    }

                    @Override
                    public void componentHidden(WComponentEvent e) {
                        events.add("hidden");
                        received.add(e);
                    }
                });
            });
            Thread.sleep(SETTLE_MILLIS);
            onUiThread(events::clear);
            onUiThread(() -> label.setWidth(80.0));
            awaitEvents(events, Arrays.asList("resized"));
            onUiThread(() -> label.setVisible(false));
            awaitEvents(events, Arrays.asList("resized", "hidden"));
            onUiThread(() -> {
                WComponentEvent resized = received.get(received.size() - 2);
                assertEquals(WComponentEvent.COMPONENT_RESIZED, resized.getID());
                assertSame(label, resized.getComponent());
                assertSame(label, resized.getSource());
                assertEquals(WComponentEvent.COMPONENT_HIDDEN, received.get(received.size() - 1).getID());
            });
        } finally {
            UiTestHarness.detach(canvas);
        }
    }

    @Test
    void classImplementingComponentListenerCanBeAddedAndRemovedAndListedWithGetComponentListeners() {
        onUiThread(() -> {
            class Recorder implements WComponentListener {
                @Override
                public void componentResized(WComponentEvent e) {
                }

                @Override
                public void componentMoved(WComponentEvent e) {
                }

                @Override
                public void componentShown(WComponentEvent e) {
                }

                @Override
                public void componentHidden(WComponentEvent e) {
                }
            }
            Recorder recorder = new Recorder();
            WPanel panel = new WPanel();
            panel.addComponentListener(recorder);
            assertArrayEquals(new WComponentListener[] {recorder}, panel.getComponentListeners());
            panel.removeComponentListener(recorder);
            assertEquals(0, panel.getComponentListeners().length);
            // ContainerEvent derives from ComponentEvent, and the ID constants have the same values as Swing
            WContainerEvent containerEvent = new WContainerEvent(panel, WContainerEvent.COMPONENT_ADDED, new WLabel("a"));
            WComponentEvent asComponentEvent = containerEvent;
            assertSame(panel, asComponentEvent.getComponent());
            assertEquals(300, asComponentEvent.getID());
            assertEquals(100, WComponentEvent.COMPONENT_MOVED);
            assertEquals(103, WComponentEvent.COMPONENT_HIDDEN);
        });
    }
}
