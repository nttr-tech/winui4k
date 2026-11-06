package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests adding and removing WCalendarView's ChangeListener from Java as a Swing developer would write it. */
class WCalendarViewForJavaTest {
    @Test
    void lambdaAndAnonymousChangeListenersCanBeAddedAndRemoved() {
        onUiThread(() -> {
            WCalendarView calendar = new WCalendarView();
            List<Object> sources = new ArrayList<>();
            WChangeListener lambda = e -> sources.add(e.getSource());
            WChangeListener anonymous = new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    sources.add(e.getSource());
                }
            };
            calendar.addChangeListener(lambda);
            calendar.addChangeListener(anonymous);
            assertArrayEquals(new WChangeListener[] {lambda, anonymous}, calendar.getChangeListeners());

            calendar.removeChangeListener(lambda);
            assertArrayEquals(new WChangeListener[] {anonymous}, calendar.getChangeListeners());
            calendar.removeChangeListener(anonymous);
            assertEquals(0, calendar.getChangeListeners().length);
            assertEquals(0, sources.size());
        });
    }
}
