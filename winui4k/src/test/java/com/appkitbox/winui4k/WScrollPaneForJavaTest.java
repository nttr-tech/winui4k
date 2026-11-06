package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
