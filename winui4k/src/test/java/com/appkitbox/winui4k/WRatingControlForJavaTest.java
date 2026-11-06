package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WRatingControl from Java the way a developer used to Swing's ChangeListener would write it. */
class WRatingControlForJavaTest {
    @Test
    void anonymousChangeListenerCanReadTheSetRatingFromTheSource() {
        onUiThread(() -> {
            WRatingControl rating = new WRatingControl();
            List<Double> values = new ArrayList<>();
            List<Object> sources = new ArrayList<>();
            rating.addChangeListener(new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    sources.add(e.getSource());
                    values.add(((WRatingControl) e.getSource()).getValue());
                }
            });
            rating.setValue(4);
            rating.setValue(4);
            assertEquals(Arrays.asList(4.0), values);
            assertSame(rating, sources.get(0));
        });
    }

    @Test
    void lambdaChangeListenerIsAlsoCalledWhenTheRatingIsClearedWithMinus1AndIsRemovedByRemove() {
        onUiThread(() -> {
            WRatingControl rating = new WRatingControl();
            List<Double> values = new ArrayList<>();
            WChangeListener listener = e -> values.add(rating.getValue());
            rating.addChangeListener(listener);
            assertArrayEquals(new WChangeListener[] {listener}, rating.getChangeListeners());
            rating.setValue(2.5);
            rating.setValue(-1);
            rating.removeChangeListener(listener);
            rating.setValue(5);
            assertEquals(Arrays.asList(2.5, -1.0), values);
            assertEquals(0, rating.getChangeListeners().length);
        });
    }
}
