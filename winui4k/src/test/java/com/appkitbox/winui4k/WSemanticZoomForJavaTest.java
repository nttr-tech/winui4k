package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/** Tests that use WSemanticZoom from Java the way a developer used to Swing's ChangeListener would write it. */
class WSemanticZoomForJavaTest {
    @Test
    void anonymousChangeListenerReadsTheActiveViewAfterTheSwitch() throws InterruptedException {
        WSemanticZoom zoom = onUiThreadGet(() -> new WSemanticZoom(new WList(Arrays.asList("a1", "b1")), new WList(Arrays.asList("a", "b"))));
        List<Boolean> zoomedIn = new CopyOnWriteArrayList<>();
        UiTestHarness.attachAndAwaitLoaded(zoom);
        try {
            WChangeListener listener = new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    zoomedIn.add(((WSemanticZoom) e.getSource()).isZoomedInViewActive());
                }
            };
            onUiThread(() -> {
                zoom.addChangeListener(listener);
                assertArrayEquals(new WChangeListener[] {listener}, zoom.getChangeListeners());
                zoom.toggleActiveView();
            });
            long deadline = System.currentTimeMillis() + 10_000;
            while (zoomedIn.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
                onUiThread(() -> { });
            }
            onUiThread(() -> zoom.removeChangeListener(listener));
        } finally {
            UiTestHarness.detach(zoom);
        }
        assertEquals(Arrays.asList(false), zoomedIn);
        assertEquals(0, (int) onUiThreadGet(() -> zoom.getChangeListeners().length));
    }
}
