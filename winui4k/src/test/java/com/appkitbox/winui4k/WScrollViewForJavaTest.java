package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/** Tests that use WScrollView from Java in the same way developers familiar with Swing's JScrollPane would write it. */
class WScrollViewForJavaTest {
    @Test
    void policyConstantsCanBeReadFromWScrollViewAndSettersEnableHorizontalScrolling() {
        assertEquals(22, WScrollView.VERTICAL_SCROLLBAR_ALWAYS);
        assertEquals(31, WScrollView.HORIZONTAL_SCROLLBAR_NEVER);
        onUiThread(() -> {
            WScrollView view = new WScrollView();
            view.setHorizontalScrollBarPolicy(WScrollView.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            assertEquals(WScrollView.HORIZONTAL_SCROLLBAR_AS_NEEDED, view.getHorizontalScrollBarPolicy());
            assertEquals(ScrollingContentOrientation.BOTH, view.getContentOrientation());
            WLabel label = new WLabel("a");
            view.setViewportView(label);
            assertSame(label, view.getContent());
        });
    }

    @Test
    void anAnonymousChangeListenerReceivesThePositionAfterSetViewPosition() throws InterruptedException {
        WScrollView view = onUiThreadGet(() -> {
            WCanvas canvas = new WCanvas();
            canvas.setWidth(1000);
            canvas.setHeight(800);
            WScrollView v = new WScrollView(canvas);
            v.setWidth(200);
            v.setHeight(150);
            v.setHorizontalScrollBarPolicy(WScrollView.HORIZONTAL_SCROLLBAR_ALWAYS);
            return v;
        });
        List<WPoint> positions = new CopyOnWriteArrayList<>();
        UiTestHarness.attachAndAwaitLoaded(view);
        try {
            onUiThread(() -> {
                view.updateLayout();
                view.addChangeListener(new WChangeListener() {
                    @Override
                    public void stateChanged(WChangeEvent e) {
                        positions.add(((WScrollView) e.getSource()).getViewPosition());
                    }
                });
                view.setViewPosition(new WPoint(30, 70));
            });
            long deadline = System.currentTimeMillis() + 10_000;
            while (!positions.contains(new WPoint(30, 70)) && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
                onUiThread(() -> { });
            }
        } finally {
            UiTestHarness.detach(view);
        }
        assertEquals(new WPoint(30, 70), positions.get(positions.size() - 1));
    }
}
