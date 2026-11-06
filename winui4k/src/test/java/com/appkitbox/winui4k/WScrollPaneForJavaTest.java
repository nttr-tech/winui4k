package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/** Tests that use WScrollPane from Java in the same way developers familiar with Swing's JScrollPane / JViewport would write it. */
class WScrollPaneForJavaTest {
    private static WCanvas content() {
        WCanvas canvas = new WCanvas();
        canvas.setWidth(1000);
        canvas.setHeight(800);
        return canvas;
    }

    @Test
    void policyConstantsCanBeReadWithTheSameValuesFromBothWScrollPaneAndWScrollPaneConstants() {
        assertEquals(20, WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        assertEquals(21, WScrollPane.VERTICAL_SCROLLBAR_NEVER);
        assertEquals(22, WScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        assertEquals(30, WScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        assertEquals(31, WScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        assertEquals(32, WScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        WScrollPaneConstants constants = onUiThreadGet(() -> new WScrollPane());
        assertEquals(WScrollPane.class, constants.getClass());
    }

    @Test
    void policiesCanBeSpecifiedWithTheSameConstructorsAndSettersAsSwing() {
        WScrollPane pane = onUiThreadGet(() -> new WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_ALWAYS, WScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        onUiThread(() -> {
            assertEquals(WScrollPane.VERTICAL_SCROLLBAR_ALWAYS, pane.getVerticalScrollBarPolicy());
            assertEquals(WScrollPane.HORIZONTAL_SCROLLBAR_NEVER, pane.getHorizontalScrollBarPolicy());
            pane.setHorizontalScrollBarPolicy(WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            assertEquals(ScrollBarVisibility.AUTO, pane.getHorizontalScrollBarVisibility());
            assertThrows(IllegalArgumentException.class, () -> pane.setVerticalScrollBarPolicy(WScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));
            WScrollPane empty = new WScrollPane(WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            WCanvas view = content();
            empty.setViewportView(view);
            assertSame(view, empty.getViewport().getView());
        });
    }

    @Test
    void changingThePositionWithGetViewportDeliversTheNewPositionToAnAnonymousChangeListener() throws InterruptedException {
        WScrollPane pane = onUiThreadGet(() -> {
            WScrollPane p = new WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            p.setWidth(200);
            p.setHeight(150);
            return p;
        });
        List<WPoint> positions = new CopyOnWriteArrayList<>();
        UiTestHarness.attachAndAwaitLoaded(pane);
        try {
            onUiThread(() -> {
                pane.updateLayout();
                WViewport viewport = pane.getViewport();
                viewport.addChangeListener(new WChangeListener() {
                    @Override
                    public void stateChanged(WChangeEvent e) {
                        positions.add(((WViewport) e.getSource()).getViewPosition());
                    }
                });
                viewport.setViewPosition(new WPoint(40, 60));
            });
            long deadline = System.currentTimeMillis() + 10_000;
            while (!positions.contains(new WPoint(40, 60)) && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
                onUiThread(() -> { });
            }
            onUiThread(() -> {
                WViewport viewport = pane.getViewport();
                assertEquals(new WPoint(40, 60), viewport.getViewPosition());
                assertEquals(new WSize(200, 150), viewport.getExtentSize());
                assertEquals(new WSize(1000, 800), viewport.getViewSize());
                assertEquals(new WRectangle(40, 60, 200, 150), viewport.getViewRect());
            });
        } finally {
            UiTestHarness.detach(pane);
        }
        assertEquals(new WPoint(40, 60), positions.get(positions.size() - 1));
    }

    @Test
    void anAnonymousAdjustmentListenerOnGetVerticalScrollBarReceivesTheValueAndType() throws InterruptedException {
        WScrollPane pane = onUiThreadGet(() -> {
            WScrollPane p = new WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_ALWAYS, WScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            p.setWidth(200);
            p.setHeight(150);
            WLabel header = new WLabel("Column");
            p.setColumnHeaderView(header);
            p.setRowHeaderView(new WLabel("Row"));
            p.setCorner(WScrollPane.UPPER_LEFT_CORNER, new WLabel("Corner"));
            assertSame(header, p.getColumnHeaderView());
            assertThrows(UnsupportedOperationException.class, () -> p.setCorner(WScrollPaneConstants.LOWER_LEFT_CORNER, new WLabel("x")));
            return p;
        });
        List<WAdjustmentEvent> events = new CopyOnWriteArrayList<>();
        UiTestHarness.attachAndAwaitLoaded(pane);
        try {
            onUiThread(() -> {
                pane.updateLayout();
                WScrollPaneBar bar = pane.getVerticalScrollBar();
                assertEquals(WAdjustable.VERTICAL, bar.getOrientation());
                bar.addAdjustmentListener(new WAdjustmentListener() {
                    @Override
                    public void adjustmentValueChanged(WAdjustmentEvent e) {
                        events.add(e);
                    }
                });
                bar.setValue(100);
                assertEquals(100, bar.getValue());
            });
            long deadline = System.currentTimeMillis() + 10_000;
            while (events.stream().noneMatch(e -> e.getValue() == 100 && !e.getValueIsAdjusting()) && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
                onUiThread(() -> { });
            }
        } finally {
            UiTestHarness.detach(pane);
        }
        WAdjustmentEvent last = events.get(events.size() - 1);
        assertEquals(WAdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, last.getID());
        assertEquals(WAdjustmentEvent.TRACK, last.getAdjustmentType());
        assertEquals(100, last.getValue());
        assertSame(onUiThreadGet(pane::getVerticalScrollBar), last.getAdjustable());
    }

    @Test
    void scrollRectToVisibleScrollsUntilTheRectangleInTheContentIsVisible() throws InterruptedException {
        WCanvas view = onUiThreadGet(WScrollPaneForJavaTest::content);
        WScrollPane pane = onUiThreadGet(() -> {
            WScrollPane p = new WScrollPane(view, WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            p.setWidth(200);
            p.setHeight(150);
            return p;
        });
        UiTestHarness.attachAndAwaitLoaded(pane);
        try {
            onUiThread(() -> {
                pane.updateLayout();
                view.scrollRectToVisible(new WRectangle(0, 700, 10, 100));
            });
            long deadline = System.currentTimeMillis() + 10_000;
            while (!onUiThreadGet(() -> pane.getViewport().getViewPosition()).equals(new WPoint(0, 650)) && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(new WPoint(0, 650), onUiThreadGet(() -> pane.getViewport().getViewPosition()));
        } finally {
            UiTestHarness.detach(pane);
        }
    }

    @Test
    void scrollToHorizontalOffsetAndARunnableAddViewChangedListener() throws InterruptedException {
        WScrollPane pane = onUiThreadGet(() -> {
            WScrollPane p = new WScrollPane(content(), WScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, WScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            p.setWidth(200);
            p.setHeight(150);
            return p;
        });
        List<String> calls = new CopyOnWriteArrayList<>();
        Runnable removed = () -> calls.add("removed");
        UiTestHarness.attachAndAwaitLoaded(pane);
        try {
            onUiThread(() -> {
                pane.updateLayout();
                pane.addViewChangedListener(new Runnable() {
                    @Override
                    public void run() {
                        calls.add("kept");
                    }
                });
                pane.addViewChangedListener(removed);
                pane.removeViewChangedListener(removed);
                pane.scrollToHorizontalOffset(300);
            });
            long deadline = System.currentTimeMillis() + 10_000;
            while (onUiThreadGet(pane::getHorizontalOffset) != 300.0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(300.0, onUiThreadGet(pane::getHorizontalOffset));
            assertTrue(calls.contains("kept"));
            assertFalse(calls.contains("removed"));
        } finally {
            UiTestHarness.detach(pane);
        }
    }
}
