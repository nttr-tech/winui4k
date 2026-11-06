package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use the same shape as Container.addContainerListener from Java as a Swing developer would write it
 * (an anonymous ContainerAdapter subclass, a ContainerListener implementation, getID / getContainer / getChild / getContainerListeners).
 */
class WContainerEventForJavaTest {
    @Test
    void anonymousContainerAdapterSubclassReceivesChildrenEnteringAndLeaving() {
        onUiThread(() -> {
            List<String> events = new ArrayList<>();
            List<WContainerEvent> received = new ArrayList<>();
            WPanel panel = new WPanel();
            WButton button = new WButton("OK");
            panel.addContainerListener(new WContainerAdapter() {
                @Override
                public void componentAdded(WContainerEvent e) {
                    events.add("added");
                    received.add(e);
                }

                @Override
                public void componentRemoved(WContainerEvent e) {
                    events.add("removed");
                    received.add(e);
                }
            });
            panel.add(button);
            panel.remove(button);
            assertEquals(Arrays.asList("added", "removed"), events);
            WContainerEvent added = received.get(0);
            assertEquals(WContainerEvent.COMPONENT_ADDED, added.getID());
            assertSame(panel, added.getSource());
            assertSame(panel, added.getContainer());
            assertSame(button, added.getChild());
            assertEquals(WContainerEvent.COMPONENT_REMOVED, received.get(1).getID());
        });
    }

    @Test
    void classImplementingContainerListenerCanBeAddedAndRemovedAndListedWithGetContainerListeners() {
        onUiThread(() -> {
            class Counter implements WContainerListener {
                int added;
                int removed;

                @Override
                public void componentAdded(WContainerEvent e) {
                    added++;
                }

                @Override
                public void componentRemoved(WContainerEvent e) {
                    removed++;
                }
            }
            Counter counter = new Counter();
            WPanel panel = new WPanel();
            panel.addContainerListener(counter);
            assertArrayEquals(new WContainerListener[] {counter}, panel.getContainerListeners());
            panel.add(new WLabel("a"));
            panel.add(new WLabel("b"), 0);
            panel.removeAll();
            assertEquals(2, counter.added);
            assertEquals(2, counter.removed);
            panel.removeContainerListener(counter);
            assertEquals(0, panel.getContainerListeners().length);
            panel.add(new WLabel("c"));
            assertEquals(2, counter.added);
        });
    }
}
