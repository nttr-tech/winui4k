package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WSplitPane from Java in the same way Swing developers would write JSplitPane. */
class WSplitPaneForJavaTest {
    @Test
    void constantsAndConstructorsCanBeUsedTheSameWayAsJSplitPane() {
        onUiThread(() -> {
            assertEquals(0, WSplitPane.VERTICAL_SPLIT);
            assertEquals(1, WSplitPane.HORIZONTAL_SPLIT);

            WLabel top = new WLabel("Top");
            WLabel bottom = new WLabel("Bottom");
            WSplitPane split = new WSplitPane(WSplitPane.VERTICAL_SPLIT, top, bottom);
            assertEquals(WSplitPane.VERTICAL_SPLIT, split.getOrientation());
            assertSame(top, split.getTopComponent());
            assertSame(bottom, split.getBottomComponent());
            assertSame(top, split.getLeftComponent());
            assertTrue(split.isContinuousLayout());

            WSplitPane notContinuous = new WSplitPane(WSplitPane.HORIZONTAL_SPLIT, false);
            assertFalse(notContinuous.isContinuousLayout());
            assertNull(notContinuous.getLeftComponent());
            notContinuous.setLeftComponent(top); // Moving it from another split removes it from the original split (as in Swing)
            assertNull(split.getTopComponent());
            assertSame(notContinuous, top.getParent());

            WSplitPane plain = new WSplitPane();
            assertEquals(WSplitPane.HORIZONTAL_SPLIT, plain.getOrientation());
            assertEquals(10, plain.getDividerSize());
        });
    }

    @Test
    void outOfRangeValuesThrowIllegalArgumentException() {
        onUiThread(() -> {
            WSplitPane split = new WSplitPane();
            assertThrows(IllegalArgumentException.class, () -> split.setOrientation(3));
            assertThrows(IllegalArgumentException.class, () -> split.setResizeWeight(1.5));
            assertThrows(IllegalArgumentException.class, () -> split.setDividerLocation(-0.5));
            assertThrows(IllegalArgumentException.class, () -> new WSplitPane(5));
        });
    }

    @Test
    void addChangeListenerReceivesDividerPositionChangesAndReadsThePositionFromGetSource() {
        WSplitPane split = onUiThreadGet(() -> {
            WSplitPane pane = new WSplitPane(WSplitPane.HORIZONTAL_SPLIT, new WLabel("Left"), new WLabel("Right"));
            pane.setWidth(300);
            pane.setHeight(80);
            pane.setHorizontalAlignment(HorizontalAlignment.LEFT);
            pane.setResizeWeight(0.5);
            pane.setDividerLocation(0.5); // A ratio before being shown becomes a position on the first layout
            return pane;
        });
        UiTestHarness.attachAndAwaitLoaded(split);
        try {
            onUiThread(split::validate);
            onUiThread(() -> {
                // (300 - 10) * 0.5 = 145
                assertEquals(145, split.getDividerLocation());

                List<Integer> locations = new ArrayList<>();
                List<Object> sources = new ArrayList<>();
                WChangeListener listener = e -> {
                    sources.add(e.getSource());
                    locations.add(((WSplitPane) e.getSource()).getDividerLocation());
                };
                split.addChangeListener(listener);
                assertArrayEquals(new WChangeListener[] {listener}, split.getChangeListeners());

                split.setDividerLocation(120); // The int overload
                assertEquals(145, split.getLastDividerLocation());
                split.setDividerLocation(0.0); // The double overload
                assertEquals(Arrays.asList(120, 0), locations);
                assertSame(split, sources.get(0));
                assertEquals(0, split.getMinimumDividerLocation());
                assertEquals(290, split.getMaximumDividerLocation());

                split.removeChangeListener(listener);
                split.setDividerLocation(50);
                assertEquals(2, locations.size());
            });
        } finally {
            UiTestHarness.detach(split);
        }
    }

    @Test
    void setEnabledAndRemoveAllHaveTheSameShapeAsJSplitPane() {
        onUiThread(() -> {
            WLabel left = new WLabel("left");
            WLabel right = new WLabel("right");
            WSplitPane split = new WSplitPane(WSplitPane.HORIZONTAL_SPLIT, left, right);
            assertTrue(split.isEnabled());
            split.setEnabled(false);
            assertFalse(split.isEnabled());
            split.setContinuousLayout(false);
            assertFalse(split.isContinuousLayout());
            split.setDividerSize(4);
            assertEquals(4, split.getDividerSize());
            split.removeAll();
            assertNull(split.getLeftComponent());
            assertNull(split.getRightComponent());
            assertNull(left.getParent());
            split.setTopComponent(left);
            split.setBottomComponent(right);
            assertSame(left, split.getLeftComponent());
            assertSame(right, split.getRightComponent());
        });
    }
}
