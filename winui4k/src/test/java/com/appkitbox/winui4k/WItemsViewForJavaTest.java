package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.attachAndAwaitLoaded;
import static com.appkitbox.winui4k.UiTestHarness.detach;
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
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WItemsView in the same way Java developers who have used JList's selection API (getSelectedIndices / setSelectionInterval /
 * addListSelectionListener / ensureIndexIsVisible) would write it. Compiling at all verifies the shape of the API.
 */
class WItemsViewForJavaTest {
    private WItemsView createView() {
        WItemsView view = new WItemsView();
        view.setSelectionMode(ItemsViewSelectionMode.EXTENDED);
        List<WItemContainer> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            items.add(new WItemContainer(new WLabel("Item " + i)));
        }
        view.setItems(items);
        return view;
    }

    @Test
    void lambdaListenersReceiveEventsOfTheSameShapeAsSwing() {
        WItemsView view = onUiThreadGet(this::createView);
        List<int[]> ranges = new ArrayList<>();
        List<Object> sources = new ArrayList<>();
        attachAndAwaitLoaded(view);
        try {
            onUiThreadGet(() -> {
                view.addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        ranges.add(new int[] {e.getFirstIndex(), e.getLastIndex()});
                        sources.add(e.getSource());
                    }
                });
                view.setSelectionInterval(1, 3);
                view.addSelectionInterval(0, 0);
                return null;
            });
        } finally {
            detach(view);
        }
        assertEquals(2, ranges.size());
        assertArrayEquals(new int[] {1, 3}, ranges.get(0));
        assertArrayEquals(new int[] {0, 0}, ranges.get(1));
        assertSame(view, sources.get(0));
    }

    @Test
    void theSelectionCanBeReadAndWrittenWithGettersAndSetters() {
        WItemsView view = onUiThreadGet(this::createView);
        attachAndAwaitLoaded(view);
        try {
            int[] indices = onUiThreadGet(() -> {
                view.setSelectedIndices(new int[] {4, 2});
                assertEquals(2, view.getSelectedIndex());
                assertTrue(view.isSelectedIndex(4));
                assertFalse(view.isSelectedIndex(3));
                view.ensureIndexIsVisible(4);
                return view.getSelectedIndices();
            });
            assertEquals(Arrays.toString(new int[] {2, 4}), Arrays.toString(indices));
        } finally {
            detach(view);
        }
    }

    @Test
    void wItemContainerIsSelectedCanBeUsedFromJavaWithIsSelectedAndSetSelected() {
        WItemContainer container = onUiThreadGet(() -> new WItemContainer(new WLabel("Standalone")));
        WItemsView view = onUiThreadGet(() -> {
            WItemsView v = new WItemsView();
            v.setSelectionMode(ItemsViewSelectionMode.SINGLE);
            v.setItems(Arrays.asList(container));
            return v;
        });
        attachAndAwaitLoaded(view);
        try {
            boolean selected = onUiThreadGet(() -> {
                container.setSelected(true);
                return container.isSelected();
            });
            assertTrue(selected);
        } finally {
            detach(view);
        }
    }

    @Test
    void itemInvokedCanBeSubscribedWithAnIntConsumerLambdaAndRemovedWithTheSameReferenceAndLayoutAndIsItemInvokedEnabledWorkWithGettersAndSetters() {
        onUiThread(() -> {
            WItemsView view = createView();
            view.setItemInvokedEnabled(true);
            assertTrue(view.isItemInvokedEnabled());
            IntConsumer listener = index -> { };
            view.addItemInvokedListener(listener);
            view.removeItemInvokedListener(listener);
            view.removeItemInvokedListener(listener);

            WUniformGridLayout grid = new WUniformGridLayout();
            grid.setMinItemWidth(120.0);
            view.setLayout(grid);
            assertSame(grid, view.getLayout());
            view.setLayout(null);
            assertNull(view.getLayout());
        });
    }

    @Test
    void wItemContainerConstructorGetChildAndSetChild() {
        onUiThread(() -> {
            WLabel first = new WLabel("Before");
            WItemContainer container = new WItemContainer(first);
            assertSame(first, container.getChild());
            WLabel second = new WLabel("After");
            container.setChild(second);
            assertSame(second, container.getChild());
            container.setChild(null);
            assertNull(container.getChild());
            assertNull(new WItemContainer().getChild());
        });
    }
}
