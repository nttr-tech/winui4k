package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that receive button state changes from Java in the same way as Swing's AbstractButton.addChangeListener
 * (anonymous classes, lambdas, removing with getChangeListeners).
 */
class WButtonChangeListenerForJavaTest {
    @Test
    void anAnonymousChangeListenerReceivesChangesFromSetEnabledAndSetSelected() {
        onUiThread(() -> {
            List<Object> sources = new ArrayList<>();
            WToggleButton toggle = new WToggleButton("Bold");
            toggle.addChangeListener(new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    sources.add(e.getSource());
                }
            });
            toggle.setSelected(true);
            toggle.setEnabled(false);
            assertEquals(2, sources.size());
            assertSame(toggle, sources.get(0));
            assertSame(toggle, sources.get(1));
        });
    }

    @Test
    void removingAllListenersReturnedByGetChangeListenersCallsNothing() {
        onUiThread(() -> {
            int[] count = {0};
            WCheckBox box = new WCheckBox("Notify");
            box.addChangeListener(e -> count[0]++);
            WChangeListener[] listeners = box.getChangeListeners();
            assertEquals(1, listeners.length);
            for (WChangeListener l : box.getChangeListeners()) {
                box.removeChangeListener(l);
            }
            box.setSelected(true);
            assertEquals(0, box.getChangeListeners().length);
            assertEquals(0, count[0]);
        });
    }
}
